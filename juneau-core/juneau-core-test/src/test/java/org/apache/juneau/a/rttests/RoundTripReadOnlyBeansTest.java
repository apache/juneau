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
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public class RoundTripReadOnlyBeansTest extends RoundTripTest {

	public RoundTripReadOnlyBeansTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		B t1 = new B(1, "a"), t2 = new B(2, "b");
		A t3 = new A(t1, t2);

		t3 = roundTrip(t3, A.class);
		assertEquals(1, t3.getF1().getF1());
		assertEquals("a", t3.getF1().getF2());
		assertEquals(2, t3.getF2().getF1());
		assertEquals("b", t3.getF2().getF2());
	}

	public static class A {
		private B f1;
		private final B f2;

		@BeanConstructor(properties="f2")
		public A(B f2) {
			this.f2 = f2;
		}

		public A(B f1, B f2) {
			this.f1 = f1;
			this.f2 = f2;
		}

		public B getF1() {
			return f1;
		}

		public void setF1(B f1) {
			this.f1 = f1;
		}

		public B getF2() {
			return f2;
		}
	}

	public static class B {
		private int f1;
		private final String f2;

		@BeanConstructor(properties="f2")
		public B(String sField) {
			this.f2 = sField;
		}

		public B(int iField, String sField) {
			this.f1 = iField;
			this.f2 = sField;
		}

		public int getF1() {
			return f1;
		}

		public void setF1(int f1) {
			this.f1 = f1;
		}

		public String getF2() {
			return f2;
		}
	}
}
