package xyz.memothelemo.lockablechests.storage

import net.minecraft.server.level.ServerPlayer

/**
 * A general type that allows to store persistent server data.
 */
interface PersistentServerData {
    fun cachePlayer(player: ServerPlayer)
    fun invalidateCache()

    /**
     * Gets the cached username of the player by their UUID.
     */
    fun getCachedUsername(playerUuid: String): String?
    fun getPlayerUuidFromName(name: String): String?

    fun getJoinedPlayerNames(): List<String>
}
