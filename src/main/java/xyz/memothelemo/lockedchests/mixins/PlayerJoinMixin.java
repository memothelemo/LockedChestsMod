package xyz.memothelemo.lockedchests.mixins;

import net.minecraft.network.packet.c2s.play.PlayerLoadedC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockedchests.events.OnPlayerJoin;

@Mixin(ServerPlayNetworkHandler.class)
public class PlayerJoinMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerLoaded", at = @At("RETURN"))
    public void onPlayerJoin(PlayerLoadedC2SPacket packet, CallbackInfo ci) {
        OnPlayerJoin.EVENT.invoker().interact(this.player);
    }
}
