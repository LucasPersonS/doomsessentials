package org.lupz.doomsdayessentials.guild;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID)
public final class ResourceWithdrawConversationManager {
    private ResourceWithdrawConversationManager() {}

    private enum Stage { NONE, ASK_ITEM, ASK_AMOUNT }
    private static class State {
        Stage stage = Stage.NONE;
        long expiresAt;
        net.minecraft.world.item.Item pendingItem;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private static final long TIMEOUT_TICKS = 20L * 30L;

    public static void start(ServerPlayer player) {
        State st = new State();
        st.stage = Stage.ASK_ITEM;
        st.expiresAt = player.level().getGameTime() + TIMEOUT_TICKS;
        STATES.put(player.getUUID(), st);
        player.sendSystemMessage(Component.literal("§6§l» §e§lSAQUE §6§l« §7Digite o recurso (§fSucata§7/§fFragmentos§7/§fLâmina§7/§fPlaca§7)."));
        pushItemTabCompletions(player);
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sp = event.getPlayer();
        State st = STATES.get(sp.getUUID());
        if (st == null || st.stage == Stage.NONE) return;
        event.setCanceled(true);
        String msg = event.getRawText().trim();
        long now = sp.level().getGameTime();
        if (now > st.expiresAt) { STATES.remove(sp.getUUID()); clearTabCompletions(sp); sp.sendSystemMessage(Component.literal("§cOperação cancelada por tempo excedido.")); return; }
        if (st.stage == Stage.ASK_ITEM) {
            net.minecraft.world.item.Item chosen = parseItem(msg);
            if (chosen == null) {
                sp.sendSystemMessage(Component.literal("§cRecurso não encontrado. §7Tente novamente (TAB ajuda)."));
                st.expiresAt = now + TIMEOUT_TICKS;
                pushItemTabCompletions(sp);
                return;
            }
            st.pendingItem = chosen;
            st.stage = Stage.ASK_AMOUNT;
            st.expiresAt = now + TIMEOUT_TICKS;
            sp.sendSystemMessage(Component.literal("§6§l» §e§lQUANTIA §6§l« §7Digite a quantidade."));
            clearTabCompletions(sp);
            return;
        }
        if (st.stage == Stage.ASK_AMOUNT) {
            int amount;
            try { amount = Integer.parseInt(msg); } catch (Exception ex) { sp.sendSystemMessage(Component.literal("§cQuantidade inválida. Digite um número.")); return; }
            GuildsManager gm = GuildsManager.get(sp.serverLevel());
            Guild g = gm.getGuildByMember(sp.getUUID());
            if (g == null) { sp.sendSystemMessage(Component.literal("§cVocê não pertence a uma organização.")); STATES.remove(sp.getUUID()); return; }
            GuildResourceBank bank = GuildResourceBank.get(sp.serverLevel());
            String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(st.pendingItem).toString();
            int have = bank.get(g.getName(), itemId);
            if (have < amount) { sp.sendSystemMessage(Component.literal("§eSaldo insuficiente: disponível=" + have)); STATES.remove(sp.getUUID()); return; }
            boolean ok = bank.consume(g.getName(), itemId, amount);
            if (!ok) { sp.sendSystemMessage(Component.literal("§cFalha ao debitar recursos.")); STATES.remove(sp.getUUID()); return; }
            int left = amount;
            while (left > 0) {
                int stackSize = Math.min(st.pendingItem.getMaxStackSize(), left);
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(st.pendingItem, stackSize);
                if (!sp.getInventory().add(stack)) sp.drop(stack, false);
                left -= stackSize;
            }
            gm.logStorageChangeWithName(g.getName(), sp.getUUID(), sp.getName().getString(), "bank_withdraw", itemId, amount, 0, 0);
            sp.sendSystemMessage(Component.literal("§aSacado §e" + amount + "§a de §f" + itemId + " §ado banco de recursos."));
            STATES.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld().getGameTime();
        STATES.entrySet().removeIf(en -> {
            boolean expired = now > en.getValue().expiresAt;
            if (expired) {
                ServerPlayer sp = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(en.getKey());
                if (sp != null) { clearTabCompletions(sp); sp.sendSystemMessage(Component.literal("§cOperação cancelada por tempo excedido.")); }
            }
            return expired;
        });
    }

    public static boolean isInConversation(ServerPlayer sp) { State st = STATES.get(sp.getUUID()); return st != null && st.stage != Stage.NONE; }

    private static void pushItemTabCompletions(ServerPlayer player) {
        try {
            java.util.List<String> names = new java.util.ArrayList<>();
            names.add("Sucata");
            names.add("Fragmentos");
            names.add("Lâmina");
            names.add("Placa");
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.SET, names));
        } catch (Throwable ignored) {}
    }

    private static void clearTabCompletions(ServerPlayer player) {
        try {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.SET, java.util.List.of()));
        } catch (Throwable ignored) {}
    }

    private static net.minecraft.world.item.Item parseItem(String name) {
        String n = normalize(name);
        if (n.startsWith("sucata") || n.contains("scrap")) return org.lupz.doomsdayessentials.item.ModItems.SCRAPMETAL.get();
        if (n.startsWith("fragmento") || n.startsWith("fragmentos") || n.contains("fragment")) return org.lupz.doomsdayessentials.item.ModItems.METAL_FRAGMENTS.get();
        if (n.startsWith("lamina") || n.contains("blade") || n.contains("metalblade")) return org.lupz.doomsdayessentials.item.ModItems.METALBLADE.get();
        if (n.startsWith("placa") || n.contains("sheet") || n.contains("sheetmetal") || n.contains("plate")) return org.lupz.doomsdayessentials.item.ModItems.SHEETMETAL.get();
        return null;
    }

    private static String normalize(String s) {
        String lower = s.trim().toLowerCase(java.util.Locale.ROOT);
        try {
            java.text.Normalizer.Form form = java.text.Normalizer.Form.NFD;
            String norm = java.text.Normalizer.normalize(lower, form);
            return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        } catch (Throwable t) {
            return lower;
        }
    }
}

