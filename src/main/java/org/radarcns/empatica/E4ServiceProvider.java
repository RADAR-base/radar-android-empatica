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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.radarcns.android.device.DeviceServiceProvider;

import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;

public class E4ServiceProvider extends DeviceServiceProvider<E4DeviceStatus> {
    public static final String EMPATICA_API_KEY = "empatica_api_key";

    @Override
    public Class<?> getServiceClass() {
        return E4Service.class;
    }

    @Override
    public String getDescription() {
        return getRadarService().getString(R.string.empatica_e4_explanation);
    }

    @Override
    public boolean hasDetailView() {
        return true;
    }

    @Override
    protected void configure(Bundle bundle) {
        super.configure(bundle);
        getConfig().putExtras(bundle, EMPATICA_API_KEY);
    }

    @Override
    public String getDisplayName() {
        return getRadarService().getString(R.string.empaticaE4DisplayName);
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @NonNull
    @Override
    public List<String> needsPermissions() {
        return Arrays.asList(ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN);
    }

    @NonNull
    @Override
    public List<String> needsFeatures() {
        return Arrays.asList(PackageManager.FEATURE_BLUETOOTH, PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @NonNull
    @Override
    public String getDeviceProducer() {
        return "Empatica";
    }

    @NonNull
    @Override
    public String getDeviceModel() {
        return "E4";
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }
}
