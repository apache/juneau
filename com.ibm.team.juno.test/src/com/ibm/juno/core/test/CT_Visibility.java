/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static com.ibm.juno.core.BeanContextProperties.*;
import static com.ibm.juno.core.Visibility.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.json.*;
import com.ibm.juno.core.test.a.*;

public class CT_Visibility {

	//====================================================================================================
	// testVisibility
	//====================================================================================================
	@Test
	public void testClassDefault() throws Exception {
		JsonSerializer s1 = JsonSerializer.DEFAULT_LAX.clone().setProperty(BEAN_beansRequireSomeProperties, "false");
		JsonSerializer s2 = JsonSerializer.DEFAULT_LAX.clone().setProperty(BEAN_beansRequireSomeProperties, "false").setProperty(BEAN_beanClassVisibility, PROTECTED);
		JsonSerializer s3 = JsonSerializer.DEFAULT_LAX.clone().setProperty(BEAN_beansRequireSomeProperties, "false").setProperty(BEAN_beanClassVisibility, DEFAULT);
		JsonSerializer s4 = JsonSerializer.DEFAULT_LAX.clone().setProperty(BEAN_beansRequireSomeProperties, "false").setProperty(BEAN_beanClassVisibility, PRIVATE);

		A1 a1 = A1.create();
		String r;

		s1.setProperty(BEAN_beanFieldVisibility, NONE);
		s2.setProperty(BEAN_beanFieldVisibility, NONE);
		s3.setProperty(BEAN_beanFieldVisibility, NONE);
		s4.setProperty(BEAN_beanFieldVisibility, NONE);

		r = s1.serialize(a1);
		assertEquals("{f5:5}", r);

		r = s2.serialize(a1);
		assertEquals("{f5:5}", r);

		r = s3.serialize(a1);
		assertEquals("{f5:5}", r);

		r = s4.serialize(a1);
		assertEquals("{f5:5}", r);

		s1.setProperty(BEAN_beanFieldVisibility, PUBLIC);
		s2.setProperty(BEAN_beanFieldVisibility, PUBLIC);
		s3.setProperty(BEAN_beanFieldVisibility, PUBLIC);
		s4.setProperty(BEAN_beanFieldVisibility, PUBLIC);

		r = s1.serialize(a1);
		assertEquals("{f1:1,a2:{f1:1,f5:5},a3:'A3',a4:'A4',a5:'A5',f5:5}", r);

		r = s2.serialize(a1);
		assertEquals("{f1:1,a2:{f1:1,f5:5},a3:{f1:1,f5:5},a4:'A4',a5:'A5',f5:5}", r);

		r = s3.serialize(a1);
		assertEquals("{f1:1,a2:{f1:1,f5:5},a3:{f1:1,f5:5},a4:{f1:1,f5:5},a5:'A5',f5:5}", r);

		r = s4.serialize(a1);
		assertEquals("{f1:1,a2:{f1:1,f5:5},a3:{f1:1,f5:5},a4:{f1:1,f5:5},a5:{f1:1,f5:5},f5:5}", r);

		s1.setProperty(BEAN_beanFieldVisibility, PROTECTED);
		s2.setProperty(BEAN_beanFieldVisibility, PROTECTED);
		s3.setProperty(BEAN_beanFieldVisibility, PROTECTED);
		s4.setProperty(BEAN_beanFieldVisibility, PROTECTED);

		r = s1.serialize(a1);
		assertEquals("{f1:1,f2:2,a2:{f1:1,f2:2,f5:5},a3:'A3',a4:'A4',a5:'A5',f5:5}", r);

		r = s2.serialize(a1);
		assertEquals("{f1:1,f2:2,a2:{f1:1,f2:2,f5:5},a3:{f1:1,f2:2,f5:5},a4:'A4',a5:'A5',f5:5}", r);

		r = s3.serialize(a1);
		assertEquals("{f1:1,f2:2,a2:{f1:1,f2:2,f5:5},a3:{f1:1,f2:2,f5:5},a4:{f1:1,f2:2,f5:5},a5:'A5',f5:5}", r);

		r = s4.serialize(a1);
		assertEquals("{f1:1,f2:2,a2:{f1:1,f2:2,f5:5},a3:{f1:1,f2:2,f5:5},a4:{f1:1,f2:2,f5:5},a5:{f1:1,f2:2,f5:5},f5:5}", r);

		s1.setProperty(BEAN_beanFieldVisibility, DEFAULT);
		s2.setProperty(BEAN_beanFieldVisibility, DEFAULT);
		s3.setProperty(BEAN_beanFieldVisibility, DEFAULT);
		s4.setProperty(BEAN_beanFieldVisibility, DEFAULT);

		r = s1.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,a2:{f1:1,f2:2,f3:3,f5:5},a3:'A3',a4:'A4',a5:'A5',f5:5}", r);

