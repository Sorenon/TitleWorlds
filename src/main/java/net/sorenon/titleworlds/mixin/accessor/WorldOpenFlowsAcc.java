package net.sorenon.titleworlds.mixin.accessor;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldOpenFlows.class)
public interface WorldOpenFlowsAcc {

    @Invoker
    static PackRepository invokeCreatePackRepository(LevelStorageSource.LevelStorageAccess levelStorageAccess) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    WorldStem invokeLoadWorldStem(WorldLoader.PackConfig packConfig, WorldLoader.WorldDataSupplier<WorldData> worldDataSupplier) throws Exception;
}
