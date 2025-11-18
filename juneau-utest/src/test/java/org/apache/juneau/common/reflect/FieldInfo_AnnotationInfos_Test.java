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
package org.apache.juneau.common.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link FieldInfo#getDeclaredAnnotations()} methods.
 */
public class FieldInfo_AnnotationInfos_Test {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface TestAnnotation1 {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface TestAnnotation2 {
		int value() default 0;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface TestAnnotation3 {
		String value() default "";
	}

	public static class TestClass {
		@TestAnnotation1("test1")
		@TestAnnotation2(42)
		public String field1;

		@TestAnnotation1("test2")
		public String field2;

		public String field3;
	}

	@Test
	public void testGetAnnotationInfos() {
		var ci = ClassInfo.of(TestClass.class);
		var field1 = ci.getPublicField(x -> x.getName().equals("field1")).get();
		var field2 = ci.getPublicField(x -> x.getName().equals("field2")).get();
		var field3 = ci.getPublicField(x -> x.getName().equals("field3")).get();

		// field1 has 2 annotations
		var annotations1 = field1.getDeclaredAnnotations();
		assertEquals(2, annotations1.size());
		assertTrue(annotations1.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation1")));
		assertTrue(annotations1.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation2")));

		// field2 has 1 annotation
		var annotations2 = field2.getDeclaredAnnotations();
		assertEquals(1, annotations2.size());
		assertTrue(annotations2.stream().anyMatch(a -> a.hasSimpleName("TestAnnotation1")));

		// field3 has no annotations
		var annotations3 = field3.getDeclaredAnnotations();
		assertEquals(0, annotations3.size());
	}

	@Test
	public void testGetAnnotationInfosTyped() {
		var ci = ClassInfo.of(TestClass.class);
		var field1 = ci.getPublicField(x -> x.getName().equals("field1")).get();
		var field2 = ci.getPublicField(x -> x.getName().equals("field2")).get();

		// Test filtering by type for field1
		var ann1_type1 = field1.getDeclaredAnnotations(TestAnnotation1.class).toList();
		assertEquals(1, ann1_type1.size());
		assertEquals("test1", ann1_type1.get(0).getValue().get());

		var ann1_type2 = field1.getDeclaredAnnotations(TestAnnotation2.class).toList();
		assertEquals(1, ann1_type2.size());
		assertEquals(42, ann1_type2.get(0).getInt("value").get());

		// Test filtering by type that doesn't exist
		var ann1_type3 = field1.getDeclaredAnnotations(TestAnnotation3.class).toList();
		assertEquals(0, ann1_type3.size());

		// Test filtering for field2
		var ann2_type1 = field2.getDeclaredAnnotations(TestAnnotation1.class).toList();
		assertEquals(1, ann2_type1.size());
		assertEquals("test2", ann2_type1.get(0).getValue().get());

		var ann2_type2 = field2.getDeclaredAnnotations(TestAnnotation2.class).toList();
		assertEquals(0, ann2_type2.size());
	}

	@Test
	public void testGetAnnotationInfosMemoization() {
		var ci = ClassInfo.of(TestClass.class);
		var field1 = ci.getPublicField(x -> x.getName().equals("field1")).get();

		// Calling getDeclaredAnnotationInfos() multiple times should return the same list instance
		var annotations1 = field1.getDeclaredAnnotations();
		var annotations2 = field1.getDeclaredAnnotations();
		assertSame(annotations1, annotations2);
	}
}

