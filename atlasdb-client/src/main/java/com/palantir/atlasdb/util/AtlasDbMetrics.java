/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.atlasdb.util;

import com.codahale.metrics.MetricRegistry;
import com.github.benmanes.caffeine.cache.Cache;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import com.palantir.tritium.api.event.InstrumentationFilter;
import com.palantir.tritium.event.InstrumentationFilters;
import com.palantir.tritium.event.InvocationContext;
import com.palantir.tritium.event.InvocationEventHandler;
import com.palantir.tritium.metrics.caffeine.CaffeineCacheStats;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import com.palantir.tritium.proxy.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AtlasDbMetrics {
    private static final SafeLogger log = SafeLoggerFactory.get(AtlasDbMetrics.class);

    private AtlasDbMetrics() {}

    public static <T, U extends T> T instrumentTimed(
            MetricRegistry metricRegistry, Class<T> serviceInterface, U service) {
        return instrument(metricRegistry, serviceInterface, service, serviceInterface.getName(), instrumentTimedOnly());
    }

    /**
     * Instruments an instance of the provided service interface, registering timers for only the methods annotated
     * with {@link com.palantir.atlasdb.metrics.Timed}.
     */
    public static <T, U extends T> T instrumentTimed(
            MetricRegistry metricRegistry, Class<T> serviceInterface, U service, String name) {
        return instrument(metricRegistry, serviceInterface, service, name, instrumentTimedOnly());
    }

    /**
     * @deprecated use {@link #instrumentTimed(MetricRegistry, Class, Object)}
     */
    @Deprecated
    public static <T, U extends T> T instrument(MetricRegistry metricRegistry, Class<T> serviceInterface, U service) {
        return instrument(metricRegistry, serviceInterface, service, serviceInterface.getName());
    }

    /**
     * @deprecated use {@link #instrumentTimed(MetricRegistry, Class, Object, String)}
     */
    @Deprecated
    public static <T, U extends T> T instrument(
            MetricRegistry metricRegistry, Class<T> serviceInterface, U service, String name) {
        return instrument(metricRegistry, serviceInterface, service, name, instrumentAllMethods());
    }

    public static <T, U extends T> T instrumentWithTaggedMetrics(
            TaggedMetricRegistry taggedMetrics, Class<T> serviceInterface, U service) {
        return Instrumentation.builder(serviceInterface, service)
                .withHandler(
                        new TaggedMetricsInvocationEventHandler(taggedMetrics, MetricRegistry.name(serviceInterface)))
                .build();
    }

    public static <T, U extends T> T instrumentWithTaggedMetrics(
            TaggedMetricRegistry taggedMetrics,
            Class<T> serviceInterface,
            U service,
            Function<InvocationContext, Map<String, String>> tagFunction) {
        return Instrumentation.builder(serviceInterface, service)
                .withHandler(taggedMetricsHandler(taggedMetrics, serviceInterface, tagFunction))
                .build();
    }

    public static <T> InvocationEventHandler<InvocationContext> taggedMetricsHandler(
            TaggedMetricRegistry taggedMetrics,
            Class<T> serviceInterface,
            Function<InvocationContext, Map<String, String>> tagFunction) {
        return new TaggedMetricsInvocationEventHandler(
                taggedMetrics, MetricRegistry.name(serviceInterface), tagFunction);
    }

    public static void registerCache(MetricRegistry metricRegistry, Cache<?, ?> cache, String metricsPrefix) {
        Set<String> existingMetrics = metricRegistry.getMetrics().keySet().stream()
                .filter(name -> name.startsWith(metricsPrefix))
                .collect(Collectors.toSet());
        if (existingMetrics.isEmpty()) {
            CaffeineCacheStats.registerCache(metricRegistry, cache, metricsPrefix);
        } else {
            log.info(
                    "Not registering cache with prefix '{}' as metric registry already contains metrics: {}",
                    SafeArg.of("metricsPrefix", metricsPrefix),
                    SafeArg.of("existingMetrics", existingMetrics));
        }
    }

    private static <T, U extends T> T instrument(
            MetricRegistry metricRegistry,
            Class<T> serviceInterface,
            U service,
            String name,
            InstrumentationFilter instrumentationFilter) {
        return Instrumentation.builder(serviceInterface, service)
                .withFilter(instrumentationFilter)
                .withHandler(new SlidingWindowMetricsInvocationHandler(metricRegistry, name))
                .build(); // Ok
    }

    private static InstrumentationFilter instrumentTimedOnly() {
        return new TimedOnlyInstrumentationFilter();
    }

    private static InstrumentationFilter instrumentAllMethods() {
        return InstrumentationFilters.INSTRUMENT_ALL;
    }
}
