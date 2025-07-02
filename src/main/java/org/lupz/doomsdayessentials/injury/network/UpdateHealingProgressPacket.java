package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;

import java.util.function.Supplier;

public class UpdateHealingProgressPacket {
   public final float healingProgress;

   public UpdateHealingProgressPacket(float healingProgress) {
      this.healingProgress = healingProgress;
   }

   public UpdateHealingProgressPacket(FriendlyByteBuf buf) {
      this.healingProgress = buf.readFloat();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeFloat(this.healingProgress);
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Minecraft.getInstance().player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
            cap.setHealingProgress(this.healingProgress);
         });
      });
      ctx.get().setPacketHandled(true);
   }
} 