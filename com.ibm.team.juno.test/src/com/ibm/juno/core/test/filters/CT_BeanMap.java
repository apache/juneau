/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test.filters;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.filters.*;

public class CT_BeanMap {
	
	//====================================================================================================
	// testFilteredEntry
	//====================================================================================================
	@Test
	public void testFilteredEntry() throws Exception {		
		BeanContext bc = new BeanContextFactory().addFilters(ByteArrayBase64Filter.class).getBeanContext();
		BeanMap<A> m = bc.forBean(new A());
		
		assertEquals("AQID", m.get("f1"));
		m.put("f1", "BAUG");
		assertEquals("BAUG", m.get("f1"));
		assertEquals(4, m.getBean().f1[0]);
		
		assertNull(m.get("f3"));
	}

	public static class A {
		public byte[] f1 = new byte[]{1,2,3};
		public byte[] f3 = null;
	}	
	
	//====================================================================================================
	// testFilteredEntryWithMultipleMatchingFilters
	// When bean properties can have multiple filters applied to them, pick the first match.
	//====================================================================================================
	@Test
	public void testFilteredEntryWithMultipleMatchingFilters() throws Exception {		
		BeanContext bc = new BeanContextFactory().addFilters(B2Filter.class,B1Filter.class).getBeanContext();
		BeanMap<B> bm = bc.forBean(B.create());
		ObjectMap om = (ObjectMap)bm.get("b1");
		assertEquals("b2", om.getString("type"));

		bc = new BeanContextFactory().addFilters(B1Filter.class,B2Filter.class).getBeanContext();
		bm = bc.forBean(B.create());
		om = (ObjectMap)bm.get("b1");
		assertEquals("b1", om.getString("type"));
	}	
	
	
	public static class B {
		public B1 b1;
		
		static B create() {
			B b = new B();
			B2 b2 = new B2();
			b2.f1 = "f1";
			b2.f2 = "f2";
			b.b1 = b2;
			return b;
		}
	}
	
	public static class B1 {
		public String f1;
	}
	
	public static class B2 extends B1 {
		public String f2;
	}
	
	public static class B1Filter extends PojoFilter<B1,ObjectMap> {
		@Override /* PojoFilter */
		public ObjectMap filter(B1 b1) {
			return new ObjectMap().append("type", "b1").append("f1", b1.f1);
		}
	}

	public static class B2Filter extends PojoFilter<B2,ObjectMap> {
		@Override /* PojoFilter */
		public ObjectMap filter(B2 b2) {
			return new ObjectMap().append("type", "b2").append("f1", b2.f1);
		}
	}
}