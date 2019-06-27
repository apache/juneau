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

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"rawtypes"})
public abstract class ComboSerializeTest {

	/* Parameter template */
//	{
//		"MyLabel",
//		myInput,
//		/* Json */		"xxx",
//		/* JsonT */		"xxx",
//		/* JsonR */		"xxx",
//		/* Xml */		"xxx",
//		/* XmlT */		"xxx",
//		/* XmlR */		"xxx",
//		/* XmlNs */		"xxx",
//		/* Html */		"xxx",
//		/* HtmlT */		"xxx",
//		/* HtmlR */		"xxx",
//		/* Uon */		"xxx",
//		/* UonT */		"xxx",
//		/* UonR */		"xxx",
//		/* UrlEnc */	"xxx",
//		/* UrlEncT */	"xxx",
//		/* UrlEncR */	"xxx",
//		/* MsgPack */	"xxx",
//		/* MsgPackT */	"xxx",
//		/* RdfXml */	"xxx",
//		/* RdfXmlT */	"xxx",
//		/* RdfXmlR */	"xxx",
//	},

	private final ComboInput comboInput;

	// These are the names of all the tests.
	// You can comment out the names here to skip them.
	private static final String[] runTests = {
		"serializeJson",
		"serializeJsonT",
		"serializeJsonR",
		"serializeXml",
		"serializeXmlT",
		"serializeXmlR",
		"serializeXmlNs",
		"serializeHtml",
		"serializeHtmlT",
		"serializeHtmlR",
		"serializeUon",
		"serializeUonT",
		"serializeUonR",
		"serializeUrlEncoding",
		"serializeUrlEncodingT",
		"serializeUrlEncodingR",
		"serializeMsgPack",
		"serializeMsgPackT",
		"serializeRdfXml",
		"serializeRdfXmlT",
		"serializeRdfXmlR",
	};

	private static final Set<String> runTestsSet = new HashSet<>(Arrays.asList(runTests));

	private final boolean SKIP_RDF_TESTS = Boolean.getBoolean("skipRdfTests");

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

	private void testSerialize(String testName, Serializer s, String expected) throws Exception {
		try {
			s = getSerializer(s);

			boolean isRdf = s instanceof RdfSerializer;

			if ((isRdf && SKIP_RDF_TESTS) || expected.equals("SKIP") || ! runTestsSet.contains(testName) ) {
				System.err.println(comboInput.label + "/" + testName + " for "+s.getClass().getSimpleName()+" skipped.");  // NOT DEBUG
				return;
			}

			String r = s.serializeToString(comboInput.getInput());

			// Can't control RdfSerializer output well, so manually remove namespace declarations
			// double-quotes with single-quotes, and spaces with tabs.
			// Also because RDF sucks really bad and can't be expected to produce consistent testable results,
			// we must also do an expensive sort-then-compare operation to verify the results.
			if (isRdf)
				r = r.replaceAll("<rdf:RDF[^>]*>", "<rdf:RDF>").replace('"', '\'');

			// Specifying "xxx" in the expected results will spit out what we should populate the field with.
			if (expected.equals("xxx")) {
				System.out.println(comboInput.label + "/" + testName + "=\n" + r.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")); // NOT DEBUG
				System.out.println(r);
				if (s instanceof MsgPackSerializer) {
					System.out.println("decoded=["+new String(StringUtils.fromHex(r))+"]");
				}
			}

			if (isRdf)
				TestUtils.assertEqualsAfterSort(expected, r, "{0}/{1} serialize-normal failed", comboInput.label, testName);
			else
				TestUtils.assertEquals(expected, r, "{0}/{1} serialize-normal failed", comboInput.label, testName);

		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError(comboInput.label + "/" + testName + " failed.  exception=" + e.getLocalizedMessage());
		}
	}

	protected Serializer applySettings(Serializer s) throws Exception {
		return s;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJson = SimpleJsonSerializer.DEFAULT;

	@Test
	public void a11_serializeJson() throws Exception {
		testSerialize("serializeJson", sJson, comboInput.json);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJsonT = JsonSerializer.create().ssq().beanTypePropertyName("t").build();

	@Test
	public void a21_serializeJsonT() throws Exception {
		testSerialize("serializeJsonT", sJsonT, comboInput.jsonT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJsonR = SimpleJsonSerializer.DEFAULT_READABLE;

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
	WriterSerializer sXmlT = XmlSerializer.create().sq().beanTypePropertyName("t").build();

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
	WriterSerializer sHtmlT = HtmlSerializer.create().sq().beanTypePropertyName("t").build();

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
	WriterSerializer sUonT = UonSerializer.create().beanTypePropertyName("t").build();

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
	WriterSerializer sUrlEncodingT = UrlEncodingSerializer.create().beanTypePropertyName("t").build();

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
	OutputStreamSerializer sMsgPackT = MsgPackSerializer.create().beanTypePropertyName("t").build();

	@Test
	public void f21_serializeMsgPackT() throws Exception {
		testSerialize("serializeMsgPackT", sMsgPackT, comboInput.msgPackT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RdfXml
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sRdfXml = RdfXmlAbbrevSerializer.DEFAULT;

	@Test
	public void g11_serializeRdfXml() throws Exception {
		testSerialize("serializeRdfXml", sRdfXml, comboInput.rdfXml);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RdfXml - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sRdfXmlT = RdfXmlAbbrevSerializer.create().beanTypePropertyName("t").build();

	@Test
	public void g21_serializeRdfXmlT() throws Exception {
		testSerialize("serializeRdfXmlT", sRdfXmlT, comboInput.rdfXmlT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RdfXml - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sRdfXmlR = RdfXmlAbbrevSerializer.create().ws().build();

	@Test
	public void g31_serializeRdfXmlR() throws Exception {
		testSerialize("serializeRdfXmlR", sRdfXmlR, comboInput.rdfXmlR);
	}
}
