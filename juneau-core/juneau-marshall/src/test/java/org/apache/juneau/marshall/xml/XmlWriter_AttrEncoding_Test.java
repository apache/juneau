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
package org.apache.juneau.marshall.xml;

import static org.apache.juneau.marshall.html.HtmlFormat.*;
import static org.apache.juneau.marshall.xml.XmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.html.*;
import org.junit.jupiter.api.*;

/**
 * Tests that attribute values written through {@link XmlWriter} are entity-encoded by default.
 *
 * <p>
 * An unencoded attribute value containing a quote character terminates the attribute early and lets the remainder of
 * the value be interpreted as markup, so any caller placing dynamic text into an attribute would otherwise be an
 * injection sink.  The no-encode overloads remain available for values already known to be safe attribute text.
 */
class XmlWriter_AttrEncoding_Test extends TestBase {

	@Test void a01_hostileValueCannotBreakOutOfTheAttribute() {
		var w = new SimpleHtmlWriter();
		w.oTag("div").attr("aria-label", "\"><script>alert(1)</script>").ceTag();
		var r = w.toString();
		assertEquals("<div aria-label='&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;'/>", r);
		assertFalse(r.contains("<script>"), r);
	}

	@Test void a02_theXmlSpecialCharactersBecomeEntities() {
		var w = new SimpleHtmlWriter();
		w.oTag("div").attr("title", "&\"<>'").ceTag();
		assertEquals("<div title='&amp;&quot;&lt;&gt;&apos;'/>", w.toString());
	}

	@Test void a03_ordinaryValuesArePassedThroughUnchanged() {
		var w = new SimpleHtmlWriter();
		w.oTag("div").attr("class", "btn btn-primary").attr("data-id", "c1:more").ceTag();
		assertEquals("<div class='btn btn-primary' data-id='c1:more'/>", w.toString());
	}

	@Test void a04_valuesAreTakenAsRawTextSoAPreEscapedValueIsEscapedAgain() {
		// Documents the caller contract: attr() owns the escaping, so callers must hand it raw text.
		var w = new SimpleHtmlWriter();
		w.oTag("div").attr("title", "a &amp; b").ceTag();
		assertEquals("<div title='a &amp;amp; b'/>", w.toString());
	}

	@Test void a05_theNoEncodeOverloadStillEmitsVerbatim() {
		var w = new SimpleHtmlWriter();
		w.oTag("div").attr("data-json", "{\"a\":1}", false).ceTag();
		assertEquals("<div data-json='{\"a\":1}'/>", w.toString());
	}

	@Marshalled(typeName = "A")
	public static class B01_Bean {
		@Xml(format = ATTR)
		public String label;
	}

	@Test void b01_xmlSerializerEncodesAttributeProperties() throws Exception {
		var a = new B01_Bean();
		a.label = "\"><script>alert(1)</script>";
		var r = XmlSerializer.DEFAULT_SQ.write(a);
		assertEquals("<A label='&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;'/>", r);
		assertFalse(r.contains("<script>"), r);
	}

	@Test void b02_encodedAttributesRoundTripBackToTheOriginalValue() throws Exception {
		var a = new B01_Bean();
		a.label = "a & b < c > d \" e ' f";
		var r = XmlSerializer.DEFAULT_SQ.write(a);
		assertEquals(a.label, XmlParser.DEFAULT.read(r, B01_Bean.class).label);
	}

	@Html(format = XML)
	@Marshalled(typeName = "div")
	public static class C01_Bean {
		@Xml(format = ATTR)
		public String label;
	}

	@Test void c01_htmlSerializerEncodesAttributeProperties() throws Exception {
		var a = new C01_Bean();
		a.label = "\"><script>alert(1)</script>";
		var r = HtmlSerializer.DEFAULT_SQ.write(a);
		assertEquals("<div label='&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;'></div>", r);
		assertFalse(r.contains("<script>"), r);
	}
}
