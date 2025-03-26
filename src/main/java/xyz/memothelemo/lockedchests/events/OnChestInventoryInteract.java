package xyz.memothelemo.lockedchests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public interface OnChestInventoryInteract {
    Event<OnChestInventoryInteract> EVENT = EventFactory.createArrayBacked(OnChestInventoryInteract.class,
            (listeners) -> (world, player, chest) -> {
                for (OnChestInventoryInteract listener : listeners) {
                    ActionResult result = listener.interact(world, player, chest);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult interact(World world, ServerPlayerEntity player, ChestBlockEntity chest);
}
