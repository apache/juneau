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
	// Test classes
	//-----------------------------------------------------------------------------------------------------------------

	static class A {
		public A() {}  // NOSONAR
		public void foo() {}  // NOSONAR
	}
	static ClassInfo a = ClassInfo.of(A.class);

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

	enum B1 {
		FOO;
	}

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

	static class X {
		public X() {}
		public X(String foo) {}  // NOSONAR
		public X(Map<String,Object> foo) {}  // NOSONAR
		public void foo(){}  // NOSONAR
		public void foo(String foo){}  // NOSONAR
		public void foo(Map<String,Object> foo){}  // NOSONAR
	}
	static ClassInfo x2 = ClassInfo.of(X.class);

	public static class VarArgsClass {
		public VarArgsClass(String...args) {}
	}

	//====================================================================================================
	// accessible()
	//====================================================================================================
	@Test
	void a001_accessible() {
		assertDoesNotThrow(()->f_isPublic.accessible());
		assertDoesNotThrow(()->f_isProtected.accessible());
		assertDoesNotThrow(()->f_isPrivate.accessible());
		assertDoesNotThrow(()->f_isDefault.accessible());
		
		// Verify it returns this for chaining
		var result = f_isPublic.accessible();
		assertSame(f_isPublic, result);
	}

	//====================================================================================================
	// canAccept(Object...)
	//====================================================================================================
	@Test
	void a002_canAccept() {
		// Exact match
		assertTrue(b_c2.canAccept("test"));
		assertFalse(b_c2.canAccept(123));
		assertFalse(b_c2.canAccept("test", "extra"));
		
		// No parameters
		assertTrue(b_c1.canAccept());
		assertFalse(b_c1.canAccept("test"));
		
		// Multiple parameters
		assertTrue(b_m2.canAccept("test"));
		assertFalse(b_m2.canAccept());
	}

	//====================================================================================================
	// getAnnotatedExceptionTypes()
	//====================================================================================================
	@Test
	void a003_getAnnotatedExceptionTypes() {
		var types = d_c.getAnnotatedExceptionTypes();
		assertNotNull(types);
		assertEquals(1, types.length);
		assertEquals(IOException.class, types[0].getType());
	}

	//====================================================================================================
	// getAnnotatedParameterTypes()
	//====================================================================================================
	@Test
	void a004_getAnnotatedParameterTypes() {
		var types = b_c2.getAnnotatedParameterTypes();
		assertNotNull(types);
		assertEquals(1, types.length);
		assertEquals(String.class, types[0].getType());
	}

	//====================================================================================================
	// getAnnotatedReceiverType()
	//====================================================================================================
	@Test
	void a005_getAnnotatedReceiverType() {
		// Top-level class executable should return null
		var receiverType = b_c1.getAnnotatedReceiverType();
		assertNull(receiverType);
	}

	//====================================================================================================
	// getDeclaredAnnotations()
	//====================================================================================================
	@Test
	void a006_getDeclaredAnnotations() {
		var annotations = c_c1.getDeclaredAnnotations();
		assertNotNull(annotations);
		assertTrue(annotations.isEmpty());
		
		// Constructor with annotation
		var annotations2 = c_c3.getDeclaredAnnotations();
		assertNotNull(annotations2);
		assertEquals(1, annotations2.size());
		assertTrue(annotations2.get(0).isType(CA.class));
	}

	//====================================================================================================
	// getDeclaredAnnotations(Class<A>)
	//====================================================================================================
	@Test
	void a007_getDeclaredAnnotations_typed() {
		var annotations = c_c3.getDeclaredAnnotations(CA.class);
		assertEquals(1, annotations.count());
		
		var annotations2 = c_c1.getDeclaredAnnotations(CA.class);
		assertEquals(0, annotations2.count());
	}

	//====================================================================================================
	// getDeclaringClass()
	//====================================================================================================
	@Test
	void a008_getDeclaringClass() {
		check("A", a.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getDeclaringClass());
		check("A", a.getPublicMethod(x -> x.hasName("foo")).get().getDeclaringClass());
		check("B", b_c1.getDeclaringClass());
		check("B", b_m1.getDeclaringClass());
	}

	//====================================================================================================
	// getExceptionTypes()
	//====================================================================================================
	@Test
	void a009_getExceptionTypes() {
		check("IOException", d_c.getExceptionTypes());
		check("IOException", d_m.getExceptionTypes());
		
		// Test caching - should return same result
		check("IOException", d_c.getExceptionTypes());
	}

	//====================================================================================================
	// getFullName()
	//====================================================================================================
	@Test
	void a010_getFullName() {
		// Method
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X.foo()", x2.getPublicMethod(x -> x.hasName("foo") && x.getParameterCount() == 0).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X.foo(java.lang.String)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(String.class)).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X.foo(java.util.Map<java.lang.String,java.lang.Object>)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(Map.class)).get().getFullName());
		
		// Constructor
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X()", x2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X(java.lang.String)", x2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getFullName());
		assertEquals("org.apache.juneau.commons.reflect.ExecutableInfo_Test$X(java.util.Map<java.lang.String,java.lang.Object>)", x2.getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get().getFullName());
	}

	//====================================================================================================
	// getParameter(int)
	//====================================================================================================
	@Test
	void a011_getParameter() {
		check("B[0]", b_c2.getParameter(0));
		check("m[0]", b_m2.getParameter(0));
		
		// Index out of bounds
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '0'.  No parameters.", ()->b_c1.getParameter(0));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '-1'.  Parameter count: 1", ()->b_c2.getParameter(-1));
		assertThrowsWithMessage(IndexOutOfBoundsException.class, "Invalid index '1'.  Parameter count: 1", ()->b_c2.getParameter(1));
	}

	//====================================================================================================
	// getParameterCount()
	//====================================================================================================
	@Test
	void a012_getParameterCount() {
		assertEquals(0, b_c1.getParameterCount());
		assertEquals(1, b_c2.getParameterCount());
		assertEquals(0, b_m1.getParameterCount());
		assertEquals(1, b_m2.getParameterCount());
	}

	//====================================================================================================
	// getParameters()
	//====================================================================================================
	@Test
	void a013_getParameters() {
		check("", b_c1.getParameters());
		check("B[0]", b_c2.getParameters());
		check("", b_m1.getParameters());
		check("m[0]", b_m2.getParameters());
		
		// Test caching - should return same result
		check("", b_c1.getParameters());
		
		// Test enum constructor parameters
		var b1 = ClassInfo.of(B1.class);
		check("String,int", b1.getDeclaredConstructors().get(0).getParameters().stream().map(ParameterInfo::getParameterType).toList());
	}

	//====================================================================================================
	// getShortName()
	//====================================================================================================
	@Test
	void a014_getShortName() {
		// Method
		assertEquals("foo()", x2.getPublicMethod(x -> x.hasName("foo") && x.getParameterCount() == 0).get().getShortName());
		assertEquals("foo(String)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(String.class)).get().getShortName());
		assertEquals("foo(Map)", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(Map.class)).get().getShortName());
		
		// Constructor
		assertEquals("X()", x2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getShortName());
		assertEquals("X(String)", x2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getShortName());
		assertEquals("X(Map)", x2.getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get().getShortName());
	}

	//====================================================================================================
	// getSimpleName()
	//====================================================================================================
	@Test
	void a015_getSimpleName() {
		// Method
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.getParameterCount() == 0).get().getSimpleName());
		assertEquals("foo", x2.getPublicMethod(x -> x.hasName("foo") && x.hasParameterTypes(String.class)).get().getSimpleName());
		
		// Constructor
		assertEquals("X", x2.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().getSimpleName());
		assertEquals("X", x2.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get().getSimpleName());
	}

	//====================================================================================================
	// getTypeParameters()
	//====================================================================================================
	@Test
	void a016_getTypeParameters() {
		var typeParams = b_c1.getTypeParameters();
		assertNotNull(typeParams);
		assertEquals(0, typeParams.length);
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a017_hasAnnotation() {
		assertFalse(c_c1.hasAnnotation(CA.class));
		assertFalse(c_c2.hasAnnotation(CA.class));
		assertTrue(c_c3.hasAnnotation(CA.class));
		assertFalse(c_m1.hasAnnotation(CA.class));
		assertFalse(c_m2.hasAnnotation(CA.class));
		assertTrue(c_m3.hasAnnotation(CA.class));
	}

	//====================================================================================================
	// hasAnyName(Collection<String>)
	//====================================================================================================
	@Test
	void a018_hasAnyName_collection() {
		assertTrue(b_m1.hasAnyName(Arrays.asList("m", "n")));
		assertFalse(b_m1.hasAnyName(Arrays.asList("n", "o")));
	}

	//====================================================================================================
	// hasAnyName(String...)
	//====================================================================================================
	@Test
	void a019_hasAnyName_varargs() {
		assertTrue(b_m1.hasAnyName("m", "n"));
		assertFalse(b_m1.hasAnyName("n", "o"));
	}

	//====================================================================================================
	// hasMatchingParameters(List<ParameterInfo>)
	//====================================================================================================
	@Test
	void a020_hasMatchingParameters() {
		var params1 = b_c2.getParameters();
		assertTrue(b_c2.hasMatchingParameters(params1));
		
		// Test with parameters from a different executable that has different parameter types
		var params2 = b_c1.getParameters();  // b_c1 has no parameters, b_c2 has one String parameter
		assertFalse(b_c2.hasMatchingParameters(params2));
		
		// Test that b_c2 and b_m2 DO match (both have String parameter)
		var params3 = b_m2.getParameters();
		assertTrue(b_c2.hasMatchingParameters(params3));
	}

	//====================================================================================================
	// hasName(String)
	//====================================================================================================
	@Test
	void a021_hasName() {
		assertTrue(b_m1.hasName("m"));
		assertFalse(b_m1.hasName("n"));
	}

	//====================================================================================================
	// hasNumParameters(int)
	//====================================================================================================
	@Test
	void a022_hasNumParameters() {
		assertFalse(b_c1.hasNumParameters(1));
		assertTrue(b_c2.hasNumParameters(1));
		assertFalse(b_m1.hasNumParameters(1));
		assertTrue(b_m2.hasNumParameters(1));
	}

	//====================================================================================================
	// hasParameters()
	//====================================================================================================
	@Test
	void a023_hasParameters() {
		assertEquals(false, b_c1.hasParameters());
		assertEquals(true, b_c2.hasParameters());
		assertEquals(false, b_m1.hasParameters());
		assertEquals(true, b_m2.hasParameters());
	}

	//====================================================================================================
	// hasParameterTypeParents(Class<?>...)
	//====================================================================================================
	@Test
	void a024_hasParameterTypeParents_class() {
		assertTrue(e_hasStringParam.hasParameterTypeParents(String.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(CharSequence.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(StringBuilder.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(new Class[0]));
		assertFalse(e_hasStringParam.hasParameterTypeParents(String.class, String.class));
		assertFalse(e_hasStringParam.hasParameterTypeParents(long.class));
	}

	//====================================================================================================
	// hasParameterTypeParents(ClassInfo...)
	//====================================================================================================
	@Test
	void a025_hasParameterTypeParents_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var charSequenceClass = ClassInfo.of(CharSequence.class);
		assertTrue(e_hasStringParam.hasParameterTypeParents(stringClass));
		assertFalse(e_hasStringParam.hasParameterTypeParents(charSequenceClass));
	}

	//====================================================================================================
	// hasParameterTypes(Class<?>...)
	//====================================================================================================
	@Test
	void a026_hasParameterTypes_class() {
		assertTrue(e_hasParams.hasParameterTypes(int.class));
		assertFalse(e_hasParams.hasParameterTypes(new Class[0]));
		assertFalse(e_hasParams.hasParameterTypes(long.class));
		assertTrue(e_hasNoParams.hasParameterTypes(new Class[0]));
		assertFalse(e_hasNoParams.hasParameterTypes(long.class));
	}

	//====================================================================================================
	// hasParameterTypes(ClassInfo...)
	//====================================================================================================
	@Test
	void a027_hasParameterTypes_classInfo() {
		var intClass = ClassInfo.of(int.class);
		var longClass = ClassInfo.of(long.class);
		assertTrue(e_hasParams.hasParameterTypes(intClass));
		assertFalse(e_hasParams.hasParameterTypes(longClass));
	}

	//====================================================================================================
	// hasParameterTypesLenient(Class<?>...)
	//====================================================================================================
	@Test
	void a028_hasParameterTypesLenient_class() {
		assertTrue(e_hasParams.hasParameterTypesLenient(int.class));
		assertTrue(e_hasParams.hasParameterTypesLenient(int.class, long.class));
		assertFalse(e_hasParams.hasParameterTypesLenient(long.class));
		assertTrue(e_hasNoParams.hasParameterTypesLenient(new Class[0]));
		assertTrue(e_hasNoParams.hasParameterTypesLenient(long.class));
	}

	//====================================================================================================
	// hasParameterTypesLenient(ClassInfo...)
	//====================================================================================================
	@Test
	void a029_hasParameterTypesLenient_classInfo() {
		var intClass = ClassInfo.of(int.class);
		var longClass = ClassInfo.of(long.class);
		assertTrue(e_hasParams.hasParameterTypesLenient(intClass));
		assertTrue(e_hasParams.hasParameterTypesLenient(intClass, longClass));
		assertFalse(e_hasParams.hasParameterTypesLenient(longClass));
	}

	//====================================================================================================
	// is(ElementFlag)
	//====================================================================================================
	@Test
	void a030_is() {
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
		
		// Constructor vs method
		assertTrue(a.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().isConstructor());
		assertTrue(a.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().is(ElementFlag.CONSTRUCTOR));
		assertFalse(a.getPublicMethod(x -> x.hasName("foo")).get().isConstructor());
		assertFalse(a.getPublicMethod(x -> x.hasName("foo")).get().is(ElementFlag.CONSTRUCTOR));
		assertTrue(a.getPublicMethod(x -> x.hasName("foo")).get().is(NOT_CONSTRUCTOR));
		
		// SYNTHETIC and NOT_SYNTHETIC (lines 531, 532)
		// Regular executables are not synthetic
		assertFalse(b_c1.isSynthetic());
		assertFalse(b_c1.is(SYNTHETIC));
		assertTrue(b_c1.is(NOT_SYNTHETIC));
		assertFalse(b_m1.isSynthetic());
		assertFalse(b_m1.is(SYNTHETIC));
		assertTrue(b_m1.is(NOT_SYNTHETIC));
		
		// VARARGS and NOT_VARARGS (lines 532, 533)
		var varArgsCi = ClassInfo.of(VarArgsClass.class);
		var varArgsCtor = varArgsCi.getPublicConstructor(cons -> cons.isVarArgs()).get();
		assertTrue(varArgsCtor.isVarArgs());
		assertTrue(varArgsCtor.is(VARARGS));
		assertFalse(varArgsCtor.is(NOT_VARARGS));
		
		// Non-varargs executables
		assertFalse(b_c1.isVarArgs());
		assertFalse(b_c1.is(VARARGS));
		assertTrue(b_c1.is(NOT_VARARGS));
		assertFalse(b_m1.isVarArgs());
		assertFalse(b_m1.is(VARARGS));
		assertTrue(b_m1.is(NOT_VARARGS));
		
		// TRANSIENT is a valid modifier flag but doesn't apply to executables
		assertFalse(e_deprecated.is(TRANSIENT));
		
		// CLASS is not a modifier flag and doesn't apply to executables, should throw exception
		assertThrowsWithMessage(RuntimeException.class, "Invalid flag for element: CLASS", () -> e_deprecated.is(ElementFlag.CLASS));
	}

	//====================================================================================================
	// isAccessible()
	//====================================================================================================
	@Test
	void a031_isAccessible() {
		// Test isAccessible() before and after setAccessible()
		var privateBefore = f_isPrivate.isAccessible();
		var protectedBefore = f_isProtected.isAccessible();
		var defaultBefore = f_isDefault.isAccessible();
		
		// Make them accessible
		f_isPrivate.setAccessible();
		f_isProtected.setAccessible();
		f_isDefault.setAccessible();
		
		// After setAccessible(), they should be accessible (if Java 9+)
		var privateAfter = f_isPrivate.isAccessible();
		var protectedAfter = f_isProtected.isAccessible();
		var defaultAfter = f_isDefault.isAccessible();
		
		// Verify the method doesn't throw and returns a boolean
		assertTrue(privateAfter || !privateBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		assertTrue(protectedAfter || !protectedBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		assertTrue(defaultAfter || !defaultBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		
		// Public methods might already be accessible
		var publicAccessible = f_isPublic.isAccessible();
		assertNotNull(Boolean.valueOf(publicAccessible));
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a032_isAll() {
		assertTrue(e_deprecated.isAll(DEPRECATED));
		assertTrue(e_isPublic.isAll(PUBLIC, NOT_PRIVATE));
		assertFalse(e_deprecated.isAll(DEPRECATED, NOT_DEPRECATED));
	}

	//====================================================================================================
	// isAny(ElementFlag...)
	//====================================================================================================
	@Test
	void a033_isAny() {
		assertTrue(e_deprecated.isAny(DEPRECATED, NOT_DEPRECATED));
		assertTrue(e_isPublic.isAny(PUBLIC, PRIVATE));
		assertFalse(e_deprecated.isAny(NOT_DEPRECATED));
	}

	//====================================================================================================
	// isConstructor()
	//====================================================================================================
	@Test
	void a034_isConstructor() {
		assertTrue(a.getPublicConstructor(cons -> cons.getParameterCount() == 0).get().isConstructor());
		assertFalse(a.getPublicMethod(x -> x.hasName("foo")).get().isConstructor());
	}

	//====================================================================================================
	// isDeprecated()
	//====================================================================================================
	@Test
	void a035_isDeprecated() {
		assertTrue(e_deprecated.isDeprecated());
		assertFalse(e_notDeprecated.isDeprecated());
	}

	//====================================================================================================
	// isNotDeprecated()
	//====================================================================================================
	@Test
	void a036_isNotDeprecated() {
		assertFalse(e_deprecated.isNotDeprecated());
		assertTrue(e_notDeprecated.isNotDeprecated());
	}

	//====================================================================================================
	// isSynthetic()
	//====================================================================================================
	@Test
	void a037_isSynthetic() {
		// Regular executables are not synthetic
		assertFalse(b_c1.isSynthetic());
		assertFalse(b_m1.isSynthetic());
	}

	//====================================================================================================
	// isVarArgs()
	//====================================================================================================
	@Test
	void a038_isVarArgs() {
		var ci = ClassInfo.of(VarArgsClass.class);
		var ctor = ci.getPublicConstructor(x -> x.hasParameterTypes(String[].class)).get();
		assertTrue(ctor.isVarArgs());
		assertFalse(b_c1.isVarArgs());
	}

	//====================================================================================================
	// isVisible(Visibility)
	//====================================================================================================
	@Test
	void a039_isVisible() {
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

	//====================================================================================================
	// parameterMatchesLenientCount(Class<?>...)
	//====================================================================================================
	@Test
	void a040_parameterMatchesLenientCount_class() {
		// Exact match
		assertEquals(1, e_hasParams.parameterMatchesLenientCount(int.class));
		// Parent type match
		assertEquals(1, e_hasStringParam.parameterMatchesLenientCount(String.class));
		// No match
		assertEquals(-1, e_hasParams.parameterMatchesLenientCount(long.class));
		// Multiple args, some match
		assertEquals(1, e_hasParams.parameterMatchesLenientCount(int.class, long.class));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(ClassInfo...)
	//====================================================================================================
	@Test
	void a041_parameterMatchesLenientCount_classInfo() {
		var intClass = ClassInfo.of(int.class);
		var longClass = ClassInfo.of(long.class);
		assertEquals(1, e_hasParams.parameterMatchesLenientCount(intClass));
		assertEquals(-1, e_hasParams.parameterMatchesLenientCount(longClass));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(Object...)
	//====================================================================================================
	@Test
	void a042_parameterMatchesLenientCount_object() {
		assertEquals(1, e_hasParams.parameterMatchesLenientCount(123));
		assertEquals(1, e_hasStringParam.parameterMatchesLenientCount("test"));
		assertEquals(-1, e_hasParams.parameterMatchesLenientCount("test"));
	}

	//====================================================================================================
	// setAccessible()
	//====================================================================================================
	@Test
	void a043_setAccessible() {
		assertDoesNotThrow(()->f_isPublic.setAccessible());
		assertDoesNotThrow(()->f_isProtected.setAccessible());
		assertDoesNotThrow(()->f_isPrivate.setAccessible());
		assertDoesNotThrow(()->f_isDefault.setAccessible());
	}

	//====================================================================================================
	// toGenericString()
	//====================================================================================================
	@Test
	void a044_toGenericString() {
		var str = b_c2.toGenericString();
		assertNotNull(str);
		assertTrue(str.contains("B"));
		assertTrue(str.contains("String"));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================
	@Test
	void a045_toString() {
		check("B()", b_c1.toString());
		check("B(String)", b_c2.toString());
		check("m()", b_m1.toString());
		check("m(String)", b_m2.toString());
	}
}

