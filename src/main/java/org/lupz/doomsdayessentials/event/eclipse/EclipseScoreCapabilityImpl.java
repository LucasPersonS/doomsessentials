package org.lupz.doomsdayessentials.event.eclipse;

import net.minecraft.nbt.CompoundTag;

public class EclipseScoreCapabilityImpl implements EclipseScoreCapability {
    private int kills;
    private int deaths;
    private boolean permaDead;

    @Override public int getKills() { return kills; }
    @Override public int getDeaths() { return deaths; }
    @Override public void addKill() { kills++; }
    @Override public void addDeath() { deaths++; }

    @Override public boolean isPermaDead() { return permaDead; }
    @Override public void setPermaDead(boolean dead) { this.permaDead = dead; }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag t = new CompoundTag();
        t.putInt("kills", kills);
        t.putInt("deaths", deaths);
        t.putBoolean("perma", permaDead);
        return t;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.kills = nbt.getInt("kills");
        this.deaths = nbt.getInt("deaths");
        this.permaDead = nbt.getBoolean("perma");
    }
} 