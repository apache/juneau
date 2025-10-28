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
package org.apache.juneau.rest.client;

import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

class RestClient_Response_Headers_Test extends TestBase {

	@Rest
	public static class A extends BasicRestObject {
		@RestGet
		public String echo(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) {
			var c = req.getHeaderParam("Check").orElse(null);
			var h = req.getHeaders().getAll(c).stream().map(RequestHeader::getValue).toArray(String[]::new);
			if (h != null)
				for (var hh : h)
					res.addHeader(c, hh);
			return "ok";
		}
	}

	private static final ZonedDateTime ZONEDDATETIME = ZonedDateTime.from(RFC_1123_DATE_TIME.parse("Sat, 29 Oct 1994 19:43:31 GMT")).truncatedTo(SECONDS);

	@Test void a01_exists() throws Exception {
		assertFalse(checkFooClient().build().get("/echo").run().getHeader("Foo").isPresent());
		assertTrue(checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").isPresent());
	}

	public static class A2 extends BasicHeader {
		private static final long serialVersionUID = 1L;

		private A2(String name, Object value) {  // NOSONAR
			super(name, value);  // Never called.
		}
	}

	@Test void a02_asHeader() throws Exception {
		var h = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asHeader(BasicStringHeader.class).assertName().is("Foo").assertStringValue().is("bar");
		assertTrue(h instanceof BasicStringHeader);

		h = checkFooClient().build().get("/echo").header("Foo","\"bar\"").run().getHeader("Foo").asHeader(ETag.class).assertName().is("ETag").assertStringValue().is("\"bar\"");
		assertTrue(h instanceof ETag);

		assertThrowsWithMessage(Exception.class, "Value 'bar' could not be parsed as an integer.", ()->checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asHeader(Age.class));
		assertThrowsWithMessage(Exception.class, "Could not determine a method to construct type", ()->checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asHeader(A2.class));

		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asCsvHeader().assertName().is("Foo").assertStringValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","*").run().getHeader("Foo").asEntityTagsHeader().assertName().is("Foo").assertStringValue().is("*");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asStringRangesHeader().assertName().is("Foo").assertStringValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asStringHeader().assertName().is("Foo").assertStringValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asUriHeader().assertName().is("Foo").assertStringValue().is("bar");
	}

	@Test void a03_asString() throws Exception {
		var s = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asString().orElse(null);
		assertEquals("bar", s);

		var m = Value.<String>empty();
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asString(m);
		assertEquals("bar", m.get());

		var o = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asString();
		assertEquals("bar", o.get());
		o = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").asString();
		assertFalse(o.isPresent());

		s = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asString().orElse("baz");
		assertEquals("bar", s);
		s = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").asString().orElse("baz");
		assertEquals("baz", s);
	}

	@Test void a04_asType() throws Exception {
		var i = checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").as(Integer.class).orElse(null);
		assertEquals(123, i.intValue());

		var m1 = Value.<Integer>empty();
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").as(m1,Integer.class);
		assertEquals(123, m1.get().intValue());

		var l = (List<Integer>) checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(LinkedList.class,Integer.class).get();
		assertList(l, "1", "2");

		var m2 = Value.empty();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(m2,LinkedList.class,Integer.class);

		ClassMeta<LinkedList<Integer>> cm1 = BeanContext.DEFAULT.getClassMeta(LinkedList.class, Integer.class);
		var cm2 = BeanContext.DEFAULT.getClassMeta(Integer.class);

		l = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(cm1).get();
		assertList(l, "1", "2");

		Value<LinkedList<Integer>> m3 = Value.empty();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(m3,cm1);
		assertList(m3.get(), "1", "2");

		assertThrowsWithMessage(Exception.class, "For input string:", ()->checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").as(m2,cm1));

		Optional<List<Integer>> o1 = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(LinkedList.class,Integer.class);
		assertList(o1.get(), "1", "2");
		o1 = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Bar").as(LinkedList.class,Integer.class);
		assertFalse(o1.isPresent());

		var o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Foo").as(Integer.class);
		assertEquals(1, o2.get().intValue());
		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Bar").as(Integer.class);
		assertFalse(o2.isPresent());

		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Foo").as(cm2);
		assertEquals(1, o2.get().intValue());
		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Bar").as(cm2);
		assertFalse(o2.isPresent());

		assertTrue(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher("foo").matches());
		assertFalse(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher("foo").matches());
		assertTrue(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher("FOO",Pattern.CASE_INSENSITIVE).matches());
		assertFalse(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher("FOO",Pattern.CASE_INSENSITIVE).matches());
	}

	@Test void a05_toResponse() throws Exception {
		var r = checkFooClient().build().get("/echo").header("Foo","123").run();
		assertSame(r, r.getHeader("Foo").response());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_assertions() throws Exception {
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").assertValue().isNull();
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").assertValue().asInteger().is(123);
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Bar").assertValue().isNull();
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").assertValue().asLong().is(123L);
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Bar").assertValue().asLong().isNull();
		checkFooClient().build().get("/echo").header(dateHeader("Foo",ZONEDDATETIME)).run().getHeader("Foo").assertValue().asZonedDateTime().isExists();
		checkFooClient().build().get("/echo").header(dateHeader("Foo",ZONEDDATETIME)).run().getHeader("Bar").assertValue().asZonedDateTime().isNull();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header methods.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getElements() throws Exception {
		var e = checkFooClient().build().get("/echo").header("Foo","bar=baz;qux=quux").run().getHeader("Foo").getElements();
		assertEquals("bar", e[0].getName());
		assertEquals("baz", e[0].getValue());
		assertEquals("quux", e[0].getParameterByName("qux").getValue());

		e = checkFooClient().build().get("/echo").header("Foo","bar=baz;qux=quux").run().getHeader("Bar").getElements();
		assertEquals(0, e.length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder checkFooClient() {
		return MockRestClient.create(A.class).json5().header("Check","Foo");
	}
}