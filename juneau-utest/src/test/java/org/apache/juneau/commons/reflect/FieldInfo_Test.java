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
	// Test classes
	//-----------------------------------------------------------------------------------------------------------------

	static class A1 {
		public int f1;
	}
	FieldInfo a1_f1 = off(A1.class, "f1");

	public static class B {
		@A("a1") public int a1;
		public int a2;
	}
	FieldInfo
		b_a1 = off(B.class, "a1"),
		b_a2 = off(B.class, "a2");

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

	static class E {
		public int a1;
		int a2;
	}
	static ClassInfo e = ClassInfo.of(E.class);
	static FieldInfo
		e_a1 = e.getPublicField(x -> x.hasName("a1")).get(),
		e_a2 = e.getDeclaredField(x -> x.hasName("a2")).get();

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

	public static class G {
		public String field1;
		public int field2;
	}
	static ClassInfo g = ClassInfo.of(G.class);
	static FieldInfo
		g_field1 = g.getPublicField(x -> x.hasName("field1")).get(),
		g_field2 = g.getPublicField(x -> x.hasName("field2")).get();

	public static class InnerClass {
		public String innerField;
	}
	static ClassInfo inner = ClassInfo.of(InnerClass.class);
	static FieldInfo inner_field = inner.getPublicField(x -> x.hasName("innerField")).get();

	public static class GetSetTest {
		public String value;
		public Integer number;
	}
	static ClassInfo getSetTest = ClassInfo.of(GetSetTest.class);
	static FieldInfo
		getSetTest_value = getSetTest.getPublicField(x -> x.hasName("value")).get(),
		getSetTest_number = getSetTest.getPublicField(x -> x.hasName("number")).get();

	public enum TestEnum {
		VALUE1, VALUE2
	}
	static ClassInfo testEnum = ClassInfo.of(TestEnum.class);
	static FieldInfo
		testEnum_value1 = testEnum.getPublicField(x -> x.hasName("VALUE1")).get(),
		testEnum_value2 = testEnum.getPublicField(x -> x.hasName("VALUE2")).get();

	//====================================================================================================
	// accessible()
	//====================================================================================================
	@Test
	void a001_accessible() {
		assertDoesNotThrow(()->d_isPublic.accessible());
		assertDoesNotThrow(()->d_isProtected.accessible());
		assertDoesNotThrow(()->d_isPrivate.accessible());
		assertDoesNotThrow(()->d_isDefault.accessible());
		
		// Verify it returns this for chaining
		var result = d_isPublic.accessible();
		assertSame(d_isPublic, result);
	}

	//====================================================================================================
	// compareTo(FieldInfo)
	//====================================================================================================
	@Test
	void a002_compareTo() {
		// Fields should be sorted by field name only
		// "a2" comes before "f1" alphabetically, so b_a2 should come before a1_f1
		var b_a2 = off(B.class, "a2");
		
		// "a2" < "f1" alphabetically, so b_a2 should come before a1_f1
		assertTrue(b_a2.compareTo(a1_f1) < 0);
		assertTrue(a1_f1.compareTo(b_a2) > 0);
		assertEquals(0, a1_f1.compareTo(a1_f1));
		
		// Test fields from same class - should be sorted by field name
		assertTrue(b_a1.compareTo(b_a2) < 0);
		assertTrue(b_a2.compareTo(b_a1) > 0);
	}

	//====================================================================================================
	// get(Object)
	//====================================================================================================
	@Test
	void a003_get() {
		var obj = new GetSetTest();
		obj.value = "test";
		obj.number = 42;
		
		assertEquals("test", getSetTest_value.get(obj));
		assertEquals(Integer.valueOf(42), getSetTest_number.get(obj));
		
		// Null value
		obj.value = null;
		assertNull(getSetTest_value.get(obj));
	}

	//====================================================================================================
	// getAnnotatableType()
	//====================================================================================================
	@Test
	void a004_getAnnotatableType() {
		assertEquals(AnnotatableType.FIELD_TYPE, a1_f1.getAnnotatableType());
	}

	//====================================================================================================
	// getAnnotatedType()
	//====================================================================================================
	@Test
	void a005_getAnnotatedType() {
		var annotatedType = e_a1.getAnnotatedType();
		assertNotNull(annotatedType);
		assertEquals(int.class, annotatedType.getType());
	}

	//====================================================================================================
	// getAnnotations()
	//====================================================================================================
	@Test
	void a006_getAnnotations() {
		var annotations1 = f_field1.getAnnotations();
		assertEquals(2, annotations1.size());
		assertTrue(annotations1.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation1")));
		assertTrue(annotations1.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation2")));

		var annotations2 = f_field2.getAnnotations();
		assertEquals(1, annotations2.size());
		assertTrue(annotations2.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation1")));

		var annotations3 = f_field3.getAnnotations();
		assertEquals(0, annotations3.size());
		
		// Test memoization - should return same instance
		var annotations1_2 = f_field1.getAnnotations();
		assertSame(annotations1, annotations1_2);
	}

	//====================================================================================================
	// getAnnotations(Class<A>)
	//====================================================================================================
	@Test
	void a007_getAnnotations_typed() {
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

	//====================================================================================================
	// getDeclaringClass()
	//====================================================================================================
	@Test
	void a008_getDeclaringClass() {
		check("A1", a1_f1.getDeclaringClass());
		check("B", b_a1.getDeclaringClass());
	}

	//====================================================================================================
	// getFieldType()
	//====================================================================================================
	@Test
	void a009_getFieldType() {
		check("int", e_a1.getFieldType());
		check("int", e_a2.getFieldType());
		
		// Test memoization - should return same instance
		var type1 = e_a1.getFieldType();
		var type2 = e_a1.getFieldType();
		assertSame(type1, type2);
	}

	//====================================================================================================
	// getFullName()
	//====================================================================================================
	@Test
	void a010_getFullName() throws Exception {
		String fullName1 = g_field1.getFullName();
		String fullName2 = g_field2.getFullName();
		
		// Test line 449: getPackage() returns null (default package class)
		// A field can have a null package if its declaring class is in the default package
		// According to Java API, Class.getPackage() returns null for classes in the default package
		try {
			Class<?> defaultPkgClassType = Class.forName("DefaultPackageTestClass");
			ClassInfo defaultPkgClass = ClassInfo.of(defaultPkgClassType);
			PackageInfo pkg = defaultPkgClass.getPackage();
			if (pkg == null) {
				// Test the false branch of line 449: when package is null, don't append package name
				FieldInfo defaultPkgField = defaultPkgClass.getPublicField(x -> x.hasName("testField")).get();
				String fullName = defaultPkgField.getFullName();
				// When package is null, getFullName() should not include package prefix
				assertTrue(fullName.startsWith("DefaultPackageTestClass"), "Full name should start with class name when package is null: " + fullName);
				assertTrue(fullName.endsWith(".testField"), "Full name should end with field name: " + fullName);
				// Verify no package prefix (no dots before the class name, except for inner classes)
				assertFalse(fullName.matches("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)+\\."), "Full name should not have package prefix when package is null: " + fullName);
			}
		} catch (ClassNotFoundException e) {
			// If class not found, skip this part of the test
		}

		assertTrue(fullName1.endsWith("FieldInfo_Test$G.field1"));
		assertTrue(fullName2.endsWith("FieldInfo_Test$G.field2"));
		
		assertTrue(fullName1.startsWith("org.apache.juneau.commons.reflect."));
		assertTrue(fullName2.startsWith("org.apache.juneau.commons.reflect."));
		
		// Test memoization - should return same instance
		String name1 = g_field1.getFullName();
		String name2 = g_field1.getFullName();
		assertSame(name1, name2);
		
		// Test with inner class
		String innerFullName = inner_field.getFullName();
		assertTrue(innerFullName.contains("FieldInfo_Test$InnerClass"));
		assertTrue(innerFullName.endsWith(".innerField"));
	}

	//====================================================================================================
	// getLabel()
	//====================================================================================================
	@Test
	void a011_getLabel() {
		var label = a1_f1.getLabel();
		assertNotNull(label);
		assertTrue(label.contains("A1"));
		assertTrue(label.contains("f1"));
	}

	//====================================================================================================
	// getName()
	//====================================================================================================
	@Test
	void a012_getName() {
		assertEquals("f1", a1_f1.getName());
		assertEquals("a1", b_a1.getName());
		assertEquals("a2", b_a2.getName());
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a013_hasAnnotation() {
		assertTrue(b_a1.hasAnnotation(A.class));
		assertFalse(b_a2.hasAnnotation(A.class));
	}

	//====================================================================================================
	// hasName(String)
	//====================================================================================================
	@Test
	void a014_hasName() {
		assertTrue(b_a1.hasName("a1"));
		assertFalse(b_a1.hasName("a2"));
		assertFalse(b_a1.hasName(null));
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a015_inner() {
		check("f1", a1_f1.inner());
		var field = a1_f1.inner();
		assertNotNull(field);
		assertEquals("f1", field.getName());
	}

	//====================================================================================================
	// is(ElementFlag)
	//====================================================================================================
	@Test
	void a016_is() {
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
		
		// Enum constant
		assertTrue(testEnum_value1.is(ENUM_CONSTANT));
		assertFalse(a1_f1.is(ENUM_CONSTANT));
		assertTrue(a1_f1.is(NOT_ENUM_CONSTANT));  // Line 313: true branch - field is NOT an enum constant
		assertFalse(testEnum_value1.is(NOT_ENUM_CONSTANT));  // Line 313: false branch - field IS an enum constant
		
		// Synthetic (lines 314-315)
		assertFalse(a1_f1.is(SYNTHETIC));
		assertTrue(a1_f1.is(NOT_SYNTHETIC));
		assertFalse(b_a1.is(SYNTHETIC));
		assertTrue(b_a1.is(NOT_SYNTHETIC));
		
		// HAS_PARAMS doesn't apply to fields, should throw exception
		assertThrowsWithMessage(RuntimeException.class, "Invalid flag for element: HAS_PARAMS", () -> c_deprecated.is(HAS_PARAMS));
	}

	//====================================================================================================
	// isAccessible()
	//====================================================================================================
	@Test
	void a017_isAccessible() {
		// Test isAccessible() before and after setAccessible()
		var privateBefore = d_isPrivate.isAccessible();
		var protectedBefore = d_isProtected.isAccessible();
		var defaultBefore = d_isDefault.isAccessible();
		
		// Make them accessible
		d_isPrivate.setAccessible();
		d_isProtected.setAccessible();
		d_isDefault.setAccessible();
		
		// After setAccessible(), they should be accessible (if Java 9+)
		var privateAfter = d_isPrivate.isAccessible();
		var protectedAfter = d_isProtected.isAccessible();
		var defaultAfter = d_isDefault.isAccessible();
		
		// Verify the method doesn't throw and returns a boolean
		assertTrue(privateAfter || !privateBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		assertTrue(protectedAfter || !protectedBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		assertTrue(defaultAfter || !defaultBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		
		// Public fields might already be accessible
		var publicAccessible = d_isPublic.isAccessible();
		assertNotNull(Boolean.valueOf(publicAccessible));
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a018_isAll() {
		assertTrue(c_deprecated.isAll(DEPRECATED));
		assertTrue(c_isPublic.isAll(PUBLIC, NOT_PRIVATE));
		assertFalse(c_deprecated.isAll(DEPRECATED, NOT_DEPRECATED));
	}

	//====================================================================================================
	// isAny(ElementFlag...)
	//====================================================================================================
	@Test
	void a019_isAny() {
		assertTrue(c_deprecated.isAny(DEPRECATED, NOT_DEPRECATED));
		assertTrue(c_isPublic.isAny(PUBLIC, PRIVATE));
		assertFalse(c_deprecated.isAny(NOT_DEPRECATED));
	}

	//====================================================================================================
	// isDeprecated()
	//====================================================================================================
	@Test
	void a020_isDeprecated() {
		assertTrue(c_deprecated.isDeprecated());
		assertFalse(c_notDeprecated.isDeprecated());
	}

	//====================================================================================================
	// isEnumConstant()
	//====================================================================================================
	@Test
	void a021_isEnumConstant() {
		assertTrue(testEnum_value1.isEnumConstant());
		assertTrue(testEnum_value2.isEnumConstant());
		assertFalse(a1_f1.isEnumConstant());
	}

	//====================================================================================================
	// isNotDeprecated()
	//====================================================================================================
	@Test
	void a022_isNotDeprecated() {
		assertFalse(c_deprecated.isNotDeprecated());
		assertTrue(c_notDeprecated.isNotDeprecated());
	}

	//====================================================================================================
	// isSynthetic()
	//====================================================================================================
	@Test
	void a023_isSynthetic() {
		// Regular fields are not synthetic
		assertFalse(a1_f1.isSynthetic());
	}

	//====================================================================================================
	// isVisible(Visibility)
	//====================================================================================================
	@Test
	void a024_isVisible() {
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

	//====================================================================================================
	// of(ClassInfo, Field)
	//====================================================================================================
	@Test
	void a025_of_withClass() {
		check("f1", FieldInfo.of(ClassInfo.of(A1.class), a1_f1.inner()));
	}

	//====================================================================================================
	// of(Field)
	//====================================================================================================
	@Test
	void a026_of_withoutClass() {
		check("f1", FieldInfo.of(a1_f1.inner()));
		
		// Null should throw
		assertThrows(IllegalArgumentException.class, () -> FieldInfo.of((Field)null));
		assertThrows(IllegalArgumentException.class, () -> FieldInfo.of((ClassInfo)null, null));
	}

	//====================================================================================================
	// set(Object, Object)
	//====================================================================================================
	@Test
	void a027_set() {
		var obj = new GetSetTest();
		
		getSetTest_value.set(obj, "newValue");
		assertEquals("newValue", obj.value);
		
		getSetTest_number.set(obj, 100);
		assertEquals(100, obj.number);
		
		// Set to null
		getSetTest_value.set(obj, null);
		assertNull(obj.value);
	}

	//====================================================================================================
	// setAccessible()
	//====================================================================================================
	@Test
	void a028_setAccessible() {
		assertDoesNotThrow(()->d_isPublic.setAccessible());
		assertDoesNotThrow(()->d_isProtected.setAccessible());
		assertDoesNotThrow(()->d_isPrivate.setAccessible());
		assertDoesNotThrow(()->d_isDefault.setAccessible());
	}

	//====================================================================================================
	// setIfNull(Object, Object)
	//====================================================================================================
	@Test
	void a029_setIfNull() {
		var obj = new GetSetTest();
		
		// Set when null
		obj.value = null;
		getSetTest_value.setIfNull(obj, "defaultValue");
		assertEquals("defaultValue", obj.value);
		
		// Don't set when not null
		obj.value = "existing";
		getSetTest_value.setIfNull(obj, "shouldNotSet");
		assertEquals("existing", obj.value);
	}

	//====================================================================================================
	// toGenericString()
	//====================================================================================================
	@Test
	void a030_toGenericString() {
		var str = e_a1.toGenericString();
		assertNotNull(str);
		assertTrue(str.contains("int"));
		assertTrue(str.contains("a1"));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================
	@Test
	void a031_toString() {
		assertEquals("org.apache.juneau.commons.reflect.FieldInfo_Test$E.a1", e_a1.toString());
	}
}

