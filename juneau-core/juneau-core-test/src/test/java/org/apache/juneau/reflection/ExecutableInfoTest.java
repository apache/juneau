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

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

public class ExecutableInfoTest {

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
			if (t instanceof Iterable)
				return StreamSupport.stream(((Iterable<?>)t).spliterator(), false).map(this).collect(Collectors.joining(","));
			if (t.getClass().isArray())
				return StreamSupport.stream(ArrayUtils.toList(t, Object.class).spliterator(), false).map(this).collect(Collectors.joining(","));
			if (t instanceof Annotation)
				return t.toString().replaceAll("\\@[^\\$]*\\$(.*)", "@$1");
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof Package)
				return ((Package)t).getName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			if (t instanceof MethodInfo)
				return ((MethodInfo)t).getDeclaringClass().getSimpleName() + '.' + ((MethodInfo)t).getShortName();
			if (t instanceof ConstructorInfo)
				return ((ConstructorInfo)t).getShortName();
			if (t instanceof FieldInfo)
				return ((FieldInfo)t).getDeclaringClass().getSimpleName() + '.' + ((FieldInfo)t).getName();
			if (t instanceof AnnotationInfo)
				return apply(((AnnotationInfo<?>)t).getAnnotation());
			if (t instanceof ParamInfo)
				return apply(((ParamInfo)t).toString());
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	static class A {
		public A() {}
		public void foo() {}
	}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void isConstructor() {
		assertTrue(a.getPublicConstructor().isConstructor());
		assertFalse(a.getPublicMethod("foo").isConstructor());
	}

	@Test
	public void getDeclaringClass() {
		check("A", a.getPublicConstructor().getDeclaringClass());
		check("A", a.getPublicMethod("foo").getDeclaringClass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameters
	//-----------------------------------------------------------------------------------------------------------------

	static class B {
		public B() {}
		public B(String s) {}
		public void m() {}
		public int m(String s) { return 0; }
	}
	static ClassInfo b = ClassInfo.of(B.class);
	static ExecutableInfo
		b_c1=b.getPublicConstructor(),
		b_c2=b.getPublicConstructor(String.class),
		b_m1=b.getPublicMethod("m"),
		b_m2=b.getPublicMethod("m", String.class)
	;

	@Test
	public void getParamCount() {
		assertEquals(0, b_c1.getParamCount());
		assertEquals(1, b_c2.getParamCount());
		assertEquals(0, b_m1.getParamCount());
		assertEquals(1, b_m2.getParamCount());
	}

	@Test
	public void hasParams() {
		assertEquals(false, b_c1.hasParams());
		assertEquals(true, b_c2.hasParams());
		assertEquals(false, b_m1.hasParams());
		assertEquals(true, b_m2.hasParams());
	}

	@Test
	public void hasNoParams() {
		assertEquals(true, b_c1.hasNoParams());
		assertEquals(false, b_c2.hasNoParams());
		assertEquals(true, b_m1.hasNoParams());
		assertEquals(false, b_m2.hasNoParams());
	}

	@Test
	public void hasNumParams() {
		assertEquals(false, b_c1.hasNumParams(1));
		assertEquals(true, b_c2.hasNumParams(1));
		assertEquals(false, b_m1.hasNumParams(1));
		assertEquals(true, b_m2.hasNumParams(1));
	}

	@Test
	public void getParams() {
		check("", b_c1.getParams());
		check("B[0]", b_c2.getParams());
		check("", b_m1.getParams());
		check("m[0]", b_m2.getParams());
	}

	@Test
	public void getParams_twice() {
		check("", b_c1.getParams());
		check("", b_c1.getParams());
	}

	@Test
	public void getParam() {
		check("B[0]", b_c2.getParam(0));
		check("m[0]", b_m2.getParam(0));
	}

	@Test
	public void getParam_nocache() {
		ClassInfo b = ClassInfo.of(B.class);
		check("B[0]", b.getPublicConstructor(String.class).getParam(0));
		check("m[0]", b.getPublicMethod("m", String.class).getParam(0));
	}

	@Test
	public void getParam_indexOutOfBounds() {
		try {
			b_c1.getParam(0);
		} catch (IndexOutOfBoundsException e) {
			assertEquals("Invalid index '0'.  No parameters.", e.getLocalizedMessage());
		}
		try {
			b_c2.getParam(-1);
		} catch (IndexOutOfBoundsException e) {
			assertEquals("Invalid index '-1'.  Parameter count: 1", e.getLocalizedMessage());
		}
		try {
			b_c2.getParam(1);
		} catch (IndexOutOfBoundsException e) {
			assertEquals("Invalid index '1'.  Parameter count: 1", e.getLocalizedMessage());
		}
	}

	@Test
	public void getParam_indexOutOfBounds_noCache() {
		ClassInfo b = ClassInfo.of(B.class);
		try {
			b.getPublicConstructor().getParam(0);
		} catch (IndexOutOfBoundsException e) {
			assertEquals("Invalid index '0'.  No parameters.", e.getLocalizedMessage());
		}
		try {
			b.getPublicConstructor(String.class).getParam(-1);
		} catch (IndexOutOfBoundsException e) {
			assertEquals("Invalid index '-1'.  Parameter count: 1", e.getLocalizedMessage());
		}
		try {
			b.getPublicConstructor(String.class).getParam(1);
		} catch (IndexOutOfBoundsException e) {
			assertEquals("Invalid index '1'.  Parameter count: 1", e.getLocalizedMessage());
		}
	}

	@Test
	public void getParamTypes() {
		check("", b_c1.getParamTypes());
		check("String", b_c2.getParamTypes());
		check("", b_m1.getParamTypes());
		check("String", b_m2.getParamTypes());
	}

	@Test
	public void getParamTypes_twice() {
		check("", b_c1.getParamTypes());
		check("", b_c1.getParamTypes());
	}

	static enum B1 {
		FOO;
	}

	@Test
	public void getParamTypes_enum() {
		ClassInfo b1 = ClassInfo.of(B1.class);
		check("B1(String,int)", b1.getDeclaredConstructors());
		check("String,int", b1.getDeclaredConstructors().get(0).getParamTypes());
	}

	@Test
	public void getParamType() {
		check("String", b_c2.getParamType(0));
		check("String", b_m2.getParamType(0));
	}

	@Test
	public void getRawParamTypes() {
		check("", b_c1.getRawParamTypes());
		check("String", b_c2.getRawParamTypes());
		check("", b_m1.getRawParamTypes());
		check("String", b_m2.getRawParamTypes());
	}

	@Test
	public void getRawParamType() {
		check("String", b_c2.getRawParamType(0));
		check("String", b_m2.getRawParamType(0));
	}

	@Test
	public void getRawGenericParamType() {
		check("String", b_c2.getRawGenericParamType(0));
		check("String", b_m2.getRawGenericParamType(0));
	}

	@Test
	public void getRawGenericParamTypes() {
		check("", b_c1.getRawGenericParamTypes());
		check("String", b_c2.getRawGenericParamTypes());
		check("", b_m1.getRawGenericParamTypes());
		check("String", b_m2.getRawGenericParamTypes());
	}

	@Test
	public void getRawParameters() {
		check("", b_c1.getRawParameters());
		assertTrue(b_c2.getRawParameters()[0].toString().startsWith("java.lang.String "));
		check("", b_m1.getRawParameters());
		assertTrue(b_m2.getRawParameters()[0].toString().startsWith("java.lang.String "));
	}

	@Test
	public void getRawParameter() {
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
		public C(@CA String foo) {}
		public @CA C(int bar) {}
		public void m() {}
		public void m(@CA String foo) {}
		public @CA void m(int bar) {}
	}
	static ClassInfo c = ClassInfo.of(C.class);
	static ExecutableInfo
		c_c1=c.getPublicConstructor(),
		c_c2=c.getPublicConstructor(String.class),
		c_c3=c.getPublicConstructor(int.class),
		c_m1=c.getPublicMethod("m"),
		c_m2=c.getPublicMethod("m", String.class),
		c_m3=c.getPublicMethod("m", int.class)
	;

	@Test
	public void getParameterAnnotations() {
		check("", c_c1.getParameterAnnotations());
		check("@CA()", c_c2.getParameterAnnotations());
		check("", c_c3.getParameterAnnotations());
		check("", c_m1.getParameterAnnotations());
		check("@CA()", c_m2.getParameterAnnotations());
		check("", c_m3.getParameterAnnotations());
	}

	@Test
	public void getParameterAnnotations_atIndex() {
		check("@CA()", c_c2.getParameterAnnotations(0));
		check("@CA()", c_m2.getParameterAnnotations(0));
	}

	@Test
	public void hasAnnotation() {
		assertFalse(c_c1.hasAnnotation(CA.class));
		assertFalse(c_c2.hasAnnotation(CA.class));
		assertTrue(c_c3.hasAnnotation(CA.class));
		assertFalse(c_m1.hasAnnotation(CA.class));
		assertFalse(c_m2.hasAnnotation(CA.class));
		assertTrue(c_m3.hasAnnotation(CA.class));
	}

	@Test
	public void getAnnotation() {
		check(null, c_c1.getAnnotation(CA.class));
		check(null, c_c2.getAnnotation(CA.class));
		check("@CA()", c_c3.getAnnotation(CA.class));
		check(null, c_m1.getAnnotation(CA.class));
		check(null, c_m2.getAnnotation(CA.class));
		check("@CA()", c_m3.getAnnotation(CA.class));
	}

	@Test
	public void getAnnotation_nullArg() {
		check(null, c_c3.getAnnotation(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Exceptions
	//-----------------------------------------------------------------------------------------------------------------

	static class D {
		public D() throws IOException {}
		public void m() throws IOException {}
	}
	static ClassInfo d = ClassInfo.of(D.class);
	static ExecutableInfo
		d_c=d.getPublicConstructor(),
		d_m=d.getPublicMethod("m")
	;

	@Test
	public void getExceptionTypes() {
		check("IOException", d_c.getExceptionTypes());
		check("IOException", d_m.getExceptionTypes());
	}

	@Test
	public void getExceptionTypes_twice() {
		check("IOException", d_c.getExceptionTypes());
		check("IOException", d_c.getExceptionTypes());
	}

	@Test
	public void getRawExceptionTypes() {
		check("IOException", d_c.getRawExceptionTypes());
		check("IOException", d_m.getRawExceptionTypes());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void isAll() {
	}

	@Test
	public void isAny() {
	}

	@Test
	public void hasArgs() {
	}

	@Test
	public void hasFuzzyArgs() {
	}

	@Test
	public void isDeprecated() {
	}

	@Test
	public void isNotDeprecated() {
	}

	@Test
	public void isAbstract() {
	}

	@Test
	public void isNotAbstract() {
	}

	@Test
	public void isPublic() {
	}

	@Test
	public void isNotPublic() {
	}

	@Test
	public void isStatic() {
	}

	@Test
	public void isNotStatic() {
	}



















	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	static class X {
		public X() {}
		public X(String foo) {}
		public X(Map<String,Object> foo) {}
		public void foo(){}
		public void foo(String foo){}
		public void foo(Map<String,Object> foo){}
	}
	ClassInfo x = ClassInfo.of(X.class);

	@Test
	public void getFullName_method() {
		assertEquals("org.apache.juneau.reflection.ExecutableInfoTest$X.foo()", x.getPublicMethod("foo").getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutableInfoTest$X.foo(java.lang.String)", x.getPublicMethod("foo", String.class).getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutableInfoTest$X.foo(java.util.Map<java.lang.String,java.lang.Object>)", x.getPublicMethod("foo", Map.class).getFullName());
	}

	@Test
	public void getFullName_constructor() {
		assertEquals("org.apache.juneau.reflection.ExecutableInfoTest$X()", x.getPublicConstructor().getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutableInfoTest$X(java.lang.String)", x.getPublicConstructor(String.class).getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutableInfoTest$X(java.util.Map<java.lang.String,java.lang.Object>)", x.getPublicConstructor(Map.class).getFullName());
	}

	@Test
	public void getShortName_method() {
		assertEquals("foo()", x.getPublicMethod("foo").getShortName());
		assertEquals("foo(String)", x.getPublicMethod("foo", String.class).getShortName());
		assertEquals("foo(Map)", x.getPublicMethod("foo", Map.class).getShortName());
	}

	@Test
	public void getShortName_constructor() {
		assertEquals("X()", x.getPublicConstructor().getShortName());
		assertEquals("X(String)", x.getPublicConstructor(String.class).getShortName());
		assertEquals("X(Map)", x.getPublicConstructor(Map.class).getShortName());
	}

	@Test
	public void getSimpleName_method() {
		assertEquals("foo", x.getPublicMethod("foo").getSimpleName());
		assertEquals("foo", x.getPublicMethod("foo", String.class).getSimpleName());
		assertEquals("foo", x.getPublicMethod("foo", Map.class).getSimpleName());
	}

	@Test
	public void getSimpleName_constructor() {
		assertEquals("X", x.getPublicConstructor().getSimpleName());
		assertEquals("X", x.getPublicConstructor(String.class).getSimpleName());
		assertEquals("X", x.getPublicConstructor(Map.class).getSimpleName());
	}
}
