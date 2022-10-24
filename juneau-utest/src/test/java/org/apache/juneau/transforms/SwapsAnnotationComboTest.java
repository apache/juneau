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

import static org.apache.juneau.testutils.StreamUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
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
					new TestMediaTypeLiterals()
				)
				.json("'JSON'")
				.jsonT("'JSON'")
				.jsonR("'JSON'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>HTML</string>")
				.htmlT("<string>HTML</string>")
				.htmlR("<string>HTML</string>")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("_value=URLENCODING")
				.urlEncT("_value=URLENCODING")
				.urlEncR("_value=URLENCODING")
				.msgPack("A74D53475041434B")
				.msgPackT("A74D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 1 */
				new ComboInput<>(
					"TestMediaTypePatterns",
					TestMediaTypePatterns.class,
					new TestMediaTypePatterns()
				)
				.json("'JSON'")
				.jsonT("'JSON'")
				.jsonR("'JSON'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>HTML</string>")
				.htmlT("<string>HTML</string>")
				.htmlR("<string>HTML</string>")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("_value=URLENCODING")
				.urlEncT("_value=URLENCODING")
				.urlEncR("_value=URLENCODING")
				.msgPack("A74D53475041434B")
				.msgPackT("A74D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 2 */
				new ComboInput<>(
					"TestMediaTypePatternsReversed",
					TestMediaTypePatternsReversed.class,
					new TestMediaTypePatternsReversed()
				)
				.json("'JSON'")
				.jsonT("'JSON'")
				.jsonR("'JSON'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>HTML</string>")
				.htmlT("<string>HTML</string>")
				.htmlR("<string>HTML</string>")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("_value=URLENCODING")
				.urlEncT("_value=URLENCODING")
				.urlEncR("_value=URLENCODING")
				.msgPack("A74D53475041434B")
				.msgPackT("A74D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 3 */
				new ComboInput<>(
					"TestMediaTypePatternsMulti",
					TestMediaTypePatternsMulti.class,
					new TestMediaTypePatternsMulti()
				)
				.json("'JSON'")
				.jsonT("'JSON'")
				.jsonR("'JSON'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>HTML</string>")
				.htmlT("<string>HTML</string>")
				.htmlR("<string>HTML</string>")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("_value=URLENCODING")
				.urlEncT("_value=URLENCODING")
				.urlEncR("_value=URLENCODING")
				.msgPack("A74D53475041434B")
				.msgPackT("A74D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 4 */
				// In this case, "text/xml" should NOT match "text/xml+rdf".
				new ComboInput<>(
					"TestMediaTypePatternsPartial1",
					TestMediaTypePatternsPartial1.class,
					new TestMediaTypePatternsPartial1()
				)
				.json("'JSON'")
				.jsonT("'JSON'")
				.jsonR("'JSON'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>HTML</string>")
				.htmlT("<string>HTML</string>")
				.htmlR("<string>HTML</string>")
				.uon("foo")
				.uonT("foo")
				.uonR("foo")
				.urlEnc("_value=foo")
				.urlEncT("_value=foo")
				.urlEncR("_value=foo")
				.msgPack("A3666F6F")
				.msgPackT("A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 5 */
				// In this case, "text/xml+rdf" should NOT match "text/xml".
				new ComboInput<>(
					"TestMediaTypePatternsPartial2",
					TestMediaTypePatternsPartial2.class,
					new TestMediaTypePatternsPartial2()
				)
				.json("'foo'")
				.jsonT("'foo'")
				.jsonR("'foo'")
				.xml("<string>foo</string>")
				.xmlT("<string>foo</string>")
				.xmlR("<string>foo</string>\n")
				.xmlNs("<string>foo</string>")
				.html("<string>foo</string>")
				.htmlT("<string>foo</string>")
				.htmlR("<string>foo</string>")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("_value=URLENCODING")
				.urlEncT("_value=URLENCODING")
				.urlEncR("_value=URLENCODING")
				.msgPack("A74D53475041434B")
				.msgPackT("A74D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 6 */
				// XML and RDF serializers.
				new ComboInput<>(
					"TestMediaTypePatternsXmlPlus",
					TestMediaTypePatternsXmlPlus.class,
					new TestMediaTypePatternsXmlPlus()
				)
				.json("'foo'")
				.jsonT("'foo'")
				.jsonR("'foo'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>foo</string>")
				.htmlT("<string>foo</string>")
				.htmlR("<string>foo</string>")
				.uon("foo")
				.uonT("foo")
				.uonR("foo")
				.urlEnc("_value=foo")
				.urlEncT("_value=foo")
				.urlEncR("_value=foo")
				.msgPack("A3666F6F")
				.msgPackT("A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>XML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 7 */
				// XML and RDF serializers.
				new ComboInput<>(
					"TestMediaTypePatternsXmlPlusReversed",
					TestMediaTypePatternsXmlPlusReversed.class,
					new TestMediaTypePatternsXmlPlusReversed()
				)
				.json("'foo'")
				.jsonT("'foo'")
				.jsonR("'foo'")
				.xml("<string>XML</string>")
				.xmlT("<string>XML</string>")
				.xmlR("<string>XML</string>\n")
				.xmlNs("<string>XML</string>")
				.html("<string>foo</string>")
				.htmlT("<string>foo</string>")
				.htmlR("<string>foo</string>")
				.uon("foo")
				.uonT("foo")
				.uonR("foo")
				.urlEnc("_value=foo")
				.urlEncT("_value=foo")
				.urlEncR("_value=foo")
				.msgPack("A3666F6F")
				.msgPackT("A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>XML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 8 */
				// RDF serializer.
				new ComboInput<>(
					"TestMediaTypePatternsRdfPlus",
					TestMediaTypePatternsRdfPlus.class,
					new TestMediaTypePatternsRdfPlus()
				)
				.json("'foo'")
				.jsonT("'foo'")
				.jsonR("'foo'")
				.xml("<string>foo</string>")
				.xmlT("<string>foo</string>")
				.xmlR("<string>foo</string>\n")
				.xmlNs("<string>foo</string>")
				.html("<string>foo</string>")
				.htmlT("<string>foo</string>")
				.htmlR("<string>foo</string>")
				.uon("foo")
				.uonT("foo")
				.uonR("foo")
				.urlEnc("_value=foo")
				.urlEncT("_value=foo")
				.urlEncR("_value=foo")
				.msgPack("A3666F6F")
				.msgPackT("A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>XML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>XML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 9 */
				new ComboInput<>(
					"TestTemplate",
					TestTemplate.class,
					new TestTemplate()
				)
				.json("foo")
				.jsonT("foo")
				.jsonR("foo")
				.xml("foo")
				.xmlT("foo")
				.xmlR("foo\n")
				.xmlNs("foo")
				.html("foo")
				.htmlT("foo")
				.htmlR("foo")
				.uon("foo")
				.uonT("foo")
				.uonR("foo")
				.urlEnc("foo")
				.urlEncT("foo")
				.urlEncR("foo")
				.msgPack("666F6F")
				.msgPackT("666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>foo</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>foo</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 10 */
				new ComboInput<>(
					"TestTemplates",
					TestTemplates.class,
					new TestTemplates()
				)
				.json("JSON")
				.jsonT("JSON")
				.jsonR("JSON")
				.xml("XML")
				.xmlT("XML")
				.xmlR("XML\n")
				.xmlNs("XML")
				.html("HTML")
				.htmlT("HTML")
				.htmlR("HTML")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("URLENCODING")
				.urlEncT("URLENCODING")
				.urlEncR("URLENCODING")
				.msgPack("4D53475041434B")
				.msgPackT("4D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 11 */
				new ComboInput<>(
					"TestProgrammaticTemplates",
					TestProgrammaticTemplates.class,
					new TestProgrammaticTemplates()
				)
				.json("JSON")
				.jsonT("JSON")
				.jsonR("JSON")
				.xml("XML")
				.xmlT("XML")
				.xmlR("XML\n")
				.xmlNs("XML")
				.html("HTML")
				.htmlT("HTML")
				.htmlR("HTML")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("URLENCODING")
				.urlEncT("URLENCODING")
				.urlEncR("URLENCODING")
				.msgPack("4D53475041434B")
				.msgPackT("4D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 12 */
				new ComboInput<>(
					"TestContextSwap",
					TestContextSwap.class,
					new TestContextSwap()
				)
				.json("TEMPLATE")
				.jsonT("TEMPLATE")
				.jsonR("TEMPLATE")
				.xml("TEMPLATE")
				.xmlT("TEMPLATE")
				.xmlR("TEMPLATE\n")
				.xmlNs("TEMPLATE")
				.html("TEMPLATE")
				.htmlT("TEMPLATE")
				.htmlR("TEMPLATE")
				.uon("TEMPLATE")
				.uonT("TEMPLATE")
				.uonR("TEMPLATE")
				.urlEnc("TEMPLATE")
				.urlEncT("TEMPLATE")
				.urlEncR("TEMPLATE")
				.msgPack("54454D504C415445")
				.msgPackT("54454D504C415445")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>TEMPLATE</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>TEMPLATE</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>TEMPLATE</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 13 */
				new ComboInput<>(
					"TestContextSwaps",
					TestContextSwaps.class,
					new TestContextSwaps()
				)
				.json("JSON")
				.jsonT("JSON")
				.jsonR("JSON")
				.xml("XML")
				.xmlT("XML")
				.xmlR("XML\n")
				.xmlNs("XML")
				.html("HTML")
				.htmlT("HTML")
				.htmlR("HTML")
				.uon("UON")
				.uonT("UON")
				.uonR("UON")
				.urlEnc("URLENCODING")
				.urlEncT("URLENCODING")
				.urlEncR("URLENCODING")
				.msgPack("4D53475041434B")
				.msgPackT("4D53475041434B")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>RDFXML</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>RDFXML</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 14 */
				new ComboInput<>(
					"BeanA",
					BeanA.class,
					new BeanA()
				)
				.json("SWAPPED")
				.jsonT("SWAPPED")
				.jsonR("SWAPPED")
				.xml("SWAPPED")
				.xmlT("SWAPPED")
				.xmlR("SWAPPED\n")
				.xmlNs("SWAPPED")
				.html("SWAPPED")
				.htmlT("SWAPPED")
				.htmlR("SWAPPED")
				.uon("(f=1)")
				.uonT("(f=1)")
				.uonR("(\n\tf=1\n)")
				.urlEnc("f=1")
				.urlEncT("f=1")
				.urlEncR("f=1")
				.msgPack("81A16601")
				.msgPackT("81A16601")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f>1</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f>1</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
			{ 	/* 15 */
				new ComboInput<>(
					"BeanB",
					BeanB.class,
					new BeanB()
				)
				.json("{f:1}")
				.jsonT("{f:1}")
				.jsonR("{\n\tf: 1\n}")
				.xml("<object><f>1</f></object>")
				.xmlT("<object><f>1</f></object>")
				.xmlR("<object>\n\t<f>1</f>\n</object>\n")
				.xmlNs("<object><f>1</f></object>")
				.html("<table><tr><td>f</td><td>1</td></tr></table>")
				.htmlT("<table><tr><td>f</td><td>1</td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("SWAPPED")
				.uonT("SWAPPED")
				.uonR("SWAPPED")
				.urlEnc("SWAPPED")
				.urlEncT("SWAPPED")
				.urlEncR("SWAPPED")
				.msgPack("53574150504544")
				.msgPackT("53574150504544")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<j:value>SWAPPED</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<j:value>SWAPPED</j:value>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <j:value>SWAPPED</j:value>\n  </rdf:Description>\n</rdf:RDF>\n")
			},
		});
	}

	public SwapsAnnotationComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.copy().swaps(
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

	@Swap(value=SwapJson.class, mediaTypes={"application/json5"})
	@Swap(value=SwapXml.class, mediaTypes={"text/xml"})
	@Swap(value=SwapHtml.class, mediaTypes={"text/html"})
	@Swap(value=SwapUon.class, mediaTypes={"text/uon"})
	@Swap(value=SwapUrlEncoding.class, mediaTypes={"application/x-www-form-urlencoded"})
	@Swap(value=SwapMsgPack.class, mediaTypes={"octal/msgpack"})
	@Swap(value=SwapRdfXml.class, mediaTypes={"text/xml+rdf"})
	public static class TestMediaTypeLiterals {}

	@Swap(value=SwapJson.class, mediaTypes={"*/json5"})
	@Swap(value=SwapXml.class, mediaTypes={"*/xml"})
	@Swap(value=SwapHtml.class, mediaTypes={"*/html"})
	@Swap(value=SwapUon.class, mediaTypes={"*/uon"})
	@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/x-www-form-urlencoded"})
	@Swap(value=SwapMsgPack.class, mediaTypes={"*/msgpack"})
	@Swap(value=SwapRdfXml.class, mediaTypes={"*/xml+rdf"})
	public static class TestMediaTypePatterns {}

	@Swap(value=SwapRdfXml.class, mediaTypes={"*/xml+rdf"})
	@Swap(value=SwapMsgPack.class, mediaTypes={"*/msgpack"})
	@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/x-www-form-urlencoded"})
	@Swap(value=SwapUon.class, mediaTypes={"*/uon"})
	@Swap(value=SwapHtml.class, mediaTypes={"*/html"})
	@Swap(value=SwapXml.class, mediaTypes={"*/xml"})
	@Swap(value=SwapJson.class, mediaTypes={"*/json5"})
	public static class TestMediaTypePatternsReversed {}

	@Swap(value=SwapJson.class, mediaTypes={"*/foo","*/json5","*/bar"})
	@Swap(value=SwapXml.class, mediaTypes={"*/foo","*/xml","*/bar"})
	@Swap(value=SwapHtml.class, mediaTypes={"*/foo","*/html","*/bar"})
	@Swap(value=SwapUon.class, mediaTypes={"*/foo","*/uon","*/bar"})
	@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/foo","*/x-www-form-urlencoded","*/bar"})
	@Swap(value=SwapMsgPack.class, mediaTypes={"*/foo","*/msgpack","*/bar"})
	@Swap(value=SwapRdfXml.class, mediaTypes={"*/foo","*/xml+rdf","*/bar"})
	public static class TestMediaTypePatternsMulti {}

	@Swap(value=SwapJson.class, mediaTypes={"*/foo","*/json5","*/bar"})
	@Swap(value=SwapXml.class, mediaTypes={"*/foo","*/xml","*/bar"})
	@Swap(value=SwapHtml.class, mediaTypes={"*/foo","*/html","*/bar"})
	public static class TestMediaTypePatternsPartial1 {
		@Override
		public String toString() {
			return "foo";
		}
	}

	@Swap(value=SwapUon.class, mediaTypes={"*/foo","*/uon","*/bar"})
	@Swap(value=SwapUrlEncoding.class, mediaTypes={"*/foo","*/x-www-form-urlencoded","*/bar"})
	@Swap(value=SwapMsgPack.class, mediaTypes={"*/foo","*/msgpack","*/bar"})
	@Swap(value=SwapRdfXml.class, mediaTypes={"*/foo","*/xml+rdf","*/bar"})
	public static class TestMediaTypePatternsPartial2 {
		@Override
		public String toString() {
			return "foo";
		}
	}

	@Swap(value=SwapXml.class, mediaTypes={"text/xml+*"})
	public static class TestMediaTypePatternsXmlPlus {
		@Override
		public String toString() {
			return "foo";
		}
	}

	@Swap(value=SwapXml.class, mediaTypes={"text/*+xml"})
	public static class TestMediaTypePatternsXmlPlusReversed {
		@Override
		public String toString() {
			return "foo";
		}
	}

	@Swap(value=SwapXml.class, mediaTypes={"text/rdf+*"})
	public static class TestMediaTypePatternsRdfPlus {
		@Override
		public String toString() {
			return "foo";
		}
	}

	public static class SwapJson extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "JSON";
		}
	}
	public static class SwapXml extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "XML";
		}
	}
	public static class SwapHtml extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "HTML";
		}
	}
	public static class SwapUon extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "UON";
		}
	}
	public static class SwapUrlEncoding extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "URLENCODING";
		}
	}
	public static class SwapMsgPack extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "MSGPACK";
		}
	}
	public static class SwapRdfXml extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o) throws Exception {
			return "RDFXML";
		}
	}

	@Swap(impl=TemplateSwap.class,template="foo")
	public static class TestTemplate {}

	@Swap(value=TemplateSwap.class, mediaTypes={"*/json5"}, template="JSON")
	@Swap(value=TemplateSwap.class, mediaTypes={"*/xml"}, template="XML")
	@Swap(value=TemplateSwap.class, mediaTypes={"*/html"}, template="HTML")
	@Swap(value=TemplateSwap.class, mediaTypes={"*/uon"}, template="UON")
	@Swap(value=TemplateSwap.class, mediaTypes={"*/x-www-form-urlencoded"}, template="URLENCODING")
	@Swap(value=TemplateSwap.class, mediaTypes={"*/msgpack"}, template="MSGPACK")
	@Swap(value=TemplateSwap.class, mediaTypes={"*/xml+rdf"}, template="RDFXML")
	public static class TestTemplates {}


	public static class TemplateSwap extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o, String template) throws Exception {
			return reader(template);
		}
	}

	@Swap(TemplateSwapJson.class)
	@Swap(TemplateSwapXml.class)
	@Swap(TemplateSwapHtml.class)
	@Swap(TemplateSwapUon.class)
	@Swap(TemplateSwapUrlEncoding.class)
	@Swap(TemplateSwapMsgPack.class)
	@Swap(TemplateSwapRdfXml.class)
	public static class TestProgrammaticTemplates {}

	public static class TemplateSwapJson extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/json5");
		}
		@Override
		public String withTemplate() {
			return "JSON";
		}
	}
	public static class TemplateSwapXml extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/xml");
		}
		@Override
		public String withTemplate() {
			return "XML";
		}
	}
	public static class TemplateSwapHtml extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/html");
		}
		@Override
		public String withTemplate() {
			return "HTML";
		}
	}
	public static class TemplateSwapUon extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/uon");
		}
		@Override
		public String withTemplate() {
			return "UON";
		}
	}
	public static class TemplateSwapUrlEncoding extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/x-www-form-urlencoded");
		}
		@Override
		public String withTemplate() {
			return "URLENCODING";
		}
	}
	public static class TemplateSwapMsgPack extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/msgpack");
		}
		@Override
		public String withTemplate() {
			return "MSGPACK";
		}
	}
	public static class TemplateSwapRdfXml extends TemplateSwap {
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/xml+rdf");
		}
		@Override
		public String withTemplate() {
			return "RDFXML";
		}
	}


	public static class TestContextSwap {}

	public static class ContextSwap extends ObjectSwap<TestContextSwap,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwap o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public String withTemplate() {
			return "TEMPLATE";
		}
	}

	public static class TestContextSwaps {}

	public static class ContextSwapJson extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/json5");
		}
		@Override
		public String withTemplate() {
			return "JSON";
		}
	}
	public static class ContextSwapXml extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/xml");
		}
		@Override
		public String withTemplate() {
			return "XML";
		}
	}
	public static class ContextSwapHtml extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/html");
		}
		@Override
		public String withTemplate() {
			return "HTML";
		}
	}
	public static class ContextSwapUon extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/uon");
		}
		@Override
		public String withTemplate() {
			return "UON";
		}
	}
	public static class ContextSwapUrlEncoding extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/x-www-form-urlencoded");
		}
		@Override
		public String withTemplate() {
			return "URLENCODING";
		}
	}
	public static class ContextSwapMsgPack extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/msgpack");
		}
		@Override
		public String withTemplate() {
			return "MSGPACK";
		}
	}
	public static class ContextSwapRdfXml extends ObjectSwap<TestContextSwaps,Object> {
		@Override
		public Object swap(BeanSession session, TestContextSwaps o, String template) throws Exception {
			return reader(template);
		}
		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/xml+rdf");
		}
		@Override
		public String withTemplate() {
			return "RDFXML";
		}
	}

	@Swap(value=BeanSwap.class, mediaTypes={"*/json5"})
	@Swap(value=BeanSwap.class, mediaTypes={"*/xml"})
	@Swap(value=BeanSwap.class, mediaTypes={"*/html"})
	public static class BeanA {
		public int f = 1;
	}

	@Swap(value=BeanSwap.class, mediaTypes={"*/uon"})
	@Swap(value=BeanSwap.class, mediaTypes={"*/x-www-form-urlencoded"})
	@Swap(value=BeanSwap.class, mediaTypes={"*/msgpack"})
	@Swap(value=BeanSwap.class, mediaTypes={"*/xml+rdf"})
	public static class BeanB {
		public int f = 1;
	}

	public static class BeanSwap extends ObjectSwap<Object,Object> {
		@Override
		public Object swap(BeanSession session, Object o, String template) throws Exception {
			return reader("SWAPPED");
		}
	}
}
