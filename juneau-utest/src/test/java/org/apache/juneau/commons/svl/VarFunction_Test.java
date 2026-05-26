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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@link VarFunction} SPI + {@link TypedFunction} reflection-based dispatch +
 * builder registration + lazy-fail for unknown functions.
 */
class VarFunction_Test extends TestBase {

	/** Simple typed-function: upper-case its single string arg. */
	public static class Upper extends TypedFunction {
		@Override public String name() { return "upper"; }
		public String invoke(String s) { return s == null ? "" : s.toUpperCase(); }
	}

	/** Multi-arity typed-function with int args. */
	public static class Sub extends TypedFunction {
		@Override public String name() { return "sub"; }
		public String invoke(int a, int b) { return String.valueOf(a - b); }
	}

	/** Variadic typed-function with String[] tail. */
	public static class Concat extends TypedFunction {
		@Override public String name() { return "concat"; }
		public String invoke(String[] parts) {
			var sb = new StringBuilder();
			for (var p : parts) sb.append(p);
			return sb.toString();
		}
	}

	/** Typed function with leading session arg + int. */
	public static class Echo extends TypedFunction {
		@Override public String name() { return "echo"; }
		public String invoke(VarResolverSession session, String s) {
			assertNotNull(session);
			return s;
		}
	}

	/** Direct VarFunction (not TypedFunction) — bypasses ArgCoercer. */
	public static class DirectFn implements VarFunction {
		@Override public String name() { return "direct"; }
		@Override public int minArity() { return 1; }
		@Override public int maxArity() { return 1; }
		@Override public String invoke(VarResolverSession session, List<Object> args) {
			return "got=" + args.get(0);
		}
	}

	@Test void a01_typedFunctionSingleArg() {
		var vr = VarResolver.create().functions(new Upper()).build();
		assertEquals("HELLO", vr.resolve("#{upper(hello)}"));
	}

	@Test void a02_typedFunctionMultiArgIntCoercion() {
		var vr = VarResolver.create().functions(new Sub()).build();
		assertEquals("3", vr.resolve("#{sub(10, 7)}"));
	}

	@Test void a03_typedFunctionVariadicStringArray() {
		var vr = VarResolver.create().functions(new Concat()).build();
		assertEquals("abc", vr.resolve("#{concat(a, b, c)}"));
	}

	@Test void a04_typedFunctionWithSessionArg() {
		var vr = VarResolver.create().functions(new Echo()).build();
		assertEquals("hello", vr.resolve("#{echo(hello)}"));
	}

	@Test void a05_directVarFunctionPassthrough() {
		var vr = VarResolver.create().functions(new DirectFn()).build();
		assertEquals("got=hello", vr.resolve("#{direct(hello)}"));
	}

	@Test void a06_unknownFunctionLazyFail() {
		var vr = VarResolver.create().build();
		// Compile succeeds (lazy-fail per OQA #4)…
		var tpl = vr.compile("hello #{nope()} world");
		assertNotNull(tpl);
		// …but resolve throws.
		var ex = assertThrows(IllegalArgumentException.class, () -> tpl.resolve(vr.createSession()));
		assertTrue(ex.getMessage().contains("No such function 'nope'"), ex.getMessage());
	}

	@Test void a07_arityCheckingTooFewArgs() {
		var vr = VarResolver.create().functions(new Sub()).build();
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.resolve("#{sub(1)}"));
		assertTrue(ex.getMessage().contains("Function 'sub' expected"), ex.getMessage());
	}

	@Test void a08_arityCheckingTooManyArgs() {
		var vr = VarResolver.create().functions(new Sub()).build();
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.resolve("#{sub(1, 2, 3)}"));
		assertTrue(ex.getMessage().contains("Function 'sub' expected"), ex.getMessage());
	}

	@Test void a09_typedFunctionByClass() {
		var vr = VarResolver.create().functions(Upper.class).build();
		assertEquals("HELLO", vr.resolve("#{upper(hello)}"));
	}

	@Test void a10_collisionLastRegisteredWins() {
		// First register a stub; second registration replaces it.
		var stub = new VarFunction() {
			@Override public String name() { return "upper"; }
			@Override public int minArity() { return 1; }
			@Override public int maxArity() { return 1; }
			@Override public String invoke(VarResolverSession s, List<Object> a) { return "stub"; }
		};
		var vr = VarResolver.create().functions(stub).functions(new Upper()).build();
		assertEquals("HELLO", vr.resolve("#{upper(hello)}"));
	}

	@Test void a11_compositionWithVarLookup() {
		System.setProperty("VarFunction_Test.composition", "fred");
		try {
			var vr = VarResolver.create()
				.vars(org.apache.juneau.commons.svl.vars.SystemPropertiesVar.class)
				.functions(new Upper())
				.build();
			assertEquals("FRED", vr.resolve("#{upper($S{VarFunction_Test.composition})}"));
		} finally {
			System.clearProperty("VarFunction_Test.composition");
		}
	}

	@Test void a12_nestedScriptCalls() {
		var vr = VarResolver.create().functions(new Upper(), new Concat()).build();
		assertEquals("ABC", vr.resolve("#{upper(#{concat(a, b, c)})}"));
	}
}
