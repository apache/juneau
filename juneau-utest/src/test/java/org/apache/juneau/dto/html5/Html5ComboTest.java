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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.assertions.Verify.*;

import java.util.*;

import org.apache.juneau.*;
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
					a("http://foo", "bar")
				)
				.json("{_type:'a',a:{href:'http://foo'},c:['bar']}")
				.jsonT("{t:'a',a:{href:'http://foo'},c:['bar']}")
				.jsonR("{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t'bar'\n\t]\n}")
				.xml("<a href='http://foo'>bar</a>")
				.xmlT("<a href='http://foo'>bar</a>")
				.xmlR("<a href='http://foo'>bar</a>\n")
				.xmlNs("<a href='http://foo'>bar</a>")
				.html("<a href='http://foo'>bar</a>")
				.htmlT("<a href='http://foo'>bar</a>")
				.htmlR("<a href='http://foo'>bar</a>\n")
				.uon("(_type=a,a=(href=http://foo),c=@(bar))")
				.uonT("(t=a,a=(href=http://foo),c=@(bar))")
				.uonR("(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar\n\t)\n)")
				.urlEnc("_type=a&a=(href=http://foo)&c=@(bar)")
				.urlEncT("t=a&a=(href=http://foo)&c=@(bar)")
				.urlEncR("_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\tbar\n)")
				.msgPack("83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A3626172")
				.msgPackT("83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 1 */
				new ComboInput<A[]>(
					"A[]",
					A[].class,
					new A[]{a("http://foo", "bar"),a("http://baz", "qux")}
				)
				.json("[{_type:'a',a:{href:'http://foo'},c:['bar']},{_type:'a',a:{href:'http://baz'},c:['qux']}]")
				.jsonT("[{t:'a',a:{href:'http://foo'},c:['bar']},{t:'a',a:{href:'http://baz'},c:['qux']}]")
				.jsonR("[\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://foo'\n\t\t},\n\t\tc: [\n\t\t\t'bar'\n\t\t]\n\t},\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://baz'\n\t\t},\n\t\tc: [\n\t\t\t'qux'\n\t\t]\n\t}\n]")
				.xml("<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>")
				.xmlT("<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>")
				.xmlR("<array>\n\t<a href='http://foo'>bar</a>\n\t<a href='http://baz'>qux</a>\n</array>\n")
				.xmlNs("<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>")
				.html("<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>")
				.htmlT("<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<a href='http://foo'>bar</a>\n\t</li>\n\t<li>\n\t\t<a href='http://baz'>qux</a>\n\t</li>\n</ul>\n")
				.uon("@((_type=a,a=(href=http://foo),c=@(bar)),(_type=a,a=(href=http://baz),c=@(qux)))")
				.uonT("@((t=a,a=(href=http://foo),c=@(bar)),(t=a,a=(href=http://baz),c=@(qux)))")
				.uonR("@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://foo\n\t\t),\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://baz\n\t\t),\n\t\tc=@(\n\t\t\tqux\n\t\t)\n\t)\n)")
				.urlEnc("0=(_type=a,a=(href=http://foo),c=@(bar))&1=(_type=a,a=(href=http://baz),c=@(qux))")
				.urlEncT("0=(t=a,a=(href=http://foo),c=@(bar))&1=(t=a,a=(href=http://baz),c=@(qux))")
				.urlEncR("0=(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar\n\t)\n)\n&1=(\n\t_type=a,\n\ta=(\n\t\thref=http://baz\n\t),\n\tc=@(\n\t\tqux\n\t)\n)")
				.msgPack("9283A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A55F74797065A161A16181A468726566AA687474703A2F2F62617AA16391A3717578")
				.msgPackT("9283A174A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A174A161A16181A468726566AA687474703A2F2F62617AA16391A3717578")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://foo'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>bar</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://baz'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>qux</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A[].class))
				.verify(x -> verify(x.length).is(2))
			},
			{	/* 2 */
				new ComboInput<List<A>>(
					"List<A>",
					getType(List.class, A.class),
					alist(a("http://foo", "bar"),a("http://baz", "qux"))
				)
				.json("[{_type:'a',a:{href:'http://foo'},c:['bar']},{_type:'a',a:{href:'http://baz'},c:['qux']}]")
				.jsonT("[{t:'a',a:{href:'http://foo'},c:['bar']},{t:'a',a:{href:'http://baz'},c:['qux']}]")
				.jsonR("[\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://foo'\n\t\t},\n\t\tc: [\n\t\t\t'bar'\n\t\t]\n\t},\n\t{\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://baz'\n\t\t},\n\t\tc: [\n\t\t\t'qux'\n\t\t]\n\t}\n]")
				.xml("<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>")
				.xmlT("<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>")
				.xmlR("<array>\n\t<a href='http://foo'>bar</a>\n\t<a href='http://baz'>qux</a>\n</array>\n")
				.xmlNs("<array><a href='http://foo'>bar</a><a href='http://baz'>qux</a></array>")
				.html("<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>")
				.htmlT("<ul><li><a href='http://foo'>bar</a></li><li><a href='http://baz'>qux</a></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<a href='http://foo'>bar</a>\n\t</li>\n\t<li>\n\t\t<a href='http://baz'>qux</a>\n\t</li>\n</ul>\n")
				.uon("@((_type=a,a=(href=http://foo),c=@(bar)),(_type=a,a=(href=http://baz),c=@(qux)))")
				.uonT("@((t=a,a=(href=http://foo),c=@(bar)),(t=a,a=(href=http://baz),c=@(qux)))")
				.uonR("@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://foo\n\t\t),\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://baz\n\t\t),\n\t\tc=@(\n\t\t\tqux\n\t\t)\n\t)\n)")
				.urlEnc("0=(_type=a,a=(href=http://foo),c=@(bar))&1=(_type=a,a=(href=http://baz),c=@(qux))")
				.urlEncT("0=(t=a,a=(href=http://foo),c=@(bar))&1=(t=a,a=(href=http://baz),c=@(qux))")
				.urlEncR("0=(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar\n\t)\n)\n&1=(\n\t_type=a,\n\ta=(\n\t\thref=http://baz\n\t),\n\tc=@(\n\t\tqux\n\t)\n)")
				.msgPack("9283A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A55F74797065A161A16181A468726566AA687474703A2F2F62617AA16391A3717578")
				.msgPackT("9283A174A161A16181A468726566AA687474703A2F2F666F6FA16391A362617283A174A161A16181A468726566AA687474703A2F2F62617AA16391A3717578")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://baz'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>qux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://foo'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>bar</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://baz'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>qux</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.size()).is(2))
				.verify(x -> verify(x.get(0)).isType(A.class))
				.verify(x -> verify(x.get(1)).isType(A.class))
			},
			{	/* 3 */
				new ComboInput<A[][]>(
					"A[][]",
					A[][].class,
					new A[][]{{a("http://a", "b"),a("http://c", "d")},{},{a("http://e", "f")}}
				)
				.json("[[{_type:'a',a:{href:'http://a'},c:['b']},{_type:'a',a:{href:'http://c'},c:['d']}],[],[{_type:'a',a:{href:'http://e'},c:['f']}]]")
				.jsonT("[[{t:'a',a:{href:'http://a'},c:['b']},{t:'a',a:{href:'http://c'},c:['d']}],[],[{t:'a',a:{href:'http://e'},c:['f']}]]")
				.jsonR("[\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://a'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'b'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://c'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'd'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://e'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'f'\n\t\t\t]\n\t\t}\n\t]\n]")
				.xml("<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array></array><array><a href='http://e'>f</a></array></array>")
				.xmlT("<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array></array><array><a href='http://e'>f</a></array></array>")
				.xmlR("<array>\n\t<array>\n\t\t<a href='http://a'>b</a>\n\t\t<a href='http://c'>d</a>\n\t</array>\n\t<array>\n\t</array>\n\t<array>\n\t\t<a href='http://e'>f</a>\n\t</array>\n</array>\n")
				.xmlNs("<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array></array><array><a href='http://e'>f</a></array></array>")
				.html("<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>")
				.htmlT("<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://a'>b</a>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<a href='http://c'>d</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul></ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://e'>f</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n")
				.uon("@(@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d))),@(),@((_type=a,a=(href=http://e),c=@(f))))")
				.uonT("@(@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d))),@(),@((t=a,a=(href=http://e),c=@(f))))")
				.uonR("@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://a\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tb\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://c\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\td\n\t\t\t)\n\t\t)\n\t),\n\t@(),\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://e\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tf\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("0=@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d)))&1=@()&2=@((_type=a,a=(href=http://e),c=@(f)))")
				.urlEncT("0=@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d)))&1=@()&2=@((t=a,a=(href=http://e),c=@(f)))")
				.urlEncR("0=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://a\n\t\t),\n\t\tc=@(\n\t\t\tb\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://c\n\t\t),\n\t\tc=@(\n\t\t\td\n\t\t)\n\t)\n)\n&1=@()\n&2=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://e\n\t\t),\n\t\tc=@(\n\t\t\tf\n\t\t)\n\t)\n)")
				.msgPack("939283A55F74797065A161A16181A468726566A8687474703A2F2F61A16391A16283A55F74797065A161A16181A468726566A8687474703A2F2F63A16391A164909183A55F74797065A161A16181A468726566A8687474703A2F2F65A16391A166")
				.msgPackT("939283A174A161A16181A468726566A8687474703A2F2F61A16391A16283A174A161A16181A468726566A8687474703A2F2F63A16391A164909183A174A161A16181A468726566A8687474703A2F2F65A16391A166")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://a'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://c'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>d</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq/>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://e'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>f</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A[][].class))
				.verify(x -> verify(x.length).is(3))
				.verify(x -> verify(x[0].length).is(2))
				.verify(x -> verify(x[1].length).is(0))
				.verify(x -> verify(x[2].length).is(1))
			},
			{	/* 4 */
				new ComboInput<List<List<A>>>(
					"List<List<A>>",
					getType(List.class, List.class, A.class),
					alist(alist(a("http://a", "b"),a("http://c", "d")),alist(a("http://e", "f")))
				)
				.json("[[{_type:'a',a:{href:'http://a'},c:['b']},{_type:'a',a:{href:'http://c'},c:['d']}],[{_type:'a',a:{href:'http://e'},c:['f']}]]")
				.jsonT("[[{t:'a',a:{href:'http://a'},c:['b']},{t:'a',a:{href:'http://c'},c:['d']}],[{t:'a',a:{href:'http://e'},c:['f']}]]")
				.jsonR("[\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://a'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'b'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://c'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'd'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://e'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'f'\n\t\t\t]\n\t\t}\n\t]\n]")
				.xml("<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array><a href='http://e'>f</a></array></array>")
				.xmlT("<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array><a href='http://e'>f</a></array></array>")
				.xmlR("<array>\n\t<array>\n\t\t<a href='http://a'>b</a>\n\t\t<a href='http://c'>d</a>\n\t</array>\n\t<array>\n\t\t<a href='http://e'>f</a>\n\t</array>\n</array>\n")
				.xmlNs("<array><array><a href='http://a'>b</a><a href='http://c'>d</a></array><array><a href='http://e'>f</a></array></array>")
				.html("<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>")
				.htmlT("<ul><li><ul><li><a href='http://a'>b</a></li><li><a href='http://c'>d</a></li></ul></li><li><ul><li><a href='http://e'>f</a></li></ul></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://a'>b</a>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<a href='http://c'>d</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://e'>f</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n")
				.uon("@(@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d))),@((_type=a,a=(href=http://e),c=@(f))))")
				.uonT("@(@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d))),@((t=a,a=(href=http://e),c=@(f))))")
				.uonR("@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://a\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tb\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://c\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\td\n\t\t\t)\n\t\t)\n\t),\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://e\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tf\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("0=@((_type=a,a=(href=http://a),c=@(b)),(_type=a,a=(href=http://c),c=@(d)))&1=@((_type=a,a=(href=http://e),c=@(f)))")
				.urlEncT("0=@((t=a,a=(href=http://a),c=@(b)),(t=a,a=(href=http://c),c=@(d)))&1=@((t=a,a=(href=http://e),c=@(f)))")
				.urlEncR("0=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://a\n\t\t),\n\t\tc=@(\n\t\t\tb\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://c\n\t\t),\n\t\tc=@(\n\t\t\td\n\t\t)\n\t)\n)\n&1=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://e\n\t\t),\n\t\tc=@(\n\t\t\tf\n\t\t)\n\t)\n)")
				.msgPack("929283A55F74797065A161A16181A468726566A8687474703A2F2F61A16391A16283A55F74797065A161A16181A468726566A8687474703A2F2F63A16391A1649183A55F74797065A161A16181A468726566A8687474703A2F2F65A16391A166")
				.msgPackT("929283A174A161A16181A468726566A8687474703A2F2F61A16391A16283A174A161A16181A468726566A8687474703A2F2F63A16391A1649183A174A161A16181A468726566A8687474703A2F2F65A16391A166")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://c'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>d</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://a'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://c'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>d</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://e'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>f</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get(0).get(0)).isType(A.class))
			},
			{	/* 5 */
				new ComboInput<java.util.Map<String,A>>(
					"Map<String,A>",
					getType(java.util.Map.class, String.class, A.class),
					map("a", a("http://b", "c"), "d", a("http://e", "f"))
				)
				.json("{a:{_type:'a',a:{href:'http://b'},c:['c']},d:{_type:'a',a:{href:'http://e'},c:['f']}}")
				.jsonT("{a:{t:'a',a:{href:'http://b'},c:['c']},d:{t:'a',a:{href:'http://e'},c:['f']}}")
				.jsonR("{\n\ta: {\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://b'\n\t\t},\n\t\tc: [\n\t\t\t'c'\n\t\t]\n\t},\n\td: {\n\t\t_type: 'a',\n\t\ta: {\n\t\t\thref: 'http://e'\n\t\t},\n\t\tc: [\n\t\t\t'f'\n\t\t]\n\t}\n}")
				.xml("<object><a href='http://b'>c</a><a _name='d' href='http://e'>f</a></object>")
				.xmlT("<object><a href='http://b'>c</a><a _name='d' href='http://e'>f</a></object>")
				.xmlR("<object>\n\t<a href='http://b'>c</a>\n\t<a _name='d' href='http://e'>f</a>\n</object>\n")
				.xmlNs("<object><a href='http://b'>c</a><a _name='d' href='http://e'>f</a></object>")
				.html("<table><tr><td>a</td><td><a href='http://b'>c</a></td></tr><tr><td>d</td><td><a href='http://e'>f</a></td></tr></table>")
				.htmlT("<table><tr><td>a</td><td><a href='http://b'>c</a></td></tr><tr><td>d</td><td><a href='http://e'>f</a></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>d</td>\n\t\t<td>\n\t\t\t<a href='http://e'>f</a>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(a=(_type=a,a=(href=http://b),c=@(c)),d=(_type=a,a=(href=http://e),c=@(f)))")
				.uonT("(a=(t=a,a=(href=http://b),c=@(c)),d=(t=a,a=(href=http://e),c=@(f)))")
				.uonR("(\n\ta=(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\td=(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://e\n\t\t),\n\t\tc=@(\n\t\t\tf\n\t\t)\n\t)\n)")
				.urlEnc("a=(_type=a,a=(href=http://b),c=@(c))&d=(_type=a,a=(href=http://e),c=@(f))")
				.urlEncT("a=(t=a,a=(href=http://b),c=@(c))&d=(t=a,a=(href=http://e),c=@(f))")
				.urlEncR("a=(\n\t_type=a,\n\ta=(\n\t\thref=http://b\n\t),\n\tc=@(\n\t\tc\n\t)\n)\n&d=(\n\t_type=a,\n\ta=(\n\t\thref=http://e\n\t),\n\tc=@(\n\t\tf\n\t)\n)")
				.msgPack("82A16183A55F74797065A161A16181A468726566A8687474703A2F2F62A16391A163A16483A55F74797065A161A16181A468726566A8687474703A2F2F65A16391A166")
				.msgPackT("82A16183A174A161A16181A468726566A8687474703A2F2F62A16391A163A16483A174A161A16181A468726566A8687474703A2F2F65A16391A166")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:a>\n<jp:d rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:d>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:a>\n<jp:d rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://e'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:d>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://b'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>c</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </jp:a>\n    <jp:d rdf:parseType='Resource'>\n      <jp:_type>a</jp:_type>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://e'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>f</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </jp:d>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("a")).isType(A.class))
				.verify(x -> verify(x.get("d")).isType(A.class))
			},
			{	/* 6 */
				new ComboInput<java.util.Map<String,A[][]>>(
					"Map<String,A[][]>",
					getType(java.util.Map.class, String.class, A[][].class),
					map("a", new A[][]{{a("http://b", "c"),a("http://d", "e")},{}}, "f", new A[][]{{a("http://g", "h")}})
				)
				.json("{a:[[{_type:'a',a:{href:'http://b'},c:['c']},{_type:'a',a:{href:'http://d'},c:['e']}],[]],f:[[{_type:'a',a:{href:'http://g'},c:['h']}]]}")
				.jsonT("{a:[[{t:'a',a:{href:'http://b'},c:['c']},{t:'a',a:{href:'http://d'},c:['e']}],[]],f:[[{t:'a',a:{href:'http://g'},c:['h']}]]}")
				.jsonR("{\n\ta: [\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'a',\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\t_type: 'a',\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://d'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'e'\n\t\t\t\t]\n\t\t\t}\n\t\t],\n\t\t[\n\t\t]\n\t],\n\tf: [\n\t\t[\n\t\t\t{\n\t\t\t\t_type: 'a',\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://g'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'h'\n\t\t\t\t]\n\t\t\t}\n\t\t]\n\t]\n}")
				.xml("<object><a _type='array'><array><a href='http://b'>c</a><a href='http://d'>e</a></array><array></array></a><f _type='array'><array><a href='http://g'>h</a></array></f></object>")
				.xmlT("<object><a t='array'><array><a href='http://b'>c</a><a href='http://d'>e</a></array><array></array></a><f t='array'><array><a href='http://g'>h</a></array></f></object>")
				.xmlR("<object>\n\t<a _type='array'>\n\t\t<array>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://d'>e</a>\n\t\t</array>\n\t\t<array>\n\t\t</array>\n\t</a>\n\t<f _type='array'>\n\t\t<array>\n\t\t\t<a href='http://g'>h</a>\n\t\t</array>\n\t</f>\n</object>\n")
				.xmlNs("<object><a _type='array'><array><a href='http://b'>c</a><a href='http://d'>e</a></array><array></array></a><f _type='array'><array><a href='http://g'>h</a></array></f></object>")
				.html("<table><tr><td>a</td><td><ul><li><ul><li><a href='http://b'>c</a></li><li><a href='http://d'>e</a></li></ul></li><li><ul></ul></li></ul></td></tr><tr><td>f</td><td><ul><li><ul><li><a href='http://g'>h</a></li></ul></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>a</td><td><ul><li><ul><li><a href='http://b'>c</a></li><li><a href='http://d'>e</a></li></ul></li><li><ul></ul></li></ul></td></tr><tr><td>f</td><td><ul><li><ul><li><a href='http://g'>h</a></li></ul></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<a href='http://d'>e</a>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<ul></ul>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>f</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<ul>\n\t\t\t\t\t\t<li>\n\t\t\t\t\t\t\t<a href='http://g'>h</a>\n\t\t\t\t\t\t</li>\n\t\t\t\t\t</ul>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(a=@(@((_type=a,a=(href=http://b),c=@(c)),(_type=a,a=(href=http://d),c=@(e))),@()),f=@(@((_type=a,a=(href=http://g),c=@(h)))))")
				.uonT("(a=@(@((t=a,a=(href=http://b),c=@(c)),(t=a,a=(href=http://d),c=@(e))),@()),f=@(@((t=a,a=(href=http://g),c=@(h)))))")
				.uonR("(\n\ta=@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=a,\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=a,\n\t\t\t\ta=(\n\t\t\t\t\thref=http://d\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\te\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t@()\n\t),\n\tf=@(\n\t\t@(\n\t\t\t(\n\t\t\t\t_type=a,\n\t\t\t\ta=(\n\t\t\t\t\thref=http://g\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\th\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("a=@(@((_type=a,a=(href=http://b),c=@(c)),(_type=a,a=(href=http://d),c=@(e))),@())&f=@(@((_type=a,a=(href=http://g),c=@(h))))")
				.urlEncT("a=@(@((t=a,a=(href=http://b),c=@(c)),(t=a,a=(href=http://d),c=@(e))),@())&f=@(@((t=a,a=(href=http://g),c=@(h))))")
				.urlEncR("a=@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://d\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\te\n\t\t\t)\n\t\t)\n\t),\n\t@()\n)\n&f=@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://g\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\th\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("82A161929283A55F74797065A161A16181A468726566A8687474703A2F2F62A16391A16383A55F74797065A161A16181A468726566A8687474703A2F2F64A16391A16590A166919183A55F74797065A161A16181A468726566A8687474703A2F2F67A16391A168")
				.msgPackT("82A161929283A174A161A16181A468726566A8687474703A2F2F62A16391A16383A174A161A16181A468726566A8687474703A2F2F64A16391A16590A166919183A174A161A16181A468726566A8687474703A2F2F67A16391A168")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:a>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n</rdf:Seq>\n</jp:a>\n<jp:f>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:a>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n</rdf:Seq>\n</jp:a>\n<jp:f>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</jp:f>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:a>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>a</jp:_type>\n              <jp:a rdf:parseType='Resource'>\n                <jp:href rdf:resource='http://b'/>\n              </jp:a>\n              <jp:c>\n                <rdf:Seq>\n                  <rdf:li>c</rdf:li>\n                </rdf:Seq>\n              </jp:c>\n            </rdf:li>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>a</jp:_type>\n              <jp:a rdf:parseType='Resource'>\n                <jp:href rdf:resource='http://d'/>\n              </jp:a>\n              <jp:c>\n                <rdf:Seq>\n                  <rdf:li>e</rdf:li>\n                </rdf:Seq>\n              </jp:c>\n            </rdf:li>\n          </rdf:Seq>\n        </rdf:li>\n        <rdf:li>\n          <rdf:Seq/>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:a>\n    <jp:f>\n      <rdf:Seq>\n        <rdf:li>\n          <rdf:Seq>\n            <rdf:li rdf:parseType='Resource'>\n              <jp:_type>a</jp:_type>\n              <jp:a rdf:parseType='Resource'>\n                <jp:href rdf:resource='http://g'/>\n              </jp:a>\n              <jp:c>\n                <rdf:Seq>\n                  <rdf:li>h</rdf:li>\n                </rdf:Seq>\n              </jp:c>\n            </rdf:li>\n          </rdf:Seq>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get("a")[0][0]).isType(A.class))
				.verify(x -> verify(x.get("f")[0][0]).isType(A.class))
			},
			{	/* 7 */
				new ComboInput<BeanWithAField>(
					"BeanWithAField",
					BeanWithAField.class,
					BeanWithAField.create(a("http://b", "c"))
				)
				.json("{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}")
				.jsonT("{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}")
				.jsonR("{\n\tf1: {\n\t\ta: {\n\t\t\thref: 'http://b'\n\t\t},\n\t\tc: [\n\t\t\t'c'\n\t\t]\n\t},\n\tf2: [\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t}\n\t],\n\tf3: [\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object>")
				.xmlT("<object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object>")
				.xmlR("<object>\n\t<a _name='f1' href='http://b'>c</a>\n\t<f2>\n\t\t<a href='http://b'>c</a>\n\t\t<a href='http://b'>c</a>\n\t</f2>\n\t<f3>\n\t\t<a href='http://b'>c</a>\n\t\t<a href='http://b'>c</a>\n\t</f3>\n</object>\n")
				.xmlNs("<object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object>")
				.html("<table><tr><td>f1</td><td><a href='http://b'>c</a></td></tr><tr><td>f2</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr><tr><td>f3</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>")
				.htmlT("<table><tr><td>f1</td><td><a href='http://b'>c</a></td></tr><tr><td>f2</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr><tr><td>f3</td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>")
				.htmlR("<table>\n\t<tr>\n\t\t<td>f1</td>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>f2</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n\t<tr>\n\t\t<td>f3</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))")
				.uonT("(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))")
				.uonR("(\n\tf1=(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\tf2=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t),\n\tf3=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("f1=(a=(href=http://b),c=@(c))&f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))&f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))")
				.urlEncT("f1=(a=(href=http://b),c=@(c))&f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))&f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))")
				.urlEncR("f1=(\n\ta=(\n\t\thref=http://b\n\t),\n\tc=@(\n\t\tc\n\t)\n)\n&f2=@(\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t)\n)\n&f3=@(\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\t(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t)\n)")
				.msgPack("83A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163")
				.msgPackT("83A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:f1 rdf:parseType='Resource'>\n      <jp:a rdf:parseType='Resource'>\n        <jp:href rdf:resource='http://b'/>\n      </jp:a>\n      <jp:c>\n        <rdf:Seq>\n          <rdf:li>c</rdf:li>\n        </rdf:Seq>\n      </jp:c>\n    </jp:f1>\n    <jp:f2>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f2>\n    <jp:f3>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://b'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:f3>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithAField.class))
			},
			{	/* 8 */
				new ComboInput<BeanWithAField[]>(
					"BeanWithAField[]",
					BeanWithAField[].class,
					new BeanWithAField[]{BeanWithAField.create(a("http://b", "c"))}
				)
				.json("[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]")
				.jsonT("[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]")
				.jsonR("[\n\t{\n\t\tf1: {\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\tf2: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t],\n\t\tf3: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t]\n\t}\n]")
				.xml("<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>")
				.xmlT("<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>")
				.xmlR("<array>\n\t<object>\n\t\t<a _name='f1' href='http://b'>c</a>\n\t\t<f2>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f2>\n\t\t<f3>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f3>\n\t</object>\n</array>\n")
				.xmlNs("<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>")
				.html("<table _type='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>")
				.htmlT("<table t='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>f1</th>\n\t\t<th>f2</th>\n\t\t<th>f3</th>\n\t</tr>\n\t<tr>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))")
				.uonT("@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))")
				.uonR("@(\n\t(\n\t\tf1=(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\tf2=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\tf3=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))")
				.urlEncT("0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))")
				.urlEncR("0=(\n\tf1=(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\tf2=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t),\n\tf3=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163")
				.msgPackT("9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:f1 rdf:parseType='Resource'>\n        <jp:a rdf:parseType='Resource'>\n          <jp:href rdf:resource='http://b'/>\n        </jp:a>\n        <jp:c>\n          <rdf:Seq>\n            <rdf:li>c</rdf:li>\n          </rdf:Seq>\n        </jp:c>\n      </jp:f1>\n      <jp:f2>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f2>\n      <jp:f3>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f3>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(BeanWithAField[].class))
			},
			{	/* 9 */
				new ComboInput<List<BeanWithAField>>(
					"List<BeanWithAField>",
					getType(List.class, BeanWithAField.class),
					alist(BeanWithAField.create(a("http://b", "c")))
				)
				.json("[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]")
				.jsonT("[{f1:{a:{href:'http://b'},c:['c']},f2:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}],f3:[{a:{href:'http://b'},c:['c']},{a:{href:'http://b'},c:['c']}]}]")
				.jsonR("[\n\t{\n\t\tf1: {\n\t\t\ta: {\n\t\t\t\thref: 'http://b'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\tf2: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t],\n\t\tf3: [\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t},\n\t\t\t{\n\t\t\t\ta: {\n\t\t\t\t\thref: 'http://b'\n\t\t\t\t},\n\t\t\t\tc: [\n\t\t\t\t\t'c'\n\t\t\t\t]\n\t\t\t}\n\t\t]\n\t}\n]")
				.xml("<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>")
				.xmlT("<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>")
				.xmlR("<array>\n\t<object>\n\t\t<a _name='f1' href='http://b'>c</a>\n\t\t<f2>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f2>\n\t\t<f3>\n\t\t\t<a href='http://b'>c</a>\n\t\t\t<a href='http://b'>c</a>\n\t\t</f3>\n\t</object>\n</array>\n")
				.xmlNs("<array><object><a _name='f1' href='http://b'>c</a><f2><a href='http://b'>c</a><a href='http://b'>c</a></f2><f3><a href='http://b'>c</a><a href='http://b'>c</a></f3></object></array>")
				.html("<table _type='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>")
				.htmlT("<table t='array'><tr><th>f1</th><th>f2</th><th>f3</th></tr><tr><td><a href='http://b'>c</a></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td><td><ul><li><a href='http://b'>c</a></li><li><a href='http://b'>c</a></li></ul></td></tr></table>")
				.htmlR("<table _type='array'>\n\t<tr>\n\t\t<th>f1</th>\n\t\t<th>f2</th>\n\t\t<th>f3</th>\n\t</tr>\n\t<tr>\n\t\t<td>\n\t\t\t<a href='http://b'>c</a>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t\t<td>\n\t\t\t<ul>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t\t<li>\n\t\t\t\t\t<a href='http://b'>c</a>\n\t\t\t\t</li>\n\t\t\t</ul>\n\t\t</td>\n\t</tr>\n</table>\n")
				.uon("@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))")
				.uonT("@((f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c)))))")
				.uonR("@(\n\t(\n\t\tf1=(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\tf2=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\tf3=@(\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\ta=(\n\t\t\t\t\thref=http://b\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tc\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))")
				.urlEncT("0=(f1=(a=(href=http://b),c=@(c)),f2=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))),f3=@((a=(href=http://b),c=@(c)),(a=(href=http://b),c=@(c))))")
				.urlEncR("0=(\n\tf1=(\n\t\ta=(\n\t\t\thref=http://b\n\t\t),\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t),\n\tf2=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t),\n\tf3=@(\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\ta=(\n\t\t\t\thref=http://b\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163")
				.msgPackT("9183A2663182A16181A468726566A8687474703A2F2F62A16391A163A266329282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163A266339282A16181A468726566A8687474703A2F2F62A16391A16382A16181A468726566A8687474703A2F2F62A16391A163")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:f1 rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</jp:f1>\n<jp:f2>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f2>\n<jp:f3>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://b'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:f3>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li rdf:parseType='Resource'>\n      <jp:f1 rdf:parseType='Resource'>\n        <jp:a rdf:parseType='Resource'>\n          <jp:href rdf:resource='http://b'/>\n        </jp:a>\n        <jp:c>\n          <rdf:Seq>\n            <rdf:li>c</rdf:li>\n          </rdf:Seq>\n        </jp:c>\n      </jp:f1>\n      <jp:f2>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f2>\n      <jp:f3>\n        <rdf:Seq>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n          <rdf:li rdf:parseType='Resource'>\n            <jp:a rdf:parseType='Resource'>\n              <jp:href rdf:resource='http://b'/>\n            </jp:a>\n            <jp:c>\n              <rdf:Seq>\n                <rdf:li>c</rdf:li>\n              </rdf:Seq>\n            </jp:c>\n          </rdf:li>\n        </rdf:Seq>\n      </jp:f3>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x.get(0)).isType(BeanWithAField.class))
			},
			{	/* 10 */
				new ComboInput<A>(
					"A-2",
					A.class,
					a("http://foo", "bar", btag, "baz")
				)
				.json("{_type:'a',a:{href:'http://foo'},c:['bar',{_type:'b',c:['bbb']},'baz']}")
				.jsonT("{t:'a',a:{href:'http://foo'},c:['bar',{t:'b',c:['bbb']},'baz']}")
				.jsonR("{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t'bar',\n\t\t{\n\t\t\t_type: 'b',\n\t\t\tc: [\n\t\t\t\t'bbb'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}")
				.xml("<a href='http://foo'>bar<b>bbb</b>baz</a>")
				.xmlT("<a href='http://foo'>bar<b>bbb</b>baz</a>")
				.xmlR("<a href='http://foo'>bar<b>bbb</b>baz</a>\n")
				.xmlNs("<a href='http://foo'>bar<b>bbb</b>baz</a>")
				.html("<a href='http://foo'>bar<b>bbb</b>baz</a>")
				.htmlT("<a href='http://foo'>bar<b>bbb</b>baz</a>")
				.htmlR("<a href='http://foo'>bar<b>bbb</b>baz</a>\n")
				.uon("(_type=a,a=(href=http://foo),c=@(bar,(_type=b,c=@(bbb)),baz))")
				.uonT("(t=a,a=(href=http://foo),c=@(bar,(t=b,c=@(bbb)),baz))")
				.uonR("(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\tbar,\n\t\t(\n\t\t\t_type=b,\n\t\t\tc=@(\n\t\t\t\tbbb\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)")
				.urlEnc("_type=a&a=(href=http://foo)&c=@(bar,(_type=b,c=@(bbb)),baz)")
				.urlEncT("t=a&a=(href=http://foo)&c=@(bar,(t=b,c=@(bbb)),baz)")
				.urlEncR("_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\tbar,\n\t(\n\t\t_type=b,\n\t\tc=@(\n\t\t\tbbb\n\t\t)\n\t),\n\tbaz\n)")
				.msgPack("83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16393A362617282A55F74797065A162A16391A3626262A362617A")
				.msgPackT("83A174A161A16181A468726566AA687474703A2F2F666F6FA16393A362617282A174A162A16391A3626262A362617A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>bar</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>b</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bbb</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 11 */
				new ComboInput<A[][]>(
					"A[][]-2",
					A[][].class,
					new A[][]{{a("http://a", "b", btag, "c"),a("http://d", "e", btag, "f")},{},{a("http://g", "h", btag, "i")}}
				)
				.json("[[{_type:'a',a:{href:'http://a'},c:['b',{_type:'b',c:['bbb']},'c']},{_type:'a',a:{href:'http://d'},c:['e',{_type:'b',c:['bbb']},'f']}],[],[{_type:'a',a:{href:'http://g'},c:['h',{_type:'b',c:['bbb']},'i']}]]")
				.jsonT("[[{t:'a',a:{href:'http://a'},c:['b',{t:'b',c:['bbb']},'c']},{t:'a',a:{href:'http://d'},c:['e',{t:'b',c:['bbb']},'f']}],[],[{t:'a',a:{href:'http://g'},c:['h',{t:'b',c:['bbb']},'i']}]]")
				.jsonR("[\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://a'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'b',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'c'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://d'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'e',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'f'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'http://g'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'h',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'i'\n\t\t\t]\n\t\t}\n\t]\n]")
				.xml("<array><array><a href='http://a'>b<b>bbb</b>c</a><a href='http://d'>e<b>bbb</b>f</a></array><array></array><array><a href='http://g'>h<b>bbb</b>i</a></array></array>")
				.xmlT("<array><array><a href='http://a'>b<b>bbb</b>c</a><a href='http://d'>e<b>bbb</b>f</a></array><array></array><array><a href='http://g'>h<b>bbb</b>i</a></array></array>")
				.xmlR("<array>\n\t<array>\n\t\t<a href='http://a'>b<b>bbb</b>c</a>\n\t\t<a href='http://d'>e<b>bbb</b>f</a>\n\t</array>\n\t<array>\n\t</array>\n\t<array>\n\t\t<a href='http://g'>h<b>bbb</b>i</a>\n\t</array>\n</array>\n")
				.xmlNs("<array><array><a href='http://a'>b<b>bbb</b>c</a><a href='http://d'>e<b>bbb</b>f</a></array><array></array><array><a href='http://g'>h<b>bbb</b>i</a></array></array>")
				.html("<ul><li><ul><li><a href='http://a'>b<b>bbb</b>c</a></li><li><a href='http://d'>e<b>bbb</b>f</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://g'>h<b>bbb</b>i</a></li></ul></li></ul>")
				.htmlT("<ul><li><ul><li><a href='http://a'>b<b>bbb</b>c</a></li><li><a href='http://d'>e<b>bbb</b>f</a></li></ul></li><li><ul></ul></li><li><ul><li><a href='http://g'>h<b>bbb</b>i</a></li></ul></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://a'>b<b>bbb</b>c</a>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<a href='http://d'>e<b>bbb</b>f</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul></ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<a href='http://g'>h<b>bbb</b>i</a>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n")
				.uon("@(@((_type=a,a=(href=http://a),c=@(b,(_type=b,c=@(bbb)),c)),(_type=a,a=(href=http://d),c=@(e,(_type=b,c=@(bbb)),f))),@(),@((_type=a,a=(href=http://g),c=@(h,(_type=b,c=@(bbb)),i))))")
				.uonT("@(@((t=a,a=(href=http://a),c=@(b,(t=b,c=@(bbb)),c)),(t=a,a=(href=http://d),c=@(e,(t=b,c=@(bbb)),f))),@(),@((t=a,a=(href=http://g),c=@(h,(t=b,c=@(bbb)),i))))")
				.uonR("@(\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://a\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tb,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tc\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://d\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\te,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tf\n\t\t\t)\n\t\t)\n\t),\n\t@(),\n\t@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=http://g\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\th,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\ti\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("0=@((_type=a,a=(href=http://a),c=@(b,(_type=b,c=@(bbb)),c)),(_type=a,a=(href=http://d),c=@(e,(_type=b,c=@(bbb)),f)))&1=@()&2=@((_type=a,a=(href=http://g),c=@(h,(_type=b,c=@(bbb)),i)))")
				.urlEncT("0=@((t=a,a=(href=http://a),c=@(b,(t=b,c=@(bbb)),c)),(t=a,a=(href=http://d),c=@(e,(t=b,c=@(bbb)),f)))&1=@()&2=@((t=a,a=(href=http://g),c=@(h,(t=b,c=@(bbb)),i)))")
				.urlEncR("0=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://a\n\t\t),\n\t\tc=@(\n\t\t\tb,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\tc\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://d\n\t\t),\n\t\tc=@(\n\t\t\te,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\tf\n\t\t)\n\t)\n)\n&1=@()\n&2=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=http://g\n\t\t),\n\t\tc=@(\n\t\t\th,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\ti\n\t\t)\n\t)\n)")
				.msgPack("939283A55F74797065A161A16181A468726566A8687474703A2F2F61A16393A16282A55F74797065A162A16391A3626262A16383A55F74797065A161A16181A468726566A8687474703A2F2F64A16393A16582A55F74797065A162A16391A3626262A166909183A55F74797065A161A16181A468726566A8687474703A2F2F67A16393A16882A55F74797065A162A16391A3626262A169")
				.msgPackT("939283A174A161A16181A468726566A8687474703A2F2F61A16393A16282A174A162A16391A3626262A16383A174A161A16181A468726566A8687474703A2F2F64A16393A16582A174A162A16391A3626262A166909183A174A161A16181A468726566A8687474703A2F2F67A16393A16882A174A162A16391A3626262A169")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>i</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://a'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://d'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>e</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>f</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://g'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>h</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>i</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://a'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://d'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>e</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>f</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq/>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href rdf:resource='http://g'/>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>h</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>i</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A[][].class))
			},
			{	/* 12 */
				new ComboInput<A>(
					"A-3",
					A.class,
					a("http://foo", "")
				)
				.json("{_type:'a',a:{href:'http://foo'},c:['']}")
				.jsonT("{t:'a',a:{href:'http://foo'},c:['']}")
				.jsonR("{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t''\n\t]\n}")
				.xml("<a href='http://foo'>_xE000_</a>")
				.xmlT("<a href='http://foo'>_xE000_</a>")
				.xmlR("<a href='http://foo'>_xE000_</a>\n")
				.xmlNs("<a href='http://foo'>_xE000_</a>")
				.html("<a href='http://foo'><sp/></a>")
				.htmlT("<a href='http://foo'><sp/></a>")
				.htmlR("<a href='http://foo'><sp/></a>\n")
				.uon("(_type=a,a=(href=http://foo),c=@(''))")
				.uonT("(t=a,a=(href=http://foo),c=@(''))")
				.uonR("(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\t''\n\t)\n)")
				.urlEnc("_type=a&a=(href=http://foo)&c=@('')")
				.urlEncT("t=a&a=(href=http://foo)&c=@('')")
				.urlEncR("_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\t''\n)")
				.msgPack("83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A0")
				.msgPackT("83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li></rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 13 */
				new ComboInput<A>(
					"A-4",
					A.class,
					a("http://foo", " ")
				)
				.json("{_type:'a',a:{href:'http://foo'},c:[' ']}")
				.jsonT("{t:'a',a:{href:'http://foo'},c:[' ']}")
				.jsonR("{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t},\n\tc: [\n\t\t' '\n\t]\n}")
				.xml("<a href='http://foo'>_x0020_</a>")
				.xmlT("<a href='http://foo'>_x0020_</a>")
				.xmlR("<a href='http://foo'>_x0020_</a>\n")
				.xmlNs("<a href='http://foo'>_x0020_</a>")
				.html("<a href='http://foo'><sp> </sp></a>")
				.htmlT("<a href='http://foo'><sp> </sp></a>")
				.htmlR("<a href='http://foo'><sp> </sp></a>\n")
				.uon("(_type=a,a=(href=http://foo),c=@(' '))")
				.uonT("(t=a,a=(href=http://foo),c=@(' '))")
				.uonR("(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t),\n\tc=@(\n\t\t' '\n\t)\n)")
				.urlEnc("_type=a&a=(href=http://foo)&c=@('+')")
				.urlEncT("t=a&a=(href=http://foo)&c=@('+')")
				.urlEncR("_type=a\n&a=(\n\thref=http://foo\n)\n&c=@(\n\t'+'\n)")
				.msgPack("83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A120")
				.msgPackT("83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A120")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>_x0020_</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>_x0020_</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>_x0020_</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 14 */
				new ComboInput<A>(
					"A-5",
					A.class,
					a("http://foo")
				)
				.json("{_type:'a',a:{href:'http://foo'}}")
				.jsonT("{t:'a',a:{href:'http://foo'}}")
				.jsonR("{\n\t_type: 'a',\n\ta: {\n\t\thref: 'http://foo'\n\t}\n}")
				.xml("<a href='http://foo' nil='true'></a>")
				.xmlT("<a href='http://foo' nil='true'></a>")
				.xmlR("<a href='http://foo' nil='true'>\n</a>\n")
				.xmlNs("<a href='http://foo' nil='true'></a>")
				.html("<a href='http://foo' nil='true'></a>")
				.htmlT("<a href='http://foo' nil='true'></a>")
				.htmlR("<a href='http://foo' nil='true'>\n</a>\n")
				.uon("(_type=a,a=(href=http://foo))")
				.uonT("(t=a,a=(href=http://foo))")
				.uonR("(\n\t_type=a,\n\ta=(\n\t\thref=http://foo\n\t)\n)")
				.urlEnc("_type=a&a=(href=http://foo)")
				.urlEncT("t=a&a=(href=http://foo)")
				.urlEncR("_type=a\n&a=(\n\thref=http://foo\n)")
				.msgPack("82A55F74797065A161A16181A468726566AA687474703A2F2F666F6F")
				.msgPackT("82A174A161A16181A468726566AA687474703A2F2F666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href rdf:resource='http://foo'/>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>a</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:href rdf:resource='http://foo'/>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(A.class))
			},
			{	/* 15 */
				new ComboInput<Abbr>(
					"Abbr-1",
					Abbr.class,
					abbr().children("foo")
				)
				.json("{_type:'abbr',c:['foo']}")
				.jsonT("{t:'abbr',c:['foo']}")
				.jsonR("{\n\t_type: 'abbr',\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<abbr>foo</abbr>")
				.xmlT("<abbr>foo</abbr>")
				.xmlR("<abbr>foo</abbr>\n")
				.xmlNs("<abbr>foo</abbr>")
				.html("<abbr>foo</abbr>")
				.htmlT("<abbr>foo</abbr>")
				.htmlR("<abbr>foo</abbr>\n")
				.uon("(_type=abbr,c=@(foo))")
				.uonT("(t=abbr,c=@(foo))")
				.uonR("(\n\t_type=abbr,\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=abbr&c=@(foo)")
				.urlEncT("t=abbr&c=@(foo)")
				.urlEncR("_type=abbr\n&c=@(\n\tfoo\n)")
				.msgPack("82A55F74797065A461626272A16391A3666F6F")
				.msgPackT("82A174A461626272A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>abbr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>abbr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>abbr</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Abbr.class))
			},
			{	/* 16 */
				new ComboInput<Abbr>(
					"Abbr-2",
					Abbr.class,
					abbr("foo", "bar", btag, "baz")
				)
				.json("{_type:'abbr',a:{title:'foo'},c:['bar',{_type:'b',c:['bbb']},'baz']}")
				.jsonT("{t:'abbr',a:{title:'foo'},c:['bar',{t:'b',c:['bbb']},'baz']}")
				.jsonR("{\n\t_type: 'abbr',\n\ta: {\n\t\ttitle: 'foo'\n\t},\n\tc: [\n\t\t'bar',\n\t\t{\n\t\t\t_type: 'b',\n\t\t\tc: [\n\t\t\t\t'bbb'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}")
				.xml("<abbr title='foo'>bar<b>bbb</b>baz</abbr>")
				.xmlT("<abbr title='foo'>bar<b>bbb</b>baz</abbr>")
				.xmlR("<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n")
				.xmlNs("<abbr title='foo'>bar<b>bbb</b>baz</abbr>")
				.html("<abbr title='foo'>bar<b>bbb</b>baz</abbr>")
				.htmlT("<abbr title='foo'>bar<b>bbb</b>baz</abbr>")
				.htmlR("<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n")
				.uon("(_type=abbr,a=(title=foo),c=@(bar,(_type=b,c=@(bbb)),baz))")
				.uonT("(t=abbr,a=(title=foo),c=@(bar,(t=b,c=@(bbb)),baz))")
				.uonR("(\n\t_type=abbr,\n\ta=(\n\t\ttitle=foo\n\t),\n\tc=@(\n\t\tbar,\n\t\t(\n\t\t\t_type=b,\n\t\t\tc=@(\n\t\t\t\tbbb\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)")
				.urlEnc("_type=abbr&a=(title=foo)&c=@(bar,(_type=b,c=@(bbb)),baz)")
				.urlEncT("t=abbr&a=(title=foo)&c=@(bar,(t=b,c=@(bbb)),baz)")
				.urlEncR("_type=abbr\n&a=(\n\ttitle=foo\n)\n&c=@(\n\tbar,\n\t(\n\t\t_type=b,\n\t\tc=@(\n\t\t\tbbb\n\t\t)\n\t),\n\tbaz\n)")
				.msgPack("83A55F74797065A461626272A16181A57469746C65A3666F6FA16393A362617282A55F74797065A162A16391A3626262A362617A")
				.msgPackT("83A174A461626272A16181A57469746C65A3666F6FA16393A362617282A174A162A16391A3626262A362617A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>abbr</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:title>foo</jp:title>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>abbr</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:title>foo</jp:title>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>abbr</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:title>foo</jp:title>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>bar</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>b</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bbb</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Abbr.class))
			},
			{	/* 17 */
				new ComboInput<Address>(
					"Address-1",
					Address.class,
					address()
				)
				.json("{_type:'address'}")
				.jsonT("{t:'address'}")
				.jsonR("{\n\t_type: 'address'\n}")
				.xml("<address nil='true'></address>")
				.xmlT("<address nil='true'></address>")
				.xmlR("<address nil='true'>\n</address>\n")
				.xmlNs("<address nil='true'></address>")
				.html("<address nil='true'></address>")
				.htmlT("<address nil='true'></address>")
				.htmlR("<address nil='true'>\n</address>\n")
				.uon("(_type=address)")
				.uonT("(t=address)")
				.uonR("(\n\t_type=address\n)")
				.urlEnc("_type=address")
				.urlEncT("t=address")
				.urlEncR("_type=address")
				.msgPack("81A55F74797065A761646472657373")
				.msgPackT("81A174A761646472657373")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>address</jp:_type>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>address</jp:t>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>address</jp:_type>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Address.class))
			},
			{	/* 18 */
				new ComboInput<Address>(
					"Address-2",
					Address.class,
					address("")
				)
				.json("{_type:'address',c:['']}")
				.jsonT("{t:'address',c:['']}")
				.jsonR("{\n\t_type: 'address',\n\tc: [\n\t\t''\n\t]\n}")
				.xml("<address>_xE000_</address>")
				.xmlT("<address>_xE000_</address>")
				.xmlR("<address>_xE000_</address>\n")
				.xmlNs("<address>_xE000_</address>")
				.html("<address><sp/></address>")
				.htmlT("<address><sp/></address>")
				.htmlR("<address><sp/></address>\n")
				.uon("(_type=address,c=@(''))")
				.uonT("(t=address,c=@(''))")
				.uonR("(\n\t_type=address,\n\tc=@(\n\t\t''\n\t)\n)")
				.urlEnc("_type=address&c=@('')")
				.urlEncT("t=address&c=@('')")
				.urlEncR("_type=address\n&c=@(\n\t''\n)")
				.msgPack("82A55F74797065A761646472657373A16391A0")
				.msgPackT("82A174A761646472657373A16391A0")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>address</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>address</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li></rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>address</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li></rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Address.class))
			},
			{	/* 19 */
				new ComboInput<Address>(
					"Address-3",
					Address.class,
					address("foo", a("bar", "baz"), a("qux", "quux"))
				)
				.json("{_type:'address',c:['foo',{_type:'a',a:{href:'bar'},c:['baz']},{_type:'a',a:{href:'qux'},c:['quux']}]}")
				.jsonT("{t:'address',c:['foo',{t:'a',a:{href:'bar'},c:['baz']},{t:'a',a:{href:'qux'},c:['quux']}]}")
				.jsonR("{\n\t_type: 'address',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'bar'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'baz'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'qux'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'quux'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>")
				.xmlT("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>")
				.xmlR("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n")
				.xmlNs("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>")
				.html("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>")
				.htmlT("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>")
				.htmlR("<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n")
				.uon("(_type=address,c=@(foo,(_type=a,a=(href=bar),c=@(baz)),(_type=a,a=(href=qux),c=@(quux))))")
				.uonT("(t=address,c=@(foo,(t=a,a=(href=bar),c=@(baz)),(t=a,a=(href=qux),c=@(quux))))")
				.uonR("(\n\t_type=address,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=bar\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tbaz\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=qux\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tquux\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=address&c=@(foo,(_type=a,a=(href=bar),c=@(baz)),(_type=a,a=(href=qux),c=@(quux)))")
				.urlEncT("t=address&c=@(foo,(t=a,a=(href=bar),c=@(baz)),(t=a,a=(href=qux),c=@(quux)))")
				.urlEncR("_type=address\n&c=@(\n\tfoo,\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=bar\n\t\t),\n\t\tc=@(\n\t\t\tbaz\n\t\t)\n\t),\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=qux\n\t\t),\n\t\tc=@(\n\t\t\tquux\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A761646472657373A16393A3666F6F83A55F74797065A161A16181A468726566A3626172A16391A362617A83A55F74797065A161A16181A468726566A3717578A16391A471757578")
				.msgPackT("82A174A761646472657373A16393A3666F6F83A174A161A16181A468726566A3626172A16391A362617A83A174A161A16181A468726566A3717578A16391A471757578")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>address</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>bar</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>qux</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>quux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>address</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>bar</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>qux</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>quux</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>address</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href>bar</jp:href>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>baz</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href>qux</jp:href>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>quux</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Address.class))
				.verify(x -> verify(x.getChild(1)).isType(A.class))
				.verify(x -> verify(x.getChild(2)).isType(A.class))
			},
			{	/* 20 */
				new ComboInput<Aside>(
					"Aside-1",
					Aside.class,
					aside(
						h1("header1"),p("foo")
					)
				)
				.json("{_type:'aside',c:[{_type:'h1',c:['header1']},{_type:'p',c:['foo']}]}")
				.jsonT("{t:'aside',c:[{t:'h1',c:['header1']},{t:'p',c:['foo']}]}")
				.jsonR("{\n\t_type: 'aside',\n\tc: [\n\t\t{\n\t\t\t_type: 'h1',\n\t\t\tc: [\n\t\t\t\t'header1'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<aside><h1>header1</h1><p>foo</p></aside>")
				.xmlT("<aside><h1>header1</h1><p>foo</p></aside>")
				.xmlR("<aside><h1>header1</h1><p>foo</p></aside>\n")
				.xmlNs("<aside><h1>header1</h1><p>foo</p></aside>")
				.html("<aside><h1>header1</h1><p>foo</p></aside>")
				.htmlT("<aside><h1>header1</h1><p>foo</p></aside>")
				.htmlR("<aside><h1>header1</h1><p>foo</p></aside>\n")
				.uon("(_type=aside,c=@((_type=h1,c=@(header1)),(_type=p,c=@(foo))))")
				.uonT("(t=aside,c=@((t=h1,c=@(header1)),(t=p,c=@(foo))))")
				.uonR("(\n\t_type=aside,\n\tc=@(\n\t\t(\n\t\t\t_type=h1,\n\t\t\tc=@(\n\t\t\t\theader1\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=aside&c=@((_type=h1,c=@(header1)),(_type=p,c=@(foo)))")
				.urlEncT("t=aside&c=@((t=h1,c=@(header1)),(t=p,c=@(foo)))")
				.urlEncR("_type=aside\n&c=@(\n\t(\n\t\t_type=h1,\n\t\tc=@(\n\t\t\theader1\n\t\t)\n\t),\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A56173696465A1639282A55F74797065A26831A16391A76865616465723182A55F74797065A170A16391A3666F6F")
				.msgPackT("82A174A56173696465A1639282A174A26831A16391A76865616465723182A174A170A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>aside</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>aside</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>aside</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h1</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>header1</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Aside.class))
				.verify(x -> verify(x.getChild(0)).isType(H1.class))
				.verify(x -> verify(x.getChild(1)).isType(P.class))
			},
			{	/* 21 */
				new ComboInput<Audio>(
					"Audio/Source-1",
					Audio.class,
					audio().controls(true).children(
						source("foo.ogg", "audio/ogg"),
						source("foo.mp3", "audio/mpeg")
					)
				)
				.json("{_type:'audio',a:{controls:'controls'},c:[{_type:'source',a:{src:'foo.ogg',type:'audio/ogg'}},{_type:'source',a:{src:'foo.mp3',type:'audio/mpeg'}}]}")
				.jsonT("{t:'audio',a:{controls:'controls'},c:[{t:'source',a:{src:'foo.ogg',type:'audio/ogg'}},{t:'source',a:{src:'foo.mp3',type:'audio/mpeg'}}]}")
				.jsonR("{\n\t_type: 'audio',\n\ta: {\n\t\tcontrols: 'controls'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'source',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.ogg',\n\t\t\t\ttype: 'audio/ogg'\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'source',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.mp3',\n\t\t\t\ttype: 'audio/mpeg'\n\t\t\t}\n\t\t}\n\t]\n}")
				.xml("<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>")
				.xmlT("<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>")
				.xmlR("<audio controls='controls'>\n\t<source src='foo.ogg' type='audio/ogg'/>\n\t<source src='foo.mp3' type='audio/mpeg'/>\n</audio>\n")
				.xmlNs("<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>")
				.html("<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>")
				.htmlT("<audio controls='controls'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>")
				.htmlR("<audio controls='controls'>\n\t<source src='foo.ogg' type='audio/ogg'/>\n\t<source src='foo.mp3' type='audio/mpeg'/>\n</audio>\n")
				.uon("(_type=audio,a=(controls=controls),c=@((_type=source,a=(src=foo.ogg,type=audio/ogg)),(_type=source,a=(src=foo.mp3,type=audio/mpeg))))")
				.uonT("(t=audio,a=(controls=controls),c=@((t=source,a=(src=foo.ogg,type=audio/ogg)),(t=source,a=(src=foo.mp3,type=audio/mpeg))))")
				.uonR("(\n\t_type=audio,\n\ta=(\n\t\tcontrols=controls\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=source,\n\t\t\ta=(\n\t\t\t\tsrc=foo.ogg,\n\t\t\t\ttype=audio/ogg\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=source,\n\t\t\ta=(\n\t\t\t\tsrc=foo.mp3,\n\t\t\t\ttype=audio/mpeg\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=audio&a=(controls=controls)&c=@((_type=source,a=(src=foo.ogg,type=audio/ogg)),(_type=source,a=(src=foo.mp3,type=audio/mpeg)))")
				.urlEncT("t=audio&a=(controls=controls)&c=@((t=source,a=(src=foo.ogg,type=audio/ogg)),(t=source,a=(src=foo.mp3,type=audio/mpeg)))")
				.urlEncR("_type=audio\n&a=(\n\tcontrols=controls\n)\n&c=@(\n\t(\n\t\t_type=source,\n\t\ta=(\n\t\t\tsrc=foo.ogg,\n\t\t\ttype=audio/ogg\n\t\t)\n\t),\n\t(\n\t\t_type=source,\n\t\ta=(\n\t\t\tsrc=foo.mp3,\n\t\t\ttype=audio/mpeg\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A5617564696FA16181A8636F6E74726F6C73A8636F6E74726F6C73A1639282A55F74797065A6736F75726365A16182A3737263A7666F6F2E6F6767A474797065A9617564696F2F6F676782A55F74797065A6736F75726365A16182A3737263A7666F6F2E6D7033A474797065AA617564696F2F6D706567")
				.msgPackT("83A174A5617564696FA16181A8636F6E74726F6C73A8636F6E74726F6C73A1639282A174A6736F75726365A16182A3737263A7666F6F2E6F6767A474797065A9617564696F2F6F676782A174A6736F75726365A16182A3737263A7666F6F2E6D7033A474797065AA617564696F2F6D706567")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>audio</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>source</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.ogg</jp:src>\n<jp:type>audio/ogg</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>source</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp3</jp:src>\n<jp:type>audio/mpeg</jp:type>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>audio</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>source</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.ogg</jp:src>\n<jp:type>audio/ogg</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>source</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp3</jp:src>\n<jp:type>audio/mpeg</jp:type>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>audio</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:controls>controls</jp:controls>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>source</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.ogg</jp:src>\n            <jp:type>audio/ogg</jp:type>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>source</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.mp3</jp:src>\n            <jp:type>audio/mpeg</jp:type>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Audio.class))
				.verify(x -> verify(x.getChild(0)).isType(Source.class))
				.verify(x -> verify(x.getChild(1)).isType(Source.class))
			},
			{	/* 22 */
				new ComboInput<P>(
					"Bdi-1",
					P.class,
					p("foo", bdi("إيان"), "bar")
				)
				.json("{_type:'p',c:['foo',{_type:'bdi',c:'إيان'},'bar']}")
				.jsonT("{t:'p',c:['foo',{t:'bdi',c:'إيان'},'bar']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'bdi',\n\t\t\tc: 'إيان'\n\t\t},\n\t\t'bar'\n\t]\n}")
				.xml("<p>foo<bdi>إيان</bdi>bar</p>")
				.xmlT("<p>foo<bdi>إيان</bdi>bar</p>")
				.xmlR("<p>foo<bdi>إيان</bdi>bar</p>\n")
				.xmlNs("<p>foo<bdi>إيان</bdi>bar</p>")
				.html("<p>foo<bdi>إيان</bdi>bar</p>")
				.htmlT("<p>foo<bdi>إيان</bdi>bar</p>")
				.htmlR("<p>foo<bdi>إيان</bdi>bar</p>\n")
				.uon("(_type=p,c=@(foo,(_type=bdi,c=إيان),bar))")
				.uonT("(t=p,c=@(foo,(t=bdi,c=إيان),bar))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=bdi,\n\t\t\tc=إيان\n\t\t),\n\t\tbar\n\t)\n)")
				.urlEnc("_type=p&c=@(foo,(_type=bdi,c=%D8%A5%D9%8A%D8%A7%D9%86),bar)")
				.urlEncT("t=p&c=@(foo,(t=bdi,c=%D8%A5%D9%8A%D8%A7%D9%86),bar)")
				.urlEncR("_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=bdi,\n\t\tc=%D8%A5%D9%8A%D8%A7%D9%86\n\t),\n\tbar\n)")
				.msgPack("82A55F74797065A170A16393A3666F6F82A55F74797065A3626469A163A8D8A5D98AD8A7D986A3626172")
				.msgPackT("82A174A170A16393A3666F6F82A174A3626469A163A8D8A5D98AD8A7D986A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>bdi</jp:_type>\n<jp:c>إيان</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>bdi</jp:t>\n<jp:c>إيان</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>bdi</jp:_type>\n          <jp:c>إيان</jp:c>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Bdi.class))
			},
			{	/* 23 */
				new ComboInput<P>(
					"Bdo-1",
					P.class,
					p("foo", bdo("rtl", "baz"), "bar")
				)
				.json("{_type:'p',c:['foo',{_type:'bdo',a:{dir:'rtl'},c:['baz']},'bar']}")
				.jsonT("{t:'p',c:['foo',{t:'bdo',a:{dir:'rtl'},c:['baz']},'bar']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'bdo',\n\t\t\ta: {\n\t\t\t\tdir: 'rtl'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'baz'\n\t\t\t]\n\t\t},\n\t\t'bar'\n\t]\n}")
				.xml("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>")
				.xmlT("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>")
				.xmlR("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n")
				.xmlNs("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>")
				.html("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>")
				.htmlT("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>")
				.htmlR("<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n")
				.uon("(_type=p,c=@(foo,(_type=bdo,a=(dir=rtl),c=@(baz)),bar))")
				.uonT("(t=p,c=@(foo,(t=bdo,a=(dir=rtl),c=@(baz)),bar))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=bdo,\n\t\t\ta=(\n\t\t\t\tdir=rtl\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tbaz\n\t\t\t)\n\t\t),\n\t\tbar\n\t)\n)")
				.urlEnc("_type=p&c=@(foo,(_type=bdo,a=(dir=rtl),c=@(baz)),bar)")
				.urlEncT("t=p&c=@(foo,(t=bdo,a=(dir=rtl),c=@(baz)),bar)")
				.urlEncR("_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=bdo,\n\t\ta=(\n\t\t\tdir=rtl\n\t\t),\n\t\tc=@(\n\t\t\tbaz\n\t\t)\n\t),\n\tbar\n)")
				.msgPack("82A55F74797065A170A16393A3666F6F83A55F74797065A362646FA16181A3646972A372746CA16391A362617AA3626172")
				.msgPackT("82A174A170A16393A3666F6F83A174A362646FA16181A3646972A372746CA16391A362617AA3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>bdo</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:dir>rtl</jp:dir>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>bdo</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:dir>rtl</jp:dir>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>bdo</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:dir>rtl</jp:dir>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>baz</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Bdo.class))
			},
			{	/* 24 */
				new ComboInput<Blockquote>(
					"Blockquote-1",
					Blockquote.class,
					blockquote("foo")
				)
				.json("{_type:'blockquote',c:['foo']}")
				.jsonT("{t:'blockquote',c:['foo']}")
				.jsonR("{\n\t_type: 'blockquote',\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<blockquote>foo</blockquote>")
				.xmlT("<blockquote>foo</blockquote>")
				.xmlR("<blockquote>foo</blockquote>\n")
				.xmlNs("<blockquote>foo</blockquote>")
				.html("<blockquote>foo</blockquote>")
				.htmlT("<blockquote>foo</blockquote>")
				.htmlR("<blockquote>foo</blockquote>\n")
				.uon("(_type=blockquote,c=@(foo))")
				.uonT("(t=blockquote,c=@(foo))")
				.uonR("(\n\t_type=blockquote,\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=blockquote&c=@(foo)")
				.urlEncT("t=blockquote&c=@(foo)")
				.urlEncR("_type=blockquote\n&c=@(\n\tfoo\n)")
				.msgPack("82A55F74797065AA626C6F636B71756F7465A16391A3666F6F")
				.msgPackT("82A174AA626C6F636B71756F7465A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>blockquote</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>blockquote</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>blockquote</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Blockquote.class))
			},
			{	/* 25 */
				new ComboInput<Br>(
					"Br-1",
					Br.class,
					br()
				)
				.json("{_type:'br'}")
				.jsonT("{t:'br'}")
				.jsonR("{\n\t_type: 'br'\n}")
				.xml("<br/>")
				.xmlT("<br/>")
				.xmlR("<br/>\n")
				.xmlNs("<br/>")
				.html("<br/>")
				.htmlT("<br/>")
				.htmlR("<br/>\n")
				.uon("(_type=br)")
				.uonT("(t=br)")
				.uonR("(\n\t_type=br\n)")
				.urlEnc("_type=br")
				.urlEncT("t=br")
				.urlEncR("_type=br")
				.msgPack("81A55F74797065A26272")
				.msgPackT("81A174A26272")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>br</jp:_type>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>br</jp:t>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>br</jp:_type>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Br.class))
			},
			{	/* 26 */
				new ComboInput<P>(
					"Br-2",
					P.class,
					p(br())
				)
				.json("{_type:'p',c:[{_type:'br'}]}")
				.jsonT("{t:'p',c:[{t:'br'}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'br'\n\t\t}\n\t]\n}")
				.xml("<p><br/></p>")
				.xmlT("<p><br/></p>")
				.xmlR("<p><br/></p>\n")
				.xmlNs("<p><br/></p>")
				.html("<p><br/></p>")
				.htmlT("<p><br/></p>")
				.htmlR("<p><br/></p>\n")
				.uon("(_type=p,c=@((_type=br)))")
				.uonT("(t=p,c=@((t=br)))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=br\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=br))")
				.urlEncT("t=p&c=@((t=br))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=br\n\t)\n)")
				.msgPack("82A55F74797065A170A1639181A55F74797065A26272")
				.msgPackT("82A174A170A1639181A174A26272")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>br</jp:_type>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>br</jp:t>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>br</jp:_type>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Br.class))
			},
			{	/* 27 */
				new ComboInput<Button>(
					"Button-1",
					Button.class,
					button("button", "foo")
				)
				.json("{_type:'button',a:{type:'button'},c:['foo']}")
				.jsonT("{t:'button',a:{type:'button'},c:['foo']}")
				.jsonR("{\n\t_type: 'button',\n\ta: {\n\t\ttype: 'button'\n\t},\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<button type='button'>foo</button>")
				.xmlT("<button type='button'>foo</button>")
				.xmlR("<button type='button'>foo</button>\n")
				.xmlNs("<button type='button'>foo</button>")
				.html("<button type='button'>foo</button>")
				.htmlT("<button type='button'>foo</button>")
				.htmlR("<button type='button'>foo</button>\n")
				.uon("(_type=button,a=(type=button),c=@(foo))")
				.uonT("(t=button,a=(type=button),c=@(foo))")
				.uonR("(\n\t_type=button,\n\ta=(\n\t\ttype=button\n\t),\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=button&a=(type=button)&c=@(foo)")
				.urlEncT("t=button&a=(type=button)&c=@(foo)")
				.urlEncR("_type=button\n&a=(\n\ttype=button\n)\n&c=@(\n\tfoo\n)")
				.msgPack("83A55F74797065A6627574746F6EA16181A474797065A6627574746F6EA16391A3666F6F")
				.msgPackT("83A174A6627574746F6EA16181A474797065A6627574746F6EA16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>button</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>button</jp:type>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>button</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>button</jp:type>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>button</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:type>button</jp:type>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Button.class))
			},
			{	/* 28 */
				new ComboInput<Canvas>(
					"Canvas-1",
					Canvas.class,
					canvas(100, 200)
				)
				.json("{_type:'canvas',a:{width:100,height:200}}")
				.jsonT("{t:'canvas',a:{width:100,height:200}}")
				.jsonR("{\n\t_type: 'canvas',\n\ta: {\n\t\twidth: 100,\n\t\theight: 200\n\t}\n}")
				.xml("<canvas width='100' height='200' nil='true'></canvas>")
				.xmlT("<canvas width='100' height='200' nil='true'></canvas>")
				.xmlR("<canvas width='100' height='200' nil='true'>\n</canvas>\n")
				.xmlNs("<canvas width='100' height='200' nil='true'></canvas>")
				.html("<canvas width='100' height='200' nil='true'></canvas>")
				.htmlT("<canvas width='100' height='200' nil='true'></canvas>")
				.htmlR("<canvas width='100' height='200' nil='true'>\n</canvas>\n")
				.uon("(_type=canvas,a=(width=100,height=200))")
				.uonT("(t=canvas,a=(width=100,height=200))")
				.uonR("(\n\t_type=canvas,\n\ta=(\n\t\twidth=100,\n\t\theight=200\n\t)\n)")
				.urlEnc("_type=canvas&a=(width=100,height=200)")
				.urlEncT("t=canvas&a=(width=100,height=200)")
				.urlEncR("_type=canvas\n&a=(\n\twidth=100,\n\theight=200\n)")
				.msgPack("82A55F74797065A663616E766173A16182A5776964746864A6686569676874D100C8")
				.msgPackT("82A174A663616E766173A16182A5776964746864A6686569676874D100C8")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>canvas</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>canvas</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>canvas</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:width>100</jp:width>\n      <jp:height>200</jp:height>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Canvas.class))
			},
			{	/* 29 */
				new ComboInput<P>(
					"Cite-1",
					P.class,
					p(cite("foo"))
				)
				.json("{_type:'p',c:[{_type:'cite',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'cite',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'cite',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><cite>foo</cite></p>")
				.xmlT("<p><cite>foo</cite></p>")
				.xmlR("<p><cite>foo</cite></p>\n")
				.xmlNs("<p><cite>foo</cite></p>")
				.html("<p><cite>foo</cite></p>")
				.htmlT("<p><cite>foo</cite></p>")
				.htmlR("<p><cite>foo</cite></p>\n")
				.uon("(_type=p,c=@((_type=cite,c=@(foo))))")
				.uonT("(t=p,c=@((t=cite,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=cite,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=cite,c=@(foo)))")
				.urlEncT("t=p&c=@((t=cite,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=cite,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A463697465A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A463697465A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>cite</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>cite</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>cite</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Cite.class))
			},
			{	/* 30 */
				new ComboInput<Code>(
					"Code-1",
					Code.class,
					code("foo\n\tbar")
				)
				.json("{_type:'code',c:['foo\\n\\tbar']}")
				.jsonT("{t:'code',c:['foo\\n\\tbar']}")
				.jsonR("{\n\t_type: 'code',\n\tc: [\n\t\t'foo\\n\\tbar'\n\t]\n}")
				.xml("<code>foo&#x000a;&#x0009;bar</code>")
				.xmlT("<code>foo&#x000a;&#x0009;bar</code>")
				.xmlR("<code>foo&#x000a;&#x0009;bar</code>\n")
				.xmlNs("<code>foo&#x000a;&#x0009;bar</code>")
				.html("<code>foo<br/><sp>&#x2003;</sp>bar</code>")
				.htmlT("<code>foo<br/><sp>&#x2003;</sp>bar</code>")
				.htmlR("<code>foo<br/><sp>&#x2003;</sp>bar</code>\n")
				.uon("(_type=code,c=@('foo\n\tbar'))")
				.uonT("(t=code,c=@('foo\n\tbar'))")
				.uonR("(\n\t_type=code,\n\tc=@(\n\t\t'foo\n\tbar'\n\t)\n)")
				.urlEnc("_type=code&c=@('foo%0A%09bar')")
				.urlEncT("t=code&c=@('foo%0A%09bar')")
				.urlEncR("_type=code\n&c=@(\n\t'foo%0A%09bar'\n)")
				.msgPack("82A55F74797065A4636F6465A16391A8666F6F0A09626172")
				.msgPackT("82A174A4636F6465A16391A8666F6F0A09626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>code</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo_x000A__x0009_bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>code</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo_x000A__x0009_bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>code</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo_x000A__x0009_bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Code.class))
			},
			{	/* 31 */
				new ComboInput<Datalist>(
					"Datalist-1",
					Datalist.class,
					datalist("foo",
						option("One"),
						option("Two")
					)
				)
				.json("{_type:'datalist',a:{id:'foo'},c:[{_type:'option',c:'One'},{_type:'option',c:'Two'}]}")
				.jsonT("{t:'datalist',a:{id:'foo'},c:[{t:'option',c:'One'},{t:'option',c:'Two'}]}")
				.jsonR("{\n\t_type: 'datalist',\n\ta: {\n\t\tid: 'foo'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'option',\n\t\t\tc: 'One'\n\t\t},\n\t\t{\n\t\t\t_type: 'option',\n\t\t\tc: 'Two'\n\t\t}\n\t]\n}")
				.xml("<datalist id='foo'><option>One</option><option>Two</option></datalist>")
				.xmlT("<datalist id='foo'><option>One</option><option>Two</option></datalist>")
				.xmlR("<datalist id='foo'>\n\t<option>One</option>\n\t<option>Two</option>\n</datalist>\n")
				.xmlNs("<datalist id='foo'><option>One</option><option>Two</option></datalist>")
				.html("<datalist id='foo'><option>One</option><option>Two</option></datalist>")
				.htmlT("<datalist id='foo'><option>One</option><option>Two</option></datalist>")
				.htmlR("<datalist id='foo'>\n\t<option>One</option>\n\t<option>Two</option>\n</datalist>\n")
				.uon("(_type=datalist,a=(id=foo),c=@((_type=option,c=One),(_type=option,c=Two)))")
				.uonT("(t=datalist,a=(id=foo),c=@((t=option,c=One),(t=option,c=Two)))")
				.uonR("(\n\t_type=datalist,\n\ta=(\n\t\tid=foo\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=option,\n\t\t\tc=One\n\t\t),\n\t\t(\n\t\t\t_type=option,\n\t\t\tc=Two\n\t\t)\n\t)\n)")
				.urlEnc("_type=datalist&a=(id=foo)&c=@((_type=option,c=One),(_type=option,c=Two))")
				.urlEncT("t=datalist&a=(id=foo)&c=@((t=option,c=One),(t=option,c=Two))")
				.urlEncR("_type=datalist\n&a=(\n\tid=foo\n)\n&c=@(\n\t(\n\t\t_type=option,\n\t\tc=One\n\t),\n\t(\n\t\t_type=option,\n\t\tc=Two\n\t)\n)")
				.msgPack("83A55F74797065A8646174616C697374A16181A26964A3666F6FA1639282A55F74797065A66F7074696F6EA163A34F6E6582A55F74797065A66F7074696F6EA163A354776F")
				.msgPackT("83A174A8646174616C697374A16181A26964A3666F6FA1639282A174A66F7074696F6EA163A34F6E6582A174A66F7074696F6EA163A354776F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>datalist</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>option</jp:_type>\n<jp:c>One</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>option</jp:_type>\n<jp:c>Two</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>datalist</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>option</jp:t>\n<jp:c>One</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>option</jp:t>\n<jp:c>Two</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>datalist</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:id>foo</jp:id>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>option</jp:_type>\n          <jp:c>One</jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>option</jp:_type>\n          <jp:c>Two</jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Datalist.class))
				.verify(x -> verify(x.getChild(0)).isType(Option.class))
				.verify(x -> verify(x.getChild(1)).isType(Option.class))
			},
			{	/* 32 */
				new ComboInput<Dl>(
					"Dl/Dt/Dd",
					Dl.class,
					dl(
						dt("foo"),
						dd("bar")
					)
				)
				.json("{_type:'dl',c:[{_type:'dt',c:['foo']},{_type:'dd',c:['bar']}]}")
				.jsonT("{t:'dl',c:[{t:'dt',c:['foo']},{t:'dd',c:['bar']}]}")
				.jsonR("{\n\t_type: 'dl',\n\tc: [\n\t\t{\n\t\t\t_type: 'dt',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'dd',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<dl><dt>foo</dt><dd>bar</dd></dl>")
				.xmlT("<dl><dt>foo</dt><dd>bar</dd></dl>")
				.xmlR("<dl>\n\t<dt>foo</dt>\n\t<dd>bar</dd>\n</dl>\n")
				.xmlNs("<dl><dt>foo</dt><dd>bar</dd></dl>")
				.html("<dl><dt>foo</dt><dd>bar</dd></dl>")
				.htmlT("<dl><dt>foo</dt><dd>bar</dd></dl>")
				.htmlR("<dl>\n\t<dt>foo</dt>\n\t<dd>bar</dd>\n</dl>\n")
				.uon("(_type=dl,c=@((_type=dt,c=@(foo)),(_type=dd,c=@(bar))))")
				.uonT("(t=dl,c=@((t=dt,c=@(foo)),(t=dd,c=@(bar))))")
				.uonR("(\n\t_type=dl,\n\tc=@(\n\t\t(\n\t\t\t_type=dt,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=dd,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=dl&c=@((_type=dt,c=@(foo)),(_type=dd,c=@(bar)))")
				.urlEncT("t=dl&c=@((t=dt,c=@(foo)),(t=dd,c=@(bar)))")
				.urlEncR("_type=dl\n&c=@(\n\t(\n\t\t_type=dt,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t),\n\t(\n\t\t_type=dd,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A2646CA1639282A55F74797065A26474A16391A3666F6F82A55F74797065A26464A16391A3626172")
				.msgPackT("82A174A2646CA1639282A174A26474A16391A3666F6F82A174A26464A16391A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>dl</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>dt</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>dd</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>dl</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>dt</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>dd</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>dl</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>dt</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>dd</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Dl.class))
				.verify(x -> verify(x.getChild(0)).isType(Dt.class))
				.verify(x -> verify(x.getChild(1)).isType(Dd.class))
			},
			{	/* 33 */
				new ComboInput<P>(
					"Del/Ins",
					P.class,
					p(del("foo",btag,"bar"),ins("baz"))
				)
				.json("{_type:'p',c:[{_type:'del',c:['foo',{_type:'b',c:['bbb']},'bar']},{_type:'ins',c:['baz']}]}")
				.jsonT("{t:'p',c:[{t:'del',c:['foo',{t:'b',c:['bbb']},'bar']},{t:'ins',c:['baz']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'del',\n\t\t\tc: [\n\t\t\t\t'foo',\n\t\t\t\t{\n\t\t\t\t\t_type: 'b',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'bbb'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'ins',\n\t\t\tc: [\n\t\t\t\t'baz'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>")
				.xmlT("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>")
				.xmlR("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n")
				.xmlNs("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>")
				.html("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>")
				.htmlT("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>")
				.htmlR("<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n")
				.uon("(_type=p,c=@((_type=del,c=@(foo,(_type=b,c=@(bbb)),bar)),(_type=ins,c=@(baz))))")
				.uonT("(t=p,c=@((t=del,c=@(foo,(t=b,c=@(bbb)),bar)),(t=ins,c=@(baz))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=del,\n\t\t\tc=@(\n\t\t\t\tfoo,\n\t\t\t\t(\n\t\t\t\t\t_type=b,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tbbb\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=ins,\n\t\t\tc=@(\n\t\t\t\tbaz\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=del,c=@(foo,(_type=b,c=@(bbb)),bar)),(_type=ins,c=@(baz)))")
				.urlEncT("t=p&c=@((t=del,c=@(foo,(t=b,c=@(bbb)),bar)),(t=ins,c=@(baz)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=del,\n\t\tc=@(\n\t\t\tfoo,\n\t\t\t(\n\t\t\t\t_type=b,\n\t\t\t\tc=@(\n\t\t\t\t\tbbb\n\t\t\t\t)\n\t\t\t),\n\t\t\tbar\n\t\t)\n\t),\n\t(\n\t\t_type=ins,\n\t\tc=@(\n\t\t\tbaz\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639282A55F74797065A364656CA16393A3666F6F82A55F74797065A162A16391A3626262A362617282A55F74797065A3696E73A16391A362617A")
				.msgPackT("82A174A170A1639282A174A364656CA16393A3666F6F82A174A162A16391A3626262A362617282A174A3696E73A16391A362617A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>del</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>ins</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>del</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>ins</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>del</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>b</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>bbb</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>ins</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>baz</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Del.class))
				.verify(x -> verify(x.getChild(Del.class, 0).getChild(1)).isType(B.class))
				.verify(x -> verify(x.getChild(1)).isType(Ins.class))
			},
			{	/* 34 */
				new ComboInput<P>(
					"Dfn",
					P.class,
					p(dfn("foo"))
				)
				.json("{_type:'p',c:[{_type:'dfn',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'dfn',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'dfn',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><dfn>foo</dfn></p>")
				.xmlT("<p><dfn>foo</dfn></p>")
				.xmlR("<p><dfn>foo</dfn></p>\n")
				.xmlNs("<p><dfn>foo</dfn></p>")
				.html("<p><dfn>foo</dfn></p>")
				.htmlT("<p><dfn>foo</dfn></p>")
				.htmlR("<p><dfn>foo</dfn></p>\n")
				.uon("(_type=p,c=@((_type=dfn,c=@(foo))))")
				.uonT("(t=p,c=@((t=dfn,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=dfn,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=dfn,c=@(foo)))")
				.urlEncT("t=p&c=@((t=dfn,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=dfn,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A364666EA16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A364666EA16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>dfn</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>dfn</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>dfn</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Dfn.class))
			},
			{	/* 35 */
				new ComboInput<Div>(
					"Div",
					Div.class,
					div("foo",btag,"bar")
				)
				.json("{_type:'div',c:['foo',{_type:'b',c:['bbb']},'bar']}")
				.jsonT("{t:'div',c:['foo',{t:'b',c:['bbb']},'bar']}")
				.jsonR("{\n\t_type: 'div',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'b',\n\t\t\tc: [\n\t\t\t\t'bbb'\n\t\t\t]\n\t\t},\n\t\t'bar'\n\t]\n}")
				.xml("<div>foo<b>bbb</b>bar</div>")
				.xmlT("<div>foo<b>bbb</b>bar</div>")
				.xmlR("<div>foo<b>bbb</b>bar</div>\n")
				.xmlNs("<div>foo<b>bbb</b>bar</div>")
				.html("<div>foo<b>bbb</b>bar</div>")
				.htmlT("<div>foo<b>bbb</b>bar</div>")
				.htmlR("<div>foo<b>bbb</b>bar</div>\n")
				.uon("(_type=div,c=@(foo,(_type=b,c=@(bbb)),bar))")
				.uonT("(t=div,c=@(foo,(t=b,c=@(bbb)),bar))")
				.uonR("(\n\t_type=div,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=b,\n\t\t\tc=@(\n\t\t\t\tbbb\n\t\t\t)\n\t\t),\n\t\tbar\n\t)\n)")
				.urlEnc("_type=div&c=@(foo,(_type=b,c=@(bbb)),bar)")
				.urlEncT("t=div&c=@(foo,(t=b,c=@(bbb)),bar)")
				.urlEncR("_type=div\n&c=@(\n\tfoo,\n\t(\n\t\t_type=b,\n\t\tc=@(\n\t\t\tbbb\n\t\t)\n\t),\n\tbar\n)")
				.msgPack("82A55F74797065A3646976A16393A3666F6F82A55F74797065A162A16391A3626262A3626172")
				.msgPackT("82A174A3646976A16393A3666F6F82A174A162A16391A3626262A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>div</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>b</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>div</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>b</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bbb</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>div</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>b</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bbb</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Div.class))
				.verify(x -> verify(x.getChild(1)).isType(B.class))
			},
			{	/* 36 */
				new ComboInput<P>(
					"Em",
					P.class,
					p("foo",em("bar"),"baz")
				)
				.json("{_type:'p',c:['foo',{_type:'em',c:['bar']},'baz']}")
				.jsonT("{t:'p',c:['foo',{t:'em',c:['bar']},'baz']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'em',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}")
				.xml("<p>foo<em>bar</em>baz</p>")
				.xmlT("<p>foo<em>bar</em>baz</p>")
				.xmlR("<p>foo<em>bar</em>baz</p>\n")
				.xmlNs("<p>foo<em>bar</em>baz</p>")
				.html("<p>foo<em>bar</em>baz</p>")
				.htmlT("<p>foo<em>bar</em>baz</p>")
				.htmlR("<p>foo<em>bar</em>baz</p>\n")
				.uon("(_type=p,c=@(foo,(_type=em,c=@(bar)),baz))")
				.uonT("(t=p,c=@(foo,(t=em,c=@(bar)),baz))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=em,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)")
				.urlEnc("_type=p&c=@(foo,(_type=em,c=@(bar)),baz)")
				.urlEncT("t=p&c=@(foo,(t=em,c=@(bar)),baz)")
				.urlEncR("_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=em,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\tbaz\n)")
				.msgPack("82A55F74797065A170A16393A3666F6F82A55F74797065A2656DA16391A3626172A362617A")
				.msgPackT("82A174A170A16393A3666F6F82A174A2656DA16391A3626172A362617A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>em</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>em</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>em</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Em.class))
			},
			{	/* 37 */
				new ComboInput<Embed>(
					"Embed",
					Embed.class,
					embed("foo.swf")
				)
				.json("{_type:'embed',a:{src:'foo.swf'}}")
				.jsonT("{t:'embed',a:{src:'foo.swf'}}")
				.jsonR("{\n\t_type: 'embed',\n\ta: {\n\t\tsrc: 'foo.swf'\n\t}\n}")
				.xml("<embed src='foo.swf'/>")
				.xmlT("<embed src='foo.swf'/>")
				.xmlR("<embed src='foo.swf'/>\n")
				.xmlNs("<embed src='foo.swf'/>")
				.html("<embed src='foo.swf'/>")
				.htmlT("<embed src='foo.swf'/>")
				.htmlR("<embed src='foo.swf'/>\n")
				.uon("(_type=embed,a=(src=foo.swf))")
				.uonT("(t=embed,a=(src=foo.swf))")
				.uonR("(\n\t_type=embed,\n\ta=(\n\t\tsrc=foo.swf\n\t)\n)")
				.urlEnc("_type=embed&a=(src=foo.swf)")
				.urlEncT("t=embed&a=(src=foo.swf)")
				.urlEncR("_type=embed\n&a=(\n\tsrc=foo.swf\n)")
				.msgPack("82A55F74797065A5656D626564A16181A3737263A7666F6F2E737766")
				.msgPackT("82A174A5656D626564A16181A3737263A7666F6F2E737766")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>embed</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.swf</jp:src>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>embed</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.swf</jp:src>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>embed</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:src>foo.swf</jp:src>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Embed.class))
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
					)
				)
				.json("{_type:'form',a:{action:'bar'},c:[{_type:'fieldset',c:[{_type:'legend',c:['foo:']},'Name:',{_type:'input',a:{type:'text'}},{_type:'br'},'Email:',{_type:'input',a:{type:'text'}},{_type:'br'},'X:',{_type:'keygen',a:{name:'X'}},{_type:'label',a:{'for':'Name'},c:['label']}]}]}")
				.jsonT("{t:'form',a:{action:'bar'},c:[{t:'fieldset',c:[{t:'legend',c:['foo:']},'Name:',{t:'input',a:{type:'text'}},{t:'br'},'Email:',{t:'input',a:{type:'text'}},{t:'br'},'X:',{t:'keygen',a:{name:'X'}},{t:'label',a:{'for':'Name'},c:['label']}]}]}")
				.jsonR("{\n\t_type: 'form',\n\ta: {\n\t\taction: 'bar'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'fieldset',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'legend',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'foo:'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'Name:',\n\t\t\t\t{\n\t\t\t\t\t_type: 'input',\n\t\t\t\t\ta: {\n\t\t\t\t\t\ttype: 'text'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'br'\n\t\t\t\t},\n\t\t\t\t'Email:',\n\t\t\t\t{\n\t\t\t\t\t_type: 'input',\n\t\t\t\t\ta: {\n\t\t\t\t\t\ttype: 'text'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'br'\n\t\t\t\t},\n\t\t\t\t'X:',\n\t\t\t\t{\n\t\t\t\t\t_type: 'keygen',\n\t\t\t\t\ta: {\n\t\t\t\t\t\tname: 'X'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'label',\n\t\t\t\t\ta: {\n\t\t\t\t\t\t'for': 'Name'\n\t\t\t\t\t},\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'label'\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>")
				.xmlT("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>")
				.xmlR("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>\n")
				.xmlNs("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>")
				.html("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>")
				.htmlT("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>")
				.htmlR("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>\n")
				.uon("(_type=form,a=(action=bar),c=@((_type=fieldset,c=@((_type=legend,c=@(foo:)),Name:,(_type=input,a=(type=text)),(_type=br),Email:,(_type=input,a=(type=text)),(_type=br),X:,(_type=keygen,a=(name=X)),(_type=label,a=(for=Name),c=@(label))))))")
				.uonT("(t=form,a=(action=bar),c=@((t=fieldset,c=@((t=legend,c=@(foo:)),Name:,(t=input,a=(type=text)),(t=br),Email:,(t=input,a=(type=text)),(t=br),X:,(t=keygen,a=(name=X)),(t=label,a=(for=Name),c=@(label))))))")
				.uonR("(\n\t_type=form,\n\ta=(\n\t\taction=bar\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=fieldset,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=legend,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tfoo:\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tName:,\n\t\t\t\t(\n\t\t\t\t\t_type=input,\n\t\t\t\t\ta=(\n\t\t\t\t\t\ttype=text\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=br\n\t\t\t\t),\n\t\t\t\tEmail:,\n\t\t\t\t(\n\t\t\t\t\t_type=input,\n\t\t\t\t\ta=(\n\t\t\t\t\t\ttype=text\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=br\n\t\t\t\t),\n\t\t\t\tX:,\n\t\t\t\t(\n\t\t\t\t\t_type=keygen,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tname=X\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=label,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tfor=Name\n\t\t\t\t\t),\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tlabel\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=form&a=(action=bar)&c=@((_type=fieldset,c=@((_type=legend,c=@(foo:)),Name:,(_type=input,a=(type=text)),(_type=br),Email:,(_type=input,a=(type=text)),(_type=br),X:,(_type=keygen,a=(name=X)),(_type=label,a=(for=Name),c=@(label)))))")
				.urlEncT("t=form&a=(action=bar)&c=@((t=fieldset,c=@((t=legend,c=@(foo:)),Name:,(t=input,a=(type=text)),(t=br),Email:,(t=input,a=(type=text)),(t=br),X:,(t=keygen,a=(name=X)),(t=label,a=(for=Name),c=@(label)))))")
				.urlEncR("_type=form\n&a=(\n\taction=bar\n)\n&c=@(\n\t(\n\t\t_type=fieldset,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=legend,\n\t\t\t\tc=@(\n\t\t\t\t\tfoo:\n\t\t\t\t)\n\t\t\t),\n\t\t\tName:,\n\t\t\t(\n\t\t\t\t_type=input,\n\t\t\t\ta=(\n\t\t\t\t\ttype=text\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=br\n\t\t\t),\n\t\t\tEmail:,\n\t\t\t(\n\t\t\t\t_type=input,\n\t\t\t\ta=(\n\t\t\t\t\ttype=text\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=br\n\t\t\t),\n\t\t\tX:,\n\t\t\t(\n\t\t\t\t_type=keygen,\n\t\t\t\ta=(\n\t\t\t\t\tname=X\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=label,\n\t\t\t\ta=(\n\t\t\t\t\tfor=Name\n\t\t\t\t),\n\t\t\t\tc=@(\n\t\t\t\t\tlabel\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A4666F726DA16181A6616374696F6EA3626172A1639182A55F74797065A86669656C64736574A1639A82A55F74797065A66C6567656E64A16391A4666F6F3AA54E616D653A82A55F74797065A5696E707574A16181A474797065A47465787481A55F74797065A26272A6456D61696C3A82A55F74797065A5696E707574A16181A474797065A47465787481A55F74797065A26272A2583A82A55F74797065A66B657967656EA16181A46E616D65A15883A55F74797065A56C6162656CA16181A3666F72A44E616D65A16391A56C6162656C")
				.msgPackT("83A174A4666F726DA16181A6616374696F6EA3626172A1639182A174A86669656C64736574A1639A82A174A66C6567656E64A16391A4666F6F3AA54E616D653A82A174A5696E707574A16181A474797065A47465787481A174A26272A6456D61696C3A82A174A5696E707574A16181A474797065A47465787481A174A26272A2583A82A174A66B657967656EA16181A46E616D65A15883A174A56C6162656CA16181A3666F72A44E616D65A16391A56C6162656C")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>form</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:action>bar</jp:action>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>fieldset</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>legend</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo:</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>Name:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>br</jp:_type>\n</rdf:li>\n<rdf:li>Email:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>br</jp:_type>\n</rdf:li>\n<rdf:li>X:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>keygen</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>X</jp:name>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>label</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:for>Name</jp:for>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>label</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>form</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:action>bar</jp:action>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>fieldset</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>legend</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo:</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>Name:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>br</jp:t>\n</rdf:li>\n<rdf:li>Email:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>br</jp:t>\n</rdf:li>\n<rdf:li>X:</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>keygen</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>X</jp:name>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>label</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:for>Name</jp:for>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>label</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>form</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:action>bar</jp:action>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>fieldset</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>legend</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>foo:</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>Name:</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>input</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:type>text</jp:type>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>br</jp:_type>\n              </rdf:li>\n              <rdf:li>Email:</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>input</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:type>text</jp:type>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>br</jp:_type>\n              </rdf:li>\n              <rdf:li>X:</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>keygen</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:name>X</jp:name>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>label</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:for>Name</jp:for>\n                </jp:a>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>label</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Form.class))
				.verify(x -> verify(x.getChild(0)).isType(Fieldset.class))
				.verify(x -> verify(x.getChild(0,0)).isType(Legend.class))
				.verify(x -> verify(x.getChild(0,2)).isType(Input.class))
				.verify(x -> verify(x.getChild(0,3)).isType(Br.class))
				.verify(x -> verify(x.getChild(0,5)).isType(Input.class))
				.verify(x -> verify(x.getChild(0,6)).isType(Br.class))
				.verify(x -> verify(x.getChild(0,8)).isType(Keygen.class))
				.verify(x -> verify(x.getChild(0,9)).isType(Label.class))
			},
			{	/* 39 */
				new ComboInput<Figure>(
					"Figure/Figcaption/Img",
					Figure.class,
					figure(
						img("foo.png").alt("foo").width(100).height(200),
						figcaption("The caption")
					)
				)
				.json("{_type:'figure',c:[{_type:'img',a:{src:'foo.png',alt:'foo',width:100,height:200}},{_type:'figcaption',c:['The caption']}]}")
				.jsonT("{t:'figure',c:[{t:'img',a:{src:'foo.png',alt:'foo',width:100,height:200}},{t:'figcaption',c:['The caption']}]}")
				.jsonR("{\n\t_type: 'figure',\n\tc: [\n\t\t{\n\t\t\t_type: 'img',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.png',\n\t\t\t\talt: 'foo',\n\t\t\t\twidth: 100,\n\t\t\t\theight: 200\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'figcaption',\n\t\t\tc: [\n\t\t\t\t'The caption'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>")
				.xmlT("<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>")
				.xmlR("<figure>\n\t<img src='foo.png' alt='foo' width='100' height='200'/>\n\t<figcaption>The caption</figcaption>\n</figure>\n")
				.xmlNs("<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>")
				.html("<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>")
				.htmlT("<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>")
				.htmlR("<figure>\n\t<img src='foo.png' alt='foo' width='100' height='200'/>\n\t<figcaption>The caption</figcaption>\n</figure>\n")
				.uon("(_type=figure,c=@((_type=img,a=(src=foo.png,alt=foo,width=100,height=200)),(_type=figcaption,c=@('The caption'))))")
				.uonT("(t=figure,c=@((t=img,a=(src=foo.png,alt=foo,width=100,height=200)),(t=figcaption,c=@('The caption'))))")
				.uonR("(\n\t_type=figure,\n\tc=@(\n\t\t(\n\t\t\t_type=img,\n\t\t\ta=(\n\t\t\t\tsrc=foo.png,\n\t\t\t\talt=foo,\n\t\t\t\twidth=100,\n\t\t\t\theight=200\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=figcaption,\n\t\t\tc=@(\n\t\t\t\t'The caption'\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=figure&c=@((_type=img,a=(src=foo.png,alt=foo,width=100,height=200)),(_type=figcaption,c=@('The+caption')))")
				.urlEncT("t=figure&c=@((t=img,a=(src=foo.png,alt=foo,width=100,height=200)),(t=figcaption,c=@('The+caption')))")
				.urlEncR("_type=figure\n&c=@(\n\t(\n\t\t_type=img,\n\t\ta=(\n\t\t\tsrc=foo.png,\n\t\t\talt=foo,\n\t\t\twidth=100,\n\t\t\theight=200\n\t\t)\n\t),\n\t(\n\t\t_type=figcaption,\n\t\tc=@(\n\t\t\t'The+caption'\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A6666967757265A1639282A55F74797065A3696D67A16184A3737263A7666F6F2E706E67A3616C74A3666F6FA5776964746864A6686569676874D100C882A55F74797065AA66696763617074696F6EA16391AB5468652063617074696F6E")
				.msgPackT("82A174A6666967757265A1639282A174A3696D67A16184A3737263A7666F6F2E706E67A3616C74A3666F6FA5776964746864A6686569676874D100C882A174AA66696763617074696F6EA16391AB5468652063617074696F6E")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>figure</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>img</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.png</jp:src>\n<jp:alt>foo</jp:alt>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>figcaption</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>The caption</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>figure</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>img</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.png</jp:src>\n<jp:alt>foo</jp:alt>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>figcaption</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>The caption</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>figure</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>img</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.png</jp:src>\n            <jp:alt>foo</jp:alt>\n            <jp:width>100</jp:width>\n            <jp:height>200</jp:height>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>figcaption</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>The caption</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Figure.class))
				.verify(x -> verify(x.getChild(0)).isType(Img.class))
				.verify(x -> verify(x.getChild(1)).isType(Figcaption.class))
			},
			{	/* 40 */
				new ComboInput<Div>(
					"H1/H2/H3/H4/H5/H6",
					Div.class,
					div(
						h1("One"),h2("Two"),h3("Three"),h4("Four"),h5("Five"),h6("Six")
					)
				)
				.json("{_type:'div',c:[{_type:'h1',c:['One']},{_type:'h2',c:['Two']},{_type:'h3',c:['Three']},{_type:'h4',c:['Four']},{_type:'h5',c:['Five']},{_type:'h6',c:['Six']}]}")
				.jsonT("{t:'div',c:[{t:'h1',c:['One']},{t:'h2',c:['Two']},{t:'h3',c:['Three']},{t:'h4',c:['Four']},{t:'h5',c:['Five']},{t:'h6',c:['Six']}]}")
				.jsonR("{\n\t_type: 'div',\n\tc: [\n\t\t{\n\t\t\t_type: 'h1',\n\t\t\tc: [\n\t\t\t\t'One'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h2',\n\t\t\tc: [\n\t\t\t\t'Two'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h3',\n\t\t\tc: [\n\t\t\t\t'Three'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h4',\n\t\t\tc: [\n\t\t\t\t'Four'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h5',\n\t\t\tc: [\n\t\t\t\t'Five'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'h6',\n\t\t\tc: [\n\t\t\t\t'Six'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>")
				.xmlT("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>")
				.xmlR("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>\n")
				.xmlNs("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>")
				.html("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>")
				.htmlT("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>")
				.htmlR("<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>\n")
				.uon("(_type=div,c=@((_type=h1,c=@(One)),(_type=h2,c=@(Two)),(_type=h3,c=@(Three)),(_type=h4,c=@(Four)),(_type=h5,c=@(Five)),(_type=h6,c=@(Six))))")
				.uonT("(t=div,c=@((t=h1,c=@(One)),(t=h2,c=@(Two)),(t=h3,c=@(Three)),(t=h4,c=@(Four)),(t=h5,c=@(Five)),(t=h6,c=@(Six))))")
				.uonR("(\n\t_type=div,\n\tc=@(\n\t\t(\n\t\t\t_type=h1,\n\t\t\tc=@(\n\t\t\t\tOne\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h2,\n\t\t\tc=@(\n\t\t\t\tTwo\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h3,\n\t\t\tc=@(\n\t\t\t\tThree\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h4,\n\t\t\tc=@(\n\t\t\t\tFour\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h5,\n\t\t\tc=@(\n\t\t\t\tFive\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=h6,\n\t\t\tc=@(\n\t\t\t\tSix\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=div&c=@((_type=h1,c=@(One)),(_type=h2,c=@(Two)),(_type=h3,c=@(Three)),(_type=h4,c=@(Four)),(_type=h5,c=@(Five)),(_type=h6,c=@(Six)))")
				.urlEncT("t=div&c=@((t=h1,c=@(One)),(t=h2,c=@(Two)),(t=h3,c=@(Three)),(t=h4,c=@(Four)),(t=h5,c=@(Five)),(t=h6,c=@(Six)))")
				.urlEncR("_type=div\n&c=@(\n\t(\n\t\t_type=h1,\n\t\tc=@(\n\t\t\tOne\n\t\t)\n\t),\n\t(\n\t\t_type=h2,\n\t\tc=@(\n\t\t\tTwo\n\t\t)\n\t),\n\t(\n\t\t_type=h3,\n\t\tc=@(\n\t\t\tThree\n\t\t)\n\t),\n\t(\n\t\t_type=h4,\n\t\tc=@(\n\t\t\tFour\n\t\t)\n\t),\n\t(\n\t\t_type=h5,\n\t\tc=@(\n\t\t\tFive\n\t\t)\n\t),\n\t(\n\t\t_type=h6,\n\t\tc=@(\n\t\t\tSix\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A3646976A1639682A55F74797065A26831A16391A34F6E6582A55F74797065A26832A16391A354776F82A55F74797065A26833A16391A5546872656582A55F74797065A26834A16391A4466F757282A55F74797065A26835A16391A44669766582A55F74797065A26836A16391A3536978")
				.msgPackT("82A174A3646976A1639682A174A26831A16391A34F6E6582A174A26832A16391A354776F82A174A26833A16391A5546872656582A174A26834A16391A4466F757282A174A26835A16391A44669766582A174A26836A16391A3536978")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>div</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>One</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h2</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Two</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h3</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Three</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h4</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Four</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h5</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Five</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h6</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Six</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>div</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>One</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h2</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Two</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h3</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Three</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h4</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Four</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h5</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Five</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h6</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Six</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>div</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h1</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>One</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h2</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Two</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h3</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Three</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h4</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Four</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h5</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Five</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h6</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Six</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Div.class))
				.verify(x -> verify(x.getChild(0)).isType(H1.class))
				.verify(x -> verify(x.getChild(1)).isType(H2.class))
				.verify(x -> verify(x.getChild(2)).isType(H3.class))
				.verify(x -> verify(x.getChild(3)).isType(H4.class))
				.verify(x -> verify(x.getChild(4)).isType(H5.class))
				.verify(x -> verify(x.getChild(5)).isType(H6.class))
			},
			{	/* 41 */
				new ComboInput<P>(
					"Hr",
					P.class,
					p(hr())
				)
				.json("{_type:'p',c:[{_type:'hr'}]}")
				.jsonT("{t:'p',c:[{t:'hr'}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'hr'\n\t\t}\n\t]\n}")
				.xml("<p><hr/></p>")
				.xmlT("<p><hr/></p>")
				.xmlR("<p><hr/></p>\n")
				.xmlNs("<p><hr/></p>")
				.html("<p><hr/></p>")
				.htmlT("<p><hr/></p>")
				.htmlR("<p><hr/></p>\n")
				.uon("(_type=p,c=@((_type=hr)))")
				.uonT("(t=p,c=@((t=hr)))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=hr\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=hr))")
				.urlEncT("t=p&c=@((t=hr))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=hr\n\t)\n)")
				.msgPack("82A55F74797065A170A1639181A55F74797065A26872")
				.msgPackT("82A174A170A1639181A174A26872")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>hr</jp:_type>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>hr</jp:t>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>hr</jp:_type>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Hr.class))
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
					)
				)
				.json("{_type:'html',c:[{_type:'head',c:[{_type:'title',c:'title'},{_type:'base',a:{href:'foo',target:'_blank'}},{_type:'link',a:{rel:'stylesheet',type:'text/css',href:'theme.css'}},{_type:'meta',a:{charset:'UTF-8'}}]},{_type:'body',c:['bar']}]}")
				.jsonT("{t:'html',c:[{t:'head',c:[{t:'title',c:'title'},{t:'base',a:{href:'foo',target:'_blank'}},{t:'link',a:{rel:'stylesheet',type:'text/css',href:'theme.css'}},{t:'meta',a:{charset:'UTF-8'}}]},{t:'body',c:['bar']}]}")
				.jsonR("{\n\t_type: 'html',\n\tc: [\n\t\t{\n\t\t\t_type: 'head',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'title',\n\t\t\t\t\tc: 'title'\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'base',\n\t\t\t\t\ta: {\n\t\t\t\t\t\thref: 'foo',\n\t\t\t\t\t\ttarget: '_blank'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'link',\n\t\t\t\t\ta: {\n\t\t\t\t\t\trel: 'stylesheet',\n\t\t\t\t\t\ttype: 'text/css',\n\t\t\t\t\t\thref: 'theme.css'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'meta',\n\t\t\t\t\ta: {\n\t\t\t\t\t\tcharset: 'UTF-8'\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'body',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>")
				.xmlT("<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>")
				.xmlR(
						"<html>\n"
						+"	<head>\n"
						+"		<title>title</title>\n"
						+"		<base href='foo' target='_blank'/>\n"
						+"		<link rel='stylesheet' type='text/css' href='theme.css'/>\n"
						+"		<meta charset='UTF-8'/>\n"
						+"	</head>\n"
						+"	<body>bar</body>\n"
						+"</html>\n")
				.xmlNs("<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>")
				.html("<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>")
				.htmlT("<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>")
				.htmlR(
						"<html>\n"
						+"	<head>\n"
						+"		<title>title</title>\n"
						+"		<base href='foo' target='_blank'/>\n"
						+"		<link rel='stylesheet' type='text/css' href='theme.css'/>\n"
						+"		<meta charset='UTF-8'/>\n"
						+"	</head>\n"
						+"	<body>bar</body>\n"
						+"</html>\n")
				.uon("(_type=html,c=@((_type=head,c=@((_type=title,c=title),(_type=base,a=(href=foo,target=_blank)),(_type=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(_type=meta,a=(charset=UTF-8)))),(_type=body,c=@(bar))))")
				.uonT("(t=html,c=@((t=head,c=@((t=title,c=title),(t=base,a=(href=foo,target=_blank)),(t=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(t=meta,a=(charset=UTF-8)))),(t=body,c=@(bar))))")
				.uonR("(\n\t_type=html,\n\tc=@(\n\t\t(\n\t\t\t_type=head,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=title,\n\t\t\t\t\tc=title\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=base,\n\t\t\t\t\ta=(\n\t\t\t\t\t\thref=foo,\n\t\t\t\t\t\ttarget=_blank\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=link,\n\t\t\t\t\ta=(\n\t\t\t\t\t\trel=stylesheet,\n\t\t\t\t\t\ttype=text/css,\n\t\t\t\t\t\thref=theme.css\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=meta,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tcharset=UTF-8\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=body,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=html&c=@((_type=head,c=@((_type=title,c=title),(_type=base,a=(href=foo,target=_blank)),(_type=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(_type=meta,a=(charset=UTF-8)))),(_type=body,c=@(bar)))")
				.urlEncT("t=html&c=@((t=head,c=@((t=title,c=title),(t=base,a=(href=foo,target=_blank)),(t=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(t=meta,a=(charset=UTF-8)))),(t=body,c=@(bar)))")
				.urlEncR("_type=html\n&c=@(\n\t(\n\t\t_type=head,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=title,\n\t\t\t\tc=title\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=base,\n\t\t\t\ta=(\n\t\t\t\t\thref=foo,\n\t\t\t\t\ttarget=_blank\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=link,\n\t\t\t\ta=(\n\t\t\t\t\trel=stylesheet,\n\t\t\t\t\ttype=text/css,\n\t\t\t\t\thref=theme.css\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=meta,\n\t\t\t\ta=(\n\t\t\t\t\tcharset=UTF-8\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=body,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A468746D6CA1639282A55F74797065A468656164A1639482A55F74797065A57469746C65A163A57469746C6582A55F74797065A462617365A16182A468726566A3666F6FA6746172676574A65F626C616E6B82A55F74797065A46C696E6BA16183A372656CAA7374796C657368656574A474797065A8746578742F637373A468726566A97468656D652E63737382A55F74797065A46D657461A16181A763686172736574A55554462D3882A55F74797065A4626F6479A16391A3626172")
				.msgPackT("82A174A468746D6CA1639282A174A468656164A1639482A174A57469746C65A163A57469746C6582A174A462617365A16182A468726566A3666F6FA6746172676574A65F626C616E6B82A174A46C696E6BA16183A372656CAA7374796C657368656574A474797065A8746578742F637373A468726566A97468656D652E63737382A174A46D657461A16181A763686172736574A55554462D3882A174A4626F6479A16391A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>html</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>head</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>title</jp:_type>\n<jp:c>title</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>base</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n<jp:target>_blank</jp:target>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>link</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:rel>stylesheet</jp:rel>\n<jp:type>text/css</jp:type>\n<jp:href>theme.css</jp:href>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>meta</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:charset>UTF-8</jp:charset>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>body</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>html</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>head</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>title</jp:t>\n<jp:c>title</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>base</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n<jp:target>_blank</jp:target>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>link</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:rel>stylesheet</jp:rel>\n<jp:type>text/css</jp:type>\n<jp:href>theme.css</jp:href>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>meta</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:charset>UTF-8</jp:charset>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>body</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>html</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>head</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>title</jp:_type>\n                <jp:c>title</jp:c>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>base</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:href>foo</jp:href>\n                  <jp:target>_blank</jp:target>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>link</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:rel>stylesheet</jp:rel>\n                  <jp:type>text/css</jp:type>\n                  <jp:href>theme.css</jp:href>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>meta</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:charset>UTF-8</jp:charset>\n                </jp:a>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>body</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Html.class))
				.verify(x -> verify(x.getChild(0)).isType(Head.class))
				.verify(x -> verify(x.getChild(0,0)).isType(Title.class))
				.verify(x -> verify(x.getChild(0,1)).isType(Base.class))
				.verify(x -> verify(x.getChild(0,2)).isType(Link.class))
				.verify(x -> verify(x.getChild(0,3)).isType(Meta.class))
				.verify(x -> verify(x.getChild(1)).isType(Body.class))
			},
			{	/* 43 */
				new ComboInput<P>(
					"I",
					P.class,
					p(i("foo"))
				)
				.json("{_type:'p',c:[{_type:'i',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'i',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'i',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><i>foo</i></p>")
				.xmlT("<p><i>foo</i></p>")
				.xmlR("<p><i>foo</i></p>\n")
				.xmlNs("<p><i>foo</i></p>")
				.html("<p><i>foo</i></p>")
				.htmlT("<p><i>foo</i></p>")
				.htmlR("<p><i>foo</i></p>\n")
				.uon("(_type=p,c=@((_type=i,c=@(foo))))")
				.uonT("(t=p,c=@((t=i,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=i,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=i,c=@(foo)))")
				.urlEncT("t=p&c=@((t=i,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=i,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A169A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A169A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>i</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>i</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>i</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(I.class))
			},
			{	/* 44 */
				new ComboInput<Iframe>(
					"Iframe",
					Iframe.class,
					iframe("foo")
				)
				.json("{_type:'iframe',c:['foo']}")
				.jsonT("{t:'iframe',c:['foo']}")
				.jsonR("{\n\t_type: 'iframe',\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<iframe>foo</iframe>")
				.xmlT("<iframe>foo</iframe>")
				.xmlR("<iframe>foo</iframe>\n")
				.xmlNs("<iframe>foo</iframe>")
				.html("<iframe>foo</iframe>")
				.htmlT("<iframe>foo</iframe>")
				.htmlR("<iframe>foo</iframe>\n")
				.uon("(_type=iframe,c=@(foo))")
				.uonT("(t=iframe,c=@(foo))")
				.uonR("(\n\t_type=iframe,\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=iframe&c=@(foo)")
				.urlEncT("t=iframe&c=@(foo)")
				.urlEncR("_type=iframe\n&c=@(\n\tfoo\n)")
				.msgPack("82A55F74797065A6696672616D65A16391A3666F6F")
				.msgPackT("82A174A6696672616D65A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>iframe</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>iframe</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>iframe</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Iframe.class))
			},
			{	/* 45 */
				new ComboInput<P>(
					"Kbd",
					P.class,
					p(kbd("foo"))
				)
				.json("{_type:'p',c:[{_type:'kbd',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'kbd',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'kbd',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><kbd>foo</kbd></p>")
				.xmlT("<p><kbd>foo</kbd></p>")
				.xmlR("<p><kbd>foo</kbd></p>\n")
				.xmlNs("<p><kbd>foo</kbd></p>")
				.html("<p><kbd>foo</kbd></p>")
				.htmlT("<p><kbd>foo</kbd></p>")
				.htmlR("<p><kbd>foo</kbd></p>\n")
				.uon("(_type=p,c=@((_type=kbd,c=@(foo))))")
				.uonT("(t=p,c=@((t=kbd,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=kbd,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=kbd,c=@(foo)))")
				.urlEncT("t=p&c=@((t=kbd,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=kbd,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A36B6264A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A36B6264A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>kbd</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>kbd</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>kbd</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Kbd.class))
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
					)
				)
				.json("{_type:'main',c:[{_type:'article',c:[{_type:'header',c:[{_type:'h1',c:['header1']},{_type:'p',c:['header2']}]},{_type:'p',c:['content']},{_type:'footer',c:[{_type:'h2',c:['footer1']},{_type:'p',c:['footer2']}]}]}]}")
				.jsonT("{t:'main',c:[{t:'article',c:[{t:'header',c:[{t:'h1',c:['header1']},{t:'p',c:['header2']}]},{t:'p',c:['content']},{t:'footer',c:[{t:'h2',c:['footer1']},{t:'p',c:['footer2']}]}]}]}")
				.jsonR("{\n\t_type: 'main',\n\tc: [\n\t\t{\n\t\t\t_type: 'article',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'header',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'h1',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'header1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'p',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'header2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'p',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'content'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'footer',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'h2',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'footer1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'p',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'footer2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>")
				.xmlT("<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>")
				.xmlR("<main>\n\t<article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article>\n</main>\n")
				.xmlNs("<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>")
				.html("<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>")
				.htmlT("<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>")
				.htmlR("<main>\n\t<article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article>\n</main>\n")
				.uon("(_type=main,c=@((_type=article,c=@((_type=header,c=@((_type=h1,c=@(header1)),(_type=p,c=@(header2)))),(_type=p,c=@(content)),(_type=footer,c=@((_type=h2,c=@(footer1)),(_type=p,c=@(footer2))))))))")
				.uonT("(t=main,c=@((t=article,c=@((t=header,c=@((t=h1,c=@(header1)),(t=p,c=@(header2)))),(t=p,c=@(content)),(t=footer,c=@((t=h2,c=@(footer1)),(t=p,c=@(footer2))))))))")
				.uonR("(\n\t_type=main,\n\tc=@(\n\t\t(\n\t\t\t_type=article,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=header,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=h1,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\theader1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\theader2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=p,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tcontent\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=footer,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=h2,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tfooter1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tfooter2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=main&c=@((_type=article,c=@((_type=header,c=@((_type=h1,c=@(header1)),(_type=p,c=@(header2)))),(_type=p,c=@(content)),(_type=footer,c=@((_type=h2,c=@(footer1)),(_type=p,c=@(footer2)))))))")
				.urlEncT("t=main&c=@((t=article,c=@((t=header,c=@((t=h1,c=@(header1)),(t=p,c=@(header2)))),(t=p,c=@(content)),(t=footer,c=@((t=h2,c=@(footer1)),(t=p,c=@(footer2)))))))")
				.urlEncR("_type=main\n&c=@(\n\t(\n\t\t_type=article,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=header,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=h1,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\theader1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\theader2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=p,\n\t\t\t\tc=@(\n\t\t\t\t\tcontent\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=footer,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=h2,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tfooter1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=p,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tfooter2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A46D61696EA1639182A55F74797065A761727469636C65A1639382A55F74797065A6686561646572A1639282A55F74797065A26831A16391A76865616465723182A55F74797065A170A16391A76865616465723282A55F74797065A170A16391A7636F6E74656E7482A55F74797065A6666F6F746572A1639282A55F74797065A26832A16391A7666F6F7465723182A55F74797065A170A16391A7666F6F74657232")
				.msgPackT("82A174A46D61696EA1639182A174A761727469636C65A1639382A174A6686561646572A1639282A174A26831A16391A76865616465723182A174A170A16391A76865616465723282A174A170A16391A7636F6E74656E7482A174A6666F6F746572A1639282A174A26832A16391A7666F6F7465723182A174A170A16391A7666F6F74657232")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>main</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>article</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>header</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>content</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>footer</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h2</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>main</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>article</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>header</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>header2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>content</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>footer</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h2</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>footer2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>main</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>article</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>header</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>h1</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>header1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>p</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>header2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>p</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>content</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>footer</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>h2</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>footer1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>p</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>footer2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Main.class))
				.verify(x -> verify(x.getChild(0)).isType(Article.class))
				.verify(x -> verify(x.getChild(0,0)).isType(Header.class))
				.verify(x -> verify(x.getChild(0,0,0)).isType(H1.class))
				.verify(x -> verify(x.getChild(0,0,1)).isType(P.class))
				.verify(x -> verify(x.getChild(0,1)).isType(P.class))
				.verify(x -> verify(x.getChild(0,2)).isType(Footer.class))
				.verify(x -> verify(x.getChild(0,2,0)).isType(H2.class))
				.verify(x -> verify(x.getChild(0,2,1)).isType(P.class))
			},
			{	/* 47 */
				new ComboInput<Map>(
					"Map/Area-1",
					Map.class,
					map(area("rect", "0,1,2,3", "foo").alt("bar")).name("baz")
				)
				.json("{_type:'map',a:{name:'baz'},c:[{_type:'area',a:{shape:'rect',coords:'0,1,2,3',href:'foo',alt:'bar'}}]}")
				.jsonT("{t:'map',a:{name:'baz'},c:[{t:'area',a:{shape:'rect',coords:'0,1,2,3',href:'foo',alt:'bar'}}]}")
				.jsonR("{\n\t_type: 'map',\n\ta: {\n\t\tname: 'baz'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'area',\n\t\t\ta: {\n\t\t\t\tshape: 'rect',\n\t\t\t\tcoords: '0,1,2,3',\n\t\t\t\thref: 'foo',\n\t\t\t\talt: 'bar'\n\t\t\t}\n\t\t}\n\t]\n}")
				.xml("<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>")
				.xmlT("<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>")
				.xmlR("<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n")
				.xmlNs("<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>")
				.html("<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>")
				.htmlT("<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>")
				.htmlR("<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n")
				.uon("(_type=map,a=(name=baz),c=@((_type=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar))))")
				.uonT("(t=map,a=(name=baz),c=@((t=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar))))")
				.uonR("(\n\t_type=map,\n\ta=(\n\t\tname=baz\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=area,\n\t\t\ta=(\n\t\t\t\tshape=rect,\n\t\t\t\tcoords='0,1,2,3',\n\t\t\t\thref=foo,\n\t\t\t\talt=bar\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=map&a=(name=baz)&c=@((_type=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar)))")
				.urlEncT("t=map&a=(name=baz)&c=@((t=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar)))")
				.urlEncR("_type=map\n&a=(\n\tname=baz\n)\n&c=@(\n\t(\n\t\t_type=area,\n\t\ta=(\n\t\t\tshape=rect,\n\t\t\tcoords='0,1,2,3',\n\t\t\thref=foo,\n\t\t\talt=bar\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A36D6170A16181A46E616D65A362617AA1639182A55F74797065A461726561A16184A57368617065A472656374A6636F6F726473A7302C312C322C33A468726566A3666F6FA3616C74A3626172")
				.msgPackT("83A174A36D6170A16181A46E616D65A362617AA1639182A174A461726561A16184A57368617065A472656374A6636F6F726473A7302C312C322C33A468726566A3666F6FA3616C74A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>map</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>baz</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>area</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:shape>rect</jp:shape>\n<jp:coords>0,1,2,3</jp:coords>\n<jp:href>foo</jp:href>\n<jp:alt>bar</jp:alt>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>map</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>baz</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>area</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:shape>rect</jp:shape>\n<jp:coords>0,1,2,3</jp:coords>\n<jp:href>foo</jp:href>\n<jp:alt>bar</jp:alt>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>map</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:name>baz</jp:name>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>area</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:shape>rect</jp:shape>\n            <jp:coords>0,1,2,3</jp:coords>\n            <jp:href>foo</jp:href>\n            <jp:alt>bar</jp:alt>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Map.class))
				.verify(x -> verify(x.getChild(0)).isType(Area.class))
			},
			{	/* 48 */
				new ComboInput<P>(
					"Mark",
					P.class,
					p(mark("foo"))
				)
				.json("{_type:'p',c:[{_type:'mark',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'mark',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'mark',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><mark>foo</mark></p>")
				.xmlT("<p><mark>foo</mark></p>")
				.xmlR("<p><mark>foo</mark></p>\n")
				.xmlNs("<p><mark>foo</mark></p>")
				.html("<p><mark>foo</mark></p>")
				.htmlT("<p><mark>foo</mark></p>")
				.htmlR("<p><mark>foo</mark></p>\n")
				.uon("(_type=p,c=@((_type=mark,c=@(foo))))")
				.uonT("(t=p,c=@((t=mark,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=mark,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=mark,c=@(foo)))")
				.urlEncT("t=p&c=@((t=mark,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=mark,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A46D61726BA16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A46D61726BA16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>mark</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>mark</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>mark</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Mark.class))
			},
			{	/* 49 */
				new ComboInput<Meter>(
					"Meter",
					Meter.class,
					meter("foo").value(1).min(0).max(2)
				)
				.json("{_type:'meter',a:{value:1,min:0,max:2},c:['foo']}")
				.jsonT("{t:'meter',a:{value:1,min:0,max:2},c:['foo']}")
				.jsonR("{\n\t_type: 'meter',\n\ta: {\n\t\tvalue: 1,\n\t\tmin: 0,\n\t\tmax: 2\n\t},\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<meter value='1' min='0' max='2'>foo</meter>")
				.xmlT("<meter value='1' min='0' max='2'>foo</meter>")
				.xmlR("<meter value='1' min='0' max='2'>foo</meter>\n")
				.xmlNs("<meter value='1' min='0' max='2'>foo</meter>")
				.html("<meter value='1' min='0' max='2'>foo</meter>")
				.htmlT("<meter value='1' min='0' max='2'>foo</meter>")
				.htmlR("<meter value='1' min='0' max='2'>foo</meter>\n")
				.uon("(_type=meter,a=(value=1,min=0,max=2),c=@(foo))")
				.uonT("(t=meter,a=(value=1,min=0,max=2),c=@(foo))")
				.uonR("(\n\t_type=meter,\n\ta=(\n\t\tvalue=1,\n\t\tmin=0,\n\t\tmax=2\n\t),\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=meter&a=(value=1,min=0,max=2)&c=@(foo)")
				.urlEncT("t=meter&a=(value=1,min=0,max=2)&c=@(foo)")
				.urlEncR("_type=meter\n&a=(\n\tvalue=1,\n\tmin=0,\n\tmax=2\n)\n&c=@(\n\tfoo\n)")
				.msgPack("83A55F74797065A56D65746572A16183A576616C756501A36D696E00A36D617802A16391A3666F6F")
				.msgPackT("83A174A56D65746572A16183A576616C756501A36D696E00A36D617802A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>meter</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n<jp:min>0</jp:min>\n<jp:max>2</jp:max>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>meter</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n<jp:min>0</jp:min>\n<jp:max>2</jp:max>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>meter</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:value>1</jp:value>\n      <jp:min>0</jp:min>\n      <jp:max>2</jp:max>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Meter.class))
			},
			{	/* 50 */
				new ComboInput<Nav>(
					"Nav",
					Nav.class,
					nav(a("foo","bar"))
				)
				.json("{_type:'nav',c:[{_type:'a',a:{href:'foo'},c:['bar']}]}")
				.jsonT("{t:'nav',c:[{t:'a',a:{href:'foo'},c:['bar']}]}")
				.jsonR("{\n\t_type: 'nav',\n\tc: [\n\t\t{\n\t\t\t_type: 'a',\n\t\t\ta: {\n\t\t\t\thref: 'foo'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<nav><a href='foo'>bar</a></nav>")
				.xmlT("<nav><a href='foo'>bar</a></nav>")
				.xmlR("<nav><a href='foo'>bar</a></nav>\n")
				.xmlNs("<nav><a href='foo'>bar</a></nav>")
				.html("<nav><a href='foo'>bar</a></nav>")
				.htmlT("<nav><a href='foo'>bar</a></nav>")
				.htmlR("<nav><a href='foo'>bar</a></nav>\n")
				.uon("(_type=nav,c=@((_type=a,a=(href=foo),c=@(bar))))")
				.uonT("(t=nav,c=@((t=a,a=(href=foo),c=@(bar))))")
				.uonR("(\n\t_type=nav,\n\tc=@(\n\t\t(\n\t\t\t_type=a,\n\t\t\ta=(\n\t\t\t\thref=foo\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=nav&c=@((_type=a,a=(href=foo),c=@(bar)))")
				.urlEncT("t=nav&c=@((t=a,a=(href=foo),c=@(bar)))")
				.urlEncR("_type=nav\n&c=@(\n\t(\n\t\t_type=a,\n\t\ta=(\n\t\t\thref=foo\n\t\t),\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A36E6176A1639183A55F74797065A161A16181A468726566A3666F6FA16391A3626172")
				.msgPackT("82A174A36E6176A1639183A174A161A16181A468726566A3666F6FA16391A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>nav</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>a</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>nav</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>a</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:href>foo</jp:href>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>nav</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>a</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:href>foo</jp:href>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Nav.class))
				.verify(x -> verify(x.getChild(0)).isType(A.class))
			},
			{	/* 51 */
				new ComboInput<Noscript>(
					"Noscript",
					Noscript.class,
					noscript("No script!")
				)
				.json("{_type:'noscript',c:['No script!']}")
				.jsonT("{t:'noscript',c:['No script!']}")
				.jsonR("{\n\t_type: 'noscript',\n\tc: [\n\t\t'No script!'\n\t]\n}")
				.xml("<noscript>No script!</noscript>")
				.xmlT("<noscript>No script!</noscript>")
				.xmlR("<noscript>No script!</noscript>\n")
				.xmlNs("<noscript>No script!</noscript>")
				.html("<noscript>No script!</noscript>")
				.htmlT("<noscript>No script!</noscript>")
				.htmlR("<noscript>No script!</noscript>\n")
				.uon("(_type=noscript,c=@('No script!'))")
				.uonT("(t=noscript,c=@('No script!'))")
				.uonR("(\n\t_type=noscript,\n\tc=@(\n\t\t'No script!'\n\t)\n)")
				.urlEnc("_type=noscript&c=@('No+script!')")
				.urlEncT("t=noscript&c=@('No+script!')")
				.urlEncR("_type=noscript\n&c=@(\n\t'No+script!'\n)")
				.msgPack("82A55F74797065A86E6F736372697074A16391AA4E6F2073637269707421")
				.msgPackT("82A174A86E6F736372697074A16391AA4E6F2073637269707421")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>noscript</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>No script!</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>noscript</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>No script!</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>noscript</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>No script!</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Noscript.class))
			},
			{	/* 52 */
				new ComboInput<Object_>(
					"Object/Param",
					Object_.class,
					object().width(1).height(2).data("foo.swf").child(param("autoplay",true))
				)
				.json("{_type:'object',a:{width:1,height:2,data:'foo.swf'},c:[{_type:'param',a:{name:'autoplay',value:true}}]}")
				.jsonT("{t:'object',a:{width:1,height:2,data:'foo.swf'},c:[{t:'param',a:{name:'autoplay',value:true}}]}")
				.jsonR("{\n\t_type: 'object',\n\ta: {\n\t\twidth: 1,\n\t\theight: 2,\n\t\tdata: 'foo.swf'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'param',\n\t\t\ta: {\n\t\t\t\tname: 'autoplay',\n\t\t\t\tvalue: true\n\t\t\t}\n\t\t}\n\t]\n}")
				.xml("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>")
				.xmlT("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>")
				.xmlR("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n")
				.xmlNs("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>")
				.html("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>")
				.htmlT("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>")
				.htmlR("<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n")
				.uon("(_type=object,a=(width=1,height=2,data=foo.swf),c=@((_type=param,a=(name=autoplay,value=true))))")
				.uonT("(t=object,a=(width=1,height=2,data=foo.swf),c=@((t=param,a=(name=autoplay,value=true))))")
				.uonR("(\n\t_type=object,\n\ta=(\n\t\twidth=1,\n\t\theight=2,\n\t\tdata=foo.swf\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=param,\n\t\t\ta=(\n\t\t\t\tname=autoplay,\n\t\t\t\tvalue=true\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=object&a=(width=1,height=2,data=foo.swf)&c=@((_type=param,a=(name=autoplay,value=true)))")
				.urlEncT("t=object&a=(width=1,height=2,data=foo.swf)&c=@((t=param,a=(name=autoplay,value=true)))")
				.urlEncR("_type=object\n&a=(\n\twidth=1,\n\theight=2,\n\tdata=foo.swf\n)\n&c=@(\n\t(\n\t\t_type=param,\n\t\ta=(\n\t\t\tname=autoplay,\n\t\t\tvalue=true\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A66F626A656374A16183A5776964746801A668656967687402A464617461A7666F6F2E737766A1639182A55F74797065A5706172616DA16182A46E616D65A86175746F706C6179A576616C7565C3")
				.msgPackT("83A174A66F626A656374A16183A5776964746801A668656967687402A464617461A7666F6F2E737766A1639182A174A5706172616DA16182A46E616D65A86175746F706C6179A576616C7565C3")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>object</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:width>1</jp:width>\n<jp:height>2</jp:height>\n<jp:data>foo.swf</jp:data>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>param</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>autoplay</jp:name>\n<jp:value>true</jp:value>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>object</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:width>1</jp:width>\n<jp:height>2</jp:height>\n<jp:data>foo.swf</jp:data>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>param</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>autoplay</jp:name>\n<jp:value>true</jp:value>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>object</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:width>1</jp:width>\n      <jp:height>2</jp:height>\n      <jp:data>foo.swf</jp:data>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>param</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:name>autoplay</jp:name>\n            <jp:value>true</jp:value>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Object_.class))
				.verify(x -> verify(x.getChild(0)).isType(Param.class))
			},
			{	/* 53 */
				new ComboInput<Ol>(
					"Ol/Li",
					Ol.class,
					ol(li("foo"))
				)
				.json("{_type:'ol',c:[{_type:'li',c:['foo']}]}")
				.jsonT("{t:'ol',c:[{t:'li',c:['foo']}]}")
				.jsonR("{\n\t_type: 'ol',\n\tc: [\n\t\t{\n\t\t\t_type: 'li',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<ol><li>foo</li></ol>")
				.xmlT("<ol><li>foo</li></ol>")
				.xmlR("<ol>\n\t<li>foo</li>\n</ol>\n")
				.xmlNs("<ol><li>foo</li></ol>")
				.html("<ol><li>foo</li></ol>")
				.htmlT("<ol><li>foo</li></ol>")
				.htmlR("<ol>\n\t<li>foo</li>\n</ol>\n")
				.uon("(_type=ol,c=@((_type=li,c=@(foo))))")
				.uonT("(t=ol,c=@((t=li,c=@(foo))))")
				.uonR("(\n\t_type=ol,\n\tc=@(\n\t\t(\n\t\t\t_type=li,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=ol&c=@((_type=li,c=@(foo)))")
				.urlEncT("t=ol&c=@((t=li,c=@(foo)))")
				.urlEncR("_type=ol\n&c=@(\n\t(\n\t\t_type=li,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A26F6CA1639182A55F74797065A26C69A16391A3666F6F")
				.msgPackT("82A174A26F6CA1639182A174A26C69A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>ol</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>li</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>ol</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>li</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>ol</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>li</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Ol.class))
				.verify(x -> verify(x.getChild(0)).isType(Li.class))
			},
			{	/* 54 */
				new ComboInput<Form>(
					"Output",
					Form.class,
					(Form)form("testform",
						0,input("range").id("a").value(50),
						"+",input("number").id("b").value(50),
						"=",output().name("x")._for("a b")
					).oninput("x.value=parseInt(a.value)+parseInt(b.value)")
				)
				.json("{_type:'form',a:{action:'testform',oninput:'x.value=parseInt(a.value)+parseInt(b.value)'},c:[0,{_type:'input',a:{type:'range',id:'a',value:50}},'+',{_type:'input',a:{type:'number',id:'b',value:50}},'=',{_type:'output',a:{name:'x','for':'a b'}}]}")
				.jsonT("{t:'form',a:{action:'testform',oninput:'x.value=parseInt(a.value)+parseInt(b.value)'},c:[0,{t:'input',a:{type:'range',id:'a',value:50}},'+',{t:'input',a:{type:'number',id:'b',value:50}},'=',{t:'output',a:{name:'x','for':'a b'}}]}")
				.jsonR("{\n\t_type: 'form',\n\ta: {\n\t\taction: 'testform',\n\t\toninput: 'x.value=parseInt(a.value)+parseInt(b.value)'\n\t},\n\tc: [\n\t\t0,\n\t\t{\n\t\t\t_type: 'input',\n\t\t\ta: {\n\t\t\t\ttype: 'range',\n\t\t\t\tid: 'a',\n\t\t\t\tvalue: 50\n\t\t\t}\n\t\t},\n\t\t'+',\n\t\t{\n\t\t\t_type: 'input',\n\t\t\ta: {\n\t\t\t\ttype: 'number',\n\t\t\t\tid: 'b',\n\t\t\t\tvalue: 50\n\t\t\t}\n\t\t},\n\t\t'=',\n\t\t{\n\t\t\t_type: 'output',\n\t\t\ta: {\n\t\t\t\tname: 'x',\n\t\t\t\t'for': 'a b'\n\t\t\t}\n\t\t}\n\t]\n}")
				.xml("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>")
				.xmlT("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>")
				.xmlR("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>\n")
				.xmlNs("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>")
				.html("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>")
				.htmlT("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>")
				.htmlR("<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b' nil='true'></output></form>\n")
				.uon("(_type=form,a=(action=testform,oninput='x.value=parseInt(a.value)+parseInt(b.value)'),c=@(0,(_type=input,a=(type=range,id=a,value=50)),+,(_type=input,a=(type=number,id=b,value=50)),'=',(_type=output,a=(name=x,for='a b'))))")
				.uonT("(t=form,a=(action=testform,oninput='x.value=parseInt(a.value)+parseInt(b.value)'),c=@(0,(t=input,a=(type=range,id=a,value=50)),+,(t=input,a=(type=number,id=b,value=50)),'=',(t=output,a=(name=x,for='a b'))))")
				.uonR("(\n\t_type=form,\n\ta=(\n\t\taction=testform,\n\t\toninput='x.value=parseInt(a.value)+parseInt(b.value)'\n\t),\n\tc=@(\n\t\t0,\n\t\t(\n\t\t\t_type=input,\n\t\t\ta=(\n\t\t\t\ttype=range,\n\t\t\t\tid=a,\n\t\t\t\tvalue=50\n\t\t\t)\n\t\t),\n\t\t+,\n\t\t(\n\t\t\t_type=input,\n\t\t\ta=(\n\t\t\t\ttype=number,\n\t\t\t\tid=b,\n\t\t\t\tvalue=50\n\t\t\t)\n\t\t),\n\t\t'=',\n\t\t(\n\t\t\t_type=output,\n\t\t\ta=(\n\t\t\t\tname=x,\n\t\t\t\tfor='a b'\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=form&a=(action=testform,oninput='x.value=parseInt(a.value)%2BparseInt(b.value)')&c=@(0,(_type=input,a=(type=range,id=a,value=50)),%2B,(_type=input,a=(type=number,id=b,value=50)),'=',(_type=output,a=(name=x,for='a+b')))")
				.urlEncT("t=form&a=(action=testform,oninput='x.value=parseInt(a.value)%2BparseInt(b.value)')&c=@(0,(t=input,a=(type=range,id=a,value=50)),%2B,(t=input,a=(type=number,id=b,value=50)),'=',(t=output,a=(name=x,for='a+b')))")
				.urlEncR("_type=form\n&a=(\n\taction=testform,\n\toninput='x.value=parseInt(a.value)%2BparseInt(b.value)'\n)\n&c=@(\n\t0,\n\t(\n\t\t_type=input,\n\t\ta=(\n\t\t\ttype=range,\n\t\t\tid=a,\n\t\t\tvalue=50\n\t\t)\n\t),\n\t%2B,\n\t(\n\t\t_type=input,\n\t\ta=(\n\t\t\ttype=number,\n\t\t\tid=b,\n\t\t\tvalue=50\n\t\t)\n\t),\n\t'=',\n\t(\n\t\t_type=output,\n\t\ta=(\n\t\t\tname=x,\n\t\t\tfor='a+b'\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A4666F726DA16182A6616374696F6EA874657374666F726DA76F6E696E707574D92B782E76616C75653D7061727365496E7428612E76616C7565292B7061727365496E7428622E76616C756529A163960082A55F74797065A5696E707574A16183A474797065A572616E6765A26964A161A576616C756532A12B82A55F74797065A5696E707574A16183A474797065A66E756D626572A26964A162A576616C756532A13D82A55F74797065A66F7574707574A16182A46E616D65A178A3666F72A3612062")
				.msgPackT("83A174A4666F726DA16182A6616374696F6EA874657374666F726DA76F6E696E707574D92B782E76616C75653D7061727365496E7428612E76616C7565292B7061727365496E7428622E76616C756529A163960082A174A5696E707574A16183A474797065A572616E6765A26964A161A576616C756532A12B82A174A5696E707574A16183A474797065A66E756D626572A26964A162A576616C756532A13D82A174A66F7574707574A16182A46E616D65A178A3666F72A3612062")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>form</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:action>testform</jp:action>\n<jp:oninput>x.value=parseInt(a.value)+parseInt(b.value)</jp:oninput>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>0</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>range</jp:type>\n<jp:id>a</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>+</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>input</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>number</jp:type>\n<jp:id>b</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>=</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>output</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>x</jp:name>\n<jp:for>a b</jp:for>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>form</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:action>testform</jp:action>\n<jp:oninput>x.value=parseInt(a.value)+parseInt(b.value)</jp:oninput>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>0</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>range</jp:type>\n<jp:id>a</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>+</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>input</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>number</jp:type>\n<jp:id>b</jp:id>\n<jp:value>50</jp:value>\n</jp:a>\n</rdf:li>\n<rdf:li>=</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>output</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>x</jp:name>\n<jp:for>a b</jp:for>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>form</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:action>testform</jp:action>\n      <jp:oninput>x.value=parseInt(a.value)+parseInt(b.value)</jp:oninput>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>0</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>input</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:type>range</jp:type>\n            <jp:id>a</jp:id>\n            <jp:value>50</jp:value>\n          </jp:a>\n        </rdf:li>\n        <rdf:li>+</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>input</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:type>number</jp:type>\n            <jp:id>b</jp:id>\n            <jp:value>50</jp:value>\n          </jp:a>\n        </rdf:li>\n        <rdf:li>=</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>output</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:name>x</jp:name>\n            <jp:for>a b</jp:for>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Form.class))
				.verify(x -> verify(x.getChild(1)).isType(Input.class))
				.verify(x -> verify(x.getChild(3)).isType(Input.class))
				.verify(x -> verify(x.getChild(5)).isType(Output.class))
			},
			{	/* 55 */
				new ComboInput<P>(
					"p",
					P.class,
					p("foo")
				)
				.json("{_type:'p',c:['foo']}")
				.jsonT("{t:'p',c:['foo']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<p>foo</p>")
				.xmlT("<p>foo</p>")
				.xmlR("<p>foo</p>\n")
				.xmlNs("<p>foo</p>")
				.html("<p>foo</p>")
				.htmlT("<p>foo</p>")
				.htmlR("<p>foo</p>\n")
				.uon("(_type=p,c=@(foo))")
				.uonT("(t=p,c=@(foo))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=p&c=@(foo)")
				.urlEncT("t=p&c=@(foo)")
				.urlEncR("_type=p\n&c=@(\n\tfoo\n)")
				.msgPack("82A55F74797065A170A16391A3666F6F")
				.msgPackT("82A174A170A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
			},
			{	/* 56 */
				new ComboInput<P[][]>(
					"P[][]",
					P[][].class,
					new P[][]{{p("a"),p("b")},{},{p("c")}}
				)
				.json("[[{_type:'p',c:['a']},{_type:'p',c:['b']}],[],[{_type:'p',c:['c']}]]")
				.jsonT("[[{t:'p',c:['a']},{t:'p',c:['b']}],[],[{t:'p',c:['c']}]]")
				.jsonR("[\n\t[\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'a'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'b'\n\t\t\t]\n\t\t}\n\t],\n\t[\n\t],\n\t[\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'c'\n\t\t\t]\n\t\t}\n\t]\n]")
				.xml("<array><array><p>a</p><p>b</p></array><array></array><array><p>c</p></array></array>")
				.xmlT("<array><array><p>a</p><p>b</p></array><array></array><array><p>c</p></array></array>")
				.xmlR("<array>\n\t<array>\n\t\t<p>a</p>\n\t\t<p>b</p>\n\t</array>\n\t<array>\n\t</array>\n\t<array>\n\t\t<p>c</p>\n\t</array>\n</array>\n")
				.xmlNs("<array><array><p>a</p><p>b</p></array><array></array><array><p>c</p></array></array>")
				.html("<ul><li><ul><li><p>a</p></li><li><p>b</p></li></ul></li><li><ul></ul></li><li><ul><li><p>c</p></li></ul></li></ul>")
				.htmlT("<ul><li><ul><li><p>a</p></li><li><p>b</p></li></ul></li><li><ul></ul></li><li><ul><li><p>c</p></li></ul></li></ul>")
				.htmlR("<ul>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<p>a</p>\n\t\t\t</li>\n\t\t\t<li>\n\t\t\t\t<p>b</p>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n\t<li>\n\t\t<ul></ul>\n\t</li>\n\t<li>\n\t\t<ul>\n\t\t\t<li>\n\t\t\t\t<p>c</p>\n\t\t\t</li>\n\t\t</ul>\n\t</li>\n</ul>\n")
				.uon("@(@((_type=p,c=@(a)),(_type=p,c=@(b))),@(),@((_type=p,c=@(c))))")
				.uonT("@(@((t=p,c=@(a)),(t=p,c=@(b))),@(),@((t=p,c=@(c))))")
				.uonR("@(\n\t@(\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\ta\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tb\n\t\t\t)\n\t\t)\n\t),\n\t@(),\n\t@(\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tc\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("0=@((_type=p,c=@(a)),(_type=p,c=@(b)))&1=@()&2=@((_type=p,c=@(c)))")
				.urlEncT("0=@((t=p,c=@(a)),(t=p,c=@(b)))&1=@()&2=@((t=p,c=@(c)))")
				.urlEncR("0=@(\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\ta\n\t\t)\n\t),\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tb\n\t\t)\n\t)\n)\n&1=@()\n&2=@(\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tc\n\t\t)\n\t)\n)")
				.msgPack("939282A55F74797065A170A16391A16182A55F74797065A170A16391A162909182A55F74797065A170A16391A163")
				.msgPackT("939282A174A170A16391A16182A174A170A16391A162909182A174A170A16391A163")
				.rdfXml("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Seq>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>a</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>b</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n<rdf:li>\n<rdf:Seq/>\n</rdf:li>\n<rdf:li>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</rdf:li>\n</rdf:Seq>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Seq>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>a</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>b</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq/>\n    </rdf:li>\n    <rdf:li>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>c</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </rdf:li>\n  </rdf:Seq>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P[][].class))
			},
			{	/* 57 */
				new ComboInput<Pre>(
					"Pre",
					Pre.class,
					pre("foo   \n   bar")
				)
				.json("{_type:'pre',c:['foo   \\n   bar']}")
				.jsonT("{t:'pre',c:['foo   \\n   bar']}")
				.jsonR("{\n\t_type: 'pre',\n\tc: [\n\t\t'foo   \\n   bar'\n\t]\n}")
				.xml("<pre>foo   &#x000a;   bar</pre>")
				.xmlT("<pre>foo   &#x000a;   bar</pre>")
				.xmlR("<pre>foo   &#x000a;   bar</pre>\n")
				.xmlNs("<pre>foo   &#x000a;   bar</pre>")
				.html("<pre>foo   \n   bar</pre>")
				.htmlT("<pre>foo   \n   bar</pre>")
				.htmlR("<pre>foo   \n   bar</pre>\n")
				.uon("(_type=pre,c=@('foo   \n   bar'))")
				.uonT("(t=pre,c=@('foo   \n   bar'))")
				.uonR("(\n\t_type=pre,\n\tc=@(\n\t\t'foo   \n   bar'\n\t)\n)")
				.urlEnc("_type=pre&c=@('foo+++%0A+++bar')")
				.urlEncT("t=pre&c=@('foo+++%0A+++bar')")
				.urlEncR("_type=pre\n&c=@(\n\t'foo+++%0A+++bar'\n)")
				.msgPack("82A55F74797065A3707265A16391AD666F6F2020200A202020626172")
				.msgPackT("82A174A3707265A16391AD666F6F2020200A202020626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>pre</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo   _x000A_   bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>pre</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo   _x000A_   bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>pre</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo   _x000A_   bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Pre.class))
			},
			{	/* 58 */
				new ComboInput<Progress>(
					"Progress",
					Progress.class,
					progress().value(1)
				)
				.json("{_type:'progress',a:{value:1}}")
				.jsonT("{t:'progress',a:{value:1}}")
				.jsonR("{\n\t_type: 'progress',\n\ta: {\n\t\tvalue: 1\n\t}\n}")
				.xml("<progress value='1' nil='true'></progress>")
				.xmlT("<progress value='1' nil='true'></progress>")
				.xmlR("<progress value='1' nil='true'>\n</progress>\n")
				.xmlNs("<progress value='1' nil='true'></progress>")
				.html("<progress value='1' nil='true'></progress>")
				.htmlT("<progress value='1' nil='true'></progress>")
				.htmlR("<progress value='1' nil='true'>\n</progress>\n")
				.uon("(_type=progress,a=(value=1))")
				.uonT("(t=progress,a=(value=1))")
				.uonR("(\n\t_type=progress,\n\ta=(\n\t\tvalue=1\n\t)\n)")
				.urlEnc("_type=progress&a=(value=1)")
				.urlEncT("t=progress&a=(value=1)")
				.urlEncR("_type=progress\n&a=(\n\tvalue=1\n)")
				.msgPack("82A55F74797065A870726F6772657373A16181A576616C756501")
				.msgPackT("82A174A870726F6772657373A16181A576616C756501")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>progress</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>progress</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:value>1</jp:value>\n</jp:a>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>progress</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:value>1</jp:value>\n    </jp:a>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Progress.class))
			},
			{	/* 59 */
				new ComboInput<P>(
					"Q",
					P.class,
					p("foo",q("bar"),"baz")
				)
				.json("{_type:'p',c:['foo',{_type:'q',c:['bar']},'baz']}")
				.jsonT("{t:'p',c:['foo',{t:'q',c:['bar']},'baz']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'q',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}")
				.xml("<p>foo<q>bar</q>baz</p>")
				.xmlT("<p>foo<q>bar</q>baz</p>")
				.xmlR("<p>foo<q>bar</q>baz</p>\n")
				.xmlNs("<p>foo<q>bar</q>baz</p>")
				.html("<p>foo<q>bar</q>baz</p>")
				.htmlT("<p>foo<q>bar</q>baz</p>")
				.htmlR("<p>foo<q>bar</q>baz</p>\n")
				.uon("(_type=p,c=@(foo,(_type=q,c=@(bar)),baz))")
				.uonT("(t=p,c=@(foo,(t=q,c=@(bar)),baz))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=q,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)")
				.urlEnc("_type=p&c=@(foo,(_type=q,c=@(bar)),baz)")
				.urlEncT("t=p&c=@(foo,(t=q,c=@(bar)),baz)")
				.urlEncR("_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=q,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\tbaz\n)")
				.msgPack("82A55F74797065A170A16393A3666F6F82A55F74797065A171A16391A3626172A362617A")
				.msgPackT("82A174A170A16393A3666F6F82A174A171A16391A3626172A362617A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>q</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>q</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>q</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Q.class))
			},
			{	/* 60 */
				new ComboInput<Ruby>(
					"Ruby/Rb/Rtc/Rp/Rt",
					Ruby.class,
					ruby(
						"法",rb("華"),"経",rtc("き",rp("け"),"ょ")
					)
				)
				.json("{_type:'ruby',c:['法',{_type:'rb',c:['華']},'経',{_type:'rtc',c:['き',{_type:'rp',c:['け']},'ょ']}]}")
				.jsonT("{t:'ruby',c:['法',{t:'rb',c:['華']},'経',{t:'rtc',c:['き',{t:'rp',c:['け']},'ょ']}]}")
				.jsonR("{\n\t_type: 'ruby',\n\tc: [\n\t\t'法',\n\t\t{\n\t\t\t_type: 'rb',\n\t\t\tc: [\n\t\t\t\t'華'\n\t\t\t]\n\t\t},\n\t\t'経',\n\t\t{\n\t\t\t_type: 'rtc',\n\t\t\tc: [\n\t\t\t\t'き',\n\t\t\t\t{\n\t\t\t\t\t_type: 'rp',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t'け'\n\t\t\t\t\t]\n\t\t\t\t},\n\t\t\t\t'ょ'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>")
				.xmlT("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>")
				.xmlR("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>\n")
				.xmlNs("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>")
				.html("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>")
				.htmlT("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>")
				.htmlR("<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>\n")
				.uon("(_type=ruby,c=@(法,(_type=rb,c=@(華)),経,(_type=rtc,c=@(き,(_type=rp,c=@(け)),ょ))))")
				.uonT("(t=ruby,c=@(法,(t=rb,c=@(華)),経,(t=rtc,c=@(き,(t=rp,c=@(け)),ょ))))")
				.uonR("(\n\t_type=ruby,\n\tc=@(\n\t\t法,\n\t\t(\n\t\t\t_type=rb,\n\t\t\tc=@(\n\t\t\t\t華\n\t\t\t)\n\t\t),\n\t\t経,\n\t\t(\n\t\t\t_type=rtc,\n\t\t\tc=@(\n\t\t\t\tき,\n\t\t\t\t(\n\t\t\t\t\t_type=rp,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\tけ\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\tょ\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=ruby&c=@(%E6%B3%95,(_type=rb,c=@(%E8%8F%AF)),%E7%B5%8C,(_type=rtc,c=@(%E3%81%8D,(_type=rp,c=@(%E3%81%91)),%E3%82%87)))")
				.urlEncT("t=ruby&c=@(%E6%B3%95,(t=rb,c=@(%E8%8F%AF)),%E7%B5%8C,(t=rtc,c=@(%E3%81%8D,(t=rp,c=@(%E3%81%91)),%E3%82%87)))")
				.urlEncR("_type=ruby\n&c=@(\n\t%E6%B3%95,\n\t(\n\t\t_type=rb,\n\t\tc=@(\n\t\t\t%E8%8F%AF\n\t\t)\n\t),\n\t%E7%B5%8C,\n\t(\n\t\t_type=rtc,\n\t\tc=@(\n\t\t\t%E3%81%8D,\n\t\t\t(\n\t\t\t\t_type=rp,\n\t\t\t\tc=@(\n\t\t\t\t\t%E3%81%91\n\t\t\t\t)\n\t\t\t),\n\t\t\t%E3%82%87\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A472756279A16394A3E6B39582A55F74797065A27262A16391A3E88FAFA3E7B58C82A55F74797065A3727463A16393A3E3818D82A55F74797065A27270A16391A3E38191A3E38287")
				.msgPackT("82A174A472756279A16394A3E6B39582A174A27262A16391A3E88FAFA3E7B58C82A174A3727463A16393A3E3818D82A174A27270A16391A3E38191A3E38287")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>ruby</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>法</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>rb</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>華</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>経</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>rtc</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>き</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>rp</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>け</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>ょ</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>ruby</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>法</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>rb</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>華</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>経</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>rtc</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>き</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>rp</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>け</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>ょ</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>ruby</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>法</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>rb</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>華</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>経</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>rtc</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>き</rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>rp</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li>け</rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n              <rdf:li>ょ</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Ruby.class))
				.verify(x -> verify(x.getChild(1)).isType(Rb.class))
				.verify(x -> verify(x.getChild(3)).isType(Rtc.class))
				.verify(x -> verify(x.getChild(3,1)).isType(Rp.class))
			},
			{	/* 61 */
				new ComboInput<P>(
					"S",
					P.class,
					p("foo",s("bar"),"baz")
				)
				.json("{_type:'p',c:['foo',{_type:'s',c:['bar']},'baz']}")
				.jsonT("{t:'p',c:['foo',{t:'s',c:['bar']},'baz']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 's',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t},\n\t\t'baz'\n\t]\n}")
				.xml("<p>foo<s>bar</s>baz</p>")
				.xmlT("<p>foo<s>bar</s>baz</p>")
				.xmlR("<p>foo<s>bar</s>baz</p>\n")
				.xmlNs("<p>foo<s>bar</s>baz</p>")
				.html("<p>foo<s>bar</s>baz</p>")
				.htmlT("<p>foo<s>bar</s>baz</p>")
				.htmlR("<p>foo<s>bar</s>baz</p>\n")
				.uon("(_type=p,c=@(foo,(_type=s,c=@(bar)),baz))")
				.uonT("(t=p,c=@(foo,(t=s,c=@(bar)),baz))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=s,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t),\n\t\tbaz\n\t)\n)")
				.urlEnc("_type=p&c=@(foo,(_type=s,c=@(bar)),baz)")
				.urlEncT("t=p&c=@(foo,(t=s,c=@(bar)),baz)")
				.urlEncR("_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=s,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t),\n\tbaz\n)")
				.msgPack("82A55F74797065A170A16393A3666F6F82A55F74797065A173A16391A3626172A362617A")
				.msgPackT("82A174A170A16393A3666F6F82A174A173A16391A3626172A362617A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>s</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>s</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>baz</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>s</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>baz</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(S.class))
			},
			{	/* 62 */
				new ComboInput<Samp>(
					"Samp",
					Samp.class,
					samp("foo")
				)
				.json("{_type:'samp',c:['foo']}")
				.jsonT("{t:'samp',c:['foo']}")
				.jsonR("{\n\t_type: 'samp',\n\tc: [\n\t\t'foo'\n\t]\n}")
				.xml("<samp>foo</samp>")
				.xmlT("<samp>foo</samp>")
				.xmlR("<samp>foo</samp>\n")
				.xmlNs("<samp>foo</samp>")
				.html("<samp>foo</samp>")
				.htmlT("<samp>foo</samp>")
				.htmlR("<samp>foo</samp>\n")
				.uon("(_type=samp,c=@(foo))")
				.uonT("(t=samp,c=@(foo))")
				.uonR("(\n\t_type=samp,\n\tc=@(\n\t\tfoo\n\t)\n)")
				.urlEnc("_type=samp&c=@(foo)")
				.urlEncT("t=samp&c=@(foo)")
				.urlEncR("_type=samp\n&c=@(\n\tfoo\n)")
				.msgPack("82A55F74797065A473616D70A16391A3666F6F")
				.msgPackT("82A174A473616D70A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>samp</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>samp</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>samp</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Samp.class))
			},
			{	/* 63 */
				new ComboInput<Script>(
					"Script",
					Script.class,
					script("text/javascript", new String[]{"\n\talert('hello world!');\n"})
				)
				.json("{_type:'script',a:{type:'text/javascript'},c:'\\n\\talert(\\'hello world!\\');\\n'}")
				.jsonT("{t:'script',a:{type:'text/javascript'},c:'\\n\\talert(\\'hello world!\\');\\n'}")
				.jsonR("{\n\t_type: 'script',\n\ta: {\n\t\ttype: 'text/javascript'\n\t},\n\tc: '\\n\\talert(\\'hello world!\\');\\n'\n}")
				.xml("<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>")
				.xmlT("<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>")
				.xmlR("<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>\n")
				.xmlNs("<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>")
				.html("<script type='text/javascript'>\n\talert('hello world!');\n</script>")
				.htmlT("<script type='text/javascript'>\n\talert('hello world!');\n</script>")
				.htmlR("<script type='text/javascript'>\n\talert('hello world!');\n</script>\n")
				.uon("(_type=script,a=(type=text/javascript),c='\n\talert(~'hello world!~');\n')")
				.uonT("(t=script,a=(type=text/javascript),c='\n\talert(~'hello world!~');\n')")
				.uonR("(\n\t_type=script,\n\ta=(\n\t\ttype=text/javascript\n\t),\n\tc='\n\talert(~'hello world!~');\n'\n)")
				.urlEnc("_type=script&a=(type=text/javascript)&c='%0A%09alert(~'hello+world!~');%0A'")
				.urlEncT("t=script&a=(type=text/javascript)&c='%0A%09alert(~'hello+world!~');%0A'")
				.urlEncR("_type=script\n&a=(\n\ttype=text/javascript\n)\n&c='%0A%09alert(~'hello+world!~');%0A'")
				.msgPack("83A55F74797065A6736372697074A16181A474797065AF746578742F6A617661736372697074A163B90A09616C657274282768656C6C6F20776F726C642127293B0A")
				.msgPackT("83A174A6736372697074A16181A474797065AF746578742F6A617661736372697074A163B90A09616C657274282768656C6C6F20776F726C642127293B0A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>script</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text/javascript</jp:type>\n</jp:a>\n<jp:c>_x000A__x0009_alert('hello world!');_x000A_</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>script</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:type>text/javascript</jp:type>\n</jp:a>\n<jp:c>_x000A__x0009_alert('hello world!');_x000A_</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>script</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:type>text/javascript</jp:type>\n    </jp:a>\n    <jp:c>_x000A__x0009_alert('hello world!');_x000A_</jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Script.class))
			},
			{	/* 64 */
				new ComboInput<Section>(
					"Section",
					Section.class,
					section(h1("foo"),p("bar"))
				)
				.json("{_type:'section',c:[{_type:'h1',c:['foo']},{_type:'p',c:['bar']}]}")
				.jsonT("{t:'section',c:[{t:'h1',c:['foo']},{t:'p',c:['bar']}]}")
				.jsonR("{\n\t_type: 'section',\n\tc: [\n\t\t{\n\t\t\t_type: 'h1',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'p',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<section><h1>foo</h1><p>bar</p></section>")
				.xmlT("<section><h1>foo</h1><p>bar</p></section>")
				.xmlR("<section><h1>foo</h1><p>bar</p></section>\n")
				.xmlNs("<section><h1>foo</h1><p>bar</p></section>")
				.html("<section><h1>foo</h1><p>bar</p></section>")
				.htmlT("<section><h1>foo</h1><p>bar</p></section>")
				.htmlR("<section><h1>foo</h1><p>bar</p></section>\n")
				.uon("(_type=section,c=@((_type=h1,c=@(foo)),(_type=p,c=@(bar))))")
				.uonT("(t=section,c=@((t=h1,c=@(foo)),(t=p,c=@(bar))))")
				.uonR("(\n\t_type=section,\n\tc=@(\n\t\t(\n\t\t\t_type=h1,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=p,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=section&c=@((_type=h1,c=@(foo)),(_type=p,c=@(bar)))")
				.urlEncT("t=section&c=@((t=h1,c=@(foo)),(t=p,c=@(bar)))")
				.urlEncR("_type=section\n&c=@(\n\t(\n\t\t_type=h1,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t),\n\t(\n\t\t_type=p,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A773656374696F6EA1639282A55F74797065A26831A16391A3666F6F82A55F74797065A170A16391A3626172")
				.msgPackT("82A174A773656374696F6EA1639282A174A26831A16391A3666F6F82A174A170A16391A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>section</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>h1</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>section</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>h1</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>section</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>h1</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>p</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Section.class))
				.verify(x -> verify(x.getChild(0)).isType(H1.class))
				.verify(x -> verify(x.getChild(1)).isType(P.class))
			},
			{	/* 65 */
				new ComboInput<Select>(
					"Select/Optgroup/Option",
					Select.class,
					select("foo", optgroup(option("o1","v1")).label("bar"))
				)
				.json("{_type:'select',a:{name:'foo'},c:[{_type:'optgroup',a:{label:'bar'},c:[{_type:'option',a:{value:'o1'},c:'v1'}]}]}")
				.jsonT("{t:'select',a:{name:'foo'},c:[{t:'optgroup',a:{label:'bar'},c:[{t:'option',a:{value:'o1'},c:'v1'}]}]}")
				.jsonR("{\n\t_type: 'select',\n\ta: {\n\t\tname: 'foo'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'optgroup',\n\t\t\ta: {\n\t\t\t\tlabel: 'bar'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'option',\n\t\t\t\t\ta: {\n\t\t\t\t\t\tvalue: 'o1'\n\t\t\t\t\t},\n\t\t\t\t\tc: 'v1'\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>")
				.xmlT("<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>")
				.xmlR("<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n")
				.xmlNs("<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>")
				.html("<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>")
				.htmlT("<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>")
				.htmlR("<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n")
				.uon("(_type=select,a=(name=foo),c=@((_type=optgroup,a=(label=bar),c=@((_type=option,a=(value=o1),c=v1)))))")
				.uonT("(t=select,a=(name=foo),c=@((t=optgroup,a=(label=bar),c=@((t=option,a=(value=o1),c=v1)))))")
				.uonR("(\n\t_type=select,\n\ta=(\n\t\tname=foo\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=optgroup,\n\t\t\ta=(\n\t\t\t\tlabel=bar\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=option,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tvalue=o1\n\t\t\t\t\t),\n\t\t\t\t\tc=v1\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=select&a=(name=foo)&c=@((_type=optgroup,a=(label=bar),c=@((_type=option,a=(value=o1),c=v1))))")
				.urlEncT("t=select&a=(name=foo)&c=@((t=optgroup,a=(label=bar),c=@((t=option,a=(value=o1),c=v1))))")
				.urlEncR("_type=select\n&a=(\n\tname=foo\n)\n&c=@(\n\t(\n\t\t_type=optgroup,\n\t\ta=(\n\t\t\tlabel=bar\n\t\t),\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=option,\n\t\t\t\ta=(\n\t\t\t\t\tvalue=o1\n\t\t\t\t),\n\t\t\t\tc=v1\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A673656C656374A16181A46E616D65A3666F6FA1639183A55F74797065A86F707467726F7570A16181A56C6162656CA3626172A1639183A55F74797065A66F7074696F6EA16181A576616C7565A26F31A163A27631")
				.msgPackT("83A174A673656C656374A16181A46E616D65A3666F6FA1639183A174A86F707467726F7570A16181A56C6162656CA3626172A1639183A174A66F7074696F6EA16181A576616C7565A26F31A163A27631")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>select</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>optgroup</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:label>bar</jp:label>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>option</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:value>o1</jp:value>\n</jp:a>\n<jp:c>v1</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>select</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>optgroup</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:label>bar</jp:label>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>option</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:value>o1</jp:value>\n</jp:a>\n<jp:c>v1</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>select</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:name>foo</jp:name>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>optgroup</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:label>bar</jp:label>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>option</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:value>o1</jp:value>\n                </jp:a>\n                <jp:c>v1</jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Select.class))
				.verify(x -> verify(x.getChild(0)).isType(Optgroup.class))
				.verify(x -> verify(x.getChild(0,0)).isType(Option.class))
			},
			{	/* 66 */
				new ComboInput<P>(
					"Small",
					P.class,
					p(small("foo"))
				)
				.json("{_type:'p',c:[{_type:'small',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'small',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'small',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><small>foo</small></p>")
				.xmlT("<p><small>foo</small></p>")
				.xmlR("<p><small>foo</small></p>\n")
				.xmlNs("<p><small>foo</small></p>")
				.html("<p><small>foo</small></p>")
				.htmlT("<p><small>foo</small></p>")
				.htmlR("<p><small>foo</small></p>\n")
				.uon("(_type=p,c=@((_type=small,c=@(foo))))")
				.uonT("(t=p,c=@((t=small,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=small,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=small,c=@(foo)))")
				.urlEncT("t=p&c=@((t=small,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=small,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A5736D616C6CA16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A5736D616C6CA16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>small</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>small</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>small</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Small.class))
			},
			{	/* 67 */
				new ComboInput<P>(
					"Span",
					P.class,
					p("My mother has ",span().style("color:blue").child("blue"), " eyes.")
				)
				.json("{_type:'p',c:['My mother has ',{_type:'span',a:{style:'color:blue'},c:['blue']},' eyes.']}")
				.jsonT("{t:'p',c:['My mother has ',{t:'span',a:{style:'color:blue'},c:['blue']},' eyes.']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'My mother has ',\n\t\t{\n\t\t\t_type: 'span',\n\t\t\ta: {\n\t\t\t\tstyle: 'color:blue'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'blue'\n\t\t\t]\n\t\t},\n\t\t' eyes.'\n\t]\n}")
				.xml("<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>")
				.xmlT("<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>")
				.xmlR("<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>\n")
				.xmlNs("<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>")
				.html("<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>")
				.htmlT("<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>")
				.htmlR("<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>\n")
				.uon("(_type=p,c=@('My mother has ',(_type=span,a=(style=color:blue),c=@(blue)),' eyes.'))")
				.uonT("(t=p,c=@('My mother has ',(t=span,a=(style=color:blue),c=@(blue)),' eyes.'))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t'My mother has ',\n\t\t(\n\t\t\t_type=span,\n\t\t\ta=(\n\t\t\t\tstyle=color:blue\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\tblue\n\t\t\t)\n\t\t),\n\t\t' eyes.'\n\t)\n)")
				.urlEnc("_type=p&c=@('My+mother+has+',(_type=span,a=(style=color:blue),c=@(blue)),'+eyes.')")
				.urlEncT("t=p&c=@('My+mother+has+',(t=span,a=(style=color:blue),c=@(blue)),'+eyes.')")
				.urlEncR("_type=p\n&c=@(\n\t'My+mother+has+',\n\t(\n\t\t_type=span,\n\t\ta=(\n\t\t\tstyle=color:blue\n\t\t),\n\t\tc=@(\n\t\t\tblue\n\t\t)\n\t),\n\t'+eyes.'\n)")
				.msgPack("82A55F74797065A170A16393AE4D79206D6F74686572206861732083A55F74797065A47370616EA16181A57374796C65AA636F6C6F723A626C7565A16391A4626C7565A620657965732E")
				.msgPackT("82A174A170A16393AE4D79206D6F74686572206861732083A174A47370616EA16181A57374796C65AA636F6C6F723A626C7565A16391A4626C7565A620657965732E")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>My mother has_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>span</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:style>color:blue</jp:style>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>blue</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>_x0020_eyes.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>My mother has_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>span</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:style>color:blue</jp:style>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>blue</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>_x0020_eyes.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>My mother has_x0020_</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>span</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:style>color:blue</jp:style>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>blue</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>_x0020_eyes.</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Span.class))
			},
			{	/* 68 */
				new ComboInput<P>(
					"Strong",
					P.class,
					p(strong("foo"))
				)
				.json("{_type:'p',c:[{_type:'strong',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'strong',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'strong',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><strong>foo</strong></p>")
				.xmlT("<p><strong>foo</strong></p>")
				.xmlR("<p><strong>foo</strong></p>\n")
				.xmlNs("<p><strong>foo</strong></p>")
				.html("<p><strong>foo</strong></p>")
				.htmlT("<p><strong>foo</strong></p>")
				.htmlR("<p><strong>foo</strong></p>\n")
				.uon("(_type=p,c=@((_type=strong,c=@(foo))))")
				.uonT("(t=p,c=@((t=strong,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=strong,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=strong,c=@(foo)))")
				.urlEncT("t=p&c=@((t=strong,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=strong,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A67374726F6E67A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A67374726F6E67A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>strong</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>strong</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>strong</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Strong.class))
			},
			{	/* 69 */
				new ComboInput<Head>(
					"Style",
					Head.class,
					head(style("\n\th1 {color:red;}\n\tp: {color:blue;}\n"))
				)
				.json("{_type:'head',c:[{_type:'style',c:'\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'}]}")
				.jsonT("{t:'head',c:[{t:'style',c:'\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'}]}")
				.jsonR("{\n\t_type: 'head',\n\tc: [\n\t\t{\n\t\t\t_type: 'style',\n\t\t\tc: '\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'\n\t\t}\n\t]\n}")
				.xml("<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>")
				.xmlT("<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>")
				.xmlR("<head>\n\t<style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style>\n</head>\n")
				.xmlNs("<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>")
				.html("<head><style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style></head>")
				.htmlT("<head><style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style></head>")
				.htmlR("<head>\n\t<style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style>\n</head>\n")
				.uon("(_type=head,c=@((_type=style,c='\n\th1 {color:red;}\n\tp: {color:blue;}\n')))")
				.uonT("(t=head,c=@((t=style,c='\n\th1 {color:red;}\n\tp: {color:blue;}\n')))")
				.uonR("(\n\t_type=head,\n\tc=@(\n\t\t(\n\t\t\t_type=style,\n\t\t\tc='\n\th1 {color:red;}\n\tp: {color:blue;}\n'\n\t\t)\n\t)\n)")
				.urlEnc("_type=head&c=@((_type=style,c='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'))")
				.urlEncT("t=head&c=@((t=style,c='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'))")
				.urlEncR("_type=head\n&c=@(\n\t(\n\t\t_type=style,\n\t\tc='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'\n\t)\n)")
				.msgPack("82A55F74797065A468656164A1639182A55F74797065A57374796C65A163D9240A096831207B636F6C6F723A7265643B7D0A09703A207B636F6C6F723A626C75653B7D0A")
				.msgPackT("82A174A468656164A1639182A174A57374796C65A163D9240A096831207B636F6C6F723A7265643B7D0A09703A207B636F6C6F723A626C75653B7D0A")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>head</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>style</jp:_type>\n<jp:c>_x000A__x0009_h1 {color:red;}_x000A__x0009_p: {color:blue;}_x000A_</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>head</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>style</jp:t>\n<jp:c>_x000A__x0009_h1 {color:red;}_x000A__x0009_p: {color:blue;}_x000A_</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>head</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>style</jp:_type>\n          <jp:c>_x000A__x0009_h1 {color:red;}_x000A__x0009_p: {color:blue;}_x000A_</jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Head.class))
				.verify(x -> verify(x.getChild(0)).isType(Style.class))
			},
			{	/* 70 */
				new ComboInput<P>(
					"Sub",
					P.class,
					p(sub("foo"))
				)
				.json("{_type:'p',c:[{_type:'sub',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'sub',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'sub',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><sub>foo</sub></p>")
				.xmlT("<p><sub>foo</sub></p>")
				.xmlR("<p><sub>foo</sub></p>\n")
				.xmlNs("<p><sub>foo</sub></p>")
				.html("<p><sub>foo</sub></p>")
				.htmlT("<p><sub>foo</sub></p>")
				.htmlR("<p><sub>foo</sub></p>\n")
				.uon("(_type=p,c=@((_type=sub,c=@(foo))))")
				.uonT("(t=p,c=@((t=sub,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=sub,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=sub,c=@(foo)))")
				.urlEncT("t=p&c=@((t=sub,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=sub,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A3737562A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A3737562A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>sub</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>sub</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>sub</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Sub.class))
			},
			{	/* 71 */
				new ComboInput<P>(
					"Sup",
					P.class,
					p(sup("foo"))
				)
				.json("{_type:'p',c:[{_type:'sup',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'sup',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'sup',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><sup>foo</sup></p>")
				.xmlT("<p><sup>foo</sup></p>")
				.xmlR("<p><sup>foo</sup></p>\n")
				.xmlNs("<p><sup>foo</sup></p>")
				.html("<p><sup>foo</sup></p>")
				.htmlT("<p><sup>foo</sup></p>")
				.htmlR("<p><sup>foo</sup></p>\n")
				.uon("(_type=p,c=@((_type=sup,c=@(foo))))")
				.uonT("(t=p,c=@((t=sup,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=sup,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=sup,c=@(foo)))")
				.urlEncT("t=p&c=@((t=sup,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=sup,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A3737570A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A3737570A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>sup</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>sup</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>sup</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Sup.class))
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
					)
				)
				.json("{_type:'table',c:[{_type:'caption',c:['caption1']},{_type:'colgroup',c:[{_type:'col',a:{'class':'foo'}},{_type:'col',a:{'class':'bar'}}]},{_type:'thead',c:[{_type:'tr',c:[{_type:'th',c:['c1']},{_type:'th',c:['c2']}]}]},{_type:'tbody',c:[{_type:'tr',c:[{_type:'td',c:['v1']},{_type:'td',c:['v2']}]}]},{_type:'tfoot',c:[{_type:'tr',c:[{_type:'td',c:['f1']},{_type:'td',c:['f2']}]}]}]}")
				.jsonT("{t:'table',c:[{t:'caption',c:['caption1']},{t:'colgroup',c:[{t:'col',a:{'class':'foo'}},{t:'col',a:{'class':'bar'}}]},{t:'thead',c:[{t:'tr',c:[{t:'th',c:['c1']},{t:'th',c:['c2']}]}]},{t:'tbody',c:[{t:'tr',c:[{t:'td',c:['v1']},{t:'td',c:['v2']}]}]},{t:'tfoot',c:[{t:'tr',c:[{t:'td',c:['f1']},{t:'td',c:['f2']}]}]}]}")
				.jsonR("{\n\t_type: 'table',\n\tc: [\n\t\t{\n\t\t\t_type: 'caption',\n\t\t\tc: [\n\t\t\t\t'caption1'\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'colgroup',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'col',\n\t\t\t\t\ta: {\n\t\t\t\t\t\t'class': 'foo'\n\t\t\t\t\t}\n\t\t\t\t},\n\t\t\t\t{\n\t\t\t\t\t_type: 'col',\n\t\t\t\t\ta: {\n\t\t\t\t\t\t'class': 'bar'\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'thead',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'tr',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'th',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'c1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'th',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'c2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'tbody',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'tr',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'v1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'v2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t},\n\t\t{\n\t\t\t_type: 'tfoot',\n\t\t\tc: [\n\t\t\t\t{\n\t\t\t\t\t_type: 'tr',\n\t\t\t\t\tc: [\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'f1'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t{\n\t\t\t\t\t\t\t_type: 'td',\n\t\t\t\t\t\t\tc: [\n\t\t\t\t\t\t\t\t'f2'\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t]\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml(
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
						+"</table>")
				.xmlT(
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
						+"</table>")
				.xmlR(
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
						+"</table>\n")
				.xmlNs(
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
						+"</table>")
				.html(
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
						+"</table>")
				.htmlT(
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
						+"</table>")
				.htmlR(
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
						+"</table>\n")
				.uon("(_type=table,c=@((_type=caption,c=@(caption1)),(_type=colgroup,c=@((_type=col,a=(class=foo)),(_type=col,a=(class=bar)))),(_type=thead,c=@((_type=tr,c=@((_type=th,c=@(c1)),(_type=th,c=@(c2)))))),(_type=tbody,c=@((_type=tr,c=@((_type=td,c=@(v1)),(_type=td,c=@(v2)))))),(_type=tfoot,c=@((_type=tr,c=@((_type=td,c=@(f1)),(_type=td,c=@(f2))))))))")
				.uonT("(t=table,c=@((t=caption,c=@(caption1)),(t=colgroup,c=@((t=col,a=(class=foo)),(t=col,a=(class=bar)))),(t=thead,c=@((t=tr,c=@((t=th,c=@(c1)),(t=th,c=@(c2)))))),(t=tbody,c=@((t=tr,c=@((t=td,c=@(v1)),(t=td,c=@(v2)))))),(t=tfoot,c=@((t=tr,c=@((t=td,c=@(f1)),(t=td,c=@(f2))))))))")
				.uonR("(\n\t_type=table,\n\tc=@(\n\t\t(\n\t\t\t_type=caption,\n\t\t\tc=@(\n\t\t\t\tcaption1\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=colgroup,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=col,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tclass=foo\n\t\t\t\t\t)\n\t\t\t\t),\n\t\t\t\t(\n\t\t\t\t\t_type=col,\n\t\t\t\t\ta=(\n\t\t\t\t\t\tclass=bar\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=thead,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=tr,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tc1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tc2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=tbody,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=tr,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tv1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tv2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=tfoot,\n\t\t\tc=@(\n\t\t\t\t(\n\t\t\t\t\t_type=tr,\n\t\t\t\t\tc=@(\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tf1\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t),\n\t\t\t\t\t\t(\n\t\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\t\tf2\n\t\t\t\t\t\t\t)\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=table&c=@((_type=caption,c=@(caption1)),(_type=colgroup,c=@((_type=col,a=(class=foo)),(_type=col,a=(class=bar)))),(_type=thead,c=@((_type=tr,c=@((_type=th,c=@(c1)),(_type=th,c=@(c2)))))),(_type=tbody,c=@((_type=tr,c=@((_type=td,c=@(v1)),(_type=td,c=@(v2)))))),(_type=tfoot,c=@((_type=tr,c=@((_type=td,c=@(f1)),(_type=td,c=@(f2)))))))")
				.urlEncT("t=table&c=@((t=caption,c=@(caption1)),(t=colgroup,c=@((t=col,a=(class=foo)),(t=col,a=(class=bar)))),(t=thead,c=@((t=tr,c=@((t=th,c=@(c1)),(t=th,c=@(c2)))))),(t=tbody,c=@((t=tr,c=@((t=td,c=@(v1)),(t=td,c=@(v2)))))),(t=tfoot,c=@((t=tr,c=@((t=td,c=@(f1)),(t=td,c=@(f2)))))))")
				.urlEncR("_type=table\n&c=@(\n\t(\n\t\t_type=caption,\n\t\tc=@(\n\t\t\tcaption1\n\t\t)\n\t),\n\t(\n\t\t_type=colgroup,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=col,\n\t\t\t\ta=(\n\t\t\t\t\tclass=foo\n\t\t\t\t)\n\t\t\t),\n\t\t\t(\n\t\t\t\t_type=col,\n\t\t\t\ta=(\n\t\t\t\t\tclass=bar\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=thead,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=tr,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tc1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=th,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tc2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=tbody,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=tr,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tv1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tv2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t),\n\t(\n\t\t_type=tfoot,\n\t\tc=@(\n\t\t\t(\n\t\t\t\t_type=tr,\n\t\t\t\tc=@(\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tf1\n\t\t\t\t\t\t)\n\t\t\t\t\t),\n\t\t\t\t\t(\n\t\t\t\t\t\t_type=td,\n\t\t\t\t\t\tc=@(\n\t\t\t\t\t\t\tf2\n\t\t\t\t\t\t)\n\t\t\t\t\t)\n\t\t\t\t)\n\t\t\t)\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A57461626C65A1639582A55F74797065A763617074696F6EA16391A863617074696F6E3182A55F74797065A8636F6C67726F7570A1639282A55F74797065A3636F6CA16181A5636C617373A3666F6F82A55F74797065A3636F6CA16181A5636C617373A362617282A55F74797065A57468656164A1639182A55F74797065A27472A1639282A55F74797065A27468A16391A2633182A55F74797065A27468A16391A2633282A55F74797065A574626F6479A1639182A55F74797065A27472A1639282A55F74797065A27464A16391A2763182A55F74797065A27464A16391A2763282A55F74797065A574666F6F74A1639182A55F74797065A27472A1639282A55F74797065A27464A16391A2663182A55F74797065A27464A16391A26632")
				.msgPackT("82A174A57461626C65A1639582A174A763617074696F6EA16391A863617074696F6E3182A174A8636F6C67726F7570A1639282A174A3636F6CA16181A5636C617373A3666F6F82A174A3636F6CA16181A5636C617373A362617282A174A57468656164A1639182A174A27472A1639282A174A27468A16391A2633182A174A27468A16391A2633282A174A574626F6479A1639182A174A27472A1639282A174A27464A16391A2763182A174A27464A16391A2763282A174A574666F6F74A1639182A174A27472A1639282A174A27464A16391A2663182A174A27464A16391A26632")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>table</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>caption</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>caption1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>colgroup</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>col</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:class>foo</jp:class>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>col</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:class>bar</jp:class>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>thead</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>th</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>th</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tbody</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tfoot</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>tr</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>td</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>table</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>caption</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>caption1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>colgroup</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>col</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:class>foo</jp:class>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>col</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:class>bar</jp:class>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>thead</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>th</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>th</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>c2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tbody</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>v2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tfoot</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>tr</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f1</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>td</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>f2</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>table</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>caption</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>caption1</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>colgroup</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>col</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:class>foo</jp:class>\n                </jp:a>\n              </rdf:li>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>col</jp:_type>\n                <jp:a rdf:parseType='Resource'>\n                  <jp:class>bar</jp:class>\n                </jp:a>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>thead</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>tr</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>th</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>c1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>th</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>c2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>tbody</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>tr</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>v1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>v2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>tfoot</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li rdf:parseType='Resource'>\n                <jp:_type>tr</jp:_type>\n                <jp:c>\n                  <rdf:Seq>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>f1</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                    <rdf:li rdf:parseType='Resource'>\n                      <jp:_type>td</jp:_type>\n                      <jp:c>\n                        <rdf:Seq>\n                          <rdf:li>f2</rdf:li>\n                        </rdf:Seq>\n                      </jp:c>\n                    </rdf:li>\n                  </rdf:Seq>\n                </jp:c>\n              </rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Table.class))
				.verify(x -> verify(x.getChild(0)).isType(Caption.class))
				.verify(x -> verify(x.getChild(1)).isType(Colgroup.class))
				.verify(x -> verify(x.getChild(1,0)).isType(Col.class))
				.verify(x -> verify(x.getChild(1,1)).isType(Col.class))
				.verify(x -> verify(x.getChild(2)).isType(Thead.class))
				.verify(x -> verify(x.getChild(2,0)).isType(Tr.class))
				.verify(x -> verify(x.getChild(2,0,0)).isType(Th.class))
				.verify(x -> verify(x.getChild(2,0,1)).isType(Th.class))
				.verify(x -> verify(x.getChild(3)).isType(Tbody.class))
				.verify(x -> verify(x.getChild(3,0)).isType(Tr.class))
				.verify(x -> verify(x.getChild(3,0,0)).isType(Td.class))
				.verify(x -> verify(x.getChild(3,0,1)).isType(Td.class))
				.verify(x -> verify(x.getChild(4)).isType(Tfoot.class))
				.verify(x -> verify(x.getChild(4,0)).isType(Tr.class))
				.verify(x -> verify(x.getChild(4,0,0)).isType(Td.class))
				.verify(x -> verify(x.getChild(4,0,1)).isType(Td.class))
			},
			{	/* 73 */
				new ComboInput<Template>(
					"Template",
					Template.class,
					template("foo",div("bar"))
				)
				.json("{_type:'template',a:{id:'foo'},c:[{_type:'div',c:['bar']}]}")
				.jsonT("{t:'template',a:{id:'foo'},c:[{t:'div',c:['bar']}]}")
				.jsonR("{\n\t_type: 'template',\n\ta: {\n\t\tid: 'foo'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'div',\n\t\t\tc: [\n\t\t\t\t'bar'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<template id='foo'><div>bar</div></template>")
				.xmlT("<template id='foo'><div>bar</div></template>")
				.xmlR("<template id='foo'><div>bar</div></template>\n")
				.xmlNs("<template id='foo'><div>bar</div></template>")
				.html("<template id='foo'><div>bar</div></template>")
				.htmlT("<template id='foo'><div>bar</div></template>")
				.htmlR("<template id='foo'><div>bar</div></template>\n")
				.uon("(_type=template,a=(id=foo),c=@((_type=div,c=@(bar))))")
				.uonT("(t=template,a=(id=foo),c=@((t=div,c=@(bar))))")
				.uonR("(\n\t_type=template,\n\ta=(\n\t\tid=foo\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=div,\n\t\t\tc=@(\n\t\t\t\tbar\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=template&a=(id=foo)&c=@((_type=div,c=@(bar)))")
				.urlEncT("t=template&a=(id=foo)&c=@((t=div,c=@(bar)))")
				.urlEncR("_type=template\n&a=(\n\tid=foo\n)\n&c=@(\n\t(\n\t\t_type=div,\n\t\tc=@(\n\t\t\tbar\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A874656D706C617465A16181A26964A3666F6FA1639182A55F74797065A3646976A16391A3626172")
				.msgPackT("83A174A874656D706C617465A16181A26964A3666F6FA1639182A174A3646976A16391A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>template</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>div</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>template</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:id>foo</jp:id>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>div</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>template</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:id>foo</jp:id>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>div</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>bar</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Template.class))
				.verify(x -> verify(x.getChild(0)).isType(Div.class))
			},
			{	/* 74 */
				new ComboInput<Textarea>(
					"Textarea",
					Textarea.class,
					textarea("foo", "bar")
				)
				.json("{_type:'textarea',a:{name:'foo'},c:'bar'}")
				.jsonT("{t:'textarea',a:{name:'foo'},c:'bar'}")
				.jsonR("{\n\t_type: 'textarea',\n\ta: {\n\t\tname: 'foo'\n\t},\n\tc: 'bar'\n}")
				.xml("<textarea name='foo'>bar</textarea>")
				.xmlT("<textarea name='foo'>bar</textarea>")
				.xmlR("<textarea name='foo'>bar</textarea>\n")
				.xmlNs("<textarea name='foo'>bar</textarea>")
				.html("<textarea name='foo'>bar</textarea>")
				.htmlT("<textarea name='foo'>bar</textarea>")
				.htmlR("<textarea name='foo'>bar</textarea>\n")
				.uon("(_type=textarea,a=(name=foo),c=bar)")
				.uonT("(t=textarea,a=(name=foo),c=bar)")
				.uonR("(\n\t_type=textarea,\n\ta=(\n\t\tname=foo\n\t),\n\tc=bar\n)")
				.urlEnc("_type=textarea&a=(name=foo)&c=bar")
				.urlEncT("t=textarea&a=(name=foo)&c=bar")
				.urlEncR("_type=textarea\n&a=(\n\tname=foo\n)\n&c=bar")
				.msgPack("83A55F74797065A87465787461726561A16181A46E616D65A3666F6FA163A3626172")
				.msgPackT("83A174A87465787461726561A16181A46E616D65A3666F6FA163A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>textarea</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>bar</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>textarea</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:name>foo</jp:name>\n</jp:a>\n<jp:c>bar</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>textarea</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:name>foo</jp:name>\n    </jp:a>\n    <jp:c>bar</jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Textarea.class))
			},
			{	/* 75 */
				new ComboInput<P>(
					"Time",
					P.class,
					p("I have a date on ",time("Valentines day").datetime("2016-02-14 18:00"), ".")
				)
				.json("{_type:'p',c:['I have a date on ',{_type:'time',a:{datetime:'2016-02-14 18:00'},c:['Valentines day']},'.']}")
				.jsonT("{t:'p',c:['I have a date on ',{t:'time',a:{datetime:'2016-02-14 18:00'},c:['Valentines day']},'.']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'I have a date on ',\n\t\t{\n\t\t\t_type: 'time',\n\t\t\ta: {\n\t\t\t\tdatetime: '2016-02-14 18:00'\n\t\t\t},\n\t\t\tc: [\n\t\t\t\t'Valentines day'\n\t\t\t]\n\t\t},\n\t\t'.'\n\t]\n}")
				.xml("<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>")
				.xmlT("<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>")
				.xmlR("<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n")
				.xmlNs("<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>")
				.html("<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>")
				.htmlT("<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>")
				.htmlR("<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n")
				.uon("(_type=p,c=@('I have a date on ',(_type=time,a=(datetime='2016-02-14 18:00'),c=@('Valentines day')),.))")
				.uonT("(t=p,c=@('I have a date on ',(t=time,a=(datetime='2016-02-14 18:00'),c=@('Valentines day')),.))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t'I have a date on ',\n\t\t(\n\t\t\t_type=time,\n\t\t\ta=(\n\t\t\t\tdatetime='2016-02-14 18:00'\n\t\t\t),\n\t\t\tc=@(\n\t\t\t\t'Valentines day'\n\t\t\t)\n\t\t),\n\t\t.\n\t)\n)")
				.urlEnc("_type=p&c=@('I+have+a+date+on+',(_type=time,a=(datetime='2016-02-14+18:00'),c=@('Valentines+day')),.)")
				.urlEncT("t=p&c=@('I+have+a+date+on+',(t=time,a=(datetime='2016-02-14+18:00'),c=@('Valentines+day')),.)")
				.urlEncR("_type=p\n&c=@(\n\t'I+have+a+date+on+',\n\t(\n\t\t_type=time,\n\t\ta=(\n\t\t\tdatetime='2016-02-14+18:00'\n\t\t),\n\t\tc=@(\n\t\t\t'Valentines+day'\n\t\t)\n\t),\n\t.\n)")
				.msgPack("82A55F74797065A170A16393B149206861766520612064617465206F6E2083A55F74797065A474696D65A16181A86461746574696D65B0323031362D30322D31342031383A3030A16391AE56616C656E74696E657320646179A12E")
				.msgPackT("82A174A170A16393B149206861766520612064617465206F6E2083A174A474696D65A16181A86461746574696D65B0323031362D30322D31342031383A3030A16391AE56616C656E74696E657320646179A12E")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>I have a date on_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>time</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:datetime>2016-02-14 18:00</jp:datetime>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Valentines day</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>I have a date on_x0020_</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>time</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:datetime>2016-02-14 18:00</jp:datetime>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li>Valentines day</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n<rdf:li>.</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>I have a date on_x0020_</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>time</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:datetime>2016-02-14 18:00</jp:datetime>\n          </jp:a>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>Valentines day</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n        <rdf:li>.</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Time.class))
			},
			{	/* 76 */
				new ComboInput<P>(
					"U",
					P.class,
					p(u("foo"))
				)
				.json("{_type:'p',c:[{_type:'u',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'u',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'u',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><u>foo</u></p>")
				.xmlT("<p><u>foo</u></p>")
				.xmlR("<p><u>foo</u></p>\n")
				.xmlNs("<p><u>foo</u></p>")
				.html("<p><u>foo</u></p>")
				.htmlT("<p><u>foo</u></p>")
				.htmlR("<p><u>foo</u></p>\n")
				.uon("(_type=p,c=@((_type=u,c=@(foo))))")
				.uonT("(t=p,c=@((t=u,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=u,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=u,c=@(foo)))")
				.urlEncT("t=p&c=@((t=u,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=u,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A175A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A175A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>u</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>u</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>u</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(U.class))
			},
			{	/* 77 */
				new ComboInput<Ul>(
					"Ul/Li",
					Ul.class,
					ul(li("foo"))
				)
				.json("{_type:'ul',c:[{_type:'li',c:['foo']}]}")
				.jsonT("{t:'ul',c:[{t:'li',c:['foo']}]}")
				.jsonR("{\n\t_type: 'ul',\n\tc: [\n\t\t{\n\t\t\t_type: 'li',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<ul><li>foo</li></ul>")
				.xmlT("<ul><li>foo</li></ul>")
				.xmlR("<ul>\n\t<li>foo</li>\n</ul>\n")
				.xmlNs("<ul><li>foo</li></ul>")
				.html("<ul><li>foo</li></ul>")
				.htmlT("<ul><li>foo</li></ul>")
				.htmlR("<ul>\n\t<li>foo</li>\n</ul>\n")
				.uon("(_type=ul,c=@((_type=li,c=@(foo))))")
				.uonT("(t=ul,c=@((t=li,c=@(foo))))")
				.uonR("(\n\t_type=ul,\n\tc=@(\n\t\t(\n\t\t\t_type=li,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=ul&c=@((_type=li,c=@(foo)))")
				.urlEncT("t=ul&c=@((t=li,c=@(foo)))")
				.urlEncR("_type=ul\n&c=@(\n\t(\n\t\t_type=li,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A2756CA1639182A55F74797065A26C69A16391A3666F6F")
				.msgPackT("82A174A2756CA1639182A174A26C69A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>ul</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>li</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>ul</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>li</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>ul</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>li</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Ul.class))
				.verify(x -> verify(x.getChild(0)).isType(Li.class))
			},
			{	/* 78 */
				new ComboInput<P>(
					"Var",
					P.class,
					p(var("foo"))
				)
				.json("{_type:'p',c:[{_type:'var',c:['foo']}]}")
				.jsonT("{t:'p',c:[{t:'var',c:['foo']}]}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t{\n\t\t\t_type: 'var',\n\t\t\tc: [\n\t\t\t\t'foo'\n\t\t\t]\n\t\t}\n\t]\n}")
				.xml("<p><var>foo</var></p>")
				.xmlT("<p><var>foo</var></p>")
				.xmlR("<p><var>foo</var></p>\n")
				.xmlNs("<p><var>foo</var></p>")
				.html("<p><var>foo</var></p>")
				.htmlT("<p><var>foo</var></p>")
				.htmlR("<p><var>foo</var></p>\n")
				.uon("(_type=p,c=@((_type=var,c=@(foo))))")
				.uonT("(t=p,c=@((t=var,c=@(foo))))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\t(\n\t\t\t_type=var,\n\t\t\tc=@(\n\t\t\t\tfoo\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=p&c=@((_type=var,c=@(foo)))")
				.urlEncT("t=p&c=@((t=var,c=@(foo)))")
				.urlEncR("_type=p\n&c=@(\n\t(\n\t\t_type=var,\n\t\tc=@(\n\t\t\tfoo\n\t\t)\n\t)\n)")
				.msgPack("82A55F74797065A170A1639182A55F74797065A3766172A16391A3666F6F")
				.msgPackT("82A174A170A1639182A174A3766172A16391A3666F6F")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>var</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>var</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>var</jp:_type>\n          <jp:c>\n            <rdf:Seq>\n              <rdf:li>foo</rdf:li>\n            </rdf:Seq>\n          </jp:c>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(0)).isType(Var.class))
			},
			{	/* 79 */
				new ComboInput<Video>(
					"Video/Source/Track",
					Video.class,
					video().width(100).height(200).controls(true).children(
						source("foo.mp4", "video/mp4"),
						track("subtitles_en.vtt", "subtitles").srclang("en")
					)
				)
				.json("{_type:'video',a:{width:100,height:200,controls:'controls'},c:[{_type:'source',a:{src:'foo.mp4',type:'video/mp4'}},{_type:'track',a:{src:'subtitles_en.vtt',kind:'subtitles',srclang:'en'}}]}")
				.jsonT("{t:'video',a:{width:100,height:200,controls:'controls'},c:[{t:'source',a:{src:'foo.mp4',type:'video/mp4'}},{t:'track',a:{src:'subtitles_en.vtt',kind:'subtitles',srclang:'en'}}]}")
				.jsonR("{\n\t_type: 'video',\n\ta: {\n\t\twidth: 100,\n\t\theight: 200,\n\t\tcontrols: 'controls'\n\t},\n\tc: [\n\t\t{\n\t\t\t_type: 'source',\n\t\t\ta: {\n\t\t\t\tsrc: 'foo.mp4',\n\t\t\t\ttype: 'video/mp4'\n\t\t\t}\n\t\t},\n\t\t{\n\t\t\t_type: 'track',\n\t\t\ta: {\n\t\t\t\tsrc: 'subtitles_en.vtt',\n\t\t\t\tkind: 'subtitles',\n\t\t\t\tsrclang: 'en'\n\t\t\t}\n\t\t}\n\t]\n}")
				.xml("<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>")
				.xmlT("<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>")
				.xmlR("<video width='100' height='200' controls='controls'>\n\t<source src='foo.mp4' type='video/mp4'/>\n\t<track src='subtitles_en.vtt' kind='subtitles' srclang='en'/>\n</video>\n")
				.xmlNs("<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>")
				.html("<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>")
				.htmlT("<video width='100' height='200' controls='controls'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>")
				.htmlR("<video width='100' height='200' controls='controls'>\n\t<source src='foo.mp4' type='video/mp4'/>\n\t<track src='subtitles_en.vtt' kind='subtitles' srclang='en'/>\n</video>\n")
				.uon("(_type=video,a=(width=100,height=200,controls=controls),c=@((_type=source,a=(src=foo.mp4,type=video/mp4)),(_type=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en))))")
				.uonT("(t=video,a=(width=100,height=200,controls=controls),c=@((t=source,a=(src=foo.mp4,type=video/mp4)),(t=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en))))")
				.uonR("(\n\t_type=video,\n\ta=(\n\t\twidth=100,\n\t\theight=200,\n\t\tcontrols=controls\n\t),\n\tc=@(\n\t\t(\n\t\t\t_type=source,\n\t\t\ta=(\n\t\t\t\tsrc=foo.mp4,\n\t\t\t\ttype=video/mp4\n\t\t\t)\n\t\t),\n\t\t(\n\t\t\t_type=track,\n\t\t\ta=(\n\t\t\t\tsrc=subtitles_en.vtt,\n\t\t\t\tkind=subtitles,\n\t\t\t\tsrclang=en\n\t\t\t)\n\t\t)\n\t)\n)")
				.urlEnc("_type=video&a=(width=100,height=200,controls=controls)&c=@((_type=source,a=(src=foo.mp4,type=video/mp4)),(_type=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en)))")
				.urlEncT("t=video&a=(width=100,height=200,controls=controls)&c=@((t=source,a=(src=foo.mp4,type=video/mp4)),(t=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en)))")
				.urlEncR("_type=video\n&a=(\n\twidth=100,\n\theight=200,\n\tcontrols=controls\n)\n&c=@(\n\t(\n\t\t_type=source,\n\t\ta=(\n\t\t\tsrc=foo.mp4,\n\t\t\ttype=video/mp4\n\t\t)\n\t),\n\t(\n\t\t_type=track,\n\t\ta=(\n\t\t\tsrc=subtitles_en.vtt,\n\t\t\tkind=subtitles,\n\t\t\tsrclang=en\n\t\t)\n\t)\n)")
				.msgPack("83A55F74797065A5766964656FA16183A5776964746864A6686569676874D100C8A8636F6E74726F6C73A8636F6E74726F6C73A1639282A55F74797065A6736F75726365A16182A3737263A7666F6F2E6D7034A474797065A9766964656F2F6D703482A55F74797065A5747261636BA16183A3737263B07375627469746C65735F656E2E767474A46B696E64A97375627469746C6573A77372636C616E67A2656E")
				.msgPackT("83A174A5766964656FA16183A5776964746864A6686569676874D100C8A8636F6E74726F6C73A8636F6E74726F6C73A1639282A174A6736F75726365A16182A3737263A7666F6F2E6D7034A474797065A9766964656F2F6D703482A174A5747261636BA16183A3737263B07375627469746C65735F656E2E767474A46B696E64A97375627469746C6573A77372636C616E67A2656E")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>video</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>source</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp4</jp:src>\n<jp:type>video/mp4</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>track</jp:_type>\n<jp:a rdf:parseType='Resource'>\n<jp:src>subtitles_en.vtt</jp:src>\n<jp:kind>subtitles</jp:kind>\n<jp:srclang>en</jp:srclang>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>video</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:width>100</jp:width>\n<jp:height>200</jp:height>\n<jp:controls>controls</jp:controls>\n</jp:a>\n<jp:c>\n<rdf:Seq>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>source</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>foo.mp4</jp:src>\n<jp:type>video/mp4</jp:type>\n</jp:a>\n</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>track</jp:t>\n<jp:a rdf:parseType='Resource'>\n<jp:src>subtitles_en.vtt</jp:src>\n<jp:kind>subtitles</jp:kind>\n<jp:srclang>en</jp:srclang>\n</jp:a>\n</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>video</jp:_type>\n    <jp:a rdf:parseType='Resource'>\n      <jp:width>100</jp:width>\n      <jp:height>200</jp:height>\n      <jp:controls>controls</jp:controls>\n    </jp:a>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>source</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>foo.mp4</jp:src>\n            <jp:type>video/mp4</jp:type>\n          </jp:a>\n        </rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>track</jp:_type>\n          <jp:a rdf:parseType='Resource'>\n            <jp:src>subtitles_en.vtt</jp:src>\n            <jp:kind>subtitles</jp:kind>\n            <jp:srclang>en</jp:srclang>\n          </jp:a>\n        </rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(Video.class))
				.verify(x -> verify(x.getChild(0)).isType(Source.class))
				.verify(x -> verify(x.getChild(1)).isType(Track.class))
			},
			{	/* 80 */
				new ComboInput<P>(
					"Wbr",
					P.class,
					p("foo",wbr(),"bar")
				)
				.json("{_type:'p',c:['foo',{_type:'wbr'},'bar']}")
				.jsonT("{t:'p',c:['foo',{t:'wbr'},'bar']}")
				.jsonR("{\n\t_type: 'p',\n\tc: [\n\t\t'foo',\n\t\t{\n\t\t\t_type: 'wbr'\n\t\t},\n\t\t'bar'\n\t]\n}")
				.xml("<p>foo<wbr/>bar</p>")
				.xmlT("<p>foo<wbr/>bar</p>")
				.xmlR("<p>foo<wbr/>bar</p>\n")
				.xmlNs("<p>foo<wbr/>bar</p>")
				.html("<p>foo<wbr/>bar</p>")
				.htmlT("<p>foo<wbr/>bar</p>")
				.htmlR("<p>foo<wbr/>bar</p>\n")
				.uon("(_type=p,c=@(foo,(_type=wbr),bar))")
				.uonT("(t=p,c=@(foo,(t=wbr),bar))")
				.uonR("(\n\t_type=p,\n\tc=@(\n\t\tfoo,\n\t\t(\n\t\t\t_type=wbr\n\t\t),\n\t\tbar\n\t)\n)")
				.urlEnc("_type=p&c=@(foo,(_type=wbr),bar)")
				.urlEncT("t=p&c=@(foo,(t=wbr),bar)")
				.urlEncR("_type=p\n&c=@(\n\tfoo,\n\t(\n\t\t_type=wbr\n\t),\n\tbar\n)")
				.msgPack("82A55F74797065A170A16393A3666F6F81A55F74797065A3776272A3626172")
				.msgPackT("82A174A170A16393A3666F6F81A174A3776272A3626172")
				.rdfXml("<rdf:RDF>\n<rdf:Description>\n<jp:_type>p</jp:_type>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:_type>wbr</jp:_type>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlT("<rdf:RDF>\n<rdf:Description>\n<jp:t>p</jp:t>\n<jp:c>\n<rdf:Seq>\n<rdf:li>foo</rdf:li>\n<rdf:li rdf:parseType='Resource'>\n<jp:t>wbr</jp:t>\n</rdf:li>\n<rdf:li>bar</rdf:li>\n</rdf:Seq>\n</jp:c>\n</rdf:Description>\n</rdf:RDF>\n")
				.rdfXmlR("<rdf:RDF>\n  <rdf:Description>\n    <jp:_type>p</jp:_type>\n    <jp:c>\n      <rdf:Seq>\n        <rdf:li>foo</rdf:li>\n        <rdf:li rdf:parseType='Resource'>\n          <jp:_type>wbr</jp:_type>\n        </rdf:li>\n        <rdf:li>bar</rdf:li>\n      </rdf:Seq>\n    </jp:c>\n  </rdf:Description>\n</rdf:RDF>\n")
				.verify(x -> verify(x).isType(P.class))
				.verify(x -> verify(x.getChild(1)).isType(Wbr.class))
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
			b.f3 = alist(a,a);
			return b;
		}
	}
}
