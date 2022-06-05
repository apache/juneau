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
package org.apache.juneau.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.reflect.ClassInfo.*;
import static org.apache.juneau.reflect.ReflectFlags.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.Context.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.reflect.MethodInfoTest.*;
import org.apache.juneau.svl.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ClassInfoTest {

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
		public void apply(AnnotationInfo<AConfig> a, Context.Builder b) {
		}
	}

	private static void check(String expected, Object o) {
		if (o instanceof List) {
			List<?> l = (List<?>)o;
			String actual = l
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else if (o instanceof Iterable) {
			String actual = StreamSupport.stream(((Iterable<?>)o).spliterator(), false)
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
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
			if (t instanceof A)
				return "@A(" + ((A)t).value() + ")";
			if (t instanceof PA)
				return "@PA(" + ((PA)t).value() + ")";
			if (t instanceof AConfig)
				return "@AConfig(" + ((AConfig)t).value() + ")";
			if (t instanceof AnnotationInfo)
				return apply(((AnnotationInfo<?>)t).inner());
			if (t instanceof AnnotationList) {
				AnnotationList al = (AnnotationList)t;
				return al.toString();
			}
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Initialization
	//-----------------------------------------------------------------------------------------------------------------

	public class A1 {}
	public class A2 extends Value<A1>{};
	public class A3 extends Value<Map<String,List<String>>>{};
	public class A4 extends Value<Map<String,String[][]>>{};
	public static Type aType, pType, pTypeDimensional, pTypeGeneric, pTypeGenericArg;
	static {
		aType = ((ParameterizedType)A2.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pType = ((ParameterizedType)A3.class.getGenericSuperclass()).getActualTypeArguments()[0];
		pTypeDimensional = ((ParameterizedType)A4.class.getGenericSuperclass()).getActualTypeArguments()[0];
		Map<String,String> m = new HashMap<>();
		pTypeGeneric = m.getClass().getGenericSuperclass();
		pTypeGenericArg = ((ParameterizedType)pTypeGeneric).getActualTypeArguments()[1];
	}

	static ClassInfo aTypeInfo=of(aType), pTypeInfo=of(pType), pTypeDimensionalInfo=of(pTypeDimensional), pTypeGenericInfo=of(pTypeGeneric), pTypeGenericArgInfo=of(pTypeGenericArg);
	static ClassInfo aClass=of(AClass.class), aInterface=of(AInterface.class);

	@Test
	public void ofType() {
		check("A1", of(A1.class));
		check("A1", of(aType));
		check("Map", pTypeInfo);
		check("Map", pTypeDimensionalInfo);
		check("AbstractMap", pTypeGenericInfo);
		check("V", pTypeGenericArgInfo);
	}

	@Test
	public void ofTypeOnObject() {
		check("A1", of(new A1()));
	}

	@Test
	public void ofTypeOnNulls() {
		check(null, of((Class<?>)null));
		check(null, of((Type)null));
		check(null, of((Object)null));
	}

	@Test
	public void inner() {
		assertTrue(of(A1.class).inner() instanceof Class);
		assertTrue(of(A1.class).innerType() instanceof Class);
	}

	@Test
	public void resolved() {
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

	@Test
	public void resolvedParams() {
		MethodInfo mi = ClassInfo.of(A6.class).getPublicMethod(x -> x.hasName("m1"));
		check("A1", mi.getParamType(0).unwrap(Optional.class));
		check("A1", mi.getReturnType().unwrap(Optional.class));
		mi = ClassInfo.of(A6.class).getPublicMethod(x -> x.hasName("m2"));
		check("A1", mi.getParamType(0).unwrap(Value.class));
		check("A1", mi.getReturnType().unwrap(Value.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parent classes and interfaces.
	//-----------------------------------------------------------------------------------------------------------------

	static interface BI1 {}
	static interface BI2 extends BI1 {}
	static interface BI3 {}
	static interface BI4 {}
	static class BC1 implements BI1, BI2 {}
	static class BC2 extends BC1 implements BI3 {}
	static class BC3 extends BC2 {}

	static ClassInfo bi1=of(BI1.class), bi2=of(BI2.class), bi3=of(BI3.class), bi4=of(BI4.class), bc1=of(BC1.class), bc2=of(BC2.class), bc3=of(BC3.class), object=of(Object.class);

	@Test
	public void getDeclaredInterfaces() {
		check("", bi4.getDeclaredInterfaces());
		check("BI1,BI2", bc1.getDeclaredInterfaces());
		check("BI3", bc2.getDeclaredInterfaces());
		check("", bc3.getDeclaredInterfaces());
	}

	@Test
	public void getDeclaredInterfaces_onType() {
		check("", aTypeInfo.getDeclaredInterfaces());
		check("", pTypeInfo.getDeclaredInterfaces());
		check("", pTypeDimensionalInfo.getDeclaredInterfaces());
		check("Map", pTypeGenericInfo.getDeclaredInterfaces());
		check("", pTypeGenericArgInfo.getDeclaredInterfaces());
	}

	@Test
	public void getDeclaredInterfaces_twice() {
		check("BI1,BI2", bc1.getDeclaredInterfaces());
		check("BI1,BI2", bc1.getDeclaredInterfaces());
	}

	@Test
	public void getInterfaces() {
		check("", bi4.getInterfaces());
		check("BI1,BI2", bc1.getInterfaces());
		check("BI3,BI1,BI2", bc2.getInterfaces());
		check("BI3,BI1,BI2", bc3.getInterfaces());
	}

	@Test
	public void getInterfaces_tiwce() {
		check("BI3,BI1,BI2", bc2.getInterfaces());
		check("BI3,BI1,BI2", bc2.getInterfaces());
	}

	@Test
	public void getParents() {
		check("BC3,BC2,BC1", bc3.getParents());
		check("", object.getParents());
		check("BI1", bi1.getParents());
	}

	@Test
	public void getAllParents() {
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("", object.getAllParents());
		check("BI1", bi1.getAllParents());
	}

	@Test
	public void getAllParents_twice() {
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
	}

	@Test
	public void getParent() {
		check("BC2", bc3.getSuperclass());
		check("BC1", bc2.getSuperclass());
		check("Object", bc1.getSuperclass());
		check(null, object.getSuperclass());
		check(null, bi2.getSuperclass());
		check(null, bi1.getSuperclass());
	}

	@Test
	public void getParent_onType() {
		check("Object", aTypeInfo.getSuperclass());
		check(null, pTypeInfo.getSuperclass());
		check(null, pTypeDimensionalInfo.getSuperclass());
		check("Object", pTypeGenericInfo.getSuperclass());
		check(null, pTypeGenericArgInfo.getSuperclass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Methods
	//-----------------------------------------------------------------------------------------------------------------

	static interface CI1 {
		void i1a();
		void i1b();
	}
	static interface CI2 extends CI1 {
		void i2b();
		void i2a();
	}
	static interface CI3 {}
	static interface CI4 {}
	static abstract class CC1 implements CI1, CI2 {
		@Override
		public void i1a() {}
		protected void c1b() {}
		public void c1a() {}
	}
	static class CC2 extends CC1 implements CI3 {
		public void c2b() {}
		@Override
		public void i1b() {}
		@Override
		public void i2b() {}
		@Override
		public void i2a() {}
		protected void c2a() {}
	}
	static class CC3 extends CC2 {
		@Override
		public void i2b() {}
		public void c3a() {}
		protected void c3b() {}
	}
	static ClassInfo cc3 = of(CC3.class), ci2 = of(CI2.class);

	@Test
	public void getPublicMethods() throws Exception {
		check("CC3.c1a(),CC3.c2b(),CC3.c3a(),CC3.i1a(),CC3.i1b(),CC3.i2a(),CC3.i2b()", cc3.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
	}

	@Test
	public void getPublicMethods_twice() throws Exception {
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
	}

	@Test
	public void getPublicMethods_onType() throws Exception {
		check("", aTypeInfo.getPublicMethods());
		check("", pTypeGenericArgInfo.getPublicMethods());
	}

	@Test
	public void getAllMethods() throws Exception {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getMethods());
	}

	@Test
	public void getAllMethods_twice() throws Exception {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getMethods());
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getMethods());
	}

	@Test
	public void getDeclaredMethods() throws Exception {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
	}

	@Test
	public void getDeclaredMethods_twice() throws Exception {
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
	}

	@Test
	public void getDeclaredMethods_onType() throws Exception {
		check("", aTypeInfo.getDeclaredMethods());
		check("", pTypeGenericArgInfo.getDeclaredMethods());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	static class E1 {
		public E1() {}
		public E1(String a) {}
		public E1(Writer a) {}
		public E1(String a, Writer b) {}
		protected E1(int a) {}
		E1(float a) {}
	}
	static class E2 {
		protected E2() {}
	}
	static abstract class E3 {
		public E3() {}
	}
	class E4 {
		public E4() {}
	}
	static class E5 {
		@Deprecated
		public E5() {}
	}
	class E6 {
		public E6(String a) {}
	}
	static ClassInfo e1=of(E1.class), e2=of(E2.class), e3=of(E3.class), e4=of(E4.class), e5=of(E5.class), e6=of(E6.class);

	@Test
	public void getPublicConstructors() {
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
	}

	@Test
	public void getPublicConstructors_twice() {
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
	}

	@Test
	public void getPublicConstructors_onType() {
		check("A1(ClassInfoTest)", aTypeInfo.getPublicConstructors());
		check("", pTypeInfo.getPublicConstructors());
		check("", pTypeDimensionalInfo.getPublicConstructors());
		check("", pTypeGenericInfo.getPublicConstructors());
		check("", pTypeGenericArgInfo.getPublicConstructors());
	}

	@Test
	public void getDeclaredConstructors() {
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
	}

	@Test
	public void getDeclaredConstructors_twice() {
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
		check("E1(),E1(float),E1(int),E1(Writer),E1(String),E1(String,Writer)", e1.getDeclaredConstructors());
	}

	@Test
	public void getDeclaredConstructors_onType() {
		check("A1(ClassInfoTest)", aTypeInfo.getDeclaredConstructors());
		check("", pTypeInfo.getDeclaredConstructors());
		check("", pTypeDimensionalInfo.getDeclaredConstructors());
		check("AbstractMap()", pTypeGenericInfo.getDeclaredConstructors());
		check("", pTypeGenericArgInfo.getDeclaredConstructors());
	}

	@Test
	public void getPublicConstructor_classArgs() {
		check("E1(String)", e1.getPublicConstructor(x -> x.hasParamTypes(String.class)));
	}

	@Test
	public void getPublicConstructor_objectArgs() {
		check("E1(String)", e1.getPublicConstructor(x -> x.canAccept("foo")));
	}

	@Test
	public void getNoArgConstructor() {
		check("E2()", e2.getNoArgConstructor(Visibility.PRIVATE));
		check("E2()", e2.getNoArgConstructor(Visibility.PROTECTED));
		check("E2()", e2.getNoArgConstructor(Visibility.DEFAULT));
		check(null, e2.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructor_abstractClass() {
		check(null, e3.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructor_innerClass() {
		check("E4(ClassInfoTest)", e4.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructor_noConstructor() {
		check(null, e6.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getPublicNoArgConstructor() {
		check("E1()", e1.getPublicConstructor(x -> x.hasNoParams()));
	}

	@Test
	public void getConstructor() {
		check("E1(int)", e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PROTECTED) && x.hasParamTypes(int.class)));
		check("E1(int)", e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PRIVATE) && x.hasParamTypes(int.class)));
		check(null, e1.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC) && x.hasParamTypes(int.class)));
		check("E3()", e3.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)));
		check("E4(ClassInfoTest)", e4.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)));
		check("E5()", e5.getDeclaredConstructor(x -> x.isVisible(Visibility.PUBLIC)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fields
	//-----------------------------------------------------------------------------------------------------------------

	static abstract class F1 {
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

	@Test
	public void getPublicFields() {
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
	}

	@Test
	public void getPublicFields_twice() {
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFields());
	}

	@Test
	public void getPublicFields_onType() {
		check("", aTypeInfo.getPublicFields());
	}

	@Test
	public void getDeclaredFields() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
	}

	@Test
	public void getDeclaredFields_twice() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFields());
	}

	@Test
	public void getDeclaredFields_onType() {
		check("A1.this$0", aTypeInfo.getDeclaredFields());
		check("", pTypeInfo.getDeclaredFields());
		check("", pTypeDimensionalInfo.getDeclaredFields());
		check("AbstractMap.keySet,AbstractMap.values", pTypeGenericInfo.getDeclaredFields());
		check("", pTypeGenericArgInfo.getDeclaredFields());
	}

	@Test
	public void getAllFieldsParentFirst() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
	}

	@Test
	public void getAllFieldsParentFirst_twice() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFields());
	}

	static class F3 {
		public int a1;
		int a2;
	}
	static ClassInfo f3=of(F3.class);

	@Test
	public void getPublicField() {
		check("F3.a1", f3.getPublicField(x -> x.hasName("a1")));
		check(null, f3.getPublicField(x -> x.hasName("a2")));
		check(null, f3.getPublicField(x -> x.hasName("a3")));
	}

	@Test
	public void getDeclaredField() {
		check("F3.a1", f3.getDeclaredField(x -> x.hasName("a1")));
		check("F3.a2", f3.getDeclaredField(x -> x.hasName("a2")));
		check(null, f3.getDeclaredField(x -> x.hasName("a3")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@A(1) static interface GI1 {}
	@A(2) static interface GI2 extends GI1 {}
	@A(3) static interface GI3 {}
	@A(4) static interface GI4 {}
	@A(5) static class G1 implements GI1, GI2 {}
	@A(6) static class G2 extends G1 implements GI3 {}
	@A(7) static class G3 extends G2 {}
	static class G4 extends G3 {}
	static class G5 implements GI3 {}

	static ClassInfo g3=of(G3.class), g4=of(G4.class), g5=of(G5.class);

	@Test
	public void getAnnotation() {
		check("@A(7)", g3.getAnnotation(A.class));
		check(null, g3.getAnnotation(B.class));
		check(null, g3.getAnnotation(null));
	}

	@Test
	public void getAnnotation_twice() {
		check("@A(7)", g3.getAnnotation(A.class));
		check("@A(7)", g3.getAnnotation(A.class));
	}

	@Test
	public void getAnnotation_onParent() {
		check("@A(7)", g4.getAnnotation(A.class));
		check(null, g4.getAnnotation(B.class));
		check(null, g4.getAnnotation(null));
	}

	@Test
	public void getAnnotation_onInterface() {
		check("@A(3)", g5.getAnnotation(A.class));
		check(null, g5.getAnnotation(B.class));
		check(null, g5.getAnnotation(null));
	}

	@Test
	public void hasAnnotation() {
		assertTrue(g3.hasAnnotation(A.class));
		assertFalse(g3.hasAnnotation(B.class));
		assertFalse(g3.hasAnnotation(null));
	}

	@Test
	public void getAnnotations() {
		check("@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g3.getAnnotations(A.class));
		check("@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g4.getAnnotations(A.class));
		check("@A(3)", g5.getAnnotations(A.class));
	}

	@Test
	public void forEachAnnotation() {
		List<Integer> l1 = list();
		g3.forEachAnnotation(A.class, null, x -> l1.add(x.value()));
		assertList(l1).asCdl().isString("2,1,3,5,6,7");

		List<Integer> l2 = list();
		g4.forEachAnnotation(A.class, null, x -> l2.add(x.value()));
		assertList(l2).asCdl().isString("2,1,3,5,6,7");

		List<Integer> l3 = list();
		g5.forEachAnnotation(A.class, null, x -> l3.add(x.value()));
		assertList(l3).asCdl().isString("3");

		List<Integer> l4 = list();
		g3.forEachAnnotation(A.class, x -> x.value() == 5, x -> l4.add(x.value()));
		assertList(l4).asCdl().isString("5");
	}

	@Test
	public void firstAnnotation() {
		assertInteger(g3.firstAnnotation(A.class, null).value()).is(2);
		assertInteger(g4.firstAnnotation(A.class, null).value()).is(2);
		assertInteger(g5.firstAnnotation(A.class, null).value()).is(3);
		assertInteger(g3.firstAnnotation(A.class, x -> x.value() == 5).value()).is(5);
	}
	@Test
	public void lastAnnotation() {
		assertInteger(g3.lastAnnotation(A.class, null).value()).is(7);
		assertInteger(g4.lastAnnotation(A.class, null).value()).is(7);
		assertInteger(g5.lastAnnotation(A.class, null).value()).is(3);
		assertInteger(g3.lastAnnotation(A.class, x -> x.value() == 5).value()).is(5);
	}

	@Test
	public void getPackageAnnotation() {
		check("@PA(10)", g3.getPackageAnnotation(PA.class));
	}

	@Test
	public void getPackageAnnotation_onType() {
		check("@PA(10)", aTypeInfo.getPackageAnnotation(PA.class));
		check(null, pTypeInfo.getPackageAnnotation(PA.class));
		check(null, pTypeDimensionalInfo.getPackageAnnotation(PA.class));
		check(null, pTypeGenericInfo.getPackageAnnotation(PA.class));
		check(null, pTypeGenericArgInfo.getPackageAnnotation(PA.class));
	}

	@Test
	public void getAnnotationsMapParentFirst() {
		check("@PA(10),@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g3.getAnnotationList());
		check("@PA(10),@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g4.getAnnotationList());
		check("@PA(10),@A(3)", g5.getAnnotationList());
	}

	@A(1) @AConfig(1) static interface GBI1 {}
	@A(2) @AConfig(2) static interface GBI2 extends GBI1 {}
	@A(3) @AConfig(3) static interface GBI3 {}
	@A(4) @AConfig(4) static interface GBI4 {}
	@A(5) @AConfig(5) static class GB1 implements GBI1, GBI2 {}
	@A(6) @AConfig(6) static class GB2 extends GB1 implements GBI3 {}
	@A(7) @AConfig(7) static class GB3 extends GB2 {}
	static class GB4 extends GB3 {}
	static class GB5 implements GBI3 {}

	static ClassInfo gb3=of(GB3.class), gb4=of(GB4.class), gb5=of(GB5.class);

	@Test
	public void getConfigAnnotationsMapParentFirst() {
		check("@AConfig(2),@AConfig(1),@AConfig(3),@AConfig(5),@AConfig(6),@AConfig(7)", gb3.getAnnotationList(CONTEXT_APPLY_FILTER));
		check("@AConfig(2),@AConfig(1),@AConfig(3),@AConfig(5),@AConfig(6),@AConfig(7)", gb4.getAnnotationList(CONTEXT_APPLY_FILTER));
		check("@AConfig(3)", gb5.getAnnotationList(CONTEXT_APPLY_FILTER));
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

	static ClassInfo hPublic=of(H_Public.class), hPackage=of(H_Package.class), hProtected=of(H_Protected.class), hPrivate=of(H_Private.class), hPublicMember=of(H_PublicMember.class), hAbstractPublic=of(H_AbstractPublic.class), hPublicDeprecated=of(H_PublicDeprecated.class);

	@Test
	public void isDeprecated() {
		assertFalse(hPublic.isDeprecated());
		assertTrue(hPublicDeprecated.isDeprecated());
	}

	@Test
	public void isDeprecated_onType() {
		assertFalse(aTypeInfo.isDeprecated());
		assertFalse(pTypeGenericArgInfo.isDeprecated());
	}

	@Test
	public void isNotDeprecated() {
		assertTrue(hPublic.isNotDeprecated());
		assertFalse(hPublicDeprecated.isNotDeprecated());
	}

	@Test
	public void isNotDeprecated_onType() {
		assertTrue(aTypeInfo.isNotDeprecated());
		assertTrue(pTypeInfo.isNotDeprecated());
		assertTrue(pTypeDimensionalInfo.isNotDeprecated());
		assertTrue(pTypeGenericInfo.isNotDeprecated());
		assertTrue(pTypeGenericArgInfo.isNotDeprecated());
	}

	@Test
	public void isPublic() {
		assertTrue(hPublic.isPublic());
		assertFalse(hProtected.isPublic());
		assertFalse(hPackage.isPublic());
		assertFalse(hPrivate.isPublic());
	}

	@Test
	public void isPublic_onType() {
		assertTrue(aTypeInfo.isPublic());
		assertTrue(pTypeInfo.isPublic());
		assertTrue(pTypeDimensionalInfo.isPublic());
		assertTrue(pTypeGenericInfo.isPublic());
		assertFalse(pTypeGenericArgInfo.isPublic());
	}

	@Test
	public void isNotPublic() {
		assertFalse(hPublic.isNotPublic());
		assertTrue(hProtected.isNotPublic());
		assertTrue(hPackage.isNotPublic());
		assertTrue(hPrivate.isNotPublic());
	}

	@Test
	public void isNotPublic_onType() {
		assertFalse(aTypeInfo.isNotPublic());
		assertTrue(pTypeGenericArgInfo.isNotPublic());
	}

	@Test
	public void isStatic() {
		assertTrue(hPublic.isStatic());
		assertFalse(hPublicMember.isStatic());
	}

	@Test
	public void isStatic_onType() {
		assertFalse(aTypeInfo.isStatic());
		assertFalse(pTypeGenericArgInfo.isStatic());
	}

	@Test
	public void isNotStatic() {
		assertFalse(hPublic.isNotStatic());
		assertTrue(hPublicMember.isNotStatic());
	}

	@Test
	public void isNotStatic_onType() {
		assertTrue(aTypeInfo.isNotStatic());
		assertTrue(pTypeInfo.isNotStatic());
		assertTrue(pTypeDimensionalInfo.isNotStatic());
		assertTrue(pTypeGenericInfo.isNotStatic());
		assertTrue(pTypeGenericArgInfo.isNotStatic());
	}

	@Test
	public void isAbstract() {
		assertTrue(hAbstractPublic.isAbstract());
		assertFalse(pTypeGenericArgInfo.isAbstract());
	}

	@Test
	public void isAbstract_onType() {
		assertFalse(aTypeInfo.isAbstract());
		assertFalse(aTypeInfo.isAbstract());
	}

	@Test
	public void isNotAbstract() {
		assertFalse(hAbstractPublic.isNotAbstract());
		assertTrue(hPublic.isNotAbstract());
	}

	@Test
	public void isNotAbstract_onType() {
		assertTrue(aTypeInfo.isNotAbstract());
		assertFalse(pTypeInfo.isNotAbstract());
		assertFalse(pTypeDimensionalInfo.isNotAbstract());
		assertFalse(pTypeGenericInfo.isNotAbstract());
		assertTrue(pTypeGenericArgInfo.isNotAbstract());
	}

	@Test
	public void isMemberClass() {
		assertTrue(hPublic.isMemberClass());
		assertTrue(hPublicMember.isMemberClass());
		assertFalse(aClass.isMemberClass());
		assertFalse(aInterface.isMemberClass());
	}

	@Test
	public void isMemberClass_onType() {
		assertTrue(aTypeInfo.isMemberClass());
		assertFalse(pTypeInfo.isMemberClass());
		assertFalse(pTypeDimensionalInfo.isMemberClass());
		assertFalse(pTypeGenericInfo.isMemberClass());
		assertFalse(pTypeGenericArgInfo.isMemberClass());
	}

	@Test
	public void isNotMemberClass() {
		assertFalse(hPublic.isNotMemberClass());
		assertFalse(hPublicMember.isNotMemberClass());
		assertTrue(aClass.isNotMemberClass());
		assertTrue(aInterface.isNotMemberClass());
	}

	@Test
	public void isNotMemberClass_onType() {
		assertFalse(aTypeInfo.isNotMemberClass());
		assertTrue(pTypeGenericArgInfo.isNotMemberClass());
	}

	@Test
	public void isNonStaticMemberClass() {
		assertFalse(hPublic.isNonStaticMemberClass());
		assertTrue(hPublicMember.isNonStaticMemberClass());
		assertFalse(aClass.isNonStaticMemberClass());
		assertFalse(aInterface.isNonStaticMemberClass());
	}

	@Test
	public void isNonStaticMemberClass_onType() {
		assertTrue(aTypeInfo.isNonStaticMemberClass());
		assertFalse(pTypeInfo.isNonStaticMemberClass());
		assertFalse(pTypeDimensionalInfo.isNonStaticMemberClass());
		assertFalse(pTypeGenericInfo.isNonStaticMemberClass());
		assertFalse(pTypeGenericArgInfo.isNonStaticMemberClass());
	}

	@Test
	public void isLocalClass() {
		class F implements Function<Object,String>{
			@Override
			public String apply(Object t) {
				return null;
			}
		}
		assertFalse(aClass.isLocalClass());
		assertTrue(of(F.class).isLocalClass());
	}

	@Test
	public void isLocalClass_type() {
		assertFalse(aTypeInfo.isLocalClass());
		assertFalse(pTypeGenericArgInfo.isLocalClass());
	}

	@Test
	public void isNotLocalClass() {
		class F implements Function<Object,String>{
			@Override
			public String apply(Object t) {
				return null;
			}
		}
		assertTrue(aClass.isNotLocalClass());
		assertFalse(of(F.class).isNotLocalClass());
	}

	@Test
	public void isNotLocalClass_type() {
		assertTrue(aTypeInfo.isNotLocalClass());
		assertTrue(pTypeGenericArgInfo.isNotLocalClass());
	}

	@Test
	public void isVisible_public() {
		assertTrue(hPublic.isVisible(Visibility.PUBLIC));
		assertFalse(hProtected.isVisible(Visibility.PUBLIC));
		assertFalse(hPackage.isVisible(Visibility.PUBLIC));
		assertFalse(hPrivate.isVisible(Visibility.PUBLIC));
	}


	@Test
	public void isVisible_protected() {
		assertTrue(hPublic.isVisible(Visibility.PROTECTED));
		assertTrue(hProtected.isVisible(Visibility.PROTECTED));
		assertFalse(hPackage.isVisible(Visibility.PROTECTED));
		assertFalse(hPrivate.isVisible(Visibility.PROTECTED));
	}

	@Test
	public void isVisible_package() {
		assertTrue(hPublic.isVisible(Visibility.DEFAULT));
		assertTrue(hProtected.isVisible(Visibility.DEFAULT));
		assertTrue(hPackage.isVisible(Visibility.DEFAULT));
		assertFalse(hPrivate.isVisible(Visibility.DEFAULT));
	}

	@Test
	public void isVisible_private() {
		assertTrue(hPublic.isVisible(Visibility.PRIVATE));
		assertTrue(hProtected.isVisible(Visibility.PRIVATE));
		assertTrue(hPackage.isVisible(Visibility.PRIVATE));
		assertTrue(hPrivate.isVisible(Visibility.PRIVATE));
	}

	@Test
	public void isVisible_onType() {
		assertTrue(aTypeInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeDimensionalInfo.isVisible(Visibility.PRIVATE));
		assertTrue(pTypeGenericInfo.isVisible(Visibility.PRIVATE));
		assertFalse(pTypeGenericArgInfo.isVisible(Visibility.PRIVATE));
	}

	@Test
	public void isPrimitive() {
		assertTrue(of(int.class).isPrimitive());
		assertFalse(of(Integer.class).isPrimitive());
	}

	@Test
	public void isPrimitive_onType() {
		assertFalse(aTypeInfo.isPrimitive());
		assertFalse(pTypeGenericArgInfo.isPrimitive());
	}

	@Test
	public void isNotPrimitive() {
		assertFalse(of(int.class).isNotPrimitive());
		assertTrue(of(Integer.class).isNotPrimitive());
	}

	@Test
	public void isNotPrimitive_onType() {
		assertTrue(aTypeInfo.isNotPrimitive());
		assertTrue(pTypeInfo.isNotPrimitive());
		assertTrue(pTypeDimensionalInfo.isNotPrimitive());
		assertTrue(pTypeGenericInfo.isNotPrimitive());
		assertTrue(pTypeGenericArgInfo.isNotPrimitive());
	}

	@Test
	public void isInterface() {
		assertTrue(aInterface.isInterface());
		assertFalse(aClass.isInterface());
	}

	@Test
	public void isInterface_onType() {
		assertFalse(aTypeInfo.isInterface());
		assertFalse(pTypeGenericArgInfo.isInterface());
	}

	@Test
	public void isClass() {
		assertTrue(aClass.isClass());
		assertFalse(aInterface.isClass());
	}

	@Test
	public void isClass_onType() {
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

	static ClassInfo h2a=of(H2a.class), h2b=of(H2b.class), h2Deprecated=of(H2_Deprecated.class), h2NotDeprecated=of(H2_NotDeprecated.class), h2Public=of(H2_Public.class), h2NotPublic=of(H2_NotPublic.class), h2Static=of(H2_Static.class), h2NotStatic=of(H2_NotStatic.class), h2Member=of(H2_Member.class), h2StaticMember=of(H2_StaticMember.class), h2Abstract=of(H2_Abstract.class), h2NotAbstract=of(H2_NotAbstract.class);

	@Test
	public void isAll() {
		assertTrue(h2a.isAll(DEPRECATED, PUBLIC, STATIC, MEMBER, ABSTRACT, ReflectFlags.CLASS));
		assertTrue(h2b.isAll(NOT_DEPRECATED, NOT_PUBLIC, STATIC, ABSTRACT, INTERFACE));
	}

	@Test
	public void isAll_onType() {
		assertTrue(aTypeInfo.isAll(PUBLIC, MEMBER, ReflectFlags.CLASS));
		assertFalse(pTypeInfo.isAll(PUBLIC, MEMBER, ReflectFlags.CLASS));
		assertFalse(pTypeDimensionalInfo.isAll(PUBLIC, MEMBER, ReflectFlags.CLASS));
		assertFalse(pTypeGenericInfo.isAll(PUBLIC, MEMBER, ReflectFlags.CLASS));
	}

	@Test
	public void isAll_deprecated() {
		assertTrue(h2Deprecated.isAll(DEPRECATED));
		assertFalse(h2NotDeprecated.isAll(DEPRECATED));
	}

	@Test
	public void isAll_notDeprecated() {
		assertFalse(h2Deprecated.isAll(NOT_DEPRECATED));
		assertTrue(h2NotDeprecated.isAll(NOT_DEPRECATED));
	}

	@Test
	public void isAll_public() {
		assertTrue(of(H2_Public.class).isAll(PUBLIC));
		assertFalse(h2NotPublic.isAll(PUBLIC));
	}

	@Test
	public void isAll_notPublic() {
		assertFalse(of(H2_Public.class).isAll(NOT_PUBLIC));
		assertTrue(h2NotPublic.isAll(NOT_PUBLIC));
	}

	@Test
	public void isAll_static() {
		assertTrue(of(H2_Static.class).isAll(STATIC));
		assertFalse(h2NotStatic.isAll(STATIC));
	}

	@Test
	public void isAll_notStatic() {
		assertFalse(of(H2_Static.class).isAll(NOT_STATIC));
		assertTrue(h2NotStatic.isAll(NOT_STATIC));
	}

	@Test
	public void isAll_member() {
		assertTrue(h2Member.isAll(MEMBER));
		assertTrue(h2StaticMember.isAll(MEMBER));
		assertFalse(aClass.isAll(MEMBER));
	}

	@Test
	public void isAll_notMember() {
		assertFalse(h2Member.isAll(NOT_MEMBER));
		assertFalse(h2StaticMember.isAll(NOT_MEMBER));
		assertTrue(aClass.isAll(NOT_MEMBER));
	}

	@Test
	public void isAll_abstract() {
		assertTrue(of(H2_Abstract.class).isAll(ABSTRACT));
		assertFalse(h2NotAbstract.isAll(ABSTRACT));
		assertTrue(aInterface.isAll(ABSTRACT));
	}

	@Test
	public void isAll_notAbstract() {
		assertFalse(of(H2_Abstract.class).isAll(NOT_ABSTRACT));
		assertTrue(h2NotAbstract.isAll(NOT_ABSTRACT));
		assertFalse(aInterface.isAll(NOT_ABSTRACT));
	}

	@Test
	public void isAll_interface() {
		assertTrue(aInterface.isAll(INTERFACE));
		assertFalse(aClass.isAll(INTERFACE));
	}

	@Test
	public void isAll_class() {
		assertFalse(aInterface.isAll(ReflectFlags.CLASS));
		assertTrue(aClass.isAll(ReflectFlags.CLASS));
	}

	@Test
	public void isAll_invalid() {
		ClassInfo a = aClass;
		assertThrown(()->a.isAll(HAS_PARAMS)).isExists();
		assertThrown(()->a.isAll(HAS_NO_PARAMS)).isExists();
		assertThrown(()->a.isAll(TRANSIENT)).isExists();
		assertThrown(()->a.isAll(NOT_TRANSIENT)).isExists();
	}

	@Test
	public void isAny() {
		assertTrue(h2a.isAny(DEPRECATED));
		assertTrue(h2a.isAny(PUBLIC));
		assertTrue(h2a.isAny(STATIC));
		assertTrue(h2a.isAny(MEMBER));
		assertTrue(h2a.isAny(ABSTRACT));
		assertTrue(h2a.isAny(ReflectFlags.CLASS));
		assertTrue(h2b.isAny(NOT_DEPRECATED));
		assertTrue(h2b.isAny(NOT_PUBLIC));
		assertTrue(h2b.isAny(STATIC));
		assertTrue(h2b.isAny(ABSTRACT));
		assertTrue(h2b.isAny(INTERFACE));
	}

	@Test
	public void isAny_onType() {
		assertFalse(aTypeInfo.isAny(new ReflectFlags[0]));
	}

	@Test
	public void isAny_deprecated() {
		assertTrue(h2Deprecated.isAny(DEPRECATED));
		assertFalse(h2NotDeprecated.isAny(DEPRECATED));
	}

	@Test
	public void isAny_notDeprecated() {
		assertFalse(h2Deprecated.isAny(NOT_DEPRECATED));
		assertTrue(h2NotDeprecated.isAny(NOT_DEPRECATED));
	}

	@Test
	public void isAny_public() {
		assertTrue(h2Public.isAny(PUBLIC));
		assertFalse(h2NotPublic.isAny(PUBLIC));
	}

	@Test
	public void isAny_notPublic() {
		assertFalse(h2Public.isAny(NOT_PUBLIC));
		assertTrue(h2NotPublic.isAny(NOT_PUBLIC));
	}

	@Test
	public void isAny_static() {
		assertTrue(h2Static.isAny(STATIC));
		assertFalse(h2NotStatic.isAny(STATIC));
	}

	@Test
	public void isAny_notStatic() {
		assertFalse(h2Static.isAny(NOT_STATIC));
		assertTrue(h2NotStatic.isAny(NOT_STATIC));
	}

	@Test
	public void isAny_member() {
		assertTrue(h2Member.isAny(MEMBER));
		assertTrue(h2StaticMember.isAny(MEMBER));
		assertFalse(aClass.isAny(MEMBER));
	}

	@Test
	public void isAny_notMember() {
		assertFalse(h2Member.isAny(NOT_MEMBER));
		assertFalse(h2StaticMember.isAny(NOT_MEMBER));
		assertTrue(aClass.isAny(NOT_MEMBER));
	}

	@Test
	public void isAny_abstract() {
		assertTrue(h2Abstract.isAny(ABSTRACT));
		assertFalse(h2NotAbstract.isAny(ABSTRACT));
		assertTrue(aInterface.isAny(ABSTRACT));
	}

	@Test
	public void isAny_notAbstract() {
		assertFalse(h2Abstract.isAny(NOT_ABSTRACT));
		assertTrue(h2NotAbstract.isAny(NOT_ABSTRACT));
		assertFalse(aInterface.isAny(NOT_ABSTRACT));
	}

	@Test
	public void isAny_interface() {
		assertTrue(aInterface.isAny(INTERFACE));
		assertFalse(aClass.isAny(INTERFACE));
	}

	@Test
	public void isAny_class() {
		assertFalse(aInterface.isAny(ReflectFlags.CLASS));
		assertTrue(aClass.isAny(ReflectFlags.CLASS));
	}

	@Test
	public void isAny_invalid() {
		ClassInfo a = aClass;
		assertThrown(()->a.isAny(HAS_PARAMS)).isExists();
		assertThrown(()->a.isAny(HAS_NO_PARAMS)).isExists();
		assertThrown(()->a.isAny(TRANSIENT)).isExists();
		assertThrown(()->a.isAny(NOT_TRANSIENT)).isExists();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive wrappers
	//-----------------------------------------------------------------------------------------------------------------

	static List<Class<?>> primitives = list(boolean.class,byte.class,short.class,char.class,int.class,long.class,float.class,double.class);
	static List<Class<?>> primitiveWrappers = list(Boolean.class,Byte.class,Short.class,Character.class,Integer.class,Long.class,Float.class,Double.class);
	static List<Object> primitiveDefaults = list(false,(byte)0,(short)0,(char)0,0,0l,0f,0d);

	@Test
	public void hasPrimitiveWrapper() {
		for (Class<?> c : primitives)
			assertTrue(of(c).hasPrimitiveWrapper());
		for (Class<?> c : primitiveWrappers)
			assertFalse(of(c).hasPrimitiveWrapper());
	}

	@Test
	public void hasPrimitiveWrapper_onType() {
		assertFalse(aTypeInfo.hasPrimitiveWrapper());
	}

	@Test
	public void getPrimitiveWrapper() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getPrimitiveWrapper(), primitiveWrappers.get(i));
		assertNull(of(String.class).getPrimitiveWrapper());
	}

	@Test
	public void getPrimitiveWrapper_onType() {
		assertNull(aTypeInfo.getPrimitiveWrapper());
	}

	@Test
	public void getPrimitiveForWrapper() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitiveWrappers.get(i)).getPrimitiveForWrapper(), primitives.get(i));
		assertNull(of(String.class).getPrimitiveForWrapper());
	}

	@Test
	public void getPrimitiveForWrapper_onType() {
		assertNull(aTypeInfo.getPrimitiveForWrapper());
	}

	@Test
	public void getWrapperIfPrimitive() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getWrapperIfPrimitive(), primitiveWrappers.get(i));
		assertEquals(of(String.class).getWrapperIfPrimitive(), String.class);
	}

	@Test
	public void getWrapperIfPrimitive_onType() {
		assertEquals("class org.apache.juneau.reflect.ClassInfoTest$A1", aTypeInfo.getWrapperIfPrimitive().toString());
		assertEquals("interface java.util.Map", pTypeInfo.getWrapperIfPrimitive().toString());
		assertEquals("interface java.util.Map", pTypeDimensionalInfo.getWrapperIfPrimitive().toString());
		assertEquals("class java.util.AbstractMap", pTypeGenericInfo.getWrapperIfPrimitive().toString());
		assertEquals(null, pTypeGenericArgInfo.getWrapperIfPrimitive());
	}

	@Test
	public void getWrapperInfoIfPrimitive() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getWrapperInfoIfPrimitive().inner(), primitiveWrappers.get(i));
		assertEquals(of(String.class).getWrapperInfoIfPrimitive().inner(), String.class);
	}

	@Test
	public void getWrapperInfoIfPrimitive_onType() {
		assertEquals(aTypeInfo.getWrapperInfoIfPrimitive().innerType(), aType);
		check("V", pTypeGenericArgInfo.getWrapperInfoIfPrimitive());
	}

	@Test
	public void getPrimitiveDefault() {
		for (int i = 0; i < primitives.size(); i++)
			assertEquals(of(primitives.get(i)).getPrimitiveDefault(), primitiveDefaults.get(i));
		assertNull(of(String.class).getPrimitiveDefault());
	}

	@Test
	public void getPrimitiveDefault_onType() {
		assertNull(aTypeInfo.getPrimitiveDefault());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	public class J1 {}
	public static class J2 {}

	static ClassInfo j1=of(J1.class), j2=of(J2.class), j1_3d=of(J1[][].class), j2_3d=of(J2[][].class);

	@Test
	public void getFullName_simple() {
		assertEquals("org.apache.juneau.reflect.AClass", aClass.getFullName());
	}

	@Test
	public void getFullName_simpleTwice() {
		assertEquals("org.apache.juneau.reflect.AClass", aClass.getFullName());
		assertEquals("org.apache.juneau.reflect.AClass", aClass.getFullName());
	}

	@Test
	public void getFullName_simpleArray() {
		assertEquals("org.apache.juneau.reflect.AClass[][]", of(AClass[][].class).getFullName());
	}

	@Test
	public void getFullName_inner() {
		assertEquals("org.apache.juneau.reflect.ClassInfoTest$J1", j1.getFullName());
		assertEquals("org.apache.juneau.reflect.ClassInfoTest$J2", j2.getFullName());
	}

	@Test
	public void getFullName_innerArray() {
		assertEquals("org.apache.juneau.reflect.ClassInfoTest$J1[][]", j1_3d.getFullName());
		assertEquals("org.apache.juneau.reflect.ClassInfoTest$J2[][]", j2_3d.getFullName());
	}

	@Test
	public void getFullName_primitive() {
		assertEquals("int", of(int.class).getFullName());
	}

	@Test
	public void getFullName_primitiveArray() {
		assertEquals("int[][]", of(int[][].class).getFullName());
	}

	@Test
	public void getFullName_simpleType() {
		assertEquals("org.apache.juneau.reflect.ClassInfoTest$A1", aTypeInfo.getFullName());
	}

	@Test
	public void getFullName_complexType() {
		assertEquals("java.util.Map<java.lang.String,java.util.List<java.lang.String>>", pTypeInfo.getFullName());
	}

	@Test
	public void getFullName_dimensionalType() {
		assertEquals("java.util.Map<java.lang.String,java.lang.String[][]>", pTypeDimensionalInfo.getFullName());
	}

	@Test
	public void getFullName_genericType() {
		assertEquals("java.util.AbstractMap<K,V>", pTypeGenericInfo.getFullName());
	}

	@Test
	public void getFullName_genericTypeArg() {
		assertEquals("V", pTypeGenericArgInfo.getFullName());
	}

	@Test
	public void getFullName_localClass() {
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {};
		assertEquals("org.apache.juneau.reflect.ClassInfoTest$1LocalClass", of(LocalClass.class).getFullName());
	}

	@Test
	public void getShortName_simple() {
		assertEquals("AClass", aClass.getShortName());
	}

	@Test
	public void getShortName_simpleTwice() {
		assertEquals("AClass", aClass.getShortName());
		assertEquals("AClass", aClass.getShortName());
	}

	@Test
	public void getShortName_simpleArray() {
		assertEquals("AClass[][]", of(AClass[][].class).getShortName());
	}

	@Test
	public void getShortName_inner() {
		assertEquals("ClassInfoTest$J1", j1.getShortName());
		assertEquals("ClassInfoTest$J2", j2.getShortName());
	}

	@Test
	public void getShortName_innerArray() {
		assertEquals("ClassInfoTest$J1[][]", j1_3d.getShortName());
		assertEquals("ClassInfoTest$J2[][]", j2_3d.getShortName());
	}

	@Test
	public void getShortName_primitive() {
		assertEquals("int", of(int.class).getShortName());
	}

	@Test
	public void getShortName_primitiveArray() {
		assertEquals("int[][]", of(int[][].class).getShortName());
	}

	@Test
	public void getShortName_simpleType() {
		assertEquals("ClassInfoTest$A1", aTypeInfo.getShortName());
	}

	@Test
	public void getShortName_complexType() {
		assertEquals("Map<String,List<String>>", pTypeInfo.getShortName());
	}

	@Test
	public void getShortName_dimensionalType() {
		assertEquals("Map<String,String[][]>", pTypeDimensionalInfo.getShortName());
	}

	@Test
	public void getShortName_genericType() {
		assertEquals("AbstractMap<K,V>", pTypeGenericInfo.getShortName());
	}

	@Test
	public void getShortName_genericTypeArg() {
		assertEquals("V", pTypeGenericArgInfo.getShortName());
	}

	@Test
	public void getShortName_localClass() {
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {};
		assertEquals("ClassInfoTest$LocalClass", of(LocalClass.class).getShortName());
	}


	@Test
	public void getSimpleName_simple() {
		assertEquals("AClass", aClass.getSimpleName());
	}

	@Test
	public void getSimpleName_simpleTwice() {
		assertEquals("AClass", aClass.getSimpleName());
		assertEquals("AClass", aClass.getSimpleName());
	}

	@Test
	public void getSimpleName_simpleArray() {
		assertEquals("AClass[][]", of(AClass[][].class).getSimpleName());
	}

	@Test
	public void getSimpleName_inner() {
		assertEquals("J1", j1.getSimpleName());
		assertEquals("J2", j2.getSimpleName());
	}

	@Test
	public void getSimpleName_innerArray() {
		assertEquals("J1[][]", j1_3d.getSimpleName());
		assertEquals("J2[][]", j2_3d.getSimpleName());
	}

	@Test
	public void getSimpleName_primitive() {
		assertEquals("int", of(int.class).getSimpleName());
	}

	@Test
	public void getSimpleName_primitiveArray() {
		assertEquals("int[][]", of(int[][].class).getSimpleName());
	}

	@Test
	public void getSimpleName_simpleType() {
		assertEquals("A1", aTypeInfo.getSimpleName());
	}

	@Test
	public void getSimpleName_complexType() {
		assertEquals("Map", pTypeInfo.getSimpleName());
	}

	@Test
	public void getSimpleName_dimensionalType() {
		assertEquals("Map", pTypeDimensionalInfo.getSimpleName());
	}

	@Test
	public void getSimpleName_genericType() {
		assertEquals("AbstractMap", pTypeGenericInfo.getSimpleName());
	}

	@Test
	public void getSimpleName_genericTypeArg() {
		assertEquals("V", pTypeGenericArgInfo.getSimpleName());
	}

	@Test
	public void getSimpleName_localClass() {
		@SuppressWarnings("serial")
		class LocalClass implements Serializable {};
		assertEquals("LocalClass", of(LocalClass.class).getSimpleName());
	}

	@Test
	public void getName() {
		assertEquals("org.apache.juneau.reflect.AClass", aClass.getName());
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


	@Test
	public void isParentOf() {
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

	@Test
	public void isParentOf_null() {
		assertFalse(ka.isParentOf(null));
	}

	@Test
	public void isParentOf_type() {
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

	@Test
	public void isChildOf() {
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

	@Test
	public void isChildOf_null() {
		assertFalse(ka.isChildOf((Class<?>)null));
	}

	@Test
	public void isChildOf_type() {
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

	@Test
	public void isStrictChildOf() {
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

	@Test
	public void isStrictChildOf_null() {
		assertFalse(ka.isStrictChildOf(null));
	}

	@Test
	public void isStrictChildOf_type() {
		assertFalse(aTypeInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeDimensionalInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeGenericInfo.isStrictChildOf(KA.class));
		assertFalse(pTypeGenericArgInfo.isStrictChildOf(KA.class));
	}

	@Test
	public void isChildOfAny() {
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

	@Test
	public void isChildOfAny_type() {
		assertFalse(aTypeInfo.isChildOfAny(KA.class));
		assertFalse(pTypeInfo.isChildOfAny(KA.class));
		assertFalse(pTypeDimensionalInfo.isChildOfAny(KA.class));
		assertFalse(pTypeGenericInfo.isChildOfAny(KA.class));
		assertFalse(pTypeGenericArgInfo.isChildOfAny(KA.class));
	}

	@Test
	public void is() {
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

	@Test
	public void is_ClassInfo() {
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

	@Test
	public void is_ClassInfo_genType() {
		assertFalse(pTypeGenericArgInfo.is(of(KA.class)));
	}

	@Test
	public void isAnyType() {
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


	@Test
	public void is_type() {
		assertFalse(aTypeInfo.is(KA.class));
		assertFalse(pTypeInfo.is(KA.class));
		assertFalse(pTypeDimensionalInfo.is(KA.class));
		assertFalse(pTypeGenericInfo.is(KA.class));
		assertFalse(pTypeGenericArgInfo.is(KA.class));
	}

	@Test
	public void getPackage() {
		check("org.apache.juneau.reflect", ka.getPackage().getName());
	}

	@Test
	public void getPackage_type() {
		check("org.apache.juneau.reflect", aTypeInfo.getPackage());
		check("java.util", pTypeInfo.getPackage());
		check("java.util", pTypeDimensionalInfo.getPackage());
		check("java.util", pTypeGenericInfo.getPackage());
		check(null, pTypeGenericArgInfo.getPackage());
	}

	@Test
	public void hasPackage() {
		assertTrue(ka.hasPackage());
	}

	@Test
	public void hasPackage_type() {
		assertTrue(aTypeInfo.hasPackage());
		assertTrue(pTypeInfo.hasPackage());
		assertTrue(pTypeDimensionalInfo.hasPackage());
		assertTrue(pTypeGenericInfo.hasPackage());
		assertFalse(pTypeGenericArgInfo.hasPackage());
	}

	@Test
	public void getDimensions() {
		assertEquals(0, ka.getDimensions());
		assertEquals(2, of(KA[][].class).getDimensions());
	}

	@Test
	public void getDimensions_type() {
		assertEquals(0, aTypeInfo.getDimensions());
		assertEquals(0, pTypeInfo.getDimensions());
		assertEquals(0, pTypeDimensionalInfo.getDimensions());
		assertEquals(0, pTypeGenericInfo.getDimensions());
		assertEquals(0, pTypeGenericArgInfo.getDimensions());
	}

	@Test
	public void getComponentType() {
		check("KA", ka.getComponentType());
		check("KA", of(KA[][].class).getComponentType());
	}

	@Test
	public void getComponentType_twice() {
		check("KA", ka.getComponentType());
		check("KA", ka.getComponentType());
	}

	@Test
	public void getComponentType_type() {
		check("A1", aTypeInfo.getComponentType());
		check("Map", pTypeInfo.getComponentType());
		check("Map", pTypeDimensionalInfo.getComponentType());
		check("AbstractMap", pTypeGenericInfo.getComponentType());
		check("V", pTypeGenericArgInfo.getComponentType());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation
	//-----------------------------------------------------------------------------------------------------------------

	public static class LA {}

	static ClassInfo la=of(LA.class);

	@Test
	public void newInstance() throws Exception {
		assertNotNull(la.newInstance());
	}

	@Test
	public void newInstance_type() {
		assertThrown(()->aTypeInfo.newInstance()).isType(ExecutableException.class);
		assertThrown(()->pTypeInfo.newInstance()).isType(ExecutableException.class);
		assertThrown(()->pTypeDimensionalInfo.newInstance()).isType(ExecutableException.class);
		assertThrown(()->pTypeGenericInfo.newInstance()).isType(Exception.class);
		assertThrown(()->pTypeGenericArgInfo.newInstance()).isType(ExecutableException.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter types
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("serial")
	public static class MA extends HashMap<String,Integer> {}
	@SuppressWarnings("serial")
	public static class MB extends MA {}
	@SuppressWarnings({ "serial", "hiding" })
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

	@Test
	public void getParameterType_simpleMap() {
		check("String", ma.getParameterType(0, HashMap.class));
		check("Integer", ma.getParameterType(1, HashMap.class));
		check("String", mb.getParameterType(0, HashMap.class));
		check("Integer", mb.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_outOfBounds() {
		assertThrown(()->ma.getParameterType(2, HashMap.class)).asMessage().is("Invalid type index. index=2, argsLength=2");
	}

	@Test
	public void getParameterType_notASubclass() {
		assertThrown(()->aClass.getParameterType(2, HashMap.class)).asMessage().is("Class 'AClass' is not a subclass of parameterized type 'HashMap'");
	}

	@Test
	public void getParameterType_nullParameterizedType() {
		assertThrown(()->aClass.getParameterType(2, null)).asMessage().is("Argument 'pt' cannot be null.");
	}

	@Test
	public void getParameterType_notParamerizedType() {
		assertThrown(()->mb.getParameterType(2, MA.class)).asMessage().is("Class 'MA' is not a parameterized type");
	}

	@Test
	public void getParameterType_unresolvedTypes() {
		assertThrown(()->mc.getParameterType(1, HashMap.class)).asMessage().is("Could not resolve variable 'E' to a type.");
	}

	@Test
	public void getParameterType_resolvedTypes() {
		check("Integer", md.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_parameterizedTypeVariable() {
		check("HashMap", me.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_arrayParameterType() {
		check("String[]", mf.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_genericArrayTypeParameter() {
		check("HashMap[]", mg.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_genericArrayTypeParameterWithoutTypes() {
		check("LinkedList[]", mh.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_unresolvedGenericArrayType() {
		assertThrown(()->mi.getParameterType(1, HashMap.class)).asMessage().is("Could not resolve variable 'X[]' to a type.");
	}

	@Test
	public void getParameterType_wildcardType() {
		assertThrown(()->mj.getParameterType(1, HashMap.class)).asMessage().is("Could not resolve variable 'X' to a type.");
	}

	@Test
	public void getParameterType_innerType() {
		check("MK", ml.getParameterType(1, HashMap.class));
	}

	@Test
	public void getParameterType_nestedType() {
		check("MM", mn.getParameterType(1, HashMap.class));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// ClassInfo.isParentOfFuzzyPrimitives(Class)
	// ClassInfo.isParentOfFuzzyPrimitives(Type)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void o01_isParentOfFuzzyPrimitives() {
		assertTrue(ClassInfo.of(String.class).isParentOfFuzzyPrimitives(String.class));
		assertTrue(ClassInfo.of(CharSequence.class).isParentOfFuzzyPrimitives(String.class));
		assertFalse(ClassInfo.of(String.class).isParentOfFuzzyPrimitives(CharSequence.class));
		assertTrue(ClassInfo.of(int.class).isParentOfFuzzyPrimitives(Integer.class));
		assertTrue(ClassInfo.of(Integer.class).isParentOfFuzzyPrimitives(int.class));
		assertTrue(ClassInfo.of(Number.class).isParentOfFuzzyPrimitives(int.class));
		assertFalse(ClassInfo.of(int.class).isParentOfFuzzyPrimitives(Number.class));
		assertFalse(ClassInfo.of(int.class).isParentOfFuzzyPrimitives(long.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void xToString() {
		assertEquals("class org.apache.juneau.reflect.AClass", aClass.toString());
		assertEquals("interface org.apache.juneau.reflect.AInterface", aInterface.toString());
		assertEquals("class org.apache.juneau.reflect.ClassInfoTest$A1", aType.toString());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.String>>", pType.toString());
		assertEquals("java.util.Map<java.lang.String, java.lang.String[][]>", pTypeDimensional.toString());
		assertEquals("java.util.AbstractMap<K, V>", pTypeGeneric.toString());
		assertEquals("V", pTypeGenericArg.toString());
	}
}
