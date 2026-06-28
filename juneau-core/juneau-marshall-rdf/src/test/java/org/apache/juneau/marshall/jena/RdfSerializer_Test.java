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
package org.apache.juneau.marshall.jena;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.internal.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5976" // Explicit per-case builder/serializer tests are clearer here than a single parameterized rewrite.
})
class RdfSerializer_Test extends TestBase {

	public static class NamedBean {
		private String name;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
	}

	@Nested class A_builderSettings extends TestBase {

		@Test void a01_addBeanTypesRdf() {
			// Verify addBeanTypesRdf builder methods don't throw and produce a valid serializer
			var s1 = RdfSerializer.create().addBeanTypesRdf().build();
			assertNotNull(s1);
			var s2 = RdfSerializer.create().addBeanTypesRdf(false).build();
			assertNotNull(s2);
			var s3 = RdfSerializer.create().addBeanTypesRdf(true).build();
			assertNotNull(s3);
		}

		@Test void a02_addLiteralTypes() {
			var x1 = RdfSerializer.create().addLiteralTypes().build().getSession();
			assertTrue(x1.isAddLiteralTypes());
			var x2 = RdfSerializer.create().addLiteralTypes(false).build().getSession();
			assertFalse(x2.isAddLiteralTypes());
		}

		@Test void a03_addRootProperty() {
			var x1 = RdfSerializer.create().addRootProperty().build().getSession();
			assertTrue(x1.isAddRootProp());
			var x2 = RdfSerializer.create().addRootProperty(false).build().getSession();
			assertFalse(x2.isAddRootProp());
		}

		@Test void a04_disableAutoDetectNamespaces() {
			var x1 = RdfSerializer.create().disableAutoDetectNamespaces().build().getSession();
			assertFalse(x1.isAutoDetectNamespaces());
			var x2 = RdfSerializer.create().disableAutoDetectNamespaces(false).build().getSession();
			assertTrue(x2.isAutoDetectNamespaces());
		}

		@Test void a05_disableUseXmlNamespaces() {
			var x1 = RdfSerializer.create().disableUseXmlNamespaces().build().getSession();
			assertFalse(x1.isUseXmlNamespaces());
			var x2 = RdfSerializer.create().disableUseXmlNamespaces(false).build().getSession();
			assertTrue(x2.isUseXmlNamespaces());
		}

		@Test void a06_looseCollections() {
			var x1 = RdfSerializer.create().looseCollections().build().getSession();
			assertTrue(x1.isLooseCollections());
			var x2 = RdfSerializer.create().looseCollections(false).build().getSession();
			assertFalse(x2.isLooseCollections());
		}

		@Test void a07_language_ntriple() {
			var x = RdfSerializer.create().language("N-TRIPLE").build().getSession();
			assertEquals("N-TRIPLE", x.getLanguage());
		}

		@Test void a08_language_turtle() {
			var x = RdfSerializer.create().language("TURTLE").build().getSession();
			assertEquals("TURTLE", x.getLanguage());
		}

		@Test void a09_collectionFormat_bag() {
			var x = RdfSerializer.create().collectionFormat(RdfCollectionFormat.BAG).build().getSession();
			assertEquals(RdfCollectionFormat.BAG, x.getCollectionFormat());
		}

		@Test void a10_collectionFormat_seq() {
			var x = RdfSerializer.create().collectionFormat(RdfCollectionFormat.SEQ).build().getSession();
			assertEquals(RdfCollectionFormat.SEQ, x.getCollectionFormat());
		}

		@Test void a11_juneauNs() {
			var ns = Namespace.of("myj", "http://myjuneau/");
			var x = RdfSerializer.create().juneauNs(ns).build().getSession();
			assertEquals(ns, x.getJuneauNs());
		}

		@Test void a12_juneauBpNs() {
			var ns = Namespace.of("mybp", "http://mybp/");
			var x = RdfSerializer.create().juneauBpNs(ns).build().getSession();
			assertEquals(ns, x.getJuneauBpNs());
		}

		@Test void a13_namespaces() {
			var ns = Namespace.of("ex", "http://example.org/");
			var x = RdfSerializer.create().namespaces(ns).build().getSession();
			assertTrue(x.getNamespaces().length > 0);
		}

		@Test void a14_copy_builder() {
			var b = RdfSerializer.create().language("N-TRIPLE").addLiteralTypes();
			var copy = b.copy();
			assertEquals("N-TRIPLE", copy.build().getSession().getLanguage());
			assertTrue(copy.build().getSession().isAddLiteralTypes());
		}

		@Test void a15_copy_serializer() {
			var s = RdfSerializer.create().language("TURTLE").addRootProperty().build();
			var copy = s.copy();
			assertEquals("TURTLE", copy.build().getSession().getLanguage());
			assertTrue(copy.build().getSession().isAddRootProp());
		}
	}

	@Nested class B_builderOverrides extends TestBase {

		@Test void b01_beanVisibility() {
			assertNotNull(RdfSerializer.create().beanClassVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfSerializer.create().beanConstructorVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfSerializer.create().beanFieldVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfSerializer.create().beanMethodVisibility(Visibility.PUBLIC).build());
		}

		@Test void b02_beanContext() {
			assertNotNull(RdfSerializer.create().marshallingContext(MarshallingContext.create().build()).build());
			assertNotNull(RdfSerializer.create().marshallingContext(MarshallingContext.create()).build());
		}

		@Marshalled(typeName="myDictBean")
		static class B03_DictBean {}

		@Test void b03_beanDictionary() {
			assertNotNull(RdfSerializer.create().beanDictionary(B03_DictBean.class).build());
		}

		@SuppressWarnings({
			"unchecked", // Raw type needed for test simplicity
			"rawtypes" // (Class) cast required by beanInterceptor API
		})
		@Test void b04_beanInterceptor() {
			assertNotNull(RdfSerializer.create().beanInterceptor(String.class, (Class)BeanInterceptor.class).build());
		}

		@Test void b05_beanMapPutReturnsOldValue() {
			assertNotNull(RdfSerializer.create().beanMapPutReturnsOldValue().build());
		}

		@Test void b06_beanProperties() {
			assertNotNull(RdfSerializer.create().beanProperties(String.class, "foo").build());
			assertNotNull(RdfSerializer.create().beanProperties(Map.of("String", "foo")).build());
			assertNotNull(RdfSerializer.create().beanProperties("java.lang.String", "foo").build());
		}

		@Test void b07_beanPropertiesExcludes() {
			assertNotNull(RdfSerializer.create().beanPropertiesExcludes(String.class, "foo").build());
			assertNotNull(RdfSerializer.create().beanPropertiesExcludes(Map.of("String", "foo")).build());
			assertNotNull(RdfSerializer.create().beanPropertiesExcludes("java.lang.String", "foo").build());
		}

		@Test void b08_beanPropertiesReadOnly() {
			assertNotNull(RdfSerializer.create().beanPropertiesReadOnly(String.class, "foo").build());
			assertNotNull(RdfSerializer.create().beanPropertiesReadOnly(Map.of("String", "foo")).build());
			assertNotNull(RdfSerializer.create().beanPropertiesReadOnly("java.lang.String", "foo").build());
		}

		@Test void b09_beanPropertiesWriteOnly() {
			assertNotNull(RdfSerializer.create().beanPropertiesWriteOnly(String.class, "foo").build());
			assertNotNull(RdfSerializer.create().beanPropertiesWriteOnly(Map.of("String", "foo")).build());
			assertNotNull(RdfSerializer.create().beanPropertiesWriteOnly("java.lang.String", "foo").build());
		}

		@Test void b10_beansRequire() {
			assertNotNull(RdfSerializer.create().beansRequireDefaultConstructor().build());
			assertNotNull(RdfSerializer.create().beansRequireSerializable().build());
			assertNotNull(RdfSerializer.create().beansRequireSettersForGetters().build());
		}

