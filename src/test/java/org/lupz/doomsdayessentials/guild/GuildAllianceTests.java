package org.lupz.doomsdayessentials.guild;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class GuildAllianceTests {
    @Test
    public void addAndCheckAllianceOnGuildModel() {
        Guild alpha = new Guild("alpha", "A", UUID.randomUUID());
        Guild beta = new Guild("beta", "B", UUID.randomUUID());
        alpha.addAlly(beta.getName());
        beta.addAlly(alpha.getName());
        Assertions.assertTrue(alpha.isAlliedWith(beta.getName()));
        Assertions.assertTrue(beta.isAlliedWith(alpha.getName()));
        alpha.removeAlly(beta.getName());
        Assertions.assertFalse(alpha.isAlliedWith(beta.getName()));
    }
}
