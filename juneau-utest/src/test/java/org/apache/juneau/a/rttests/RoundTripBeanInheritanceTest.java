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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripBeanInheritanceTest extends RoundTripTest {

	public RoundTripBeanInheritanceTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testBeanInheritance
	//====================================================================================================
	@Test
	public void testBeanInheritance() throws Exception {

		// Skip tests that just return the same object.
		if (returnOriginalObject)
			return;

		A2 t1 = new A2(), t2;
		t1.init();
		t2 = roundTrip(t1, A2.class);
		assertObject(t1).isSameJsonAs(t2);

		A3 t3 = new A3();
		t3.init();
		roundTrip(t3, A3.class);
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

		public void setX(String x) {
			this.x = x;
		}

		public void setY(String y) {
			this.y = y;
		}

		public void setZ(String z) {
			this.z = z;
		}

		@Override /* Object */
		public String toString() {
			return ("A1(x: " + this.x + ", y: " + this.y + ", z: " + this.z + ")");
		}

		public A1 init() {
			x = null;
			y = "";
			z = "z";
			return this;
		}
	}

	public static class A2 extends A1 {
		public A2() {
		}

		public A2(String x, String y, String z) {
			super(x, y, z);
		}

		public String getX() {
			return this.x;
		}

		public String getY() {
			return this.y;
		}

		public String getZ() {
			return this.z;
		}
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
	// testBeanInheritance2
	//====================================================================================================
	@Test
	public void testBeanInheritance2() throws Exception {
		B1 t1 = new B1().init(), t2;
		t2 = roundTrip(t1, B1.class);
		assertObject(t1).isSameJsonAs(t2);
	}

	public static class B1 extends B2 {
		private A2 f4;

		public A2 getF4() {
			return this.f4;
		}

		public void setF4(A2 f4) {
			this.f4 = f4;
		}

		@Override /* Object */
		public String toString() {
			return super.toString() + " / " + this.f4;
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

		public String getF1() {
			return this.f1;
		}

		public void setF1(String f1) {
			this.f1 = f1;
		}

		public int getF2() {
			return this.f2;
		}

		public void setF2(int f2) {
			this.f2 = f2;
		}

		public boolean isF3() {
			return this.f3;
		}

		public void setF3(boolean f3) {
			this.f3 = f3;
		}

		@Override /* Object */
		public String toString() {
			return ("B2(f1: " + this.getF1() + ", f2: " + this.getF2() + ")");
		}
	}
}