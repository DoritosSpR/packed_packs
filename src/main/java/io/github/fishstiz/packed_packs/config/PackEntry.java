package io.github.fishstiz.packed_packs.config;

import com.google.gson.*;
import io.github.fishstiz.packed_packs.PackedPacks;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public record PackEntry(
        String id,
        @Nullable Boolean hidden,
        @Nullable Boolean required,
        @Nullable PackEntry.SerializedPosition fixed
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

        private final Pack.Position position;

        SerializedPosition(Pack.Position position) {
            this.position = position;
        }

        public Pack.Position pos() {
            return this.position;
        }

        static SerializedPosition get(Pack.Position position) {
            return switch (position) {
                case TOP -> TOP;
                case BOTTOM -> BOTTOM;
            };
        }
    }

    static class PackMap extends Object2ObjectLinkedOpenHashMap<String, PackEntry> {
        PackMap() {
        }

        PackMap(Map<String, PackEntry> map) {
            super(map);
        }
    }

    static class MapAdapter implements JsonSerializer<PackMap>, JsonDeserializer<PackMap> {
        @Override
        public JsonElement serialize(PackMap src, Type typeOfSrc, JsonSerializationContext context) {
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
        public PackMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            PackMap map = new PackMap();
            for (JsonElement el : json.getAsJsonArray()) {
                PackEntry entry = context.deserialize(el, PackEntry.class);
                map.put(entry.id(), entry);
            }
            return map;
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
