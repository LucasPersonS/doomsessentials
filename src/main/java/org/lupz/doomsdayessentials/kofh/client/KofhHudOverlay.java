package org.lupz.doomsdayessentials.kofh.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lupz.doomsdayessentials.kofh.KofhConfig;

/**
 * Simple HUD overlay for King of the Hill.
 * Shows area name, remaining time, current controller or contested status,
 * multiplier, and top scoreboard entries (up to 5).
 */
public final class KofhHudOverlay {
    private KofhHudOverlay() {}

    public static final IGuiOverlay HUD = (gui, gfx, partial, width, height) -> {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // Only render when timer is active
        if (KofhClientState.getRemainingSeconds() <= 0) return;

        // Proximity gating: show HUD only when near the hill center (horizontal distance)
        String dimId = KofhClientState.getDimensionId();
        String playerDim = mc.level != null ? mc.level.dimension().location().toString() : "";
        if (!playerDim.equals(dimId)) return;
        double dx = mc.player.getX() - KofhClientState.getCenterX();
        double dz = mc.player.getZ() - KofhClientState.getCenterZ();
        double dist2 = dx*dx + dz*dz;
        int thr = KofhConfig.HUD_DISTANCE_BLOCKS.get();
        if (dist2 > (thr*thr)) return;

        int panelWidth = 240;
        int lines = Math.min(5, KofhClientState.getEntries().size());
        int panelHeight = 20 + lines*12 + 10; // header + entries + padding
        int x0 = (width - panelWidth) / 2;
        int y0 = 8;
        int x = width / 2;
        int y = y0 + 6;

        String area = KofhClientState.getAreaName();
        int secs = KofhClientState.getRemainingSeconds();
        String controller = KofhClientState.getControllingGuild();
        boolean contested = KofhClientState.isContested();
        double mult = KofhClientState.getMultiplier();

        // Header line
        String tLabel = ChatFormatting.GOLD + "KOFH: " + ChatFormatting.YELLOW + area;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);
        String status;
        if (contested) {
            status = ChatFormatting.RED + "Contestada";
        } else if (controller != null) {
            status = ChatFormatting.GREEN + controller;
        } else {
            status = ChatFormatting.GRAY + "Sem controle";
        }
        // Background panel
        drawPanel(gfx, x0, y0, panelWidth, panelHeight, contested ? 0x55AA0000 : (controller != null ? 0x55226E22 : 0x55333333));
        // Header line
        String header = tLabel + ChatFormatting.WHITE + "  Tempo: " + time + ChatFormatting.DARK_GRAY + "  |  " + ChatFormatting.WHITE + "Mult: x" + String.format("%.2f", mult) + ChatFormatting.DARK_GRAY + "  |  " + ChatFormatting.WHITE + "Ctrl: " + status;
        drawCentered(gfx, header, x, y);
        y += 14;

        // Scoreboard entries
        int rank = 1;
        for (var e : KofhClientState.getEntries()) {
            String line = ChatFormatting.YELLOW + String.valueOf(rank) + ChatFormatting.WHITE + ". " + ChatFormatting.AQUA + e.guildName + ChatFormatting.GRAY + "  " + ChatFormatting.WHITE + "|  " + ChatFormatting.GOLD + e.points + ChatFormatting.GRAY + " pts";
            drawCentered(gfx, line, x, y);
            y += 12;
            if (++rank > 5) break;
        }
    };

    private static void drawCentered(GuiGraphics gfx, String text, int centerX, int y) {
        var font = Minecraft.getInstance().font;
        int w = font.width(text);
        gfx.drawString(font, text, centerX - w/2, y, 0xFFFFFF);
    }

    private static void drawPanel(GuiGraphics gfx, int x, int y, int w, int h, int accentColor) {
        // Base background (semi-transparent dark)
        gfx.fill(x, y, x+w, y+h, 0xAA000000);
        // Accent bar on top
        gfx.fill(x, y, x+w, y+2, accentColor);
        // Border lines
        int border = 0x33555555;
        gfx.fill(x, y, x+w, y+1, border);
        gfx.fill(x, y+h-1, x+w, y+h, border);
        gfx.fill(x, y, x+1, y+h, border);
        gfx.fill(x+w-1, y, x+w, y+h, border);
    }
}
