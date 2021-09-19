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
		assertOptional(bs.getBean(A.class)).isNull();
		bs.addBean(A.class, new A());
		assertBoolean(bs.hasBean(A.class)).isTrue();
		assertOptional(bs.getBean(A.class)).exists();
		bs = BeanStore.of(bs);
		assertBoolean(bs.hasBean(A.class)).isTrue();
		assertOptional(bs.getBean(A.class)).exists();
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

		assertObject(bs.createMethodFinder(E.class, x).find("createA0").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createA1").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createA2").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createA3").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createA4").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createA5").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createA6").run()).isNull();
		assertThrown(()->bs.createMethodFinder(E.class, x).find("createA7").run()).message().is("foo");

		assertObject(bs.createMethodFinder(E.class, x).find("createB0").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createB1").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createB2").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createB3").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createB4").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createB5").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createB6").run()).isNull();
		assertThrown(()->bs.createMethodFinder(E.class, x).find("createB7").run()).message().is("foo");

		assertObject(bs.createMethodFinder(E.class, x).find("createC1").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC2").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run().a).isNull();
		bs.addBean(A.class, new A());
		assertObject(bs.createMethodFinder(E.class, x).find("createC1").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC2").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run().a).exists();
		bs.addSupplier(A.class, ()->(A)null);
		assertObject(bs.createMethodFinder(E.class, x).find("createC1").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC2").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run().a).isNull();
		bs.addSupplier(A.class, ()->new A());
		assertObject(bs.createMethodFinder(E.class, x).find("createC1").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC2").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run().a).exists();

		assertObject(bs.createMethodFinder(E.class, x).find("createC4").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC5").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC5").run().a).isNull();
		bs.addSupplier("Foo", ()->new A());
		assertObject(bs.createMethodFinder(E.class, x).find("createC4").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC5").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC5").run().a).exists();
		bs.addSupplier("Foo", null);

		bs.addSupplier(A.class, ()->null);
		assertObject(bs.createMethodFinder(E.class, x).find("createC1").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC2").run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createC3").run().a).isNull();

		assertObject(bs.createMethodFinder(E.class, x).find("createAx").thenFind("createA1").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createA1").thenFind("createAx").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createA1", A.class).thenFind("createA2", A.class).run()).isNull();
		assertObject(bs.createMethodFinder(E.class, x).find("createA1", A.class).thenFind("createA1").run()).exists();
		assertObject(bs.createMethodFinder(E.class, x).find("createA1", A.class).withDefault(new E()).run()).exists();

		BeanStore bs2 = BeanStore.of(bs, null);
		assertObject(bs2.createMethodFinder(E.class, x).find("createA1").run()).exists();

		assertString(bs2.toString()).is("{parent:{beanMap:['"+CNAME+"$A']}}");
	}
}
