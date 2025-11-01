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
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.reflect.ReflectFlags.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.common.reflect.*;
import org.junit.jupiter.api.*;

class ExecutableInfo_Test extends TestBase {

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
			if (t instanceof Annotation)
				return "@" + ((Annotation)t).annotationType().getSimpleName() + "()";
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			if (t instanceof ConstructorInfo)
				return ((ConstructorInfo)t).getShortName();
			if (t instanceof ParameterInfo)
				return apply(((ParameterInfo)t).toString());
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	static class A {
		public A() {}  // NOSONAR
		public void foo() {}  // NOSONAR
	}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void isConstructor() {
		assertTrue(a.getPublicConstructor(ConstructorInfo::hasNoParams).isConstructor());
		assertFalse(a.getPublicMethod(x -> x.hasName("foo")).isConstructor());
	}

	@Test void getDeclaringClass() {
		check("A", a.getPublicConstructor(ConstructorInfo::hasNoParams).getDeclaringClass());
		check("A", a.getPublicMethod(x -> x.hasName("foo")).getDeclaringClass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameters
	//-----------------------------------------------------------------------------------------------------------------

	static class B {
		public B() {}
		public B(String s) {}  // NOSONAR
		public void m() {}  // NOSONAR
		public int m(String s) { return 0; }  // NOSONAR
	}
	static ClassInfo b = ClassInfo.of(B.class);
	static ExecutableInfo
		b_c1 = b.getPublicConstructor(ConstructorInfo::hasNoParams),
		b_c2 = b.getPublicConstructor(x -> x.hasParamTypes(String.class)),
		b_m1 = b.getPublicMethod(x -> x.hasName("m") && x.hasNoParams()),
		b_m2 = b.getPublicMethod(x -> x.hasName("m") && x.hasParamTypes(String.class))
	;

	@Test void getParamCount() {
		assertEquals(0, b_c1.getParamCount());
		assertEquals(1, b_c2.getParamCount());
		assertEquals(0, b_m1.getParamCount());
		assertEquals(1, b_m2.getParamCount());
	}

	@Test void hasParams() {
		assertEquals(false, b_c1.hasParams());
		assertEquals(true, b_c2.hasParams());
		assertEquals(false, b_m1.hasParams());
		assertEquals(true, b_m2.hasParams());
	}

	@Test void hasNoParams() {
		assertEquals(true, b_c1.hasNoParams());
		assertEquals(false, b_c2.hasNoParams());
		assertEquals(true, b_m1.hasNoParams());
		assertEquals(false, b_m2.hasNoParams());
	}

	@Test void hasNumParams() {
		assertEquals(false, b_c1.hasNumParams(1));
		assertEquals(true, b_c2.hasNumParams(1));
		assertEquals(false, b_m1.hasNumParams(1));
		assertEquals(true, b_m2.hasNumParams(1));
	}

	@Test void getParams() {
		check("", b_c1.getParams());
		check("B[0]", b_c2.getParams());
		check("", b_m1.getParams());
		check("m[0]", b_m2.getParams());
	}

	@Test void getParams_twice() {
		check("", b_c1.getParams());
		check("", b_c1.getParams());
	}

	@Test void getParam() {
		check("B[0]", b_c2.getParam(0));
		check("m[0]", b_m2.getParam(0));
	}

	@Test void getParam_nocache() {
		var b2 = ClassInfo.of(B.class);
		check("B[0]", b2.getPublicConstructor(x -> x.hasParamTypes(String.class)).getParam(0));
		check("m[0]", b2.getPublicMethod(x -> x.hasName("m") && x.hasParamTypes(String.class)).getParam(0));
	}

	@Test void getParam_indexOutOfBounds() {
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '0'.  No parameters.", ()->b_c1.getParam(0));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '-1'.  Parameter count: 1", ()->b_c2.getParam(-1));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '1'.  Parameter count: 1", ()->b_c2.getParam(1));
	}

	@Test void getParam_indexOutOfBounds_noCache() {
		var b2 = ClassInfo.of(B.class);
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '0'.  No parameters.", ()->b2.getPublicConstructor(ConstructorInfo::hasNoParams).getParam(0));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '-1'.  Parameter count: 1", ()->b2.getPublicConstructor(x -> x.hasParamTypes(String.class)).getParam(-1));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '1'.  Parameter count: 1", ()->b2.getPublicConstructor(x -> x.hasParamTypes(String.class)).getParam(1));
	}

	@Test void getParamTypes() {
		check("", b_c1.getParamTypes());
		check("String", b_c2.getParamTypes());
		check("", b_m1.getParamTypes());
		check("String", b_m2.getParamTypes());
	}

