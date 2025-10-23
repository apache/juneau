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
package org.apache.juneau.http.header;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link HeaderList}, {@link HeaderListBuilder}, {@link BasicHeaderIterator}
 */
class HeaderList_Test extends TestBase {

	private static final Header
		FOO_1 = header("Foo","1"),
		FOO_2 = header("Foo","2"),
		FOO_3 = header("Foo","3"),
		FOO_4 = header("Foo","4"),
		FOO_5 = header("Foo","5"),
		FOO_6 = header("Foo","6"),
		FOO_7 = header("Foo","7"),
		BAR_1 = header("Bar","1"),
		BAR_2 = header("Bar","2"),

		X_x = header("X", "x");

	@Test void a01_basic() {
		var x = HeaderList.create();
		assertEmpty(x);
		assertList(x.append(FOO_1), "Foo: 1");
		assertList(x.append(FOO_2), "Foo: 1", "Foo: 2");
		assertList(x.append(HeaderList.of().getAll()), "Foo: 1", "Foo: 2");
		assertList(x.append(HeaderList.of(FOO_3).getAll()), "Foo: 1", "Foo: 2", "Foo: 3");
		assertList(x.append(HeaderList.of(FOO_4, FOO_5).getAll()), "Foo: 1", "Foo: 2", "Foo: 3", "Foo: 4", "Foo: 5");
		assertList(x.append(HeaderList.of(FOO_6, FOO_7).getAll()), "Foo: 1", "Foo: 2", "Foo: 3", "Foo: 4", "Foo: 5", "Foo: 6", "Foo: 7");
		assertList(x.append((Header)null), "Foo: 1", "Foo: 2", "Foo: 3", "Foo: 4", "Foo: 5", "Foo: 6", "Foo: 7");
		assertList(x.append((List<Header>)null), "Foo: 1", "Foo: 2", "Foo: 3", "Foo: 4", "Foo: 5", "Foo: 6", "Foo: 7");
		assertEmpty(new HeaderList.Void());
	}

	@Test void a02_creators() {
		assertList(headerList(FOO_1, FOO_2, null), "Foo: 1", "Foo: 2");
		assertList(headerList(alist(FOO_1, FOO_2, null)), "Foo: 1", "Foo: 2");
		assertList(headerList("Foo","1","Foo","2"), "Foo: 1", "Foo: 2");
		assertThrowsWithMessage(IllegalArgumentException.class, "Odd number of parameters passed into HeaderList.ofPairs()", ()->headerList("Foo"));
		assertEmpty(HeaderList.of((List<Header>)null));
		assertEmpty(HeaderList.of(Collections.emptyList()));
		assertList(HeaderList.of(alist(FOO_1)), "Foo: 1");
		assertEmpty(HeaderList.of((Header[])null));
		assertEmpty(HeaderList.of());
		assertList(HeaderList.of(FOO_1), "Foo: 1");
		assertEmpty(HeaderList.ofPairs((String[])null));
		assertEmpty(HeaderList.ofPairs());
	}

	@Test void a03_addMethods() {
		var pname = "HeaderSupplierTest.x";

		var x = HeaderList.create().resolving();
		System.setProperty(pname, "y");

		x.append("X1","bar");
		x.append("X2","$S{"+pname+"}");
		x.append("X3","bar");
		x.append("X4",()->"$S{"+pname+"}");
		x.append(SerializedHeader.of("X5","bar",openApiSession(),null,false));

		assertList(x, "X1: bar", "X2: y", "X3: bar", "X4: y", "X5: bar");

		System.setProperty(pname, "z");

		assertList(x, "X1: bar", "X2: z", "X3: bar", "X4: z", "X5: bar");

		System.clearProperty(pname);
	}

	@Test void a04_toArrayMethods() {
		var x = HeaderList
			.create()
			.append("X1","1")
			.append(headerList("X2","2").getAll());
		assertList(x, "X1: 1", "X2: 2");
	}

