package xyz.memothelemo.lockablechests.commands.states.selectable

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.interfaces.LockableChest
import xyz.memothelemo.lockablechests.storage.ServerDataFactory

class TrustCommandState(private val targetUuid: String)
    : ChestSelectorCommand("trust", "trust", "trust a player")
{
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
        val perms = chest.`lc$getAccessPermissions`(player)
        if (!perms.hasOwnerAccess())
            return ModUtil.makeCannotInteractMessage(
                player.server,
                chest.`lc$getData`().owner.get(),
                "trust someone"
            )

        val added = chest.`lc$trustPlayer`(this.targetUuid)
        val targetPlayerName = {
            val data = ServerDataFactory.getPersistentData(player.server)
            data.getCachedUsername(this.targetUuid) ?: "<unknown>"
        }

        return if (added) {
            Component.text("Successfully trusted ")
                .append(Component
                    .text(targetPlayerName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("!"))
        } else {
            Component.text("You already trusted ")
                .color(NamedTextColor.RED)
                .append(Component
                    .text(targetPlayerName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("!"))
        }
    }
}