	@Test void getParamTypes_twice() {
		check("", b_c1.getParamTypes());
		check("", b_c1.getParamTypes());
	}

	enum B1 {
		FOO;
	}

	@Test void getParamTypes_enum() {
		var b1 = ClassInfo.of(B1.class);
		check("B1(String,int)", b1.getDeclaredConstructors());
		check("String,int", b1.getDeclaredConstructors().get(0).getParamTypes());
	}

	@Test void getParamType() {
		check("String", b_c2.getParamType(0));
		check("String", b_m2.getParamType(0));
	}

	@Test void getRawParamTypes() {
		check("", b_c1.getRawParamTypes());
		check("String", b_c2.getRawParamTypes());
		check("", b_m1.getRawParamTypes());
		check("String", b_m2.getRawParamTypes());
	}

	@Test void getRawParamType() {
		check("String", b_c2.getRawParamType(0));
		check("String", b_m2.getRawParamType(0));
	}

	@Test void getRawGenericParamType() {
		check("String", b_c2.getRawGenericParamType(0));
		check("String", b_m2.getRawGenericParamType(0));
	}

	@Test void getRawGenericParamTypes() {
		check("", b_c1.getRawGenericParamTypes());
		check("String", b_c2.getRawGenericParamTypes());
		check("", b_m1.getRawGenericParamTypes());
		check("String", b_m2.getRawGenericParamTypes());
	}

	@Test void getRawParameters() {
		check("", b_c1.getRawParameters());
		assertTrue(b_c2.getRawParameters().get(0).toString().startsWith("java.lang.String "));
		check("", b_m1.getRawParameters());
		assertTrue(b_m2.getRawParameters().get(0).toString().startsWith("java.lang.String "));
	}

