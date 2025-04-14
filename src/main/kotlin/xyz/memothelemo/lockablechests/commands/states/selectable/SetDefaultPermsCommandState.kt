package xyz.memothelemo.lockablechests.commands.states.selectable

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.api.types.Permissions
import xyz.memothelemo.lockablechests.interfaces.LockableChest

class SetDefaultPermsCommandState(private val targetPerms: Permissions)
    : ChestSelectorCommand(
    "set default permissions",
    "perms default",
    "set default permissions for everyone (not including trusted and manually set)"
    )
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
        val actorPerms = chest.`lc$getAccessPermissions`(player)

        if (!actorPerms.hasOwnerAccess())
            return ModUtil.makeCannotInteractMessage(
                player.server,
                chest.`lc$getData`().owner.get(),
                "set default permissions"
            )

        chest.`lc$getData`().defaultPerms = targetPerms
        chest.`lc$updateToOtherSide`()
        entity.setChanged()

        return Component.text()
            .append(Component.text("Successfully set default permissions for a chest at "))
            .append(Component.text("${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                .color(NamedTextColor.GOLD)
            )
            .append(Component.text("."))
            .build()
    }
}