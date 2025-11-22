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
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

class MethodInfo_Test extends TestBase {

	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface A {
		String value();
	}

	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface AX {
		String value();
	}

	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	@ContextApply(AConfigApply.class)
	public static @interface AConfig {
		String value();
	}

	public static class AConfigApply extends AnnotationApplier<AConfig,Context.Builder> {
		protected AConfigApply(VarResolverSession vr) {
			super(AConfig.class, Context.Builder.class, vr);
		}
		@Override
		public void apply(AnnotationInfo<AConfig> ai, Context.Builder b) {}  // NOSONAR
	}

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof MethodInfo t2)
				return t2.getDeclaringClass().getNameSimple() + '.' + ((MethodInfo)t).getShortName();
			if (t instanceof Method t2)
				return t2.getDeclaringClass().getSimpleName() + '.' + MethodInfo.of((Method)t).getShortName();
			if (t instanceof List<?> t2)
				return (t2.stream().map(this).collect(Collectors.joining(",")));
			if (t instanceof AnnotationInfo t2)
				return apply(t2.inner());
			if (t instanceof A t2)
				return "@A(" + t2.value() + ")";
			if (t instanceof PA t2)
				return "@PA(" + t2.value() + ")";
			if (t instanceof AConfig t2)
				return "@AConfig(" + t2.value() + ")";
			if (t instanceof ClassInfo t2)
				return t2.getNameSimple();
			return t.toString();
		}
	};

	private static MethodInfo ofm(Class<?> c, String name, Class<?>...pt) {
		try {
			return MethodInfo.of(c.getDeclaredMethod(name, pt));
		} catch (NoSuchMethodException | SecurityException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {
		public void m() {}  // NOSONAR
	}
	static MethodInfo a_m = ofm(A1.class, "m");  // NOSONAR

	@Test void of_withDeclaringClass() {
		check("A1.m()", a_m);
		check("A1.m()", MethodInfo.of(ClassInfo.of(A1.class), a_m.inner()));
	}

	@Test void of_withoutDeclaringClass() {
		var mi = MethodInfo.of(a_m.inner());
		check("A1.m()", mi);
	}

	@Test void of_null() {
		assertThrows(IllegalArgumentException.class, () -> MethodInfo.of(null));
		assertThrows(IllegalArgumentException.class, () -> MethodInfo.of((ClassInfo)null, null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Matching methods.
	//-----------------------------------------------------------------------------------------------------------------

	public interface B1 {
		int foo(int x);
		int foo(String x);
		int foo();
	}
	public static class B2 {
		public int foo(int x) { return 0; }  // NOSONAR
		public int foo(String x) {return 0;}  // NOSONAR
		public int foo() {return 0;}
	}
	public static class B3 extends B2 implements B1 {
		@Override public int foo(int x) {return 0;}
		@Override public int foo(String x) {return 0;}
		@Override public int foo() {return 0;}
	}

	@Test void findMatchingMethods() throws Exception {
		var mi = MethodInfo.of(B3.class.getMethod("foo", int.class));
		var l = new ArrayList<MethodInfo>();
		mi.getMatchingMethods().stream().forEach(l::add);
		check("B3.foo(int),B1.foo(int),B2.foo(int)", l);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getMatchingMethods()
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	class GetMatchingMethodsTests {

		public interface BM1 {
			void foo(String s);
		}

		public interface BM2 {
			void foo(String s);
		}

		public class BM3 {
			public void foo(String s) {}  // NOSONAR
		}

		public interface BM4 extends BM1 {
			@Override void foo(String s);
		}

		public class BM5 extends BM3 implements BM2 {
			@Override public void foo(String s) {}  // NOSONAR
		}

		public class BM6 extends BM5 implements BM4 {
			@Override public void foo(String s) {}  // NOSONAR
		}

		@Test void simpleHierarchy() throws Exception {
			var mi = MethodInfo.of(B3.class.getMethod("foo", int.class));
			check("B3.foo(int),B1.foo(int),B2.foo(int)", mi.getMatchingMethods());
		}

		@Test void multipleInterfaces() throws Exception {
			var mi = MethodInfo.of(BM5.class.getMethod("foo", String.class));
			check("BM5.foo(String),BM2.foo(String),BM3.foo(String)", mi.getMatchingMethods());
		}

		@Test void nestedInterfaces() throws Exception {
			var mi = MethodInfo.of(BM6.class.getMethod("foo", String.class));
			check("BM6.foo(String),BM4.foo(String),BM1.foo(String),BM5.foo(String),BM2.foo(String),BM3.foo(String)", mi.getMatchingMethods());
		}

		@Test void onlyThis() throws Exception {
			var mi = MethodInfo.of(Object.class.getMethod("toString"));
			check("Object.toString()", mi.getMatchingMethods());
		}

		public interface BM7 {
			void bar();
		}

		public class BM8 implements BM7 {
			@Override public void bar() {}  // NOSONAR
			public void baz() {}  // NOSONAR
		}

		@Test void withInterface() throws Exception {
			var mi = MethodInfo.of(BM8.class.getMethod("bar"));
			check("BM8.bar(),BM7.bar()", mi.getMatchingMethods());
		}

		@Test void noMatchInParent() throws Exception {
			var mi = MethodInfo.of(BM8.class.getMethod("baz"));
			check("BM8.baz()", mi.getMatchingMethods());
		}

		// False match tests - different method names
		public class BM9 {
			public void foo(String s) {}  // NOSONAR
			public void bar(String s) {}  // NOSONAR
		}

		public class BM10 extends BM9 {
			@Override public void foo(String s) {}  // NOSONAR
		}

		@Test void differentMethodName() throws Exception {
			var mi = MethodInfo.of(BM10.class.getMethod("foo", String.class));
			// Should not match bar() even though it has same parameters
			check("BM10.foo(String),BM9.foo(String)", mi.getMatchingMethods());
		}

		// False match tests - same method name, different argument types
		public class BM11 {
			public void foo(int x) {}  // NOSONAR
			public void foo(String s) {}  // NOSONAR
		}

		public class BM12 extends BM11 {
			@Override public void foo(int x) {}  // NOSONAR
		}

		@Test void sameNameDifferentArgs() throws Exception {
			var mi = MethodInfo.of(BM12.class.getMethod("foo", int.class));
			// Should not match foo(String) even though same method name
			check("BM12.foo(int),BM11.foo(int)", mi.getMatchingMethods());
		}

		// False match tests - different method name, same argument types
		public interface BM13 {
			void bar(String s);
		}

		public class BM14 {
			public void baz(String s) {}  // NOSONAR
		}

		public class BM15 extends BM14 implements BM13 {
			@Override public void bar(String s) {}  // NOSONAR
			public void foo(String s) {}  // NOSONAR
		}

		@Test void differentNameSameArgs() throws Exception {
			var mi = MethodInfo.of(BM15.class.getMethod("foo", String.class));
			// Should not match bar() or baz() even though they have same parameters
			check("BM15.foo(String)", mi.getMatchingMethods());
		}

		// False match tests - different parameter count
		public class BM16 {
			public void foo(String s) {}  // NOSONAR
			public void foo(String s, int x) {}  // NOSONAR
		}

		public class BM17 extends BM16 {
			@Override public void foo(String s) {}  // NOSONAR
		}

		@Test void differentParameterCount() throws Exception {
			var mi = MethodInfo.of(BM17.class.getMethod("foo", String.class));
			// Should not match foo(String, int)
			check("BM17.foo(String),BM16.foo(String)", mi.getMatchingMethods());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@A("C1")
	public interface C1 {
		@A("a1") void a1();
		@A("a2a") void a2();
		@A("a3") void a3(CharSequence foo);
		void a4();
		void a5();
	}

	@A("C2")
	public static class C2 implements C1 {
		@Override public void a1() {}  // NOSONAR
		@Override @A("a2b") public void a2() {}  // NOSONAR
		@Override public void a3(CharSequence s) {}  // NOSONAR
		@Override public void a4() {}  // NOSONAR
		@Override public void a5() {}  // NOSONAR
	}

	@A("C3")
	public static class C3 extends C2 {
		@Override public void a1() {}  // NOSONAR
		@Override public void a2() {}  // NOSONAR
		@Override public void a3(CharSequence foo) {}  // NOSONAR
		@Override @A("a4") public void a4() {}  // NOSONAR
		@Override public void a5() {}  // NOSONAR
	}

	static MethodInfo
		c_a1 = ofm(C3.class, "a1"),  // NOSONAR
		c_a2 = ofm(C3.class, "a2"),  // NOSONAR
		c_a3 = ofm(C3.class, "a3", CharSequence.class),  // NOSONAR
		c_a4 = ofm(C3.class, "a4"),  // NOSONAR
		c_a5 = ofm(C3.class, "a5");  // NOSONAR

	@Test void getAnnotationsParentFirst() {
		check("@A(C1),@A(C2),@A(C3),@A(a1)", annotations(c_a1, A.class));
		check("@A(C1),@A(C2),@A(C3),@A(a2a),@A(a2b)", annotations(c_a2, A.class));
		check("@A(C1),@A(C2),@A(C3),@A(a3)", annotations(c_a3, A.class));
		check("@A(C1),@A(C2),@A(C3),@A(a4)", annotations(c_a4, A.class));
		check("@A(C1),@A(C2),@A(C3)", annotations(c_a5, A.class));
	}

	@Test void getAnnotationsParentFirst_notExistent() {
		check("", annotations(c_a1, AX.class));
		check("", annotations(c_a2, AX.class));
		check("", annotations(c_a3, AX.class));
		check("", annotations(c_a4, AX.class));
		check("", annotations(c_a5, AX.class));
	}

	private static List<A> annotations(MethodInfo mi, Class<? extends Annotation> a) {
		return rstream(AnnotationProvider.INSTANCE.find(a, mi)).map(AnnotationInfo::inner).map(x -> (A)x).toList();
	}

	@Test void getAnnotationAny() {
		check("@A(a1)", Stream.<Class<? extends Annotation>>of(AX.class, A.class).map(t -> c_a1.getAnnotations(t).findFirst().map(AnnotationInfo::inner).orElse(null)).filter(Objects::nonNull).findFirst().orElse(null));
		check("@A(a2b)", Stream.<Class<? extends Annotation>>of(AX.class, A.class).map(t -> c_a2.getAnnotations(t).findFirst().map(AnnotationInfo::inner).orElse(null)).filter(Objects::nonNull).findFirst().orElse(null));
		check("@A(a3)", Stream.<Class<? extends Annotation>>of(AX.class, A.class).map(t -> c_a3.getAnnotations(t).findFirst().map(AnnotationInfo::inner).orElse(null)).filter(Objects::nonNull).findFirst().orElse(null));
		check("@A(a4)", Stream.<Class<? extends Annotation>>of(AX.class, A.class).map(t -> c_a4.getAnnotations(t).findFirst().map(AnnotationInfo::inner).orElse(null)).filter(Objects::nonNull).findFirst().orElse(null));
		check(null, Stream.<Class<? extends Annotation>>of(AX.class, A.class).map(t -> c_a5.getAnnotations(t).findFirst().map(AnnotationInfo::inner).orElse(null)).filter(Objects::nonNull).findFirst().orElse(null));
	}

//	@Test void getAnnotationsMapParentFirst() {
//		// Note: Order changed after inlining - method annotations now come after class annotations
//		check("@PA(10),@A(C1),@A(C2),@A(C3),@A(a1)", rstream(c_a1.getAllAnnotations()).collect(Collectors.toList()));
//		check("@PA(10),@A(C1),@A(C2),@A(C3),@A(a2a),@A(a2b)", rstream(c_a2.getAllAnnotations()).collect(Collectors.toList()));
//		check("@PA(10),@A(C1),@A(C2),@A(C3),@A(a3)", rstream(c_a3.getAllAnnotations()).collect(Collectors.toList()));
//		check("@PA(10),@A(C1),@A(C2),@A(C3),@A(a4)", rstream(c_a4.getAllAnnotations()).collect(Collectors.toList()));
//		check("@PA(10),@A(C1),@A(C2),@A(C3)", rstream(c_a5.getAllAnnotations()).collect(Collectors.toList()));
//	}

	@A("C1") @AConfig("C1")
	public interface CB1 {
		@A("a1") @AConfig("a1") void a1();
		@A("a2a") @AConfig("a2a") void a2();
		@A("a3") @AConfig("a3") void a3(CharSequence foo);
		void a4();
		void a5();
	}

	@A("C2") @AConfig("C2")
	public static class CB2 implements CB1 {
		@Override public void a1() {}  // NOSONAR
		@Override @A("a2b") @AConfig("a2b") public void a2() {}  // NOSONAR
		@Override public void a3(CharSequence s) {}  // NOSONAR
		@Override public void a4() {}  // NOSONAR
		@Override public void a5() {}  // NOSONAR
	}

	@A("C3") @AConfig("C3")
	public static class CB3 extends CB2 {
		@Override public void a1() {}  // NOSONAR
		@Override public void a2() {}  // NOSONAR
		@Override public void a3(CharSequence foo) {}  // NOSONAR
		@Override @A("a4") @AConfig("a4") public void a4() {}  // NOSONAR
		@Override public void a5() {}  // NOSONAR
	}

	static MethodInfo
		cb_a1 = ofm(CB3.class, "a1"),  // NOSONAR
		cb_a2 = ofm(CB3.class, "a2"),  // NOSONAR
		cb_a3 = ofm(CB3.class, "a3", CharSequence.class),  // NOSONAR
		cb_a4 = ofm(CB3.class, "a4"),  // NOSONAR
		cb_a5 = ofm(CB3.class, "a5");  // NOSONAR

//	@Test void getConfigAnnotationsMapParentFirst() {
//		// Note: Order changed after inlining - method annotations now come after class annotations
//		check("@AConfig(C1),@AConfig(C2),@AConfig(C3),@AConfig(a1)", rstream(cb_a1.getAllAnnotations()).filter(CONTEXT_APPLY_FILTER).toList());
//		check("@AConfig(C1),@AConfig(C2),@AConfig(C3),@AConfig(a2a),@AConfig(a2b)", rstream(cb_a2.getAllAnnotations()).filter(CONTEXT_APPLY_FILTER).toList());
//		check("@AConfig(C1),@AConfig(C2),@AConfig(C3),@AConfig(a3)", rstream(cb_a3.getAllAnnotations()).filter(CONTEXT_APPLY_FILTER).toList());
//		check("@AConfig(C1),@AConfig(C2),@AConfig(C3),@AConfig(a4)", rstream(cb_a4.getAllAnnotations()).filter(CONTEXT_APPLY_FILTER).toList());
//		check("@AConfig(C1),@AConfig(C2),@AConfig(C3)", rstream(cb_a5.getAllAnnotations()).filter(CONTEXT_APPLY_FILTER).toList());
//	}

	//-----------------------------------------------------------------------------------------------------------------
	// Return type.
	//-----------------------------------------------------------------------------------------------------------------

	public static class D {
		public void a1() {}  // NOSONAR
		public Integer a2() {return null;}
	}
	static MethodInfo
		d_a1 = ofm(D.class, "a1"),  // NOSONAR
		d_a2 = ofm(D.class, "a2");  // NOSONAR

	@Test void getReturnType() {
		check("void", d_a1.getReturnType());
		check("Integer", d_a2.getReturnType());
	}

	@Test void hasReturnType() {
		assertTrue(d_a1.hasReturnType(void.class));
		assertFalse(d_a1.hasReturnType(Integer.class));
		assertTrue(d_a2.hasReturnType(Integer.class));
		assertFalse(d_a2.hasReturnType(Number.class));
	}

	@Test void hasReturnTypeParent() {
		assertTrue(d_a1.hasReturnTypeParent(void.class));
		assertFalse(d_a1.hasReturnTypeParent(Integer.class));
		assertTrue(d_a2.hasReturnTypeParent(Integer.class));
		assertTrue(d_a2.hasReturnTypeParent(Number.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	public static class E {
		private String f;
		public void a1(CharSequence foo) {
			f = foo == null ? null : foo.toString();
		}
		public void a2(int f1, int f2) {}  // NOSONAR
		public void a3() {}  // NOSONAR
	}
	static MethodInfo
		e_a1 = ofm(E.class, "a1", CharSequence.class),  // NOSONAR
		e_a2 = ofm(E.class, "a2", int.class, int.class),  // NOSONAR
		e_a3 = ofm(E.class, "a3");  // NOSONAR

	@Test void invoke() throws Exception {
		var e = new E();
		e_a1.invoke(e, "foo");
		assertEquals("foo", e.f);
		e_a1.invoke(e, (CharSequence)null);
		assertNull(e.f);
	}

	@Test void invokeFuzzy() throws Exception {
		var e = new E();
		e_a1.invokeLenient(e, "foo", 123);
		assertEquals("foo", e.f);
		e_a1.invokeLenient(e, 123, "bar");
		assertEquals("bar", e.f);
	}

	@Test void getSignature() {
		assertEquals("a1(java.lang.CharSequence)", e_a1.getSignature());
		assertEquals("a2(int,int)", e_a2.getSignature());
		assertEquals("a3", e_a3.getSignature());
	}

	@Test void hasOnlyParameterTypes() {
		assertTrue(e_a1.hasOnlyParameterTypes(CharSequence.class));
		assertTrue(e_a1.hasOnlyParameterTypes(CharSequence.class, Map.class));
		assertFalse(e_a1.hasOnlyParameterTypes());
	}

	public static class F {
		public void isA() {}  // NOSONAR
		public void is() {}  // NOSONAR
		public void getA() {}  // NOSONAR
		public void get() {}  // NOSONAR
		public void setA() {}  // NOSONAR
		public void set() {}  // NOSONAR
		public void foo() {}  // NOSONAR
	}
	static MethodInfo
		f_isA = ofm(F.class, "isA"),  // NOSONAR
		f_is = ofm(F.class, "is"),  // NOSONAR
		f_getA = ofm(F.class, "getA"),  // NOSONAR
		f_get = ofm(F.class, "get"),  // NOSONAR
		f_setA = ofm(F.class, "setA"),  // NOSONAR
		f_set = ofm(F.class, "set"),  // NOSONAR
		f_foo = ofm(F.class, "foo");  // NOSONAR

	@Test void getPropertyName() {
		assertEquals("a", f_isA.getPropertyName());
		assertEquals("is", f_is.getPropertyName());
		assertEquals("a", f_getA.getPropertyName());
		assertEquals("get", f_get.getPropertyName());
		assertEquals("a", f_setA.getPropertyName());
		assertEquals("set", f_set.getPropertyName());
		assertEquals("foo", f_foo.getPropertyName());
	}

	@Test void isBridge() {
		assertFalse(f_foo.isBridge());
	}

	public static class G {
		public void a1() {}  // NOSONAR
		public void a1(int a1) {}  // NOSONAR
		public void a1(int a1, int a2) {}  // NOSONAR
		public void a1(String a1) {}  // NOSONAR
		public void a2() {}  // NOSONAR
		public void a3() {}  // NOSONAR
	}
	static MethodInfo
		g_a1a = ofm(G.class, "a1"),  // NOSONAR
		g_a1b = ofm(G.class, "a1", int.class),  // NOSONAR
		g_a1c = ofm(G.class, "a1", int.class, int.class),  // NOSONAR
		g_a1d = ofm(G.class, "a1", String.class),  // NOSONAR
		g_a2 = ofm(G.class, "a2"),  // NOSONAR
		g_a3 = ofm(G.class, "a3");  // NOSONAR

	@Test void compareTo() {
		var s = new TreeSet<>(l(g_a1a, g_a1b, g_a1c, g_a1d, g_a2, g_a3));
		check("[a1(), a1(int), a1(String), a1(int,int), a2(), a3()]", s);
	}

	@Test void forEachParam_fluentChaining() {
		// Test stream operations on parameters
		int[] count = {0};
		g_a1c.getParameters().stream().filter(x -> true).forEach(x -> count[0]++);
		assertEquals(2, count[0]);
	}
}