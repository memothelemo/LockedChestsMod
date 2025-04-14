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
import xyz.memothelemo.lockablechests.api.types.ModActionResult
import xyz.memothelemo.lockablechests.commands.states.StatefulCommand
import xyz.memothelemo.lockablechests.interfaces.LockableChest

class UnlockCommandState: StatefulCommand {
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
                    .text("This is not a chest. Please click a chest you like to unlock it.")
                    .color(NamedTextColor.RED))
                .appendNewline().appendNewline()
                .append(TIP).build()
        }

        val chest = entity as LockableChest
        when (val result = chest.`lc$unlockChest`(player)) {
            is ModActionResult.Done -> {}
            is ModActionResult.AlreadyUnlocked -> return Component.text()
                .append(Component.text("This chest is already unlocked!").color(NamedTextColor.RED))
                .appendNewline()
                .appendNewline().append(TIP)
                .build()
            else -> return this.handleCommonModResult(player, result)
        }

        return Component.text("Successfully unlocked a chest at ")
            .append(Component.text("[${blockPos.x}, ${blockPos.y}, ${blockPos.z}]").color(NamedTextColor.GOLD))
            .append(Component.text("!"))
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
        private val TIP = Component.text()
            .append(Component.text("TIP: ").decorate(TextDecoration.BOLD))
            .append(Component.text("You can exit unlock chest mode by running `/lc unlock` command again.")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC))

        private val INTERACT = Component.text()
            .append(Component
                .text("Please click a chest you like to unlock it.")
                .color(NamedTextColor.RED))
            .appendNewline().appendNewline()
            .append(TIP).build()

        private val ENTERED = Component.text("Entered ")
            .append(
                Component.text("unlock chest mode. ")
                .decorate(TextDecoration.BOLD)
                .color(NamedTextColor.GOLD)
            )
            .append(Component.text("Click a chest you want to unlock."))
            .appendNewline()
            .appendNewline()
            .append(TIP)
    }
}
