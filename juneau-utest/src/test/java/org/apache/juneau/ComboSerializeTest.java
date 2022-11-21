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

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings({"rawtypes"})
public abstract class ComboSerializeTest {

	private final ComboInput comboInput;

	private Map<Serializer,Serializer> serializerMap = new IdentityHashMap<>();

	public ComboSerializeTest(ComboInput<?> comboInput) {
		this.comboInput = comboInput;
	}

	private Serializer getSerializer(Serializer s) throws Exception {
		Serializer s2 = serializerMap.get(s);
		if (s2 == null) {
			s2 = applySettings(s);
			serializerMap.put(s, s2);
		}
		return s2;
	}

	private boolean isSkipped(String testName, String expected) throws Exception {
		if ("SKIP".equals(expected) || comboInput.isTestSkipped(testName)) {
			System.err.println(getClass().getName() + ": " + comboInput.label + "/" + testName + " skipped.");  // NOT DEBUG
			return true;
		}
		return false;
	}

	private void testSerialize(String testName, Serializer s, String expected) throws Exception {
		try {
			if (isSkipped(testName, expected))
				return;

			s = getSerializer(s);

			String r = s.serializeToString(comboInput.in.get());

			// Specifying "xxx" in the expected results will spit out what we should populate the field with.
			if (expected.equals("xxx")) {
				System.out.println(comboInput.label + "/" + testName + "=\n" + r.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")); // NOT DEBUG
				System.out.println(r);
				if (s instanceof MsgPackSerializer) {
					System.out.println("decoded=["+new String(StringUtils.fromHex(r))+"]");
				}
			}

			assertString(r).setMsg("{0}/{1} parse-normal failed: <<<MSG>>>", comboInput.label, testName).is(expected);

		} catch (AssertionError e) {
			if (comboInput.exceptionMsg == null)
				throw e;
			assertThrowable(e).asMessages().isAny(contains(comboInput.exceptionMsg));
		} catch (Exception e) {
			if (comboInput.exceptionMsg == null) {
				e.printStackTrace();
				throw new AssertionError(comboInput.label + "/" + testName + " failed.  exception=" + e.getLocalizedMessage());
			}
			assertThrowable(e).asMessages().isAny(contains(comboInput.exceptionMsg));
		}
	}

	protected Serializer applySettings(Serializer s) throws Exception {
		return s;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJson = Json5Serializer.DEFAULT;

	@Test
	public void a11_serializeJson() throws Exception {
		testSerialize("serializeJson", sJson, comboInput.json);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJsonT = Json5Serializer.create().json5().typePropertyName("t").build();

	@Test
	public void a21_serializeJsonT() throws Exception {
		testSerialize("serializeJsonT", sJsonT, comboInput.jsonT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJsonR = Json5Serializer.DEFAULT_READABLE;

	@Test
	public void a31_serializeJsonR() throws Exception {
		testSerialize("serializeJsonR", sJsonR, comboInput.jsonR);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXml = XmlSerializer.DEFAULT_SQ;

	@Test
	public void b11_serializeXml() throws Exception {
		testSerialize("serializeXml", sXml, comboInput.xml);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXmlT = XmlSerializer.create().sq().typePropertyName("t").build();

	@Test
	public void b21_serializeXmlT() throws Exception {
		testSerialize("serializeXmlT", sXmlT, comboInput.xmlT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXmlR = XmlSerializer.DEFAULT_SQ_READABLE;

	@Test
	public void b31_serializeXmlR() throws Exception {
		testSerialize("serializeXmlR", sXmlR, comboInput.xmlR);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Namespaces
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXmlNs = XmlSerializer.DEFAULT_NS_SQ;

	@Test
	public void b41_serializeXmlNs() throws Exception {
		testSerialize("serializeXmlNs", sXmlNs, comboInput.xmlNs);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sHtml = HtmlSerializer.DEFAULT_SQ;

	@Test
	public void c11_serializeHtml() throws Exception {
		testSerialize("serializeHtml", sHtml, comboInput.html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sHtmlT = HtmlSerializer.create().sq().typePropertyName("t").build();

	@Test
	public void c21_serializeHtmlT() throws Exception {
		testSerialize("serializeHtmlT", sHtmlT, comboInput.htmlT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sHtmlR = HtmlSerializer.DEFAULT_SQ_READABLE;

	@Test
	public void c31_serializeHtmlR() throws Exception {
		testSerialize("serializeHtmlR", sHtmlR, comboInput.htmlR);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUon = UonSerializer.DEFAULT;

	@Test
	public void d11_serializeUon() throws Exception {
		testSerialize("serializeUon", sUon, comboInput.uon);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUonT = UonSerializer.create().typePropertyName("t").build();

	@Test
	public void d21_serializeUonT() throws Exception {
		testSerialize("serializeUonT", sUonT, comboInput.uonT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUonR = UonSerializer.DEFAULT_READABLE;

	@Test
	public void d31_serializeUonR() throws Exception {
		testSerialize("serializeUonR", sUonR, comboInput.uonR);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUrlEncoding = UrlEncodingSerializer.DEFAULT;

	@Test
	public void e11_serializeUrlEncoding() throws Exception {
		testSerialize("serializeUrlEncoding", sUrlEncoding, comboInput.urlEncoding);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingT = UrlEncodingSerializer.create().typePropertyName("t").build();

	@Test
	public void e21_serializeUrlEncodingT() throws Exception {
		testSerialize("serializeUrlEncodingT", sUrlEncodingT, comboInput.urlEncodingT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingR = UrlEncodingSerializer.DEFAULT_READABLE;

	@Test
	public void e31_serializeUrlEncodingR() throws Exception {
		testSerialize("serializeUrlEncodingR", sUrlEncodingR, comboInput.urlEncodingR);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack
	//-----------------------------------------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPack = MsgPackSerializer.DEFAULT;

	@Test
	public void f11_serializeMsgPack() throws Exception {
		testSerialize("serializeMsgPack", sMsgPack, comboInput.msgPack);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPackT = MsgPackSerializer.create().typePropertyName("t").build();

	@Test
	public void f21_serializeMsgPackT() throws Exception {
		testSerialize("serializeMsgPackT", sMsgPackT, comboInput.msgPackT);
	}
}
