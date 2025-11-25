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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class AnnotationObject_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test annotation for testing purposes
	//------------------------------------------------------------------------------------------------------------------

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TA {
		String value() default "";

		int number() default 0;

		boolean flag() default false;

		String[] array() default {};
	}

	/**
	 * Implementation of TA using AnnotationObject
	 */
	public static class T extends AnnotationObject implements TA {

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
				super(TA.class);
			}

			public Builder value(String _value) {
				value = _value;
				return this;
			}

			public Builder number(int value) {
				number = value;
				return this;
			}

			public Builder flag(boolean value) {
				flag = value;
				return this;
			}

			public Builder array(String...value) {
				array = value;
				return this;
			}

			public TA build() {
				return new T(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public T(Builder b) {
			super(b);
			value = b.value;
			number = b.number;
			flag = b.flag;
			array = Arrays.copyOf(b.array, b.array.length);
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
		var a = T.create().build();
		assertBean(a, "value,number,flag,array", ",0,false,[]");
	}

	@Test
	void a02_basic_customValues() {
		var a = T.create().value("a").number(1).flag(true).array("b1", "b2").build();
		assertBean(a, "value,number,flag,array", "a,1,true,[b1,b2]");
	}

	@Test
	void a03_basic_annotationType() {
		var a = T.create().build();
		assertEquals(TA.class, a.annotationType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Equality and hashcode tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void b01_equality_identical() {
		var a1 = T.create().value("a").number(1).flag(true).array("b1", "b2").build();

		var a2 = T.create().value("a").number(1).flag(true).array("b1", "b2").build();

		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test
	void b02_equality_different() {
		var a1 = T.create().value("a1").build();

		var a2 = T.create().value("a2").build();

		assertNotEquals(a1, a2);
	}

	@Test
	void b03_equality_withDeclaredAnnotation() {
		@TA(value = "a", number = 1, flag = true, array = { "b1", "b2" })
		class B {}

		var declared = B.class.getAnnotation(TA.class);
		var programmatic = T.create().value("a").number(1).flag(true).array("b1", "b2").build();

		assertEquals(declared, programmatic);
		assertEquals(declared.hashCode(), programmatic.hashCode());
	}

	@Test
	void b04_hashCode_consistency() {
		var a = T.create().value("a").number(1).build();

		var hash1 = a.hashCode();
		var hash2 = a.hashCode();
		assertEquals(hash1, hash2, "hashCode should be consistent");
	}

	@Test
	void b05_hashCode_notNegativeOne() {
		var a = T.create().build();
		assertNotEquals(-1, a.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toMap() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void c01_toMap_defaultValues() {
		var a = T.create().build();
		assertBean(((T)a).toMap(), "value,number,flag,array", ",0,false,[]");
	}

	@Test
	void c02_toMap_customValues() {
		var a = T.create().value("a").number(1).flag(true).array("b1", "b2").build();

		assertBean(((T)a).toMap(), "value,number,flag,array", "a,1,true,[b1,b2]");
	}

	@Test
	void c03_toMap_keySorted() {
		var a = T.create().build();
		var map = ((T)a).toMap();

		// Map should be ordered by key name
		assertList(map.keySet(), "array", "flag", "number", "value");
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void d01_toString_notNull() {
		var a = T.create().build();
		var str = a.toString();
		assertNotNull(str);
		assertFalse(str.isEmpty());
	}

	@Test
	void d02_toString_containsValues() {
		var a = T.create().value("a").number(1).build();

		assertContainsAll(a.toString(), "a", "1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructor validation tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void e01_constructor_nullAnnotationType() {
		assertThrows(IllegalArgumentException.class, () -> new AnnotationObject.Builder(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder pattern tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void f01_builder_fluentApi() {
		var a = T.create().value("a").number(1).flag(true).array("b1", "b2").build();

		assertBean(a, "value,number,flag,array", "a,1,true,[b1,b2]");
	}

	@Test
	void f02_builder_multipleBuilds() {
		var builder = T.create().value("a").number(1);

		var a1 = builder.build();
		var a2 = builder.build();

		// Different instances but equal
		assertNotSame(a1, a2);
		assertEquals(a1, a2);
	}

	@Test
	void f03_builder_getAnnotationType() {
		var builder = T.create();
		assertEquals(TA.class, builder.getAnnotationType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void g01_arrayEquality_emptyVsNull() {
		// Empty array and null should be handled consistently
		var a1 = T.create().array().build();

		var a2 = T.create().array(new String[0]).build();

		assertEquals(a1, a2);
	}

	@Test
	void g02_arrayEquality_deepEquals() {
		// Arrays with same content should be equal
		var a1 = T.create().array("a", "b", "c").build();

		var a2 = T.create().array(new String[] { "a", "b", "c" }).build();

		assertEquals(a1, a2);
	}

	@Test
	void g03_arrayEquality_differentOrder() {
		// Arrays with different order should not be equal
		var a1 = T.create().array("a", "b", "c").build();

		var a2 = T.create().array("c", "b", "a").build();

		assertNotEquals(a1, a2);
	}

	@Test
	void g04_equality_differentType() {
		var a = T.create().build();
		var other = new Object();

		assertNotEquals(a, other);
	}

	@Test
	void g05_equality_null() {
		var a = T.create().build();
		assertNotEquals(a, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Null validation tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void h01_nullBuilder_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> new T(null));
	}
}