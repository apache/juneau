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
import static org.apache.juneau.reflection.ClassInfo.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.*;

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
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			if (t instanceof MethodInfo)
				return ((MethodInfo)t).getDeclaringClass().getSimpleName() + '.' + ((MethodInfo)t).getLabel();
			if (t instanceof ConstructorInfo)
				return ((ConstructorInfo)t).getLabel();
			if (t instanceof FieldInfo)
				return ((FieldInfo)t).getDeclaringClass().getSimpleName() + '.' + ((FieldInfo)t).getLabel();
			if (t instanceof A)
				return "@A(" + ((A)t).value() + ")";
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Initialization
	//-----------------------------------------------------------------------------------------------------------------

	public class A1 {}
	public class A2 extends Value<A1>{};

	@Test
	public void ofType() {
		check("A1", of(A1.class));
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
		check("A1", of(A1.class).resolved());
		check("A1", of(A2.class).resolved());
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

	@Test
	public void getDeclaredInterfaces() {
		check("", of(BI4.class).getDeclaredInterfaces());
		check("BI1,BI2", of(BC1.class).getDeclaredInterfaces());
		check("BI3", of(BC2.class).getDeclaredInterfaces());
		check("", of(BC3.class).getDeclaredInterfaces());
	}

	@Test
	public void getDeclaredInterfaces_twice() {
		ClassInfo bc1 = of(BC1.class);
		check("BI1,BI2", bc1.getDeclaredInterfaces());
		check("BI1,BI2", bc1.getDeclaredInterfaces());
	}

	@Test
	public void getInterfaces() {
		check("", of(BI4.class).getInterfaces());
		check("BI1,BI2", of(BC1.class).getInterfaces());
		check("BI3,BI1,BI2", of(BC2.class).getInterfaces());
		check("BI3,BI1,BI2", of(BC3.class).getInterfaces());
	}

	@Test
	public void getInterfaces_tiwce() {
		ClassInfo bc2 = of(BC2.class);
		check("BI3,BI1,BI2", bc2.getInterfaces());
		check("BI3,BI1,BI2", bc2.getInterfaces());
	}

	@Test
	public void getParents() {
		ClassInfo bc3 = of(BC3.class);
		check("BC3,BC2,BC1", bc3.getParents());
		check("", of(Object.class).getParents());
		check("BI1", of(BI1.class).getParents());
	}

	@Test
	public void getParentsParentFirst() {
		ClassInfo bc3 = of(BC3.class);
		check("BC1,BC2,BC3", bc3.getParentsParentFirst());
		check("", of(Object.class).getParentsParentFirst());
		check("BI1", of(BI1.class).getParentsParentFirst());
	}

	@Test
	public void getAllParents() {
		ClassInfo bc3 = of(BC3.class);
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("", of(Object.class).getAllParents());
		check("BI1", of(BI1.class).getAllParents());
	}

	@Test
	public void getAllParents_twice() {
		ClassInfo bc3 = of(BC3.class);
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
		check("BC3,BC2,BC1,BI3,BI1,BI2", bc3.getAllParents());
	}

	@Test
	public void getAllParentsParentFirst() {
		ClassInfo bc3 = of(BC3.class);
		check("BI2,BI1,BI3,BC1,BC2,BC3", bc3.getAllParentsParentFirst());
		check("", of(Object.class).getAllParentsParentFirst());
		check("BI1", of(BI1.class).getAllParentsParentFirst());
	}

	@Test
	public void getParent() {
		check("BC2", of(BC3.class).getParent());
		check("BC1", of(BC2.class).getParent());
		check("Object", of(BC1.class).getParent());
		check(null, of(Object.class).getParent());
		check(null, of(BI2.class).getParent());
		check(null, of(BI1.class).getParent());
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

	@Test
	public void getPublicMethods() throws Exception {
		ClassInfo cc3 = of(CC3.class), ci2 = of(CI2.class);
		check("CC3.c1a(),CC3.c2b(),CC3.c3a(),CC3.i1a(),CC3.i1b(),CC3.i2a(),CC3.i2b()", cc3.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
	}

	@Test
	public void getPublicMethods_twice() throws Exception {
		ClassInfo ci2 = of(CI2.class);
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
		check("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethods());
	}

	@Test
	public void getAllMethods() throws Exception {
		ClassInfo cc3 = of(CC3.class);
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
	}

	@Test
	public void getAllMethods_twice() throws Exception {
		ClassInfo cc3 = of(CC3.class);
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
	}

	@Test
	public void getAllMethodsParentFirst() throws Exception {
		ClassInfo cc3 = of(CC3.class);
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsParentFirst());
	}

	@Test
	public void getAllMethodsParentFirst_twice() throws Exception {
		ClassInfo cc3 = of(CC3.class);
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsParentFirst());
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsParentFirst());
	}

	@Test
	public void getDeclaredMethods() throws Exception {
		ClassInfo cc3 = of(CC3.class), ci2 = of(CI2.class);
		check("CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
	}

	@Test
	public void getDeclaredMethods_twice() throws Exception {
		ClassInfo ci2 = of(CI2.class);
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
		check("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethods());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Special methods
	//-----------------------------------------------------------------------------------------------------------------

	static class DA1 {
		public static DA1 create(String s) {return null;}
	}
	static class DA2 {
		public static DA2 create(Object o) {return null;}
	}
	static class DA3 {
		@Deprecated
		public static DA3 create(String s) {return null;}
	}
	static class DA4 {
		public static DA1 create(String s) {return null;}
	}
	static class DA5 {
		public static DA5 create(String s1, String s2) {return null;}
	}
	static class DA6 {
		public DA6 create(String s1) {return null;}
	}
	static class DA7 {
		static DA7 create(String s1) {return null;}
	}
	static class DA8 {
		public static DA8 create2(String s1) {return null;}
	}

	@Test
	public void getFromStringMethod() throws Exception {
		check("DA1.create(String)", of(DA1.class).getFromStringMethod());
		check(null, of(DA2.class).getFromStringMethod());
		check(null, of(DA3.class).getFromStringMethod());
		check(null, of(DA4.class).getFromStringMethod());
		check(null, of(DA5.class).getFromStringMethod());
		check(null, of(DA6.class).getFromStringMethod());
		check(null, of(DA7.class).getFromStringMethod());
		check(null, of(DA8.class).getFromStringMethod());
	}

	static class DBx {}
	static class DB1 {
		public static DB1 create(DBx x) {return null;}
	}
	static class DB2 {
		public static DB2 fromDBx(DBx x) {return null;}
	}
	static class DB3 {
		public static DB3 from(DBx x) {return null;}
	}
	static class DB4 {
		public static DBx fromDBx(DBx x) {return null;}
	}
	static class DB5 {
		public DB5 fromDBx(DBx x) {return null;}
	}
	static class DB6 {
		protected static DB6 fromDBx(DBx x) {return null;}
	}
	static class DB7 {
		protected static DB7 from(DBx x) {return null;}
	}
	static class DB8 {
		@Deprecated
		public static DB8 create(DBx x) {return null;}
	}
	static class DB9 {
		public static DB9 create(DB1 x) {return null;}
	}
	static class DB10 {
		public static DB10 foo(DBx x) {return null;}
	}
	static class DB11 {
		public static DB11 fromFoo(DBx x) {return null;}
	}

	@Test
	public void getStaticCreateMethod() throws Exception {
		check("DB1.create(DBx)", of(DB1.class).getStaticCreateMethod(DBx.class));
		check("DB2.fromDBx(DBx)", of(DB2.class).getStaticCreateMethod(DBx.class));
		check("DB3.from(DBx)", of(DB3.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB4.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB5.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB6.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB7.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB8.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB9.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB10.class).getStaticCreateMethod(DBx.class));
		check(null, of(DB11.class).getStaticCreateMethod(DBx.class));
	}

	static class DCx {}
	static class DC1 {
		public static DCx create() {return null;}
	}
	static class DC2 {
		protected static DCx create() {return null;}
	}
	static class DC3 {
		public DCx create() {return null;}
	}
	static class DC4 {
		public static void create() {}
	}
	static class DC5 {
		public static DCx createFoo() {return null;}
	}

	@Test
	public void getBuilderCreateMethod() throws Exception {
		check("DC1.create()", of(DC1.class).getBuilderCreateMethod());
		check(null, of(DC2.class).getBuilderCreateMethod());
		check(null, of(DC3.class).getBuilderCreateMethod());
		check(null, of(DC4.class).getBuilderCreateMethod());
		check(null, of(DC5.class).getBuilderCreateMethod());
	}

	static class DDx {}
	static class DD1 {
		public DDx build() {return null;}
	}
	static class DD2 {
		public void build() {}
	}
	static class DD3 {
		public static DDx build() {return null;}
	}
	static class DD4 {
		public DDx build2() {return null;}
	}
	static class DD5 {
		public DDx build(String x) {return null;}
	}

	@Test
	public void getBuilderBuildMethod() throws Exception {
		check("DD1.build()", of(DD1.class).getBuilderBuildMethod());
		check(null, of(DD2.class).getBuilderBuildMethod());
		check(null, of(DD3.class).getBuilderBuildMethod());
		check(null, of(DD4.class).getBuilderBuildMethod());
		check(null, of(DD5.class).getBuilderBuildMethod());
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

	@Test
	public void getPublicConstructors() {
		ClassInfo e1 = of(E1.class);
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
		check("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructors());
	}

	@Test
	public void getPublicConstructor_classArgs() {
		ClassInfo e1 = of(E1.class);
		check("E1(String)", e1.getPublicConstructor(String.class));
		check("E1(Writer)", e1.getPublicConstructor(StringWriter.class));
	}

	@Test
	public void getPublicConstructor_objectArgs() {
		ClassInfo e1 = of(E1.class);
		check("E1(String)", e1.getPublicConstructor("foo"));
	}

	@Test
	public void getPublicConstructorFuzzy() {
		ClassInfo e1 = of(E1.class);
		check("E1(String)", e1.getPublicConstructorFuzzy("foo", new HashMap<>()));
		check("E1()", e1.getPublicConstructorFuzzy(new HashMap<>()));
	}

	@Test
	public void getNoArgConstructor() {
		ClassInfo e2 = of(E2.class);
		check("E2()", e2.getNoArgConstructor(Visibility.PRIVATE));
		check("E2()", e2.getNoArgConstructor(Visibility.PROTECTED));
		check("E2()", e2.getNoArgConstructor(Visibility.DEFAULT));
		check(null, e2.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructor_abstractClass() {
		ClassInfo e3 = of(E3.class);
		check(null, e3.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructor_innerClass() {
		ClassInfo e4 = of(E4.class);
		check("E4(ClassInfoTest)", e4.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructor_noConstructor() {
		ClassInfo e6 = of(E6.class);
		check(null, e6.getNoArgConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getPublicNoArgConstructor() {
		ClassInfo e1 = of(E1.class);
		check("E1()", e1.getPublicNoArgConstructor());
	}

	@Test
	public void getConstructor() {
		ClassInfo e1 = of(E1.class);
		check("E1(int)", e1.getConstructor(Visibility.PROTECTED, int.class));
		check("E1(int)", e1.getConstructor(Visibility.PRIVATE, int.class));
		check(null, e1.getConstructor(Visibility.PUBLIC, int.class));

		ClassInfo ea3 = of(E3.class);
		check("E3()", ea3.getConstructor(Visibility.PUBLIC));

		ClassInfo ea4 = of(E4.class);
		check("E4(ClassInfoTest)", ea4.getConstructor(Visibility.PUBLIC));

		ClassInfo ea5 = of(E5.class);
		check("E5()", ea5.getConstructor(Visibility.PUBLIC));
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

	@Test
	public void getPublicFields() {
		ClassInfo f2 = of(F2.class);
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicField());
		check("F2.f1a,F1.f1b,F2.f2b", f2.getPublicField());
	}

	@Test
	public void getDeclaredFields() {
		ClassInfo f2 = of(F2.class);
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredField());
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredField());
	}

	@Test
	public void getAllFields() {
		ClassInfo f2 = of(F2.class);
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFields());
	}

	@Test
	public void getAllFields_twice() {
		ClassInfo f2 = of(F2.class);
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFields());
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFields());
	}

	@Test
	public void getAllFieldsParentFirst() {
		ClassInfo f2 = of(F2.class);
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldsParentFirst());
	}

	@Test
	public void getAllFieldsParentFirst_twice() {
		ClassInfo f2 = of(F2.class);
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldsParentFirst());
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldsParentFirst());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@A(1)
	static interface GI1 {}
	@A(2)
	static interface GI2 extends GI1 {}
	@A(3)
	static interface GI3 {}
	@A(4)
	static interface GI4 {}
	@A(5)
	static class G1 implements GI1, GI2 {}
	@A(6)
	static class G2 extends G1 implements GI3 {}
	@A(7)
	static class G3 extends G2 {}

	@Test
	public void getAnnotation() {
		ClassInfo g3 = of(G3.class);
		check("@A(7)", g3.getAnnotation(A.class));
		check(null, g3.getAnnotation(B.class));
		check(null, g3.getAnnotation(null));
	}

	@Test
	public void getAnnotation_twice() {
		ClassInfo g3 = of(G3.class);
		check("@A(7)", g3.getAnnotation(A.class));
		check("@A(7)", g3.getAnnotation(A.class));
	}

	@Test
	public void hasAnnotation() {
		ClassInfo g3 = of(G3.class);
		assertTrue(g3.hasAnnotation(A.class));
		assertFalse(g3.hasAnnotation(B.class));
		assertFalse(g3.hasAnnotation(null));
	}

	@Test
	public void getAnnotations() {
		ClassInfo g3 = of(G3.class);
		check("@A(7),@A(6),@A(5),@A(1),@A(2),@A(1),@A(3)", g3.getAnnotations(A.class));
	}

	@Test
	public void getAnnotations_parentFirst() {
	//public <T extends Annotation> List<T> getAnnotations(Class<T> a, boolean parentFirst) {
	}

	@Test
	public void getDeclaredAnnotation() {
	//public <T extends Annotation> T getDeclaredAnnotation(Class<T> a) {
	}

	@Test
	public void appendAnnotations() {
	//public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a) {
	}

	@Test
	public void appendAnnotations_parentFirst() {
	//public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a, boolean parentFirst) {
	}

	@Test
	public void getClassAnnotations() {
	//public <T extends Annotation> List<ClassAnnotation<T>> getClassAnnotations(Class<T> a) {
	}

	@Test
	public void getClassAnnotations_parentFirst() {
	//public <T extends Annotation> List<ClassAnnotation<T>> getClassAnnotations(Class<T> a, boolean parentFirst) {
	}









































	//====================================================================================================
	// isParentClass(Class, Class)
	//====================================================================================================

	public interface B1x {}
	public static class B2x implements B1x {}
	public static class B3x extends B2x {}


	@Test
	public void isParentOf() throws Exception {

		// Strict
		assertTrue(of(B1x.class).isParentOf(B2x.class, true));
		assertTrue(of(B2x.class).isParentOf(B3x.class, true));
		assertTrue(of(Object.class).isParentOf(B3x.class, true));
		assertFalse(of(B1x.class).isParentOf(B1x.class, true));
		assertFalse(of(B2x.class).isParentOf(B2x.class, true));
		assertFalse(of(B3x.class).isParentOf(B3x.class, true));
		assertFalse(of(B3x.class).isParentOf(B2x.class, true));
		assertFalse(of(B2x.class).isParentOf(B1x.class, true));
		assertFalse(of(B3x.class).isParentOf(Object.class, true));

		// Not strict
		assertTrue(of(B1x.class).isParentOf(B2x.class, false));
		assertTrue(of(B2x.class).isParentOf(B3x.class, false));
		assertTrue(of(Object.class).isParentOf(B3x.class, false));
		assertTrue(of(B1x.class).isParentOf(B1x.class, false));
		assertTrue(of(B2x.class).isParentOf(B2x.class, false));
		assertTrue(of(B3x.class).isParentOf(B3x.class, false));
		assertFalse(of(B3x.class).isParentOf(B2x.class, false));
		assertFalse(of(B2x.class).isParentOf(B1x.class, false));
		assertFalse(of(B3x.class).isParentOf(Object.class, false));
	}



	//====================================================================================================
	// getSimpleName()
	//====================================================================================================

	@Test
	public void getShortName() throws Exception {
		assertEquals("ClassInfoTest.Gx1", of(Gx1.class).getShortName());
		assertEquals("ClassInfoTest.Gx2", of(Gx2.class).getShortName());
	}

	public class Gx1 {}
	public static class Gx2 {}


}
