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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/** Tests for {@link Structure}. */
class Structure_Test extends TestBase {

	@Test void a01_ul() {
		var s = Structure.ul(List.of("a", "b"));
		assertEquals(Structure.Kind.UL, s.kind());
		assertEquals(List.of("a", "b"), s.items());
		assertEquals("- a\n- b", s.toString());
		assertTrue(s.options().isEmpty());
		assertTrue(s.rows().isEmpty());
	}

	@Test void a02_ol() {
		var s = Structure.ol(List.of("a", "b"));
		assertEquals(Structure.Kind.OL, s.kind());
		assertEquals("1. a\n2. b", s.toString());
	}

	@Test void a03_block() {
		var s = Structure.block("hello");
		assertEquals(Structure.Kind.BLOCK, s.kind());
		assertEquals(List.of("hello"), s.items());
		assertEquals("hello", s.toString());
	}

	@Test void a04_table() {
		var s = Structure.table(List.of("n", "v"), List.of(List.of("a", 1), List.of("b", 2)));
		assertEquals(Structure.Kind.TABLE, s.kind());
		assertEquals(List.of("n", "v"), s.columns());
		assertEquals(2, s.rows().size());
		assertEquals(List.of("b", 2), s.rows().get(1));
		assertTrue(s.items().isEmpty());
		assertEquals("n\tv\na\t1\nb\t2", s.toString());
	}

	@Test void b01_defensiveCopies() {
		var items = new ArrayList<Object>(List.of("a"));
		var opts = new HashMap<String,Object>(Map.of("k", "v"));
		var s = Structure.ul(items, opts);
		items.add("b");
		opts.put("x", "y");
		assertEquals(List.of("a"), s.items());
		assertEquals(Map.of("k", "v"), s.options());

		var row = new ArrayList<Object>(List.of("a"));
		var rows = new ArrayList<List<?>>(List.of(row));
		var t = Structure.table(List.of("c"), rows);
		row.add("z");
		rows.clear();
		assertEquals(List.of(List.of("a")), t.rows());
	}

	@Test void b02_immutable() {
		var s = Structure.ol(List.of("a"), Map.of("k", 1));
		assertThrows(UnsupportedOperationException.class, () -> ((List<Object>)s.items()).add("b"));
		assertThrows(UnsupportedOperationException.class, () -> s.options().put("x", 2));
		var t = Structure.table(List.of("c"), List.of(List.of("a")));
		assertThrows(UnsupportedOperationException.class, () -> t.columns().add("d"));
		assertThrows(UnsupportedOperationException.class, () -> ((List<Object>)t.rows().get(0)).add("b"));
	}

	@Test void b03_optionsOverloads() {
		assertEquals(Map.of("a", 1), Structure.ul(List.of(), Map.of("a", 1)).options());
		assertEquals(Map.of("a", 1), Structure.ol(List.of(), Map.of("a", 1)).options());
		assertEquals(Map.of("a", 1), Structure.block("x", Map.of("a", 1)).options());
		assertEquals(Map.of("a", 1), Structure.table(List.of(), List.of(), Map.of("a", 1)).options());
	}

	@Test void b04_nullsRejected() {
		assertThrows(NullPointerException.class, () -> Structure.ul(null));
		assertThrows(NullPointerException.class, () -> Structure.table(null, List.of()));
		assertThrows(NullPointerException.class, () -> Structure.table(List.of(), null));
	}

	@Test void c01_emptyAndNullElements() {
		assertEquals("", Structure.ul(List.of()).toString());
		assertEquals("- null", Structure.ul(Arrays.asList((Object)null)).toString());
		assertEquals("null", Structure.block(null).toString());
	}

	@Test void d01_nullRowAndNullOptions() {
		var rows = new ArrayList<List<?>>();
		rows.add(null);
		var e = assertThrows(NullPointerException.class, () -> Structure.table(List.of("c"), rows));
		assertTrue(e.getMessage().contains("rows"), e.getMessage());
		assertTrue(Structure.ul(List.of("a"), null).options().isEmpty());
		assertTrue(Structure.table(List.of(), List.of(), null).options().isEmpty());
	}
}
