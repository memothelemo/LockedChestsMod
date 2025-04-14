package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface OnBlockClick {
    Event<OnBlockClick> EVENT = EventFactory.createArrayBacked(
        OnBlockClick.class,
        (listeners) -> (level, player, state, pos) -> {
            for (OnBlockClick listener : listeners) {
                listener.onBlockClick(level, player, state, pos);
            }
        }
    );

    void onBlockClick(
            @NotNull Level level,
            @NotNull ServerPlayer player,
            @NotNull BlockState blockState,
            @NotNull BlockPos blockPos
    );
}
