package xyz.memothelemo.lockablechests.api.types;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Permissions {
    public static Codec<Permissions> CODEC = Codec.of(
            Codec.LONG.comap((v) -> v.value),
            Codec.LONG.map(Permissions::new)
    );

    public static @NotNull Permissions EMPTY = new Permissions(0);
    public static @NotNull Permissions OWNER = new Permissions(1 << 1);

    public static @NotNull Permissions VIEW_INVENTORY = new Permissions(1 << 3);
    public static @NotNull Permissions CHANGE_INVENTORY = new Permissions(1 << 4);

    public static @NotNull Permissions TRUSTED = Permissions.EMPTY
            .add(VIEW_INVENTORY)
            .add(CHANGE_INVENTORY);

    public static Permissions parseSelector(String input) {
        Permissions result = new Permissions(0);
        for (String segment : input.split(",")) {
            segment = segment.trim();
            result = switch (segment) {
                case "VIEW_INVENTORY" -> result.add(Permissions.VIEW_INVENTORY);
                case "CHANGE_INVENTORY" -> result.add(Permissions.CHANGE_INVENTORY);
                default -> result;
            };
        }
        return result;
    }

    public ArrayList<String> listParts() {
        ArrayList<String> list = new ArrayList<>();
        if (this.hasOwnerAccess()) {
            list.add("OWNER");
            return list;
        }

        if (this.has(Permissions.VIEW_INVENTORY))
            list.add("VIEW_INVENTORY");

        if (this.has(Permissions.CHANGE_INVENTORY))
            list.add("CHANGE_INVENTORY");

        return list;
    }

    public String toString() {
        return String.join(" | ", this.listParts());
    }

    public @NotNull Permissions add(@NotNull Permissions other) {
        return new Permissions(this.value | other.value);
    }

    public @NotNull Permissions remove(@NotNull Permissions other) {
        return new Permissions(this.value & ~other.value);
    }

    public boolean has(@NotNull Permissions required) {
        return this.hasOwnerAccess() || this.intersects(required);
    }

    public boolean isEmpty() {
        return this.value == 0;
    }

    public boolean hasOwnerAccess() {
        return this.intersects(Permissions.OWNER);
    }

    public boolean isTrusted() {
        return this.intersects(Permissions.TRUSTED);
    }

    //////////////////////////////////////////////////////////////////////////////////
    private long value = 0;
    private Permissions(long value) {
        this.value = value;
    }

    private boolean intersects(Permissions other) {
        return (this.value & other.value) == other.value;
    }
}
