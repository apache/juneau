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
package org.apache.juneau.transforms;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class BeanFilter_Test extends SimpleTestBase {

	//====================================================================================================
	// Interface bean filters
	//====================================================================================================
	@Test void a01_interfaceBeanFilters() {
		var session = BeanContext.create().interfaces(A1.class).build().getSession();
		var bm = session.newBeanMap(A3.class);
		assertEquals("f1", bm.get("f1"));
		assertNull(bm.get("f2"));
		assertNull(bm.get("f3"));

		session = BeanContext.create().interfaces(A2.class).build().getSession();
		bm = session.newBeanMap(A3.class);
		assertEquals("f1", bm.get("f1"));
		assertEquals("f2", bm.get("f2"));
		assertNull(bm.get("f3"));
	}

	public interface A1 {
		String getF1();
	}

	public interface A2 extends A1 {
		String getF2();
	}

	public static class A3 implements A2 {
		@Override /* A1 */ public String getF1() { return "f1"; }
		@Override /* A2 */ public String getF2() { return "f2"; }
		public String getF3() { return "f3"; }
	}

	//====================================================================================================
	// Abstract class bean filters
	//====================================================================================================
	@Test void a02_abstractClassBeanFilters() {
		var session = BeanContext.create().interfaces(B1.class).build().getSession();
		var bm = session.newBeanMap(Test2.class);
		assertEquals("f1", bm.get("f1"));
		assertNull(bm.get("f2"));
		assertNull(bm.get("f3"));

		session = BeanContext.create().interfaces(B2.class).build().getSession();
		bm = session.newBeanMap(Test2.class);
		assertEquals("f1", bm.get("f1"));
		assertEquals("f2", bm.get("f2"));
		assertNull(bm.get("f3"));
	}

	public abstract static class B1 {
		public abstract String getF1();
	}

	public abstract static class B2 extends B1 {
		@Override /* B1 */
		public abstract String getF1();
		public abstract String getF2();
	}

	public static class Test2 extends B2 {
		@Override /* B1 */ public String getF1() { return "f1"; }
		@Override /* B2 */ public String getF2() { return "f2"; }
		public String getF3() { return "f3"; }
	}

	//====================================================================================================
	// Filtered with stop classes
	//====================================================================================================
	@Test void a03_filteredWithStopClass() {
		var c3 = new C3();
		assertJson(c3, "{f3:3,p3:3}");
	}

	public class C1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}

	public class C2 extends C1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}

	@Bean(stopClass=C2.class)
	public class C3 extends C2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}

	@Test void a04_filterWithStopClassOnParentClass() {
		var d3 = new D3();
		assertJson(d3, "{f3:3,p3:3}");
	}

	public class D1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}

	@Bean(stopClass=D2.class)
	public class D2 extends D1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}

	public class D3 extends D2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}

	@Test void a05_filteredWithStopClassOnParentClassWithOverriddenAnnotation() {
		var e3 = new E3();
		assertJson(e3, "{f3:3,p3:3}");
	}

	public class E1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}

	@Bean(stopClass=E2.class)
	public class E2 extends E1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}

	@Bean(xp="foo")
	public class E3 extends E2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}

	@Test void a06_filteredWithStopClassesAtMulitpleLevels() {
		var e3 = new F3();
		assertJson(e3, "{f3:3,p3:3}");
	}

	@Bean(stopClass=F1.class)
	public class F1 {
		public int f1 = 1;
		public int getP1() { return 1; }
	}

	public class F2 extends F1 {
		public int f2 = 2;
		public int getP2() { return 2; }
	}

	@Bean(stopClass=F2.class)
	public class F3 extends F2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}
}