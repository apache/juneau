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

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Tests the {@link ArgCoercer} argument-coercion table for {@code #{...}} function dispatch.
 *
 * <p>
 * Drives {@link ArgCoercer} indirectly through {@link TypedFunction} subclasses so the integration
 * path (function dispatch → coercion → typed invoke) is exercised end-to-end.
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class ArgCoercer_Test extends TestBase {

	public static class IntFn extends TypedFunction {
		@Override public String name() { return "intfn"; }
		public String invoke(int x) { return "int=" + x; }
	}

	public static class LongFn extends TypedFunction {
		@Override public String name() { return "longfn"; }
		public String invoke(long x) { return "long=" + x; }
	}

	public static class DoubleFn extends TypedFunction {
		@Override public String name() { return "dblfn"; }
		public String invoke(double x) { return "dbl=" + x; }
	}

	public static class BoolFn extends TypedFunction {
		@Override public String name() { return "boolfn"; }
		public String invoke(boolean x) { return "bool=" + x; }
	}

	public static class ArrFn extends TypedFunction {
		@Override public String name() { return "arrfn"; }
		public String invoke(String[] xs) { return "arr=" + Arrays.toString(xs); }
	}

	/**
	 * Function with a non-final {@code String[]} param so it triggers JSON-array shortcut
	 * coercion (rather than variadic gather) per OQA #7.
	 */
	public static class JsonArrFn extends TypedFunction {
		@Override public String name() { return "jsonArr"; }
		public String invoke(String[] xs, String tag) { return tag + "=" + Arrays.toString(xs); }
	}

	public static class ObjFn extends TypedFunction {
		@Override public String name() { return "objfn"; }
		public String invoke(Object o) { return "obj=" + o; }
	}

	private VarResolver vr() {
		return VarResolver.create()
			.functions(new IntFn(), new LongFn(), new DoubleFn(), new BoolFn(),
				new ArrFn(), new JsonArrFn(), new ObjFn())
			.build();
	}

	@Test void a01_intCoercion() {
		assertEquals("int=42", vr().resolve("#{intfn(42)}"));
		assertEquals("int=-7", vr().resolve("#{intfn(-7)}"));
	}

	@Test void a02_intCoercionFailsOnNonNumeric() {
		var ex = assertThrows(IllegalArgumentException.class, () -> vr().resolve("#{intfn(notanumber)}"));
		assertTrue(ex.getMessage().contains("cannot coerce"), ex.getMessage());
	}

	@Test void a03_longCoercion() {
		assertEquals("long=999999999999", vr().resolve("#{longfn(999999999999)}"));
	}

	@Test void a04_doubleCoercion() {
		assertEquals("dbl=3.14", vr().resolve("#{dblfn(3.14)}"));
	}

	@Test void a05_booleanCoercionTrueTokens() {
		assertEquals("bool=true", vr().resolve("#{boolfn(true)}"));
		assertEquals("bool=true", vr().resolve("#{boolfn(TRUE)}"));
		assertEquals("bool=true", vr().resolve("#{boolfn(1)}"));
		assertEquals("bool=true", vr().resolve("#{boolfn(yes)}"));
		assertEquals("bool=true", vr().resolve("#{boolfn(YES)}"));
		assertEquals("bool=true", vr().resolve("#{boolfn(on)}"));
		assertEquals("bool=true", vr().resolve("#{boolfn(ON)}"));
	}

	@Test void a06_booleanCoercionFalseTokens() {
		assertEquals("bool=false", vr().resolve("#{boolfn(false)}"));
		assertEquals("bool=false", vr().resolve("#{boolfn(0)}"));
		assertEquals("bool=false", vr().resolve("#{boolfn(no)}"));
		assertEquals("bool=false", vr().resolve("#{boolfn(off)}"));
		// Empty / quoted-empty coerces to false.
		assertEquals("bool=false", vr().resolve("#{boolfn(\"\")}"));
	}

	@Test void a07_booleanCoercionRejectsUnknown() {
		var ex = assertThrows(IllegalArgumentException.class, () -> vr().resolve("#{boolfn(perhaps)}"));
		assertTrue(ex.getMessage().contains("cannot coerce 'perhaps' to boolean"), ex.getMessage());
	}

	@Test void a08_stringArrayJsonShortcutInNonFinalSlot() {
		// JSON-array shortcut form must be quoted in the script body so the recursive-descent
		// parser doesn't split on the inner commas. Once delivered to ArgCoercer as a single
		// String, the JSON-array parse handles the rest. The String[] slot must be NON-final
		// for JSON-array coercion to apply (a final String[] is variadic per OQA #7's "final
		// slot collects excess" rule).
		assertEquals("X=[a, b, c]", vr().resolve("#{jsonArr(\"[\\\"a\\\",\\\"b\\\",\\\"c\\\"]\", X)}"));
	}

	@Test void a09_stringArrayVariadicGather() {
		// Final String[] slot — args are gathered, not JSON-parsed.
		assertEquals("arr=[a, b, c]", vr().resolve("#{arrfn(a, b, c)}"));
	}

	@Test void a09b_stringArrayBareWrappedInNonFinalSlot() {
		assertEquals("X=[hello]", vr().resolve("#{jsonArr(hello, X)}"));
	}

	@Test void a10_objectPassthrough() {
		assertEquals("obj=hello", vr().resolve("#{objfn(hello)}"));
	}
}
