package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.professions.MedicoProfession;

public final class MedicoCommand {
    private MedicoCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("medico")
                    .requires(src -> {
                        if (!src.hasPermission(0)) return false;
                        if (!(src.getEntity() instanceof ServerPlayer sp)) return true; // allow console
                        return sp.getPersistentData().getBoolean("isMedico");
                    })
                    .then(Commands.literal("heal").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        return MedicoProfession.useHealingAbility(player) ? 1 : 0;
                    }))
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal("§7Uso: /medico heal"));
                        return 0;
                    })
        );
    }
} 