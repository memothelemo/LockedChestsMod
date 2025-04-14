package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface HopperEventCallback {
    Event<Eject> EJECT = EventFactory.createArrayBacked(
            Eject.class,
            (listeners) -> (level, container) -> {
                for (Eject listener : listeners) {
                    InteractionResult result = listener.onHopperEjectItems(level, container);
                    if (result != InteractionResult.PASS)
                        return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
    );

    Event<Suck> SUCK = EventFactory.createArrayBacked(
            Suck.class,
            (listeners) -> (level, container) -> {
                for (Suck listener : listeners) {
                    InteractionResult result = listener.onHopperSuckItems(level, container);
                    if (result != InteractionResult.PASS)
                        return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
    );

    interface Eject {
        InteractionResult onHopperEjectItems(@NotNull Level level, @NotNull Container targetContainer);
    }

    interface Suck {
        InteractionResult onHopperSuckItems(@NotNull Level level, @NotNull Container sourceContainer);
    }
}
