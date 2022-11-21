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
package org.apache.juneau.http.part;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

/**
 * Tests: {@link PartList}, {@link PartList.Builder}, {@link BasicPartIterator}
 */
@FixMethodOrder(NAME_ASCENDING)
public class PartList_Test {

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
		public static APart X = new APart("x"), Y = new APart("y"), Z = new APart("z");
		public APart(Object value) {
			super("a", stringify(value));
		}
	}

	@FormData("b")
	public static class BPart extends BasicStringPart {
		public static BPart X = new BPart("x"), Y = new BPart("y"), Z = new BPart("z");
		public BPart(Object value) {
			super("b", stringify(value));
		}
	}

	@Path("c")
	public static class CPart extends BasicStringPart {
		public static CPart X = new CPart("x");
		public CPart(Object value) {
			super("c", stringify(value));
		}
	}

	@Test
	public void a01_basic() {
		PartList x = PartList.create();

		assertObject(x).isString("");
		x.append(FOO_1);
		assertObject(x).isString("Foo=1");
		x.append(FOO_2);
		assertObject(x).isString("Foo=1&Foo=2");
		x.append(PartList.of().getAll());
		assertObject(x).isString("Foo=1&Foo=2");
		x.append(PartList.of(FOO_3).getAll());
		assertObject(x).isString("Foo=1&Foo=2&Foo=3");
		x.append(PartList.of(FOO_4, FOO_5).getAll());
		assertObject(x).isString("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5");
		x.append(PartList.of(FOO_6, FOO_7).getAll());
		assertObject(x).isString("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5&Foo=6&Foo=7");
		x.append((NameValuePair)null);
		assertObject(x).isString("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5&Foo=6&Foo=7");
		x.append((List<NameValuePair>)null);
		assertObject(x).isString("Foo=1&Foo=2&Foo=3&Foo=4&Foo=5&Foo=6&Foo=7");

		assertObject(new PartList.Void()).isString("");
	}

	@Test
	public void a02_creators() {
		PartList x;

		x = partList(FOO_1, FOO_2, null);
		assertObject(x).isString("Foo=1&Foo=2");

		x = partList(alist(FOO_1, FOO_2, null));
		assertObject(x).isString("Foo=1&Foo=2");

		x = partList("Foo","1","Foo","2");
		assertObject(x).isString("Foo=1&Foo=2");

		assertThrown(()->partList("Foo")).asMessage().is("Odd number of parameters passed into PartList.ofPairs()");

		x = PartList.of((List<NameValuePair>)null);
		assertObject(x).isString("");

		x = PartList.of(Collections.emptyList());
		assertObject(x).isString("");

		x = PartList.of(alist(FOO_1));
		assertObject(x).isString("Foo=1");

		x = PartList.of((NameValuePair[])null);
		assertObject(x).isString("");

		x = PartList.of();
		assertObject(x).isString("");

		x = PartList.of(FOO_1);
		assertObject(x).isString("Foo=1");

		x = PartList.ofPairs((String[])null);
		assertObject(x).isString("");

		x = PartList.ofPairs();
		assertObject(x).isString("");
	}

	@Test
	public void a03_addMethods() {
		String pname = "PartSupplierTest.x";

		PartList x = PartList.create().resolving();
		System.setProperty(pname, "y");

		x.append("X1","bar");
		x.append("X2","$S{"+pname+"}");
		x.append("X3","bar");
		x.append("X4",()->"$S{"+pname+"}");
		x.append(new SerializedPart("X5","bar",HttpPartType.QUERY,openApiSession(),null,false));

		assertObject(x).isString("X1=bar&X2=y&X3=bar&X4=y&X5=bar");

		System.setProperty(pname, "z");

		assertObject(x).isString("X1=bar&X2=z&X3=bar&X4=z&X5=bar");

		System.clearProperty(pname);
	}

	@Test
	public void a04_toArrayMethods() {
		PartList x = PartList
			.create()
			.append("X1","1")
			.append(partList("X2","2").getAll());
		assertObject(x).isString("X1=1&X2=2");
	}

	@Test
	public void a05_copy() {
		PartList x = PartList.of(FOO_1).copy();
		assertObject(x).isString("Foo=1");
	}

	@Test
	public void a06_getCondensed() {
		PartList x = PartList.of(FOO_1);
		assertOptional(x.get((String)null)).isNull();
		assertOptional(x.get("Foo")).isString("Foo=1");
		assertOptional(x.get("Bar")).isNull();
		x = PartList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertOptional(x.get("Foo")).isString("Foo=1,2,3");
		assertOptional(x.get("Bar")).isNull();
	}

	@Query("Foo")
	static class Foo extends BasicStringPart {
		public Foo(Object value) {
			super("Foo", stringify(value));
		}
	}

	@Test
	public void a07_getCondensed_asType() {
		PartList x = PartList.of(FOO_1);
		assertOptional(x.get(null, APart.class)).isNull();
		assertOptional(x.get("Foo", APart.class)).isString("a=1");
		assertOptional(x.get("Bar", APart.class)).isNull();
		x = PartList.of(FOO_1, FOO_2, FOO_3, X_x);
		assertOptional(x.get("Foo", APart.class)).isString("a=1,2,3");
		assertOptional(x.get("Bar", APart.class)).isNull();
		assertOptional(x.get(Foo.class)).isString("Foo=1,2,3");
		final PartList x2 = x;
		assertThrown(()->x2.get(String.class)).asMessage().is("Part name could not be found on bean type 'java.lang.String'");
	}

	@Test
	public void a08_get() {
		PartList x = PartList.of(FOO_1, FOO_2, X_x);
		assertArray(x.getAll(null)).isString("[]");
		assertArray(x.getAll("Foo")).isString("[Foo=1, Foo=2]");
		assertArray(x.getAll("FOO")).isString("[]");
		assertArray(x.getAll("Bar")).isString("[]");
	}

	@Test
	public void a09_getFirst() {
		PartList x = PartList.of(FOO_1, FOO_2, X_x);
		assertOptional(x.getFirst(null)).isNull();
		assertOptional(x.getFirst("Foo")).isString("Foo=1");
		assertOptional(x.getFirst("FOO")).isNull();
		assertOptional(x.getFirst("Bar")).isNull();
	}

	@Test
	public void a10_getLast() {
		PartList x = PartList.of(FOO_1, FOO_2, X_x);
		assertOptional(x.getLast(null)).isNull();
		assertOptional(x.getLast("Foo")).isString("Foo=2");
		assertOptional(x.getLast("FOO")).isNull();
		assertOptional(x.getLast("Bar")).isNull();
	}

	@Test
	public void a11_contains() {
		PartList x = PartList.of(FOO_1, FOO_2, X_x);
		assertBoolean(x.contains(null)).isFalse();
		assertBoolean(x.contains("Foo")).isTrue();
		assertBoolean(x.contains("FOO")).isFalse();
		assertBoolean(x.contains("Bar")).isFalse();
	}

	@Test
	public void a12_partIterator_all() {
		PartList x = PartList.of();
		assertBoolean(x.partIterator().hasNext()).isFalse();
		x = PartList.of(FOO_1);
		assertBoolean(x.partIterator().hasNext()).isTrue();
	}

	@Test
	public void a13_partIterator_single() {
		PartList x = PartList.of();
		assertBoolean(x.partIterator("Foo").hasNext()).isFalse();
		x = PartList.of(FOO_1);
		assertBoolean(x.partIterator("Foo").hasNext()).isTrue();
		assertBoolean(x.partIterator("FOO").hasNext()).isFalse();
	}

	@Test
	public void a14_forEach_all() {
		PartList x = PartList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.forEach(h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = PartList.of(FOO_1, FOO_2);
		final AtomicInteger i2 = new AtomicInteger();
		x.forEach(h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a15_forEach_single() {
		PartList x = PartList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.forEach("Foo", h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = PartList.of(FOO_1, FOO_2, X_x);
		final AtomicInteger i2 = new AtomicInteger();
		x.forEach("Foo", h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a16_stream_all() {
		PartList x = PartList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.stream().forEach(h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = PartList.of(FOO_1, FOO_2);
		final AtomicInteger i2 = new AtomicInteger();
		x.stream().forEach(h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a17_stream_single() {
		PartList x = PartList.of();

		final AtomicInteger i1 = new AtomicInteger();
		x.stream("Foo").forEach(h -> i1.incrementAndGet());
		assertInteger(i1.get()).is(0);

		x = PartList.of(FOO_1, FOO_2, X_x);
		final AtomicInteger i2 = new AtomicInteger();
		x.stream("Foo").forEach(h -> i2.incrementAndGet());
		assertInteger(i2.get()).is(2);
	}

	@Test
	public void a18_caseSensitive() {
		PartList x1 = PartList.create().append(FOO_1, FOO_2, X_x);
		assertArray(x1.getAll("Foo")).isString("[Foo=1, Foo=2]");
		assertArray(x1.getAll("FOO")).isString("[]");

		PartList x2 = x1.copy().caseInsensitive(true);
		assertArray(x2.getAll("Foo")).isString("[Foo=1, Foo=2]");
		assertArray(x2.getAll("FOO")).isString("[Foo=1, Foo=2]");
	}

	@Test
	public void a19_size() {
		PartList x = PartList.of(FOO_1);
		assertInteger(x.size()).is(1);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_builder_clear() {
		PartList x = PartList.create();
		x.append(FOO_1);
		x.clear();
		assertObject(x).isString("");
	}

	@Test
	public void b02_builder_append() {
		PartList x1 = PartList.create().append(FOO_1);
		PartList x2 = PartList
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
		assertObject(x2).isString("Foo=1&Foo=2&Foo=3&Bar=b1&Bar=b2&Foo=4");
	}

	@Test
	public void b03_builder_prepend() {
		PartList x1 = PartList.create().append(FOO_1);
		PartList x2 = PartList
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
		assertObject(x2).isString("Foo=4&Bar=b2&Bar=b1&Foo=2&Foo=3&Foo=1");
	}

	@Test
	public void b04_builder_remove() {
		PartList x = PartList
			.create()
			.append(FOO_1,FOO_2,FOO_3,FOO_4,FOO_5,FOO_6,FOO_7)
			.remove((PartList)null)
			.remove((NameValuePair)null)
			.remove(PartList.of(FOO_1))
			.remove(FOO_2)
			.remove(FOO_3, FOO_4)
			.remove(alist(FOO_5));
		assertObject(x).isString("Foo=6&Foo=7");

		x = PartList.create().append(FOO_1,FOO_2).remove((String[])null).remove("Bar","Foo");
		assertObject(x).isString("");
	}

	@Test
	public void b05_builder_set() {
		PartList x = null;

		x = PartList
			.create()
			.append(FOO_1,FOO_2)
			.set(FOO_3)
			.set(BAR_1)
			.set((NameValuePair)null)
			.set((PartList)null);
		assertObject(x).isString("Foo=3&Bar=1");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(FOO_3);
		assertObject(x).isString("Bar=1&Foo=3&Bar=2");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((NameValuePair[])null)
			.set(null,FOO_3,FOO_4,FOO_5);
		assertObject(x).isString("Bar=1&Bar=2&Foo=3&Foo=4&Foo=5");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set((List<NameValuePair>)null)
			.set(alist(null,FOO_3,FOO_4,FOO_5));
		assertObject(x).isString("Bar=1&Bar=2&Foo=3&Foo=4&Foo=5");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.caseInsensitive(true)
			.set("FOO", "x");
		assertObject(x).isString("Bar=1&FOO=x&Bar=2");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.caseInsensitive(true)
			.set("FOO", ()->"x");
		assertObject(x).isString("Bar=1&FOO=x&Bar=2");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set("FOO", ()->"x");
		assertObject(x).isString("Bar=1&Foo=1&Foo=2&Bar=2&FOO=x");

		x = PartList
			.create()
			.append(BAR_1,FOO_1,FOO_2,BAR_2)
			.set(PartList.of(FOO_3,FOO_4));
		assertObject(x).isString("Bar=1&Bar=2&Foo=3&Foo=4");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicPartIterator
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_iterators() {
		PartList x = PartList.of(APart.X, BPart.X);

		PartIterator i1 = x.partIterator();
		assertObject(i1.next()).isString("a=x");
		assertObject(i1.next()).isString("b=x");
		assertThrown(()->i1.next()).asMessage().is("Iteration already finished.");

		PartIterator i2 = x.partIterator();
		assertObject(i2.next()).isString("a=x");
		assertObject(i2.next()).isString("b=x");
		assertThrown(()->i2.next()).asMessage().is("Iteration already finished.");

		PartIterator i3 = x.partIterator("a");
		assertObject(i3.next()).isString("a=x");
		assertThrown(()->i3.next()).asMessage().is("Iteration already finished.");

		PartIterator i4 = x.partIterator("A");
		assertThrown(()->i4.next()).asMessage().is("Iteration already finished.");

		PartList x2 = PartList.create().append(APart.X,BPart.X).caseInsensitive(true);

		PartIterator i5 = x2.partIterator("A");
		assertObject(i5.next()).isString("a=x");
		assertThrown(()->i5.next()).asMessage().is("Iteration already finished.");

		assertThrown(()->i5.remove()).asMessage().is("Not supported.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Default headers
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_defaultParts() {
		PartList x1 = PartList.create().setDefault(APart.X);
		assertObject(x1).isString("a=x");

		PartList x2 = PartList.create().set(APart.X).setDefault(APart.Y);
		assertObject(x2).isString("a=x");

		PartList x3 = PartList.create().set(BPart.X,APart.X,BPart.Y).setDefault(APart.Y);
		assertObject(x3).isString("b=x&a=x&b=y");

		PartList x4 = PartList.create().set(BPart.X,BPart.Y).setDefault(APart.X);
		assertObject(x4).isString("b=x&b=y&a=x");

		PartList x5 = PartList.create().set(BPart.X,BPart.Y).setDefault(APart.X).setDefault(BPart.X);
		assertObject(x5).isString("b=x&b=y&a=x");

		PartList x7 = PartList.create().setDefault(APart.X).setDefault(APart.Y);
		assertObject(x7).isString("a=x");

		PartList x8 = PartList.create().setDefault(APart.X,APart.Y).setDefault(APart.Z);
		assertObject(x8).isString("a=x");

		PartList x9 = PartList
			.create()
			.setDefault((NameValuePair)null)
			.setDefault((PartList)null)
			.setDefault((NameValuePair[])null)
			.setDefault((List<NameValuePair>)null);
		assertObject(x9).isString("");

		PartList x10 = PartList.create().setDefault("a","x");
		assertObject(x10).isString("a=x");

		PartList x11 = PartList.create().setDefault("a",()->"x");
		assertObject(x11).isString("a=x");

		PartList x12 = PartList.create().set(BPart.X,BPart.Y).setDefault(alist(APart.X,BPart.Z,null));
		assertObject(x12).isString("b=x&b=y&a=x");

		PartList x13 = PartList.create().set(BPart.X,BPart.Y).setDefault(PartList.of(APart.X,BPart.Z,null));
		assertObject(x13).isString("b=x&b=y&a=x");

		PartList x14 = PartList.create().set(BPart.X,BPart.Y)
			.setDefault(alist(APart.X,BPart.X,null))
			.setDefault(alist(APart.Y,BPart.Y,null))
			.setDefault(alist(CPart.X));
		assertObject(x14).isString("b=x&b=y&a=x&c=x");
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
