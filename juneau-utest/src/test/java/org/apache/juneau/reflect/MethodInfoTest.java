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
package org.apache.juneau.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.Context.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.svl.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MethodInfoTest {

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
		public void apply(AnnotationInfo<AConfig> ai, Context.Builder b) {
		}
	}

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@SuppressWarnings({ "rawtypes" })
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof MethodInfo)
				return ((MethodInfo)t).getDeclaringClass().getSimpleName() + '.' + ((MethodInfo)t).getShortName();
			if (t instanceof Method)
				return ((Method)t).getDeclaringClass().getSimpleName() + '.' + MethodInfo.of((Method)t).getShortName();
			if (t instanceof List)
				return ((List<?>)t).stream().map(this).collect(Collectors.joining(","));
			if (t instanceof AnnotationInfo)
				return apply(((AnnotationInfo)t).inner());
			if (t instanceof A)
				return "@A(" + ((A)t).value() + ")";
			if (t instanceof PA)
				return "@PA(" + ((PA)t).value() + ")";
			if (t instanceof AConfig)
				return "@AConfig(" + ((AConfig)t).value() + ")";
			if (t instanceof AnnotationList) {
				AnnotationList al = (AnnotationList)t;
				return al.toString();
			}
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
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
		public void m() {}
	}
	static MethodInfo a_m = ofm(A1.class, "m");

	@Test
	public void of_withDeclaringClass() {
		check("A1.m()", a_m);
		check("A1.m()", MethodInfo.of(ClassInfo.of(A1.class), a_m.inner()));
	}

	@Test
	public void of_withoutDeclaringClass() {
		MethodInfo mi = MethodInfo.of(a_m.inner());
		check("A1.m()", mi);
	}

	@Test
	public void of_null() {
		check(null, MethodInfo.of(null));
		check(null, MethodInfo.of((ClassInfo)null, null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Matching methods.
	//-----------------------------------------------------------------------------------------------------------------

	public static interface B1 {
		public int foo(int x);
		public int foo(String x);
		public int foo();
	}
	public static class B2 {
		public int foo(int x) { return 0; }
		public int foo(String x) {return 0;}
		public int foo() {return 0;}
	}
	public static class B3 extends B2 implements B1 {
		@Override
		public int foo(int x) {return 0;}
		@Override
		public int foo(String x) {return 0;}
		@Override
		public int foo() {return 0;}
	}

	@Test
	public void findMatchingMethods() throws Exception {
		MethodInfo mi = MethodInfo.of(B3.class.getMethod("foo", int.class));
		List<MethodInfo> l = new ArrayList<>();
		mi.forEachMatching(x -> true, x -> l.add(x));
		check("B3.foo(int),B2.foo(int),B1.foo(int)", l);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@A("C1")
	public static interface C1 {
		@A("a1") void a1();
		@A("a2a") void a2();
		@A("a3") void a3(CharSequence foo);
		void a4();
		void a5();
	}

	@A("C2")
	public static class C2 implements C1 {
		@Override public void a1() {}
		@Override @A("a2b") public void a2() {}
		@Override public void a3(CharSequence s) {}
		@Override public void a4() {}
		@Override public void a5() {}
	}

	@A("C3")
	public static class C3 extends C2 {
		@Override public void a1() {}
		@Override public void a2() {}
		@Override public void a3(CharSequence foo) {}
		@Override @A("a4") public void a4() {}
		@Override public void a5() {}
	}

	static MethodInfo
		c_a1 = ofm(C3.class, "a1"),
		c_a2 = ofm(C3.class, "a2"),
		c_a3 = ofm(C3.class, "a3", CharSequence.class),
		c_a4 = ofm(C3.class, "a4"),
		c_a5 = ofm(C3.class, "a5");

	@Test
	public void getAnnotationsParentFirst() {
		check("@A(C1),@A(C2),@A(C3),@A(a1)", annotations(c_a1, A.class));
		check("@A(C1),@A(C2),@A(C3),@A(a2a),@A(a2b)", annotations(c_a2, A.class));
		check("@A(C1),@A(C2),@A(C3),@A(a3)", annotations(c_a3, A.class));
		check("@A(C1),@A(C2),@A(C3),@A(a4)", annotations(c_a4, A.class));
		check("@A(C1),@A(C2),@A(C3)", annotations(c_a5, A.class));
	}

	@Test
	public void getAnnotationsParentFirst_notExistent() {
		check("", annotations(c_a1, AX.class));
		check("", annotations(c_a2, AX.class));
		check("", annotations(c_a3, AX.class));
		check("", annotations(c_a4, AX.class));
		check("", annotations(c_a5, AX.class));
	}

	private static List<A> annotations(MethodInfo mi, Class<? extends Annotation> a) {
		List<A> l = new ArrayList<>();
		mi.forEachAnnotation(a, x -> true, x -> l.add((A)x));
		return l;
	}

	@Test
	public void getAnnotation() {
		check("@A(a1)", c_a1.getAnnotation(A.class));
		check("@A(a2b)", c_a2.getAnnotation(A.class));
		check("@A(a3)", c_a3.getAnnotation(A.class));
		check("@A(a4)", c_a4.getAnnotation(A.class));
		check(null, c_a5.getAnnotation(A.class));
	}

	@Test
	public void getAnnotationAny() {
		check("@A(a1)", c_a1.getAnyAnnotation(AX.class, A.class));
		check("@A(a2b)", c_a2.getAnyAnnotation(AX.class, A.class));
		check("@A(a3)", c_a3.getAnyAnnotation(AX.class, A.class));
		check("@A(a4)", c_a4.getAnyAnnotation(AX.class, A.class));
		check(null, c_a5.getAnyAnnotation(AX.class, A.class));
	}

	@Test
	public void getAnnotationsMapParentFirst() {
		check("@PA(10),@A(C1),@A(a1),@A(C2),@A(C3)", c_a1.getAnnotationList());
		check("@PA(10),@A(C1),@A(a2a),@A(C2),@A(a2b),@A(C3)", c_a2.getAnnotationList());
		check("@PA(10),@A(C1),@A(a3),@A(C2),@A(C3)", c_a3.getAnnotationList());
		check("@PA(10),@A(C1),@A(C2),@A(C3),@A(a4)", c_a4.getAnnotationList());
		check("@PA(10),@A(C1),@A(C2),@A(C3)", c_a5.getAnnotationList());
	}

	@A("C1") @AConfig("C1")
	public static interface CB1 {
		@A("a1") @AConfig("a1") void a1();
		@A("a2a") @AConfig("a2a") void a2();
		@A("a3") @AConfig("a3") void a3(CharSequence foo);
		void a4();
		void a5();
	}

	@A("C2") @AConfig("C2")
	public static class CB2 implements CB1 {
		@Override public void a1() {}
		@Override @A("a2b") @AConfig("a2b") public void a2() {}
		@Override public void a3(CharSequence s) {}
		@Override public void a4() {}
		@Override public void a5() {}
	}

	@A("C3") @AConfig("C3")
	public static class CB3 extends CB2 {
		@Override public void a1() {}
		@Override public void a2() {}
		@Override public void a3(CharSequence foo) {}
		@Override @A("a4") @AConfig("a4") public void a4() {}
		@Override public void a5() {}
	}

	static MethodInfo
		cb_a1 = ofm(CB3.class, "a1"),
		cb_a2 = ofm(CB3.class, "a2"),
		cb_a3 = ofm(CB3.class, "a3", CharSequence.class),
		cb_a4 = ofm(CB3.class, "a4"),
		cb_a5 = ofm(CB3.class, "a5");

	@Test
	public void getConfigAnnotationsMapParentFirst() {
		check("@AConfig(C1),@AConfig(a1),@AConfig(C2),@AConfig(C3)", cb_a1.getAnnotationList(CONTEXT_APPLY_FILTER));
		check("@AConfig(C1),@AConfig(a2a),@AConfig(C2),@AConfig(a2b),@AConfig(C3)", cb_a2.getAnnotationList(CONTEXT_APPLY_FILTER));
		check("@AConfig(C1),@AConfig(a3),@AConfig(C2),@AConfig(C3)", cb_a3.getAnnotationList(CONTEXT_APPLY_FILTER));
		check("@AConfig(C1),@AConfig(C2),@AConfig(C3),@AConfig(a4)", cb_a4.getAnnotationList(CONTEXT_APPLY_FILTER));
		check("@AConfig(C1),@AConfig(C2),@AConfig(C3)", cb_a5.getAnnotationList(CONTEXT_APPLY_FILTER));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Return type.
	//-----------------------------------------------------------------------------------------------------------------

	public static class D {
		public void a1() {}
		public Integer a2() {return null;}
	}
	static MethodInfo
		d_a1 = ofm(D.class, "a1"),
		d_a2 = ofm(D.class, "a2");

	@Test
	public void getReturnType() {
		check("void", d_a1.getReturnType());
		check("Integer", d_a2.getReturnType());
	}

	@Test
	public void hasReturnType() {
		assertTrue(d_a1.hasReturnType(void.class));
		assertFalse(d_a1.hasReturnType(Integer.class));
		assertTrue(d_a2.hasReturnType(Integer.class));
		assertFalse(d_a2.hasReturnType(Number.class));
	}

	@Test
	public void hasReturnTypeParent() {
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
		public void a2(int f1, int f2) {}
		public void a3() {}
	}
	static MethodInfo
		e_a1 = ofm(E.class, "a1", CharSequence.class),
		e_a2 = ofm(E.class, "a2", int.class, int.class),
		e_a3 = ofm(E.class, "a3");

	@Test
	public void invoke() throws Exception {
		E e = new E();
		e_a1.invoke(e, "foo");
		assertEquals("foo", e.f);
		e_a1.invoke(e, (CharSequence)null);
		assertNull(e.f);
	}

	@Test
	public void invokeFuzzy() throws Exception {
		E e = new E();
		e_a1.invokeFuzzy(e, "foo", 123);
		assertEquals("foo", e.f);
		e_a1.invokeFuzzy(e, 123, "bar");
		assertEquals("bar", e.f);
	}

	@Test
	public void getSignature() {
		assertEquals("a1(java.lang.CharSequence)", e_a1.getSignature());
		assertEquals("a2(int,int)", e_a2.getSignature());
		assertEquals("a3", e_a3.getSignature());
	}

	@Test
	public void argsOnlyOfType() {
		assertTrue(e_a1.argsOnlyOfType(CharSequence.class));
		assertTrue(e_a1.argsOnlyOfType(CharSequence.class, Map.class));
		assertFalse(e_a1.argsOnlyOfType());
	}

	public static class F {
		public void isA() {}
		public void is() {}
		public void getA() {}
		public void get() {}
		public void setA() {}
		public void set() {}
		public void foo() {}
	}
	static MethodInfo
		f_isA = ofm(F.class, "isA"),
		f_is = ofm(F.class, "is"),
		f_getA = ofm(F.class, "getA"),
		f_get = ofm(F.class, "get"),
		f_setA = ofm(F.class, "setA"),
		f_set = ofm(F.class, "set"),
		f_foo = ofm(F.class, "foo");

	@Test
	public void getPropertyName() {
		assertEquals("a", f_isA.getPropertyName());
		assertEquals("is", f_is.getPropertyName());
		assertEquals("a", f_getA.getPropertyName());
		assertEquals("get", f_get.getPropertyName());
		assertEquals("a", f_setA.getPropertyName());
		assertEquals("set", f_set.getPropertyName());
		assertEquals("foo", f_foo.getPropertyName());
	}

	@Test
	public void isBridge() {
		assertFalse(f_foo.isBridge());
	}

	public static class G {
		public void a1() {}
		public void a1(int a1) {}
		public void a1(int a1, int a2) {}
		public void a1(String a1) {}
		public void a2() {}
		public void a3() {}
	}
	static MethodInfo
		g_a1a = ofm(G.class, "a1"),
		g_a1b = ofm(G.class, "a1", int.class),
		g_a1c = ofm(G.class, "a1", int.class, int.class),
		g_a1d = ofm(G.class, "a1", String.class),
		g_a2 = ofm(G.class, "a2"),
		g_a3 = ofm(G.class, "a3");

	@Test
	public void compareTo() {
		Set<MethodInfo> s = new TreeSet<>(Arrays.asList(g_a1a, g_a1b, g_a1c, g_a1d, g_a2, g_a3));
		check("[a1(), a1(int), a1(String), a1(int,int), a2(), a3()]", s);
	}
}
