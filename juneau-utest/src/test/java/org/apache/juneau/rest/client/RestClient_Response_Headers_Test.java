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
package org.apache.juneau.rest.client;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Response_Headers_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestGet
		public String echo(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) {
			String c = req.getHeader("Check");
			String[] h = req.getRequestHeaders().getAll(req.getHeader("Check")).stream().map(x -> x.getValue()).toArray(String[]::new);
			if (h != null)
				for (String hh : h)
					res.addHeader(c, hh);
			return "ok";
		}
	}

	private static final Calendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR.set(2000,11,31,12,34,56);
	}

	@Test
	public void a01_exists() throws Exception {
		assertFalse(checkFooClient().build().get("/echo").run().getResponseHeader("Foo").exists());
		assertTrue(checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").exists());
	}

	public static class A2 extends BasicHeader {
		private static final long serialVersionUID = 1L;

		private A2(String name, Object value) {
			super(name, value);  // Never called.
		}
	}

	@Test
	public void a02_asHeader() throws Exception {
		Header h = checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asHeader(BasicStringHeader.class).assertName().is("Foo").assertValue().is("bar");
		assertTrue(h instanceof BasicStringHeader);

		h = checkFooClient().build().get("/echo").header("Foo","\"bar\"").run().getResponseHeader("Foo").asHeader(ETag.class).assertName().is("ETag").assertValue().is("\"bar\"");
		assertTrue(h instanceof ETag);

		assertThrown(()->checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asHeader(Age.class)).contains("Value could not be parsed");
		assertThrown(()->checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asHeader(A2.class)).contains("Could not determine a method to construct type");

		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asCsvArrayHeader().assertName().is("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","*").run().getResponseHeader("Foo").asEntityTagArrayHeader().assertName().is("Foo").assertValue().is("*");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asStringRangeArrayHeader().assertName().is("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asStringHeader().assertName().is("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asUriHeader().assertName().is("Foo").assertValue().is("bar");
	}

	@Test
	public void a03_asString() throws Exception {
		String s = checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asString().orElse(null);
		assertEquals("bar", s);

		Mutable<String> m = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asString(m);
		assertEquals("bar", m.get());

		Optional<String> o = checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asString();
		assertEquals("bar", o.get());
		o = checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Bar").asString();
		assertFalse(o.isPresent());

		s = checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").asString().orElse("baz");
		assertEquals("bar", s);
		s = checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Bar").asString().orElse("baz");
		assertEquals("baz", s);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void a04_asType() throws Exception {
		Integer i = checkFooClient().build().get("/echo").header("Foo","123").run().getResponseHeader("Foo").as(Integer.class).orElse(null);
		assertEquals(123, i.intValue());

		Mutable<Integer> m1 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","123").run().getResponseHeader("Foo").as(m1,Integer.class);
		assertEquals(123, m1.get().intValue());

		List<Integer> l = (List<Integer>) checkFooClient().build().get("/echo").header("Foo","1,2").run().getResponseHeader("Foo").as(LinkedList.class,Integer.class).get();
		assertObject(l).asJson().is("[1,2]");

		Mutable<Integer> m2 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getResponseHeader("Foo").as(m2,LinkedList.class,Integer.class);

		ClassMeta<LinkedList<Integer>> cm1 = BeanContext.DEFAULT.getClassMeta(LinkedList.class, Integer.class);
		ClassMeta<Integer> cm2 = BeanContext.DEFAULT.getClassMeta(Integer.class);

		l = checkFooClient().build().get("/echo").header("Foo","1,2").run().getResponseHeader("Foo").as(cm1).get();
		assertObject(l).asJson().is("[1,2]");

		Mutable<LinkedList<Integer>> m3 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getResponseHeader("Foo").as(m3,cm1);
		assertObject(m3.get()).asJson().is("[1,2]");

		assertThrown(()->checkFooClient().build().get("/echo").header("Foo","foo").run().getResponseHeader("Foo").as(m2,cm1)).contains("Invalid number");

		Optional<List<Integer>> o1 = checkFooClient().build().get("/echo").header("Foo","1,2").run().getResponseHeader("Foo").as(LinkedList.class,Integer.class);
		assertObject(o1.get()).asJson().is("[1,2]");
		o1 = checkFooClient().build().get("/echo").header("Foo","1,2").run().getResponseHeader("Bar").as(LinkedList.class,Integer.class);
		assertFalse(o1.isPresent());

		Optional<Integer> o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getResponseHeader("Foo").as(Integer.class);
		assertEquals(1, o2.get().intValue());
		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getResponseHeader("Bar").as(Integer.class);
		assertFalse(o2.isPresent());

		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getResponseHeader("Foo").as(cm2);
		assertEquals(1, o2.get().intValue());
		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getResponseHeader("Bar").as(cm2);
		assertFalse(o2.isPresent());

		assertTrue(checkFooClient().build().get("/echo").header("Foo","foo").run().getResponseHeader("Foo").asMatcher("foo").matches());
		assertFalse(checkFooClient().build().get("/echo").header("Foo","foo").run().getResponseHeader("Bar").asMatcher("foo").matches());
		assertTrue(checkFooClient().build().get("/echo").header("Foo","foo").run().getResponseHeader("Foo").asMatcher("FOO",Pattern.CASE_INSENSITIVE).matches());
		assertFalse(checkFooClient().build().get("/echo").header("Foo","foo").run().getResponseHeader("Bar").asMatcher("FOO",Pattern.CASE_INSENSITIVE).matches());
	}

	@Test
	public void a05_toResponse() throws Exception {
		RestResponse r = checkFooClient().build().get("/echo").header("Foo","123").run();
		assertTrue(r == r.getResponseHeader("Foo").toResponse());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_assertions() throws Exception {
		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getResponseHeader("Bar").assertValue().doesNotExist();
		checkFooClient().build().get("/echo").header("Foo","123").run().getResponseHeader("Foo").assertValue().asInteger().is(123);
		checkFooClient().build().get("/echo").header("Foo","123").run().getResponseHeader("Bar").assertValue().doesNotExist();
		checkFooClient().build().get("/echo").header("Foo","123").run().getResponseHeader("Foo").assertValue().asLong().is(123l);
		checkFooClient().build().get("/echo").header("Foo","123").run().getResponseHeader("Bar").assertValue().asLong().doesNotExist();
		checkFooClient().build().get("/echo").header(BasicDateHeader.of("Foo",CALENDAR)).run().getResponseHeader("Foo").assertValue().asDate().exists();
		checkFooClient().build().get("/echo").header(BasicDateHeader.of("Foo",CALENDAR)).run().getResponseHeader("Bar").assertValue().asDate().doesNotExist();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header methods.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_getElements() throws Exception {
		HeaderElement[] e = checkFooClient().build().get("/echo").header("Foo","bar=baz;qux=quux").run().getResponseHeader("Foo").getElements();
		assertEquals("bar", e[0].getName());
		assertEquals("baz", e[0].getValue());
		assertEquals("quux", e[0].getParameterByName("qux").getValue());

		e = checkFooClient().build().get("/echo").header("Foo","bar=baz;qux=quux").run().getResponseHeader("Bar").getElements();
		assertEquals(0, e.length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder checkFooClient() {
		return MockRestClient.create(A.class).simpleJson().header("Check","Foo");
	}
}
