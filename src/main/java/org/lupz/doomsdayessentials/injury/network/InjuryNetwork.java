package org.lupz.doomsdayessentials.injury.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lupz.doomsdayessentials.EssentialsMod;

public class InjuryNetwork {
   private static final String PROTOCOL_VERSION = "1";
   public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
       ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "injury_network"),
       () -> PROTOCOL_VERSION,
       PROTOCOL_VERSION::equals,
       PROTOCOL_VERSION::equals
   );
   private static int packetId = 0;

   private static int id() {
      return packetId++;
   }

   public static void register() {
      INSTANCE.messageBuilder(UpdateHealingProgressPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
          .encoder(UpdateHealingProgressPacket::encode)
          .decoder(UpdateHealingProgressPacket::new)
          .consumerMainThread(UpdateHealingProgressPacket::handle)
          .add();
      INSTANCE.messageBuilder(DisplayInjuryTitlePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
          .encoder(DisplayInjuryTitlePacket::encode)
          .decoder(DisplayInjuryTitlePacket::new)
          .consumerMainThread(DisplayInjuryTitlePacket::handle)
          .add();
      INSTANCE.messageBuilder(UpdateInjuryLevelPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
          .encoder(UpdateInjuryLevelPacket::encode)
          .decoder(UpdateInjuryLevelPacket::new)
          .consumerMainThread(UpdateInjuryLevelPacket::handle)
          .add();
   }

   public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
      INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
   }
} 