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
package org.apache.juneau.xml;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.api.*;

class CommonXml_Test extends TestBase {

	//====================================================================================================
	// Test 18a - @Bean.uri annotation
	//====================================================================================================
	@Test void a01_beanUriAnnotation() throws Exception {
		var p = XmlParser.DEFAULT;
		var s = XmlSerializer.DEFAULT_SQ;

		var t = new A("http://foo", 123, "bar");
		var xml = s.serialize(t);
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
		public A(String url, int id, String name) {
			this.url = url(url);
			this.id = id;
			this.name = name;
		}
	}

	//====================================================================================================
	// Bean.uri annotation, only uri property
	//====================================================================================================
	@Test void a02_beanUriAnnotationOnlyUriProperty() throws Exception {
		var s = XmlSerializer.create().sq().build();

		var t = new B("http://foo");
		var xml = s.serialize(t);
		assertEquals("<object url='http://foo'><url2>http://foo/2</url2></object>", xml);
	}

	public static class B {
		@Xml(format=XmlFormat.ATTR) public URL url;
		public URL url2;
		public B() {}
		public B(String url) {
			this.url = url(url);
			this.url2 = url(url+"/2");
		}
	}
}