package xyz.memothelemo.lockedchests.mixins;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockedchests.LockedChestsUtil;
import xyz.memothelemo.lockedchests.events.OnChestInventoryInteract;
import xyz.memothelemo.lockedchests.interfaces.LockableChest;

@Mixin(ServerPlayNetworkHandler.class)
public class ChestInventoryMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void onChestInventoryInteract(ClickSlotC2SPacket packet, CallbackInfo ci) {
        World world = this.player.getServerWorld();

        // We don't try to attempt to raycast if we're not in chest screen handler
        if (!(this.player.currentScreenHandler instanceof GenericContainerScreenHandler)) return;

        // Get the total item slots of a single/double chest
        ChestBlockEntity chest = LockedChestsUtil.INSTANCE.raycastChest(this.player);
        if (chest == null) return;

        if (this.isDraggedInChest(packet, (LockableChest) chest)) {
            ActionResult result = OnChestInventoryInteract.EVENT.invoker().interact(world, this.player, chest);
            if (result != ActionResult.FAIL) return;
            this.cancelInteraction(ci);
        }
    }

    @Unique
    private Boolean isDraggedInChest(ClickSlotC2SPacket packet, LockableChest chest) {
        int maxChestIdx = chest.getTotalSlotsIdx();
        return packet.getModifiedStacks()
                .keySet()
                .intStream()
                .anyMatch(key -> key <= maxChestIdx);

        // 1.21.5
        // return packet.modifiedStacks()
        // .keySet()
        // .intStream()
        // .anyMatch(key -> key <= maxChestIdx);
    }

    @Unique
    private void cancelInteraction(CallbackInfo ci) {
        this.player.updateLastActionTime();
        this.player.currentScreenHandler.updateToClient();
        ci.cancel();
    }
}
