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
package org.apache.juneau.marshall.hjson;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for Hjson media type configuration.
 */
@SuppressWarnings({
	"unchecked"  // Unchecked cast required for generic test utility.
})
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
	void g03_contentNegotiation() {
		var a = JsonMap.of("name", "test", "count", 42);
		var hjson = toHjson(a);
		assertNotNull(hjson);
		var b = (Map<String,Object>) fromHjson(hjson, Map.class, String.class, Object.class);
		assertBean(b, "name,count", "test,42");
	}

	// Helpers keep the marshaller reference fully-qualified (the simple name Hjson is shadowed by the @Hjson annotation in this package).

	private static String toHjson(Object o) {
		return org.apache.juneau.marshall.marshaller.Hjson.of(o);
	}

	private static <T> T fromHjson(String input, Type type, Type... args) {
		return org.apache.juneau.marshall.marshaller.Hjson.to(input, type, args);
	}
}
