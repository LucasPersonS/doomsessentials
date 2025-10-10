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
import org.lupz.doomsdayessentials.territory.ResourceGeneratorManager;

/**
 * Main guild UI: centralizes all guild actions under one GUI.
 * Layout: 6x9 with action buttons. Uses lore-only icons and click handlers.
 */
public class GuildMainMenu extends AbstractContainerMenu {
    private final Container container = new SimpleContainer(54);
    private final Player player;
    private Guild guild;

    // Centered, proportional layout (rows 2-4 around center columns 2,4,6)
    private static final int SLOT_REWARDS = 20;            // row2 col2
    private static final int SLOT_DEPOSIT = 22;            // row2 col4
    private static final int SLOT_TERRITORY_REWARDS = 24;  // row2 col6
    private static final int SLOT_OPEN_UPGRADES = 26;      // row2 col8
    private static final int SLOT_ALLIANCE = 29;           // row3 col2
    private static final int SLOT_MEMBERS = 31;            // row3 col4
    private static final int SLOT_INVADIR = 33;            // row3 col6
    private static final int SLOT_BASE = 38;               // row4 col2
    private static final int SLOT_SET_BASE = 40;           // row4 col4
    private static final int SLOT_TOTEM = 42;              // row4 col6
    private static final int SLOT_LEAVE = 44;              // row4 col8
    private static final int SLOT_MAP_TOGGLE = 36;         // row4 col0 (edge toggle)
    private static final int SLOT_ACCEPT_INVITE = 31;      // center when not in a guild

