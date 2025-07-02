package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.client.InjuryClientPacketHandler;

import java.util.function.Supplier;

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

   public float getHealingProgress() {
      return healingProgress;
   }

   public static void handle(UpdateHealingProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> {
          DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> InjuryClientPacketHandler.handleUpdateHealingProgress(msg));
      });
      ctx.get().setPacketHandled(true);
   }
} 