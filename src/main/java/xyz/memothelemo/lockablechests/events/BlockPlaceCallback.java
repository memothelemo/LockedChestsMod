package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface BlockPlaceCallback {
    Event<BlockPlaceCallback> EVENT = EventFactory.createArrayBacked(
        BlockPlaceCallback.class,
        (listeners) -> (level, player, stack, blockState, pos) -> {
            for (BlockPlaceCallback listener : listeners) {
                InteractionResult result = listener.onPlaceBlock(level, player, stack, blockState, pos);
                if (result != InteractionResult.PASS)
                    return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
    );

    InteractionResult onPlaceBlock(
            @NotNull Level level,
            @NotNull ServerPlayer player,
            @NotNull ItemStack stack,
            @NotNull BlockState blockState,
            @NotNull BlockPos pos
    );
}
