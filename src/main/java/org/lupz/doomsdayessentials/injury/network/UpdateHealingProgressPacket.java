package org.lupz.doomsdayessentials.injury.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;

public class UpdateHealingProgressPacket {
   private final float healingProgress;

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
          DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
              Minecraft.getInstance().player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
                  cap.setHealingProgress(this.healingProgress);
                  cap.setHealing(this.healingProgress >= 0.0F);
              });
          });
      });
      ctx.get().setPacketHandled(true);
   }
} 