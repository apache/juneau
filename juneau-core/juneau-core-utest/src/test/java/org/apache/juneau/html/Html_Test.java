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
package org.apache.juneau.html;

import static org.apache.juneau.html.annotation.HtmlFormat.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@SuppressWarnings({"unchecked","rawtypes","serial"})
@FixMethodOrder(NAME_ASCENDING)
public class Html_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Verifies that lists of maps/beans are converted to tables correctly.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void a01_testTables1() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		Object[] t;
		String html;

		t = new Object[] {new A1(), new A1()};
		html = s.serialize(t);
		assertEquals("<table _type='array'><tr><th>f1</th></tr><tr><td>f1</td></tr><tr><td>f1</td></tr></table>", html);

	}

	public static class A1 {
		public String f1 = "f1";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test URI_ANCHOR_SET options
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void a02_testAnchorTextOptions() throws Exception {
		HtmlSerializerBuilder s = HtmlSerializer.create().sq().addKeyValueTableHeaders().uriResolution(UriResolution.NONE);
		TestURI t = new TestURI();
		String r;
		String expected = null;

		s.uriAnchorText(AnchorText.TO_STRING);
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>"
			+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>"
			+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>http://www.apache.org/fa/xa#MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.uriAnchorText(AnchorText.URI);
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>"
			+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>"
			+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>http://www.apache.org/fa/xa#MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.uriAnchorText(AnchorText.LAST_TOKEN);
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>x0</a>"
			+"\n[f1]=<a href='f1/x1'>x1</a>"
			+"\n[f2]=<a href='/f2/x2'>x2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>x3</a>"
			+"\n[f4]=<a href='f4/x4'>x4</a>"
			+"\n[f5]=<a href='/f5/x5'>x5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>x6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>x7</a>"
			+"\n[f8]=<a href='f8/x8'>x8</a>"
			+"\n[f9]=<a href='f9/x9'>x9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>xa</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>xd</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>xe</a>";
		assertEquals(expected, r);

		s.uriAnchorText(AnchorText.URI_ANCHOR);
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>"
			+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>"
			+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.labelParameter("label2");
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>"
			+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>"
			+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar</a>"
			+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>MY_LABEL</a>"
			+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>MY_LABEL</a>";
		assertEquals(expected, r);

		s.disableDetectLinksInStrings();
		r = strip(s.build().serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>"
			+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>"
			+"\n[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>"
			+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
			+"\n[fa]=http://www.apache.org/fa/xa#MY_LABEL"
			+"\n[fb]=http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar"
			+"\n[fc]=http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL"
			+"\n[fd]=http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar"
			+"\n[fe]=http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL";
			assertEquals(expected, r);

			s.unset(HtmlSerializer.HTML_disableDetectLinksInStrings);
			s.disableDetectLabelParameters();
			r = strip(s.build().serialize(t));
			expected = ""
				+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
				+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
				+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
				+"\n[f3]=<a href='http://www.apache.org/f3/x3'>http://www.apache.org/f3/x3</a>"
				+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
				+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
				+"\n[f6]=<a href='http://www.apache.org/f6/x6'>http://www.apache.org/f6/x6</a>"
				+"\n[f7]=<a href='http://www.apache.org/f7/x7'>http://www.apache.org/f7/x7</a>"
				+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
				+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
				+"\n[fa]=<a href='http://www.apache.org/fa/xa#MY_LABEL'>MY_LABEL</a>"
				+"\n[fb]=<a href='http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar'>http://www.apache.org/fb/xb?label=MY_LABEL&amp;foo=bar</a>"
				+"\n[fc]=<a href='http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL'>http://www.apache.org/fc/xc?foo=bar&amp;label=MY_LABEL</a>"
				+"\n[fd]=<a href='http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar'>http://www.apache.org/fd/xd?label2=MY_LABEL&amp;foo=bar</a>"
				+"\n[fe]=<a href='http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL'>http://www.apache.org/fe/xe?foo=bar&amp;label2=MY_LABEL</a>";
			assertEquals(expected, r);
	}

	private String strip(String html) {
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
	@Test
	public void b01_testHtmlAnnotationAsPlainText() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();
		Object o = null;
		String r;

		o = new B1();
		r = s.serialize(o);
		assertEquals("<test>", r);

		o = new B2();
		r = s.serialize(o);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><f1></td></tr></table>", r);
	}

	@Html(format=PLAIN_TEXT)
	public static class B1 {
		public String f1 = "<f1>";
		@Override /* Object */
		public String toString() {
			return "<test>";
		}
	}

	public static class B2 {
		@Html(format=PLAIN_TEXT)
		public String f1 = "<f1>";
	}

	@Test
	public void b02_testHtmlAnnotationAsPlainText_usingConfig() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().addKeyValueTableHeaders().applyAnnotations(B3Config.class).build();

		Object o = null;
		String r;

		o = new B3();
		r = s.serialize(o);
		assertEquals("<test>", r);

		o = new B4();
		r = s.serialize(o);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><f1></td></tr></table>", r);
	}

	@Html(on="B3", format=PLAIN_TEXT)
	@Html(on="B4.f1", format=PLAIN_TEXT)
	private static class B3Config {}

	public static class B3 {
		public String f1 = "<f1>";
		@Override /* Object */
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
	@Test
	public void c01_testHtmlAnnotationAsXml() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();
		Object o = null;
		String r;

		o = new C1();
		r = s.serialize(o);
		assertEquals("<object><f1>&lt;f1&gt;</f1></object>", r);

		o = new C2();
		r = s.serialize(o);
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

	@Test
	public void c02_testHtmlAnnotationAsXml_usingConfig() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().addKeyValueTableHeaders().applyAnnotations(C3Config.class).build();
		Object o = null;
		String r;

		o = new C3();
		r = s.serialize(o);
		assertEquals("<object><f1>&lt;f1&gt;</f1></object>", r);

		o = new C4();
		r = s.serialize(o);
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
	@Test
	public void d01_testNoTableHeaders() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		Object o = null;
		String r;

		Map m = new MyMap();
		m.put("foo", "bar");
		o = OList.of(m);
		r = s.serialize(o);
		assertEquals("<ul><li><table><tr><td>foo</td><td>bar</td></tr></table></li></ul>", r);
	}

	@Html(noTables=true, noTableHeaders=true)
	public static class MyMap extends LinkedHashMap<String,String> {}

	@Test
	public void d02_testNoTableHeaders_usingConfig() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ.builder().applyAnnotations(MyMap2Config.class).build();
		Object o = null;
		String r;

		Map m = new MyMap2();
		m.put("foo", "bar");
		o = OList.of(m);
		r = s.serialize(o);
		assertEquals("<ul><li><table><tr><td>foo</td><td>bar</td></tr></table></li></ul>", r);
	}

	@Html(on="org.apache.juneau.html.Html_Test$MyMap2", noTables=true, noTableHeaders=true)
	private static class MyMap2Config {}

	public static class MyMap2 extends LinkedHashMap<String,String> {}

	//-----------------------------------------------------------------------------------------------------------------
	// Test @Html.noTableHeaders on beans
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void d03_testNoTableHeadersOnBeans() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		Object o = null;
		String r;

		MyBean b = new MyBean();
		o = OList.of(b,b);
		r = s.serialize(o);
		assertEquals("<table _type='array'><tr><td>1</td><td>2</td><td>3</td></tr><tr><td>1</td><td>2</td><td>3</td></tr></table>", r);
	}

	@Html(noTableHeaders=true)
	public static class MyBean {
		public int a=1,b=2,c=3;
	}

	@Test
	public void d04_testNoTableHeadersOnBeans_usingConfig() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ.builder().applyAnnotations(MyBean2Config.class).build();
		Object o = null;
		String r;

		MyBean b = new MyBean();
		o = OList.of(b,b);
		r = s.serialize(o);
		assertEquals("<table _type='array'><tr><td>1</td><td>2</td><td>3</td></tr><tr><td>1</td><td>2</td><td>3</td></tr></table>", r);
	}

	@Html(on="MyBean2", noTableHeaders=true)
	private static class MyBean2Config {}

	public static class MyBean2 {
		public int a=1,b=2,c=3;
	}

	@Test
	public void d05_testNoTableHeadersOnBeans_usingConcreteAnnotation() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ.builder().annotations(HtmlAnnotation.create("MyBean2").noTables(true).build()).build();
		Object o = null;
		String r;

		MyBean b = new MyBean();
		o = OList.of(b,b);
		r = s.serialize(o);
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

	@Test
	public void e01_collectionOfBeansWithBpi() throws Exception {
		E[] ee = new E[]{
			new E(null, 2, 3),
			new E(4, 5, 6)
		};
		assertEquals("<table _type='array'><tr><th>f3</th><th>f2</th><th>f1</th></tr><tr><td>3</td><td>2</td><td><null/></td></tr><tr><td>6</td><td>5</td><td>4</td></tr></table>", HtmlSerializer.DEFAULT_SQ.toString(ee));
		assertEquals("<table _type='array'><tr><th>f3</th><th>f2</th><th>f1</th></tr><tr><td>3</td><td>2</td><td><null/></td></tr><tr><td>6</td><td>5</td><td>4</td></tr></table>", HtmlSerializer.DEFAULT_SQ.toString(Arrays.asList(ee)));

		ee = new E[] {
			new E(null, null, null),
			new E(null, null, null)
		};
		assertEquals("<table _type='array'><tr><th>f3</th><th>f2</th><th>f1</th></tr><tr><td><null/></td><td><null/></td><td><null/></td></tr><tr><td><null/></td><td><null/></td><td><null/></td></tr></table>", HtmlSerializer.DEFAULT_SQ.toString(ee));
	}
}