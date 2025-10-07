package io.github.fishstiz.packed_packs.config;

import com.google.gson.*;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;

import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.mapOrDefault;

@SuppressWarnings({"FieldMayBeFinal", "MismatchedQueryAndUpdateOfCollection"})
public class Profile implements PackOptions, Serializable {
    public static final int NAME_MAX_LENGTH = 32;
    private long id;
    private String name;
    private boolean locked = false;
    private Map<String, PackEntry> packIds = new Object2ObjectLinkedOpenHashMap<>();

    public Profile() {
    }

    public Profile(String name) {
        this();
        this.name = trimName(name);
    }

    private Profile(String name, List<String> packIds) {
        this(name);
        this.packIds = this.toMap(packIds);
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
        if (!this.locked) this.name = trimName(name);
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds.keySet());
    }

    public void setPacks(List<Pack> packs) {
        if (!this.locked) {
            this.packIds = this.toMap(PackUtil.extractPackIds(packs));
        }
    }

    public void setRequired(boolean required, Pack... packs) {
        for (Pack pack : packs) {
            PackEntry entry = this.packIds.get(pack.getId());
            if (entry != null) {
                this.packIds.put(pack.getId(), new PackEntry(pack.getId(), entry.hidden(), required, entry.fixed()));
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

    public void setHidden(boolean hidden, Pack... packs) {
        for (Pack pack : packs) {
            PackEntry entry = this.packIds.get(pack.getId());
            if (entry != null) {
                this.packIds.put(pack.getId(), new PackEntry(pack.getId(), hidden, entry.required(), entry.fixed()));
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
        return Boolean.TRUE.equals(mapOrDefault(this.packIds.get(pack.getId()), pack.isRequired(), PackEntry::required));
    }

    @Override
    public boolean isFixed(Pack pack) {
        return mapOrDefault(this.packIds.get(pack.getId()), pack.isFixedPosition(), packEntry -> packEntry.fixed != null);
    }

    @Override
    public Pack.Position getPosition(Pack pack) {
        PackEntry entry = this.packIds.get(pack.getId());
        if (entry != null && entry.fixed != null) {
            return entry.fixed.position;
        }
        return pack.getDefaultPosition();
    }

    @Override
    public PackSelectionConfig getSelectionConfig(Pack pack) {
        PackEntry packEntry = this.packIds.get(pack.getId());

        if (packEntry != null) {
            return new PackSelectionConfig(this.isRequired(pack), this.getPosition(pack), this.isFixed(pack));
        }

        return pack.selectionConfig();
    }

    // does not copy pack entry options
    public Profile copy() {
        String profileName = this.name;

        if (profileName != null && !profileName.isBlank()) {
            profileName += " - " + ResourceUtil.getText("profile.copy").getString();
        }

        return new Profile(profileName, this.getPackIds());
    }

    private Map<String, PackEntry> toMap(List<String> packIds) {
        Map<String, PackEntry> entryMap = new Object2ObjectLinkedOpenHashMap<>();

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

    static class PackEntryMapAdapter implements JsonSerializer<Map<String, PackEntry>>, JsonDeserializer<Map<String, PackEntry>> {
        @Override
        public JsonElement serialize(Map<String, PackEntry> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray arr = new JsonArray();
            for (PackEntry entry : src.values()) {
                if (entry.hidden() == null && entry.required() == null && entry.fixed() == null) {
                    arr.add(entry.id());
                } else {
                    arr.add(context.serialize(entry));
                }
            }
            return arr;
        }

        @Override
        public Map<String, PackEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Map<String, PackEntry> map = new Object2ObjectLinkedOpenHashMap<>();
            for (JsonElement el : json.getAsJsonArray()) {
                PackEntry entry = context.deserialize(el, PackEntry.class);
                map.put(entry.id(), entry);
            }
            return map;
        }
    }

    public record PackEntry(
            String id,
            @Nullable Boolean hidden,
            @Nullable Boolean required,
            @Nullable SerializedPosition fixed
    ) implements Serializable {
        private static final String ID_SERIALIZED_NAME = "id";
        private static final String HIDDEN_SERIALIZED_NAME = "hidden";
        private static final String REQUIRED_SERIALIZED_NAME = "required";
        private static final String FIXED_SERIALIZED_NAME = "fixed";

        public PackEntry(String id) {
            this(id, null, null, null);
        }

        enum SerializedPosition {
            TOP(Pack.Position.TOP),
            BOTTOM(Pack.Position.BOTTOM);

            final Pack.Position position;

            SerializedPosition(Pack.Position position) {
                this.position = position;
            }

            public static SerializedPosition get(Pack.Position position) {
                return switch (position) {
                    case TOP -> TOP;
                    case BOTTOM -> BOTTOM;
                };
            }
        }

        static class Adapter implements JsonSerializer<PackEntry>, JsonDeserializer<PackEntry> {
            @Override
            public JsonElement serialize(PackEntry src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", src.id());
                if (src.hidden() != null) obj.addProperty(HIDDEN_SERIALIZED_NAME, src.hidden());
                if (src.required() != null) obj.addProperty(REQUIRED_SERIALIZED_NAME, src.required());
                if (src.fixed() != null) obj.addProperty(FIXED_SERIALIZED_NAME, src.fixed().name());
                return obj;
            }

            @Override
            public PackEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                    return new PackEntry(json.getAsString());
                }

                JsonObject obj = json.getAsJsonObject();

                String id = Objects.requireNonNull(obj.get(ID_SERIALIZED_NAME).getAsString(), "'id' in pack entry must not be null");
                Boolean hidden = obj.has(HIDDEN_SERIALIZED_NAME) ? obj.get(HIDDEN_SERIALIZED_NAME).getAsBoolean() : null;
                Boolean required = obj.has(REQUIRED_SERIALIZED_NAME) ? obj.get(REQUIRED_SERIALIZED_NAME).getAsBoolean() : null;
                SerializedPosition fixed = null;
                if (obj.has(FIXED_SERIALIZED_NAME)) {
                    String position = obj.get(FIXED_SERIALIZED_NAME).getAsString();
                    try {
                        fixed = SerializedPosition.valueOf(position.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        PackedPacks.LOGGER.error(
                                "[packed_packs] Invalid value for key 'fixed': '{}' in {}. Expected one of {}",
                                position, id, SerializedPosition.values()
                        );
                    }
                }

                return new PackEntry(id, hidden, required, fixed);
            }
        }
    }
}
