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
import java.util.*;

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

		// @formatter:off
		static ReflectionMap<Number>
			A1_SIMPLE = create().append("A1", 1).build(),
			A1b_SIMPLE = create().append("ReflectionMap_Test$A_Class$A1", 1).build(),
			A1_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$A_Class$A1", 1).build();  // Note this could be a static field.
		// @formatter:on

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

		// Test for wildcard matching (line 584)
		@Test void a03_wildcardMatching() {
			var rm = create().append("*", 1).build();
			assertTrue(rm.find(A1.class).findAny().isPresent());
			assertTrue(rm.find(A2.class).findAny().isPresent());
			assertTrue(rm.find(String.class).findAny().isPresent());
		}

		// Test for inner class matching (lines 586, 588, 595-596)
		static class OuterClass {
			static class InnerClass1 {
				static class InnerClass2 {}
			}
		}

		@Test void a04_innerClassMatching() throws Exception {
			var inner1 = OuterClass.InnerClass1.class;
			var inner2 = OuterClass.InnerClass1.InnerClass2.class;

			// Match inner class by partial name (should match after stripping package)
			var rm1 = create().append("OuterClass$InnerClass1", 1).build();
			assertTrue(rm1.find(inner1).findAny().isPresent());

			// Match inner class by simple name only (should match in while loop - lines 595-596)
			var rm2 = create().append("InnerClass1", 2).build();
			assertTrue(rm2.find(inner1).findAny().isPresent());

			// Match nested inner class by simple name (should match in while loop)
			var rm3 = create().append("InnerClass2", 3).build();
			assertTrue(rm3.find(inner2).findAny().isPresent());

			// Match nested inner class by partial path
			var rm4 = create().append("InnerClass1$InnerClass2", 4).build();
			assertTrue(rm4.find(inner2).findAny().isPresent());
		}

		// Test for class without package (line 586 false branch, line 588 false branch)
		@Test void a05_classWithoutPackage() throws Exception {
			// Primitive array class has no package and no '$' in name
			var intArrayClass = int[].class;
			var rm1 = create().append("int[]", 1).build();
			assertTrue(rm1.find(intArrayClass).findAny().isPresent());

			// Test with a regular non-inner class (no '$') to hit line 586 false branch
			var rm2 = create().append("String", 2).build();
			assertTrue(rm2.find(String.class).findAny().isPresent());
		}

		// Test for inner class with and without package (line 588 branches)
		@Test void a06_innerClassPackageHandling() throws Exception {
			var inner1 = OuterClass.InnerClass1.class;

			// Pattern with full path including package - tests line 588 true branch (package not null)
			var rm1 = create().append("ReflectionMap_Test$A_Class$OuterClass$InnerClass1", 1).build();
			assertTrue(rm1.find(inner1).findAny().isPresent());

			// Pattern with partial path - tests while loop and line 588 execution
			var rm2 = create().append("A_Class$OuterClass$InnerClass1", 2).build();
			assertTrue(rm2.find(inner1).findAny().isPresent());

			// Test line 588 false branch (package IS null but class has $ - inner class in default package)
			// Must use reflection to access classes from default package
			var defaultPackageInner = Class.forName("DefaultPackageTestClass$InnerClass");
			var defaultPackageNested = Class.forName("DefaultPackageTestClass$InnerClass$NestedInner");

			// This class has '$' so line 586 is true, but getPackage() returns null, so line 588 is false
			var rm3 = create().append("DefaultPackageTestClass$InnerClass", 3).build();
			assertTrue(rm3.find(defaultPackageInner).findAny().isPresent());

			// Test matching by simple inner class name (exercises while loop with null package)
			var rm4 = create().append("InnerClass", 4).build();
			assertTrue(rm4.find(defaultPackageInner).findAny().isPresent());

			// Test nested inner class in default package
			var rm5 = create().append("NestedInner", 5).build();
			assertTrue(rm5.find(defaultPackageNested).findAny().isPresent());
		}

		// Additional test to ensure both branches of line 586 are covered
		@Test void a07_nonInnerClassMatching() throws Exception {
			// Test classes without $ (line 586 false branch)
			var rm1 = create().append("String", 1).build();
			assertTrue(rm1.find(String.class).findAny().isPresent()); // No $ in name

			var rm2 = create().append("Integer", 2).build();
			assertTrue(rm2.find(Integer.class).findAny().isPresent()); // No $ in name
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

		// @formatter:off
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
		// @formatter:on

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

		// Test for generic parameters in method signatures (lines 407-411)
		@SuppressWarnings("rawtypes")
		static class B2a {
			public void m1(List x) { /* no-op */ }
			public void m2(List<String> x) { /* no-op */ }
			public void m3(Map<String, Integer> x) { /* no-op */ }
			public void m4(List<List<String>> x) { /* no-op */ }
			public void m5(Map x, List y) { /* no-op */ }
		}

		@Test void a03_generics() throws Exception {
			// Generic parameters should be stripped, so List<String> matches List
			var rm1 = create().append("B2a.m1(List<String>)", 1).build();
			assertTrue(rm1.find(B2a.class.getMethod("m1", List.class)).findAny().isPresent());

			// Also works the other way - List pattern matches method declared with List<String>
			var rm2 = create().append("B2a.m2(List)", 2).build();
			assertTrue(rm2.find(B2a.class.getMethod("m2", List.class)).findAny().isPresent());

			// Multiple generic parameters
			var rm3 = create().append("B2a.m3(Map<String,Integer>)", 3).build();
			assertTrue(rm3.find(B2a.class.getMethod("m3", Map.class)).findAny().isPresent());

			// Nested generic parameters
			var rm4 = create().append("B2a.m4(List<List<String>>)", 4).build();
			assertTrue(rm4.find(B2a.class.getMethod("m4", List.class)).findAny().isPresent());

			// Multiple parameters with generics
			var rm5 = create().append("B2a.m5(Map<String,Integer>, List<String>)", 5).build();
			assertTrue(rm5.find(B2a.class.getMethod("m5", Map.class, List.class)).findAny().isPresent());

			// With fully qualified names
			var rm6 = create().append("B2a.m1(java.util.List<java.lang.String>)", 6).build();
			assertTrue(rm6.find(B2a.class.getMethod("m1", List.class)).findAny().isPresent());
		}

		// Test for array parameters in method signatures
		static class B3 {
			public void m1(String[] x) { /* no-op */ }
			public void m2(String[][] x) { /* no-op */ }
			public void m3(int[] x) { /* no-op */ }
		}

		@Test void a04_arrayParams() throws Exception {
			// Single-dimensional array with simple name
			var rm1 = create().append("B3.m1(String[])", 1).build();
			assertTrue(rm1.find(B3.class.getMethod("m1", String[].class)).findAny().isPresent());

			// Single-dimensional array with full name
			var rm1b = create().append("B3.m1(java.lang.String[])", 1).build();
			assertTrue(rm1b.find(B3.class.getMethod("m1", String[].class)).findAny().isPresent());

			// Multi-dimensional array
			var rm2 = create().append("B3.m2(String[][])", 2).build();
			assertTrue(rm2.find(B3.class.getMethod("m2", String[][].class)).findAny().isPresent());

			// Primitive array
			var rm3 = create().append("B3.m3(int[])", 3).build();
			assertTrue(rm3.find(B3.class.getMethod("m3", int[].class)).findAny().isPresent());
		}

		// Test for generic arrays - combining generics and arrays
		static class B4 {
			public void m1(List<String>[] x) { /* no-op */ }
			public void m2(Map<String, Integer>[][] x) { /* no-op */ }
		}

		@Test void a05_genericArrays() throws Exception {
			// Array of generic types - generics should be stripped, array dimensions preserved
			var rm1 = create().append("B4.m1(List<String>[])", 1).build();
			assertTrue(rm1.find(B4.class.getMethod("m1", List[].class)).findAny().isPresent());

			// Also works without generics in pattern
			var rm1b = create().append("B4.m1(List[])", 1).build();
			assertTrue(rm1b.find(B4.class.getMethod("m1", List[].class)).findAny().isPresent());

			// Multi-dimensional array of generic types
			var rm2 = create().append("B4.m2(Map<String,Integer>[][])", 2).build();
			assertTrue(rm2.find(B4.class.getMethod("m2", Map[][].class)).findAny().isPresent());

			// With fully qualified names
			var rm3 = create().append("B4.m1(java.util.List<java.lang.String>[])", 3).build();
			assertTrue(rm3.find(B4.class.getMethod("m1", List[].class)).findAny().isPresent());
		}

		// Test for negative matching - array dimension mismatches and type mismatches
		static class B5 {
			public void testMethod(String[] x) { /* no-op */ }
			public void testMethod(String[][] x) { /* no-op */ }
			public void testMethod(Integer[] x) { /* no-op */ }
			public void testMethod(int[] x) { /* no-op */ }
		}

		@Test void a06_arrayNegativeMatching() throws Exception {
			// Array dimension mismatch - pattern has 1 dimension, actual has 2
			var rm1 = create().append("B5.testMethod(String[])", 1).build();
			assertFalse(rm1.find(B5.class.getMethod("testMethod", String[][].class)).findAny().isPresent());

			// Array dimension mismatch - pattern has 2 dimensions, actual has 1
			var rm2 = create().append("B5.testMethod(String[][])", 2).build();
			assertFalse(rm2.find(B5.class.getMethod("testMethod", String[].class)).findAny().isPresent());

			// Array type mismatch - pattern is String[], actual is Integer[]
			var rm3 = create().append("B5.testMethod(String[])", 1).build();
			assertFalse(rm3.find(B5.class.getMethod("testMethod", Integer[].class)).findAny().isPresent());

			// Array type mismatch - pattern is Integer[], actual is String[]
			var rm4 = create().append("B5.testMethod(Integer[])", 3).build();
			assertFalse(rm4.find(B5.class.getMethod("testMethod", String[].class)).findAny().isPresent());

			// Array type mismatch - pattern is int[], actual is String[]
			var rm5 = create().append("B5.testMethod(int[])", 4).build();
			assertFalse(rm5.find(B5.class.getMethod("testMethod", String[].class)).findAny().isPresent());
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

		// @formatter:off
		static ReflectionMap<Number>
			C1f1_SIMPLE = create().append("C1.f1", 1).build(),
			C1f1_FULL = create().append("org.apache.juneau.common.reflect.ReflectionMap_Test$C_Field$C1.f1", 1).build();
		// @formatter:on

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

		// @formatter:off
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
		// @formatter:on

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

		// Test for generic parameters with commas in constructor signatures
		static class D3 {
			public D3(Map<String, Integer> x) { /* no-op */ }
			public D3(Map<String, Integer> x, List<String> y) { /* no-op */ }
			public D3(Map<String, Integer>[] x) { /* no-op */ }
		}

		@Test void a03_generics() throws Exception {
			// Generic parameters with commas should be handled correctly
			var rm1 = create().append("D3(Map<String,Integer>)", 1).build();
			assertTrue(rm1.find(D3.class.getConstructor(Map.class)).findAny().isPresent());

			// Also works without generics in pattern
			var rm1b = create().append("D3(Map)", 1).build();
			assertTrue(rm1b.find(D3.class.getConstructor(Map.class)).findAny().isPresent());

			// Multiple parameters with generics containing commas
			var rm2 = create().append("D3(Map<String,Integer>, List<String>)", 2).build();
			assertTrue(rm2.find(D3.class.getConstructor(Map.class, List.class)).findAny().isPresent());

			// Generic array parameters
			var rm3 = create().append("D3(Map<String,Integer>[])", 3).build();
			assertTrue(rm3.find(D3.class.getConstructor(Map[].class)).findAny().isPresent());

			// With fully qualified names
			var rm4 = create().append("D3(java.util.Map<java.lang.String,java.lang.Integer>)", 4).build();
			assertTrue(rm4.find(D3.class.getConstructor(Map.class)).findAny().isPresent());
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
			var rm = create()
				.append("F1", 1)                    // class
				.append("F1.toString", 2)           // method without args (args=null)
				.append("F1.toString(String)", 3)   // method with args
				.append("F1.myField", 4)            // field
				.append("F1()", 5)                  // constructor
				.build();
			assertString("{classEntries=[{simpleName=F1, fullName=F1, value=1}], methodEntries=[{simpleClassName=F1, fullClassName=F1, methodName=toString, value=2}, {simpleClassName=F1, fullClassName=F1, methodName=toString, args=[String], value=3}, {simpleClassName=F1, fullClassName=F1, methodName=myField, value=4}], fieldEntries=[{simpleClassName=F1, fullClassName=F1, fieldName=toString, value=2}, {simpleClassName=F1, fullClassName=F1, fieldName=myField, value=4}], constructorEntries=[{simpleClassName=F1, fullClassName=F1, value=5}]}", rm);
		}
	}
}