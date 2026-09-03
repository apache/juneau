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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.html.*;
import org.junit.jupiter.api.*;

/**
 * Tests that attribute values set on an {@link HtmlElement} are entity-escaped when the element is serialized.
 *
 * <p>
 * Element text children have always been escaped; these lock in the matching guarantee for attribute values, which is
 * what makes it safe to route dynamic text (labels, tooltips, ids) into an attribute.
 */
class HtmlElement_AttrEncoding_Test extends TestBase {

	@Test void a01_aQuoteBearingValueCannotBreakOutOfItsAttribute() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(div().attr("aria-label", "\"><script>alert(1)</script>"));
		assertFalse(r.contains("<script>"), r);
		assertTrue(r.contains("aria-label='&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;'"), r);
	}

	@Test void a02_theXmlSpecialCharactersBecomeEntities() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(div().attr("title", "&\"<>'"));
		assertTrue(r.contains("title='&amp;&quot;&lt;&gt;&apos;'"), r);
	}

	@Test void a03_ordinaryValuesArePassedThroughUnchanged() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(div().class_("btn btn-primary").id("c1"));
		assertTrue(r.contains("class='btn btn-primary'"), r);
		assertTrue(r.contains("id='c1'"), r);
		assertFalse(r.contains("&amp;"), r);
	}

	@Test void a04_aQueryStringHrefIsEncodedOnceNotTwice() throws Exception {
		var r = HtmlSerializer.DEFAULT_SQ.write(a("http://x/y?a=1&b=2", "text"));
		assertEquals("<a href='http://x/y?a=1&amp;b=2'>text</a>", r);
	}
}
