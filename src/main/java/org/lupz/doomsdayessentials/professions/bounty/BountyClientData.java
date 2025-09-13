package org.lupz.doomsdayessentials.professions.bounty;

import java.util.ArrayList;
import java.util.List;

public final class BountyClientData {
	private BountyClientData() {}
	public static class Entry { public final String targetName; public final int gears; public Entry(String n,int g){targetName=n;gears=g;} }
	public static final List<Entry> ENTRIES = new ArrayList<>();
	public static void set(List<Entry> list) { ENTRIES.clear(); ENTRIES.addAll(list); }
} 