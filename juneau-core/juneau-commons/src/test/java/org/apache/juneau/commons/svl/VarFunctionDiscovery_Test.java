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

/**
 * Phase E discovery wiring tests — verify that {@link VarResolver#DEFAULT} auto-registers the
 * built-in catalog and that explicit registrations override built-ins on name collision.
 */
class VarFunctionDiscovery_Test extends TestBase {

	/** Override built-in {@code upper(...)} to force-uppercase + bang for collision testing. */
	public static class BangUpper extends TypedFunction {
		@Override public String name() { return "upper"; }
		public String invoke(String s) { return s.toUpperCase(Locale.ROOT) + "!"; }
	}

	@Test void defaultResolverHasBuiltins() {
		// Sample one function from each Phase D category.
		assertEquals("FOO", VarResolver.DEFAULT.resolve("#{upper(foo)}"));    // string
		assertEquals("3", VarResolver.DEFAULT.resolve("#{toInt(3)}"));        // type-conv
		assertEquals("5", VarResolver.DEFAULT.resolve("#{add(2, 3)}"));       // arithmetic
		assertEquals("true", VarResolver.DEFAULT.resolve("#{eq(a, a)}"));     // boolean
		assertEquals("yes", VarResolver.DEFAULT.resolve("#{if(true, yes, no)}"));  // conditional
		assertEquals("true", VarResolver.DEFAULT.resolve("#{match(abc, \"a.c\")}")); // regex
		assertEquals("Zm9v", VarResolver.DEFAULT.resolve("#{base64Encode(foo)}"));   // encoding
	}

	@Test void explicitRegistrationOverridesBuiltin() {
		var vr = VarResolver.create().defaultFunctions().functions(BangUpper.class).build();
		assertEquals("HELLO!", vr.resolve("#{upper(hello)}"));
	}

	@Test void unknownFunctionFailsLazily() {
		// Unknown function name resolves to an exception at resolve-time.
		var vr = VarResolver.DEFAULT;
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.resolve("#{nosuchfn(x)}"));
		assertTrue(ex.getMessage().contains("nosuchfn"), () -> "Expected message to mention function name; got: " + ex.getMessage());
	}

	@Test void emptyCatalogResolverHasNoBuiltins() {
		// VarResolver.create().build() — no .defaultFunctions() — still resolves Vars but not
		// any function references.
		var vr = VarResolver.create().defaultVars().build();
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.resolve("#{upper(foo)}"));
		assertTrue(ex.getMessage().contains("upper"), () -> "Expected message to mention 'upper'; got: " + ex.getMessage());
	}

	@Test void defaultFunctionsRegistersAllCategories() {
		// Spot-check that every Phase D category contributes at least one function.
		var b = VarResolver.create().defaultFunctions();
		var seen = new HashSet<String>();
		// Build to inspect the resulting map indirectly via resolve calls.
		var vr = b.build();
		for (var template : List.of(
				"#{upper(x)}",          // StringFunctions
				"#{toInt(1)}",          // TypeConversionFunctions
				"#{add(1, 2)}",         // ArithmeticFunctions
				"#{and(true, true)}",   // BooleanFunctions
				"#{coalesce(\"\", x)}", // ConditionalFunctions
				"#{match(abc, abc)}",   // RegexFunctions
				"#{urlEncode(x)}",      // EncodingFunctions
				"#{formatDate(0)}",     // DateFunctions
				"#{uuid()}",            // RandomFunctions
				"#{size(\"[]\")}"       // JsonFunctions
		)) {
			var v = vr.resolve(template);
			assertNotNull(v, () -> "Template '" + template + "' returned null");
			seen.add(template);
		}
		assertEquals(10, seen.size());
	}
}
