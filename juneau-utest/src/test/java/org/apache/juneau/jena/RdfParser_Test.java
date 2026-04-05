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
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;

class RdfParser_Test extends TestBase {

	public static class NamedBean {
		private String name;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
	}

	@Nested class A_builderSettings extends TestBase {

		@Test void a01_language_ntriple() {
			var x = RdfParser.create().language("N-TRIPLE").build();
			assertEquals("N-TRIPLE", x.getLanguage());
		}

		@Test void a02_language_turtle() {
			var x = RdfParser.create().language("TURTLE").build();
			assertEquals("TURTLE", x.getLanguage());
		}

		@Test void a03_language_rdfxml() {
			var x = RdfParser.create().xml().build();
			assertEquals("RDF/XML", x.getLanguage());
		}

		@Test void a04_language_xmlabbrev() {
			var x = RdfParser.create().xmlabbrev().build();
			assertEquals("RDF/XML-ABBREV", x.getLanguage());
		}

		@Test void a05_language_n3() {
			var x = RdfParser.create().n3().build();
			assertEquals("N3", x.getLanguage());
		}

		@Test void a06_language_nquads() {
			var x = RdfParser.create().nQuads().build();
			assertEquals("N-QUADS", x.getLanguage());
		}

		@Test void a07_language_trig() {
			var x = RdfParser.create().triG().build();
			assertEquals("TRIG", x.getLanguage());
		}

		@Test void a08_language_trix() {
			var x = RdfParser.create().triX().build();
			assertEquals("TRIX", x.getLanguage());
		}

		@Test void a09_language_turtle_shortcut() {
			var x = RdfParser.create().turtle().build();
			assertEquals("TURTLE", x.getLanguage());
		}

		@Test void a10_language_jsonld() {
			var x = RdfParser.create().jsonLd().build();
			assertEquals("JSON-LD", x.getLanguage());
		}

		@Test void a11_language_rdfjson() {
			var x = RdfParser.create().rdfJson().build();
			assertEquals("RDF/JSON", x.getLanguage());
		}

		@Test void a12_collectionFormat_bag() {
			var x = RdfParser.create().collectionFormat(RdfCollectionFormat.BAG).build();
			assertEquals(RdfCollectionFormat.BAG, x.getCollectionFormat());
		}

		@Test void a13_collectionFormat_seq() {
			var x = RdfParser.create().collectionFormat(RdfCollectionFormat.SEQ).build();
			assertEquals(RdfCollectionFormat.SEQ, x.getCollectionFormat());
		}

		@Test void a14_looseCollections() {
			var x1 = RdfParser.create().looseCollections().build();
			assertTrue(x1.isLooseCollections());
			var x2 = RdfParser.create().looseCollections(false).build();
			assertFalse(x2.isLooseCollections());
		}

		@Test void a15_trimWhitespace() {
			var x1 = RdfParser.create().trimWhitespace().build();
			assertTrue(x1.isTrimWhitespace());
			var x2 = RdfParser.create().trimWhitespace(false).build();
			assertFalse(x2.isTrimWhitespace());
		}

		@Test void a16_juneauNs() {
			var ns = Namespace.of("myj", "http://myjuneau/");
			var x = RdfParser.create().juneauNs(ns).build();
			assertEquals(ns, x.getJuneauNs());
		}

		@Test void a17_juneauBpNs() {
			var ns = Namespace.of("mybp", "http://mybp/");
			var x = RdfParser.create().juneauBpNs(ns).build();
			assertEquals(ns, x.getJuneauBpNs());
		}

		@Test void a18_jenaSettings() {
			var x = RdfParser.create().jena("jena.prop", "value").build();
			assertEquals("value", x.getJenaSettings().get("jena.prop"));
		}

		@Test void a19_unbuffered() {
			var x1 = RdfParser.create().unbuffered().build();
			assertNotNull(x1);
			var x2 = RdfParser.create().unbuffered(false).build();
			assertNotNull(x2);
		}

