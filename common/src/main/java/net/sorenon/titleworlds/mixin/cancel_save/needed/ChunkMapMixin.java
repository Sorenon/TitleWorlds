package net.sorenon.titleworlds.mixin.cancel_save.needed;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.sorenon.titleworlds.SnapshotCreateServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    protected abstract byte markPosition(ChunkPos chunkPos,
                                         ChunkStatus.ChunkType chunkType);

    @Shadow
    @Final
    ServerLevel level;

    /**
     * Prevent saving any chunk terrain
     */
    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;incrementCounter(Ljava/lang/String;)V"), cancellable = true)
    void cancelSave(ChunkAccess chunkAccess, CallbackInfoReturnable<Boolean> cir) {
        if (STATE.isTitleWorld && STATE.noSave
                || this.level.getServer() instanceof SnapshotCreateServer) {
            this.markPosition(chunkAccess.getPos(), chunkAccess.getStatus().getChunkType());
            cir.setReturnValue(true);
        }
    }
}
