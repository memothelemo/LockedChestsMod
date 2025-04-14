package xyz.memothelemo.lockablechests.commands.states.selectable

import com.mojang.datafixers.util.Either
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

open class ChestSelectorCommand(
    verb: String,
    command: String,
    youLikeClause: String
): StatefulCommand {
    protected val tipComponent = Component
        .text("You can exit $verb chest mode by running `/lc $command` command again.")
        .color(NamedTextColor.GRAY)
        .decorate(TextDecoration.ITALIC)

    private val interactComponent = Component.text()
        .append(Component
            .text("Please click a chest you like to $youLikeClause with it.")
            .color(NamedTextColor.RED))
        .appendNewline().appendNewline()
        .append(tipComponent).build()

    private val notChestComponent = Component.text()
        .append(Component
            .text("This is not a chest. Please click a chest you like to $verb it.")
            .color(NamedTextColor.RED))
        .appendNewline().appendNewline()
        .append(tipComponent)
        .build()

    protected var enteredComponent = Component.text("Entered ")
        .append(
            Component.text("$verb chest mode. ")
                .decorate(TextDecoration.BOLD)
                .color(NamedTextColor.GOLD)
        )
        .append(Component.text("Click a chest you want to $verb with."))
        .appendNewline()
        .appendNewline()
        .append(tipComponent)

    override fun onInvoke(player: ServerPlayer) {
        player.sendSystemMessage(ModUtil.render(this.enteredComponent), false)
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
        player.sendSystemMessage(ModUtil.render(this.interactComponent))
        return InteractionResult.FAIL
    }

    protected fun getBlockOnClicked(
        level: Level,
        blockPos: BlockPos
    ): Either<ChestBlockEntity, Component> {
        val entity = level.getBlockEntity(blockPos)
        return if (entity is ChestBlockEntity) {
            Either.left(entity)
        } else {
            Either.right(this.notChestComponent)
        }
    }
}