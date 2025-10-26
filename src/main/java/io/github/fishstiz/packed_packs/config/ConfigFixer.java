package io.github.fishstiz.packed_packs.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.SerializedName;
import io.github.fishstiz.packed_packs.PackedPacks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Deprecated(forRemoval = true)
class ConfigFixer {
    static File getPreviousPreferencesFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(PackedPacks.MOD_ID + ".preferences.properties").toFile();
    }

    static File getPreviousConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(PackedPacks.MOD_ID + ".json").toFile();
    }

    static void migrateConfig(Config config) {
        Instant now = Instant.now();
        PackedPacks.LOGGER.info("[packed_packs] Migrating config...");

        DevConfig devConfig = DevConfig.get();

        try {
            List<Profile> datapackProfiles = config.getDatapacks().oldProfiles;
            List<Profile> resourcepackProfiles = config.getResourcepacks().oldProfiles;

            config.save();

            devConfig.getDatapacks().setDefaultProfile(config.getDatapacks().oldDefaultProfile);
            devConfig.getResourcepacks().setDefaultProfile(config.getResourcepacks().oldDefaultProfile);

            devConfig.save();

            if (!datapackProfiles.isEmpty()) {
                config.getDatapacks().availableProfiles = datapackProfiles;
                for (Profile profile : datapackProfiles) {
                    Profiles.save(config.getDatapacks().packType(), profile);
                    PackedPacks.LOGGER.info("[packed_packs] Migrated resource pack profile '{}' with id '{}'", profile.getName(), profile.getId());
                }
            }
            if (!resourcepackProfiles.isEmpty()) {
                config.getResourcepacks().availableProfiles = resourcepackProfiles;
                for (Profile profile : resourcepackProfiles) {
                    Profiles.save(config.getResourcepacks().packType(), profile);
                    PackedPacks.LOGGER.info("[packed_packs] Migrated data pack profile '{}' with id '{}'", profile.getName(), profile.getId());
                }
            }


            PackedPacks.LOGGER.info("[packed_packs] Config migrated in {} ms.", Duration.between(now, Instant.now()).toMillis());
        } catch (Exception e) {
            PackedPacks.LOGGER.warn("[packed_packs] Failed to migrate config. ", e);
        }
    }

    static class ProfileExclusionStrategy implements ExclusionStrategy {
        private static final String DEFAULT_PROFILE = "defaultProfile";
        private static final String PROFILES_ARRAY = "profiles";
        private static final String PROFILE_ID = "id";

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            if (Config.Packs.class.isAssignableFrom(f.getDeclaringClass())) {
                if (f.getName().equals(PROFILES_ARRAY) || f.getName().equals(DEFAULT_PROFILE)) {
                    return true;
                }
                SerializedName serializedName = f.getAnnotation(SerializedName.class);
                if (serializedName != null) {
                    if (serializedName.value().equals(PROFILES_ARRAY)) {
                        return true;
                    }
                    if (serializedName.value().equals(DEFAULT_PROFILE)) {
                        return true;
                    }
                }
            }
            if (f.getDeclaringClass().equals(Profile.class) && f.getName().equals(PROFILE_ID)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
