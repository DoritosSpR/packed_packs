package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.FileUtil;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.mapOrDefault;

public class Profile implements PackOptions, Serializable {
    public static final int NAME_MAX_LENGTH = 32;
    static final String PROFILE_EXTENSION = ".profile.json";
    private static final String PROFILE_EXTENSION_QUOTE = Pattern.quote(PROFILE_EXTENSION);
    private boolean locked = false;
    private String name;
    private Map<String, PackOverride> overrides = new Object2ObjectOpenHashMap<>();
    private Set<String> packIds = new ObjectLinkedOpenHashSet<>();
    transient String id;
    transient Path saveFolder;
    transient boolean temp = false;

    Profile() {
        this.id = createTempId();
    }

    Profile(String name, Path saveFolder) {
        this.temp = true;
        this.saveFolder = saveFolder;
        this.name = trimName(name);
        this.id = findAvailableId(this.saveFolder, this.name);
    }

    private Profile(String name, Set<String> packIds, Map<String, PackOverride> overrides, Path saveFolder) {
        this(name, saveFolder);
        this.packIds = new ObjectLinkedOpenHashSet<>(packIds);
        this.overrides = new Object2ObjectOpenHashMap<>(overrides);
        this.overrides.replaceAll((id, override) -> new PackOverride(override.hidden(), override.required(), override.position()));
    }

    public String getId() {
        return this.id;
    }

    boolean remapPackId(String packId, String newId) {
        boolean remapped = false;
        if (this.packIds.contains(packId) && !this.packIds.contains(newId)) {
            List<String> packIdsList = new ObjectArrayList<>(this.packIds);
            int index = packIdsList.indexOf(packId);
            if (index != -1) {
                PackedPacks.LOGGER.info("[packed_packs] Updating pack id '{}' to '{}' in profile '{}'", packId, newId, this.name);
                packIdsList.add(index, newId);
                this.packIds = new ObjectLinkedOpenHashSet<>(packIdsList);
                remapped = true;
            }
        }
        PackOverride packOverride = this.overrides.get(packId);
        if (packOverride != null) {
            PackedPacks.LOGGER.info("[packed_packs] Copying overrides from pack id '{}' to '{}' in profile '{}'", packId, newId, this.name);
            this.overrides.put(newId, packOverride);
            remapped = true;
        }
        return remapped;
    }

    public String getName() {
        return this.name;
    }

    void setName(String name) {
        if (!this.isLocked()) {
            this.name = trimName(name);
            if (this.temp && this.saveFolder != null) {
                this.id = findAvailableId(this.saveFolder, this.name);
            }
        }
    }

    public Profile copy() {
        String profileName = this.name;

        if (profileName != null && !profileName.isBlank()) {
            profileName += " - " + ResourceUtil.getText("profile.copy").getString();
        }

        return new Profile(profileName, this.packIds, this.overrides, this.saveFolder);
    }

