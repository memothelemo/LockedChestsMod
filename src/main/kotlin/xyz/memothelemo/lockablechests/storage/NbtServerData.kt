package xyz.memothelemo.lockablechests.storage

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import xyz.memothelemo.lockablechests.ModLogger

class NbtServerData private constructor() : PersistentServerData, SavedData() {
    private var names = HashMap<String, String>()

    override fun cachePlayer(player: ServerPlayer) {
        this.names[player.stringUUID] = player.name.string
        this.setDirty()
    }

    override fun invalidateCache() {
        this.names.clear()
        this.setDirty()
    }

    override fun getCachedUsername(playerUuid: String): String? {
        return this.names[playerUuid]
    }

    override fun getJoinedPlayerNames(): List<String> {
        val results = ArrayList<String>()
        for (entry in this.names) {
            results.add(entry.value)
        }
        return results
    }

    override fun getPlayerUuidFromName(name: String): String? {
        for (entry in this.names) {
            if (entry.value == name) {
                return entry.key
            }
        }
        return null
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        ModLogger.debug("Saving NbtServerData...")

        val encoded = CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this)
            .resultOrPartial(Util.prefix(
                "Failed to save lockable chest data",
                ModLogger::warn
            ))
            .orElseGet { CompoundTag() } as CompoundTag

        return encoded
    }

    companion object {
        private const val PERSISTENT_KEY = "LockableChests"
        private val CODEC = Codec.lazyInitialized<NbtServerData> {
            return@lazyInitialized RecordCodecBuilder.create { it ->
                return@create it.group(
                    Codec.unboundedMap(ExtraCodecs.NON_EMPTY_STRING, ExtraCodecs.NON_EMPTY_STRING)
                        .fieldOf("cached_names")
                        .forGetter { it.names },
                ).apply(it) { names ->
                    val empty = empty()
                    empty.names = HashMap(names)
                    return@apply empty
                }
            }
        }

        fun empty(): NbtServerData {
            return NbtServerData()
        }

        fun fromNbt(nbt: CompoundTag, registries: HolderLookup.Provider): DataResult<NbtServerData> {
            return CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), nbt)
        }

        fun fromServer(server: MinecraftServer): NbtServerData {
            val manager = server.overworld().dataStorage;
            val factory = Factory(
                { NbtServerData() },
                { nbt, registers -> fromNbt(nbt, registers)
                    .resultOrPartial(Util.prefix(
                        "Could not load LockableChests's server data, initializing with empty data instead\n",
                        ModLogger::warn
                    ))
                    .orElseGet { empty() }
                },
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE
            )

            val result = manager.computeIfAbsent(factory, PERSISTENT_KEY) as NbtServerData
            result.setDirty()
            return result
        }
    }
}