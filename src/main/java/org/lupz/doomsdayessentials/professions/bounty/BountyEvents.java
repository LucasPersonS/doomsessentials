package org.lupz.doomsdayessentials.professions.bounty;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public class BountyEvents {
	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent e) {
		e.getDispatcher().register(Commands.literal("bounty")
			.then(Commands.literal("place")
				.then(Commands.argument("player", StringArgumentType.word())
					.then(Commands.argument("gears", IntegerArgumentType.integer(1))
						.executes(ctx -> {
							ServerPlayer sp = ctx.getSource().getPlayerOrException();
							String name = StringArgumentType.getString(ctx, "player");
							int gears = IntegerArgumentType.getInteger(ctx, "gears");
							ServerPlayer target = sp.server.getPlayerList().getPlayerByName(name);
							if (target == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Jogador não encontrado")); return 0; }
							boolean ok = BountyManager.placeBounty(sp, target.getUUID(), gears);
							ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(ok ? "Recompensa colocada" : "Gears insuficientes"), false);
							return ok ? 1 : 0;
						})))));

	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onPlayerKill(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) {
		// no-op
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
		if (!(e.getEntity() instanceof ServerPlayer victim)) return;
		if (!(e.getSource().getEntity() instanceof ServerPlayer killer)) return;
		BountyManager.onPlayerKilled(killer, victim);
	}
} 