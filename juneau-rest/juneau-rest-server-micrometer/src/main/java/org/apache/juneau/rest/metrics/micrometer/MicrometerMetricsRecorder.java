/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.metrics.micrometer;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.time.*;

import org.apache.juneau.rest.metrics.*;

import io.micrometer.core.instrument.*;

/**
 * {@link MetricsRecorder} bridge that records every {@code @RestOp} call as a Micrometer
 * {@link Timer} sample on the configured {@link MeterRegistry}.
 *
 * <h5 class='topic'>Metric shape</h5>
 *
 * <p>
 * Each call records to one Timer named (by default) {@value #DEFAULT_TIMER_NAME} with four tags:
 * <ul>
 * 	<li><b>{@code method}</b> &mdash; uppercased HTTP method (e.g. {@code GET}, {@code POST}).
 * 	<li><b>{@code uri}</b> &mdash; the {@code @RestOp} path template (e.g. {@code /users/{id}}), not
 * 		the raw concrete URI. Using the template keeps tag cardinality bounded; raw URIs
 * 		({@code /users/123}, {@code /users/124}, ...) would explode the registry's series count.
 * 		Empty string when the operation has no path pattern.
 * 	<li><b>{@code status}</b> &mdash; HTTP response status code as a string (e.g. {@code 200},
 * 		{@code 404}, {@code 500}).
 * 	<li><b>{@code exception}</b> &mdash; the thrown exception's simple-name (e.g. {@code BadRequest},
 * 		{@code NullPointerException}), or {@value #NO_EXCEPTION_TAG} when the call completed normally.
 * </ul>
 *
 * <p>
 * The shape mirrors Spring Boot's
 * <a class="doclink" href="https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#actuator.metrics.supported.spring-mvc">{@code WebMvcMetricsFilter}</a>
 * convention so existing dashboards / alerts that scrape {@code http.server.requests} from Spring
 * Boot services keep working when those services are reimplemented on Juneau.
 *
 * <h5 class='topic'>Wiring</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> ObservabilityConfig {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> MeterRegistry meterRegistry() {
 * 			<jk>return new</jk> PrometheusMeterRegistry(PrometheusConfig.<jsf>DEFAULT</jsf>);
 * 		}
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> MetricsRecorder metricsRecorder(MeterRegistry <jv>r</jv>) {
 * 			<jk>return new</jk> MicrometerMetricsRecorder(<jv>r</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The {@code MetricsRecorder} bean is picked up by {@code juneau-rest-server} through the
 * {@code RestContext}'s bean store and invoked once per {@code @RestOp} call.
 *
 * <h5 class='topic'>Customizing the metric name</h5>
 *
 * <p>
 * Pass a non-default name to the two-arg constructor when the default
 * {@value #DEFAULT_TIMER_NAME} doesn't match an existing dashboard / Prometheus rule set:
 *
 * <p class='bjava'>
 * 	<jk>new</jk> MicrometerMetricsRecorder(<jv>r</jv>, <js>"my.service.http.timer"</js>)
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MetricsRecorder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * 	<li class='link'><a class="doclink" href="https://micrometer.io/docs/concepts">Micrometer Concepts</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class MicrometerMetricsRecorder implements MetricsRecorder {

	/** Default timer name {@code "http.server.requests"} &mdash; matches Spring Boot's convention. */
	public static final String DEFAULT_TIMER_NAME = "http.server.requests";

	/** Tag value used for the {@code exception} tag when the call completed normally. */
	public static final String NO_EXCEPTION_TAG = "None";

	private static final String TAG_METHOD = "method";
	private static final String TAG_URI = "uri";
	private static final String TAG_STATUS = "status";
	private static final String TAG_EXCEPTION = "exception";

	private static final String ARG_registry = "registry";
	private static final String ARG_timerName = "timerName";

	private final MeterRegistry registry;
	private final String timerName;

	/**
	 * Constructor using the default timer name {@value #DEFAULT_TIMER_NAME}.
	 *
	 * @param registry The Micrometer {@link MeterRegistry} to publish timers to. Must not be <jk>null</jk>.
	 */
	public MicrometerMetricsRecorder(MeterRegistry registry) {
		this(registry, DEFAULT_TIMER_NAME);
	}

	/**
	 * Constructor with a custom timer name.
	 *
	 * @param registry The Micrometer {@link MeterRegistry} to publish timers to. Must not be <jk>null</jk>.
	 * @param timerName The timer name. Must not be <jk>null</jk> or blank.
	 */
	public MicrometerMetricsRecorder(MeterRegistry registry, String timerName) {
		this.registry = assertArgNotNull(ARG_registry, registry);
		this.timerName = assertArgNotNullOrBlank(ARG_timerName, timerName);
	}

	/**
	 * Returns the {@link MeterRegistry} this recorder publishes to.
	 *
	 * @return The {@link MeterRegistry}.
	 */
	public MeterRegistry getRegistry() { return registry; }

	/**
	 * Returns the configured timer name.
	 *
	 * @return The timer name.
	 */
	public String getTimerName() { return timerName; }

	@Override /* MetricsRecorder */
	public void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error) {
		Timer.builder(timerName)
			.tag(TAG_METHOD, defaultIfBlank(httpMethod, ""))
			.tag(TAG_URI, defaultIfBlank(uriTemplate, ""))
			.tag(TAG_STATUS, Integer.toString(statusCode))
			.tag(TAG_EXCEPTION, exceptionTag(error))
			.register(registry)
			.record(elapsed);
	}

	private static String exceptionTag(Throwable error) {
		return error == null ? NO_EXCEPTION_TAG : error.getClass().getSimpleName();
	}

	private static String defaultIfBlank(String value, String fallback) {
		return (value == null || value.isEmpty()) ? fallback : value;
	}
}
