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
package org.apache.juneau.http.part;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link PartList}, {@link PartList.Builder}, {@link BasicPartIterator}
 */
class PartList_Test extends TestBase {

	private static final NameValuePair
		FOO_1 = part("Foo","1"),
		FOO_2 = part("Foo","2"),
		FOO_3 = part("Foo","3"),
		FOO_4 = part("Foo","4"),
		FOO_5 = part("Foo","5"),
		FOO_6 = part("Foo","6"),
		FOO_7 = part("Foo","7"),
		BAR_1 = part("Bar","1"),
		BAR_2 = part("Bar","2"),

		X_x = part("X", "x");

	@Query("a")
	public static class APart extends BasicStringPart {
		public static final APart X = new APart("x"), Y = new APart("y"), Z = new APart("z");
		public APart(Object value) {
			super("a", s(value));
		}
	}

	@FormData("b")
	public static class BPart extends BasicStringPart {
		public static final BPart X = new BPart("x"), Y = new BPart("y"), Z = new BPart("z");
		public BPart(Object value) {
			super("b", s(value));
		}
	}

	@Path("c")
	public static class CPart extends BasicStringPart {
		public static final CPart X = new CPart("x");
		public CPart(Object value) {
			super("c", s(value));
		}
	}

	@Test void a01_basic() {
		var x = PartList.create();

		assertEquals("", s(x));
		x.append(FOO_1);
		assertEquals("Foo=1", s(x));
		x.append(FOO_2);
		assertEquals("Foo=1&Foo=2", s(x));
		x.append(PartList.of().getAll());
		assertEquals("Foo=1&Foo=2", s(x));
		x.append(PartList.of(FOO_3).getAll());
		assertEquals("Foo=1&Foo=2&Foo=3", s(x));
		x.append(PartList.of(FOO_4, FOO_5).getAll());
		assertEquals("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5", s(x));
		x.append(PartList.of(FOO_6, FOO_7).getAll());
		assertEquals("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5&Foo=6&Foo=7", s(x));
		x.append((NameValuePair)null);
		assertEquals("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5&Foo=6&Foo=7", s(x));
		x.append((List<NameValuePair>)null);
		assertEquals("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5&Foo=6&Foo=7", s(x));

		assertEquals("", s(new PartList.Void()));
	}

	@Test void a02_creators() {
		var x = partList(FOO_1, FOO_2, null);

		assertEquals("Foo=1&Foo=2", s(x));

		x = partList(alist(FOO_1, FOO_2, null));
		assertEquals("Foo=1&Foo=2", s(x));

		x = partList("Foo","1","Foo","2");
		assertEquals("Foo=1&Foo=2", s(x));

		assertThrowsWithMessage(IllegalArgumentException.class, "Odd number of parameters passed into PartList.ofPairs()", ()->partList("Foo"));

		x = PartList.of((List<NameValuePair>)null);
		assertEquals("", s(x));

		x = PartList.of(Collections.emptyList());
		assertEquals("", s(x));

		x = PartList.of(alist(FOO_1));
		assertEquals("Foo=1", s(x));

		x = PartList.of((NameValuePair[])null);
		assertEquals("", s(x));

		x = PartList.of();
		assertEquals("", s(x));

		x = PartList.of(FOO_1);
		assertEquals("Foo=1", s(x));

		x = PartList.ofPairs((String[])null);
		assertEquals("", s(x));

		x = PartList.ofPairs();
		assertEquals("", s(x));
	}

	@Test void a03_addMethods() {
		var pname = "PartSupplierTest.x";

		var x = PartList.create().resolving();
		System.setProperty(pname, "y");

		x.append("X1","bar");
		x.append("X2","$S{"+pname+"}");
		x.append("X3","bar");
		x.append("X4",()->"$S{"+pname+"}");
		x.append(new SerializedPart("X5","bar",HttpPartType.QUERY,openApiSession(),null,false));

		assertEquals("X1=bar&X2=y&X3=bar&X4=y&X5=bar", s(x));

		System.setProperty(pname, "z");

		assertEquals("X1=bar&X2=z&X3=bar&X4=z&X5=bar", s(x));

		System.clearProperty(pname);
	}

