package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.capability.InjuryCapabilityProvider;

import java.util.function.Supplier;

public class UpdateInjuryLevelPacket {
   public final int injuryLevel;

   public UpdateInjuryLevelPacket(int injuryLevel) {
      this.injuryLevel = injuryLevel;
   }

   public UpdateInjuryLevelPacket(FriendlyByteBuf buf) {
      this.injuryLevel = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.injuryLevel);
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft.getInstance().player.getCapability(InjuryCapabilityProvider.INJURY_CAPABILITY).ifPresent(cap -> {
               cap.setInjuryLevel(this.injuryLevel);
            });
         });
      });
      ctx.get().setPacketHandled(true);
   }
} 