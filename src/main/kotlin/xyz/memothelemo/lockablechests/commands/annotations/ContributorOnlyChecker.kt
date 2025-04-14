package xyz.memothelemo.lockablechests.commands.annotations

import net.minecraft.server.level.ServerPlayer
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.fabric.actor.FabricCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.process.CommandCondition
import xyz.memothelemo.lockablechests.LockableChests
import xyz.memothelemo.lockablechests.ModUtil

class ContributorOnlyChecker: CommandCondition<FabricCommandActor> {
    override fun test(context: ExecutionContext<FabricCommandActor>) {
        val contributorOnly = context.command().annotations().contains(ContributorOnly::class.java)
        if (!contributorOnly || !LockableChests.DYSTOPIA_MODE || !ModUtil.hasLuckPermsMod()) return

        val provider = net.luckperms.api.LuckPermsProvider.get()
        val user = provider.getPlayerAdapter(ServerPlayer::class.java)
            .getPermissionData(context.actor().requirePlayer())

        val isContributor = user.checkPermission("group.ds2.contributors").asBoolean()
        if (!isContributor) {
            throw CommandErrorException("Only Dystopia contributors can use the lockable chests perk!")
        }
    }
}