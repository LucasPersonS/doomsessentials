package org.lupz.doomsdayessentials.guild.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.block.ModBlocks;
import org.lupz.doomsdayessentials.guild.*;
import org.lupz.doomsdayessentials.guild.ClientGuildData;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.UUID;

/**
 * Registers the main /organizacao command tree.
 *
 * NOTE: At this stage only the "criar" subcommand is implemented – more will
 * be migrated from the legacy mod gradually.
 */
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OrganizacaoCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("organizacao")
                // /organizacao criar <nome> <tag>
                .then(Commands.literal("criar")
                        .then(Commands.argument("nome", StringArgumentType.string())
                                .then(Commands.argument("tag", StringArgumentType.string())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ServerPlayer player;
                                            try {
                                                player = source.getPlayerOrException();
                                            } catch (Exception e) {
                                                return 0; // Command not invoked by player
                                            }
                                            ServerLevel level = player.serverLevel();
                                            GuildsManager manager = GuildsManager.get(level);
                                            UUID uuid = player.getUUID();

                                            if (manager.getGuildByMember(uuid) != null) {
                                                source.sendFailure(Component.literal("Você já pertence a uma organização.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            String nome = StringArgumentType.getString(ctx, "nome");
                                            String tag = StringArgumentType.getString(ctx, "tag");

                                            if (manager.getGuild(nome) != null) {
                                                source.sendFailure(Component.literal("Uma organização com este nome já existe.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            manager.createGuild(nome, tag, uuid);
                                            source.sendSuccess(() -> Component.literal("Organização " + nome + " criada com sucesso!").withStyle(ChatFormatting.GREEN), false);
                                            return 1;
                                        }))))
                // /organizacao add <nick> -> send invite
                .then(Commands.literal("add")
                        .then(Commands.argument("nick", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "nick");
                                    GuildsManager m = GuildsManager.get(player.serverLevel());
                                    Guild guild = m.getGuildByMember(player.getUUID());
                                    if (guild == null) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode adicionar membros.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (m.getGuildByMember(target.getUUID()) != null) {
                                        ctx.getSource().sendFailure(Component.literal("Esse jogador já pertence a outra organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    m.invitePlayer(guild.getName(), target.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Convite enviado!").withStyle(ChatFormatting.GREEN), false);
                                    target.sendSystemMessage(Component.literal("Você foi convidado para a organização " + guild.getTag() + ". Use /organizacao aceitar " + guild.getTag() + " para entrar.").withStyle(ChatFormatting.AQUA));
                                    return 1;
                                })))
                // /organizacao aceitar <tag>
                .then(Commands.literal("aceitar")
                        .then(Commands.argument("tag", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String tag = StringArgumentType.getString(ctx, "tag");
                                    GuildsManager m = GuildsManager.get(player.serverLevel());
                                    if (m.getGuildByMember(player.getUUID()) != null) {
                                        player.sendSystemMessage(Component.literal("Você já pertence a uma organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    // find guild by tag
                                    Guild g = m.getAllGuilds().stream().filter(gl -> gl.getTag().equalsIgnoreCase(tag)).findFirst().orElse(null);
                                    if (g == null) {
                                        player.sendSystemMessage(Component.literal("Organização não encontrada.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (!m.hasInvite(player.getUUID(), g.getName())) {
                                        player.sendSystemMessage(Component.literal("Você não possui convite para esta organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (!m.acceptInvite(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal("Não foi possível entrar na organização (talvez lotada).").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    player.sendSystemMessage(Component.literal("Você entrou na organização " + g.getTag() + "!").withStyle(ChatFormatting.GREEN));
                                    return 1;
                                })))
                // /organizacao kick <nick>  (agora aceita offline)
                .then(Commands.literal("kick")
                        .then(Commands.argument("nick", com.mojang.brigadier.arguments.StringArgumentType.string())
                                // Sugestões: nomes dos membros da guilda
                                .suggests((ctx, builder) -> {
                                    try {
                                        ServerPlayer srcPlayer = ctx.getSource().getPlayerOrException();
                                        GuildsManager gm = GuildsManager.get(srcPlayer.serverLevel());
                                        Guild g = gm.getGuildByMember(srcPlayer.getUUID());
                                        if (g != null) {
                                            var server = ctx.getSource().getServer();
                                            for (GuildMember mem : g.getMembers()) {
                                                String name;
                                                var p = server.getPlayerList().getPlayer(mem.getPlayerUUID());
                                                if (p != null) {
                                                    name = p.getGameProfile().getName();
                                                } else {
                                                    name = server.getProfileCache().get(mem.getPlayerUUID()).map(com.mojang.authlib.GameProfile::getName).orElse(null);
                                                }
                                                if (name != null) builder.suggest(name);
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String nick = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nick");

                                    GuildsManager m = GuildsManager.get(player.serverLevel());
                                    Guild guild = m.getGuildByMember(player.getUUID());
                                    if (guild == null) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode expulsar membros.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    var server = ctx.getSource().getServer();
                                    java.util.UUID targetUUID = null;

                                    // Tenta online primeiro
                                    ServerPlayer online = server.getPlayerList().getPlayerByName(nick);
                                    if (online != null) {
                                        targetUUID = online.getUUID();
                                    } else {
                                        var prof = server.getProfileCache().get(nick);
                                        if (prof.isPresent()) targetUUID = prof.get().getId();
                                    }

                                    if (targetUUID == null) {
                                        ctx.getSource().sendFailure(Component.literal("Jogador não encontrado.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    if (player.getUUID().equals(targetUUID)) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pode se expulsar.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    if (!m.removeMember(guild.getName(), targetUUID)) {
                                        ctx.getSource().sendFailure(Component.literal("Esse jogador não é membro da sua organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    ctx.getSource().sendSuccess(() -> Component.literal("Jogador expulso.").withStyle(ChatFormatting.GREEN), false);

                                    // Se estiver online, notifica a vítima
                                    if (online != null) {
                                        online.sendSystemMessage(Component.literal("§cVocê foi expulso da organização " + guild.getTag()));
                                    }
                                    return 1;
                                })))
                // /organizacao promover <nick>
                .then(Commands.literal("promover")
                        .then(Commands.argument("nick", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "nick");
                                    GuildsManager m = GuildsManager.get(player.serverLevel());
                                    Guild guild = m.getGuildByMember(player.getUUID());
                                    if (guild == null) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode promover membros.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    GuildMember targetMember = guild.getMember(target.getUUID());
                                    if (targetMember == null) {
                                        ctx.getSource().sendFailure(Component.literal("Esse jogador não faz parte da sua organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (targetMember.getRank() != GuildMember.Rank.MEMBER) {
                                        ctx.getSource().sendFailure(Component.literal("Esse jogador já é Oficial ou Líder.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    targetMember.setRank(GuildMember.Rank.OFFICER);
                                    m.setDirty();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Jogador promovido a Oficial.").withStyle(ChatFormatting.GREEN), false);
                                    return 1;
                                })))
                // /organizacao despromover <nick>
                .then(Commands.literal("despromover")
                        .then(Commands.argument("nick", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "nick");
                                    GuildsManager m = GuildsManager.get(player.serverLevel());
                                    Guild guild = m.getGuildByMember(player.getUUID());
                                    if (guild == null) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode despromover membros.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    GuildMember targetMember = guild.getMember(target.getUUID());
                                    if (targetMember == null) {
                                        ctx.getSource().sendFailure(Component.literal("Esse jogador não faz parte da sua organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (targetMember.getRank() != GuildMember.Rank.OFFICER) {
                                        ctx.getSource().sendFailure(Component.literal("Esse jogador não é Oficial.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    targetMember.setRank(GuildMember.Rank.MEMBER);
                                    m.setDirty();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Jogador despromovido.").withStyle(ChatFormatting.GREEN), false);
                                    return 1;
                                })))
                // /organizacao info [tag]
                .then(Commands.literal("info")
                        // Sem argumentos: lista todas as organizações existentes
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            GuildsManager m = GuildsManager.get(server.overworld());
                            int count = 0;
                            ctx.getSource().sendSuccess(() -> Component.literal("§6--- Organizações no servidor ---"), false);
                            for (Guild g : m.getAllGuilds()) {
                                int onlineMembers = (int) g.getMembers().stream().filter(mem -> server.getPlayerList().getPlayer(mem.getPlayerUUID()) != null).count();
                                ctx.getSource().sendSuccess(() -> Component.literal("§e" + g.getTag() + " §7(" + g.getName() + ") §f- §a" + onlineMembers + "/" + g.getMembers().size() + " online"), false);
                                count++;
                            }
                            return count;
                        })
                        // Com argumento: detalhes da organização
                        .then(Commands.argument("tag", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    var server = ctx.getSource().getServer();
                                    GuildsManager m = GuildsManager.get(server.overworld());
                                    for (Guild g : m.getAllGuilds()) {
                                        builder.suggest(g.getTag());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String tag = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "tag");
                                    var server = ctx.getSource().getServer();
                                    GuildsManager m = GuildsManager.get(server.overworld());
                                    Guild guild = m.getAllGuilds().stream().filter(gl -> gl.getTag().equalsIgnoreCase(tag)).findFirst().orElse(null);
                                    if (guild == null) {
                                        ctx.getSource().sendFailure(Component.literal("Organização não encontrada.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    java.util.List<GuildMember> leaders = new java.util.ArrayList<>();
                                    java.util.List<GuildMember> officers = new java.util.ArrayList<>();
                                    java.util.List<GuildMember> online = new java.util.ArrayList<>();
                                    java.util.List<GuildMember> offline = new java.util.ArrayList<>();

                                    for (GuildMember mbr : guild.getMembers()) {
                                        boolean isOnline = server.getPlayerList().getPlayer(mbr.getPlayerUUID()) != null;
                                        switch (mbr.getRank()) {
                                            case LEADER -> leaders.add(mbr);
                                            case OFFICER -> officers.add(mbr);
                                            case MEMBER -> {
                                                if (isOnline) online.add(mbr); else offline.add(mbr);
                                            }
                                        }
                                    }

                                    Component header = Component.literal("§6--- Organização: §e" + guild.getName() + " §6[§b" + guild.getTag() + "§6] ---");
                                    int onlineCount = leaders.stream().map(gm -> server.getPlayerList().getPlayer(gm.getPlayerUUID()) != null ? 1 : 0).reduce(0, Integer::sum)
                                            + officers.stream().map(gm -> server.getPlayerList().getPlayer(gm.getPlayerUUID()) != null ? 1 : 0).reduce(0, Integer::sum)
                                            + online.size();
                                    Component counts = Component.literal("§eMembros Online: §a" + onlineCount + "/" + guild.getMembers().size());

                                    java.util.List<Component> lines = new java.util.ArrayList<>();
                                    lines.add(header);
                                    lines.add(counts);
                                    lines.add(Component.literal("§6--------------------"));

                                    java.util.function.Function<GuildMember, String> nameFn = gm -> {
                                        var p = server.getPlayerList().getPlayer(gm.getPlayerUUID());
                                        if (p != null) return p.getName().getString();
                                        return server.getProfileCache().get(gm.getPlayerUUID()).map(com.mojang.authlib.GameProfile::getName).orElse("?");
                                    };

                                    for (GuildMember leaderMember : leaders) {
                                        lines.add(Component.literal("§6[Líder] §a" + nameFn.apply(leaderMember)));
                                    }
                                    for (GuildMember officerMember : officers) {
                                        boolean onlineOf = server.getPlayerList().getPlayer(officerMember.getPlayerUUID()) != null;
                                        lines.add(Component.literal("§b[Oficial] " + (onlineOf ? "§a" : "§c") + nameFn.apply(officerMember)));
                                    }
                                    for (GuildMember onlineMember : online) {
                                        lines.add(Component.literal("§a" + nameFn.apply(onlineMember)));
                                    }
                                    for (GuildMember offlineMember : offline) {
                                        lines.add(Component.literal("§c" + nameFn.apply(offlineMember)));
                                    }

                                    lines.forEach(comp -> ctx.getSource().sendSuccess(() -> comp, false));
                                    return 1;
                                })))
                // /organizacao mapa
                .then(Commands.literal("mapa").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean on = ClientGuildData.toggleMap(player.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal(on ? "Visualização de território ativada." : "Visualização de território desativada.").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
                    return 1;
                }))
                // /organizacao sair
                .then(Commands.literal("sair").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GuildsManager m = GuildsManager.get(player.serverLevel());
                    Guild guild = m.getGuildByMember(player.getUUID());
                    if (guild == null) {
                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    GuildMember member = guild.getMember(player.getUUID());
                    if (member.getRank() == GuildMember.Rank.LEADER) {
                        ctx.getSource().sendFailure(Component.literal("O líder não pode sair.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (!member.canLeave()) {
                        long ms = member.getTimeUntilCanLeave();
                        long h = ms / 3600000L;
                        long mLeft = (ms % 3600000L) / 60000L;
                        ctx.getSource().sendFailure(Component.literal(String.format("Você precisa esperar %d h %d min para sair.", h, mLeft)).withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    guild.removeMember(player.getUUID());
                    m.setDirty();
                    ctx.getSource().sendSuccess(() -> Component.literal("Você saiu da organização.").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))
                // /organizacao setbase
                .then(Commands.literal("setbase").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GuildsManager m = GuildsManager.get(player.serverLevel());
                    Guild guild = m.getGuildByMember(player.getUUID());
                    if (guild == null) {
                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode definir a base.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (!guild.isInTerritory(player.blockPosition())) {
                        ctx.getSource().sendFailure(Component.literal("A base deve estar dentro do território.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    guild.setBasePosition(player.blockPosition());
                    m.setDirty();
                    ctx.getSource().sendSuccess(() -> Component.literal("Base definida!").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))
                // /organizacao base
                .then(Commands.literal("base").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GuildsManager m = GuildsManager.get(player.serverLevel());
                    Guild guild = m.getGuildByMember(player.getUUID());
                    if (guild == null || guild.getBasePosition() == null) {
                        ctx.getSource().sendFailure(Component.literal("A base não está definida.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    var bp = guild.getBasePosition();
                    player.teleportTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
                    ctx.getSource().sendSuccess(() -> Component.literal("Teleportado para a base.").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))
                // /organizacao totem – gives the totem item to leader
                .then(Commands.literal("totem").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GuildsManager m = GuildsManager.get(player.serverLevel());
                    Guild guild = m.getGuildByMember(player.getUUID());
                    if (guild == null) {
                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode pegar o totem.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (guild.getTotemPosition() != null) {
                        ctx.getSource().sendFailure(Component.literal("O totem já está posicionado.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    player.getInventory().add(new ItemStack(ModBlocks.TOTEM_BLOCK.get()));
                    ctx.getSource().sendSuccess(() -> Component.literal("Totem entregue.").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))
                // /organizacao invadir – start war in foreign territory
                .then(Commands.literal("invadir").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    GuildsManager m = GuildsManager.get(level);
                    Guild attacker = m.getGuildByMember(player.getUUID());
                    if (attacker == null) {
                        ctx.getSource().sendFailure(Component.literal("Você precisa estar em uma organização.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    Guild defender = m.getGuildAt(player.blockPosition());
                    if (defender == null) {
                        ctx.getSource().sendFailure(Component.literal("Você não está no território de nenhuma organização.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (attacker.getName().equals(defender.getName())) {
                        ctx.getSource().sendFailure(Component.literal("Você não pode invadir sua própria organização.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (!m.canStartWarInTerritory(defender.getName())) {
                        ctx.getSource().sendFailure(Component.literal("Este território está sob cooldown de guerra.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    m.startWar(attacker.getName(), defender.getName());
                    ctx.getSource().sendSuccess(() -> Component.literal("Guerra iniciada!").withStyle(ChatFormatting.RED), false);
                    return 1;
                }))
                // /organizacao guerra <tag> – declare war regardless of territory
                .then(Commands.literal("guerra")
                        .then(Commands.argument("org1", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    // Suggest all guild tags
                                    var server = ctx.getSource().getServer();
                                    if (server != null) {
                                        GuildsManager m = GuildsManager.get(server.overworld());
                                        for (Guild g : m.getAllGuilds()) {
                                            builder.suggest(g.getTag());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("org2", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            String first = StringArgumentType.getString(ctx, "org1");
                                            var server = ctx.getSource().getServer();
                                            if (server != null) {
                                                GuildsManager m = GuildsManager.get(server.overworld());
                                                for (Guild g : m.getAllGuilds()) {
                                                    if (!g.getTag().equalsIgnoreCase(first)) {
                                                        builder.suggest(g.getTag());
                                                    }
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            CommandSourceStack src = ctx.getSource();

                                            if (!src.hasPermission(3)) {
                                                src.sendFailure(Component.literal("Somente administradores podem declarar guerras entre organizações.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            String tag1 = StringArgumentType.getString(ctx, "org1");
                                            String tag2 = StringArgumentType.getString(ctx, "org2");

                                            var server = src.getServer();
                                            if (server == null) {
                                                src.sendFailure(Component.literal("Servidor indisponível.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            ServerLevel level;
                                            if (src.getEntity() instanceof ServerPlayer sp) {
                                                level = sp.serverLevel();
                                            } else {
                                                level = server.overworld();
                                            }

                                            GuildsManager m = GuildsManager.get(level);

                                            Guild g1 = m.getAllGuilds().stream().filter(g -> g.getTag().equalsIgnoreCase(tag1)).findFirst().orElse(null);
                                            Guild g2 = m.getAllGuilds().stream().filter(g -> g.getTag().equalsIgnoreCase(tag2)).findFirst().orElse(null);

                                            if (g1 == null || g2 == null) {
                                                src.sendFailure(Component.literal("Organização não encontrada.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                            if (g1.getName().equals(g2.getName())) {
                                                src.sendFailure(Component.literal("Selecione duas organizações diferentes.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                            if (m.getWar(g1.getName(), g2.getName()) != null) {
                                                src.sendFailure(Component.literal("Já existe uma guerra em andamento entre essas organizações.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            java.util.Set<String> attackerSide = new java.util.HashSet<>();
                                            java.util.Set<String> defenderSide = new java.util.HashSet<>();

                                            attackerSide.add(g1.getName());
                                            defenderSide.add(g2.getName());

                                            if (org.lupz.doomsdayessentials.guild.GuildConfig.INCLUDE_ALLIES_IN_WAR.get()) {
                                                attackerSide.addAll(g1.getAllies());
                                                defenderSide.addAll(g2.getAllies());
                                            }

                                            final int[] warsStarted = {0};
                                            for (String atk : attackerSide) {
                                                for (String def : defenderSide) {
                                                    if (atk.equals(def)) continue;
                                                    if (m.getWar(atk, def) == null) {
                                                        m.startWar(atk, def);
                                                        warsStarted[0]++;
                                                    }
                                                }
                                            }

                                            src.sendSuccess(() -> Component.literal("Guerra iniciada envolvendo " + warsStarted[0] + " confrontos entre as alianças de " + g1.getTag() + " e " + g2.getTag() + "!").withStyle(ChatFormatting.RED), false);
                                            return 1;
                                        }))))
                // /organizacao endwar <org1> <org2> – end war between organizations
                .then(Commands.literal("endwar")
                        .then(Commands.argument("org1", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    // Suggest all guild tags
                                    var server = ctx.getSource().getServer();
                                    if (server != null) {
                                        GuildsManager m = GuildsManager.get(server.overworld());
                                        for (Guild g : m.getAllGuilds()) {
                                            builder.suggest(g.getTag());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("org2", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            String first = StringArgumentType.getString(ctx, "org1");
                                            var server = ctx.getSource().getServer();
                                            if (server != null) {
                                                GuildsManager m = GuildsManager.get(server.overworld());
                                                for (Guild g : m.getAllGuilds()) {
                                                    if (!g.getTag().equalsIgnoreCase(first)) {
                                                        builder.suggest(g.getTag());
                                                    }
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            CommandSourceStack src = ctx.getSource();

                                            if (!src.hasPermission(3)) {
                                                src.sendFailure(Component.literal("Somente administradores podem encerrar guerras entre organizações.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            String tag1 = StringArgumentType.getString(ctx, "org1");
                                            String tag2 = StringArgumentType.getString(ctx, "org2");

                                            var server = src.getServer();
                                            if (server == null) {
                                                src.sendFailure(Component.literal("Servidor indisponível.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            ServerLevel level;
                                            if (src.getEntity() instanceof ServerPlayer sp) {
                                                level = sp.serverLevel();
                                            } else {
                                                level = server.overworld();
                                            }

                                            GuildsManager m = GuildsManager.get(level);

                                            Guild g1 = m.getAllGuilds().stream().filter(g -> g.getTag().equalsIgnoreCase(tag1)).findFirst().orElse(null);
                                            Guild g2 = m.getAllGuilds().stream().filter(g -> g.getTag().equalsIgnoreCase(tag2)).findFirst().orElse(null);

                                            if (g1 == null || g2 == null) {
                                                src.sendFailure(Component.literal("Organização não encontrada.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                            if (g1.getName().equals(g2.getName())) {
                                                src.sendFailure(Component.literal("Selecione duas organizações diferentes.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                            if (m.getWar(g1.getName(), g2.getName()) == null) {
                                                src.sendFailure(Component.literal("Não há guerra em andamento entre essas organizações.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            if (m.endWar(g1.getName(), g2.getName())) {
                                                src.sendSuccess(() -> Component.literal("Guerra encerrada entre " + g1.getTag() + " e " + g2.getTag() + ".").withStyle(ChatFormatting.GREEN), false);
                                            } else {
                                                src.sendFailure(Component.literal("Erro ao encerrar guerra.").withStyle(ChatFormatting.RED));
                                            }
                                            return 1;
                                        }))))
                // /organizacao alianca <tag> – toggle alliance
                .then(Commands.literal("alianca")
                        .then(Commands.argument("tag", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    CommandSourceStack src = ctx.getSource();
                                    try {
                                        ServerPlayer p = src.getPlayerOrException();
                                        GuildsManager man = GuildsManager.get(p.serverLevel());
                                        Guild selfG = man.getGuildByMember(p.getUUID());
                                        if (selfG != null) {
                                            for (Guild g : man.getAllGuilds()) {
                                                if (!g.getTag().equalsIgnoreCase(selfG.getTag())) {
                                                    builder.suggest(g.getTag());
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    GuildsManager m = GuildsManager.get(player.serverLevel());
                                    Guild guild = m.getGuildByMember(player.getUUID());
                                    if (guild == null) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pertence a uma organização.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (guild.getMember(player.getUUID()).getRank() != GuildMember.Rank.LEADER) {
                                        ctx.getSource().sendFailure(Component.literal("Apenas o líder pode gerenciar alianças.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    String tag = StringArgumentType.getString(ctx, "tag");
                                    Guild target = m.getAllGuilds().stream().filter(g -> g.getTag().equalsIgnoreCase(tag)).findFirst().orElse(null);
                                    if (target == null) {
                                        ctx.getSource().sendFailure(Component.literal("Organização não encontrada.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    if (guild.getName().equals(target.getName())) {
                                        ctx.getSource().sendFailure(Component.literal("Você não pode formar aliança consigo mesmo.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    boolean allied = m.areAllied(guild.getName(), target.getName());
                                    if (allied) {
                                        m.removeAlliance(guild.getName(), target.getName());
                                        ctx.getSource().sendSuccess(() -> Component.literal("Aliança com " + target.getTag() + " removida.").withStyle(ChatFormatting.YELLOW), false);
                                    } else {
                                        if (!m.addAlliance(guild.getName(), target.getName())) {
                                            ctx.getSource().sendFailure(Component.literal("Limite máximo de alianças atingido em uma das organizações.").withStyle(ChatFormatting.RED));
                                            return 0;
                                        }
                                        ctx.getSource().sendSuccess(() -> Component.literal("Aliança formada com " + target.getTag() + "!").withStyle(ChatFormatting.GREEN), false);
                                    }
                                    return 1;
                                })))
        );
    }
} 