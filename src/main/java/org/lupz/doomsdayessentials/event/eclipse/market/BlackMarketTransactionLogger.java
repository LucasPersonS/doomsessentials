package org.lupz.doomsdayessentials.event.eclipse.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Append-only JSONL logger of market transactions for audit/history.
 */
public final class BlackMarketTransactionLogger {
    private static final Gson GSON = new GsonBuilder().create();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private BlackMarketTransactionLogger(){}

    public static void log(Player player, MerchantOffer offer){
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("doomsdayessentials");
            Files.createDirectories(dir);
            String day = DAY.format(Instant.now());
            Path file = dir.resolve("black_market_history-" + day + ".jsonl");
            Map<String, Object> record = new HashMap<>();
            record.put("ts", Instant.now().toEpochMilli());
            record.put("player", player.getScoreboardName());
            record.put("uuid", player.getStringUUID());
            record.put("alias", NightMarketManager.aliasOf(offer));
            Map<String, Object> buy = new HashMap<>();
            buy.put("a", offer.getBaseCostA().getItem().toString() + ":" + offer.getBaseCostA().getCount());
            if (!offer.getCostB().isEmpty()) buy.put("b", offer.getCostB().getItem().toString() + ":" + offer.getCostB().getCount());
            record.put("buy", buy);
            record.put("sell", offer.getResult().getItem().toString() + ":" + offer.getResult().getCount());
            String line = GSON.toJson(record) + System.lineSeparator();
            Files.writeString(file, line, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}

