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

import java.lang.reflect.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
public abstract class ComboRoundTripTest_Base extends TestBase {

	/**
	 * Creates a ClassMeta for the given types.
	 */
	protected static final Type getType(Type type, Type...args) {
		return BeanContext.DEFAULT_SESSION.getClassMeta(type, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a11_serializeJson(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("json");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a12_parseJson(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("json");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a13_verifyJson(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("json");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a21_serializeJsonT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("jsonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a22_parseJsonT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("jsonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a23_verifyJsonT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("jsonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a31_serializeJsonR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("jsonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a32_parseJsonR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("jsonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a33_verifyJsonR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("jsonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b11_serializeXml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("xml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b12_parseXml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("xml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b13_verifyXml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("xml");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b21_serializeXmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("xmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b22_parseXmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("xmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b23_verifyXmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("xmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b31_serializeXmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("xmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b32_parseXmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("xmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b33_verifyXmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("xmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Namespaces
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b41_serializeXmlNs(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("xmlNs");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b42_parseXmlNs(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("xmlNs");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b43_verifyXmlNs(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("xmlNs");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c11_serializeHtml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("html");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c12_parseHtml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("html");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c13_verifyHtml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("html");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c21_serializeHtmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("htmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c22_parseHtmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("htmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c23_verifyHtmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("htmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c31_serializeHtmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("htmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c32_parseHtmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("htmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c33_verifyHtmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("htmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d11_serializeUon(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("uon");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d12_parseUon(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("uon");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d13_verifyUon(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("uon");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d21_serializeUonT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("uonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d22_parseUonT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("uonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d23_verifyUonT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("uonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d31_serializeUonR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("uonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d32_parseUonR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("uonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d33_verifyUonR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("uonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e11_serializeUrlEncoding(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("urlEnc");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e12_parseUrlEncoding(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("urlEnc");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e13_verifyUrlEncoding(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("urlEnc");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e21_serializeUrlEncodingT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("urlEncT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e22_parseUrlEncodingT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("urlEncT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e23_verifyUrlEncodingT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("urlEncT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e31_serializeUrlEncodingR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("urlEncR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e32_parseUrlEncodingR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("urlEncR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e33_verifyUrlEncodingR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("urlEncR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f11_serializeMsgPack(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("msgPack");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f12_parseMsgPack(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("msgPack");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f13_parseMsgPackJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("msgPack");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f14_verifyMsgPack(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("msgPack");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f21_serializeMsgPackT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("msgPackT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f22_parseMsgPackT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("msgPackT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f23_parseMsgPackTJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("msgPackT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f24_verifyMsgPackT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("msgPackT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YAML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void g11_serializeYaml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("yaml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void g12_parseYaml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("yaml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void g13_verifyYaml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("yaml");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YAML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void g21_serializeYamlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("yamlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void g22_parseYamlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("yamlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void g23_verifyYamlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("yamlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YAML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void g31_serializeYamlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("yamlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void g32_parseYamlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("yamlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void g33_verifyYamlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("yamlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/XML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h21_serializeRdfXml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfXml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h22_parseRdfXml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfXml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h23_parseRdfXmlJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfXml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h24_verifyRdfXml(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfXml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h25_serializeRdfXmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfXmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h26_parseRdfXmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfXmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h27_verifyRdfXmlT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfXmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h28_serializeRdfXmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfXmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h29_parseRdfXmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfXmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h2a_verifyRdfXmlR(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfXmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/THRIFT
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h31_serializeRdfThrift(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfThrift");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h32_parseRdfThrift(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfThrift");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h33_parseRdfThriftJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfThrift");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h34_verifyRdfThrift(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfThrift");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h35_serializeRdfThriftT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfThriftT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h36_parseRdfThriftT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfThriftT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h37_verifyRdfThriftT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfThriftT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/PROTO
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h41_serializeRdfProto(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfProto");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h42_parseRdfProto(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfProto");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h43_parseRdfProtoJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfProto");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h44_verifyRdfProto(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfProto");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h45_serializeRdfProtoT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfProtoT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h46_parseRdfProtoT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfProtoT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h47_verifyRdfProtoT(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfProtoT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/XML-ABBREV
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h51_serializeRdfXmlAbbrev(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfXmlAbbrev");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h52_parseRdfXmlAbbrev(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfXmlAbbrev");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h53_parseRdfXmlAbbrevJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfXmlAbbrev");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h54_verifyRdfXmlAbbrev(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfXmlAbbrev");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/Turtle
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h61_serializeRdfTurtle(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfTurtle");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h62_parseRdfTurtle(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfTurtle");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h63_parseRdfTurtleJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfTurtle");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h64_verifyRdfTurtle(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfTurtle");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/N3
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h71_serializeRdfN3(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfN3");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h72_parseRdfN3(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfN3");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h73_parseRdfN3JsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfN3");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h74_verifyRdfN3(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfN3");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/N-Triples
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h81_serializeRdfNtriple(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfNtriple");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h82_parseRdfNtriple(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfNtriple");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h83_parseRdfNtripleJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfNtriple");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h84_verifyRdfNtriple(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfNtriple");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/N-Quads
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h91_serializeRdfNquads(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfNquads");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h92_parseRdfNquads(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfNquads");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h93_parseRdfNquadsJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfNquads");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h94_verifyRdfNquads(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfNquads");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/TriG
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void ha1_serializeRdfTrig(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfTrig");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void ha2_parseRdfTrig(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfTrig");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void ha3_parseRdfTrigJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfTrig");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void ha4_verifyRdfTrig(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfTrig");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/JSON-LD
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void hb1_serializeRdfJsonLd(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfJsonLd");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hb2_parseRdfJsonLd(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfJsonLd");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hb3_parseRdfJsonLdJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfJsonLd");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hb4_verifyRdfJsonLd(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfJsonLd");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/JSON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void hc1_serializeRdfJson(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfJson");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hc2_parseRdfJson(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfJson");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hc3_parseRdfJsonJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfJson");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hc4_verifyRdfJson(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfJson");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RDF/TriX
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void hd1_serializeRdfTriX(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("rdfTriX");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hd2_parseRdfTriX(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("rdfTriX");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hd3_parseRdfTriXJsonEquivalency(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseJsonEquivalency("rdfTriX");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void hd4_verifyRdfTriX(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("rdfTriX");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CSV
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void h11_serializeCsv(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testSerialize("csv");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h12_parseCsv(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParse("csv");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void h13_verifyCsv(ComboRoundTrip_Tester<?> t) throws Exception {
		t.testParseVerify("csv");
	}
}