package xyz.memothelemo.lockablechests.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import xyz.memothelemo.lockablechests.interfaces.LockableChest;

public interface LockableChestCallback {
    Event<Locked> LOCKED = EventFactory.createArrayBacked(
        LockableChestCallback.Locked.class,
        (listeners) -> (level, newOwner, chest) -> {
            for (LockableChestCallback.Locked listener : listeners) {
                InteractionResult result = listener.onChestLocked(level, newOwner, chest);
                if (result != InteractionResult.PASS) return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
    );

    interface Locked {
        InteractionResult onChestLocked(Level level, ServerPlayer newOwner, LockableChest chest);
    }

    Event<Unlocked> UNLOCKED = EventFactory.createArrayBacked(
        LockableChestCallback.Unlocked.class,
        (listeners) -> (level, owner, chest) -> {
            for (LockableChestCallback.Unlocked listener : listeners) {
                InteractionResult result = listener.onChestUnlock(level, owner, chest);
                if (result != InteractionResult.PASS) return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
    );

    interface Unlocked {
        InteractionResult onChestUnlock(Level level, ServerPlayer owner, LockableChest chest);
    }
}
