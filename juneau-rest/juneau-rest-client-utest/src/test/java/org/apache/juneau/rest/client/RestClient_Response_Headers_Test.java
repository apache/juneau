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
		@RestOp
		public String getEcho(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) {
			String c = req.getHeader("Check");
			String[] h = req.getHeaders().get(c);
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
		assertFalse(checkFooClient().build().get("/echo").run().getHeader("Foo").exists());
		assertTrue(checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").exists());
	}

	public static class A2 extends BasicHeader {
		private static final long serialVersionUID = 1L;

		private A2(String name, Object value) {
			super(name, value);  // Never called.
		}
	}

	@Test
	public void a02_asHeader() throws Exception {
		Header h = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asHeader(BasicStringHeader.class).assertName().is("Foo").assertValue().is("bar");
		assertTrue(h instanceof BasicStringHeader);

		h = checkFooClient().build().get("/echo").header("Foo","\"bar\"").run().getHeader("Foo").asHeader(ETag.class).assertName().is("ETag").assertValue().is("\"bar\"");
		assertTrue(h instanceof ETag);

		assertThrown(()->checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asHeader(Age.class)).contains("Value could not be parsed");
		assertThrown(()->checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asHeader(A2.class)).contains("Could not determine a method to construct type");

		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asCsvArrayHeader().assertName().is("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","*").run().getHeader("Foo").asEntityTagArrayHeader().assertName().is("Foo").assertValue().is("*");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asStringRangeArrayHeader().assertName().is("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asStringHeader().assertName().is("Foo").assertValue().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asUriHeader().assertName().is("Foo").assertValue().is("bar");
	}

	@Test
	public void a03_asString() throws Exception {
		String s = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asString();
		assertEquals("bar", s);

		Mutable<String> m = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asString(m);
		assertEquals("bar", m.get());

		Optional<String> o = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asOptionalString();
		assertEquals("bar", o.get());
		o = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").asOptionalString();
		assertFalse(o.isPresent());

		s = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asStringOrElse("baz");
		assertEquals("bar", s);
		s = checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").asStringOrElse("baz");
		assertEquals("baz", s);

		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").asStringOrElse(m,"baz");
		assertEquals("bar", m.get());
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").asStringOrElse(m,"baz");
		assertEquals("baz", m.get());
	}

	@Test
	public void a04_asType() throws Exception {
		Integer i = checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").as(Integer.class);
		assertEquals(123, i.intValue());

		Mutable<Integer> m1 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").as(m1,Integer.class);
		assertEquals(123, m1.get().intValue());

		List<Integer> l = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(LinkedList.class,Integer.class);
		assertObject(l).json().is("[1,2]");

		Mutable<Integer> m2 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(m2,LinkedList.class,Integer.class);
		assertObject(m2.get()).json().is("[1,2]");

		ClassMeta<LinkedList<Integer>> cm1 = BeanContext.DEFAULT.getClassMeta(LinkedList.class, Integer.class);
		ClassMeta<Integer> cm2 = BeanContext.DEFAULT.getClassMeta(Integer.class);

		l = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(cm1);
		assertObject(l).json().is("[1,2]");

		Mutable<LinkedList<Integer>> m3 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").as(m3,cm1);
		assertObject(m3.get()).json().is("[1,2]");

		assertThrown(()->checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").as(m2,cm1)).contains("Invalid number");

		Optional<List<Integer>> o1 = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").asOptional(LinkedList.class,Integer.class);
		assertObject(o1.get()).json().is("[1,2]");
		o1 = checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Bar").asOptional(LinkedList.class,Integer.class);
		assertFalse(o1.isPresent());

		Mutable<Optional<List<Integer>>> m4 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Foo").asOptional(m4,LinkedList.class,Integer.class);
		assertObject(m4.get().get()).json().is("[1,2]");
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Bar").asOptional(m4,LinkedList.class,Integer.class);
		assertFalse(m4.get().isPresent());

		Optional<Integer> o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Foo").asOptional(Integer.class);
		assertEquals(1, o2.get().intValue());
		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Bar").asOptional(Integer.class);
		assertFalse(o2.isPresent());

		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Foo").asOptional(cm2);
		assertEquals(1, o2.get().intValue());
		o2 = checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Bar").asOptional(cm2);
		assertFalse(o2.isPresent());

		Mutable<Optional<Integer>> m5 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Foo").asOptional(m5,Integer.class);
		assertEquals(1, m5.get().get().intValue());
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Bar").asOptional(m5,Integer.class);
		assertFalse(m5.get().isPresent());

		m5 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","1").run().getHeader("Foo").asOptional(m5,cm2);
		assertEquals(1, m5.get().get().intValue());
		checkFooClient().build().get("/echo").header("Foo","1,2").run().getHeader("Bar").asOptional(m5,cm2);
		assertFalse(m5.get().isPresent());

		assertTrue(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher("foo").matches());
		assertFalse(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher("foo").matches());
		assertTrue(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher("FOO",Pattern.CASE_INSENSITIVE).matches());
		assertFalse(checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher("FOO",Pattern.CASE_INSENSITIVE).matches());

		Mutable<Matcher> m6 = Mutable.create();
		checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher(m6,"foo");
		assertTrue(m6.get().matches());
		checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher(m6,"foo");
		assertFalse(m6.get().matches());
		checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher(m6,"FOO",Pattern.CASE_INSENSITIVE);
		assertTrue(m6.get().matches());
		checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher(m6,"FOO",Pattern.CASE_INSENSITIVE);
		assertFalse(m6.get().matches());
		checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Foo").asMatcher(m6,Pattern.compile("FOO",Pattern.CASE_INSENSITIVE));
		assertTrue(m6.get().matches());
		checkFooClient().build().get("/echo").header("Foo","foo").run().getHeader("Bar").asMatcher(m6,Pattern.compile("FOO",Pattern.CASE_INSENSITIVE));
		assertFalse(m6.get().matches());
	}

	@Test
	public void a05_toResponse() throws Exception {
		RestResponse r = checkFooClient().build().get("/echo").header("Foo","123").run();
		assertTrue(r == r.getHeader("Foo").toResponse());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_assertions() throws Exception {
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Foo").assertString().is("bar");
		checkFooClient().build().get("/echo").header("Foo","bar").run().getHeader("Bar").assertString().doesNotExist();
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").assertInteger().is(123);
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Bar").assertInteger().doesNotExist();
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Foo").assertLong().is(123l);
		checkFooClient().build().get("/echo").header("Foo","123").run().getHeader("Bar").assertLong().doesNotExist();
		checkFooClient().build().get("/echo").header(BasicDateHeader.of("Foo",CALENDAR)).run().getHeader("Foo").assertDate().exists();
		checkFooClient().build().get("/echo").header(BasicDateHeader.of("Foo",CALENDAR)).run().getHeader("Bar").assertDate().doesNotExist();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header methods.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_getElements() throws Exception {
		HeaderElement[] e = checkFooClient().build().get("/echo").header("Foo","bar=baz;qux=quux").run().getHeader("Foo").getElements();
		assertEquals("bar", e[0].getName());
		assertEquals("baz", e[0].getValue());
		assertEquals("quux", e[0].getParameterByName("qux").getValue());

		e = checkFooClient().build().get("/echo").header("Foo","bar=baz;qux=quux").run().getHeader("Bar").getElements();
		assertEquals(0, e.length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder checkFooClient() {
		return MockRestClient.create(A.class).simpleJson().header("Check","Foo");
	}
}
