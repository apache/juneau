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
package org.apache.juneau.rest.server.metrics;

import java.time.*;

/**
 * SPI for receiving per-request metric events from {@code juneau-rest-server}.
 *
 * <p>
 * One {@code record(...)} call fires after every {@code @RestOp} handler invocation &mdash; including
 * exception paths. Bridge implementations translate the event into the format their downstream
 * registry expects (Micrometer {@code io.micrometer.core.instrument.Timer}, OpenTelemetry
 * metric instruments, Dropwizard {@code MetricRegistry}, etc.).
 *
 * <h5 class='topic'>Off-by-default contract</h5>
 *
 * <p>
 * {@code juneau-rest-server} resolves the {@code MetricsRecorder} via
 * {@code RestContext.getBeanStore().getBean(MetricsRecorder.class)}. When no bean is supplied,
 * {@link NoOpMetricsRecorder#INSTANCE} is used and the framework never allocates a downstream
 * registry, never reaches Micrometer / OpenTelemetry on its own, and never adds any per-request
 * cost beyond a single static-field read. To opt in, the consumer registers a {@code MetricsRecorder}
 * bean &mdash; typically via {@code @Bean MetricsRecorder} on the resource or its parent.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> MetricsRecorder recorder(MeterRegistry <jv>registry</jv>) {
 * 			<jk>return new</jk> MicrometerMetricsRecorder(<jv>registry</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Threading</h5>
 *
 * <p>
 * Implementations <b>must</b> be thread-safe &mdash; the same {@code MetricsRecorder} bean is invoked
 * concurrently from every request thread for the lifetime of the {@code RestContext}. The framework
 * calls {@code record(...)} synchronously inside the request thread immediately after the response
 * status is known, so implementations should keep the call cheap (constant-time tag computation,
 * non-blocking registry writes).
 *
 * <h5 class='topic'>Event ordering</h5>
 *
 * <p>
 * For a given {@code @RestOp} invocation, {@code record(...)} fires exactly once after the operation
 * completes &mdash; whether the handler returned normally, threw a {@code BasicHttpException}, or
 * triggered an internal-server-error. The {@code elapsed} parameter measures wall-clock time spent
 * inside the framework's per-op invocation boundary (from just before parameter resolution to just
 * after the handler returns or throws). It does <b>not</b> include response serialization performed
 * later in the request pipeline.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link NoOpMetricsRecorder}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.tracing.TracerHook}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface MetricsRecorder {

	/**
	 * Records one per-request metric event.
	 *
	 * @param opName The fully-qualified {@code @RestOp} method name (e.g.
	 * 	{@code "com.example.OrderResource.create(java.lang.String,com.example.Order)"}).
	 * 	Never <jk>null</jk>; never blank.
	 * @param httpMethod The uppercased HTTP method of the request (e.g. {@code "GET"}, {@code "POST"}).
	 * 	Never <jk>null</jk>; never blank.
	 * @param uriTemplate The {@code @RestOp} path template (e.g. {@code "/users/{id}"}), <b>not</b> the
	 * 	raw concrete URI. Bridges should use this as the {@code uri} tag to keep cardinality bounded.
	 * 	May be empty for resource-level operations that match {@code /*}; never <jk>null</jk>.
	 * @param statusCode The HTTP response status code (e.g. {@code 200}, {@code 404}, {@code 500}).
	 * 	If the handler threw before the status was set, this is the framework's inferred status (typically
	 * 	{@code 500} for an unmapped exception, or the exception's status for a {@code BasicHttpException}).
	 * @param elapsed Wall-clock duration of the {@code @RestOp} invocation. Never <jk>null</jk>; never
	 * 	negative.
	 * @param error The exception thrown by the handler, or <jk>null</jk> if the call completed normally.
	 * 	Bridges typically derive an {@code exception} tag from the throwable's simple class name, or
	 * 	{@code "None"} when null (mirrors Spring Boot's {@code WebMvcMetricsFilter} convention).
	 * @param metricName Per-op metric name override from {@link org.apache.juneau.rest.server.RestOp#metricName()}.
	 * 	Empty string (default) means the implementation should use its own default name derivation.
	 * 	Never <jk>null</jk>.
	 * @param metricTags Per-op additional metric tags from {@link org.apache.juneau.rest.server.RestOp#metricTags()}.
	 * 	Format: comma-separated {@code key=value} pairs (e.g. {@code "team=payments,region=us-east"}).
	 * 	Empty string (default) means no additional tags. Never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S6213", // 'record' is the established SPI method name; renaming would break the public observability API.
		"java:S107"   // The 8 parameters form the stable per-request metric event contract; a holder object would obscure this SPI signature.
	})
	void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags);

	/**
	 * Records one custom (non-request) observation event.
	 *
	 * <p>
	 * Unlike {@link #record(String, String, String, int, Duration, Throwable, String, String)}, this
	 * entry point is <b>not</b> tied to an HTTP request &mdash; it is the substrate for the explicit
	 * programmatic observation API ({@link org.apache.juneau.rest.server.observation.Observations}) so
	 * application code can time an arbitrary block of work. Bridge implementations record a timer named
	 * {@code metricName} carrying {@code metricTags} (plus an {@code exception} tag derived from
	 * {@code error}).
	 *
	 * <p>
	 * The default implementation does nothing &mdash; a bridge that does not override it simply does not
	 * record custom observations, and the no-backend path stays zero-allocation. The shipped Micrometer
	 * bridge overrides it.
	 *
	 * @param metricName The timer name (e.g. {@code "order.load"}). Never <jk>null</jk>; never blank.
	 * @param metricTags Additional tags as comma-separated {@code key=value} pairs (e.g.
	 * 	{@code "team=payments,region=us-east"}). Empty string means no additional tags. Never <jk>null</jk>.
	 * @param elapsed Wall-clock duration of the observed block. Never <jk>null</jk>; never negative.
	 * @param error The exception thrown by the observed block, or <jk>null</jk> if it completed normally.
	 */
	@SuppressWarnings({
		"java:S6213" // 'record' is the established SPI method name; renaming would break the public observability API.
	})
	default void record(String metricName, String metricTags, Duration elapsed, Throwable error) {
		// Default no-op: bridges that support custom observations override this.
	}
}
