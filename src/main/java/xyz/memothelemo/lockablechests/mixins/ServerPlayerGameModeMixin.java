package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.memothelemo.lockablechests.ModLogger;
import xyz.memothelemo.lockablechests.events.BlockInteractCallback;
import xyz.memothelemo.lockablechests.events.OnBlockClick;
import xyz.memothelemo.lockablechests.events.BlockPlaceCallback;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow public abstract boolean isCreative();
    @Shadow protected ServerLevel level;
    @Shadow private GameType gameModeForPlayer;

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"
            ),
            cancellable = true
    )
    public void detectBlockInteraction(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        boolean hoveringItem = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        boolean placeForcefully = player.isSecondaryUseActive() && hoveringItem;
        if (placeForcefully) return;

        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = level.getBlockState(blockPos);
        InteractionResult result = BlockInteractCallback.EVENT.invoker()
                .onBlockInteract(level, player, hand, stack, blockState, blockPos);

        if (result != InteractionResult.PASS)
            cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;isOnCooldown(Lnet/minecraft/world/item/ItemStack;)Z"
            ),
            cancellable = true
    )
    public void onBlockPlace(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack)) return;

        UseOnContext ctx = new UseOnContext(player, hand, hitResult);
        InteractionResult result;

        BlockPos maybeExactPos = ctx.getClickedPos().relative(ctx.getClickedFace());
        BlockState prevBlockState = level.getBlockState(maybeExactPos);

        int oldCount = stack.getCount();
        result = stack.useOn(ctx);

        // If placement attempt is successful, then we can evaluate further
        // using event callbacks to determine whether we should cancel
        // the player's placement.
        if (!result.consumesAction()) {
            cir.setReturnValue(result);
            return;
        }

        BlockState currentBlockState = level.getBlockState(maybeExactPos);
        InteractionResult callbackResult = BlockPlaceCallback.EVENT
                .invoker()
                .onPlaceBlock(level, player, stack, currentBlockState, maybeExactPos);

        // Revert any block placement changes
        if (callbackResult != InteractionResult.PASS) {
            ModLogger.INSTANCE.debug("Placement failed at "
                    + maybeExactPos
                    + "! Callback result is cancelled");

            stack.setCount(oldCount);
            if (!level.setBlockAndUpdate(maybeExactPos, prevBlockState)) {
                ModLogger.INSTANCE.warn("Could not revert to previous state at "
                        + maybeExactPos
                        + " while trying to cancel placement made by Player "
                        + player.getName().getString());
            }

            player.containerMenu.broadcastFullState();
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (this.isCreative()) stack.setCount(oldCount);
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, hitResult.getBlockPos(), stack);
    }


    @Inject(
            method = "handleBlockBreakAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    public void detectBlockClicks(
            BlockPos pos,
            ServerboundPlayerActionPacket.Action action,
            Direction face, int maxBuildHeight, int sequence,
            CallbackInfo ci
    ) {
        if (!this.level.mayInteract(this.player, pos)) return;

        BlockState state = this.level.getBlockState(pos);
        if (!state.isAir() && !this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
            OnBlockClick.EVENT.invoker().onBlockClick(this.level, this.player, state, pos);
        }
    }
}
