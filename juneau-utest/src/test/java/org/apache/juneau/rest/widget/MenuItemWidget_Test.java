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
package org.apache.juneau.rest.widget;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link MenuItemWidget}.
 *
 * <p>Most tests dispatch through a tiny {@link MockRestClient}-backed REST resource so the handler is given
 * a real {@link RestRequest} / {@link RestResponse} pair. The handler invokes the widget's
 * {@code getHtml/getScript/getStyle} methods and writes the result back to the client where the test
 * asserts on the body. This exercises the branches in {@link MenuItemWidget#getHtml(RestRequest,RestResponse)}
 * including the {@code pre}/{@code post} script paths, {@link Reader} / {@link CharSequence} / object
 * content branches, and the private {@code getId} attribute increment.
 */
@SuppressWarnings({"resource"})
class MenuItemWidget_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test widgets
	//------------------------------------------------------------------------------------------------------------------

	/** Returns a CharSequence as content; no pre/post scripts. */
	public static class StringContentWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "string-label"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return "<p>plain string content</p>"; }
	}

	/** Returns a Reader as content; exercises the Reader branch + pipe(). */
	public static class ReaderContentWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "reader-label"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return new StringReader("<p>reader content</p>"); }
	}

	/** Returns an arbitrary bean as content; exercises the HtmlSerializer branch. */
	public static class BeanContentWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "bean-label"; }
		@Override public Object getContent(RestRequest req, RestResponse res) {
			return new Div().children(new P().children("bean rendered content"));
		}
	}

	/** Adds before-show script only; exercises the {@code pre != null} branch + getId(). */
	public static class BeforeShowOnlyWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "before-only"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return "content-x"; }
		@Override public String getBeforeShowScript(RestRequest req, RestResponse res) { return "alert('pre');"; }
	}

	/** Adds after-show script only; exercises the {@code post != null} branch. */
	public static class AfterShowOnlyWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "after-only"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return "content-y"; }
		@Override public String getAfterShowScript(RestRequest req, RestResponse res) { return "alert('post');"; }
	}

	/** Adds both pre and post-show scripts; exercises both branches and twice-incremented getId. */
	public static class BothShowScriptsWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "both"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return "content-z"; }
		@Override public String getBeforeShowScript(RestRequest req, RestResponse res) { return "alert('pre');"; }
		@Override public String getAfterShowScript(RestRequest req, RestResponse res) { return "alert('post');"; }
	}

	/** Returns empty strings for pre/post scripts (nullIfEmpty should treat them as null). */
	public static class EmptyShowScriptsWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "empty-scripts"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return "content-q"; }
		@Override public String getBeforeShowScript(RestRequest req, RestResponse res) { return ""; }
		@Override public String getAfterShowScript(RestRequest req, RestResponse res) { return ""; }
	}

	/** Returns a Reader that throws IOException on read - exercises the IOException catch branch. */
	public static class ThrowingReaderWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "throwing-reader"; }
		@Override public Object getContent(RestRequest req, RestResponse res) {
			return new Reader() {
				@Override public int read(char[] cbuf, int off, int len) throws IOException { throw new IOException("boom"); }
				@Override public void close() { /* no-op */ }
			};
		}
	}

	/** Returns a bean whose serialization throws - exercises the HtmlSerializer Exception catch branch. */
	public static class ThrowingBeanWidget extends MenuItemWidget {
		@Override public String getLabel(RestRequest req, RestResponse res) { return "throwing-bean"; }
		@Override public Object getContent(RestRequest req, RestResponse res) { return new ThrowingBean(); }
	}

	/** Bean whose getter throws an exception during serialization. */
	public static class ThrowingBean {
		public String getKaboom() { throw new RuntimeException("serialize-fail"); }
	}

	//------------------------------------------------------------------------------------------------------------------
	// REST resource that dispatches widget methods
	//
	// Each handler receives a real RestRequest / RestResponse and runs the requested widget operation.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@RestGet("/string/html")
		public String stringHtml(RestRequest req, RestResponse res) {
			return new StringContentWidget().getHtml(req, res);
		}

		@RestGet("/reader/html")
		public String readerHtml(RestRequest req, RestResponse res) {
			return new ReaderContentWidget().getHtml(req, res);
		}

		@RestGet("/bean/html")
		public String beanHtml(RestRequest req, RestResponse res) {
			return new BeanContentWidget().getHtml(req, res);
		}

		@RestGet("/before/html")
		public String beforeHtml(RestRequest req, RestResponse res) {
			return new BeforeShowOnlyWidget().getHtml(req, res);
		}

		@RestGet("/after/html")
		public String afterHtml(RestRequest req, RestResponse res) {
			return new AfterShowOnlyWidget().getHtml(req, res);
		}

		@RestGet("/both/html")
		public String bothHtml(RestRequest req, RestResponse res) {
			return new BothShowScriptsWidget().getHtml(req, res);
		}

		@RestGet("/empty/html")
		public String emptyHtml(RestRequest req, RestResponse res) {
			return new EmptyShowScriptsWidget().getHtml(req, res);
		}

		// Calls getHtml against a Reader that throws IOException; expects the call to throw a RuntimeException.
		// We capture the exception class+message and return as a string so the test can assert on it.
		@RestGet("/throwingReader/html")
		public String throwingReaderHtml(RestRequest req, RestResponse res) {
			try {
				new ThrowingReaderWidget().getHtml(req, res);
				return "NO_EXCEPTION";
			} catch (RuntimeException e) {
				return "EX:" + e.getClass().getSimpleName() + ":" + (e.getMessage() == null ? "" : e.getMessage());
			}
		}

		// Calls getHtml against a bean whose getter throws; expects the call to throw a RuntimeException.
		@RestGet("/throwingBean/html")
		public String throwingBeanHtml(RestRequest req, RestResponse res) {
			try {
				new ThrowingBeanWidget().getHtml(req, res);
				return "NO_EXCEPTION";
			} catch (RuntimeException e) {
				return "EX:" + e.getClass().getSimpleName();
			}
		}

		// Calls getHtml twice on the same request; verifies the LastMenuItemId attribute is preserved & incremented.
		@RestGet("/twice/html")
		public String twiceHtml(RestRequest req, RestResponse res) {
			var w = new BothShowScriptsWidget();
			var first = w.getHtml(req, res);
			var second = w.getHtml(req, res);
			return "FIRST:" + first + "|SECOND:" + second;
		}

		// Forces the bean serialization branch and toggles isDebug = true.
		@RestGet("/bean/debug/html")
		public String beanDebugHtml(RestRequest req, RestResponse res) {
			req.setAttribute("Debug", true);
			return new BeanContentWidget().getHtml(req, res);
		}

		@RestGet("/script")
		public String script(RestRequest req, RestResponse res) {
			var s = new StringContentWidget().getScript(req, res);
			return s == null ? "<NULL>" : s;
		}

		@RestGet("/style")
		public String style(RestRequest req, RestResponse res) {
			var s = new StringContentWidget().getStyle(req, res);
			return s == null ? "<NULL>" : s;
		}

		@RestGet("/default/script")
		public String defaultScript(RestRequest req, RestResponse res) {
			var w = new StringContentWidget();
			var pre = w.getBeforeShowScript(req, res);
			var post = w.getAfterShowScript(req, res);
			return "pre=" + (pre == null ? "null" : pre) + ";post=" + (post == null ? "null" : post);
		}
	}

	private static MockRestClient client() {
		return MockRestClient.create(A.class).plainText().build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a - getHtml branches: simple string content
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_html_stringContent_rendersLabelAndContent() throws Exception {
		var html = client().get("/string/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "menu-item");
		assertContains(html, "string-label");
		assertContains(html, "menuClick(this)");
		assertContains(html, "popup-content");
		assertContains(html, "<p>plain string content</p>");
		// No pre/post-show script function should be emitted.
		assertFalse(html.contains("onPreShow"), () -> "Expected no pre-show script in:\n" + html);
		assertFalse(html.contains("onPostShow"), () -> "Expected no post-show script in:\n" + html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - getHtml branches: Reader content
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_html_readerContent_pipesIntoOutput() throws Exception {
		var html = client().get("/reader/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "reader-label");
		assertContains(html, "<p>reader content</p>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - getHtml branches: bean (HtmlSerializer) content
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_html_beanContent_serializesViaHtmlSerializer() throws Exception {
		var html = client().get("/bean/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "bean-label");
		assertContains(html, "bean rendered content");
	}

	@Test void c02_html_beanContent_debugAttributeFlag() throws Exception {
		// Exercises the debug=true branch in HtmlSerializerSession factory.
		var html = client().get("/bean/debug/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "bean rendered content");
	}

	@Test void c03_html_beanContent_plainTextQueryParam() throws Exception {
		// Exercises the useWhitespace=true branch (req.isPlainText() == true) when ?plainText=true.
		var html = client().get("/bean/html?plainText=true").run().assertStatus(200).getContent().asString();
		assertContains(html, "bean rendered content");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d - getHtml branches: pre/post-show script generation
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_html_beforeShowOnly_emitsScriptAndOnclick() throws Exception {
		var html = client().get("/before/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "<script>");
		assertContains(html, "onPreShow1()");
		assertContains(html, "alert('pre');");
		assertContains(html, "menuClick(this)");
		// No post-show function present.
		assertFalse(html.contains("onPostShow"), () -> "post-show should not be in output: " + html);
	}

	@Test void d02_html_afterShowOnly_emitsScriptAndOnclick() throws Exception {
		var html = client().get("/after/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "<script>");
		assertContains(html, "onPostShow1()");
		assertContains(html, "alert('post');");
		assertContains(html, "menuClick(this)");
		assertFalse(html.contains("onPreShow"), () -> "pre-show should not be in output: " + html);
	}

	@Test void d03_html_bothShowScripts_emitsBoth() throws Exception {
		var html = client().get("/both/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "onPreShow1()");
		assertContains(html, "onPostShow1()");
		// post-show function must contain its own getAfterShowScript() body, not the pre-show body.
		assertContains(html, "alert('pre');");
		assertContains(html, "alert('post');");
		assertContains(html, "menuClick(this)");
	}

	@Test void d04_html_emptyShowScripts_treatedAsNull() throws Exception {
		// Empty strings should be treated as null by nullIfEmpty(); no <script> tag, no functions.
		var html = client().get("/empty/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "empty-scripts");
		assertFalse(html.contains("onPreShow"), () -> "pre-show should not be in output: " + html);
		assertFalse(html.contains("onPostShow"), () -> "post-show should not be in output: " + html);
		assertFalse(html.contains("<script>"), () -> "script tag should not be present: " + html);
	}

	@Test void d06_html_readerThrowsIOException_wrappedAsRuntime() throws Exception {
		var s = client().get("/throwingReader/html").run().assertStatus(200).getContent().asString();
		assertTrue(s.startsWith("EX:"), () -> "Expected wrapped exception: " + s);
		assertContains(s, "boom");
	}

	@Test void d07_html_beanSerializerThrows_wrappedAsRuntime() throws Exception {
		var s = client().get("/throwingBean/html").run().assertStatus(200).getContent().asString();
		assertTrue(s.startsWith("EX:"), () -> "Expected wrapped exception: " + s);
	}

	@Test void d05_html_calledTwice_idIncrements() throws Exception {
		var html = client().get("/twice/html").run().assertStatus(200).getContent().asString();
		// First invocation should generate id=1 functions, second should generate id=2.
		assertContains(html, "onPreShow1()");
		assertContains(html, "onPostShow1()");
		assertContains(html, "onPreShow2()");
		assertContains(html, "onPostShow2()");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e - getScript / getStyle / default-show-scripts
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_script_loadsMenuItemWidgetJs() throws Exception {
		var s = client().get("/script").run().assertStatus(200).getContent().asString();
		assertNotEquals("<NULL>", s, "getScript() should not return null - resource MenuItemWidget.js should load");
		assertFalse(s.isEmpty(), "getScript() output should be non-empty");
	}

	@Test void e02_style_loadsMenuItemWidgetCss() throws Exception {
		var s = client().get("/style").run().assertStatus(200).getContent().asString();
		assertNotEquals("<NULL>", s, "getStyle() should not return null - resource MenuItemWidget.css should load");
		assertFalse(s.isEmpty(), "getStyle() output should be non-empty");
	}

	@Test void e03_defaultBeforeAndAfterShowScripts_returnNull() throws Exception {
		var s = client().get("/default/script").run().assertStatus(200).getContent().asString();
		assertEquals("pre=null;post=null", s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f - direct (non-HTTP) calls that don't need a request
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_getName_defaultIsSimpleName() {
		assertEquals("StringContentWidget", new StringContentWidget().getName());
		assertEquals("ReaderContentWidget", new ReaderContentWidget().getName());
		assertEquals("BothShowScriptsWidget", new BothShowScriptsWidget().getName());
	}

	@Test void f02_getLabel_concreteSubclasses() {
		assertEquals("string-label", new StringContentWidget().getLabel(null, null));
		assertEquals("reader-label", new ReaderContentWidget().getLabel(null, null));
		assertEquals("bean-label", new BeanContentWidget().getLabel(null, null));
		assertEquals("before-only", new BeforeShowOnlyWidget().getLabel(null, null));
		assertEquals("after-only", new AfterShowOnlyWidget().getLabel(null, null));
		assertEquals("both", new BothShowScriptsWidget().getLabel(null, null));
	}

	@Test void f03_defaultBeforeAfterShowScripts_returnNull() {
		var w = new StringContentWidget();
		assertNull(w.getBeforeShowScript(null, null));
		assertNull(w.getAfterShowScript(null, null));
	}

	@Test void f04_getContent_returnsExpectedTypes() {
		assertTrue(new StringContentWidget().getContent(null, null) instanceof CharSequence);
		assertTrue(new ReaderContentWidget().getContent(null, null) instanceof Reader);
		assertTrue(new BeanContentWidget().getContent(null, null) instanceof Div);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private static void assertContains(String s, String needle) {
		if (!s.contains(needle))
			throw new AssertionError("Expected to contain '" + needle + "' but did not.\nBody:\n" + s);
	}
}
