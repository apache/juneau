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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.junit.*;

/**
 * ParamInfo tests.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ParamInfoTest {

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
			if (t instanceof List)
				return ((List<?>)t).stream().map(this).collect(Collectors.joining(","));
			if (t.getClass().isArray())
				return StreamSupport.stream(ArrayUtils.toList(t, Object.class).spliterator(), false).map(this).collect(Collectors.joining(","));
			if (t instanceof MethodInfo)
				return ((MethodInfo)t).getDeclaringClass().getSimpleName() + '.' + ((MethodInfo)t).getShortName();
			if (t instanceof CA)
				return "@CA(" + ((CA)t).value() + ")";
			if (t instanceof DA)
				return "@DA(" + ((DA)t).value() + ")";
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	static class B {
		public B(int a, String b) {}
		public void a1(int a, String b) {}
		void a2(int a, String b) {}
	}

	static ClassInfo b = ClassInfo.of(B.class);
	static ParamInfo
		b_b_a = b.getPublicConstructor(x -> x.hasParamTypes(int.class, String.class)).getParam(0),
		b_b_b = b.getPublicConstructor(x -> x.hasParamTypes(int.class, String.class)).getParam(1),
		b_a1_a = b.getMethod(x -> x.hasName("a1")).getParam(0),
		b_a1_b = b.getMethod(x -> x.hasName("a1")).getParam(1),
		b_a2_a = b.getMethod(x -> x.hasName("a2")).getParam(0),
		b_a2_b = b.getMethod(x -> x.hasName("a2")).getParam(1);


	@Test
	public void getIndex() {
		assertEquals(0, b_b_a.getIndex());
		assertEquals(1, b_b_b.getIndex());
		assertEquals(0, b_a1_a.getIndex());
		assertEquals(1, b_a1_b.getIndex());
		assertEquals(0, b_a2_a.getIndex());
		assertEquals(1, b_a2_b.getIndex());
	}

	@Test
	public void getMethod() {
		check("B.a1(int,String)", b_a1_a.getMethod());
		check("B.a1(int,String)", b_a1_b.getMethod());
		check("B.a2(int,String)", b_a2_a.getMethod());
		check("B.a2(int,String)", b_a2_b.getMethod());
	}

	@Test
	public void getMethod_onConstrutor() {
		check(null, b_b_a.getMethod());
		check(null, b_b_b.getMethod());
	}

	@Test
	public void getConstructor() {
		check("B(int,String)", b_b_a.getConstructor());
		check("B(int,String)", b_b_b.getConstructor());
	}

	@Test
	public void getConstructor_onMethod() {
		check(null, b_a1_a.getConstructor());
		check(null, b_a1_b.getConstructor());
		check(null, b_a2_a.getConstructor());
		check(null, b_a2_b.getConstructor());
	}

	@Test
	public void getParameterType() {
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
	@CA("3") public static interface C3 {}
	@CA("4") public static interface C4 {}

	public static interface CB {
		public void a1(@CA("5") C1 x);
		public void a2(@CA("5") C1 x);
	}
	public static class CC implements CB {
		public CC(@CA("9") C1 x) {}
		@Override
		public void a1(C1 x) {}
		@Override
		public void a2(@CA("6") C1 x) {}
	}
	static ClassInfo
		cb = ClassInfo.of(CB.class),
		cc = ClassInfo.of(CC.class);
	static ParamInfo
		cc_cc = cc.getPublicConstructor(x -> x.hasParamTypes(C1.class)).getParam(0),
		cb_a1 = cb.getMethod(x -> x.hasName("a1")).getParam(0),
		cb_a2 = cb.getMethod(x -> x.hasName("a2")).getParam(0),
		cc_a1 = cc.getMethod(x -> x.hasName("a1")).getParam(0),
		cc_a2 = cc.getMethod(x -> x.hasName("a2")).getParam(0);

	@Test
	public void getDeclaredAnnotations() throws Exception {
		check("@CA(5)", declaredAnnotations(cb_a1, CA.class));
		check("@CA(5)", declaredAnnotations(cb_a2, CA.class));
		check("", declaredAnnotations(cc_a1, CA.class));
		check("@CA(6)", declaredAnnotations(cc_a2, CA.class));
	}

	@Test
	public void getDeclaredAnnotations_constructor() throws Exception {
		check("@CA(9)", declaredAnnotations(cc_cc, CA.class));
	}

	private static <T extends Annotation> List<T> declaredAnnotations(ParamInfo pi, Class<T> type) {
		List<T> l = new ArrayList<>();
		pi.forEachDeclaredAnnotation(type, x -> true, x -> l.add(x));
		return l;
	}

	@Test
	public void getDeclaredAnnotation() throws Exception {
		check("@CA(5)", cb_a1.getDeclaredAnnotation(CA.class));
		check("@CA(5)", cb_a2.getDeclaredAnnotation(CA.class));
		check(null, cc_a1.getDeclaredAnnotation(CA.class));
		check("@CA(6)", cc_a2.getDeclaredAnnotation(CA.class));
	}

	@Test
	public void getDeclaredAnnotation_constructor() throws Exception {
		check("@CA(9)", cc_cc.getDeclaredAnnotation(CA.class));
	}

	@Test
	public void getDeclaredAnnotation_notFound() throws Exception {
		check(null, cb_a1.getDeclaredAnnotation(DA.class));
	}

	@Test
	public void getDeclaredAnnotation_notFound_constructor() throws Exception {
		check(null, cc_cc.getDeclaredAnnotation(DA.class));
	}

	@Test
	public void getDeclaredAnnotation_null() throws Exception {
		check(null, cb_a1.getDeclaredAnnotation(null));
	}

	@Test
	public void getDeclaredAnnotation_null_constructor() throws Exception {
		check(null, cc_cc.getDeclaredAnnotation(null));
	}

	@Test
	public void getAnnotationsParentFirst() throws Exception {
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5)", annotations(cb_a1, CA.class));
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5)", annotations(cb_a2, CA.class));
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5)", annotations(cc_a1, CA.class));
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(5),@CA(6)", annotations(cc_a2, CA.class));
	}

	@Test
	public void getAnnotationsParentFirst_notFound() throws Exception {
		check("", annotations(cb_a1, DA.class));
	}

	@Test
	public void getAnnotationsParentFirst_constructor() throws Exception {
		check("@CA(4),@CA(3),@CA(2),@CA(1),@CA(9)", annotations(cc_cc, CA.class));
	}

	@Test
	public void getAnnotationsParentFirst_notFound_constructor() throws Exception {
		check("", annotations(cc_cc, DA.class));
	}

	@Test
	public void getAnnotation() throws Exception {
		check("@CA(5)", cb_a1.getAnnotation(CA.class));
		check("@CA(5)", cb_a2.getAnnotation(CA.class));
		check("@CA(5)", cc_a1.getAnnotation(CA.class));
		check("@CA(6)", cc_a2.getAnnotation(CA.class));
	}

	@Test
	public void getAnnotation_notFound() throws Exception {
		check(null, cb_a1.getAnnotation(DA.class));
	}

	@Test
	public void getAnnotation_constructor() throws Exception {
		check("@CA(9)", cc_cc.getAnnotation(CA.class));
	}

	@Test
	public void getAnnotation_notFound_constructor() throws Exception {
		check(null, cc_cc.getAnnotation(DA.class));
	}

	@Test
	public void getAnnotation_twice() throws Exception {
		check("@CA(5)", cb_a1.getAnnotation(CA.class));
		check("@CA(5)", cb_a1.getAnnotation(CA.class));
	}

	@Test
	public void getAnnotation_twice_constructor() throws Exception {
		check("@CA(9)", cc_cc.getAnnotation(CA.class));
		check("@CA(9)", cc_cc.getAnnotation(CA.class));
	}

	@Test
	public void hasAnnotation() throws Exception {
		assertTrue(cb_a1.hasAnnotation(CA.class));
		assertTrue(cb_a2.hasAnnotation(CA.class));
		assertTrue(cc_a1.hasAnnotation(CA.class));
		assertTrue(cc_a2.hasAnnotation(CA.class));
		assertFalse(cb_a1.hasAnnotation(DA.class));
	}

	@Test
	public void hasAnnotation_constructor() throws Exception {
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
	@DA("3") public static interface D3 {}
	@DA("4") public static interface D4 {}

	public static interface DB {
		public void a1(@DA("0") D1 x);
	}
	public static class DC implements DB {
		@Override
		public void a1(@DA("5") D1 x) {}
	}

	static ClassInfo
		db = ClassInfo.of(DB.class),
		dc = ClassInfo.of(DC.class);
	static ParamInfo
		db_a1 = db.getMethod(x -> x.hasName("a1")).getParam(0),
		dc_a1 = dc.getMethod(x -> x.hasName("a1")).getParam(0);

	@Test
	public void getAnnotationsParentFirst_inherited() throws Exception {
		check("@DA(4),@DA(3),@DA(2),@DA(1),@DA(0)", annotations(db_a1, DA.class));
		check("@DA(4),@DA(3),@DA(2),@DA(1),@DA(0),@DA(5)", annotations(dc_a1, DA.class));
	}

	@Test
	public void getAnnotationsParentFirst_inherited_notFound() throws Exception {
		check("", annotations(db_a1, CA.class));
	}

	@Test
	public void getAnnotation_inherited() throws Exception {
		check("@DA(0)", db_a1.getAnnotation(DA.class));
		check("@DA(5)", dc_a1.getAnnotation(DA.class));
	}

	@Test
	public void getAnnotation_inherited_notFound() throws Exception {
		check(null, db_a1.getAnnotation(CA.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	static class E {
		public void a1(int a, @Name("b") int b) {}
	}

	static ClassInfo e = ClassInfo.of(E.class);
	static ParamInfo
		e_a1_a = e.getMethod(x -> x.hasName("a1")).getParam(0),
		e_a1_b = e.getMethod(x -> x.hasName("a1")).getParam(1);

	@Test
	public void hasName() {
		e_a1_a.hasName();  // This might be true or false based on the JVM compiler used.
		assertTrue(e_a1_b.hasName());
	}

	@Test
	public void getName() {
		e_a1_a.getName();  // This might be null or a value based on the JVM compiler used.
		assertEquals("b", e_a1_b.getName());
	}

	@Test
	public void toString2() {
		assertEquals("a1[1]", e_a1_b.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static <T extends Annotation> List<T> annotations(ParamInfo pi, Class<T> a) {
		List<T> l = list();
		pi.forEachAnnotation(a, x -> true, x -> l.add(x));
		return l;
	}
}
