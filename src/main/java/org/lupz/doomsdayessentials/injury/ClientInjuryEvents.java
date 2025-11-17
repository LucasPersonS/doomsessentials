package org.lupz.doomsdayessentials.injury;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientInjuryEvents {

    // Prevent opening inventory or any container GUI while downed (client-side)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        InjuryHelper.getCapability(mc.player).ifPresent(cap -> {
            if (cap.isDowned()) {
                Screen screen = event.getScreen();
                if (screen instanceof InventoryScreen ||
                    screen instanceof AbstractContainerScreen<?>) {
                    event.setCanceled(true);
                    // Force-close any open server menu as well
                    mc.player.closeContainer();
                    mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cVocê está abatido e não pode abrir o inventário ou contêineres."));
                }
            }
        });
    }
}
