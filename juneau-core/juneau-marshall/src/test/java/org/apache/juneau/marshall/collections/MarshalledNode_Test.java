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
package org.apache.juneau.marshall.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the typed tree façade {@link MarshalledNode}.
 *
 * <p>
 * Covers node-type predicates, typed accessors, child navigation, fluent tree building (asserting both the
 * underlying {@link JsonMap}/{@link JsonList} structure and the serialized JSON output), the live-view mutation
 * contract, and {@link MarshalledNode#copy()} deep-copy independence.
 */
class MarshalledNode_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a - Predicates
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_objectNodePredicates() {
		var n = MarshalledNode.of(JsonMap.of("a", 1));
		assertTrue(n.isObject());
		assertFalse(n.isArray());
		assertFalse(n.isValue());
		assertFalse(n.isNull());
	}

	@Test void a02_arrayNodePredicates() {
		var n = MarshalledNode.of(JsonList.of(1, 2, 3));
		assertFalse(n.isObject());
		assertTrue(n.isArray());
		assertFalse(n.isValue());
		assertFalse(n.isNull());
	}

	@Test void a03_valueNodePredicates() {
		var n = MarshalledNode.of("hello");
		assertFalse(n.isObject());
		assertFalse(n.isArray());
		assertTrue(n.isValue());
		assertFalse(n.isNull());
	}

	@Test void a04_nullNodePredicates() {
		var n = MarshalledNode.of(null);
		assertFalse(n.isObject());
		assertFalse(n.isArray());
		assertFalse(n.isValue());
		assertTrue(n.isNull());
	}

	@Test void a05_objectNodeFactory() {
		var n = MarshalledNode.objectNode();
		assertTrue(n.isObject());
		assertInstanceOf(JsonMap.class, n.value());
		assertEquals(0, n.size());
	}

	@Test void a06_arrayNodeFactory() {
		var n = MarshalledNode.arrayNode();
		assertTrue(n.isArray());
		assertInstanceOf(JsonList.class, n.value());
		assertEquals(0, n.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b - Typed accessors
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_asString() {
		assertEquals("hello", MarshalledNode.of("hello").asString());
		assertEquals("123", MarshalledNode.of(123).asString());
	}

	@Test void b02_asInt() {
		assertEquals(Integer.valueOf(42), MarshalledNode.of(42).asInt());
		assertEquals(Integer.valueOf(7), MarshalledNode.of("7").asInt());
	}

	@Test void b03_asLong() {
		assertEquals(Long.valueOf(9999999999L), MarshalledNode.of(9999999999L).asLong());
		assertEquals(Long.valueOf(5), MarshalledNode.of("5").asLong());
	}

	@Test void b04_asDouble() {
		assertEquals(Double.valueOf(3.5), MarshalledNode.of(3.5).asDouble());
		assertEquals(Double.valueOf(2.0), MarshalledNode.of("2.0").asDouble());
	}

	@Test void b05_asBoolean() {
		assertEquals(Boolean.TRUE, MarshalledNode.of(true).asBoolean());
		assertEquals(Boolean.TRUE, MarshalledNode.of("true").asBoolean());
		assertEquals(Boolean.FALSE, MarshalledNode.of("false").asBoolean());
	}

	public static class B06_Bean {
		public String name;
		public int age;
	}

	@Test void b06_asBean() {
		var n = MarshalledNode.of(JsonMap.of("name", "John", "age", 45));
		var b = n.as(B06_Bean.class);
		assertNotNull(b);
		assertEquals("John", b.name);
		assertEquals(45, b.age);
	}

	@Test void b07_wrongTypeReturnsNull() {
		// Scalar accessors on container/null nodes return null.
		assertNull(MarshalledNode.of(JsonMap.of("a", 1)).asString());
		assertNull(MarshalledNode.of(JsonList.of(1)).asInt());
		assertNull(MarshalledNode.of(null).asString());
		assertNull(MarshalledNode.of(null).asInt());
		// Non-convertible scalar returns null rather than throwing.
		assertNull(MarshalledNode.of("notanumber").asInt());
	}

	@Test void b08_value() {
		var raw = JsonMap.of("a", 1);
		assertSame(raw, MarshalledNode.of(raw).value());
		assertEquals("x", MarshalledNode.of("x").value());
		assertNull(MarshalledNode.of(null).value());
	}

	@Test void b09_asNullAndConversionFailureReturnNull() {
		// as(Class) on a null node short-circuits to null (no conversion attempted).
		assertNull(MarshalledNode.of(null).as(B06_Bean.class));
		// as(Class) swallows InvalidDataConversionException and returns null for a non-convertible scalar...
		assertNull(MarshalledNode.of("notanumber").as(Integer.class));
		// ...and for a container that cannot convert to the requested scalar type.
		assertNull(MarshalledNode.of(JsonList.of(1, 2)).as(Integer.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c - Navigation
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_getByName() {
		var n = MarshalledNode.of(JsonMap.of("a", "A", "b", JsonMap.of("c", "C")));
		assertEquals("A", n.get("a").asString());
		assertTrue(n.get("b").isObject());
		assertEquals("C", n.get("b").get("c").asString());
	}

	@Test void c02_getByNameMissing() {
		var n = MarshalledNode.of(JsonMap.of("a", "A"));
		assertNull(n.get("missing"));
	}

	@Test void c03_getByNameWrongType() {
		assertNull(MarshalledNode.of(JsonList.of(1, 2)).get("a"));
		assertNull(MarshalledNode.of("scalar").get("a"));
	}

	@Test void c04_getByIndex() {
		var n = MarshalledNode.of(JsonList.of("x", "y", "z"));
		assertEquals("x", n.get(0).asString());
		assertEquals("z", n.get(2).asString());
	}

	@Test void c05_getByIndexOutOfRange() {
		var n = MarshalledNode.of(JsonList.of("x"));
		assertNull(n.get(5));
		assertNull(n.get(-1));
	}

	@Test void c06_getByIndexWrongType() {
		assertNull(MarshalledNode.of(JsonMap.of("a", 1)).get(0));
		assertNull(MarshalledNode.of("scalar").get(0));
	}

	@Test void c07_size() {
		assertEquals(2, MarshalledNode.of(JsonMap.of("a", 1, "b", 2)).size());
		assertEquals(3, MarshalledNode.of(JsonList.of(1, 2, 3)).size());
		assertEquals(0, MarshalledNode.of("scalar").size());
		assertEquals(0, MarshalledNode.of(null).size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d - Fluent build
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_fluentBuild() {
		var n = MarshalledNode.objectNode()
			.put("a", 1)
			.put("b", MarshalledNode.arrayNode().add("x").add("y").value());

		// Underlying structure.
		var m = (JsonMap)n.value();
		assertEquals(1, m.getInt("a"));
		assertInstanceOf(JsonList.class, m.get("b"));
		assertEquals("x", m.getList("b").getString(0));
		assertEquals("y", m.getList("b").getString(1));

		// Serialized JSON output.
		assertEquals("{\"a\":1,\"b\":[\"x\",\"y\"]}", Json.DEFAULT.of(m));
	}

	@Test void d02_putRequiresObjectNode() {
		var array = MarshalledNode.arrayNode();
		var scalar = MarshalledNode.of("scalar");
		var nul = MarshalledNode.of(null);
		assertThrows(IllegalStateException.class, () -> array.put("a", 1));
		assertThrows(IllegalStateException.class, () -> scalar.put("a", 1));
		assertThrows(IllegalStateException.class, () -> nul.put("a", 1));
	}

	@Test void d03_addRequiresArrayNode() {
		var object = MarshalledNode.objectNode();
		var scalar = MarshalledNode.of("scalar");
		var nul = MarshalledNode.of(null);
		assertThrows(IllegalStateException.class, () -> object.add("x"));
		assertThrows(IllegalStateException.class, () -> scalar.add("x"));
		assertThrows(IllegalStateException.class, () -> nul.add("x"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// e - Live view
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_putMutatesBackingMap() {
		var map = JsonMap.of("a", 1);
		MarshalledNode.of(map).put("b", 2);
		assertEquals(2, map.getInt("b"));
		assertEquals("{\"a\":1,\"b\":2}", Json.DEFAULT.of(map));
	}

	@Test void e02_addMutatesBackingList() {
		var list = JsonList.of("x");
		MarshalledNode.of(list).add("y");
		assertEquals(2, list.size());
		assertEquals("y", list.getString(1));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// f - Copy independence
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_copyMutateCopyLeavesOriginal() {
		var original = JsonMap.of("a", 1, "nested", JsonList.of(10, 20));
		var node = MarshalledNode.of(original);
		var copy = node.copy();

		((JsonMap)copy.value()).put("a", 99);
		copy.get("nested").add(30);

		// Original is unchanged.
		assertEquals(1, original.getInt("a"));
		assertEquals(2, original.getList("nested").size());
	}

	@Test void f02_copyMutateOriginalLeavesCopy() {
		var original = JsonMap.of("a", 1, "nested", JsonList.of(10, 20));
		var copy = MarshalledNode.of(original).copy();

		original.put("a", 99);
		original.getList("nested").add(30);

		// Copy is unchanged.
		assertEquals(1, copy.get("a").asInt());
		assertEquals(2, copy.get("nested").size());
	}

	@Test void f03_copyScalarAndNull() {
		assertEquals("x", MarshalledNode.of("x").copy().asString());
		assertTrue(MarshalledNode.of(null).copy().isNull());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// g - JSON-Pointer (RFC 6901)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_at() {
		var n = MarshalledNode.of(JsonMap.of("a", JsonMap.of("b", JsonList.of("x", "y"))));
		assertEquals("x", n.at("/a/b/0").asString());
		assertTrue(n.at("").isObject());
	}

	@Test void g02_atMissReturnsNull() {
		var n = MarshalledNode.of(JsonMap.of("a", 1));
		assertNull(n.at("/missing"));
		assertNull(n.at("/a/b"));
	}

	@Test void g03_atPresentNullReturnsNullNode() {
		var m = new JsonMap();
		m.put("a", null);
		var r = MarshalledNode.of(m).at("/a");
		assertNotNull(r);
		assertTrue(r.isNull());
	}

	@Test void g04_find() {
		var n = MarshalledNode.of(JsonMap.of("a", 1));
		assertTrue(n.find("/a").isPresent());
		assertEquals(Integer.valueOf(1), n.find("/a").get().asInt());
		assertFalse(n.find("/missing").isPresent());
	}

	@Test void g05_findPresentNull() {
		var m = new JsonMap();
		m.put("a", null);
		var opt = MarshalledNode.of(m).find("/a");
		assertTrue(opt.isPresent());
		assertTrue(opt.get().isNull());
	}

	@Test void g06_set() {
		var m = new JsonMap();
		var n = MarshalledNode.of(m);
		assertSame(n, n.set("/a/b", 1));
		assertEquals("{\"a\":{\"b\":1}}", Json.DEFAULT.of(m));
	}

	@Test void g07_setAppend() {
		var m = JsonMap.of("a", JsonList.of(1, 2));
		MarshalledNode.of(m).set("/a/-", 3);
		assertEquals("{\"a\":[1,2,3]}", Json.DEFAULT.of(m));
	}

	@Test void g08_setRootThrows() {
		var n = MarshalledNode.objectNode();
		assertThrows(IllegalArgumentException.class, () -> n.set("", 1));
	}

	@Test void g09_remove() {
		var m = JsonMap.of("a", 1, "b", 2);
		assertEquals(Integer.valueOf(1), MarshalledNode.of(m).remove("/a"));
		assertEquals("{\"b\":2}", Json.DEFAULT.of(m));
		assertNull(MarshalledNode.of(m).remove("/missing"));
	}

	@Test void g10_marshalledMapAt() {
		var m = JsonMap.of("a", JsonMap.of("b", 1));
		assertEquals(Integer.valueOf(1), m.at("/a/b").asInt());
	}

	@Test void g11_marshalledListAt() {
		var l = JsonList.of("x", "y");
		assertEquals("y", l.at("/1").asString());
	}
}
