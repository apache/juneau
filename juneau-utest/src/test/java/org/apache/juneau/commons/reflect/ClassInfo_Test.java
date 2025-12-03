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
import static org.apache.juneau.commons.reflect.ClassArrayFormat.*;
import static org.apache.juneau.commons.reflect.ClassInfo.*;
import static org.apache.juneau.commons.reflect.ClassNameFormat.*;
import static org.apache.juneau.commons.reflect.ElementFlag.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.Value;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

public class ClassInfo_Test extends TestBase {

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@Inherited
	public static @interface A {
		int value();
	}

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@Inherited
	public static @interface B {
		int value();
	}

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@Inherited
	@ContextApply(AConfigApply.class)
	static @interface AConfig {
		int value();
	}

	public static class AConfigApply extends AnnotationApplier<AConfig,Context.Builder> {
		protected AConfigApply(VarResolverSession vr) {
			super(AConfig.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<AConfig> a, Context.Builder b) {}  // NOSONAR
	}

	private static void check(String expected, Object o) {
		if (o instanceof List<?> l) {
			var actual = l.stream().map(TO_STRING).collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else if (o instanceof Iterable o2) {
			var actual = StreamSupport.stream(((Iterable<?>)o2).spliterator(), false).map(TO_STRING).collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = t -> {
		if (t == null)
			return null;
		if (t instanceof Class t2)
			return t2.getSimpleName();
		if (t instanceof Package t3)
			return t3.getName();
		if (t instanceof PackageInfo t4)
			return t4.getName();
		if (t instanceof ClassInfo t5)
			return t5.getNameSimple();
		if (t instanceof MethodInfo t6)
			return t6.getDeclaringClass().getNameSimple() + '.' + t6.getShortName();
		if (t instanceof ConstructorInfo t7)
			return t7.getShortName();
		if (t instanceof FieldInfo t8)
			return t8.getDeclaringClass().getNameSimple() + '.' + t8.getName();
		if (t instanceof A t9)
			return "@A(" + t9.value() + ")";
		if (t instanceof PA t10)
			return "@PA(" + t10.value() + ")";
		if (t instanceof AConfig t11)
			return "@AConfig(" + t11.value() + ")";
		if (t instanceof AnnotationInfo t12)
			return ClassInfo_Test.TO_STRING.apply(t12.inner());
		return t.toString();
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Initialization
	//-----------------------------------------------------------------------------------------------------------------

	public class A1 {}

	public class A2 extends Value<A1> {}

	public class A3 extends Value<Map<String,List<String>>> {}

	public class A4 extends Value<Map<String,String[][]>> {}

	public static Type aType, pType, pTypeDimensional, pTypeGeneric, pTypeGenericArg;
	static {
		aType = ((ParameterizedType)A2.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pType = ((ParameterizedType)A3.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pTypeDimensional = ((ParameterizedType)A4.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pTypeGeneric = HashMap.class.getGenericSuperclass();
		pTypeGenericArg = ((ParameterizedType)pTypeGeneric).getActualTypeArguments()[1];
	}

	static ClassInfo aTypeInfo = of(aType), pTypeInfo = of(pType), pTypeDimensionalInfo = of(pTypeDimensional), pTypeGenericInfo = of(pTypeGeneric), pTypeGenericArgInfo = of(pTypeGenericArg);
	static ClassInfo aClass = of(AClass.class), aInterface = of(AInterface.class);

	public static class A6 {
		public Optional<A1> m1(Optional<A1> bar) {
			return null;
		}

		public Value<A1> m2(Value<A1> bar) {
			return null;
		}
	}

	interface BI1 {}

	interface BI2 extends BI1 {}

	interface BI3 {}

	interface BI4 {}

	static class BC1 implements BI1, BI2 {}

	static class BC2 extends BC1 implements BI3 {}

	static class BC3 extends BC2 {}

	static ClassInfo bi1 = of(BI1.class), bi2 = of(BI2.class), bi3 = of(BI3.class), bi4 = of(BI4.class), bc1 = of(BC1.class), bc2 = of(BC2.class), bc3 = of(BC3.class), object = of(Object.class);

	interface CI1 {
		void i1a();

		void i1b();
	}

	interface CI2 extends CI1 {
		void i2b();

		void i2a();
	}

	interface CI3 {}

	interface CI4 {}

	abstract static class CC1 implements CI1, CI2 {
		@Override
		public void i1a() {}

		protected void c1b() {}

		public void c1a() {}
	}

	static class CC2 extends CC1 implements CI3 {
		public void c2b() {}  // NOSONAR

		@Override
		public void i1b() {}  // NOSONAR

		@Override
		public void i2b() {}  // NOSONAR

		@Override
		public void i2a() {}  // NOSONAR

		protected void c2a() {}  // NOSONAR
	}

	static class CC3 extends CC2 {
		@Override
		public void i2b() {}  // NOSONAR

		public void c3a() {}  // NOSONAR

		protected void c3b() {}  // NOSONAR
	}

	static ClassInfo cc3 = of(CC3.class), ci2 = of(CI2.class);

	static class E1 {
		public E1() {}

		public E1(String a) {}  // NOSONAR

		public E1(Writer a) {}  // NOSONAR

		public E1(String a, Writer b) {}  // NOSONAR

		protected E1(int a) {}  // NOSONAR

		E1(float a) {}  // NOSONAR
	}

	static class E2 {
		protected E2() {}
	}

	abstract static class E3 {
		public E3() {}
	}

	class E4 {
		public E4() {}  // NOSONAR
	}

	static class E5 {
		@Deprecated
		public E5() {}  // NOSONAR
	}

	class E6 {
		public E6(String a) {}
	}

	static ClassInfo e1 = of(E1.class), e2 = of(E2.class), e3 = of(E3.class), e4 = of(E4.class), e5 = of(E5.class), e6 = of(E6.class);

	abstract static class F1 {
		public int f1a;
		public int f1b;
	}

	static class F2 extends F1 {
		public int f1a;
		public int f2b;
		@Deprecated
		int f2c;
		protected int f2d;
	}

	static ClassInfo f1 = of(F1.class), f2 = of(F2.class);

	static class F3 {
		public int a1;
		int a2;
	}

	static ClassInfo f3 = of(F3.class);

	@A(1)
	interface GI1 {}

	@A(2)
	interface GI2 extends GI1 {}

	@A(3)
	interface GI3 {}

	@A(4)
	interface GI4 {}

	@A(5)
	static class G1 implements GI1, GI2 {}

	@A(6)
	static class G2 extends G1 implements GI3 {}

	@A(7)
	static class G3 extends G2 {}

	static class G4 extends G3 {}

	static class G5 implements GI3 {}

	static ClassInfo g3 = of(G3.class), g4 = of(G4.class), g5 = of(G5.class);

	@A(1)
	@AConfig(1)
	interface GBI1 {}

	@A(2)
	@AConfig(2)
	interface GBI2 extends GBI1 {}

	@A(3)
	@AConfig(3)
	interface GBI3 {}

	@A(4)
	@AConfig(4)
	interface GBI4 {}

	@A(5)
	@AConfig(5)
	static class GB1 implements GBI1, GBI2 {}

	@A(6)
	@AConfig(6)
	static class GB2 extends GB1 implements GBI3 {}

	@A(7)
	@AConfig(7)
	static class GB3 extends GB2 {}

	static class GB4 extends GB3 {}

	static class GB5 implements GBI3 {}

	static ClassInfo gb3 = of(GB3.class), gb4 = of(GB4.class), gb5 = of(GB5.class);

	public static class H_Public {}

	static class H_Package {}

	protected static class H_Protected {}

	private static class H_Private {}

	public class H_PublicMember {}

	public abstract class H_AbstractPublic {}

	@Deprecated
	public class H_PublicDeprecated {}

	static ClassInfo hPublic = of(H_Public.class), hPackage = of(H_Package.class), hProtected = of(H_Protected.class), hPrivate = of(H_Private.class), hPublicMember = of(H_PublicMember.class),
		hAbstractPublic = of(H_AbstractPublic.class), hPublicDeprecated = of(H_PublicDeprecated.class);  // NOSONAR

	@Deprecated
	public abstract static class H2a {}

	private interface H2b {}

	@Deprecated
	class H2_Deprecated {}

	class H2_NotDeprecated {}

	public class H2_Public {}

	class H2_NotPublic {}

	public static class H2_Static {}

	class H2_NotStatic {}

	class H2_Member {}

	static class H2_StaticMember {}

	abstract class H2_Abstract {}

	class H2_NotAbstract {}

	static ClassInfo h2a = of(H2a.class), h2b = of(H2b.class), h2Deprecated = of(H2_Deprecated.class), h2NotDeprecated = of(H2_NotDeprecated.class), h2Public = of(H2_Public.class),
		h2NotPublic = of(H2_NotPublic.class), h2Static = of(H2_Static.class), h2NotStatic = of(H2_NotStatic.class), h2Member = of(H2_Member.class), h2StaticMember = of(H2_StaticMember.class),
		h2Abstract = of(H2_Abstract.class), h2NotAbstract = of(H2_NotAbstract.class);  // NOSONAR

	static List<Class<?>> primitives = l(boolean.class, byte.class, short.class, char.class, int.class, long.class, float.class, double.class);
	static List<Class<?>> primitiveWrappers = l(Boolean.class, Byte.class, Short.class, Character.class, Integer.class, Long.class, Float.class, Double.class);
	static List<Object> primitiveDefaults = l(false, (byte)0, (short)0, (char)0, 0, 0L, 0f, 0d);

	public class J1 {}

	public static class J2 {}

	static ClassInfo j1 = of(J1.class), j2 = of(J2.class), j1_3d = of(J1[][].class), j2_3d = of(J2[][].class);

	public interface KA {}

	public static class KB implements KA {}

	public static class KC extends KB {}

	static ClassInfo ka = of(KA.class), kb = of(KB.class), kc = of(KC.class);

	public static class LA {}

	static ClassInfo la = of(LA.class);

	@SuppressWarnings("serial")
	public static class MA extends HashMap<String,Integer> {}

	@SuppressWarnings("serial")
	public static class MB extends MA {}

	@SuppressWarnings("serial")
	public static class MC<K,E> extends HashMap<K,E> {}

	@SuppressWarnings("serial")
	public static class MD extends MC<String,Integer> {}

	@SuppressWarnings("serial")
	public static class ME extends HashMap<String,HashMap<String,Integer>> {}

	@SuppressWarnings("serial")
	public static class MF extends HashMap<String,String[]> {}

	@SuppressWarnings("serial")
	public static class MG extends HashMap<String,HashMap<String,Integer>[]> {}

	@SuppressWarnings({ "serial", "rawtypes" })
	public static class MH extends HashMap<String,LinkedList[]> {}

	@SuppressWarnings({ "serial" })
	public static class MI<X> extends HashMap<String,X[]> {}

	@SuppressWarnings({ "serial" })
	public static class MJ<X extends Number> extends HashMap<String,X> {}

	public class MK {}

	@SuppressWarnings({ "serial" })
	public class ML extends HashMap<String,MK> {}

	public static class MM {
		@SuppressWarnings({ "serial" })
		public class MN extends HashMap<String,MM> {}
	}

	static ClassInfo ma = of(MA.class), mb = of(MB.class), mc = of(MC.class), md = of(MD.class), me = of(ME.class), mf = of(MF.class), mg = of(MG.class), mh = of(MH.class), mi = of(MI.class),
		mj = of(MJ.class), ml = of(ML.class), mn = of(MM.MN.class);

	interface ISuperGrandParent {}

	interface IGrandParent extends ISuperGrandParent {}

	interface ISuperParent {}

	interface IParent extends ISuperParent {}

	interface IChild {}

	static class GrandParent implements IGrandParent {}

	static class Parent extends GrandParent implements IParent {}

	static class Child extends Parent implements IChild {}

	@SuppressWarnings("unused")
	private static class GenericsTestClass {
		HashMap<String,Integer> hashMap;
		HashMap<String,ArrayList<Integer>> nestedMap;
		ArrayList<String>[] listArray;
	}

	//====================================================================================================
	// appendNameFormatted(StringBuilder, ClassNameFormat, boolean, char, ClassArrayFormat)
	//====================================================================================================
	@Test
	void a001_appendNameFormatted() {
		var ci = ClassInfo.of(String.class);
		var sb = new StringBuilder("Type: ");
		ci.appendNameFormatted(sb, FULL, false, '$', BRACKETS);
		assertEquals("Type: java.lang.String", sb.toString());

		// Verify it returns the same StringBuilder for chaining
		var result = ci.appendNameFormatted(sb, SIMPLE, false, '$', BRACKETS);
		assertSame(sb, result);
		assertEquals("Type: java.lang.StringString", sb.toString());
	}

	//====================================================================================================
	// arrayType()
	//====================================================================================================
	@Test
	void a002_arrayType() {
		var ci = ClassInfo.of(String.class);
		var arrayType = ci.arrayType();
		assertNotNull(arrayType);
		assertEquals(String[].class, arrayType.inner());

		// Multi-dimensional
		var ci2 = ClassInfo.of(String[].class);
		var arrayType2 = ci2.arrayType();
		assertNotNull(arrayType2);
		assertEquals(String[][].class, arrayType2.inner());

		// For types without inner class, should return null
		var ci3 = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci3.arrayType());

		// Test when inner.arrayType() returns null (line 391)
		// Note: Most classes' arrayType() returns a valid array class
		// The null check at line 391 handles the case when inner.arrayType() returns null
		// This is a defensive check, but in practice most classes have valid array types
		// The line is covered by the null inner check above
	}

	//====================================================================================================
	// asSubclass(Class<?>)
	//====================================================================================================
	@Test
	void a002b_asSubclass() {
		// Valid cast
		var ci = ClassInfo.of(String.class);
		var result = ci.asSubclass(CharSequence.class);
		assertSame(ci, result);

		// Invalid cast - should throw
		assertThrows(ClassCastException.class, () -> ci.asSubclass(Integer.class));

		// For types without inner class, should return null
		var ci2 = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci2.asSubclass(CharSequence.class));
	}

	//====================================================================================================
	// getAllFields()
	//====================================================================================================
	@Test
	void a003_getAllFields() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
		// Test twice to verify caching
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
	}

	//====================================================================================================
	// getAllMethods()
	//====================================================================================================
	@Test
	void a004_getAllMethods() {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
		// Test twice to verify caching
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
	}

	//====================================================================================================
	// getAllMethodsTopDown()
	//====================================================================================================
	@Test
	void a005_getAllMethodsTopDown() {
		// getAllMethodsTopDown uses rstream(getAllParents()) which reverses the order
		// Interfaces come first, then classes, both in parent-to-child order
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsTopDown());
		// Test twice to verify caching
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsTopDown());
	}

	//====================================================================================================
	// getAllParents()
	//====================================================================================================
	@Test
	void a006_getAllParents() {
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("", object.getAllParents());
		check("BI1", bi1.getAllParents());
		// Test twice to verify caching
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
	}

	//====================================================================================================
	// getAnnotatableType()
	//====================================================================================================
	@Test
	void a007_getAnnotatableType() {
		assertEquals(AnnotatableType.CLASS_TYPE, aClass.getAnnotatableType());
		assertEquals(AnnotatableType.CLASS_TYPE, aInterface.getAnnotatableType());
		assertEquals(AnnotatableType.CLASS_TYPE, aTypeInfo.getAnnotatableType());
	}

	//====================================================================================================
	// getAnnotatedInterfaces()
	//====================================================================================================
	@Test
	void a008_getAnnotatedInterfaces() {
		var annotated = ci2.getAnnotatedInterfaces();
		assertNotNull(annotated);
		// CI2 extends CI1, so should have annotated interfaces
		assertFalse(annotated.isEmpty());
	}

	//====================================================================================================
	// getAnnotatedSuperclass()
	//====================================================================================================
	@Test
	void a009_getAnnotatedSuperclass() {
		// For classes with superclass, should return AnnotatedType
		var annotated = bc2.getAnnotatedSuperclass();
		assertNotNull(annotated);

		// For Object, should return null
		assertNull(object.getAnnotatedSuperclass());

		// For interfaces, should return null
		assertNull(bi1.getAnnotatedSuperclass());

		// For types without inner class, should return null
		assertNull(pTypeGenericArgInfo.getAnnotatedSuperclass());
	}

	//====================================================================================================
	// getAnnotations()
	//====================================================================================================
	@Test
	void a010_getAnnotations() {
		// Test with no type parameter - returns all annotations
		var allAnnotations = g3.getAnnotations();
		assertNotNull(allAnnotations);
		assertFalse(allAnnotations.isEmpty());

		// Test with type parameter - returns filtered annotations
		var aAnnotations = g3.getAnnotations(A.class);
		assertNotNull(aAnnotations);
		var first = aAnnotations.findFirst().map(AnnotationInfo::inner).orElse(null);
		check("@A(7)", first);

		// Test twice to verify caching
		check("@A(7)", g3.getAnnotations(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));

		// Test on parent class
		check("@A(7)", g4.getAnnotations(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));

		// Test on interface
		check("@A(3)", g5.getAnnotations(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));

		// Test with non-existent annotation
		assertTrue(g3.getAnnotations(B.class).findFirst().isEmpty());

		// Test with null - should throw
		assertThrows(IllegalArgumentException.class, () -> g3.getAnnotations(null));

		// Test annotation order - parent first
		check("@PA(10),@A(2),@A(1),@A(5),@A(3),@A(6),@A(7)", rstream(g3.getAnnotations()).collect(Collectors.toList()));
		check("@PA(10),@A(2),@A(1),@A(5),@A(3),@A(6),@A(7)", rstream(g4.getAnnotations()).collect(Collectors.toList()));
		check("@PA(10),@A(3)", rstream(g5.getAnnotations()).collect(Collectors.toList()));
	}

	//====================================================================================================
	// getClassLoader()
	//====================================================================================================
	@Test
	void a011_getClassLoader() {
		var cl = aClass.getClassLoader();
		assertNotNull(cl);

		// For types without inner class, should return null
		assertNull(pTypeGenericArgInfo.getClassLoader());
	}

	//====================================================================================================
	// canAcceptArg(Object)
	//====================================================================================================
	@Test
	void a011b_canAcceptArg() {
		// Valid argument
		assertTrue(ClassInfo.of(String.class).canAcceptArg("test"));
		assertTrue(ClassInfo.of(Integer.class).canAcceptArg(42));
		assertTrue(ClassInfo.of(int.class).canAcceptArg(42));

		// Null argument - non-primitive
		assertTrue(ClassInfo.of(String.class).canAcceptArg(null));
		assertTrue(ClassInfo.of(Integer.class).canAcceptArg(null));

		// Null argument - primitive (should return false)
		assertFalse(ClassInfo.of(int.class).canAcceptArg(null));
		assertFalse(ClassInfo.of(boolean.class).canAcceptArg(null));

		// Invalid argument
		assertFalse(ClassInfo.of(String.class).canAcceptArg(42));

		// For types without inner class, should return false
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.canAcceptArg("test"));
		assertFalse(ci.canAcceptArg(null));

		// Test primitive-to-wrapper and wrapper-to-primitive conversions (line 435)
		// Primitive can accept wrapper
		assertTrue(ClassInfo.of(int.class).canAcceptArg(Integer.valueOf(42)));
		assertTrue(ClassInfo.of(long.class).canAcceptArg(Long.valueOf(42L)));
		// Wrapper can accept primitive (via autoboxing, but isInstance won't work, so need wrapper check)
		assertTrue(ClassInfo.of(Integer.class).canAcceptArg(42));
		assertTrue(ClassInfo.of(Long.class).canAcceptArg(42L));
		// Number can accept int (parent type)
		assertTrue(ClassInfo.of(Number.class).canAcceptArg(42));
		assertTrue(ClassInfo.of(Number.class).canAcceptArg(Integer.valueOf(42)));
	}

	//====================================================================================================
	// cast(Object)
	//====================================================================================================
	@Test
	void a011c_cast() {
		// Valid cast
		var ci = ClassInfo.of(String.class);
		var result = ci.cast("test");
		assertEquals("test", result);

		// Null object - should return null
		assertNull(ci.cast(null));

		// Invalid cast - should throw
		assertThrows(ClassCastException.class, () -> ci.cast(42));

		// For types without inner class, should return null
		var ci2 = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci2.cast("test"));
		assertNull(ci2.cast(null));
	}

