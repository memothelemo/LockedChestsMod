package xyz.memothelemo.lockablechests.types

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ExtraCodecs
import xyz.memothelemo.lockablechests.ModLogger
import xyz.memothelemo.lockablechests.api.types.Permissions
import java.util.Optional

class NewLCData private constructor(@JvmField var position: BlockPos) {
    @JvmField var customName: Optional<String> = Optional.empty()
    @JvmField var owner: Optional<String> = Optional.empty()
    @JvmField var players: HashMap<String, PlayerEntry> = HashMap()

    // Default permissions for everyone I guess...
    @JvmField var defaultPerms: Permissions = Permissions.EMPTY

    fun clear() {
        this.customName = Optional.empty()
        this.owner = Optional.empty()
        this.players.clear()
        this.defaultPerms = Permissions.EMPTY
    }

    fun overrideFields(source: NewLCData) {
        this.customName = source.customName
        this.owner = source.owner
        this.players.clear()
        this.defaultPerms = source.defaultPerms
        source.players.forEach { (k, v) -> this.players[k] = v }
    }

    fun saveWithNbt(chestTag: CompoundTag, registries: HolderLookup.Provider) {
        val ctx = registries.createSerializationContext(NbtOps.INSTANCE)
        val encoded = CURRENT_SCHEMA.codec.encodeStart(ctx, this)
            .resultOrPartial(Util.prefix(
                "Failed to save lockable chest data to the chest entity NBT data",
                ModLogger::warn
            ))
            .orElseGet { CompoundTag() }

        chestTag.put(NBT_PERSISTENT_KEY, encoded)
    }

    class PlayerEntry {
        @JvmField var override: Optional<Permissions> = Optional.empty()
        @JvmField var isTrusted = false

        companion object {
            val CODEC: Codec<PlayerEntry> = Codec.lazyInitialized {
                return@lazyInitialized RecordCodecBuilder.create {
                    return@create it.group(
                        Permissions.CODEC.optionalFieldOf("override")
                            .forGetter(PlayerEntry::override),
                        Codec.BOOL.optionalFieldOf("trusted", false)
                            .forGetter(PlayerEntry::isTrusted)
                    ).apply(it) { override, isTrusted ->
                        val entry = PlayerEntry()
                        entry.override = override
                        entry.isTrusted = isTrusted
                        return@apply entry
                    }
                }
            }
        }
    }

    private class V1Schema: VersionSchema() {
        override val version: Int
            get() = 1

        val codec: Codec<NewLCData>
            get() = Codec.lazyInitialized {
                return@lazyInitialized RecordCodecBuilder.create {
                    return@create it.group(
                        Codec.STRING.optionalFieldOf("custom_name")
                            .forGetter(NewLCData::customName),
                        ExtraCodecs.NON_EMPTY_STRING.optionalFieldOf("owner")
                            .forGetter(NewLCData::owner),
                        Codec.unboundedMap(ExtraCodecs.NON_EMPTY_STRING, PlayerEntry.CODEC)
                            .optionalFieldOf("players", HashMap())
                            .forGetter(NewLCData::players),
                        BlockPos.CODEC.optionalFieldOf("position", BlockPos.ZERO)
                            .forGetter(NewLCData::position),
                        Permissions.CODEC.optionalFieldOf("default_permissions", Permissions.EMPTY)
                            .forGetter(NewLCData::defaultPerms)
                    ).apply(it) { customName, owner, players, position, defaultPerms ->
                        val data = empty(position)
                        data.customName = customName
                        data.owner = owner
                        data.players = HashMap(players)
                        data.defaultPerms = defaultPerms
                        return@apply data
                    }
                }
            }

        override fun migrate(tag: CompoundTag, position: BlockPos): MigrateResult {
            return MigrateResult.Done
        }
    }

    private abstract class VersionSchema {
        abstract val version: Int

        open fun migrate(tag: CompoundTag, position: BlockPos): MigrateResult {
            if (tag.getInt("version") == this.version) {
                return MigrateResult.Done
            }
            TODO("implement pls")
        }
    }

    private enum class MigrateResult {
        CurrentVersion,
        NewData,
        Done,
        Failed,
        UnsupportedVersion,
    }

