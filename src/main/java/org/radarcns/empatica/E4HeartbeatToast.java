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

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import org.radarcns.android.device.DeviceServiceConnection;
import org.radarcns.android.util.Boast;
import org.radarcns.data.Record;
import org.radarcns.kafka.ObservationKey;
import org.radarcns.passive.empatica.EmpaticaE4InterBeatInterval;
import org.radarcns.topic.AvroTopic;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Shows recently collected heartbeats in a Toast.
 */
public class E4HeartbeatToast extends
        AsyncTask<Void, Void, String> {
    private final Context context;
    private final DeviceServiceConnection<E4DeviceStatus> connection;
    private static final DecimalFormat singleDecimal = new DecimalFormat("0.0");

    public E4HeartbeatToast(Context context, DeviceServiceConnection<E4DeviceStatus> connection) {
        this.context = context;
        this.connection = connection;
    }

    @Override
    @SafeVarargs
    protected final String doInBackground(Void... params) {
        AvroTopic<ObservationKey, EmpaticaE4InterBeatInterval> topic = E4DeviceManager.interBeatIntervalTopic;
        if (topic != null) {
            try {
                List<Record<ObservationKey, EmpaticaE4InterBeatInterval>> measurements =
                        connection.getRecords(topic, 2);

                if (!measurements.isEmpty()) {
                    StringBuilder sb = new StringBuilder(64);
                    for (Record<ObservationKey, EmpaticaE4InterBeatInterval> measurement : measurements) {
                        long timeMs = Math.round(1000d * measurement.value.getTimeReceived());
                        double diffTime = (System.currentTimeMillis() - timeMs) / 1000d;
                        sb.append(singleDecimal.format(diffTime));
                        sb.append(" sec. ago: ");
                        sb.append(singleDecimal.format(60d / measurement.value.getInterBeatInterval()));
                        sb.append(" bpm\n");
                    }
                    return sb.toString();
                }
            } catch (IOException ignore) {
            }
        }
        return "No heart rate collected yet.";
    }


    @Override
    protected void onPostExecute(String s) {
        Boast.makeText(context, s, Toast.LENGTH_LONG).show();
    }
}
