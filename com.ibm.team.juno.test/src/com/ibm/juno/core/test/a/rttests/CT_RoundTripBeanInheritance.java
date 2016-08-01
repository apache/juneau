/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;



/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public class CT_RoundTripBeanInheritance extends RoundTripTest {
	
	public CT_RoundTripBeanInheritance(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);	
	}
	
	//====================================================================================================
	// testBeanInheritance
	//====================================================================================================
	@Test
	public void testBeanInheritance() throws Exception {
		
		// Skip tests that just return the same object.
		if (returnOriginalObject)
			return;
		
		A2 t1 = new A2(), t2;
		t1.init();
		t2 = roundTrip(t1, A2.class);
		assertEqualObjects(t1, t2);

		A3 t3 = new A3();
		t3.init();
		try {
			ClassMeta<?> cm = BeanContext.DEFAULT.getClassMeta(A3.class);
			assertEquals("No properties detected on bean class", cm.getNotABeanReason());
			roundTrip(t3, A3.class);
			fail("Exception expected");
		} catch (ParseException e) {
		} catch (SerializeException e) {
		} catch (InvalidDataConversionException e) {}
	}
		
		
	public static abstract class A1 {
		protected String x = null;
		protected String y = null;
		protected String z = null;

		public A1() {
			this.x = null;
			this.y = null;
			this.z = null;
		}

		public A1(String x, String y, String z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public void setX(String x) {
			this.x = x;
		}

		public void setY(String y) {
			this.y = y;
		}

		public void setZ(String z) {
			this.z = z;
		}

		@Override /* Object */
		public String toString() {
			return ("A1(x: " + this.x + ", y: " + this.y + ", z: " + this.z + ")");
		}
		
		public A1 init() {
			x = null;
			y = "";
			z = "z";
			return this;
		}
	}

	public static class A2 extends A1 {
		public A2() {
			super();
		}

		public A2(String x, String y, String z) {
			super(x, y, z);
		}

		public String getX() {
			return this.x;
		}

		public String getY() {
			return this.y;
		}

		public String getZ() {
			return this.z;
		}
	}

	// This is not supposed to be a valid bean since it has no getters defined.
	public static class A3 extends A1 {
		public A3() {
			super();
		}

		public A3(String x, String y, String z) {
			super(x, y, z);
		}

		public String isX() {
			throw new RuntimeException("Should not be called!");
		}

		public String isY() {
			throw new RuntimeException("Should not be called!");
		}

		public String isZ() {
			throw new RuntimeException("Should not be called!");
		}
	}
	
	//====================================================================================================
	// testBeanInheritance2
	//====================================================================================================
	@Test
	public void testBeanInheritance2() throws Exception {
		B1 t1 = new B1().init(), t2;
		t2 = roundTrip(t1, B1.class);
		assertEqualObjects(t1, t2);
	}

	public static class B1 extends B2 {
		private A2 f4;

		public A2 getF4() {
			return this.f4;
		}

		public void setF4(A2 f4) {
			this.f4 = f4;
		}

		@Override /* Object */
		public String toString() {
			return super.toString() + " / " + this.f4;
		}
		
		public B1 init() {
			setF1("A1");
			setF2(101);
			setF3(false);
			setF4((A2)new A2().init());
			return this;
		}
	}	

	public static class B2 {
		private String f1 = null;
		private int f2 = -1;
		private boolean f3 = false;

		public String getF1() {
			return this.f1;
		}

		public void setF1(String f1) {
			this.f1 = f1;
		}

		public int getF2() {
			return this.f2;
		}

		public void setF2(int f2) {
			this.f2 = f2;
		}

		public boolean isF3() {
			return this.f3;
		}

		public void setF3(boolean f3) {
			this.f3 = f3;
		}

		@Override /* Object */
		public String toString() {
			return ("B2(f1: " + this.getF1() + ", f2: " + this.getF2() + ")");
		}
	}
}
