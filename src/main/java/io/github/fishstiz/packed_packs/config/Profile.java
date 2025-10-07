package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.pack.PackAssets;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.mapOrDefault;

public class Profile implements PackOptions, Serializable {
    public static final int NAME_MAX_LENGTH = 32;
    private boolean locked = false;
    private long id;
    private String name;
    private PackEntry.PackMap packIds = new PackEntry.PackMap();

    public Profile() {
    }

    public Profile(String name) {
        this();
        this.name = trimName(name);
    }

    private Profile(String name, PackEntry.PackMap packIds) {
        this(name);
        this.packIds = packIds;
    }

    void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (!this.isLocked()) this.name = trimName(name);
    }

    public Profile copy() {
        String profileName = this.name;

        if (profileName != null && !profileName.isBlank()) {
            profileName += " - " + ResourceUtil.getText("profile.copy").getString();
        }

        return new Profile(profileName, new PackEntry.PackMap(this.packIds));
    }

    void setPackMap(PackEntry.PackMap packMap) {
        this.packIds = packMap;
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds.keySet());
    }

    public void setPacks(List<Pack> packs) {
        if (!this.locked) {
            this.setPackMap(this.toMap(PackUtil.extractPackIds(packs)));
        }
    }

    public void setHidden(@Nullable Boolean hidden, Pack... packs) {
        for (Pack pack : packs) {
            PackEntry entry = this.packIds.get(pack.getId());
            if (entry != null) {
                if (Boolean.FALSE.equals(hidden)) hidden = null;
                this.packIds.put(pack.getId(), new PackEntry(pack.getId(), hidden, entry.required(), entry.fixed()));
            }
        }
    }

    public void setRequired(@Nullable Boolean required, Pack... packs) {
        for (Pack pack : packs) {
            String id = pack.getId();
            if (required != null && (id.equals(PackAssets.VANILLA_ID) || id.equals(PackAssets.FABRIC_ID))) {
                continue;
            }

            PackEntry entry = this.packIds.get(id);
            if (entry != null) {
                this.packIds.put(id, new PackEntry(id, entry.hidden(), required, entry.fixed()));
            }
        }
    }

    public void setPosition(@Nullable Pack.Position position, Pack... packs) {
        for (Pack pack : packs) {
            PackEntry entry = this.packIds.get(pack.getId());
            if (entry != null) {
                this.packIds.put(pack.getId(), new PackEntry(
                        pack.getId(),
                        entry.hidden(),
                        entry.required(),
                        position != null ? PackEntry.SerializedPosition.get(position) : null
                ));
            }
        }
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public boolean isHidden(Pack pack) {
        return Boolean.TRUE.equals(mapOrDefault(this.packIds.get(pack.getId()), false, PackEntry::hidden));
    }

    @Override
    public boolean isRequired(Pack pack) {
        return Boolean.TRUE.equals(mapOrDefault(this.packIds.get(pack.getId()), false, PackEntry::required));
    }

    @Override
    public boolean isFixed(Pack pack) {
        return mapOrDefault(this.packIds.get(pack.getId()), false, entry -> entry.fixed() != null);
    }

    @Override
    public @Nullable Pack.Position getPosition(Pack pack) {
        PackEntry entry = this.packIds.get(pack.getId());
        if (entry != null && entry.fixed() != null) {
            return entry.fixed().pos();
        }
        return null;
    }

    @Override
    public @Nullable PackSelectionConfig getSelectionConfig(Pack pack) {
        PackEntry packEntry = this.packIds.get(pack.getId());
        if (packEntry != null && (packEntry.required() != null || packEntry.fixed() != null)) {
            return new PackSelectionConfig(this.isRequired(pack), this.getPosition(pack), this.isFixed(pack));
        }
        return null;
    }

    public boolean overridesRequired(Pack pack) {
        return this.overridesProperty(pack, PackEntry::required);
    }

    public boolean overridesPosition(Pack pack) {
        return this.overridesProperty(pack, PackEntry::fixed);
    }

    private boolean overridesProperty(Pack pack, Function<PackEntry, @Nullable Object> property) {
        PackEntry entry = this.packIds.get(pack.getId());
        return entry != null && property.apply(entry) != null;
    }

    private PackEntry.PackMap toMap(List<String> packIds) {
        PackEntry.PackMap entryMap = new PackEntry.PackMap();

        for (String packId : packIds) {
            PackEntry entry = this.packIds.get(packId);
            entryMap.put(packId, entry != null ? entry : new PackEntry(packId));
        }

        return entryMap;
    }

    private static String trimName(String name) {
        if (name == null) return null;
        return name.length() <= NAME_MAX_LENGTH ? name : name.substring(0, NAME_MAX_LENGTH);
    }
}
