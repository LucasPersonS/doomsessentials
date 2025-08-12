package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.EclipseScoreCapabilityProvider;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EclipseScoreCommand {

    private EclipseScoreCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
            Commands.literal("eclipse").then(Commands.literal("score")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                        p.getCapability(EclipseScoreCapabilityProvider.SCORE_CAPABILITY).ifPresent(cap -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("Score "+p.getName().getString()+": "+cap.getScore()+" (kills="+cap.getKills()+", deaths="+cap.getDeaths()+")" + (cap.isPermaDead()?" [PERMA]":"")), false);
                        });
                        return 1;
                    })
                )
            )
        );
    }
} 