package net.sorenon.titleworlds.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.sorenon.titleworlds.ModService;

import java.nio.file.Path;

public class ModServiceImpl implements ModService {

    public LevelStorageSource LEVEL_SOURCE;
    public LevelStorageSource saveOnExitSource;
    public Path configPath;

    public ModServiceImpl() {
        Minecraft minecraft = Minecraft.getInstance();
        Path titleWorldsPath = minecraft.gameDirectory.toPath().resolve("titleworlds");
        Path exitOnSavePath = titleWorldsPath.resolve("latest");

        TitleWorldsModFabric.LOGGER.info("Opening level storage source");

        LEVEL_SOURCE = new LevelStorageSource(titleWorldsPath, minecraft.gameDirectory.toPath().resolve("titleworlds"), minecraft.getFixerUpper());
        saveOnExitSource = new LevelStorageSource(exitOnSavePath, minecraft.gameDirectory.toPath().resolve("titleworldbackups"), minecraft.getFixerUpper());
        configPath = FabricLoader.getInstance().getGameDir().resolve("config");
    }


    @Override
    public LevelStorageSource getLevelSource() {
        return LEVEL_SOURCE;
    }

    @Override
    public LevelStorageSource getSaveOnExitSource() {
        return saveOnExitSource;
    }

    @Override
    public Path getConfigDirectory() {
        return configPath;
    }
}