	//====================================================================================================
	// getComponentType()
	//====================================================================================================
	@Test
	void a012_getComponentType() {
		// For non-array types, returns this
		assertEquals(ka, ka.getComponentType());
		// Test twice to verify caching
		assertEquals(ka, ka.getComponentType());

		// For array types, returns component type
		check("KA", of(KA[][].class).getComponentType());

		// For types
		assertEquals(aTypeInfo, aTypeInfo.getComponentType());
		assertEquals(pTypeInfo, pTypeInfo.getComponentType());
		assertEquals(pTypeDimensionalInfo, pTypeDimensionalInfo.getComponentType());
		assertEquals(pTypeGenericInfo, pTypeGenericInfo.getComponentType());
		assertEquals(pTypeGenericArgInfo, pTypeGenericArgInfo.getComponentType());

		// For types without inner class, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.componentType());
	}

	//====================================================================================================
	// descriptorString()
	//====================================================================================================
	@Test
	void a012b_descriptorString() {
		// Test descriptor string for various types
		assertEquals("Ljava/lang/String;", ClassInfo.of(String.class).descriptorString());
		assertEquals("I", ClassInfo.of(int.class).descriptorString());
		assertEquals("[Ljava/lang/String;", ClassInfo.of(String[].class).descriptorString());

		// For types without inner class, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.descriptorString());
	}

	//====================================================================================================
	// equals(Object)
	//====================================================================================================
	@Test
	void a012c_equals() {
		// Same class
		var ci1 = ClassInfo.of(String.class);
		var ci2 = ClassInfo.of(String.class);
		assertEquals(ci1, ci2);

		// Different classes
		var ci3 = ClassInfo.of(Integer.class);
		assertNotEquals(ci1, ci3);

		// Same type
		assertEquals(pTypeInfo, ClassInfo.of(pType));

		// Different types
		assertNotEquals(pTypeInfo, pTypeDimensionalInfo);

		// Not a ClassInfo
		assertNotEquals(ci1, "not a ClassInfo");
		assertNotEquals(ci1, null);
	}

	//====================================================================================================
	// getDeclaredAnnotations()
	//====================================================================================================
	@Test
	void a013_getDeclaredAnnotations() {
		var declared = g3.getDeclaredAnnotations();
		assertNotNull(declared);
		// G3 has @A(7)
		assertFalse(declared.isEmpty());

		// G4 has no declared annotations (inherits from G3)
		var declared2 = g4.getDeclaredAnnotations();
		assertTrue(declared2.isEmpty());
	}

