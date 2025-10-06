package com.happysg.radar.block.controller.yaw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.Lang;
import net.minecraft.network.chat.Component;

public enum InstSpeedMod {

    FORWARD_FAST(2, ">>"), FORWARD(1, "->"), BACK(-1, "<-"), BACK_FAST(-2, "<<"),

    ;

    String translationKey;
    int value;
    Component label;

    private InstSpeedMod(int modifier, Component label) {
        this.label = label;
        translationKey = "gui.sequenced_gearshift.speed." + Lang.asId(name());
        value = modifier;
    }
    private InstSpeedMod(int modifier, String label) {
        this.label = Component.literal(label);
        translationKey = "gui.sequenced_gearshift.speed." + Lang.asId(name());
        value = modifier;
    }

    static List<Component> getOptions() {
        List<Component> options = new ArrayList<>();
        for (InstSpeedMod entry : values())
            options.add(CreateLang.translateDirect(entry.translationKey));
        return options;
    }

    public static InstSpeedMod getByModifier(int modifier) {
        return Arrays.stream(InstSpeedMod.values())
                .filter(speedModifier -> speedModifier.value == modifier)
                .findAny()
                .orElse(InstSpeedMod.FORWARD);
    }

}
