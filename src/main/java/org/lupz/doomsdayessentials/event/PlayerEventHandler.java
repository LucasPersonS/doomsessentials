package org.lupz.doomsdayessentials.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.CombatManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.combat.command.AreaCommand;
import org.lupz.doomsdayessentials.combat.command.CombatCommand;
import org.lupz.doomsdayessentials.command.AdminChatCommand;
import org.lupz.doomsdayessentials.command.DoomsHelpCommand;
import org.lupz.doomsdayessentials.command.GlobalChatCommand;
import org.lupz.doomsdayessentials.command.SoundCommand;
import org.lupz.doomsdayessentials.config.EssentialsConfig;
import org.lupz.doomsdayessentials.injury.InjuryCommands;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncAreasPacket;
import org.lupz.doomsdayessentials.network.packet.s2c.SyncCombatStatePacket;
import org.lupz.doomsdayessentials.professions.commands.MedicoCommand;
import org.lupz.doomsdayessentials.professions.commands.ProfissoesCommand;
import org.lupz.doomsdayessentials.professions.commands.RastreadorCommand;
import org.lupz.doomsdayessentials.professions.MedicoProfession;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (CombatManager.get().isCombatLogger(player.getUUID())) {
                player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                CombatManager.get().removeCombatLogger(player.getUUID());
            }

            // Send the full area and combat state to the player who just joined
            PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncAreasPacket(AreaManager.get().getAreas()));
            PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncCombatStatePacket(CombatManager.get().getPlayersInCombat()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (CombatManager.get().isInCombat(player.getUUID())) {
                CombatManager.get().addCombatLogger(player.getUUID());
                String message = player.getDisplayName().getString() + " deslogou em combate. Ruim demais seloko kkk";
                player.server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
            }
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }

            ManagedArea area = AreaManager.get().getAreaAt(player.serverLevel(), player.blockPosition());
            boolean inDangerZone = area != null && area.getType().isDanger();
            boolean inCombat = CombatManager.get().isInCombat(player.getUUID());

            if (inCombat || inDangerZone) {
                String command = event.getParseResults().getReader().getString();
                String commandName = command.startsWith("/") ? command.substring(1).split(" ")[0] : command.split(" ")[0];

                if (EssentialsConfig.ALLOWED_COMBAT_COMMANDS.get().stream().noneMatch(cmd -> cmd.equalsIgnoreCase(commandName))) {
                    player.sendSystemMessage(Component.literal("§cVocê não pode usar este comando em combate ou em uma zona de perigo."));
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        DoomsHelpCommand.register(dispatcher);
        AdminChatCommand.register(dispatcher);
        SoundCommand.register(dispatcher);
        InjuryCommands.register(dispatcher);
        CombatCommand.register(dispatcher);
        AreaCommand.register(dispatcher);
        ProfissoesCommand.register(dispatcher);
        MedicoCommand.register(dispatcher);
        RastreadorCommand.register(dispatcher);
        GlobalChatCommand.register(dispatcher);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            MedicoProfession.tickMedico(event.player);
        }
    }
} 