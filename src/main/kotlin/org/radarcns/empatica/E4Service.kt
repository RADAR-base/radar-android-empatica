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

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.empatica.empalink.EmpaDeviceManager
import org.radarcns.android.RadarConfiguration
import org.radarcns.android.device.DeviceService
import org.radarcns.empatica.E4ServiceProvider.Companion.EMPATICA_API_KEY
import org.slf4j.LoggerFactory

/**
 * A service that manages a E4DeviceManager and a TableDataHandler to send store the data of an
 * Empatica E4 and send it to a Kafka REST proxy.
 */
class E4Service : DeviceService<E4DeviceStatus>() {
    private lateinit var apiKey: String
    private lateinit var mHandlerThread: HandlerThread
    private lateinit var mHandler: Handler

    private lateinit var empaManager: EmpaDeviceManager

    override fun onCreate() {
        super.onCreate()
        mHandlerThread = HandlerThread("E4-device-handler", Process.THREAD_PRIORITY_MORE_FAVORABLE)
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)

        val delegate = E4Delegate(this)
        empaManager = EmpaDeviceManager(this, delegate, delegate, delegate)
    }

    override fun getDeviceManager(): E4DeviceManager? = super.getDeviceManager() as? E4DeviceManager

    override fun createDeviceManager() = E4DeviceManager(this, empaManager, mHandler, apiKey)

    override fun getDefaultState() = E4DeviceStatus()

    override fun onInvocation(bundle: Bundle) {
        super.onInvocation(bundle)
        apiKey = RadarConfiguration.getStringExtra(bundle, EMPATICA_API_KEY)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            empaManager.cleanUp()
        } catch (ex: RuntimeException) {
            logger.error("Failed to clean up Empatica manager", ex)
        }
        mHandlerThread.quit()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(E4Service::class.java)
    }
}
