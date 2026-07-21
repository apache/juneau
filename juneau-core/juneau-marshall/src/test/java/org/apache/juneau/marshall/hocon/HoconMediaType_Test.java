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
package org.apache.juneau.marshall.hocon;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for HOCON media type configuration.
 */
@SuppressWarnings({
	"unchecked"  // Unchecked cast required for generic test utility.
})
class HoconMediaType_Test {

	@Test
	void j01_produces() {
		var ct = HoconSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("hocon", ct.getSubType());
	}

	@Test
	void j02_consumes() {
		var types = HoconParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("hocon")), "Expected hocon types: " + types);
	}

	@Test
	void j03_contentNegotiation() {
		var a = JsonMap.of("name", "test", "count", 42);
		var hocon = toHocon(a);
		assertNotNull(hocon);
		var b = (Map<String, Object>) fromHocon(hocon, Map.class, String.class, Object.class);
		assertBean(b, "name,count", "test,42");
	}

	// Helpers keep the marshaller reference fully-qualified (the simple name Hocon is shadowed by the @Hocon annotation in this package).

	private static String toHocon(Object o) {
		return org.apache.juneau.marshall.marshaller.Hocon.of(o);
	}

	private static <T> T fromHocon(String input, Type type, Type... args) {
		return org.apache.juneau.marshall.marshaller.Hocon.to(input, type, args);
	}
}
