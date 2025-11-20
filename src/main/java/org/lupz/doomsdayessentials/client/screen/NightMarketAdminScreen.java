package org.lupz.doomsdayessentials.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lupz.doomsdayessentials.network.PacketHandler;
import org.lupz.doomsdayessentials.event.eclipse.market.admin.NightMarketAdminActionPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple administrative UI for Night Market management.
 * Provides form inputs to add trades, save/load/activate presets, and a preview list of current offers.
 */
public class NightMarketAdminScreen extends Screen {
    private final java.util.UUID marketId;
    private final net.minecraft.core.BlockPos blockPos;
    private String activePreset;
    private final List<OfferEntry> offers = new ArrayList<>();
    private final java.util.List<Button> offerDeleteButtons = new java.util.ArrayList<>();

    // Form widgets
    private EditBox buy1IdBox, buy1CountBox, buy2IdBox, buy2CountBox, sellIdBox, sellCountBox, maxUsesBox, xpBox, priceMultBox;
    private EditBox bundleAliasBox, bundleItemsBox;
    private final java.util.List<BundleRow> bundleRows = new java.util.ArrayList<>();
    private Button addBundleItemBtn;
    private boolean showSecondBuy = false, sellBundle = false, showParams = false;
    private Button toggleSecondBtn, toggleBundleBtn, toggleParamsBtn, createBtn, slotEditorBtn;
    private EditBox presetNameBox, presetVersionBox;
    private @Nullable OfferEntry selectedOffer;

    // Layout fields for consistent rendering
    private int left, top, rowH, colLabel, colInput, colInputRight;
    // Simple autocompletion: available item IDs and transient suggestion state
    private final List<String> allItemIds = new ArrayList<>();
    private @Nullable EditBox suggestionsFor;
    private List<String> currentSuggestions = new ArrayList<>();
    // Suggestion overlay metrics
    private int suggX, suggY, suggW, suggH;
    // Track original placeholder strings because EditBox in this MC version doesn't expose them
    private final java.util.Map<EditBox, String> placeholders = new java.util.HashMap<>();

    public NightMarketAdminScreen(java.util.UUID marketId, net.minecraft.core.BlockPos pos, String offersJson, String activePreset) {
        super(Component.literal("Admin - Mercado Negro"));
        this.marketId = marketId;
        this.blockPos = pos;
        this.activePreset = activePreset == null ? "" : activePreset;
        parseOffersJson(offersJson);
    }

    private void parseOffersJson(String json){
        try{
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("trades");
            if (arr != null){
                for (int i=0;i<arr.size();i++){
                    JsonObject t = arr.get(i).getAsJsonObject();
                    String alias = t.has("alias") ? t.get("alias").getAsString() : ("offer-"+(i+1));
                    JsonArray sells = t.getAsJsonArray("sells");
                    String sellId = "";
                    int sellCount = 1;
                    if (sells != null && sells.size()>0){
                        JsonObject s = sells.get(0).getAsJsonObject();
                        sellId = s.has("id") ? s.get("id").getAsString() : "";
                        sellCount = s.has("count") ? s.get("count").getAsInt() : 1;
                    }
                    offers.add(new OfferEntry(alias, sellId, sellCount));
                }
            }
        }catch(Exception ignored){}
    }

