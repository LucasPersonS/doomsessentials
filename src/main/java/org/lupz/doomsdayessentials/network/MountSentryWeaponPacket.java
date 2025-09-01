package org.lupz.doomsdayessentials.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.entity.SentryEntity;

import java.util.function.Supplier;

public class MountSentryWeaponPacket {
	private final int entityId;

	public MountSentryWeaponPacket(int entityId) { this.entityId = entityId; }

	public static void encode(MountSentryWeaponPacket msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.entityId); }
	public static MountSentryWeaponPacket decode(FriendlyByteBuf buf) { return new MountSentryWeaponPacket(buf.readVarInt()); }
	public static void handle(MountSentryWeaponPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			if (!(player.level() instanceof ServerLevel sl)) return;
			var ent = sl.getEntity(msg.entityId);
			if (ent instanceof SentryEntity sentry) {
				sentry.mountGunFrom(player);
			}
		});
		ctx.get().setPacketHandled(true);
	}
} 