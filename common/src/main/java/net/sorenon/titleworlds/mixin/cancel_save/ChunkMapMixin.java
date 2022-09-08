package net.sorenon.titleworlds.mixin.cancel_save;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    /**
     * Prevent the server waiting for updates to finish
     */
    @Inject(method = "hasWork", at = @At("HEAD"), cancellable = true)
    void skipWork(CallbackInfoReturnable<Boolean> cir) {
        if (STATE.isTitleWorld && STATE.noSave) {
            cir.setReturnValue(false);
        }
    }
}
