package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GuildStorageActionPacket {
    public enum Action {
        PREV_PAGE,
        NEXT_PAGE,
        FILTER_CYCLE,
        SORT,
        UPGRADE,
        OPEN_LOGS
    }

    private final Action action;

    public GuildStorageActionPacket(Action action) {
        this.action = action;
    }

    public static void encode(GuildStorageActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static GuildStorageActionPacket decode(FriendlyByteBuf buf) {
        Action a = buf.readEnum(Action.class);
        return new GuildStorageActionPacket(a);
    }

    public static void handle(GuildStorageActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            Player p = c.getSender();
            if (p == null) return;
            if (p.containerMenu instanceof GuildStorageMenu menu) {
                menu.setAllowControlAction(true);
                try {
                    switch (msg.action) {
                        case PREV_PAGE -> menu.clicked(45, 0, ClickType.PICKUP, p);
                        case NEXT_PAGE -> menu.clicked(53, 0, ClickType.PICKUP, p);
                        case FILTER_CYCLE -> menu.clicked(46, 0, ClickType.PICKUP, p);
                        case SORT -> menu.clicked(47, 0, ClickType.PICKUP, p);
                        case UPGRADE -> menu.clicked(50, 0, ClickType.PICKUP, p);
                        case OPEN_LOGS -> menu.clicked(52, 0, ClickType.PICKUP, p);
                    }
                } finally {
                    menu.setAllowControlAction(false);
                }
            }
        });
        c.setPacketHandled(true);
    }
}
