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

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

class RestWidget_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a - Tooltip
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_tooltip_swap() {
		var t = new Tooltip("display", "line1", "line2");
		var div = t.swap(null);
		assertNotNull(div);
		assertTrue(div instanceof Div);
		assertEquals("tooltip", div.getAttr("class"));
	}

	@Test void a02_tooltip_displayContent() {
		var t = new Tooltip("<b>bold</b>", "tip content");
		var div = t.swap(null);
		assertNotNull(div);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - Widget.getName()
	//------------------------------------------------------------------------------------------------------------------

	public static class TestWidget extends Widget {
		@Override
		public String getHtml(RestRequest req, RestResponse res) {
			return "<div>test</div>";
		}
	}

	@Test void b01_widget_getName_defaultIsSimpleName() {
		var w = new TestWidget();
		assertEquals("TestWidget", w.getName());
	}

	public static class CustomNameWidget extends Widget {
		@Override
		public String getName() { return "MyCustomName"; }

		@Override
		public String getHtml(RestRequest req, RestResponse res) {
			return "<span>custom</span>";
		}
	}

	@Test void b02_widget_getName_custom() {
		var w = new CustomNameWidget();
		assertEquals("MyCustomName", w.getName());
	}

	@Test void b03_widget_defaultMethodsReturnNull() {
		var w = new TestWidget();
		assertNull(w.getScript(null, null));
		assertNull(w.getStyle(null, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - PoweredByApache via MockRestClient
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(
		widgets={PoweredByApache.class},
		footer="$W{PoweredByApache}"
	)
	public static class C extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet("/")
		public String get() { return "OK"; }
	}

	@Test void c01_poweredByApache_rendersInFooter() throws Exception {
		var c = MockRestClient.build(C.class);
		var html = c.get("/").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "http://apache.org");
		assertContains(html, "asf.png");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d - PoweredByJuneau via MockRestClient
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(
		widgets={PoweredByJuneau.class},
		footer="$W{PoweredByJuneau}"
	)
	public static class D extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet("/")
		public String get() { return "OK"; }
	}

	@Test void d01_poweredByJuneau_rendersInFooter() throws Exception {
		var c = MockRestClient.build(D.class);
		var html = c.get("/").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "http://juneau.apache.org");
		assertContains(html, "juneau.png");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e - ThemeMenuItem via MockRestClient
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(
		widgets={ThemeMenuItem.class},
		navlinks={"$W{ThemeMenuItem}"}
	)
	public static class E extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet("/")
		public String get() { return "OK"; }
	}

	@Test void e01_themeMenuItem_rendersLabel() throws Exception {
		var c = MockRestClient.build(E.class);
		var html = c.get("/").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "themes");
		assertContains(html, "devops");
		assertContains(html, "light");
		assertContains(html, "original");
		assertContains(html, "dark");
	}

	//------------------------------------------------------------------------------------------------------------------
	// f - ContentTypeMenuItem via MockRestClient
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(
		widgets={ContentTypeMenuItem.class},
		navlinks={"$W{ContentTypeMenuItem}"}
	)
	public static class F extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet("/")
		public String get() { return "OK"; }
	}

	@Test void f01_contentTypeMenuItem_rendersLabel() throws Exception {
		var c = MockRestClient.build(F.class);
		var html = c.get("/").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "content-type");
	}

	//------------------------------------------------------------------------------------------------------------------
	// g - QueryMenuItem via MockRestClient
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(
		widgets={QueryMenuItem.class},
		navlinks={"$W{QueryMenuItem}"}
	)
	public static class G extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet("/")
		public String get() { return "OK"; }
	}

	@Test void g01_queryMenuItem_rendersLabel() throws Exception {
		var c = MockRestClient.build(G.class);
		var html = c.get("/").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(html, "query");
	}

	//------------------------------------------------------------------------------------------------------------------
	// h - MenuItemWidget base behavior
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_menuItemWidget_getLabel() {
		assertEquals("query", new QueryMenuItem().getLabel(null, null));
		assertEquals("content-type", new ContentTypeMenuItem().getLabel(null, null));
		assertEquals("themes", new ThemeMenuItem().getLabel(null, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private static void assertContains(String s, String needle) {
		if (!s.contains(needle))
			throw new AssertionError("Expected to contain '" + needle + "' but did not.\nBody:\n" + s);
	}
}
