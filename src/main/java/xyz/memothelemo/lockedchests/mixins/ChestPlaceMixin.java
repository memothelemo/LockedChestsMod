package xyz.memothelemo.lockedchests.mixins;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.memothelemo.lockedchests.events.OnChestUpgrade;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ChestPlaceMixin {
    @Shadow private GameMode gameMode;

    @Shadow @Final protected ServerPlayerEntity player;

    @Shadow public abstract boolean isCreative();

    @Unique
    private boolean chestPlaceMixin$isHoldingChestBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ChestBlock;
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onBlockPlaced(
            ServerPlayerEntity player,
            World world,
            ItemStack stack,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!this.chestPlaceMixin$isHoldingChestBlock(stack)) return;

        // Since the stack is a chest, we don't need any extra validation
        BlockPos hitPos = hitResult.getBlockPos();
        BlockState hitBlockState = world.getBlockState(hitPos);

        boolean isHoldingAnItem = !player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty();
        boolean forcefullyPlaceItem = player.shouldCancelInteraction() && isHoldingAnItem;

        // Let internal `interactBlock` handle this case here.
        if (!hitBlockState.getBlock().isEnabled(world.getEnabledFeatures()) || this.gameMode == GameMode.SPECTATOR) return;

        ItemStack heldItem = stack.copy();
        ActionResult result;

        // Replicating `interactBlock` decompiled code, one by one.
        if (!forcefullyPlaceItem) {
            ActionResult useResult = hitBlockState.onUseWithItem(player.getStackInHand(hand), world, player, hand, hitResult);
            if (useResult.isAccepted()) {
                Criteria.ITEM_USED_ON_BLOCK.trigger(player, hitPos, heldItem);
                cir.setReturnValue(useResult);
                return;
            }

            // I honestly don't know what it is being used for.
            if (useResult instanceof ActionResult.PassToDefaultBlockAction && hand == Hand.MAIN_HAND) {
                result = hitBlockState.onUse(world, player, hitResult);
                if (result.isAccepted()) {
                    Criteria.DEFAULT_BLOCK_USE.trigger(player, hitPos);
                    cir.setReturnValue(useResult);
                    return;
                }
            }
        }

        // Tool interaction / block placement
        boolean shouldMaybePlaceItem = !stack.isEmpty() && !player.getItemCooldownManager().isCoolingDown(stack);
        int oldStackCount = stack.getCount();
        if (!shouldMaybePlaceItem) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        ItemUsageContext itemUsageContext = new ItemUsageContext(player, hand, hitResult);
        BlockPos exactBlockPos = itemUsageContext.getBlockPos().offset(itemUsageContext.getSide());
        BlockState previousExactBlockState = world.getBlockState(exactBlockPos);
        result = stack.useOnBlock(itemUsageContext);

        ActionResult eventResult = ActionResult.PASS;
        if ((world.getBlockEntity(exactBlockPos) instanceof ChestBlockEntity chest)) {
            ChestType chestType = chest.getCachedState().get(ChestBlock.CHEST_TYPE);
            if (chestType != ChestType.SINGLE) {
                eventResult = OnChestUpgrade.EVENT.invoker().interact(world, player, chest);
            }
        }

        if (eventResult != ActionResult.FAIL) {
            // Finalize the final action result and off we go!
            if (result.isAccepted()) {
                Criteria.ITEM_USED_ON_BLOCK.trigger(player, hitPos, heldItem);
            }
            cir.setReturnValue(result);
            return;
        }

        // memo's place cancellation method
        stack.setCount(oldStackCount);
        world.setBlockState(exactBlockPos, previousExactBlockState);

        // update player's screen if it does exists
        if (player.currentScreenHandler != null) {
            player.currentScreenHandler.updateToClient();
        }

        cir.setReturnValue(ActionResult.FAIL);
    }
}
