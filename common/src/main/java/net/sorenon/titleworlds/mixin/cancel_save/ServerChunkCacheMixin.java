package net.sorenon.titleworlds.mixin.cancel_save;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {

    /**
     * Prevent save on close() to optimize title world close time
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    void cancelSave(boolean bl, CallbackInfo ci) {
        if (STATE.isTitleWorld && STATE.noSave) {
            ci.cancel();
        }
    }
}
