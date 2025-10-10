package org.lupz.doomsdayessentials.professions.bounty.client;

public final class HuntedClientState {
	private static boolean hunted = false;
	private static long huntedUntilMs = 0L;

	private HuntedClientState() {}

	public static void setHunted(boolean isHunted, long untilEpochMs) {
		if (isHunted) {
			hunted = true;
			huntedUntilMs = Math.max(huntedUntilMs, untilEpochMs);
		} else {
			hunted = false;
			huntedUntilMs = 0L;
		}
	}

	public static boolean isHunted() {
		if (!hunted) return false;
		if (huntedUntilMs <= 0L) return true;
		return System.currentTimeMillis() <= huntedUntilMs;
	}

	public static long getHuntedUntilMs() {
		return huntedUntilMs;
	}
} 