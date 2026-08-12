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
package org.apache.juneau.rest.server.tracing.otel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@link TraceContextCarrierTextMapGetter} against a minimal in-memory
 * {@link TraceContextCarrier} fake &mdash; {@code keys()} in particular is never invoked by the
 * OpenTelemetry SDK's own W3C propagators (they look up known keys directly), so it's otherwise
 * untested by {@link OtelTracerHook_Test}'s end-to-end fixtures.
 */
class TraceContextCarrierTextMapGetter_Test extends TestBase {

	private static final class MapCarrier implements TraceContextCarrier {
		final Map<String,String> map;
		MapCarrier(Map<String,String> map) { this.map = map; }
		@Override public String get(String key) { return map.get(key); }
		@Override public Iterable<String> keys() { return map.keySet(); }
		@Override public void set(String key, String value) { map.put(key, value); }
	}

	/** A carrier that violates the "never null" javadoc contract for {@link TraceContextCarrier#keys()}. */
	private static final class NullKeysCarrier implements TraceContextCarrier {
		@Override public String get(String key) { return null; }
		@Override public Iterable<String> keys() { return null; }
		@Override public void set(String key, String value) { /* unused by this fixture */ }
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: keys(...).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_keys_nullCarrier_returnsEmptyList() {
		var keys = TraceContextCarrierTextMapGetter.INSTANCE.keys(null);
		assertNotNull(keys);
		assertFalse(keys.iterator().hasNext());
	}

	@Test void a02_keys_carrierReturnsNull_returnsEmptyList() {
		var keys = TraceContextCarrierTextMapGetter.INSTANCE.keys(new NullKeysCarrier());
		assertNotNull(keys);
		assertFalse(keys.iterator().hasNext());
	}

	@Test void a03_keys_delegatesToCarrier() {
		var carrier = new MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "v1", "tracestate", "v2")));
		var keys = TraceContextCarrierTextMapGetter.INSTANCE.keys(carrier);
		var seen = new LinkedHashSet<String>();
		keys.forEach(seen::add);
		assertEquals(Set.of("traceparent", "tracestate"), seen);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: get(...).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_get_nullCarrier_returnsNull() {
		assertNull(TraceContextCarrierTextMapGetter.INSTANCE.get(null, "traceparent"));
	}

	@Test void b02_get_nullKey_returnsNull() {
		var carrier = new MapCarrier(new LinkedHashMap<>());
		assertNull(TraceContextCarrierTextMapGetter.INSTANCE.get(carrier, null));
	}

	@Test void b03_get_absentKey_returnsNull() {
		var carrier = new MapCarrier(new LinkedHashMap<>());
		assertNull(TraceContextCarrierTextMapGetter.INSTANCE.get(carrier, "traceparent"));
	}

	@Test void b04_get_emptyValue_returnsNull() {
		var carrier = new MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "")));
		assertNull(TraceContextCarrierTextMapGetter.INSTANCE.get(carrier, "traceparent"));
	}

	@Test void b05_get_presentValue_returnsValue() {
		var carrier = new MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "00-x-y-01")));
		assertEquals("00-x-y-01", TraceContextCarrierTextMapGetter.INSTANCE.get(carrier, "traceparent"));
	}

	@Test void b06_singleton_isStable() {
		assertSame(TraceContextCarrierTextMapGetter.INSTANCE, TraceContextCarrierTextMapGetter.INSTANCE);
	}
}
