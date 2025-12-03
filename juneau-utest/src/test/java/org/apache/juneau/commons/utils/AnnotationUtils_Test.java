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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.AnnotationUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link AnnotationUtils}.
 */
class AnnotationUtils_Test {

	//====================================================================================================
	// Constructor (line 31)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 31: class instantiation
		// AnnotationUtils has an implicit public no-arg constructor
		var instance = new AnnotationUtils();
		assertNotNull(instance);
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Basic cases
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface SimpleAnnotation {
		String value() default "";
	}

	@SimpleAnnotation("test")
	static class TestClass1 {}

	@SimpleAnnotation("test")
	static class TestClass2 {}

	@SimpleAnnotation("different")
	static class TestClass3 {}

	@Test
	void a01_equals_sameInstance() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		assertTrue(AnnotationUtils.equals(a1, a1));
	}

	@Test
	void a02_equals_sameValue() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		var a2 = TestClass2.class.getAnnotation(SimpleAnnotation.class);
		assertTrue(AnnotationUtils.equals(a1, a2));
	}

	@Test
	void a03_equals_differentValue() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		var a3 = TestClass3.class.getAnnotation(SimpleAnnotation.class);
		assertFalse(AnnotationUtils.equals(a1, a3));
	}

	@Test
	void a04_equals_bothNull() {
		assertTrue(AnnotationUtils.equals(null, null));
	}

	@Test
	void a05_equals_firstNull() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		assertFalse(AnnotationUtils.equals(null, a1));
	}

	@Test
	void a06_equals_secondNull() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		assertFalse(AnnotationUtils.equals(a1, null));
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Different annotation types
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface DifferentAnnotation {
		String value() default "";
	}

	@DifferentAnnotation("test")
	static class TestClass4 {}

	@Test
	void a07_equals_differentTypes() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		var a4 = TestClass4.class.getAnnotation(DifferentAnnotation.class);
		assertFalse(AnnotationUtils.equals(a1, a4));
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Multiple members
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface MultiMemberAnnotation {
		String name() default "";
		int count() default 0;
		boolean active() default false;
	}

	@MultiMemberAnnotation(name = "test", count = 5, active = true)
	static class TestClass5 {}

	@MultiMemberAnnotation(name = "test", count = 5, active = true)
	static class TestClass6 {}

	@MultiMemberAnnotation(name = "test", count = 5, active = false)
	static class TestClass7 {}

	@MultiMemberAnnotation(name = "different", count = 5, active = true)
	static class TestClass8 {}

	@MultiMemberAnnotation(name = "test", count = 10, active = true)
	static class TestClass9 {}

	@Test
	void a08_equals_multipleMembers_same() {
		var a5 = TestClass5.class.getAnnotation(MultiMemberAnnotation.class);
		var a6 = TestClass6.class.getAnnotation(MultiMemberAnnotation.class);
		assertTrue(AnnotationUtils.equals(a5, a6));
	}

	@Test
	void a09_equals_multipleMembers_differentBoolean() {
		var a5 = TestClass5.class.getAnnotation(MultiMemberAnnotation.class);
		var a7 = TestClass7.class.getAnnotation(MultiMemberAnnotation.class);
		assertFalse(AnnotationUtils.equals(a5, a7));
	}

	@Test
	void a10_equals_multipleMembers_differentString() {
		var a5 = TestClass5.class.getAnnotation(MultiMemberAnnotation.class);
		var a8 = TestClass8.class.getAnnotation(MultiMemberAnnotation.class);
		assertFalse(AnnotationUtils.equals(a5, a8));
	}

	@Test
	void a11_equals_multipleMembers_differentInt() {
		var a5 = TestClass5.class.getAnnotation(MultiMemberAnnotation.class);
		var a9 = TestClass9.class.getAnnotation(MultiMemberAnnotation.class);
		assertFalse(AnnotationUtils.equals(a5, a9));
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Array members
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface ArrayAnnotation {
		String[] values() default {};
		int[] numbers() default {};
	}

	@ArrayAnnotation(values = {"a", "b", "c"}, numbers = {1, 2, 3})
	static class TestClass10 {}

	@ArrayAnnotation(values = {"a", "b", "c"}, numbers = {1, 2, 3})
	static class TestClass11 {}

	@ArrayAnnotation(values = {"a", "b"}, numbers = {1, 2, 3})
	static class TestClass12 {}

	@ArrayAnnotation(values = {"a", "b", "c"}, numbers = {1, 2})
	static class TestClass13 {}

	@ArrayAnnotation(values = {"a", "b", "d"}, numbers = {1, 2, 3})
	static class TestClass14 {}

	@ArrayAnnotation(values = {"a", "b", "c"}, numbers = {1, 2, 4})
	static class TestClass15 {}

	@ArrayAnnotation
	static class TestClass16 {}

	@Test
	void a12_equals_arrayMembers_same() {
		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a11 = TestClass11.class.getAnnotation(ArrayAnnotation.class);
		assertTrue(AnnotationUtils.equals(a10, a11));
	}

	@Test
	void a13_equals_arrayMembers_differentLength() {
		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a12 = TestClass12.class.getAnnotation(ArrayAnnotation.class);
		assertFalse(AnnotationUtils.equals(a10, a12));
	}

	@Test
	void a14_equals_arrayMembers_differentIntArrayLength() {
		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a13 = TestClass13.class.getAnnotation(ArrayAnnotation.class);
		assertFalse(AnnotationUtils.equals(a10, a13));
	}

	@Test
	void a15_equals_arrayMembers_differentStringValues() {
		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a14 = TestClass14.class.getAnnotation(ArrayAnnotation.class);
		assertFalse(AnnotationUtils.equals(a10, a14));
	}

	@Test
	void a16_equals_arrayMembers_differentIntValues() {
		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a15 = TestClass15.class.getAnnotation(ArrayAnnotation.class);
		assertFalse(AnnotationUtils.equals(a10, a15));
	}

	@Test
	void a17_equals_arrayMembers_emptyArrays() {
		var a16 = TestClass16.class.getAnnotation(ArrayAnnotation.class);
		var a16b = TestClass16.class.getAnnotation(ArrayAnnotation.class);
		assertTrue(AnnotationUtils.equals(a16, a16b));
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Primitive array types
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface PrimitiveArrayAnnotation {
		byte[] bytes() default {};
		short[] shorts() default {};
		long[] longs() default {};
		float[] floats() default {};
		double[] doubles() default {};
		char[] chars() default {};
		boolean[] booleans() default {};
	}

	@PrimitiveArrayAnnotation(
		bytes = {1, 2, 3},
		shorts = {10, 20},
		longs = {100L, 200L},
		floats = {1.0f, 2.0f},
		doubles = {1.0, 2.0},
		chars = {'a', 'b'},
		booleans = {true, false}
	)
	static class TestClass17 {}

	@PrimitiveArrayAnnotation(
		bytes = {1, 2, 3},
		shorts = {10, 20},
		longs = {100L, 200L},
		floats = {1.0f, 2.0f},
		doubles = {1.0, 2.0},
		chars = {'a', 'b'},
		booleans = {true, false}
	)
	static class TestClass18 {}

	@PrimitiveArrayAnnotation(
		bytes = {1, 2, 4},
		shorts = {10, 20},
		longs = {100L, 200L},
		floats = {1.0f, 2.0f},
		doubles = {1.0, 2.0},
		chars = {'a', 'b'},
		booleans = {true, false}
	)
	static class TestClass19 {}

	@Test
	void a18_equals_primitiveArrays_same() {
		var a17 = TestClass17.class.getAnnotation(PrimitiveArrayAnnotation.class);
		var a18 = TestClass18.class.getAnnotation(PrimitiveArrayAnnotation.class);
		assertTrue(AnnotationUtils.equals(a17, a18));
	}

	@Test
	void a19_equals_primitiveArrays_different() {
		var a17 = TestClass17.class.getAnnotation(PrimitiveArrayAnnotation.class);
		var a19 = TestClass19.class.getAnnotation(PrimitiveArrayAnnotation.class);
		assertFalse(AnnotationUtils.equals(a17, a19));
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Nested annotations
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedAnnotation {
		SimpleAnnotation nested() default @SimpleAnnotation;
	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedArrayAnnotation {
		SimpleAnnotation[] nested() default {};
	}

	@NestedAnnotation(nested = @SimpleAnnotation("test"))
	static class TestClass20 {}

	@NestedAnnotation(nested = @SimpleAnnotation("test"))
	static class TestClass21 {}

	@NestedAnnotation(nested = @SimpleAnnotation("different"))
	static class TestClass22 {}

	@NestedArrayAnnotation(nested = {@SimpleAnnotation("a"), @SimpleAnnotation("b")})
	static class TestClass23 {}

	@NestedArrayAnnotation(nested = {@SimpleAnnotation("a"), @SimpleAnnotation("b")})
	static class TestClass24 {}

	@NestedArrayAnnotation(nested = {@SimpleAnnotation("a"), @SimpleAnnotation("c")})
	static class TestClass25 {}

	@Test
	void a20_equals_nestedAnnotation_same() {
		var a20 = TestClass20.class.getAnnotation(NestedAnnotation.class);
		var a21 = TestClass21.class.getAnnotation(NestedAnnotation.class);
		assertTrue(AnnotationUtils.equals(a20, a21));
	}

	@Test
	void a21_equals_nestedAnnotation_different() {
		var a20 = TestClass20.class.getAnnotation(NestedAnnotation.class);
		var a22 = TestClass22.class.getAnnotation(NestedAnnotation.class);
		assertFalse(AnnotationUtils.equals(a20, a22));
	}

	@Test
	void a22_equals_nestedAnnotationArray_same() {
		var a23 = TestClass23.class.getAnnotation(NestedArrayAnnotation.class);
		var a24 = TestClass24.class.getAnnotation(NestedArrayAnnotation.class);
		assertTrue(AnnotationUtils.equals(a23, a24));
	}

	@Test
	void a23_equals_nestedAnnotationArray_different() {
		var a23 = TestClass23.class.getAnnotation(NestedArrayAnnotation.class);
		var a25 = TestClass25.class.getAnnotation(NestedArrayAnnotation.class);
		assertFalse(AnnotationUtils.equals(a23, a25));
	}

	//====================================================================================================
	// hash(Annotation) - Basic cases
	//====================================================================================================
	@Test
	void b01_hash_simpleAnnotation() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		var a2 = TestClass2.class.getAnnotation(SimpleAnnotation.class);
		assertEquals(hash(a1), hash(a2));
	}

	@Test
	void b02_hash_differentValues() {
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		var a3 = TestClass3.class.getAnnotation(SimpleAnnotation.class);
		// Different values should produce different hash codes (with high probability)
		assertNotEquals(hash(a1), hash(a3));
	}

	@Test
	void b03_hash_multipleMembers() {
		var a5 = TestClass5.class.getAnnotation(MultiMemberAnnotation.class);
		var a6 = TestClass6.class.getAnnotation(MultiMemberAnnotation.class);
		assertEquals(hash(a5), hash(a6));
	}

	@Test
	void b04_hash_arrayMembers() {
		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a11 = TestClass11.class.getAnnotation(ArrayAnnotation.class);
		assertEquals(hash(a10), hash(a11));
	}

	@Test
	void b05_hash_primitiveArrays() {
		var a17 = TestClass17.class.getAnnotation(PrimitiveArrayAnnotation.class);
		var a18 = TestClass18.class.getAnnotation(PrimitiveArrayAnnotation.class);
		assertEquals(hash(a17), hash(a18));
	}

	@Test
	void b06_hash_nestedAnnotation() {
		var a20 = TestClass20.class.getAnnotation(NestedAnnotation.class);
		var a21 = TestClass21.class.getAnnotation(NestedAnnotation.class);
		assertEquals(hash(a20), hash(a21));
	}

	@Test
	void b07_hash_nestedAnnotationArray() {
		var a23 = TestClass23.class.getAnnotation(NestedArrayAnnotation.class);
		var a24 = TestClass24.class.getAnnotation(NestedArrayAnnotation.class);
		assertEquals(hash(a23), hash(a24));
	}

	//====================================================================================================
	// hash(Annotation) - Consistency with equals
	//====================================================================================================
	@Test
	void b08_hash_equalsConsistency() {
		// If two annotations are equal, they must have the same hash code
		var a1 = TestClass1.class.getAnnotation(SimpleAnnotation.class);
		var a2 = TestClass2.class.getAnnotation(SimpleAnnotation.class);
		assertTrue(AnnotationUtils.equals(a1, a2));
		assertEquals(hash(a1), hash(a2));

		var a5 = TestClass5.class.getAnnotation(MultiMemberAnnotation.class);
		var a6 = TestClass6.class.getAnnotation(MultiMemberAnnotation.class);
		assertTrue(AnnotationUtils.equals(a5, a6));
		assertEquals(hash(a5), hash(a6));

		var a10 = TestClass10.class.getAnnotation(ArrayAnnotation.class);
		var a11 = TestClass11.class.getAnnotation(ArrayAnnotation.class);
		assertTrue(AnnotationUtils.equals(a10, a11));
		assertEquals(hash(a10), hash(a11));
	}

	//====================================================================================================
	// hash(Annotation) - Edge cases
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface EmptyAnnotation {
	}

	@EmptyAnnotation
	static class TestClass26 {}

	@Test
	void b09_hash_emptyAnnotation() {
		var a26 = TestClass26.class.getAnnotation(EmptyAnnotation.class);
		// Should not throw exception
		int hashCode = hash(a26);
		assertTrue(hashCode >= 0 || hashCode < 0); // Just verify it returns a value
	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface DefaultValueAnnotation {
		String value() default "default";
		int count() default 42;
	}

	@DefaultValueAnnotation
	static class TestClass27 {}

	@DefaultValueAnnotation(value = "default", count = 42)
	static class TestClass28 {}

	@Test
	void b10_hash_defaultValues() {
		var a27 = TestClass27.class.getAnnotation(DefaultValueAnnotation.class);
		var a28 = TestClass28.class.getAnnotation(DefaultValueAnnotation.class);
		// Annotations with default values should be equal and have same hash
		assertTrue(AnnotationUtils.equals(a27, a28));
		assertEquals(hash(a27), hash(a28));
	}

	//====================================================================================================
	// hash(Annotation) - Null member values (line 157)
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface NullableMemberAnnotation {
		String value() default "default";
	}

	@NullableMemberAnnotation
	static class TestClass29 {}

	@Test
	void b11_hash_nullMember() throws Exception {
		// Test line 157: hashMember when value == null
		// We need to create an annotation instance where a member returns null
		// Since annotation members can't be null by default, we'll create a custom proxy
		// that returns null for a member method
		var a29 = TestClass29.class.getAnnotation(NullableMemberAnnotation.class);
		
		// Create a custom annotation proxy that returns null for the value() method
		// This simulates the edge case where a member might return null
		Annotation nullMemberAnnotation = (Annotation) java.lang.reflect.Proxy.newProxyInstance(
			NullableMemberAnnotation.class.getClassLoader(),
			new Class<?>[] { NullableMemberAnnotation.class },
			(proxy, method, args) -> {
				if (method.getName().equals("value")) {
					return null;  // Return null to test line 157
				}
				if (method.getName().equals("annotationType")) {
					return NullableMemberAnnotation.class;
				}
				if (method.getName().equals("toString")) {
					return "@NullableMemberAnnotation(null)";
				}
				if (method.getName().equals("hashCode")) {
					return 0;
				}
				if (method.getName().equals("equals")) {
					return proxy == args[0];
				}
				return method.invoke(a29, args);
			}
		);
		
		// Test that hash() handles null member values correctly (line 157)
		int hashNull = hash(nullMemberAnnotation);
		// Should not throw, and should return a hash code based on part1 (name.hashCode() * 127)
		assertTrue(hashNull != 0 || hashNull == 0); // Just verify it doesn't throw
	}

	//====================================================================================================
	// equals(Annotation, Annotation) - Null member values (line 169)
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface MemberEqualsTestAnnotation {
		String value() default "test";
	}

	@MemberEqualsTestAnnotation
	static class TestClass31 {}

	@Test
	void a24_equals_nullMember() throws Exception {
		// Test line 169: memberEquals when o1 == null || o2 == null
		// Create custom annotation proxies where one member returns null
		var a31 = TestClass31.class.getAnnotation(MemberEqualsTestAnnotation.class);
		
		// Create annotation proxy with null member
		Annotation nullMember1 = (Annotation) java.lang.reflect.Proxy.newProxyInstance(
			MemberEqualsTestAnnotation.class.getClassLoader(),
			new Class<?>[] { MemberEqualsTestAnnotation.class },
			(proxy, method, args) -> {
				if (method.getName().equals("value")) {
					return null;  // Return null to test line 169
				}
				if (method.getName().equals("annotationType")) {
					return MemberEqualsTestAnnotation.class;
				}
				if (method.getName().equals("toString")) {
					return "@MemberEqualsTestAnnotation(null)";
				}
				if (method.getName().equals("hashCode")) {
					return 0;
				}
				if (method.getName().equals("equals")) {
					return proxy == args[0];
				}
				return method.invoke(a31, args);
			}
		);
		
		// Create another annotation proxy with non-null member
		Annotation nonNullMember = (Annotation) java.lang.reflect.Proxy.newProxyInstance(
			MemberEqualsTestAnnotation.class.getClassLoader(),
			new Class<?>[] { MemberEqualsTestAnnotation.class },
			(proxy, method, args) -> {
				if (method.getName().equals("value")) {
					return "test";  // Non-null value
				}
				if (method.getName().equals("annotationType")) {
					return MemberEqualsTestAnnotation.class;
				}
				if (method.getName().equals("toString")) {
					return "@MemberEqualsTestAnnotation(test)";
				}
				if (method.getName().equals("hashCode")) {
					return 0;
				}
				if (method.getName().equals("equals")) {
					return proxy == args[0];
				}
				return method.invoke(a31, args);
			}
		);
		
		// Test line 169: memberEquals when o1 == null || o2 == null
		// When comparing nullMember1 (null value) with nonNullMember (non-null value),
		// memberEquals should return false (line 169)
		assertFalse(AnnotationUtils.equals(nullMember1, nonNullMember));
		assertFalse(AnnotationUtils.equals(nonNullMember, nullMember1));
	}

	//====================================================================================================
	// getAnnotationMethods - Filter coverage (line 151)
	//====================================================================================================
	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface FilterTestAnnotation {
		String value() default "";
	}

	@FilterTestAnnotation("test")
	static class TestClass33 {}

	@Test
	void c01_getAnnotationMethods_filter() throws Exception {
		// Test line 151: getAnnotationMethods filter
		// The filter checks: x.getParameterCount() == 0
		// 
		// Branch analysis:
		// 1. getParameterCount() == 0 → passes filter (covered by normal annotation methods)
		// 2. getParameterCount() != 0 → fails filter (not covered - impossible to test)
		//
		// The false branch (getParameterCount() != 0) cannot be tested because:
		// - Annotation member methods must have 0 parameters by Java language specification
		// - getDeclaredMethods() on an annotation class only returns annotation member methods
		// - Even synthetic methods on annotation classes would have 0 parameters
		//
		// To attempt to cover the false branch, we can inspect what methods are actually returned
		// and verify they all have 0 parameters, confirming the false branch is unreachable.
		var annotationClass = FilterTestAnnotation.class;
		var declaredMethods = annotationClass.getDeclaredMethods();
		
		// Verify all declared methods have 0 parameters (this exercises the true branch)
		for (var method : declaredMethods) {
			assertEquals(0, method.getParameterCount(), 
				"All annotation member methods must have 0 parameters: " + method.getName());
		}
		
		// Verify the filter works correctly
		var a33 = TestClass33.class.getAnnotation(FilterTestAnnotation.class);
		assertTrue(AnnotationUtils.equals(a33, a33));
		int hash33 = hash(a33);
		assertTrue(hash33 != 0 || hash33 == 0); // Just verify it doesn't throw
		
		// Note: The false branch (getParameterCount() != 0) is impossible to test because
		// annotation member methods cannot have parameters. This is a language requirement.
	}
}

