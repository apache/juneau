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

import static org.apache.juneau.reflection.ClassInfo.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.*;

public class ClassInfoTest {

	private static void assertListEquals(String expected, List<?> l) {
		String actual = l
			.stream()
			.map(TO_STRING)
			.collect(Collectors.joining(","));
		assertEquals(expected, actual);
	}

	private static void assertInfoEquals(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
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
				return ((MethodInfo)t).getDeclaringClassInfo().getSimpleName() + '.' + ((MethodInfo)t).getLabel();
			if (t instanceof ConstructorInfo)
				return ((ConstructorInfo)t).getLabel();
			if (t instanceof FieldInfo)
				return ((FieldInfo)t).getDeclaringClassInfo().getSimpleName() + '.' + ((FieldInfo)t).getLabel();
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	public class A1 {}
	public class A2 extends Value<A1>{};

	@Test
	public void ofType() {
		assertInfoEquals("A1", of(A1.class));
	}

	@Test
	public void ofTypeOnObject() {
		assertInfoEquals("A1", of(new A1()));
	}

	@Test
	public void ofTypeOnNulls() {
		assertNull(of((Class<?>)null));
		assertNull(of((Type)null));
		assertNull(of((Object)null));
	}

	@Test
	public void inner() {
		assertTrue(of(A1.class).inner() instanceof Class);
		assertTrue(of(A1.class).innerType() instanceof Class);
	}

	@Test
	public void resolved() {
		assertInfoEquals("A1", of(A1.class).resolved());
		assertInfoEquals("A1", of(A2.class).resolved());
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
	public void getInterfaceInfos() {
		assertListEquals("", of(BI4.class).getInterfaceInfos());
		assertListEquals("BI1,BI2", of(BC1.class).getInterfaceInfos());
		assertListEquals("BI3", of(BC2.class).getInterfaceInfos());
		assertListEquals("", of(BC3.class).getInterfaceInfos());
	}

	@Test
	public void getInterfaceInfosTwice() {
		ClassInfo bc2 = of(BC2.class);
		assertListEquals("BI3", bc2.getInterfaceInfos());
		assertListEquals("BI3", bc2.getInterfaceInfos());
	}

	@Test
	public void getParentInfos() {
		ClassInfo bc3 = of(BC3.class);
		assertListEquals("BC3,BC2,BC1", bc3.getParentInfos());
		assertListEquals("BC3,BC2,BC1", bc3.getParentInfos(false,false));
		assertListEquals("BC1,BC2,BC3", bc3.getParentInfos(true,false));

		assertListEquals("BC3,BC2,BI3,BC1,BI1,BI2", bc3.getParentInfos(false,true));
		assertListEquals("BI2,BI1,BC1,BI3,BC2,BC3", bc3.getParentInfos(true,true));
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
	public void getAllMethodInfos() throws Exception {
		ClassInfo cc3 = of(CC3.class);
		assertListEquals("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethodInfos());
		assertListEquals("CC3.c3a(),CC3.c3b(),CC3.i2b(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CI1.i1a(),CI1.i1b(),CI2.i2a(),CI2.i2b()", cc3.getAllMethodInfos(false));
		assertListEquals("CI2.i2a(),CI2.i2b(),CI1.i1a(),CI1.i1b(),CC1.c1a(),CC1.c1b(),CC1.i1a(),CC2.c2a(),CC2.c2b(),CC2.i1b(),CC2.i2a(),CC2.i2b(),CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getAllMethodInfos(true));
	}

	@Test
	public void getDeclaredMethodInfos() throws Exception {
		ClassInfo cc3 = of(CC3.class), ci2 = of(CI2.class);
		assertListEquals("CC3.c3a(),CC3.c3b(),CC3.i2b()", cc3.getDeclaredMethodInfos());
		assertListEquals("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethodInfos());
		assertListEquals("CI2.i2a(),CI2.i2b()", ci2.getDeclaredMethodInfos());
	}

	@Test
	public void getPublicMethodInfos() throws Exception {
		ClassInfo cc3 = of(CC3.class), ci2 = of(CI2.class);
		assertListEquals("CC3.c1a(),CC3.c2b(),CC3.c3a(),CC3.i1a(),CC3.i1b(),CC3.i2a(),CC3.i2b()", cc3.getPublicMethodInfos());
		assertListEquals("CI2.i1a(),CI2.i1b(),CI2.i2a(),CI2.i2b()", ci2.getPublicMethodInfos());
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
	public void getFromStringMethodInfo() throws Exception {
		assertInfoEquals("DA1.create(String)", of(DA1.class).getFromStringMethodInfo());
		assertNull(of(DA2.class).getFromStringMethodInfo());
		assertNull(of(DA3.class).getFromStringMethodInfo());
		assertNull(of(DA4.class).getFromStringMethodInfo());
		assertNull(of(DA5.class).getFromStringMethodInfo());
		assertNull(of(DA6.class).getFromStringMethodInfo());
		assertNull(of(DA7.class).getFromStringMethodInfo());
		assertNull(of(DA8.class).getFromStringMethodInfo());
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
	public void getStaticCreateMethodInfo() throws Exception {
		assertInfoEquals("DB1.create(DBx)", of(DB1.class).getStaticCreateMethodInfo(DBx.class));
		assertInfoEquals("DB2.fromDBx(DBx)", of(DB2.class).getStaticCreateMethodInfo(DBx.class));
		assertInfoEquals("DB3.from(DBx)", of(DB3.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB4.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB5.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB6.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB7.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB8.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB9.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB10.class).getStaticCreateMethodInfo(DBx.class));
		assertNull(of(DB11.class).getStaticCreateMethodInfo(DBx.class));
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
	public void getBuilderCreateMethodInfo() throws Exception {
		assertInfoEquals("DC1.create()", of(DC1.class).getBuilderCreateMethodInfo());
		assertNull(of(DC2.class).getBuilderCreateMethodInfo());
		assertNull(of(DC3.class).getBuilderCreateMethodInfo());
		assertNull(of(DC4.class).getBuilderCreateMethodInfo());
		assertNull(of(DC5.class).getBuilderCreateMethodInfo());
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
	public void getBuilderBuildMethodInfo() throws Exception {
		assertInfoEquals("DD1.build()", of(DD1.class).getBuilderBuildMethodInfo());
		assertNull(of(DD2.class).getBuilderBuildMethodInfo());
		assertNull(of(DD3.class).getBuilderBuildMethodInfo());
		assertNull(of(DD4.class).getBuilderBuildMethodInfo());
		assertNull(of(DD5.class).getBuilderBuildMethodInfo());
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
	public void getPublicConstructorInfos() {
		ClassInfo e1 = of(E1.class);
		assertListEquals("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructorInfos());
		assertListEquals("E1(),E1(Writer),E1(String),E1(String,Writer)", e1.getPublicConstructorInfos());
	}

	@Test
	public void getPublicConstructorInfo_classArgs() {
		ClassInfo e1 = of(E1.class);
		assertInfoEquals("E1(String)", e1.getPublicConstructorInfo(String.class));
		assertInfoEquals("E1(Writer)", e1.getPublicConstructorInfo(StringWriter.class));
	}

	@Test
	public void getPublicConstructorInfo_objectArgs() {
		ClassInfo e1 = of(E1.class);
		assertInfoEquals("E1(String)", e1.getPublicConstructorInfo("foo"));
	}

	@Test
	public void getPublicConstructorFuzzyInfo() {
		ClassInfo e1 = of(E1.class);
		assertInfoEquals("E1(String)", e1.getPublicConstructorFuzzyInfo("foo", new HashMap<>()));
		assertInfoEquals("E1()", e1.getPublicConstructorFuzzyInfo(new HashMap<>()));
	}

	@Test
	public void getNoArgConstructorInfo() {
		ClassInfo e2 = of(E2.class);
		assertInfoEquals("E2()", e2.getNoArgConstructorInfo(Visibility.PRIVATE));
		assertInfoEquals("E2()", e2.getNoArgConstructorInfo(Visibility.PROTECTED));
		assertInfoEquals("E2()", e2.getNoArgConstructorInfo(Visibility.DEFAULT));
		assertNull(e2.getNoArgConstructorInfo(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructorInfo_abstractClass() {
		ClassInfo e3 = of(E3.class);
		assertNull(e3.getNoArgConstructorInfo(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructorInfo_innerClass() {
		ClassInfo e4 = of(E4.class);
		assertInfoEquals("E4(ClassInfoTest)", e4.getNoArgConstructorInfo(Visibility.PUBLIC));
	}

	@Test
	public void getNoArgConstructorInfo_noConstructor() {
		ClassInfo e6 = of(E6.class);
		assertNull(e6.getNoArgConstructorInfo(Visibility.PUBLIC));
	}

	@Test
	public void getPublicNoArgConstructorInfo() {
		ClassInfo e1 = of(E1.class);
		assertInfoEquals("E1()", e1.getPublicNoArgConstructorInfo());
	}

	@Test
	public void getConstructorInfo() {
		ClassInfo e1 = of(E1.class);
		assertInfoEquals("E1(int)", e1.getConstructorInfo(Visibility.PROTECTED, int.class));
		assertInfoEquals("E1(int)", e1.getConstructorInfo(Visibility.PRIVATE, int.class));
		assertNull(e1.getConstructorInfo(Visibility.PUBLIC, int.class));

		ClassInfo ea3 = of(E3.class);
		assertInfoEquals("E3()", ea3.getConstructorInfo(Visibility.PUBLIC));

		ClassInfo ea4 = of(E4.class);
		assertInfoEquals("E4(ClassInfoTest)", ea4.getConstructorInfo(Visibility.PUBLIC));

		ClassInfo ea5 = of(E5.class);
		assertInfoEquals("E5()", ea5.getConstructorInfo(Visibility.PUBLIC));
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
	public void getPublicFieldInfos() {
		ClassInfo f2 = of(F2.class);
		assertListEquals("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFieldInfos());
		assertListEquals("F2.f1a,F1.f1b,F2.f2b", f2.getPublicFieldInfos());
	}

	@Test
	public void getDeclaredFieldInfos() {
		ClassInfo f2 = of(F2.class);
		assertListEquals("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFieldInfos());
		assertListEquals("F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getDeclaredFieldInfos());
	}

	@Test
	public void getAllFieldInfos() {
		ClassInfo f2 = of(F2.class);
		assertListEquals("F2.f1a,F2.f2b,F2.f2c,F2.f2d,F1.f1a,F1.f1b", f2.getAllFieldInfos());
	}

	@Test
	public void getAllFieldInfos_parentFirst() {
		ClassInfo f2 = of(F2.class);
		assertListEquals("F1.f1a,F1.f1b,F2.f1a,F2.f2b,F2.f2c,F2.f2d", f2.getAllFieldInfos(true));
	}

	//====================================================================================================
	// isParentClass(Class, Class)
	//====================================================================================================

	public interface B1x {}
	public static class B2x implements B1x {}
	public static class B3x extends B2x {}


	@Test
	public void testIsParentClass() throws Exception {

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
		assertEquals("ClassInfoTest.G1", of(G1.class).getShortName());
		assertEquals("ClassInfoTest.G2", of(G2.class).getShortName());
	}

	public class G1 {}
	public static class G2 {}


}
