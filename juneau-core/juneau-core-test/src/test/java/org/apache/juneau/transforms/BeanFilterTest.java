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
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class BeanFilterTest {

	//====================================================================================================
	// Interface bean filters
	//====================================================================================================
	@Test
	public void testInterfaceBeanFilters() throws Exception {
		BeanSession session;
		BeanMap<A3> bm;

		session = BeanContext.create().beanFilters(A1.class).build().createSession();
		bm = session.newBeanMap(A3.class);
		assertEquals("f1", bm.get("f1"));
		assertNull(bm.get("f2"));
		assertNull(bm.get("f3"));

		session = BeanContext.create().beanFilters(A2.class).build().createSession();
		bm = session.newBeanMap(A3.class);
		assertEquals("f1", bm.get("f1"));
		assertEquals("f2", bm.get("f2"));
		assertNull(bm.get("f3"));
	}

	public static interface A1 {
		public String getF1();
	}

	public static interface A2 extends A1 {
		public String getF2();
	}

	public static class A3 implements A2 {
		@Override /* A1 */
		public String getF1() {
			return "f1";
		}
		@Override /* A2 */
		public String getF2() {
			return "f2";
		}
		public String getF3() {
			return "f3";
		}
	}

	//====================================================================================================
	// Abstract class bean filters
	//====================================================================================================
	@Test
	public void testAbstractClassBeanFilters() throws Exception {
		BeanSession session;
		BeanMap<Test2> bm;

		session = BeanContext.create().beanFilters(B1.class).build().createSession();
		bm = session.newBeanMap(Test2.class);
		assertEquals("f1", bm.get("f1"));
		assertNull(bm.get("f2"));
		assertNull(bm.get("f3"));

		session = BeanContext.create().beanFilters(B2.class).build().createSession();
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
		@Override /* B1 */
		public String getF1() {
			return "f1";
		}
		@Override /* B2 */
		public String getF2() {
			return "f2";
		}
		public String getF3() {
			return "f3";
		}
	}

	//====================================================================================================
	// Filtered with stop classes
	//====================================================================================================
	@Test
	public void testFilteredWithStopClass() throws Exception {
		C3 c3 = new C3();
		assertObjectEquals("{f3:3,p3:3}", c3);
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

	@Test
	public void testFilterWithStopClassOnParentClass() throws Exception {
		D3 d3 = new D3();
		assertObjectEquals("{f3:3,p3:3}", d3);
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

	@Test
	public void testFilteredWithStopClassOnParentClassWithOverriddenAnnotation() throws Exception {
		E3 e3 = new E3();
		assertObjectEquals("{f3:3,p3:3}", e3);
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

	@Bean(excludeProperties="foo")
	public class E3 extends E2 {
		public int f3 = 3;
		public int getP3() { return 3; }
	}

	@Test
	public void testFilteredWithStopClassesAtMulitpleLevels() throws Exception {
		F3 e3 = new F3();
		assertObjectEquals("{f3:3,p3:3}", e3);
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