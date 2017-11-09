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

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.testbeans.*;
import org.junit.*;

@SuppressWarnings({"javadoc","unchecked","rawtypes","serial"})
public class HtmlTest {

	//====================================================================================================
	// Verifies that lists of maps/beans are converted to tables correctly.
	//====================================================================================================
	@Test
	public void testTables1() throws Exception {
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

	//====================================================================================================
	// Test URI_ANCHOR_SET options
	//====================================================================================================
	@Test
	public void testAnchorTextOptions() throws Exception {
		HtmlSerializerBuilder s = HtmlSerializer.create().sq().addKeyValueTableHeaders(true).uriResolution(UriResolution.NONE);
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

		s.detectLinksInStrings(false);
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

			s.detectLinksInStrings(true);
			s.lookForLabelParameters(false);
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

	//====================================================================================================
	// Test @Html.asPlainText annotation on classes and fields
	//====================================================================================================
	@Test
	public void testHtmlAnnotationAsPlainText() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().addKeyValueTableHeaders(true).build();
		Object o = null;
		String r;

		o = new B1();
		r = s.serialize(o);
		assertEquals("<test>", r);

		o = new B2();
		r = s.serialize(o);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><f1></td></tr></table>", r);
	}

	@Html(asPlainText=true)
	public static class B1 {
		public String f1 = "<f1>";
		@Override /* Object */
		public String toString() {
			return "<test>";
		}
	}

	public static class B2 {
		@Html(asPlainText=true)
		public String f1 = "<f1>";
	}

	//====================================================================================================
	// Test @Html.asXml annotation on classes and fields
	//====================================================================================================
	@Test
	public void testHtmlAnnotationAsXml() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().addKeyValueTableHeaders(true).build();
		Object o = null;
		String r;

		o = new C1();
		r = s.serialize(o);
		assertEquals("<object><f1>&lt;f1&gt;</f1></object>", r);

		o = new C2();
		r = s.serialize(o);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td>&lt;f1&gt;</td></tr></table>", r);
	}

	@Html(asXml=true)
	public static class C1 {
		public String f1 = "<f1>";
	}

	public static class C2 {
		@Html(asXml=true)
		public String f1 = "<f1>";
	}

	//====================================================================================================
	// Test @Html.noTableHeaders
	//====================================================================================================
	@Test
	public void testNoTableHeaders() throws Exception {
		HtmlSerializer s = HtmlSerializer.DEFAULT_SQ;
		Object o = null;
		String r;

		Map m = new MyMap();
		m.put("foo", "bar");
		o = new ObjectList().append(m);
		r = s.serialize(o);
		assertEquals("<ul><li><table><tr><td>foo</td><td>bar</td></tr></table></li></ul>", r);
	}

	@Html(noTables=true, noTableHeaders=true)
	public static class MyMap extends LinkedHashMap<String,String> {}

}