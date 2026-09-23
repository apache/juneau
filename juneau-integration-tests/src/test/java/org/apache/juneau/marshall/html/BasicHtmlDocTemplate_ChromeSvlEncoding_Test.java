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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.remote.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.vars.*;
import org.junit.jupiter.api.*;

/**
 * Verifies HTML-doc chrome SVL encoding: substitutions written into raw HTML chrome are XML-escaped,
 * static template tags stay tags, {@code $W}/{@code $C}/{@code $RS} pass through, and script/style
 * leave request vars unresolved.
 */
@SuppressWarnings({
	"serial"  // serialVersionUID not required for test classes.
})
class BasicHtmlDocTemplate_ChromeSvlEncoding_Test extends TestBase {

	private static final String SAMPLE = "a<b&c>";
	private static final String SAMPLE_QUERY = "q=a%3Cb%26c%3E";
	private static final String ENCODED = "a&lt;b&amp;c&gt;";

	private static String htmlGet(Class<?> c, String path) throws Exception {
		return MockRestClient.buildLax(c).get(path).accept("text/html").run().getContent().asString();
	}

	private static void assertChromeEncoded(String html) {
		assertTrue(html.contains(ENCODED), html);
		assertFalse(html.contains(SAMPLE), html);
		assertTrue(html.contains("<h5>"), html);
		assertTrue(html.contains("</h5>"), html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — chrome fields encode request-var substitutions and keep surrounding tags
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(aside = "<h5>$RQ{q}</h5>")
	public static class A01_Aside extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void a01_asideEncodesRequestQuery() throws Exception {
		assertChromeEncoded(htmlGet(A01_Aside.class, "/page?" + SAMPLE_QUERY));
	}

	@Rest
	@HtmlDocConfig(header = "<h5>$RH{X-Q}</h5>")
	public static class A02_Header extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void a02_headerEncodesRequestHeader() throws Exception {
		var html = MockRestClient.buildLax(A02_Header.class)
			.get("/page")
			.accept("text/html")
			.header("X-Q", SAMPLE)
			.run()
			.getContent().asString();
		assertChromeEncoded(html);
	}

	@Rest
	@HtmlDocConfig(footer = "<h5>$RF{q}</h5>")
	public static class A03_Footer extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestPost
		public String page() {
			return "OK";
		}
	}

	@Test
	void a03_footerEncodesRequestForm() throws Exception {
		var html = MockRestClient.buildLax(A03_Footer.class)
			.post("/page", SAMPLE_QUERY)
			.accept("text/html")
			.contentType("application/x-www-form-urlencoded")
			.run()
			.getContent().asString();
		assertChromeEncoded(html);
	}

	@Rest
	@HtmlDocConfig(nav = "<h5>$RP{p}</h5>")
	public static class A04_Nav extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet("/page/{p}")
		public String page() {
			return "OK";
		}
	}

	@Test
	void a04_navEncodesRequestPath() throws Exception {
		assertChromeEncoded(htmlGet(A04_Nav.class, "/page/a%3Cb%26c%3E"));
	}

	@Rest
	@HtmlDocConfig(head = "<meta name='q' content='$RQ{q}'>")
	public static class A05_Head extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void a05_headEncodesRequestQuery() throws Exception {
		var html = htmlGet(A05_Head.class, "/page?" + SAMPLE_QUERY);
		assertTrue(html.contains(ENCODED), html);
		assertFalse(html.contains(SAMPLE), html);
		assertTrue(html.contains("<meta name='q'"), html);
	}

	@Rest
	@HtmlDocConfig(aside = "<h5>$RQ{q}</h5>")
	public static class A06_Quotes extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void a06_asideEncodesQuotes() throws Exception {
		var html = htmlGet(A06_Quotes.class, "/page?q=x%22y%27z");
		assertTrue(html.contains("x&quot;y&apos;z"), html);
		assertFalse(html.contains("x\"y'z"), html);
		assertTrue(html.contains("<h5>"), html);
	}

