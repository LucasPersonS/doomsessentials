package org.lupz.doomsdayessentials.rarity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RarityCommand {
    private RarityCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("rarity")
                .requires(src -> src.hasPermission(2)) // admins only
                .then(Commands.literal("set")
                    .then(Commands.argument("tier", StringArgumentType.string()).suggests(TIER_SUGGESTIONS)
                        .executes(RarityCommand::setRarity)))
                // alias: /rarity additem <tier>
                .then(Commands.literal("additem")
                    .then(Commands.argument("tier", StringArgumentType.string()).suggests(TIER_SUGGESTIONS)
                        .executes(RarityCommand::setRarity)))
                .then(Commands.literal("clear").executes(RarityCommand::removeHeldOverride))
                .then(Commands.literal("removeitem").executes(RarityCommand::removeHeldOverride))
                .then(Commands.literal("removebase").executes(RarityCommand::removeBaseOverride))
                .then(Commands.literal("get").executes(RarityCommand::getRarity))
        );
    }

    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemainingLowerCase();
        for (RarityManager.RarityTier t : RarityManager.RarityTier.values()) {
            if (t.id.startsWith(rem)) builder.suggest(t.id);
        }
        return builder.buildFuture();
    };

    private static int setRarity(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhum item."));
            return 0;
        }
        String tierStr = StringArgumentType.getString(ctx, "tier");
        RarityManager.RarityTier tier = RarityManager.RarityTier.fromString(tierStr);
        if (tier == null) {
            ctx.getSource().sendFailure(Component.literal("Raridade inválida. Use: common, uncommon, rare, epic, legendary."));
            return 0;
        }
        // Register override: prefer variant (e.g., TACZ GunId) when present
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem());
        if (key != null) {
            final String itemId = key.toString();
            final String gunId = held.getTag() != null ? held.getTag().getString("GunId") : "";
            if (!gunId.isEmpty()) {
                RarityServerRegistry.setVariantTier(itemId, gunId, tier);
                ctx.getSource().sendSuccess(() -> Component.literal("Raridade global (VARIANTE) definida: " + tier.id.toUpperCase(Locale.ROOT) + " para " + itemId + " | GunId=" + gunId), true);
            } else {
                RarityServerRegistry.setItemTier(itemId, tier);
                ctx.getSource().sendSuccess(() -> Component.literal("Raridade global (ITEM) definida: " + tier.id.toUpperCase(Locale.ROOT) + " para " + itemId), true);
            }
        }
        return 1;
    }

    private static int removeHeldOverride(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhum item."));
            return 0;
        }
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem());
        if (key != null) {
            final String itemId = key.toString();
            final String gunId = held.getTag() != null ? held.getTag().getString("GunId") : "";
            if (!gunId.isEmpty()) {
                RarityServerRegistry.removeVariantTier(itemId, gunId);
                ctx.getSource().sendSuccess(() -> Component.literal("Raridade global (VARIANTE) removida para: " + itemId + " | GunId=" + gunId), true);
            } else {
                RarityServerRegistry.removeItemTier(itemId);
                ctx.getSource().sendSuccess(() -> Component.literal("Raridade global (ITEM) removida para: " + itemId), true);
            }
        } else {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }
        return 1;
    }

    private static int removeBaseOverride(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhum item."));
            return 0;
        }
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem());
        if (key == null) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }
        final String itemId = key.toString();
        RarityServerRegistry.removeItemTier(itemId);
        ctx.getSource().sendSuccess(() -> Component.literal("Raridade global (ITEM) removida para: " + itemId), true);
        return 1;
    }

    private static int getRarity(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhum item."));
            return 0;
        }
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem());
        if (key == null) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }
        final String itemId = key.toString();
        final String gunId = held.getTag() != null ? held.getTag().getString("GunId") : "";
        if (!gunId.isEmpty()) {
            var v = RarityServerRegistry.getVariantTier(itemId, gunId);
            if (v != null) {
                ctx.getSource().sendSuccess(() -> Component.literal("Raridade (global VARIANTE): " + v.id + " para GunId=" + gunId), false);
                return 1;
            }
        }
        var ov = RarityServerRegistry.getItemTier(itemId);
        if (ov != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("Raridade (global ITEM): " + ov.id), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("Sem raridade global."), false);
        }
        return 1;
    }
}
