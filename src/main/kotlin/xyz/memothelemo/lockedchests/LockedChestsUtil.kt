package xyz.memothelemo.lockedchests

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.luckperms.api.LuckPermsProvider
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import xyz.memothelemo.lockedchests.interfaces.LockableChest
import java.util.UUID

object LockedChestsUtil {
    private const val UNKNOWN_NAME = "<unknown>";

    // Dystopia exclusive usage :)
    fun canUseThisMod(player: ServerPlayerEntity): Boolean {
        val luckPerms = LuckPermsProvider.get()
        val user = luckPerms.getPlayerAdapter(ServerPlayerEntity::class.java).getUser(player)

        return user.cachedData.permissionData
            .checkPermission("group.ds2.contributors")
            .asBoolean()
    }

    val REF_COLOR = NamedTextColor.GOLD;
    val GREEN_COLOR = NamedTextColor.GREEN;

    fun raycastChest(player: PlayerEntity): ChestBlockEntity? {
        val result = player.raycast(5.0, 0.0f, false);
        if (result !is BlockHitResult) return null

        val chest = player.world.getBlockEntity(result.blockPos)
        return if (chest is ChestBlockEntity) {
            chest
        } else {
            null
        }
    }

    fun renderComponent(component: Component): Text {
        // I assume nothing will go wrong with this statement here
        val json = GsonComponentSerializer.gson().serializeToTree(component)
        return Text.Serialization.fromJsonTree(json, DynamicRegistryManager.EMPTY)!!
    }

    fun getOfflineUsername(world: World, chest: LockableChest): String {
        val owner = chest.chestOwnerUuid ?: return UNKNOWN_NAME
        return getOfflineUsername(world, owner);
    }

    fun getOfflineUsername(world: World, playerId: String): String {
        val server = world.server ?: return UNKNOWN_NAME;
        return getOfflineUsername(server, playerId);
    }

    fun getOfflineUsername(server: MinecraftServer, playerId: String): String {
        try {
            val uuid = UUID.fromString(playerId);
            return getOfflineUsername(server, uuid);
        } catch (_: IllegalArgumentException) {
            return UNKNOWN_NAME;
        }
    }

    fun getOfflineUsername(server: MinecraftServer, playerUuid: UUID): String {
        // Maybe they're still around, try to get their recent offline username.
        val fromPlayerList = server.playerManager.playerList.stream()
            .filter { plr -> plr.uuid == playerUuid }
            .findFirst();

        if (fromPlayerList.isPresent)
            return fromPlayerList.get().name.string;

        // Last resort, from the global state
        val state = GlobalState.load(server)
        return state.getCachedPlayerName(playerUuid.toString()) ?: UNKNOWN_NAME
    }
}