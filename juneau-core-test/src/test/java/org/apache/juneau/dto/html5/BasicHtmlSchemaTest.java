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
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
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
		sHtmlSqReadable = HtmlSerializer.DEFAULT_SQ_READABLE;

	private static final ReaderParser
		pXml = XmlParser.DEFAULT,
		pHtml = HtmlParser.DEFAULT;

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
			},
			{
				"A-2",
				a("http://foo", "bar", btag, "baz"),
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>",
				"<a href='http://foo'>bar<b>bbb</b>baz</a>\n",
			},
			{
				"A-3",
				a("http://foo", ""),
				"<a href='http://foo'>_xE000_</a>",
				"<a href='http://foo'>_xE000_</a>\n",
				"<a href='http://foo'><sp/></a>",
				"<a href='http://foo'><sp/></a>\n",
			},
			{
				"A-4",
				a("http://foo", " "),
				"<a href='http://foo'>_x0020_</a>",
				"<a href='http://foo'>_x0020_</a>\n",
				"<a href='http://foo'><sp> </sp></a>",
				"<a href='http://foo'><sp> </sp></a>\n",
			},
			{
				"A-5",
				a("http://foo"),
				"<a href='http://foo'/>",
				"<a href='http://foo'/>\n",
				"<a href='http://foo'/>",
				"<a href='http://foo'/>\n",
			},
			{
				"Abbr-1",
				abbr().children("foo"),
				"<abbr>foo</abbr>",
				"<abbr>foo</abbr>\n",
				"<abbr>foo</abbr>",
				"<abbr>foo</abbr>\n",
			},
			{
				"Abbr-2",
				abbr("foo", "bar", btag, "baz"),
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n",
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>",
				"<abbr title='foo'>bar<b>bbb</b>baz</abbr>\n",
			},
			{
				"Address-1",
				address(),
				"<address/>",
				"<address/>\n",
				"<address/>",
				"<address/>\n",
			},
			{
				"Address-2",
				address(""),
				"<address>_xE000_</address>",
				"<address>_xE000_</address>\n",
				"<address><sp/></address>",
				"<address><sp/></address>\n",
			},
			{
				"Address-3",
				address("foo", a("bar", "baz"), a("qux", "quux")),
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n",
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>",
				"<address>foo<a href='bar'>baz</a><a href='qux'>quux</a></address>\n",
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
			},
			{
				"Bdi-1",
				p("foo", bdi("إيان"), "bar"),
				"<p>foo<bdi>إيان</bdi>bar</p>",
				"<p>foo<bdi>إيان</bdi>bar</p>\n",
				"<p>foo<bdi>إيان</bdi>bar</p>",
				"<p>foo<bdi>إيان</bdi>bar</p>\n",
			},
			{
				"Bdo-1",
				p("foo", bdo("rtl", "baz"), "bar"),
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n",
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>",
				"<p>foo<bdo dir='rtl'>baz</bdo>bar</p>\n",
			},
			{
				"Blockquote-1",
				blockquote("foo"),
				"<blockquote>foo</blockquote>",
				"<blockquote>foo</blockquote>\n",
				"<blockquote>foo</blockquote>",
				"<blockquote>foo</blockquote>\n",
			},
			{
				"Br-1",
				br(),
				"<br/>",
				"<br/>\n",
				"<br/>",
				"<br/>\n",
			},
			{
				"Br-2",
				p(br()),
				"<p><br/></p>",
				"<p><br/></p>\n",
				"<p><br/></p>",
				"<p><br/></p>\n",
			},
			{
				"Button-1",
				button("button", "foo"),
				"<button type='button'>foo</button>",
				"<button type='button'>foo</button>\n",
				"<button type='button'>foo</button>",
				"<button type='button'>foo</button>\n",
			},
			{
				"Canvas-1",
				canvas(100, 200),
				"<canvas width='100' height='200'/>",
				"<canvas width='100' height='200'/>\n",
				"<canvas width='100' height='200'/>",
				"<canvas width='100' height='200'/>\n",
			},
			{
				"Cite-1",
				p(cite("foo")),
				"<p><cite>foo</cite></p>",
				"<p><cite>foo</cite></p>\n",
				"<p><cite>foo</cite></p>",
				"<p><cite>foo</cite></p>\n",
			},
			{
				"Cite-1",
				p(cite("foo")),
				"<p><cite>foo</cite></p>",
				"<p><cite>foo</cite></p>\n",
				"<p><cite>foo</cite></p>",
				"<p><cite>foo</cite></p>\n",
			},
			{
				"Code-1",
				code("foo\n\tbar"),
				"<code>foo&#x000a;&#x0009;bar</code>",
				"<code>foo&#x000a;&#x0009;bar</code>\n",
				"<code>foo<br/><sp>&#x2003;</sp>bar</code>",
				"<code>foo<br/><sp>&#x2003;</sp>bar</code>\n",
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
			},
			{
				"Del/Ins",
				p(del("foo",btag,"bar"),ins("baz")),
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n",
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>",
				"<p><del>foo<b>bbb</b>bar</del><ins>baz</ins></p>\n",
			},
			{
				"Dfn",
				p(dfn("foo")),
				"<p><dfn>foo</dfn></p>",
				"<p><dfn>foo</dfn></p>\n",
				"<p><dfn>foo</dfn></p>",
				"<p><dfn>foo</dfn></p>\n",
			},
			{
				"Div",
				div("foo",btag,"bar"),
				"<div>foo<b>bbb</b>bar</div>",
				"<div>foo<b>bbb</b>bar</div>\n",
				"<div>foo<b>bbb</b>bar</div>",
				"<div>foo<b>bbb</b>bar</div>\n",
			},
			{
				"Em",
				p("foo",em("bar"),"baz"),
				"<p>foo<em>bar</em>baz</p>",
				"<p>foo<em>bar</em>baz</p>\n",
				"<p>foo<em>bar</em>baz</p>",
				"<p>foo<em>bar</em>baz</p>\n",
			},
			{
				"Embed",
				embed("foo.swf"),
				"<embed src='foo.swf'/>",
				"<embed src='foo.swf'/>\n",
				"<embed src='foo.swf'/>",
				"<embed src='foo.swf'/>\n",
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
			},
			{
				"Hr",
				p(hr()),
				"<p><hr/></p>",
				"<p><hr/></p>\n",
				"<p><hr/></p>",
				"<p><hr/></p>\n",
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
			},
			{
				"I",
				p(i("foo")),
				"<p><i>foo</i></p>",
				"<p><i>foo</i></p>\n",
				"<p><i>foo</i></p>",
				"<p><i>foo</i></p>\n",
			},
			{
				"Iframe",
				iframe("foo"),
				"<iframe>foo</iframe>",
				"<iframe>foo</iframe>\n",
				"<iframe>foo</iframe>",
				"<iframe>foo</iframe>\n",
			},
			{
				"Kbd",
				p(kbd("foo")),
				"<p><kbd>foo</kbd></p>",
				"<p><kbd>foo</kbd></p>\n",
				"<p><kbd>foo</kbd></p>",
				"<p><kbd>foo</kbd></p>\n",
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
			},
			{
				"Map/Area-1",
				map(area("rect", "0,1,2,3", "foo").alt("bar")).name("baz"),
				"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
				"<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n",
				"<map name='baz'><area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/></map>",
				"<map name='baz'>\n\t<area shape='rect' coords='0,1,2,3' href='foo' alt='bar'/>\n</map>\n",
			},
			{
				"Mark",
				p(mark("foo")),
				"<p><mark>foo</mark></p>",
				"<p><mark>foo</mark></p>\n",
				"<p><mark>foo</mark></p>",
				"<p><mark>foo</mark></p>\n",
			},
			{
				"Meter",
				meter("foo").value(1).min(0).max(2),
				"<meter value='1' min='0' max='2'>foo</meter>",
				"<meter value='1' min='0' max='2'>foo</meter>\n",
				"<meter value='1' min='0' max='2'>foo</meter>",
				"<meter value='1' min='0' max='2'>foo</meter>\n",
			},
			{
				"Nav",
				nav(a("foo","bar")),
				"<nav><a href='foo'>bar</a></nav>",
				"<nav><a href='foo'>bar</a></nav>\n",
				"<nav><a href='foo'>bar</a></nav>",
				"<nav><a href='foo'>bar</a></nav>\n",
			},
			{
				"Noscript",
				noscript("No script!"),
				"<noscript>No script!</noscript>",
				"<noscript>No script!</noscript>\n",
				"<noscript>No script!</noscript>",
				"<noscript>No script!</noscript>\n",
			},
			{
				"Object/Param",
				object().width(1).height(2).data("foo.swf").child(param("autoplay",true)),
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n",
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>",
				"<object width='1' height='2' data='foo.swf'><param name='autoplay' value='true'/></object>\n",
			},
			{
				"Ol/Li",
				ol(li("foo")),
				"<ol><li>foo</li></ol>",
				"<ol>\n\t<li>foo</li>\n</ol>\n",
				"<ol><li>foo</li></ol>",
				"<ol>\n\t<li>foo</li>\n</ol>\n",
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
			},
			{
				"p",
				p("foo"),
				"<p>foo</p>",
				"<p>foo</p>\n",
				"<p>foo</p>",
				"<p>foo</p>\n",
			},
			{
				"Pre",
				pre("foo   \n   bar"),
				"<pre>foo   &#x000a;   bar</pre>",
				"<pre>foo   &#x000a;   bar</pre>\n",
				"<pre>foo   \n   bar</pre>",
				"<pre>foo   \n   bar</pre>\n",	
			},
			{
				"Progress",
				progress().value(1),
				"<progress value='1'/>",
				"<progress value='1'/>\n",
				"<progress value='1'/>",
				"<progress value='1'/>\n",	
			},
			{
				"Q",
				p("foo",q("bar"),"baz"),
				"<p>foo<q>bar</q>baz</p>",
				"<p>foo<q>bar</q>baz</p>\n",
				"<p>foo<q>bar</q>baz</p>",
				"<p>foo<q>bar</q>baz</p>\n",	
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
			},
			{
				"S",
				p("foo",s("bar"),"baz"),
				"<p>foo<s>bar</s>baz</p>",
				"<p>foo<s>bar</s>baz</p>\n",
				"<p>foo<s>bar</s>baz</p>",
				"<p>foo<s>bar</s>baz</p>\n",
			},
			{
				"Samp",
				samp("foo"),
				"<samp>foo</samp>",
				"<samp>foo</samp>\n",
				"<samp>foo</samp>",
				"<samp>foo</samp>\n",
			},
			{
				"Script",
				script("text/javascript", "\n\talert('hello world!');\n"),
				"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>",
				"<script type='text/javascript'>&#x000a;&#x0009;alert('hello world!');&#x000a;</script>\n",
				"<script type='text/javascript'>\n\talert('hello world!');\n</script>",
				"<script type='text/javascript'>\n\talert('hello world!');\n</script>\n",
			},
			{
				"Section",
				section(h1("foo"),p("bar")),
				"<section><h1>foo</h1><p>bar</p></section>",
				"<section><h1>foo</h1><p>bar</p></section>\n",
				"<section><h1>foo</h1><p>bar</p></section>",
				"<section><h1>foo</h1><p>bar</p></section>\n",
			},
			{
				"Select/Optgroup/Option",
				select("foo", optgroup(option("o1","v1")).label("bar")),
				"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
				"<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n",
				"<select name='foo'><optgroup label='bar'><option value='o1'>v1</option></optgroup></select>",
				"<select name='foo'>\n\t<optgroup label='bar'>\n\t\t<option value='o1'>v1</option>\n\t</optgroup>\n</select>\n",
			},
			{
				"Small",
				p(small("foo")),
				"<p><small>foo</small></p>",
				"<p><small>foo</small></p>\n",
				"<p><small>foo</small></p>",
				"<p><small>foo</small></p>\n",
			},
			{
				"Span",
				p("My mother has ",span().style("color:blue").child("blue"), " eyes."),
				"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>",
				"<p>My mother has_x0020_<span style='color:blue'>blue</span>_x0020_eyes.</p>\n",
				"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>",
				"<p>My mother has<sp> </sp><span style='color:blue'>blue</span><sp> </sp>eyes.</p>\n",
			},
			{
				"Strong",
				p(strong("foo")),
				"<p><strong>foo</strong></p>",
				"<p><strong>foo</strong></p>\n",
				"<p><strong>foo</strong></p>",
				"<p><strong>foo</strong></p>\n",
			},
			{
				"Style",
				head(style("\n\th1 {color:red;}\n\tp: {color:blue;}\n")),
				"<head><style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style></head>",
				"<head>\n\t<style>&#x000a;&#x0009;h1 {color:red;}&#x000a;&#x0009;p: {color:blue;}&#x000a;</style>\n</head>\n",
				"<head><style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style></head>",
				"<head>\n\t<style>\n\th1 {color:red;}\n\tp: {color:blue;}\n</style>\n</head>\n",
			},
			{
				"Sub",
				p(sub("foo")),
				"<p><sub>foo</sub></p>",
				"<p><sub>foo</sub></p>\n",
				"<p><sub>foo</sub></p>",
				"<p><sub>foo</sub></p>\n",
			},
			{
				"Sup",
				p(sup("foo")),
				"<p><sup>foo</sup></p>",
				"<p><sup>foo</sup></p>\n",
				"<p><sup>foo</sup></p>",
				"<p><sup>foo</sup></p>\n",
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
			},
			{
				"Template",
				template("foo",div("bar")),
				"<template id='foo'><div>bar</div></template>",
				"<template id='foo'><div>bar</div></template>\n",
				"<template id='foo'><div>bar</div></template>",
				"<template id='foo'><div>bar</div></template>\n",
			},
			{
				"Textarea",
				textarea("foo", "bar"),
				"<textarea name='foo'>bar</textarea>",
				"<textarea name='foo'>bar</textarea>\n",
				"<textarea name='foo'>bar</textarea>",
				"<textarea name='foo'>bar</textarea>\n",
			},
			{	
				"Time",
				p("I have a date on ",time("Valentines day").datetime("2016-02-14 18:00"), "."),
				"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
				"<p>I have a date on_x0020_<time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n",
				"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>",
				"<p>I have a date on<sp> </sp><time datetime='2016-02-14 18:00'>Valentines day</time>.</p>\n",
			},
			{
				"U",
				p(u("foo")),
				"<p><u>foo</u></p>",
				"<p><u>foo</u></p>\n",
				"<p><u>foo</u></p>",
				"<p><u>foo</u></p>\n",
			},
			{
				"Ul/Li",
				ul(li("foo")),
				"<ul><li>foo</li></ul>",
				"<ul>\n\t<li>foo</li>\n</ul>\n",
				"<ul><li>foo</li></ul>",
				"<ul>\n\t<li>foo</li>\n</ul>\n",
			},
			{
				"Var",
				p(var("foo")),
				"<p><var>foo</var></p>",
				"<p><var>foo</var></p>\n",
				"<p><var>foo</var></p>",
				"<p><var>foo</var></p>\n",
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
			},
			{
				"Wbr",
				p("foo",wbr(),"bar"),
				"<p>foo<wbr/>bar</p>",
				"<p>foo<wbr/>bar</p>\n",
				"<p>foo<wbr/>bar</p>",
				"<p>foo<wbr/>bar</p>\n",
			},
		});
	}


	private String label, xml, xmlr, html, htmlr;
	private Object in;

	public BasicHtmlSchemaTest(String label, Object in, String xml, String xmlr, String html, String htmlr) throws Exception {
		this.label = label;
		this.in = in;
		this.xml = xml;
		this.xmlr = xmlr;
		this.html = html;
		this.htmlr = htmlr;
	}

	private void testSerialize(WriterSerializer s, String expected) throws Exception {
		try {
			String r = s.serialize(in);
			assertEquals(label + " serialize-normal failed", expected, r);
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(label + " test failed", e);
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
			throw new AssertionError(label + " test failed", e);
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
}
