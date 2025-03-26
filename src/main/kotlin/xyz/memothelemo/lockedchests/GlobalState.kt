package xyz.memothelemo.lockedchests

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.World

// 1.21.4 code:
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.registry.RegistryWrapper

// Support for 1.21.5 code:
//import com.mojang.serialization.Codec
//import com.mojang.serialization.codecs.RecordCodecBuilder
// import net.minecraft.world.PersistentStateType

/// Stores all of the information about every player's locked chests.
class GlobalState: PersistentState() {
    private val allLockedChests = HashMap<String, HashSet<BlockPos>>()
    private val cachedUsernames = HashMap<String, String>()

    fun cachePlayerName(player: ServerPlayerEntity) {
        this.cachedUsernames[player.uuidAsString] = player.name.string
        this.markDirty()
    }

    fun getCachedPlayerName(playerUuid: String): String? {
        return this.cachedUsernames[playerUuid]
    }

    fun getAllLockedChests(player: ServerPlayerEntity): List<BlockPos> {
        return this.allLockedChests.getOrDefault(player.uuidAsString, HashSet()).toList()
    }

    fun addLockedChest(player: ServerPlayerEntity, pos: BlockPos) {
        addLockedChest(player.uuidAsString, pos)
    }

    fun addLockedChest(playerUuid: String, pos: BlockPos) {
        val list = this.allLockedChests.getOrPut(playerUuid) { HashSet() }
        list.add(pos)
        this.markDirty()
    }

    fun removeLockedChest(player: ServerPlayerEntity, pos: BlockPos) {
        removeLockedChest(player.uuidAsString, pos)
    }

    fun removeLockedChest(playerUuid: String, pos: BlockPos) {
        val list = this.allLockedChests.getOrPut(playerUuid) { HashSet() }
        list.remove(pos)
        this.markDirty()
    }

    override fun writeNbt(nbt: NbtCompound, registries: RegistryWrapper.WrapperLookup?): NbtCompound {
        val cachedUsernames = NbtCompound()
        for (entry in this.cachedUsernames) {
            cachedUsernames.put(entry.key, NbtString.of(entry.value))
        }

        val allChestsNbt = NbtCompound()
        for (entry in this.allLockedChests.entries) {
            val list = NbtList()
            entry.value.forEach { list.add(serializeBlockPos(it)) }
            allChestsNbt.put(entry.key, list)
        }

        nbt.put("all_locked_chests", allChestsNbt)
        nbt.put("cached_usernames", cachedUsernames)
        return nbt
    }

    companion object {
        private const val NBT_KEY = "lockedchests.global_state"
        private var CACHED: GlobalState? = null

        // 1.21.5:
//        private val DEFAULT_CODEC: Codec<GlobalState> = RecordCodecBuilder.create { instance ->
//            instance.group(
//                Codec.unboundedMap(
//                    Codec.STRING,
//                    BlockPos.CODEC.listOf().xmap({ HashSet(it) }, { it.toList() })
//                )
//                    .fieldOf("all_locked_chests")
//                    .forGetter {
//                        it.allLockedChests
//                    }
//            )
//                .apply(instance, ::createFromCodec)
//        }

        fun load(server: MinecraftServer): GlobalState {
            if (CACHED != null) return CACHED as GlobalState

            val world = server.getWorld(World.OVERWORLD)
            assert(world != null)

            val state = world!!.persistentStateManager.getOrCreate<GlobalState>(
                Type(
                    { createEmpty() as GlobalState },
                    { n, _ -> createFromNbt(n) as GlobalState },
                    null
                ),
                NBT_KEY,
            )

            // 1.21.5:
//            val state = world!!.persistentStateManager.getOrCreate<GlobalState>(
//                PersistentStateType<GlobalState>(
//                    NBT_KEY,
//                    { GlobalState.createEmpty() as GlobalState },
//                    DEFAULT_CODEC,
//                    null
//                )
//            )
            CACHED = state as GlobalState

            return state
        }

        private fun createEmpty(): PersistentState {
            return GlobalState()
        }

        // 1.21.5:
        // private fun createFromCodec(data: Map<String, HashSet<BlockPos>>): GlobalState {
        // val state = GlobalState()
        // state.allLockedChests.putAll(data)
        // return state
        // }

        // 1.21.4 code:
        private fun createFromNbt(tag: NbtCompound): PersistentState {
            val state = GlobalState()

            val cachedUsernames = tag.getCompound("cached_usernames")
            for (key in cachedUsernames.keys) {
                val value = cachedUsernames.get(key)
                if (value !is NbtString) continue
                state.cachedUsernames[key] = value.asString()
            }

            val rawAllLockedChests = tag.getCompound("all_locked_chests")
            for (key in rawAllLockedChests.keys) {
                val allLockedChestPerPlayer = HashSet<BlockPos>()
                rawAllLockedChests.getList(key, NbtElement.COMPOUND_TYPE.toInt())
                    .stream()
                    .filter { it is NbtCompound }
                    .forEach {
                        val pos = parseBlockPosFromNbt(it as NbtCompound)
                        allLockedChestPerPlayer.add(pos)
                    }

                state.allLockedChests[key] = allLockedChestPerPlayer
            }

            return state
        }

        private fun parseBlockPosFromNbt(tag: NbtCompound): BlockPos {
            return BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"))
        }

        private fun serializeBlockPos(pos: BlockPos): NbtCompound {
            val compound = NbtCompound()
            compound.putInt("x", pos.x)
            compound.putInt("y", pos.y)
            compound.putInt("z", pos.z)

            return compound
        }
    }
}
