/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.jena;

import static com.ibm.juno.core.jena.RdfProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import java.net.*;

import org.junit.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.jena.*;

public class CT_CommonXml {
	
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
		@BeanProperty(beanUri=true) public URL url;
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
		@BeanProperty(beanUri=true) public URL url;
		public URL url2;
		
		public static B create() throws Exception {
			B t = new B();
			t.url = new URL("http://foo");
			t.url2 = new URL("http://foo/2");
			return t;
		}
	}
}
