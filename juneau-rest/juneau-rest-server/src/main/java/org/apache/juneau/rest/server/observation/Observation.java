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

/**
 * Handle to one in-flight custom (non-request) observation.
 *
 * <p>
 * Returned by {@link Observer#start(String, String)} and by the {@link Observations} facade. An
 * observation pairs a metric timer with a tracing span around an arbitrary block of work that has no
 * associated HTTP request &mdash; the request-path equivalent is the framework-driven boundary in
 * {@code RestOpInvoker}.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p>
 * The handle is {@link AutoCloseable} and is designed for try-with-resources. On {@link #close()} it
 * records the elapsed time to the metrics recorder and ends the span. Mark a failure with
 * {@link #setError(Throwable)} before the block exits so both the timer's {@code exception} tag and the
 * span's error status reflect it:
 *
 * <p class='bjava'>
 * 	<jk>try</jk> (Observation <jv>o</jv> = Observations.<jsm>observe</jsm>(<js>"order.load"</js>, <js>"team=payments"</js>)) {
 * 		<jk>return</jk> loadOrder(<jv>id</jv>);
 * 	} <jk>catch</jk> (RuntimeException <jv>e</jv>) {
 * 		<jv>o</jv>.setError(<jv>e</jv>);
 * 		<jk>throw</jk> <jv>e</jv>;
 * 	}
 * </p>
 *
 * <h5 class='topic'>No-backend contract</h5>
 *
 * <p>
 * When no metrics recorder / tracer is installed, {@link Observations} hands back
 * {@link #NOOP} &mdash; a process-wide singleton whose methods do nothing and whose timing is never
 * taken, so an observation in an unconfigured process costs nothing beyond the try-with-resources
 * scaffolding.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // NOOP is an intentional stateless no-op singleton held in a static field; its close() is empty, so there is nothing to leak.
})
public interface Observation extends AutoCloseable {

	/**
	 * Process-wide no-op observation, returned whenever no backend is installed.
	 *
	 * <p>
	 * Drops the error on the floor and does nothing on close.
	 */
	Observation NOOP = new Observation() {
		@Override /* Observation */
		public void setError(Throwable error) {
			// Intentionally empty.
		}

		@Override /* Observation */
		public void close() {
			// Intentionally empty.
		}
	};

	/**
	 * Marks the observed block as failed.
	 *
	 * <p>
	 * The throwable is surfaced as the metric timer's {@code exception} tag and recorded on the tracing
	 * span (status {@code ERROR} + {@code exception.type} attribute) when the observation closes. Safe
	 * to call at most once; a {@code null} argument is treated as "no error".
	 *
	 * @param error The exception that caused the block to fail. May be <jk>null</jk> (treated as success).
	 */
	void setError(Throwable error);

	/**
	 * Closes the observation &mdash; records the elapsed timer sample and ends the span.
	 *
	 * <p>
	 * Always called via the try-with-resources {@code finally} contract. Implementations must not throw.
	 */
	@Override /* AutoCloseable */
	void close();
}
