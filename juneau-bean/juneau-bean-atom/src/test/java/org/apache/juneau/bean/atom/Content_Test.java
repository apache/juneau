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
}