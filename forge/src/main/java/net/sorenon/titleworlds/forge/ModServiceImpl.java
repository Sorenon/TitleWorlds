package net.sorenon.titleworlds.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.sorenon.titleworlds.ModService;

import java.nio.file.Path;

public class ModServiceImpl implements ModService {

    public LevelStorageSource LEVEL_SOURCE;
    public LevelStorageSource saveOnExitSource;

    public ModServiceImpl() {
        Minecraft minecraft = Minecraft.getInstance();
        Path titleWorldsPath = minecraft.gameDirectory.toPath().resolve("titleworlds");
        Path exitOnSavePath = titleWorldsPath.resolve("latest");

        TitleWorldsModForge.LOGGER.info("Opening level storage source");

        LEVEL_SOURCE = new LevelStorageSource(titleWorldsPath, minecraft.gameDirectory.toPath().resolve("titleworlds"), minecraft.getFixerUpper());
        saveOnExitSource = new LevelStorageSource(exitOnSavePath, minecraft.gameDirectory.toPath().resolve("titleworldbackups"), minecraft.getFixerUpper());
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
        return FMLPaths.CONFIGDIR.get();
    }
}
