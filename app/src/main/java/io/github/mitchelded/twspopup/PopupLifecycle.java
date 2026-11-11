package io.github.mitchelded.twspopup;

/** Main-thread generation tokens for delayed opens, animation callbacks and timeouts. */
final class PopupLifecycle {
    private int generation;
    private String pendingVendorAddress;
    private long pendingConnectionAt;

    int begin(String vendorAddress, long connectionAt) {
        pendingVendorAddress = vendorAddress;
        pendingConnectionAt = connectionAt;
        return ++generation;
    }

    int generation() { return generation; }

    boolean isCurrent(int token) { return token == generation; }

    void opened(int token) {
        if (isCurrent(token)) pendingVendorAddress = null;
    }

    void cancel() {
        generation++;
        pendingVendorAddress = null;
    }

    boolean cancelPendingVendor(String address, long disconnectedAt) {
        if (address == null || !address.equals(pendingVendorAddress)
                || disconnectedAt < pendingConnectionAt) return false;
        cancel();
        return true;
    }
}
