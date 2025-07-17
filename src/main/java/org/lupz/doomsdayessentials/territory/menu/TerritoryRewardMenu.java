package org.lupz.doomsdayessentials.territory.menu;

import net.minecraft.network.chat.Component;
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
import org.lupz.doomsdayessentials.territory.ResourceAreaData;
import org.lupz.doomsdayessentials.territory.ResourceGeneratorManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only menu showing os geradores de território da guilda e um botão para coletar recompensas.
 */
public class TerritoryRewardMenu extends AbstractContainerMenu {
    private final Container container = new SimpleContainer(54);
    private final Player player;
    private String guildName;

    public TerritoryRewardMenu(int windowId, Inventory playerInv) {
        super(ProfessionMenuTypes.TERRITORY_REWARD_MENU.get(), windowId);
        this.player = playerInv.player;
        ServerPlayer sp = playerInv.player instanceof ServerPlayer ? (ServerPlayer) playerInv.player : null;
        Guild g = null;
        if (sp != null) {
            ServerLevel lvl = sp.serverLevel();
            g = GuildsManager.get(lvl).getGuildByMember(sp.getUUID());
        }
        this.guildName = g != null ? g.getName() : null;
        buildContents();

        // 6x9 grid slots (double chest)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new ReadOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void buildContents() {
        int slot = 0;
        if (guildName != null) {
            List<ResourceAreaData> generators = ResourceGeneratorManager.get().getGeneratorsForGuild(guildName);
            for (ResourceAreaData d : generators) {
                for (ResourceAreaData.LootEntry entry : d.lootEntries) {
                    if (slot >= 54) break;
                    var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(entry.id));
                    if (item == null) continue;
                    ItemStack stack = new ItemStack(item);
                    stack.setCount(Math.min(64, entry.stored));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.literal("§7Área: §f" + d.areaName));
                    lore.add(Component.literal("§aProduzindo: §f" + entry.perHour + "/h"));
                    lore.add(Component.literal("§eArmazenado: §f" + entry.stored + "/" + d.storageCap));
                    addLore(stack, lore);
                    container.setItem(slot++, stack);
                }
            }
        }
        // Place collect button bottom-center (slot 49)
        int collectSlot = 49;
        ItemStack collect = new ItemStack(Items.PLAYER_HEAD);
        collect.setHoverName(Component.literal("§a§lCOLETAR"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Clique para coletar todos os recursos armazenados"));
        addLore(collect, lore);
        container.setItem(collectSlot, collect);

        // Fill remaining slots with gray panes as background
        ItemStack filler = new ItemStack(net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) {
            if (i == collectSlot) continue;
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, filler.copy());
            }
        }
    }

    private void addLore(ItemStack stack, List<Component> lore) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (Component c : lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", tag);
    }

    // ------------------------------------------------------------------
    // Interaction overrides
    // ------------------------------------------------------------------
    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player clickPlayer) {
        if (slotId == 49 && clickType == ClickType.PICKUP) {
            if (guildName != null && clickPlayer instanceof ServerPlayer serverPlayer) {
                int total = ResourceGeneratorManager.get().collectForGuild(serverPlayer, guildName);
                if (total > 0) {
                    clickPlayer.sendSystemMessage(Component.literal("§aRecebido " + total + " itens dos geradores."));
                } else {
                    clickPlayer.sendSystemMessage(Component.literal("§eNenhum item para coletar."));
                }
                buildContents();
                broadcastChanges();
            }
            return;
        }
        // prevent taking other items
        super.clicked(slotId, dragType, clickType, clickPlayer);
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