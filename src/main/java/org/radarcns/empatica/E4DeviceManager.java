/*
 * Copyright 2017 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarcns.empatica;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.widget.Toast;

import com.empatica.empalink.ConfigurationProfileException;
import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaSessionEvent;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import org.radarcns.android.auth.AppSource;
import org.radarcns.android.device.AbstractDeviceManager;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.android.util.Boast;
import org.radarcns.kafka.ObservationKey;
import org.radarcns.passive.empatica.EmpaticaE4Acceleration;
import org.radarcns.passive.empatica.EmpaticaE4BatteryLevel;
import org.radarcns.passive.empatica.EmpaticaE4BloodVolumePulse;
import org.radarcns.passive.empatica.EmpaticaE4ElectroDermalActivity;
import org.radarcns.passive.empatica.EmpaticaE4InterBeatInterval;
import org.radarcns.passive.empatica.EmpaticaE4SensorStatus;
import org.radarcns.passive.empatica.EmpaticaE4Temperature;
import org.radarcns.topic.AvroTopic;
import org.radarcns.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Manages scanning for an Empatica E4 wearable and connecting to it */
@SuppressWarnings("WeakerAccess")
class E4DeviceManager extends AbstractDeviceManager<E4Service, E4DeviceStatus> implements EmpaDataDelegate, EmpaStatusDelegate {
    private static final Logger logger = LoggerFactory.getLogger(E4DeviceManager.class);

    private final String apiKey;
    private final Thread.UncaughtExceptionHandler originalExHandler;
    private final Thread mainThread;
    private Handler mHandler;
    private final HandlerThread mHandlerThread;

    private final AvroTopic<ObservationKey, EmpaticaE4Acceleration> accelerationTopic =
            createTopic("android_empatica_e4_acceleration", EmpaticaE4Acceleration.class);
    private final AvroTopic<ObservationKey, EmpaticaE4BatteryLevel> batteryLevelTopic =
            createTopic("android_empatica_e4_battery_level", EmpaticaE4BatteryLevel.class);
    private final AvroTopic<ObservationKey, EmpaticaE4BloodVolumePulse> bloodVolumePulseTopic =
            createTopic("android_empatica_e4_blood_volume_pulse", EmpaticaE4BloodVolumePulse.class);
    private final AvroTopic<ObservationKey, EmpaticaE4ElectroDermalActivity> edaTopic =
            createTopic("android_empatica_e4_electrodermal_activity", EmpaticaE4ElectroDermalActivity.class);
    private final AvroTopic<ObservationKey, EmpaticaE4InterBeatInterval> interBeatIntervalTopic =
            createTopic("android_empatica_e4_inter_beat_interval", EmpaticaE4InterBeatInterval.class);
    private final AvroTopic<ObservationKey, EmpaticaE4Temperature> temperatureTopic =
            createTopic("android_empatica_e4_temperature", EmpaticaE4Temperature.class);
    private final AvroTopic<ObservationKey, EmpaticaE4SensorStatus> sensorStatusTopic =
            createTopic("android_empatica_e4_sensor_status", EmpaticaE4SensorStatus.class);

    private EmpaDeviceManager deviceManager;
    private boolean isScanning;
    private Pattern[] acceptableIds;
    private static final SparseArray<String> STATUS_NAMES = new SparseArray<>();
    static {
        STATUS_NAMES.put(EmpaSensorStatus.ON_WRIST, "ON_WRIST");
        STATUS_NAMES.put(EmpaSensorStatus.NOT_ON_WRIST, "NOT_ON_WRIST");
        STATUS_NAMES.put(EmpaSensorStatus.DEAD, "DEAD");
    }

    // BLE scan timeout
    private static final long ANDROID_N_MAX_SCAN_DURATION_MS = 30 * 60 * 1000L; // 30 minutes
    private static final long SCAN_TIMEOUT = ANDROID_N_MAX_SCAN_DURATION_MS / 2;

