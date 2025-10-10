package org.lupz.doomsdayessentials.guild.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

/**
 * Minimal command to open the Guild main menu via chat: /organizacao or /organizacao menu
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OrganizacaoMenuCommand {

    private OrganizacaoMenuCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static int deleteOwnGuild(CommandSourceStack source) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild g = gm.getGuildByMember(p.getUUID());
            if (g == null) { source.sendFailure(Component.literal("§cVocê não pertence a uma organização.")); return 0; }
            org.lupz.doomsdayessentials.guild.GuildMember self = g.getMember(p.getUUID());
            if (self == null || self.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.LEADER) {
                source.sendFailure(Component.literal("§cApenas o Líder pode deletar a organização."));
                return 0;
            }
            // Check empty storage
            net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> storage = gm.getOrCreateStorage(g.getName());
            boolean storageEmpty = true;
            for (net.minecraft.world.item.ItemStack s : storage) {
                if (s != null && !s.isEmpty()) { storageEmpty = false; break; }
            }
            if (!storageEmpty) { source.sendFailure(Component.literal("§cO cofre deve estar vazio para deletar a organização.")); return 0; }
            // Check resource bank empty
            org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(level);
            if (!bank.isEmpty(g.getName())) { source.sendFailure(Component.literal("§cOs recursos da organização devem estar zerados para deletar.")); return 0; }
            boolean ok = gm.deleteGuild(g.getName());
            if (ok) {
                bank.removeGuild(g.getName());
                source.sendSuccess(() -> Component.literal("§aOrganização '" + g.getName() + "' deletada."), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFalha ao deletar a organização."));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("organizacao")
            // Default: open menu
            .executes(ctx -> openMenu(ctx.getSource()))
            // Explicit subcommand
            .then(Commands.literal("menu").executes(ctx -> openMenu(ctx.getSource())))
            // New: upgrade subcommand opens the upgrade GUI
            .then(Commands.literal("upgrade").executes(ctx -> openUpgrade(ctx.getSource())))
            // Admin: reset upgrades for a guild (set storage level back to 1)
            .then(Commands.literal("resetupgrades")
                .requires(src -> src.hasPermission(3))
                .then(Commands.argument("guild", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> resetUpgrades(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "guild"))))
            )
            // Leader-only: delete guild if storage and resources are empty
            .then(Commands.literal("deletar").executes(ctx -> deleteOwnGuild(ctx.getSource())))
        );
    }

    private static int openMenu(CommandSourceStack source) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, pl) -> new org.lupz.doomsdayessentials.guild.menu.GuildMainMenu(id, inv),
                    Component.literal("Organização"))
            );
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int openUpgrade(CommandSourceStack source) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, pl) -> new org.lupz.doomsdayessentials.guild.menu.GuildUpgradeMenu(id, inv),
                    Component.literal("Upgrades de Organização"))
            );
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int resetUpgrades(CommandSourceStack source, String guildName) {
        try {
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild g = gm.getGuild(guildName);
            if (g == null) {
                source.sendFailure(Component.literal("§cGuilda não encontrada: " + guildName));
                return 0;
            }
            g.setStorageLevel(1);
            gm.setDirty();
            source.sendSuccess(() -> Component.literal("§aUpgrades da guilda '" + guildName + "' foram resetados para o nível 1."), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}