	@Test void a04_toArrayMethods() {
		var x = PartList
			.create()
			.append("X1","1")
			.append(partList("X2","2").getAll());
		assertEquals("X1=1&X2=2", s(x));
	}

	@Test void a05_copy() {
		var x = PartList.of(FOO_1).copy();
		assertEquals("Foo=1", s(x));
	}

	@Test void a06_getCondensed() {
		var x = PartList.of(FOO_1);
		assertEmpty(x.get((String)null));
		assertString("Foo=1", x.get("Foo"));
		assertEmpty(x.get("Bar"));
		x = PartList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertString("Foo=1,2,3", x.get("Foo"));
		assertEmpty(x.get("Bar"));
	}

	@Query("Foo")
	static class Foo extends BasicStringPart {
		public Foo(Object value) {
			super("Foo", s(value));
		}
	}

	@Test void a07_getCondensed_asType() {
		var x = PartList.of(FOO_1);
		assertEmpty(x.get(null, APart.class));
		assertString("a=1", x.get("Foo", APart.class));
		assertEmpty(x.get("Bar", APart.class));
		x = PartList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertString("a=1,2,3", x.get("Foo", APart.class));
		assertEmpty(x.get("Bar", APart.class));
		assertString("Foo=1,2,3", x.get(Foo.class));
		var x2 = x;
		assertThrowsWithMessage(IllegalArgumentException.class, "Part name could not be found on bean type 'java.lang.String'", ()->x2.get(String.class));
	}

	@Test void a08_get() {
		var x = PartList.of(FOO_1, FOO_2, X_x);
		assertEmpty(x.getAll(null));
		assertList(x.getAll("Foo"), "Foo=1", "Foo=2");
		assertEmpty(x.getAll("FOO"));
		assertEmpty(x.getAll("Bar"));
	}

	@Test void a09_getFirst() {
		var x = PartList.of(FOO_1, FOO_2, X_x);
		assertEmpty(x.getFirst(null));
		assertString("Foo=1", x.getFirst("Foo"));
		assertEmpty(x.getFirst("FOO"));
		assertEmpty(x.getFirst("Bar"));
	}

	@Test void a10_getLast() {
		var x = PartList.of(FOO_1, FOO_2, X_x);
		assertEmpty(x.getLast(null));
		assertString("Foo=2", x.getLast("Foo"));
		assertEmpty(x.getLast("FOO"));
		assertEmpty(x.getLast("Bar"));
	}

	@Test void a11_contains() {
		var x = PartList.of(FOO_1, FOO_2, X_x);
		assertFalse(x.contains(null));
		assertTrue(x.contains("Foo"));
		assertFalse(x.contains("FOO"));
		assertFalse(x.contains("Bar"));
	}

	@Test void a12_partIterator_all() {
		var x = PartList.of();
		assertFalse(x.partIterator().hasNext());
		x = PartList.of(FOO_1);
		assertTrue(x.partIterator().hasNext());
	}

	@Test void a13_partIterator_single() {
		var x = PartList.of();
		assertFalse(x.partIterator("Foo").hasNext());
		x = PartList.of(FOO_1);
		assertTrue(x.partIterator("Foo").hasNext());
		assertFalse(x.partIterator("FOO").hasNext());
	}

