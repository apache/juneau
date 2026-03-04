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
 * WITHOUT WARRANTIES OR CONDITIONS FOR A PARTICULAR PURPOSE.  See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshaller;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to Map in tests
})
class Ini_Test extends TestBase {

	@Test
	void a01_of() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "test");
		a.put("count", 42);
		var ini = Ini.of(a);
		assertNotNull(ini);
		assertTrue(ini.contains("name") && ini.contains("test"));
		assertTrue(ini.contains("count") && ini.contains("42"));
	}

	@Test
	void a02_to() throws Exception {
		var ini = "name = Alice\nage = 30";
		var m = (Map<String, Object>) Ini.to(ini, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Alice,30");
	}

	@Test
	void a03_roundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "foo");
		a.put("value", 123);
		var ini = Ini.of(a);
		var b = (Map<String, Object>) Ini.to(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,value", "foo,123");
	}

	@Test
	void a04_defaultInstance() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("k", "v");
		var ini = Ini.DEFAULT.write(a);
		assertTrue(ini.contains("k") && ini.contains("v"));
		var b = (Map<String, Object>) Ini.DEFAULT.read(ini, Map.class, String.class, Object.class);
		assertBean(b, "k", "v");
	}
}
