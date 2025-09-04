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

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/*
 * Tests the BEAN_examples property and @Example annotation.
 */
class PojoExamplesTest extends SimpleTestBase {

	private final JsonParserSession session = JsonParser.DEFAULT.getSession();

	//====================================================================================================
	// test BEAN_examples
	//====================================================================================================
	@Test void a01_property() {
		var bs = BeanContext.create().example(A.class, new A().init()).build().getSession();
		assertJson("{f1:'f1a'}", bs.getClassMeta(A.class).getExample(bs,session));
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
	@Test void a02_exampleField() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1b'}", bs.getClassMeta(B1.class).getExample(bs,session));
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

	@Test void a03_exampleField_usingConfig() {
		var bs = BeanContext.create().applyAnnotations(B1cConfig.class).build().getSession();
		assertJson("{f1:'f1b'}", bs.getClassMeta(B1c.class).getExample(bs,session));
	}

	@Example(on="Dummy1.EXAMPLE")
	@Example(on="B1c.EXAMPLE")
	@Example(on="Dummy2.EXAMPLE")
	private static class B1cConfig {}

	public static class B1c {
		public String f1;

		public static B1c EXAMPLE = new B1c().init();

		public B1c init() {
			this.f1 = "f1b";
			return this;
		}
	}

	//====================================================================================================
	// test @Example on private field
	//====================================================================================================
	@Test void a04_exampleFieldPrivate() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1b'}", bs.getClassMeta(B2.class).getExample(bs,session));
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

	@Test void a05_exampleFieldPrivate_usingConfig() {
		var bs = BeanContext.create().applyAnnotations(B2cConfig.class).build().getSession();
		assertJson("{f1:'f1b'}", bs.getClassMeta(B2c.class).getExample(bs,session));
	}

	@Example(on="Dummy1.EXAMPLE")
	@Example(on="B2c.EXAMPLE")
	@Example(on="Dummy2.EXAMPLE")
	private static class B2cConfig {}

	public static class B2c {
		public String f1;

		@SuppressWarnings("unused")
		private static B2c EXAMPLE = new B2c().init();

		public B2c init() {
			this.f1 = "f1b";
			return this;
		}
	}

	//====================================================================================================
	// test @Example on public no-arg method.
	//====================================================================================================
	@Test void a06_exampleOnPublicNoArgMethod() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1c'}", bs.getClassMeta(C1.class).getExample(bs,session));
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

	@Test void a07_exampleOnPublicNoArgMethod_usingConfig() {
		var bs = BeanContext.create().applyAnnotations(C1cConfig.class).build().getSession();
		assertJson("{f1:'f1c'}", bs.getClassMeta(C1c.class).getExample(bs,session));
	}

	@Example(on="Dummy1.x")
	@Example(on="C1c.x")
	@Example(on="Dummy2.x")
	private static class C1cConfig {}

	public static class C1c {
		public String f1;

		public C1c init() {
			this.f1 = "f1c";
			return this;
		}

		public static C1c x() {
			return new C1c().init();
		}
	}

	//====================================================================================================
	// test @Example on private no-arg method.
	//====================================================================================================
	@Test void a08_exampleOnPrivateNoArgMethod() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1c'}", bs.getClassMeta(C2.class).getExample(bs,session));
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

	@Test void a09_exampleOnPrivateNoArgMethod_usingConfig() {
		var bs = BeanContext.create().applyAnnotations(C2cConfig.class).build().getSession();
		assertJson("{f1:'f1c'}", bs.getClassMeta(C2c.class).getExample(bs,session));
	}

	@Example(on="Dummy1.x")
	@Example(on="C2c.x")
	@Example(on="Dummy2.x")
	private static class C2cConfig {}

	public static class C2c {
		public String f1;

		public C2c init() {
			this.f1 = "f1c";
			return this;
		}

		@SuppressWarnings("unused")
		private static C2c x() {
			return new C2c().init();
		}
	}

	//====================================================================================================
	// test @Example on public 1-arg method
	//====================================================================================================
	@Test void a10_exampleOnPublicOneArgMethod() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1d'}", bs.getClassMeta(D1.class).getExample(bs,session));
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

	@Test void a11_exampleOnPublicOneArgMethod_usingConfig() {
		var bs = BeanContext.create().applyAnnotations(D1cConfig.class).build().getSession();
		assertJson("{f1:'f1d'}", bs.getClassMeta(D1c.class).getExample(bs,session));
	}

	@Example(on="Dummy1.x(BeanSession)")
	@Example(on="D1c.x(BeanSession)")
	@Example(on="Dummy2.x(BeanSession)")
	private static class D1cConfig {}

	public static class D1c {
		public String f1;

		public D1c init() {
			this.f1 = "f1d";
			return this;
		}

		public static D1c x(BeanSession bs) {
			return new D1c().init();
		}
	}

	//====================================================================================================
	// test example() method, no annotation.
	//====================================================================================================
	@Test void a12_exampleMethod() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1e'}", bs.getClassMeta(E1.class).getExample(bs,session));
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
	@Test void a13_exampleBeanSessionMethod() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertJson("{f1:'f1e'}", bs.getClassMeta(E2.class).getExample(bs,session));
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
	@Test void a14_invalidUsesOfExample() {
		var bs = BeanContext.DEFAULT_SESSION;
		assertThrowsWithMessage(Exception.class, "invalid method 'example(String)'", ()->bs.getClassMeta(F1.class));
		assertThrowsWithMessage(Exception.class, "invalid method 'example()'", ()->bs.getClassMeta(F2.class));
		assertThrowsWithMessage(Exception.class, Utils.list("invalid field","$F3.F3"), ()->bs.getClassMeta(F3.class));
		assertThrowsWithMessage(Exception.class, Utils.list("invalid field ","$F4.f4"), ()->bs.getClassMeta(F4.class));
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