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
import org.radarcns.android.device.DeviceService;
import org.radarcns.kafka.ObservationKey;
import org.radarcns.topic.AvroTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.radarcns.android.RadarConfiguration.EMPATICA_API_KEY;

/**
 * A service that manages a E4DeviceManager and a TableDataHandler to send store the data of an
 * Empatica E4 and send it to a Kafka REST proxy.
 */
public class E4Service extends DeviceService<E4DeviceStatus> {
    private static final Logger logger = LoggerFactory.getLogger(E4Service.class);
    private String apiKey;

    @Override
    protected E4DeviceManager createDeviceManager() {
        return new E4DeviceManager(this, apiKey);
    }

    @Override
    protected E4DeviceStatus getDefaultState() {
        return new E4DeviceStatus();
    }

    @Override
    protected E4Topics getTopics() {
        return E4Topics.getInstance();
    }

    @Override
    protected List<AvroTopic<ObservationKey, ? extends SpecificRecord>> getCachedTopics() {
        E4Topics topics = getTopics();
        return Arrays.<AvroTopic<ObservationKey, ? extends SpecificRecord>>asList(
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
        }
    }
}