		@Test void a20_useEnumNames() {
			var x = RdfParser.create().useEnumNames().build();
			assertNotNull(x);
		}

		@Test void a21_useJavaBeanIntrospector() {
			var x = RdfParser.create().useJavaBeanIntrospector().build();
			assertNotNull(x);
		}

		@Test void a22_timeZone() {
			var tz = TimeZone.getTimeZone("America/New_York");
			var x = RdfParser.create().timeZone(tz).build();
			assertNotNull(x);
		}

		@Test void a23_typeName() {
			var x = RdfParser.create().typeName(String.class, "myString").build();
			assertNotNull(x);
		}

		@Test void a24_typePropertyName_onClass() {
			var x = RdfParser.create().typePropertyName(String.class, "_mytype").build();
			assertNotNull(x);
		}

		@Test void a25_swap_twoFunction() {
			// The 3-param swap always throws since unswapFunction is required
			assertThrows(IllegalArgumentException.class, () ->
				RdfParser.create().swap(Integer.class, String.class, i -> String.valueOf(i)).build()
			);
		}

		@Test void a26_swap_fourFunction() {
			var x = RdfParser.create()
				.swap(Integer.class, String.class, i -> String.valueOf(i), s -> Integer.parseInt(s))
				.build();
			assertNotNull(x);
		}

		@Test void a27_copy() {
			var b = RdfParser.create().language("TURTLE").looseCollections();
			var copy = b.copy();
			var x = copy.build();
			assertEquals("TURTLE", x.getLanguage());
			assertTrue(x.isLooseCollections());
		}

		@Test void a28_copy_fromParser() {
			var p = RdfParser.create().language("N-TRIPLE").trimWhitespace().build();
			var copy = p.copy();
			var x = copy.build();
			assertEquals("N-TRIPLE", x.getLanguage());
			assertTrue(x.isTrimWhitespace());
		}
	}

	@Nested class B_properties extends TestBase {

		@Test void b01_toString_includes_language() {
			var x = RdfParser.create().language("TURTLE").build();
			var str = x.toString();
			assertNotNull(str);
			assertTrue(str.contains("TURTLE"));
		}

		@Test void b02_getJenaSettings_empty_by_default() {
			var x = RdfParser.create().build();
			assertNotNull(x.getJenaSettings());
		}

		@Test void b03_getCollectionFormat_default() {
			var x = RdfParser.create().build();
			assertEquals(RdfCollectionFormat.DEFAULT, x.getCollectionFormat());
		}

		@Test void b04_isLooseCollections_default() {
			var x = RdfParser.create().build();
			assertFalse(x.isLooseCollections());
		}

		@Test void b05_isTrimWhitespace_default() {
			var x = RdfParser.create().build();
			assertFalse(x.isTrimWhitespace());
		}
	}

	@Nested class C_parsing extends TestBase {

		@Test void c01_parse_string_ntriple() throws Exception {
			var serialized = RdfSerializer.create().language("N-TRIPLE").build().serialize("foo");
			var p = RdfParser.create().language("N-TRIPLE").build();
			var result = p.parse(serialized, String.class);
			assertEquals("foo", result);
		}

		@Test void c02_parse_string_turtle() throws Exception {
			var serialized = RdfSerializer.create().language("TURTLE").build().serialize("foo");
			var p = RdfParser.create().language("TURTLE").build();
			var result = p.parse(serialized, String.class);
			assertEquals("foo", result);
		}

		@Test void c03_parse_string_rdfxml() throws Exception {
			var serialized = RdfSerializer.create().language("RDF/XML").build().serialize("foo");
			var p = RdfParser.create().language("RDF/XML").build();
			var result = p.parse(serialized, String.class);
			assertEquals("foo", result);
		}

