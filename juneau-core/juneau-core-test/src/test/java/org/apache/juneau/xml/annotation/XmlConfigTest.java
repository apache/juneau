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
package org.apache.juneau.xml.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.xml.XmlSerializer.*;
import static org.apache.juneau.xml.XmlParser.*;

import java.util.function.*;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;

/**
 * Tests the @XmlConfig annotation.
 */
public class XmlConfigTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			return t.toString();
		}
	};

	static StringResolver sr = new StringResolver() {
		@Override
		public String resolve(String input) {
			if (input.startsWith("$"))
				input = input.substring(1);
			return input;
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class AA extends XmlEventAllocator {
		@Override
		public XMLEventAllocator newInstance() {
			return null;
		}
		@Override
		public XMLEvent allocate(XMLStreamReader reader) throws XMLStreamException {
			return null;
		}
		@Override
		public void allocate(XMLStreamReader reader, XMLEventConsumer consumer) throws XMLStreamException {
		}
	}
	public static class AB extends XmlReporter {
		@Override
		public void report(String message, String errorType, Object relatedInformation, Location location) throws XMLStreamException {
		}
	}
	public static class AC extends XmlResolver {
		@Override
		public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
			return null;
		}
	}

	@XmlConfig(
		addBeanTypes="$true",
		addNamespaceUrisToRoot="$true",
		autoDetectNamespaces="$true",
		defaultNamespace="$foo",
		enableNamespaces="$true",
		eventAllocator=AA.class,
		namespaces="$foo",
		preserveRootElement="$true",
		reporter=AB.class,
		resolver=AC.class,
		validating="$true",
		xsNamespace="$foo"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		XmlSerializer x = XmlSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(XML_addNamespaceUrisToRoot));
		check("true", x.getProperty(XML_autoDetectNamespaces));
		check("foo", x.getProperty(XML_defaultNamespace));
		check("true", x.getProperty(XML_enableNamespaces));
		check(null, x.getProperty(XML_eventAllocator));
		check("[foo:null]", x.getProperty(XML_namespaces));
		check(null, x.getProperty(XML_preserveRootElement));
		check(null, x.getProperty(XML_reporter));
		check(null, x.getProperty(XML_resolver));
		check(null, x.getProperty(XML_validating));
		check("foo:null", x.getProperty(XML_xsNamespace));
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		XmlParser x = XmlParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(XML_addNamespaceUrisToRoot));
		check(null, x.getProperty(XML_autoDetectNamespaces));
		check(null, x.getProperty(XML_defaultNamespace));
		check(null, x.getProperty(XML_enableNamespaces));
		check("AA", x.getProperty(XML_eventAllocator));
		check(null, x.getProperty(XML_namespaces));
		check("true", x.getProperty(XML_preserveRootElement));
		check("AB", x.getProperty(XML_reporter));
		check("AC", x.getProperty(XML_resolver));
		check("true", x.getProperty(XML_validating));
		check(null, x.getProperty(XML_xsNamespace));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@XmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		XmlSerializer x = XmlSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		XmlParser x = XmlParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		XmlSerializer x = XmlSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		XmlParser x = XmlParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}
}
