package org.lupz.doomsdayessentials.killlog;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KillLogCommand {

	private static final Path LOG_DIR = Path.of("config", "doomsdayessentials", "killlogs");
	private static final DateTimeFormatter UI_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm'h'");

	private KillLogCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		var root = Commands.literal("killlog").requires(src -> src.hasPermission(2));

		root.then(
			Commands.argument("player", StringArgumentType.word())
				.suggests(KillLogCommand::suggestOnlinePlayers)
				.executes(ctx -> openInventoryUI(ctx.getSource(), StringArgumentType.getString(ctx, "player")))
		);

		root.then(
			Commands.literal("clear")
				.then(Commands.argument("player", StringArgumentType.word())
					.suggests(KillLogCommand::suggestOnlinePlayers)
					.executes(ctx -> clearByNameOrUuid(ctx.getSource(), StringArgumentType.getString(ctx, "player"))))
		);

		dispatcher.register(root);
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
		PlayerList pl = c.getSource().getServer().getPlayerList();
		for (ServerPlayer p : pl.getPlayers()) {
			b.suggest(p.getGameProfile().getName());
		}
		return b.buildFuture();
	}

	private static int openInventoryUI(CommandSourceStack src, String jogador) {
		ServerPlayer viewer;
		try {
			viewer = src.getPlayerOrException();
		} catch (Exception e) {
			src.sendFailure(Component.literal("Apenas jogadores podem abrir esta UI.").withStyle(ChatFormatting.RED));
			return 0;
		}
		String uuidStr = resolveUuidFromInput(src, jogador);
		if (uuidStr == null) {
			src.sendFailure(Component.literal("Jogador/UUID não encontrado: " + jogador).withStyle(ChatFormatting.RED));
			return 0;
		}
		Path file = LOG_DIR.resolve(uuidStr + ".jsonl");
		if (!Files.exists(file)) {
			src.sendFailure(Component.literal("Sem logs para este jogador.").withStyle(ChatFormatting.RED));
			return 0;
		}
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			List<org.lupz.doomsdayessentials.killlog.menu.KillLogMenu.Entry> entries = new ArrayList<>();
			com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
			// Reverse order so most recent deaths appear first
			for (int i = lines.size() - 1; i >= 0; i--) {
				String line = lines.get(i);
				if (line.isBlank()) continue;
				var obj = parser.parse(line).getAsJsonObject();
				String tsIso = obj.get("timestamp").getAsString();
				String tsUi;
				try { tsUi = ZonedDateTime.parse(tsIso).format(UI_FMT); } catch (Exception ex) { tsUi = tsIso; }
				double x = obj.has("x") ? obj.get("x").getAsDouble() : 0.0;
				double y = obj.has("y") ? obj.get("y").getAsDouble() : 0.0;
				double z = obj.has("z") ? obj.get("z").getAsDouble() : 0.0;
				String causeId = obj.has("causeId") ? obj.get("causeId").getAsString() : "";
				String killerName = obj.has("killerName") ? obj.get("killerName").getAsString() : "";
				String weaponDisplay = (obj.has("weapon") && obj.get("weapon").isJsonObject() && obj.getAsJsonObject("weapon").has("display")) ? obj.getAsJsonObject("weapon").get("display").getAsString() : "Null";
				String areaName = obj.has("areaName") ? obj.get("areaName").getAsString() : "none";
				String areaType = obj.has("areaType") ? obj.get("areaType").getAsString() : "none";
				// last hits summary
				List<String> lastHits = new ArrayList<>();
				if (obj.has("last_hits") && obj.get("last_hits").isJsonArray()) {
					var arr = obj.getAsJsonArray("last_hits");
					for (int j = Math.max(0, arr.size()-5); j < arr.size(); j++) {
						var h = arr.get(j).getAsJsonObject();
						String nm = h.has("name") ? h.get("name").getAsString() : "";
						String tp = h.has("type") ? h.get("type").getAsString() : "";
						String sc = h.has("source") ? h.get("source").getAsString() : "";
						lastHits.add((nm.isEmpty()?tp:nm) + " (" + sc + ")");
					}
				}

				List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
				if (obj.has("inventory") && obj.get("inventory").isJsonArray()) {
					var arr = obj.getAsJsonArray("inventory");
					for (var el : arr) {
						var it = el.getAsJsonObject();
						String id = it.get("id").getAsString();
						int count = it.get("count").getAsInt();
						net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(id));
						if (item != null) {
							net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(item, Math.max(1, count));
							if (it.has("nbt")) {
								try { st.setTag(TagParser.parseTag(it.get("nbt").getAsString())); } catch (Exception ignore) {}
							}
							items.add(st);
						}
					}
				}
				entries.add(new org.lupz.doomsdayessentials.killlog.menu.KillLogMenu.Entry(tsUi, items, x, y, z, causeId, killerName, weaponDisplay, areaName, areaType, lastHits));
			}

			net.minecraft.network.FriendlyByteBuf send = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
			send.writeUtf(uuidStr);
			send.writeVarInt(entries.size());
			for (var e : entries) {
				send.writeUtf(e.timestampIso);
				send.writeDouble(e.x);
				send.writeDouble(e.y);
				send.writeDouble(e.z);
				send.writeUtf(e.causeId);
				send.writeUtf(e.killerName != null ? e.killerName : "");
				send.writeUtf(e.weaponDisplay != null ? e.weaponDisplay : "Null");
				send.writeUtf(e.areaName != null ? e.areaName : "none");
				send.writeUtf(e.areaType != null ? e.areaType : "none");
				send.writeVarInt(e.items.size());
				for (var st : e.items) send.writeItem(st);
				send.writeVarInt(e.lastHits.size());
				for (String s : e.lastHits) send.writeUtf(s);
			}

			NetworkHooks.openScreen(viewer, new net.minecraft.world.MenuProvider() {
				@Override public net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.literal("Killlog: " + jogador); }
				@Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player player) {
					return new org.lupz.doomsdayessentials.killlog.menu.KillLogMenu(id, inv);
				}
			}, out -> out.writeBytes(send));

			src.sendSuccess(() -> Component.literal("Abrindo killlog de inventário para '" + jogador + "'"), false);
			return 1;
		} catch (IOException e) {
			src.sendFailure(Component.literal("Erro lendo o log: " + e.getMessage()).withStyle(ChatFormatting.RED));
			return 0;
		}
	}

	private static String resolveUuidFromInput(CommandSourceStack src, String input) {
		try { return UUID.fromString(input).toString(); } catch (IllegalArgumentException ignored) {}
		ServerPlayer online = src.getServer().getPlayerList().getPlayerByName(input);
		if (online != null) return online.getUUID().toString();
		return src.getServer().getProfileCache().get(input).map(com.mojang.authlib.GameProfile::getId).map(UUID::toString).orElse(null);
	}

	private static int clearByNameOrUuid(CommandSourceStack src, String jogador) {
		String uuidStr = resolveUuidFromInput(src, jogador);
		if (uuidStr == null) {
			src.sendFailure(Component.literal("Jogador/UUID não encontrado: " + jogador).withStyle(ChatFormatting.RED));
			return 0;
		}
		Path file = LOG_DIR.resolve(uuidStr + ".jsonl");
		try {
			Files.deleteIfExists(file);
			src.sendSuccess(() -> Component.literal("Log limpo."), true);
			return 1;
		} catch (IOException e) {
			src.sendFailure(Component.literal("Erro limpando o log: " + e.getMessage()).withStyle(ChatFormatting.RED));
			return 0;
		}
	}
} 