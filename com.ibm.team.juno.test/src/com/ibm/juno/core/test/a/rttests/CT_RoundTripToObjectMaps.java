/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;



/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public class CT_RoundTripToObjectMaps extends RoundTripTest {

	public CT_RoundTripToObjectMaps(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// Class with X(ObjectMap) constructor and toObjectMap() method.
	//====================================================================================================
	@SuppressWarnings({ "serial", "unchecked" })
	@Test
	public void test() throws Exception {
		A a = new A(new ObjectMap("{f1:'a',f2:2}"));
		a = roundTrip(a, A.class);
		assertEquals("a", a.f1);
		assertEquals(2, a.f2);

		A[] aa = new A[]{a};
		aa = roundTrip(aa, A[].class);
		assertEquals(1, aa.length);
		assertEquals("a", aa[0].f1);
		assertEquals(2, aa[0].f2);

		List<A> a2 = new ArrayList<A>(){{add(new A(new ObjectMap("{f1:'a',f2:2}")));}};
		a2 = roundTrip(a2, BeanContext.DEFAULT.getCollectionClassMeta(List.class, A.class));
		assertEquals(1, a2.size());
		assertEquals("a", a2.get(0).f1);
		assertEquals(2, a2.get(0).f2);

		Map<String,A> a3 = new LinkedHashMap<String,A>(){{put("a", new A(new ObjectMap("{f1:'a',f2:2}")));}};
		a3 = roundTrip(a3, BeanContext.DEFAULT.getMapClassMeta(Map.class, String.class, A.class));
		assertEquals(1, a3.size());
		assertEquals("a", a3.get("a").f1);
		assertEquals(2, a3.get("a").f2);
	}

	public static class A {
		private String f1;
		private int f2;
		public A(ObjectMap m) {
			this.f1 = m.getString("f1");
			this.f2 = m.getInt("f2");
		}
		public ObjectMap toObjectMap() {
			return new ObjectMap().append("f1",f1).append("f2",f2);
		}
	}

}
