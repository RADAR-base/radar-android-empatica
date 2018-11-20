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

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Bundle
import org.radarcns.android.device.DeviceServiceProvider
import java.util.*

class E4ServiceProvider : DeviceServiceProvider<E4DeviceStatus>() {
    override fun getServiceClass() = E4Service::class.java

    override fun configure(bundle: Bundle) {
        super.configure(bundle)
        config.putExtras(bundle, EMPATICA_API_KEY)
    }

    override fun needsPermissions(): List<String> {
        return Arrays.asList(ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN)
    }

    override fun needsFeatures(): List<String> {
        return Arrays.asList(PackageManager.FEATURE_BLUETOOTH, PackageManager.FEATURE_BLUETOOTH_LE)
    }

    override fun getDescription(): String = radarService.getString(R.string.empatica_e4_explanation)
    override fun hasDetailView() = true
    override fun getDisplayName(): String = radarService.getString(R.string.empaticaE4DisplayName)
    override fun isFilterable() = true

    override fun getDeviceProducer() = "Empatica"
    override fun getDeviceModel() = "E4"
    override fun getVersion() = BuildConfig.VERSION_NAME

    companion object {
        const val EMPATICA_API_KEY = "empatica_api_key"
    }
}
