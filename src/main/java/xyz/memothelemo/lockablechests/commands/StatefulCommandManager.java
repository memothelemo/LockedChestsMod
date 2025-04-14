package xyz.memothelemo.lockablechests.commands;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.memothelemo.lockablechests.commands.states.StatefulCommand;

import java.util.HashMap;
import java.util.function.Supplier;

public class StatefulCommandManager {
    // We're using player's ID instead of ServerPlayer because it can memory leak!
    private static final HashMap<String, StatefulCommand> playerStates = new HashMap<>();

    public static <T extends StatefulCommand> @NotNull Boolean isCurrentState(ServerPlayer player, Class<T> clazz) {
        return clazz.isInstance(getCurrentState(player));
    }

    public static @Nullable StatefulCommand getCurrentState(ServerPlayer player) {
        return playerStates.get(player.getStringUUID());
    }

    @SuppressWarnings("unchecked")
    public static <T extends StatefulCommand> @NotNull T getOrReplaceState(
            ServerPlayer player,
            Class<T> clazz,
            Supplier<T> supplier
    ) {
        StatefulCommand state = playerStates.get(player.getStringUUID());
        if (clazz.isInstance(state))
            return (T) (Object) state;

        T newState = supplier.get();
        newState.onInvoke(player);
        playerStates.put(player.getStringUUID(), newState);

        return newState;
    }

    public static void clearState(ServerPlayer player) {
        StatefulCommand previous = playerStates.remove(player.getStringUUID());
        if (previous != null) previous.onAbort(player);
    }
}
