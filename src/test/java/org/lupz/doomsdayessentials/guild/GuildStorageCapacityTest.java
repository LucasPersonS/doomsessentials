package org.lupz.doomsdayessentials.guild;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class GuildStorageCapacityTest {
    @Test
    public void capacityFormulaByLevel() {
        GuildsManager gm = new GuildsManager();
        String g = "testguild";
        gm.createGuild(g, "tg", UUID.randomUUID());
        Assertions.assertEquals(1000, gm.getStorageCapacityItems(g));
        gm.getGuild(g).setStorageLevel(5);
        Assertions.assertEquals(1000 + (5 - 1) * 2000, gm.getStorageCapacityItems(g));
        gm.getGuild(g).setStorageLevel(10);
        Assertions.assertEquals(1000 + (10 - 1) * 2000, gm.getStorageCapacityItems(g));
    }
}

