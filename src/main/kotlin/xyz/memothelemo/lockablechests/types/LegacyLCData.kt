package xyz.memothelemo.lockablechests.types

import com.mojang.serialization.Codec
import net.minecraft.Util
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import xyz.memothelemo.lockablechests.ModLogger

class LegacyLCData(
    val owner: String? = null,
    val trustedPlayers: List<String> = ArrayList()
) {
    companion object {
            private const val OWNER_KEY = "lockedchests.owner"
        private const val TRUSTED_KEY = "lockedchests.trusted_players"

        fun hasLegacyFields(tag: CompoundTag): Boolean {
            return tag.contains(OWNER_KEY) || tag.contains(TRUSTED_KEY)
        }

        fun deleteLegacyFields(tag: CompoundTag) {
            if (tag.contains(OWNER_KEY)) tag.remove(OWNER_KEY)
            if (tag.contains(TRUSTED_KEY)) tag.remove(TRUSTED_KEY)
        }

        fun fromNbt(tag: CompoundTag, registries: HolderLookup.Provider): LegacyLCData {
            var owner: String? = null
            val trustedPlayers = ArrayList<String>()

            if (tag.contains(OWNER_KEY)) owner = tag.getString(OWNER_KEY)
            if (tag.contains(TRUSTED_KEY)) {
                val ctx = registries.createSerializationContext(NbtOps.INSTANCE)
                Codec.STRING.listOf()
                    .parse(ctx, tag.get(TRUSTED_KEY))
                    .resultOrPartial(Util.prefix(
                        "Failed to load trusted players in legacy data",
                        ModLogger::warn
                    ))
                    .ifPresent { it.stream().forEach(trustedPlayers::add) }
            }

            return LegacyLCData(owner, trustedPlayers)
        }
    }
}