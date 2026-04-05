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
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.XVar;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

class RdfConfigAnnotation_Test extends TestBase {

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// language
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(language = "N-TRIPLE")
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void a01_language_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertEquals("N-TRIPLE", x.getLanguage());
	}

	@RdfConfig(language = "TURTLE")
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void a02_language_parser() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotations()));
		var x = RdfParser.create().apply(al).build().getSession();
		assertEquals("TURTLE", x.getLanguage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// addLiteralTypes
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(addLiteralTypes = "true")
	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void a03_addLiteralTypes() {
		var al = AnnotationWorkList.of(sr, rstream(c.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertTrue(x.isAddLiteralTypes());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// addRootProperty
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(addRootProperty = "true")
	static class D {}
	static ClassInfo d = ClassInfo.of(D.class);

	@Test void a04_addRootProperty() {
		var al = AnnotationWorkList.of(sr, rstream(d.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertTrue(x.isAddRootProp());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// looseCollections
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(looseCollections = "true")
	static class E {}
	static ClassInfo e = ClassInfo.of(E.class);

	@Test void a05_looseCollections_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(e.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertTrue(x.isLooseCollections());
	}

	@Test void a06_looseCollections_parser() {
		var al = AnnotationWorkList.of(sr, rstream(e.getAnnotations()));
		// Parser applies looseCollections; verify build does not throw
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// collectionFormat
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(collectionFormat = "BAG")
	static class F {}
	static ClassInfo f = ClassInfo.of(F.class);

	@Test void a07_collectionFormat_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(f.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertEquals(RdfCollectionFormat.BAG, x.getCollectionFormat());
	}

	@RdfConfig(collectionFormat = "LIST")
	static class G {}
	static ClassInfo g = ClassInfo.of(G.class);

	@Test void a08_collectionFormat_parser() {
		var al = AnnotationWorkList.of(sr, rstream(g.getAnnotations()));
		// Parser applies collectionFormat; verify build does not throw
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Empty annotation (defaults)
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig()
	static class H {}
	static ClassInfo h = ClassInfo.of(H.class);

	@Test void a09_noValues_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(h.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertEquals("RDF/XML-ABBREV", x.getLanguage());
	}

	@Test void a10_noValues_parser() {
		var al = AnnotationWorkList.of(sr, rstream(h.getAnnotations()));
		var x = RdfParser.create().apply(al).build().getSession();
		assertEquals("RDF/XML-ABBREV", x.getLanguage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation (defaults)
	//-----------------------------------------------------------------------------------------------------------------

	static class I {}
	static ClassInfo i = ClassInfo.of(I.class);

	@Test void a11_noAnnotation_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(i.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertEquals("RDF/XML-ABBREV", x.getLanguage());
		assertFalse(x.isAddLiteralTypes());
		assertFalse(x.isAddRootProp());
		assertFalse(x.isLooseCollections());
	}

	@Test void a12_noAnnotation_parser() {
		var al = AnnotationWorkList.of(sr, rstream(i.getAnnotations()));
		var x = RdfParser.create().apply(al).build().getSession();
		assertEquals("RDF/XML-ABBREV", x.getLanguage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// disableAutoDetectNamespaces
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(disableAutoDetectNamespaces = "true")
	static class J {}
	static ClassInfo j = ClassInfo.of(J.class);

	@Test void a13_disableAutoDetectNamespaces() {
		var al = AnnotationWorkList.of(sr, rstream(j.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertFalse(x.isAutoDetectNamespaces());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// addBeanTypes
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(addBeanTypes = "true")
	static class K {}
	static ClassInfo k = ClassInfo.of(K.class);

	@Test void a14_addBeanTypes() {
		var al = AnnotationWorkList.of(sr, rstream(k.getAnnotations()));
		// Verify build doesn't throw; addBeanTypesRdf is set
		assertDoesNotThrow(() -> RdfSerializer.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// juneauNs, juneauBpNs, disableUseXmlNamespaces for serializer
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(juneauNs = "myj:http://myjuneau/", juneauBpNs = "mybp:http://mybp/", disableUseXmlNamespaces = "true")
	static class L {}
	static ClassInfo l = ClassInfo.of(L.class);

	@Test void a15_juneauNs_juneauBpNs_disableUseXmlNamespaces_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(l.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertEquals("myj", x.getJuneauNs().getName());
		assertEquals("mybp", x.getJuneauBpNs().getName());
		assertFalse(x.isUseXmlNamespaces());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// juneauNs, juneauBpNs, trimWhitespace for parser
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(juneauNs = "myj:http://myjuneau/", juneauBpNs = "mybp:http://mybp/", trimWhitespace = "true")
	static class M {}
	static ClassInfo m = ClassInfo.of(M.class);

	@Test void a16_juneauNs_juneauBpNs_trimWhitespace_parser() {
		var al = AnnotationWorkList.of(sr, rstream(m.getAnnotations()));
		var x = RdfParser.create().apply(al).build().getSession();
		assertEquals("myj", x.getJuneauNs().getName());
		assertEquals("mybp", x.getJuneauBpNs().getName());
		assertTrue(x.isTrimWhitespace());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// namespaces for serializer
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(namespaces = "ex:http://example.org/")
	static class N {}
	static ClassInfo n2 = ClassInfo.of(N.class);

	@Test void a17_namespaces_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(n2.getAnnotations()));
		var x = RdfSerializer.create().apply(al).build().getSession();
		assertTrue(x.getNamespaces().length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// rdfxml settings (serializer)
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(
		rdfxml_iriRules = "strict",
		rdfxml_errorMode = "STRICT",
		rdfxml_embedding = "true",
		rdfxml_longId = "true",
		rdfxml_allowBadUris = "true",
		rdfxml_relativeUris = "same-document",
		rdfxml_showXmlDeclaration = "true",
		rdfxml_disableShowDoctypeDeclaration = "true",
		rdfxml_tab = "4",
		rdfxml_attributeQuoteChar = "'",
		rdfxml_blockRules = "idAttr"
	)
	static class O {}
	static ClassInfo o = ClassInfo.of(O.class);

	@Test void a18_rdfxmlSettings_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(o.getAnnotations()));
		assertDoesNotThrow(() -> RdfSerializer.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// rdfxml settings (parser)
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(
		rdfxml_iriRules = "strict",
		rdfxml_errorMode = "STRICT",
		rdfxml_embedding = "true",
		rdfxml_longId = "true",
		rdfxml_allowBadUris = "true",
		rdfxml_relativeUris = "same-document",
		rdfxml_showXmlDeclaration = "true",
		rdfxml_disableShowDoctypeDeclaration = "true",
		rdfxml_tab = "4",
		rdfxml_attributeQuoteChar = "'",
		rdfxml_blockRules = "idAttr"
	)
	static class P {}
	static ClassInfo p = ClassInfo.of(P.class);

	@Test void a19_rdfxmlSettings_parser() {
		var al = AnnotationWorkList.of(sr, rstream(p.getAnnotations()));
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// n3 settings (serializer)
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(
		n3_minGap = "2",
		n3_disableObjectLists = "true",
		n3_subjectColumn = "10",
		n3_propertyColumn = "10",
		n3_indentProperty = "5",
		n3_widePropertyLen = "20",
		n3_disableAbbrevBaseUri = "true",
		n3_disableUsePropertySymbols = "true",
		n3_disableUseTripleQuotedStrings = "true",
		n3_disableUseDoubles = "true"
	)
	static class Q {}
	static ClassInfo q = ClassInfo.of(Q.class);

	@Test void a20_n3Settings_serializer() {
		var al = AnnotationWorkList.of(sr, rstream(q.getAnnotations()));
		assertDoesNotThrow(() -> RdfSerializer.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// n3 settings (parser)
	//-----------------------------------------------------------------------------------------------------------------

	@RdfConfig(
		n3_minGap = "2",
		n3_disableObjectLists = "true",
		n3_subjectColumn = "10",
		n3_propertyColumn = "10",
		n3_indentProperty = "5",
		n3_widePropertyLen = "20",
		n3_disableAbbrevBaseUri = "true",
		n3_disableUsePropertySymbols = "true",
		n3_disableUseTripleQuotedStrings = "true",
		n3_disableUseDoubles = "true"
	)
	static class R {}
	static ClassInfo r = ClassInfo.of(R.class);

	@Test void a21_n3Settings_parser() {
		var al = AnnotationWorkList.of(sr, rstream(r.getAnnotations()));
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}
}
