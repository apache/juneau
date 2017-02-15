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

import static org.junit.Assert.*;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.msgpack.MsgPackParser;
import org.apache.juneau.msgpack.MsgPackSerializer;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.UonParser;
import org.apache.juneau.urlencoding.UonSerializer;
import org.apache.juneau.urlencoding.UrlEncodingParser;
import org.apache.juneau.urlencoding.UrlEncodingSerializer;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;


@RunWith(Parameterized.class)
@SuppressWarnings({"javadoc"})
public class BasicHtmlSchemaTest {

	private static final WriterSerializer
		sXmlSq = XmlSerializer.DEFAULT_SQ,
		sXmlNsSq = XmlSerializer.DEFAULT_NS_SQ,
		sXmlSqReadable = XmlSerializer.DEFAULT_SQ_READABLE,
		sHtmlSq = HtmlSerializer.DEFAULT_SQ,
		sHtmlSqReadable = HtmlSerializer.DEFAULT_SQ_READABLE,
		sJson = JsonSerializer.DEFAULT_LAX,
		sJsonT = JsonSerializer.DEFAULT_LAX.clone().setBeanTypePropertyName("t"),
		sUon = UonSerializer.DEFAULT,
		sUonT = UonSerializer.DEFAULT.clone().setBeanTypePropertyName("t"),
		sUrlEncoding = UrlEncodingSerializer.DEFAULT,
		sUrlEncodingT = UrlEncodingSerializer.DEFAULT.clone().setBeanTypePropertyName("t");
	
	private static final OutputStreamSerializer 
		sMsgPack = MsgPackSerializer.DEFAULT,
		sMsgPackT = MsgPackSerializer.DEFAULT.clone().setBeanTypePropertyName("t");
	
	private static final ReaderParser
		pXml = XmlParser.DEFAULT,
		pHtml = HtmlParser.DEFAULT,
		pJson = JsonParser.DEFAULT,
		pJsonT = JsonParser.DEFAULT.clone().setBeanTypePropertyName("t"),
		pUon = UonParser.DEFAULT,
		pUonT = UonParser.DEFAULT.clone().setBeanTypePropertyName("t"),
		pUrlEncoding = UrlEncodingParser.DEFAULT,
		pUrlEncodingT = UrlEncodingParser.DEFAULT.clone().setBeanTypePropertyName("t");

	private static final InputStreamParser 
		pMsgPack = MsgPackParser.DEFAULT,
		pMsgPackT = MsgPackParser.DEFAULT.clone().setBeanTypePropertyName("t");
	
