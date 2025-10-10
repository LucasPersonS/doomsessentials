package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildMember;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

import java.util.Comparator;
import java.util.List;

public class GuildMembersMenu extends AbstractContainerMenu {
    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private Guild guild;

    public GuildMembersMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.GUILD_MEMBERS_MENU.get(), windowId);
        this.player = inv.player;
        if (player instanceof ServerPlayer sp) {
            ServerLevel lvl = sp.serverLevel();
            this.guild = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
        }
        rebuild();
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(cont, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
                    @Override public boolean mayPickup(@NotNull Player p) { return false; }
                });
            }
        }
        int invTopY = 84 + (6 - 3) * 18; // 138 for 6 rows
        int hotbarY = 142 + (6 - 3) * 18; // 196
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invTopY + row * 18));
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col, 8 + col * 18, hotbarY));
    }

    private void rebuild() {
        for (int i = 0; i < 54; i++) cont.setItem(i, ItemStack.EMPTY);
        if (guild == null) return;
        List<GuildMember> members = new java.util.ArrayList<>(guild.getMembers());
        members.sort(Comparator.comparing((GuildMember m) -> m.getRank().ordinal()).thenComparing(GuildMember::getPlayerUUID));
        int i = 0;
        for (GuildMember m : members) {
            if (i >= 54) break;
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            boolean online = false;
            String name = playerName(m.getPlayerUUID());
            if (player instanceof ServerPlayer sp) {
                online = sp.getServer().getPlayerList().getPlayer(m.getPlayerUUID()) != null;
            }
            String joinStr = joinDateString(m);
            head.setHoverName(Component.literal(rankPrefix(m.getRank()) + (online ? "§a" : "§c") + name));
            addLore(head, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Entrada: §f" + joinStr),
                    Component.literal(online ? "§aOnline" : "§cOffline")
            )));
            cont.setItem(i++, head);
        }
        ItemStack back = new ItemStack(Items.PAPER);
        back.setHoverName(Component.literal("§eVoltar"));
        cont.setItem(49, back);
    }

    private String playerName(java.util.UUID uuid) {
        if (player instanceof ServerPlayer sp) {
            var s = sp.getServer();
            var online = s.getPlayerList().getPlayer(uuid);
            if (online != null) return online.getGameProfile().getName();
            return s.getProfileCache().get(uuid).map(com.mojang.authlib.GameProfile::getName).orElse("?");
        }
        return "?";
    }

    private String rankPrefix(GuildMember.Rank r) {
        return switch (r) {
            case LEADER -> "§6[Líder] §a";
            case OFFICER -> "§b[Co-Líder] §a";
            case MEMBER -> "§7[Membro] §f";
        };
    }

    private String joinDateString(GuildMember m) {
        java.time.Instant inst = java.time.Instant.ofEpochMilli(m.getJoinTimestamp());
        java.time.ZonedDateTime z = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.of("America/Sao_Paulo"));
        return String.format("%02d/%02d/%04d", z.getDayOfMonth(), z.getMonthValue(), z.getYear());
    }

    private void addLore(ItemStack stack, java.util.List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp)) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        if (slotId == 49 && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv), Component.literal("Organização")));
            return;
        }
        // Open per-member actions when a head is clicked
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < 54) {
            int index = slotId;
            if (guild != null) {
                java.util.List<GuildMember> list = new java.util.ArrayList<>(guild.getMembers());
                list.sort(java.util.Comparator.comparing((GuildMember mm) -> mm.getRank().ordinal()).thenComparing(GuildMember::getPlayerUUID));
                if (index < list.size()) {
                    java.util.UUID target = list.get(index).getPlayerUUID();
                    sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMemberActionsMenu(id, inv, target), Component.literal("Gerenciar Membro")));
                    return;
                }
            }
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }
}


