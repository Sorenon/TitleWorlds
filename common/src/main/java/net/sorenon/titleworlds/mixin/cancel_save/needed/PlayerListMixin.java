package net.sorenon.titleworlds.mixin.cancel_save.needed;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.sorenon.titleworlds.SnapshotCreateServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    /**
     * Prevent saving player data
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    void cancelSave(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (STATE.isTitleWorld && STATE.noSave
                || this.server instanceof SnapshotCreateServer) {
            ci.cancel();
        }
    }
}
