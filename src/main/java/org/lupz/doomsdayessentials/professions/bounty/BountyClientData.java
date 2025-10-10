package org.lupz.doomsdayessentials.professions.bounty;

import java.util.ArrayList;
import java.util.List;

public final class BountyClientData {
	private BountyClientData() {}
	public static class Entry { public final String targetName; public final String rewardName; public final int amount; public Entry(String n,String r,int a){targetName=n;rewardName=r;amount=a;} }
	public static final List<Entry> ENTRIES = new ArrayList<>();
	public static void set(List<Entry> list) { ENTRIES.clear(); ENTRIES.addAll(list); }
} 