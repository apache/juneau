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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;

import org.junit.jupiter.api.*;

/**
 * Tests for AnnotationInfo value retrieval methods.
 */
public class AnnotationInfo_ValueMethods_Test {

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestAnnotation {
		String stringValue() default "default";
		int intValue() default 42;
		boolean boolValue() default true;
		long longValue() default 100L;
		double doubleValue() default 3.14;
		float floatValue() default 2.5f;
		Class<?> classValue() default String.class;
		String[] stringArray() default {"a", "b"};
		Class<?>[] classArray() default {String.class, Integer.class};
	}

	@TestAnnotation(
		stringValue = "test",
		intValue = 123,
		boolValue = false,
		longValue = 999L,
		doubleValue = 1.23,
		floatValue = 4.56f,
		classValue = Integer.class,
		stringArray = {"x", "y", "z"},
		classArray = {Long.class, Double.class}
	)
	public static class AnnotatedClass {}

	private AnnotationInfo<TestAnnotation> getTestAnnotationInfo() {
		var ci = ClassInfo.of(AnnotatedClass.class);
		return ci.getAnnotations(TestAnnotation.class).findFirst().orElse(null);
	}

	@Test
	public void testHasName() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.hasName("org.apache.juneau.common.reflect.AnnotationInfo_ValueMethods_Test$TestAnnotation"));
		assertFalse(ai.hasName("TestAnnotation"));
	}

	@Test
	public void testHasSimpleName() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.hasSimpleName("TestAnnotation"));
		assertFalse(ai.hasSimpleName("org.apache.juneau.common.reflect.AnnotationInfo_ValueMethods_Test$TestAnnotation"));
	}

	@Test
	public void testGetString() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getString("stringValue").isPresent());
		assertEquals("test", ai.getString("stringValue").get());
		assertTrue(ai.getString("nonexistent").isEmpty());
	}

	@Test
	public void testGetInt() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getInt("intValue").isPresent());
		assertEquals(123, ai.getInt("intValue").get());
		assertTrue(ai.getInt("nonexistent").isEmpty());
	}

	@Test
	public void testGetBoolean() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getBoolean("boolValue").isPresent());
		assertEquals(false, ai.getBoolean("boolValue").get());
		assertTrue(ai.getBoolean("nonexistent").isEmpty());
	}

	@Test
	public void testGetLong() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getLong("longValue").isPresent());
		assertEquals(999L, ai.getLong("longValue").get());
		assertTrue(ai.getLong("nonexistent").isEmpty());
	}

	@Test
	public void testGetDouble() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getDouble("doubleValue").isPresent());
		assertEquals(1.23, ai.getDouble("doubleValue").get(), 0.001);
		assertTrue(ai.getDouble("nonexistent").isEmpty());
	}

	@Test
	public void testGetFloat() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getFloat("floatValue").isPresent());
		assertEquals(4.56f, ai.getFloat("floatValue").get(), 0.001);
		assertTrue(ai.getFloat("nonexistent").isEmpty());
	}

	@Test
	public void testGetClassValue() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getClassValue("classValue").isPresent());
		assertEquals(Integer.class, ai.getClassValue("classValue").get());
		assertTrue(ai.getClassValue("nonexistent").isEmpty());
	}

	@Test
	public void testGetStringArray() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getStringArray("stringArray").isPresent());
		String[] array = ai.getStringArray("stringArray").get();
		assertNotNull(array);
		assertEquals(3, array.length);
		assertEquals("x", array[0]);
		assertEquals("y", array[1]);
		assertEquals("z", array[2]);
		assertTrue(ai.getStringArray("nonexistent").isEmpty());
	}

	@Test
	public void testGetClassArray() {
		var ai = getTestAnnotationInfo();
		
		assertTrue(ai.getClassArray("classArray").isPresent());
		Class<?>[] array = ai.getClassArray("classArray").get();
		assertNotNull(array);
		assertEquals(2, array.length);
		assertEquals(Long.class, array[0]);
		assertEquals(Double.class, array[1]);
		assertTrue(ai.getClassArray("nonexistent").isEmpty());
	}

	@Test
	public void testGetReturnType() {
		var ai = getTestAnnotationInfo();
		
		// Test various return types
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
		
		// Nonexistent method
		assertTrue(ai.getReturnType("nonexistent").isEmpty());
	}
}

