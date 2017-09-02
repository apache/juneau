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
package org.apache.juneau.utils;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.junit.Assert.*;
import static org.apache.juneau.TestUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.internal.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class ClassUtilsTest {

	//====================================================================================================
	// getReadableClassName(Class)
	// getReadableClassName(String)
	//====================================================================================================
	@Test
	public void testGetReadableClassName() throws Exception {
		assertEquals("java.lang.Object", getReadableClassName(Object.class));
		assertEquals("java.lang.Object[]", getReadableClassName(Object[].class));
		assertEquals("java.lang.Object[][]", getReadableClassName(Object[][].class));

		assertEquals("boolean", getReadableClassName(boolean.class));
		assertEquals("boolean[]", getReadableClassName(boolean[].class));
		assertEquals("boolean[][]", getReadableClassName(boolean[][].class));

		assertEquals("byte", getReadableClassName(byte.class));
		assertEquals("byte[]", getReadableClassName(byte[].class));
		assertEquals("byte[][]", getReadableClassName(byte[][].class));

		assertEquals("char", getReadableClassName(char.class));
		assertEquals("char[]", getReadableClassName(char[].class));
		assertEquals("char[][]", getReadableClassName(char[][].class));

		assertEquals("double", getReadableClassName(double.class));
		assertEquals("double[]", getReadableClassName(double[].class));
		assertEquals("double[][]", getReadableClassName(double[][].class));

		assertEquals("float", getReadableClassName(float.class));
		assertEquals("float[]", getReadableClassName(float[].class));
		assertEquals("float[][]", getReadableClassName(float[][].class));

		assertEquals("int", getReadableClassName(int.class));
		assertEquals("int[]", getReadableClassName(int[].class));
		assertEquals("int[][]", getReadableClassName(int[][].class));

		assertEquals("long", getReadableClassName(long.class));
		assertEquals("long[]", getReadableClassName(long[].class));
		assertEquals("long[][]", getReadableClassName(long[][].class));

		assertEquals("short", getReadableClassName(short.class));
		assertEquals("short[]", getReadableClassName(short[].class));
		assertEquals("short[][]", getReadableClassName(short[][].class));

		assertNull(getReadableClassName((Class<?>)null));
		assertNull(getReadableClassName((String)null));
	}

	//====================================================================================================
	// isParentClass(Class, Class)
	//====================================================================================================
	@Test
	public void testIsParentClass() throws Exception {

		// Strict
		assertTrue(isParentClass(A.class, A1.class, true));
		assertTrue(isParentClass(A1.class, A2.class, true));
		assertTrue(isParentClass(Object.class, A2.class, true));
		assertFalse(isParentClass(A.class, A.class, true));
		assertFalse(isParentClass(A1.class, A1.class, true));
		assertFalse(isParentClass(A2.class, A2.class, true));
		assertFalse(isParentClass(A2.class, A1.class, true));
		assertFalse(isParentClass(A1.class, A.class, true));
		assertFalse(isParentClass(A2.class, Object.class, true));

		// Not strict
		assertTrue(isParentClass(A.class, A1.class, false));
		assertTrue(isParentClass(A1.class, A2.class, false));
		assertTrue(isParentClass(Object.class, A2.class, false));
		assertTrue(isParentClass(A.class, A.class, false));
		assertTrue(isParentClass(A1.class, A1.class, false));
		assertTrue(isParentClass(A2.class, A2.class, false));
		assertFalse(isParentClass(A2.class, A1.class, false));
		assertFalse(isParentClass(A1.class, A.class, false));
		assertFalse(isParentClass(A2.class, Object.class, false));
	}

	public interface A {}

	public static class A1 implements A {}

	public static class A2 extends A1 {}

	//====================================================================================================
	// getReadableClassNames(Object[])
	//====================================================================================================
	@Test
	public void testGetReadableClassNames() throws Exception {
		assertEquals("['java.lang.String','java.lang.Integer','java.lang.Boolean','null']", getReadableClassNames(new Object[]{"a",1,true,null}).toString());
	}

	public void getClassFromReadableName() throws Exception {
		fail("Not implemented");
	}

	//====================================================================================================
	// findPublicMethod
	//====================================================================================================
	@Test
	public void testFindPublicMethod() {

		assertNotNull(findPublicMethod(B.class, "m1", void.class));
		assertNull(findPublicMethod(B.class, "m1", int.class));

		assertNull(findPublicMethod(B.class, "m2", void.class));

		assertNull(findPublicMethod(B.class, "m3", void.class));
		assertNotNull(findPublicMethod(B.class, "m3", int.class));

		assertNotNull(findPublicMethod(B.class, "m4", CharSequence.class));
		assertNotNull(findPublicMethod(B.class, "m4", Object.class));
		assertNull(findPublicMethod(B.class, "m4", String.class));

		assertNotNull(findPublicMethod(B.class, "m5", void.class, int.class, CharSequence.class));
		assertNotNull(findPublicMethod(B.class, "m5", void.class, int.class, String.class));
		assertNull(findPublicMethod(B.class, "m5", void.class, int.class, Object.class));

		assertNull(findPublicMethod(B.class, "m5", void.class, int.class));
		assertNull(findPublicMethod(B.class, "m5", void.class, int.class, CharSequence.class, CharSequence.class));
	}

	public static class B {

		public void m1() {};
		protected void m2() {};
		public int m3() { return 0; }
		public CharSequence m4() { return ""; }

		public void m5(int f1, CharSequence f2) {}
	}


	//====================================================================================================
	// getMethodAnnotation
	//====================================================================================================
	@Test
	public void getMethodAnnotations() throws Exception {
		assertEquals("a1", getMethodAnnotation(TestAnnotation.class, CI3.class.getMethod("a1")).value());
		assertEquals("a2b", getMethodAnnotation(TestAnnotation.class, CI3.class.getMethod("a2")).value());
		assertEquals("a3", getMethodAnnotation(TestAnnotation.class, CI3.class.getMethod("a3", CharSequence.class)).value());
		assertEquals("a4", getMethodAnnotation(TestAnnotation.class, CI3.class.getMethod("a4")).value());
	}

	public static interface CI1 {
		@TestAnnotation("a1")
		void a1();
		@TestAnnotation("a2a")
		void a2();
		@TestAnnotation("a3")
		void a3(CharSequence foo);

		void a4();
	}

	public static class CI2 implements CI1 {
		@Override
		public void a1() {}
		@Override
		@TestAnnotation("a2b")
		public void a2() {}
		@Override
		public void a3(CharSequence s) {}
		@Override
		public void a4() {}
	}

	public static class CI3 extends CI2 {
		@Override
		public void a1() {}
		@Override public void a2() {}
		@Override
		@TestAnnotation("a4")
		public void a4() {}
	}

	@Target(METHOD)
	@Retention(RUNTIME)
	public @interface TestAnnotation {
		String value() default "";
	}

	//====================================================================================================
	// getParentClassesParentFirst()
	//====================================================================================================
	@Test
	public void getParentClassesParentFirst() throws Exception {
		Set<String> s = new TreeSet<String>();
		for (Iterator<Class<?>> i = ClassUtils.getParentClasses(CD.class, true, true); i.hasNext();) {
			Class<?> c = i.next();
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CA1','CA2','CA3','CB','CC','CD']", s);

		s = new TreeSet<String>();
		for (Iterator<Class<?>> i = ClassUtils.getParentClasses(CD.class, true, false); i.hasNext();) {
			Class<?> c = i.next();
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CB','CC','CD']", s);

		s = new TreeSet<String>();
		for (Iterator<Class<?>> i = ClassUtils.getParentClasses(CD.class, false, true); i.hasNext();) {
			Class<?> c = i.next();
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CA1','CA2','CA3','CB','CC','CD']", s);

		s = new TreeSet<String>();
		for (Iterator<Class<?>> i = ClassUtils.getParentClasses(CD.class, false, false); i.hasNext();) {
			Class<?> c = i.next();
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CB','CC','CD']", s);
	}

	static interface CA1 {}
	static interface CA2 extends CA1 {}
	static interface CA3 {}
	static interface CA4 {}
	static class CB implements CA1, CA2 {}
	static class CC extends CB implements CA3 {}
	static class CD extends CC {}

	//====================================================================================================
	// getAllMethodsParentFirst()
	//====================================================================================================
	@Test
	public void getParentMethodsParentFirst() throws Exception {
		Set<String> s = new TreeSet<String>();
		for (Method m : ClassUtils.getAllMethods(DD.class, true))
			if (! m.getName().startsWith("$"))
				s.add(m.getDeclaringClass().getSimpleName() + '.' + m.getName());
		assertObjectEquals("['DA1.da1','DA2.da2','DB.da1','DB.db','DC.da2','DC.dc','DD.da2','DD.dd']", s);

		s = new TreeSet<String>();
		for (Method m : ClassUtils.getAllMethods(DD.class, false))
			if (! m.getName().startsWith("$"))
				s.add(m.getDeclaringClass().getSimpleName() + '.' + m.getName());
		assertObjectEquals("['DA1.da1','DA2.da2','DB.da1','DB.db','DC.da2','DC.dc','DD.da2','DD.dd']", s);
	}

	static interface DA1 {
		void da1();
	}
	static interface DA2 extends DA1 {
		void da2();
	}
	static interface DA3 {}
	static interface DA4 {}
	static abstract class DB implements DA1, DA2 {
		@Override
		public void da1() {}
		public void db() {}
	}
	static class DC extends DB implements DA3 {
		@Override
		public void da2() {}
		public void dc() {}
	}
	static class DD extends DC {
		@Override
		public void da2() {}
		public void dd() {}
	}

	//====================================================================================================
	// getAllFieldsParentFirst()
	//====================================================================================================
	@Test
	public void getParentFieldsParentFirst() throws Exception {
		Set<String> s = new TreeSet<String>();
		for (Field f : ClassUtils.getAllFields(EB.class, true)) {
			if (! f.getName().startsWith("$"))
				s.add(f.getDeclaringClass().getSimpleName() + '.' + f.getName());
		}
		assertObjectEquals("['EA.a1','EB.a1','EB.b1']", s);

		s = new TreeSet<String>();
		for (Field f : ClassUtils.getAllFields(EB.class, false)) {
			if (! f.getName().startsWith("$"))
				s.add(f.getDeclaringClass().getSimpleName() + '.' + f.getName());
		}
		assertObjectEquals("['EA.a1','EB.a1','EB.b1']", s);
	}

	static class EA {
		int a1;
	}
	static class EB extends EA {
		int a1;
		int b1;
	}
}
