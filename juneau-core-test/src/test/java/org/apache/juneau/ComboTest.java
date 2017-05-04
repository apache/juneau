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

import static org.apache.juneau.jena.Constants.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class ComboTest {

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
		"parseJson",
		"serializeJsonT",
		"parseJsonT",
		"serializeJsonR",
		"parseJsonR",
		"serializeXml",
		"parseXml",
		"serializeXmlT",
		"parseXmlT",
		"serializeXmlR",
		"parseXmlR",
		"serializeXmlNs",
		"parseXmlNs",
		"serializeHtml",
		"parseHtml",
		"serializeHtmlT",
		"parseHtmlT",
		"serializeHtmlR",
		"parseHtmlR",
		"serializeUon",
		"parseUon",
		"serializeUonT",
		"parseUonT",
		"serializeUonR",
		"parseUonR",
		"serializeUrlEncoding",
		"parseUrlEncoding",
		"serializeUrlEncodingT",
		"parseUrlEncodingT",
		"serializeUrlEncodingR",
		"parseUrlEncodingR",
		"serializeMsgPack",
		"parseMsgPack",
		"parseMsgPackJsonEquivalency",
		"serializeMsgPackT",
		"parseMsgPackT",
		"parseMsgPackTJsonEquivalency",
		"serializeRdfXml",
		"parseRdfXml",
		"serializeRdfXmlT",
		"parseRdfXmlT",
		"serializeRdfXmlR",
		"parseRdfXmlR",
	};

	private static final Set<String> runTestsSet = new HashSet<String>(Arrays.asList(runTests));
	
	private final boolean SKIP_RDF_TESTS = Boolean.getBoolean("skipRdfTests");

	private Map<Serializer,Serializer> serializerMap = new IdentityHashMap<Serializer,Serializer>();
	private Map<Parser,Parser> parserMap = new IdentityHashMap<Parser,Parser>();
	
	public ComboTest(ComboInput<?> comboInput) {
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
	
	private Parser getParser(Parser p) throws Exception {
		Parser p2 = parserMap.get(p);
		if (p2 == null) {
			p2 = applySettings(p);
			parserMap.put(p, p2);
		}
		return p2;
	}

	private void testSerialize(String testName, Serializer s, String expected) throws Exception {
		try {
			s = getSerializer(s);
			
			boolean isRdf = s instanceof RdfSerializer;

			if ((isRdf && SKIP_RDF_TESTS) || expected.isEmpty() || ! runTestsSet.contains(testName) ) {
				System.err.println(comboInput.label + "/" + testName + " for "+s.getClass().getSimpleName()+" skipped.");
				return;
			}
			
			String r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(comboInput.in) : ((OutputStreamSerializer)s).serializeToHex(comboInput.in);
			
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
			}
			
			if (isRdf)
				TestUtils.assertEqualsAfterSort(expected, r, "{0}/{1} parse-normal failed", comboInput.label, testName);
			else
				TestUtils.assertEquals(expected, r, "{0}/{1} parse-normal failed", comboInput.label, testName);
			
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError(comboInput.label + "/" + testName + " failed.  exception=" + e.getLocalizedMessage());
		}
	}
	
	private void testParse(String testName, Serializer s, Parser p, String expected) throws Exception {
		try {
			s = getSerializer(s);
			p = getParser(p);
			
			boolean isRdf = s instanceof RdfSerializer;

			if ((isRdf && SKIP_RDF_TESTS) || expected.isEmpty() || ! runTestsSet.contains(testName) ) {
				System.err.println(comboInput.label + "/" + testName + " for "+s.getClass().getSimpleName()+" skipped.");
				return;
			}
			
			String r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(comboInput.in) : ((OutputStreamSerializer)s).serializeToHex(comboInput.in);
			Object o = p.parse(r, comboInput.type);
			r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(o) : ((OutputStreamSerializer)s).serializeToHex(o);
			
			if (isRdf)
				r = r.replaceAll("<rdf:RDF[^>]*>", "<rdf:RDF>").replace('"', '\'');
			
			if (isRdf)
				TestUtils.assertEqualsAfterSort(expected, r, "{0}/{1} parse-normal failed", comboInput.label, testName);
			else
				TestUtils.assertEquals(expected, r, "{0}/{1} parse-normal failed", comboInput.label, testName);

		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(comboInput.label + "/" + testName + " failed.", e);
		}
	}
	
	private void testParseVerify(String testName, Serializer s, Parser p) throws Exception {
		try {
			s = getSerializer(s);
			p = getParser(p);
			
			String r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(comboInput.in) : ((OutputStreamSerializer)s).serializeToHex(comboInput.in);
			Object o = p.parse(r, comboInput.type);
			
			comboInput.verify(o);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(comboInput.label + "/" + testName + " failed.", e);
		}
	}
	
	
	private void testParseJsonEquivalency(String testName, OutputStreamSerializer s, InputStreamParser p, String expected) throws Exception {
		try {
			s = (OutputStreamSerializer)getSerializer(s);
			p = (InputStreamParser)getParser(p);
			WriterSerializer sJson = (WriterSerializer)getSerializer(this.sJson);

			String r = s.serializeToHex(comboInput.in);
			Object o = p.parse(r, comboInput.type);
			r = sJson.serialize(o);
			assertEquals(comboInput.label + "/" + testName + " parse-normal failed on JSON equivalency", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(comboInput.label + "/" + testName + " failed.", e);
		}
	}
	
	protected Serializer applySettings(Serializer s) throws Exception {
		return s;
	}
	
	protected Parser applySettings(Parser p) throws Exception {
		return p;
	}
	
	//--------------------------------------------------------------------------------
	// JSON
	//--------------------------------------------------------------------------------
	WriterSerializer sJson = JsonSerializer.DEFAULT_LAX;
	ReaderParser pJson = JsonParser.DEFAULT;
	
	@Test
	public void a11_serializeJson() throws Exception {
		testSerialize("serializeJson", sJson, comboInput.json);
	}
	
	@Test
	public void a12_parseJson() throws Exception {
		testParse("parseJson", sJson, pJson, comboInput.json);
	}
	
	@Test
	public void a13_verifyJson() throws Exception {
		testParseVerify("verifyJson", sJson, pJson);
	}

	//--------------------------------------------------------------------------------
	// JSON - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sJsonT = new JsonSerializerBuilder().simple().beanTypePropertyName("t").build();
	ReaderParser pJsonT = new JsonParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void a21_serializeJsonT() throws Exception {
		testSerialize("serializeJsonT", sJsonT, comboInput.jsonT);
	}
	
	@Test
	public void a22_parseJsonT() throws Exception {
		testParse("parseJsonT", sJsonT, pJsonT, comboInput.jsonT);
	}
	
	@Test
	public void a23_verifyJsonT() throws Exception {
		testParseVerify("verifyJsonT", sJsonT, pJsonT);
	}

	//--------------------------------------------------------------------------------
	// JSON - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sJsonR = JsonSerializer.DEFAULT_LAX_READABLE;
	ReaderParser pJsonR = JsonParser.DEFAULT;
	
	@Test
	public void a31_serializeJsonR() throws Exception {
		testSerialize("serializeJsonR", sJsonR, comboInput.jsonR);
	}
	
	@Test
	public void a32_parseJsonR() throws Exception {
		testParse("parseJsonR", sJsonR, pJsonR, comboInput.jsonR);
	}
	
	@Test
	public void a33_verifyJsonR() throws Exception {
		testParseVerify("verifyJsonR", sJsonR, pJsonR);
	}

	//--------------------------------------------------------------------------------
	// XML
	//--------------------------------------------------------------------------------
	WriterSerializer sXml = XmlSerializer.DEFAULT_SQ;
	ReaderParser pXml = XmlParser.DEFAULT;
	
	@Test
	public void b11_serializeXml() throws Exception {
		testSerialize("serializeXml", sXml, comboInput.xml);
	}
	
	@Test
	public void b12_parseXml() throws Exception {
		testParse("parseXml", sXml, pXml, comboInput.xml);
	}
	
	@Test
	public void b13_verifyXml() throws Exception {
		testParseVerify("verifyXml", sXml, pXml);
	}

	//--------------------------------------------------------------------------------
	// XML - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sXmlT = new XmlSerializerBuilder().sq().beanTypePropertyName("t").build();
	ReaderParser pXmlT = new XmlParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void b21_serializeXmlT() throws Exception {
		testSerialize("serializeXmlT", sXmlT, comboInput.xmlT);
	}
	
	@Test
	public void b22_parseXmlT() throws Exception {
		testParse("parseXmlT", sXmlT, pXmlT, comboInput.xmlT);
	}
	
	@Test
	public void b23_verifyXmlT() throws Exception {
		testParseVerify("parseXmlTVerify", sXmlT, pXmlT);
	}
	
	//--------------------------------------------------------------------------------
	// XML - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sXmlR = XmlSerializer.DEFAULT_SQ_READABLE;
	ReaderParser pXmlR = XmlParser.DEFAULT;
	
	@Test
	public void b31_serializeXmlR() throws Exception {
		testSerialize("serializeXmlR", sXmlR, comboInput.xmlR);
	}
	
	@Test
	public void b32_parseXmlR() throws Exception {
		testParse("parseXmlR", sXmlR, pXmlR, comboInput.xmlR);
	}
	
	@Test
	public void b33_verifyXmlR() throws Exception {
		testParseVerify("parseXmlRVerify", sXmlR, pXmlR);
	}

	//--------------------------------------------------------------------------------
	// XML - Namespaces
	//--------------------------------------------------------------------------------
	WriterSerializer sXmlNs = XmlSerializer.DEFAULT_NS_SQ;
	ReaderParser pXmlNs = XmlParser.DEFAULT;

	@Test
	public void b41_serializeXmlNs() throws Exception {
		testSerialize("serializeXmlNs", sXmlNs, comboInput.xmlNs);
	}
	
	@Test
	public void b42_parseXmlNs() throws Exception {
		testParse("parseXmlNs", sXmlNs, pXmlNs, comboInput.xmlNs);
	}
	
	@Test
	public void b43_verifyXmlNs() throws Exception {
		testParseVerify("verifyXmlNs", sXmlNs, pXmlNs);
	}

	//--------------------------------------------------------------------------------
	// HTML
	//--------------------------------------------------------------------------------
	WriterSerializer sHtml = HtmlSerializer.DEFAULT_SQ;
	ReaderParser pHtml = HtmlParser.DEFAULT;
	
	@Test
	public void c11_serializeHtml() throws Exception {
		testSerialize("serializeHtml", sHtml, comboInput.html);
	}
	
	@Test
	public void c12_parseHtml() throws Exception {
		testParse("parseHtml", sHtml, pHtml, comboInput.html);
	}
	
	@Test
	public void c13_verifyHtml() throws Exception {
		testParseVerify("verifyHtml", sHtml, pHtml);
	}
	
	//--------------------------------------------------------------------------------
	// HTML - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sHtmlT = new HtmlSerializerBuilder().sq().beanTypePropertyName("t").build();
	ReaderParser pHtmlT =  new HtmlParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void c21_serializeHtmlT() throws Exception {
		testSerialize("serializeHtmlT", sHtmlT, comboInput.htmlT);
	}
	
	@Test
	public void c22_parseHtmlT() throws Exception {
		testParse("parseHtmlT", sHtmlT, pHtmlT, comboInput.htmlT);
	}
	
	@Test
	public void c23_verifyHtmlT() throws Exception {
		testParseVerify("verifyHtmlT", sHtmlT, pHtmlT);
	}
	
	//--------------------------------------------------------------------------------
	// HTML - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sHtmlR = HtmlSerializer.DEFAULT_SQ_READABLE;
	ReaderParser pHtmlR = HtmlParser.DEFAULT;
	
	@Test
	public void c31_serializeHtmlR() throws Exception {
		testSerialize("serializeHtmlR", sHtmlR, comboInput.htmlR);
	}
	
	@Test
	public void c32_parseHtmlR() throws Exception {
		testParse("parseHtmlR", sHtmlR, pHtmlR, comboInput.htmlR);
	}
	
	@Test
	public void c33_verifyHtmlR() throws Exception {
		testParseVerify("verifyHtmlR", sHtmlR, pHtmlR);
	}
	
	//--------------------------------------------------------------------------------
	// UON
	//--------------------------------------------------------------------------------
	WriterSerializer sUon = UonSerializer.DEFAULT;
	ReaderParser pUon = UonParser.DEFAULT;
	
	@Test
	public void d11_serializeUon() throws Exception {
		testSerialize("serializeUon", sUon, comboInput.uon);
	}
	
	@Test
	public void d12_parseUon() throws Exception {
		testParse("parseUon", sUon, pUon, comboInput.uon);
	}
	
	@Test
	public void d13_verifyUon() throws Exception {
		testParseVerify("verifyUon", sUon, pUon);
	}
	
	//--------------------------------------------------------------------------------
	// UON - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sUonT = new UonSerializerBuilder().beanTypePropertyName("t").build();
	ReaderParser pUonT = new UonParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void d21_serializeUonT() throws Exception {
		testSerialize("serializeUonT", sUonT, comboInput.uonT);
	}
	
	@Test
	public void d22_parseUonT() throws Exception {
		testParse("parseUonT", sUonT, pUonT, comboInput.uonT);
	}
	
	@Test
	public void d23_verifyUonT() throws Exception {
		testParseVerify("verifyUonT", sUonT, pUonT);
	}
	
	//--------------------------------------------------------------------------------
	// UON - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sUonR = UonSerializer.DEFAULT_READABLE;
	ReaderParser pUonR = UonParser.DEFAULT;
	
	@Test
	public void d31_serializeUonR() throws Exception {
		testSerialize("serializeUonR", sUonR, comboInput.uonR);
	}
	
	@Test
	public void d32_parseUonR() throws Exception {
		testParse("parseUonR", sUonR, pUonR, comboInput.uonR);
	}
	
	@Test
	public void d33_verifyUonR() throws Exception {
		testParseVerify("verifyUonR", sUonR, pUonR);
	}
	
	//--------------------------------------------------------------------------------
	// UrlEncoding
	//--------------------------------------------------------------------------------
	WriterSerializer sUrlEncoding = UrlEncodingSerializer.DEFAULT;
	ReaderParser pUrlEncoding = UrlEncodingParser.DEFAULT;
	
	@Test
	public void e11_serializeUrlEncoding() throws Exception {
		testSerialize("serializeUrlEncoding", sUrlEncoding, comboInput.urlEncoding);
	}
	
	@Test
	public void e12_parseUrlEncoding() throws Exception {
		testParse("parseUrlEncoding", sUrlEncoding, pUrlEncoding, comboInput.urlEncoding);
	}
	
	@Test
	public void e13_verifyUrlEncoding() throws Exception {
		testParseVerify("verifyUrlEncoding", sUrlEncoding, pUrlEncoding);
	}
	
	//--------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingT = new UrlEncodingSerializerBuilder().beanTypePropertyName("t").build();
	ReaderParser pUrlEncodingT = new UrlEncodingParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void e21_serializeUrlEncodingT() throws Exception {
		testSerialize("serializeUrlEncodingT", sUrlEncodingT, comboInput.urlEncodingT);
	}
	
	@Test
	public void e22_parseUrlEncodingT() throws Exception {
		testParse("parseUrlEncodingT", sUrlEncodingT, pUrlEncodingT, comboInput.urlEncodingT);
	}
	
	@Test
	public void e23_verifyUrlEncodingT() throws Exception {
		testParseVerify("verifyUrlEncodingT", sUrlEncodingT, pUrlEncodingT);
	}
	
	//--------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingR = UrlEncodingSerializer.DEFAULT_READABLE;
	ReaderParser pUrlEncodingR = UrlEncodingParser.DEFAULT;
	
	@Test
	public void e31_serializeUrlEncodingR() throws Exception {
		testSerialize("serializeUrlEncodingR", sUrlEncodingR, comboInput.urlEncodingR);
	}
	
	@Test
	public void e32_parseUrlEncodingR() throws Exception {
		testParse("parseUrlEncodingR", sUrlEncodingR, pUrlEncodingR, comboInput.urlEncodingR);
	}
	
	@Test
	public void e33_verifyUrlEncodingR() throws Exception {
		testParseVerify("verifyUrlEncodingR", sUrlEncodingR, pUrlEncodingR);
	}
	
	//--------------------------------------------------------------------------------
	// MsgPack
	//--------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPack = MsgPackSerializer.DEFAULT;
	InputStreamParser pMsgPack = MsgPackParser.DEFAULT;
	
	@Test
	public void f11_serializeMsgPack() throws Exception {
		testSerialize("serializeMsgPack", sMsgPack, comboInput.msgPack);
	}
	
	@Test
	public void f12_parseMsgPack() throws Exception {
		testParse("parseMsgPack", sMsgPack, pMsgPack, comboInput.msgPack);
	}
	
	@Test
	public void f13_parseMsgPackJsonEquivalency() throws Exception {
		testParseJsonEquivalency("parseMsgPackJsonEquivalency", sMsgPack, pMsgPack, comboInput.json);
	}
	
	@Test
	public void f14_verifyMsgPack() throws Exception {
		testParseVerify("verifyMsgPack", sMsgPack, pMsgPack);
	}
	
	//--------------------------------------------------------------------------------
	// MsgPack - 't' property
	//--------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPackT = new MsgPackSerializerBuilder().beanTypePropertyName("t").build();
	InputStreamParser pMsgPackT = new MsgPackParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void f21_serializeMsgPackT() throws Exception {
		testSerialize("serializeMsgPackT", sMsgPackT, comboInput.msgPackT);
	}
	
	@Test
	public void f22_parseMsgPackT() throws Exception {
		testParse("parseMsgPackT", sMsgPackT, pMsgPackT, comboInput.msgPackT);
	}
	
	@Test
	public void f23_parseMsgPackTJsonEquivalency() throws Exception {
		testParseJsonEquivalency("parseMsgPackTJsonEquivalency", sMsgPackT, pMsgPackT, comboInput.json);
	}
	
	@Test
	public void f24_verifyMsgPackT() throws Exception {
		testParseVerify("verifyMsgPackT", sMsgPackT, pMsgPackT);
	}
	
	//--------------------------------------------------------------------------------
	// RdfXml
	//--------------------------------------------------------------------------------
	WriterSerializer sRdfXml = RdfSerializer.DEFAULT_XMLABBREV;
	ReaderParser pRdfXml = RdfParser.DEFAULT_XML;
	
	@Test
	public void g11_serializeRdfXml() throws Exception {
		testSerialize("serializeRdfXml", sRdfXml, comboInput.rdfXml);
	}
	
	@Test
	public void g12_parseRdfXml() throws Exception {
		testParse("parseRdfXml", sRdfXml, pRdfXml, comboInput.rdfXml);
	}
	
	@Test
	public void g13_verifyRdfXml() throws Exception {
		testParseVerify("verifyRdfXml", sRdfXml, pRdfXml);
	}
	
	//--------------------------------------------------------------------------------
	// RdfXml - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sRdfXmlT = new RdfSerializerBuilder().language(LANG_RDF_XML_ABBREV).beanTypePropertyName("t").build();
	ReaderParser pRdfXmlT = new RdfParserBuilder().beanTypePropertyName("t").build();
	
	@Test
	public void g21_serializeRdfXmlT() throws Exception {
		testSerialize("serializeRdfXmlT", sRdfXmlT, comboInput.rdfXmlT);
	}
	
	@Test
	public void g22_parseRdfXmlT() throws Exception {
		testParse("parseRdfXmlT", sRdfXmlT, pRdfXmlT, comboInput.rdfXmlT);
	}
	
	@Test
	public void g23_verifyRdfXmlT() throws Exception {
		testParseVerify("parseRdfXmlTVerify", sRdfXmlT, pRdfXmlT);
	}
	
	//--------------------------------------------------------------------------------
	// RdfXml - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sRdfXmlR = new RdfSerializerBuilder().language(LANG_RDF_XML_ABBREV).ws().build();
	ReaderParser pRdfXmlR = RdfParser.DEFAULT_XML;
	
	@Test
	public void g31_serializeRdfXmlR() throws Exception {
		testSerialize("serializeRdfXmlR", sRdfXmlR, comboInput.rdfXmlR);
	}
	
	@Test
	public void g32_parseRdfXmlR() throws Exception {
		testParse("parseRdfXmlR", sRdfXmlR, pRdfXmlR, comboInput.rdfXmlR);
	}
	
	@Test
	public void g33_verifyRdfXmlR() throws Exception {
		testParseVerify("Verify", sRdfXmlR, pRdfXmlR);
	}
}
