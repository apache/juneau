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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.testutils.StreamUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings("serial")
public class Assertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	public static class A1 extends RuntimeException {
		public A1(String msg, Throwable cause) { super(msg, cause); }
	}

	public static class A2 extends RuntimeException {
		public A2(String msg, Throwable cause) { super(msg, cause); }
	}

	public static class A3 extends RuntimeException {
		public A3(String msg) { throw new A2("fromA3", null); }
	}

	public static class A extends StringAssertion {
		public A(Object text) { super(text); }
		public A doError() { throw error(new A1("foo", null), "bar {0}", "baz"); }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basicErrorHandling() throws Exception {
		A a = new A("");
		a.setSilent();

		assertThrown(()->a.doError())
			.isExactType(BasicAssertionError.class)
			.asMessage().is("bar baz")
			.asCausedBy().isExactType(A1.class)
			.asCausedBy().asMessage().is("foo")
			.asCausedBy().asCausedBy().isNull();

		a.setThrowable(A2.class);
		assertThrown(()->a.doError())
			.isExactType(A2.class)
			.asMessage().is("bar baz")
			.asCausedBy().isExactType(A1.class)
			.asCausedBy().asMessage().is("foo")
			.asCausedBy().asCausedBy().isNull();

		a.setThrowable(A3.class);
		assertThrown(()->a.doError())
			.isExactType(BasicRuntimeException.class)
			.asMessage().is("bar baz")
			.asCausedBy().isExactType(A1.class)
			.asCausedBy().asMessage().is("foo")
			.asCausedBy().asCausedBy().isNull();
	}

	@Test
	public void a02_out() throws Exception {
		PrintStream out = printStream();

		try {
			assertString("x").setOut(out).isNull();
		} catch (BasicAssertionError e) {}

		assertString(out).isContains("Value was not null.");
	}
}
