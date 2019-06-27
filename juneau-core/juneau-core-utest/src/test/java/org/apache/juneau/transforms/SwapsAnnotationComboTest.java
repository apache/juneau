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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests Swap annotation.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class SwapsAnnotationComboTest extends ComboSerializeTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
				new ComboInput<>(
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
			{ 	/* 9 */
				new ComboInput<>(
					"TestTemplate",
					TestTemplate.class,
					new TestTemplate(),
					/* Json */		"foo",
					/* JsonT */		"foo",
					/* JsonR */		"foo",
					/* Xml */		"foo",
					/* XmlT */		"foo",
					/* XmlR */		"foo\n",
					/* XmlNs */		"foo",
					/* Html */		"foo",
					/* HtmlT */		"foo",
					/* HtmlR */		"foo",
					/* Uon */		"foo",
					/* UonT */		"foo",
					/* UonR */		"foo",
					/* UrlEnc */	"foo",
					/* UrlEncT */	"foo",
					/* UrlEncR */	"foo",
					/* MsgPack */	"666F6F",
					/* MsgPackT */	"666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 10 */
				new ComboInput<>(
					"TestTemplates",
					TestTemplates.class,
					new TestTemplates(),
					/* Json */		"JSON",
					/* JsonT */		"JSON",
					/* JsonR */		"JSON",
					/* Xml */		"XML",
					/* XmlT */		"XML",
					/* XmlR */		"XML\n",
					/* XmlNs */		"XML",
					/* Html */		"HTML",
					/* HtmlT */		"HTML",
					/* HtmlR */		"HTML",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"URLENCODING",
					/* UrlEncT */	"URLENCODING",
					/* UrlEncR */	"URLENCODING",
					/* MsgPack */	"4D53475041434B",
					/* MsgPackT */	"4D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 11 */
				new ComboInput<>(
					"TestProgrammaticTemplates",
					TestProgrammaticTemplates.class,
					new TestProgrammaticTemplates(),
					/* Json */		"JSON",
					/* JsonT */		"JSON",
					/* JsonR */		"JSON",
					/* Xml */		"XML",
					/* XmlT */		"XML",
					/* XmlR */		"XML\n",
					/* XmlNs */		"XML",
					/* Html */		"HTML",
					/* HtmlT */		"HTML",
					/* HtmlR */		"HTML",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"URLENCODING",
					/* UrlEncT */	"URLENCODING",
					/* UrlEncR */	"URLENCODING",
					/* MsgPack */	"4D53475041434B",
					/* MsgPackT */	"4D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 12 */
				new ComboInput<>(
					"TestContextSwap",
					TestContextSwap.class,
					new TestContextSwap(),
					/* Json */		"TEMPLATE",
					/* JsonT */		"TEMPLATE",
					/* JsonR */		"TEMPLATE",
					/* Xml */		"TEMPLATE",
					/* XmlT */		"TEMPLATE",
					/* XmlR */		"TEMPLATE\n",
					/* XmlNs */		"TEMPLATE",
					/* Html */		"TEMPLATE",
					/* HtmlT */		"TEMPLATE",
					/* HtmlR */		"TEMPLATE",
					/* Uon */		"TEMPLATE",
					/* UonT */		"TEMPLATE",
					/* UonR */		"TEMPLATE",
					/* UrlEnc */	"TEMPLATE",
					/* UrlEncT */	"TEMPLATE",
					/* UrlEncR */	"TEMPLATE",
					/* MsgPack */	"54454D504C415445",
					/* MsgPackT */	"54454D504C415445",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>TEMPLATE</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>TEMPLATE</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>TEMPLATE</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 13 */
				new ComboInput<>(
					"TestContextSwaps",
					TestContextSwaps.class,
					new TestContextSwaps(),
					/* Json */		"JSON",
					/* JsonT */		"JSON",
					/* JsonR */		"JSON",
					/* Xml */		"XML",
					/* XmlT */		"XML",
					/* XmlR */		"XML\n",
					/* XmlNs */		"XML",
					/* Html */		"HTML",
					/* HtmlT */		"HTML",
					/* HtmlR */		"HTML",
					/* Uon */		"UON",
					/* UonT */		"UON",
					/* UonR */		"UON",
					/* UrlEnc */	"URLENCODING",
					/* UrlEncT */	"URLENCODING",
					/* UrlEncR */	"URLENCODING",
					/* MsgPack */	"4D53475041434B",
					/* MsgPackT */	"4D53475041434B",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 14 */
				new ComboInput<>(
					"BeanA",
					BeanA.class,
					new BeanA(),
					/* Json */		"SWAPPED",
					/* JsonT */		"SWAPPED",
					/* JsonR */		"SWAPPED",
					/* Xml */		"SWAPPED",
					/* XmlT */		"SWAPPED",
					/* XmlR */		"SWAPPED\n",
					/* XmlNs */		"SWAPPED",
					/* Html */		"SWAPPED",
					/* HtmlT */		"SWAPPED",
					/* HtmlR */		"SWAPPED",
					/* Uon */		"(f=1)",
					/* UonT */		"(f=1)",
					/* UonR */		"(\n\tf=1\n)",
					/* UrlEnc */	"f=1",
					/* UrlEncT */	"f=1",
					/* UrlEncR */	"f=1",
					/* MsgPack */	"81A16601",
					/* MsgPackT */	"81A16601",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
			{ 	/* 15 */
				new ComboInput<>(
					"BeanB",
					BeanB.class,
					new BeanB(),
					/* Json */		"{f:1}",
					/* JsonT */		"{f:1}",
					/* JsonR */		"{\n\tf: 1\n}",
					/* Xml */		"<object><f>1</f></object>",
					/* XmlT */		"<object><f>1</f></object>",
					/* XmlR */		"<object>\n\t<f>1</f>\n</object>\n",
					/* XmlNs */		"<object><f>1</f></object>",
					/* Html */		"<table><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td>1</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"SWAPPED",
					/* UonT */		"SWAPPED",
					/* UonR */		"SWAPPED",
					/* UrlEnc */	"SWAPPED",
					/* UrlEncT */	"SWAPPED",
					/* UrlEncR */	"SWAPPED",
					/* MsgPack */	"53574150504544",
					/* MsgPackT */	"53574150504544",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>SWAPPED</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>SWAPPED</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>SWAPPED</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
			},
		});
	}

	public SwapsAnnotationComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().pojoSwaps(
				ContextSwap.class,
				ContextSwapJson.class,
				ContextSwapXml.class,
				ContextSwapHtml.class,
				ContextSwapUon.class,
				ContextSwapUrlEncoding.class,
				ContextSwapMsgPack.class,
				ContextSwapRdfXml.class
			).build();
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
		@Override
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
		@Override
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
		@Override
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
		@Override
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
		@Override
		public String toString() {
			return "foo";
		}
	}

	public static class SwapJson extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "JSON";
		}
	}
	public static class SwapXml extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "XML";
		}
	}
	public static class SwapHtml extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "HTML";
		}
	}
	public static class SwapUon extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "UON";
		}
	}
	public static class SwapUrlEncoding extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "URLENCODING";
		}
	}
	public static class SwapMsgPack extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "MSGPACK";
		}
	}
	public static class SwapRdfXml extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "RDFXML";
		}
	}

	@Swap(impl=TemplateSwap.class,template="foo")
	public static class TestTemplate {}

	@Swaps(
		{
			@Swap(value=TemplateSwap.class, mediaTypes={"*/json"}, template="JSON"),
			@Swap(value=TemplateSwap.class, mediaTypes={"*/xml"}, template="XML"),
			@Swap(value=TemplateSwap.class, mediaTypes={"*/html"}, template="HTML"),
			@Swap(value=TemplateSwap.class, mediaTypes={"*/uon"}, template="UON"),
			@Swap(value=TemplateSwap.class, mediaTypes={"*/x-www-form-urlencoded"}, template="URLENCODING"),
			@Swap(value=TemplateSwap.class, mediaTypes={"*/msgpack"}, template="MSGPACK"),
			@Swap(value=TemplateSwap.class, mediaTypes={"*/xml+rdf"}, template="RDFXML"),
		}
	)
	public static class TestTemplates {}


	public static class TemplateSwap extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o, String template) throws Exception {
			return new StringReader(template);
		}
	}

	@Swaps(
		{
			@Swap(TemplateSwapJson.class),
			@Swap(TemplateSwapXml.class),
			@Swap(TemplateSwapHtml.class),
			@Swap(TemplateSwapUon.class),
			@Swap(TemplateSwapUrlEncoding.class),
			@Swap(TemplateSwapMsgPack.class),
			@Swap(TemplateSwapRdfXml.class),
		}
	)
	public static class TestProgrammaticTemplates {}

	public static class TemplateSwapJson extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/json");
		}
		@Override
		public String withTemplate() {
			return "JSON";
		}
	}
	public static class TemplateSwapXml extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/xml");
		}
		@Override
		public String withTemplate() {
			return "XML";
		}
	}
	public static class TemplateSwapHtml extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/html");
		}
		@Override
		public String withTemplate() {
			return "HTML";
		}
	}
	public static class TemplateSwapUon extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/uon");
		}
		@Override
		public String withTemplate() {
			return "UON";
		}
	}
	public static class TemplateSwapUrlEncoding extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/x-www-form-urlencoded");
		}
		@Override
		public String withTemplate() {
			return "URLENCODING";
		}
	}
	public static class TemplateSwapMsgPack extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/msgpack");
		}
		@Override
		public String withTemplate() {
			return "MSGPACK";
		}
	}
	public static class TemplateSwapRdfXml extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/xml+rdf");
		}
		@Override
		public String withTemplate() {
			return "RDFXML";
		}
	}


	public static class TestContextSwap {}

	public static class ContextSwap extends PojoSwap<TestContextSwap,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwap o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public String withTemplate() {
			return "TEMPLATE";
		}
	}

	public static class TestContextSwaps {}

	public static class ContextSwapJson extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/json");
		}
		@Override
		public String withTemplate() {
			return "JSON";
		}
	}
	public static class ContextSwapXml extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/xml");
		}
		@Override
		public String withTemplate() {
			return "XML";
		}
	}
	public static class ContextSwapHtml extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/html");
		}
		@Override
		public String withTemplate() {
			return "HTML";
		}
	}
	public static class ContextSwapUon extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/uon");
		}
		@Override
		public String withTemplate() {
			return "UON";
		}
	}
	public static class ContextSwapUrlEncoding extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/x-www-form-urlencoded");
		}
		@Override
		public String withTemplate() {
			return "URLENCODING";
		}
	}
	public static class ContextSwapMsgPack extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/msgpack");
		}
		@Override
		public String withTemplate() {
			return "MSGPACK";
		}
	}
	public static class ContextSwapRdfXml extends PojoSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return new StringReader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/xml+rdf");
		}
		@Override
		public String withTemplate() {
			return "RDFXML";
		}
	}

	@Swaps(
		{
			@Swap(value=BeanSwap.class, mediaTypes={"*/json"}),
			@Swap(value=BeanSwap.class, mediaTypes={"*/xml"}),
			@Swap(value=BeanSwap.class, mediaTypes={"*/html"}),
		}
	)
	public static class BeanA {
		public int f = 1;
	}

	@Swaps(
		{
			@Swap(value=BeanSwap.class, mediaTypes={"*/uon"}),
			@Swap(value=BeanSwap.class, mediaTypes={"*/x-www-form-urlencoded"}),
			@Swap(value=BeanSwap.class, mediaTypes={"*/msgpack"}),
			@Swap(value=BeanSwap.class, mediaTypes={"*/xml+rdf"}),
		}
	)
	public static class BeanB {
		public int f = 1;
	}

	public static class BeanSwap extends PojoSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o, String template) throws Exception {
			return new StringReader("SWAPPED");
		}
	}
}
