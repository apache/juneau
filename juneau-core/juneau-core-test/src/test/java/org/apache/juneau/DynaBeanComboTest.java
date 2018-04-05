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

import static org.apache.juneau.TestUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests DynaBean support.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class DynaBeanComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<BeanWithDynaField>(
					"BeanWithDynaField",
					BeanWithDynaField.class,
					new BeanWithDynaField().init(),
					/* Json */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonT */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonR */		"{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}",
					/* Xml */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlT */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlR */		"<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n",
					/* XmlNs */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* Html */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonT */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonR */		"(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)",
					/* UrlEnc */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncT */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncR */	"f1=1\n&f2a=a\n&f2b=b\n&f3=3",
					/* MsgPack */	"84A2663101A3663261A161A3663262A162A2663303",
					/* MsgPackT */	"84A2663101A3663261A161A3663262A162A2663303",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithDynaField o) {
						assertType(BeanWithDynaField.class, o);
					}
				}
			},
			{ 	/* 1 */
				new ComboInput<BeanWithDynaMethods>(
					"BeanWithDynaMethods",
					BeanWithDynaMethods.class,
					new BeanWithDynaMethods().init(),
					/* Json */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonT */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonR */		"{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}",
					/* Xml */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlT */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlR */		"<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n",
					/* XmlNs */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* Html */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonT */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonR */		"(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)",
					/* UrlEnc */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncT */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncR */	"f1=1\n&f2a=a\n&f2b=b\n&f3=3",
					/* MsgPack */	"84A2663101A3663261A161A3663262A162A2663303",
					/* MsgPackT */	"84A2663101A3663261A161A3663262A162A2663303",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithDynaMethods o) {
						assertType(BeanWithDynaMethods.class, o);
						Assert.assertTrue(o.setterCalled);
					}
				}
			},
			{ 	/* 2 */
				new ComboInput<BeanWithDynaGetterOnly>(
					"BeanWithDynaGetterOnly",
					BeanWithDynaGetterOnly.class,
					new BeanWithDynaGetterOnly().init(),
					/* Json */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonT */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonR */		"{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}",
					/* Xml */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlT */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlR */		"<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n",
					/* XmlNs */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* Html */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonT */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonR */		"(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)",
					/* UrlEnc */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncT */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncR */	"f1=1\n&f2a=a\n&f2b=b\n&f3=3",
					/* MsgPack */	"84A2663101A3663261A161A3663262A162A2663303",
					/* MsgPackT */	"84A2663101A3663261A161A3663262A162A2663303",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithDynaGetterOnly o) {
						assertType(BeanWithDynaGetterOnly.class, o);
					}
				}
			},
			{ 	/* 3 */
				new ComboInput<BeanWithDynaFieldSwapped>(
					"BeanWithDynaFieldSwapped",
					BeanWithDynaFieldSwapped.class,
					new BeanWithDynaFieldSwapped().init(),
					/* Json */		"{f1a:'1901-03-03T18:11:12Z'}",
					/* JsonT */		"{f1a:'1901-03-03T18:11:12Z'}",
					/* JsonR */		"{\n\tf1a: '1901-03-03T18:11:12Z'\n}",
					/* Xml */		"<object><f1a>1901-03-03T18:11:12Z</f1a></object>",
					/* XmlT */		"<object><f1a>1901-03-03T18:11:12Z</f1a></object>",
					/* XmlR */		"<object>\n\t<f1a>1901-03-03T18:11:12Z</f1a>\n</object>\n",
					/* XmlNs */		"<object><f1a>1901-03-03T18:11:12Z</f1a></object>",
					/* Html */		"<table><tr><td>f1a</td><td>1901-03-03T18:11:12Z</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1a</td><td>1901-03-03T18:11:12Z</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1a</td>\n\t\t<td>1901-03-03T18:11:12Z</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1a=1901-03-03T18:11:12Z)",
					/* UonT */		"(f1a=1901-03-03T18:11:12Z)",
					/* UonR */		"(\n\tf1a=1901-03-03T18:11:12Z\n)",
					/* UrlEnc */	"f1a=1901-03-03T18:11:12Z",
					/* UrlEncT */	"f1a=1901-03-03T18:11:12Z",
					/* UrlEncR */	"f1a=1901-03-03T18:11:12Z",
					/* MsgPack */	"81A3663161B4313930312D30332D30335431383A31313A31325A",
					/* MsgPackT */	"81A3663161B4313930312D30332D30335431383A31313A31325A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1a>1901-03-03T18:11:12Z</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1a>1901-03-03T18:11:12Z</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1a>1901-03-03T18:11:12Z</jp:f1a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithDynaFieldSwapped o) {
						assertType(BeanWithDynaFieldSwapped.class, o);
						assertType(Calendar.class, o.f1.get("f1a"));
					}
				}
			},
			{ 	/* 4 */
				new ComboInput<BeanWithDynaFieldStringList>(
					"BeanWithDynaFieldStringList",
					BeanWithDynaFieldStringList.class,
					new BeanWithDynaFieldStringList().init(),
					/* Json */		"{f1a:['foo','bar']}",
					/* JsonT */		"{f1a:['foo','bar']}",
					/* JsonR */		"{\n\tf1a: [\n\t\t'foo',\n\t\t'bar'\n\t]\n}",
					/* Xml */		"<object><f1a><string>foo</string><string>bar</string></f1a></object>",
					/* XmlT */		"<object><f1a><string>foo</string><string>bar</string></f1a></object>",
					/* XmlR */		"<object>\n\t<f1a>\n\t\t<string>foo</string>\n\t\t<string>bar</string>\n\t</f1a>\n</object>\n",
					/* XmlNs */		"<object><f1a><string>foo</string><string>bar</string></f1a></object>",
					/* Html */		"<table><tr><td>f1a</td><td><ul><li>foo</li><li>bar</li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1a</td><td><ul><li>foo</li><li>bar</li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1a</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>foo</li>\n\t\t\t\t<li>bar</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1a=@(foo,bar))",
					/* UonT */		"(f1a=@(foo,bar))",
					/* UonR */		"(\n\tf1a=@(\n\t\tfoo,\n\t\tbar\n\t)\n)",
					/* UrlEnc */	"f1a=@(foo,bar)",
					/* UrlEncT */	"f1a=@(foo,bar)",
					/* UrlEncR */	"f1a=@(\n\tfoo,\n\tbar\n)",
					/* MsgPack */	"81A366316192A3666F6FA3626172",
					/* MsgPackT */	"81A366316192A3666F6FA3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1a>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1a>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:f1a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1a>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:f1a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithDynaFieldStringList o) {
						assertType(BeanWithDynaFieldStringList.class, o);
					}
				}
			},
			{ 	/* 5 */
				new ComboInput<BeanWithDynaMethodsAndExtraKeys>(
					"BeanWithDynaMethodsAndExtraKeys",
					BeanWithDynaMethodsAndExtraKeys.class,
					new BeanWithDynaMethodsAndExtraKeys().init(),
					/* Json */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonT */		"{f1:1,f2a:'a',f2b:'b',f3:3}",
					/* JsonR */		"{\n\tf1: 1,\n\tf2a: 'a',\n\tf2b: 'b',\n\tf3: 3\n}",
					/* Xml */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlT */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* XmlR */		"<object>\n\t<f1>1</f1>\n\t<f2a>a</f2a>\n\t<f2b>b</f2b>\n\t<f3>3</f3>\n</object>\n",
					/* XmlNs */		"<object><f1>1</f1><f2a>a</f2a><f2b>b</f2b><f3>3</f3></object>",
					/* Html */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1</td><td>1</td></tr><tr><td>f2a</td><td>a</td></tr><tr><td>f2b</td><td>b</td></tr><tr><td>f3</td><td>3</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>1</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2a</td>\n\t\t<td>a</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2b</td>\n\t\t<td>b</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>3</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonT */		"(f1=1,f2a=a,f2b=b,f3=3)",
					/* UonR */		"(\n\tf1=1,\n\tf2a=a,\n\tf2b=b,\n\tf3=3\n)",
					/* UrlEnc */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncT */	"f1=1&f2a=a&f2b=b&f3=3",
					/* UrlEncR */	"f1=1\n&f2a=a\n&f2b=b\n&f3=3",
					/* MsgPack */	"84A2663101A3663261A161A3663262A162A2663303",
					/* MsgPackT */	"84A2663101A3663261A161A3663262A162A2663303",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1>1</jp:f1>\n<jp:f2a>a</jp:f2a>\n<jp:f2b>b</jp:f2b>\n<jp:f3>3</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1>1</jp:f1>\n    <jp:f2a>a</jp:f2a>\n    <jp:f2b>b</jp:f2b>\n    <jp:f3>3</jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithDynaMethodsAndExtraKeys o) {
						assertType(BeanWithDynaMethodsAndExtraKeys.class, o);
						Assert.assertTrue(o.setterCalled);
					}
				}
			},
		});
	}

	public DynaBeanComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().trimNullProperties(false).build();
	}

	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p.builder().build();
	}

	@Bean(sort=true)
	public static class BeanWithDynaField {
		public int f1;
		@BeanProperty(name="*")
		public Map<String,Object> f2 = new LinkedHashMap<String,Object>();
		public int f3;

		public BeanWithDynaField init() {
			this.f1 = 1;
			this.f2 = new ObjectMap().append("f2a", "a").append("f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaMethods {

		private int f1, f3;
		private Map<String,Object> f2 = new LinkedHashMap<String,Object>();
		private boolean setterCalled = false;

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF3() {
			return f3;
		}
		public void setF3(int f3) {
			this.f3 = f3;
		}

		@BeanProperty(name="*")
		public Map<String, Object> xxx() {
			return f2;
		}

		@BeanProperty(name="*")
		public void setYYY(String name, Object o) {
			setterCalled = true;
			this.f2.put(name, o);
		}

		public BeanWithDynaMethods init() {
			this.f1 = 1;
			this.f2 = new ObjectMap().append("f2a", "a").append("f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaMethodsAndExtraKeys {

		private int f1, f3;
		private Map<String,Object> f2 = new LinkedHashMap<String,Object>();
		private boolean setterCalled = false;

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF3() {
			return f3;
		}
		public void setF3(int f3) {
			this.f3 = f3;
		}

		@BeanProperty(name="*")
		public Object get(String name) {
			return f2.get(name);
		}

		@BeanProperty(name="*")
		public void set(String name, Object o) {
			setterCalled = true;
			this.f2.put(name, o);
		}

		@BeanProperty(name="*")
		public Collection<String> getExtraKeys() {
			return f2.keySet();
		}
		
		public BeanWithDynaMethodsAndExtraKeys init() {
			this.f1 = 1;
			this.f2 = new ObjectMap().append("f2a", "a").append("f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaGetterOnly {

		private int f1, f3;
		private Map<String,Object> f2 = new LinkedHashMap<String,Object>();

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF3() {
			return f3;
		}
		public void setF3(int f3) {
			this.f3 = f3;
		}

		@BeanProperty(name="*")
		public Map<String, Object> xxx() {
			return f2;
		}

		public BeanWithDynaGetterOnly init() {
			this.f1 = 1;
			this.f2 = new ObjectMap().append("f2a", "a").append("f2b", "b");
			this.f3 = 3;
			return this;
		}
	}

	private static Calendar singleDate = new GregorianCalendar(TimeZone.getTimeZone("PST"));
	static {
		singleDate.setTimeInMillis(0);
		singleDate.set(1901, 2, 3, 10, 11, 12);
	}

	@Bean(sort=true)
	public static class BeanWithDynaFieldSwapped {
		@BeanProperty(name="*")
		@Swap(CalendarSwap.ISO8601DTZ.class)
		public Map<String,Calendar> f1 = new LinkedHashMap<String,Calendar>();

		public BeanWithDynaFieldSwapped init() {
			this.f1.put("f1a", singleDate);
			return this;
		}
	}

	@Bean(sort=true)
	public static class BeanWithDynaFieldStringList {
		@BeanProperty(name="*")
		public Map<String,List<String>> f1 = new LinkedHashMap<String,List<String>>();

		public BeanWithDynaFieldStringList init() {
			this.f1.put("f1a", Arrays.asList(new String[]{"foo","bar"}));
			return this;
		}
	}
}