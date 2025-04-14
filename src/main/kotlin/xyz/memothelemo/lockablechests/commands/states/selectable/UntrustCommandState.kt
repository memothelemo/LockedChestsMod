package xyz.memothelemo.lockablechests.commands.states.selectable

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.commands.states.StatefulCommand
import xyz.memothelemo.lockablechests.interfaces.LockableChest
import xyz.memothelemo.lockablechests.storage.ServerDataFactory

class UntrustCommandState(private val targetUuid: String): StatefulCommand {
    override fun onInvoke(player: ServerPlayer) {
        player.sendSystemMessage(ModUtil.render(ENTERED), false)
    }

    override fun onBlockClicked(
        level: Level,
        player: ServerPlayer,
        blockState: BlockState,
        blockPos: BlockPos
    ): Component {
        val entity = level.getBlockEntity(blockPos)
        if (entity !is ChestBlockEntity) {
            return Component.text()
                .append(Component
                    .text("This is not a chest. Please click a chest you like to untrust it.")
                    .color(NamedTextColor.RED))
                .appendNewline().appendNewline()
                .append(TIP).build()
        }

        val chest = entity as LockableChest
        val perms = chest.`lc$getAccessPermissions`(player)
        if (!perms.hasOwnerAccess())
            return ModUtil.makeCannotInteractMessage(
                player.server,
                chest.`lc$getData`().owner.get(),
                "untrust someone"
            )

        val removed = chest.`lc$untrustPlayer`(this.targetUuid)
        val targetPlayerName = {
            val data = ServerDataFactory.getPersistentData(player.server)
            data.getCachedUsername(this.targetUuid) ?: "<unknown>"
        }

        return if (removed) {
            Component.text("Successfully untrusted ")
                .append(Component
                    .text(targetPlayerName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("!"))
        } else {
            Component.text("You already untrusted ")
                .color(NamedTextColor.RED)
                .append(Component
                    .text(targetPlayerName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("!"))
        }
    }

    override fun onBlockDestroy(
        level: Level,
        player: ServerPlayer,
        blockState: BlockState,
        blockPos: BlockPos
    ): Boolean {
        return false
    }

    override fun onBlockInteract(
        level: Level,
        player: ServerPlayer,
        blockState: BlockState,
        blockPos: BlockPos
    ): InteractionResult {
        player.sendSystemMessage(ModUtil.render(INTERACT))
        return InteractionResult.FAIL
    }

    companion object {
        private val TIP = Component
            .text("You can exit select chest mode by running `/lc untrust` command again.")
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC)

        private val INTERACT = Component.text()
            .append(Component
                .text("Please click a chest you like to have someone untrust with it.")
                .color(NamedTextColor.RED))
            .appendNewline().appendNewline()
            .append(TIP).build()

        private val ENTERED = Component.text("Entered ")
            .append(
                Component.text("select chest mode. ")
                    .decorate(TextDecoration.BOLD)
                    .color(NamedTextColor.GOLD)
            )
            .append(Component.text("Click a chest you want to untrust a player with."))
            .appendNewline()
            .appendNewline()
            .append(TIP)
    }
}