    public E4DeviceManager(E4Service e4Service, String apiKey) {
        super(e4Service);

        this.apiKey = apiKey;
        deviceManager = null;
        // Initialize the Device Manager using your API key. You need to have Internet access at this point.
        this.mHandlerThread = new HandlerThread("E4-device-handler", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        this.mHandlerThread.setUncaughtExceptionHandler((t, e) -> {
            logger.error("Empatica crashed. Disconnecting", e);
            Boast.makeText(getService(), R.string.empatica_failed, Toast.LENGTH_LONG).show();
            updateStatus(DeviceStatusListener.Status.DISCONNECTED);
        });
        mainThread = Thread.currentThread();
        originalExHandler = mainThread.getUncaughtExceptionHandler();
        mainThread.setUncaughtExceptionHandler((t, e) -> {
            if (e instanceof ConfigurationProfileException) {
                logger.error("Empatica crashed because there is no internet connection. Disconnecting", e);
                Boast.makeText(getService(), R.string.empatica_failed, Toast.LENGTH_LONG).show();
                updateStatus(DeviceStatusListener.Status.DISCONNECTED);
            } else if (originalExHandler != null){
                originalExHandler.uncaughtException(t, e);
            }
        });
        this.isScanning = false;
        this.acceptableIds = null;
    }

    @Override
    public void start(@NonNull final Set<String> acceptableIds) {
        logger.info("Starting scanning");
        this.mHandlerThread.start();
        synchronized (this) {
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
        }
        post(() -> {
            logger.info("Creating EmpaDeviceManager");
            // Create a new EmpaDeviceManager. E4DeviceManager is both its data and status delegate.
            deviceManager = new EmpaDeviceManager(getService(), E4DeviceManager.this, E4DeviceManager.this, (EmpaSessionEvent event, float progress) -> {
                logger.info("Empatica session event {} with progress {}", event, progress);
                if (event == EmpaSessionEvent.UNAUTHORIZED_USER_ERROR) {
                    updateStatus(DeviceStatusListener.Status.DISCONNECTED);
                }
            });
            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            logger.info("Authenticating EmpaDeviceManager");
            deviceManager.authenticateWithAPIKey(apiKey);
            E4DeviceManager.this.acceptableIds = Strings.containsPatterns(acceptableIds);
            logger.info("Authenticated EmpaDeviceManager");
        });

        // Restart scanning after a fixed timeout, to prevent BLE from stopping scanning after 30mins (on Android N)
        // https://github.com/AltBeacon/android-beacon-library/pull/529
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (deviceManager != null && isScanning) {
                    // stop scanning
                    logger.info("Stopping scanning (BLE timeout)");
                    try {
                        deviceManager.stopScanning();
                        logger.info("Stopped scanning (BLE timeout)");
                        isScanning = false;
                    } catch (NullPointerException ex) {
                        logger.warn("Empatica internally already stopped scanning");
                    }

                    // start scanning again
                    logger.info("Starting scanning (BLE timeout)");
                    try {
                        deviceManager.startScanning();
                        logger.info("Started scanning (BLE timeout)");
                        isScanning = true;
                    } catch (NullPointerException ex) {
                        logger.error("Empatica internally did not initialize");
                    }
                }

                Handler localHandler = getHandler();
                if (localHandler != null) {
                    localHandler.postDelayed(this, SCAN_TIMEOUT);
                }
            }
        }, SCAN_TIMEOUT);
    }

    @Override
    public void didUpdateStatus(final EmpaStatus empaStatus) {
        logger.info("Updated E4 status to {}", empaStatus);
        switch (empaStatus) {
            case READY:
                post(() -> {
                    // somehow, the status is set to disconnected when EmpaDeviceManager is
                    // being created
                    if (deviceManager == null) {
                        return;
                    }
                    // The device manager is ready for use
                    // Start scanning
                    try {
                        deviceManager.startScanning();
                        logger.info("Started scanning");
                        isScanning = true;
                        updateStatus(DeviceStatusListener.Status.READY);
                    } catch (NullPointerException ex) {
                        logger.error("Empatica internally did not initialize");
                        updateStatus(DeviceStatusListener.Status.DISCONNECTED);
                    }
                });
                break;
            case CONNECTED:
                if (isScanning) {
                    logger.info("Stopping scanning");
                    try {
                        deviceManager.stopScanning();
                    } catch (NullPointerException ex) {
                        logger.warn("Empatica internally already stopped scanning");
                    }
                    isScanning = false;
                }
                updateStatus(DeviceStatusListener.Status.CONNECTED);
                break;
            case DISCONNECTED:
                // The device manager disconnected from a device. Before it ever makes a connection,
                // it also calls this, so check if we have a connected device first.
                if (getState().getStatus() != DeviceStatusListener.Status.DISCONNECTED && getName() != null) {
                    updateStatus(DeviceStatusListener.Status.DISCONNECTED);
                }
                break;
        }
    }

    @Override
    public void didEstablishConnection() {
        logger.info("Established connection with E4");
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice empaDevice, String deviceName, int rssi, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        logger.info("Bluetooth address: {}", empaDevice.device.getAddress());
        if (allowed) {
            final String sourceId = empaDevice.device.getAddress();
            if (acceptableIds.length > 0
                    && !Strings.findAny(acceptableIds, deviceName)
                    && !Strings.findAny(acceptableIds, sourceId)) {
                logger.info("Device {} with ID {} is not listed in acceptable device IDs", deviceName, sourceId);
                getService().deviceFailedToConnect(deviceName);
                return;
            }
            setName(deviceName);
            post(() -> {
                try {
                    // Connect to the device
                    updateStatus(DeviceStatusListener.Status.CONNECTING);
                    deviceManager.connectDevice(empaDevice);

                    Map<String, String> attributes = new ArrayMap<>(3);
                    attributes.put("sdk", "empalink-2.2.aar");
                    attributes.put("macAddress", empaDevice.device.getAddress());
                    attributes.put("hardwareId", empaDevice.hardwareId);
                    attributes.put("serialNumber", empaDevice.serialNumber);
                    attributes.put("name", deviceName);
                    getService().registerDevice(deviceName, attributes);
                } catch (ConnectionNotAllowedException e) {
                    // This should happen only if you try to connect when allowed == false.
                    getService().deviceFailedToConnect(deviceName);
                }
            });
        } else {
            logger.warn("Device {} with address {} is not an allowed device.", deviceName, empaDevice.device.getAddress());
            getService().deviceFailedToConnect(deviceName);
        }
    }

    private void post(Runnable runnable) {
        Handler localHander = getHandler();
        if (localHander != null) {
            localHander.post(runnable);
        }
    }

    @Override
    protected void registerDeviceAtReady() {
        // do not register at ready, register later
    }

    @Override
    public void didRegister(AppSource source) {
        super.didRegister(source);
        getState().getId().setSourceId(source.getSourceId());
    }

    private synchronized Handler getHandler() {
        return this.mHandler;
    }

    @Override
    public boolean isClosed() {
        return getHandler() == null;
    }

    @Override
    public void close() {
        logger.info("Closing device {}", getName());
        Handler localHandler;
        synchronized (this) {
            if (mHandler == null) {
                throw new IllegalStateException("Already closed");
            }
            localHandler = mHandler;
            mHandler = null;
        }
        localHandler.post(() -> {
            String name = getName();
            logger.info("Initiated device {} stop-sequence", name);
            if (isScanning) {
                try {
                    deviceManager.stopScanning();
                } catch (NullPointerException ex) {
                    logger.warn("Empatica internally already stopped scanning");
                }
                isScanning = false;
            }
            if (name != null) {
                try {
                    deviceManager.disconnect();
                } catch (NullPointerException ex) {
                    logger.warn("Empatica internally already disconnected");
                }
            }
            logger.info("Cleaning up device manager");
            try {
                deviceManager.cleanUp();
            } catch (NullPointerException ex) {
                logger.warn("Empatica internally already cleaned up");
            }
            logger.info("Cleaned up device manager");
            if (getState().getStatus() != DeviceStatusListener.Status.DISCONNECTED) {
                updateStatus(DeviceStatusListener.Status.DISCONNECTED);
            }
            logger.info("Finished device {} stop-sequence", name);
        });
        this.mHandlerThread.quitSafely();
        mainThread.setUncaughtExceptionHandler(originalExHandler);
    }

    @Override
    public void didRequestEnableBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            logger.warn("Bluetooth is not enabled.");
            updateStatus(DeviceStatusListener.Status.DISCONNECTED);
        }
    }

    @Override
    public void didUpdateOnWristStatus(int status) {
        double now = System.currentTimeMillis() / 1000d;
        EmpaticaE4SensorStatus value = new EmpaticaE4SensorStatus(now, now, "e4", STATUS_NAMES.get(status, "UNKNOWN"));
        send(sensorStatusTopic, value);
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        getState().setAcceleration(x / 64f, y / 64f, z / 64f);
        float[] latestAcceleration = getState().getAcceleration();
        EmpaticaE4Acceleration value = new EmpaticaE4Acceleration(
                timestamp, System.currentTimeMillis() / 1000d,
                latestAcceleration[0], latestAcceleration[1], latestAcceleration[2]);

        send(accelerationTopic, value);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        getState().setBloodVolumePulse(bvp);
        EmpaticaE4BloodVolumePulse value = new EmpaticaE4BloodVolumePulse(timestamp, System.currentTimeMillis() / 1000d, bvp);
        send(bloodVolumePulseTopic, value);
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        getState().setBatteryLevel(battery);
        EmpaticaE4BatteryLevel value = new EmpaticaE4BatteryLevel(timestamp, System.currentTimeMillis() / 1000d, battery);
        send(batteryLevelTopic, value);
    }

    @Override
    public void didReceiveTag(double timestamp) {

    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        getState().setElectroDermalActivity(gsr);
        EmpaticaE4ElectroDermalActivity value = new EmpaticaE4ElectroDermalActivity(timestamp, System.currentTimeMillis() / 1000d, gsr);
        send(edaTopic, value);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        getState().setInterBeatInterval(ibi);
        EmpaticaE4InterBeatInterval value = new EmpaticaE4InterBeatInterval(timestamp, System.currentTimeMillis() / 1000d, ibi);
        send(interBeatIntervalTopic, value);
    }

    @Override
    public void didReceiveTemperature(float temperature, double timestamp) {
        getState().setTemperature(temperature);
        EmpaticaE4Temperature value = new EmpaticaE4Temperature(timestamp, System.currentTimeMillis() / 1000d, temperature);
        send(temperatureTopic, value);
    }

    @Override
    public void didUpdateSensorStatus(int status, EmpaSensorType type) {
        String statusString = STATUS_NAMES.get(status, "UNKNOWN");
        getState().setSensorStatus(type, statusString);
        double now = System.currentTimeMillis() / 1000d;
        EmpaticaE4SensorStatus value = new EmpaticaE4SensorStatus(now, now, type.name(), statusString);
        send(sensorStatusTopic, value);
    }
}
