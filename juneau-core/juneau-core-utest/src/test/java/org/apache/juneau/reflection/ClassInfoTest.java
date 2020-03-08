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
import static org.apache.juneau.reflect.ClassInfo.*;
import static org.apache.juneau.reflect.ReflectFlags.*;
import static org.junit.Assert.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.reflection.MethodInfoTest.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;
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

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@Inherited
	@PropertyStoreApply(AConfigApply.class)
	static @interface AConfig {
		int value();
	}

	public static class AConfigApply extends ConfigApply<AConfig> {
		protected AConfigApply(Class<AConfig> c, VarResolverSession r) {
			super(c, r);
		}
		@Override
		public void apply(AnnotationInfo<AConfig> a, PropertyStoreBuilder ps) {
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
				return apply(((AnnotationInfo<?>)t).getAnnotation());
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
	public void getParentsParentFirst() {
		check("BC1,BC2,BC3", bc3.getParentsParentFirst());
		check("", object.getParentsParentFirst());
		check("BI1", bi1.getParentsParentFirst());
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
	public void getAllParentsParentFirst() {
		check("BI2,BI1,BI3,BC1,BC2,BC3", bc3.getAllParentsParentFirst());
		check("", object.getAllParentsParentFirst());
		check("BI1", bi1.getAllParentsParentFirst());
	}

	@Test
	public void getParent() {
		check("BC2", bc3.getParent());
		check("BC1", bc2.getParent());
		check("Object", bc1.getParent());
		check(null, object.getParent());
		check(null, bi2.getParent());
		check(null, bi1.getParent());
	}

	@Test
	public void getParent_onType() {
		check("Object", aTypeInfo.getParent());
		check(null, pTypeInfo.getParent());
		check(null, pTypeDimensionalInfo.getParent());
		check("Object", pTypeGenericInfo.getParent());
		check(null, pTypeGenericArgInfo.getParent());
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
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
	}

	@Test
	public void getAllMethods_twice() throws Exception {
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
		check("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethods());
	}

	@Test
	public void getAllMethodsParentFirst() throws Exception {
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsParentFirst());
	}

	@Test
	public void getAllMethodsParentFirst_twice() throws Exception {
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsParentFirst());
		check("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodsParentFirst());
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

	static class C2 {
		public void a1() {}
		public void a2(int x){}
		void b1() {}
		void b2(int x){}
	}
	static ClassInfo c2 = ClassInfo.of(C2.class);

	@Test
	public void getPublicMethod_noArgs() {
		check("C2.a1()", c2.getPublicMethod("a1"));
		check(null, c2.getPublicMethod("a2"));
		check(null, c2.getPublicMethod("b1"));
		check(null, c2.getPublicMethod("b2"));
	}

	@Test
	public void getPublicMethod_withArgs() {
		check(null, c2.getPublicMethod("a1", int.class));
		check("C2.a2(int)", c2.getPublicMethod("a2", int.class));
		check(null, c2.getPublicMethod("b1", int.class));
		check(null, c2.getPublicMethod("b2", int.class));
	}

	@Test
	public void getMethod_noArgs() {
		check("C2.a1()", c2.getMethod("a1"));
		check(null, c2.getMethod("a2"));
		check("C2.b1()", c2.getMethod("b1"));
		check(null, c2.getMethod("b2"));
	}

	@Test
	public void getMethod_withArgs() {
		check(null, c2.getMethod("a1", int.class));
		check("C2.a2(int)", c2.getMethod("a2", int.class));
		check(null, c2.getMethod("b1", int.class));
		check("C2.b2(int)", c2.getMethod("b2", int.class));
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
	static ClassInfo da1=of(DA1.class), da2=of(DA2.class), da3=of(DA3.class), da4=of(DA4.class), da5=of(DA5.class), da6=of(DA6.class), da7=of(DA7.class), da8=of(DA8.class);

	@Test
	public void getFromStringMethod() throws Exception {
		check("DA1.create(String)", da1.getStaticCreateMethod(String.class));
		check(null, da2.getStaticCreateMethod(String.class));
		check(null, da3.getStaticCreateMethod(String.class));
		check(null, da4.getStaticCreateMethod(String.class));
		check(null, da5.getStaticCreateMethod(String.class));
		check(null, da6.getStaticCreateMethod(String.class));
		check(null, da7.getStaticCreateMethod(String.class));
		check(null, da8.getStaticCreateMethod(String.class));
	}

	@Test
	public void getFromStringMethod_onType() throws Exception {
		check(null, aTypeInfo.getStaticCreateMethod(String.class));
		check(null, pTypeGenericArgInfo.getStaticCreateMethod(String.class));
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
	static ClassInfo db1=of(DB1.class), db2=of(DB2.class), db3=of(DB3.class), db4=of(DB4.class), db5=of(DB5.class), db6=of(DB6.class), db7=of(DB7.class), db8=of(DB8.class), db9=of(DB9.class), db10=of(DB10.class), db11=of(DB11.class);

	@Test
	public void getStaticCreateMethod() throws Exception {
		check("DB1.create(DBx)", db1.getStaticCreateMethod(DBx.class));
		check("DB2.fromDBx(DBx)", db2.getStaticCreateMethod(DBx.class));
		check("DB3.from(DBx)", db3.getStaticCreateMethod(DBx.class));
		check(null, db4.getStaticCreateMethod(DBx.class));
		check(null, db5.getStaticCreateMethod(DBx.class));
		check(null, db6.getStaticCreateMethod(DBx.class));
		check(null, db7.getStaticCreateMethod(DBx.class));
		check(null, db8.getStaticCreateMethod(DBx.class));
		check(null, db9.getStaticCreateMethod(DBx.class));
		check(null, db10.getStaticCreateMethod(DBx.class));
		check(null, db11.getStaticCreateMethod(DBx.class));
	}

	@Test
	public void getStaticCreateMethod_onType() throws Exception {
		check(null, aTypeInfo.getStaticCreateMethod(DBx.class));
		check(null, pTypeGenericArgInfo.getStaticCreateMethod(DBx.class));
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
	static ClassInfo dc1=of(DC1.class), dc2=of(DC2.class), dc3=of(DC3.class), dc4=of(DC4.class), dc5=of(DC5.class);

	@Test
	public void getBuilderCreateMethod() throws Exception {
		check("DC1.create()", dc1.getBuilderCreateMethod());
		check(null, dc2.getBuilderCreateMethod());
		check(null, dc3.getBuilderCreateMethod());
		check(null, dc4.getBuilderCreateMethod());
		check(null, dc5.getBuilderCreateMethod());
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
	static ClassInfo dd1=of(DD1.class), dd2=of(DD2.class), dd3=of(DD3.class), dd4=of(DD4.class), dd5=of(DD5.class);

	@Test
	public void getBuilderBuildMethod() throws Exception {
		check("DD1.build()", dd1.getBuilderBuildMethod());
		check(null, dd2.getBuilderBuildMethod());
		check(null, dd3.getBuilderBuildMethod());
		check(null, dd4.getBuilderBuildMethod());
		check(null, dd5.getBuilderBuildMethod());
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
		check("E1(String)", e1.getPublicConstructor(String.class));
		check("E1(Writer)", e1.getAvailablePublicConstructor(StringWriter.class));
	}

	@Test
	public void getPublicConstructor_objectArgs() {
		check("E1(String)", e1.getPublicConstructor("foo"));
	}

	@Test
	public void getPublicConstructorFuzzy() {
		check("E1(String)", e1.getPublicConstructorFuzzy("foo", new HashMap<>()));
		check("E1()", e1.getPublicConstructorFuzzy(new HashMap<>()));
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
		check("E1()", e1.getPublicConstructor());
	}

	@Test
	public void getConstructor() {
		check("E1(int)", e1.getConstructor(Visibility.PROTECTED, int.class));
		check("E1(int)", e1.getConstructor(Visibility.PRIVATE, int.class));
		check(null, e1.getConstructor(Visibility.PUBLIC, int.class));
		check("E3()", e3.getConstructor(Visibility.PUBLIC));
		check("E4(ClassInfoTest)", e4.getConstructor(Visibility.PUBLIC));
		check("E5()", e5.getConstructor(Visibility.PUBLIC));
	}

	@Test
	public void getDeclaredConstructor() {
		check("E1()", e1.getDeclaredConstructor());
		check("E1(int)", e1.getDeclaredConstructor(int.class));
		check(null, e1.getDeclaredConstructor(Object.class));
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
	public void getAllFields() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFields());
	}

	@Test
	public void getAllFields_twice() {
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFields());
		check("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFields());
	}

	@Test
	public void getAllFields_onType() {
		check("A1.this$0", aTypeInfo.getAllFields());
		check("", pTypeInfo.getAllFields());
		check("", pTypeDimensionalInfo.getAllFields());
		check("AbstractMap.keySet,AbstractMap.values", pTypeGenericInfo.getAllFields());
		check("", pTypeGenericArgInfo.getAllFields());
	}

	@Test
	public void getAllFieldsParentFirst() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldsParentFirst());
	}

	@Test
	public void getAllFieldsParentFirst_twice() {
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldsParentFirst());
		check("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldsParentFirst());
	}

	static class F3 {
		public int a1;
		int a2;
	}
	static ClassInfo f3=of(F3.class);

	@Test
	public void getPublicField() {
		check("F3.a1", f3.getPublicField("a1"));
		check(null, f3.getPublicField("a2"));
		check(null, f3.getPublicField("a3"));
	}

	@Test
	public void getDeclaredField() {
		check("F3.a1", f3.getDeclaredField("a1"));
		check("F3.a2", f3.getDeclaredField("a2"));
		check(null, f3.getDeclaredField("a3"));
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
		check("@A(7)", g3.getLastAnnotation(A.class));
		check(null, g3.getLastAnnotation(B.class));
		check(null, g3.getLastAnnotation(null));
	}

	@Test
	public void getAnnotation_twice() {
		check("@A(7)", g3.getLastAnnotation(A.class));
		check("@A(7)", g3.getLastAnnotation(A.class));
	}

	@Test
	public void getAnnotation_onParent() {
		check("@A(7)", g4.getLastAnnotation(A.class));
		check(null, g4.getLastAnnotation(B.class));
		check(null, g4.getLastAnnotation(null));
	}

	@Test
	public void getAnnotation_onInterface() {
		check("@A(3)", g5.getLastAnnotation(A.class));
		check(null, g5.getLastAnnotation(B.class));
		check(null, g5.getLastAnnotation(null));
	}

	@Test
	public void hasAnnotation() {
		assertTrue(g3.hasAnnotation(A.class));
		assertFalse(g3.hasAnnotation(B.class));
		assertFalse(g3.hasAnnotation(null));
	}

	@Test
	public void getAnnotationsParentFirst() {
		check("@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g3.getAnnotations(A.class));
	}

	@Test
	public void getDeclaredAnnotation() {
		check("@A(7)", g3.getDeclaredAnnotation(A.class));
		check(null, g3.getDeclaredAnnotation(B.class));
	}

	@Test
	public void getDeclaredAnnotation_null() {
		check(null, g3.getDeclaredAnnotation(null));
	}

	@Test
	public void getDeclaredAnnotation_twice() {
		check("@A(7)", g3.getDeclaredAnnotation(A.class));
		check("@A(7)", g3.getDeclaredAnnotation(A.class));
	}

	@Test
	public void getDeclaredAnnotation_onType() {
		check(null, aTypeInfo.getDeclaredAnnotation(A.class));
	}

	@Test
	public void getDeclaredAnnotationInfo() {
		check("@A(7)", g3.getDeclaredAnnotationInfo(A.class));
		check(null, g3.getDeclaredAnnotationInfo(B.class));
	}

	@Test
	public void getDeclaredAnnotationInfo_twice() {
		check("@A(7)", g3.getDeclaredAnnotationInfo(A.class));
		check("@A(7)", g3.getDeclaredAnnotationInfo(A.class));
	}

	@Test
	public void getAnnotationInfosParentFirst() {
		check("@A(2),@A(1),@A(3),@A(5),@A(6),@A(7)", g3.getAnnotationInfos(A.class));
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
	public void getPackageAnnotationInfo() {
		check("@PA(10)", g3.getPackageAnnotationInfo(PA.class));
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
		check("@AConfig(2),@AConfig(1),@AConfig(3),@AConfig(5),@AConfig(6),@AConfig(7)", gb3.getAnnotationList(ConfigAnnotationFilter.INSTANCE));
		check("@AConfig(2),@AConfig(1),@AConfig(3),@AConfig(5),@AConfig(6),@AConfig(7)", gb4.getAnnotationList(ConfigAnnotationFilter.INSTANCE));
		check("@AConfig(3)", gb5.getAnnotationList(ConfigAnnotationFilter.INSTANCE));
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
		try {
			a.isAll(HAS_PARAMS);
			fail("Expected exception.");
		} catch (Exception e) {}
		try {
			a.isAll(HAS_NO_PARAMS);
			fail("Expected exception.");
		} catch (Exception e) {}
		try {
			a.isAll(TRANSIENT);
			fail("Expected exception.");
		} catch (Exception e) {}
		try {
			a.isAll(NOT_TRANSIENT);
			fail("Expected exception.");
		} catch (Exception e) {}
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
		try {
			a.isAny(HAS_PARAMS);
			fail("Expected exception.");
		} catch (Exception e) {}
		try {
			a.isAny(HAS_NO_PARAMS);
			fail("Expected exception.");
		} catch (Exception e) {}
		try {
			a.isAny(TRANSIENT);
			fail("Expected exception.");
		} catch (Exception e) {}
		try {
			a.isAny(NOT_TRANSIENT);
			fail("Expected exception.");
		} catch (Exception e) {}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive wrappers
	//-----------------------------------------------------------------------------------------------------------------

	static List<Class<?>> primitives = AList.create(boolean.class,byte.class,short.class,char.class,int.class,long.class,float.class,double.class);
	static List<Class<?>> primitiveWrappers = AList.create(Boolean.class,Byte.class,Short.class,Character.class,Integer.class,Long.class,Float.class,Double.class);
	static List<Object> primitiveDefaults = AList.create(false,(byte)0,(short)0,(char)0,0,0l,0f,0d);

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
		assertEquals("class org.apache.juneau.reflection.ClassInfoTest$A1", aTypeInfo.getWrapperIfPrimitive().toString());
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
		assertEquals("org.apache.juneau.reflection.AClass", aClass.getFullName());
	}

	@Test
	public void getFullName_simpleTwice() {
		assertEquals("org.apache.juneau.reflection.AClass", aClass.getFullName());
		assertEquals("org.apache.juneau.reflection.AClass", aClass.getFullName());
	}

	@Test
	public void getFullName_simpleArray() {
		assertEquals("org.apache.juneau.reflection.AClass[][]", of(AClass[][].class).getFullName());
	}

	@Test
	public void getFullName_inner() {
		assertEquals("org.apache.juneau.reflection.ClassInfoTest$J1", j1.getFullName());
		assertEquals("org.apache.juneau.reflection.ClassInfoTest$J2", j2.getFullName());
	}

	@Test
	public void getFullName_innerArray() {
		assertEquals("org.apache.juneau.reflection.ClassInfoTest$J1[][]", j1_3d.getFullName());
		assertEquals("org.apache.juneau.reflection.ClassInfoTest$J2[][]", j2_3d.getFullName());
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
		assertEquals("org.apache.juneau.reflection.ClassInfoTest$A1", aTypeInfo.getFullName());
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
		assertEquals("org.apache.juneau.reflection.ClassInfoTest$1LocalClass", of(LocalClass.class).getFullName());
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
		assertEquals("org.apache.juneau.reflection.AClass", aClass.getName());
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
		check("org.apache.juneau.reflection", ka.getPackage().getName());
	}

	@Test
	public void getPackage_type() {
		check("org.apache.juneau.reflection", aTypeInfo.getPackage());
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
	public void newInstance() {
		try {
			assertNotNull(la.newInstance());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void newInstance_type() {
		try {
			aTypeInfo.newInstance();
			fail();
		}
		catch (ExecutableException e) { /* OK */ }
		catch (Exception e) { fail(e.getMessage()); }

		try {
			pTypeInfo.newInstance();
			fail();
		}
		catch (ExecutableException e) { /* OK */ }
		catch (Exception e) { fail(e.getMessage()); }
		try {
			pTypeDimensionalInfo.newInstance();
			fail();
		}
		catch (ExecutableException e) { /* OK */ }
		catch (Exception e) { fail(e.getMessage()); }

		try {
			pTypeGenericInfo.newInstance();
			fail();
		}
		catch (ExecutableException e) { /* OK */ }
		catch (Exception e) { /* OK */ }
		try {
			pTypeGenericArgInfo.newInstance();
			fail();
		}
		catch (ExecutableException e) { /* OK */ }
		catch (Exception e) { fail(e.getMessage()); }
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
		try {
			ma.getParameterType(2, HashMap.class);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid type index. index=2, argsLength=2", e.getMessage());
		}
	}

	@Test
	public void getParameterType_notASubclass() {
		try {
			aClass.getParameterType(2, HashMap.class);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Class 'AClass' is not a subclass of parameterized type 'HashMap'", e.getMessage());
		}
	}

	@Test
	public void getParameterType_nullParameterizedType() {
		try {
			aClass.getParameterType(2, null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Parameterized type cannot be null", e.getMessage());
		}
	}

	@Test
	public void getParameterType_notParamerizedType() {
		try {
			mb.getParameterType(2, MA.class);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Class 'MA' is not a parameterized type", e.getMessage());
		}
	}

	@Test
	public void getParameterType_unresolvedTypes() {
		try {
			mc.getParameterType(1, HashMap.class);
			fail();
		} catch (Exception e) {
			assertEquals("Could not resolve variable 'E' to a type.", e.getMessage());
		}
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
		try {
			mi.getParameterType(1, HashMap.class);
		} catch (Exception e) {
			assertEquals("Could not resolve variable 'X[]' to a type.", e.getMessage());
		}
	}

	@Test
	public void getParameterType_wildcardType() {
		try {
			mj.getParameterType(1, HashMap.class);
		} catch (Exception e) {
			assertEquals("Could not resolve variable 'X' to a type.", e.getMessage());
		}
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
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void xToString() {
		assertEquals("class org.apache.juneau.reflection.AClass", aClass.toString());
		assertEquals("interface org.apache.juneau.reflection.AInterface", aInterface.toString());
		assertEquals("class org.apache.juneau.reflection.ClassInfoTest$A1", aType.toString());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.String>>", pType.toString());
		assertEquals("java.util.Map<java.lang.String, java.lang.String[][]>", pTypeDimensional.toString());
		assertEquals("java.util.AbstractMap<K, V>", pTypeGeneric.toString());
		assertEquals("V", pTypeGenericArg.toString());
	}
}
