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
package org.apache.juneau.hocon;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@link HoconSerializer} and {@link HoconParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to Map/bean in tests
})
class HoconRoundTrip_Test extends TestBase {

	@Test
	void c01_simpleBeanRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "Alice");
		a.put("age", 30);
		a.put("active", true);
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(b, "name,age,active", "Alice,30,true");
	}

	@Test
	void c02_nestedBeanRoundTrip() throws Exception {
		var addr = new LinkedHashMap<String, Object>();
		addr.put("city", "Boston");
		addr.put("state", "MA");
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "Alice");
		a.put("address", addr);
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(b, "name,address{city,state}", "Alice,{Boston,MA}");
	}

	@Test
	void c03_collectionRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("tags", list("a", "b", "c"));
		a.put("counts", list(1, 2, 3));
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(b, "tags,counts", "[a,b,c],[1,2,3]");
	}

	@Test
	void c04_mapRoundTrip() throws Exception {
		var nested = new LinkedHashMap<String, Object>();
		nested.put("host", "localhost");
		nested.put("port", 8080);
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "app");
		a.put("database", nested);
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(b, "name,database{host,port}", "app,{localhost,8080}");
	}

	@Test
	void c05_enumRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("size", "LARGE");
		a.put("status", "ACTIVE");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(b, "size,status", "LARGE,ACTIVE");
	}

	@Test
	void c06_multilineStringRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("desc", "line1\nline2\nline3");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("line1\nline2\nline3", b.get("desc"));
	}

	@Test
	void c07_specialCharStringRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("s", "a{b}c:d\"e");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("a{b}c:d\"e", b.get("s"));
	}

	@Test
	void c08_booleanStringRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("asBoolean", true);
		a.put("asString", "true");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(true, b.get("asBoolean"));
		assertEquals("true", b.get("asString"));
	}

	@Test
	void c09_numberStringRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("asNumber", 42);
		a.put("asString", "42");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(42, b.get("asNumber"));
		assertEquals("42", b.get("asString"));
	}

	@Test
	void c10_nullRoundTrip() throws Exception {
		var hocon = "name = x\nmiddle = null";
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("x", b.get("name"));
		assertNull(b.get("middle"));
	}

	@Test
	void c11_emptyStringRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("empty", "");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("", b.get("empty"));
	}

	@Test
	void c12_complexBeanRoundTrip() throws Exception {
		var nested = new LinkedHashMap<String, Object>();
		nested.put("city", "NYC");
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "Bob");
		a.put("age", 25);
		a.put("active", true);
		a.put("tags", list("x", "y"));
		a.put("address", nested);
		a.put("empty", "");
		var hocon = HoconSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(b, "name,age,active,tags,address{city},empty", "Bob,25,true,[x,y],{NYC},");
	}
}
