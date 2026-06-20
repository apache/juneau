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
package org.apache.juneau.marshall.json5l;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for JSON5L media type configuration.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<JsonMap> in tests
})
class Json5lMediaType_Test {

	@Test
	void a01_producesCorrectMediaType() {
		var ct = Json5lSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("json5l", ct.getSubType());
	}

	@Test
	void a02_acceptsAllMediaTypes() {
		var types = new ArrayList<String>();
		Json5lSerializer.DEFAULT.forEachAcceptMediaType(mt -> types.add(mt.getType() + "/" + mt.getSubType()));
		assertTrue(types.stream().anyMatch(t -> t.contains("json5l")), "Expected application/json5l: " + types);
		assertTrue(types.stream().anyMatch(t -> "text/json5l".equals(t)), "Expected text/json5l: " + types);
		// Cross-acceptance of the JSONL family (reduced q-value).
		assertTrue(types.stream().anyMatch(t -> t.contains("jsonl")), "Expected jsonl cross-accept: " + types);
		assertTrue(types.stream().anyMatch(t -> t.contains("ndjson")), "Expected ndjson cross-accept: " + types);
	}

	@Test
	void a03_consumesAllMediaTypes() {
		var types = Json5lParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("json5l")), "Expected json5l types: " + types);
		assertTrue(types.stream().anyMatch(t -> t.contains("jsonl")), "Expected jsonl cross-accept: " + types);
		assertTrue(types.stream().anyMatch(t -> t.contains("ndjson")), "Expected ndjson cross-accept: " + types);
	}

	@Test
	void a04_contentNegotiation() throws Exception {
		var a = list(JsonMap.of("k", "v"));
		var json5l = Json5l.DEFAULT.of(a);
		assertNotNull(json5l);
		var b = (List<JsonMap>) Json5l.DEFAULT.to(json5l, List.class, JsonMap.class);
		assertBean(b, "0{k}", "{v}");
	}
}
