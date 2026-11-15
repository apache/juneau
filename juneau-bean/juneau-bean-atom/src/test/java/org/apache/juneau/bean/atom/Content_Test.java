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
package org.apache.juneau.bean.atom;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

class Content_Test extends TestBase {

	@Test void a01_basic() {
		var x = new Content("text");
		assertNotNull(x);
		assertEquals("text", x.getType());
	}

	@Test void a02_withText() {
		var x = new Content("html");
		x.setText("<p>HTML content</p>");
		assertEquals("<p>HTML content</p>", x.getText());
	}

	@Test void a03_withSrc() {
		var x = new Content();
		x.setSrc("http://example.com/content");
		assertEquals("http://example.com/content", x.getSrc().toString());
	}

	@Test void a04_fluentSetters() {
		var x = new Content();

		// Test setType returns same instance for fluent chaining
		assertSame(x, x.setType("html"));
		assertEquals("html", x.getType());

		// Test setText returns same instance
		assertSame(x, x.setText("Test text"));
		assertEquals("Test text", x.getText());

		// Test setSrc returns same instance
		assertSame(x, x.setSrc("http://example.com/video.mp4"));
		assertEquals("http://example.com/video.mp4", x.getSrc().toString());

		// Test setBase returns same instance (from Common)
		assertSame(x, x.setBase("http://example.com/"));

		// Test setLang returns same instance (from Common)
		assertSame(x, x.setLang("en"));
	}

	@Test void a05_fluentChaining() {
		// Test multiple fluent calls can be chained
		var x = new Content()
			.setType("xhtml")
			.setText("<div>XHTML content</div>")
			.setBase("http://example.com/")
			.setLang("en-US");

		assertEquals("xhtml", x.getType());
		assertEquals("<div>XHTML content</div>", x.getText());
		assertEquals("en-US", x.getLang());
	}

	@Test void a06_outOfLineContent() {
		// Test external content (out-of-line) with src attribute
		var x = new Content()
			.setType("video/mp4")
			.setSrc("http://example.org/movie.mp4");

		assertEquals("video/mp4", x.getType());
		assertEquals("http://example.org/movie.mp4", x.getSrc().toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Text escaping
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_text_markupCharactersEscaped() throws Exception {
		var x = new Text("text").setText("a <b> & c");
		var xml = XmlSerializer.create().sq().build().write(x);
		assertTrue(xml.contains("a &lt;b&gt; &amp; c"), xml);
		assertFalse(xml.contains("<b>"), xml);
		assertEquals("a <b> & c", XmlParser.DEFAULT.read(xml, Text.class).getText());
	}

	@Test void c02_html_markupCharactersEscaped() throws Exception {
		var x = new Content("html").setText("<p>Hi &amp; bye</p>");
		var xml = XmlSerializer.create().sq().build().write(x);
		assertFalse(xml.contains("<p>"), xml);
		assertTrue(xml.contains("&lt;p&gt;Hi &amp;amp; bye&lt;/p&gt;"), xml);
		assertEquals("<p>Hi &amp; bye</p>", XmlParser.DEFAULT.read(xml, Content.class).getText());
	}

	@Test void c03_defaultType_escaped() throws Exception {
		var x = new Text().setText("<i>x</i>");
		var xml = XmlSerializer.create().sq().build().write(x);
		assertFalse(xml.contains("<i>"), xml);
		assertEquals("<i>x</i>", XmlParser.DEFAULT.read(xml, Text.class).getText());
	}

	@Test void c04_xhtml_passesThrough() throws Exception {
		var x = new Content("xhtml").setText("<div><p>ok</p></div>");
		var xml = XmlSerializer.create().sq().build().write(x);
		assertTrue(xml.contains("<div><p>ok</p></div>"), xml);
		assertEquals("<div><p>ok</p></div>", XmlParser.DEFAULT.read(xml, Content.class).getText());
	}

	@Test void c05_plainText_roundTrips() throws Exception {
		var x = new Text("text").setText("plain");
		var xml = XmlSerializer.create().sq().build().write(x);
		assertEquals("plain", XmlParser.DEFAULT.read(xml, Text.class).getText());
	}
}