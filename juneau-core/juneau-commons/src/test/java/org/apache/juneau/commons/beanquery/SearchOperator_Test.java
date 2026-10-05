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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class SearchOperator_Test extends TestBase {

	//====================================================================================================
	// a - create()/build() defaults
	//====================================================================================================

	@Test
	void a01_create_hasDefaults() {
		var op = SearchOperator.create("$foo", "help text").build();
		assertBean(op, "name,help,custom,combinator,minArgs,maxArgs,types,typedArgs", "$foo,help text,true,false,1,-1,[],false");
	}

	//====================================================================================================
	// b - build() validation
	//====================================================================================================

	@Test
	void b01_build_nullName_throws() {
		var b = SearchOperator.create("$foo", "help text").name(null);
		assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test
	void b02_build_blankName_throws() {
		var b = SearchOperator.create("$foo", "help text").name("   ");
		assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test
	void b03_build_maxLessThanMin_throws() {
		var b = SearchOperator.create("$foo", "help text").minArgs(5).maxArgs(2);
		assertThrows(IllegalArgumentException.class, b::build);
	}

	//====================================================================================================
	// c - copy() round-trip/independence
	//====================================================================================================

	@Test
	void c01_copy_roundTrips_allFields() {
		SearchPredicate pred = (cell, args) -> true;
		var op = SearchOperator.create("$foo", "help text")
			.minArgs(2).maxArgs(4).types(SearchType.NUMERIC, SearchType.TEXT)
			.typedArgs(true)
			.predicate(pred)
			.extension(String.class, "ext-value")
			.build();

		var copy = op.copy().build();

		assertEquals(op, copy);
		assertEquals(op.name(), copy.name());
		assertEquals(op.help(), copy.help());
		assertEquals(op.minArgs(), copy.minArgs());
		assertEquals(op.maxArgs(), copy.maxArgs());
		assertEquals(op.types(), copy.types());
		assertEquals(op.isCustom(), copy.isCustom());
		assertEquals(op.isCombinator(), copy.isCombinator());
		assertEquals(op.typedArgs(), copy.typedArgs());
		assertTrue(copy.typedArgs());
		assertSame(op.predicate(), copy.predicate());
		assertEquals(op.extension(String.class), copy.extension(String.class));
	}

	@Test
	void c02_copy_producesIndependentBuilder() {
		var op = SearchOperator.create("$foo", "help text").minArgs(1).maxArgs(1).build();
		var builder = op.copy();
		builder.name("$bar").minArgs(9).maxArgs(9).types(SearchType.NUMERIC).extension(String.class, "mutated");

		// The original instance must be unaffected by mutating the builder returned by copy().
		assertBean(op, "name,minArgs,maxArgs,types", "$foo,1,1,[]");
		assertTrue(op.extension(String.class).isEmpty());

		var mutated = builder.build();
		assertBean(mutated, "name,minArgs", "$bar,9");
	}

	@Test
	void c03_copy_customOperatorRenamed_stillWorks() {
		// Regression guard: the new built-in rename restriction (section f) must not affect customs.
		var op = SearchOperator.create("$foo", "help text").build();
		var renamed = op.copy().name("$bar").build();
		assertEquals("$bar", renamed.name());
		assertTrue(renamed.isCustom());
	}

	//====================================================================================================
	// d - typed extension()
	//====================================================================================================

	@Test
	void d01_extension_absentByDefault() {
		var op = SearchOperator.create("$foo", "help text").build();
		assertEquals(Optional.empty(), op.extension(Boolean.class));
	}

	@Test
	void d02_extension_roundTrips() {
		var op = SearchOperator.create("$foo", "help text").extension(String.class, "foo").build();
		assertEquals(Optional.of("foo"), op.extension(String.class));
	}

	@Test
	void d03_extension_distinctTypeKeysDoNotCollide() {
		var op = SearchOperator.create("$foo", "help text")
			.extension(Integer.class, 42)
			.extension(String.class, "foo")
			.build();
		assertEquals(Optional.of(42), op.extension(Integer.class));
		assertEquals(Optional.of("foo"), op.extension(String.class));
	}

	@Test
	void d04_extension_builtInstanceIsImmuneToLaterBuilderMutation() {
		var builder = SearchOperator.create("$foo", "help text").extension(String.class, "original");
		var op = builder.build();

		// Mutating the SAME builder after build() must not leak through to the already-built instance.
		builder.extension(String.class, "mutated");

		assertEquals(Optional.of("original"), op.extension(String.class));
	}

	//====================================================================================================
	// e - equals()/hashCode()
	//====================================================================================================

	@Test
	void e01_equals_sameFields_true() {
		SearchPredicate pred = (cell, args) -> true;
		var a = SearchOperator.create("$foo", "help text").minArgs(1).maxArgs(2).types(SearchType.NUMERIC)
			.predicate(pred).extension(String.class, "x").build();
		var b = SearchOperator.create("$foo", "help text").minArgs(1).maxArgs(2).types(SearchType.NUMERIC)
			.predicate(pred).extension(String.class, "x").build();
		assertEquals(a, b);
	}

	@Test
	@SuppressWarnings({
		"java:S3415" // Deliberate: a must be the equals() receiver so the non-OWNER and null overloads are exercised.
	})
	void e02_equals_differentField_false() {
		var a = SearchOperator.create("$foo", "help text").build();
		var b = SearchOperator.create("$bar", "help text").build();
		var c = SearchOperator.create("$foo", "different help").build();
		assertNotEquals(a, b);
		assertNotEquals(a, c);
		assertNotEquals(a, "$foo");
		assertNotEquals(a, null);
	}

	@Test
	void e03_hashCode_consistentWithEquals() {
		SearchPredicate pred = (cell, args) -> true;
		var a = SearchOperator.create("$foo", "help text").minArgs(1).maxArgs(2).types(SearchType.NUMERIC)
			.predicate(pred).extension(String.class, "x").build();
		var b = SearchOperator.create("$foo", "help text").minArgs(1).maxArgs(2).types(SearchType.NUMERIC)
			.predicate(pred).extension(String.class, "x").build();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	//====================================================================================================
	// f - copy() of a built-in: renaming is rejected at build(), help-text-only changes stay legal
	//====================================================================================================

	@Test
	void f01_copy_builtinRenamed_throwsAtBuild() {
		var builtin = SearchOperatorSet.standard().get("$eq");
		var b = builtin.copy().name("$renamed");
		var e = assertThrows(IllegalArgumentException.class, b::build);
		assertEquals("Built-in operator '$eq' cannot be renamed; only help text may be changed on a built-in copy.", e.getMessage());
	}

	@Test
	void f02_copy_builtinHelpTextOnlyChange_buildsAndEvaluatesCorrectly() {
		var builtin = SearchOperatorSet.standard().get("$eq");
		var copy = builtin.copy().help("New help text").build();

		assertEquals(builtin.name(), copy.name());
		assertEquals("New help text", copy.help());
		assertFalse(copy.isCustom());

		var node = SearchExpression.func(copy, List.of(SearchExpression.literal("OPEN", false)));
		assertTrue(InMemoryMatch.matches(node, "OPEN", SearchType.TEXT));
		assertFalse(InMemoryMatch.matches(node, "open", SearchType.TEXT));
	}

	//====================================================================================================
	// g - typedArgs
	//====================================================================================================

	@Test
	void g01_typedArgs_defaultFalse_setTrue_copyKeeps() {
		var op = SearchOperator.create("$near", "Fuzzy match. Example: $near(x)").build();
		assertFalse(op.typedArgs());
		var typed = SearchOperator.create("$near", "Fuzzy match. Example: $near(x)").typedArgs(true).build();
		assertTrue(typed.typedArgs());
		assertTrue(typed.copy().build().typedArgs());
		assertNotEquals(op, typed);
	}

	@Test
	void g02_typedArgs_builtinsFalse() {
		assertFalse(SearchOperatorSet.standard().get("$eq").typedArgs());
	}
}
