package io.github.mitchelded.twspopup;

import org.junit.Test;
import static org.junit.Assert.*;

public class PopupLifecycleTest {
    @Test public void disconnectCancelsPendingOpenBeforeSessionExists() {
        PopupLifecycle lifecycle = new PopupLifecycle();
        int token = lifecycle.begin("a", 100);
        assertTrue(lifecycle.cancelPendingVendor("a", 200));
        assertFalse(lifecycle.isCurrent(token));
    }

    @Test public void disconnectDoesNotCancelOtherDeviceOrManualOpening() {
        PopupLifecycle lifecycle = new PopupLifecycle();
        int token = lifecycle.begin("a", 100);
        assertFalse(lifecycle.cancelPendingVendor("b", 200));
        assertTrue(lifecycle.isCurrent(token));
        token = lifecycle.begin(null, -1);
        assertFalse(lifecycle.cancelPendingVendor("a", 200));
        assertTrue(lifecycle.isCurrent(token));
    }

    @Test public void delayedDisconnectCannotCancelNewConnection() {
        PopupLifecycle lifecycle = new PopupLifecycle();
        int token = lifecycle.begin("a", 300);
        assertFalse(lifecycle.cancelPendingVendor("a", 200));
        assertTrue(lifecycle.isCurrent(token));
    }

    @Test public void staleCompletionCannotClearNewPendingOpening() {
        PopupLifecycle lifecycle = new PopupLifecycle();
        int previous = lifecycle.begin("a", 100);
        int current = lifecycle.begin("b", 200);
        lifecycle.opened(previous);
        assertTrue(lifecycle.cancelPendingVendor("b", 300));
        assertFalse(lifecycle.isCurrent(current));
    }

    @Test public void dismissalInvalidatesEarlierOpenAndTimeoutTokens() {
        PopupLifecycle lifecycle = new PopupLifecycle();
        int token = lifecycle.begin("a", 100);
        lifecycle.opened(token);
        assertFalse(lifecycle.cancelPendingVendor("a", 200));
        assertTrue(lifecycle.isCurrent(token));
        lifecycle.cancel();
        assertFalse(lifecycle.isCurrent(token));
    }
}
