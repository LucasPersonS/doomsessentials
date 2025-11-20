package org.lupz.doomsdayessentials.event.eclipse.market.admin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketBlockEntity;
import org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** C2S: Actions triggered from NightMarketAdminScreen */
public class NightMarketAdminActionPacket {
    public enum Type { ADD_TRADE, DELETE_TRADE, SAVE_PRESET, LOAD_PRESET, ACTIVATE_PRESET, CLEAR_MARKET, START_SESSION, UNDO, REDO, COMMIT, CANCEL, OPEN_EDITOR }

    private final Type type;
    private final UUID marketId;
    private final BlockPos pos; // for activate preset
    private final String buy1Id, buy2Id, sellId, presetName;
    private final String bundleAlias, bundleItems;
    private final int buy1Count, buy2Count, sellCount, maxUses, xp;
    private final float priceMult;
    private final Integer version; // optional, for load
    private final Integer deleteIndex; // for DELETE_TRADE

    public NightMarketAdminActionPacket(Type type, UUID marketId, BlockPos pos,
                                        String buy1Id, int buy1Count,
                                        String buy2Id, int buy2Count,
                                        String sellId, int sellCount,
                                        int maxUses, int xp, float priceMult,
                                        String presetName, Integer version){
        this.type = type; this.marketId = marketId; this.pos = pos;
        this.buy1Id = buy1Id; this.buy1Count = buy1Count;
        this.buy2Id = buy2Id; this.buy2Count = buy2Count;
        this.sellId = sellId; this.sellCount = sellCount;
        this.maxUses = maxUses; this.xp = xp; this.priceMult = priceMult;
        this.presetName = presetName; this.version = version;
        this.bundleAlias = null; this.bundleItems = null; this.deleteIndex = null;
    }

    public static NightMarketAdminActionPacket addTrade(UUID marketId, String buy1Id, int buy1Count, String buy2Id, int buy2Count,
                                                        String sellId, int sellCount, int maxUses, int xp, float priceMult){
        return new NightMarketAdminActionPacket(Type.ADD_TRADE, marketId, null, buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult, null, null);
    }

    public static NightMarketAdminActionPacket addTradeHere(UUID marketId, BlockPos pos, String buy1Id, int buy1Count, String buy2Id, int buy2Count,
                                                            String sellId, int sellCount, int maxUses, int xp, float priceMult){
        return new NightMarketAdminActionPacket(Type.ADD_TRADE, marketId, pos, buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult, null, null);
    }

    public static NightMarketAdminActionPacket addBundleTrade(UUID marketId, String buy1Id, int buy1Count, String buy2Id, int buy2Count,
                                                              String bundleAlias, String bundleItems, int maxUses, int xp, float priceMult){
        NightMarketAdminActionPacket p = new NightMarketAdminActionPacket(Type.ADD_TRADE, marketId, null, buy1Id, buy1Count, buy2Id, buy2Count, org.lupz.doomsdayessentials.item.ModItems.BLACK_MARKET_BUNDLE.getId().toString(), 1, maxUses, xp, priceMult, null, null);
        p.bundleAliasField(bundleAlias); p.bundleItemsField(bundleItems); return p;
    }

    public static NightMarketAdminActionPacket addBundleTradeHere(UUID marketId, BlockPos pos, String buy1Id, int buy1Count, String buy2Id, int buy2Count,
                                                                  String bundleAlias, String bundleItems, int maxUses, int xp, float priceMult){
        NightMarketAdminActionPacket p = new NightMarketAdminActionPacket(Type.ADD_TRADE, marketId, pos, buy1Id, buy1Count, buy2Id, buy2Count, org.lupz.doomsdayessentials.item.ModItems.BLACK_MARKET_BUNDLE.getId().toString(), 1, maxUses, xp, priceMult, null, null);
        p.bundleAliasField(bundleAlias); p.bundleItemsField(bundleItems); return p;
    }

    private void bundleAliasField(String alias){
        try{ java.lang.reflect.Field f = NightMarketAdminActionPacket.class.getDeclaredField("bundleAlias"); f.setAccessible(true); f.set(this, alias); }catch(Exception ignored){}
    }

    private void bundleItemsField(String items){
        try{ java.lang.reflect.Field f = NightMarketAdminActionPacket.class.getDeclaredField("bundleItems"); f.setAccessible(true); f.set(this, items); }catch(Exception ignored){}
    }

    public static NightMarketAdminActionPacket savePreset(UUID marketId, String presetName){
        return new NightMarketAdminActionPacket(Type.SAVE_PRESET, marketId, null, null,0,null,0,null,0,0,0,0f, presetName, null);
    }

