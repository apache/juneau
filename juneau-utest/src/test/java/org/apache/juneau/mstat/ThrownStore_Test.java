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
package org.apache.juneau.mstat;

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.stats.*;
import org.junit.*;

@SuppressWarnings("serial")
@FixMethodOrder(NAME_ASCENDING)
public class ThrownStore_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_testBasic() {

		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		ThrownStore db = new ThrownStore();

		assertInteger(db.add(t1).getCount()).is(1);
		assertInteger(db.add(t1).getCount()).is(2);
		assertInteger(db.add(t2).getCount()).is(1);

		assertLong(db.getStats(t1).get().getHash()).is(db.getStats(t1).get().getHash());
		assertLong(db.getStats(t1).get().getHash()).isNot(db.getStats(t2).get().getHash());
	}

	@Test
	public void a02_getStats() {

		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		ThrownStore db = new ThrownStore();

		db.add(t1);
		db.add(t1);
		db.add(t2);

		List<ThrownStats> l = db.getStats();  // Should be a snapshot.
		db.add(t1);

		assertList(l).isSize(2);
		assertInteger(l.get(0).getCount()).is(2);
		assertInteger(l.get(1).getCount()).is(1);
	}

	@Test
	public void a03_reset() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();

		ThrownStore db = new ThrownStore();
		db.add(t1);
		db.reset();

		assertOptional(db.getStats(t1)).isNull();
	}

	@Test
	public void a04_sameStackTraces() {
		ThrownStore db = new ThrownStore();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};

		db.add(t1);
		db.add(t2);

		assertInteger(db.getStats(t1).get().getCount()).is(2);
		assertInteger(db.getStats(t2).get().getCount()).is(2);
	}

	@Test
	public void a05_slightlyDifferentStackTraces() {
		ThrownStore db = new ThrownStore();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 7)
				};
			}
		};

		db.add(t1);
		db.add(t2);

		assertInteger(db.getStats(t1).get().getCount()).is(1);
		assertInteger(db.getStats(t2).get().getCount()).is(1);
	}

	@Test
	public void a06_proxyElements() {
		ThrownStore db = new ThrownStore();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop$1", "baz", "Stop.class", 6),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop$2", "baz", "Stop.class", 6),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};

		db.add(t1);
		db.add(t2);

		assertInteger(db.getStats(t1).get().getCount()).is(2);
		assertInteger(db.getStats(t2).get().getCount()).is(2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_builder_default() {
		assertObject(ThrownStore.create().build()).isType(ThrownStore.class);
	}

	public static class B1 extends ThrownStore{}

	@Test
	public void b02_builder_implClass() {
		assertObject(ThrownStore.create().type(B1.class).build()).isType(B1.class);
	}

	public static class B4 extends ThrownStore {
		public B4(ThrownStore.Builder b) throws Exception {
			throw new RuntimeException("foobar");
		}
	}

	@Test
	public void b04_builder_implClass_bad() {
		assertThrown(()->ThrownStore.create().type(B4.class).build()).asMessages().isContains("foobar");
	}

	public static class B5a {}

	public static class B5b extends ThrownStore {
		public B5b(ThrownStore.Builder b, B5a x) throws Exception {
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	public static class B5c extends ThrownStore {
		public B5c(ThrownStore.Builder b, Optional<B5a> x) throws Exception {
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	@Test
	public void b05_builder_beanFactory() throws Exception {
		BeanStore bs = BeanStore.create().build();

		assertThrown(()->ThrownStore.create(bs).type(B5b.class).build()).asMessages().isAny(contains("Public constructor found but could not find prerequisites: B5a"));
		assertObject(ThrownStore.create(bs).type(B5c.class).build()).isType(B5c.class);

		bs.addBean(B5a.class, new B5a());
		assertObject(ThrownStore.create(bs).type(B5b.class).build()).isType(B5b.class);
		assertObject(ThrownStore.create(bs).type(B5c.class).build()).isType(B5c.class);
	}

	public static class B6a {}

	public static class B6b extends ThrownStats {
		public B6b(ThrownStats.Builder b, B6a x) throws Exception {
			super(b);
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	public static class B6c extends ThrownStats {
		public B6c(ThrownStats.Builder b, Optional<B6a> x) throws Exception {
			super(b);
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	@Test
	public void b06_statsImplClass() throws Exception {
		BeanStore bs = BeanStore.create().build();

		Throwable t1 = new Throwable();
		t1.fillInStackTrace();

		assertThrown(()->ThrownStore.create(bs).statsImplClass(B6b.class).build().add(t1)).asMessages().isAny(contains("Public constructor found but could not find prerequisites: B6a"));
		assertObject(ThrownStore.create(bs).statsImplClass(B6c.class).build().add(t1)).isType(B6c.class);

		bs.addBean(B6a.class, new B6a());
		assertObject(ThrownStore.create(bs).statsImplClass(B6b.class).build().add(t1)).isType(B6b.class);
		assertObject(ThrownStore.create(bs).statsImplClass(B6c.class).build().add(t1)).isType(B6c.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ThrownStats tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_thrownStats_basic() throws Exception {
		Throwable t1 = new Throwable("foo");
		t1.fillInStackTrace();
		Throwable t2 = new Throwable("bar", t1);
		t2.fillInStackTrace();

		ThrownStore store = ThrownStore.create().build();

		ThrownStats stats = store.add(t2);
		assertLong(stats.getHash()).isNot(0l);
		assertLong(stats.getGuid()).isNot(0l);
		assertLong(stats.getFirstOccurrence()).isNot(0l);
		assertLong(stats.getLastOccurrence()).isNot(0l);
		assertString(stats.getFirstMessage()).is("bar");
		assertObject(stats.getStackTrace()).asJson().isContains("org.apache.juneau");
		assertObject(stats).asString().isContains("bar");

		stats = stats.clone();
		assertLong(stats.getHash()).isNot(0l);
		assertLong(stats.getGuid()).isNot(0l);
		assertLong(stats.getFirstOccurrence()).isNot(0l);
		assertLong(stats.getLastOccurrence()).isNot(0l);
		assertString(stats.getFirstMessage()).is("bar");
		assertObject(stats.getStackTrace()).asJson().isContains("org.apache.juneau");
		assertObject(stats).asString().isContains("bar");

		stats = stats.getCausedBy().get();
		assertLong(stats.getHash()).isNot(0l);
		assertLong(stats.getGuid()).isNot(0l);
		assertLong(stats.getFirstOccurrence()).isNot(0l);
		assertLong(stats.getLastOccurrence()).isNot(0l);
		assertString(stats.getFirstMessage()).is("foo");
		assertObject(stats.getStackTrace()).asJson().isContains("org.apache.juneau");
		assertObject(stats).asString().isContains("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// ThrownStore tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class D1 {}
	public static class D2 {}


	@Test
	public void d01_ignoreClasses() {
		ThrownStore db = ThrownStore.create().ignoreClasses(D1.class,D2.class,ThrownStore_Test.class).build();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement(D1.class.getName(), "baz", "D1.class", 3),
					new StackTraceElement(D1.class.getName()+"$X", "baz", "D1.X.class", 4),
					new StackTraceElement("Object", "baz", "Object.class", 5)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement(D2.class.getName(), "baz", "D2.class", 3),
					new StackTraceElement(D2.class.getName()+"$X", "baz", "D2.X.class", 4),
					new StackTraceElement("Object", "baz", "Object.class", 5)
				};
			}
		};

		db.add(t1);
		db.add(t2);

		assertObject(db.getStats(t1).get().getStackTrace()).asJson().is("['Foo.bar(Foo.class:1)','Foo.baz(Foo.class:2)','<ignored>','<ignored>','Object.baz(Object.class:5)']");
		assertObject(db.getStats(t2).get().getStackTrace()).asJson().is("['Foo.bar(Foo.class:1)','Foo.baz(Foo.class:2)','<ignored>','<ignored>','Object.baz(Object.class:5)']");

		assertInteger(db.getStats(t1).get().getCount()).is(2);
		assertInteger(db.getStats(t2).get().getCount()).is(2);

		ThrownStore db2 = ThrownStore.create().parent(db).build();

		db2.add(t1);
		db2.add(t2);

		assertObject(db2.getStats(t1).get().getStackTrace()).asJson().is("['Foo.bar(Foo.class:1)','Foo.baz(Foo.class:2)','<ignored>','<ignored>','Object.baz(Object.class:5)']");
		assertObject(db2.getStats(t2).get().getStackTrace()).asJson().is("['Foo.bar(Foo.class:1)','Foo.baz(Foo.class:2)','<ignored>','<ignored>','Object.baz(Object.class:5)']");

		assertInteger(db2.getStats(t1).get().getCount()).is(2);
		assertInteger(db2.getStats(t2).get().getCount()).is(2);
		assertInteger(db.getStats(t1).get().getCount()).is(4);
		assertInteger(db.getStats(t2).get().getCount()).is(4);
	}
}
