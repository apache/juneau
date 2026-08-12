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
 * Direct unit tests for {@link TraceContextCarrierTextMapSetter} against a minimal in-memory
 * {@link TraceContextCarrier} fake &mdash; the null-argument permutations of its compound guard are
 * only ever exercised end-to-end with all three arguments non-null (a real propagator never calls
 * {@code set(...)} with a null key/value), so each is covered directly here.
 */
class TraceContextCarrierTextMapSetter_Test extends TestBase {

	private static final class MapCarrier implements TraceContextCarrier {
		final Map<String,String> map = new LinkedHashMap<>();
		@Override public String get(String key) { return map.get(key); }
		@Override public Iterable<String> keys() { return map.keySet(); }
		@Override public void set(String key, String value) { map.put(key, value); }
	}

	@Test void a01_nullCarrier_isNoOp() {
		assertDoesNotThrow(() -> TraceContextCarrierTextMapSetter.INSTANCE.set(null, "k", "v"));
	}

	@Test void a02_nullKey_isNoOp() {
		var carrier = new MapCarrier();
		TraceContextCarrierTextMapSetter.INSTANCE.set(carrier, null, "v");
		assertTrue(carrier.map.isEmpty());
	}

	@Test void a03_nullValue_isNoOp() {
		var carrier = new MapCarrier();
		TraceContextCarrierTextMapSetter.INSTANCE.set(carrier, "k", null);
		assertTrue(carrier.map.isEmpty());
	}

	@Test void a04_allNonNull_writesToCarrier() {
		var carrier = new MapCarrier();
		TraceContextCarrierTextMapSetter.INSTANCE.set(carrier, "traceparent", "00-x-y-01");
		assertEquals("00-x-y-01", carrier.map.get("traceparent"));
	}

	@Test void a05_singleton_isStable() {
		assertSame(TraceContextCarrierTextMapSetter.INSTANCE, TraceContextCarrierTextMapSetter.INSTANCE);
	}
}
