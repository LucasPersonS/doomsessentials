package org.lupz.doomsdayessentials.territory.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.territory.ResourceAreaData;
import org.lupz.doomsdayessentials.territory.ResourceGeneratorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only preview of a single generator area – anybody can open via /territory generator info.
 */
public class GeneratorInfoMenu extends AbstractContainerMenu {
    private final Container container = new SimpleContainer(54);

    public GeneratorInfoMenu(int windowId, Inventory playerInv, String areaName) {
        super(org.lupz.doomsdayessentials.professions.menu.ProfessionMenuTypes.GENERATOR_INFO_MENU.get(), windowId);
        buildContents(areaName);
        // add 6*9 read-only slots
        for (int r=0;r<6;r++)
            for(int c=0;c<9;c++)
                addSlot(new ReadOnlySlot(container, c+r*9, 8+c*18, 18+r*18));

        // No player inventory slots – preview only
    }

    private void buildContents(String area) {
        ResourceAreaData data = ResourceGeneratorManager.get().get(area);
        if (data == null) return;
        int slot=0;
        // Header summary item -> slot 49 (row5,col4)
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(java.time.ZoneId.of("America/Sao_Paulo"));
        ItemStack header = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
        var tag = header.getOrCreateTag();
        tag.putString("SkullOwner", "sean1346"); // Green (#117743) head from minecraft-heads

        // Change header title to bold green "Informações"
        header.setHoverName(Component.literal("Informações").withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
        List<Component> hlore = new java.util.ArrayList<>();
        hlore.add(Component.literal("§bDono: §f" + (data.ownerGuild==null?"§7-":data.ownerGuild)));
        if (data.claimTimestamp>0){
            hlore.add(Component.literal("§dCapturado: §f"+fmt.format(java.time.Instant.ofEpochMilli(data.claimTimestamp))));
            long endMs=data.claimTimestamp+172_800_000L;
            hlore.add(Component.literal("§6Fim posse: §f"+fmt.format(java.time.Instant.ofEpochMilli(endMs))));
        }
        hlore.add(Component.literal("§eCapacidade: §f"+data.storageCap));
        addLore(header,hlore);
        container.setItem(49, header);

        // ---------------------------------------------------------
        // Fill remaining empty slots with black stained glass panes
        // ---------------------------------------------------------
        ItemStack filler = new ItemStack(net.minecraft.world.item.Items.BLACK_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < 54; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, filler.copy());
            }
        }
        for (ResourceAreaData.LootEntry e : data.lootEntries) {
            var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(e.id));
            if (item==null) continue;
            int remaining = e.stored;
            while (remaining>0 && slot<54) {
                ItemStack st = new ItemStack(item);
                int cnt = Math.min(64, remaining);
                st.setCount(cnt);
                List<Component> lore=new ArrayList<>();
                lore.add(Component.literal("§aProduzindo: §f"+e.perHour+"/h"));
                lore.add(Component.literal("§eArmazenado: §f"+e.stored+"/"+data.storageCap));
                addLore(st,lore);
                container.setItem(slot++, st);
                remaining -= cnt;
            }
        }
    }

    private void addLore(ItemStack s, List<Component> lore){
        var tag=new net.minecraft.nbt.ListTag();
        for (Component c:lore) tag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        s.getOrCreateTagElement("display").put("Lore",tag);
    }

    @Override public boolean stillValid(Player p){return true;}
    @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}

    private static class ReadOnlySlot extends Slot{
        public ReadOnlySlot(Container cont,int idx,int x,int y){super(cont,idx,x,y);} @Override public boolean mayPlace(ItemStack s){return false;} @Override public boolean mayPickup(Player p){return false;}
    }
} 