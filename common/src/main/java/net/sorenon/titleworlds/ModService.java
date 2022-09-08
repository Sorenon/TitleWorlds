package net.sorenon.titleworlds;

import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;

public interface ModService {

    LevelStorageSource getLevelSource();

    LevelStorageSource getSaveOnExitSource();

    Path getConfigDirectory();
}
