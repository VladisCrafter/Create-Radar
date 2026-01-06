package com.happysg.radar.commands;

import com.happysg.radar.config.RadarConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DebugBeam {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("radardebug")
                        .requires(src -> src.hasPermission(2)) // op-only, change if needed
                        .then(Commands.literal("beam")
                                .executes(ctx -> toggleDebugBeams(ctx.getSource()))
                        )
        );
    }

    private static int toggleDebugBeams(CommandSourceStack source) {
        RadarConfig.DEBUG_BEAMS = !RadarConfig.DEBUG_BEAMS;

        source.sendSuccess(
                () -> Component.literal(
                        "Radar debug beams: " + (RadarConfig.DEBUG_BEAMS ? "ON" : "OFF")
                ),
                true
        );

        return 1;
    }


}
