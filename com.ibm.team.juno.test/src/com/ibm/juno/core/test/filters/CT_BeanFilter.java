/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test.filters;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;

public class CT_BeanFilter {
	
	//====================================================================================================
	// Filtered interfaces
	//====================================================================================================
	@Test
	public void testFilteredInterfaces() throws Exception {		
		BeanContext bc;
		BeanMap<A3> bm;
		
		bc = new BeanContextFactory().addFilters(A1.class).getBeanContext();
		bm = bc.newBeanMap(A3.class);
		assertEquals("f1", bm.get("f1"));
		assertNull(bm.get("f2"));
		assertNull(bm.get("f3"));

		bc = new BeanContextFactory().addFilters(A2.class).getBeanContext();
		bm = bc.newBeanMap(A3.class);
		assertEquals("f1", bm.get("f1"));
		assertEquals("f2", bm.get("f2"));
		assertNull(bm.get("f3"));
	}
	
	public static interface A1 {
		public String getF1();
	}	

	public static interface A2 extends A1 {
		public String getF2();
	}	

	public static class A3 implements A2 {
		@Override /* A1 */
		public String getF1() {
			return "f1";
		}
		@Override /* A2 */
		public String getF2() {
			return "f2";
		}
		public String getF3() {
			return "f3";
		}
	}	
	
	//====================================================================================================
	// Filtered abstract classes
	//====================================================================================================
	@Test
	public void testFilteredAbstractClasses() throws Exception {		
		BeanContext bc;
		BeanMap<Test2> bm;
		
		bc = new BeanContextFactory().addFilters(B1.class).getBeanContext();
		bm = bc.newBeanMap(Test2.class);
		assertEquals("f1", bm.get("f1"));
		assertNull(bm.get("f2"));
		assertNull(bm.get("f3"));

		bc = new BeanContextFactory().addFilters(B2.class).getBeanContext();
		bm = bc.newBeanMap(Test2.class);
		assertEquals("f1", bm.get("f1"));
		assertEquals("f2", bm.get("f2"));
		assertNull(bm.get("f3"));
	}
	
	public abstract static class B1 {
		public abstract String getF1();
	}	

	public abstract static class B2 extends B1 {
		@Override /* B1 */
		public abstract String getF1();
		public abstract String getF2();
	}	

	public static class Test2 extends B2 {
		@Override /* B1 */
		public String getF1() {
			return "f1";
		}
		@Override /* B2 */
		public String getF2() {
			return "f2";
		}
		public String getF3() {
			return "f3";
		}
	}	
	
	//====================================================================================================
	// Filtered with stop classes
	//====================================================================================================
	@Test
	public void testFilteredWithStopClass() throws Exception {	
		C3 c3 = new C3();
		assertObjectEquals("{f3:3,p3:3}", c3);
	}

	public class C1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}
	
	public class C2 extends C1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}
	
	@Bean(stopClass=C2.class)
	public class C3 extends C2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}
	
	@Test
	public void testFilteredWithStopClassOnParentClass() throws Exception {	
		D3 d3 = new D3();
		assertObjectEquals("{f3:3,p3:3}", d3);
	}

	public class D1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}
	
	@Bean(stopClass=D2.class)
	public class D2 extends D1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}
	
	public class D3 extends D2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}

	@Test
	public void testFilteredWithStopClassOnParentClassWithOverriddenAnnotation() throws Exception {	
		E3 e3 = new E3();
		assertObjectEquals("{f3:3,p3:3}", e3);
	}

	public class E1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}
	
	@Bean(stopClass=E2.class)
	public class E2 extends E1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}
	
	@Bean(excludeProperties={"foo"})
	public class E3 extends E2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}

	@Test
	public void testFilteredWithStopClassesAtMulitpleLevels() throws Exception {	
		F3 e3 = new F3();
		assertObjectEquals("{f3:3,p3:3}", e3);
	}

	@Bean(stopClass=F1.class)
	public class F1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}
	
	public class F2 extends F1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}
	
	@Bean(stopClass=F2.class)
	public class F3 extends F2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}
}