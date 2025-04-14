package xyz.memothelemo.lockablechests.commands

import net.kyori.adventure.text.Component
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.fabric.actor.FabricCommandActor
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.commands.annotations.ContributorOnly
import xyz.memothelemo.lockablechests.commands.states.selectable.LockCommandState
import xyz.memothelemo.lockablechests.commands.states.selectable.UnlockCommandState

@Suppress("unused")
class OwnershipCommands {
    @Command("lc lock")
    @Description("Locks the chest by letting the player click the chest to unlock.")
    @ContributorOnly
    fun handleLockChest(actor: FabricCommandActor) {
        // If `/lc lock` is executed twice, then cancel it.
        val player = actor.requirePlayer()
        if (StatefulCommandManager.isCurrentState(player, LockCommandState::class.java)) {
            val message = ModUtil.render(Component.text("Exited lock chest mode"))
            actor.reply(message)
            StatefulCommandManager.clearState(player)
            return
        }
        StatefulCommandManager.getOrReplaceState(player, LockCommandState::class.java) { LockCommandState() }
    }

    @Command("lc unlock")
    @Description("Unlocks the chest by letting the player click the chest to lock.")
    fun handleUnlockChest(actor: FabricCommandActor) {
        // If `/lc unlock` is executed twice, then cancel it.
        val player = actor.requirePlayer()
        if (StatefulCommandManager.isCurrentState(player, UnlockCommandState::class.java)) {
            val message = ModUtil.render(Component.text("Exited unlock chest mode"))
            actor.reply(message)
            StatefulCommandManager.clearState(player)
            return
        }
        StatefulCommandManager.getOrReplaceState(player, UnlockCommandState::class.java) { UnlockCommandState() }
    }
}