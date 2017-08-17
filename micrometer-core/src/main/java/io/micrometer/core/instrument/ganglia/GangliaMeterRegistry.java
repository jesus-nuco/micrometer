/**
 * Copyright 2017 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.ganglia;

import com.codahale.metrics.ganglia.GangliaReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.TagFormatter;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GangliaMeterRegistry extends DropwizardMeterRegistry {
    private final GangliaReporter reporter;
    private final GangliaConfig config;

    public GangliaMeterRegistry() {
        this(System::getProperty);
    }

    public GangliaMeterRegistry(GangliaConfig config) {
        this(config, HierarchicalNameMapper.DEFAULT, Clock.SYSTEM);
    }

    public GangliaMeterRegistry(GangliaConfig config, HierarchicalNameMapper nameMapper, Clock clock) {
        // Technically, Ganglia doesn't have any constraints on metric or tag names, but the encoding of Unicode can look
        // horrible in the UI. So be aware...
        super(nameMapper, clock, TagFormatter.identity);
        this.config = config;

        try {
            final GMetric ganglia = new GMetric(config.host(), config.port(), config.addressingMode(), config.ttl());
            this.reporter = GangliaReporter.forRegistry(getDropwizardRegistry())
                    .convertRatesTo(config.rateUnits())
                    .convertDurationsTo(config.durationUnits())
                    .build(ganglia);
            start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to configure Ganglia metrics reporting", e);
        }
    }

    public void stop() {
        this.reporter.stop();
    }

    public void start() {
        this.reporter.start(config.step().getSeconds(), TimeUnit.SECONDS);
    }
}
