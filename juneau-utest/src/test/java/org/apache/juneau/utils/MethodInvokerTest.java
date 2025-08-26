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
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.stats.*;
import org.junit.jupiter.api.*;

class MethodInvokerTest extends SimpleTestBase {

	private MethodExecStore store = MethodExecStore
		.create()
		.thrownStore(
			ThrownStore.create().ignoreClasses(MethodInvokerTest.class).build()
		)
		.build();

	public static class A {
		public int foo() { return 0; }
		public int bar() { throw new RuntimeException("bar"); }
		public void baz(int x) { /* no-op */ }
	}

	private MethodInvoker create(Method m) {
		return new MethodInvoker(m, store.getStats(m));
	}

	@Test void a01_basic() throws Exception {
		var m = A.class.getMethod("foo");

		var mi = create(m);

		var a = new A();
		mi.invoke(a);
		mi.invoke(a);
		mi.invoke(a);

		assertBean(mi.getStats(), "runs,errors", "3,0");
	}

	@Test void a02_exception() throws Exception {
		var m = A.class.getMethod("bar");

		var mi = create(m);

		var a = new A();
		assertThrows(Exception.class, ()->mi.invoke(a));
		assertThrows(Exception.class, ()->mi.invoke(a));
		assertThrows(Exception.class, ()->mi.invoke(a));

		assertBean(mi.getStats(), "runs,errors", "3,3");
	}

	@Test void a03_illegalArgument() throws Exception {
		var m = A.class.getMethod("baz", int.class);

		var mi = create(m);

		var a = new A();
		assertThrows(Exception.class, ()->mi.invoke(a, "x"));
		assertThrows(Exception.class, ()->mi.invoke(a));
		assertThrows(Exception.class, ()->mi.invoke(a, 1, "x"));

		assertBean(mi.getStats(), "runs,errors", "3,3");
	}

	@Test void a04_otherMethods() throws Exception {
		var m = A.class.getMethod("foo");
		var mi = create(m);

		assertEquals(m, mi.inner().inner());
		assertEquals("A", mi.getDeclaringClass().getSimpleName());
		assertEquals(A.class.getName() + ".foo()", mi.getFullName());
	}
}