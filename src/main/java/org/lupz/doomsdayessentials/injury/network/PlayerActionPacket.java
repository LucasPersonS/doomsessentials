package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.professions.MedicalHelpManager;
import org.lupz.doomsdayessentials.injury.InjuryEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;

import java.util.function.Supplier;

public class PlayerActionPacket {

    private final Action action;

    public PlayerActionPacket(Action action) {
        this.action = action;
    }

    public PlayerActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (this.action) {
                case GIVE_UP:
                    InjuryEvents.killPlayer(player);
                    break;
                case CALL_MEDIC:
                    // Attempt to create request; if already open, inform player
                    boolean ok = MedicalHelpManager.createRequest(player);
                    if (!ok) {
                        player.sendSystemMessage(Component.literal("§eVocê já tem um pedido de ajuda aberto."));
                        break;
                    }

                    // Build clickable message identical to /medico help command
                    Component acceptButton = Component.literal("[CLIQUE PARA ATENDER]")
                            .withStyle(Style.EMPTY.withBold(true).withItalic(true).withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/medico aceitar " + player.getName().getString()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Clique para atender"))));

                    Component msg = Component.literal("§6[§cSOS§6] ")
                            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(Component.literal(" §6precisa de ajuda! » "))
                            .append(acceptButton);

                    // Broadcast to all online Médicos
                    for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                        if ("medico".equalsIgnoreCase(ProfissaoManager.getProfession(p.getUUID()))) {
                            p.sendSystemMessage(msg);
                        }
                    }

                    player.sendSystemMessage(Component.literal("§aPedido de ajuda enviado aos médicos online."));
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }


    public enum Action {
        GIVE_UP,
        CALL_MEDIC
    }
} 