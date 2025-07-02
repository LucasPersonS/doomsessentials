package org.lupz.doomsdayessentials.injury.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;
import org.lupz.doomsdayessentials.injury.network.DisplayInjuryTitlePacket;
import org.lupz.doomsdayessentials.injury.network.UpdateDownedStatePacket;
import org.lupz.doomsdayessentials.injury.network.UpdateHealingProgressPacket;
import org.lupz.doomsdayessentials.injury.network.UpdateInjuryLevelPacket;

public class InjuryClientPacketHandler {

    public static void handleUpdateDownedState(UpdateDownedStatePacket msg) {
        InjuryClientState.setDowned(msg.isDowned(), msg.getDownedUntil());
        if (msg.isDowned()) {
            Minecraft.getInstance().setScreen(new DownedScreen());
        } else {
            if (Minecraft.getInstance().screen instanceof DownedScreen) {
                Minecraft.getInstance().setScreen(null);
            }
        }
    }

    public static void handleUpdateHealingProgress(UpdateHealingProgressPacket msg) {
        Minecraft.getInstance().player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
            cap.setHealingProgress(msg.getHealingProgress());
            cap.setHealing(msg.getHealingProgress() >= 0.0F);
        });
    }

    public static void handleDisplayInjuryTitle(DisplayInjuryTitlePacket msg) {
        if (msg.getInjuryLevel() > 0) {
            Minecraft mc = Minecraft.getInstance();
            Component title = Component.translatable("injury.title.main");
            Component subtitle = Component.translatable("injury.title.subtitle." + msg.getInjuryLevel());
            mc.gui.setTimes(10, 70, 20);
            mc.gui.setTitle(title);
            mc.gui.setSubtitle(subtitle);
        }
    }

    public static void handleUpdateInjuryLevel(UpdateInjuryLevelPacket msg) {
        Minecraft.getInstance().player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
            cap.setInjuryLevel(msg.getInjuryLevel());
        });
    }
} 