		@Test void b11_debug() {
			assertNotNull(RdfSerializer.create().debug().build());
			assertNotNull(RdfSerializer.create().debug(false).build());
		}

		@Test void b12_detectRecursions() {
			assertNotNull(RdfSerializer.create().detectRecursions().build());
			assertNotNull(RdfSerializer.create().detectRecursions(false).build());
		}

		@Test void b13_disableBeansRequireSomeProperties() {
			assertNotNull(RdfSerializer.create().disableBeansRequireSomeProperties().build());
		}

		@Test void b14_disableIgnore() {
			assertNotNull(RdfSerializer.create().disableIgnoreMissingSetters().build());
			assertNotNull(RdfSerializer.create().disableIgnoreTransientFields().build());
			assertNotNull(RdfSerializer.create().disableIgnoreUnknownNullBeanProperties().build());
			assertNotNull(RdfSerializer.create().disableInterfaceProxies().build());
		}
	}

	@Nested class C_serialization extends TestBase {

		@Test void c01_serialize_string_ntriple() throws Exception {
			var s = RdfSerializer.create().language("N-TRIPLE").build();
			var result = s.serialize("foo");
			assertNotNull(result);
			assertFalse(result.isEmpty());
		}

		@Test void c02_serialize_string_turtle() throws Exception {
			var s = RdfSerializer.create().language("TURTLE").build();
			var result = s.serialize("foo");
			assertNotNull(result);
			assertFalse(result.isEmpty());
		}

		@Test void c03_serialize_string_rdfxml() throws Exception {
			var s = RdfSerializer.create().language("RDF/XML").build();
			var result = s.serialize("foo");
			assertNotNull(result);
			assertFalse(result.isEmpty());
		}

		@Test void c04_serialize_with_addLiteralTypes() throws Exception {
			var s = RdfSerializer.create().language("N-TRIPLE").addLiteralTypes().build();
			var result = s.serialize(42);
			assertNotNull(result);
			assertFalse(result.isEmpty());
		}

		@Test void c05_serialize_with_addRootProperty() throws Exception {
			var s = RdfSerializer.create().language("TURTLE").addRootProperty().build();
			var result = s.serialize("foo");
			assertNotNull(result);
			assertFalse(result.isEmpty());
		}
	}

	@Nested class E_beanMeta extends TestBase {

		// Parent class annotated with @Rdf - ap.find() uses PARENTS traversal and finds this
		// This exercises RdfBeanPropertyMeta.forEach (lines 67-71): both collectionFormat and beanUri branches
		@Rdf(collectionFormat = RdfCollectionFormat.SEQ)
		public static class E01_RdfParent {
			private List<String> items = new ArrayList<>(List.of("a", "b"));
			public List<String> getItems() { return items; }
			public void setItems(List<String> v) { items = v; }
		}

		@Rdf(beanUri = true)
		public static class E01_RdfParentWithBeanUri extends E01_RdfParent {
			private String uri = "http://example.org/x";
			public String getUri() { return uri; }
			public void setUri(String v) { uri = v; }
		}

		public static class E01_ChildBean extends E01_RdfParentWithBeanUri {}

		@Test void e01_beanPropertyMeta_parent_rdf() throws Exception {
			var s = (RdfSerializer) RdfSerializer.create().language("N-TRIPLE").build();
			var bc = s.getMarshallingContext();
			var cm = bc.getClassMeta(E01_ChildBean.class);
			var bm = cm.getBeanMeta();
			assertNotNull(bm);
			// Directly create RdfBeanPropertyMeta to verify annotation discovery
			var itemsBpm = bm.getPropertyMeta("items");
			assertNotNull(itemsBpm);
			var rdfBpm = new RdfBeanPropertyMeta(itemsBpm, s);
			// @Rdf(collectionFormat=SEQ) is on E01_RdfParent which is a parent of E01_ChildBean
			// The PARENTS traversal should find it
			assertNotNull(rdfBpm.getCollectionFormat()); // SEQ or DEFAULT
			// Also serialize to trigger the coverage paths
			var result = s.serialize(new E01_ChildBean());
			assertNotNull(result);
		}

		// Class-level @Rdf on parent - exercises RdfClassMeta lines 51, 54, 82
		@Rdf(collectionFormat = RdfCollectionFormat.SEQ, prefix = "ex", namespace = "http://example.org/")
		public static class E02_RdfAnnotatedParent {}

		public static class E02_BeanWithRdfParent extends E02_RdfAnnotatedParent {
			private String name = "test";
			public String getName() { return name; }
			public void setName(String v) { name = v; }
		}

		// @Rdf with no collectionFormat (DEFAULT) - exercises the false branch in filter (line 54)
		@Rdf(prefix = "ex2", namespace = "http://example2.org/")
		public static class E03_RdfAnnotatedParentNoCollectionFormat {}

		public static class E03_BeanWithNoCollectionFormat extends E03_RdfAnnotatedParentNoCollectionFormat {
			private String name = "test";
			public String getName() { return name; }
			public void setName(String v) { name = v; }
		}

		@Test void e02_classMeta_rdf_parent() throws Exception {
			// E02_BeanWithRdfParent's ClassMeta has E02_RdfAnnotatedParent as parent with @Rdf(collectionFormat=SEQ)
			// cm.forEachAnnotation(Rdf.class) with PARENTS+PACKAGE traversal finds @Rdf on the parent
			// SEQ != DEFAULT so the filter at line 54 passes and collectionFormat = SEQ (true branch)
			var s = (RdfSerializer) RdfSerializer.create().language("N-TRIPLE").build();
			var bc = s.getMarshallingContext();
			var cm = bc.getClassMeta(E02_BeanWithRdfParent.class);
			var rdfCm = s.getRdfClassMeta(cm);
			assertNotNull(rdfCm);
			assertNotNull(rdfCm.getNamespace());  // exercises getNamespace() at line 82
			assertEquals(RdfCollectionFormat.SEQ, rdfCm.getCollectionFormat());
			var result = s.serialize(new E02_BeanWithRdfParent());
			assertNotNull(result);
		}

		@Test void e03_classMeta_no_collection_format() throws Exception {
			// E03_BeanWithNoCollectionFormat has a parent with @Rdf but no collectionFormat
			// The filter at line 54 returns false for DEFAULT, so orElse(DEFAULT) is used (false branch)
			var s = (RdfSerializer) RdfSerializer.create().language("N-TRIPLE").build();
			var bc = s.getMarshallingContext();
			var cm = bc.getClassMeta(E03_BeanWithNoCollectionFormat.class);
			var rdfCm = s.getRdfClassMeta(cm);
			assertNotNull(rdfCm);
			assertEquals(RdfCollectionFormat.DEFAULT, rdfCm.getCollectionFormat());
			var result = s.serialize(new E03_BeanWithNoCollectionFormat());
			assertNotNull(result);
		}
	}

	@Test void d01_constants_class() {
		// Verify constants are accessible at runtime (not all are compile-time inlined)
		assertNotNull(Constants.RDF_NIL);
		assertNotNull(Constants.RDF_SEQ);
		assertNotNull(Constants.RDF_BAG);
		assertNotNull(Constants.RDF_juneauNs_ITEMS);
	}

	@Nested class F_inheritedBuilderMethods extends TestBase {

		static class F01_TestSerializerListener extends SerializerListener {}

		@Test void f01_copyConstrutor() {
			var ns = Namespace.of("ex", "http://example.org/");
			var s = RdfSerializer.create().language("N-TRIPLE").namespaces(ns).build();
			var copy = s.copy().build();
			assertNotNull(copy);
		}

		@Test void f02_exampleAndCharsets() {
			assertNotNull(RdfSerializer.create().example(Integer.class, "42").build());
			assertNotNull(RdfSerializer.create().example(Integer.class, 42).build());
			assertNotNull(RdfSerializer.create().fileCharset(java.nio.charset.StandardCharsets.UTF_8).build());
			assertNotNull(RdfSerializer.create().streamCharset(java.nio.charset.StandardCharsets.UTF_8).build());
		}

