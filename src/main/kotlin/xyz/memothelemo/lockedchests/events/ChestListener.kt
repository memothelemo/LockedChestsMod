package xyz.memothelemo.lockedchests.events

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.kyori.adventure.text.Component
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.util.ActionResult
import xyz.memothelemo.lockedchests.GlobalState
import xyz.memothelemo.lockedchests.LockedChestsUtil
import xyz.memothelemo.lockedchests.interfaces.LockableChest

object ChestListener {
    fun listen() {
        OnChestAffectedByExplosion.EVENT.register { _, chest ->
            if ((chest as LockableChest).chestOwnerUuid != null) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        OnChestUpgrade.EVENT.register { _, player, entity ->
            val chest = entity as LockableChest;
            chest.updateLockableData();

            // silently fail if the player cannot interact with the lockable chest
            val isOwner = player.uuidAsString == chest.chestOwnerUuid;

            val owner = LockedChestsUtil.getOfflineUsername(player.world, chest)
            val message = Component.text()
                .content("You cannot upgrade this chest. It is locked by ")
                .append(Component.text(owner).color(LockedChestsUtil.REF_COLOR))
                .append(Component.text("."))
                .build()

            return@register if (isOwner || chest.chestOwnerUuid == null) {
                ActionResult.PASS
            } else {
                player.sendMessage(LockedChestsUtil.renderComponent(message), false)
                ActionResult.FAIL
            }
        }

        OnChestInventoryInteract.EVENT.register { world, player, chest ->
            // silently fail if the player cannot interact with the lockable chest
            return@register if ((chest as LockableChest).canInteract(world, player)) {
                ActionResult.PASS
            } else {
                ActionResult.FAIL
            }
        }

        UseBlockCallback.EVENT.register { player, world, _, result ->
            val block = world.getBlockEntity(result.blockPos)
            if (block !is ChestBlockEntity) return@register ActionResult.PASS

            val chest = block as LockableChest
            if (chest.canInteract(world, player)) return@register ActionResult.PASS

            val owner = LockedChestsUtil.getOfflineUsername(world, chest)
            val message = Component.text()
                .content("You cannot open this chest. It is locked by ")
                .append(Component.text(owner).color(LockedChestsUtil.REF_COLOR))
                .append(Component.text("."))
                .build()

            player.sendMessage(LockedChestsUtil.renderComponent(message), false)
            return@register ActionResult.FAIL
        }

        PlayerBlockBreakEvents.AFTER.register { world, player, _, _, entity ->
            if (entity != null) {
                val state = GlobalState.load(world.server!!)
                state.removeLockedChest(player.uuidAsString, entity.pos)
            }
        }

        PlayerBlockBreakEvents.BEFORE.register { world, player, _, _, entity ->
            if (entity !is ChestBlockEntity) return@register true

            val chest = entity as LockableChest
            if (chest.canInteract(world, player)) return@register true

            val owner = LockedChestsUtil.getOfflineUsername(world, chest)
            val message = Component.text()
                .content("You cannot break this chest. It is locked by ")
                .append(Component.text(owner).color(LockedChestsUtil.REF_COLOR))
                .append(Component.text("."))
                .build()

            player.sendMessage(LockedChestsUtil.renderComponent(message), false)
            return@register false
        }
    }
}
