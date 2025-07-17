package org.lupz.doomsdayessentials.professions.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lupz.doomsdayessentials.EssentialsMod;

public class EngineerNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "engineer"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.registerMessage(nextId(),
                BuyEngineerItemPacket.class,
                BuyEngineerItemPacket::encode,
                BuyEngineerItemPacket::decode,
                BuyEngineerItemPacket::handle
        );
        INSTANCE.registerMessage(nextId(),
                UseEngineerHammerPacket.class,
                UseEngineerHammerPacket::encode,
                UseEngineerHammerPacket::decode,
                UseEngineerHammerPacket::handle
        );
    }
} 