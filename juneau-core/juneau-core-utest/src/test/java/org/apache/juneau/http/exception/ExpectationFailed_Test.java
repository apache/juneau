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
package org.apache.juneau.http.exception;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ExpectationFailed_Test {

	private static final int CODE = ExpectationFailed.CODE;
	private static final String MESSAGE = ExpectationFailed.MESSAGE;

	@Rest
	public static class A {
		@RestMethod
		public void getF1() throws ExpectationFailed {
			throw new ExpectationFailed();
		}
		@RestMethod
		public void getF2() throws ExpectationFailed {
			throw new ExpectationFailed("foo {0}", "bar");
		}
		@RestMethod
		public void getF3() throws ExpectationFailed {
			throw new ExpectationFailed(new RuntimeException("baz"));
		}
		@RestMethod
		public void getF4() throws ExpectationFailed {
			throw new ExpectationFailed(new RuntimeException("baz"), "foo {0}", "bar");
		}
		@RestMethod
		public void getF5() throws ExpectationFailed {
			throw new ExpectationFailed().header("Foo", "bar");
		}
		@RestMethod
		public void getF6() throws ExpectationFailed {
			throw new ExpectationFailed("foo");
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient c = MockRestClient.create(A.class).ignoreErrors().build();

		c.get("/f1").run()
			.assertStatus().code().is(CODE)
			.assertBody().is(MESSAGE);
		c.get("/f2").run()
			.assertStatus().code().is(CODE)
			.assertBody().is("foo bar");
		c.get("/f3").run()
			.assertStatus().code().is(CODE)
			.assertBody().is("baz");
		c.get("/f4").run()
			.assertStatus().code().is(CODE)
			.assertBody().is("foo bar");
		c.get("/f5").run()
			.assertStatus().code().is(CODE)
			.assertBody().is(MESSAGE)
			.assertStringHeader("Foo").is("bar");
		c.get("/f6").run()
			.assertStatus().code().is(CODE)
			.assertBody().is("foo");
	}
}
