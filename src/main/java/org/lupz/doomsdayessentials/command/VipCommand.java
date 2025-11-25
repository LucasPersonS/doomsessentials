package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VipCommand {

    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("dissoluto");
        builder.suggest("infectado");
        builder.suggest("sobrevivente");
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("dooms").requires(src -> src.hasPermission(2))
                .then(Commands.literal("vip")
                    .then(Commands.literal("set")
                        .then(Commands.argument("variant", StringArgumentType.word()).suggests(VARIANT_SUGGESTIONS)
                            .executes(VipCommand::setSelf)
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> setOther(ctx, EntityArgument.getPlayer(ctx, "player")))))
                    )
                    .then(Commands.literal("get")
                        .executes(VipCommand::getSelf)
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> getOther(ctx, EntityArgument.getPlayer(ctx, "player"))))
                    )
                )
        );
    }

    private static int setSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        String v = normalize(StringArgumentType.getString(ctx, "variant"));
        if (v == null) {
            ctx.getSource().sendFailure(Component.literal("Variante inválida. Use dissoluto, infectado ou sobrevivente."));
            return 0;
        }
        setTier(sp, v);
        ctx.getSource().sendSuccess(() -> Component.literal("VIP definido para ").append(Component.literal(sp.getName().getString() + ": " + v)), true);
        return 1;
    }

    private static int setOther(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        String v = normalize(StringArgumentType.getString(ctx, "variant"));
        if (v == null) {
            ctx.getSource().sendFailure(Component.literal("Variante inválida. Use dissoluto, infectado ou sobrevivente."));
            return 0;
        }
        setTier(target, v);
        ctx.getSource().sendSuccess(() -> Component.literal("VIP definido para ").append(Component.literal(target.getName().getString() + ": " + v)), true);
        target.sendSystemMessage(Component.literal("Seu VIP foi definido para: " + v));
        return 1;
    }

    private static int getSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
            ctx.getSource().sendFailure(Component.literal("Somente jogadores podem usar este comando."));
            return 0;
        }
        String v = getTier(sp);
        ctx.getSource().sendSuccess(() -> Component.literal("Seu VIP: ").append(Component.literal(v == null ? "nenhum" : v)), false);
        return 1;
    }

    private static int getOther(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        String v = getTier(target);
        ctx.getSource().sendSuccess(() -> Component.literal("VIP de ").append(Component.literal(target.getName().getString() + ": " + (v == null ? "nenhum" : v))), false);
        return 1;
    }

    private static void setTier(ServerPlayer p, String tier) {
        p.getPersistentData().putString("vipTier", tier);
    }

    public static String getTier(ServerPlayer p) {
        String t = p.getPersistentData().getString("vipTier");
        if (t == null || t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String v) {
        String s = v == null ? null : v.toLowerCase(Locale.ROOT);
        if ("dissoluto".equals(s) || "infectado".equals(s) || "sobrevivente".equals(s)) return s;
        return null;
    }
}

