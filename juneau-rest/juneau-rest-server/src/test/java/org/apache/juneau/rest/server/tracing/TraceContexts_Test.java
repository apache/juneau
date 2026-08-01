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
package org.apache.juneau.rest.server.tracing;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link TraceContexts#inject(TracerHook, TraceContextCarrier)} in isolation &mdash; it
 * null-checks its arguments and otherwise only delegates to {@link TracerHook#inject(TraceContextCarrier)},
 * so a default/no-op hook writes nothing to the carrier while a hook that overrides {@code inject(...)}
 * is invoked with the exact carrier instance passed in.
 */
class TraceContexts_Test extends TestBase {

	/** Records the carrier (if any) it was invoked with, and how many times. */
	private static final class RecordingCarrier implements TraceContextCarrier {
		final Map<String,String> written = new LinkedHashMap<>();

		@Override public String get(String key) { return null; }
		@Override public Iterable<String> keys() { return List.of(); }
		@Override public void set(String key, String value) { written.put(key, value); }
	}

	@Test void a01_nullTracer_throws() {
		var carrier = new RecordingCarrier();
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'tracer' cannot be null",
			() -> TraceContexts.inject(null, carrier));
	}

	@Test void a02_nullCarrier_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'carrier' cannot be null",
			() -> TraceContexts.inject(NoOpTracerHook.INSTANCE, null));
	}

	@Test void a03_defaultHook_writesNothing() {
		var carrier = new RecordingCarrier();
		TraceContexts.inject(NoOpTracerHook.INSTANCE, carrier);
		assertTrue(carrier.written.isEmpty(), "default/no-op TracerHook.inject(...) must not write anything");
	}

	@Test void a04_customHook_delegatesToTracerHookInject() {
		var calls = new AtomicInteger();
		var carrier = new RecordingCarrier();
		TracerHook hook = new TracerHook() {
			@Override public Scope startSpan(RestRequest request) { return NoOpTracerHook.NoOpScope.INSTANCE; }

			@Override public void inject(TraceContextCarrier c) {
				calls.incrementAndGet();
				assertSame(carrier, c, "TraceContexts.inject(...) must pass the exact carrier through");
				c.set("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
			}
		};

		TraceContexts.inject(hook, carrier);

		assertEquals(1, calls.get(), "TraceContexts.inject(...) must delegate exactly once");
		assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", carrier.written.get("traceparent"));
	}
}
