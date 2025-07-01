package org.lupz.doomsdayessentials.injury.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

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

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> showInjuryTitle(this.injuryLevel));
      });
      ctx.get().setPacketHandled(true);
   }

   private static void showInjuryTitle(int level) {
      if (level > 0) {
         Minecraft mc = Minecraft.getInstance();
         Component title = Component.translatable("injury.title.main");
         Component subtitle = Component.translatable("injury.title.subtitle." + level);
         mc.gui.setTimes(10, 70, 20);
         mc.gui.setTitle(title);
         mc.gui.setSubtitle(subtitle);
      }
   }
} 