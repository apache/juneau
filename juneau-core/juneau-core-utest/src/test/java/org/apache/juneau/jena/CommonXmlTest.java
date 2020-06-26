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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.jena.RdfCommon.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.net.*;

import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class CommonXmlTest {

	private RdfSerializerBuilder getBasicSerializer() {
		return RdfSerializer.create()
			.sq()
			.set(RDF_rdfxml_allowBadUris, true)
			.set(RDF_rdfxml_showDoctypeDeclaration, false)
			.set(RDF_rdfxml_showXmlDeclaration, false);
	}

	private String strip(String s) {
		return s.replaceFirst("<rdf:RDF[^>]+>\\s*", "").replaceAll("</rdf:RDF>$", "").trim().replaceAll("[\\r\\n]", "");
	}

	//====================================================================================================
	// Bean.uri annotation
	//====================================================================================================
	@Test
	public void testBeanUriAnnotation() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfXmlParser.DEFAULT;
		A t1 = A.create(), t2;
		String r;

		r = s.build().serialize(t1);
		assertEquals("<rdf:Description rdf:about='http://foo'><jp:name>bar</jp:name></rdf:Description>", strip(r));
		t2 = p.parse(r, A.class);
		assertObject(t1).sameAs(t2);
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
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfXmlParser.DEFAULT;
		B t1 = B.create(), t2;
		String r;

		r = s.build().serialize(t1);
		assertEquals("<rdf:Description rdf:about='http://foo'><jp:url2 rdf:resource='http://foo/2'/></rdf:Description>", strip(r));
		t2 = p.parse(r, B.class);
		assertObject(t1).sameAs(t2);
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
