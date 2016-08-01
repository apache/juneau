/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.xml;

import static com.ibm.juno.core.test.TestUtils.*;
import static com.ibm.juno.core.xml.XmlSerializerProperties.*;
import static com.ibm.juno.core.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;

import java.net.*;

import org.junit.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.core.xml.annotation.*;

public class CT_CommonXml {
	
	//====================================================================================================
	// Test 18a - @Bean.uri annotation
	//====================================================================================================
	@Test
	public void testBeanUriAnnotation() throws Exception {
		XmlParser p = XmlParser.DEFAULT;
		XmlSerializer s = XmlSerializer.DEFAULT_SIMPLE_SQ;
		
		A t = new A("http://foo", 123, "bar");
		String xml = s.serialize(t);
		assertEquals("<object url='http://foo' id='123'><name>bar</name></object>", xml);

		t = p.parse(xml, A.class);
		assertEquals("http://foo", t.url.toString());
		assertEquals(123, t.id);
		assertEquals("bar", t.name);
	
		validateXml(t, s);
	}

	public static class A {
		@BeanProperty(beanUri=true) public URL url;
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
		XmlSerializer s = new XmlSerializer.Sq().setProperty(XML_addNamespaceUrisToRoot, false);
		
		B t = new B("http://foo");
		String xml = s.serialize(t);
		assertEquals("<object url='http://foo'><url2>http://foo/2</url2></object>", xml);
	}
	
	public static class B {
		@BeanProperty(beanUri=true) public URL url;
		public URL url2;
		public B() {}
		public B(String url) throws Exception {
			this.url = new URL(url);
			this.url2 = new URL(url+"/2");
		}
	}
}
