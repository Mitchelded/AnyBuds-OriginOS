package io.github.mitchelded.twspopup;

import android.bluetooth.BluetoothDevice;

/** Read-only acquisition boundary. Implementations preserve unknown per-part values. */
interface BatteryProvider {
    BatterySnapshot read(BluetoothDevice device, int aggregatePercent, long readElapsedRealtimeMs);
}
