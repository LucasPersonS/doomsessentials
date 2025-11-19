package org.lupz.doomsdayessentials.event.eclipse.market;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.lupz.doomsdayessentials.item.ModItems;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads/validates black_market.json and converts it into MerchantOffers.
 * Also creates a default file on first run.
 */
public final class BlackMarketConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ResourceLocation.class, (JsonDeserializer<ResourceLocation>) (json, typeOfT, ctx) -> {
                String s = json.getAsString();
                ResourceLocation rl = s.contains(":") ? ResourceLocation.tryParse(s) : ResourceLocation.fromNamespaceAndPath("minecraft", s);
                if (rl == null) throw new JsonParseException("Invalid id: " + s);
                return rl;
            })
            .create();

    private static List<BlackMarketTrade> TRADES = List.of();

    private BlackMarketConfigManager(){}

    public static Path configDir(){
        return FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials");
    }

    public static Path configFile(){
        return configDir().resolve("black_market.json");
    }

    public static void ensureDefault() {
        try {
            Files.createDirectories(configDir());
            if (!Files.exists(configFile())) {
                String sample = sampleJson();
                Files.writeString(configFile(), sample);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reload() throws IOException, JsonParseException {
        ensureDefault();
        try (Reader r = Files.newBufferedReader(configFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            List<BlackMarketTrade> parsed = parseTrades(root);
            validate(parsed);
            TRADES = List.copyOf(parsed);
        }
    }

    public static List<BlackMarketTrade> getTrades(){ return TRADES; }

    public static MerchantOffers toOffers(@SuppressWarnings("unused") Level lvl, BlackMarketFilter filter){
        MerchantOffers offers = new MerchantOffers();
        for (BlackMarketTrade t : TRADES) {
            if (filter != null && !filter.test(t)) continue;
            MerchantOffer offer = toOffer(t);
            if (offer != null) {
                offers.add(offer);
                NightMarketManager.registerAlias(offer, t.alias);
            }
        }
        return offers;
    }

    /** Parse offers directly from a JSON string compatible with black_market.json format. */
    public static MerchantOffers toOffersFromJson(String jsonContent, @SuppressWarnings("unused") Level lvl, BlackMarketFilter filter) throws JsonParseException {
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
        List<BlackMarketTrade> parsed = parseTrades(root);
        validate(parsed);
        MerchantOffers offers = new MerchantOffers();
        for (BlackMarketTrade t : parsed) {
            if (filter != null && !filter.test(t)) continue;
            MerchantOffer offer = toOffer(t);
            if (offer != null) {
                offers.add(offer);
                NightMarketManager.registerAlias(offer, t.alias);
            }
        }
        return offers;
    }

    private static MerchantOffer toOffer(BlackMarketTrade t){
        // Buy items: support 1 or 2 items
        if (t.buys == null || t.buys.isEmpty()) return null;
        if (t.buys.size() > 2) return null;
        ItemStack buyA = stackOf(t.buys.get(0));
        if (buyA.isEmpty()) return null;
        ItemStack buyB = ItemStack.EMPTY;
        if (t.buys.size() == 2) {
            buyB = stackOf(t.buys.get(1));
            if (buyB.isEmpty()) return null;
        }

        // Sell items: size >= 1. If size > 1, create bundle item.
        if (t.sells == null || t.sells.isEmpty()) return null;
        ItemStack sellStack;
        if (t.sells.size() == 1) {
            sellStack = stackOf(t.sells.get(0));
            if (sellStack.isEmpty()) return null;
        } else {
            sellStack = BlackMarketBundleItem.createBundle(t.alias, t.sells);
            if (sellStack.isEmpty()) return null;
        }

        return buyB.isEmpty()
                ? new MerchantOffer(buyA, sellStack, t.maxUses, t.xp, t.priceMultiplier)
                : new MerchantOffer(buyA, buyB, sellStack, t.maxUses, t.xp, t.priceMultiplier);
    }

    private static ItemStack stackOf(ItemStackSpec spec){
        Item item = ForgeRegistries.ITEMS.getValue(spec.id);
        if (item == null) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(item, Math.max(1, spec.count));
        if (spec.nbt != null) s.setTag(spec.nbt.copy());
        return s;
    }

    private static List<BlackMarketTrade> parseTrades(JsonObject root){
        JsonArray arr = root.getAsJsonArray("trades");
        if (arr == null) return List.of();
        List<BlackMarketTrade> list = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String alias = obj.has("alias") ? obj.get("alias").getAsString() : UUID.randomUUID().toString();
            List<ItemStackSpec> buys = readStacks(obj.getAsJsonArray("buys"));
            List<ItemStackSpec> sells = readStacks(obj.getAsJsonArray("sells"));
            int maxUses = obj.has("maxUses") ? obj.get("maxUses").getAsInt() : 1000;
            int xp = obj.has("xp") ? obj.get("xp").getAsInt() : 0;
            float priceMult = obj.has("priceMultiplier") ? obj.get("priceMultiplier").getAsFloat() : 0.05f;
            List<String> tags = obj.has("tags") ? toStrings(obj.getAsJsonArray("tags")) : List.of();
            String mod = obj.has("mod") ? obj.get("mod").getAsString() : null;
            list.add(new BlackMarketTrade(alias, buys, sells, maxUses, xp, priceMult, tags, mod));
        }
        return list;
    }

    private static List<ItemStackSpec> readStacks(JsonArray arr){
        if (arr == null) return List.of();
        List<ItemStackSpec> out = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            ResourceLocation id = GSON.fromJson(o.get("id"), ResourceLocation.class);
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            CompoundTag tag = null;
            if (o.has("nbt")) {
                // Accept raw SNBT string or object; convert to CompoundTag
                JsonElement nbtEl = o.get("nbt");
                try {
                    if (nbtEl.isJsonPrimitive()) {
                        tag = TagParser.parseTag(nbtEl.getAsString());
                    } else {
                        // Convert object to string and parse
                        tag = TagParser.parseTag(nbtEl.toString());
                    }
                } catch (Exception ex) {
                    throw new JsonParseException("Invalid NBT for " + id + ": " + ex.getMessage(), ex);
                }
            }
            out.add(new ItemStackSpec(id, count, tag));
        }
        return out;
    }

    private static List<String> toStrings(JsonArray arr){
        if (arr == null) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr) out.add(e.getAsString());
        return out;
    }

    private static void validate(List<BlackMarketTrade> trades) {
        Set<String> aliases = new HashSet<>();
        for (BlackMarketTrade t : trades) {
            if (t.buys.isEmpty()) throw new JsonParseException("Trade " + t.alias + " has no buys");
            if (t.buys.size() > 2) throw new JsonParseException("Trade " + t.alias + " has more than 2 buys");
            if (t.sells.isEmpty()) throw new JsonParseException("Trade " + t.alias + " has no sells");
            if (!aliases.add(t.alias)) throw new JsonParseException("Duplicate alias: " + t.alias);
            for (ItemStackSpec s : t.buys) {
                if (ForgeRegistries.ITEMS.getValue(s.id) == null)
                    throw new JsonParseException("Unknown item: " + s.id);
            }
            for (ItemStackSpec s : t.sells) {
                if (ForgeRegistries.ITEMS.getValue(s.id) == null)
                    throw new JsonParseException("Unknown item: " + s.id);
            }
        }
    }

    private static String sampleJson(){
        return "{\n" +
                "  \"trades\": [\n" +
                "    {\n" +
                "      \"alias\": \"emerald_to_diamond\",\n" +
                "      \"buys\": [{ \"id\": \"minecraft:emerald\", \"count\": 5 }],\n" +
                "      \"sells\": [{ \"id\": \"minecraft:diamond\", \"count\": 1 }],\n" +
                "      \"maxUses\": 64, \"xp\": 5, \"priceMultiplier\": 0.05,\n" +
                "      \"tags\": [\"mineracao\"], \"mod\": \"minecraft\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"alias\": \"emerald_to_food_bundle\",\n" +
                "      \"buys\": [{ \"id\": \"minecraft:emerald\", \"count\": 3 }],\n" +
                "      \"sells\": [\n" +
                "        { \"id\": \"minecraft:bread\", \"count\": 2 },\n" +
                "        { \"id\": \"minecraft:carrot\", \"count\": 3 }\n" +
                "      ],\n" +
                "      \"maxUses\": 128, \"xp\": 2, \"priceMultiplier\": 0.05,\n" +
                "      \"tags\": [\"alimento\", \"pacote\"], \"mod\": \"minecraft\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }
}
