package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    public Packs getDatapacks() {
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
        Config config = ConfigLoader.loadOrSave(file, Config.class, Config::new);
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

    public static class Packs implements Serializable {
        private boolean replaceOriginal = true;
        private boolean hideIncompatibleWarnings = false;
        private final List<String> additionalFolders = new ArrayList<>();
        private @Nullable Long lastViewed = null;
        private long autoIncrement = 0;
        private final List<Profile> profiles = new ArrayList<>();
        private transient @Nullable Profile lastViewedProfile;

        public List<Profile> getProfiles() {
            return List.copyOf(this.profiles);
        }

        public void addProfile(Profile profile) {
            profile.setId(++this.autoIncrement);
            this.profiles.add(profile);
        }

        public void removeProfile(Profile profile) {
            this.profiles.remove(profile);

            if (this.profiles.isEmpty()) {
                this.autoIncrement = 0;
            }
        }

        public @Nullable Profile getLastViewed() {
            if (this.lastViewed == null) return null;

            if (this.lastViewedProfile == null) {
                for (Profile profile : this.profiles) {
                    if (profile.getId() == this.lastViewed) {
                        this.lastViewedProfile = profile;
                        break;
                    }
                }
            }

            return lastViewedProfile;
        }

        public void setLastViewed(@Nullable Profile lastViewed) {
            this.lastViewed = lastViewed != null ? lastViewed.getId() : null;
            this.lastViewedProfile = lastViewed;
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

    public static class DataPacks extends Packs {
    }

    public static class ResourcePacks extends Packs {
        private boolean applyOnClose = true;

        public boolean isApplyOnClose() {
            return this.applyOnClose;
        }

        public void setApplyOnClose(boolean applyOnClose) {
            this.applyOnClose = applyOnClose;
        }
    }
}
