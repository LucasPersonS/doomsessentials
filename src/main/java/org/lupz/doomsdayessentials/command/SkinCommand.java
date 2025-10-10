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

	static {
		// ak47 supports skin base "kuronami" with variants
		BASE_WEAPON_TO_SKIN_BASES.put("ak47", List.of("kuronami"));
		SKIN_BASE_TO_VARIANTS.put("kuronami", List.of("normal", "purple", "black", "red"));
		SKIN_BASE_TO_NAMESPACE.put("kuronami", "doomsday");
		// Resolve base from either TACZ gun id or skin id prefixes
		PREFIX_TO_BASE_WEAPON.put("ak47", "ak47");
		PREFIX_TO_BASE_WEAPON.put("kuronami", "ak47");

		// deagle supports skin base "deagle_prometheus" (assuming only normal variant, can expand later)
		BASE_WEAPON_TO_SKIN_BASES.put("deagle", List.of("deagle_prometheus"));
		SKIN_BASE_TO_VARIANTS.put("deagle_prometheus", List.of("normal"));
		SKIN_BASE_TO_NAMESPACE.put("deagle_prometheus", "doomsday");
		PREFIX_TO_BASE_WEAPON.put("deagle", "deagle");
		PREFIX_TO_BASE_WEAPON.put("deagle_prometheus", "deagle");
		// Alias: some items may carry GunId path 'cfdz' for this deagle skin series
		PREFIX_TO_BASE_WEAPON.put("cfdz", "deagle");
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
		// Validate variant
		List<String> allowedVariants = SKIN_BASE_TO_VARIANTS.getOrDefault(matchedBase, List.of());
		Optional<String> matchedVariantOpt = allowedVariants.stream().filter(v -> v.equalsIgnoreCase(variant)).findFirst();
		if (matchedVariantOpt.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Variação inválida para " + matchedBase + ". Use /skin para listar."));
			return 0;
		}
		String matchedVariant = matchedVariantOpt.get();

		// Build target GunId ResourceLocation string
		String ns = SKIN_BASE_TO_NAMESPACE.getOrDefault(matchedBase, EssentialsMod.MOD_ID);
		String path = buildSkinPath(matchedBase, matchedVariant);
		String newGunId = ns + ":" + path;

		// Validate TACZ item namespace to avoid accidental application to non-TACZ items
		ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(held.getItem());
		if (itemKey == null || itemKey.getNamespace() == null || !"tacz".equals(itemKey.getNamespace())) {
			ctx.getSource().sendFailure(Component.literal("O item na mão não é uma arma TACZ válida."));
			return 0;
		}

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

	private static String buildSkinPath(String skinBase, String variant) {
		String v = variant.toLowerCase(Locale.ROOT);
		if ("normal".equals(v)) return skinBase;
		return skinBase + "_" + v;
	}

	private static String resolveBaseWeaponFromHeld(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) return null;
		var tag = held.getTag();
		if (tag == null || !tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING)) return null;
		String gunId = tag.getString("GunId");
		if (gunId == null || gunId.isEmpty()) return null;
		String path = gunId.contains(":") ? gunId.split(":", 2)[1] : gunId;
		// Extract first prefix before underscore to match mapping (e.g., kuronami_black -> kuronami)
		String prefix = path;
		int us = path.indexOf('_');
		if (us > 0) prefix = path.substring(0, us);
		// Try direct mapping first, else fallback to full path
		String base = PREFIX_TO_BASE_WEAPON.get(prefix);
		if (base != null) return base;
		return PREFIX_TO_BASE_WEAPON.get(path);
	}
} 
