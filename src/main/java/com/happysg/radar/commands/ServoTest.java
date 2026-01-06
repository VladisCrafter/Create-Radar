package com.happysg.radar.commands;

import com.happysg.radar.block.Test.TestBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.sun.jdi.connect.Connector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ServoTest {

    // How far the player can target a block
    private static final double REACH = 6.0D;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("servo_angle")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("angle", FloatArgumentType.floatArg(0f, 360f))
                                .then(Commands.argument("enabled",BoolArgumentType.bool())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();

                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("This command must be run by a player."));
                                        return 0;
                                    }
                                    boolean enabled = BoolArgumentType.getBool(ctx,"enabled");
                                    float angle = FloatArgumentType.getFloat(ctx, "angle");
                                    Level level = player.level();

                                    HitResult hit = player.pick(REACH, 0.0F, false); // false = don't include fluids
                                    if (hit.getType() != HitResult.Type.BLOCK) {
                                        source.sendFailure(Component.literal("You're not looking at a block."));
                                        return 0;
                                    }

                                    BlockHitResult bhr = (BlockHitResult) hit;
                                    var pos = bhr.getBlockPos();

                                    BlockEntity be = level.getBlockEntity(pos);
                                    if (be instanceof TestBlockEntity test) {
                                            test.setOutputAttached(enabled);
                                            if (enabled){
                                                test.setOutputTargetRPM(angle);
                                            }
                                    }

                                    source.sendSuccess(
                                            () -> Component.literal("Set servo at " + pos.toShortString() + " to " + angle + "°"),
                                            true
                                    );

                                    return 1;
                                })
                        )
        ));
    }
}
