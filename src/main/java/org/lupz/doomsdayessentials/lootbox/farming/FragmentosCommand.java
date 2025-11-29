package org.lupz.doomsdayessentials.lootbox.farming;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Comando /fragmentos para abrir menu GUI de troca de fragmentos
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FragmentosCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fragmentos")
                        .executes(FragmentosCommand::openFragmentMenu));
    }

    private static int openFragmentMenu(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cApenas jogadores podem usar este comando."));
            return 0;
        }

        // Abrir menu GUI
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Troca de Fragmentos");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId,
                    net.minecraft.world.entity.player.Inventory playerInv,
                    net.minecraft.world.entity.player.Player p) {
                return new org.lupz.doomsdayessentials.lootbox.FragmentMenu(windowId, playerInv);
            }
        });

        return 1;
    }
}
