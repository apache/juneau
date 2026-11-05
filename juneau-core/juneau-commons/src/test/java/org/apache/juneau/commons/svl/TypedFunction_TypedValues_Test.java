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
import org.apache.juneau.commons.svl.functions.*;
import org.junit.jupiter.api.*;

/** Tests typed argument / result handling in {@link TypedFunction} and {@link ArgCoercer}. */
class TypedFunction_TypedValues_Test extends TestBase {

	/** Mixed-arity function for overload-selection checks. */
	public static class Pick extends TypedFunction {
		@Override public String name() { return "pick"; }
		public String invoke(String a, String b) { return "2:" + a + b; }
		public String invoke(String a, String b, String c) { return "3:" + a + b + c; }
	}

	/** Returns a non-String object. */
	public static class Num extends TypedFunction {
		@Override public String name() { return "num"; }
		public Long invoke(long a) { return a * 2; }
	}

	@Test void a01_typedArgAndStringArgBothWork() throws Exception {
		var abs = new ArithmeticFunctions.Abs();
		assertEquals("3", abs.invokeValue(null, List.of(-3L)));
		assertEquals("3", abs.invoke(null, List.of("-3")));
	}

	@Test void a02_nullToPrimitiveThrows() {
		var abs = new ArithmeticFunctions.Abs();
		var args = new ArrayList<Object>();
		args.add(null);
		var e = assertThrows(IllegalArgumentException.class, () -> abs.invokeValue(null, args));
		assertTrue(e.getMessage().contains("abs") && e.getMessage().contains("arg 0"), e.getMessage());
	}

	@Test void a03_arityOverloadsUnchanged() {
		var p = new Pick();
		assertEquals("2:ab", p.invoke(null, List.of("a", "b")));
		assertEquals("3:abc", p.invoke(null, List.of("a", "b", "c")));
	}

	@Test void a04_invokeValueReturnsUnstringifiedResult() throws Exception {
		var n = new Num();
		assertEquals(8L, n.invokeValue(null, List.of(4)));
		assertEquals("8", n.invoke(null, List.of("4")));
	}

	@Test void a05_coerceOneTypedSources() {
		assertEquals(3.0, ArgCoercer.coerceOne("f", 0, double.class, 3));
		assertEquals(3, ArgCoercer.coerceOne("f", 0, int.class, 3L));
		assertEquals(3L, ArgCoercer.coerceOne("f", 0, Long.class, 3.0));
		assertEquals("3", ArgCoercer.coerceOne("f", 0, String.class, 3.0));
		assertEquals("true", ArgCoercer.coerceOne("f", 0, String.class, true));
		assertEquals(true, ArgCoercer.coerceOne("f", 0, boolean.class, Boolean.TRUE));
		assertNull(ArgCoercer.coerceOne("f", 0, Integer.class, null));
		assertNull(ArgCoercer.coerceOne("f", 0, Object.class, null));
		assertEquals(7L, ArgCoercer.coerceOne("f", 0, Object.class, 7L));
	}

	@Test void a06_nullToPrimitiveBooleanThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ArgCoercer.coerceOne("f", 2, boolean.class, null));
		assertTrue(e.getMessage().contains("'f'") && e.getMessage().contains("arg 2"), e.getMessage());
	}

	@Test void a07_nullToBoxedBooleanStaysNull() {
		assertNull(ArgCoercer.coerceOne("f", 0, Boolean.class, null));
	}

	@Test void a08_floatToString() {
		assertEquals("3", ArgCoercer.coerceOne("f", 0, String.class, 3.0f));
		assertEquals("2.5", ArgCoercer.coerceOne("f", 0, String.class, 2.5f));
	}
}