		@Test void f03_findFluentSetters() {
			assertNotNull(RdfSerializer.create().findFluentSetters().build());
			assertNotNull(RdfSerializer.create().findFluentSetters(String.class).build());
		}

		@Test void f04_ignoreAndImpl() {
			assertNotNull(RdfSerializer.create().ignoreInvocationExceptionsOnGetters().build());
			assertNotNull(RdfSerializer.create().ignoreInvocationExceptionsOnSetters().build());
			assertNotNull(RdfSerializer.create().ignoreRecursions().build());
			assertNotNull(RdfSerializer.create().ignoreRecursions(false).build());
			assertNotNull(RdfSerializer.create().ignoreUnknownBeanProperties().build());
			assertNotNull(RdfSerializer.create().ignoreUnknownEnumValues().build());
			assertNotNull(RdfSerializer.create().impl(RdfSerializer.create().build()).build());
			assertNotNull(RdfSerializer.create().implClass(List.class, ArrayList.class).build());
			var implMap = new HashMap<Class<?>,Class<?>>();
			implMap.put(List.class, ArrayList.class);
			assertNotNull(RdfSerializer.create().implClasses(implMap).build());
			assertNotNull(RdfSerializer.create().interfaceClass(List.class, List.class).build());
			assertNotNull(RdfSerializer.create().interfaces(List.class).build());
		}

		@Test void f05_depthAndNesting() {
			assertNotNull(RdfSerializer.create().initialDepth(0).build());
			assertNotNull(RdfSerializer.create().maxDepth(10).build());
			assertNotNull(RdfSerializer.create().maxIndent(10).build());
		}

		@Test void f06_keepNullAndListener() {
			assertNotNull(RdfSerializer.create().keepNullProperties().build());
			assertNotNull(RdfSerializer.create().keepNullProperties(false).build());
			assertNotNull(RdfSerializer.create().listener(F01_TestSerializerListener.class).build());
			assertNotNull(RdfSerializer.create().locale(Locale.ENGLISH).build());
			assertNotNull(RdfSerializer.create().mediaType(org.apache.juneau.commons.http.MediaType.of("text/xml+rdf")).build());
		}

		@Test void f07_n3Settings() {
			assertNotNull(RdfSerializer.create().n3().build());
			assertNotNull(RdfSerializer.create().n3_disableAbbrevBaseUri().build());
			assertNotNull(RdfSerializer.create().n3_disableAbbrevBaseUri(false).build());
			assertNotNull(RdfSerializer.create().n3_disableObjectLists().build());
			assertNotNull(RdfSerializer.create().n3_disableObjectLists(false).build());
			assertNotNull(RdfSerializer.create().n3_disableUseDoubles().build());
			assertNotNull(RdfSerializer.create().n3_disableUseDoubles(false).build());
			assertNotNull(RdfSerializer.create().n3_disableUsePropertySymbols().build());
			assertNotNull(RdfSerializer.create().n3_disableUsePropertySymbols(false).build());
			assertNotNull(RdfSerializer.create().n3_disableUseTripleQuotedStrings().build());
			assertNotNull(RdfSerializer.create().n3_disableUseTripleQuotedStrings(false).build());
			assertNotNull(RdfSerializer.create().n3_indentProperty(4).build());
			assertNotNull(RdfSerializer.create().n3_minGap(1).build());
			assertNotNull(RdfSerializer.create().n3_propertyColumn(8).build());
			assertNotNull(RdfSerializer.create().n3_subjectColumn(8).build());
			assertNotNull(RdfSerializer.create().n3_widePropertyLen(20).build());
		}

		@Test void f08_notBeanAndPropertyNamer() {
			assertNotNull(RdfSerializer.create().notBeanClasses(String.class).build());
			assertNotNull(RdfSerializer.create().notBeanPackages("java.lang").build());
			assertNotNull(RdfSerializer.create().ntriple().build());
			assertNotNull(RdfSerializer.create().produces("text/xml+rdf").build());
			assertNotNull(RdfSerializer.create().propertyNamer(String.class, PropertyNamerDLC.class).build());
			assertNotNull(RdfSerializer.create().propertyNamer(PropertyNamerDLC.class).build());
			assertNotNull(RdfSerializer.create().quoteChar('"').build());
			assertNotNull(RdfSerializer.create().quoteCharOverride('"').build());
		}

		@Test void f09_rdfXmlSettings() {
			assertNotNull(RdfSerializer.create().rdfxml_allowBadUris().build());
			assertNotNull(RdfSerializer.create().rdfxml_allowBadUris(false).build());
			assertNotNull(RdfSerializer.create().rdfxml_attributeQuoteChar('"').build());
			assertNotNull(RdfSerializer.create().rdfxml_blockRules("").build());
			assertNotNull(RdfSerializer.create().rdfxml_disableShowDoctypeDeclaration().build());
			assertNotNull(RdfSerializer.create().rdfxml_disableShowDoctypeDeclaration(false).build());
			assertNotNull(RdfSerializer.create().rdfxml_embedding().build());
			assertNotNull(RdfSerializer.create().rdfxml_embedding(false).build());
			assertNotNull(RdfSerializer.create().rdfxml_errorMode("default").build());
			assertNotNull(RdfSerializer.create().rdfxml_iriRules("lax").build());
			assertNotNull(RdfSerializer.create().rdfxml_longId().build());
			assertNotNull(RdfSerializer.create().rdfxml_longId(false).build());
			assertNotNull(RdfSerializer.create().rdfxml_relativeUris("").build());
			assertNotNull(RdfSerializer.create().rdfxml_showXmlDeclaration("false").build());
			assertNotNull(RdfSerializer.create().rdfxml_tab(4).build());
			assertNotNull(RdfSerializer.create().rdfxml_xmlbase("http://example.org/").build());
		}

		@Test void f10_sortAndOther() {
			assertNotNull(RdfSerializer.create().sortCollections().build());
			assertNotNull(RdfSerializer.create().sortCollections(false).build());
			assertNotNull(RdfSerializer.create().sortMaps().build());
			assertNotNull(RdfSerializer.create().sortMaps(false).build());
			assertNotNull(RdfSerializer.create().build());
			assertNotNull(RdfSerializer.create().build());
			assertNotNull(RdfSerializer.create().sq().build());
			assertNotNull(RdfSerializer.create().stopClass(String.class, Object.class).build());
			assertNotNull(RdfSerializer.create().swaps(new Class<?>[0]).build());
			assertNotNull(RdfSerializer.create().swaps(new Object[0]).build());
			assertNotNull(RdfSerializer.create().timeZone(TimeZone.getDefault()).build());
			assertNotNull(RdfSerializer.create().trimEmptyCollections().build());
			assertNotNull(RdfSerializer.create().trimEmptyCollections(false).build());
			assertNotNull(RdfSerializer.create().trimEmptyMaps().build());
			assertNotNull(RdfSerializer.create().trimEmptyMaps(false).build());
			assertNotNull(RdfSerializer.create().trimStrings().build());
			assertNotNull(RdfSerializer.create().trimStrings(false).build());
		}

		@Test void f11_languageShortcuts() {
			assertNotNull(RdfSerializer.create().turtle().build());
			assertNotNull(RdfSerializer.create().jsonLd().build());
			assertNotNull(RdfSerializer.create().nQuads().build());
			assertNotNull(RdfSerializer.create().triG().build());
			assertNotNull(RdfSerializer.create().triX().build());
			assertNotNull(RdfSerializer.create().rdfJson().build());
			assertNotNull(RdfSerializer.create().xml().build());
			assertNotNull(RdfSerializer.create().xmlabbrev().build());
		}

