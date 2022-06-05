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

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.stats.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MethodExecStore_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Builder tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_builder_default() {
		assertObject(MethodExecStore.create().build()).isType(MethodExecStore.class);
	}

	public static class A1 extends MethodExecStore{
		protected A1(Builder builder) {
			super(builder);
		}
	}

	@Test
	public void a02_builder_implClass() {
		assertObject(MethodExecStore.create().type(A1.class).build()).isType(A1.class);
	}

	public static class A4 extends MethodExecStore {
		public A4(MethodExecStore.Builder b) throws Exception {
			super(b);
			throw new RuntimeException("foobar");
		}
	}

	@Test
	public void a04_builder_implClass_bad() {
		assertThrown(()->MethodExecStore.create().type(A4.class).build()).asMessages().isContains("foobar");
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

	@Test
	public void a05_builder_beanFactory() throws Exception {
		BeanStore bs = BeanStore.create().build();

		assertThrown(()->MethodExecStore.create(bs).type(A5b.class).build()).asMessages().isAny(contains("Public constructor found but could not find prerequisites: A5a"));
		assertObject(MethodExecStore.create(bs).type(A5c.class).build()).isType(A5c.class);

		bs.addBean(A5a.class, new A5a());
		assertObject(MethodExecStore.create(bs).type(A5b.class).build()).isType(A5b.class);
		assertObject(MethodExecStore.create(bs).type(A5c.class).build()).isType(A5c.class);
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

	@Test
	public void a06_builder_statsImplClass() throws Exception {
		BeanStore bs = BeanStore.create().build();
		Method m = MethodExecStore_Test.class.getMethod("a06_builder_statsImplClass");

		assertThrown(()->MethodExecStore.create(bs).statsImplClass(A6b.class).build().getStats(m)).asMessages().isAny(contains("Public constructor found but could not find prerequisites: A6a"));
		assertObject(MethodExecStore.create(bs).statsImplClass(A6c.class).build().getStats(m)).isType(A6c.class);

		bs.addBean(A6a.class, new A6a());
		assertObject(MethodExecStore.create(bs).statsImplClass(A6b.class).build().getStats(m)).isType(A6b.class);
		assertObject(MethodExecStore.create(bs).statsImplClass(A6c.class).build().getStats(m)).isType(A6c.class);
	}

	@Test
	public void a07_builder_thrownStore() throws Exception {
		Method m = MethodExecStore_Test.class.getMethod("a07_builder_thrownStore");
		ThrownStore s = ThrownStore.create().build();

		MethodExecStore store = MethodExecStore.create().thrownStore(s).build();
		store.getStats(m).error(new Throwable());
		assertList(s.getStats()).isSize(1);
		assertObject(store.getThrownStore()).isSame(s);

		ThrownStore s2 = ThrownStore.create().build();
		BeanStore bs = BeanStore.create().build().addBean(ThrownStore.class, s2);
		store = MethodExecStore.create(bs).build();
		assertObject(store.getThrownStore()).isSame(s2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Store tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_store_getStats() throws Exception {
		Method m = MethodExecStore_Test.class.getMethod("b01_store_getStats");
		ThrownStore s = ThrownStore.create().build();

		MethodExecStore store = MethodExecStore.create().thrownStore(s).build();
		store.getStats(m).error(new Throwable());

		assertList(store.getStats(m).getThrownStore().getStats()).isSize(1);
		assertList(store.getStats(m).getThrownStore().getStats()).isSize(1);
		assertCollection(store.getStats()).isSize(1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// MethodExecStats tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_stats_basic() throws Exception {
		Method m = MethodExecStore_Test.class.getMethod("c01_stats_basic");
		ThrownStore s = ThrownStore.create().build();

		MethodExecStore store = MethodExecStore.create().thrownStore(s).build();
		MethodExecStats stats = store.getStats(m);

		assertLong(stats.getGuid()).isNot(0l);
		assertObject(stats.getMethod()).isSame(m);

		assertInteger(stats.getRuns()).is(0);
		assertInteger(stats.getRunning()).is(0);
		assertInteger(stats.getErrors()).is(0);
		assertInteger(stats.getMinTime()).is(0);
		assertInteger(stats.getMaxTime()).is(0);
		assertInteger(stats.getAvgTime()).is(0);
		assertLong(stats.getTotalTime()).is(0l);

		stats.started().finished(100*1000000).started().finished(200*1000000).started().error(new Throwable());

		assertInteger(stats.getRuns()).is(3);
		assertInteger(stats.getRunning()).is(1);
		assertInteger(stats.getErrors()).is(1);
		assertInteger(stats.getMinTime()).is(100);
		assertInteger(stats.getMaxTime()).is(200);
		assertInteger(stats.getAvgTime()).is(150);
		assertLong(stats.getTotalTime()).is(300l);

		assertObject(stats).asString().isContains("300");
	}
}
