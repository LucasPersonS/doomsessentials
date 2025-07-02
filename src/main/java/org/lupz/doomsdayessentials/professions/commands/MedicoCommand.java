package org.lupz.doomsdayessentials.professions.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lupz.doomsdayessentials.professions.MedicoProfession;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import org.lupz.doomsdayessentials.professions.MedicalHelpManager;
import java.util.UUID;

public final class MedicoCommand {
    private MedicoCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("medico")
                    .requires(src -> src.hasPermission(0))
                    .then(Commands.literal("heal")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                if (!player.getPersistentData().getBoolean("isMedico")) {
                                    player.sendSystemMessage(Component.translatable("profession.medico.not_medico"));
                                    return 0;
                                }
                                return MedicoProfession.useHealingAbility(player) ? 1 : 0;
                            }))
                    .then(Commands.literal("help").requires(src -> src.getEntity() instanceof ServerPlayer)
                            .executes(ctx -> {
                                ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                boolean ok = MedicalHelpManager.createRequest(sender);
                                if (!ok) {
                                    sender.sendSystemMessage(Component.literal("§eVocê já tem um pedido de ajuda aberto."));
                                    return 0;
                                }

                                Component acceptButton = Component.literal("[CLIQUE PARA ATENDER]")
                                        .withStyle(Style.EMPTY.withBold(true).withItalic(true).withColor(ChatFormatting.GREEN)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                        "/medico aceitar " + sender.getName().getString()))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        Component.literal("Clique para atender"))));

                                Component msg = Component.literal("§6[§cSOS§6] ")
                                        .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                        .append(Component.literal(" §6precisa de ajuda! » "))
                                        .append(acceptButton);

                                // Broadcast to all medics
                                for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                    if (p.getPersistentData().getBoolean("isMedico")) {
                                        p.sendSystemMessage(msg);
                                    }
                                }
                                sender.sendSystemMessage(Component.literal("§aPedido de ajuda enviado aos médicos online."));
                                return 1;
                            }))
                    .then(Commands.literal("aceitar")
                            .then(Commands.argument("alvo", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer medico = ctx.getSource().getPlayerOrException();
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
                                        boolean ok = MedicalHelpManager.acceptRequest(medico, target.getUUID());
                                        if (!ok) {
                                            medico.sendSystemMessage(Component.literal("§cEsse chamado já foi aceito por outro médico ou não existe."));
                                            return 0;
                                        }
                                        medico.sendSystemMessage(Component.literal("§aVocê aceitou o pedido de " + target.getName().getString() + "."));
                                        target.sendSystemMessage(Component.literal("§aO médico " + medico.getName().getString() + " está a caminho!"));
                                        return 1;
                                    })))
                    .then(Commands.literal("bed")
                            .executes(ctx -> {
                                ServerPlayer medico = ctx.getSource().getPlayerOrException();
                                if (!medico.getPersistentData().getBoolean("isMedico")) {
                                    medico.sendSystemMessage(Component.translatable("profession.medico.not_medico"));
                                    return 0;
                                }
                                return MedicoProfession.putPatientInBed(medico, medico) ? 1 : 0;
                            })
                            .then(Commands.argument("alvo", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer medico = ctx.getSource().getPlayerOrException();
                                        if (!medico.getPersistentData().getBoolean("isMedico")) {
                                            medico.sendSystemMessage(Component.translatable("profession.medico.not_medico"));
                                            return 0;
                                        }
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
                                        return MedicoProfession.putPatientInBed(medico, target) ? 1 : 0;
                                    })))
                    .then(Commands.literal("encerrar")
                            .requires(src -> src.getEntity() instanceof ServerPlayer)
                            .executes(ctx -> {
                                ServerPlayer sender = ctx.getSource().getPlayerOrException();

                                UUID requester = sender.getUUID();
                                // If sender is medico handling a request, use its target
                                if (sender.getPersistentData().hasUUID("medicoHelpTarget")) {
                                    requester = sender.getPersistentData().getUUID("medicoHelpTarget");
                                }

                                UUID medicoUuid = MedicalHelpManager.completeRequest(requester);
                                if (medicoUuid == null) {
                                    sender.sendSystemMessage(Component.literal("§eNenhum pedido foi encerrado."));
                                    return 0;
                                }
                                // Clear tag from medico
                                ServerPlayer medico = ctx.getSource().getServer().getPlayerList().getPlayer(medicoUuid);
                                if (medico != null) {
                                    medico.getPersistentData().remove("medicoHelpTarget");
                                    medico.sendSystemMessage(Component.literal("§aPedido de ajuda encerrado."));
                                }
                                if (!sender.getUUID().equals(medicoUuid)) {
                                    sender.sendSystemMessage(Component.literal("§aPedido de ajuda encerrado."));
                                }
                                return 1;
                            }))
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal("§7Uso: /medico heal|bed [jogador]|help|encerrar"));
                        return 0;
                    })
        );
    }
} 