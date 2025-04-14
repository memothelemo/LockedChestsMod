package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.NotNull;

public interface ChestInventoryChangeCallback {
    Event<ChestInventoryChangeCallback> EVENT = EventFactory.createArrayBacked(
        ChestInventoryChangeCallback.class,
        (listeners) -> (level, player, chest) -> {
            for (ChestInventoryChangeCallback listener : listeners) {
                InteractionResult result = listener.onChestInventoryChanged(level, player, chest);
                if (result != InteractionResult.PASS)
                    return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
    );

    InteractionResult onChestInventoryChanged(
            @NotNull Level level,
            @NotNull ServerPlayer player,
            @NotNull ChestBlockEntity chest
    );
}
