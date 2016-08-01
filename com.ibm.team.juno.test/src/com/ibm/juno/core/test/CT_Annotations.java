/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;


@SuppressWarnings({"rawtypes"})
public class CT_Annotations {

	//====================================================================================================
	// Bean with explicitly specified properties.
	//====================================================================================================
	@Test
	public void testBeanWithExplicitProperties() throws Exception {		
		BeanContext bc = BeanContext.DEFAULT;
		BeanMap bm = null;
		
		// Basic test
		bm = bc.newBeanMap(Person1.class).load("{age:21,name:'foobar'}");
		assertNotNull(bm);
		assertNotNull(bm.getBean());
		assertEquals(bm.get("age"), 21);
		assertEquals(bm.get("name"), "foobar");
		
		bm.put("age", 65);
		bm.put("name", "futbol");
		assertEquals(bm.get("age"), 65);
		assertEquals(bm.get("name"), "futbol");
	}

	/** Class with explicitly specified properties */
	@Bean(properties = { "age", "name" })
	public static class Person1 {
		public int age;
		private String name;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}

	//====================================================================================================
	// Private/protected/default fields should be ignored.
	//====================================================================================================
	@Test
	public void testForOnlyPublicFields() throws Exception {
		BeanContext bc = BeanContext.DEFAULT;
		BeanMap bm = null;

		// Make sure only public fields are detected
		bm = bc.newBeanMap(A.class).load("{publicField:123}");
		assertNotNull("F1", bm);
		assertNotNull("F2", bm.getBean());
		assertObjectEquals("{publicField:123}", bm.getBean());

	}

	public static class A {
		public int publicField;
		protected int protectedField;
		@SuppressWarnings("unused")
		private int privateField;
		int defaultField;
	}
}