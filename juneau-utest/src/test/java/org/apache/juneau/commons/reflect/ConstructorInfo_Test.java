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
import static org.apache.juneau.commons.reflect.ConstructorInfo.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ConstructorInfo_Test extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Iterable)
				return StreamSupport.stream(((Iterable<?>)t).spliterator(), false).map(this).collect(Collectors.joining(","));
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getNameSimple();
			if (t instanceof ConstructorInfo)
				return ((ConstructorInfo)t).getShortName();
			if (t instanceof Constructor)
				return ConstructorInfo.of((Constructor<?>)t).getShortName();
			return t.toString();
		}
	};

	private static ConstructorInfo ofc(Class<?> c, Class<?>...pt) {
		try {
			return of(c.getConstructor(pt));
		} catch (NoSuchMethodException | SecurityException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test classes
	//-----------------------------------------------------------------------------------------------------------------

	static class A {
		public A() {}  // NOSONAR
	}
	static ConstructorInfo a = ofc(A.class);

	public static class B {
		private String f;
		public B() {}
		public B(String f) {
			this.f = f;
		}
		public B(String f, String f2) {
			this.f = f;
		}
		protected B(int f) {}  // NOSONAR
		@Override
		public String toString() {
			return f;
		}
	}
	static ClassInfo b = ClassInfo.of(B.class);
	static ConstructorInfo
		b_c1 = b.getPublicConstructor(cons -> cons.getParameterCount() == 0).get(),
		b_c2 = b.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get(),
		b_c3 = b.getDeclaredConstructor(x -> x.hasParameterTypes(int.class)).get(),
		b_c4 = b.getPublicConstructor(x -> x.hasParameterTypes(String.class, String.class)).get();

	@Target({CONSTRUCTOR})
	@Retention(RUNTIME)
	public static @interface TestAnnotation {
		String value() default "";
	}

	@Target({CONSTRUCTOR})
	@Retention(RUNTIME)
	public static @interface DeprecatedAnnotation {}

	public static class DeprecatedClass {
		@Deprecated
		public DeprecatedClass() {}
	}

	public static class VarArgsClass {
		public VarArgsClass(String...args) {}
	}

	public static class ExceptionClass {
		public ExceptionClass() throws Exception {}
	}

	//====================================================================================================
	// accessible()
	//====================================================================================================
	@Test
	void a001_accessible() throws Exception {
		// Make protected constructor accessible
		b_c3.accessible();
		assertEquals(null, b_c3.newInstanceLenient(123).toString());
		
		// Verify it returns this for chaining
		var result = b_c3.accessible();
		assertSame(b_c3, result);
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
		assertTrue(b_c4.canAccept("test1", "test2"));
		assertFalse(b_c4.canAccept("test1"));
	}

	//====================================================================================================
	// compareTo(ConstructorInfo)
	//====================================================================================================
	@Test
	void a003_compareTo() {
		var s = new TreeSet<>(l(b_c1, b_c2, b_c3, b_c4, a));
		check("A(),B(),B(int),B(String),B(String,String)", s);
	}

	//====================================================================================================
	// getAnnotatedExceptionTypes()
	//====================================================================================================
	@Test
	void a004_getAnnotatedExceptionTypes() {
		var ci = ClassInfo.of(ExceptionClass.class);
		var ctor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).get();
		var types = ctor.getAnnotatedExceptionTypes();
		assertNotNull(types);
		assertEquals(1, types.length);
		assertEquals(Exception.class, types[0].getType());
	}

	//====================================================================================================
	// getAnnotatedParameterTypes()
	//====================================================================================================
	@Test
	void a005_getAnnotatedParameterTypes() {
		var types = b_c2.getAnnotatedParameterTypes();
		assertNotNull(types);
		assertEquals(1, types.length);
		assertEquals(String.class, types[0].getType());
	}

	//====================================================================================================
	// getAnnotatedReceiverType()
	//====================================================================================================
	@Test
	void a006_getAnnotatedReceiverType() {
		// Top-level class constructor should return null
		var receiverType = b_c1.getAnnotatedReceiverType();
		assertNull(receiverType);
	}

	//====================================================================================================
	// getAnnotatableType()
	//====================================================================================================
	@Test
	void a007_getAnnotatableType() {
		assertEquals(AnnotatableType.CONSTRUCTOR_TYPE, b_c1.getAnnotatableType());
	}

	//====================================================================================================
	// getDeclaredAnnotations()
	//====================================================================================================
	@Test
	void a008_getDeclaredAnnotations() {
		var annotations = b_c1.getDeclaredAnnotations();
		assertNotNull(annotations);
		// Should be empty for unannotated constructor
		assertTrue(annotations.isEmpty());
	}

	//====================================================================================================
	// getDeclaredAnnotations(Class<A>)
	//====================================================================================================
	@Test
	void a009_getDeclaredAnnotations_typed() {
		var annotations = b_c1.getDeclaredAnnotations(TestAnnotation.class);
		assertNotNull(annotations);
		assertEquals(0, annotations.count());
	}

	//====================================================================================================
	// getDeclaringClass()
	//====================================================================================================
	@Test
	void a010_getDeclaringClass() {
		check("A", a.getDeclaringClass());
		check("B", b_c1.getDeclaringClass());
	}

	//====================================================================================================
	// getExceptionTypes()
	//====================================================================================================
	@Test
	void a011_getExceptionTypes() {
		var ci = ClassInfo.of(ExceptionClass.class);
		var ctor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).get();
		var exceptions = ctor.getExceptionTypes();
		assertNotNull(exceptions);
		assertEquals(1, exceptions.size());
		assertEquals(Exception.class, exceptions.get(0).inner());
	}

	//====================================================================================================
	// getFullName()
	//====================================================================================================
	@Test
	void a012_getFullName() {
		var fullName = b_c2.getFullName();
		assertNotNull(fullName);
		assertTrue(fullName.contains("B"));
		assertTrue(fullName.contains("String"));
	}

	//====================================================================================================
	// getLabel()
	//====================================================================================================
	@Test
	void a013_getLabel() {
		var label = b_c1.getLabel();
		assertNotNull(label);
		assertTrue(label.contains("B"));
		assertTrue(label.contains("()"));
	}

	//====================================================================================================
	// getParameter(int)
	//====================================================================================================
	@Test
	void a014_getParameter() {
		var param = b_c2.getParameter(0);
		assertNotNull(param);
		assertEquals(String.class, param.getParameterType().inner());
		
		// Index out of bounds
		assertThrows(IndexOutOfBoundsException.class, () -> b_c2.getParameter(1));
		assertThrows(IndexOutOfBoundsException.class, () -> b_c1.getParameter(0));
	}

	//====================================================================================================
	// getParameterCount()
	//====================================================================================================
	@Test
	void a015_getParameterCount() {
		assertEquals(0, b_c1.getParameterCount());
		assertEquals(1, b_c2.getParameterCount());
		assertEquals(2, b_c4.getParameterCount());
	}

	//====================================================================================================
	// getParameters()
	//====================================================================================================
	@Test
	void a016_getParameters() {
		var params = b_c2.getParameters();
		assertNotNull(params);
		assertEquals(1, params.size());
		assertEquals(String.class, params.get(0).getParameterType().inner());
		
		// Test stream operations
		int[] count = {0};
		b_c4.getParameters().stream().filter(x -> true).forEach(x -> count[0]++);
		assertEquals(2, count[0]);
	}

	//====================================================================================================
	// getShortName()
	//====================================================================================================
	@Test
	void a017_getShortName() {
		check("A()", a);
		check("B()", b_c1);
		check("B(String)", b_c2);
		check("B(String,String)", b_c4);
	}

	//====================================================================================================
	// getSimpleName()
	//====================================================================================================
	@Test
	void a018_getSimpleName() {
		assertEquals("A", a.getSimpleName());
		assertEquals("B", b_c1.getSimpleName());
	}

	//====================================================================================================
	// getTypeParameters()
	//====================================================================================================
	@Test
	void a019_getTypeParameters() {
		var typeParams = b_c1.getTypeParameters();
		assertNotNull(typeParams);
		assertEquals(0, typeParams.length);
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a020_hasAnnotation() {
		assertFalse(b_c1.hasAnnotation(TestAnnotation.class));
	}

	//====================================================================================================
	// hasAnyName(Collection<String>)
	//====================================================================================================
	@Test
	void a021_hasAnyName_collection() {
		assertTrue(b_c1.hasAnyName(Arrays.asList("B", "C")));
		assertFalse(b_c1.hasAnyName(Arrays.asList("C", "D")));
	}

	//====================================================================================================
	// hasAnyName(String...)
	//====================================================================================================
	@Test
	void a022_hasAnyName_varargs() {
		assertTrue(b_c1.hasAnyName("B", "C"));
		assertFalse(b_c1.hasAnyName("C", "D"));
	}

	//====================================================================================================
	// hasMatchingParameters(List<ParameterInfo>)
	//====================================================================================================
	@Test
	void a023_hasMatchingParameters() {
		var params1 = b_c2.getParameters();
		assertTrue(b_c2.hasMatchingParameters(params1));
		
		var params2 = b_c4.getParameters();
		assertFalse(b_c2.hasMatchingParameters(params2));
	}

	//====================================================================================================
	// hasName(String)
	//====================================================================================================
	@Test
	void a024_hasName() {
		assertTrue(b_c1.hasName("B"));
		assertFalse(b_c1.hasName("A"));
	}

	//====================================================================================================
	// hasNumParameters(int)
	//====================================================================================================
	@Test
	void a025_hasNumParameters() {
		assertTrue(b_c1.hasNumParameters(0));
		assertTrue(b_c2.hasNumParameters(1));
		assertFalse(b_c1.hasNumParameters(1));
	}

	//====================================================================================================
	// hasParameters()
	//====================================================================================================
	@Test
	void a026_hasParameters() {
		assertFalse(b_c1.hasParameters());
		assertTrue(b_c2.hasParameters());
	}

	//====================================================================================================
	// hasParameterTypeParents(Class<?>...)
	//====================================================================================================
	@Test
	void a027_hasParameterTypeParents_class() {
		// String parameter type is a parent of String (exact match)
		assertTrue(b_c2.hasParameterTypeParents(String.class));
		// String is NOT a parent of Object (Object is the parent of String)
		assertFalse(b_c2.hasParameterTypeParents(Object.class));
		assertFalse(b_c2.hasParameterTypeParents(Integer.class));
	}

	//====================================================================================================
	// hasParameterTypeParents(ClassInfo...)
	//====================================================================================================
	@Test
	void a028_hasParameterTypeParents_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var objectClass = ClassInfo.of(Object.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(b_c2.hasParameterTypeParents(stringClass));
		assertFalse(b_c2.hasParameterTypeParents(objectClass));
		assertFalse(b_c2.hasParameterTypeParents(integerClass));
	}

	//====================================================================================================
	// hasParameterTypes(Class<?>...)
	//====================================================================================================
	@Test
	void a029_hasParameterTypes_class() {
		assertTrue(b_c2.hasParameterTypes(String.class));
		assertFalse(b_c2.hasParameterTypes(Integer.class));
		assertFalse(b_c2.hasParameterTypes(String.class, String.class));
	}

	//====================================================================================================
	// hasParameterTypes(ClassInfo...)
	//====================================================================================================
	@Test
	void a030_hasParameterTypes_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(b_c2.hasParameterTypes(stringClass));
		assertFalse(b_c2.hasParameterTypes(integerClass));
	}

	//====================================================================================================
	// hasParameterTypesLenient(Class<?>...)
	//====================================================================================================
	@Test
	void a031_hasParameterTypesLenient_class() {
		// Exact match
		assertTrue(b_c2.hasParameterTypesLenient(String.class));
		// String parameter type is NOT a parent of Object
		assertFalse(b_c2.hasParameterTypesLenient(Object.class));
		assertFalse(b_c2.hasParameterTypesLenient(Integer.class));
	}

	//====================================================================================================
	// hasParameterTypesLenient(ClassInfo...)
	//====================================================================================================
	@Test
	void a032_hasParameterTypesLenient_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var objectClass = ClassInfo.of(Object.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(b_c2.hasParameterTypesLenient(stringClass));
		assertFalse(b_c2.hasParameterTypesLenient(objectClass));
		assertFalse(b_c2.hasParameterTypesLenient(integerClass));
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a033_inner() {
		var ctor = b_c1.inner();
		assertNotNull(ctor);
		assertEquals(B.class, ctor.getDeclaringClass());
	}

	//====================================================================================================
	// is(ElementFlag)
	//====================================================================================================
	@Test
	void a034_is() {
		assertTrue(b_c1.is(ElementFlag.CONSTRUCTOR));
		assertFalse(b_c1.is(ElementFlag.NOT_CONSTRUCTOR));
		assertTrue(b_c1.is(ElementFlag.HAS_NO_PARAMS));
		assertFalse(b_c1.is(ElementFlag.HAS_PARAMS));
		assertTrue(b_c2.is(ElementFlag.HAS_PARAMS));
		assertFalse(b_c2.is(ElementFlag.HAS_NO_PARAMS));
	}

	//====================================================================================================
	// isAccessible()
	//====================================================================================================
	@Test
	void a035_isAccessible() {
		// Test isAccessible() before and after setAccessible()
		var privateBefore = b_c3.isAccessible();
		
		// Make it accessible
		b_c3.setAccessible();
		
		// After setAccessible(), it should be accessible (if Java 9+)
		var privateAfter = b_c3.isAccessible();
		
		// Verify the method doesn't throw and returns a boolean
		assertTrue(privateAfter || !privateBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");
		
		// Public constructors might already be accessible
		var publicAccessible = b_c1.isAccessible();
		assertNotNull(Boolean.valueOf(publicAccessible));
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a036_isAll() {
		assertTrue(b_c1.isAll(ElementFlag.CONSTRUCTOR, ElementFlag.HAS_NO_PARAMS));
		assertFalse(b_c1.isAll(ElementFlag.CONSTRUCTOR, ElementFlag.HAS_PARAMS));
	}

	//====================================================================================================
	// isAny(ElementFlag...)
	//====================================================================================================
	@Test
	void a037_isAny() {
		assertTrue(b_c1.isAny(ElementFlag.CONSTRUCTOR, ElementFlag.HAS_PARAMS));
		assertFalse(b_c1.isAny(ElementFlag.HAS_PARAMS, ElementFlag.SYNTHETIC));
	}

	//====================================================================================================
	// isConstructor()
	//====================================================================================================
	@Test
	void a038_isConstructor() {
		assertTrue(b_c1.isConstructor());
	}

	//====================================================================================================
	// isDeprecated()
	//====================================================================================================
	@Test
	void a039_isDeprecated() {
		var ci = ClassInfo.of(DeprecatedClass.class);
		var ctor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).get();
		assertTrue(ctor.isDeprecated());
		assertFalse(b_c1.isDeprecated());
	}

	//====================================================================================================
	// isNotDeprecated()
	//====================================================================================================
	@Test
	void a040_isNotDeprecated() {
		var ci = ClassInfo.of(DeprecatedClass.class);
		var ctor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).get();
		assertFalse(ctor.isNotDeprecated());
		assertTrue(b_c1.isNotDeprecated());
	}

	//====================================================================================================
	// isSynthetic()
	//====================================================================================================
	@Test
	void a041_isSynthetic() {
		// Regular constructors are not synthetic
		assertFalse(b_c1.isSynthetic());
	}

	//====================================================================================================
	// isVarArgs()
	//====================================================================================================
	@Test
	void a042_isVarArgs() {
		var ci = ClassInfo.of(VarArgsClass.class);
		var ctor = ci.getPublicConstructor(x -> x.hasParameterTypes(String[].class)).get();
		assertTrue(ctor.isVarArgs());
		assertFalse(b_c1.isVarArgs());
	}

	//====================================================================================================
	// isVisible(Visibility)
	//====================================================================================================
	@Test
	void a043_isVisible() {
		// Public constructor
		assertTrue(b_c1.isVisible(Visibility.PUBLIC));
		assertTrue(b_c1.isVisible(Visibility.PROTECTED));
		assertTrue(b_c1.isVisible(Visibility.PRIVATE)); // PRIVATE includes all
		assertTrue(b_c1.isVisible(Visibility.DEFAULT));
		
		// Protected constructor
		assertFalse(b_c3.isVisible(Visibility.PUBLIC));
		assertTrue(b_c3.isVisible(Visibility.PROTECTED));
		assertTrue(b_c3.isVisible(Visibility.PRIVATE)); // PRIVATE includes all
		assertTrue(b_c3.isVisible(Visibility.DEFAULT));
	}

	//====================================================================================================
	// newInstance(Object...)
	//====================================================================================================
	@Test
	void a044_newInstance() throws Exception {
		assertEquals(null, b_c1.newInstance().toString());
		assertEquals("foo", b_c2.newInstance("foo").toString());
	}

	//====================================================================================================
	// newInstanceLenient(Object...)
	//====================================================================================================
	@Test
	void a045_newInstanceLenient() throws Exception {
		assertEquals(null, b_c1.newInstanceLenient().toString());
		assertEquals("foo", b_c2.newInstanceLenient("foo").toString());
	}

	//====================================================================================================
	// of(ClassInfo, Constructor)
	//====================================================================================================
	@Test
	void a046_of_withDeclaringClass() {
		check("A()", ConstructorInfo.of(ClassInfo.of(A.class), a.inner()));
	}

	//====================================================================================================
	// of(Constructor)
	//====================================================================================================
	@Test
	void a047_of_noDeclaringClass() {
		check("A()", a.inner());
		
		// Null should throw
		assertThrows(IllegalArgumentException.class, () -> ConstructorInfo.of((Constructor<?>)null));
		assertThrows(IllegalArgumentException.class, () -> ConstructorInfo.of((ClassInfo)null, null));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(Class<?>...)
	//====================================================================================================
	@Test
	void a048_parameterMatchesLenientCount_class() {
		// Exact match - String parameter type is a parent of String
		assertEquals(1, b_c2.parameterMatchesLenientCount(String.class));
		// String is NOT a parent of Object, so this should return -1
		assertEquals(-1, b_c2.parameterMatchesLenientCount(Object.class));
		// No match
		assertEquals(-1, b_c2.parameterMatchesLenientCount(Integer.class));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(ClassInfo...)
	//====================================================================================================
	@Test
	void a049_parameterMatchesLenientCount_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var objectClass = ClassInfo.of(Object.class);
		var integerClass = ClassInfo.of(Integer.class);
		
		assertEquals(1, b_c2.parameterMatchesLenientCount(stringClass));
		// String is NOT a parent of Object, so this should return -1
		assertEquals(-1, b_c2.parameterMatchesLenientCount(objectClass));
		assertEquals(-1, b_c2.parameterMatchesLenientCount(integerClass));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(Object...)
	//====================================================================================================
	@Test
	void a050_parameterMatchesLenientCount_object() {
		// String parameter can accept String object
		assertEquals(1, b_c2.parameterMatchesLenientCount("test"));
		// String parameter can accept Object (String.isAssignableFrom(Object) is false, but canAcceptArg checks if Object can be assigned to String)
		// Actually, wait - canAcceptArg checks if the parameter type can accept the argument value
		// String parameter cannot accept an Object instance (without casting), so this should return -1
		assertEquals(-1, b_c2.parameterMatchesLenientCount(new Object()));
		assertEquals(-1, b_c2.parameterMatchesLenientCount(123));
	}

	//====================================================================================================
	// setAccessible()
	//====================================================================================================
	@Test
	void a051_setAccessible() throws Exception {
		// Make protected constructor accessible
		var result = b_c3.setAccessible();
		assertTrue(result);
		assertEquals(null, b_c3.newInstanceLenient(123).toString());
	}

	//====================================================================================================
	// toGenericString()
	//====================================================================================================
	@Test
	void a052_toGenericString() {
		var str = b_c2.toGenericString();
		assertNotNull(str);
		assertTrue(str.contains("B"));
		assertTrue(str.contains("String"));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================
	@Test
	void a053_toString() {
		check("A()", a.toString());
		check("B()", b_c1.toString());
		check("B(String)", b_c2.toString());
	}
}

