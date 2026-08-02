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
package org.apache.juneau.http.tracing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class TraceContextCarrier_Test {

	static final class A01_TestCarrier implements TraceContextCarrier {
		private final Map<String,String> map = new LinkedHashMap<>();
		@Override public String get(String key) { return map.get(key); }
		@Override public Iterable<String> keys() { return map.keySet(); }
		@Override public void set(String key, String value) { map.put(key, value); }
	}

	@Test
	void a01_roundTripsTraceKeys() {
		var c = new A01_TestCarrier();
		c.set("traceparent", "00-a-01");
		c.set("tracestate", "vendor=x");
		c.set("baggage", "user=42");
		assertEquals("00-a-01", c.get("traceparent"));
		assertEquals("vendor=x", c.get("tracestate"));
		assertEquals("user=42", c.get("baggage"));
		assertEquals(List.of("traceparent", "tracestate", "baggage"), new ArrayList<>((Collection<String>)c.keys()));
	}
}
