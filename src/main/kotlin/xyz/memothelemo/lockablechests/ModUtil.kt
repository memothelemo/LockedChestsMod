package xyz.memothelemo.lockablechests

import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.RegistryAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import xyz.memothelemo.lockablechests.api.types.ModActionResult
import xyz.memothelemo.lockablechests.storage.ServerDataFactory

object ModUtil {
    fun hasLuckPermsMod(): Boolean {
        return FabricLoader.getInstance().isModLoaded("luckperms")
    }

    fun render(component: Component): net.minecraft.network.chat.Component {
        // I assume nothing will go wrong with this statement here
        val json = GsonComponentSerializer.gson().serializeToTree(component)
        return net.minecraft.network.chat.Component.Serializer
            .fromJson(json, RegistryAccess.EMPTY)!!
    }

    fun makeCannotInteractMessage(server: MinecraftServer, result: ModActionResult): Component {
        return makeCannotInteractMessage(server, result.ownerUuid.get())
    }

    fun makeCannotInteractMessage(server: MinecraftServer, ownerUuid: String): Component {
        val data = ServerDataFactory.getPersistentData(server)
        val username = data.getCachedUsername(ownerUuid) ?: "<unknown>"
        return Component.text()
            .append(Component
                .text("You cannot interact with this chest. It is locked by ")
                .color(NamedTextColor.RED)
            )
            .append(Component.text(username).color(NamedTextColor.GOLD))
            .append(Component.text(".").color(NamedTextColor.RED))
            .build()
    }

    fun makeCannotInteractMessage(server: MinecraftServer, ownerUuid: String, action: String): Component {
        val data = ServerDataFactory.getPersistentData(server)
        val username = data.getCachedUsername(ownerUuid) ?: "<unknown>"
        return Component.text()
            .append(Component
                .text("You cannot $action with this chest. It is locked by ")
                .color(NamedTextColor.RED)
            )
            .append(Component.text(username).color(NamedTextColor.GOLD))
            .append(Component.text(".").color(NamedTextColor.RED))
            .build()
    }
}