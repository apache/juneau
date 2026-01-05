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
package org.apache.juneau.cp;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.annotation.Named;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

class BeanStore_Test extends TestBase {

	@Documented
	@Target({PARAMETER})
	@Retention(RUNTIME)
	@Inherited
	public @interface Name {
		String value();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {}
	public static class A2 {}

	public static final String A1n = A1.class.getSimpleName();  // NOSONAR

	public static class A3 extends BasicBeanStore {
		protected A3(Builder builder) {
			super(builder);
		}
	}

	private static A1 a1a = new A1(), a1b = new A1(), a1c = new A1(), a1d = new A1(), a1e = new A1();
	private static A2 a2a = new A2();

	@Test void a00_dummy() {
		assertDoesNotThrow(BasicBeanStore.Void::new);
	}

	@Test void a01_builderCopyConstructor() {
		var b1p = BasicBeanStore.create().readOnly().threadSafe().build();
		var b1c = BasicBeanStore.create().parent(b1p).build();
		assertContains("readOnly=true", b1c.toString());
		assertContains("threadSafe=true", b1c.toString());
	}

	@Test void a02_readOnly() {
		var b1p = BasicBeanStore.create().readOnly().build();
		var b1c = BasicBeanStore.create().parent(b1p).build();
		var b2p = BasicBeanStore.create().readOnly().threadSafe().build();
		var b2c = BasicBeanStore.create().parent(b1p).threadSafe().build();

		for (var b : array(b1p, b2p)) {
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", ()->b.add(A1.class, a1a));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", ()->b.add(A1.class, a1a, "foo"));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", ()->b.addBean(A1.class, a1a));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", ()->b.addBean(A1.class, a1a, "foo"));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", ()->b.addSupplier(A1.class, ()->a1a));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", ()->b.addSupplier(A1.class, ()->a1a, "foo"));
			assertThrowsWithMessage(IllegalStateException.class, "Method cannot be used because BasicBeanStore is read-only.", b::clear);
		}

		for (var b : array(b1c, b2c)) {
			b.add(A1.class, a1a);
			b.add(A1.class, a1a, "foo");
			b.addBean(A1.class, a1a);
			b.addBean(A1.class, a1a, "foo");
			b.addSupplier(A1.class, ()->a1a);
			b.addSupplier(A1.class, ()->a1a, "foo");
			b.clear();
		}
	}

	@Test void a04_addBean() {
		var b1p = BasicBeanStore.create().build();
		var b1c = BasicBeanStore.of(b1p);
		var b2p = BasicBeanStore.create().threadSafe().build();
		var b2c = BasicBeanStore.create().threadSafe().parent(b2p).build();

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
//
//		b1p.add(A1.class, a1b);
//		b2p.add(A1.class, a1b);
//		for (var b : array(b1p, b1c, b2p, b2c)) {
//			assertTrue(b.hasBean(A1.class));
//			assertEquals(a1b, b.getBean(A1.class).get());
//			assertList(b.stream(A1.class).map(BasicBeanStore.Entry::get), a1b, a1a);
//		}
//
//		b1c.add(A2.class, a2a);
//		b2c.add(A2.class, a2a);
//		for (var b : array(b1p, b2p)) {
//			assertFalse(b.hasBean(A2.class));
//			assertEmpty(b.getBean(A2.class));
//			assertEmpty(b.stream(A2.class));
//		}
//		for (var b : array(b1c, b2c)) {
//			assertTrue(b.hasBean(A2.class));
//			assertEquals(a2a, b.getBean(A2.class).get());
//			assertList(b.stream(A2.class).map(BasicBeanStore.Entry::get), a2a);
//		}
//
//		assertMatchesGlob("{entries=[{type=A1,bean="+Utils.id(a1b)+"},{type=A1,bean="+Utils.id(a1a)+"}],identity=*}", b1p);
//		assertMatchesGlob("{entries=[{type=A2,bean="+Utils.id(a2a)+"}],identity=*,parent={entries=[{type=A1,bean="+Utils.id(a1b)+"},{type=A1,bean="+Utils.id(a1a)+"}],identity=*}}", b1c);
//		assertMatchesGlob("{entries=[{type=A1,bean="+Utils.id(a1b)+"},{type=A1,bean="+Utils.id(a1a)+"}],identity=*,threadSafe=true}", b2p);
//		assertMatchesGlob("{entries=[{type=A2,bean="+Utils.id(a2a)+"}],identity=*,parent={entries=[{type=A1,bean="+Utils.id(a1b)+"},{type=A1,bean="+Utils.id(a1a)+"}],identity=*,threadSafe=true},threadSafe=true}", b2c);
//
//		b1p.removeBean(A1.class);
//		b1c.clear().addBean(A1.class, a1a);
//		b2p.removeBean(A1.class);
//		b2c.clear().addBean(A1.class, a1a);
//
//		for (var b : array(b1p, b2p)) {
//			assertFalse(b.hasBean(A1.class));
//			assertEmpty(b.getBean(A1.class));
//			assertEmpty(b.stream(A1.class));
//		}
//		for (var b : array(b1c, b2c)) {
//			assertTrue(b.hasBean(A1.class));
//			assertEquals(a1a, b.getBean(A1.class).get());
//			assertList(b.stream(A1.class).map(BasicBeanStore.Entry::get), a1a);
//		}
//
//		b1c.removeBean(A1.class);
//		b2c.removeBean(A1.class);
//		for (var b : array(b1p, b1c, b2p, b2c)) {
//			assertFalse(b.hasBean(A1.class));
//			assertEmpty(b.getBean(A1.class));
//			assertEmpty(b.stream(A1.class));
//		}
	}

	@Test void a05_addNamedBeans() {
		var b1p = BasicBeanStore.create().build();
		var b1c = BasicBeanStore.of(b1p);
		var b2p = BasicBeanStore.create().threadSafe().build();
		var b2c = BasicBeanStore.create().threadSafe().parent(b2p).build();

		for (var b : array(b1p, b2p)) {
			b.addBean(A1.class, a1a).addBean(A1.class, a1b, "foo").addBean(A1.class, a1c, "bar").addBean(A1.class, a1d, "bar").addBean(A2.class, a2a, "foo");
		}
		for (var b : array(b1c, b2c)) {
			b.addBean(A1.class, a1e);
		}

//		for (var b : array(b1p, b2p)) {
//			assertList(b.stream(A1.class).map(BasicBeanStore.Entry::get), a1d,a1c,a1b,a1a);
//		}
//		for (var b : array(b1c, b2c)) {
//			assertList(b.stream(A1.class).map(BasicBeanStore.Entry::get), a1e,a1d,a1c,a1b,a1a);
//		}
//
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
		BasicBeanStore a3;

		public B1(A1 a1, Optional<A2> a2, BasicBeanStore a3) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
		}

		public B1(@Named("foo") A1 a1, @Named("bar") Optional<A2> a2) {
			this.a1 = a1;
			this.a2 = a2;
		}

		public void m1(A1 a1, Optional<A2> a2, BasicBeanStore a3) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
		}

		public void m2(@Named("foo") A1 a1, @Named("bar") Optional<A2> a2) {
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = null;
		}

		public static B1 m3(A1 a1, Optional<A2> a2, BasicBeanStore a3) {
			return new B1(a1, a2, a3);
		}
	}

	@Test void b01_getParams() {

		Predicate<Object> pEmptyOptional = x -> !((Optional<?>)x).isPresent();
		Predicate<Object> pIsBeanStore = BasicBeanStore.class::isInstance;
		Predicate<Object> pNull = x -> x == null;
		Predicate<Object> pA1a = x -> x==a1a;
		Predicate<Object> pA2a = x -> ((Optional<?>)x).get()==a2a;

		var outer = new B1(null, null, null);

		var b1p = BasicBeanStore.create().build().addBean(B1.class, outer);
		var b1c = BasicBeanStore.create().parent(b1p).build();
		var b2p = BasicBeanStore.create().threadSafe().build().addBean(B1.class, outer);
		var b2c = BasicBeanStore.create().parent(b1p).threadSafe().build();

		var ci = ClassInfo.of(B1.class);
		var c1 = ci.getPublicConstructor(x -> x.hasParameterTypes(A1.class, Optional.class, BasicBeanStore.class)).get();
		var c2 = ci.getPublicConstructor(x -> x.hasParameterTypes(A1.class, Optional.class)).get();
		var m1 = ci.getPublicMethod(x-> x.hasName("m1")).get();
		var m2 = ci.getPublicMethod(x-> x.hasName("m2")).get();
		var m3 = ci.getPublicMethod(x-> x.hasName("m3")).get();

		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, m1, m3)) {
				assertString(A1n, b.getMissingParams(e, outer));
				assertFalse(b.hasAllParams(e, outer));
			}
			for (var e : array(c2, m2)) {
				assertString(A1n+"@foo", b.getMissingParams(e, outer));
				assertFalse(b.hasAllParams(e, outer));
			}
		}

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertList(b.getParams(c1, outer), pNull, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, outer), pNull, pEmptyOptional);
			assertList(b.getParams(m1, outer), pNull, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(m2, outer), pNull, pEmptyOptional);
			assertList(b.getParams(m3, outer), pNull, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1a);
		b2p.add(A1.class, a1a);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertNull(b.getMissingParams(c1, outer));
			assertString(A1n+"@foo", b.getMissingParams(c2, outer));
			assertNull(b.getMissingParams(m1, outer));
			assertString(A1n+"@foo", b.getMissingParams(m2, outer));
			assertNull(b.getMissingParams(m3, outer));
			assertTrue(b.hasAllParams(c1, outer));
			assertFalse(b.hasAllParams(c2, outer));
			assertTrue(b.hasAllParams(m1, outer));
			assertFalse(b.hasAllParams(m2, outer));
			assertTrue(b.hasAllParams(m3, outer));
			assertList(b.getParams(c1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, outer), pNull, pEmptyOptional);
			assertList(b.getParams(m1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(m2, outer), pNull, pEmptyOptional);
			assertList(b.getParams(m3, outer), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1a, "foo");
		b2p.add(A1.class, a1a, "foo");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e, outer));
				assertTrue(b.hasAllParams(e, outer));
			}
			assertList(b.getParams(c1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, outer), pA1a, pEmptyOptional);
			assertList(b.getParams(m1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(m2, outer), pA1a, pEmptyOptional);
			assertList(b.getParams(m3, outer), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1b, "bar");
		b2p.add(A1.class, a1b, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e, outer));
				assertTrue(b.hasAllParams(e, outer));
			}
			assertList(b.getParams(c1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, outer), pA1a, pEmptyOptional);
			assertList(b.getParams(m1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(m2, outer), pA1a, pEmptyOptional);
			assertList(b.getParams(m3, outer), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A2.class, a2a, "bar");
		b2p.add(A2.class, a2a, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e, outer));
				assertTrue(b.hasAllParams(e, outer));
			}
			assertList(b.getParams(c1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, outer), pA1a, pA2a);
			assertList(b.getParams(m1, outer), pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(m2, outer), pA1a, pA2a);
			assertList(b.getParams(m3, outer), pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A2.class, a2a, null);
		b2p.add(A2.class, a2a, null);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2, m1, m2, m3)) {
				assertNull(b.getMissingParams(e, outer));
				assertTrue(b.hasAllParams(e, outer));
			}
			assertList(b.getParams(c1, outer), pA1a, pA2a, pIsBeanStore);
			assertList(b.getParams(c2, outer), pA1a, pA2a);
			assertList(b.getParams(m1, outer), pA1a, pA2a, pIsBeanStore);
			assertList(b.getParams(m2, outer), pA1a, pA2a);
			assertList(b.getParams(m3, outer), pA1a, pA2a, pIsBeanStore);
		}
	}

	public class B2 {
		A1 a1;
		Optional<A2> a2;
		BasicBeanStore a3;

		public B2(A1 a1, Optional<A2> a2, BasicBeanStore a3) {
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
		Predicate<Object> pIsBeanStore = BasicBeanStore.class::isInstance;
		Predicate<Object> pNull = x -> x == null;
		Predicate<Object> pThis = x -> x == this;
		Predicate<Object> pA1a = x -> x==a1a;
		Predicate<Object> pA2a = x -> ((Optional<?>)x).get()==a2a;

		var b1p = BasicBeanStore.create().build().addBean(BeanStore_Test.class, this);
		var b1c = BasicBeanStore.create().parent(b1p).build();
		var b2p = BasicBeanStore.create().threadSafe().build().addBean(BeanStore_Test.class, this);
		var b2c = BasicBeanStore.create().parent(b1p).threadSafe().build();

		var ci = ClassInfo.of(B2.class);
		var c1 = ci.getPublicConstructor(x -> x.hasParameterTypes(BeanStore_Test.class, A1.class, Optional.class, BasicBeanStore.class)).get();
		var c2 = ci.getPublicConstructor(x -> x.hasParameterTypes(BeanStore_Test.class, A1.class, Optional.class)).get();

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertString(A1n, b.getMissingParams(c1, this));
			assertString(A1n+"@foo", b.getMissingParams(c2, this));
			assertFalse(b.hasAllParams(c1, this));
			assertFalse(b.hasAllParams(c2, this));
		}

		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertList(b.getParams(c1, this), pThis, pNull, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, this), pThis, pNull, pEmptyOptional);
		}

		b1p.add(A1.class, a1a);
		b2p.add(A1.class, a1a);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			assertNull(b.getMissingParams(c1, this));
			assertString(A1n+"@foo", b.getMissingParams(c2, this));
			assertTrue(b.hasAllParams(c1, this));
			assertFalse(b.hasAllParams(c2, this));
			assertList(b.getParams(c1, this), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, this), pThis, pNull, pEmptyOptional);
		}

		b1p.add(A1.class, a1a, "foo");
		b2p.add(A1.class, a1a, "foo");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e, this));
				assertTrue(b.hasAllParams(e, this));
			}
			assertList(b.getParams(c1, this), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, this), pThis, pA1a, pEmptyOptional);
		}

		b1p.add(A1.class, a1b, "bar");
		b2p.add(A1.class, a1b, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e, this));
				assertTrue(b.hasAllParams(e, this));
			}
			assertList(b.getParams(c1, this), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, this), pThis, pA1a, pEmptyOptional);
		}

		b1p.add(A2.class, a2a, "bar");
		b2p.add(A2.class, a2a, "bar");
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e, this));
				assertTrue(b.hasAllParams(e, this));
			}
			assertList(b.getParams(c1, this), pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertList(b.getParams(c2, this), pThis, pA1a, pA2a);
		}

		b1p.add(A2.class, a2a, null);
		b2p.add(A2.class, a2a, null);
		for (var b : array(b1p, b1c, b2p, b2c)) {
			for (var e : array(c1, c2)) {
				assertNull(b.getMissingParams(e, this));
				assertTrue(b.hasAllParams(e, this));
			}
			assertList(b.getParams(c1, this), pThis, pA1a, pA2a, pIsBeanStore);
			assertList(b.getParams(c2, this), pThis, pA1a, pA2a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// createMethodFinder()
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public A1 a;
	}

	@Test void c00_createMethodFinder_invalidArgs() {
		var b = BasicBeanStore.create().build();
//		assertThrowsWithMessage(IllegalArgumentException.class, "Method cannot be used without outer bean definition.", ()->new BeanCreateMethodFinder<>(null, b));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'beanType' cannot be null.", ()->new BeanCreateMethodFinder<>((Class<?>)null, "", b));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'resourceClass' cannot be null.", ()->new BeanCreateMethodFinder<>(String.class, null, b));
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

	// Bean matching.
	public static class C3 {
		public C createC1(A1 a) { return new C(); }
		public static C createC2(A1 a) { return new C(); }
		public static C createC3(Optional<A1> a) { C e = new C(); e.a = a.orElse(null); return e; }
		public static C createC4(@Named("Foo") A1 a) { return new C(); }
		public static C createC5(@Named("Foo") Optional<A1> a) { C e = new C(); e.a = a.orElse(null); return e; }
		public static C createC6(BasicBeanStore bs) { assertNotNull(bs); return new C(); }
	}


	//-----------------------------------------------------------------------------------------------------------------
	// createBean()
	//-----------------------------------------------------------------------------------------------------------------

	public static class D1a {}
	public class D1b {}

	@Test void d01_createBean_basic() {
		var bs = BasicBeanStore.create().build();
		assertNotNull(BeanCreator.of(D1a.class, bs).outer(this).run());
		assertNotNull(BeanCreator.of(D1b.class, bs).outer(this).run());
		assertThrows(IllegalArgumentException.class, () -> BeanCreator.of(null, bs).outer(this).run());
	}

	public static class D2 {
		public static D2 create() { return d2; }
	}
	public static D2 d2 = new D2();

	@Test void d02_createBean_staticCreator_create() {
		var bs = BasicBeanStore.INSTANCE;
		assertEquals(d2, BeanCreator.of(D2.class, bs).run());
	}

	public abstract static class D3 {
		public static D3 getInstance() { return d3; }
	}
	public static D3 d3 = new D3() {};

	@Test void d03_createBean_staticCreator_getInstance() {
		var bs = BasicBeanStore.INSTANCE;
		assertEquals(d3, BeanCreator.of(D3.class, bs).run());
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
		var bs = BasicBeanStore.INSTANCE;
		assertNotEqualsAny(BeanCreator.of(D4a.class, bs).run(), d4a1, d4a2, d4a3, d4a4);
		assertNotEqualsAny(BeanCreator.of(D4b.class, bs).run(), d4b1, d4b2);
		assertNotNull(BeanCreator.of(D4c.class, bs).run());
	}

	public static class D5 {
		public static D5 create() { return d5a; }
		public static D5 create(Integer i, String s) { return d5b; }
		public static D5 create(Integer i) { return d5c; }
		protected D5() {}
	}
	public static D5 d5a = new D5(), d5b = new D5(), d5c = new D5();

	@Test void d05_createBean_staticCreator_withBeans() {
		var bs = BasicBeanStore.create().build();
		assertEquals(d5a, BeanCreator.of(D5.class, bs).run());
		bs.add(Integer.class, 1);
		assertEquals(d5c, BeanCreator.of(D5.class, bs).run());
		bs.add(String.class, "x");
		assertEquals(d5b, BeanCreator.of(D5.class, bs).run());
	}

	public static class D6 {
		public String s;
		public static D6 create() { return new D6("0"); }
		protected D6(String s) {
			this.s = s;
		}
	}

	@Test void d06_createBean_staticCreator_ignoredWithBuilder() {
		var bs = BasicBeanStore.INSTANCE;
		assertString("1", BeanCreator.of(D6.class, bs).builder(String.class, "1").run().s);
	}

	public static class D7 {
		public String a;
		public static D7 create(Optional<String> s) { return new D7(s.orElse("X")); }
		protected D7(String s) { a = s; }
	}

	@Test void d07_createBean_staticCreator_withOptional() {
		var bs = BasicBeanStore.create().build();
		assertString("X", BeanCreator.of(D7.class, bs).run().a);
		bs.add(String.class, "bar");
		assertString("bar", BeanCreator.of(D7.class, bs).run().a);
	}

	public static class D8 {
		public String a = "foo";
		public static D8 create(Optional<String> s, Integer i) { return new D8(s.orElse(null) + "," + i); }
		private D8(String s) { a = s; }
	}

	@Test void d08_createBean_staticCreator_missingPrereqs() {
		var bs = BasicBeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class org.apache.juneau.cp.BeanStore_Test$D8: Static creator found but could not find prerequisites: Integer.", ()->BeanCreator.of(D8.class, bs).run());
		bs.add(Integer.class, 1);
		assertString("null,1", BeanCreator.of(D8.class, bs).run().a);
		bs.add(String.class, "bar");
		assertString("bar,1", BeanCreator.of(D8.class, bs).run().a);
	}

	public abstract static class D9a {
		public D9a() {}
	}

	public interface D9b {}

	@Test void d09_createBean_staticCreator_withBeans() {
		var bs = BasicBeanStore.INSTANCE;
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D9a.class.getName()+": Class is abstract.", ()->BeanCreator.of(D9a.class, bs).run());
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D9b.class.getName()+": Class is an interface.", ()->BeanCreator.of(D9b.class, bs).run());
	}

	public static class D10 {
		public String a;
		public D10(String s) { a = "s="+s; }
		public D10(Integer i) { a = "i="+i; }
		public D10(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test void d10_createBean_constructors_public() {
		var bs = BasicBeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D10.class.getName()+": Public constructor found but could not find prerequisites: Integer or Integer,String or String.", ()->BeanCreator.of(D10.class, bs).run());
		bs.add(String.class, "foo");
		assertString("s=foo", BeanCreator.of(D10.class, bs).run().a);
		bs.add(Integer.class, 1);
		assertString("s=foo,i=1", BeanCreator.of(D10.class, bs).run().a);
	}

	public static class D11 {
		public String a;
		protected D11(String s) { a = "s="+s; }
		protected D11(Integer i) { a = "i="+i; }
		protected D11(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test void d11_createBean_constructors_protected() {
		var bs = BasicBeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D11.class.getName()+": Protected constructor found but could not find prerequisites: Integer or Integer,String or String.", ()->BeanCreator.of(D11.class, bs).run());
		bs.add(String.class, "foo");
		assertString("s=foo", BeanCreator.of(D11.class, bs).run().a);
		bs.add(Integer.class, 1);
		assertString("s=foo,i=1", BeanCreator.of(D11.class, bs).run().a);
	}

	public static class D12 {
		public String a;
		public D12(String s) { a = "s="+s; }
		protected D12(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test void d12_createBean_constructors_publicOverProtected() {
		var bs = BasicBeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D12.class.getName()+": Public constructor found but could not find prerequisites: String.", ()->BeanCreator.of(D12.class, bs).run());
		bs.add(String.class, "foo");
		bs.add(Integer.class, 1);
		assertString("s=foo", BeanCreator.of(D12.class, bs).run().a);
	}

	public static class D13 {
		private D13() {}
	}

	@Test void d13_createBean_constructors_private() {
		var bs = BasicBeanStore.INSTANCE;
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D13.class.getName()+": No public/protected constructors found.", ()->BeanCreator.of(D13.class, bs).run());
	}

	public static class D14 {
		public String a;
		public D14(@Named("foo") String o) { a = o; }
		public D14(@Named("foo") String o, Integer i) { a = o + "," + i; }
	}

	@Test void d14_createBean_constructors_namedBean() {
		var bs = BasicBeanStore.create().build();
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D14.class.getName()+": Public constructor found but could not find prerequisites: Integer,String@foo or String@foo.", ()->BeanCreator.of(D14.class, bs).run());
		bs.add(String.class, "bar", "foo");
		assertString("bar", BeanCreator.of(D14.class, bs).run().a);
	}

	public class D15 {
		public String a;
		public D15(@Named("foo") String o) { a = o; }
		public D15(@Named("foo") String o, Integer i) { a = o + "," + i; }
	}

	@Test void d15_createBean_constructors_namedBean_withOuter() {
		var bs = BasicBeanStore.create().build().addBean(BeanStore_Test.class, new BeanStore_Test());
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D15.class.getName()+": Public constructor found but could not find prerequisites: Integer,String@foo or String@foo.", ()->BeanCreator.of(D15.class, bs).run());
		bs.add(String.class, "bar", "foo");
		assertString("bar", BeanCreator.of(D15.class, bs).run().a);
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
		var bs = BasicBeanStore.create().build();
		var b = D16.create();
		b.b = "foo";
		assertString("foo", BeanCreator.of(D16.class, bs).builder(D16.Builder.class, b).run().a);
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
		var bs = BasicBeanStore.create().build();
		assertNull(BeanCreator.<D17>of(D17.class, bs).run().a);
		assertThrowsWithMessage(ExecutableException.class, "Could not instantiate class "+D17.class.getName()+": Protected constructor found but could not find prerequisites: Builder or Builder,Integer or Integer.", ()->BeanCreator.<D17>of(D17.class, bs).builder(Boolean.class, true).run());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	@SafeVarargs
	private static <T> T[] array(T...t) {
		return t;
	}
}