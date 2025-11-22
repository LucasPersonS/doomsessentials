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

    private static int createGuild(CommandSourceStack source, String name, String tag) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            
            // Check if player is already in a guild
            org.lupz.doomsdayessentials.guild.Guild existing = gm.getGuildByMember(p.getUUID());
            if (existing != null) {
                source.sendFailure(Component.literal("§cVocê já pertence a organização '" + existing.getName() + "'."));
                return 0;
            }
            
            // Check if guild name already exists
            if (gm.getGuild(name) != null) {
                source.sendFailure(Component.literal("§cJá existe uma organização com o nome '" + name + "'."));
                return 0;
            }
            
            // Validate name and tag length
            if (name.length() < 3 || name.length() > 16) {
                source.sendFailure(Component.literal("§cO nome da organização deve ter entre 3 e 16 caracteres."));
                return 0;
            }
            
            if (tag.length() < 2 || tag.length() > 6) {
                source.sendFailure(Component.literal("§cA tag da organização deve ter entre 2 e 6 caracteres."));
                return 0;
            }

            int cost = org.lupz.doomsdayessentials.guild.GuildConfig.GUILD_CREATION_COST_SCRAPS.get();
            net.minecraft.world.item.Item scrapItem = org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get();
            int available = p.getInventory().countItem(scrapItem);
            if (available < cost) {
                source.sendFailure(Component.literal("§cVocê precisa de " + cost + " sucatas para criar uma organização."));
                return 0;
            }
            int toRemove = cost;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = p.getInventory().getItem(i);
                if (stack.is(scrapItem)) {
                    int rem = Math.min(toRemove, stack.getCount());
                    stack.shrink(rem);
                    toRemove -= rem;
                    if (toRemove <= 0) break;
                }
            }

            // Create the guild
            gm.createGuild(name, tag, p.getUUID());
            source.sendSuccess(() -> Component.literal("§aOrganização '§6" + name + "§a' [§6" + tag + "§a] criada com sucesso!"), true);
            source.sendSuccess(() -> Component.literal("§eVocê é agora o líder da organização. Use §6/organizacao§e para gerenciar."), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErro ao criar organização: " + e.getMessage()));
            return 0;
        }
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
            // Open organization mail UI
            .then(Commands.literal("correio").executes(ctx -> openMail(ctx.getSource())))
            // New: upgrade subcommand opens the upgrade GUI
            .then(Commands.literal("upgrade").executes(ctx -> openUpgrade(ctx.getSource())))
            // Withdraw items from guild storage: /organizacao sacar <item_id> <quantidade>
            .then(Commands.literal("sacar")
                .then(Commands.argument("item", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .then(Commands.argument("quantidade", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> withdraw(
                            ctx.getSource(),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "item"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "quantidade")
                        ))
                    )
                )
            )
            // Withdraw items from guild resource bank: /organizacao sacarrecursos <item_id> <quantidade>
            .then(Commands.literal("sacarrecursos")
                .then(Commands.argument("item", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .then(Commands.argument("quantidade", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> withdrawResources(
                            ctx.getSource(),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "item"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "quantidade")
                        ))
                    )
                )
            )
            // Create guild: /organizacao criar <nome> <tag>
            .then(Commands.literal("criar")
                .then(Commands.argument("nome", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .then(Commands.argument("tag", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> createGuild(
                            ctx.getSource(),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nome"),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "tag")
                        ))
                    )
                )
            )
            // Admin: reset upgrades for a guild (set storage level back to 1)
            .then(Commands.literal("resetupgrades")
                .requires(src -> src.hasPermission(3))
                .then(Commands.argument("guild", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> resetUpgrades(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "guild"))))
            )
            // Leader-only: delete guild if storage and resources are empty
            .then(Commands.literal("deletar").executes(ctx -> deleteOwnGuild(ctx.getSource())))
            // Alliances: invite via chat
            .then(Commands.literal("alianca")
                .then(Commands.argument("nome", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> inviteAlliance(
                        ctx.getSource(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nome")
                    ))
                )
            )
            // Alliances: accept invite
            .then(Commands.literal("aceitar")
                .then(Commands.argument("nome", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> acceptAlliance(
                        ctx.getSource(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nome")
                    ))
                )
            )
            // Alliances: break alliance
            .then(Commands.literal("quebraralianca")
                .then(Commands.argument("nome", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> breakAlliance(
                        ctx.getSource(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nome")
                    ))
                )
            )
            // List groups
            .then(Commands.literal("grupos").executes(ctx -> listGroups(ctx.getSource())))
            // Info by tag
            .then(Commands.literal("info")
                .then(Commands.argument("tag", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> infoByTag(
                        ctx.getSource(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "tag")
                    ))
                )
            )
            // Admin: toggle guild storage debug logging
            .then(Commands.literal("debugstorage")
                .requires(src -> src.hasPermission(3))
                .then(Commands.literal("on").executes(ctx -> {
                    org.lupz.doomsdayessentials.guild.GuildConfig.STORAGE_DEBUG_ENABLED.set(true);
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§aDebug do cofre ativado."), true);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    org.lupz.doomsdayessentials.guild.GuildConfig.STORAGE_DEBUG_ENABLED.set(false);
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eDebug do cofre desativado."), true);
                    return 1;
                }))
            )
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

    private static int openMail(CommandSourceStack source) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, pl) -> new org.lupz.doomsdayessentials.guild.menu.GuildMailMenu(id, inv),
                    Component.literal("Correio da Organização"))
            );
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int withdraw(CommandSourceStack source, String itemId, int quantidade) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild g = gm.getGuildByMember(p.getUUID());
            if (g == null) { source.sendFailure(Component.literal("§cVocê não pertence a uma organização.")); return 0; }
            int moved = gm.withdrawFromGuildStorage(p, g.getName(), itemId, quantidade);
            if (moved <= 0) {
                source.sendFailure(Component.literal("§eItem indisponível no cofre ou quantidade insuficiente."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("§aSacado §e" + moved + "§a do item §f" + itemId + " §ado cofre."), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int withdrawResources(CommandSourceStack source, String itemId, int quantidade) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild g = gm.getGuildByMember(p.getUUID());
            if (g == null) { source.sendFailure(Component.literal("§cVocê não pertence a uma organização.")); return 0; }
            org.lupz.doomsdayessentials.guild.GuildResourceBank bank = org.lupz.doomsdayessentials.guild.GuildResourceBank.get(level);
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(itemId);
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || item == net.minecraft.world.level.block.Blocks.AIR.asItem()) { source.sendFailure(Component.literal("§cItem inválido: " + itemId)); return 0; }
            boolean accepted = item == org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get() ||
                               item == org.lupz.doomsdayessentials.item.ModItems.METAL_FRAGMENTS.get() ||
                               item == org.lupz.doomsdayessentials.item.ModItems.METALBLADE.get() ||
                               item == org.lupz.doomsdayessentials.item.ModItems.SHEETMETAL.get();
            if (!accepted) { source.sendFailure(Component.literal("§eApenas recursos aceitos podem ser sacados.")); return 0; }
            int have = bank.get(g.getName(), itemId);
            if (have < quantidade) { source.sendFailure(Component.literal("§eSaldo insuficiente: disponível=" + have)); return 0; }
            boolean ok = bank.consume(g.getName(), itemId, quantidade);
            if (!ok) { source.sendFailure(Component.literal("§cFalha ao debitar recursos.")); return 0; }
            int left = quantidade;
            while (left > 0) {
                int stackSize = Math.min(item.getMaxStackSize(), left);
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, stackSize);
                if (!p.getInventory().add(stack)) p.drop(stack, false);
                left -= stackSize;
            }
            gm.logStorageChangeWithName(g.getName(), p.getUUID(), p.getName().getString(), "bank_withdraw", itemId, quantidade, 0, 0);
            source.sendSuccess(() -> Component.literal("§aSacado §e" + quantidade + "§a de §f" + itemId + " §ado banco de recursos."), true);
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

    private static int inviteAlliance(CommandSourceStack source, String targetGuildName) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild self = gm.getGuildByMember(p.getUUID());
            if (self == null) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cVocê não pertence a uma organização.")); return 0; }
            org.lupz.doomsdayessentials.guild.GuildMember me = self.getMember(p.getUUID());
            if (me == null || (me.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.LEADER && me.getRank() != org.lupz.doomsdayessentials.guild.GuildMember.Rank.OFFICER)) {
                source.sendFailure(net.minecraft.network.chat.Component.literal("§cApenas Líderes/Oficiais podem convidar alianças."));
                return 0;
            }
            org.lupz.doomsdayessentials.guild.Guild target = gm.getGuild(targetGuildName);
            if (target == null) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cOrganização não encontrada: " + targetGuildName)); return 0; }
            if (self.getName().equals(target.getName())) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cNão é possível formar aliança consigo mesmo.")); return 0; }
            if (gm.areAllied(self.getName(), target.getName())) { source.sendFailure(net.minecraft.network.chat.Component.literal("§eJá existe aliança com " + target.getName())); return 0; }
            int maxAllies = org.lupz.doomsdayessentials.guild.GuildConfig.MAX_ALLIANCES.get();
            if (maxAllies != 0 && self.getAllies().size() >= maxAllies) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cLimite de alianças atingido.")); return 0; }
            gm.sendAllianceInvite(self.getName(), target.getName());
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§aConvite de aliança enviado para §6" + target.getName()), true);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int acceptAlliance(CommandSourceStack source, String fromGuildName) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild self = gm.getGuildByMember(p.getUUID());
            if (self == null) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cVocê não pertence a uma organização.")); return 0; }
            boolean ok = gm.acceptAllianceInvite(self.getName(), fromGuildName);
            if (!ok) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cNão há convite pendente de " + fromGuildName)); return 0; }
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§aAliança formada com §6" + fromGuildName), true);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int breakAlliance(CommandSourceStack source, String otherGuildName) {
        try {
            ServerPlayer p = source.getPlayerOrException();
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild self = gm.getGuildByMember(p.getUUID());
            if (self == null) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cVocê não pertence a uma organização.")); return 0; }
            boolean ok = gm.removeAlliance(self.getName(), otherGuildName);
            if (!ok) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cNão há aliança ativa com " + otherGuildName)); return 0; }
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eAliança encerrada com §6" + otherGuildName), true);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int listGroups(CommandSourceStack source) {
        try {
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (org.lupz.doomsdayessentials.guild.Guild g : gm.getAllGuilds()) {
                int leaders = (int) g.getMembers().stream().filter(m -> m.getRank() == org.lupz.doomsdayessentials.guild.GuildMember.Rank.LEADER).count();
                int officers = (int) g.getMembers().stream().filter(m -> m.getRank() == org.lupz.doomsdayessentials.guild.GuildMember.Rank.OFFICER).count();
                int members = (int) g.getMembers().stream().filter(m -> m.getRank() == org.lupz.doomsdayessentials.guild.GuildMember.Rank.MEMBER).count();
                lines.add("§6" + g.getName() + " §7[" + g.getTag() + "] §fL:" + leaders + " O:" + officers + " M:" + members + " §eAllies:" + g.getAllies().size());
            }
            if (lines.isEmpty()) { source.sendFailure(net.minecraft.network.chat.Component.literal("§eNenhuma organização encontrada.")); return 0; }
            for (String s : lines) source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(s), false);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int infoByTag(CommandSourceStack source, String tag) {
        try {
            net.minecraft.server.level.ServerLevel level = source.getLevel();
            org.lupz.doomsdayessentials.guild.GuildsManager gm = org.lupz.doomsdayessentials.guild.GuildsManager.get(level);
            org.lupz.doomsdayessentials.guild.Guild found = null;
            for (org.lupz.doomsdayessentials.guild.Guild g : gm.getAllGuilds()) if (g.getTag().equalsIgnoreCase(tag)) { found = g; break; }
            if (found == null) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cOrganização não encontrada com a tag " + tag)); return 0; }
            final String fName = found.getName();
            final String fTag = found.getTag();
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§6" + fName + " §7[" + fTag + "]"), false);
            for (org.lupz.doomsdayessentials.guild.GuildMember m : found.getMembers()) {
                String name = java.util.Optional.ofNullable(level.getServer().getPlayerList().getPlayer(m.getPlayerUUID())).map(p -> p.getName().getString()).orElse(m.getPlayerUUID().toString());
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§f- " + name + " §7(" + m.getRank().name() + ")"), false);
            }
            if (!found.getAllies().isEmpty()) {
                final String allies = String.join(", ", found.getAllies());
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eAlianças: " + allies), false);
            }
            return 1;
        } catch (Exception e) { return 0; }
    }
}


