/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;



/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public class CT_RoundTripObjectsWithSpecialMethods extends RoundTripTest {

	public CT_RoundTripObjectsWithSpecialMethods(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// @NameProperty method.
	//====================================================================================================
	@Test
	public void testNameProperty() throws Exception {
		A t = new A().init();
		t = roundTrip(t);
		assertObjectEquals("{a2:{f2:2},m:{k1:{f2:2}}}", t);
		if (isValidationOnly())
			return;
		assertEquals("a2", t.a2.name);
		assertEquals("k1", t.m.get("k1").name);
	}

	public static class A {
		public A2 a2;
		public Map<String,A2> m;

		A init() {
			a2 = new A2().init();
			m = new LinkedHashMap<String,A2>();
			m.put("k1", new A2().init());
			return this;
		}

	}
	public static class A2 {
		String name;
		public int f2;

		@NameProperty
		protected void setName(String name) {
			this.name = name;
		}

		A2 init() {
			f2 = 2;
			return this;
		}
	}

	//====================================================================================================
	// @ParentProperty method.
	//====================================================================================================
	@Test
	public void testParentProperty() throws Exception {
		B t = new B().init();
		t = roundTrip(t);
		if (isValidationOnly())
			return;
		assertEquals(t.f1, t.b2.parent.f1);
	}

	public static class B {
		public int f1;
		public B2 b2;

		B init() {
			f1 = 1;
			b2 = new B2().init();
			return this;
		}

	}
	public static class B2 {
		B parent;
		public int f2;

		@ParentProperty
		protected void setParent(B parent) {
			this.parent = parent;
		}

		B2 init() {
			f2 = 2;
			return this;
		}
	}
}
