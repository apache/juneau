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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link StructureFunctions} and the collection form of {@code join}. */
class StructureFunctions_Test extends TestBase {

	/** Test-only function exposing a list of maps (the root scope is empty). */
	public static class Items implements VarFunction {
		@Override public String name() { return "items"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return "items"; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) {
			return List.of(Map.of("name", "a"), Map.of("name", "b"), Map.of("name", "c"));
		}
	}

	/** Test-only function returning a list of rows. */
	public static class Rows implements VarFunction {
		@Override public String name() { return "rows"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return "rows"; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return List.of(List.of("1","2","3")); }
	}

	private static VarResolver vr() {
		return VarResolver.create().defaultFunctions().functions(new Items(), new Rows()).strict(true).build();
	}

	private static Object val(String expr) {
		var seen = new Object[1];
		var session = vr().createSession().valueFormatter(o -> { seen[0] = o; return String.valueOf(o); });
		session.resolve(expr);
		return seen[0];
	}

	@Test void a01_ulVarargs() {
		var s = (Structure)val("#{ul('x','y')}");
		assertEquals(Structure.Kind.UL, s.kind());
		assertEquals(List.of("x","y"), s.items());
	}

	@Test void a02_ulSingleListIsItems() {
		var s = (Structure)val("#{ul(items().![name])}");
		assertEquals(Structure.Kind.UL, s.kind());
		assertEquals(List.of("a","b","c"), s.items());
	}

	@Test void a03_olVarargsAndList() {
		assertEquals(Structure.Kind.OL, ((Structure)val("#{ol('x')}")).kind());
		assertEquals(List.of("a","b","c"), ((Structure)val("#{ol(items().![name])}")).items());
	}

	@Test void a04_block() {
		var s = (Structure)val("#{block('hi')}");
		assertEquals(Structure.Kind.BLOCK, s.kind());
		assertEquals(List.of("hi"), s.items());
	}

	@Test void a05_table() {
		var s = (Structure)val("#{table(items().![name], rows())}");
		assertEquals(Structure.Kind.TABLE, s.kind());
		assertEquals(List.of("a","b","c"), s.columns());
		assertEquals(List.of(List.of("1","2","3")), s.rows());
	}

	@Test void a06_stringForms() {
		var vr = vr();
		assertEquals("- a\n- b\n- c", vr.resolve("#{ul(items().![name])}"));
		assertEquals("1. a\n2. b\n3. c", vr.resolve("#{ol(items().![name])}"));
		assertEquals("hi", vr.resolve("#{block('hi')}"));
	}

	@Test void b01_joinCollection() {
		assertEquals("a, b, c", vr().resolve("#{join(', ', items().![name])}"));
	}

	@Test void b02_joinVarargsUnchanged() {
		assertEquals("a/b/c", vr().resolve("#{join('/', 'a', 'b', 'c')}"));
	}

	@Test void c01_arityErrors() {
		var vr = vr();
		assertThrows(ExprException.class, () -> vr.resolve("#{block()}"));
		assertThrows(ExprException.class, () -> vr.resolve("#{block(1,2)}"));
		assertThrows(ExprException.class, () -> vr.resolve("#{table(items().![name])}"));
		assertThrows(ExprException.class, () -> vr.resolve("#{ul()}"));
	}

	/** Test-only function returning a header and rows of mismatched width, and a null-header list. */
	public static class Bad implements VarFunction {
		private final String name;
		private final Object value;
		Bad(String name, Object value) { this.name = name; this.value = value; }
		@Override public String name() { return name; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return name; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return value; }
	}

	@Test void c03_tableRowWidthMismatch() {
		var vr = VarResolver.create().defaultFunctions().strict(true)
			.functions(new Bad("hdr", List.of("a", "b")), new Bad("badrows", List.of(List.of("1", "2"), List.of("3"))),
				new Bad("nullhdr", Arrays.asList("a", null)), new Bad("okrows", List.of(List.of("1", "2")))).build();
		var ex = assertThrows(ExprException.class, () -> vr.resolve("#{table(hdr(), badrows())}"));
		assertTrue(ex.getMessage().contains("row 1 has 1 cells but the header has 2 columns"), ex.getMessage());
		var ex2 = assertThrows(ExprException.class, () -> vr.resolve("#{table(nullhdr(), okrows())}"));
		assertTrue(ex2.getMessage().contains("header must not contain null"), ex2.getMessage());
		assertTrue(ex.getCause() instanceof VarResolverException, String.valueOf(ex.getCause()));
	}

	@Test void c04_ulSingleListArgIsItemsList() {
		// ul(list) means "the items are the list's elements", never a single list-valued item.
		var s = (Structure)val("#{ul(items().![name])}");
		assertEquals(3, s.items().size());
	}

	@Test void c02_typeErrors() {
		var vr = vr();
		assertThrows(ExprException.class, () -> vr.resolve("#{table('a', 'b')}"));
		assertThrows(ExprException.class, () -> vr.resolve("#{table(items().![name], 'b')}"));
	}

	@Test void d01_allowFunctionsDeniesStructure() {
		var vr = VarResolver.create().defaultFunctions().functions(new Items()).strict(true).allowFunctions("upper").build();
		assertEquals("A", vr.resolve("#{upper('a')}"));
		var ex = assertThrows(ExprException.class, () -> vr.resolve("#{ul('a')}"));
		assertTrue(ex.getMessage().contains("not permitted"), ex.getMessage());
	}

	@Test void e01_allDiscovered() {
		var vr = vr();
		for (var n : List.of("ul","ol","block","table","join"))
			assertNotNull(vr.createSession().findFunction(n), n);
	}
}
