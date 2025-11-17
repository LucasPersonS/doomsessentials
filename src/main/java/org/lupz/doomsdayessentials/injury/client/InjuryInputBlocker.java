package org.lupz.doomsdayessentials.injury.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Client-side input restrictions while downed.
 *
 * When the player is in the downed state, allow only WASD crawling movement and chat typing.
 * Explicitly suppress attack, use, jump, sprint, and sneak inputs every tick.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InjuryInputBlocker {
    private InjuryInputBlocker() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.player == null) return;

        if (!InjuryClientState.isDowned()) return;

        // Keep pose enforced client-side (server already does this too)
        if (mc.player.getPose() != net.minecraft.world.entity.Pose.SWIMMING) {
            mc.player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        }

        // Allow only directional movement (W/A/S/D). Block the rest.
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyShift.setDown(false); // sneak
        mc.options.keyDrop.setDown(false); // prevent item dropping while downed

        // Also ensure sprinting flag is off locally to avoid client-side sprint animation
        mc.player.setSprinting(false);
    }
}
