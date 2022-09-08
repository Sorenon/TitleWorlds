package net.sorenon.titleworlds.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.loading.ClientModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(ClientModLoader.class)
public class ClientModLoaderMixin {

    @Inject(method = "completeModLoading", at = @At("RETURN"), remap = false)
    private static void isThereSeriouslyNoEventForThis(CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue()) {
            STATE.reloading = true;
            Minecraft.getInstance().clearLevel();
        }
    }
}