    public boolean includes(Pack pack) {
        return this.packIds.contains(pack.getId());
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds);
    }

    public void setPacks(Collection<Pack> selected) {
        if (!this.locked) {
            selected = PackUtil.flattenPacks(selected);

            this.packIds = new ObjectLinkedOpenHashSet<>(PackUtil.extractPackIds(selected));
        }
    }

    public void syncPacks(Collection<Pack> available, Collection<Pack> selected) {
        if (!this.locked) {
            available = PackUtil.flattenPacks(available);
            selected = PackUtil.flattenPacks(selected);

            this.packIds = new ObjectLinkedOpenHashSet<>(PackUtil.extractPackIds(selected));
            Set<String> availableIds = new ObjectOpenHashSet<>(PackUtil.extractPackIds(available));
            this.overrides.entrySet().removeIf(entry -> {
                PackOverride override = entry.getValue();
                String packId = entry.getKey();
                return !override.hasOverride() || (!this.packIds.contains(packId) && !availableIds.contains(packId));
            });
        }
    }

    public void setHidden(boolean hidden, Collection<Pack> packs) {
        for (Pack pack : PackUtil.flattenPacks(packs)) {
            this.setHidden(hidden, pack);
        }
    }

    public void setHidden(boolean hidden, Pack pack) {
        this.applyOrRemoveOverride(pack.getId(), hidden ? true : null, PackOverride::setHidden);
    }

    public void setRequired(@Nullable Boolean required, Collection<Pack> packs) {
        for (Pack pack : PackUtil.flattenPacks(packs)) {
            this.setRequired(required, pack);
        }
    }

    public void setRequired(@Nullable Boolean required, Pack pack) {
        if (!Boolean.FALSE.equals(required) || !PackUtil.isEssential(pack)) {
            this.applyOrRemoveOverride(pack.getId(), required, PackOverride::setRequired);
        }
    }

    public void setPosition(@Nullable PackOverride.Position position, Collection<Pack> packs) {
        for (Pack pack : PackUtil.flattenPacks(packs)) {
            this.setPosition(position, pack);
        }
    }

    public void setPosition(@Nullable PackOverride.Position position, Pack pack) {
        this.applyOrRemoveOverride(pack.getId(), position, PackOverride::setPosition);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public boolean isHidden(Pack pack) {
        return Boolean.TRUE.equals(mapOrDefault(this.overrides.get(pack.getId()), false, PackOverride::hidden));
    }

    @Override
    public boolean isRequired(Pack pack) {
        return Boolean.TRUE.equals(mapOrDefault(this.overrides.get(pack.getId()), false, PackOverride::required));
    }

    @Override
    public boolean isFixed(Pack pack) {
        if (this.overridesPosition(pack)) {
            return Objects.requireNonNull(this.overrides.get(pack.getId()).position()).fixed();
        }
        return false;
    }

    @Override
    public @Nullable Pack.Position getPosition(Pack pack) {
        if (this.overridesPosition(pack)) {
            return Objects.requireNonNull(this.overrides.get(pack.getId()).position()).get(pack);
        }
        return null;
    }

    public @Nullable PackOverride.Position getPositionOverride(Pack pack) {
        if (this.overridesPosition(pack)) {
            return this.overrides.get(pack.getId()).position();
        }
        return null;
    }

    @Override
    public @Nullable PackSelectionConfig getSelectionConfig(Pack pack) {
        PackOverride packEntry = this.overrides.get(pack.getId());
        if (packEntry != null && (packEntry.required() != null || packEntry.position() != null)) {
            return new PackSelectionConfig(this.isRequired(pack), this.getPosition(pack), this.isFixed(pack));
        }
        return null;
    }

    public boolean overridesRequired(Pack pack) {
        return this.overridesProperty(pack, PackOverride::required);
    }

    public boolean overridesPosition(Pack pack) {
        return this.overridesProperty(pack, PackOverride::position);
    }

    private boolean overridesProperty(Pack pack, Function<PackOverride, @Nullable Object> property) {
        PackOverride entry = this.overrides.get(pack.getId());
        return entry != null && property.apply(entry) != null;
    }

    public boolean hasOverride(Pack pack) {
        PackOverride entry = this.overrides.get(pack.getId());
        return entry != null && entry.hasOverride();
    }

    private <T> void applyOrRemoveOverride(String packId, T property, BiConsumer<PackOverride, T> setter) {
        PackOverride override = this.overrides.computeIfAbsent(packId, id -> new PackOverride());
        setter.accept(override, property);
        if (!override.hasOverride()) this.overrides.remove(packId);
    }

    private static String trimName(String name) {
        if (name == null) return null;
        return name.length() <= NAME_MAX_LENGTH ? name : name.substring(0, NAME_MAX_LENGTH);
    }

    private static String createTempId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS"));
    }

    private static String findAvailableId(Path saveFolder, String name) {
        try {
            return removeExtension(FileUtil.findAvailableName(
                    Objects.requireNonNull(saveFolder, "saveFolder"),
                    Objects.requireNonNull(name, "name"),
                    PROFILE_EXTENSION
            ));
        } catch (IOException e) {
            return createTempId();
        }
    }

    static String removeExtension(String id) {
        return id.replaceFirst(PROFILE_EXTENSION_QUOTE + "$", "");
    }

    public boolean isTemp() {
        return this.temp;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Profile other)) {
            return false;
        }
        if (Objects.equals(other.getId(), this.getId())) {
            return true;
        }
        return false;
    }
}