		@Test void c04_parse_string_n3() throws Exception {
			var serialized = RdfSerializer.create().language("N3").build().serialize("foo");
			var p = RdfParser.create().language("N3").build();
			var result = p.parse(serialized, String.class);
			assertEquals("foo", result);
		}
	}

	@Nested class D_inheritedBuilderMethods extends TestBase {

		@Bean(typeName = "D01_DictBean")
		static class D01_DictBean {}

		static class D10_TestParserListener extends ParserListener {}

		@SuppressWarnings({
			"unchecked", // Raw type needed for beanInterceptor test
			"rawtypes" // (Class) cast required by beanInterceptor API
		})
		@Test void d01_beanVisibilityAndContext() {
			assertNotNull(RdfParser.create().beanClassVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfParser.create().beanConstructorVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfParser.create().beanFieldVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfParser.create().beanMethodVisibility(Visibility.PUBLIC).build());
			assertNotNull(RdfParser.create().beanContext(BeanContext.DEFAULT).build());
			assertNotNull(RdfParser.create().beanContext(BeanContext.create()).build());
			assertNotNull(RdfParser.create().beanDictionary(D01_DictBean.class).build());
			assertNotNull(RdfParser.create().beanInterceptor(String.class, (Class)BeanInterceptor.class).build());
			assertNotNull(RdfParser.create().beanMapPutReturnsOldValue().build());
		}

		@Test void d02_beanProperties() {
			assertNotNull(RdfParser.create().beanProperties(String.class, "foo").build());
			assertNotNull(RdfParser.create().beanProperties(Map.of("String", "foo")).build());
			assertNotNull(RdfParser.create().beanProperties("java.lang.String", "foo").build());
			assertNotNull(RdfParser.create().beanPropertiesExcludes(String.class, "foo").build());
			assertNotNull(RdfParser.create().beanPropertiesExcludes(Map.of("String", "foo")).build());
			assertNotNull(RdfParser.create().beanPropertiesExcludes("java.lang.String", "foo").build());
			assertNotNull(RdfParser.create().beanPropertiesReadOnly(String.class, "foo").build());
			assertNotNull(RdfParser.create().beanPropertiesReadOnly(Map.of("String", "foo")).build());
			assertNotNull(RdfParser.create().beanPropertiesReadOnly("java.lang.String", "foo").build());
			assertNotNull(RdfParser.create().beanPropertiesWriteOnly(String.class, "foo").build());
			assertNotNull(RdfParser.create().beanPropertiesWriteOnly(Map.of("String", "foo")).build());
			assertNotNull(RdfParser.create().beanPropertiesWriteOnly("java.lang.String", "foo").build());
		}

		@Test void d03_beansRequire() {
			assertNotNull(RdfParser.create().beansRequireDefaultConstructor().build());
			assertNotNull(RdfParser.create().beansRequireSerializable().build());
			assertNotNull(RdfParser.create().beansRequireSettersForGetters().build());
		}

		@Test void d04_autoCloseAndConsumes() {
			assertNotNull(RdfParser.create().autoCloseStreams().build());
			assertNotNull(RdfParser.create().autoCloseStreams(false).build());
			assertNotNull(RdfParser.create().consumes("text/xml+rdf").build());
		}

		@Test void d05_debug() {
			assertNotNull(RdfParser.create().debug().build());
			assertNotNull(RdfParser.create().debug(false).build());
			assertNotNull(RdfParser.create().debugOutputLines(5).build());
		}

		@Test void d06_dictionaryAndDisable() {
			assertNotNull(RdfParser.create().dictionaryOn(String.class, String.class).build());
			assertNotNull(RdfParser.create().disableBeansRequireSomeProperties().build());
			assertNotNull(RdfParser.create().disableIgnoreMissingSetters().build());
			assertNotNull(RdfParser.create().disableIgnoreTransientFields().build());
			assertNotNull(RdfParser.create().disableIgnoreUnknownNullBeanProperties().build());
			assertNotNull(RdfParser.create().disableInterfaceProxies().build());
		}

