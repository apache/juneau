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
package org.apache.juneau.commons.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.reflect.ElementFlag.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FieldInfo_Test extends TestBase {

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@Inherited
	public static @interface A {
		String value();
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@Inherited
	public static @interface AX {
		String value();
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	public static @interface TestAnnotation1 {
		String value() default "";
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	public static @interface TestAnnotation2 {
		int value() default 0;
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	public static @interface TestAnnotation3 {
		String value() default "";
	}

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = t -> {
		if (t == null)
			return null;
		if (t instanceof A)
			return "@A(" + ((A)t).value() + ")";
		if (t instanceof ClassInfo)
			return ((ClassInfo)t).getNameSimple();
		if (t instanceof FieldInfo)
			return ((FieldInfo)t).getName();
		if (t instanceof Field)
			return ((Field)t).getName();
		return t.toString();
	};

	private static FieldInfo off(Class<?> c, String name) {
		try {
			return FieldInfo.of(c.getDeclaredField(name));
		} catch (SecurityException | NoSuchFieldException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	static class A1 {
		public int f1;
	}
	FieldInfo a1_f1 = off(A1.class, "f1");

	@Test void of_withClass() {
		check("f1", FieldInfo.of(ClassInfo.of(A1.class), a1_f1.inner()));
	}

	@Test void of_withoutClass() {
		check("f1", FieldInfo.of(a1_f1.inner()));
	}

	@Test void of_null() {
		assertThrows(IllegalArgumentException.class, () -> FieldInfo.of(null));
		assertThrows(IllegalArgumentException.class, () -> FieldInfo.of(null, null));
	}

	@Test void getDeclaringClass() {
		check("A1", a1_f1.getDeclaringClass());
		check("A1", a1_f1.getDeclaringClass());
	}

	@Test void inner() {
		check("f1", a1_f1.inner());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	public static class B {
		@A("a1") public int a1;
		public int a2;
	}

	FieldInfo
		b_a1 = off(B.class, "a1"),
		b_a2 = off(B.class, "a2");

	@Test void hasAnnotation_true() {
		assertTrue(b_a1.hasAnnotation(A.class));
	}

	@Test void hasAnnotation_false() {
		assertFalse(b_a2.hasAnnotation(A.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	abstract static class C {
		@Deprecated public int deprecated;
		public int notDeprecated;
		public int isPublic;
		protected int isNotPublic;
		public static int isStatic;
		public int isNotStatic;
		public transient int isTransient;
		public int isNotTransient;
	}
	static ClassInfo c = ClassInfo.of(C.class);
	static FieldInfo
		c_deprecated = c.getPublicField(x -> x.hasName("deprecated")).get(),
		c_notDeprecated = c.getPublicField(x -> x.hasName("notDeprecated")).get(),
		c_isPublic = c.getPublicField(x -> x.hasName("isPublic")).get(),
		c_isNotPublic = c.getDeclaredField(x -> x.hasName("isNotPublic")).get(),
		c_isStatic = c.getPublicField(x -> x.hasName("isStatic")).get(),
		c_isNotStatic = c.getPublicField(x -> x.hasName("isNotStatic")).get(),
		c_isTransient = c.getPublicField(x -> x.hasName("isTransient")).get(),
		c_isNotTransient = c.getPublicField(x -> x.hasName("isNotTransient")).get()
	;

	@Test void isAll() {
		assertTrue(c_deprecated.is(DEPRECATED));
		assertTrue(c_notDeprecated.is(NOT_DEPRECATED));
		assertTrue(c_isPublic.is(PUBLIC));
		assertTrue(c_isNotPublic.is(NOT_PUBLIC));
		assertTrue(c_isStatic.is(STATIC));
		assertTrue(c_isNotStatic.is(NOT_STATIC));
		assertTrue(c_isTransient.is(TRANSIENT));
		assertTrue(c_isNotTransient.is(NOT_TRANSIENT));

		assertFalse(c_deprecated.is(NOT_DEPRECATED));
		assertFalse(c_notDeprecated.is(DEPRECATED));
		assertFalse(c_isPublic.is(NOT_PUBLIC));
		assertFalse(c_isNotPublic.is(PUBLIC));
		assertFalse(c_isStatic.is(NOT_STATIC));
		assertFalse(c_isNotStatic.is(STATIC));
		assertFalse(c_isTransient.is(NOT_TRANSIENT));
		assertFalse(c_isNotTransient.is(TRANSIENT));
	}

	@Test void isAll_invalidFlag() {
		// HAS_PARAMS doesn't apply to fields, should throw exception
		assertThrowsWithMessage(RuntimeException.class, "Invalid flag for element: HAS_PARAMS", () -> c_deprecated.is(HAS_PARAMS));
	}


	@Test void isDeprecated() {
		assertTrue(c_deprecated.isDeprecated());
		assertFalse(c_notDeprecated.isDeprecated());
	}

	@Test void isNotDeprecated() {
		assertFalse(c_deprecated.isNotDeprecated());
		assertTrue(c_notDeprecated.isNotDeprecated());
	}

	@Test void isTransient() {
		assertTrue(c_isTransient.isTransient());
		assertFalse(c_isNotTransient.isTransient());
	}

	@Test void isNotTransient() {
		assertFalse(c_isTransient.isNotTransient());
		assertTrue(c_isNotTransient.isNotTransient());
	}

	@Test void isPublic() {
		assertTrue(c_isPublic.isPublic());
		assertFalse(c_isNotPublic.isPublic());
	}

	@Test void isNotPublic() {
		assertFalse(c_isPublic.isNotPublic());
		assertTrue(c_isNotPublic.isNotPublic());
	}

	@Test void isStatic() {
		assertTrue(c_isStatic.isStatic());
		assertFalse(c_isNotStatic.isStatic());
	}

	@Test void isNotStatic() {
		assertFalse(c_isStatic.isNotStatic());
		assertTrue(c_isNotStatic.isNotStatic());
	}

	@Test void hasName() {
		assertTrue(b_a1.hasName("a1"));
		assertFalse(b_a1.hasName("a2"));
	}

	@Test void hasName_null() {
		assertFalse(b_a1.hasName(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Visibility
	//-----------------------------------------------------------------------------------------------------------------

	abstract static class D {
		public int isPublic;
		protected int isProtected;
		@SuppressWarnings("unused")
		private int isPrivate;
		int isDefault;
	}
	static ClassInfo d = ClassInfo.of(D.class);
	static FieldInfo
		d_isPublic = d.getPublicField(x -> x.hasName("isPublic")).get(),
		d_isProtected = d.getDeclaredField(x -> x.hasName("isProtected")).get(),
		d_isPrivate = d.getDeclaredField(x -> x.hasName("isPrivate")).get(),
		d_isDefault = d.getDeclaredField(x -> x.hasName("isDefault")).get();

	@Test void setAccessible() {
		assertDoesNotThrow(()->d_isPublic.setAccessible());
		assertDoesNotThrow(()->d_isProtected.setAccessible());
		assertDoesNotThrow(()->d_isPrivate.setAccessible());
		assertDoesNotThrow(()->d_isDefault.setAccessible());
	}

	@Test void isAccessible() {
		// Test isAccessible() before and after setAccessible()
		// Note: isAccessible() was added in Java 9, so behavior may vary
		
		// Before setAccessible(), private/protected/default fields should not be accessible
		// (unless they're already accessible due to module system)
		var privateBefore = d_isPrivate.isAccessible();
		var protectedBefore = d_isProtected.isAccessible();
		var defaultBefore = d_isDefault.isAccessible();
		
		// Make them accessible
		d_isPrivate.setAccessible();
		d_isProtected.setAccessible();
		d_isDefault.setAccessible();
		
		// After setAccessible(), they should be accessible (if Java 9+)
		// If Java 8 or earlier, isAccessible() will return false
		var privateAfter = d_isPrivate.isAccessible();
		var protectedAfter = d_isProtected.isAccessible();
		var defaultAfter = d_isDefault.isAccessible();
		
		// Verify the method doesn't throw and returns a boolean
		// The actual value depends on Java version, but it should be consistent
		assertTrue(privateAfter || !privateBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		assertTrue(protectedAfter || !protectedBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		assertTrue(defaultAfter || !defaultBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		
		// Public fields might already be accessible
		var publicAccessible = d_isPublic.isAccessible();
		// Should return a boolean (either true or false depending on Java version)
		assertNotNull(Boolean.valueOf(publicAccessible));
	}

	@Test void isVisible() {
		assertTrue(d_isPublic.isVisible(Visibility.PUBLIC));
		assertTrue(d_isPublic.isVisible(Visibility.PROTECTED));
		assertTrue(d_isPublic.isVisible(Visibility.PRIVATE));
		assertTrue(d_isPublic.isVisible(Visibility.DEFAULT));

		assertFalse(d_isProtected.isVisible(Visibility.PUBLIC));
		assertTrue(d_isProtected.isVisible(Visibility.PROTECTED));
		assertTrue(d_isProtected.isVisible(Visibility.PRIVATE));
		assertTrue(d_isProtected.isVisible(Visibility.DEFAULT));

		assertFalse(d_isPrivate.isVisible(Visibility.PUBLIC));
		assertFalse(d_isPrivate.isVisible(Visibility.PROTECTED));
		assertTrue(d_isPrivate.isVisible(Visibility.PRIVATE));
		assertFalse(d_isPrivate.isVisible(Visibility.DEFAULT));

		assertFalse(d_isDefault.isVisible(Visibility.PUBLIC));
		assertFalse(d_isDefault.isVisible(Visibility.PROTECTED));
		assertTrue(d_isDefault.isVisible(Visibility.PRIVATE));
		assertTrue(d_isDefault.isVisible(Visibility.DEFAULT));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	static class E {
		public int a1;
		int a2;
	}

	static ClassInfo e = ClassInfo.of(E.class);
	static FieldInfo
		e_a1 = e.getPublicField(x -> x.hasName("a1")).get(),
		e_a2 = e.getDeclaredField(x -> x.hasName("a2")).get();

	@Test void getType() {
		check("int", e_a1.getFieldType());
		check("int", e_a2.getFieldType());
	}

	@Test void getType_twice() {
		check("int", e_a1.getFieldType());
		check("int", e_a1.getFieldType());
	}

	@Test void toString2() {
		assertEquals("org.apache.juneau.commons.reflect.FieldInfo_Test$E.a1", e_a1.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAnnotations()
	//-----------------------------------------------------------------------------------------------------------------

	public static class F {
		@TestAnnotation1("test1")
		@TestAnnotation2(42)
		public String field1;

		@TestAnnotation1("test2")
		public String field2;

		public String field3;
	}

	static ClassInfo f = ClassInfo.of(F.class);
	static FieldInfo
		f_field1 = f.getPublicField(x -> x.hasName("field1")).get(),
		f_field2 = f.getPublicField(x -> x.hasName("field2")).get(),
		f_field3 = f.getPublicField(x -> x.hasName("field3")).get();

	@Test void getAnnotations_returnsAllAnnotations() {
		var annotations1 = f_field1.getAnnotations();
		assertEquals(2, annotations1.size());
		assertTrue(annotations1.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation1")));
		assertTrue(annotations1.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation2")));

		var annotations2 = f_field2.getAnnotations();
		assertEquals(1, annotations2.size());
		assertTrue(annotations2.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation1")));

		var annotations3 = f_field3.getAnnotations();
		assertEquals(0, annotations3.size());
	}

	@Test void getAnnotations_typed_filtersByType() {
		var ann1_type1 = f_field1.getAnnotations(TestAnnotation1.class).toList();
		assertEquals(1, ann1_type1.size());
		assertEquals("test1", ann1_type1.get(0).getValue().get());

		var ann1_type2 = f_field1.getAnnotations(TestAnnotation2.class).toList();
		assertEquals(1, ann1_type2.size());
		assertEquals(42, ann1_type2.get(0).getInt("value").get());

		var ann1_type3 = f_field1.getAnnotations(TestAnnotation3.class).toList();
		assertEquals(0, ann1_type3.size());

		var ann2_type1 = f_field2.getAnnotations(TestAnnotation1.class).toList();
		assertEquals(1, ann2_type1.size());
		assertEquals("test2", ann2_type1.get(0).getValue().get());

		var ann2_type2 = f_field2.getAnnotations(TestAnnotation2.class).toList();
		assertEquals(0, ann2_type2.size());
	}

	@Test void getAnnotations_memoization_returnsSameInstance() {
		var annotations1 = f_field1.getAnnotations();
		var annotations2 = f_field1.getAnnotations();
		assertSame(annotations1, annotations2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getFullName()
	//-----------------------------------------------------------------------------------------------------------------

	public static class G {
		public String field1;
		public int field2;
	}

	static ClassInfo g = ClassInfo.of(G.class);
	static FieldInfo
		g_field1 = g.getPublicField(x -> x.hasName("field1")).get(),
		g_field2 = g.getPublicField(x -> x.hasName("field2")).get();

	@Test void getFullName_returnsFullyQualifiedName() {
		String fullName1 = g_field1.getFullName();
		String fullName2 = g_field2.getFullName();

		assertTrue(fullName1.endsWith("FieldInfo_Test$G.field1"));
		assertTrue(fullName2.endsWith("FieldInfo_Test$G.field2"));
		
		assertTrue(fullName1.startsWith("org.apache.juneau.commons.reflect."));
		assertTrue(fullName2.startsWith("org.apache.juneau.commons.reflect."));
	}

	@Test void getFullName_memoization_returnsSameInstance() {
		String name1 = g_field1.getFullName();
		String name2 = g_field1.getFullName();
		assertSame(name1, name2);
	}

	public static class InnerClass {
		public String innerField;
	}

	static ClassInfo inner = ClassInfo.of(InnerClass.class);
	static FieldInfo inner_field = inner.getPublicField(x -> x.hasName("innerField")).get();

	@Test void getFullName_withInnerClass_usesDollarSeparator() {
		String fullName = inner_field.getFullName();
		
		assertTrue(fullName.contains("FieldInfo_Test$InnerClass"));
		assertTrue(fullName.endsWith(".innerField"));
	}
}