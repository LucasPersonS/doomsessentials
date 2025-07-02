package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.professions.RastreadorProfession;

public final class RastreadorCommand {
    private RastreadorCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rastreador")
                        .requires(src -> {
                            if (!src.hasPermission(0)) return false;
                            if (!(src.getEntity() instanceof ServerPlayer sp)) return true;
                            return sp.getPersistentData().getBoolean("isRastreador");
                        })
                        .then(Commands.literal("glow").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return RastreadorProfession.useGlowAbility(player) ? 1 : 0;
                        }))
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("§7Uso: /rastreador glow"));
                            return 0;
                        })
        );
    }
} 