package xyz.memothelemo.lockablechests.commands.states.selectable

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.api.types.Permissions
import xyz.memothelemo.lockablechests.interfaces.LockableChest
import xyz.memothelemo.lockablechests.storage.ServerDataFactory

class ViewPermsCommandState(val targetUuid: String, server: MinecraftServer)
    : ChestSelectorCommand("view permissions", "perms view <same player>", "view permissions from a player")
{
    private val cachedUsername = ServerDataFactory
        .getPersistentData(server)
        .getCachedUsername(targetUuid)
        ?: "<unknown>"

    init {
        this.enteredComponent = Component.text("Entered view permissions chest mode for ")
            .append(
                Component.text("$cachedUsername. ")
                    .decorate(TextDecoration.BOLD)
                    .color(NamedTextColor.GOLD)
            )
            .append(Component.text("Click a chest you want to view permissions with."))
            .appendNewline()
            .appendNewline()
            .append(tipComponent)
    }

    override fun onBlockClicked(
        level: Level,
        player: ServerPlayer,
        blockState: BlockState,
        blockPos: BlockPos
    ): Component {
        val maybeEntity = this.getBlockOnClicked(level, blockPos)
        if (maybeEntity.left().isEmpty)
            return maybeEntity.right().get()

        val entity = maybeEntity.left().get()
        val chest = entity as LockableChest
        val actorPerms = chest.`lc$getAccessPermissions`(player)

        // If it has no owner, then throw it out!
        if (chest.`lc$getData`().owner.isEmpty)
            return Component.text("This chest has no owner yet.")
                .color(NamedTextColor.RED)

        if (!actorPerms.hasOwnerAccess())
            return ModUtil.makeCannotInteractMessage(
                player.server,
                chest.`lc$getData`().owner.get(),
                "view someone's permissions on a chest"
            )

        var composed = Component.text()
            .append(Component.text("Chest permissions for "))
            .append(Component.text(this.cachedUsername)
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
            )
            .append(Component.text(" at "))
            .append(Component.text("${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                .color(NamedTextColor.GOLD)
            )
            .append(Component.text(":"))

        val actualPerms = chest.`lc$getAccessPermissions`(targetUuid, true)
        if (actualPerms != Permissions.EMPTY) composed = composed.appendNewline()

        actualPerms.listParts().forEach {
            composed = composed
                .append(Component.text("- ").color(NamedTextColor.GRAY))
                .append(Component.text(it).color(NamedTextColor.GOLD))
        }

        return composed.build()
    }
}