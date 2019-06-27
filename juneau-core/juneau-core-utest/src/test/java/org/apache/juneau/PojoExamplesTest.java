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
package org.apache.juneau;

import static org.apache.juneau.testutils.TestUtils.*;

import org.apache.juneau.annotation.*;
import org.junit.*;

/*
 * Tests the BEAN_examples property and @Example annotation.
 */
public class PojoExamplesTest {

	//====================================================================================================
	// test BEAN_examples
	//====================================================================================================
	@Test
	public void testProperty() throws Exception {
		BeanSession bs = BeanContext.create().example(A.class, new A().init()).build().createSession();
		assertObjectEquals("{f1:'f1a'}", bs.getClassMeta(A.class).getExample(bs));
	}

	public static class A {
		public String f1;

		public A init() {
			this.f1 = "f1a";
			return this;
		}
	}

	//====================================================================================================
	// test @Example on public field
	//====================================================================================================
	@Test
	public void testExampleField() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1b'}", bs.getClassMeta(B1.class).getExample(bs));
	}

	public static class B1 {
		public String f1;

		@Example
		public static B1 EXAMPLE = new B1().init();

		public B1 init() {
			this.f1 = "f1b";
			return this;
		}
	}

	//====================================================================================================
	// test @Example on private field
	//====================================================================================================
	@Test
	public void testExampleFieldPrivate() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1b'}", bs.getClassMeta(B2.class).getExample(bs));
	}

	public static class B2 {
		public String f1;

		@Example
		private static B2 EXAMPLE = new B2().init();

		public B2 init() {
			this.f1 = "f1b";
			return this;
		}
	}

	//====================================================================================================
	// test @Example on public no-arg method.
	//====================================================================================================
	@Test
	public void testExampleOnPublicNoArgMethod() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1c'}", bs.getClassMeta(C1.class).getExample(bs));
	}

	public static class C1 {
		public String f1;

		public C1 init() {
			this.f1 = "f1c";
			return this;
		}

		@Example
		public static C1 x() {
			return new C1().init();
		}
	}

	//====================================================================================================
	// test @Example on private no-arg method.
	//====================================================================================================
	@Test
	public void testExampleOnPrivateNoArgMethod() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1c'}", bs.getClassMeta(C2.class).getExample(bs));
	}

	public static class C2 {
		public String f1;

		public C2 init() {
			this.f1 = "f1c";
			return this;
		}

		@Example
		private static C2 x() {
			return new C2().init();
		}
	}

	//====================================================================================================
	// test @Example on public 1-arg method
	//====================================================================================================
	@Test
	public void testExampleOnPublicOneArgMethod() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1d'}", bs.getClassMeta(D1.class).getExample(bs));
	}

	public static class D1 {
		public String f1;

		public D1 init() {
			this.f1 = "f1d";
			return this;
		}

		@Example
		public static D1 x(BeanSession bs) {
			return new D1().init();
		}
	}

	//====================================================================================================
	// test example() method, no annotation.
	//====================================================================================================
	@Test
	public void testExampleMethod() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1e'}", bs.getClassMeta(E1.class).getExample(bs));
	}

	public static class E1 {
		public String f1;

		public E1 init() {
			this.f1 = "f1e";
			return this;
		}

		public static E1 example() {
			return new E1().init();
		}
	}

	//====================================================================================================
	// test example(BeanSession) method, no annotation.
	//====================================================================================================
	@Test
	public void testExampleBeanSessionMethod() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		assertObjectEquals("{f1:'f1e'}", bs.getClassMeta(E2.class).getExample(bs));
	}

	public static class E2 {
		public String f1;

		public E2 init() {
			this.f1 = "f1e";
			return this;
		}

		public static E2 example(BeanSession bs) {
			return new E2().init();
		}
	}

	//====================================================================================================
	// test invalid uses of @Example
	//====================================================================================================
	@Test
	public void testInvalidUsesOfExample() throws Exception {
		BeanSession bs = BeanContext.create().build().createSession();
		try {
			bs.getClassMeta(F1.class);
		} catch (Exception e) {
			assertContains(e.getMessage(), "@Example used on invalid method 'example(String)'");
		}
		try {
			bs.getClassMeta(F2.class);
		} catch (Exception e) {
			assertContains(e.getMessage(), "@Example used on invalid method 'example()'");
		}
		try {
			bs.getClassMeta(F3.class);
		} catch (Exception e) {
			assertContains(e.getMessage(), "@Example used on invalid field 'org.apache.juneau.PojoExamplesTest$F3.F3'");
		}
		try {
			bs.getClassMeta(F4.class);
		} catch (Exception e) {
			assertContains(e.getMessage(), "@Example used on invalid field 'org.apache.juneau.PojoExamplesTest$F4.f4'");
		}
	}

	public static class F1 {
		@Example
		public static F1 example(String s) {
			return null;
		}
	}
	public static class F2 {
		@Example
		public F2 example() {
			return null;
		}
	}
	public static class F3 {
		@Example
		public static String F3 = "foo";
	}
	public static class F4 {
		@Example
		public F4 f4 = new F4();
	}
}