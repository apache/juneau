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
package org.apache.juneau.hjson;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for Hjson media type configuration.
 */
@SuppressWarnings("unchecked")
class HjsonMediaType_Test {

	@Test
	void g01_produces() {
		var ct = HjsonSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("hjson", ct.getSubType());
	}

	@Test
	void g02_consumes() {
		var types = HjsonParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("hjson")), "Expected hjson types: " + types);
	}

	@Test
	void g03_contentNegotiation() throws Exception {
		var a = JsonMap.of("name", "test", "count", 42);
		var hjson = Hjson.of(a);
		assertNotNull(hjson);
		var b = (Map<String, Object>) Hjson.to(hjson, Map.class, String.class, Object.class);
		assertBean(b, "name,count", "test,42");
	}
}
