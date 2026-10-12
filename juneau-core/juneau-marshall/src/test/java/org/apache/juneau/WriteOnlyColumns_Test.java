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

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.parquet.*;
import org.junit.jupiter.api.*;

/**
 * A write-only bean property must not leak its name into tabular headers or generated schemas.
 */
class WriteOnlyColumns_Test extends TestBase {

	public static class User {
		public String name;

		@BeanProp(wo="true")
		public String secretword;

		static User create() {
			var x = new User();
			x.name = "alice";
			x.secretword = "s3cr3t";
			return x;
		}
	}

	private static List<User> list() {
		return List.of(User.create(), User.create());
	}

	@Test void a01_html_collectionTable() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ.write(list());
		assertTrue(s.contains("name"), s);
		assertFalse(s.contains("secretword"), s);
		assertFalse(s.contains("s3cr3t"), s);
	}

	@Test void a02_html_singleBean() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ.write(User.create());
		assertFalse(s.contains("secretword"), s);
		assertFalse(s.contains("s3cr3t"), s);
	}

	@Test void a03_htmlSchema() throws Exception {
		var s = HtmlSchemaSerializer.DEFAULT_SIMPLE.write(User.create());
		assertTrue(s.contains("name"), s);
		assertTrue(s.contains("<tr><td>secretword</td><td><table><tr><td>type</td><td>string</td></tr><tr><td>writeOnly</td><td><boolean>true</boolean></td></tr>"), s);
	}

	@Test void a04_csv() throws Exception {
		var s = CsvSerializer.DEFAULT.write(list());
		assertTrue(s.startsWith("name"), s);
		assertFalse(s.contains("secretword"), s);
		assertFalse(s.contains("s3cr3t"), s);
	}

	@Test void a05_markdown() throws Exception {
		var s = MarkdownSerializer.DEFAULT.write(list());
		assertTrue(s.contains("name"), s);
		assertFalse(s.contains("secretword"), s);
		assertFalse(s.contains("s3cr3t"), s);
		s = MarkdownSerializer.DEFAULT.write(User.create());
		assertFalse(s.contains("secretword"), s);
	}

	@Test void a06_jsonSchema() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession().getSchema(User.class).toString();
		assertTrue(s.contains("\"name\":{\"type\":\"string\"}"), s);
		// Advertised as writeOnly (so clients know it can be sent) rather than dropped.
		assertTrue(s.contains("\"secretword\":{\"type\":\"string\",\"writeOnly\":true}"), s);
	}

	@Test void a07_jsonSchemaSerializer() throws Exception {
		var s = JsonSerializer.DEFAULT.copy().build().write(User.create());
		assertFalse(s.contains("secretword"), s);
		var s2 = JsonSchemaSerializer.DEFAULT.write(User.create());
		assertTrue(s2.contains("\"name\":{\"type\":\"string\"}"), s2);
		assertTrue(s2.contains("\"secretword\":{\"type\":\"string\",\"writeOnly\":true}"), s2);
	}

	@Test void a08_parquet() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list());
		var text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
		assertTrue(text.contains("name"), "name column expected");
		assertFalse(text.contains("secretword"), "write-only column present in schema");
		assertFalse(text.contains("s3cr3t"));
	}

	@Test void b01_parserStillSetsWriteOnly() throws Exception {
		var x = org.apache.juneau.marshall.json5.Json5Parser.DEFAULT.read("{name:'bob',secretword:'pw'}", User.class);
		assertEquals("bob", x.name);
		assertEquals("pw", x.secretword);
	}

	@Test void b02_csvParserStillSetsWriteOnly() throws Exception {
		@SuppressWarnings("unchecked")
		var l = (List<User>)CsvParser.DEFAULT.read("name,secretword\nbob,pw\n", List.class, User.class);
		assertEquals("pw", l.get(0).secretword);
	}

	@Test void b03_beanMapPutStillSetsWriteOnly() throws Exception {
		var m = org.apache.juneau.marshall.MarshallingContext.DEFAULT.newBeanMap(User.class);
		m.put("secretword", "pw");
		assertEquals("pw", m.getBean().secretword);
		assertNull(m.get("secretword"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Beans with dynamic properties plus a write-only property.
	//------------------------------------------------------------------------------------------------------------------

	public static class Dyna {
		public String name;

		@BeanProp(wo="true")
		public String secretword;

		private final Map<String,Object> extras = new LinkedHashMap<>();

		@BeanProp(name="*")
		public Map<String,Object> getExtras() { return extras; }

		@BeanProp(name="*")
		public void setExtra(String key, Object value) { extras.put(key, value); }

		static Dyna create() {
			var x = new Dyna();
			x.name = "alice";
			x.secretword = "s3cr3t";
			x.extras.put("color", "red");
			return x;
		}
	}

	@Test void c01_dyna_msgpackRoundTrip() throws Exception {
		var s = MsgPackSerializer.create().keepNullProperties().build();
		var bytes = s.write(Dyna.create());
		assertFalse(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("secretword"));
		var x = MsgPackParser.DEFAULT.read(bytes, Dyna.class);
		assertEquals("alice", x.name);
		assertEquals("red", x.extras.get("color"));
	}

	@Test void c02_dyna_cborRoundTrip() throws Exception {
		var s = CborSerializer.create().keepNullProperties().build();
		var bytes = s.write(Dyna.create());
		assertFalse(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("secretword"));
		var x = CborParser.DEFAULT.read(bytes, Dyna.class);
		assertEquals("alice", x.name);
		assertEquals("red", x.extras.get("color"));
	}

	@Test void c03_dyna_json() throws Exception {
		var s = JsonSerializer.create().keepNullProperties().build().write(Dyna.create());
		assertFalse(s.contains("secretword"), s);
		assertTrue(s.contains("color"), s);
	}

	@Test void c04_dyna_parserStillSetsWriteOnly() throws Exception {
		var x = Json5Parser.DEFAULT.read("{name:'bob',secretword:'pw',color:'blue'}", Dyna.class);
		assertEquals("pw", x.secretword);
		assertEquals("blue", x.extras.get("color"));
	}

	@Test void c05_xml() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ.write(User.create());
		assertTrue(s.contains("alice"), s);
		assertFalse(s.contains("secretword"), s);
		assertFalse(s.contains("s3cr3t"), s);
		s = XmlSerializer.DEFAULT_SQ.write(list());
		assertFalse(s.contains("secretword"), s);
		var x = XmlParser.DEFAULT.read("<object><name>bob</name><secretword>pw</secretword></object>", User.class);
		assertEquals("pw", x.secretword);
	}

	public static class DynaWo {
		public String name;

		private final Map<String,Object> extras = new LinkedHashMap<>();

		@BeanProp(name="*", wo="true")
		public Map<String,Object> getExtras() { return extras; }

		@BeanProp(name="*")
		public void setExtra(String key, Object value) { extras.put(key, value); }
	}

	@Test void d01_writeOnlyDynaProperty_notWritten() throws Exception {
		var b = new DynaWo();
		b.name = "alice";
		b.extras.put("color", "red");
		var s = JsonSerializer.create().keepNullProperties().build().write(b);
		assertFalse(s.contains("color"), s);
		assertTrue(s.contains("alice"), s);
	}

	public static class Inner {
		public String a = "1";
		public String b = "2";
		public String c = "3";

		@BeanProp(wo="true")
		public String secretword = "s3cr3t";
	}

	public static class Outer {
		public String name = "outer";
		public Inner inner = new Inner();
	}

	@Test void d02_toml_inlineTableCountIgnoresWriteOnly() throws Exception {
		// Threshold 3: the three readable properties fit; the write-only one must not push it over.
		var toml = org.apache.juneau.marshall.toml.TomlSerializer.DEFAULT.write(new Outer());
		assertTrue(toml.contains("inner = {"), toml);
		assertFalse(toml.contains("secretword"), toml);
	}

	public static class InnerRo {
		public String a = "1";
		public String b = "2";

		@BeanProp(ro="true")
		public String c = "3";

		@BeanProp(ro="true")
		public String d = "4";
	}

	public static class OuterRo {
		public String name = "outer";
		public InnerRo inner = new InnerRo();
	}

	@Test void d03_toml_inlineTableCountIncludesReadOnly() throws Exception {
		// Read-only properties are still written, so four written properties exceed the threshold of 3.
		var toml = org.apache.juneau.marshall.toml.TomlSerializer.DEFAULT.write(new OuterRo());
		assertTrue(toml.contains("c = "), toml);
		assertFalse(toml.contains("inner = {"), toml);
	}
}
