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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.junit.jupiter.api.*;

class AnnotationInfo_Test extends TestBase {

	//====================================================================================================
	// Test annotations and classes
	//====================================================================================================

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface TestAnnotation {
		String value() default "default";
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface MultiTypeAnnotation {
		String stringValue() default "default";

		int intValue() default 0;

		boolean boolValue() default true;

		long longValue() default 100L;

		double doubleValue() default 3.14;

		float floatValue() default 2.5f;

		Class<?> classValue() default String.class;

		String[] stringArray() default { "a", "b" };

		Class<?>[] classArray() default { String.class, Integer.class };
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface RankedAnnotation {
		int rank() default 0;
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface UnrankedAnnotation {
		String value() default "";
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface ClassArrayAnnotation {
		Class<?>[] classes() default {};
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface ClassValueAnnotation {
		Class<?> value() default String.class;
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	@Documented
	public static @interface DocumentedAnnotation {}

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(GroupAnnotation.class)
	public static @interface GroupAnnotation {}

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(GroupAnnotation.class)
	public static @interface GroupMember1 {}

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(GroupAnnotation.class)
	public static @interface GroupMember2 {}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface NotInGroup {}

	@TestAnnotation("test")
	public static class TestClass {}

	@MultiTypeAnnotation(stringValue = "test", intValue = 123, boolValue = false, longValue = 999L, doubleValue = 1.23, floatValue = 4.56f, classValue = Integer.class, stringArray = { "x", "y",
		"z" }, classArray = { Long.class, Double.class })
	public static class MultiTypeClass {}

	@RankedAnnotation(rank = 5)
	public static class RankedClass {}

	@UnrankedAnnotation
	public static class UnrankedClass {}

	@ClassArrayAnnotation(classes = { String.class, Integer.class })
	public static class ClassArrayClass {}

	@ClassValueAnnotation(Integer.class)
	public static class ClassValueClass {}

	@DocumentedAnnotation
	public static class DocumentedClass {}

	@GroupMember1
	@GroupMember2
	@NotInGroup
	public static class GroupTestClass {}

	//====================================================================================================
	// of() - Static factory method
	//====================================================================================================

	@Test
	void a01_of_createsAnnotationInfo() {
		var ci = ClassInfo.of(TestClass.class);
		var annotation = ci.inner().getAnnotation(TestAnnotation.class);
		var ai = AnnotationInfo.of(ci, annotation);

		assertNotNull(ai);
		assertEquals(TestAnnotation.class, ai.annotationType());
		assertEquals("test", ai.getValue().orElse(null));
	}

	@Test
	void a02_of_withNullAnnotation_throwsException() {
		var ci = ClassInfo.of(TestClass.class);
		assertThrows(IllegalArgumentException.class, () -> AnnotationInfo.of(ci, null));
	}

	//====================================================================================================
	// annotationType()
	//====================================================================================================

	@Test
	void b01_annotationType_returnsCorrectType() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		assertEquals(TestAnnotation.class, ai.annotationType());
	}

	//====================================================================================================
	// cast()
	//====================================================================================================

	@Test
	void c01_cast_sameType_returnsThis() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var casted = ai.cast(TestAnnotation.class);
		assertNotNull(casted);
		assertSame(ai, casted);
	}

	@Test
	void c02_cast_differentType_returnsNull() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var casted = ai.cast(Deprecated.class);
		assertNull(casted);
	}

	//====================================================================================================
	// equals() and hashCode()
	//====================================================================================================

	@Test
	void d01_equals_sameAnnotation_returnsTrue() {
		var ci = ClassInfo.of(TestClass.class);
		var ai1 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		var ai2 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);

		assertNotNull(ai1);
		assertNotNull(ai2);
		assertEquals(ai1, ai2);
		assertEquals(ai1.hashCode(), ai2.hashCode());
	}

	@Test
	void d02_equals_differentAnnotation_returnsFalse() {
		var ci = ClassInfo.of(TestClass.class);
		var ai1 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);

		@Deprecated
		class DeprecatedClass {}
		var ci2 = ClassInfo.of(DeprecatedClass.class);
		var ai2 = ci2.getAnnotations(Deprecated.class).findFirst().orElse(null);

		assertNotNull(ai1);
		assertNotNull(ai2);
		assertNotEquals(ai1, ai2);
	}

	@Test
	void d03_equals_withAnnotationInfo_comparesAnnotations() {
		var ci = ClassInfo.of(TestClass.class);
		var ai1 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		var ai2 = AnnotationInfo.of(ci, ci.inner().getAnnotation(TestAnnotation.class));

		assertNotNull(ai1);
		assertEquals(ai1, ai2);
	}

	@Test
	void d04_equals_withAnnotation_comparesAnnotations() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		var annotation = ci.inner().getAnnotation(TestAnnotation.class);

		assertNotNull(ai);
		assertEquals(ai, annotation);
	}

