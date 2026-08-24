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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/**
 * {@link ServerValuesRegistry} resolution semantics against a hand-built {@link VarResolver} session: scalar
 * coercion, missing-name null, non-scalar rejection, provider-throw fail-closed, and per-render memoization.
 */
class ServerValuesRegistry_Test extends TestBase {

	private static VarResolverSession session() {
		return VarResolver.DEFAULT.createSession();
	}

	@Test void a01_resolvesScalarValues() {
		var reg = ServerValuesRegistry.of(ServerValues.create()
			.value("s", s -> "hello")
			.value("n", s -> 42)
			.value("b", s -> true));
		var s = session();
		assertEquals("hello", reg.resolve(s, "s"));
		assertEquals("42", reg.resolve(s, "n"));
		assertEquals("true", reg.resolve(s, "b"));
	}

	@Test void a02_missingName_returnsNull() {
		var reg = ServerValuesRegistry.of(ServerValues.create().value("s", s -> "hello"));
		assertNull(reg.resolve(session(), "missing"));
	}

	@Test void a03_nullProviderResult_returnsNull() {
		var reg = ServerValuesRegistry.of(ServerValues.create().value("s", s -> null));
		assertNull(reg.resolve(session(), "s"));
	}

	@Test void a04_nonScalar_rejected() {
		var reg = ServerValuesRegistry.of(ServerValues.create()
			.value("list", s -> List.of("a", "b"))
			.value("map", s -> Map.of("k", "v")));
		var s = session();
		var e = assertThrows(IllegalArgumentException.class, () -> reg.resolve(s, "list"));
		assertTrue(e.getMessage().contains("non-scalar"), e::getMessage);
		assertFalse(e.getMessage().contains("[a, b]"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> reg.resolve(s, "map"));
	}

	@Test void a05_providerThrow_failsClosed() {
		var reg = ServerValuesRegistry.of(ServerValues.create()
			.value("boom", s -> { throw new RuntimeException("kaboom"); }));
		var s = session();
		assertThrows(RuntimeException.class, () -> reg.resolve(s, "boom"));
	}

	@Test void a06_cacheable_memoizedOncePerRender() {
		var calls = new AtomicInteger();
		var m = new LinkedHashMap<String,ServerValuesValue>();
		m.put("c", ServerValuesValue.of("c", s -> { calls.incrementAndGet(); return "v"; }, true));
		var reg = ServerValuesRegistry.of(ServerValues.create().values(m));
		var s = session();
		assertEquals("v", reg.resolve(s, "c"));
		assertEquals("v", reg.resolve(s, "c"));
		assertEquals(1, calls.get());
	}

	@Test void a07_nonCacheable_reinvokedEachReference() {
		var calls = new AtomicInteger();
		var reg = ServerValuesRegistry.of(ServerValues.create()
			.value("c", s -> { calls.incrementAndGet(); return "v"; }));
		var s = session();
		reg.resolve(s, "c");
		reg.resolve(s, "c");
		assertEquals(2, calls.get());
	}
}
