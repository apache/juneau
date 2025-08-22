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

import static org.junit.Assert.*;

import org.apache.juneau.annotation.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class ReadOnlyBeans_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_basic(RoundTripTester t) throws Exception {
		var x1 = new B(1, "a");
		var x2 = new B(2, "b");
		var x3 = new A(x1, x2);

		x3 = t.roundTrip(x3, A.class);
		assertEquals(1, x3.getF1().getF1());
		assertEquals("a", x3.getF1().getF2());
		assertEquals(2, x3.getF2().getF1());
		assertEquals("b", x3.getF2().getF2());
	}

	public static class A {
		private B f1;
		private final B f2;

		@Beanc(properties="f2")
		public A(B f2) {
			this.f2 = f2;
		}

		public A(B f1, B f2) {
			this.f1 = f1;
			this.f2 = f2;
		}

		public B getF1() { return f1; }
		public void setF1(B v) { f1 = v; }

		public B getF2() { return f2; }
	}

	public static class B {
		private int f1;
		private final String f2;

		@Beanc(properties="f2")
		public B(String sField) {
			this.f2 = sField;
		}

		public B(int iField, String sField) {
			this.f1 = iField;
			this.f2 = sField;
		}

		public int getF1() { return f1;}
		public void setF1(int v) { f1 = v; }

		public String getF2() { return f2; }
	}
}