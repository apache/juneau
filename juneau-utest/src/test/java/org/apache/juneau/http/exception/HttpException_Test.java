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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.lang.reflect.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HttpException_Test {

	@Rest
	public static class A {
		@RestGet
		public void f1() throws HttpException {
			throw new HttpException(new RuntimeException("foo"), 225, "bar {0}", "baz");
		}
		@RestGet
		public void f2() throws HttpException {
			throw new HttpException("foo").setStatus(225);
		}
		@RestGet
		public void f3() throws HttpException {
			throw new HttpException(new RuntimeException("baz"), 225);
		}
		@RestGet
		public void f4() throws HttpException {
			throw new HttpException(225, "bar {0}", "baz");
		}
		@RestGet
		public void f5() throws HttpException {
			throw new HttpException(null).setStatus(225).header("Foo", "bar");
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient c = MockRestClient.create(A.class).ignoreErrors().build();

		c.get("/f1").run()
			.assertStatus().code().is(225)
			.assertBody().is("bar baz");
		c.get("/f2").run()
			.assertStatus().code().is(225)
			.assertBody().is("foo");
		c.get("/f3").run()
			.assertStatus().code().is(225)
			.assertBody().is("baz");
		c.get("/f4").run()
			.assertStatus().code().is(225)
			.assertBody().is("bar baz");
		c.get("/f5").run()
			.assertStatus().code().is(225)
			.assertBody().is("")
			.assertHeader("Foo").is("bar");
	}

	@Test
	public void a02_getRootCause() throws Exception {
		HttpException x = new HttpException(null);
		assertObject(x.getRootCause()).doesNotExist();

		x = new HttpException(new HttpException(100,"foo"),100);
		assertObject(x.getRootCause()).doesNotExist();

		x = new HttpException(new RuntimeException("foo"),100);
		assertObject(x.getRootCause()).isType(RuntimeException.class);

		x = new HttpException(new HttpException(new RuntimeException("foo"),100),100);
		assertObject(x.getRootCause()).isType(RuntimeException.class);

		x = new HttpException(new InvocationTargetException(new RuntimeException("foo")),100);
		assertObject(x.getRootCause()).isType(RuntimeException.class);
	}

	@Test
	public void a03_getFullStackMessage() throws Exception {
		HttpException x = new HttpException(null);
		assertString(x.getFullStackMessage(false)).is("");
		assertString(x.getFullStackMessage(true)).is("");

		x = new HttpException("foo<bar>&baz");
		assertString(x.getFullStackMessage(false)).is("foo<bar>&baz");
		assertString(x.getFullStackMessage(true)).is("foo bar  baz");

		x = new HttpException(new RuntimeException("foo<bar>&qux"), 100, "foo{0}","<bar>&baz");
		assertString(x.getFullStackMessage(false)).is("foo<bar>&baz\nCaused by (RuntimeException): foo<bar>&qux");
		assertString(x.getFullStackMessage(true)).is("foo bar  baz\nCaused by (RuntimeException): foo bar  qux");

		x = new HttpException(new RuntimeException(), 100, "foo{0}","<bar>&baz");
		assertString(x.getFullStackMessage(false)).is("foo<bar>&baz\nCaused by (RuntimeException)");
		assertString(x.getFullStackMessage(true)).is("foo bar  baz\nCaused by (RuntimeException)");

		assertInteger(x.hashCode()).exists();
	}
}
