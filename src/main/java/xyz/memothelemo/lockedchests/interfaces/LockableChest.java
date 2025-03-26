package xyz.memothelemo.lockedchests.interfaces;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LockableChest {
    @NotNull Boolean canInteract(World world, PlayerEntity player);
    @Nullable LockableChest getOtherSide();

    @NotNull Integer getTotalSlotsIdx();

    @Nullable String getChestOwnerUuid();
    void setChestOwner(@NotNull MinecraftServer server, @Nullable PlayerEntity player);

    void trustPlayer(@NotNull PlayerEntity player);
    void untrustPlayer(@NotNull PlayerEntity player);

    @NotNull String[] getTrustedPlayerIds();
    void setTrustedPlayerIds(@NotNull String[] players);

    Boolean isPlayerTrusted(@NotNull PlayerEntity player);
    void updateLockableData();
}