		r = s2.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,a2:{f1:1,f2:2,f3:3,f5:5},a3:{f1:1,f2:2,f3:3,f5:5},a4:'A4',a5:'A5',f5:5}", r);

		r = s3.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,a2:{f1:1,f2:2,f3:3,f5:5},a3:{f1:1,f2:2,f3:3,f5:5},a4:{f1:1,f2:2,f3:3,f5:5},a5:'A5',f5:5}", r);

		r = s4.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,a2:{f1:1,f2:2,f3:3,f5:5},a3:{f1:1,f2:2,f3:3,f5:5},a4:{f1:1,f2:2,f3:3,f5:5},a5:{f1:1,f2:2,f3:3,f5:5},f5:5}", r);

		s1.setProperty(BEAN_beanFieldVisibility, PRIVATE);
		s2.setProperty(BEAN_beanFieldVisibility, PRIVATE);
		s3.setProperty(BEAN_beanFieldVisibility, PRIVATE);
		s4.setProperty(BEAN_beanFieldVisibility, PRIVATE);

		r = s1.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5},a3:'A3',a4:'A4',a5:'A5',f5:5}", r);

		r = s2.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5},a3:{f1:1,f2:2,f3:3,f4:4,f5:5},a4:'A4',a5:'A5',f5:5}", r);

		r = s3.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5},a3:{f1:1,f2:2,f3:3,f4:4,f5:5},a4:{f1:1,f2:2,f3:3,f4:4,f5:5},a5:'A5',f5:5}", r);

		r = s4.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5},a3:{f1:1,f2:2,f3:3,f4:4,f5:5},a4:{f1:1,f2:2,f3:3,f4:4,f5:5},a5:{f1:1,f2:2,f3:3,f4:4,f5:5},f5:5}", r);

		s1.setProperty(BEAN_methodVisibility, NONE);
		s2.setProperty(BEAN_methodVisibility, NONE);
		s3.setProperty(BEAN_methodVisibility, NONE);
		s4.setProperty(BEAN_methodVisibility, NONE);

		r = s1.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4},a3:'A3',a4:'A4',a5:'A5'}", r);

		r = s2.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4},a3:{f1:1,f2:2,f3:3,f4:4},a4:'A4',a5:'A5'}", r);

		r = s3.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4},a3:{f1:1,f2:2,f3:3,f4:4},a4:{f1:1,f2:2,f3:3,f4:4},a5:'A5'}", r);

		r = s4.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4},a3:{f1:1,f2:2,f3:3,f4:4},a4:{f1:1,f2:2,f3:3,f4:4},a5:{f1:1,f2:2,f3:3,f4:4}}", r);

		s1.setProperty(BEAN_methodVisibility, PROTECTED);
		s2.setProperty(BEAN_methodVisibility, PROTECTED);
		s3.setProperty(BEAN_methodVisibility, PROTECTED);
		s4.setProperty(BEAN_methodVisibility, PROTECTED);

		r = s1.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a3:'A3',a4:'A4',a5:'A5',f5:5,f6:6}", r);

		r = s2.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a3:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a4:'A4',a5:'A5',f5:5,f6:6}", r);

		r = s3.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a3:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a4:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a5:'A5',f5:5,f6:6}", r);

		r = s4.serialize(a1);
		assertEquals("{f1:1,f2:2,f3:3,f4:4,a2:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a3:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a4:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},a5:{f1:1,f2:2,f3:3,f4:4,f5:5,f6:6},f5:5,f6:6}", r);

	}

	static class A {
		public int f1;
		public A(){}

		static A create() {
			A x = new A();
			x.f1 = 1;
			return x;
		}
	}
}