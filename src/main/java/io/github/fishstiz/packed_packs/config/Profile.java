package io.github.fishstiz.packed_packs.config;

import com.google.common.hash.Hashing;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.mapOrDefault;

public class Profile implements PackOptions, Serializable {
    public static final int NAME_MAX_LENGTH = 32;
    private static final String TEMP_PREFIX = "__temp__";
    private static final String DELIMITER = "__";
    private boolean locked = false;
    private String name;
    private Map<String, PackOverride> overrides = new Object2ObjectOpenHashMap<>();
    private Set<String> packIds = new ObjectLinkedOpenHashSet<>();
    private transient String id;
    private transient String hash;

    private Profile() {
        this.id = TEMP_PREFIX + Instant.now().toEpochMilli();
    }

    public Profile(String name) {
        this.name = trimName(name);
        this.hash = Hashing.murmur3_32_fixed().hashString(this.name + Instant.now().toEpochMilli(), StandardCharsets.UTF_8).toString();
        this.id = this.name + DELIMITER + this.hash;
    }

    private Profile(String name, Set<String> packIds, Map<String, PackOverride> overrides) {
        this(name);
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
            if (this.hash != null) {
                this.id = this.id.replaceAll("^.*(?=" + Pattern.quote(this.hash) + "$)", this.name + DELIMITER);
            }
        }
    }

    public Profile copy() {
        String profileName = this.name;

        if (profileName != null && !profileName.isBlank()) {
            profileName += " - " + ResourceUtil.getText("profile.copy").getString();
        }

        return new Profile(profileName, this.packIds, this.overrides);
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

    void lockId() {
        this.hash = null;
    }

    void lockId(String id) {
        this.id = id;
        this.lockId();
    }

    /**
     * @deprecated removal on stable release. packIds changed from array of objects to array of plain string
     */
    @Deprecated(forRemoval = true)
    static class Deserializer implements JsonDeserializer<Profile> {
        @Override
        public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Profile profile = new Profile();

            if (obj.has("locked")) profile.locked = obj.get("locked").getAsBoolean();
            if (obj.has("name")) profile.name = obj.get("name").getAsString();

            JsonElement packIdsJson = obj.get("packIds");
            Set<String> packIds = new ObjectLinkedOpenHashSet<>();
            Map<String, PackOverride> overrides = new Object2ObjectOpenHashMap<>();

            if (packIdsJson != null && packIdsJson.isJsonArray()) {
                for (JsonElement e : packIdsJson.getAsJsonArray()) {
                    if (e.isJsonPrimitive()) {
                        packIds.add(e.getAsString());
                    } else if (e.isJsonObject()) {
                        JsonObject entry = e.getAsJsonObject();
                        if (entry.has("id")) {
                            String id = entry.get("id").getAsString();
                            packIds.add(id);

                            if (entry.has("hidden") || entry.has("required") || entry.has("fixed") || entry.has("position")) {
                                PackOverride override = ctx.deserialize(entry, PackOverride.class);
                                overrides.put(id, override);
                            }
                        }
                    }
                }
            }

            JsonElement hiddenIds = obj.get("hiddenIds");
            if (hiddenIds != null && hiddenIds.isJsonArray()) {
                for (JsonElement e : hiddenIds.getAsJsonArray()) {
                    if (e.isJsonPrimitive()) {
                        overrides.computeIfAbsent(e.getAsString(), id -> new PackOverride()).setHidden(true);
                    }
                }
            }

            if (obj.has("overrides")) {
                profile.overrides = ctx.deserialize(obj.get("overrides"), new TypeToken<Map<String, PackOverride>>() {
                }.getType());
            } else {
                profile.overrides = overrides;
            }

            profile.packIds = packIds;

            return profile;
        }
    }
}
