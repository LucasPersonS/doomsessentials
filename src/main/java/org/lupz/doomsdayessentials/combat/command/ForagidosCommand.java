package org.lupz.doomsdayessentials.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.CombatManager;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForagidosCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("foragidos")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    var server = ctx.getSource().getServer();
                    int count = 0;
                    for (UUID id : CombatManager.get().getWantedPlayers()) {
                        ServerPlayer p = server.getPlayerList().getPlayer(id);
                        int secs = CombatManager.get().getWantedRemainingSeconds(id);
                        String name = p != null ? p.getName().getString() : id.toString();
                        String status = p != null ? "online" : "offline";
                        String loc = "";
                        if (p != null) {
                            int cx = p.blockPosition().getX() >> 4;
                            int cz = p.blockPosition().getZ() >> 4;
                            loc = String.format(" ~chunk(%d,%d)", cx, cz);
                        }
                        int m = secs / 60;
                        int s = secs % 60;
                        String line = String.format("§e%s §7(%s) §f%02dm%02ds%s", name, status, m, s, loc);
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                        count++;
                    }
                    if (count == 0) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§aNenhum foragido no momento."), false);
                    }
                    return count;
                })
            )
            .then(Commands.literal("add")
                .then(Commands.argument("alvo", EntityArgument.player())
                    .then(Commands.argument("minutos", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                            int minutes = IntegerArgumentType.getInteger(ctx, "minutos");
                            CombatManager.get().addWanted(tgt.getUUID(), minutes * 60);
                            ctx.getSource().sendSuccess(() -> Component.literal("§cMarcado como foragido."), true);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("alvo", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer tgt = EntityArgument.getPlayer(ctx, "alvo");
                        CombatManager.get().removeWanted(tgt.getUUID());
                        ctx.getSource().sendSuccess(() -> Component.literal("§aRemovido da lista de foragidos."), true);
                        return 1;
                    })
                )
            )
        );
    }
}
