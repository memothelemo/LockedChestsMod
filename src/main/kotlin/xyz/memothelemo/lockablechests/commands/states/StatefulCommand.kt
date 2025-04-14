package xyz.memothelemo.lockablechests.commands.states

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import xyz.memothelemo.lockablechests.ModLogger
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.api.types.ModActionResult
import xyz.memothelemo.lockablechests.storage.ServerDataFactory
import kotlin.jvm.optionals.getOrNull

interface StatefulCommand {
    fun onInvoke(player: ServerPlayer) {}
    fun onAbort(player: ServerPlayer) {}

    fun onBlockClicked(level: Level, player: ServerPlayer, blockState: BlockState, blockPos: BlockPos): Component? {
        return null
    }

    fun onBlockInteract(level: Level, player: ServerPlayer, blockState: BlockState, blockPos: BlockPos): InteractionResult {
        return InteractionResult.PASS
    }

    fun onBlockDestroy(level: Level, player: ServerPlayer, blockState: BlockState, blockPos: BlockPos): Boolean {
        return true
    }

    fun handleCommonModResult(player: ServerPlayer, result: ModActionResult): Component {
        if (result == ModActionResult.CANCELLED)
            return Component.text("Your request has been cancelled by the API")
                .color(NamedTextColor.RED)

        if (result is ModActionResult.OwnerOnly || result is ModActionResult.Restricted)
            return ModUtil.makeCannotInteractMessage(player.server, result)

        ModLogger.warn("[${this.javaClass.name}] Unhandled result! result = $result")
        return Component.text(
            "Unknown error occurred. Please contact the server "
                + "administrator to fix this problem.")
            .color(NamedTextColor.RED)
    }
}
