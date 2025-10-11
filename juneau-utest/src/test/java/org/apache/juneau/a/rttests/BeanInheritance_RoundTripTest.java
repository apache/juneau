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

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class BeanInheritance_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// testBeanInheritance
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_beanInheritance(RoundTrip_Tester t) throws Exception {

		// Skip tests that just return the same object.
		if (t.returnOriginalObject)
			return;

		var t1 = new A2().init();
		var t2 = t.roundTrip(t1, A2.class);
		assertEquals(json(t2), json(t1));

		var t3 = new A3().init();
		t.roundTrip(t3, A3.class);
	}


	public abstract static class A1 {
		protected String x;
		protected String y;
		protected String z;

		public A1() {
			this.x = null;
			this.y = null;
			this.z = null;
		}

		public A1(String x, String y, String z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public void setX(String v) { x = v; }

		public void setY(String v) { y = v; }

		public void setZ(String v) { z = v; }

		@Override /* Object */
		public String toString() {
			return ("A1(x: " + x + ", y: " + y + ", z: " + z + ")");
		}

		public A1 init() {
			x = null;
			y = "";
			z = "z";
			return this;
		}
	}

	public static class A2 extends A1 {
		public A2() {}

		public A2(String x, String y, String z) {
			super(x, y, z);
		}

		public String getX() { return x; }

		public String getY() { return y; }

		public String getZ() { return z; }
	}

	// This is not supposed to be a valid bean since it has no getters defined.
	public static class A3 extends A1 {
		public A3() {
		}

		public A3(String x, String y, String z) {
			super(x, y, z);
		}

		public String isX() {
			throw new IllegalCallerException("Should not be called!");
		}

		public String isY() {
			throw new IllegalCallerException("Should not be called!");
		}

		public String isZ() {
			throw new IllegalCallerException("Should not be called!");
		}
	}

	//====================================================================================================
	// Test bean inheritance
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_beanInheritance(RoundTrip_Tester t) throws Exception {
		var t1 = new B1().init();
		var t2 = t. roundTrip(t1, B1.class);
		assertEquals(json(t2), json(t1));
	}

	public static class B1 extends B2 {
		private A2 f4;

		public A2 getF4() { return f4; }
		public void setF4(A2 v) { f4 = v; }

		@Override /* Object */
		public String toString() {
			return super.toString() + " / " + f4;
		}

		public B1 init() {
			setF1("A1");
			setF2(101);
			setF3(false);
			setF4((A2)new A2().init());
			return this;
		}
	}

	public static class B2 {
		private String f1;
		private int f2 = -1;
		private boolean f3;

		public String getF1() { return f1; }
		public void setF1(String v) { f1 = v; }

		public int getF2() { return f2; }
		public void setF2(int v) { f2 = v; }

		public boolean isF3() { return f3; }
		public void setF3(boolean v) { f3 = v; }

		@Override /* Object */
		public String toString() {
			return ("B2(f1: " + getF1() + ", f2: " + getF2() + ")");
		}
	}
}