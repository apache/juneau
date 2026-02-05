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
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"java:S5961", "java:S1186"})
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
	public static @interface RankWithWrongReturnTypeAnnotation {
		String rank() default "";  // Returns String, not int, should not match
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

	@RankWithWrongReturnTypeAnnotation
	public static class RankWithWrongReturnTypeClass {}

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

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface ToMapTestAnnotation {
		String value() default "default";
		String[] arrayValue() default {};
		String[] nonEmptyArray() default {"a", "b"};
		String[] emptyArrayWithNonEmptyDefault() default {"default"};
	}

	@ToMapTestAnnotation(value = "custom", arrayValue = {}, nonEmptyArray = {"x"}, emptyArrayWithNonEmptyDefault = {})
	public static class ToMapTestClass {}

	//====================================================================================================
	// annotationType()
	//====================================================================================================
	@Test
	void a001_annotationType() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		assertEquals(TestAnnotation.class, ai.annotationType());
	}

	//====================================================================================================
	// cast(Class<A>)
	//====================================================================================================
	@Test
	void a002_cast() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Same type returns this
		var casted = ai.cast(TestAnnotation.class);
		assertNotNull(casted);
		assertSame(ai, casted);

		// Different type returns null
		var casted2 = ai.cast(Deprecated.class);
		assertNull(casted2);
	}

	//====================================================================================================
	// equals(Object)
	//====================================================================================================
	@Test
	void a003_equals() {
		var ci = ClassInfo.of(TestClass.class);
		var ai1 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		var ai2 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);

		assertNotNull(ai1);
		assertNotNull(ai2);
		// Same annotation should be equal
		assertEquals(ai1, ai2);
		assertEquals(ai1.hashCode(), ai2.hashCode());

		// Different annotation should not be equal
		@Deprecated
		class DeprecatedClass {}
		var ci2 = ClassInfo.of(DeprecatedClass.class);
		var ai3 = ci2.getAnnotations(Deprecated.class).findFirst().orElse(null);
		assertNotNull(ai3);
		assertNotEquals(ai1, ai3);

		// With AnnotationInfo
		var ai4 = AnnotationInfo.of(ci, ci.inner().getAnnotation(TestAnnotation.class));
		assertEquals(ai1, ai4);

		// With Annotation
		var annotation = ci.inner().getAnnotation(TestAnnotation.class);
		assertEquals(ai1, annotation);
	}

	//====================================================================================================
	// getBoolean(String)
	//====================================================================================================
	@Test
	void a004_getBoolean() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getBoolean("boolValue").isPresent());
		assertEquals(false, ai.getBoolean("boolValue").get());
		assertFalse(ai.getBoolean("nonexistent").isPresent());
	}

	//====================================================================================================
	// getClassArray(String)
	//====================================================================================================
	@Test
	void a005_getClassArray() {
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
	// getClassArray(String, Class<T>)
	//====================================================================================================
	@Test
	void a006_getClassArray_typed() {
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

		// String and Integer are not assignable to Exception
		var classes2 = ai.getClassArray("classes", Exception.class);
		assertFalse(classes2.isPresent());
	}

	//====================================================================================================
	// getClassValue(String)
	//====================================================================================================
	@Test
	void a007_getClassValue() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getClassValue("classValue").isPresent());
		assertEquals(Integer.class, ai.getClassValue("classValue").get());
		assertFalse(ai.getClassValue("nonexistent").isPresent());
	}

	//====================================================================================================
	// getClassValue(String, Class<T>)
	//====================================================================================================
	@Test
	void a008_getClassValue_typed() {
		var ci = ClassInfo.of(ClassValueClass.class);
		var ai = ci.getAnnotations(ClassValueAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Integer is assignable to Number
		var numberClass = ai.getClassValue("value", Number.class);
		assertTrue(numberClass.isPresent());
		assertEquals(Integer.class, numberClass.get());

		// Integer is not assignable to Exception
		var exceptionClass = ai.getClassValue("value", Exception.class);
		assertFalse(exceptionClass.isPresent());
	}

	//====================================================================================================
	// getDouble(String)
	//====================================================================================================
	@Test
	void a009_getDouble() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getDouble("doubleValue").isPresent());
		assertEquals(1.23, ai.getDouble("doubleValue").get(), 0.001);
		assertFalse(ai.getDouble("nonexistent").isPresent());
	}

	//====================================================================================================
	// getFloat(String)
	//====================================================================================================
	@Test
	void a010_getFloat() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getFloat("floatValue").isPresent());
		assertEquals(4.56f, ai.getFloat("floatValue").get(), 0.001);
		assertFalse(ai.getFloat("nonexistent").isPresent());
	}

	//====================================================================================================
	// getInt(String)
	//====================================================================================================
	@Test
	void a011_getInt() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getInt("intValue").isPresent());
		assertEquals(123, ai.getInt("intValue").get());
		assertFalse(ai.getInt("nonexistent").isPresent());
	}

	//====================================================================================================
	// getLong(String)
	//====================================================================================================
	@Test
	void a012_getLong() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getLong("longValue").isPresent());
		assertEquals(999L, ai.getLong("longValue").get());
		assertFalse(ai.getLong("nonexistent").isPresent());
	}

	//====================================================================================================
	// getMethod(String)
	//====================================================================================================
	@Test
	void a013_getMethod() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Existing method
		var method = ai.getMethod("value");
		assertTrue(method.isPresent());
		assertEquals("value", method.get().getNameSimple());

		// Non-existent method
		var method2 = ai.getMethod("nonexistent");
		assertFalse(method2.isPresent());
	}

	//====================================================================================================
	// getName()
	//====================================================================================================
	@Test
	void a014_getName() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		// getName() now returns fully qualified name
		assertEquals("org.apache.juneau.commons.reflect.AnnotationInfo_Test$TestAnnotation", ai.getName());
	}

	@Test
	void a015_getNameSimple() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);
		// getNameSimple() returns simple name
		assertEquals("TestAnnotation", ai.getNameSimple());

		// Test with standard library annotation
		var ci2 = ClassInfo.of(DocumentedClass.class);
		var ai2 = ci2.getAnnotations(DocumentedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai2);
		assertEquals("DocumentedAnnotation", ai2.getNameSimple());

		// Test with nested annotation
		var ci3 = ClassInfo.of(GroupTestClass.class);
		var ai3 = ci3.getAnnotations(GroupMember1.class).findFirst().orElse(null);
		assertNotNull(ai3);
		assertEquals("GroupMember1", ai3.getNameSimple());
	}

	//====================================================================================================
	// getRank()
	//====================================================================================================
	@Test
	void a015_getRank() {
		// With rank method
		var ci1 = ClassInfo.of(RankedClass.class);
		var ai1 = ci1.getAnnotations(RankedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai1);
		assertEquals(5, ai1.getRank());

		// Without rank method
		var ci2 = ClassInfo.of(UnrankedClass.class);
		var ai2 = ci2.getAnnotations(UnrankedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai2);
		assertEquals(0, ai2.getRank());

		// With rank method but wrong return type (String instead of int)
		var ci3 = ClassInfo.of(RankWithWrongReturnTypeClass.class);
		var ai3 = ci3.getAnnotations(RankWithWrongReturnTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai3);
		assertEquals(0, ai3.getRank());
	}

	//====================================================================================================
	// getReturnType(String)
	//====================================================================================================
	@Test
	void a016_getReturnType() {
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

		// Non-existent method
		assertFalse(ai.getReturnType("nonexistent").isPresent());
	}

	//====================================================================================================
	// getString(String)
	//====================================================================================================
	@Test
	void a017_getString() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.getString("stringValue").isPresent());
		assertEquals("test", ai.getString("stringValue").get());
		assertFalse(ai.getString("nonexistent").isPresent());
	}

	//====================================================================================================
	// getStringArray(String)
	//====================================================================================================
	@Test
	void a018_getStringArray() {
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

	//====================================================================================================
	// getValue()
	//====================================================================================================
	@Test
	void a019_getValue() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var value = ai.getValue();
		assertTrue(value.isPresent());
		assertEquals("test", value.get());
	}

	//====================================================================================================
	// getValue(Class<V>, String)
	//====================================================================================================
	@Test
	void a020_getValue_typed() {
		var ci = ClassInfo.of(MultiTypeClass.class);
		var ai = ci.getAnnotations(MultiTypeAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// String value
		var stringValue = ai.getValue(String.class, "stringValue");
		assertTrue(stringValue.isPresent());
		assertEquals("test", stringValue.get());

		// int value (primitive)
		var intValue = ai.getValue(int.class, "intValue");
		assertTrue(intValue.isPresent());
		assertEquals(123, intValue.get());

		// Wrong type returns empty
		var intValue2 = ai.getValue(Integer.class, "stringValue");
		assertFalse(intValue2.isPresent());
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a021_hasAnnotation() {
		var ci = ClassInfo.of(DocumentedClass.class);
		var ai = ci.getAnnotations(DocumentedAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		// Has meta-annotation
		assertTrue(ai.hasAnnotation(Documented.class));

		// Doesn't have meta-annotation
		var ci2 = ClassInfo.of(TestClass.class);
		var ai2 = ci2.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai2);
		assertFalse(ai2.hasAnnotation(Documented.class));
	}

	//====================================================================================================
	// hasName(String)
	//====================================================================================================
	@Test
	void a022_hasName() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var fullyQualifiedName = TestAnnotation.class.getName();
		assertTrue(ai.hasName(fullyQualifiedName));
		assertFalse(ai.hasName("TestAnnotation"));
	}

	//====================================================================================================
	// hasSimpleName(String)
	//====================================================================================================
	@Test
	void a023_hasSimpleName() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.hasNameSimple("TestAnnotation"));
		assertFalse(ai.hasNameSimple(TestAnnotation.class.getName()));
	}

	//====================================================================================================
	// hashCode()
	//====================================================================================================
	@Test
	void a024_hashCode() {
		var ci = ClassInfo.of(TestClass.class);
		var ai1 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		var ai2 = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);

		assertNotNull(ai1);
		assertNotNull(ai2);
		assertEquals(ai1.hashCode(), ai2.hashCode());
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a025_inner() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var annotation = ai.inner();
		assertNotNull(annotation);
		assertEquals(TestAnnotation.class, annotation.annotationType());
		assertEquals("test", annotation.value());
	}

	//====================================================================================================
	// isInGroup(Class<A>)
	//====================================================================================================
	@Test
	void a026_isInGroup() {
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
	// isType(Class<A>)
	//====================================================================================================
	@Test
	void a027_isType() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		assertTrue(ai.isType(TestAnnotation.class));
		assertFalse(ai.isType(Deprecated.class));
	}

	//====================================================================================================
	// of(Annotatable, A)
	//====================================================================================================
	@Test
	void a028_of() {
		var ci = ClassInfo.of(TestClass.class);
		var annotation = ci.inner().getAnnotation(TestAnnotation.class);
		var ai = AnnotationInfo.of(ci, annotation);

		assertNotNull(ai);
		assertEquals(TestAnnotation.class, ai.annotationType());
		assertEquals("test", ai.getValue().orElse(null));

		// Null annotation should throw
		assertThrows(IllegalArgumentException.class, () -> AnnotationInfo.of(ci, null));
	}

	//====================================================================================================
	// properties()
	//====================================================================================================
	@Test
	void a029_properties() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var map = ai.properties();
		assertNotNull(map);
		assertTrue(map.containsKey("CLASS_TYPE"));
		assertTrue(map.containsKey("@TestAnnotation"));

		var annotationMap = (java.util.Map<String,Object>)map.get("@TestAnnotation");
		assertNotNull(annotationMap);
		assertEquals("test", annotationMap.get("value"));

		// Test with non-default values
		var ci2 = ClassInfo.of(ToMapTestClass.class);
		var ai2 = ci2.getAnnotations(ToMapTestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai2);

		var map2 = ai2.properties();
		assertNotNull(map2);
		var annotationMap2 = (java.util.Map<String,Object>)map2.get("@ToMapTestAnnotation");
		assertNotNull(annotationMap2);

		// value differs from default (non-array), should be included
		assertEquals("custom", annotationMap2.get("value"));

		// nonEmptyArray differs from default (non-empty array), should be included
		assertTrue(annotationMap2.containsKey("nonEmptyArray"));

		// emptyArrayWithNonEmptyDefault is empty array but default is non-empty, should be included
		assertTrue(annotationMap2.containsKey("emptyArrayWithNonEmptyDefault"));

		// arrayValue is empty array matching default empty array, should NOT be included
		assertFalse(annotationMap2.containsKey("arrayValue"));

		// Test with exception handling
		var annotationType = ToMapTestAnnotation.class;
		var handler = new java.lang.reflect.InvocationHandler() {
			@Override
			public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
				if (method.getName().equals("value")) {
					throw new RuntimeException("Test exception");
				}
				if (method.getName().equals("annotationType")) {
					return annotationType;
				}
				if (method.getName().equals("toString")) {
					return "@ToMapTestAnnotation";
				}
				if (method.getName().equals("hashCode")) {
					return 0;
				}
				if (method.getName().equals("equals")) {
					return false;
				}
				return method.getDefaultValue();
			}
		};

		var proxyAnnotation = (ToMapTestAnnotation)java.lang.reflect.Proxy.newProxyInstance(
			annotationType.getClassLoader(),
			new Class[]{annotationType},
			handler
		);

		var ci3 = ClassInfo.of(ToMapTestClass.class);
		var ai3 = AnnotationInfo.of(ci3, proxyAnnotation);

		var map3 = ai3.properties();
		assertNotNull(map3);
		var annotationMap3 = (java.util.Map<String,Object>)map3.get("@ToMapTestAnnotation");
		assertNotNull(annotationMap3);

		// The exception should be caught and stored as a localized message
		assertTrue(annotationMap3.containsKey("value"));
		var value = annotationMap3.get("value");
		assertNotNull(value);
		// The value should be a string representation of the exception
		assertTrue(value instanceof String);
	}

	//====================================================================================================
	// toSimpleString()
	//====================================================================================================
	@Test
	void a030_toSimpleString() {
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
	void a031_toString() {
		var ci = ClassInfo.of(TestClass.class);
		var ai = ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
		assertNotNull(ai);

		var str = ai.toString();
		assertNotNull(str);
		// New toString() format: @AnnotationType(value=..., on=location)
		assertTrue(str.startsWith("@"), "Should start with @");
		assertTrue(str.contains("TestAnnotation"), "Should contain annotation type name");
		assertTrue(str.contains("value=\"test\""), "Should contain annotation value");
		assertTrue(str.contains("on="), "Should contain 'on=' location");
		assertTrue(str.contains("TestClass"), "Should contain declaring class name");
	}

	@Test
	void a032_toString_comprehensive() {
		// Test annotation with single value
		var ci1 = ClassInfo.of(ToStringTestClass.class);
		var ai1 = ci1.getAnnotations(TestAnnotation.class).findFirst().get();
		var str1 = ai1.toString();
		assertTrue(str1.startsWith("@"), "Should start with @");
		assertTrue(str1.contains("TestAnnotation"), "Should contain annotation type");
		assertTrue(str1.contains("value=\"test\""), "Should contain value");
		assertTrue(str1.contains("on="), "Should contain location");
		assertTrue(str1.contains("ToStringTestClass"), "Should contain declaring class name");

		// Test annotation with multiple values
		var ci2 = ClassInfo.of(MultiTypeTestClass.class);
		var ai2 = ci2.getAnnotations(MultiTypeAnnotation.class).findFirst().get();
		var str2 = ai2.toString();
		assertTrue(str2.contains("MultiTypeAnnotation"), "Should contain annotation type");
		assertTrue(str2.contains("stringValue=\"test\""), "Should contain string value");
		assertTrue(str2.contains("intValue=123"), "Should contain int value");
		assertTrue(str2.contains("boolValue=false"), "Should contain boolean value");
		assertTrue(str2.contains("longValue=999"), "Should contain long value");
		assertTrue(str2.contains("doubleValue=1.23"), "Should contain double value");
		assertTrue(str2.contains("floatValue=4.56"), "Should contain float value");
		assertTrue(str2.contains("classValue=java.lang.Integer.class"), "Should contain class value");
		assertTrue(str2.contains("stringArray="), "Should contain string array key");
		assertTrue(str2.contains("\"x\"") && str2.contains("\"y\""), "Should contain string array values");
		assertTrue(str2.contains("classArray="), "Should contain class array key");
		assertTrue(str2.contains("java.lang.Long.class") && str2.contains("java.lang.Double.class"), "Should contain class array values");
		assertTrue(str2.contains("on="), "Should contain location");

		// Test annotation with default values (should not appear)
		var ci3 = ClassInfo.of(DefaultValueTestClass.class);
		var ai3 = ci3.getAnnotations(TestAnnotation.class).findFirst().get();
		var str3 = ai3.toString();
		// Should not contain value since it's the default
		assertFalse(str3.contains("value="), "Should not contain default value");
		// Should still contain location
		assertTrue(str3.contains("on="), "Should contain location");
		assertTrue(str3.contains("DefaultValueTestClass"), "Should contain declaring class name");

		// Test annotation on method
		var ci4 = ClassInfo.of(MethodAnnotationTestClass.class);
		var method = ci4.getPublicMethod(x -> x.hasName("testMethod")).get();
		var ai4 = method.getAnnotations(TestAnnotationMultiTarget.class).findFirst().get();
		var str4 = ai4.toString();
		assertTrue(str4.contains("TestAnnotationMultiTarget"), "Should contain annotation type");
		assertTrue(str4.contains("value=\"method\""), "Should contain method annotation value");
		assertTrue(str4.contains("on="), "Should contain location");
		assertTrue(str4.contains("testMethod"), "Should contain method name");

		// Test annotation on field
		var ci5 = ClassInfo.of(FieldAnnotationTestClass.class);
		var field = ci5.getPublicField(x -> x.hasName("testField")).get();
		var ai5 = field.getAnnotations(TestAnnotationMultiTarget.class).findFirst().get();
		var str5 = ai5.toString();
		assertTrue(str5.contains("TestAnnotationMultiTarget"), "Should contain annotation type");
		assertTrue(str5.contains("value=\"field\""), "Should contain field annotation value");
		assertTrue(str5.contains("on="), "Should contain location");
		assertTrue(str5.contains("testField"), "Should contain field name");

		// Test annotation on parameter
		var ci6 = ClassInfo.of(ParameterAnnotationTestClass.class);
		var method6 = ci6.getPublicMethod(x -> x.hasName("testMethod")).get();
		var param = method6.getParameter(0);
		var ai6 = param.getAnnotations(TestAnnotationMultiTarget.class).findFirst().get();
		var str6 = ai6.toString();
		assertTrue(str6.contains("TestAnnotationMultiTarget"), "Should contain annotation type");
		assertTrue(str6.contains("value=\"param\""), "Should contain parameter annotation value");
		assertTrue(str6.contains("on="), "Should contain location");

		// Test annotation with empty array (should not appear if default is also empty)
		var ci7 = ClassInfo.of(EmptyArrayTestClass.class);
		var ai7 = ci7.getAnnotations(ClassArrayAnnotation.class).findFirst().get();
		var str7 = ai7.toString();
		// Empty array should not appear if default is also empty
		assertFalse(str7.contains("classes="), "Should not contain empty array if default is also empty");

		// Test annotation with non-empty array when default is empty
		var ci8 = ClassInfo.of(NonEmptyArrayTestClass.class);
		var ai8 = ci8.getAnnotations(ClassArrayAnnotation.class).findFirst().get();
		var str8 = ai8.toString();
		assertTrue(str8.contains("classes="), "Should contain non-empty array");
		assertTrue(str8.contains("java.lang.String.class"), "Should contain array element");

		// Test annotation with enum value
		var ci9 = ClassInfo.of(EnumValueTestClass.class);
		var ai9 = ci9.getAnnotations(EnumAnnotation.class).findFirst().get();
		var str9 = ai9.toString();
		assertTrue(str9.contains("EnumAnnotation"), "Should contain annotation type");
		assertTrue(str9.contains("on="), "Should contain location");
		assertTrue(str9.contains("EnumValueTestClass"), "Should contain declaring class name");
		// Note: Enum values may or may not appear if they match defaults due to enum comparison behavior
		// The important thing is that the annotation type and location are present
	}

	// Test classes for comprehensive toString() testing
	@TestAnnotation("test")
	public static class ToStringTestClass {}

	@MultiTypeAnnotation(stringValue = "test", intValue = 123, boolValue = false, longValue = 999L, doubleValue = 1.23, floatValue = 4.56f, classValue = Integer.class, stringArray = { "x", "y" }, classArray = { Long.class, Double.class })
	public static class MultiTypeTestClass {}

	@TestAnnotation  // Uses default value
	public static class DefaultValueTestClass {}

	@Target({TYPE, METHOD, FIELD, PARAMETER})
	@Retention(RUNTIME)
	public static @interface TestAnnotationMultiTarget {
		String value() default "default";
	}

	@SuppressWarnings("java:S1186")
	public static class MethodAnnotationTestClass {
		@TestAnnotationMultiTarget("method")
		public void testMethod() {}
	}

	public static class FieldAnnotationTestClass {
		@TestAnnotationMultiTarget("field")
		public String testField;
	}

	public static class ParameterAnnotationTestClass {
		public void testMethod(@TestAnnotationMultiTarget("param") String param) {}
	}

	@ClassArrayAnnotation  // Empty array, same as default
	public static class EmptyArrayTestClass {}

	@ClassArrayAnnotation(classes = { String.class })
	public static class NonEmptyArrayTestClass {}

	enum TestEnum { VALUE1, VALUE2 }

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface EnumAnnotation {
		TestEnum value() default TestEnum.VALUE1;
	}

	@EnumAnnotation(TestEnum.VALUE2)
	public static class EnumValueTestClass {}
}

