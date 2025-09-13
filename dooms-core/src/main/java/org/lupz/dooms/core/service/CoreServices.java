package org.lupz.dooms.core.service;

public final class CoreServices {
	private CoreServices() {}

	private static volatile NetworkBridge networkBridge;

	public static void setNetworkBridge(NetworkBridge bridge) {
		networkBridge = bridge;
	}

	public static NetworkBridge getNetworkBridge() {
		return networkBridge;
	}
} 