	//====================================================================================================
	// getName()
	//====================================================================================================

	@Test
	void e01_getName_returnsSimpleName() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		assertEquals("TestAnnotation", ai.getName());
	}

	//====================================================================================================
	// getRank()
	//====================================================================================================

	@Test
	void f01_getRank_withRankMethod_returnsRank() {
		var ci = ClassInfo.of(RankedClass.class);
		var ai = ci.getAnnotations(RankedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		assertEquals(5, ai.getRank());
	}

	@Test
	void f02_getRank_withoutRankMethod_returnsZero() {
		var ci = ClassInfo.of(UnrankedClass.class);
		var ai = ci.getAnnotations(UnrankedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		assertEquals(0, ai.getRank());
	}

	//====================================================================================================
	// getMethod()
	//====================================================================================================

	@Test
	void g01_getMethod_existingMethod_returnsMethodInfo() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var method = ai.getMethod("value");
		assertTrue(method.isPresent());
		assertEquals("value", method.get().getSimpleName());
	}

	@Test
	void g02_getMethod_nonexistentMethod_returnsEmpty() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var method = ai.getMethod("nonexistent");
		assertFalse(method.isPresent());
	}

	//====================================================================================================
	// getReturnType()
	//====================================================================================================

	@Test
	void h01_getReturnType_returnsCorrectType() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getReturnType("stringValue").isPresent());
		assertEquals(String.class, ai.getReturnType("stringValue").get().inner());
		assertEquals(int.class, ai.getReturnType("intValue").get().inner());
		assertEquals(boolean.class, ai.getReturnType("boolValue").get().inner());
		assertEquals(long.class, ai.getReturnType("longValue").get().inner());
		assertEquals(double.class, ai.getReturnType("doubleValue").get().inner());
		assertEquals(float.class, ai.getReturnType("floatValue").get().inner());
		assertEquals(Class.class, ai.getReturnType("classValue").get().inner());
		assertEquals(String[].class, ai.getReturnType("stringArray").get().inner());
		assertEquals(Class[].class, ai.getReturnType("classArray").get().inner());
	}

	@Test
	void h02_getReturnType_nonexistentMethod_returnsEmpty() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertFalse(ai.getReturnType("nonexistent").isPresent());
	}

	//====================================================================================================
	// Value retrieval methods - getString(), getInt(), getBoolean(), etc.
	//====================================================================================================

	@Test
	void i01_getString_returnsStringValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getString("stringValue").isPresent());
		assertEquals("test", ai.getString("stringValue").get());
		assertFalse(ai.getString("nonexistent").isPresent());
	}

	@Test
	void i02_getInt_returnsIntValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getInt("intValue").isPresent());
		assertEquals(123, ai.getInt("intValue").get());
		assertFalse(ai.getInt("nonexistent").isPresent());
	}

	@Test
	void i03_getBoolean_returnsBooleanValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getBoolean("boolValue").isPresent());
		assertEquals(false, ai.getBoolean("boolValue").get());
		assertFalse(ai.getBoolean("nonexistent").isPresent());
	}

	@Test
	void i04_getLong_returnsLongValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getLong("longValue").isPresent());
		assertEquals(999L, ai.getLong("longValue").get());
		assertFalse(ai.getLong("nonexistent").isPresent());
	}

	@Test
	void i05_getDouble_returnsDoubleValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getDouble("doubleValue").isPresent());
		assertEquals(1.23, ai.getDouble("doubleValue").get(), 0.001);
		assertFalse(ai.getDouble("nonexistent").isPresent());
	}

	@Test
	void i06_getFloat_returnsFloatValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getFloat("floatValue").isPresent());
		assertEquals(4.56f, ai.getFloat("floatValue").get(), 0.001);
		assertFalse(ai.getFloat("nonexistent").isPresent());
	}

	@Test
	void i07_getClassValue_returnsClassValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getClassValue("classValue").isPresent());
		assertEquals(Integer.class, ai.getClassValue("classValue").get());
		assertFalse(ai.getClassValue("nonexistent").isPresent());
	}

	@Test
	void i08_getStringArray_returnsStringArray() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getStringArray("stringArray").isPresent());
		var array = ai.getStringArray("stringArray").get();
		assertNotNull(array);
		assertEquals(3, array.length);
		assertEquals("x", array[0]);
		assertEquals("y", array[1]);
		assertEquals("z", array[2]);
		assertFalse(ai.getStringArray("nonexistent").isPresent());
	}

	@Test
	void i09_getClassArray_returnsClassArray() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getClassArray("classArray").isPresent());
		var array = ai.getClassArray("classArray").get();
		assertNotNull(array);
		assertEquals(2, array.length);
		assertEquals(Long.class, array[0]);
		assertEquals(Double.class, array[1]);
		assertFalse(ai.getClassArray("nonexistent").isPresent());
	}

	//====================================================================================================
	// getValue() - Convenience method
	//====================================================================================================

	@Test
	void j01_getValue_returnsValueMethod() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var value = ai.getValue();
		assertTrue(value.isPresent());
		assertEquals("test", value.get());
	}

	//====================================================================================================
	// getValue(Class<V> type, String name)
	//====================================================================================================

	@Test
	void k01_getValue_withType_returnsTypedValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var stringValue = ai.getValue(String.class, "stringValue");
		assertTrue(stringValue.isPresent());
		assertEquals("test", stringValue.get());

		// intValue returns int.class, not Integer.class
		var intValue = ai.getValue(int.class, "intValue");
		assertTrue(intValue.isPresent());
		assertEquals(123, intValue.get());
	}

	@Test
	void k02_getValue_wrongType_returnsEmpty() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Try to get stringValue as Integer
		var intValue = ai.getValue(Integer.class, "stringValue");
		assertFalse(intValue.isPresent());
	}

	//====================================================================================================
	// getClassArray(String, Class<T>)
	//====================================================================================================

	@Test
	void l01_getClassArray_typed_returnsTypedArray() {
		var ci = ClassInfo.of(ClassArrayClass.class);
		var ai = ci.getAnnotations(ClassArrayAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Both String and Integer are assignable to Object
		var classes = ai.getClassArray("classes", Object.class);
		assertTrue(classes.isPresent());
		var array = classes.get();
		assertEquals(2, array.length);
		assertEquals(String.class, array[0]);
		assertEquals(Integer.class, array[1]);
	}

	@Test
	void l02_getClassArray_typed_notAssignable_returnsEmpty() {
		var ci = ClassInfo.of(ClassArrayClass.class);
		var ai = ci.getAnnotations(ClassArrayAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// String and Integer are not assignable to Exception
		var classes = ai.getClassArray("classes", Exception.class);
		assertFalse(classes.isPresent());
	}

	//====================================================================================================
	// getClassValue(String, Class<T>)
	//====================================================================================================

	@Test
	void m01_getClassValue_typed_returnsTypedClass() {
		var ci = ClassInfo.of(ClassValueClass.class);
		var ai = ci.getAnnotations(ClassValueAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var numberClass = ai.getClassValue("value", Number.class);
		assertTrue(numberClass.isPresent());
		assertEquals(Integer.class, numberClass.get());
	}

	@Test
	void m02_getClassValue_typed_notAssignable_returnsEmpty() {
		var ci = ClassInfo.of(ClassValueClass.class);
		var ai = ci.getAnnotations(ClassValueAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Integer is not assignable to Exception
		var exceptionClass = ai.getClassValue("value", Exception.class);
		assertFalse(exceptionClass.isPresent());
	}

	//====================================================================================================
	// hasName() and hasSimpleName()
	//====================================================================================================

	@Test
	void n01_hasName_returnsTrueForFullyQualifiedName() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var fullyQualifiedName = TestAnnotation.class.getName();
		assertTrue(ai.hasName(fullyQualifiedName));
		assertFalse(ai.hasName("TestAnnotation"));
	}

	@Test
	void n02_hasSimpleName_returnsTrueForSimpleName() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.hasSimpleName("TestAnnotation"));
		assertFalse(ai.hasSimpleName(TestAnnotation.class.getName()));
	}

	//====================================================================================================
	// hasAnnotation()
	//====================================================================================================

	@Test
	void o01_hasAnnotation_withMetaAnnotation_returnsTrue() {
		var ci = ClassInfo.of(DocumentedClass.class);
		var ai = ci.getAnnotations(DocumentedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.hasAnnotation(Documented.class));
	}

	@Test
	void o02_hasAnnotation_withoutMetaAnnotation_returnsFalse() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertFalse(ai.hasAnnotation(Documented.class));
	}

	//====================================================================================================
	// inner()
	//====================================================================================================

	@Test
	void p01_inner_returnsWrappedAnnotation() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var annotation = ai.inner();
		assertNotNull(annotation);
		assertEquals(TestAnnotation.class, annotation.annotationType());
		assertEquals("test", annotation.value());
	}

	//====================================================================================================
	// isInGroup()
	//====================================================================================================

	@Test
	void q01_isInGroup_returnsTrueForGroupMembers() {
		var ci = ClassInfo.of(GroupTestClass.class);
		var groupMember1 = ci.getAnnotations(GroupMember1.class).findFirst().orElse(null);
		var groupMember2 = ci.getAnnotations(GroupMember2.class).findFirst().orElse(null);
		var notInGroup = ci.getAnnotations(NotInGroup.class).findFirst().orElse(null);

		assertNotNull(groupMember1);
		assertNotNull(groupMember2);
		assertNotNull(notInGroup);

		assertTrue(groupMember1.isInGroup(GroupAnnotation.class));
		assertTrue(groupMember2.isInGroup(GroupAnnotation.class));
		assertFalse(notInGroup.isInGroup(GroupAnnotation.class));
	}

	//====================================================================================================
	// isType()
	//====================================================================================================

	@Test
	void r01_isType_sameType_returnsTrue() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.isType(TestAnnotation.class));
	}

	@Test
	void r02_isType_differentType_returnsFalse() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertFalse(ai.isType(Deprecated.class));
	}

	//====================================================================================================
	// toMap()
	//====================================================================================================

	@Test
	void s01_toMap_returnsMapRepresentation() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var map = ai.toMap();
		assertNotNull(map);
		assertTrue(map.containsKey("CLASS_TYPE"));
		assertTrue(map.containsKey("@TestAnnotation"));

		var annotationMap = (java.util.Map<String,Object>)map.get("@TestAnnotation");
		assertNotNull(annotationMap);
		assertEquals("test", annotationMap.get("value"));
	}

	//====================================================================================================
	// toSimpleString()
	//====================================================================================================

	@Test
	void t01_toSimpleString_returnsFormattedString() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var str = ai.toSimpleString();
		assertNotNull(str);
		assertTrue(str.contains("@TestAnnotation"));
		assertTrue(str.contains("on="));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================

	@Test
	void u01_toString_returnsStringRepresentation() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var str = ai.toString();
		assertNotNull(str);
		// toString() returns the map representation
		assertTrue(str.contains("CLASS_TYPE") || str.contains("@TestAnnotation"));
	}
}
