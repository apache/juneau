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
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for attr() and attrUri() fluent setter overrides in HTML5 element classes.
 */
class HtmlElement_Attr_Test extends TestBase {

	@Test void a01_div_attr_fluent_chaining() {
		Div x = div();
		// Verify fluent chaining returns same instance
		assertSame(x, x.attr("data-test", "value1"));
		assertSame(x, x.attr("data-foo", "value2"));
		assertString("<div data-test='value1' data-foo='value2'></div>", x);
	}

	@Test void a02_div_attrUri_fluent_chaining() {
		Div x = div();
		assertSame(x, x.attrUri("data-url", "http://localhost/test"));
		assertString("<div data-url='http://localhost/test'></div>", x);
	}

	@Test void a03_figure_attr_chaining() {
		Figure x = figure()
			.attr("data-id", "123")
			.attr("role", "img");
		assertString("<figure data-id='123' role='img'></figure>", x);
	}

	@Test void a04_br_attr() {
		Br x = br();
		assertSame(x, x.attr("data-break", "soft"));
		assertString("<br data-break='soft'/>", x);
	}

	@Test void a05_img_attr() {
		Img x = img()
			.attr("alt", "Test")
			.attrUri("src", "http://example.com/img.png");
		assertTrue(x.toString().contains("alt='Test'"));
		assertTrue(x.toString().contains("src='http://example.com/img.png'"));
	}

	@Test void a06_b_attr() {
		B x = b();
		assertSame(x, x.attr("data-weight", "bold"));
		assertString("<b data-weight='bold'></b>", x);
	}

	@Test void a07_script_attr() {
		Script x = script();
		assertSame(x, x.attr("type", "text/javascript"));
		assertSame(x, x.attrUri("src", "http://example.com/script.js"));
		assertTrue(x.toString().contains("type='text/javascript'"));
		assertTrue(x.toString().contains("src='http://example.com/script.js'"));
	}

	@Test void a08_style_attr() {
		Style x = style();
		assertSame(x, x.attr("type", "text/css"));
		assertTrue(x.toString().contains("type='text/css'"));
	}

	@Test void a09_attr_null_removes_attribute() {
		Div x = div()
			.attr("data-test", "value")
			.attr("data-test", null);
		assertString("<div></div>", x);
	}

	@Test void a10_attrUri_with_standard_attrs() {
		A x = a()
			.href("http://example.com")
			.attrUri("data-fallback", "http://fallback.com");
		assertTrue(x.toString().contains("href='http://example.com'"));
		assertTrue(x.toString().contains("data-fallback='http://fallback.com'"));
	}

	@Test void a11_no_serialization_errors() {
		// Verifies @Beanp annotation inheritance doesn't cause serialization errors
		Div x = div()
			.attr("data-test", "value")
			.attrUri("data-url", "http://test.com")
			._class("test");
		var result = x.toString();
		assertNotNull(result);
		assertTrue(result.contains("data-test='value'"));
		assertTrue(result.contains("data-url='http://test.com'"));
		assertTrue(result.contains("class='test'"));
	}
}