package xyz.memothelemo.lockablechests.api.types;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import xyz.memothelemo.lockablechests.storage.PersistentServerData;

@SuppressWarnings("unused")
public interface ModActionResult {
    @NotNull ModActionResult DONE = new Done();
    @NotNull ModActionResult CANCELLED = new Cancelled();
    @NotNull ModActionResult ALREADY_LOCKED = new AlreadyLocked();
    @NotNull ModActionResult ALREADY_UNLOCKED = new AlreadyUnlocked();

    /**
     * Gets the offline username of the specified UUID.
     */
    default Optional<String> getOfflineOwnerName(PersistentServerData data) {
        if (this.getOwnerUuid().isEmpty())
            return Optional.empty();

        String uuid = this.getOwnerUuid().get();
        String cached = data.getCachedUsername(uuid);
        if (cached == null) cached = "<unknown>";

        return Optional.of(cached);
    }

    /**
     * Gets the chest owner's cached username from the
     * persistent server data.
     * <p>
     * It will return an optional empty value if the error has
     * no chest owner metadata.
     */
    default @NotNull Optional<String> getOwnerUuid() {
        return Optional.empty();
    }

    /**
     * Whether the result is considered as `"cancelled"`.
     */
    default boolean isCancelled() {
        return false;
    }

    /**
     * Whether the result cannot be performed because maybe the player
     * has no permissions or not the owner of the chest that they
     * tried to perform restricted operations.
     */
    default boolean isRestricted() {
        return false;
    }

    class Done implements ModActionResult {}
    class AlreadyLocked implements ModActionResult {}
    class AlreadyUnlocked implements ModActionResult {}

    class Cancelled implements ModActionResult {
        @Override
        public boolean isCancelled() {
            return true;
        }
    }

    class OwnerOnly implements ModActionResult {
        private final String ownerUuid;

        public OwnerOnly(@NotNull String ownerUuid) {
            this.ownerUuid = ownerUuid;
        }

        @Override
        public @NotNull Optional<String> getOwnerUuid() {
            return Optional.of(this.ownerUuid);
        }

        @Override
        public boolean isRestricted() {
            return true;
        }
    }

    class Restricted implements ModActionResult {
        private final String ownerUuid;

        public Restricted(@NotNull String ownerUuid) {
            this.ownerUuid = ownerUuid;
        }

        @Override
        public @NotNull Optional<String> getOwnerUuid() {
            return Optional.of(this.ownerUuid);
        }

        @Override
        public boolean isRestricted() {
            return true;
        }
    }
}
