package xyz.memothelemo.lockablechests.commands

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.fabric.actor.FabricCommandActor
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.commands.annotations.ContributorOnly
import xyz.memothelemo.lockablechests.commands.states.selectable.TrustCommandState
import xyz.memothelemo.lockablechests.commands.states.selectable.UntrustCommandState

@Suppress("unused")
class TrustCommands {
    @Command("lc trust <target>")
    @Description("Allows the other player to open a locked chest")
    @ContributorOnly
    fun trustPlayerToChest(actor: FabricCommandActor, target: ServerPlayer) {
        val player = actor.requirePlayer()
        if (player.uuid == target.uuid) {
            actor.error("You cannot trust yourself to your locked chests!")
            return
        }

        if (StatefulCommandManager.isCurrentState(player, TrustCommandState::class.java)) {
            val message = ModUtil.render(Component.text("Exited select chest mode"))
            actor.reply(message)
            StatefulCommandManager.clearState(player)
            return
        }

        StatefulCommandManager.getOrReplaceState(player, TrustCommandState::class.java) {
            TrustCommandState(target.stringUUID)
        }
    }

    @Command("lc untrust <target>")
    @Description("Forbids the other player to open a locked chest")
    fun untrustPlayerToChest(actor: FabricCommandActor, target: ServerPlayer) {
        val player = actor.requirePlayer()
        if (player.uuid == target.uuid) {
            actor.error("You cannot untrust yourself to your locked chests!")
            return
        }

        if (StatefulCommandManager.isCurrentState(player, TrustCommandState::class.java)) {
            val message = ModUtil.render(Component.text("Exited select chest mode"))
            actor.reply(message)
            StatefulCommandManager.clearState(player)
            return
        }

        StatefulCommandManager.getOrReplaceState(player, UntrustCommandState::class.java) {
            UntrustCommandState(target.stringUUID)
        }
    }
}