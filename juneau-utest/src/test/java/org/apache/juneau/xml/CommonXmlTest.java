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

import static org.apache.juneau.utest.utils.Utils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.net.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class CommonXmlTest {

	//====================================================================================================
	// Test 18a - @Bean.uri annotation
	//====================================================================================================
	@Test
	public void testBeanUriAnnotation() throws Exception {
		XmlParser p = XmlParser.DEFAULT;
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;

		A t = new A("http://foo", 123, "bar");
		String xml = s.serialize(t);
		assertEquals("<object url='http://foo' id='123'><name>bar</name></object>", xml);

		t = p.parse(xml, A.class);
		assertEquals("http://foo", t.url.toString());
		assertEquals(123, t.id);
		assertEquals("bar", t.name);

		validateXml(t, s);
	}

	@Bean(p="url,id,name")
	public static class A {
		@Xml(format=XmlFormat.ATTR) public URL url;
		@Xml(format=ATTR) public int id;
		public String name;
		public A() {}
		public A(String url, int id, String name) throws Exception {
			this.url = new URL(url);
			this.id = id;
			this.name = name;
		}
	}

	//====================================================================================================
	// Bean.uri annotation, only uri property
	//====================================================================================================
	@Test
	public void testBeanUriAnnotationOnlyUriProperty() throws Exception {
		XmlSerializer s = XmlSerializer.create().sq().build();

		B t = new B("http://foo");
		String xml = s.serialize(t);
		assertEquals("<object url='http://foo'><url2>http://foo/2</url2></object>", xml);
	}

	public static class B {
		@Xml(format=XmlFormat.ATTR) public URL url;
		public URL url2;
		public B() {}
		public B(String url) throws Exception {
			this.url = new URL(url);
			this.url2 = new URL(url+"/2");
		}
	}
}