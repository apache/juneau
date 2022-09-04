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
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanStore_Test {

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

	public static String A1n = A1.class.getSimpleName();

	public static class A3 extends BeanStore {
		protected A3(Builder builder) {
			super(builder);
		}
	}

	private static A1 a1a = new A1(), a1b = new A1(), a1c = new A1(), a1d = new A1(), a1e = new A1();
	private static A2 a2a = new A2();

	@Test
	public void a00_dummy() {
		new BeanStore.Void();
	}

	@Test
	public void a01_builderCopyConstructor() {
		BeanStore b1p = BeanStore.create().readOnly().threadSafe().build();
		BeanStore b1c = BeanStore.create().parent(b1p).build();
		assertString(b1c.toString()).isContains("readOnly:true","threadSafe:true");
	}

	@Test
	public void a02_readOnly() {
		BeanStore b1p = BeanStore.create().readOnly().build();
		BeanStore b1c = BeanStore.create().parent(b1p).build();
		BeanStore b2p = BeanStore.create().readOnly().threadSafe().build();
		BeanStore b2c = BeanStore.create().parent(b1p).threadSafe().build();

		for (BeanStore b : array(b1p, b2p)) {
			assertThrown(()->b.add(A1.class, a1a)).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.add(A1.class, a1a, "foo")).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.addBean(A1.class, a1a)).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.addBean(A1.class, a1a, "foo")).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.addSupplier(A1.class, ()->a1a)).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.addSupplier(A1.class, ()->a1a, "foo")).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.clear()).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.removeBean(A1.class)).asMessage().is("Method cannot be used because BeanStore is read-only.");
			assertThrown(()->b.removeBean(A1.class, "foo")).asMessage().is("Method cannot be used because BeanStore is read-only.");
		}

		for (BeanStore b : array(b1c, b2c)) {
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

	@Test
	public void a04_addBean() {
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.of(b1p);
		BeanStore b2p = BeanStore.create().threadSafe().build();
		BeanStore b2c = BeanStore.create().threadSafe().parent(b2p).build();

		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isFalse();
			assertOptional(b.getBean(A1.class)).isNull();
		}

		b1p.addBean(A1.class, a1a);
		b2p.addBean(A1.class, a1a);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isTrue();
			assertOptional(b.getBean(A1.class)).is(a1a);
		}

		b1p.clear();
		b2p.clear();
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isFalse();
			assertOptional(b.getBean(A1.class)).isNull();
		}

		b1p.addBean(A1.class, null);
		b2p.addBean(A1.class, null);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isTrue();
			assertOptional(b.getBean(A1.class)).isNull();
		}

		b1p.clear().addSupplier(A1.class, ()->a1a);
		b2p.clear().addSupplier(A1.class, ()->a1a);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isTrue();
			assertOptional(b.getBean(A1.class)).is(a1a);
		}

		b1p.add(A1.class, a1b);
		b2p.add(A1.class, a1b);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isTrue();
			assertOptional(b.getBean(A1.class)).is(a1b);
			assertList(b.stream(A1.class).map(BeanStoreEntry::get)).isHas(a1b,a1a);
		}

		b1c.add(A2.class, a2a);
		b2c.add(A2.class, a2a);
		for (BeanStore b : array(b1p, b2p)) {
			assertBoolean(b.hasBean(A2.class)).isFalse();
			assertOptional(b.getBean(A2.class)).isNull();
			assertList(b.stream(A2.class)).isEmpty();
		}
		for (BeanStore b : array(b1c, b2c)) {
			assertBoolean(b.hasBean(A2.class)).isTrue();
			assertOptional(b.getBean(A2.class)).is(a2a);
			assertList(b.stream(A2.class).map(BeanStoreEntry::get)).isHas(a2a);
		}

		assertString(b1p.toString()).isMatches("{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}]}");
		assertString(b1c.toString()).isMatches("{*,entries:[{type:'A2',bean:'"+identity(a2a)+"'}],parent:{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}]}}");
		assertString(b2p.toString()).isMatches("{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}],threadSafe:true}");
		assertString(b2c.toString()).isMatches("{*,entries:[{type:'A2',bean:'"+identity(a2a)+"'}],parent:{*,entries:[{type:'A1',bean:'"+identity(a1b)+"'},{type:'A1',bean:'"+identity(a1a)+"'}],threadSafe:true},threadSafe:true}");

		b1p.removeBean(A1.class);
		b1c.clear().addBean(A1.class, a1a);
		b2p.removeBean(A1.class);
		b2c.clear().addBean(A1.class, a1a);

		for (BeanStore b : array(b1p, b2p)) {
			assertBoolean(b.hasBean(A1.class)).isFalse();
			assertOptional(b.getBean(A1.class)).isNull();
			assertList(b.stream(A1.class)).isEmpty();
		}
		for (BeanStore b : array(b1c, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isTrue();
			assertOptional(b.getBean(A1.class)).is(a1a);
			assertList(b.stream(A1.class).map(BeanStoreEntry::get)).isHas(a1a);
		}

		b1c.removeBean(A1.class);
		b2c.removeBean(A1.class);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertBoolean(b.hasBean(A1.class)).isFalse();
			assertOptional(b.getBean(A1.class)).isNull();
			assertList(b.stream(A1.class)).isEmpty();
		}
	}

	@Test
	public void a05_addNamedBeans() {
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.of(b1p);
		BeanStore b2p = BeanStore.create().threadSafe().build();
		BeanStore b2c = BeanStore.create().threadSafe().parent(b2p).build();

		for (BeanStore b : array(b1p, b2p)) {
			b.addBean(A1.class, a1a).addBean(A1.class, a1b, "foo").addBean(A1.class, a1c, "bar").addBean(A1.class, a1d, "bar").addBean(A2.class, a2a, "foo");
		}
		for (BeanStore b : array(b1c, b2c)) {
			b.addBean(A1.class, a1e);
		}

		for (BeanStore b : array(b1p, b2p)) {
			assertList(b.stream(A1.class).map(BeanStoreEntry::get)).isHas(a1d,a1c,a1b,a1a);
		}
		for (BeanStore b : array(b1c, b2c)) {
			assertList(b.stream(A1.class).map(BeanStoreEntry::get)).isHas(a1e,a1d,a1c,a1b,a1a);
		}

		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertOptional(b.getBean(A1.class, "foo")).is(a1b);
			assertOptional(b.getBean(A1.class, "bar")).is(a1d);
			assertOptional(b.getBean(A1.class, "baz")).isNull();
		}
		for (BeanStore b : array(b1p, b2p)) {
			assertOptional(b.getBean(A1.class, null)).is(a1a);
		}
		for (BeanStore b : array(b1c, b2c)) {
			assertOptional(b.getBean(A1.class, null)).is(a1e);
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

	@Test
	public void b01_getParams() {

		Predicate<Object> pEmptyOptional = x -> !((Optional<?>)x).isPresent();
		Predicate<Object> pIsBeanStore = x -> x instanceof BeanStore;
		Predicate<Object> pNull = x -> x == null;
		Predicate<Object> pA1a = x -> x==a1a;
		Predicate<Object> pA2a = x -> ((Optional<?>)x).get()==a2a;

		B1 outer = new B1(null, null, null);

		BeanStore b1p = BeanStore.create().outer(outer).build();
		BeanStore b1c = BeanStore.create().outer(outer).parent(b1p).build();
		BeanStore b2p = BeanStore.create().outer(outer).threadSafe().build();
		BeanStore b2c = BeanStore.create().outer(outer).parent(b1p).threadSafe().build();

		ClassInfo ci = ClassInfo.of(B1.class);
		ConstructorInfo c1 = ci.getPublicConstructor(x -> x.hasParamTypes(A1.class, Optional.class, BeanStore.class));
		ConstructorInfo c2 = ci.getPublicConstructor(x -> x.hasParamTypes(A1.class, Optional.class));
		MethodInfo m1 = ci.getPublicMethod(x-> x.hasName("m1"));
		MethodInfo m2 = ci.getPublicMethod(x-> x.hasName("m2"));
		MethodInfo m3 = ci.getPublicMethod(x-> x.hasName("m3"));

		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, m1, m3)) {
				assertString(b.getMissingParams(e)).is(A1n);
				assertBoolean(b.hasAllParams(e)).isFalse();
			}
			for (ExecutableInfo e : array(c2, m2)) {
				assertString(b.getMissingParams(e)).is(A1n+"@foo");
				assertBoolean(b.hasAllParams(e)).isFalse();
			}
		}

		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertArray(b.getParams(c1)).is(pNull, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pNull, pEmptyOptional);
			assertArray(b.getParams(m1)).is(pNull, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2)).is(pNull, pEmptyOptional);
			assertArray(b.getParams(m3)).is(pNull, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1a);
		b2p.add(A1.class, a1a);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertString(b.getMissingParams(c1)).isNull();
			assertString(b.getMissingParams(c2)).is(A1n+"@foo");
			assertString(b.getMissingParams(m1)).isNull();
			assertString(b.getMissingParams(m2)).is(A1n+"@foo");
			assertString(b.getMissingParams(m3)).isNull();
			assertBoolean(b.hasAllParams(c1)).isTrue();
			assertBoolean(b.hasAllParams(c2)).isFalse();
			assertBoolean(b.hasAllParams(m1)).isTrue();
			assertBoolean(b.hasAllParams(m2)).isFalse();
			assertBoolean(b.hasAllParams(m3)).isTrue();
			assertArray(b.getParams(c1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pNull, pEmptyOptional);
			assertArray(b.getParams(m1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2)).is(pNull, pEmptyOptional);
			assertArray(b.getParams(m3)).is(pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1a, "foo");
		b2p.add(A1.class, a1a, "foo");
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2, m1, m2, m3)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pA1a, pEmptyOptional);
			assertArray(b.getParams(m1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2)).is(pA1a, pEmptyOptional);
			assertArray(b.getParams(m3)).is(pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A1.class, a1b, "bar");
		b2p.add(A1.class, a1b, "bar");
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2, m1, m2, m3)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pA1a, pEmptyOptional);
			assertArray(b.getParams(m1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2)).is(pA1a, pEmptyOptional);
			assertArray(b.getParams(m3)).is(pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A2.class, a2a, "bar");
		b2p.add(A2.class, a2a, "bar");
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2, m1, m2, m3)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pA1a, pA2a);
			assertArray(b.getParams(m1)).is(pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(m2)).is(pA1a, pA2a);
			assertArray(b.getParams(m3)).is(pA1a, pEmptyOptional, pIsBeanStore);
		}

		b1p.add(A2.class, a2a, null);
		b2p.add(A2.class, a2a, null);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2, m1, m2, m3)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pA1a, pA2a, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pA1a, pA2a);
			assertArray(b.getParams(m1)).is(pA1a, pA2a, pIsBeanStore);
			assertArray(b.getParams(m2)).is(pA1a, pA2a);
			assertArray(b.getParams(m3)).is(pA1a, pA2a, pIsBeanStore);
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

	@Test
	public void b02_getParams_innerClass() {

		Predicate<Object> pEmptyOptional = x -> !((Optional<?>)x).isPresent();
		Predicate<Object> pIsBeanStore = x -> x instanceof BeanStore;
		Predicate<Object> pNull = x -> x == null;
		Predicate<Object> pThis = x -> x == this;
		Predicate<Object> pA1a = x -> x==a1a;
		Predicate<Object> pA2a = x -> ((Optional<?>)x).get()==a2a;

		BeanStore b1p = BeanStore.create().outer(this).build();
		BeanStore b1c = BeanStore.create().outer(this).parent(b1p).build();
		BeanStore b2p = BeanStore.create().outer(this).threadSafe().build();
		BeanStore b2c = BeanStore.create().outer(this).parent(b1p).threadSafe().build();

		ClassInfo ci = ClassInfo.of(B2.class);
		ConstructorInfo c1 = ci.getPublicConstructor(x -> x.hasParamTypes(BeanStore_Test.class, A1.class, Optional.class, BeanStore.class));
		ConstructorInfo c2 = ci.getPublicConstructor(x -> x.hasParamTypes(BeanStore_Test.class, A1.class, Optional.class));

		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertString(b.getMissingParams(c1)).is(A1n);
			assertString(b.getMissingParams(c2)).is(A1n+"@foo");
			assertBoolean(b.hasAllParams(c1)).isFalse();
			assertBoolean(b.hasAllParams(c2)).isFalse();
		}

		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertArray(b.getParams(c1)).is(pThis, pNull, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pThis, pNull, pEmptyOptional);
		}

		b1p.add(A1.class, a1a);
		b2p.add(A1.class, a1a);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			assertString(b.getMissingParams(c1)).isNull();
			assertString(b.getMissingParams(c2)).is(A1n+"@foo");
			assertBoolean(b.hasAllParams(c1)).isTrue();
			assertBoolean(b.hasAllParams(c2)).isFalse();
			assertArray(b.getParams(c1)).is(pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pThis, pNull, pEmptyOptional);
		}

		b1p.add(A1.class, a1a, "foo");
		b2p.add(A1.class, a1a, "foo");
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pThis, pA1a, pEmptyOptional);
		}

		b1p.add(A1.class, a1b, "bar");
		b2p.add(A1.class, a1b, "bar");
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pThis, pA1a, pEmptyOptional);
		}

		b1p.add(A2.class, a2a, "bar");
		b2p.add(A2.class, a2a, "bar");
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2)) {
				assertString(b.getMissingParams(e)).isNull();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pThis, pA1a, pEmptyOptional, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pThis, pA1a, pA2a);
		}

		b1p.add(A2.class, a2a, null);
		b2p.add(A2.class, a2a, null);
		for (BeanStore b : array(b1p, b1c, b2p, b2c)) {
			for (ExecutableInfo e : array(c1, c2)) {
				assertString(b.getMissingParams(e)).isEmpty();
				assertBoolean(b.hasAllParams(e)).isTrue();
			}
			assertArray(b.getParams(c1)).is(pThis, pA1a, pA2a, pIsBeanStore);
			assertArray(b.getParams(c2)).is(pThis, pA1a, pA2a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// createMethodFinder()
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public A1 a;
	}

	@Test
	public void c00_createMethodFinder_invalidArgs() throws Exception {
		BeanStore b = BeanStore.create().build();

		assertThrown(()->b.createMethodFinder(null)).asMessage().is("Method cannot be used without outer bean definition.");
		assertThrown(()->b.createMethodFinder((Class<?>)null,"")).asMessage().is("Argument 'beanType' cannot be null.");
		assertThrown(()->b.createMethodFinder(String.class,null)).asMessage().is("Argument 'resourceClass' cannot be null.");
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

	@Test
	public void c01_createMethodFinder_instanceMethods() throws Exception {
		C1 x = new C1();
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.create().parent(b1p).build();
		BeanStore b2p = BeanStore.create().build();
		BeanStore b2c = BeanStore.create().outer(x).parent(b2p).build();
		BeanStore b3p = BeanStore.create().build();
		BeanStore b3c = BeanStore.create().outer(this).parent(b3p).build();

		for (String m : array("createA0", "createA2", "createA3", "createA4", "createA5", "createA6")) {
			for (BeanStore b : array(b1c, b2c, b3c)) {
				assertObject(b.createMethodFinder(C.class, x).find(m).run()).isNull();
				assertObject(b.createMethodFinder(C.class, C1.class).find(m).run()).isNull();
			}
			for (BeanStore b : array(b2c, b3c)) {
				assertObject(b.createMethodFinder(C.class).find(m).run()).isNull();
			}
		}
		assertObject(b1c.createMethodFinder(C.class, x).find("createA1").run()).isNotNull();
		assertObject(b1c.createMethodFinder(C.class, C1.class).find("createA1").run()).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createA1").run()).isNotNull();
		assertObject(b3c.createMethodFinder(C.class).find("createA1").run()).isNull();
		assertThrown(()->b1c.createMethodFinder(C.class, x).find("createA7").run()).asMessage().isContains("foo");
		assertObject(b1c.createMethodFinder(C.class, C1.class).find("createA7").run()).isNull();
		assertThrown(()->b2c.createMethodFinder(C.class).find("createA7").run()).asMessage().isContains("foo");
		assertObject(b3c.createMethodFinder(C.class).find("createA7").run()).isNull();
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

	@Test
	public void c02_createMethodFinder_staticMethods() throws Exception {
		C2 x = new C2();
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.create().parent(b1p).build();
		BeanStore b2p = BeanStore.create().build();
		BeanStore b2c = BeanStore.create().outer(x).parent(b2p).build();
		BeanStore b3p = BeanStore.create().build();
		BeanStore b3c = BeanStore.create().outer(this).parent(b3p).build();

		for (String m : array("createB0", "createB2", "createB3", "createB4", "createB5", "createB6")) {
			for (BeanStore b : array(b1c, b2c, b3c)) {
				assertObject(b.createMethodFinder(C.class, x).find(m).run()).isNull();
				assertObject(b.createMethodFinder(C.class, C2.class).find(m).run()).isNull();
			}
			for (BeanStore b : array(b2c, b3c)) {
				assertObject(b.createMethodFinder(C.class).find(m).run()).isNull();
			}
		}
		assertObject(b1c.createMethodFinder(C.class, x).find("createB1").run()).isNotNull();
		assertObject(b1c.createMethodFinder(C.class, C2.class).find("createB1").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createB1").run()).isNotNull();
		assertObject(b3c.createMethodFinder(C.class).find("createB1").run()).isNull();
		assertThrown(()->b1c.createMethodFinder(C.class, x).find("createB7").run()).asMessage().isContains("foo");
		assertThrown(()->b1c.createMethodFinder(C.class, C2.class).find("createB7").run()).asMessage().isContains("foo");
		assertThrown(()->b2c.createMethodFinder(C.class).find("createB7").run()).asMessage().isContains("foo");
		assertObject(b3c.createMethodFinder(C.class).find("createB7").run()).isNull();
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

	@Test
	public void c03_createMethodFinder_beanMatching() throws Exception {
		C3 x = new C3();
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.create().parent(b1p).build();
		BeanStore b2p = BeanStore.create().build();
		BeanStore b2c = BeanStore.create().outer(x).parent(b2p).build();
		BeanStore b3p = BeanStore.create().build();
		BeanStore b3c = BeanStore.create().outer(this).parent(b3p).build();

		for (BeanStore b : array(b1c, b2c, b3c)) {
			assertObject(b.createMethodFinder(C.class, x).find("createC1").run()).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC2").run()).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC3").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC6").run()).isNotNull();
		}

		b1p.addBean(A1.class, new A1());
		b2p.addBean(A1.class, new A1());
		b3p.addBean(A1.class, new A1());
		for (BeanStore b : array(b1c, b2c)) {
			assertObject(b.createMethodFinder(C.class, x).find("createC1").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC3").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC6").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC1").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC3").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC6").run()).isNotNull();
		}
		assertObject(b2c.createMethodFinder(C.class).find("createC1").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC2").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC3").run().a).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC4").run()).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC5").run().a).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC6").run()).isNotNull();
		for (String m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertObject(b3c.createMethodFinder(C.class).find(m).run()).isNull();
		}

		b1p.clear().addSupplier(A1.class, ()->new A1());
		b2p.clear().addSupplier(A1.class, ()->new A1());
		for (BeanStore b : array(b1c, b2c)) {
			assertObject(b.createMethodFinder(C.class, x).find("createC1").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC3").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC6").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC1").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC3").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC6").run()).isNotNull();
		}
		assertObject(b2c.createMethodFinder(C.class).find("createC1").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC2").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC3").run().a).isType(A1.class);
		assertObject(b2c.createMethodFinder(C.class).find("createC4").run()).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC5").run().a).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC6").run()).isNotNull();
		for (String m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertObject(b3c.createMethodFinder(C.class).find(m).run()).isNull();
		}

		b1p.clear().add(A1.class, null);
		b2p.clear().add(A1.class, null);
		for (BeanStore b : array(b1c, b2c)) {
			assertObject(b.createMethodFinder(C.class, x).find("createC1").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC3").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC6").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC1").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC3").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC4").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC5").run().a).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC6").run()).isNotNull();
		}
		assertObject(b2c.createMethodFinder(C.class).find("createC1").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC2").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC3").run().a).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC4").run()).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC5").run().a).isNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC6").run()).isNotNull();
		for (String m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertObject(b3c.createMethodFinder(C.class).find(m).run()).isNull();
		}

		b1p.clear().addBean(A1.class, new A1()).add(A1.class, new A1(), "Foo");
		b2p.clear().addBean(A1.class, new A1()).add(A1.class, new A1(), "Foo");
		for (BeanStore b : array(b1c, b2c)) {
			assertObject(b.createMethodFinder(C.class, x).find("createC1").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC3").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC4").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC5").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, x).find("createC6").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC1").run()).isNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC2").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC3").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC4").run()).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC5").run().a).isNotNull();
			assertObject(b.createMethodFinder(C.class, C3.class).find("createC6").run()).isNotNull();
		}
		assertObject(b2c.createMethodFinder(C.class).find("createC1").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC2").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC3").run().a).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC4").run()).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC5").run().a).isNotNull();
		assertObject(b2c.createMethodFinder(C.class).find("createC6").run()).isNotNull();
		for (String m : array("createC1","createC2","createC3","createC4","createC5","createC6")) {
			assertObject(b3c.createMethodFinder(C.class).find(m).run()).isNull();
		}
	}

	// Bean matching.
	public static class C4 {
		public static String createC1(A1 a) { return "createC1"; }
		public static String createC2() { return "createC2"; }
	}

	@Test
	public void c04_createMethodFinder_beanMatching_requiredArgs() throws Exception {
		C4 x = new C4();
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.create().outer(x).parent(b1p).build();

		assertString(b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run()).is("createC2");
		assertString(b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run()).is("createC2");

		b1p.add(A1.class, null);
		assertString(b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run()).is("createC2");
		assertString(b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run()).is("createC1");

		b1p.clear();
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC1").thenFind("createC2").run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC2").thenFind("createC1").run()).is("createC2");
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run()).is("createC1");

		assertString(b1c.createMethodFinder(String.class).withDefault("X").run()).is("X");
		assertString(b1c.createMethodFinder(String.class).withDefault(()->"X").run()).is("X");

		b1c.createMethodFinder(String.class).withDefault("X").run(y -> assertString(y).is("X"));
	}

	// Bean matching.
	public class C5 {
		public String createC1(A1 a) { return "createC1"; }
		public String createC2() { return "createC2"; }
	}

	@Test
	public void c05_createMethodFinder_beanMatching_requiredArgs_innerClass() throws Exception {
		C5 x = new C5();
		BeanStore b1p = BeanStore.create().build();
		BeanStore b1c = BeanStore.create().outer(x).parent(b1p).build();

		assertString(b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run()).is("createC2");
		assertString(b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run()).is("createC2");

		b1p.add(A1.class, null);
		assertString(b1c.createMethodFinder(String.class).find("createC1").thenFind("createC2").run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).find("createC2").thenFind("createC1").run()).is("createC2");
		assertString(b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run()).is("createC1");

		b1p.clear();
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC1").thenFind("createC2").run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find("createC2").thenFind("createC1").run()).is("createC2");
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).run()).is("createC1");
		assertString(b1c.createMethodFinder(String.class).addBean(A1.class, null).find(x2->x2.hasName("createC2") && x2.hasAllArgs(A1.class)).thenFind(x2->x2.hasName("createC1") && x2.hasAllArgs(A1.class)).run()).is("createC1");

		assertString(b1c.createMethodFinder(String.class).withDefault("X").run()).is("X");
		assertString(b1c.createMethodFinder(String.class).withDefault(()->"X").run()).is("X");

		b1c.createMethodFinder(String.class).withDefault("X").run(y -> assertString(y).is("X"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// createBean()
	//-----------------------------------------------------------------------------------------------------------------

	public static class D1a {}
	public class D1b {}

	@Test
	public void d01_createBean_basic() {
		BeanStore bs = BeanStore.create().outer(new BeanStore_Test()).build();
		assertObject(bs.createBean(D1a.class).run()).isNotNull();
		assertObject(bs.createBean(D1b.class).run()).isNotNull();
		assertObject(bs.createBean(null).run()).isNull();
	}

	public static class D2 {
		public static D2 create() { return d2; }
	}
	public static D2 d2 = new D2();

	@Test
	public void d02_createBean_staticCreator_create() {
		BeanStore bs = BeanStore.INSTANCE;
		assertObject(bs.createBean(D2.class).run()).is(d2);
	}

	public static abstract class D3 {
		public static D3 getInstance() { return d3; }
	}
	public static D3 d3 = new D3() {};

	@Test
	public void d03_createBean_staticCreator_getInstance() {
		BeanStore bs = BeanStore.INSTANCE;
		assertObject(bs.createBean(D3.class).run()).is(d3);
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

	@Test
	public void d04_createBean_staticCreator_invalidSignatures() {
		BeanStore bs = BeanStore.INSTANCE;
		assertObject(bs.createBean(D4a.class).run()).isNotAny(d4a1, d4a2, d4a3, d4a4);
		assertObject(bs.createBean(D4b.class).run()).isNotAny(d4b1, d4b2);
		assertObject(bs.createBean(D4c.class).run()).isNotNull();
	}

	public static class D5 {
		public static D5 create() { return d5a; }
		public static D5 create(Integer i, String s) { return d5b; }
		public static D5 create(Integer i) { return d5c; }
		protected D5() {}
	}
	public static D5 d5a = new D5(), d5b = new D5(), d5c = new D5();

	@Test
	public void d05_createBean_staticCreator_withBeans() {
		BeanStore bs = BeanStore.create().build();
		assertObject(bs.createBean(D5.class).run()).is(d5a);
		bs.add(Integer.class, 1);
		assertObject(bs.createBean(D5.class).run()).is(d5c);
		bs.add(String.class, "x");
		assertObject(bs.createBean(D5.class).run()).is(d5b);
		bs.removeBean(Integer.class);
		assertObject(bs.createBean(D5.class).run()).is(d5a);
	}

	public static class D6 {
		public String s;
		public static D6 create() { return new D6("0"); }
		protected D6(String s) {
			this.s = s;
		}
	}

	@Test
	public void d06_createBean_staticCreator_ignoredWithBuilder() {
		BeanStore bs = BeanStore.INSTANCE;
		assertString(bs.createBean(D6.class).builder(String.class, "1").run().s).is("1");
	}

	public static class D7 {
		public String a;
		public static D7 create(Optional<String> s) { return new D7(s.orElse("X")); }
		protected D7(String s) { a = s; }
	}

	@Test
	public void d07_createBean_staticCreator_withOptional() {
		BeanStore bs = BeanStore.create().build();
		assertString(bs.createBean(D7.class).run().a).is("X");
		bs.add(String.class, "bar");
		assertString(bs.createBean(D7.class).run().a).is("bar");
	}

	public static class D8 {
		public String a = "foo";
		public static D8 create(Optional<String> s, Integer i) { return new D8(s.orElse(null) + "," + i); }
		private D8(String s) { a = s; }
	}

	@Test
	public void d08_createBean_staticCreator_missingPrereqs() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.createBean(D8.class).run()).asMessage().is("Could not instantiate class org.apache.juneau.cp.BeanStore_Test$D8: Static creator found but could not find prerequisites: Integer.");
		bs.add(Integer.class, 1);
		assertString(bs.createBean(D8.class).run().a).is("null,1");
		bs.add(String.class, "bar");
		assertString(bs.createBean(D8.class).run().a).is("bar,1");
	}

	public static abstract class D9a {
		public D9a() {}
	}

	public interface D9b {}

	@Test
	public void d09_createBean_staticCreator_withBeans() {
		BeanStore bs = BeanStore.INSTANCE;
		assertThrown(()->bs.createBean(D9a.class).run()).asMessage().is("Could not instantiate class "+D9a.class.getName()+": Class is abstract.");
		assertThrown(()->bs.createBean(D9b.class).run()).asMessage().is("Could not instantiate class "+D9b.class.getName()+": Class is an interface.");
	}

	public static class D10 {
		public String a;
		public D10(String s) { a = "s="+s; }
		public D10(Integer i) { a = "i="+i; }
		public D10(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test
	public void d10_createBean_constructors_public() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.createBean(D10.class).run()).asMessage().is("Could not instantiate class "+D10.class.getName()+": Public constructor found but could not find prerequisites: Integer or Integer,String or String.");
		bs.add(String.class, "foo");
		assertString(bs.createBean(D10.class).run().a).is("s=foo");
		bs.add(Integer.class, 1);
		assertString(bs.createBean(D10.class).run().a).is("s=foo,i=1");
		bs.removeBean(String.class);
		assertString(bs.createBean(D10.class).run().a).is("i=1");
	}

	public static class D11 {
		public String a;
		protected D11(String s) { a = "s="+s; }
		protected D11(Integer i) { a = "i="+i; }
		protected D11(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test
	public void d11_createBean_constructors_protected() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.createBean(D11.class).run()).asMessage().is("Could not instantiate class "+D11.class.getName()+": Protected constructor found but could not find prerequisites: Integer or Integer,String or String.");
		bs.add(String.class, "foo");
		assertString(bs.createBean(D11.class).run().a).is("s=foo");
		bs.add(Integer.class, 1);
		assertString(bs.createBean(D11.class).run().a).is("s=foo,i=1");
		bs.removeBean(String.class);
		assertString(bs.createBean(D11.class).run().a).is("i=1");
	}

	public static class D12 {
		public String a;
		public D12(String s) { a = "s="+s; }
		protected D12(String s, Integer i) { a = "s="+s+",i="+i; }
	}

	@Test
	public void d12_createBean_constructors_publicOverProtected() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.createBean(D12.class).run()).asMessage().is("Could not instantiate class "+D12.class.getName()+": Public constructor found but could not find prerequisites: String.");
		bs.add(String.class, "foo");
		bs.add(Integer.class, 1);
		assertString(bs.createBean(D12.class).run().a).is("s=foo");
	}

	public static class D13 {
		private D13() {}
	}

	@Test
	public void d13_createBean_constructors_private() {
		BeanStore bs = BeanStore.INSTANCE;
		assertThrown(()->bs.createBean(D13.class).run()).asMessage().is("Could not instantiate class "+D13.class.getName()+": No public/protected constructors found.");
	}

	public static class D14 {
		public String a;
		public D14(@Named("foo") String o) { a = o.toString(); }
		public D14(@Named("foo") String o, Integer i) { a = o.toString() + "," + i; }
	}

	@Test
	public void d14_createBean_constructors_namedBean() {
		BeanStore bs = BeanStore.create().build();
		assertThrown(()->bs.createBean(D14.class).run()).asMessage().is("Could not instantiate class "+D14.class.getName()+": Public constructor found but could not find prerequisites: Integer,String@foo or String@foo.");
		bs.add(String.class, "bar", "foo");
		assertString(bs.createBean(D14.class).run().a).is("bar");
	}

	public class D15 {
		public String a;
		public D15(@Named("foo") String o) { a = o.toString(); }
		public D15(@Named("foo") String o, Integer i) { a = o.toString() + "," + i; }
	}

	@Test
	public void d15_createBean_constructors_namedBean_withOuter() {
		BeanStore bs = BeanStore.create().outer(new BeanStore_Test()).build();
		assertThrown(()->bs.createBean(D15.class).run()).asMessage().is("Could not instantiate class "+D15.class.getName()+": Public constructor found but could not find prerequisites: Integer,String@foo or String@foo.");
		bs.add(String.class, "bar", "foo");
		assertString(bs.createBean(D15.class).run().a).is("bar");
	}

	public static class D16 {
		public String a;
		public static Builder create() { return new Builder(); }
		public static class Builder {
			public String b;
		}
		protected D16(Builder b) { a = b.b; }
	}

	@Test
	public void d16_createBean_builders() {
		BeanStore bs = BeanStore.create().build();
		D16.Builder b = D16.create();
		b.b = "foo";
		assertString(bs.createBean(D16.class).builder(D16.Builder.class, b).run().a).is("foo");
	}

	public static class D17 {
		public String a;
		public static Builder create() { return new Builder(); }
		public static class Builder {
			public String b;
		}
		protected D17(Builder b, Integer i) { a = b.b; }
		protected D17(Integer i) {}
		D17(String s) {}
		protected D17(Builder b) { a = b.b; }
	}

	@Test
	public void d17_createBean_builders_inherent() {
		BeanStore bs = BeanStore.create().build();
		assertString(bs.createBean(D17.class).run().a).isNull();
		assertThrown(()->bs.createBean(D17.class).builder(Boolean.class, true).run()).asMessage().is("Could not instantiate class "+D17.class.getName()+": Protected constructor found but could not find prerequisites: Builder or Builder,Integer or Integer.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	@SafeVarargs
	private static <T> T[] array(T...t) {
		return t;
	}
}
