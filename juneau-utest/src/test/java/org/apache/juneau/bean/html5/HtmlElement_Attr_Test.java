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
			.class_("test");
		var result = x.toString();
		assertNotNull(result);
		assertTrue(result.contains("data-test='value'"));
		assertTrue(result.contains("data-url='http://test.com'"));
		assertTrue(result.contains("class='test'"));
	}

	// attrUri() on all element classes not covered by a01-a11
	@Test void b01_attrUri_allRemainingElements() {
		var u = "http://example.org/";
		assertNotNull(abbr().attrUri("data-u", u));
		assertNotNull(address().attrUri("data-u", u));
		assertNotNull(article().attrUri("data-u", u));
		assertNotNull(aside().attrUri("data-u", u));
		assertNotNull(b().attrUri("data-u", u));
		assertNotNull(bdi().attrUri("data-u", u));
		assertNotNull(bdo().attrUri("data-u", u));
		assertNotNull(blockquote().attrUri("data-u", u));
		assertNotNull(body().attrUri("data-u", u));
		assertNotNull(br().attrUri("data-u", u));
		assertNotNull(canvas().attrUri("data-u", u));
		assertNotNull(caption().attrUri("data-u", u));
		assertNotNull(cite().attrUri("data-u", u));
		assertNotNull(code().attrUri("data-u", u));
		assertNotNull(col().attrUri("data-u", u));
		assertNotNull(colgroup().attrUri("data-u", u));
		assertNotNull(data().attrUri("data-u", u));
		assertNotNull(datalist().attrUri("data-u", u));
		assertNotNull(dd().attrUri("data-u", u));
		assertNotNull(del().attrUri("data-u", u));
		assertNotNull(dfn().attrUri("data-u", u));
		assertNotNull(dl().attrUri("data-u", u));
		assertNotNull(dt().attrUri("data-u", u));
		assertNotNull(em().attrUri("data-u", u));
		assertNotNull(fieldset().attrUri("data-u", u));
		assertNotNull(figcaption().attrUri("data-u", u));
		assertNotNull(figure().attrUri("data-u", u));
		assertNotNull(footer().attrUri("data-u", u));
		assertNotNull(h1().attrUri("data-u", u));
		assertNotNull(h2().attrUri("data-u", u));
		assertNotNull(h3().attrUri("data-u", u));
		assertNotNull(h4().attrUri("data-u", u));
		assertNotNull(h5().attrUri("data-u", u));
		assertNotNull(h6().attrUri("data-u", u));
		assertNotNull(head().attrUri("data-u", u));
		assertNotNull(header().attrUri("data-u", u));
		assertNotNull(hr().attrUri("data-u", u));
		assertNotNull(html().attrUri("data-u", u));
		assertNotNull(i().attrUri("data-u", u));
		assertNotNull(input().attrUri("data-u", u));
		assertNotNull(ins().attrUri("data-u", u));
		assertNotNull(kbd().attrUri("data-u", u));
		assertNotNull(keygen().attrUri("data-u", u));
		assertNotNull(label().attrUri("data-u", u));
		assertNotNull(legend().attrUri("data-u", u));
		assertNotNull(li().attrUri("data-u", u));
		assertNotNull(link().attrUri("data-u", u));
		assertNotNull(main().attrUri("data-u", u));
		assertNotNull(map().attrUri("data-u", u));
		assertNotNull(mark().attrUri("data-u", u));
		assertNotNull(meta().attrUri("data-u", u));
		assertNotNull(meter().attrUri("data-u", u));
		assertNotNull(nav().attrUri("data-u", u));
		assertNotNull(noscript().attrUri("data-u", u));
		assertNotNull(object().attrUri("data-u", u));
		assertNotNull(ol().attrUri("data-u", u));
		assertNotNull(optgroup().attrUri("data-u", u));
		assertNotNull(option().attrUri("data-u", u));
		assertNotNull(output().attrUri("data-u", u));
		assertNotNull(p().attrUri("data-u", u));
		assertNotNull(param().attrUri("data-u", u));
		assertNotNull(pre().attrUri("data-u", u));
		assertNotNull(progress().attrUri("data-u", u));
		assertNotNull(q().attrUri("data-u", u));
		assertNotNull(rb().attrUri("data-u", u));
		assertNotNull(rp().attrUri("data-u", u));
		assertNotNull(rt().attrUri("data-u", u));
		assertNotNull(rtc().attrUri("data-u", u));
		assertNotNull(ruby().attrUri("data-u", u));
		assertNotNull(s().attrUri("data-u", u));
		assertNotNull(samp().attrUri("data-u", u));
		assertNotNull(section().attrUri("data-u", u));
		assertNotNull(select().attrUri("data-u", u));
		assertNotNull(small().attrUri("data-u", u));
		assertNotNull(span().attrUri("data-u", u));
		assertNotNull(strong().attrUri("data-u", u));
		assertNotNull(sub().attrUri("data-u", u));
		assertNotNull(sup().attrUri("data-u", u));
		assertNotNull(table().attrUri("data-u", u));
		assertNotNull(tbody().attrUri("data-u", u));
		assertNotNull(td().attrUri("data-u", u));
		assertNotNull(template().attrUri("data-u", u));
		assertNotNull(textarea().attrUri("data-u", u));
		assertNotNull(tfoot().attrUri("data-u", u));
		assertNotNull(th().attrUri("data-u", u));
		assertNotNull(thead().attrUri("data-u", u));
		assertNotNull(time().attrUri("data-u", u));
		assertNotNull(title().attrUri("data-u", u));
		assertNotNull(tr().attrUri("data-u", u));
		assertNotNull(u().attrUri("data-u", u));
		assertNotNull(ul().attrUri("data-u", u));
		assertNotNull(var().attrUri("data-u", u));
		assertNotNull(wbr().attrUri("data-u", u));
	}

	// HtmlElement.attr() missing branches (line 97): url/href/action keys
	@Test void b02_htmlElement_attr_urlAndActionKeys() {
		var x = div()
			.attr("url", "http://example.org/")
			.attr("href", "http://example.org/page")
			.attr("formaction", "http://example.org/submit");
		assertTrue(x.toString().contains("url='http://example.org/'"));
		assertTrue(x.toString().contains("href='http://example.org/page'"));
		assertTrue(x.toString().contains("formaction='http://example.org/submit'"));
	}

	// HtmlElement.getAttr() line 186: attrs == null branch
	@Test void b03_htmlElement_getAttr_nullAttrs() {
		var x = new Div();
		assertNull(x.getAttr(String.class, "data-test"));
	}

	// HtmlElement.setAttrs() line 1054: url/href/action keys → converted to URI
	@Test void b04_htmlElement_setAttrs_withUriKeys() {
		var attrs = new java.util.LinkedHashMap<String, Object>();
		attrs.put("url", "http://example.org/");
		attrs.put("href", "http://example.org/page");
		attrs.put("formaction", "http://example.org/submit");
		attrs.put("class", "test");
		var x = div().setAttrs(attrs);
		var str = x.toString();
		assertTrue(str.contains("url='http://example.org/'"));
		assertTrue(str.contains("href='http://example.org/page'"));
		assertTrue(str.contains("formaction='http://example.org/submit'"));
	}

	// HtmlElementMixed.getChild() through HtmlElementContainer (lines 153-154)
	@Test void b05_htmlElementMixed_getChild_throughContainer() {
		var x = p(
			table(
				tr(
					td("cell")
				)
			)
		);
		// Navigates: p[0] = table (HtmlElementContainer) → hits else-if branch on line 153
		assertNotNull(x.getChild(0, 0));
	}

	// HtmlBuilder missing factory methods with children args
	@Test void b06_htmlBuilder_missingFactoryMethods() {
		assertNotNull(object("child1"));
		assertNotNull(progress("child1"));
		assertNotNull(rt("child1"));
	}
}