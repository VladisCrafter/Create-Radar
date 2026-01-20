package com.happysg.radar.block.behavior.networks.config;

import net.minecraft.nbt.*;
import java.util.ArrayList;
import java.util.List;

public record IdentificationConfig(List<String> usernames, List<Boolean> friendly, String label) {

    public static final IdentificationConfig DEFAULT = new IdentificationConfig(List.of(), List.of(), "");

    public IdentificationConfig {
        if (usernames == null) usernames = List.of();
        if (friendly == null) friendly = List.of();
        if (label == null) label = "";

        int n = Math.min(usernames.size(), friendly.size());
        if (usernames.size() != n) usernames = usernames.subList(0, n);
        if (friendly.size() != n) friendly = friendly.subList(0, n);
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();

        ListTag names = new ListTag();
        ListTag flags = new ListTag();

        for (int i = 0; i < usernames.size(); i++) {
            names.add(StringTag.valueOf(usernames.get(i)));
            flags.add(ByteTag.valueOf((byte) (friendly.get(i) ? 1 : 0)));
        }

        t.put("names", names);
        t.put("flags", flags);
        t.putString("label", label);
        return t;
    }

    public static IdentificationConfig fromTag(CompoundTag t) {
        if (t == null || t.isEmpty()) return DEFAULT;

        ListTag names = t.getList("names", Tag.TAG_STRING);
        ListTag flags = t.getList("flags", Tag.TAG_BYTE);
        int n = Math.min(names.size(), flags.size());

        List<String> outNames = new ArrayList<>(n);
        List<Boolean> outFlags = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            outNames.add(names.getString(i));
            outFlags.add(flags.getInt(i) != 0);
        }

        return new IdentificationConfig(outNames, outFlags, t.getString("label"));
    }
}
