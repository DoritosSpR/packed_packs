package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.AliasRegex;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class DevConfig implements Serializable {
    private static final String FILENAME = "config.meta.json";
    private static final DevConfig INSTANCE = loadOrDefault();
    private final ResourcePacks resourcepacks = new ResourcePacks();
    private final DataPacks datapacks = new DataPacks();

    private static Path getPath() {
        return PackedPacks.getConfigDir().resolve(FILENAME);
    }

    private static DevConfig loadOrDefault() {
        return JsonLoader.loadJsonOrDefault(getPath(), DevConfig.class, DevConfig::new);
    }

    public static DevConfig get() {
        return INSTANCE;
    }

    public void save() {
        JsonLoader.saveJson(this, getPath());
    }

    private DevConfig() {
    }

    public Packs get(PackType packType) {
        return switch (packType) {
            case CLIENT_RESOURCES -> this.resourcepacks;
            case SERVER_DATA -> this.datapacks;
        };
    }

    public DataPacks getDatapacks() {
        return this.datapacks;
    }

    public ResourcePacks getResourcepacks() {
        return this.resourcepacks;
    }

    public abstract static sealed class Packs implements Serializable {
        private final Map<String, String> aliases = new Object2ObjectLinkedOpenHashMap<>();
        private transient @Nullable Map<Pattern, String> aliasPatterns;
        private transient List<Pair<String, String>> remappedPacks;
        private @Nullable String defaultProfile;
        private transient Profile cachedDefaultProfile;

        public abstract PackType packType();

        public Set<String> getAliases() {
            return this.aliases.keySet();
        }

        public List<String> getAliases(String packId) {
            return CollectionsUtil.reverseLookup(packId, this.aliases);
        }

        public boolean hasAlias(String packId) {
            return this.aliases.containsValue(packId);
        }

        public @Nullable String getAndSaveCanonicalId(Collection<Profile> profiles, String packId) {
            String canonicalId = this.aliases.get(packId);
            if (canonicalId != null) {
                this.savePackIdsOnResolve(profiles, packId, canonicalId);
                return canonicalId;
            }

            Map<Pattern, String> patternMap = this.getAliasPatterns();
            if (patternMap != null) {
                canonicalId = AliasRegex.resolveCanonicalId(packId, patternMap);
                if (canonicalId != null) {
                    PackedPacks.LOGGER.info("[packed_packs] Resolved unknown pack '{}' to '{}' with regex, caching result.", packId, canonicalId);
                    this.savePackIdsOnResolve(profiles, packId, canonicalId);
                }
            }

            return canonicalId;
        }

        private @Nullable Map<Pattern, String> getAliasPatterns() {
            if (this.aliases.isEmpty()) {
                return null;
            }
            if (this.aliasPatterns == null) {
                this.aliasPatterns = AliasRegex.findPatternsFromKeys(this.aliases);
            }
            return this.aliasPatterns;
        }

        private void savePackIdsOnResolve(Collection<Profile> profiles, String packId, String canonicalId) {
            if (!this.aliases.containsKey(packId)) {
                this.aliases.put(packId, canonicalId);
                this.aliases.remove(canonicalId);

                if (remappedPacks == null) {
                    remappedPacks = new ObjectArrayList<>();
                }

                Profile defaultProfile = this.getDefaultProfile();
                if (defaultProfile != null && defaultProfile.remapPackId(packId, canonicalId)) {
                    Profiles.save(this.packType(), defaultProfile);
                }
                for (Profile profile : profiles) {
                    if (!Objects.equals(profile, defaultProfile) && profile.remapPackId(packId, canonicalId)) {
                        Profiles.save(this.packType(), profile);
                    }
                }

                DevConfig.get().save();
            }
        }

        public void setAliases(String packId, List<String> aliases) {
            CollectionsUtil.updateReverseMapping(this.aliases, packId, aliases);
        }

        public @Nullable Profile getDefaultProfile() {
            if (this.defaultProfile == null) {
                return null;
            }
            if (this.cachedDefaultProfile != null) {
                return this.cachedDefaultProfile;
            }

            this.cachedDefaultProfile = Profiles.get(this.packType(), this.defaultProfile);
            if (this.cachedDefaultProfile == null) {
                this.defaultProfile = null;
            }

            return this.cachedDefaultProfile;
        }

        void setDefaultProfile(@Nullable String profileId) {
            this.defaultProfile = profileId;
            this.cachedDefaultProfile = null;
        }

        public void setDefaultProfile(@Nullable Profile profile) {
            this.defaultProfile = profile == null ? null : profile.getId();
            this.cachedDefaultProfile = profile;
        }
    }

    public static final class ResourcePacks extends Packs {
        @Override
        public PackType packType() {
            return PackType.CLIENT_RESOURCES;
        }
    }

    public static final class DataPacks extends Packs {
        @Override
        public PackType packType() {
            return PackType.SERVER_DATA;
        }
    }
}
