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
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parity tests between Juneau {@code #{...}} expressions and Spring SpEL (J0439 Task 7.2).
 *
 * <p>
 * No SpEL dependency: each expected value is hard-coded from the documented SpEL semantics named in the comment on
 * the case. Output is what {@code #{...}} renders: lists as compact JSON (strings quoted inside), top-level strings bare.
 *
 * <p>
 * Section A is parity.  Section B lists INTENTIONAL divergences from SpEL, asserting Juneau's actual behavior.
 */
class SpelParity_Test extends TestBase {

	private static VarFunction fn(String name, Supplier<Object> s) {
		return new VarFunction() {
			@Override public String name() { return name; }
			@Override public int minArity() { return 0; }
			@Override public int maxArity() { return 0; }
			@Override public String invoke(VarResolverSession session, List<Object> args) { return "legacy"; }
			@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return s.get(); }
		};
	}

	private static Map<String,Object> person(String name, int age) {
		var m = new LinkedHashMap<String,Object>();
		m.put("name", name);
		m.put("age", age);
		return m;
	}

	/** Fixture data is exposed through zero-arg functions (SpEL would use variables/root object). */
	private static final VarResolver VR = VarResolver.create().defaultFunctions()
		.functions(fn("nums", () -> List.of(1, 2, 3, 4, 5)))
		.functions(fn("people", () -> List.of(person("Ann", 30), person("Bob", 15), person("Cy", 40))))
		.functions(fn("ann", () -> person("Ann", 30)))
		.functions(fn("nul", () -> null))
		.functions(fn("emptyStr", () -> ""))
		.build();

	record Case(String expr, String expected) {
		@Override public String toString() { return expr + "  =>  " + expected; }
	}

	private static Case c(String expr, String expected) { return new Case(expr, expected); }

	static Stream<Case> parityCases() {
		return Stream.of(
			// Arithmetic (SpEL OpPlus/OpMinus/...: int op int stays int; any double promotes to double).
			c("#{1 + 2}", "3"),
			c("#{5 - 7}", "-2"),
			c("#{6 * 7}", "42"),
			c("#{8 / 2}", "4"),
			c("#{7 / 2}", "3"),                 // int/int stays integral
			c("#{7 / 2.0}", "3.5"),
			c("#{7 % 3}", "1"),
			c("#{2 + 3 * 4}", "14"),            // standard precedence
			c("#{(2 + 3) * 4}", "20"),
			c("#{-5 + 2}", "-3"),               // unary minus
			c("#{1.5 + 1}", "2.5"),             // int promotes to double
			// String concat: '+' concatenates when either side is a String.
			c("#{'a' + 'b'}", "ab"),
			c("#{'n=' + 5}", "n=5"),
			c("#{5 + 'x'}", "5x"),
			c("#{\"dq\" + 'sq'}", "dqsq"),
			// Comparison.
			c("#{1 < 2}", "true"),
			c("#{2 <= 2}", "true"),
			c("#{3 > 4}", "false"),
			c("#{4 >= 5}", "false"),
			c("#{1 == 1}", "true"),
			c("#{1 != 1}", "false"),
			c("#{'a' == 'a'}", "true"),         // SpEL == uses equals for non-numbers
			c("#{'a' < 'b'}", "true"),          // Comparable strings
			// Logical.
			c("#{true and false}", "false"),
			c("#{true or false}", "true"),
			c("#{not true}", "false"),
			c("#{!false}", "true"),
			c("#{true && 1 < 2}", "true"),
			// matches: full-match regex (SpEL uses String.matches).
			c("#{'abc123' matches '[a-z]+[0-9]+'}", "true"),
			c("#{'abc' matches 'b'}", "false"),
			// Ternary.
			c("#{1 < 2 ? 'yes' : 'no'}", "yes"),
			c("#{false ? 1 : 2}", "2"),
			// Elvis (non-null, non-empty operand kept; null falls through).
			c("#{'x' ?: 'y'}", "x"),
			c("#{nul() ?: 'dflt'}", "dflt"),
			// Safe navigation: null-safe property access yields null (renders empty).
			c("#{nul()?.name}", ""),
			c("#{ann().name}", "Ann"),
			c("#{ann()?.age}", "30"),
			// Indexing: lists by position, maps by key.
			c("#{nums()[0]}", "1"),
			c("#{nums()[4]}", "5"),
			c("#{ann()['name']}", "Ann"),
			c("#{'abc'[1]}", "b"),
			// Selection .?[] (SpEL: filter; 'this'/#this is the current element).
			c("#{nums().?[this > 3]}", "[4,5]"),
			c("#{nums().?[this % 2 == 0]}", "[2,4]"),
			c("#{people().?[age > 18].![name]}", "[\"Ann\",\"Cy\"]"),
			// Projection .![] (SpEL: map each element).
			c("#{nums().![this * 2]}", "[2,4,6,8,10]"),
			c("#{people().![name]}", "[\"Ann\",\"Bob\",\"Cy\"]"),
			c("#{people().![name + ':' + age]}", "[\"Ann:30\",\"Bob:15\",\"Cy:40\"]")
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parityCases")
	void a01_parity(Case cs) {
		assertEquals(cs.expected(), VR.resolve(cs.expr()), cs.expr());
	}

	@Test void a02_caseCount() {
		assertTrue(parityCases().count() >= 40);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Section B: intentional divergences from SpEL.  Each asserts Juneau's actual behavior.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_elvisIsNullOnly() {
		// SpEL: '' ?: 'dflt' => 'dflt' (Elvis treats empty String as absent).
		// Juneau: Elvis and ':default' fire on null only; "" is kept.
		assertEquals("", VR.resolve("#{'' ?: 'dflt'}"));
		assertEquals("", VR.resolve("#{emptyStr() ?: 'dflt'}"));
		assertEquals("dflt", VR.resolve("#{nul() ?: 'dflt'}"));
		// false and 0 are kept as well.
		assertEquals("false", VR.resolve("#{false ?: 'dflt'}"));
		assertEquals("0", VR.resolve("#{0 ?: 'dflt'}"));
	}

	@Test void b02_bothQuoteStylesAreStrings() {
		// SpEL: single quotes = String; double quotes also String in SpEL, but Juneau additionally keeps
		// legacy double-quoted function arguments working.
		assertEquals("ab", VR.resolve("#{\"a\" + 'b'}"));
		assertEquals("A", VR.resolve("#{upper(\"a\")}"));
	}

	@Test void b03_noTypeOperator() {
		// SpEL: T(java.lang.Math).max(1,2) => 2.  Juneau has no T(); unknown function => literal kept (lenient).
		assertEquals("#{T(java.lang.Math).max(1,2)}", VR.resolve("#{T(java.lang.Math).max(1,2)}"));
	}

	@Test void b04_noMethodCalls() {
		// SpEL: 'abc'.toUpperCase() => ABC.  Juneau has no method invocation on values.
		assertEquals("#{'abc'.toUpperCase()}", VR.resolve("#{'abc'.toUpperCase()}"));
	}

	@Test void b05_noBeanReference() {
		// SpEL: @myBean => bean from the BeanResolver.  Juneau has no @bean references.
		assertEquals("#{@myBean}", VR.resolve("#{@myBean}"));
	}

	@Test void b06_bareWordIsLookupNotString() {
		// Bare words are scope lookups (absent => empty), never strings.
		assertEquals("", VR.resolve("#{hello}"));
	}

	@Test void b07_publicAddIsDouble() {
		// Operator '+' is SpEL-like (int stays int); the public add() function is always double.
		assertEquals("3", VR.resolve("#{1 + 2}"));
		assertEquals("3", VR.resolve("#{add(1,2)}"));   // rendered without trailing .0 by the formatter
	}

	@Test void b08_stringIndexOutOfRange() {
		assertEquals("", VR.resolve("#{'abc'[5]}"));   // lenient: null renders empty
		var strict = VarResolver.create().defaultFunctions().strict(true).build();
		assertThrows(ExprException.class, () -> strict.resolve("#{'abc'[5]}"));
		assertThrows(ExprException.class, () -> strict.resolve("#{'abc'['x']}"));
	}
}
