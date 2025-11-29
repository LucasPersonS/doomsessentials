package org.lupz.doomsdayessentials.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Fragmento de lootbox que pode ser trocado por lootboxes completas no menu
 * /fragmentos.
 * Jogadores ganham fragmentos através de farming (mobs, mining, quests).
 */
public class LootboxFragmentItem extends Item {
    private final String rarity;

    public LootboxFragmentItem(Properties props, String rarity) {
        super(props);
        this.rarity = rarity;
    }

    public String getRarity() {
        return rarity;
    }

    @Override
    public @Nonnull Component getName(@Nonnull ItemStack stack) {
        return switch (rarity.toLowerCase()) {
            case "incomum" -> Component.literal("§a§lFragmento de Lootbox Incomum");
            case "rara" -> Component.literal("§9§lFragmento de Lootbox Rara");
            case "epica" -> Component.literal("§5§lFragmento de Lootbox Épica");
            case "lendaria" -> Component.literal("§6§lFragmento de Lootbox Lendária");
            default -> Component.literal("§7Fragmento de Lootbox");
        };
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip,
            @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int needed = switch (rarity.toLowerCase()) {
            case "incomum" -> 100;
            case "rara" -> 150;
            case "epica" -> 200;
            case "lendaria" -> 300;
            default -> 100;
        };

        tooltip.add(Component.literal("§7Use §e/fragmentos §7para trocar"));
        tooltip.add(Component.literal("§e" + needed + " fragmentos §7por §f1 lootbox§7."));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8Obtido através de farming:"));
        tooltip.add(Component.literal("§8• Matar mobs"));
        tooltip.add(Component.literal("§8• Minerar blocos"));
        tooltip.add(Component.literal("§8• Completar quests diárias"));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        // Brilho para épica e lendária
        return rarity.equalsIgnoreCase("epica") || rarity.equalsIgnoreCase("lendaria");
    }
}
