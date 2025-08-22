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
package org.apache.juneau;

import java.lang.reflect.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
public abstract class ComboRoundTripTest_Base extends SimpleTestBase {

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
	public void a11_serializeJson(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("json");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a12_parseJson(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("json");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a13_verifyJson(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("json");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a21_serializeJsonT(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("jsonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a22_parseJsonT(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("jsonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a23_verifyJsonT(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("jsonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a31_serializeJsonR(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("jsonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a32_parseJsonR(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("jsonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void a33_verifyJsonR(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("jsonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b11_serializeXml(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("xml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b12_parseXml(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("xml");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b13_verifyXml(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("xml");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b21_serializeXmlT(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("xmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b22_parseXmlT(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("xmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b23_verifyXmlT(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("xmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b31_serializeXmlR(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("xmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b32_parseXmlR(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("xmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b33_verifyXmlR(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("xmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Namespaces
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b41_serializeXmlNs(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("xmlNs");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b42_parseXmlNs(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("xmlNs");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void b43_verifyXmlNs(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("xmlNs");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c11_serializeHtml(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("html");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c12_parseHtml(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("html");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c13_verifyHtml(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("html");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c21_serializeHtmlT(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("htmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c22_parseHtmlT(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("htmlT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c23_verifyHtmlT(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("htmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c31_serializeHtmlR(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("htmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c32_parseHtmlR(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("htmlR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void c33_verifyHtmlR(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("htmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d11_serializeUon(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("uon");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d12_parseUon(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("uon");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d13_verifyUon(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("uon");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d21_serializeUonT(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("uonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d22_parseUonT(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("uonT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d23_verifyUonT(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("uonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d31_serializeUonR(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("uonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d32_parseUonR(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("uonR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void d33_verifyUonR(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("uonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e11_serializeUrlEncoding(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("urlEnc");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e12_parseUrlEncoding(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("urlEnc");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e13_verifyUrlEncoding(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("urlEnc");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e21_serializeUrlEncodingT(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("urlEncT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e22_parseUrlEncodingT(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("urlEncT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e23_verifyUrlEncodingT(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("urlEncT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e31_serializeUrlEncodingR(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("urlEncR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e32_parseUrlEncodingR(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("urlEncR");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void e33_verifyUrlEncodingR(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("urlEncR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f11_serializeMsgPack(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("msgPack");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f12_parseMsgPack(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("msgPack");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f13_parseMsgPackJsonEquivalency(ComboRoundTripTester<?> t) throws Exception {
		t.testParseJsonEquivalency("msgPack");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f14_verifyMsgPack(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("msgPack");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f21_serializeMsgPackT(ComboRoundTripTester<?> t) throws Exception {
		t.testSerialize("msgPackT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f22_parseMsgPackT(ComboRoundTripTester<?> t) throws Exception {
		t.testParse("msgPackT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f23_parseMsgPackTJsonEquivalency(ComboRoundTripTester<?> t) throws Exception {
		t.testParseJsonEquivalency("msgPackT");
	}

	@ParameterizedTest
	@MethodSource("testers")
	public void f24_verifyMsgPackT(ComboRoundTripTester<?> t) throws Exception {
		t.testParseVerify("msgPackT");
	}
}