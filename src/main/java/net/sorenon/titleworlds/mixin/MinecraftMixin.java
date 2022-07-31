package net.sorenon.titleworlds.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.sorenon.titleworlds.Timer;
import net.sorenon.titleworlds.TitleWorldsMod;
import net.sorenon.titleworlds.mixin.accessor.WorldOpenFlowsAcc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oshi.util.tuples.Triplet;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin extends ReentrantBlockableEventLoop<Runnable> {

    @Shadow
    @Nullable
    public ClientLevel level;

    public MinecraftMixin(String string) {
        super(string);
    }

    @Shadow
    public abstract void setScreen(@Nullable Screen guiScreen);

    @Shadow
    @Final
    private AtomicReference<StoringChunkProgressListener> progressListener;

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
    private volatile boolean running;

    @Shadow
    @Final
    private YggdrasilAuthenticationService authenticationService;

    @Shadow
    @Final
    private ProfileKeyPairManager profileKeyPairManager;

    @Shadow
    public abstract void tick();

    @Unique
    private boolean closingLevel;

    @Unique
    private static final Logger LOGGER = LogManager.getLogger("Title World Loader");

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
            TitleWorldsMod.LOGGER.info("Closing Title World");
            TitleWorldsMod.state.isTitleWorld = false;
            TitleWorldsMod.state.pause = false;
        } else if (this.closingLevel && this.running) {
            TitleWorldsMod.LOGGER.info("Loading Title World");
            tryLoadTitleWorld();
        } else if (TitleWorldsMod.state.reloading) {
            tryLoadTitleWorld();
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
        tryLoadTitleWorld();
    }

    @Unique
    private static final Random random = new Random();

    @SuppressWarnings("UnusedReturnValue")
    @Unique
    public boolean tryLoadTitleWorld() {
        if (!TitleWorldsMod.CONFIG.enabled) {
            return false;
        }

        try {
            var list = TitleWorldsMod.LEVEL_SOURCE.findLevelCandidates().levels();

            if (list.isEmpty()) {
                LOGGER.info("TitleWorlds folder is empty");
                return false;
            }

            var config = TitleWorldsMod.CONFIG;
            int titleworldIndex = config.useTitleWorldOverride ? config.titleWorldOverride : random.nextInt(list.size());
            this.loadTitleWorld(list.get(titleworldIndex).directoryName());


            return true;

        } catch (ExecutionException | InterruptedException | LevelStorageException | ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Exception when loading title world", e);
            return false;
        }
    }

    @Unique
    @Nullable
    private Future<?> activeLoadingFuture = null;

    @Unique
    @Nullable
    private Runnable cleanup = null;

    /**
     * Instead of loading the world synchronously, we load as much as possible off thread
     * This allows 2 things
     * <p>
     * 1. The client thread can load the came while load files are being loaded in a different thread
     * 2. The game can render the progress screen
     * <p>
     * This does come at a cost of extra complexity but can cut down load times significantly
     * <p>
     * Each future is split at a point where synchronous access is needed
     * TODO is there a different way to do that?
     */
    @Unique
    private void loadTitleWorld(String levelName
    ) throws ExecutionException, InterruptedException {
        var timer = new Timer(TitleWorldsMod.CONFIG.profiling);

        LOGGER.info("Loading title world");
        TitleWorldsMod.state.isTitleWorld = true;
        TitleWorldsMod.state.neededRadiusCenterInclusive = TitleWorldsMod.CONFIG.preloadChunksRadius;

        timer.start();

        var worldResourcesFuture
                = CompletableFuture.supplyAsync(() -> openWorldResources(levelName, false));

        timer.run("call openWorldResources", true);

        activeLoadingFuture = worldResourcesFuture;
        cleanup = () -> {
            try {
                var worldResources = worldResourcesFuture.get();
                worldResources.getA().close();
            } catch (InterruptedException | ExecutionException | IOException e) {
                LOGGER.error("Exception caught when cleaning up async world load stage 1", e);
            }
        };

        LOGGER.info("Loading world resources");
        while (!worldResourcesFuture.isDone()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }

        timer.run("wait openWorldResources");

        var worldResources = worldResourcesFuture.get();
        LevelStorageSource.LevelStorageAccess levelStorageAccess = worldResources.getA();
        PackRepository packRepository = worldResources.getB();
        CompletableFuture<WorldStem> worldStemCompletableFuture = worldResources.getC();

        activeLoadingFuture = worldStemCompletableFuture;
        cleanup = () -> {
            try {
                levelStorageAccess.close();
                worldStemCompletableFuture.get().close();
            } catch (InterruptedException | ExecutionException | IOException e) {
                TitleWorldsMod.LOGGER.error("Exception caught when cleaning up async world load stage 2", e);
            }
        };

        LOGGER.info("Waiting for WorldStem to load");
        while (!worldStemCompletableFuture.isDone()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }

        timer.run("wait WorldStem");

        WorldStem worldStem = worldStemCompletableFuture.get();

        this.progressListener.set(null);

        LOGGER.info("Starting server");
        activeLoadingFuture = CompletableFuture.runAsync(() -> startSingleplayerServer(levelName, levelStorageAccess, worldStem, packRepository));
        cleanup = null;

        timer.run("call startSingleplayerServer", true);

        while (singleplayerServer == null || !this.singleplayerServer.isReady()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }

        timer.run("wait startSingleplayerServer");

        LOGGER.info("Joining singleplayer server");
        var joinServerFuture = CompletableFuture.runAsync(this::joinSingleplayerServer);
        timer.run("call joinSingleplayerServer", true);

        activeLoadingFuture = joinServerFuture;

        while (!joinServerFuture.isDone()) {
            this.runAllTasks();
            this.runTick(false);
            if (!TitleWorldsMod.state.isTitleWorld) {
                return;
            }
        }
        activeLoadingFuture = null;

        timer.run("wait joinSingleplayerServer");

        LOGGER.info("Logging into title world");
        TitleWorldsMod.state.reloading = false;
    }

    @Unique
    private Triplet<LevelStorageSource.LevelStorageAccess, PackRepository, CompletableFuture<WorldStem>> openWorldResources(
            String levelName,
            boolean vanillaOnly
    ) {
        LevelStorageSource.LevelStorageAccess levelStorageAccess;
        try {
            levelStorageAccess = TitleWorldsMod.LEVEL_SOURCE.createAccess(levelName);

            if (TitleWorldsMod.CONFIG.screenshotOnExit) {
                levelStorageAccess = TitleWorldsMod.saveOnExitSource.createAccess(levelName);
            }
        } catch (IOException var21) {
            throw new RuntimeException("Failed to read data");
        }

        var packRepository = WorldOpenFlowsAcc.invokeCreatePackRepository(levelStorageAccess);

        return new Triplet<>(levelStorageAccess, packRepository, this.loadWorldStem(levelStorageAccess, vanillaOnly, packRepository));
    }

    private CompletableFuture<WorldStem> loadWorldStem(LevelStorageSource.LevelStorageAccess levelStorageAccess,
                                                       boolean bl,
                                                       PackRepository packRepository) {
        DataPackConfig dataPackConfig = levelStorageAccess.getDataPacks();
        if (dataPackConfig == null) {
            throw new RuntimeException("Failed to load data pack config");
        } else {
            WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, dataPackConfig, bl);
            WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, 2);

            return WorldStem.load(initConfig, (resourceManager, dataPackConfigx) -> {
                RegistryAccess.Writable writable = RegistryAccess.builtinCopy();
                DynamicOps<Tag> dynamicOps = RegistryOps.createAndLoad(NbtOps.INSTANCE, writable, resourceManager);
                WorldData worldData = levelStorageAccess.getDataTag(dynamicOps, dataPackConfigx, writable.allElementsLifecycle());
                if (worldData == null) {
                    throw new RuntimeException("Failed to load world");
                } else {
                    return Pair.of(worldData, writable.freeze());
                }
            }, Util.backgroundExecutor(), this);
        }
    }

    @Unique
    private void startSingleplayerServer(
            String levelName,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            WorldStem worldStem,
            PackRepository packRepository
    ) {
        WorldData worldData = worldStem.worldData();
        this.progressListener.set(null);

        try {
            levelStorageAccess.saveDataTag(worldStem.registryAccess(), worldStem.worldData());
            Services services = Services.create(this.authenticationService, this.gameDirectory);
            services.profileCache().setExecutor(this);
            SkullBlockEntity.setup(services, this);
            GameProfileCache.setUsesAuthentication(false);
            this.singleplayerServer = MinecraftServer.spin(thread -> new IntegratedServer(thread, (Minecraft) (Object) this, levelStorageAccess, packRepository, worldStem, services, i -> {
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
    private void joinSingleplayerServer() {
        SocketAddress minecraftSessionService = this.singleplayerServer.getConnection().startMemoryChannel();
        Connection pendingConnection = Connection.connectToLocalServer(minecraftSessionService);

        pendingConnection.setListener(
                new ClientHandshakePacketListenerImpl(
                        pendingConnection,
                        (Minecraft) (Object) this,
                        null,
                        component -> {
                        }
                )
        );

        pendingConnection.send(new ClientIntentionPacket(minecraftSessionService.toString(), 0, ConnectionProtocol.LOGIN));

        //this.pendingConnection must be set before sending ServerboundHelloPacket or a rare crash can occur
        this.pendingConnection = pendingConnection;
        pendingConnection.send(new ServerboundHelloPacket(this.getUser().getName(), this.profileKeyPairManager.profilePublicKeyData(), Optional.ofNullable(this.getUser().getProfileId())));
    }
}
