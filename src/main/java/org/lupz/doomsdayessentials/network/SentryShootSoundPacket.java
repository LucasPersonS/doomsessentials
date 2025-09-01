package org.lupz.doomsdayessentials.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SentryShootSoundPacket {
	private final int entityId;
	private final String soundId;
	private final float volume;
	private final float pitch;

	public SentryShootSoundPacket(int entityId, String soundId, float volume, float pitch) {
		this.entityId = entityId;
		this.soundId = soundId;
		this.volume = volume;
		this.pitch = pitch;
	}

	public static void encode(SentryShootSoundPacket msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.entityId);
		buf.writeUtf(msg.soundId);
		buf.writeFloat(msg.volume);
		buf.writeFloat(msg.pitch);
	}

	public static SentryShootSoundPacket decode(FriendlyByteBuf buf) {
		return new SentryShootSoundPacket(buf.readVarInt(), buf.readUtf(), buf.readFloat(), buf.readFloat());
	}

	public static void handle(SentryShootSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var mc = Minecraft.getInstance();
			if (mc.level == null) return;
			var ent = mc.level.getEntity(msg.entityId);
			if (ent == null) return;
			SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(msg.soundId));
			if (se != null) {
				mc.level.playLocalSound(ent.getX(), ent.getY(), ent.getZ(), se, SoundSource.BLOCKS, msg.volume, msg.pitch, false);
			}
		});
		ctx.get().setPacketHandled(true);
	}
} 