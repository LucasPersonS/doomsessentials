package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.market.MarketBlocks;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NightMarketCommand {

    private NightMarketCommand(){}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e){
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("dooms").requires(s->s.hasPermission(2))
            .then(Commands.literal("market")
                .then(Commands.literal("place").executes(ctx->{
                    ServerLevel lvl = ctx.getSource().getLevel();
                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                    lvl.setBlockAndUpdate(pos, MarketBlocks.NIGHT_MARKET_BLOCK.get().defaultBlockState());
                    ctx.getSource().sendSuccess(() -> Component.literal("Night Market placed."), true);
                    return 1;
                }))
                .then(Commands.literal("addtrade")
                    .then(Commands.argument("buy1", StringArgumentType.string())
                        .then(Commands.argument("buy1Count", IntegerArgumentType.integer(1))
                            .then(Commands.argument("sell", StringArgumentType.string())
                                .then(Commands.argument("sellCount", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("xp", IntegerArgumentType.integer(0))
                                            .then(Commands.argument("priceMult", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> {
                                                    boolean ok = NightMarketManager.addOffer(
                                                        StringArgumentType.getString(ctx, "buy1"),
                                                        IntegerArgumentType.getInteger(ctx, "buy1Count"),
                                                        null, 0,
                                                        StringArgumentType.getString(ctx, "sell"),
                                                        IntegerArgumentType.getInteger(ctx, "sellCount"),
                                                        IntegerArgumentType.getInteger(ctx, "maxUses"),
                                                        IntegerArgumentType.getInteger(ctx, "xp"),
                                                        FloatArgumentType.getFloat(ctx, "priceMult")
                                                    );
                                                    ctx.getSource().sendSuccess(() -> Component.literal(ok?"Trade added":"Invalid items"), true);
                                                    return ok?1:0;
                                                })
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("addtrade2")
                    .then(Commands.argument("buy1", StringArgumentType.string())
                        .then(Commands.argument("buy1Count", IntegerArgumentType.integer(1))
                            .then(Commands.argument("buy2", StringArgumentType.string())
                                .then(Commands.argument("buy2Count", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("sell", StringArgumentType.string())
                                        .then(Commands.argument("sellCount", IntegerArgumentType.integer(1))
                                            .then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("xp", IntegerArgumentType.integer(0))
                                                    .then(Commands.argument("priceMult", FloatArgumentType.floatArg(0f))
                                                        .executes(ctx -> {
                                                            boolean ok = NightMarketManager.addOffer(
                                                                StringArgumentType.getString(ctx, "buy1"),
                                                                IntegerArgumentType.getInteger(ctx, "buy1Count"),
                                                                StringArgumentType.getString(ctx, "buy2"),
                                                                IntegerArgumentType.getInteger(ctx, "buy2Count"),
                                                                StringArgumentType.getString(ctx, "sell"),
                                                                IntegerArgumentType.getInteger(ctx, "sellCount"),
                                                                IntegerArgumentType.getInteger(ctx, "maxUses"),
                                                                IntegerArgumentType.getInteger(ctx, "xp"),
                                                                FloatArgumentType.getFloat(ctx, "priceMult")
                                                            );
                                                            ctx.getSource().sendSuccess(() -> Component.literal(ok?"Trade added":"Invalid items"), true);
                                                            return ok?1:0;
                                                        })
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("cleartrades").executes(ctx->{
                    NightMarketManager.clear();
                    ctx.getSource().sendSuccess(() -> Component.literal("Trades cleared."), true);
                    return 1;
                }))
            )
        );
    }
} 