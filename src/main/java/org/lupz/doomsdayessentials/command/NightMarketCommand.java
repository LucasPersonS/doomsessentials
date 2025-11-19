package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.event.eclipse.market.MarketBlocks;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NightMarketCommand {

    private NightMarketCommand(){}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e){
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (ctx, builder) -> {
            for (var k : ForgeRegistries.ITEMS.getKeys()) builder.suggest(k.toString());
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> PRESET_NAME_SUGGESTIONS = (ctx, builder) -> {
            try {
                if (ctx.getSource().getEntity() instanceof ServerPlayer sp){
                    java.util.UUID mId = findNearestMarket(sp);
                    if (mId != null){
                        var names = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.listPresetNames(mId);
                        for (String n : names) builder.suggest(n);
                    }
                }
            } catch (Exception ignored) {}
            return builder.buildFuture();
        };
        dispatcher.register(Commands.literal("dooms").requires(s->s.hasPermission(2))
            .then(Commands.literal("market")
                .then(Commands.literal("adminui").executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                        ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                        return 0;
                    }
                    java.util.UUID mId = findNearestMarket(sp);
                    if (mId == null){
                        ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                        return 0;
                    }
                    // Find block pos of nearest market block entity with this marketId
                    BlockPos playerPos = sp.blockPosition();
                    int radius = 5;
                    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                    BlockPos foundPos = null;
                    for (int dx=-radius; dx<=radius && foundPos==null; dx++){
                        for (int dy=-radius; dy<=radius && foundPos==null; dy++){
                            for (int dz=-radius; dz<=radius && foundPos==null; dz++){
                                cursor.set(playerPos.getX()+dx, playerPos.getY()+dy, playerPos.getZ()+dz);
                                BlockEntity be = sp.level().getBlockEntity(cursor);
                                if (be instanceof org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity nbe && mId.equals(nbe.getMarketId())){
                                    foundPos = cursor.immutable();
                                }
                            }
                        }
                    }
                    if (foundPos == null){
                        ctx.getSource().sendFailure(Component.literal("Night Market block not found."));
                        return 0;
                    }
                    // Serialize offers and open UI on client
                    var offers = org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(ctx.getSource().getLevel(), mId);
                    String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(ctx.getSource().getLevel(), offers);
                    String active = ((org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity)sp.level().getBlockEntity(foundPos)).getActivePreset();
                    org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new org.lupz.doomsdayessentials.event.eclipse.market.admin.NightMarketAdminOpenPacket(mId, foundPos, json, active));
                    ctx.getSource().sendSuccess(() -> Component.literal("Abrindo UI administrativa do Mercado Negro."), false);
                    return 1;
                }))
                .then(Commands.literal("reloadjson").executes(ctx->{
                    org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager.ensureDefault();
                    org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.reloadFromJson(ctx.getSource().getLevel());
                    ctx.getSource().sendSuccess(() -> Component.literal("Black market JSON reloaded."), true);
                    return 1;
                }))
                .then(Commands.literal("reloadjsonhere").executes(ctx->{
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                        ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                        return 0;
                    }
                    java.util.UUID mId = findNearestMarket(sp);
                    if (mId == null){
                        ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                        return 0;
                    }
                    org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager.ensureDefault();
                    org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.reloadIntoMarket(ctx.getSource().getLevel(), mId);
                    ctx.getSource().sendSuccess(() -> Component.literal("Black market JSON reloaded for nearest market."), true);
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx->{
                    var offers = org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(ctx.getSource().getLevel());
                    int i=1;
                    for (net.minecraft.world.item.trading.MerchantOffer o : offers){
                        String alias = org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.aliasOf(o);
                        final int idx = i;
                        ctx.getSource().sendSuccess(() -> Component.literal(idx+". " + (alias.isEmpty()?"<sem alias>":alias) + " -> " + o.getResult().getHoverName().getString()), false);
                        i++;
                    }
                    return i-1;
                }))
                .then(Commands.literal("search")
                    .then(Commands.argument("query", StringArgumentType.string())
                        .executes(ctx->{
                            String q = StringArgumentType.getString(ctx, "query");
                            var filter = new org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketFilter(q, java.util.Set.of(), null);
                            try{
                                org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager.reload();
                                var offers = org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager.toOffers(ctx.getSource().getLevel(), filter);
                                int i=1;
                                for (net.minecraft.world.item.trading.MerchantOffer o : offers){
                                    String alias = org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.aliasOf(o);
                                    final int idx = i;
                                    ctx.getSource().sendSuccess(() -> Component.literal(idx+". " + (alias.isEmpty()?"<sem alias>":alias) + " -> " + o.getResult().getHoverName().getString()), false);
                                    i++;
                                }
                                return i-1;
                            }catch(Exception ex){
                                ctx.getSource().sendFailure(Component.literal("Erro ao buscar: " + ex.getMessage()));
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("place").executes(ctx->{
                    ServerLevel lvl = ctx.getSource().getLevel();
                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                    lvl.setBlockAndUpdate(pos, MarketBlocks.NIGHT_MARKET_BLOCK.get().defaultBlockState());
                    ctx.getSource().sendSuccess(() -> Component.literal("Night Market placed."), true);
                    return 1;
                }))
                .then(Commands.literal("addtrade")
                    .then(Commands.argument("buy1", StringArgumentType.string())
                        .suggests(ITEM_SUGGESTIONS)
                        .then(Commands.argument("buy1Count", IntegerArgumentType.integer(1))
                            .then(Commands.argument("sell", StringArgumentType.string())
                                .suggests(ITEM_SUGGESTIONS)
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
                .then(Commands.literal("addtradehere")
                    .then(Commands.argument("buy1", StringArgumentType.string()).suggests(ITEM_SUGGESTIONS)
                        .then(Commands.argument("buy1Count", IntegerArgumentType.integer(1))
                            .then(Commands.argument("sell", StringArgumentType.string()).suggests(ITEM_SUGGESTIONS)
                                .then(Commands.argument("sellCount", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("xp", IntegerArgumentType.integer(0))
                                            .then(Commands.argument("priceMult", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                                        ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                                        return 0;
                                                    }
                                                    java.util.UUID mId = findNearestMarket(sp);
                                                    if (mId == null){
                                                        ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                                        return 0;
                                                    }
                                                    boolean ok = NightMarketManager.addOfferTo(
                                                        mId,
                                                        StringArgumentType.getString(ctx, "buy1"),
                                                        IntegerArgumentType.getInteger(ctx, "buy1Count"),
                                                        null, 0,
                                                        StringArgumentType.getString(ctx, "sell"),
                                                        IntegerArgumentType.getInteger(ctx, "sellCount"),
                                                        IntegerArgumentType.getInteger(ctx, "maxUses"),
                                                        IntegerArgumentType.getInteger(ctx, "xp"),
                                                        FloatArgumentType.getFloat(ctx, "priceMult")
                                                    );
                                                    ctx.getSource().sendSuccess(() -> Component.literal(ok?"Trade added to nearest market":"Invalid items"), true);
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
                        .suggests(ITEM_SUGGESTIONS)
                        .then(Commands.argument("buy1Count", IntegerArgumentType.integer(1))
                            .then(Commands.argument("buy2", StringArgumentType.string())
                                .suggests(ITEM_SUGGESTIONS)
                                .then(Commands.argument("buy2Count", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("sell", StringArgumentType.string())
                                        .suggests(ITEM_SUGGESTIONS)
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
                .then(Commands.literal("cleartradeshere").executes(ctx->{
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                        ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                        return 0;
                    }
                    java.util.UUID mId = findNearestMarket(sp);
                    if (mId == null){
                        ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                        return 0;
                    }
                    // Clear only that market's offers
                    org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.clearMarket(mId);
                    ctx.getSource().sendSuccess(() -> Component.literal("Trades cleared for nearest market."), true);
                    return 1;
                }))
                .then(Commands.literal("addheld")
                    .then(Commands.argument("sellCount", IntegerArgumentType.integer(1))
                        .then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
                            .then(Commands.argument("xp", IntegerArgumentType.integer(0))
                                .then(Commands.argument("priceMult", FloatArgumentType.floatArg(0f))
                                    .executes(NightMarketCommand::addHeldItemTrade)
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("preset")
                    .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                    ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                    return 0;
                                }
                                java.util.UUID mId = findNearestMarket(sp);
                                if (mId == null){
                                    ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                    return 0;
                                }
                                String name = StringArgumentType.getString(ctx, "name");
                                try {
                                    int v = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.savePreset(ctx.getSource().getLevel(), mId, name);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Preset saved: " + name + "-v" + v), true);
                                    return 1;
                                } catch (Exception e){
                                    ctx.getSource().sendFailure(Component.literal("Failed to save preset: " + e.getMessage()));
                                    return 0;
                                }
                            })
                        )
                    )
                    .then(Commands.literal("load")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests(PRESET_NAME_SUGGESTIONS)
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                    ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                    return 0;
                                }
                                java.util.UUID mId = findNearestMarket(sp);
                                if (mId == null){
                                    ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                    return 0;
                                }
                                String name = StringArgumentType.getString(ctx, "name");
                                boolean ok = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.loadPreset(ctx.getSource().getLevel(), mId, name, java.util.Optional.empty());
                                if (ok){
                                    ctx.getSource().sendSuccess(() -> Component.literal("Preset loaded: " + name), true);
                                    return 1;
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("Preset not found or failed to load."));
                                    return 0;
                                }
                            })
                            .then(Commands.argument("version", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                        ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                        return 0;
                                    }
                                    java.util.UUID mId = findNearestMarket(sp);
                                    if (mId == null){
                                        ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                        return 0;
                                    }
                                    String name = StringArgumentType.getString(ctx, "name");
                                    int version = IntegerArgumentType.getInteger(ctx, "version");
                                    boolean ok = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.loadPreset(ctx.getSource().getLevel(), mId, name, java.util.Optional.of(version));
                                    if (ok){
                                        ctx.getSource().sendSuccess(() -> Component.literal("Preset loaded: " + name + "-v" + version), true);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Preset version not found or failed to load."));
                                        return 0;
                                    }
                                })
                            )
                        )
                    )
                    .then(Commands.literal("list")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                return 0;
                            }
                            java.util.UUID mId = findNearestMarket(sp);
                            if (mId == null){
                                ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                return 0;
                            }
                            try {
                                var names = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.listPresetNames(mId);
                                if (names.isEmpty()){
                                    ctx.getSource().sendSuccess(() -> Component.literal("No presets found."), false);
                                }
                                for (String n : names){
                                    ctx.getSource().sendSuccess(() -> Component.literal("- " + n), false);
                                }
                                return names.size();
                            } catch (Exception e){
                                ctx.getSource().sendFailure(Component.literal("Failed to list presets: " + e.getMessage()));
                                return 0;
                            }
                        })
                    )
                    .then(Commands.literal("versions")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests(PRESET_NAME_SUGGESTIONS)
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                    ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                    return 0;
                                }
                                java.util.UUID mId = findNearestMarket(sp);
                                if (mId == null){
                                    ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                    return 0;
                                }
                                String name = StringArgumentType.getString(ctx, "name");
                                try {
                                    var versions = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.listPresetVersions(mId, name);
                                    if (versions.isEmpty()){
                                        ctx.getSource().sendSuccess(() -> Component.literal("No versions for preset '"+name+"'."), false);
                                    }
                                    for (int v : versions){
                                        ctx.getSource().sendSuccess(() -> Component.literal("- v" + v), false);
                                    }
                                    return versions.size();
                                } catch (Exception e){
                                    ctx.getSource().sendFailure(Component.literal("Failed to list versions: " + e.getMessage()));
                                    return 0;
                                }
                            })
                        )
                    )
                    .then(Commands.literal("diff")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests(PRESET_NAME_SUGGESTIONS)
                            .then(Commands.argument("v1", IntegerArgumentType.integer(1))
                                .then(Commands.argument("v2", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                            ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                            return 0;
                                        }
                                        java.util.UUID mId = findNearestMarket(sp);
                                        if (mId == null){
                                            ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                            return 0;
                                        }
                                        String name = StringArgumentType.getString(ctx, "name");
                                        int v1 = IntegerArgumentType.getInteger(ctx, "v1");
                                        int v2 = IntegerArgumentType.getInteger(ctx, "v2");
                                        String diff = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.diffPresets(ctx.getSource().getLevel(), mId, name, v1, v2);
                                        ctx.getSource().sendSuccess(() -> Component.literal(diff), false);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    .then(Commands.literal("activate")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests(PRESET_NAME_SUGGESTIONS)
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)){
                                    ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                    return 0;
                                }
                                java.util.UUID mId = findNearestMarket(sp);
                                if (mId == null){
                                    ctx.getSource().sendFailure(Component.literal("No nearby Night Market found."));
                                    return 0;
                                }
                                String name = StringArgumentType.getString(ctx, "name");
                                // Set active preset on nearest block entity
                                BlockPos playerPos = sp.blockPosition();
                                int radius = 5;
                                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                                for (int dx=-radius; dx<=radius; dx++){
                                    for (int dy=-radius; dy<=radius; dy++){
                                        for (int dz=-radius; dz<=radius; dz++){
                                            cursor.set(playerPos.getX()+dx, playerPos.getY()+dy, playerPos.getZ()+dz);
                                            BlockEntity be = sp.level().getBlockEntity(cursor);
                                            if (be instanceof org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity nbe && mId.equals(nbe.getMarketId())){
                                                nbe.setActivePreset(name);
                                                ctx.getSource().sendSuccess(() -> Component.literal("Active preset set: " + name), true);
                                                return 1;
                                            }
                                        }
                                    }
                                }
                                ctx.getSource().sendFailure(Component.literal("Night Market block not found."));
                                return 0;
                            })
                        )
                    )
                )
            )
        );
    }

    private static int addHeldItemTrade(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        // Check if the command source is a player
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        // Get the held item
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("You must hold an item in your main hand."));
            return 0;
        }

        // Get command arguments
        int sellCount = IntegerArgumentType.getInteger(ctx, "sellCount");
        int maxUses = IntegerArgumentType.getInteger(ctx, "maxUses");
        int xp = IntegerArgumentType.getInteger(ctx, "xp");
        float priceMult = FloatArgumentType.getFloat(ctx, "priceMult");

        // Get the item's resource location
        String itemId = ForgeRegistries.ITEMS.getKey(heldItem.getItem()).toString();

        // Add the trade (held item as buy1, no buy2, same item as sell)
        java.util.UUID targetMarket = null;
        if (ctx.getSource().getEntity() instanceof ServerPlayer sp){
            targetMarket = findNearestMarket(sp);
        }
        boolean success = targetMarket != null
            ? NightMarketManager.addOfferTo(targetMarket, itemId, 1, null, 0, itemId, sellCount, maxUses, xp, priceMult)
            : NightMarketManager.addOffer(itemId, 1, null, 0, itemId, sellCount, maxUses, xp, priceMult);

        if (success) {
            ctx.getSource().sendSuccess(() -> Component.literal("Trade added: 1x " + heldItem.getHoverName().getString() + " -> " + sellCount + "x " + heldItem.getHoverName().getString()), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Failed to add trade. Invalid item."));
            return 0;
        }
    }

    private static java.util.UUID findNearestMarket(ServerPlayer sp){
        BlockPos playerPos = sp.blockPosition();
        int radius = 5;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        java.util.UUID found = null;
        for (int dx=-radius; dx<=radius; dx++){
            for (int dy=-radius; dy<=radius; dy++){
                for (int dz=-radius; dz<=radius; dz++){
                    cursor.set(playerPos.getX()+dx, playerPos.getY()+dy, playerPos.getZ()+dz);
                    BlockEntity be = sp.level().getBlockEntity(cursor);
                    if (be instanceof NightMarketBlockEntity nbe){
                        found = nbe.getMarketId();
                        return found;
                    }
                }
            }
        }
        return null;
    }
}
