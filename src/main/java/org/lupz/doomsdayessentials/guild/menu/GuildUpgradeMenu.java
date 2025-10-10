package org.lupz.doomsdayessentials.guild.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildMember;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.item.ModItems;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

/** Upgrade GUI for guild storage. Shows current level, capacity and next cost in scrapmetal. */
public class GuildUpgradeMenu extends AbstractContainerMenu {
    private final Container cont = new SimpleContainer(54);
    private final Player player;
    private Guild guild;

    private static final int SLOT_BACK = 45;
    private static final int SLOT_UPGRADE = 22; // center button

    public GuildUpgradeMenu(int windowId, Inventory inv) {
        super(ProfessionMenuTypes.GUILD_UPGRADE_MENU.get(), windowId);
        this.player = inv.player;
        if (player instanceof ServerPlayer sp) {
            this.guild = GuildsManager.get(sp.serverLevel()).getGuildByMember(sp.getUUID());
        }
        rebuild();
        for (int row = 0; row < 6; ++row) for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(cont, col + row * 9, 8 + col * 18, 18 + row * 18) {
                @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
                @Override public boolean mayPickup(@NotNull Player p) { return false; }
            });
        // Player inventory layout
        int invY = 198; int invTopY = invY - 58;
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invTopY + row * 18));
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(inv, col, 8 + col * 18, invY));
    }

    private void rebuild() {
        for (int i = 0; i < 54; i++) cont.setItem(i, ItemStack.EMPTY);
        ItemStack title = new ItemStack(net.minecraft.world.item.Items.PAPER);
        title.setHoverName(Component.literal("§6§lAprimorar Cofre"));
        cont.setItem(4, title);

        if (!(player instanceof ServerPlayer sp) || guild == null) return;
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        int level = guild.getStorageLevel();
        int cap = gm.getStorageCapacityItems(guild.getName());
        int nextLevel = Math.min(level + 1, 10);
        // Cost curve: level N -> cost = 500 + (N-1)*500 scrapmetal, capped reasonable
        int cost = 500 + (level - 1) * 500;
        if (level >= 10) cost = 0;

        ItemStack info = new ItemStack(net.minecraft.world.item.Items.BOOK);
        info.setHoverName(Component.literal("§bNível Atual: §f" + level));
        addLore(info, java.util.List.of(
                Component.literal("§7Capacidade: §e" + cap + " itens"),
                Component.literal(level < 10 ? ("§7Próximo Nível: §e" + nextLevel) : "§7Máximo atingido")));
        cont.setItem(20, info);

        ItemStack upgrade = new ItemStack(net.minecraft.world.item.Items.ANVIL);
        upgrade.setHoverName(Component.literal(level < 10 ? "§6Aprimorar para Nível " + nextLevel : "§aNível Máximo"));
        java.util.List<Component> lore = new java.util.ArrayList<>();
        if (level < 10) {
            lore.add(Component.literal("§7Custo: §c" + cost + " sucata (recursos da organização)"));
            lore.add(Component.literal("§7Clique para comprar usando os recursos da guilda."));
        } else {
            lore.add(Component.literal("§7Você já atingiu o nível máximo."));
        }
        addLore(upgrade, lore);
        cont.setItem(SLOT_UPGRADE, upgrade);

        ItemStack back = new ItemStack(org.lupz.doomsdayessentials.item.ModItems.GUI_BACK.get());
        back.setHoverName(Component.literal("§eVoltar"));
        addLore(back, java.util.List.of(Component.literal("§7Voltar ao menu da organização")));
        cont.setItem(SLOT_BACK, back);

        // Fill
        ItemStack filler = new ItemStack(net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) if (cont.getItem(i).isEmpty()) cont.setItem(i, filler.copy());
    }

    private void addLore(ItemStack stack, java.util.List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (!(clickPlayer instanceof ServerPlayer sp) || guild == null) { super.clicked(slotId, dragType, clickType, clickPlayer); return; }
        GuildsManager gm = GuildsManager.get(sp.serverLevel());
        if (slotId == SLOT_BACK && clickType == ClickType.PICKUP) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GuildMainMenu(id, inv), Component.literal("Organização")));
            return;
        }
        if (slotId == SLOT_UPGRADE && clickType == ClickType.PICKUP) {
            GuildMember self = guild.getMember(sp.getUUID());
            if (self == null || (self.getRank() != GuildMember.Rank.LEADER && self.getRank() != GuildMember.Rank.OFFICER)) {
                sp.sendSystemMessage(Component.literal("§cApenas Líder/Oficial pode comprar upgrades."));
                return;
            }
            int level = guild.getStorageLevel();
            if (level >= 10) { sp.sendSystemMessage(Component.literal("§aNível máximo atingido.")); return; }
            int cost = 500 + (level - 1) * 500;
            // Use guild resource bank instead of player's inventory
            org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(sp.serverLevel());
            String scrapId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ModItems.SCRAPMETAL.get()).toString();
            int have = bank.get(guild.getName(), scrapId);
            if (have < cost) { sp.sendSystemMessage(Component.literal("§cA organização precisa de " + cost + " sucata no cofre.")); return; }
            boolean debited = bank.consume(guild.getName(), scrapId, cost);
            if (!debited) { sp.sendSystemMessage(Component.literal("§cFalha ao debitar recursos da organização.")); return; }
            boolean ok = gm.upgradeStorageLevel(guild.getName());
            if (ok) {
                sp.sendSystemMessage(Component.literal("§aCofre aprimorado para nível " + guild.getStorageLevel() + "."));
                rebuild(); broadcastChanges();
            } else {
                sp.sendSystemMessage(Component.literal("§cNão foi possível aprimorar."));
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override
    public boolean stillValid(@NotNull Player p) { return true; }

    @Override
    public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }
}


