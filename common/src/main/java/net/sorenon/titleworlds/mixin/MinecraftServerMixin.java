package net.sorenon.titleworlds.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    protected abstract void updateMobSpawningFlags();

    /**
     * Cuts down load time by approx 7s at the cost of having chunks pop in on world load
     * Has been shown to cause issues such as the player falling through the world
     */
    @Inject(method = "prepareLevels", at = @At("HEAD"), cancellable = true)
    void skipLoadingChunksAroundPlayer(ChunkProgressListener chunkProgressListener, CallbackInfo ci) {
        if (STATE.isTitleWorld) {
            if (STATE.neededRadiusCenterInclusive == 0) {
                ci.cancel();
                this.updateMobSpawningFlags();
            }
        }
    }

    /**
     * Depending on how much the player cares they can increase the needed amount of chunks from zero
     * Causes fewer issues but may still be unstable
     */
    @ModifyConstant(method = "prepareLevels", constant = @Constant(intValue = 11))
    private int changeNeededRadius(int value) {
        if (STATE.isTitleWorld && STATE.neededRadiusCenterInclusive != 11) {
            return STATE.neededRadiusCenterInclusive;
        }
        return value;
    }

    @ModifyConstant(method = "prepareLevels", constant = @Constant(intValue = 441))
    private int changeNeededChunks(int value) {
        if (STATE.isTitleWorld && STATE.neededRadiusCenterInclusive != 11) {
            //Doubling the center inclusive radius adds the center chunk twice so we need to negate one
            int diameter = STATE.neededRadiusCenterInclusive * 2 - 1;
            return diameter * diameter;
        }
        return value;
    }
}
