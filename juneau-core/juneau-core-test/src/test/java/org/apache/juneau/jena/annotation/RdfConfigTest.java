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
package org.apache.juneau.jena.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.jena.RdfSerializer.*;
import static org.apache.juneau.jena.RdfParser.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.jena.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @RdfConfig annotation.
 */
public class RdfConfigTest {

	private static void check(String expected, Object o) {
		if (o instanceof List) {
			List<?> l = (List<?>)o;
			String actual = l
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
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

	@RdfConfig(
		addBeanTypes="$true",
		addLiteralTypes="$true",
		addRootProperty="$true",
		arp_embedding="$true",
		arp_errorMode="$strict",
		arp_iriRules="$strict",
		autoDetectNamespaces="$true",
		collectionFormat="$SEQ",
		juneauBpNs="$foo",
		juneauNs="$foo",
		language="$N3",
		looseCollections="$true",
		n3_abbrevBaseUri="$true",
		n3_indentProperty="$1",
		n3_minGap="$1",
		n3_objectLists="$true",
		n3_propertyColumn="$1",
		n3_subjectColumn="$1",
		n3_useDoubles="$true",
		n3_usePropertySymbols="$true",
		n3_useTripleQuotedStrings="$true",
		n3_widePropertyLen="$1",
		namespaces="$foo",
		rdfxml_allowBadUris="$true",
		rdfxml_attributeQuoteChar="$'",
		rdfxml_blockRules="$foo",
		rdfxml_longId="$true",
		rdfxml_relativeUris="$absolute",
		rdfxml_showDoctypeDeclaration="$true",
		rdfxml_showXmlDeclaration="$true",
		rdfxml_tab="$1",
		rdfxml_xmlBase="$foo",
		trimWhitespace="$true",
		useXmlNamespaces="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		RdfSerializer x = RdfSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(RDF_addLiteralTypes));
		check("true", x.getProperty(RDF_addRootProperty));
		check("true", x.getProperty(RDF_arp_embedding));
		check("strict", x.getProperty(RDF_arp_errorMode));
		check("strict", x.getProperty(RDF_arp_iriRules));
		check("true", x.getProperty(RDF_autoDetectNamespaces));
		check("SEQ", x.getProperty(RDF_collectionFormat));
		check("foo", x.getProperty(RDF_juneauBpNs));
		check("foo", x.getProperty(RDF_juneauNs));
		check("N3", x.getProperty(RDF_language));
		check("true", x.getProperty(RDF_looseCollections));
		check("true", x.getProperty(RDF_n3_abbrevBaseUri));
		check("1", x.getProperty(RDF_n3_indentProperty));
		check("1", x.getProperty(RDF_n3_minGap));
		check("true", x.getProperty(RDF_n3_objectLists));
		check("1", x.getProperty(RDF_n3_propertyColumn));
		check("1", x.getProperty(RDF_n3_subjectColumn));
		check("true", x.getProperty(RDF_n3_useDoubles));
		check("true", x.getProperty(RDF_n3_usePropertySymbols));
		check("true", x.getProperty(RDF_n3_useTripleQuotedStrings));
		check("1", x.getProperty(RDF_n3_widePropertyLen));
		check("foo", x.getProperty(RDF_namespaces));
		check("true", x.getProperty(RDF_rdfxml_allowBadUris));
		check("'", x.getProperty(RDF_rdfxml_attributeQuoteChar));
		check("foo", x.getProperty(RDF_rdfxml_blockRules));
		check("true", x.getProperty(RDF_rdfxml_longId));
		check("absolute", x.getProperty(RDF_rdfxml_relativeUris));
		check("true", x.getProperty(RDF_rdfxml_showDoctypeDeclaration));
		check("true", x.getProperty(RDF_rdfxml_showXmlDeclaration));
		check("1", x.getProperty(RDF_rdfxml_tab));
		check("foo", x.getProperty(RDF_rdfxml_xmlBase));
		check(null, x.getProperty(RDF_trimWhitespace));
		check("true", x.getProperty(RDF_useXmlNamespaces));
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		RdfParser x = RdfParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(RDF_addLiteralTypes));
		check(null, x.getProperty(RDF_addRootProperty));
		check("true", x.getProperty(RDF_arp_embedding));
		check("strict", x.getProperty(RDF_arp_errorMode));
		check("strict", x.getProperty(RDF_arp_iriRules));
		check(null, x.getProperty(RDF_autoDetectNamespaces));
		check("SEQ", x.getProperty(RDF_collectionFormat));
		check("foo", x.getProperty(RDF_juneauBpNs));
		check("foo", x.getProperty(RDF_juneauNs));
		check("N3", x.getProperty(RDF_language));
		check("true", x.getProperty(RDF_looseCollections));
		check("true", x.getProperty(RDF_n3_abbrevBaseUri));
		check("1", x.getProperty(RDF_n3_indentProperty));
		check("1", x.getProperty(RDF_n3_minGap));
		check("true", x.getProperty(RDF_n3_objectLists));
		check("1", x.getProperty(RDF_n3_propertyColumn));
		check("1", x.getProperty(RDF_n3_subjectColumn));
		check("true", x.getProperty(RDF_n3_useDoubles));
		check("true", x.getProperty(RDF_n3_usePropertySymbols));
		check("true", x.getProperty(RDF_n3_useTripleQuotedStrings));
		check("1", x.getProperty(RDF_n3_widePropertyLen));
		check(null, x.getProperty(RDF_namespaces));
		check("true", x.getProperty(RDF_rdfxml_allowBadUris));
		check("'", x.getProperty(RDF_rdfxml_attributeQuoteChar));
		check("foo", x.getProperty(RDF_rdfxml_blockRules));
		check("true", x.getProperty(RDF_rdfxml_longId));
		check("absolute", x.getProperty(RDF_rdfxml_relativeUris));
		check("true", x.getProperty(RDF_rdfxml_showDoctypeDeclaration));
		check("true", x.getProperty(RDF_rdfxml_showXmlDeclaration));
		check("1", x.getProperty(RDF_rdfxml_tab));
		check("foo", x.getProperty(RDF_rdfxml_xmlBase));
		check("true", x.getProperty(RDF_trimWhitespace));
		check("true", x.getProperty(RDF_useXmlNamespaces));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		RdfSerializer x = RdfSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(RDF_addLiteralTypes));
		check(null, x.getProperty(RDF_addRootProperty));
		check(null, x.getProperty(RDF_arp_embedding));
		check(null, x.getProperty(RDF_arp_errorMode));
		check(null, x.getProperty(RDF_arp_iriRules));
		check(null, x.getProperty(RDF_autoDetectNamespaces));
		check(null, x.getProperty(RDF_collectionFormat));
		check(null, x.getProperty(RDF_juneauBpNs));
		check(null, x.getProperty(RDF_juneauNs));
		check(null, x.getProperty(RDF_language));
		check(null, x.getProperty(RDF_looseCollections));
		check(null, x.getProperty(RDF_n3_abbrevBaseUri));
		check(null, x.getProperty(RDF_n3_indentProperty));
		check(null, x.getProperty(RDF_n3_minGap));
		check(null, x.getProperty(RDF_n3_objectLists));
		check(null, x.getProperty(RDF_n3_propertyColumn));
		check(null, x.getProperty(RDF_n3_subjectColumn));
		check(null, x.getProperty(RDF_n3_useDoubles));
		check(null, x.getProperty(RDF_n3_usePropertySymbols));
		check(null, x.getProperty(RDF_n3_useTripleQuotedStrings));
		check(null, x.getProperty(RDF_n3_widePropertyLen));
		check(null, x.getProperty(RDF_namespaces));
		check(null, x.getProperty(RDF_rdfxml_allowBadUris));
		check(null, x.getProperty(RDF_rdfxml_attributeQuoteChar));
		check(null, x.getProperty(RDF_rdfxml_blockRules));
		check(null, x.getProperty(RDF_rdfxml_longId));
		check(null, x.getProperty(RDF_rdfxml_relativeUris));
		check(null, x.getProperty(RDF_rdfxml_showDoctypeDeclaration));
		check(null, x.getProperty(RDF_rdfxml_showXmlDeclaration));
		check(null, x.getProperty(RDF_rdfxml_tab));
		check(null, x.getProperty(RDF_rdfxml_xmlBase));
		check(null, x.getProperty(RDF_trimWhitespace));
		check(null, x.getProperty(RDF_useXmlNamespaces));
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		RdfParser x = RdfParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(RDF_addLiteralTypes));
		check(null, x.getProperty(RDF_addRootProperty));
		check(null, x.getProperty(RDF_arp_embedding));
		check(null, x.getProperty(RDF_arp_errorMode));
		check(null, x.getProperty(RDF_arp_iriRules));
		check(null, x.getProperty(RDF_autoDetectNamespaces));
		check(null, x.getProperty(RDF_collectionFormat));
		check(null, x.getProperty(RDF_juneauBpNs));
		check(null, x.getProperty(RDF_juneauNs));
		check(null, x.getProperty(RDF_language));
		check(null, x.getProperty(RDF_looseCollections));
		check(null, x.getProperty(RDF_n3_abbrevBaseUri));
		check(null, x.getProperty(RDF_n3_indentProperty));
		check(null, x.getProperty(RDF_n3_minGap));
		check(null, x.getProperty(RDF_n3_objectLists));
		check(null, x.getProperty(RDF_n3_propertyColumn));
		check(null, x.getProperty(RDF_n3_subjectColumn));
		check(null, x.getProperty(RDF_n3_useDoubles));
		check(null, x.getProperty(RDF_n3_usePropertySymbols));
		check(null, x.getProperty(RDF_n3_useTripleQuotedStrings));
		check(null, x.getProperty(RDF_n3_widePropertyLen));
		check(null, x.getProperty(RDF_namespaces));
		check(null, x.getProperty(RDF_rdfxml_allowBadUris));
		check(null, x.getProperty(RDF_rdfxml_attributeQuoteChar));
		check(null, x.getProperty(RDF_rdfxml_blockRules));
		check(null, x.getProperty(RDF_rdfxml_longId));
		check(null, x.getProperty(RDF_rdfxml_relativeUris));
		check(null, x.getProperty(RDF_rdfxml_showDoctypeDeclaration));
		check(null, x.getProperty(RDF_rdfxml_showXmlDeclaration));
		check(null, x.getProperty(RDF_rdfxml_tab));
		check(null, x.getProperty(RDF_rdfxml_xmlBase));
		check(null, x.getProperty(RDF_trimWhitespace));
		check(null, x.getProperty(RDF_useXmlNamespaces));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		RdfSerializer x = RdfSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		RdfParser x = RdfParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}
}
