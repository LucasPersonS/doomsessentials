package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GuildStorageActionPacket {
    public enum Action {
        PREV_PAGE, NEXT_PAGE, FILTER_CYCLE, SORT, UPGRADE, OPEN_LOGS, SEARCH
    }

    private final Action action;
    private final String searchText;

    public GuildStorageActionPacket(Action action) {
        this(action, "");
    }

    public GuildStorageActionPacket(Action action, String searchText) {
        this.action = action;
        this.searchText = searchText == null ? "" : searchText;
    }

    public static void encode(GuildStorageActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.searchText);
    }

    public static GuildStorageActionPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        String searchText = buf.readUtf();
        return new GuildStorageActionPacket(action, searchText);
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
                        case OPEN_LOGS -> {
                            if (menu != null) menu.clicked(52, 0, ClickType.PICKUP, p);
                        }
                        case SEARCH -> {
                            if (menu != null) menu.setSearchText(msg.searchText);
                        }
                    }
                } finally {
                    menu.setAllowControlAction(false);
                }
            }
        });
        c.setPacketHandled(true);
    }
}
