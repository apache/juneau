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
import static org.apache.juneau.Context.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.reflect.ElementFlag.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.common.reflect.ClassInfo.*;
import static org.apache.juneau.common.reflect.ClassNameFormat.*;
import static org.apache.juneau.common.reflect.ClassArrayFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.collections.*;
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
			String actual = l
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else if (o instanceof Iterable) {
			var actual = StreamSupport.stream(((Iterable<?>)o).spliterator(), false)
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = t -> {
		if (t == null)
			return null;
		if (t instanceof Class)
			return ((Class<?>)t).getSimpleName();
		if (t instanceof Package)
			return ((Package)t).getName();
		if (t instanceof PackageInfo)
			return ((PackageInfo)t).getName();
		if (t instanceof ClassInfo)
			return ((ClassInfo)t).getNameSimple();
		if (t instanceof MethodInfo)
			return ((MethodInfo)t).getDeclaringClass().getNameSimple() + '.' + ((MethodInfo)t).getShortName();
		if (t instanceof ConstructorInfo)
			return ((ConstructorInfo)t).getShortName();
		if (t instanceof FieldInfo)
			return ((FieldInfo)t).getDeclaringClass().getNameSimple() + '.' + ((FieldInfo)t).getName();
		if (t instanceof A)
			return "@A(" + ((A)t).value() + ")";
		if (t instanceof PA)
			return "@PA(" + ((PA)t).value() + ")";
		if (t instanceof AConfig)
			return "@AConfig(" + ((AConfig)t).value() + ")";
		if (t instanceof AnnotationInfo)
			return ClassInfo_Test.TO_STRING.apply(((AnnotationInfo<?>)t).inner());
		if (t instanceof AnnotationList al) {
			return al.toString();
		}
		return t.toString();
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Initialization
	//-----------------------------------------------------------------------------------------------------------------

	public class A1 {}
	public class A2 extends Value<A1>{}
	public class A3 extends Value<Map<String,List<String>>>{}
	public class A4 extends Value<Map<String,String[][]>>{}
	public static Type aType, pType, pTypeDimensional, pTypeGeneric, pTypeGenericArg;
	static {
		aType = ((ParameterizedType)A2.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pType = ((ParameterizedType)A3.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pTypeDimensional = ((ParameterizedType)A4.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pTypeGeneric = HashMap.class.getGenericSuperclass();
		pTypeGenericArg = ((ParameterizedType)pTypeGeneric).getActualTypeArguments()[1];
	}

	static ClassInfo aTypeInfo=of(aType), pTypeInfo=of(pType), pTypeDimensionalInfo=of(pTypeDimensional), pTypeGenericInfo=of(pTypeGeneric), pTypeGenericArgInfo=of(pTypeGenericArg);
	static ClassInfo aClass=of(AClass.class), aInterface=of(AInterface.class);

	@Test void ofType() {
		check("A1", of(A1.class));
		check("A1", of(aType));
		check("Map", pTypeInfo);
		check("Map", pTypeDimensionalInfo);
		check("AbstractMap", pTypeGenericInfo);
		check("V", pTypeGenericArgInfo);
	}

	@Test void ofTypeOnObject() {
		check("A1", of(new A1()));
	}

	@Test void ofTypeOnNulls() {
		check(null, of((Class<?>)null));
		check(null, of((Type)null));
		check(null, of((Object)null));
	}

	@Test void inner() {
		assertNotNull(of(A1.class).inner());
		assertTrue(of(A1.class).innerType() instanceof Class);
	}

	@Test void resolved() {
		check("A1", of(A1.class).unwrap(Value.class));
		check("A1", of(A2.class).unwrap(Value.class));
	}

	public static class A6 {
		public Optional<A1> m1(Optional<A1> bar) {
			return null;
		}
		public Value<A1> m2(Value<A1> bar) {
			return null;
		}
	}

	@Test void resolvedParams() {
		var mi2 = ClassInfo.of(A6.class).getPublicMethod(x -> x.hasName("m1"));
		check("A1", mi2.getParameter(0).getParameterType().unwrap(Optional.class));
		check("A1", mi2.getReturnType().unwrap(Optional.class));
		mi2 = ClassInfo.of(A6.class).getPublicMethod(x -> x.hasName("m2"));
		check("A1", mi2.getParameter(0).getParameterType().unwrap(Value.class));
		check("A1", mi2.getReturnType().unwrap(Value.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parent classes and interfaces.
	//-----------------------------------------------------------------------------------------------------------------

	interface BI1 {}
	interface BI2 extends BI1 {}
	interface BI3 {}
	interface BI4 {}
	static class BC1 implements BI1, BI2 {}
	static class BC2 extends BC1 implements BI3 {}
	static class BC3 extends BC2 {}

	static ClassInfo bi1=of(BI1.class), bi2=of(BI2.class), bi3=of(BI3.class), bi4=of(BI4.class), bc1=of(BC1.class), bc2=of(BC2.class), bc3=of(BC3.class), object=of(Object.class);

	@Test void getDeclaredInterfaces() {
		check("", bi4.getDeclaredInterfaces());
		check("BI1,BI2", bc1.getDeclaredInterfaces());
		check("BI3", bc2.getDeclaredInterfaces());
		check("", bc3.getDeclaredInterfaces());
	}

	@Test void getDeclaredInterfaces_onType() {
		check("", aTypeInfo.getDeclaredInterfaces());
		check("", pTypeInfo.getDeclaredInterfaces());
		check("", pTypeDimensionalInfo.getDeclaredInterfaces());
		check("Map", pTypeGenericInfo.getDeclaredInterfaces());
		check("", pTypeGenericArgInfo.getDeclaredInterfaces());
	}

	@Test void getDeclaredInterfaces_twice() {
		check("BI1,BI2", bc1.getDeclaredInterfaces());
		check("BI1,BI2", bc1.getDeclaredInterfaces());
	}

	@Test void getInterfaces() {
		check("", bi4.getInterfaces());
		check("BI1,BI2", bc1.getInterfaces());
		check("BI3,BI1,BI2", bc2.getInterfaces());
		check("BI3,BI1,BI2", bc3.getInterfaces());
	}

	@Test void getInterfaces_tiwce() {
		check("BI3,BI1,BI2", bc2.getInterfaces());
		check("BI3,BI1,BI2", bc2.getInterfaces());
	}

	@Test void getParents() {
		check("BC3,BC2,BC1", bc3.getParents());
		check("", object.getParents());
		check("BI1", bi1.getParents());
	}

	@Test void getAllParents() {
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("", object.getAllParents());
		check("BI1", bi1.getAllParents());
	}

	@Test void getAllParents_twice() {
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
	}

	@Test void getParent() {
		check("BC2", bc3.getSuperclass());
		check("BC1", bc2.getSuperclass());
		check("Object", bc1.getSuperclass());
		check(null, object.getSuperclass());
		check(null, bi2.getSuperclass());
		check(null, bi1.getSuperclass());
	}

	@Test void getParent_onType() {
		check("Object", aTypeInfo.getSuperclass());
		check(null, pTypeInfo.getSuperclass());
		check(null, pTypeDimensionalInfo.getSuperclass());
		check("Object", pTypeGenericInfo.getSuperclass());
		check(null, pTypeGenericArgInfo.getSuperclass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Methods
	//-----------------------------------------------------------------------------------------------------------------

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

	@Test void getPublicMethods() {
		check("CC3.c1a(),CC3.c2b(),CC3.c3a(),CC3.i1a(),CC3.i1b(),CC3.i2a(),CC3.i2b()", cc3.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
	}

	@Test void getPublicMethods_twice() {
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
	}

	@Test void getPublicMethods_onType() {
		check("", aTypeInfo.getPublicMethods());
		check("", pTypeGenericArgInfo.getPublicMethods());
	}

	@Test void getAllMethods() {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getMethods());
	}

	@Test void getAllMethods_twice() {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getMethods());
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getMethods());
	}

	@Test void getDeclaredMethods() {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
	}

	@Test void getDeclaredMethods_twice() {
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
	}

	@Test void getDeclaredMethods_onType() {
		check("", aTypeInfo.getDeclaredMethods());
		check("", pTypeGenericArgInfo.getDeclaredMethods());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

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
	static ClassInfo e1=of(E1.class), e2=of(E2.class), e3=of(E3.class), e4=of(E4.class), e5=of(E5.class), e6=of(E6.class);

	@Test void getPublicConstructors() {
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
	}

	@Test void getPublicConstructors_twice() {
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
	}

	@Test void getPublicConstructors_onType() {
		check("A1(ClassInfo_Test)", aTypeInfo.getPublicConstructors());
		check("", pTypeInfo.getPublicConstructors());
		check("", pTypeDimensionalInfo.getPublicConstructors());
		check("", pTypeGenericInfo.getPublicConstructors());
		check("", pTypeGenericArgInfo.getPublicConstructors());
	}

	@Test void getDeclaredConstructors() {
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
	}

	@Test void getDeclaredConstructors_twice() {
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
	}

	@Test void getDeclaredConstructors_onType() {
		check("A1(ClassInfo_Test)", aTypeInfo.getDeclaredConstructors());
		check("", pTypeInfo.getDeclaredConstructors());
		check("", pTypeDimensionalInfo.getDeclaredConstructors());
		check("AbstractMap()", pTypeGenericInfo.getDeclaredConstructors());
		check("", pTypeGenericArgInfo.getDeclaredConstructors());
	}

	@Test void getPublicConstructor_classArgs() {
		check("E1(String)", e1.getPublicConstructor(x -> x.hasParameterTypes(String.class)));
	}

	@Test void getPublicConstructor_objectArgs() {
		check("E1(String)", e1.getPublicConstructor(x -> x.canAccept("foo")));
	}

	@Test void getNoArgConstructor() {
		check("E2()", e2.getNoArgConstructor(Visibility.PRIVATE));
		check("E2()", e2.getNoArgConstructor(Visibility.PROTECTED));
		check("E2()", e2.getNoArgConstructor(Visibility.DEFAULT));
		check(null, e2.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test void getNoArgConstructor_abstractClass() {
		check(null, e3.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test void getNoArgConstructor_innerClass() {
		check("E4(ClassInfo_Test)", e4.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test void getNoArgConstructor_noConstructor() {
		check(null, e6.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test void getPublicNoArgConstructor() {
		check("E1()", e1.getPublicConstructor(ConstructorInfo::hasNoParameters));
	}

	@Test void getConstructor() {
		check("E1(int)", e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PROTECTED) && x.hasParameterTypes(int.class)));
		check("E1(int)", e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PRIVATE) && x.hasParameterTypes(int.class)));
		check(null, e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC) && x.hasParameterTypes(int.class)));
		check("E3()", e3.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)));
		check("E4(ClassInfo_Test)", e4.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)));
		check("E5()", e5.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fields
	//-----------------------------------------------------------------------------------------------------------------

	abstract static class F1 {
		public int f1a;
		public int f1b;
	}
	static class F2 extends F1 {
		public int f1a;
		public int f2b;
		@Deprecated int f2c;
		protected int f2d;
	}
	static ClassInfo f1=of(F1.class), f2=of(F2.class);

	@Test void getPublicFields() {
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
	}

	@Test void getPublicFields_twice() {
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
	}

	@Test void getPublicFields_onType() {
		check("", aTypeInfo.getPublicFields());
	}

	@Test void getDeclaredFields() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
	}

	@Test void getDeclaredFields_twice() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
	}

	@Test void getDeclaredFields_onType() {
		check("A1.this$0", aTypeInfo.getDeclaredFields());
		check("", pTypeInfo.getDeclaredFields());
		check("", pTypeDimensionalInfo.getDeclaredFields());
		check("AbstractMap.keySet,AbstractMap.values", pTypeGenericInfo.getDeclaredFields());
		check("", pTypeGenericArgInfo.getDeclaredFields());
	}

	@Test void getAllFieldsParentFirst() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
	}

	@Test void getAllFieldsParentFirst_twice() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
	}

	static class F3 {
		public int a1;
		int a2;
	}
	static ClassInfo f3=of(F3.class);

	@Test void getPublicField() {
		check("F3.a1", f3.getPublicField(x -> x.hasName("a1")));
		check(null, f3.getPublicField(x -> x.hasName("a2")));
		check(null, f3.getPublicField(x -> x.hasName("a3")));
	}

	@Test void getDeclaredField() {
		check("F3.a1", f3.getDeclaredField(x -> x.hasName("a1")));
		check("F3.a2", f3.getDeclaredField(x -> x.hasName("a2")));
		check(null, f3.getDeclaredField(x -> x.hasName("a3")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@A(1) interface GI1 {}
	@A(2) interface GI2 extends GI1 {}
	@A(3) interface GI3 {}
	@A(4) interface GI4 {}
	@A(5) static class G1 implements GI1, GI2 {}
	@A(6) static class G2 extends G1 implements GI3 {}
	@A(7) static class G3 extends G2 {}
	static class G4 extends G3 {}
	static class G5 implements GI3 {}

	static ClassInfo g3=of(G3.class), g4=of(G4.class), g5=of(G5.class);

	@Test void getAnnotation() {
		check("@A(7)", g3.getAnnotationInfos(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check(null, g3.getAnnotationInfos(B.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check(null, null);
	}

	@Test void getAnnotation_twice() {
		check("@A(7)", g3.getAnnotationInfos(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check("@A(7)", g3.getAnnotationInfos(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));
	}

	@Test void getAnnotation_onParent() {
		check("@A(7)", g4.getAnnotationInfos(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check(null, g4.getAnnotationInfos(B.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check(null, null);
	}

	@Test void getAnnotation_onInterface() {
		check("@A(3)", g5.getAnnotationInfos(A.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check(null, g5.getAnnotationInfos(B.class).findFirst().map(AnnotationInfo::inner).orElse(null));
		check(null, null);
	}

	@Test void hasAnnotation() {
		assertTrue(g3.hasAnnotation(A.class));
		assertFalse(g3.hasAnnotation(B.class));
		assertThrows(IllegalArgumentException.class, ()->g3.hasAnnotation(null));
	}

	@Test void getAnnotations() {
		check("@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g3.getAnnotations(A.class));
		check("@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g4.getAnnotations(A.class));
		check("@A(3)", g5.getAnnotations(A.class));
	}

	@Test void forEachAnnotation() {
		var l1 = list();
		g3.forEachAnnotation(A.class, null, x -> l1.add(x.value()));
		assertList(l1, "2", "1", "3", "5", "6", "7");

		var l2 = list();
		g4.forEachAnnotation(A.class, null, x -> l2.add(x.value()));
		assertList(l2, "2", "1", "3", "5", "6", "7");

		var l3 = list();
		g5.forEachAnnotation(A.class, null, x -> l3.add(x.value()));
		assertList(l3, "3");

		var l4 = list();
		g3.forEachAnnotation(A.class, x -> x.value() == 5, x -> l4.add(x.value()));
		assertList(l4, "5");
	}

	@Test void lastAnnotation() {
		assertEquals(7, g3.lastAnnotation(A.class, null).value());
		assertEquals(7, g4.lastAnnotation(A.class, null).value());
		assertEquals(3, g5.lastAnnotation(A.class, null).value());
		assertEquals(5, g3.lastAnnotation(A.class, x -> x.value() == 5).value());
	}

	@Test void getPackageAnnotation() {
		check("@PA(10)", g3.getPackageAnnotation(PA.class));
	}

	@Test void getPackageAnnotation_onType() {
		check("@PA(10)", aTypeInfo.getPackageAnnotation(PA.class));
		check(null, pTypeInfo.getPackageAnnotation(PA.class));
		check(null, pTypeDimensionalInfo.getPackageAnnotation(PA.class));
		check(null, pTypeGenericInfo.getPackageAnnotation(PA.class));
		check(null, pTypeGenericArgInfo.getPackageAnnotation(PA.class));
	}

	@Test void getAnnotationsMapParentFirst() {
		// Note: Order changed after inlining - interfaces now processed when they appear in hierarchy, not after all classes
		check("@PA(10),@A(2),@A(1),@A(5),@A(3),@A(6),@A(7)", rstream(g3.getAnnotationInfos()).collect(Collectors.toList()));
		check("@PA(10),@A(2),@A(1),@A(5),@A(3),@A(6),@A(7)", rstream(g4.getAnnotationInfos()).collect(Collectors.toList()));
		check("@PA(10),@A(3)", rstream(g5.getAnnotationInfos()).collect(Collectors.toList()));
	}

	@A(1) @AConfig(1) interface GBI1 {}
	@A(2) @AConfig(2) interface GBI2 extends GBI1 {}
	@A(3) @AConfig(3) interface GBI3 {}
	@A(4) @AConfig(4) interface GBI4 {}
	@A(5) @AConfig(5) static class GB1 implements GBI1, GBI2 {}
	@A(6) @AConfig(6) static class GB2 extends GB1 implements GBI3 {}
	@A(7) @AConfig(7) static class GB3 extends GB2 {}
	static class GB4 extends GB3 {}
	static class GB5 implements GBI3 {}

	static ClassInfo gb3=of(GB3.class), gb4=of(GB4.class), gb5=of(GB5.class);

	@Test void getConfigAnnotationsMapParentFirst() {
		check("@AConfig(2),@AConfig(1),@AConfig(5),@AConfig(3),@AConfig(6),@AConfig(7)", rstream(gb3.getAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toList()));
		check("@AConfig(2),@AConfig(1),@AConfig(5),@AConfig(3),@AConfig(6),@AConfig(7)", rstream(gb4.getAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toList()));
		check("@AConfig(3)", rstream(gb5.getAnnotationInfos()).filter(CONTEXT_APPLY_FILTER).collect(Collectors.toList()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	public static class H_Public {}
	static class H_Package{}
	protected static class H_Protected{}
	private static class H_Private{}
	public class H_PublicMember {}
	public abstract class H_AbstractPublic {}
	@Deprecated public class H_PublicDeprecated {}

	static ClassInfo hPublic=of(H_Public.class), hPackage=of(H_Package.class), hProtected=of(H_Protected.class), hPrivate=of(H_Private.class), hPublicMember=of(H_PublicMember.class), hAbstractPublic=of(H_AbstractPublic.class), hPublicDeprecated=of(H_PublicDeprecated.class);  // NOSONAR

	@Test void isDeprecated() {
		assertFalse(hPublic.isDeprecated());
		assertTrue(hPublicDeprecated.isDeprecated());
	}

	@Test void isDeprecated_onType() {
		assertFalse(aTypeInfo.isDeprecated());
		assertFalse(pTypeGenericArgInfo.isDeprecated());
	}

	@Test void isNotDeprecated() {
		assertTrue(hPublic.isNotDeprecated());
		assertFalse(hPublicDeprecated.isNotDeprecated());
	}

	@Test void isNotDeprecated_onType() {
		assertTrue(aTypeInfo.isNotDeprecated());
		assertTrue(pTypeInfo.isNotDeprecated());
		assertTrue(pTypeDimensionalInfo.isNotDeprecated());
		assertTrue(pTypeGenericInfo.isNotDeprecated());
		assertTrue(pTypeGenericArgInfo.isNotDeprecated());
	}

	@Test void isPublic() {
		assertTrue(hPublic.isPublic());
		assertFalse(hProtected.isPublic());
		assertFalse(hPackage.isPublic());
		assertFalse(hPrivate.isPublic());
	}

	@Test void isPublic_onType() {
		assertTrue(aTypeInfo.isPublic());
		assertTrue(pTypeInfo.isPublic());
		assertTrue(pTypeDimensionalInfo.isPublic());
		assertTrue(pTypeGenericInfo.isPublic());
		assertFalse(pTypeGenericArgInfo.isPublic());
	}

	@Test void isNotPublic() {
		assertFalse(hPublic.isNotPublic());
		assertTrue(hProtected.isNotPublic());
		assertTrue(hPackage.isNotPublic());
		assertTrue(hPrivate.isNotPublic());
	}

	@Test void isNotPublic_onType() {
		assertFalse(aTypeInfo.isNotPublic());
		assertTrue(pTypeGenericArgInfo.isNotPublic());
	}

	@Test void isStatic() {
		assertTrue(hPublic.isStatic());
		assertFalse(hPublicMember.isStatic());
	}

	@Test void isStatic_onType() {
		assertFalse(aTypeInfo.isStatic());
		assertFalse(pTypeGenericArgInfo.isStatic());
	}

	@Test void isNotStatic() {
		assertFalse(hPublic.isNotStatic());
		assertTrue(hPublicMember.isNotStatic());
	}

	@Test void isNotStatic_onType() {
		assertTrue(aTypeInfo.isNotStatic());
		assertTrue(pTypeInfo.isNotStatic());
		assertTrue(pTypeDimensionalInfo.isNotStatic());
		assertTrue(pTypeGenericInfo.isNotStatic());
		assertTrue(pTypeGenericArgInfo.isNotStatic());
	}

	@Test void isAbstract() {
		assertTrue(hAbstractPublic.isAbstract());
		assertFalse(pTypeGenericArgInfo.isAbstract());
	}

	@Test void isAbstract_onType() {
		assertFalse(aTypeInfo.isAbstract());
		assertFalse(aTypeInfo.isAbstract());
	}

	@Test void isNotAbstract() {
		assertFalse(hAbstractPublic.isNotAbstract());
		assertTrue(hPublic.isNotAbstract());
	}

	@Test void isNotAbstract_onType() {
		assertTrue(aTypeInfo.isNotAbstract());
		assertFalse(pTypeInfo.isNotAbstract());
		assertFalse(pTypeDimensionalInfo.isNotAbstract());
		assertFalse(pTypeGenericInfo.isNotAbstract());
		assertTrue(pTypeGenericArgInfo.isNotAbstract());
	}

	@Test void isMemberClass() {
		assertTrue(hPublic.isMemberClass());
		assertTrue(hPublicMember.isMemberClass());
		assertFalse(aClass.isMemberClass());
		assertFalse(aInterface.isMemberClass());
	}

	@Test void isMemberClass_onType() {
		assertTrue(aTypeInfo.isMemberClass());
		assertFalse(pTypeInfo.isMemberClass());
		assertFalse(pTypeDimensionalInfo.isMemberClass());
		assertFalse(pTypeGenericInfo.isMemberClass());
		assertFalse(pTypeGenericArgInfo.isMemberClass());
	}

	@Test void isNotMemberClass() {
		assertFalse(hPublic.isNotMemberClass());
		assertFalse(hPublicMember.isNotMemberClass());
		assertTrue(aClass.isNotMemberClass());
		assertTrue(aInterface.isNotMemberClass());
	}

	@Test void isNotMemberClass_onType() {
		assertFalse(aTypeInfo.isNotMemberClass());
		assertTrue(pTypeGenericArgInfo.isNotMemberClass());
	}

	@Test void isNonStaticMemberClass() {
		assertFalse(hPublic.isNonStaticMemberClass());
		assertTrue(hPublicMember.isNonStaticMemberClass());
		assertFalse(aClass.isNonStaticMemberClass());
		assertFalse(aInterface.isNonStaticMemberClass());
	}

	@Test void isNonStaticMemberClass_onType() {
		assertTrue(aTypeInfo.isNonStaticMemberClass());
		assertFalse(pTypeInfo.isNonStaticMemberClass());
		assertFalse(pTypeDimensionalInfo.isNonStaticMemberClass());
		assertFalse(pTypeGenericInfo.isNonStaticMemberClass());
		assertFalse(pTypeGenericArgInfo.isNonStaticMemberClass());
	}

	@Test void isLocalClass() {
		class F implements Function<Object,String>{
			@Override
			public String apply(Object t) {
				return null;
			}
		}
		assertFalse(aClass.isLocalClass());
		assertTrue(of(F.class).isLocalClass());
	}

	@Test void isLocalClass_type() {
		assertFalse(aTypeInfo.isLocalClass());
		assertFalse(pTypeGenericArgInfo.isLocalClass());
	}

	@Test void isNotLocalClass() {
		class F implements Function<Object,String>{
			@Override
			public String apply(Object t) {
				return null;
			}
		}
		assertTrue(aClass.isNotLocalClass());
		assertFalse(of(F.class).isNotLocalClass());
	}

	@Test void isNotLocalClass_type() {
		assertTrue(aTypeInfo.isNotLocalClass());
		assertTrue(pTypeGenericArgInfo.isNotLocalClass());
	}

	@Test void isVisible_public() {
		assertTrue(hPublic.isVisible(Visibility.PUBLIC));
		assertFalse(hProtected.isVisible(Visibility.PUBLIC));
		assertFalse(hPackage.isVisible(Visibility.PUBLIC));
		assertFalse(hPrivate.isVisible(Visibility.PUBLIC));
	}

	@Test void isVisible_protected() {
		assertTrue(hPublic.isVisible(Visibility.PROTECTED));
		assertTrue(hProtected.isVisible(Visibility.PROTECTED));
		assertFalse(hPackage.isVisible(Visibility.PROTECTED));
		assertFalse(hPrivate.isVisible(Visibility.PROTECTED));
	}

	@Test void isVisible_package() {
		assertTrue(hPublic.isVisible(Visibility.DEFAULT));
		assertTrue(hProtected.isVisible(Visibility.DEFAULT));
		assertTrue(hPackage.isVisible(Visibility.DEFAULT));
		assertFalse(hPrivate.isVisible(Visibility.DEFAULT));
	}

	@Test void isVisible_private() {
		assertTrue(hPublic.isVisible(Visibility.PRIVATE));
		assertTrue(hProtected.isVisible(Visibility.PRIVATE));
		assertTrue(hPackage.isVisible(Visibility.PRIVATE));
		assertTrue(hPrivate.isVisible(Visibility.PRIVATE));
	}

	@Test void isVisible_onType() {
		assertTrue(aTypeInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeDimensionalInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeGenericInfo.isVisible(Visibility.PRIVATE));
		assertFalse(pTypeGenericArgInfo.isVisible(Visibility.PRIVATE));
	}

	@Test void isPrimitive() {
		assertTrue(of(int.class).isPrimitive());
		assertFalse(of(Integer.class).isPrimitive());
	}

	@Test void isPrimitive_onType() {
		assertFalse(aTypeInfo.isPrimitive());
		assertFalse(pTypeGenericArgInfo.isPrimitive());
	}

	@Test void isNotPrimitive() {
		assertFalse(of(int.class).isNotPrimitive());
		assertTrue(of(Integer.class).isNotPrimitive());
	}

	@Test void isNotPrimitive_onType() {
		assertTrue(aTypeInfo.isNotPrimitive());
		assertTrue(pTypeInfo.isNotPrimitive());
		assertTrue(pTypeDimensionalInfo.isNotPrimitive());
		assertTrue(pTypeGenericInfo.isNotPrimitive());
		assertTrue(pTypeGenericArgInfo.isNotPrimitive());
	}

	@Test void isInterface() {
		assertTrue(aInterface.isInterface());
		assertFalse(aClass.isInterface());
	}

	@Test void isInterface_onType() {
		assertFalse(aTypeInfo.isInterface());
		assertFalse(pTypeGenericArgInfo.isInterface());
	}

	@Test void isClass() {
		assertTrue(aClass.isClass());
		assertFalse(aInterface.isClass());
	}

	@Test void isClass_onType() {
		assertTrue(aTypeInfo.isClass());
		assertFalse(pTypeInfo.isClass());
		assertFalse(pTypeDimensionalInfo.isClass());
		assertTrue(pTypeGenericInfo.isClass());
		assertFalse(pTypeGenericArgInfo.isClass());
	}

	@Deprecated public abstract static class H2a {}
	private interface H2b {}
	@Deprecated class H2_Deprecated {}
	class H2_NotDeprecated {}
	public class H2_Public {}
	class H2_NotPublic {}
	public static class H2_Static {}
	class H2_NotStatic {}
	class H2_Member {}
	static class H2_StaticMember {}
	abstract class H2_Abstract {}
	class H2_NotAbstract {}

	static ClassInfo h2a=of(H2a.class), h2b=of(H2b.class), h2Deprecated=of(H2_Deprecated.class), h2NotDeprecated=of(H2_NotDeprecated.class), h2Public=of(H2_Public.class), h2NotPublic=of(H2_NotPublic.class), h2Static=of(H2_Static.class), h2NotStatic=of(H2_NotStatic.class), h2Member=of(H2_Member.class), h2StaticMember=of(H2_StaticMember.class), h2Abstract=of(H2_Abstract.class), h2NotAbstract=of(H2_NotAbstract.class);  // NOSONAR

	@Test void isAll() {
		assertTrue(h2a.isAll(DEPRECATED, PUBLIC, STATIC, MEMBER, ABSTRACT, ElementFlag.CLASS));
		assertTrue(h2b.isAll(NOT_DEPRECATED, NOT_PUBLIC, STATIC, ABSTRACT, INTERFACE));
	}

	@Test void isAll_onType() {
		assertTrue(aTypeInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
		assertFalse(pTypeInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
		assertFalse(pTypeDimensionalInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
		assertFalse(pTypeGenericInfo.isAll(PUBLIC, MEMBER, ElementFlag.CLASS));
	}

	@Test void isAll_deprecated() {
		assertTrue(h2Deprecated.is(DEPRECATED));
		assertFalse(h2NotDeprecated.is(DEPRECATED));
	}

	@Test void isAll_notDeprecated() {
		assertFalse(h2Deprecated.is(NOT_DEPRECATED));
		assertTrue(h2NotDeprecated.is(NOT_DEPRECATED));
	}

	@Test void isAll_public() {
		assertTrue(of(H2_Public.class).is(PUBLIC));
		assertFalse(h2NotPublic.is(PUBLIC));
	}

	@Test void isAll_notPublic() {
		assertFalse(of(H2_Public.class).is(NOT_PUBLIC));
		assertTrue(h2NotPublic.is(NOT_PUBLIC));
	}

	@Test void isAll_static() {
		assertTrue(of(H2_Static.class).is(STATIC));
		assertFalse(h2NotStatic.is(STATIC));
	}

	@Test void isAll_notStatic() {
		assertFalse(of(H2_Static.class).is(NOT_STATIC));
		assertTrue(h2NotStatic.is(NOT_STATIC));
	}

	@Test void isAll_member() {
		assertTrue(h2Member.is(MEMBER));
		assertTrue(h2StaticMember.is(MEMBER));
		assertFalse(aClass.is(MEMBER));
	}

	@Test void isAll_notMember() {
		assertFalse(h2Member.is(NOT_MEMBER));
		assertFalse(h2StaticMember.is(NOT_MEMBER));
		assertTrue(aClass.is(NOT_MEMBER));
	}

	@Test void isAll_abstract() {
		assertTrue(of(H2_Abstract.class).is(ABSTRACT));
		assertFalse(h2NotAbstract.is(ABSTRACT));
		assertTrue(aInterface.is(ABSTRACT));
	}

	@Test void isAll_notAbstract() {
		assertFalse(of(H2_Abstract.class).is(NOT_ABSTRACT));
		assertTrue(h2NotAbstract.is(NOT_ABSTRACT));
		assertFalse(aInterface.is(NOT_ABSTRACT));
	}

	@Test void isAll_interface() {
		assertTrue(aInterface.is(INTERFACE));
		assertFalse(aClass.is(INTERFACE));
	}

	@Test void isAll_class() {
		assertFalse(aInterface.is(ElementFlag.CLASS));
		assertTrue(aClass.is(ElementFlag.CLASS));
	}



	//-----------------------------------------------------------------------------------------------------------------
	// Primitive wrappers
	//-----------------------------------------------------------------------------------------------------------------

	static List<Class<?>> primitives = l(boolean.class,byte.class,short.class,char.class,int.class,long.class,float.class,double.class);
	static List<Class<?>> primitiveWrappers = l(Boolean.class,Byte.class,Short.class,Character.class,Integer.class,Long.class,Float.class,Double.class);
	static List<Object> primitiveDefaults = l(false,(byte)0,(short)0,(char)0,0,0L,0f,0d);

	@Test void hasPrimitiveWrapper() {
		for (var c : primitives)
			assertTrue(of(c).hasPrimitiveWrapper());
		for (var c : primitiveWrappers)
			assertFalse(of(c).hasPrimitiveWrapper());
	}

	@Test void hasPrimitiveWrapper_onType() {
		assertFalse(aTypeInfo.hasPrimitiveWrapper());
	}

	@Test void getPrimitiveWrapper() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getPrimitiveWrapper(), primitiveWrappers.get(i));
		assertNull(of(String.class).getPrimitiveWrapper());
	}

	@Test void getPrimitiveWrapper_onType() {
		assertNull(aTypeInfo.getPrimitiveWrapper());
	}

	@Test void getPrimitiveForWrapper() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitiveWrappers.get(i)).getPrimitiveForWrapper(), primitives.get(i));
		assertNull(of(String.class).getPrimitiveForWrapper());
	}

	@Test void getPrimitiveForWrapper_onType() {
		assertNull(aTypeInfo.getPrimitiveForWrapper());
	}

	@Test void getWrapperIfPrimitive() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getWrapperIfPrimitive(), primitiveWrappers.get(i));
		assertEquals(of(String.class).getWrapperIfPrimitive(), String.class);
	}

	@Test void getWrapperIfPrimitive_onType() {
		assertEquals("class org.apache.juneau.common.reflect.ClassInfo_Test$A1", aTypeInfo.getWrapperIfPrimitive().toString());
		assertEquals("interface java.util.Map", pTypeInfo.getWrapperIfPrimitive().toString());
		assertEquals("interface java.util.Map", pTypeDimensionalInfo.getWrapperIfPrimitive().toString());
		assertEquals("class java.util.AbstractMap", pTypeGenericInfo.getWrapperIfPrimitive().toString());
		assertEquals(null, pTypeGenericArgInfo.getWrapperIfPrimitive());
	}

	@Test void getWrapperInfoIfPrimitive() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getWrapperInfoIfPrimitive().inner(), primitiveWrappers.get(i));
		assertEquals(of(String.class).getWrapperInfoIfPrimitive().inner(), String.class);
	}

	@Test void getWrapperInfoIfPrimitive_onType() {
		assertEquals(aTypeInfo.getWrapperInfoIfPrimitive().innerType(), aType);
		check("V", pTypeGenericArgInfo.getWrapperInfoIfPrimitive());
	}

	@Test void getPrimitiveDefault() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getPrimitiveDefault(), primitiveDefaults.get(i));
		assertNull(of(String.class).getPrimitiveDefault());
	}

	@Test void getPrimitiveDefault_onType() {
		assertNull(aTypeInfo.getPrimitiveDefault());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	public class J1 {}
	public static class J2 {}

	static ClassInfo j1=of(J1.class), j2=of(J2.class), j1_3d=of(J1[][].class), j2_3d=of(J2[][].class);

	@Test void getFullName_simple() {
		assertEquals("org.apache.juneau.common.reflect.AClass", aClass.getNameFull());
	}

	@Test void getFullName_simpleTwice() {
		assertEquals("org.apache.juneau.common.reflect.AClass", aClass.getNameFull());
		assertEquals("org.apache.juneau.common.reflect.AClass", aClass.getNameFull());
	}

	@Test void getFullName_simpleArray() {
		assertEquals("org.apache.juneau.common.reflect.AClass[][]", of(AClass[][].class).getNameFull());
	}

	@Test void getFullName_inner() {
		assertEquals("org.apache.juneau.common.reflect.ClassInfo_Test$J1", j1.getNameFull());
		assertEquals("org.apache.juneau.common.reflect.ClassInfo_Test$J2", j2.getNameFull());
	}

	@Test void getFullName_innerArray() {
		assertEquals("org.apache.juneau.common.reflect.ClassInfo_Test$J1[][]", j1_3d.getNameFull());
		assertEquals("org.apache.juneau.common.reflect.ClassInfo_Test$J2[][]", j2_3d.getNameFull());
	}

	@Test void getFullName_primitive() {
		assertEquals("int", of(int.class).getNameFull());
	}

	@Test void getFullName_primitiveArray() {
		assertEquals("int[][]", of(int[][].class).getNameFull());
	}

	@Test void getFullName_simpleType() {
		assertEquals("org.apache.juneau.common.reflect.ClassInfo_Test$A1", aTypeInfo.getNameFull());
	}

	@Test void getFullName_complexType() {
		assertEquals("java.util.Map<java.lang.String,java.util.List<java.lang.String>>", pTypeInfo.getNameFull());
	}

	@Test void getFullName_dimensionalType() {
		assertEquals("java.util.Map<java.lang.String,java.lang.String[][]>", pTypeDimensionalInfo.getNameFull());
	}

	@Test void getFullName_genericType() {
		assertEquals("java.util.AbstractMap<K,V>", pTypeGenericInfo.getNameFull());
	}

	@Test void getFullName_genericTypeArg() {
		assertEquals("V", pTypeGenericArgInfo.getNameFull());
	}

	@Test void getFullName_localClass() {
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {}
		assertEquals("org.apache.juneau.common.reflect.ClassInfo_Test$1LocalClass", of(LocalClass.class).getNameFull());
	}

	@Test void getShortName_simple() {
		assertEquals("AClass", aClass.getNameShort());
	}

	@Test void getShortName_simpleTwice() {
		assertEquals("AClass", aClass.getNameShort());
		assertEquals("AClass", aClass.getNameShort());
	}

	@Test void getShortName_simpleArray() {
		assertEquals("AClass[][]", of(AClass[][].class).getNameShort());
	}

	@Test void getShortName_inner() {
		assertEquals("ClassInfo_Test$J1", j1.getNameShort());
		assertEquals("ClassInfo_Test$J2", j2.getNameShort());
	}

	@Test void getShortName_innerArray() {
		assertEquals("ClassInfo_Test$J1[][]", j1_3d.getNameShort());
		assertEquals("ClassInfo_Test$J2[][]", j2_3d.getNameShort());
	}

	@Test void getShortName_primitive() {
		assertEquals("int", of(int.class).getNameShort());
	}

	@Test void getShortName_primitiveArray() {
		assertEquals("int[][]", of(int[][].class).getNameShort());
	}

	@Test void getShortName_simpleType() {
		assertEquals("ClassInfo_Test$A1", aTypeInfo.getNameShort());
	}

	@Test void getShortName_complexType() {
		assertEquals("Map<String,List<String>>", pTypeInfo.getNameShort());
	}

	@Test void getShortName_dimensionalType() {
		assertEquals("Map<String,String[][]>", pTypeDimensionalInfo.getNameShort());
	}

	@Test void getShortName_genericType() {
		assertEquals("AbstractMap<K,V>", pTypeGenericInfo.getNameShort());
	}

	@Test void getShortName_genericTypeArg() {
		assertEquals("V", pTypeGenericArgInfo.getNameShort());
	}

	@Test void getShortName_localClass() {
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {}
		assertEquals("ClassInfo_Test$LocalClass", of(LocalClass.class).getNameShort());
	}

	@Test void getSimpleName_simple() {
		assertEquals("AClass", aClass.getNameSimple());
	}

	@Test void getSimpleName_simpleTwice() {
		assertEquals("AClass", aClass.getNameSimple());
		assertEquals("AClass", aClass.getNameSimple());
	}

	@Test void getSimpleName_simpleArray() {
		assertEquals("AClass[][]", of(AClass[][].class).getNameSimple());
	}

	@Test void getSimpleName_inner() {
		assertEquals("J1", j1.getNameSimple());
		assertEquals("J2", j2.getNameSimple());
	}

	@Test void getSimpleName_innerArray() {
		assertEquals("J1[][]", j1_3d.getNameSimple());
		assertEquals("J2[][]", j2_3d.getNameSimple());
	}

	@Test void getSimpleName_primitive() {
		assertEquals("int", of(int.class).getNameSimple());
	}

	@Test void getSimpleName_primitiveArray() {
		assertEquals("int[][]", of(int[][].class).getNameSimple());
	}

	@Test void getSimpleName_simpleType() {
		assertEquals("A1", aTypeInfo.getNameSimple());
	}

	@Test void getSimpleName_complexType() {
		assertEquals("Map", pTypeInfo.getNameSimple());
	}

	@Test void getSimpleName_dimensionalType() {
		assertEquals("Map", pTypeDimensionalInfo.getNameSimple());
	}

	@Test void getSimpleName_genericType() {
		assertEquals("AbstractMap", pTypeGenericInfo.getNameSimple());
	}

	@Test void getSimpleName_genericTypeArg() {
		assertEquals("V", pTypeGenericArgInfo.getNameSimple());
	}

	@Test void getSimpleName_localClass() {
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {}
		assertEquals("LocalClass", of(LocalClass.class).getNameSimple());
	}

	@Test void getName() {
		assertEquals("org.apache.juneau.common.reflect.AClass", aClass.getName());
		assertEquals("java.util.AbstractMap", pTypeGenericInfo.getName());
		assertEquals("V", pTypeGenericArgInfo.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Hierarchy
	//-----------------------------------------------------------------------------------------------------------------

	public interface KA {}
	public static class KB implements KA {}
	public static class KC extends KB {}

	static ClassInfo ka=of(KA.class), kb=of(KB.class), kc=of(KC.class);

	@Test void isParentOf() {
		assertTrue(ka.isParentOf(KA.class));
		assertTrue(ka.isParentOf(KB.class));
		assertTrue(ka.isParentOf(KC.class));
		assertFalse(kb.isParentOf(KA.class));
		assertTrue(kb.isParentOf(KB.class));
		assertTrue(kb.isParentOf(KC.class));
		assertFalse(kc.isParentOf(KA.class));
		assertFalse(kc.isParentOf(KB.class));
		assertTrue(kc.isParentOf(KC.class));
	}

	@Test void isParentOf_null() {
		assertFalse(ka.isParentOf(null));
	}

	@Test void isParentOf_type() {
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

	@Test void isChildOf() {
		assertTrue(ka.isChildOf(KA.class));
		assertFalse(ka.isChildOf(KB.class));
		assertFalse(ka.isChildOf(KC.class));
		assertTrue(kb.isChildOf(KA.class));
		assertTrue(kb.isChildOf(KB.class));
		assertFalse(kb.isChildOf(KC.class));
		assertTrue(kc.isChildOf(KA.class));
		assertTrue(kc.isChildOf(KB.class));
		assertTrue(kc.isChildOf(KC.class));
	}

	@Test void isChildOf_null() {
		assertFalse(ka.isChildOf((Class<?>)null));
	}

	@Test void isChildOf_type() {
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
	}

	@Test void isStrictChildOf() {
		assertFalse(ka.isStrictChildOf(KA.class));
		assertFalse(ka.isStrictChildOf(KB.class));
		assertFalse(ka.isStrictChildOf(KC.class));
		assertTrue(kb.isStrictChildOf(KA.class));
		assertFalse(kb.isStrictChildOf(KB.class));
		assertFalse(kb.isStrictChildOf(KC.class));
		assertTrue(kc.isStrictChildOf(KA.class));
		assertTrue(kc.isStrictChildOf(KB.class));
		assertFalse(kc.isStrictChildOf(KC.class));
	}

	@Test void isStrictChildOf_null() {
		assertFalse(ka.isStrictChildOf(null));
	}

	@Test void isStrictChildOf_type() {
		assertFalse(aTypeInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeDimensionalInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeGenericInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeGenericArgInfo.isStrictChildOf(KA.class));
	}

	@Test void isChildOfAny() {
		assertTrue(ka.isChildOfAny(KA.class));
		assertFalse(ka.isChildOfAny(KB.class));
		assertFalse(ka.isChildOfAny(KC.class));
		assertTrue(kb.isChildOfAny(KA.class));
		assertTrue(kb.isChildOfAny(KB.class));
		assertFalse(kb.isChildOfAny(KC.class));
		assertTrue(kc.isChildOfAny(KA.class));
		assertTrue(kc.isChildOfAny(KB.class));
		assertTrue(kc.isChildOfAny(KC.class));
	}

	@Test void isChildOfAny_type() {
		assertFalse(aTypeInfo.isChildOfAny(KA.class));
		assertFalse(pTypeInfo.isChildOfAny(KA.class));
		assertFalse(pTypeDimensionalInfo.isChildOfAny(KA.class));
		assertFalse(pTypeGenericInfo.isChildOfAny(KA.class));
		assertFalse(pTypeGenericArgInfo.isChildOfAny(KA.class));
	}

	@Test void is() {
		assertTrue(ka.is(KA.class));
		assertFalse(ka.is(KB.class));
		assertFalse(ka.is(KC.class));
		assertFalse(kb.is(KA.class));
		assertTrue(kb.is(KB.class));
		assertFalse(kb.is(KC.class));
		assertFalse(kc.is(KA.class));
		assertFalse(kc.is(KB.class));
		assertTrue(kc.is(KC.class));
	}

	@Test void is_ClassInfo() {
		assertTrue(ka.is(of(KA.class)));
		assertFalse(ka.is(of(KB.class)));
		assertFalse(ka.is(of(KC.class)));
		assertFalse(kb.is(of(KA.class)));
		assertTrue(kb.is(of(KB.class)));
		assertFalse(kb.is(of(KC.class)));
		assertFalse(kc.is(of(KA.class)));
		assertFalse(kc.is(of(KB.class)));
		assertTrue(kc.is(of(KC.class)));
	}

	@Test void is_ClassInfo_genType() {
		assertFalse(pTypeGenericArgInfo.is(of(KA.class)));
	}

	@Test void isAnyType() {
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

	@Test void is_type() {
		assertFalse(aTypeInfo.is(KA.class));
		assertFalse(pTypeInfo.is(KA.class));
		assertFalse(pTypeDimensionalInfo.is(KA.class));
		assertFalse(pTypeGenericInfo.is(KA.class));
		assertFalse(pTypeGenericArgInfo.is(KA.class));
	}

	@Test void getPackage() {
		check("org.apache.juneau.common.reflect", ka.getPackage().getName());
	}

	@Test void getPackage_type() {
		check("org.apache.juneau.common.reflect", aTypeInfo.getPackage());
		check("java.util", pTypeInfo.getPackage());
		check("java.util", pTypeDimensionalInfo.getPackage());
		check("java.util", pTypeGenericInfo.getPackage());
		check(null, pTypeGenericArgInfo.getPackage());
	}

	@Test void hasPackage() {
		assertTrue(ka.hasPackage());
	}

	@Test void hasPackage_type() {
		assertTrue(aTypeInfo.hasPackage());
		assertTrue(pTypeInfo.hasPackage());
		assertTrue(pTypeDimensionalInfo.hasPackage());
		assertTrue(pTypeGenericInfo.hasPackage());
		assertFalse(pTypeGenericArgInfo.hasPackage());
	}

	@Test void getDimensions() {
		assertEquals(0, ka.getDimensions());
		assertEquals(2, of(KA[][].class).getDimensions());
	}

	@Test void getDimensions_type() {
		assertEquals(0, aTypeInfo.getDimensions());
		assertEquals(0, pTypeInfo.getDimensions());
		assertEquals(0, pTypeDimensionalInfo.getDimensions());
		assertEquals(0, pTypeGenericInfo.getDimensions());
		assertEquals(0, pTypeGenericArgInfo.getDimensions());
	}

	@Test void getComponentType() {
		assertEquals(ka, ka.getComponentType());  // Not an array, returns this
		check("KA", of(KA[][].class).getComponentType());
	}

	@Test void getComponentType_twice() {
		assertEquals(ka, ka.getComponentType());  // Not an array, returns this
		assertEquals(ka, ka.getComponentType());  // Not an array, returns this
	}

	@Test void getComponentType_type() {
		assertEquals(aTypeInfo, aTypeInfo.getComponentType());  // Not an array, returns this
		assertEquals(pTypeInfo, pTypeInfo.getComponentType());  // Not an array, returns this
		assertEquals(pTypeDimensionalInfo, pTypeDimensionalInfo.getComponentType());  // Not an array (ParameterizedType, not array)
		assertEquals(pTypeGenericInfo, pTypeGenericInfo.getComponentType());  // Not an array, returns this
		assertEquals(pTypeGenericArgInfo, pTypeGenericArgInfo.getComponentType());  // Not an array, returns this
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation
	//-----------------------------------------------------------------------------------------------------------------

	public static class LA {}

	static ClassInfo la=of(LA.class);

	@Test void newInstance() {
		assertNotNull(la.newInstance());
	}

	@Test void newInstance_type() {
		assertThrows(ExecutableException.class, ()->aTypeInfo.newInstance());
		assertThrows(ExecutableException.class, ()->pTypeInfo.newInstance());
		assertThrows(ExecutableException.class, ()->pTypeDimensionalInfo.newInstance());
		assertThrows(Exception.class, ()->pTypeGenericInfo.newInstance());
		assertThrows(ExecutableException.class, ()->pTypeGenericArgInfo.newInstance());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter types
	//-----------------------------------------------------------------------------------------------------------------

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
	@SuppressWarnings({ "serial"})
	public static class MI<X> extends HashMap<String,X[]> {}
	@SuppressWarnings({ "serial"})
	public static class MJ<X extends Number> extends HashMap<String,X> {}
	public class MK {}
	@SuppressWarnings({ "serial"})
	public class ML extends HashMap<String,MK> {}
	public static class MM {
		@SuppressWarnings({ "serial"})
		public class MN extends HashMap<String,MM> {}
	}

	static ClassInfo ma=of(MA.class), mb=of(MB.class), mc=of(MC.class), md=of(MD.class), me=of(ME.class), mf=of(MF.class), mg=of(MG.class), mh=of(MH.class), mi=of(MI.class), mj=of(MJ.class), ml=of(ML.class), mn=of(MM.MN.class);

	@Test void getParameterType_simpleMap() {
		check("String", ma.getParameterType(0, HashMap.class));
		check("Integer", ma.getParameterType(1, HashMap.class));
		check("String", mb.getParameterType(0, HashMap.class));
		check("Integer", mb.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_outOfBounds() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid type index. index=2, argsLength=2", ()->ma.getParameterType(2, HashMap.class));
	}

	@Test void getParameterType_notASubclass() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Class 'AClass' is not a subclass of parameterized type 'HashMap'", ()->aClass.getParameterType(2, HashMap.class));
	}

	@Test void getParameterType_nullParameterizedType() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'pt' cannot be null.", ()->aClass.getParameterType(2, null));
	}

	@Test void getParameterType_notParamerizedType() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Class 'MA' is not a parameterized type", ()->mb.getParameterType(2, MA.class));
	}

	@Test void getParameterType_unresolvedTypes() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Could not resolve variable 'E' to a type.", ()->mc.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_resolvedTypes() {
		check("Integer", md.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_parameterizedTypeVariable() {
		check("HashMap", me.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_arrayParameterType() {
		check("String[]", mf.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_genericArrayTypeParameter() {
		check("HashMap[]", mg.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_genericArrayTypeParameterWithoutTypes() {
		check("LinkedList[]", mh.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_unresolvedGenericArrayType() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Could not resolve variable 'X[]' to a type.", ()->mi.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_wildcardType() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Could not resolve variable 'X' to a type.", ()->mj.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_innerType() {
		check("MK", ml.getParameterType(1, HashMap.class));
	}

	@Test void getParameterType_nestedType() {
		check("MM", mn.getParameterType(1, HashMap.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ClassInfo.isParentOfFuzzyPrimitives(Class)
	// ClassInfo.isParentOfFuzzyPrimitives(Type)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void o01_isParentOfFuzzyPrimitives() {
		assertTrue(ClassInfo.of(String.class).isParentOfLenient(String.class));
		assertTrue(ClassInfo.of(CharSequence.class).isParentOfLenient(String.class));
		assertFalse(ClassInfo.of(String.class).isParentOfLenient(CharSequence.class));
		assertTrue(ClassInfo.of(int.class).isParentOfLenient(Integer.class));
		assertTrue(ClassInfo.of(Integer.class).isParentOfLenient(int.class));
		assertTrue(ClassInfo.of(Number.class).isParentOfLenient(int.class));
		assertFalse(ClassInfo.of(int.class).isParentOfLenient(Number.class));
		assertFalse(ClassInfo.of(int.class).isParentOfLenient(long.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getFormattedName tests
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	public class P_getFormattedName {

		@Test
		void a01_simple() {
			ClassInfo ci = ClassInfo.of(String.class);

			// SIMPLE format
			assertEquals("String",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
			assertEquals("String",
				ci.getNameFormatted(SIMPLE, false, '.', BRACKETS));
			assertEquals("String",
				ci.getNameFormatted(SIMPLE, true, '$', BRACKETS));
		}

		@Test
		void a02_short() {
			ClassInfo ci = ClassInfo.of(String.class);

			// SHORT format
			assertEquals("String",
				ci.getNameFormatted(SHORT, false, '$', BRACKETS));
			assertEquals("String",
				ci.getNameFormatted(SHORT, true, '$', BRACKETS));
		}

		@Test
		void a03_full() {
			ClassInfo ci = ClassInfo.of(String.class);

			// FULL format
			assertEquals("java.lang.String",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));
			assertEquals("java.lang.String",
				ci.getNameFormatted(FULL, true, '$', BRACKETS));
		}

		@Test
		void a04_innerClass() {
			ClassInfo ci = ClassInfo.of(Map.Entry.class);

			// SIMPLE - only innermost name
			assertEquals("Entry",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));

			// SHORT - outer class + inner class
			assertEquals("Map$Entry",
				ci.getNameFormatted(SHORT, false, '$', BRACKETS));
			assertEquals("Map.Entry",
				ci.getNameFormatted(SHORT, false, '.', BRACKETS));

			// FULL - package + outer + inner
			assertEquals("java.util.Map$Entry",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));
			assertEquals("java.util.Map.Entry",
				ci.getNameFormatted(FULL, false, '.', BRACKETS));
		}

		@Test
		void a05_arrays() {
			ClassInfo ci = ClassInfo.of(String[].class);

			// BRACKETS format
			assertEquals("String[]",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
			assertEquals("String[]",
				ci.getNameFormatted(SHORT, false, '$', BRACKETS));
			assertEquals("java.lang.String[]",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));

			// WORD format
			assertEquals("StringArray",
				ci.getNameFormatted(SIMPLE, false, '$', WORD));
			assertEquals("StringArray",
				ci.getNameFormatted(SHORT, false, '$', WORD));
			assertEquals("java.lang.StringArray",
				ci.getNameFormatted(FULL, false, '$', WORD));
		}

		@Test
		void a06_multiDimensionalArrays() {
			ClassInfo ci = ClassInfo.of(String[][].class);

			// BRACKETS format
			assertEquals("String[][]",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
			assertEquals("java.lang.String[][]",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));

			// WORD format
			assertEquals("StringArrayArray",
				ci.getNameFormatted(SIMPLE, false, '$', WORD));
			assertEquals("java.lang.StringArrayArray",
				ci.getNameFormatted(FULL, false, '$', WORD));
		}

		@Test
		void a07_primitiveArrays() {
			ClassInfo ci = ClassInfo.of(int[].class);

			// BRACKETS format
			assertEquals("int[]",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
			assertEquals("int[]",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));

			// WORD format
			assertEquals("intArray",
				ci.getNameFormatted(SIMPLE, false, '$', WORD));
			assertEquals("intArray",
				ci.getNameFormatted(FULL, false, '$', WORD));
		}

		@Test
		void a08_generics() throws Exception {
			// Get a ParameterizedType for HashMap<String, Integer>
			Field f = GenericsTestClass.class.getDeclaredField("hashMap");
			Type t = f.getGenericType();
			ClassInfo ci = ClassInfo.of(t);

			// Without type params
			assertEquals("HashMap",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
			assertEquals("java.util.HashMap",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));

			// With type params - SIMPLE
			assertEquals("HashMap<String,Integer>",
				ci.getNameFormatted(SIMPLE, true, '$', BRACKETS));

			// With type params - SHORT
			assertEquals("HashMap<String,Integer>",
				ci.getNameFormatted(SHORT, true, '$', BRACKETS));

			// With type params - FULL
			assertEquals("java.util.HashMap<java.lang.String,java.lang.Integer>",
				ci.getNameFormatted(FULL, true, '$', BRACKETS));
		}

		@Test
		void a09_nestedGenerics() throws Exception {
			// Get a ParameterizedType for Map<String, List<Integer>>
			Field f = GenericsTestClass.class.getDeclaredField("nestedMap");
			Type t = f.getGenericType();
			ClassInfo ci = ClassInfo.of(t);

			// With type params - SIMPLE
			assertEquals("HashMap<String,ArrayList<Integer>>",
				ci.getNameFormatted(SIMPLE, true, '$', BRACKETS));

			// With type params - FULL
			assertEquals("java.util.HashMap<java.lang.String,java.util.ArrayList<java.lang.Integer>>",
				ci.getNameFormatted(FULL, true, '$', BRACKETS));
		}

		@Test
		void a10_genericArrays() throws Exception {
			// Get a ParameterizedType for List<String>[]
			Field f = GenericsTestClass.class.getDeclaredField("listArray");
			Type t = f.getGenericType();
			ClassInfo ci = ClassInfo.of(t);

			// BRACKETS format
			assertEquals("ArrayList<String>[]",
				ci.getNameFormatted(SIMPLE, true, '$', BRACKETS));
			assertEquals("java.util.ArrayList<java.lang.String>[]",
				ci.getNameFormatted(FULL, true, '$', BRACKETS));

			// WORD format
			assertEquals("ArrayList<String>Array",
				ci.getNameFormatted(SIMPLE, true, '$', WORD));
		}

		@Test
		void a11_appendVersion() {
			ClassInfo ci = ClassInfo.of(String.class);
			StringBuilder sb = new StringBuilder("Type: ");
			ci.appendNameFormatted(sb, FULL, false, '$', BRACKETS);
			assertEquals("Type: java.lang.String", sb.toString());

			// Verify it returns the same StringBuilder for chaining
			StringBuilder result = ci.appendNameFormatted(sb, SIMPLE, false, '$', BRACKETS);
			assertSame(sb, result);
			assertEquals("Type: java.lang.StringString", sb.toString());
		}

		@Test
		void a12_equivalentMethods() {
			ClassInfo ci = ClassInfo.of(String.class);

			// getName() equivalent
			assertEquals(ci.getName(),
				ci.getNameFormatted(FULL, false, '$', BRACKETS));

			// getSimpleName() equivalent
			assertEquals(ci.getNameSimple(),
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
		}

		@Test
		void a13_innerClassArrays() {
			ClassInfo ci = ClassInfo.of(Map.Entry[].class);

			// SIMPLE
			assertEquals("Entry[]",
				ci.getNameFormatted(SIMPLE, false, '$', BRACKETS));
			assertEquals("EntryArray",
				ci.getNameFormatted(SIMPLE, false, '$', WORD));

			// SHORT
			assertEquals("Map$Entry[]",
				ci.getNameFormatted(SHORT, false, '$', BRACKETS));
			assertEquals("Map.Entry[]",
				ci.getNameFormatted(SHORT, false, '.', BRACKETS));
			assertEquals("Map$EntryArray",
				ci.getNameFormatted(SHORT, false, '$', WORD));

			// FULL
			assertEquals("java.util.Map$Entry[]",
				ci.getNameFormatted(FULL, false, '$', BRACKETS));
			assertEquals("java.util.Map.Entry[]",
				ci.getNameFormatted(FULL, false, '.', BRACKETS));
			assertEquals("java.util.Map$EntryArray",
				ci.getNameFormatted(FULL, false, '$', WORD));
		}

		// Helper class for testing generics
		@SuppressWarnings("unused")
		private static class GenericsTestClass {
			HashMap<String, Integer> hashMap;
			HashMap<String, ArrayList<Integer>> nestedMap;
			ArrayList<String>[] listArray;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getParentsAndInterfaces tests
	//-----------------------------------------------------------------------------------------------------------------

	// Test hierarchy that mimics RootResources structure:
	// - Child extends Parent implements IChild
	// - Parent extends GrandParent implements IParent
	// - GrandParent implements IGrandParent
	// - IParent extends ISuperParent
	// - IGrandParent extends ISuperGrandParent

	interface ISuperGrandParent {}
	interface IGrandParent extends ISuperGrandParent {}
	interface ISuperParent {}
	interface IParent extends ISuperParent {}
	interface IChild {}
	static class GrandParent implements IGrandParent {}
	static class Parent extends GrandParent implements IParent {}
	static class Child extends Parent implements IChild {}

	@Test
	void getParentsAndInterfaces_includesAllInterfaces() {
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
		
		var names = parentsAndInterfaces.stream()
			.map(ClassInfo::getNameSimple)
			.collect(Collectors.toList());
		
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

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void xToString() {
		assertEquals("class org.apache.juneau.common.reflect.AClass", aClass.toString());
		assertEquals("interface org.apache.juneau.common.reflect.AInterface", aInterface.toString());
		assertEquals("class org.apache.juneau.common.reflect.ClassInfo_Test$A1", aType.toString());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.String>>", pType.toString());
		assertEquals("java.util.Map<java.lang.String, java.lang.String[][]>", pTypeDimensional.toString());
		assertEquals("java.util.AbstractMap<K, V>", pTypeGeneric.toString());
		assertEquals("V", pTypeGenericArg.toString());
	}
}