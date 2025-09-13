package org.lupz.doomsdayessentials.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.killfeed.network.packet.KillFeedPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncAreasPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncCombatStatePacket;
import org.lupz.doomsdayessentials.professions.network.SelectProfessionPacket;
import org.lupz.doomsdayessentials.professions.network.BuyShopItemPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.TerritoryProgressPacket;
import org.lupz.doomsdayessentials.professions.network.BuyEngineerItemPacket;
import org.lupz.doomsdayessentials.recycler.network.ToggleRecyclerPacket;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int index = 0;

    public static void register() {
        CHANNEL.messageBuilder(SyncAreasPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAreasPacket::encode)
                .decoder(SyncAreasPacket::decode)
                .consumerMainThread(SyncAreasPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncCombatStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCombatStatePacket::encode)
                .decoder(SyncCombatStatePacket::decode)
                .consumerMainThread(SyncCombatStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(KillFeedPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(KillFeedPacket::toBytes)
                .decoder(KillFeedPacket::new)
                .consumerMainThread(KillFeedPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectProfessionPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectProfessionPacket::encode)
                .decoder(SelectProfessionPacket::decode)
                .consumerMainThread(SelectProfessionPacket::handle)
                .add();

        CHANNEL.messageBuilder(BuyShopItemPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuyShopItemPacket::encode)
                .decoder(BuyShopItemPacket::decode)
                .consumerMainThread(BuyShopItemPacket::handle)
                .add();

        CHANNEL.messageBuilder(TerritoryProgressPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TerritoryProgressPacket::encode)
                .decoder(TerritoryProgressPacket::decode)
                .consumerMainThread(TerritoryProgressPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket::handle)
                .add();

        CHANNEL.messageBuilder(BuyEngineerItemPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuyEngineerItemPacket::encode)
                .decoder(BuyEngineerItemPacket::decode)
                .consumerMainThread(BuyEngineerItemPacket::handle)
                .add();

        CHANNEL.messageBuilder(ToggleRecyclerPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleRecyclerPacket::encode)
                .decoder(ToggleRecyclerPacket::decode)
                .consumerMainThread(ToggleRecyclerPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.bounty.PlaceBountyPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.professions.bounty.PlaceBountyPacket::encode)
                .decoder(org.lupz.doomsdayessentials.professions.bounty.PlaceBountyPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.bounty.PlaceBountyPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.bounty.RequestBountiesPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.professions.bounty.RequestBountiesPacket::encode)
                .decoder(org.lupz.doomsdayessentials.professions.bounty.RequestBountiesPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.bounty.RequestBountiesPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.bounty.BountiesListPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.professions.bounty.BountiesListPacket::encode)
                .decoder(org.lupz.doomsdayessentials.professions.bounty.BountiesListPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.bounty.BountiesListPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.network.UseProfessionSkillPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.professions.network.UseProfessionSkillPacket::encode)
                .decoder(org.lupz.doomsdayessentials.professions.network.UseProfessionSkillPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.network.UseProfessionSkillPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.MadnessEffectPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.MadnessEffectPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.MadnessEffectPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.MadnessEffectPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener::encode)
                .decoder(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener::handle)
                .add();


        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.SentryShootSoundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.SentryShootSoundPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.SentryShootSoundPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.SentryShootSoundPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.MountSentryWeaponPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.network.MountSentryWeaponPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.MountSentryWeaponPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.MountSentryWeaponPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.ClosedZonePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.ClosedZonePacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.ClosedZonePacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.ClosedZonePacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.EclipseStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.EclipseStatePacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.EclipseStatePacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.EclipseStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.EclipseCyclePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.EclipseCyclePacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.EclipseCyclePacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.EclipseCyclePacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket::handle)
                .add();

        // AbductionBeamPacket removed along with Photon dependency.
    }
} 