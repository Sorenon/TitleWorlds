package net.sorenon.titleworlds.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageException;
import net.sorenon.titleworlds.Platform;
import net.sorenon.titleworlds.Screenshot3D;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Unique
    private boolean noLevels;

    @Inject(method = "init", at = @At("HEAD"))
    void onInit(CallbackInfo ci) {
//        boolean modmenu = FabricLoader.getInstance().isModLoaded("modmenu");
        boolean modmenu = false;

        var level = Minecraft.getInstance().level;
        if (!STATE.isTitleWorld && level != null) {
            this.addRenderableWidget(new ImageButton(
                    this.width / 2 + 104,
                    (this.height / 4 + 48) + 60 + (modmenu ? 24 : 0), 20,
                    20, 0, 0, 20,
                    new ResourceLocation("titleworlds", "/textures/gui/3dscreenshot.png"),
                    32, 64,
                    (button -> {
                        String name = Screenshot3D.take3DScreenshot(level, null);
                        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("titleworlds.message.saved_3d_screenshot", name));
                    })));
        } else if (TitleWorldsMod.CONFIG.enabled) {
            if (TitleWorldsMod.CONFIG.reloadButton) {
                this.addRenderableWidget(new ImageButton(
                        this.width / 2 + 104,
                        (this.height / 4 + 48) + 60 + (modmenu ? 24 : 0), 20,
                        20, 0, 0, 20,
                        new ResourceLocation("titleworlds", "/textures/gui/reload.png"),
                        32, 64,
                        (button -> {
                            if (!STATE.reloading) {
                                STATE.reloading = true;
                                Minecraft.getInstance().clearLevel();
                            }
                        })));
            }
        }

        if (STATE.isTitleWorld) {
            this.noLevels = false;
        } else {
            try {
                this.noLevels = Platform.getLevelStorageSource().findLevelCandidates().isEmpty();
            } catch (LevelStorageException e) {
                //TODO
//                TitleWorldsMod.LOGGER.error(e);
            }
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"), method = "render")
    void cancelCubemapRender(PanoramaRenderer instance, float f, float g) {
        if (Minecraft.getInstance().level == null || !Minecraft.getInstance().isRunning()) {
            if (STATE.isTitleWorld) {
                this.renderDirtBackground(0);
            } else {
                instance.render(f, g);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
    void render(PoseStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.noLevels) {
            GuiComponent.drawCenteredString(matrices, font, "Put one or more worlds in the titleworlds folder and restart the game", this.width / 2, 2, 16777215);
        }
    }

    @Inject(method = "isPauseScreen", cancellable = true, at = @At("HEAD"))
    void isPauseScreen(CallbackInfoReturnable<Boolean> cir) {
        if (STATE.isTitleWorld) {
            cir.setReturnValue(STATE.pause);
        }
    }

    @Inject(method = "shouldCloseOnEsc", cancellable = true, at = @At("HEAD"))
    void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir) {
        if (!STATE.isTitleWorld) {
            cir.setReturnValue(true);
        }
    }
}