	@Test void a14_forEach_all() {
		var x = PartList.of();

		var i1 = new AtomicInteger();
		x.forEach(h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = PartList.of(FOO_1, FOO_2);
		var i2 = new AtomicInteger();
		x.forEach(h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a15_forEach_single() {
		var x = PartList.of();

		var i1 = new AtomicInteger();
		x.forEach("Foo", h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = PartList.of(FOO_1, FOO_2, X_x);
		var i2 = new AtomicInteger();
		x.forEach("Foo", h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a16_stream_all() {
		var x = PartList.of();

		var i1 = new AtomicInteger();
		x.stream().forEach(h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = PartList.of(FOO_1, FOO_2);
		var i2 = new AtomicInteger();
		x.stream().forEach(h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a17_stream_single() {
		var x = PartList.of();

		var i1 = new AtomicInteger();
		x.stream("Foo").forEach(h -> i1.incrementAndGet());
		assertEquals(0, i1.get());

		x = PartList.of(FOO_1, FOO_2, X_x);
		var i2 = new AtomicInteger();
		x.stream("Foo").forEach(h -> i2.incrementAndGet());
		assertEquals(2, i2.get());
	}

	@Test void a18_caseSensitive() {
		var x1 = PartList.create().append(FOO_1, FOO_2, X_x);
		assertList(x1.getAll("Foo"), "Foo=1", "Foo=2");
		assertEmpty(x1.getAll("FOO"));

		var x2 = x1.copy().caseInsensitive(true);
		assertList(x2.getAll("Foo"), "Foo=1", "Foo=2");
		assertList(x2.getAll("FOO"), "Foo=1", "Foo=2");
	}

	@Test void a19_size() {
		var x = PartList.of(FOO_1);
		assertEquals(1, x.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_builder_clear() {
		var x = PartList.create();
		x.append(FOO_1);
		x.clear();
		assertEquals("", s(x));
	}

	@Test void b02_builder_append() {
		var x1 = PartList.create().append(FOO_1);
		var x2 = PartList
			.create()
			.append()
			.append((PartList)null)
			.append((NameValuePair)null)
			.append((NameValuePair[])null)
			.append(x1)
			.append(FOO_2, FOO_3)
			.append("Bar", "b1")
			.append("Bar", ()->"b2")
			.append((List<NameValuePair>)null)
			.append(alist(FOO_4));
		assertEquals("Foo=1&Foo=2&Foo=3&Bar=b1&Bar=b2&Foo=4", s(x2));
	}

	@Test void b03_builder_prepend() {
		var x1 = PartList.create().append(FOO_1);
		var x2 = PartList
			.create()
			.prepend()
			.prepend((PartList)null)
			.prepend((NameValuePair)null)
			.prepend((NameValuePair[])null)
			.prepend(x1)
			.prepend(FOO_2, FOO_3)
			.prepend("Bar", "b1")
			.prepend("Bar", ()->"b2")
			.prepend((List<NameValuePair>)null)
			.prepend(alist(FOO_4));
		assertEquals("Foo=4&Bar=b2&Bar=b1&Foo=2&Foo=3&Foo=1", s(x2));
	}

	@Test void b04_builder_remove() {
		var x = PartList
			.create()
			.append(FOO_1,FOO_2,FOO_3,FOO_4,FOO_5,FOO_6,FOO_7)
			.remove((PartList)null)
			.remove((NameValuePair)null)
			.remove(PartList.of(FOO_1))
			.remove(FOO_2)
			.remove(FOO_3, FOO_4)
			.remove(alist(FOO_5));
		assertEquals("Foo=6&Foo=7", s(x));

		x = PartList.create().append(FOO_1,FOO_2).remove((String[])null).remove("Bar","Foo");
		assertEquals("", s(x));
	}

	@Test void b05_builder_set() {
		var x = PartList
			.create()
			.append(FOO_1,FOO_2)
			.set(FOO_3)
			.set(BAR_1)
			.set((NameValuePair)null)
			.set((PartList)null);
		assertEquals("Foo=3&Bar=1", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(FOO_3);
		assertEquals("Bar=1&Foo=3&Bar=2", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((NameValuePair[])null)
			.set(null,FOO_3,FOO_4,FOO_5);
		assertEquals("Bar=1&Bar=2&Foo=3&Foo=4&Foo=5", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((List<NameValuePair>)null)
			.set(alist(null,FOO_3,FOO_4,FOO_5));
		assertEquals("Bar=1&Bar=2&Foo=3&Foo=4&Foo=5", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.caseInsensitive(true)
			.set("FOO", "x");
		assertEquals("Bar=1&FOO=x&Bar=2", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.caseInsensitive(true)
			.set("FOO", ()->"x");
		assertEquals("Bar=1&FOO=x&Bar=2", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", ()->"x");
		assertEquals("Bar=1&Foo=1&Foo=2&Bar=2&FOO=x", s(x));

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(PartList.of(FOO_3,FOO_4));
		assertEquals("Bar=1&Bar=2&Foo=3&Foo=4", s(x));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicPartIterator
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_iterators() {
		var x = PartList.of(APart.X, BPart.X);

		var i1 = x.partIterator();
		assertString("a=x", i1.next());
		assertString("b=x", i1.next());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i1::next);

		var i2 = x.partIterator();
		assertString("a=x", i2.next());
		assertString("b=x", i2.next());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i2::next);

		var i3 = x.partIterator("a");
		assertString("a=x", i3.next());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i3::next);

		var i4 = x.partIterator("A");
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i4::next);

		var x2 = PartList.create().append(APart.X,BPart.X).caseInsensitive(true);

		var i5 = x2.partIterator("A");
		assertString("a=x", i5.next());
		assertThrowsWithMessage(NoSuchElementException.class, "Iteration already finished.", i5::next);

		assertThrowsWithMessage(UnsupportedOperationException.class, "Not supported.", i5::remove);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Default headers
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_defaultParts() {
		var x1 = PartList.create().setDefault(APart.X);
		assertEquals("a=x", s(x1));

		var x2 = PartList.create().set(APart.X).setDefault(APart.Y);
		assertEquals("a=x", s(x2));

		var x3 = PartList.create().set(BPart.X,APart.X,BPart.Y).setDefault(APart.Y);
		assertEquals("b=x&a=x&b=y", s(x3));

		var x4 = PartList.create().set(BPart.X,BPart.Y).setDefault(APart.X);
		assertEquals("b=x&b=y&a=x", s(x4));

		var x5 = PartList.create().set(BPart.X,BPart.Y).setDefault(APart.X).setDefault(BPart.X);
		assertEquals("b=x&b=y&a=x", s(x5));

		var x7 = PartList.create().setDefault(APart.X).setDefault(APart.Y);
		assertEquals("a=x", s(x7));

		var x8 = PartList.create().setDefault(APart.X,APart.Y).setDefault(APart.Z);
		assertEquals("a=x", s(x8));

		var x9 = PartList
			.create()
			.setDefault((NameValuePair)null)
			.setDefault((PartList)null)
			.setDefault((NameValuePair[])null)
			.setDefault((List<NameValuePair>)null);
		assertEquals("", s(x9));

		var x10 = PartList.create().setDefault("a","x");
		assertEquals("a=x", s(x10));

		var x11 = PartList.create().setDefault("a",()->"x");
		assertEquals("a=x", s(x11));

		var x12 = PartList.create().set(BPart.X,BPart.Y).setDefault(alist(APart.X,BPart.Z,null));
		assertEquals("b=x&b=y&a=x", s(x12));

		var x13 = PartList.create().set(BPart.X,BPart.Y).setDefault(PartList.of(APart.X,BPart.Z,null));
		assertEquals("b=x&b=y&a=x", s(x13));

		var x14 = PartList.create().set(BPart.X,BPart.Y)
			.setDefault(alist(APart.X,BPart.X,null))
			.setDefault(alist(APart.Y,BPart.Y,null))
			.setDefault(alist(CPart.X));
		assertEquals("b=x&b=y&a=x&c=x", s(x14));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static NameValuePair part(String name, Object val) {
		return basicPart(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.getPartSession();
	}
}