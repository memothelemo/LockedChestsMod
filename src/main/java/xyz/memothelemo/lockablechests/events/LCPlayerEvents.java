package xyz.memothelemo.lockablechests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public interface LCPlayerEvents {
    Event<Join> JOIN = EventFactory.createArrayBacked(
            Join.class,
            (listeners) -> (player) -> {
                for (Join listener : listeners) {
                    listener.onPlayerJoin(player);
                }
            }
    );

    Event<Leave> LEAVE = EventFactory.createArrayBacked(
            Leave.class,
            (listeners) -> (player) -> {
                for (Leave listener : listeners) {
                    listener.onPlayerLeave(player);
                }
            }
    );

    interface Join {
        void onPlayerJoin(@NotNull ServerPlayer player);
    }

    interface Leave {
        void onPlayerLeave(@NotNull ServerPlayer player);
    }
}
