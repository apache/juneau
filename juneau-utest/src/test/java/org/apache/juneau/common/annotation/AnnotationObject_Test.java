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
package org.apache.juneau.common.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class AnnotationObject_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test annotation for testing purposes
	//------------------------------------------------------------------------------------------------------------------

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestAnnotation {
		String value() default "";
		int number() default 0;
		boolean flag() default false;
		String[] array() default {};
	}

	/**
	 * Implementation of TestAnnotation using AnnotationObject
	 */
	public static class TestAnnotationObject extends AnnotationObject implements TestAnnotation {

		private final String value;
		private final int number;
		private final boolean flag;
		private final String[] array;

		public static class Builder extends AnnotationObject.Builder {
			String value = "";
			int number = 0;
			boolean flag = false;
			String[] array = {};

			public Builder() {
				super(TestAnnotation.class);
			}

			public Builder value(String value) {
				this.value = value;
				return this;
			}

			public Builder number(int number) {
				this.number = number;
				return this;
			}

			public Builder flag(boolean flag) {
				this.flag = flag;
				return this;
			}

			public Builder array(String...array) {
				this.array = array;
				return this;
			}

			public TestAnnotation build() {
				return new TestAnnotationObject(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TestAnnotationObject(Builder b) {
			super(b);
			this.value = b.value;
			this.number = b.number;
			this.flag = b.flag;
			this.array = Arrays.copyOf(b.array, b.array.length);
			postConstruct();
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public int number() {
			return number;
		}

		@Override
		public boolean flag() {
			return flag;
		}

		@Override
		public String[] array() {
			return array;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_basic_defaultValues() {
		TestAnnotation a = TestAnnotationObject.create().build();
		assertEquals("", a.value());
		assertEquals(0, a.number());
		assertEquals(false, a.flag());
		assertArrayEquals(new String[0], a.array());
	}

	@Test
	void a02_basic_customValues() {
		TestAnnotation a = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.flag(true)
			.array("a", "b", "c")
			.build();

		assertEquals("test", a.value());
		assertEquals(42, a.number());
		assertEquals(true, a.flag());
		assertArrayEquals(new String[]{"a", "b", "c"}, a.array());
	}

	@Test
	void a03_basic_annotationType() {
		TestAnnotation a = TestAnnotationObject.create().build();
		assertEquals(TestAnnotation.class, a.annotationType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Equality and hashcode tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void b01_equality_identical() {
		TestAnnotation a1 = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.flag(true)
			.array("a", "b")
			.build();

		TestAnnotation a2 = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.flag(true)
			.array("a", "b")
			.build();

		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test
	void b02_equality_different() {
		TestAnnotation a1 = TestAnnotationObject.create()
			.value("test1")
			.build();

		TestAnnotation a2 = TestAnnotationObject.create()
			.value("test2")
			.build();

		assertNotEquals(a1, a2);
	}

	@Test
	void b03_equality_withDeclaredAnnotation() {
		@TestAnnotation(value="test", number=42, flag=true, array={"a", "b"})
		class TestClass {}

		TestAnnotation declared = TestClass.class.getAnnotation(TestAnnotation.class);
		TestAnnotation programmatic = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.flag(true)
			.array("a", "b")
			.build();

		assertEquals(declared, programmatic);
		assertEquals(declared.hashCode(), programmatic.hashCode());
	}

	@Test
	void b04_hashCode_consistency() {
		TestAnnotation a = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.build();

		int hash1 = a.hashCode();
		int hash2 = a.hashCode();
		assertEquals(hash1, hash2, "hashCode should be consistent");
	}

	@Test
	void b05_hashCode_notNegativeOne() {
		TestAnnotation a = TestAnnotationObject.create().build();
		assertNotEquals(-1, a.hashCode(), "hashCode should not be -1 after postConstruct");
	}

	//------------------------------------------------------------------------------------------------------------------
	// toMap() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void c01_toMap_defaultValues() {
		TestAnnotation a = TestAnnotationObject.create().build();
		Map<String, Object> map = ((TestAnnotationObject)a).toMap();

		assertNotNull(map);
		assertEquals("", map.get("value"));
		assertEquals(0, map.get("number"));
		assertEquals(false, map.get("flag"));
		assertArrayEquals(new String[0], (String[])map.get("array"));
	}

	@Test
	void c02_toMap_customValues() {
		TestAnnotation a = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.flag(true)
			.array("a", "b")
			.build();
		Map<String, Object> map = ((TestAnnotationObject)a).toMap();

		assertEquals("test", map.get("value"));
		assertEquals(42, map.get("number"));
		assertEquals(true, map.get("flag"));
		assertArrayEquals(new String[]{"a", "b"}, (String[])map.get("array"));
	}

	@Test
	void c03_toMap_keySorted() {
		TestAnnotation a = TestAnnotationObject.create().build();
		Map<String, Object> map = ((TestAnnotationObject)a).toMap();

		// Map should be ordered by key name
		List<String> keys = new ArrayList<>(map.keySet());
		assertEquals("array", keys.get(0));
		assertEquals("flag", keys.get(1));
		assertEquals("number", keys.get(2));
		assertEquals("value", keys.get(3));
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void d01_toString_notNull() {
		TestAnnotation a = TestAnnotationObject.create().build();
		String str = a.toString();
		assertNotNull(str);
		assertFalse(str.isEmpty());
	}

	@Test
	void d02_toString_containsValues() {
		TestAnnotation a = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.build();
		String str = a.toString();

		assertTrue(str.contains("test"));
		assertTrue(str.contains("42"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// postConstruct() requirement tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void e01_postConstruct_required() {
		// Test that hashCode() throws exception if postConstruct() was not called
		AnnotationObject obj = new AnnotationObject(new AnnotationObject.Builder(TestAnnotation.class)) {
			// Intentionally not calling postConstruct()
		};

		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			obj.hashCode();
		});

		assertTrue(e.getMessage().contains("postConstruct"));
	}

	@Test
	void e02_postConstruct_called() {
		// Test that hashCode() works correctly when postConstruct() is called
		AnnotationObject obj = new AnnotationObject(new AnnotationObject.Builder(TestAnnotation.class)) {
			{
				postConstruct();
			}
		};

		assertNotEquals(-1, obj.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder pattern tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void f01_builder_fluentApi() {
		TestAnnotation a = TestAnnotationObject.create()
			.value("test")
			.number(42)
			.flag(true)
			.array("a", "b")
			.build();

		assertNotNull(a);
		assertEquals("test", a.value());
	}

	@Test
	void f02_builder_multipleBuilds() {
		TestAnnotationObject.Builder builder = TestAnnotationObject.create()
			.value("test")
			.number(42);

		TestAnnotation a1 = builder.build();
		TestAnnotation a2 = builder.build();

		// Different instances but equal
		assertNotSame(a1, a2);
		assertEquals(a1, a2);
	}

	@Test
	void f03_builder_getAnnotationType() {
		TestAnnotationObject.Builder builder = TestAnnotationObject.create();
		assertEquals(TestAnnotation.class, builder.getAnnotationType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void g01_arrayEquality_emptyVsNull() {
		// Empty array and null should be handled consistently
		TestAnnotation a1 = TestAnnotationObject.create()
			.array()
			.build();

		TestAnnotation a2 = TestAnnotationObject.create()
			.array(new String[0])
			.build();

		assertEquals(a1, a2);
	}

	@Test
	void g02_arrayEquality_deepEquals() {
		// Arrays with same content should be equal
		TestAnnotation a1 = TestAnnotationObject.create()
			.array("a", "b", "c")
			.build();

		TestAnnotation a2 = TestAnnotationObject.create()
			.array(new String[]{"a", "b", "c"})
			.build();

		assertEquals(a1, a2);
	}

	@Test
	void g03_arrayEquality_differentOrder() {
		// Arrays with different order should not be equal
		TestAnnotation a1 = TestAnnotationObject.create()
			.array("a", "b", "c")
			.build();

		TestAnnotation a2 = TestAnnotationObject.create()
			.array("c", "b", "a")
			.build();

		assertNotEquals(a1, a2);
	}

	@Test
	void g04_equality_differentType() {
		TestAnnotation a = TestAnnotationObject.create().build();
		Object other = new Object();

		assertNotEquals(a, other);
	}

	@Test
	void g05_equality_null() {
		TestAnnotation a = TestAnnotationObject.create().build();
		assertNotEquals(a, null);
	}
}

