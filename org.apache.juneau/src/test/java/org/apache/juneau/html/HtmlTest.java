/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.html;

import static org.apache.juneau.html.HtmlSerializerContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.annotation.Transform;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testbeans.*;
import org.apache.juneau.transform.*;
import org.junit.*;

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
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new A1(), new A2()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new A1(), new ObjectMap("{f1:'f1'}")};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new ObjectMap("{f1:'f1'}"), new A1()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		// This should be serialized as a list since the objects have different properties.
		t = new Object[] {new A1(), new ObjectMap("{f2:'f2'}")};
		html = s.serialize(t);
		assertEquals("<ul><li><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><string>f1</string></td></tr></table></li><li><table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f2</string></td><td><string>f2</string></td></tr></table></li></ul>", html);

		// Tables with some beans with @Bean#properties annotations.
		t = new Object[] {new A1(), new A3()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new A3(), new A1()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		// Tables with some beans with @Bean#transforms annotations.
		t = new Object[] {new A4(), new A1()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new A1(), new A4()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new A5(), new A1()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);

		t = new Object[] {new A1(), new A5()};
		html = s.serialize(t);
		assertEquals("<table type='array'><tr><th>f1</th></tr><tr><td><string>f1</string></td></tr><tr><td><string>f1</string></td></tr></table>", html);
	}

	public static class A1 {
		public String f1 = "f1";
	}

	public static class A2 {
		public String f1 = "f1";
	}

	@Bean(properties="f1")
	public static class A3 {
		public String f1 = "f1";
		public String f2 = "f2";
	}

	@Transform(A4Transform.class)
	public static class A4 {
		public String f2 = "f2";
	}

	public static class A4Transform extends PojoTransform<A4,A1> {
		@Override /* PojoTransform */
		public A1 transform(A4 o) throws SerializeException {
			return new A1();
		}
	}

	@Transform(A5Transform.class)
	public static class A5 {
		public String f2 = "f2";
	}

	public static class A5Transform extends PojoTransform<A5,ObjectMap> {
		@Override /* PojoTransform */
		public ObjectMap transform(A5 o) {
			return new ObjectMap().append("f1", "f1");
		}
	}

	//====================================================================================================
	// Test URI_ANCHOR_SET options
	//====================================================================================================
	@Test
	public void testAnchorTextOptions() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();
		TestURI t = new TestURI();
		String r;
		String expected = null;

		s.setProperty(HTML_uriAnchorText, TO_STRING);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
			+"\n[f4]=<a href='f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
			+"\n[f8]=<a href='f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>http://www.ibm.com/fa/xa#MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_absolutePathUriBase, "http://myhost");
		s.setProperty(SERIALIZER_relativeUriBase, "/cr");
		s.setProperty(HTML_uriAnchorText, TO_STRING);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='http://myhost/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='http://myhost/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>http://www.ibm.com/fa/xa#MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.setProperty(HTML_uriAnchorText, URI);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>/cr/f0/x0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>/cr/f1/x1</a>"
			+"\n[f2]=<a href='http://myhost/f2/x2'>http://myhost/f2/x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>/cr/f4/x4</a>"
			+"\n[f5]=<a href='http://myhost/f5/x5'>http://myhost/f5/x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>/cr/f8/x8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>/cr/f9/x9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>http://www.ibm.com/fa/xa#MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.setProperty(HTML_uriAnchorText, LAST_TOKEN);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>x0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>x1</a>"
			+"\n[f2]=<a href='http://myhost/f2/x2'>x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>x3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>x4</a>"
			+"\n[f5]=<a href='http://myhost/f5/x5'>x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>x7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>x8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>x9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>xa</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>xd</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>xe</a>";
		assertEquals(expected, r);

		s.setProperty(HTML_uriAnchorText, URI_ANCHOR);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='http://myhost/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='http://myhost/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL</a>";
		assertEquals(expected, r);

		s.setProperty(HTML_labelParameter, "label2");
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='http://myhost/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='http://myhost/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9/x9</a>"
			+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>MY_LABEL</a>"
			+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar</a>"
			+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL</a>"
			+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>MY_LABEL</a>"
			+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>MY_LABEL</a>";
		assertEquals(expected, r);

		s.setProperty(HTML_detectLinksInStrings, false);
		r = strip(s.serialize(t));
		expected = ""
			+"\n[f0]=<a href='/cr/f0/x0'>f0/x0</a>"
			+"\n[f1]=<a href='/cr/f1/x1'>f1/x1</a>"
			+"\n[f2]=<a href='http://myhost/f2/x2'>/f2/x2</a>"
			+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
			+"\n[f4]=<a href='/cr/f4/x4'>f4/x4</a>"
			+"\n[f5]=<a href='http://myhost/f5/x5'>/f5/x5</a>"
			+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
			+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
			+"\n[f8]=<a href='/cr/f8/x8'>f8/x8</a>"
			+"\n[f9]=<a href='/cr/f9/x9'>f9/x9</a>"
			+"\n[fa]=<string>http://www.ibm.com/fa/xa#MY_LABEL</string>"
			+"\n[fb]=<string>http://www.ibm.com/fb/xb?label=MY_LABEL&amp;foo=bar</string>"
			+"\n[fc]=<string>http://www.ibm.com/fc/xc?foo=bar&amp;label=MY_LABEL</string>"
			+"\n[fd]=<string>http://www.ibm.com/fd/xd?label2=MY_LABEL&amp;foo=bar</string>"
			+"\n[fe]=<string>http://www.ibm.com/fe/xe?foo=bar&amp;label2=MY_LABEL</string>";
			assertEquals(expected, r);

			s.setProperty(HTML_detectLinksInStrings, true);
			s.setProperty(HTML_lookForLabelParameters, false);
			r = strip(s.serialize(t));
			expected = ""
				+"\n[f0]=<a href='/cr/f0/x0'>f0/x0</a>"
				+"\n[f1]=<a href='/cr/f1/x1'>f1/x1</a>"
				+"\n[f2]=<a href='http://myhost/f2/x2'>/f2/x2</a>"
				+"\n[f3]=<a href='http://www.ibm.com/f3/x3'>http://www.ibm.com/f3/x3</a>"
				+"\n[f4]=<a href='/cr/f4/x4'>f4/x4</a>"
				+"\n[f5]=<a href='http://myhost/f5/x5'>/f5/x5</a>"
				+"\n[f6]=<a href='http://www.ibm.com/f6/x6'>http://www.ibm.com/f6/x6</a>"
				+"\n[f7]=<a href='http://www.ibm.com/f7/x7'>http://www.ibm.com/f7/x7</a>"
				+"\n[f8]=<a href='/cr/f8/x8'>f8/x8</a>"
				+"\n[f9]=<a href='/cr/f9/x9'>f9/x9</a>"
				+"\n[fa]=<a href='http://www.ibm.com/fa/xa#MY_LABEL'>MY_LABEL</a>"
				+"\n[fb]=<a href='http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar'>http://www.ibm.com/fb/xb?label=MY_LABEL&foo=bar</a>"
				+"\n[fc]=<a href='http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL'>http://www.ibm.com/fc/xc?foo=bar&label=MY_LABEL</a>"
				+"\n[fd]=<a href='http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar'>http://www.ibm.com/fd/xd?label2=MY_LABEL&foo=bar</a>"
				+"\n[fe]=<a href='http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL'>http://www.ibm.com/fe/xe?foo=bar&label2=MY_LABEL</a>";
			assertEquals(expected, r);
	}

	private String strip(String html) {
		return html
			.replace("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr>", "")
			.replace("</table>", "")
			.replace("<tr><td><string>", "\n[")
			.replace("</string></td><td>", "]=")
			.replace("</td></tr>", "");
	}

	//====================================================================================================
	// Test @Html.asPlainText annotation on classes and fields
	//====================================================================================================
	@Test
	public void testHtmlAnnotationAsPlainText() throws Exception {
		HtmlSerializer s = new HtmlSerializer.Sq();
		Object o = null;
		String r;

		o = new B1();
		r = s.serialize(o);
		assertEquals("<test>", r);

		o = new B2();
		r = s.serialize(o);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><f1></td></tr></table>", r);
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
		HtmlSerializer s = new HtmlSerializer.Sq();
		Object o = null;
		String r;

		o = new C1();
		r = s.serialize(o);
		assertEquals("<object><f1>&lt;f1&gt;</f1></object>", r);

		o = new C2();
		r = s.serialize(o);
		assertEquals("<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>f1</string></td><td><string>&lt;f1&gt;</string></td></tr></table>", r);
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
		HtmlSerializer s = new HtmlSerializer.Sq();
		Object o = null;
		String r;

		Map m = new MyMap();
		m.put("foo", "bar");
		o = new ObjectList().append(m);
		r = s.serialize(o);
		assertEquals("<ul><li><table type='object'><tr><td><string>foo</string></td><td><string>bar</string></td></tr></table></li></ul>", r);
	}

	@Html(noTables=true, noTableHeaders=true)
	public static class MyMap extends LinkedHashMap<String,String> {}

}