		@Test void f12_typeAndUri() {
			assertNotNull(RdfSerializer.create().type(RdfSerializer.class).build());
			assertNotNull(RdfSerializer.create().typeName(String.class, "myString").build());
			assertNotNull(RdfSerializer.create().typePropertyName(String.class, "_type").build());
			assertNotNull(RdfSerializer.create().typePropertyName("_type").build());
			assertNotNull(RdfSerializer.create().uriContext(UriContext.of("http://localhost", "", "", "")).build());
			assertNotNull(RdfSerializer.create().uriRelativity(UriRelativity.RESOURCE).build());
			assertNotNull(RdfSerializer.create().uriResolution(UriResolution.ABSOLUTE).build());
			assertNotNull(RdfSerializer.create().enumFormat(EnumFormat.NAME).build());
			assertNotNull(RdfSerializer.create().useJavaBeanIntrospector().build());
			assertNotNull(RdfSerializer.create().useWhitespace().build());
			assertNotNull(RdfSerializer.create().useWhitespace(false).build());
			assertNotNull(RdfSerializer.create().ws().build());
		}
	}

	@Nested class G_streamClasses extends TestBase {

		@Test void g01_streamSerializer_rdfthrift_no_produces() {
			// Build without setting produces() to trigger switch(LANG_RDFTHRIFT) in getProduces() // NOSONAR
			var s = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build();
			assertNotNull(s);
		}

		@Test void g02_streamSerializer_rdfproto_no_produces() {
			// Build without setting produces() to trigger switch(LANG_RDFPROTO) in getProduces() // NOSONAR
			var s = RdfStreamSerializer.create().language(Constants.LANG_RDFPROTO).build();
			assertNotNull(s);
		}

		@Test void g03_streamSerializer_default_language_no_produces() {
			// Default language (not THRIFT/PROTO) triggers default case in getProduces()
			var s = RdfStreamSerializer.create().language("N-TRIPLE").build();
			assertNotNull(s);
		}

		@Test void g04_streamSerializer_getDelegationMethods() {
			// Exercise the delegation methods in RdfStreamSerializer
			var s = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build();
			var bc = s.getMarshallingContext();
			var cm = bc.getClassMeta(String.class);
			assertNotNull(s.getRdfClassMeta(cm));
			assertNotNull(s.getCollectionFormat());
			assertNotNull(s.getJuneauNs());
			assertNotNull(s.getJuneauBpNs());
			assertNotNull(s.getNamespaces());
			assertNotNull(s.getLanguage());
			assertNotNull(s.getRdfSerializer());
			assertFalse(s.isAddLiteralTypes());
			assertFalse(s.isAddRootProp());
			assertFalse(s.isLooseCollections());
			assertTrue(s.isUseXmlNamespaces());
			assertTrue(s.isAutoDetectNamespaces());
		}

		@Test void g05_streamSerializer_copy() {
			var s = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build();
			var copy = s.copy().build();
			assertNotNull(copy);
		}

		@Test void g06_streamParser_rdfthrift_no_consumes() {
			// Build without setting consumes() to trigger switch(LANG_RDFTHRIFT) in getConsumes() // NOSONAR
			var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
			assertNotNull(p);
		}

		@Test void g07_streamParser_rdfproto_no_consumes() {
			// Build without setting consumes() to trigger switch(LANG_RDFPROTO) in getConsumes() // NOSONAR
			var p = RdfStreamParser.create().language(Constants.LANG_RDFPROTO).build();
			assertNotNull(p);
		}

		@Test void g08_streamParser_default_language_no_consumes() {
			// Default case in switch
			var p = RdfStreamParser.create().language("N-TRIPLE").build();
			assertNotNull(p);
		}

		@Test void g09_streamParser_getDelegationMethods() {
			// Exercise the delegation methods in RdfStreamParser
			var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
			var bc = p.getMarshallingContext();
			var cm = bc.getClassMeta(String.class);
			assertNotNull(p.getRdfClassMeta(cm));
			assertNotNull(p.getLanguage());
			assertNotNull(p.getRdfParser());
		}

		@Test void g10_streamParser_copy() {
			var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
			var copy = p.copy().build();
			assertNotNull(copy);
		}

		@Test void g11_rdfSerializer_xmlMetaProviderMethods() {
			// Cover getXmlBeanMeta and getXmlBeanPropertyMeta in RdfSerializer
			var s = RdfSerializer.create().build();
			var bc = s.getMarshallingContext();
			var bm = bc.getBeanMeta(NamedBean.class);
			assertNotNull(bm);
			assertNotNull(s.getXmlBeanMeta(bm));
			var bpm = bm.getPropertyMeta("name");
			assertNotNull(bpm);
			assertNotNull(s.getXmlBeanPropertyMeta(bpm));
			assertNotNull(s.getXmlClassMeta(bc.getClassMeta(String.class)));
		}

		@Test void g12_rdfSerializer_sessionBuilderMethods() {
			// Cover session builder fluent methods in RdfSerializerSession
			var s = RdfSerializer.create().build();
			assertNotNull(s.createSession().apply(String.class, x -> {}).build());
			assertNotNull(s.createSession().debug(false).build());
			assertNotNull(s.createSession().javaMethod(null).build());
			assertNotNull(s.createSession().locale(Locale.US).build());
			assertNotNull(s.createSession().fileCharset(java.nio.charset.Charset.defaultCharset()).build());
			assertNotNull(s.createSession().mediaType(org.apache.juneau.commons.http.MediaType.JSON).build());
		}
	}

	@Nested class H_languageRoundTrips extends TestBase {

		@Test void h01_serialize_n3() throws Exception {
			// N3 language variant — not covered by A_builderSettings session-getter tests
			var s = RdfSerializer.create().n3().build();
			assertEquals("N3", s.getSession().getLanguage());
			var result = s.serialize("hello");
			assertNotNull(result);
			assertFalse(result.isEmpty());
		}

