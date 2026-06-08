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
package org.apache.juneau.http.part;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for the {@link org.apache.juneau.http.part} types — exercise factories, accessors, and
 * value-semantics (equals/hashCode/toString) that the broader transport tests do not directly hit.
 */
class HttpParts_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// HttpPartBean — equals/hashCode/toString and the lazy-Supplier factory.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_partBean_eagerFactory() {
		var p = HttpPartBean.of("foo", "bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
		assertEquals("foo=bar", p.toString());
	}

	@Test void a02_partBean_lazyFactory() {
		var p = HttpPartBean.of("foo", () -> "bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void a03_partBean_equalsAndHashCode_sameValue() {
		var a = HttpPartBean.of("x", "1");
		var b = HttpPartBean.of("x", "1");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test void a04_partBean_equalsAndHashCode_differentValue() {
		var a = HttpPartBean.of("x", "1");
		var b = HttpPartBean.of("x", "2");
		assertNotEquals(a, b);
	}

	@Test void a05_partBean_equalsAndHashCode_differentName() {
		var a = HttpPartBean.of("x", "1");
		var b = HttpPartBean.of("y", "1");
		assertNotEquals(a, b);
	}

	@Test void a06_partBean_equals_typeMismatch() {
		var p = HttpPartBean.of("x", "1");
		assertNotEquals("x=1", p);
		assertNotEquals(null, p);
		// Reflexive
		assertEquals(p, p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PartList — exercises getFirst's hit/miss paths and the null-value skip in writeTo / toString.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_partList_getFirst_hitAndMiss() {
		var pl = PartList.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2"));
		assertEquals("1", pl.getFirst("a").getValue());
		assertNull(pl.getFirst("missing"));
		assertEquals(2, pl.size());
		assertFalse(pl.isEmpty());
	}

	@Test void b02_partList_toString_skipsNullValues() {
		// One eager part + one lazy part whose supplier returns null — the null value should be skipped.
		var pl = PartList.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", () -> null));
		assertEquals("a=1", pl.toString());
	}
}
