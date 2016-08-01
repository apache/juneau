/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static com.ibm.juno.core.BeanContextProperties.*;
import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.core.json.*;

public class CT_IgnoredClasses {

	//====================================================================================================
	// testFilesRenderedAsStrings
	//====================================================================================================
	@Test
	public void testFilesRenderedAsStrings() throws Exception {
		assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("win"));
		// Files should be rendered as strings.
		File f = new File("C:/temp");
		assertObjectEquals("'C:\\\\temp'", f);
	}

	//====================================================================================================
	// testIgnorePackages
	//====================================================================================================
	@Test
	public void testIgnorePackages() throws Exception {
		A a = new A();
		JsonSerializer s = new JsonSerializer.Simple();
		assertEquals("{f1:'isBean'}", s.serialize(a));
		s.setProperty(BEAN_addNotBeanPackages, "com.ibm.juno.core.test");
		assertEquals("'isNotBean'", s.serialize(a));
		s.setProperty(BEAN_removeNotBeanPackages, "com.ibm.juno.core.test");
		assertEquals("{f1:'isBean'}", s.serialize(a));
		s.setProperty(BEAN_addNotBeanPackages, "com.ibm.juno.core.test.*");
		assertEquals("'isNotBean'", s.serialize(a));
		s.setProperty(BEAN_removeNotBeanPackages, "com.ibm.juno.core.test.*");
		assertEquals("{f1:'isBean'}", s.serialize(a));
		s.setProperty(BEAN_addNotBeanPackages, "com.ibm.juno.*");
		assertEquals("'isNotBean'", s.serialize(a));
		s.setProperty(BEAN_removeNotBeanPackages, "com.ibm.juno.*");
		assertEquals("{f1:'isBean'}", s.serialize(a));
		s.setProperty(BEAN_addNotBeanPackages, "com.ibm.juno");
		assertEquals("{f1:'isBean'}", s.serialize(a));
		s.setProperty(BEAN_addNotBeanPackages, "com.ibm.juno.core.test.x");
		assertEquals("{f1:'isBean'}", s.serialize(a));
	}

	public static class A {
		public String f1 = "isBean";
		@Override /* Object */
		public String toString() {
			return "isNotBean";
		}
	}
	// TODO - Ignored packages.
}
