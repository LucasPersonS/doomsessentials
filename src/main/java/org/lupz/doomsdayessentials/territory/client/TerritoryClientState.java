package org.lupz.doomsdayessentials.territory.client;

import org.lupz.doomsdayessentials.network.packet.s2c.TerritoryProgressPacket;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import org.lupz.doomsdayessentials.client.ClientCombatState;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import net.minecraft.core.BlockPos;

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

    private static void spawnParticlesAroundPlayer(int color) {
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

    private static void spawnBorderParticles(ManagedArea area, int color) {
        if (area == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        RandomSource r = RandomSource.create();
        BlockPos p1 = area.getPos1();
        BlockPos p2 = area.getPos2();
        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        // Spawn a handful of particles along the edges each call.
        for (int i = 0; i < 100; i++) {
            double x, y, z;
            int edge = r.nextInt(12);
            switch (edge) {
                case 0 -> { x=minX; y=r.nextInt(maxY-minY+1)+minY; z=r.nextInt(maxZ-minZ+1)+minZ; }
                case 1 -> { x=maxX+1; y=r.nextInt(maxY-minY+1)+minY; z=r.nextInt(maxZ-minZ+1)+minZ; }
                case 2 -> { x=r.nextInt(maxX-minX+1)+minX; y=minY; z=r.nextInt(maxZ-minZ+1)+minZ; }
                case 3 -> { x=r.nextInt(maxX-minX+1)+minX; y=maxY+1; z=r.nextInt(maxZ-minZ+1)+minZ; }
                case 4 -> { x=r.nextInt(maxX-minX+1)+minX; y=r.nextInt(maxY-minY+1)+minY; z=minZ; }
                case 5 -> { x=r.nextInt(maxX-minX+1)+minX; y=r.nextInt(maxY-minY+1)+minY; z=maxZ+1; }
                default -> { x=minX; y=minY; z=minZ; }
            }
            net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(new Vector3f(
                    ((color>>16)&255)/255f,
                    ((color>>8)&255)/255f,
                    (color&255)/255f), 1.0f);
            mc.level.addParticle(dust, x+0.5, y+0.5, z+0.5, 0, 0.01, 0);
        }
    }

    public static void update(TerritoryProgressPacket p){
        // Detect capture start / end
        boolean starting = !prevRunning && p.isRunning();
        boolean captured = prevRunning && !p.isRunning();
        boolean guildChanged = prevRunning && p.isRunning() && (prevGuild==null? p.getGuildName()!=null : !prevGuild.equals(p.getGuildName()));

        ManagedArea area = ClientCombatState.getAreaByName(p.getAreaName());

        int color;
        if (p.isRunning()) {
            color = (p.getGuildName()==null) ? 0xFFA500 : 0x00FF00; // orange if contested, green if progressing
        } else {
            color = 0x0066FF; // blue when idle (no players)
        }

        // Removed client-side particle rendering – now handled server-side with colored dust

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