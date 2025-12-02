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
package org.apache.juneau.commons.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.reflect.ElementFlag.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
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
			if (t instanceof AnnotationInfo)
				return "@" + ((AnnotationInfo<?>)t).inner().annotationType().getSimpleName() + "()";
			if (t instanceof Annotation)
				return "@" + ((Annotation)t).annotationType().getSimpleName() + "()";
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getNameSimple();
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
		assertTrue(a.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().isConstructor());
		assertFalse(a.getPublicMethod(x -> x.hasName("foo")).get().isConstructor());
	}

	@Test void getDeclaringClass() {
		check("A", a.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getDeclaringClass());
		check("A", a.getPublicMethod(x -> x.hasName("foo")).get().getDeclaringClass());
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
		b_c1 = b.getPublicConstructor(cons -> cons.getParameterCount() == 0).get(),
		b_c2 = b.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get(),
		b_m1 = b.getPublicMethod(x -> x.hasName("m") && x.getParameterCount() == 0).get(),
		b_m2 = b.getPublicMethod(x -> x.hasName("m") && x.hasParameterTypes(String.class)).get()
	;

	@Test void getParamCount() {
		assertEquals(0, b_c1.getParameterCount());
		assertEquals(1, b_c2.getParameterCount());
		assertEquals(0, b_m1.getParameterCount());
		assertEquals(1, b_m2.getParameterCount());
	}

	@Test void hasParams() {
		assertEquals(false, b_c1.hasParameters());
		assertEquals(true, b_c2.hasParameters());
		assertEquals(false, b_m1.hasParameters());
		assertEquals(true, b_m2.hasParameters());
	}

	@Test void hasNoParams() {
		assertEquals(true, b_c1.getParameterCount() == 0);
		assertEquals(false, b_c2.getParameterCount() == 0);
		assertEquals(true, b_m1.getParameterCount() == 0);
		assertEquals(false, b_m2.getParameterCount() == 0);
	}

	@Test void hasNumParams() {
		assertEquals(false, b_c1.hasNumParameters(1));
		assertEquals(true, b_c2.hasNumParameters(1));
		assertEquals(false, b_m1.hasNumParameters(1));
		assertEquals(true, b_m2.hasNumParameters(1));
	}

	@Test void getParams() {
		check("", b_c1.getParameters());
		check("B[0]", b_c2.getParameters());
		check("", b_m1.getParameters());
		check("m[0]", b_m2.getParameters());
	}

	@Test void getParams_twice() {
		check("", b_c1.getParameters());
		check("", b_c1.getParameters());
	}

	@Test void getParam() {
		check("B[0]", b_c2.getParameter(0));
		check("m[0]", b_m2.getParameter(0));
	}

	@Test void getParam_nocache() {
		var b2 = ClassInfo.of(B.class);
		check("B[0]", b2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getParameter(0));
		check("m[0]", b2.getPublicMethod(x -> x.hasName("m") && x.hasParameterTypes(String.class)).get().getParameter(0));
	}

	@Test void getParam_indexOutOfBounds() {
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '0'.  No parameters.", ()->b_c1.getParameter(0));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '-1'.  Parameter count: 1", ()->b_c2.getParameter(-1));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '1'.  Parameter count: 1", ()->b_c2.getParameter(1));
	}

	@Test void getParam_indexOutOfBounds_noCache() {
		var b2 = ClassInfo.of(B.class);
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '0'.  No parameters.", ()->b2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getParameter(0));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '-1'.  Parameter count: 1", ()->b2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getParameter(-1));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '1'.  Parameter count: 1", ()->b2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getParameter(1));
	}

	@Test void getParamTypes() {
		check("", b_c1.getParameters().stream().map(ParameterInfo::getParameterType).toList());
		check("String", b_c2.getParameters().stream().map(ParameterInfo::getParameterType).toList());
		check("", b_m1.getParameters().stream().map(ParameterInfo::getParameterType).toList());
		check("String", b_m2.getParameters().stream().map(ParameterInfo::getParameterType).toList());
	}

	@Test void getParamTypes_twice() {
		check("", b_c1.getParameters().stream().map(ParameterInfo::getParameterType).toList());
		check("", b_c1.getParameters().stream().map(ParameterInfo::getParameterType).toList());
	}

	enum B1 {
		FOO;
	}

	@Test void getParamTypes_enum() {
	var b1 = ClassInfo.of(B1.class);
	check("B1(String,int)", b1.getDeclaredConstructors());
	check("String,int", b1.getDeclaredConstructors().get(0).getParameters().stream().map(ParameterInfo::getParameterType).toList());
}

	@Test void getParamType() {
		check("String", b_c2.getParameter(0).getParameterType());
		check("String", b_m2.getParameter(0).getParameterType());
	}

	@Test void getRawParamTypes() {
		check("", b_c1.getParameters().stream().map(p -> p.getParameterType().inner()).toList());
		check("String", b_c2.getParameters().stream().map(p -> p.getParameterType().inner()).toList());
		check("", b_m1.getParameters().stream().map(p -> p.getParameterType().inner()).toList());
		check("String", b_m2.getParameters().stream().map(p -> p.getParameterType().inner()).toList());
	}

	@Test void getRawParamType() {
		check("String", b_c2.getParameter(0).getParameterType().inner());
		check("String", b_m2.getParameter(0).getParameterType().inner());
	}

	@Test void getRawGenericParamType() {
		check("String", b_c2.getParameter(0).getParameterType().innerType());
		check("String", b_m2.getParameter(0).getParameterType().innerType());
	}

	@Test void getRawGenericParamTypes() {
		check("", b_c1.getParameters().stream().map(p -> p.getParameterType().innerType()).toList());
		check("String", b_c2.getParameters().stream().map(p -> p.getParameterType().innerType()).toList());
		check("", b_m1.getParameters().stream().map(p -> p.getParameterType().innerType()).toList());
		check("String", b_m2.getParameters().stream().map(p -> p.getParameterType().innerType()).toList());
	}

	@Test void getRawParameters() {
		check("", b_c1.getParameters());
		assertTrue(b_c2.getParameters().get(0).inner().toString().startsWith("java.lang.String "));
		check("", b_m1.getParameters());
		assertTrue(b_m2.getParameters().get(0).inner().toString().startsWith("java.lang.String "));
	}

	@Test void getRawParameter() {
		assertTrue(b_c2.getParameter(0).inner().toString().startsWith("java.lang.String "));
		assertTrue(b_m2.getParameter(0).inner().toString().startsWith("java.lang.String "));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@Documented
	@Target({PARAMETER,METHOD,java.lang.annotation.ElementType.CONSTRUCTOR})
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
		c_c1=c.getPublicConstructor(cons -> cons.getParameterCount() == 0).get(),
		c_c2=c.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get(),
		c_c3=c.getPublicConstructor(x -> x.hasParameterTypes(int.class)).get()
	;
	static MethodInfo
		c_m1=c.getPublicMethod(x -> x.hasName("m") && x.getParameterCount() == 0).get(),
		c_m2=c.getPublicMethod(x -> x.hasName("m") && x.hasParameterTypes(String.class)).get(),
		c_m3=c.getPublicMethod(x -> x.hasName("m") && x.hasParameterTypes(int.class)).get()
	;

	@Test void getParameterAnnotations() {
		check("", c_c1.getParameters().stream().map(p -> p.getAnnotations()).toArray());
		check("@CA()", c_c2.getParameters().stream().map(p -> p.getAnnotations()).toArray());
		check("", c_c3.getParameters().stream().map(p -> p.getAnnotations()).toArray());
		check("", c_m1.getParameters().stream().map(p -> p.getAnnotations()).toArray());
		check("@CA()", c_m2.getParameters().stream().map(p -> p.getAnnotations()).toArray());
		check("", c_m3.getParameters().stream().map(p -> p.getAnnotations()).toArray());
	}

	@Test void getParameterAnnotations_atIndex() {
		check("@CA()", c_c2.getParameter(0).getAnnotations());
		check("@CA()", c_m2.getParameter(0).getAnnotations());
	}

	@Test void hasAnnotation() {
		assertFalse(c_c1.hasAnnotation(CA.class));
		assertFalse(c_c2.hasAnnotation(CA.class));
		assertTrue(c_c3.hasAnnotation(CA.class));
		assertFalse(c_m1.hasAnnotation(CA.class));
		assertFalse(c_m2.hasAnnotation(CA.class));
		assertTrue(c_m3.hasAnnotation(CA.class));
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
		d_c=d.getPublicConstructor(cons -> cons.getParameterCount() == 0).get(),
		d_m=d.getPublicMethod(x -> x.hasName("m")).get()
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
		e_deprecated = e.getPublicMethod(x -> x.hasName("deprecated")).get(),
		e_notDeprecated = e.getPublicMethod(x -> x.hasName("notDeprecated")).get(),
		e_hasParams = e.getPublicMethod(x -> x.hasName("hasParams")).get(),
		e_hasStringParam = e.getPublicMethod(x -> x.hasName("hasStringParam")).get(),
		e_hasNoParams = e.getPublicMethod(x -> x.hasName("hasNoParams")).get(),
		e_isPublic = e.getPublicMethod(x -> x.hasName("isPublic")).get(),
		e_isNotPublic = e.getMethod(x -> x.hasName("isNotPublic")).get(),
		e_isStatic = e.getPublicMethod(x -> x.hasName("isStatic")).get(),
		e_isNotStatic = e.getPublicMethod(x -> x.hasName("isNotStatic")).get(),
		e_isAbstract = e.getPublicMethod(x -> x.hasName("isAbstract")).get(),
		e_isNotAbstract = e.getPublicMethod(x -> x.hasName("isNotAbstract")).get()
	;

	@Test void isAll() {
		assertTrue(e_deprecated.is(DEPRECATED));
		assertTrue(e_notDeprecated.is(NOT_DEPRECATED));
		assertTrue(e_hasParams.is(HAS_PARAMS));
		assertTrue(e_hasNoParams.is(HAS_NO_PARAMS));
		assertTrue(e_isPublic.is(PUBLIC));
		assertTrue(e_isNotPublic.is(NOT_PUBLIC));
		assertTrue(e_isStatic.is(STATIC));
		assertTrue(e_isNotStatic.is(NOT_STATIC));
		assertTrue(e_isAbstract.is(ABSTRACT));
		assertTrue(e_isNotAbstract.is(NOT_ABSTRACT));

		assertFalse(e_deprecated.is(NOT_DEPRECATED));
		assertFalse(e_notDeprecated.is(DEPRECATED));
		assertFalse(e_hasParams.is(HAS_NO_PARAMS));
		assertFalse(e_hasNoParams.is(HAS_PARAMS));
		assertFalse(e_isPublic.is(NOT_PUBLIC));
		assertFalse(e_isNotPublic.is(PUBLIC));
		assertFalse(e_isStatic.is(NOT_STATIC));
		assertFalse(e_isNotStatic.is(STATIC));
		assertFalse(e_isAbstract.is(NOT_ABSTRACT));
		assertFalse(e_isNotAbstract.is(ABSTRACT));
	}

	@Test void isAll_invalidFlag() {
		// TRANSIENT is a valid modifier flag but doesn't apply to executables
		// Should return false (executables can't be transient) but not throw exception
		assertFalse(e_deprecated.is(TRANSIENT));

		// CLASS is not a modifier flag and doesn't apply to executables, should throw exception
		assertThrowsWithMessage(RuntimeException.class, "Invalid flag for element: CLASS", () -> e_deprecated.is(ElementFlag.CLASS));
	}


	@Test void hasArgs() {
		assertTrue(e_hasParams.hasParameterTypes(int.class));
		assertFalse(e_hasParams.hasParameterTypes(new Class[0]));
		assertFalse(e_hasParams.hasParameterTypes(long.class));
		assertTrue(e_hasNoParams.hasParameterTypes(new Class[0]));
		assertFalse(e_hasNoParams.hasParameterTypes(long.class));
	}

	@Test void hasArgParents() {
		assertTrue(e_hasStringParam.hasParameterTypeParents(String.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(CharSequence.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(StringBuilder.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(new Class[0]));
		assertFalse(e_hasStringParam.hasParameterTypeParents(String.class, String.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(long.class));
	}

	@Test void hasFuzzyArgs() {
		assertTrue(e_hasParams.hasParameterTypesLenient(int.class));
		assertTrue(e_hasParams.hasParameterTypesLenient(int.class, long.class));
		assertFalse(e_hasParams.hasParameterTypesLenient(long.class));
		assertTrue(e_hasNoParams.hasParameterTypesLenient(new Class[0]));
		assertTrue(e_hasNoParams.hasParameterTypesLenient(long.class));
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
		f_isPublic = f.getPublicMethod(x -> x.hasName("isPublic")).get(),
		f_isProtected = f.getMethod(x -> x.hasName("isProtected")).get(),
		f_isPrivate = f.getMethod(x -> x.hasName("isPrivate")).get(),
		f_isDefault = f.getMethod(x -> x.hasName("isDefault")).get();

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
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X.foo()", x2.getPublicMethod(x -> x.hasName("foo") && x.getParameterCount() == 0).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X.foo(java.lang.String)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(String.class)).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X.foo(java.util.Map<java.lang.String,java.lang.Object>)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(Map.class)).get().getFullName());
	}

	@Test void getFullName_constructor() {
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X()", x2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X(java.lang.String)", x2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X(java.util.Map<java.lang.String,java.lang.Object>)", x2.getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get().getFullName());
	}

	@Test void getShortName_method() {
		assertEquals("foo()", x2.getPublicMethod(x -> x.hasName("foo") && x.getParameterCount() == 0).get().getShortName());
		assertEquals("foo(String)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(String.class)).get().getShortName());
		assertEquals("foo(Map)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(Map.class)).get().getShortName());
	}

	@Test void getShortName_constructor() {
		assertEquals("X()", x2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getShortName());
		assertEquals("X(String)", x2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getShortName());
		assertEquals("X(Map)", x2.getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get().getShortName());
	}

	@Test void getSimpleName_method() {
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.getParameterCount() == 0).get().getSimpleName());
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(String.class)).get().getSimpleName());
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(Map.class)).get().getSimpleName());
	}

	@Test void getSimpleName_constructor() {
		assertEquals("X", x2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getSimpleName());
		assertEquals("X", x2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getSimpleName());
		assertEquals("X", x2.getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get().getSimpleName());
	}
}