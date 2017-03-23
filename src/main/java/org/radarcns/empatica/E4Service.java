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

import android.os.Bundle;

import org.apache.avro.specific.SpecificRecord;
import org.radarcns.android.RadarConfiguration;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceManager;
import org.radarcns.android.device.DeviceService;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.android.device.DeviceTopics;
import org.radarcns.key.MeasurementKey;
import org.radarcns.topic.AvroTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.radarcns.android.RadarConfiguration.DEFAULT_GROUP_ID_KEY;
import static org.radarcns.android.RadarConfiguration.EMPATICA_API_KEY;

/**
 * A service that manages a E4DeviceManager and a TableDataHandler to send store the data of an
 * Empatica E4 and send it to a Kafka REST proxy.
 */
public class E4Service extends DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(E4Service.class);
    private E4Topics topics;
    private String apiKey;
    private String groupId;

    @Override
    public void onCreate() {
        logger.info("Creating E4 service {}", this);
        super.onCreate();

        topics = E4Topics.getInstance();
    }

    @Override
    protected DeviceManager createDeviceManager() {
        return new E4DeviceManager(this, this, apiKey, groupId, getDataHandler(), topics);
    }

    @Override
    protected BaseDeviceState getDefaultState() {
        E4DeviceStatus newStatus = new E4DeviceStatus();
        newStatus.setStatus(DeviceStatusListener.Status.DISCONNECTED);
        return newStatus;
    }

    @Override
    protected DeviceTopics getTopics() {
        return topics;
    }

    @Override
    protected List<AvroTopic<MeasurementKey, ? extends SpecificRecord>> getCachedTopics() {
        return Arrays.<AvroTopic<MeasurementKey, ? extends SpecificRecord>>asList(
                topics.getAccelerationTopic(), topics.getBloodVolumePulseTopic(),
                topics.getElectroDermalActivityTopic(), topics.getInterBeatIntervalTopic(),
                topics.getTemperatureTopic(), topics.getSensorStatusTopic());
    }

    @Override
    protected void onInvocation(Bundle bundle) {
        super.onInvocation(bundle);
        if (apiKey == null) {
            apiKey = RadarConfiguration.getStringExtra(bundle, EMPATICA_API_KEY);
            logger.info("Using API key {}", apiKey);
            groupId = RadarConfiguration.getStringExtra(bundle, DEFAULT_GROUP_ID_KEY);
        }
    }
}