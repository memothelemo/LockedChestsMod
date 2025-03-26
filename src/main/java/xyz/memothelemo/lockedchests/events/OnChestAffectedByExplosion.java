package xyz.memothelemo.lockedchests.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

// TODO: Make this shorter. Its name is pretty long
public interface OnChestAffectedByExplosion {
    Event<OnChestAffectedByExplosion> EVENT = EventFactory.createArrayBacked(OnChestAffectedByExplosion.class,
            (listeners) -> (world, chest) -> {
                for (OnChestAffectedByExplosion listener : listeners) {
                    ActionResult result = listener.interact(world, chest);
                    if (result != ActionResult.PASS) return result;
                }
                return ActionResult.PASS;
            });

    ActionResult interact(World world, ChestBlockEntity chest);
}
