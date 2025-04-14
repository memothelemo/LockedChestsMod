package xyz.memothelemo.lockablechests

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import xyz.memothelemo.lockablechests.api.types.Permissions
import xyz.memothelemo.lockablechests.events.BlockExplodeCallback
import xyz.memothelemo.lockablechests.events.BlockInteractCallback
import xyz.memothelemo.lockablechests.events.BlockPlaceCallback
import xyz.memothelemo.lockablechests.events.ChestInventoryChangeCallback
import xyz.memothelemo.lockablechests.events.HopperEventCallback
import xyz.memothelemo.lockablechests.events.LCPlayerEvents
import xyz.memothelemo.lockablechests.interfaces.LockableChest
import xyz.memothelemo.lockablechests.storage.ServerDataFactory

object ModListener:
    BlockExplodeCallback,
    BlockPlaceCallback,
    BlockInteractCallback,
    ChestInventoryChangeCallback,
    HopperEventCallback.Eject,
    HopperEventCallback.Suck,
    PlayerBlockBreakEvents.Before,
    LCPlayerEvents.Join
{
    fun initialize() {
        BlockExplodeCallback.EVENT.register(this)
        BlockPlaceCallback.EVENT.register(this)
        BlockInteractCallback.EVENT.register(this)
        ChestInventoryChangeCallback.EVENT.register(this)
        HopperEventCallback.EJECT.register(this)
        HopperEventCallback.SUCK.register(this)
        PlayerBlockBreakEvents.BEFORE.register(this)
        LCPlayerEvents.JOIN.register(this)
    }

    override fun onPlayerJoin(player: ServerPlayer) {
        ServerDataFactory.getPersistentData(player.server).cachePlayer(player)
    }

    override fun shouldExplodeBlock(level: ServerLevel, blockState: BlockState, blockPos: BlockPos): InteractionResult {
        val chest = getLockableChest(level, blockPos) ?: return InteractionResult.PASS
        return if (chest.`lc$getData`().owner.isPresent) {
            InteractionResult.FAIL
        } else {
            InteractionResult.PASS
        }
    }

    override fun onPlaceBlock(
        level: Level,
        player: ServerPlayer,
        stack: ItemStack,
        blockState: BlockState,
        pos: BlockPos
    ): InteractionResult {
        val maybeNewChestSide = getLockableChest(level, pos) ?: return InteractionResult.PASS
        val otherSide = maybeNewChestSide.`lc$getOtherSide`() as LockableChest?
            ?: return InteractionResult.PASS

        if (!otherSide.`lc$getAccessPermissions`(player).hasOwnerAccess()) {
            val component = ModUtil.makeCannotInteractMessage(
                player.server,
                otherSide.`lc$getData`().owner.get(),
                "upgrade"
            )
            player.sendSystemMessage(ModUtil.render(component), false)
            return InteractionResult.FAIL;
        }

        otherSide.`lc$updateToOtherSide`()
        return InteractionResult.PASS
    }

    override fun onBlockInteract(
        level: Level,
        player: ServerPlayer,
        hand: InteractionHand,
        stack: ItemStack,
        blockState: BlockState,
        blockPos: BlockPos
    ): InteractionResult? {
        val lockable = getLockableChest(level, blockPos) ?: return InteractionResult.PASS
        val perms = lockable.`lc$getAccessPermissions`(player)
        return if (perms.has(Permissions.VIEW_INVENTORY)) {
            InteractionResult.PASS
        } else {
            val component = ModUtil.makeCannotInteractMessage(
                player.server,
                lockable.`lc$getData`().owner.get(),
            )
            player.sendSystemMessage(ModUtil.render(component), false)
            InteractionResult.FAIL
        }
    }

    override fun onChestInventoryChanged(
        level: Level,
        player: ServerPlayer,
        chest: ChestBlockEntity
    ): InteractionResult {
        val lockable = chest as LockableChest
        val perms = lockable.`lc$getAccessPermissions`(player)
        return if (perms.has(Permissions.CHANGE_INVENTORY)) {
            InteractionResult.PASS
        } else {
            val component = ModUtil.makeCannotInteractMessage(
                player.server,
                chest.`lc$getData`().owner.get(),
            )
            player.sendSystemMessage(ModUtil.render(component), false)
            InteractionResult.FAIL
        }
    }

    private fun getLockableChest(level: Level, pos: BlockPos): LockableChest? {
        val entity = level.getBlockEntity(pos) ?: return null
        if (entity is ChestBlockEntity)
            return entity as LockableChest

        return null
    }

    override fun onHopperEjectItems(level: Level, targetContainer: Container): InteractionResult {
        if (targetContainer !is ChestBlockEntity)
            return InteractionResult.PASS

        val chest = targetContainer as LockableChest
        return if (chest.`lc$getData`().owner.isPresent) {
            InteractionResult.FAIL
        } else {
            InteractionResult.PASS
        }
    }

    override fun onHopperSuckItems(level: Level, sourceContainer: Container): InteractionResult {
        return onHopperEjectItems(level, sourceContainer)
    }

    override fun beforeBlockBreak(
        level: Level?,
        player: Player?,
        blockPos: BlockPos?,
        blockState: BlockState?,
        p4: BlockEntity?
    ): Boolean {
        if (player !is ServerPlayer) return true

        val chest = getLockableChest(level!!, blockPos!!) ?: return true
        val perms = chest.`lc$getAccessPermissions`(player)
        if (!perms.hasOwnerAccess()) {
            val component = ModUtil.makeCannotInteractMessage(
                player.server,
                chest.`lc$getData`().owner.get(),
                "destroy"
            )
            player.sendSystemMessage(ModUtil.render(component), false)
            return false
        }

        return true
    }
}