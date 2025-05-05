package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.server.packs.repository.Pack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Profile implements Serializable {
    public static final int NAME_MAX_LENGTH = 32;
    private int id;
    private String name = "";
    private List<String> packIds = new ArrayList<>();

    public Profile(String name) {
        this.name = trimName(name);
    }

    private Profile(String name, List<String> packIds) {
        this(name);
        this.packIds = List.copyOf(packIds);
    }

    void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = trimName(name);
    }

    public List<String> getPackIds() {
        return this.packIds;
    }

    public void setPacks(List<Pack> packs) {
        List<String> ids = new ArrayList<>();
        for (Pack pack : packs) {
            if (pack != null) ids.add(pack.getId());
        }
        this.packIds = ids;
    }

    public Profile copy() {
        String profileName = this.name;

        if (profileName != null && !profileName.isBlank()) {
            profileName += " - " + ResourceUtil.getText("profile.copy").getString();
        }

        return new Profile(profileName, this.packIds);
    }

    private static String trimName(String name) {
        if (name == null) return null;
        return name.length() <= NAME_MAX_LENGTH ? name : name.substring(0, NAME_MAX_LENGTH);
    }
}
