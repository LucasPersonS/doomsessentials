package org.lupz.dooms.core.service;

import net.minecraft.server.level.ServerPlayer;

public interface NetworkBridge {
	void sendSkyTint(ServerPlayer player, int rgb, float alpha);
} 