package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface BlockInteractCallback {
    Event<BlockInteractCallback> EVENT = EventFactory.createArrayBacked(
            BlockInteractCallback.class,
            (listeners) -> (level, player, hand, stack, blockState, blockPos) -> {
                for (BlockInteractCallback listener : listeners) {
                    InteractionResult result = listener.onBlockInteract(
                            level, player,
                            hand, stack,
                            blockState, blockPos
                    );
                    if (result != InteractionResult.PASS)
                        return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
    );

    InteractionResult onBlockInteract(
            @NotNull Level level,
            @NotNull ServerPlayer player,
            @NotNull InteractionHand hand,
            @NotNull ItemStack stack,
            @NotNull BlockState blockState,
            @NotNull BlockPos blockPos
    );
}
