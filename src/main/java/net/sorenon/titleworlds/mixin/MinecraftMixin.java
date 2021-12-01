package net.sorenon.titleworlds.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.util.Function4;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.*;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oshi.util.tuples.Quartet;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin extends ReentrantBlockableEventLoop<Runnable> {

    @Shadow
    @Nullable
    public ClientLevel level;

    public MinecraftMixin(String string) {
        super(string);
    }

    @Shadow
    @Final
    private LevelStorageSource levelSource;

    @Shadow
    public abstract void setScreen(@Nullable Screen guiScreen);

    @Shadow
    @Final
    private AtomicReference<StoringChunkProgressListener> progressListener;

    @Shadow
    @Final
    private Proxy proxy;

    @Shadow
    @Final
    public File gameDirectory;

    @Shadow
    private @Nullable IntegratedServer singleplayerServer;

    @Shadow
    private boolean isLocalServer;

    @Shadow
    @Final
    private Queue<Runnable> progressTasks;

    @Shadow
    protected abstract void runTick(boolean renderLevel);

    @Shadow
    private @Nullable Connection pendingConnection;

    @Shadow
    public abstract User getUser();

    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    public abstract void clearLevel();

    @Shadow
    private volatile boolean running;
    @Unique
    private boolean closingLevel;

    /**
     * Called when joining / leaving a server
     */
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    void preClearLevel(Screen screen, CallbackInfo ci) {
        if (TitleWorldsMod.state.isTitleWorld) {
            if (activeLoadingFuture != null) {
                while (!activeLoadingFuture.isDone()) {
                    this.runAllTasks();
                    this.runTick(false);
                }
                activeLoadingFuture = null;
                if (cleanup != null) {
                    cleanup.run();
                }
            }

            if (singleplayerServer != null) {
                // Ensure the server has initialized so we don't orphan it
                while (!this.singleplayerServer.isReady()) {
                    this.runAllTasks();
                    this.runTick(false);
                }
                if (this.pendingConnection != null || this.level != null) {
                    // Wait for connection to establish so it can be killed cleanly on this.level.disconnect();
                    while (this.pendingConnection != null) {
                        this.runAllTasks();
                        this.runTick(false);
                    }
                }
                this.singleplayerServer.halt(false);
            }
        } else {
            this.closingLevel = this.level != null;
        }

        if (this.level != null) {
            this.level.disconnect();
        }
    }

    /**
     * Called when joining / leaving a server
     */
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("RETURN"))
    void postClearLevel(Screen screen, CallbackInfo ci) {
        if (TitleWorldsMod.state.isTitleWorld) {
            TitleWorldsMod.state.isTitleWorld = false;
            TitleWorldsMod.state.pause = false;
        } else if (this.closingLevel && this.running) {
            try {
                this.loadTitleWorld(TitleWorldsMod.getRandomWorld(), RegistryAccess.builtin(), Minecraft::loadDataPacks, Minecraft::loadWorldData, false);
            } catch (ExecutionException | InterruptedException | LevelStorageException e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "setCurrentServer", at = @At("HEAD"))
    void setCurrentServer(CallbackInfo ci) {
        if (this.level != null) {
            this.clearLevel();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    void setScreen(Screen guiScreen, CallbackInfo ci) {
        if (TitleWorldsMod.state.isTitleWorld) {
            if (this.screen instanceof TitleScreen && guiScreen instanceof TitleScreen) {
                ci.cancel();
            } else if (guiScreen == null) {
                setScreen(new TitleScreen());
                ci.cancel();
            } else if (guiScreen instanceof ProgressScreen || guiScreen instanceof ReceivingLevelScreen) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    void init(GameConfig gameConfig, CallbackInfo ci) {
        try {
            this.loadTitleWorld(TitleWorldsMod.getRandomWorld(), RegistryAccess.builtin(), Minecraft::loadDataPacks, Minecraft::loadWorldData, false);
        } catch (ExecutionException | InterruptedException | LevelStorageException e) {
            e.printStackTrace();
        }
    }

    @Unique
    @Nullable
    private Future<?> activeLoadingFuture = null;

    @Unique
    @Nullable
    private Runnable cleanup = null;

    @Unique
    private void loadTitleWorld(String levelName,
                                RegistryAccess.RegistryHolder dynamicRegistries,
                                Function<LevelStorageSource.LevelStorageAccess, DataPackConfig> levelSaveToDatapackFunction,
                                Function4<LevelStorageSource.LevelStorageAccess, RegistryAccess.RegistryHolder, ResourceManager, DataPackConfig, WorldData> quadFunction,
                                boolean vanillaOnly
    ) throws ExecutionException, InterruptedException {
        TitleWorldsMod.LOGGER.info("Loading title world");
        TitleWorldsMod.state.isTitleWorld = true;
        TitleWorldsMod.state.pause = false;

        var worldResourcesFuture = CompletableFuture.supplyAsync(() -> openWorldResources(levelName, dynamicRegistries, levelSaveToDatapackFunction, vanillaOnly));

        activeLoadingFuture = worldResourcesFuture;
        cleanup = () -> {
            try {
                var worldResources = worldResourcesFuture.get();
                worldResources.getA().close();
                worldResources.getC().close();
            } catch (InterruptedException | ExecutionException | IOException e) {
                TitleWorldsMod.LOGGER.error("Exception caught when cleaning up async world load 1", e);
            }
        };

        TitleWorldsMod.LOGGER.info("One");
        while (!worldResourcesFuture.isDone()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }

        var worldResources = worldResourcesFuture.get();
        LevelStorageSource.LevelStorageAccess levelStorageAccess = worldResources.getA();
        PackRepository packRepository = worldResources.getC();
        CompletableFuture<ServerResources> serverResourcesFuture = worldResources.getD();

        activeLoadingFuture = serverResourcesFuture;
        cleanup = () -> {
            try {
                levelStorageAccess.close();
                packRepository.close();
                serverResourcesFuture.get().close();
            } catch (InterruptedException | ExecutionException | IOException e) {
                TitleWorldsMod.LOGGER.error("Exception caught when cleaning up async world load 2", e);
            }
        };

        TitleWorldsMod.LOGGER.info("Two");
        while (!serverResourcesFuture.isDone()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }

        ServerResources serverResources = serverResourcesFuture.get();

        this.progressListener.set(null);

        activeLoadingFuture = CompletableFuture.runAsync(() -> startSingleplayerServer(levelName, levelStorageAccess, dynamicRegistries, serverResources, worldResources.getB(), packRepository, quadFunction));
        cleanup = null;

        TitleWorldsMod.LOGGER.info("Three");
        while (singleplayerServer == null || !this.singleplayerServer.isReady()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }

        var joinServerFuture = CompletableFuture.supplyAsync(this::joinSingleplayerServer);

        activeLoadingFuture = joinServerFuture;

        TitleWorldsMod.LOGGER.info("Four");
        while (!joinServerFuture.isDone()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }
        activeLoadingFuture = null;
        this.pendingConnection = joinServerFuture.get();
        TitleWorldsMod.LOGGER.info("Five");
    }

    @Unique
    private Quartet<LevelStorageSource.LevelStorageAccess, DataPackConfig, PackRepository, CompletableFuture<ServerResources>> openWorldResources(
            String levelName,
            RegistryAccess.RegistryHolder dynamicRegistries,
            Function<LevelStorageSource.LevelStorageAccess, DataPackConfig> levelSaveToDatapackFunction,
            boolean vanillaOnly
    ) {
        LevelStorageSource.LevelStorageAccess levelStorageAccess;
        try {
            levelStorageAccess = TitleWorldsMod.levelSource.createAccess(levelName);
        } catch (IOException var21) {
            throw new RuntimeException("Failed to read data");
        }

        DataPackConfig dataPackConfig = levelSaveToDatapackFunction.apply(levelStorageAccess);
        PackRepository packRepository = new PackRepository(PackType.SERVER_DATA, new ServerPacksSource(), new FolderRepositorySource(levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), PackSource.WORLD));
        DataPackConfig dataPackConfig2 = MinecraftServer.configurePackRepository(packRepository, dataPackConfig, vanillaOnly);
        CompletableFuture<ServerResources> completableFuture = ServerResources.loadResources(packRepository.openAllSelected(), dynamicRegistries, Commands.CommandSelection.INTEGRATED, 2, Util.backgroundExecutor(), this);
        return new Quartet<>(levelStorageAccess, dataPackConfig2, packRepository, completableFuture);
    }

    @Unique
    private void startSingleplayerServer(
            String levelName,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            RegistryAccess.RegistryHolder dynamicRegistries,
            ServerResources serverResources,
            DataPackConfig dataPackConfig,
            PackRepository packRepository,
            Function4<LevelStorageSource.LevelStorageAccess, RegistryAccess.RegistryHolder, ResourceManager, DataPackConfig, WorldData> quadFunction
    ) {
        WorldData worldData = quadFunction.apply(levelStorageAccess, dynamicRegistries, serverResources.getResourceManager(), dataPackConfig);

        try {
            levelStorageAccess.saveDataTag(dynamicRegistries, worldData);
            serverResources.updateGlobals();
            YggdrasilAuthenticationService iOException4 = new YggdrasilAuthenticationService(this.proxy);
            MinecraftSessionService minecraftSessionService = iOException4.createMinecraftSessionService();
            GameProfileRepository gameProfileRepository = iOException4.createProfileRepository();
            GameProfileCache gameProfileCache = new GameProfileCache(gameProfileRepository, new File(this.gameDirectory, MinecraftServer.USERID_CACHE_FILE.getName()));
            gameProfileCache.setExecutor((Minecraft) (Object) this);
            SkullBlockEntity.setup(gameProfileCache, minecraftSessionService, this);
            GameProfileCache.setUsesAuthentication(false);
            this.singleplayerServer = MinecraftServer.spin(thread -> new IntegratedServer(thread, (Minecraft) (Object) this, dynamicRegistries, levelStorageAccess, packRepository, serverResources, worldData, minecraftSessionService, gameProfileRepository, gameProfileCache, i -> {
                StoringChunkProgressListener storingChunkProgressListener = new StoringChunkProgressListener(i);
                this.progressListener.set(storingChunkProgressListener);
                return ProcessorChunkProgressListener.createStarted(storingChunkProgressListener, this.progressTasks::add);
            }));
            this.isLocalServer = true;
        } catch (Throwable var19) {
            CrashReport minecraftSessionService = CrashReport.forThrowable(var19, "Starting integrated server");
            CrashReportCategory gameProfileRepository = minecraftSessionService.addCategory("Starting integrated server");
            gameProfileRepository.setDetail("Level ID", levelName);
            gameProfileRepository.setDetail("Level Name", worldData.getLevelName());
            throw new ReportedException(minecraftSessionService);
        }
    }

    @Unique
    private Connection joinSingleplayerServer() {
        SocketAddress minecraftSessionService = this.singleplayerServer.getConnection().startMemoryChannel();
        Connection gameProfileRepository = Connection.connectToLocalServer(minecraftSessionService);
        gameProfileRepository.setListener(new ClientHandshakePacketListenerImpl(gameProfileRepository, (Minecraft) (Object) this, null, component -> {
        }));
        gameProfileRepository.send(new ClientIntentionPacket(minecraftSessionService.toString(), 0, ConnectionProtocol.LOGIN));
        gameProfileRepository.send(new ServerboundHelloPacket(this.getUser().getGameProfile()));
        return gameProfileRepository;
    }

}
