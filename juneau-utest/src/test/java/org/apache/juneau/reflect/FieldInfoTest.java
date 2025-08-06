// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.reflect.ReflectFlags.*;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FieldInfoTest extends SimpleTestBase {

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
    		return ((ClassInfo)t).getSimpleName();
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
		check(null, FieldInfo.of(null));
		check(null, FieldInfo.of(null, null));
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

	@Test void getAnnotation() {
		check("@A(a1)", b_a1.getAnnotation(A.class));
		check(null, b_a2.getAnnotation(A.class));
	}

	@Test void getAnnotation_null() {
		check(null, b_a1.getAnnotation(null));
	}

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
		c_deprecated = c.getPublicField(x -> x.hasName("deprecated")),
		c_notDeprecated = c.getPublicField(x -> x.hasName("notDeprecated")),
		c_isPublic = c.getPublicField(x -> x.hasName("isPublic")),
		c_isNotPublic = c.getDeclaredField(x -> x.hasName("isNotPublic")),
		c_isStatic = c.getPublicField(x -> x.hasName("isStatic")),
		c_isNotStatic = c.getPublicField(x -> x.hasName("isNotStatic")),
		c_isTransient = c.getPublicField(x -> x.hasName("isTransient")),
		c_isNotTransient = c.getPublicField(x -> x.hasName("isNotTransient"))
	;

	@Test void isAll() {
		assertTrue(c_deprecated.isAll(DEPRECATED));
		assertTrue(c_notDeprecated.isAll(NOT_DEPRECATED));
		assertTrue(c_isPublic.isAll(PUBLIC));
		assertTrue(c_isNotPublic.isAll(NOT_PUBLIC));
		assertTrue(c_isStatic.isAll(STATIC));
		assertTrue(c_isNotStatic.isAll(NOT_STATIC));
		assertTrue(c_isTransient.isAll(TRANSIENT));
		assertTrue(c_isNotTransient.isAll(NOT_TRANSIENT));

		assertFalse(c_deprecated.isAll(NOT_DEPRECATED));
		assertFalse(c_notDeprecated.isAll(DEPRECATED));
		assertFalse(c_isPublic.isAll(NOT_PUBLIC));
		assertFalse(c_isNotPublic.isAll(PUBLIC));
		assertFalse(c_isStatic.isAll(NOT_STATIC));
		assertFalse(c_isNotStatic.isAll(STATIC));
		assertFalse(c_isTransient.isAll(NOT_TRANSIENT));
		assertFalse(c_isNotTransient.isAll(TRANSIENT));
	}

	@Test void isAll_invalidFlag() {
		assertThrows(BasicRuntimeException.class, ()->c_deprecated.isAll(HAS_PARAMS), "Invalid flag for field: HAS_PARAMS");
	}

	@Test void isAny() {
		assertTrue(c_deprecated.isAny(DEPRECATED));
		assertTrue(c_notDeprecated.isAny(NOT_DEPRECATED));
		assertTrue(c_isPublic.isAny(PUBLIC));
		assertTrue(c_isNotPublic.isAny(NOT_PUBLIC));
		assertTrue(c_isStatic.isAny(STATIC));
		assertTrue(c_isNotStatic.isAny(NOT_STATIC));
		assertTrue(c_isTransient.isAny(TRANSIENT));
		assertTrue(c_isNotTransient.isAny(NOT_TRANSIENT));

		assertFalse(c_deprecated.isAny(NOT_DEPRECATED));
		assertFalse(c_notDeprecated.isAny(DEPRECATED));
		assertFalse(c_isPublic.isAny(NOT_PUBLIC));
		assertFalse(c_isNotPublic.isAny(PUBLIC));
		assertFalse(c_isStatic.isAny(NOT_STATIC));
		assertFalse(c_isNotStatic.isAny(STATIC));
		assertFalse(c_isTransient.isAny(NOT_TRANSIENT));
		assertFalse(c_isNotTransient.isAny(TRANSIENT));
	}

	@Test void isAny_invalidFlag() {
		assertThrows(BasicRuntimeException.class, ()->c_deprecated.isAny(HAS_PARAMS), "Invalid flag for field: HAS_PARAMS");
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
		d_isPublic = d.getPublicField(x -> x.hasName("isPublic")),
		d_isProtected = d.getDeclaredField(x -> x.hasName("isProtected")),
		d_isPrivate = d.getDeclaredField(x -> x.hasName("isPrivate")),
		d_isDefault = d.getDeclaredField(x -> x.hasName("isDefault"));

	@Test void setAccessible() {
		assertNotThrown(()->d_isPublic.setAccessible());
		assertNotThrown(()->d_isProtected.setAccessible());
		assertNotThrown(()->d_isPrivate.setAccessible());
		assertNotThrown(()->d_isDefault.setAccessible());
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
		e_a1 = e.getPublicField(x -> x.hasName("a1")),
		e_a2 = e.getDeclaredField(x -> x.hasName("a2"));

	@Test void getType() {
		check("int", e_a1.getType());
		check("int", e_a2.getType());
	}

	@Test void getType_twice() {
		check("int", e_a1.getType());
		check("int", e_a1.getType());
	}

	@Test void toString2() {
		assertEquals("org.apache.juneau.reflect.FieldInfoTest$E.a1", e_a1.toString());
	}
}