package org.lupz.doomsdayessentials.combat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.combat.AreaManager;
import org.lupz.doomsdayessentials.combat.AreaType;
import org.lupz.doomsdayessentials.combat.ManagedArea;
import net.minecraft.ChatFormatting;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;

@Mod.EventBusSubscriber(modid = org.lupz.doomsdayessentials.EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        org.slf4j.LoggerFactory.getLogger("doomsdayessentials")
                .info("RegisterCommandsEvent fired – registering /area command");
        register(e.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("area")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (AreaType t : AreaType.values()) {
                                    b.suggest(t.name().toLowerCase());
                                }
                                return b.buildFuture();
                            })
                            .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                    .executes(AreaCommand::createArea))))))
                .then(Commands.literal("list")
                    .executes(AreaCommand::listAreas))
                .then(Commands.literal("delete")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(AreaCommand::suggestAreaNames)
                        .executes(AreaCommand::deleteArea)))
                .then(Commands.literal("confirmdelete")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(AreaCommand::suggestAreaNames)
                        .executes(AreaCommand::confirmDeleteArea)))
                .then(Commands.literal("flag")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(AreaCommand::suggestAreaNames)
                        .then(Commands.argument("flag", StringArgumentType.word())
                            .suggests((c, b) -> {
                                b.suggest("prevent_pvp");
                                b.suggest("prevent_explosions");
                                b.suggest("prevent_mob_griefing");
                                b.suggest("prevent_block_modification");
                                b.suggest("allow_flight");
                                b.suggest("disable_fall_damage");
                                b.suggest("prevent_hunger_loss");
                                b.suggest("heal_players");
                                b.suggest("radiation_damage");
                                b.suggest("entry_message");
                                b.suggest("exit_message");
                                return b.buildFuture();
                            })
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests(AreaCommand::suggestFlagValue)
                                .executes(AreaCommand::setFlag)))))
                .then(Commands.literal("expand")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(AreaCommand::suggestAreaNames)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .then(Commands.argument("direction", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    b.suggest("up");
                                    b.suggest("down");
                                    b.suggest("north");
                                    b.suggest("south");
                                    b.suggest("east");
                                    b.suggest("west");
                                    return b.buildFuture();
                                })
                                .executes(AreaCommand::expandArea))))));
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAreaNames(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        AreaManager.get().getAreas().forEach(area -> b.suggest(area.getName()));
        return b.buildFuture();
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFlagValue(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        String flagName = StringArgumentType.getString(c, "flag");
        switch (flagName) {
            case "prevent_pvp", "prevent_explosions", "prevent_mob_griefing", "prevent_block_modification",
                 "allow_flight", "disable_fall_damage", "prevent_hunger_loss", "heal_players" -> {
                b.suggest("true");
                b.suggest("false");
            }
            case "radiation_damage" -> {
                b.suggest("2.0");
                b.suggest("4.0");
            }
            case "entry_message", "exit_message" -> b.suggest("\"Welcome to the safezone!\"");
        }
        return b.buildFuture();
    }

    // ---------------------------------------------------------------------
    // Command impl
    // ---------------------------------------------------------------------

    private static int createArea(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        String typeStr = StringArgumentType.getString(ctx, "type");
        AreaType type;
        try {
            type = AreaType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendFailure(Component.literal("Invalid area type. Use DANGER or SAFE").withStyle(ChatFormatting.RED));
            return 0;
        }

        BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
        BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");

        ServerLevel level = ctx.getSource().getLevel();
        ResourceKey<Level> dim = level.dimension();

        if (AreaManager.get().getArea(name) != null) {
            ctx.getSource().sendFailure(Component.literal("Area with that name already exists.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ManagedArea area = new ManagedArea(name, type, dim, pos1, pos2);
        AreaManager.get().addArea(area);
        ctx.getSource().sendSuccess(() -> Component.literal("Created area '").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("'").withStyle(ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int listAreas(CommandContext<CommandSourceStack> ctx) {
        if (AreaManager.get().getAreas().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No areas defined.").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("--- Areas ---").withStyle(ChatFormatting.GOLD), false);
        AreaManager.get().getAreas().forEach(area -> {
            Component typeComponent = area.getType() == AreaType.DANGER ?
                    Component.literal("DANGER").withStyle(ChatFormatting.RED) :
                    Component.literal("SAFE").withStyle(ChatFormatting.GREEN);

            BlockPos c = new BlockPos((area.getPos1().getX()+area.getPos2().getX())/2,
                                       (area.getPos1().getY()+area.getPos2().getY())/2,
                                       (area.getPos1().getZ()+area.getPos2().getZ())/2);

            MutableComponent tpBtn = Component.literal(" [TP]").withStyle(ChatFormatting.AQUA)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/tp @s " + c.getX() + " " + c.getY() + " " + c.getZ()))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Clique para teleportar"))).withUnderlined(true));

            ctx.getSource().sendSuccess(() -> Component.literal("- ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(area.getName()).withStyle(ChatFormatting.WHITE))
                    .append(tpBtn)
                    .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(typeComponent)
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY)), false);
        });
        return AreaManager.get().getAreas().size();
    }

    // ---------------------------------------------------------------------
    // Delete / confirm delete handling
    // ---------------------------------------------------------------------

    private record PendingDeletion(String areaName, long timestamp) {}

    private static final java.util.Map<java.util.UUID, PendingDeletion> pendingDeletes = new java.util.concurrent.ConcurrentHashMap<>();

    private static final long DELETE_WINDOW_MS = 30_000; // 30s

    private static int deleteArea(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (AreaManager.get().getArea(name) == null) {
            ctx.getSource().sendFailure(Component.literal("Area not found.").withStyle(ChatFormatting.RED));
            return 0;
        }
        java.util.UUID sender = ctx.getSource().getEntity() != null ? ctx.getSource().getEntity().getUUID() : java.util.UUID.randomUUID();
        pendingDeletes.put(sender, new PendingDeletion(name, System.currentTimeMillis()));
        ctx.getSource().sendSuccess(() -> Component.literal("Type ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("/area confirmdelete " + name).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" in 30s to confirm.").withStyle(ChatFormatting.YELLOW)), false);
        return 1;
    }

    private static int confirmDeleteArea(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        java.util.UUID sender = ctx.getSource().getEntity() != null ? ctx.getSource().getEntity().getUUID() : java.util.UUID.randomUUID();
        PendingDeletion pending = pendingDeletes.get(sender);
        if (pending == null) {
            ctx.getSource().sendFailure(Component.literal("You have no pending deletion.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!pending.areaName.equalsIgnoreCase(name)) {
            ctx.getSource().sendFailure(Component.literal("Pending deletion name mismatch.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (System.currentTimeMillis() - pending.timestamp > DELETE_WINDOW_MS) {
            pendingDeletes.remove(sender);
            ctx.getSource().sendFailure(Component.literal("Your deletion request expired.").withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean removed = AreaManager.get().deleteArea(name);
        pendingDeletes.remove(sender);
        if (removed) {
            ctx.getSource().sendSuccess(() -> Component.literal("Area '").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("' deleted.").withStyle(ChatFormatting.GREEN)), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Failed to delete area.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    // ---------------------------------------------------------------------
    // Flag command implementation
    // ---------------------------------------------------------------------

    private static int setFlag(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        String flagName = StringArgumentType.getString(ctx, "flag").toLowerCase();
        String valueStr = StringArgumentType.getString(ctx, "value");

        ManagedArea area = AreaManager.get().getArea(name);
        if (area == null) {
            ctx.getSource().sendFailure(Component.literal("Area not found.").withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean success = false;
        String successMessage = "";

        switch (flagName) {
            case "prevent_pvp" -> {
                area.setPreventPvp(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "PvP protection in '" + name + "' " + (area.isPreventPvp() ? "enabled" : "disabled") + ".";
            }
            case "prevent_explosions" -> {
                area.setPreventExplosions(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Explosion protection in '" + name + "' " + (area.isPreventExplosions() ? "enabled" : "disabled") + ".";
            }
            case "prevent_mob_griefing" -> {
                area.setPreventMobGriefing(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Mob griefing protection in '" + name + "' " + (area.isPreventMobGriefing() ? "enabled" : "disabled") + ".";
            }
            case "prevent_block_modification" -> {
                area.setPreventBlockModification(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Block modification protection in '" + name + "' " + (area.isPreventBlockModification() ? "enabled" : "disabled") + ".";
            }
            case "allow_flight" -> {
                area.setAllowFlight(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Flight in '" + name + "' " + (area.isAllowFlight() ? "allowed" : "disallowed") + ".";
            }
            case "disable_fall_damage" -> {
                area.setDisableFallDamage(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Fall damage in '" + name + "' " + (area.isDisableFallDamage() ? "disabled" : "enabled") + ".";
            }
            case "prevent_hunger_loss" -> {
                area.setPreventHungerLoss(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Hunger loss in '" + name + "' " + (area.isPreventHungerLoss() ? "prevented" : "allowed") + ".";
            }
            case "heal_players" -> {
                area.setHealPlayers(Boolean.parseBoolean(valueStr));
                success = true;
                successMessage = "Player healing in '" + name + "' " + (area.isHealPlayers() ? "enabled" : "disabled") + ".";
            }
            case "radiation_damage" -> {
                try {
                    float dmg = Float.parseFloat(valueStr);
                    area.setRadiationDamage(dmg);
                    success = true;
                    successMessage = "Radiation damage in '" + name + "' set to " + dmg + " HP per second.";
                } catch (NumberFormatException ex) {
                    ctx.getSource().sendFailure(Component.literal("Invalid number for radiation_damage").withStyle(ChatFormatting.RED));
                }
            }
            case "entry_message" -> {
                area.setEntryMessage(valueStr);
                success = true;
                successMessage = "Entry message for '" + name + "' set to: " + valueStr;
            }
            case "exit_message" -> {
                area.setExitMessage(valueStr);
                success = true;
                successMessage = "Exit message for '" + name + "' set to: " + valueStr;
            }
        }

        if (success) {
            final String finalMessage = successMessage;
            ctx.getSource().sendSuccess(() -> Component.literal(finalMessage).withStyle(ChatFormatting.GREEN), true);
            AreaManager.get().saveAreas();
            AreaManager.get().broadcastAreaUpdate();
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Unknown flag '" + flagName + "'.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int expandArea(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        ManagedArea area = AreaManager.get().getArea(name);
        if (area == null) {
            ctx.getSource().sendFailure(Component.literal("Area not found.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        String direction = StringArgumentType.getString(ctx, "direction").toLowerCase();

        BlockPos pos1 = area.getPos1();
        BlockPos pos2 = area.getPos2();

        switch (direction) {
            case "up" -> pos2 = pos2.above(amount);
            case "down" -> pos1 = pos1.below(amount);
            case "north" -> pos1 = pos1.north(amount);
            case "south" -> pos2 = pos2.south(amount);
            case "east" -> pos2 = pos2.east(amount);
            case "west" -> pos1 = pos1.west(amount);
            default -> {
                ctx.getSource().sendFailure(Component.literal("Invalid direction. Use up, down, north, south, east, or west.").withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        ManagedArea newArea = new ManagedArea(area, pos1, pos2);
        AreaManager.get().addArea(newArea);
        AreaManager.get().saveAreas();

        ctx.getSource().sendSuccess(() -> Component.literal("Area '").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("' expanded by " + amount + " blocks " + direction + ".").withStyle(ChatFormatting.GREEN)), true);
        return 1;
    }
} 