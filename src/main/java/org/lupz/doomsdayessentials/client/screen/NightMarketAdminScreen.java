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

    // Form widgets
    private EditBox buy1IdBox, buy1CountBox, buy2IdBox, buy2CountBox, sellIdBox, sellCountBox, maxUsesBox, xpBox, priceMultBox;
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
        // Layout constants
        left = this.width/2 - 200;
        top = this.height/2 - 110;

        rowH = 18;
        colLabel = left;
        colInput = left + 100;
        // Right-side input column for SELL
        colInputRight = colInput + 200;

        // Build item ID list for autocompletion (client-side registry)
        try{
            for (var key : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys()){
                allItemIds.add(key.toString());
            }
            allItemIds.sort(String::compareTo);
        }catch(Exception ignored){}

        // Preset section
        presetNameBox = new EditBox(this.font, colInput, top, 160, 16, Component.literal("Preset"));
        presetNameBox.setMaxLength(64);
        presetNameBox.setValue(activePreset);
        presetNameBox.setSuggestion("Preset name (e.g. default)");
        placeholders.put(presetNameBox, "Preset name (e.g. default)");
        this.addRenderableWidget(presetNameBox);

        presetVersionBox = new EditBox(this.font, colInput + 164, top, 40, 16, Component.literal("v"));
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
        int formTop = top + 5*rowH;
        // Buy 1
        buy1IdBox = new EditBox(this.font, colInput, formTop, 170, 16, Component.literal("Buy 1"));
        buy1IdBox.setSuggestion("Item to buy (e.g. minecraft:emerald)");
        placeholders.put(buy1IdBox, "Item to buy (e.g. minecraft:emerald)");
        buy1CountBox = new EditBox(this.font, colInput + 174, formTop, 40, 16, Component.literal("x"));
        buy1CountBox.setSuggestion("count");
        placeholders.put(buy1CountBox, "count");
        // Buy 2 (optional)
        buy2IdBox = new EditBox(this.font, colInput, formTop + rowH, 170, 16, Component.literal("Buy 2 (optional)"));
        buy2IdBox.setSuggestion("Optional item (e.g. minecraft:diamond)");
        placeholders.put(buy2IdBox, "Optional item (e.g. minecraft:diamond)");
        buy2CountBox = new EditBox(this.font, colInput + 174, formTop + rowH, 40, 16, Component.literal("x"));
        buy2CountBox.setSuggestion("count");
        placeholders.put(buy2CountBox, "count");
        // Sell (on right)
        sellIdBox = new EditBox(this.font, colInputRight, formTop, 170, 16, Component.literal("Sell"));
        sellIdBox.setSuggestion("Item to sell (e.g. minecraft:arrow)");
        placeholders.put(sellIdBox, "Item to sell (e.g. minecraft:arrow)");
        sellCountBox = new EditBox(this.font, colInputRight + 174, formTop, 40, 16, Component.literal("x"));
        sellCountBox.setSuggestion("count");
        placeholders.put(sellCountBox, "count");
        // Trade parameters
        maxUsesBox = new EditBox(this.font, colInputRight, formTop + 2*rowH, 60, 16, Component.literal("Max Uses"));
        maxUsesBox.setSuggestion("e.g. 64");
        placeholders.put(maxUsesBox, "e.g. 64");
        xpBox = new EditBox(this.font, colInputRight + 64, formTop + 2*rowH, 60, 16, Component.literal("XP"));
        xpBox.setSuggestion("e.g. 1");
        placeholders.put(xpBox, "e.g. 1");
        priceMultBox = new EditBox(this.font, colInputRight + 128, formTop + 2*rowH, 60, 16, Component.literal("Price Mult"));
        priceMultBox.setSuggestion("e.g. 0.05");
        placeholders.put(priceMultBox, "e.g. 0.05");

        for (EditBox eb : new EditBox[]{buy1IdBox,buy1CountBox,buy2IdBox,buy2CountBox,sellIdBox,sellCountBox,maxUsesBox,xpBox,priceMultBox}){
            this.addRenderableWidget(eb);
        }

        // Attach responders to compute suggestions while typing (and auto-hide placeholders)
        attachItemResponder(buy1IdBox);
        attachItemResponder(buy2IdBox);
        attachItemResponder(sellIdBox);
        attachPlaceholderAutoHide(buy1CountBox);
        attachPlaceholderAutoHide(buy2CountBox);
        attachPlaceholderAutoHide(sellCountBox);
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

        // Quick templates to speed up editing
        int tmplY = formTop + 3*rowH + 2;
        this.addRenderableWidget(Button.builder(Component.literal("Template: 1 emerald -> 16 bread"), b -> {
            buy1IdBox.setValue("minecraft:emerald"); buy1CountBox.setValue("1");
            sellIdBox.setValue("minecraft:bread"); sellCountBox.setValue("16");
            maxUsesBox.setValue("64"); xpBox.setValue("1"); priceMultBox.setValue("0.05");
        }).pos(colLabel, tmplY).size(220, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Template: 32 emerald -> 1 diamond"), b -> {
            buy1IdBox.setValue("minecraft:emerald"); buy1CountBox.setValue("32");
            sellIdBox.setValue("minecraft:diamond"); sellCountBox.setValue("1");
            maxUsesBox.setValue("64"); xpBox.setValue("1"); priceMultBox.setValue("0.1");
        }).pos(colLabel + 224, tmplY).size(220, 18).build());

        // Edit session controls
        int ctrlY = top + 2*rowH;
        this.addRenderableWidget(Button.builder(Component.literal("Start Edit"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.startSession(marketId)))
                .pos(colLabel, ctrlY).size(85, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Undo"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.undo(marketId)))
                .pos(colLabel + 90, ctrlY).size(60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Redo"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.redo(marketId)))
                .pos(colLabel + 152, ctrlY).size(60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Apply"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.commit(marketId)))
                .pos(colLabel + 214, ctrlY).size(60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> PacketHandler.CHANNEL.sendToServer(NightMarketAdminActionPacket.cancel(marketId)))
                .pos(colLabel + 276, ctrlY).size(60, 18).build());
    }

    private int parseIntOr(String s, int def){ try{ return Integer.parseInt(s);}catch(Exception e){ return def; } }
    private float parseFloatOr(String s, float def){ try{ return Float.parseFloat(s);}catch(Exception e){ return def; } }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt){
        this.renderBackground(g);
        super.render(g, mx, my, pt);

        // Titles
        g.drawString(this.font, Component.literal("§6Night Market Admin"), left, top - 40, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal("Active preset: " + (activePreset.isEmpty()?"<none>":activePreset)), left, top - 24, 0xAAAAAA, false);

        // Section headers
        g.drawString(this.font, Component.literal("Presets"), left, top - 12, 0xFFD580, false);
        g.drawString(this.font, Component.literal("Buy Items"), colInput, top + 5*rowH - 12, 0xFFD580, false);
        g.drawString(this.font, Component.literal("Sell Item"), colInputRight, top + 5*rowH - 12, 0xFFD580, false);

        // Offers preview list on the right
        int listLeft = left + 360;
        int listTop = top;
        int i=0;
        for (OfferEntry e : offers){
            int y = listTop + i*18;
            boolean hovered = mx >= listLeft && mx <= listLeft+180 && my >= y && my <= y+16;
            int color = hovered ? 0xFFFFEE : 0xFFFFFF;
            g.drawString(this.font, Component.literal((i+1)+". "+ (e.alias.isBlank()?"<sem alias>":e.alias)), listLeft + 20, y, color, false);
            // render result item icon if available
            if (!e.sellId.isBlank()){
                ItemStack icon = itemFromId(e.sellId, e.sellCount);
                if (!icon.isEmpty()) g.renderItem(icon, listLeft, y-2);
            }
            i++; if (i>10) break; // simple preview up to 10 entries
        }

        // Render clickable suggestion overlay for focused item boxes
        if (this.getFocused() instanceof EditBox eb && isItemBox(eb)){
            List<String> sugg = findItemMatches(eb.getValue(), 5);
            currentSuggestions = sugg;
            if (!sugg.isEmpty()){
                int sx = eb.getX();
                int sy = eb.getY() + eb.getHeight() + 2;
                // Keep overlay within screen bounds
                int w = Math.min(220, Math.max(120, this.width - sx - 20));
                int h = 16 * sugg.size() + 4;
                suggX = sx; suggY = sy; suggW = w; suggH = h;
                // Background
                g.fill(sx, sy, sx + w, sy + h, 0xAA000000);
                int y = sy + 2;
                for (int j=0;j<sugg.size();j++){
                    String id = sugg.get(j);
                    // Icon
                    ItemStack icon = itemFromId(id, 1);
                    if (!icon.isEmpty()) g.renderItem(icon, sx + 2, y - 2);
                    // Text
                    g.drawString(this.font, Component.literal(id), sx + 22, y, 0xE0FFFF, false);
                    y += 16;
                }
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
                int idx = (int)((my - suggY - 2) / 16);
                if (idx >= 0 && idx < currentSuggestions.size()){
                    eb.setValue(currentSuggestions.get(idx));
                    eb.setCursorPosition(eb.getValue().length());
                    return true;
                }
            }
        }
        // Click offer list selection
        int listLeft = this.width/2 - 180 + 240;
        int listTop = this.height/2 - 110;
        for (int i=0;i<offers.size();i++){
            int y = listTop + i*18;
            if (mx >= listLeft && mx <= listLeft+180 && my >= y && my <= y+16){
                selectedOffer = offers.get(i);
                return true;
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
        return eb == buy1IdBox || eb == buy2IdBox || eb == sellIdBox;
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
