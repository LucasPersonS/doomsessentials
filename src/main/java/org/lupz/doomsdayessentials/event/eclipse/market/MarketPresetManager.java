package org.lupz.doomsdayessentials.event.eclipse.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles local persistence of per-market presets with simple versioning.
 * Presets are stored under config/doomsdayessentials/markets/<marketId>/presets/presetName-vN.json
 */
public final class MarketPresetManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MarketPresetManager(){}

    public static Path marketsDir(){
        return FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials").resolve("markets");
    }

    public static Path marketDir(java.util.UUID marketId){
        return marketsDir().resolve(marketId.toString());
    }

    public static Path presetsDir(java.util.UUID marketId){
        return marketDir(marketId).resolve("presets");
    }

    /** Save current offers of a market as a preset with an auto-incremented version. */
    public static int savePreset(Level lvl, java.util.UUID marketId, String presetName) throws IOException {
        MerchantOffers offers = NightMarketManager.getOffers(lvl, marketId);
        Files.createDirectories(presetsDir(marketId));
        int nextVersion = nextVersion(presetsDir(marketId), presetName);
        Path file = presetsDir(marketId).resolve(presetName + "-v" + nextVersion + ".json");
        String json = exportOffersToJson(lvl, offers);
        Files.writeString(file, json);
        return nextVersion;
    }

    /** Load a preset into the market, replacing current offers. If version is empty, loads highest version. */
    public static boolean loadPreset(Level lvl, java.util.UUID marketId, String presetName, Optional<Integer> version){
        try {
            Files.createDirectories(presetsDir(marketId));
            Path file = version.isPresent()
                    ? presetsDir(marketId).resolve(presetName + "-v" + version.get() + ".json")
                    : latestPresetFile(presetsDir(marketId), presetName).orElse(null);
            if (file == null || !Files.exists(file)) return false;
            String content = Files.readString(file);
            MerchantOffers offers = BlackMarketConfigManager.toOffersFromJson(content, lvl, new BlackMarketFilter(null, java.util.Set.of(), null));
            NightMarketManager.setOffers(marketId, offers);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /** List available presets for the given market. */
    public static List<String> listPresetNames(java.util.UUID marketId) throws IOException {
        Path dir = presetsDir(marketId);
        if (!Files.exists(dir)) return List.of();
        try (var stream = Files.list(dir)){
            return stream.filter(p->p.getFileName().toString().endsWith(".json"))
                    .map(p->{
                        String name = p.getFileName().toString();
                        int idx = name.lastIndexOf("-v");
                        return idx>0 ? name.substring(0, idx) : name.replace(".json", "");
                    })
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /** List versions available for a preset name. */
    public static List<Integer> listPresetVersions(java.util.UUID marketId, String presetName) throws IOException {
        Path dir = presetsDir(marketId);
        if (!Files.exists(dir)) return List.of();
        try (var stream = Files.list(dir)){
            return stream.filter(p->p.getFileName().toString().startsWith(presetName+"-v"))
                    .map(p->{
                        String name = p.getFileName().toString();
                        String vStr = name.substring((presetName+"-v").length(), name.length()-".json".length());
                        try { return Integer.parseInt(vStr);} catch (NumberFormatException e){ return -1; }
                    })
                    .filter(v->v>=0)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /** Generate a human-readable diff between two versions of a preset. */
    public static String diffPresets(Level lvl, java.util.UUID marketId, String presetName, int v1, int v2){
        try {
            Path dir = presetsDir(marketId);
            Path f1 = dir.resolve(presetName+"-v"+v1+".json");
            Path f2 = dir.resolve(presetName+"-v"+v2+".json");
            if (!Files.exists(f1) || !Files.exists(f2)) return "Preset versions not found.";
            var offers1 = BlackMarketConfigManager.toOffersFromJson(Files.readString(f1), lvl, new BlackMarketFilter(null, java.util.Set.of(), null));
            var offers2 = BlackMarketConfigManager.toOffersFromJson(Files.readString(f2), lvl, new BlackMarketFilter(null, java.util.Set.of(), null));
            return humanReadableDiff(offers1, offers2);
        } catch (Exception e){
            return "Error diffing presets: " + e.getMessage();
        }
    }

    // -- helpers --
    private static int nextVersion(Path dir, String presetName) throws IOException {
        List<Integer> versions = listPresetVersionsFromDir(dir, presetName);
        return versions.isEmpty() ? 1 : (versions.get(versions.size()-1) + 1);
    }

    private static Optional<Path> latestPresetFile(Path dir, String presetName) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(dir)){
            stream.filter(p->p.getFileName().toString().startsWith(presetName+"-v") && p.getFileName().toString().endsWith(".json"))
                    .forEach(files::add);
        }
        files.sort(Comparator.comparingInt(p->{
            String name = p.getFileName().toString();
            String vStr = name.substring((presetName+"-v").length(), name.length()-".json".length());
            try { return Integer.parseInt(vStr);} catch (NumberFormatException e){ return -1; }
        }));
        if (files.isEmpty()) return Optional.empty();
        return Optional.of(files.get(files.size()-1));
    }

    private static List<Integer> listPresetVersionsFromDir(Path dir, String presetName) throws IOException {
        if (!Files.exists(dir)) return List.of();
        try (var stream = Files.list(dir)){
            return stream.filter(p->p.getFileName().toString().startsWith(presetName+"-v") && p.getFileName().toString().endsWith(".json"))
                    .map(p->{
                        String name = p.getFileName().toString();
                        String vStr = name.substring((presetName+"-v").length(), name.length()-".json".length());
                        try { return Integer.parseInt(vStr);} catch (NumberFormatException e){ return -1; }
                    })
                    .filter(v->v>=0)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /** Export offers to JSON compatible with BlackMarketConfigManager format. */
    public static String exportOffersToJson(Level lvl, MerchantOffers offers){
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        int i=1;
        for (MerchantOffer o : offers){
            JsonObject t = new JsonObject();
            String alias = NightMarketManager.aliasOf(o);
            if (alias == null || alias.isEmpty()) alias = "offer-"+i;
            t.addProperty("alias", alias);
            JsonArray buys = new JsonArray();
            buys.add(stackToJson(o.getBaseCostA()));
            ItemStack b = o.getCostB(); if (!b.isEmpty()) buys.add(stackToJson(b));
            t.add("buys", buys);
            JsonArray sells = new JsonArray();
            sells.add(stackToJson(o.getResult()));
            t.add("sells", sells);
            t.addProperty("maxUses", o.getMaxUses());
            t.addProperty("xp", o.getXp());
            t.addProperty("priceMultiplier", o.getPriceMultiplier());
            arr.add(t);
            i++;
        }
        root.add("trades", arr);
        return GSON.toJson(root);
    }

    private static JsonObject stackToJson(ItemStack s){
        JsonObject o = new JsonObject();
        o.addProperty("id", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).toString());
        o.addProperty("count", s.getCount());
        if (s.getTag() != null && !s.getTag().isEmpty()){
            // Persist raw NBT as a string (round-trip supported by ItemStackSpec in the main parser)
            o.addProperty("nbt", s.getTag().toString());
        }
        return o;
    }

    private static String humanReadableDiff(MerchantOffers a, MerchantOffers b){
        StringBuilder sb = new StringBuilder();
        sb.append("Diff (A -> B)\n");
        sb.append("A size: ").append(a.size()).append(", B size: ").append(b.size()).append('\n');
        // naive diff: list aliases and result items
        sb.append("-- A only --\n");
        for (MerchantOffer oa : a){
            String sig = NightMarketManager.aliasOf(oa)+" -> "+oa.getResult().getHoverName().getString();
            boolean inB = b.stream().anyMatch(ob -> ob.getResult().getItem() == oa.getResult().getItem() && ob.getResult().getCount()==oa.getResult().getCount());
            if (!inB) sb.append(sig).append('\n');
        }
        sb.append("-- B only --\n");
        for (MerchantOffer ob : b){
            String sig = NightMarketManager.aliasOf(ob)+" -> "+ob.getResult().getHoverName().getString();
            boolean inA = a.stream().anyMatch(oa -> oa.getResult().getItem() == ob.getResult().getItem() && oa.getResult().getCount()==ob.getResult().getCount());
            if (!inA) sb.append(sig).append('\n');
        }
        return sb.toString();
    }
}
