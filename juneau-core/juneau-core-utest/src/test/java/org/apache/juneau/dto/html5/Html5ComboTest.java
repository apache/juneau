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
package org.apache.juneau.dto.html5;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Exhaustive serialization tests for all the HTML5 DTOs.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({})
public class Html5ComboTest extends ComboRoundTripTest {

	private static final B btag = b("bbb");

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{	/* 0 */
				new ComboInput<A>(
					"A",
					A.class,
					a("http://foo", "bar"),
					/* Json */		"{_type:'a',a:{href:'http://foo'},c:['bar']}",
					/* JsonT */		"{t:'a',a:{href:'http://foo'},c:['bar']}",
					/* JsonR */		"{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t'bar'\n\t]\n}",
					/* Xml */		"<a href='http://foo'>bar</a>",
					/* XmlT */		"<a href='http://foo'>bar</a>",
					/* XmlR */		"<a href='http://foo'>bar</a>\n",
					/* XmlNs */		"<a href='http://foo'>bar</a>",
					/* Html */		"<a href='http://foo'>bar</a>",
					/* HtmlT */		"<a href='http://foo'>bar</a>",
					/* HtmlR */		"<a href='http://foo'>bar</a>\n",
					/* Uon */		"(_type=a,a=(href=http://foo),c=@(bar))",
					/* UonT */		"(t=a,a=(href=http://foo),c=@(bar))",
					/* UonR */		"(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar\n\t)\n)",
					/* UrlEnc */	"_type=a&a=(href=http://foo)&c=@(bar)",
					/* UrlEncT */	"t=a&a=(href=http://foo)&c=@(bar)",
					/* UrlEncR */	"_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\tbar\n)",
					/* MsgPack */	"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A3626172",
					/* MsgPackT */	"83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A o) {
						assertInstanceOf(A.class, o);
					}
				}
			},
			{	/* 1 */
				new ComboInput<A[]>(
					"A[]",
					A[].class,
					new A[]{a("http://foo", "bar"),a("http://baz", "qux")},
					/* Json */		"[{_type:'a',a:{href:'http://foo'},c:['bar']},{_type:'a',a:{href:'http://baz'},c:['qux']}]",
					/* JsonT */		"[{t:'a',a:{href:'http://foo'},c:['bar']},{t:'a',a:{href:'http://baz'},c:['qux']}]",
					/* JsonR */		"[\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://foo'\n\t\t},\n\t\tc: [\n\t\t\t'bar'\n\t\t]\n\t},\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://baz'\n\t\t},\n\t\tc: [\n\t\t\t'qux'\n\t\t]\n\t}\n]",
					/* Xml */		"<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>",
					/* XmlT */		"<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>",
					/* XmlR */		"<array>\n\t<a href='http://foo'>bar</a>\n\t<a href='http://baz'>qux</a>\n</array>\n",
					/* XmlNs */		"<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>",
					/* Html */		"<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>",
					/* HtmlT */		"<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<a href='http://foo'>bar</a>\n\t</li>\n\t<li>\n\t\t<a href='http://baz'>qux</a>\n\t</li>\n</ul>\n",
					/* Uon */		"@((_type=a,a=(href=http://foo),c=@(bar)),(_type=a,a=(href=http://baz),c=@(qux)))",
					/* UonT */		"@((t=a,a=(href=http://foo),c=@(bar)),(t=a,a=(href=http://baz),c=@(qux)))",
					/* UonR */		"@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://foo\n\t\t),\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://baz\n\t\t),\n\t\tc=@(\n\t\t\tqux\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=(_type=a,a=(href=http://foo),c=@(bar))&1=(_type=a,a=(href=http://baz),c=@(qux))",
					/* UrlEncT */	"0=(t=a,a=(href=http://foo),c=@(bar))&1=(t=a,a=(href=http://baz),c=@(qux))",
					/* UrlEncR */	"0=(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar\n\t)\n)\n&1=(\n\t_type=a,\n\ta=(\n\t\thref=http://baz\n\t),\n\tc=@(\n\t\tqux\n\t)\n)",
					/* MsgPack */	"9283A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A55F74797065A161A16181A468726566AA687474703A2F2F62617AA16391A3717578",
					/* MsgPackT */	"9283A174A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A174A161A16181A468726566AA687474703A2F2F62617AA16391A3717578",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://foo'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>bar</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://baz'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>qux</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A[] o) {
						assertInstanceOf(A[].class, o);
						assertEquals(2, o.length);
					}
				}
			},
			{	/* 2 */
				new ComboInput<List<A>>(
					"List<A>",
					getType(List.class, A.class),
					AList.of(a("http://foo", "bar"),a("http://baz", "qux")),
					/* Json */		"[{_type:'a',a:{href:'http://foo'},c:['bar']},{_type:'a',a:{href:'http://baz'},c:['qux']}]",
					/* JsonT */		"[{t:'a',a:{href:'http://foo'},c:['bar']},{t:'a',a:{href:'http://baz'},c:['qux']}]",
					/* JsonR */		"[\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://foo'\n\t\t},\n\t\tc: [\n\t\t\t'bar'\n\t\t]\n\t},\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://baz'\n\t\t},\n\t\tc: [\n\t\t\t'qux'\n\t\t]\n\t}\n]",
					/* Xml */		"<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>",
					/* XmlT */		"<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>",
					/* XmlR */		"<array>\n\t<a href='http://foo'>bar</a>\n\t<a href='http://baz'>qux</a>\n</array>\n",
					/* XmlNs */		"<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>",
					/* Html */		"<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>",
					/* HtmlT */		"<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<a href='http://foo'>bar</a>\n\t</li>\n\t<li>\n\t\t<a href='http://baz'>qux</a>\n\t</li>\n</ul>\n",
					/* Uon */		"@((_type=a,a=(href=http://foo),c=@(bar)),(_type=a,a=(href=http://baz),c=@(qux)))",
					/* UonT */		"@((t=a,a=(href=http://foo),c=@(bar)),(t=a,a=(href=http://baz),c=@(qux)))",
					/* UonR */		"@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://foo\n\t\t),\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://baz\n\t\t),\n\t\tc=@(\n\t\t\tqux\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=(_type=a,a=(href=http://foo),c=@(bar))&1=(_type=a,a=(href=http://baz),c=@(qux))",
					/* UrlEncT */	"0=(t=a,a=(href=http://foo),c=@(bar))&1=(t=a,a=(href=http://baz),c=@(qux))",
					/* UrlEncR */	"0=(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar\n\t)\n)\n&1=(\n\t_type=a,\n\ta=(\n\t\thref=http://baz\n\t),\n\tc=@(\n\t\tqux\n\t)\n)",
					/* MsgPack */	"9283A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A55F74797065A161A16181A468726566AA687474703A2F2F62617AA16391A3717578",
					/* MsgPackT */	"9283A174A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A174A161A16181A468726566AA687474703A2F2F62617AA16391A3717578",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://foo'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>bar</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://baz'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>qux</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<A> o) {
						assertEquals(2, o.size());
						assertInstanceOf(A.class, o.get(0));
						assertInstanceOf(A.class, o.get(1));
					}
				}
			},
			{	/* 3 */
				new ComboInput<A[][]>(
					"A[][]",
					A[][].class,
					new A[][]{{a("http://a", "b"),a("http://c", "d")},{},{a("http://e", "f")}},
					/* Json */		"[[{_type:'a',a:{href:'http://a'},c:['b']},{_type:'a',a:{href:'http://c'},c:['d']}],[],[{_type:'a',a:{href:'http://e'},c:['f']}]]",
					/* JsonT */		"[[{t:'a',a:{href:'http://a'},c:['b']},{t:'a',a:{href:'http://c'},c:['d']}],[],[{t:'a',a:{href:'http://e'},c:['f']}]]",
					/* JsonR */		"[\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://a'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'b'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://c'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'd'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://e'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'f'\n\t\t\t]\n\t\t}\n\t]\n]",
					/* Xml */		"<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array></array><array><a href='http://e'>f</a></array></array>",
					/* XmlT */		"<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array></array><array><a href='http://e'>f</a></array></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<a href='http://a'>b</a>\n\t\t<a href='http://c'>d</a>\n\t</array>\n\t<array>\n\t</array>\n\t<array>\n\t\t<a href='http://e'>f</a>\n\t</array>\n</array>\n",
					/* XmlNs */		"<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array></array><array><a href='http://e'>f</a></array></array>",
					/* Html */		"<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://a'>b</a>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<a href='http://c'>d</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul></ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://e'>f</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
					/* Uon */		"@(@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d))),@(),@((_type=a,a=(href=http://e),c=@(f))))",
					/* UonT */		"@(@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d))),@(),@((t=a,a=(href=http://e),c=@(f))))",
					/* UonR */		"@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://a\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tb\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://c\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\td\n\t\t\t)\n\t\t)\n\t),\n\t@(),\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://e\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tf\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d)))&1=@()&2=@((_type=a,a=(href=http://e),c=@(f)))",
					/* UrlEncT */	"0=@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d)))&1=@()&2=@((t=a,a=(href=http://e),c=@(f)))",
					/* UrlEncR */	"0=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://a\n\t\t),\n\t\tc=@(\n\t\t\tb\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://c\n\t\t),\n\t\tc=@(\n\t\t\td\n\t\t)\n\t)\n)\n&1=@()\n&2=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://e\n\t\t),\n\t\tc=@(\n\t\t\tf\n\t\t)\n\t)\n)",
					/* MsgPack */	"939283A55F74797065A161A16181A468726566A8687474703A2F2F61A16391A16283A55F74797065A161A16181A468726566A8687474703A2F2F63A16391A164909183A55F74797065A161A16181A468726566A8687474703A2F2F65A16391A166",
					/* MsgPackT */	"939283A174A161A16181A468726566A8687474703A2F2F61A16391A16283A174A161A16181A468726566A8687474703A2F2F63A16391A164909183A174A161A16181A468726566A8687474703A2F2F65A16391A166",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://a'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://c'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>d</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq/>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://e'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>f</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A[][] o) {
						assertInstanceOf(A[][].class, o);
						assertEquals(3, o.length);
						assertEquals(2, o[0].length);
						assertEquals(0, o[1].length);
						assertEquals(1, o[2].length);
					}
				}
			},
			{	/* 4 */
				new ComboInput<List<List<A>>>(
					"List<List<A>>",
					getType(List.class, List.class, A.class),
					AList.of(AList.of(a("http://a", "b"),a("http://c", "d")),AList.of(a("http://e", "f"))),
					/* Json */		"[[{_type:'a',a:{href:'http://a'},c:['b']},{_type:'a',a:{href:'http://c'},c:['d']}],[{_type:'a',a:{href:'http://e'},c:['f']}]]",
					/* JsonT */		"[[{t:'a',a:{href:'http://a'},c:['b']},{t:'a',a:{href:'http://c'},c:['d']}],[{t:'a',a:{href:'http://e'},c:['f']}]]",
					/* JsonR */		"[\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://a'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'b'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://c'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'd'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://e'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'f'\n\t\t\t]\n\t\t}\n\t]\n]",
					/* Xml */		"<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array><a href='http://e'>f</a></array></array>",
					/* XmlT */		"<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array><a href='http://e'>f</a></array></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<a href='http://a'>b</a>\n\t\t<a href='http://c'>d</a>\n\t</array>\n\t<array>\n\t\t<a href='http://e'>f</a>\n\t</array>\n</array>\n",
					/* XmlNs */		"<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array><a href='http://e'>f</a></array></array>",
					/* Html */		"<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://a'>b</a>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<a href='http://c'>d</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://e'>f</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
					/* Uon */		"@(@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d))),@((_type=a,a=(href=http://e),c=@(f))))",
					/* UonT */		"@(@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d))),@((t=a,a=(href=http://e),c=@(f))))",
					/* UonR */		"@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://a\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tb\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://c\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\td\n\t\t\t)\n\t\t)\n\t),\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://e\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tf\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d)))&1=@((_type=a,a=(href=http://e),c=@(f)))",
					/* UrlEncT */	"0=@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d)))&1=@((t=a,a=(href=http://e),c=@(f)))",
					/* UrlEncR */	"0=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://a\n\t\t),\n\t\tc=@(\n\t\t\tb\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://c\n\t\t),\n\t\tc=@(\n\t\t\td\n\t\t)\n\t)\n)\n&1=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://e\n\t\t),\n\t\tc=@(\n\t\t\tf\n\t\t)\n\t)\n)",
					/* MsgPack */	"929283A55F74797065A161A16181A468726566A8687474703A2F2F61A16391A16283A55F74797065A161A16181A468726566A8687474703A2F2F63A16391A1649183A55F74797065A161A16181A468726566A8687474703A2F2F65A16391A166",
					/* MsgPackT */	"929283A174A161A16181A468726566A8687474703A2F2F61A16391A16283A174A161A16181A468726566A8687474703A2F2F63A16391A1649183A174A161A16181A468726566A8687474703A2F2F65A16391A166",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://a'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://c'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>d</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://e'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>f</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<List<A>> o) {
						assertInstanceOf(A.class, o.get(0).get(0));
					}
				}
			},
			{	/* 5 */
				new ComboInput<java.util.Map<String,A>>(
					"Map<String,A>",
					getType(java.util.Map.class, String.class, A.class),
					AMap.of("a", a("http://b", "c"), "d", a("http://e", "f")),
					/* Json */		"{a:{_type:'a',a:{href:'http://b'},c:['c']},d:{_type:'a',a:{href:'http://e'},c:['f']}}",
					/* JsonT */		"{a:{t:'a',a:{href:'http://b'},c:['c']},d:{t:'a',a:{href:'http://e'},c:['f']}}",
					/* JsonR */		"{\n\ta: {\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://b'\n\t\t},\n\t\tc: [\n\t\t\t'c'\n\t\t]\n\t},\n\td: {\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://e'\n\t\t},\n\t\tc: [\n\t\t\t'f'\n\t\t]\n\t}\n}",
					/* Xml */		"<object><a href='http://b'>c</a><a _name='d' href='http://e'>f</a></object>",
					/* XmlT */		"<object><a href='http://b'>c</a><a _name='d' href='http://e'>f</a></object>",
					/* XmlR */		"<object>\n\t<a href='http://b'>c</a>\n\t<a _name='d' href='http://e'>f</a>\n</object>\n",
					/* XmlNs */		"<object><a href='http://b'>c</a><a _name='d' href='http://e'>f</a></object>",
					/* Html */		"<table><tr><td>a</td><td><a href='http://b'>c</a></td></tr><tr><td>d</td><td><a href='http://e'>f</a></td></tr></table>",
					/* HtmlT */		"<table><tr><td>a</td><td><a href='http://b'>c</a></td></tr><tr><td>d</td><td><a href='http://e'>f</a></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>d</td>\n\t\t<td>\n\t\t\t<a href='http://e'>f</a>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(a=(_type=a,a=(href=http://b),c=@(c)),d=(_type=a,a=(href=http://e),c=@(f)))",
					/* UonT */		"(a=(t=a,a=(href=http://b),c=@(c)),d=(t=a,a=(href=http://e),c=@(f)))",
					/* UonR */		"(\n\ta=(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\td=(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://e\n\t\t),\n\t\tc=@(\n\t\t\tf\n\t\t)\n\t)\n)",
					/* UrlEnc */	"a=(_type=a,a=(href=http://b),c=@(c))&d=(_type=a,a=(href=http://e),c=@(f))",
					/* UrlEncT */	"a=(t=a,a=(href=http://b),c=@(c))&d=(t=a,a=(href=http://e),c=@(f))",
					/* UrlEncR */	"a=(\n\t_type=a,\n\ta=(\n\t\thref=http://b\n\t),\n\tc=@(\n\t\tc\n\t)\n)\n&d=(\n\t_type=a,\n\ta=(\n\t\thref=http://e\n\t),\n\tc=@(\n\t\tf\n\t)\n)",
					/* MsgPack */	"82A16183A55F74797065A161A16181A468726566A8687474703A2F2F62A16391A163A16483A55F74797065A161A16181A468726566A8687474703A2F2F65A16391A166",
					/* MsgPackT */	"82A16183A174A161A16181A468726566A8687474703A2F2F62A16391A163A16483A174A161A16181A468726566A8687474703A2F2F65A16391A166",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:a>\n<jp:d rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:d>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:a>\n<jp:d rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:d>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:a rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://b'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>c</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </jp:a>\n    <jp:d rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://e'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>f</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </jp:d>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(java.util.Map<String,A> o) {
						assertInstanceOf(A.class, o.get("a"));
						assertInstanceOf(A.class, o.get("d"));
					}
				}
			},
			{	/* 6 */
				new ComboInput<java.util.Map<String,A[][]>>(
					"Map<String,A[][]>",
					getType(java.util.Map.class, String.class, A[][].class),
					AMap.of("a", new A[][]{{a("http://b", "c"),a("http://d", "e")},{}}, "f", new A[][]{{a("http://g", "h")}}),
					/* Json */		"{a:[[{_type:'a',a:{href:'http://b'},c:['c']},{_type:'a',a:{href:'http://d'},c:['e']}],[]],f:[[{_type:'a',a:{href:'http://g'},c:['h']}]]}",
					/* JsonT */		"{a:[[{t:'a',a:{href:'http://b'},c:['c']},{t:'a',a:{href:'http://d'},c:['e']}],[]],f:[[{t:'a',a:{href:'http://g'},c:['h']}]]}",
					/* JsonR */		"{\n\ta: [\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'a',\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\t_type: 'a',\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://d'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'e'\n\t\t\t\t]\n\t\t\t}\n\t\t],\n\t\t[\n\t\t]\n\t],\n\tf: [\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'a',\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://g'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'h'\n\t\t\t\t]\n\t\t\t}\n\t\t]\n\t]\n}",
					/* Xml */		"<object><a _type='array'><array><a href='http://b'>c</a><a href='http://d'>e</a></array><array></array></a><f _type='array'><array><a href='http://g'>h</a></array></f></object>",
					/* XmlT */		"<object><a t='array'><array><a href='http://b'>c</a><a href='http://d'>e</a></array><array></array></a><f t='array'><array><a href='http://g'>h</a></array></f></object>",
					/* XmlR */		"<object>\n\t<a _type='array'>\n\t\t<array>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://d'>e</a>\n\t\t</array>\n\t\t<array>\n\t\t</array>\n\t</a>\n\t<f _type='array'>\n\t\t<array>\n\t\t\t<a href='http://g'>h</a>\n\t\t</array>\n\t</f>\n</object>\n",
					/* XmlNs */		"<object><a _type='array'><array><a href='http://b'>c</a><a href='http://d'>e</a></array><array></array></a><f _type='array'><array><a href='http://g'>h</a></array></f></object>",
					/* Html */		"<table><tr><td>a</td><td><ul><li><ul><li><a href='http://b'>c</a></li><li><a href='http://d'>e</a></li></ul></li><li><ul></ul></li></ul></td></tr><tr><td>f</td><td><ul><li><ul><li><a href='http://g'>h</a></li></ul></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>a</td><td><ul><li><ul><li><a href='http://b'>c</a></li><li><a href='http://d'>e</a></li></ul></li><li><ul></ul></li></ul></td></tr><tr><td>f</td><td><ul><li><ul><li><a href='http://g'>h</a></li></ul></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<a href='http://d'>e</a>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<ul></ul>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<a href='http://g'>h</a>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(a=@(@((_type=a,a=(href=http://b),c=@(c)),(_type=a,a=(href=http://d),c=@(e))),@()),f=@(@((_type=a,a=(href=http://g),c=@(h)))))",
					/* UonT */		"(a=@(@((t=a,a=(href=http://b),c=@(c)),(t=a,a=(href=http://d),c=@(e))),@()),f=@(@((t=a,a=(href=http://g),c=@(h)))))",
					/* UonR */		"(\n\ta=@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=a,\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=a,\n\t\t\t\ta=(\n\t\t\t\t\thref=http://d\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\te\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t@()\n\t),\n\tf=@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=a,\n\t\t\t\ta=(\n\t\t\t\t\thref=http://g\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\th\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"a=@(@((_type=a,a=(href=http://b),c=@(c)),(_type=a,a=(href=http://d),c=@(e))),@())&f=@(@((_type=a,a=(href=http://g),c=@(h))))",
					/* UrlEncT */	"a=@(@((t=a,a=(href=http://b),c=@(c)),(t=a,a=(href=http://d),c=@(e))),@())&f=@(@((t=a,a=(href=http://g),c=@(h))))",
					/* UrlEncR */	"a=@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://d\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\te\n\t\t\t)\n\t\t)\n\t),\n\t@()\n)\n&f=@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://g\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\th\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A161929283A55F74797065A161A16181A468726566A8687474703A2F2F62A16391A16383A55F74797065A161A16181A468726566A8687474703A2F2F64A16391A16590A166919183A55F74797065A161A16181A468726566A8687474703A2F2F67A16391A168",
					/* MsgPackT */	"82A161929283A174A161A16181A468726566A8687474703A2F2F62A16391A16383A174A161A16181A468726566A8687474703A2F2F64A16391A16590A166919183A174A161A16181A468726566A8687474703A2F2F67A16391A168",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:a>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n</rdf:Seq>\n</jp:a>\n<jp:f>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:a>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n</rdf:Seq>\n</jp:a>\n<jp:f>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:a>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>a</jp:_type>\n              <jp:a rdf:parseType='Resource'>\n                <jp:href rdf:resource='http://b'/>\n              </jp:a>\n              <jp:c>\n                <rdf:Seq>\n                  <rdf:li>c</rdf:li>\n                </rdf:Seq>\n              </jp:c>\n            </rdf:li>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>a</jp:_type>\n              <jp:a rdf:parseType='Resource'>\n                <jp:href rdf:resource='http://d'/>\n              </jp:a>\n              <jp:c>\n                <rdf:Seq>\n                  <rdf:li>e</rdf:li>\n                </rdf:Seq>\n              </jp:c>\n            </rdf:li>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li>\n          <rdf:Seq/>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:a>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>a</jp:_type>\n              <jp:a rdf:parseType='Resource'>\n                <jp:href rdf:resource='http://g'/>\n              </jp:a>\n              <jp:c>\n                <rdf:Seq>\n                  <rdf:li>h</rdf:li>\n                </rdf:Seq>\n              </jp:c>\n            </rdf:li>\n          </rdf:Seq>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(java.util.Map<String,A[][]> o) {
						assertInstanceOf(A.class, o.get("a")[0][0]);
						assertInstanceOf(A.class, o.get("f")[0][0]);
					}
				}
			},
			{	/* 7 */
				new ComboInput<BeanWithAField>(
					"BeanWithAField",
					BeanWithAField.class,
					BeanWithAField.create(a("http://b", "c")),
					/* Json */		"{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}",
					/* JsonT */		"{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}",
					/* JsonR */		"{\n\tf1: {\n\t\ta: {\n\t\t\thref: 'http://b'\n\t\t},\n\t\tc: [\n\t\t\t'c'\n\t\t]\n\t},\n\tf2: [\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t}\n\t],\n\tf3: [\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object>",
					/* XmlT */		"<object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object>",
					/* XmlR */		"<object>\n\t<a _name='f1' href='http://b'>c</a>\n\t<f2>\n\t\t<a href='http://b'>c</a>\n\t\t<a href='http://b'>c</a>\n\t</f2>\n\t<f3>\n\t\t<a href='http://b'>c</a>\n\t\t<a href='http://b'>c</a>\n\t</f3>\n</object>\n",
					/* XmlNs */		"<object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object>",
					/* Html */		"<table><tr><td>f1</td><td><a href='http://b'>c</a></td></tr><tr><td>f2</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr><tr><td>f3</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>",
					/* HtmlT */		"<table><tr><td>f1</td><td><a href='http://b'>c</a></td></tr><tr><td>f2</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr><tr><td>f3</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>",
					/* HtmlR */		"<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))",
					/* UonT */		"(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))",
					/* UonR */		"(\n\tf1=(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\tf2=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t),\n\tf3=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"f1=(a=(href=http://b),c=@(c))&f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))&f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))",
					/* UrlEncT */	"f1=(a=(href=http://b),c=@(c))&f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))&f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))",
					/* UrlEncR */	"f1=(\n\ta=(\n\t\thref=http://b\n\t),\n\tc=@(\n\t\tc\n\t)\n)\n&f2=@(\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t)\n)\n&f3=@(\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163",
					/* MsgPackT */	"83A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:f1 rdf:parseType='Resource'>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://b'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>c</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </jp:f1>\n    <jp:f2>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f2>\n    <jp:f3>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithAField o) {
						assertInstanceOf(BeanWithAField.class, o);
					}
				}
			},
			{	/* 8 */
				new ComboInput<BeanWithAField[]>(
					"BeanWithAField[]",
					BeanWithAField[].class,
					new BeanWithAField[]{BeanWithAField.create(a("http://b", "c"))},
					/* Json */		"[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]",
					/* JsonT */		"[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]",
					/* JsonR */		"[\n\t{\n\t\tf1: {\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\tf2: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t],\n\t\tf3: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t]\n\t}\n]",
					/* Xml */		"<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>",
					/* XmlT */		"<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>",
					/* XmlR */		"<array>\n\t<object>\n\t\t<a _name='f1' href='http://b'>c</a>\n\t\t<f2>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f2>\n\t\t<f3>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f3>\n\t</object>\n</array>\n",
					/* XmlNs */		"<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>",
					/* Html */		"<table _type='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>",
					/* HtmlT */		"<table t='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>",
					/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>f1</th>\n\t\t<th>f2</th>\n\t\t<th>f3</th>\n\t</tr>\n\t<tr>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))",
					/* UonT */		"@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))",
					/* UonR */		"@(\n\t(\n\t\tf1=(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\tf2=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\tf3=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))",
					/* UrlEncT */	"0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))",
					/* UrlEncR */	"0=(\n\tf1=(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\tf2=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t),\n\tf3=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163",
					/* MsgPackT */	"9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:f1 rdf:parseType='Resource'>\n        <jp:a rdf:parseType='Resource'>\n          <jp:href rdf:resource='http://b'/>\n        </jp:a>\n        <jp:c>\n          <rdf:Seq>\n            <rdf:li>c</rdf:li>\n          </rdf:Seq>\n        </jp:c>\n      </jp:f1>\n      <jp:f2>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f2>\n      <jp:f3>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f3>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(BeanWithAField[] o) {
						assertInstanceOf(BeanWithAField[].class, o);
					}
				}
			},
			{	/* 9 */
				new ComboInput<List<BeanWithAField>>(
					"List<BeanWithAField>",
					getType(List.class, BeanWithAField.class),
					AList.of(BeanWithAField.create(a("http://b", "c"))),
					/* Json */		"[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]",
					/* JsonT */		"[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]",
					/* JsonR */		"[\n\t{\n\t\tf1: {\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\tf2: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t],\n\t\tf3: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t]\n\t}\n]",
					/* Xml */		"<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>",
					/* XmlT */		"<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>",
					/* XmlR */		"<array>\n\t<object>\n\t\t<a _name='f1' href='http://b'>c</a>\n\t\t<f2>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f2>\n\t\t<f3>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f3>\n\t</object>\n</array>\n",
					/* XmlNs */		"<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>",
					/* Html */		"<table _type='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>",
					/* HtmlT */		"<table t='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>",
					/* HtmlR */		"<table _type='array'>\n\t<tr>\n\t\t<th>f1</th>\n\t\t<th>f2</th>\n\t\t<th>f3</th>\n\t</tr>\n\t<tr>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n",
					/* Uon */		"@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))",
					/* UonT */		"@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))",
					/* UonR */		"@(\n\t(\n\t\tf1=(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\tf2=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\tf3=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))",
					/* UrlEncT */	"0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))",
					/* UrlEncR */	"0=(\n\tf1=(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\tf2=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t),\n\tf3=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163",
					/* MsgPackT */	"9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:f1 rdf:parseType='Resource'>\n        <jp:a rdf:parseType='Resource'>\n          <jp:href rdf:resource='http://b'/>\n        </jp:a>\n        <jp:c>\n          <rdf:Seq>\n            <rdf:li>c</rdf:li>\n          </rdf:Seq>\n        </jp:c>\n      </jp:f1>\n      <jp:f2>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f2>\n      <jp:f3>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f3>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(List<BeanWithAField> o) {
						assertInstanceOf(BeanWithAField.class, o.get(0));
					}
				}
			},
			{	/* 10 */
				new ComboInput<A>(
					"A-2",
					A.class,
					a("http://foo", "bar", btag, "baz"),
					/* Json */		"{_type:'a',a:{href:'http://foo'},c:['bar',{_type:'b',c:['bbb']},'baz']}",
					/* JsonT */		"{t:'a',a:{href:'http://foo'},c:['bar',{t:'b',c:['bbb']},'baz']}",
					/* JsonR */		"{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t'bar',\n\t\t{\n\t\t\t_type: 'b',\n\t\t\tc: [\n\t\t\t\t'bbb'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}",
					/* Xml */		"<a href='http://foo'>bar<b>bbb</b>baz</a>",
					/* XmlT */		"<a href='http://foo'>bar<b>bbb</b>baz</a>",
					/* XmlR */		"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
					/* XmlNs */		"<a href='http://foo'>bar<b>bbb</b>baz</a>",
					/* Html */		"<a href='http://foo'>bar<b>bbb</b>baz</a>",
					/* HtmlT */		"<a href='http://foo'>bar<b>bbb</b>baz</a>",
					/* HtmlR */		"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
					/* Uon */		"(_type=a,a=(href=http://foo),c=@(bar,(_type=b,c=@(bbb)),baz))",
					/* UonT */		"(t=a,a=(href=http://foo),c=@(bar,(t=b,c=@(bbb)),baz))",
					/* UonR */		"(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar,\n\t\t(\n\t\t\t_type=b,\n\t\t\tc=@(\n\t\t\t\tbbb\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)",
					/* UrlEnc */	"_type=a&a=(href=http://foo)&c=@(bar,(_type=b,c=@(bbb)),baz)",
					/* UrlEncT */	"t=a&a=(href=http://foo)&c=@(bar,(t=b,c=@(bbb)),baz)",
					/* UrlEncR */	"_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\tbar,\n\t(\n\t\t_type=b,\n\t\tc=@(\n\t\t\tbbb\n\t\t)\n\t),\n\tbaz\n)",
					/* MsgPack */	"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16393A362617282A55F74797065A162A16391A3626262A362617A",
					/* MsgPackT */	"83A174A161A16181A468726566AA687474703A2F2F666F6FA16393A362617282A174A162A16391A3626262A362617A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>bar</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>b</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bbb</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A o) {
						assertInstanceOf(A.class, o);
					}
				}
			},
			{	/* 11 */
				new ComboInput<A[][]>(
					"A[][]-2",
					A[][].class,
					new A[][]{{a("http://a", "b", btag, "c"),a("http://d", "e", btag, "f")},{},{a("http://g", "h", btag, "i")}},
					/* Json */		"[[{_type:'a',a:{href:'http://a'},c:['b',{_type:'b',c:['bbb']},'c']},{_type:'a',a:{href:'http://d'},c:['e',{_type:'b',c:['bbb']},'f']}],[],[{_type:'a',a:{href:'http://g'},c:['h',{_type:'b',c:['bbb']},'i']}]]",
					/* JsonT */		"[[{t:'a',a:{href:'http://a'},c:['b',{t:'b',c:['bbb']},'c']},{t:'a',a:{href:'http://d'},c:['e',{t:'b',c:['bbb']},'f']}],[],[{t:'a',a:{href:'http://g'},c:['h',{t:'b',c:['bbb']},'i']}]]",
					/* JsonR */		"[\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://a'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'b',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://d'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'e',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'f'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://g'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'h',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'i'\n\t\t\t]\n\t\t}\n\t]\n]",
					/* Xml */		"<array><array><a href='http://a'>b<b>bbb</b>c</a><a href='http://d'>e<b>bbb</b>f</a></array><array></array><array><a href='http://g'>h<b>bbb</b>i</a></array></array>",
					/* XmlT */		"<array><array><a href='http://a'>b<b>bbb</b>c</a><a href='http://d'>e<b>bbb</b>f</a></array><array></array><array><a href='http://g'>h<b>bbb</b>i</a></array></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<a href='http://a'>b<b>bbb</b>c</a>\n\t\t<a href='http://d'>e<b>bbb</b>f</a>\n\t</array>\n\t<array>\n\t</array>\n\t<array>\n\t\t<a href='http://g'>h<b>bbb</b>i</a>\n\t</array>\n</array>\n",
					/* XmlNs */		"<array><array><a href='http://a'>b<b>bbb</b>c</a><a href='http://d'>e<b>bbb</b>f</a></array><array></array><array><a href='http://g'>h<b>bbb</b>i</a></array></array>",
					/* Html */		"<ul><li><ul><li><a href='http://a'>b<b>bbb</b>c</a></li><li><a href='http://d'>e<b>bbb</b>f</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://g'>h<b>bbb</b>i</a></li></ul></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><a href='http://a'>b<b>bbb</b>c</a></li><li><a href='http://d'>e<b>bbb</b>f</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://g'>h<b>bbb</b>i</a></li></ul></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://a'>b<b>bbb</b>c</a>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<a href='http://d'>e<b>bbb</b>f</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul></ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://g'>h<b>bbb</b>i</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
					/* Uon */		"@(@((_type=a,a=(href=http://a),c=@(b,(_type=b,c=@(bbb)),c)),(_type=a,a=(href=http://d),c=@(e,(_type=b,c=@(bbb)),f))),@(),@((_type=a,a=(href=http://g),c=@(h,(_type=b,c=@(bbb)),i))))",
					/* UonT */		"@(@((t=a,a=(href=http://a),c=@(b,(t=b,c=@(bbb)),c)),(t=a,a=(href=http://d),c=@(e,(t=b,c=@(bbb)),f))),@(),@((t=a,a=(href=http://g),c=@(h,(t=b,c=@(bbb)),i))))",
					/* UonR */		"@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://a\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tb,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://d\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\te,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tf\n\t\t\t)\n\t\t)\n\t),\n\t@(),\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://g\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\th,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\ti\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=@((_type=a,a=(href=http://a),c=@(b,(_type=b,c=@(bbb)),c)),(_type=a,a=(href=http://d),c=@(e,(_type=b,c=@(bbb)),f)))&1=@()&2=@((_type=a,a=(href=http://g),c=@(h,(_type=b,c=@(bbb)),i)))",
					/* UrlEncT */	"0=@((t=a,a=(href=http://a),c=@(b,(t=b,c=@(bbb)),c)),(t=a,a=(href=http://d),c=@(e,(t=b,c=@(bbb)),f)))&1=@()&2=@((t=a,a=(href=http://g),c=@(h,(t=b,c=@(bbb)),i)))",
					/* UrlEncR */	"0=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://a\n\t\t),\n\t\tc=@(\n\t\t\tb,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\tc\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://d\n\t\t),\n\t\tc=@(\n\t\t\te,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\tf\n\t\t)\n\t)\n)\n&1=@()\n&2=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://g\n\t\t),\n\t\tc=@(\n\t\t\th,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\ti\n\t\t)\n\t)\n)",
					/* MsgPack */	"939283A55F74797065A161A16181A468726566A8687474703A2F2F61A16393A16282A55F74797065A162A16391A3626262A16383A55F74797065A161A16181A468726566A8687474703A2F2F64A16393A16582A55F74797065A162A16391A3626262A166909183A55F74797065A161A16181A468726566A8687474703A2F2F67A16393A16882A55F74797065A162A16391A3626262A169",
					/* MsgPackT */	"939283A174A161A16181A468726566A8687474703A2F2F61A16393A16282A174A162A16391A3626262A16383A174A161A16181A468726566A8687474703A2F2F64A16393A16582A174A162A16391A3626262A166909183A174A161A16181A468726566A8687474703A2F2F67A16393A16882A174A162A16391A3626262A169",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>i</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>i</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://a'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://d'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>e</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>f</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq/>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://g'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>h</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>i</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A[][] o) {
						assertInstanceOf(A[][].class, o);
					}
				}
			},
			{	/* 12 */
				new ComboInput<A>(
					"A-3",
					A.class,
					a("http://foo", ""),
					/* Json */		"{_type:'a',a:{href:'http://foo'},c:['']}",
					/* JsonT */		"{t:'a',a:{href:'http://foo'},c:['']}",
					/* JsonR */		"{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t''\n\t]\n}",
					/* Xml */		"<a href='http://foo'>_xE000_</a>",
					/* XmlT */		"<a href='http://foo'>_xE000_</a>",
					/* XmlR */		"<a href='http://foo'>_xE000_</a>\n",
					/* XmlNs */		"<a href='http://foo'>_xE000_</a>",
					/* Html */		"<a href='http://foo'><sp/></a>",
					/* HtmlT */		"<a href='http://foo'><sp/></a>",
					/* HtmlR */		"<a href='http://foo'><sp/></a>\n",
					/* Uon */		"(_type=a,a=(href=http://foo),c=@(''))",
					/* UonT */		"(t=a,a=(href=http://foo),c=@(''))",
					/* UonR */		"(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\t''\n\t)\n)",
					/* UrlEnc */	"_type=a&a=(href=http://foo)&c=@('')",
					/* UrlEncT */	"t=a&a=(href=http://foo)&c=@('')",
					/* UrlEncR */	"_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\t''\n)",
					/* MsgPack */	"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A0",
					/* MsgPackT */	"83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li></rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A o) {
						assertInstanceOf(A.class, o);
					}
				}
			},
			{	/* 13 */
				new ComboInput<A>(
					"A-4",
					A.class,
					a("http://foo", " "),
					/* Json */		"{_type:'a',a:{href:'http://foo'},c:[' ']}",
					/* JsonT */		"{t:'a',a:{href:'http://foo'},c:[' ']}",
					/* JsonR */		"{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t' '\n\t]\n}",
					/* Xml */		"<a href='http://foo'>_x0020_</a>",
					/* XmlT */		"<a href='http://foo'>_x0020_</a>",
					/* XmlR */		"<a href='http://foo'>_x0020_</a>\n",
					/* XmlNs */		"<a href='http://foo'>_x0020_</a>",
					/* Html */		"<a href='http://foo'><sp> </sp></a>",
					/* HtmlT */		"<a href='http://foo'><sp> </sp></a>",
					/* HtmlR */		"<a href='http://foo'><sp> </sp></a>\n",
					/* Uon */		"(_type=a,a=(href=http://foo),c=@(' '))",
					/* UonT */		"(t=a,a=(href=http://foo),c=@(' '))",
					/* UonR */		"(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\t' '\n\t)\n)",
					/* UrlEnc */	"_type=a&a=(href=http://foo)&c=@('+')",
					/* UrlEncT */	"t=a&a=(href=http://foo)&c=@('+')",
					/* UrlEncR */	"_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\t'+'\n)",
					/* MsgPack */	"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A120",
					/* MsgPackT */	"83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A120",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>_x0020_</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>_x0020_</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>_x0020_</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A o) {
						assertInstanceOf(A.class, o);
					}
				}
			},
			{	/* 14 */
				new ComboInput<A>(
					"A-5",
					A.class,
					a("http://foo"),
					/* Json */		"{_type:'a',a:{href:'http://foo'}}",
					/* JsonT */		"{t:'a',a:{href:'http://foo'}}",
					/* JsonR */		"{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t}\n}",
					/* Xml */		"<a href='http://foo' nil='true'></a>",
					/* XmlT */		"<a href='http://foo' nil='true'></a>",
					/* XmlR */		"<a href='http://foo' nil='true'>\n</a>\n",
					/* XmlNs */		"<a href='http://foo' nil='true'></a>",
					/* Html */		"<a href='http://foo' nil='true'></a>",
					/* HtmlT */		"<a href='http://foo' nil='true'></a>",
					/* HtmlR */		"<a href='http://foo' nil='true'>\n</a>\n",
					/* Uon */		"(_type=a,a=(href=http://foo))",
					/* UonT */		"(t=a,a=(href=http://foo))",
					/* UonR */		"(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t)\n)",
					/* UrlEnc */	"_type=a&a=(href=http://foo)",
					/* UrlEncT */	"t=a&a=(href=http://foo)",
					/* UrlEncR */	"_type=a\n&a=(\n\thref=http://foo\n)",
					/* MsgPack */	"82A55F74797065A161A16181A468726566AA687474703A2F2F666F6F",
					/* MsgPackT */	"82A174A161A16181A468726566AA687474703A2F2F666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(A o) {
						assertInstanceOf(A.class, o);
					}
				}
			},
			{	/* 15 */
				new ComboInput<Abbr>(
					"Abbr-1",
					Abbr.class,
					abbr().children("foo"),
					/* Json */		"{_type:'abbr',c:['foo']}",
					/* JsonT */		"{t:'abbr',c:['foo']}",
					/* JsonR */		"{\n\t_type: 'abbr',\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<abbr>foo</abbr>",
					/* XmlT */		"<abbr>foo</abbr>",
					/* XmlR */		"<abbr>foo</abbr>\n",
					/* XmlNs */		"<abbr>foo</abbr>",
					/* Html */		"<abbr>foo</abbr>",
					/* HtmlT */		"<abbr>foo</abbr>",
					/* HtmlR */		"<abbr>foo</abbr>\n",
					/* Uon */		"(_type=abbr,c=@(foo))",
					/* UonT */		"(t=abbr,c=@(foo))",
					/* UonR */		"(\n\t_type=abbr,\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=abbr&c=@(foo)",
					/* UrlEncT */	"t=abbr&c=@(foo)",
					/* UrlEncR */	"_type=abbr\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"82A55F74797065A461626272A16391A3666F6F",
					/* MsgPackT */	"82A174A461626272A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>abbr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>abbr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>abbr</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Abbr o) {
						assertInstanceOf(Abbr.class, o);
					}
				}
			},
			{	/* 16 */
				new ComboInput<Abbr>(
					"Abbr-2",
					Abbr.class,
					abbr("foo", "bar", btag, "baz"),
					/* Json */		"{_type:'abbr',a:{title:'foo'},c:['bar',{_type:'b',c:['bbb']},'baz']}",
					/* JsonT */		"{t:'abbr',a:{title:'foo'},c:['bar',{t:'b',c:['bbb']},'baz']}",
					/* JsonR */		"{\n\t_type: 'abbr',\n\ta: {\n\t\ttitle: 'foo'\n\t},\n\tc: [\n\t\t'bar',\n\t\t{\n\t\t\t_type: 'b',\n\t\t\tc: [\n\t\t\t\t'bbb'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}",
					/* Xml */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
					/* XmlT */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
					/* XmlR */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n",
					/* XmlNs */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
					/* Html */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
					/* HtmlT */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
					/* HtmlR */		"<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n",
					/* Uon */		"(_type=abbr,a=(title=foo),c=@(bar,(_type=b,c=@(bbb)),baz))",
					/* UonT */		"(t=abbr,a=(title=foo),c=@(bar,(t=b,c=@(bbb)),baz))",
					/* UonR */		"(\n\t_type=abbr,\n\ta=(\n\t\ttitle=foo\n\t),\n\tc=@(\n\t\tbar,\n\t\t(\n\t\t\t_type=b,\n\t\t\tc=@(\n\t\t\t\tbbb\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)",
					/* UrlEnc */	"_type=abbr&a=(title=foo)&c=@(bar,(_type=b,c=@(bbb)),baz)",
					/* UrlEncT */	"t=abbr&a=(title=foo)&c=@(bar,(t=b,c=@(bbb)),baz)",
					/* UrlEncR */	"_type=abbr\n&a=(\n\ttitle=foo\n)\n&c=@(\n\tbar,\n\t(\n\t\t_type=b,\n\t\tc=@(\n\t\t\tbbb\n\t\t)\n\t),\n\tbaz\n)",
					/* MsgPack */	"83A55F74797065A461626272A16181A57469746C65A3666F6FA16393A362617282A55F74797065A162A16391A3626262A362617A",
					/* MsgPackT */	"83A174A461626272A16181A57469746C65A3666F6FA16393A362617282A174A162A16391A3626262A362617A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>abbr</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:title>foo</jp:title>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>abbr</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:title>foo</jp:title>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>abbr</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:title>foo</jp:title>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>bar</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>b</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bbb</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Abbr o) {
						assertInstanceOf(Abbr.class, o);
					}
				}
			},
			{	/* 17 */
				new ComboInput<Address>(
					"Address-1",
					Address.class,
					address(),
					/* Json */		"{_type:'address'}",
					/* JsonT */		"{t:'address'}",
					/* JsonR */		"{\n\t_type: 'address'\n}",
					/* Xml */		"<address nil='true'></address>",
					/* XmlT */		"<address nil='true'></address>",
					/* XmlR */		"<address nil='true'>\n</address>\n",
					/* XmlNs */		"<address nil='true'></address>",
					/* Html */		"<address nil='true'></address>",
					/* HtmlT */		"<address nil='true'></address>",
					/* HtmlR */		"<address nil='true'>\n</address>\n",
					/* Uon */		"(_type=address)",
					/* UonT */		"(t=address)",
					/* UonR */		"(\n\t_type=address\n)",
					/* UrlEnc */	"_type=address",
					/* UrlEncT */	"t=address",
					/* UrlEncR */	"_type=address",
					/* MsgPack */	"81A55F74797065A761646472657373",
					/* MsgPackT */	"81A174A761646472657373",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>address</jp:_type>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>address</jp:t>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>address</jp:_type>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Address o) {
						assertInstanceOf(Address.class, o);
					}
				}
			},
			{	/* 18 */
				new ComboInput<Address>(
					"Address-2",
					Address.class,
					address(""),
					/* Json */		"{_type:'address',c:['']}",
					/* JsonT */		"{t:'address',c:['']}",
					/* JsonR */		"{\n\t_type: 'address',\n\tc: [\n\t\t''\n\t]\n}",
					/* Xml */		"<address>_xE000_</address>",
					/* XmlT */		"<address>_xE000_</address>",
					/* XmlR */		"<address>_xE000_</address>\n",
					/* XmlNs */		"<address>_xE000_</address>",
					/* Html */		"<address><sp/></address>",
					/* HtmlT */		"<address><sp/></address>",
					/* HtmlR */		"<address><sp/></address>\n",
					/* Uon */		"(_type=address,c=@(''))",
					/* UonT */		"(t=address,c=@(''))",
					/* UonR */		"(\n\t_type=address,\n\tc=@(\n\t\t''\n\t)\n)",
					/* UrlEnc */	"_type=address&c=@('')",
					/* UrlEncT */	"t=address&c=@('')",
					/* UrlEncR */	"_type=address\n&c=@(\n\t''\n)",
					/* MsgPack */	"82A55F74797065A761646472657373A16391A0",
					/* MsgPackT */	"82A174A761646472657373A16391A0",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>address</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>address</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>address</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li></rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Address o) {
						assertInstanceOf(Address.class, o);
					}
				}
			},
			{	/* 19 */
				new ComboInput<Address>(
					"Address-3",
					Address.class,
					address("foo", a("bar", "baz"), a("qux", "quux")),
					/* Json */		"{_type:'address',c:['foo',{_type:'a',a:{href:'bar'},c:['baz']},{_type:'a',a:{href:'qux'},c:['quux']}]}",
					/* JsonT */		"{t:'address',c:['foo',{t:'a',a:{href:'bar'},c:['baz']},{t:'a',a:{href:'qux'},c:['quux']}]}",
					/* JsonR */		"{\n\t_type: 'address',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'bar'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'baz'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'qux'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'quux'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
					/* XmlT */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
					/* XmlR */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n",
					/* XmlNs */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
					/* Html */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
					/* HtmlT */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
					/* HtmlR */		"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n",
					/* Uon */		"(_type=address,c=@(foo,(_type=a,a=(href=bar),c=@(baz)),(_type=a,a=(href=qux),c=@(quux))))",
					/* UonT */		"(t=address,c=@(foo,(t=a,a=(href=bar),c=@(baz)),(t=a,a=(href=qux),c=@(quux))))",
					/* UonR */		"(\n\t_type=address,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=bar\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tbaz\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=qux\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tquux\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=address&c=@(foo,(_type=a,a=(href=bar),c=@(baz)),(_type=a,a=(href=qux),c=@(quux)))",
					/* UrlEncT */	"t=address&c=@(foo,(t=a,a=(href=bar),c=@(baz)),(t=a,a=(href=qux),c=@(quux)))",
					/* UrlEncR */	"_type=address\n&c=@(\n\tfoo,\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=bar\n\t\t),\n\t\tc=@(\n\t\t\tbaz\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=qux\n\t\t),\n\t\tc=@(\n\t\t\tquux\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A761646472657373A16393A3666F6F83A55F74797065A161A16181A468726566A3626172A16391A362617A83A55F74797065A161A16181A468726566A3717578A16391A471757578",
					/* MsgPackT */	"82A174A761646472657373A16393A3666F6F83A174A161A16181A468726566A3626172A16391A362617A83A174A161A16181A468726566A3717578A16391A471757578",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>address</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>bar</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>qux</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>quux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>address</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>bar</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>qux</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>quux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>address</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href>bar</jp:href>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>baz</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href>qux</jp:href>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>quux</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Address o) {
						assertInstanceOf(Address.class, o);
						assertInstanceOf(A.class, o.getChild(1));
						assertInstanceOf(A.class, o.getChild(2));
					}
				}
			},
			{	/* 20 */
				new ComboInput<Aside>(
					"Aside-1",
					Aside.class,
					aside(
						h1("header1"),p("foo")
					),
					/* Json */		"{_type:'aside',c:[{_type:'h1',c:['header1']},{_type:'p',c:['foo']}]}",
					/* JsonT */		"{t:'aside',c:[{t:'h1',c:['header1']},{t:'p',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'aside',\n\tc: [\n\t\t{\n\t\t\t_type: 'h1',\n\t\t\tc: [\n\t\t\t\t'header1'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<aside><h1>header1</h1><p>foo</p></aside>",
					/* XmlT */		"<aside><h1>header1</h1><p>foo</p></aside>",
					/* XmlR */		"<aside><h1>header1</h1><p>foo</p></aside>\n",
					/* XmlNs */		"<aside><h1>header1</h1><p>foo</p></aside>",
					/* Html */		"<aside><h1>header1</h1><p>foo</p></aside>",
					/* HtmlT */		"<aside><h1>header1</h1><p>foo</p></aside>",
					/* HtmlR */		"<aside><h1>header1</h1><p>foo</p></aside>\n",
					/* Uon */		"(_type=aside,c=@((_type=h1,c=@(header1)),(_type=p,c=@(foo))))",
					/* UonT */		"(t=aside,c=@((t=h1,c=@(header1)),(t=p,c=@(foo))))",
					/* UonR */		"(\n\t_type=aside,\n\tc=@(\n\t\t(\n\t\t\t_type=h1,\n\t\t\tc=@(\n\t\t\t\theader1\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=aside&c=@((_type=h1,c=@(header1)),(_type=p,c=@(foo)))",
					/* UrlEncT */	"t=aside&c=@((t=h1,c=@(header1)),(t=p,c=@(foo)))",
					/* UrlEncR */	"_type=aside\n&c=@(\n\t(\n\t\t_type=h1,\n\t\tc=@(\n\t\t\theader1\n\t\t)\n\t),\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A56173696465A1639282A55F74797065A26831A16391A76865616465723182A55F74797065A170A16391A3666F6F",
					/* MsgPackT */	"82A174A56173696465A1639282A174A26831A16391A76865616465723182A174A170A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>aside</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>aside</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>aside</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h1</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>header1</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Aside o) {
						assertInstanceOf(Aside.class, o);
						assertInstanceOf(H1.class, o.getChild(0));
						assertInstanceOf(P.class, o.getChild(1));
					}
				}
			},
			{	/* 21 */
				new ComboInput<Audio>(
					"Audio/Source-1",
					Audio.class,
					audio().controls(true).children(
						source("foo.ogg", "audio/ogg"),
						source("foo.mp3", "audio/mpeg")
					),
					/* Json */		"{_type:'audio',a:{controls:'controls'},c:[{_type:'source',a:{src:'foo.ogg',type:'audio/ogg'}},{_type:'source',a:{src:'foo.mp3',type:'audio/mpeg'}}]}",
					/* JsonT */		"{t:'audio',a:{controls:'controls'},c:[{t:'source',a:{src:'foo.ogg',type:'audio/ogg'}},{t:'source',a:{src:'foo.mp3',type:'audio/mpeg'}}]}",
					/* JsonR */		"{\n\t_type: 'audio',\n\ta: {\n\t\tcontrols: 'controls'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'source',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.ogg',\n\t\t\t\ttype: 'audio/ogg'\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'source',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.mp3',\n\t\t\t\ttype: 'audio/mpeg'\n\t\t\t}\n\t\t}\n\t]\n}",
					/* Xml */		"<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
					/* XmlT */		"<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
					/* XmlR */		"<audio controls='controls'>\n\t<source src='foo.ogg' type='audio/ogg'/>\n\t<source src='foo.mp3' type='audio/mpeg'/>\n</audio>\n",
					/* XmlNs */		"<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
					/* Html */		"<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
					/* HtmlT */		"<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
					/* HtmlR */		"<audio controls='controls'>\n\t<source src='foo.ogg' type='audio/ogg'/>\n\t<source src='foo.mp3' type='audio/mpeg'/>\n</audio>\n",
					/* Uon */		"(_type=audio,a=(controls=controls),c=@((_type=source,a=(src=foo.ogg,type=audio/ogg)),(_type=source,a=(src=foo.mp3,type=audio/mpeg))))",
					/* UonT */		"(t=audio,a=(controls=controls),c=@((t=source,a=(src=foo.ogg,type=audio/ogg)),(t=source,a=(src=foo.mp3,type=audio/mpeg))))",
					/* UonR */		"(\n\t_type=audio,\n\ta=(\n\t\tcontrols=controls\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=source,\n\t\t\ta=(\n\t\t\t\tsrc=foo.ogg,\n\t\t\t\ttype=audio/ogg\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=source,\n\t\t\ta=(\n\t\t\t\tsrc=foo.mp3,\n\t\t\t\ttype=audio/mpeg\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=audio&a=(controls=controls)&c=@((_type=source,a=(src=foo.ogg,type=audio/ogg)),(_type=source,a=(src=foo.mp3,type=audio/mpeg)))",
					/* UrlEncT */	"t=audio&a=(controls=controls)&c=@((t=source,a=(src=foo.ogg,type=audio/ogg)),(t=source,a=(src=foo.mp3,type=audio/mpeg)))",
					/* UrlEncR */	"_type=audio\n&a=(\n\tcontrols=controls\n)\n&c=@(\n\t(\n\t\t_type=source,\n\t\ta=(\n\t\t\tsrc=foo.ogg,\n\t\t\ttype=audio/ogg\n\t\t)\n\t),\n\t(\n\t\t_type=source,\n\t\ta=(\n\t\t\tsrc=foo.mp3,\n\t\t\ttype=audio/mpeg\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A5617564696FA16181A8636F6E74726F6C73A8636F6E74726F6C73A1639282A55F74797065A6736F75726365A16182A3737263A7666F6F2E6F6767A474797065A9617564696F2F6F676782A55F74797065A6736F75726365A16182A3737263A7666F6F2E6D7033A474797065AA617564696F2F6D706567",
					/* MsgPackT */	"83A174A5617564696FA16181A8636F6E74726F6C73A8636F6E74726F6C73A1639282A174A6736F75726365A16182A3737263A7666F6F2E6F6767A474797065A9617564696F2F6F676782A174A6736F75726365A16182A3737263A7666F6F2E6D7033A474797065AA617564696F2F6D706567",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>audio</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>source</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.ogg</jp:src>\n<jp:type>audio/ogg</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>source</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp3</jp:src>\n<jp:type>audio/mpeg</jp:type>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>audio</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>source</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.ogg</jp:src>\n<jp:type>audio/ogg</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>source</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp3</jp:src>\n<jp:type>audio/mpeg</jp:type>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>audio</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:controls>controls</jp:controls>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>source</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.ogg</jp:src>\n            <jp:type>audio/ogg</jp:type>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>source</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.mp3</jp:src>\n            <jp:type>audio/mpeg</jp:type>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Audio o) {
						assertInstanceOf(Audio.class, o);
						assertInstanceOf(Source.class, o.getChild(0));
						assertInstanceOf(Source.class, o.getChild(1));
					}
				}
			},
			{	/* 22 */
				new ComboInput<P>(
					"Bdi-1",
					P.class,
					p("foo", bdi("إيان"), "bar"),
					/* Json */		"{_type:'p',c:['foo',{_type:'bdi',c:'إيان'},'bar']}",
					/* JsonT */		"{t:'p',c:['foo',{t:'bdi',c:'إيان'},'bar']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'bdi',\n\t\t\tc: 'إيان'\n\t\t},\n\t\t'bar'\n\t]\n}",
					/* Xml */		"<p>foo<bdi>إيان</bdi>bar</p>",
					/* XmlT */		"<p>foo<bdi>إيان</bdi>bar</p>",
					/* XmlR */		"<p>foo<bdi>إيان</bdi>bar</p>\n",
					/* XmlNs */		"<p>foo<bdi>إيان</bdi>bar</p>",
					/* Html */		"<p>foo<bdi>إيان</bdi>bar</p>",
					/* HtmlT */		"<p>foo<bdi>إيان</bdi>bar</p>",
					/* HtmlR */		"<p>foo<bdi>إيان</bdi>bar</p>\n",
					/* Uon */		"(_type=p,c=@(foo,(_type=bdi,c=إيان),bar))",
					/* UonT */		"(t=p,c=@(foo,(t=bdi,c=إيان),bar))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=bdi,\n\t\t\tc=إيان\n\t\t),\n\t\tbar\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo,(_type=bdi,c=%D8%A5%D9%8A%D8%A7%D9%86),bar)",
					/* UrlEncT */	"t=p&c=@(foo,(t=bdi,c=%D8%A5%D9%8A%D8%A7%D9%86),bar)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=bdi,\n\t\tc=%D8%A5%D9%8A%D8%A7%D9%86\n\t),\n\tbar\n)",
					/* MsgPack */	"82A55F74797065A170A16393A3666F6F82A55F74797065A3626469A163A8D8A5D98AD8A7D986A3626172",
					/* MsgPackT */	"82A174A170A16393A3666F6F82A174A3626469A163A8D8A5D98AD8A7D986A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>bdi</jp:_type>\n<jp:c>إيان</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>bdi</jp:t>\n<jp:c>إيان</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>bdi</jp:_type>\n          <jp:c>إيان</jp:c>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Bdi.class, o.getChild(1));
					}
				}
			},
			{	/* 23 */
				new ComboInput<P>(
					"Bdo-1",
					P.class,
					p("foo", bdo("rtl", "baz"), "bar"),
					/* Json */		"{_type:'p',c:['foo',{_type:'bdo',a:{dir:'rtl'},c:['baz']},'bar']}",
					/* JsonT */		"{t:'p',c:['foo',{t:'bdo',a:{dir:'rtl'},c:['baz']},'bar']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'bdo',\n\t\t\ta: {\n\t\t\t\tdir: 'rtl'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'baz'\n\t\t\t]\n\t\t},\n\t\t'bar'\n\t]\n}",
					/* Xml */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
					/* XmlT */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
					/* XmlR */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n",
					/* XmlNs */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
					/* Html */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
					/* HtmlT */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
					/* HtmlR */		"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n",
					/* Uon */		"(_type=p,c=@(foo,(_type=bdo,a=(dir=rtl),c=@(baz)),bar))",
					/* UonT */		"(t=p,c=@(foo,(t=bdo,a=(dir=rtl),c=@(baz)),bar))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=bdo,\n\t\t\ta=(\n\t\t\t\tdir=rtl\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tbaz\n\t\t\t)\n\t\t),\n\t\tbar\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo,(_type=bdo,a=(dir=rtl),c=@(baz)),bar)",
					/* UrlEncT */	"t=p&c=@(foo,(t=bdo,a=(dir=rtl),c=@(baz)),bar)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=bdo,\n\t\ta=(\n\t\t\tdir=rtl\n\t\t),\n\t\tc=@(\n\t\t\tbaz\n\t\t)\n\t),\n\tbar\n)",
					/* MsgPack */	"82A55F74797065A170A16393A3666F6F83A55F74797065A362646FA16181A3646972A372746CA16391A362617AA3626172",
					/* MsgPackT */	"82A174A170A16393A3666F6F83A174A362646FA16181A3646972A372746CA16391A362617AA3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>bdo</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:dir>rtl</jp:dir>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>bdo</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:dir>rtl</jp:dir>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>bdo</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:dir>rtl</jp:dir>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>baz</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Bdo.class, o.getChild(1));
					}
				}
			},
			{	/* 24 */
				new ComboInput<Blockquote>(
					"Blockquote-1",
					Blockquote.class,
					blockquote("foo"),
					/* Json */		"{_type:'blockquote',c:['foo']}",
					/* JsonT */		"{t:'blockquote',c:['foo']}",
					/* JsonR */		"{\n\t_type: 'blockquote',\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<blockquote>foo</blockquote>",
					/* XmlT */		"<blockquote>foo</blockquote>",
					/* XmlR */		"<blockquote>foo</blockquote>\n",
					/* XmlNs */		"<blockquote>foo</blockquote>",
					/* Html */		"<blockquote>foo</blockquote>",
					/* HtmlT */		"<blockquote>foo</blockquote>",
					/* HtmlR */		"<blockquote>foo</blockquote>\n",
					/* Uon */		"(_type=blockquote,c=@(foo))",
					/* UonT */		"(t=blockquote,c=@(foo))",
					/* UonR */		"(\n\t_type=blockquote,\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=blockquote&c=@(foo)",
					/* UrlEncT */	"t=blockquote&c=@(foo)",
					/* UrlEncR */	"_type=blockquote\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"82A55F74797065AA626C6F636B71756F7465A16391A3666F6F",
					/* MsgPackT */	"82A174AA626C6F636B71756F7465A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>blockquote</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>blockquote</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>blockquote</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Blockquote o) {
						assertInstanceOf(Blockquote.class, o);
					}
				}
			},
			{	/* 25 */
				new ComboInput<Br>(
					"Br-1",
					Br.class,
					br(),
					/* Json */		"{_type:'br'}",
					/* JsonT */		"{t:'br'}",
					/* JsonR */		"{\n\t_type: 'br'\n}",
					/* Xml */		"<br/>",
					/* XmlT */		"<br/>",
					/* XmlR */		"<br/>\n",
					/* XmlNs */		"<br/>",
					/* Html */		"<br/>",
					/* HtmlT */		"<br/>",
					/* HtmlR */		"<br/>\n",
					/* Uon */		"(_type=br)",
					/* UonT */		"(t=br)",
					/* UonR */		"(\n\t_type=br\n)",
					/* UrlEnc */	"_type=br",
					/* UrlEncT */	"t=br",
					/* UrlEncR */	"_type=br",
					/* MsgPack */	"81A55F74797065A26272",
					/* MsgPackT */	"81A174A26272",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>br</jp:_type>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>br</jp:t>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>br</jp:_type>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Br o) {
						assertInstanceOf(Br.class, o);
					}
				}
			},
			{	/* 26 */
				new ComboInput<P>(
					"Br-2",
					P.class,
					p(br()),
					/* Json */		"{_type:'p',c:[{_type:'br'}]}",
					/* JsonT */		"{t:'p',c:[{t:'br'}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'br'\n\t\t}\n\t]\n}",
					/* Xml */		"<p><br/></p>",
					/* XmlT */		"<p><br/></p>",
					/* XmlR */		"<p><br/></p>\n",
					/* XmlNs */		"<p><br/></p>",
					/* Html */		"<p><br/></p>",
					/* HtmlT */		"<p><br/></p>",
					/* HtmlR */		"<p><br/></p>\n",
					/* Uon */		"(_type=p,c=@((_type=br)))",
					/* UonT */		"(t=p,c=@((t=br)))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=br\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=br))",
					/* UrlEncT */	"t=p&c=@((t=br))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=br\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639181A55F74797065A26272",
					/* MsgPackT */	"82A174A170A1639181A174A26272",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>br</jp:_type>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>br</jp:t>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>br</jp:_type>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Br.class, o.getChild(0));
					}
				}
			},
			{	/* 27 */
				new ComboInput<Button>(
					"Button-1",
					Button.class,
					button("button", "foo"),
					/* Json */		"{_type:'button',a:{type:'button'},c:['foo']}",
					/* JsonT */		"{t:'button',a:{type:'button'},c:['foo']}",
					/* JsonR */		"{\n\t_type: 'button',\n\ta: {\n\t\ttype: 'button'\n\t},\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<button type='button'>foo</button>",
					/* XmlT */		"<button type='button'>foo</button>",
					/* XmlR */		"<button type='button'>foo</button>\n",
					/* XmlNs */		"<button type='button'>foo</button>",
					/* Html */		"<button type='button'>foo</button>",
					/* HtmlT */		"<button type='button'>foo</button>",
					/* HtmlR */		"<button type='button'>foo</button>\n",
					/* Uon */		"(_type=button,a=(type=button),c=@(foo))",
					/* UonT */		"(t=button,a=(type=button),c=@(foo))",
					/* UonR */		"(\n\t_type=button,\n\ta=(\n\t\ttype=button\n\t),\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=button&a=(type=button)&c=@(foo)",
					/* UrlEncT */	"t=button&a=(type=button)&c=@(foo)",
					/* UrlEncR */	"_type=button\n&a=(\n\ttype=button\n)\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"83A55F74797065A6627574746F6EA16181A474797065A6627574746F6EA16391A3666F6F",
					/* MsgPackT */	"83A174A6627574746F6EA16181A474797065A6627574746F6EA16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>button</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>button</jp:type>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>button</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>button</jp:type>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>button</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:type>button</jp:type>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Button o) {
						assertInstanceOf(Button.class, o);
					}
				}
			},
			{	/* 28 */
				new ComboInput<Canvas>(
					"Canvas-1",
					Canvas.class,
					canvas(100, 200),
					/* Json */		"{_type:'canvas',a:{width:100,height:200}}",
					/* JsonT */		"{t:'canvas',a:{width:100,height:200}}",
					/* JsonR */		"{\n\t_type: 'canvas',\n\ta: {\n\t\twidth: 100,\n\t\theight: 200\n\t}\n}",
					/* Xml */		"<canvas width='100' height='200' nil='true'></canvas>",
					/* XmlT */		"<canvas width='100' height='200' nil='true'></canvas>",
					/* XmlR */		"<canvas width='100' height='200' nil='true'>\n</canvas>\n",
					/* XmlNs */		"<canvas width='100' height='200' nil='true'></canvas>",
					/* Html */		"<canvas width='100' height='200' nil='true'></canvas>",
					/* HtmlT */		"<canvas width='100' height='200' nil='true'></canvas>",
					/* HtmlR */		"<canvas width='100' height='200' nil='true'>\n</canvas>\n",
					/* Uon */		"(_type=canvas,a=(width=100,height=200))",
					/* UonT */		"(t=canvas,a=(width=100,height=200))",
					/* UonR */		"(\n\t_type=canvas,\n\ta=(\n\t\twidth=100,\n\t\theight=200\n\t)\n)",
					/* UrlEnc */	"_type=canvas&a=(width=100,height=200)",
					/* UrlEncT */	"t=canvas&a=(width=100,height=200)",
					/* UrlEncR */	"_type=canvas\n&a=(\n\twidth=100,\n\theight=200\n)",
					/* MsgPack */	"82A55F74797065A663616E766173A16182A5776964746864A6686569676874D100C8",
					/* MsgPackT */	"82A174A663616E766173A16182A5776964746864A6686569676874D100C8",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>canvas</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>canvas</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>canvas</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:width>100</jp:width>\n      <jp:height>200</jp:height>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Canvas o) {
						assertInstanceOf(Canvas.class, o);
					}
				}
			},
			{	/* 29 */
				new ComboInput<P>(
					"Cite-1",
					P.class,
					p(cite("foo")),
					/* Json */		"{_type:'p',c:[{_type:'cite',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'cite',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'cite',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><cite>foo</cite></p>",
					/* XmlT */		"<p><cite>foo</cite></p>",
					/* XmlR */		"<p><cite>foo</cite></p>\n",
					/* XmlNs */		"<p><cite>foo</cite></p>",
					/* Html */		"<p><cite>foo</cite></p>",
					/* HtmlT */		"<p><cite>foo</cite></p>",
					/* HtmlR */		"<p><cite>foo</cite></p>\n",
					/* Uon */		"(_type=p,c=@((_type=cite,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=cite,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=cite,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=cite,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=cite,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=cite,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A463697465A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A463697465A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>cite</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>cite</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>cite</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Cite.class, o.getChild(0));
					}
				}
			},
			{	/* 30 */
				new ComboInput<Code>(
					"Code-1",
					Code.class,
					code("foo\n\tbar"),
					/* Json */		"{_type:'code',c:['foo\\n\\tbar']}",
					/* JsonT */		"{t:'code',c:['foo\\n\\tbar']}",
					/* JsonR */		"{\n\t_type: 'code',\n\tc: [\n\t\t'foo\\n\\tbar'\n\t]\n}",
					/* Xml */		"<code>foo&#x000a;&#x0009;bar</code>",
					/* XmlT */		"<code>foo&#x000a;&#x0009;bar</code>",
					/* XmlR */		"<code>foo&#x000a;&#x0009;bar</code>\n",
					/* XmlNs */		"<code>foo&#x000a;&#x0009;bar</code>",
					/* Html */		"<code>foo<br/><sp>&#x2003;</sp>bar</code>",
					/* HtmlT */		"<code>foo<br/><sp>&#x2003;</sp>bar</code>",
					/* HtmlR */		"<code>foo<br/><sp>&#x2003;</sp>bar</code>\n",
					/* Uon */		"(_type=code,c=@('foo\n\tbar'))",
					/* UonT */		"(t=code,c=@('foo\n\tbar'))",
					/* UonR */		"(\n\t_type=code,\n\tc=@(\n\t\t'foo\n\tbar'\n\t)\n)",
					/* UrlEnc */	"_type=code&c=@('foo%0A%09bar')",
					/* UrlEncT */	"t=code&c=@('foo%0A%09bar')",
					/* UrlEncR */	"_type=code\n&c=@(\n\t'foo%0A%09bar'\n)",
					/* MsgPack */	"82A55F74797065A4636F6465A16391A8666F6F0A09626172",
					/* MsgPackT */	"82A174A4636F6465A16391A8666F6F0A09626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>code</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo_x000A__x0009_bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>code</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo_x000A__x0009_bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>code</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo_x000A__x0009_bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Code o) {
						assertInstanceOf(Code.class, o);
					}
				}
			},
			{	/* 31 */
				new ComboInput<Datalist>(
					"Datalist-1",
					Datalist.class,
					datalist("foo",
						option("One"),
						option("Two")
					),
					/* Json */		"{_type:'datalist',a:{id:'foo'},c:[{_type:'option',c:'One'},{_type:'option',c:'Two'}]}",
					/* JsonT */		"{t:'datalist',a:{id:'foo'},c:[{t:'option',c:'One'},{t:'option',c:'Two'}]}",
					/* JsonR */		"{\n\t_type: 'datalist',\n\ta: {\n\t\tid: 'foo'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'option',\n\t\t\tc: 'One'\n\t\t},\n\t\t{\n\t\t\t_type: 'option',\n\t\t\tc: 'Two'\n\t\t}\n\t]\n}",
					/* Xml */		"<datalist id='foo'><option>One</option><option>Two</option></datalist>",
					/* XmlT */		"<datalist id='foo'><option>One</option><option>Two</option></datalist>",
					/* XmlR */		"<datalist id='foo'>\n\t<option>One</option>\n\t<option>Two</option>\n</datalist>\n",
					/* XmlNs */		"<datalist id='foo'><option>One</option><option>Two</option></datalist>",
					/* Html */		"<datalist id='foo'><option>One</option><option>Two</option></datalist>",
					/* HtmlT */		"<datalist id='foo'><option>One</option><option>Two</option></datalist>",
					/* HtmlR */		"<datalist id='foo'>\n\t<option>One</option>\n\t<option>Two</option>\n</datalist>\n",
					/* Uon */		"(_type=datalist,a=(id=foo),c=@((_type=option,c=One),(_type=option,c=Two)))",
					/* UonT */		"(t=datalist,a=(id=foo),c=@((t=option,c=One),(t=option,c=Two)))",
					/* UonR */		"(\n\t_type=datalist,\n\ta=(\n\t\tid=foo\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=option,\n\t\t\tc=One\n\t\t),\n\t\t(\n\t\t\t_type=option,\n\t\t\tc=Two\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=datalist&a=(id=foo)&c=@((_type=option,c=One),(_type=option,c=Two))",
					/* UrlEncT */	"t=datalist&a=(id=foo)&c=@((t=option,c=One),(t=option,c=Two))",
					/* UrlEncR */	"_type=datalist\n&a=(\n\tid=foo\n)\n&c=@(\n\t(\n\t\t_type=option,\n\t\tc=One\n\t),\n\t(\n\t\t_type=option,\n\t\tc=Two\n\t)\n)",
					/* MsgPack */	"83A55F74797065A8646174616C697374A16181A26964A3666F6FA1639282A55F74797065A66F7074696F6EA163A34F6E6582A55F74797065A66F7074696F6EA163A354776F",
					/* MsgPackT */	"83A174A8646174616C697374A16181A26964A3666F6FA1639282A174A66F7074696F6EA163A34F6E6582A174A66F7074696F6EA163A354776F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>datalist</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>option</jp:_type>\n<jp:c>One</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>option</jp:_type>\n<jp:c>Two</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>datalist</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>option</jp:t>\n<jp:c>One</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>option</jp:t>\n<jp:c>Two</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>datalist</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:id>foo</jp:id>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>option</jp:_type>\n          <jp:c>One</jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>option</jp:_type>\n          <jp:c>Two</jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Datalist o) {
						assertInstanceOf(Datalist.class, o);
						assertInstanceOf(Option.class, o.getChild(0));
						assertInstanceOf(Option.class, o.getChild(1));
					}
				}
			},
			{	/* 32 */
				new ComboInput<Dl>(
					"Dl/Dt/Dd",
					Dl.class,
					dl(
						dt("foo"),
						dd("bar")
					),
					/* Json */		"{_type:'dl',c:[{_type:'dt',c:['foo']},{_type:'dd',c:['bar']}]}",
					/* JsonT */		"{t:'dl',c:[{t:'dt',c:['foo']},{t:'dd',c:['bar']}]}",
					/* JsonR */		"{\n\t_type: 'dl',\n\tc: [\n\t\t{\n\t\t\t_type: 'dt',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'dd',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<dl><dt>foo</dt><dd>bar</dd></dl>",
					/* XmlT */		"<dl><dt>foo</dt><dd>bar</dd></dl>",
					/* XmlR */		"<dl>\n\t<dt>foo</dt>\n\t<dd>bar</dd>\n</dl>\n",
					/* XmlNs */		"<dl><dt>foo</dt><dd>bar</dd></dl>",
					/* Html */		"<dl><dt>foo</dt><dd>bar</dd></dl>",
					/* HtmlT */		"<dl><dt>foo</dt><dd>bar</dd></dl>",
					/* HtmlR */		"<dl>\n\t<dt>foo</dt>\n\t<dd>bar</dd>\n</dl>\n",
					/* Uon */		"(_type=dl,c=@((_type=dt,c=@(foo)),(_type=dd,c=@(bar))))",
					/* UonT */		"(t=dl,c=@((t=dt,c=@(foo)),(t=dd,c=@(bar))))",
					/* UonR */		"(\n\t_type=dl,\n\tc=@(\n\t\t(\n\t\t\t_type=dt,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=dd,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=dl&c=@((_type=dt,c=@(foo)),(_type=dd,c=@(bar)))",
					/* UrlEncT */	"t=dl&c=@((t=dt,c=@(foo)),(t=dd,c=@(bar)))",
					/* UrlEncR */	"_type=dl\n&c=@(\n\t(\n\t\t_type=dt,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t),\n\t(\n\t\t_type=dd,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A2646CA1639282A55F74797065A26474A16391A3666F6F82A55F74797065A26464A16391A3626172",
					/* MsgPackT */	"82A174A2646CA1639282A174A26474A16391A3666F6F82A174A26464A16391A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>dl</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>dt</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>dd</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>dl</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>dt</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>dd</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>dl</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>dt</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>dd</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Dl o) {
						assertInstanceOf(Dl.class, o);
						assertInstanceOf(Dt.class, o.getChild(0));
						assertInstanceOf(Dd.class, o.getChild(1));
					}
				}
			},
			{	/* 33 */
				new ComboInput<P>(
					"Del/Ins",
					P.class,
					p(del("foo",btag,"bar"),ins("baz")),
					/* Json */		"{_type:'p',c:[{_type:'del',c:['foo',{_type:'b',c:['bbb']},'bar']},{_type:'ins',c:['baz']}]}",
					/* JsonT */		"{t:'p',c:[{t:'del',c:['foo',{t:'b',c:['bbb']},'bar']},{t:'ins',c:['baz']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'del',\n\t\t\tc: [\n\t\t\t\t'foo',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'ins',\n\t\t\tc: [\n\t\t\t\t'baz'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
					/* XmlT */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
					/* XmlR */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n",
					/* XmlNs */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
					/* Html */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
					/* HtmlT */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
					/* HtmlR */		"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n",
					/* Uon */		"(_type=p,c=@((_type=del,c=@(foo,(_type=b,c=@(bbb)),bar)),(_type=ins,c=@(baz))))",
					/* UonT */		"(t=p,c=@((t=del,c=@(foo,(t=b,c=@(bbb)),bar)),(t=ins,c=@(baz))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=del,\n\t\t\tc=@(\n\t\t\t\tfoo,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=ins,\n\t\t\tc=@(\n\t\t\t\tbaz\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=del,c=@(foo,(_type=b,c=@(bbb)),bar)),(_type=ins,c=@(baz)))",
					/* UrlEncT */	"t=p&c=@((t=del,c=@(foo,(t=b,c=@(bbb)),bar)),(t=ins,c=@(baz)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=del,\n\t\tc=@(\n\t\t\tfoo,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\tbar\n\t\t)\n\t),\n\t(\n\t\t_type=ins,\n\t\tc=@(\n\t\t\tbaz\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639282A55F74797065A364656CA16393A3666F6F82A55F74797065A162A16391A3626262A362617282A55F74797065A3696E73A16391A362617A",
					/* MsgPackT */	"82A174A170A1639282A174A364656CA16393A3666F6F82A174A162A16391A3626262A362617282A174A3696E73A16391A362617A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>del</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>ins</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>del</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>ins</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>del</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>ins</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>baz</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Del.class, o.getChild(0));
						assertInstanceOf(B.class, o.getChild(Del.class, 0).getChild(1));
						assertInstanceOf(Ins.class, o.getChild(1));
					}
				}
			},
			{	/* 34 */
				new ComboInput<P>(
					"Dfn",
					P.class,
					p(dfn("foo")),
					/* Json */		"{_type:'p',c:[{_type:'dfn',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'dfn',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'dfn',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><dfn>foo</dfn></p>",
					/* XmlT */		"<p><dfn>foo</dfn></p>",
					/* XmlR */		"<p><dfn>foo</dfn></p>\n",
					/* XmlNs */		"<p><dfn>foo</dfn></p>",
					/* Html */		"<p><dfn>foo</dfn></p>",
					/* HtmlT */		"<p><dfn>foo</dfn></p>",
					/* HtmlR */		"<p><dfn>foo</dfn></p>\n",
					/* Uon */		"(_type=p,c=@((_type=dfn,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=dfn,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=dfn,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=dfn,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=dfn,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=dfn,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A364666EA16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A364666EA16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>dfn</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>dfn</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>dfn</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Dfn.class, o.getChild(0));
					}
				}
			},
			{	/* 35 */
				new ComboInput<Div>(
					"Div",
					Div.class,
					div("foo",btag,"bar"),
					/* Json */		"{_type:'div',c:['foo',{_type:'b',c:['bbb']},'bar']}",
					/* JsonT */		"{t:'div',c:['foo',{t:'b',c:['bbb']},'bar']}",
					/* JsonR */		"{\n\t_type: 'div',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'b',\n\t\t\tc: [\n\t\t\t\t'bbb'\n\t\t\t]\n\t\t},\n\t\t'bar'\n\t]\n}",
					/* Xml */		"<div>foo<b>bbb</b>bar</div>",
					/* XmlT */		"<div>foo<b>bbb</b>bar</div>",
					/* XmlR */		"<div>foo<b>bbb</b>bar</div>\n",
					/* XmlNs */		"<div>foo<b>bbb</b>bar</div>",
					/* Html */		"<div>foo<b>bbb</b>bar</div>",
					/* HtmlT */		"<div>foo<b>bbb</b>bar</div>",
					/* HtmlR */		"<div>foo<b>bbb</b>bar</div>\n",
					/* Uon */		"(_type=div,c=@(foo,(_type=b,c=@(bbb)),bar))",
					/* UonT */		"(t=div,c=@(foo,(t=b,c=@(bbb)),bar))",
					/* UonR */		"(\n\t_type=div,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=b,\n\t\t\tc=@(\n\t\t\t\tbbb\n\t\t\t)\n\t\t),\n\t\tbar\n\t)\n)",
					/* UrlEnc */	"_type=div&c=@(foo,(_type=b,c=@(bbb)),bar)",
					/* UrlEncT */	"t=div&c=@(foo,(t=b,c=@(bbb)),bar)",
					/* UrlEncR */	"_type=div\n&c=@(\n\tfoo,\n\t(\n\t\t_type=b,\n\t\tc=@(\n\t\t\tbbb\n\t\t)\n\t),\n\tbar\n)",
					/* MsgPack */	"82A55F74797065A3646976A16393A3666F6F82A55F74797065A162A16391A3626262A3626172",
					/* MsgPackT */	"82A174A3646976A16393A3666F6F82A174A162A16391A3626262A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>div</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>div</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>div</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>b</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bbb</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Div o) {
						assertInstanceOf(Div.class, o);
						assertInstanceOf(B.class, o.getChild(1));
					}
				}
			},
			{	/* 36 */
				new ComboInput<P>(
					"Em",
					P.class,
					p("foo",em("bar"),"baz"),
					/* Json */		"{_type:'p',c:['foo',{_type:'em',c:['bar']},'baz']}",
					/* JsonT */		"{t:'p',c:['foo',{t:'em',c:['bar']},'baz']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'em',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}",
					/* Xml */		"<p>foo<em>bar</em>baz</p>",
					/* XmlT */		"<p>foo<em>bar</em>baz</p>",
					/* XmlR */		"<p>foo<em>bar</em>baz</p>\n",
					/* XmlNs */		"<p>foo<em>bar</em>baz</p>",
					/* Html */		"<p>foo<em>bar</em>baz</p>",
					/* HtmlT */		"<p>foo<em>bar</em>baz</p>",
					/* HtmlR */		"<p>foo<em>bar</em>baz</p>\n",
					/* Uon */		"(_type=p,c=@(foo,(_type=em,c=@(bar)),baz))",
					/* UonT */		"(t=p,c=@(foo,(t=em,c=@(bar)),baz))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=em,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo,(_type=em,c=@(bar)),baz)",
					/* UrlEncT */	"t=p&c=@(foo,(t=em,c=@(bar)),baz)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=em,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\tbaz\n)",
					/* MsgPack */	"82A55F74797065A170A16393A3666F6F82A55F74797065A2656DA16391A3626172A362617A",
					/* MsgPackT */	"82A174A170A16393A3666F6F82A174A2656DA16391A3626172A362617A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>em</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>em</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>em</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Em.class, o.getChild(1));
					}
				}
			},
			{	/* 37 */
				new ComboInput<Embed>(
					"Embed",
					Embed.class,
					embed("foo.swf"),
					/* Json */		"{_type:'embed',a:{src:'foo.swf'}}",
					/* JsonT */		"{t:'embed',a:{src:'foo.swf'}}",
					/* JsonR */		"{\n\t_type: 'embed',\n\ta: {\n\t\tsrc: 'foo.swf'\n\t}\n}",
					/* Xml */		"<embed src='foo.swf'/>",
					/* XmlT */		"<embed src='foo.swf'/>",
					/* XmlR */		"<embed src='foo.swf'/>\n",
					/* XmlNs */		"<embed src='foo.swf'/>",
					/* Html */		"<embed src='foo.swf'/>",
					/* HtmlT */		"<embed src='foo.swf'/>",
					/* HtmlR */		"<embed src='foo.swf'/>\n",
					/* Uon */		"(_type=embed,a=(src=foo.swf))",
					/* UonT */		"(t=embed,a=(src=foo.swf))",
					/* UonR */		"(\n\t_type=embed,\n\ta=(\n\t\tsrc=foo.swf\n\t)\n)",
					/* UrlEnc */	"_type=embed&a=(src=foo.swf)",
					/* UrlEncT */	"t=embed&a=(src=foo.swf)",
					/* UrlEncR */	"_type=embed\n&a=(\n\tsrc=foo.swf\n)",
					/* MsgPack */	"82A55F74797065A5656D626564A16181A3737263A7666F6F2E737766",
					/* MsgPackT */	"82A174A5656D626564A16181A3737263A7666F6F2E737766",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>embed</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.swf</jp:src>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>embed</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.swf</jp:src>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>embed</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:src>foo.swf</jp:src>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Embed o) {
						assertInstanceOf(Embed.class, o);
					}
				}
			},
			{	/* 38 */
				new ComboInput<Form>(
					"Form/Fieldset/Legend/Input/Keygen/Label",
					Form.class,
					form("bar",
						fieldset(
							legend("foo:"),
							"Name:", input("text"), br(),
							"Email:", input("text"), br(),
							"X:", keygen().name("X"),
							label("label")._for("Name")
						)
					),
					/* Json */		"{_type:'form',a:{action:'bar'},c:[{_type:'fieldset',c:[{_type:'legend',c:['foo:']},'Name:',{_type:'input',a:{type:'text'}},{_type:'br'},'Email:',{_type:'input',a:{type:'text'}},{_type:'br'},'X:',{_type:'keygen',a:{name:'X'}},{_type:'label',a:{'for':'Name'},c:['label']}]}]}",
					/* JsonT */		"{t:'form',a:{action:'bar'},c:[{t:'fieldset',c:[{t:'legend',c:['foo:']},'Name:',{t:'input',a:{type:'text'}},{t:'br'},'Email:',{t:'input',a:{type:'text'}},{t:'br'},'X:',{t:'keygen',a:{name:'X'}},{t:'label',a:{'for':'Name'},c:['label']}]}]}",
					/* JsonR */		"{\n\t_type: 'form',\n\ta: {\n\t\taction: 'bar'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'fieldset',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'legend',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'foo:'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'Name:',\n\t\t\t\t{\n\t\t\t\t\t_type: 'input',\n\t\t\t\t\ta: {\n\t\t\t\t\t\ttype: 'text'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'br'\n\t\t\t\t},\n\t\t\t\t'Email:',\n\t\t\t\t{\n\t\t\t\t\t_type: 'input',\n\t\t\t\t\ta: {\n\t\t\t\t\t\ttype: 'text'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'br'\n\t\t\t\t},\n\t\t\t\t'X:',\n\t\t\t\t{\n\t\t\t\t\t_type: 'keygen',\n\t\t\t\t\ta: {\n\t\t\t\t\t\tname: 'X'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'label',\n\t\t\t\t\ta: {\n\t\t\t\t\t\t'for': 'Name'\n\t\t\t\t\t},\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'label'\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
					/* XmlT */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
					/* XmlR */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>\n",
					/* XmlNs */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
					/* Html */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
					/* HtmlT */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
					/* HtmlR */		"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>\n",
					/* Uon */		"(_type=form,a=(action=bar),c=@((_type=fieldset,c=@((_type=legend,c=@(foo:)),Name:,(_type=input,a=(type=text)),(_type=br),Email:,(_type=input,a=(type=text)),(_type=br),X:,(_type=keygen,a=(name=X)),(_type=label,a=(for=Name),c=@(label))))))",
					/* UonT */		"(t=form,a=(action=bar),c=@((t=fieldset,c=@((t=legend,c=@(foo:)),Name:,(t=input,a=(type=text)),(t=br),Email:,(t=input,a=(type=text)),(t=br),X:,(t=keygen,a=(name=X)),(t=label,a=(for=Name),c=@(label))))))",
					/* UonR */		"(\n\t_type=form,\n\ta=(\n\t\taction=bar\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=fieldset,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=legend,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tfoo:\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tName:,\n\t\t\t\t(\n\t\t\t\t\t_type=input,\n\t\t\t\t\ta=(\n\t\t\t\t\t\ttype=text\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=br\n\t\t\t\t),\n\t\t\t\tEmail:,\n\t\t\t\t(\n\t\t\t\t\t_type=input,\n\t\t\t\t\ta=(\n\t\t\t\t\t\ttype=text\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=br\n\t\t\t\t),\n\t\t\t\tX:,\n\t\t\t\t(\n\t\t\t\t\t_type=keygen,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tname=X\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=label,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tfor=Name\n\t\t\t\t\t),\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tlabel\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=form&a=(action=bar)&c=@((_type=fieldset,c=@((_type=legend,c=@(foo:)),Name:,(_type=input,a=(type=text)),(_type=br),Email:,(_type=input,a=(type=text)),(_type=br),X:,(_type=keygen,a=(name=X)),(_type=label,a=(for=Name),c=@(label)))))",
					/* UrlEncT */	"t=form&a=(action=bar)&c=@((t=fieldset,c=@((t=legend,c=@(foo:)),Name:,(t=input,a=(type=text)),(t=br),Email:,(t=input,a=(type=text)),(t=br),X:,(t=keygen,a=(name=X)),(t=label,a=(for=Name),c=@(label)))))",
					/* UrlEncR */	"_type=form\n&a=(\n\taction=bar\n)\n&c=@(\n\t(\n\t\t_type=fieldset,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=legend,\n\t\t\t\tc=@(\n\t\t\t\t\tfoo:\n\t\t\t\t)\n\t\t\t),\n\t\t\tName:,\n\t\t\t(\n\t\t\t\t_type=input,\n\t\t\t\ta=(\n\t\t\t\t\ttype=text\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=br\n\t\t\t),\n\t\t\tEmail:,\n\t\t\t(\n\t\t\t\t_type=input,\n\t\t\t\ta=(\n\t\t\t\t\ttype=text\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=br\n\t\t\t),\n\t\t\tX:,\n\t\t\t(\n\t\t\t\t_type=keygen,\n\t\t\t\ta=(\n\t\t\t\t\tname=X\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=label,\n\t\t\t\ta=(\n\t\t\t\t\tfor=Name\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tlabel\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A4666F726DA16181A6616374696F6EA3626172A1639182A55F74797065A86669656C64736574A1639A82A55F74797065A66C6567656E64A16391A4666F6F3AA54E616D653A82A55F74797065A5696E707574A16181A474797065A47465787481A55F74797065A26272A6456D61696C3A82A55F74797065A5696E707574A16181A474797065A47465787481A55F74797065A26272A2583A82A55F74797065A66B657967656EA16181A46E616D65A15883A55F74797065A56C6162656CA16181A3666F72A44E616D65A16391A56C6162656C",
					/* MsgPackT */	"83A174A4666F726DA16181A6616374696F6EA3626172A1639182A174A86669656C64736574A1639A82A174A66C6567656E64A16391A4666F6F3AA54E616D653A82A174A5696E707574A16181A474797065A47465787481A174A26272A6456D61696C3A82A174A5696E707574A16181A474797065A47465787481A174A26272A2583A82A174A66B657967656EA16181A46E616D65A15883A174A56C6162656CA16181A3666F72A44E616D65A16391A56C6162656C",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>form</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:action>bar</jp:action>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>fieldset</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>legend</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo:</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>Name:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>br</jp:_type>\n</rdf:li>\n<rdf:li>Email:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>br</jp:_type>\n</rdf:li>\n<rdf:li>X:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>keygen</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>X</jp:name>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>label</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:for>Name</jp:for>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>label</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>form</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:action>bar</jp:action>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>fieldset</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>legend</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo:</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>Name:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>br</jp:t>\n</rdf:li>\n<rdf:li>Email:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>br</jp:t>\n</rdf:li>\n<rdf:li>X:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>keygen</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>X</jp:name>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>label</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:for>Name</jp:for>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>label</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>form</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:action>bar</jp:action>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>fieldset</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>legend</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>foo:</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>Name:</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>input</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:type>text</jp:type>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>br</jp:_type>\n              </rdf:li>\n              <rdf:li>Email:</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>input</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:type>text</jp:type>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>br</jp:_type>\n              </rdf:li>\n              <rdf:li>X:</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>keygen</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:name>X</jp:name>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>label</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:for>Name</jp:for>\n                </jp:a>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>label</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Form o) {
						assertInstanceOf(Form.class, o);
						assertInstanceOf(Fieldset.class, o.getChild(0));
						assertInstanceOf(Legend.class, o.getChild(0,0));
						assertInstanceOf(Input.class, o.getChild(0,2));
						assertInstanceOf(Br.class, o.getChild(0,3));
						assertInstanceOf(Input.class, o.getChild(0,5));
						assertInstanceOf(Br.class, o.getChild(0,6));
						assertInstanceOf(Keygen.class, o.getChild(0,8));
						assertInstanceOf(Label.class, o.getChild(0,9));
					}
				}
			},
			{	/* 39 */
				new ComboInput<Figure>(
					"Figure/Figcaption/Img",
					Figure.class,
					figure(
						img("foo.png").alt("foo").width(100).height(200),
						figcaption("The caption")
					),
					/* Json */		"{_type:'figure',c:[{_type:'img',a:{src:'foo.png',alt:'foo',width:100,height:200}},{_type:'figcaption',c:['The caption']}]}",
					/* JsonT */		"{t:'figure',c:[{t:'img',a:{src:'foo.png',alt:'foo',width:100,height:200}},{t:'figcaption',c:['The caption']}]}",
					/* JsonR */		"{\n\t_type: 'figure',\n\tc: [\n\t\t{\n\t\t\t_type: 'img',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.png',\n\t\t\t\talt: 'foo',\n\t\t\t\twidth: 100,\n\t\t\t\theight: 200\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'figcaption',\n\t\t\tc: [\n\t\t\t\t'The caption'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
					/* XmlT */		"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
					/* XmlR */		"<figure>\n\t<img src='foo.png' alt='foo' width='100' height='200'/>\n\t<figcaption>The caption</figcaption>\n</figure>\n",
					/* XmlNs */		"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
					/* Html */		"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
					/* HtmlT */		"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
					/* HtmlR */		"<figure>\n\t<img src='foo.png' alt='foo' width='100' height='200'/>\n\t<figcaption>The caption</figcaption>\n</figure>\n",
					/* Uon */		"(_type=figure,c=@((_type=img,a=(src=foo.png,alt=foo,width=100,height=200)),(_type=figcaption,c=@('The caption'))))",
					/* UonT */		"(t=figure,c=@((t=img,a=(src=foo.png,alt=foo,width=100,height=200)),(t=figcaption,c=@('The caption'))))",
					/* UonR */		"(\n\t_type=figure,\n\tc=@(\n\t\t(\n\t\t\t_type=img,\n\t\t\ta=(\n\t\t\t\tsrc=foo.png,\n\t\t\t\talt=foo,\n\t\t\t\twidth=100,\n\t\t\t\theight=200\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=figcaption,\n\t\t\tc=@(\n\t\t\t\t'The caption'\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=figure&c=@((_type=img,a=(src=foo.png,alt=foo,width=100,height=200)),(_type=figcaption,c=@('The+caption')))",
					/* UrlEncT */	"t=figure&c=@((t=img,a=(src=foo.png,alt=foo,width=100,height=200)),(t=figcaption,c=@('The+caption')))",
					/* UrlEncR */	"_type=figure\n&c=@(\n\t(\n\t\t_type=img,\n\t\ta=(\n\t\t\tsrc=foo.png,\n\t\t\talt=foo,\n\t\t\twidth=100,\n\t\t\theight=200\n\t\t)\n\t),\n\t(\n\t\t_type=figcaption,\n\t\tc=@(\n\t\t\t'The+caption'\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A6666967757265A1639282A55F74797065A3696D67A16184A3737263A7666F6F2E706E67A3616C74A3666F6FA5776964746864A6686569676874D100C882A55F74797065AA66696763617074696F6EA16391AB5468652063617074696F6E",
					/* MsgPackT */	"82A174A6666967757265A1639282A174A3696D67A16184A3737263A7666F6F2E706E67A3616C74A3666F6FA5776964746864A6686569676874D100C882A174AA66696763617074696F6EA16391AB5468652063617074696F6E",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>figure</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>img</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.png</jp:src>\n<jp:alt>foo</jp:alt>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>figcaption</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>The caption</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>figure</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>img</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.png</jp:src>\n<jp:alt>foo</jp:alt>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>figcaption</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>The caption</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>figure</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>img</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.png</jp:src>\n            <jp:alt>foo</jp:alt>\n            <jp:width>100</jp:width>\n            <jp:height>200</jp:height>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>figcaption</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>The caption</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Figure o) {
						assertInstanceOf(Figure.class, o);
						assertInstanceOf(Img.class, o.getChild(0));
						assertInstanceOf(Figcaption.class, o.getChild(1));
					}
				}
			},
			{	/* 40 */
				new ComboInput<Div>(
					"H1/H2/H3/H4/H5/H6",
					Div.class,
					div(
						h1("One"),h2("Two"),h3("Three"),h4("Four"),h5("Five"),h6("Six")
					),
					/* Json */		"{_type:'div',c:[{_type:'h1',c:['One']},{_type:'h2',c:['Two']},{_type:'h3',c:['Three']},{_type:'h4',c:['Four']},{_type:'h5',c:['Five']},{_type:'h6',c:['Six']}]}",
					/* JsonT */		"{t:'div',c:[{t:'h1',c:['One']},{t:'h2',c:['Two']},{t:'h3',c:['Three']},{t:'h4',c:['Four']},{t:'h5',c:['Five']},{t:'h6',c:['Six']}]}",
					/* JsonR */		"{\n\t_type: 'div',\n\tc: [\n\t\t{\n\t\t\t_type: 'h1',\n\t\t\tc: [\n\t\t\t\t'One'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h2',\n\t\t\tc: [\n\t\t\t\t'Two'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h3',\n\t\t\tc: [\n\t\t\t\t'Three'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h4',\n\t\t\tc: [\n\t\t\t\t'Four'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h5',\n\t\t\tc: [\n\t\t\t\t'Five'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h6',\n\t\t\tc: [\n\t\t\t\t'Six'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
					/* XmlT */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
					/* XmlR */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>\n",
					/* XmlNs */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
					/* Html */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
					/* HtmlT */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
					/* HtmlR */		"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>\n",
					/* Uon */		"(_type=div,c=@((_type=h1,c=@(One)),(_type=h2,c=@(Two)),(_type=h3,c=@(Three)),(_type=h4,c=@(Four)),(_type=h5,c=@(Five)),(_type=h6,c=@(Six))))",
					/* UonT */		"(t=div,c=@((t=h1,c=@(One)),(t=h2,c=@(Two)),(t=h3,c=@(Three)),(t=h4,c=@(Four)),(t=h5,c=@(Five)),(t=h6,c=@(Six))))",
					/* UonR */		"(\n\t_type=div,\n\tc=@(\n\t\t(\n\t\t\t_type=h1,\n\t\t\tc=@(\n\t\t\t\tOne\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h2,\n\t\t\tc=@(\n\t\t\t\tTwo\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h3,\n\t\t\tc=@(\n\t\t\t\tThree\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h4,\n\t\t\tc=@(\n\t\t\t\tFour\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h5,\n\t\t\tc=@(\n\t\t\t\tFive\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h6,\n\t\t\tc=@(\n\t\t\t\tSix\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=div&c=@((_type=h1,c=@(One)),(_type=h2,c=@(Two)),(_type=h3,c=@(Three)),(_type=h4,c=@(Four)),(_type=h5,c=@(Five)),(_type=h6,c=@(Six)))",
					/* UrlEncT */	"t=div&c=@((t=h1,c=@(One)),(t=h2,c=@(Two)),(t=h3,c=@(Three)),(t=h4,c=@(Four)),(t=h5,c=@(Five)),(t=h6,c=@(Six)))",
					/* UrlEncR */	"_type=div\n&c=@(\n\t(\n\t\t_type=h1,\n\t\tc=@(\n\t\t\tOne\n\t\t)\n\t),\n\t(\n\t\t_type=h2,\n\t\tc=@(\n\t\t\tTwo\n\t\t)\n\t),\n\t(\n\t\t_type=h3,\n\t\tc=@(\n\t\t\tThree\n\t\t)\n\t),\n\t(\n\t\t_type=h4,\n\t\tc=@(\n\t\t\tFour\n\t\t)\n\t),\n\t(\n\t\t_type=h5,\n\t\tc=@(\n\t\t\tFive\n\t\t)\n\t),\n\t(\n\t\t_type=h6,\n\t\tc=@(\n\t\t\tSix\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A3646976A1639682A55F74797065A26831A16391A34F6E6582A55F74797065A26832A16391A354776F82A55F74797065A26833A16391A5546872656582A55F74797065A26834A16391A4466F757282A55F74797065A26835A16391A44669766582A55F74797065A26836A16391A3536978",
					/* MsgPackT */	"82A174A3646976A1639682A174A26831A16391A34F6E6582A174A26832A16391A354776F82A174A26833A16391A5546872656582A174A26834A16391A4466F757282A174A26835A16391A44669766582A174A26836A16391A3536978",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>div</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>One</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h2</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Two</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h3</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Three</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h4</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Four</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h5</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Five</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h6</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Six</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>div</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>One</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h2</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Two</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h3</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Three</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h4</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Four</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h5</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Five</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h6</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Six</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>div</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h1</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>One</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h2</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Two</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h3</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Three</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h4</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Four</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h5</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Five</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h6</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Six</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Div o) {
						assertInstanceOf(Div.class, o);
						assertInstanceOf(H1.class, o.getChild(0));
						assertInstanceOf(H2.class, o.getChild(1));
						assertInstanceOf(H3.class, o.getChild(2));
						assertInstanceOf(H4.class, o.getChild(3));
						assertInstanceOf(H5.class, o.getChild(4));
						assertInstanceOf(H6.class, o.getChild(5));
					}
				}
			},
			{	/* 41 */
				new ComboInput<P>(
					"Hr",
					P.class,
					p(hr()),
					/* Json */		"{_type:'p',c:[{_type:'hr'}]}",
					/* JsonT */		"{t:'p',c:[{t:'hr'}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'hr'\n\t\t}\n\t]\n}",
					/* Xml */		"<p><hr/></p>",
					/* XmlT */		"<p><hr/></p>",
					/* XmlR */		"<p><hr/></p>\n",
					/* XmlNs */		"<p><hr/></p>",
					/* Html */		"<p><hr/></p>",
					/* HtmlT */		"<p><hr/></p>",
					/* HtmlR */		"<p><hr/></p>\n",
					/* Uon */		"(_type=p,c=@((_type=hr)))",
					/* UonT */		"(t=p,c=@((t=hr)))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=hr\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=hr))",
					/* UrlEncT */	"t=p&c=@((t=hr))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=hr\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639181A55F74797065A26872",
					/* MsgPackT */	"82A174A170A1639181A174A26872",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>hr</jp:_type>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>hr</jp:t>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>hr</jp:_type>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Hr.class, o.getChild(0));
					}
				}
			},
			{	/* 42 */
				new ComboInput<Html>(
					"Html/Head/Body/Title/Base/Link/Meta",
					Html.class,
					html(
						head(
							title("title"),
							base("foo").target("_blank"),
							link().rel("stylesheet").type("text/css").href("theme.css"),
							meta().charset("UTF-8")
						),
						body(
							"bar"
						)
					),
					/* Json */		"{_type:'html',c:[{_type:'head',c:[{_type:'title',c:'title'},{_type:'base',a:{href:'foo',target:'_blank'}},{_type:'link',a:{rel:'stylesheet',type:'text/css',href:'theme.css'}},{_type:'meta',a:{charset:'UTF-8'}}]},{_type:'body',c:['bar']}]}",
					/* JsonT */		"{t:'html',c:[{t:'head',c:[{t:'title',c:'title'},{t:'base',a:{href:'foo',target:'_blank'}},{t:'link',a:{rel:'stylesheet',type:'text/css',href:'theme.css'}},{t:'meta',a:{charset:'UTF-8'}}]},{t:'body',c:['bar']}]}",
					/* JsonR */		"{\n\t_type: 'html',\n\tc: [\n\t\t{\n\t\t\t_type: 'head',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'title',\n\t\t\t\t\tc: 'title'\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'base',\n\t\t\t\t\ta: {\n\t\t\t\t\t\thref: 'foo',\n\t\t\t\t\t\ttarget: '_blank'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'link',\n\t\t\t\t\ta: {\n\t\t\t\t\t\trel: 'stylesheet',\n\t\t\t\t\t\ttype: 'text/css',\n\t\t\t\t\t\thref: 'theme.css'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'meta',\n\t\t\t\t\ta: {\n\t\t\t\t\t\tcharset: 'UTF-8'\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'body',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
					/* XmlT */		"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
					/* XmlR */
						"<html>\n"
						+"	<head>\n"
						+"		<title>title</title>\n"
						+"		<base href='foo' target='_blank'/>\n"
						+"		<link rel='stylesheet' type='text/css' href='theme.css'/>\n"
						+"		<meta charset='UTF-8'/>\n"
						+"	</head>\n"
						+"	<body>bar</body>\n"
						+"</html>\n",
					/* XmlNs */		"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
					/* Html */		"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
					/* HtmlT */		"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
					/* HtmlR */
						"<html>\n"
						+"	<head>\n"
						+"		<title>title</title>\n"
						+"		<base href='foo' target='_blank'/>\n"
						+"		<link rel='stylesheet' type='text/css' href='theme.css'/>\n"
						+"		<meta charset='UTF-8'/>\n"
						+"	</head>\n"
						+"	<body>bar</body>\n"
						+"</html>\n",
					/* Uon */		"(_type=html,c=@((_type=head,c=@((_type=title,c=title),(_type=base,a=(href=foo,target=_blank)),(_type=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(_type=meta,a=(charset=UTF-8)))),(_type=body,c=@(bar))))",
					/* UonT */		"(t=html,c=@((t=head,c=@((t=title,c=title),(t=base,a=(href=foo,target=_blank)),(t=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(t=meta,a=(charset=UTF-8)))),(t=body,c=@(bar))))",
					/* UonR */		"(\n\t_type=html,\n\tc=@(\n\t\t(\n\t\t\t_type=head,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=title,\n\t\t\t\t\tc=title\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=base,\n\t\t\t\t\ta=(\n\t\t\t\t\t\thref=foo,\n\t\t\t\t\t\ttarget=_blank\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=link,\n\t\t\t\t\ta=(\n\t\t\t\t\t\trel=stylesheet,\n\t\t\t\t\t\ttype=text/css,\n\t\t\t\t\t\thref=theme.css\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=meta,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tcharset=UTF-8\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=body,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=html&c=@((_type=head,c=@((_type=title,c=title),(_type=base,a=(href=foo,target=_blank)),(_type=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(_type=meta,a=(charset=UTF-8)))),(_type=body,c=@(bar)))",
					/* UrlEncT */	"t=html&c=@((t=head,c=@((t=title,c=title),(t=base,a=(href=foo,target=_blank)),(t=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(t=meta,a=(charset=UTF-8)))),(t=body,c=@(bar)))",
					/* UrlEncR */	"_type=html\n&c=@(\n\t(\n\t\t_type=head,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=title,\n\t\t\t\tc=title\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=base,\n\t\t\t\ta=(\n\t\t\t\t\thref=foo,\n\t\t\t\t\ttarget=_blank\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=link,\n\t\t\t\ta=(\n\t\t\t\t\trel=stylesheet,\n\t\t\t\t\ttype=text/css,\n\t\t\t\t\thref=theme.css\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=meta,\n\t\t\t\ta=(\n\t\t\t\t\tcharset=UTF-8\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=body,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A468746D6CA1639282A55F74797065A468656164A1639482A55F74797065A57469746C65A163A57469746C6582A55F74797065A462617365A16182A468726566A3666F6FA6746172676574A65F626C616E6B82A55F74797065A46C696E6BA16183A372656CAA7374796C657368656574A474797065A8746578742F637373A468726566A97468656D652E63737382A55F74797065A46D657461A16181A763686172736574A55554462D3882A55F74797065A4626F6479A16391A3626172",
					/* MsgPackT */	"82A174A468746D6CA1639282A174A468656164A1639482A174A57469746C65A163A57469746C6582A174A462617365A16182A468726566A3666F6FA6746172676574A65F626C616E6B82A174A46C696E6BA16183A372656CAA7374796C657368656574A474797065A8746578742F637373A468726566A97468656D652E63737382A174A46D657461A16181A763686172736574A55554462D3882A174A4626F6479A16391A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>html</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>head</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>title</jp:_type>\n<jp:c>title</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>base</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n<jp:target>_blank</jp:target>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>link</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:rel>stylesheet</jp:rel>\n<jp:type>text/css</jp:type>\n<jp:href>theme.css</jp:href>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>meta</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:charset>UTF-8</jp:charset>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>body</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>html</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>head</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>title</jp:t>\n<jp:c>title</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>base</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n<jp:target>_blank</jp:target>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>link</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:rel>stylesheet</jp:rel>\n<jp:type>text/css</jp:type>\n<jp:href>theme.css</jp:href>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>meta</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:charset>UTF-8</jp:charset>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>body</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>html</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>head</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>title</jp:_type>\n                <jp:c>title</jp:c>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>base</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:href>foo</jp:href>\n                  <jp:target>_blank</jp:target>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>link</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:rel>stylesheet</jp:rel>\n                  <jp:type>text/css</jp:type>\n                  <jp:href>theme.css</jp:href>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>meta</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:charset>UTF-8</jp:charset>\n                </jp:a>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>body</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Html o) {
						assertInstanceOf(Html.class, o);
						assertInstanceOf(Head.class, o.getChild(0));
						assertInstanceOf(Title.class, o.getChild(0,0));
						assertInstanceOf(Base.class, o.getChild(0,1));
						assertInstanceOf(Link.class, o.getChild(0,2));
						assertInstanceOf(Meta.class, o.getChild(0,3));
						assertInstanceOf(Body.class, o.getChild(1));
					}
				}
			},
			{	/* 43 */
				new ComboInput<P>(
					"I",
					P.class,
					p(i("foo")),
					/* Json */		"{_type:'p',c:[{_type:'i',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'i',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'i',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><i>foo</i></p>",
					/* XmlT */		"<p><i>foo</i></p>",
					/* XmlR */		"<p><i>foo</i></p>\n",
					/* XmlNs */		"<p><i>foo</i></p>",
					/* Html */		"<p><i>foo</i></p>",
					/* HtmlT */		"<p><i>foo</i></p>",
					/* HtmlR */		"<p><i>foo</i></p>\n",
					/* Uon */		"(_type=p,c=@((_type=i,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=i,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=i,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=i,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=i,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=i,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A169A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A169A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>i</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>i</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>i</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(I.class, o.getChild(0));
					}
				}
			},
			{	/* 44 */
				new ComboInput<Iframe>(
					"Iframe",
					Iframe.class,
					iframe("foo"),
					/* Json */		"{_type:'iframe',c:['foo']}",
					/* JsonT */		"{t:'iframe',c:['foo']}",
					/* JsonR */		"{\n\t_type: 'iframe',\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<iframe>foo</iframe>",
					/* XmlT */		"<iframe>foo</iframe>",
					/* XmlR */		"<iframe>foo</iframe>\n",
					/* XmlNs */		"<iframe>foo</iframe>",
					/* Html */		"<iframe>foo</iframe>",
					/* HtmlT */		"<iframe>foo</iframe>",
					/* HtmlR */		"<iframe>foo</iframe>\n",
					/* Uon */		"(_type=iframe,c=@(foo))",
					/* UonT */		"(t=iframe,c=@(foo))",
					/* UonR */		"(\n\t_type=iframe,\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=iframe&c=@(foo)",
					/* UrlEncT */	"t=iframe&c=@(foo)",
					/* UrlEncR */	"_type=iframe\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"82A55F74797065A6696672616D65A16391A3666F6F",
					/* MsgPackT */	"82A174A6696672616D65A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>iframe</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>iframe</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>iframe</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Iframe o) {
						assertInstanceOf(Iframe.class, o);
					}
				}
			},
			{	/* 45 */
				new ComboInput<P>(
					"Kbd",
					P.class,
					p(kbd("foo")),
					/* Json */		"{_type:'p',c:[{_type:'kbd',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'kbd',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'kbd',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><kbd>foo</kbd></p>",
					/* XmlT */		"<p><kbd>foo</kbd></p>",
					/* XmlR */		"<p><kbd>foo</kbd></p>\n",
					/* XmlNs */		"<p><kbd>foo</kbd></p>",
					/* Html */		"<p><kbd>foo</kbd></p>",
					/* HtmlT */		"<p><kbd>foo</kbd></p>",
					/* HtmlR */		"<p><kbd>foo</kbd></p>\n",
					/* Uon */		"(_type=p,c=@((_type=kbd,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=kbd,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=kbd,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=kbd,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=kbd,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=kbd,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A36B6264A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A36B6264A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>kbd</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>kbd</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>kbd</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Kbd.class, o.getChild(0));
					}
				}
			},
			{	/* 46 */
				new ComboInput<Main>(
					"Main/Article/Header/Footer-1",
					Main.class,
					main(
						article(
							header(h1("header1"),p("header2")),
							p("content"),
							footer(h2("footer1"),p("footer2"))
						)
					),
					/* Json */		"{_type:'main',c:[{_type:'article',c:[{_type:'header',c:[{_type:'h1',c:['header1']},{_type:'p',c:['header2']}]},{_type:'p',c:['content']},{_type:'footer',c:[{_type:'h2',c:['footer1']},{_type:'p',c:['footer2']}]}]}]}",
					/* JsonT */		"{t:'main',c:[{t:'article',c:[{t:'header',c:[{t:'h1',c:['header1']},{t:'p',c:['header2']}]},{t:'p',c:['content']},{t:'footer',c:[{t:'h2',c:['footer1']},{t:'p',c:['footer2']}]}]}]}",
					/* JsonR */		"{\n\t_type: 'main',\n\tc: [\n\t\t{\n\t\t\t_type: 'article',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'header',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'h1',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'header1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'p',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'header2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'p',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'content'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'footer',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'h2',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'footer1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'p',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'footer2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
					/* XmlT */		"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
					/* XmlR */		"<main>\n\t<article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article>\n</main>\n",
					/* XmlNs */		"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
					/* Html */		"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
					/* HtmlT */		"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
					/* HtmlR */		"<main>\n\t<article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article>\n</main>\n",
					/* Uon */		"(_type=main,c=@((_type=article,c=@((_type=header,c=@((_type=h1,c=@(header1)),(_type=p,c=@(header2)))),(_type=p,c=@(content)),(_type=footer,c=@((_type=h2,c=@(footer1)),(_type=p,c=@(footer2))))))))",
					/* UonT */		"(t=main,c=@((t=article,c=@((t=header,c=@((t=h1,c=@(header1)),(t=p,c=@(header2)))),(t=p,c=@(content)),(t=footer,c=@((t=h2,c=@(footer1)),(t=p,c=@(footer2))))))))",
					/* UonR */		"(\n\t_type=main,\n\tc=@(\n\t\t(\n\t\t\t_type=article,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=header,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=h1,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\theader1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\theader2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=p,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tcontent\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=footer,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=h2,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tfooter1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tfooter2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=main&c=@((_type=article,c=@((_type=header,c=@((_type=h1,c=@(header1)),(_type=p,c=@(header2)))),(_type=p,c=@(content)),(_type=footer,c=@((_type=h2,c=@(footer1)),(_type=p,c=@(footer2)))))))",
					/* UrlEncT */	"t=main&c=@((t=article,c=@((t=header,c=@((t=h1,c=@(header1)),(t=p,c=@(header2)))),(t=p,c=@(content)),(t=footer,c=@((t=h2,c=@(footer1)),(t=p,c=@(footer2)))))))",
					/* UrlEncR */	"_type=main\n&c=@(\n\t(\n\t\t_type=article,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=header,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=h1,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\theader1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\theader2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=p,\n\t\t\t\tc=@(\n\t\t\t\t\tcontent\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=footer,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=h2,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tfooter1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tfooter2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A46D61696EA1639182A55F74797065A761727469636C65A1639382A55F74797065A6686561646572A1639282A55F74797065A26831A16391A76865616465723182A55F74797065A170A16391A76865616465723282A55F74797065A170A16391A7636F6E74656E7482A55F74797065A6666F6F746572A1639282A55F74797065A26832A16391A7666F6F7465723182A55F74797065A170A16391A7666F6F74657232",
					/* MsgPackT */	"82A174A46D61696EA1639182A174A761727469636C65A1639382A174A6686561646572A1639282A174A26831A16391A76865616465723182A174A170A16391A76865616465723282A174A170A16391A7636F6E74656E7482A174A6666F6F746572A1639282A174A26832A16391A7666F6F7465723182A174A170A16391A7666F6F74657232",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>main</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>article</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>header</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>content</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>footer</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h2</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>main</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>article</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>header</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>content</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>footer</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h2</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>main</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>article</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>header</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>h1</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>header1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>p</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>header2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>p</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>content</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>footer</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>h2</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>footer1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>p</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>footer2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Main o) {
						assertInstanceOf(Main.class, o);
						assertInstanceOf(Article.class, o.getChild(0));
						assertInstanceOf(Header.class, o.getChild(0,0));
						assertInstanceOf(H1.class, o.getChild(0,0,0));
						assertInstanceOf(P.class, o.getChild(0,0,1));
						assertInstanceOf(P.class, o.getChild(0,1));
						assertInstanceOf(Footer.class, o.getChild(0,2));
						assertInstanceOf(H2.class, o.getChild(0,2,0));
						assertInstanceOf(P.class, o.getChild(0,2,1));
					}
				}
			},
			{	/* 47 */
				new ComboInput<Map>(
					"Map/Area-1",
					Map.class,
					map(area("rect", "0,1,2,3", "foo").alt("bar")).name("baz"),
					/* Json */		"{_type:'map',a:{name:'baz'},c:[{_type:'area',a:{shape:'rect',coords:'0,1,2,3',href:'foo',alt:'bar'}}]}",
					/* JsonT */		"{t:'map',a:{name:'baz'},c:[{t:'area',a:{shape:'rect',coords:'0,1,2,3',href:'foo',alt:'bar'}}]}",
					/* JsonR */		"{\n\t_type: 'map',\n\ta: {\n\t\tname: 'baz'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'area',\n\t\t\ta: {\n\t\t\t\tshape: 'rect',\n\t\t\t\tcoords: '0,1,2,3',\n\t\t\t\thref: 'foo',\n\t\t\t\talt: 'bar'\n\t\t\t}\n\t\t}\n\t]\n}",
					/* Xml */		"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
					/* XmlT */		"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
					/* XmlR */		"<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n",
					/* XmlNs */		"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
					/* Html */		"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
					/* HtmlT */		"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
					/* HtmlR */		"<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n",
					/* Uon */		"(_type=map,a=(name=baz),c=@((_type=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar))))",
					/* UonT */		"(t=map,a=(name=baz),c=@((t=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar))))",
					/* UonR */		"(\n\t_type=map,\n\ta=(\n\t\tname=baz\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=area,\n\t\t\ta=(\n\t\t\t\tshape=rect,\n\t\t\t\tcoords='0,1,2,3',\n\t\t\t\thref=foo,\n\t\t\t\talt=bar\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=map&a=(name=baz)&c=@((_type=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar)))",
					/* UrlEncT */	"t=map&a=(name=baz)&c=@((t=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar)))",
					/* UrlEncR */	"_type=map\n&a=(\n\tname=baz\n)\n&c=@(\n\t(\n\t\t_type=area,\n\t\ta=(\n\t\t\tshape=rect,\n\t\t\tcoords='0,1,2,3',\n\t\t\thref=foo,\n\t\t\talt=bar\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A36D6170A16181A46E616D65A362617AA1639182A55F74797065A461726561A16184A57368617065A472656374A6636F6F726473A7302C312C322C33A468726566A3666F6FA3616C74A3626172",
					/* MsgPackT */	"83A174A36D6170A16181A46E616D65A362617AA1639182A174A461726561A16184A57368617065A472656374A6636F6F726473A7302C312C322C33A468726566A3666F6FA3616C74A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>map</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>baz</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>area</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:shape>rect</jp:shape>\n<jp:coords>0,1,2,3</jp:coords>\n<jp:href>foo</jp:href>\n<jp:alt>bar</jp:alt>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>map</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>baz</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>area</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:shape>rect</jp:shape>\n<jp:coords>0,1,2,3</jp:coords>\n<jp:href>foo</jp:href>\n<jp:alt>bar</jp:alt>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>map</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:name>baz</jp:name>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>area</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:shape>rect</jp:shape>\n            <jp:coords>0,1,2,3</jp:coords>\n            <jp:href>foo</jp:href>\n            <jp:alt>bar</jp:alt>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Map o) {
						assertInstanceOf(Map.class, o);
						assertInstanceOf(Area.class, o.getChild(0));
					}
				}
			},
			{	/* 48 */
				new ComboInput<P>(
					"Mark",
					P.class,
					p(mark("foo")),
					/* Json */		"{_type:'p',c:[{_type:'mark',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'mark',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'mark',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><mark>foo</mark></p>",
					/* XmlT */		"<p><mark>foo</mark></p>",
					/* XmlR */		"<p><mark>foo</mark></p>\n",
					/* XmlNs */		"<p><mark>foo</mark></p>",
					/* Html */		"<p><mark>foo</mark></p>",
					/* HtmlT */		"<p><mark>foo</mark></p>",
					/* HtmlR */		"<p><mark>foo</mark></p>\n",
					/* Uon */		"(_type=p,c=@((_type=mark,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=mark,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=mark,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=mark,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=mark,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=mark,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A46D61726BA16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A46D61726BA16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>mark</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>mark</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>mark</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Mark.class, o.getChild(0));
					}
				}
			},
			{	/* 49 */
				new ComboInput<Meter>(
					"Meter",
					Meter.class,
					meter("foo").value(1).min(0).max(2),
					/* Json */		"{_type:'meter',a:{value:1,min:0,max:2},c:['foo']}",
					/* JsonT */		"{t:'meter',a:{value:1,min:0,max:2},c:['foo']}",
					/* JsonR */		"{\n\t_type: 'meter',\n\ta: {\n\t\tvalue: 1,\n\t\tmin: 0,\n\t\tmax: 2\n\t},\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<meter value='1' min='0' max='2'>foo</meter>",
					/* XmlT */		"<meter value='1' min='0' max='2'>foo</meter>",
					/* XmlR */		"<meter value='1' min='0' max='2'>foo</meter>\n",
					/* XmlNs */		"<meter value='1' min='0' max='2'>foo</meter>",
					/* Html */		"<meter value='1' min='0' max='2'>foo</meter>",
					/* HtmlT */		"<meter value='1' min='0' max='2'>foo</meter>",
					/* HtmlR */		"<meter value='1' min='0' max='2'>foo</meter>\n",
					/* Uon */		"(_type=meter,a=(value=1,min=0,max=2),c=@(foo))",
					/* UonT */		"(t=meter,a=(value=1,min=0,max=2),c=@(foo))",
					/* UonR */		"(\n\t_type=meter,\n\ta=(\n\t\tvalue=1,\n\t\tmin=0,\n\t\tmax=2\n\t),\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=meter&a=(value=1,min=0,max=2)&c=@(foo)",
					/* UrlEncT */	"t=meter&a=(value=1,min=0,max=2)&c=@(foo)",
					/* UrlEncR */	"_type=meter\n&a=(\n\tvalue=1,\n\tmin=0,\n\tmax=2\n)\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"83A55F74797065A56D65746572A16183A576616C756501A36D696E00A36D617802A16391A3666F6F",
					/* MsgPackT */	"83A174A56D65746572A16183A576616C756501A36D696E00A36D617802A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>meter</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n<jp:min>0</jp:min>\n<jp:max>2</jp:max>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>meter</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n<jp:min>0</jp:min>\n<jp:max>2</jp:max>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>meter</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:value>1</jp:value>\n      <jp:min>0</jp:min>\n      <jp:max>2</jp:max>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Meter o) {
						assertInstanceOf(Meter.class, o);
					}
				}
			},
			{	/* 50 */
				new ComboInput<Nav>(
					"Nav",
					Nav.class,
					nav(a("foo","bar")),
					/* Json */		"{_type:'nav',c:[{_type:'a',a:{href:'foo'},c:['bar']}]}",
					/* JsonT */		"{t:'nav',c:[{t:'a',a:{href:'foo'},c:['bar']}]}",
					/* JsonR */		"{\n\t_type: 'nav',\n\tc: [\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'foo'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<nav><a href='foo'>bar</a></nav>",
					/* XmlT */		"<nav><a href='foo'>bar</a></nav>",
					/* XmlR */		"<nav><a href='foo'>bar</a></nav>\n",
					/* XmlNs */		"<nav><a href='foo'>bar</a></nav>",
					/* Html */		"<nav><a href='foo'>bar</a></nav>",
					/* HtmlT */		"<nav><a href='foo'>bar</a></nav>",
					/* HtmlR */		"<nav><a href='foo'>bar</a></nav>\n",
					/* Uon */		"(_type=nav,c=@((_type=a,a=(href=foo),c=@(bar))))",
					/* UonT */		"(t=nav,c=@((t=a,a=(href=foo),c=@(bar))))",
					/* UonR */		"(\n\t_type=nav,\n\tc=@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=foo\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=nav&c=@((_type=a,a=(href=foo),c=@(bar)))",
					/* UrlEncT */	"t=nav&c=@((t=a,a=(href=foo),c=@(bar)))",
					/* UrlEncR */	"_type=nav\n&c=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=foo\n\t\t),\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A36E6176A1639183A55F74797065A161A16181A468726566A3666F6FA16391A3626172",
					/* MsgPackT */	"82A174A36E6176A1639183A174A161A16181A468726566A3666F6FA16391A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>nav</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>nav</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>nav</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href>foo</jp:href>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Nav o) {
						assertInstanceOf(Nav.class, o);
						assertInstanceOf(A.class, o.getChild(0));
					}
				}
			},
			{	/* 51 */
				new ComboInput<Noscript>(
					"Noscript",
					Noscript.class,
					noscript("No script!"),
					/* Json */		"{_type:'noscript',c:['No script!']}",
					/* JsonT */		"{t:'noscript',c:['No script!']}",
					/* JsonR */		"{\n\t_type: 'noscript',\n\tc: [\n\t\t'No script!'\n\t]\n}",
					/* Xml */		"<noscript>No script!</noscript>",
					/* XmlT */		"<noscript>No script!</noscript>",
					/* XmlR */		"<noscript>No script!</noscript>\n",
					/* XmlNs */		"<noscript>No script!</noscript>",
					/* Html */		"<noscript>No script!</noscript>",
					/* HtmlT */		"<noscript>No script!</noscript>",
					/* HtmlR */		"<noscript>No script!</noscript>\n",
					/* Uon */		"(_type=noscript,c=@('No script!'))",
					/* UonT */		"(t=noscript,c=@('No script!'))",
					/* UonR */		"(\n\t_type=noscript,\n\tc=@(\n\t\t'No script!'\n\t)\n)",
					/* UrlEnc */	"_type=noscript&c=@('No+script!')",
					/* UrlEncT */	"t=noscript&c=@('No+script!')",
					/* UrlEncR */	"_type=noscript\n&c=@(\n\t'No+script!'\n)",
					/* MsgPack */	"82A55F74797065A86E6F736372697074A16391AA4E6F2073637269707421",
					/* MsgPackT */	"82A174A86E6F736372697074A16391AA4E6F2073637269707421",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>noscript</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>No script!</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>noscript</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>No script!</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>noscript</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>No script!</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Noscript o) {
						assertInstanceOf(Noscript.class, o);
					}
				}
			},
			{	/* 52 */
				new ComboInput<Object_>(
					"Object/Param",
					Object_.class,
					object().width(1).height(2).data("foo.swf").child(param("autoplay",true)),
					/* Json */		"{_type:'object',a:{width:1,height:2,data:'foo.swf'},c:[{_type:'param',a:{name:'autoplay',value:true}}]}",
					/* JsonT */		"{t:'object',a:{width:1,height:2,data:'foo.swf'},c:[{t:'param',a:{name:'autoplay',value:true}}]}",
					/* JsonR */		"{\n\t_type: 'object',\n\ta: {\n\t\twidth: 1,\n\t\theight: 2,\n\t\tdata: 'foo.swf'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'param',\n\t\t\ta: {\n\t\t\t\tname: 'autoplay',\n\t\t\t\tvalue: true\n\t\t\t}\n\t\t}\n\t]\n}",
					/* Xml */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
					/* XmlT */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
					/* XmlR */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n",
					/* XmlNs */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
					/* Html */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
					/* HtmlT */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
					/* HtmlR */		"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n",
					/* Uon */		"(_type=object,a=(width=1,height=2,data=foo.swf),c=@((_type=param,a=(name=autoplay,value=true))))",
					/* UonT */		"(t=object,a=(width=1,height=2,data=foo.swf),c=@((t=param,a=(name=autoplay,value=true))))",
					/* UonR */		"(\n\t_type=object,\n\ta=(\n\t\twidth=1,\n\t\theight=2,\n\t\tdata=foo.swf\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=param,\n\t\t\ta=(\n\t\t\t\tname=autoplay,\n\t\t\t\tvalue=true\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=object&a=(width=1,height=2,data=foo.swf)&c=@((_type=param,a=(name=autoplay,value=true)))",
					/* UrlEncT */	"t=object&a=(width=1,height=2,data=foo.swf)&c=@((t=param,a=(name=autoplay,value=true)))",
					/* UrlEncR */	"_type=object\n&a=(\n\twidth=1,\n\theight=2,\n\tdata=foo.swf\n)\n&c=@(\n\t(\n\t\t_type=param,\n\t\ta=(\n\t\t\tname=autoplay,\n\t\t\tvalue=true\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A66F626A656374A16183A5776964746801A668656967687402A464617461A7666F6F2E737766A1639182A55F74797065A5706172616DA16182A46E616D65A86175746F706C6179A576616C7565C3",
					/* MsgPackT */	"83A174A66F626A656374A16183A5776964746801A668656967687402A464617461A7666F6F2E737766A1639182A174A5706172616DA16182A46E616D65A86175746F706C6179A576616C7565C3",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>object</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:width>1</jp:width>\n<jp:height>2</jp:height>\n<jp:data>foo.swf</jp:data>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>param</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>autoplay</jp:name>\n<jp:value>true</jp:value>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>object</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:width>1</jp:width>\n<jp:height>2</jp:height>\n<jp:data>foo.swf</jp:data>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>param</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>autoplay</jp:name>\n<jp:value>true</jp:value>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>object</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:width>1</jp:width>\n      <jp:height>2</jp:height>\n      <jp:data>foo.swf</jp:data>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>param</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:name>autoplay</jp:name>\n            <jp:value>true</jp:value>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Object_ o) {
						assertInstanceOf(Object_.class, o);
						assertInstanceOf(Param.class, o.getChild(0));
					}
				}
			},
			{	/* 53 */
				new ComboInput<Ol>(
					"Ol/Li",
					Ol.class,
					ol(li("foo")),
					/* Json */		"{_type:'ol',c:[{_type:'li',c:['foo']}]}",
					/* JsonT */		"{t:'ol',c:[{t:'li',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'ol',\n\tc: [\n\t\t{\n\t\t\t_type: 'li',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<ol><li>foo</li></ol>",
					/* XmlT */		"<ol><li>foo</li></ol>",
					/* XmlR */		"<ol>\n\t<li>foo</li>\n</ol>\n",
					/* XmlNs */		"<ol><li>foo</li></ol>",
					/* Html */		"<ol><li>foo</li></ol>",
					/* HtmlT */		"<ol><li>foo</li></ol>",
					/* HtmlR */		"<ol>\n\t<li>foo</li>\n</ol>\n",
					/* Uon */		"(_type=ol,c=@((_type=li,c=@(foo))))",
					/* UonT */		"(t=ol,c=@((t=li,c=@(foo))))",
					/* UonR */		"(\n\t_type=ol,\n\tc=@(\n\t\t(\n\t\t\t_type=li,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=ol&c=@((_type=li,c=@(foo)))",
					/* UrlEncT */	"t=ol&c=@((t=li,c=@(foo)))",
					/* UrlEncR */	"_type=ol\n&c=@(\n\t(\n\t\t_type=li,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A26F6CA1639182A55F74797065A26C69A16391A3666F6F",
					/* MsgPackT */	"82A174A26F6CA1639182A174A26C69A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>ol</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>li</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>ol</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>li</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>ol</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>li</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Ol o) {
						assertInstanceOf(Ol.class, o);
						assertInstanceOf(Li.class, o.getChild(0));
					}
				}
			},
			{	/* 54 */
				new ComboInput<Form>(
					"Output",
					Form.class,
					(Form)form("testform",
						0,input("range").id("a").value(50),
						"+",input("number").id("b").value(50),
						"=",output().name("x")._for("a b")
					).oninput("x.value=parseInt(a.value)+parseInt(b.value)"),
					/* Json */		"{_type:'form',a:{action:'testform',oninput:'x.value=parseInt(a.value)+parseInt(b.value)'},c:[0,{_type:'input',a:{type:'range',id:'a',value:50}},'+',{_type:'input',a:{type:'number',id:'b',value:50}},'=',{_type:'output',a:{name:'x','for':'a b'}}]}",
					/* JsonT */		"{t:'form',a:{action:'testform',oninput:'x.value=parseInt(a.value)+parseInt(b.value)'},c:[0,{t:'input',a:{type:'range',id:'a',value:50}},'+',{t:'input',a:{type:'number',id:'b',value:50}},'=',{t:'output',a:{name:'x','for':'a b'}}]}",
					/* JsonR */		"{\n\t_type: 'form',\n\ta: {\n\t\taction: 'testform',\n\t\toninput: 'x.value=parseInt(a.value)+parseInt(b.value)'\n\t},\n\tc: [\n\t\t0,\n\t\t{\n\t\t\t_type: 'input',\n\t\t\ta: {\n\t\t\t\ttype: 'range',\n\t\t\t\tid: 'a',\n\t\t\t\tvalue: 50\n\t\t\t}\n\t\t},\n\t\t'+',\n\t\t{\n\t\t\t_type: 'input',\n\t\t\ta: {\n\t\t\t\ttype: 'number',\n\t\t\t\tid: 'b',\n\t\t\t\tvalue: 50\n\t\t\t}\n\t\t},\n\t\t'=',\n\t\t{\n\t\t\t_type: 'output',\n\t\t\ta: {\n\t\t\t\tname: 'x',\n\t\t\t\t'for': 'a b'\n\t\t\t}\n\t\t}\n\t]\n}",
					/* Xml */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>",
					/* XmlT */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>",
					/* XmlR */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>\n",
					/* XmlNs */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>",
					/* Html */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>",
					/* HtmlT */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>",
					/* HtmlR */		"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>\n",
					/* Uon */		"(_type=form,a=(action=testform,oninput='x.value=parseInt(a.value)+parseInt(b.value)'),c=@(0,(_type=input,a=(type=range,id=a,value=50)),+,(_type=input,a=(type=number,id=b,value=50)),'=',(_type=output,a=(name=x,for='a b'))))",
					/* UonT */		"(t=form,a=(action=testform,oninput='x.value=parseInt(a.value)+parseInt(b.value)'),c=@(0,(t=input,a=(type=range,id=a,value=50)),+,(t=input,a=(type=number,id=b,value=50)),'=',(t=output,a=(name=x,for='a b'))))",
					/* UonR */		"(\n\t_type=form,\n\ta=(\n\t\taction=testform,\n\t\toninput='x.value=parseInt(a.value)+parseInt(b.value)'\n\t),\n\tc=@(\n\t\t0,\n\t\t(\n\t\t\t_type=input,\n\t\t\ta=(\n\t\t\t\ttype=range,\n\t\t\t\tid=a,\n\t\t\t\tvalue=50\n\t\t\t)\n\t\t),\n\t\t+,\n\t\t(\n\t\t\t_type=input,\n\t\t\ta=(\n\t\t\t\ttype=number,\n\t\t\t\tid=b,\n\t\t\t\tvalue=50\n\t\t\t)\n\t\t),\n\t\t'=',\n\t\t(\n\t\t\t_type=output,\n\t\t\ta=(\n\t\t\t\tname=x,\n\t\t\t\tfor='a b'\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=form&a=(action=testform,oninput='x.value=parseInt(a.value)%2BparseInt(b.value)')&c=@(0,(_type=input,a=(type=range,id=a,value=50)),%2B,(_type=input,a=(type=number,id=b,value=50)),'=',(_type=output,a=(name=x,for='a+b')))",
					/* UrlEncT */	"t=form&a=(action=testform,oninput='x.value=parseInt(a.value)%2BparseInt(b.value)')&c=@(0,(t=input,a=(type=range,id=a,value=50)),%2B,(t=input,a=(type=number,id=b,value=50)),'=',(t=output,a=(name=x,for='a+b')))",
					/* UrlEncR */	"_type=form\n&a=(\n\taction=testform,\n\toninput='x.value=parseInt(a.value)%2BparseInt(b.value)'\n)\n&c=@(\n\t0,\n\t(\n\t\t_type=input,\n\t\ta=(\n\t\t\ttype=range,\n\t\t\tid=a,\n\t\t\tvalue=50\n\t\t)\n\t),\n\t%2B,\n\t(\n\t\t_type=input,\n\t\ta=(\n\t\t\ttype=number,\n\t\t\tid=b,\n\t\t\tvalue=50\n\t\t)\n\t),\n\t'=',\n\t(\n\t\t_type=output,\n\t\ta=(\n\t\t\tname=x,\n\t\t\tfor='a+b'\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A4666F726DA16182A6616374696F6EA874657374666F726DA76F6E696E707574D92B782E76616C75653D7061727365496E7428612E76616C7565292B7061727365496E7428622E76616C756529A163960082A55F74797065A5696E707574A16183A474797065A572616E6765A26964A161A576616C756532A12B82A55F74797065A5696E707574A16183A474797065A66E756D626572A26964A162A576616C756532A13D82A55F74797065A66F7574707574A16182A46E616D65A178A3666F72A3612062",
					/* MsgPackT */	"83A174A4666F726DA16182A6616374696F6EA874657374666F726DA76F6E696E707574D92B782E76616C75653D7061727365496E7428612E76616C7565292B7061727365496E7428622E76616C756529A163960082A174A5696E707574A16183A474797065A572616E6765A26964A161A576616C756532A12B82A174A5696E707574A16183A474797065A66E756D626572A26964A162A576616C756532A13D82A174A66F7574707574A16182A46E616D65A178A3666F72A3612062",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>form</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:action>testform</jp:action>\n<jp:oninput>x.value=parseInt(a.value)+parseInt(b.value)</jp:oninput>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>0</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>range</jp:type>\n<jp:id>a</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>+</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>number</jp:type>\n<jp:id>b</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>=</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>output</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>x</jp:name>\n<jp:for>a b</jp:for>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>form</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:action>testform</jp:action>\n<jp:oninput>x.value=parseInt(a.value)+parseInt(b.value)</jp:oninput>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>0</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>range</jp:type>\n<jp:id>a</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>+</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>number</jp:type>\n<jp:id>b</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>=</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>output</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>x</jp:name>\n<jp:for>a b</jp:for>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>form</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:action>testform</jp:action>\n      <jp:oninput>x.value=parseInt(a.value)+parseInt(b.value)</jp:oninput>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>0</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>input</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:type>range</jp:type>\n            <jp:id>a</jp:id>\n            <jp:value>50</jp:value>\n          </jp:a>\n        </rdf:li>\n        <rdf:li>+</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>input</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:type>number</jp:type>\n            <jp:id>b</jp:id>\n            <jp:value>50</jp:value>\n          </jp:a>\n        </rdf:li>\n        <rdf:li>=</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>output</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:name>x</jp:name>\n            <jp:for>a b</jp:for>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Form o) {
						assertInstanceOf(Form.class, o);
						assertInstanceOf(Input.class, o.getChild(1));
						assertInstanceOf(Input.class, o.getChild(3));
						assertInstanceOf(Output.class, o.getChild(5));
					}
				}
			},
			{	/* 55 */
				new ComboInput<P>(
					"p",
					P.class,
					p("foo"),
					/* Json */		"{_type:'p',c:['foo']}",
					/* JsonT */		"{t:'p',c:['foo']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<p>foo</p>",
					/* XmlT */		"<p>foo</p>",
					/* XmlR */		"<p>foo</p>\n",
					/* XmlNs */		"<p>foo</p>",
					/* Html */		"<p>foo</p>",
					/* HtmlT */		"<p>foo</p>",
					/* HtmlR */		"<p>foo</p>\n",
					/* Uon */		"(_type=p,c=@(foo))",
					/* UonT */		"(t=p,c=@(foo))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo)",
					/* UrlEncT */	"t=p&c=@(foo)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"82A55F74797065A170A16391A3666F6F",
					/* MsgPackT */	"82A174A170A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
					}
				}
			},
			{	/* 56 */
				new ComboInput<P[][]>(
					"P[][]",
					P[][].class,
					new P[][]{{p("a"),p("b")},{},{p("c")}},
					/* Json */		"[[{_type:'p',c:['a']},{_type:'p',c:['b']}],[],[{_type:'p',c:['c']}]]",
					/* JsonT */		"[[{t:'p',c:['a']},{t:'p',c:['b']}],[],[{t:'p',c:['c']}]]",
					/* JsonR */		"[\n\t[\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'a'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'b'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t}\n\t]\n]",
					/* Xml */		"<array><array><p>a</p><p>b</p></array><array></array><array><p>c</p></array></array>",
					/* XmlT */		"<array><array><p>a</p><p>b</p></array><array></array><array><p>c</p></array></array>",
					/* XmlR */		"<array>\n\t<array>\n\t\t<p>a</p>\n\t\t<p>b</p>\n\t</array>\n\t<array>\n\t</array>\n\t<array>\n\t\t<p>c</p>\n\t</array>\n</array>\n",
					/* XmlNs */		"<array><array><p>a</p><p>b</p></array><array></array><array><p>c</p></array></array>",
					/* Html */		"<ul><li><ul><li><p>a</p></li><li><p>b</p></li></ul></li><li><ul></ul></li><li><ul><li><p>c</p></li></ul></li></ul>",
					/* HtmlT */		"<ul><li><ul><li><p>a</p></li><li><p>b</p></li></ul></li><li><ul></ul></li><li><ul><li><p>c</p></li></ul></li></ul>",
					/* HtmlR */		"<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<p>a</p>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<p>b</p>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul></ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<p>c</p>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n",
					/* Uon */		"@(@((_type=p,c=@(a)),(_type=p,c=@(b))),@(),@((_type=p,c=@(c))))",
					/* UonT */		"@(@((t=p,c=@(a)),(t=p,c=@(b))),@(),@((t=p,c=@(c))))",
					/* UonR */		"@(\n\t@(\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\ta\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tb\n\t\t\t)\n\t\t)\n\t),\n\t@(),\n\t@(\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"0=@((_type=p,c=@(a)),(_type=p,c=@(b)))&1=@()&2=@((_type=p,c=@(c)))",
					/* UrlEncT */	"0=@((t=p,c=@(a)),(t=p,c=@(b)))&1=@()&2=@((t=p,c=@(c)))",
					/* UrlEncR */	"0=@(\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\ta\n\t\t)\n\t),\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tb\n\t\t)\n\t)\n)\n&1=@()\n&2=@(\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t)\n)",
					/* MsgPack */	"939282A55F74797065A170A16391A16182A55F74797065A170A16391A162909182A55F74797065A170A16391A163",
					/* MsgPackT */	"939282A174A170A16391A16182A174A170A16391A162909182A174A170A16391A163",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>a</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq/>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P[][] o) {
						assertInstanceOf(P[][].class, o);
					}
				}
			},
			{	/* 57 */
				new ComboInput<Pre>(
					"Pre",
					Pre.class,
					pre("foo   \n   bar"),
					/* Json */		"{_type:'pre',c:['foo   \\n   bar']}",
					/* JsonT */		"{t:'pre',c:['foo   \\n   bar']}",
					/* JsonR */		"{\n\t_type: 'pre',\n\tc: [\n\t\t'foo   \\n   bar'\n\t]\n}",
					/* Xml */		"<pre>foo   &#x000a;   bar</pre>",
					/* XmlT */		"<pre>foo   &#x000a;   bar</pre>",
					/* XmlR */		"<pre>foo   &#x000a;   bar</pre>\n",
					/* XmlNs */		"<pre>foo   &#x000a;   bar</pre>",
					/* Html */		"<pre>foo   \n   bar</pre>",
					/* HtmlT */		"<pre>foo   \n   bar</pre>",
					/* HtmlR */		"<pre>foo   \n   bar</pre>\n",
					/* Uon */		"(_type=pre,c=@('foo   \n   bar'))",
					/* UonT */		"(t=pre,c=@('foo   \n   bar'))",
					/* UonR */		"(\n\t_type=pre,\n\tc=@(\n\t\t'foo   \n   bar'\n\t)\n)",
					/* UrlEnc */	"_type=pre&c=@('foo+++%0A+++bar')",
					/* UrlEncT */	"t=pre&c=@('foo+++%0A+++bar')",
					/* UrlEncR */	"_type=pre\n&c=@(\n\t'foo+++%0A+++bar'\n)",
					/* MsgPack */	"82A55F74797065A3707265A16391AD666F6F2020200A202020626172",
					/* MsgPackT */	"82A174A3707265A16391AD666F6F2020200A202020626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>pre</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo   _x000A_   bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>pre</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo   _x000A_   bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>pre</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo   _x000A_   bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Pre o) {
						assertInstanceOf(Pre.class, o);
					}
				}
			},
			{	/* 58 */
				new ComboInput<Progress>(
					"Progress",
					Progress.class,
					progress().value(1),
					/* Json */		"{_type:'progress',a:{value:1}}",
					/* JsonT */		"{t:'progress',a:{value:1}}",
					/* JsonR */		"{\n\t_type: 'progress',\n\ta: {\n\t\tvalue: 1\n\t}\n}",
					/* Xml */		"<progress value='1' nil='true'></progress>",
					/* XmlT */		"<progress value='1' nil='true'></progress>",
					/* XmlR */		"<progress value='1' nil='true'>\n</progress>\n",
					/* XmlNs */		"<progress value='1' nil='true'></progress>",
					/* Html */		"<progress value='1' nil='true'></progress>",
					/* HtmlT */		"<progress value='1' nil='true'></progress>",
					/* HtmlR */		"<progress value='1' nil='true'>\n</progress>\n",
					/* Uon */		"(_type=progress,a=(value=1))",
					/* UonT */		"(t=progress,a=(value=1))",
					/* UonR */		"(\n\t_type=progress,\n\ta=(\n\t\tvalue=1\n\t)\n)",
					/* UrlEnc */	"_type=progress&a=(value=1)",
					/* UrlEncT */	"t=progress&a=(value=1)",
					/* UrlEncR */	"_type=progress\n&a=(\n\tvalue=1\n)",
					/* MsgPack */	"82A55F74797065A870726F6772657373A16181A576616C756501",
					/* MsgPackT */	"82A174A870726F6772657373A16181A576616C756501",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>progress</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>progress</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>progress</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:value>1</jp:value>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Progress o) {
						assertInstanceOf(Progress.class, o);
					}
				}
			},
			{	/* 59 */
				new ComboInput<P>(
					"Q",
					P.class,
					p("foo",q("bar"),"baz"),
					/* Json */		"{_type:'p',c:['foo',{_type:'q',c:['bar']},'baz']}",
					/* JsonT */		"{t:'p',c:['foo',{t:'q',c:['bar']},'baz']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'q',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}",
					/* Xml */		"<p>foo<q>bar</q>baz</p>",
					/* XmlT */		"<p>foo<q>bar</q>baz</p>",
					/* XmlR */		"<p>foo<q>bar</q>baz</p>\n",
					/* XmlNs */		"<p>foo<q>bar</q>baz</p>",
					/* Html */		"<p>foo<q>bar</q>baz</p>",
					/* HtmlT */		"<p>foo<q>bar</q>baz</p>",
					/* HtmlR */		"<p>foo<q>bar</q>baz</p>\n",
					/* Uon */		"(_type=p,c=@(foo,(_type=q,c=@(bar)),baz))",
					/* UonT */		"(t=p,c=@(foo,(t=q,c=@(bar)),baz))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=q,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo,(_type=q,c=@(bar)),baz)",
					/* UrlEncT */	"t=p&c=@(foo,(t=q,c=@(bar)),baz)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=q,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\tbaz\n)",
					/* MsgPack */	"82A55F74797065A170A16393A3666F6F82A55F74797065A171A16391A3626172A362617A",
					/* MsgPackT */	"82A174A170A16393A3666F6F82A174A171A16391A3626172A362617A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>q</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>q</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>q</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Q.class, o.getChild(1));
					}
				}
			},
			{	/* 60 */
				new ComboInput<Ruby>(
					"Ruby/Rb/Rtc/Rp/Rt",
					Ruby.class,
					ruby(
						"法",rb("華"),"経",rtc("き",rp("け"),"ょ")
					),
					/* Json */		"{_type:'ruby',c:['法',{_type:'rb',c:['華']},'経',{_type:'rtc',c:['き',{_type:'rp',c:['け']},'ょ']}]}",
					/* JsonT */		"{t:'ruby',c:['法',{t:'rb',c:['華']},'経',{t:'rtc',c:['き',{t:'rp',c:['け']},'ょ']}]}",
					/* JsonR */		"{\n\t_type: 'ruby',\n\tc: [\n\t\t'法',\n\t\t{\n\t\t\t_type: 'rb',\n\t\t\tc: [\n\t\t\t\t'華'\n\t\t\t]\n\t\t},\n\t\t'経',\n\t\t{\n\t\t\t_type: 'rtc',\n\t\t\tc: [\n\t\t\t\t'き',\n\t\t\t\t{\n\t\t\t\t\t_type: 'rp',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'け'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'ょ'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
					/* XmlT */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
					/* XmlR */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>\n",
					/* XmlNs */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
					/* Html */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
					/* HtmlT */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
					/* HtmlR */		"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>\n",
					/* Uon */		"(_type=ruby,c=@(法,(_type=rb,c=@(華)),経,(_type=rtc,c=@(き,(_type=rp,c=@(け)),ょ))))",
					/* UonT */		"(t=ruby,c=@(法,(t=rb,c=@(華)),経,(t=rtc,c=@(き,(t=rp,c=@(け)),ょ))))",
					/* UonR */		"(\n\t_type=ruby,\n\tc=@(\n\t\t法,\n\t\t(\n\t\t\t_type=rb,\n\t\t\tc=@(\n\t\t\t\t華\n\t\t\t)\n\t\t),\n\t\t経,\n\t\t(\n\t\t\t_type=rtc,\n\t\t\tc=@(\n\t\t\t\tき,\n\t\t\t\t(\n\t\t\t\t\t_type=rp,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tけ\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tょ\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=ruby&c=@(%E6%B3%95,(_type=rb,c=@(%E8%8F%AF)),%E7%B5%8C,(_type=rtc,c=@(%E3%81%8D,(_type=rp,c=@(%E3%81%91)),%E3%82%87)))",
					/* UrlEncT */	"t=ruby&c=@(%E6%B3%95,(t=rb,c=@(%E8%8F%AF)),%E7%B5%8C,(t=rtc,c=@(%E3%81%8D,(t=rp,c=@(%E3%81%91)),%E3%82%87)))",
					/* UrlEncR */	"_type=ruby\n&c=@(\n\t%E6%B3%95,\n\t(\n\t\t_type=rb,\n\t\tc=@(\n\t\t\t%E8%8F%AF\n\t\t)\n\t),\n\t%E7%B5%8C,\n\t(\n\t\t_type=rtc,\n\t\tc=@(\n\t\t\t%E3%81%8D,\n\t\t\t(\n\t\t\t\t_type=rp,\n\t\t\t\tc=@(\n\t\t\t\t\t%E3%81%91\n\t\t\t\t)\n\t\t\t),\n\t\t\t%E3%82%87\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A472756279A16394A3E6B39582A55F74797065A27262A16391A3E88FAFA3E7B58C82A55F74797065A3727463A16393A3E3818D82A55F74797065A27270A16391A3E38191A3E38287",
					/* MsgPackT */	"82A174A472756279A16394A3E6B39582A174A27262A16391A3E88FAFA3E7B58C82A174A3727463A16393A3E3818D82A174A27270A16391A3E38191A3E38287",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>ruby</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>法</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>rb</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>華</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>経</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>rtc</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>き</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>rp</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>け</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>ょ</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>ruby</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>法</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>rb</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>華</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>経</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>rtc</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>き</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>rp</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>け</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>ょ</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>ruby</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>法</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>rb</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>華</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>経</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>rtc</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>き</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>rp</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>け</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>ょ</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Ruby o) {
						assertInstanceOf(Ruby.class, o);
						assertInstanceOf(Rb.class, o.getChild(1));
						assertInstanceOf(Rtc.class, o.getChild(3));
						assertInstanceOf(Rp.class, o.getChild(3,1));
					}
				}
			},
			{	/* 61 */
				new ComboInput<P>(
					"S",
					P.class,
					p("foo",s("bar"),"baz"),
					/* Json */		"{_type:'p',c:['foo',{_type:'s',c:['bar']},'baz']}",
					/* JsonT */		"{t:'p',c:['foo',{t:'s',c:['bar']},'baz']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 's',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}",
					/* Xml */		"<p>foo<s>bar</s>baz</p>",
					/* XmlT */		"<p>foo<s>bar</s>baz</p>",
					/* XmlR */		"<p>foo<s>bar</s>baz</p>\n",
					/* XmlNs */		"<p>foo<s>bar</s>baz</p>",
					/* Html */		"<p>foo<s>bar</s>baz</p>",
					/* HtmlT */		"<p>foo<s>bar</s>baz</p>",
					/* HtmlR */		"<p>foo<s>bar</s>baz</p>\n",
					/* Uon */		"(_type=p,c=@(foo,(_type=s,c=@(bar)),baz))",
					/* UonT */		"(t=p,c=@(foo,(t=s,c=@(bar)),baz))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=s,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo,(_type=s,c=@(bar)),baz)",
					/* UrlEncT */	"t=p&c=@(foo,(t=s,c=@(bar)),baz)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=s,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\tbaz\n)",
					/* MsgPack */	"82A55F74797065A170A16393A3666F6F82A55F74797065A173A16391A3626172A362617A",
					/* MsgPackT */	"82A174A170A16393A3666F6F82A174A173A16391A3626172A362617A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>s</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>s</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>s</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(S.class, o.getChild(1));
					}
				}
			},
			{	/* 62 */
				new ComboInput<Samp>(
					"Samp",
					Samp.class,
					samp("foo"),
					/* Json */		"{_type:'samp',c:['foo']}",
					/* JsonT */		"{t:'samp',c:['foo']}",
					/* JsonR */		"{\n\t_type: 'samp',\n\tc: [\n\t\t'foo'\n\t]\n}",
					/* Xml */		"<samp>foo</samp>",
					/* XmlT */		"<samp>foo</samp>",
					/* XmlR */		"<samp>foo</samp>\n",
					/* XmlNs */		"<samp>foo</samp>",
					/* Html */		"<samp>foo</samp>",
					/* HtmlT */		"<samp>foo</samp>",
					/* HtmlR */		"<samp>foo</samp>\n",
					/* Uon */		"(_type=samp,c=@(foo))",
					/* UonT */		"(t=samp,c=@(foo))",
					/* UonR */		"(\n\t_type=samp,\n\tc=@(\n\t\tfoo\n\t)\n)",
					/* UrlEnc */	"_type=samp&c=@(foo)",
					/* UrlEncT */	"t=samp&c=@(foo)",
					/* UrlEncR */	"_type=samp\n&c=@(\n\tfoo\n)",
					/* MsgPack */	"82A55F74797065A473616D70A16391A3666F6F",
					/* MsgPackT */	"82A174A473616D70A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>samp</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>samp</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>samp</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Samp o) {
						assertInstanceOf(Samp.class, o);
					}
				}
			},
			{	/* 63 */
				new ComboInput<Script>(
					"Script",
					Script.class,
					script("text/javascript", new String[]{"\n\talert('hello world!');\n"}),
					/* Json */		"{_type:'script',a:{type:'text/javascript'},c:'\\n\\talert(\\'hello world!\\');\\n'}",
					/* JsonT */		"{t:'script',a:{type:'text/javascript'},c:'\\n\\talert(\\'hello world!\\');\\n'}",
					/* JsonR */		"{\n\t_type: 'script',\n\ta: {\n\t\ttype: 'text/javascript'\n\t},\n\tc: '\\n\\talert(\\'hello world!\\');\\n'\n}",
					/* Xml */		"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>",
					/* XmlT */		"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>",
					/* XmlR */		"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>\n",
					/* XmlNs */		"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>",
					/* Html */		"<script type='text/javascript'>\n\talert('hello world!');\n</script>",
					/* HtmlT */		"<script type='text/javascript'>\n\talert('hello world!');\n</script>",
					/* HtmlR */		"<script type='text/javascript'>\n\talert('hello world!');\n</script>\n",
					/* Uon */		"(_type=script,a=(type=text/javascript),c='\n\talert(~'hello world!~');\n')",
					/* UonT */		"(t=script,a=(type=text/javascript),c='\n\talert(~'hello world!~');\n')",
					/* UonR */		"(\n\t_type=script,\n\ta=(\n\t\ttype=text/javascript\n\t),\n\tc='\n\talert(~'hello world!~');\n'\n)",
					/* UrlEnc */	"_type=script&a=(type=text/javascript)&c='%0A%09alert(~'hello+world!~');%0A'",
					/* UrlEncT */	"t=script&a=(type=text/javascript)&c='%0A%09alert(~'hello+world!~');%0A'",
					/* UrlEncR */	"_type=script\n&a=(\n\ttype=text/javascript\n)\n&c='%0A%09alert(~'hello+world!~');%0A'",
					/* MsgPack */	"83A55F74797065A6736372697074A16181A474797065AF746578742F6A617661736372697074A163B90A09616C657274282768656C6C6F20776F726C642127293B0A",
					/* MsgPackT */	"83A174A6736372697074A16181A474797065AF746578742F6A617661736372697074A163B90A09616C657274282768656C6C6F20776F726C642127293B0A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>script</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text/javascript</jp:type>\n</jp:a>\n<jp:c>_x000A__x0009_alert('hello world!');_x000A_</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>script</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text/javascript</jp:type>\n</jp:a>\n<jp:c>_x000A__x0009_alert('hello world!');_x000A_</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>script</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:type>text/javascript</jp:type>\n    </jp:a>\n    <jp:c>_x000A__x0009_alert('hello world!');_x000A_</jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Script o) {
						assertInstanceOf(Script.class, o);
					}
				}
			},
			{	/* 64 */
				new ComboInput<Section>(
					"Section",
					Section.class,
					section(h1("foo"),p("bar")),
					/* Json */		"{_type:'section',c:[{_type:'h1',c:['foo']},{_type:'p',c:['bar']}]}",
					/* JsonT */		"{t:'section',c:[{t:'h1',c:['foo']},{t:'p',c:['bar']}]}",
					/* JsonR */		"{\n\t_type: 'section',\n\tc: [\n\t\t{\n\t\t\t_type: 'h1',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<section><h1>foo</h1><p>bar</p></section>",
					/* XmlT */		"<section><h1>foo</h1><p>bar</p></section>",
					/* XmlR */		"<section><h1>foo</h1><p>bar</p></section>\n",
					/* XmlNs */		"<section><h1>foo</h1><p>bar</p></section>",
					/* Html */		"<section><h1>foo</h1><p>bar</p></section>",
					/* HtmlT */		"<section><h1>foo</h1><p>bar</p></section>",
					/* HtmlR */		"<section><h1>foo</h1><p>bar</p></section>\n",
					/* Uon */		"(_type=section,c=@((_type=h1,c=@(foo)),(_type=p,c=@(bar))))",
					/* UonT */		"(t=section,c=@((t=h1,c=@(foo)),(t=p,c=@(bar))))",
					/* UonR */		"(\n\t_type=section,\n\tc=@(\n\t\t(\n\t\t\t_type=h1,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=section&c=@((_type=h1,c=@(foo)),(_type=p,c=@(bar)))",
					/* UrlEncT */	"t=section&c=@((t=h1,c=@(foo)),(t=p,c=@(bar)))",
					/* UrlEncR */	"_type=section\n&c=@(\n\t(\n\t\t_type=h1,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t),\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A773656374696F6EA1639282A55F74797065A26831A16391A3666F6F82A55F74797065A170A16391A3626172",
					/* MsgPackT */	"82A174A773656374696F6EA1639282A174A26831A16391A3666F6F82A174A170A16391A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>section</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>section</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>section</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h1</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Section o) {
						assertInstanceOf(Section.class, o);
						assertInstanceOf(H1.class, o.getChild(0));
						assertInstanceOf(P.class, o.getChild(1));
					}
				}
			},
			{	/* 65 */
				new ComboInput<Select>(
					"Select/Optgroup/Option",
					Select.class,
					select("foo", optgroup(option("o1","v1")).label("bar")),
					/* Json */		"{_type:'select',a:{name:'foo'},c:[{_type:'optgroup',a:{label:'bar'},c:[{_type:'option',a:{value:'o1'},c:'v1'}]}]}",
					/* JsonT */		"{t:'select',a:{name:'foo'},c:[{t:'optgroup',a:{label:'bar'},c:[{t:'option',a:{value:'o1'},c:'v1'}]}]}",
					/* JsonR */		"{\n\t_type: 'select',\n\ta: {\n\t\tname: 'foo'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'optgroup',\n\t\t\ta: {\n\t\t\t\tlabel: 'bar'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'option',\n\t\t\t\t\ta: {\n\t\t\t\t\t\tvalue: 'o1'\n\t\t\t\t\t},\n\t\t\t\t\tc: 'v1'\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
					/* XmlT */		"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
					/* XmlR */		"<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n",
					/* XmlNs */		"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
					/* Html */		"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
					/* HtmlT */		"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
					/* HtmlR */		"<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n",
					/* Uon */		"(_type=select,a=(name=foo),c=@((_type=optgroup,a=(label=bar),c=@((_type=option,a=(value=o1),c=v1)))))",
					/* UonT */		"(t=select,a=(name=foo),c=@((t=optgroup,a=(label=bar),c=@((t=option,a=(value=o1),c=v1)))))",
					/* UonR */		"(\n\t_type=select,\n\ta=(\n\t\tname=foo\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=optgroup,\n\t\t\ta=(\n\t\t\t\tlabel=bar\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=option,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tvalue=o1\n\t\t\t\t\t),\n\t\t\t\t\tc=v1\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=select&a=(name=foo)&c=@((_type=optgroup,a=(label=bar),c=@((_type=option,a=(value=o1),c=v1))))",
					/* UrlEncT */	"t=select&a=(name=foo)&c=@((t=optgroup,a=(label=bar),c=@((t=option,a=(value=o1),c=v1))))",
					/* UrlEncR */	"_type=select\n&a=(\n\tname=foo\n)\n&c=@(\n\t(\n\t\t_type=optgroup,\n\t\ta=(\n\t\t\tlabel=bar\n\t\t),\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=option,\n\t\t\t\ta=(\n\t\t\t\t\tvalue=o1\n\t\t\t\t),\n\t\t\t\tc=v1\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A673656C656374A16181A46E616D65A3666F6FA1639183A55F74797065A86F707467726F7570A16181A56C6162656CA3626172A1639183A55F74797065A66F7074696F6EA16181A576616C7565A26F31A163A27631",
					/* MsgPackT */	"83A174A673656C656374A16181A46E616D65A3666F6FA1639183A174A86F707467726F7570A16181A56C6162656CA3626172A1639183A174A66F7074696F6EA16181A576616C7565A26F31A163A27631",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>select</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>optgroup</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:label>bar</jp:label>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>option</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:value>o1</jp:value>\n</jp:a>\n<jp:c>v1</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>select</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>optgroup</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:label>bar</jp:label>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>option</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:value>o1</jp:value>\n</jp:a>\n<jp:c>v1</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>select</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:name>foo</jp:name>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>optgroup</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:label>bar</jp:label>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>option</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:value>o1</jp:value>\n                </jp:a>\n                <jp:c>v1</jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Select o) {
						assertInstanceOf(Select.class, o);
						assertInstanceOf(Optgroup.class, o.getChild(0));
						assertInstanceOf(Option.class, o.getChild(0,0));
					}
				}
			},
			{	/* 66 */
				new ComboInput<P>(
					"Small",
					P.class,
					p(small("foo")),
					/* Json */		"{_type:'p',c:[{_type:'small',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'small',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'small',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><small>foo</small></p>",
					/* XmlT */		"<p><small>foo</small></p>",
					/* XmlR */		"<p><small>foo</small></p>\n",
					/* XmlNs */		"<p><small>foo</small></p>",
					/* Html */		"<p><small>foo</small></p>",
					/* HtmlT */		"<p><small>foo</small></p>",
					/* HtmlR */		"<p><small>foo</small></p>\n",
					/* Uon */		"(_type=p,c=@((_type=small,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=small,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=small,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=small,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=small,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=small,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A5736D616C6CA16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A5736D616C6CA16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>small</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>small</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>small</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Small.class, o.getChild(0));
					}
				}
			},
			{	/* 67 */
				new ComboInput<P>(
					"Span",
					P.class,
					p("My mother has ",span().style("color:blue").child("blue"), " eyes."),
					/* Json */		"{_type:'p',c:['My mother has ',{_type:'span',a:{style:'color:blue'},c:['blue']},' eyes.']}",
					/* JsonT */		"{t:'p',c:['My mother has ',{t:'span',a:{style:'color:blue'},c:['blue']},' eyes.']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'My mother has ',\n\t\t{\n\t\t\t_type: 'span',\n\t\t\ta: {\n\t\t\t\tstyle: 'color:blue'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'blue'\n\t\t\t]\n\t\t},\n\t\t' eyes.'\n\t]\n}",
					/* Xml */		"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>",
					/* XmlT */		"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>",
					/* XmlR */		"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>\n",
					/* XmlNs */		"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>",
					/* Html */		"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>",
					/* HtmlT */		"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>",
					/* HtmlR */		"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>\n",
					/* Uon */		"(_type=p,c=@('My mother has ',(_type=span,a=(style=color:blue),c=@(blue)),' eyes.'))",
					/* UonT */		"(t=p,c=@('My mother has ',(t=span,a=(style=color:blue),c=@(blue)),' eyes.'))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t'My mother has ',\n\t\t(\n\t\t\t_type=span,\n\t\t\ta=(\n\t\t\t\tstyle=color:blue\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tblue\n\t\t\t)\n\t\t),\n\t\t' eyes.'\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@('My+mother+has+',(_type=span,a=(style=color:blue),c=@(blue)),'+eyes.')",
					/* UrlEncT */	"t=p&c=@('My+mother+has+',(t=span,a=(style=color:blue),c=@(blue)),'+eyes.')",
					/* UrlEncR */	"_type=p\n&c=@(\n\t'My+mother+has+',\n\t(\n\t\t_type=span,\n\t\ta=(\n\t\t\tstyle=color:blue\n\t\t),\n\t\tc=@(\n\t\t\tblue\n\t\t)\n\t),\n\t'+eyes.'\n)",
					/* MsgPack */	"82A55F74797065A170A16393AE4D79206D6F74686572206861732083A55F74797065A47370616EA16181A57374796C65AA636F6C6F723A626C7565A16391A4626C7565A620657965732E",
					/* MsgPackT */	"82A174A170A16393AE4D79206D6F74686572206861732083A174A47370616EA16181A57374796C65AA636F6C6F723A626C7565A16391A4626C7565A620657965732E",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>My mother has_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>span</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:style>color:blue</jp:style>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>blue</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>_x0020_eyes.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>My mother has_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>span</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:style>color:blue</jp:style>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>blue</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>_x0020_eyes.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>My mother has_x0020_</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>span</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:style>color:blue</jp:style>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>blue</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>_x0020_eyes.</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Span.class, o.getChild(1));
					}
				}
			},
			{	/* 68 */
				new ComboInput<P>(
					"Strong",
					P.class,
					p(strong("foo")),
					/* Json */		"{_type:'p',c:[{_type:'strong',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'strong',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'strong',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><strong>foo</strong></p>",
					/* XmlT */		"<p><strong>foo</strong></p>",
					/* XmlR */		"<p><strong>foo</strong></p>\n",
					/* XmlNs */		"<p><strong>foo</strong></p>",
					/* Html */		"<p><strong>foo</strong></p>",
					/* HtmlT */		"<p><strong>foo</strong></p>",
					/* HtmlR */		"<p><strong>foo</strong></p>\n",
					/* Uon */		"(_type=p,c=@((_type=strong,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=strong,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=strong,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=strong,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=strong,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=strong,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A67374726F6E67A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A67374726F6E67A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>strong</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>strong</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>strong</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Strong.class, o.getChild(0));
					}
				}
			},
			{	/* 69 */
				new ComboInput<Head>(
					"Style",
					Head.class,
					head(style("\n\th1 {color:red;}\n\tp: {color:blue;}\n")),
					/* Json */		"{_type:'head',c:[{_type:'style',c:'\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'}]}",
					/* JsonT */		"{t:'head',c:[{t:'style',c:'\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'}]}",
					/* JsonR */		"{\n\t_type: 'head',\n\tc: [\n\t\t{\n\t\t\t_type: 'style',\n\t\t\tc: '\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'\n\t\t}\n\t]\n}",
					/* Xml */		"<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>",
					/* XmlT */		"<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>",
					/* XmlR */		"<head>\n\t<style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style>\n</head>\n",
					/* XmlNs */		"<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>",
					/* Html */		"<head><style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style></head>",
					/* HtmlT */		"<head><style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style></head>",
					/* HtmlR */		"<head>\n\t<style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style>\n</head>\n",
					/* Uon */		"(_type=head,c=@((_type=style,c='\n\th1 {color:red;}\n\tp: {color:blue;}\n')))",
					/* UonT */		"(t=head,c=@((t=style,c='\n\th1 {color:red;}\n\tp: {color:blue;}\n')))",
					/* UonR */		"(\n\t_type=head,\n\tc=@(\n\t\t(\n\t\t\t_type=style,\n\t\t\tc='\n\th1 {color:red;}\n\tp: {color:blue;}\n'\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=head&c=@((_type=style,c='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'))",
					/* UrlEncT */	"t=head&c=@((t=style,c='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'))",
					/* UrlEncR */	"_type=head\n&c=@(\n\t(\n\t\t_type=style,\n\t\tc='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'\n\t)\n)",
					/* MsgPack */	"82A55F74797065A468656164A1639182A55F74797065A57374796C65A163D9240A096831207B636F6C6F723A7265643B7D0A09703A207B636F6C6F723A626C75653B7D0A",
					/* MsgPackT */	"82A174A468656164A1639182A174A57374796C65A163D9240A096831207B636F6C6F723A7265643B7D0A09703A207B636F6C6F723A626C75653B7D0A",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>head</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>style</jp:_type>\n<jp:c>_x000A__x0009_h1 {color:red;}_x000A__x0009_p: {color:blue;}_x000A_</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>head</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>style</jp:t>\n<jp:c>_x000A__x0009_h1 {color:red;}_x000A__x0009_p: {color:blue;}_x000A_</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>head</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>style</jp:_type>\n          <jp:c>_x000A__x0009_h1 {color:red;}_x000A__x0009_p: {color:blue;}_x000A_</jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Head o) {
						assertInstanceOf(Head.class, o);
						assertInstanceOf(Style.class, o.getChild(0));
					}
				}
			},
			{	/* 70 */
				new ComboInput<P>(
					"Sub",
					P.class,
					p(sub("foo")),
					/* Json */		"{_type:'p',c:[{_type:'sub',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'sub',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'sub',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><sub>foo</sub></p>",
					/* XmlT */		"<p><sub>foo</sub></p>",
					/* XmlR */		"<p><sub>foo</sub></p>\n",
					/* XmlNs */		"<p><sub>foo</sub></p>",
					/* Html */		"<p><sub>foo</sub></p>",
					/* HtmlT */		"<p><sub>foo</sub></p>",
					/* HtmlR */		"<p><sub>foo</sub></p>\n",
					/* Uon */		"(_type=p,c=@((_type=sub,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=sub,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=sub,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=sub,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=sub,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=sub,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A3737562A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A3737562A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>sub</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>sub</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>sub</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Sub.class, o.getChild(0));
					}
				}
			},
			{	/* 71 */
				new ComboInput<P>(
					"Sup",
					P.class,
					p(sup("foo")),
					/* Json */		"{_type:'p',c:[{_type:'sup',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'sup',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'sup',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><sup>foo</sup></p>",
					/* XmlT */		"<p><sup>foo</sup></p>",
					/* XmlR */		"<p><sup>foo</sup></p>\n",
					/* XmlNs */		"<p><sup>foo</sup></p>",
					/* Html */		"<p><sup>foo</sup></p>",
					/* HtmlT */		"<p><sup>foo</sup></p>",
					/* HtmlR */		"<p><sup>foo</sup></p>\n",
					/* Uon */		"(_type=p,c=@((_type=sup,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=sup,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=sup,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=sup,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=sup,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=sup,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A3737570A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A3737570A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>sup</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>sup</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>sup</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Sup.class, o.getChild(0));
					}
				}
			},
			{	/* 72 */
				new ComboInput<Table>(
					"Table/Colgroup/Col/Caption/THead/TBody/TFoot/Tr/Th/Td-1",
					Table.class,
					table(
						caption("caption1"),
						colgroup(
							col()._class("foo"),
							col()._class("bar")
						),
						thead(tr(th("c1"),th("c2"))),
						tbody(tr(td("v1"),td("v2"))),
						tfoot(tr(td("f1"),td("f2")))
					),
					/* Json */		"{_type:'table',c:[{_type:'caption',c:['caption1']},{_type:'colgroup',c:[{_type:'col',a:{'class':'foo'}},{_type:'col',a:{'class':'bar'}}]},{_type:'thead',c:[{_type:'tr',c:[{_type:'th',c:['c1']},{_type:'th',c:['c2']}]}]},{_type:'tbody',c:[{_type:'tr',c:[{_type:'td',c:['v1']},{_type:'td',c:['v2']}]}]},{_type:'tfoot',c:[{_type:'tr',c:[{_type:'td',c:['f1']},{_type:'td',c:['f2']}]}]}]}",
					/* JsonT */		"{t:'table',c:[{t:'caption',c:['caption1']},{t:'colgroup',c:[{t:'col',a:{'class':'foo'}},{t:'col',a:{'class':'bar'}}]},{t:'thead',c:[{t:'tr',c:[{t:'th',c:['c1']},{t:'th',c:['c2']}]}]},{t:'tbody',c:[{t:'tr',c:[{t:'td',c:['v1']},{t:'td',c:['v2']}]}]},{t:'tfoot',c:[{t:'tr',c:[{t:'td',c:['f1']},{t:'td',c:['f2']}]}]}]}",
					/* JsonR */		"{\n\t_type: 'table',\n\tc: [\n\t\t{\n\t\t\t_type: 'caption',\n\t\t\tc: [\n\t\t\t\t'caption1'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'colgroup',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'col',\n\t\t\t\t\ta: {\n\t\t\t\t\t\t'class': 'foo'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'col',\n\t\t\t\t\ta: {\n\t\t\t\t\t\t'class': 'bar'\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'thead',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'tr',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'th',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'c1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'th',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'c2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'tbody',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'tr',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'v1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'v2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'tfoot',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'tr',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'f1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'f2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */
						"<table>"
							+"<caption>caption1</caption>"
							+"<colgroup>"
								+"<col class='foo'/>"
								+"<col class='bar'/>"
							+"</colgroup>"
							+"<thead>"
								+"<tr>"
									+"<th>c1</th>"
									+"<th>c2</th>"
								+"</tr>"
							+"</thead>"
							+"<tbody>"
								+"<tr>"
									+"<td>v1</td>"
									+"<td>v2</td>"
								+"</tr>"
							+"</tbody>"
							+"<tfoot>"
								+"<tr>"
									+"<td>f1</td>"
									+"<td>f2</td>"
								+"</tr>"
							+"</tfoot>"
						+"</table>",
					/* XmlT */
						"<table>"
							+"<caption>caption1</caption>"
							+"<colgroup>"
								+"<col class='foo'/>"
								+"<col class='bar'/>"
							+"</colgroup>"
							+"<thead>"
								+"<tr>"
									+"<th>c1</th>"
									+"<th>c2</th>"
								+"</tr>"
							+"</thead>"
							+"<tbody>"
								+"<tr>"
									+"<td>v1</td>"
									+"<td>v2</td>"
								+"</tr>"
							+"</tbody>"
							+"<tfoot>"
								+"<tr>"
									+"<td>f1</td>"
									+"<td>f2</td>"
								+"</tr>"
							+"</tfoot>"
						+"</table>",
					/* XmlR */
						"<table>\n"
							+"\t<caption>caption1</caption>\n"
							+"\t<colgroup>\n"
								+"\t\t<col class='foo'/>\n"
								+"\t\t<col class='bar'/>\n"
							+"\t</colgroup>\n"
							+"\t<thead>\n"
								+"\t\t<tr>\n"
									+"\t\t\t<th>c1</th>\n"
									+"\t\t\t<th>c2</th>\n"
								+"\t\t</tr>\n"
							+"\t</thead>\n"
							+"\t<tbody>\n"
								+"\t\t<tr>\n"
									+"\t\t\t<td>v1</td>\n"
									+"\t\t\t<td>v2</td>\n"
								+"\t\t</tr>\n"
							+"\t</tbody>\n"
							+"\t<tfoot>\n"
								+"\t\t<tr>\n"
									+"\t\t\t<td>f1</td>\n"
									+"\t\t\t<td>f2</td>\n"
								+"\t\t</tr>\n"
							+"\t</tfoot>\n"
						+"</table>\n",
					/* XmlNs */
						"<table>"
							+"<caption>caption1</caption>"
							+"<colgroup>"
								+"<col class='foo'/>"
								+"<col class='bar'/>"
							+"</colgroup>"
							+"<thead>"
								+"<tr>"
									+"<th>c1</th>"
									+"<th>c2</th>"
								+"</tr>"
							+"</thead>"
							+"<tbody>"
								+"<tr>"
									+"<td>v1</td>"
									+"<td>v2</td>"
								+"</tr>"
							+"</tbody>"
							+"<tfoot>"
								+"<tr>"
									+"<td>f1</td>"
									+"<td>f2</td>"
								+"</tr>"
							+"</tfoot>"
						+"</table>",
					/* Html */
						"<table>"
							+"<caption>caption1</caption>"
							+"<colgroup>"
								+"<col class='foo'/>"
								+"<col class='bar'/>"
							+"</colgroup>"
							+"<thead>"
								+"<tr>"
									+"<th>c1</th>"
									+"<th>c2</th>"
								+"</tr>"
							+"</thead>"
							+"<tbody>"
								+"<tr>"
									+"<td>v1</td>"
									+"<td>v2</td>"
								+"</tr>"
							+"</tbody>"
							+"<tfoot>"
								+"<tr>"
									+"<td>f1</td>"
									+"<td>f2</td>"
								+"</tr>"
							+"</tfoot>"
						+"</table>",
					/* HtmlT */
						"<table>"
							+"<caption>caption1</caption>"
							+"<colgroup>"
								+"<col class='foo'/>"
								+"<col class='bar'/>"
							+"</colgroup>"
							+"<thead>"
								+"<tr>"
									+"<th>c1</th>"
									+"<th>c2</th>"
								+"</tr>"
							+"</thead>"
							+"<tbody>"
								+"<tr>"
									+"<td>v1</td>"
									+"<td>v2</td>"
								+"</tr>"
							+"</tbody>"
							+"<tfoot>"
								+"<tr>"
									+"<td>f1</td>"
									+"<td>f2</td>"
								+"</tr>"
							+"</tfoot>"
						+"</table>",
					/* HtmlR */
						"<table>\n"
							+"\t<caption>caption1</caption>\n"
							+"\t<colgroup>\n"
								+"\t\t<col class='foo'/>\n"
								+"\t\t<col class='bar'/>\n"
							+"\t</colgroup>\n"
							+"\t<thead>\n"
								+"\t\t<tr>\n"
									+"\t\t\t<th>c1</th>\n"
									+"\t\t\t<th>c2</th>\n"
								+"\t\t</tr>\n"
							+"\t</thead>\n"
							+"\t<tbody>\n"
								+"\t\t<tr>\n"
									+"\t\t\t<td>v1</td>\n"
									+"\t\t\t<td>v2</td>\n"
								+"\t\t</tr>\n"
							+"\t</tbody>\n"
							+"\t<tfoot>\n"
								+"\t\t<tr>\n"
									+"\t\t\t<td>f1</td>\n"
									+"\t\t\t<td>f2</td>\n"
								+"\t\t</tr>\n"
							+"\t</tfoot>\n"
						+"</table>\n",
					/* Uon */		"(_type=table,c=@((_type=caption,c=@(caption1)),(_type=colgroup,c=@((_type=col,a=(class=foo)),(_type=col,a=(class=bar)))),(_type=thead,c=@((_type=tr,c=@((_type=th,c=@(c1)),(_type=th,c=@(c2)))))),(_type=tbody,c=@((_type=tr,c=@((_type=td,c=@(v1)),(_type=td,c=@(v2)))))),(_type=tfoot,c=@((_type=tr,c=@((_type=td,c=@(f1)),(_type=td,c=@(f2))))))))",
					/* UonT */		"(t=table,c=@((t=caption,c=@(caption1)),(t=colgroup,c=@((t=col,a=(class=foo)),(t=col,a=(class=bar)))),(t=thead,c=@((t=tr,c=@((t=th,c=@(c1)),(t=th,c=@(c2)))))),(t=tbody,c=@((t=tr,c=@((t=td,c=@(v1)),(t=td,c=@(v2)))))),(t=tfoot,c=@((t=tr,c=@((t=td,c=@(f1)),(t=td,c=@(f2))))))))",
					/* UonR */		"(\n\t_type=table,\n\tc=@(\n\t\t(\n\t\t\t_type=caption,\n\t\t\tc=@(\n\t\t\t\tcaption1\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=colgroup,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=col,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tclass=foo\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=col,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tclass=bar\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=thead,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=tr,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tc1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tc2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=tbody,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=tr,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tv1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tv2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=tfoot,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=tr,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tf1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tf2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=table&c=@((_type=caption,c=@(caption1)),(_type=colgroup,c=@((_type=col,a=(class=foo)),(_type=col,a=(class=bar)))),(_type=thead,c=@((_type=tr,c=@((_type=th,c=@(c1)),(_type=th,c=@(c2)))))),(_type=tbody,c=@((_type=tr,c=@((_type=td,c=@(v1)),(_type=td,c=@(v2)))))),(_type=tfoot,c=@((_type=tr,c=@((_type=td,c=@(f1)),(_type=td,c=@(f2)))))))",
					/* UrlEncT */	"t=table&c=@((t=caption,c=@(caption1)),(t=colgroup,c=@((t=col,a=(class=foo)),(t=col,a=(class=bar)))),(t=thead,c=@((t=tr,c=@((t=th,c=@(c1)),(t=th,c=@(c2)))))),(t=tbody,c=@((t=tr,c=@((t=td,c=@(v1)),(t=td,c=@(v2)))))),(t=tfoot,c=@((t=tr,c=@((t=td,c=@(f1)),(t=td,c=@(f2)))))))",
					/* UrlEncR */	"_type=table\n&c=@(\n\t(\n\t\t_type=caption,\n\t\tc=@(\n\t\t\tcaption1\n\t\t)\n\t),\n\t(\n\t\t_type=colgroup,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=col,\n\t\t\t\ta=(\n\t\t\t\t\tclass=foo\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=col,\n\t\t\t\ta=(\n\t\t\t\t\tclass=bar\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=thead,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=tr,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tc1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tc2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=tbody,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=tr,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tv1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tv2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=tfoot,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=tr,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tf1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tf2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A57461626C65A1639582A55F74797065A763617074696F6EA16391A863617074696F6E3182A55F74797065A8636F6C67726F7570A1639282A55F74797065A3636F6CA16181A5636C617373A3666F6F82A55F74797065A3636F6CA16181A5636C617373A362617282A55F74797065A57468656164A1639182A55F74797065A27472A1639282A55F74797065A27468A16391A2633182A55F74797065A27468A16391A2633282A55F74797065A574626F6479A1639182A55F74797065A27472A1639282A55F74797065A27464A16391A2763182A55F74797065A27464A16391A2763282A55F74797065A574666F6F74A1639182A55F74797065A27472A1639282A55F74797065A27464A16391A2663182A55F74797065A27464A16391A26632",
					/* MsgPackT */	"82A174A57461626C65A1639582A174A763617074696F6EA16391A863617074696F6E3182A174A8636F6C67726F7570A1639282A174A3636F6CA16181A5636C617373A3666F6F82A174A3636F6CA16181A5636C617373A362617282A174A57468656164A1639182A174A27472A1639282A174A27468A16391A2633182A174A27468A16391A2633282A174A574626F6479A1639182A174A27472A1639282A174A27464A16391A2763182A174A27464A16391A2763282A174A574666F6F74A1639182A174A27472A1639282A174A27464A16391A2663182A174A27464A16391A26632",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>table</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>caption</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>caption1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>colgroup</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>col</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:class>foo</jp:class>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>col</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:class>bar</jp:class>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>thead</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>th</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>th</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tbody</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tfoot</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>table</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>caption</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>caption1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>colgroup</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>col</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:class>foo</jp:class>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>col</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:class>bar</jp:class>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>thead</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>th</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>th</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tbody</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tfoot</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>table</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>caption</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>caption1</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>colgroup</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>col</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:class>foo</jp:class>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>col</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:class>bar</jp:class>\n                </jp:a>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>thead</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>tr</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>th</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>c1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>th</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>c2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>tbody</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>tr</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>v1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>v2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>tfoot</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>tr</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>f1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>f2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Table o) {
						assertInstanceOf(Table.class, o);
						assertInstanceOf(Caption.class, o.getChild(0));
						assertInstanceOf(Colgroup.class, o.getChild(1));
						assertInstanceOf(Col.class, o.getChild(1,0));
						assertInstanceOf(Col.class, o.getChild(1,1));
						assertInstanceOf(Thead.class, o.getChild(2));
						assertInstanceOf(Tr.class, o.getChild(2,0));
						assertInstanceOf(Th.class, o.getChild(2,0,0));
						assertInstanceOf(Th.class, o.getChild(2,0,1));
						assertInstanceOf(Tbody.class, o.getChild(3));
						assertInstanceOf(Tr.class, o.getChild(3,0));
						assertInstanceOf(Td.class, o.getChild(3,0,0));
						assertInstanceOf(Td.class, o.getChild(3,0,1));
						assertInstanceOf(Tfoot.class, o.getChild(4));
						assertInstanceOf(Tr.class, o.getChild(4,0));
						assertInstanceOf(Td.class, o.getChild(4,0,0));
						assertInstanceOf(Td.class, o.getChild(4,0,1));
					}
				}
			},
			{	/* 73 */
				new ComboInput<Template>(
					"Template",
					Template.class,
					template("foo",div("bar")),
					/* Json */		"{_type:'template',a:{id:'foo'},c:[{_type:'div',c:['bar']}]}",
					/* JsonT */		"{t:'template',a:{id:'foo'},c:[{t:'div',c:['bar']}]}",
					/* JsonR */		"{\n\t_type: 'template',\n\ta: {\n\t\tid: 'foo'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'div',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<template id='foo'><div>bar</div></template>",
					/* XmlT */		"<template id='foo'><div>bar</div></template>",
					/* XmlR */		"<template id='foo'><div>bar</div></template>\n",
					/* XmlNs */		"<template id='foo'><div>bar</div></template>",
					/* Html */		"<template id='foo'><div>bar</div></template>",
					/* HtmlT */		"<template id='foo'><div>bar</div></template>",
					/* HtmlR */		"<template id='foo'><div>bar</div></template>\n",
					/* Uon */		"(_type=template,a=(id=foo),c=@((_type=div,c=@(bar))))",
					/* UonT */		"(t=template,a=(id=foo),c=@((t=div,c=@(bar))))",
					/* UonR */		"(\n\t_type=template,\n\ta=(\n\t\tid=foo\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=div,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=template&a=(id=foo)&c=@((_type=div,c=@(bar)))",
					/* UrlEncT */	"t=template&a=(id=foo)&c=@((t=div,c=@(bar)))",
					/* UrlEncR */	"_type=template\n&a=(\n\tid=foo\n)\n&c=@(\n\t(\n\t\t_type=div,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A874656D706C617465A16181A26964A3666F6FA1639182A55F74797065A3646976A16391A3626172",
					/* MsgPackT */	"83A174A874656D706C617465A16181A26964A3666F6FA1639182A174A3646976A16391A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>template</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>div</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>template</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>div</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>template</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:id>foo</jp:id>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>div</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Template o) {
						assertInstanceOf(Template.class, o);
						assertInstanceOf(Div.class, o.getChild(0));
					}
				}
			},
			{	/* 74 */
				new ComboInput<Textarea>(
					"Textarea",
					Textarea.class,
					textarea("foo", "bar"),
					/* Json */		"{_type:'textarea',a:{name:'foo'},c:'bar'}",
					/* JsonT */		"{t:'textarea',a:{name:'foo'},c:'bar'}",
					/* JsonR */		"{\n\t_type: 'textarea',\n\ta: {\n\t\tname: 'foo'\n\t},\n\tc: 'bar'\n}",
					/* Xml */		"<textarea name='foo'>bar</textarea>",
					/* XmlT */		"<textarea name='foo'>bar</textarea>",
					/* XmlR */		"<textarea name='foo'>bar</textarea>\n",
					/* XmlNs */		"<textarea name='foo'>bar</textarea>",
					/* Html */		"<textarea name='foo'>bar</textarea>",
					/* HtmlT */		"<textarea name='foo'>bar</textarea>",
					/* HtmlR */		"<textarea name='foo'>bar</textarea>\n",
					/* Uon */		"(_type=textarea,a=(name=foo),c=bar)",
					/* UonT */		"(t=textarea,a=(name=foo),c=bar)",
					/* UonR */		"(\n\t_type=textarea,\n\ta=(\n\t\tname=foo\n\t),\n\tc=bar\n)",
					/* UrlEnc */	"_type=textarea&a=(name=foo)&c=bar",
					/* UrlEncT */	"t=textarea&a=(name=foo)&c=bar",
					/* UrlEncR */	"_type=textarea\n&a=(\n\tname=foo\n)\n&c=bar",
					/* MsgPack */	"83A55F74797065A87465787461726561A16181A46E616D65A3666F6FA163A3626172",
					/* MsgPackT */	"83A174A87465787461726561A16181A46E616D65A3666F6FA163A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>textarea</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>bar</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>textarea</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>bar</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>textarea</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:name>foo</jp:name>\n    </jp:a>\n    <jp:c>bar</jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Textarea o) {
						assertInstanceOf(Textarea.class, o);
					}
				}
			},
			{	/* 75 */
				new ComboInput<P>(
					"Time",
					P.class,
					p("I have a date on ",time("Valentines day").datetime("2016-02-14 18:00"), "."),
					/* Json */		"{_type:'p',c:['I have a date on ',{_type:'time',a:{datetime:'2016-02-14 18:00'},c:['Valentines day']},'.']}",
					/* JsonT */		"{t:'p',c:['I have a date on ',{t:'time',a:{datetime:'2016-02-14 18:00'},c:['Valentines day']},'.']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'I have a date on ',\n\t\t{\n\t\t\t_type: 'time',\n\t\t\ta: {\n\t\t\t\tdatetime: '2016-02-14 18:00'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'Valentines day'\n\t\t\t]\n\t\t},\n\t\t'.'\n\t]\n}",
					/* Xml */		"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
					/* XmlT */		"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
					/* XmlR */		"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n",
					/* XmlNs */		"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
					/* Html */		"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
					/* HtmlT */		"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
					/* HtmlR */		"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n",
					/* Uon */		"(_type=p,c=@('I have a date on ',(_type=time,a=(datetime='2016-02-14 18:00'),c=@('Valentines day')),.))",
					/* UonT */		"(t=p,c=@('I have a date on ',(t=time,a=(datetime='2016-02-14 18:00'),c=@('Valentines day')),.))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t'I have a date on ',\n\t\t(\n\t\t\t_type=time,\n\t\t\ta=(\n\t\t\t\tdatetime='2016-02-14 18:00'\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\t'Valentines day'\n\t\t\t)\n\t\t),\n\t\t.\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@('I+have+a+date+on+',(_type=time,a=(datetime='2016-02-14+18:00'),c=@('Valentines+day')),.)",
					/* UrlEncT */	"t=p&c=@('I+have+a+date+on+',(t=time,a=(datetime='2016-02-14+18:00'),c=@('Valentines+day')),.)",
					/* UrlEncR */	"_type=p\n&c=@(\n\t'I+have+a+date+on+',\n\t(\n\t\t_type=time,\n\t\ta=(\n\t\t\tdatetime='2016-02-14+18:00'\n\t\t),\n\t\tc=@(\n\t\t\t'Valentines+day'\n\t\t)\n\t),\n\t.\n)",
					/* MsgPack */	"82A55F74797065A170A16393B149206861766520612064617465206F6E2083A55F74797065A474696D65A16181A86461746574696D65B0323031362D30322D31342031383A3030A16391AE56616C656E74696E657320646179A12E",
					/* MsgPackT */	"82A174A170A16393B149206861766520612064617465206F6E2083A174A474696D65A16181A86461746574696D65B0323031362D30322D31342031383A3030A16391AE56616C656E74696E657320646179A12E",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>I have a date on_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>time</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:datetime>2016-02-14 18:00</jp:datetime>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Valentines day</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>I have a date on_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>time</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:datetime>2016-02-14 18:00</jp:datetime>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Valentines day</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>I have a date on_x0020_</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>time</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:datetime>2016-02-14 18:00</jp:datetime>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Valentines day</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>.</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Time.class, o.getChild(1));
					}
				}
			},
			{	/* 76 */
				new ComboInput<P>(
					"U",
					P.class,
					p(u("foo")),
					/* Json */		"{_type:'p',c:[{_type:'u',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'u',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'u',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><u>foo</u></p>",
					/* XmlT */		"<p><u>foo</u></p>",
					/* XmlR */		"<p><u>foo</u></p>\n",
					/* XmlNs */		"<p><u>foo</u></p>",
					/* Html */		"<p><u>foo</u></p>",
					/* HtmlT */		"<p><u>foo</u></p>",
					/* HtmlR */		"<p><u>foo</u></p>\n",
					/* Uon */		"(_type=p,c=@((_type=u,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=u,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=u,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=u,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=u,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=u,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A175A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A175A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>u</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>u</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>u</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(U.class, o.getChild(0));
					}
				}
			},
			{	/* 77 */
				new ComboInput<Ul>(
					"Ul/Li",
					Ul.class,
					ul(li("foo")),
					/* Json */		"{_type:'ul',c:[{_type:'li',c:['foo']}]}",
					/* JsonT */		"{t:'ul',c:[{t:'li',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'ul',\n\tc: [\n\t\t{\n\t\t\t_type: 'li',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<ul><li>foo</li></ul>",
					/* XmlT */		"<ul><li>foo</li></ul>",
					/* XmlR */		"<ul>\n\t<li>foo</li>\n</ul>\n",
					/* XmlNs */		"<ul><li>foo</li></ul>",
					/* Html */		"<ul><li>foo</li></ul>",
					/* HtmlT */		"<ul><li>foo</li></ul>",
					/* HtmlR */		"<ul>\n\t<li>foo</li>\n</ul>\n",
					/* Uon */		"(_type=ul,c=@((_type=li,c=@(foo))))",
					/* UonT */		"(t=ul,c=@((t=li,c=@(foo))))",
					/* UonR */		"(\n\t_type=ul,\n\tc=@(\n\t\t(\n\t\t\t_type=li,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=ul&c=@((_type=li,c=@(foo)))",
					/* UrlEncT */	"t=ul&c=@((t=li,c=@(foo)))",
					/* UrlEncR */	"_type=ul\n&c=@(\n\t(\n\t\t_type=li,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A2756CA1639182A55F74797065A26C69A16391A3666F6F",
					/* MsgPackT */	"82A174A2756CA1639182A174A26C69A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>ul</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>li</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>ul</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>li</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>ul</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>li</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Ul o) {
						assertInstanceOf(Ul.class, o);
						assertInstanceOf(Li.class, o.getChild(0));
					}
				}
			},
			{	/* 78 */
				new ComboInput<P>(
					"Var",
					P.class,
					p(var("foo")),
					/* Json */		"{_type:'p',c:[{_type:'var',c:['foo']}]}",
					/* JsonT */		"{t:'p',c:[{t:'var',c:['foo']}]}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'var',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}",
					/* Xml */		"<p><var>foo</var></p>",
					/* XmlT */		"<p><var>foo</var></p>",
					/* XmlR */		"<p><var>foo</var></p>\n",
					/* XmlNs */		"<p><var>foo</var></p>",
					/* Html */		"<p><var>foo</var></p>",
					/* HtmlT */		"<p><var>foo</var></p>",
					/* HtmlR */		"<p><var>foo</var></p>\n",
					/* Uon */		"(_type=p,c=@((_type=var,c=@(foo))))",
					/* UonT */		"(t=p,c=@((t=var,c=@(foo))))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=var,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@((_type=var,c=@(foo)))",
					/* UrlEncT */	"t=p&c=@((t=var,c=@(foo)))",
					/* UrlEncR */	"_type=p\n&c=@(\n\t(\n\t\t_type=var,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)",
					/* MsgPack */	"82A55F74797065A170A1639182A55F74797065A3766172A16391A3666F6F",
					/* MsgPackT */	"82A174A170A1639182A174A3766172A16391A3666F6F",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>var</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>var</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>var</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Var.class, o.getChild(0));
					}
				}
			},
			{	/* 79 */
				new ComboInput<Video>(
					"Video/Source/Track",
					Video.class,
					video().width(100).height(200).controls(true).children(
						source("foo.mp4", "video/mp4"),
						track("subtitles_en.vtt", "subtitles").srclang("en")
					),
					/* Json */		"{_type:'video',a:{width:100,height:200,controls:'controls'},c:[{_type:'source',a:{src:'foo.mp4',type:'video/mp4'}},{_type:'track',a:{src:'subtitles_en.vtt',kind:'subtitles',srclang:'en'}}]}",
					/* JsonT */		"{t:'video',a:{width:100,height:200,controls:'controls'},c:[{t:'source',a:{src:'foo.mp4',type:'video/mp4'}},{t:'track',a:{src:'subtitles_en.vtt',kind:'subtitles',srclang:'en'}}]}",
					/* JsonR */		"{\n\t_type: 'video',\n\ta: {\n\t\twidth: 100,\n\t\theight: 200,\n\t\tcontrols: 'controls'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'source',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.mp4',\n\t\t\t\ttype: 'video/mp4'\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'track',\n\t\t\ta: {\n\t\t\t\tsrc: 'subtitles_en.vtt',\n\t\t\t\tkind: 'subtitles',\n\t\t\t\tsrclang: 'en'\n\t\t\t}\n\t\t}\n\t]\n}",
					/* Xml */		"<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
					/* XmlT */		"<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
					/* XmlR */		"<video width='100' height='200' controls='controls'>\n\t<source src='foo.mp4' type='video/mp4'/>\n\t<track src='subtitles_en.vtt' kind='subtitles' srclang='en'/>\n</video>\n",
					/* XmlNs */		"<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
					/* Html */		"<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
					/* HtmlT */		"<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
					/* HtmlR */		"<video width='100' height='200' controls='controls'>\n\t<source src='foo.mp4' type='video/mp4'/>\n\t<track src='subtitles_en.vtt' kind='subtitles' srclang='en'/>\n</video>\n",
					/* Uon */		"(_type=video,a=(width=100,height=200,controls=controls),c=@((_type=source,a=(src=foo.mp4,type=video/mp4)),(_type=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en))))",
					/* UonT */		"(t=video,a=(width=100,height=200,controls=controls),c=@((t=source,a=(src=foo.mp4,type=video/mp4)),(t=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en))))",
					/* UonR */		"(\n\t_type=video,\n\ta=(\n\t\twidth=100,\n\t\theight=200,\n\t\tcontrols=controls\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=source,\n\t\t\ta=(\n\t\t\t\tsrc=foo.mp4,\n\t\t\t\ttype=video/mp4\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=track,\n\t\t\ta=(\n\t\t\t\tsrc=subtitles_en.vtt,\n\t\t\t\tkind=subtitles,\n\t\t\t\tsrclang=en\n\t\t\t)\n\t\t)\n\t)\n)",
					/* UrlEnc */	"_type=video&a=(width=100,height=200,controls=controls)&c=@((_type=source,a=(src=foo.mp4,type=video/mp4)),(_type=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en)))",
					/* UrlEncT */	"t=video&a=(width=100,height=200,controls=controls)&c=@((t=source,a=(src=foo.mp4,type=video/mp4)),(t=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en)))",
					/* UrlEncR */	"_type=video\n&a=(\n\twidth=100,\n\theight=200,\n\tcontrols=controls\n)\n&c=@(\n\t(\n\t\t_type=source,\n\t\ta=(\n\t\t\tsrc=foo.mp4,\n\t\t\ttype=video/mp4\n\t\t)\n\t),\n\t(\n\t\t_type=track,\n\t\ta=(\n\t\t\tsrc=subtitles_en.vtt,\n\t\t\tkind=subtitles,\n\t\t\tsrclang=en\n\t\t)\n\t)\n)",
					/* MsgPack */	"83A55F74797065A5766964656FA16183A5776964746864A6686569676874D100C8A8636F6E74726F6C73A8636F6E74726F6C73A1639282A55F74797065A6736F75726365A16182A3737263A7666F6F2E6D7034A474797065A9766964656F2F6D703482A55F74797065A5747261636BA16183A3737263B07375627469746C65735F656E2E767474A46B696E64A97375627469746C6573A77372636C616E67A2656E",
					/* MsgPackT */	"83A174A5766964656FA16183A5776964746864A6686569676874D100C8A8636F6E74726F6C73A8636F6E74726F6C73A1639282A174A6736F75726365A16182A3737263A7666F6F2E6D7034A474797065A9766964656F2F6D703482A174A5747261636BA16183A3737263B07375627469746C65735F656E2E767474A46B696E64A97375627469746C6573A77372636C616E67A2656E",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>video</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>source</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp4</jp:src>\n<jp:type>video/mp4</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>track</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>subtitles_en.vtt</jp:src>\n<jp:kind>subtitles</jp:kind>\n<jp:srclang>en</jp:srclang>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>video</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>source</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp4</jp:src>\n<jp:type>video/mp4</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>track</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>subtitles_en.vtt</jp:src>\n<jp:kind>subtitles</jp:kind>\n<jp:srclang>en</jp:srclang>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>video</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:width>100</jp:width>\n      <jp:height>200</jp:height>\n      <jp:controls>controls</jp:controls>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>source</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.mp4</jp:src>\n            <jp:type>video/mp4</jp:type>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>track</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>subtitles_en.vtt</jp:src>\n            <jp:kind>subtitles</jp:kind>\n            <jp:srclang>en</jp:srclang>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(Video o) {
						assertInstanceOf(Video.class, o);
						assertInstanceOf(Source.class, o.getChild(0));
						assertInstanceOf(Track.class, o.getChild(1));
					}
				}
			},
			{	/* 80 */
				new ComboInput<P>(
					"Wbr",
					P.class,
					p("foo",wbr(),"bar"),
					/* Json */		"{_type:'p',c:['foo',{_type:'wbr'},'bar']}",
					/* JsonT */		"{t:'p',c:['foo',{t:'wbr'},'bar']}",
					/* JsonR */		"{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'wbr'\n\t\t},\n\t\t'bar'\n\t]\n}",
					/* Xml */		"<p>foo<wbr/>bar</p>",
					/* XmlT */		"<p>foo<wbr/>bar</p>",
					/* XmlR */		"<p>foo<wbr/>bar</p>\n",
					/* XmlNs */		"<p>foo<wbr/>bar</p>",
					/* Html */		"<p>foo<wbr/>bar</p>",
					/* HtmlT */		"<p>foo<wbr/>bar</p>",
					/* HtmlR */		"<p>foo<wbr/>bar</p>\n",
					/* Uon */		"(_type=p,c=@(foo,(_type=wbr),bar))",
					/* UonT */		"(t=p,c=@(foo,(t=wbr),bar))",
					/* UonR */		"(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=wbr\n\t\t),\n\t\tbar\n\t)\n)",
					/* UrlEnc */	"_type=p&c=@(foo,(_type=wbr),bar)",
					/* UrlEncT */	"t=p&c=@(foo,(t=wbr),bar)",
					/* UrlEncR */	"_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=wbr\n\t),\n\tbar\n)",
					/* MsgPack */	"82A55F74797065A170A16393A3666F6F81A55F74797065A3776272A3626172",
					/* MsgPackT */	"82A174A170A16393A3666F6F81A174A3776272A3626172",
					/* RdfXml */	"<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>wbr</jp:_type>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlT */	"<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>wbr</jp:t>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n",
					/* RdfXmlR */	"<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>wbr</jp:_type>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n"
				)
				{
					@Override
					public void verify(P o) {
						assertInstanceOf(P.class, o);
						assertInstanceOf(Wbr.class, o.getChild(1));
					}
				}
			},
		});
	}

	public Html5ComboTest(ComboInput<?> comboInput) {
		super(comboInput);
	}

	public static class BeanWithAField {
		public A f1;
		public A[] f2;
		public Collection<A> f3;

		public static BeanWithAField create(A a) {
			BeanWithAField b = new BeanWithAField();
			b.f1 = a;
			b.f2 = new A[]{a,a};
			b.f3 = AList.of(a,a);
			return b;
		}
	}
}
