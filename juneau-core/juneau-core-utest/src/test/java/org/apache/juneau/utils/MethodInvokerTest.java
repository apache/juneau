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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.*;

import org.junit.*;

public class MethodInvokerTest {

	public static class A {
		public int foo() { return 0; }
		public int bar() { throw new RuntimeException("bar"); }
		public void baz(int x) { }
	}

	@Test
	public void testBasic() throws Exception {
		Method m = A.class.getMethod("foo");

		MethodExecStats mes = new MethodExecStats(m);
		MethodInvoker mi = new MethodInvoker(m, mes);

		A a = new A();
		mi.invoke(a);
		mi.invoke(a);
		mi.invoke(a);

		assertObjectMatches("{method:'A.foo',runs:3,running:0,errors:0,avgTime:*,totalTime:*,exceptions:[]}", mes);
	}

	@Test
	public void testException() throws Exception {
		Method m = A.class.getMethod("bar");

		MethodExecStats mes = new MethodExecStats(m);
		MethodInvoker mi = new MethodInvoker(m, mes);

		A a = new A();
		try {
			mi.invoke(a);
		} catch (Exception e) {}
		try {
			mi.invoke(a);
		} catch (Exception e) {}
		try {
			mi.invoke(a);
		} catch (Exception e) {}

		assertObjectMatches("{method:'A.bar',runs:3,running:0,errors:3,avgTime:0,totalTime:*,exceptions:[{exception:'RuntimeException',hash:'*',count:3}]}", mes);
	}

	@Test
	public void testIllegalArgument() throws Exception {
		Method m = A.class.getMethod("baz", int.class);

		MethodExecStats mes = new MethodExecStats(m);
		MethodInvoker mi = new MethodInvoker(m, mes);

		A a = new A();
		try {
			mi.invoke(a, "x");
		} catch (Exception e) {}
		try {
			mi.invoke(a);
		} catch (Exception e) {}
		try {
			mi.invoke(a, 1, "x");
		} catch (Exception e) {}

		assertObjectMatches("{method:'A.baz',runs:3,running:0,errors:3,avgTime:0,totalTime:*,exceptions:[{exception:'IllegalArgumentException',hash:'*',count:3}]}", mes);
	}

	@Test
	public void testOtherMethods() throws Exception {
		Method m = A.class.getMethod("foo");
		MethodExecStats mes = new MethodExecStats(m);
		MethodInvoker mi = new MethodInvoker(m, mes);

		assertEquals(m, mi.inner());
		assertEquals("A", mi.getDeclaringClass().getSimpleName());
		assertEquals("foo", mi.getName());
	}

}
