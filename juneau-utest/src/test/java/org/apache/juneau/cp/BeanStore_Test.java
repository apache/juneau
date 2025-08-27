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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;
import org.junit.jupiter.api.*;

class BeanStore_Test extends SimpleTestBase {

	@Documented
	@Target({PARAMETER})
	@Retention(RUNTIME)
	@Inherited
	public @interface Named {
		String value();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {}
	public static class A2 {}

	public static final String A1n = A1.class.getSimpleName();  // NOSONAR

	public static class A3 extends BeanStore {
		protected A3(Builder builder) {
			super(builder);
		}
	}

	private static A1 a1a = new A1(), a1b = new A1(), a1c = new A1(), a1d = new A1(), a1e = new A1();
	private static A2 a2a = new A2();

	@Test void a00_dummy() {
		assertNotThrown(BeanStore.Void::new);
	}

	@Test void a01_builderCopyConstructor() {
		var b1p = BeanStore.create().readOnly().threadSafe().build();
		var b1c = BeanStore.create().parent(b1p).build();
		assertContains("readOnly:true", b1c.toString());
		assertContains("threadSafe:true", b1c.toString());
	}

	@Test void a02_readOnly() {
		var b1p = BeanStore.create().readOnly().build();
		var b1c = BeanStore.create().parent(b1p).build();
		var b2p = BeanStore.create().readOnly().threadSafe().build();
		var b2c = BeanStore.create().parent(b1p).threadSafe().build();

		for (BeanStore b : array(b1p, b2p)) {
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.add(A1.class, a1a));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.add(A1.class, a1a, "foo"));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.addBean(A1.class, a1a));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.addBean(A1.class, a1a, "foo"));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.addSupplier(A1.class, ()->a1a));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.addSupplier(A1.class, ()->a1a, "foo"));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", b::clear);
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.removeBean(A1.class));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BeanStore is read-only.", ()->b.removeBean(A1.class, "foo"));
		}

		for (var b : array(b1c, b2c)) {
			b.add(A1.class, a1a);
			b.add(A1.class, a1a, "foo");
			b.addBean(A1.class, a1a);
			b.addBean(A1.class, a1a, "foo");
			b.addSupplier(A1.class, ()->a1a);
			b.addSupplier(A1.class, ()->a1a, "foo");
			b.clear();
			b.removeBean(A1.class);
			b.removeBean(A1.class, "foo");
		}
	}

	@Test void a04_addBean() {
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.of(b1p);
		var b2p = BeanStore.create().threadSafe().build();
		var b2c = BeanStore.create().threadSafe().parent(b2p).build();

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertFalse(b.hasBean(A1.class));
			assertEmpty(b.getBean(A1.class));
		}

		b1p.addBean(A1.class, a1a);
		b2p.addBean(A1.class, a1a);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertTrue(b.hasBean(A1.class));
			assertEquals(a1a, b.getBean(A1.class).get());
		}

		b1p.clear();
		b2p.clear();
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertFalse(b.hasBean(A1.class));
			assertEmpty(b.getBean(A1.class));
		}

		b1p.addBean(A1.class, null);
		b2p.addBean(A1.class, null);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertTrue(b.hasBean(A1.class));
			assertEmpty(b.getBean(A1.class));
		}

		b1p.clear().addSupplier(A1.class, ()->a1a);
		b2p.clear().addSupplier(A1.class, ()->a1a);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertTrue(b.hasBean(A1.class));
			assertEquals(a1a, b.getBean(A1.class).get());
		}

		b1p.add(A1.class, a1b);
		b2p.add(A1.class, a1b);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertTrue(b.hasBean(A1.class));
			assertEquals(a1b, b.getBean(A1.class).get());
			assertStream(b.stream(A1.class).map(BeanStoreEntry::get), a1b, a1a);
		}

		b1c.add(A2.class, a2a);
		b2c.add(A2.class, a2a);
		for (var b : array(b1p, b2p)) {
			assertFalse(b.hasBean(A2.class));
			assertEmpty(b.getBean(A2.class));
			assertStream(b.stream(A2.class));
		}
		for (var b : array(b1c, b2c)) {
			assertTrue(b.hasBean(A2.class));
			assertEquals(a2a, b.getBean(A2.class).get());
			assertStream(b.stream(A2.class).map(BeanStoreEntry::get), a2a);
		}

		assertMatches("{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}]}", b1p.toString());
		assertMatches("{*,entries:[{type:'A2',bean:'"+identity(a2a)+"'}],parent:{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}]}}", b1c.toString());
		assertMatches("{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}],threadSafe:true}", b2p.toString());
		assertMatches("{*,entries:[{type:'A2',bean:'"+identity(a2a)+"'}],parent:{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}],threadSafe:true},threadSafe:true}", b2c.toString());

		b1p.removeBean(A1.class);
		b1c.clear().addBean(A1.class, a1a);
		b2p.removeBean(A1.class);
		b2c.clear().addBean(A1.class, a1a);

		for (var b : array(b1p, b2p)) {
			assertFalse(b.hasBean(A1.class));
			assertEmpty(b.getBean(A1.class));
			assertStream(b.stream(A1.class));
		}
		for (var b : array(b1c, b2c)) {
			assertTrue(b.hasBean(A1.class));
			assertEquals(a1a, b.getBean(A1.class).get());
			assertStream(b.stream(A1.class).map(BeanStoreEntry::get), a1a);
		}

		b1c.removeBean(A1.class);
		b2c.removeBean(A1.class);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertFalse(b.hasBean(A1.class));
			assertEmpty(b.getBean(A1.class));
			assertStream(b.stream(A1.class));
		}
	}

	@Test void a05_addNamedBeans() {
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.of(b1p);
		var b2p = BeanStore.create().threadSafe().build();
		var b2c = BeanStore.create().threadSafe().parent(b2p).build();

		for (var b : array(b1p, b2p)) {
			b.addBean(A1.class, a1a).addBean(A1.class, a1b, "foo").addBean(A1.class, a1c, "bar").addBean(A1.class, a1d, "bar").addBean(A2.class, a2a, "foo");
		}
		for (var b : array(b1c, b2c)) {
			b.addBean(A1.class, a1e);
		}

		for (var b : array(b1p, b2p)) {
			assertStream(b.stream(A1.class).map(BeanStoreEntry::get), a1d,a1c,a1b,a1a);
		}
		for (var b : array(b1c, b2c)) {
			assertStream(b.stream(A1.class).map(BeanStoreEntry::get), a1e,a1d,a1c,a1b,a1a);
		}

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertEquals(a1b, b.getBean(A1.class, "foo").get());
			assertEquals(a1d, b.getBean(A1.class, "bar").get());
			assertEmpty(b.getBean(A1.class, "baz"));
		}
		for (var b : array(b1p, b2p)) {
			assertEquals(a1a, b.getBean(A1.class, null).get());
		}
		for (var b : array(b1c, b2c)) {
			assertEquals(a1e, b.getBean(A1.class, null).get());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter matching
	//-----------------------------------------------------------------------------------------------------------------

	public static class B1 {
		A1 a1;
		Optional<A2> a2;
		BeanStore a3;

		public B1(A1 a1, Optional<A2> a2, BeanStore a3) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
		}

		public B1(@Named("foo") A1 a1, @Named("bar") Optional<A2> a2) {
			this.a1 = a1;
			this.a2 = a2;
		}

		public void m1(A1 a1, Optional<A2> a2, BeanStore a3) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
		}

		public void m2(@Named("foo") A1 a1, @Named("bar") Optional<A2> a2) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = null;
		}

		public static B1 m3(A1 a1, Optional<A2> a2, BeanStore a3) {
			return new B1(a1, a2, a3);
		}
	}

	@Test void b01_getParams() {

		Predicate<Object> pEmptyOptional = x -> !((Optional<?>)x).isPresent();
		Predicate<Object> pIsBeanStore = BeanStore.class::isInstance;
		Predicate<Object> pNull = x -> x == null;
		Predicate<Object> pA1a = x -> x==a1a;
		Predicate<Object> pA2a = x -> ((Optional<?>)x).get()==a2a;

		var outer = new B1(null, null, null);

		var b1p = BeanStore.create().outer(outer).build();
		var b1c = BeanStore.create().outer(outer).parent(b1p).build();
		var b2p = BeanStore.create().outer(outer).threadSafe().build();
		var b2c = BeanStore.create().outer(outer).parent(b1p).threadSafe().build();

		var ci = ClassInfo.of(B1.class);
		var c1 = ci.getPublicConstructor(x -> x.hasParamTypes(A1.class, Optional.class, BeanStore.class));
		var c2 = ci.getPublicConstructor(x -> x.hasParamTypes(A1.class, Optional.class));
		var m1 = ci.getPublicMethod(x-> x.hasName("m1"));
		var m2 = ci.getPublicMethod(x-> x.hasName("m2"));
		var m3 = ci.getPublicMethod(x-> x.hasName("m3"));

		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, m1, m3)) {
				assertString(A1n, b.getMissingParams(e));
				assertFalse(b.hasAllParams(e));
			}
			for (var e : array(c2, m2)) {
				assertString(A1n+"@foo", b.getMissingParams(e));
				assertFalse(b.hasAllParams(e));
			}
		}

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertArray(b.getParams(c1), pNull, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pNull, pEmptyOptional);
			assertArray(b.getParams(m1), pNull, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2), pNull, pEmptyOptional);
			assertArray(b.getParams(m3), pNull, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1a);
		b2p.add(A1.class, a1a);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertNull(b.getMissingParams(c1));
			assertString(A1n+"@foo", b.getMissingParams(c2));
			assertNull(b.getMissingParams(m1));
			assertString(A1n+"@foo", b.getMissingParams(m2));
			assertNull(b.getMissingParams(m3));
			assertTrue(b.hasAllParams(c1));
			assertFalse(b.hasAllParams(c2));
			assertTrue(b.hasAllParams(m1));
			assertFalse(b.hasAllParams(m2));
			assertTrue(b.hasAllParams(m3));
			assertArray(b.getParams(c1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pNull, pEmptyOptional);
			assertArray(b.getParams(m1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2), pNull, pEmptyOptional);
			assertArray(b.getParams(m3), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1a, "foo");
		b2p.add(A1.class, a1a, "foo");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pA1a, pEmptyOptional);
			assertArray(b.getParams(m1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2), pA1a, pEmptyOptional);
			assertArray(b.getParams(m3), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1b, "bar");
		b2p.add(A1.class, a1b, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pA1a, pEmptyOptional);
			assertArray(b.getParams(m1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2), pA1a, pEmptyOptional);
			assertArray(b.getParams(m3), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A2.class, a2a, "bar");
		b2p.add(A2.class, a2a, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pA1a, pA2a);
			assertArray(b.getParams(m1), pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2), pA1a, pA2a);
			assertArray(b.getParams(m3), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A2.class, a2a, null);
		b2p.add(A2.class, a2a, null);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pA1a, pA2a, pIsBeanStore);
			assertArray(b.getParams(c2), pA1a, pA2a);
			assertArray(b.getParams(m1), pA1a, pA2a, pIsBeanStore);
			assertArray(b.getParams(m2), pA1a, pA2a);
			assertArray(b.getParams(m3), pA1a, pA2a, pIsBeanStore);
		}
	}

	public class B2 {
		A1 a1;
		Optional<A2> a2;
		BeanStore a3;

		public B2(A1 a1, Optional<A2> a2, BeanStore a3) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
		}

		public B2(@Named("foo") A1 a1, @Named("bar") Optional<A2> a2) {
			this.a1 = a1;
			this.a2 = a2;
		}
	}

	@Test void b02_getParams_innerClass() {

		Predicate<Object> pEmptyOptional = x -> !((Optional<?>)x).isPresent();
		Predicate<Object> pIsBeanStore = BeanStore.class::isInstance;
		Predicate<Object> pNull = x -> x == null;
		Predicate<Object> pThis = x -> x == this;
		Predicate<Object> pA1a = x -> x==a1a;
		Predicate<Object> pA2a = x -> ((Optional<?>)x).get()==a2a;

		var b1p = BeanStore.create().outer(this).build();
		var b1c = BeanStore.create().outer(this).parent(b1p).build();
		var b2p = BeanStore.create().outer(this).threadSafe().build();
		var b2c = BeanStore.create().outer(this).parent(b1p).threadSafe().build();

		var ci = ClassInfo.of(B2.class);
		var c1 = ci.getPublicConstructor(x -> x.hasParamTypes(BeanStore_Test.class, A1.class, Optional.class, BeanStore.class));
		var c2 = ci.getPublicConstructor(x -> x.hasParamTypes(BeanStore_Test.class, A1.class, Optional.class));

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertString(A1n, b.getMissingParams(c1));
			assertString(A1n+"@foo", b.getMissingParams(c2));
			assertFalse(b.hasAllParams(c1));
			assertFalse(b.hasAllParams(c2));
		}

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertArray(b.getParams(c1), pThis, pNull, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pThis, pNull, pEmptyOptional);
		}

		b1p.add(A1.class, a1a);
		b2p.add(A1.class, a1a);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertNull(b.getMissingParams(c1));
			assertString(A1n+"@foo", b.getMissingParams(c2));
			assertTrue(b.hasAllParams(c1));
			assertFalse(b.hasAllParams(c2));
			assertArray(b.getParams(c1), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pThis, pNull, pEmptyOptional);
		}

		b1p.add(A1.class, a1a, "foo");
		b2p.add(A1.class, a1a, "foo");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pThis, pA1a, pEmptyOptional);
		}

		b1p.add(A1.class, a1b, "bar");
		b2p.add(A1.class, a1b, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pThis, pA1a, pEmptyOptional);
		}

		b1p.add(A2.class, a2a, "bar");
		b2p.add(A2.class, a2a, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2), pThis, pA1a, pA2a);
		}

		b1p.add(A2.class, a2a, null);
		b2p.add(A2.class, a2a, null);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e));
				assertTrue(b.hasAllParams(e));
			}
			assertArray(b.getParams(c1), pThis, pA1a, pA2a, pIsBeanStore);
			assertArray(b.getParams(c2), pThis, pA1a, pA2a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// createMethodFinder()
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public A1 a;
	}

	@Test void c00_createMethodFinder_invalidArgs() {
		var b = BeanStore.create().build();
		assertThrowsWithMessage(IllegalArgumentException.class, "Method cannot be used without outer bean definition.", ()->b.createMethodFinder(null));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'beanType' cannot be null.", ()->b.createMethodFinder((Class<?>)null,""));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'resourceClass' cannot be null.", ()->b.createMethodFinder(String.class,null));
	}

	// Instance methods.
	public static class C1 {
		public C createA1() { return new C(); }
		public A1 createA2() { return new A1(); }
		protected C createA3() { return new C(); }
		@Deprecated public C createA4() { return new C(); }
		@BeanIgnore public C createA5() { return new C(); }
		public C createA6() { return null; }
		public C createA7() { throw new RuntimeException("foo"); }
	}

	@Test void c01_createMethodFinder_instanceMethods() throws Exception {
		var x = new C1();
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.create().parent(b1p).build();
		var b2p = BeanStore.create().build();
		var b2c = BeanStore.create().outer(x).parent(b2p).build();
		var b3p = BeanStore.create().build();
		var b3c = BeanStore.create().outer(this).parent(b3p).build();

		for (var m : array("createA0", "createA2", "createA3", "createA4", "createA5", "createA6")) {
			for (var b : array(b1c, b2c, b3c)) {
				assertNull(b.createMethodFinder(C.class, x).find(m).run());
				assertNull(b.createMethodFinder(C.class, C1.class).find(m).run());
			}
			for (var b : array(b2c, b3c)) {
				assertNull(b.createMethodFinder(C.class).find(m).run());
			}
		}
		assertNotNull(b1c.createMethodFinder(C.class, x).find("createA1").run());
		assertNull(b1c.createMethodFinder(C.class, C1.class).find("createA1").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createA1").run());
		assertNull(b3c.createMethodFinder(C.class).find("createA1").run());
		assertThrowsWithMessage(Exception.class, "foo", ()->b1c.createMethodFinder(C.class, x).find("createA7").run());
		assertNull(b1c.createMethodFinder(C.class, C1.class).find("createA7").run());
		assertThrowsWithMessage(Exception.class, "foo", ()->b2c.createMethodFinder(C.class).find("createA7").run());
		assertNull(b3c.createMethodFinder(C.class).find("createA7").run());
	}

	// Static methods.
	public static class C2 {
		public static C createB1() { return new C(); }
		public static A1 createB2() { return new A1(); }
		protected static C createB3() { return new C(); }
		@Deprecated public static C createB4() { return new C(); }
		@BeanIgnore public static C createB5() { return new C(); }
		public static C createB6() { return null; }
		public static C createB7() { throw new RuntimeException("foo"); }
	}

	@Test void c02_createMethodFinder_staticMethods() throws Exception {
		var x = new C2();
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.create().parent(b1p).build();
		var b2p = BeanStore.create().build();
		var b2c = BeanStore.create().outer(x).parent(b2p).build();
		var b3p = BeanStore.create().build();
		var b3c = BeanStore.create().outer(this).parent(b3p).build();

		for (var m : array("createB0", "createB2", "createB3", "createB4", "createB5", "createB6")) {
			for (var b : array(b1c, b2c, b3c)) {
				assertNull(b.createMethodFinder(C.class, x).find(m).run());
				assertNull(b.createMethodFinder(C.class, C2.class).find(m).run());
			}
			for (var b : array(b2c, b3c)) {
				assertNull(b.createMethodFinder(C.class).find(m).run());
			}
		}
		assertNotNull(b1c.createMethodFinder(C.class, x).find("createB1").run());
		assertNotNull(b1c.createMethodFinder(C.class, C2.class).find("createB1").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createB1").run());
		assertNull(b3c.createMethodFinder(C.class).find("createB1").run());
		assertThrowsWithMessage(Exception.class, "foo", ()->b1c.createMethodFinder(C.class, x).find("createB7").run());
		assertThrowsWithMessage(Exception.class, "foo", ()->b1c.createMethodFinder(C.class, C2.class).find("createB7").run());
		assertThrowsWithMessage(Exception.class, "foo", ()->b2c.createMethodFinder(C.class).find("createB7").run());
		assertNull(b3c.createMethodFinder(C.class).find("createB7").run());
	}

	// Bean matching.
	public static class C3 {
		public C createC1(A1 a) { return new C(); }
		public static C createC2(A1 a) { return new C(); }
		public static C createC3(Optional<A1> a) { C e = new C(); e.a = a.orElse(null); return e; }
		public static C createC4(@Named("Foo") A1 a) { return new C(); }
		public static C createC5(@Named("Foo") Optional<A1> a) { C e = new C(); e.a = a.orElse(null); return e; }
		public static C createC6(BeanStore bs) { assertNotNull(bs); return new C(); }
	}

	@Test void c03_createMethodFinder_beanMatching() throws Exception {
		var x = new C3();
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.create().parent(b1p).build();
		var b2p = BeanStore.create().build();
		var b2c = BeanStore.create().outer(x).parent(b2p).build();
		var b3p = BeanStore.create().build();
		var b3c = BeanStore.create().outer(this).parent(b3p).build();

		for (var b : array(b1c, b2c, b3c)) {
			assertNull(b.createMethodFinder(C.class, x).find("createC1").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC2").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, x).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, x).find("createC6").run());
		}

		b1p.addBean(A1.class, new A1());
		b2p.addBean(A1.class, new A1());
		b3p.addBean(A1.class, new A1());
		for (var b : array(b1c, b2c)) {
			assertNotNull(b.createMethodFinder(C.class, x).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC2").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, x).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, x).find("createC6").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC2").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC6").run());
		}
		assertNotNull(b2c.createMethodFinder(C.class).find("createC1").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC2").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC3").run().a);
		assertNull(b2c.createMethodFinder(C.class).find("createC4").run());
		assertNull(b2c.createMethodFinder(C.class).find("createC5").run().a);
		assertNotNull(b2c.createMethodFinder(C.class).find("createC6").run());
		for (var m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertNull(b3c.createMethodFinder(C.class).find(m).run());
		}

		b1p.clear().addSupplier(A1.class, A1::new);
		b2p.clear().addSupplier(A1.class, A1::new);
		for (var b : array(b1c, b2c)) {
			assertNotNull(b.createMethodFinder(C.class, x).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC2").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, x).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, x).find("createC6").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC2").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC6").run());
		}
		assertNotNull(b2c.createMethodFinder(C.class).find("createC1").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC2").run());
		assertType(A1.class, b2c.createMethodFinder(C.class).find("createC3").run().a);
		assertNull(b2c.createMethodFinder(C.class).find("createC4").run());
		assertNull(b2c.createMethodFinder(C.class).find("createC5").run().a);
		assertNotNull(b2c.createMethodFinder(C.class).find("createC6").run());
		for (var m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertNull(b3c.createMethodFinder(C.class).find(m).run());
		}

		b1p.clear().add(A1.class, null);
		b2p.clear().add(A1.class, null);
		for (var b : array(b1c, b2c)) {
			assertNotNull(b.createMethodFinder(C.class, x).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC2").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, x).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, x).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, x).find("createC6").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC2").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC3").run().a);
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC4").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC6").run());
		}
		assertNotNull(b2c.createMethodFinder(C.class).find("createC1").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC2").run());
		assertNull(b2c.createMethodFinder(C.class).find("createC3").run().a);
		assertNull(b2c.createMethodFinder(C.class).find("createC4").run());
		assertNull(b2c.createMethodFinder(C.class).find("createC5").run().a);
		assertNotNull(b2c.createMethodFinder(C.class).find("createC6").run());
		for (var m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertNull(b3c.createMethodFinder(C.class).find(m).run());
		}

		b1p.clear().addBean(A1.class, new A1()).add(A1.class, new A1(), "Foo");
		b2p.clear().addBean(A1.class, new A1()).add(A1.class, new A1(), "Foo");
		for (var b : array(b1c, b2c)) {
			assertNotNull(b.createMethodFinder(C.class, x).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC2").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC3").run().a);
			assertNotNull(b.createMethodFinder(C.class, x).find("createC4").run());
			assertNotNull(b.createMethodFinder(C.class, x).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, x).find("createC6").run());
			assertNull(b.createMethodFinder(C.class, C3.class).find("createC1").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC2").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC3").run().a);
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC4").run());
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC5").run().a);
			assertNotNull(b.createMethodFinder(C.class, C3.class).find("createC6").run());
		}
		assertNotNull(b2c.createMethodFinder(C.class).find("createC1").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC2").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC3").run().a);
		assertNotNull(b2c.createMethodFinder(C.class).find("createC4").run());
		assertNotNull(b2c.createMethodFinder(C.class).find("createC5").run().a);
		assertNotNull(b2c.createMethodFinder(C.class).find("createC6").run());
		for (var m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertNull(b3c.createMethodFinder(C.class).find(m).run());
		}
	}

	// Bean matching.
	public static class C4 {
		public static String createC1(A1 a) { return "createC1"; }
		public static String createC2() { return "createC2"; }
	}

	@Test void c04_createMethodFinder_beanMatching_requiredArgs() throws Exception {
		var x = new C4();
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.create().outer(x).parent(b1p).build();

		assertString("createC2", b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run());
		assertString("createC2", b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run());

		b1p.add(A1.class, null);
		assertString("createC1", b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run());
		assertString("createC2", b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run());
		assertString("createC1", b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run());
		assertString("createC1", b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run());

		b1p.clear();
		assertString("createC1", b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC1").thenFind("createC2").run());
		assertString("createC2", b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC2").thenFind("createC1").run());
		assertString("createC1", b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run());
		assertString("createC1", b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run());

		assertString("X", b1c.createMethodFinder(String.class).withDefault("X").run());
		assertString("X", b1c.createMethodFinder(String.class).withDefault(()->"X").run());

		b1c.createMethodFinder(String.class).withDefault("X").run(y -> assertString("X", y));
	}

	// Bean matching.
	public class C5 {
		public String createC1(A1 a) { return "createC1"; }
		public String createC2() { return "createC2"; }
	}

	@Test void c05_createMethodFinder_beanMatching_requiredArgs_innerClass() throws Exception {
		var x = new C5();
		var b1p = BeanStore.create().build();
		var b1c = BeanStore.create().outer(x).parent(b1p).build();

		assertString("createC2", b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run());
		assertString("createC2", b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run());

		b1p.add(A1.class, null);
		assertString("createC1", b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run());
		assertString("createC2", b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run());
		assertString("createC1", b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run());
		assertString("createC1", b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run());

		b1p.clear();
		assertString("createC1", b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC1").thenFind("createC2").run());
		assertString("createC2", b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC2").thenFind("createC1").run());
		assertString("createC1", b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run());
		assertString("createC1", b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run());

		assertString("X", b1c.createMethodFinder(String.class).withDefault("X").run());
		assertString("X", b1c.createMethodFinder(String.class).withDefault(()->"X").run());

		b1c.createMethodFinder(String.class).withDefault("X").run(y -> assertString("X", y));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// createBean()
	//-----------------------------------------------------------------------------------------------------------------

	public static class D1a {}
	public class D1b {}

	@Test void d01_createBean_basic() {
		var bs = BeanStore.create().outer(new BeanStore_Test()).build();
		assertNotNull(bs.createBean(D1a.class).run());
		assertNotNull(bs.createBean(D1b.class).run());
		assertNull(bs.createBean(null).run());
	}

	public static class D2 {
		public static D2 create() { return d2; }
	}
	public static D2 d2 = new D2();

	@Test void d02_createBean_staticCreator_create() {
		var bs = BeanStore.INSTANCE;
		assertEquals(d2, bs.createBean(D2.class).run());
	}

	public abstract static class D3 {
		public static D3 getInstance() { return d3; }
	}
	public static D3 d3 = new D3() {};

	@Test void d03_createBean_staticCreator_getInstance() {
		var bs = BeanStore.INSTANCE;
		assertEquals(d3, bs.createBean(D3.class).run());
	}

	public static class D4a {
		public static D4a getFoo() { return d4a1; }
		protected static D4a create() { return d4a2; }
		public static D4a create(String foo) { return d4a3; }
		@Deprecated protected static D4a getInstance() { return d4a4; }
		protected D4a() {}
	}
	public static D4a d4a1 = new D4a(), d4a2 = new D4a(), d4a3 = new D4a(), d4a4 = new D4a();

	public static class D4b {
		public D4b create() { return d4b1; }
		@BeanIgnore public static D4b getInstance() { return d4b2; }
		protected D4b() {}
	}
	public static D4b d4b1 = new D4b(), d4b2 = new D4b();

	public static class D4c {
		public static String create() { return null; }
		protected D4c() {}
	}

	@Test void d04_createBean_staticCreator_invalidSignatures() {
		var bs = BeanStore.INSTANCE;
		assertNotEqualsAny(bs.createBean(D4a.class).run(), d4a1, d4a2, d4a3, d4a4);
		assertNotEqualsAny(bs.createBean(D4b.class).run(), d4b1, d4b2);
		assertNotNull(bs.createBean(D4c.class).run());
	}

	public static class D5 {
		public static D5 create() { return d5a; }
		public static D5 create(Integer i, String s) { return d5b; }
		public static D5 create(Integer i) { return d5c; }
		protected D5() {}
	}
	public static D5 d5a = new D5(), d5b = new D5(), d5c = new D5();

	@Test void d05_createBean_staticCreator_withBeans() {
		var bs = BeanStore.create().build();
		assertEquals(d5a, bs.createBean(D5.class).run());
		bs.add(Integer.class, 1);
		assertEquals(d5c, bs.createBean(D5.class).run());
		bs.add(String.class, "x");
		assertEquals(d5b, bs.createBean(D5.class).run());
		bs.removeBean(Integer.class);
		assertEquals(d5a, bs.createBean(D5.class).run());
	}

	public static class D6 {
		public String s;
		public static D6 create() { return new D6("0"); }
		protected D6(String s) {
			this.s = s;
		}
	}

	@Test void d06_createBean_staticCreator_ignoredWithBuilder() {
		var bs = BeanStore.INSTANCE;
		assertString("1", bs.createBean(D6.class).builder(String.class, "1").run().s);
	}

	public static class D7 {
		public String a;
		public static D7 create(Optional<String> s) { return new D7(s.orElse("X")); }
		protected D7(String s) { a = s; }
	}

	@Test void d07_createBean_staticCreator_withOptional() {
		var bs = BeanStore.create().build();
		assertString("X", bs.createBean(D7.class).run().a);
		bs.add(String.class, "bar");
		assertString("bar", bs.createBean(D7.class).run().a);
	}

	public static class D8 {
		public String a = "foo";
		public static D8 create(Optional<String> s, Integer i) { return new D8(s.orElse(null) + "," + i); }
		private D8(String s) { a = s; }
	}

	@Test void d08_createBean_staticCreator_missingPrereqs() {
		var bs = BeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class org.apache.juneau.cp.BeanStore_Test$D8: Static creator found but could not find prerequisites: Integer.", ()->bs.createBean(D8.class).run());
		bs.add(Integer.class, 1);
		assertString("null,1", bs.createBean(D8.class).run().a);
		bs.add(String.class, "bar");
		assertString("bar,1", bs.createBean(D8.class).run().a);
	}

	public abstract static class D9a {
		public D9a() {}
	}

	public interface D9b {}

	@Test void d09_createBean_staticCreator_withBeans() {
		var bs = BeanStore.INSTANCE;
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D9a.class.getName()+": Class is abstract.", ()->bs.createBean(D9a.class).run());
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D9b.class.getName()+": Class is an interface.", ()->bs.createBean(D9b.class).run());
	}

	public static class D10 {
		public String a;
		public D10(String s) { a = "s="+s; }
		public D10(Integer i) { a = "i="+i; }
		public D10(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test void d10_createBean_constructors_public() {
		var bs = BeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D10.class.getName()+": Public constructor found but could not find prerequisites: Integer or Integer,String or String.", ()->bs.createBean(D10.class).run());
		bs.add(String.class, "foo");
		assertString("s=foo", bs.createBean(D10.class).run().a);
		bs.add(Integer.class, 1);
		assertString("s=foo,i=1", bs.createBean(D10.class).run().a);
		bs.removeBean(String.class);
		assertString("i=1", bs.createBean(D10.class).run().a);
	}

	public static class D11 {
		public String a;
		protected D11(String s) { a = "s="+s; }
		protected D11(Integer i) { a = "i="+i; }
		protected D11(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test void d11_createBean_constructors_protected() {
		var bs = BeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D11.class.getName()+": Protected constructor found but could not find prerequisites: Integer or Integer,String or String.", ()->bs.createBean(D11.class).run());
		bs.add(String.class, "foo");
		assertString("s=foo", bs.createBean(D11.class).run().a);
		bs.add(Integer.class, 1);
		assertString("s=foo,i=1", bs.createBean(D11.class).run().a);
		bs.removeBean(String.class);
		assertString("i=1", bs.createBean(D11.class).run().a);
	}

	public static class D12 {
		public String a;
		public D12(String s) { a = "s="+s; }
		protected D12(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test void d12_createBean_constructors_publicOverProtected() {
		var bs = BeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D12.class.getName()+": Public constructor found but could not find prerequisites: String.", ()->bs.createBean(D12.class).run());
		bs.add(String.class, "foo");
		bs.add(Integer.class, 1);
		assertString("s=foo", bs.createBean(D12.class).run().a);
	}

	public static class D13 {
		private D13() {}
	}

	@Test void d13_createBean_constructors_private() {
		var bs = BeanStore.INSTANCE;
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D13.class.getName()+": No public/protected constructors found.", ()->bs.createBean(D13.class).run());
	}

	public static class D14 {
		public String a;
		public D14(@Named("foo") String o) { a = o; }
		public D14(@Named("foo") String o, Integer i) { a = o + "," + i; }
	}

	@Test void d14_createBean_constructors_namedBean() {
		var bs = BeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D14.class.getName()+": Public constructor found but could not find prerequisites: Integer,String@foo or String@foo.", ()->bs.createBean(D14.class).run());
		bs.add(String.class, "bar", "foo");
		assertString("bar", bs.createBean(D14.class).run().a);
	}

	public class D15 {
		public String a;
		public D15(@Named("foo") String o) { a = o; }
		public D15(@Named("foo") String o, Integer i) { a = o + "," + i; }
	}

	@Test void d15_createBean_constructors_namedBean_withOuter() {
		var bs = BeanStore.create().outer(new BeanStore_Test()).build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D15.class.getName()+": Public constructor found but could not find prerequisites: Integer,String@foo or String@foo.", ()->bs.createBean(D15.class).run());
		bs.add(String.class, "bar", "foo");
		assertString("bar", bs.createBean(D15.class).run().a);
	}

	public static class D16 {
		public String a;
		public static Builder create() { return new Builder(); }
		public static class Builder {
			public String b;
		}
		protected D16(Builder b) { a = b.b; }
	}

	@Test void d16_createBean_builders() {
		var bs = BeanStore.create().build();
		var b = D16.create();
		b.b = "foo";
		assertString("foo", bs.createBean(D16.class).builder(D16.Builder.class, b).run().a);
	}

	public static class D17 {
		public String a;
		public static Builder create() { return new Builder(); }
		public static class Builder {
			public String b;
		}
		protected D17(Builder b, Integer i) { a = b.b; }
		protected D17(Integer i) {}  // NOSONAR
		D17(String s) {}  // NOSONAR
		protected D17(Builder b) { a = b.b; }
	}

	@Test void d17_createBean_builders_inherent() {
		var bs = BeanStore.create().build();
		assertNull(bs.createBean(D17.class).run().a);
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D17.class.getName()+": Protected constructor found but could not find prerequisites: Builder or Builder,Integer or Integer.", ()->bs.createBean(D17.class).builder(Boolean.class, true).run());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	@SafeVarargs
	private static <T> T[] array(T...t) {
		return t;
	}
}