package net.sorenon.titleworlds.mixin.cancel_save;

import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentEntitySectionManager.class)
public class PersistentEntitySectionManagerMixin {

    /**
     * Prevent save on serverLevel.close() to optimize title world close time
     */
    @Inject(method = "saveAll", at = @At("HEAD"), cancellable = true)
    void cancelSave(CallbackInfo ci) {
        if (TitleWorldsMod.state.isTitleWorld && TitleWorldsMod.state.noSave) {
            ci.cancel();
        }
    }
}
