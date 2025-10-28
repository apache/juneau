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
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests to ensure the valueOf(String), fromString(String), parse(String), and parseString(String) methods
 * are used correctly by parsers.
 */
class ObjectsAsStrings_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// testBasic
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_basic(RoundTrip_Tester t) throws Exception {
		var x = new A().init();
		x = t.roundTrip(x);
		assertBean(x, "a1{f},a2{f},a3{f},a4{f}", "{1},{2},{3},{4}");
	}

	public static class A {
		public A1 a1;
		public A2 a2;
		public A3 a3;
		public A4 a4;

		public A init() {
			a1 = new A1();
			a1.f = "1";
			a2 = new A2();
			a2.f = "2";
			a3 = new A3();
			a3.f = "3";
			a4 = new A4();
			a4.f = "4";
			return this;
		}
	}

	public static class A1 {
		public String f;
		public static A1 fromString(String s) {
			var x = new A1();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "A1-" + f;
		}
	}

	public static class A2 {
		public String f;
		public static A2 valueOf(String s) {
			var x = new A2();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "A2-" + f;
		}
	}

	public static class A3 {
		public String f;
		public static A3 parse(String s) {
			var x = new A3();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "A3-" + f;
		}
	}

	public static class A4 {
		public String f;
		public static A4 parseString(String s) {
			var x = new A4();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "A4-" + f;
		}
	}

	//====================================================================================================
	// testEnumWithOverriddenStringValue
	// The B1 enum should serialize as "X1" but the B2 enum should serialize as "X-1".
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_enumWithOverriddenStringValue(RoundTrip_Tester t) throws Exception {
		var x = new B().init();
		if (! t.returnOriginalObject) {
			var r = t.getSerializer().serialize(x);
			assertTrue(toString(r).contains("X-2"));
		}
		x = t.roundTrip(x);
		assertBean(x, "b1,b2", "X1,X2");
	}

	public static class B {
		public B1 b1;
		public B2 b2;

		public B init() {
			b1 = B1.X1;
			b2 = B2.X2;
			return this;
		}

	}

	public enum B1 {
		X1(1),
		X2(2),
		X3(3);

		@SuppressWarnings("unused")
		private int i;
		B1(int i) {
			this.i = i;
		}
	}

	public enum B2 {
		X1(1),
		X2(2),
		X3(3);

		private int i;
		B2(int i) {
			this.i = i;
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "X-" + i;
		}

		public static B2 fromString(String s) {
			return valueOf("X" + s.substring(2));
		}
	}

	//====================================================================================================
	// testMethodOrdering
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_ordering(RoundTrip_Tester t) throws Exception {
		var x = new C().init();
		x = t.roundTrip(x);
		assertJson("{c1:{f:'1'},c2:{f:'2'},c3:{f:'3'},c4:{f:'4'}}", x);
	}

	public static class C {
		public C1 c1;
		public C2 c2;
		public C3 c3;
		public C4 c4;

		public C init() {
			c1 = new C1();
			c1.f = "1";
			c2 = new C2();
			c2.f = "2";
			c3 = new C3();
			c3.f = "3";
			c4 = new C4();
			c4.f = "4";
			return this;
		}
	}

	public static class C1 {
		public String f;
		public static C2 valueOf(String s) {
			throw new IllegalCallerException("Shouldn't be called!");
		}
		public static C2 parse(String s) {
			throw new IllegalCallerException("Shouldn't be called!");
		}
		public static C2 parseString(String s) {
			throw new IllegalCallerException("Shouldn't be called!");
		}
		public static C1 fromString(String s) {
			var x = new C1();
			x.f = s.substring(3);
			return x;
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "C1-" + f;
		}
	}

	public static class C2 {
		public String f;
		public static C2 parse(String s) {
			throw new IllegalCallerException("Shouldn't be called!");
		}
		public static C2 parseString(String s) {
			throw new IllegalCallerException("Shouldn't be called!");
		}
		public static C2 valueOf(String s) {
			var x = new C2();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "C2-" + f;
		}
	}

	public static class C3 {
		public String f;
		public static C2 parseString(String s) {
			throw new IllegalCallerException("Shouldn't be called!");
		}
		public static C3 parse(String s) {
			var x = new C3();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "C3-" + f;
		}
	}

	public static class C4 {
		public String f;
		public static C4 parseString(String s) {
			var x = new C4();
			x.f = s.substring(3);
			return x;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "C4" + f;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//------------------------------------------------------------------------------------------------------------------

	private static final String toString(Object o) {
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		if (o instanceof byte[])
			return new String((byte[])o, UTF8);
		return o.toString();
	}
}