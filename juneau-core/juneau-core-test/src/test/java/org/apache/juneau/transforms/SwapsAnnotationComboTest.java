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
package org.apache.juneau.transforms;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for the CalendarSwap class.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc"})
public class SwapsAnnotationComboTest extends ComboSerializeTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<TestMediaTypeLiterals>(
					"TestMediaTypeLiterals",
					TestMediaTypeLiterals.class,
					new TestMediaTypeLiterals(),
					/* Json */		"'JSON'",
					/* JsonT */		"'JSON'",
					/* JsonR */		"'JSON'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>HTML</string>",
					/* HtmlT */		"<string>HTML</string>",
					/* HtmlR */		"<string>HTML</string>",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"_value=URLENCODING",
					/* UrlEncT */	"_value=URLENCODING",
					/* UrlEncR */	"_value=URLENCODING",
					/* MsgPack */	"A74D53475041434B",
					/* MsgPackT */	"A74D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 1 */
				new ComboInput<TestMediaTypePatterns>(
					"TestMediaTypePatterns",
					TestMediaTypePatterns.class,
					new TestMediaTypePatterns(),
					/* Json */		"'JSON'",
					/* JsonT */		"'JSON'",
					/* JsonR */		"'JSON'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>HTML</string>",
					/* HtmlT */		"<string>HTML</string>",
					/* HtmlR */		"<string>HTML</string>",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"_value=URLENCODING",
					/* UrlEncT */	"_value=URLENCODING",
					/* UrlEncR */	"_value=URLENCODING",
					/* MsgPack */	"A74D53475041434B",
					/* MsgPackT */	"A74D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 2 */
				new ComboInput<TestMediaTypePatternsReversed>(
					"TestMediaTypePatternsReversed",
					TestMediaTypePatternsReversed.class,
					new TestMediaTypePatternsReversed(),
					/* Json */		"'JSON'",
					/* JsonT */		"'JSON'",
					/* JsonR */		"'JSON'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>HTML</string>",
					/* HtmlT */		"<string>HTML</string>",
					/* HtmlR */		"<string>HTML</string>",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"_value=URLENCODING",
					/* UrlEncT */	"_value=URLENCODING",
					/* UrlEncR */	"_value=URLENCODING",
					/* MsgPack */	"A74D53475041434B",
					/* MsgPackT */	"A74D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 3 */
				new ComboInput<TestMediaTypePatternsMulti>(
					"TestMediaTypePatternsMulti",
					TestMediaTypePatternsMulti.class,
					new TestMediaTypePatternsMulti(),
					/* Json */		"'JSON'",
					/* JsonT */		"'JSON'",
					/* JsonR */		"'JSON'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>HTML</string>",
					/* HtmlT */		"<string>HTML</string>",
					/* HtmlR */		"<string>HTML</string>",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"_value=URLENCODING",
					/* UrlEncT */	"_value=URLENCODING",
					/* UrlEncR */	"_value=URLENCODING",
					/* MsgPack */	"A74D53475041434B",
					/* MsgPackT */	"A74D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 4 */
				// In this case, "text/xml" should NOT match "text/xml+rdf".
				new ComboInput<TestMediaTypePatternsPartial1>(
					"TestMediaTypePatternsPartial1",
					TestMediaTypePatternsPartial1.class,
					new TestMediaTypePatternsPartial1(),
					/* Json */		"'JSON'",
					/* JsonT */		"'JSON'",
					/* JsonR */		"'JSON'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>HTML</string>",
					/* HtmlT */		"<string>HTML</string>",
					/* HtmlR */		"<string>HTML</string>",
					/* Uon */		"foo",
					/* UonT */		"foo",
					/* UonR */		"foo",
					/* UrlEnc */	"_value=foo",
					/* UrlEncT */	"_value=foo",
					/* UrlEncR */	"_value=foo",
					/* MsgPack */	"A3666F6F",
					/* MsgPackT */	"A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 5 */
				// In this case, "text/xml+rdf" should NOT match "text/xml".
				new ComboInput<TestMediaTypePatternsPartial2>(
					"TestMediaTypePatternsPartial2",
					TestMediaTypePatternsPartial2.class,
					new TestMediaTypePatternsPartial2(),
					/* Json */		"'foo'",
					/* JsonT */		"'foo'",
					/* JsonR */		"'foo'",
					/* Xml */		"<string>foo</string>",
					/* XmlT */		"<string>foo</string>",
					/* XmlR */		"<string>foo</string>\n",
					/* XmlNs */		"<string>foo</string>",
					/* Html */		"<string>foo</string>",
					/* HtmlT */		"<string>foo</string>",
					/* HtmlR */		"<string>foo</string>",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"_value=URLENCODING",
					/* UrlEncT */	"_value=URLENCODING",
					/* UrlEncR */	"_value=URLENCODING",
					/* MsgPack */	"A74D53475041434B",
					/* MsgPackT */	"A74D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 6 */
				// In this case, "text/xml+*" should match both the XML and RDF serializers.
				new ComboInput<TestMediaTypePatternsXmlPlus>(
					"TestMediaTypePatternsXmlPlus",
					TestMediaTypePatternsXmlPlus.class,
					new TestMediaTypePatternsXmlPlus(),
					/* Json */		"'foo'",
					/* JsonT */		"'foo'",
					/* JsonR */		"'foo'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>foo</string>",
					/* HtmlT */		"<string>foo</string>",
					/* HtmlR */		"<string>foo</string>",
					/* Uon */		"foo",
					/* UonT */		"foo",
					/* UonR */		"foo",
					/* UrlEnc */	"_value=foo",
					/* UrlEncT */	"_value=foo",
					/* UrlEncR */	"_value=foo",
					/* MsgPack */	"A3666F6F",
					/* MsgPackT */	"A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>XML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 7 */
				// In this case, "text/*+xml" should match both the XML and RDF serializers.
				new ComboInput<TestMediaTypePatternsXmlPlusReversed>(
					"TestMediaTypePatternsXmlPlusReversed",
					TestMediaTypePatternsXmlPlusReversed.class,
					new TestMediaTypePatternsXmlPlusReversed(),
					/* Json */		"'foo'",
					/* JsonT */		"'foo'",
					/* JsonR */		"'foo'",
					/* Xml */		"<string>XML</string>",
					/* XmlT */		"<string>XML</string>",
					/* XmlR */		"<string>XML</string>\n",
					/* XmlNs */		"<string>XML</string>",
					/* Html */		"<string>foo</string>",
					/* HtmlT */		"<string>foo</string>",
					/* HtmlR */		"<string>foo</string>",
					/* Uon */		"foo",
					/* UonT */		"foo",
					/* UonR */		"foo",
					/* UrlEnc */	"_value=foo",
					/* UrlEncT */	"_value=foo",
					/* UrlEncR */	"_value=foo",
					/* MsgPack */	"A3666F6F",
					/* MsgPackT */	"A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>XML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 8 */
				// In this case, "text/rdf+*" should match only the RDF serializer.
				new ComboInput<TestMediaTypePatternsRdfPlus>(
					"TestMediaTypePatternsRdfPlus",
					TestMediaTypePatternsRdfPlus.class,
					new TestMediaTypePatternsRdfPlus(),
					/* Json */		"'foo'",
					/* JsonT */		"'foo'",
					/* JsonR */		"'foo'",
					/* Xml */		"<string>foo</string>",
					/* XmlT */		"<string>foo</string>",
					/* XmlR */		"<string>foo</string>\n",
					/* XmlNs */		"<string>foo</string>",
					/* Html */		"<string>foo</string>",
					/* HtmlT */		"<string>foo</string>",
					/* HtmlR */		"<string>foo</string>",
					/* Uon */		"foo",
					/* UonT */		"foo",
					/* UonR */		"foo",
					/* UrlEnc */	"_value=foo",
					/* UrlEncT */	"_value=foo",
					/* UrlEncR */	"_value=foo",
					/* MsgPack */	"A3666F6F",
					/* MsgPackT */	"A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>XML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
//			{ 	/* 0 */
//				new ComboInput<byte[]>(
//					"ByteArray1d",
//					byte[].class,
//					new byte[] {1,2,3},
//					/* Json */		"xxx",
//					/* JsonT */		"xxx",
//					/* JsonR */		"xxx",
//					/* Xml */		"xxx",
//					/* XmlT */		"xxx",
//					/* XmlR */		"xxx",
//					/* XmlNs */		"xxx",
//					/* Html */		"xxx",
//					/* HtmlT */		"xxx",
//					/* HtmlR */		"xxx",
//					/* Uon */		"xxx",
//					/* UonT */		"xxx",
//					/* UonR */		"xxx",
//					/* UrlEnc */	"xxx",
//					/* UrlEncT */	"xxx",
//					/* UrlEncR */	"xxx",
//					/* MsgPack */	"xxx",
//					/* MsgPackT */	"xxx",
//					/* RdfXml */	"xxx",
//					/* RdfXmlT */	"xxx",
//					/* RdfXmlR */	"xxx"
//				)
//			},
		});
	}

	public SwapsAnnotationComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().pojoSwaps(ByteArrayBase64Swap.class).trimNullProperties(false).build();
	}

	@Swaps(
		{
			@Swap(value=SwapJson.class, mediaTypes={"application/json"}),
			@Swap(value=SwapXml.class, mediaTypes={"text/xml"}),
			@Swap(value=SwapHtml.class, mediaTypes={"text/html"}),
			@Swap(value=SwapUon.class, mediaTypes={"text/uon"}),
			@Swap(value=SwapUrlEncoding.class, mediaTypes={"application/x-www-form-urlencoded"}),
			@Swap(value=SwapMsgPack.class, mediaTypes={"octal/msgpack"}),
			@Swap(value=SwapRdfXml.class, mediaTypes={"text/xml+rdf"}),
		}
	)
	public static class TestMediaTypeLiterals {}

	@Swaps(
		{
			@Swap(value=SwapJson.class, mediaTypes={"*/json"}),
			@Swap(value=SwapXml.class, mediaTypes={"*/xml"}),
			@Swap(value=SwapHtml.class, mediaTypes={"*/html"}),
			@Swap(value=SwapUon.class, mediaTypes={"*/uon"}),
			@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/x-www-form-urlencoded"}),
			@Swap(value=SwapMsgPack.class, mediaTypes={"*/msgpack"}),
			@Swap(value=SwapRdfXml.class, mediaTypes={"*/xml+rdf"}),
		}
	)
	public static class TestMediaTypePatterns {}

	@Swaps(
		{
			@Swap(value=SwapRdfXml.class, mediaTypes={"*/xml+rdf"}),
			@Swap(value=SwapMsgPack.class, mediaTypes={"*/msgpack"}),
			@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/x-www-form-urlencoded"}),
			@Swap(value=SwapUon.class, mediaTypes={"*/uon"}),
			@Swap(value=SwapHtml.class, mediaTypes={"*/html"}),
			@Swap(value=SwapXml.class, mediaTypes={"*/xml"}),
			@Swap(value=SwapJson.class, mediaTypes={"*/json"}),
		}
	)
	public static class TestMediaTypePatternsReversed {}

	@Swaps(
		{
			@Swap(value=SwapJson.class, mediaTypes={"*/foo","*/json","*/bar"}),
			@Swap(value=SwapXml.class, mediaTypes={"*/foo","*/xml","*/bar"}),
			@Swap(value=SwapHtml.class, mediaTypes={"*/foo","*/html","*/bar"}),
			@Swap(value=SwapUon.class, mediaTypes={"*/foo","*/uon","*/bar"}),
			@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/foo","*/x-www-form-urlencoded","*/bar"}),
			@Swap(value=SwapMsgPack.class, mediaTypes={"*/foo","*/msgpack","*/bar"}),
			@Swap(value=SwapRdfXml.class, mediaTypes={"*/foo","*/xml+rdf","*/bar"}),
		}
	)
	public static class TestMediaTypePatternsMulti {}

	@Swaps(
		{
			@Swap(value=SwapJson.class, mediaTypes={"*/foo","*/json","*/bar"}),
			@Swap(value=SwapXml.class, mediaTypes={"*/foo","*/xml","*/bar"}),
			@Swap(value=SwapHtml.class, mediaTypes={"*/foo","*/html","*/bar"}),
		}
	)
	public static class TestMediaTypePatternsPartial1 {
		public String toString() {
			return "foo";
		}
	}

	@Swaps(
		{
			@Swap(value=SwapUon.class, mediaTypes={"*/foo","*/uon","*/bar"}),
			@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/foo","*/x-www-form-urlencoded","*/bar"}),
			@Swap(value=SwapMsgPack.class, mediaTypes={"*/foo","*/msgpack","*/bar"}),
			@Swap(value=SwapRdfXml.class, mediaTypes={"*/foo","*/xml+rdf","*/bar"}),
		}
	)
	public static class TestMediaTypePatternsPartial2 {
		public String toString() {
			return "foo";
		}
	}
	
	@Swaps(
		{
			@Swap(value=SwapXml.class, mediaTypes={"text/xml+*"}),
		}
	)
	public static class TestMediaTypePatternsXmlPlus {
		public String toString() {
			return "foo";
		}
	}

	@Swaps(
		{
			@Swap(value=SwapXml.class, mediaTypes={"text/*+xml"}),
		}
	)
	public static class TestMediaTypePatternsXmlPlusReversed {
		public String toString() {
			return "foo";
		}
	}

	@Swaps(
		{
			@Swap(value=SwapXml.class, mediaTypes={"text/rdf+*"}),
		}
	)
	public static class TestMediaTypePatternsRdfPlus {
		public String toString() {
			return "foo";
		}
	}

	public static class SwapJson extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "JSON";
		}
	}
	public static class SwapXml extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "XML";
		}
	}
	public static class SwapHtml extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "HTML";
		}
	}
	public static class SwapUon extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "UON";
		}
	}
	public static class SwapUrlEncoding extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "URLENCODING";
		}
	}
	public static class SwapMsgPack extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "MSGPACK";
		}
	}
	public static class SwapRdfXml extends PojoSwap {
		public Object swap(BeanSession session, Object o) throws Exception {
			return "RDFXML";
		}
	}
}
