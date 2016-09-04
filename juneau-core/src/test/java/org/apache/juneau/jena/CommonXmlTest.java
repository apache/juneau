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
package org.apache.juneau.jena;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.net.*;

import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class CommonXmlTest {

	private RdfSerializer getBasicSerializer() {
		return new RdfSerializer()
			.setProperty(SERIALIZER_quoteChar, '\'')
			.setProperty(SERIALIZER_useIndentation, false)
			.setProperty(RDF_rdfxml_allowBadUris, true)
			.setProperty(RDF_rdfxml_showDoctypeDeclaration, false)
			.setProperty(RDF_rdfxml_showXmlDeclaration, false);
	}

	private String strip(String s) {
		return s.replaceFirst("<rdf:RDF[^>]+>\\s*", "").replaceAll("</rdf:RDF>$", "").trim().replaceAll("[\\r\\n]", "");
	}

	//====================================================================================================
	// Bean.uri annotation
	//====================================================================================================
	@Test
	public void testBeanUriAnnotation() throws Exception {
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		A t1 = A.create(), t2;
		String r;

		r = s.serialize(t1);
		assertEquals("<rdf:Description rdf:about='http://foo'><jp:name>bar</jp:name></rdf:Description>", strip(r));
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);
	}

	public static class A {
		@Rdf(beanUri=true) @Xml(format=XmlFormat.ATTR) public URL url;
		public String name;

		public static A create() throws Exception {
			A t = new A();
			t.url = new URL("http://foo");
			t.name = "bar";
			return t;
		}
	}

	//====================================================================================================
	// Bean.uri annotation, only uri property
	//====================================================================================================
	@Test
	public void testBeanUriAnnotationOnlyUriProperty() throws Exception {
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		B t1 = B.create(), t2;
		String r;

		r = s.serialize(t1);
		assertEquals("<rdf:Description rdf:about='http://foo'><jp:url2 rdf:resource='http://foo/2'/></rdf:Description>", strip(r));
		t2 = p.parse(r, B.class);
		assertEqualObjects(t1, t2);
	}

	public static class B {
		@Rdf(beanUri=true) @Xml(format=XmlFormat.ATTR) public URL url;
		public URL url2;

		public static B create() throws Exception {
			B t = new B();
			t.url = new URL("http://foo");
			t.url2 = new URL("http://foo/2");
			return t;
		}
	}
}
