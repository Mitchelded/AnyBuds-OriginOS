package io.github.mitchelded.twspopup;

/** One read of Android's cached metadata. Source measurement time is unavailable. */
final class BatterySnapshot {
    enum Source { UNKNOWN, ANDROID_METADATA_CACHE }

    static final class Part {
        final int percent;
        final Boolean charging;
        final Source source;

        Part(int percent, Boolean charging) {
            this.percent = validPercent(percent);
            this.charging = charging;
            source = this.percent >= 0 || charging != null ? Source.ANDROID_METADATA_CACHE : Source.UNKNOWN;
        }
    }

    final String deviceAddress;
    final long readElapsedRealtimeMs;
    final int aggregatePercent;
    final Part left;
    final Part right;
    final Part chargingCase;
    final int failedReads;

    BatterySnapshot(String deviceAddress, long readElapsedRealtimeMs, int aggregatePercent,
                    Part left, Part right, Part chargingCase, int failedReads) {
        this.deviceAddress = deviceAddress;
        this.readElapsedRealtimeMs = readElapsedRealtimeMs;
        this.aggregatePercent = validPercent(aggregatePercent);
        this.left = left;
        this.right = right;
        this.chargingCase = chargingCase;
        this.failedReads = failedReads;
    }

    int chargingMask() {
        return (Boolean.TRUE.equals(left.charging) ? 1 : 0)
                | (Boolean.TRUE.equals(right.charging) ? 2 : 0)
                | (Boolean.TRUE.equals(chargingCase.charging) ? 4 : 0);
    }

    int chargingKnownMask() {
        return (left.charging != null ? 1 : 0)
                | (right.charging != null ? 2 : 0)
                | (chargingCase.charging != null ? 4 : 0);
    }

    static int validPercent(int value) {
        return value >= 0 && value <= 100 ? value : -1;
    }

    @Override
    public String toString() {
        return "source=android_metadata_cache L=" + left.percent + " R=" + right.percent
                + " case=" + chargingCase.percent + " aggregate=" + aggregatePercent
                + " charging=" + chargingMask() + " chargingKnown=" + chargingKnownMask()
                + " readElapsedMs=" + readElapsedRealtimeMs + " measurementTime=unknown"
                + " failedReads=" + failedReads;
    }
}
