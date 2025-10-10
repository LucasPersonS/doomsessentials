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

        CHANNEL.messageBuilder
                (org.lupz.doomsdayessentials.recycler.network.ToggleRecyclerPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.recycler.network.ToggleRecyclerPacket::encode)
                .decoder(org.lupz.doomsdayessentials.recycler.network.ToggleRecyclerPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.recycler.network.ToggleRecyclerPacket::handle)
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

        CHANNEL.messageBuilder
                (org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.SyncFrequencyPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.StartSlidePacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.network.StartSlidePacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.StartSlidePacket::new)
                .consumerMainThread(org.lupz.doomsdayessentials.network.StartSlidePacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.CancelSlidePacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.network.CancelSlidePacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.CancelSlidePacket::new)
                .consumerMainThread(org.lupz.doomsdayessentials.network.CancelSlidePacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.StartSlideS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.StartSlideS2CPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.StartSlideS2CPacket::new)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.StartSlideS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.EndSlideS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.EndSlideS2CPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.EndSlideS2CPacket::new)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.EndSlideS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.bounty.HuntedStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.professions.bounty.HuntedStatePacket::encode)
                .decoder(org.lupz.doomsdayessentials.professions.bounty.HuntedStatePacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.bounty.HuntedStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.lootbox.network.LootboxFinishPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.lootbox.network.LootboxFinishPacket::encode)
                .decoder(org.lupz.doomsdayessentials.lootbox.network.LootboxFinishPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.lootbox.network.LootboxFinishPacket::handle)
                .add();

        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.killlog.network.TakeKillLogItemsPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(org.lupz.doomsdayessentials.killlog.network.TakeKillLogItemsPacket::encode)
                .decoder(org.lupz.doomsdayessentials.killlog.network.TakeKillLogItemsPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.killlog.network.TakeKillLogItemsPacket::handle)
                .add();

        // ProfessionSelection opener (S2C) – open selection screen on client
        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener::encode)
                .decoder(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.professions.menu.ProfessionSelectionOpener::handle)
                .add();

        // Register eclipse packets at the end to avoid shifting existing IDs
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

        // Sky tint S2C (sent on login by SkyColorManager)
        CHANNEL.messageBuilder(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket::encode)
                .decoder(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket::decode)
                .consumerMainThread(org.lupz.doomsdayessentials.network.packet.s2c.SkyTintPacket::handle)
                .add();

        // AbductionBeamPacket removed along with Photon dependency.
    }
} 