package net.sorenon.titleworlds.mixin.cancel_save;

import net.minecraft.server.level.ServerChunkCache;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    void cancelSave(boolean bl, CallbackInfo ci){
        if (TitleWorldsMod.state.isTitleWorld && TitleWorldsMod.state.noSave) {
            ci.cancel();
        }
    }
}
