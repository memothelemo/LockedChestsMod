package xyz.memothelemo.lockedchests

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.server.network.ServerPlayerEntity
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.fabric.actor.FabricCommandActor
import xyz.memothelemo.lockedchests.interfaces.LockableChest

class Commands {
    private val PAGE_SIZE = 10;

    @Command("lockedchests list <page>")
    @Description("Gets a list all of the locked chests that the player have")
    fun listTheirLockedChests(
        actor: FabricCommandActor,
        @Optional page: UInt = 1u,
    ) {
        val player = actor.requirePlayer()
        val lockedChests = GlobalState.load(player.server).getAllLockedChests(player)
        if (lockedChests.isEmpty()) {
            val message = Component.text("You haven't locked any chests yet.")
                .color(NamedTextColor.GRAY)

            actor.reply(LockedChestsUtil.renderComponent(message))
            return;
        }

        if (page.toInt() < 1) {
            actor.error("Page should be greater than 0! (1 to infinity)")
            return;
        }

        var estMaxPages = lockedChests.size / PAGE_SIZE;
        val pageInt = page.toInt();

        if (lockedChests.size % PAGE_SIZE > 0) estMaxPages += 1;
        if (pageInt > estMaxPages) {
            actor.error("The list has $estMaxPages page(s) but you set the page view to $pageInt. "
                + "You may want to run `/lockedchests list` to get the total pages from the list.")
            return;
        }

        val startIdx = (pageInt - 1) * PAGE_SIZE
        val endIdx = startIdx + PAGE_SIZE - 1

        var component = Component.text("List of locked chests ")
            .append(Component.text("(Page $pageInt to $estMaxPages)")
                .color(NamedTextColor.GRAY))

        lockedChests.withIndex()
            .filter { i -> i.index in startIdx..endIdx }
            .forEach { entry ->
                component = component.appendNewline()
                    .append(Component.text("> "))
                    .append(Component.text("${entry.value.x}, ${entry.value.y}, ${entry.value.z}")
                        .color(LockedChestsUtil.REF_COLOR))
            }

        actor.reply(LockedChestsUtil.renderComponent(component))
    }

    @Command("lockedchests lock")
    @Description("Locks the chest that the player is looking at")
    fun lockChest(actor: FabricCommandActor) {
        val player = actor.requirePlayer()
        if (throwIfNotContributor(actor, player)) return;

        val chest = LockedChestsUtil.raycastChest(player) as LockableChest?
        if (chest == null) {
            actor.error("Please look at a chest you want to lock.")
            return
        }

        when (chest.chestOwnerUuid) {
            player.uuidAsString -> {
                actor.error("You already locked this chest.")
                return;
            }
            null -> chest.setChestOwner(player.server, player)
            else -> {
                val ownerUsername = LockedChestsUtil.getOfflineUsername(player.world, chest)
                actor.error("You cannot interact with this chest. It is locked by $ownerUsername.")
                return
            }
        }
        (chest as ChestBlockEntity).markDirty()

        val component = Component.text()
            .append(Component.text("This chest is now locked by "))
            .append(Component.text("you", LockedChestsUtil.GREEN_COLOR, TextDecoration.BOLD))
            .append(Component.text("!"))
            .appendNewline()
            .appendNewline()
            .append(Component.text("To unlock the chest, you may run this command `")
                .color(NamedTextColor.GRAY))
            .append(Component.text("/lockedchests unlock").color(LockedChestsUtil.REF_COLOR))
            .append(Component.text("`.").color(NamedTextColor.GRAY))
            .appendNewline()
            .appendNewline()
            .append(Component.text("You may also want some players to access your chest "
                    + "by running this command: `")
                .color(NamedTextColor.GRAY))
            .append(Component.text("/lockedchests trust <player>").color(LockedChestsUtil.REF_COLOR))
            .append(Component.text("`.").color(NamedTextColor.GRAY))
            .build()

        actor.reply(LockedChestsUtil.renderComponent(component))
    }

