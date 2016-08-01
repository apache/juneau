/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;


import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testFilters",
	filters={TestFilters.FilterA2.class}
)
public class TestFilters extends TestFiltersParent {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test class filter overrides parent class filter
	// Should return "A2-1".
	//====================================================================================================
	@RestMethod(name="GET", path="/testClassFilterOverridesParentClassFilter")
	public A testClassFilterOverridesParentClassFilter() {
		return new A();
	}
	@RestMethod(name="PUT", path="/testClassFilterOverridesParentClassFilter")
	public A test1b(@Content A a) {
		return a;
	}
	@RestMethod(name="PUT", path="/testClassFilterOverridesParentClassFilter/{a}")
	public A test1c(@Attr A a) {
		return a;
	}

	//====================================================================================================
	// Test method filter overrides class filter
	// Should return "A3-1".
	//====================================================================================================
	@RestMethod(name="GET", path="/testMethodFilterOverridesClassFilter", filters={FilterA3.class})
	public A test2a() {
		return new A();
	}
	@RestMethod(name="PUT", path="/testMethodFilterOverridesClassFilter", filters={FilterA3.class})
	public A test2b(@Content A a) {
		return a;
	}
	@RestMethod(name="PUT", path="/testMethodFilterOverridesClassFilter/{a}", filters={FilterA3.class})
	public A test2c(@Attr A a) {
		return a;
	}


	public static class A {
		public int f1;
	}

	public static class FilterA1 extends PojoFilter<A,String> {
		@Override /* PojoFilter */
		public String filter(A a) throws SerializeException {
			return "A1-" + a.f1;
		}
		@Override /* PojoFilter */
		public A unfilter(String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A1"))
				throw new RuntimeException("Invalid input for FilterA1!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class FilterA2 extends PojoFilter<A,String> {
		@Override /* PojoFilter */
		public String filter(A a) throws SerializeException {
			return "A2-" + a.f1;
		}
		@Override /* PojoFilter */
		public A unfilter(String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A2"))
				throw new RuntimeException("Invalid input for FilterA2!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class FilterA3 extends PojoFilter<A,String> {
		@Override /* PojoFilter */
		public String filter(A a) throws SerializeException {
			return "A3-" + a.f1;
		}
		@Override /* PojoFilter */
		public A unfilter(String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A3"))
				throw new RuntimeException("Invalid input for FilterA3!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}
}
