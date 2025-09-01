package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ProfessionSelectionOpener {
	public ProfessionSelectionOpener() {}
	public static void encode(ProfessionSelectionOpener msg, FriendlyByteBuf buf) {}
	public static ProfessionSelectionOpener decode(FriendlyByteBuf buf) { return new ProfessionSelectionOpener(); }
	public static void handle(ProfessionSelectionOpener msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> openClient());
		ctx.get().setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private static void openClient() {
		net.minecraft.client.Minecraft.getInstance().setScreen(new org.lupz.doomsdayessentials.professions.menu.SelectProfessionScreen());
	}
} 