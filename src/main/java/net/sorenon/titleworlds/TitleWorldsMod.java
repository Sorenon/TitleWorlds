package net.sorenon.titleworlds;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class TitleWorldsMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Title World");

    private static KeyMapping keyBinding;

    public static State state = new State();

    public static LevelStorageSource levelSource;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Opening level storage source");
        Minecraft minecraft = Minecraft.getInstance();
        Path titleWorldsPath = minecraft.gameDirectory.toPath().resolve("titleworlds");
        levelSource = new LevelStorageSource(titleWorldsPath, minecraft.gameDirectory.toPath().resolve("titleworldbackups"), minecraft.getFixerUpper());

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
    }

    public static class State {
        public boolean isTitleWorld = false;

        public boolean pause = false;
    }
}
