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

import org.apache.juneau.mstat.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
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

		assertObject(mes).json().matchesSimple("{method:'A.foo',runs:3,running:0,errors:0,minTime:*,maxTime:*,avgTime:*,totalTime:*,exceptions:[]}");
	}

	@Test
	public void testException() throws Exception {
		Method m = A.class.getMethod("bar");

		MethodExecStats mes = new MethodExecStats(m);
		MethodInvoker mi = new MethodInvoker(m, mes);

		A a = new A();
		assertThrown(()->{mi.invoke(a);}).exists();
		assertThrown(()->{mi.invoke(a);}).exists();
		assertThrown(()->{mi.invoke(a);}).exists();

		assertObject(mes).json().matchesSimple("{method:'A.bar',runs:3,running:0,errors:3,minTime:*,maxTime:*,avgTime:*,totalTime:*,exceptions:[{hash:'*',count:3,exceptionClass:*,message:*,stackTrace:*}]}");
	}

	@Test
	public void testIllegalArgument() throws Exception {
		Method m = A.class.getMethod("baz", int.class);

		MethodExecStats mes = new MethodExecStats(m);
		MethodInvoker mi = new MethodInvoker(m, mes);

		A a = new A();
		assertThrown(()->{mi.invoke(a, "x");}).exists();
		assertThrown(()->{mi.invoke(a);}).exists();
		assertThrown(()->{mi.invoke(a, 1, "x");}).exists();

		assertObject(mes).json().matchesSimple("{method:'A.baz',runs:3,running:0,errors:3,minTime:*,maxTime:*,avgTime:*,totalTime:*,exceptions:[{hash:'*',count:3,exceptionClass:*,message:*,stackTrace:*}]}");
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
