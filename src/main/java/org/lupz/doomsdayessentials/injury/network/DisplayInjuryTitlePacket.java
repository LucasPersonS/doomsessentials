package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.injury.client.InjuryClientPacketHandler;

import java.util.function.Supplier;

public class DisplayInjuryTitlePacket {
   private final int injuryLevel;

   public DisplayInjuryTitlePacket(int injuryLevel) {
      this.injuryLevel = injuryLevel;
   }

   public DisplayInjuryTitlePacket(FriendlyByteBuf buf) {
      this.injuryLevel = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.injuryLevel);
   }

   public int getInjuryLevel() {
      return injuryLevel;
   }

   public static void handle(DisplayInjuryTitlePacket msg, Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> InjuryClientPacketHandler.handleDisplayInjuryTitle(msg));
      });
      ctx.get().setPacketHandled(true);
   }
} 