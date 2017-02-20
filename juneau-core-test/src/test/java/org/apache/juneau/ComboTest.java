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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.juneau.html.HtmlParser;
import org.apache.juneau.html.HtmlSerializer;
import org.apache.juneau.jena.RdfParser;
import org.apache.juneau.jena.RdfSerializer;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.msgpack.MsgPackParser;
import org.apache.juneau.msgpack.MsgPackSerializer;
import org.apache.juneau.parser.InputStreamParser;
import org.apache.juneau.parser.Parser;
import org.apache.juneau.parser.ReaderParser;
import org.apache.juneau.serializer.OutputStreamSerializer;
import org.apache.juneau.serializer.Serializer;
import org.apache.juneau.serializer.WriterSerializer;
import org.apache.juneau.urlencoding.UonParser;
import org.apache.juneau.urlencoding.UonSerializer;
import org.apache.juneau.urlencoding.UrlEncodingParser;
import org.apache.juneau.urlencoding.UrlEncodingSerializer;
import org.apache.juneau.xml.XmlParser;
import org.apache.juneau.xml.XmlSerializer;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Superclass for tests that verify results against all supported content types. 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
	
	private final String 
		label, 
		oJson, oJsonT, oJsonR,
		oXml, oXmlT, oXmlR, oXmlNs,
		oHtml, oHtmlT, oHtmlR,
		oUon, oUonT, oUonR,
		oUrlEncoding, oUrlEncodingT, oUrlEncodingR,
		oMsgPack, oMsgPackT,
		oRdfXml, oRdfXmlT, oRdfXmlR;
	private final Object in;
	
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
	
	public ComboTest(
		String label, 
		Object in, 
		String oJson, String oJsonT, String oJsonR,
		String oXml, String oXmlT, String oXmlR, String oXmlNs,
		String oHtml, String oHtmlT, String oHtmlR,
		String oUon, String oUonT, String oUonR,
		String oUrlEncoding, String oUrlEncodingT, String oUrlEncodingR,
		String oMsgPack, String oMsgPackT,
		String oRdfXml, String oRdfXmlT, String oRdfXmlR
	) {
		this.label = label;
		this.in = in;
		this.oJson = oJson; this.oJsonT = oJsonT; this.oJsonR = oJsonR;
		this.oXml = oXml; this.oXmlT = oXmlT; this.oXmlR = oXmlR; this.oXmlNs = oXmlNs;
		this.oHtml = oHtml; this.oHtmlT = oHtmlT; this.oHtmlR = oHtmlR;
		this.oUon = oUon; this.oUonT = oUonT; this.oUonR = oUonR;
		this.oUrlEncoding = oUrlEncoding; this.oUrlEncodingT = oUrlEncodingT; this.oUrlEncodingR = oUrlEncodingR;
		this.oMsgPack = oMsgPack; this.oMsgPackT = oMsgPackT; 
		this.oRdfXml = oRdfXml; this.oRdfXmlT = oRdfXmlT; this.oRdfXmlR = oRdfXmlR;
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
				System.err.println(label + "/" + testName + " for "+s.getClass().getSimpleName()+" skipped.");
				return;
			}
			
			String r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(in) : ((OutputStreamSerializer)s).serializeToHex(in);
			
			// Can't control RdfSerializer output well, so manually remove namespace declarations
			// double-quotes with single-quotes, and spaces with tabs.
			// Also because RDF sucks really bad and can't be expected to produce consistent testable results,
			// we must also do an expensive sort-then-compare operation to verify the results.
			if (isRdf) 
				r = r.replaceAll("<rdf:RDF[^>]*>", "<rdf:RDF>").replace('"', '\'');
		
			// Specifying "xxx" in the expected results will spit out what we should populate the field with.
			if (expected.equals("xxx")) {
				System.out.println(label + "/" + testName + "=\n" + r.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")); // NOT DEBUG
				System.out.println(r);
			}
			
			if (isRdf)
				TestUtils.assertEqualsAfterSort(expected, r, "{0}/{1} parse-normal failed", label, testName);
			else
				TestUtils.assertEquals(expected, r, "{0}/{1} parse-normal failed", label, testName);
			
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + "/" + testName + " failed.  exception=" + e.getLocalizedMessage());
		}
	}
	
	private void testParse(String testName, Serializer s, Parser p, String expected) throws Exception {
		try {
			s = getSerializer(s);
			p = getParser(p);
			
			boolean isRdf = s instanceof RdfSerializer;

			if ((isRdf && SKIP_RDF_TESTS) || expected.isEmpty() || ! runTestsSet.contains(testName) ) {
				System.err.println(label + "/" + testName + " for "+s.getClass().getSimpleName()+" skipped.");
				return;
			}
			
			String r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(in) : ((OutputStreamSerializer)s).serializeToHex(in);
			Object o = p.parse(r, in == null ? Object.class : in.getClass());
			r = s.isWriterSerializer() ? ((WriterSerializer)s).serialize(o) : ((OutputStreamSerializer)s).serializeToHex(o);
			
			if (isRdf) 
				r = r.replaceAll("<rdf:RDF[^>]*>", "<rdf:RDF>").replace('"', '\'');
			
			if (isRdf)
				TestUtils.assertEqualsAfterSort(expected, r, "{0}/{1} parse-normal failed", label, testName);
			else
				TestUtils.assertEquals(expected, r, "{0}/{1} parse-normal failed", label, testName);

		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(label + "/" + testName + " failed.", e);
		}
	}
	
	private void testParseJsonEquivalency(String testName, OutputStreamSerializer s, InputStreamParser p, String expected) throws Exception {
		try {
			s = (OutputStreamSerializer)getSerializer(s);
			p = (InputStreamParser)getParser(p);
			WriterSerializer sJson = (WriterSerializer)getSerializer(this.sJson);

			String r = s.serializeToHex(in);
			Object o = p.parse(r, in == null ? Object.class : in.getClass());
			r = sJson.serialize(o);
			assertEquals(label + "/" + testName + " parse-normal failed on JSON equivalency", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(label + "/" + testName + " failed.", e);
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
	public void serializeJson() throws Exception {
		testSerialize("serializeJson", sJson, oJson);
	}
	
	@Test
	public void parseJson() throws Exception {
		testParse("parseJson", sJson, pJson, oJson);
	}
	
	//--------------------------------------------------------------------------------
	// JSON - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sJsonT = JsonSerializer.DEFAULT_LAX.clone().setBeanTypePropertyName("t");
	ReaderParser pJsonT = JsonParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeJsonT() throws Exception {
		testSerialize("serializeJsonT", sJsonT, oJsonT);
	}
	
	@Test
	public void parseJsonT() throws Exception {
		testParse("parseJsonT", sJsonT, pJsonT, oJsonT);
	}
	
	//--------------------------------------------------------------------------------
	// JSON - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sJsonR = JsonSerializer.DEFAULT_LAX_READABLE;
	ReaderParser pJsonR = JsonParser.DEFAULT;
	
	@Test
	public void serializeJsonR() throws Exception {
		testSerialize("serializeJsonR", sJsonR, oJsonR);
	}
	
	@Test
	public void parseJsonR() throws Exception {
		testParse("parseJsonR", sJsonR, pJsonR, oJsonR);
	}
	
	//--------------------------------------------------------------------------------
	// XML
	//--------------------------------------------------------------------------------
	WriterSerializer sXml = XmlSerializer.DEFAULT_SQ;
	ReaderParser pXml = XmlParser.DEFAULT;
	
	@Test
	public void serializeXml() throws Exception {
		testSerialize("serializeXml", sXml, oXml);
	}
	
	@Test
	public void parseXml() throws Exception {
		testParse("parseXml", sXml, pXml, oXml);
	}
	
	//--------------------------------------------------------------------------------
	// XML - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sXmlT = XmlSerializer.DEFAULT_SQ.clone().setBeanTypePropertyName("t");
	ReaderParser pXmlT = XmlParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeXmlT() throws Exception {
		testSerialize("serializeXmlT", sXmlT, oXmlT);
	}
	
	@Test
	public void parseXmlT() throws Exception {
		testParse("parseXmlT", sXmlT, pXmlT, oXmlT);
	}
	
	//--------------------------------------------------------------------------------
	// XML - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sXmlR = XmlSerializer.DEFAULT_SQ_READABLE;
	ReaderParser pXmlR = XmlParser.DEFAULT;
	
	@Test
	public void serializeXmlR() throws Exception {
		testSerialize("serializeXmlR", sXmlR, oXmlR);
	}
	
	@Test
	public void parseXmlR() throws Exception {
		testParse("parseXmlR", sXmlR, pXmlR, oXmlR);
	}
	
	//--------------------------------------------------------------------------------
	// XML - Namespaces
	//--------------------------------------------------------------------------------
	WriterSerializer sXmlNs = XmlSerializer.DEFAULT_NS_SQ;
	ReaderParser pXmlNs = XmlParser.DEFAULT;

	@Test
	public void serializeXmlNs() throws Exception {
		testSerialize("serializeXmlNs", sXmlNs, oXmlNs);
	}
	
	@Test
	public void parseXmlNs() throws Exception {
		testParse("parseXmlNs", sXmlNs, pXmlNs, oXmlNs);
	}
	
	//--------------------------------------------------------------------------------
	// HTML
	//--------------------------------------------------------------------------------
	WriterSerializer sHtml = HtmlSerializer.DEFAULT_SQ;
	ReaderParser pHtml = HtmlParser.DEFAULT;
	
	@Test
	public void serializeHtml() throws Exception {
		testSerialize("serializeHtml", sHtml, oHtml);
	}
	
	@Test
	public void parseHtml() throws Exception {
		testParse("parseHtml", sHtml, pHtml, oHtml);
	}
	
	//--------------------------------------------------------------------------------
	// HTML - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sHtmlT = HtmlSerializer.DEFAULT_SQ.clone().setBeanTypePropertyName("t");
	ReaderParser pHtmlT = HtmlParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeHtmlT() throws Exception {
		testSerialize("serializeHtmlT", sHtmlT, oHtmlT);
	}
	
	@Test
	public void parseHtmlT() throws Exception {
		testParse("parseHtmlT", sHtmlT, pHtmlT, oHtmlT);
	}
	
	//--------------------------------------------------------------------------------
	// HTML - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sHtmlR = HtmlSerializer.DEFAULT_SQ_READABLE;
	ReaderParser pHtmlR = HtmlParser.DEFAULT;
	
	@Test
	public void serializeHtmlR() throws Exception {
		testSerialize("serializeHtmlR", sHtmlR, oHtmlR);
	}
	
	@Test
	public void parseHtmlR() throws Exception {
		testParse("parseHtmlR", sHtmlR, pHtmlR, oHtmlR);
	}
	
	//--------------------------------------------------------------------------------
	// UON
	//--------------------------------------------------------------------------------
	WriterSerializer sUon = UonSerializer.DEFAULT;
	ReaderParser pUon = UonParser.DEFAULT;
	
	@Test
	public void serializeUon() throws Exception {
		testSerialize("serializeUon", sUon, oUon);
	}
	
	@Test
	public void parseUon() throws Exception {
		testParse("parseUon", sUon, pUon, oUon);
	}
	
	//--------------------------------------------------------------------------------
	// UON - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sUonT = UonSerializer.DEFAULT.clone().setBeanTypePropertyName("t");
	ReaderParser pUonT = UonParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeUonT() throws Exception {
		testSerialize("serializeUonT", sUonT, oUonT);
	}
	
	@Test
	public void parseUonT() throws Exception {
		testParse("parseUonT", sUonT, pUonT, oUonT);
	}
	
	//--------------------------------------------------------------------------------
	// UON - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sUonR = UonSerializer.DEFAULT_READABLE;
	ReaderParser pUonR = UonParser.DEFAULT;
	
	@Test
	public void serializeUonR() throws Exception {
		testSerialize("serializeUonR", sUonR, oUonR);
	}
	
	@Test
	public void parseUonR() throws Exception {
		testParse("parseUonR", sUonR, pUonR, oUonR);
	}
	
	//--------------------------------------------------------------------------------
	// UrlEncoding
	//--------------------------------------------------------------------------------
	WriterSerializer sUrlEncoding = UrlEncodingSerializer.DEFAULT;
	ReaderParser pUrlEncoding = UrlEncodingParser.DEFAULT;
	
	@Test
	public void serializeUrlEncoding() throws Exception {
		testSerialize("serializeUrlEncoding", sUrlEncoding, oUrlEncoding);
	}
	
	@Test
	public void parseUrlEncoding() throws Exception {
		testParse("parseUrlEncoding", sUrlEncoding, pUrlEncoding, oUrlEncoding);
	}
	
	//--------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingT = UrlEncodingSerializer.DEFAULT.clone().setBeanTypePropertyName("t");
	ReaderParser pUrlEncodingT = UrlEncodingParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeUrlEncodingT() throws Exception {
		testSerialize("serializeUrlEncodingT", sUrlEncodingT, oUrlEncodingT);
	}
	
	@Test
	public void parseUrlEncodingT() throws Exception {
		testParse("parseUrlEncodingT", sUrlEncodingT, pUrlEncodingT, oUrlEncodingT);
	}
	
	//--------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingR = UrlEncodingSerializer.DEFAULT_READABLE;
	ReaderParser pUrlEncodingR = UrlEncodingParser.DEFAULT;
	
	@Test
	public void serializeUrlEncodingR() throws Exception {
		testSerialize("serializeUrlEncodingR", sUrlEncodingR, oUrlEncodingR);
	}
	
	@Test
	public void parseUrlEncodingR() throws Exception {
		testParse("parseUrlEncodingR", sUrlEncodingR, pUrlEncodingR, oUrlEncodingR);
	}
	
	//--------------------------------------------------------------------------------
	// MsgPack
	//--------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPack = MsgPackSerializer.DEFAULT;
	InputStreamParser pMsgPack = MsgPackParser.DEFAULT;
	
	@Test
	public void serializeMsgPack() throws Exception {
		testSerialize("serializeMsgPack", sMsgPack, oMsgPack);
	}
	
	@Test
	public void parseMsgPack() throws Exception {
		testParse("parseMsgPack", sMsgPack, pMsgPack, oMsgPack);
	}
	
	@Test
	public void parseMsgPackJsonEquivalency() throws Exception {
		testParseJsonEquivalency("parseMsgPackJsonEquivalency", sMsgPack, pMsgPack, oJson);
	}
	
	//--------------------------------------------------------------------------------
	// MsgPack - 't' property
	//--------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPackT = MsgPackSerializer.DEFAULT.clone().setBeanTypePropertyName("t");
	InputStreamParser pMsgPackT = MsgPackParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeMsgPackT() throws Exception {
		testSerialize("serializeMsgPackT", sMsgPackT, oMsgPackT);
	}
	
	@Test
	public void parseMsgPackT() throws Exception {
		testParse("parseMsgPackT", sMsgPackT, pMsgPackT, oMsgPackT);
	}
	
	@Test
	public void parseMsgPackTJsonEquivalency() throws Exception {
		testParseJsonEquivalency("parseMsgPackTJsonEquivalency", sMsgPackT, pMsgPackT, oJson);
	}
	
	//--------------------------------------------------------------------------------
	// RdfXml
	//--------------------------------------------------------------------------------
	WriterSerializer sRdfXml = RdfSerializer.DEFAULT_XMLABBREV;
	ReaderParser pRdfXml = RdfParser.DEFAULT_XML;
	
	@Test
	public void serializeRdfXml() throws Exception {
		testSerialize("serializeRdfXml", sRdfXml, oRdfXml);
	}
	
	@Test
	public void parseRdfXml() throws Exception {
		testParse("parseRdfXml", sRdfXml, pRdfXml, oRdfXml);
	}
	
	//--------------------------------------------------------------------------------
	// RdfXml - 't' property
	//--------------------------------------------------------------------------------
	WriterSerializer sRdfXmlT = RdfSerializer.DEFAULT_XMLABBREV.clone().setBeanTypePropertyName("t");
	ReaderParser pRdfXmlT = RdfParser.DEFAULT_XML.clone().setBeanTypePropertyName("t");
	
	@Test
	public void serializeRdfXmlT() throws Exception {
		testSerialize("serializeRdfXmlT", sRdfXmlT, oRdfXmlT);
	}
	
	@Test
	public void parseRdfXmlT() throws Exception {
		testParse("parseRdfXmlT", sRdfXmlT, pRdfXmlT, oRdfXmlT);
	}
	
	//--------------------------------------------------------------------------------
	// RdfXml - Readable
	//--------------------------------------------------------------------------------
	WriterSerializer sRdfXmlR = RdfSerializer.DEFAULT_XMLABBREV.clone().setUseWhitespace(true);
	ReaderParser pRdfXmlR = RdfParser.DEFAULT_XML;
	
	@Test
	public void serializeRdfXmlR() throws Exception {
		testSerialize("serializeRdfXmlR", sRdfXmlR, oRdfXmlR);
	}
	
	@Test
	public void parseRdfXmlR() throws Exception {
		testParse("parseRdfXmlR", sRdfXmlR, pRdfXmlR, oRdfXmlR);
	}
}
