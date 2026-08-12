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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pinning tests for the {@link StringCtor @StringCtor} annotation.
 */
class StringCtor_Test extends TestBase {

	MarshallingContext bc = MarshallingContext.DEFAULT;

	//-----------------------------------------------------------------------------------------------------------------
	// @StringCtor names a method that doesn't match any of the conventional names.
	//-----------------------------------------------------------------------------------------------------------------

	@StringCtor("of")
	public static class A {
		public final String value;
		private A(String value) { this.value = value; }
		public static A of(String value) { return new A(value); }
		// Named "unwrap" so it would never be picked up by the conventional name-based lookup.
		public static String unwrap(A a) { return a.value; }
	}

	@Test void a01_annotatedFactoryMethod_used() throws Exception {
		var t = bc.getClassMeta(A.class);
		var a = t.newInstanceFromString(null, "foo");
		assertEquals("foo", a.value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @StringCtor takes precedence over a conventionally-named method that would otherwise be chosen.
	//-----------------------------------------------------------------------------------------------------------------

	@StringCtor("real")
	public static class B {
		public final String value;
		private B(String tag, String value) { this.value = tag + ":" + value; }
		public static B real(String value) { return new B("real", value); }
		// Conventionally named; would be picked up by the 7-way name guessing if @StringCtor were absent/ignored.
		public static B valueOf(String value) { return new B("valueOf", value); }
	}

	@Test void a02_annotatedFactoryMethod_takesPrecedenceOverConventionalName() throws Exception {
		var t = bc.getClassMeta(B.class);
		var b = t.newInstanceFromString(null, "foo");
		assertEquals("real:foo", b.value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unannotated classes still fall back to the existing conventional-name / string-constructor lookup.
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public final String value;
		private C(String value) { this.value = value; }
		public static C valueOf(String value) { return new C(value); }
	}

	@Test void a03_unannotatedClass_fallsBackToConventionalName() throws Exception {
		var t = bc.getClassMeta(C.class);
		var c = t.newInstanceFromString(null, "foo");
		assertEquals("foo", c.value);
	}

	public static class D {
		public final String value;
		public D(String value) { this.value = value; }
	}

	@Test void a04_unannotatedClass_fallsBackToStringConstructor() throws Exception {
		var t = bc.getClassMeta(D.class);
		var d = t.newInstanceFromString(null, "foo");
		assertEquals("foo", d.value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Regression:  findFromStringMethod() must not NPE when marshallingContext is null.
	// This is the case for the args-array ClassMeta (used for RRPC method-parameter arrays), which is built via
	// the context-less constructor below and never has an annotation provider to consult for @StringCtor.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a05_argsArrayClassMeta_nullMarshallingContext_doesNotThrowAndFallsBack() {
		var argsType = new ClassMeta<>(List.of(bc.getClassMeta(String.class)));
		assertTrue(argsType.isArgs());
		assertNull(argsType.getMarshallingContext());
		assertFalse(argsType.canCreateNewInstanceFromString(null));
	}
}