    @Override
    protected void init(){
        left = this.width/2 - 220;
        top = this.height/2 - 120;

        rowH = 20;
        colLabel = left;
        colInput = left + 120;
        colInputRight = left + 360;

        // Build item ID list for autocompletion (client-side registry)
        try{
            for (var key : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys()){
                allItemIds.add(key.toString());
            }
            allItemIds.sort(String::compareTo);
        }catch(Exception ignored){}

        // Preset section
        presetNameBox = new EditBox(this.font, colInput, top, 180, 18, Component.literal("Preset"));
        presetNameBox.setMaxLength(64);
        presetNameBox.setValue(activePreset);
        presetNameBox.setSuggestion("Preset name (e.g. default)");
        placeholders.put(presetNameBox, "Preset name (e.g. default)");
        this.addRenderableWidget(presetNameBox);

        presetVersionBox = new EditBox(this.font, colInput + 184, top, 44, 18, Component.literal("v"));
        presetVersionBox.setMaxLength(4);
        presetVersionBox.setSuggestion("v#");
        placeholders.put(presetVersionBox, "v#");
        this.addRenderableWidget(presetVersionBox);

        this.addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> {
            String name = presetNameBox.getValue();
            if (!name.isBlank()) PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.savePreset(marketId, name));
        }).pos(colLabel, top).size(80, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Carregar"), b -> {
            String name = presetNameBox.getValue();
            Integer v = null;
            try { v = presetVersionBox.getValue().isBlank()?null:Integer.parseInt(presetVersionBox.getValue()); } catch (NumberFormatException ignored) {}
            if (!name.isBlank()) PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.loadPreset(marketId, name, v));
        }).pos(colLabel, top + rowH).size(80, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Ativar"), b -> {
            String name = presetNameBox.getValue();
            if (!name.isBlank()) PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.activatePreset(blockPos, name));
        }).pos(colLabel, top + 2*rowH).size(80, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Limpar"), b -> {
            PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.clearMarket(marketId));
        }).pos(colLabel, top + 3*rowH).size(80, 18).build());

        // Add trade section (refactored layout with BUY on left and SELL on right)
        int formTop = top + 4*rowH;
        // Buy 1
        buy1IdBox = new EditBox(this.font, colInput, formTop, 190, 18, Component.literal("Buy 1"));
        buy1IdBox.setSuggestion("Item to buy (e.g. minecraft:emerald)");
        placeholders.put(buy1IdBox, "Item to buy (e.g. minecraft:emerald)");
        buy1CountBox = new EditBox(this.font, colInput + 194, formTop, 44, 18, Component.literal("x"));
        buy1CountBox.setSuggestion("count");
        placeholders.put(buy1CountBox, "count");
        // Buy 2 (optional)
        buy2IdBox = new EditBox(this.font, colInput, formTop + rowH, 190, 18, Component.literal("Buy 2 (optional)"));
        buy2IdBox.setSuggestion("Optional item (e.g. minecraft:diamond)");
        placeholders.put(buy2IdBox, "Optional item (e.g. minecraft:diamond)");
        buy2CountBox = new EditBox(this.font, colInput + 194, formTop + rowH, 44, 18, Component.literal("x"));
        buy2CountBox.setSuggestion("count");
        placeholders.put(buy2CountBox, "count");
        // Sell (on right)
        sellIdBox = new EditBox(this.font, colInputRight, formTop, 190, 18, Component.literal("Sell"));
        sellIdBox.setSuggestion("Item to sell (e.g. minecraft:arrow)");
        placeholders.put(sellIdBox, "Item to sell (e.g. minecraft:arrow)");
        sellCountBox = new EditBox(this.font, colInputRight + 194, formTop, 44, 18, Component.literal("x"));
        sellCountBox.setSuggestion("count");
        placeholders.put(sellCountBox, "count");
        // Bundle (optional alternative to Sell item): alias + items spec
        bundleAliasBox = new EditBox(this.font, colInputRight, formTop + rowH, 190, 18, Component.literal("Bundle Alias"));
        bundleAliasBox.setSuggestion("e.g. Starter Pack");
        placeholders.put(bundleAliasBox, "e.g. Starter Pack");
        bundleItemsBox = new EditBox(this.font, colInputRight + 194, formTop + rowH, 220, 18, Component.literal("Items"));
        bundleItemsBox.setSuggestion("ns:id x count, ns:id x count");
        placeholders.put(bundleItemsBox, "ns:id x count, ns:id x count");
        // Trade parameters
        maxUsesBox = new EditBox(this.font, colInputRight, formTop + 2*rowH, 68, 18, Component.literal("Max Uses"));
        maxUsesBox.setSuggestion("e.g. 64");
        placeholders.put(maxUsesBox, "e.g. 64");
        xpBox = new EditBox(this.font, colInputRight + 72, formTop + 2*rowH, 68, 18, Component.literal("XP"));
        xpBox.setSuggestion("e.g. 1");
        placeholders.put(xpBox, "e.g. 1");
        priceMultBox = new EditBox(this.font, colInputRight + 144, formTop + 2*rowH, 68, 18, Component.literal("Price Mult"));
        priceMultBox.setSuggestion("e.g. 0.05");
        placeholders.put(priceMultBox, "e.g. 0.05");

        for (EditBox eb : new EditBox[]{buy1IdBox,buy1CountBox,buy2IdBox,buy2CountBox,sellIdBox,sellCountBox,bundleAliasBox,bundleItemsBox,maxUsesBox,xpBox,priceMultBox}){
            this.addRenderableWidget(eb);
        }

        // Attach responders to compute suggestions while typing (and auto-hide placeholders)
        attachItemResponder(buy1IdBox);
        attachItemResponder(buy2IdBox);
        attachItemResponder(sellIdBox);
        attachPlaceholderAutoHide(buy1CountBox);
        attachPlaceholderAutoHide(buy2CountBox);
        attachPlaceholderAutoHide(sellCountBox);
        attachPlaceholderAutoHide(bundleAliasBox);
        attachPlaceholderAutoHide(bundleItemsBox);
        attachPlaceholderAutoHide(maxUsesBox);
        attachPlaceholderAutoHide(xpBox);
        attachPlaceholderAutoHide(priceMultBox);
        attachPlaceholderAutoHide(presetNameBox);
        attachPlaceholderAutoHide(presetVersionBox);

        this.addRenderableWidget(Button.builder(Component.literal("Add Trade"), b -> {
            try{
                String buy1Id = buy1IdBox.getValue(); int buy1Count = parseIntOr(buy1CountBox.getValue(), 1);
                String buy2Id = buy2IdBox.getValue(); int buy2Count = parseIntOr(buy2CountBox.getValue(), 0);
                String sellId = sellIdBox.getValue(); int sellCount = parseIntOr(sellCountBox.getValue(), 1);
                int maxUses = parseIntOr(maxUsesBox.getValue(), 64);
                int xp = parseIntOr(xpBox.getValue(), 1);
                float priceMult = parseFloatOr(priceMultBox.getValue(), 0.05f);
                if (validateTradeInputs(buy1Id, buy1Count, sellId, sellCount, priceMult)){
                    PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.addTrade(marketId, buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult));
                }
            }catch(Exception ignored){}
        }).pos(colLabel, formTop).size(90, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Add Bundle Trade"), b -> {
            try{
                String buy1Id = buy1IdBox.getValue(); int buy1Count = parseIntOr(buy1CountBox.getValue(), 1);
                String buy2Id = buy2IdBox.getValue(); int buy2Count = parseIntOr(buy2CountBox.getValue(), 0);
                String alias = bundleAliasBox.getValue(); String itemsSpec = bundleItemsBox.getValue();
                int maxUses = parseIntOr(maxUsesBox.getValue(), 64);
                int xp = parseIntOr(xpBox.getValue(), 1);
                float priceMult = parseFloatOr(priceMultBox.getValue(), 0.05f);
                if (validateTradeInputs(buy1Id, buy1Count, "minecraft:stone", 1, priceMult) && !alias.isBlank() && !itemsSpec.isBlank()){
                    PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.addBundleTrade(marketId, buy1Id, buy1Count, buy2Id, buy2Count, alias, itemsSpec, maxUses, xp, priceMult));
                }
            }catch(Exception ignored){}
        }).pos(colLabel + 96, formTop).size(130, 18).build());

        // Quick templates to speed up editing
        int tmplY = formTop + 3*rowH + 4;
        this.addRenderableWidget(Button.builder(Component.literal("Template: 1 emerald -> 16 bread"), b -> {
            buy1IdBox.setValue("minecraft:emerald"); buy1CountBox.setValue("1");
            sellIdBox.setValue("minecraft:bread"); sellCountBox.setValue("16");
            maxUsesBox.setValue("64"); xpBox.setValue("1"); priceMultBox.setValue("0.05");
        }).pos(colLabel, tmplY).size(240, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Template: 32 emerald -> 1 diamond"), b -> {
            buy1IdBox.setValue("minecraft:emerald"); buy1CountBox.setValue("32");
            sellIdBox.setValue("minecraft:diamond"); sellCountBox.setValue("1");
            maxUsesBox.setValue("64"); xpBox.setValue("1"); priceMultBox.setValue("0.1");
        }).pos(colLabel + 244, tmplY).size(240, 20).build());

        // Edit session controls
        int ctrlY = top + 2*rowH;
        this.addRenderableWidget(Button.builder(Component.literal("Start Edit"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.startSession(marketId)))
                .pos(colLabel, ctrlY).size(88, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Undo"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.undo(marketId)))
                .pos(colLabel + 92, ctrlY).size(64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Redo"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.redo(marketId)))
                .pos(colLabel + 158, ctrlY).size(64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Apply"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.commit(marketId)))
                .pos(colLabel + 224, ctrlY).size(72, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.cancel(marketId)))
                .pos(colLabel + 300, ctrlY).size(72, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Editar com Slots"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.openEditor(marketId, blockPos)))
                .pos(colLabel + 376, ctrlY).size(128, 20).build());

        this.clearWidgets();
        buildSimplified();
    }

    private void buildSimplified(){
        left = this.width/2 - 160;
        top = this.height/2 - 110;
        rowH = 22;
        colInput = left;

        try{
            for (var key : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys()){
                allItemIds.add(key.toString());
            }
            allItemIds.sort(String::compareTo);
        }catch(Exception ignored){}

        int formTop = top + rowH;
        buy1IdBox = new EditBox(this.font, colInput, formTop, 220, 20, Component.literal("Buy 1"));
        buy1IdBox.setSuggestion("namespace:item");
        placeholders.put(buy1IdBox, "namespace:item");
        buy1CountBox = new EditBox(this.font, colInput + 226, formTop, 54, 20, Component.literal("x"));
        buy1CountBox.setSuggestion("count");
        placeholders.put(buy1CountBox, "count");

        buy2IdBox = new EditBox(this.font, colInput, formTop + rowH, 220, 20, Component.literal("Buy 2"));
        buy2IdBox.setSuggestion("optional");
        placeholders.put(buy2IdBox, "optional");
        buy2CountBox = new EditBox(this.font, colInput + 226, formTop + rowH, 54, 20, Component.literal("x"));
        buy2CountBox.setSuggestion("count");
        placeholders.put(buy2CountBox, "count");

        sellIdBox = new EditBox(this.font, colInput, formTop + 2*rowH, 220, 20, Component.literal("Sell"));
        sellIdBox.setSuggestion("namespace:item");
        placeholders.put(sellIdBox, "namespace:item");
        sellCountBox = new EditBox(this.font, colInput + 226, formTop + 2*rowH, 54, 20, Component.literal("x"));
        sellCountBox.setSuggestion("count");
        placeholders.put(sellCountBox, "count");

        bundleAliasBox = new EditBox(this.font, colInput, formTop + 2*rowH, 140, 20, Component.literal("Alias"));
        bundleAliasBox.setSuggestion("pack name");
        placeholders.put(bundleAliasBox, "pack name");
        bundleItemsBox = new EditBox(this.font, colInput + 146, formTop + 2*rowH, 134, 20, Component.literal("Items"));
        bundleItemsBox.setVisible(false);

        maxUsesBox = new EditBox(this.font, colInput, formTop + 3*rowH, 80, 20, Component.literal("Uses"));
        maxUsesBox.setValue("64");
        xpBox = new EditBox(this.font, colInput + 84, formTop + 3*rowH, 80, 20, Component.literal("XP"));
        xpBox.setValue("1");
        priceMultBox = new EditBox(this.font, colInput + 168, formTop + 3*rowH, 112, 20, Component.literal("Mult"));
        priceMultBox.setValue("0.05");

        for (EditBox eb : new EditBox[]{buy1IdBox,buy1CountBox,buy2IdBox,buy2CountBox,sellIdBox,sellCountBox,bundleAliasBox,bundleItemsBox,maxUsesBox,xpBox,priceMultBox}){
            this.addRenderableWidget(eb);
        }

        attachItemResponder(buy1IdBox);
        attachItemResponder(buy2IdBox);
        attachItemResponder(sellIdBox);
        for (EditBox eb : new EditBox[]{buy1CountBox,buy2CountBox,sellCountBox,bundleAliasBox,bundleItemsBox,maxUsesBox,xpBox,priceMultBox}) attachPlaceholderAutoHide(eb);

        toggleSecondBtn = this.addRenderableWidget(Button.builder(Component.literal("Second Item: Off"), b -> {
            showSecondBuy = !showSecondBuy; b.setMessage(Component.literal(showSecondBuy?"Second Item: On":"Second Item: Off"));
            buy2IdBox.setVisible(showSecondBuy); buy2CountBox.setVisible(showSecondBuy);
        }).pos(colInput, top).size(140, 20).build());

        toggleBundleBtn = this.addRenderableWidget(Button.builder(Component.literal("Sell: Item"), b -> {
            sellBundle = !sellBundle; b.setMessage(Component.literal(sellBundle?"Sell: Bundle":"Sell: Item"));
            sellIdBox.setVisible(!sellBundle); sellCountBox.setVisible(!sellBundle);
            bundleAliasBox.setVisible(sellBundle);
            for (BundleRow r : bundleRows){ r.setVisible(sellBundle); }
            addBundleItemBtn.visible = sellBundle;
        }).pos(colInput + 146, top).size(134, 20).build());

        toggleParamsBtn = this.addRenderableWidget(Button.builder(Component.literal("Params: Default"), b -> {
            showParams = !showParams; b.setMessage(Component.literal(showParams?"Params: Custom":"Params: Default"));
            maxUsesBox.setVisible(showParams); xpBox.setVisible(showParams); priceMultBox.setVisible(showParams);
        }).pos(colInput, formTop + 3*rowH + 24).size(180, 20).build());

        createBtn = this.addRenderableWidget(Button.builder(Component.literal("Create Trade"), b -> {
            try{
                String buy1Id = buy1IdBox.getValue(); int buy1Count = parseIntOr(buy1CountBox.getValue(), 1);
                String buy2Id = showSecondBuy ? buy2IdBox.getValue() : null; int buy2Count = showSecondBuy ? parseIntOr(buy2CountBox.getValue(), 0) : 0;
                int maxUses = showParams ? parseIntOr(maxUsesBox.getValue(), 64) : 64;
                int xp = showParams ? parseIntOr(xpBox.getValue(), 1) : 1;
                float priceMult = showParams ? parseFloatOr(priceMultBox.getValue(), 0.05f) : 0.05f;
                if (!sellBundle){
                    String sellId = sellIdBox.getValue(); int sellCount = parseIntOr(sellCountBox.getValue(), 1);
                    if (validateTradeInputs(buy1Id, buy1Count, sellId, sellCount, priceMult)){
                        PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.addTrade(marketId, buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult));
                    }
                } else {
                    String alias = bundleAliasBox.getValue();
                    StringBuilder sb = new StringBuilder();
                    for (int i=0;i<bundleRows.size();i++){
                        BundleRow r = bundleRows.get(i);
                        String id = r.idBox.getValue(); int cnt = parseIntOr(r.countBox.getValue(), 1);
                        if (id != null && !id.isBlank()){
                            if (sb.length()>0) sb.append(", ");
                            sb.append(id).append(" x ").append(cnt);
                        }
                    }
                    String itemsSpec = sb.toString();
                    if (validateTradeInputs(buy1Id, buy1Count, "minecraft:stone", 1, priceMult) && !alias.isBlank() && !itemsSpec.isBlank()){
                        PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.addBundleTrade(marketId, buy1Id, buy1Count, buy2Id, buy2Count, alias, itemsSpec, maxUses, xp, priceMult));
                    }
                }
            }catch(Exception ignored){}
        }).pos(colInput, formTop + 4*rowH + 24).size(180, 22).build());

        slotEditorBtn = this.addRenderableWidget(Button.builder(Component.literal("Editar com Slots"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.openEditor(marketId, blockPos)))
                .pos(colInput + 186, formTop + 4*rowH + 24).size(180, 22).build());

        buy2IdBox.setVisible(false); buy2CountBox.setVisible(false);
        bundleAliasBox.setVisible(false); bundleItemsBox.setVisible(false);
        maxUsesBox.setVisible(false); xpBox.setVisible(false); priceMultBox.setVisible(false);
        addBundleItemBtn = this.addRenderableWidget(Button.builder(Component.literal("+ Item"), b -> {
            addBundleRow();
        }).pos(colInput + 146, formTop + 3*rowH).size(134, 20).build());
        addBundleItemBtn.visible = false;
        buildOfferListButtons();
    }

    private void buildOfferListButtons(){
        for (Button b : offerDeleteButtons) this.removeWidget(b);
        offerDeleteButtons.clear();
        int listLeft = this.width - 220;
        int listTop = top;
        int visible = Math.min(12, offers.size());
        for (int i=0;i<visible;i++){
            int y = listTop + i*20;
            final int idx = i;
            Button del = Button.builder(Component.literal("🗑"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.deleteTrade(marketId, idx)))
                    .pos(listLeft + 180, y).size(30, 20).build();
            this.addRenderableWidget(del);
            offerDeleteButtons.add(del);
        }
    }

    private void addBundleRow(){
        int idx = bundleRows.size();
        BundleRow r = new BundleRow();
        int formTop = top + rowH;
        r.idBox = new EditBox(this.font, colInput, formTop + 3*rowH + idx*rowH, 220, 20, Component.literal("Item"));
        r.idBox.setSuggestion("namespace:item");
        r.countBox = new EditBox(this.font, colInput + 226, formTop + 3*rowH + idx*rowH, 54, 20, Component.literal("x"));
        r.countBox.setSuggestion("count");
        r.removeBtn = Button.builder(Component.literal("-"), b -> {
            this.removeWidget(r.idBox); this.removeWidget(r.countBox); this.removeWidget(r.removeBtn);
            bundleRows.remove(r); relayoutBundleRows();
        }).pos(colInput + 284, formTop + 3*rowH + idx*rowH).size(24, 20).build();
        this.addRenderableWidget(r.idBox); this.addRenderableWidget(r.countBox); this.addRenderableWidget(r.removeBtn);
        attachItemResponder(r.idBox); attachPlaceholderAutoHide(r.countBox);
        bundleRows.add(r);
        relayoutBundleRows();
        if (sellBundle){ r.setVisible(true);} else { r.setVisible(false); }
    }

    private void relayoutBundleRows(){
        int formTop = top + rowH;
        for (int i=0;i<bundleRows.size();i++){
            BundleRow r = bundleRows.get(i);
            int y = formTop + 3*rowH + i*rowH;
            r.idBox.setX(colInput); r.idBox.setY(y);
            r.countBox.setX(colInput + 226); r.countBox.setY(y);
            r.removeBtn.setPosition(colInput + 284, y);
        }
        addBundleItemBtn.setPosition(colInput + 146, formTop + 3*rowH + bundleRows.size()*rowH);
    }

    

    private static class BundleRow {
        EditBox idBox; EditBox countBox; Button removeBtn;
        void setVisible(boolean v){ idBox.setVisible(v); countBox.setVisible(v); removeBtn.visible = v; }
    }

    private int parseIntOr(String s, int def){ try{ return Integer.parseInt(s);}catch(Exception e){ return def; } }
    private float parseFloatOr(String s, float def){ try{ return Float.parseFloat(s);}catch(Exception e){ return def; } }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt){
        this.renderBackground(g);
        super.render(g, mx, my, pt);

        g.drawString(this.font, Component.literal("§6Mercado Negro - Admin"), left, top - 24, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal((showSecondBuy?"2º item: On":"2º item: Off") + "  |  " + (sellBundle?"Saída: Bundle":"Saída: Item")), left, top - 6, 0xAAAAAA, false);

        int listLeft = this.width - 220;
        int listTop = top;
        for (int i=0;i<Math.min(12, offers.size());i++){
            OfferEntry e = offers.get(i);
            int y = listTop + i*20;
            ItemStack icon = itemFromId(e.sellId, e.sellCount);
            if (!icon.isEmpty()) g.renderItem(icon, listLeft, y-2);
            g.drawString(this.font, Component.literal(e.alias), listLeft + 22, y, 0xFFFFFF, false);
        }

        // Render clickable suggestion overlay with adaptive anchoring
        if (this.getFocused() instanceof EditBox eb && isItemBox(eb)){
            List<String> sugg = findItemMatches(eb.getValue(), 5);
            currentSuggestions = sugg;
            if (!sugg.isEmpty()){
                int boxX = eb.getX();
                int boxY = eb.getY();
                int boxW = eb.getWidth();
                int boxH = eb.getHeight();
                int w = 180;
                int h = 18 * sugg.size() + 6;

                java.util.function.BiPredicate<Integer,Integer> overlapsAny = (ox, oy) -> {
                    int ow = w, oh = h;
                    for (var child : this.children()){
                        if (child instanceof net.minecraft.client.gui.components.AbstractWidget aw){
                            if (!aw.visible) continue;
                            int cx = aw.getX(), cy = aw.getY(), cw = aw.getWidth(), ch = aw.getHeight();
                            boolean inter = ox < cx + cw && ox + ow > cx && oy < cy + ch && oy + oh > cy;
                            if (inter) return true;
                        }
                    }
                    return false;
                };

                int sx = boxX + boxW + 6; int sy = boxY - 2; // prefer right
                if (sx + w + 6 > this.width || overlapsAny.test(sx, sy)){
                    sx = boxX - w - 6; sy = boxY - 2; // try left
                }
                if (sx < 6 || overlapsAny.test(sx, sy)){
                    sx = boxX; sy = boxY + boxH + 2; // try below
                }
                if (sy + h + 6 > this.height || overlapsAny.test(sx, sy)){
                    sx = boxX; sy = boxY - h - 2; // try above
                }
                if (sx < 6 || sy < 6 || sx + w + 6 > this.width || sy + h + 6 > this.height || overlapsAny.test(sx, sy)){
                    sx = Math.max(6, this.width - w - 6);
                    sy = Math.max(6, Math.min(boxY, this.height - h - 6));
                }
                suggX = sx; suggY = sy; suggW = w; suggH = h;
                g.pose().pushPose();
                g.pose().translate(0, 0, 400);
                g.fill(sx, sy, sx + w, sy + h, 0xCC101010);
                int y = sy + 2;
                for (int j=0;j<sugg.size();j++){
                    String id = sugg.get(j);
                    // Icon
                    ItemStack icon = itemFromId(id, 1);
                    if (!icon.isEmpty()) g.renderItem(icon, sx + 2, y - 2);
                    // Text
                    g.drawString(this.font, Component.literal(id), sx + 22, y, 0xE0FFFF, false);
                    y += 18;
                }
                g.pose().popPose();
            } else {
                suggH = 0;
            }
        } else {
            suggH = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button){
        // Click suggestion overlay
        if (button == 0 && suggH > 0 && this.getFocused() instanceof EditBox eb && isItemBox(eb)){
            if (mx >= suggX && mx <= suggX + suggW && my >= suggY && my <= suggY + suggH){
                int idx = (int)((my - suggY - 2) / 18);
                if (idx >= 0 && idx < currentSuggestions.size()){
                    eb.setValue(currentSuggestions.get(idx));
                    eb.setCursorPosition(eb.getValue().length());
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mx, my, button);
    }

    private ItemStack itemFromId(String id, int count){
        try{
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) return ItemStack.EMPTY;
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) return ItemStack.EMPTY;
            ItemStack s = new ItemStack(item, Math.max(1, count));
            return s;
        }catch(Exception e){ return ItemStack.EMPTY; }
    }

    // --- Autocompletion helpers ---
    private void attachItemResponder(EditBox box){
        final String placeholder = placeholders.get(box);
        box.setResponder(s -> {
            // Update suggestions for item id fields
            suggestionsFor = box;
            currentSuggestions = findItemMatches(s, 5);
            // Auto-hide placeholder while typing
            if (placeholder != null) box.setSuggestion((s == null || s.isBlank()) ? placeholder : null);
        });
    }

    // For non-item fields: only auto-hide placeholder as the user types
    private void attachPlaceholderAutoHide(EditBox box){
        final String placeholder = placeholders.get(box);
        box.setResponder(s -> {
            if (placeholder != null) box.setSuggestion((s == null || s.isBlank()) ? placeholder : null);
        });
    }

    private boolean isItemBox(EditBox eb){
        if (eb == buy1IdBox || eb == buy2IdBox || eb == sellIdBox) return true;
        for (BundleRow r : bundleRows){ if (eb == r.idBox) return true; }
        return false;
    }

    private List<String> findItemMatches(String input, int limit){
        List<String> out = new ArrayList<>();
        if (input == null) return out;
        String prefix = input.trim().toLowerCase();
        if (prefix.isEmpty()) return out;
        // If no namespace, assume minecraft:
        String nsPrefix = prefix.contains(":") ? prefix : ("minecraft:" + prefix);
        // Prefer popular items first
        List<String> popular = List.of("minecraft:emerald","minecraft:diamond","minecraft:bread","minecraft:arrow","minecraft:torch","minecraft:stone","minecraft:iron_ingot");
        for (String id : popular){
            String low = id.toLowerCase();
            if ((low.startsWith(prefix) || low.startsWith(nsPrefix)) && allItemIds.contains(id)){
                out.add(id);
                if (out.size() >= limit) return out;
            }
        }
        for (String id : allItemIds){
            String low = id.toLowerCase();
            if (low.startsWith(prefix) || low.startsWith(nsPrefix)){
                if (!out.contains(id)) out.add(id);
                if (out.size() >= limit) break;
            }
        }
        return out;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        // Tab to autocomplete item IDs
        if (keyCode == 258){ // GLFW.GLFW_KEY_TAB
            if (this.getFocused() instanceof EditBox eb && isItemBox(eb)){
                List<String> sugg = findItemMatches(eb.getValue(), 1);
                if (!sugg.isEmpty()){
                    eb.setValue(sugg.get(0));
                    eb.setCursorPosition(eb.getValue().length());
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    

    // --- Data refresh helpers ---
    public void updateData(String offersJson, String activePreset){
        try{
            this.activePreset = activePreset == null ? this.activePreset : activePreset;
            this.offers.clear();
            parseOffersJson(offersJson);
            buildOfferListButtons();
        }catch(Exception ignored){}
    }

    // --- Client-side validation ---
    private boolean validateTradeInputs(String buy1Id, int buy1Count, String sellId, int sellCount, float priceMult){
        if (buy1Id == null || buy1Id.isBlank()){ showToast("Buy 1 item ID required"); return false; }
        if (sellId == null || sellId.isBlank()){ showToast("Sell item ID required"); return false; }
        if (buy1Count <= 0){ showToast("Buy 1 count must be > 0"); return false; }
        if (sellCount <= 0){ showToast("Sell count must be > 0"); return false; }
        if (priceMult < 0f || priceMult > 1f){ showToast("Price multiplier must be between 0 and 1"); return false; }
        return true;
    }

    private void showToast(String msg){
        if (this.minecraft != null && this.minecraft.player != null){
            this.minecraft.player.displayClientMessage(Component.literal(msg), true);
        }
    }

    private static class OfferEntry {
        final String alias; final String sellId; final int sellCount;
        OfferEntry(String alias, String sellId, int sellCount){ this.alias = alias; this.sellId = sellId; this.sellCount = sellCount; }
    }

    public static void open(java.util.UUID marketId, net.minecraft.core.BlockPos pos, String offersJson, String activePreset){
        Minecraft.getInstance().setScreen(new NightMarketAdminScreen(marketId, pos, offersJson, activePreset));
    }
}
