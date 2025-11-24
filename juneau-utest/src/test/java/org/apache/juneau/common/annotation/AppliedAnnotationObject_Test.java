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
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.common.reflect.*;
import org.junit.jupiter.api.*;

class AppliedAnnotationObject_Test extends TestBase {

	private static final String CNAME = AppliedAnnotationObject_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Test annotation for testing purposes
	//------------------------------------------------------------------------------------------------------------------

	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestAppliedAnnotation {
		String[] on() default {};
		String value() default "";
		int number() default 0;
	}

	/**
	 * Implementation of TestAppliedAnnotation using AppliedAnnotationObject with basic Builder
	 */
	public static class TestAppliedAnnotationObject extends AppliedAnnotationObject implements TestAppliedAnnotation {

		private final String value;
		private final int number;

		public static class Builder extends AppliedAnnotationObject.Builder {
			String value = "";
			int number = 0;

			public Builder() {
				super(TestAppliedAnnotation.class);
			}

			public Builder value(String value) {
				this.value = value;
				return this;
			}

			public Builder number(int number) {
				this.number = number;
				return this;
			}

			@Override
			public Builder on(String...value) {
				super.on(value);
				return this;
			}

			public TestAppliedAnnotation build() {
				return new TestAppliedAnnotationObject(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TestAppliedAnnotationObject(Builder b) {
			super(b);
			this.value = b.value;
			this.number = b.number;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public int number() {
			return number;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test classes for targeting
	//------------------------------------------------------------------------------------------------------------------

	public static class TargetClass1 {
		public String field1;
		public int field2;

		public TargetClass1() {}
		public TargetClass1(String s) {}

		public void method1() {}
		public String method2(int x) { return null; }
	}

	public static class TargetClass2 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - on() with strings
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_basic_noTargets() {
		var a = TestAppliedAnnotationObject.create().build();
		assertArrayEquals(new String[0], a.on());
	}

	@Test
	void a02_basic_singleTarget() {
		var a = TestAppliedAnnotationObject.create()
			.on("com.example.MyClass")
			.build();

		assertArrayEquals(new String[]{"com.example.MyClass"}, a.on());
	}

	@Test
	void a03_basic_multipleTargets() {
		var a = TestAppliedAnnotationObject.create()
			.on("com.example.Class1")
			.on("com.example.Class2")
			.build();

		assertArrayEquals(new String[]{"com.example.Class1", "com.example.Class2"}, a.on());
	}

	@Test
	void a04_basic_varargsTargets() {
		var a = TestAppliedAnnotationObject.create()
			.on("target1", "target2", "target3")
			.build();

		assertArrayEquals(new String[]{"target1", "target2", "target3"}, a.on());
	}

	@Test
	void a05_basic_withOtherProperties() {
		var a = TestAppliedAnnotationObject.create()
			.on("com.example.MyClass")
			.value("test")
			.number(42)
			.build();

		assertArrayEquals(new String[]{"com.example.MyClass"}, a.on());
		assertEquals("test", a.value());
		assertEquals(42, a.number());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Equality and hashcode tests with on property
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void b01_equality_sameTargets() {
		var a1 = TestAppliedAnnotationObject.create()
			.on("target1", "target2")
			.value("test")
			.build();

		var a2 = TestAppliedAnnotationObject.create()
			.on("target1", "target2")
			.value("test")
			.build();

		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test
	void b02_equality_differentTargets() {
		var a1 = TestAppliedAnnotationObject.create()
			.on("target1")
			.build();

		var a2 = TestAppliedAnnotationObject.create()
			.on("target2")
			.build();

		assertNotEquals(a1, a2);
	}

	@Test
	void b03_equality_differentTargetOrder() {
		// Arrays with different order should not be equal
		var a1 = TestAppliedAnnotationObject.create()
			.on("target1", "target2")
			.build();

		var a2 = TestAppliedAnnotationObject.create()
			.on("target2", "target1")
			.build();

		assertNotEquals(a1, a2);
	}

	@Test
	void b04_equality_noTargetsVsWithTargets() {
		var a1 = TestAppliedAnnotationObject.create().build();
		var a2 = TestAppliedAnnotationObject.create()
			.on("target1")
			.build();

		assertNotEquals(a1, a2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BuilderT tests - Class targeting
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Implementation with BuilderT for class targeting
	 */
	public static class TestAppliedAnnotationObjectT extends AppliedAnnotationObject implements TestAppliedAnnotation {

		private final String value;

		public static class Builder extends AppliedAnnotationObject.BuilderT {
			String value = "";

			public Builder() {
				super(TestAppliedAnnotation.class);
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

			public TestAppliedAnnotation build() {
				return new TestAppliedAnnotationObjectT(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TestAppliedAnnotationObjectT(Builder b) {
			super(b);
			this.value = b.value;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public int number() {
			return 0;
		}
	}

	@Test
	void c01_builderT_onClassArray() {
		var a = TestAppliedAnnotationObjectT.create()
			.on(TargetClass1.class, TargetClass2.class)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1",
			CNAME + "$TargetClass2"
		};
		assertArrayEquals(expected, a.on());
	}

	@Test
	void c02_builderT_onClassInfo() {
		var ci1 = ClassInfo.of(TargetClass1.class);
		var ci2 = ClassInfo.of(TargetClass2.class);

		var a = TestAppliedAnnotationObjectT.create()
			.on(ci1, ci2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1",
			CNAME + "$TargetClass2"
		};
		assertArrayEquals(expected, a.on());
	}

	@Test
	void c03_builderT_mixedTargeting() {
		var a = TestAppliedAnnotationObjectT.create()
			.on("com.example.StringTarget")
			.on(TargetClass1.class)
			.build();

		String[] expected = {
			"com.example.StringTarget",
			CNAME + "$TargetClass1"
		};
		assertArrayEquals(expected, a.on());
	}

	//------------------------------------------------------------------------------------------------------------------
	// BuilderM tests - Method targeting
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Implementation with BuilderM for method targeting
	 */
	public static class TestAppliedAnnotationObjectM extends AppliedAnnotationObject implements TestAppliedAnnotation {

		public static class Builder extends AppliedAnnotationObject.BuilderM {

			public Builder() {
				super(TestAppliedAnnotation.class);
			}

			@Override
			public Builder on(String...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(Method...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(MethodInfo...value) {
				super.on(value);
				return this;
			}

			public TestAppliedAnnotation build() {
				return new TestAppliedAnnotationObjectM(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TestAppliedAnnotationObjectM(Builder b) {
			super(b);
		}

		@Override
		public String value() {
			return "";
		}

		@Override
		public int number() {
			return 0;
		}
	}

	@Test
	void d01_builderM_onMethod() throws Exception {
		Method m1 = TargetClass1.class.getMethod("method1");
		Method m2 = TargetClass1.class.getMethod("method2", int.class);

		var a = TestAppliedAnnotationObjectM.create()
			.on(m1, m2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1.method1()",
			CNAME + "$TargetClass1.method2(int)"
		};
		assertArrayEquals(expected, a.on());
	}

	@Test
	void d02_builderM_onMethodInfo() throws Exception {
		var mi1 = MethodInfo.of(TargetClass1.class.getMethod("method1"));
		var mi2 = MethodInfo.of(TargetClass1.class.getMethod("method2", int.class));

		var a = TestAppliedAnnotationObjectM.create()
			.on(mi1, mi2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1.method1()",
			CNAME + "$TargetClass1.method2(int)"
		};
		assertArrayEquals(expected, a.on());
	}

	//------------------------------------------------------------------------------------------------------------------
	// BuilderC tests - Constructor targeting
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Implementation with BuilderC for constructor targeting
	 */
	public static class TestAppliedAnnotationObjectC extends AppliedAnnotationObject implements TestAppliedAnnotation {

		public static class Builder extends AppliedAnnotationObject.BuilderC {

			public Builder() {
				super(TestAppliedAnnotation.class);
			}

			@Override
			public Builder on(String...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(Constructor<?>...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(ConstructorInfo...value) {
				super.on(value);
				return this;
			}

			public TestAppliedAnnotation build() {
				return new TestAppliedAnnotationObjectC(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TestAppliedAnnotationObjectC(Builder b) {
			super(b);
		}

		@Override
		public String value() {
			return "";
		}

		@Override
		public int number() {
			return 0;
		}
	}

	@Test
	void e01_builderC_onConstructor() throws Exception {
		Constructor<?> c1 = TargetClass1.class.getConstructor();
		Constructor<?> c2 = TargetClass1.class.getConstructor(String.class);

		var a = TestAppliedAnnotationObjectC.create()
			.on(c1, c2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1()",
			CNAME + "$TargetClass1(java.lang.String)"
		};
		assertArrayEquals(expected, a.on());
	}

	@Test
	void e02_builderC_onConstructorInfo() throws Exception {
		var ci1 = ConstructorInfo.of(TargetClass1.class.getConstructor());
		var ci2 = ConstructorInfo.of(TargetClass1.class.getConstructor(String.class));

		var a = TestAppliedAnnotationObjectC.create()
			.on(ci1, ci2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1()",
			CNAME + "$TargetClass1(java.lang.String)"
		};
		assertArrayEquals(expected, a.on());
	}

	//------------------------------------------------------------------------------------------------------------------
	// BuilderMF tests - Method and Field targeting
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Implementation with BuilderMF for method and field targeting
	 */
	public static class TestAppliedAnnotationObjectMF extends AppliedAnnotationObject implements TestAppliedAnnotation {

		public static class Builder extends AppliedAnnotationObject.BuilderMF {

			public Builder() {
				super(TestAppliedAnnotation.class);
			}

			@Override
			public Builder on(String...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(Method...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(MethodInfo...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(Field...value) {
				super.on(value);
				return this;
			}

			@Override
			public Builder on(FieldInfo...value) {
				super.on(value);
				return this;
			}

			public TestAppliedAnnotation build() {
				return new TestAppliedAnnotationObjectMF(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TestAppliedAnnotationObjectMF(Builder b) {
			super(b);
		}

		@Override
		public String value() {
			return "";
		}

		@Override
		public int number() {
			return 0;
		}
	}

	@Test
	void f01_builderMF_onField() throws Exception {
		Field f1 = TargetClass1.class.getField("field1");
		Field f2 = TargetClass1.class.getField("field2");

		var a = TestAppliedAnnotationObjectMF.create()
			.on(f1, f2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1.field1",
			CNAME + "$TargetClass1.field2"
		};
		assertArrayEquals(expected, a.on());
	}

	@Test
	void f02_builderMF_onFieldInfo() throws Exception {
		var fi1 = FieldInfo.of(ClassInfo.of(TargetClass1.class), TargetClass1.class.getField("field1"));
		var fi2 = FieldInfo.of(ClassInfo.of(TargetClass1.class), TargetClass1.class.getField("field2"));

		var a = TestAppliedAnnotationObjectMF.create()
			.on(fi1, fi2)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1.field1",
			CNAME + "$TargetClass1.field2"
		};
		assertArrayEquals(expected, a.on());
	}

	@Test
	void f03_builderMF_mixedMethodsAndFields() throws Exception {
		Method m = TargetClass1.class.getMethod("method1");
		Field f = TargetClass1.class.getField("field1");

		var a = TestAppliedAnnotationObjectMF.create()
			.on(m)
			.on(f)
			.build();

		String[] expected = {
			CNAME + "$TargetClass1.method1()",
			CNAME + "$TargetClass1.field1"
		};
		assertArrayEquals(expected, a.on());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent API tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void g01_fluentApi_chaining() {
		var a = TestAppliedAnnotationObject.create()
			.on("target1")
			.value("test")
			.on("target2")
			.number(42)
			.on("target3")
			.build();

		assertArrayEquals(new String[]{"target1", "target2", "target3"}, a.on());
		assertEquals("test", a.value());
		assertEquals(42, a.number());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toMap() tests with on property
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void h01_toMap_withTargets() {
		var a = TestAppliedAnnotationObject.create()
			.on("target1", "target2")
			.value("test")
			.build();

		var map = ((TestAppliedAnnotationObject)a).toMap();
		assertArrayEquals(new String[]{"target1", "target2"}, (String[])map.get("on"));
		assertEquals("test", map.get("value"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void i01_edgeCase_emptyTargets() {
		var a = TestAppliedAnnotationObject.create()
			.on()
			.build();

		assertArrayEquals(new String[0], a.on());
	}

	@Test
	void i02_edgeCase_builderReuse() {
		var builder = TestAppliedAnnotationObject.create()
			.on("target1")
			.value("test");

		var a1 = builder.build();
		var a2 = builder.build();

		// Different instances but equal
		assertNotSame(a1, a2);
		assertEquals(a1, a2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Null validation tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void j01_nullClass_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectT.create()
				.on((Class<?>)null)
				.build()
		);
	}

	@Test
	void j02_nullClassInfo_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectT.create()
				.on((ClassInfo)null)
				.build()
		);
	}

	@Test
	void j03_nullMethod_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectM.create()
				.on((java.lang.reflect.Method)null)
				.build()
		);
	}

	@Test
	void j04_nullMethodInfo_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectM.create()
				.on((MethodInfo)null)
				.build()
		);
	}

	@Test
	void j05_nullField_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectMF.create()
				.on((java.lang.reflect.Field)null)
				.build()
		);
	}

	@Test
	void j06_nullFieldInfo_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectMF.create()
				.on((FieldInfo)null)
				.build()
		);
	}

	@Test
	void j07_nullConstructor_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectC.create()
				.on((java.lang.reflect.Constructor<?>)null)
				.build()
		);
	}

	@Test
	void j08_nullConstructorInfo_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			TestAppliedAnnotationObjectC.create()
				.on((ConstructorInfo)null)
				.build()
		);
	}
}

