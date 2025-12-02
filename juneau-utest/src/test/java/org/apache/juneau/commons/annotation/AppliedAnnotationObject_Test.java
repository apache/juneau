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
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

class AppliedAnnotationObject_Test extends TestBase {

	private static final String CNAME = AppliedAnnotationObject_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Test annotation for testing purposes
	//------------------------------------------------------------------------------------------------------------------

	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TA {
		String[] on() default {};

		String value() default "";

		int number() default 0;
	}

	/**
	 * Implementation of TA using AppliedAnnotationObject with basic Builder
	 */
	public static class TAO extends AppliedAnnotationObject implements TA {

		private final String value;
		private final int number;

		public static class Builder extends AppliedAnnotationObject.Builder {
			String value = "";
			int number = 0;

			public Builder() {
				super(TA.class);
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

			public TA build() {
				return new TAO(this);
			}
		}

		public static Builder create() {
			return new Builder();
		}

		public TAO(Builder b) {
			super(b);
			value = b.value;
			number = b.number;
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

	public static class TC {
		public String field1;
		public int field2;

		public TC() {}

		public TC(String s) {}

		public void method1() {}

		public String method2(int x) {
			return null;
		}
	}

	public static class TC2 {}

	//------------------------------------------------------------------------------------------------------------------
	// Nested test classes
	//------------------------------------------------------------------------------------------------------------------

	@Nested
	@DisplayName("Basic on() tests with strings")
	class A_BasicOnTests extends TestBase {

		@Test
		void a01_noTargets() {
			var a = TAO.create().build();
			assertList(a.on());
		}

		@Test
		void a02_singleTarget() {
			var a = TAO.create().on("com.example.MyClass").build();

			assertList(a.on(), "com.example.MyClass");
		}

		@Test
		void a03_multipleTargets() {
			var a = TAO.create().on("com.example.Class1").on("com.example.Class2").build();

			assertList(a.on(), "com.example.Class1", "com.example.Class2");
		}

		@Test
		void a04_varargsTargets() {
			var a = TAO.create().on("target1", "target2", "target3").build();

			assertList(a.on(), "target1", "target2", "target3");
		}

		@Test
		void a05_withOtherProperties() {
			var a = TAO.create().on("com.example.MyClass").value("test").number(42).build();

			assertBean(a, "on,value,number", "[com.example.MyClass],test,42");
		}
	}

	@Nested
	@DisplayName("Equality and hashcode tests")
	class B_EqualityAndHashCodeTests extends TestBase {

		@Test
		void b01_sameTargets() {
			var a1 = TAO.create().on("target1", "target2").value("test").build();

			var a2 = TAO.create().on("target1", "target2").value("test").build();

			assertEquals(a1, a2);
			assertEquals(a1.hashCode(), a2.hashCode());
		}

		@Test
		void b02_differentTargets() {
			var a1 = TAO.create().on("target1").build();

			var a2 = TAO.create().on("target2").build();

			assertNotEquals(a1, a2);
		}

		@Test
		void b03_differentTargetOrder() {
			// Arrays with different order should not be equal
			var a1 = TAO.create().on("target1", "target2").build();

			var a2 = TAO.create().on("target2", "target1").build();

			assertNotEquals(a1, a2);
		}

		@Test
		void b04_noTargetsVsWithTargets() {
			var a1 = TAO.create().build();
			var a2 = TAO.create().on("target1").build();

			assertNotEquals(a1, a2);
		}
	}

	@Nested
	@DisplayName("BuilderT - Class targeting tests")
	class C_BuilderTTests extends TestBase {

		/**
		 * Implementation with BuilderT for class targeting
		 */
		public static class C extends AppliedOnClassAnnotationObject implements TA {

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
					return new C(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public C(Builder b) {
				super(b);
				value = b.value;
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
		void c01_onClassArray() {
			var a = C.create().on(TC.class, TC2.class).build();

			assertList(a.on(), CNAME + "$TC", CNAME + "$TC2");
		}

		@Test
		void c02_onClassInfo() {
			var ci1 = ClassInfo.of(TC.class);
			var ci2 = ClassInfo.of(TC2.class);

			var a = C.create().on(ci1, ci2).build();

			assertList(a.on(), CNAME + "$TC", CNAME + "$TC2");
		}

		@Test
		void c03_mixedTargeting() {
			var a = C.create().on("com.example.StringTarget").on(TC.class).build();

			assertList(a.on(), "com.example.StringTarget", CNAME + "$TC");
		}

		@Test
		void c04_onClassClass() {
			var a = (AppliedOnClassAnnotationObject)C.create().onClass(TC.class, TC2.class).build();

			assertList(a.onClass(), TC.class, TC2.class);
		}

		@Test
		void c05_onClassClassInfo() {
			var ci1 = ClassInfo.of(TC.class);
			var ci2 = ClassInfo.of(TC2.class);

			var a = (AppliedOnClassAnnotationObject)C.create().onClass(ci1, ci2).build();

			assertList(a.onClass(), TC.class, TC2.class);
		}
	}

	@Nested
	@DisplayName("BuilderM - Method targeting tests")
	class D_BuilderMTests extends TestBase {

		/**
		 * Implementation with BuilderM for method targeting
		 */
		public static class D extends AppliedAnnotationObject implements TA {

			public static class Builder extends AppliedAnnotationObject.BuilderM {

				public Builder() {
					super(TA.class);
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

				public TA build() {
					return new D(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public D(Builder b) {
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
		void d01_onMethod() throws Exception {
			var m1 = TC.class.getMethod("method1");
			var m2 = TC.class.getMethod("method2", int.class);

			var a = D.create().on(m1, m2).build();

			assertList(a.on(), CNAME + "$TC.method1()", CNAME + "$TC.method2(int)");
		}

		@Test
		void d02_onMethodInfo() throws Exception {
			var mi1 = MethodInfo.of(TC.class.getMethod("method1"));
			var mi2 = MethodInfo.of(TC.class.getMethod("method2", int.class));

			var a = D.create().on(mi1, mi2).build();

			assertList(a.on(), CNAME + "$TC.method1()", CNAME + "$TC.method2(int)");
		}
	}

	@Nested
	@DisplayName("BuilderC - Constructor targeting tests")
	class E_BuilderCTests extends TestBase {

		/**
		 * Implementation with BuilderC for constructor targeting
		 */
		public static class E extends AppliedAnnotationObject implements TA {

			public static class Builder extends AppliedAnnotationObject.BuilderC {

				public Builder() {
					super(TA.class);
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

				public TA build() {
					return new E(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public E(Builder b) {
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
		void e01_onConstructor() throws Exception {
			var c1 = TC.class.getConstructor();
			var c2 = TC.class.getConstructor(String.class);

			var a = E.create().on(c1, c2).build();

			assertList(a.on(), CNAME + "$TC()", CNAME + "$TC(java.lang.String)");
		}

		@Test
		void e02_onConstructorInfo() throws Exception {
			var ci1 = ConstructorInfo.of(TC.class.getConstructor());
			var ci2 = ConstructorInfo.of(TC.class.getConstructor(String.class));

			var a = E.create().on(ci1, ci2).build();

			assertList(a.on(), CNAME + "$TC()", CNAME + "$TC(java.lang.String)");
		}
	}

	@Nested
	@DisplayName("BuilderMF - Method and Field targeting tests")
	class F_BuilderMFTests extends TestBase {

		/**
		 * Implementation with BuilderMF for method and field targeting
		 */
		public static class F extends AppliedAnnotationObject implements TA {

			public static class Builder extends AppliedAnnotationObject.BuilderMF {

				public Builder() {
					super(TA.class);
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

				public TA build() {
					return new F(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public F(Builder b) {
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
		void f01_onField() throws Exception {
			var f1 = TC.class.getField("field1");
			var f2 = TC.class.getField("field2");

			var a = F.create().on(f1, f2).build();

			assertList(a.on(), CNAME + "$TC.field1", CNAME + "$TC.field2");
		}

		@Test
		void f02_onFieldInfo() throws Exception {
			var fi1 = FieldInfo.of(ClassInfo.of(TC.class), TC.class.getField("field1"));
			var fi2 = FieldInfo.of(ClassInfo.of(TC.class), TC.class.getField("field2"));

			var a = F.create().on(fi1, fi2).build();

			assertList(a.on(), CNAME + "$TC.field1", CNAME + "$TC.field2");
		}

		@Test
		void f03_mixedMethodsAndFields() throws Exception {
			var m = TC.class.getMethod("method1");
			var f = TC.class.getField("field1");

			var a = F.create().on(m).on(f).build();

			assertList(a.on(), CNAME + "$TC.method1()", CNAME + "$TC.field1");
		}

		@Test
		void f04_onMethodInfo() throws Exception {
			var mi1 = MethodInfo.of(TC.class.getMethod("method1"));
			var mi2 = MethodInfo.of(TC.class.getMethod("method2", int.class));

			var a = F.create().on(mi1, mi2).build();

			assertList(a.on(), CNAME + "$TC.method1()", CNAME + "$TC.method2(int)");
		}
	}

	@Nested
	@DisplayName("BuilderTM - Class and Method targeting tests")
	class G_BuilderTMTests extends TestBase {

		/**
		 * Implementation with BuilderTM for class and method targeting
		 */
		public static class G extends AppliedOnClassAnnotationObject implements TA {

			public static class Builder extends AppliedAnnotationObject.BuilderTM {

				public Builder() {
					super(TA.class);
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
				public Builder onClass(Class<?>...value) {
					super.onClass(value);
					return this;
				}

				public TA build() {
					return new G(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public G(Builder b) {
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
		void g01_onClassAndMethod() throws Exception {
			var m = TC.class.getMethod("method1");

			var a = G.create().on(TC.class).on(m).build();

			assertList(a.on(), CNAME + "$TC", CNAME + "$TC.method1()");
		}

		@Test
		void g02_onMethodInfo() throws Exception {
			var mi = MethodInfo.of(TC.class.getMethod("method1"));

			var a = G.create().on(mi).build();

			assertList(a.on(), CNAME + "$TC.method1()");
		}

		@Test
		void g03_onClassClass() {
			var a = (AppliedOnClassAnnotationObject) G.create().onClass(TC.class, TC2.class).build();

			assertList(a.onClass(), TC.class, TC2.class);
		}
	}

	@Nested
	@DisplayName("BuilderTMF - Class, Method, and Field targeting tests")
	class H_BuilderTMFTests extends TestBase {

		/**
		 * Implementation with BuilderTMF for class, method, and field targeting
		 */
		public static class H extends AppliedOnClassAnnotationObject implements TA {

			public static class Builder extends AppliedAnnotationObject.BuilderTMF {

				public Builder() {
					super(TA.class);
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

				public TA build() {
					return new H(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public H(Builder b) {
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
		void h01_onClassMethodAndField() throws Exception {
			var m = TC.class.getMethod("method1");
			var f = TC.class.getField("field1");

			var a = H.create().on(TC.class).on(m).on(f).build();

			assertList(a.on(), CNAME + "$TC", CNAME + "$TC.method1()", CNAME + "$TC.field1");
		}

		@Test
		void h02_onMethodInfo() throws Exception {
			var mi = MethodInfo.of(TC.class.getMethod("method1"));

			var a = H.create().on(mi).build();

			assertList(a.on(), CNAME + "$TC.method1()");
		}

		@Test
		void h03_onFieldInfo() throws Exception {
			var fi = FieldInfo.of(ClassInfo.of(TC.class), TC.class.getField("field1"));

			var a = H.create().on(fi).build();

			assertList(a.on(), CNAME + "$TC.field1");
		}
	}

	@Nested
	@DisplayName("BuilderTMFC - Class, Method, Field, and Constructor targeting tests")
	class I_BuilderTMFCTests extends TestBase {

		/**
		 * Implementation with BuilderTMFC for complete targeting
		 */
		public static class I extends AppliedOnClassAnnotationObject implements TA {

			public static class Builder extends AppliedAnnotationObject.BuilderTMFC {

				public Builder() {
					super(TA.class);
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

				public TA build() {
					return new I(this);
				}
			}

			public static Builder create() {
				return new Builder();
			}

			public I(Builder b) {
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
		void i01_onAllTypes() throws Exception {
			var m = TC.class.getMethod("method1");
			var f = TC.class.getField("field1");
			var c = TC.class.getConstructor();

			var a = I.create().on(TC.class).on(m).on(f).on(c).build();

			assertList(a.on(), CNAME + "$TC", CNAME + "$TC.method1()", CNAME + "$TC.field1", CNAME + "$TC()");
		}

		@Test
		void i02_onMethodInfo() throws Exception {
			var mi = MethodInfo.of(TC.class.getMethod("method1"));

			var a = I.create().on(mi).build();

			assertList(a.on(), CNAME + "$TC.method1()");
		}

		@Test
		void i03_onFieldInfo() throws Exception {
			var fi = FieldInfo.of(ClassInfo.of(TC.class), TC.class.getField("field1"));

			var a = I.create().on(fi).build();

			assertList(a.on(), CNAME + "$TC.field1");
		}

		@Test
		void i04_onConstructorInfo() throws Exception {
			var ci = ConstructorInfo.of(TC.class.getConstructor(String.class));

			var a = I.create().on(ci).build();

			assertList(a.on(), CNAME + "$TC(java.lang.String)");
		}
	}

	@Nested
	@DisplayName("Fluent API tests")
	class J_FluentApiTests extends TestBase {

		@Test
		void j01_chaining() {
			var a = TAO.create().on("target1").value("test").on("target2").number(42).on("target3").build();

			assertBean(a, "on,value,number", "[target1,target2,target3],test,42");
		}
	}

	@Nested
	@DisplayName("toMap() tests")
	class K_ToMapTests extends TestBase {

		@Test
		void k01_withTargets() {
			var a = TAO.create().on("target1", "target2").value("test").build();

			var map = ((TAO)a).toMap();
			assertBean(map, "on,value", "[target1,target2],test");
		}
	}

	@Nested
	@DisplayName("Edge case tests")
	class L_EdgeCaseTests extends TestBase {

		@Test
		void l01_emptyTargets() {
			var a = TAO.create().on().build();

			assertList(a.on());
		}

		@Test
		void l02_builderReuse() {
			var builder = TAO.create().on("target1").value("test");

			var a1 = builder.build();
			var a2 = builder.build();

			// Different instances but equal
			assertNotSame(a1, a2);
			assertEquals(a1, a2);
		}
	}

	@Nested
	@DisplayName("Null validation tests")
	class M_NullValidationTests extends TestBase {

		@Test
		void m01_nullClass_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> C_BuilderTTests.C.create().on((Class<?>)null).build());
		}

		@Test
		void m02_nullClassInfo_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> C_BuilderTTests.C.create().on((ClassInfo)null).build());
		}

		@Test
		void m03_nullMethod_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> D_BuilderMTests.D.create().on((java.lang.reflect.Method)null).build());
		}

		@Test
		void m04_nullMethodInfo_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> D_BuilderMTests.D.create().on((MethodInfo)null).build());
		}

		@Test
		void m05_nullField_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> F_BuilderMFTests.F.create().on((java.lang.reflect.Field)null).build());
		}

		@Test
		void m06_nullFieldInfo_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> F_BuilderMFTests.F.create().on((FieldInfo)null).build());
		}

		@Test
		void m07_nullConstructor_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> E_BuilderCTests.E.create().on((java.lang.reflect.Constructor<?>)null).build());
		}

		@Test
		void m08_nullConstructorInfo_throwsException() {
			assertThrows(IllegalArgumentException.class, () -> E_BuilderCTests.E.create().on((ConstructorInfo)null).build());
		}
	}
}
