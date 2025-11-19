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
        // Level 1: 4 + 1 = 5 pages * 54 = 270 slots
        Assertions.assertEquals(270, gm.getStorageCapacityItems(g));

        gm.getGuild(g).setStorageLevel(5);
        // Level 5: 4 + 5 = 9 pages * 54 = 486 slots
        Assertions.assertEquals(486, gm.getStorageCapacityItems(g));

        gm.getGuild(g).setStorageLevel(10);
        // Level 10: 4 + 10 = 14 pages * 54 = 756 slots
        Assertions.assertEquals(756, gm.getStorageCapacityItems(g));
    }
}
