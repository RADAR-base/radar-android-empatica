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

package org.radarcns.empatica

import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.util.ArrayMap
import android.util.SparseArray
import com.empatica.empalink.ConnectionNotAllowedException
import com.empatica.empalink.EmpaDeviceManager
import com.empatica.empalink.EmpaticaDevice
import com.empatica.empalink.config.EmpaSensorStatus
import com.empatica.empalink.config.EmpaSensorType
import com.empatica.empalink.config.EmpaSessionEvent
import com.empatica.empalink.config.EmpaStatus
import com.empatica.empalink.delegate.EmpaDataDelegate
import com.empatica.empalink.delegate.EmpaSessionManagerDelegate
import com.empatica.empalink.delegate.EmpaStatusDelegate
import org.radarcns.android.device.AbstractDeviceManager
import org.radarcns.android.device.DeviceStatusListener
import org.radarcns.passive.empatica.*
import org.radarcns.util.Strings
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/** Manages scanning for an Empatica E4 wearable and connecting to it  */
class E4DeviceManager(e4Service: E4Service, private val deviceManager: EmpaDeviceManager, private val handler: Handler, private val apiKey: String) : AbstractDeviceManager<E4Service, E4DeviceStatus>(e4Service), EmpaDataDelegate, EmpaStatusDelegate, EmpaSessionManagerDelegate {
    private val accelerationTopic = createTopic("android_empatica_e4_acceleration", EmpaticaE4Acceleration::class.java)
    private val batteryLevelTopic = createTopic("android_empatica_e4_battery_level", EmpaticaE4BatteryLevel::class.java)
    private val bloodVolumePulseTopic = createTopic("android_empatica_e4_blood_volume_pulse", EmpaticaE4BloodVolumePulse::class.java)
    private val edaTopic = createTopic("android_empatica_e4_electrodermal_activity", EmpaticaE4ElectroDermalActivity::class.java)
    private val interBeatIntervalTopic = createTopic("android_empatica_e4_inter_beat_interval", EmpaticaE4InterBeatInterval::class.java)
    private val temperatureTopic = createTopic("android_empatica_e4_temperature", EmpaticaE4Temperature::class.java)
    private val sensorStatusTopic = createTopic("android_empatica_e4_sensor_status", EmpaticaE4SensorStatus::class.java)

    private val isScanning = AtomicBoolean(false)
    private var hasBeenConnecting = false
    private var acceptableIds: Array<Pattern>? = null

    init {
        logger.info("Created new E4DeviceManager {}", System.identityHashCode(this))
        // Initialize the Device Manager using your API key. You need to have Internet access at this point.
        this.acceptableIds = null
    }

    override fun start(acceptableIds: Set<String>) {
        logger.info("Starting scanning")
        handler.post {
            // Create a new EmpaDeviceManager. E4DeviceManager is both its data and status delegate.
            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            logger.info("Authenticating EmpaDeviceManager")
            deviceManager.authenticateWithAPIKey(apiKey)
            this.acceptableIds = Strings.containsPatterns(acceptableIds)
            logger.info("Authenticated EmpaDeviceManager")
        }

        // Restart scanning after a fixed timeout, to prevent BLE from stopping scanning after 30mins (on Android N)
        // https://github.com/AltBeacon/android-beacon-library/pull/529
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isScanning.get()) {
                    // start scanning again
                    try {
                        logger.info("Stopping scanning (BLE timeout)")
                        deviceManager.stopScanning()
                        logger.info("Starting scanning (BLE timeout)")
                        deviceManager.startScanning()
                        logger.info("Started scanning (BLE timeout)")
                    } catch (ex: RuntimeException) {
                        logger.error("Empatica internally did not initialize")
                    }
                }

