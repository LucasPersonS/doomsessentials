package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DisplayInjuryTitlePacket {
   public final int injuryLevel;

   public DisplayInjuryTitlePacket(int injuryLevel) {
      this.injuryLevel = injuryLevel;
   }

   public DisplayInjuryTitlePacket(FriendlyByteBuf buf) {
      this.injuryLevel = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.injuryLevel);
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.injuryLevel > 0) {
               Minecraft mc = Minecraft.getInstance();
               Component title = Component.translatable("injury.title.main");
               Component subtitle = Component.translatable("injury.title.subtitle." + this.injuryLevel);
               mc.gui.setTimes(10, 70, 20);
               mc.gui.setTitle(title);
               mc.gui.setSubtitle(subtitle);
            }
         });
      });
      ctx.get().setPacketHandled(true);
   }
} 