    @Command("lockedchests unlock")
    @Description("Unlocks the chest that the player is looking at")
    fun unlockChest(actor: FabricCommandActor) {
        val player = actor.requirePlayer()
        val chest = LockedChestsUtil.raycastChest(player) as LockableChest?
        if (chest == null) {
            actor.error("Please look at a chest you want to lock.")
            return
        }

        when (chest.chestOwnerUuid) {
            player.uuidAsString -> chest.setChestOwner(player.server, null)
            null -> {
                actor.error("You cannot unlock an unlocked chest.")
                return
            }
            else -> {
                val ownerUsername = LockedChestsUtil.getOfflineUsername(player.world, chest)
                actor.error("You cannot interact with this chest. It is locked by $ownerUsername.")
                return
            }
        }

        (chest as ChestBlockEntity).markDirty()
        actor.reply("This chest is now unlocked.")
    }

    @Command("lockedchests trust <target>")
    @Description("Allows the other player to open a locked chest")
    fun trustPlayerToChest(actor: FabricCommandActor, target: ServerPlayerEntity) {
        val player = actor.requirePlayer()
        if (throwIfNotContributor(actor, player)) return;

        val chest = LockedChestsUtil.raycastChest(player) as LockableChest?
        if (chest == null) {
            actor.error("Please look at a chest you want to trust to a player with.")
            return
        }

        when (chest.chestOwnerUuid) {
            player.uuidAsString -> {}
            else -> {
                actor.error("You're not allowed to trust a player to this chest.")
                return
            }
        }

        if (player.uuidAsString == target.uuidAsString) {
            actor.error("You cannot trust yourself, you're the owner of this chest.")
            return
        }
        chest.trustPlayer(target)
        (chest as ChestBlockEntity).markDirty()

        val component = Component.text()
            .append(Component.text("Successfully trusted "))
            .append(Component.text(target.name.string, LockedChestsUtil.REF_COLOR, TextDecoration.BOLD))
            .append(Component.text("!"))
            .appendNewline()
            .appendNewline()
            .append(Component.text("You may also want to untrust them, forbiding them to "
                    + "open your chest by running this command: `").color(NamedTextColor.GRAY))
            .append(Component.text("/lockedchests untrust <player>").color(LockedChestsUtil.REF_COLOR))
            .append(Component.text("`.").color(NamedTextColor.GRAY))
            .build()

        actor.reply(LockedChestsUtil.renderComponent(component))
    }

    @Command("lockedchests untrust <target>")
    @Description("Forbids the other player to open a locked chest")
    fun untrustPlayerToChest(actor: FabricCommandActor, target: ServerPlayerEntity) {
        val player = actor.requirePlayer()
        val chest = LockedChestsUtil.raycastChest(player) as LockableChest?
        if (chest == null) {
            actor.error("Please look at a chest you want to untrust to a player with.")
            return
        }

        when (chest.chestOwnerUuid) {
            player.uuidAsString -> {}
            else -> {
                actor.error("You're not allowed to untrust a player to this chest.")
                return
            }
        }

        if (!chest.isPlayerTrusted(target)) {
            actor.error("${target.name.string} is not trusted to this chest.")
            return
        }

        if (player.uuidAsString == target.uuidAsString) {
            actor.error("You cannot trust yourself, you're the owner of this chest.")
            return
        }
        chest.untrustPlayer(target)
        (chest as ChestBlockEntity).markDirty()

        val component = Component.text()
            .append(Component.text("Successfully untrusted "))
            .append(Component.text(target.name.string, LockedChestsUtil.GREEN_COLOR, TextDecoration.BOLD))
            .append(Component.text("!"))
            .build()

        actor.reply(LockedChestsUtil.renderComponent(component))
    }

    private val notContributorMessage = Component.text()
        .append(Component.text("Only contributors can use the locked chests perk!").color(NamedTextColor.RED))
        .appendNewline()
        .appendNewline()
        .append(Component.text("TIP: ").decorate(TextDecoration.BOLD))
        .append(Component.text("You can be a contributor by joining our Discord server (run `/ds discord` "
            + "to get the invite link) and refer to #contributor-application channel for more information.")
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC))
        .build()

    private fun throwIfNotContributor(actor: FabricCommandActor, player: ServerPlayerEntity): Boolean {
        val result = !LockedChestsUtil.canUseThisMod(player);
        if (result) actor.reply(LockedChestsUtil.renderComponent(notContributorMessage))
        return result
    }
}