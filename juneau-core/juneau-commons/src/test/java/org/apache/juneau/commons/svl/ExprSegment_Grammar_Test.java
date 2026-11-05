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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests function calls in {@code #{...}} expressions (J0439 Task 3.3).
 */
class ExprSegment_Grammar_Test extends TestBase {

	private static final VarResolver LENIENT = VarResolver.create().defaultFunctions().build();
	private static final VarResolver STRICT = VarResolver.create().defaultFunctions().strict(true).build();

	@Test void a01_simpleCall() {
		assertEquals("A", LENIENT.resolve("#{upper('a')}"));
	}

	@Test void a02_numericArgs() {
		assertEquals("3", LENIENT.resolve("#{add(1,2)}"));
	}

	@Test void a03_nestedCalls() {
		assertEquals("6", LENIENT.resolve("#{add(add(1,2),3)}"));
	}

	@Test void a04_eqStringEquality() {
		assertEquals("false", LENIENT.resolve("#{eq('a','b')}"));
		assertEquals("true", LENIENT.resolve("#{eq('a','a')}"));
	}

	@Test void a05_bareWordIsScopeLookup() {
		// 'hello' is a scope lookup in the empty root scope (null), not the string "hello": upper(null) is absent.
		assertEquals("", LENIENT.resolve("#{upper(hello)}"));
		assertEquals("", STRICT.resolve("#{upper(hello)}"));
		assertEquals("", LENIENT.resolve("#{coalesce(hello,'')}"));
	}

	@Test void a06_unknownFunctionLenientKeepsLiteral() {
		assertEquals("#{nope('x')}", LENIENT.resolve("#{nope('x')}"));
	}

	@Test void a07_unknownFunctionStrictFailsAtCompile() {
		var e = assertThrows(ExprException.class, () -> STRICT.compile("#{nope('x')}"));
		assertTrue(e.getMessage().contains("unknown function 'nope'"), e.getMessage());
	}

	@Test void a08_unknownFunctionOnUntakenBranchFailsAtCompileInStrict() {
		assertThrows(ExprException.class, () -> STRICT.compile("#{true ? 'a' : nope('x')}"));
	}

	@Test void a09_unknownFunctionOnUntakenBranchLenientIsLiteral() {
		assertEquals("#{true ? 'a' : nope('x')}", LENIENT.resolve("#{true ? 'a' : nope('x')}"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unknown-function walker: every AST branch that can hold a Call.
	//-----------------------------------------------------------------------------------------------------------------

	private static void assertUnknown(String expr) {
		assertThrows(ExprException.class, () -> STRICT.compile(expr), expr);
		assertEquals(expr, LENIENT.resolve(expr), expr);
	}

	@Test void b01_nestedArg() { assertUnknown("#{add(1, nope(2))}"); }
	@Test void b02_unary() { assertUnknown("#{!nope(1)}"); }
	@Test void b03_binary() { assertUnknown("#{1 + nope(1)}"); }
	@Test void b04_elvis() { assertUnknown("#{'a' ?: nope(1)}"); }
	@Test void b05_index() { assertUnknown("#{'abc'[nope(1)]}"); }
	@Test void b06_path() { assertUnknown("#{nope(1).length}"); }
	@Test void b07_select() { assertUnknown("#{items.?[nope(1)]}"); }
	@Test void b08_project() { assertUnknown("#{items.![nope(1)]}"); }
	@Test void b09_ternaryCondition() { assertUnknown("#{nope(1) ? 'a' : 'b'}"); }

	@Test void b10_strictErrorPosition() {
		var e = assertThrows(ExprException.class, () -> STRICT.compile("x #{add(1, nope(2))} y"));
		// Position is relative to the whole template: "x #{add(1, " is 11 chars, so 'nope' starts at 11.
		assertEquals(11, e.getPos());
		assertEquals("unknown function 'nope'", e.getBaseMessage());
	}

	@Test void b11_knownFunctionsAndOperatorsAreNotFlagged() {
		assertEquals("Ab", STRICT.resolve("#{upper('a') + 'b'}"));
	}

	@Test void b12_unknownCallInsideVarRefBody() {
		// A ${...} reference is opaque in the AST, so a call inside its body is not checked at compile time;
		// it is checked lazily when the reference is evaluated.
		var t = assertDoesNotThrow(() -> STRICT.compile("#{${S:#{nope(1)}}}"));
		assertNotNull(t);
	}
}
