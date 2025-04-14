package xyz.memothelemo.lockablechests.storage

import net.minecraft.server.MinecraftServer

object ServerDataFactory {
    fun getPersistentData(server: MinecraftServer)
        = NbtServerData.fromServer(server)
}
