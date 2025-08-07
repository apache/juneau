// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.a.rttests;

import static org.apache.juneau.AssertionHelpers.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripGenericsTest extends RoundTripTest {

	public RoundTripGenericsTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
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
		// During parsing, these become JsonMaps.
		Pair pair = new Pair<>(new Source().init(), new Target().init());
		pair = roundTrip(pair);
		assertJson(pair, "{s:{s1:'a1'},t:{t1:'b1'}}");
		assertEquals("JsonMap", pair.getS().getClass().getSimpleName());
		assertEquals("JsonMap", pair.getT().getClass().getSimpleName());

		// If you specify a concrete class, the type variables become bound and
		// the property types correctly resolve.
		pair = roundTrip(pair, RealPair.class);
		assertJson(pair, "{s:{s1:'a1'},t:{t1:'b1'}}");
		assertEquals("Source", pair.getS().getClass().getSimpleName());
		assertEquals("Target", pair.getT().getClass().getSimpleName());
	}

	// Class with unbound type variables.
	@Bean(p="s,t")
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