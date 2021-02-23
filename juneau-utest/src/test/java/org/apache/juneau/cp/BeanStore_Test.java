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
package org.apache.juneau.cp;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import javax.inject.*;

import org.apache.juneau.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanStore_Test {

	private static final String CNAME = BeanStore_Test.class.getName();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {}

	@Test
	public void a01_addBean() {
		BeanStore bs = new BeanStore();
		assertBoolean(bs.hasBean(A.class)).isFalse();
		assertObject(bs.getBean(A.class)).doesNotExist();
		bs.addBean(A.class, new A());
		assertBoolean(bs.hasBean(A.class)).isTrue();
		assertObject(bs.getBean(A.class)).exists();
		bs = BeanStore.of(bs);
		assertBoolean(bs.hasBean(A.class)).isTrue();
		assertObject(bs.getBean(A.class)).exists();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Create bean - No args
	//-----------------------------------------------------------------------------------------------------------------

	public static class B1 {
		public static B1 create() {
			return new B1();
		}
		private B1() {}
	}

	@Test
	public void b01_createBean_create() throws Exception {
		BeanStore bs = new BeanStore();
		assertObject(bs.createBean(B1.class)).isType(B1.class);
	}

	public static class B2 {
		public static B2 getInstance() {
			return new B2();
		}
		private B2() {}
	}

	@Test
	public void b02_createBean_getInstance() throws Exception {
		BeanStore bs = new BeanStore();
		assertObject(bs.createBean(B2.class)).isType(B2.class);
	}

	public static class B3a {
		protected static B3a create() {
			return new B3a();
		}
		private B3a() {}
	}

	@Test
	public void b03a_createBean_create_notVisible() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(B3a.class)).is("Could not instantiate class "+CNAME+"$B3a: Public constructor or creator not found.");
	}

	public static class B3b {
		public static B3b create2() {
			return new B3b();
		}
		private B3b() {}
	}

	@Test
	public void b03b_createBean_create_wrongName() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(B3b.class)).is("Could not instantiate class "+CNAME+"$B3b: Public constructor or creator not found.");
	}

	public static class B3c {
		public static B3b create() {
			return new B3b();
		}
		private B3c() {}
	}

	@Test
	public void b03c_createBean_create_wrongReturnType() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(B3c.class)).is("Could not instantiate class "+CNAME+"$B3c: Public constructor or creator not found.");
	}

	public static class B3d {
		@Deprecated
		public static B3d create() {
			return new B3d();
		}
		private B3d() {}
	}

	@Test
	public void b03d_createBean_create_deprecated() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(B3d.class)).is("Could not instantiate class "+CNAME+"$B3d: Public constructor or creator not found.");
	}

	public static class B3e {
		@BeanIgnore
		public static B3e create() {
			return new B3e();
		}
		private B3e() {}
	}

	@Test
	public void b03e_createBean_create_beanIgnore() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(B3e.class)).is("Could not instantiate class "+CNAME+"$B3e: Public constructor or creator not found.");
	}

	public abstract static class B4a {
		public static B4a create() {
			return new B4a() {};
		}
		private B4a() {}
	}

	@Test
	public void b04a_createBean_create_abstract() throws Exception {
		BeanStore bs = new BeanStore();
		assertObject(bs.createBean(B4a.class)).exists();
	}

	public static interface B4b {
		public static B4b create() {
			return new B4b() {};
		}
	}

	@Test
	public void b04b_createBean_create_interface() throws Exception {
		BeanStore bs = new BeanStore();
		assertObject(bs.createBean(B4b.class)).exists();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Create bean - With args
	//-----------------------------------------------------------------------------------------------------------------

	public static class C1a {
		public A a;
		public static C1a create(A a) {
			C1a x = new C1a();
			x.a = a;
			return x;
		}
		private C1a() {}
	}

	@Test
	public void c01_createBean_create_withArgs() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(C1a.class)).stderr().is("Could not instantiate class "+CNAME+"$C1a: Static creator found but could not find prerequisites: A.");
		bs.addBean(A.class, new A());
		assertObject(bs.createBean(C1a.class)).exists();
		assertObject(bs.createBean(C1a.class)).exists();
	}

	public interface C2a {
		public static C2a create(A a) {
			return new C2a(){};
		}
	}

	@Test
	public void c02a_createBean_create_withArgs_interface() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(C2a.class)).stderr().is("Could not instantiate class "+CNAME+"$C2a: Static creator found but could not find prerequisites: A.");
		bs.addBean(A.class, new A());
		assertObject(bs.createBean(C2a.class)).exists();
		assertObject(bs.createBean(C2a.class)).exists();
	}

	public static abstract class C2b {
		public static C2b create(A a) {
			return new C2b(){};
		}
	}

	@Test
	public void c02b_createBean_create_withArgs_abstractClass() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(C2b.class)).stderr().is("Could not instantiate class "+CNAME+"$C2b: Static creator found but could not find prerequisites: A.");
		bs.addBean(A.class, new A());
		assertObject(bs.createBean(C2b.class)).exists();
		assertObject(bs.createBean(C2b.class)).exists();
	}

	public static interface C2c {}

	@Test
	public void c02c_createBean_create_withArgs_interface() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(C2c.class)).stderr().is("Could not instantiate class "+CNAME+"$C2c: Class is an interface.");
		bs.addBean(A.class, new A());
		assertThrown(()->bs.createBean(C2c.class)).stderr().is("Could not instantiate class "+CNAME+"$C2c: Class is an interface.");
	}

	public static abstract class C2d {}

	@Test
	public void c02d_createBean_create_withArgs_abstractClass() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(C2d.class)).stderr().is("Could not instantiate class "+CNAME+"$C2d: Class is abstract.");
		bs.addBean(A.class, new A());
		assertThrown(()->bs.createBean(C2d.class)).stderr().is("Could not instantiate class "+CNAME+"$C2d: Class is abstract.");
	}

	public static class C3a {
		public A a;
		public static C3a create(@Named("Foo") A a) {
			C3a x = new C3a();
			x.a = a;
			return x;
		}
		private C3a() {}
	}

	@Test
	public void c03_createBean_create_withNamedArgs() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(C3a.class)).stderr().is("Could not instantiate class "+CNAME+"$C3a: Static creator found but could not find prerequisites: A.");
		bs.addBean("Foo", new A());
		assertObject(bs.createBean(C3a.class)).exists();
		assertObject(bs.createBean(C3a.class)).exists();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Construct bean - With args
	//-----------------------------------------------------------------------------------------------------------------

	public static class D1a {
		public A a;
		public D1a(A a) {
			this.a = a;
		}
	}

	@Test
	public void d01a_createBean_construct_withArgs() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(D1a.class)).stderr().is("Could not instantiate class "+CNAME+"$D1a: Public constructor found but could not find prerequisites: A.");
		bs.addBean(A.class, new A());
		assertObject(bs.createBean(D1a.class)).exists();
		assertObject(bs.createBean(D1a.class)).exists();
	}

	public class D1b {
		public A a;
		public D1b(A a) {
			this.a = a;
		}
	}

	@Test
	public void d01b_createBean_construct_withArgs_inner() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(D1b.class)).stderr().is("Could not instantiate class "+CNAME+"$D1b: Public constructor found but could not find prerequisites: BeanStore_Test,A.");
		BeanStore bs2 = BeanStore.of(null,this);
		assertThrown(()->bs2.createBean(D1b.class)).stderr().is("Could not instantiate class "+CNAME+"$D1b: Public constructor found but could not find prerequisites: A.");
		bs2.addBean(A.class, new A());
		assertObject(bs2.createBean(D1b.class)).exists();
		assertObject(bs2.createBean(D1b.class)).exists();
	}

	public static class D2a {
		public A a;
		public D2a(@Named("Foo") A a) {
			this.a = a;
		}
	}

	@Test
	public void d02a_createBean_construct_withNamedArgs() throws Exception {
		BeanStore bs = new BeanStore();
		assertThrown(()->bs.createBean(D2a.class)).stderr().is("Could not instantiate class "+CNAME+"$D2a: Public constructor found but could not find prerequisites: A.");
		bs.addBean("Foo", new A());
		assertObject(bs.createBean(D2a.class)).exists();
		assertObject(bs.createBean(D2a.class)).exists();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Create bean via method.
	//-----------------------------------------------------------------------------------------------------------------

	public static class E {
		public A a;
	}

	public static class E1 {

		public E createA1() {
			return new E();
		}
		public A createA2() {
			return new A();
		}
		protected A createA3() {
			return new A();
		}
		@Deprecated
		public A createA4() {
			return new A();
		}
		@BeanIgnore
		public A createA5() {
			return new A();
		}
		public E createA6() {
			return null;
		}
		public E createA7() {
			throw new RuntimeException("foo");
		}

		public static E createB1() {
			return new E();
		}
		public static A createB2() {
			return new A();
		}
		protected static A createB3() {
			return new A();
		}
		@Deprecated
		public static A createB4() {
			return new A();
		}
		@BeanIgnore
		public static A createB5() {
			return new A();
		}
		public static E createB6() {
			return null;
		}
		public static E createB7() {
			throw new RuntimeException("foo");
		}

		public E createC1(A a) {
			return new E();
		}

		public static E createC2(A a) {
			return new E();
		}

		public static E createC3(Optional<A> a) {
			E e = new E();
			e.a = a.orElse(null);
			return e;
		}

		public static E createC4(@Named("Foo") A a) {
			return new E();
		}

		public static E createC5(@Named("Foo") Optional<A> a) {
			E e = new E();
			e.a = a.orElse(null);
			return e;
		}
	}

	@Test
	public void e01_beanCreateMethodFinder() throws Exception {
		BeanStore bs = BeanStore.create().build();
		E1 x = new E1();

		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA0").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA1").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA2").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA3").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA4").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA5").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA6").run()).doesNotExist();
		assertThrown(()->bs.beanCreateMethodFinder(E.class, x).find("createA7").run()).contains("foo");

		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB0").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB1").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB2").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB3").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB4").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB5").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createB6").run()).doesNotExist();
		assertThrown(()->bs.beanCreateMethodFinder(E.class, x).find("createB7").run()).contains("foo");

		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC1").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC2").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run().a).doesNotExist();
		bs.addBean(A.class, new A());
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC1").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC2").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run().a).exists();
		bs.addSupplier(A.class, ()->(A)null);
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC1").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC2").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run().a).doesNotExist();
		bs.addSupplier(A.class, ()->new A());
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC1").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC2").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run().a).exists();

		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC4").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC5").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC5").run().a).doesNotExist();
		bs.addSupplier("Foo", ()->new A());
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC4").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC5").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC5").run().a).exists();
		bs.addSupplier("Foo", null);

		bs.addSupplier(A.class, ()->null);
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC1").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC2").run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createC3").run().a).doesNotExist();

		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createAx").thenFind("createA1").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA1").thenFind("createAx").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA1", A.class).thenFind("createA2", A.class).run()).doesNotExist();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA1", A.class).thenFind("createA1").run()).exists();
		assertObject(bs.beanCreateMethodFinder(E.class, x).find("createA1", A.class).withDefault(new E()).run()).exists();

		bs.addSupplier(A.class, ()->new A());
		assertObject(bs.createBean(A.class)).exists();

		BeanStore bs2 = BeanStore.of(bs, null);
		assertObject(bs2.beanCreateMethodFinder(E.class, x).find("createA1").run()).exists();

		assertString(bs2.toString()).is("{parent:{beanMap:['"+CNAME+"$A']}}");
	}
}
