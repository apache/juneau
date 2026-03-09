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
package org.apache.juneau.bson;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@link BsonSerializer} and {@link BsonParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to Map in tests
})
class BsonRoundTrip_Test extends TestBase {

	@Test
	void a01_simpleMapRoundTrip() throws Exception {
		var a = JsonMap.of("name", "Alice", "age", 30, "active", true);
		var bytes = BsonSerializer.create().keepNullProperties().build().serialize(a);
		var b = (Map<String, Object>) BsonParser.create().build().parse(bytes, Map.class, String.class, Object.class);
		assertBean(b, "name,age,active", "Alice,30,true");
	}

	@Test
	void a02_nestedMapRoundTrip() throws Exception {
		var addr = new LinkedHashMap<String, Object>();
		addr.put("city", "Boston");
		addr.put("state", "MA");
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "Alice");
		a.put("address", addr);
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(a);
		var b = (Map<String, Object>) p.parse(bytes, Map.class, String.class, Object.class);
		assertBean(b, "name,address{city,state}", "Alice,{Boston,MA}");
	}

	@Test
	void a03_listRoundTrip() throws Exception {
		var a = list(1, 2, 3);
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(a);
		var b = p.parse(bytes, List.class);
		assertEquals(a, b);
	}

	@Test
	void a04_scalarWrappedRoundTrip() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(42);
		var b = p.parse(bytes, int.class);
		assertEquals(42, b);
	}

	@Test
	void a05_intArrayRoundTrip() throws Exception {
		var a = ints(1, 2, 3);
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(a);
		var b = p.parse(bytes, int[].class);
		assertArrayEquals(a, b);
	}

	@Test
	void a06_enumRoundTrip() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(JsonMap.of("size", "LARGE"));
		var b = p.parse(bytes, Map.class);
		assertEquals("LARGE", b.get("size"));
	}

	@Test
	void a07_dateRoundTrip() throws Exception {
		var instant = Instant.ofEpochMilli(1700000000000L);
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(JsonMap.of("ts", instant));
		var b = p.parse(bytes, Map.class);
		assertEquals(1700000000000L, b.get("ts"));
	}

	@Test
	void a08_byteArrayRoundTrip() throws Exception {
		var data = new byte[] { 0x01, 0x02, 0x03 };
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(JsonMap.of("data", data));
		var b = p.parse(bytes, Map.class);
		assertArrayEquals(data, (byte[])b.get("data"));
	}
}
