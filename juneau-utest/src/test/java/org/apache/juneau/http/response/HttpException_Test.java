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
package org.apache.juneau.http.response;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpResponses.*;
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
		public void f1() throws BasicHttpException {
			throw new BasicHttpException(225, new RuntimeException("foo"), "bar {0}", "baz");
		}
		@RestGet
		public void f2() throws BasicHttpException {
			throw new BasicHttpException(225, "foo");
		}
		@RestGet
		public void f3() throws BasicHttpException {
			throw new BasicHttpException(225, new RuntimeException("baz"));
		}
		@RestGet
		public void f4() throws BasicHttpException {
			throw new BasicHttpException(225, "bar {0}", "baz");
		}
		@RestGet
		public void f5() throws BasicHttpException {
			throw httpException().setStatusCode2(225).setHeader2("Foo", "bar");
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient c = MockRestClient.create(A.class).ignoreErrors().build();

		c.get("/f1").run()
			.assertStatus().asCode().is(225)
			.assertContent("bar baz");
		c.get("/f2").run()
			.assertStatus().asCode().is(225)
			.assertContent("foo");
		c.get("/f3").run()
			.assertStatus().asCode().is(225)
			.assertContent("java.lang.RuntimeException: baz");
		c.get("/f4").run()
			.assertStatus().asCode().is(225)
			.assertContent("bar baz");
		c.get("/f5").run()
			.assertStatus().asCode().is(225)
			.assertContent("")
			.assertHeader("Foo").is("bar");
	}

	@Test
	public void a02_getRootCause() throws Exception {
		BasicHttpException x = new BasicHttpException(100, null);
		assertObject(x.getRootCause()).isNull();

		x = new BasicHttpException(100, new BasicHttpException(100,"foo"));
		assertObject(x.getRootCause()).isNull();

		x = new BasicHttpException(100, new RuntimeException("foo"));
		assertObject(x.getRootCause()).isType(RuntimeException.class);

		x = new BasicHttpException(100, new BasicHttpException(100, new RuntimeException("foo")));
		assertObject(x.getRootCause()).isType(RuntimeException.class);

		x = new BasicHttpException(100, new InvocationTargetException(new RuntimeException("foo")));
		assertObject(x.getRootCause()).isType(RuntimeException.class);
	}

	@Test
	public void a03_getFullStackMessage() throws Exception {
		BasicHttpException x = new BasicHttpException(100, null);
		assertString(x.getFullStackMessage(false)).is("Continue");
		assertString(x.getFullStackMessage(true)).is("Continue");

		x = new BasicHttpException(100, "foo<bar>&baz");
		assertString(x.getFullStackMessage(false)).is("foo<bar>&baz");
		assertString(x.getFullStackMessage(true)).is("foo bar  baz");

		x = new BasicHttpException(100, new RuntimeException("foo<bar>&qux"), "foo{0}","<bar>&baz");
		assertString(x.getFullStackMessage(false)).is("foo<bar>&baz\nCaused by (RuntimeException): foo<bar>&qux");
		assertString(x.getFullStackMessage(true)).is("foo bar  baz\nCaused by (RuntimeException): foo bar  qux");

		x = new BasicHttpException(100, new RuntimeException(), "foo{0}","<bar>&baz");
		assertString(x.getFullStackMessage(false)).is("foo<bar>&baz\nCaused by (RuntimeException)");
		assertString(x.getFullStackMessage(true)).is("foo bar  baz\nCaused by (RuntimeException)");

		assertInteger(x.hashCode()).isExists();
	}
}
