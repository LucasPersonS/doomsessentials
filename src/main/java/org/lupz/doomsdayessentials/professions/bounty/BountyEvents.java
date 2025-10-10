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
		e.getDispatcher().register(
			Commands.literal("bounty")
				.then(
					Commands.literal("place")
						.then(
							Commands.argument("player", StringArgumentType.word())
								.then(
									Commands.argument("gears", IntegerArgumentType.integer(1))
										.executes(ctx -> {
											ServerPlayer sp = ctx.getSource().getPlayerOrException();
											if (!org.lupz.doomsdayessentials.professions.CacadorProfession.isHunter(sp)) {
												ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§cApenas Caçadores de Recompensa podem usar este comando."));
												return 0;
											}
											String name = StringArgumentType.getString(ctx, "player");
											int gears = IntegerArgumentType.getInteger(ctx, "gears");
											ServerPlayer target = sp.server.getPlayerList().getPlayerByName(name);
											if (target == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Jogador não encontrado")); return 0; }
											boolean ok = BountyManager.placeBounty(sp, target.getUUID(), org.lupz.doomsdayessentials.item.ModItems.GEARS.get(), gears);
											ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(ok ? "Recompensa colocada" : "Itens insuficientes"), false);
											return ok ? 1 : 0;
										})
								)
						)
				)
				.then(
					Commands.literal("accept")
						.then(
							Commands.argument("player", StringArgumentType.word())
								.executes(ctx -> {
									ServerPlayer sp = ctx.getSource().getPlayerOrException();
									String name = StringArgumentType.getString(ctx, "player");
									ServerPlayer target = sp.server.getPlayerList().getPlayerByName(name);
									if (target == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Jogador não encontrado")); return 0; }
									boolean ok = BountyManager.acceptBounty(sp, target.getUUID());
									ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(ok ? "Caçada aceita" : "Falha ao aceitar"), false);
									return ok ? 1 : 0;
								})
						)
				)
				.then(
					Commands.literal("admin")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("reset")
							.executes(ctx -> { BountyManager.clearAllBounties(); ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eMural de recompensas resetado."), true); return 1; }))
						.then(Commands.literal("cleanup")
							.executes(ctx -> { int n = BountyManager.cleanupExpiredBounties(); ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§aRemovidas " + n + " caçadas expiradas."), true); return 1; }))
						.then(Commands.literal("remove")
							.then(Commands.argument("player", StringArgumentType.word())
								.executes(ctx -> {
									ServerPlayer sp = ctx.getSource().getPlayerOrException();
									String name = StringArgumentType.getString(ctx, "player");
									ServerPlayer target = sp.server.getPlayerList().getPlayerByName(name);
									if (target == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Jogador não encontrado")); return 0; }
									boolean removed = BountyManager.removeBountiesForTarget(target.getUUID());
									ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(removed ? "§cCaçadas removidas para " + name : "§7Nenhuma caçada para o jogador."), true);
									return removed ? 1 : 0;
								}))
						)
						.then(Commands.literal("clearaccept")
							.then(Commands.literal("hunter")
								.then(Commands.argument("player", StringArgumentType.word())
									.executes(ctx -> {
										ServerPlayer sp = ctx.getSource().getPlayerOrException();
										String name = StringArgumentType.getString(ctx, "player");
										ServerPlayer hunter = sp.server.getPlayerList().getPlayerByName(name);
										if (hunter == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Jogador não encontrado")); return 0; }
										boolean ok = BountyManager.clearAcceptanceForHunter(hunter.getUUID());
										ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(ok ? "§eAceitação limpa para o caçador." : "§7Nada para limpar."), true);
										return ok ? 1 : 0;
									})
								)
							)
							.then(Commands.literal("target")
								.then(Commands.argument("player", StringArgumentType.word())
									.executes(ctx -> {
										ServerPlayer sp = ctx.getSource().getPlayerOrException();
										String name = StringArgumentType.getString(ctx, "player");
										ServerPlayer target = sp.server.getPlayerList().getPlayerByName(name);
										if (target == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Jogador não encontrado")); return 0; }
										int n = BountyManager.clearAcceptanceForTarget(target.getUUID());
										ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(n > 0 ? ("§e" + n + " aceitações limpas para o alvo.") : "§7Nada para limpar."), true);
										return n > 0 ? 1 : 0;
									})
								)
							)
						)
					)
				);

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