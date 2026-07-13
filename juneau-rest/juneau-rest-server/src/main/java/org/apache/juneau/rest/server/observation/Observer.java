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
package org.apache.juneau.rest.server.observation;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;

import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.tracing.*;

/**
 * Factory for custom (non-request) {@link Observation}s, composing a {@link MetricsRecorder} and a
 * {@link TracerHook}.
 *
 * <p>
 * This is the explicit programmatic observation substrate for {@code juneau-rest-server}: it reuses the
 * exact same two SPIs the request path uses ({@link MetricsRecorder}, {@link TracerHook}) and the same
 * {@code metricName} / {@code metricTags} comma-separated tag model, but is decoupled from
 * {@code RestRequest} so application code can instrument an arbitrary block of work. Juneau deliberately
 * has no method-interception / AOP substrate, so observation is explicit (a try-with-resources block)
 * rather than annotation-driven &mdash; consistent with the framework's explicit-over-magic philosophy.
 *
 * <h5 class='topic'>No-backend contract</h5>
 *
 * <p>
 * An {@code Observer} built from the no-op SPIs ({@link #NOOP}) never takes a timestamp and hands back
 * {@link Observation#NOOP}, so an observation in an unconfigured process costs nothing beyond the
 * try-with-resources scaffolding. {@link #start(String, String)} short-circuits to the no-op handle
 * whenever <i>both</i> backends are the no-op singletons.
 *
 * <h5 class='topic'>Wiring</h5>
 *
 * <p>
 * Typically obtained from a {@code RestContext}'s bean store &mdash; register the same
 * {@code @Bean MetricsRecorder} / {@code @Bean TracerHook} you already register for request-path
 * observability, then build an {@code Observer} from them (or install one as the {@link Observations}
 * default). Construct directly for tests:
 *
 * <p class='bjava'>
 * 	Observer <jv>o</jv> = <jk>new</jk> Observer(<jk>new</jk> MicrometerMetricsRecorder(<jv>registry</jv>), <jk>new</jk> OtelTracerHook(<jv>otel</jv>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Observations}
 * 	<li class='jc'>{@link Observation}
 * 	<li class='jc'>{@link MetricsRecorder}
 * 	<li class='jc'>{@link TracerHook}
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // start(...) returns the Observation for the caller's try-with-resources; ActiveObservation holds the tracer Scope it closes in its own close().
})
public final class Observer {

	/**
	 * Observer backed by the no-op metrics recorder and tracer &mdash; produces only
	 * {@link Observation#NOOP}.
	 */
	public static final Observer NOOP = new Observer(NoOpMetricsRecorder.INSTANCE, NoOpTracerHook.INSTANCE);

	// Constant name intentionally uses camelCase to match observation/metrics naming conventions.
	@SuppressWarnings("java:S115")
	private static final String ARG_recorder = "recorder";
	// Constant name intentionally uses camelCase to match observation/metrics naming conventions.
	@SuppressWarnings("java:S115")
	private static final String ARG_tracer = "tracer";
	// Constant name intentionally uses camelCase to match observation/metrics naming conventions.
	@SuppressWarnings("java:S115")
	private static final String ARG_name = "name";

	private final MetricsRecorder recorder;
	private final TracerHook tracer;
	private final boolean active;

	/**
	 * Constructor.
	 *
	 * @param recorder The metrics recorder to time observations against. Must not be <jk>null</jk>;
	 * 	use {@link NoOpMetricsRecorder#INSTANCE} to disable metrics.
	 * @param tracer The tracer to open spans against. Must not be <jk>null</jk>; use
	 * 	{@link NoOpTracerHook#INSTANCE} to disable tracing.
	 */
	public Observer(MetricsRecorder recorder, TracerHook tracer) {
		this.recorder = assertArgNotNull(ARG_recorder, recorder);
		this.tracer = assertArgNotNull(ARG_tracer, tracer);
		this.active = recorder != NoOpMetricsRecorder.INSTANCE || tracer != NoOpTracerHook.INSTANCE;
	}

	/**
	 * Starts a new observation.
	 *
	 * <p>
	 * When both backends are the no-op singletons this returns {@link Observation#NOOP} without taking a
	 * timestamp or opening a span. Otherwise it opens a span via {@link TracerHook#startSpan(String)} and
	 * captures a start timestamp; the paired timer sample is recorded on {@link Observation#close()}.
	 *
	 * @param name The observation name &mdash; used as both the span name and the metric timer name.
	 * 	Must not be <jk>null</jk> or blank.
	 * @param tags Additional metric tags as comma-separated {@code key=value} pairs (e.g.
	 * 	{@code "team=payments,region=us-east"}), or <jk>null</jk> / empty for none.
	 * @return The started {@link Observation}. Never <jk>null</jk>.
	 */
	public Observation start(String name, String tags) {
		assertArgNotNullOrBlank(ARG_name, name);
		if (! active)
			return Observation.NOOP;
		var scope = tracer.startSpan(name);
		return new ActiveObservation(recorder, scope, name, tags == null ? "" : tags);
	}

	/**
	 * Returns whether this observer has at least one live backend (metrics or tracing).
	 *
	 * @return <jk>true</jk> if either the metrics recorder or the tracer is non-no-op.
	 */
	public boolean isActive() {
		return active;
	}

	private static final class ActiveObservation implements Observation {

		private final MetricsRecorder recorder;
		private final Scope scope;
		private final String name;
		private final String tags;
		private final long startNanos;
		private Throwable error;

		ActiveObservation(MetricsRecorder recorder, Scope scope, String name, String tags) {
			this.recorder = recorder;
			this.scope = scope;
			this.name = name;
			this.tags = tags;
			this.startNanos = System.nanoTime();
		}

		@Override /* Observation */
		public void setError(Throwable error) {
			this.error = error;
		}

		@Override /* Observation */
		public void close() {
			var elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
			try {
				if (nn(error))
					scope.setError(error);
			} finally {
				try {
					scope.close();
				} finally {
					recorder.record(name, tags, elapsed, error);
				}
			}
		}
	}
}
