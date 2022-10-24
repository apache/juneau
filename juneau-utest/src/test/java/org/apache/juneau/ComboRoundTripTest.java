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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;

/**
 * Superclass for tests that verify results against all supported content types.
 */
@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class ComboRoundTripTest {

	private static final BeanSession BEANSESSION = BeanContext.DEFAULT_SESSION;

	private final ComboInput comboInput;

	private Map<Serializer,Serializer> serializerMap = new IdentityHashMap<>();
	private Map<Parser,Parser> parserMap = new IdentityHashMap<>();

	public ComboRoundTripTest(ComboInput<?> comboInput) {
		this.comboInput = comboInput;
	}

	/**
	 * Creates a ClassMeta for the given types.
	 */
	public static final Type getType(Type type, Type...args) {
		return BEANSESSION.getClassMeta(type, args);
	}

	private Serializer getSerializer(Serializer s) throws Exception {
		Serializer s2 = serializerMap.get(s);
		if (s2 == null) {
			s2 = applySettings(s);
			if (! (comboInput.swaps.isEmpty() && comboInput.applies.isEmpty())) {
				Serializer.Builder b = s2.copy();
				b.swaps((Class<?>[])comboInput.swaps.toArray(new Class[0]));
				List<Tuple2<Class<?>,Consumer<?>>> pairs = comboInput.applies;
				for (Tuple2<Class<?>,Consumer<?>> pair : pairs) {
					if (pair.getA().equals(BeanContext.Builder.class))
						b.beanContext((Consumer<BeanContext.Builder>) pair.getB());
					else if (pair.getA().isInstance(b))
						b.apply(Serializer.Builder.class, (Consumer<Serializer.Builder>) pair.getB());
				}
				s2 = b.build();
			}
			serializerMap.put(s, s2);
		}
		return s2;
	}

	private Parser getParser(Parser p) throws Exception {
		Parser p2 = parserMap.get(p);
		if (p2 == null) {
			p2 = applySettings(p);
			if (! (comboInput.swaps.isEmpty() && comboInput.applies.isEmpty())) {
				Parser.Builder b = p2.copy();
				b.swaps((Class<?>[])comboInput.swaps.toArray(new Class[0]));
				List<Tuple2<Class<?>,Consumer<?>>> pairs = comboInput.applies;
				for (Tuple2<Class<?>,Consumer<?>> pair : pairs) {
					if (pair.getA().equals(BeanContext.Builder.class))
						b.beanContext((Consumer<BeanContext.Builder>) pair.getB());
					else if (pair.getA().isInstance(b))
						b.apply(Parser.Builder.class, (Consumer<Parser.Builder>) pair.getB());
				}
				p2 = b.build();
			}
			parserMap.put(p, p2);
		}
		return p2;
	}

	private boolean isSkipped(String testName, String expected) throws Exception {
		if ("SKIP".equals(expected) || comboInput.isTestSkipped(testName)) {
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
				System.out.println(getClass().getName() + ": " + comboInput.label + "/" + testName + "=\n" + r.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")); // NOT DEBUG
				System.out.println(r);
			}

			assertString(r).setMsg("{0}/{1} parse-normal failed.\n{msg}", comboInput.label, testName).is(expected);

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

	private void testParse(String testName, Serializer s, Parser p, String expected) throws Exception {
		try {
			if (isSkipped(testName, expected))
				return;

			s = getSerializer(s);
			p = getParser(p);

			String r = s.serializeToString(comboInput.in.get());
			Object o = p.parse(r, comboInput.type);
			o = comboInput.convert(o);
			r = s.serializeToString(o);

			assertString(r).setMsg("{0}/{1} parse-normal failed", comboInput.label, testName).is(expected);

		} catch (AssertionError e) {
			if (comboInput.exceptionMsg == null)
				throw e;
			assertThrowable(e).asMessages().isAny(contains(comboInput.exceptionMsg));
		} catch (Throwable e) {
			if (comboInput.exceptionMsg == null) {
				e.printStackTrace();
				throw new AssertionError(comboInput.label + "/" + testName + " failed.  exception=" + e.getLocalizedMessage());
			}
			assertThrowable(e).asMessages().isAny(contains(comboInput.exceptionMsg));
		}
	}

	private void testParseVerify(String testName, Serializer s, Parser p) throws Exception {
		try {
			if (isSkipped(testName, ""))
				return;

			s = getSerializer(s);
			p = getParser(p);

			String r = s.serializeToString(comboInput.in.get());
			Object o = p.parse(r, comboInput.type);

			comboInput.verify(o, testName);
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


	private void testParseJsonEquivalency(String testName, OutputStreamSerializer s, InputStreamParser p, String expected) throws Exception {
		try {
			s = (OutputStreamSerializer)getSerializer(s);
			p = (InputStreamParser)getParser(p);
			WriterSerializer sJson = (WriterSerializer)getSerializer(this.sJson);

			String r = s.serializeToString(comboInput.in.get());
			Object o = p.parse(r, comboInput.type);
			r = sJson.serialize(o);
			assertEquals(comboInput.label + "/" + testName + " parse-normal failed on JSON equivalency", expected, r);
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

	protected Parser applySettings(Parser p) throws Exception {
		return p;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJson = Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJsonT = JsonSerializer.create().json5().typePropertyName("t").addBeanTypes().addRootType().build();
	ReaderParser pJsonT = JsonParser.create().typePropertyName("t").build();

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

	//-----------------------------------------------------------------------------------------------------------------
	// JSON - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sJsonR = Json5Serializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// XML
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXml = XmlSerializer.DEFAULT_SQ.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// XML - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXmlT = XmlSerializer.create().sq().typePropertyName("t").addBeanTypes().addRootType().build();
	ReaderParser pXmlT = XmlParser.create().typePropertyName("t").build();

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

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXmlR = XmlSerializer.DEFAULT_SQ_READABLE.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// XML - Namespaces
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sXmlNs = XmlSerializer.DEFAULT_NS_SQ.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// HTML
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sHtml = HtmlSerializer.DEFAULT_SQ.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sHtmlT = HtmlSerializer.create().sq().typePropertyName("t").addBeanTypes().addRootType().build();
	ReaderParser pHtmlT =  HtmlParser.create().typePropertyName("t").build();

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

	//-----------------------------------------------------------------------------------------------------------------
	// HTML - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sHtmlR = HtmlSerializer.DEFAULT_SQ_READABLE.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// UON
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUon = UonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// UON - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUonT = UonSerializer.create().typePropertyName("t").addBeanTypes().addRootType().build();
	ReaderParser pUonT = UonParser.create().typePropertyName("t").build();

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

	//-----------------------------------------------------------------------------------------------------------------
	// UON - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUonR = UonSerializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUrlEncoding = UrlEncodingSerializer.DEFAULT.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingT = UrlEncodingSerializer.create().typePropertyName("t").addBeanTypes().addRootType().build();
	ReaderParser pUrlEncodingT = UrlEncodingParser.create().typePropertyName("t").build();

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

	//-----------------------------------------------------------------------------------------------------------------
	// UrlEncoding - Readable
	//-----------------------------------------------------------------------------------------------------------------
	WriterSerializer sUrlEncodingR = UrlEncodingSerializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack
	//-----------------------------------------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPack = MsgPackSerializer.create().addBeanTypes().addRootType().build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// MsgPack - 't' property
	//-----------------------------------------------------------------------------------------------------------------
	OutputStreamSerializer sMsgPackT = MsgPackSerializer.create().typePropertyName("t").addBeanTypes().addRootType().build();
	InputStreamParser pMsgPackT = MsgPackParser.create().typePropertyName("t").build();

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
}
