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
package org.apache.juneau.xml;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class XmlContentTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Test beans with @Xml(format=CONTENT)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testContentFormat() throws Exception {
		A t = A.newInstance(), t2;
		XmlSerializer s1 = XmlSerializer.DEFAULT_SQ.builder().keepNullProperties().build(),
			s2 = XmlSerializer.create().sq().ws().keepNullProperties().build();
		XmlParser p = XmlParser.DEFAULT;
		WriterSerializerSession session;
		String r;
		StringWriter sw;

		//-------------------------------------------------------------------------------------------------------------
		// Null
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = null;

		sw = new StringWriter();
		session = s1.createSession(SerializerSessionArgs.create());
		session.serialize(t, sw);
		r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		sw = new StringWriter();
		session = s2.createSession(SerializerSessionArgs.create());
		session.serialize(t, sw);
		r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>\n", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Normal text
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>\n", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Special characters
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,.\n\r\t\b";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>~!@#$%^&amp;*()_+`-={}|[]\\:\";'&lt;&gt;?,.&#x000a;&#x000d;&#x0009;_x0008_</A>", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>~!@#$%^&amp;*()_+`-={}|[]\\:\";'&lt;&gt;?,.&#x000a;&#x000d;&#x0009;_x0008_</A>\n", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Leading spaces
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "  foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ foobar</A>", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ foobar</A>\n", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Trailing spaces
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "foobar  ";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar _x0020_</A>", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>foobar _x0020_</A>\n", r);
		t2 = p.parse(r, A.class);
		assertObject(t).sameAs(t2);
	}

	@Bean(typeName="A")
	public static class A {
		@Xml(format=ATTR) public String f1;
		@Xml(format=TEXT) public String f2;

		public static A newInstance() {
			A t = new A();
			t.f1 = "f1";
			t.f2 = null;
			return t;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test beans with @Xml(format=MIXED)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testXmlMixed() throws Exception {
		B t = B.newInstance(), t2;
		XmlSerializer s1 = XmlSerializer.DEFAULT_SQ.builder().keepNullProperties().build(),
			s2 = XmlSerializer.create().sq().ws().keepNullProperties().build();
		XmlParser p = XmlParser.DEFAULT;
		WriterSerializerSession session;
		String r;
		StringWriter sw;

		//-------------------------------------------------------------------------------------------------------------
		// Null
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = null;

		sw = new StringWriter();
		session = s1.createSession(SerializerSessionArgs.create());
		session.serialize(t, sw);
		r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		sw = new StringWriter();
		session = s2.createSession(SerializerSessionArgs.create());
		session.serialize(t, sw);
		r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>\n", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Normal text
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>\n", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Normal XML
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "<xxx>foobar<yyy>baz</yyy>foobar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt;</A>", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt;</A>\n", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// Normal XML with leading and trailing space
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "  <xxx>foobar<yyy>baz</yyy>foobar</xxx>  ";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ &lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt; _x0020_</A>", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ &lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt; _x0020_</A>\n", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// XML with attributes
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "<xxx x=\"x\">foobar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foobar&lt;/xxx&gt;</A>", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foobar&lt;/xxx&gt;</A>\n", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		//-------------------------------------------------------------------------------------------------------------
		// XML with embedded entities
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "<xxx x=\"x\">foo&lt;&gt;bar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foo&amp;lt;&amp;gt;bar&lt;/xxx&gt;</A>", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foo&amp;lt;&amp;gt;bar&lt;/xxx&gt;</A>\n", r);
		t2 = p.parse(r, B.class);
		assertObject(t).sameAs(t2);
	}

	@Bean(typeName="A")
	public static class B {
		@Xml(format=ATTR) public String f1;
		@Xml(format=TEXT) public String f2;

		public static B newInstance() {
			B t = new B();
			t.f1 = "f1";
			t.f2 = null;
			return t;
		}
	}
}
