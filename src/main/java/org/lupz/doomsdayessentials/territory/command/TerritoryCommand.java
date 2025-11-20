package org.lupz.doomsdayessentials.territory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import org.lupz.doomsdayessentials.territory.TerritoryEventManager;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.CommandBuildContext;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TerritoryCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher(), e.getBuildContext());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, net.minecraft.commands.CommandBuildContext context) {
        // Root command /territory (admin only)
        var territoryRoot = Commands.literal("territory")
                .requires(src -> src.hasPermission(2));

        // -----------------------------
        // /territory event ...
        var eventCmd = Commands.literal("event")
                .then(Commands.literal("start")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .then(Commands.argument("durationMinutes", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("minPlayers", IntegerArgumentType.integer(1))
                                                .executes(EventCommands::start)))))
                .then(Commands.literal("status")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .executes(EventCommands::status))
                        .executes(EventCommands::status))
                .then(Commands.literal("players")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .then(Commands.argument("minPlayers", IntegerArgumentType.integer(1))
                                        .executes(EventCommands::setPlayers))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(EventCommands::suggestAreaNames)
                                .executes(EventCommands::stop))
                        .executes(EventCommands::stopAll))
                .then(Commands.literal("collect"));

        // -----------------------------
        // /territory generator ...
        var generatorCmd = Commands.literal("generator")
                .then(Commands.literal("info")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .executes(TerritoryCommand::generatorInfo)))
                .then(Commands.literal("set")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("field", StringArgumentType.word()).suggests(TerritoryCommand::suggestFieldNames)
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(TerritoryCommand::generatorSet)))))
                .then(Commands.literal("additem")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("item", ItemArgument.item(context)).suggests(TerritoryCommand::suggestItemNames)
                                        .then(Commands.argument("perHour", IntegerArgumentType.integer(1))
                                                .executes(TerritoryCommand::generatorAddItem)))))
                .then(Commands.literal("delitem")
                        .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                                .then(Commands.argument("item", ItemArgument.item(context)).suggests(TerritoryCommand::suggestItemNames)
                                        .executes(TerritoryCommand::generatorDelItem))))
                .then(Commands.literal("reload").executes(TerritoryCommand::reloadGenerators));

        var adminDelete = Commands.literal("deletearea")
                .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                        .executes(TerritoryCommand::adminDeleteArea));

        var adminSetOwner = Commands.literal("setowner")
                .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                        .then(Commands.argument("guild", StringArgumentType.string()).suggests(TerritoryCommand::suggestGuildNames)
                                .executes(TerritoryCommand::adminSetOwner)));

        var adminUnclaim = Commands.literal("unclaim")
                .then(Commands.argument("area", StringArgumentType.word()).suggests(TerritoryCommand::suggestAreaNames)
                        .executes(TerritoryCommand::adminUnclaim));

        territoryRoot.then(eventCmd).then(generatorCmd).then(adminDelete).then(adminSetOwner).then(adminUnclaim);
        dispatcher.register(territoryRoot);
    }

    // Delegated to EventCommands
    private static int startEvent(CommandContext<CommandSourceStack> ctx) { return EventCommands.start(ctx); }

    private static int stopEvent(CommandContext<CommandSourceStack> ctx) { return EventCommands.stop(ctx); }

    private static int stopAll(CommandContext<CommandSourceStack> ctx) { return EventCommands.stopAll(ctx); }

    private static int status(CommandContext<CommandSourceStack> ctx) { return EventCommands.status(ctx); }

    // ---------------------------------------------------------------
    // /territory event players <area> <minPlayers>
    // ---------------------------------------------------------------
    private static int setPlayers(CommandContext<CommandSourceStack> ctx) { return EventCommands.setPlayers(ctx); }

    /**
     * @deprecated A partir de 0.9.0 use /organizacao recompensas
     */
    @Deprecated
    private static int collect(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(Component.literal("Comando obsoleto. Use /organizacao recompensas."));
        return 0;
    }

    private static int generatorInfo(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        if (mgr.get(areaName) == null) {
            ctx.getSource().sendFailure(Component.literal("Gerador não encontrado."));
            return 0;
        }

        // If invoked by a player, open GUI
        try {
            net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, ply) -> new org.lupz.doomsdayessentials.territory.menu.GeneratorInfoMenu(id, inv, areaName), Component.literal("Gerador: " + areaName)));
            return 1;
        } catch (Exception ignored) {}

        // Fallback to chat when source is console or command block
        ctx.getSource().sendSuccess(() -> Component.literal("Use in-game to view GUI."), false);
        return 1;
    }

    private static int generatorSet(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        String field = StringArgumentType.getString(ctx, "field");
        String value = StringArgumentType.getString(ctx, "value");
        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.createIfAbsent(areaName);
        try {
            switch (field) {
                case "loot" -> {
                    if (data.lootEntries.isEmpty()) data.lootEntries.add(new org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry(value, 1));
                    else data.lootEntries.get(0).id = value;
                }
                case "perhour", "itemsPerHour" -> {
                    int v = Integer.parseInt(value);
                    if (data.lootEntries.isEmpty()) data.lootEntries.add(new org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry("minecraft:stone", v));
                    else data.lootEntries.get(0).perHour = v;
                }
                case "cap", "storageCap" -> data.storageCap = Integer.parseInt(value);
                case "owner" -> data.ownerGuild = value.equalsIgnoreCase("null") ? null : value;
                default -> {
                    ctx.getSource().sendFailure(Component.literal("Campo desconhecido."));
                    return 0;
                }
            }
        } catch (NumberFormatException ex) {
            ctx.getSource().sendFailure(Component.literal("Valor inválido."));
            return 0;
        }
        mgr.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Gerador atualizado."), true);
        return 1;
    }

    private static int reloadGenerators(CommandContext<CommandSourceStack> ctx) {
        org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().reload();
        ctx.getSource().sendSuccess(() -> Component.literal("Geradores recarregados do Gdisco."), true);
        return 1;
    }

    private static int adminDeleteArea(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        boolean ok = AreaManager.get().deleteArea(areaName);
        if (ok) {
            ctx.getSource().sendSuccess(() -> Component.literal("Área removida."), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Área não encontrada."));
        return 0;
    }

    private static int adminSetOwner(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        String guild = StringArgumentType.getString(ctx, "guild");
        var am = AreaManager.get();
        var cur = am.getArea(areaName);
        if (cur == null) {
            ctx.getSource().sendFailure(Component.literal("Área não encontrada."));
            return 0;
        }
        am.deleteArea(areaName);
        ManagedArea safe = new ManagedArea(areaName, org.lupz.doomsdayessentials.combat.AreaType.SAFE, cur.getDimension(), cur.getPos1(), cur.getPos2());
        am.addArea(safe);
        org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().claimArea(areaName, guild);
        var c1 = cur.getPos1(); var c2 = cur.getPos2();
        double cx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        double cy = c1.getY() + 4;
        double cz = (c2.getZ() + c1.getZ()) / 2.0 + 0.5;
        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(areaName, cx, cy, cz, (byte)3));
        ctx.getSource().sendSuccess(() -> Component.literal("Dono atualizado."), true);
        return 1;
    }

    private static int adminUnclaim(CommandContext<CommandSourceStack> ctx) {
        String areaName = StringArgumentType.getString(ctx, "area");
        var am = AreaManager.get();
        var cur = am.getArea(areaName);
        if (cur == null) {
            ctx.getSource().sendFailure(Component.literal("Área não encontrada."));
            return 0;
        }
        am.deleteArea(areaName);
        ManagedArea danger = new ManagedArea(areaName, org.lupz.doomsdayessentials.combat.AreaType.DANGER, cur.getDimension(), cur.getPos1(), cur.getPos2());
        am.addArea(danger);
        org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get().unclaimArea(areaName);
        var c1 = cur.getPos1(); var c2 = cur.getPos2();
        double cx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        double cy = c1.getY() + 4;
        double cz = (c2.getZ() + c1.getZ()) / 2.0 + 0.5;
        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new org.lupz.doomsdayessentials.network.packet.s2c.TerritoryMarkerPacket(areaName, cx, cy, cz, (byte)0));
        ctx.getSource().sendSuccess(() -> Component.literal("Território liberado."), true);
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAreaNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        // Suggest all defined areas
        org.lupz.doomsdayessentials.combat.AreaManager.get().getAreas().forEach(a -> b.suggest(a.getName()));
        return b.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFieldNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        b.suggest("loot");
        b.suggest("itemsPerHour");
        b.suggest("storageCap");
        b.suggest("owner");
        return b.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestItemNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys().forEach(rl -> b.suggest(rl.toString()));
        return b.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestGuildNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null && srv.overworld() != null) {
            org.lupz.doomsdayessentials.guild.GuildsManager.get(srv.overworld()).getGuildNames().forEach(b::suggest);
        }
        return b.buildFuture();
    }

    // ------------------------------------------------------------------
    // additem / delitem impl
    // ------------------------------------------------------------------

    private static int generatorAddItem(CommandContext<CommandSourceStack> ctx) {
        String area = StringArgumentType.getString(ctx, "area");
        ItemInput itemInput = ItemArgument.getItem(ctx, "item");
        int perHour = IntegerArgumentType.getInteger(ctx, "perHour");

        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.createIfAbsent(area);

        ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(itemInput.getItem());
        if (rl == null) {
            ctx.getSource().sendFailure(Component.literal("Item inválido."));
            return 0;
        }

        var existing = data.lootEntries.stream().filter(e -> e.id.equals(rl.toString())).findFirst().orElse(null);
        if (existing != null) {
            existing.perHour = perHour;
            ctx.getSource().sendSuccess(() -> Component.literal("Item atualizado."), true);
        } else {
            data.lootEntries.add(new org.lupz.doomsdayessentials.territory.ResourceAreaData.LootEntry(rl.toString(), perHour));
            ctx.getSource().sendSuccess(() -> Component.literal("Item adicionado."), true);
        }
        mgr.save();
        return 1;
    }

    private static int generatorDelItem(CommandContext<CommandSourceStack> ctx) {
        String area = StringArgumentType.getString(ctx, "area");
        ItemInput itemInput = ItemArgument.getItem(ctx, "item");
        var mgr = org.lupz.doomsdayessentials.territory.ResourceGeneratorManager.get();
        var data = mgr.get(area);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal("Gerador não encontrado."));
            return 0;
        }
        boolean removed = data.lootEntries.removeIf(e -> e.id.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(itemInput.getItem()).toString()));
        if (removed) {
            mgr.save();
            ctx.getSource().sendSuccess(() -> Component.literal("Item removido."), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Item não encontrado."));
        return 0;
    }
} 
