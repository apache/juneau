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

import static org.apache.juneau.testutils.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for the CalendarSwap class.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class ByteArrayBase64SwapComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<byte[]>(
					"ByteArray1d",
					byte[].class,
					new byte[] {1,2,3},
					/* Json */		"'AQID'",
					/* JsonT */		"'AQID'",
					/* JsonR */		"'AQID'",
					/* Xml */		"<string>AQID</string>",
					/* XmlT */		"<string>AQID</string>",
					/* XmlR */		"<string>AQID</string>\n",
					/* XmlNs */		"<string>AQID</string>",
					/* Html */		"<string>AQID</string>",
					/* HtmlT */		"<string>AQID</string>",
					/* HtmlR */		"<string>AQID</string>",
					/* Uon */		"AQID",
					/* UonT */		"AQID",
					/* UonR */		"AQID",
					/* UrlEnc */	"_value=AQID",
					/* UrlEncT */	"_value=AQID",
					/* UrlEncR */	"_value=AQID",
					/* MsgPack */	"A441514944",
					/* MsgPackT */	"A441514944",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<j:value>AQID</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<j:value>AQID</j:value>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <j:value>AQID</j:value>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(byte[] o) {
						assertType(byte[].class, o);
					}
				}
			},
			{ 	/* 1 */
				new ComboInput<byte[][]>(
					"ByteArray2d",
					byte[][].class,
					new byte[][]{{1,2,3},{4,5,6},null},
					/* Json */		"['AQID','BAUG',null]",
					/* JsonT */		"['AQID','BAUG',null]",
					/* JsonR */		"[\n\t'AQID',\n\t'BAUG',\n\tnull\n]",
					/* Xml */		"<array><string>AQID</string><string>BAUG</string><null/></array>",
					/* XmlT */		"<array><string>AQID</string><string>BAUG</string><null/></array>",
					/* XmlR */		"<array>\n\t<string>AQID</string>\n\t<string>BAUG</string>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><string>AQID</string><string>BAUG</string><null/></array>",
					/* Html */		"<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>AQID</li>\n\t<li>BAUG</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(AQID,BAUG,null)",
					/* UonT */		"@(AQID,BAUG,null)",
					/* UonR */		"@(\n\tAQID,\n\tBAUG,\n\tnull\n)",
					/* UrlEnc */	"0=AQID&1=BAUG&2=null",
					/* UrlEncT */	"0=AQID&1=BAUG&2=null",
					/* UrlEncR */	"0=AQID\n&1=BAUG\n&2=null",
					/* MsgPack */	"93A441514944A442415547C0",
					/* MsgPackT */	"93A441514944A442415547C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>AQID</rdf:li>\n    <rdf:li>BAUG</rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(byte[][] o) {
						assertType(byte[][].class, o);
					}
				}
			},
			{ 	/* 2 */
				new ComboInput<List<byte[]>>(
					"ListOfByteArrays",
					getType(List.class,byte[].class),
					new AList<byte[]>()
						.append(new byte[]{1,2,3})
						.append(new byte[]{4,5,6})
						.append(null)
					,
					/* Json */		"['AQID','BAUG',null]",
					/* JsonT */		"['AQID','BAUG',null]",
					/* JsonR */		"[\n\t'AQID',\n\t'BAUG',\n\tnull\n]",
					/* Xml */		"<array><string>AQID</string><string>BAUG</string><null/></array>",
					/* XmlT */		"<array><string>AQID</string><string>BAUG</string><null/></array>",
					/* XmlR */		"<array>\n\t<string>AQID</string>\n\t<string>BAUG</string>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><string>AQID</string><string>BAUG</string><null/></array>",
					/* Html */		"<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li>AQID</li><li>BAUG</li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>AQID</li>\n\t<li>BAUG</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(AQID,BAUG,null)",
					/* UonT */		"@(AQID,BAUG,null)",
					/* UonR */		"@(\n\tAQID,\n\tBAUG,\n\tnull\n)",
					/* UrlEnc */	"0=AQID&1=BAUG&2=null",
					/* UrlEncT */	"0=AQID&1=BAUG&2=null",
					/* UrlEncR */	"0=AQID\n&1=BAUG\n&2=null",
					/* MsgPack */	"93A441514944A442415547C0",
					/* MsgPackT */	"93A441514944A442415547C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>AQID</rdf:li>\n    <rdf:li>BAUG</rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<byte[]> o) {
						assertType(List.class, o);
						assertType(byte[].class, o.get(0));
					}
				}
			},
			{ 	/* 3 */
				new ComboInput<Map<String,byte[]>>(
					"MapOfByteArrays",
					getType(Map.class,String.class,byte[].class),
					new AMap<String,byte[]>()
						.append("foo", new byte[]{1,2,3})
						.append("bar", null)
						.append(null, new byte[]{4,5,6})
						.append("null", new byte[]{7,8,9})
					,
					/* Json */		"{foo:'AQID',bar:null,null:'BAUG','null':'BwgJ'}",
					/* JsonT */		"{foo:'AQID',bar:null,null:'BAUG','null':'BwgJ'}",
					/* JsonR */		"{\n\tfoo: 'AQID',\n\tbar: null,\n\tnull: 'BAUG',\n\t'null': 'BwgJ'\n}",
					/* Xml */		"<object><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_><null>BwgJ</null></object>",
					/* XmlT */		"<object><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_><null>BwgJ</null></object>",
					/* XmlR */		"<object>\n\t<foo>AQID</foo>\n\t<bar _type='null'/>\n\t<_x0000_>BAUG</_x0000_>\n\t<null>BwgJ</null>\n</object>\n",
					/* XmlNs */		"<object><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_><null>BwgJ</null></object>",
					/* Html */		"<table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr><tr><td>null</td><td>BwgJ</td></tr></table>",
					/* HtmlT */		"<table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr><tr><td>null</td><td>BwgJ</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>foo</td>\n\t\t<td>AQID</td>\n\t</tr>\n\t<tr>\n\t\t<td>bar</td>\n\t\t<td><null/></td>\n\t</tr>\n\t<tr>\n\t\t<td><null/></td>\n\t\t<td>BAUG</td>\n\t</tr>\n\t<tr>\n\t\t<td>null</td>\n\t\t<td>BwgJ</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(foo=AQID,bar=null,null=BAUG,'null'=BwgJ)",
					/* UonT */		"(foo=AQID,bar=null,null=BAUG,'null'=BwgJ)",
					/* UonR */		"(\n\tfoo=AQID,\n\tbar=null,\n\tnull=BAUG,\n\t'null'=BwgJ\n)",
					/* UrlEnc */	"foo=AQID&bar=null&null=BAUG&'null'=BwgJ",
					/* UrlEncT */	"foo=AQID&bar=null&null=BAUG&'null'=BwgJ",
					/* UrlEncR */	"foo=AQID\n&bar=null\n&null=BAUG\n&'null'=BwgJ",
					/* MsgPack */	"84A3666F6FA441514944A3626172C0C0A442415547A46E756C6CA44277674A",
					/* MsgPackT */	"84A3666F6FA441514944A3626172C0C0A442415547A46E756C6CA44277674A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n<jp:null>BwgJ</jp:null>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n<jp:null>BwgJ</jp:null>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:foo>AQID</jp:foo>\n    <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n    <jp:_x0000_>BAUG</jp:_x0000_>\n    <jp:null>BwgJ</jp:null>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,byte[]> o) {
						assertType(Map.class, o);
						assertType(String.class, o.keySet().iterator().next());
						assertType(byte[].class, o.values().iterator().next());
					}
				}
			},
			{ 	/* 4 */
				new ComboInput<BeanWithByteArrayField>(
					"BeanWithByteArrayField",
					BeanWithByteArrayField.class,
					new BeanWithByteArrayField().init(),
					/* Json */		"{f:'AQID'}",
					/* JsonT */		"{f:'AQID'}",
					/* JsonR */		"{\n\tf: 'AQID'\n}",
					/* Xml */		"<object><f>AQID</f></object>",
					/* XmlT */		"<object><f>AQID</f></object>",
					/* XmlR */		"<object>\n\t<f>AQID</f>\n</object>\n",
					/* XmlNs */		"<object><f>AQID</f></object>",
					/* Html */		"<table><tr><td>f</td><td>AQID</td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td>AQID</td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>AQID</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=AQID)",
					/* UonT */		"(f=AQID)",
					/* UonR */		"(\n\tf=AQID\n)",
					/* UrlEnc */	"f=AQID",
					/* UrlEncT */	"f=AQID",
					/* UrlEncR */	"f=AQID",
					/* MsgPack */	"81A166A441514944",
					/* MsgPackT */	"81A166A441514944",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>AQID</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>AQID</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>AQID</jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithByteArrayField o) {
						assertType(BeanWithByteArrayField.class, o);
					}
				}
			},
			{	/* 5 */
				new ComboInput<BeanWithByteArray2dField>(
					"BeanWithByteArray2dField",
					BeanWithByteArray2dField.class,
					new BeanWithByteArray2dField().init(),
					/* Json */		"{f:['AQID','BAUG',null]}",
					/* JsonT */		"{f:['AQID','BAUG',null]}",
					/* JsonR */		"{\n\tf: [\n\t\t'AQID',\n\t\t'BAUG',\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><f><string>AQID</string><string>BAUG</string><null/></f></object>",
					/* XmlT */		"<object><f><string>AQID</string><string>BAUG</string><null/></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<string>AQID</string>\n\t\t<string>BAUG</string>\n\t\t<null/>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><string>AQID</string><string>BAUG</string><null/></f></object>",
					/* Html */		"<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>AQID</li>\n\t\t\t\t<li>BAUG</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=@(AQID,BAUG,null))",
					/* UonT */		"(f=@(AQID,BAUG,null))",
					/* UonR */		"(\n\tf=@(\n\t\tAQID,\n\t\tBAUG,\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"f=@(AQID,BAUG,null)",
					/* UrlEncT */	"f=@(AQID,BAUG,null)",
					/* UrlEncR */	"f=@(\n\tAQID,\n\tBAUG,\n\tnull\n)",
					/* MsgPack */	"81A16693A441514944A442415547C0",
					/* MsgPackT */	"81A16693A441514944A442415547C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>AQID</rdf:li>\n        <rdf:li>BAUG</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithByteArray2dField o) {
						assertType(BeanWithByteArray2dField.class, o);
					}
				}
			},
			{	/* 6 */
				new ComboInput<BeanWithByteArrayNullField>(
					"BeanWithByteArrayNullField",
					BeanWithByteArrayNullField.class,
					new BeanWithByteArrayNullField().init(),
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
				)
				{
					@Override
					public void verify(BeanWithByteArrayNullField o) {
						assertType(BeanWithByteArrayNullField.class, o);
					}
				}
			},
			{	/* 7 */
				new ComboInput<BeanWithByteArrayListField>(
					"BeanWithByteArrayListField",
					BeanWithByteArrayListField.class,
					new BeanWithByteArrayListField().init(),
					/* Json */		"{f:['AQID','BAUG',null]}",
					/* JsonT */		"{f:['AQID','BAUG',null]}",
					/* JsonR */		"{\n\tf: [\n\t\t'AQID',\n\t\t'BAUG',\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><f><string>AQID</string><string>BAUG</string><null/></f></object>",
					/* XmlT */		"<object><f><string>AQID</string><string>BAUG</string><null/></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<string>AQID</string>\n\t\t<string>BAUG</string>\n\t\t<null/>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><string>AQID</string><string>BAUG</string><null/></f></object>",
					/* Html */		"<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>AQID</li>\n\t\t\t\t<li>BAUG</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=@(AQID,BAUG,null))",
					/* UonT */		"(f=@(AQID,BAUG,null))",
					/* UonR */		"(\n\tf=@(\n\t\tAQID,\n\t\tBAUG,\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"f=@(AQID,BAUG,null)",
					/* UrlEncT */	"f=@(AQID,BAUG,null)",
					/* UrlEncR */	"f=@(\n\tAQID,\n\tBAUG,\n\tnull\n)",
					/* MsgPack */	"81A16693A441514944A442415547C0",
					/* MsgPackT */	"81A16693A441514944A442415547C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>AQID</rdf:li>\n        <rdf:li>BAUG</rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithByteArrayListField o) {
						assertType(BeanWithByteArrayListField.class, o);
					}
				}
			},
			{	/* 8 */
				new ComboInput<BeanWithByteArrayMapField>(
					"BeanWithByteArrayMapField",
					BeanWithByteArrayMapField.class,
					new BeanWithByteArrayMapField().init(),
					/* Json */		"{f:{foo:'AQID',bar:null,null:'BAUG'}}",
					/* JsonT */		"{f:{foo:'AQID',bar:null,null:'BAUG'}}",
					/* JsonR */		"{\n\tf: {\n\t\tfoo: 'AQID',\n\t\tbar: null,\n\t\tnull: 'BAUG'\n\t}\n}",
					/* Xml */		"<object><f><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f></object>",
					/* XmlT */		"<object><f><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<foo>AQID</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>BAUG</_x0000_>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f></object>",
					/* Html */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>AQID</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=(foo=AQID,bar=null,null=BAUG))",
					/* UonT */		"(f=(foo=AQID,bar=null,null=BAUG))",
					/* UonR */		"(\n\tf=(\n\t\tfoo=AQID,\n\t\tbar=null,\n\t\tnull=BAUG\n\t)\n)",
					/* UrlEnc */	"f=(foo=AQID,bar=null,null=BAUG)",
					/* UrlEncT */	"f=(foo=AQID,bar=null,null=BAUG)",
					/* UrlEncR */	"f=(\n\tfoo=AQID,\n\tbar=null,\n\tnull=BAUG\n)",
					/* MsgPack */	"81A16683A3666F6FA441514944A3626172C0C0A442415547",
					/* MsgPackT */	"81A16683A3666F6FA441514944A3626172C0C0A442415547",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo>AQID</jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_>BAUG</jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithByteArrayMapField o) {
						assertType(BeanWithByteArrayMapField.class, o);
					}
				}
			},
			{	/* 9 */
				new ComboInput<BeanWithByteArrayBeanListField>(
					"BeanWithByteArrayBeanListField",
					BeanWithByteArrayBeanListField.class,
					new BeanWithByteArrayBeanListField().init(),
					/* Json */		"{f:[{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},null]}",
					/* JsonT */		"{f:[{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},null]}",
					/* JsonR */		"{\n\tf: [\n\t\t{\n\t\t\tf1: 'AQID',\n\t\t\tf2: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: 'AQID',\n\t\t\t\tbar: null,\n\t\t\t\tnull: 'BAUG'\n\t\t\t}\n\t\t},\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><f><object><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></object><null/></f></object>",
					/* XmlT */		"<object><f><object><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 t='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f5></object><null/></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<object>\n\t\t\t<f1>AQID</f1>\n\t\t\t<f2>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>AQID</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>BAUG</_x0000_>\n\t\t\t</f5>\n\t\t</object>\n\t\t<null/>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><object><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></object><null/></f></object>",
					/* Html */		"<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><ul><li><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<table>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t</tr>\n\t\t\t\t\t</table>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null))",
					/* UonT */		"(f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null))",
					/* UonR */		"(\n\tf=@(\n\t\t(\n\t\t\tf1=AQID,\n\t\t\tf2=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=AQID,\n\t\t\t\tbar=null,\n\t\t\t\tnull=BAUG\n\t\t\t)\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null)",
					/* UrlEncT */	"f=@((f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),null)",
					/* UrlEncR */	"f=@(\n\t(\n\t\tf1=AQID,\n\t\tf2=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=AQID,\n\t\t\tbar=null,\n\t\t\tnull=BAUG\n\t\t)\n\t),\n\tnull\n)",
					/* MsgPack */	"81A1669285A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547C0",
					/* MsgPackT */	"81A1669285A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:f1>AQID</jp:f1>\n          <jp:f2>\n            <rdf:Seq>\n              <rdf:li>AQID</rdf:li>\n              <rdf:li>BAUG</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f2>\n          <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:f4>\n            <rdf:Seq>\n              <rdf:li>AQID</rdf:li>\n              <rdf:li>BAUG</rdf:li>\n              <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            </rdf:Seq>\n          </jp:f4>\n          <jp:f5 rdf:parseType='Resource'>\n            <jp:foo>AQID</jp:foo>\n            <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n            <jp:_x0000_>BAUG</jp:_x0000_>\n          </jp:f5>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithByteArrayBeanListField o) {
						assertType(BeanWithByteArrayBeanListField.class, o);
					}
				}
			},
			{	/* 10 */
				new ComboInput<BeanWithByteArrayBeanMapField>(
					"BeanWithByteArrayBeanMapField",
					BeanWithByteArrayBeanMapField.class,
					new BeanWithByteArrayBeanMapField().init(),
					/* Json */		"{f:{foo:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},bar:null,null:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}}}}",
					/* JsonT */		"{f:{foo:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}},bar:null,null:{f1:'AQID',f2:['AQID','BAUG',null],f3:null,f4:['AQID','BAUG',null],f5:{foo:'AQID',bar:null,null:'BAUG'}}}}",
					/* JsonR */		"{\n\tf: {\n\t\tfoo: {\n\t\t\tf1: 'AQID',\n\t\t\tf2: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: 'AQID',\n\t\t\t\tbar: null,\n\t\t\t\tnull: 'BAUG'\n\t\t\t}\n\t\t},\n\t\tbar: null,\n\t\tnull: {\n\t\t\tf1: 'AQID',\n\t\t\tf2: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf3: null,\n\t\t\tf4: [\n\t\t\t\t'AQID',\n\t\t\t\t'BAUG',\n\t\t\t\tnull\n\t\t\t],\n\t\t\tf5: {\n\t\t\t\tfoo: 'AQID',\n\t\t\t\tbar: null,\n\t\t\t\tnull: 'BAUG'\n\t\t\t}\n\t\t}\n\t}\n}",
					/* Xml */		"<object><f><foo><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></_x0000_></f></object>",
					/* XmlT */		"<object><f><foo><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 t='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f5></foo><bar t='null'/><_x0000_><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 t='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar t='null'/><_x0000_>BAUG</_x0000_></f5></_x0000_></f></object>",
					/* XmlR */		"<object>\n\t<f>\n\t\t<foo>\n\t\t\t<f1>AQID</f1>\n\t\t\t<f2>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>AQID</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>BAUG</_x0000_>\n\t\t\t</f5>\n\t\t</foo>\n\t\t<bar _type='null'/>\n\t\t<_x0000_>\n\t\t\t<f1>AQID</f1>\n\t\t\t<f2>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f2>\n\t\t\t<f3 _type='null'/>\n\t\t\t<f4>\n\t\t\t\t<string>AQID</string>\n\t\t\t\t<string>BAUG</string>\n\t\t\t\t<null/>\n\t\t\t</f4>\n\t\t\t<f5>\n\t\t\t\t<foo>AQID</foo>\n\t\t\t\t<bar _type='null'/>\n\t\t\t\t<_x0000_>BAUG</_x0000_>\n\t\t\t</f5>\n\t\t</_x0000_>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><f><foo><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></foo><bar _type='null'/><_x0000_><f1>AQID</f1><f2><string>AQID</string><string>BAUG</string><null/></f2><f3 _type='null'/><f4><string>AQID</string><string>BAUG</string><null/></f4><f5><foo>AQID</foo><bar _type='null'/><_x0000_>BAUG</_x0000_></f5></_x0000_></f></object>",
					/* Html */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr></table></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f</td><td><table><tr><td>foo</td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td><table><tr><td>f1</td><td>AQID</td></tr><tr><td>f2</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f3</td><td><null/></td></tr><tr><td>f4</td><td><ul><li>AQID</li><li>BAUG</li><li><null/></li></ul></td></tr><tr><td>f5</td><td><table><tr><td>foo</td><td>AQID</td></tr><tr><td>bar</td><td><null/></td></tr><tr><td><null/></td><td>BAUG</td></tr></table></td></tr></table></td></tr></table></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<table>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f1</td>\n\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f2</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f3</td>\n\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f4</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>AQID</li>\n\t\t\t\t\t\t\t\t\t\t<li>BAUG</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>f5</td>\n\t\t\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t\t\t<table>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>foo</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>AQID</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>bar</td>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td><null/></td>\n\t\t\t\t\t\t\t\t\t\t\t<td>BAUG</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</table>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG))))",
					/* UonT */		"(f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG))))",
					/* UonR */		"(\n\tf=(\n\t\tfoo=(\n\t\t\tf1=AQID,\n\t\t\tf2=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=AQID,\n\t\t\t\tbar=null,\n\t\t\t\tnull=BAUG\n\t\t\t)\n\t\t),\n\t\tbar=null,\n\t\tnull=(\n\t\t\tf1=AQID,\n\t\t\tf2=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf3=null,\n\t\t\tf4=@(\n\t\t\t\tAQID,\n\t\t\t\tBAUG,\n\t\t\t\tnull\n\t\t\t),\n\t\t\tf5=(\n\t\t\t\tfoo=AQID,\n\t\t\t\tbar=null,\n\t\t\t\tnull=BAUG\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)))",
					/* UrlEncT */	"f=(foo=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)),bar=null,null=(f1=AQID,f2=@(AQID,BAUG,null),f3=null,f4=@(AQID,BAUG,null),f5=(foo=AQID,bar=null,null=BAUG)))",
					/* UrlEncR */	"f=(\n\tfoo=(\n\t\tf1=AQID,\n\t\tf2=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=AQID,\n\t\t\tbar=null,\n\t\t\tnull=BAUG\n\t\t)\n\t),\n\tbar=null,\n\tnull=(\n\t\tf1=AQID,\n\t\tf2=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf3=null,\n\t\tf4=@(\n\t\t\tAQID,\n\t\t\tBAUG,\n\t\t\tnull\n\t\t),\n\t\tf5=(\n\t\t\tfoo=AQID,\n\t\t\tbar=null,\n\t\t\tnull=BAUG\n\t\t)\n\t)\n)",
					/* MsgPack */	"81A16683A3666F6F85A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547A3626172C0C085A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547",
					/* MsgPackT */	"81A16683A3666F6F85A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547A3626172C0C085A26631A441514944A2663293A441514944A442415547C0A26633C0A2663493A441514944A442415547C0A2663583A3666F6FA441514944A3626172C0C0A442415547",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f rdf:parseType='Resource'>\n<jp:foo rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_ rdf:parseType='Resource'>\n<jp:f1>AQID</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f2>\n<jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:f4>\n<rdf:Seq>\n<rdf:li>AQID</rdf:li>\n<rdf:li>BAUG</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:f4>\n<jp:f5 rdf:parseType='Resource'>\n<jp:foo>AQID</jp:foo>\n<jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n<jp:_x0000_>BAUG</jp:_x0000_>\n</jp:f5>\n</jp:_x0000_>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f rdf:parseType='Resource'>\n      <jp:foo rdf:parseType='Resource'>\n        <jp:f1>AQID</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>AQID</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>BAUG</jp:_x0000_>\n        </jp:f5>\n      </jp:foo>\n      <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      <jp:_x0000_ rdf:parseType='Resource'>\n        <jp:f1>AQID</jp:f1>\n        <jp:f2>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f2>\n        <jp:f3 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n        <jp:f4>\n          <rdf:Seq>\n            <rdf:li>AQID</rdf:li>\n            <rdf:li>BAUG</rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </jp:f4>\n        <jp:f5 rdf:parseType='Resource'>\n          <jp:foo>AQID</jp:foo>\n          <jp:bar rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          <jp:_x0000_>BAUG</jp:_x0000_>\n        </jp:f5>\n      </jp:_x0000_>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithByteArrayBeanMapField o) {
						assertType(BeanWithByteArrayBeanMapField.class, o);
					}
				}
			},
		});
	}

	public ByteArrayBase64SwapComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.builder().pojoSwaps(ByteArrayBase64Swap.class).trimNullProperties(false).build();
	}

	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p.builder().pojoSwaps(ByteArrayBase64Swap.class).build();
	}

	public static class BeanWithByteArrayField {
		public byte[] f;
		public BeanWithByteArrayField init() {
			f = new byte[]{1,2,3};
			return this;
		}
	}

	public static class BeanWithByteArray2dField {
		public byte[][] f;
		public BeanWithByteArray2dField init() {
			f = new byte[][]{{1,2,3},{4,5,6},null};
			return this;
		}
	}

	public static class BeanWithByteArrayNullField {
		public byte[] f;
		public BeanWithByteArrayNullField init() {
			f = null;
			return this;
		}
	}

	public static class BeanWithByteArrayListField {
		public List<byte[]> f;
		public BeanWithByteArrayListField init() {
			f = new AList<byte[]>()
				.append(new byte[]{1,2,3})
				.append(new byte[]{4,5,6})
				.append(null)
			;
			return this;
		}
	}

	public static class BeanWithByteArrayMapField {
		public Map<String,byte[]> f;
		public BeanWithByteArrayMapField init() {
			f = new AMap<String,byte[]>()
				.append("foo", new byte[]{1,2,3})
				.append("bar", null)
				.append(null, new byte[]{4,5,6})
			;
			return this;
		}
	}

	public static class BeanWithByteArrayBeanListField {
		public List<B> f;
		public BeanWithByteArrayBeanListField init() {
			f = new AList<B>()
				.append(new B().init())
				.append(null)
			;
			return this;
		}
	}

	public static class BeanWithByteArrayBeanMapField {
		public Map<String,B> f;
		public BeanWithByteArrayBeanMapField init() {
			f = new AMap<String,B>()
				.append("foo", new B().init())
				.append("bar", null)
				.append(null, new B().init())
			;
			return this;
		}
	}

	public static class B {
		public byte[] f1;
		public byte[][] f2;
		public byte[] f3;
		public List<byte[]> f4;
		public Map<String,byte[]> f5;

		public B init() {
			f1 = new byte[]{1,2,3};
			f2 = new byte[][]{{1,2,3},{4,5,6},null};
			f3 = null;
			f4 = new AList<byte[]>()
				.append(new byte[]{1,2,3})
				.append(new byte[]{4,5,6})
				.append(null)
			;
			f5 = new AMap<String,byte[]>()
				.append("foo", new byte[]{1,2,3})
				.append("bar", null)
				.append(null, new byte[]{4,5,6})
			;
			return this;
		}
	}
}
