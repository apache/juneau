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
package org.apache.juneau.common.reflect;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ReflectionMap_Test extends TestBase {

	private static ReflectionMap.Builder<Number> create() {
		return ReflectionMap.create(Number.class);
	}

	private static void checkEntries(ReflectionMap<?> m, boolean hasClass, boolean hasMethods, boolean hasFields, boolean hasConstructors) {
		assertEquals(m.classEntries.size() == 0, ! hasClass);
		assertEquals(m.methodEntries.size() == 0, ! hasMethods);
		assertEquals(m.fieldEntries.size() == 0, ! hasFields);
		assertEquals(m.constructorEntries.size() == 0, ! hasConstructors);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Class names
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	class A_Class {

		static class A1 {}
		static class A2 {}

		static ReflectionMap<Number>
			A1_SIMPLE = create().append("A1", 1).build(),
			A1b_SIMPLE = create().append("ReflectionMap_Test$A_Class$A1", 1).build(),
			A1_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$A_Class$A1", 1).build();  // Note this could be a static field.

		@Test void a01_checkEntries() {
			checkEntries(A1_SIMPLE, true, false, false, false);
			checkEntries(A1_FULL, true, false, true, false);
		}

		private static void test(Class<?> c, boolean match_A1_SIMPLE, boolean match_A1b_SIMPLE, boolean match_A1_FULL) {
			assertEquals(match_A1_SIMPLE, A1_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_A1b_SIMPLE, A1b_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_A1_FULL, A1_FULL.find(c).findAny().isPresent());

			assertEquals(match_A1_SIMPLE, A1_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_A1b_SIMPLE, A1b_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_A1_FULL, A1_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());

			assertFalse(A1_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(A1b_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(A1_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());

			assertEquals(match_A1_SIMPLE, A1_SIMPLE.find(c).count() > 0);
			assertEquals(match_A1b_SIMPLE, A1b_SIMPLE.find(c).count() > 0);
			assertEquals(match_A1_FULL, A1_FULL.find(c).count() > 0);

			assertEquals(match_A1_SIMPLE, A1_SIMPLE.find(c).filter(v -> v instanceof Integer).count() > 0);
			assertEquals(match_A1b_SIMPLE, A1b_SIMPLE.find(c).filter(v -> v instanceof Integer).count() > 0);
			assertEquals(match_A1_FULL, A1_FULL.find(c).filter(v -> v instanceof Integer).count() > 0);

			assertFalse(A1_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(A1b_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(A1_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
		}

		@Test void a02_find() {
			test(A1.class, true, true, true);
			test(A2.class, false, false, false);
			test(null, false, false, false);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method names
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	class B_Method {

		static class B1 {
			public void m1() { /* no-op */ }
			public void m1(int x) { /* no-op */ }
			public void m1(String x) { /* no-op */ }
			public void m1(String x, int y) { /* no-op */ }
			public void m2(int x) { /* no-op */ }
		}
		static class B2 {
			public void m1() { /* no-op */ }
		}

		static ReflectionMap<Number>
			B1m1_SIMPLE = create().append("B1.m1", 1).build(),
			B1m1i_SIMPLE = create().append("B1.m1(int)", 1).build(),
			B1m1s_SIMPLE = create().append("B1.m1(String)", 1).build(),
			B1m1ss_SIMPLE = create().append("B1.m1(java.lang.String)", 1).build(),
			B1m1si_SIMPLE = create().append("B1.m1(String,int)", 1).build(),
			B1m1ssi_SIMPLE = create().append("B1.m1(java.lang.String , int)", 1).build(),
			B1m1_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$B_Method$B1.m1", 1).build(),
			B1m1i_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$B_Method$B1.m1(int)", 1).build(),
			B1m1s_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$B_Method$B1.m1(String)", 1).build(),
			B1m1ss_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$B_Method$B1.m1(java.lang.String)", 1).build(),
			B1m1si_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$B_Method$B1.m1(String,int)", 1).build(),
			B1m1ssi_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$B_Method$B1.m1(java.lang.String , int)", 1).build();

		@Test void a01_checkEntries() {
			checkEntries(B1m1_SIMPLE, false, true, true, false);
			checkEntries(B1m1i_SIMPLE, false, true, false, false);
			checkEntries(B1m1s_SIMPLE, false, true, false, false);
			checkEntries(B1m1ss_SIMPLE, false, true, false, false);
			checkEntries(B1m1si_SIMPLE, false, true, false, false);
			checkEntries(B1m1ssi_SIMPLE, false, true, false, false);
			checkEntries(B1m1_FULL, false, true, true, false);
			checkEntries(B1m1i_FULL, false, true, false, false);
			checkEntries(B1m1s_FULL, false, true, false, false);
			checkEntries(B1m1ss_FULL, false, true, false, false);
			checkEntries(B1m1si_FULL, false, true, false, false);
			checkEntries(B1m1ssi_FULL, false, true, false, false);
		}

		private static void test(Method m, boolean match_B1m1_SIMPLE, boolean match_B1m1i_SIMPLE, boolean match_B1m1s_SIMPLE, boolean match_B1m1ss_SIMPLE,
				boolean match_B1m1si_SIMPLE, boolean match_B1m1ssi_SIMPLE, boolean match_B1m1_FULL, boolean match_B1m1i_FULL, boolean match_B1m1s_FULL,
				boolean match_B1m1ss_FULL, boolean match_B1m1si_FULL, boolean match_B1m1ssi_FULL) {

			assertEquals(match_B1m1_SIMPLE, B1m1_SIMPLE.find(m).findAny().isPresent());
			assertEquals(match_B1m1i_SIMPLE, B1m1i_SIMPLE.find(m).findAny().isPresent());
			assertEquals(match_B1m1s_SIMPLE, B1m1s_SIMPLE.find(m).findAny().isPresent());
			assertEquals(match_B1m1ss_SIMPLE, B1m1ss_SIMPLE.find(m).findAny().isPresent());
			assertEquals(match_B1m1si_SIMPLE, B1m1si_SIMPLE.find(m).findAny().isPresent());
			assertEquals(match_B1m1ssi_SIMPLE, B1m1ssi_SIMPLE.find(m).findAny().isPresent());
			assertEquals(match_B1m1_FULL, B1m1_FULL.find(m).findAny().isPresent());
			assertEquals(match_B1m1i_FULL, B1m1i_FULL.find(m).findAny().isPresent());
			assertEquals(match_B1m1s_FULL, B1m1s_FULL.find(m).findAny().isPresent());
			assertEquals(match_B1m1ss_FULL, B1m1ss_FULL.find(m).findAny().isPresent());
			assertEquals(match_B1m1si_FULL, B1m1si_FULL.find(m).findAny().isPresent());
			assertEquals(match_B1m1ssi_FULL, B1m1ssi_FULL.find(m).findAny().isPresent());

			assertEquals(match_B1m1_SIMPLE, B1m1_SIMPLE.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1i_SIMPLE, B1m1i_SIMPLE.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1s_SIMPLE, B1m1s_SIMPLE.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1ss_SIMPLE, B1m1ss_SIMPLE.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1si_SIMPLE, B1m1si_SIMPLE.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1ssi_SIMPLE, B1m1ssi_SIMPLE.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1_FULL, B1m1_FULL.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1i_FULL, B1m1i_FULL.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1s_FULL, B1m1s_FULL.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1ss_FULL, B1m1ss_FULL.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1si_FULL, B1m1si_FULL.find(m).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_B1m1ssi_FULL, B1m1ssi_FULL.find(m).filter(v -> v instanceof Integer).findAny().isPresent());

			assertFalse(B1m1_SIMPLE.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1i_SIMPLE.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1s_SIMPLE.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1ss_SIMPLE.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1si_SIMPLE.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1ssi_SIMPLE.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1_FULL.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1i_FULL.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1s_FULL.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1ss_FULL.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1si_FULL.find(m).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(B1m1ssi_FULL.find(m).filter(v -> v instanceof Long).findAny().isPresent());
		}

		@Test void a02_find() throws Exception {
			test(B1.class.getMethod("m1"), true, false, false, false, false, false, true, false, false, false, false, false);
			test(B1.class.getMethod("m1", int.class), true, true, false, false, false, false, true, true, false, false, false, false);
			test(B1.class.getMethod("m1", String.class), true, false, true, true, false, false, true, false, true, true, false, false);
			test(B1.class.getMethod("m1", String.class, int.class), true, false, false, false, true, true, true, false, false, false, true, true);
			test(B1.class.getMethod("m2", int.class), false, false, false, false, false, false, false, false, false, false, false, false);
			test(B2.class.getMethod("m1"), false, false, false, false, false, false, false, false, false, false, false, false);
			test(null, false, false, false, false, false, false, false, false, false, false, false, false);
		}
	
		// Test for generic parameters in method signatures (lines 409-411)
		@Test void a03_generics() throws Exception {
			ReflectionMap<Number> rm = create().append("B1.m1(List<String>)", 1).build();
			// Generic parameters should be stripped, so this should match m1(List)
			// Note: We can't easily test this because Java doesn't preserve generic info at runtime,
			// but we can verify the pattern was parsed without error
			assertEquals(1, rm.methodEntries.size());
		}
	
		// Test for array parameters in method signatures (lines 414-424)
		static class B3 {
			public void m1(String[] x) { /* no-op */ }
			public void m2(String[][] x) { /* no-op */ }
			public void m3(int[] x) { /* no-op */ }
		}
	
		@Test void a04_arrayParams() throws Exception {
			// Single-dimensional array with simple name
			ReflectionMap<Number> rm1 = create().append("B3.m1(String[])", 1).build();
			assertTrue(rm1.find(B3.class.getMethod("m1", String[].class)).findAny().isPresent());
	
			// Single-dimensional array with full name
			ReflectionMap<Number> rm1b = create().append("B3.m1(java.lang.String[])", 1).build();
			assertTrue(rm1b.find(B3.class.getMethod("m1", String[].class)).findAny().isPresent());
	
			// Multi-dimensional array
			ReflectionMap<Number> rm2 = create().append("B3.m2(String[][])", 2).build();
			assertTrue(rm2.find(B3.class.getMethod("m2", String[][].class)).findAny().isPresent());
	
			// Primitive array
			ReflectionMap<Number> rm3 = create().append("B3.m3(int[])", 3).build();
			assertTrue(rm3.find(B3.class.getMethod("m3", int[].class)).findAny().isPresent());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Field names
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	class C_Field {

		static class C1 {
			public int f1;
			public int f2;
		}
		static class C2 {
			public int f1;
		}

		static ReflectionMap<Number>
			C1f1_SIMPLE = create().append("C1.f1", 1).build(),
			C1f1_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$C_Field$C1.f1", 1).build();

		@Test void a01_checkEntries() {
			checkEntries(C1f1_SIMPLE, false, true, true, false);
			checkEntries(C1f1_FULL, false, true, true, false);
		}

		private static void test(Field f, boolean match_C1f1_SIMPLE, boolean match_C1f1_FULL) {
			assertEquals(match_C1f1_SIMPLE, C1f1_SIMPLE.find(f).findAny().isPresent());
			assertEquals(match_C1f1_FULL, C1f1_FULL.find(f).findAny().isPresent());

			assertEquals(match_C1f1_SIMPLE, C1f1_SIMPLE.find(f).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_C1f1_FULL, C1f1_FULL.find(f).filter(v -> v instanceof Integer).findAny().isPresent());

			assertFalse(C1f1_SIMPLE.find(f).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(C1f1_FULL.find(f).filter(v -> v instanceof Long).findAny().isPresent());
		}

		@Test void a02_find() throws Exception {
			test(C1.class.getField("f1"), true, true);
			test(C1.class.getField("f2"), false, false);
			test(C2.class.getField("f1"), false, false);
			test(null, false, false);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructor names
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	class D_Constructor {

		static class D1 {
			public D1() { /* no-op */ }
			public D1(int x) { /* no-op */ }
			public D1(String x) { /* no-op */ }
			public D1(String x, int y) { /* no-op */ }
		}
		static class D2 {
			public D2() { /* no-op */ }
		}

		static ReflectionMap<Number>
			D_SIMPLE = create().append("D1()", 1).build(),
			Di_SIMPLE = create().append("D1(int)", 1).build(),
			Ds_SIMPLE = create().append("D1(String)", 1).build(),
			Dss_SIMPLE = create().append("D1(java.lang.String)", 1).build(),
			Dsi_SIMPLE = create().append("D1(String, int)", 1).build(),
			Dssi_SIMPLE = create().append("D1(java.lang.String, int)", 1).build(),
			D_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$D_Constructor$D1()", 1).build(),
			Di_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$D_Constructor$D1(int)", 1).build(),
			Ds_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$D_Constructor$D1(String)", 1).build(),
			Dss_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$D_Constructor$D1(java.lang.String)", 1).build(),
			Dsi_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$D_Constructor$D1(String, int)", 1).build(),
			Dssi_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$D_Constructor$D1(java.lang.String, int)", 1).build();

		@Test void a01_checkEntries() {
			checkEntries(D_SIMPLE, false, false, false, true);
			checkEntries(Di_SIMPLE, false, false, false, true);
			checkEntries(Ds_SIMPLE, false, false, false, true);
			checkEntries(Dss_SIMPLE, false, false, false, true);
			checkEntries(Dsi_SIMPLE, false, false, false, true);
			checkEntries(Dssi_SIMPLE, false, false, false, true);
			checkEntries(D_FULL, false, false, false, true);
			checkEntries(Di_FULL, false, false, false, true);
			checkEntries(Ds_FULL, false, false, false, true);
			checkEntries(Dss_FULL, false, false, false, true);
			checkEntries(Dsi_FULL, false, false, false, true);
			checkEntries(Dssi_FULL, false, false, false, true);
		}

		private static void test(Constructor<?> c, boolean match_D_SIMPLE, boolean match_Di_SIMPLE, boolean match_Ds_SIMPLE, boolean match_Dss_SIMPLE,
				boolean match_Dsi_SIMPLE, boolean match_Dssi_SIMPLE, boolean match_D_FULL, boolean match_Di_FULL, boolean match_Ds_FULL,
				boolean match_Dss_FULL, boolean match_Dsi_FULL, boolean match_Dssi_FULL) {

			assertEquals(match_D_SIMPLE, D_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_Di_SIMPLE, Di_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_Ds_SIMPLE, Ds_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_Dss_SIMPLE, Dss_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_Dsi_SIMPLE, Dsi_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_Dssi_SIMPLE, Dssi_SIMPLE.find(c).findAny().isPresent());
			assertEquals(match_D_FULL, D_FULL.find(c).findAny().isPresent());
			assertEquals(match_Di_FULL, Di_FULL.find(c).findAny().isPresent());
			assertEquals(match_Ds_FULL, Ds_FULL.find(c).findAny().isPresent());
			assertEquals(match_Dss_FULL, Dss_FULL.find(c).findAny().isPresent());
			assertEquals(match_Dsi_FULL, Dsi_FULL.find(c).findAny().isPresent());
			assertEquals(match_Dssi_FULL, Dssi_FULL.find(c).findAny().isPresent());

			assertEquals(match_D_SIMPLE, D_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Di_SIMPLE, Di_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Ds_SIMPLE, Ds_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Dss_SIMPLE, Dss_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Dsi_SIMPLE, Dsi_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Dssi_SIMPLE, Dssi_SIMPLE.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_D_FULL, D_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Di_FULL, Di_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Ds_FULL, Ds_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Dss_FULL, Dss_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Dsi_FULL, Dsi_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());
			assertEquals(match_Dssi_FULL, Dssi_FULL.find(c).filter(v -> v instanceof Integer).findAny().isPresent());

			assertFalse(D_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Di_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Ds_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Dss_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Dsi_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Dssi_SIMPLE.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(D_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Di_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Ds_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Dss_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Dsi_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
			assertFalse(Dssi_FULL.find(c).filter(v -> v instanceof Long).findAny().isPresent());
		}

		@Test void a02_find() throws Exception {
			test(D1.class.getConstructor(), true, false, false, false, false, false, true, false, false, false, false, false);
			test(D1.class.getConstructor(int.class), false, true, false, false, false, false, false, true, false, false, false, false);
			test(D1.class.getConstructor(String.class), false, false, true, true, false, false, false, false, true, true, false, false);
			test(D1.class.getConstructor(String.class, int.class), false, false, false, false, true, true, false, false, false, false, true, true);
			test(D2.class.getConstructor(), false, false, false, false, false, false, false, false, false, false, false, false);
			test(null, false, false, false, false, false, false, false, false, false, false, false, false);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Invalid input
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	class E_InvalidInput {

		@Test void a01_blankInput() {
			assertThrowsWithMessage(RuntimeException.class, "Invalid reflection signature: []", ()->create().append("", 1));
		}

		@Test void a02_nullInput() {
			assertThrowsWithMessage(RuntimeException.class, "Invalid reflection signature: [null]", ()->create().append(null, 1));
		}

		@Test void a03_badInput() {
			assertThrowsWithMessage(RuntimeException.class, "Invalid reflection signature: [foo)]", ()->create().append("foo)", 1));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other tests
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	class F_Other {

		static class F1 {}

		static ReflectionMap<Number> RM_F = create().append("F2, F1", 1).build();

		@Test void a01_cdl() {
			assertString("1", RM_F.find(F1.class).findFirst().get());
		}

		static ReflectionMap<Number> RM_G = create().build();

		@Test void a02_emptyReflectionMap() throws Exception {
			assertFalse(RM_G.find(A_Class.A1.class).findAny().isPresent());
			assertFalse(RM_G.find(B_Method.B1.class.getMethod("m1")).findAny().isPresent());
			assertFalse(RM_G.find(C_Field.C1.class.getField("f1")).findAny().isPresent());
			assertFalse(RM_G.find(D_Constructor.D1.class.getConstructor()).findAny().isPresent());
		}

		@Test void a03_toString() {
			ReflectionMap<Number> rm = create()
				.append("F1", 1)                    // class
				.append("F1.toString", 2)           // method (and field)
				.append("F1()", 3)                  // constructor
				.build();
			assertString("{classEntries=[{simpleName=F1, fullName=F1, value=1}], methodEntries=[{simpleClassName=F1, fullClassName=F1, methodName=toString, value=2}], fieldEntries=[{simpleClassName=F1, fullClassName=F1, fieldName=toString, value=2}], constructorEntries=[{simpleClassName=F1, fullClassName=F1, value=3}]}", rm);
		}
	}
}