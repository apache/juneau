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
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.Named;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S4144", // Identical methods intentional for different test scenarios
	"java:S1186", // Empty method body intentional for callback testing
	"java:S1172", // Unused parameters kept for API consistency or framework requirements
})
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
				return ((ConstructorInfo)t).getNameShort();
			if (t instanceof Constructor)
				return ConstructorInfo.of((Constructor<?>)t).getNameShort();
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
		public A() {}
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
		protected B(int f) {}
		@Override
		public String toString() {
			return f;
		}
	}
	static ClassInfo b = ClassInfo.of(B.class);
	static ConstructorInfo
		bc1 = b.getPublicConstructor(cons -> cons.getParameterCount() == 0).get(),
		bc2 = b.getPublicConstructor(x -> x.hasParameterTypes(String.class)).get(),
		bc3 = b.getDeclaredConstructor(x -> x.hasParameterTypes(int.class)).get(),
		bc4 = b.getPublicConstructor(x -> x.hasParameterTypes(String.class, String.class)).get();

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

	public static class EqualsTestClass {
		public EqualsTestClass() {}
		public EqualsTestClass(String param) {}
	}

	//====================================================================================================
	// accessible()
	//====================================================================================================
	@Test
	void a001_accessible() {
		// Make protected constructor accessible
		bc3.accessible();
		assertEquals(null, bc3.newInstanceLenient(123).toString());

		// Verify it returns this for chaining
		var result = bc3.accessible();
		assertSame(bc3, result);
	}

	//====================================================================================================
	// canAccept(Object...)
	//====================================================================================================
	@Test
	void a002_canAccept() {
		// Exact match
		assertTrue(bc2.canAccept("test"));
		assertFalse(bc2.canAccept(123));
		assertFalse(bc2.canAccept("test", "extra"));

		// No parameters
		assertTrue(bc1.canAccept());
		assertFalse(bc1.canAccept("test"));

		// Multiple parameters
		assertTrue(bc4.canAccept("test1", "test2"));
		assertFalse(bc4.canAccept("test1"));
	}

	//====================================================================================================
	// compareTo(ConstructorInfo)
	//====================================================================================================
	@Test
	void a003_compareTo() {
		var s = new TreeSet<>(l(bc1, bc2, bc3, bc4, a));
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
		var types = bc2.getAnnotatedParameterTypes();
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
		var receiverType = bc1.getAnnotatedReceiverType();
		assertNull(receiverType);
	}

	//====================================================================================================
	// getAnnotatableType()
	//====================================================================================================
	@Test
	void a007_getAnnotatableType() {
		assertEquals(AnnotatableType.CONSTRUCTOR_TYPE, bc1.getAnnotatableType());
	}

	//====================================================================================================
	// getDeclaredAnnotations()
	//====================================================================================================
	@Test
	void a008_getDeclaredAnnotations() {
		var annotations = bc1.getDeclaredAnnotations();
		assertNotNull(annotations);
		// Should be empty for unannotated constructor
		assertTrue(annotations.isEmpty());
	}

	//====================================================================================================
	// getDeclaredAnnotations(Class<A>)
	//====================================================================================================
	@Test
	void a009_getDeclaredAnnotations_typed() {
		var annotations = bc1.getDeclaredAnnotations(TestAnnotation.class);
		assertNotNull(annotations);
		assertEquals(0, annotations.count());
	}

	//====================================================================================================
	// getDeclaringClass()
	//====================================================================================================
	@Test
	void a010_getDeclaringClass() {
		check("A", a.getDeclaringClass());
		check("B", bc1.getDeclaringClass());
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
		var fullName = bc2.getNameFull();
		assertNotNull(fullName);
		assertTrue(fullName.contains("B"));
		assertTrue(fullName.contains("String"));
	}

	//====================================================================================================
	// getLabel()
	//====================================================================================================
	@Test
	void a013_getLabel() {
		var label = bc1.getLabel();
		assertNotNull(label);
		assertTrue(label.contains("B"));
		assertTrue(label.contains("()"));
	}

	//====================================================================================================
	// getParameter(int)
	//====================================================================================================
	@Test
	void a014_getParameter() {
		var param = bc2.getParameter(0);
		assertNotNull(param);
		assertEquals(String.class, param.getParameterType().inner());

		// Index out of bounds
		assertThrows(IndexOutOfBoundsException.class, () -> bc2.getParameter(1));
		assertThrows(IndexOutOfBoundsException.class, () -> bc1.getParameter(0));
	}

	//====================================================================================================
	// getParameterCount()
	//====================================================================================================
	@Test
	void a015_getParameterCount() {
		assertEquals(0, bc1.getParameterCount());
		assertEquals(1, bc2.getParameterCount());
		assertEquals(2, bc4.getParameterCount());
	}

	//====================================================================================================
	// getParameters()
	//====================================================================================================
	@Test
	void a016_getParameters() {
		var params = bc2.getParameters();
		assertNotNull(params);
		assertEquals(1, params.size());
		assertEquals(String.class, params.get(0).getParameterType().inner());

		// Test stream operations
		int[] count = {0};
		bc4.getParameters().stream().filter(x -> true).forEach(x -> count[0]++);
		assertEquals(2, count[0]);
	}

	//====================================================================================================
	// getShortName()
	//====================================================================================================
	@Test
	void a017_getShortName() {
		check("A()", a);
		check("B()", bc1);
		check("B(String)", bc2);
		check("B(String,String)", bc4);
	}

	//====================================================================================================
	// getSimpleName()
	//====================================================================================================
	@Test
	void a018_getSimpleName() {
		assertEquals("A", a.getNameSimple());
		assertEquals("B", bc1.getNameSimple());
	}

	//====================================================================================================
	// getTypeParameters()
	//====================================================================================================
	@Test
	void a019_getTypeParameters() {
		var typeParams = bc1.getTypeParameters();
		assertNotNull(typeParams);
		assertEquals(0, typeParams.length);
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a020_hasAnnotation() {
		assertFalse(bc1.hasAnnotation(TestAnnotation.class));
	}

	//====================================================================================================
	// hasAnyName(Collection<String>)
	//====================================================================================================
	@Test
	void a021_hasAnyName_collection() {
		assertTrue(bc1.hasAnyName(Arrays.asList("B", "C")));
		assertFalse(bc1.hasAnyName(Arrays.asList("C", "D")));
	}

	//====================================================================================================
	// hasAnyName(String...)
	//====================================================================================================
	@Test
	void a022_hasAnyName_varargs() {
		assertTrue(bc1.hasAnyName("B", "C"));
		assertFalse(bc1.hasAnyName("C", "D"));
	}

	//====================================================================================================
	// hasMatchingParameters(List<ParameterInfo>)
	//====================================================================================================
	@Test
	void a023_hasMatchingParameters() {
		var params1 = bc2.getParameters();
		assertTrue(bc2.hasMatchingParameters(params1));

		var params2 = bc4.getParameters();
		assertFalse(bc2.hasMatchingParameters(params2));
	}

	//====================================================================================================
	// hasName(String)
	//====================================================================================================
	@Test
	void a024_hasName() {
		assertTrue(bc1.hasName("B"));
		assertFalse(bc1.hasName("A"));
	}

	//====================================================================================================
	// hasNumParameters(int)
	//====================================================================================================
	@Test
	void a025_hasNumParameters() {
		assertTrue(bc1.hasNumParameters(0));
		assertTrue(bc2.hasNumParameters(1));
		assertFalse(bc1.hasNumParameters(1));
	}

	//====================================================================================================
	// hasParameters()
	//====================================================================================================
	@Test
	void a026_hasParameters() {
		assertFalse(bc1.hasParameters());
		assertTrue(bc2.hasParameters());
	}

	//====================================================================================================
	// hasParameterTypeParents(Class<?>...)
	//====================================================================================================
	@Test
	void a027_hasParameterTypeParents_class() {
		// String parameter type is a parent of String (exact match)
		assertTrue(bc2.hasParameterTypeParents(String.class));
		// String is NOT a parent of Object (Object is the parent of String)
		assertFalse(bc2.hasParameterTypeParents(Object.class));
		assertFalse(bc2.hasParameterTypeParents(Integer.class));
	}

	//====================================================================================================
	// hasParameterTypeParents(ClassInfo...)
	//====================================================================================================
	@Test
	void a028_hasParameterTypeParents_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var objectClass = ClassInfo.of(Object.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(bc2.hasParameterTypeParents(stringClass));
		assertFalse(bc2.hasParameterTypeParents(objectClass));
		assertFalse(bc2.hasParameterTypeParents(integerClass));
	}

	//====================================================================================================
	// hasParameterTypes(Class<?>...)
	//====================================================================================================
	@Test
	void a029_hasParameterTypes_class() {
		assertTrue(bc2.hasParameterTypes(String.class));
		assertFalse(bc2.hasParameterTypes(Integer.class));
		assertFalse(bc2.hasParameterTypes(String.class, String.class));
	}

	//====================================================================================================
	// hasParameterTypes(ClassInfo...)
	//====================================================================================================
	@Test
	void a030_hasParameterTypes_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(bc2.hasParameterTypes(stringClass));
		assertFalse(bc2.hasParameterTypes(integerClass));
	}

	//====================================================================================================
	// hasParameterTypesLenient(Class<?>...)
	//====================================================================================================
	@Test
	void a031_hasParameterTypesLenient_class() {
		// Exact match
		assertTrue(bc2.hasParameterTypesLenient(String.class));
		// String parameter type is NOT a parent of Object
		assertFalse(bc2.hasParameterTypesLenient(Object.class));
		assertFalse(bc2.hasParameterTypesLenient(Integer.class));
	}

	//====================================================================================================
	// hasParameterTypesLenient(ClassInfo...)
	//====================================================================================================
	@Test
	void a032_hasParameterTypesLenient_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var objectClass = ClassInfo.of(Object.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(bc2.hasParameterTypesLenient(stringClass));
		assertFalse(bc2.hasParameterTypesLenient(objectClass));
		assertFalse(bc2.hasParameterTypesLenient(integerClass));
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a033_inner() {
		var ctor = bc1.inner();
		assertNotNull(ctor);
		assertEquals(B.class, ctor.getDeclaringClass());
	}

	//====================================================================================================
	// is(ElementFlag)
	//====================================================================================================
	@Test
	void a034_is() {
		assertTrue(bc1.is(ElementFlag.CONSTRUCTOR));
		assertFalse(bc1.is(ElementFlag.NOT_CONSTRUCTOR));
		assertTrue(bc1.is(ElementFlag.HAS_NO_PARAMS));
		assertFalse(bc1.is(ElementFlag.HAS_PARAMS));
		assertTrue(bc2.is(ElementFlag.HAS_PARAMS));
		assertFalse(bc2.is(ElementFlag.HAS_NO_PARAMS));
	}

	//====================================================================================================
	// isAccessible()
	//====================================================================================================
	@Test
	void a035_isAccessible() {
		// Test isAccessible() before and after setAccessible()
		var privateBefore = bc3.isAccessible();

		// Make it accessible
		bc3.setAccessible();

		// After setAccessible(), it should be accessible (if Java 9+)
		var privateAfter = bc3.isAccessible();

		// Verify the method doesn't throw and returns a boolean
		assertTrue(privateAfter || !privateBefore, "After setAccessible(), isAccessible() should return true (Java 9+) or false (Java 8)");

		// Public constructors might already be accessible
		assertDoesNotThrow(() -> bc1.isAccessible());
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a036_isAll() {
		assertTrue(bc1.isAll(ElementFlag.CONSTRUCTOR, ElementFlag.HAS_NO_PARAMS));
		assertFalse(bc1.isAll(ElementFlag.CONSTRUCTOR, ElementFlag.HAS_PARAMS));
	}

	//====================================================================================================
	// isAny(ElementFlag...)
	//====================================================================================================
	@Test
	void a037_isAny() {
		assertTrue(bc1.isAny(ElementFlag.CONSTRUCTOR, ElementFlag.HAS_PARAMS));
		assertFalse(bc1.isAny(ElementFlag.HAS_PARAMS, ElementFlag.SYNTHETIC));
	}

	//====================================================================================================
	// isConstructor()
	//====================================================================================================
	@Test
	void a038_isConstructor() {
		assertTrue(bc1.isConstructor());
	}

	//====================================================================================================
	// isDeprecated()
	//====================================================================================================
	@Test
	void a039_isDeprecated() {
		var ci = ClassInfo.of(DeprecatedClass.class);
		var ctor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).get();
		assertTrue(ctor.isDeprecated());
		assertFalse(bc1.isDeprecated());
	}

	//====================================================================================================
	// isNotDeprecated()
	//====================================================================================================
	@Test
	void a040_isNotDeprecated() {
		var ci = ClassInfo.of(DeprecatedClass.class);
		var ctor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).get();
		assertFalse(ctor.isNotDeprecated());
		assertTrue(bc1.isNotDeprecated());
	}

	//====================================================================================================
	// isSynthetic()
	//====================================================================================================
	@Test
	void a041_isSynthetic() {
		// Regular constructors are not synthetic
		assertFalse(bc1.isSynthetic());
	}

	//====================================================================================================
	// isVarArgs()
	//====================================================================================================
	@Test
	void a042_isVarArgs() {
		var ci = ClassInfo.of(VarArgsClass.class);
		var ctor = ci.getPublicConstructor(x -> x.hasParameterTypes(String[].class)).get();
		assertTrue(ctor.isVarArgs());
		assertFalse(bc1.isVarArgs());
	}

	//====================================================================================================
	// isVisible(Visibility)
	//====================================================================================================
	@Test
	void a043_isVisible() {
		// Public constructor
		assertTrue(bc1.isVisible(Visibility.PUBLIC));
		assertTrue(bc1.isVisible(Visibility.PROTECTED));
		assertTrue(bc1.isVisible(Visibility.PRIVATE)); // PRIVATE includes all
		assertTrue(bc1.isVisible(Visibility.DEFAULT));

		// Protected constructor
		assertFalse(bc3.isVisible(Visibility.PUBLIC));
		assertTrue(bc3.isVisible(Visibility.PROTECTED));
		assertTrue(bc3.isVisible(Visibility.PRIVATE)); // PRIVATE includes all
		assertTrue(bc3.isVisible(Visibility.DEFAULT));
	}

	//====================================================================================================
	// newInstance(Object...)
	//====================================================================================================
	@Test
	void a044_newInstance() {
		assertEquals(null, bc1.newInstance().toString());
		assertEquals("foo", bc2.newInstance("foo").toString());
	}

	//====================================================================================================
	// newInstanceLenient(Object...)
	//====================================================================================================
	@Test
	void a045_newInstanceLenient() {
		assertEquals(null, bc1.newInstanceLenient().toString());
		assertEquals("foo", bc2.newInstanceLenient("foo").toString());
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
		assertEquals(1, bc2.parameterMatchesLenientCount(String.class));
		// String is NOT a parent of Object, so this should return -1
		assertEquals(-1, bc2.parameterMatchesLenientCount(Object.class));
		// No match
		assertEquals(-1, bc2.parameterMatchesLenientCount(Integer.class));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(ClassInfo...)
	//====================================================================================================
	@Test
	void a049_parameterMatchesLenientCount_classInfo() {
		var stringClass = ClassInfo.of(String.class);
		var objectClass = ClassInfo.of(Object.class);
		var integerClass = ClassInfo.of(Integer.class);

		assertEquals(1, bc2.parameterMatchesLenientCount(stringClass));
		// String is NOT a parent of Object, so this should return -1
		assertEquals(-1, bc2.parameterMatchesLenientCount(objectClass));
		assertEquals(-1, bc2.parameterMatchesLenientCount(integerClass));
	}

	//====================================================================================================
	// parameterMatchesLenientCount(Object...)
	//====================================================================================================
	@Test
	void a050_parameterMatchesLenientCount_object() {
		// String parameter can accept String object
		assertEquals(1, bc2.parameterMatchesLenientCount("test"));
		// String parameter can accept Object (String.isAssignableFrom(Object) is false, but canAcceptArg checks if Object can be assigned to String)
		// Actually, wait - canAcceptArg checks if the parameter type can accept the argument value
		// String parameter cannot accept an Object instance (without casting), so this should return -1
		assertEquals(-1, bc2.parameterMatchesLenientCount(new Object()));
		assertEquals(-1, bc2.parameterMatchesLenientCount(123));
	}

	//====================================================================================================
	// setAccessible()
	//====================================================================================================
	@Test
	void a051_setAccessible() {
		// Make protected constructor accessible
		var result = bc3.setAccessible();
		assertTrue(result);
		assertEquals(null, bc3.newInstanceLenient(123).toString());
	}

	//====================================================================================================
	// toGenericString()
	//====================================================================================================
	@Test
	void a052_toGenericString() {
		var str = bc2.toGenericString();
		assertNotNull(str);
		assertTrue(str.contains("B"));
		assertTrue(str.contains("String"));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================
	@Test
	void a053_toString() {
		check("public org.apache.juneau.commons.reflect.ConstructorInfo_Test$A()", a.toString());
		check("public org.apache.juneau.commons.reflect.ConstructorInfo_Test$B()", bc1.toString());
		check("public org.apache.juneau.commons.reflect.ConstructorInfo_Test$B(java.lang.String)", bc2.toString());
	}

	//====================================================================================================
	// equals(Object) and hashCode()
	//====================================================================================================
	@Test
	void a054_equals_hashCode() throws NoSuchMethodException, SecurityException {
		// Get ConstructorInfo instances from the same Constructor
		Constructor<?> c1 = EqualsTestClass.class.getConstructor();
		ConstructorInfo ci1a = ConstructorInfo.of(c1);
		ConstructorInfo ci1b = ConstructorInfo.of(c1);

		Constructor<?> c2 = EqualsTestClass.class.getConstructor(String.class);
		ConstructorInfo ci2 = ConstructorInfo.of(c2);

		// Same constructor should be equal
		assertEquals(ci1a, ci1b);
		assertEquals(ci1a.hashCode(), ci1b.hashCode());

		// Different constructors should not be equal
		assertNotEquals(ci1a, ci2);
		assertNotEquals(null, ci1a);
		assertNotEquals("not a ConstructorInfo", ci1a);

		// Reflexive
		assertEquals(ci1a, ci1a);

		// Symmetric
		assertEquals(ci1a, ci1b);
		assertEquals(ci1b, ci1a);

		// Transitive
		ConstructorInfo ci1c = ConstructorInfo.of(c1);
		assertEquals(ci1a, ci1b);
		assertEquals(ci1b, ci1c);
		assertEquals(ci1a, ci1c);

		// HashMap usage - same constructor should map to same value
		Map<ConstructorInfo, String> map = new HashMap<>();
		map.put(ci1a, "value1");
		assertEquals("value1", map.get(ci1b));
		assertEquals("value1", map.get(ci1c));

		// HashMap usage - different constructors should map to different values
		map.put(ci2, "value2");
		assertEquals("value2", map.get(ci2));
		assertNotEquals("value2", map.get(ci1a));

		// HashSet usage
		Set<ConstructorInfo> set = new HashSet<>();
		set.add(ci1a);
		assertTrue(set.contains(ci1b));
		assertTrue(set.contains(ci1c));
		assertFalse(set.contains(ci2));
	}

	//====================================================================================================
	// Dependency Injection Tests (moved from InjectUtils_Test)
	//====================================================================================================

	// Test bean classes
	static class TestService {
		private final String name;
		TestService(String name) { this.name = name; }
		String getName() { return name; }
		@Override public String toString() { return "TestService[" + name + "]"; }
		@Override public boolean equals(Object o) { return o instanceof TestService o2 && eq(this, o2, (x,y) -> eq(x.name, y.name)); }
		@Override public int hashCode() { return h(name); }
	}

	static class AnotherService {
		private final int value;
		AnotherService(int value) { this.value = value; }
		int getValue() { return value; }
		@Override public boolean equals(Object o) { return o instanceof AnotherService o2 && eq(this, o2, (x,y) -> eq(x.value, y.value)); }
		@Override public int hashCode() { return value; }
	}

	// Test classes with various constructor signatures
	public static class TestClass1 {
		public TestClass1(TestService service) {
			// Single bean parameter
		}
	}

	public static class TestClass2 {
		public TestClass2(@Named("service1") TestService service) {
			// Named bean parameter
		}
	}

	public static class TestClass3 {
		public TestClass3(Optional<TestService> service) {
			// Optional parameter
		}
	}

	public static class TestClass4 {
		public TestClass4(TestService[] services) {
			// Array parameter
		}
	}

	public static class TestClass5 {
		public TestClass5(List<TestService> services) {
			// List parameter
		}
	}

	public static class TestClass6 {
		public TestClass6(Set<TestService> services) {
			// Set parameter
		}
	}

	public static class TestClass7 {
		public TestClass7(Map<String, TestService> services) {
			// Map parameter
		}
	}

	public static class TestClass8 {
		public TestClass8(TestService service1, AnotherService service2) {
			// Multiple parameters
		}
	}

	public static class TestClass9 {
		public TestClass9(TestService service, Optional<AnotherService> optional, List<TestService> list) {
			// Mixed parameter types
		}
	}

	// Inner class for testing outer parameter
	// Note: This is a non-static inner class, so it requires an outer instance
	static class OuterClass {
		public OuterClass() {}

		// Non-static inner class
		class InnerClass {
			public InnerClass(OuterClass outer, TestService service) {
				// First parameter is outer class instance
			}
		}
	}

	private BasicBeanStore2 beanStore;

	@BeforeEach
	void setUpBeanStore() {
		beanStore = new BasicBeanStore2(null);
	}

	//====================================================================================================
	// getMissingParameterTypes
	//====================================================================================================

	@Test
	void b001_getMissingParameterTypes_allAvailable() {
		beanStore.addBean(TestService.class, new TestService("test1"));
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result);
	}

	@Test
	void b002_getMissingParameterTypes_singleMissing() {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertEquals("TestService", result);
	}

	@Test
	void b003_getMissingParameterTypes_namedBeanFound() {
		beanStore.addBean(TestService.class, new TestService("test1"), "service1");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result);
	}

	@Test
	void b004_getMissingParameterTypes_namedBeanMissing() {
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertEquals("TestService@service1", result);
	}

	@Test
	void b005_getMissingParameterTypes_optionalSkipped() {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result); // Optional parameters are skipped
	}

	@Test
	void b006_getMissingParameterTypes_arraySkipped() {
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result); // Arrays are skipped
	}

	@Test
	void b007_getMissingParameterTypes_listSkipped() {
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result); // Lists are skipped
	}

	@Test
	void b008_getMissingParameterTypes_setSkipped() {
		var constructor = ClassInfo.of(TestClass6.class).getPublicConstructor(x -> x.hasParameterTypes(Set.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result); // Sets are skipped
	}

	@Test
	void b009_getMissingParameterTypes_mapSkipped() {
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertNull(result); // Maps are skipped
	}

	@Test
	void b010_getMissingParameterTypes_multipleMissing() {
		var constructor = ClassInfo.of(TestClass8.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, AnotherService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertTrue(result.contains("AnotherService"));
		assertTrue(result.contains("TestService"));
		// Should be sorted
		assertTrue(result.indexOf("AnotherService") < result.indexOf("TestService"));
	}

	@Test
	void b011_getMissingParameterTypes_innerClassOuterSkipped() {
		beanStore.addBean(TestService.class, new TestService("test1"));
		beanStore.addBean(OuterClass.class, new OuterClass()); // Add outer class bean so explicit parameter is available
		var outer = new OuterClass();
		// Use the class literal directly instead of getting it from an instance
		// For non-static inner classes, the constructor has 3 parameters: (implicit outer, explicit outer, TestService)
		// The first parameter (implicit outer) is skipped, but the second (explicit outer) is checked
		var classInfo = ClassInfo.of(OuterClass.InnerClass.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 3 && x.getParameter(2).isType(TestService.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found. Available constructors: " + constructors);
		var constructor = constructorOpt.get();
		var result = constructor.getMissingParameterTypes(beanStore, outer);
		assertNull(result); // All parameters should be available (implicit outer skipped, explicit outer and TestService in store)
	}

	//====================================================================================================
	// resolveParameters
	//====================================================================================================

	@Test
	void b012_resolveParameters_singleBean() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertSame(service, params[0]);
	}

	@Test
	void b013_resolveParameters_singleBeanNotFound() {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertThrows(ExecutableException.class, () -> constructor.resolveParameters(beanStore, null));
	}

	@Test
	void b014_resolveParameters_namedBean() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertSame(service1, params[0]);
	}

	@Test
	void b015_resolveParameters_optionalBeanFound() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Optional);
		var opt = (Optional<TestService>) params[0];
		assertTrue(opt.isPresent());
		assertSame(service, opt.get());
	}

	@Test
	void b016_resolveParameters_optionalBeanNotFound() {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Optional);
		var opt = (Optional<TestService>) params[0];
		assertFalse(opt.isPresent());
	}

	@Test
	void b017_resolveParameters_array() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof TestService[]);
		var array = (TestService[]) params[0];
		assertEquals(2, array.length);
		assertTrue(array[0].equals(service1) || array[0].equals(service2));
		assertTrue(array[1].equals(service1) || array[1].equals(service2));
	}

	@Test
	void b018_resolveParameters_arrayEmpty() {
		var constructor = ClassInfo.of(TestClass4.class).getPublicConstructor(x -> x.hasParameterTypes(TestService[].class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof TestService[]);
		var array = (TestService[]) params[0];
		assertEquals(0, array.length);
	}

	@Test
	void b019_resolveParameters_list() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof List);
		var list = (List<TestService>) params[0];
		assertEquals(2, list.size());
		assertTrue(list.contains(service1));
		assertTrue(list.contains(service2));
	}

	@Test
	void b020_resolveParameters_set() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass6.class).getPublicConstructor(x -> x.hasParameterTypes(Set.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Set);
		var set = (Set<TestService>) params[0];
		assertEquals(2, set.size());
		assertTrue(set.contains(service1));
		assertTrue(set.contains(service2));
	}

	@Test
	void b021_resolveParameters_map() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1, "service1");
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Map);
		var map = (Map<String, TestService>) params[0];
		assertEquals(2, map.size());
		assertSame(service1, map.get("service1"));
		assertSame(service2, map.get("service2"));
	}

	@Test
	void b022_resolveParameters_mapWithUnnamedBean() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1); // Unnamed
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass7.class).getPublicConstructor(x -> x.hasParameterTypes(Map.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(1, params.length);
		assertTrue(params[0] instanceof Map);
		var map = (Map<String, TestService>) params[0];
		assertEquals(2, map.size());
		assertSame(service1, map.get("")); // Unnamed beans use empty string as key
		assertSame(service2, map.get("service2"));
	}

	@Test
	void b023_resolveParameters_mixedTypes() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		var another = new AnotherService(42);
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		beanStore.addBean(AnotherService.class, another);
		var constructor = ClassInfo.of(TestClass9.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, Optional.class, List.class)).get();
		var params = constructor.resolveParameters(beanStore, null);
		assertEquals(3, params.length);
		assertSame(service1, params[0]); // Single bean
		assertTrue(params[1] instanceof Optional); // Optional
		var opt = (Optional<AnotherService>) params[1];
		assertTrue(opt.isPresent());
		assertSame(another, opt.get());
		assertTrue(params[2] instanceof List); // List
		var list = (List<TestService>) params[2];
		assertEquals(2, list.size());
	}

	@Test
	void b024_resolveParameters_innerClassOuter() {
		beanStore.addBean(TestService.class, new TestService("test1"));
		beanStore.addBean(OuterClass.class, new OuterClass()); // Add outer class bean for explicit parameter
		var outer = new OuterClass();
		// Use the class literal directly instead of getting it from an instance
		// For non-static inner classes, the constructor has 3 parameters: (implicit outer, explicit outer, TestService)
		// resolveParameters only uses the bean for index 0, so index 1 will be resolved from bean store
		var classInfo = ClassInfo.of(OuterClass.InnerClass.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 3 && x.getParameter(2).isType(TestService.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found");
		var constructor = constructorOpt.get();
		var params = constructor.resolveParameters(beanStore, outer);
		assertEquals(3, params.length);
		assertSame(outer, params[0]); // Implicit outer instance (from bean parameter)
		assertNotNull(params[1]); // Explicit outer parameter (from bean store)
		assertNotNull(params[2]); // Service from bean store
	}

	//====================================================================================================
	// canResolveAll
	//====================================================================================================

	@Test
	void b025_canResolveAll_allAvailable() {
		beanStore.addBean(TestService.class, new TestService("test1"));
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(constructor.canResolveAllParameters(beanStore, null));
	}

	@Test
	void b026_canResolveAll_missing() {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(constructor.canResolveAllParameters(beanStore, null));
	}

	@Test
	void b027_canResolveAll_optionalSkipped() {
		var constructor = ClassInfo.of(TestClass3.class).getPublicConstructor(x -> x.hasParameterTypes(Optional.class)).get();
		assertTrue(constructor.canResolveAllParameters(beanStore, null)); // Optional is skipped
	}

	@Test
	void b028_canResolveAll_collectionsSkipped() {
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		assertTrue(constructor.canResolveAllParameters(beanStore, null)); // Collections are skipped
	}

	@Test
	void b029_canResolveAll_namedBeanFound() {
		beanStore.addBean(TestService.class, new TestService("test1"), "service1");
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(constructor.canResolveAllParameters(beanStore, null));
	}

	@Test
	void b030_canResolveAll_namedBeanMissing() {
		var constructor = ClassInfo.of(TestClass2.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(constructor.canResolveAllParameters(beanStore, null));
	}

	//====================================================================================================
	// inject
	//====================================================================================================

	@Test
	void b031_inject_constructor() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.inject(beanStore, null);
		assertNotNull(result);
		assertTrue(result instanceof TestClass1);
	}

	@Test
	void b032_inject_constructorWithCollections() {
		var service1 = new TestService("test1");
		var service2 = new TestService("test2");
		beanStore.addBean(TestService.class, service1);
		beanStore.addBean(TestService.class, service2, "service2");
		var constructor = ClassInfo.of(TestClass5.class).getPublicConstructor(x -> x.hasParameterTypes(List.class)).get();
		var result = constructor.inject(beanStore, null);
		assertNotNull(result);
		assertTrue(result instanceof TestClass5);
	}

	@Test
	void b033_inject_innerClassConstructor() {
		beanStore.addBean(TestService.class, new TestService("test1"));
		beanStore.addBean(OuterClass.class, new OuterClass()); // Add outer class bean for explicit parameter
		var outer = new OuterClass();
		// Use the class literal directly instead of getting it from an instance
		// For non-static inner classes, the constructor has 3 parameters: (implicit outer, explicit outer, TestService)
		var classInfo = ClassInfo.of(OuterClass.InnerClass.class);
		var constructors = classInfo.getDeclaredConstructors();
		var constructorOpt = constructors.stream()
			.filter(x -> x.getParameterCount() == 3 && x.getParameter(2).isType(TestService.class))
			.findFirst();
		assertTrue(constructorOpt.isPresent(), "Constructor should be found");
		var constructor = constructorOpt.get();
		constructor.accessible(); // Make constructor accessible
		var result = constructor.inject(beanStore, outer);
		assertNotNull(result);
		assertTrue(result.getClass().getName().contains("InnerClass"));
	}

	//====================================================================================================
	// Edge case tests
	//====================================================================================================

	@Test
	void b034_getMissingParameterTypes_beanNull() {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null);
		assertEquals("TestService", result); // Should check bean store since bean is null
	}

	@Test
	void b035_getMissingParameterTypes_firstParamDoesNotMatchBean() {
		var wrongBean = new AnotherService(42);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, wrongBean);
		assertEquals("TestService", result); // Should check bean store since types don't match
	}

	@Test
	void b036_canResolveAll_beanNull() {
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(constructor.canResolveAllParameters(beanStore, null)); // Should check bean store
	}

	@Test
	void b037_canResolveAll_firstParamDoesNotMatchBean() {
		var wrongBean = new AnotherService(42);
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertFalse(constructor.canResolveAllParameters(beanStore, wrongBean)); // Should check bean store
	}

	@Test
	void b038_canResolveAll_firstParamMatchesBean() {
		var bean = new TestService("test1");
		// Constructor with TestService as first parameter - should use bean, not check store
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(constructor.canResolveAllParameters(beanStore, bean)); // First param satisfied by bean, no store check needed
	}

	@Test
	void b039_canResolveAll_firstParamMatchesBeanButSecondMissing() {
		var bean = new TestService("test1");
		// Constructor with TestService (first) and AnotherService (second) - first satisfied by bean, second missing
		var constructor = ClassInfo.of(TestClass8.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class, AnotherService.class)).get();
		assertFalse(constructor.canResolveAllParameters(beanStore, bean)); // First param satisfied by bean, but second is missing
	}

	//====================================================================================================
	// otherBeans parameter tests
	//====================================================================================================

	@Test
	void b040_getMissingParameterTypes_otherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null, service);
		assertNull(result); // Should find service in otherBeans
	}

	@Test
	void b041_getMissingParameterTypes_otherBeansNotMatching() {
		var wrongService = new AnotherService(42);
		// Wrong type in otherBeans - should still be missing
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.getMissingParameterTypes(beanStore, null, wrongService);
		assertEquals("TestService", result); // Should still be missing
	}

	@Test
	void b042_resolveParameters_otherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = constructor.resolveParameters(beanStore, null, service);
		assertEquals(1, params.length);
		assertSame(service, params[0]); // Should use service from otherBeans
	}

	@Test
	void b043_resolveParameters_otherBeansPreferStore() {
		var storeService = new TestService("store");
		var otherService = new TestService("other");
		beanStore.addBean(TestService.class, storeService);
		// Store has service, otherBeans also has service - should prefer store
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var params = constructor.resolveParameters(beanStore, null, otherService);
		assertEquals(1, params.length);
		assertSame(storeService, params[0]); // Should prefer store over otherBeans
	}

	@Test
	void b044_canResolveAll_otherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		assertTrue(constructor.canResolveAllParameters(beanStore, null, service)); // Should find service in otherBeans
	}

	@Test
	void b045_inject_constructorWithOtherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var constructor = ClassInfo.of(TestClass1.class).getPublicConstructor(x -> x.hasParameterTypes(TestService.class)).get();
		var result = constructor.inject(beanStore, null, service);
		assertNotNull(result);
		assertTrue(result instanceof TestClass1);
	}
}