		@Test void d07_exampleAndCharsets() {
			assertNotNull(RdfParser.create().example(Integer.class, "42").build());
			assertNotNull(RdfParser.create().example(Integer.class, 42).build());
			assertNotNull(RdfParser.create().fileCharset(java.nio.charset.StandardCharsets.UTF_8).build());
			assertNotNull(RdfParser.create().streamCharset(java.nio.charset.StandardCharsets.UTF_8).build());
		}

		@Test void d08_findFluentSetters() {
			assertNotNull(RdfParser.create().findFluentSetters().build());
			assertNotNull(RdfParser.create().findFluentSetters(String.class).build());
		}

		@Test void d09_ignoreAndImpl() {
			assertNotNull(RdfParser.create().ignoreInvocationExceptionsOnGetters().build());
			assertNotNull(RdfParser.create().ignoreInvocationExceptionsOnSetters().build());
			assertNotNull(RdfParser.create().ignoreUnknownBeanProperties().build());
			assertNotNull(RdfParser.create().ignoreUnknownEnumValues().build());
			assertNotNull(RdfParser.create().impl(RdfParser.create().build()).build());
			assertNotNull(RdfParser.create().implClass(List.class, ArrayList.class).build());
			var implMap = new HashMap<Class<?>,Class<?>>();
			implMap.put(List.class, ArrayList.class);
			assertNotNull(RdfParser.create().implClasses(implMap).build());
			assertNotNull(RdfParser.create().interfaceClass(List.class, List.class).build());
			assertNotNull(RdfParser.create().interfaces(List.class).build());
		}

		@Test void d10_listenerLocaleMediaType() {
			assertNotNull(RdfParser.create().listener(D10_TestParserListener.class).build());
			assertNotNull(RdfParser.create().locale(Locale.ENGLISH).build());
			assertNotNull(RdfParser.create().mediaType(org.apache.juneau.MediaType.of("text/xml+rdf")).build());
		}

		@Test void d11_notBeanAndPropertyNamer() {
			assertNotNull(RdfParser.create().notBeanClasses(String.class).build());
			assertNotNull(RdfParser.create().notBeanPackages("java.lang").build());
			assertNotNull(RdfParser.create().propertyNamer(String.class, PropertyNamerDLC.class).build());
			assertNotNull(RdfParser.create().propertyNamer(PropertyNamerDLC.class).build());
		}

		@Test void d12_languageShortcutAndN3() {
			assertNotNull(RdfParser.create().ntriple().build());
			assertNotNull(RdfParser.create().n3_disableAbbrevBaseUri().build());
			assertNotNull(RdfParser.create().n3_disableAbbrevBaseUri(false).build());
			assertNotNull(RdfParser.create().n3_disableObjectLists().build());
			assertNotNull(RdfParser.create().n3_disableObjectLists(false).build());
			assertNotNull(RdfParser.create().n3_disableUseDoubles().build());
			assertNotNull(RdfParser.create().n3_disableUseDoubles(false).build());
			assertNotNull(RdfParser.create().n3_disableUsePropertySymbols().build());
			assertNotNull(RdfParser.create().n3_disableUsePropertySymbols(false).build());
			assertNotNull(RdfParser.create().n3_disableUseTripleQuotedStrings().build());
			assertNotNull(RdfParser.create().n3_disableUseTripleQuotedStrings(false).build());
			assertNotNull(RdfParser.create().n3_indentProperty(4).build());
			assertNotNull(RdfParser.create().n3_minGap(1).build());
			assertNotNull(RdfParser.create().n3_propertyColumn(8).build());
			assertNotNull(RdfParser.create().n3_subjectColumn(8).build());
			assertNotNull(RdfParser.create().n3_widePropertyLen(20).build());
		}

