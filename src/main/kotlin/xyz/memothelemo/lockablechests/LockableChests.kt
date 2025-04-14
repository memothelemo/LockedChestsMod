package xyz.memothelemo.lockablechests

import net.fabricmc.api.ModInitializer
import xyz.memothelemo.lockablechests.commands.ModCommands

object LockableChests : ModInitializer {
	const val DEBUG_MODE = false
	const val DYSTOPIA_MODE = true

	override fun onInitialize() {
		ModCommands.initialize()
		ModListener.initialize()

		ModLogger.info("Loaded Lockable Chests mod")
		if (DYSTOPIA_MODE) {
			ModLogger.info("This mod is running in Dystopia mode")
		}

		if (DEBUG_MODE) {
			ModLogger.warn("Debug mode is enabled for Lockable Chests mod")
		}
	}
}
