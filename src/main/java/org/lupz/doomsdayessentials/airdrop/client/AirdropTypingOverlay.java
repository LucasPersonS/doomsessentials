package org.lupz.doomsdayessentials.airdrop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Client-side HUD overlay that displays a typing animation message for Airdrop events.
 */
public class AirdropTypingOverlay {
    private static String messageKey = null;
    private static String[] messageArgs = new String[0];
    private static long startMs = 0L;
    private static byte currentState = 0;

    // Typing speed and total display time
    private static final long TYPE_SPEED_MS_PER_CHAR = 40L; // ~25 chars/sec
    private static final long BASE_DISPLAY_MS = 2500L; // base time after typing completes
    private static final float SCALE = 1.75f; // bigger text

    public static final IGuiOverlay OVERLAY = (gui, graphics, partial, width, height) -> {
        if (messageKey == null || Minecraft.getInstance().player == null) return;
        String fullText = net.minecraft.network.chat.Component.translatable(messageKey, (Object[])messageArgs).getString();

        long now = System.currentTimeMillis();
        int totalChars = fullText.length();
        long typingDuration = Math.max(250L, totalChars * TYPE_SPEED_MS_PER_CHAR);
        long elapsed = now - startMs;
        long totalDuration = typingDuration + BASE_DISPLAY_MS;
        if (elapsed < 0 || elapsed > totalDuration) {
            // End of display
            messageKey = null;
            return;
        }

        // Compute how many characters to render (typing effect)
        int shownChars;
        if (elapsed < typingDuration) {
            shownChars = (int)Math.max(0, Math.min(totalChars, elapsed / TYPE_SPEED_MS_PER_CHAR));
        } else {
            shownChars = totalChars;
        }

        String textToShow = fullText.substring(0, Math.max(0, Math.min(totalChars, shownChars)));

        // Choose color by state
        int color;
        if (currentState == org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_OPENED) {
            color = 0xFFFFAA; // yellow
        } else if (currentState == org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_LANDED) {
            color = 0xAAFFFF; // cyan
        } else {
            color = 0xFF5555; // red for despawn
        }

        // Fade out in the last 500ms
        float alpha = 1.0f;
        long fadeStart = totalDuration - 500L;
        if (elapsed > fadeStart) {
            alpha = Math.max(0f, (totalDuration - elapsed) / 500f);
        }

        var font = Minecraft.getInstance().font;
        int strWidth = (int)(font.width(textToShow) * SCALE);
        int x = (width - strWidth) / 2;
        int y = 34; // below top HUD

        RenderSystem.enableBlend();
        com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA.value,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE.value,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value
        );
        int a = ((int)(alpha * 255f) & 0xFF) << 24;

        graphics.pose().pushPose();
        graphics.pose().scale(SCALE, SCALE, 1.0f);
        int scaledX = (int)(x / SCALE);
        int scaledY = (int)(y / SCALE);

        // Optional nickname highlight: if first arg exists, highlight its occurrence in the text
        int highlightColor = 0x55FFFF; // aqua for player nick
        String nick = (messageArgs != null && messageArgs.length > 0) ? messageArgs[0] : null;
        int nickStartFull = (nick != null && !nick.isEmpty()) ? fullText.indexOf(nick) : -1;
        if (nickStartFull >= 0) {
            int nickEndFull = nickStartFull + nick.length();
            int preLen = Math.max(0, Math.min(shownChars, nickStartFull));
            int typedNickLen = Math.max(0, Math.min(nick.length(), shownChars - nickStartFull));
            int postLen = Math.max(0, shownChars - (nickStartFull + typedNickLen));

            String pre = textToShow.substring(0, preLen);
            String nickShown = typedNickLen > 0 ? fullText.substring(nickStartFull, nickStartFull + typedNickLen) : "";
            String post = postLen > 0 ? fullText.substring(nickStartFull + typedNickLen, nickStartFull + typedNickLen + postLen) : "";

            int dx = 0;
            if (!pre.isEmpty()) {
                graphics.drawString(font, pre, scaledX, scaledY, a | color);
                dx += font.width(pre);
            }
            if (!nickShown.isEmpty()) {
                graphics.drawString(font, nickShown, scaledX + dx, scaledY, a | highlightColor);
                dx += font.width(nickShown);
            }
            if (!post.isEmpty()) {
                graphics.drawString(font, post, scaledX + dx, scaledY, a | color);
            }
        } else {
            graphics.drawString(font, textToShow, scaledX, scaledY, a | color);
        }
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    };

    public static void showKey(String key, byte state, String... args) {
        messageKey = key;
        messageArgs = args == null ? new String[0] : args;
        currentState = state;
        startMs = System.currentTimeMillis();

        // Play UI sound feedback according to state
        var sndMgr = Minecraft.getInstance().getSoundManager();
        net.minecraft.sounds.SoundEvent se;
        if (state == org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_OPENED) {
            // UI_TOAST_IN is available directly as a SoundEvent constant
            se = net.minecraft.sounds.SoundEvents.UI_TOAST_IN;
        } else if (state == org.lupz.doomsdayessentials.airdrop.network.AirdropNoticePacket.STATE_LANDED) {
            // UI_BUTTON_CLICK requires retrieving the underlying SoundEvent from its holder/supplier
            se = net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get();
        } else {
            // UI_TOAST_OUT is available directly as a SoundEvent constant
            se = net.minecraft.sounds.SoundEvents.UI_TOAST_OUT;
        }
        sndMgr.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(se, 1.0f));
    }
}
