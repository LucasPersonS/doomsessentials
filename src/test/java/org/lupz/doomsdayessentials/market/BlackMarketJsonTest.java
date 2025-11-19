package org.lupz.doomsdayessentials.market;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketConfigManager;
import org.lupz.doomsdayessentials.event.eclipse.market.BlackMarketTrade;

import java.util.List;

public class BlackMarketJsonTest {
    @Test
    public void parseBasicTrades() {
        String json = "{\n" +
                "  \"trades\": [\n" +
                "    {\n" +
                "      \"alias\": \"t1\",\n" +
                "      \"buys\": [{ \"id\": \"minecraft:emerald\", \"count\": 2 }],\n" +
                "      \"sells\": [{ \"id\": \"minecraft:bread\", \"count\": 1 }]\n" +
                "    },\n" +
                "    {\n" +
                "      \"alias\": \"t2\",\n" +
                "      \"buys\": [\n" +
                "        { \"id\": \"minecraft:emerald\", \"count\": 1 },\n" +
                "        { \"id\": \"minecraft:stick\", \"count\": 3 }\n" +
                "      ],\n" +
                "      \"sells\": [\n" +
                "        { \"id\": \"minecraft:carrot\", \"count\": 2 },\n" +
                "        { \"id\": \"minecraft:bread\", \"count\": 1 }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<BlackMarketTrade> trades = invokeParseTrades(root);
        Assertions.assertEquals(2, trades.size());
        Assertions.assertEquals("t1", trades.get(0).alias);
        Assertions.assertEquals(1, trades.get(0).sells.size());
        Assertions.assertEquals(2, trades.get(1).sells.size());
        Assertions.assertEquals(2, trades.get(1).buys.size());
    }

    // Use reflection to access parseTrades for unit test without reloading from disk
    @SuppressWarnings("unchecked")
    private static List<BlackMarketTrade> invokeParseTrades(JsonObject root){
        try {
            var m = BlackMarketConfigManager.class.getDeclaredMethod("parseTrades", JsonObject.class);
            m.setAccessible(true);
            return (List<BlackMarketTrade>) m.invoke(null, root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

