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

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
public abstract class ComboSerializeTest_Base extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// JSON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a11_serializeJson(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("json");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a21_serializeJsonT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("jsonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a31_serializeJsonR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("jsonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b11_serializeXml(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("xml");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b21_serializeXmlT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("xmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b31_serializeXmlR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("xmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Namespaces
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b41_serializeXmlNs(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("xmlNs");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c11_serializeHtml(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("html");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c21_serializeHtmlT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("htmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c31_serializeHtmlR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("htmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d11_serializeUon(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("uon");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d21_serializeUonT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("uonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d31_serializeUonR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("uonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e11_serializeUrlEncoding(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("urlEnc");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e21_serializeUrlEncodingT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("urlEncT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e31_serializeUrlEncodingR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("urlEncR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f11_serializeMsgPack(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("msgPack");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f21_serializeMsgPackT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("msgPackT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YAML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void g11_serializeYaml(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("yaml");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YAML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void g21_serializeYamlT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("yamlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YAML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void g31_serializeYamlR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("yamlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/XML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h21_serializeRdfXml(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfXml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h22_serializeRdfXmlT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfXmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h23_serializeRdfXmlR(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfXmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/THRIFT
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h31_serializeRdfThrift(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfThrift");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h32_serializeRdfThriftT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfThriftT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/PROTO
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h41_serializeRdfProto(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfProto");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h42_serializeRdfProtoT(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfProtoT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/XML-ABBREV, Turtle, N3, N-Triples, N-Quads, TriG, JSON-LD, RDF/JSON, TriX
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h51_serializeRdfXmlAbbrev(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfXmlAbbrev");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h52_serializeRdfTurtle(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfTurtle");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h53_serializeRdfN3(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfN3");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h54_serializeRdfNtriple(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfNtriple");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h55_serializeRdfNquads(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfNquads");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h56_serializeRdfTrig(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfTrig");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h57_serializeRdfJsonLd(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfJsonLd");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h58_serializeRdfJson(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfJson");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h59_serializeRdfTriX(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("rdfTriX");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CSV
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h11_serializeCsv(ComboSerialize_Tester<?> t) throws Exception {
		t.testSerialize("csv");
	}
}