		@Test void d13_rdfXmlSettings() {
			assertNotNull(RdfParser.create().rdfxml_allowBadUris().build());
			assertNotNull(RdfParser.create().rdfxml_allowBadUris(false).build());
			assertNotNull(RdfParser.create().rdfxml_attributeQuoteChar('"').build());
			assertNotNull(RdfParser.create().rdfxml_blockRules("").build());
			assertNotNull(RdfParser.create().rdfxml_disableShowDoctypeDeclaration().build());
			assertNotNull(RdfParser.create().rdfxml_disableShowDoctypeDeclaration(false).build());
			assertNotNull(RdfParser.create().rdfxml_embedding().build());
			assertNotNull(RdfParser.create().rdfxml_embedding(false).build());
			assertNotNull(RdfParser.create().rdfxml_errorMode("default").build());
			assertNotNull(RdfParser.create().rdfxml_iriRules("lax").build());
			assertNotNull(RdfParser.create().rdfxml_longId().build());
			assertNotNull(RdfParser.create().rdfxml_longId(false).build());
			assertNotNull(RdfParser.create().rdfxml_relativeUris("").build());
			assertNotNull(RdfParser.create().rdfxml_showXmlDeclaration("false").build());
			assertNotNull(RdfParser.create().rdfxml_tab(4).build());
			assertNotNull(RdfParser.create().rdfxml_xmlbase("http://example.org/").build());
		}

		@Test void d14_sortStopAndOther() {
			assertNotNull(RdfParser.create().sortProperties().build());
			assertNotNull(RdfParser.create().sortProperties(String.class).build());
			assertNotNull(RdfParser.create().stopClass(String.class, Object.class).build());
			assertNotNull(RdfParser.create().swaps(new Class<?>[0]).build());
			assertNotNull(RdfParser.create().swaps(new Object[0]).build());
			assertNotNull(RdfParser.create().trimStrings().build());
			assertNotNull(RdfParser.create().trimStrings(false).build());
			assertNotNull(RdfParser.create().type(RdfParser.class).build());
			assertNotNull(RdfParser.create().typePropertyName("_type").build());
		}

		@Test void d15_n3SubLanguages_consumes() {
			// Cover the switch cases for N3-PP, N3-PLAIN, N3-TRIPLES in getConsumes()
			assertNotNull(RdfParser.create().language("N3-PP").build());
			assertNotNull(RdfParser.create().language("N3-PLAIN").build());
			assertNotNull(RdfParser.create().language("N3-TRIPLES").build());
		}

		@Test void d16_sessionBuilderMethods() {
			// Cover RdfParserSession.Builder fluent methods
			var p = RdfParser.create().build();
			var sb = p.createSession();
			assertNotNull(sb.apply(String.class, x -> {}).build());
			assertNotNull(p.createSession().debug(false).build());
			assertNotNull(p.createSession().javaMethod(null).build());
			assertNotNull(p.createSession().locale(Locale.US).build());
			assertNotNull(p.createSession().fileCharset(java.nio.charset.Charset.defaultCharset()).build());
			assertNotNull(p.createSession().mediaType(org.apache.juneau.MediaType.JSON).build());
		}

		@Test void d17_xmlMetaProviderMethods() {
			// Cover getXmlBeanMeta and getXmlBeanPropertyMeta methods in RdfParser
			var p = RdfParser.create().build();
			var bc = p.getBeanContext();
			var bm = bc.getBeanMeta(NamedBean.class);
			assertNotNull(bm);
			assertNotNull(p.getXmlBeanMeta(bm));
			var bpm = bm.getPropertyMeta("name");
			assertNotNull(bpm);
			assertNotNull(p.getXmlBeanPropertyMeta(bpm));
			assertNotNull(p.getXmlClassMeta(bc.getClassMeta(String.class)));
		}

		@Test void d18_swapBuilderMethod() {
			// Cover swap(normalClass, swappedClass, swapFunction) with 3 args
			assertNotNull(RdfParser.create().swap(String.class, Integer.class, x -> Integer.parseInt(x), x -> x.toString()).build());
		}
	}
}
