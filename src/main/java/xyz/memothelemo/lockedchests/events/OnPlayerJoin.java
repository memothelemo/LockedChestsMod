package xyz.memothelemo.lockedchests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface OnPlayerJoin {
    Event<OnPlayerJoin> EVENT = EventFactory.createArrayBacked(OnPlayerJoin.class,
            (listeners) -> (player) -> {
                for (OnPlayerJoin listener : listeners) {
                    listener.interact(player);
                }
            });

    void interact(ServerPlayerEntity player);
}
