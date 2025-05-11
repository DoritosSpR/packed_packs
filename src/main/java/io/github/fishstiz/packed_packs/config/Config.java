package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Config implements Serializable {
    private boolean showActionBar = false;
    private boolean hideIncompatible = false;
    private Query.SortOption sort = Query.SortOption.A_Z;
    private final ResourcePacks resourcepacks = new ResourcePacks();
    private final Packs datapacks = new Packs();
    transient File file;

    Config() {
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

    public void save() {
        if (this.file != null) {
            ConfigLoader.save(this, file);
        } else {
            throw new IllegalStateException("Config file has not been initialized.");
        }
    }

    public static class Packs implements Serializable {
        private boolean replaceOriginal = false;
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
    }

    public static class ResourcePacks extends Packs {
        private boolean applyOnClose = false;

        public boolean isApplyOnClose() {
            return this.applyOnClose;
        }

        public void setApplyOnClose(boolean applyOnClose) {
            this.applyOnClose = applyOnClose;
        }
    }
}
