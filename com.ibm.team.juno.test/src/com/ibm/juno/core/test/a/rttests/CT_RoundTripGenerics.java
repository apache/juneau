/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public class CT_RoundTripGenerics extends RoundTripTest {

	public CT_RoundTripGenerics(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testBeansWithUnboundTypeVars
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void testBeansWithUnboundTypeVars() throws Exception {

		if (returnOriginalObject)
			return;

		// Unbound type variables should be interpreted as Object.
		// During parsing, these become ObjectMaps.
		Pair pair = new Pair<Source,Target>(new Source().init(), new Target().init());
		pair = roundTrip(pair);
		assertObjectEquals("{s:{s1:'a1'},t:{t1:'b1'}}", pair);
		assertEquals("ObjectMap", pair.getS().getClass().getSimpleName());
		assertEquals("ObjectMap", pair.getT().getClass().getSimpleName());

		// If you specify a concrete class, the type variables become bound and
		// the property types correctly resolve.
		pair = roundTrip(pair, RealPair.class);
		assertObjectEquals("{s:{s1:'a1'},t:{t1:'b1'}}", pair);
		assertEquals("Source", pair.getS().getClass().getSimpleName());
		assertEquals("Target", pair.getT().getClass().getSimpleName());
	}

	// Class with unbound type variables.
	public static class Pair<S,T> {
		private S s;
		private T t;

		public Pair() {}

		public Pair(S s, T t) {
			this.s = s;
			this.t = t;
		}

		// Getters/setters
		public S getS() { return s; }
		public void setS(S s) { this.s = s; }
		public T getT() { return t; }
		public void setT(T t) { this.t = t; }
	}

	// Sublcass with bound type variables.
	public static class RealPair extends Pair<Source,Target> {}

	public static class Source {
		public String s1;
		public Source init() {
			this.s1 = "a1";
			return this;
		}
	}

	public static class Target {
		public String t1;
		public Target init() {
			this.t1 = "b1";
			return this;
		}
	}
}
