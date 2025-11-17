package org.lupz.doomsdayessentials.professions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.professions.MedicoProfession;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;

import java.util.UUID;
import java.util.function.Supplier;

public class SelectProfessionPacket {

    private final String professionId;

    public SelectProfessionPacket(String professionId) {
        this.professionId = professionId;
    }

    public static void encode(SelectProfessionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.professionId);
    }

    public static SelectProfessionPacket decode(FriendlyByteBuf buf) {
        return new SelectProfessionPacket(buf.readUtf());
    }

    public static void handle(SelectProfessionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String current = ProfissaoManager.getProfession(player.getUUID());

            boolean isCreative = player.isCreative();

            // Prevent changing profession unless creative mode
            if (!isCreative && current != null) {
                player.sendSystemMessage(Component.translatable("profession.change.denied"));
                return;
            }

            if ("abandonar".equalsIgnoreCase(msg.professionId)) {
                // Remove from registry first
                ProfissaoManager.removeProfession(player.getUUID());

                // Apply leave logic specific to current profession
                if ("medico".equalsIgnoreCase(current)) {
                    MedicoProfession.onLeaveMedico(player);
                } else if ("combatente".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.CombatenteProfession.onLeave(player);
                } else if ("rastreador".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.RastreadorProfession.onLeave(player);
                } else if ("engenheiro".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.EngenheiroProfession.onLeave(player);
                } else if ("armeiro".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.ArmeiroProfession.onLeave(player);
                }

                player.sendSystemMessage(Component.translatable("profession.leave"));

                // Fecha e reabre menu para atualizar itens disponíveis
                player.closeContainer();
                net.minecraftforge.network.NetworkHooks.openScreen(player, new org.lupz.doomsdayessentials.professions.menu.ProfissoesMenuProvider());
                return;
            }

            // Trying to pick a new profession: validate limits in config
            if (!ProfissaoManager.canBecome(msg.professionId)) {
                String professionNameColored = switch (msg.professionId.toLowerCase()) {
                    case "medico" -> "§eMédicos";
                    case "combatente" -> "§cCombatentes";
                    case "rastreador" -> "§bRastreadores";
                    default -> msg.professionId;
                };
                player.sendSystemMessage(Component.literal("§cO limite de " + professionNameColored + " foi atingido. Não é possível se tornar um agora."));
                EssentialsMod.LOGGER.info("Player {} tried to become a {} but the limit has been reached.", player.getName().getString(), msg.professionId);
                return;
            }

            // Check if trying to select the same profession
            if (current != null && current.equalsIgnoreCase(msg.professionId)) {
                player.sendSystemMessage(Component.literal("§eVocê já é um " + msg.professionId + "."));
                EssentialsMod.LOGGER.info("Player {} tried to select {} but already has it.", 
                    player.getName().getString(), msg.professionId);
                return;
            }
            
            // IMPORTANT: Clean up old profession BEFORE switching to new one
            if (current != null && !current.isEmpty()) {
                EssentialsMod.LOGGER.info("Player {} is switching from {} to {}, cleaning up old profession...", 
                    player.getName().getString(), current, msg.professionId);
                
                // Call onLeave for the current profession to clean up passives
                if ("medico".equalsIgnoreCase(current)) {
                    MedicoProfession.onLeaveMedico(player);
                } else if ("combatente".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.CombatenteProfession.onLeave(player);
                } else if ("rastreador".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.RastreadorProfession.onLeave(player);
                } else if ("engenheiro".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.EngenheiroProfession.onLeave(player);
                } else if ("armeiro".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.ArmeiroProfession.onLeave(player);
                } else if ("cacador".equalsIgnoreCase(current)) {
                    org.lupz.doomsdayessentials.professions.CacadorProfession.onLeave(player);
                }
                
                player.sendSystemMessage(Component.literal("§7Você deixou de ser " + current + "."));
            }

            // Clear ALL profession tags before setting new one (extra safety)
            ProfissaoManager.clearAllProfessionTags(player);
            
            // Now set the new profession
            ProfissaoManager.setProfession(player.getUUID(), msg.professionId);
            
            // Apply the new profession's benefits
            if ("medico".equalsIgnoreCase(msg.professionId)) {
                MedicoProfession.onBecomeMedico(player);
            } else if ("combatente".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.CombatenteProfession.onBecome(player);
            } else if ("rastreador".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.RastreadorProfession.onBecome(player);
            } else if ("engenheiro".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.EngenheiroProfession.onBecome(player);
            } else if ("cacador".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.CacadorProfession.onBecome(player);
            } else if ("armeiro".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.ArmeiroProfession.onBecome(player);
            } else {
                player.sendSystemMessage(Component.literal("Você se tornou um " + msg.professionId + "."));
            }
            EssentialsMod.LOGGER.info("Player {} selected profession: {}", player.getName().getString(), msg.professionId);
        });
        ctx.get().setPacketHandled(true);
    }
} 