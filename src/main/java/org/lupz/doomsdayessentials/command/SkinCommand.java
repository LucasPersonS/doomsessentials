package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.command.VipCommand;

import java.util.*;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkinCommand {

	// Map TACZ base weapon -> available skin bases (series)
    private static final Map<String, List<String>> BASE_WEAPON_TO_SKIN_BASES = new HashMap<>();
	// Map any GunId path prefix -> base weapon (to resolve when already holding a skin)
	private static final Map<String, String> PREFIX_TO_BASE_WEAPON = new HashMap<>();
	// Variants per skin base
	private static final Map<String, List<String>> SKIN_BASE_TO_VARIANTS = new HashMap<>();
	// Namespace per skin base (to build full RL)
    private static final Map<String, String> SKIN_BASE_TO_NAMESPACE = new HashMap<>();
    private static final java.util.Set<String> DISSOLUTO_ONLY_SKIN_BASES = new java.util.HashSet<>();
    private static final java.util.Set<String> INFECTADO_ONLY_SKIN_BASES = new java.util.HashSet<>();

	static {
        // AK family supports skin base "kuronami" with variants (applies to any GunId path containing "ak")
        BASE_WEAPON_TO_SKIN_BASES.put("ak_family", List.of("kuronami", "ak_texas", "ak_kaltsit", "ak_laffey"));
        SKIN_BASE_TO_VARIANTS.put("kuronami", List.of("normal", "purple", "black", "red"));
        SKIN_BASE_TO_NAMESPACE.put("kuronami", "doomsday");
        SKIN_BASE_TO_VARIANTS.put("ak_texas", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("ak_kaltsit", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("ak_laffey", List.of("normal"));
        SKIN_BASE_TO_NAMESPACE.put("ak_texas", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("ak_kaltsit", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("ak_laffey", "doomsday");
        // Resolve base from either TACZ gun id or skin id prefixes
        PREFIX_TO_BASE_WEAPON.put("ak47", "ak_family");
        PREFIX_TO_BASE_WEAPON.put("kuronami", "ak_family");
        PREFIX_TO_BASE_WEAPON.put("ak24", "ak_family");
        PREFIX_TO_BASE_WEAPON.put("ak105", "ak_family");

		// deagle supports skin base "deagle_prometheus" (assuming only normal variant, can expand later)
		BASE_WEAPON_TO_SKIN_BASES.put("deagle", List.of("deagle_prometheus"));
		SKIN_BASE_TO_VARIANTS.put("deagle_prometheus", List.of("normal"));
		SKIN_BASE_TO_NAMESPACE.put("deagle_prometheus", "doomsday");
		PREFIX_TO_BASE_WEAPON.put("deagle", "deagle");
		PREFIX_TO_BASE_WEAPON.put("deagle_prometheus", "deagle");
		// Alias: some items may carry GunId path 'cfdz' for this deagle skin series
		PREFIX_TO_BASE_WEAPON.put("cfdz", "deagle");

        DISSOLUTO_ONLY_SKIN_BASES.add("kuronami");
        INFECTADO_ONLY_SKIN_BASES.add("ak_texas");
        INFECTADO_ONLY_SKIN_BASES.add("ak_kaltsit");
        INFECTADO_ONLY_SKIN_BASES.add("ak_laffey");

        BASE_WEAPON_TO_SKIN_BASES.put("m4_family", List.of("m4_koei", "mk18_jianjiu", "sig556_shiroko", "type20_hibiki", "galilace_lesh", "awp_hm"));
        SKIN_BASE_TO_VARIANTS.put("m4_koei", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("mk18_jianjiu", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("sig556_shiroko", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("type20_hibiki", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("galilace_lesh", List.of("normal"));
        SKIN_BASE_TO_VARIANTS.put("awp_hm", List.of("normal"));
        SKIN_BASE_TO_NAMESPACE.put("m4_koei", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("mk18_jianjiu", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("sig556_shiroko", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("type20_hibiki", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("galilace_lesh", "doomsday");
        SKIN_BASE_TO_NAMESPACE.put("awp_hm", "doomsday");
        PREFIX_TO_BASE_WEAPON.put("m4", "m4_family");
        PREFIX_TO_BASE_WEAPON.put("mk18", "m4_family");
        PREFIX_TO_BASE_WEAPON.put("sig556", "m4_family");
        PREFIX_TO_BASE_WEAPON.put("type20", "m4_family");
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent e) {
		register(e.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("skin")
					// /skin -> list available skins for the resolved base weapon
					.executes(SkinCommand::listSkins)
					.then(Commands.literal("list").executes(SkinCommand::listSkins))
					.then(Commands.literal("set")
						.then(Commands.argument("skinBase", StringArgumentType.string()).suggests(SKIN_BASE_SUGGESTIONS)
							.then(Commands.argument("variant", StringArgumentType.string()).suggests(VARIANT_SUGGESTIONS)
								.executes(SkinCommand::setSkin))
							// Allow defaulting to "normal" if variant omitted
							.executes(ctx -> setSkinWithDefault(ctx, "normal"))))
		);
	}

	private static final SuggestionProvider<CommandSourceStack> SKIN_BASE_SUGGESTIONS = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) return builder.buildFuture();
        String baseWeapon = resolveBaseWeaponFromHeld(player);
        for (String skinBase : BASE_WEAPON_TO_SKIN_BASES.getOrDefault(baseWeapon, List.of())) {
            if (skinBase.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) builder.suggest(skinBase);
        }
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) return builder.buildFuture();
		String skinBase;
		try { skinBase = StringArgumentType.getString(ctx, "skinBase"); } catch (Exception e) { skinBase = null; }
		List<String> variants = SKIN_BASE_TO_VARIANTS.getOrDefault(skinBase, List.of());
		for (String v : variants) {
			if (v.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) builder.suggest(v);
		}
		return builder.buildFuture();
	};

	private static int listSkins(CommandContext<CommandSourceStack> ctx) {
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
			ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
			return 0;
		}
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhuma arma."));
			return 0;
		}
        String baseWeapon = resolveBaseWeaponFromHeld(player);
        if (baseWeapon == null) {
            ctx.getSource().sendFailure(Component.literal("Arma não compatível ou sem GunId (TACZ)."));
            return 0;
        }

		List<String> skinBases = BASE_WEAPON_TO_SKIN_BASES.get(baseWeapon);
		if (skinBases == null || skinBases.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Nenhuma skin disponível para: " + baseWeapon));
			return 0;
		}

		MutableComponent header = Component.literal("Skins disponíveis para ")
				.append(Component.literal(baseWeapon).withStyle(Style.EMPTY.withBold(true)))
				.append(":");
		ctx.getSource().sendSuccess(() -> header, false);

		for (String skinBase : skinBases) {
			List<String> variants = SKIN_BASE_TO_VARIANTS.getOrDefault(skinBase, List.of());
			MutableComponent line = Component.literal(" - ")
					.append(Component.literal(skinBase).withStyle(Style.EMPTY.withBold(true)))
					.append(Component.literal(" "));
			// Show variants as clickable options
			for (int i = 0; i < variants.size(); i++) {
				String v = variants.get(i);
				line.append(Component.literal(v)
					.withStyle(Style.EMPTY.withUnderlined(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skin set " + skinBase + " " + v))));
				if (i < variants.size() - 1) line.append(Component.literal(", "));
			}
			ctx.getSource().sendSuccess(() -> line, false);
		}
		return 1;
	}

	private static int setSkinWithDefault(CommandContext<CommandSourceStack> ctx, String defaultVariant) {
		// If user omits variant, assume defaultVariant (e.g., normal)
		String skinBase = StringArgumentType.getString(ctx, "skinBase");
		return setSkinInternal(ctx, skinBase, defaultVariant);
	}

	private static int setSkin(CommandContext<CommandSourceStack> ctx) {
		String skinBase = StringArgumentType.getString(ctx, "skinBase");
		String variant = StringArgumentType.getString(ctx, "variant");
		return setSkinInternal(ctx, skinBase, variant);
	}

    private static int setSkinInternal(CommandContext<CommandSourceStack> ctx, String skinBase, String variant) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        java.util.List<? extends String> vips = EssentialsConfig.VIP_PLAYERS.get();
        String vipTier = VipCommand.getTier(player);
        boolean isVip = player.hasPermissions(2) || vipTier != null || vips.contains(player.getUUID().toString());
        if (!isVip) {
            ctx.getSource().sendFailure(Component.literal("Apenas VIP pode usar /skin set."));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Você não está segurando nenhuma arma."));
            return 0;
        }

		String baseWeapon = resolveBaseWeaponFromHeld(player);
		if (baseWeapon == null) {
			ctx.getSource().sendFailure(Component.literal("Arma não compatível ou sem GunId (TACZ)."));
			return 0;
		}
		// Validate skin base allowed for this weapon
		List<String> allowedSkinBases = BASE_WEAPON_TO_SKIN_BASES.getOrDefault(baseWeapon, List.of());
		Optional<String> matchedBaseOpt = allowedSkinBases.stream().filter(s -> s.equalsIgnoreCase(skinBase)).findFirst();
		if (matchedBaseOpt.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Skin base inválida para " + baseWeapon + ". Use /skin para listar."));
			return 0;
		}
        String matchedBase = matchedBaseOpt.get();

        if (DISSOLUTO_ONLY_SKIN_BASES.contains(matchedBase)) {
            String tier = vipTier;
            if (tier == null || !"dissoluto".equals(tier)) {
                ctx.getSource().sendFailure(Component.literal("A skin " + matchedBase + " é exclusiva para VIP Dissoluto."));
                return 0;
            }
        }
        if (INFECTADO_ONLY_SKIN_BASES.contains(matchedBase)) {
            String tier = vipTier;
            if (tier == null || !("infectado".equals(tier) || "dissoluto".equals(tier))) {
                ctx.getSource().sendFailure(Component.literal("A skin " + matchedBase + " é exclusiva para VIP Infectado/Dissoluto."));
                return 0;
            }
        }
		// Validate variant
		List<String> allowedVariants = SKIN_BASE_TO_VARIANTS.getOrDefault(matchedBase, List.of());
		Optional<String> matchedVariantOpt = allowedVariants.stream().filter(v -> v.equalsIgnoreCase(variant)).findFirst();
		if (matchedVariantOpt.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Variação inválida para " + matchedBase + ". Use /skin para listar."));
			return 0;
		}
		String matchedVariant = matchedVariantOpt.get();

        // Build target GunId to use an overlay index that swaps display only
        String ns = SKIN_BASE_TO_NAMESPACE.getOrDefault(matchedBase, EssentialsMod.MOD_ID);
        String basePath = getHeldGunPath(player);
        String path = buildOverlayPath(basePath, matchedBase, matchedVariant);
        String newGunId = ns + ":" + path;

        // Proceed as long as the held item carries a GunId; namespace may vary across packs

		// Replace held item by updating GunId in NBT, preserving attachments/ammo
		var tag = held.getOrCreateTag();
		String gunIdOld = tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING) ? tag.getString("GunId") : baseWeapon;
		tag.putString("GunId", newGunId);

		ItemStack replaced = new ItemStack(held.getItem(), held.getCount());
		replaced.setTag(tag.copy());
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, replaced);
		player.getInventory().setChanged();

		ctx.getSource().sendSuccess(() -> Component.literal("Skin alterada: ")
				.append(Component.literal(gunIdOld).withStyle(Style.EMPTY.withStrikethrough(true)))
				.append(Component.literal(" -> "))
				.append(Component.literal(newGunId).withStyle(Style.EMPTY.withBold(true))), true);
		return 1;
	}

    private static String buildOverlayPath(String basePath, String skinBase, String variant) {
        String v = variant.toLowerCase(Locale.ROOT);
        String cleaned = normalizeBasePath(basePath);
        String prefix = cleaned.toLowerCase(Locale.ROOT) + "_" + skinBase.toLowerCase(Locale.ROOT);
        if ("normal".equals(v)) return prefix;
        return prefix + "_" + v;
    }

    private static String normalizeBasePath(String path) {
        if (path == null || path.isEmpty()) return path;
        String lower = path.toLowerCase(Locale.ROOT);
        for (String skinBase : SKIN_BASE_TO_VARIANTS.keySet()) {
            String baseKey = "_" + skinBase.toLowerCase(Locale.ROOT);
            // Strip trailing "_skinBase"
            if (lower.endsWith(baseKey)) {
                int idx = lower.lastIndexOf(baseKey);
                if (idx >= 0) return path.substring(0, idx);
            }
            // Strip trailing "_skinBase_variant"
            for (String v : SKIN_BASE_TO_VARIANTS.getOrDefault(skinBase, List.of())) {
                String combo = baseKey + "_" + v.toLowerCase(Locale.ROOT);
                if (lower.endsWith(combo)) {
                    int idx = lower.lastIndexOf(combo);
                    if (idx >= 0) return path.substring(0, idx);
                }
            }
        }
        return path;
    }

    private static String getHeldGunPath(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return null;
        var tag = held.getTag();
        if (tag == null || !tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) return null;
        String gunId = tag.getString("GunId");
        String path = gunId.contains(":") ? gunId.split(":", 2)[1] : gunId;
        return path;
    }

    private static String resolveBaseWeaponFromHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return null;
        var tag = held.getTag();
        if (tag == null || !tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) return null;
        String gunId = tag.getString("GunId");
        if (gunId == null || gunId.isEmpty()) return null;
        String path = gunId.contains(":") ? gunId.split(":", 2)[1] : gunId;
        // If path contains "ak" anywhere, treat as AK family
        if (path.toLowerCase(Locale.ROOT).contains("ak")) return "ak_family";
        // Extract first prefix before underscore for other mappings
        String prefix = path;
        int us = path.indexOf('_');
        if (us > 0) prefix = path.substring(0, us);
        String base = PREFIX_TO_BASE_WEAPON.get(prefix);
        if (base != null) return base;
        return PREFIX_TO_BASE_WEAPON.get(path);
    }
} 
