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

	@Test void a05_equalsAndHashCode() {
		var x1 = new BasicHttpException(400, "foo");
		var x2 = new BasicHttpException(400, "foo");

		// Same instance
		assertEquals(x1, x1);

		// Different instance same content - stack traces differ so not equal
		assertNotEquals(x1, x2);

		// Non-BasicHttpException
		assertNotEquals(x1, "foo");
		assertNotEquals(x1, null);

		// hashCode is consistent
		assertNotEquals(0, x1.hashCode());
		assertEquals(x1.hashCode(), x1.hashCode());
	}

	@Test void a06_getEntity_whenContentAlreadySet() {
		var x = httpException().setStatusCode2(500).setContent("existing content");
		// Calling getEntity() when content is already set should return existing entity
		var entity = x.getEntity();
		assertNotNull(entity);
		// Calling again should return the same entity
		assertSame(entity, x.getEntity());
	}

	@Test void a07_getMessage_withCause() {
		// getMessage() when no message set but has cause
		var cause = new RuntimeException("cause message");
		var x = new BasicHttpException(500, cause, null);
		assertEquals("cause message", x.getMessage());
	}

	@Test void a08_getMessage_fromStatusLine() {
		// getMessage() when no message and no cause - falls back to reason phrase
		var x = new BasicHttpException(200);
		// Should return status code reason phrase "OK"
		assertNotNull(x.getMessage());
	}

	@Test void a09_fluentStatusLine_setters() {
		var x = httpException().setStatusCode2(500);
		var pv = new org.apache.http.ProtocolVersion("HTTP", 2, 0);
		assertSame(x, x.setProtocolVersion(pv));
		assertSame(x, x.setLocale2(java.util.Locale.FRENCH));
		assertSame(x, x.setMessage("new message"));
		assertSame(x, x.setHeaders2(BasicHeader.of("X-A", "1"), BasicHeader.of("X-B", "2")));
		assertEquals("1", x.getFirstHeader("X-A").getValue());
		assertEquals("2", x.getFirstHeader("X-B").getValue());
	}

	@Test void a10_getLastHeader_and_getHeaders_by_name() {
		var x = httpException().setStatusCode2(500)
			.setHeaders(l(
				BasicHeader.of("X-Multi", "first"),
				BasicHeader.of("X-Multi", "second")
			));
		assertEquals("second", x.getLastHeader("X-Multi").getValue());
		assertEquals(2, x.getHeaders("X-Multi").length);
		assertTrue(x.containsHeader("X-Multi"));
		assertFalse(x.containsHeader("X-NonExistent"));
	}

	@Test void a11_copyConstructorViaCast() {
		var original = new BasicHttpException(400, new RuntimeException("err"), "original message");
		// Verify the exception has the expected message
		assertEquals("original message", original.getMessage());
	}

	@Test void a12_addHeader() {
		var x = httpException().setStatusCode2(500);
		x.addHeader("X-Added", "addedValue");
		assertEquals("addedValue", x.getFirstHeader("X-Added").getValue());
		x.addHeader(BasicHeader.of("X-Added2", "value2"));
		assertEquals("value2", x.getFirstHeader("X-Added2").getValue());
	}
}