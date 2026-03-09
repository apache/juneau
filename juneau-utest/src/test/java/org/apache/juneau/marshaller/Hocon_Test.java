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
package org.apache.juneau.marshaller;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link Hocon}.
 */
@SuppressWarnings("unchecked")
class Hocon_Test {

	@Test
	void h01_of() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "test");
		a.put("count", 42);
		var hocon = Hocon.of(a);
		assertNotNull(hocon);
		assertTrue(hocon.contains("name") && hocon.contains("test"));
		assertTrue(hocon.contains("count") && hocon.contains("42"));
	}

	@Test
	void h02_to() throws Exception {
		var hocon = "name = Alice\nage = 30";
		var m = (Map<String, Object>) Hocon.to(hocon, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Alice,30");
	}

	@Test
	void h03_roundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "foo");
		a.put("value", 123);
		var hocon = Hocon.of(a);
		var b = (Map<String, Object>) Hocon.to(hocon, Map.class, String.class, Object.class);
		assertBean(b, "name,value", "foo,123");
	}

	@Test
	void h04_defaultInstance() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("k", "v");
		var hocon = Hocon.DEFAULT.write(a);
		assertTrue(hocon.contains("k") && hocon.contains("v"));
		var b = (Map<String, Object>) Hocon.DEFAULT.read(hocon, Map.class, String.class, Object.class);
		assertBean(b, "k", "v");
	}
}
