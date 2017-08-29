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
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import org.apache.avro.JsonProperties;
import org.radarcns.android.data.DataCache;
import org.radarcns.android.data.TableDataHandler;
import org.radarcns.android.device.AbstractDeviceManager;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.key.MeasurementKey;
import org.radarcns.topic.AvroTopic;
import org.radarcns.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/** Manages scanning for an Empatica E4 wearable and connecting to it */
class E4DeviceManager extends AbstractDeviceManager<E4Service, E4DeviceStatus> implements EmpaDataDelegate, EmpaStatusDelegate {
    private static final Logger logger = LoggerFactory.getLogger(E4DeviceManager.class);

    private final String apiKey;
    private Handler mHandler;
    private final HandlerThread mHandlerThread;

    private final DataCache<MeasurementKey, EmpaticaE4Acceleration> accelerationTable;
    private final DataCache<MeasurementKey, EmpaticaE4BloodVolumePulse> bvpTable;
    private final DataCache<MeasurementKey, EmpaticaE4ElectroDermalActivity> edaTable;
    private final DataCache<MeasurementKey, EmpaticaE4InterBeatInterval> ibiTable;
    private final DataCache<MeasurementKey, EmpaticaE4Temperature> temperatureTable;
    private final DataCache<MeasurementKey, EmpaticaE4SensorStatus> sensorStatusTable;
    private final AvroTopic<MeasurementKey, EmpaticaE4BatteryLevel> batteryTopic;

    private EmpaDeviceManager deviceManager;
    private boolean isScanning;
    private Pattern[] acceptableIds;

    public E4DeviceManager(E4Service e4Service, String apiKey, String userId, TableDataHandler dataHandler, E4Topics topics) {
        super(e4Service, new E4DeviceStatus(), dataHandler, userId, null);

        this.accelerationTable = dataHandler.getCache(topics.getAccelerationTopic());
        this.bvpTable = dataHandler.getCache(topics.getBloodVolumePulseTopic());
        this.edaTable = dataHandler.getCache(topics.getElectroDermalActivityTopic());
        this.ibiTable = dataHandler.getCache(topics.getInterBeatIntervalTopic());
        this.temperatureTable = dataHandler.getCache(topics.getTemperatureTopic());
        this.sensorStatusTable = dataHandler.getCache(topics.getSensorStatusTopic());
        this.batteryTopic = topics.getBatteryLevelTopic();

        this.apiKey = apiKey;
        deviceManager = null;
        // Initialize the Device Manager using your API key. You need to have Internet access at this point.
        this.mHandlerThread = new HandlerThread("E4-device-handler", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        this.mHandlerThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("Empatica crashed. Disconnecting", e);
                updateStatus(DeviceStatusListener.Status.DISCONNECTED);
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
        post(new Runnable() {
            @Override
            public void run() {
                logger.info("Creating EmpaDeviceManager");
                // Create a new EmpaDeviceManager. E4DeviceManager is both its data and status delegate.
                deviceManager = new EmpaDeviceManager(getService(), E4DeviceManager.this, E4DeviceManager.this);
                // Initialize the Device Manager using your API key. You need to have Internet access at this point.
                logger.info("Authenticating EmpaDeviceManager");
                deviceManager.authenticateWithAPIKey(apiKey);
                E4DeviceManager.this.acceptableIds = Strings.containsPatterns(acceptableIds);
                logger.info("Authenticated EmpaDeviceManager");
            }
        });
    }

    @Override
    public void didUpdateStatus(final EmpaStatus empaStatus) {
        logger.info("Updated E4 status to {}", empaStatus);
        switch (empaStatus) {
            case READY:
                post(new Runnable() {
                    @Override
                    public void run() {
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
    public void didDiscoverDevice(final BluetoothDevice bluetoothDevice, final String deviceName, int i, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        logger.info("Bluetooth address: {}", bluetoothDevice.getAddress());
        if (allowed) {
            final String sourceId = bluetoothDevice.getAddress();
            if (acceptableIds.length > 0
                    && !Strings.findAny(acceptableIds, deviceName)
                    && !Strings.findAny(acceptableIds, sourceId)) {
                logger.info("Device {} with ID {} is not listed in acceptable device IDs", deviceName, sourceId);
                getService().deviceFailedToConnect(deviceName);
                return;
            }
            setName(deviceName);
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Connect to the device
                        updateStatus(DeviceStatusListener.Status.CONNECTING);
                        deviceManager.connectDevice(bluetoothDevice);
                        getState().getId().setSourceId(sourceId);
                    } catch (ConnectionNotAllowedException e) {
                        // This should happen only if you try to connect when allowed == false.
                        getService().deviceFailedToConnect(deviceName);
                    }
                }
            });
        } else {
            logger.warn("Device {} with address {} is not an allowed device.", deviceName, bluetoothDevice.getAddress());
            getService().deviceFailedToConnect(deviceName);
        }
    }

    private void post(Runnable runnable) {
        Handler localHander = getHandler();
        if (localHander != null) {
            localHander.post(runnable);
        }
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
        localHandler.post(new Runnable() {
            @Override
            public void run() {
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
            }
        });
        this.mHandlerThread.quitSafely();
    }

    @Override
    public void didRequestEnableBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            logger.warn("Bluetooth is not enabled.");
            updateStatus(DeviceStatusListener.Status.DISCONNECTED);
        }
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        getState().setAcceleration(x / 64f, y / 64f, z / 64f);
        float[] latestAcceleration = getState().getAcceleration();
        EmpaticaE4Acceleration value = new EmpaticaE4Acceleration(
                timestamp, System.currentTimeMillis() / 1000d,
                latestAcceleration[0], latestAcceleration[1], latestAcceleration[2]);

        send(accelerationTable, value);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        getState().setBloodVolumePulse(bvp);
        EmpaticaE4BloodVolumePulse value = new EmpaticaE4BloodVolumePulse(timestamp, System.currentTimeMillis() / 1000d, bvp);
        send(bvpTable, value);
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        getState().setBatteryLevel(battery);
        EmpaticaE4BatteryLevel value = new EmpaticaE4BatteryLevel(timestamp, System.currentTimeMillis() / 1000d, battery);
        trySend(batteryTopic, 0L, value);
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        getState().setElectroDermalActivity(gsr);
        EmpaticaE4ElectroDermalActivity value = new EmpaticaE4ElectroDermalActivity(timestamp, System.currentTimeMillis() / 1000d, gsr);
        send(edaTable, value);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        getState().setInterBeatInterval(ibi);
        EmpaticaE4InterBeatInterval value = new EmpaticaE4InterBeatInterval(timestamp, System.currentTimeMillis() / 1000d, ibi);
        send(ibiTable, value);
    }

    @Override
    public void didReceiveTemperature(float temperature, double timestamp) {
        getState().setTemperature(temperature);
        EmpaticaE4Temperature value = new EmpaticaE4Temperature(timestamp, System.currentTimeMillis() / 1000d, temperature);
        send(temperatureTable, value);
    }

    @Override
    public void didUpdateSensorStatus(EmpaSensorStatus empaSensorStatus, EmpaSensorType empaSensorType) {
        getState().setSensorStatus(empaSensorType, empaSensorStatus);
        double now = System.currentTimeMillis() / 1000d;
        EmpaticaE4SensorStatus value = new EmpaticaE4SensorStatus(now, now, empaSensorType.name(), empaSensorStatus.name());
        send(sensorStatusTable, value);
    }
}
