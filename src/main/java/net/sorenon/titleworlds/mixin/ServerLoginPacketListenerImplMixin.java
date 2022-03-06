package net.sorenon.titleworlds.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow
    public abstract String getUserName();

    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    void extraDisconnectData(Component reason, CallbackInfo ci) {
        if (TitleWorldsMod.state.isTitleWorld) {
            LOGGER.error("TitleWorld: {} lost connection: {}", this.getUserName(), reason.getString());
            new Exception().printStackTrace();
            LOGGER.error("Loading the title world failed in an unexpected way, please report this at https://github.com/Sorenon/TitleWorlds/issues");
        }
    }
}
