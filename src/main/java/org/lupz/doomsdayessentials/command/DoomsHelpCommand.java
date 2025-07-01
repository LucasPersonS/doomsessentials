package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraft.sounds.SoundSource;
import org.lupz.doomsdayessentials.sound.ModSounds;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DoomsHelpCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("doomshelp")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(DoomsHelpCommand::sendHelp))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("Usage: /doomshelp <message>").withStyle(ChatFormatting.RED));
                    return 0;
                }));
    }

    private static int sendHelp(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sender)) {
            ctx.getSource().sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String text = StringArgumentType.getString(ctx, "message");

        // Build help component
        Component prefix = Component.literal("[Help]").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
        Component name = Component.literal(" " + sender.getName().getString()).withStyle(ChatFormatting.GOLD);
        Component sep = Component.literal(": ").withStyle(ChatFormatting.WHITE);
        Component msg = Component.literal(text).withStyle(ChatFormatting.YELLOW);
        // Clickable TP component
        Style tpStyle = Style.EMPTY.withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + sender.getName().getString()));
        Component tp = Component.literal(" [TP]").setStyle(tpStyle);

        Component finalMsg = prefix.copy().append(name).append(sep).append(msg).append(tp);

        // Send to ops
        ctx.getSource().getServer().getPlayerList().getPlayers().forEach(p -> {
            if (p.hasPermissions(2)) {
                p.sendSystemMessage(finalMsg);
                p.playNotifySound(ModSounds.FREQUENCIA1.get(), SoundSource.MASTER, 1.0f, 1.0f);
            }
        });

        // Confirmation to sender
        sender.sendSystemMessage(Component.literal("Your help request has been sent to the admins.").withStyle(ChatFormatting.GREEN));
        return 1;
    }
} 