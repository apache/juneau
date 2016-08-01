/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import java.lang.reflect.*;

import org.junit.*;

import com.ibm.juno.server.*;

public class CT_JacocoDummy {

	//====================================================================================================
	// Dummy code to add test coverage in Jacoco.
	//====================================================================================================
	@Test
	public void accessPrivateConstructorsOnStaticUtilityClasses() throws Exception {

		Class<?>[] classes = new Class[] {
			RestUtils.class
		};

		for (Class<?> c : classes) {
			Constructor<?> c1 = c.getDeclaredConstructor();
			c1.setAccessible(true);
			c1.newInstance();
		}
	}
}
