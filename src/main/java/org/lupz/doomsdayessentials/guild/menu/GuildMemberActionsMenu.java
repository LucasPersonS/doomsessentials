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
 * Per-member management actions: promote, demote, kick, with a back button.
 */
public class GuildMemberActionsMenu extends AbstractContainerMenu {
    private static final int SLOT_PROMOTE = 21;
    private static final int SLOT_DEMOTE = 23;
    private static final int SLOT_TRANSFER = 25;
    private static final int SLOT_KICK = 31;
    private static final int SLOT_BACK = 49;

    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private Guild guild;
    private final java.util.UUID targetUUID;

    public GuildMemberActionsMenu(int windowId, Inventory inv, java.util.UUID targetUUID) {
        super(ProfessionMenuTypes.GUILD_MEMBER_ACTIONS_MENU.get(), windowId);
        this.player = inv.player;
        this.targetUUID = targetUUID;
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
        String targetName = "?";
        if (player instanceof ServerPlayer sp) {
            var s = sp.getServer();
            var online = s.getPlayerList().getPlayer(targetUUID);
            if (online != null) targetName = online.getGameProfile().getName(); else targetName = s.getProfileCache().get(targetUUID).map(com.mojang.authlib.GameProfile::getName).orElse("?");
        }
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.setHoverName(Component.literal("§eGerenciar: §f" + targetName));
        cont.setItem(4, head);

        ItemStack promote = new ItemStack(Items.EMERALD);
        promote.setHoverName(Component.literal("§aPromover"));
        cont.setItem(SLOT_PROMOTE, promote);

        ItemStack demote = new ItemStack(Items.REDSTONE);
        demote.setHoverName(Component.literal("§eDespromover"));
        cont.setItem(SLOT_DEMOTE, demote);

        ItemStack kick = new ItemStack(Items.BARRIER);
        kick.setHoverName(Component.literal("§cExpulsar"));
        cont.setItem(SLOT_KICK, kick);

        ItemStack back = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_BACK.get());
        back.setHoverName(Component.literal("§eVoltar"));
        cont.setItem(SLOT_BACK, back);

        // Leader-only: transfer leadership to OFFICER target
        if (player instanceof ServerPlayer sp) {
            GuildsManager gm = GuildsManager.get(sp.serverLevel());
            Guild g = gm.getGuildByMember(sp.getUUID());
            if (g != null) {
                GuildMember self = g.getMember(sp.getUUID());
                GuildMember target = g.getMember(targetUUID);
                if (self != null && target != null && self.getRank() == GuildMember.Rank.LEADER && target.getRank() == GuildMember.Rank.OFFICER) {
                    ItemStack transfer = new ItemStack(Items.NETHER_STAR);
                    transfer.setHoverName(Component.literal("§6Transferir Liderança"));
                    cont.setItem(SLOT_TRANSFER, transfer);
                }
            }
        }
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp)) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        if (guild == null) guild = GuildsManager.get(sp.serverLevel()).getGuildByMember(sp.getUUID());
        if (slotId == SLOT_BACK && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMembersMenu(id, inv), Component.literal("Membros da Organização")));
            return;
        }
        if (guild == null) return;
        GuildsManager m = GuildsManager.get(sp.serverLevel());
        GuildMember self = guild.getMember(sp.getUUID());
        GuildMember target = guild.getMember(targetUUID);
        if (self == null || target == null) return;
        if (self.getRank() != GuildMember.Rank.LEADER) {
            sp.sendSystemMessage(Component.literal("§cApenas o líder pode gerenciar membros."));
            return;
        }
        if (slotId == SLOT_PROMOTE && clickType == ClickType.PICKUP) {
            if (target.getRank() == GuildMember.Rank.MEMBER) {
                target.setRank(GuildMember.Rank.OFFICER);
                m.setDirty();
                sp.sendSystemMessage(Component.literal("§aMembro promovido a Oficial."));
            } else {
                sp.sendSystemMessage(Component.literal("§eEste jogador já é Oficial/Líder."));
            }
            return;
        }
        if (slotId == SLOT_DEMOTE && clickType == ClickType.PICKUP) {
            if (target.getRank() == GuildMember.Rank.OFFICER) {
                target.setRank(GuildMember.Rank.MEMBER);
                m.setDirty();
                sp.sendSystemMessage(Component.literal("§eJogador despromovido."));
            } else {
                sp.sendSystemMessage(Component.literal("§eEste jogador não é Oficial."));
            }
            return;
        }
        if (slotId == SLOT_TRANSFER && clickType == ClickType.PICKUP) {
            if (self.getRank() != GuildMember.Rank.LEADER) {
                sp.sendSystemMessage(Component.literal("§cApenas o líder pode transferir a liderança."));
                return;
            }
            if (target.getRank() != GuildMember.Rank.OFFICER) {
                sp.sendSystemMessage(Component.literal("§eA liderança só pode ser transferida para um Co-Líder."));
                return;
            }
            if (targetUUID.equals(sp.getUUID())) {
                sp.sendSystemMessage(Component.literal("§eVocê já é o líder."));
                return;
            }
            // Demote current leader, promote target
            self.setRank(GuildMember.Rank.OFFICER);
            target.setRank(GuildMember.Rank.LEADER);
            m.setDirty();
            sp.sendSystemMessage(Component.literal("§aLiderança transferida."));
            var online = sp.getServer().getPlayerList().getPlayer(targetUUID);
            if (online != null) online.sendSystemMessage(Component.literal("§aVocê agora é o líder da organização."));
            // Back to members list
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMembersMenu(id, inv), Component.literal("Membros da Organização")));
            return;
        }
        if (slotId == SLOT_KICK && clickType == ClickType.PICKUP) {
            if (targetUUID.equals(sp.getUUID())) {
                sp.sendSystemMessage(Component.literal("§cVocê não pode se expulsar."));
                return;
            }
            if (!m.removeMember(guild.getName(), targetUUID)) {
                sp.sendSystemMessage(Component.literal("§cEsse jogador não é membro da sua organização."));
                return;
            }
            m.setDirty();
            sp.sendSystemMessage(Component.literal("§aJogador expulso."));
            var online = sp.getServer().getPlayerList().getPlayer(targetUUID);
            if (online != null) online.sendSystemMessage(Component.literal("§cVocê foi expulso da organização " + guild.getTag()));
            // Back to members list
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMembersMenu(id, inv), Component.literal("Membros da Organização")));
            return;
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }
}


