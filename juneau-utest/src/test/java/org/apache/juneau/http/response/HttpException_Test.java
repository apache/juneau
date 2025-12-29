/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.response;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.http.HttpResponses.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class HttpException_Test extends TestBase {

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
		@RestGet
		public void f6() throws BasicHttpException {
			throw httpException().setStatusCode2(225).setHeaders(l(
				BasicHeader.of("X-Custom", "value1"),
				BasicHeader.of("X-Test", "value2")
			));
		}
		@RestGet
		public void f7() throws BasicHttpException {
			throw httpException().setStatusCode2(225).setContent("Custom exception content");
		}
	}

	@Test void a01_basic() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().build();

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
		c.get("/f6").run()
			.assertStatus().asCode().is(225)
			.assertHeader("X-Custom").is("value1")
			.assertHeader("X-Test").is("value2");
		c.get("/f7").run()
			.assertStatus().asCode().is(225)
			.assertContent("Custom exception content");
	}

	@Test void a02_getRootCause() {
		var x = new BasicHttpException(100, null);
		assertNull(x.getRootCause());

		x = new BasicHttpException(100, new BasicHttpException(100,"foo"));
		assertNull(x.getRootCause());

		x = new BasicHttpException(100, new RuntimeException("foo"));
		assertInstanceOf(RuntimeException.class, x.getRootCause());

		x = new BasicHttpException(100, new BasicHttpException(100, new RuntimeException("foo")));
		assertInstanceOf(RuntimeException.class, x.getRootCause());

		x = new BasicHttpException(100, new InvocationTargetException(new RuntimeException("foo")));
		assertInstanceOf(RuntimeException.class, x.getRootCause());
	}

	@Test void a03_getFullStackMessage() {
		var x = new BasicHttpException(100, null);
		assertEquals("Continue", x.getFullStackMessage(false));
		assertEquals("Continue", x.getFullStackMessage(true));

		x = new BasicHttpException(100, "foo<bar>&baz");
		assertEquals("foo<bar>&baz", x.getFullStackMessage(false));
		assertEquals("foo bar  baz", x.getFullStackMessage(true));

		x = new BasicHttpException(100, new RuntimeException("foo<bar>&qux"), "foo{0}","<bar>&baz");
		assertEquals("foo<bar>&baz\nCaused by (RuntimeException): foo<bar>&qux", x.getFullStackMessage(false));
		assertEquals("foo bar  baz\nCaused by (RuntimeException): foo bar  qux", x.getFullStackMessage(true));

		x = new BasicHttpException(100, new RuntimeException(), "foo{0}","<bar>&baz");
		assertEquals("foo<bar>&baz\nCaused by (RuntimeException)", x.getFullStackMessage(false));
		assertEquals("foo bar  baz\nCaused by (RuntimeException)", x.getFullStackMessage(true));
	}

	@Test void a04_fluentSetters() {
		var x = httpException().setStatusCode2(500);

		// Test setHeaders(List<Header>) returns same instance for fluent chaining
		assertSame(x, x.setHeaders(l(
			BasicHeader.of("X-Fluent-Test", "fluent-value")
		)));
		assertEquals("fluent-value", x.getFirstHeader("X-Fluent-Test").getValue());

		// Test setContent(String) returns same instance for fluent chaining
		assertSame(x, x.setContent("test error content"));

		// Test setContent(HttpEntity) returns same instance for fluent chaining
		var x2 = httpException().setStatusCode2(500);
		HttpEntity entity = x2.getEntity();
		assertSame(x2, x2.setContent(entity));
	}
}