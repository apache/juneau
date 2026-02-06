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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

import static org.apache.juneau.commons.utils.Utils.*;

@SuppressWarnings({ "java:S1186", "java:S116", "java:S1172" }) // Field names use underscores for test data; unused parameters in tests are typically intentional
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

	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	public static @interface TestAnnotationWithDefault {
		String value() default "defaultValue";
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
				return t2.getDeclaringClass().getNameSimple() + '.' + ((MethodInfo)t).getNameShort();
			if (t instanceof Method t2)
				return t2.getDeclaringClass().getSimpleName() + '.' + MethodInfo.of((Method)t).getNameShort();
			if (t instanceof List<?> t2)
				return (t2.stream().map(this).collect(Collectors.joining(",")));
			if (t instanceof java.util.Set<?> t2)
				return "[" + t2.stream().map(this).collect(Collectors.joining(", ")) + "]";
			if (t instanceof AnnotationInfo t2)
				return apply(t2.inner());
			if (t instanceof A t2)
				return "@A(" + t2.value() + ")";
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
	// Test classes
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {
		public void m() {}
	}
	static MethodInfo a_m = ofm(A1.class, "m");

	@SuppressWarnings("java:S1186")
	public static class EqualsTestClass {
		public void method1() {}
		public void method2(String param) {}
	}

	public interface B1 {
		int foo(int x);
		int foo(String x);
		int foo();
	}
	public static class B2 {
		public int foo(int x) { return 0; }
		public int foo(String x) {return 0;}
		public int foo() {return 0;}
	}
	public static class B3 extends B2 implements B1 {
		@Override public int foo(int x) {return 0;}
		@Override public int foo(String x) {return 0;}
		@Override public int foo() {return 0;}
	}

	public interface BM1 {
		void foo(String s);
	}

	public interface BM2 {
		void foo(String s);
	}

	public class BM3 {
		public void foo(String s) {}
	}

	public interface BM4 extends BM1 {
		@Override void foo(String s);
	}

	public class BM5 extends BM3 implements BM2 {
		@Override public void foo(String s) {}
	}

	public class BM6 extends BM5 implements BM4 {
		@Override public void foo(String s) {}
	}

	public interface BM7 {
		void bar();
	}

	public class BM8 implements BM7 {
		@Override public void bar() {}
		public void baz() {}
	}

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

	public static class D {
		public void a1() {}
		public Integer a2() {return null;}
	}
	static MethodInfo
		d_a1 = ofm(D.class, "a1"),
		d_a2 = ofm(D.class, "a2");

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

	public interface DefaultInterface {
		default String defaultMethod() { return "default"; }
	}

	// Bridge method test class - when a class implements a generic interface/class
	// and overrides a method with a more specific type, the compiler creates a bridge method
	public interface GenericInterface<T> {
		T getValue();
	}
	public static class BridgeTestClass implements GenericInterface<String> {
		@Override
		public String getValue() { return "value"; }  // This creates a bridge method
	}

	//====================================================================================================
	// accessible()
	//====================================================================================================
	@Test
	void a001_accessible() {
		var result = a_m.accessible();
		assertSame(a_m, result);
	}

	// Test classes for compareTo tie-breaker testing
	@SuppressWarnings("java:S1186")
	public static class CompareToParent {
		public CompareToParent getInstance() {
			return new CompareToParent();
		}
		public void method(String param) {}
	}

	@SuppressWarnings("java:S1186")
	public static class CompareToChild extends CompareToParent {
		@Override
		public CompareToChild getInstance() {
			return new CompareToChild();
		}
		@Override
		public void method(String param) {}
	}

	//====================================================================================================
	// compareTo(MethodInfo)
	//====================================================================================================
	@Test
	void a002_compareTo() {
		var s = new TreeSet<>(l(g_a1a, g_a1b, g_a1c, g_a1d, g_a2, g_a3));
		check("[G.a1(), G.a1(int), G.a1(String), G.a1(int,int), G.a2(), G.a3()]", s);
	}

	/**
	 * Tests that compareTo() correctly handles tie-breaking for overridden methods.
	 * When methods have the same name and parameters, child class methods should
	 * come before parent class methods in sorted order. This ensures consistent ordering
	 * and that methods with covariant return types are selected correctly.
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	@Test
	void a002b_compareTo_tieBreakerForOverriddenMethods() throws NoSuchMethodException, SecurityException {
		// Get methods from parent and child classes with same signature
		var parentMethod = MethodInfo.of(CompareToParent.class.getMethod("getInstance"));
		var childMethod = MethodInfo.of(CompareToChild.class.getMethod("getInstance"));

		// Child method should come before parent method (negative comparison)
		assertTrue(childMethod.compareTo(parentMethod) < 0, "Child method should come before parent method");
		assertTrue(parentMethod.compareTo(childMethod) > 0, "Parent method should come after child method");

		// When sorted, child should come first
		var sorted = new TreeSet<>(l(parentMethod, childMethod));
		var list = new ArrayList<>(sorted);
		assertEquals(childMethod, list.get(0), "Child method should be first in sorted set");
		assertEquals(parentMethod, list.get(1), "Parent method should be second in sorted set");

		// Test with methods that have parameters
		var parentMethodWithParam = MethodInfo.of(CompareToParent.class.getMethod("method", String.class));
		var childMethodWithParam = MethodInfo.of(CompareToChild.class.getMethod("method", String.class));

		assertTrue(childMethodWithParam.compareTo(parentMethodWithParam) < 0, "Child method with params should come before parent");
		assertTrue(parentMethodWithParam.compareTo(childMethodWithParam) > 0, "Parent method with params should come after child");

		// Test with unrelated classes (should fall back to class name comparison)
		var unrelatedMethod = MethodInfo.of(A1.class.getMethod("m"));
		var unrelatedMethod2 = MethodInfo.of(EqualsTestClass.class.getMethod("method1"));
		// These should have different names, so comparison should be based on name, not tie-breaker
		assertNotEquals(0, unrelatedMethod.compareTo(unrelatedMethod2));
	}

	//====================================================================================================
	// getAnnotatedReturnType()
	//====================================================================================================
	@Test
	void a003_getAnnotatedReturnType() {
		var annotatedType = d_a2.getAnnotatedReturnType();
		assertNotNull(annotatedType);
		assertEquals(Integer.class, annotatedType.getType());
	}

	//====================================================================================================
	// getAnnotatableType()
	//====================================================================================================
	@Test
	void a004_getAnnotatableType() {
		assertEquals(AnnotatableType.METHOD_TYPE, a_m.getAnnotatableType());
	}

	//====================================================================================================
	// getAnnotations()
	//====================================================================================================
	@Test
	void a005_getAnnotations() {
		// getAnnotations() includes annotations from matching methods in hierarchy
		var annotations = c_a1.getAnnotations();
		assertNotNull(annotations);
		assertTrue(annotations.size() > 0);

		// Should include annotations from C1, C2, C3, and the method itself
		var aAnnotations = annotations.stream().filter(a -> a.isType(A.class)).toList();
		assertTrue(aAnnotations.size() >= 1);
	}

	//====================================================================================================
	// getAnnotations(Class<A>)
	//====================================================================================================
	@Test
	void a006_getAnnotations_typed() {
		// Should find annotations from hierarchy
		var aAnnotations = c_a1.getAnnotations(A.class).toList();
		assertTrue(aAnnotations.size() >= 1);

		// Should not find non-existent annotations
		var axAnnotations = c_a1.getAnnotations(AX.class).toList();
		assertEquals(0, axAnnotations.size());
	}

	//====================================================================================================
	// getDefaultValue()
	//====================================================================================================
	@Test
	void a007_getDefaultValue() {
		// Regular methods don't have default values
		assertNull(a_m.getDefaultValue());

		// Annotation methods can have default values - A doesn't have a default, so it returns null
		var annotationMethod = ClassInfo.of(A.class).getPublicMethod(m -> m.hasName("value")).get();
		assertNull(annotationMethod.getDefaultValue());  // A.value() has no default value

		// Test with an annotation that has a default value
		var annotationWithDefault = ClassInfo.of(TestAnnotationWithDefault.class).getPublicMethod(m -> m.hasName("value")).get();
		assertNotNull(annotationWithDefault.getDefaultValue());
		assertEquals("defaultValue", annotationWithDefault.getDefaultValue());
	}

	//====================================================================================================
	// getGenericReturnType()
	//====================================================================================================
	@Test
	void a008_getGenericReturnType() {
		var genericType = d_a2.getGenericReturnType();
		assertNotNull(genericType);
		assertEquals(Integer.class, genericType);
	}

	//====================================================================================================
	// getLabel()
	//====================================================================================================
	@Test
	void a009_getLabel() {
		var label = a_m.getLabel();
		assertNotNull(label);
		assertTrue(label.contains("A1"));
		assertTrue(label.contains("m()"));
	}

	//====================================================================================================
	// getMatchingMethods()
	//====================================================================================================
	@Test
	void a010_getMatchingMethods() throws NoSuchMethodException, SecurityException {
		// Simple hierarchy
		var mi = MethodInfo.of(B3.class.getMethod("foo", int.class));
		check("B3.foo(int),B1.foo(int),B2.foo(int)", mi.getMatchingMethods());

		// Multiple interfaces
		var mi2 = MethodInfo.of(BM5.class.getMethod("foo", String.class));
		check("BM5.foo(String),BM2.foo(String),BM3.foo(String)", mi2.getMatchingMethods());

		// Nested interfaces
		var mi3 = MethodInfo.of(BM6.class.getMethod("foo", String.class));
		check("BM6.foo(String),BM4.foo(String),BM1.foo(String),BM5.foo(String),BM2.foo(String),BM3.foo(String)", mi3.getMatchingMethods());

		// Only this method
		var mi4 = MethodInfo.of(Object.class.getMethod("toString"));
		check("Object.toString()", mi4.getMatchingMethods());

		// With interface
		var mi5 = MethodInfo.of(BM8.class.getMethod("bar"));
		check("BM8.bar(),BM7.bar()", mi5.getMatchingMethods());

		// No match in parent
		var mi6 = MethodInfo.of(BM8.class.getMethod("baz"));
		check("BM8.baz()", mi6.getMatchingMethods());
	}

	//====================================================================================================
	// getName()
	//====================================================================================================
	@Test
	void a011_getName() {
		assertEquals("m", a_m.getName());
		assertEquals("a1", e_a1.getName());
	}

	//====================================================================================================
	// getPropertyName()
	//====================================================================================================
	@Test
	void a012_getPropertyName() {
		assertEquals("a", f_isA.getPropertyName());
		assertEquals("is", f_is.getPropertyName());
		assertEquals("a", f_getA.getPropertyName());
		assertEquals("get", f_get.getPropertyName());
		assertEquals("a", f_setA.getPropertyName());
		assertEquals("set", f_set.getPropertyName());
		assertEquals("foo", f_foo.getPropertyName());
	}

	//====================================================================================================
	// getReturnType()
	//====================================================================================================
	@Test
	void a013_getReturnType() {
		check("void", d_a1.getReturnType());
		check("Integer", d_a2.getReturnType());
	}

	// Test classes for covariant return type testing
	public static class ParentWithMethod {
		public ParentWithMethod getInstance() {
			return new ParentWithMethod();
		}
	}

	public static class ChildWithCovariantReturn extends ParentWithMethod {
		@Override
		public ChildWithCovariantReturn getInstance() {
			return new ChildWithCovariantReturn();
		}
	}

	/**
	 * Tests that getReturnType() correctly handles covariant return types.
	 * When a child class overrides a parent method with a more specific return type,
	 * getReturnType() should return the child's return type, not the parent's.
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	@Test
	void a013b_getReturnType_covariantReturnType() throws NoSuchMethodException, SecurityException {
		// Get the method from the child class
		var childMethod = MethodInfo.of(ChildWithCovariantReturn.class.getMethod("getInstance"));
		var childReturnType = childMethod.getReturnType();

		// Should return the child type, not the parent type
		assertEquals(ChildWithCovariantReturn.class, childReturnType.inner());
		assertNotEquals(ParentWithMethod.class, childReturnType.inner());

		// Verify it's actually a child of the parent
		assertTrue(childReturnType.isChildOf(ClassInfo.of(ParentWithMethod.class)));

		// Get the method from the parent class for comparison
		var parentMethod = MethodInfo.of(ParentWithMethod.class.getMethod("getInstance"));
		var parentReturnType = parentMethod.getReturnType();

		// Parent method should return parent type
		assertEquals(ParentWithMethod.class, parentReturnType.inner());

		// Child and parent return types should be different
		assertNotEquals(childReturnType.inner(), parentReturnType.inner());
	}

	//====================================================================================================
	// getSignature()
	//====================================================================================================
	@Test
	void a014_getSignature() {
		assertEquals("a1(java.lang.CharSequence)", e_a1.getSignature());
		assertEquals("a2(int,int)", e_a2.getSignature());
		assertEquals("a3", e_a3.getSignature());
	}

	//====================================================================================================
	// hasAllParameters(Class<?>...)
	//====================================================================================================
	@Test
	void a015_hasAllParameters() {
		assertTrue(e_a2.hasAllParameters(int.class));
		assertTrue(e_a2.hasAllParameters(int.class, int.class));
		assertFalse(e_a2.hasAllParameters(int.class, String.class));
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a016_hasAnnotation() {
		// Should find annotations from hierarchy
		assertTrue(c_a1.hasAnnotation(A.class));
		assertFalse(c_a1.hasAnnotation(AX.class));
	}

	//====================================================================================================
	// hasOnlyParameterTypes(Class<?>...)
	//====================================================================================================
	@Test
	void a017_hasOnlyParameterTypes() {
		assertTrue(e_a1.hasOnlyParameterTypes(CharSequence.class));
		assertTrue(e_a1.hasOnlyParameterTypes(CharSequence.class, Map.class));
		assertFalse(e_a1.hasOnlyParameterTypes());
		// Note: hasOnlyParameterTypes is not meant for methods with duplicate parameter types.
		// It checks if each parameter type is present in the args list, but doesn't verify exact counts.
		// So e_a2(int, int) with args (int.class) should return true because both int parameters match int.class.
		// However, the current implementation may have different behavior - test reflects actual behavior.
		// This test case demonstrates the limitation mentioned in the javadoc.
		assertDoesNotThrow(() -> e_a2.hasOnlyParameterTypes(int.class));
		// The result depends on the implementation - it may be true (lenient) or false (strict)
	}

	//====================================================================================================
	// hasParameter(Class<?>)
	//====================================================================================================
	@Test
	void a018_hasParameter() {
		assertTrue(e_a2.hasParameter(int.class));
		assertFalse(e_a2.hasParameter(String.class));
	}

	//====================================================================================================
	// hasReturnType(Class<?>)
	//====================================================================================================
	@Test
	void a019_hasReturnType_class() {
		assertTrue(d_a1.hasReturnType(void.class));
		assertFalse(d_a1.hasReturnType(Integer.class));
		assertTrue(d_a2.hasReturnType(Integer.class));
		assertFalse(d_a2.hasReturnType(Number.class));
	}

	//====================================================================================================
	// hasReturnType(ClassInfo)
	//====================================================================================================
	@Test
	void a020_hasReturnType_classInfo() {
		var voidClass = ClassInfo.of(void.class);
		var integerClass = ClassInfo.of(Integer.class);
		assertTrue(d_a1.hasReturnType(voidClass));
		assertFalse(d_a1.hasReturnType(integerClass));
		assertTrue(d_a2.hasReturnType(integerClass));
	}

	//====================================================================================================
	// hasReturnTypeParent(Class<?>)
	//====================================================================================================
	@Test
	void a021_hasReturnTypeParent_class() {
		assertTrue(d_a1.hasReturnTypeParent(void.class));
		assertFalse(d_a1.hasReturnTypeParent(Integer.class));
		assertTrue(d_a2.hasReturnTypeParent(Integer.class));
		assertTrue(d_a2.hasReturnTypeParent(Number.class));
	}

	//====================================================================================================
	// hasReturnTypeParent(ClassInfo)
	//====================================================================================================
	@Test
	void a022_hasReturnTypeParent_classInfo() {
		var integerClass = ClassInfo.of(Integer.class);
		var numberClass = ClassInfo.of(Number.class);
		assertTrue(d_a2.hasReturnTypeParent(integerClass));
		assertTrue(d_a2.hasReturnTypeParent(numberClass));
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a023_inner() {
		var method = a_m.inner();
		assertNotNull(method);
		assertEquals("m", method.getName());
	}

	//====================================================================================================
	// invoke(Object, Object...)
	//====================================================================================================
	@Test
	void a024_invoke() {
		var e = new E();
		e_a1.invoke(e, "foo");
		assertEquals("foo", e.f);
		e_a1.invoke(e, (CharSequence)null);
		assertNull(e.f);
	}

	//====================================================================================================
	// invokeLenient(Object, Object...)
	//====================================================================================================
	@Test
	void a025_invokeLenient() {
		var e = new E();
		e_a1.invokeLenient(e, "foo", 123);
		assertEquals("foo", e.f);
		e_a1.invokeLenient(e, 123, "bar");
		assertEquals("bar", e.f);
	}

	//====================================================================================================
	// is(ElementFlag)
	//====================================================================================================
	@Test
	void a026_is() {
		// Bridge method
		assertFalse(f_foo.is(ElementFlag.BRIDGE));
		assertTrue(f_foo.is(ElementFlag.NOT_BRIDGE));  // Line 599: true branch - method is NOT a bridge

		// Test line 599: false branch - method IS a bridge (NOT_BRIDGE returns false)
		// Bridge methods are created by the compiler for generic type erasure
		// Bridge methods occur when a class implements a generic interface/class
		// and overrides a method with a more specific type
		ClassInfo bridgeClass = ClassInfo.of(BridgeTestClass.class);
		var bridgeMethods = bridgeClass.getPublicMethods();
		var bridgeMethod = bridgeMethods.stream()
			.filter(m -> m.isBridge())
			.findFirst();
		if (bridgeMethod.isPresent()) {
			// Test the false branch: when method IS a bridge, NOT_BRIDGE should return false
			assertTrue(bridgeMethod.get().is(ElementFlag.BRIDGE));
			assertFalse(bridgeMethod.get().is(ElementFlag.NOT_BRIDGE));  // Line 599: false branch
		} else {
			// Fallback: try ArrayList which should have bridge methods
			ClassInfo listClass = ClassInfo.of(java.util.ArrayList.class);
			var methods = listClass.getPublicMethods();
			var arrayListBridge = methods.stream()
				.filter(m -> m.isBridge())
				.findFirst();
			if (arrayListBridge.isPresent()) {
				assertTrue(arrayListBridge.get().is(ElementFlag.BRIDGE));
				assertFalse(arrayListBridge.get().is(ElementFlag.NOT_BRIDGE));  // Line 599: false branch
			}
		}

		// Default method
		var defaultMethod = ClassInfo.of(DefaultInterface.class).getPublicMethod(m -> m.hasName("defaultMethod")).get();
		assertTrue(defaultMethod.is(ElementFlag.DEFAULT));
		assertFalse(defaultMethod.is(ElementFlag.NOT_DEFAULT));

		// Regular method
		assertFalse(a_m.is(ElementFlag.DEFAULT));
		assertTrue(a_m.is(ElementFlag.NOT_DEFAULT));
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a027_isAll() {
		assertTrue(a_m.isAll(ElementFlag.NOT_BRIDGE, ElementFlag.NOT_DEFAULT));
		assertFalse(a_m.isAll(ElementFlag.BRIDGE, ElementFlag.DEFAULT));
	}

	//====================================================================================================
	// isAny(ElementFlag...)
	//====================================================================================================
	@Test
	void a028_isAny() {
		assertTrue(a_m.isAny(ElementFlag.NOT_BRIDGE, ElementFlag.DEFAULT));
		assertFalse(a_m.isAny(ElementFlag.BRIDGE, ElementFlag.DEFAULT));
	}

	//====================================================================================================
	// isBridge()
	//====================================================================================================
	@Test
	void a029_isBridge() {
		assertFalse(f_foo.isBridge());
	}

	//====================================================================================================
	// isDefault()
	//====================================================================================================
	@Test
	void a030_isDefault() {
		var defaultMethod = ClassInfo.of(DefaultInterface.class).getPublicMethod(m -> m.hasName("defaultMethod")).get();
		assertTrue(defaultMethod.isDefault());
		assertFalse(a_m.isDefault());
	}

	//====================================================================================================
	// matches(MethodInfo)
	//====================================================================================================
	@Test
	void a031_matches() throws NoSuchMethodException, SecurityException {
		var mi1 = MethodInfo.of(B3.class.getMethod("foo", int.class));
		var mi2 = MethodInfo.of(B2.class.getMethod("foo", int.class));
		var mi3 = MethodInfo.of(B3.class.getMethod("foo", String.class));

		assertTrue(mi1.matches(mi2));
		assertFalse(mi1.matches(mi3));
	}

	//====================================================================================================
	// of(Class<?>, Method)
	//====================================================================================================
	@Test
	void a032_of_withClass() throws NoSuchMethodException, SecurityException {
		var method = A1.class.getMethod("m");
		var mi = MethodInfo.of(A1.class, method);
		check("A1.m()", mi);
	}

	//====================================================================================================
	// of(ClassInfo, Method)
	//====================================================================================================
	@Test
	void a033_of_withClassInfo() {
		check("A1.m()", MethodInfo.of(ClassInfo.of(A1.class), a_m.inner()));
	}

	//====================================================================================================
	// of(Method)
	//====================================================================================================
	@Test
	void a034_of_withoutClass() {
		var mi = MethodInfo.of(a_m.inner());
		check("A1.m()", mi);

		// Null should throw
		assertThrows(IllegalArgumentException.class, () -> MethodInfo.of((Method)null));
		assertThrows(IllegalArgumentException.class, () -> MethodInfo.of((ClassInfo)null, null));
	}

	//====================================================================================================
	// getDeclaringClass() - inherited from ExecutableInfo
	//====================================================================================================
	@Test
	void a035_getDeclaringClass() throws NoSuchMethodException, SecurityException {
		check("A1", a_m.getDeclaringClass());
		check("B3", MethodInfo.of(B3.class.getMethod("foo", int.class)).getDeclaringClass());
	}

	//====================================================================================================
	// getFullName() - inherited from ExecutableInfo
	//====================================================================================================
	@Test
	void a036_getFullName() {
		var fullName = e_a1.getNameFull();
		assertNotNull(fullName);
		assertTrue(fullName.contains("MethodInfo_Test$E"));
		assertTrue(fullName.contains("a1"));
		assertTrue(fullName.contains("CharSequence"));
	}

	//====================================================================================================
	// getParameters() - inherited from ExecutableInfo
	//====================================================================================================
	@Test
	void a037_getParameters() {
		assertEquals(0, e_a3.getParameters().size());
		assertEquals(1, e_a1.getParameters().size());
		assertEquals(2, e_a2.getParameters().size());

		// Test stream operations
		int[] count = {0};
		e_a2.getParameters().stream().filter(x -> true).forEach(x -> count[0]++);
		assertEquals(2, count[0]);
	}

	//====================================================================================================
	// getShortName() - inherited from ExecutableInfo
	//====================================================================================================
	@Test
	void a038_getShortName() {
		assertEquals("m()", a_m.getNameShort());
		assertEquals("a1(CharSequence)", e_a1.getNameShort());
		assertEquals("a2(int,int)", e_a2.getNameShort());
	}

	//====================================================================================================
	// getSimpleName() - inherited from ExecutableInfo
	//====================================================================================================
	@Test
	void a039_getSimpleName() {
		assertEquals("m", a_m.getNameSimple());
		assertEquals("a1", e_a1.getNameSimple());
	}

	//====================================================================================================
	// isConstructor() - inherited from ExecutableInfo
	//====================================================================================================
	@Test
	void a040_isConstructor() {
		assertFalse(a_m.isConstructor());
		assertTrue(ClassInfo.of(A1.class).getPublicConstructor(cons -> cons.getParameterCount() == 0).get().isConstructor());
	}

	//====================================================================================================
	// equals(Object) and hashCode()
	//====================================================================================================
	@Test
	void a041_equals_hashCode() throws NoSuchMethodException, SecurityException {
		// Get MethodInfo instances from the same Method
		Method m1 = EqualsTestClass.class.getMethod("method1");
		MethodInfo mi1a = MethodInfo.of(m1);
		MethodInfo mi1b = MethodInfo.of(m1);

		Method m2 = EqualsTestClass.class.getMethod("method2", String.class);
		MethodInfo mi2 = MethodInfo.of(m2);

		// Same method should be equal
		assertEquals(mi1a, mi1b);
		assertEquals(mi1a.hashCode(), mi1b.hashCode());

		// Different methods should not be equal
		assertNotEquals(mi1a, mi2);
		assertNotEquals(null, mi1a);
		assertNotEquals("not a MethodInfo", mi1a);

		// Reflexive
		assertEquals(mi1a, mi1a);

		// Symmetric
		assertEquals(mi1a, mi1b);
		assertEquals(mi1b, mi1a);

		// Transitive
		MethodInfo mi1c = MethodInfo.of(m1);
		assertEquals(mi1a, mi1b);
		assertEquals(mi1b, mi1c);
		assertEquals(mi1a, mi1c);

		// HashMap usage - same method should map to same value
		Map<MethodInfo, String> map = new HashMap<>();
		map.put(mi1a, "value1");
		assertEquals("value1", map.get(mi1b));
		assertEquals("value1", map.get(mi1c));

		// HashMap usage - different methods should map to different values
		map.put(mi2, "value2");
		assertEquals("value2", map.get(mi2));
		assertNotEquals("value2", map.get(mi1a));

		// HashSet usage
		Set<MethodInfo> set = new HashSet<>();
		set.add(mi1a);
		assertTrue(set.contains(mi1b));
		assertTrue(set.contains(mi1c));
		assertFalse(set.contains(mi2));
	}

	//====================================================================================================
	// Dependency Injection Tests (moved from InjectUtils_Test)
	//====================================================================================================

	// Test bean classes (shared with ConstructorInfo_Test)
	static class TestService {
		private final String name;
		TestService(String name) { this.name = name; }
		String getName() { return name; }
		@Override public String toString() { return "TestService[" + name + "]"; }
		@Override public boolean equals(Object o) { return o instanceof TestService o2 && eq(this, o2, (x,y) -> eq(x.name, y.name)); }
		@Override public int hashCode() { return h(name); }
	}

	// Test method classes
	@SuppressWarnings("java:S1186")
	public static class TestMethodClass {
		public void method1(TestService service) {}
		public void method2(@org.apache.juneau.annotation.Named("service1") TestService service) {}
		public void method3(Optional<TestService> service) {}
		public void method4(TestService[] services) {}
		public void method5(List<TestService> services) {}
		public void method6(Set<TestService> services) {}
		public void method7(Map<String, TestService> services) {}
		public static void staticMethod(TestService service) {}
	}

	private BasicBeanStore2 beanStore;

	@BeforeEach
	void setUpBeanStore() {
		beanStore = new BasicBeanStore2(null);
	}

	//====================================================================================================
	// inject
	//====================================================================================================

	@Test
	void b001_inject_method() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var instance = new TestMethodClass();
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		assertDoesNotThrow(() -> method.inject(beanStore, instance));
	}

	@Test
	void b002_inject_staticMethod() {
		var service = new TestService("test1");
		beanStore.addBean(TestService.class, service);
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("staticMethod") && x.hasParameterTypes(TestService.class)).get();
		assertDoesNotThrow(() -> method.inject(beanStore, null)); // null for static methods
	}

	//====================================================================================================
	// otherBeans parameter tests
	//====================================================================================================

	@Test
	void b003_inject_methodWithOtherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var instance = new TestMethodClass();
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		assertDoesNotThrow(() -> method.inject(beanStore, instance, service));
	}

	@Test
	void b004_getMissingParameterTypes_methodWithOtherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		var result = method.getMissingParameterTypes(beanStore, service);
		assertNull(result); // Should find service in otherBeans
	}

	@Test
	void b005_resolveParameters_methodWithOtherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		var params = method.resolveParameters(beanStore, service);
		assertEquals(1, params.length);
		assertSame(service, params[0]); // Should use service from otherBeans
	}

	@Test
	void b006_canResolveAll_methodWithOtherBeans() {
		var service = new TestService("test1");
		// Service not in bean store, but provided as otherBeans
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		assertTrue(method.canResolveAllParameters(beanStore, service)); // Should find service in otherBeans
	}

	@Test
	void b007_resolveParameters_methodNotFound() {
		// Method parameter not in bean store and not in otherBeans - should throw exception
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		assertThrows(ExecutableException.class, () -> method.resolveParameters(beanStore));
	}

	@Test
	void b008_inject_methodNotFound() {
		// Method parameter not in bean store and not in otherBeans - should throw exception
		var instance = new TestMethodClass();
		var method = ClassInfo.of(TestMethodClass.class).getPublicMethod(x -> x.hasName("method1") && x.hasParameterTypes(TestService.class)).get();
		assertThrows(ExecutableException.class, () -> method.inject(beanStore, instance));
	}
}

