package net.sorenon.titleworlds.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

	protected TitleScreenMixin(Component component) {
		super(component);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"), method = "render")
	void cancel(PanoramaRenderer instance, float f, float g){
		if (Minecraft.getInstance().level == null || !Minecraft.getInstance().isRunning()) {
//			instance.render(f, g);
			this.renderDirtBackground(0);
		}
	}

	@Inject(method = "isPauseScreen", cancellable = true, at = @At("HEAD"))
	void isPauseScreen(CallbackInfoReturnable<Boolean> cir){
		if (TitleWorldsMod.state.isTitleWorld) {
			cir.setReturnValue(TitleWorldsMod.state.pause);
		}
	}

	@Inject(method = "shouldCloseOnEsc", cancellable = true, at = @At("HEAD"))
	void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir){
		if (!TitleWorldsMod.state.isTitleWorld) {
			cir.setReturnValue(true);
		}
	}
}