	@Rest
	@HtmlDocConfig(navlinks = "<h5>$RQ{q}</h5>")
	public static class A07_NavlinkHtml extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void a07_navlinkHtmlFragmentEncodesRequestQuery() throws Exception {
		assertChromeEncoded(htmlGet(A07_NavlinkHtml.class, "/page?" + SAMPLE_QUERY));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — static HTML in chrome stays tags
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(aside = "<p>hello</p>")
	public static class B01_StaticHtml extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void b01_staticHtmlAsideStaysTags() throws Exception {
		var html = htmlGet(B01_StaticHtml.class, "/page");
		assertTrue(html.contains("<p>hello</p>"), html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — HTML passthrough $W / $C / $RS
	//-----------------------------------------------------------------------------------------------------------------

	public static class TagWidget implements HtmlWidget {
		@Override
		public String getName() {
			return "TagWidget";
		}

		@Override
		public String getHtml(VarResolverSession session) {
			return "<span>widget</span>";
		}

		@Override
		public String getScript(VarResolverSession session) {
			return "var widgetScript=1;";
		}

		@Override
		public String getStyle(VarResolverSession session) {
			return ".widgetStyle{color:navy}";
		}
	}

	@Rest
	@HtmlDocConfig(widgets = TagWidget.class, aside = "$W{TagWidget}")
	public static class C01_Widget extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void c01_widgetHtmlPassthrough() throws Exception {
		var html = htmlGet(C01_Widget.class, "/page");
		assertTrue(html.contains("<span>widget</span>"), html);
	}

	@Rest
	@HtmlDocConfig(aside = "$C{noSuchChromeKey,<span>passthrough</span>}")
	public static class C02_Config extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void c02_configHtmlPassthrough() throws Exception {
		var html = htmlGet(C02_Config.class, "/page");
		assertTrue(html.contains("<span>passthrough</span>"), html);
	}

	@Rest(title = "ChromeTitle")
	@HtmlDocConfig(aside = "$RS{title}")
	public static class C03_SwaggerTitle extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void c03_swaggerTitlePassthrough() throws Exception {
		var html = htmlGet(C03_SwaggerTitle.class, "/page");
		assertTrue(html.contains("ChromeTitle"), html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — request-var resolve() stays raw outside chrome
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D01_PlainResolve {
		@RestGet
		public String page(RestRequest req) {
			return req.getVarResolverSession().resolve("$RQ{q}");
		}

		@RestGet("/var")
		public String varClass(RestRequest req) {
			return new RequestQueryVar().resolve(req.getVarResolverSession(), "q");
		}
	}

	@Test
	void d01_plainResolveOutsideChromeStaysRaw() throws Exception {
		var content = MockRestClient.build(D01_PlainResolve.class)
			.get("/page?" + SAMPLE_QUERY)
			.run()
			.getContent().asString();
		assertTrue(content.contains(SAMPLE), content);
		assertFalse(content.contains(ENCODED), content);
	}

	@Test
	void d02_requestQueryVarResolveStaysRaw() throws Exception {
		var content = MockRestClient.build(D01_PlainResolve.class)
			.get("/var?" + SAMPLE_QUERY)
			.run()
			.getContent().asString();
		assertTrue(content.contains(SAMPLE), content);
		assertFalse(content.contains(ENCODED), content);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — article body is single-encoded via HtmlWriter.text()
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E01_Article extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "a&b";
		}
	}

	@Test
	void e01_articleBodyNotDoubleEncoded() throws Exception {
		var html = htmlGet(E01_Article.class, "/page");
		assertTrue(html.contains("a&amp;b"), html);
		assertFalse(html.contains("a&amp;amp;b"), html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — no-results message encodes like chrome
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(noResultsMessage = "<h5>$RQ{q}</h5>")
	public static class F01_NoResults extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public List<String> page() {
			return List.of();
		}
	}

	@Test
	void f01_noResultsEncodesRequestQuery() throws Exception {
		assertChromeEncoded(htmlGet(F01_NoResults.class, "/page?" + SAMPLE_QUERY));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — script/style refuse request vars; widget script/style still appear
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(
		widgets = TagWidget.class,
		script = "var x = \"$RQ{q}\";",
		style = ".x{content:\"$RQ{q}\";}"
	)
	public static class G01_ScriptStyle extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void g01_scriptDoesNotInterpolateRequestQuery() throws Exception {
		var html = htmlGet(G01_ScriptStyle.class, "/page?" + SAMPLE_QUERY);
		assertTrue(html.contains("$RQ{q}"), html);
		assertFalse(html.contains(SAMPLE), html);
		assertTrue(html.contains("var widgetScript=1;"), html);
	}

	@Test
	void g02_styleDoesNotInterpolateRequestQuery() throws Exception {
		var html = htmlGet(G01_ScriptStyle.class, "/page?" + SAMPLE_QUERY);
		assertTrue(html.contains("$RQ{q}"), html);
		assertFalse(html.contains(SAMPLE), html);
		assertTrue(html.contains(".widgetStyle{color:navy}"), html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h — RrpcServlet HTML nav still shows registered class/method names
	//-----------------------------------------------------------------------------------------------------------------

	public interface H_Interface {
		String echo(String input);
	}

	public static class H_Impl implements H_Interface {
		@Override
		public String echo(String input) {
			return input;
		}
	}

	@Rest
	public static class H_RrpcServlet extends RrpcServlet {
		private final Map<Class<?>,Object> services = Map.of(H_Interface.class, new H_Impl());

		@Override
		protected Map<Class<?>,Object> getServiceMap() {
			return services;
		}
	}

	@Test
	void h01_rrpcHtmlNavShowsInterfaceAndMethodNames() throws Exception {
		var iface = H_Interface.class.getName();
		var html = htmlGet(H_RrpcServlet.class, "/" + iface);
		assertTrue(html.contains(iface), html);
		assertTrue(html.contains("echo"), html);
		assertTrue(html.contains("<h5>"), html);
	}
}
