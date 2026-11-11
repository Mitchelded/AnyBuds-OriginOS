package io.github.mitchelded.twspopup;

import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Method;

/** Optional hidden/system API reader inside the Bluetooth host process. */
final class AndroidMetadataBatteryProvider implements BatteryProvider {
    @Override
    public BatterySnapshot read(BluetoothDevice device, int aggregate, long readElapsedMs) {
        Method getter;
        try {
            getter = BluetoothDevice.class.getMethod("getMetadata", int.class);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Resolving an absent or blocked API leaves every metadata field unknown.
            return MetadataBatteryProvider.read(device.getAddress(), readElapsedMs, aggregate, key -> {
                throw new UnsupportedOperationException("Bluetooth metadata API unavailable", e);
            });
        }
        return MetadataBatteryProvider.read(device.getAddress(), readElapsedMs, aggregate,
                key -> (byte[]) getter.invoke(device, key));
    }
}
