package org.lupz.doomsdayessentials.killlog.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TakeKillLogItemsPacket {
	private final String targetUuid;
	private final int entryIndex;

	public TakeKillLogItemsPacket(String targetUuid, int entryIndex) {
		this.targetUuid = targetUuid;
		this.entryIndex = entryIndex;
	}

	public static void encode(TakeKillLogItemsPacket pkt, FriendlyByteBuf buf) {
		buf.writeUtf(pkt.targetUuid);
		buf.writeVarInt(pkt.entryIndex);
	}

	public static TakeKillLogItemsPacket decode(FriendlyByteBuf buf) {
		return new TakeKillLogItemsPacket(buf.readUtf(), buf.readVarInt());
	}

	public static void handle(TakeKillLogItemsPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;
			try {
				Path file = Path.of("config", "doomsdayessentials", "killlogs", pkt.targetUuid + ".jsonl");
				if (!Files.exists(file)) {
					player.displayClientMessage(net.minecraft.network.chat.Component.literal("Sem logs."), false);
					return;
				}
				List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
				if (pkt.entryIndex < 0 || pkt.entryIndex >= lines.size()) {
					player.displayClientMessage(net.minecraft.network.chat.Component.literal("Índice inválido."), false);
					return;
				}
				// Parse selected line items
				com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(lines.get(pkt.entryIndex)).getAsJsonObject();
				List<ItemStack> items = new ArrayList<>();
				if (obj.has("inventory") && obj.get("inventory").isJsonArray()) {
					var arr = obj.getAsJsonArray("inventory");
					for (var el : arr) {
						var it = el.getAsJsonObject();
						String id = it.get("id").getAsString();
						int count = it.get("count").getAsInt();
						net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(id));
						if (item != null) {
							ItemStack st = new ItemStack(item, Math.max(1, count));
							if (it.has("nbt")) {
								try {
									st.setTag(net.minecraft.nbt.TagParser.parseTag(it.get("nbt").getAsString()));
								} catch (Exception ignore) {}
							}
							items.add(st);
						}
					}
				}
				for (ItemStack st : items) {
					if (!player.getInventory().add(st)) {
						player.drop(st, false);
					}
				}
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("Itens coletados da killlog."), false);
			} catch (IOException e) {
				EssentialsMod.LOGGER.error("Erro lendo killlog para pegar itens", e);
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("Erro lendo killlog."), false);
			}
		});
		ctx.setPacketHandled(true);
	}

	public static void sendToServer(String uuid, int index) {
		org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.sendToServer(new TakeKillLogItemsPacket(uuid, index));
	}
} 