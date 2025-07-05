package org.lupz.doomsdayessentials.territory.client;

import org.lupz.doomsdayessentials.network.packet.s2c.TerritoryProgressPacket;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public class TerritoryClientState {
    public static String areaName;
    public static String guildName;
    public static int progress;
    public static int total;
    public static boolean running;
    public static long lastUpdate;

    // Local previous snapshot to detect changes
    private static boolean prevRunning;
    private static String prevGuild;

    private static void spawnParticles(int color) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        RandomSource r = mc.player.getRandom();
        for (int i = 0; i < 30; i++) {
            double ox = (r.nextDouble()-0.5)*2.0;
            double oy = r.nextDouble()*1.5 + 0.5;
            double oz = (r.nextDouble()-0.5)*2.0;
            net.minecraft.core.particles.DustParticleOptions smoke = new net.minecraft.core.particles.DustParticleOptions(new Vector3f(
                    ((color>>16)&255)/255f,
                    ((color>>8)&255)/255f,
                    (color&255)/255f), 1.0f);
            mc.level.addParticle(smoke,
                    mc.player.getX()+ox, mc.player.getY()+oy, mc.player.getZ()+oz,
                    0, 0.02, 0);
        }
    }

    public static void update(TerritoryProgressPacket p){
        // Detect capture start / end
        boolean starting = !prevRunning && p.isRunning();
        boolean captured = prevRunning && !p.isRunning();
        boolean guildChanged = prevRunning && p.isRunning() && (prevGuild==null? p.getGuildName()!=null : !prevGuild.equals(p.getGuildName()));

        if (starting || guildChanged) {
            // Green sparkles if a guild gained control, red if contested/reset (guild null)
            int col = p.getGuildName()==null ? 0xFF0000 : 0x00FF00;
            spawnParticles(col);
        }
        if (captured) {
            spawnParticles(0x00FF00);
        }

        running = p.isRunning();
        areaName = p.getAreaName();
        guildName = p.getGuildName();
        progress = p.getProgress();
        total = p.getTotal();
        lastUpdate = System.currentTimeMillis();

        prevRunning = running;
        prevGuild = guildName;
    }
} 