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

import static org.apache.juneau.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
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
public class BeanDictionaryComboTest extends ComboRoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ 	/* 0 */
				new ComboInput<A>(
					"A",
					A.class,
					new A().init(),
					/* Json */		"{_type:'A',a:1}",
					/* JsonT */		"{t:'A',a:1}",
					/* JsonR */		"{\n\t_type: 'A',\n\ta: 1\n}",
					/* Xml */		"<A><a>1</a></A>",
					/* XmlT */		"<A><a>1</a></A>",
					/* XmlR */		"<A>\n\t<a>1</a>\n</A>\n",
					/* XmlNs */		"<A><a>1</a></A>",
					/* Html */		"<table _type='A'><tr><td>a</td><td>1</td></tr></table>",
					/* HtmlT */		"<table t='A'><tr><td>a</td><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='A'>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(_type=A,a=1)",
					/* UonT */		"(t=A,a=1)",
					/* UonR */		"(\n\t_type=A,\n\ta=1\n)",
					/* UrlEnc */	"_type=A&a=1",
					/* UrlEncT */	"t=A&a=1",
					/* UrlEncR */	"_type=A\n&a=1",
					/* MsgPack */	"82A55F74797065A141A16101",
					/* MsgPackT */	"82A174A141A16101",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>A</jp:_type>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A o) {
						assertType(A.class, o);
					}
				}
			},
			{	/* 1 */
				new ComboInput<A[]>(
					"A[]",
					A[].class,
					new A[]{new A().init()},
					/* Json */		"[{_type:'A',a:1}]",
					/* JsonT */		"[{t:'A',a:1}]",
					/* JsonR */		"[\n\t{\n\t\t_type: 'A',\n\t\ta: 1\n\t}\n]",
					/* Xml */		"<array><A><a>1</a></A></array>",
					/* XmlT */		"<array><A><a>1</a></A></array>",
					/* XmlR */		"<array>\n\t<A>\n\t\t<a>1</a>\n\t</A>\n</array>\n",
					/* XmlNs */		"<array><A><a>1</a></A></array>",
					/* Html */		"<table _type='array'><tr><th>a</th></tr><tr _type='A'><td>1</td></tr></table>",
					/* HtmlT */		"<table t='array'><tr><th>a</th></tr><tr t='A'><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t</tr>\n\t<tr _type='A'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"@((_type=A,a=1))",
					/* UonT */		"@((t=A,a=1))",
					/* UonR */		"@(\n\t(\n\t\t_type=A,\n\t\ta=1\n\t)\n)",
					/* UrlEnc */	"0=(_type=A,a=1)",
					/* UrlEncT */	"0=(t=A,a=1)",
					/* UrlEncR */	"0=(\n\t_type=A,\n\ta=1\n)",
					/* MsgPack */	"9182A55F74797065A141A16101",
					/* MsgPackT */	"9182A174A141A16101",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>A</jp:_type>\n      <jp:a>1</jp:a>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A[] o) {
						assertType(A.class, o[0]);
					}
				}
			},
			{	/* 2 */
				new ComboInput<A[][][]>(
					"A[][][]",
					A[][][].class,
					new A[][][]{{{new A().init(),null},null},null},
					/* Json */		"[[[{_type:'A',a:1},null],null],null]",
					/* JsonT */		"[[[{t:'A',a:1},null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'A',\n\t\t\t\ta: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<A>\n\t\t\t\t<a>1</a>\n\t\t\t</A>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@((_type=A,a=1),null),null),null)",
					/* UonT */		"@(@(@((t=A,a=1),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@((_type=A,a=1),null),null)&1=null",
					/* UrlEncT */	"0=@(@((t=A,a=1),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t(\n\t\t\t_type=A,\n\t\t\ta=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"92929282A55F74797065A141A16101C0C0C0",
					/* MsgPackT */	"92929282A174A141A16101C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>A</jp:_type>\n              <jp:a>1</jp:a>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A[][][] o) {
						assertType(A.class, o[0][0][0]);
					}
				}
			},
			{	/* 3 */
				new ComboInput<List<A[][][]>>(
					"List<A[][][]>",
					getType(List.class, A[][][].class),
					new AList<A[][][]>().append(new A[][][]{{{new A().init(),null},null},null}).append(null),
					/* Json */		"[[[[{_type:'A',a:1},null],null],null],null]",
					/* JsonT */		"[[[[{t:'A',a:1},null],null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@(@((_type=A,a=1),null),null),null),null)",
					/* UonT */		"@(@(@(@((t=A,a=1),null),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@(@((_type=A,a=1),null),null),null)&1=null",
					/* UrlEncT */	"0=@(@(@((t=A,a=1),null),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"9292929282A55F74797065A141A16101C0C0C0C0",
					/* MsgPackT */	"9292929282A174A141A16101C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<A[][][]> o) {
						assertType(A.class, o.get(0)[0][0][0]);
					}
				}
			},
			{	/* 4 */
				new ComboInput<Map<String,A[][][]>>(
					"Map<String,A[][][]>",
					getType(Map.class,String.class,A[][][].class),
					new AMap<String,A[][][]>().append("x", new A[][][]{{{new A().init(),null},null},null}),
					/* Json */		"{x:[[[{_type:'A',a:1},null],null],null]}",
					/* JsonT */		"{x:[[[{t:'A',a:1},null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@((_type=A,a=1),null),null),null))",
					/* UonT */		"(x=@(@(@((t=A,a=1),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@((_type=A,a=1),null),null),null)",
					/* UrlEncT */	"x=@(@(@((t=A,a=1),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A17892929282A55F74797065A141A16101C0C0C0",
					/* MsgPackT */	"81A17892929282A174A141A16101C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,A[][][]> o) {
						assertType(A.class, o.get("x")[0][0][0]);
					}
				}
			},
			{	/* 5 */
				new ComboInput<Map<String,List<A[][][]>>>(
					"Map<String,List<A[][][]>>",
					getType(Map.class,String.class,List.class,A[][][].class),
					new AMap<String,List<A[][][]>>().append("x", new AList<A[][][]>().append(new A[][][]{{{new A().init(),null},null},null}).append(null)),
					/* Json */		"{x:[[[[{_type:'A',a:1},null],null],null],null]}",
					/* JsonT */		"{x:[[[[{t:'A',a:1},null],null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\t_type: 'A',\n\t\t\t\t\t\ta: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<A>\n\t\t\t\t\t\t<a>1</a>\n\t\t\t\t\t</A>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@(@((_type=A,a=1),null),null),null),null))",
					/* UonT */		"(x=@(@(@(@((t=A,a=1),null),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=A,\n\t\t\t\t\t\ta=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@(@((_type=A,a=1),null),null),null),null)",
					/* UrlEncT */	"x=@(@(@(@((t=A,a=1),null),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A1789292929282A55F74797065A141A16101C0C0C0C0",
					/* MsgPackT */	"81A1789292929282A174A141A16101C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>A</jp:_type>\n                      <jp:a>1</jp:a>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,List<A[][][]>> o) {
						assertType(A.class, o.get("x").get(0)[0][0][0]);
					}
				}
			},
			{	/* 6 */
				new ComboInput<IA>(
					"IA",
					IA.class,
					new A().init(),
					/* Json */		"{_type:'A',a:1}",
					/* JsonT */		"{t:'A',a:1}",
					/* JsonR */		"{\n\t_type: 'A',\n\ta: 1\n}",
					/* Xml */		"<A><a>1</a></A>",
					/* XmlT */		"<A><a>1</a></A>",
					/* XmlR */		"<A>\n\t<a>1</a>\n</A>\n",
					/* XmlNs */		"<A><a>1</a></A>",
					/* Html */		"<table _type='A'><tr><td>a</td><td>1</td></tr></table>",
					/* HtmlT */		"<table t='A'><tr><td>a</td><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='A'>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(_type=A,a=1)",
					/* UonT */		"(t=A,a=1)",
					/* UonR */		"(\n\t_type=A,\n\ta=1\n)",
					/* UrlEnc */	"_type=A&a=1",
					/* UrlEncT */	"t=A&a=1",
					/* UrlEncR */	"_type=A\n&a=1",
					/* MsgPack */	"82A55F74797065A141A16101",
					/* MsgPackT */	"82A174A141A16101",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>A</jp:_type>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(IA o) {
						assertType(A.class, o);
					}
				}
			},
			{	/* 7 */
				new ComboInput<IA[]>(
					"IA[]",
					IA[].class,
					new IA[]{new A().init()},
					/* Json */		"[{_type:'A',a:1}]",
					/* JsonT */		"[{t:'A',a:1}]",
					/* JsonR */		"[\n\t{\n\t\t_type: 'A',\n\t\ta: 1\n\t}\n]",
					/* Xml */		"<array><A><a>1</a></A></array>",
					/* XmlT */		"<array><A><a>1</a></A></array>",
					/* XmlR */		"<array>\n\t<A>\n\t\t<a>1</a>\n\t</A>\n</array>\n",
					/* XmlNs */		"<array><A><a>1</a></A></array>",
					/* Html */		"<table _type='array'><tr><th>a</th></tr><tr _type='A'><td>1</td></tr></table>",
					/* HtmlT */		"<table t='array'><tr><th>a</th></tr><tr t='A'><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t</tr>\n\t<tr _type='A'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"@((_type=A,a=1))",
					/* UonT */		"@((t=A,a=1))",
					/* UonR */		"@(\n\t(\n\t\t_type=A,\n\t\ta=1\n\t)\n)",
					/* UrlEnc */	"0=(_type=A,a=1)",
					/* UrlEncT */	"0=(t=A,a=1)",
					/* UrlEncR */	"0=(\n\t_type=A,\n\ta=1\n)",
					/* MsgPack */	"9182A55F74797065A141A16101",
					/* MsgPackT */	"9182A174A141A16101",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>A</jp:_type>\n      <jp:a>1</jp:a>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(IA[] o) {
						assertType(A.class, o[0]);
					}
				}
			},
			{	/* 8 */
				new ComboInput<IA[][][]>(
					"IA[][][]",
					IA[][][].class,
					new IA[][][]{{{new A().init(),null},null},null},
					/* Json */		"[[[{_type:'A',a:1},null],null],null]",
					/* JsonT */		"[[[{t:'A',a:1},null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'A',\n\t\t\t\ta: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<A>\n\t\t\t\t<a>1</a>\n\t\t\t</A>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@((_type=A,a=1),null),null),null)",
					/* UonT */		"@(@(@((t=A,a=1),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@((_type=A,a=1),null),null)&1=null",
					/* UrlEncT */	"0=@(@((t=A,a=1),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t(\n\t\t\t_type=A,\n\t\t\ta=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"92929282A55F74797065A141A16101C0C0C0",
					/* MsgPackT */	"92929282A174A141A16101C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>A</jp:_type>\n              <jp:a>1</jp:a>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(IA[][][] o) {
						assertType(A.class, o[0][0][0]);
					}
				}
			},
			{	/* 9 */
				new ComboInput<List<IA[][][]>>(
					"List<IA[][][]>",
					getType(List.class,IA[][][].class),
					new AList<IA[][][]>().append(new IA[][][]{{{new A().init(),null},null},null}).append(null),
					/* Json */		"[[[[{_type:'A',a:1},null],null],null],null]",
					/* JsonT */		"[[[[{t:'A',a:1},null],null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@(@((_type=A,a=1),null),null),null),null)",
					/* UonT */		"@(@(@(@((t=A,a=1),null),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@(@((_type=A,a=1),null),null),null)&1=null",
					/* UrlEncT */	"0=@(@(@((t=A,a=1),null),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"9292929282A55F74797065A141A16101C0C0C0C0",
					/* MsgPackT */	"9292929282A174A141A16101C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<IA[][][]> o) {
						assertType(A.class, o.get(0)[0][0][0]);
					}
				}
			},
			{	/* 10 */
				new ComboInput<Map<String,IA[][][]>>(
					"Map<String,IA[][][]>",
					getType(Map.class,String.class,IA[][][].class),
					new AMap<String,IA[][][]>().append("x", new IA[][][]{{{new A().init(),null},null},null}),
					/* Json */		"{x:[[[{_type:'A',a:1},null],null],null]}",
					/* JsonT */		"{x:[[[{t:'A',a:1},null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@((_type=A,a=1),null),null),null))",
					/* UonT */		"(x=@(@(@((t=A,a=1),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@((_type=A,a=1),null),null),null)",
					/* UrlEncT */	"x=@(@(@((t=A,a=1),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A17892929282A55F74797065A141A16101C0C0C0",
					/* MsgPackT */	"81A17892929282A174A141A16101C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,IA[][][]> o) {
						assertType(A.class, o.get("x")[0][0][0]);
					}
				}
			},
			{	/* 11 */
				new ComboInput<Map<String,List<IA[][][]>>>(
					"Map<String,List<IA[][][]>>",
					getType(Map.class,String.class,List.class,IA[][][].class),
					new AMap<String,List<IA[][][]>>().append("x", new AList<IA[][][]>().append(new IA[][][]{{{new A().init(),null},null},null}).append(null)),
					/* Json */		"{x:[[[[{_type:'A',a:1},null],null],null],null]}",
					/* JsonT */		"{x:[[[[{t:'A',a:1},null],null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\t_type: 'A',\n\t\t\t\t\t\ta: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<A>\n\t\t\t\t\t\t<a>1</a>\n\t\t\t\t\t</A>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@(@((_type=A,a=1),null),null),null),null))",
					/* UonT */		"(x=@(@(@(@((t=A,a=1),null),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=A,\n\t\t\t\t\t\ta=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@(@((_type=A,a=1),null),null),null),null)",
					/* UrlEncT */	"x=@(@(@(@((t=A,a=1),null),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A1789292929282A55F74797065A141A16101C0C0C0C0",
					/* MsgPackT */	"81A1789292929282A174A141A16101C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>A</jp:_type>\n                      <jp:a>1</jp:a>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,List<IA[][][]>> o) {
						assertType(A.class, o.get("x").get(0)[0][0][0]);
					}
				}
			},
			{	/* 12 */
				new ComboInput<B>(
					"B",
					B.class,
					new B().init(),
					/* Json */		"{z:'B',b:1}",
					/* JsonT */		"{z:'B',b:1}",
					/* JsonR */		"{\n\tz: 'B',\n\tb: 1\n}",
					/* Xml */		"<B><b>1</b></B>",
					/* XmlT */		"<B><b>1</b></B>",
					/* XmlR */		"<B>\n\t<b>1</b>\n</B>\n",
					/* XmlNs */		"<B><b>1</b></B>",
					/* Html */		"<table z='B'><tr><td>b</td><td>1</td></tr></table>",
					/* HtmlT */		"<table z='B'><tr><td>b</td><td>1</td></tr></table>",
					/* HtmlR */		"<table z='B'>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(z=B,b=1)",
					/* UonT */		"(z=B,b=1)",
					/* UonR */		"(\n\tz=B,\n\tb=1\n)",
					/* UrlEnc */	"z=B&b=1",
					/* UrlEncT */	"z=B&b=1",
					/* UrlEncR */	"z=B\n&b=1",
					/* MsgPack */	"82A17AA142A16201",
					/* MsgPackT */	"82A17AA142A16201",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:z>B</jp:z>\n    <jp:b>1</jp:b>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(B o) {
						assertType(B.class, o);
					}
				}
			},
			{	/* 13 */
				new ComboInput<B[]>(
					"B[]",
					B[].class,
					new B[]{new B().init()},
					/* Json */		"[{z:'B',b:1}]",
					/* JsonT */		"[{z:'B',b:1}]",
					/* JsonR */		"[\n\t{\n\t\tz: 'B',\n\t\tb: 1\n\t}\n]",
					/* Xml */		"<array><B><b>1</b></B></array>",
					/* XmlT */		"<array><B><b>1</b></B></array>",
					/* XmlR */		"<array>\n\t<B>\n\t\t<b>1</b>\n\t</B>\n</array>\n",
					/* XmlNs */		"<array><B><b>1</b></B></array>",
					/* Html */		"<table _type='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
					/* HtmlT */		"<table t='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>b</th>\n\t</tr>\n\t<tr z='B'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"@((z=B,b=1))",
					/* UonT */		"@((z=B,b=1))",
					/* UonR */		"@(\n\t(\n\t\tz=B,\n\t\tb=1\n\t)\n)",
					/* UrlEnc */	"0=(z=B,b=1)",
					/* UrlEncT */	"0=(z=B,b=1)",
					/* UrlEncR */	"0=(\n\tz=B,\n\tb=1\n)",
					/* MsgPack */	"9182A17AA142A16201",
					/* MsgPackT */	"9182A17AA142A16201",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:z>B</jp:z>\n      <jp:b>1</jp:b>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(B[] o) {
						assertType(B.class, o[0]);
					}
				}
			},
			{	/* 14 */
				new ComboInput<B[][][]>(
					"B[][][]",
					B[][][].class,
					new B[][][]{{{new B().init(),null},null},null},
					/* Json */		"[[[{z:'B',b:1},null],null],null]",
					/* JsonT */		"[[[{z:'B',b:1},null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\tz: 'B',\n\t\t\t\tb: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<B>\n\t\t\t\t<b>1</b>\n\t\t\t</B>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@((z=B,b=1),null),null),null)",
					/* UonT */		"@(@(@((z=B,b=1),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@((z=B,b=1),null),null)&1=null",
					/* UrlEncT */	"0=@(@((z=B,b=1),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t(\n\t\t\tz=B,\n\t\t\tb=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"92929282A17AA142A16201C0C0C0",
					/* MsgPackT */	"92929282A17AA142A16201C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:z>B</jp:z>\n              <jp:b>1</jp:b>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(B[][][] o) {
						assertType(B.class, o[0][0][0]);
					}
				}
			},
			{	/* 15 */
				new ComboInput<List<B[][][]>>(
					"List<B[][][]>",
					getType(List.class, B[][][].class),
					new AList<B[][][]>().append(new B[][][]{{{new B().init(),null},null},null}).append(null),
					/* Json */		"[[[[{z:'B',b:1},null],null],null],null]",
					/* JsonT */		"[[[[{z:'B',b:1},null],null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UonT */		"@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@(@((z=B,b=1),null),null),null)&1=null",
					/* UrlEncT */	"0=@(@(@((z=B,b=1),null),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"9292929282A17AA142A16201C0C0C0C0",
					/* MsgPackT */	"9292929282A17AA142A16201C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<B[][][]> o) {
						assertType(B.class, o.get(0)[0][0][0]);
					}
				}
			},
			{	/* 16 */
				new ComboInput<Map<String,B[][][]>>(
					"Map<String,B[][][]>",
					getType(Map.class,String.class,B[][][].class),
					new AMap<String,B[][][]>().append("x", new B[][][]{{{new B().init(),null},null},null}),
					/* Json */		"{x:[[[{z:'B',b:1},null],null],null]}",
					/* JsonT */		"{x:[[[{z:'B',b:1},null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@((z=B,b=1),null),null),null))",
					/* UonT */		"(x=@(@(@((z=B,b=1),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@((z=B,b=1),null),null),null)",
					/* UrlEncT */	"x=@(@(@((z=B,b=1),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A17892929282A17AA142A16201C0C0C0",
					/* MsgPackT */	"81A17892929282A17AA142A16201C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,B[][][]> o) {
						assertType(B.class, o.get("x")[0][0][0]);
					}
				}
			},
			{	/* 17 */
				new ComboInput<Map<String,List<B[][][]>>>(
					"Map<String,List<B[][][]>>",
					getType(Map.class,String.class,List.class,B[][][].class),
					new AMap<String,List<B[][][]>>().append("x", new AList<B[][][]>().append(new B[][][]{{{new B().init(),null},null},null}).append(null)),
					/* Json */		"{x:[[[[{z:'B',b:1},null],null],null],null]}",
					/* JsonT */		"{x:[[[[{z:'B',b:1},null],null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\tz: 'B',\n\t\t\t\t\t\tb: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<B>\n\t\t\t\t\t\t<b>1</b>\n\t\t\t\t\t</B>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@(@((z=B,b=1),null),null),null),null))",
					/* UonT */		"(x=@(@(@(@((z=B,b=1),null),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\tz=B,\n\t\t\t\t\t\tb=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UrlEncT */	"x=@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A1789292929282A17AA142A16201C0C0C0C0",
					/* MsgPackT */	"81A1789292929282A17AA142A16201C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:z>B</jp:z>\n                      <jp:b>1</jp:b>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,List<B[][][]>> o) {
						assertType(B.class, o.get("x").get(0)[0][0][0]);
					}
				}
			},
			{	/* 18 */
				new ComboInput<IB>(
					"IB",
					IB.class,
					new B().init(),
					/* Json */		"{z:'B',b:1}",
					/* JsonT */		"{z:'B',b:1}",
					/* JsonR */		"{\n\tz: 'B',\n\tb: 1\n}",
					/* Xml */		"<B><b>1</b></B>",
					/* XmlT */		"<B><b>1</b></B>",
					/* XmlR */		"<B>\n\t<b>1</b>\n</B>\n",
					/* XmlNs */		"<B><b>1</b></B>",
					/* Html */		"<table z='B'><tr><td>b</td><td>1</td></tr></table>",
					/* HtmlT */		"<table z='B'><tr><td>b</td><td>1</td></tr></table>",
					/* HtmlR */		"<table z='B'>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(z=B,b=1)",
					/* UonT */		"(z=B,b=1)",
					/* UonR */		"(\n\tz=B,\n\tb=1\n)",
					/* UrlEnc */	"z=B&b=1",
					/* UrlEncT */	"z=B&b=1",
					/* UrlEncR */	"z=B\n&b=1",
					/* MsgPack */	"82A17AA142A16201",
					/* MsgPackT */	"82A17AA142A16201",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:z>B</jp:z>\n    <jp:b>1</jp:b>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(IB o) {
						assertType(B.class, o);
					}
				}
			},
			{	/* 19 */
				new ComboInput<IB[]>(
					"IB[]",
					IB[].class,
					new IB[]{new B().init()},
					/* Json */		"[{z:'B',b:1}]",
					/* JsonT */		"[{z:'B',b:1}]",
					/* JsonR */		"[\n\t{\n\t\tz: 'B',\n\t\tb: 1\n\t}\n]",
					/* Xml */		"<array><B><b>1</b></B></array>",
					/* XmlT */		"<array><B><b>1</b></B></array>",
					/* XmlR */		"<array>\n\t<B>\n\t\t<b>1</b>\n\t</B>\n</array>\n",
					/* XmlNs */		"<array><B><b>1</b></B></array>",
					/* Html */		"<table _type='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
					/* HtmlT */		"<table t='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>",
					/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>b</th>\n\t</tr>\n\t<tr z='B'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n",
					/* Uon */		"@((z=B,b=1))",
					/* UonT */		"@((z=B,b=1))",
					/* UonR */		"@(\n\t(\n\t\tz=B,\n\t\tb=1\n\t)\n)",
					/* UrlEnc */	"0=(z=B,b=1)",
					/* UrlEncT */	"0=(z=B,b=1)",
					/* UrlEncR */	"0=(\n\tz=B,\n\tb=1\n)",
					/* MsgPack */	"9182A17AA142A16201",
					/* MsgPackT */	"9182A17AA142A16201",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:z>B</jp:z>\n      <jp:b>1</jp:b>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(IB[] o) {
						assertType(B.class, o[0]);
					}
				}
			},
			{	/* 20 */
				new ComboInput<IB[][][]>(
					"IB[][][]",
					IB[][][].class,
					new IB[][][]{{{new B().init(),null},null},null},
					/* Json */		"[[[{z:'B',b:1},null],null],null]",
					/* JsonT */		"[[[{z:'B',b:1},null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\tz: 'B',\n\t\t\t\tb: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<B>\n\t\t\t\t<b>1</b>\n\t\t\t</B>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@((z=B,b=1),null),null),null)",
					/* UonT */		"@(@(@((z=B,b=1),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@((z=B,b=1),null),null)&1=null",
					/* UrlEncT */	"0=@(@((z=B,b=1),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t(\n\t\t\tz=B,\n\t\t\tb=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"92929282A17AA142A16201C0C0C0",
					/* MsgPackT */	"92929282A17AA142A16201C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:z>B</jp:z>\n              <jp:b>1</jp:b>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(IB[][][] o) {
						assertType(B.class, o[0][0][0]);
					}
				}
			},
			{	/* 21 */
				new ComboInput<List<IB[][][]>>(
					"List<IB[][][]>",
					getType(List.class,IB[][][].class),
					new AList<IB[][][]>().append(new IB[][][]{{{new B().init(),null},null},null}).append(null),
					/* Json */		"[[[[{z:'B',b:1},null],null],null],null]",
					/* JsonT */		"[[[[{z:'B',b:1},null],null],null],null]",
					/* JsonR */		"[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]",
					/* Xml */		"<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>",
					/* XmlT */		"<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n",
					/* XmlNs */		"<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>",
					/* Html */		"<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n",
					/* Uon */		"@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UonT */		"@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UonR */		"@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* UrlEnc */	"0=@(@(@((z=B,b=1),null),null),null)&1=null",
					/* UrlEncT */	"0=@(@(@((z=B,b=1),null),null),null)&1=null",
					/* UrlEncR */	"0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null",
					/* MsgPack */	"9292929282A17AA142A16201C0C0C0C0",
					/* MsgPackT */	"9292929282A17AA142A16201C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<IB[][][]> o) {
						assertType(B.class, o.get(0)[0][0][0]);
					}
				}
			},
			{	/* 22 */
				new ComboInput<Map<String,IB[][][]>>(
					"Map<String,IB[][][]>",
					getType(Map.class,String.class,IB[][][].class),
					new AMap<String,IB[][][]>().append("x", new IB[][][]{{{new B().init(),null},null},null}),
					/* Json */		"{x:[[[{z:'B',b:1},null],null],null]}",
					/* JsonT */		"{x:[[[{z:'B',b:1},null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@((z=B,b=1),null),null),null))",
					/* UonT */		"(x=@(@(@((z=B,b=1),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@((z=B,b=1),null),null),null)",
					/* UrlEncT */	"x=@(@(@((z=B,b=1),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A17892929282A17AA142A16201C0C0C0",
					/* MsgPackT */	"81A17892929282A17AA142A16201C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,IB[][][]> o) {
						assertType(B.class, o.get("x")[0][0][0]);
					}
				}
			},
			{	/* 23 */
				new ComboInput<Map<String,List<IB[][][]>>>(
					"Map<String,List<IB[][][]>>",
					getType(Map.class,String.class,List.class,IB[][][].class),
					new AMap<String,List<IB[][][]>>().append("x", new AList<IB[][][]>().append(new IB[][][]{{{new B().init(),null},null},null}).append(null)),
					/* Json */		"{x:[[[[{z:'B',b:1},null],null],null],null]}",
					/* JsonT */		"{x:[[[[{z:'B',b:1},null],null],null],null]}",
					/* JsonR */		"{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\tz: 'B',\n\t\t\t\t\t\tb: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}",
					/* Xml */		"<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlT */		"<object><x t='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>",
					/* XmlR */		"<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<B>\n\t\t\t\t\t\t<b>1</b>\n\t\t\t\t\t</B>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n",
					/* XmlNs */		"<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>",
					/* Html */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(x=@(@(@(@((z=B,b=1),null),null),null),null))",
					/* UonT */		"(x=@(@(@(@((z=B,b=1),null),null),null),null))",
					/* UonR */		"(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\tz=B,\n\t\t\t\t\t\tb=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)",
					/* UrlEnc */	"x=@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UrlEncT */	"x=@(@(@(@((z=B,b=1),null),null),null),null)",
					/* UrlEncR */	"x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)",
					/* MsgPack */	"81A1789292929282A17AA142A16201C0C0C0C0",
					/* MsgPackT */	"81A1789292929282A17AA142A16201C0C0C0C0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:z>B</jp:z>\n                      <jp:b>1</jp:b>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map<String,List<IB[][][]>> o) {
						assertType(B.class, o.get("x").get(0)[0][0][0]);
					}
				}
			},
		});
	}

	public BeanDictionaryComboTest(ComboInput<?> comboInput) {
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

	@Bean(beanDictionary={A.class})
	public static interface IA {}

	@Bean(typeName="A")
	public static class A implements IA {
		public int a;

		public A init() {
			a = 1;
			return this;
		}
	}

	@Bean(beanDictionary={B.class}, typePropertyName="z")
	public static interface IB {}

	@Bean(typeName="B")
	public static class B implements IB {
		public int b;

		public B init() {
			b = 1;
			return this;
		}
	}
}
