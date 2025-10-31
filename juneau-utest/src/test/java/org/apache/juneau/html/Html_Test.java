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
package org.apache.juneau.html;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.html.annotation.HtmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

@SuppressWarnings("serial")
class Html_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Verifies that lists of maps/beans are converted to tables correctly.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_testTables1() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;
		var t = a(new A1(), new A1());
		var html = s.serialize(t);
		assertEquals("<table _type='array'><tr><th>f1</th></tr><tr><td>f1</td></tr><tr><td>f1</td></tr></table>", html);
	}

	public static class A1 {
		public String f1 = "f1";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test URI_ANCHOR_SET options
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a02_testAnchorTextOptions() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().uriResolution(UriResolution.NONE);
		var t = new TestURI();

		s.uriAnchorText(AnchorText.TO_STRING);
		var r = strip(s.build().serialize(t));
		var expected = """

			[f0]=<a href='f0/x0'>f0/x0</a>
			[f1]=<a href='f1/x1'>f1/x1</a>
			[f2]=<a href='/f2/x2'>/f2/x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>
			[f4]=<a href='f4/x4'>f4/x4</a>
			[f5]=<a href='/f5/x5'>/f5/x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>
			[f8]=<a href='f8/x8'>f8/x8</a>
			[f9]=<a href='f9/x9'>f9/x9</a>
			[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>http://www.apache.org/fa/xa#MY_LABEL</a>
			[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>
			[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>
			[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>
			[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>""";
		assertEquals(expected, r);

		s.uriAnchorText(AnchorText.URI);
		r = strip(s.build().serialize(t));
		expected = """

			[f0]=<a href='f0/x0'>f0/x0</a>
			[f1]=<a href='f1/x1'>f1/x1</a>
			[f2]=<a href='/f2/x2'>/f2/x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>
			[f4]=<a href='f4/x4'>f4/x4</a>
			[f5]=<a href='/f5/x5'>/f5/x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>
			[f8]=<a href='f8/x8'>f8/x8</a>
			[f9]=<a href='f9/x9'>f9/x9</a>
			[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>http://www.apache.org/fa/xa#MY_LABEL</a>
			[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>
			[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>
			[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>
			[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>""";
		assertEquals(expected, r);

		s.uriAnchorText(AnchorText.LAST_TOKEN);
		r = strip(s.build().serialize(t));
		expected = """

			[f0]=<a href='f0/x0'>x0</a>
			[f1]=<a href='f1/x1'>x1</a>
			[f2]=<a href='/f2/x2'>x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>x3</a>
			[f4]=<a href='f4/x4'>x4</a>
			[f5]=<a href='/f5/x5'>x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>x7</a>
			[f8]=<a href='f8/x8'>x8</a>
			[f9]=<a href='f9/x9'>x9</a>
			[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>xa</a>
			[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>
			[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>
			[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>xd</a>
			[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>xe</a>""";
		assertEquals(expected, r);

		s.uriAnchorText(AnchorText.URI_ANCHOR);
		r = strip(s.build().serialize(t));
		expected = """

			[f0]=<a href='f0/x0'>f0/x0</a>
			[f1]=<a href='f1/x1'>f1/x1</a>
			[f2]=<a href='/f2/x2'>/f2/x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>
			[f4]=<a href='f4/x4'>f4/x4</a>
			[f5]=<a href='/f5/x5'>/f5/x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>
			[f8]=<a href='f8/x8'>f8/x8</a>
			[f9]=<a href='f9/x9'>f9/x9</a>
			[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>MY_LABEL</a>
			[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>
			[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>
			[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>
			[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>""";
		assertEquals(expected, r);

		s.labelParameter("label2");
		r = strip(s.build().serialize(t));
		expected = """

			[f0]=<a href='f0/x0'>f0/x0</a>
			[f1]=<a href='f1/x1'>f1/x1</a>
			[f2]=<a href='/f2/x2'>/f2/x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>
			[f4]=<a href='f4/x4'>f4/x4</a>
			[f5]=<a href='/f5/x5'>/f5/x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>
			[f8]=<a href='f8/x8'>f8/x8</a>
			[f9]=<a href='f9/x9'>f9/x9</a>
			[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>MY_LABEL</a>
			[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar</a>
			[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL</a>
			[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>MY_LABEL</a>
			[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>MY_LABEL</a>""";
		assertEquals(expected, r);

		s.disableDetectLinksInStrings();
		r = strip(s.build().serialize(t));
		expected = """

			[f0]=<a href='f0/x0'>f0/x0</a>
			[f1]=<a href='f1/x1'>f1/x1</a>
			[f2]=<a href='/f2/x2'>/f2/x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>
			[f4]=<a href='f4/x4'>f4/x4</a>
			[f5]=<a href='/f5/x5'>/f5/x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>
			[f8]=<a href='f8/x8'>f8/x8</a>
			[f9]=<a href='f9/x9'>f9/x9</a>
			[fa]=http://www.apache.org/fa/xa#MY_LABEL
			[fb]=http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar
			[fc]=http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL
			[fd]=http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar
			[fe]=http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL""";
		assertEquals(expected, r);

		s.disableDetectLinksInStrings(false);
		s.disableDetectLabelParameters();
		r = strip(s.build().serialize(t));
		expected = """

			[f0]=<a href='f0/x0'>f0/x0</a>
			[f1]=<a href='f1/x1'>f1/x1</a>
			[f2]=<a href='/f2/x2'>/f2/x2</a>
			[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>
			[f4]=<a href='f4/x4'>f4/x4</a>
			[f5]=<a href='/f5/x5'>/f5/x5</a>
			[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>
			[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>
			[f8]=<a href='f8/x8'>f8/x8</a>
			[f9]=<a href='f9/x9'>f9/x9</a>
			[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>MY_LABEL</a>
			[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar</a>
			[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL</a>
			[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>
			[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>""";
		assertEquals(expected, r);
	}

	private static String strip(String html) {
		return html
			.replace("<table><tr><th>key</th><th>value</th></tr>", "")
			.replace("</table>", "")
			.replace("<tr><td>", "\n[")
			.replace("</td><td>", "]=")
			.replace("</td></tr>", "");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test @Html.asPlainText annotation on classes and fields
	//-----------------------------------------------------------------------------------------------------------------
	@Test void b01_testHtmlAnnotationAsPlainText() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();

		var o = new B1();
		var r = s.serialize(o);
		assertEquals("<test>", r);

		var o2 = new B2();
		r = s.serialize(o2);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><f1></td></tr></table>", r);
	}

	@Html(format=PLAIN_TEXT)
	public static class B1 {
		public String f1 = "<f1>";
		@Override /* Overridden from Object */
		public String toString() {
			return "<test>";
		}
	}

	public static class B2 {
		@Html(format=PLAIN_TEXT)
		public String f1 = "<f1>";
	}

	@Test void b02_testHtmlAnnotationAsPlainText_usingConfig() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().applyAnnotations(B3Config.class).build();

		var o = new B3();
		var r = s.serialize(o);
		assertEquals("<test>", r);

		var o2 = new B4();
		r = s.serialize(o2);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><f1></td></tr></table>", r);
	}

	@Html(on="B3", format=PLAIN_TEXT)
	@Html(on="B4.f1", format=PLAIN_TEXT)
	private static class B3Config {}

	public static class B3 {
		public String f1 = "<f1>";
		@Override /* Overridden from Object */
		public String toString() {
			return "<test>";
		}
	}

	public static class B4 {
		public String f1 = "<f1>";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test @Html.asXml annotation on classes and fields
	//-----------------------------------------------------------------------------------------------------------------
	@Test void c01_testHtmlAnnotationAsXml() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();

		var o = new C1();
		var r = s.serialize(o);
		assertEquals("<object><f1>&lt;f1&gt;</f1></object>", r);

		var o2 = new C2();
		r = s.serialize(o2);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td>&lt;f1&gt;</td></tr></table>", r);
	}

	@Html(format=XML)
	public static class C1 {
		public String f1 = "<f1>";
	}

	public static class C2 {
		@Html(format=XML)
		public String f1 = "<f1>";
	}

	@Test void c02_testHtmlAnnotationAsXml_usingConfig() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().applyAnnotations(C3Config.class).build();

		var o = new C3();
		var r = s.serialize(o);
		assertEquals("<object><f1>&lt;f1&gt;</f1></object>", r);

		var o2 = new C4();
		r = s.serialize(o2);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td>&lt;f1&gt;</td></tr></table>", r);
	}

	@Html(on="C3,C4.f1",format=XML)
	private static class C3Config {}

	public static class C3 {
		public String f1 = "<f1>";
	}

	public static class C4 {
		public String f1 = "<f1>";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test @Html.noTableHeaders
	//-----------------------------------------------------------------------------------------------------------------
	@Test void d01_testNoTableHeaders() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;

		var m = new MyMap();
		m.put("foo", "bar");
		var o = JsonList.of(m);
		var r = s.serialize(o);
		assertEquals("<ul><li><table><tr><td>foo</td><td>bar</td></tr></table></li></ul>", r);
	}

	@Html(noTables=true, noTableHeaders=true)
	public static class MyMap extends LinkedHashMap<String,String> {}

	@Test void d02_testNoTableHeaders_usingConfig() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ.copy().applyAnnotations(MyMap2Config.class).build();

		var m = new MyMap2();
		m.put("foo", "bar");
		var o = JsonList.of(m);
		var r = s.serialize(o);
		assertEquals("<ul><li><table><tr><td>foo</td><td>bar</td></tr></table></li></ul>", r);
	}

	@Html(on="org.apache.juneau.html.Html_Test$MyMap2", noTables=true, noTableHeaders=true)
	private static class MyMap2Config {}

	public static class MyMap2 extends LinkedHashMap<String,String> {}

	//-----------------------------------------------------------------------------------------------------------------
	// Test @Html.noTableHeaders on beans
	//-----------------------------------------------------------------------------------------------------------------
	@Test void d03_testNoTableHeadersOnBeans() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;

		var b = new MyBean();
		var o = JsonList.of(b,b);
		var r = s.serialize(o);
		assertEquals("<table _type='array'><tr><td>1</td><td>2</td><td>3</td></tr><tr><td>1</td><td>2</td><td>3</td></tr></table>", r);
	}

	@Html(noTableHeaders=true)
	public static class MyBean {
		public int a=1,b=2,c=3;
	}

	@Test void d04_testNoTableHeadersOnBeans_usingConfig() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ.copy().applyAnnotations(MyBean2Config.class).build();

		var b = new MyBean();
		var o = JsonList.of(b,b);
		var r = s.serialize(o);
		assertEquals("<table _type='array'><tr><td>1</td><td>2</td><td>3</td></tr><tr><td>1</td><td>2</td><td>3</td></tr></table>", r);
	}

	@Html(on="MyBean2", noTableHeaders=true)
	private static class MyBean2Config {}

	public static class MyBean2 {
		public int a=1,b=2,c=3;
	}

	@Test void d05_testNoTableHeadersOnBeans_usingConcreteAnnotation() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ.copy().annotations(HtmlAnnotation.create("MyBean2").noTables(true).build()).build();

		var b = new MyBean();
		var o = JsonList.of(b,b);
		var r = s.serialize(o);
		assertEquals("<table _type='array'><tr><td>1</td><td>2</td><td>3</td></tr><tr><td>1</td><td>2</td><td>3</td></tr></table>", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Bean(bpi) on collections of beans
	//-----------------------------------------------------------------------------------------------------------------

	@Bean(p="f3,f2,f1")
	public static class E {
		public Integer f1, f2, f3;

		public E(Integer f1, Integer f2, Integer f3) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
		}
	}

	@Test void e01_collectionOfBeansWithBpi() {
		E[] ee = a(
			new E(null, 2, 3),
			new E(4, 5, 6)
		);
		assertEquals("<table _type='array'><tr><th>f3</th><th>f2</th><th>f1</th></tr><tr><td>3</td><td>2</td><td><null/></td></tr><tr><td>6</td><td>5</td><td>4</td></tr></table>", HtmlSerializer.DEFAULT_SQ.toString(ee));
		assertEquals("<table _type='array'><tr><th>f3</th><th>f2</th><th>f1</th></tr><tr><td>3</td><td>2</td><td><null/></td></tr><tr><td>6</td><td>5</td><td>4</td></tr></table>", HtmlSerializer.DEFAULT_SQ.toString(l(ee)));

		ee = a(
			new E(null, null, null),
			new E(null, null, null)
		);
		assertEquals("<table _type='array'><tr><th>f3</th><th>f2</th><th>f1</th></tr><tr><td><null/></td><td><null/></td><td><null/></td></tr><tr><td><null/></td><td><null/></td><td><null/></td></tr></table>", HtmlSerializer.DEFAULT_SQ.toString(ee));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test @Html(style) annotation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_htmlStyle() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;
		F[] ff = {
			new F("foo", "bar"),
			new F("baz", "qux")
		};
		var html = s.serialize(ff);
		assertTrue(html.contains("style='white-space:normal'"));
		assertTrue(html.contains("style='min-width:200px'"));
	}

	public static class F {
		@Html(style="white-space:normal")
		public String f1;

		@Html(style="min-width:200px")
		public String f2;

		public F(String f1, String f2) {
			this.f1 = f1;
			this.f2 = f2;
		}
	}
}