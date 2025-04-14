package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface BlockExplodeCallback {
    Event<BlockExplodeCallback> EVENT = EventFactory.createArrayBacked(
        BlockExplodeCallback.class,
        (listeners) -> (level, state, pos) -> {
            for (BlockExplodeCallback listener : listeners) {
                InteractionResult result = listener.shouldExplodeBlock(level, state, pos);
                if (result != InteractionResult.PASS)
                    return result;
            }
            return InteractionResult.PASS;
        }
    );

    InteractionResult shouldExplodeBlock(
            @NotNull ServerLevel level,
            @NotNull BlockState blockState,
            @NotNull BlockPos blockPos
    );
}
