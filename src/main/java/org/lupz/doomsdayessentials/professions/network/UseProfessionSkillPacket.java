package org.lupz.doomsdayessentials.professions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.professions.CombatenteProfession;
import org.lupz.doomsdayessentials.professions.EngenheiroProfession;
import org.lupz.doomsdayessentials.professions.MedicoProfession;
import org.lupz.doomsdayessentials.professions.ProfissaoManager;
import org.lupz.doomsdayessentials.professions.RastreadorProfession;

import java.util.function.Supplier;

public class UseProfessionSkillPacket {

	public UseProfessionSkillPacket() {}

	public static void encode(UseProfessionSkillPacket msg, FriendlyByteBuf buf) {
		// no payload
	}

	public static UseProfessionSkillPacket decode(FriendlyByteBuf buf) {
		return new UseProfessionSkillPacket();
	}

	public static void handle(UseProfessionSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			String profession = ProfissaoManager.getProfession(player.getUUID());
			if (profession == null) return;
			switch (profession.toLowerCase()) {
				case "medico" -> MedicoProfession.useHealingAbility(player);
				case "rastreador" -> RastreadorProfession.useGlowAbility(player);
				case "combatente" -> CombatenteProfession.activateAdrenaline(player);
				case "engenheiro" -> EngenheiroProfession.useTurretSkill(player);
				case "cacador" -> org.lupz.doomsdayessentials.professions.CacadorProfession.useMarkSkill(player);
				default -> {
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
} 