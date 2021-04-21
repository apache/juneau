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
package org.apache.juneau.http.header;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

/**
 * Tests: {@link HeaderList}, {@link HeaderListBuilder}, {@link BasicHeaderIterator}
 */
@FixMethodOrder(NAME_ASCENDING)
public class HeaderList_Test {

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

	@Test
	public void a01_basic() {
		HeaderListBuilder x = HeaderList.create();

		assertObject(x.build()).isString("[]");
		x.append(FOO_1);
		assertObject(x.build()).isString("[Foo: 1]");
		x.append(FOO_2);
		assertObject(x.build()).isString("[Foo: 1, Foo: 2]");
		x.append(HeaderList.of().getAll());
		assertObject(x.build()).isString("[Foo: 1, Foo: 2]");
		x.append(HeaderList.of(FOO_3).getAll());
		assertObject(x.build()).isString("[Foo: 1, Foo: 2, Foo: 3]");
		x.append(HeaderList.of(FOO_4, FOO_5).getAll());
		assertObject(x.build()).isString("[Foo: 1, Foo: 2, Foo: 3, Foo: 4, Foo: 5]");
		x.append(HeaderList.of(FOO_6, FOO_7).getAll());
		assertObject(x.build()).isString("[Foo: 1, Foo: 2, Foo: 3, Foo: 4, Foo: 5, Foo: 6, Foo: 7]");
		x.append((Header)null);
		assertObject(x.build()).isString("[Foo: 1, Foo: 2, Foo: 3, Foo: 4, Foo: 5, Foo: 6, Foo: 7]");
		x.append((List<Header>)null);
		assertObject(x.build()).isString("[Foo: 1, Foo: 2, Foo: 3, Foo: 4, Foo: 5, Foo: 6, Foo: 7]");

		assertObject(new HeaderList.Null()).isString("[]");
	}

	@Test
	public void a02_creators() {
		HeaderList x;

		x = headerList(FOO_1, FOO_2, null);
		assertObject(x).isString("[Foo: 1, Foo: 2]");

		x = headerList(AList.of(FOO_1, FOO_2, null));
		assertObject(x).isString("[Foo: 1, Foo: 2]");

		x = headerList("Foo","1","Foo","2");
		assertObject(x).isString("[Foo: 1, Foo: 2]");

		assertThrown(()->headerList("Foo")).is("Odd number of parameters passed into HeaderList.ofPairs()");

		x = HeaderList.of((List<Header>)null);
		assertObject(x).isString("[]");

		x = HeaderList.of(Collections.emptyList());
		assertObject(x).isString("[]");

		x = HeaderList.of(AList.of(FOO_1));
		assertObject(x).isString("[Foo: 1]");

		x = HeaderList.of((Header[])null);
		assertObject(x).isString("[]");

		x = HeaderList.of();
		assertObject(x).isString("[]");

		x = HeaderList.of(FOO_1);
		assertObject(x).isString("[Foo: 1]");

		x = HeaderList.ofPairs((String[])null);
		assertObject(x).isString("[]");

		x = HeaderList.ofPairs();
		assertObject(x).isString("[]");
	}

	@Test
	public void a03_addMethods() {
		String pname = "HeaderSupplierTest.x";

		HeaderListBuilder x = HeaderList.create().resolving();
		System.setProperty(pname, "y");

		x.append("X1","bar");
		x.append("X2","$S{"+pname+"}");
		x.append("X3","bar");
		x.append("X4",()->"$S{"+pname+"}");
		x.append(SerializedHeader.of("X5","bar",openApiSession(),null,false));

		assertObject(x.build()).isString("[X1: bar, X2: y, X3: bar, X4: y, X5: bar]");

		System.setProperty(pname, "z");

		assertObject(x.build()).isString("[X1: bar, X2: z, X3: bar, X4: z, X5: bar]");

		System.clearProperty(pname);
	}

	@Test
	public void a04_toArrayMethods() {
		HeaderListBuilder x = HeaderList
			.create()
			.append("X1","1")
			.append(headerList("X2","2").getAll());
		assertObject(x.build()).isString("[X1: 1, X2: 2]");
	}

	@Test
	public void a05_copy() {
		HeaderList x = HeaderList.of(FOO_1).copy().build();
		assertObject(x).isString("[Foo: 1]");
	}