    public GuildMainMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.GUILD_MAIN_MENU.get(), windowId);
        this.player = inv.player;
        if (player instanceof ServerPlayer sp) {
            ServerLevel lvl = sp.serverLevel();
            this.guild = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
        }
        build();
        // Menu grid (top 6x9)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new ReadOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        // Player inventory slots aligned to vanilla chest layout (6 rows)
        int yBase = 103 + ((6 - 4) * 18); // 139
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, yBase + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 161 + ((6 - 4) * 18))); // 197
        }
    }

    private void build() {
        // Clear
        for (int i = 0; i < 54; i++) container.setItem(i, ItemStack.EMPTY);

        // Title / header center
        ItemStack title = new ItemStack(Items.PAPER);
        title.setHoverName(Component.literal("§6§lOrganizações"));
        addLore(title, new java.util.ArrayList<>(java.util.List.of(
                Component.literal(guild != null ? "§7Você está em: §e" + guild.getTag() : "§cVocê não pertence a nenhuma organização."))));
        container.setItem(4, title);

        if (guild == null) {
            // When player is not in a guild: show accept invite if any
            if (player instanceof ServerPlayer sp) {
                GuildsManager gm = GuildsManager.get(sp.serverLevel());
                String invGuild = gm.getInviteFor(sp.getUUID());
                if (invGuild != null) {
                    ItemStack accept = new ItemStack(Items.WRITABLE_BOOK);
                    Guild g = gm.getGuild(invGuild);
                    accept.setHoverName(Component.literal("§aAceitar Convite"));
                    addLore(accept, new java.util.ArrayList<>(java.util.List.of(
                            Component.literal("§7Convite para: §e" + (g != null ? g.getTag() : invGuild))
                    )));
                    container.setItem(SLOT_ACCEPT_INVITE, accept);
                }
            }
        } else {
            // Rewards / Storage
            ItemStack rewards = new ItemStack(Items.CHEST);
            rewards.setHoverName(Component.literal("§aCofre & Recompensas"));
            addLore(rewards, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Clique para abrir o cofre global da organização."))));
            container.setItem(SLOT_REWARDS, rewards);

            // Deposit Resources (Guild Bank)
            ItemStack deposit = new ItemStack(Items.IRON_INGOT);
            deposit.setHoverName(Component.literal("§bDepositar Recursos"));
            addLore(deposit, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Abra o depósito para armazenar recursos da guilda."))));
            container.setItem(SLOT_DEPOSIT, deposit);

            // Map toggle
            ItemStack mapa = new ItemStack(Items.MAP);
            mapa.setHoverName(Component.literal("§eAlternar Mapa do Território"));
            addLore(mapa, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Liga/Desliga a visualização do território."))));
            container.setItem(SLOT_MAP_TOGGLE, mapa);

            // Invadir
            ItemStack invadir = new ItemStack(Items.IRON_SWORD);
            invadir.setHoverName(Component.literal("§cIniciar Invasão"));
            addLore(invadir, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Perto da Base de Recursos inimiga, com requisitos online."))));
            container.setItem(SLOT_INVADIR, invadir);

            // Base teleport
            ItemStack base = new ItemStack(Items.RECOVERY_COMPASS);
            base.setHoverName(Component.literal("§aIr para a Base"));
            addLore(base, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Teleporta para a base da organização (cooldown)."))));
            container.setItem(SLOT_BASE, base);

            // Set base (leader only)
            ItemStack setBase = new ItemStack(Items.LODESTONE);
            setBase.setHoverName(Component.literal("§eDefinir Base"));
            addLore(setBase, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Define a base na posição atual (somente líder)."))));
            container.setItem(SLOT_SET_BASE, setBase);

            // Totem (leader only)
            ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
            totem.setHoverName(Component.literal("§6Pegar Totem"));
            addLore(totem, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Entrega o totem da organização ao líder."))));
            container.setItem(SLOT_TOTEM, totem);

            // Leave (non-leader)
            ItemStack leave = new ItemStack(Items.BARRIER);
            leave.setHoverName(Component.literal("§cSair da Organização"));
            addLore(leave, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Sair da sua organização (respeita cooldown)."))));
            container.setItem(SLOT_LEAVE, leave);

            // Alliance
            ItemStack alianca = new ItemStack(Items.WRITABLE_BOOK);
            alianca.setHoverName(Component.literal("§dGerenciar Alianças"));
            addLore(alianca, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Enviar e aceitar convites de aliança."))));
            container.setItem(SLOT_ALLIANCE, alianca);

            // Members (basic info shortcut)
            ItemStack members = new ItemStack(Items.PLAYER_HEAD);
            members.setHoverName(Component.literal("§bMembros e Informações"));
            addLore(members, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Listar membros, base, território, etc."))));
            container.setItem(SLOT_MEMBERS, members);

            // Territory rewards separate button
            ItemStack trew = new ItemStack(Items.GOLD_INGOT);
            trew.setHoverName(Component.literal("§6Recompensas de Território"));
            addLore(trew, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Coletar drops dos geradores de território."))));
            container.setItem(SLOT_TERRITORY_REWARDS, trew);

            // Open Upgrades menu
            ItemStack upg = new ItemStack(Items.ANVIL);
            upg.setHoverName(Component.literal("§6Abrir Upgrades"));
            addLore(upg, new java.util.ArrayList<>(java.util.List.of(
                    Component.literal("§7Abrir o menu de upgrades da organização."))));
            container.setItem(SLOT_OPEN_UPGRADES, upg);

        }

        // Fill background
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) {
            if (container.getItem(i).isEmpty()) container.setItem(i, filler.copy());
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
        ServerLevel level = sp.serverLevel();
        GuildsManager gm = GuildsManager.get(level);
        if (guild == null) guild = gm.getGuildByMember(sp.getUUID());

        if (slotId == SLOT_REWARDS && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildStorageMenu(id, inv, 0), Component.literal("Cofre da Organização")));
            return;
        }
        if (slotId == SLOT_DEPOSIT && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildResourceDepositMenu(id, inv),
                Component.literal("Depósito de Recursos"))
            );
            return;
        }
        if (slotId == SLOT_TERRITORY_REWARDS && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new org.lupz.doomsdayessentials.territory.menu.TerritoryRewardMenu(id, inv), Component.literal("Recompensas de Território")));
            return;
        }
        if (slotId == SLOT_OPEN_UPGRADES && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildUpgradeMenu(id, inv),
                Component.literal("Upgrades de Organização")));
            return;
        }
        if (slotId == SLOT_MAP_TOGGLE && clickType == ClickType.PICKUP) {
            boolean on = org.lupz.doomsdayessentials.guild.ClientGuildData.toggleMap(sp.getUUID());
            sp.sendSystemMessage(Component.literal(on ? "§aVisualização ativada." : "§eVisualização desativada."));
            return;
        }
        if (slotId == SLOT_INVADIR && clickType == ClickType.PICKUP) { handleInvadir(sp); return; }
        if (slotId == SLOT_BASE && clickType == ClickType.PICKUP) { handleBaseTeleport(sp); return; }
        if (slotId == SLOT_SET_BASE && clickType == ClickType.PICKUP) { handleSetBase(sp); return; }
        if (slotId == SLOT_TOTEM && clickType == ClickType.PICKUP) { handleTotem(sp); return; }
        if (slotId == SLOT_LEAVE && clickType == ClickType.PICKUP) { handleLeave(sp); return; }
        if (slotId == SLOT_ACCEPT_INVITE && clickType == ClickType.PICKUP) { handleAcceptInvite(sp); return; }
        if (slotId == SLOT_ALLIANCE && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildAlliancesMenu(id, inv), Component.literal("Alianças")));
            return;
        }
        if (slotId == SLOT_MEMBERS && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new org.lupz.doomsdayessentials.guild.menu.GuildMembersMenu(id, inv), Component.literal("Membros da Organização")));
            return;
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    private void handleAcceptInvite(ServerPlayer sp) {
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        if (gm.getGuildByMember(sp.getUUID()) != null) return;
        String gname = gm.getInviteFor(sp.getUUID());
        if (gname == null) { sp.sendSystemMessage(Component.literal("§eNenhum convite pendente.")); return; }
        if (!gm.acceptInvite(sp.getUUID())) { sp.sendSystemMessage(Component.literal("§cNão foi possível entrar na organização.")); return; }
        sp.sendSystemMessage(Component.literal("§aVocê entrou na organização!"));
    }

    private void handleLeave(ServerPlayer sp) {
        GuildsManager m = GuildsManager.get(sp.serverLevel());
        Guild g = m.getGuildByMember(sp.getUUID());
        if (g == null) { sp.sendSystemMessage(Component.literal("§cVocê não pertence a uma organização.")); return; }
        GuildMember member = g.getMember(sp.getUUID());
        if (member.getRank() == GuildMember.Rank.LEADER) { sp.sendSystemMessage(Component.literal("§cO líder não pode sair.")); return; }
        if (!member.canLeave()) {
            long ms = member.getTimeUntilCanLeave();
            long h = ms / 3600000L; long mLeft = (ms % 3600000L) / 60000L;
            sp.sendSystemMessage(Component.literal(String.format("§eVocê precisa esperar %d h %d min para sair.", h, mLeft)));
            return;
        }
        g.removeMember(sp.getUUID());
        m.setDirty();
        sp.sendSystemMessage(Component.literal("§aVocê saiu da organização."));
    }

    private void handleSetBase(ServerPlayer sp) {
        GuildsManager m = GuildsManager.get(sp.serverLevel());
        Guild g = m.getGuildByMember(sp.getUUID());
        if (g == null) { sp.sendSystemMessage(Component.literal("§cVocê não pertence a uma organização.")); return; }
        GuildMember self = g.getMember(sp.getUUID());
        if (self.getRank() != GuildMember.Rank.LEADER) { sp.sendSystemMessage(Component.literal("§cApenas o líder pode definir a base.")); return; }
        if (!g.isInTerritory(sp.blockPosition())) { sp.sendSystemMessage(Component.literal("§eA base deve estar dentro do território.")); return; }
        g.setBasePosition(sp.blockPosition()); m.setDirty();
        sp.sendSystemMessage(Component.literal("§aBase definida!"));
    }

    private void handleBaseTeleport(ServerPlayer sp) {
        GuildsManager m = GuildsManager.get(sp.serverLevel());
        Guild g = m.getGuildByMember(sp.getUUID());
        if (g == null || g.getBasePosition() == null) { sp.sendSystemMessage(Component.literal("§eA base não está definida.")); return; }
        if (org.lupz.doomsdayessentials.combat.CombatManager.get().isInCombat(sp.getUUID())) { sp.sendSystemMessage(Component.literal("§cVocê não pode usar em combate.")); return; }
        var tag = sp.getPersistentData();
        long last = tag.getLong("de_base_tp");
        long cdMs = org.lupz.doomsdayessentials.config.EssentialsConfig.GUILD_BASE_COOLDOWN_MINUTES.get() * 60L * 1000L;
        long now = System.currentTimeMillis();
        if (now - last < cdMs) {
            long rem = cdMs - (now - last); long min = rem / 60000; long sec = (rem % 60000) / 1000;
            sp.sendSystemMessage(Component.literal("§eAguarde " + min + "m" + sec + "s para usar novamente."));
            return;
        }
        var bp = g.getBasePosition();
        sp.teleportTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
        tag.putLong("de_base_tp", now);
        sp.sendSystemMessage(Component.literal("§aTeleportado para a base."));
    }

    private void handleTotem(ServerPlayer sp) {
        GuildsManager m = GuildsManager.get(sp.serverLevel());
        Guild g = m.getGuildByMember(sp.getUUID());
        if (g == null) { sp.sendSystemMessage(Component.literal("§cVocê não pertence a uma organização.")); return; }
        if (g.getMember(sp.getUUID()).getRank() != GuildMember.Rank.LEADER) { sp.sendSystemMessage(Component.literal("§cApenas o líder pode pegar o totem.")); return; }
        if (g.getTotemPosition() != null) { sp.sendSystemMessage(Component.literal("§eO totem já está posicionado.")); return; }
        sp.getInventory().add(new ItemStack(org.lupz.doomsdayessentials.block.ModBlocks.TOTEM_BLOCK.get()));
        sp.sendSystemMessage(Component.literal("§aTotem entregue."));
    }

    private void handleInvadir(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        GuildsManager m = GuildsManager.get(level);
        Guild attacker = m.getGuildByMember(sp.getUUID());
        if (attacker == null) { sp.sendSystemMessage(Component.literal("§cVocê precisa estar em uma organização.")); return; }
        org.lupz.doomsdayessentials.combat.AreaManager am = org.lupz.doomsdayessentials.combat.AreaManager.get();
        org.lupz.doomsdayessentials.combat.ManagedArea area = am.getAreaAt(level, sp.blockPosition());
        if (area == null || !area.getType().isResource()) { sp.sendSystemMessage(Component.literal("§eVocê precisa estar perto da Base de Recursos inimiga para iniciar a invasão.")); return; }
        Guild defender = m.getGuildAt(sp.blockPosition());
        if (defender == null) { sp.sendSystemMessage(Component.literal("§eVocê não está no território de nenhuma organização.")); return; }
        if (attacker.getName().equals(defender.getName())) { sp.sendSystemMessage(Component.literal("§eVocê não pode invadir sua própria organização.")); return; }
        if (m.isGuildProtected(defender.getName())) { sp.sendSystemMessage(Component.literal("§eEsta organização está sob proteção e não pode ser atacada agora.")); return; }
        if (m.isPairBanned(attacker.getName(), defender.getName())) { sp.sendSystemMessage(Component.literal("§eVocê não pode reinvadir esta organização ainda.")); return; }
        if (!m.canStartWarInTerritory(defender.getName())) { sp.sendSystemMessage(Component.literal("§eEste território está sob cooldown de guerra.")); return; }
        GuildMember self = attacker.getMember(sp.getUUID());
        if (self == null || (self.getRank() != GuildMember.Rank.LEADER && self.getRank() != GuildMember.Rank.OFFICER)) { sp.sendSystemMessage(Component.literal("§eApenas o Líder ou Oficial pode iniciar uma invasão.")); return; }
        int minAtk = org.lupz.doomsdayessentials.guild.GuildConfig.WAR_MIN_ONLINE_ATTACKERS.get();
        int minDef = org.lupz.doomsdayessentials.guild.GuildConfig.WAR_MIN_ONLINE_DEFENDERS.get();
        int atkOnline = m.getOnlineCount(level, attacker.getName());
        int defOnline = m.getOnlineCount(level, defender.getName());
        if (org.lupz.doomsdayessentials.guild.GuildConfig.INCLUDE_ALLIES_IN_WAR.get()) {
            for (String ally : attacker.getAllies()) atkOnline += m.getOnlineCount(level, ally);
            for (String ally : defender.getAllies()) defOnline += m.getOnlineCount(level, ally);
        }
        if (atkOnline < minAtk || defOnline < minDef) { sp.sendSystemMessage(Component.literal("§eSão necessários pelo menos " + minAtk + " atacantes e " + minDef + " defensores online.")); return; }
        org.lupz.doomsdayessentials.guild.War war = m.startWar(attacker.getName(), defender.getName());
        if (war == null) { sp.sendSystemMessage(Component.literal("§eJá há uma guerra entre essas organizações!")); return; }

        var server = sp.getServer();
        if (server != null) {
            Component broadcastMsg = Component.literal("A organização " + attacker.getTag() + " iniciou uma invasão contra " + defender.getTag() + "!");
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(attacker.getTag() + " vs " + defender.getTag())));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("INVASÃO!")));
                p.playNotifySound(net.minecraft.sounds.SoundEvents.RAID_HORN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
                p.sendSystemMessage(broadcastMsg);
            }
        }
        sp.sendSystemMessage(Component.literal("§cGuerra iniciada!"));
    }

    @Override
    public boolean stillValid(@NotNull Player p) { return true; }
    @Override
    public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container cont, int idx, int x, int y) { super(cont, idx, x, y); }
        @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
        @Override public boolean mayPickup(@NotNull Player p) { return false; }
    }
}


