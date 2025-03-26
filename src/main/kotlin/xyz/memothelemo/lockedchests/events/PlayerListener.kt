package xyz.memothelemo.lockedchests.events

import xyz.memothelemo.lockedchests.GlobalState

object PlayerListener {
    fun listen() {
        OnPlayerJoin.EVENT.register { player ->
            val state = GlobalState.load(player.server)
            state.cachePlayerName(player)
        }
    }
}