	@Test void a05_copy() {
		var x = HeaderList.of(FOO_1).copy();
		assertList(x, "Foo: 1");
	}

	@Test void a06_getCondensed() {
		var x = HeaderList.of(FOO_1);
		assertEmpty(x.get((String)null));
		assertString("Foo: 1", x.get("Foo"));
		assertEmpty(x.get("Bar"));
		x = HeaderList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertString("Foo: 1, 2, 3", x.get("Foo"));
		assertEmpty(x.get("Bar"));
	}

	@org.apache.juneau.http.annotation.Header("Foo")
	static class Foo extends BasicStringHeader {
		private static final long serialVersionUID = 1L;

		public Foo(String value) {
			super("Foo", value);
		}
	}

	@Test void a07_getCondensed_asType() {
		var x = HeaderList.of(FOO_1);
		assertEmpty(x.get(null, Allow.class));
		assertString("Allow: 1", x.get("Foo", Allow.class));
		assertEmpty(x.get("Bar", Allow.class));
		x = HeaderList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertString("Allow: 1, 2, 3", x.get("Foo", Allow.class));
		assertEmpty(x.get("Bar", Allow.class));
		assertString("Foo: 1, 2, 3", x.get(Foo.class));
		final var x2 = x;
		assertThrowsWithMessage(IllegalArgumentException.class, "Header name could not be found on bean type 'java.lang.String'", ()->x2.get(String.class));
	}

	@Test void a08_get() {
		var x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertEmpty(x.getAll(null));
		assertList(x.getAll("Foo"), "Foo: 1", "Foo: 2");
		assertList(x.getAll("FOO"), "Foo: 1", "Foo: 2");
		assertEmpty(x.getAll("Bar"));
	}

	@Test void a09_getFirst() {
		var x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertEmpty(x.getFirst(null));
		assertString("Foo: 1", x.getFirst("Foo"));
		assertString("Foo: 1", x.getFirst("FOO"));
		assertEmpty(x.getFirst("Bar"));
	}

	@Test void a10_getLast() {
		var x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertEmpty(x.getLast(null));
		assertString("Foo: 2", x.getLast("Foo"));
		assertString("Foo: 2", x.getLast("FOO"));
		assertEmpty(x.getLast("Bar"));
	}

	@Test void a11_contains() {
		var x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertFalse(x.contains(null));
		assertTrue(x.contains("Foo"));
		assertTrue(x.contains("FOO"));
		assertFalse(x.contains("Bar"));
	}

	@Test void a12_headerIterator_all() {
		assertFalse(HeaderList.of().headerIterator().hasNext());
		assertTrue(HeaderList.of(FOO_1).headerIterator().hasNext());
	}

	@Test void a13_headerIterator_single() {
		var x = HeaderList.of();
		assertFalse(x.headerIterator("Foo").hasNext());
		x = HeaderList.of(FOO_1);
		assertTrue(x.headerIterator("Foo").hasNext());
		assertTrue(x.headerIterator("FOO").hasNext());
	}

