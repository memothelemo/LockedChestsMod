package xyz.memothelemo.lockablechests.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.memothelemo.lockablechests.api.types.ModActionResult;
import xyz.memothelemo.lockablechests.api.types.Permissions;
import xyz.memothelemo.lockablechests.types.NewLCData;

public interface LockableChest {
    void lc$overrideData(NewLCData data);
    @NotNull NewLCData lc$getData();

    @Nullable ChestBlockEntity lc$getOtherSide();
    @NotNull BlockPos lc$getBlockPos();

    /**
     * Updates chest data to other side of the chest if the lockable
     * chest argument is a double chest.
     */
    void lc$updateToOtherSide();

    /**
     * Checks whether the player views the chest inventory.
     */
    boolean lc$isPlayerViewing(@NotNull ServerPlayer player);

    @NotNull Permissions lc$getAccessPermissions(@NotNull ServerPlayer player);
    @NotNull Permissions lc$getAccessPermissions(@NotNull String playerUuid);

    @NotNull Permissions lc$getAccessPermissions(@NotNull ServerPlayer player, boolean actual);
    @NotNull Permissions lc$getAccessPermissions(@NotNull String playerUuid, boolean actual);

    @NotNull ModActionResult lc$lockChest(@NotNull ServerPlayer player);
    @NotNull ModActionResult lc$unlockChest(@NotNull ServerPlayer player);

    boolean lc$trustPlayer(@NotNull ServerPlayer player);
    boolean lc$untrustPlayer(@NotNull ServerPlayer player);

    boolean lc$trustPlayer(@NotNull String playerUuid);
    boolean lc$untrustPlayer(@NotNull String playerUuid);
}