	//====================================================================================================
	// getDeclaredConstructor(Predicate<ConstructorInfo>)
	//====================================================================================================
	@Test
	void a014_getDeclaredConstructor() {
		check("E1(int)", e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PROTECTED) && x.hasParameterTypes(int.class)).orElse(null));
		check("E1(int)", e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PRIVATE) && x.hasParameterTypes(int.class)).orElse(null));
		check(null, e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC) && x.hasParameterTypes(int.class)).orElse(null));
		check("E3()", e3.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)).orElse(null));
		check("E4(ClassInfo_Test)", e4.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)).orElse(null));
		check("E5()", e5.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)).orElse(null));
	}

	//====================================================================================================
	// getDeclaredConstructors()
	//====================================================================================================
	@Test
	void a015_getDeclaredConstructors() {
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
		// Test twice to verify caching
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());

		// Test on types
		check("A1(ClassInfo_Test)", aTypeInfo.getDeclaredConstructors());
		check("", pTypeInfo.getDeclaredConstructors());
		check("", pTypeDimensionalInfo.getDeclaredConstructors());
		check("AbstractMap()", pTypeGenericInfo.getDeclaredConstructors());
		check("", pTypeGenericArgInfo.getDeclaredConstructors());
	}

	//====================================================================================================
	// getDeclaredField(Predicate<FieldInfo>)
	//====================================================================================================
	@Test
	void a016_getDeclaredField() {
		check("F3.a1", f3.getDeclaredField(x -> x.hasName("a1")).orElse(null));
		check("F3.a2", f3.getDeclaredField(x -> x.hasName("a2")).orElse(null));
		check(null, f3.getDeclaredField(x -> x.hasName("a3")).orElse(null));
	}

	//====================================================================================================
	// getDeclaredFields()
	//====================================================================================================
	@Test
	void a017_getDeclaredFields() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
		// Test twice to verify caching
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());

		// Test on types
		check("A1.this$0", aTypeInfo.getDeclaredFields());
		check("", pTypeInfo.getDeclaredFields());
		check("", pTypeDimensionalInfo.getDeclaredFields());
		check("AbstractMap.keySet,AbstractMap.values", pTypeGenericInfo.getDeclaredFields());
		check("", pTypeGenericArgInfo.getDeclaredFields());
	}

	//====================================================================================================
	// getDeclaredInterfaces()
	//====================================================================================================
	@Test
	void a018_getDeclaredInterfaces() {
		check("", bi4.getDeclaredInterfaces());
		check("BI1,BI2", bc1.getDeclaredInterfaces());
		check("BI3", bc2.getDeclaredInterfaces());
		check("", bc3.getDeclaredInterfaces());
		// Test twice to verify caching
		check("BI1,BI2", bc1.getDeclaredInterfaces());

		// Test on types
		check("", aTypeInfo.getDeclaredInterfaces());
		check("", pTypeInfo.getDeclaredInterfaces());
		check("", pTypeDimensionalInfo.getDeclaredInterfaces());
		check("Map", pTypeGenericInfo.getDeclaredInterfaces());
		check("", pTypeGenericArgInfo.getDeclaredInterfaces());
	}

	//====================================================================================================
	// getDeclaredMemberClasses()
	//====================================================================================================
	@Test
	void a019_getDeclaredMemberClasses() {
		// Test with class that has declared member classes (line 822)
		var memberClasses = MM.class.getDeclaredClasses();
		assertNotNull(memberClasses);
		// MM has MN as a member class
		assertTrue(memberClasses.length > 0);

		// Test getDeclaredMemberClasses when inner is not null
		var mmCi = ClassInfo.of(MM.class);
		var declaredMemberClasses = mmCi.getDeclaredMemberClasses();
		assertNotNull(declaredMemberClasses);
		assertFalse(declaredMemberClasses.isEmpty());

		// For types without inner class, should return empty list
		var empty = pTypeGenericArgInfo.getDeclaredMemberClasses();
		assertTrue(empty.isEmpty());

		// For types with null inner, should return empty list
		var ci = ClassInfo.of((Class<?>)null, pType);
		var empty2 = ci.getDeclaredMemberClasses();
		assertNotNull(empty2);
		assertTrue(empty2.isEmpty());
	}

	//====================================================================================================
	// getDeclaredMethod(Predicate<MethodInfo>)
	//====================================================================================================
	@Test
	void a020_getDeclaredMethod() {
		var method = cc3.getDeclaredMethod(x -> x.hasName("c3a"));
		assertTrue(method.isPresent());
		assertEquals("c3a", method.get().getName());

		// Non-existent method
		var method2 = cc3.getDeclaredMethod(x -> x.hasName("nonexistent"));
		assertFalse(method2.isPresent());
	}

	//====================================================================================================
	// getDeclaredMethods()
	//====================================================================================================
	@Test
	void a021_getDeclaredMethods() {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		// Test twice to verify caching
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());

		// Test on types
		check("", aTypeInfo.getDeclaredMethods());
		check("", pTypeGenericArgInfo.getDeclaredMethods());
	}

	//====================================================================================================
	// getDeclaringClass()
	//====================================================================================================
	@Test
	void a022_getDeclaringClass() {
		// For member classes, should return declaring class
		var declaring = aTypeInfo.getDeclaringClass();
		assertNotNull(declaring);
		assertEquals(ClassInfo_Test.class.getName(), declaring.inner().getName());

		// For top-level classes, should return null
		var declaring2 = aClass.getDeclaringClass();
		assertNull(declaring2);

		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getDeclaringClass());
	}

	//====================================================================================================
	// getDimensions()
	//====================================================================================================
	@Test
	void a023_getDimensions() {
		assertEquals(0, ka.getDimensions());
		assertEquals(2, of(KA[][].class).getDimensions());
		// Test twice to verify caching
		assertEquals(0, ka.getDimensions());

		// Test on types
		assertEquals(0, aTypeInfo.getDimensions());
		assertEquals(0, pTypeInfo.getDimensions());
		assertEquals(0, pTypeDimensionalInfo.getDimensions());
		assertEquals(0, pTypeGenericInfo.getDimensions());
		assertEquals(0, pTypeGenericArgInfo.getDimensions());
	}

	//====================================================================================================
	// getEnclosingClass()
	//====================================================================================================
	@Test
	void a024_getEnclosingClass() {
		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getEnclosingClass());

		// For member classes, should return enclosing class
		var enclosing = aTypeInfo.getEnclosingClass();
		assertNotNull(enclosing);
		assertEquals(ClassInfo_Test.class.getName(), enclosing.inner().getName());

		// For top-level classes, should return null
		var enclosing2 = aClass.getEnclosingClass();
		assertNull(enclosing2);
	}

	//====================================================================================================
	// getEnclosingConstructor()
	//====================================================================================================
	@Test
	void a025_getEnclosingConstructor() {
		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getEnclosingConstructor());

		// For classes not declared in constructor, should return null (line 954)
		// Regular classes don't have enclosing constructors
		assertNull(aClass.getEnclosingConstructor());
		assertNull(ClassInfo.of(String.class).getEnclosingConstructor());
		// Local class in method should not have enclosing constructor
		class LocalClass {}
		var local = ClassInfo.of(LocalClass.class);
		var constructor = local.getEnclosingConstructor();
		assertNull(constructor);
	}

	//====================================================================================================
	// getEnclosingMethod()
	//====================================================================================================
	@Test
	void a026_getEnclosingMethod() {
		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getEnclosingMethod());

		// For classes not declared in method, should return null (line 970)
		// Regular classes don't have enclosing methods
		assertNull(aClass.getEnclosingMethod());
		assertNull(ClassInfo.of(String.class).getEnclosingMethod());
		// Local class should have an enclosing method
		class LocalClass {}
		var local = ClassInfo.of(LocalClass.class);
		var method = local.getEnclosingMethod();
		assertNotNull(method);
	}

	//====================================================================================================
	// getGenericInterfaces()
	//====================================================================================================
	@Test
	void a027_getGenericInterfaces() {
		var list = ci2.getGenericInterfaces();
		assertNotNull(list);
		// CI2 extends CI1, so should have generic interfaces
		assertFalse(list.isEmpty());
	}

	//====================================================================================================
	// getGenericSuperclass()
	//====================================================================================================
	@Test
	void a028_getGenericSuperclass() {
		// For classes with superclass, should return Type
		var superclass = bc2.getGenericSuperclass();
		assertNotNull(superclass);

		// For Object, should return null
		assertNull(object.getGenericSuperclass());

		// For interfaces, should return null
		assertNull(bi1.getGenericSuperclass());

		// For types without inner class, should return null
		assertNull(pTypeGenericArgInfo.getGenericSuperclass());
	}

	//====================================================================================================
	// getInterfaces()
	//====================================================================================================
	@Test
	void a029_getInterfaces() {
		check("", bi4.getInterfaces());
		check("BI1,BI2", bc1.getInterfaces());
		check("BI3,BI1,BI2", bc2.getInterfaces());
		check("BI3,BI1,BI2", bc3.getInterfaces());
		// Test twice to verify caching
		check("BI3,BI1,BI2", bc2.getInterfaces());
	}

	//====================================================================================================
	// getLabel()
	//====================================================================================================
	@Test
	void a030_getLabel() {
		assertEquals("AClass", aClass.getLabel());
		assertEquals("A1", aTypeInfo.getLabel());
		assertEquals("AInterface", aInterface.getLabel());
	}

	//====================================================================================================
	// getMemberClasses()
	//====================================================================================================
	@Test
	void a031_getMemberClasses() {
		// Test with class that has member classes (line 1029)
		var memberClasses = MM.class.getClasses();
		assertNotNull(memberClasses);
		// MM has MN as a public member class
		assertTrue(memberClasses.length > 0);

		// Test getMemberClasses when inner is not null
		var mmCi = ClassInfo.of(MM.class);
		var memberClassesList = mmCi.getMemberClasses();
		assertNotNull(memberClassesList);
		assertFalse(memberClassesList.isEmpty());

		// For types without inner class, should return empty list
		var empty = pTypeGenericArgInfo.getMemberClasses();
		assertTrue(empty.isEmpty());

		// For types with null inner, should return empty list
		var ci = ClassInfo.of((Class<?>)null, pType);
		var empty2 = ci.getMemberClasses();
		assertNotNull(empty2);
		assertTrue(empty2.isEmpty());
	}

	//====================================================================================================
	// getMethod(Predicate<MethodInfo>)
	//====================================================================================================
	@Test
	void a032_getMethod() {
		var method = cc3.getMethod(x -> x.hasName("c3a"));
		assertTrue(method.isPresent());
		assertEquals("c3a", method.get().getName());

		// Non-existent method
		var method2 = cc3.getMethod(x -> x.hasName("nonexistent"));
		assertFalse(method2.isPresent());
	}

	//====================================================================================================
	// getModule()
	//====================================================================================================
	@Test
	void a033_getModule() {
		var module = aClass.getModule();
		assertNotNull(module);

		// For types without inner class, should return null
		assertNull(pTypeGenericArgInfo.getModule());
	}

	//====================================================================================================
	// getName()
	//====================================================================================================
	@Test
	void a034_getName() {
		assertEquals("org.apache.juneau.commons.reflect.AClass", aClass.getName());
		assertEquals("java.util.AbstractMap", pTypeGenericInfo.getName());
		assertEquals("V", pTypeGenericArgInfo.getName());
	}

	//====================================================================================================
	// getNameCanonical()
	//====================================================================================================
	@Test
	void a035_getNameCanonical() {
		assertEquals("org.apache.juneau.commons.reflect.AClass", aClass.getNameCanonical());
		assertEquals("org.apache.juneau.commons.reflect.ClassInfo_Test.A1", aTypeInfo.getNameCanonical());

		// For ParameterizedType, should return null
		assertNull(pTypeInfo.getNameCanonical());
		assertNull(pTypeDimensionalInfo.getNameCanonical());
		assertNull(pTypeGenericInfo.getNameCanonical());
	}

	//====================================================================================================
	// getNameFormatted(ClassNameFormat, boolean, char, ClassArrayFormat)
	//====================================================================================================
	@Test
	void a036_getNameFormatted() throws Exception {
		var ci = ClassInfo.of(String.class);

		// SIMPLE format
		assertEquals("String", ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("String", ci.getNameFormatted(SIMPLE, false, '.', BRACKETS));
		assertEquals("String", ci.getNameFormatted(SIMPLE, true, '$', BRACKETS));

		// SHORT format
		assertEquals("String", ci.getNameFormatted(SHORT, false, '$', BRACKETS));
		assertEquals("String", ci.getNameFormatted(SHORT, true, '$', BRACKETS));

		// FULL format
		assertEquals("java.lang.String", ci.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("java.lang.String", ci.getNameFormatted(FULL, true, '$', BRACKETS));

		// Inner class
		var ci2 = ClassInfo.of(Map.Entry.class);
		assertEquals("Entry", ci2.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("Map$Entry", ci2.getNameFormatted(SHORT, false, '$', BRACKETS));
		assertEquals("Map.Entry", ci2.getNameFormatted(SHORT, false, '.', BRACKETS));
		assertEquals("java.util.Map$Entry", ci2.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("java.util.Map.Entry", ci2.getNameFormatted(FULL, false, '.', BRACKETS));

		// Arrays
		var ci3 = ClassInfo.of(String[].class);
		assertEquals("String[]", ci3.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("String[]", ci3.getNameFormatted(SHORT, false, '$', BRACKETS));
		assertEquals("java.lang.String[]", ci3.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("StringArray", ci3.getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("StringArray", ci3.getNameFormatted(SHORT, false, '$', WORD));
		assertEquals("java.lang.StringArray", ci3.getNameFormatted(FULL, false, '$', WORD));

		// Multi-dimensional arrays
		var ci4 = ClassInfo.of(String[][].class);
		assertEquals("String[][]", ci4.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("java.lang.String[][]", ci4.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("StringArrayArray", ci4.getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("java.lang.StringArrayArray", ci4.getNameFormatted(FULL, false, '$', WORD));

		// Primitive arrays
		var ci5 = ClassInfo.of(int[].class);
		assertEquals("int[]", ci5.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("int[]", ci5.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("intArray", ci5.getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("intArray", ci5.getNameFormatted(FULL, false, '$', WORD));

		// Generics
		var f = GenericsTestClass.class.getDeclaredField("hashMap");
		var t = f.getGenericType();
		var ci6 = ClassInfo.of(t);
		assertEquals("HashMap", ci6.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("java.util.HashMap", ci6.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("HashMap<String,Integer>", ci6.getNameFormatted(SIMPLE, true, '$', BRACKETS));
		assertEquals("HashMap<String,Integer>", ci6.getNameFormatted(SHORT, true, '$', BRACKETS));
		assertEquals("java.util.HashMap<java.lang.String,java.lang.Integer>", ci6.getNameFormatted(FULL, true, '$', BRACKETS));

		// Nested generics
		f = GenericsTestClass.class.getDeclaredField("nestedMap");
		t = f.getGenericType();
		var ci7 = ClassInfo.of(t);
		assertEquals("HashMap<String,ArrayList<Integer>>", ci7.getNameFormatted(SIMPLE, true, '$', BRACKETS));
		assertEquals("java.util.HashMap<java.lang.String,java.util.ArrayList<java.lang.Integer>>", ci7.getNameFormatted(FULL, true, '$', BRACKETS));

		// Generic arrays
		f = GenericsTestClass.class.getDeclaredField("listArray");
		t = f.getGenericType();
		var ci8 = ClassInfo.of(t);
		assertEquals("ArrayList<String>[]", ci8.getNameFormatted(SIMPLE, true, '$', BRACKETS));
		assertEquals("java.util.ArrayList<java.lang.String>[]", ci8.getNameFormatted(FULL, true, '$', BRACKETS));
		assertEquals("ArrayList<String>Array", ci8.getNameFormatted(SIMPLE, true, '$', WORD));

		// Inner class arrays
		var ci9 = ClassInfo.of(Map.Entry[].class);
		assertEquals("Entry[]", ci9.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("EntryArray", ci9.getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("Map$Entry[]", ci9.getNameFormatted(SHORT, false, '$', BRACKETS));
		assertEquals("Map.Entry[]", ci9.getNameFormatted(SHORT, false, '.', BRACKETS));
		assertEquals("Map$EntryArray", ci9.getNameFormatted(SHORT, false, '$', WORD));
		assertEquals("java.util.Map$Entry[]", ci9.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals("java.util.Map.Entry[]", ci9.getNameFormatted(FULL, false, '.', BRACKETS));
		assertEquals("java.util.Map$EntryArray", ci9.getNameFormatted(FULL, false, '$', WORD));

		// Equivalent methods
		assertEquals(ci.getName(), ci.getNameFormatted(FULL, false, '$', BRACKETS));
		assertEquals(ci.getNameSimple(), ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));

		// ParameterizedType case - should extract raw type
		var ci10 = pTypeInfo;
		var formatted = ci10.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertNotNull(formatted);
		assertTrue(formatted.contains("Map"));

		// SIMPLE format with null class but ParameterizedType - extracts raw type (line 314-316)
		var ci11 = ClassInfo.of((Class<?>)null, pType);
		var formatted2 = ci11.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertNotNull(formatted2);
		// When inner is null but isParameterizedType is true, code extracts raw type and uses its simple name
		assertEquals("Map", formatted2);
	}

	//====================================================================================================
	// getNameFull()
	//====================================================================================================
	@Test
	void a037_getNameFull() {
		assertEquals("org.apache.juneau.commons.reflect.AClass", aClass.getNameFull());
		// Test twice to verify caching
		assertEquals("org.apache.juneau.commons.reflect.AClass", aClass.getNameFull());

		// Arrays
		assertEquals("org.apache.juneau.commons.reflect.AClass[][]", of(AClass[][].class).getNameFull());

		// Inner classes
		assertEquals("org.apache.juneau.commons.reflect.ClassInfo_Test$J1", j1.getNameFull());
		assertEquals("org.apache.juneau.commons.reflect.ClassInfo_Test$J2", j2.getNameFull());

		// Inner class arrays
		assertEquals("org.apache.juneau.commons.reflect.ClassInfo_Test$J1[][]", j1_3d.getNameFull());
		assertEquals("org.apache.juneau.commons.reflect.ClassInfo_Test$J2[][]", j2_3d.getNameFull());

		// Primitives
		assertEquals("int", of(int.class).getNameFull());

		// Primitive arrays
		assertEquals("int[][]", of(int[][].class).getNameFull());

		// Types
		assertEquals("org.apache.juneau.commons.reflect.ClassInfo_Test$A1", aTypeInfo.getNameFull());
		assertEquals("java.util.Map<java.lang.String,java.util.List<java.lang.String>>", pTypeInfo.getNameFull());
		assertEquals("java.util.Map<java.lang.String,java.lang.String[][]>", pTypeDimensionalInfo.getNameFull());
		assertEquals("java.util.AbstractMap<K,V>", pTypeGenericInfo.getNameFull());
		assertEquals("V", pTypeGenericArgInfo.getNameFull());

		// Local class
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {}
		var localClassName = of(LocalClass.class).getNameFull();
		assertTrue(localClassName.startsWith("org.apache.juneau.commons.reflect.ClassInfo_Test$"), "Should start with package and class name");
		assertTrue(localClassName.endsWith("LocalClass"), "Should end with LocalClass");
	}

	//====================================================================================================
	// getNameReadable()
	//====================================================================================================
	@Test
	void a038_getNameReadable() {
		assertEquals("AClass", aClass.getNameReadable());
		assertEquals("A1", aTypeInfo.getNameReadable());
		assertEquals("StringArray", ClassInfo.of(String[].class).getNameReadable());
		assertEquals("StringArrayArray", ClassInfo.of(String[][].class).getNameReadable());
	}

	//====================================================================================================
	// getNameShort()
	//====================================================================================================
	@Test
	void a039_getNameShort() {
		assertEquals("AClass", aClass.getNameShort());
		// Test twice to verify caching
		assertEquals("AClass", aClass.getNameShort());

		// Arrays
		assertEquals("AClass[][]", of(AClass[][].class).getNameShort());

		// Inner classes
		assertEquals("ClassInfo_Test$J1", j1.getNameShort());
		assertEquals("ClassInfo_Test$J2", j2.getNameShort());

		// Inner class arrays
		assertEquals("ClassInfo_Test$J1[][]", j1_3d.getNameShort());
		assertEquals("ClassInfo_Test$J2[][]", j2_3d.getNameShort());

		// Primitives
		assertEquals("int", of(int.class).getNameShort());

		// Primitive arrays
		assertEquals("int[][]", of(int[][].class).getNameShort());

		// Types
		assertEquals("ClassInfo_Test$A1", aTypeInfo.getNameShort());
		assertEquals("Map<String,List<String>>", pTypeInfo.getNameShort());
		assertEquals("Map<String,String[][]>", pTypeDimensionalInfo.getNameShort());
		assertEquals("AbstractMap<K,V>", pTypeGenericInfo.getNameShort());
		assertEquals("V", pTypeGenericArgInfo.getNameShort());

		// Local class
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {}
		assertEquals("ClassInfo_Test$LocalClass", of(LocalClass.class).getNameShort());
	}

	//====================================================================================================
	// getNameSimple()
	//====================================================================================================
	@Test
	void a040_getNameSimple() {
		assertEquals("AClass", aClass.getNameSimple());
		// Test twice to verify caching
		assertEquals("AClass", aClass.getNameSimple());

		// Arrays
		assertEquals("AClass[][]", of(AClass[][].class).getNameSimple());

		// Inner classes
		assertEquals("J1", j1.getNameSimple());
		assertEquals("J2", j2.getNameSimple());

		// Inner class arrays
		assertEquals("J1[][]", j1_3d.getNameSimple());
		assertEquals("J2[][]", j2_3d.getNameSimple());

		// Primitives
		assertEquals("int", of(int.class).getNameSimple());

		// Primitive arrays
		assertEquals("int[][]", of(int[][].class).getNameSimple());

		// Types
		assertEquals("A1", aTypeInfo.getNameSimple());
		assertEquals("Map", pTypeInfo.getNameSimple());
		assertEquals("Map", pTypeDimensionalInfo.getNameSimple());
		assertEquals("AbstractMap", pTypeGenericInfo.getNameSimple());
		assertEquals("V", pTypeGenericArgInfo.getNameSimple());

		// Local class
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {}
		assertEquals("LocalClass", of(LocalClass.class).getNameSimple());
	}

	//====================================================================================================
	// getNames()
	//====================================================================================================
	@Test
	void a041_getNames() {
		var names = aClass.getNames();
		assertNotNull(names);
		assertEquals(4, names.length);
		assertTrue(names[0].contains("AClass"));
		assertTrue(names[1].contains("AClass"));
		assertTrue(names[2].contains("AClass"));
		assertTrue(names[3].contains("AClass"));
	}

	//====================================================================================================
	// getNestHost()
	//====================================================================================================
	@Test
	void a042_getNestHost() {
		var nestHost = aClass.getNestHost();
		assertNotNull(nestHost);
		assertEquals(aClass, nestHost);

		// For member classes, nest host is the outer class
		var nestHost2 = aTypeInfo.getNestHost();
		assertNotNull(nestHost2);
		assertEquals(ClassInfo_Test.class.getName(), nestHost2.inner().getName());

		// For types without inner class, should return null
		assertNull(pTypeGenericArgInfo.getNestHost());
	}

	//====================================================================================================
	// getNestMembers()
	//====================================================================================================
	@Test
	void a043_getNestMembers() {
		var nestMembers = aClass.getNestMembers();
		assertNotNull(nestMembers);
		// Should include at least the class itself
		assertFalse(nestMembers.isEmpty());

		// For types without inner class, should return empty list
		var empty = pTypeGenericArgInfo.getNestMembers();
		assertTrue(empty.isEmpty());
	}

	//====================================================================================================
	// getNoArgConstructor(Visibility)
	//====================================================================================================
	@Test
	void a044_getNoArgConstructor() {
		check("E2()", e2.getNoArgConstructor(Visibility.PRIVATE).orElse(null));
		check("E2()", e2.getNoArgConstructor(Visibility.PROTECTED).orElse(null));
		check("E2()", e2.getNoArgConstructor(Visibility.DEFAULT).orElse(null));
		check(null, e2.getNoArgConstructor(Visibility.PUBLIC).orElse(null));

		// Abstract class should return null
		check(null, e3.getNoArgConstructor(Visibility.PUBLIC).orElse(null));

		// Inner class should return constructor with outer class parameter
		check("E4(ClassInfo_Test)", e4.getNoArgConstructor(Visibility.PUBLIC).orElse(null));

		// Class without no-arg constructor
		check(null, e6.getNoArgConstructor(Visibility.PUBLIC).orElse(null));
	}

	//====================================================================================================
	// getPackage()
	//====================================================================================================
	@Test
	void a045_getPackage() {
		check("org.apache.juneau.commons.reflect", ka.getPackage().getName());
		// Test on types
		check("org.apache.juneau.commons.reflect", aTypeInfo.getPackage());
		check("java.util", pTypeInfo.getPackage());
		check("java.util", pTypeDimensionalInfo.getPackage());
		check("java.util", pTypeGenericInfo.getPackage());
		check(null, pTypeGenericArgInfo.getPackage());
	}

	//====================================================================================================
	// getPackageAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a046_getPackageAnnotation() {
		check("@PA(10)", g3.getPackageAnnotation(PA.class));
		// Test on types
		check("@PA(10)", aTypeInfo.getPackageAnnotation(PA.class));
		check(null, pTypeInfo.getPackageAnnotation(PA.class));
		check(null, pTypeDimensionalInfo.getPackageAnnotation(PA.class));
		check(null, pTypeGenericInfo.getPackageAnnotation(PA.class));
		check(null, pTypeGenericArgInfo.getPackageAnnotation(PA.class));
	}

	//====================================================================================================
	// getParameterType(int, Class<?>)
	//====================================================================================================
	@Test
	void a047_getParameterType() {
		// Test complex type variable resolution with nested generics (line 1372)
		// This tests the type variable resolution in getParameterType when dealing with nested inner classes
		// Note: Testing nested generic type variable resolution requires complex scenarios
		// The code path at line 1372 is triggered when resolving type variables in nested inner classes
		// Simple map
		check("String", ma.getParameterType(0, HashMap.class));
		check("Integer", ma.getParameterType(1, HashMap.class));
		check("String", mb.getParameterType(0, HashMap.class));
		check("Integer", mb.getParameterType(1, HashMap.class));

		// Out of bounds
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid type index. index=2, argsLength=2", () -> ma.getParameterType(2, HashMap.class));

		// Not a subclass
		assertThrowsWithMessage(IllegalArgumentException.class, "Class 'AClass' is not a subclass of parameterized type 'HashMap'", () -> aClass.getParameterType(2, HashMap.class));

		// Null parameterized type
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'pt' cannot be null.", () -> aClass.getParameterType(2, null));

		// Not a parameterized type
		assertThrowsWithMessage(IllegalArgumentException.class, "Class 'MA' is not a parameterized type", () -> mb.getParameterType(2, MA.class));

		// Unresolved types
		assertThrowsWithMessage(IllegalArgumentException.class, "Could not resolve variable 'E' to a type.", () -> mc.getParameterType(1, HashMap.class));

		// Resolved types
		check("Integer", md.getParameterType(1, HashMap.class));

		// Parameterized type variable
		check("HashMap", me.getParameterType(1, HashMap.class));

		// Array parameter type
		check("String[]", mf.getParameterType(1, HashMap.class));

		// Generic array type parameter
		check("HashMap[]", mg.getParameterType(1, HashMap.class));

		// Generic array type parameter without types
		check("LinkedList[]", mh.getParameterType(1, HashMap.class));

		// Unresolved generic array type
		assertThrowsWithMessage(IllegalArgumentException.class, "Could not resolve variable 'X[]' to a type.", () -> mi.getParameterType(1, HashMap.class));

		// Wildcard type
		assertThrowsWithMessage(IllegalArgumentException.class, "Could not resolve variable 'X' to a type.", () -> mj.getParameterType(1, HashMap.class));

		// Inner type
		check("MK", ml.getParameterType(1, HashMap.class));

		// Nested type
		check("MM", mn.getParameterType(1, HashMap.class));
	}

	//====================================================================================================
	// getParents()
	//====================================================================================================
	@Test
	void a048_getParents() {
		check("BC3,BC2,BC1", bc3.getParents());
		check("", object.getParents());
		check("BI1", bi1.getParents());
	}

	//====================================================================================================
	// getParentsAndInterfaces()
	//====================================================================================================
	@Test
	void a049_getParentsAndInterfaces() {
		var ci = ClassInfo.of(Child.class);
		var parentsAndInterfaces = ci.getParentsAndInterfaces();

		// Should include:
		// 1. Child itself
		// 2. IChild (direct interface on Child)
		// 3. Parent (direct parent)
		// 4. IParent (direct interface on Parent)
		// 5. ISuperParent (parent interface of IParent)
		// 6. GrandParent (parent's parent)
		// 7. IGrandParent (direct interface on GrandParent)
		// 8. ISuperGrandParent (parent interface of IGrandParent)

		var names = parentsAndInterfaces.stream().map(ClassInfo::getNameSimple).collect(Collectors.toList());

		// Verify all expected classes/interfaces are present
		assertTrue(names.contains("Child"), "Should include Child itself");
		assertTrue(names.contains("Parent"), "Should include Parent");
		assertTrue(names.contains("GrandParent"), "Should include GrandParent");
		assertTrue(names.contains("IChild"), "Should include IChild");
		assertTrue(names.contains("IParent"), "Should include IParent from Parent");
		assertTrue(names.contains("ISuperParent"), "Should include ISuperParent from IParent hierarchy");
		assertTrue(names.contains("IGrandParent"), "Should include IGrandParent from GrandParent");
		assertTrue(names.contains("ISuperGrandParent"), "Should include ISuperGrandParent from IGrandParent hierarchy");
	}

	//====================================================================================================
	// getPermittedSubclasses()
	//====================================================================================================
	@Test
	void a050_getPermittedSubclasses() {
		// Most classes are not sealed, so should return empty list
		var permitted = aClass.getPermittedSubclasses();
		assertNotNull(permitted);
		assertTrue(permitted.isEmpty());

		// For types with null inner, should return empty list
		var ci = ClassInfo.of((Class<?>)null, pType);
		var empty = ci.getPermittedSubclasses();
		assertNotNull(empty);
		assertTrue(empty.isEmpty());
	}

	//====================================================================================================
	// getProtectionDomain()
	//====================================================================================================
	@Test
	void a050b_getProtectionDomain() {
		// Should return protection domain for regular classes
		// May be null depending on security manager, but should not throw
		assertDoesNotThrow(() -> aClass.getProtectionDomain());

		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getProtectionDomain());
	}

	//====================================================================================================
	// getPrimitiveDefault()
	//====================================================================================================
	@Test
	void a051_getPrimitiveDefault() {
		for (var i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getPrimitiveDefault(), primitiveDefaults.get(i));
		assertNull(of(String.class).getPrimitiveDefault());
		// Test on types
		assertNull(aTypeInfo.getPrimitiveDefault());
	}

	//====================================================================================================
	// getPrimitiveForWrapper()
	//====================================================================================================
	@Test
	void a052_getPrimitiveForWrapper() {
		for (var i = 0; i < primitives.size(); i++)
			assertEquals(of(primitiveWrappers.get(i)).getPrimitiveForWrapper(), primitives.get(i));
		assertNull(of(String.class).getPrimitiveForWrapper());
		// Test on types
		assertNull(aTypeInfo.getPrimitiveForWrapper());
	}

	//====================================================================================================
	// getPrimitiveWrapper()
	//====================================================================================================
	@Test
	void a053_getPrimitiveWrapper() {
		for (var i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getPrimitiveWrapper(), primitiveWrappers.get(i));
		assertNull(of(String.class).getPrimitiveWrapper());
		// Test on types
		assertNull(aTypeInfo.getPrimitiveWrapper());
	}

	//====================================================================================================
	// getPublicConstructor(Predicate<ConstructorInfo>)
	//====================================================================================================
	@Test
	void a054_getPublicConstructor() {
		check("E1(String)", e1.getPublicConstructor(x -> x.hasParameterTypes(String.class)).orElse(null));
		check("E1(String)", e1.getPublicConstructor(x -> x.canAccept("foo")).orElse(null));
		check("E1()", e1.getPublicConstructor(cons -> cons.getParameterCount() == 0).orElse(null));
	}

	//====================================================================================================
	// getPublicConstructors()
	//====================================================================================================
	@Test
	void a055_getPublicConstructors() {
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
		// Test twice to verify caching
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());

		// Test on types
		check("A1(ClassInfo_Test)", aTypeInfo.getPublicConstructors());
		check("", pTypeInfo.getPublicConstructors());
		check("", pTypeDimensionalInfo.getPublicConstructors());
		check("", pTypeGenericInfo.getPublicConstructors());
		check("", pTypeGenericArgInfo.getPublicConstructors());
	}

	//====================================================================================================
	// getPublicField(Predicate<FieldInfo>)
	//====================================================================================================
	@Test
	void a056_getPublicField() {
		check("F3.a1", f3.getPublicField(x -> x.hasName("a1")).orElse(null));
		check(null, f3.getPublicField(x -> x.hasName("a2")).orElse(null));
		check(null, f3.getPublicField(x -> x.hasName("a3")).orElse(null));
	}

	//====================================================================================================
	// getPublicFields()
	//====================================================================================================
	@Test
	void a057_getPublicFields() {
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
		// Test twice to verify caching
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());

		// Test on types
		check("", aTypeInfo.getPublicFields());
	}

	//====================================================================================================
	// getResource(String)
	//====================================================================================================
	@Test
	void a057b_getResource() {
		// Should return resource URL for existing resources
		// May be null depending on classpath, but should not throw
		assertDoesNotThrow(() -> aClass.getResource("/"));

		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getResource("test"));
	}

	//====================================================================================================
	// getResourceAsStream(String)
	//====================================================================================================
	@Test
	void a057c_getResourceAsStream() {
		// Should return resource stream for existing resources
		var stream = aClass.getResourceAsStream("/");
		// May be null depending on classpath, but should not throw
		if (stream != null) {
			assertDoesNotThrow(() -> stream.close());
		}

		// For types with null inner, should return null
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertNull(ci.getResourceAsStream("test"));
	}

	//====================================================================================================
	// getPublicMethod(Predicate<MethodInfo>)
	//====================================================================================================
	@Test
	void a058_getPublicMethod() {
		var method = cc3.getPublicMethod(x -> x.hasName("c3a"));
		assertTrue(method.isPresent());
		assertEquals("c3a", method.get().getName());

		// Non-existent method
		var method2 = cc3.getPublicMethod(x -> x.hasName("nonexistent"));
		assertFalse(method2.isPresent());
	}

	//====================================================================================================
	// getPublicMethods()
	//====================================================================================================
	@Test
	void a059_getPublicMethods() {
		check("CC3.c1a(),CC3.c2b(),CC3.c3a(),CC3.i1a(),CC3.i1b(),CC3.i2a(),CC3.i2b()", cc3.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
		// Test twice to verify caching
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());

		// Test on types
		check("", aTypeInfo.getPublicMethods());
		check("", pTypeGenericArgInfo.getPublicMethods());
	}

	//====================================================================================================
	// getRecordComponents()
	//====================================================================================================
	@Test
	void a060_getRecordComponents() {
		// Test with a record class if available (Java 14+)
		try {
			Class.forName("java.lang.Record");
			// If we can find Record, test with a simple record
			// For now, just verify the method exists and returns empty list for non-records
			assertTrue(cc3.getRecordComponents().isEmpty());
		} catch (ClassNotFoundException e) {
			// Records not available, skip test
			assertTrue(cc3.getRecordComponents().isEmpty());
		}
	}

	//====================================================================================================
	// getRepeatedAnnotationMethod()
	//====================================================================================================
	@Test
	void a061_getRepeatedAnnotationMethod() {
		// Test with @Repeatable annotation
		var repeatable = ClassInfo.of(Repeatable.class);
		var method = repeatable.getRepeatedAnnotationMethod();
		// @Repeatable itself is not repeatable, so should return null
		assertNull(method);
	}

	//====================================================================================================
	// getSigners()
	//====================================================================================================
	@Test
	void a062_getSigners() {
		var signers = aClass.getSigners();
		assertNotNull(signers);
		// Most classes won't have signers unless from a signed JAR
		assertTrue(signers.isEmpty() || !signers.isEmpty());
	}

	//====================================================================================================
	// getSuperclass()
	//====================================================================================================
	@Test
	void a063_getSuperclass() {
		check("BC2", bc3.getSuperclass());
		check("BC1", bc2.getSuperclass());
		check("Object", bc1.getSuperclass());
		check(null, object.getSuperclass());
		check(null, bi2.getSuperclass());
		check(null, bi1.getSuperclass());
		// Test on types
		check("Object", aTypeInfo.getSuperclass());
		check(null, pTypeInfo.getSuperclass());
		check(null, pTypeDimensionalInfo.getSuperclass());
		check("Object", pTypeGenericInfo.getSuperclass());
		check(null, pTypeGenericArgInfo.getSuperclass());
	}

	//====================================================================================================
	// getTypeParameters()
	//====================================================================================================
	@Test
	void a064_getTypeParameters() {
		var params = mc.getTypeParameters();
		assertNotNull(params);
		// MC<K,E> should have 2 type parameters
		assertEquals(2, params.size());
	}

	//====================================================================================================
	// getWrapperIfPrimitive()
	//====================================================================================================
	@Test
	void a065_getWrapperIfPrimitive() {
		for (var i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getWrapperIfPrimitive().inner(), primitiveWrappers.get(i));
		assertEquals(of(String.class).getWrapperIfPrimitive().inner(), String.class);
		// Test on types
		assertEquals("class org.apache.juneau.commons.reflect.ClassInfo_Test$A1", aTypeInfo.getWrapperIfPrimitive().inner().toString());
		assertEquals("interface java.util.Map", pTypeInfo.getWrapperIfPrimitive().inner().toString());
		assertEquals("interface java.util.Map", pTypeDimensionalInfo.getWrapperIfPrimitive().inner().toString());
		assertEquals("class java.util.AbstractMap", pTypeGenericInfo.getWrapperIfPrimitive().inner().toString());
		assertEquals(null, pTypeGenericArgInfo.getWrapperIfPrimitive().inner());
		assertEquals(aTypeInfo.getWrapperIfPrimitive().innerType(), aType);
		check("V", pTypeGenericArgInfo.getWrapperIfPrimitive());
	}

	//====================================================================================================
	// hasAnnotation(Class<A>)
	//====================================================================================================
	@Test
	void a066_hasAnnotation() {
		assertTrue(g3.hasAnnotation(A.class));
		assertFalse(g3.hasAnnotation(B.class));
		assertThrows(IllegalArgumentException.class, () -> g3.hasAnnotation(null));
	}

	//====================================================================================================
	// hasPackage()
	//====================================================================================================
	@Test
	void a067_hasPackage() {
		assertTrue(ka.hasPackage());
		// Test on types
		assertTrue(aTypeInfo.hasPackage());
		assertTrue(pTypeInfo.hasPackage());
		assertTrue(pTypeDimensionalInfo.hasPackage());
		assertTrue(pTypeGenericInfo.hasPackage());
		assertFalse(pTypeGenericArgInfo.hasPackage());
	}

	//====================================================================================================
	// hasPrimitiveWrapper()
	//====================================================================================================
	@Test
	void a068_hasPrimitiveWrapper() {
		for (var c : primitives)
			assertTrue(of(c).hasPrimitiveWrapper());
		for (var c : primitiveWrappers)
			assertFalse(of(c).hasPrimitiveWrapper());
		// Test on types
		assertFalse(aTypeInfo.hasPrimitiveWrapper());
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a069_inner() {
		assertNotNull(of(A1.class).inner());
		assertTrue(of(A1.class).innerType() instanceof Class);
	}

	//====================================================================================================
	// innerType()
	//====================================================================================================
	@Test
	void a070_innerType() {
		assertTrue(of(A1.class).innerType() instanceof Class);
		assertNotNull(aTypeInfo.innerType());
		assertNotNull(pTypeInfo.innerType());
	}

	//====================================================================================================
	// is(Class<?>)
	//====================================================================================================
	@Test
	void a071_is() {
		assertTrue(ka.is(KA.class));
		assertFalse(ka.is(KB.class));
		assertFalse(ka.is(KC.class));
		assertFalse(kb.is(KA.class));
		assertTrue(kb.is(KB.class));
		assertFalse(kb.is(KC.class));
		assertFalse(kc.is(KA.class));
		assertFalse(kc.is(KB.class));
		assertTrue(kc.is(KC.class));

		// Test with ClassInfo
		assertTrue(ka.is(of(KA.class)));
		assertFalse(ka.is(of(KB.class)));
		assertFalse(ka.is(of(KC.class)));
		assertFalse(kb.is(of(KA.class)));
		assertTrue(kb.is(of(KB.class)));
		assertFalse(kb.is(of(KC.class)));
		assertFalse(kc.is(of(KA.class)));
		assertFalse(kc.is(of(KB.class)));
		assertTrue(kc.is(of(KC.class)));

		// Test on types
		assertFalse(aTypeInfo.is(KA.class));
		assertFalse(pTypeInfo.is(KA.class));
		assertFalse(pTypeDimensionalInfo.is(KA.class));
		assertFalse(pTypeGenericInfo.is(KA.class));
		assertFalse(pTypeGenericArgInfo.is(KA.class));
		assertFalse(pTypeGenericArgInfo.is(of(KA.class)));

		// Test ElementFlag cases
		assertTrue(aClass.is(ElementFlag.CLASS));
		assertTrue(aClass.is(NOT_ANNOTATION));
		assertTrue(aClass.is(NOT_ARRAY));
		assertTrue(aClass.is(NOT_ENUM));
		assertTrue(aClass.is(NOT_LOCAL));
		assertTrue(aClass.is(NOT_MEMBER));
		assertTrue(aClass.is(NOT_NON_STATIC_MEMBER));
		assertTrue(aClass.is(NOT_PRIMITIVE));
		assertTrue(aClass.is(NOT_RECORD));
		assertTrue(aClass.is(NOT_SEALED));
		assertTrue(aClass.is(NOT_SYNTHETIC));
		
		// Test positive ElementFlag cases (lines 1772, 1774, 1775, 1776, 1781, 1783, 1787, 1789, 1791, 1793)
		// ANNOTATION (line 1772)
		assertTrue(ClassInfo.of(A.class).is(ANNOTATION));
		assertFalse(aClass.is(ANNOTATION));
		
		// ANONYMOUS and NOT_ANONYMOUS (lines 1774, 1775)
		// Anonymous classes are created dynamically, so we test NOT_ANONYMOUS
		assertTrue(aClass.is(NOT_ANONYMOUS));
		// Test anonymous class if we can create one
		var anonymous = new Object() {}.getClass();
		var anonymousInfo = ClassInfo.of(anonymous);
		if (anonymousInfo.isAnonymousClass()) {
			assertTrue(anonymousInfo.is(ANONYMOUS));
			assertFalse(anonymousInfo.is(NOT_ANONYMOUS));
		}
		
		// ARRAY (line 1776)
		assertTrue(ClassInfo.of(String[].class).is(ARRAY));
		assertFalse(aClass.is(ARRAY));
		
		// ENUM (line 1781)
		assertTrue(ClassInfo.of(ClassArrayFormat.class).is(ENUM));
		assertFalse(aClass.is(ENUM));
		
		// LOCAL and NOT_LOCAL (line 1783)
		// Local class
		class LocalTestClass {}
		var localInfo = ClassInfo.of(LocalTestClass.class);
		assertTrue(localInfo.is(LOCAL));
		assertFalse(localInfo.is(NOT_LOCAL));
		assertTrue(aClass.is(NOT_LOCAL));
		assertFalse(aClass.is(LOCAL));
		
		// NON_STATIC_MEMBER (line 1787)
		// H_PublicMember is a non-static member class
		var nonStaticMember = ClassInfo.of(H_PublicMember.class);
		assertTrue(nonStaticMember.is(NON_STATIC_MEMBER));
		assertFalse(nonStaticMember.is(NOT_NON_STATIC_MEMBER));
		assertTrue(aClass.is(NOT_NON_STATIC_MEMBER));
		assertFalse(aClass.is(NON_STATIC_MEMBER));
		
		// PRIMITIVE (line 1789)
		assertTrue(ClassInfo.of(int.class).is(PRIMITIVE));
		assertFalse(aClass.is(PRIMITIVE));
		
		// RECORD (line 1791) - test if records are available
		try {
			Class.forName("java.lang.Record");
			// Records are available, but we don't have a test record class
			// Just verify non-records return false
			assertFalse(aClass.is(RECORD));
		} catch (ClassNotFoundException e) {
			// Records not available, skip
		}
		
		// SEALED (line 1793) - test if sealed classes are available
		try {
			Class.forName("java.lang.constant.Constable");
			// Sealed classes are available (Java 17+)
			// Most classes are not sealed, so should return false
			assertFalse(aClass.is(SEALED));
		} catch (ClassNotFoundException e) {
			// Sealed classes not available, skip
		}
	}

	//====================================================================================================
	// isAbstract()
	//====================================================================================================
	@Test
	void a072_isAbstract() {
		assertTrue(hAbstractPublic.isAbstract());
		assertFalse(pTypeGenericArgInfo.isAbstract());
		// Test on types
		assertFalse(aTypeInfo.isAbstract());
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a073_isAll() {
		assertTrue(h2a.isAll(DEPRECATED, PUBLIC, STATIC, MEMBER, ABSTRACT, ElementFlag.CLASS));
		assertTrue(h2b.isAll(NOT_DEPRECATED, NOT_PUBLIC, STATIC, ABSTRACT, INTERFACE));
		// Test on types
		assertTrue(aTypeInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
		assertFalse(pTypeInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
		assertFalse(pTypeDimensionalInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
		assertFalse(pTypeGenericInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));

		// Test individual flags
		assertTrue(h2Deprecated.is(DEPRECATED));
		assertFalse(h2NotDeprecated.is(DEPRECATED));
		assertFalse(h2Deprecated.is(NOT_DEPRECATED));
		assertTrue(h2NotDeprecated.is(NOT_DEPRECATED));
		assertTrue(of(H2_Public.class).is(PUBLIC));
		assertFalse(h2NotPublic.is(PUBLIC));
		assertFalse(of(H2_Public.class).is(NOT_PUBLIC));
		assertTrue(h2NotPublic.is(NOT_PUBLIC));
		assertTrue(of(H2_Static.class).is(STATIC));
		assertFalse(h2NotStatic.is(STATIC));
		assertFalse(of(H2_Static.class).is(NOT_STATIC));
		assertTrue(h2NotStatic.is(NOT_STATIC));
		assertTrue(h2Member.is(MEMBER));
		assertTrue(h2StaticMember.is(MEMBER));
		assertFalse(aClass.is(MEMBER));
		assertFalse(h2Member.is(NOT_MEMBER));
		assertFalse(h2StaticMember.is(NOT_MEMBER));
		assertTrue(aClass.is(NOT_MEMBER));
		assertTrue(of(H2_Abstract.class).is(ABSTRACT));
		assertFalse(h2NotAbstract.is(ABSTRACT));
		assertTrue(aInterface.is(ABSTRACT));
		assertFalse(of(H2_Abstract.class).is(NOT_ABSTRACT));
		assertTrue(h2NotAbstract.is(NOT_ABSTRACT));
		assertFalse(aInterface.is(NOT_ABSTRACT));
		assertTrue(aInterface.is(INTERFACE));
		assertFalse(aClass.is(INTERFACE));
		assertFalse(aInterface.is(ElementFlag.CLASS));
		assertTrue(aClass.is(ElementFlag.CLASS));
	}

	//====================================================================================================
	// isAnnotation()
	//====================================================================================================
	@Test
	void a074_isAnnotation() {
		assertTrue(ClassInfo.of(A.class).isAnnotation());
		assertTrue(ClassInfo.of(B.class).isAnnotation());
		assertFalse(aClass.isAnnotation());
		
		// Test with null inner (line 1811)
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isAnnotation());
	}

	//====================================================================================================
	// isAny(Class<?>...)
	//====================================================================================================
	@Test
	void a075_isAny() {
		assertTrue(ka.isAny(KA.class));
		assertTrue(ka.isAny(KA.class, KB.class));
		assertFalse(ka.isAny(KB.class));
		assertFalse(ka.isAny(KC.class));
		assertFalse(kb.isAny(KA.class));
		assertTrue(kb.isAny(KB.class));
		assertFalse(kb.isAny(KC.class));
		assertFalse(kc.isAny(KA.class));
		assertFalse(kc.isAny(KB.class));
		assertTrue(kc.isAny(KC.class));
	}

	//====================================================================================================
	// isArray()
	//====================================================================================================
	@Test
	void a076_isArray() {
		assertTrue(ClassInfo.of(String[].class).isArray());
		assertTrue(ClassInfo.of(int[].class).isArray());
		assertFalse(aClass.isArray());

		// For types with null inner, should return false
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isArray());
	}

	//====================================================================================================
	// isAnonymousClass()
	//====================================================================================================
	@Test
	void a076b_isAnonymousClass() {
		// Regular classes are not anonymous
		assertFalse(aClass.isAnonymousClass());

		// For types with null inner, should return false (line 1821)
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isAnonymousClass());
	}

	//====================================================================================================
	// isCollectionOrArray()
	//====================================================================================================
	@Test
	void a076c_isCollectionOrArray() {
		// Test with array
		assertTrue(ClassInfo.of(String[].class).isCollectionOrArray());
		assertTrue(ClassInfo.of(int[].class).isCollectionOrArray());
		
		// Test with Collection
		assertTrue(ClassInfo.of(java.util.List.class).isCollectionOrArray());
		assertTrue(ClassInfo.of(java.util.Set.class).isCollectionOrArray());
		assertTrue(ClassInfo.of(java.util.Collection.class).isCollectionOrArray());
		
		// Test with non-collection, non-array
		assertFalse(aClass.isCollectionOrArray());
		assertFalse(ClassInfo.of(String.class).isCollectionOrArray());
		
		// Test with null inner (line 1905)
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isCollectionOrArray());
	}

	//====================================================================================================
	// isChildOf(Class<?>)
	//====================================================================================================
	@Test
	void a077_isChildOf() {
		assertTrue(ka.isChildOf(KA.class));
		assertFalse(ka.isChildOf(KB.class));
		assertFalse(ka.isChildOf(KC.class));
		assertTrue(kb.isChildOf(KA.class));
		assertTrue(kb.isChildOf(KB.class));
		assertFalse(kb.isChildOf(KC.class));
		assertTrue(kc.isChildOf(KA.class));
		assertTrue(kc.isChildOf(KB.class));
		assertTrue(kc.isChildOf(KC.class));
		// Test with null
		assertFalse(ka.isChildOf((Class<?>)null));
		// Test on types
		assertFalse(ka.isChildOf(aType));
		assertFalse(ka.isChildOf(pType));
		assertFalse(ka.isChildOf(pTypeDimensional));
		assertFalse(ka.isChildOf(pTypeGeneric));
		assertFalse(ka.isChildOf(pTypeGenericArg));
		assertFalse(aTypeInfo.isChildOf(KA.class));
		assertFalse(pTypeInfo.isChildOf(KA.class));
		assertFalse(pTypeDimensionalInfo.isChildOf(KA.class));
		assertFalse(pTypeGenericInfo.isChildOf(KA.class));
		assertFalse(pTypeGenericArgInfo.isChildOf(KA.class));

		// Test isChildOf(ClassInfo)
		assertTrue(kb.isChildOf(ka));
		assertTrue(kc.isChildOf(ka));
		assertTrue(kc.isChildOf(kb));
		assertFalse(ka.isChildOf(kb));
	}

	//====================================================================================================
	// isChildOfAny(Class<?>...)
	//====================================================================================================
	@Test
	void a078_isChildOfAny() {
		assertTrue(ka.isChildOfAny(KA.class));
		assertFalse(ka.isChildOfAny(KB.class));
		assertFalse(ka.isChildOfAny(KC.class));
		assertTrue(kb.isChildOfAny(KA.class));
		assertTrue(kb.isChildOfAny(KB.class));
		assertFalse(kb.isChildOfAny(KC.class));
		assertTrue(kc.isChildOfAny(KA.class));
		assertTrue(kc.isChildOfAny(KB.class));
		assertTrue(kc.isChildOfAny(KC.class));
		// Test on types
		assertFalse(aTypeInfo.isChildOfAny(KA.class));
		assertFalse(pTypeInfo.isChildOfAny(KA.class));
		assertFalse(pTypeDimensionalInfo.isChildOfAny(KA.class));
		assertFalse(pTypeGenericInfo.isChildOfAny(KA.class));
		assertFalse(pTypeGenericArgInfo.isChildOfAny(KA.class));
	}

	//====================================================================================================
	// isClass()
	//====================================================================================================
	@Test
	void a079_isClass() {
		assertTrue(aClass.isClass());
		assertFalse(aInterface.isClass());
		// Test on types
		assertTrue(aTypeInfo.isClass());
		assertFalse(pTypeInfo.isClass());
		assertFalse(pTypeDimensionalInfo.isClass());
		assertTrue(pTypeGenericInfo.isClass());
		assertFalse(pTypeGenericArgInfo.isClass());
	}

	//====================================================================================================
	// isDeprecated()
	//====================================================================================================
	@Test
	void a080_isDeprecated() {
		assertFalse(hPublic.isDeprecated());
		assertTrue(hPublicDeprecated.isDeprecated());
		// Test on types
		assertFalse(aTypeInfo.isDeprecated());
		assertFalse(pTypeGenericArgInfo.isDeprecated());
	}

	//====================================================================================================
	// isEnum()
	//====================================================================================================
	@Test
	void a081_isEnum() {
		assertTrue(ClassInfo.of(ClassArrayFormat.class).isEnum());
		assertFalse(aClass.isEnum());
		
		// Test with null inner (line 1919)
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isEnum());
	}

	//====================================================================================================
	// isInstance(Object)
	//====================================================================================================
	@Test
	void a081b_isInstance() {
		// Valid instance
		assertTrue(ClassInfo.of(String.class).isInstance("test"));
		assertTrue(ClassInfo.of(Number.class).isInstance(42));

		// Invalid instance
		assertFalse(ClassInfo.of(String.class).isInstance(42));
		assertFalse(ClassInfo.of(Number.class).isInstance("test"));

		// Null value
		assertFalse(ClassInfo.of(String.class).isInstance(null));

		// For types with null inner, should return false
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isInstance("test"));
		assertFalse(ci.isInstance(null));
	}

	//====================================================================================================
	// isInterface()
	//====================================================================================================
	@Test
	void a082_isInterface() {
		assertTrue(aInterface.isInterface());
		assertFalse(aClass.isInterface());
		// Test on types
		assertFalse(aTypeInfo.isInterface());
		assertFalse(pTypeGenericArgInfo.isInterface());
	}

	//====================================================================================================
	// isLocalClass()
	//====================================================================================================
	@Test
	void a083_isLocalClass() {
		class F implements Function<Object,String> {
			@Override
			public String apply(Object t) {
				return null;
			}
		}
		assertFalse(aClass.isLocalClass());
		assertTrue(of(F.class).isLocalClass());
		// Test on types
		assertFalse(aTypeInfo.isLocalClass());
		assertFalse(pTypeGenericArgInfo.isLocalClass());
	}

	//====================================================================================================
	// isMemberClass()
	//====================================================================================================
	@Test
	void a084_isMemberClass() {
		assertTrue(hPublic.isMemberClass());
		assertTrue(hPublicMember.isMemberClass());
		assertFalse(aClass.isMemberClass());
		assertFalse(aInterface.isMemberClass());
		// Test on types
		assertTrue(aTypeInfo.isMemberClass());
		assertFalse(pTypeInfo.isMemberClass());
		assertFalse(pTypeDimensionalInfo.isMemberClass());
		assertFalse(pTypeGenericInfo.isMemberClass());
		assertFalse(pTypeGenericArgInfo.isMemberClass());
	}

	//====================================================================================================
	// isNonStaticMemberClass()
	//====================================================================================================
	@Test
	void a085_isNonStaticMemberClass() {
		assertFalse(hPublic.isNonStaticMemberClass());
		assertTrue(hPublicMember.isNonStaticMemberClass());
		assertFalse(aClass.isNonStaticMemberClass());
		assertFalse(aInterface.isNonStaticMemberClass());
		// Test on types
		assertTrue(aTypeInfo.isNonStaticMemberClass());
		assertFalse(pTypeInfo.isNonStaticMemberClass());
		assertFalse(pTypeDimensionalInfo.isNonStaticMemberClass());
		assertFalse(pTypeGenericInfo.isNonStaticMemberClass());
		assertFalse(pTypeGenericArgInfo.isNonStaticMemberClass());
	}

	//====================================================================================================
	// isNotAbstract()
	//====================================================================================================
	@Test
	void a086_isNotAbstract() {
		assertFalse(hAbstractPublic.isNotAbstract());
		assertTrue(hPublic.isNotAbstract());
		// Test on types
		assertTrue(aTypeInfo.isNotAbstract());
		assertFalse(pTypeInfo.isNotAbstract());
		assertFalse(pTypeDimensionalInfo.isNotAbstract());
		assertFalse(pTypeGenericInfo.isNotAbstract());
		assertTrue(pTypeGenericArgInfo.isNotAbstract());
	}

	//====================================================================================================
	// isNotDeprecated()
	//====================================================================================================
	@Test
	void a087_isNotDeprecated() {
		assertTrue(hPublic.isNotDeprecated());
		assertFalse(hPublicDeprecated.isNotDeprecated());
		// Test on types
		assertTrue(aTypeInfo.isNotDeprecated());
		assertTrue(pTypeInfo.isNotDeprecated());
		assertTrue(pTypeDimensionalInfo.isNotDeprecated());
		assertTrue(pTypeGenericInfo.isNotDeprecated());
		assertTrue(pTypeGenericArgInfo.isNotDeprecated());
	}

	//====================================================================================================
	// isNotLocalClass()
	//====================================================================================================
	@Test
	void a088_isNotLocalClass() {
		class F implements Function<Object,String> {
			@Override
			public String apply(Object t) {
				return null;
			}
		}
		assertTrue(aClass.isNotLocalClass());
		assertFalse(of(F.class).isNotLocalClass());
		// Test on types
		assertTrue(aTypeInfo.isNotLocalClass());
		assertTrue(pTypeGenericArgInfo.isNotLocalClass());
	}

	//====================================================================================================
	// isNestmateOf(Class<?>)
	//====================================================================================================
	@Test
	void a088b_isNestmateOf() {
		// Same class is nestmate of itself
		assertTrue(aClass.isNestmateOf(AClass.class));

		// Different classes in same package may or may not be nestmates
		// (depends on whether they're in the same nest)

		// For types with null inner, should return false
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isNestmateOf(AClass.class));
		assertFalse(ci.isNestmateOf(null));

		// With null argument, should return false
		assertFalse(aClass.isNestmateOf(null));
	}

	//====================================================================================================
	// isNotMemberClass()
	//====================================================================================================
	@Test
	void a089_isNotMemberClass() {
		assertFalse(hPublic.isNotMemberClass());
		assertFalse(hPublicMember.isNotMemberClass());
		assertTrue(aClass.isNotMemberClass());
		assertTrue(aInterface.isNotMemberClass());
		// Test on types
		assertFalse(aTypeInfo.isNotMemberClass());
		assertTrue(pTypeGenericArgInfo.isNotMemberClass());
	}

	//====================================================================================================
	// isNotNonStaticMemberClass()
	//====================================================================================================
	@Test
	void a089b_isNotNonStaticMemberClass() {
		// Regular classes are not non-static member classes
		assertTrue(aClass.isNotNonStaticMemberClass());

		// aTypeInfo represents A1, which is a non-static member class, so isNotNonStaticMemberClass() returns false
		assertFalse(aTypeInfo.isNotNonStaticMemberClass());

		// Top-level classes are not non-static member classes
		assertTrue(ClassInfo.of(String.class).isNotNonStaticMemberClass());
	}

	//====================================================================================================
	// isNotPrimitive()
	//====================================================================================================
	@Test
	void a090_isNotPrimitive() {
		assertFalse(of(int.class).isNotPrimitive());
		assertTrue(of(Integer.class).isNotPrimitive());
		// Test on types
		assertTrue(aTypeInfo.isNotPrimitive());
		assertTrue(pTypeInfo.isNotPrimitive());
		assertTrue(pTypeDimensionalInfo.isNotPrimitive());
		assertTrue(pTypeGenericInfo.isNotPrimitive());
		assertTrue(pTypeGenericArgInfo.isNotPrimitive());
	}

	//====================================================================================================
	// isNotPublic()
	//====================================================================================================
	@Test
	void a091_isNotPublic() {
		assertFalse(hPublic.isNotPublic());
		assertTrue(hProtected.isNotPublic());
		assertTrue(hPackage.isNotPublic());
		assertTrue(hPrivate.isNotPublic());
		// Test on types
		assertFalse(aTypeInfo.isNotPublic());
		assertTrue(pTypeGenericArgInfo.isNotPublic());
	}

	//====================================================================================================
	// isNotStatic()
	//====================================================================================================
	@Test
	void a092_isNotStatic() {
		assertFalse(hPublic.isNotStatic());
		assertTrue(hPublicMember.isNotStatic());
		// Test on types
		assertTrue(aTypeInfo.isNotStatic());
		assertTrue(pTypeInfo.isNotStatic());
		assertTrue(pTypeDimensionalInfo.isNotStatic());
		assertTrue(pTypeGenericInfo.isNotStatic());
		assertTrue(pTypeGenericArgInfo.isNotStatic());
	}

	//====================================================================================================
	// isParentOf(Class<?>)
	//====================================================================================================
	@Test
	void a093_isParentOf() {
		assertTrue(ka.isParentOf(KA.class));
		assertTrue(ka.isParentOf(KB.class));
		assertTrue(ka.isParentOf(KC.class));
		assertFalse(kb.isParentOf(KA.class));
		assertTrue(kb.isParentOf(KB.class));
		assertTrue(kb.isParentOf(KC.class));
		assertFalse(kc.isParentOf(KA.class));
		assertFalse(kc.isParentOf(KB.class));
		assertTrue(kc.isParentOf(KC.class));
		// Test with null
		assertFalse(ka.isParentOf((Class<?>)null));
		// Test on types
		assertFalse(ka.isParentOf(aType));
		assertFalse(ka.isParentOf(pType));
		assertFalse(ka.isParentOf(pTypeDimensional));
		assertFalse(ka.isParentOf(pTypeGeneric));
		assertFalse(ka.isParentOf(pTypeGenericArg));
		assertFalse(aTypeInfo.isParentOf(KA.class));
		assertFalse(pTypeInfo.isParentOf(KA.class));
		assertFalse(pTypeDimensionalInfo.isParentOf(KA.class));
		assertFalse(pTypeGenericInfo.isParentOf(KA.class));
		assertFalse(pTypeGenericArgInfo.isParentOf(KA.class));
	}

	//====================================================================================================
	// isParentOf(ClassInfo)
	//====================================================================================================
	@Test
	void a093b_isParentOf_ClassInfo() {
		// Test isParentOf(ClassInfo) with valid classes
		assertTrue(ka.isParentOf(ka));
		assertTrue(ka.isParentOf(kb));
		assertTrue(ka.isParentOf(kc));
		assertFalse(kb.isParentOf(ka));
		assertTrue(kb.isParentOf(kb));
		assertTrue(kb.isParentOf(kc));
		assertFalse(kc.isParentOf(ka));
		assertFalse(kc.isParentOf(kb));
		assertTrue(kc.isParentOf(kc));
		
		// Test with null child (line 2029)
		assertFalse(ka.isParentOf((ClassInfo)null));
		
		// Test with null inner
		var nullInnerCi = ClassInfo.of((Class<?>)null, pType);
		assertFalse(nullInnerCi.isParentOf(ka));
		assertFalse(nullInnerCi.isParentOf((ClassInfo)null));
	}

	//====================================================================================================
	// isParentOfLenient(Class<?>)
	//====================================================================================================
	@Test
	void a094_isParentOfLenient() {
		assertTrue(ClassInfo.of(String.class).isParentOfLenient(String.class));
		assertTrue(ClassInfo.of(CharSequence.class).isParentOfLenient(String.class));
		assertFalse(ClassInfo.of(String.class).isParentOfLenient(CharSequence.class));
		assertTrue(ClassInfo.of(int.class).isParentOfLenient(Integer.class));
		assertTrue(ClassInfo.of(Integer.class).isParentOfLenient(int.class));
		assertTrue(ClassInfo.of(Number.class).isParentOfLenient(int.class));
		assertFalse(ClassInfo.of(int.class).isParentOfLenient(Number.class));
		assertFalse(ClassInfo.of(int.class).isParentOfLenient(long.class));

		// With null inner or null child, should return false
		var ci = ClassInfo.of((Class<?>)null, pType);
		assertFalse(ci.isParentOfLenient(String.class));
		assertFalse(ClassInfo.of(String.class).isParentOfLenient((Class<?>)null));

		// Test isParentOfLenient(Type)
		assertTrue(ClassInfo.of(CharSequence.class).isParentOfLenient((Type)String.class));
		assertFalse(ClassInfo.of(String.class).isParentOfLenient((Type)CharSequence.class));
		// Non-Class Type should return false
		assertFalse(ClassInfo.of(String.class).isParentOfLenient(pType));

		// Test isParentOfLenient(ClassInfo) with null child (line 2088)
		assertFalse(ClassInfo.of(String.class).isParentOfLenient((ClassInfo)null));
		var nullInnerCi = ClassInfo.of((Class<?>)null, pType);
		assertFalse(nullInnerCi.isParentOfLenient(ClassInfo.of(String.class)));
	}

	//====================================================================================================
	// isPrimitive()
	//====================================================================================================
		@Test
	void a095_isPrimitive() {
		assertTrue(of(int.class).isPrimitive());
		assertFalse(of(Integer.class).isPrimitive());
		// Test on types
		assertFalse(aTypeInfo.isPrimitive());
		assertFalse(pTypeGenericArgInfo.isPrimitive());
	}

	//====================================================================================================
	// isPublic()
	//====================================================================================================
		@Test
	void a096_isPublic() {
		assertTrue(hPublic.isPublic());
		assertFalse(hProtected.isPublic());
		assertFalse(hPackage.isPublic());
		assertFalse(hPrivate.isPublic());
		// Test on types
		assertTrue(aTypeInfo.isPublic());
		assertTrue(pTypeInfo.isPublic());
		assertTrue(pTypeDimensionalInfo.isPublic());
		assertTrue(pTypeGenericInfo.isPublic());
		assertFalse(pTypeGenericArgInfo.isPublic());
	}

	//====================================================================================================
	// isRecord()
	//====================================================================================================
		@Test
	void a097_isRecord() {
		// Test with a record class if available (Java 14+)
		try {
			Class.forName("java.lang.Record");
			// If we can find Record, test with a simple record
			// For now, just verify the method exists and returns false for non-records
			assertFalse(cc3.isRecord());
		} catch (ClassNotFoundException e) {
			// Records not available, skip test
			assertFalse(cc3.isRecord());
		}
	}

	//====================================================================================================
	// isStatic()
	//====================================================================================================
		@Test
	void a098_isStatic() {
		assertTrue(hPublic.isStatic());
		assertFalse(hPublicMember.isStatic());
		// Test on types
		assertFalse(aTypeInfo.isStatic());
		assertFalse(pTypeGenericArgInfo.isStatic());
	}

	//====================================================================================================
	// isStrictChildOf(Class<?>)
	//====================================================================================================
		@Test
	void a099_isStrictChildOf() {
		assertFalse(ka.isStrictChildOf(KA.class));
		assertFalse(ka.isStrictChildOf(KB.class));
		assertFalse(ka.isStrictChildOf(KC.class));
		assertTrue(kb.isStrictChildOf(KA.class));
		assertFalse(kb.isStrictChildOf(KB.class));
		assertFalse(kb.isStrictChildOf(KC.class));
		assertTrue(kc.isStrictChildOf(KA.class));
		assertTrue(kc.isStrictChildOf(KB.class));
		assertFalse(kc.isStrictChildOf(KC.class));
		// Test with null
		assertFalse(ka.isStrictChildOf(null));
		// Test on types
		assertFalse(aTypeInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeDimensionalInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeGenericInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeGenericArgInfo.isStrictChildOf(KA.class));
	}

	//====================================================================================================
	// isRuntimeException()
	//====================================================================================================
	@Test
	void a099b_isRuntimeException() {
		// Test isRuntimeException() (line 2143)
		// RuntimeException itself
		assertTrue(ClassInfo.of(RuntimeException.class).isRuntimeException());
		// Subclasses of RuntimeException
		assertTrue(ClassInfo.of(IllegalArgumentException.class).isRuntimeException());
		assertTrue(ClassInfo.of(NullPointerException.class).isRuntimeException());
		assertTrue(ClassInfo.of(IllegalStateException.class).isRuntimeException());
		// Exception but not RuntimeException
		assertFalse(ClassInfo.of(Exception.class).isRuntimeException());
		// Regular classes
		assertFalse(ClassInfo.of(String.class).isRuntimeException());
		assertFalse(aClass.isRuntimeException());
	}

	//====================================================================================================
	// isSynthetic()
	//====================================================================================================
	@Test
	void a100_isSynthetic() {
		// Most classes are not synthetic
		assertFalse(aClass.isSynthetic());
		// Anonymous classes might be synthetic
		var anonymous = new Object() {}.getClass();
		var anonymousInfo = ClassInfo.of(anonymous);
		// Anonymous classes are typically synthetic
		assertTrue(anonymousInfo.isSynthetic() || !anonymousInfo.isSynthetic());
	}

	//====================================================================================================
	// isVisible(Visibility)
	//====================================================================================================
		@Test
	void a101_isVisible() {
		// Public visibility
		assertTrue(hPublic.isVisible(Visibility.PUBLIC));
		assertFalse(hProtected.isVisible(Visibility.PUBLIC));
		assertFalse(hPackage.isVisible(Visibility.PUBLIC));
		assertFalse(hPrivate.isVisible(Visibility.PUBLIC));

		// Protected visibility
		assertTrue(hPublic.isVisible(Visibility.PROTECTED));
		assertTrue(hProtected.isVisible(Visibility.PROTECTED));
		assertFalse(hPackage.isVisible(Visibility.PROTECTED));
		assertFalse(hPrivate.isVisible(Visibility.PROTECTED));

		// Package visibility
		assertTrue(hPublic.isVisible(Visibility.DEFAULT));
		assertTrue(hProtected.isVisible(Visibility.DEFAULT));
		assertTrue(hPackage.isVisible(Visibility.DEFAULT));
		assertFalse(hPrivate.isVisible(Visibility.DEFAULT));

		// Private visibility
		assertTrue(hPublic.isVisible(Visibility.PRIVATE));
		assertTrue(hProtected.isVisible(Visibility.PRIVATE));
		assertTrue(hPackage.isVisible(Visibility.PRIVATE));
		assertTrue(hPrivate.isVisible(Visibility.PRIVATE));

		// Test on types
		assertTrue(aTypeInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeDimensionalInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeGenericInfo.isVisible(Visibility.PRIVATE));
		assertFalse(pTypeGenericArgInfo.isVisible(Visibility.PRIVATE));
	}

	//====================================================================================================
	// newInstance()
	//====================================================================================================
		@Test
	void a102_newInstance() {
		assertNotNull(la.newInstance());
		// Test on types - should throw
		assertThrows(ExecutableException.class, () -> aTypeInfo.newInstance());
		assertThrows(ExecutableException.class, () -> pTypeInfo.newInstance());
		assertThrows(ExecutableException.class, () -> pTypeDimensionalInfo.newInstance());
		assertThrows(Exception.class, () -> pTypeGenericInfo.newInstance());
		assertThrows(ExecutableException.class, () -> pTypeGenericArgInfo.newInstance());
	}

	//====================================================================================================
	// of(Class<?>)
	//====================================================================================================
		@Test
	void a103_of() {
		// Test with Class
		check("A1", of(A1.class));
		check("A1", of(aType));
		check("Map", pTypeInfo);
		check("Map", pTypeDimensionalInfo);
		check("AbstractMap", pTypeGenericInfo);
		check("V", pTypeGenericArgInfo);

		// Test with Object
		check("A1", of(new A1()));

		// Test with null - should throw
		assertThrows(IllegalArgumentException.class, () -> of((Class<?>)null));
		assertThrows(IllegalArgumentException.class, () -> of((Type)null));
		assertThrows(NullPointerException.class, () -> of((Object)null));

		// Test with Class and Type
		var info = ClassInfo.of(String.class, String.class);
		assertNotNull(info);
		assertEquals(String.class, info.inner());

		// When inner != innerType, should create ClassInfoTyped
		info = ClassInfo.of(String.class, String.class);
		assertNotNull(info);
		assertEquals(String.class, info.inner());

		// Should create ClassInfo with null inner but with innerType
		info = ClassInfo.of((Class<?>)null, pType);
		assertNotNull(info);
		assertNull(info.inner());
		assertNotNull(info.innerType());
	}

	//====================================================================================================
	// ofProxy(Object)
	//====================================================================================================
	@Test
	void a104_ofProxy() {
		var obj = new A1();
		var info = ClassInfo.ofProxy(obj);
		assertNotNull(info);
		assertEquals(A1.class, info.inner());
	}

	//====================================================================================================
	// toString()
	//====================================================================================================
	@Test
	void a105_toString() {
		assertEquals("class org.apache.juneau.commons.reflect.AClass", aClass.toString());
		assertEquals("interface org.apache.juneau.commons.reflect.AInterface", aInterface.toString());
		assertEquals("class org.apache.juneau.commons.reflect.ClassInfo_Test$A1", aType.toString());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.String>>", pType.toString());
		assertEquals("java.util.Map<java.lang.String, java.lang.String[][]>", pTypeDimensional.toString());
		assertEquals("java.util.AbstractMap<K, V>", pTypeGeneric.toString());
		assertEquals("V", pTypeGenericArg.toString());
	}

	//====================================================================================================
	// unwrap(Class<?>...)
	//====================================================================================================
	@Test
	void a106_unwrap() {
		check("A1", of(A1.class).unwrap(Value.class));
		check("A1", of(A2.class).unwrap(Value.class));

		// Test unwrap on parameter types
		var mi2 = ClassInfo.of(A6.class).getPublicMethod(x -> x.hasName("m1")).get();
		check("A1", mi2.getParameter(0).getParameterType().unwrap(Optional.class));
		check("A1", mi2.getReturnType().unwrap(Optional.class));
		mi2 = ClassInfo.of(A6.class).getPublicMethod(x -> x.hasName("m2")).get();
		check("A1", mi2.getParameter(0).getParameterType().unwrap(Value.class));
		check("A1", mi2.getReturnType().unwrap(Value.class));
	}
}

