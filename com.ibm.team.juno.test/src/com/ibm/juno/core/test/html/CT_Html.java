/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.html;

import static com.ibm.juno.core.html.HtmlSerializerProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.annotation.Filter;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.html.*;
import com.ibm.juno.core.html.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.testbeans.*;

public class CT_Html {

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

		// Tables with some beans with @Bean#filters annotations.
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

	@Filter(A4Filter.class)
	public static class A4 {
		public String f2 = "f2";
	}

	public static class A4Filter extends PojoFilter<A4,A1> {
		@Override /* PojoFilter */
		public A1 filter(A4 o) throws SerializeException {
			return new A1();
		}
	}

	@Filter(A5Filter.class)
	public static class A5 {
		public String f2 = "f2";
	}

	public static class A5Filter extends PojoFilter<A5,ObjectMap> {
		@Override /* PojoFilter */
		public ObjectMap filter(A5 o) {
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

		s.setProperty(HTML_uriAnchorText, TO_STRING);
		r = s.serialize(t);
		assertTrue(r.contains("<a href='foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='/foo/bar'>/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='/foo/bar'>/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='foo/bar'>foo/bar</a>"));

		s.setProperty(SERIALIZER_absolutePathUriBase, "http://myhost");
		s.setProperty(SERIALIZER_relativeUriBase, "/cr");
		s.setProperty(HTML_uriAnchorText, TO_STRING);
		r = s.serialize(t);
		assertTrue(r.contains("<a href='/cr/foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='http://myhost/foo/bar'>/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='http://myhost/foo/bar'>/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>foo/bar</a>"));

		s.setProperty(HTML_uriAnchorText, URI);
		r = s.serialize(t);
		assertTrue(r.contains("<a href='/cr/foo/bar'>/cr/foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>/cr/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://myhost/foo/bar'>http://myhost/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>/cr/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://myhost/foo/bar'>http://myhost/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>http://www.ibm.com/foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>/cr/foo/bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>/cr/foo/bar</a>"));

		s.setProperty(HTML_uriAnchorText, LAST_TOKEN);
		r = s.serialize(t);
		assertTrue(r.contains("<a href='/cr/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='http://myhost/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='http://myhost/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='http://www.ibm.com/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>bar</a>"));
		assertTrue(r.contains("<a href='/cr/foo/bar'>bar</a>"));
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
}