	@Test void getRawParameter() {
		assertTrue(b_c2.getRawParameter(0).toString().startsWith("java.lang.String "));
		assertTrue(b_m2.getRawParameter(0).toString().startsWith("java.lang.String "));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@Documented
	@Target({PARAMETER,METHOD,CONSTRUCTOR})
	@Retention(RUNTIME)
	@Inherited
	public static @interface CA {}

	static class C {
		public C() {}
		public C(@CA String foo) {}  // NOSONAR
		public @CA C(int bar) {}  // NOSONAR
		public void m() {}  // NOSONAR
		public void m(@CA String foo) {}  // NOSONAR
		public @CA void m(int bar) {}  // NOSONAR
	}
	static ClassInfo c = ClassInfo.of(C.class);
	static ConstructorInfo
		c_c1=c.getPublicConstructor(ConstructorInfo::hasNoParams),
		c_c2=c.getPublicConstructor(x -> x.hasParamTypes(String.class)),
		c_c3=c.getPublicConstructor(x -> x.hasParamTypes(int.class))
	;
	static MethodInfo
		c_m1=c.getPublicMethod(x -> x.hasName("m") && x.hasNoParams()),
		c_m2=c.getPublicMethod(x -> x.hasName("m") && x.hasParamTypes(String.class)),
		c_m3=c.getPublicMethod(x -> x.hasName("m") && x.hasParamTypes(int.class))
	;

	@Test void getParameterAnnotations() {
		check("", c_c1._getParameterAnnotations());
		check("@CA()", c_c2._getParameterAnnotations());
		check("", c_c3._getParameterAnnotations());
		check("", c_m1._getParameterAnnotations());
		check("@CA()", c_m2._getParameterAnnotations());
		check("", c_m3._getParameterAnnotations());
	}

	@Test void getParameterAnnotations_atIndex() {
		check("@CA()", c_c2._getParameterAnnotations(0));
		check("@CA()", c_m2._getParameterAnnotations(0));
	}

	@Test void hasAnnotation() {
		assertFalse(c_c1.hasAnnotation(CA.class));
		assertFalse(c_c2.hasAnnotation(CA.class));
		assertTrue(c_c3.hasAnnotation(CA.class));
		assertFalse(c_m1.hasAnnotation(CA.class));
		assertFalse(c_m2.hasAnnotation(CA.class));
		assertTrue(c_m3.hasAnnotation(CA.class));
	}

	@Test void getAnnotation() {
		check(null, c_c2.getAnnotation(CA.class));
		check("@CA()", c_c3.getAnnotation(CA.class));
		check(null, c_m1.getAnnotation(CA.class));
		check(null, c_m2.getAnnotation(CA.class));
		check("@CA()", c_m3.getAnnotation(CA.class));
	}

	@Test void getAnnotation_nullArg() {
		check(null, c_c3.getAnnotation(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Exceptions
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	static class D {
		public D() throws IOException {}  // NOSONAR
		public void m() throws IOException {}  // NOSONAR
	}
	static ClassInfo d = ClassInfo.of(D.class);
	static ExecutableInfo
		d_c=d.getPublicConstructor(ConstructorInfo::hasNoParams),
		d_m=d.getPublicMethod(x -> x.hasName("m"))
	;

	@Test void getExceptionTypes() {
		check("IOException", d_c.getExceptionTypes());
		check("IOException", d_m.getExceptionTypes());
	}

	@Test void getExceptionTypes_twice() {
		check("IOException", d_c.getExceptionTypes());
		check("IOException", d_c.getExceptionTypes());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	abstract static class E {
		@Deprecated public void deprecated() {}
		public void notDeprecated() {}
		public void hasParams(int foo) {}
		public void hasStringParam(String foo) {}
		public void hasNoParams() {}
		public void isPublic() {}
		protected void isNotPublic() {}
		public static void isStatic() {}
		public void isNotStatic() {}
		public abstract void isAbstract();
		public void isNotAbstract() {}
	}
	static ClassInfo e = ClassInfo.of(E.class);
	static ExecutableInfo
		e_deprecated = e.getPublicMethod(x -> x.hasName("deprecated")),
		e_notDeprecated = e.getPublicMethod(x -> x.hasName("notDeprecated")),
		e_hasParams = e.getPublicMethod(x -> x.hasName("hasParams")),
		e_hasStringParam = e.getPublicMethod(x -> x.hasName("hasStringParam")),
		e_hasNoParams = e.getPublicMethod(x -> x.hasName("hasNoParams")),
		e_isPublic = e.getPublicMethod(x -> x.hasName("isPublic")),
		e_isNotPublic = e.getMethod(x -> x.hasName("isNotPublic")),
		e_isStatic = e.getPublicMethod(x -> x.hasName("isStatic")),
		e_isNotStatic = e.getPublicMethod(x -> x.hasName("isNotStatic")),
		e_isAbstract = e.getPublicMethod(x -> x.hasName("isAbstract")),
		e_isNotAbstract = e.getPublicMethod(x -> x.hasName("isNotAbstract"))
	;

	@Test void isAll() {
		assertTrue(e_deprecated.isAll(DEPRECATED));
		assertTrue(e_notDeprecated.isAll(NOT_DEPRECATED));
		assertTrue(e_hasParams.isAll(HAS_PARAMS));
		assertTrue(e_hasNoParams.isAll(HAS_NO_PARAMS));
		assertTrue(e_isPublic.isAll(PUBLIC));
		assertTrue(e_isNotPublic.isAll(NOT_PUBLIC));
		assertTrue(e_isStatic.isAll(STATIC));
		assertTrue(e_isNotStatic.isAll(NOT_STATIC));
		assertTrue(e_isAbstract.isAll(ABSTRACT));
		assertTrue(e_isNotAbstract.isAll(NOT_ABSTRACT));

		assertFalse(e_deprecated.isAll(NOT_DEPRECATED));
		assertFalse(e_notDeprecated.isAll(DEPRECATED));
		assertFalse(e_hasParams.isAll(HAS_NO_PARAMS));
		assertFalse(e_hasNoParams.isAll(HAS_PARAMS));
		assertFalse(e_isPublic.isAll(NOT_PUBLIC));
		assertFalse(e_isNotPublic.isAll(PUBLIC));
		assertFalse(e_isStatic.isAll(NOT_STATIC));
		assertFalse(e_isNotStatic.isAll(STATIC));
		assertFalse(e_isAbstract.isAll(NOT_ABSTRACT));
		assertFalse(e_isNotAbstract.isAll(ABSTRACT));
	}

	@Test void isAll_invalidFlag() {
		assertThrowsWithMessage(RuntimeException.class, "Invalid flag for executable: TRANSIENT", ()->e_deprecated.isAll(TRANSIENT));
	}

	@Test void isAny() {
		assertTrue(e_deprecated.isAny(DEPRECATED));
		assertTrue(e_notDeprecated.isAny(NOT_DEPRECATED));
		assertTrue(e_hasParams.isAny(HAS_PARAMS));
		assertTrue(e_hasNoParams.isAny(HAS_NO_PARAMS));
		assertTrue(e_isPublic.isAny(PUBLIC));
		assertTrue(e_isNotPublic.isAny(NOT_PUBLIC));
		assertTrue(e_isStatic.isAny(STATIC));
		assertTrue(e_isNotStatic.isAny(NOT_STATIC));
		assertTrue(e_isAbstract.isAny(ABSTRACT));
		assertTrue(e_isNotAbstract.isAny(NOT_ABSTRACT));

		assertFalse(e_deprecated.isAny(NOT_DEPRECATED));
		assertFalse(e_notDeprecated.isAny(DEPRECATED));
		assertFalse(e_hasParams.isAny(HAS_NO_PARAMS));
		assertFalse(e_hasNoParams.isAny(HAS_PARAMS));
		assertFalse(e_isPublic.isAny(NOT_PUBLIC));
		assertFalse(e_isNotPublic.isAny(PUBLIC));
		assertFalse(e_isStatic.isAny(NOT_STATIC));
		assertFalse(e_isNotStatic.isAny(STATIC));
		assertFalse(e_isAbstract.isAny(NOT_ABSTRACT));
		assertFalse(e_isNotAbstract.isAny(ABSTRACT));
	}

	@Test void isAny_invalidFlag() {
		assertThrowsWithMessage(RuntimeException.class, "Invalid flag for executable: TRANSIENT", ()->e_deprecated.isAny(TRANSIENT));
	}

	@Test void hasArgs() {
		assertTrue(e_hasParams.hasParamTypes(int.class));
		assertFalse(e_hasParams.hasParamTypes(new Class[0]));
		assertFalse(e_hasParams.hasParamTypes(long.class));
		assertTrue(e_hasNoParams.hasParamTypes(new Class[0]));
		assertFalse(e_hasNoParams.hasParamTypes(long.class));
	}

	@Test void hasArgParents() {
		assertTrue(e_hasStringParam.hasMatchingParamTypes(String.class));
		assertFalse(e_hasStringParam.hasMatchingParamTypes(CharSequence.class));
		assertFalse(e_hasStringParam.hasMatchingParamTypes(StringBuilder.class));
		assertFalse(e_hasStringParam.hasMatchingParamTypes(new Class[0]));
		assertFalse(e_hasStringParam.hasMatchingParamTypes(String.class, String.class));
		assertFalse(e_hasStringParam.hasMatchingParamTypes(long.class));
	}

	@Test void hasFuzzyArgs() {
		assertTrue(e_hasParams.hasFuzzyParamTypes(int.class));
		assertTrue(e_hasParams.hasFuzzyParamTypes(int.class, long.class));
		assertFalse(e_hasParams.hasFuzzyParamTypes(long.class));
		assertTrue(e_hasNoParams.hasFuzzyParamTypes(new Class[0]));
		assertTrue(e_hasNoParams.hasFuzzyParamTypes(long.class));
	}

	@Test void isDeprecated() {
		assertTrue(e_deprecated.isDeprecated());
		assertFalse(e_notDeprecated.isDeprecated());
	}

	@Test void isNotDeprecated() {
		assertFalse(e_deprecated.isNotDeprecated());
		assertTrue(e_notDeprecated.isNotDeprecated());
	}

	@Test void isAbstract() {
		assertTrue(e_isAbstract.isAbstract());
		assertFalse(e_isNotAbstract.isAbstract());
	}

	@Test void isNotAbstract() {
		assertFalse(e_isAbstract.isNotAbstract());
		assertTrue(e_isNotAbstract.isNotAbstract());
	}

	@Test void isPublic() {
		assertTrue(e_isPublic.isPublic());
		assertFalse(e_isNotPublic.isPublic());
	}

	@Test void isNotPublic() {
		assertFalse(e_isPublic.isNotPublic());
		assertTrue(e_isNotPublic.isNotPublic());
	}

	@Test void isStatic() {
		assertTrue(e_isStatic.isStatic());
		assertFalse(e_isNotStatic.isStatic());
	}

	@Test void isNotStatic() {
		assertFalse(e_isStatic.isNotStatic());
		assertTrue(e_isNotStatic.isNotStatic());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Visibility
	//-----------------------------------------------------------------------------------------------------------------

	abstract static class F {
		public void isPublic() {}
		protected void isProtected() {}
		@SuppressWarnings("unused")
		private void isPrivate() {}
		void isDefault() {}
	}
	static ClassInfo f = ClassInfo.of(F.class);
	static ExecutableInfo
		f_isPublic = f.getPublicMethod(x -> x.hasName("isPublic")),
		f_isProtected = f.getMethod(x -> x.hasName("isProtected")),
		f_isPrivate = f.getMethod(x -> x.hasName("isPrivate")),
		f_isDefault = f.getMethod(x -> x.hasName("isDefault"));

	@Test void setAccessible() {
		assertDoesNotThrow(()->f_isPublic.accessible());
		assertDoesNotThrow(()->f_isProtected.accessible());
		assertDoesNotThrow(()->f_isPrivate.accessible());
		assertDoesNotThrow(()->f_isDefault.accessible());
	}

	@Test void isVisible() {
		assertTrue(f_isPublic.isVisible(Visibility.PUBLIC));
		assertTrue(f_isPublic.isVisible(Visibility.PROTECTED));
		assertTrue(f_isPublic.isVisible(Visibility.PRIVATE));
		assertTrue(f_isPublic.isVisible(Visibility.DEFAULT));

		assertFalse(f_isProtected.isVisible(Visibility.PUBLIC));
		assertTrue(f_isProtected.isVisible(Visibility.PROTECTED));
		assertTrue(f_isProtected.isVisible(Visibility.PRIVATE));
		assertTrue(f_isProtected.isVisible(Visibility.DEFAULT));

		assertFalse(f_isPrivate.isVisible(Visibility.PUBLIC));
		assertFalse(f_isPrivate.isVisible(Visibility.PROTECTED));
		assertTrue(f_isPrivate.isVisible(Visibility.PRIVATE));
		assertFalse(f_isPrivate.isVisible(Visibility.DEFAULT));

		assertFalse(f_isDefault.isVisible(Visibility.PUBLIC));
		assertFalse(f_isDefault.isVisible(Visibility.PROTECTED));
		assertTrue(f_isDefault.isVisible(Visibility.PRIVATE));
		assertTrue(f_isDefault.isVisible(Visibility.DEFAULT));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	static class X {
		public X() {}
		public X(String foo) {}  // NOSONAR
		public X(Map<String,Object> foo) {}  // NOSONAR
		public void foo(){}  // NOSONAR
		public void foo(String foo){}  // NOSONAR
		public void foo(Map<String,Object> foo){}  // NOSONAR
	}
	static ClassInfo x2 = ClassInfo.of(X.class);

	@Test void getFullName_method() {
		assertEquals("org.apache.juneau.common.reflect.ExecutableInfo_Test$X.foo()", x2.getPublicMethod(x -> x.hasName("foo") && x.hasNoParams()).getFullName());
		assertEquals("org.apache.juneau.common.reflect.ExecutableInfo_Test$X.foo(java.lang.String)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParamTypes(String.class)).getFullName());
		assertEquals("org.apache.juneau.common.reflect.ExecutableInfo_Test$X.foo(java.util.Map<java.lang.String,java.lang.Object>)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParamTypes(Map.class)).getFullName());
	}

	@Test void getFullName_constructor() {
		assertEquals("org.apache.juneau.common.reflect.ExecutableInfo_Test$X()", x2.getPublicConstructor(ConstructorInfo::hasNoParams).getFullName());
		assertEquals("org.apache.juneau.common.reflect.ExecutableInfo_Test$X(java.lang.String)", x2.getPublicConstructor(x -> x.hasParamTypes(String.class)).getFullName());
		assertEquals("org.apache.juneau.common.reflect.ExecutableInfo_Test$X(java.util.Map<java.lang.String,java.lang.Object>)", x2.getPublicConstructor(x -> x.hasParamTypes(Map.class)).getFullName());
	}

	@Test void getShortName_method() {
		assertEquals("foo()", x2.getPublicMethod(x -> x.hasName("foo") && x.hasNoParams()).getShortName());
		assertEquals("foo(String)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParamTypes(String.class)).getShortName());
		assertEquals("foo(Map)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParamTypes(Map.class)).getShortName());
	}

	@Test void getShortName_constructor() {
		assertEquals("X()", x2.getPublicConstructor(ConstructorInfo::hasNoParams).getShortName());
		assertEquals("X(String)", x2.getPublicConstructor(x -> x.hasParamTypes(String.class)).getShortName());
		assertEquals("X(Map)", x2.getPublicConstructor(x -> x.hasParamTypes(Map.class)).getShortName());
	}

	@Test void getSimpleName_method() {
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.hasNoParams()).getSimpleName());
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParamTypes(String.class)).getSimpleName());
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParamTypes(Map.class)).getSimpleName());
	}

	@Test void getSimpleName_constructor() {
		assertEquals("X", x2.getPublicConstructor(ConstructorInfo::hasNoParams).getSimpleName());
		assertEquals("X", x2.getPublicConstructor(x -> x.hasParamTypes(String.class)).getSimpleName());
		assertEquals("X", x2.getPublicConstructor(x -> x.hasParamTypes(Map.class)).getSimpleName());
	}
}