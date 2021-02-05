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
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RangeNotSatisfiable_Test {

	private static final int CODE = RangeNotSatisfiable.CODE;
	private static final String MESSAGE = RangeNotSatisfiable.MESSAGE;

	@Rest
	public static class A {
		@RestOp
		public void getF1() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable();
		}
		@RestOp
		public void getF2() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable("foo {0}", "bar");
		}
		@RestOp
		public void getF3() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable(new RuntimeException("baz"));
		}
		@RestOp
		public void getF4() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable(new RuntimeException("baz"), "foo {0}", "bar");
		}
		@RestOp
		public void getF5() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable().header("Foo", "bar");
		}
		@RestOp
		public void getF6() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable("foo");
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient c = MockRestClient.create(A.class).ignoreErrors().noLog().build();

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
