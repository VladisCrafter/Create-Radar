package com.happysg.radar.commands;

import com.happysg.radar.block.behavior.networks.INetworkNode;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.WeaponNetworkData;
import com.happysg.radar.config.RadarConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public class DebugBeam {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("radardebug")
                        .requires(src -> src.hasPermission(2)) // op-only, change if needed
                        .then(Commands.literal("beam")
                                .executes(ctx -> toggleDebugBeams(ctx.getSource()))
                        )
        );
        dispatcher.register(
                Commands.literal("radar")
                        .requires(src -> src.hasPermission(2)) // OP-only; change if desired
                        .then(Commands.literal("dump_links")
                                .executes(ctx -> dumpLinks(ctx.getSource()))
                        )
        );
        dispatcher.register(
                Commands.literal("radar")
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.literal("debug")
                                .then(Commands.literal("filters")
                                        .executes(ctx -> dumpNetworkFilters(ctx.getSource()))
                                )
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

    private static int dumpLinks(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WeaponNetworkData data = WeaponNetworkData.get(level);

        if (data.getGroups().isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("No mount link groups found."),
                    false
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("--- Radar Mount Links ---"),
                false
        );

        for (WeaponNetworkData.Group group : data.getGroups().values()) {
            WeaponNetworkData.MountKey key = group.key;

            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "Mount: %s @ %s",
                            key.dim().location(),
                            posStr(key.mountPos())
                    )),
                    false
            );

            source.sendSuccess(() ->
                    Component.literal("  Yaw:    " + optPos(group.yawPos)), false);
            source.sendSuccess(() ->
                    Component.literal("  Pitch:  " + optPos(group.pitchPos)), false);
            source.sendSuccess(() ->
                    Component.literal("  Firing: " + optPos(group.firingPos)), false);

            if (group.dataLinks.isEmpty()) {
                source.sendSuccess(
                        () -> Component.literal("  DataLinks: <none>"),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal("  DataLinks:"),
                        false
                );
                for (BlockPos p : group.dataLinks) {
                    source.sendSuccess(
                            () -> Component.literal("    - " + posStr(p)),
                            false
                    );
                }
            }
        }

        return 1;
    }
    private static int dumpNetworkFilters(CommandSourceStack source) {
        ServerLevel level = source.getLevel();

        NetworkData data = NetworkData.get(level);

        if (data.getGroups().isEmpty()) {
            source.sendFailure(Component.literal(
                    "No network groups found."
            ));
            return 0;
        }

        source.sendSystemMessage(Component.literal(
                "=== Radar Network Filters ==="
        ).withStyle(ChatFormatting.GOLD));

        data.getGroups().forEach((key, group) -> {



            source.sendSystemMessage(Component.literal(
                    " Monitor: " + group.monitorPos
            ));

            source.sendSystemMessage(Component.literal(
                    " Radar: " + group.radarPos + " (" + group.radarKind + ")"
            ));

            source.sendSystemMessage(Component.literal(
                    " Weapon Endpoints: " + group.weaponEndpoints.size()
            ));

            source.sendSystemMessage(Component.literal(
                    " Targeting Filter:"
            ).withStyle(ChatFormatting.AQUA));

            source.sendSystemMessage(Component.literal(
                    group.targetingTag.toString()
            ).withStyle(ChatFormatting.GRAY));

            source.sendSystemMessage(Component.literal(
                    " Identification Filter:"
            ).withStyle(ChatFormatting.AQUA));

            source.sendSystemMessage(Component.literal(
                    group.identificationTag.toString()
            ).withStyle(ChatFormatting.GRAY));

            source.sendSystemMessage(Component.literal(
                    " Detection Filter:"
            ).withStyle(ChatFormatting.AQUA));

            source.sendSystemMessage(Component.literal(
                    group.detectionTag.toString()
            ).withStyle(ChatFormatting.GRAY));
        });

        return 1;
    }


    private static String posStr(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    private static String optPos(@Nullable BlockPos pos) {
        return pos == null ? "<none>" : posStr(pos);
    }

}