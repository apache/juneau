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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.function.*;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

/**
 * Tests the @XmlConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class XmlConfigAnnotationTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t.getClass().isArray())
				return apply(ArrayUtils.toList(t, Object.class));
			if (t instanceof AA)
				return "AA";
			if (t instanceof AB)
				return "AB";
			if (t instanceof AC)
				return "AC";
			return t.toString();
		}
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

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
		addBeanTypes="$X{true}",
		addNamespaceUrisToRoot="$X{true}",
		disableAutoDetectNamespaces="$X{true}",
		defaultNamespace="$X{foo}",
		enableNamespaces="$X{true}",
		eventAllocator=AA.class,
		namespaces="$X{foo}",
		preserveRootElement="$X{true}",
		reporter=AB.class,
		resolver=AC.class,
		validating="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		XmlSerializerSession x = XmlSerializer.create().apply(al).build().getSession();
		check("true", x.isAddBeanTypes());
		check("true", x.isAddNamespaceUrisToRoot());
		check("false", x.isAutoDetectNamespaces());
		check("foo:null", x.getDefaultNamespace());
		check("true", x.isEnableNamespaces());
		check("[foo:null]", x.getNamespaces());
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		XmlParserSession x = XmlParser.create().apply(al).build().getSession();
		check("AA", x.getEventAllocator());
		check("true", x.isPreserveRootElement());
		check("AB", x.getReporter());
		check("AC", x.getResolver());
		check("true", x.isValidating());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@XmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		XmlSerializerSession x = XmlSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isAddNamespaceUrisToRoot());
		check("true", x.isAutoDetectNamespaces());
		check("juneau:http://www.apache.org/2013/Juneau", x.getDefaultNamespace());
		check("false", x.isEnableNamespaces());
		check("[]", x.getNamespaces());
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		XmlParserSession x = XmlParser.create().apply(al).build().getSession();
		check(null, x.getEventAllocator());
		check("false", x.isPreserveRootElement());
		check(null, x.getReporter());
		check(null, x.getResolver());
		check("false", x.isValidating());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		XmlSerializerSession x = XmlSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isAddNamespaceUrisToRoot());
		check("true", x.isAutoDetectNamespaces());
		check("juneau:http://www.apache.org/2013/Juneau", x.getDefaultNamespace());
		check("false", x.isEnableNamespaces());
		check("[]", x.getNamespaces());
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		XmlParserSession x = XmlParser.create().apply(al).build().getSession();
		check(null, x.getEventAllocator());
		check("false", x.isPreserveRootElement());
		check(null, x.getReporter());
		check(null, x.getResolver());
		check("false", x.isValidating());
	}
}
