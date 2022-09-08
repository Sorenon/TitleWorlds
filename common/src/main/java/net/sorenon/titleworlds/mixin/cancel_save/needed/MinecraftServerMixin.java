package net.sorenon.titleworlds.mixin.cancel_save.needed;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    /**
     * Prevent saving misc world data
     */
    @Inject(method = "saveAllChunks", at = @At("HEAD"), cancellable = true)
    void cancelSave(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        if (STATE.isTitleWorld && STATE.noSave) {
            cir.setReturnValue(false);
        }
    }
}
