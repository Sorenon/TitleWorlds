package net.sorenon.titleworlds;

import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;

public class Platform {

    public static ModService INSTANCE = null;

    public static LevelStorageSource getLevelStorageSource() {
        return INSTANCE.getLevelSource();
    }

    public static LevelStorageSource getSaveOnExitSource() {
        return INSTANCE.getSaveOnExitSource();
    }

    public static Path getConfigDirectory() {
        return INSTANCE.getConfigDirectory();
    }
}
