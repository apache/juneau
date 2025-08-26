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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.rest.stats.*;
import org.junit.jupiter.api.*;

class MethodExecStore_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Builder tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_builder_default() {
		assertType(MethodExecStore.class, MethodExecStore.create().build());
	}

	public static class A1 extends MethodExecStore{
		protected A1(Builder builder) {
			super(builder);
		}
	}

	@Test void a02_builder_implClass() {
		assertType(A1.class, MethodExecStore.create().type(A1.class).build());
	}

	public static class A4 extends MethodExecStore {
		public A4(MethodExecStore.Builder b) throws Exception {
			super(b);
			throw new RuntimeException("foobar");
		}
	}

	@Test void a04_builder_implClass_bad() {
		assertThrowsWithMessage(Exception.class, "foobar", ()->MethodExecStore.create().type(A4.class).build());
	}

	public static class A5a {}

	public static class A5b extends MethodExecStore {
		public A5b(MethodExecStore.Builder b, A5a x) throws Exception {
			super(b);
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	public static class A5c extends MethodExecStore {
		public A5c(MethodExecStore.Builder b, Optional<A5a> x) throws Exception {
			super(b);
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	@Test void a05_builder_beanFactory() {
		var bs = BeanStore.create().build();

		assertThrowsWithMessage(Exception.class, "Public constructor found but could not find prerequisites: A5a", ()->MethodExecStore.create(bs).type(A5b.class).build());
		assertType(A5c.class, MethodExecStore.create(bs).type(A5c.class).build());

		bs.addBean(A5a.class, new A5a());
		assertType(A5b.class, MethodExecStore.create(bs).type(A5b.class).build());
		assertType(A5c.class, MethodExecStore.create(bs).type(A5c.class).build());
	}


	public static class A6a {}

	public static class A6b extends MethodExecStats {
		public A6b(MethodExecStats.Builder b, A6a x) throws Exception {
			super(b);
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	public static class A6c extends MethodExecStats {
		public A6c(MethodExecStats.Builder b, Optional<A6a> x) throws Exception {
			super(b);
			if (x == null)
				throw new RuntimeException("Bad");
		}
	}

	@Test public void a06_builder_statsImplClass() throws Exception {  // NOSONAR - Must be public.
		var bs = BeanStore.create().build();
		var m = MethodExecStore_Test.class.getMethod("a06_builder_statsImplClass");

		assertThrowsWithMessage(Exception.class, "Public constructor found but could not find prerequisites: A6a", ()->MethodExecStore.create(bs).statsImplClass(A6b.class).build().getStats(m));
		assertType(A6c.class, MethodExecStore.create(bs).statsImplClass(A6c.class).build().getStats(m));

		bs.addBean(A6a.class, new A6a());
		assertType(A6b.class, MethodExecStore.create(bs).statsImplClass(A6b.class).build().getStats(m));
		assertType(A6c.class, MethodExecStore.create(bs).statsImplClass(A6c.class).build().getStats(m));
	}

	@Test public void a07_builder_thrownStore() throws Exception {  // NOSONAR - Must be public.
		var m = MethodExecStore_Test.class.getMethod("a07_builder_thrownStore");
		var s = ThrownStore.create().build();

		var store = MethodExecStore.create().thrownStore(s).build();
		store.getStats(m).error(new Throwable());
		assertSize(1, s.getStats());
		TestUtils.assertSameObject(s, store.getThrownStore());

		var s2 = ThrownStore.create().build();
		var bs = BeanStore.create().build().addBean(ThrownStore.class, s2);
		store = MethodExecStore.create(bs).build();
		TestUtils.assertSameObject(s2, store.getThrownStore());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Store tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test public void b01_store_getStats() throws Exception {  // NOSONAR - Must be public.
		var m = MethodExecStore_Test.class.getMethod("b01_store_getStats");
		var s = ThrownStore.create().build();

		var store = MethodExecStore.create().thrownStore(s).build();
		store.getStats(m).error(new Throwable());

		assertSize(1, store.getStats(m).getThrownStore().getStats());
		assertSize(1, store.getStats(m).getThrownStore().getStats());
		assertSize(1, store.getStats());
	}

	//------------------------------------------------------------------------------------------------------------------
	// MethodExecStats tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test public void c01_stats_basic() throws Exception {  // NOSONAR - Must be public.
		var m = MethodExecStore_Test.class.getMethod("c01_stats_basic");
		var s = ThrownStore.create().build();

		var store = MethodExecStore.create().thrownStore(s).build();
		var stats = store.getStats(m);

		assertNotEquals(0L, stats.getGuid());
		TestUtils.assertSameObject(m, stats.getMethod());

		assertEquals(0, stats.getRuns());
		assertEquals(0, stats.getRunning());
		assertEquals(0, stats.getErrors());
		assertEquals(0, stats.getMinTime());
		assertEquals(0, stats.getMaxTime());
		assertEquals(0, stats.getAvgTime());
		assertEquals(0L, stats.getTotalTime());

		stats.started().finished(100*1000000).started().finished(200*1000000).started().error(new Throwable());

		assertEquals(3, stats.getRuns());
		assertEquals(1, stats.getRunning());
		assertEquals(1, stats.getErrors());
		assertEquals(100, stats.getMinTime());
		assertEquals(200, stats.getMaxTime());
		assertEquals(150, stats.getAvgTime());
		assertEquals(300L, stats.getTotalTime());

		assertContains("300", stats);
	}
}