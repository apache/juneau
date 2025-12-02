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
package org.apache.juneau.commons.annotation;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

class AppliedOnClassAnnotationObject_Test extends TestBase {

	private static final String CNAME = AppliedOnClassAnnotationObject_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Test annotation for testing purposes
	//------------------------------------------------------------------------------------------------------------------

	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TA {
		String[] on() default {};

		Class<?>[] onClass() default {};

		String value() default "";
	}

	/**
	 * Implementation of TA using AppliedOnClassAnnotationObject
	 */
	public static class T extends AppliedOnClassAnnotationObject implements TA {

		private final String value;

		public static class Builder extends AppliedAnnotationObject.BuilderT {
			String value = "";

			public Builder() {
				super(TA.class);
			}

			public Builder value(String value) {
				this.value = value;
				return this;
			}

			@Override
			public Builder on(String...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(Class<?>...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(ClassInfo...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder onClass(Class<?>...value) {
				super.onClass(value);
				return this;
			}

			@Override
			public Builder onClass(ClassInfo...value) {
				super.onClass(value);
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
		}

		@Override
		public String value() {
			return value;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test classes for targeting
	//------------------------------------------------------------------------------------------------------------------

	public static class TC1 {}

	public static class TC2 {}

	public static class TC3 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_basic_noOnClass() {
		var a = T.create().build();
		assertList(a.onClass());
	}

	@Test
	void a02_basic_singleClass() {
		var a = T.create().onClass(TC1.class).build();

		assertList(a.onClass(), TC1.class);
	}

	@Test
	void a03_basic_multipleClasses() {
		var a = T.create().onClass(TC1.class, TC2.class).build();

		assertList(a.onClass(), TC1.class, TC2.class);
	}

	@Test
	void a04_basic_withValue() {
		var a = T.create().onClass(TC1.class).value("test").build();

		assertList(a.onClass(), TC1.class);
		assertEquals("test", a.value());
	}

	@Test
	void a05_basic_annotationType() {
		var a = T.create().build();
		assertEquals(TA.class, a.annotationType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// on() vs onClass() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void b01_onVsOnClass_onClassOnly() {
		var a = T.create().onClass(TC1.class, TC2.class).build();

		// onClass() returns the Class objects
		assertList(a.onClass(), TC1.class, TC2.class);

		// on() should be empty when only onClass() is used
		assertList(a.on());
	}

	@Test
	void b02_onVsOnClass_onOnly() {
		var a = T.create().on("com.example.Class1", "com.example.Class2").build();

		// on() returns the string targets
		assertList(a.on(), "com.example.Class1", "com.example.Class2");

		// onClass() should be empty when only on() is used
		assertList(a.onClass());
	}

	@Test
	void b03_onVsOnClass_both() {
		var a = T.create().on("com.example.Class1").onClass(TC1.class).build();

		// Both should be independent
		assertList(a.on(), "com.example.Class1");
		assertList(a.onClass(), TC1.class);
	}

	@Test
	void b04_onVsOnClass_onWithClassConversion() {
		var a = T.create().on(TC1.class, TC2.class).build();

		// on(Class...) converts to strings
		assertList(a.on(), CNAME + "$TC1", CNAME + "$TC2");

		// onClass() should be empty since we used on(Class...) not onClass(Class...)
		assertList(a.onClass());
	}

	//------------------------------------------------------------------------------------------------------------------
	// ClassInfo tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void c01_classInfo_onClassInfo() {
		var ci1 = ClassInfo.of(TC1.class);
		var ci2 = ClassInfo.of(TC2.class);

		var a = T.create().onClass(ci1, ci2).build();

		assertList(a.onClass(), TC1.class, TC2.class);
	}

	@Test
	void c02_classInfo_onClassInfo_mixed() {
		var ci = ClassInfo.of(TC1.class);

		var a = T.create().onClass(ci).onClass(TC2.class).build();

		assertList(a.onClass(), TC1.class, TC2.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Equality and hashcode tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void d01_equality_sameOnClass() {
		var a1 = T.create().onClass(TC1.class, TC2.class).value("test").build();

		var a2 = T.create().onClass(TC1.class, TC2.class).value("test").build();

		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test
	void d02_equality_differentOnClass() {
		var a1 = T.create().onClass(TC1.class).build();

		var a2 = T.create().onClass(TC2.class).build();

		assertNotEquals(a1, a2);
	}

	@Test
	void d03_equality_differentOrder() {
		var a1 = T.create().onClass(TC1.class, TC2.class).build();

		var a2 = T.create().onClass(TC2.class, TC1.class).build();

		assertNotEquals(a1, a2);
	}

	@Test
	void d04_equality_withDeclaredAnnotation() {
		@TA(onClass = { TC1.class, TC2.class }, value = "test")
		class TestClass {}

		var declared = TestClass.class.getAnnotation(TA.class);
		var programmatic = T.create().onClass(TC1.class, TC2.class).value("test").build();

		assertEquals(declared, programmatic);
		assertEquals(declared.hashCode(), programmatic.hashCode());
	}

	@Test
	void d05_hashCode_consistency() {
		var a = T.create().onClass(TC1.class).value("test").build();

		var hash1 = a.hashCode();
		var hash2 = a.hashCode();
		assertEquals(hash1, hash2, "hashCode should be consistent");
	}

	//------------------------------------------------------------------------------------------------------------------
	// toMap() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void e01_toMap_withOnClass() {
		var a = T.create().onClass(TC1.class, TC2.class).value("test").build();
		var map = ((T)a).toMap();

		assertBean(map, "value,on,onClass", "test,[],[TC1,TC2]");
	}

	@Test
	void e02_toMap_withBoth() {
		var a = T.create().on("com.example.Class1").onClass(TC1.class).value("test").build();
		var map = ((T)a).toMap();

		assertBean(map, "value,on,onClass", "test,[com.example.Class1],[TC1]");
	}

	@Test
	void e03_toMap_empty() {
		var a = T.create().build();
		var map = ((T)a).toMap();

		assertBean(map, "value,on,onClass", ",[],[]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString() tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void f01_toString_notNull() {
		var a = T.create().build();
		var str = a.toString();
		assertNotNull(str);
		assertFalse(str.isEmpty());
	}

	@Test
	void f02_toString_containsValues() {
		var a = T.create().onClass(TC1.class).value("test").build();

		assertContainsAll(a.toString(), "test", "onClass");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder pattern tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void g01_builder_fluentApi() {
		var a = T.create().onClass(TC1.class).value("test").onClass(TC2.class).build();

		assertList(a.onClass(), TC1.class, TC2.class);
		assertEquals("test", a.value());
	}

	@Test
	void g02_builder_multipleBuilds() {
		var builder = T.create().onClass(TC1.class).value("test");

		var a1 = builder.build();
		var a2 = builder.build();

		// Different instances but equal
		assertNotSame(a1, a2);
		assertEquals(a1, a2);
	}

	@Test
	void g03_builder_chaining() {
		var a = T.create().onClass(TC1.class).on("com.example.Class1").value("test").onClass(TC2.class).build();

		assertList(a.onClass(), TC1.class, TC2.class);
		assertList(a.on(), "com.example.Class1");
		assertEquals("test", a.value());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void h01_edgeCase_emptyOnClass() {
		var a = T.create().onClass(new Class<?>[0]).build();

		assertList(a.onClass());
	}

	@Test
	void h02_edgeCase_arrayEquality() {
		var a1 = T.create().onClass(TC1.class, TC2.class, TC3.class).build();

		var a2 = T.create().onClass(new Class<?>[] { TC1.class, TC2.class, TC3.class }).build();

		assertEquals(a1, a2);
	}

	@Test
	void h03_edgeCase_equality_differentType() {
		var a = T.create().build();
		var other = new Object();

		assertNotEquals(a, other);
	}

	@Test
	void h04_edgeCase_equality_null() {
		var a = T.create().build();
		assertNotEquals(a, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Null validation tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void i01_null_classInArray() {
		assertThrows(IllegalArgumentException.class, () -> T.create().onClass((Class<?>)null).build());
	}

	@Test
	void i02_null_classInfoInArray() {
		assertThrows(IllegalArgumentException.class, () -> T.create().onClass((ClassInfo)null).build());
	}

	@Test
	void i03_null_builder() {
		assertThrows(IllegalArgumentException.class, () -> new T(null));
	}
}
