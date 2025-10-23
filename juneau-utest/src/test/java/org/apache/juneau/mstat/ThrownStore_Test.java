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
package org.apache.juneau.mstat;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.rest.stats.*;
import org.junit.jupiter.api.*;

class ThrownStore_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_testBasic() {

		var t1 = new Throwable();
		t1.fillInStackTrace();
		var t2 = new Throwable();
		t2.fillInStackTrace();

		var db = new ThrownStore();

		assertEquals(1, db.add(t1).getCount());
		assertEquals(2, db.add(t1).getCount());
		assertEquals(1, db.add(t2).getCount());

		assertEquals(db.getStats(t1).get().getHash(), db.getStats(t1).get().getHash());
		assertNotEquals(db.getStats(t2).get().getHash(), db.getStats(t1).get().getHash());
	}

	@Test void a02_getStats() {

		var t1 = new Throwable();
		t1.fillInStackTrace();
		var t2 = new Throwable();
		t2.fillInStackTrace();

		var db = new ThrownStore();

		db.add(t1);
		db.add(t1);
		db.add(t2);

		List<ThrownStats> l = db.getStats();  // Should be a snapshot.
		db.add(t1);

		assertSize(2, l);
		assertEquals(2, l.get(0).getCount());
		assertEquals(1, l.get(1).getCount());
	}

	@Test void a03_reset() {
		var t1 = new Throwable();
		t1.fillInStackTrace();

		var db = new ThrownStore();
		db.add(t1);
		db.reset();

		assertEmpty(db.getStats(t1));
	}

	@Test void a04_sameStackTraces() {
		var db = new ThrownStore();

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

		assertEquals(2, db.getStats(t1).get().getCount());
		assertEquals(2, db.getStats(t2).get().getCount());
	}

	@Test void a05_slightlyDifferentStackTraces() {
		var db = new ThrownStore();

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

		assertEquals(1, db.getStats(t1).get().getCount());
		assertEquals(1, db.getStats(t2).get().getCount());
	}

	@Test void a06_proxyElements() {
		var db = new ThrownStore();

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

		assertEquals(2, db.getStats(t1).get().getCount());
		assertEquals(2, db.getStats(t2).get().getCount());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_builder_default() {
		assertInstanceOf(ThrownStore.class, ThrownStore.create().build());
	}

	public static class B1 extends ThrownStore{}

	@Test void b02_builder_implClass() {
		assertInstanceOf(B1.class, ThrownStore.create().type(B1.class).build());
	}

	public static class B4 extends ThrownStore {
		public B4(ThrownStore.Builder b) throws Exception {
			throw new RuntimeException("foobar");
		}
	}

	@Test void b04_builder_implClass_bad() {
		assertThrowsWithMessage(Exception.class, "foobar", ()->ThrownStore.create().type(B4.class).build());
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

	@Test void b05_builder_beanFactory() {
		var bs = BeanStore.create().build();

		assertThrowsWithMessage(Exception.class, "Public constructor found but could not find prerequisites: B5a", ()->ThrownStore.create(bs).type(B5b.class).build());
		assertInstanceOf(B5c.class, ThrownStore.create(bs).type(B5c.class).build());

		bs.addBean(B5a.class, new B5a());
		assertInstanceOf(B5b.class, ThrownStore.create(bs).type(B5b.class).build());
		assertInstanceOf(B5c.class, ThrownStore.create(bs).type(B5c.class).build());
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

	@Test void b06_statsImplClass() {
		var bs = BeanStore.create().build();

		var t1 = new Throwable();
		t1.fillInStackTrace();

		assertThrowsWithMessage(Exception.class, "Public constructor found but could not find prerequisites: B6a", ()->ThrownStore.create(bs).statsImplClass(B6b.class).build().add(t1));
		assertInstanceOf(B6c.class, ThrownStore.create(bs).statsImplClass(B6c.class).build().add(t1));

		bs.addBean(B6a.class, new B6a());
		assertInstanceOf(B6b.class, ThrownStore.create(bs).statsImplClass(B6b.class).build().add(t1));
		assertInstanceOf(B6c.class, ThrownStore.create(bs).statsImplClass(B6c.class).build().add(t1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ThrownStats tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_thrownStats_basic() {
		var t1 = new Throwable("foo");
		t1.fillInStackTrace();
		var t2 = new Throwable("bar", t1);
		t2.fillInStackTrace();

		var store = ThrownStore.create().build();

		var stats = store.add(t2);
		assertNotEquals(0L, stats.getHash());
		assertNotEquals(0L, stats.getGuid());
		assertNotEquals(0L, stats.getFirstOccurrence());
		assertNotEquals(0L, stats.getLastOccurrence());
		assertEquals("bar", stats.getFirstMessage());
		assertContains("org.apache.juneau", r(stats.getStackTrace()));
		assertContains("bar", stats);

		stats = stats.clone();
		assertNotEquals(0L, stats.getHash());
		assertNotEquals(0L, stats.getGuid());
		assertNotEquals(0L, stats.getFirstOccurrence());
		assertNotEquals(0L, stats.getLastOccurrence());
		assertEquals("bar", stats.getFirstMessage());
		assertContains("org.apache.juneau", r(stats.getStackTrace()));
		assertContains("bar", stats);

		stats = stats.getCausedBy().get();
		assertNotEquals(0L, stats.getHash());
		assertNotEquals(0L, stats.getGuid());
		assertNotEquals(0L, stats.getFirstOccurrence());
		assertNotEquals(0L, stats.getLastOccurrence());
		assertEquals("foo", stats.getFirstMessage());
		assertContains("org.apache.juneau", r(stats.getStackTrace()));
		assertContains("foo", stats);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ThrownStore tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class D1 {}
	public static class D2 {}

	@Test void d01_ignoreClasses() {
		var db = ThrownStore.create().ignoreClasses(D1.class,D2.class,ThrownStore_Test.class).build();

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

		assertList(db.getStats(t1).get().getStackTrace(), "Foo.bar(Foo.class:1)", "Foo.baz(Foo.class:2)", "<ignored>", "<ignored>", "Object.baz(Object.class:5)");
		assertList(db.getStats(t2).get().getStackTrace(), "Foo.bar(Foo.class:1)", "Foo.baz(Foo.class:2)", "<ignored>", "<ignored>", "Object.baz(Object.class:5)");

		assertEquals(2, db.getStats(t1).get().getCount());
		assertEquals(2, db.getStats(t2).get().getCount());

		var db2 = ThrownStore.create().parent(db).build();

		db2.add(t1);
		db2.add(t2);

		assertList(db2.getStats(t1).get().getStackTrace(), "Foo.bar(Foo.class:1)", "Foo.baz(Foo.class:2)", "<ignored>", "<ignored>", "Object.baz(Object.class:5)");
		assertList(db2.getStats(t2).get().getStackTrace(), "Foo.bar(Foo.class:1)", "Foo.baz(Foo.class:2)", "<ignored>", "<ignored>", "Object.baz(Object.class:5)");

		assertEquals(2, db2.getStats(t1).get().getCount());
		assertEquals(2, db2.getStats(t2).get().getCount());
		assertEquals(4, db.getStats(t1).get().getCount());
		assertEquals(4, db.getStats(t2).get().getCount());
	}
}