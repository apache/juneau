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
package org.apache.juneau.rest.beans;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.junit.jupiter.api.*;

/**
 * Tests for Hyperlink fluent setter overrides.
 */
class Hyperlink_Test extends TestBase {

	@Test void a01_fluentChaining_anchorAttributes() {
		var h = new Hyperlink();

		// Test that fluent methods return Hyperlink (not A)
		Hyperlink result;

		result = h.href("/foo");
		assertSame(h, result);
		assertInstanceOf(Hyperlink.class, result);

		result = h.download("file.pdf");
		assertSame(h, result);

		result = h.hreflang("en");
		assertSame(h, result);

		result = h.rel("nofollow");
		assertSame(h, result);

		result = h.target("_blank");
		assertSame(h, result);

		result = h.type("text/html");
		assertSame(h, result);
	}

	@Test void a02_fluentChaining_globalAttributes() {
		var h = new Hyperlink();

		// Test global HTML attributes
		Hyperlink result;

		result = h._class("link-class");
		assertSame(h, result);
		assertInstanceOf(Hyperlink.class, result);

		result = h.id("link-id");
		assertSame(h, result);

		result = h.style("color:blue");
		assertSame(h, result);

		result = h.title("Link title");
		assertSame(h, result);

		result = h.lang("en");
		assertSame(h, result);

		result = h.accesskey("l");
		assertSame(h, result);

		result = h.contenteditable(true);
		assertSame(h, result);

		result = h.dir("ltr");
		assertSame(h, result);

		result = h.hidden(false);
		assertSame(h, result);

		result = h.spellcheck(true);
		assertSame(h, result);

		result = h.tabindex(1);
		assertSame(h, result);

		result = h.translate(false);
		assertSame(h, result);
	}

	@Test void a03_fluentChaining_eventHandlers() {
		var h = new Hyperlink();

		// Test event handler attributes
		Hyperlink result;

		result = h.onclick("alert('clicked')");
		assertSame(h, result);
		assertInstanceOf(Hyperlink.class, result);

		result = h.onmouseover("console.log('hover')");
		assertSame(h, result);

		result = h.onfocus("console.log('focus')");
		assertSame(h, result);

		result = h.onblur("console.log('blur')");
		assertSame(h, result);

		result = h.onload("console.log('load')");
		assertSame(h, result);
	}

	@Test void a04_fluentChaining_childMethods() {
		var h = new Hyperlink();

		// Test child/attribute methods
		Hyperlink result;

		result = h.child("Link text");
		assertSame(h, result);
		assertInstanceOf(Hyperlink.class, result);

		result = h.children("More", " text");
		assertSame(h, result);

		result = h.attr("data-test", "value");
		assertSame(h, result);

		result = h.attrUri("data-uri", "http://test.com");
		assertSame(h, result);
	}

	@Test void a05_fluentChaining_complex() {
		// Test chaining multiple fluent calls
		var result = new Hyperlink()
			.href("/path/to/page")
			.target("_blank")
			._class("nav-link")
			.id("main-link")
			.onclick("track()")
			.children("Click here");

		assertInstanceOf(Hyperlink.class, result);
	}

	@Test void a06_output_basic() throws Exception {
		var h = new Hyperlink("/foo", "bar");

		String html = HtmlSerializer.DEFAULT.serialize(h);

		assertTrue(html.contains("href"));
		assertTrue(html.contains("/foo"));
		assertTrue(html.contains("bar"));
	}

	@Test void a07_output_withAttributes() throws Exception {
		var h = new Hyperlink()
			.href("/path")
			.target("_blank")
			._class("link")
			.id("my-link")
			.children("Link text");

		String html = HtmlSerializer.DEFAULT.serialize(h);

		assertTrue(html.contains("href"));
		assertTrue(html.contains("/path"));
		assertTrue(html.contains("target"));
		assertTrue(html.contains("_blank"));
		assertTrue(html.contains("class"));
		assertTrue(html.contains("link"));
		assertTrue(html.contains("id"));
		assertTrue(html.contains("my-link"));
		assertTrue(html.contains("Link text"));
	}

	@Test void a08_output_eventHandlers() throws Exception {
		var h = new Hyperlink()
			.href("#")
			.onclick("doSomething()")
			.onmouseover("highlight(this)")
			.children("Interactive");

		String html = HtmlSerializer.DEFAULT.serialize(h);

		assertTrue(html.contains("onclick"));
		assertTrue(html.contains("doSomething"));
		assertTrue(html.contains("onmouseover"));
		assertTrue(html.contains("highlight"));
	}

	@Test void a09_staticCreator() {
		var h = Hyperlink.create("/test", "Test link");

		assertInstanceOf(Hyperlink.class, h);
	}

	@Test void a10_staticCreator_output() throws Exception {
		var h = Hyperlink.create("/static", "Static link");

		String html = HtmlSerializer.DEFAULT.serialize(h);

		assertTrue(html.contains("/static"));
		assertTrue(html.contains("Static link"));
	}

	@Test void a11_multipleEventHandlers() {
		var h = new Hyperlink();

		// Test multiple event handlers
		Hyperlink result = h
			.onabort("handle1()")
			.oncancel("handle2()")
			.oncanplay("handle3()")
			.onchange("handle4()")
			.ondblclick("handle5()")
			.ondurationchange("handle6()")
			.onemptied("handle7()")
			.onended("handle8()")
			.onerror("handle9()")
			.oninput("handle10()")
			.oninvalid("handle11()")
			.onkeydown("handle12()")
			.onkeypress("handle13()")
			.onkeyup("handle14()")
			.onloadeddata("handle15()")
			.onloadedmetadata("handle16()")
			.onloadstart("handle17()")
			.onmousedown("handle18()")
			.onmouseenter("handle19()")
			.onmouseleave("handle20()")
			.onmousemove("handle21()")
			.onmouseout("handle22()")
			.onmouseup("handle23()")
			.onmousewheel("handle24()")
			.onpause("handle25()")
			.onplay("handle26()")
			.onplaying("handle27()")
			.onprogress("handle28()")
			.onratechange("handle29()")
			.onreset("handle30()")
			.onresize("handle31()")
			.onscroll("handle32()")
			.onseeked("handle33()")
			.onseeking("handle34()")
			.onselect("handle35()")
			.onshow("handle36()")
			.onstalled("handle37()")
			.onsubmit("handle38()")
			.onsuspend("handle39()")
			.ontimeupdate("handle40()")
			.ontoggle("handle41()")
			.onvolumechange("handle42()")
			.onwaiting("handle43()")
			.oncuechange("handle44()");

		assertSame(h, result);
		assertInstanceOf(Hyperlink.class, result);
	}
}