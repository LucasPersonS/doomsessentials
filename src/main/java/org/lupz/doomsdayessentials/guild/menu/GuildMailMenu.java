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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.lupz.doomsdayessentials.guild.Guild;
import org.lupz.doomsdayessentials.guild.GuildsManager;
import org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes;

import java.util.ArrayList;
import java.util.List;
public class GuildMailMenu extends AbstractContainerMenu {
    private final Container container = new SimpleContainer(54);
    private final Player player;
    private String guildName;

    public GuildMailMenu(int windowId, Inventory playerInv) {
        super(ProfessionMenuTypes.GUILD_MAIL_MENU.get(), windowId);
        this.player = playerInv.player;
        ServerPlayer sp = playerInv.player instanceof ServerPlayer ? (ServerPlayer) playerInv.player : null;
        Guild g = null;
        if (sp != null) {
            ServerLevel lvl = sp.serverLevel();
            g = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
        }
        this.guildName = g != null ? g.getName() : null;
        buildContents();

        for (int row = 0; row < 6; ++row) for (int col = 0; col < 9; ++col) this.addSlot(new ReadOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
        int invTopY = 84 + (6 - 3) * 18; // 138
        int hotbarY = 142 + (6 - 3) * 18; // 196
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 9; ++col) this.addSlot(new ReadOnlySlot(playerInv, col + row * 9 + 9, 8 + col * 18, invTopY + row * 18));
        for (int col = 0; col < 9; ++col) this.addSlot(new ReadOnlySlot(playerInv, col, 8 + col * 18, hotbarY));
    }

    private void buildContents() {
        int slot = 0;
        if (guildName != null && player instanceof ServerPlayer sp) {
            GuildsManager gm = GuildsManager.get(sp.serverLevel());
            java.util.Map<String, Integer> mail = gm.getGuildMail(guildName);
            for (var e : mail.entrySet()) {
                if (slot >= 54) break;
                var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(e.getKey()));
                if (item == null) continue;
                ItemStack stack = new ItemStack(item);
                int shown = e.getValue();
                if (shown > 9999) shown = 9999;
                stack.setCount(shown);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("§7Em correio: §f" + e.getValue()));
                addLore(stack, lore);
                container.setItem(slot++, stack);
            }
        }
        int collectSlot = 49;
        ItemStack collect = new ItemStack(Items.PLAYER_HEAD);
        collect.setHoverName(Component.literal("§a§lCOLETAR"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Clique para coletar os itens do correio"));
        addLore(collect, lore);
        container.setItem(collectSlot, collect);

        ItemStack filler = new ItemStack(net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) {
            if (i == collectSlot) continue;
            if (container.getItem(i).isEmpty()) container.setItem(i, filler.copy());
        }
    }

    private void addLore(ItemStack stack, List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (slotId == 49 && clickType == ClickType.PICKUP) {
            if (guildName != null && clickPlayer instanceof ServerPlayer serverPlayer) {
                int total = GuildsManager.get(serverPlayer.serverLevel()).collectGuildMail(serverPlayer, guildName);
                if (total > 0) {
                    clickPlayer.sendSystemMessage(Component.literal("§aRecebido " + total + " itens do correio."));
                } else {
                    clickPlayer.sendSystemMessage(Component.literal("§eNenhum item no correio ou cooldown ativo."));
                }
                buildContents();
                broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, clickPlayer);
    }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container cont, int idx, int x, int y) { super(cont, idx, x, y); }
        @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
        @Override public boolean mayPickup(@NotNull Player p) { return false; }
    }
}
