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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.api.*;

class XmlContent_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test beans with @Xml(format=CONTENT)
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_contentFormat() throws Exception {
		var t = A.newInstance();
		var s1 = XmlSerializer.DEFAULT_SQ.copy().keepNullProperties().build();
		var s2 = XmlSerializer.create().sq().ws().keepNullProperties().build();
		var p = XmlParser.DEFAULT;

		//-------------------------------------------------------------------------------------------------------------
		// Null
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = null;

		var sw = new StringWriter();
		s1.serialize(t, sw);
		var r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>", r);
		var t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		sw = new StringWriter();
		s2.serialize(t, sw);
		r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>\n", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Normal text
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Special characters
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,.\n\r\t\b";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>~!@#$%^&amp;*()_+`-={}|[]\\:\";'&lt;&gt;?,.&#x000a;&#x000d;&#x0009;_x0008_</A>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>~!@#$%^&amp;*()_+`-={}|[]\\:\";'&lt;&gt;?,.&#x000a;&#x000d;&#x0009;_x0008_</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Leading spaces
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "  foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ foobar</A>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ foobar</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Trailing spaces
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "foobar  ";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar _x0020_</A>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>foobar _x0020_</A>\n", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t));
	}

	@Bean(typeName="A")
	public static class A {
		@Xml(format=ATTR) public String f1;
		@Xml(format=TEXT) public String f2;

		public static A newInstance() {
			var t = new A();
			t.f1 = "f1";
			t.f2 = null;
			return t;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test beans with @Xml(format=MIXED)
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a02_xmlMixed() throws Exception {
		var t = B.newInstance();
		var s1 = XmlSerializer.DEFAULT_SQ.copy().keepNullProperties().build();
		var s2 = XmlSerializer.create().sq().ws().keepNullProperties().build();
		var p = XmlParser.DEFAULT;
		var sw = new StringWriter();

		//-------------------------------------------------------------------------------------------------------------
		// Null
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = null;

		s1.serialize(t, sw);
		var r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>", r);
		var t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		sw = new StringWriter();
		s2.serialize(t, sw);
		r = sw.toString();
		assertEquals("<A f1='f1' nil='true'></A>\n", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Normal text
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "foobar";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>foobar</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Normal XML
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "<xxx>foobar<yyy>baz</yyy>foobar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt;</A>", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt;</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// Normal XML with leading and trailing space
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "  <xxx>foobar<yyy>baz</yyy>foobar</xxx>  ";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ &lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt; _x0020_</A>", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>_x0020_ &lt;xxx&gt;foobar&lt;yyy&gt;baz&lt;/yyy&gt;foobar&lt;/xxx&gt; _x0020_</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// XML with attributes
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "<xxx x=\"x\">foobar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foobar&lt;/xxx&gt;</A>", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foobar&lt;/xxx&gt;</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		//-------------------------------------------------------------------------------------------------------------
		// XML with embedded entities
		//-------------------------------------------------------------------------------------------------------------
		t.f2 = "<xxx x=\"x\">foo&lt;&gt;bar</xxx>";

		r = s1.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foo&amp;lt;&amp;gt;bar&lt;/xxx&gt;</A>", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));

		r = s2.serialize(t);
		assertEquals("<A f1='f1'>&lt;xxx x=\"x\"&gt;foo&amp;lt;&amp;gt;bar&lt;/xxx&gt;</A>\n", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t));
	}

	@Bean(typeName="A")
	public static class B {
		@Xml(format=ATTR) public String f1;
		@Xml(format=TEXT) public String f2;

		public static B newInstance() {
			var t = new B();
			t.f1 = "f1";
			t.f2 = null;
			return t;
		}
	}
}