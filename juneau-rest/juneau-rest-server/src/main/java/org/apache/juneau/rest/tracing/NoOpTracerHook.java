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
package org.apache.juneau.rest.tracing;

import org.apache.juneau.rest.*;

/**
 * Default {@link TracerHook} implementation that opens no spans and records nothing.
 *
 * <p>
 * Used whenever no consumer-provided {@code TracerHook} bean is reachable from the
 * {@code RestContext}'s bean store. Ensures the {@link TracerHook} contract is always satisfied
 * &mdash; the framework can call {@link #startSpan(RestRequest)} unconditionally without a
 * null-check, and the returned {@link NoOpScope} satisfies the close-in-finally contract with
 * zero allocations.
 *
 * <p>
 * Both {@link #INSTANCE} and {@link NoOpScope#INSTANCE} are process-wide singletons; the
 * {@link #startSpan(RestRequest)} method returns the same scope reference for every call and JIT-inlines
 * the no-op setters / close to empty method bodies.
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S6548" // Intentional process-wide no-op singleton; the Singleton pattern is required to satisfy the TracerHook SPI contract with zero allocation.
})
public final class NoOpTracerHook implements TracerHook {

	/** Process-wide singleton instance. */
	public static final NoOpTracerHook INSTANCE = new NoOpTracerHook();

	private NoOpTracerHook() {}

	@Override /* TracerHook */
	public Scope startSpan(RestRequest request) {
		return NoOpScope.INSTANCE;
	}

	/**
	 * No-op {@link Scope} returned by {@link NoOpTracerHook}.
	 *
	 * <p>
	 * Drops every status, error, and close transition on the floor. Public so user code that wants to
	 * fall back to the no-op behaviour from a custom {@link TracerHook} (for example, when the
	 * incoming trace context is unparseable) can return {@link #INSTANCE} instead of implementing a
	 * private equivalent.
	 */
	@SuppressWarnings({
		"resource", // Singleton; intentionally held for the process lifetime.
		"java:S6548" // Intentional process-wide no-op singleton returned for every span; the Singleton pattern is required for the zero-allocation no-op scope.
	})
	public static final class NoOpScope implements Scope {

		/** Process-wide singleton instance. */
		public static final NoOpScope INSTANCE = new NoOpScope();

		private NoOpScope() {}

		@Override /* Scope */
		public void setStatusCode(int statusCode) {
			// Intentionally empty.
		}

		@Override /* Scope */
		public void setError(Throwable error) {
			// Intentionally empty.
		}

		@Override /* Scope */
		public void close() {
			// Intentionally empty.
		}
	}
}
