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
 * Targeted coverage tests for {@link TypedFunction}, filling gaps not exercised by
 * {@link VarFunction_Test} / {@link ArgCoercer_Test} (multi-overload dispatch selection,
 * null/checked-exception invoke results, and the "no invoke method" construction failure).
 */
@SuppressWarnings({
	"java:S5778" // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class TypedFunction_Coverage_Test extends TestBase {

	/** Two fixed-arity overloads - dispatch must skip the (sorted-first) 3-arg one to reach the 2-arg one. */
	public static class Substring extends TypedFunction {
		@Override public String name() { return "substring"; }
		public String invoke(String s, int start) { return s.substring(start); }
		public String invoke(String s, int start, int end) { return s.substring(start, end); }
	}

	/** Fixed-arity overload combined with a variadic overload of a smaller fixed count. */
	public static class MultiVariadic extends TypedFunction {
		@Override public String name() { return "multi"; }
		public String invoke(String a, String b) { return "pair:" + a + "," + b; }
		public String invoke(String[] parts) { return "variadic:" + parts.length; }
	}

	/** Returns null to exercise the null-result-becomes-empty-string branch. */
	public static class NullReturning extends TypedFunction {
		@Override public String name() { return "nullfn"; }
		@SuppressWarnings({
			"unused" // Parameter required to match the 1-arg invoke() signature dispatched to via reflection.
		})
		public String invoke(String s) { return null; }
	}

	/** Throws a checked (non-RuntimeException) exception from invoke. */
	public static class CheckedThrower extends TypedFunction {
		@Override public String name() { return "checkedthrow"; }
		@SuppressWarnings({
			"unused" // Parameter required to match the 1-arg invoke() signature dispatched to via reflection.
		})
		public String invoke(String s) throws java.io.IOException { throw new java.io.IOException("boom"); }
	}

	/** Throws an unchecked exception from invoke - should propagate as-is, not get wrapped. */
	public static class RuntimeThrower extends TypedFunction {
		@Override public String name() { return "runtimethrow"; }
		@SuppressWarnings({
			"unused" // Parameter required to match the 1-arg invoke() signature dispatched to via reflection.
		})
		public String invoke(String s) { throw new IllegalStateException("kaboom"); }
	}

	/** No invoke(...) method declared at all - construction must fail. */
	public static class NoInvoke extends TypedFunction {
		@Override public String name() { return "noinvoke"; }
	}

	@Test
	void a01_multiOverload_dispatchSkipsNonMatchingArityToFindLaterMatch() {
		var vr = VarResolver.create().functions(new Substring()).build();
		assertEquals("llo", vr.resolve("#{substring(hello, 2)}"));
		assertEquals("el", vr.resolve("#{substring(hello, 1, 3)}"));
	}

	@Test
	void a02_fixedArityOverloadCombinedWithVariadic_fixedArityPreferredWhenMatching() {
		var vr = VarResolver.create().functions(new MultiVariadic()).build();
		assertEquals("pair:a,b", vr.resolve("#{multi(a, b)}"));
		assertEquals("variadic:3", vr.resolve("#{multi(a, b, c)}"));
	}

	@Test
	void a03_variadicOverload_arityBelowFixedCount_notAccepted() {
		// MultiVariadic's variadic overload has fixedCount 0 (String[] alone), so any n >= 0 matches it,
		// but the 2-arg fixed overload takes precedence for n==2. Verify a 0-arg call still resolves via
		// the variadic overload (n=0 >= fixedCount=0), confirming acceptsArity's variadic branch works both ways.
		var vr = VarResolver.create().functions(new MultiVariadic()).build();
		assertEquals("variadic:0", vr.resolve("#{multi()}"));
	}

	@Test
	void a04_nullInvokeResult_resolvesToEmptyString() {
		var vr = VarResolver.create().functions(new NullReturning()).build();
		assertEquals("", vr.resolve("#{nullfn(x)}"));
	}

	@Test
	void a05_checkedExceptionFromInvoke_wrappedAsIllegalArgumentException() {
		var vr = VarResolver.create().functions(new CheckedThrower()).build();
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.resolve("#{checkedthrow(x)}"));
		assertTrue(ex.getMessage().contains("checkedthrow"), ex.getMessage());
		assertTrue(ex.getMessage().contains("boom"), ex.getMessage());
	}

	@Test
	void a06_runtimeExceptionFromInvoke_propagatedUnwrapped() {
		var vr = VarResolver.create().functions(new RuntimeThrower()).build();
		var ex = assertThrows(IllegalStateException.class, () -> vr.resolve("#{runtimethrow(x)}"));
		assertEquals("kaboom", ex.getMessage());
	}

	@Test
	void a07_noInvokeMethodDeclared_constructionThrows() {
		var ex = assertThrows(IllegalArgumentException.class, NoInvoke::new);
		assertTrue(ex.getMessage().contains("must declare a public invoke"), ex.getMessage());
	}
}
