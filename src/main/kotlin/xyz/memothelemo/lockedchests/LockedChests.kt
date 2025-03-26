package xyz.memothelemo.lockedchests

import net.fabricmc.api.ModInitializer
import revxrsal.commands.fabric.FabricLamp
import xyz.memothelemo.lockedchests.events.ChestListener
import xyz.memothelemo.lockedchests.events.PlayerListener

object
LockedChests : ModInitializer {
	override fun onInitialize() {
		val lamp = FabricLamp.builder().build()
		lamp.register(Commands())

		PlayerListener.listen()
		ChestListener.listen()
	}
}
