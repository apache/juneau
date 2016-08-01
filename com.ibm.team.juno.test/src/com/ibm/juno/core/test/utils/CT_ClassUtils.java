/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static com.ibm.juno.core.utils.ClassUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

public class CT_ClassUtils {

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
}
