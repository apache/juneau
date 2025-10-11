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
}