	@Test void a14_forEach_all() {
		var x = HeaderList.of();

		var i1 = new AtomicInteger();
		x.forEach(h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = HeaderList.of(FOO_1, FOO_2);
		var i2 = new AtomicInteger();
		x.forEach(h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a15_forEach_single() {
		var x = HeaderList.of();

		var i1 = new AtomicInteger();
		x.forEach("FOO", h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = HeaderList.of(FOO_1, FOO_2, X_x);
		var i2 = new AtomicInteger();
		x.forEach("FOO", h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a16_stream_all() {
		var x = HeaderList.of();

		var i1 = new AtomicInteger();
		x.stream().forEach(h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = HeaderList.of(FOO_1, FOO_2);
		var i2 = new AtomicInteger();
		x.stream().forEach(h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a17_stream_single() {
		var x = HeaderList.of();

		var i1 = new AtomicInteger();
		x.stream("FOO").forEach(h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = HeaderList.of(FOO_1, FOO_2, X_x);
		var i2 = new AtomicInteger();
		x.stream("FOO").forEach(h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a18_caseSensitive() {
		var x = HeaderList.create().caseSensitive(true).append(FOO_1, FOO_2, X_x);
		assertList(x.getAll("Foo"), "Foo: 1", "Foo: 2");
		assertEmpty(x.getAll("FOO"));
	}

	@Test void a19_size() {
		assertSize(1, HeaderList.of(FOO_1));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_builder_clear() {
		var x = HeaderList.create().append(FOO_1);
		x.clear();
		assertEmpty(x);
	}

	@Test void b02_builder_append() {
		var x1 = HeaderList.create().append(FOO_1);
		var x2 = HeaderList
			.create()
			.append()
			.append((HeaderList)null)
			.append((Header)null)
			.append((Header[])null)
			.append(x1)
			.append(FOO_2, FOO_3)
			.append("Bar", "b1")
			.append("Bar", ()->"b2")
			.append((List<Header>)null)
			.append(alist(FOO_4));
		assertList(x2, "Foo: 1", "Foo: 2", "Foo: 3", "Bar: b1", "Bar: b2", "Foo: 4");
	}

	@Test void b03_builder_prepend() {
		var x1 = HeaderList.create().append(FOO_1);
		var x2 = HeaderList
			.create()
			.prepend()
			.prepend((HeaderList)null)
			.prepend((Header)null)
			.prepend((Header[])null)
			.prepend(x1)
			.prepend(FOO_2, FOO_3)
			.prepend("Bar", "b1")
			.prepend("Bar", ()->"b2")
			.prepend((List<Header>)null)
			.prepend(alist(FOO_4));
		assertList(x2, "Foo: 4", "Bar: b2", "Bar: b1", "Foo: 2", "Foo: 3", "Foo: 1");
	}

	@Test void b04_builder_remove() {
		var x = HeaderList
			.create()
			.append(FOO_1,FOO_2,FOO_3,FOO_4,FOO_5,FOO_6,FOO_7)
			.remove((HeaderList)null)
			.remove((Header)null)
			.remove(HeaderList.of(FOO_1))
			.remove(FOO_2)
			.remove(FOO_3, FOO_4)
			.remove(alist(FOO_5));
		assertList(x, "Foo: 6", "Foo: 7");

		x = HeaderList.create().append(FOO_1,FOO_2).remove((String[])null).remove("Bar","Foo");
		assertEmpty(x);
	}

	@Test void b05_builder_set() {
		var x = HeaderList
			.create()
			.append(FOO_1,FOO_2)
			.set(FOO_3)
			.set(BAR_1)
			.set((Header)null)
			.set((HeaderList)null);
		assertList(x, "Foo: 3", "Bar: 1");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(FOO_3);
		assertList(x, "Bar: 1", "Foo: 3", "Bar: 2");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((Header[])null)
			.set(null,FOO_3,FOO_4,FOO_5);
		assertList(x, "Bar: 1", "Bar: 2", "Foo: 3", "Foo: 4", "Foo: 5");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((List<Header>)null)
			.set(alist(null,FOO_3,FOO_4,FOO_5));
		assertList(x, "Bar: 1", "Bar: 2", "Foo: 3", "Foo: 4", "Foo: 5");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", "x");
		assertList(x, "Bar: 1", "FOO: x", "Bar: 2");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", ()->"x");
		assertList(x, "Bar: 1", "FOO: x", "Bar: 2");

		x = HeaderList
			.create()
			.caseSensitive(true)
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", ()->"x");
		assertList(x, "Bar: 1", "Foo: 1", "Foo: 2", "Bar: 2", "FOO: x");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(HeaderList.of(FOO_3,FOO_4));
		assertList(x, "Bar: 1", "Bar: 2", "Foo: 3", "Foo: 4");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHeaderIterator
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_iterators() {
		var x = HeaderList.of(Accept.TEXT_XML,ContentType.TEXT_XML);

		var i1 = x.headerIterator();
		assertString("Accept: text/xml", i1.nextHeader());
		assertString("Content-Type: text/xml", i1.nextHeader());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i1::nextHeader);

		var i2 = x.headerIterator();
		assertString("Accept: text/xml", i2.next());
		assertString("Content-Type: text/xml", i2.nextHeader());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i2::next);

		var i3 = x.headerIterator("accept");
		assertString("Accept: text/xml", i3.nextHeader());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i3::nextHeader);

		var x2 = HeaderList.create().append(Accept.TEXT_XML,ContentType.TEXT_XML).caseSensitive(true);

		var i4 = x2.headerIterator("Accept");
		assertString("Accept: text/xml", i4.nextHeader());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i4::nextHeader);

		var i5 = x2.headerIterator("accept");
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i5::nextHeader);

		assertThrowsWithMessage(UnsupportedOperationException.class, "Not supported.", i5::remove);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Default headers
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_defaultHeaders() {
		var x1 = HeaderList.create().setDefault(Accept.TEXT_XML);
		assertList(x1, "Accept: text/xml");

		var x2 = HeaderList.create().set(Accept.TEXT_PLAIN).setDefault(Accept.TEXT_XML);
		assertList(x2, "Accept: text/plain");

		var x3 = HeaderList.create().set(ContentType.TEXT_XML,Accept.TEXT_PLAIN,ContentType.TEXT_XML).setDefault(Accept.TEXT_XML);
		assertList(x3, "Content-Type: text/xml", "Accept: text/plain", "Content-Type: text/xml");

		var x4 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_XML).setDefault(Accept.TEXT_XML);
		assertList(x4, "Content-Type: text/xml", "Content-Type: text/xml", "Accept: text/xml");

		var x5 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_XML).setDefault(Accept.TEXT_XML).setDefault(ContentType.TEXT_HTML);
		assertList(x5, "Content-Type: text/xml", "Content-Type: text/xml", "Accept: text/xml");

		var x6 = HeaderList.create().setDefault(Accept.TEXT_XML,Accept.TEXT_PLAIN);
		assertList(x6, "Accept: text/xml");

		var x7 = HeaderList.create().setDefault(Accept.TEXT_XML).setDefault(Accept.TEXT_PLAIN);
		assertList(x7, "Accept: text/xml");

		var x8 = HeaderList.create().setDefault(Accept.TEXT_XML,Accept.TEXT_HTML).setDefault(Accept.TEXT_PLAIN);
		assertList(x8, "Accept: text/xml");

		var x9 = HeaderList
			.create()
			.setDefault((Header)null)
			.setDefault((HeaderList)null)
			.setDefault((Header[])null)
			.setDefault((List<Header>)null);
		assertEmpty(x9);

		var x10 = HeaderList.create().setDefault("Accept","text/xml");
		assertList(x10, "Accept: text/xml");

		var x11 = HeaderList.create().setDefault("Accept",()->"text/xml");
		assertList(x11, "Accept: text/xml");

		var x12 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_PLAIN).setDefault(alist(Accept.TEXT_XML,ContentType.TEXT_HTML,null));
		assertList(x12, "Content-Type: text/xml", "Content-Type: text/plain", "Accept: text/xml");

		var x13 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_PLAIN).setDefault(HeaderList.of(Accept.TEXT_XML,ContentType.TEXT_HTML,null));
		assertList(x13, "Content-Type: text/xml", "Content-Type: text/plain", "Accept: text/xml");

		var x14 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_PLAIN)
			.setDefault(alist(Accept.TEXT_XML,ContentType.TEXT_HTML,null))
			.setDefault(alist(Accept.TEXT_HTML,ContentType.TEXT_XML,null))
			.setDefault(alist(Age.of(1)));
		assertList(x14, "Content-Type: text/xml", "Content-Type: text/plain", "Accept: text/xml", "Age: 1");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.getPartSession();
	}
}