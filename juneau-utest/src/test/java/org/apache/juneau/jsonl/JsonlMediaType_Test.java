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
package org.apache.juneau.jsonl;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for JSONL media type configuration.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<JsonMap> in tests
})
class JsonlMediaType_Test {

	@Test
	void a01_producesCorrectMediaType() {
		var ct = JsonlSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("jsonl", ct.getSubType());
	}

	@Test
	void a02_acceptsAllMediaTypes() {
		var types = new ArrayList<String>();
		JsonlSerializer.DEFAULT.forEachAcceptMediaType(mt -> types.add(mt.getType() + "/" + mt.getSubType()));
		assertTrue(types.stream().anyMatch(t -> "application/jsonl".equals(t) || t.contains("jsonl")), "Expected application/jsonl: " + types);
		assertTrue(types.stream().anyMatch(t -> "application/x-ndjson".equals(t) || t.contains("ndjson")), "Expected application/x-ndjson: " + types);
		assertTrue(types.stream().anyMatch("text/jsonl"::equals), "Expected text/jsonl: " + types);
	}

	@Test
	void a03_consumesAllMediaTypes() {
		var types = JsonlParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("jsonl")), "Expected jsonl types: " + types);
		assertTrue(types.stream().anyMatch(t -> t.contains("ndjson")), "Expected ndjson: " + types);
	}

	@Test
	void a04_contentNegotiation() throws Exception {
		var a = list(JsonMap.of("k", "v"));
		var jsonl = Jsonl.of(a);
		assertNotNull(jsonl);
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{k}", "{v}");
	}
}
