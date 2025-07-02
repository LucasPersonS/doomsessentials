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

            ProfissaoManager.setProfession(player.getUUID(), msg.professionId);
            if ("medico".equalsIgnoreCase(msg.professionId)) {
                MedicoProfession.onBecomeMedico(player);
            } else if ("combatente".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.CombatenteProfession.onBecome(player);
            } else if ("rastreador".equalsIgnoreCase(msg.professionId)) {
                org.lupz.doomsdayessentials.professions.RastreadorProfession.onBecome(player);
            } else {
                player.sendSystemMessage(Component.literal("Você se tornou um " + msg.professionId + "."));
            }
            EssentialsMod.LOGGER.info("Player {} selected profession: {}", player.getName().getString(), msg.professionId);
        });
        ctx.get().setPacketHandled(true);
    }
} 