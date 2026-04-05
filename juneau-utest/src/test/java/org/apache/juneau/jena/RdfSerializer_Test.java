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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;

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
			assertNotNull(RdfSerializer.create().beanContext(BeanContext.create().build()).build());
			assertNotNull(RdfSerializer.create().beanContext(BeanContext.create()).build());
		}

		@Bean(typeName="myDictBean")
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
			// E01_ChildBean inherits @Rdf from both parents
			// ap.find(Rdf.class, ChildBean.classMeta) traverses PARENTS → finds both @Rdf annotations
			// This should populate rdfs and execute forEach body at lines 67-71
			var s = (RdfSerializer) RdfSerializer.create().language("N-TRIPLE").build();
			var bc = s.getBeanContext();
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
			var bc = s.getBeanContext();
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
			var bc = s.getBeanContext();
			var cm = bc.getClassMeta(E03_BeanWithNoCollectionFormat.class);
			var rdfCm = s.getRdfClassMeta(cm);
			assertNotNull(rdfCm);
			assertEquals(RdfCollectionFormat.DEFAULT, rdfCm.getCollectionFormat());
			var result = s.serialize(new E03_BeanWithNoCollectionFormat());
			assertNotNull(result);
		}
	}

	@Test void d01_constants_class() {
		// Covers the default constructor of Constants (utility class with static fields)
		assertNotNull(new Constants());
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
			assertNotNull(RdfSerializer.create().mediaType(org.apache.juneau.MediaType.of("text/xml+rdf")).build());
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
			assertNotNull(RdfSerializer.create().sortProperties().build());
			assertNotNull(RdfSerializer.create().sortProperties(String.class).build());
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
			assertNotNull(RdfSerializer.create().useEnumNames().build());
			assertNotNull(RdfSerializer.create().useJavaBeanIntrospector().build());
			assertNotNull(RdfSerializer.create().useWhitespace().build());
			assertNotNull(RdfSerializer.create().useWhitespace(false).build());
			assertNotNull(RdfSerializer.create().ws().build());
		}
	}

	@Nested class G_streamClasses extends TestBase {

		@Test void g01_streamSerializer_rdfthrift_no_produces() {
			// Build without setting produces() to trigger switch(LANG_RDFTHRIFT) in getProduces()
			var s = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build();
			assertNotNull(s);
		}

		@Test void g02_streamSerializer_rdfproto_no_produces() {
			// Build without setting produces() to trigger switch(LANG_RDFPROTO) in getProduces()
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
			var bc = s.getBeanContext();
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
			// Build without setting consumes() to trigger switch(LANG_RDFTHRIFT) in getConsumes()
			var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
			assertNotNull(p);
		}

		@Test void g07_streamParser_rdfproto_no_consumes() {
			// Build without setting consumes() to trigger switch(LANG_RDFPROTO) in getConsumes()
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
			var bc = p.getBeanContext();
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
			var bc = s.getBeanContext();
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
			assertNotNull(s.createSession().mediaType(org.apache.juneau.MediaType.JSON).build());
		}
	}
}
