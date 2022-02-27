package net.sorenon.titleworlds.mixin.cancel_save;

import net.minecraft.server.level.ServerLevel;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class LevelMixin {

    @Inject(method = "noSave", at = @At("HEAD"), cancellable = true)
    void noSave(CallbackInfoReturnable<Boolean> cir){
        if (TitleWorldsMod.state.isTitleWorld && TitleWorldsMod.state.noSave) {
            cir.setReturnValue(true);
        }
    }
}
