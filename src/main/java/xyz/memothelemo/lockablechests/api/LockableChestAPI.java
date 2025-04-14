package xyz.memothelemo.lockablechests.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.memothelemo.lockablechests.api.types.ModActionResult;
import xyz.memothelemo.lockablechests.interfaces.LockableChest;

import java.util.List;

public class LockableChestAPI {
    public static boolean isTrusted(@NotNull ChestBlockEntity source, @NotNull ServerPlayer target) {
        return ((LockableChest) source).lc$getAccessPermissions(target).isTrusted();
    }

    public static @NotNull ModActionResult lockChest(@NotNull ServerPlayer player, @NotNull ChestBlockEntity entity) {
        LockableChest chest = (LockableChest) entity;
        return chest.lc$lockChest(player);
    }

    public static @NotNull ModActionResult unlockChest(@NotNull ServerPlayer player, @NotNull ChestBlockEntity entity) {
        LockableChest chest = (LockableChest) entity;
        return chest.lc$unlockChest(player);
    }
}
