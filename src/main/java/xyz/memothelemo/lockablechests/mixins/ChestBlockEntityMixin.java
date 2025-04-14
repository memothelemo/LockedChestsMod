package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockablechests.ModLogger;
import xyz.memothelemo.lockablechests.api.events.LockableChestCallback;
import xyz.memothelemo.lockablechests.api.types.ModActionResult;
import xyz.memothelemo.lockablechests.api.types.Permissions;
import xyz.memothelemo.lockablechests.interfaces.LockableChest;
import xyz.memothelemo.lockablechests.types.LegacyLCData;
import xyz.memothelemo.lockablechests.types.NewLCData;

import java.util.Optional;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityMixin implements LockableChest {
    @Unique private NewLCData lc$data = NewLCData.Companion.empty(this.lc$getBlockPos());

    @Override
    public @NotNull NewLCData lc$getData() {
        return this.lc$data;
    }

    @Override
    public void lc$overrideData(NewLCData data) {
        this.lc$data.overrideFields(data);
        ((ChestBlockEntity) (Object) this).setChanged();
    }

    @Override
    public boolean lc$isPlayerViewing(@NotNull ServerPlayer player) {
        if (!(player.containerMenu instanceof ChestMenu menu))
            return false;

        Container container = menu.getContainer();
        return container == this || container instanceof CompoundContainer
                && ((CompoundContainer) container).contains((Container) this);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    public void loadLCData(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ModLogger.INSTANCE.debug("Loading lockable chest " + this.lc$getBlockPos() + "'s data");

        NewLCData data;
        if (LegacyLCData.Companion.hasLegacyFields(tag)) {
            ModLogger.INSTANCE.info("Chest " + this.lc$getBlockPos() + " has legacy lockable chest data, migrating to new schema...");

            LegacyLCData legacy = LegacyLCData.Companion.fromNbt(tag, registries);
            data = NewLCData.Companion.fromLegacy(legacy, this.lc$getBlockPos());
            LegacyLCData.Companion.deleteLegacyFields(tag);

            ModLogger.INSTANCE.info("Chest " + this.lc$getBlockPos() + " successfully migrated to new data schema");
        } else {
            data = NewLCData.Companion.fromNbt(this.lc$getBlockPos(), tag, registries);
            if (data.position != this.lc$getBlockPos()) {
                ModLogger.INSTANCE.debug("Lockable chest of " + this.lc$data.position + "'s position changed!");
                data.position = this.lc$getBlockPos();
            }
        }

        this.lc$data = data;
        ((BlockEntity) (Object) this).setChanged();
        ModLogger.INSTANCE.debug("Loaded lockable chest data");
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    public void saveLCData(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.lc$data.saveWithNbt(tag, registries);
        ModLogger.INSTANCE.debug("Saved LockableChests data from chest " + this.lc$getBlockPos());
    }

    @Unique @Override
    public @Nullable ChestBlockEntity lc$getOtherSide() {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        Direction facing = ChestBlock.getConnectedDirection(chest.getBlockState());
        ChestType chestType = chest.getBlockState().getValue(ChestBlock.TYPE);

        ChestBlockEntity other = null;
        if (chestType != ChestType.SINGLE && chest.hasLevel()) {
            Level level = chest.getLevel();
            assert level != null;

            BlockPos otherPos = chest.getBlockPos().relative(facing);
            if (level.getBlockEntity(otherPos) instanceof ChestBlockEntity loaded)
                other = loaded;
        }

        return other;
    }

    @Unique @Override
    public @NotNull BlockPos lc$getBlockPos() {
        return ((BlockEntity) (Object) this).getBlockPos();
    }

    /////// LOCKABLE CHEST RELATED FUNCTIONS ///////
    @Override
    public @NotNull Permissions lc$getAccessPermissions(@NotNull ServerPlayer player) {
        return lc$getAccessPermissions(player, false);
    }

    @Override
    public @NotNull Permissions lc$getAccessPermissions(@NotNull String playerUuid) {
        return lc$getAccessPermissions(playerUuid, false);
    }

    @Override
    public @NotNull Permissions lc$getAccessPermissions(@NotNull ServerPlayer player, boolean actual) {
        return lc$getAccessPermissions(player.getStringUUID(), actual);
    }

    @Override
    public @NotNull Permissions lc$getAccessPermissions(@NotNull String playerUuid, boolean actual) {
        if (this.lc$data.owner.isEmpty())
            return actual ? Permissions.EMPTY : Permissions.OWNER;

        String owner = this.lc$data.owner.get();
        if (owner.equals(playerUuid)) return Permissions.OWNER;

        NewLCData.PlayerEntry entry = this.lc$data.players.get(playerUuid);
        if (entry == null)
            return this.lc$data.defaultPerms;

        return entry.isTrusted
                ? Permissions.TRUSTED
                : entry.override.orElse(Permissions.EMPTY);
    }

    @Unique @Override
    public @NotNull ModActionResult lc$lockChest(@NotNull ServerPlayer player) {
        if (this.lc$data.owner.isPresent()) {
            String owner = this.lc$data.owner.get();
            if (owner.equals(player.getStringUUID())) {
                return ModActionResult.ALREADY_LOCKED;
            } else {
                return new ModActionResult.OwnerOnly(owner);
            }
        }

        InteractionResult result = LockableChestCallback.LOCKED
                .invoker()
                .onChestLocked(player.level(), player, this);

        if (result != InteractionResult.PASS)
            return ModActionResult.CANCELLED;

        this.lc$data.owner = Optional.of(player.getStringUUID());
        this.lc$updateToOtherSide();
        ((ChestBlockEntity) (Object) this).setChanged();

        return ModActionResult.DONE;
    }

    @Override
    public @NotNull ModActionResult lc$unlockChest(@NotNull ServerPlayer player) {
        if (this.lc$data.owner.isEmpty())
            return ModActionResult.ALREADY_UNLOCKED;

        String oldOwner = this.lc$data.owner.get();
        if (!oldOwner.equals(player.getStringUUID()))
            return new ModActionResult.OwnerOnly(oldOwner);

        InteractionResult result = LockableChestCallback.UNLOCKED
                .invoker()
                .onChestUnlock(player.level(), player, this);

        if (result != InteractionResult.PASS)
            return ModActionResult.CANCELLED;

        this.lc$data.clear();
        this.lc$updateToOtherSide(false);
        ((ChestBlockEntity) (Object) this).setChanged();

        return ModActionResult.DONE;
    }

    @Override
    public boolean lc$trustPlayer(@NotNull ServerPlayer player) {
        return lc$trustPlayer(player.getStringUUID());
    }

    @Override
    public boolean lc$untrustPlayer(@NotNull ServerPlayer player) {
        return lc$untrustPlayer(player.getStringUUID());
    }

    @Override
    public boolean lc$trustPlayer(@NotNull String playerUuid) {
        if (this.lc$data.players.get(playerUuid) != null)
            return false;

        NewLCData.PlayerEntry entry = new NewLCData.PlayerEntry();
        entry.isTrusted = true;
        this.lc$data.players.put(playerUuid, entry);
        this.lc$updateToOtherSide();
        return true;
    }

    @Override
    public boolean lc$untrustPlayer(@NotNull String playerUuid) {
        if (this.lc$data.players.get(playerUuid) == null)
            return false;

        this.lc$data.players.remove(playerUuid);
        this.lc$updateToOtherSide();
        return true;
    }

    @Unique
    private void lc$updateToOtherSide(boolean expectNewOwner) {
        ModLogger.INSTANCE.debug("Updating chest data to the other side from position " + this.lc$getBlockPos());

        ChestBlockEntity other = this.lc$getOtherSide();
        if (other == null) {
            ModLogger.INSTANCE.debug("This chest " + this.lc$getBlockPos() + " is not a double chest");
            return;
        }

        NewLCData source;
        NewLCData mutable;

        Optional<String> thisOwner = this.lc$data.owner;
        Optional<String> otherOwner = ((LockableChest) other).lc$getData().owner;
        if (thisOwner.isPresent() && otherOwner.isPresent() && !thisOwner.equals(otherOwner)) {
            ModLogger.INSTANCE.debug("Chest " + this.lc$getBlockPos()
                    + " has conflicting ownership from left and right chests!");
            return;
        }

        if (!expectNewOwner || thisOwner.isPresent() && otherOwner.isEmpty()) {
            source = this.lc$data;
            mutable = ((LockableChest) other).lc$getData();
        } else if (thisOwner.isEmpty() && otherOwner.isPresent()) {
            source = ((LockableChest) other).lc$getData();
            mutable = this.lc$data;
        } else return;

        mutable.overrideFields(source);
        ModLogger.INSTANCE.debug("Successfully updated lockable chest data from the other side "
                + this.lc$getBlockPos());
    }

    @Unique @Override
    public void lc$updateToOtherSide() {
        lc$updateToOtherSide(true);
    }
}
