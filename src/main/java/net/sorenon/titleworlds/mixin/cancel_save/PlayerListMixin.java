package net.sorenon.titleworlds.mixin.cancel_save;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    void cancelSave(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (TitleWorldsMod.state.isTitleWorld && TitleWorldsMod.state.noSave) {
            ci.cancel();
        }
    }
}