	@Test
	public void a06_getCondensed() {
		HeaderList x = HeaderList.of(FOO_1);
		assertObject(x.get((String)null)).isNull();
		assertObject(x.get("Foo")).isString("Foo: 1");
		assertObject(x.get("Bar")).isNull();
		x = HeaderList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertObject(x.get("Foo")).isString("Foo: 1, 2, 3");
		assertObject(x.get("Bar")).isNull();
	}

	@org.apache.juneau.http.annotation.Header("Foo")
	static class Foo extends BasicStringHeader {
		private static final long serialVersionUID = 1L;

		public Foo(String value) {
			super("Foo", value);
		}
	}

	@Test
	public void a07_getCondensed_asType() {
		HeaderList x = HeaderList.of(FOO_1);
		assertObject(x.get(null, Allow.class)).isNull();
		assertObject(x.get("Foo", Allow.class)).isString("Allow: 1");
		assertObject(x.get("Bar", Allow.class)).isNull();
		x = HeaderList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertObject(x.get("Foo", Allow.class)).isString("Allow: 1, 2, 3");
		assertObject(x.get("Bar", Allow.class)).isNull();
		assertObject(x.get(Foo.class)).isString("Foo: 1, 2, 3");
		final HeaderList x2 = x;
		assertThrown(()->x2.get(String.class)).contains("Header name could not be found on bean type 'java.lang.String'");
	}

	@Test
	public void a08_get() {
		HeaderList x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertArray(x.getAll(null)).isString("[]");
		assertArray(x.getAll("Foo")).isString("[Foo: 1, Foo: 2]");
		assertArray(x.getAll("FOO")).isString("[Foo: 1, Foo: 2]");
		assertArray(x.getAll("Bar")).isString("[]");
	}

	@Test
	public void a09_getFirst() {
		HeaderList x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertObject(x.getFirst(null)).isNull();
		assertObject(x.getFirst("Foo")).isString("Foo: 1");
		assertObject(x.getFirst("FOO")).isString("Foo: 1");
		assertObject(x.getFirst("Bar")).isNull();
	}

	@Test
	public void a10_getLast() {
		HeaderList x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertObject(x.getLast(null)).isNull();
		assertObject(x.getLast("Foo")).isString("Foo: 2");
		assertObject(x.getLast("FOO")).isString("Foo: 2");
		assertObject(x.getLast("Bar")).isNull();
	}

	@Test
	public void a11_contains() {
		HeaderList x = HeaderList.of(FOO_1, FOO_2, X_x);
		assertBoolean(x.contains(null)).isFalse();
		assertBoolean(x.contains("Foo")).isTrue();
		assertBoolean(x.contains("FOO")).isTrue();
		assertBoolean(x.contains("Bar")).isFalse();
	}

	@Test
	public void a12_headerIterator_all() {
		HeaderList x = HeaderList.of();
		assertBoolean(x.iterator().hasNext()).isFalse();
		x = HeaderList.of(FOO_1);
		assertBoolean(x.iterator().hasNext()).isTrue();
	}

	@Test
	public void a13_headerIterator_single() {
		HeaderList x = HeaderList.of();
		assertBoolean(x.iterator("Foo").hasNext()).isFalse();
		x = HeaderList.of(FOO_1);
		assertBoolean(x.iterator("Foo").hasNext()).isTrue();
		assertBoolean(x.iterator("FOO").hasNext()).isTrue();
	}

