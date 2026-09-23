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
package org.apache.juneau.html.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class BasicHtmlDocTemplate_ChromeSvlEncoding_Test extends TestBase {

	private static final String SAMPLE = "a<b&c>";
	private static final String SAMPLE_ENC = "a&lt;b&amp;c&gt;";
	private static final String QUOTES = "x\"y'z";
	private static final String QUOTES_ENC = "x&quot;y&apos;z";

	//------------------------------------------------------------------------------------------------------------------
	// Chrome request-var substitutions are encoded; surrounding tags stay tags.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(title="ChromeTitle")
	public static class A extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		@HtmlDocConfig(aside="<h5>$RQ{q}</h5>")
		public Object asideQuery() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header="<h5>$RQ{q}</h5>")
		public Object headerQuery() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer="<h5>$RQ{q}</h5>")
		public Object footerQuery() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks="NONE", nav="<h5>$RQ{q}</h5>")
		public Object navQuery() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(head="<h5>$RQ{q}</h5>")
		public Object headQuery() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header="<h5>$RQ{q}</h5>")
		public Object headerQuotes() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside="<h5>$RH{h}</h5>")
		public Object asideHeader() {
			return "OK";
		}
		@RestPost
		@HtmlDocConfig(aside="<h5>$RF{q}</h5>")
		public Object asideForm() {
			return "OK";
		}
		@RestGet(path="/p/{p}")
		@HtmlDocConfig(aside="<h5>$RP{p}</h5>")
		public Object asidePath() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"x:<h5>$RQ{q}</h5>"})
		public Object navlinkHtml() {
			return "OK";
		}
	}

	@Test void a01_asideQuery_encoded() throws Exception {
		assertChromeEncoded(client(A.class).get("/asideQuery").queryData("q", SAMPLE).accept("text/html").run().getContent().asString(), "aside");
	}

	@Test void a02_headerQuery_encoded() throws Exception {
		assertChromeEncoded(client(A.class).get("/headerQuery").queryData("q", SAMPLE).accept("text/html").run().getContent().asString(), "header");
	}

	@Test void a03_footerQuery_encoded() throws Exception {
		assertChromeEncoded(client(A.class).get("/footerQuery").queryData("q", SAMPLE).accept("text/html").run().getContent().asString(), "footer");
	}

	@Test void a04_navQuery_encoded() throws Exception {
		assertChromeEncoded(client(A.class).get("/navQuery").queryData("q", SAMPLE).accept("text/html").run().getContent().asString(), "nav");
	}

	@Test void a05_headQuery_encoded() throws Exception {
		var html = client(A.class).get("/headQuery").queryData("q", SAMPLE).accept("text/html").run().getContent().asString();
		assertContainsEncoded(html);
		assertTrue(html.contains("<h5>" + SAMPLE_ENC + "</h5>"));
	}

	@Test void a06_headerQuotes_encoded() throws Exception {
		var html = client(A.class).get("/headerQuotes").queryData("q", QUOTES).accept("text/html").run().getContent().asString();
		assertTrue(html.contains("<h5>" + QUOTES_ENC + "</h5>"));
		assertFalse(html.contains(QUOTES));
	}

	@Test void a07_asideHeader_encoded() throws Exception {
		assertChromeEncoded(client(A.class).get("/asideHeader").header("h", SAMPLE).accept("text/html").run().getContent().asString(), "aside");
	}

	@Test void a08_asideForm_encoded() throws Exception {
		assertChromeEncoded(client(A.class).post("/asideForm").formData("q", SAMPLE).accept("text/html").run().getContent().asString(), "aside");
	}

	@Test void a09_asidePath_encoded() throws Exception {
		assertChromeEncoded(client(A.class).get("/p/{p}").pathData("p", SAMPLE).accept("text/html").run().getContent().asString(), "aside");
	}

	@Test void a10_navlinkHtmlFragment_encoded() throws Exception {
		var html = client(A.class).get("/navlinkHtml").queryData("q", SAMPLE).accept("text/html").run().getContent().asString();
		assertTrue(html.contains("<h5>" + SAMPLE_ENC + "</h5>"));
		assertFalse(html.contains(SAMPLE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Static HTML tags in chrome are unchanged.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		@HtmlDocConfig(aside="<p>hello</p>")
		public Object staticAside() {
			return "OK";
		}
	}

	@Test void b01_staticHtml_stillTags() throws Exception {
		client(B.class).get("/staticAside").accept("text/html").run().assertContent().isContains("<aside><p>hello</p></aside>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Passthrough: $W, $C, $RS.
	//------------------------------------------------------------------------------------------------------------------

	public static class TagWidget implements HtmlWidget {
		@Override
		public String getName() {
			return "Tag";
		}
		@Override
		public String getHtml(VarResolverSession session) {
			return "<span>ok</span>";
		}
		@Override
		public String getScript(VarResolverSession session) {
			return "var w=1;";
		}
		@Override
		public String getStyle(VarResolverSession session) {
			return "/*w*/";
		}
	}

	@Rest(title="ChromeTitle")
	public static class C extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		@HtmlDocConfig(aside="$W{Tag}", widgets=TagWidget.class)
		public Object widgetHtml() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside="$C{missingKey,<span>ok</span>}")
		public Object configHtml() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside="<h5>$RS{title}</h5>")
		public Object swaggerTitle() {
			return "OK";
		}
	}

	@Test void c01_widgetHtml_passthrough() throws Exception {
		client(C.class).get("/widgetHtml").accept("text/html").run().assertContent().isContains("<aside><span>ok</span></aside>");
	}

	@Test void c02_configHtml_passthrough() throws Exception {
		client(C.class).get("/configHtml").accept("text/html").run().assertContent().isContains("<aside><span>ok</span></aside>");
	}

	@Test void c03_swaggerTitle_passthrough() throws Exception {
		client(C.class).get("/swaggerTitle").accept("text/html").run().assertContent().isContains("<aside><h5>ChromeTitle</h5></aside>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Request vars themselves unchanged outside chrome; article is single-encoded; no-results encoded.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String raw(org.apache.juneau.rest.RestRequest req) {
			var session = req.getVarResolverSession();
			return new RequestQueryVar().resolve(session, "q") + "|" + session.resolve("$RQ{q}");
		}
		@RestGet
		public String articleAmp() {
			return "a&b";
		}
		@RestGet
		@HtmlDocConfig(noResultsMessage="<h5>$RQ{q}</h5>")
		public Object noResults() {
			return List.of();
		}
	}

	@Test void d01_requestVarResolve_stillRaw() throws Exception {
		client(D.class).get("/raw").queryData("q", SAMPLE).accept("text/json").run().assertContent().isContains(SAMPLE + "|" + SAMPLE);
	}

	@Test void d02_articleBody_singleEncoded() throws Exception {
		var html = client(D.class).get("/articleAmp").accept("text/html").run().getContent().asString();
		assertTrue(html.contains("a&amp;b"));
		assertFalse(html.contains("a&amp;amp;b"));
	}

	@Test void d03_noResults_encoded() throws Exception {
		var html = client(D.class).get("/noResults").queryData("q", SAMPLE).accept("text/html").run().getContent().asString();
		assertTrue(html.contains("<h5>" + SAMPLE_ENC + "</h5>"));
		assertFalse(html.contains(SAMPLE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Script / style leave request vars unresolved; widget getScript / getStyle still appear.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		@HtmlDocConfig(
			script="var x=$RQ{q};",
			style="/* $RQ{q} */",
			widgets=TagWidget.class
		)
		public Object scriptStyle() {
			return "OK";
		}
	}

	@Test void e01_scriptStyle_requestVarsUnresolved() throws Exception {
		var html = client(E.class).get("/scriptStyle").queryData("q", SAMPLE).accept("text/html").run().getContent().asString();
		assertTrue(html.contains("$RQ{q}"));
		assertFalse(html.contains(SAMPLE));
		assertTrue(html.contains("var w=1;"));
		assertTrue(html.contains("/*w*/"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient client(Class<?> c) {
		return MockRestClient.build(c);
	}

	private static void assertChromeEncoded(String html, String section) {
		assertTrue(html.contains("<" + section + ">") || html.contains("<" + section), html);
		assertTrue(html.contains("<h5>" + SAMPLE_ENC + "</h5>"), html);
		assertFalse(html.contains(SAMPLE), html);
	}

	private static void assertContainsEncoded(String html) {
		assertTrue(html.contains(SAMPLE_ENC), html);
		assertFalse(html.contains(SAMPLE), html);
	}
}
