/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.annotation.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class Generics_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// testBeansWithUnboundTypeVars
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_beansWithUnboundTypeVars(RoundTrip_Tester t) throws Exception {

		if (t.returnOriginalObject)
			return;

		// Unbound type variables should be interpreted as Object.
		// During parsing, these become JsonMaps.
		var x = new Pair<Object,Object>(new Source().init(), new Target().init());
		x = t.roundTrip(x);
		assertBean(x, "s{s1},t{t1}", "{a1},{b1}");
		assertEquals("JsonMap", x.getS().getClass().getSimpleName());
		assertEquals("JsonMap", x.getT().getClass().getSimpleName());

		// If you specify a concrete class, the type variables become bound and
		// the property types correctly resolve.
		x = t.roundTrip(x, RealPair.class);
		assertBean(x, "s{s1},t{t1}", "{a1},{b1}");
		assertEquals("Source", x.getS().getClass().getSimpleName());
		assertEquals("Target", x.getT().getClass().getSimpleName());
	}

	// Class with unbound type variables.
	@Bean(p="s,t")
	public static class Pair<S,T> {

		public Pair() {}

		public Pair(S s, T t) {
			this.s = s;
			this.t = t;
		}

		// Getters/setters
		private S s;
		public S getS() { return s; }
		public void setS(S v) { s = v; }

		private T t;
		public T getT() { return t; }
		public void setT(T v) { t = v; }
	}

	// Sublcass with bound type variables.
	public static class RealPair extends Pair<Source,Target> {}

	public static class Source {
		public String s1;
		public Source init() {
			s1 = "a1";
			return this;
		}
	}

	public static class Target {
		public String t1;
		public Target init() {
			t1 = "b1";
			return this;
		}
	}
}