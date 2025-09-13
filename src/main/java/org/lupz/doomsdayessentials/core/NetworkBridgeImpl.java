package org.lupz.doomsdayessentials.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.lupz.dooms.core.service.NetworkBridge;
import org.lupz.doomsdayessentials.network.PacketHandler;

public class NetworkBridgeImpl implements NetworkBridge {
	@Override
	public void sendSkyTint(ServerPlayer player, int rgb, float alpha) {
		PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
				new org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket(rgb, alpha));
	}
} 