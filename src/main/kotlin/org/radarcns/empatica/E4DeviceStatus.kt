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

import android.os.Parcel
import android.os.Parcelable
import com.empatica.empalink.config.EmpaSensorType
import org.radarcns.android.device.BaseDeviceState
import org.radarcns.android.device.DeviceStateCreator
import java.util.*

/**
 * The status on a single point in time of an Empatica E4 device.
 */
class E4DeviceStatus() : BaseDeviceState() {
    private val acceleration = floatArrayOf(java.lang.Float.NaN, java.lang.Float.NaN, java.lang.Float.NaN)
    private var batteryLevel = java.lang.Float.NaN
    @set:Synchronized
    var bloodVolumePulse = java.lang.Float.NaN
    @set:Synchronized
    var electroDermalActivity = java.lang.Float.NaN
    @set:Synchronized
    var interBeatInterval = java.lang.Float.NaN
    private var temperature = java.lang.Float.NaN
    private val sensorStatus = EnumMap<EmpaSensorType, String>(EmpaSensorType::class.java)

    constructor(parcel: Parcel) : this() {
        batteryLevel = parcel.readFloat()
        bloodVolumePulse = parcel.readFloat()
        electroDermalActivity = parcel.readFloat()
        interBeatInterval = parcel.readFloat()
        temperature = parcel.readFloat()
    }

    @Synchronized
    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeFloat(this.acceleration[0])
        dest.writeFloat(this.acceleration[1])
        dest.writeFloat(this.acceleration[2])
        dest.writeFloat(this.batteryLevel)
        dest.writeFloat(this.bloodVolumePulse)
        dest.writeFloat(this.electroDermalActivity)
        dest.writeFloat(this.interBeatInterval)
        dest.writeFloat(this.temperature)
        dest.writeInt(sensorStatus.size)
        for ((key, value) in sensorStatus) {
            dest.writeInt(key.ordinal)
            dest.writeString(value)
        }
    }

    override fun updateFromParcel(`in`: Parcel) {
        super.updateFromParcel(`in`)
        acceleration[0] = `in`.readFloat()
        acceleration[1] = `in`.readFloat()
        acceleration[2] = `in`.readFloat()
        batteryLevel = `in`.readFloat()
        bloodVolumePulse = `in`.readFloat()
        electroDermalActivity = `in`.readFloat()
        interBeatInterval = `in`.readFloat()
        temperature = `in`.readFloat()
        val numSensors = `in`.readInt()
        for (i in 0 until numSensors) {
            sensorStatus[EmpaSensorType.values()[`in`.readInt()]] = `in`.readString()!!
        }
    }

    override fun hasAcceleration(): Boolean {
        return true
    }

    override fun getAcceleration(): FloatArray {
        return acceleration
    }

    @Synchronized
    fun setAcceleration(x: Float, y: Float, z: Float) {
        this.acceleration[0] = x
        this.acceleration[1] = y
        this.acceleration[2] = z
    }

    override fun getBatteryLevel(): Float {
        return batteryLevel
    }

    @Synchronized
    fun setBatteryLevel(batteryLevel: Float) {
        this.batteryLevel = batteryLevel
    }

    override fun hasHeartRate(): Boolean {
        return true
    }

    override fun getHeartRate(): Float {
        return 60 / interBeatInterval
    }

    override fun hasTemperature(): Boolean {
        return true
    }

    override fun getTemperature(): Float {
        return temperature
    }

    @Synchronized
    fun setTemperature(temperature: Float) {
        this.temperature = temperature
    }

    fun getSensorStatus(): Map<EmpaSensorType, String> {
        return sensorStatus
    }

    @Synchronized
    fun setSensorStatus(type: EmpaSensorType, status: String) {
        sensorStatus[type] = status
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<E4DeviceStatus> = DeviceStateCreator(E4DeviceStatus::class.java)
    }
}
