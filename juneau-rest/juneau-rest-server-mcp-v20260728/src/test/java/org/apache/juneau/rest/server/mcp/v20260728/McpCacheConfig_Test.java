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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpCacheHint} and {@link McpCacheConfig}.
 */
class McpCacheConfig_Test {

	@Test void a01_hintAcceptsNullZeroPositiveAndScope() {
		var a = new McpCacheHint().setTtlMs(null).setTtlMs(0).setTtlMs(5).setCacheScope(McpCacheScope.PRIVATE);
		assertEquals(5, a.getTtlMs());
		assertEquals(McpCacheScope.PRIVATE, a.getCacheScope());
	}

	@Test void a02_hintRejectsNegativeDeterministically() {
		var hint = new McpCacheHint();
		var e = assertThrows(IllegalArgumentException.class, () -> hint.setTtlMs(-7));
		assertEquals("ttlMs -7 is below minimum 0", e.getMessage());
	}

	@Test void a03_allNamedFieldsRoundTrip() {
		var hints = IntStream.range(0, 7).mapToObj(x -> new McpCacheHint().setTtlMs(x)).toList();
		var a = new McpCacheConfig()
			.setDefaultHint(hints.get(0)).setToolsList(hints.get(1)).setPromptsList(hints.get(2))
			.setResourcesList(hints.get(3)).setResourceTemplatesList(hints.get(4))
			.setResourcesRead(hints.get(5)).addResourceReadOverride("file:///a", hints.get(6));
		assertSame(hints.get(0), a.getDefaultHint());
		assertSame(hints.get(1), a.getToolsList());
		assertSame(hints.get(2), a.getPromptsList());
		assertSame(hints.get(3), a.getResourcesList());
		assertSame(hints.get(4), a.getResourceTemplatesList());
		assertSame(hints.get(5), a.getResourcesRead());
		assertSame(hints.get(6), a.getResourceReadOverrides().get("file:///a"));
	}

	static class InvalidHint extends McpCacheHint {
		@Override public Integer getTtlMs() { return -9; }
	}

	@Test void b01_eachAssignmentFamilyRevalidates() {
		var bad = new InvalidHint();
		var setters = List.<Consumer<McpCacheHint>>of(
			x -> new McpCacheConfig().setDefaultHint(x),
			x -> new McpCacheConfig().setToolsList(x),
			x -> new McpCacheConfig().setPromptsList(x),
			x -> new McpCacheConfig().setResourcesList(x),
			x -> new McpCacheConfig().setResourceTemplatesList(x),
			x -> new McpCacheConfig().setResourcesRead(x),
			x -> new McpCacheConfig().addResourceReadOverride("file:///a", x));
		setters.forEach(x -> assertEquals("ttlMs -9 is below minimum 0",
			assertThrows(IllegalArgumentException.class, () -> x.accept(bad)).getMessage()));
		var config = new McpCacheConfig();
		Map<String,McpCacheHint> overrides = Map.of("file:///a", bad);
		assertThrows(IllegalArgumentException.class,
			() -> config.setResourceReadOverrides(overrides));
	}

	@Test void b02_overrideMapCopiesPreservesOrderAndNullFallbackEntries() {
		var source = new LinkedHashMap<String,McpCacheHint>();
		source.put("a", new McpCacheHint().setTtlMs(1));
		source.put("b", null);
		var config = new McpCacheConfig().setResourceReadOverrides(source);
		source.clear();
		assertEquals(List.of("a", "b"), new ArrayList<>(config.getResourceReadOverrides().keySet()));
		assertNull(config.getResourceReadOverrides().get("b"));
		var overrides = config.getResourceReadOverrides();
		var extraHint = new McpCacheHint();
		assertThrows(UnsupportedOperationException.class,
			() -> overrides.put("c", extraHint));
	}

	@Test void b03_nullUriRejected() {
		var config = new McpCacheConfig();
		var hint = new McpCacheHint();
		var e = assertThrows(IllegalArgumentException.class,
			() -> config.addResourceReadOverride(null, hint));
		assertEquals("resourceReadOverrides URI must not be null", e.getMessage());
	}
}
