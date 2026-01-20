package com.happysg.radar.commands;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RadarDebugCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("radar")
                        .then(Commands.literal("debug")
                                .then(Commands.literal("weapon_endpoints")
                                        .requires(cs -> cs.hasPermission(2))
                                        .executes(ctx -> dumpWeaponEndpoints(ctx.getSource()))
                                )
                        )
        );
    }

    private static int dumpWeaponEndpoints(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        NetworkData data = NetworkData.get(level);

        source.sendSuccess(() ->
                        Component.literal("=== Radar Weapon Endpoints Dump ===")
                                .withStyle(ChatFormatting.GOLD),
                false
        );

        if (data.getGroupsByFiltererView().isEmpty()) {
            source.sendSuccess(() ->
                            Component.literal("No filter groups found.")
                                    .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 1;
        }

        for (NetworkData.Group group : data.getGroupsByFiltererView().values()) {
            BlockPos filtererPos = group.key.filtererPos();
            ResourceKey<Level> dim = group.key.dim();

            source.sendSuccess(() ->
                            Component.literal("")
                                    .append(Component.literal("[FilterGroup] ").withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal("Filterer @ "))
                                    .append(Component.literal(dim.location().toString())
                                            .withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(" "))
                                    .append(Component.literal(filtererPos.toShortString())
                                            .withStyle(ChatFormatting.GREEN)),
                    false
            );
            if(group.monitorPos !=null) {
                source.sendSuccess(() ->
                        Component.literal("- Monitor pos: " + group.monitorPos.toString())
                                .withStyle(ChatFormatting.GRAY), false
                );
                continue;
            }

            if (group.weaponEndpoints.isEmpty()) {
                source.sendSuccess(() ->
                                Component.literal(" - Weapon endpoints: <none>")
                                        .withStyle(ChatFormatting.DARK_GRAY),
                        false
                );
                continue;
            }

            source.sendSuccess(() ->
                            Component.literal(" - Weapon endpoints (" + group.weaponEndpoints.size() + "):")
                                    .withStyle(ChatFormatting.GRAY),
                    false
            );

            for (BlockPos ep : group.weaponEndpoints) {
                source.sendSuccess(() ->
                                Component.literal("   • ")
                                        .append(Component.literal(ep.toShortString())
                                                .withStyle(ChatFormatting.WHITE)),
                        false
                );
            }
        }

        source.sendSuccess(() ->
                        Component.literal("=== End Dump ===")
                                .withStyle(ChatFormatting.GOLD),
                false
        );

        return 1;
    }
}