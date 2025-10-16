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

import static org.apache.juneau.http.HttpResponses.*;
import static org.apache.juneau.http.response.BadRequest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class BadRequest_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet
		public void f1() throws BadRequest {
			throw new BadRequest();
		}
		@RestGet
		public void f2() throws BadRequest {
			throw new BadRequest("foo {0}", "bar");
		}
		@RestGet
		public void f3() throws BadRequest {
			throw new BadRequest(new RuntimeException("baz"));
		}
		@RestGet
		public void f4() throws BadRequest {
			throw new BadRequest(new RuntimeException("baz"), "foo {0}", "bar");
		}
		@RestGet
		public void f5() throws BadRequest {
			throw badRequest().setHeader2("Foo", "bar");
		}
		@RestGet
		public void f6() throws BadRequest {
			throw new BadRequest("foo");
		}
		@RestGet
		public void f7() throws BadRequest {
			throw badRequest().setHeaders(Arrays.asList(
				BasicHeader.of("Foo", "bar"),
				BasicHeader.of("Baz", "qux")
			));
		}
		@RestGet
		public void f8() throws BadRequest {
			throw badRequest().setContent("Custom content");
		}
		@RestGet
		public void f9() throws BadRequest {
			throw badRequest().setContent("Another custom message");
		}
	}

	@Test void a01_basic() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().noTrace().build();

		c.get("/f1").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent(REASON_PHRASE);
		c.get("/f2").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo bar");
		c.get("/f3").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("baz");
		c.get("/f4").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo bar");
		c.get("/f5").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent(REASON_PHRASE)
			.assertHeader("Foo").is("bar");
		c.get("/f6").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo");
		c.get("/f7").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertHeader("Foo").is("bar")
			.assertHeader("Baz").is("qux");
		c.get("/f8").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("Custom content");
		c.get("/f9").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("Another custom message");
	}

	@Test void a02_fluentSetters() {
		var x = badRequest();

		// Test setHeaders(List<Header>)
		assertSame(x, x.setHeaders(Arrays.asList(
			BasicHeader.of("X-Test", "test-value")
		)));
		assertEquals("test-value", x.getFirstHeader("X-Test").getValue());

		// Test setContent(String)
		assertSame(x, x.setContent("test content"));

		// Test setContent(HttpEntity) returns same instance for fluent chaining
		var x2 = badRequest();
		HttpEntity entity = x2.getEntity();
		assertSame(x2, x2.setContent(entity));
	}
}