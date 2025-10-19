package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.util.AliasRegex;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.PackedPacks.MOD_ID;

public class Config implements Serializable {
    private boolean devMode = false;
    private boolean showActionBar = false;
    private boolean hideIncompatible = false;
    private Query.SortOption sort = Query.SortOption.VANILLA;
    private final ResourcePacks resourcepacks = new ResourcePacks();
    private final DataPacks datapacks = new DataPacks();
    transient File file;

    Config() {
    }

    public boolean isDevMode() {
        return this.devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void setShowActionBar(boolean showActionBar) {
        this.showActionBar = showActionBar;
    }

    public boolean isShowActionBar() {
        return this.showActionBar;
    }

    public boolean isHideIncompatible() {
        return this.hideIncompatible;
    }

    public void setHideIncompatible(boolean hideIncompatible) {
        this.hideIncompatible = hideIncompatible;
    }

    public Query.SortOption getSort() {
        return this.sort;
    }

    public void setSort(Query.SortOption sort) {
        this.sort = sort;
    }

    public ResourcePacks getResourcepacks() {
        return resourcepacks;
    }

    public DataPacks getDatapacks() {
        return datapacks;
    }

    public Packs get(PackType packType) {
        return switch (packType) {
            case CLIENT_RESOURCES -> this.getResourcepacks();
            case SERVER_DATA -> this.getDatapacks();
        };
    }

    public static Config load(Path directory) {
        File file = directory.resolve(MOD_ID + ".json").toFile();
        Config config;

        try {
            config = ConfigLoader.loadOrSave(file, Config.class, Config::new);
        } catch (Exception e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to read config file, using default. ", e);
            config = new Config();
        }

        config.file = file;
        return config;
    }

    public void save() {
        if (this.file != null) {
            ConfigLoader.save(this, file);
        } else {
            throw new IllegalStateException("Config file has not been initialized.");
        }
    }

    public abstract static sealed class Packs implements Serializable {
        private boolean replaceOriginal = true;
        private boolean hideIncompatibleWarnings = false;
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private final List<String> additionalFolders = new ObjectArrayList<>();
        private final Map<String, String> packIdAliases = new Object2ObjectLinkedOpenHashMap<>();
        private boolean rememberLastViewedProfile = false;
        private @Nullable Long lastViewedProfile = null;
        private @Nullable Long defaultProfile = null;
        private long autoIncrement = 0;
        private final List<Profile> profiles = new ObjectArrayList<>();
        private transient @Nullable Profile cachedDefaultProfile = null;
        private transient @Nullable Profile cachedLastViewedProfile = null;
        private transient @Nullable Map<Pattern, String> aliasPatterns;

        public abstract PackType packType();

        public Set<String> getAliases() {
            return this.packIdAliases.keySet();
        }

        public List<String> getAliases(String packId) {
            return CollectionsUtil.reverseLookup(packId, this.packIdAliases);
        }

        public boolean hasAlias(String packId) {
            return this.packIdAliases.containsValue(packId);
        }

        public @Nullable String getAndSaveCanonicalId(Config config, String packId) {
            String canonicalId = this.packIdAliases.get(packId);
            if (canonicalId != null) {
                this.savePackIdsOnResolve(config, packId, canonicalId);
                return canonicalId;
            }

            Map<Pattern, String> patternMap = this.getAliasPatterns();
            if (patternMap != null) {
                canonicalId = AliasRegex.resolveCanonicalId(packId, patternMap);
                if (canonicalId != null) {
                    PackedPacks.LOGGER.info("[packed_packs] Resolved unknown pack '{}' to '{}' with regex, caching result.", packId, canonicalId);
                    this.savePackIdsOnResolve(config, packId, canonicalId);
                }
            }

            return canonicalId;
        }

        private @Nullable Map<Pattern, String> getAliasPatterns() {
            if (this.packIdAliases.isEmpty()) {
                return null;
            }
            if (this.aliasPatterns == null) {
                this.aliasPatterns = AliasRegex.findPatternsFromKeys(this.packIdAliases);
            }
            return this.aliasPatterns;
        }

        private void savePackIdsOnResolve(Config config, String packId, String canonicalId) {
            if (!this.packIdAliases.containsKey(packId)) {
                this.packIdAliases.put(packId, canonicalId);
                this.packIdAliases.remove(canonicalId);
                for (Profile profile : this.profiles) {
                    profile.updatePackId(packId, canonicalId);
                }
                config.save();
            }
        }

        public void setAliases(String packId, List<String> aliases) {
            CollectionsUtil.updateReverseMapping(this.packIdAliases, packId, aliases);
        }

        public @Nullable Profile getDefaultProfile() {
            if (this.defaultProfile == null) {
                return null;
            }
            if (this.cachedDefaultProfile != null) {
                return this.cachedDefaultProfile;
            }
            this.cachedDefaultProfile = CollectionsUtil.firstMatch(this.profiles, this.defaultProfile, Profile::getId);
            return this.cachedDefaultProfile;
        }

        public void setDefaultProfile(@Nullable Profile defaultProfile) {
            if (defaultProfile == null || CollectionsUtil.containsId(this.profiles, defaultProfile.getId(), Profile::getId)) {
                this.defaultProfile = defaultProfile != null ? defaultProfile.getId() : null;
                this.cachedDefaultProfile = defaultProfile;

                if (defaultProfile != null) {
                    profiles.remove(defaultProfile);
                    profiles.addFirst(defaultProfile);
                }
            }
        }

        public boolean isLastViewedProfileRemembered() {
            return this.rememberLastViewedProfile;
        }

        public void setRememberLastViewedProfile(boolean rememberLastViewedProfile) {
            this.rememberLastViewedProfile = rememberLastViewedProfile;
        }

        public @Nullable Profile getLastViewedProfile() {
            if (this.lastViewedProfile == null) {
                return null;
            }
            if (this.cachedLastViewedProfile != null) {
                return this.cachedLastViewedProfile;
            }
            this.cachedLastViewedProfile = CollectionsUtil.firstMatch(this.profiles, this.lastViewedProfile, Profile::getId);
            return this.cachedLastViewedProfile;
        }

        public void setLastViewedProfile(@Nullable Profile lastViewedProfile) {
            if (lastViewedProfile == null || CollectionsUtil.containsId(this.profiles, lastViewedProfile.getId(), Profile::getId)) {
                this.lastViewedProfile = lastViewedProfile != null ? lastViewedProfile.getId() : null;
                this.cachedLastViewedProfile = lastViewedProfile;
            }
        }

        public List<Profile> getProfiles() {
            return Collections.unmodifiableList(this.profiles);
        }

        public void addProfile(Profile profile) {
            profile.setId(++this.autoIncrement);
            this.profiles.add(profile);
        }

        public void removeProfile(Profile profile) {
            this.profiles.remove(profile);

            if (profile != null) {
                if (Objects.equals(this.defaultProfile, profile.getId())) {
                    this.defaultProfile = null;
                    this.cachedDefaultProfile = null;
                }
                if (Objects.equals(this.lastViewedProfile, profile.getId())) {
                    this.lastViewedProfile = null;
                    this.cachedLastViewedProfile = null;
                }
            }

            if (this.profiles.isEmpty()) {
                this.autoIncrement = 0;
                this.cachedDefaultProfile = null;
                this.defaultProfile = null;
                this.lastViewedProfile = null;
                this.cachedLastViewedProfile = null;
            }
        }

        public boolean isReplaceOriginal() {
            return this.replaceOriginal;
        }

        public void setReplaceOriginal(boolean replaceOriginal) {
            this.replaceOriginal = replaceOriginal;
        }

        public boolean isIncompatibleWarningsHidden() {
            return this.hideIncompatibleWarnings;
        }

        public void setHideIncompatibleWarnings(boolean hidden) {
            this.hideIncompatibleWarnings = hidden;
        }

        public List<String> getAdditionalFolders() {
            return List.copyOf(this.additionalFolders);
        }
    }

    public static final class DataPacks extends Packs {
        @Override
        public PackType packType() {
            return PackType.SERVER_DATA;
        }
    }

    public static final class ResourcePacks extends Packs {
        private boolean applyOnClose = true;

        @Override
        public PackType packType() {
            return PackType.CLIENT_RESOURCES;
        }

        public boolean isApplyOnClose() {
            return this.applyOnClose;
        }

        public void setApplyOnClose(boolean applyOnClose) {
            this.applyOnClose = applyOnClose;
        }
    }
}
