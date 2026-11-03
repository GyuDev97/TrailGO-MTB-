package com.bike.trailgo.ui.riding;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BatteryAlertPolicyTest {

    @Test
    public void warnsOnceAtTwentyPercentNotAbove() {
        BatteryAlertPolicy policy = new BatteryAlertPolicy();
        assertNull(policy.evaluate(55, false));
        assertNull(policy.evaluate(21, false));
        String message = policy.evaluate(20, false);
        assertNotNull(message);
        assertTrue(message.contains("20%"));
        assertNull(policy.evaluate(19, false));
        assertNull(policy.evaluate(15, false));
    }

    @Test
    public void warnsAgainOnceAtTenPercent() {
        BatteryAlertPolicy policy = new BatteryAlertPolicy();
        assertNotNull(policy.evaluate(20, false));
        assertNotNull(policy.evaluate(10, false));
        assertNull(policy.evaluate(9, false));
        assertNull(policy.evaluate(5, false));
    }

    @Test
    public void startingAlreadyBelowTenSaysOnlyTheLastWarning() {
        BatteryAlertPolicy policy = new BatteryAlertPolicy();
        assertNotNull(policy.evaluate(8, false));
        assertNull(policy.evaluate(8, false));
        assertNull(policy.evaluate(7, false));
    }

    @Test
    public void nothingWhileChargingOrWhenUnknown() {
        BatteryAlertPolicy policy = new BatteryAlertPolicy();
        assertNull(policy.evaluate(5, true));
        assertNull(policy.evaluate(-1, false));
        assertNull(policy.evaluate(101, false));
        // charging did not use up the warning
        assertNotNull(policy.evaluate(15, false));
    }

    @Test
    public void chargingPastThirtyPercentArmsTheWarningsAgain() {
        BatteryAlertPolicy policy = new BatteryAlertPolicy();
        assertNotNull(policy.evaluate(19, false));
        assertNull(policy.evaluate(25, true));   // charging, but not yet back above the re-arm level
        assertNull(policy.evaluate(19, false));  // still the same warning
        assertNull(policy.evaluate(60, true));   // charged
        assertNotNull(policy.evaluate(19, false));
    }
}
