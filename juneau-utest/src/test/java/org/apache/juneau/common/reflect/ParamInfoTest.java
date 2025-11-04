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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

import org.apache.juneau.annotation.Name;

/**
 * ParamInfo tests.
 */
class ParamInfoTest extends TestBase {

	private static String originalDisableParamNameDetection;

	@BeforeAll
	public static void beforeAll() {
		// Save original system property value
		originalDisableParamNameDetection = System.getProperty("juneau.disableParamNameDetection");
		
		// Set to true to ensure consistent behavior regardless of JVM compiler settings
		System.setProperty("juneau.disableParamNameDetection", "true");
		ParameterInfo.reset();
	}

	@AfterAll
	public static void afterAll() {
		// Restore original system property value
		if (originalDisableParamNameDetection == null)
			System.clearProperty("juneau.disableParamNameDetection");
		else
			System.setProperty("juneau.disableParamNameDetection", originalDisableParamNameDetection);
		ParameterInfo.reset();
	}

	@Documented
	@Target(METHOD)
	@Retention(RUNTIME)
	@Inherited
	public static @interface A {
		String value();
	}

	@Documented
	@Target(METHOD)
	@Retention(RUNTIME)
	@Inherited
	public static @interface AX {
		String value();
	}

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof List)
				return ((List<?>)t).stream().map(this).collect(Collectors.joining(","));
			if (isArray(t))
				return StreamSupport.stream(toList(t, Object.class).spliterator(), false).map(this).collect(Collectors.joining(","));
			if (t instanceof MethodInfo)
				return ((MethodInfo)t).getDeclaringClass().getNameSimple() + '.' + ((MethodInfo)t).getShortName();
			if (t instanceof CA)
				return "@CA(" + ((CA)t).value() + ")";
			if (t instanceof DA)
				return "@DA(" + ((DA)t).value() + ")";
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getNameSimple();
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	static class B {
		public B(int a, String b) {}
		public void a1(int a, String b) {}  // NOSONAR
		void a2(int a, String b) {}  // NOSONAR
	}

	static ClassInfo b = ClassInfo.of(B.class);
	static ParameterInfo
		b_b_a = b.getPublicConstructor(x -> x.hasParameterTypes(int.class, String.class)).getParameter(0),  // NOSONAR
		b_b_b = b.getPublicConstructor(x -> x.hasParameterTypes(int.class, String.class)).getParameter(1),  // NOSONAR
		b_a1_a = b.getMethod(x -> x.hasName("a1")).getParameter(0),  // NOSONAR
		b_a1_b = b.getMethod(x -> x.hasName("a1")).getParameter(1),  // NOSONAR
		b_a2_a = b.getMethod(x -> x.hasName("a2")).getParameter(0),  // NOSONAR
		b_a2_b = b.getMethod(x -> x.hasName("a2")).getParameter(1);  // NOSONAR

	@Test void getIndex() {
		assertEquals(0, b_b_a.getIndex());
		assertEquals(1, b_b_b.getIndex());
		assertEquals(0, b_a1_a.getIndex());
		assertEquals(1, b_a1_b.getIndex());
		assertEquals(0, b_a2_a.getIndex());
		assertEquals(1, b_a2_b.getIndex());
	}

	@Test void getMethod() {
		check("B.a1(int,String)", b_a1_a.getMethod());
		check("B.a1(int,String)", b_a1_b.getMethod());
		check("B.a2(int,String)", b_a2_a.getMethod());
		check("B.a2(int,String)", b_a2_b.getMethod());
	}

	@Test void getMethod_onConstrutor() {
		check(null, b_b_a.getMethod());
		check(null, b_b_b.getMethod());
	}

	@Test void getConstructor() {
		check("B(int,String)", b_b_a.getConstructor());
		check("B(int,String)", b_b_b.getConstructor());
	}

	@Test void getConstructor_onMethod() {
		check(null, b_a1_a.getConstructor());
		check(null, b_a1_b.getConstructor());
		check(null, b_a2_a.getConstructor());
		check(null, b_a2_b.getConstructor());
	}

	@Test void getParameterType() {
		check("int", b_b_a.getParameterType());
		check("String", b_b_b.getParameterType());
		check("int", b_a1_a.getParameterType());
		check("String", b_a1_b.getParameterType());
		check("int", b_a2_a.getParameterType());
		check("String", b_a2_b.getParameterType());

	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations.
	//-----------------------------------------------------------------------------------------------------------------

	@Target({PARAMETER,TYPE})
	@Retention(RUNTIME)
	public static @interface CA {
		public String value();
	}
	@CA("1") public static class C1 extends C2 {}
	@CA("2") public static class C2 implements C3, C4 {}
	@CA("3") public interface C3 {}
	@CA("4") public interface C4 {}

	public interface CB {
		void a1(@CA("5") C1 x);
		void a2(@CA("5") C1 x);
	}
	public static class CC implements CB {
		public CC(@CA("9") C1 x) {}
		@Override
		public void a1(C1 x) {}  // NOSONAR
		@Override
		public void a2(@CA("6") C1 x) {}  // NOSONAR
	}
	static ClassInfo
		cb = ClassInfo.of(CB.class),
		cc = ClassInfo.of(CC.class);
	static ParameterInfo
		cc_cc = cc.getPublicConstructor(x -> x.hasParameterTypes(C1.class)).getParameter(0),  // NOSONAR
		cb_a1 = cb.getMethod(x -> x.hasName("a1")).getParameter(0),  // NOSONAR
		cb_a2 = cb.getMethod(x -> x.hasName("a2")).getParameter(0),  // NOSONAR
		cc_a1 = cc.getMethod(x -> x.hasName("a1")).getParameter(0),  // NOSONAR
		cc_a2 = cc.getMethod(x -> x.hasName("a2")).getParameter(0);  // NOSONAR

	@Test void getDeclaredAnnotations() {
		check("@CA(5)", declaredAnnotations(cb_a1, CA.class));
		check("@CA(5)", declaredAnnotations(cb_a2, CA.class));
		check("", declaredAnnotations(cc_a1, CA.class));
		check("@CA(6)", declaredAnnotations(cc_a2, CA.class));
	}

	@Test void getDeclaredAnnotations_constructor() {
		check("@CA(9)", declaredAnnotations(cc_cc, CA.class));
	}

	private static <T extends Annotation> List<T> declaredAnnotations(ParameterInfo pi, Class<T> type) {
		return pi.getAnnotations(type).map(x -> x.inner()).toList();
	}

	@Test void getDeclaredAnnotation() {
		check("@CA(5)", cb_a1.getDeclaredAnnotation(CA.class));
		check("@CA(5)", cb_a2.getDeclaredAnnotation(CA.class));
		check(null, cc_a1.getDeclaredAnnotation(CA.class));
		check("@CA(6)", cc_a2.getDeclaredAnnotation(CA.class));
	}

	@Test void getDeclaredAnnotation_constructor() {
		check("@CA(9)", cc_cc.getDeclaredAnnotation(CA.class));
	}

	@Test void getDeclaredAnnotation_notFound() {
		check(null, cb_a1.getDeclaredAnnotation(DA.class));
	}

	@Test void getDeclaredAnnotation_notFound_constructor() {
		check(null, cc_cc.getDeclaredAnnotation(DA.class));
	}

	@Test void getDeclaredAnnotation_null() {
		check(null, cb_a1.getDeclaredAnnotation(null));
	}

	@Test void getDeclaredAnnotation_null_constructor() {
		check(null, cc_cc.getDeclaredAnnotation(null));
	}

	@Test void getAnnotationsParentFirst() {
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5)", annotations(cb_a1, CA.class));
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5)", annotations(cb_a2, CA.class));
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5)", annotations(cc_a1, CA.class));
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5),@CA(6)", annotations(cc_a2, CA.class));
	}

	@Test void getAnnotationsParentFirst_notFound() {
		check("", annotations(cb_a1, DA.class));
	}

	@Test void getAnnotationsParentFirst_constructor() {
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(9)", annotations(cc_cc, CA.class));
	}

	@Test void getAnnotationsParentFirst_notFound_constructor() {
		check("", annotations(cc_cc, DA.class));
	}

	@Test void findAnnotationInfo() {
		check("@CA(5)", cb_a1.findAnnotationInfo(CA.class).inner());
		check("@CA(5)", cb_a2.findAnnotationInfo(CA.class).inner());
		check("@CA(5)", cc_a1.findAnnotationInfo(CA.class).inner());
		check("@CA(6)", cc_a2.findAnnotationInfo(CA.class).inner());
	}

	@Test void findAnnotationInfo_notFound() {
		var ai = cb_a1.findAnnotationInfo(DA.class);
		check(null, ai == null ? null : ai.inner());
	}

	@Test void findAnnotationInfo_constructor() {
		check("@CA(9)", cc_cc.findAnnotationInfo(CA.class).inner());
	}

	@Test void findAnnotationInfo_notFound_constructor() {
		var ai = cc_cc.findAnnotationInfo(DA.class);
		check(null, ai == null ? null : ai.inner());
	}

	@Test void findAnnotationInfo_twice() {
		check("@CA(5)", cb_a1.findAnnotationInfo(CA.class).inner());
		check("@CA(5)", cb_a1.findAnnotationInfo(CA.class).inner());
	}

	@Test void findAnnotationInfo_twice_constructor() {
		check("@CA(9)", cc_cc.findAnnotationInfo(CA.class).inner());
		check("@CA(9)", cc_cc.findAnnotationInfo(CA.class).inner());
	}

	@Test void hasAnnotation() {
		assertTrue(cb_a1.hasAnnotation(CA.class));
		assertTrue(cb_a2.hasAnnotation(CA.class));
		assertTrue(cc_a1.hasAnnotation(CA.class));
		assertTrue(cc_a2.hasAnnotation(CA.class));
		assertFalse(cb_a1.hasAnnotation(DA.class));
	}

	@Test void hasAnnotation_constructor() {
		assertTrue(cc_cc.hasAnnotation(CA.class));
		assertFalse(cc_cc.hasAnnotation(DA.class));
	}

	@Target({PARAMETER,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface DA {
		public String value();
	}
	@DA("1") public static class D1 extends D2 {}
	@DA("2") public static class D2 implements D3, D4 {}
	@DA("3") public interface D3 {}
	@DA("4") public interface D4 {}

	public interface DB {
		void a1(@DA("0") D1 x);
	}
	public static class DC implements DB {
		@Override
		public void a1(@DA("5") D1 x) {}  // NOSONAR
	}

	static ClassInfo
		db = ClassInfo.of(DB.class),
		dc = ClassInfo.of(DC.class);
	static ParameterInfo
		db_a1 = db.getMethod(x -> x.hasName("a1")).getParameter(0),  // NOSONAR
		dc_a1 = dc.getMethod(x -> x.hasName("a1")).getParameter(0);  // NOSONAR

	@Test void getAnnotationsParentFirst_inherited() {
		check("@DA(4),@DA(3),@DA(2),@DA(1),@DA(0)", annotations(db_a1, DA.class));
		check("@DA(4),@DA(3),@DA(2),@DA(1),@DA(0),@DA(5)", annotations(dc_a1, DA.class));
	}

	@Test void getAnnotationsParentFirst_inherited_notFound() {
		check("", annotations(db_a1, CA.class));
	}

	@Test void findAnnotationInfo_inherited() {
		check("@DA(0)", db_a1.findAnnotationInfo(DA.class).inner());
		check("@DA(5)", dc_a1.findAnnotationInfo(DA.class).inner());
	}

	@Test void findAnnotationInfo_inherited_notFound() {
		var ai = db_a1.findAnnotationInfo(CA.class);
		check(null, ai == null ? null : ai.inner());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	static class E {
		public void a1(int a, @Name("b") int b) {}  // NOSONAR
	}

	static ClassInfo e = ClassInfo.of(E.class);
	static ParameterInfo
		e_a1_a = e.getMethod(x -> x.hasName("a1")).getParameter(0),  // NOSONAR
		e_a1_b = e.getMethod(x -> x.hasName("a1")).getParameter(1);  // NOSONAR

	@Test void hasName() {
		// With DISABLE_PARAM_NAME_DETECTION=true, only parameters with @Name annotation have names
		assertFalse(e_a1_a.hasName());  // No @Name annotation
		assertTrue(e_a1_b.hasName());   // Has @Name("b")
	}

	@Test void getName() {
		// With DISABLE_PARAM_NAME_DETECTION=true:
		// - Parameters with @Name use the annotation value
		// - Parameters without @Name fall back to parameter.getName() which may return
		//   bytecode names (if compiled with -parameters) or synthetic names (arg0, arg1, etc.)
		assertNotNull(e_a1_a.getName());  // No @Name, falls back to parameter.getName()
		assertEquals("b", e_a1_b.getName());  // Has @Name("b")
	}

	@Test void toString2() {
		assertEquals("a1[1]", e_a1_b.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getMatchingParameters()
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	class GetMatchingParametersTests {

		// Method hierarchy tests
		public interface PM1 {
			void foo(String s);
		}

		public static class PM2 implements PM1 {
			@Override public void foo(String s) {}  // NOSONAR
		}

		public static class PM3 extends PM2 {
			@Override public void foo(String s) {}  // NOSONAR
		}

		@Test void method_simpleHierarchy() throws Exception {
			var mi = MethodInfo.of(PM3.class.getMethod("foo", String.class));
			var pi = mi.getParameter(0);
			var matching = pi.getMatchingParameters();
			assertEquals(3, matching.size());
			check("PM3", matching.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PM2", matching.get(1).getDeclaringExecutable().getDeclaringClass());
			check("PM1", matching.get(2).getDeclaringExecutable().getDeclaringClass());
		}

		// Method with multiple interfaces
		public interface PM4 {
			void bar(int x, String s);
		}

		public interface PM5 {
			void bar(int x, String s);
		}

		public static class PM6 implements PM4, PM5 {
			@Override public void bar(int x, String s) {}  // NOSONAR
		}

		@Test void method_multipleInterfaces() throws Exception {
			var mi = MethodInfo.of(PM6.class.getMethod("bar", int.class, String.class));
			var pi0 = mi.getParameter(0);
			var matching0 = pi0.getMatchingParameters();
			assertEquals(3, matching0.size());
			check("PM6", matching0.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PM4", matching0.get(1).getDeclaringExecutable().getDeclaringClass());
			check("PM5", matching0.get(2).getDeclaringExecutable().getDeclaringClass());

			var pi1 = mi.getParameter(1);
			var matching1 = pi1.getMatchingParameters();
			assertEquals(3, matching1.size());
			check("PM6", matching1.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PM4", matching1.get(1).getDeclaringExecutable().getDeclaringClass());
			check("PM5", matching1.get(2).getDeclaringExecutable().getDeclaringClass());
		}

		// Constructor hierarchy with same parameter count
		public static class PC1 {
			public PC1(String foo) {}  // NOSONAR
		}

		public static class PC2 extends PC1 {
			public PC2(String foo) { super(foo); }  // NOSONAR
		}

		public static class PC3 extends PC2 {
			public PC3(String foo) { super(foo); }  // NOSONAR
		}

		@Test void constructor_simpleHierarchy() throws Exception {
			var ci = ConstructorInfo.of(PC3.class.getConstructor(String.class));
			var pi = ci.getParameter(0);
			var matching = pi.getMatchingParameters();
			assertEquals(3, matching.size());
			check("PC3", matching.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PC2", matching.get(1).getDeclaringExecutable().getDeclaringClass());
			check("PC1", matching.get(2).getDeclaringExecutable().getDeclaringClass());
		}

		// Constructor hierarchy with different parameter counts
		public static class PC4 {
			public PC4(String foo, int bar) {}  // NOSONAR
		}

		public static class PC5 extends PC4 {
			public PC5(String foo) { super(foo, 0); }  // NOSONAR
		}

		@Test void constructor_differentParameterCounts() throws Exception {
			var ci = ConstructorInfo.of(PC5.class.getConstructor(String.class));
			var pi = ci.getParameter(0);
			var matching = pi.getMatchingParameters();
			// Should find matching "foo" parameter in PC4 even though PC4 has more parameters
			assertEquals(2, matching.size());
			check("PC5", matching.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PC4", matching.get(1).getDeclaringExecutable().getDeclaringClass());
		}

		// Constructor with multiple constructors in parent
		public static class PC6 {
			public PC6(String foo) {}  // NOSONAR
			public PC6(String foo, int bar) {}  // NOSONAR
		}

		public static class PC7 extends PC6 {
			public PC7(String foo) { super(foo); }  // NOSONAR
		}

		@Test void constructor_multipleParentConstructors() throws Exception {
			var ci = ConstructorInfo.of(PC7.class.getConstructor(String.class));
			var pi = ci.getParameter(0);
			var matching = pi.getMatchingParameters();
			// Should find "foo" parameter in both PC6 constructors
			assertEquals(3, matching.size());
			check("PC7", matching.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PC6", matching.get(1).getDeclaringExecutable().getDeclaringClass());
			check("PC6", matching.get(2).getDeclaringExecutable().getDeclaringClass());
		}

		// False match tests - different parameter names
		public interface PM7 {
			void baz(String differentName);
		}

		public static class PM8 implements PM7 {
			@Override public void baz(String differentName) {}  // NOSONAR
			public void foo(String s) {}  // NOSONAR
		}

		@Test void method_differentParameterName() throws Exception {
			var mi = MethodInfo.of(PM8.class.getMethod("foo", String.class));
			var pi = mi.getParameter(0);
			var matching = pi.getMatchingParameters();
			// Should only find this parameter
			assertEquals(1, matching.size());
			check("PM8", matching.get(0).getDeclaringExecutable().getDeclaringClass());
		}

		// False match tests - different parameter types
		public interface PM9 {
			void qux(int x);
		}

		public static class PM10 implements PM9 {
			@Override public void qux(int x) {}  // NOSONAR
		}

		public static class PM11 extends PM10 {
			public void qux(String x) {}  // NOSONAR - different overload
		}

		@Test void method_differentParameterType() throws Exception {
			var mi = MethodInfo.of(PM11.class.getMethod("qux", String.class));
			var pi = mi.getParameter(0);
			var matching = pi.getMatchingParameters();
			// Should only find this parameter (different type from parent)
			assertEquals(1, matching.size());
			check("PM11", matching.get(0).getDeclaringExecutable().getDeclaringClass());
		}

		// Constructor false match - different parameter type
		public static class PC8 {
			public PC8(int foo) {}  // NOSONAR
		}

		public static class PC9 extends PC8 {
			public PC9(String foo) { super(0); }  // NOSONAR
		}

		@Test void constructor_differentParameterType() throws Exception {
			var ci = ConstructorInfo.of(PC9.class.getConstructor(String.class));
			var pi = ci.getParameter(0);
			var matching = pi.getMatchingParameters();
			// Should only find this parameter (different type from parent)
			assertEquals(1, matching.size());
			check("PC9", matching.get(0).getDeclaringExecutable().getDeclaringClass());
		}

		// Constructor false match - different parameter name
		public static class PC10 {
			public PC10(String bar) {}  // NOSONAR
		}

		public static class PC11 extends PC10 {
			public PC11(String foo) { super(foo); }  // NOSONAR
		}

		@Test void constructor_differentParameterName() throws Exception {
			var ci = ConstructorInfo.of(PC11.class.getConstructor(String.class));
			var pi = ci.getParameter(0);
			var matching = pi.getMatchingParameters();
			// Note: Parameter names are not retained without -parameters flag,
			// so this will match by type and synthetic name (e.g., "arg0")
			assertEquals(2, matching.size());
			check("PC11", matching.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PC10", matching.get(1).getDeclaringExecutable().getDeclaringClass());
		}

		// Test with @Name annotation
		public static class PC12 {
			public PC12(@Name("foo") String x) {}  // NOSONAR
			public PC12(@Name("bar") String x, int y) {}  // NOSONAR
		}

		public static class PC13 extends PC12 {
			public PC13(@Name("foo") String x) { super(x); }  // NOSONAR
		}

	@Test void constructor_withNameAnnotation_matching() throws Exception {
		var ci = ConstructorInfo.of(PC13.class.getConstructor(String.class));
		var pi = ci.getParameter(0);
		var matching = pi.getMatchingParameters();
		// Constructors match by index+type only (not by name) to avoid circular dependency
		// So this matches ALL PC12 constructors with parameter at index 0 with type String
		// PC12 has TWO such constructors: PC12(String) and PC12(String, int)
		assertEquals(3, matching.size());
		check("PC13", matching.get(0).getDeclaringExecutable().getDeclaringClass());
		check("PC12", matching.get(1).getDeclaringExecutable().getDeclaringClass());
		check("PC12", matching.get(2).getDeclaringExecutable().getDeclaringClass());
		// Both PC12 parameters have their own @Name annotations
		assertEquals("foo", matching.get(1).getName());
		assertEquals("bar", matching.get(2).getName());
	}

		// Test with @Name annotation - different names
		public static class PC14 {
			public PC14(@Name("bar") String x) {}  // NOSONAR
		}

		public static class PC15 extends PC14 {
			public PC15(@Name("foo") String x) { super(x); }  // NOSONAR
		}

	@Test void constructor_withNameAnnotation_differentNames() throws Exception {
		var ci = ConstructorInfo.of(PC15.class.getConstructor(String.class));
		var pi = ci.getParameter(0);
		var matching = pi.getMatchingParameters();
		// Constructors match by index+type only (not by name) to avoid circular dependency
		// So this DOES match parent parameter even though names differ ("foo" vs "bar")
		assertEquals(2, matching.size());
		check("PC15", matching.get(0).getDeclaringExecutable().getDeclaringClass());
		check("PC14", matching.get(1).getDeclaringExecutable().getDeclaringClass());
		assertEquals("foo", matching.get(0).getName());
		assertEquals("bar", matching.get(1).getName());
	}

		// Test with @Name annotation on methods
		public interface PM12 {
			void test(@Name("param1") String x);
		}

		public static class PM13 implements PM12 {
			@Override public void test(@Name("param1") String x) {}  // NOSONAR
		}

		@Test void method_withNameAnnotation_matching() throws Exception {
			var mi = MethodInfo.of(PM13.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var matching = pi.getMatchingParameters();
			assertEquals(2, matching.size());
			check("PM13", matching.get(0).getDeclaringExecutable().getDeclaringClass());
			check("PM12", matching.get(1).getDeclaringExecutable().getDeclaringClass());
			assertEquals("param1", matching.get(0).getName());
			assertEquals("param1", matching.get(1).getName());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// findAnnotationInfos() / findAnnotationInfo()
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	class FindAnnotationInfosTests {

		// Annotations for testing
		@Documented
		@Target({PARAMETER, TYPE})
		@Retention(RUNTIME)
		public @interface FA1 {
			int value();
		}

		@Documented
		@Target({PARAMETER, TYPE})
		@Retention(RUNTIME)
		public @interface FA2 {
			String value();
		}

		// Test finding annotation on parameter itself
		public static class F1 {
			public void test(@FA1(1) String x) {}  // NOSONAR
		}

		@Test void findOnParameter() throws Exception {
			var mi = MethodInfo.of(F1.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(1, infos.size());
			assertEquals(1, infos.get(0).inner().value());
		}

		@Test void findOnParameter_single() throws Exception {
			var mi = MethodInfo.of(F1.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var info = pi.findAnnotationInfo(FA1.class);
			assertNotNull(info);
			assertEquals(1, info.inner().value());
		}

		// Test finding annotation from matching method parameters
		public interface F2 {
			void test(@FA1(2) String x);
		}

		public static class F3 implements F2 {
			@Override public void test(String x) {}  // NOSONAR
		}

		@Test void findFromMatchingMethod() throws Exception {
			var mi = MethodInfo.of(F3.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(1, infos.size());
			assertEquals(2, infos.get(0).inner().value());
		}

		// Test finding annotation from parameter type
		@FA1(3)
		public static class F4Type {}

		public static class F5 {
			public void test(F4Type x) {}  // NOSONAR
		}

		@Test void findFromParameterType() throws Exception {
			var mi = MethodInfo.of(F5.class.getMethod("test", F4Type.class));
			var pi = mi.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(1, infos.size());
			assertEquals(3, infos.get(0).inner().value());
		}

		// Test finding multiple annotations from hierarchy
		public interface F6 {
			void test(@FA1(4) String x);
		}

		public static class F7 {
			public void test(@FA1(5) String x) {}  // NOSONAR
		}

		public static class F8 extends F7 implements F6 {
			@Override public void test(@FA1(6) String x) {}  // NOSONAR
		}

		@Test void findMultipleFromHierarchy() throws Exception {
			var mi = MethodInfo.of(F8.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(3, infos.size());
			assertEquals(6, infos.get(0).inner().value()); // F8
			assertEquals(4, infos.get(1).inner().value()); // F6
			assertEquals(5, infos.get(2).inner().value()); // F7
		}

		@Test void findMultipleFromHierarchy_single() throws Exception {
			var mi = MethodInfo.of(F8.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var info = pi.findAnnotationInfo(FA1.class);
			assertNotNull(info);
			assertEquals(6, info.inner().value()); // Returns first (F8)
		}

		// Test finding annotation from constructor parameters
		public static class F9 {
			public F9(@FA1(7) String x) {}  // NOSONAR
		}

		public static class F10 extends F9 {
			public F10(@FA1(8) String x) { super(x); }  // NOSONAR
		}

		@Test void findFromMatchingConstructor() throws Exception {
			var ci = ConstructorInfo.of(F10.class.getConstructor(String.class));
			var pi = ci.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(2, infos.size());
			assertEquals(8, infos.get(0).inner().value()); // F10
			assertEquals(7, infos.get(1).inner().value()); // F9
		}

		// Test not found
		public static class F11 {
			public void test(String x) {}  // NOSONAR
		}

		@Test void notFound() throws Exception {
			var mi = MethodInfo.of(F11.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(0, infos.size());
		}

		@Test void notFound_single() throws Exception {
			var mi = MethodInfo.of(F11.class.getMethod("test", String.class));
			var pi = mi.getParameter(0);
			var info = pi.findAnnotationInfo(FA1.class);
			assertNull(info);
		}

		// Test parameter annotation takes precedence over type annotation
		@FA1(9)
		public static class F12Type {}

		public static class F13 {
			public void test(@FA1(10) F12Type x) {}  // NOSONAR
		}

		@Test void parameterAnnotationBeforeTypeAnnotation() throws Exception {
			var mi = MethodInfo.of(F13.class.getMethod("test", F12Type.class));
			var pi = mi.getParameter(0);
			var infos = pi.findAnnotationInfos(FA1.class);
			assertEquals(2, infos.size());
			assertEquals(10, infos.get(0).inner().value()); // Parameter annotation first
			assertEquals(9, infos.get(1).inner().value());  // Type annotation second
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static <T extends Annotation> List<T> annotations(ParameterInfo pi, Class<T> a) {
		List<T> l = list();
		pi.forEachAnnotation(a, x -> true, l::add);
		return l;
	}
}