    companion object {
        private val VERSIONS: MutableMap<Int, VersionSchema> = HashMap()
        private val CURRENT_SCHEMA: V1Schema = V1Schema()
        init {
            VERSIONS[1] = CURRENT_SCHEMA
        }

        fun empty(position: BlockPos): NewLCData {
            return NewLCData(position)
        }

        fun fromLegacy(legacy: LegacyLCData, position: BlockPos): NewLCData {
            val data = NewLCData(position)
            data.owner = Optional.ofNullable(legacy.owner)
            legacy.trustedPlayers.forEach {
                val entry = PlayerEntry()
                entry.isTrusted = true
                data.players[it] = entry
            }
            return data
        }

        private const val NBT_PERSISTENT_KEY = "LockableChests"
        fun fromNbt(position: BlockPos, chestTag: CompoundTag, registries: HolderLookup.Provider): NewLCData {
            if (!chestTag.contains(NBT_PERSISTENT_KEY))
                chestTag.put(NBT_PERSISTENT_KEY, CompoundTag())

            val lcTag = chestTag.getCompound(NBT_PERSISTENT_KEY)
            if (lcTag.contains("version")) {
                val result = migrate(lcTag, position)
                if (result == MigrateResult.UnsupportedVersion) {
                    ModLogger.warn("Invalid lockable chest data schema version "
                            + lcTag.getInt("version")
                            + ", resorting to default data instead."
                    )
                    return empty(position)
                } else if (result == MigrateResult.Failed) {
                    ModLogger.warn("Invalid migrate lockable chest data from schema version "
                            + lcTag.getInt("version")
                            + ", resorting to default data instead."
                    )
                    return empty(position)
                }
            }

            val ctx = registries.createSerializationContext(NbtOps.INSTANCE)
            val result = CURRENT_SCHEMA.codec.parse(ctx, lcTag)
                .resultOrPartial(Util.prefix(
                    "Failed to load lockable chest data, resorting to default data instead.",
                    ModLogger::warn
                ))
                .orElseGet { empty(position) }

            return result
        }

        private fun migrate(tag: CompoundTag, position: BlockPos): MigrateResult {
            // Do we need to migrate? we'll going to base on the version
            if (!tag.contains("version"))
                return MigrateResult.NewData

            val version = tag.getInt("version")
            if (CURRENT_SCHEMA.version == version)
                return MigrateResult.CurrentVersion

            // Making sure we're within all "supported" schema versions in LockableChests perhaps?
            if (VERSIONS[version] == null)
                return MigrateResult.UnsupportedVersion

            // v1 -> v2 -> v3 -> v4 -> v5
            for (i in version + 1..CURRENT_SCHEMA.version) {
                val schema = VERSIONS[i]
                val result = schema!!.migrate(tag, position)
                if (result == MigrateResult.Failed)
                    return MigrateResult.Failed
            }

            return MigrateResult.Done
        }
    }

//    var customName: Optional<String> = Optional.empty()
//    var owner: Optional<String> = Optional.empty()
//
//    // Permissions for every player stuff like that.
//    var players: HashMap<String, Int> = HashMap()
//
//    private class V1Schema() : VersionSchema() {
//        override val version: Int
//            get() = 1
//
//        override fun migrate(tag: CompoundTag?, position: BlockPos?): MigrationResult {
//            throw UnsupportedOperationException("Unimplemented method 'migrate'")
//        }
//    }
//
//    private abstract class VersionSchema() {
//        abstract val version: Int
//
//        abstract fun migrate(tag: CompoundTag?, position: BlockPos?): MigrationResult
//    }
//
//    private enum class MigrationResult {
//        SameVersion,
//        NewData,
//        Done,
//        Failed,
//        UnsupportedVersion,
//    }
//
//    companion object {
//        fun empty(position: BlockPos): ChestDataPrototype {
//            return ChestDataPrototype(position)
//        }
//
//        private val NBT_PERSISTENT_KEY = "LockableChests"
//        fun fromNbt(position: BlockPos, chestTag: CompoundTag, registries: HolderLookup.Provider?) {
//            if (!chestTag.contains(NBT_PERSISTENT_KEY)) chestTag.put(NBT_PERSISTENT_KEY, CompoundTag())
//
//            val lcTag = chestTag.getCompound(NBT_PERSISTENT_KEY)
//            if (lcTag.contains("version")) {
//                val result = migrate(lcTag, position)
//                if (result == MigrationResult.UnsupportedVersion) {
//                    warn(
//                        "Invalid lockable chest data schema version "
//                                + lcTag.getInt("version") + ", resorting to default data instead."
//                    )
//                }
//            }
//        }
//
//        fun fromLegacy(legacyData: LegacyLCData, position: BlockPos): ChestDataPrototype {
//            val data = ChestDataPrototype(position)
//            data.owner = Optional.ofNullable(legacyData.owner)
//            legacyData.trustedPlayers.forEach(Consumer { player: String? -> })
//            return data
//        }
//
//        private val VERSIONS: MutableMap<Int, VersionSchema?> = HashMap()
//        private val CURRENT_SCHEMA: VersionSchema
//
//        private fun migrate(tag: CompoundTag, position: BlockPos): MigrationResult {
//            // Do we need to migrate? we'll going to base on the version
//            if (!tag.contains("version")) {
//                return MigrationResult.NewData
//            }
//
//            val version = tag.getInt("version")
//            if (CURRENT_SCHEMA.version == version) {
//                return MigrationResult.SameVersion
//            }
//
//            // Making sure we're in the correct version perhaps?
//            if (VERSIONS[version] == null) return MigrationResult.UnsupportedVersion
//
//            // v1 -> v2 -> v3 -> v4
//            for (i in version + 1..CURRENT_SCHEMA.version) {
//                val schema = VERSIONS[i]
//                val result = schema!!.migrate(tag, position)
//                if (result == MigrationResult.Failed) {
//                    return MigrationResult.Failed
//                }
//            }
//
//            return MigrationResult.Done
//        }
//
//        init {
//            CURRENT_SCHEMA = V1Schema()
//            VERSIONS[1] = CURRENT_SCHEMA
//        }
//    }
} //package xyz.memothelemo.lockablechests.types;
//
//import com.mojang.serialization.Codec;
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.util.ExtraCodecs;
//import org.jetbrains.annotations.NotNull;
//import xyz.memothelemo.lockablechests.ModLogger;
//import xyz.memothelemo.lockablechests.api.types.Permissions;
//
//import java.util.HashMap;
//import java.util.Optional;
//
//public class NewChestData {
//    private static final int CURRENT_SCHEMA_VERSION = 1;
//    private static final String NBT_PERSISTENT_KEY = "LockableChests";
//
//    private static final Codec<NewChestData> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create((builder) -> {
//        return builder.group(
//                ExtraCodecs.POSITIVE_INT.optionalFieldOf("version", CURRENT_SCHEMA_VERSION)
//                        .forGetter((it) -> it.version),
//                Codec.STRING.optionalFieldOf("custom_name")
//                        .forGetter((it) -> it.customName),
//                Codec.unboundedMap(ExtraCodecs.NON_EMPTY_STRING, Entry.CODEC)
//                        .optionalFieldOf("players", new HashMap<>())
//                        .forGetter((it) -> it.players),
//                BlockPos.CODEC.optionalFieldOf("position", BlockPos.ZERO)
//                        .forGetter((it) -> it.position)
//        ).apply(builder, (version, custom_name, players, position) -> {
//            NewChestData data = NewChestData.empty(position);
//            data.version = version;
//            data.customName = custom_name;
//            data.players = new HashMap<>(players);
//            return data;
//        });
//    }));
//
//    public @NotNull Optional<String> customName = Optional.empty();
//    public @NotNull Optional<String> owner = Optional.empty();
//    // Permissions for every player stuff like that.
//    public @NotNull HashMap<String, Entry> players = new HashMap<>();
//    public @NotNull BlockPos position;
//    public int version = CURRENT_SCHEMA_VERSION;
//
//    public static NewChestData empty(BlockPos position) {
//        return new NewChestData(position);
//    }
//
//    public static NewChestData fromNbt(BlockPos position, CompoundTag chestTag, HolderLookup.Provider registries) {
//        CompoundTag localTag;
//        if (!chestTag.contains(NBT_PERSISTENT_KEY))
//            chestTag.put(NBT_PERSISTENT_KEY, new CompoundTag());
//
//        localTag = chestTag.getCompound(NBT_PERSISTENT_KEY);
//        if (localTag.contains("version")) {
//            int loadedVersion = localTag.getInt("version");
//            if (loadedVersion == 0 || loadedVersion > CURRENT_SCHEMA_VERSION) {
//                ModLogger.INSTANCE.warn("Invalid lockable chest data schema version "
//                    + loadedVersion + ", resorting to default data instead.");
//
//                return NewChestData.empty(position);
//            }
//        }
//
//
//    }
//
//    public static NewChestData fromLegacy(LegacyLCData legacyData, BlockPos position) {
//        NewChestData data = new NewChestData(position);
//        data.owner = Optional.ofNullable(legacyData.getOwner());
//        legacyData.getTrustedPlayers().forEach((player) -> {
//            Entry entry = new Entry();
//            entry.trusted = true;
//            data.players.put(player, entry);
//        });
//        return data;
//    }
//
//    private NewChestData(@NotNull BlockPos position) {
//        this.position = position;
//    }
//
//    public static class Entry {
//        public @NotNull Optional<Permissions> override = Optional.empty();
//        public boolean trusted = false;
//
//        public Entry() {}
//        private Entry(@NotNull Optional<Permissions> override, boolean trusted) {
//            this.override = override;
//            this.trusted = trusted;
//        }
//
//        public static Codec<Entry> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create((builder) ->
//                builder.group(
//                    Permissions.CODEC.optionalFieldOf("override").forGetter((it) -> it.override),
//                    Codec.BOOL.optionalFieldOf("trusted", false).forGetter((it) -> it.trusted)
//                ).apply(builder, Entry::new)));
//    }
//}