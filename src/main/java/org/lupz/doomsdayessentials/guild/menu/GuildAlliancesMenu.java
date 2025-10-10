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
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.guild.GuildMember;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

public class GuildAlliancesMenu extends AbstractContainerMenu {
    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private Guild guild;

    private static final int SLOT_BACK = 49;

    public GuildAlliancesMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.GUILD_ALLIANCES_MENU.get(), windowId);
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
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, yBase + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, yBase + 58));
        }
    }

    private void rebuild() {
        for (int i = 0; i < 54; i++) cont.setItem(i, ItemStack.EMPTY);
        if (guild == null) return;
        int idx = 0;
        // Show current alliances first
        for (String allyName : guild.getAllies()) {
            ItemStack paper = new ItemStack(Items.PAPER);
            paper.setHoverName(Component.literal("§aAliado: §f" + allyName));
            cont.setItem(idx++, paper);
        }
        // Show pending alliance invites to accept (from other guilds)
        if (player instanceof ServerPlayer sp0) {
            GuildsManager gm0 = GuildsManager.get(sp0.serverLevel());
            java.util.Set<String> invites = gm0.getAllianceInvitesFor(guild.getName());
            for (String from : invites) {
                if (idx >= 45) break;
                ItemStack accept = new ItemStack(Items.WRITTEN_BOOK);
                Guild fg = gm0.getGuild(from);
                accept.setHoverName(Component.literal("§aAceitar: §f" + (fg != null ? fg.getTag() : from)));
                cont.setItem(idx++, accept);
            }
        }
        // Show other guilds to invite
        if (player instanceof ServerPlayer sp) {
            GuildsManager m = GuildsManager.get(sp.serverLevel());
            for (Guild g : m.getAllGuilds()) {
                if (g.getName().equals(guild.getName())) continue;
                if (guild.getAllies().contains(g.getName())) continue;
                if (idx >= 45) break; // leave bottom row for controls
                ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
                book.setHoverName(Component.literal("§eConvidar: §f" + g.getTag()));
                cont.setItem(idx++, book);
            }
        }
        ItemStack back = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_BACK.get());
        back.setHoverName(Component.literal("§eVoltar"));
        cont.setItem(SLOT_BACK, back);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp) || clickType != ClickType.PICKUP) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        if (guild == null) guild = GuildsManager.get(sp.serverLevel()).getGuildByMember(sp.getUUID());
        if (slotId == SLOT_BACK) { sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv), Component.literal("Organização"))); return; }
        if (guild == null) return;
        GuildsManager m = GuildsManager.get(sp.serverLevel());
        // Map slot to action based on rebuild order: first allies, then invites
        int idx = 0;
        java.util.List<String> allies = new java.util.ArrayList<>(guild.getAllies());
        if (slotId < allies.size()) {
            String ally = allies.get(slotId);
            // Remove alliance
            if (m.removeAlliance(guild.getName(), ally)) sp.sendSystemMessage(Component.literal("§eAliança removida com " + ally + "."));
            rebuild(); broadcastChanges(); return;
        }
        idx = allies.size();
        java.util.List<Guild> others = new java.util.ArrayList<>();
        for (Guild g : m.getAllGuilds()) if (!g.getName().equals(guild.getName()) && !guild.getAllies().contains(g.getName())) others.add(g);
        int inviteIndex = slotId - idx;
        if (inviteIndex >= 0) {
            // Click falls into others list or accept-invites region depending on idx mapping above
            // Determine how many accepts existed
            int acceptCount = m.getAllianceInvitesFor(guild.getName()).size();
            int alliesCount = guild.getAllies().size();
            if (slotId >= alliesCount && slotId < alliesCount + acceptCount) {
                // Accept invite
                java.util.List<String> froms = new java.util.ArrayList<>(m.getAllianceInvitesFor(guild.getName()));
                int acIdx = slotId - alliesCount;
                if (acIdx >= 0 && acIdx < froms.size()) {
                    String fromGuild = froms.get(acIdx);
                    GuildMember self = guild.getMember(sp.getUUID());
                    if (self == null || self.getRank() != GuildMember.Rank.LEADER) { sp.sendSystemMessage(Component.literal("§cApenas o líder pode aceitar alianças.")); return; }
                    if (m.acceptAllianceInvite(guild.getName(), fromGuild)) sp.sendSystemMessage(Component.literal("§aAliança oficializada."));
                    else sp.sendSystemMessage(Component.literal("§cFalha ao aceitar aliança."));
                    rebuild(); broadcastChanges(); return;
                }
            } else {
                // Send invite to others
                int base = alliesCount + acceptCount;
                int idxInOthers = slotId - base;
                if (idxInOthers >= 0 && idxInOthers < others.size()) {
                    Guild target = others.get(idxInOthers);
                    GuildMember self = guild.getMember(sp.getUUID());
                    if (self == null || self.getRank() != GuildMember.Rank.LEADER) { sp.sendSystemMessage(Component.literal("§cApenas o líder pode enviar convites.")); return; }
                    m.sendAllianceInvite(guild.getName(), target.getName());
                    sp.sendSystemMessage(Component.literal("§aConvite enviado para " + target.getTag() + "."));
                    return;
                }
            }
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }
}


