package net.sorenon.titleworlds;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.sorenon.titleworlds.config.ConfigUtil;
import net.sorenon.titleworlds.config.GlobalConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TitleWorldsMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Title World");

    private static KeyMapping keyBinding;

    public static State state = new State();

    public static LevelStorageSource levelSource;
    public static LevelStorageSource saveOnExitSource;

    public static GlobalConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = ConfigUtil.loadConfig();

        LOGGER.info("Opening level storage source");
        Minecraft minecraft = Minecraft.getInstance();
        Path titleWorldsPath = minecraft.gameDirectory.toPath().resolve("titleworlds");
        Path exitOnSavePath = titleWorldsPath.resolve("latest");

        levelSource = new LevelStorageSource(titleWorldsPath, minecraft.gameDirectory.toPath().resolve("titleworldbackups"), minecraft.getFixerUpper());
        saveOnExitSource = new LevelStorageSource(exitOnSavePath, minecraft.gameDirectory.toPath().resolve("titleworldbackups"), minecraft.getFixerUpper());

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.titleworlds.opentitlescreen",
                InputConstants.UNKNOWN.getValue(),
                "category.titleworlds"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.isDown()) {
                client.setScreen(new TitleScreen());
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("titleworlds:3Dscreenshot")
                    .executes(ctx -> {
                        String name = Screenshot3D.take3DScreenshot(ctx.getSource().getWorld(), null);
                        ctx.getSource().sendFeedback(MutableComponent.create(new LiteralContents("Saved 3D screenshot as " + name)));
                        return 1;
                    }).then(argument("name", StringArgumentType.string())
                            .executes(ctx -> {
                                String name = Screenshot3D.take3DScreenshot(ctx.getSource().getWorld(), StringArgumentType.getString(ctx, "name"));
                                ctx.getSource().sendFeedback(MutableComponent.create(new LiteralContents("Saved 3D screenshot as " + name)));
                                return 1;
                            })));
        });
    }

    public static class State {
        public boolean isTitleWorld = false;

        //TODO we need to figure out a different way to pause the game because this prevents syncing
        public boolean pause = false;
        public boolean noSave = true;
        public boolean reloading = false;
        public int neededRadiusCenterInclusive = 4;
    }

    /*
     Unstable but super speedy
     Default for vanilla like modpacks?
     neededRadiusCenterInclusive = 0
     [14:53:50] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer took 634ms
     [14:53:50] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer 5121ms since start
     [14:53:50] [Render thread/INFO] (Title World Loader) Joining singleplayer server
     [14:53:50] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer took 691ms
     [14:53:50] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer 5813ms since start

     Probably stops player from falling through the world but still kinda unstable
     Default for MCXR?
     neededRadiusCenterInclusive = 1
     [14:54:50] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer took 1230ms
     [14:54:50] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer 5923ms since start
     [14:54:50] [Render thread/INFO] (Title World Loader) Joining singleplayer server
     [14:54:51] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer took 686ms
     [14:54:51] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer 6611ms since start

     FastLoader default -> Should be fine for playable worlds
     neededRadiusCenterInclusive = 4
     [14:56:21] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer took 1675ms
     [14:56:21] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer 6466ms since start
     [14:56:21] [Render thread/INFO] (Title World Loader) Joining singleplayer server
     [14:56:22] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer took 692ms
     [14:56:22] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer 7159ms since start

     Minecraft's default -> Most stable for playable worlds
     neededRadiusCenterInclusive = 11
     [14:58:34] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer took 2565ms
     [14:58:34] [Render thread/INFO] (TitleWorlds Profiling) wait startSingleplayerServer 7134ms since start
     [14:58:34] [Render thread/INFO] (Title World Loader) Joining singleplayer server
     [14:58:35] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer took 707ms
     [14:58:35] [Render thread/INFO] (TitleWorlds Profiling) wait joinSingleplayerServer 7842ms since start

     VIEW_ONLY_FAST = 0
     VIEW_ONLY_STABLE = 1

     PLAYABLE_FAST = 4
     PLAYABLE_STABLE = 11
     */

}
