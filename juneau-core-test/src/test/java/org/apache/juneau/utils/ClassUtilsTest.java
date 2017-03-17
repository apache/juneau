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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.junit.Assert.*;

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
}
