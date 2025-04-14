package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Tuple;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockablechests.events.ChestInventoryChangeCallback;
import xyz.memothelemo.lockablechests.interfaces.CompoundContainerAccessor;

import java.util.Objects;

@Mixin(ServerGamePacketListenerImpl.class)
public class InventoryChangeMixin {
    @Shadow public ServerPlayer player;

    // Tuple< chest entity (first entity if it is a double chest), container (whole) >
    @Unique
    private @Nullable Tuple<ChestBlockEntity, Container> lc$getChestMetadataFromMenu() {
        if (!(this.player.containerMenu instanceof ChestMenu menu))
            return null;

        if (menu.getContainer() instanceof ChestBlockEntity entity)
            return new Tuple<>(entity, entity);

        // If the player is looking at the double chest, the container
        // should be an instance of CompoundContainer with two inner
        // chest containers.
        if (!(menu.getContainer() instanceof CompoundContainer container))
            return null;

        if (((CompoundContainerAccessor)  container).lc$getContainer1() instanceof ChestBlockEntity entity
            && ((CompoundContainerAccessor) container).lc$getContainer2() instanceof ChestBlockEntity)
            return new Tuple<>(entity, container);

        return null;
    }

    @Inject(
            method = "handleContainerClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;suppressRemoteUpdates()V"
            ),
            cancellable = true
    )
    public void onContainerButtonClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        Tuple<ChestBlockEntity, Container> tuple = this.lc$getChestMetadataFromMenu();
        if (tuple == null) return;

        int maxChestSlotIdx = tuple.getB().getContainerSize() - 1;
        boolean withinChest = packet.getChangedSlots()
            .keySet()
            .intStream()
            .anyMatch(key -> key <= maxChestSlotIdx);

        if (!withinChest) return;
        InteractionResult result = ChestInventoryChangeCallback.EVENT.invoker().onChestInventoryChanged(
                Objects.requireNonNull(tuple.getA().getLevel()),
                this.player,
                tuple.getA()
        );

        // performing cancellation
        if (result != InteractionResult.PASS) {
            this.player.containerMenu.broadcastFullState();
            ci.cancel();
        }
    }
}