                handler.postDelayed(this, SCAN_TIMEOUT)
            }
        }, SCAN_TIMEOUT)
    }

    override fun didUpdateStatus(empaStatus: EmpaStatus) {
        logger.info("{}: Updated E4 status to {}", hashCode(), empaStatus)
        when (empaStatus) {
            EmpaStatus.READY -> handler.post {
                // The device manager is ready for use
                // Start scanning
                try {
                    if (isScanning.compareAndSet(false, true)) {
                        deviceManager.startScanning()
                        logger.info("Started scanning")
                    }
                    updateStatus(DeviceStatusListener.Status.READY)
                } catch (ex: RuntimeException) {
                    logger.error("Empatica internally did not initialize")
                    updateStatus(DeviceStatusListener.Status.DISCONNECTED)
                }
            }
            EmpaStatus.CONNECTING -> hasBeenConnecting = true
            EmpaStatus.CONNECTED -> updateStatus(DeviceStatusListener.Status.CONNECTED)
            EmpaStatus.DISCONNECTED ->
                // The device manager disconnected from a device. Before it ever makes a connection,
                // it also calls this, so check if we have a connected device first.
                if (hasBeenConnecting) {
                    updateStatus(DeviceStatusListener.Status.DISCONNECTED)
                }
            else -> {
                // not handling all cases
            }
        }
    }

    override fun didEstablishConnection() {
        logger.info("Established connection with E4")
    }

    override fun didDiscoverDevice(empaDevice: EmpaticaDevice, deviceName: String, rssi: Int, allowed: Boolean) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        val address = empaDevice.device.address
        logger.info("{}: Bluetooth address: {}", System.identityHashCode(this), address)
        if (allowed) {
            if (acceptableIds!!.isNotEmpty()
                    && !Strings.findAny(acceptableIds!!, deviceName)
                    && !Strings.findAny(acceptableIds!!, address)) {
                logger.info("Device {} with ID {} is not listed in acceptable device IDs", deviceName, address)
                service.deviceFailedToConnect(deviceName)
                return
            }
            stopScanning()
            logger.info("Will connect device {}", deviceName)
            handler.post {
                try {
                    // Connect to the device
                    updateStatus(DeviceStatusListener.Status.CONNECTING)
                    deviceManager.connectDevice(empaDevice)

                    val attributes = ArrayMap<String, String>(5)
                    attributes["sdk"] = "empalink-2.2.aar"
                    attributes["macAddress"] = address
                    attributes["hardwareId"] = empaDevice.hardwareId
                    attributes["serialNumber"] = empaDevice.serialNumber
                    attributes["name"] = deviceName
                    service.registerDevice(deviceName, attributes)
                } catch (e: ConnectionNotAllowedException) {
                    // This should happen only if you try to connect when allowed == false.
                    service.deviceFailedToConnect(deviceName)
                }
            }
        } else {
            logger.warn("Device {} with address {} is not an allowed device.", deviceName, empaDevice.device.address)
            service.deviceFailedToConnect(deviceName)
        }
    }

    override fun registerDeviceAtReady() {
        // do not register at ready, register later
    }

    private fun stopScanning() {
        if (isScanning.compareAndSet(true, false)) {
            try {
                deviceManager.stopScanning()
            } catch (ex: NullPointerException) {
                logger.warn("Empatica internally already stopped scanning")
            }
        }
    }

    @Throws(IOException::class)
    override fun close() {
        logger.info("Closing device {}", name)
        if (isClosed) {
            return
        }
        super.close()
        handler.post {
            stopScanning()
            logger.info("Initiated device {} stop-sequence", name)
            if (hasBeenConnecting) {
                try {
                    deviceManager.cancelConnection()
                } catch (ex: NullPointerException) {
                    logger.warn("Empatica internally already disconnected")
                }

            }
            logger.info("Finished device {} stop-sequence", name)
        }
    }

    override fun didRequestEnableBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            logger.warn("Bluetooth is not enabled.")
            updateStatus(DeviceStatusListener.Status.DISCONNECTED)
        }
    }

    override fun didUpdateOnWristStatus(status: Int) {
        val now = System.currentTimeMillis() / 1000.0
        send(sensorStatusTopic, EmpaticaE4SensorStatus(
                now, now, "e4", STATUS_NAMES.get(status, "UNKNOWN")))
    }

    override fun didReceiveAcceleration(x: Int, y: Int, z: Int, timestamp: Double) {
        state.setAcceleration(x / 64f, y / 64f, z / 64f)
        val latestAcceleration = state.acceleration
        send(accelerationTopic, EmpaticaE4Acceleration(
                timestamp, System.currentTimeMillis() / 1000.0,
                latestAcceleration[0], latestAcceleration[1], latestAcceleration[2]))
    }

    override fun didReceiveBVP(bvp: Float, timestamp: Double) {
        state.bloodVolumePulse = bvp
        send(bloodVolumePulseTopic, EmpaticaE4BloodVolumePulse(
                timestamp, System.currentTimeMillis() / 1000.0, bvp))
    }

    override fun didReceiveBatteryLevel(battery: Float, timestamp: Double) {
        state.batteryLevel = battery
        send(batteryLevelTopic, EmpaticaE4BatteryLevel(
                timestamp, System.currentTimeMillis() / 1000.0, battery))
    }

    override fun didReceiveTag(timestamp: Double) {

    }

    override fun didReceiveGSR(gsr: Float, timestamp: Double) {
        state.electroDermalActivity = gsr
        val value = EmpaticaE4ElectroDermalActivity(timestamp, System.currentTimeMillis() / 1000.0, gsr)
        send(edaTopic, value)
    }

    override fun didReceiveIBI(ibi: Float, timestamp: Double) {
        state.interBeatInterval = ibi
        val value = EmpaticaE4InterBeatInterval(timestamp, System.currentTimeMillis() / 1000.0, ibi)
        send(interBeatIntervalTopic, value)
    }

    override fun didReceiveTemperature(temperature: Float, timestamp: Double) {
        state.temperature = temperature
        val value = EmpaticaE4Temperature(timestamp, System.currentTimeMillis() / 1000.0, temperature)
        send(temperatureTopic, value)
    }

    override fun didUpdateSensorStatus(status: Int, type: EmpaSensorType) {
        val statusString = STATUS_NAMES.get(status, "UNKNOWN")
        state.setSensorStatus(type, statusString)
        val now = System.currentTimeMillis() / 1000.0
        val value = EmpaticaE4SensorStatus(now, now, type.name, statusString)
        send(sensorStatusTopic, value)
    }

    override fun hashCode() = System.identityHashCode(this)

    override fun equals(other: Any?) = this === other

    override fun didUpdateSessionStatus(event: EmpaSessionEvent?, progress: Float) {
        logger.info("Empatica session event {} with progress {}", event, progress)
        if (event == EmpaSessionEvent.UNAUTHORIZED_USER_ERROR) {
            updateStatus(DeviceStatusListener.Status.DISCONNECTED)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(E4DeviceManager::class.java)
        private val STATUS_NAMES = SparseArray<String>()

        init {
            STATUS_NAMES.put(EmpaSensorStatus.ON_WRIST, "ON_WRIST")
            STATUS_NAMES.put(EmpaSensorStatus.NOT_ON_WRIST, "NOT_ON_WRIST")
            STATUS_NAMES.put(EmpaSensorStatus.DEAD, "DEAD")
        }

        // BLE scan timeout
        private const val ANDROID_N_MAX_SCAN_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val SCAN_TIMEOUT = ANDROID_N_MAX_SCAN_DURATION_MS / 2
    }
}