	@Test
	public void a14_forEach_all() {
		HeaderList x = HeaderList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.forEach(h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = HeaderList.of(FOO_1, FOO_2);
		final AtomicInteger i2 = new AtomicInteger();
		x.forEach(h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a15_forEach_single() {
		HeaderList x = HeaderList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.forEach("FOO", h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = HeaderList.of(FOO_1, FOO_2, X_x);
		final AtomicInteger i2 = new AtomicInteger();
		x.forEach("FOO", h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a16_stream_all() {
		HeaderList x = HeaderList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.stream().forEach(h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = HeaderList.of(FOO_1, FOO_2);
		final AtomicInteger i2 = new AtomicInteger();
		x.stream().forEach(h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a17_stream_single() {
		HeaderList x = HeaderList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.stream("FOO").forEach(h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = HeaderList.of(FOO_1, FOO_2, X_x);
		final AtomicInteger i2 = new AtomicInteger();
		x.stream("FOO").forEach(h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a18_caseSensitive() {
		HeaderList x = HeaderList.create().caseSensitive().append(FOO_1, FOO_2, X_x).build();
		assertArray(x.getAll("Foo")).isString("[Foo: 1, Foo: 2]");
		assertArray(x.getAll("FOO")).isString("[]");
	}

	@Test
	public void a19_size() {
		HeaderList x = HeaderList.of(FOO_1);
		assertInteger(x.size()).is(1);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_builder_clear() {
		HeaderListBuilder x = HeaderList.create();
		x.append(FOO_1);
		x.clear();
		assertObject(x.build()).isString("[]");
	}

	@Test
	public void b02_builder_append() {
		HeaderList x1 = HeaderList.create().append(FOO_1).build();
		HeaderList x2 = HeaderList
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
			.append(AList.of(FOO_4))
			.build();
		assertObject(x2).isString("[Foo: 1, Foo: 2, Foo: 3, Bar: b1, Bar: b2, Foo: 4]");
	}

	@Test
	public void b03_builder_prepend() {
		HeaderList x1 = HeaderList.create().append(FOO_1).build();
		HeaderList x2 = HeaderList
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
			.prepend(AList.of(FOO_4))
			.build();
		assertObject(x2).isString("[Foo: 4, Bar: b2, Bar: b1, Foo: 2, Foo: 3, Foo: 1]");
	}

	@Test
	public void b04_builder_remove() {
		HeaderList x = HeaderList
			.create()
			.append(FOO_1,FOO_2,FOO_3,FOO_4,FOO_5,FOO_6,FOO_7)
			.remove((HeaderList)null)
			.remove((Header)null)
			.remove(HeaderList.of(FOO_1))
			.remove(FOO_2)
			.remove(FOO_3, FOO_4)
			.remove(AList.of(FOO_5))
			.build();
		assertObject(x).isString("[Foo: 6, Foo: 7]");

		x = HeaderList.create().append(FOO_1,FOO_2).remove((String[])null).remove("Bar","Foo").build();
		assertObject(x).isString("[]");
	}

	@Test
	public void b05_builder_set() {
		HeaderList x = null;

		x = HeaderList
			.create()
			.append(FOO_1,FOO_2)
			.set(FOO_3)
			.set(BAR_1)
			.set((Header)null)
			.set((HeaderList)null)
			.build();
		assertObject(x).isString("[Foo: 3, Bar: 1]");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(FOO_3)
			.build();
		assertObject(x).isString("[Bar: 1, Foo: 3, Bar: 2]");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((Header[])null)
			.set(null,FOO_3,FOO_4,FOO_5)
			.build();
		assertObject(x).isString("[Bar: 1, Bar: 2, Foo: 3, Foo: 4, Foo: 5]");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((List<Header>)null)
			.set(AList.of(null,FOO_3,FOO_4,FOO_5))
			.build();
		assertObject(x).isString("[Bar: 1, Bar: 2, Foo: 3, Foo: 4, Foo: 5]");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", "x")
			.build();
		assertObject(x).isString("[Bar: 1, FOO: x, Bar: 2]");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", ()->"x")
			.build();
		assertObject(x).isString("[Bar: 1, FOO: x, Bar: 2]");

		x = HeaderList
			.create()
			.caseSensitive()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", ()->"x")
			.build();
		assertObject(x).isString("[Bar: 1, Foo: 1, Foo: 2, Bar: 2, FOO: x]");

		x = HeaderList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(HeaderList.of(FOO_3,FOO_4))
			.build();
		assertObject(x).isString("[Bar: 1, Bar: 2, Foo: 3, Foo: 4]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHeaderIterator
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_iterators() {
		HeaderList x = HeaderList.of(Accept.TEXT_XML,ContentType.TEXT_XML);

		HeaderIterator i1 = x.iterator();
		assertObject(i1.nextHeader()).isString("Accept: text/xml");
		assertObject(i1.nextHeader()).isString("Content-Type: text/xml");
		assertThrown(()->i1.nextHeader()).contains("Iteration already finished.");

		HeaderIterator i2 = x.iterator();
		assertObject(i2.next()).isString("Accept: text/xml");
		assertObject(i2.nextHeader()).isString("Content-Type: text/xml");
		assertThrown(()->i2.next()).contains("Iteration already finished.");

		HeaderIterator i3 = x.iterator("accept");
		assertObject(i3.nextHeader()).isString("Accept: text/xml");
		assertThrown(()->i3.nextHeader()).contains("Iteration already finished.");

		HeaderList x2 = HeaderList.create().append(Accept.TEXT_XML,ContentType.TEXT_XML).caseSensitive().build();

		HeaderIterator i4 = x2.iterator("Accept");
		assertObject(i4.nextHeader()).isString("Accept: text/xml");
		assertThrown(()->i4.nextHeader()).contains("Iteration already finished.");

		HeaderIterator i5 = x2.iterator("accept");
		assertThrown(()->i5.nextHeader()).contains("Iteration already finished.");

		assertThrown(()->i5.remove()).contains("Remove is not supported.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Default headers
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_defaultHeaders() {
		HeaderList x1 = HeaderList.create().setDefault(Accept.TEXT_XML).build();
		assertObject(x1).isString("[Accept: text/xml]");

		HeaderList x2 = HeaderList.create().set(Accept.TEXT_PLAIN).setDefault(Accept.TEXT_XML).build();
		assertObject(x2).isString("[Accept: text/plain]");

		HeaderList x3 = HeaderList.create().set(ContentType.TEXT_XML,Accept.TEXT_PLAIN,ContentType.TEXT_XML).setDefault(Accept.TEXT_XML).build();
		assertObject(x3).isString("[Content-Type: text/xml, Accept: text/plain, Content-Type: text/xml]");

		HeaderList x4 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_XML).setDefault(Accept.TEXT_XML).build();
		assertObject(x4).isString("[Content-Type: text/xml, Content-Type: text/xml, Accept: text/xml]");

		HeaderList x5 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_XML).setDefault(Accept.TEXT_XML).setDefault(ContentType.TEXT_HTML).build();
		assertObject(x5).isString("[Content-Type: text/xml, Content-Type: text/xml, Accept: text/xml]");

		HeaderList x6 = HeaderList.create().setDefault(Accept.TEXT_XML,Accept.TEXT_PLAIN).build();
		assertObject(x6).isString("[Accept: text/xml, Accept: text/plain]");

		HeaderList x7 = HeaderList.create().setDefault(Accept.TEXT_XML).setDefault(Accept.TEXT_PLAIN).build();
		assertObject(x7).isString("[Accept: text/plain]");

		HeaderList x8 = HeaderList.create().setDefault(Accept.TEXT_XML,Accept.TEXT_HTML).setDefault(Accept.TEXT_PLAIN).build();
		assertObject(x8).isString("[Accept: text/plain]");

		HeaderList x9 = HeaderList
			.create()
			.setDefault((Header)null)
			.setDefault((HeaderList)null)
			.setDefault((Header[])null)
			.setDefault((List<Header>)null)
			.build();
		assertObject(x9).isString("[]");

		HeaderList x10 = HeaderList.create().setDefault("Accept","text/xml").build();
		assertObject(x10).isString("[Accept: text/xml]");

		HeaderList x11 = HeaderList.create().setDefault("Accept",()->"text/xml").build();
		assertObject(x11).isString("[Accept: text/xml]");

		HeaderList x12 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_PLAIN).setDefault(AList.of(Accept.TEXT_XML,ContentType.TEXT_HTML,null)).build();
		assertObject(x12).isString("[Content-Type: text/xml, Content-Type: text/plain, Accept: text/xml]");

		HeaderList x13 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_PLAIN).setDefault(HeaderList.of(Accept.TEXT_XML,ContentType.TEXT_HTML,null)).build();
		assertObject(x13).isString("[Content-Type: text/xml, Content-Type: text/plain, Accept: text/xml]");

		HeaderList x14 = HeaderList.create().set(ContentType.TEXT_XML,ContentType.TEXT_PLAIN)
			.setDefault(AList.of(Accept.TEXT_XML,ContentType.TEXT_HTML,null))
			.setDefault(AList.of(Accept.TEXT_HTML,ContentType.TEXT_XML,null))
			.setDefault(AList.of(Age.of(1)))
			.build();
		assertObject(x14).isString("[Content-Type: text/xml, Content-Type: text/plain, Accept: text/html, Age: 1]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.createPartSession(null);
	}
}
