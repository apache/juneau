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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import java.lang.reflect.*;

import org.apache.juneau.rest.stats.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MethodInvokerTest {

	private MethodExecStore store = MethodExecStore
		.create()
		.thrownStore(
			ThrownStore.create().ignoreClasses(MethodInvokerTest.class).build()
		)
		.build();

	public static class A {
		public int foo() { return 0; }
		public int bar() { throw new RuntimeException("bar"); }
		public void baz(int x) { }
	}

	private MethodInvoker create(Method m) {
		return new MethodInvoker(m, store.getStats(m));
	}

	@Test
	public void testBasic() throws Exception {
		Method m = A.class.getMethod("foo");

		MethodInvoker mi = create(m);

		A a = new A();
		mi.invoke(a);
		mi.invoke(a);
		mi.invoke(a);

		assertBean(mi.getStats()).asPropertyMap("runs","errors").asJson().is("{runs:3,errors:0}");
	}

	@Test
	public void testException() throws Exception {
		Method m = A.class.getMethod("bar");

		MethodInvoker mi = create(m);

		A a = new A();
		assertThrown(()->mi.invoke(a)).isExists();
		assertThrown(()->mi.invoke(a)).isExists();
		assertThrown(()->mi.invoke(a)).isExists();

		assertBean(mi.getStats()).asPropertyMap("runs","errors").asJson().is("{runs:3,errors:3}");
	}

	@Test
	public void testIllegalArgument() throws Exception {
		Method m = A.class.getMethod("baz", int.class);

		MethodInvoker mi = create(m);

		A a = new A();
		assertThrown(()->mi.invoke(a, "x")).isExists();
		assertThrown(()->mi.invoke(a)).isExists();
		assertThrown(()->mi.invoke(a, 1, "x")).isExists();

		assertBean(mi.getStats()).asPropertyMap("runs","errors").asJson().is("{runs:3,errors:3}");
	}

	@Test
	public void testOtherMethods() throws Exception {
		Method m = A.class.getMethod("foo");
		MethodInvoker mi = create(m);

		assertEquals(m, mi.inner().inner());
		assertEquals("A", mi.getDeclaringClass().getSimpleName());
		assertEquals(A.class.getName() + ".foo()", mi.getFullName());
	}

}
