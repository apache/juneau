/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau;

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Exhaustive serialization tests for maximum indentation.
 */
@SuppressWarnings({"serial"})
class MaxIndent_Test extends TestBase {

	private static Input input(int index, String label, Object bean, int maxDepth, String json, String xml, String html, String uon, String urlEnc) {
		return new Input("["+index+"] " + label, bean, maxDepth, json, xml, html, uon, urlEnc);
	}

	private static final Input[] INPUTS = {
		input(1, "List1dOfBeans-0", new List1dOfBeans().init1(), 0,
			/* Json */		"[{a:1, b:'foo'}]",
			/* Xml */		"<array><object><a>1</a><b>foo</b></object></array>\n",
			/* Html */		"<table _type='array'><tr><th>a</th><th>b</th></tr><tr><td>1</td><td>foo</td></tr></table>\n",
			/* Uon */		"@((a=1,b=foo))",
			/* UrlEnc */	"0=(a=1,b=foo)"),
		input(2, "List1dOfBeans-1", new List1dOfBeans().init1(), 1,
			/* Json */		"[\n\t{a:1, b:'foo'}\n]",
			/* Xml */		"<array>\n\t<object><a>1</a><b>foo</b></object>\n</array>\n",
			/* Html */		"<table _type='array'>\n\t<tr><th>a</th><th>b</th></tr>\n\t<tr><td>1</td><td>foo</td></tr>\n</table>\n",
			/* Uon */		"@(\n\t(a=1,b=foo)\n)",
			/* UrlEnc */	"0=(\n\ta=1,\n\tb=foo\n)"),
		input(3, "List1dOfBeans-2", new List1dOfBeans().init1(), 2,
			/* Json */		"[\n\t{\n\t\ta: 1,\n\t\tb: 'foo'\n\t}\n]",
			/* Xml */		"<array>\n\t<object>\n\t\t<a>1</a>\n\t\t<b>foo</b>\n\t</object>\n</array>\n",
			/* Html */		"<table _type='array'>\n\t<tr>\n\t\t<th>a</th>\n\t\t<th>b</th>\n\t</tr>\n\t<tr>\n\t\t<td>1</td>\n\t\t<td>foo</td>\n\t</tr>\n</table>\n",
			/* Uon */		"@(\n\t(\n\t\ta=1,\n\t\tb=foo\n\t)\n)",
			/* UrlEnc */	"0=(\n\ta=1,\n\tb=foo\n)"),
		input(4, "List2dOfBeans-0", new List2dOfBeans().init2(), 0,
			/* Json */		"[[{a:1, b:'foo'}]]",
			/* Xml */		"<array><array><object><a>1</a><b>foo</b></object></array></array>\n",
			/* Html */		"<ul><li><table _type='array'><tr><th>a</th><th>b</th></tr><tr><td>1</td><td>foo</td></tr></table></li></ul>\n",
			/* Uon */		"@(@((a=1,b=foo)))",
			/* UrlEnc */	"0=@((a=1,b=foo))"),
		input(5, "List2dOfBeans-1", new List2dOfBeans().init2(), 1,
			/* Json */		"[\n\t[{a:1, b:'foo'}]\n]",
			/* Xml */		"<array>\n\t<array><object><a>1</a><b>foo</b></object></array>\n</array>\n",
			/* Html */		"<ul>\n\t<li><table _type='array'><tr><th>a</th><th>b</th></tr><tr><td>1</td><td>foo</td></tr></table></li>\n</ul>\n",
			/* Uon */		"@(\n\t@((a=1,b=foo))\n)",
			/* UrlEnc */	"0=@(\n\t(a=1,b=foo)\n)"),
		input(6, "List2dOfBeans-2", new List2dOfBeans().init2(), 2,
			/* Json */		"[\n\t[\n\t\t{a:1, b:'foo'}\n\t]\n]",
			/* Xml */		"<array>\n\t<array>\n\t\t<object><a>1</a><b>foo</b></object>\n\t</array>\n</array>\n",
			/* Html */		"<ul>\n\t<li>\n\t\t<table _type='array'><tr><th>a</th><th>b</th></tr><tr><td>1</td><td>foo</td></tr></table>\n\t</li>\n</ul>\n",
			/* Uon */		"@(\n\t@(\n\t\t(a=1,b=foo)\n\t)\n)",
			/* UrlEnc */	"0=@(\n\t(\n\t\ta=1,\n\t\tb=foo\n\t)\n)"),
		input(7, "List2dOfBeans-3", new List2dOfBeans().init2(), 3,
			/* Json */		"[\n\t[\n\t\t{\n\t\t\ta: 1,\n\t\t\tb: 'foo'\n\t\t}\n\t]\n]",
			/* Xml */		"<array>\n\t<array>\n\t\t<object>\n\t\t\t<a>1</a>\n\t\t\t<b>foo</b>\n\t\t</object>\n\t</array>\n</array>\n",
			/* Html */		"<ul>\n\t<li>\n\t\t<table _type='array'>\n\t\t\t<tr><th>a</th><th>b</th></tr>\n\t\t\t<tr><td>1</td><td>foo</td></tr>\n\t\t</table>\n\t</li>\n</ul>\n",
			/* Uon */		"@(\n\t@(\n\t\t(\n\t\t\ta=1,\n\t\t\tb=foo\n\t\t)\n\t)\n)",
			/* UrlEnc */	"0=@(\n\t(\n\t\ta=1,\n\t\tb=foo\n\t)\n)"),
		input(8, "Map1dOfBeans-0", new Map1dOfBeans().init1(), 0,
			/* Json */		"{a:{a:1, b:'foo'}}",
			/* Xml */		"<object><a><a>1</a><b>foo</b></a></object>\n",
			/* Html */		"<table><tr><td>a</td><td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td></tr></table>\n",
			/* Uon */		"(a=(a=1,b=foo))",
			/* UrlEnc */	"a=(a=1,b=foo)"),
		input(9, "Map1dOfBeans-1", new Map1dOfBeans().init1(), 1,
			/* Json */		"{\n\ta: {a:1, b:'foo'}\n}",
			/* Xml */		"<object>\n\t<a><a>1</a><b>foo</b></a>\n</object>\n",
			/* Html */		"<table>\n\t<tr><td>a</td><td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td></tr>\n</table>\n",
			/* Uon */		"(\n\ta=(a=1,b=foo)\n)",
			/* UrlEnc */	"a=(\n\ta=1,\n\tb=foo\n)"),
		input(10, "Map1dOfBeans-2", new Map1dOfBeans().init1(), 2,
			/* Json */		"{\n\ta: {\n\t\ta: 1,\n\t\tb: 'foo'\n\t}\n}",
			/* Xml */		"<object>\n\t<a>\n\t\t<a>1</a>\n\t\t<b>foo</b>\n\t</a>\n</object>\n",
			/* Html */		"<table>\n\t<tr>\n\t\t<td>a</td>\n\t\t<td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td>\n\t</tr>\n</table>\n",
			/* Uon */		"(\n\ta=(\n\t\ta=1,\n\t\tb=foo\n\t)\n)",
			/* UrlEnc */	"a=(\n\ta=1,\n\tb=foo\n)"),
		input(11, "Map2dOfBeans-0", new Map2dOfBeans().init2(), 0,
			/* Json */		"{b:{a:{a:1, b:'foo'}}}",
			/* Xml */		"<object><b><a><a>1</a><b>foo</b></a></b></object>\n",
			/* Html */		"<table><tr><td>b</td><td><table><tr><td>a</td><td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td></tr></table></td></tr></table>\n",
			/* Uon */		"(b=(a=(a=1,b=foo)))",
			/* UrlEnc */	"b=(a=(a=1,b=foo))"),
		input(12, "Map2dOfBeans-1", new Map2dOfBeans().init2(), 1,
			/* Json */		"{\n\tb: {a:{a:1, b:'foo'}}\n}",
			/* Xml */		"<object>\n\t<b><a><a>1</a><b>foo</b></a></b>\n</object>\n",
			/* Html */		"<table>\n\t<tr><td>b</td><td><table><tr><td>a</td><td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td></tr></table></td></tr>\n</table>\n",
			/* Uon */		"(\n\tb=(a=(a=1,b=foo))\n)",
			/* UrlEnc */	"b=(\n\ta=(a=1,b=foo)\n)"),
		input(13, "Map2dOfBeans-2", new Map2dOfBeans().init2(), 2,
			/* Json */		"{\n\tb: {\n\t\ta: {a:1, b:'foo'}\n\t}\n}",
			/* Xml */		"<object>\n\t<b>\n\t\t<a><a>1</a><b>foo</b></a>\n\t</b>\n</object>\n",
			/* Html */		"<table>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td><table><tr><td>a</td><td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td></tr></table></td>\n\t</tr>\n</table>\n",
			/* Uon */		"(\n\tb=(\n\t\ta=(a=1,b=foo)\n\t)\n)",
			/* UrlEnc */	"b=(\n\ta=(\n\t\ta=1,\n\t\tb=foo\n\t)\n)"),
		input(14, "Map2dOfBeans-3", new Map2dOfBeans().init2(), 3,
			/* Json */		"{\n\tb: {\n\t\ta: {\n\t\t\ta: 1,\n\t\t\tb: 'foo'\n\t\t}\n\t}\n}",
			/* Xml */		"<object>\n\t<b>\n\t\t<a>\n\t\t\t<a>1</a>\n\t\t\t<b>foo</b>\n\t\t</a>\n\t</b>\n</object>\n",
			/* Html */		"<table>\n\t<tr>\n\t\t<td>b</td>\n\t\t<td>\n\t\t\t<table><tr><td>a</td><td><table><tr><td>a</td><td>1</td></tr><tr><td>b</td><td>foo</td></tr></table></td></tr></table>\n\t\t</td>\n\t</tr>\n</table>\n",
			/* Uon */		"(\n\tb=(\n\t\ta=(\n\t\t\ta=1,\n\t\t\tb=foo\n\t\t)\n\t)\n)",
			/* UrlEnc */	"b=(\n\ta=(\n\t\ta=1,\n\t\tb=foo\n\t)\n)")
	};

