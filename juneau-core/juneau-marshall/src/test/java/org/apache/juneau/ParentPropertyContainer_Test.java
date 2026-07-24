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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Regression tests for FINISHED-291.
 *
 * <p>
 * Verifies that a bean annotated with {@link ParentProperty} that is nested inside a collection or
 * map (or nested containers) receives its <b>nearest enclosing bean</b> as its parent when parsed,
 * skipping all intermediate containers.  This is the canonical {@code AddressBook}/{@code List<Person>}
 * case that previously failed to round-trip because the parser injected the containing {@code List}
 * (not the grandparent bean) as the parent.
 */
class ParentPropertyContainer_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test beans.
	//------------------------------------------------------------------------------------------------------------------

	public static class AddressBook {
		public List<Person> people = new ArrayList<>();
		public List<List<Person>> groups = new ArrayList<>();
		public Map<String,Person> byName = new LinkedHashMap<>();
		public Map<String,List<Person>> byCity = new LinkedHashMap<>();
		public Set<Person> members = new LinkedHashSet<>();
	}

	public static class Person {
		public String name;

		@ParentProperty
		public AddressBook addressBook;
	}

	static AddressBook newAddressBook() {
		var ab = new AddressBook();
		ab.people.add(person("p1"));
		ab.people.add(person("p2"));
		ab.groups.add(new ArrayList<>(List.of(person("g1"), person("g2"))));
		ab.byName.put("k1", person("m1"));
		ab.byCity.put("NYC", new ArrayList<>(List.of(person("c1"))));
		ab.members.add(person("s1"));
		return ab;
	}

	static Person person(String name) {
		var p = new Person();
		p.name = name;
		return p;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Format matrix.
	//------------------------------------------------------------------------------------------------------------------

	record Fmt(String name, Serializer s, Parser p) {
		@Override public String toString() { return name; }
	}

	static Stream<Fmt> formats() {
		return Stream.of(
			new Fmt("Json", JsonSerializer.DEFAULT, JsonParser.DEFAULT),
			new Fmt("Xml", XmlSerializer.DEFAULT, XmlParser.DEFAULT),
			new Fmt("Html", HtmlSerializer.DEFAULT, HtmlParser.DEFAULT),
			new Fmt("Uon", UonSerializer.DEFAULT, UonParser.DEFAULT),
			new Fmt("UrlEncoding", UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT),
			new Fmt("Yaml", YamlSerializer.DEFAULT, YamlParser.DEFAULT),
			new Fmt("MsgPack", MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT),
			new Fmt("Cbor", CborSerializer.DEFAULT, CborParser.DEFAULT),
			new Fmt("Bson", BsonSerializer.DEFAULT, BsonParser.DEFAULT),
			new Fmt("Hocon", HoconSerializer.DEFAULT, HoconParser.DEFAULT),
			new Fmt("Hjson", HjsonSerializer.DEFAULT, HjsonParser.DEFAULT)
		);
	}

	static Object write(Serializer s, Object o) throws Exception {
		if (s instanceof WriterSerializer ws)
			return ws.write(o);
		return ((OutputStreamSerializer)s).write(o);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tests.
	//------------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("formats")
	void a01_listElementParentIsEnclosingBean(Fmt f) throws Exception {
		var ab = newAddressBook();
		var ab2 = f.p.read(write(f.s, ab), AddressBook.class);

		assertEquals(2, ab2.people.size(), f.name);
		for (var p : ab2.people)
			assertSame(ab2, p.addressBook, () -> f.name + ": List<Person> element parent should be the enclosing AddressBook");
	}

	@ParameterizedTest
	@MethodSource("formats")
	void a02_nestedListElementParentSkipsAllContainers(Fmt f) throws Exception {
		var ab = newAddressBook();
		var ab2 = f.p.read(write(f.s, ab), AddressBook.class);

		assertEquals(1, ab2.groups.size(), f.name);
		var group = ab2.groups.get(0);
		assertEquals(2, group.size(), f.name);
		for (var p : group)
			assertSame(ab2, p.addressBook, () -> f.name + ": List<List<Person>> element parent should skip both lists");
	}

	@ParameterizedTest
	@MethodSource("formats")
	void a03_mapValueParentIsEnclosingBean(Fmt f) throws Exception {
		var ab = newAddressBook();
		var ab2 = f.p.read(write(f.s, ab), AddressBook.class);

		assertFalse(ab2.byName.isEmpty(), f.name);
		for (var p : ab2.byName.values())
			assertSame(ab2, p.addressBook, () -> f.name + ": Map<String,Person> value parent should be the enclosing AddressBook");
	}

	@ParameterizedTest
	@MethodSource("formats")
	void a04_mapOfListElementParentSkipsContainers(Fmt f) throws Exception {
		var ab = newAddressBook();
		var ab2 = f.p.read(write(f.s, ab), AddressBook.class);

		assertFalse(ab2.byCity.isEmpty(), f.name);
		for (var l : ab2.byCity.values())
			for (var p : l)
				assertSame(ab2, p.addressBook, () -> f.name + ": Map<String,List<Person>> element parent should skip map and list");
	}

	@ParameterizedTest
	@MethodSource("formats")
	void a05_setElementParentIsEnclosingBean(Fmt f) throws Exception {
		var ab = newAddressBook();
		var ab2 = f.p.read(write(f.s, ab), AddressBook.class);

		assertFalse(ab2.members.isEmpty(), f.name);
		for (var p : ab2.members)
			assertSame(ab2, p.addressBook, () -> f.name + ": Set<Person> element parent should be the enclosing AddressBook");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Top-level collection - parent should be null (no enclosing bean), not throw.
	//
	// Note: HOCON is excluded from the root-level-array cases.  HOCON is a config-file format whose document root
	// must be an object (braceless key/value pairs), so a top-level array is a parser limitation unrelated to
	// @ParentProperty.  HOCON's element-parent and null-parent semantics are still exercised by a01-a05.
	//------------------------------------------------------------------------------------------------------------------

	static Stream<Fmt> readerFormatsNoHocon() {
		return formats().filter(f -> f.s instanceof WriterSerializer && f.p instanceof ReaderParser && ! "Hocon".equals(f.name));
	}

	static Stream<Fmt> formatsNoHocon() {
		return formats().filter(f -> ! "Hocon".equals(f.name));
	}

	@ParameterizedTest
	@MethodSource("readerFormatsNoHocon")
	void b01_topLevelListElementHasNullParent(Fmt f) throws Exception {
		var l = new ArrayList<Person>(List.of(person("x"), person("y")));
		var out = write(f.s, l);
		@SuppressWarnings("unchecked")
		List<Person> l2 = (List<Person>) f.p.read((String) out, List.class, Person.class);

		assertEquals(2, l2.size(), f.name);
		for (var p : l2)
			assertNull(p.addressBook, () -> f.name + ": top-level list element should have a null parent (no enclosing bean)");
	}

	@ParameterizedTest
	@MethodSource("formatsNoHocon")
	void b02_topLevelListBinaryHasNullParent(Fmt f) throws Exception {
		// Binary/stream formats: round-trip via byte[] / InputStream.
		var l = new ArrayList<Person>(List.of(person("x")));
		Object out = write(f.s, l);
		@SuppressWarnings("unchecked")
		List<Person> l2 = out instanceof byte[] b
			? (List<Person>) f.p.read(new ByteArrayInputStream(b), List.class, Person.class)
			: (List<Person>) f.p.read((String) out, List.class, Person.class);

		assertEquals(1, l2.size(), f.name);
		assertNull(l2.get(0).addressBook, () -> f.name + ": top-level list element should have a null parent");
	}
}
