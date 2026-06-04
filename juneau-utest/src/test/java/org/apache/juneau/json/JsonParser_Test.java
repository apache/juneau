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
package org.apache.juneau.json;

import org.apache.juneau.json5.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

class JsonParser_Test extends TestBase {

	private static final JsonParser p = JsonParser.DEFAULT;

	//====================================================================================================
	// Test invalid input
	//====================================================================================================
	@Test void a01_invalidJson() {
		assertThrows(ParseException.class, ()->p.parse("{\na:1,\nb:xxx\n}", Object.class));
	}

	@Test void a02_nonExistentAttribute() throws Exception {
		var json = "{foo:,bar:}";
		var m = Json5Parser.DEFAULT.parse(json, Json5Map.class);
		assertEquals("{foo:null,bar:null}", m.toString());
	}

	@Test void a03_nonStringAsString() {
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->p.parse("123", String.class));
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->p.parse(" 123 ", String.class));
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->p.parse("{\"fa\":123}", A.class));
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->p.parse(" { \"fa\" : 123 } ", A.class));
		assertThrowsWithMessage(Exception.class, "Invalid quote character", ()->p.parse("'123'", String.class));
	}

	public static class A {
		public String fa;
	}

	@Test void a04_strictMode() {
		assertThrowsWithMessage(Exception.class, "Missing value detected.", ()->p.parse("{\"foo\":,\"bar\":}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Invalid quote character", ()->p.parse("{\"foo\":'bar'}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Invalid quote character", ()->p.parse("{'foo':\"bar\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Unquoted attribute detected.", ()->p.parse("{foo:\"bar\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "String concatenation detected.", ()->p.parse("{\"foo\":\"bar\"+\"baz\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "String concatenation detected.", ()->p.parse("{\"foo\":\"bar\" + \"baz\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Javascript comment detected.", ()->p.parse("{\"foo\":/*comment*/\"bar\"}", JsonMap.class));
	}

	/**
	 * JSON numbers and booleans should be representable as strings and converted accordingly.
	 */
	@Test void a05_primitivesAsStrings() throws Exception {
		var p2 = Json5Parser.DEFAULT;
		var s = Json5Serializer.DEFAULT;

		var json = "{f01:'1',f02:'1',f03:'true',f04:'true',f05:'1',f06:'1',f07:'1',f08:'1',f09:'1',f10:'1'}";
		var b = p2.parse(json, B.class);
		assertEquals("{f01:1,f02:1,f03:true,f04:true,f05:1.0,f06:1.0,f07:1,f08:1,f09:1,f10:1}", s.toString(b));

		json = "{f01:'',f02:'',f03:'',f04:'',f05:'',f06:'',f07:'',f08:'',f09:'',f10:''}";
		b = p2.parse(json, B.class);
		assertEquals("{f01:0,f02:0,f03:false,f04:false,f05:0.0,f06:0.0,f07:0,f08:0,f09:0,f10:0}", s.toString(b));
	}

	public static class B {
		public int f01;
		public Integer f02;
		public boolean f03;
		public Boolean f04;
		public float f05;
		public Float f06;
		public long f07;
		public Long f08;
		public byte f09;
		public Byte f10;
	}

	//====================================================================================================
	// testInvalidJsonNumbers
	// Lax parser allows octal and hexadecimal numbers.  Strict parser does not.
	//====================================================================================================
	@Test void a06_invalidJsonNumbers() {
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p.parse("\"\"", Number.class));
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p.parse("0123", Number.class));
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p.parse("-0123", Number.class));
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p.parse("0x123", Number.class));
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p.parse("-0x123", Number.class));
	}

	@Test void a06b_validJsonNumbers() throws Exception {
		assertEquals(0, p.parse("0", Number.class).intValue());
		assertEquals(0, p.parse("-0", Number.class).intValue());
	}

	//====================================================================================================
	// testUnquotedStrings
	// Lax parser allows unquoted strings if POJO can be converted from a string.
	//====================================================================================================
	@Test void a07_unquotedStrings() {
		assertThrows(ParseException.class, ()->p.parse("foobar", C.class));
	}

	public static class C {
		String f;
		public static C valueOf(String s) {
			var c = new C();
			c.f = s;
			return c;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "f="+f;
		}
	}

	//====================================================================================================
	// testStreamsAutoClose
	// Validates PARSER_autoCloseStreams.
	//====================================================================================================
	@Test void a08_streamsAutoClose() throws Exception {
		var p2 = Json5Parser.DEFAULT.copy().autoCloseStreams().build();
		var r = reader("{foo:'bar'}{baz:'qux'}");

		var x = p2.parse(r, JsonMap.class);
		assertBean(x, "foo", "bar");
		assertThrowsWithMessage(Exception.class, "Reader is closed", ()->p2.parse(r, JsonMap.class));
	}

	//====================================================================================================
	// testMultipleObjectsInStream
	// Validates that readers are not closed so that we can read streams of POJOs.
	//====================================================================================================
	@Test void a09_multipleObjectsInStream() throws Exception {
		var p2 = Json5Parser.create().unbuffered().build();
		var r = reader("{foo:'bar'}{baz:'qux'}");

		var x = (Object)p2.parse(r, JsonMap.class);
		assertBean(x, "foo", "bar");
		x = p2.parse(r, JsonMap.class);
		assertBean(x, "baz", "qux");

		r = reader("[123][456]");
		x = p2.parse(r, JsonList.class);
		assertList(x, "123");
		x = p2.parse(r, JsonList.class);
		assertList(x, "456");
	}

	private static Reader reader(String in) {
		return new CloseableStringReader(in);
	}

	public enum B_Enum { A, B, C }

	public static class B01_Bean {
		private Set<B_Enum> v;
		public Set<B_Enum> getV() { return v; }
		public B01_Bean setV(Set<B_Enum> x) { v = x; return this; }
	}

	// Headline case, mirroring the exact repro from FINISHED-147 (abstract Set<Enum>, empty array, null field).
	@Test void b01_emptyArrayIntoAbstractSetOfEnum() throws Exception {
		var p2 = JsonParser.create().ignoreUnknownBeanProperties().build();
		var x = p2.parse("{\"v\":[]}", B01_Bean.class);
		assertNotNull(x.getV());
		assertTrue(x.getV().isEmpty());
		assertInstanceOf(LinkedHashSet.class, x.getV());
	}

	// Non-empty array of the same shape must still populate (guards the populated path).
	@Test void b02_populatedArrayIntoAbstractSetOfEnum() throws Exception {
		var x = p.parse("{\"v\":[\"A\",\"B\"]}", B01_Bean.class);
		assertInstanceOf(LinkedHashSet.class, x.getV());
		assertEquals(Set.of(B_Enum.A, B_Enum.B), x.getV());
	}

	public static class B03_Bean {
		private List<B_Enum> v;
		public List<B_Enum> getV() { return v; }
		public B03_Bean setV(List<B_Enum> x) { v = x; return this; }
	}

	// For a List field the parser's native JsonList is itself a List, so it is assigned directly (no
	// abstract-materialization coercion is needed).  Either way the contract is: non-null, empty List.
	@Test void b03_emptyArrayIntoAbstractListOfEnum() throws Exception {
		var x = p.parse("{\"v\":[]}", B03_Bean.class);
		assertNotNull(x.getV());
		assertTrue(x.getV().isEmpty());
		assertInstanceOf(List.class, x.getV());
	}

	public static class B04_Bean {
		private Map<String,B_Enum> v;
		public Map<String,B_Enum> getV() { return v; }
		public B04_Bean setV(Map<String,B_Enum> x) { v = x; return this; }
	}

	@Test void b04_emptyObjectIntoAbstractMapOfEnum() throws Exception {
		var x = p.parse("{\"v\":{}}", B04_Bean.class);
		assertNotNull(x.getV());
		assertTrue(x.getV().isEmpty());
		assertInstanceOf(LinkedHashMap.class, x.getV());
	}

	public static class B05_Bean {
		private Set<String> v;
		public Set<String> getV() { return v; }
		public B05_Bean setV(Set<String> x) { v = x; return this; }
	}

	@Test void b05_emptyArrayIntoAbstractSetOfString() throws Exception {
		var x = p.parse("{\"v\":[]}", B05_Bean.class);
		assertNotNull(x.getV());
		assertTrue(x.getV().isEmpty());
		assertInstanceOf(LinkedHashSet.class, x.getV());
	}

	public static class B06_Bean {
		private List<String> v;
		public List<String> getV() { return v; }
		public B06_Bean setV(List<String> x) { v = x; return this; }
	}

	@Test void b06_emptyArrayIntoAbstractListOfString() throws Exception {
		var x = p.parse("{\"v\":[]}", B06_Bean.class);
		assertNotNull(x.getV());
		assertTrue(x.getV().isEmpty());
		assertInstanceOf(List.class, x.getV());
	}

	public static class B07_Bean {
		private Map<String,String> v;
		public Map<String,String> getV() { return v; }
		public B07_Bean setV(Map<String,String> x) { v = x; return this; }
	}

	@Test void b07_emptyObjectIntoAbstractMapOfString() throws Exception {
		var x = p.parse("{\"v\":{}}", B07_Bean.class);
		assertNotNull(x.getV());
		assertTrue(x.getV().isEmpty());
		assertInstanceOf(LinkedHashMap.class, x.getV());
	}
}