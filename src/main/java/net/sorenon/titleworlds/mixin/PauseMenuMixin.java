package net.sorenon.titleworlds.mixin;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.sorenon.titleworlds.Screenshot3D;
import net.sorenon.titleworlds.TWConfigGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseMenuMixin extends Screen {

    protected PauseMenuMixin(Component component) {
        super(component);
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", ordinal = 7, target = "Lnet/minecraft/client/gui/screens/PauseScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"), cancellable = true)
    public void createPauseMenuMixin(CallbackInfo ci) {
        ci.cancel();
        Component component = this.minecraft.isLocalServer() ? Component.translatable("menu.returnToMenu") : Component.translatable("menu.disconnect");
        this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 120 + -16, 204, 20, component, (buttonx) -> {
            boolean bl = this.minecraft.isLocalServer();
            boolean bl2 = this.minecraft.isConnectedToRealms();
            buttonx.active = false;
            this.minecraft.level.disconnect();
            if (bl) {

                if (TWConfigGlobal.ScreenshotOnExit)
                    Screenshot3D.take3DScreenshotOnExit(this.minecraft.level);

                this.minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
            } else {
                this.minecraft.clearLevel();
            }

            TitleScreen titleScreen = new TitleScreen();
            if (bl) {
                this.minecraft.setScreen(titleScreen);
            } else if (bl2) {
                this.minecraft.setScreen(new RealmsMainScreen(titleScreen));
            } else {
                this.minecraft.setScreen(new JoinMultiplayerScreen(titleScreen));
            }

        }));
    }

}