	static Input[] inputs() {
		return INPUTS;
	}

	static class Input {
		String label;
		Object in;
		int maxDepth;
		String json, xml, html, uon, urlEnc;

		Input(String label, Object in, int maxDepth, String json, String xml, String html, String uon, String urlEnc) {
			this.label = label;
			this.in = in;
			this.maxDepth = maxDepth;
			this.json = json;
			this.xml = xml;
			this.html = html;
			this.uon = uon;
			this.urlEnc = urlEnc;
		}
	}

	public static class List1dOfBeans extends LinkedList<ABean> {
		public List1dOfBeans init1() {
			add(ABean.get());
			return this;
		}
	}

	public static class List2dOfBeans extends LinkedList<List1dOfBeans> {
		public List2dOfBeans init2() {
			add(new List1dOfBeans().init1());
			return this;
		}
	}

	public static class Map1dOfBeans extends LinkedHashMap<String,ABean> {
		public Map1dOfBeans init1() {
			put("a", ABean.get());
			return this;
		}
	}

	public static class Map2dOfBeans extends LinkedHashMap<String,Map1dOfBeans> {
		public Map2dOfBeans init2() {
			put("b", new Map1dOfBeans().init1());
			return this;
		}
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void a1_serializeJson(Input input) throws Exception {
		var s = Json5Serializer.DEFAULT_READABLE.copy().maxIndent(input.maxDepth).build();
		testSerialize("json", s, input.json, input);
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void b11_serializeXml(Input input) throws Exception {
		var s = XmlSerializer.DEFAULT_SQ_READABLE.copy().maxIndent(input.maxDepth).build();
		testSerialize("xml", s, input.xml, input);
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void c11_serializeHtml(Input input) throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ_READABLE.copy().maxIndent(input.maxDepth).build();
		testSerialize("html", s, input.html, input);
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void d11_serializeUon(Input input) throws Exception {
		var s = UonSerializer.DEFAULT_READABLE.copy().maxIndent(input.maxDepth).build();
		testSerialize("uon", s, input.uon, input);
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void e11_serializeUrlEncoding(Input input) throws Exception {
		var s = UrlEncodingSerializer.DEFAULT_READABLE.copy().maxIndent(input.maxDepth).build();
		testSerialize("urlEncoding", s, input.urlEnc, input);
	}

	private void testSerialize(String testName, Serializer s, String expected, Input input) throws Exception {
		try {
			var r = s.serializeToString(input.in);

			// Specifying "xxx" in the expected results will spit out what we should populate the field with.
			if (expected.equals("xxx")) {
				System.out.println(input.label + "/" + testName + "=\n" + r.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")); // NOT DEBUG
				System.out.println(r);
				return;
			}

			assertEquals(expected, r, fms("{0}/{1} parse-normal failed", input.label, testName));

		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError(input.label + "/" + testName + " failed.  exception=" + e.getLocalizedMessage());
		}
	}
}