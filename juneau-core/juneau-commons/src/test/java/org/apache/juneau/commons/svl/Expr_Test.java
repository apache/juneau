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
import org.apache.juneau.commons.svl.Expr.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Expr#show()}.
 */
class Expr_Test extends TestBase {

	private static Expr id(String n) { return new Ident(n, 0); }

	@Test void a01_literals() {
		assertEquals("'it''s'", new Literal("it's", 0).show());
		assertEquals("1", new Literal(1L, 0).show());
		assertEquals("null", new Literal(null, 0).show());
		assertEquals("true", new Literal(true, 0).show());
	}

	@Test void a02_leaves() {
		assertEquals("a", id("a").show());
		assertEquals("${C:x}", new VarRef("${C:x}", 0).show());
	}

	@Test void a03_binaryUnaryCall() {
		var b = new Binary("+", new Literal(1L, 0), new Binary("*", new Literal(2L, 0), new Literal(3L, 0), 0), 0);
		assertEquals("(+ 1 (* 2 3))", b.show());
		assertEquals("(not a)", new Unary("not", id("a"), 0).show());
		assertEquals("(- x)", new Unary("-", id("x"), 0).show());
		assertEquals("(f a b)", new Call("f", List.of(id("a"), id("b")), 0).show());
		assertEquals("(f)", new Call("f", List.of(), 0).show());
	}

	@Test void a04_conditionals() {
		assertEquals("(? c a b)", new Ternary(id("c"), id("a"), id("b"), 0).show());
		assertEquals("(?: l r)", new Elvis(id("l"), id("r"), 0).show());
	}

	@Test void a05_access() {
		assertEquals("(. a b)", new Path(id("a"), "b", false, 0).show());
		assertEquals("(?. a b)", new Path(id("a"), "b", true, 0).show());
		assertEquals("(idx a k)", new Index(id("a"), id("k"), false, 0).show());
		assertEquals("(?idx a k)", new Index(id("a"), id("k"), true, 0).show());
		assertEquals("(.? t p)", new Select(id("t"), id("p"), 0).show());
		assertEquals("(.! t m)", new Project(id("t"), id("m"), 0).show());
	}
}
