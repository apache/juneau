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
public abstract class ComboSerializeTest_Base {

	protected static <T> ComboSerializeTester.Builder<T> tester(int index, String label, T bean) {
		return ComboSerializeTester.tester(index, label, bean);
	}
	/**
	 * Creates a ClassMeta for the given types.
	 */
	public static final Type getType(Type type, Type...args) {
		return BeanContext.DEFAULT_SESSION.getClassMeta(type, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a11_serializeJson(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("json");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a21_serializeJsonT(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("jsonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void a31_serializeJsonR(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("jsonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b11_serializeXml(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("xml");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b21_serializeXmlT(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("xmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b31_serializeXmlR(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("xmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Namespaces
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void b41_serializeXmlNs(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("xmlNs");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c11_serializeHtml(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("html");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c21_serializeHtmlT(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("htmlT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void c31_serializeHtmlR(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("htmlR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d11_serializeUon(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("uon");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d21_serializeUonT(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("uonT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void d31_serializeUonR(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("uonR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e11_serializeUrlEncoding(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("urlEnc");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e21_serializeUrlEncodingT(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("urlEncT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void e31_serializeUrlEncodingR(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("urlEncR");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f11_serializeMsgPack(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("msgPack");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack - 't' property
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	public void f21_serializeMsgPackT(ComboSerializeTester<?> t) throws Exception {
		t.testSerialize("msgPackT");
	}
}