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

import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

/**
 * Tests that attributes written for XML-format beans in HTML output are filtered.
 */
class HtmlSerializer_XmlFormatAttrs_Test extends TestBase {

	@Html(format = HtmlFormat.XML)
	public static class El {
		private final Map<String,Object> attrs = new LinkedHashMap<>();
		private URI href;

		@Xml(format = XmlFormat.ATTRS) public Map<String,Object> getAttrs() { return attrs; }
		@Xml(format = XmlFormat.ATTR) @Uri public URI getHref() { return href; }
		public El href(String v) { href = URI.create(v); return this; }
		public El attr(String k, Object v) { attrs.put(k, v); return this; }
	}

	@Test void a01_disallowedSchemeInUriPropertyOmitted() throws Exception {
		var out = HtmlSerializer.DEFAULT.write(new El().href("javascript:foo()"));
		assertFalse(out.contains("javascript"), out);
	}

	@Test void a02_disallowedSchemeInAttrsMapOmitted() throws Exception {
		var out = HtmlSerializer.DEFAULT.write(new El().attr("src", "JaVaScRiPt:foo()").attr("action", "data:text/html,x"));
		assertFalse(out.toLowerCase().contains("javascript"), out);
		assertFalse(out.contains("data:"), out);
	}

	@Test void a03_allowedSchemeKept() throws Exception {
		var out = HtmlSerializer.DEFAULT.write(new El().attr("href", "https://x/y").attr("class", "c"));
		assertTrue(out.contains("https://x/y"), out);
		assertTrue(out.contains("class="), out);
	}

	@Test void a04_invalidAttributeNameOmitted() throws Exception {
		var out = HtmlSerializer.DEFAULT.write(new El().attr("x onmouseover=foo() y", "z").attr("ok", "v"));
		assertFalse(out.contains("onmouseover"), out);
		assertTrue(out.contains("ok="), out);
	}
}
