package xyz.memothelemo.lockablechests.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.fabric.actor.FabricCommandActor
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.api.types.Permissions
import xyz.memothelemo.lockablechests.commands.annotations.JoinedPlayers
import xyz.memothelemo.lockablechests.commands.states.selectable.SetDefaultPermsCommandState
import xyz.memothelemo.lockablechests.commands.states.selectable.ViewPermsCommandState
import xyz.memothelemo.lockablechests.storage.ServerDataFactory
import java.util.UUID

@Command("lc perms")
@Description("Permission related commands for chests")
@Suppress("unused")
class PermissionCommands {
    @Subcommand("default <permissions>")
    @Description("Sets default permissions for everyone not including overridden and trusted players")
    fun setDefaultPerms(actor: FabricCommandActor, @Optional permissions: String?) {
        val sender = actor.requirePlayer()
        val parsed = Permissions.parseSelector(permissions ?: "")

        if (StatefulCommandManager.isCurrentState(sender, SetDefaultPermsCommandState::class.java)) {
            val message = ModUtil.render(Component.text("Exited set default permissions chest mode"))
            actor.reply(message)
            StatefulCommandManager.clearState(sender)
            return
        }

        if (parsed.isEmpty) {
            val component = Component.text()
                .append(Component.text("Please specify what permissions do you want to set for everyone"
                        + " not including players you set manually or you trust.")
                    .color(NamedTextColor.RED)
                )
                .appendNewline()
                .appendNewline()
                .append(Component.text("LEGEND:").decorate(TextDecoration.BOLD))
                .appendNewline()

            Permissions.TRUSTED.listParts().forEach {
                component.append(Component.text("- $it").color(NamedTextColor.GRAY))
                    .appendNewline()
            }

            component.appendNewline()
                .append(Component.text("You can set multiple permissions with for example: "))
                .append(Component.text("VIEW_INVENTORY,CHANGE_INVENTORY").color(NamedTextColor.GRAY))

            actor.sendRawMessage(ModUtil.render(component.build()))
            return
        }

        StatefulCommandManager.getOrReplaceState(sender, SetDefaultPermsCommandState::class.java) {
            SetDefaultPermsCommandState(parsed)
        }
    }

    @Subcommand("view <player>")
    @Description("Gets player's permission from a clicked chest")
    fun getPermsForPlayer(actor: FabricCommandActor, @JoinedPlayers player: String) {
        val sender = actor.requirePlayer()
        var targetUuid = player
        if (!isUuid(player)) {
            val data = ServerDataFactory.getPersistentData(sender.server)
            val uuid = data.getPlayerUuidFromName(player)
            if (uuid == null) {
                actor.error("This player have not joined the server before. Please "
                    + "check their name and run the command again.")
                return
            }
            targetUuid = uuid
        }

        val currentState = StatefulCommandManager.getCurrentState(sender)
        if (currentState is ViewPermsCommandState && currentState.targetUuid == targetUuid) {
            val message = ModUtil.render(Component.text("Exited view permissions mode"))
            actor.reply(message)
            StatefulCommandManager.clearState(sender)
            return
        }

        StatefulCommandManager.getOrReplaceState(sender, ViewPermsCommandState::class.java) {
            ViewPermsCommandState(targetUuid, sender.server)
        }
    }

    private fun isUuid(value: String): Boolean {
        try {
            UUID.fromString(value)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}