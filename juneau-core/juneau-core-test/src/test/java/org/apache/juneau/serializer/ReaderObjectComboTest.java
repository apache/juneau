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
package org.apache.juneau.serializer;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that Reader and InputStream objects are serialized correctly.
 * Note that these are one-way serializations and you're not guaranteed to produce parsable output.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class ReaderObjectComboTest extends ComboSerializeTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<Reader>(
					"SimpleReader",
					Reader.class,
					null,
					/* Json */		"foobar",
					/* JsonT */		"foobar",
					/* JsonR */		"foobar",
					/* Xml */		"foobar",
					/* XmlT */		"foobar",
					/* XmlR */		"foobar\n",
					/* XmlNs */		"foobar",
					/* Html */		"foobar",
					/* HtmlT */		"foobar",
					/* HtmlR */		"foobar",
					/* Uon */		"foobar",
					/* UonT */		"foobar",
					/* UonR */		"foobar",
					/* UrlEnc */	"foobar",
					/* UrlEncT */	"foobar",
					/* UrlEncR */	"foobar",
					/* MsgPack */	"666F6F626172",
					/* MsgPackT */	"666F6F626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>foobar</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public Reader getInput() {
						return new StringReader("foobar");
					}
				}
			},
			{ 	/* 1 */
				new ComboInput<InputStream>(
					"SimpleInputStream",
					InputStream.class,
					null,
					/* Json */		"foobar",
					/* JsonT */		"foobar",
					/* JsonR */		"foobar",
					/* Xml */		"foobar",
					/* XmlT */		"foobar",
					/* XmlR */		"foobar\n",
					/* XmlNs */		"foobar",
					/* Html */		"foobar",
					/* HtmlT */		"foobar",
					/* HtmlR */		"foobar",
					/* Uon */		"foobar",
					/* UonT */		"foobar",
					/* UonR */		"foobar",
					/* UrlEnc */	"foobar",
					/* UrlEncT */	"foobar",
					/* UrlEncR */	"foobar",
					/* MsgPack */	"666F6F626172",
					/* MsgPackT */	"666F6F626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>foobar</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>foobar</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public InputStream getInput() throws Exception {
						return IOUtils.toInputStream("foobar");
					}
				}
			},
			{ 	/* 2 */
				new ComboInput<BeanWithReaderField>(
					"BeanWithReaderField",
					BeanWithReaderField.class,
					null,
					/* Json */		"{f:fv}",
					/* JsonT */		"{f:fv}",
					/* JsonR */		"{\n\tf: fv\n}",
					/* Xml */		"<object><f>fv</f></object>",
					/* XmlT */		"<object><f>fv</f></object>",
					/* XmlR */		"<object>\n\t<f>fv</f>\n</object>\n",
					/* XmlNs */		"<object><f>fv</f></object>",
					/* Html */		"<table><tr><td>f</td><td>fv</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td>fv</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>fv</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=fv)",
					/* UonT */		"(f=fv)",
					/* UonR */		"(\n\tf=fv\n)",
					/* UrlEnc */	"f=fv",
					/* UrlEncT */	"f=fv",
					/* UrlEncR */	"f=fv",
					/* MsgPack */	"81A1666676",
					/* MsgPackT */	"81A1666676",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>fv</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>fv</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>fv</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReaderField getInput() throws Exception {
						return new BeanWithReaderField().init();
					}
				}
			},
			{ 	/* 3 */
				new ComboInput<BeanWithReader1dField>(
					"BeanWithReader1dField",
					BeanWithReader1dField.class,
					null,
					/* Json */		"{f:[fv1,fv2,null]}",
					/* JsonT */		"{f:[fv1,fv2,null]}",
					/* JsonR */		"{\n\tf: [\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><f>fv1fv2<null/></f></object>",
					/* XmlT */		"<object><f>fv1fv2<null/></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\tfv1\n\t\tfv2\n\t\t<null/>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f>fv1fv2<null/></f></object>",
					/* Html */		"<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>fv1</li>\n\t\t\t\t<li>fv2</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=@(fv1,fv2,null))",
					/* UonT */		"(f=@(fv1,fv2,null))",
					/* UonR */		"(\n\tf=@(\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"f=@(fv1,fv2,null)",
					/* UrlEncT */	"f=@(fv1,fv2,null)",
					/* UrlEncR */	"f=@(\n\tfv1,\n\tfv2,\n\tnull\n)",
					/* MsgPack */	"81A16693667631667632C0",
					/* MsgPackT */	"81A16693667631667632C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>fv1</rdf:li>\n        <rdf:li>fv2</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReader1dField getInput() throws Exception {
						return new BeanWithReader1dField().init();
					}
				}
			},
			{ 	/* 4 */
				new ComboInput<BeanWithReaderNullField>(
					"BeanWithReaderNullField",
					BeanWithReaderNullField.class,
					null,
					/* Json */		"{f:null}",
					/* JsonT */		"{f:null}",
					/* JsonR */		"{\n\tf: null\n}",
					/* Xml */		"<object><f _type='null'/></object>",
					/* XmlT */		"<object><f t='null'/></object>",
					/* XmlR */		"<object>\n\t<f _type='null'/>\n</object>\n",
					/* XmlNs */		"<object><f _type='null'/></object>",
					/* Html */		"<table><tr><td>f</td><td><null/></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><null/></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td><null/></td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=null)",
					/* UonT */		"(f=null)",
					/* UonR */		"(\n\tf=null\n)",
					/* UrlEnc */	"f=null",
					/* UrlEncT */	"f=null",
					/* UrlEncR */	"f=null",
					/* MsgPack */	"81A166C0",
					/* MsgPackT */	"81A166C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReaderNullField getInput() throws Exception {
						return new BeanWithReaderNullField().init();
					}
				}
			},
			{ 	/* 5 */
				new ComboInput<BeanWithReaderListField>(
					"BeanWithReaderListField",
					BeanWithReaderListField.class,
					null,
					/* Json */		"{f:[fv1,fv2,null]}",
					/* JsonT */		"{f:[fv1,fv2,null]}",
					/* JsonR */		"{\n\tf: [\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><f>fv1fv2<null/></f></object>",
					/* XmlT */		"<object><f>fv1fv2<null/></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\tfv1\n\t\tfv2\n\t\t<null/>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f>fv1fv2<null/></f></object>",
					/* Html */		"<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><ul><li>fv1</li><li>fv2</li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>fv1</li>\n\t\t\t\t<li>fv2</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=@(fv1,fv2,null))",
					/* UonT */		"(f=@(fv1,fv2,null))",
					/* UonR */		"(\n\tf=@(\n\t\tfv1,\n\t\tfv2,\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"f=@(fv1,fv2,null)",
					/* UrlEncT */	"f=@(fv1,fv2,null)",
					/* UrlEncR */	"f=@(\n\tfv1,\n\tfv2,\n\tnull\n)",
					/* MsgPack */	"81A16693667631667632C0",
					/* MsgPackT */	"81A16693667631667632C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>fv1</rdf:li>\n<rdf:li>fv2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>fv1</rdf:li>\n        <rdf:li>fv2</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReaderListField getInput() throws Exception {
						return new BeanWithReaderListField().init();
					}
				}
			},
			{ 	/* 6 */
				new ComboInput<BeanWithReaderMapField>(
					"BeanWithReaderMapField",
					BeanWithReaderMapField.class,
					null,
					/* Json */		"{f:{foo:fv1,bar:null,null:fv2}}",
					/* JsonT */		"{f:{foo:fv1,bar:null,null:fv2}}",
					/* JsonR */		"{\n\tf: {\n\t\tfoo: fv1,\n\t\tbar: null,\n\t\tnull: fv2\n\t}\n}",
					/* Xml */		"<object><f><foo>fv1</foo><bar _type='null'/><_x0000_>fv2</_x0000_></f></object>",
					/* XmlT */		"<object><f><foo>fv1</foo><bar t='null'/><_x0000_>fv2</_x0000_></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<foo>fv1</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>fv2</_x0000_>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><foo>fv1</foo><bar _type='null'/><_x0000_>fv2</_x0000_></f></object>",
					/* Html */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td>fv1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>fv2</td></tr></table></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td>fv1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>fv2</td></tr></table></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>fv1</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>fv2</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=(foo=fv1,bar=null,null=fv2))",
					/* UonT */		"(f=(foo=fv1,bar=null,null=fv2))",
					/* UonR */		"(\n\tf=(\n\t\tfoo=fv1,\n\t\tbar=null,\n\t\tnull=fv2\n\t)\n)",
					/* UrlEnc */	"f=(foo=fv1,bar=null,null=fv2)",
					/* UrlEncT */	"f=(foo=fv1,bar=null,null=fv2)",
					/* UrlEncR */	"f=(\n\tfoo=fv1,\n\tbar=null,\n\tnull=fv2\n)",
					/* MsgPack */	"81A16683A3666F6F667631A3626172C0C0667632",
					/* MsgPackT */	"81A16683A3666F6F667631A3626172C0C0667632",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>fv1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>fv2</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>fv1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>fv2</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo>fv1</jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_>fv2</jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReaderMapField getInput() throws Exception {
						return new BeanWithReaderMapField().init();
					}
				}
			},
			{ 	/* 7 */
				new ComboInput<BeanWithReaderBeanListField>(
					"BeanWithReaderBeanListField",
					BeanWithReaderBeanListField.class,
					null,
					/* Json */		"{f:[{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},null]}",
					/* JsonT */		"{f:[{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},null]}",
					/* JsonR */		"{\n\tf: [\n\t\t{\n\t\t\tf1: f1v,\n\t\t\tf2: [\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: f5v1,\n\t\t\t\tbar: null,\n\t\t\t\tnull: f5v2\n\t\t\t}\n\t\t},\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><f><object><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></object><null/></f></object>",
					/* XmlT */		"<object><f><object><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 t='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar t='null'/><_x0000_>f5v2</_x0000_></f5></object><null/></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<object>\n\t\t\t<f1>f1v</f1>\n\t\t\t<f2>\n\t\t\t\tf2v1\n\t\t\t\tf2v2\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\tf4v1\n\t\t\t\tf4v2\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>f5v1</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>f5v2</_x0000_>\n\t\t\t</f5>\n\t\t</object>\n\t\t<null/>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><object><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></object><null/></f></object>",
					/* Html */		"<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<table>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t<td>f1v</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>f2v1</li>\n\t\t\t\t\t\t\t\t\t<li>f2v2</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>f4v1</li>\n\t\t\t\t\t\t\t\t\t<li>f4v2</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t<td>f5v1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t<td>f5v2</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t</table>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null))",
					/* UonT */		"(f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null))",
					/* UonR */		"(\n\tf=@(\n\t\t(\n\t\t\tf1=f1v,\n\t\t\tf2=@(\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=f5v1,\n\t\t\t\tbar=null,\n\t\t\t\tnull=f5v2\n\t\t\t)\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null)",
					/* UrlEncT */	"f=@((f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),null)",
					/* UrlEncR */	"f=@(\n\t(\n\t\tf1=f1v,\n\t\tf2=@(\n\t\t\tf2v1,\n\t\t\tf2v2,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tf4v1,\n\t\t\tf4v2,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=f5v1,\n\t\t\tbar=null,\n\t\t\tnull=f5v2\n\t\t)\n\t),\n\tnull\n)",
					/* MsgPack */	"81A1669285A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632C0",
					/* MsgPackT */	"81A1669285A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:f1>f1v</jp:f1>\n          <jp:f2>\n            <rdf:Seq>\n              <rdf:li>f2v1</rdf:li>\n              <rdf:li>f2v2</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f2>\n          <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:f4>\n            <rdf:Seq>\n              <rdf:li>f4v1</rdf:li>\n              <rdf:li>f4v2</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f4>\n          <jp:f5 rdf:parseType='Resource'>\n            <jp:foo>f5v1</jp:foo>\n            <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            <jp:_x0000_>f5v2</jp:_x0000_>\n          </jp:f5>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReaderBeanListField getInput() throws Exception {
						return new BeanWithReaderBeanListField().init();
					}
				}
			},
			{ 	/* 8 */
				new ComboInput<BeanWithReaderBeanMapField>(
					"BeanWithReaderBeanMapField",
					BeanWithReaderBeanMapField.class,
					null,
					/* Json */		"{f:{foo:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},bar:null,null:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}}}}",
					/* JsonT */		"{f:{foo:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}},bar:null,null:{f1:f1v,f2:[f2v1,f2v2,null],f3:null,f4:[f4v1,f4v2,null],f5:{foo:f5v1,bar:null,null:f5v2}}}}",
					/* JsonR */		"{\n\tf: {\n\t\tfoo: {\n\t\t\tf1: f1v,\n\t\t\tf2: [\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: f5v1,\n\t\t\t\tbar: null,\n\t\t\t\tnull: f5v2\n\t\t\t}\n\t\t},\n\t\tbar: null,\n\t\tnull: {\n\t\t\tf1: f1v,\n\t\t\tf2: [\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: f5v1,\n\t\t\t\tbar: null,\n\t\t\t\tnull: f5v2\n\t\t\t}\n\t\t}\n\t}\n}",
					/* Xml */		"<object><f><foo><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></_x0000_></f></object>",
					/* XmlT */		"<object><f><foo><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 t='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar t='null'/><_x0000_>f5v2</_x0000_></f5></foo><bar t='null'/><_x0000_><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 t='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar t='null'/><_x0000_>f5v2</_x0000_></f5></_x0000_></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<foo>\n\t\t\t<f1>f1v</f1>\n\t\t\t<f2>\n\t\t\t\tf2v1\n\t\t\t\tf2v2\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\tf4v1\n\t\t\t\tf4v2\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>f5v1</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>f5v2</_x0000_>\n\t\t\t</f5>\n\t\t</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>\n\t\t\t<f1>f1v</f1>\n\t\t\t<f2>\n\t\t\t\tf2v1\n\t\t\t\tf2v2\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\tf4v1\n\t\t\t\tf4v2\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>f5v1</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>f5v2</_x0000_>\n\t\t\t</f5>\n\t\t</_x0000_>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><foo><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>f1v</f1><f2>f2v1f2v2<null/></f2><f3 _type='null'/><f4>f4v1f4v2<null/></f4><f5><foo>f5v1</foo><bar _type='null'/><_x0000_>f5v2</_x0000_></f5></_x0000_></f></object>",
					/* Html */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr></table></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>f1v</td></tr><tr><td>f2</td><td><ul><li>f2v1</li><li>f2v2</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>f4v1</li><li>f4v2</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>f5v1</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>f5v2</td></tr></table></td></tr></table></td></tr></table></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>f1v</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f2v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f2v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f4v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f4v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v2</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>f1v</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f2v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f2v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>f4v1</li>\n\t\t\t\t\t\t\t\t\t\t<li>f4v2</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>f5v2</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2))))",
					/* UonT */		"(f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2))))",
					/* UonR */		"(\n\tf=(\n\t\tfoo=(\n\t\t\tf1=f1v,\n\t\t\tf2=@(\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=f5v1,\n\t\t\t\tbar=null,\n\t\t\t\tnull=f5v2\n\t\t\t)\n\t\t),\n\t\tbar=null,\n\t\tnull=(\n\t\t\tf1=f1v,\n\t\t\tf2=@(\n\t\t\t\tf2v1,\n\t\t\t\tf2v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tf4v1,\n\t\t\t\tf4v2,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=f5v1,\n\t\t\t\tbar=null,\n\t\t\t\tnull=f5v2\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)))",
					/* UrlEncT */	"f=(foo=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)),bar=null,null=(f1=f1v,f2=@(f2v1,f2v2,null),f3=null,f4=@(f4v1,f4v2,null),f5=(foo=f5v1,bar=null,null=f5v2)))",
					/* UrlEncR */	"f=(\n\tfoo=(\n\t\tf1=f1v,\n\t\tf2=@(\n\t\t\tf2v1,\n\t\t\tf2v2,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tf4v1,\n\t\t\tf4v2,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=f5v1,\n\t\t\tbar=null,\n\t\t\tnull=f5v2\n\t\t)\n\t),\n\tbar=null,\n\tnull=(\n\t\tf1=f1v,\n\t\tf2=@(\n\t\t\tf2v1,\n\t\t\tf2v2,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tf4v1,\n\t\t\tf4v2,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=f5v1,\n\t\t\tbar=null,\n\t\t\tnull=f5v2\n\t\t)\n\t)\n)",
					/* MsgPack */	"81A16683A3666F6F85A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632A3626172C0C085A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632",
					/* MsgPackT */	"81A16683A3666F6F85A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632A3626172C0C085A26631663176A26632936632763166327632C0A26633C0A26634936634763166347632C0A2663583A3666F6F66357631A3626172C0C066357632",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>f1v</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>f2v1</rdf:li>\n<rdf:li>f2v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>f4v1</rdf:li>\n<rdf:li>f4v2</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>f5v1</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>f5v2</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo rdf:parseType='Resource'>\n        <jp:f1>f1v</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>f2v1</rdf:li>\n            <rdf:li>f2v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>f4v1</rdf:li>\n            <rdf:li>f4v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>f5v1</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>f5v2</jp:_x0000_>\n        </jp:f5>\n      </jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_ rdf:parseType='Resource'>\n        <jp:f1>f1v</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>f2v1</rdf:li>\n            <rdf:li>f2v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>f4v1</rdf:li>\n            <rdf:li>f4v2</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>f5v1</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>f5v2</jp:_x0000_>\n        </jp:f5>\n      </jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				) {
					@Override
					public BeanWithReaderBeanMapField getInput() throws Exception {
						return new BeanWithReaderBeanMapField().init();
					}
				}
			},
		});
	}

	public ReaderObjectComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().trimNullProperties(false).build();
	}

	public static class BeanWithReaderField {
		public Reader f;
		public BeanWithReaderField init() {
			f = new StringReader("fv");
			return this;
		}
	}

	public static class BeanWithReader1dField {
		public Reader[] f;
		public BeanWithReader1dField init() {
			f = new Reader[]{new StringReader("fv1"),new StringReader("fv2"),null};
			return this;
		}
	}

	public static class BeanWithReaderNullField {
		public Reader f;
		public BeanWithReaderNullField init() {
			f = null;
			return this;
		}
	}

	public static class BeanWithReaderListField {
		public List<Reader> f;
		public BeanWithReaderListField init() {
			f = new AList<Reader>()
				.append(new StringReader("fv1"))
				.append(new StringReader("fv2"))
				.append(null)
			;
			return this;
		}
	}

	public static class BeanWithReaderMapField {
		public Map<String,Reader> f;
		public BeanWithReaderMapField init() {
			f = new AMap<String,Reader>()
				.append("foo", new StringReader("fv1"))
				.append("bar", null)
				.append(null, new StringReader("fv2"))
			;
			return this;
		}
	}

	public static class BeanWithReaderBeanListField {
		public List<B> f;
		public BeanWithReaderBeanListField init() {
			f = new AList<B>()
				.append(new B().init())
				.append(null)
			;
			return this;
		}
	}

	public static class BeanWithReaderBeanMapField {
		public Map<String,B> f;
		public BeanWithReaderBeanMapField init() {
			f = new AMap<String,B>()
				.append("foo", new B().init())
				.append("bar", null)
				.append(null, new B().init())
			;
			return this;
		}
	}

	public static class B {
		public Reader f1;
		public Reader[] f2;
		public Reader f3;
		public List<Reader> f4;
		public Map<String,Reader> f5;

		public B init() {
			f1 = new StringReader("f1v");
			f2 = new Reader[]{new StringReader("f2v1"),new StringReader("f2v2"),null};
			f3 = null;
			f4 = new AList<Reader>()
				.append(new StringReader("f4v1"))
				.append(new StringReader("f4v2"))
				.append(null)
			;
			f5 = new AMap<String,Reader>()
				.append("foo", new StringReader("f5v1"))
				.append("bar", null)
				.append(null, new StringReader("f5v2"))
			;
			return this;
		}
	}
}
