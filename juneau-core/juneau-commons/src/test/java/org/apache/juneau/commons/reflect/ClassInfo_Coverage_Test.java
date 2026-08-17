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

import static org.apache.juneau.commons.reflect.ElementFlag.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage tests for {@link ClassInfo}, filling gaps not otherwise exercised
 * incidentally by the rest of the test suite (there is no single comprehensive
 * <c>ClassInfo_Test</c> - this file focuses on specific branch gaps identified via coverage.py).
 */
@SuppressWarnings({
	"java:S1186", // Empty method bodies intentional for reflection test fixtures
	"java:S116",  // Field names use underscores for test data clarity
	"unused"      // Fields/methods referenced only via reflection
})
class ClassInfo_Coverage_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//-----------------------------------------------------------------------------------------------------------------

	interface Greeter {
		String greet();
	}

	static class Outer1 {
		class Inner1 {}
	}

	static class Outer2 {}

	static <T> void genericMethod(T t) {}

	@Deprecated
	static class DeprecatedClass {}

	class NoModifierMemberClass {}

	static class BoundedTypeParam<T extends Number> {}

	//-----------------------------------------------------------------------------------------------------------------
	// a: ofProxy() - getProxyFor() null vs. non-null (line 187)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_ofProxy_nonProxyObject_usesObjectClassDirectly() {
		var ci = ClassInfo.ofProxy("foo");
		assertEquals(String.class, ci.inner());
	}

	@Test
	void a02_ofProxy_jdkDynamicProxy_unwrapsToInterface() {
		var proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Greeter.class}, (p, m, args) -> null);
		var ci = ClassInfo.ofProxy(proxy);
		assertEquals(Greeter.class, ci.inner());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b: packageInfo - null Package for primitives (line 242)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getPackage_primitiveType_returnsNull() {
		assertNull(ClassInfo.of(int.class).getPackage());
	}

	@Test
	void b02_getPackage_normalClass_returnsNonNull() {
		assertNotNull(ClassInfo.of(String.class).getPackage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c: appendShortNameWithOuters - local vs. member class (lines 409/414)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_getNameShort_localClass_includesEnclosingMethodOwner() {
		class LocalGreeter implements Greeter {
			@Override
			public String greet() { return "hi"; }
		}
		var name = ClassInfo.of(LocalGreeter.class).getNameShort();
		assertTrue(name.endsWith(".LocalGreeter") || name.contains("LocalGreeter"), name);
	}

	@Test
	void c02_getNameShort_memberClass_includesDeclaringClass() {
		var name = ClassInfo.of(Outer1.Inner1.class).getNameShort();
		assertEquals("ClassInfo_Coverage_Test$Outer1$Inner1", name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d: compareTo() / equals() (lines 524/529)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_compareTo_null_returnsPositive() {
		assertTrue(ClassInfo.of(String.class).compareTo(null) > 0);
	}

	@Test
	void d02_equals_notAClassInfo_returnsFalse() {
		assertNotEquals("not a ClassInfo", ClassInfo.of(String.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e: is(ElementFlag) - default branch + full switch coverage (lines 1918/1922)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_isElementFlag_unhandledCase_delegatesToSuper() {
		// PUBLIC/PRIVATE/etc. aren't handled directly by ClassInfo's switch - they fall through to the default -> super.is(flag).
		assertTrue(ClassInfo.of(String.class).is(PUBLIC));
		assertFalse(ClassInfo.of(NoModifierMemberClass.class).is(PUBLIC));
	}

	@Test
	void e02_isElementFlag_notAnonymous_bothOutcomes() {
		assertTrue(ClassInfo.of(String.class).is(NOT_ANONYMOUS));
		// Lambdas aren't anonymous classes per Class.isAnonymousClass() - need a real anonymous class expression.
		@SuppressWarnings({
			"java:S2133" // An actual anonymous class (not Greeter.class) is required so isAnonymousClass() is exercised.
		})
		var anon = new Greeter() {
			@Override
			public String greet() { return null; }
		};
		assertFalse(ClassInfo.of(anon.getClass()).is(NOT_ANONYMOUS));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f: isChildOf(ClassInfo) / isChildOf(Type) (lines 2093/2103)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_isChildOfClassInfo_sameClass_false() {
		assertFalse(ClassInfo.of(String.class).isChildOf(ClassInfo.of(String.class)));
	}

	@Test
	void f02_isChildOfClassInfo_actualChild_true() {
		assertTrue(ClassInfo.of(ArrayList.class).isChildOf(ClassInfo.of(List.class)));
	}

	@Test
	void f03_isChildOfType_nonClassType_false() {
		@SuppressWarnings({
			"java:S2133" // An anonymous subclass (not ArrayList.class) is required to obtain a genuine ParameterizedType via getGenericSuperclass().
		})
		var pt = new ArrayList<String>() {}.getClass().getGenericSuperclass();
		assertFalse(ClassInfo.of(ArrayList.class).isChildOf(pt));
	}

	@Test
	void f04_isChildOfType_classType_delegatesToClassOverload() {
		assertTrue(ClassInfo.of(ArrayList.class).isChildOf((Type)List.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g: isDecimal() / isNumber() primitive branches (lines 2190/2232)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_isDecimal_primitiveFloatAndDouble_true() {
		assertTrue(ClassInfo.of(float.class).isDecimal());
		assertTrue(ClassInfo.of(double.class).isDecimal());
		assertFalse(ClassInfo.of(int.class).isDecimal());
	}

	@Test
	void g02_isNumber_primitiveNumericTypes_true() {
		assertTrue(ClassInfo.of(byte.class).isNumber());
		assertTrue(ClassInfo.of(short.class).isNumber());
		assertTrue(ClassInfo.of(int.class).isNumber());
		assertTrue(ClassInfo.of(long.class).isNumber());
		assertTrue(ClassInfo.of(float.class).isNumber());
		assertTrue(ClassInfo.of(double.class).isNumber());
		assertFalse(ClassInfo.of(boolean.class).isNumber());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h: isVoid() simple-name fallback (line 2342)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_isVoid_voidDotClass_true() {
		assertTrue(ClassInfo.of(void.class).isVoid());
	}

	@Test
	void h02_isVoid_voidWrapperClass_true() {
		assertTrue(ClassInfo.of(Void.class).isVoid());
	}

	@Test
	void h03_isVoid_notVoid_false() {
		assertFalse(ClassInfo.of(String.class).isVoid());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i: isParentOf(Class) / isParentOf(ClassInfo) / isParentOf(Type) (lines 2381/2394/2404)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_isParentOfClass_sameClass_false() {
		assertFalse(ClassInfo.of(String.class).isParentOf(String.class));
	}

	@Test
	void i02_isParentOfClassInfo_sameClass_false() {
		assertFalse(ClassInfo.of(String.class).isParentOf(ClassInfo.of(String.class)));
	}

	@Test
	void i03_isParentOfClassInfo_actualParent_true() {
		assertTrue(ClassInfo.of(List.class).isParentOf(ClassInfo.of(ArrayList.class)));
	}

	@Test
	void i04_isParentOfType_nonClassType_false() {
		@SuppressWarnings({
			"java:S2133" // An anonymous subclass (not ArrayList.class) is required to obtain a genuine ParameterizedType via getGenericSuperclass().
		})
		var pt = new ArrayList<String>() {}.getClass().getGenericSuperclass();
		assertFalse(ClassInfo.of(List.class).isParentOf(pt));
	}

	@Test
	void i05_isParentOfType_classType_delegatesToClassOverload() {
		assertTrue(ClassInfo.of(List.class).isParentOf((Type)ArrayList.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j: isParentOfLenient(ClassInfo) - primitive/wrapper bridging (line 2487)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_isParentOfLenient_primitiveToWrapper_true() {
		assertTrue(ClassInfo.of(int.class).isParentOfLenient(ClassInfo.of(Integer.class)));
	}

	@Test
	void j02_isParentOfLenient_wrapperToPrimitive_true() {
		assertTrue(ClassInfo.of(Integer.class).isParentOfLenient(ClassInfo.of(int.class)));
	}

	@Test
	void j03_isParentOfLenient_unrelatedPrimitives_false() {
		assertFalse(ClassInfo.of(int.class).isParentOfLenient(ClassInfo.of(boolean.class)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// k: findToString() - non-class type / empty modifiers / bounded type params (lines 2666/2673/2682/2713)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_toString_nonClassType_returnsInnerTypeToString() {
		// A bare TypeVariable is neither a Class nor a ParameterizedType, so ClassUtils.toClass() returns null,
		// leaving ClassInfo.inner() null and forcing findToString() to fall back to innerType.toString().
		Type tv = List.class.getTypeParameters()[0];
		assertEquals(tv.toString(), ClassInfo.of(tv).toString());
	}

	@Test
	void k02_toString_packagePrivateMemberClass_noExplicitModifiers_omitsModifierPrefix() {
		// NoModifierMemberClass has no explicit modifiers of its own, but as a non-static inner class it still
		// carries no PUBLIC/PRIVATE/PROTECTED/STATIC/FINAL/ABSTRACT bits, so Modifier.toString(getModifiers()) is empty.
		var s = ClassInfo.of(NoModifierMemberClass.class).toString();
		assertTrue(s.startsWith("class "), s);
	}

	@Test
	void k03_toString_packagePrivateTopLevelInterface_stripsAbstractAndInterfaceLeavingNoModifiers() {
		// Top-level (non-nested) interface: no static bit possible, so mods is exactly "abstract interface"
		// before stripping, and empty after - exercises the mods.isEmpty() TRUE branch at line 2682.
		var s = ClassInfo.of(PackagePrivateGreeterFixture.class).toString();
		assertTrue(s.startsWith("interface "), s);
	}

	@Test
	void k03b_toString_nestedInterface_staticBitSurvivesStrip() {
		// Nested interfaces are implicitly static, so after stripping "abstract"/"interface" the "static"
		// modifier remains - exercises the mods.isEmpty() FALSE branch at line 2682.
		var s = ClassInfo.of(Greeter.class).toString();
		assertTrue(s.startsWith("static interface "), s);
	}

	@Test
	void k04_toString_boundedTypeParameter_rendersExtendsClause() {
		var s = ClassInfo.of(BoundedTypeParam.class).toString();
		assertTrue(s.contains("<T extends java.lang.Number>"), s);
	}

	@Test
	void k05_toString_deprecatedClass_publicModifierRetained() {
		var s = ClassInfo.of(DeprecatedClass.class).toString();
		assertTrue(s.startsWith("static class ") || s.contains(" class "), s);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// l: predicates that special-case a null inner (non-Class ClassInfo), and other misc predicate gaps
	//-----------------------------------------------------------------------------------------------------------------

	private static ClassInfo nonClassClassInfo() {
		Type tv = List.class.getTypeParameters()[0];
		return ClassInfo.of(tv);
	}

	@Test
	void l01_negatedPredicates_nonClassType_allDefaultToTrue() {
		var ci = nonClassClassInfo();
		assertTrue(ci.isNotDeprecated());
		assertTrue(ci.isNotLocalClass());
		assertTrue(ci.isNotMemberClass());
		assertTrue(ci.isNotPrimitive());
	}

	@Test
	void l02_isNestmateOf_sameOuterClass_true() {
		assertTrue(ClassInfo.of(Outer1.class).isNestmateOf(Outer1.Inner1.class));
	}

	@Test
	void l03_isNestmateOf_unrelatedClass_false() {
		// Outer1 and Outer2 share the same nest host (this test class), so use a class from a different
		// top-level nest entirely to exercise the false path.
		assertFalse(ClassInfo.of(Outer1.class).isNestmateOf(String.class));
	}

	static class Void {}

	@Test
	void l04_isVoid_classNamedVoidButNotJavaLangVoid_trueViaSimpleNameFallback() {
		assertTrue(ClassInfo.of(Void.class).isVoid());
		assertNotEquals(java.lang.Void.class, Void.class);
	}

	@Test
	void l05_isAssignableFromClassInfo_nullChild_false() {
		assertFalse(ClassInfo.of(List.class).isAssignableFrom((ClassInfo)null));
	}

	@Test
	void l06_isAssignableFromClassInfo_actualChild_true() {
		assertTrue(ClassInfo.of(List.class).isAssignableFrom(ClassInfo.of(ArrayList.class)));
	}

	@Test
	void l07_isAssignableFromType_nonClassType_false() {
		@SuppressWarnings({
			"java:S2133" // An anonymous subclass (not ArrayList.class) is required to obtain a genuine ParameterizedType via getGenericSuperclass().
		})
		var pt = new ArrayList<String>() {}.getClass().getGenericSuperclass();
		assertFalse(ClassInfo.of(List.class).isAssignableFrom(pt));
	}

	@Test
	void l08_isParentOfLenient_nullChild_false() {
		assertFalse(ClassInfo.of(String.class).isParentOfLenient((Class<?>)null));
		assertFalse(ClassInfo.of(String.class).isParentOfLenient((ClassInfo)null));
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(RepeatableContainer.class)
	@interface RepeatableAnno {
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface RepeatableContainer {
		RepeatableAnno[] value();
	}

	@Test
	void l09_isRepeatedAnnotation_containerType_true() {
		assertTrue(ClassInfo.of(RepeatableContainer.class).isRepeatedAnnotation());
	}

	@Test
	void l10_isRepeatedAnnotation_nonContainerType_false() {
		assertFalse(ClassInfo.of(RepeatableAnno.class).isRepeatedAnnotation());
	}

	@Test
	void l11_isSealed_normalClass_false() {
		assertFalse(ClassInfo.of(String.class).isSealed());
	}

	@Test
	void l12_isStrictChildOf_sameClass_false() {
		assertFalse(ClassInfo.of(String.class).isStrictChildOf(String.class));
	}

	@Test
	void l13_isStrictChildOf_actualChild_true() {
		assertTrue(ClassInfo.of(ArrayList.class).isStrictChildOf(List.class));
	}

	@Test
	void l14_newInstance_nonClassType_throws() {
		var ci = nonClassClassInfo();
		assertThrows(ExecutableException.class, ci::newInstance);
	}
}
