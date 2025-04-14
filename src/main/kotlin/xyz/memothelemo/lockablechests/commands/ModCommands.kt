package xyz.memothelemo.lockablechests.commands

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.fabric.FabricLamp
import xyz.memothelemo.lockablechests.ModUtil
import xyz.memothelemo.lockablechests.commands.annotations.ContributorOnlyChecker
import xyz.memothelemo.lockablechests.commands.annotations.JoinedPlayers
import xyz.memothelemo.lockablechests.events.BlockInteractCallback
import xyz.memothelemo.lockablechests.events.LCPlayerEvents
import xyz.memothelemo.lockablechests.events.OnBlockClick
import xyz.memothelemo.lockablechests.storage.ServerDataFactory
import kotlin.collections.ArrayList

object ModCommands: BlockInteractCallback, OnBlockClick, PlayerBlockBreakEvents.Before {
    private var server: MinecraftServer? = null

    fun initialize() {
        LCPlayerEvents.LEAVE.register {
            StatefulCommandManager.clearState(it)
        }
        OnBlockClick.EVENT.register(this)
        BlockInteractCallback.EVENT.register(this)
        PlayerBlockBreakEvents.BEFORE.register(this)

        ServerLifecycleEvents.SERVER_STARTED.register {
            server = it;
            ServerDataFactory.getPersistentData(server!!)
        }

        val lamp = FabricLamp.builder()
            .commandCondition(ContributorOnlyChecker())
            .suggestionProviders { providers -> providers.addProviderForAnnotation(JoinedPlayers::class.java) {
                SuggestionProvider { _ ->
                    if (server == null) return@SuggestionProvider ArrayList()
                    val data = ServerDataFactory.getPersistentData(server!!)
                    data.getJoinedPlayerNames()
                }
            } }
            .build()

        lamp.register(OwnershipCommands())
        lamp.register(TrustCommands())
        lamp.register(PermissionCommands())
    }

    override fun onBlockClick(level: Level, player: ServerPlayer, blockState: BlockState, blockPos: BlockPos) {
        val response = StatefulCommandManager.getCurrentState(player)
            ?.onBlockClicked(level, player, blockState, blockPos)

        if (response != null)
            player.sendSystemMessage(ModUtil.render(response), false)
    }

    override fun onBlockInteract(
        level: Level,
        player: ServerPlayer,
        hand: InteractionHand,
        stack: ItemStack,
        blockState: BlockState,
        blockPos: BlockPos
    ): InteractionResult? {
        return StatefulCommandManager.getCurrentState(player)
            ?.onBlockInteract(level, player, blockState, blockPos)
            ?: return InteractionResult.PASS
    }

    override fun beforeBlockBreak(
        level: Level?,
        player: Player?,
        blockPos: BlockPos?,
        blockState: BlockState?,
        p4: BlockEntity?
    ): Boolean {
        if (player !is ServerPlayer) return true
        return StatefulCommandManager.getCurrentState(player)
            ?.onBlockDestroy(level!!, player, blockState!!, blockPos!!)
            ?: return true
    }
}