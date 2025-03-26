package xyz.memothelemo.lockedchests.mixins;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockedchests.GlobalState;
import xyz.memothelemo.lockedchests.interfaces.LockableChest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

@Mixin(ChestBlockEntity.class)
public abstract class LockedChestsMixin implements LockableChest {
    @Shadow public abstract int size();

    @Unique private final static String NBT_OWNER_KEY = "lockedchests.owner";
    @Unique private final static String NBT_TRUSTED_PLAYERS_KEY = "lockedchests.trusted_players";

    @Unique private @Nullable String ownerId = null;
    @Unique private @Nullable ArrayList<String> trustedPlayerIds = null;

    @NotNull
    @Unique
    public Boolean canInteract(World world, PlayerEntity player) {
        String ownerUuid = this.getChestOwnerUuid();
        return ownerUuid == null
                || ownerUuid.equals(player.getUuidAsString())
                || this.isPlayerTrusted(player);
    }

    @Nullable
    @Unique
    public String getChestOwnerUuid() {
        return this.ownerId;
    }

    @Unique
    public @NotNull Integer getTotalSlotsIdx() {
        int totalSlots = this.size();
        if (this.getOtherSide() != null) {
            totalSlots = totalSlots * 2;
        }
        assert totalSlots > 0;
        return totalSlots - 1;
    }

    @Unique
    public void setChestOwner(@NotNull MinecraftServer server, @Nullable PlayerEntity player) {
        String oldOwner = this.ownerId;
        this.ownerId = player != null ? player.getUuidAsString() : null;

        LockedChestsMixin otherSide = (LockedChestsMixin) this.getOtherSide();
        if (otherSide != null) {
            otherSide.ownerId = this.ownerId;
        }

        GlobalState state = GlobalState.Companion.load(server);
        ChestBlockEntity entity = (ChestBlockEntity) (Object) this;

        if (oldOwner != null) {
            state.removeLockedChest(oldOwner, entity.getPos());
        }

        if (player == null) {
            this.trustedPlayerIds = null;
            this.ownerId = null;
        } else {
            state.addLockedChest(player.getUuidAsString(), entity.getPos());
            this.trustedPlayerIds = new ArrayList<>();
        }
    }

    @Unique
    public void trustPlayer(@NotNull PlayerEntity player) {
        if (this.trustedPlayerIds == null) return;
        this.trustedPlayerIds.add(player.getUuidAsString());

        LockedChestsMixin otherSide = (LockedChestsMixin) this.getOtherSide();
        if (otherSide != null) {
            assert otherSide.trustedPlayerIds != null;
            otherSide.trustedPlayerIds.add(player.getUuidAsString());
        }
    }

    @Unique
    public void untrustPlayer(@NotNull PlayerEntity player) {
        if (this.trustedPlayerIds == null) return;
        this.trustedPlayerIds.remove(player.getUuidAsString());

        LockedChestsMixin otherSide = (LockedChestsMixin) this.getOtherSide();
        if (otherSide != null) {
            assert otherSide.trustedPlayerIds != null;
            otherSide.trustedPlayerIds.remove(player.getUuidAsString());
        }
    }

    @Override
    @Unique
    public @NotNull String[] getTrustedPlayerIds() {
        return Objects.requireNonNullElseGet(
                this.trustedPlayerIds,
                () -> new ArrayList<String>()
        ).toArray(new String[0]);
    }

    @Override
    @Unique
    public void setTrustedPlayerIds(@NotNull String[] players) {
        ArrayList<String> newData = new ArrayList<>(players.length);
        newData.addAll(Arrays.asList(players));
        this.trustedPlayerIds = newData;

        LockedChestsMixin otherSide = (LockedChestsMixin) this.getOtherSide();
        if (otherSide != null) otherSide.trustedPlayerIds = newData;
    }

    @Override
    @Unique
    public Boolean isPlayerTrusted(@NotNull PlayerEntity player) {
        if (this.trustedPlayerIds == null) {
            return false;
        } else {
            return this.trustedPlayerIds.contains(player.getUuidAsString());
        }
    }

    // Gets the other side of the double chests
    @Unique
    public @Nullable LockableChest getOtherSide() {
        ChestBlockEntity chest = (ChestBlockEntity) ((Object) this);
        Direction facing = ChestBlock.getFacing(chest.getCachedState());
        ChestType chestType = chest.getCachedState().get(ChestBlock.CHEST_TYPE);

        LockableChest otherSide = null  ;
        if (chestType != ChestType.SINGLE && chest.hasWorld()) {
            World world = chest.getWorld();
            assert world != null;
            if (world.getBlockEntity(chest.getPos().offset(facing)) instanceof ChestBlockEntity sideEntity) {
                otherSide = (LockableChest) (Object) sideEntity;
            }
        }

        return otherSide;
    }

    @Unique
    public void updateLockableData() {
        LockedChestsMixin other = (LockedChestsMixin) this.getOtherSide();

        // Maybe the other side has one (if it exists)
        if (this.ownerId == null) {
            // If the other side has an owner, call that function instead.
            if (other != null && other.ownerId != null)
                other.updateLockableData();

            return;
        }

        // Overriding data to the other side, internally.
        if (other == null) return;

        other.ownerId = this.ownerId;
        other.trustedPlayerIds = this.trustedPlayerIds;

        ((ChestBlockEntity)(Object) other).markDirty();
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readCustomData(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        if (nbt.contains(NBT_OWNER_KEY)) {
            this.ownerId = nbt.getString(NBT_OWNER_KEY);
            // 1.21.5:
            // this.ownerId = nbt.getString(NBT_OWNER_KEY, "");
        }

        if (nbt.contains(NBT_TRUSTED_PLAYERS_KEY)) {
            // 1.21.5:
            NbtList list = nbt.getList(NBT_TRUSTED_PLAYERS_KEY, NbtElement.STRING_TYPE);
            // NbtList list = nbt.getListOrEmpty(NBT_TRUSTED_PLAYERS_KEY);

            ArrayList<String> newData = new ArrayList<>(list.size());
            list.stream()
                    .filter(nbtElement -> nbtElement instanceof NbtString)
                    .map(NbtElement::asString)
                    .forEach(newData::add);
            // 1.21.5: .forEach(e -> e.ifPresent(newData::add));

            this.trustedPlayerIds = newData;
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeCustomData(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        if (this.ownerId != null) {
            nbt.put(NBT_OWNER_KEY, NbtString.of(this.ownerId));
        } else {
            nbt.remove(NBT_OWNER_KEY);
        }

        if (this.trustedPlayerIds != null) {
            NbtList newList = new NbtList();
            this.trustedPlayerIds.forEach(element -> newList.add(NbtString.of(element)));
            nbt.put(NBT_TRUSTED_PLAYERS_KEY, newList);
        } else {
            nbt.remove(NBT_TRUSTED_PLAYERS_KEY);
        }
    }
}