		@Test void h02_serialize_ntriple_string_containsContent() throws Exception {
			// N-TRIPLE output always emits at least one triple line
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize("world");
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void h03_serialize_turtle_string_containsContent() throws Exception {
			// TURTLE output for a plain string value is non-empty
			var s = RdfSerializer.create().turtle().build();
			var result = s.serialize("test");
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void h04_addBeanTypesRdf_true_produces_output() throws Exception {
			// addBeanTypesRdf(true) should produce a non-empty serialization without error
			var s = RdfSerializer.create().ntriple().addBeanTypesRdf(true).build();
			assertNotNull(s);
			var bean = new NamedBean();
			bean.setName("test");
			var result = s.serialize(bean);
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void h05_addBeanTypesRdf_false_produces_output() throws Exception {
			// addBeanTypesRdf(false) serializes normally without type triples
			var s = RdfSerializer.create().ntriple().addBeanTypesRdf(false).build();
			assertNotNull(s);
			var bean = new NamedBean();
			bean.setName("test");
			var result = s.serialize(bean);
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void h06_serialize_list_ntriple() throws Exception {
			// Collection serialization via N-TRIPLE language
			var s = RdfSerializer.create().ntriple().build();
			var list = new ArrayList<>(List.of("a", "b", "c"));
			var result = s.serialize(list);
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void h07_serialize_list_turtle() throws Exception {
			// Collection serialization via TURTLE language
			var s = RdfSerializer.create().turtle().build();
			var list = new ArrayList<>(List.of("x", "y"));
			var result = s.serialize(list);
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void h08_trimStrings_session_flag() throws Exception {
			// trimStrings=true is surfaced via the inherited serializer session flag
			var s1 = RdfSerializer.create().trimStrings(true).build();
			assertNotNull(s1);
			var s2 = RdfSerializer.create().trimStrings(false).build();
			assertNotNull(s2);
			// Serialize a string with surrounding whitespace; trimStrings strips it before output
			var trimmed = s1.serialize("  hello  ");
			var notTrimmed = s2.serialize("  hello  ");
			assertNotNull(trimmed);
			assertNotNull(notTrimmed);
		}
	}

	@Nested class I_streamSerializerBranchFills extends TestBase {

		public static class BeanWithList {
			public List<String> items = new ArrayList<>();
		}

		@Test void i01_collectionFormat_bag_on_bean_property() throws Exception {
			// Triggers RdfStreamSerializerSession collectionFormat BAG path on a bean property list
			var bean = new BeanWithList();
			bean.items = new ArrayList<>(List.of("a", "b", "c"));
			var s = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build();
			var result = s.serialize(bean);
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void i02_collectionFormat_seq_on_bean_property() throws Exception {
			// Triggers RdfStreamSerializerSession collectionFormat SEQ path
			var bean = new BeanWithList();
			bean.items = new ArrayList<>(List.of("x", "y"));
			var s = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build();
			var result = s.serialize(bean);
			assertNotNull(result);
		}

		@Test void i03_collectionFormat_multiValued_on_bean_property() throws Exception {
			// Triggers RdfStreamSerializerSession collectionFormat MULTI_VALUED path
			var bean = new BeanWithList();
			bean.items = new ArrayList<>(List.of("p", "q"));
			var s = RdfSerializer.create().turtle().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build();
			var result = s.serialize(bean);
			assertNotNull(result);
		}

		@Test void i04_serialize_null_value_in_bean_keepNullProperties() throws Exception {
			// Triggers null property value serialization path
			var bean = new BeanWithList();
			bean.items = null;
			var s = RdfSerializer.create().ntriple().keepNullProperties().build();
			var result = s.serialize(bean);
			assertNotNull(result);
		}
	}

	@Nested class J_serializationVariousTypes extends TestBase {

		@Test void j01_serialize_map_ntriple() throws Exception {
			// sType.isMap() path with a plain HashMap (non-BeanMap) exercises serializeMap()
			var s = RdfSerializer.create().ntriple().build();
			var map = new LinkedHashMap<String,String>();
			map.put("key1", "val1");
			map.put("key2", "val2");
			assertFalse(s.serialize(map).isBlank());
		}

		@Test void j02_serialize_map_turtle() throws Exception {
			// Same map path via TURTLE language
			var s = RdfSerializer.create().turtle().build();
			var map = new LinkedHashMap<String,String>();
			map.put("alpha", "one");
			assertFalse(s.serialize(map).isBlank());
		}

		@Test void j03_serialize_reader() throws Exception {
			// sType.isReader() path in serializeAnything
			var s = RdfSerializer.create().ntriple().build();
			assertNotNull(s.serialize(new StringReader("reader content")));
		}

		@Test void j04_serialize_inputstream() throws Exception {
			// sType.isInputStream() path in serializeAnything
			var s = RdfSerializer.create().ntriple().build();
			assertNotNull(s.serialize(new ByteArrayInputStream("bytes".getBytes())));
		}

		@Test void j05_serialize_number_with_literal_types() throws Exception {
			// isAddLiteralTypes()=true exercises the createTypedLiteral branch (line 325)
			var s = RdfSerializer.create().ntriple().addLiteralTypes().build();
			assertNotNull(s.serialize(42));
		}

		@Test void j06_serialize_boolean_with_literal_types() throws Exception {
			// isAddLiteralTypes()=true for boolean value
			var s = RdfSerializer.create().ntriple().addLiteralTypes().build();
			assertNotNull(s.serialize(true));
		}

		@Test void j07_serialize_list_format() throws Exception {
			// LIST switch case in serializeAnything (line 313) — distinct from BAG/SEQ/DEFAULT
			var s = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build();
			assertFalse(s.serialize(new ArrayList<>(List.of("a", "b", "c"))).isBlank());
		}

		public static class J08_SimpleBean {
			public String name = "x";
		}

		@Test void j08_looseCollections_list() throws Exception {
			// doSerialize looseCollections + isCollection branch (lines 447-448)
			// Uses beans so each element produces an RDF resource with properties
			var s = RdfSerializer.create().ntriple().looseCollections().build();
			var list = new ArrayList<>(List.of(new J08_SimpleBean(), new J08_SimpleBean()));
			assertFalse(s.serialize(list).isBlank());
		}

		@Test void j09_looseCollections_array() throws Exception {
			// doSerialize looseCollections + isArray (not isCollection) branch (line 448)
			var s = RdfSerializer.create().ntriple().looseCollections().build();
			var arr = new J08_SimpleBean[]{ new J08_SimpleBean(), new J08_SimpleBean() };
			assertFalse(s.serialize(arr).isBlank());
		}

		@Test void j10_serialize_string_array() throws Exception {
			// sType.isCollectionOrArray() via array — exercises toList() path
			var s = RdfSerializer.create().ntriple().build();
			assertFalse(s.serialize(new String[]{"one", "two", "three"}).isBlank());
		}

		@Test void j11_language_n3pp_n3plain_n3triples() {
			// Covers the N3-PP, N3-PLAIN, N3-TRIPLES cases in the getAccept()/getProduces() switch
			assertNotNull(RdfSerializer.create().language("N3-PP").build());
			assertNotNull(RdfSerializer.create().language("N3-PLAIN").build());
			assertNotNull(RdfSerializer.create().language("N3-TRIPLES").build());
		}

		@Test void j12_invalid_language_throws_on_serialize() {
			// lang==null path in session constructor — throws exception on first serialize call
			var s = RdfSerializer.create().language("NOT-A-REAL-LANGUAGE").build();
			assertThrows(Exception.class, () -> s.serialize("test"));
		}

		@Test void j13_copy_from_serializer_with_disabled_settings() {
			// Builder(RdfSerializer copyFrom) with autoDetectNamespaces=false/useXmlNamespaces=false
			// exercises the !copyFrom.autoDetectNamespaces and !copyFrom.useXmlNamespaces branches
			var s = RdfSerializer.create().disableAutoDetectNamespaces().disableUseXmlNamespaces().build();
			var copy = s.copy().build();
			assertFalse(copy.getSession().isAutoDetectNamespaces());
			assertFalse(copy.getSession().isUseXmlNamespaces());
		}

		@Marshalled(typeName = "J14_TypedBean")
		public static class J14_TypedBean {
			public String label = "typed-label";
		}

		@Test void j14_serialize_typed_bean_in_object_array() throws Exception {
			// getBeanTypeName returns non-null when eType=Object != aType=J14_TypedBean
			// exercises the nn(typeName) true branch at line 344 in serializeBeanMap
			var s = RdfSerializer.create().ntriple().addBeanTypesRdf(true)
				.beanDictionary(J14_TypedBean.class).build();
			assertFalse(s.serialize(new Object[]{ new J14_TypedBean() }).isBlank());
		}

		public static class J15_BeanWithNullField {
			public String present = "yes";
			public String absent = null;
		}

		@Test void j15_serialize_bean_null_prop_keepNullProperties() throws Exception {
			// null property value with bpm set and isKeepNullProperties()=true (lines 255-257)
			var s = RdfSerializer.create().ntriple().keepNullProperties().build();
			assertNotNull(s.serialize(new J15_BeanWithNullField()));
		}

		@Test void j16_serialize_bean_null_prop_no_keepNullProperties() throws Exception {
			// null property value with bpm set and isKeepNullProperties()=false (line 255 false branch)
			var s = RdfSerializer.create().ntriple().build();
			assertNotNull(s.serialize(new J15_BeanWithNullField()));
		}

		public static class J17_BeanWithFilteredProp {
			public String included = "yes";
			@SuppressWarnings("unused")
			public String excluded = "no";
		}

		@Test void j17_serialize_bean_with_property_filter() throws Exception {
			// beanProperties filter exercises canIgnoreValue() at line 368
			var s = RdfSerializer.create().ntriple().beanProperties(J17_BeanWithFilteredProp.class, "included").build();
			assertNotNull(s.serialize(new J17_BeanWithFilteredProp()));
		}

		public static class J18_BeanWithMultiValued {
			public List<String> tags = new ArrayList<>(List.of("a", "b", "c"));
		}

		@Test void j18_serialize_multi_valued_collection() throws Exception {
			// MULTI_VALUED format routes to serializeToMultiProperties (line 308) for bean collection properties
			var s = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build();
			assertFalse(s.serialize(new J18_BeanWithMultiValued()).isBlank());
		}

		@Test void j19_serialize_char_zero() throws Exception {
			// sType.isChar() && charValue()==0 branch (line 254) — char(0) → null in RDF
			var s = RdfSerializer.create().ntriple().build();
			assertNotNull(s.serialize('\0'));
		}

		@Test void j20_serialize_char_nonzero() throws Exception {
			// isChar() with a normal non-zero char goes to isCharSequence()||isChar() at line 318
			var s = RdfSerializer.create().ntriple().build();
			assertNotNull(s.serialize('X'));
		}

		@Rdf(collectionFormat = RdfCollectionFormat.BAG)
		public static class J21_BagAnnotatedList extends ArrayList<String> {
			private static final long serialVersionUID = 1L;
		}

		@Test void j21_serialize_collection_with_class_level_rdf_annotation() throws Exception {
			// cRdf.getCollectionFormat() != DEFAULT exercises the class-level override (line 303)
			var list = new J21_BagAnnotatedList();
			list.add("x");
			list.add("y");
			var s = RdfSerializer.create().ntriple().build();
			assertFalse(s.serialize(list).isBlank());
		}

		public static class J22_BeanWithUriField {
			@org.apache.juneau.marshall.Uri
			public String myUri = "http://example.org/resource";
		}

		@Test void j22_serialize_bean_with_uri_property() throws Exception {
			// bpMeta.isUri() path in serializeBeanMap → isURI=true passed to serializeAnything (line 380)
			// exercises sType.isUri() || isURI branch (line 263) with an absolute URI
			var s = RdfSerializer.create().ntriple().build();
			assertFalse(s.serialize(new J22_BeanWithUriField()).isBlank());
		}

		public static class J23_BeanWithRelativeUri {
			@org.apache.juneau.marshall.Uri
			public String myUri = "relative/path";
		}

		@Test void j23_serialize_bean_with_relative_uri_property() throws Exception {
			// Non-absolute URI path (line 266 false branch) → encodeTextInvalidChars (line 269)
			// also exercises encodeTextInvalidChars with a non-null value
			var s = RdfSerializer.create().ntriple().build();
			assertNotNull(s.serialize(new J23_BeanWithRelativeUri()));
		}

		public static class J24_BeanWithRdfNs {
			@Rdf(namespace = "http://myns.example.org/", prefix = "myns")
			public String label = "value";
		}

		@Test void j24_serialize_bean_with_rdf_namespace_property() throws Exception {
			// bpRdf.getNamespace() != null path (line 372 false branch, line 374 false branch, line 376)
			// → autoDetectNamespaces adds the namespace prefix to the model
			var s = RdfSerializer.create().ntriple().build();
			assertFalse(s.serialize(new J24_BeanWithRdfNs()).isBlank());
		}

		@Test void j25_serialize_seq_collection_format() throws Exception {
			// SEQ format exercises the default branch in the switch (line 314)
			var s = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build();
			assertFalse(s.serialize(new ArrayList<>(List.of("p", "q"))).isBlank());
		}
	}

	@Nested class K_streamRoundTrips extends TestBase {

		@Test void k01_stream_serialize_string_thrift() throws Exception {
			// Actual serialization with RDF Thrift to cover RdfStreamSerializerSession branches
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize("hello");
			assertTrue(bytes.length > 0);
		}

		@Test void k02_stream_serialize_bean_thrift() throws Exception {
			// Serialize a bean via Thrift — exercises bean handling in RdfStreamSerializerSession
			var bean = new NamedBean();
			bean.setName("test");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(bean);
			assertTrue(bytes.length > 0);
		}

		@Test void k03_stream_serialize_list_thrift() throws Exception {
			// Collection serialization via Thrift
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new ArrayList<>(List.of("a", "b", "c")));
			assertTrue(bytes.length > 0);
		}

		@Test void k04_stream_serialize_map_thrift() throws Exception {
			// Map serialization via Thrift (non-BeanMap path)
			var map = new LinkedHashMap<String,String>();
			map.put("k", "v");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(map);
			assertTrue(bytes.length > 0);
		}

		@Test void k05_stream_roundtrip_string_thrift() throws Exception {
			// Round-trip String via Thrift — exercises RdfStreamParserSession
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize("roundtrip");
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, String.class);
			assertEquals("roundtrip", result);
		}

		@Test void k06_stream_roundtrip_bean_thrift() throws Exception {
			// Round-trip bean via Thrift — exercises parseIntoBeanMap in stream session
			var bean = new NamedBean();
			bean.setName("stream-test");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(bean);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, NamedBean.class);
			assertNotNull(result);
		}

		@Test void k07_stream_serialize_array_thrift() throws Exception {
			// Array serialization via Thrift exercises isArray path in stream session
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new String[]{"a", "b", "c"});
			assertTrue(bytes.length > 0);
		}

		@Test void k08_stream_serialize_boolean_thrift() throws Exception {
			// Boolean serialization via Thrift — exercises isBoolean path in stream session
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(Boolean.TRUE);
			assertTrue(bytes.length > 0);
		}

		@Test void k09_stream_serialize_byte_array_thrift() throws Exception {
			// sType.isByteArray() path in stream session — emitted as base64 typed literal (line 350-353)
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new byte[]{1, 2, 3, 4});
			assertTrue(bytes.length > 0);
		}

		@Test void k10_stream_serialize_char_thrift() throws Exception {
			// sType.isChar() with non-zero char — routes to isCharSequence()||isChar() (line 372)
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize('Q');
			assertTrue(bytes.length > 0);
		}

		@Test void k11_stream_serialize_url_thrift() throws Exception {
			// sType.isUri() path — absolute URL serialized as resource (line 321-325)
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new java.net.URL("http://example.org/stream"));
			assertTrue(bytes.length > 0);
		}

		@Test void k12_stream_roundtrip_byte_array() throws Exception {
			// Round-trip byte[] via Thrift — exercises isByteArray() in stream parser session
			var input = new byte[]{10, 20, 30};
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(input);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, byte[].class);
			assertNotNull(result);
		}

