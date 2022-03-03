package net.sorenon.titleworlds.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    /**
     * Prevents the game from trying to close the current screen when the player joins a title world
     */
//    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
//    void cancelSetScreen(Minecraft instance, Screen screen) {
//        if (!TitleWorldsMod.state.isTitleWorld) {
//            instance.setScreen(null);
//        }
//    }
}
