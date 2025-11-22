package org.lupz.doomsdayessentials.combat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

public class ManagedAreaTimeWindowTest {
    @Test
    public void daytimeWindowActiveOnlyInside() {
        ManagedArea.TimeWindow w = new ManagedArea.TimeWindow(LocalTime.of(18,0), LocalTime.of(22,0));
        Assertions.assertTrue(w.isActive(LocalTime.of(19,0)));
        Assertions.assertFalse(w.isActive(LocalTime.of(23,0)));
        Assertions.assertFalse(w.isActive(LocalTime.of(17,59)));
    }

    @Test
    public void wrapAroundMidnightWorks() {
        ManagedArea.TimeWindow w = new ManagedArea.TimeWindow(LocalTime.of(22,0), LocalTime.of(2,0));
        Assertions.assertTrue(w.isActive(LocalTime.of(23,0)));
        Assertions.assertTrue(w.isActive(LocalTime.of(1,30)));
        Assertions.assertFalse(w.isActive(LocalTime.of(3,0)));
        Assertions.assertFalse(w.isActive(LocalTime.of(21,59)));
    }

    @Test
    public void fullDayWindowAlwaysActive() {
        ManagedArea.TimeWindow w = new ManagedArea.TimeWindow(LocalTime.of(0,0), LocalTime.of(0,0));
        Assertions.assertTrue(w.isActive(LocalTime.of(12,0)));
        Assertions.assertTrue(w.isActive(LocalTime.of(23,59)));
        Assertions.assertTrue(w.isActive(LocalTime.of(0,1)));
    }
}