    public static NightMarketAdminActionPacket loadPreset(UUID marketId, String presetName, Integer version){
        return new NightMarketAdminActionPacket(Type.LOAD_PRESET, marketId, null, null,0,null,0,null,0,0,0,0f, presetName, version);
    }

    public static NightMarketAdminActionPacket activatePreset(BlockPos pos, String presetName){
        return new NightMarketAdminActionPacket(Type.ACTIVATE_PRESET, null, pos, null,0,null,0,null,0,0,0,0f, presetName, null);
    }

    public static NightMarketAdminActionPacket clearMarket(UUID marketId){
        return new NightMarketAdminActionPacket(Type.CLEAR_MARKET, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket startSession(UUID marketId){
        return new NightMarketAdminActionPacket(Type.START_SESSION, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket undo(UUID marketId){
        return new NightMarketAdminActionPacket(Type.UNDO, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket redo(UUID marketId){
        return new NightMarketAdminActionPacket(Type.REDO, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket commit(UUID marketId){
        return new NightMarketAdminActionPacket(Type.COMMIT, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket cancel(UUID marketId){
        return new NightMarketAdminActionPacket(Type.CANCEL, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket openEditor(UUID marketId, BlockPos pos){
        return new NightMarketAdminActionPacket(Type.OPEN_EDITOR, marketId, pos, null,0,null,0,null,0,0,0,0f, null, null);
    }

    public static NightMarketAdminActionPacket deleteTrade(UUID marketId, int index){
        NightMarketAdminActionPacket p = new NightMarketAdminActionPacket(Type.DELETE_TRADE, marketId, null, null,0,null,0,null,0,0,0,0f, null, null);
        try{ java.lang.reflect.Field f = NightMarketAdminActionPacket.class.getDeclaredField("deleteIndex"); f.setAccessible(true); f.set(p, index);}catch(Exception ignored){}
        return p;
    }

    public static void encode(NightMarketAdminActionPacket msg, FriendlyByteBuf buf){
        buf.writeEnum(msg.type);
        buf.writeBoolean(msg.marketId != null); if (msg.marketId != null) buf.writeUUID(msg.marketId);
        buf.writeBoolean(msg.pos != null); if (msg.pos != null) buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.buy1Id==null?"":msg.buy1Id);
        buf.writeInt(msg.buy1Count);
        buf.writeUtf(msg.buy2Id==null?"":msg.buy2Id);
        buf.writeInt(msg.buy2Count);
        buf.writeUtf(msg.sellId==null?"":msg.sellId);
        buf.writeInt(msg.sellCount);
        buf.writeInt(msg.maxUses);
        buf.writeInt(msg.xp);
        buf.writeFloat(msg.priceMult);
        buf.writeUtf(msg.presetName==null?"":msg.presetName);
        buf.writeUtf(msg.bundleAlias==null?"":msg.bundleAlias);
        buf.writeUtf(msg.bundleItems==null?"":msg.bundleItems);
        buf.writeBoolean(msg.version != null); if (msg.version != null) buf.writeInt(msg.version);
        buf.writeBoolean(msg.deleteIndex != null); if (msg.deleteIndex != null) buf.writeInt(msg.deleteIndex);
    }

    public static NightMarketAdminActionPacket decode(FriendlyByteBuf buf){
        Type t = buf.readEnum(Type.class);
        UUID mId = buf.readBoolean()?buf.readUUID():null;
        BlockPos pos = buf.readBoolean()?buf.readBlockPos():null;
        String buy1Id = buf.readUtf(); int buy1Count = buf.readInt();
        String buy2Id = buf.readUtf(); int buy2Count = buf.readInt();
        String sellId = buf.readUtf(); int sellCount = buf.readInt();
        int maxUses = buf.readInt(); int xp = buf.readInt(); float priceMult = buf.readFloat();
        String presetName = buf.readUtf(); String bundleAlias = buf.readUtf(); String bundleItems = buf.readUtf(); Integer version = buf.readBoolean()?buf.readInt():null;
        Integer delIdx = buf.readBoolean()?buf.readInt():null;
        if (buy1Id.isBlank()) buy1Id = null; if (buy2Id.isBlank()) buy2Id = null; if (sellId.isBlank()) sellId = null; if (presetName.isBlank()) presetName = null; if (bundleAlias.isBlank()) bundleAlias = null; if (bundleItems.isBlank()) bundleItems = null;
        NightMarketAdminActionPacket p = new NightMarketAdminActionPacket(t, mId, pos, buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult, presetName, version);
        try{ java.lang.reflect.Field fa = NightMarketAdminActionPacket.class.getDeclaredField("bundleAlias"); fa.setAccessible(true); fa.set(p, bundleAlias);}catch(Exception ignored){}
        try{ java.lang.reflect.Field fi = NightMarketAdminActionPacket.class.getDeclaredField("bundleItems"); fi.setAccessible(true); fi.set(p, bundleItems);}catch(Exception ignored){}
        try{ java.lang.reflect.Field fd = NightMarketAdminActionPacket.class.getDeclaredField("deleteIndex"); fd.setAccessible(true); fd.set(p, delIdx);}catch(Exception ignored){}
        return p;
    }

    public static void handle(NightMarketAdminActionPacket msg, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender(); if (sp == null) return;
            switch (msg.type){
                case ADD_TRADE -> {
                    if ((msg.marketId == null && msg.pos == null) || msg.buy1Id == null || msg.sellId == null){
                        if (sp != null) sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Missing required fields"), true);
                        return;
                    }
                    // Validation
                    int buy1C = Math.max(1, msg.buy1Count);
                    int buy2C = Math.max(0, msg.buy2Count);
                    int sellC = Math.max(1, msg.sellCount);
                    int maxUses = Math.max(1, msg.maxUses);
                    int xp = Math.max(0, msg.xp);
                    float mult = msg.priceMult;
                    if (mult < 0f || mult > 1.0f){
                        if (sp != null) sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Price multiplier must be between 0 and 1"), true);
                        return;
                    }
                    java.util.UUID targetId = msg.marketId;
                    if (msg.pos != null){
                        var be = sp.level().getBlockEntity(msg.pos);
                        if (be instanceof NightMarketBlockEntity nbe){ targetId = nbe.getMarketId(); }
                    }
                    boolean ok = false;
                    net.minecraft.world.item.trading.MerchantOffer created = null;
                    if (msg.bundleItems != null && msg.bundleAlias != null){
                        java.util.List<org.lupz.doomsdayessentials.event.eclipse.market.ItemStackSpec> specs = new java.util.ArrayList<>();
                        for (String part : msg.bundleItems.split(",")){
                            String ptxt = part.trim(); if (ptxt.isEmpty()) continue;
                            String[] toks = ptxt.split("\\s+"); if (toks.length == 0) continue;
                            String idStr = toks[0]; int cnt = 1;
                            for (int k=1;k<toks.length;k++){
                                String tk = toks[k];
                                if (tk.startsWith("x")) { try{ cnt = Integer.parseInt(tk.substring(1)); }catch(Exception ignored){} }
                                else { try{ cnt = Integer.parseInt(tk); }catch(Exception ignored){} }
                            }
                            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(idStr);
                            if (rl != null) specs.add(org.lupz.doomsdayessentials.event.eclipse.market.ItemStackSpec.of(rl, cnt));
                        }
                        net.minecraft.world.item.ItemStack sellStack = org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketBundleItem.createBundle(msg.bundleAlias, specs);
                        created = NightMarketManager.addOfferStackTo(targetId, msg.buy1Id, buy1C, msg.buy2Id, buy2C, sellStack, maxUses, xp, mult);
                        ok = created != null;
                        if (ok && msg.bundleAlias != null) NightMarketManager.registerAlias(targetId, created, msg.bundleAlias);
                    } else {
                        ok = NightMarketManager.addOfferTo(targetId, msg.buy1Id, buy1C, msg.buy2Id, buy2C, msg.sellId, sellC, maxUses, xp, mult);
                    }
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(ok?"Trade added":"Trade already exists or invalid item IDs"), true);
                    if (ok){
                        if (targetId != null && org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.hasSession(targetId)){
                            org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.snapshot(sp.level(), targetId);
                        }
                        String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(sp.level(), org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(sp.level(), targetId));
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(targetId, null, json, null));
                        int size = org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffersMutable(targetId).size();
                        sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Offers now: " + size), true);
                    }
                }
                case DELETE_TRADE -> {
                    if ((msg.marketId == null && msg.pos == null) || msg.deleteIndex == null) return;
                    java.util.UUID targetId = msg.marketId;
                    if (msg.pos != null){
                        var be = sp.level().getBlockEntity(msg.pos);
                        if (be instanceof NightMarketBlockEntity nbe){ targetId = nbe.getMarketId(); }
                    }
                    boolean removed = NightMarketManager.removeOfferAt(targetId, Math.max(0, msg.deleteIndex));
                    if (removed){
                        if (org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.hasSession(targetId)){
                            org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.snapshot(sp.level(), targetId);
                        }
                        String json = MarketPresetManager.exportOffersToJson(sp.level(), NightMarketManager.getOffers(sp.level(), targetId));
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(targetId, null, json, null));
                        sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Trade deleted"), true);
                    } else {
                        sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Invalid trade index"), true);
                    }
                }
                case SAVE_PRESET -> {
                    if (msg.marketId == null || msg.presetName == null) return;
                    try { MarketPresetManager.savePreset(sp.level(), msg.marketId, msg.presetName); } catch (Exception ignored) {}
                }
                case LOAD_PRESET -> {
                    if (msg.marketId == null || msg.presetName == null) return;
                    boolean ok = MarketPresetManager.loadPreset(sp.level(), msg.marketId, msg.presetName, Optional.ofNullable(msg.version));
                    if (ok){
                        if (org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.hasSession(msg.marketId)){
                            org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.snapshot(sp.level(), msg.marketId);
                        }
                        String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(sp.level(), org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(sp.level(), msg.marketId));
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(msg.marketId, null, json, null));
                    }
                }
                case ACTIVATE_PRESET -> {
                    if (msg.pos == null || msg.presetName == null) return;
                    var be = sp.level().getBlockEntity(msg.pos);
                    if (be instanceof NightMarketBlockEntity nbe){
                        // Bind active preset name to the block entity
                        nbe.setActivePreset(msg.presetName);
                        // Also load the preset's offers directly into this market so right-click shows them
                        UUID mId = nbe.getMarketId();
                        boolean loaded = MarketPresetManager.loadPreset(sp.level(), mId, msg.presetName, java.util.Optional.empty());
                        if (loaded){
                            String json = MarketPresetManager.exportOffersToJson(sp.level(), NightMarketManager.getOffers(sp.level(), mId));
                            org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(mId, msg.pos, json, msg.presetName));
                        } else {
                            // Still send a refresh to update just the active preset label, even if load failed
                            org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(null, msg.pos, "{}", msg.presetName));
                        }
                    }
                }
                case CLEAR_MARKET -> {
                    if (msg.marketId == null) return;
                    NightMarketManager.clearMarket(msg.marketId);
                    if (org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.hasSession(msg.marketId)){
                        org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.snapshot(sp.level(), msg.marketId);
                    }
                    String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(sp.level(), org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(sp.level(), msg.marketId));
                    org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(msg.marketId, null, json, null));
                }
                case START_SESSION -> {
                    if (msg.marketId == null) return;
                    boolean started = org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.start(sp.level(), msg.marketId);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(started?"Edit session started":"Failed to start session"), true);
                }
                case UNDO -> {
                    if (msg.marketId == null) return;
                    boolean ok = org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.undo(sp.level(), msg.marketId);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(ok?"Undone":"Nothing to undo"), true);
                    if (ok){
                        String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(sp.level(), org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(sp.level(), msg.marketId));
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(msg.marketId, null, json, null));
                    }
                }
                case REDO -> {
                    if (msg.marketId == null) return;
                    boolean ok = org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.redo(sp.level(), msg.marketId);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(ok?"Redone":"Nothing to redo"), true);
                    if (ok){
                        String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(sp.level(), org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(sp.level(), msg.marketId));
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(msg.marketId, null, json, null));
                    }
                }
                case COMMIT -> {
                    if (msg.marketId == null) return;
                    boolean ok = org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.commit(msg.marketId);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(ok?"Edit session committed":"No active session"), true);
                }
                case CANCEL -> {
                    if (msg.marketId == null) return;
                    boolean ok = org.lupz.doomsdayessentials.event.eclipse.market.MarketEditSession.cancel(sp.level(), msg.marketId);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(ok?"Edit session canceled":"No active session"), true);
                    if (ok){
                        String json = org.lupz.doomsdayessentials.event.eclipse.market.MarketPresetManager.exportOffersToJson(sp.level(), org.lupz.doomsdayessentials.event.eclipse.market.NightMarketManager.getOffers(sp.level(), msg.marketId));
                        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NightMarketAdminRefreshPacket(msg.marketId, null, json, null));
                    }
                }
                case OPEN_EDITOR -> {
                    if (msg.marketId == null || msg.pos == null) return;
                    net.minecraftforge.network.NetworkHooks.openScreen(sp, new net.minecraft.world.MenuProvider() {
                        @Override public net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.literal("Editar Troca"); }
                        @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                            return new org.lupz.doomsdayessentials.event.eclipse.market.NightMarketMenu(id, inv);
                        }
                    }, buf -> { buf.writeUUID(msg.marketId); buf.writeBlockPos(msg.pos); });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
