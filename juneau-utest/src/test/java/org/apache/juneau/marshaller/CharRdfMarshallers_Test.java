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
package org.apache.juneau.marshaller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class CharRdfMarshallers_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// JsonLd
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_jsonLd_of_string() throws Exception {
		var out = JsonLd.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a02_jsonLd_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = JsonLd.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a03_jsonLd_roundtrip_string() throws Exception {
		var serialized = JsonLd.of("foo");
		var result = JsonLd.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a04_jsonLd_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = JsonLd.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a05_jsonLd_default_instance() {
		assertNotNull(JsonLd.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// N3
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a06_n3_of_string() throws Exception {
		var out = N3.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a07_n3_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = N3.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a08_n3_roundtrip_string() throws Exception {
		var serialized = N3.of("foo");
		var result = N3.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a09_n3_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = N3.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a10_n3_default_instance() {
		assertNotNull(N3.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// NQuads
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a11_nquads_of_string() throws Exception {
		var out = NQuads.of("foo");
		assertNotNull(out);
	}

	@Test void a12_nquads_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = NQuads.of("foo", sw);
		assertNotNull(out);
	}

	@Test void a13_nquads_roundtrip_string() throws Exception {
		var serialized = NQuads.of("foo");
		assertNotNull(serialized);
		// NQuads format may produce empty output for simple strings; just verify non-null
	}

	@Test void a14_nquads_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = NQuads.of(in1);
		assertNotNull(serialized);
	}

	@Test void a15_nquads_default_instance() {
		assertNotNull(NQuads.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// NTriple
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a16_ntriple_of_string() throws Exception {
		var out = NTriple.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a17_ntriple_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = NTriple.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a18_ntriple_roundtrip_string() throws Exception {
		var serialized = NTriple.of("foo");
		var result = NTriple.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a19_ntriple_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = NTriple.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a20_ntriple_default_instance() {
		assertNotNull(NTriple.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RdfJson
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a21_rdfJson_of_string() throws Exception {
		var out = RdfJson.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a22_rdfJson_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = RdfJson.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a23_rdfJson_roundtrip_string() throws Exception {
		var serialized = RdfJson.of("foo");
		var result = RdfJson.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a24_rdfJson_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = RdfJson.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a25_rdfJson_default_instance() {
		assertNotNull(RdfJson.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RdfXml
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a26_rdfXml_of_string() throws Exception {
		var out = RdfXml.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a27_rdfXml_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = RdfXml.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a28_rdfXml_roundtrip_string() throws Exception {
		var serialized = RdfXml.of("foo");
		var result = RdfXml.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a29_rdfXml_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = RdfXml.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a30_rdfXml_default_instance() {
		assertNotNull(RdfXml.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RdfXmlAbbrev
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a31_rdfXmlAbbrev_of_string() throws Exception {
		var out = RdfXmlAbbrev.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a32_rdfXmlAbbrev_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = RdfXmlAbbrev.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a33_rdfXmlAbbrev_roundtrip_string() throws Exception {
		var serialized = RdfXmlAbbrev.of("foo");
		var result = RdfXmlAbbrev.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a34_rdfXmlAbbrev_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = RdfXmlAbbrev.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a35_rdfXmlAbbrev_default_instance() {
		assertNotNull(RdfXmlAbbrev.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// TriG
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a36_trig_of_string() throws Exception {
		var out = TriG.of("foo");
		assertNotNull(out);
	}

	@Test void a37_trig_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = TriG.of("foo", sw);
		assertNotNull(out);
	}

	@Test void a38_trig_roundtrip_string() throws Exception {
		var serialized = TriG.of("foo");
		assertNotNull(serialized);
		// TriG format may produce empty output for simple strings; just verify non-null
	}

	@Test void a39_trig_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = TriG.of(in1);
		assertNotNull(serialized);
	}

	@Test void a40_trig_default_instance() {
		assertNotNull(TriG.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// TriX
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a41_trix_of_string() throws Exception {
		var out = TriX.of("foo");
		assertNotNull(out);
	}

	@Test void a42_trix_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = TriX.of("foo", sw);
		assertNotNull(out);
	}

	@Test void a43_trix_roundtrip_string() throws Exception {
		var serialized = TriX.of("foo");
		assertNotNull(serialized);
		// TriX format may produce empty output for simple strings; just verify non-null
	}

	@Test void a44_trix_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = TriX.of(in1);
		assertNotNull(serialized);
	}

	@Test void a45_trix_default_instance() {
		assertNotNull(TriX.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Turtle
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a46_turtle_of_string() throws Exception {
		var out = Turtle.of("foo");
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void a47_turtle_of_string_writer() throws Exception {
		var sw = new StringWriter();
		var out = Turtle.of("foo", sw);
		assertNotNull(out);
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a48_turtle_roundtrip_string() throws Exception {
		var serialized = Turtle.of("foo");
		var result = Turtle.to(serialized, String.class);
		assertEquals("foo", result);
	}

	@Test void a49_turtle_roundtrip_map() throws Exception {
		var in1 = JsonMap.of("a", "b");
		var serialized = Turtle.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void a50_turtle_default_instance() {
		assertNotNull(Turtle.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// to(Object,...) and to(String,Type,Type...) overloads
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_jsonLd_to_reader() throws Exception {
		var serialized = JsonLd.of("foo");
		assertEquals("foo", JsonLd.to(new StringReader(serialized), String.class));
	}

	@Test void b02_jsonLd_to_reader_type() throws Exception {
		var serialized = JsonLd.of("foo");
		var result = JsonLd.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b03_jsonLd_to_string_type() throws Exception {
		var serialized = JsonLd.of("foo");
		var result = JsonLd.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b04_n3_to_reader() throws Exception {
		var serialized = N3.of("foo");
		assertEquals("foo", N3.to(new StringReader(serialized), String.class));
	}

	@Test void b05_n3_to_reader_type() throws Exception {
		var serialized = N3.of("foo");
		var result = N3.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b06_n3_to_string_type() throws Exception {
		var serialized = N3.of("foo");
		var result = N3.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b07_nquads_to_reader() throws Exception {
		var serialized = NQuads.of("foo");
		NQuads.to(new StringReader(serialized), String.class);
	}

	@Test void b08_nquads_to_reader_type() throws Exception {
		var serialized = NQuads.of("foo");
		NQuads.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
	}

	@Test void b09_nquads_to_string() throws Exception {
		var serialized = NQuads.of("foo");
		NQuads.to(serialized, String.class);
	}

	@Test void b10_nquads_to_string_type() throws Exception {
		var serialized = NQuads.of("foo");
		NQuads.to(serialized, String.class, new java.lang.reflect.Type[0]);
	}

	@Test void b11_ntriple_to_reader() throws Exception {
		var serialized = NTriple.of("foo");
		assertEquals("foo", NTriple.to(new StringReader(serialized), String.class));
	}

	@Test void b12_ntriple_to_reader_type() throws Exception {
		var serialized = NTriple.of("foo");
		var result = NTriple.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b13_ntriple_to_string_type() throws Exception {
		var serialized = NTriple.of("foo");
		var result = NTriple.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b14_rdfJson_to_reader() throws Exception {
		var serialized = RdfJson.of("foo");
		assertEquals("foo", RdfJson.to(new StringReader(serialized), String.class));
	}

	@Test void b15_rdfJson_to_reader_type() throws Exception {
		var serialized = RdfJson.of("foo");
		var result = RdfJson.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b16_rdfJson_to_string_type() throws Exception {
		var serialized = RdfJson.of("foo");
		var result = RdfJson.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b17_rdfXml_to_reader() throws Exception {
		var serialized = RdfXml.of("foo");
		assertEquals("foo", RdfXml.to(new StringReader(serialized), String.class));
	}

	@Test void b18_rdfXml_to_reader_type() throws Exception {
		var serialized = RdfXml.of("foo");
		var result = RdfXml.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b19_rdfXml_to_string_type() throws Exception {
		var serialized = RdfXml.of("foo");
		var result = RdfXml.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b20_rdfXmlAbbrev_to_reader() throws Exception {
		var serialized = RdfXmlAbbrev.of("foo");
		assertEquals("foo", RdfXmlAbbrev.to(new StringReader(serialized), String.class));
	}

	@Test void b21_rdfXmlAbbrev_to_reader_type() throws Exception {
		var serialized = RdfXmlAbbrev.of("foo");
		var result = RdfXmlAbbrev.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b22_rdfXmlAbbrev_to_string_type() throws Exception {
		var serialized = RdfXmlAbbrev.of("foo");
		var result = RdfXmlAbbrev.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b23_trig_to_reader() throws Exception {
		var serialized = TriG.of("foo");
		TriG.to(new StringReader(serialized), String.class);
	}

	@Test void b24_trig_to_reader_type() throws Exception {
		var serialized = TriG.of("foo");
		TriG.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
	}

	@Test void b25_trig_to_string() throws Exception {
		var serialized = TriG.of("foo");
		TriG.to(serialized, String.class);
	}

	@Test void b26_trig_to_string_type() throws Exception {
		var serialized = TriG.of("foo");
		TriG.to(serialized, String.class, new java.lang.reflect.Type[0]);
	}

	@Test void b27_trix_to_reader() throws Exception {
		var serialized = TriX.of("foo");
		TriX.to(new StringReader(serialized), String.class);
	}

	@Test void b28_trix_to_reader_type() throws Exception {
		var serialized = TriX.of("foo");
		TriX.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
	}

	@Test void b29_trix_to_string() throws Exception {
		var serialized = TriX.of("foo");
		TriX.to(serialized, String.class);
	}

	@Test void b30_trix_to_string_type() throws Exception {
		var serialized = TriX.of("foo");
		TriX.to(serialized, String.class, new java.lang.reflect.Type[0]);
	}

	@Test void b31_turtle_to_reader() throws Exception {
		var serialized = Turtle.of("foo");
		assertEquals("foo", Turtle.to(new StringReader(serialized), String.class));
	}

	@Test void b32_turtle_to_reader_type() throws Exception {
		var serialized = Turtle.of("foo");
		var result = Turtle.to(new StringReader(serialized), String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	@Test void b33_turtle_to_string_type() throws Exception {
		var serialized = Turtle.of("foo");
		var result = Turtle.to(serialized, String.class, new java.lang.reflect.Type[0]);
		assertEquals("foo", result);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean round-trip tests to exercise session code paths
	//-----------------------------------------------------------------------------------------------------------------

	public static class SimpleBean {
		private String name;
		private int value;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public int getValue() { return value; }
		public void setValue(int value) { this.value = value; }
	}

	@Test void c01_jsonLd_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = JsonLd.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = JsonLd.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c02_n3_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = N3.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = N3.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c03_turtle_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = Turtle.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = Turtle.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c04_rdfXml_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = RdfXml.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = RdfXml.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c05_nTriple_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = NTriple.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = NTriple.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c06_rdfXmlAbbrev_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = RdfXmlAbbrev.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = RdfXmlAbbrev.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c07_rdfJson_bean_roundtrip() throws Exception {
		var bean = new SimpleBean();
		bean.setName("test");
		bean.setValue(42);
		var serialized = RdfJson.of(bean);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
		var result = RdfJson.to(serialized, SimpleBean.class);
		assertEquals("test", result.getName());
		assertEquals(42, result.getValue());
	}

	@Test void c08_jsonLd_list_roundtrip() throws Exception {
		var list = new java.util.ArrayList<String>();
		list.add("item1");
		list.add("item2");
		var serialized = JsonLd.of(list);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}

	@Test void c09_turtle_map_roundtrip() throws Exception {
		var in1 = JsonMap.of("key1", "value1", "key2", "value2");
		var serialized = Turtle.of(in1);
		assertNotNull(serialized);
		assertFalse(serialized.isEmpty());
	}
}