	private static final B btag = b("bbb");

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{
				"A-1",
				a("http://foo", "bar"),
				"<a href='http://foo'>bar</a>",
				"<a href='http://foo'>bar</a>\n",
				"<a href='http://foo'>bar</a>",
				"<a href='http://foo'>bar</a>\n",
				"{_type:'a',a:{href:'http://foo'},c:['bar']}",
				"{t:'a',a:{href:'http://foo'},c:['bar']}",
				"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A3626172",
				"83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A3626172",
				"(_type=a,a=(href=http://foo),c=@(bar))",
				"(t=a,a=(href=http://foo),c=@(bar))",
				"_type=a&a=(href=http://foo)&c=@(bar)",
				"t=a&a=(href=http://foo)&c=@(bar)",
			},
			{
				"A-2",
				a("http://foo", "bar", btag, "baz"),
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
				"{_type:'a',a:{href:'http://foo'},c:['bar',{_type:'b',c:['bbb']},'baz']}",
				"{t:'a',a:{href:'http://foo'},c:['bar',{t:'b',c:['bbb']},'baz']}",
				"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16393A362617282A55F74797065A162A16391A3626262A362617A",
				"83A174A161A16181A468726566AA687474703A2F2F666F6FA16393A362617282A174A162A16391A3626262A362617A",
				"(_type=a,a=(href=http://foo),c=@(bar,(_type=b,c=@(bbb)),baz))",
				"(t=a,a=(href=http://foo),c=@(bar,(t=b,c=@(bbb)),baz))",
				"_type=a&a=(href=http://foo)&c=@(bar,(_type=b,c=@(bbb)),baz)",
				"t=a&a=(href=http://foo)&c=@(bar,(t=b,c=@(bbb)),baz)",
			},
			{
				"A-3",
				a("http://foo", ""),
				"<a href='http://foo'>_xE000_</a>",
				"<a href='http://foo'>_xE000_</a>\n",
				"<a href='http://foo'><sp/></a>",
				"<a href='http://foo'><sp/></a>\n",
				"{_type:'a',a:{href:'http://foo'},c:['']}",
				"{t:'a',a:{href:'http://foo'},c:['']}",
				"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A0",
				"83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A0",
				"(_type=a,a=(href=http://foo),c=@(''))",
				"(t=a,a=(href=http://foo),c=@(''))",
				"_type=a&a=(href=http://foo)&c=@('')",
				"t=a&a=(href=http://foo)&c=@('')",
			},
			{
				"A-4",
				a("http://foo", " "),
				"<a href='http://foo'>_x0020_</a>",
				"<a href='http://foo'>_x0020_</a>\n",
				"<a href='http://foo'><sp> </sp></a>",
				"<a href='http://foo'><sp> </sp></a>\n",
				"{_type:'a',a:{href:'http://foo'},c:[' ']}",
				"{t:'a',a:{href:'http://foo'},c:[' ']}",
				"83A55F74797065A161A16181A468726566AA687474703A2F2F666F6FA16391A120",
				"83A174A161A16181A468726566AA687474703A2F2F666F6FA16391A120",
				"(_type=a,a=(href=http://foo),c=@(' '))",
				"(t=a,a=(href=http://foo),c=@(' '))",
				"_type=a&a=(href=http://foo)&c=@('+')",
				"t=a&a=(href=http://foo)&c=@('+')",
			},
			{
				"A-5",
				a("http://foo"),
				"<a href='http://foo'/>",
				"<a href='http://foo'/>\n",
				"<a href='http://foo'/>",
				"<a href='http://foo'/>\n",
				"{_type:'a',a:{href:'http://foo'}}",
				"{t:'a',a:{href:'http://foo'}}",
				"82A55F74797065A161A16181A468726566AA687474703A2F2F666F6F",
				"82A174A161A16181A468726566AA687474703A2F2F666F6F",
				"(_type=a,a=(href=http://foo))",
				"(t=a,a=(href=http://foo))",
				"_type=a&a=(href=http://foo)",
				"t=a&a=(href=http://foo)",
			},
			{
				"Abbr-1",
				abbr().children("foo"),
				"<abbr>foo</abbr>",
				"<abbr>foo</abbr>\n",
				"<abbr>foo</abbr>",
				"<abbr>foo</abbr>\n",
				"{_type:'abbr',c:['foo']}",
				"{t:'abbr',c:['foo']}",
				"82A55F74797065A461626272A16391A3666F6F",
				"82A174A461626272A16391A3666F6F",
				"(_type=abbr,c=@(foo))",
				"(t=abbr,c=@(foo))",
				"_type=abbr&c=@(foo)",
				"t=abbr&c=@(foo)",
			},
			{
				"Abbr-2",
				abbr("foo", "bar", btag, "baz"),
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n",
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n",
				"{_type:'abbr',a:{title:'foo'},c:['bar',{_type:'b',c:['bbb']},'baz']}",
				"{t:'abbr',a:{title:'foo'},c:['bar',{t:'b',c:['bbb']},'baz']}",
				"83A55F74797065A461626272A16181A57469746C65A3666F6FA16393A362617282A55F74797065A162A16391A3626262A362617A",
				"83A174A461626272A16181A57469746C65A3666F6FA16393A362617282A174A162A16391A3626262A362617A",
				"(_type=abbr,a=(title=foo),c=@(bar,(_type=b,c=@(bbb)),baz))",
				"(t=abbr,a=(title=foo),c=@(bar,(t=b,c=@(bbb)),baz))",
				"_type=abbr&a=(title=foo)&c=@(bar,(_type=b,c=@(bbb)),baz)",
				"t=abbr&a=(title=foo)&c=@(bar,(t=b,c=@(bbb)),baz)",
			},
			{
				"Address-1",
				address(),
				"<address/>",
				"<address/>\n",
				"<address/>",
				"<address/>\n",
				"{_type:'address'}",
				"{t:'address'}",
				"81A55F74797065A761646472657373",
				"81A174A761646472657373",
				"(_type=address)",
				"(t=address)",
				"_type=address",
				"t=address",
			},
			{
				"Address-2",
				address(""),
				"<address>_xE000_</address>",
				"<address>_xE000_</address>\n",
				"<address><sp/></address>",
				"<address><sp/></address>\n",
				"{_type:'address',c:['']}",
				"{t:'address',c:['']}",
				"82A55F74797065A761646472657373A16391A0",
				"82A174A761646472657373A16391A0",
				"(_type=address,c=@(''))",
				"(t=address,c=@(''))",
				"_type=address&c=@('')",
				"t=address&c=@('')",
			},
			{
				"Address-3",
				address("foo", a("bar", "baz"), a("qux", "quux")),
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n",
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n",
				"{_type:'address',c:['foo',{_type:'a',a:{href:'bar'},c:['baz']},{_type:'a',a:{href:'qux'},c:['quux']}]}",
				"{t:'address',c:['foo',{t:'a',a:{href:'bar'},c:['baz']},{t:'a',a:{href:'qux'},c:['quux']}]}",
				"82A55F74797065A761646472657373A16393A3666F6F83A55F74797065A161A16181A468726566A3626172A16391A362617A83A55F74797065A161A16181A468726566A3717578A16391A471757578",
				"82A174A761646472657373A16393A3666F6F83A174A161A16181A468726566A3626172A16391A362617A83A174A161A16181A468726566A3717578A16391A471757578",
				"(_type=address,c=@(foo,(_type=a,a=(href=bar),c=@(baz)),(_type=a,a=(href=qux),c=@(quux))))",
				"(t=address,c=@(foo,(t=a,a=(href=bar),c=@(baz)),(t=a,a=(href=qux),c=@(quux))))",
				"_type=address&c=@(foo,(_type=a,a=(href=bar),c=@(baz)),(_type=a,a=(href=qux),c=@(quux)))",
				"t=address&c=@(foo,(t=a,a=(href=bar),c=@(baz)),(t=a,a=(href=qux),c=@(quux)))",
			},
			{
				"Aside-1",
				aside(
					h1("header1"),p("foo")
				),
				"<aside><h1>header1</h1><p>foo</p></aside>",
				"<aside><h1>header1</h1><p>foo</p></aside>\n",
				"<aside><h1>header1</h1><p>foo</p></aside>",
				"<aside><h1>header1</h1><p>foo</p></aside>\n",
				"{_type:'aside',c:[{_type:'h1',c:['header1']},{_type:'p',c:['foo']}]}",
				"{t:'aside',c:[{t:'h1',c:['header1']},{t:'p',c:['foo']}]}",
				"82A55F74797065A56173696465A1639282A55F74797065A26831A16391A76865616465723182A55F74797065A170A16391A3666F6F",
				"82A174A56173696465A1639282A174A26831A16391A76865616465723182A174A170A16391A3666F6F",
				"(_type=aside,c=@((_type=h1,c=@(header1)),(_type=p,c=@(foo))))",
				"(t=aside,c=@((t=h1,c=@(header1)),(t=p,c=@(foo))))",
				"_type=aside&c=@((_type=h1,c=@(header1)),(_type=p,c=@(foo)))",
				"t=aside&c=@((t=h1,c=@(header1)),(t=p,c=@(foo)))",
			},
			{
				"Audio/Source-1",
				audio().controls(true).children(
					source("foo.ogg", "audio/ogg"),
					source("foo.mp3", "audio/mpeg")
				),
				"<audio controls='true'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
				"<audio controls='true'>\n\t<source src='foo.ogg' type='audio/ogg'/>\n\t<source src='foo.mp3' type='audio/mpeg'/>\n</audio>\n",
				"<audio controls='true'><source src='foo.ogg' type='audio/ogg'/><source src='foo.mp3' type='audio/mpeg'/></audio>",
				"<audio controls='true'>\n\t<source src='foo.ogg' type='audio/ogg'/>\n\t<source src='foo.mp3' type='audio/mpeg'/>\n</audio>\n",
				"{_type:'audio',a:{controls:true},c:[{_type:'source',a:{src:'foo.ogg',type:'audio/ogg'}},{_type:'source',a:{src:'foo.mp3',type:'audio/mpeg'}}]}",
				"{t:'audio',a:{controls:true},c:[{t:'source',a:{src:'foo.ogg',type:'audio/ogg'}},{t:'source',a:{src:'foo.mp3',type:'audio/mpeg'}}]}",
				"83A55F74797065A5617564696FA16181A8636F6E74726F6C73C3A1639282A55F74797065A6736F75726365A16182A3737263A7666F6F2E6F6767A474797065A9617564696F2F6F676782A55F74797065A6736F75726365A16182A3737263A7666F6F2E6D7033A474797065AA617564696F2F6D706567",
				"83A174A5617564696FA16181A8636F6E74726F6C73C3A1639282A174A6736F75726365A16182A3737263A7666F6F2E6F6767A474797065A9617564696F2F6F676782A174A6736F75726365A16182A3737263A7666F6F2E6D7033A474797065AA617564696F2F6D706567",
				"(_type=audio,a=(controls=true),c=@((_type=source,a=(src=foo.ogg,type=audio/ogg)),(_type=source,a=(src=foo.mp3,type=audio/mpeg))))",
				"(t=audio,a=(controls=true),c=@((t=source,a=(src=foo.ogg,type=audio/ogg)),(t=source,a=(src=foo.mp3,type=audio/mpeg))))",
				"_type=audio&a=(controls=true)&c=@((_type=source,a=(src=foo.ogg,type=audio/ogg)),(_type=source,a=(src=foo.mp3,type=audio/mpeg)))",
				"t=audio&a=(controls=true)&c=@((t=source,a=(src=foo.ogg,type=audio/ogg)),(t=source,a=(src=foo.mp3,type=audio/mpeg)))",
			},
			{
				"Bdi-1",
				p("foo", bdi("إيان"), "bar"),
				"<p>foo<bdi>إيان</bdi>bar</p>",
				"<p>foo<bdi>إيان</bdi>bar</p>\n",
				"<p>foo<bdi>إيان</bdi>bar</p>",
				"<p>foo<bdi>إيان</bdi>bar</p>\n",
				"{_type:'p',c:['foo',{_type:'bdi',c:'إيان'},'bar']}",
				"{t:'p',c:['foo',{t:'bdi',c:'إيان'},'bar']}",
				"82A55F74797065A170A16393A3666F6F82A55F74797065A3626469A163A8D8A5D98AD8A7D986A3626172",
				"82A174A170A16393A3666F6F82A174A3626469A163A8D8A5D98AD8A7D986A3626172",
				"(_type=p,c=@(foo,(_type=bdi,c=إيان),bar))",
				"(t=p,c=@(foo,(t=bdi,c=إيان),bar))",
				"_type=p&c=@(foo,(_type=bdi,c=%D8%A5%D9%8A%D8%A7%D9%86),bar)",
				"t=p&c=@(foo,(t=bdi,c=%D8%A5%D9%8A%D8%A7%D9%86),bar)",
			},
			{
				"Bdo-1",
				p("foo", bdo("rtl", "baz"), "bar"),
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n",
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n",
				"{_type:'p',c:['foo',{_type:'bdo',a:{dir:'rtl'},c:['baz']},'bar']}",
				"{t:'p',c:['foo',{t:'bdo',a:{dir:'rtl'},c:['baz']},'bar']}",
				"82A55F74797065A170A16393A3666F6F83A55F74797065A362646FA16181A3646972A372746CA16391A362617AA3626172",
				"82A174A170A16393A3666F6F83A174A362646FA16181A3646972A372746CA16391A362617AA3626172",
				"(_type=p,c=@(foo,(_type=bdo,a=(dir=rtl),c=@(baz)),bar))",
				"(t=p,c=@(foo,(t=bdo,a=(dir=rtl),c=@(baz)),bar))",
				"_type=p&c=@(foo,(_type=bdo,a=(dir=rtl),c=@(baz)),bar)",
				"t=p&c=@(foo,(t=bdo,a=(dir=rtl),c=@(baz)),bar)",
			},
			{
				"Blockquote-1",
				blockquote("foo"),
				"<blockquote>foo</blockquote>",
				"<blockquote>foo</blockquote>\n",
				"<blockquote>foo</blockquote>",
				"<blockquote>foo</blockquote>\n",
				"{_type:'blockquote',c:['foo']}",
				"{t:'blockquote',c:['foo']}",
				"82A55F74797065AA626C6F636B71756F7465A16391A3666F6F",
				"82A174AA626C6F636B71756F7465A16391A3666F6F",
				"(_type=blockquote,c=@(foo))",
				"(t=blockquote,c=@(foo))",
				"_type=blockquote&c=@(foo)",
				"t=blockquote&c=@(foo)",
			},
			{
				"Br-1",
				br(),
				"<br/>",
				"<br/>\n",
				"<br/>",
				"<br/>\n",
				"{_type:'br'}",
				"{t:'br'}",
				"81A55F74797065A26272",
				"81A174A26272",
				"(_type=br)",
				"(t=br)",
				"_type=br",
				"t=br",
			},
			{
				"Br-2",
				p(br()),
				"<p><br/></p>",
				"<p><br/></p>\n",
				"<p><br/></p>",
				"<p><br/></p>\n",
				"{_type:'p',c:[{_type:'br'}]}",
				"{t:'p',c:[{t:'br'}]}",
				"82A55F74797065A170A1639181A55F74797065A26272",
				"82A174A170A1639181A174A26272",
				"(_type=p,c=@((_type=br)))",
				"(t=p,c=@((t=br)))",
				"_type=p&c=@((_type=br))",
				"t=p&c=@((t=br))",
			},
			{
				"Button-1",
				button("button", "foo"),
				"<button type='button'>foo</button>",
				"<button type='button'>foo</button>\n",
				"<button type='button'>foo</button>",
				"<button type='button'>foo</button>\n",
				"{_type:'button',a:{type:'button'},c:['foo']}",
				"{t:'button',a:{type:'button'},c:['foo']}",
				"83A55F74797065A6627574746F6EA16181A474797065A6627574746F6EA16391A3666F6F",
				"83A174A6627574746F6EA16181A474797065A6627574746F6EA16391A3666F6F",
				"(_type=button,a=(type=button),c=@(foo))",
				"(t=button,a=(type=button),c=@(foo))",
				"_type=button&a=(type=button)&c=@(foo)",
				"t=button&a=(type=button)&c=@(foo)",
			},
			{
				"Canvas-1",
				canvas(100, 200),
				"<canvas width='100' height='200'/>",
				"<canvas width='100' height='200'/>\n",
				"<canvas width='100' height='200'/>",
				"<canvas width='100' height='200'/>\n",
				"{_type:'canvas',a:{width:100,height:200}}",
				"{t:'canvas',a:{width:100,height:200}}",
				"82A55F74797065A663616E766173A16182A5776964746864A6686569676874D100C8",
				"82A174A663616E766173A16182A5776964746864A6686569676874D100C8",
				"(_type=canvas,a=(width=100,height=200))",
				"(t=canvas,a=(width=100,height=200))",
				"_type=canvas&a=(width=100,height=200)",
				"t=canvas&a=(width=100,height=200)",
			},
			{
				"Cite-1",
				p(cite("foo")),
				"<p><cite>foo</cite></p>",
				"<p><cite>foo</cite></p>\n",
				"<p><cite>foo</cite></p>",
				"<p><cite>foo</cite></p>\n",
				"{_type:'p',c:[{_type:'cite',c:['foo']}]}",
				"{t:'p',c:[{t:'cite',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A463697465A16391A3666F6F",
				"82A174A170A1639182A174A463697465A16391A3666F6F",
				"(_type=p,c=@((_type=cite,c=@(foo))))",
				"(t=p,c=@((t=cite,c=@(foo))))",
				"_type=p&c=@((_type=cite,c=@(foo)))",
				"t=p&c=@((t=cite,c=@(foo)))",
			},
			{
				"Code-1",
				code("foo\n\tbar"),
				"<code>foo&#x000a;&#x0009;bar</code>",
				"<code>foo&#x000a;&#x0009;bar</code>\n",
				"<code>foo<br/><sp>&#x2003;</sp>bar</code>",
				"<code>foo<br/><sp>&#x2003;</sp>bar</code>\n",
				"{_type:'code',c:['foo\\n\\tbar']}",
				"{t:'code',c:['foo\\n\\tbar']}",
				"82A55F74797065A4636F6465A16391A8666F6F0A09626172",
				"82A174A4636F6465A16391A8666F6F0A09626172",
				"(_type=code,c=@('foo\n\tbar'))",
				"(t=code,c=@('foo\n\tbar'))",
				"_type=code&c=@('foo%0A%09bar')",
				"t=code&c=@('foo%0A%09bar')",
			},
			{
				"Datalist-1",
				datalist("foo",
					option("One"),
					option("Two")
				),
				"<datalist id='foo'><option value='One'/><option value='Two'/></datalist>",
				"<datalist id='foo'>\n\t<option value='One'/>\n\t<option value='Two'/>\n</datalist>\n",
				"<datalist id='foo'><option value='One'/><option value='Two'/></datalist>",
				"<datalist id='foo'>\n\t<option value='One'/>\n\t<option value='Two'/>\n</datalist>\n",
				"{_type:'datalist',a:{id:'foo'},c:[{_type:'option',a:{value:'One'}},{_type:'option',a:{value:'Two'}}]}",
				"{t:'datalist',a:{id:'foo'},c:[{t:'option',a:{value:'One'}},{t:'option',a:{value:'Two'}}]}",
				"83A55F74797065A8646174616C697374A16181A26964A3666F6FA1639282A55F74797065A66F7074696F6EA16181A576616C7565A34F6E6582A55F74797065A66F7074696F6EA16181A576616C7565A354776F",
				"83A174A8646174616C697374A16181A26964A3666F6FA1639282A174A66F7074696F6EA16181A576616C7565A34F6E6582A174A66F7074696F6EA16181A576616C7565A354776F",
				"(_type=datalist,a=(id=foo),c=@((_type=option,a=(value=One)),(_type=option,a=(value=Two))))",
				"(t=datalist,a=(id=foo),c=@((t=option,a=(value=One)),(t=option,a=(value=Two))))",
				"_type=datalist&a=(id=foo)&c=@((_type=option,a=(value=One)),(_type=option,a=(value=Two)))",
				"t=datalist&a=(id=foo)&c=@((t=option,a=(value=One)),(t=option,a=(value=Two)))",
			},
			{
				"Dl/Dt/Dd",
				dl(
					dt("foo"),
					dd("bar")
				),
				"<dl><dt>foo</dt><dd>bar</dd></dl>",
				"<dl>\n\t<dt>foo</dt>\n\t<dd>bar</dd>\n</dl>\n",
				"<dl><dt>foo</dt><dd>bar</dd></dl>",
				"<dl>\n\t<dt>foo</dt>\n\t<dd>bar</dd>\n</dl>\n",
				"{_type:'dl',c:[{_type:'dt',c:['foo']},{_type:'dd',c:['bar']}]}",
				"{t:'dl',c:[{t:'dt',c:['foo']},{t:'dd',c:['bar']}]}",
				"82A55F74797065A2646CA1639282A55F74797065A26474A16391A3666F6F82A55F74797065A26464A16391A3626172",
				"82A174A2646CA1639282A174A26474A16391A3666F6F82A174A26464A16391A3626172",
				"(_type=dl,c=@((_type=dt,c=@(foo)),(_type=dd,c=@(bar))))",
				"(t=dl,c=@((t=dt,c=@(foo)),(t=dd,c=@(bar))))",
				"_type=dl&c=@((_type=dt,c=@(foo)),(_type=dd,c=@(bar)))",
				"t=dl&c=@((t=dt,c=@(foo)),(t=dd,c=@(bar)))",
			},
			{
				"Del/Ins",
				p(del("foo",btag,"bar"),ins("baz")),
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n",
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n",
				"{_type:'p',c:[{_type:'del',c:['foo',{_type:'b',c:['bbb']},'bar']},{_type:'ins',c:['baz']}]}",
				"{t:'p',c:[{t:'del',c:['foo',{t:'b',c:['bbb']},'bar']},{t:'ins',c:['baz']}]}",
				"82A55F74797065A170A1639282A55F74797065A364656CA16393A3666F6F82A55F74797065A162A16391A3626262A362617282A55F74797065A3696E73A16391A362617A",
				"82A174A170A1639282A174A364656CA16393A3666F6F82A174A162A16391A3626262A362617282A174A3696E73A16391A362617A",
				"(_type=p,c=@((_type=del,c=@(foo,(_type=b,c=@(bbb)),bar)),(_type=ins,c=@(baz))))",
				"(t=p,c=@((t=del,c=@(foo,(t=b,c=@(bbb)),bar)),(t=ins,c=@(baz))))",
				"_type=p&c=@((_type=del,c=@(foo,(_type=b,c=@(bbb)),bar)),(_type=ins,c=@(baz)))",
				"t=p&c=@((t=del,c=@(foo,(t=b,c=@(bbb)),bar)),(t=ins,c=@(baz)))",
			},
			{
				"Dfn",
				p(dfn("foo")),
				"<p><dfn>foo</dfn></p>",
				"<p><dfn>foo</dfn></p>\n",
				"<p><dfn>foo</dfn></p>",
				"<p><dfn>foo</dfn></p>\n",
				"{_type:'p',c:[{_type:'dfn',c:['foo']}]}",
				"{t:'p',c:[{t:'dfn',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A364666EA16391A3666F6F",
				"82A174A170A1639182A174A364666EA16391A3666F6F",
				"(_type=p,c=@((_type=dfn,c=@(foo))))",
				"(t=p,c=@((t=dfn,c=@(foo))))",
				"_type=p&c=@((_type=dfn,c=@(foo)))",
				"t=p&c=@((t=dfn,c=@(foo)))",
			},
			{
				"Div",
				div("foo",btag,"bar"),
				"<div>foo<b>bbb</b>bar</div>",
				"<div>foo<b>bbb</b>bar</div>\n",
				"<div>foo<b>bbb</b>bar</div>",
				"<div>foo<b>bbb</b>bar</div>\n",
				"{_type:'div',c:['foo',{_type:'b',c:['bbb']},'bar']}",
				"{t:'div',c:['foo',{t:'b',c:['bbb']},'bar']}",
				"82A55F74797065A3646976A16393A3666F6F82A55F74797065A162A16391A3626262A3626172",
				"82A174A3646976A16393A3666F6F82A174A162A16391A3626262A3626172",
				"(_type=div,c=@(foo,(_type=b,c=@(bbb)),bar))",
				"(t=div,c=@(foo,(t=b,c=@(bbb)),bar))",
				"_type=div&c=@(foo,(_type=b,c=@(bbb)),bar)",
				"t=div&c=@(foo,(t=b,c=@(bbb)),bar)",
			},
			{
				"Em",
				p("foo",em("bar"),"baz"),
				"<p>foo<em>bar</em>baz</p>",
				"<p>foo<em>bar</em>baz</p>\n",
				"<p>foo<em>bar</em>baz</p>",
				"<p>foo<em>bar</em>baz</p>\n",
				"{_type:'p',c:['foo',{_type:'em',c:['bar']},'baz']}",
				"{t:'p',c:['foo',{t:'em',c:['bar']},'baz']}",
				"82A55F74797065A170A16393A3666F6F82A55F74797065A2656DA16391A3626172A362617A",
				"82A174A170A16393A3666F6F82A174A2656DA16391A3626172A362617A",
				"(_type=p,c=@(foo,(_type=em,c=@(bar)),baz))",
				"(t=p,c=@(foo,(t=em,c=@(bar)),baz))",
				"_type=p&c=@(foo,(_type=em,c=@(bar)),baz)",
				"t=p&c=@(foo,(t=em,c=@(bar)),baz)",
			},
			{
				"Embed",
				embed("foo.swf"),
				"<embed src='foo.swf'/>",
				"<embed src='foo.swf'/>\n",
				"<embed src='foo.swf'/>",
				"<embed src='foo.swf'/>\n",
				"{_type:'embed',a:{src:'foo.swf'}}",
				"{t:'embed',a:{src:'foo.swf'}}",
				"82A55F74797065A5656D626564A16181A3737263A7666F6F2E737766",
				"82A174A5656D626564A16181A3737263A7666F6F2E737766",
				"(_type=embed,a=(src=foo.swf))",
				"(t=embed,a=(src=foo.swf))",
				"_type=embed&a=(src=foo.swf)",
				"t=embed&a=(src=foo.swf)",
			},
			{
				"Form/Fieldset/Legend/Input/Keygen/Label",
				form("bar",
					fieldset(
						legend("foo:"),
						"Name:", input("text"), br(),
						"Email:", input("text"), br(),
						"X:", keygen().name("X"),
						label("label")._for("Name")
					)
				),
				"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
				"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>\n",
				"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>",
				"<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>\n",
				"{_type:'form',a:{action:'bar'},c:[{_type:'fieldset',c:[{_type:'legend',c:['foo:']},'Name:',{_type:'input',a:{type:'text'}},{_type:'br'},'Email:',{_type:'input',a:{type:'text'}},{_type:'br'},'X:',{_type:'keygen',a:{name:'X'}},{_type:'label',a:{'for':'Name'},c:['label']}]}]}",
				"{t:'form',a:{action:'bar'},c:[{t:'fieldset',c:[{t:'legend',c:['foo:']},'Name:',{t:'input',a:{type:'text'}},{t:'br'},'Email:',{t:'input',a:{type:'text'}},{t:'br'},'X:',{t:'keygen',a:{name:'X'}},{t:'label',a:{'for':'Name'},c:['label']}]}]}",
				"83A55F74797065A4666F726DA16181A6616374696F6EA3626172A1639182A55F74797065A86669656C64736574A1639A82A55F74797065A66C6567656E64A16391A4666F6F3AA54E616D653A82A55F74797065A5696E707574A16181A474797065A47465787481A55F74797065A26272A6456D61696C3A82A55F74797065A5696E707574A16181A474797065A47465787481A55F74797065A26272A2583A82A55F74797065A66B657967656EA16181A46E616D65A15883A55F74797065A56C6162656CA16181A3666F72A44E616D65A16391A56C6162656C",
				"83A174A4666F726DA16181A6616374696F6EA3626172A1639182A174A86669656C64736574A1639A82A174A66C6567656E64A16391A4666F6F3AA54E616D653A82A174A5696E707574A16181A474797065A47465787481A174A26272A6456D61696C3A82A174A5696E707574A16181A474797065A47465787481A174A26272A2583A82A174A66B657967656EA16181A46E616D65A15883A174A56C6162656CA16181A3666F72A44E616D65A16391A56C6162656C",
				"(_type=form,a=(action=bar),c=@((_type=fieldset,c=@((_type=legend,c=@(foo:)),Name:,(_type=input,a=(type=text)),(_type=br),Email:,(_type=input,a=(type=text)),(_type=br),X:,(_type=keygen,a=(name=X)),(_type=label,a=(for=Name),c=@(label))))))",
				"(t=form,a=(action=bar),c=@((t=fieldset,c=@((t=legend,c=@(foo:)),Name:,(t=input,a=(type=text)),(t=br),Email:,(t=input,a=(type=text)),(t=br),X:,(t=keygen,a=(name=X)),(t=label,a=(for=Name),c=@(label))))))",
				"_type=form&a=(action=bar)&c=@((_type=fieldset,c=@((_type=legend,c=@(foo:)),Name:,(_type=input,a=(type=text)),(_type=br),Email:,(_type=input,a=(type=text)),(_type=br),X:,(_type=keygen,a=(name=X)),(_type=label,a=(for=Name),c=@(label)))))",
				"t=form&a=(action=bar)&c=@((t=fieldset,c=@((t=legend,c=@(foo:)),Name:,(t=input,a=(type=text)),(t=br),Email:,(t=input,a=(type=text)),(t=br),X:,(t=keygen,a=(name=X)),(t=label,a=(for=Name),c=@(label)))))",
			},
			{
				"Figure/Figcaption/Img",
				figure(
					img("foo.png").alt("foo").width(100).height(200),
					figcaption("The caption")
				),
				"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
				"<figure>\n\t<img src='foo.png' alt='foo' width='100' height='200'/>\n\t<figcaption>The caption</figcaption>\n</figure>\n",
				"<figure><img src='foo.png' alt='foo' width='100' height='200'/><figcaption>The caption</figcaption></figure>",
				"<figure>\n\t<img src='foo.png' alt='foo' width='100' height='200'/>\n\t<figcaption>The caption</figcaption>\n</figure>\n",
				"{_type:'figure',c:[{_type:'img',a:{src:'foo.png',alt:'foo',width:100,height:200}},{_type:'figcaption',c:['The caption']}]}",
				"{t:'figure',c:[{t:'img',a:{src:'foo.png',alt:'foo',width:100,height:200}},{t:'figcaption',c:['The caption']}]}",
				"82A55F74797065A6666967757265A1639282A55F74797065A3696D67A16184A3737263A7666F6F2E706E67A3616C74A3666F6FA5776964746864A6686569676874D100C882A55F74797065AA66696763617074696F6EA16391AB5468652063617074696F6E",
				"82A174A6666967757265A1639282A174A3696D67A16184A3737263A7666F6F2E706E67A3616C74A3666F6FA5776964746864A6686569676874D100C882A174AA66696763617074696F6EA16391AB5468652063617074696F6E",
				"(_type=figure,c=@((_type=img,a=(src=foo.png,alt=foo,width=100,height=200)),(_type=figcaption,c=@('The caption'))))",
				"(t=figure,c=@((t=img,a=(src=foo.png,alt=foo,width=100,height=200)),(t=figcaption,c=@('The caption'))))",
				"_type=figure&c=@((_type=img,a=(src=foo.png,alt=foo,width=100,height=200)),(_type=figcaption,c=@('The+caption')))",
				"t=figure&c=@((t=img,a=(src=foo.png,alt=foo,width=100,height=200)),(t=figcaption,c=@('The+caption')))",
			},
			{
				"H1/H2/H3/H4/H5/H6",
				div(
					h1("One"),h2("Two"),h3("Three"),h4("Four"),h5("Five"),h6("Six")
				),
				"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
				"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>\n",
				"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>",
				"<div><h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5><h6>Six</h6></div>\n",
				"{_type:'div',c:[{_type:'h1',c:['One']},{_type:'h2',c:['Two']},{_type:'h3',c:['Three']},{_type:'h4',c:['Four']},{_type:'h5',c:['Five']},{_type:'h6',c:['Six']}]}",
				"{t:'div',c:[{t:'h1',c:['One']},{t:'h2',c:['Two']},{t:'h3',c:['Three']},{t:'h4',c:['Four']},{t:'h5',c:['Five']},{t:'h6',c:['Six']}]}",
				"82A55F74797065A3646976A1639682A55F74797065A26831A16391A34F6E6582A55F74797065A26832A16391A354776F82A55F74797065A26833A16391A5546872656582A55F74797065A26834A16391A4466F757282A55F74797065A26835A16391A44669766582A55F74797065A26836A16391A3536978",
				"82A174A3646976A1639682A174A26831A16391A34F6E6582A174A26832A16391A354776F82A174A26833A16391A5546872656582A174A26834A16391A4466F757282A174A26835A16391A44669766582A174A26836A16391A3536978",
				"(_type=div,c=@((_type=h1,c=@(One)),(_type=h2,c=@(Two)),(_type=h3,c=@(Three)),(_type=h4,c=@(Four)),(_type=h5,c=@(Five)),(_type=h6,c=@(Six))))",
				"(t=div,c=@((t=h1,c=@(One)),(t=h2,c=@(Two)),(t=h3,c=@(Three)),(t=h4,c=@(Four)),(t=h5,c=@(Five)),(t=h6,c=@(Six))))",
				"_type=div&c=@((_type=h1,c=@(One)),(_type=h2,c=@(Two)),(_type=h3,c=@(Three)),(_type=h4,c=@(Four)),(_type=h5,c=@(Five)),(_type=h6,c=@(Six)))",
				"t=div&c=@((t=h1,c=@(One)),(t=h2,c=@(Two)),(t=h3,c=@(Three)),(t=h4,c=@(Four)),(t=h5,c=@(Five)),(t=h6,c=@(Six)))",
			},
			{
				"Hr",
				p(hr()),
				"<p><hr/></p>",
				"<p><hr/></p>\n",
				"<p><hr/></p>",
				"<p><hr/></p>\n",
				"{_type:'p',c:[{_type:'hr'}]}",
				"{t:'p',c:[{t:'hr'}]}",
				"82A55F74797065A170A1639181A55F74797065A26872",
				"82A174A170A1639181A174A26872",
				"(_type=p,c=@((_type=hr)))",
				"(t=p,c=@((t=hr)))",
				"_type=p&c=@((_type=hr))",
				"t=p&c=@((t=hr))",
			},
			{
				"Html/Head/Body/Title/Base/Link/Meta",
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
				"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
				"<html>\n"
				+"	<head>\n"
				+"		<title>title</title>\n"
				+"		<base href='foo' target='_blank'/>\n"
				+"		<link rel='stylesheet' type='text/css' href='theme.css'/>\n"
				+"		<meta charset='UTF-8'/>\n"
				+"	</head>\n"
				+"	<body>bar</body>\n"
				+"</html>\n",
				"<html><head><title>title</title><base href='foo' target='_blank'/><link rel='stylesheet' type='text/css' href='theme.css'/><meta charset='UTF-8'/></head><body>bar</body></html>",
				"<html>\n"
				+"	<head>\n"
				+"		<title>title</title>\n"
				+"		<base href='foo' target='_blank'/>\n"
				+"		<link rel='stylesheet' type='text/css' href='theme.css'/>\n"
				+"		<meta charset='UTF-8'/>\n"
				+"	</head>\n"
				+"	<body>bar</body>\n"
				+"</html>\n",
				"{_type:'html',c:[{_type:'head',c:[{_type:'title',c:'title'},{_type:'base',a:{href:'foo',target:'_blank'}},{_type:'link',a:{rel:'stylesheet',type:'text/css',href:'theme.css'}},{_type:'meta',a:{charset:'UTF-8'}}]},{_type:'body',c:['bar']}]}",
				"{t:'html',c:[{t:'head',c:[{t:'title',c:'title'},{t:'base',a:{href:'foo',target:'_blank'}},{t:'link',a:{rel:'stylesheet',type:'text/css',href:'theme.css'}},{t:'meta',a:{charset:'UTF-8'}}]},{t:'body',c:['bar']}]}",
				"82A55F74797065A468746D6CA1639282A55F74797065A468656164A1639482A55F74797065A57469746C65A163A57469746C6582A55F74797065A462617365A16182A468726566A3666F6FA6746172676574A65F626C616E6B82A55F74797065A46C696E6BA16183A372656CAA7374796C657368656574A474797065A8746578742F637373A468726566A97468656D652E63737382A55F74797065A46D657461A16181A763686172736574A55554462D3882A55F74797065A4626F6479A16391A3626172",
				"82A174A468746D6CA1639282A174A468656164A1639482A174A57469746C65A163A57469746C6582A174A462617365A16182A468726566A3666F6FA6746172676574A65F626C616E6B82A174A46C696E6BA16183A372656CAA7374796C657368656574A474797065A8746578742F637373A468726566A97468656D652E63737382A174A46D657461A16181A763686172736574A55554462D3882A174A4626F6479A16391A3626172",
				"(_type=html,c=@((_type=head,c=@((_type=title,c=title),(_type=base,a=(href=foo,target=_blank)),(_type=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(_type=meta,a=(charset=UTF-8)))),(_type=body,c=@(bar))))",
				"(t=html,c=@((t=head,c=@((t=title,c=title),(t=base,a=(href=foo,target=_blank)),(t=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(t=meta,a=(charset=UTF-8)))),(t=body,c=@(bar))))",
				"_type=html&c=@((_type=head,c=@((_type=title,c=title),(_type=base,a=(href=foo,target=_blank)),(_type=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(_type=meta,a=(charset=UTF-8)))),(_type=body,c=@(bar)))",
				"t=html&c=@((t=head,c=@((t=title,c=title),(t=base,a=(href=foo,target=_blank)),(t=link,a=(rel=stylesheet,type=text/css,href=theme.css)),(t=meta,a=(charset=UTF-8)))),(t=body,c=@(bar)))",
			},
			{
				"I",
				p(i("foo")),
				"<p><i>foo</i></p>",
				"<p><i>foo</i></p>\n",
				"<p><i>foo</i></p>",
				"<p><i>foo</i></p>\n",
				"{_type:'p',c:[{_type:'i',c:['foo']}]}",
				"{t:'p',c:[{t:'i',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A169A16391A3666F6F",
				"82A174A170A1639182A174A169A16391A3666F6F",
				"(_type=p,c=@((_type=i,c=@(foo))))",
				"(t=p,c=@((t=i,c=@(foo))))",
				"_type=p&c=@((_type=i,c=@(foo)))",
				"t=p&c=@((t=i,c=@(foo)))",
			},
			{
				"Iframe",
				iframe("foo"),
				"<iframe>foo</iframe>",
				"<iframe>foo</iframe>\n",
				"<iframe>foo</iframe>",
				"<iframe>foo</iframe>\n",
				"{_type:'iframe',c:['foo']}",
				"{t:'iframe',c:['foo']}",
				"82A55F74797065A6696672616D65A16391A3666F6F",
				"82A174A6696672616D65A16391A3666F6F",
				"(_type=iframe,c=@(foo))",
				"(t=iframe,c=@(foo))",
				"_type=iframe&c=@(foo)",
				"t=iframe&c=@(foo)",
			},
			{
				"Kbd",
				p(kbd("foo")),
				"<p><kbd>foo</kbd></p>",
				"<p><kbd>foo</kbd></p>\n",
				"<p><kbd>foo</kbd></p>",
				"<p><kbd>foo</kbd></p>\n",
				"{_type:'p',c:[{_type:'kbd',c:['foo']}]}",
				"{t:'p',c:[{t:'kbd',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A36B6264A16391A3666F6F",
				"82A174A170A1639182A174A36B6264A16391A3666F6F",
				"(_type=p,c=@((_type=kbd,c=@(foo))))",
				"(t=p,c=@((t=kbd,c=@(foo))))",
				"_type=p&c=@((_type=kbd,c=@(foo)))",
				"t=p&c=@((t=kbd,c=@(foo)))",
			},
			{
				"Main/Article/Header/Footer-1",
				main(
					article(
						header(h1("header1"),p("header2")),
						p("content"),
						footer(h2("footer1"),p("footer2"))
					)
				),
				"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
				"<main>\n\t<article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article>\n</main>\n",
				"<main><article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article></main>",
				"<main>\n\t<article><header><h1>header1</h1><p>header2</p></header><p>content</p><footer><h2>footer1</h2><p>footer2</p></footer></article>\n</main>\n",
				"{_type:'main',c:[{_type:'article',c:[{_type:'header',c:[{_type:'h1',c:['header1']},{_type:'p',c:['header2']}]},{_type:'p',c:['content']},{_type:'footer',c:[{_type:'h2',c:['footer1']},{_type:'p',c:['footer2']}]}]}]}",
				"{t:'main',c:[{t:'article',c:[{t:'header',c:[{t:'h1',c:['header1']},{t:'p',c:['header2']}]},{t:'p',c:['content']},{t:'footer',c:[{t:'h2',c:['footer1']},{t:'p',c:['footer2']}]}]}]}",
				"82A55F74797065A46D61696EA1639182A55F74797065A761727469636C65A1639382A55F74797065A6686561646572A1639282A55F74797065A26831A16391A76865616465723182A55F74797065A170A16391A76865616465723282A55F74797065A170A16391A7636F6E74656E7482A55F74797065A6666F6F746572A1639282A55F74797065A26832A16391A7666F6F7465723182A55F74797065A170A16391A7666F6F74657232",
				"82A174A46D61696EA1639182A174A761727469636C65A1639382A174A6686561646572A1639282A174A26831A16391A76865616465723182A174A170A16391A76865616465723282A174A170A16391A7636F6E74656E7482A174A6666F6F746572A1639282A174A26832A16391A7666F6F7465723182A174A170A16391A7666F6F74657232",
				"(_type=main,c=@((_type=article,c=@((_type=header,c=@((_type=h1,c=@(header1)),(_type=p,c=@(header2)))),(_type=p,c=@(content)),(_type=footer,c=@((_type=h2,c=@(footer1)),(_type=p,c=@(footer2))))))))",
				"(t=main,c=@((t=article,c=@((t=header,c=@((t=h1,c=@(header1)),(t=p,c=@(header2)))),(t=p,c=@(content)),(t=footer,c=@((t=h2,c=@(footer1)),(t=p,c=@(footer2))))))))",
				"_type=main&c=@((_type=article,c=@((_type=header,c=@((_type=h1,c=@(header1)),(_type=p,c=@(header2)))),(_type=p,c=@(content)),(_type=footer,c=@((_type=h2,c=@(footer1)),(_type=p,c=@(footer2)))))))",
				"t=main&c=@((t=article,c=@((t=header,c=@((t=h1,c=@(header1)),(t=p,c=@(header2)))),(t=p,c=@(content)),(t=footer,c=@((t=h2,c=@(footer1)),(t=p,c=@(footer2)))))))",
			},
			{
				"Map/Area-1",
				map(area("rect", "0,1,2,3", "foo").alt("bar")).name("baz"),
				"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
				"<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n",
				"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
				"<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n",
				"{_type:'map',a:{name:'baz'},c:[{_type:'area',a:{shape:'rect',coords:'0,1,2,3',href:'foo',alt:'bar'}}]}",
				"{t:'map',a:{name:'baz'},c:[{t:'area',a:{shape:'rect',coords:'0,1,2,3',href:'foo',alt:'bar'}}]}",
				"83A55F74797065A36D6170A16181A46E616D65A362617AA1639182A55F74797065A461726561A16184A57368617065A472656374A6636F6F726473A7302C312C322C33A468726566A3666F6FA3616C74A3626172",
				"83A174A36D6170A16181A46E616D65A362617AA1639182A174A461726561A16184A57368617065A472656374A6636F6F726473A7302C312C322C33A468726566A3666F6FA3616C74A3626172",
				"(_type=map,a=(name=baz),c=@((_type=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar))))",
				"(t=map,a=(name=baz),c=@((t=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar))))",
				"_type=map&a=(name=baz)&c=@((_type=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar)))",
				"t=map&a=(name=baz)&c=@((t=area,a=(shape=rect,coords='0,1,2,3',href=foo,alt=bar)))",
			},
			{
				"Mark",
				p(mark("foo")),
				"<p><mark>foo</mark></p>",
				"<p><mark>foo</mark></p>\n",
				"<p><mark>foo</mark></p>",
				"<p><mark>foo</mark></p>\n",
				"{_type:'p',c:[{_type:'mark',c:['foo']}]}",
				"{t:'p',c:[{t:'mark',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A46D61726BA16391A3666F6F",
				"82A174A170A1639182A174A46D61726BA16391A3666F6F",
				"(_type=p,c=@((_type=mark,c=@(foo))))",
				"(t=p,c=@((t=mark,c=@(foo))))",
				"_type=p&c=@((_type=mark,c=@(foo)))",
				"t=p&c=@((t=mark,c=@(foo)))",
			},
			{
				"Meter",
				meter("foo").value(1).min(0).max(2),
				"<meter value='1' min='0' max='2'>foo</meter>",
				"<meter value='1' min='0' max='2'>foo</meter>\n",
				"<meter value='1' min='0' max='2'>foo</meter>",
				"<meter value='1' min='0' max='2'>foo</meter>\n",
				"{_type:'meter',a:{value:1,min:0,max:2},c:['foo']}",
				"{t:'meter',a:{value:1,min:0,max:2},c:['foo']}",
				"83A55F74797065A56D65746572A16183A576616C756501A36D696E00A36D617802A16391A3666F6F",
				"83A174A56D65746572A16183A576616C756501A36D696E00A36D617802A16391A3666F6F",
				"(_type=meter,a=(value=1,min=0,max=2),c=@(foo))",
				"(t=meter,a=(value=1,min=0,max=2),c=@(foo))",
				"_type=meter&a=(value=1,min=0,max=2)&c=@(foo)",
				"t=meter&a=(value=1,min=0,max=2)&c=@(foo)",
			},
			{
				"Nav",
				nav(a("foo","bar")),
				"<nav><a href='foo'>bar</a></nav>",
				"<nav><a href='foo'>bar</a></nav>\n",
				"<nav><a href='foo'>bar</a></nav>",
				"<nav><a href='foo'>bar</a></nav>\n",
				"{_type:'nav',c:[{_type:'a',a:{href:'foo'},c:['bar']}]}",
				"{t:'nav',c:[{t:'a',a:{href:'foo'},c:['bar']}]}",
				"82A55F74797065A36E6176A1639183A55F74797065A161A16181A468726566A3666F6FA16391A3626172",
				"82A174A36E6176A1639183A174A161A16181A468726566A3666F6FA16391A3626172",
				"(_type=nav,c=@((_type=a,a=(href=foo),c=@(bar))))",
				"(t=nav,c=@((t=a,a=(href=foo),c=@(bar))))",
				"_type=nav&c=@((_type=a,a=(href=foo),c=@(bar)))",
				"t=nav&c=@((t=a,a=(href=foo),c=@(bar)))",
			},
			{
				"Noscript",
				noscript("No script!"),
				"<noscript>No script!</noscript>",
				"<noscript>No script!</noscript>\n",
				"<noscript>No script!</noscript>",
				"<noscript>No script!</noscript>\n",
				"{_type:'noscript',c:['No script!']}",
				"{t:'noscript',c:['No script!']}",
				"82A55F74797065A86E6F736372697074A16391AA4E6F2073637269707421",
				"82A174A86E6F736372697074A16391AA4E6F2073637269707421",
				"(_type=noscript,c=@('No script!'))",
				"(t=noscript,c=@('No script!'))",
				"_type=noscript&c=@('No+script!')",
				"t=noscript&c=@('No+script!')",
			},
			{
				"Object/Param",
				object().width(1).height(2).data("foo.swf").child(param("autoplay",true)),
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n",
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n",
				"{_type:'object',a:{width:1,height:2,data:'foo.swf'},c:[{_type:'param',a:{name:'autoplay',value:true}}]}",
				"{t:'object',a:{width:1,height:2,data:'foo.swf'},c:[{t:'param',a:{name:'autoplay',value:true}}]}",
				"83A55F74797065A66F626A656374A16183A5776964746801A668656967687402A464617461A7666F6F2E737766A1639182A55F74797065A5706172616DA16182A46E616D65A86175746F706C6179A576616C7565C3",
				"83A174A66F626A656374A16183A5776964746801A668656967687402A464617461A7666F6F2E737766A1639182A174A5706172616DA16182A46E616D65A86175746F706C6179A576616C7565C3",
				"(_type=object,a=(width=1,height=2,data=foo.swf),c=@((_type=param,a=(name=autoplay,value=true))))",
				"(t=object,a=(width=1,height=2,data=foo.swf),c=@((t=param,a=(name=autoplay,value=true))))",
				"_type=object&a=(width=1,height=2,data=foo.swf)&c=@((_type=param,a=(name=autoplay,value=true)))",
				"t=object&a=(width=1,height=2,data=foo.swf)&c=@((t=param,a=(name=autoplay,value=true)))",
			},
			{
				"Ol/Li",
				ol(li("foo")),
				"<ol><li>foo</li></ol>",
				"<ol>\n\t<li>foo</li>\n</ol>\n",
				"<ol><li>foo</li></ol>",
				"<ol>\n\t<li>foo</li>\n</ol>\n",
				"{_type:'ol',c:[{_type:'li',c:['foo']}]}",
				"{t:'ol',c:[{t:'li',c:['foo']}]}",
				"82A55F74797065A26F6CA1639182A55F74797065A26C69A16391A3666F6F",
				"82A174A26F6CA1639182A174A26C69A16391A3666F6F",
				"(_type=ol,c=@((_type=li,c=@(foo))))",
				"(t=ol,c=@((t=li,c=@(foo))))",
				"_type=ol&c=@((_type=li,c=@(foo)))",
				"t=ol&c=@((t=li,c=@(foo)))",
			},
			{
				"Output",
				form("testform",
					0,input("range").id("a").value(50),
					"+",input("number").id("b").value(50),
					"=",output().name("x")._for("a b")
				).oninput("x.value=parseInt(a.value)+parseInt(b.value)"),
				"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b'/></form>",
				"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b'/></form>\n",
				"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b'/></form>",
				"<form action='testform' oninput='x.value=parseInt(a.value)+parseInt(b.value)'>0<input type='range' id='a' value='50'/>+<input type='number' id='b' value='50'/>=<output name='x' for='a b'/></form>\n",
				"{_type:'form',a:{action:'testform',oninput:'x.value=parseInt(a.value)+parseInt(b.value)'},c:[0,{_type:'input',a:{type:'range',id:'a',value:50}},'+',{_type:'input',a:{type:'number',id:'b',value:50}},'=',{_type:'output',a:{name:'x','for':'a b'}}]}",
				"{t:'form',a:{action:'testform',oninput:'x.value=parseInt(a.value)+parseInt(b.value)'},c:[0,{t:'input',a:{type:'range',id:'a',value:50}},'+',{t:'input',a:{type:'number',id:'b',value:50}},'=',{t:'output',a:{name:'x','for':'a b'}}]}",
				"83A55F74797065A4666F726DA16182A6616374696F6EA874657374666F726DA76F6E696E707574D92B782E76616C75653D7061727365496E7428612E76616C7565292B7061727365496E7428622E76616C756529A163960082A55F74797065A5696E707574A16183A474797065A572616E6765A26964A161A576616C756532A12B82A55F74797065A5696E707574A16183A474797065A66E756D626572A26964A162A576616C756532A13D82A55F74797065A66F7574707574A16182A46E616D65A178A3666F72A3612062",
				"83A174A4666F726DA16182A6616374696F6EA874657374666F726DA76F6E696E707574D92B782E76616C75653D7061727365496E7428612E76616C7565292B7061727365496E7428622E76616C756529A163960082A174A5696E707574A16183A474797065A572616E6765A26964A161A576616C756532A12B82A174A5696E707574A16183A474797065A66E756D626572A26964A162A576616C756532A13D82A174A66F7574707574A16182A46E616D65A178A3666F72A3612062",
				"(_type=form,a=(action=testform,oninput='x.value=parseInt(a.value)+parseInt(b.value)'),c=@(0,(_type=input,a=(type=range,id=a,value=50)),+,(_type=input,a=(type=number,id=b,value=50)),'=',(_type=output,a=(name=x,for='a b'))))",
				"(t=form,a=(action=testform,oninput='x.value=parseInt(a.value)+parseInt(b.value)'),c=@(0,(t=input,a=(type=range,id=a,value=50)),+,(t=input,a=(type=number,id=b,value=50)),'=',(t=output,a=(name=x,for='a b'))))",
				"_type=form&a=(action=testform,oninput='x.value=parseInt(a.value)%2BparseInt(b.value)')&c=@(0,(_type=input,a=(type=range,id=a,value=50)),%2B,(_type=input,a=(type=number,id=b,value=50)),'=',(_type=output,a=(name=x,for='a+b')))",
				"t=form&a=(action=testform,oninput='x.value=parseInt(a.value)%2BparseInt(b.value)')&c=@(0,(t=input,a=(type=range,id=a,value=50)),%2B,(t=input,a=(type=number,id=b,value=50)),'=',(t=output,a=(name=x,for='a+b')))",
			},
			{
				"p",
				p("foo"),
				"<p>foo</p>",
				"<p>foo</p>\n",
				"<p>foo</p>",
				"<p>foo</p>\n",
				"{_type:'p',c:['foo']}",
				"{t:'p',c:['foo']}",
				"82A55F74797065A170A16391A3666F6F",
				"82A174A170A16391A3666F6F",
				"(_type=p,c=@(foo))",
				"(t=p,c=@(foo))",
				"_type=p&c=@(foo)",
				"t=p&c=@(foo)",
			},
			{
				"Pre",
				pre("foo   \n   bar"),
				"<pre>foo   &#x000a;   bar</pre>",
				"<pre>foo   &#x000a;   bar</pre>\n",
				"<pre>foo   \n   bar</pre>",
				"<pre>foo   \n   bar</pre>\n",	
				"{_type:'pre',c:['foo   \\n   bar']}",
				"{t:'pre',c:['foo   \\n   bar']}",
				"82A55F74797065A3707265A16391AD666F6F2020200A202020626172",
				"82A174A3707265A16391AD666F6F2020200A202020626172",
				"(_type=pre,c=@('foo   \n   bar'))",
				"(t=pre,c=@('foo   \n   bar'))",
				"_type=pre&c=@('foo+++%0A+++bar')",
				"t=pre&c=@('foo+++%0A+++bar')",
			},
			{
				"Progress",
				progress().value(1),
				"<progress value='1'/>",
				"<progress value='1'/>\n",
				"<progress value='1'/>",
				"<progress value='1'/>\n",	
				"{_type:'progress',a:{value:1}}",
				"{t:'progress',a:{value:1}}",
				"82A55F74797065A870726F6772657373A16181A576616C756501",
				"82A174A870726F6772657373A16181A576616C756501",
				"(_type=progress,a=(value=1))",
				"(t=progress,a=(value=1))",
				"_type=progress&a=(value=1)",
				"t=progress&a=(value=1)",
			},
			{
				"Q",
				p("foo",q("bar"),"baz"),
				"<p>foo<q>bar</q>baz</p>",
				"<p>foo<q>bar</q>baz</p>\n",
				"<p>foo<q>bar</q>baz</p>",
				"<p>foo<q>bar</q>baz</p>\n",	
				"{_type:'p',c:['foo',{_type:'q',c:['bar']},'baz']}",
				"{t:'p',c:['foo',{t:'q',c:['bar']},'baz']}",
				"82A55F74797065A170A16393A3666F6F82A55F74797065A171A16391A3626172A362617A",
				"82A174A170A16393A3666F6F82A174A171A16391A3626172A362617A",
				"(_type=p,c=@(foo,(_type=q,c=@(bar)),baz))",
				"(t=p,c=@(foo,(t=q,c=@(bar)),baz))",
				"_type=p&c=@(foo,(_type=q,c=@(bar)),baz)",
				"t=p&c=@(foo,(t=q,c=@(bar)),baz)",
			},
			{
				"Ruby/Rb/Rtc/Rp/Rt",
				ruby(
					"法",rb("華"),"経",rtc("き",rp("け"),"ょ")
				),
				"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
				"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>\n",
				"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>",
				"<ruby>法<rb>華</rb>経<rtc>き<rp>け</rp>ょ</rtc></ruby>\n",
				"{_type:'ruby',c:['法',{_type:'rb',c:['華']},'経',{_type:'rtc',c:['き',{_type:'rp',c:['け']},'ょ']}]}",
				"{t:'ruby',c:['法',{t:'rb',c:['華']},'経',{t:'rtc',c:['き',{t:'rp',c:['け']},'ょ']}]}",
				"82A55F74797065A472756279A16394A3E6B39582A55F74797065A27262A16391A3E88FAFA3E7B58C82A55F74797065A3727463A16393A3E3818D82A55F74797065A27270A16391A3E38191A3E38287",
				"82A174A472756279A16394A3E6B39582A174A27262A16391A3E88FAFA3E7B58C82A174A3727463A16393A3E3818D82A174A27270A16391A3E38191A3E38287",
				"(_type=ruby,c=@(法,(_type=rb,c=@(華)),経,(_type=rtc,c=@(き,(_type=rp,c=@(け)),ょ))))",
				"(t=ruby,c=@(法,(t=rb,c=@(華)),経,(t=rtc,c=@(き,(t=rp,c=@(け)),ょ))))",
				"_type=ruby&c=@(%E6%B3%95,(_type=rb,c=@(%E8%8F%AF)),%E7%B5%8C,(_type=rtc,c=@(%E3%81%8D,(_type=rp,c=@(%E3%81%91)),%E3%82%87)))",
				"t=ruby&c=@(%E6%B3%95,(t=rb,c=@(%E8%8F%AF)),%E7%B5%8C,(t=rtc,c=@(%E3%81%8D,(t=rp,c=@(%E3%81%91)),%E3%82%87)))",
			},
			{
				"S",
				p("foo",s("bar"),"baz"),
				"<p>foo<s>bar</s>baz</p>",
				"<p>foo<s>bar</s>baz</p>\n",
				"<p>foo<s>bar</s>baz</p>",
				"<p>foo<s>bar</s>baz</p>\n",
				"{_type:'p',c:['foo',{_type:'s',c:['bar']},'baz']}",
				"{t:'p',c:['foo',{t:'s',c:['bar']},'baz']}",
				"82A55F74797065A170A16393A3666F6F82A55F74797065A173A16391A3626172A362617A",
				"82A174A170A16393A3666F6F82A174A173A16391A3626172A362617A",
				"(_type=p,c=@(foo,(_type=s,c=@(bar)),baz))",
				"(t=p,c=@(foo,(t=s,c=@(bar)),baz))",
				"_type=p&c=@(foo,(_type=s,c=@(bar)),baz)",
				"t=p&c=@(foo,(t=s,c=@(bar)),baz)",
			},
			{
				"Samp",
				samp("foo"),
				"<samp>foo</samp>",
				"<samp>foo</samp>\n",
				"<samp>foo</samp>",
				"<samp>foo</samp>\n",
				"{_type:'samp',c:['foo']}",
				"{t:'samp',c:['foo']}",
				"82A55F74797065A473616D70A16391A3666F6F",
				"82A174A473616D70A16391A3666F6F",
				"(_type=samp,c=@(foo))",
				"(t=samp,c=@(foo))",
				"_type=samp&c=@(foo)",
				"t=samp&c=@(foo)",
			},
			{
				"Script",
				script("text/javascript", "\n\talert('hello world!');\n"),
				"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>",
				"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>\n",
				"<script type='text/javascript'>\n\talert('hello world!');\n</script>",
				"<script type='text/javascript'>\n\talert('hello world!');\n</script>\n",
				"{_type:'script',a:{type:'text/javascript'},c:'\\n\\talert(\\'hello world!\\');\\n'}",
				"{t:'script',a:{type:'text/javascript'},c:'\\n\\talert(\\'hello world!\\');\\n'}",
				"83A55F74797065A6736372697074A16181A474797065AF746578742F6A617661736372697074A163B90A09616C657274282768656C6C6F20776F726C642127293B0A",
				"83A174A6736372697074A16181A474797065AF746578742F6A617661736372697074A163B90A09616C657274282768656C6C6F20776F726C642127293B0A",
				"(_type=script,a=(type=text/javascript),c='\n\talert(~'hello world!~');\n')",
				"(t=script,a=(type=text/javascript),c='\n\talert(~'hello world!~');\n')",
				"_type=script&a=(type=text/javascript)&c='%0A%09alert(~'hello+world!~');%0A'",
				"t=script&a=(type=text/javascript)&c='%0A%09alert(~'hello+world!~');%0A'",
			},
			{
				"Section",
				section(h1("foo"),p("bar")),
				"<section><h1>foo</h1><p>bar</p></section>",
				"<section><h1>foo</h1><p>bar</p></section>\n",
				"<section><h1>foo</h1><p>bar</p></section>",
				"<section><h1>foo</h1><p>bar</p></section>\n",
				"{_type:'section',c:[{_type:'h1',c:['foo']},{_type:'p',c:['bar']}]}",
				"{t:'section',c:[{t:'h1',c:['foo']},{t:'p',c:['bar']}]}",
				"82A55F74797065A773656374696F6EA1639282A55F74797065A26831A16391A3666F6F82A55F74797065A170A16391A3626172",
				"82A174A773656374696F6EA1639282A174A26831A16391A3666F6F82A174A170A16391A3626172",
				"(_type=section,c=@((_type=h1,c=@(foo)),(_type=p,c=@(bar))))",
				"(t=section,c=@((t=h1,c=@(foo)),(t=p,c=@(bar))))",
				"_type=section&c=@((_type=h1,c=@(foo)),(_type=p,c=@(bar)))",
				"t=section&c=@((t=h1,c=@(foo)),(t=p,c=@(bar)))",
			},
			{
				"Select/Optgroup/Option",
				select("foo", optgroup(option("o1","v1")).label("bar")),
				"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
				"<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n",
				"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
				"<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n",
				"{_type:'select',a:{name:'foo'},c:[{_type:'optgroup',a:{label:'bar'},c:[{_type:'option',a:{value:'o1'},c:'v1'}]}]}",
				"{t:'select',a:{name:'foo'},c:[{t:'optgroup',a:{label:'bar'},c:[{t:'option',a:{value:'o1'},c:'v1'}]}]}",
				"83A55F74797065A673656C656374A16181A46E616D65A3666F6FA1639183A55F74797065A86F707467726F7570A16181A56C6162656CA3626172A1639183A55F74797065A66F7074696F6EA16181A576616C7565A26F31A163A27631",
				"83A174A673656C656374A16181A46E616D65A3666F6FA1639183A174A86F707467726F7570A16181A56C6162656CA3626172A1639183A174A66F7074696F6EA16181A576616C7565A26F31A163A27631",
				"(_type=select,a=(name=foo),c=@((_type=optgroup,a=(label=bar),c=@((_type=option,a=(value=o1),c=v1)))))",
				"(t=select,a=(name=foo),c=@((t=optgroup,a=(label=bar),c=@((t=option,a=(value=o1),c=v1)))))",
				"_type=select&a=(name=foo)&c=@((_type=optgroup,a=(label=bar),c=@((_type=option,a=(value=o1),c=v1))))",
				"t=select&a=(name=foo)&c=@((t=optgroup,a=(label=bar),c=@((t=option,a=(value=o1),c=v1))))",
			},
			{
				"Small",
				p(small("foo")),
				"<p><small>foo</small></p>",
				"<p><small>foo</small></p>\n",
				"<p><small>foo</small></p>",
				"<p><small>foo</small></p>\n",
				"{_type:'p',c:[{_type:'small',c:['foo']}]}",
				"{t:'p',c:[{t:'small',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A5736D616C6CA16391A3666F6F",
				"82A174A170A1639182A174A5736D616C6CA16391A3666F6F",
				"(_type=p,c=@((_type=small,c=@(foo))))",
				"(t=p,c=@((t=small,c=@(foo))))",
				"_type=p&c=@((_type=small,c=@(foo)))",
				"t=p&c=@((t=small,c=@(foo)))",
			},
			{
				"Span",
				p("My mother has ",span().style("color:blue").child("blue"), " eyes."),
				"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>",
				"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>\n",
				"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>",
				"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>\n",
				"{_type:'p',c:['My mother has ',{_type:'span',a:{style:'color:blue'},c:['blue']},' eyes.']}",
				"{t:'p',c:['My mother has ',{t:'span',a:{style:'color:blue'},c:['blue']},' eyes.']}",
				"82A55F74797065A170A16393AE4D79206D6F74686572206861732083A55F74797065A47370616EA16181A57374796C65AA636F6C6F723A626C7565A16391A4626C7565A620657965732E",
				"82A174A170A16393AE4D79206D6F74686572206861732083A174A47370616EA16181A57374796C65AA636F6C6F723A626C7565A16391A4626C7565A620657965732E",
				"(_type=p,c=@('My mother has ',(_type=span,a=(style=color:blue),c=@(blue)),' eyes.'))",
				"(t=p,c=@('My mother has ',(t=span,a=(style=color:blue),c=@(blue)),' eyes.'))",
				"_type=p&c=@('My+mother+has+',(_type=span,a=(style=color:blue),c=@(blue)),'+eyes.')",
				"t=p&c=@('My+mother+has+',(t=span,a=(style=color:blue),c=@(blue)),'+eyes.')",
			},
			{
				"Strong",
				p(strong("foo")),
				"<p><strong>foo</strong></p>",
				"<p><strong>foo</strong></p>\n",
				"<p><strong>foo</strong></p>",
				"<p><strong>foo</strong></p>\n",
				"{_type:'p',c:[{_type:'strong',c:['foo']}]}",
				"{t:'p',c:[{t:'strong',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A67374726F6E67A16391A3666F6F",
				"82A174A170A1639182A174A67374726F6E67A16391A3666F6F",
				"(_type=p,c=@((_type=strong,c=@(foo))))",
				"(t=p,c=@((t=strong,c=@(foo))))",
				"_type=p&c=@((_type=strong,c=@(foo)))",
				"t=p&c=@((t=strong,c=@(foo)))",
			},
			{
				"Style",
				head(style("\n\th1 {color:red;}\n\tp: {color:blue;}\n")),
				"<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>",
				"<head>\n\t<style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style>\n</head>\n",
				"<head><style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style></head>",
				"<head>\n\t<style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style>\n</head>\n",
				"{_type:'head',c:[{_type:'style',c:'\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'}]}",
				"{t:'head',c:[{t:'style',c:'\\n\\th1 {color:red;}\\n\\tp: {color:blue;}\\n'}]}",
				"82A55F74797065A468656164A1639182A55F74797065A57374796C65A163D9240A096831207B636F6C6F723A7265643B7D0A09703A207B636F6C6F723A626C75653B7D0A",
				"82A174A468656164A1639182A174A57374796C65A163D9240A096831207B636F6C6F723A7265643B7D0A09703A207B636F6C6F723A626C75653B7D0A",
				"(_type=head,c=@((_type=style,c='\n\th1 {color:red;}\n\tp: {color:blue;}\n')))",
				"(t=head,c=@((t=style,c='\n\th1 {color:red;}\n\tp: {color:blue;}\n')))",
				"_type=head&c=@((_type=style,c='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'))",
				"t=head&c=@((t=style,c='%0A%09h1+%7Bcolor:red;%7D%0A%09p:+%7Bcolor:blue;%7D%0A'))",
			},
			{
				"Sub",
				p(sub("foo")),
				"<p><sub>foo</sub></p>",
				"<p><sub>foo</sub></p>\n",
				"<p><sub>foo</sub></p>",
				"<p><sub>foo</sub></p>\n",
				"{_type:'p',c:[{_type:'sub',c:['foo']}]}",
				"{t:'p',c:[{t:'sub',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A3737562A16391A3666F6F",
				"82A174A170A1639182A174A3737562A16391A3666F6F",
				"(_type=p,c=@((_type=sub,c=@(foo))))",
				"(t=p,c=@((t=sub,c=@(foo))))",
				"_type=p&c=@((_type=sub,c=@(foo)))",
				"t=p&c=@((t=sub,c=@(foo)))",
			},
			{
				"Sup",
				p(sup("foo")),
				"<p><sup>foo</sup></p>",
				"<p><sup>foo</sup></p>\n",
				"<p><sup>foo</sup></p>",
				"<p><sup>foo</sup></p>\n",
				"{_type:'p',c:[{_type:'sup',c:['foo']}]}",
				"{t:'p',c:[{t:'sup',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A3737570A16391A3666F6F",
				"82A174A170A1639182A174A3737570A16391A3666F6F",
				"(_type=p,c=@((_type=sup,c=@(foo))))",
				"(t=p,c=@((t=sup,c=@(foo))))",
				"_type=p&c=@((_type=sup,c=@(foo)))",
				"t=p&c=@((t=sup,c=@(foo)))",
			},
			{
				"Table/Colgroup/Col/Caption/THead/TBody/TFoot/Tr/Th/Td-1",
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
				"{_type:'table',c:[{_type:'caption',c:['caption1']},{_type:'colgroup',c:[{_type:'col',a:{'class':'foo'}},{_type:'col',a:{'class':'bar'}}]},{_type:'thead',c:[{_type:'tr',c:[{_type:'th',c:['c1']},{_type:'th',c:['c2']}]}]},{_type:'tbody',c:[{_type:'tr',c:[{_type:'td',c:['v1']},{_type:'td',c:['v2']}]}]},{_type:'tfoot',c:[{_type:'tr',c:[{_type:'td',c:['f1']},{_type:'td',c:['f2']}]}]}]}",
				"{t:'table',c:[{t:'caption',c:['caption1']},{t:'colgroup',c:[{t:'col',a:{'class':'foo'}},{t:'col',a:{'class':'bar'}}]},{t:'thead',c:[{t:'tr',c:[{t:'th',c:['c1']},{t:'th',c:['c2']}]}]},{t:'tbody',c:[{t:'tr',c:[{t:'td',c:['v1']},{t:'td',c:['v2']}]}]},{t:'tfoot',c:[{t:'tr',c:[{t:'td',c:['f1']},{t:'td',c:['f2']}]}]}]}",
				"82A55F74797065A57461626C65A1639582A55F74797065A763617074696F6EA16391A863617074696F6E3182A55F74797065A8636F6C67726F7570A1639282A55F74797065A3636F6CA16181A5636C617373A3666F6F82A55F74797065A3636F6CA16181A5636C617373A362617282A55F74797065A57468656164A1639182A55F74797065A27472A1639282A55F74797065A27468A16391A2633182A55F74797065A27468A16391A2633282A55F74797065A574626F6479A1639182A55F74797065A27472A1639282A55F74797065A27464A16391A2763182A55F74797065A27464A16391A2763282A55F74797065A574666F6F74A1639182A55F74797065A27472A1639282A55F74797065A27464A16391A2663182A55F74797065A27464A16391A26632",
				"82A174A57461626C65A1639582A174A763617074696F6EA16391A863617074696F6E3182A174A8636F6C67726F7570A1639282A174A3636F6CA16181A5636C617373A3666F6F82A174A3636F6CA16181A5636C617373A362617282A174A57468656164A1639182A174A27472A1639282A174A27468A16391A2633182A174A27468A16391A2633282A174A574626F6479A1639182A174A27472A1639282A174A27464A16391A2763182A174A27464A16391A2763282A174A574666F6F74A1639182A174A27472A1639282A174A27464A16391A2663182A174A27464A16391A26632",
				"(_type=table,c=@((_type=caption,c=@(caption1)),(_type=colgroup,c=@((_type=col,a=(class=foo)),(_type=col,a=(class=bar)))),(_type=thead,c=@((_type=tr,c=@((_type=th,c=@(c1)),(_type=th,c=@(c2)))))),(_type=tbody,c=@((_type=tr,c=@((_type=td,c=@(v1)),(_type=td,c=@(v2)))))),(_type=tfoot,c=@((_type=tr,c=@((_type=td,c=@(f1)),(_type=td,c=@(f2))))))))",
				"(t=table,c=@((t=caption,c=@(caption1)),(t=colgroup,c=@((t=col,a=(class=foo)),(t=col,a=(class=bar)))),(t=thead,c=@((t=tr,c=@((t=th,c=@(c1)),(t=th,c=@(c2)))))),(t=tbody,c=@((t=tr,c=@((t=td,c=@(v1)),(t=td,c=@(v2)))))),(t=tfoot,c=@((t=tr,c=@((t=td,c=@(f1)),(t=td,c=@(f2))))))))",
				"_type=table&c=@((_type=caption,c=@(caption1)),(_type=colgroup,c=@((_type=col,a=(class=foo)),(_type=col,a=(class=bar)))),(_type=thead,c=@((_type=tr,c=@((_type=th,c=@(c1)),(_type=th,c=@(c2)))))),(_type=tbody,c=@((_type=tr,c=@((_type=td,c=@(v1)),(_type=td,c=@(v2)))))),(_type=tfoot,c=@((_type=tr,c=@((_type=td,c=@(f1)),(_type=td,c=@(f2)))))))",
				"t=table&c=@((t=caption,c=@(caption1)),(t=colgroup,c=@((t=col,a=(class=foo)),(t=col,a=(class=bar)))),(t=thead,c=@((t=tr,c=@((t=th,c=@(c1)),(t=th,c=@(c2)))))),(t=tbody,c=@((t=tr,c=@((t=td,c=@(v1)),(t=td,c=@(v2)))))),(t=tfoot,c=@((t=tr,c=@((t=td,c=@(f1)),(t=td,c=@(f2)))))))",
			},
			{
				"Template",
				template("foo",div("bar")),
				"<template id='foo'><div>bar</div></template>",
				"<template id='foo'><div>bar</div></template>\n",
				"<template id='foo'><div>bar</div></template>",
				"<template id='foo'><div>bar</div></template>\n",
				"{_type:'template',a:{id:'foo'},c:[{_type:'div',c:['bar']}]}",
				"{t:'template',a:{id:'foo'},c:[{t:'div',c:['bar']}]}",
				"83A55F74797065A874656D706C617465A16181A26964A3666F6FA1639182A55F74797065A3646976A16391A3626172",
				"83A174A874656D706C617465A16181A26964A3666F6FA1639182A174A3646976A16391A3626172",
				"(_type=template,a=(id=foo),c=@((_type=div,c=@(bar))))",
				"(t=template,a=(id=foo),c=@((t=div,c=@(bar))))",
				"_type=template&a=(id=foo)&c=@((_type=div,c=@(bar)))",
				"t=template&a=(id=foo)&c=@((t=div,c=@(bar)))",
			},
			{
				"Textarea",
				textarea("foo", "bar"),
				"<textarea name='foo'>bar</textarea>",
				"<textarea name='foo'>bar</textarea>\n",
				"<textarea name='foo'>bar</textarea>",
				"<textarea name='foo'>bar</textarea>\n",
				"{_type:'textarea',a:{name:'foo'},c:'bar'}",
				"{t:'textarea',a:{name:'foo'},c:'bar'}",
				"83A55F74797065A87465787461726561A16181A46E616D65A3666F6FA163A3626172",
				"83A174A87465787461726561A16181A46E616D65A3666F6FA163A3626172",
				"(_type=textarea,a=(name=foo),c=bar)",
				"(t=textarea,a=(name=foo),c=bar)",
				"_type=textarea&a=(name=foo)&c=bar",
				"t=textarea&a=(name=foo)&c=bar",
			},
			{	
				"Time",
				p("I have a date on ",time("Valentines day").datetime("2016-02-14 18:00"), "."),
				"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
				"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n",
				"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
				"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n",
				"{_type:'p',c:['I have a date on ',{_type:'time',a:{datetime:'2016-02-14 18:00'},c:['Valentines day']},'.']}",
				"{t:'p',c:['I have a date on ',{t:'time',a:{datetime:'2016-02-14 18:00'},c:['Valentines day']},'.']}",
				"82A55F74797065A170A16393B149206861766520612064617465206F6E2083A55F74797065A474696D65A16181A86461746574696D65B0323031362D30322D31342031383A3030A16391AE56616C656E74696E657320646179A12E",
				"82A174A170A16393B149206861766520612064617465206F6E2083A174A474696D65A16181A86461746574696D65B0323031362D30322D31342031383A3030A16391AE56616C656E74696E657320646179A12E",
				"(_type=p,c=@('I have a date on ',(_type=time,a=(datetime='2016-02-14 18:00'),c=@('Valentines day')),.))",
				"(t=p,c=@('I have a date on ',(t=time,a=(datetime='2016-02-14 18:00'),c=@('Valentines day')),.))",
				"_type=p&c=@('I+have+a+date+on+',(_type=time,a=(datetime='2016-02-14+18:00'),c=@('Valentines+day')),.)",
				"t=p&c=@('I+have+a+date+on+',(t=time,a=(datetime='2016-02-14+18:00'),c=@('Valentines+day')),.)",
			},
			{
				"U",
				p(u("foo")),
				"<p><u>foo</u></p>",
				"<p><u>foo</u></p>\n",
				"<p><u>foo</u></p>",
				"<p><u>foo</u></p>\n",
				"{_type:'p',c:[{_type:'u',c:['foo']}]}",
				"{t:'p',c:[{t:'u',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A175A16391A3666F6F",
				"82A174A170A1639182A174A175A16391A3666F6F",
				"(_type=p,c=@((_type=u,c=@(foo))))",
				"(t=p,c=@((t=u,c=@(foo))))",
				"_type=p&c=@((_type=u,c=@(foo)))",
				"t=p&c=@((t=u,c=@(foo)))",
			},
			{
				"Ul/Li",
				ul(li("foo")),
				"<ul><li>foo</li></ul>",
				"<ul>\n\t<li>foo</li>\n</ul>\n",
				"<ul><li>foo</li></ul>",
				"<ul>\n\t<li>foo</li>\n</ul>\n",
				"{_type:'ul',c:[{_type:'li',c:['foo']}]}",
				"{t:'ul',c:[{t:'li',c:['foo']}]}",
				"82A55F74797065A2756CA1639182A55F74797065A26C69A16391A3666F6F",
				"82A174A2756CA1639182A174A26C69A16391A3666F6F",
				"(_type=ul,c=@((_type=li,c=@(foo))))",
				"(t=ul,c=@((t=li,c=@(foo))))",
				"_type=ul&c=@((_type=li,c=@(foo)))",
				"t=ul&c=@((t=li,c=@(foo)))",
			},
			{
				"Var",
				p(var("foo")),
				"<p><var>foo</var></p>",
				"<p><var>foo</var></p>\n",
				"<p><var>foo</var></p>",
				"<p><var>foo</var></p>\n",
				"{_type:'p',c:[{_type:'var',c:['foo']}]}",
				"{t:'p',c:[{t:'var',c:['foo']}]}",
				"82A55F74797065A170A1639182A55F74797065A3766172A16391A3666F6F",
				"82A174A170A1639182A174A3766172A16391A3666F6F",
				"(_type=p,c=@((_type=var,c=@(foo))))",
				"(t=p,c=@((t=var,c=@(foo))))",
				"_type=p&c=@((_type=var,c=@(foo)))",
				"t=p&c=@((t=var,c=@(foo)))",
			},
			{
				"Video/Source/Track",
				video().width(100).height(200).controls(true).children(
					source("foo.mp4", "video/mp4"),
					track("subtitles_en.vtt", "subtitles").srclang("en")
				),
				"<video width='100' height='200' controls='true'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
				"<video width='100' height='200' controls='true'>\n\t<source src='foo.mp4' type='video/mp4'/>\n\t<track src='subtitles_en.vtt' kind='subtitles' srclang='en'/>\n</video>\n",
				"<video width='100' height='200' controls='true'><source src='foo.mp4' type='video/mp4'/><track src='subtitles_en.vtt' kind='subtitles' srclang='en'/></video>",
				"<video width='100' height='200' controls='true'>\n\t<source src='foo.mp4' type='video/mp4'/>\n\t<track src='subtitles_en.vtt' kind='subtitles' srclang='en'/>\n</video>\n",
				"{_type:'video',a:{width:100,height:200,controls:true},c:[{_type:'source',a:{src:'foo.mp4',type:'video/mp4'}},{_type:'track',a:{src:'subtitles_en.vtt',kind:'subtitles',srclang:'en'}}]}",
				"{t:'video',a:{width:100,height:200,controls:true},c:[{t:'source',a:{src:'foo.mp4',type:'video/mp4'}},{t:'track',a:{src:'subtitles_en.vtt',kind:'subtitles',srclang:'en'}}]}",
				"83A55F74797065A5766964656FA16183A5776964746864A6686569676874D100C8A8636F6E74726F6C73C3A1639282A55F74797065A6736F75726365A16182A3737263A7666F6F2E6D7034A474797065A9766964656F2F6D703482A55F74797065A5747261636BA16183A3737263B07375627469746C65735F656E2E767474A46B696E64A97375627469746C6573A77372636C616E67A2656E",
				"83A174A5766964656FA16183A5776964746864A6686569676874D100C8A8636F6E74726F6C73C3A1639282A174A6736F75726365A16182A3737263A7666F6F2E6D7034A474797065A9766964656F2F6D703482A174A5747261636BA16183A3737263B07375627469746C65735F656E2E767474A46B696E64A97375627469746C6573A77372636C616E67A2656E",
				"(_type=video,a=(width=100,height=200,controls=true),c=@((_type=source,a=(src=foo.mp4,type=video/mp4)),(_type=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en))))",
				"(t=video,a=(width=100,height=200,controls=true),c=@((t=source,a=(src=foo.mp4,type=video/mp4)),(t=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en))))",
				"_type=video&a=(width=100,height=200,controls=true)&c=@((_type=source,a=(src=foo.mp4,type=video/mp4)),(_type=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en)))",
				"t=video&a=(width=100,height=200,controls=true)&c=@((t=source,a=(src=foo.mp4,type=video/mp4)),(t=track,a=(src=subtitles_en.vtt,kind=subtitles,srclang=en)))",
			},
			{
				"Wbr",
				p("foo",wbr(),"bar"),
				"<p>foo<wbr/>bar</p>",
				"<p>foo<wbr/>bar</p>\n",
				"<p>foo<wbr/>bar</p>",
				"<p>foo<wbr/>bar</p>\n",
				"{_type:'p',c:['foo',{_type:'wbr'},'bar']}",
				"{t:'p',c:['foo',{t:'wbr'},'bar']}",
				"82A55F74797065A170A16393A3666F6F81A55F74797065A3776272A3626172",
				"82A174A170A16393A3666F6F81A174A3776272A3626172",
				"(_type=p,c=@(foo,(_type=wbr),bar))",
				"(t=p,c=@(foo,(t=wbr),bar))",
				"_type=p&c=@(foo,(_type=wbr),bar)",
				"t=p&c=@(foo,(t=wbr),bar)",
			},
		});
	}


	private String label, xml, xmlr, html, htmlr, json, jsonT, msgPack, msgPackT, uon, uonT, urlEncoding, urlEncodingT;
	private Object in;

	public BasicHtmlSchemaTest(String label, Object in, String xml, String xmlr, String html, String htmlr, String json, String jsonT, String msgPack, String msgPackT, String uon, String uonT, String urlEncoding, String urlEncodingT) throws Exception {
		this.label = label;
		this.in = in;
		this.xml = xml;
		this.xmlr = xmlr;
		this.html = html;
		this.htmlr = htmlr;
		this.json = json;
		this.jsonT = jsonT;
		this.msgPack = msgPack;
		this.msgPackT = msgPackT;
		this.uon = uon;
		this.uonT = uonT;
		this.urlEncoding = urlEncoding;
		this.urlEncodingT = urlEncodingT;
	}

	private void testSerialize(WriterSerializer s, String expected) throws Exception {
		try {
			String r = s.serialize(in);
			if (expected.equals("xxx"))
				System.out.println(label + "=\t" + r); // NOT DEBUG
			assertEquals(label + " serialize-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
		}
	}

	private void testParse(WriterSerializer s, ReaderParser p, String expected) throws Exception {
		try {
			String r = s.serialize(in);
			Object o = p.parse(r, in == null ? Object.class : in.getClass());
			r = s.serialize(o);
			assertEquals(label + " parse-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
		}
	}

	private void testSerialize(OutputStreamSerializer s, String expected) throws Exception {
		try {
			String r = s.serializeToHex(in);
			if (expected.equals("xxx"))
				System.out.println(label + "=\t" + r); // NOT DEBUG
			assertEquals(label + " serialize-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
		}
	}

	private void testParse(OutputStreamSerializer s, InputStreamParser p, String expected) throws Exception {
		try {
			String r = s.serializeToHex(in);
			Object o = p.parse(r, in == null ? Object.class : in.getClass());
			r = s.serializeToHex(o);
			assertEquals(label + " parse-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
		}
	}

	private void testParseJsonEquivalency(OutputStreamSerializer s, InputStreamParser p, String expected) throws Exception {
		try {
			String r = s.serializeToHex(in);
			Object o = p.parse(r, in == null ? Object.class : in.getClass());
			r = sJson.serialize(o);
			assertEquals(label + " parse-normal failed on JSON equivalency", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed.  exception=" + e.getLocalizedMessage());
		}
	}

	@Test
	public void serializeXmlDefaultSq() throws Exception {
		testSerialize(sXmlSq, xml);
	}

	@Test
	public void parseXmlDefaultSq() throws Exception {
		testParse(sXmlSq, pXml, xml);
	}

	@Test
	public void serializeXmlDefaultNsSq() throws Exception {
		testSerialize(sXmlNsSq, xml);
	}

	@Test
	public void parseXmlDefaultNsSq() throws Exception {
		testParse(sXmlNsSq, pXml, xml);
	}

	@Test
	public void serializeHtmlDefaultSq() throws Exception {
		testSerialize(sHtmlSq, html);
	}

	@Test
	public void parseHtmlDefaultSq() throws Exception {
		testParse(sHtmlSq, pHtml, html);
	}

	@Test
	public void serializeXmlDefaultSqReadable() throws Exception {
		testSerialize(sXmlSqReadable, xmlr);
	}

	@Test
	public void parseXmlDefaultSqReadable() throws Exception {
		testParse(sXmlSqReadable, pXml, xmlr);
	}

	@Test
	public void serializeHtmlDefaultSqReadable() throws Exception {
		testSerialize(sHtmlSqReadable, htmlr);
	}

	@Test
	public void parseHtmlDefaultSqReadable() throws Exception {
		testParse(sHtmlSqReadable, pHtml, htmlr);
	}

	@Test
	public void serializeJson() throws Exception {
		testSerialize(sJson, json);
	}

	@Test
	public void parseJson() throws Exception {
		testParse(sJson, pJson, json);
	}

	@Test
	public void serializeJsonT() throws Exception {
		testSerialize(sJsonT, jsonT);
	}

	@Test
	public void parseJsonT() throws Exception {
		testParse(sJsonT, pJsonT, jsonT);
	}

	@Test
	public void serializeMsgPack() throws Exception {
		testSerialize(sMsgPack, msgPack);
	}

	@Test
	public void parseMsgPack() throws Exception {
		testParse(sMsgPack, pMsgPack, msgPack);
	}

	@Test
	public void parseMsgPackJsonEquivalency() throws Exception {
		testParseJsonEquivalency(sMsgPack, pMsgPack, json);
	}

	@Test
	public void serializeMsgPackT() throws Exception {
		testSerialize(sMsgPackT, msgPackT);
	}

	@Test
	public void parseMsgPackT() throws Exception {
		testParse(sMsgPackT, pMsgPackT, msgPackT);
	}

	@Test
	public void parseMsgPackTJsonEquivalency() throws Exception {
		testParseJsonEquivalency(sMsgPackT, pMsgPackT, json);
	}

	@Test
	public void serializeUon() throws Exception {
		testSerialize(sUon, uon);
	}

	@Test
	public void parseUon() throws Exception {
		testParse(sUon, pUon, uon);
	}

	@Test
	public void serializeUonT() throws Exception {
		testSerialize(sUonT, uonT);
	}

	@Test
	public void parseUonT() throws Exception {
		testParse(sUonT, pUonT, uonT);
	}

	@Test
	public void serializeUrlEncoding() throws Exception {
		testSerialize(sUrlEncoding, urlEncoding);
	}

	@Test
	public void parseUrlEncoding() throws Exception {
		testParse(sUrlEncoding, pUrlEncoding, urlEncoding);
	}

	@Test
	public void serializeUrlEncodingT() throws Exception {
		testSerialize(sUrlEncodingT, urlEncodingT);
	}

	@Test
	public void parseUrlEncodingT() throws Exception {
		testParse(sUrlEncodingT, pUrlEncodingT, urlEncodingT);
	}
}
