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
package org.apache.juneau.xml;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;
import static org.apache.juneau.xml.XmlUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;

import java.io.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class XmlContentTest {

	//--------------------------------------------------------------------------------
	// Test beans with @Xml(format=CONTENT)
	//--------------------------------------------------------------------------------
	@Test
	public void testContentFormat() throws Exception {
		A t = A.newInstance(), t2;
		XmlSerializer s1 = XmlSerializer.DEFAULT_SIMPLE_SQ,
			s2 = new XmlSerializer().setProperty(SERIALIZER_quoteChar, '\'').setProperty(SERIALIZER_useIndentation, true).setProperty(XML_enableNamespaces, false);
		XmlParser p = XmlParser.DEFAULT;
		XmlSerializerSession session;
		String r;
		StringWriter sw;

		//----------------------------------------------------------------
		// Null
		//----------------------------------------------------------------
		t.f2 = null;

		sw = new StringWriter();
		session = s1.createSession(sw, new ObjectMap("{"+SERIALIZER_trimNullProperties+":false}"), null);
		s1.serialize(session, t);
		r = sw.toString();
		assertEquals("<A f1='f1'>_x0000_</A>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		sw = new StringWriter();
		session = s2.createSession(sw, new ObjectMap("{"+SERIALIZER_trimNullProperties+":false}"), null);
		s2.serialize(session, t);
		r = sw.toString();
		assertEquals("<A f1='f1'>\n\t_x0000_\n</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Normal text
		//----------------------------------------------------------------
		t.f2 = "foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\tfoobar\n</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Special characters
		//----------------------------------------------------------------
		t.f2 = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,.\n\r\t\b";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>~!@#$%^&amp;*()_+`-={}|[]\\:\";'&lt;&gt;?,.&#x000a;&#x000d;&#x0009;_x0008_</A>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\t~!@#$%^&amp;*()_+`-={}|[]\\:\";'&lt;&gt;?,.&#x000a;&#x000d;&#x0009;_x0008_\n</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Leading spaces
		//----------------------------------------------------------------
		t.f2 = "  foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ foobar</A>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\t_x0020_ foobar\n</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Trailing spaces
		//----------------------------------------------------------------
		t.f2 = "foobar  ";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar _x0020_</A>", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\tfoobar _x0020_\n</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEqualObjects(t, t2);
	}

	@Xml(name="A")
	public static class A {
		@Xml(format=ATTR) public String f1;
		@Xml(format=CONTENT) public String f2;

		public static A newInstance() {
			A t = new A();
			t.f1 = "f1";
			t.f2 = null;
			return t;
		}
	}

	//--------------------------------------------------------------------------------
	// Test beans with @Xml(format=XMLCONTENT)
	//--------------------------------------------------------------------------------
	@Test
	public void testXmlContentFormat() throws Exception {
		B t = B.newInstance(), t2;
		XmlSerializer s1 = XmlSerializer.DEFAULT_SIMPLE_SQ,
			s2 = new XmlSerializer().setProperty(SERIALIZER_quoteChar, '\'').setProperty(SERIALIZER_useIndentation, true).setProperty(XML_enableNamespaces, false);
		XmlParser p = XmlParser.DEFAULT;
		XmlSerializerSession session;
		String r;
		StringWriter sw;

		//----------------------------------------------------------------
		// Null
		//----------------------------------------------------------------
		t.f2 = null;

		sw = new StringWriter();
		session = s1.createSession(sw, new ObjectMap("{"+SERIALIZER_trimNullProperties+":false}"), null);
		s1.serialize(session, t);
		r = sw.toString();
		assertEquals("<A f1='f1'>_x0000_</A>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		sw = new StringWriter();
		session = s2.createSession(sw, new ObjectMap("{"+SERIALIZER_trimNullProperties+":false}"), null);
		s2.serialize(session, t);
		r = sw.toString();
		assertEquals("<A f1='f1'>\n\t_x0000_\n</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Normal text
		//----------------------------------------------------------------
		t.f2 = "foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\tfoobar\n</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Normal XML
		//----------------------------------------------------------------
		t.f2 = "<xxx>foobar<yyy>baz</yyy>foobar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'><xxx>foobar<yyy>baz</yyy>foobar</xxx></A>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\t<xxx>foobar<yyy>baz</yyy>foobar</xxx>\n</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// Normal XML with leading and trailing space
		//----------------------------------------------------------------
		t.f2 = "  <xxx>foobar<yyy>baz</yyy>foobar</xxx>  ";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ <xxx>foobar<yyy>baz</yyy>foobar</xxx> _x0020_</A>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\t_x0020_ <xxx>foobar<yyy>baz</yyy>foobar</xxx> _x0020_\n</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// XML with attributes
		//----------------------------------------------------------------
		t.f2 = "<xxx x=\"x\">foobar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'><xxx x=\"x\">foobar</xxx></A>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\t<xxx x=\"x\">foobar</xxx>\n</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		//----------------------------------------------------------------
		// XML with embedded entities
		//----------------------------------------------------------------
		t.f2 = "<xxx x=\"x\">foo&lt;&gt;bar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'><xxx x=\"x\">foo&lt;&gt;bar</xxx></A>", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>\n\t<xxx x=\"x\">foo&lt;&gt;bar</xxx>\n</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEqualObjects(t, t2);
	}

	@Xml(name="A")
	public static class B {
		@Xml(format=ATTR) public String f1;
		@Xml(format=CONTENT, contentHandler=BContentHandler.class) public String f2;

		public static B newInstance() {
			B t = new B();
			t.f1 = "f1";
			t.f2 = null;
			return t;
		}
	}

	public static class BContentHandler implements XmlContentHandler<B> {

		@Override /* XmlContentHandler */
		public void parse(XMLStreamReader r, B b) throws Exception {
			b.f2 = decode(readXmlContents(r).trim());
		}

		@Override /* XmlContentHandler */
		public void serialize(XmlWriter w, B b) throws Exception {
			w.encodeTextInvalidChars(b.f2);
		}

	}

	//--------------------------------------------------------------------------------
	// Test beans with too many @Xml.format=CONTENT/XMLCONTENT annotations.
	//--------------------------------------------------------------------------------
	@Test
	public void testBadContent() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SIMPLE_SQ;
		try {
			s.serialize(new C1());
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Multiple instances of CONTENT properties defined on class"));
		}
		// Run twice to make sure we throw exceptions after the first call.
		try {
			s.serialize(new C1());
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Multiple instances of CONTENT properties defined on class"));
		}
	}
	public static class C1 {
		@Xml(format=CONTENT) public String f1;
		@Xml(format=CONTENT) public String f2;
	}
}
