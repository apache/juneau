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
package org.apache.juneau.reflection;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.reflect.*;
import org.junit.*;

public class MethodInfoTest {

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

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
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
			if (t instanceof A)
				return "@A(" + ((A)t).value() + ")";
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
		check(null, MethodInfo.of(null, null));
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
		check("B3.foo(int),B2.foo(int),B1.foo(int)", mi.getMatching());
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	public static interface C1 {
		@A("a1") void a1();
		@A("a2a") void a2();
		@A("a3") void a3(CharSequence foo);
		void a4();
		void a5();
	}

	public static class C2 implements C1 {
		@Override public void a1() {}
		@Override @A("a2b") public void a2() {}
		@Override public void a3(CharSequence s) {}
		@Override public void a4() {}
		@Override public void a5() {}
	}

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
	public void getAnnotations() {
		check("@A(a1)", c_a1.getAnnotations(A.class));
		check("@A(a2b),@A(a2a)", c_a2.getAnnotations(A.class));
		check("@A(a3)", c_a3.getAnnotations(A.class));
		check("@A(a4)", c_a4.getAnnotations(A.class));
		check("", c_a5.getAnnotations(A.class));
	}

	@Test
	public void getAnnotations_notExistent() {
		check("", c_a1.getAnnotations(AX.class));
		check("", c_a2.getAnnotations(AX.class));
		check("", c_a3.getAnnotations(AX.class));
		check("", c_a4.getAnnotations(AX.class));
		check("", c_a5.getAnnotations(AX.class));
	}

	@Test
	public void getAnnotationsParentFirst() {
		check("@A(a1)", c_a1.getAnnotationsParentFirst(A.class));
		check("@A(a2a),@A(a2b)", c_a2.getAnnotationsParentFirst(A.class));
		check("@A(a3)", c_a3.getAnnotationsParentFirst(A.class));
		check("@A(a4)", c_a4.getAnnotationsParentFirst(A.class));
		check("", c_a5.getAnnotationsParentFirst(A.class));
	}

	@Test
	public void appendAnnotations() {
		check("@A(a1)", c_a1.appendAnnotations(new ArrayList<>(), A.class));
		check("@A(a2b),@A(a2a)", c_a2.appendAnnotations(new ArrayList<>(), A.class));
		check("@A(a3)", c_a3.appendAnnotations(new ArrayList<>(), A.class));
		check("@A(a4)", c_a4.appendAnnotations(new ArrayList<>(), A.class));
		check("", c_a5.appendAnnotations(new ArrayList<>(), A.class));
	}

	@Test
	public void appendAnnotationsParentFirst() {
		check("@A(a1)", c_a1.appendAnnotationsParentFirst(new ArrayList<>(), A.class));
		check("@A(a2a),@A(a2b)", c_a2.appendAnnotationsParentFirst(new ArrayList<>(), A.class));
		check("@A(a3)", c_a3.appendAnnotationsParentFirst(new ArrayList<>(), A.class));
		check("@A(a4)", c_a4.appendAnnotationsParentFirst(new ArrayList<>(), A.class));
		check("", c_a5.appendAnnotationsParentFirst(new ArrayList<>(), A.class));
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
		check("@A(a1)", c_a1.getAnnotation(AX.class, A.class));
		check("@A(a2b)", c_a2.getAnnotation(AX.class, A.class));
		check("@A(a3)", c_a3.getAnnotation(AX.class, A.class));
		check("@A(a4)", c_a4.getAnnotation(AX.class, A.class));
		check(null, c_a5.getAnnotation(AX.class, A.class));
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
	public void invoke() {
		try {
			E e = new E();
			e_a1.invoke(e, "foo");
			assertEquals("foo", e.f);
			e_a1.invoke(e, (CharSequence)null);
			assertNull(e.f);
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}

	@Test
	public void invokeFuzzy() {
		try {
			E e = new E();
			e_a1.invokeFuzzy(e, "foo", 123);
			assertEquals("foo", e.f);
			e_a1.invokeFuzzy(e, 123, "bar");
			assertEquals("bar", e.f);
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
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
