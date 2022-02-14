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

import static org.apache.juneau.assertions.Verify.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.runner.*;
import org.junit.runners.*;

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
					new A().init()
				)
				.json("{_type:'A',a:1}")
				.jsonT("{t:'A',a:1}")
				.jsonR("{\n\t_type: 'A',\n\ta: 1\n}")
				.xml("<A><a>1</a></A>")
				.xmlT("<A><a>1</a></A>")
				.xmlR("<A>\n\t<a>1</a>\n</A>\n")
				.xmlNs("<A><a>1</a></A>")
				.html("<table _type='A'><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table t='A'><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table _type='A'>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(_type=A,a=1)")
				.uonT("(t=A,a=1)")
				.uonR("(\n\t_type=A,\n\ta=1\n)")
				.urlEnc("_type=A&a=1")
				.urlEncT("t=A&a=1")
				.urlEncR("_type=A\n&a=1")
				.msgPack("82A55F74797065A141A16101")
				.msgPackT("82A174A141A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>A</jp:_type>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 1 */
				new ComboInput<A[]>(
					"A[]",
					A[].class,
					new A[]{new A().init()}
				)
				.json("[{_type:'A',a:1}]")
				.jsonT("[{t:'A',a:1}]")
				.jsonR("[\n\t{\n\t\t_type: 'A',\n\t\ta: 1\n\t}\n]")
				.xml("<array><A><a>1</a></A></array>")
				.xmlT("<array><A><a>1</a></A></array>")
				.xmlR("<array>\n\t<A>\n\t\t<a>1</a>\n\t</A>\n</array>\n")
				.xmlNs("<array><A><a>1</a></A></array>")
				.html("<table _type='array'><tr><th>a</th></tr><tr _type='A'><td>1</td></tr></table>")
				.htmlT("<table t='array'><tr><th>a</th></tr><tr t='A'><td>1</td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t</tr>\n\t<tr _type='A'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("@((_type=A,a=1))")
				.uonT("@((t=A,a=1))")
				.uonR("@(\n\t(\n\t\t_type=A,\n\t\ta=1\n\t)\n)")
				.urlEnc("0=(_type=A,a=1)")
				.urlEncT("0=(t=A,a=1)")
				.urlEncR("0=(\n\t_type=A,\n\ta=1\n)")
				.msgPack("9182A55F74797065A141A16101")
				.msgPackT("9182A174A141A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>A</jp:_type>\n      <jp:a>1</jp:a>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0]).isType(A.class))
			},
			{	/* 2 */
				new ComboInput<A[][][]>(
					"A[][][]",
					A[][][].class,
					new A[][][]{{{new A().init(),null},null},null}
				)
				.json("[[[{_type:'A',a:1},null],null],null]")
				.jsonT("[[[{t:'A',a:1},null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'A',\n\t\t\t\ta: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<A>\n\t\t\t\t<a>1</a>\n\t\t\t</A>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@((_type=A,a=1),null),null),null)")
				.uonT("@(@(@((t=A,a=1),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@((_type=A,a=1),null),null)&1=null")
				.urlEncT("0=@(@((t=A,a=1),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t(\n\t\t\t_type=A,\n\t\t\ta=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("92929282A55F74797065A141A16101C0C0C0")
				.msgPackT("92929282A174A141A16101C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>A</jp:_type>\n              <jp:a>1</jp:a>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0][0][0]).isType(A.class))
			},
			{	/* 3 */
				new ComboInput<List<A[][][]>>(
					"List<A[][][]>",
					getType(List.class, A[][][].class),
					list(new A[][][]{{{new A().init(),null},null},null},null)
				)
				.json("[[[[{_type:'A',a:1},null],null],null],null]")
				.jsonT("[[[[{t:'A',a:1},null],null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@(@((_type=A,a=1),null),null),null),null)")
				.uonT("@(@(@(@((t=A,a=1),null),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@(@((_type=A,a=1),null),null),null)&1=null")
				.urlEncT("0=@(@(@((t=A,a=1),null),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("9292929282A55F74797065A141A16101C0C0C0C0")
				.msgPackT("9292929282A174A141A16101C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get(0)[0][0][0]).isType(A.class))
			},
			{	/* 4 */
				new ComboInput<Map<String,A[][][]>>(
					"Map<String,A[][][]>",
					getType(Map.class,String.class,A[][][].class),
					map("x", new A[][][]{{{new A().init(),null},null},null})
				)
				.json("{x:[[[{_type:'A',a:1},null],null],null]}")
				.jsonT("{x:[[[{t:'A',a:1},null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@((_type=A,a=1),null),null),null))")
				.uonT("(x=@(@(@((t=A,a=1),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@((_type=A,a=1),null),null),null)")
				.urlEncT("x=@(@(@((t=A,a=1),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A17892929282A55F74797065A141A16101C0C0C0")
				.msgPackT("81A17892929282A174A141A16101C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x")[0][0][0]).isType(A.class))
			},
			{	/* 5 */
				new ComboInput<Map<String,List<A[][][]>>>(
					"Map<String,List<A[][][]>>",
					getType(Map.class,String.class,List.class,A[][][].class),
					map("x",list(new A[][][]{{{new A().init(),null},null},null},null))
				)
				.json("{x:[[[[{_type:'A',a:1},null],null],null],null]}")
				.jsonT("{x:[[[[{t:'A',a:1},null],null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\t_type: 'A',\n\t\t\t\t\t\ta: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<A>\n\t\t\t\t\t\t<a>1</a>\n\t\t\t\t\t</A>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@(@((_type=A,a=1),null),null),null),null))")
				.uonT("(x=@(@(@(@((t=A,a=1),null),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=A,\n\t\t\t\t\t\ta=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@(@((_type=A,a=1),null),null),null),null)")
				.urlEncT("x=@(@(@(@((t=A,a=1),null),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A1789292929282A55F74797065A141A16101C0C0C0C0")
				.msgPackT("81A1789292929282A174A141A16101C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>A</jp:_type>\n                      <jp:a>1</jp:a>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				.verify(x -> verify(x.get("x").get(0)[0][0][0]).isType(A.class))
			},
			{	/* 6 */
				new ComboInput<IA>(
					"IA",
					IA.class,
					new A().init()
				)
				.json("{_type:'A',a:1}")
				.jsonT("{t:'A',a:1}")
				.jsonR("{\n\t_type: 'A',\n\ta: 1\n}")
				.xml("<A><a>1</a></A>")
				.xmlT("<A><a>1</a></A>")
				.xmlR("<A>\n\t<a>1</a>\n</A>\n")
				.xmlNs("<A><a>1</a></A>")
				.html("<table _type='A'><tr><td>a</td><td>1</td></tr></table>")
				.htmlT("<table t='A'><tr><td>a</td><td>1</td></tr></table>")
				.htmlR("<table _type='A'>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(_type=A,a=1)")
				.uonT("(t=A,a=1)")
				.uonR("(\n\t_type=A,\n\ta=1\n)")
				.urlEnc("_type=A&a=1")
				.urlEncT("t=A&a=1")
				.urlEncR("_type=A\n&a=1")
				.msgPack("82A55F74797065A141A16101")
				.msgPackT("82A174A141A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>A</jp:_type>\n    <jp:a>1</jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 7 */
				new ComboInput<IA[]>(
					"IA[]",
					IA[].class,
					new IA[]{new A().init()}
				)
				.json("[{_type:'A',a:1}]")
				.jsonT("[{t:'A',a:1}]")
				.jsonR("[\n\t{\n\t\t_type: 'A',\n\t\ta: 1\n\t}\n]")
				.xml("<array><A><a>1</a></A></array>")
				.xmlT("<array><A><a>1</a></A></array>")
				.xmlR("<array>\n\t<A>\n\t\t<a>1</a>\n\t</A>\n</array>\n")
				.xmlNs("<array><A><a>1</a></A></array>")
				.html("<table _type='array'><tr><th>a</th></tr><tr _type='A'><td>1</td></tr></table>")
				.htmlT("<table t='array'><tr><th>a</th></tr><tr t='A'><td>1</td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t</tr>\n\t<tr _type='A'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("@((_type=A,a=1))")
				.uonT("@((t=A,a=1))")
				.uonR("@(\n\t(\n\t\t_type=A,\n\t\ta=1\n\t)\n)")
				.urlEnc("0=(_type=A,a=1)")
				.urlEncT("0=(t=A,a=1)")
				.urlEncR("0=(\n\t_type=A,\n\ta=1\n)")
				.msgPack("9182A55F74797065A141A16101")
				.msgPackT("9182A174A141A16101")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>A</jp:_type>\n      <jp:a>1</jp:a>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0]).isType(A.class))
			},
			{	/* 8 */
				new ComboInput<IA[][][]>(
					"IA[][][]",
					IA[][][].class,
					new IA[][][]{{{new A().init(),null},null},null}
				)
				.json("[[[{_type:'A',a:1},null],null],null]")
				.jsonT("[[[{t:'A',a:1},null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'A',\n\t\t\t\ta: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<A>\n\t\t\t\t<a>1</a>\n\t\t\t</A>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@((_type=A,a=1),null),null),null)")
				.uonT("@(@(@((t=A,a=1),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@((_type=A,a=1),null),null)&1=null")
				.urlEncT("0=@(@((t=A,a=1),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t(\n\t\t\t_type=A,\n\t\t\ta=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("92929282A55F74797065A141A16101C0C0C0")
				.msgPackT("92929282A174A141A16101C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>A</jp:_type>\n              <jp:a>1</jp:a>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0][0][0]).isType(A.class))
			},
			{	/* 9 */
				new ComboInput<List<IA[][][]>>(
					"List<IA[][][]>",
					getType(List.class,IA[][][].class),
					list(new IA[][][]{{{new A().init(),null},null},null},null)
				)
				.json("[[[[{_type:'A',a:1},null],null],null],null]")
				.jsonT("[[[[{t:'A',a:1},null],null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@(@((_type=A,a=1),null),null),null),null)")
				.uonT("@(@(@(@((t=A,a=1),null),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@(@((_type=A,a=1),null),null),null)&1=null")
				.urlEncT("0=@(@(@((t=A,a=1),null),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("9292929282A55F74797065A141A16101C0C0C0C0")
				.msgPackT("9292929282A174A141A16101C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get(0)[0][0][0]).isType(A.class))
			},
			{	/* 10 */
				new ComboInput<Map<String,IA[][][]>>(
					"Map<String,IA[][][]>",
					getType(Map.class,String.class,IA[][][].class),
					map("x",new IA[][][]{{{new A().init(),null},null},null})
				)
				.json("{x:[[[{_type:'A',a:1},null],null],null]}")
				.jsonT("{x:[[[{t:'A',a:1},null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\t_type: 'A',\n\t\t\t\t\ta: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<A>\n\t\t\t\t\t<a>1</a>\n\t\t\t\t</A>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><A><a>1</a></A><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@((_type=A,a=1),null),null),null))")
				.uonT("(x=@(@(@((t=A,a=1),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@((_type=A,a=1),null),null),null)")
				.urlEncT("x=@(@(@((t=A,a=1),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=A,\n\t\t\t\ta=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A17892929282A55F74797065A141A16101C0C0C0")
				.msgPackT("81A17892929282A174A141A16101C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:_type>A</jp:_type>\n                  <jp:a>1</jp:a>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x")[0][0][0]).isType(A.class))
			},
			{	/* 11 */
				new ComboInput<Map<String,List<IA[][][]>>>(
					"Map<String,List<IA[][][]>>",
					getType(Map.class,String.class,List.class,IA[][][].class),
					map("x",list(new IA[][][]{{{new A().init(),null},null},null},null))
				)
				.json("{x:[[[[{_type:'A',a:1},null],null],null],null]}")
				.jsonT("{x:[[[[{t:'A',a:1},null],null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\t_type: 'A',\n\t\t\t\t\t\ta: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<A>\n\t\t\t\t\t\t<a>1</a>\n\t\t\t\t\t</A>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><array><A><a>1</a></A><null/></array><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table _type='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table t='A'><tr><td>a</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table _type='A'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>a</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@(@((_type=A,a=1),null),null),null),null))")
				.uonT("(x=@(@(@(@((t=A,a=1),null),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=A,\n\t\t\t\t\t\ta=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@(@((_type=A,a=1),null),null),null),null)")
				.urlEncT("x=@(@(@(@((t=A,a=1),null),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\t_type=A,\n\t\t\t\t\ta=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A1789292929282A55F74797065A141A16101C0C0C0C0")
				.msgPackT("81A1789292929282A174A141A16101C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>A</jp:_type>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>A</jp:t>\n<jp:a>1</jp:a>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>A</jp:_type>\n                      <jp:a>1</jp:a>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x").get(0)[0][0][0]).isType(A.class))
			},
			{	/* 12 */
				new ComboInput<B>(
					"B",
					B.class,
					new B().init()
				)
				.json("{z:'B',b:1}")
				.jsonT("{z:'B',b:1}")
				.jsonR("{\n\tz: 'B',\n\tb: 1\n}")
				.xml("<B><b>1</b></B>")
				.xmlT("<B><b>1</b></B>")
				.xmlR("<B>\n\t<b>1</b>\n</B>\n")
				.xmlNs("<B><b>1</b></B>")
				.html("<table z='B'><tr><td>b</td><td>1</td></tr></table>")
				.htmlT("<table z='B'><tr><td>b</td><td>1</td></tr></table>")
				.htmlR("<table z='B'>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(z=B,b=1)")
				.uonT("(z=B,b=1)")
				.uonR("(\n\tz=B,\n\tb=1\n)")
				.urlEnc("z=B&b=1")
				.urlEncT("z=B&b=1")
				.urlEncR("z=B\n&b=1")
				.msgPack("82A17AA142A16201")
				.msgPackT("82A17AA142A16201")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:z>B</jp:z>\n    <jp:b>1</jp:b>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(B.class))
			},
			{	/* 13 */
				new ComboInput<B[]>(
					"B[]",
					B[].class,
					new B[]{new B().init()}
				)
				.json("[{z:'B',b:1}]")
				.jsonT("[{z:'B',b:1}]")
				.jsonR("[\n\t{\n\t\tz: 'B',\n\t\tb: 1\n\t}\n]")
				.xml("<array><B><b>1</b></B></array>")
				.xmlT("<array><B><b>1</b></B></array>")
				.xmlR("<array>\n\t<B>\n\t\t<b>1</b>\n\t</B>\n</array>\n")
				.xmlNs("<array><B><b>1</b></B></array>")
				.html("<table _type='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>")
				.htmlT("<table t='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>b</th>\n\t</tr>\n\t<tr z='B'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("@((z=B,b=1))")
				.uonT("@((z=B,b=1))")
				.uonR("@(\n\t(\n\t\tz=B,\n\t\tb=1\n\t)\n)")
				.urlEnc("0=(z=B,b=1)")
				.urlEncT("0=(z=B,b=1)")
				.urlEncR("0=(\n\tz=B,\n\tb=1\n)")
				.msgPack("9182A17AA142A16201")
				.msgPackT("9182A17AA142A16201")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:z>B</jp:z>\n      <jp:b>1</jp:b>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0]).isType(B.class))
			},
			{	/* 14 */
				new ComboInput<B[][][]>(
					"B[][][]",
					B[][][].class,
					new B[][][]{{{new B().init(),null},null},null}
				)
				.json("[[[{z:'B',b:1},null],null],null]")
				.jsonT("[[[{z:'B',b:1},null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\tz: 'B',\n\t\t\t\tb: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<B>\n\t\t\t\t<b>1</b>\n\t\t\t</B>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@((z=B,b=1),null),null),null)")
				.uonT("@(@(@((z=B,b=1),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@((z=B,b=1),null),null)&1=null")
				.urlEncT("0=@(@((z=B,b=1),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t(\n\t\t\tz=B,\n\t\t\tb=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("92929282A17AA142A16201C0C0C0")
				.msgPackT("92929282A17AA142A16201C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:z>B</jp:z>\n              <jp:b>1</jp:b>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0][0][0]).isType(B.class))
			},
			{	/* 15 */
				new ComboInput<List<B[][][]>>(
					"List<B[][][]>",
					getType(List.class, B[][][].class),
					list(new B[][][]{{{new B().init(),null},null},null},null)
				)
				.json("[[[[{z:'B',b:1},null],null],null],null]")
				.jsonT("[[[[{z:'B',b:1},null],null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@(@((z=B,b=1),null),null),null),null)")
				.uonT("@(@(@(@((z=B,b=1),null),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@(@((z=B,b=1),null),null),null)&1=null")
				.urlEncT("0=@(@(@((z=B,b=1),null),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("9292929282A17AA142A16201C0C0C0C0")
				.msgPackT("9292929282A17AA142A16201C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get(0)[0][0][0]).isType(B.class))
			},
			{	/* 16 */
				new ComboInput<Map<String,B[][][]>>(
					"Map<String,B[][][]>",
					getType(Map.class,String.class,B[][][].class),
					map("x",new B[][][]{{{new B().init(),null},null},null})
				)
				.json("{x:[[[{z:'B',b:1},null],null],null]}")
				.jsonT("{x:[[[{z:'B',b:1},null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@((z=B,b=1),null),null),null))")
				.uonT("(x=@(@(@((z=B,b=1),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@((z=B,b=1),null),null),null)")
				.urlEncT("x=@(@(@((z=B,b=1),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A17892929282A17AA142A16201C0C0C0")
				.msgPackT("81A17892929282A17AA142A16201C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x")[0][0][0]).isType(B.class))
			},
			{	/* 17 */
				new ComboInput<Map<String,List<B[][][]>>>(
					"Map<String,List<B[][][]>>",
					getType(Map.class,String.class,List.class,B[][][].class),
					map("x",list(new B[][][]{{{new B().init(),null},null},null},null))
				)
				.json("{x:[[[[{z:'B',b:1},null],null],null],null]}")
				.jsonT("{x:[[[[{z:'B',b:1},null],null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\tz: 'B',\n\t\t\t\t\t\tb: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<B>\n\t\t\t\t\t\t<b>1</b>\n\t\t\t\t\t</B>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@(@((z=B,b=1),null),null),null),null))")
				.uonT("(x=@(@(@(@((z=B,b=1),null),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\tz=B,\n\t\t\t\t\t\tb=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@(@((z=B,b=1),null),null),null),null)")
				.urlEncT("x=@(@(@(@((z=B,b=1),null),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A1789292929282A17AA142A16201C0C0C0C0")
				.msgPackT("81A1789292929282A17AA142A16201C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:z>B</jp:z>\n                      <jp:b>1</jp:b>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x").get(0)[0][0][0]).isType(B.class))
			},
			{	/* 18 */
				new ComboInput<IB>(
					"IB",
					IB.class,
					new B().init()
				)
				.json("{z:'B',b:1}")
				.jsonT("{z:'B',b:1}")
				.jsonR("{\n\tz: 'B',\n\tb: 1\n}")
				.xml("<B><b>1</b></B>")
				.xmlT("<B><b>1</b></B>")
				.xmlR("<B>\n\t<b>1</b>\n</B>\n")
				.xmlNs("<B><b>1</b></B>")
				.html("<table z='B'><tr><td>b</td><td>1</td></tr></table>")
				.htmlT("<table z='B'><tr><td>b</td><td>1</td></tr></table>")
				.htmlR("<table z='B'>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("(z=B,b=1)")
				.uonT("(z=B,b=1)")
				.uonR("(\n\tz=B,\n\tb=1\n)")
				.urlEnc("z=B&b=1")
				.urlEncT("z=B&b=1")
				.urlEncR("z=B\n&b=1")
				.msgPack("82A17AA142A16201")
				.msgPackT("82A17AA142A16201")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:z>B</jp:z>\n    <jp:b>1</jp:b>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(B.class))
			},
			{	/* 19 */
				new ComboInput<IB[]>(
					"IB[]",
					IB[].class,
					new IB[]{new B().init()}
				)
				.json("[{z:'B',b:1}]")
				.jsonT("[{z:'B',b:1}]")
				.jsonR("[\n\t{\n\t\tz: 'B',\n\t\tb: 1\n\t}\n]")
				.xml("<array><B><b>1</b></B></array>")
				.xmlT("<array><B><b>1</b></B></array>")
				.xmlR("<array>\n\t<B>\n\t\t<b>1</b>\n\t</B>\n</array>\n")
				.xmlNs("<array><B><b>1</b></B></array>")
				.html("<table _type='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>")
				.htmlT("<table t='array'><tr><th>b</th></tr><tr z='B'><td>1</td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>b</th>\n\t</tr>\n\t<tr z='B'>\n\t\t<td>1</td>\n\t</tr>\n</table>\n")
				.uon("@((z=B,b=1))")
				.uonT("@((z=B,b=1))")
				.uonR("@(\n\t(\n\t\tz=B,\n\t\tb=1\n\t)\n)")
				.urlEnc("0=(z=B,b=1)")
				.urlEncT("0=(z=B,b=1)")
				.urlEncR("0=(\n\tz=B,\n\tb=1\n)")
				.msgPack("9182A17AA142A16201")
				.msgPackT("9182A17AA142A16201")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:z>B</jp:z>\n      <jp:b>1</jp:b>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0]).isType(B.class))
			},
			{	/* 20 */
				new ComboInput<IB[][][]>(
					"IB[][][]",
					IB[][][].class,
					new IB[][][]{{{new B().init(),null},null},null}
				)
				.json("[[[{z:'B',b:1},null],null],null]")
				.jsonT("[[[{z:'B',b:1},null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t{\n\t\t\t\tz: 'B',\n\t\t\t\tb: 1\n\t\t\t},\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<B>\n\t\t\t\t<b>1</b>\n\t\t\t</B>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t</table>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@((z=B,b=1),null),null),null)")
				.uonT("@(@(@((z=B,b=1),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@((z=B,b=1),null),null)&1=null")
				.urlEncT("0=@(@((z=B,b=1),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t(\n\t\t\tz=B,\n\t\t\tb=1\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("92929282A17AA142A16201C0C0C0")
				.msgPackT("92929282A17AA142A16201C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:z>B</jp:z>\n              <jp:b>1</jp:b>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x[0][0][0]).isType(B.class))
			},
			{	/* 21 */
				new ComboInput<List<IB[][][]>>(
					"List<IB[][][]>",
					getType(List.class,IB[][][].class),
					list(new IB[][][]{{{new B().init(),null},null},null},null)
				)
				.json("[[[[{z:'B',b:1},null],null],null],null]")
				.jsonT("[[[[{z:'B',b:1},null],null],null],null]")
				.jsonR("[\n\t[\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t],\n\tnull\n]")
				.xml("<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>")
				.xmlT("<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>")
				.xmlR("<array>\n\t<array>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</array>\n\t<null/>\n</array>\n")
				.xmlNs("<array><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></array>")
				.html("<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlT("<ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<ul>\n\t\t\t\t\t<li>\n\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t</ul>\n\t\t\t\t\t</li>\n\t\t\t\t\t<li><null/></li>\n\t\t\t\t</ul>\n\t\t\t</li>\n\t\t\t<li><null/></li>\n\t\t</ul>\n\t</li>\n\t<li><null/></li>\n</ul>\n")
				.uon("@(@(@(@((z=B,b=1),null),null),null),null)")
				.uonT("@(@(@(@((z=B,b=1),null),null),null),null)")
				.uonR("@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.urlEnc("0=@(@(@((z=B,b=1),null),null),null)&1=null")
				.urlEncT("0=@(@(@((z=B,b=1),null),null),null)&1=null")
				.urlEncR("0=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)\n&1=null")
				.msgPack("9292929282A17AA142A16201C0C0C0C0")
				.msgPackT("9292929282A17AA142A16201C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get(0)[0][0][0]).isType(B.class))
			},
			{	/* 22 */
				new ComboInput<Map<String,IB[][][]>>(
					"Map<String,IB[][][]>",
					getType(Map.class,String.class,IB[][][].class),
					map("x",new IB[][][]{{{new B().init(),null},null},null})
				)
				.json("{x:[[[{z:'B',b:1},null],null],null]}")
				.jsonT("{x:[[[{z:'B',b:1},null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t{\n\t\t\t\t\tz: 'B',\n\t\t\t\t\tb: 1\n\t\t\t\t},\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<B>\n\t\t\t\t\t<b>1</b>\n\t\t\t\t</B>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><B><b>1</b></B><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@((z=B,b=1),null),null),null))")
				.uonT("(x=@(@(@((z=B,b=1),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@((z=B,b=1),null),null),null)")
				.urlEncT("x=@(@(@((z=B,b=1),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t(\n\t\t\t\tz=B,\n\t\t\t\tb=1\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A17892929282A17AA142A16201C0C0C0")
				.msgPackT("81A17892929282A17AA142A16201C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li rdf:parseType='Resource'>\n                  <jp:z>B</jp:z>\n                  <jp:b>1</jp:b>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x")[0][0][0]).isType(B.class))
			},
			{	/* 23 */
				new ComboInput<Map<String,List<IB[][][]>>>(
					"Map<String,List<IB[][][]>>",
					getType(Map.class,String.class,List.class,IB[][][].class),
					map("x",list(new IB[][][]{{{new B().init(),null},null},null},null))
				)
				.json("{x:[[[[{z:'B',b:1},null],null],null],null]}")
				.jsonT("{x:[[[[{z:'B',b:1},null],null],null],null]}")
				.jsonR("{\n\tx: [\n\t\t[\n\t\t\t[\n\t\t\t\t[\n\t\t\t\t\t{\n\t\t\t\t\t\tz: 'B',\n\t\t\t\t\t\tb: 1\n\t\t\t\t\t},\n\t\t\t\t\tnull\n\t\t\t\t],\n\t\t\t\tnull\n\t\t\t],\n\t\t\tnull\n\t\t],\n\t\tnull\n\t]\n}")
				.xml("<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlT("<object><x t='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>")
				.xmlR("<object>\n\t<x _type='array'>\n\t\t<array>\n\t\t\t<array>\n\t\t\t\t<array>\n\t\t\t\t\t<B>\n\t\t\t\t\t\t<b>1</b>\n\t\t\t\t\t</B>\n\t\t\t\t\t<null/>\n\t\t\t\t</array>\n\t\t\t\t<null/>\n\t\t\t</array>\n\t\t\t<null/>\n\t\t</array>\n\t\t<null/>\n\t</x>\n</object>\n")
				.xmlNs("<object><x _type='array'><array><array><array><B><b>1</b></B><null/></array><null/></array><null/></array><null/></x></object>")
				.html("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>x</td><td><ul><li><ul><li><ul><li><ul><li><table z='B'><tr><td>b</td><td>1</td></tr></table></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></li><li><null/></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>x</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t<ul>\n\t\t\t\t\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t\t\t\t\t<table z='B'>\n\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>b</td>\n\t\t\t\t\t\t\t\t\t\t\t\t\t<td>1</td>\n\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t</table>\n\t\t\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t\t\t</li>\n\t\t\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t\t\t</ul>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li><null/></li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li><null/></li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(x=@(@(@(@((z=B,b=1),null),null),null),null))")
				.uonT("(x=@(@(@(@((z=B,b=1),null),null),null),null))")
				.uonR("(\n\tx=@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t@(\n\t\t\t\t\t(\n\t\t\t\t\t\tz=B,\n\t\t\t\t\t\tb=1\n\t\t\t\t\t),\n\t\t\t\t\tnull\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t)\n)")
				.urlEnc("x=@(@(@(@((z=B,b=1),null),null),null),null)")
				.urlEncT("x=@(@(@(@((z=B,b=1),null),null),null),null)")
				.urlEncR("x=@(\n\t@(\n\t\t@(\n\t\t\t@(\n\t\t\t\t(\n\t\t\t\t\tz=B,\n\t\t\t\t\tb=1\n\t\t\t\t),\n\t\t\t\tnull\n\t\t\t),\n\t\t\tnull\n\t\t),\n\t\tnull\n\t),\n\tnull\n)")
				.msgPack("81A1789292929282A17AA142A16201C0C0C0C0")
				.msgPackT("81A1789292929282A17AA142A16201C0C0C0C0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:x>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:z>B</jp:z>\n<jp:b>1</jp:b>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</rdf:li>\n<rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n</rdf:Seq>\n</jp:x>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:x>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li>\n              <rdf:Seq>\n                <rdf:li>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:z>B</jp:z>\n                      <jp:b>1</jp:b>\n                    </rdf:li>\n                    <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n                  </rdf:Seq>\n                </rdf:li>\n                <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n              </rdf:Seq>\n            </rdf:li>\n            <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/>\n      </rdf:Seq>\n    </jp:x>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("x").get(0)[0][0][0]).isType(B.class))
			},
		});
	}

	public BeanDictionaryComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	@Override
	protected Serializer applySettings(Serializer s) throws Exception {
		return s.copy().keepNullProperties().build();
	}

	@Override
	protected Parser applySettings(Parser p) throws Exception {
		return p;
	}

	@Bean(dictionary={A.class})
	public static interface IA {}

	@Bean(typeName="A")
	public static class A implements IA {
		public int a;

		public A init() {
			a = 1;
			return this;
		}
	}

	@Bean(dictionary={B.class}, typePropertyName="z")
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
