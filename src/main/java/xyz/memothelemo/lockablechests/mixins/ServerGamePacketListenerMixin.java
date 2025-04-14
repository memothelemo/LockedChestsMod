package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockablechests.events.LCPlayerEvents;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("TAIL"))
    public void invokePlayerLeft(DisconnectionDetails details, CallbackInfo ci) {
        LCPlayerEvents.LEAVE.invoker().onPlayerLeave(this.player);
    }
}
