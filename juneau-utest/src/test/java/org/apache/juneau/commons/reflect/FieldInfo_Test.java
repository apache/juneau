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
}