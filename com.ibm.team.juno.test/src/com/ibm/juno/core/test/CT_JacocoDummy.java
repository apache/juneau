/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.test;

import java.lang.reflect.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.ini.*;
import com.ibm.juno.core.jena.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.annotation.*;


public class CT_JacocoDummy {

	//====================================================================================================
	// Dummy code to add test coverage in Jacoco.
	//====================================================================================================
	@Test
	public void accessPrivateConstructorsOnStaticUtilityClasses() throws Exception {

		Class<?>[] classes = new Class[] {
			StringUtils.class, ArrayUtils.class, ClassUtils.class, CollectionUtils.class, ConfigUtils.class
		};

		for (Class<?> c : classes) {
			Constructor<?> c1 = c.getDeclaredConstructor();
			c1.setAccessible(true);
			c1.newInstance();
		}

		ConfigFileFormat.valueOf(ConfigFileFormat.INI.toString());
		Filter.FilterType.valueOf(Filter.FilterType.POJO.toString());
		RdfCollectionFormat.valueOf(RdfCollectionFormat.DEFAULT.toString());
		XmlFormat.valueOf(XmlFormat.NORMAL.toString());
		Visibility.valueOf(Visibility.DEFAULT.toString());
	}
}
