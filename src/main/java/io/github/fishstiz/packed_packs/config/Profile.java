package io.github.fishstiz.packed_packs.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.mapOrDefault;

public class Profile implements PackOptions, Serializable {
    public static final int NAME_MAX_LENGTH = 32;
    private boolean locked = false;
    private long id;
    private String name;
    private Map<String, PackOverride> overrides;
    private Set<String> packIds = new ObjectLinkedOpenHashSet<>();

    public Profile() {
        this.overrides = new Object2ObjectOpenHashMap<>();
    }

    public Profile(String name) {
        this();
        this.name = trimName(name);
    }

    private Profile(String name, Set<String> packIds, Map<String, PackOverride> overrides) {
        this.name = trimName(name);
        this.packIds = new ObjectLinkedOpenHashSet<>(packIds);
        this.overrides = new Object2ObjectOpenHashMap<>(overrides);
        this.overrides.replaceAll((id, override) -> new PackOverride(override.hidden(), override.required(), override.position()));
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
            this.packIds = new ObjectLinkedOpenHashSet<>(PackUtil.extractPackIds(selected));
        }
    }

    public void syncPacks(Collection<Pack> available, Collection<Pack> selected) {
        if (!this.locked) {
            this.packIds = new ObjectLinkedOpenHashSet<>(PackUtil.extractPackIds(selected));
            Set<String> availableIds = new ObjectOpenHashSet<>(PackUtil.extractPackIds(available));
            this.overrides.keySet().removeIf(id -> !this.packIds.contains(id) && !availableIds.contains(id));
        }
    }

    public void setHidden(boolean hidden, Pack... packs) {
        for (Pack pack : packs) {
            this.applyOrRemoveOverride(pack.getId(), hidden, PackOverride::setHidden);
        }
    }

    public void setRequired(@Nullable Boolean required, Pack... packs) {
        for (Pack pack : packs) {
            if (Boolean.FALSE.equals(required) && PackUtil.isEssential(pack)) {
                continue;
            }

            this.applyOrRemoveOverride(pack.getId(), required, PackOverride::setRequired);
        }
    }

    public void setPosition(@Nullable PackOverride.Position position, Pack... packs) {
        for (Pack pack : packs) {
            this.applyOrRemoveOverride(pack.getId(), position, PackOverride::setPosition);
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
            if (obj.has("id")) profile.id = obj.get("id").getAsLong();
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
