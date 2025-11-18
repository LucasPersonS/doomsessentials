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

/**
 * Dedicated, paginated menu to invite players to the guild.
 * Shows only eligible players (online and not in any guild) and supports
 * Prev/Next paging when there are many online players.
 */
public class GuildInviteMenu extends AbstractContainerMenu {
    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private Guild guild;
    private int page = 0; // zero-based page index

    private static final int SLOT_BACK = 49;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_NEXT = 50;

    public GuildInviteMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.GUILD_MEMBERS_MENU.get(), windowId); // reuse same type for simplicity
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
        int yBase = 18 + 6 * 18 + 4;
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, yBase + row * 18));
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col, 8 + col * 18, yBase + 58));
    }

    private void rebuild() {
        for (int i = 0; i < 54; i++) cont.setItem(i, ItemStack.EMPTY);
        if (guild == null) return;

        // Controls: Back, Prev, Next
        ItemStack back = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_BACK.get());
        back.setHoverName(Component.literal("§eVoltar"));
        cont.setItem(SLOT_BACK, back);
        ItemStack prev = new ItemStack(Items.ARROW);
        prev.setHoverName(Component.literal("§eAnterior"));
        cont.setItem(SLOT_PREV, prev);
        ItemStack next = new ItemStack(Items.ARROW);
        next.setHoverName(Component.literal("§ePróximo"));
        cont.setItem(SLOT_NEXT, next);

        if (player instanceof ServerPlayer sp) {
            GuildsManager gm = GuildsManager.get(sp.serverLevel());
            GuildMember self = guild.getMember(sp.getUUID());
            boolean canInvite = self != null && (self.getRank() == GuildMember.Rank.LEADER || self.getRank() == GuildMember.Rank.OFFICER);
            if (!canInvite) {
                ItemStack barrier = new ItemStack(Items.BARRIER);
                barrier.setHoverName(Component.literal("§cApenas líder/co-líder pode convidar"));
                cont.setItem(4, barrier);
                return;
            }

            java.util.List<ServerPlayer> candidates = new java.util.ArrayList<>(sp.getServer().getPlayerList().getPlayers());
            // Filter eligible: not self, not in any guild
            candidates.removeIf(p2 -> p2.getUUID().equals(sp.getUUID()) || gm.getGuildByMember(p2.getUUID()) != null);
            candidates.sort(java.util.Comparator.comparing(p2 -> p2.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER));

            // Paging over first 45 slots (0..44). Bottom row reserved for controls.
            int pageSize = 45;
            int total = candidates.size();
            int maxPage = Math.max(0, (total - 1) / pageSize);
            if (page > maxPage) page = maxPage;
            int start = page * pageSize;
            int end = Math.min(start + pageSize, total);
            int idx = 0;
            for (int i = start; i < end; i++) {
                ServerPlayer target = candidates.get(i);
                boolean invited = gm.hasInvite(target.getUUID(), guild.getName());
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.setHoverName(Component.literal("§a" + target.getGameProfile().getName()));
                addLore(head, new java.util.ArrayList<>(java.util.List.of(
                        Component.literal(invited ? "§eConvite já enviado" : "§7Clique para convidar"),
                        Component.literal("§7Para: §6" + guild.getTag())
                )));
                cont.setItem(idx++, head);
            }

            // Indicators for page at center top if desired
            ItemStack info = new ItemStack(Items.PAPER);
            info.setHoverName(Component.literal("§7Página " + (page + 1) + " / " + Math.max(1, (maxPage + 1))));
            cont.setItem(4, info);
        }
    }

    private void addLore(ItemStack stack, java.util.List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp)) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        if (guild == null) guild = GuildsManager.get(sp.serverLevel()).getGuildByMember(sp.getUUID());
        if (slotId == SLOT_BACK) { sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMembersMenu(id, inv), Component.literal("Membros"))); return; }
        if (slotId == SLOT_PREV) { if (page > 0) { page--; rebuild(); broadcastChanges(); } return; }
        if (slotId == SLOT_NEXT) { page++; rebuild(); broadcastChanges(); return; }
        if (guild == null) return;
        GuildMember self = guild.getMember(sp.getUUID());
        if (self == null || (self.getRank() != GuildMember.Rank.LEADER && self.getRank() != GuildMember.Rank.OFFICER)) { sp.sendSystemMessage(Component.literal("§cSem permissão para convidar.")); return; }

        // Candidates are in slots 0..44 (paged)
        if (slotId >= 0 && slotId < 45) {
            GuildsManager gm = GuildsManager.get(sp.serverLevel());
            java.util.List<ServerPlayer> candidates = new java.util.ArrayList<>(sp.getServer().getPlayerList().getPlayers());
            candidates.removeIf(p2 -> p2.getUUID().equals(sp.getUUID()) || gm.getGuildByMember(p2.getUUID()) != null);
            candidates.sort(java.util.Comparator.comparing(p2 -> p2.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER));
            int index = page * 45 + slotId;
            if (index >= 0 && index < candidates.size()) {
                ServerPlayer target = candidates.get(index);
                if (gm.hasInvite(target.getUUID(), guild.getName())) {
                    sp.sendSystemMessage(Component.literal("§eConvite já enviado para " + target.getGameProfile().getName() + "."));
                    return;
                }
                gm.invitePlayer(guild.getName(), target.getUUID());
                sp.sendSystemMessage(Component.literal("§aConvite enviado para " + target.getGameProfile().getName() + "."));
                target.sendSystemMessage(Component.literal("§eVocê recebeu um convite para §6" + guild.getTag() + "§e. Abra §f/organizacao§e para aceitar."));
                rebuild(); broadcastChanges();
                return;
            }
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }
}

