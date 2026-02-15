package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

public class Config implements Serializable {
    private static final String FILENAME = "config.json";
    private static final Config INSTANCE = loadOrCreate();
    private boolean devMode = false;
    private boolean showActionBar = false;
    private boolean hideIncompatible = false;
    private String sort;
    private final ResourcePacks resourcepacks = new ResourcePacks();
    private final DataPacks datapacks = new DataPacks();

    private Config() {
    }

    private static Path getPath() {
        return PackedPacks.getConfigDir().resolve(FILENAME);
    }

    private static Config loadOrCreate() {
        return JsonLoader.loadOrCreateJson(getPath(), Config.class, Config::new);
    }

    public static Config get() {
        return INSTANCE;
    }

    public Packs get(PackType packType) {
        return switch (packType) {
            case CLIENT_RESOURCES -> this.getResourcepacks();
            case SERVER_DATA -> this.getDatapacks();
        };
    }

    public void save() {
        JsonLoader.saveJson(this, getPath());
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
        return Query.SortOption.getOrDefault(this.sort);
    }

    public void setSort(Query.SortOption sort) {
        this.sort = sort.name();
    }

    public ResourcePacks getResourcepacks() {
        return this.resourcepacks;
    }

    public DataPacks getDatapacks() {
        return this.datapacks;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public abstract static sealed class Packs implements Serializable {
        private boolean replaceOriginal = true;
        private boolean hideIncompatibleWarnings = false;
        private final List<String> additionalFolders = new ObjectArrayList<>();
        private boolean rememberLastViewedProfile = false;
        private @Nullable String lastViewedProfile = null;
        private List<String> profileOrder = new ObjectArrayList<>();
        private transient @Nullable List<Profile> availableProfiles;
        private transient @Nullable Profile cachedLastViewedProfile = null;

        public abstract PackType packType();

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
            this.cachedLastViewedProfile = CollectionsUtil.firstMatch(this.getProfiles(), this.lastViewedProfile, Profile::getId);
            if (this.cachedLastViewedProfile == null) {
                this.lastViewedProfile = null;
            }
            return this.cachedLastViewedProfile;
        }

        public void setLastViewedProfile(@Nullable Profile lastViewedProfile) {
            this.lastViewedProfile = lastViewedProfile != null ? lastViewedProfile.getId() : null;
            this.cachedLastViewedProfile = lastViewedProfile;
        }

        public List<Profile> getProfiles() {
            if (this.availableProfiles == null) {
                List<Profile> availableProfiles = Profiles.getAll(this.packType(), Util.backgroundExecutor());

                Map<String, Integer> profileOrderMap = new Object2IntOpenHashMap<>(this.profileOrder.size());
                for (int i = 0; i < this.profileOrder.size(); i++) {
                    profileOrderMap.put(this.profileOrder.get(i), i);
                }

                availableProfiles.sort(Comparator.<Profile>comparingInt(profile ->
                        profileOrderMap.containsKey(profile.getId())
                                ? profileOrderMap.get(profile.getId()) + this.profileOrder.size()
                                : 0
                ).thenComparing(Profile::getName));

                this.availableProfiles = availableProfiles;
            }


            return Collections.unmodifiableList(this.availableProfiles);
        }

        public void setProfileOrder(SequencedCollection<Profile> profiles) {
            this.profileOrder = CollectionsUtil.map(profiles, Profile::getId, ObjectArrayList::new);
        }

        public void addProfile(Profile profile) {
            this.profileOrder.add(profile.getId());
            if (this.availableProfiles != null) {
                this.availableProfiles.add(profile);
            }
        }

        public void renameProfile(Profile profile, String name) {
            profile.setName(name);
        }

        public void removeProfile(Profile profile) {
            Profiles.delete(this.packType(), profile);

            this.profileOrder.remove(profile.getId());
            if (this.availableProfiles != null) {
                this.availableProfiles.remove(profile);
            }

            if (this.profileOrder.isEmpty() || Objects.equals(this.lastViewedProfile, profile.getId())) {
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
