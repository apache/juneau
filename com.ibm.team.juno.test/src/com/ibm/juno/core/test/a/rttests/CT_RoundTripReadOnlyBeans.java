/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public class CT_RoundTripReadOnlyBeans extends RoundTripTest {
	
	public CT_RoundTripReadOnlyBeans(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);	
	}

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		B t1 = new B(1, "a"), t2 = new B(2, "b");
		A t3 = new A(t1, t2);

		t3 = roundTrip(t3, A.class);
		assertEquals(1, t3.getF1().getF1());
		assertEquals("a", t3.getF1().getF2());
		assertEquals(2, t3.getF2().getF1());
		assertEquals("b", t3.getF2().getF2());
	}
	
	public static class A {
		private B f1;
		private final B f2;
		
		@BeanConstructor(properties={"f2"})
		public A(B f2) {
			this.f2 = f2;
		}

		public A(B f1, B f2) {
			this.f1 = f1;
			this.f2 = f2;
		}
	
		public B getF1() {
			return f1;
		}
		
		public void setF1(B f1) {
			this.f1 = f1;
		}
		
		public B getF2() {
			return f2;
		}
	}
	
	public static class B {
		private int f1;
		private final String f2;
		
		@BeanConstructor(properties={"f2"})
		public B(String sField) {
			this.f2 = sField;
		}

		public B(int iField, String sField) {
			this.f1 = iField;
			this.f2 = sField;
		}
	
		public int getF1() {
			return f1;
		}
		
		public void setF1(int f1) {
			this.f1 = f1;
		}
		
		public String getF2() {
			return f2;
		}
	}
}