		@Test void k13_stream_roundtrip_char() throws Exception {
			// Round-trip char via Thrift — exercises isChar() in stream parser session
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize('X');
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Character.class);
			assertNotNull(result);
		}

		@Test void k14_stream_roundtrip_url() throws Exception {
			// Round-trip URL via Thrift — exercises isUri() in stream parser session (line 347)
			var url = new java.net.URL("http://example.org/k14");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(url);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void k15_stream_optional_thrift() throws Exception {
			// Optional serialized and parsed via Thrift — covers isOptional() branch in stream sessions
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(opt("opt-val"));
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Optional.class);
			assertNotNull(result);
		}

		public static class K16_BeanWithNull {
			public String name = null;
			public String val = "x";
		}

		@Test void k16_stream_keepnull_thrift() throws Exception {
			// keepNullProperties on stream serializer — covers isKeepNullProperties() TRUE in stream session
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).keepNullProperties().build()
				.serialize(new K16_BeanWithNull());
			assertTrue(bytes.length > 0);
		}

		public static class K17_BeanWithUri {
			@Rdf(beanUri = true)
			public String uri = "http://example.org/k17";
			public String name = "beanUriTest";
		}

		@Test void k17_stream_beanuri_thrift() throws Exception {
			// Bean with @Rdf(beanUri=true) — covers hasBeanUri() TRUE and bpRdf.isBeanUri() skip in stream session
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new K17_BeanWithUri());
			assertTrue(bytes.length > 0);
		}

		@Test void k18_stream_instant_thrift() throws Exception {
			// Serialize Instant and round-trip via Thrift — covers isTemporal() branch in stream parser session
			var now = Instant.now();
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(now);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Instant.class);
			assertNotNull(result);
		}

		@Test void k19_stream_duration_thrift() throws Exception {
			// Serialize Duration and round-trip via Thrift — covers isDuration() branch in stream parser session
			var d = Duration.ofHours(2);
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(d);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Duration.class);
			assertNotNull(result);
		}

		@Test void k20_stream_period_thrift() throws Exception {
			// Serialize Period and round-trip via Thrift — covers isPeriod() branch in stream parser session
			var p = Period.ofDays(5);
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(p);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Period.class);
			assertNotNull(result);
		}

		@Rdf(collectionFormat = RdfCollectionFormat.BAG)
		public static class K21_BagList extends ArrayList<String> {
			private static final long serialVersionUID = 1L;
		}

		@Test void k21_stream_bag_annotation_thrift() throws Exception {
			// @Rdf(collectionFormat=BAG) class annotation — covers class-level collectionFormat override in stream session
			var list = new K21_BagList();
			list.add("p");
			list.add("q");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(list);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, K21_BagList.class);
			assertNotNull(result);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Test void k22_stream_delegate_list_thrift() throws Exception {
			// DelegateList serialized via Thrift — covers isDelegate() branch in RdfStreamSerializerSession
			var ctx = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().getMarshallingContext();
			var cm = (ClassMeta)ctx.getClassMeta(ArrayList.class);
			var dl = new DelegateList(cm);
			dl.add("alpha");
			dl.add("beta");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(dl);
			assertTrue(bytes.length > 0);
		}

		@Test void k23_stream_integer_as_object() throws Exception {
			// Serialize integer (number branch in stream serializer line 374) and parse as Object
			// — covers non-String literal path (line 269 FALSE) in stream parser parseAnything
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(42);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Object.class);
			assertNotNull(result);
		}

		public static class K24_BeanWithNullUri {
			@Rdf(beanUri = true)
			public String uri = null;
			public String name = "nullBeanUri";
		}

		@Test void k24_stream_bean_with_null_beanuri() throws Exception {
			// @Rdf(beanUri=true) where uri is null — covers getUri(null,null) nn(uri)=FALSE in stream session
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new K24_BeanWithNullUri());
			assertTrue(bytes.length > 0);
		}

		@Test void k25_stream_beanuri_roundtrip() throws Exception {
			// Round-trip bean with @Rdf(beanUri=true) via Thrift — covers parseIntoBeanMap hasBeanUri() TRUE
			// in stream parser session (line 389: hasBeanUri && nn(r2.getURI()) → set uri from resource URI)
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new K17_BeanWithUri());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, K17_BeanWithUri.class);
			assertNotNull(result);
		}

		@Test void k26_stream_null_to_null() throws Exception {
			// Serialize null via Thrift → model has no triples → getRoots() empty → null returned
			// Covers stream doParse: roots.isEmpty()=TRUE, type.isOptional()=FALSE branch
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize((Object)null);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Object.class);
			assertNull(result);
		}

		@Test void k27_stream_null_as_optional() throws Exception {
			// Serialize null, parse as Optional → isEmpty=TRUE → opte()
			// Covers stream doParse: roots.isEmpty()=TRUE, type.isOptional()=TRUE branch
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize((Object)null);
			var result = (Optional<?>)RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Optional.class);
			assertNotNull(result);
			assertTrue(result.isEmpty());
		}

		@Test void k28_stream_date_roundtrip() throws Exception {
			// Serialize Date and parse back via Thrift — covers isDate() branch in stream parser parseAnything
			var date = new java.util.Date(1000000000L);
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(date);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, java.util.Date.class);
			assertNotNull(result);
		}

		@Test void k29_stream_calendar_roundtrip() throws Exception {
			// Serialize Calendar and parse back via Thrift — covers isCalendar() branch in stream parser parseAnything
			var cal = java.util.Calendar.getInstance();
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(cal);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, java.util.Calendar.class);
			assertNotNull(result);
		}

		public static class K30_MultiValuedBean {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public List<String> tags = new ArrayList<>(List.of("a", "b", "c"));
		}

		@Test void k30_stream_multivalue_annotation_thrift() throws Exception {
			// Bean with @Rdf(collectionFormat=MULTI_VALUED) via Thrift — covers MULTI_VALUED path in stream sessions
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new K30_MultiValuedBean());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, K30_MultiValuedBean.class);
			assertNotNull(result);
		}

		@Test void k31_stream_list_roundtrip() throws Exception {
			// Non-empty ArrayList serialized/parsed via Thrift (default SEQ format) —
			// covers SEQ container loop body in stream parser parseIntoCollection
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new ArrayList<>(List.of("x", "y", "z")));
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, ArrayList.class);
			assertNotNull(result);
		}

		@Test void k32_stream_string_roundtrip() throws Exception {
			// Plain String round-trip via Thrift — covers sType.isCharSequence() parse branch
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize("hello");
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, String.class);
			assertEquals("hello", result);
		}

		@Test void k33_stream_char_roundtrip() throws Exception {
			// Character round-trip via Thrift — covers sType.isChar() parse branch
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize('Z');
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Character.class);
			assertEquals('Z', result);
		}

		@Test void k34_stream_boolean_roundtrip() throws Exception {
			// Boolean round-trip via Thrift — covers sType.isBoolean() parse branch
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(true);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Boolean.class);
			assertEquals(Boolean.TRUE, result);
		}

		@Test void k35_stream_integer_typed_roundtrip() throws Exception {
			// Integer round-trip via Thrift — covers sType.isNumber() parse branch + parseNumber()
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(99);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Integer.class);
			assertEquals(99, result);
		}

		@Test void k36_stream_map_roundtrip() throws Exception {
			// Map round-trip via Thrift — covers sType.isMap() path in stream parser parseAnything
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(Map.of("key1", "val1"));
			var result = (Map<?,?>)RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Map.class);
			assertNotNull(result);
		}

		@Test void k37_stream_bean_unknown_property() throws Exception {
			// Bean with unknown property → ignoreUnknownBeanProperties() FALSE branch in stream parser
			// onUnknownProperty called (default: ignore) — covers parseIntoBeanMap else branch (line 444+)
			// Serialize a richer bean, parse into a slimmer one
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new K17_BeanWithUri());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).ignoreUnknownBeanProperties().build()
				.parse(bytes, K37_SlimBean.class);
			assertNotNull(result);
		}

		public static class K37_SlimBean { public String name; }

		@Test void k38_stream_bag_as_object() throws Exception {
			// BAG collection serialized via Thrift, parsed as Object — covers isBag()=TRUE path
			// in stream parser parseAnything sType.isObject() branch (line 281)
			var list = new K21_BagList();
			list.add("p");
			list.add("q");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(list);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Object.class);
			assertNotNull(result);
		}

		public static class K39_BeanWithList {
			@Rdf(collectionFormat = RdfCollectionFormat.LIST)
			public List<String> items = new ArrayList<>(List.of("p", "q"));
		}

		@Test void k39_stream_list_annotation_list_parse() throws Exception {
			// LIST (RDFList) collection round-trip via Thrift — covers r.canAs(RDFList.class) TRUE path
			// in stream parser parseIntoCollection(RDFList, ...)
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize(new K39_BeanWithList());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, K39_BeanWithList.class);
			assertNotNull(result);
		}

		@Test void k40_stream_string_as_object() throws Exception {
			// Serialize string, parse as Object — covers sType.isObject() + n.isLiteral() TRUE
			// + o instanceof String → decodeString() path (line 269-270 TRUE)
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.serialize("world");
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Object.class);
			assertNotNull(result);
		}
	}

	@Nested class M_moreSerializerBranches extends TestBase {

		public static class M01_BeanWithNull {
			public String name = null;
			public String val = "x";
		}

		@Test void m01_serialize_null_property_keepnull() throws Exception {
			// keepNullProperties() → isKeepNullProperties() TRUE (line 256): null property gets rdf:nil node.
			// Also covers the checkNull predicate (line 349): isKeepNullProperties()||nn(x) with x=null.
			var s = RdfSerializer.create().ntriple().keepNullProperties().build();
			var result = s.serialize(new M01_BeanWithNull());
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@Test void m02_serialize_beanmap_directly() throws Exception {
			// Serialize a BeanMap<T> directly — covers o instanceof BeanMap branch (line 282):
			// BeanMap implements Map → sType.isMap() = TRUE; o instanceof BeanMap o2 → TRUE.
			var s = RdfSerializer.create().ntriple().build();
			var bm = s.getMarshallingContext().toBeanMap(new NamedBean());
			bm.put("name", "bm-test");
			var result = s.serialize(bm);
			assertNotNull(result);
		}

		public static class M03_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri = "http://example.org/m03";
			public String label = "test";
		}

		@Test void m03_serialize_bean_with_rdf_bean_uri() throws Exception {
			// Bean with @Rdf(beanUri=true) — covers hasBeanUri() TRUE (line 275) and bpRdf.isBeanUri() skip (line 359)
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize(new M03_BeanWithBeanUri());
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		public static class M04_BeanWithNullBeanUri {
			@Rdf(beanUri = true)
			public String uri = null;
			public String label = "nulluri";
		}

		@Test void m04_serialize_bean_with_null_bean_uri() throws Exception {
			// @Rdf(beanUri=true) where uri field is null → getUri(null,null) covers nn(uri)=FALSE (line 190 FALSE)
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize(new M04_BeanWithNullBeanUri());
			assertNotNull(result);
		}

		@Test void m05_serialize_loose_collections_list() throws Exception {
			// looseCollections() → doSerialize line 447 TRUE: iterates each element as a separate root
			var s = RdfSerializer.create().ntriple().looseCollections().build();
			var b1 = new NamedBean();
			b1.setName("a");
			var b2 = new NamedBean();
			b2.setName("b");
			var result = s.serialize(new ArrayList<>(List.of(b1, b2)));
			assertNotNull(result);
		}

		@Test void m06_serialize_loose_collections_array() throws Exception {
			// looseCollections() + array type → doSerialize line 448 isCollection()=FALSE → toList()
			var s = RdfSerializer.create().ntriple().looseCollections().build();
			var b1 = new NamedBean();
			b1.setName("x");
			var b2 = new NamedBean();
			b2.setName("y");
			var result = s.serialize(new NamedBean[]{b1, b2});
			assertNotNull(result);
		}

		@Test void m07_serialize_add_literal_types() throws Exception {
			// isAddLiteralTypes() TRUE → serializeAnything line 325: n = m.createTypedLiteral(o)
			var s = RdfSerializer.create().ntriple().addLiteralTypes().build();
			var result = s.serialize(42);
			assertNotNull(result);
		}

		@Test void m08_serialize_add_root_property() throws Exception {
			// isAddRootProp() TRUE → doSerialize line 460-461: r.addProperty(pRoot,"true")
			var s = RdfSerializer.create().ntriple().addRootProperty().build();
			var result = s.serialize("hello");
			assertNotNull(result);
		}

		@Test void m09_serialize_non_absolute_uri_as_literal() throws Exception {
			// isUri() but URI is not absolute (relative) → serializeAnything line 268-269:
			// isAbsoluteUri() FALSE → createLiteral(encodeTextInvalidChars(uri))
			var s = RdfSerializer.create().ntriple().build();
			@Uri
			class RelativeUriBean {
				public String getPath() { return "relative/path"; }
			}
			var result = s.serialize(new RelativeUriBean().getPath());
			assertNotNull(result);
		}

		@Test void m10_serialize_null_char_value() throws Exception {
			// char '\0' → serializeAnything line 254 TRUE branch: sType.isChar() && charValue==0
			// → null branch (bpm==null here) → n = createResource(RDF_NIL)
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize('\0');
			assertNotNull(result);
		}

		@Test void m11_serialize_rdf_namespace_no_autodetect() throws Exception {
			// Bean with @Rdf(prefix+namespace) on class → bpRdf's underlying @Rdf(on-class) produces non-null ns
			// in findNamespace →  serializeBeanMap: ns set from class @Rdf → first if FALSE, second if FALSE
			// disableAutoDetectNamespaces() → else-if isAutoDetectNamespaces()=FALSE → else-if skipped
			var s = RdfSerializer.create().ntriple().disableAutoDetectNamespaces().build();
			var result = s.serialize(new M11_BeanWithRdfNs());
			assertNotNull(result);
		}

		@Rdf(prefix = "m11", namespace = "http://example.org/m11/")
		public static class M11_BeanWithRdfNs {
			public String label = "ns-test";
		}

		@Test void m12_serialize_rdf_property_namespace() throws Exception {
			// Property with @Rdf(prefix+namespace) → ns set from bpRdf.getNamespace() (non-null) →
			// first if-condition(ns==null) FALSE → goes to else-if isAutoDetectNamespaces()=TRUE →
			// addModelPrefix(ns) called (covers the else-if TRUE branch in lambda$4)
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize(new M12_BeanWithPropNs());
			assertNotNull(result);
		}

		public static class M12_BeanWithPropNs {
			@Rdf(prefix = "m12", namespace = "http://example.org/m12/")
			public String label = "propns";
		}

		@Test void m13_serialize_multivalue_with_rdf_property_ns() throws Exception {
			// MULTI_VALUED collection + @Rdf(prefix+namespace) on property → serializeToMultiProperties:
			// ns = bpRdf.getNamespace() (non-null) → first if FALSE → else-if isAutoDetectNamespaces() →
			// addModelPrefix(ns) called in lambda$8
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize(new M13_BeanWithMultiNs());
			assertNotNull(result);
		}

		public static class M13_BeanWithMultiNs {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED, prefix = "m13", namespace = "http://example.org/m13/")
			public List<String> tags = new ArrayList<>(List.of("a", "b"));
		}

		@Test void m14_serialize_loose_collections_non_collection() throws Exception {
			// looseCollections=true, o is NOT a collection → compound condition [T,T,F] (isCollectionOrArray=false)
			// → falls through to else branch in doSerialize
			var s = RdfSerializer.create().ntriple().looseCollections().build();
			var b = new NamedBean();
			b.setName("loose-bean");
			var result = s.serialize(b);
			assertNotNull(result);
		}
	}

	@Nested class L_serializerSessionBranchFills extends TestBase {

		@Test void l01_serialize_optional() throws Exception {
			// Serialize Optional<String> — covers isOptional(aType) branch (line 220) in serializeAnything
			var s = RdfSerializer.create().ntriple().build();
			var result = s.serialize(opt("opt-value"));
			assertNotNull(result);
			assertFalse(result.isBlank());
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Test void l02_serialize_delegate_list() throws Exception {
			// Serialize DelegateList — covers isDelegate() branch (line 228) in serializeAnything
			// DelegateList implements Delegate<T>; its wType is the wrapped collection type
			var s = RdfSerializer.create().ntriple().build();
			var ctx = s.getMarshallingContext();
			var cm = (ClassMeta)ctx.getClassMeta(ArrayList.class);
			var dl = new DelegateList(cm);
			dl.add("alpha");
			dl.add("beta");
			var result = s.serialize(dl);
			assertNotNull(result);
			assertFalse(result.isBlank());
		}
	}
}
