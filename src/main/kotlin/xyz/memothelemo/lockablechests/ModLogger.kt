package xyz.memothelemo.lockablechests

import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

@Suppress("unused")
object ModLogger {
    // SLF4J will not give us a logger if the name specified is invalid
    private val logger = LoggerFactory.getLogger("EdenMC")!!

    fun debug(message: String) {
        if (LockableChests.DEBUG_MODE) log(Level.INFO, "[DEBUG] $message")
    }
    fun debug(message: String, e: Throwable) {
        if (LockableChests.DEBUG_MODE) log(Level.INFO, "[DEBUG] $message", e)
    }

    fun warn(message: String) = log(Level.WARN, message)
    fun warn(message: String, e: Throwable) = log(Level.WARN, message, e)

    fun info(message: String) = log(Level.INFO, message)
    fun info(message: String, e: Throwable) = log(Level.INFO, message, e)

    private fun log(level: Level, message: String) {
        for (line in message.lines()) {
            logger.atLevel(level).log("[LockableChests] {}", line)
        }
    }

    private fun log(level: Level, message: String, e: Throwable) {
        for (line in message.lines()) {
            logger.atLevel(level).log("[LockableChests] {}", line)
        }
        logger.atLevel(level).log("[LockableChests] {}", ExceptionUtils.getStackTrace(e))
    }
}
