package org.lupz.doomsdayessentials.trail.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.trail.TrailManager;
import org.lupz.doomsdayessentials.trail.TrailData;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TrailCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new TrailCommand(event.getDispatcher(), event.getBuildContext());
    }

    public TrailCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("trail")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("particle", ParticleArgument.particle(context))
                    // Default command: /trail <player> <particle>
                    .executes(ctx -> setTrail(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"),
                        ParticleArgument.getParticle(ctx, "particle"),
                        0.1f, 0.5f, 0.1f, 0.1f, 10 // Default values
                    ))
                    // Full command with all options
                    .then(Commands.argument("dx", FloatArgumentType.floatArg())
                        .then(Commands.argument("dy", FloatArgumentType.floatArg())
                            .then(Commands.argument("dz", FloatArgumentType.floatArg())
                                .then(Commands.argument("speed", FloatArgumentType.floatArg())
                                    .then(Commands.argument("count", IntegerArgumentType.integer())
                                        .executes(ctx -> setTrail(
                                            ctx.getSource(),
                                            EntityArgument.getPlayer(ctx, "player"),
                                            ParticleArgument.getParticle(ctx, "particle"),
                                            FloatArgumentType.getFloat(ctx, "dx"),
                                            FloatArgumentType.getFloat(ctx, "dy"),
                                            FloatArgumentType.getFloat(ctx, "dz"),
                                            FloatArgumentType.getFloat(ctx, "speed"),
                                            IntegerArgumentType.getInteger(ctx, "count")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );

        dispatcher.register(Commands.literal("untrail")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> removeTrail(
                    ctx.getSource(),
                    EntityArgument.getPlayer(ctx, "player")
                ))
            )
        );
    }

    private int setTrail(CommandSourceStack source, ServerPlayer player, ParticleOptions particle, float dx, float dy, float dz, float speed, int count) {
        TrailData trailData = new TrailData(particle, dx, dy, dz, speed, count);
        TrailManager.getInstance().setTrail(player.getUUID(), trailData);
        source.sendSuccess(() -> Component.literal("Trail set for " + player.getName().getString()), true);
        return 1;
    }

    private int removeTrail(CommandSourceStack source, ServerPlayer player) {
        TrailManager.getInstance().removeTrail(player.getUUID());
        source.sendSuccess(() -> Component.literal("Trail removed for " + player.getName().getString()), true);
        return 1;
    }
} 