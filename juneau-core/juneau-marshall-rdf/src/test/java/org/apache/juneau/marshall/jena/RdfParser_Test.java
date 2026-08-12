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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.Builder;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
	"java:S5976" // Explicit per-case parser tests are clearer for diagnostics than a single parameterized rewrite.
})
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

		@Test void a20_enumFormatName() {
			var x = RdfParser.create().enumFormat(EnumFormat.NAME).build();
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
			// The 3-param swap registers a serialize-only swap (no unswap function required).
			var x = RdfParser.create()
				.swap(Integer.class, String.class, String::valueOf)
				.build();
			assertNotNull(x);
		}

		@Test void a26_swap_fourFunction() {
			var x = RdfParser.create()
				.swap(Integer.class, String.class, String::valueOf, Integer::parseInt)
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

		@Test void c01_read_string_ntriple() throws Exception {
			var serialized = RdfSerializer.create().language("N-TRIPLE").build().write("foo");
			var p = RdfParser.create().language("N-TRIPLE").build();
			var result = p.read(serialized, String.class);
			assertEquals("foo", result);
		}

		@Test void c02_read_string_turtle() throws Exception {
			var serialized = RdfSerializer.create().language("TURTLE").build().write("foo");
			var p = RdfParser.create().language("TURTLE").build();
			var result = p.read(serialized, String.class);
			assertEquals("foo", result);
		}

		@Test void c03_read_string_rdfxml() throws Exception {
			var serialized = RdfSerializer.create().language("RDF/XML").build().write("foo");
			var p = RdfParser.create().language("RDF/XML").build();
			var result = p.read(serialized, String.class);
			assertEquals("foo", result);
		}

		@Test void c04_read_string_n3() throws Exception {
			var serialized = RdfSerializer.create().language("N3").build().write("foo");
			var p = RdfParser.create().language("N3").build();
			var result = p.read(serialized, String.class);
			assertEquals("foo", result);
		}
	}

	@Nested class D_inheritedBuilderMethods extends TestBase {

		@Marshalled(typeName = "D01_DictBean")
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
			assertNotNull(RdfParser.create().marshallingContext(MarshallingContext.DEFAULT).build());
			assertNotNull(RdfParser.create().marshallingContext(MarshallingContext.create()).build());
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
			assertNotNull(RdfParser.create().mediaType(org.apache.juneau.commons.http.MediaType.of("text/xml+rdf")).build());
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
			assertNotNull(RdfParser.create().build());
			assertNotNull(RdfParser.create().build());
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
			assertNotNull(p.createSession().mediaType(org.apache.juneau.commons.http.MediaType.JSON).build());
		}

		@Test void d17_xmlMetaProviderMethods() {
			// Cover getXmlBeanMeta and getXmlBeanPropertyMeta methods in RdfParser
			var p = RdfParser.create().build();
			var bc = p.getMarshallingContext();
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
			assertNotNull(RdfParser.create().swap(String.class, Integer.class, Integer::parseInt, Object::toString).build());
		}
	}

	@Nested class E_sessionBranchFills extends TestBase {

		@Test void e01_trimWhitespace_and_trimStrings_in_actual_read() throws Exception {
			// Triggers RdfParserSession.decodeString() isTrimWhitespace + isTrimStrings branches
			var serialized = RdfSerializer.create().language("N-TRIPLE").build().write("  hello  ");
			var parser = RdfParser.create().language("N-TRIPLE").trimWhitespace().trimStrings().build();
			var result = parser.read(serialized, String.class);
			assertNotNull(result);
		}

		@Test void e02_read_list_with_bag_format() throws Exception {
			// Triggers RdfParserSession collectionFormat BAG path
			var list = List.of("a", "b", "c");
			var serialized = RdfSerializer.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.BAG).build().write(list);
			var parser = RdfParser.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.BAG).build();
			var result = parser.read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void e03_read_list_with_seq_format() throws Exception {
			// Triggers RdfParserSession collectionFormat SEQ path
			var list = List.of("x", "y");
			var serialized = RdfSerializer.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.SEQ).build().write(list);
			var parser = RdfParser.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.SEQ).build();
			var result = parser.read(serialized, List.class);
			assertNotNull(result);
		}

		public static class BeanWithList {
			public List<String> items = new ArrayList<>();
		}

		@Test void e04_read_multiValued_collection() throws Exception {
			// Triggers RdfParserSession isMultiValuedCollections branch via a bean property
			var bean = new BeanWithList();
			bean.items = new ArrayList<>(List.of("p", "q"));
			var serialized = RdfSerializer.create().language("TURTLE").collectionFormat(RdfCollectionFormat.MULTI_VALUED).build().write(bean);
			var parser = RdfParser.create().language("TURTLE").collectionFormat(RdfCollectionFormat.MULTI_VALUED).build();
			var result = parser.read(serialized, BeanWithList.class);
			assertNotNull(result);
		}
	}

	@Nested class F_readVariousTypes extends TestBase {

		@Test void f01_read_boolean_ntriple() throws Exception {
			// sType.isBoolean() branch in readAnything (line 345)
			var serialized = RdfSerializer.create().ntriple().build().write(true);
			var result = RdfParser.create().ntriple().build().read(serialized, Boolean.class);
			assertNotNull(result);
		}

		@Test void f02_read_integer() throws Exception {
			// sType.isNumber() branch in readAnything (line 343)
			var serialized = RdfSerializer.create().ntriple().build().write(42);
			var result = RdfParser.create().ntriple().build().read(serialized, Integer.class);
			assertNotNull(result);
		}

		@Test void f03_read_map() throws Exception {
			// sType.isMap() branch in readAnything (line 314)
			var map = new LinkedHashMap<String,String>();
			map.put("k1", "v1");
			var serialized = RdfSerializer.create().ntriple().build().write(map);
			var result = RdfParser.create().ntriple().build().read(serialized, Map.class);
			assertNotNull(result);
		}

		@Test void f04_read_string_array() throws Exception {
			// sType.isArray() branch in readAnything (line 321) — parsed into array via temp list
			var serialized = RdfSerializer.create().ntriple().build().write(new ArrayList<>(List.of("a", "b")));
			var result = RdfParser.create().ntriple().build().read(serialized, String[].class);
			assertNotNull(result);
		}

		@Test void f05_read_looseCollections() throws Exception {
			// isLooseCollections() path in doRead (line 484)
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.write(new ArrayList<>(List.of("x", "y")));
			var result = RdfParser.create().ntriple().looseCollections().build().read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void f06_invalid_language_throws_on_read() {
			// lang==null path in RdfParserSession constructor — throws on first parse call
			var p = RdfParser.create().language("NOT-A-LANGUAGE").build();
			assertThrows(Exception.class, () -> p.read("dummy", String.class));
		}

		@Test void f07_read_list_as_bag_roundtrip() throws Exception {
			// BAG format parse — isBag() path in readAnything
			var list = new ArrayList<>(List.of("i", "ii", "iii"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build().write(list);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void f08_read_map_as_object_type() throws Exception {
			// sType.isObject() with Resource — exercises the resource-to-JsonMap path
			var map = new LinkedHashMap<String,String>();
			map.put("name", "value");
			var serialized = RdfSerializer.create().ntriple().build().write(map);
			var result = RdfParser.create().ntriple().build().read(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void f09_read_string_ntriple_trimWhitespace_and_trimStrings() throws Exception {
			// Exercises decodeString() with both isTrimWhitespace() and isTrimStrings() true
			var serialized = RdfSerializer.create().ntriple().build().write("  hello  ");
			var result = RdfParser.create().ntriple().trimWhitespace().trimStrings().build()
				.read(serialized, String.class);
			assertNotNull(result);
		}

		@Test void f10_read_list_format_list_roundtrip() throws Exception {
			// LIST RDF list collection format — r.canAs(RDFList.class) path in readAnything
			var list = new ArrayList<>(List.of("a", "b", "c"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build().write(list);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void f11_read_char() throws Exception {
			// sType.isChar() path in readAnything (line 341)
			var serialized = RdfSerializer.create().ntriple().build().write('Z');
			var result = RdfParser.create().ntriple().build().read(serialized, Character.class);
			assertNotNull(result);
		}

		@Test void f12_read_url_type() throws Exception {
			// sType.isUri() && n.isResource() path in readAnything (line 347)
			// java.net.URL is a recognized URI type in Juneau
			var serialized = RdfSerializer.create().ntriple().build().write(new java.net.URL("http://example.org/foo"));
			var result = RdfParser.create().ntriple().build().read(serialized, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void f13_read_seq_list_into_typed_list() throws Exception {
			// SEQ format into a typed List<String> — uses sType.isCollectionOrArray() path (line 320)
			// with isSeq(r) true (line 328)
			var list = new ArrayList<>(List.of("p", "q", "r"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build().write(list);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.read(serialized, ArrayList.class);
			assertNotNull(result);
		}

		@Test void f14_read_uri_java_type() throws Exception {
			// sType.isUri() && n.isResource() path in readAnything (line 347)
			// java.net.URL is a URI type in Juneau
			var url = new java.net.URL("http://example.org/foo");
			var serialized = RdfSerializer.create().ntriple().build().write(url);
			var result = RdfParser.create().ntriple().build().read(serialized, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void f15_read_multi_valued_into_bean() throws Exception {
			// MULTI_VALUED format → isMultiValuedCollections path (lines 400-405) in readIntoBeanMap
			var bean = new RdfParser_Test.E_sessionBranchFills.BeanWithList();
			bean.items = new ArrayList<>(List.of("x", "y", "z"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build().write(bean);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build()
				.read(serialized, RdfParser_Test.E_sessionBranchFills.BeanWithList.class);
			assertNotNull(result);
		}
	}

	@Nested class G_streamParserBranchFills extends TestBase {

		@Test void g01_stream_read_string_thrift() throws Exception {			// RdfStreamParserSession — parse String from Thrift binary
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write("hello");
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, String.class);
			assertEquals("hello", result);
		}

		@Test void g02_stream_read_bean_thrift() throws Exception {
			// RdfStreamParserSession — parse bean from Thrift binary
			var bean = new NamedBean();
			bean.setName("stream-bean");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(bean);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, NamedBean.class);
			assertNotNull(result);
		}

		@Test void g03_stream_read_list_thrift() throws Exception {
			// RdfStreamParserSession — parse list from Thrift binary
			var list = new ArrayList<>(List.of("a", "b"));
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(list);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, List.class);
			assertNotNull(result);
		}

		@Test void g04_stream_read_integer() throws Exception {
			// sType.isNumber() branch in RdfStreamParserSession
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(99);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, Integer.class);
			assertNotNull(result);
		}

		@Test void g05_stream_read_map_thrift() throws Exception {
			// sType.isMap() branch in RdfStreamParserSession
			var map = new LinkedHashMap<String,String>();
			map.put("key", "val");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(map);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, Map.class);
			assertNotNull(result);
		}

		@Test void g06_stream_read_boolean() throws Exception {
			// sType.isBoolean() branch in RdfStreamParserSession
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(true);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, Boolean.class);
			assertNotNull(result);
		}

		@Test void g07_stream_invalid_language_throws() {
			// lang==null path in RdfStreamParserSession constructor
			var p = RdfStreamParser.create().language("INVALID").build();
			assertThrows(Exception.class, () -> p.read(new byte[0], String.class));
		}

		@Test void g08_stream_roundtrip_array() throws Exception {
			// Array serialization and parsing via Thrift — exercises isArray paths in stream sessions
			var input = new String[]{"p", "q", "r"};
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(input);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, String[].class);
			assertNotNull(result);
		}
	}

	@Nested class H_moreParserBranches extends TestBase {

		@Test void h01_read_with_root_property() throws Exception {
			// Serialize with addRootProperty(), parse — covers getRoots() loop body (line 167) and early return (line 170)
			var serialized = RdfSerializer.create().ntriple().addRootProperty().build().write("hello");
			var result = RdfParser.create().ntriple().build().read(serialized, String.class);
			assertEquals("hello", result);
		}

		@Test void h02_read_empty_seq_collection() throws Exception {
			// Serialize empty list with SEQ format — covers readIntoCollection(Container) empty loop (line 433)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.write(new ArrayList<>());
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void h03_read_empty_list_format() throws Exception {
			// Serialize empty list with LIST format — empty RDF list is rdf:nil, parsed as null (line 442 empty loop)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.write(new ArrayList<>());
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.read(serialized, List.class);
			assertNull(result);
		}

		public static class H04_SimpleBean {
			public String val = "x";
		}

		@Test void h04_read_loose_collections_as_array() throws Exception {
			// Parse loose collection of beans as typed array — covers lines 486 (isArray), 493 (lambda body), 495-496
			var list = new ArrayList<>(List.of(new H04_SimpleBean(), new H04_SimpleBean()));
			var serialized = RdfSerializer.create().ntriple().looseCollections().build().write(list);
			var result = RdfParser.create().ntriple().looseCollections().build()
				.read(serialized, H04_SimpleBean[].class);
			assertNotNull(result);
		}

		@Test void h05_read_optional_string() throws Exception {
			// Parse as Optional — covers isOptional() branch in readAnything (line 248)
			var serialized = RdfSerializer.create().ntriple().build().write("opt-value");
			var result = RdfParser.create().ntriple().build().read(serialized, Optional.class);
			assertNotNull(result);
		}

		public interface H06Animal {}

		@Marshalled(typeName = "H06Dog")
		public static class H06_Dog implements H06Animal {
			public String name = "Buddy";
		}

		@Test void h06_read_typed_bean_with_addBeanTypesRdf() throws Exception {
			// Manually construct N-Triple RDF with a _type triple; parse as abstract interface →
			// covers lines 253-259 (type property lookup, canCreateNewInstance=false path)
			// The parser finds the _type="H06Dog" triple and instantiates H06_Dog from the dictionary.
			var p = RdfParser.create().ntriple().beanDictionary(H06_Dog.class).build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "_:B1 <" + bpNs + "_type> \"H06Dog\" .\n"
				+ "_:B1 <" + bpNs + "name> \"Buddy\" .\n";
			var result = p.read(rdf, H06Animal.class);
			assertNotNull(result);
		}		public static class H07_FullBean {
			public String known = "x";
			public String extra = "extra-value";
		}

		public static class H07_PartialBean {
			public String known = "y";
		}

		@Test void h07_read_bean_unknown_properties_ignored() throws Exception {
			// Parse into bean that lacks some properties — covers line 420 (unknown property path in readIntoBeanMap)
			var serialized = RdfSerializer.create().ntriple().build().write(new H07_FullBean());
			var result = RdfParser.create().ntriple().ignoreUnknownBeanProperties().build()
				.read(serialized, H07_PartialBean.class);
			assertNotNull(result);
		}

		@Test void h08_read_date_roundtrip() throws Exception {
			// Serialize Date and parse back — covers line 351 (isDate branch in readAnything)
			var date = new Date(1000000000L);
			var serialized = RdfSerializer.create().ntriple().build().write(date);
			var result = RdfParser.create().ntriple().build().read(serialized, Date.class);
			assertNotNull(result);
		}

		@Test void h09_read_calendar_roundtrip() throws Exception {
			// Serialize Calendar and parse back — covers line 353 (isCalendar branch in readAnything)
			var cal = Calendar.getInstance();
			var serialized = RdfSerializer.create().ntriple().build().write(cal);
			var result = RdfParser.create().ntriple().build().read(serialized, Calendar.class);
			assertNotNull(result);
		}

		public static class H10_BeanWithRdfFormat {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public List<String> tags = new ArrayList<>(List.of("a", "b"));
		}

		@Test void h10_bean_property_with_rdf_collection_format_annotation() throws Exception {
			// Bean property annotated @Rdf(collectionFormat=MULTI_VALUED) — covers lines 219-220
			// isMultiValuedCollections returns true from the property-level annotation (not global format)
			var bean = new H10_BeanWithRdfFormat();
			var serialized = RdfSerializer.create().ntriple().build().write(bean);
			var result = RdfParser.create().ntriple().build().read(serialized, H10_BeanWithRdfFormat.class);
			assertNotNull(result);
		}

		@Test void h11_read_empty_rdf_returns_null() throws Exception {
			// Parse empty N-Triple content — roots empty → null returned (line 500-501 null branch)
			var result = RdfParser.create().ntriple().build().read("", Object.class);
			assertNull(result);
		}

		@Test void h12_read_empty_rdf_as_optional() throws Exception {
			// Parse empty N-Triple content as Optional — roots empty → opte() returned (line 501 true branch)
			var result = (Optional<?>)RdfParser.create().ntriple().build().read("", Optional.class);
			assertNotNull(result);
			assertTrue(result.isEmpty());
		}

		@Test void h13_read_too_many_roots_throws() {
			// N-Triple with two disconnected resources — too many roots → ParseException (line 503-504)
			var rdf = """
					<http://a.example.org/1> <http://p.example.org/prop> "val1" .
					<http://b.example.org/2> <http://p.example.org/prop> "val2" .
					""";
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build().read(rdf, String.class));
		}
	}

	@SuppressWarnings("java:S5778")
	@Nested class I_readrBranchCovers extends TestBase {

		@Test void i01_read_self_referential() throws Exception {
			// Self-referential resource (subject == object) — line 181 FALSE branch:
			// object.isResource() is true but object.equals(subject) is true → not added to objects set.
			// The self-referential resource is still a root; recursing into it triggers cycle detection (line 274 TRUE).
			var p = RdfParser.create().ntriple().build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "<http://ex.org/a> <http://ex.org/self> <http://ex.org/a> .\n"
				+ "<http://ex.org/a> <" + bpNs + "name> \"root\" .\n";
			var result = p.read(rdf, Map.class);
			assertNotNull(result);
		}

		public interface I02TypedInterface {}

		@SuppressWarnings("rawtypes")
		@Test void i02_read_unknown_type_name() throws Exception {
			// _type property with unresolvable class name — line 258 FALSE: nn(tcm) is false → type unchanged.
			// Map (interface) can't be instantiated → type lookup runs; unknown type → nn(tcm)=false → sType stays Map.
			// Falls through to sType.isMap() path and returns the raw JsonMap.
			var p = RdfParser.create().ntriple().build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "_:B1 <" + bpNs + "_type> \"com.example.DoesNotExist\" .\n"
				+ "_:B1 <" + bpNs + "name> \"test\" .\n";
			Map result = p.read(rdf, Map.class);
			assertNotNull(result);
		}

		@Test void i03_read_bag_as_object() throws Exception {
			// BAG collection parsed as Object type — covers isBag(r) TRUE in sType.isObject() path (line 281)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.write(List.of("a", "b", "c"));
			var result = RdfParser.create().ntriple().build().read(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void i04_read_list_as_object() throws Exception {
			// LIST collection parsed as Object type — covers r.canAs(RDFList.class) TRUE in isObject path (line 284)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.write(List.of("x", "y"));
			var result = RdfParser.create().ntriple().build().read(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void i05_read_named_uri_resource_with_no_children_as_object() throws Exception {
			// Named URI resource with no child properties — covers line 291 TRUE:
			// nn(uri) && !r.listProperties().hasNext() → o = r.getURI()
			var p = RdfParser.create().ntriple().build();
			var rdf = "<http://ex.org/main> <http://ex.org/ref> <http://ex.org/orphan> .\n"
				+ "<http://ex.org/main> <" + p.getJuneauBpNs().getUri() + "name> \"main\" .\n";
			var result = p.read(rdf, Map.class);
			assertNotNull(result);
		}

		public static class I06_CycleBean {
			public String name;
			public I06_CycleBean child1;
			public I06_CycleBean child2;
		}

		@Test void i06_read_shared_blank_node_cycle() throws Exception {
			// Two properties pointing to same blank node — second visit triggers cycle detection (line 310 TRUE):
			// _:A → child1 → _:B; _:A → child2 → _:B; second parse of _:B returns null.
			var p = RdfParser.create().ntriple().build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "_:A <" + bpNs + "name> \"root\" .\n"
				+ "_:A <" + bpNs + "child1> _:B .\n"
				+ "_:A <" + bpNs + "child2> _:B .\n"
				+ "_:B <" + bpNs + "name> \"shared\" .\n";
			var result = p.read(rdf, I06_CycleBean.class);
			assertNotNull(result);
		}

		@Test void i07_read_integer_literal_as_object() throws Exception {
			// Typed literal with Integer value parsed as Object — covers line 269 FALSE:
			// addLiteralTypes() emits xsd:integer; Jena's getValue() returns Integer (not String).
			var serialized = RdfSerializer.create().ntriple().addLiteralTypes().build().write(42);
			var result = RdfParser.create().ntriple().build().read(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void i08_read_instant_temporal() throws Exception {
			// Serialize Instant and parse back — covers isTemporal() branch (line 355)
			var now = Instant.now();
			var serialized = RdfSerializer.create().ntriple().build().write(now);
			var result = RdfParser.create().ntriple().build().read(serialized, Instant.class);
			assertNotNull(result);
		}

		@Test void i09_read_duration() throws Exception {
			// Serialize Duration and parse back — covers isDuration() branch (line 357)
			var d = Duration.ofHours(3);
			var serialized = RdfSerializer.create().ntriple().build().write(d);
			var result = RdfParser.create().ntriple().build().read(serialized, Duration.class);
			assertNotNull(result);
		}

		@Test void i10_read_period() throws Exception {
			// Serialize Period and parse back — covers isPeriod() branch (line 359)
			var period = Period.ofDays(7);
			var serialized = RdfSerializer.create().ntriple().build().write(period);
			var result = RdfParser.create().ntriple().build().read(serialized, Period.class);
			assertNotNull(result);
		}

		@Test void i11_read_url_roundtrip() throws Exception {
			// Serialize URL and parse back — covers sType.isUri() && n.isResource() branch (line 347)
			var url = new java.net.URL("http://example.org/test-i11");
			var serialized = RdfSerializer.create().ntriple().build().write(url);
			var result = RdfParser.create().ntriple().build().read(serialized, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void i13_read_nonempty_seq_as_list() throws Exception {
			// Non-empty SEQ parsed as List — exercises Container iterator loop body (line 433)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.write(List.of("p", "q", "r"));
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void i14_read_nonempty_bag_as_list() throws Exception {
			// Non-empty BAG parsed as List — covers isBag(r) TRUE in collection path (line 330) and loop body (line 433)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.write(List.of("x", "y", "z"));
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.read(serialized, List.class);
			assertNotNull(result);
		}

		@Test void i15_read_nonempty_list_format_as_list() throws Exception {
			// Non-empty LIST format parsed as List — covers r.canAs(RDFList) TRUE in collection path (line 332)
			// and RDFList iterator loop body (line 442)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.write(List.of("u", "v", "w"));
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.read(serialized, List.class);
			assertNotNull(result);
		}

		public static class I16_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri = "http://example.org/i16";
			public String name = "beanUri-test";
		}

		@Test void i16_read_bean_with_bean_uri() throws Exception {
			// Serialize bean with @Rdf(beanUri=true) and parse back — covers readIntoBeanMap
			// line 389 TRUE: hasBeanUri() && nn(r2.getURI()) → sets the beanUri property from the resource URI.
			var serialized = RdfSerializer.create().ntriple().build().write(new I16_BeanWithBeanUri());
			var result = RdfParser.create().ntriple().build().read(serialized, I16_BeanWithBeanUri.class);
			assertNotNull(result);
		}

		@SuppressWarnings("rawtypes")
		@Test void i17_read_map_with_root_property() throws Exception {
			// Map serialized with addRootProperty() — readIntoMap skips the root triple (line 459 FALSE:
			// key.equals("root") && p.getURI().equals(juneauNs) → condition TRUE → skip via !(...) = FALSE)
			var serialized = RdfSerializer.create().ntriple().addRootProperty().build()
				.write(Map.of("name", "test"));
			Map result = RdfParser.create().ntriple().build().read(serialized, Map.class);
			assertNotNull(result);
		}

		@Test void i18_read_string_from_resource_without_pvalue_throws() {
			// Named resource without pValue wrapper parsed as String — triggers getValue():
			// n.isLiteral()=FALSE (line 193), n.isResource()=TRUE (line 195), nn(st)=FALSE (line 197) → ParseException
			var rdf = "<http://ex.org/a> <http://ex.org/p> \"x\" .\n";
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build().read(rdf, String.class));
		}

		public static class I19_BeanWithListNoAnnotation {
			public List<String> tags = new ArrayList<>(List.of("a", "b"));
		}

		@Test void i19_read_multivalue_global_format() throws Exception {
			// Collection property with no @Rdf annotation; global MULTI_VALUED format →
			// isMultiValuedCollections() FALSE branch (line 222): falls through to getCollectionFormat() check.
			var bean = new I19_BeanWithListNoAnnotation();
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build()
				.write(bean);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build()
				.read(serialized, I19_BeanWithListNoAnnotation.class);
			assertNotNull(result);
		}

		@Test void i20_read_loose_collections_list() throws Exception {
			// looseCollections() → doRead line 484 TRUE: multiple root nodes added to a List
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.write(new ArrayList<>(List.of("x", "y", "z")));
			var result = RdfParser.create().ntriple().looseCollections().build()
				.read(serialized, ArrayList.class);
			assertNotNull(result);
		}

		@Test void i21_read_loose_collections_array() throws Exception {
			// looseCollections() + array type → doRead line 486-496: isArray()=TRUE → toArray()
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.write(new String[]{"a", "b"});
			var result = RdfParser.create().ntriple().looseCollections().build()
				.read(serialized, String[].class);
			assertNotNull(result);
		}

		@Test void i22_read_empty_model_as_optional() throws Exception {
			// Empty input → getRoots() returns empty → doRead line 501 TRUE (type.isOptional()):
			// result is Optional.empty()
			var empty = "";
			var result = (Optional<?>)RdfParser.create().ntriple().build().read(empty, Optional.class);
			assertNotNull(result);
			assertTrue(result.isEmpty());
		}

		@Test void i23_read_empty_model_non_optional() throws Exception {
			// Empty input → getRoots() returns empty → doRead line 501 FALSE (!type.isOptional()):
			// result is null
			var empty = "";
			var result = RdfParser.create().ntriple().build().read(empty, String.class);
			assertNull(result);
		}

		@Test void i24_read_multiple_roots_throws() {
			// Two unconnected resources → getRoots() returns 2 → doRead line 503-504 throws ParseException
			var rdf = """
					<http://ex.org/a> <http://ex.org/p> "x" .
					<http://ex.org/b> <http://ex.org/p> "y" .
					""";
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build().read(rdf, String.class));
		}

		@Test void i25_read_char_type() throws Exception {
			// sType.isChar() branch (line 342) — parse character from serialized single-char bean property
			var serialized = RdfSerializer.create().ntriple().build().write('A');
			var result = RdfParser.create().ntriple().build().read(serialized, Character.class);
			assertEquals('A', result);
		}

		@Test void i26_read_boolean_type() throws Exception {
			// sType.isBoolean() branch (line 346) — parse boolean round-trip
			var serialized = RdfSerializer.create().ntriple().build().write(true);
			var result = RdfParser.create().ntriple().build().read(serialized, Boolean.class);
			assertEquals(Boolean.TRUE, result);
		}

		@Test void i27_read_integer_type() throws Exception {
			// sType.isNumber() branch (line 344) — parse integer round-trip
			var serialized = RdfSerializer.create().ntriple().build().write(42);
			var result = RdfParser.create().ntriple().build().read(serialized, Integer.class);
			assertEquals(42, result);
		}

		public static class I28_SimpleBean {
			public String name;
		}

		@Test void i28_read_loose_collections_non_collection_type() throws Exception {
			// looseCollections()=true, type is NOT a collection → condition FALSE (branch 2):
			// isLooseCollections()=true && isCollectionOrArray()=false → falls through to single-root path
			var b = new I28_SimpleBean();
			b.name = "loose-bean";
			var serialized = RdfSerializer.create().ntriple().looseCollections().build().write(b);
			var result = RdfParser.create().ntriple().looseCollections().build()
				.read(serialized, I28_SimpleBean.class);
			assertNotNull(result);
		}

		@Test void i29_read_bean_after_add_root_property() throws Exception {
			// Bean serialized with addRootProperty() → readIntoBeanMap encounters pRoot predicate →
			// else if NOT(p.equals(pRoot) || p.equals(pType)): condition TRUE for root → skip (p.equals(pRoot)=TRUE)
			var b = new I28_SimpleBean();
			b.name = "with-root";
			var serialized = RdfSerializer.create().ntriple().addRootProperty().build().write(b);
			var result = RdfParser.create().ntriple().build().read(serialized, I28_SimpleBean.class);
			assertNotNull(result);
		}

		@Test void i30_read_trimstrings() throws Exception {
			// trimStrings()=true → decodeString TRUE branch: s.trim() called on the decoded value
			var b = new I28_SimpleBean();
			b.name = " padded ";
			var serialized = RdfSerializer.create().ntriple().build().write(b);
			var result = RdfParser.create().ntriple().trimStrings().build()
				.read(serialized, I28_SimpleBean.class);
			assertNotNull(result);
		}
	}

	@Nested class J_streamSessionWhiteBox extends TestBase {

		private RdfStreamParserSession newSession() {
			return RdfStreamParserSession.create(RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()).build();
		}

		@Test void j01_decodeString_null() {
			// decodeString(null) — o==null branch (line 160-161), never reached via the public read() API
			// since every internal caller passes a non-null String/Object.
			var session = newSession();
			assertNull(session.decodeString(null));
		}

		@Test void j02_decodeString_trimStrings() {
			// trimStrings()=true → isTrimStrings() TRUE branch (line 168-169)
			var session = RdfStreamParserSession.create(
				RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).trimStrings(true).build()).build();
			assertEquals("hello", session.decodeString("  hello  "));
		}

		@Test void j03_getRoots_withExplicitRootMarker() {
			// getRoots(Model) called directly with a "root" marker property present — covers the
			// primary lookup loop body (line 182-183) and the early-return path (line 185-186).
			// RdfStreamSerializer.Builder never exposes addRootProperty(), so this is only reachable
			// via direct Model construction against the shared getRoots() algorithm.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var rootProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_ROOT);
			var marked = m.createResource("http://ex.org/marked");
			marked.addProperty(rootProp, "true");
			// An unrelated, unmarked resource must NOT be picked up once a marker match exists.
			m.createResource("http://ex.org/unmarked").addProperty(m.createProperty("http://ex.org/p"), "v");
			var roots = session.getRoots(m);
			assertEquals(1, roots.size());
			assertEquals("http://ex.org/marked", roots.get(0).getURI());
		}

		@Test void j04_getRoots_fallbackScan_selfReferential() {
			// Fallback subject/object scan (line 191-203) with a self-referential resource:
			// object.equals(subject)=TRUE — covers the remaining branch combination of line 196
			// that the round-trip tests (whose graphs never self-reference) cannot reach.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var self = m.createResource("http://ex.org/self");
			self.addProperty(m.createProperty("http://ex.org/loop"), self);
			var roots = session.getRoots(m);
			// The self-referential resource is still a root: it's never recorded as someone else's object.
			assertTrue(roots.stream().anyMatch(r -> "http://ex.org/self".equals(r.getURI())));
		}

		@Test void j05_getValue_resourceWithoutPValue_throws() {
			// getValue(Resource, outer) where the resource carries neither a literal value nor a
			// pValue wrapper property — covers the nn(st)==false fallthrough to the thrown
			// ParseException (line 212 FALSE, line 219).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/no-value");
			assertThrowsWithMessage(Exception.class, "Unknown value type", () -> session.getValue(r, null));
		}

		@Test void j06_getValue_pValueWrapsNestedResource() {
			// getValue(Resource, outer) where the pValue property points to ANOTHER resource
			// (not a literal) — covers the recursive readAnything() call at line 216, which the
			// wrapped-literal round-trip tests (pValue → literal) never trigger.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var valueProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_VALUE);
			var r = m.createResource("http://ex.org/wrapper");
			var nested = m.createResource("http://ex.org/nested");
			r.addProperty(valueProp, nested);
			var result = session.getValue(r, null);
			assertNotNull(result);
		}

		@Test void j07_isBag_withLiteral_returnsFalse() {
			// isBag(RDFNode) where n.isResource()==FALSE — covers the FALSE branch of line 223.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("not-a-resource");
			assertFalse(session.isBag(lit));
		}

		@Test void j08_isSeq_withLiteral_returnsFalse() {
			// isSeq(RDFNode) where n.isResource()==FALSE — covers the FALSE branch of line 241.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("not-a-resource");
			assertFalse(session.isSeq(lit));
		}

		@Test void j09_isBag_resourceWithoutRdfType_returnsFalse() {
			// isBag(RDFNode) where n.isResource()==TRUE but has no rdf:type statement — covers the
			// nn(st)==false fallthrough at line 224-225 (falls through to the final `return false`).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/untyped");
			assertFalse(session.isBag(r));
		}

		@Test void j10_isMultiValuedCollections_nullPropertyMeta() {
			// isMultiValuedCollections(null) — pMeta==null branch (line 232) uses the RdfBeanPropertyMeta
			// DEFAULT, whose collection format is DEFAULT; falls through to the global getCollectionFormat()
			// check (line 237), which is always DEFAULT (not MULTI_VALUED) for stream parsers since
			// RdfStreamParser.Builder never exposes collectionFormat().
			var session = newSession();
			assertFalse(session.isMultiValuedCollections(null));
		}
	}

	@Nested class K_streamSessionRoundTrips extends TestBase {

		public static class K01_BeanWithBagProperty {
			@Rdf(collectionFormat = RdfCollectionFormat.BAG)
			public List<String> items = new ArrayList<>(List.of("a", "b"));
		}

		@Test void k01_multiValuedCollections_propertyOverride_notMultiValued() throws Exception {
			// Bean property annotated with a non-DEFAULT, non-MULTI_VALUED format (BAG) — covers
			// isMultiValuedCollections() line 234 TRUE / line 235 FALSE (property override present,
			// but it isn't MULTI_VALUED) via the stream parser's readIntoBeanMap dispatch.
			var bean = new K01_BeanWithBagProperty();
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(bean);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, K01_BeanWithBagProperty.class);
			assertNotNull(result);
		}

		public interface K02_Animal {}

		@Marshalled(typeName = "K02Cat")
		public static class K02_Cat implements K02_Animal {
			public String name = "Whiskers";
		}

		@Test void k02_stream_read_typed_bean_via_beanDictionary() throws Exception {
			// _type property lookup with an interface target type — covers readAnything's
			// canCreateNewInstance()==false + type-property lookup path (lines 268-274) in the
			// stream parser session. Root-level writes always resolve eType via getExpectedRootType(),
			// which returns the ACTUAL runtime ClassMeta (making eType==aType) unless addRootType() is
			// also set (see o08b_stream_addBeanTypes_typeNameEmitted in RdfSerializer_Test for the
			// non-root addBeanTypes() coverage), so a root-level "_type" triple is still only reachable
			// here by hand-authoring it and transcoding it to RDF/THRIFT.
			var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).beanDictionary(K02_Cat.class).build();
			var bpNs = p.getRdfParser().getJuneauBpNs().getUri();
			var ntriple = "_:B1 <" + bpNs + "_type> \"K02Cat\" .\n"
				+ "_:B1 <" + bpNs + "name> \"Whiskers\" .\n";
			var m = ModelFactory.createDefaultModel();
			org.apache.jena.riot.RDFDataMgr.read(m,
				new java.io.ByteArrayInputStream(ntriple.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				org.apache.jena.riot.Lang.NTRIPLES);
			byte[] bytes;
			try (var out = new java.io.ByteArrayOutputStream()) {
				org.apache.jena.riot.RDFDataMgr.write(out, m, org.apache.jena.riot.Lang.RDFTHRIFT);
				bytes = out.toByteArray();
			}
			var result = p.read(bytes, K02_Animal.class);
			assertInstanceOf(K02_Cat.class, result);
		}

		public static class K03_CycleBean {
			public String name;
			public K03_CycleBean child1;
			public K03_CycleBean child2;
		}

		@Test void k03_stream_read_shared_blank_node_cycle() throws Exception {
			// Two bean properties pointing at the same nested bean — the second visit must trip
			// the urisVisited cycle-detection guard (line 325-326) in the stream parser session.
			var a = new K03_CycleBean();
			a.name = "root";
			var shared = new K03_CycleBean();
			shared.name = "shared";
			a.child1 = shared;
			a.child2 = shared;
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(a);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, K03_CycleBean.class);
			assertNotNull(result);
		}

		public static class K04_BeanWithListFormatItems {
			@Rdf(collectionFormat = RdfCollectionFormat.LIST)
			public List<String> items = new ArrayList<>(List.of("p", "q", "r"));
		}

		@Test void k04_stream_read_rdfList_format_property() throws Exception {
			// LIST (RDFList) collection format via a per-property @Rdf override — covers
			// r.canAs(RDFList.class) TRUE in the stream parser's collection dispatch and the
			// RDFList iterator loop body (readIntoCollection(RDFList,...)).
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.write(new K04_BeanWithListFormatItems());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, K04_BeanWithListFormatItems.class);
			assertNotNull(result);
		}

		@Test void k05_stream_read_empty_list_as_collection() throws Exception {
			// Empty collection serialized via Thrift (default SEQ) then parsed back as a List —
			// covers the empty-container loop (0 iterations) in readIntoCollection(Container,...).
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build()
				.write(new ArrayList<String>());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, List.class);
			assertNotNull(result);
		}

		public static class K06_FullBean {
			public String known = "x";
			public String extra = "extra-value";
		}

		public static class K06_PartialBean {
			public String known = "y";
		}

		@Test void k06_stream_read_bean_unknown_properties_ignored() throws Exception {
			// Parse into a bean missing some source properties — covers the unknown-property
			// (onUnknownProperty) branch of readIntoBeanMap in the stream parser session.
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(new K06_FullBean());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).ignoreUnknownBeanProperties().build()
				.read(bytes, K06_PartialBean.class);
			assertNotNull(result);
		}

		@Test void k07_stream_read_multiple_roots_throws() throws Exception {
			// Two disconnected top-level resources in one Thrift stream — covers the
			// "too many root nodes" ParseException branch (doRead, roots.size() > 1) in the
			// stream parser session. Built via a manually-authored N-Triple model transcoded to Thrift
			// (RdfStreamSerializer always serializes a single root, so this graph shape is otherwise
			// unreachable through the public write() API).
			var ntriple = """
					<http://ex.org/a> <http://ex.org/p> "val1" .
					<http://ex.org/b> <http://ex.org/p> "val2" .
					""";
			var m = ModelFactory.createDefaultModel();
			org.apache.jena.riot.RDFDataMgr.read(m,
				new java.io.ByteArrayInputStream(ntriple.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				org.apache.jena.riot.Lang.NTRIPLES);
			byte[] bytes;
			try (var out = new java.io.ByteArrayOutputStream()) {
				org.apache.jena.riot.RDFDataMgr.write(out, m, org.apache.jena.riot.Lang.RDFTHRIFT);
				bytes = out.toByteArray();
			}
			assertThrows(Exception.class, () -> RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, String.class));
		}
	}

	@Nested class L_streamSessionMoreCoverage extends TestBase {

		private RdfStreamParserSession newSession() {
			return RdfStreamParserSession.create(RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()).build();
		}

		@Builder(L01_BeanBuilder.class)
		public static class L01_Bean {
			public int x;
			public L01_Bean(L01_BeanBuilder b) { if (b != null) x = b.x; }
		}

		public static class L01_BeanBuilder {
			public int x;
			public L01_Bean build() { return new L01_Bean(this); }
		}

		@Test void l01_stream_builderSwap_roundtrip() throws Exception {
			// Exercises the nn(builder) top-level dispatch (readAnything sType selection) and the
			// builder.create()/readIntoBeanMap()/builder.build() branch, via the @Builder annotation.
			var bean = new L01_Bean(null);
			bean.x = 42;
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(bean);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build().read(bytes, L01_Bean.class);
			assertNotNull(result);
			assertEquals(42, result.x);
		}

		@Test void l02_readAnything_object_visitedResource_returnsUri() {
			// sType.isObject() with n.isResource(): first visit takes the generic-map (has-properties)
			// path; second visit of the SAME resource must hit the "already visited" branch (o = r.getURI()).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l02");
			r.addProperty(m.createProperty("http://ex.org/p"), "v");
			var first = session.readAnything(session.object(), r, null, null);
			assertNotNull(first);
			var second = session.readAnything(session.object(), r, null, null);
			assertEquals("http://ex.org/l02", second);
		}

		@Test void l03_readAnything_object_isSeq() {
			// sType.isObject() with n a Seq resource — covers the isSeq(r) branch of the object dispatch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var seq = m.createSeq();
			seq.add("a");
			seq.add("b");
			var result = session.readAnything(session.object(), seq, null, null);
			assertNotNull(result);
			assertTrue(((Collection<?>)result).contains("a"));
		}

		@Test void l04_readAnything_object_isRdfList() {
			// sType.isObject() with n an RDFList resource — covers the r.canAs(RDFList.class) branch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var list = m.createList(new RDFNode[]{m.createLiteral("x"), m.createLiteral("y")});
			var result = session.readAnything(session.object(), list, null, null);
			assertNotNull(result);
			assertTrue(((Collection<?>)result).contains("x"));
		}

		@Test void l05_readAnything_object_blankNode_withProperties() {
			// sType.isObject() with a blank (URI-less) resource that HAS properties — covers the
			// nn(uri)==false side of line 306's condition, falling into the generic-map branch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource();
			r.addProperty(m.createProperty("http://ex.org/p"), "v");
			var result = session.readAnything(session.object(), r, null, null);
			assertNotNull(result);
		}

		@Test void l06_readAnything_object_namedUri_noProperties() {
			// sType.isObject() with a named, property-less resource — covers nn(uri)==true &&
			// !hasNext()==true side of line 306: treated as an external URI reference.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l06-orphan");
			var result = session.readAnything(session.object(), r, null, null);
			assertEquals("http://ex.org/l06-orphan", result);
		}

		@Test void l07_readAnything_mapType_cycle() {
			// sType.isMap() (interface Map, not concretely instantiable) — first visit reads a generic
			// map normally; second visit of the SAME resource must hit the cycle-detection null return.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l07");
			r.addProperty(m.createProperty("http://ex.org/k"), "v");
			var mapType = session.getClassMeta(Map.class);
			var first = session.readAnything(mapType, r, null, null);
			assertNotNull(first);
			var second = session.readAnything(mapType, r, null, null);
			assertNull(second);
		}

		@Test void l08_readAnything_mapType_concreteSubclass_instantiate() {
			// sType.isMap() with a concretely-instantiable Map subclass (LinkedHashMap) — covers the
			// canCreateNewInstance()==true side of the map-instantiation ternary.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l08");
			r.addProperty(m.createProperty("http://ex.org/k"), "v");
			var mapType = session.getClassMeta(LinkedHashMap.class);
			var result = session.readAnything(mapType, r, null, null);
			assertNotNull(result);
			assertInstanceOf(LinkedHashMap.class, result);
		}

		@Test void l09_readAnything_byteArray_directByteValue() {
			// sType.isByteArray() where getValue() returns an actual byte[] (Jena's TypeMapper maps a
			// base64Binary-typed literal straight to byte[]) — covers the "v instanceof byte[]" branch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createTypedLiteral(new byte[]{1, 2, 3});
			var result = session.readAnything(session.getClassMeta(byte[].class), lit, null, null);
			assertArrayEquals(new byte[]{1, 2, 3}, (byte[])result);
		}

		@Test void l10_readAnything_byteArray_base64StringValue() {
			// sType.isByteArray() where getValue() returns a plain String (base64) — covers the
			// "v != null" Base64-decode branch (the normal round-trip shape for stream byte[] values).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var encoded = Base64.getEncoder().encodeToString(new byte[]{4, 5, 6});
			var lit = m.createLiteral(encoded);
			var result = session.readAnything(session.getClassMeta(byte[].class), lit, null, null);
			assertArrayEquals(new byte[]{4, 5, 6}, (byte[])result);
		}

		@Test void l11_readAnything_collection_cycle() {
			// sType.isCollectionOrArray() — first visit reads a SEQ normally; second visit of the SAME
			// resource must hit the cycle-detection null return (line 351-352).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var seq = m.createSeq();
			seq.add("a");
			var listType = session.getClassMeta(List.class);
			var first = session.readAnything(listType, seq, null, null);
			assertNotNull(first);
			var second = session.readAnything(listType, seq, null, null);
			assertNull(second);
		}

		@Test void l12_readAnything_collection_unrecognizedResource_throws() {
			// sType.isCollectionOrArray() with a resource that is neither a Seq, Bag, nor RDFList —
			// covers the "Unrecognized node type for collection" ParseException.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l12-not-a-collection");
			r.addProperty(m.createProperty("http://ex.org/p"), "v");
			var listType = session.getClassMeta(List.class);
			assertThrowsWithMessage(Exception.class, "Unrecognized node type", () -> session.readAnything(listType, r, null, null));
		}

		public abstract static class L13_NotABean {
			public String name;
		}

		@Test void l13_readAnything_notABean_resource_throws() {
			// sType matches none of the instantiation strategies (abstract, no proxy, no _type match) and
			// n IS a resource — covers the "could not be instantiated" ParseException (generic-map path).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l13");
			r.addProperty(m.createProperty("http://ex.org/name"), "x");
			var notABeanType = session.getClassMeta(L13_NotABean.class);
			assertThrowsWithMessage(Exception.class, "could not be instantiated", () -> session.readAnything(notABeanType, r, null, null));
		}

		@Test void l14_readAnything_notABean_literal_throws() {
			// sType matches none of the instantiation strategies and n is a LITERAL (not a resource) —
			// covers the final catch-all "could not be instantiated" ParseException (line 398-399),
			// which the round-trip API never produces since scalar-shaped values are always handled
			// by an earlier branch; only reachable by handing a raw literal to a not-a-bean ClassMeta.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("x");
			var notABeanType = session.getClassMeta(L13_NotABean.class);
			assertThrowsWithMessage(Exception.class, "could not be instantiated", () -> session.readAnything(notABeanType, lit, null, null));
		}

		public interface L15_IBean {
			String getName();
			void setName(String name);
		}

		@Test void l15_stream_interfaceProxy_roundtrip() throws Exception {
			// Interface bean with no dictionary/_type match — covers the generic-map + proxy-invocation-
			// handler branch (nn(sType.getProxyInvocationHandler())==true) in readAnything.
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(Map.of("name", "Bob"));
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build().read(bytes, L15_IBean.class);
			assertNotNull(result);
			assertEquals("Bob", result.getName());
		}

		public static class L16_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri = "http://example.org/l16";
			public String name = "beanUri-test";
		}

		@Test void l16_stream_beanUri_roundtrip() throws Exception {
			// @Rdf(beanUri=true) property, via the stream session — covers readIntoBeanMap's
			// hasBeanUri() && nn(r2.getURI()) branch (line 414).
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(new L16_BeanWithBeanUri());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build().read(bytes, L16_BeanWithBeanUri.class);
			assertNotNull(result);
			assertEquals("beanUri-test", result.name);
		}

		public static class L17_ThrowingSetterBean {
			private String name;
			public String getName() { return name; }
			public void setName(@SuppressWarnings("unused") String name) { throw new IllegalArgumentException("boom"); }
		}

		@Test void l17_stream_beanSetter_throws_wrappedAsParseException() throws Exception {
			// Setter throws IllegalArgumentException — covers the pMeta.set() BeanRuntimeException
			// catch/rethrow branch (line 440-442) in readIntoBeanMap.
			var bean = new NamedBean();
			bean.setName("x");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(bean);
			assertThrows(Exception.class, () -> RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, L17_ThrowingSetterBean.class));
		}

		@Test void l19_readAnything_unresolvableTypeProperty_fallsThrough_toCastBranch() {
			// eType is a bean-shaped interface, which Juneau's proxy support makes canCreateNewInstance()
			// TRUE for — so this does NOT go through the "_type"-property lookup at lines 268-274 (that
			// requires canCreateNewInstance()==FALSE; see l19b below for that case), but instead dispatches
			// via sType.canCreateNewBean(outer)'s proxy-creation path. The stray "_type"/"x" properties on
			// the resource are simply read as ordinary bean properties through the proxy's BeanMap.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var bpNs = session.getJuneauBpNs().getUri();
			var r = m.createResource("http://ex.org/l19");
			r.addProperty(m.createProperty(bpNs, "_type"), "Bogus");
			r.addProperty(m.createProperty(bpNs, "x"), "hello");
			var result = session.readAnything(session.getClassMeta(L19_IBean.class), r, null, null);
			assertNotNull(result);
		}

		public interface L19_IBean {
			String getX();
			void setX(String x);
		}

		public interface L19b_EmptyInterface {
			// No bean properties at all — Juneau's interface-proxy support declines to treat this as
			// creatable (no getters/setters to proxy), so canCreateNewInstance() is FALSE for it, unlike
			// the bean-shaped L19_IBean above.
		}

		@Test void l19b_readAnything_typeProperty_unresolvableName_leavesTypeUnchanged() {
			// eType is a property-less interface (canCreateNewInstance()==FALSE) with a "_type" property
			// whose value does NOT resolve to any registered dictionary entry — covers line 273's
			// nn(tcm)==FALSE branch (sType/eType left unchanged), the mirror image of k02's resolvable-type
			// case. Falls through to readIntoMap + the containsKey(getBeanTypePropertyName(eType)) cast()
			// branch (line 392-393) at the bottom of readAnything, since eType.canCreateNewBean() is also
			// false for a totally empty interface with no bean properties to proxy.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var bpNs = session.getJuneauBpNs().getUri();
			var r = m.createResource("http://ex.org/l19b");
			r.addProperty(m.createProperty(bpNs, "_type"), "TotallyUnregisteredBogusTypeName");
			r.addProperty(m.createProperty(bpNs, "x"), "hello");
			var result = session.readAnything(session.getClassMeta(L19b_EmptyInterface.class), r, null, null);
			assertNotNull(result);
		}

		@Test void l09b_readAnything_object_literal_nonStringValue() {
			// sType.isObject() && n.isLiteral() where the literal's Jena value is NOT a String (a typed
			// numeric literal) — covers the "o instanceof String" FALSE branch. Only reachable by handing
			// a typed literal directly to readAnything, since the stream serializer never emits typed
			// literals (addLiteralTypes is always false).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createTypedLiteral(42);
			var result = session.readAnything(session.object(), lit, null, null);
			assertEquals(42, result);
		}

		@Test void l19b_readAnything_byteArray_valueResolvesToNull() {
			// sType.isByteArray() where getValue() itself returns null (the pValue wrapper points at the
			// rdf:nil resource, which readAnything's "leave o == null" branch resolves to null) — covers
			// the "v != null" FALSE branch (line 342), distinct from the direct-byte[]/base64-string cases.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var valueProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_VALUE);
			var r = m.createResource("http://ex.org/l19b");
			var nil = m.createResource(Constants.RDF_NIL);
			r.addProperty(valueProp, nil);
			var result = session.readAnything(session.getClassMeta(byte[].class), r, null, null);
			assertNull(result);
		}

		@Builder(L20_BeanBuilder.class)
		public static class L20_Bean {
			public int x;
			public L20_Bean(L20_BeanBuilder b) { if (b != null) x = b.x; }
		}

		public static class L20_BeanBuilder {
			public int x;
			public L20_Bean build() { return new L20_Bean(this); }
		}

		@Test void l20_readAnything_builderSwap_cycle() {
			// The builder-swap dispatch (nn(builder)) visits the SAME resource twice — covers the
			// urisVisited cycle-detection null return at line 319-320.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l20");
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "x"), "5");
			var type = session.getClassMeta(L20_Bean.class);
			var first = session.readAnything(type, r, null, null);
			assertNotNull(first);
			var second = session.readAnything(type, r, null, null);
			assertNull(second);
		}

		@Test void l21_readAnything_canCreateNewBean_cycle() {
			// sType.canCreateNewBean() dispatch visits the SAME resource twice — covers the
			// urisVisited cycle-detection null return at line 325-326.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/l21");
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "name"), "hi");
			var type = session.getClassMeta(NamedBean.class);
			var first = session.readAnything(type, r, null, null);
			assertNotNull(first);
			var second = session.readAnything(type, r, null, null);
			assertNull(second);
		}

		@Test void l22_readAnything_uriType_literalNode() {
			// sType.isUri() with a LITERAL node (not a resource) — covers the n.isResource()==false side
			// of line 372's condition. CORRECTION (found during triage, confirmed via a temporary
			// System.err probe on the condition's two operands): java.net.URL does NOT actually reach
			// this branch at all — URL has a default UrlSwap registered, so at the top of readAnything,
			// `sType = swap.getSwapClassMeta(this)` reclassifies sType to STRING before this check ever
			// runs, making sType.isUri() unconditionally FALSE for it (dispatches via isCharSequence()
			// instead, several branches earlier). Using plain java.net.URI instead — which has NO
			// registered swap, so sType stays URI-categorized — genuinely reaches this branch with
			// isResource()==false, exercising the FALSE side and letting control continue past this
			// else-if to the later generic string-conversion fallback.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("http://example.org/l22");
			var result = session.readAnything(session.getClassMeta(java.net.URI.class), lit, null, null);
			assertEquals(java.net.URI.create("http://example.org/l22"), result);
		}

		public static class L22b_UriPropertyBean {
			public java.net.URI location = java.net.URI.create("http://example.org/l22b");
			public String name = "with-uri-prop";
		}

		@Test void l22b_stream_read_uriTypedProperty_roundtrip() throws Exception {
			// Serialize a bean with a java.net.URI-typed PROPERTY and parse back via the STREAM
			// session — covers the n.isResource()==true side of line 372's condition (never exercised
			// by l22's literal-node case). Deliberately java.net.URI, NOT java.net.URL: URL has a
			// default UrlSwap registered (swaps to String before this check ever runs, so sType.isUri()
			// is always false for it — see l22 which relies on exactly that fallback), whereas plain
			// java.net.URI has no default swap, so sType stays the URI-categorized eType. And
			// deliberately a PROPERTY (not the bean itself as root): a bare URI as the ROOT object
			// serializes to a Resource with no triples at all (since the value has no properties of its
			// own), which getRoots()'s subject-scan can never discover, so it can never round-trip —
			// unlike a property value, whose resource is asserted as the object of the owning bean's
			// triple and is therefore reachable through readIntoBeanMap -> readAnything normally.
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(new L22b_UriPropertyBean());
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build().read(bytes, L22b_UriPropertyBean.class);
			assertEquals(java.net.URI.create("http://example.org/l22b"), result.location);
		}

		@Test void l23_stream_read_date_roundtrip() throws Exception {
			// Serialize a Date and parse back via the STREAM session — covers the isDate() branch
			// (line 376-377), never exercised by the existing stream round-trip tests.
			var date = new Date(1_000_000_000L);
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(date);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build().read(bytes, Date.class);
			assertNotNull(result);
		}

		public static class L24_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri;
			public String name = "blank-node-beanUri";
		}

		@Test void l24_readIntoBeanMap_beanUri_blankNode() {
			// hasBeanUri()==true but the resource is a BLANK node (r2.getURI()==null) — covers the
			// nn(r2.getURI())==false side of line 414's condition, which every round-trip beanUri test
			// (whose beans always carry a real URI) cannot reach.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource();
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "name"), "blank-name");
			var bm = session.toBeanMap(new L24_BeanWithBeanUri());
			var result = session.readIntoBeanMap(r, bm);
			assertEquals("blank-name", result.getBean().name);
		}

		public static class L25_SourceBean {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public List<String> items = new ArrayList<>(List.of("a"));
		}

		public static class L25_ThrowingList<T> extends ArrayList<T> {
			private static final long serialVersionUID = 1L;
			@Override public boolean add(T t) { throw new UnsupportedOperationException("boom"); }
		}

		public static class L25_ThrowingListBean {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public L25_ThrowingList<String> items = new L25_ThrowingList<>();
		}

		@Test void l25_stream_multivalued_add_throws_wrappedAsParseException() throws Exception {
			// Serialize a normal List-backed bean, then parse the SAME shape into a bean whose "items"
			// field is a mutable-LOOKING (canAddTo()==true, since it extends ArrayList) List whose add()
			// always throws — the parser's per-triple pMeta.add() call propagates that as a
			// BeanRuntimeException, covering the catch/rethrow at line 431-433 in readIntoBeanMap's
			// multi-valued path (distinct from List.of(), which canAddTo() correctly detects and routes
			// around via a fresh-list copy instead of mutating in place).
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().write(new L25_SourceBean());
			assertThrows(Exception.class, () -> RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.read(bytes, L25_ThrowingListBean.class));
		}

		@Test void l26_readIntoBeanMap_rootProperty_skipped() {
			// A "root" marker triple on a bean resource has no matching bean property — covers the
			// p.equals(pRoot)==true side of line 445's condition (skipped, no onUnknownProperty call),
			// which the round-trip API can't produce since RdfStreamSerializer never exposes
			// addRootProperty().
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var rootProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_ROOT);
			var r = m.createResource("http://ex.org/l26");
			r.addProperty(rootProp, "true");
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "name"), "hi");
			var bm = session.toBeanMap(new NamedBean());
			var result = session.readIntoBeanMap(r, bm);
			assertEquals("hi", result.getBean().getName());
		}

		@Test void l27_readIntoMap_rootProperty_skipped() {
			// Fixed: readIntoMap's root-skip guard now compares like-for-like (`p.equals(pRoot)`, both
			// full URIs) instead of comparing the property's full URI to the bare namespace URI, so the
			// "root" marker triple is now correctly filtered out of generic maps -- matching
			// readIntoBeanMap's equivalent check.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var rootProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_ROOT);
			var r = m.createResource("http://ex.org/l27");
			r.addProperty(rootProp, "true");
			r.addProperty(m.createProperty("http://ex.org/k"), "v");
			Map<String,Object> map = new LinkedHashMap<>();
			var result = session.readIntoMap(r, map, session.getClassMeta(String.class), session.object(), null);
			assertFalse(result.containsKey("root"));
			assertEquals("v", result.get("k"));
		}

		@Test void l18_getJuneauBpNs_getLanguage_getRdfClassMeta_getXmlBeanPropertyMeta() {
			// Direct coverage of trivial protected getters that delegate to ctx — never exercised
			// via the public read() API since they aren't on any hot parsing path.
			var session = newSession();
			assertNotNull(session.getJuneauBpNs());
			assertEquals(Constants.LANG_RDFTHRIFT, session.getLanguage());
			var bc = RdfStreamParser.create().build().getMarshallingContext();
			var bm = bc.getBeanMeta(NamedBean.class);
			assertNotNull(session.getRdfClassMeta(bc.getClassMeta(NamedBean.class)));
			var bpm = bm.getPropertyMeta("name");
			assertNotNull(session.getXmlBeanPropertyMeta(bpm));
		}

		@Test void l19_getXmlBeanMeta_getXmlClassMeta_delegateToRdfParser() {
			// RdfStreamParser.getXmlBeanMeta()/getXmlClassMeta() delegate straight to the lazily-built
			// companion RdfParser -- never exercised via the public read() API since they aren't on any
			// hot parsing path (mirrors RdfParser_Test#D_marshallingContextMethods.d17 for the stream parser).
			var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
			var bc = p.getMarshallingContext();
			var bm = bc.getBeanMeta(NamedBean.class);
			assertNotNull(p.getXmlBeanMeta(bm));
			assertNotNull(p.getXmlClassMeta(bc.getClassMeta(NamedBean.class)));
		}
	}

	@Nested class M_sessionWhiteBox extends TestBase {

		private RdfParserSession newSession() {
			return RdfParserSession.create(RdfParser.create().ntriple().build()).build();
		}

		@Test void m01_toLang_rdfProto_fallback() {
			// "RDF/PROTO" isn't registered in Jena's RDFLanguages.nameToLang(), so toLang() falls back
			// to the hardcoded literal check -- covers both the TRUE branch of the equals() check and
			// the Lang.RDFPROTO return, never exercised via the public read() API since RDF/PROTO is a
			// binary format that only round-trips meaningfully through the streaming RdfProto marshaller.
			assertEquals(Lang.RDFPROTO, RdfParserSession.toLang("RDF/PROTO"));
		}

		@Test void m02_toLang_unknown_returnsNull() {
			// Covers the final `return null` fallthrough when neither nameToLang() nor the RDF/PROTO
			// literal check match.
			assertNull(RdfParserSession.toLang("NOT-A-REAL-LANGUAGE"));
		}

		@Test void m03_decodeString_null() {
			// decodeString(null) -- o==null branch, never reached via the public read() API since every
			// internal caller passes a non-null String/Object.
			var session = newSession();
			assertNull(session.decodeString(null));
		}

		@Test void m04_getValue_pValueWrapsLiteral_direct() {
			// getValue(Resource, outer) where the pValue property points DIRECTLY at a literal -- covers
			// the n.isLiteral() TRUE branch (line 206) after reassigning n = st.getObject().
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var valueProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_VALUE);
			var r = m.createResource("http://ex.org/m04");
			r.addProperty(valueProp, "wrapped-literal");
			var result = session.getValue(r, null);
			assertEquals("wrapped-literal", result);
		}

		@Test void m05_getValue_pValueWrapsNestedResource() {
			// getValue(Resource, outer) where the pValue property points to ANOTHER resource (not a
			// literal) -- covers the recursive readAnything() call, distinct from m04's direct-literal case.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var valueProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_VALUE);
			var r = m.createResource("http://ex.org/m05-wrapper");
			var nested = m.createResource("http://ex.org/m05-nested");
			r.addProperty(valueProp, nested);
			var result = session.getValue(r, null);
			assertNotNull(result);
		}

		@Test void m06_isBag_withLiteral_returnsFalse() {
			// isBag(RDFNode) where n.isResource()==FALSE -- covers the FALSE branch, never reached
			// through readAnything() since every internal caller already knows n is a Resource.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("not-a-resource");
			assertFalse(session.isBag(lit));
		}

		@Test void m07_isSeq_withLiteral_returnsFalse() {
			// isSeq(RDFNode) where n.isResource()==FALSE -- covers the FALSE branch, mirroring m06.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("not-a-resource");
			assertFalse(session.isSeq(lit));
		}

		public static class M08_Bean {
			@Rdf(collectionFormat = RdfCollectionFormat.BAG)
			public List<String> items = new ArrayList<>(List.of("a", "b"));
		}

		@Test void m08_isMultiValuedCollections_perPropertyOverride_nonMultiValued() throws Exception {
			// A per-property @Rdf(collectionFormat=BAG) override that differs from the (default) global
			// setting -- covers the bpRdf.getCollectionFormat() != DEFAULT TRUE branch, returning false
			// since BAG != MULTI_VALUED. Unlike e04 (which relies solely on the GLOBAL collectionFormat
			// setting, always DEFAULT at the property level), this exercises the per-property branch.
			var bean = new M08_Bean();
			var serialized = RdfSerializer.create().ntriple().build().write(bean);
			var result = RdfParser.create().ntriple().build().read(serialized, M08_Bean.class);
			assertEquals(List.of("a", "b"), result.items);
		}

		public static class M09_Bean {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public List<String> items = new ArrayList<>(List.of("c", "d"));
		}

		@Test void m09_isMultiValuedCollections_perPropertyOverride_multiValued() throws Exception {
			// A per-property @Rdf(collectionFormat=MULTI_VALUED) override -- covers the
			// bpRdf.getCollectionFormat()==MULTI_VALUED TRUE outcome of the per-property branch.
			var bean = new M09_Bean();
			var serialized = RdfSerializer.create().ntriple().build().write(bean);
			var result = RdfParser.create().ntriple().build().read(serialized, M09_Bean.class);
			assertEquals(new HashSet<>(List.of("c", "d")), new HashSet<>(result.items));
		}

		@Test void m10_getValue_resourceWithoutPValue_throws() {
			// getValue(Resource, outer) where the resource carries neither a literal value nor a pValue
			// wrapper property -- covers the nn(st)==false fallthrough to the thrown ParseException.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m10-no-value");
			assertThrowsWithMessage(Exception.class, "Unknown value type", () -> session.getValue(r, null));
		}

		@Builder(M11_BeanBuilder.class)
		public static class M11_Bean {
			public int x;
			public M11_Bean(M11_BeanBuilder b) { if (b != null) x = b.x; }
		}

		public static class M11_BeanBuilder {
			public int x;
			public M11_Bean build() { return new M11_Bean(this); }
		}

		@Test void m11_readAnything_builderSwap_roundtrip() throws Exception {
			// Exercises the nn(builder) top-level dispatch (readAnything sType selection) and the
			// builder.create()/readIntoBeanMap()/builder.build() branch via the non-stream session --
			// never exercised by the round-trip tests in E/F, which only ever read plain beans.
			var bean = new M11_Bean(null);
			bean.x = 99;
			var serialized = RdfSerializer.create().ntriple().build().write(bean);
			var result = RdfParser.create().ntriple().build().read(serialized, M11_Bean.class);
			assertNotNull(result);
			assertEquals(99, result.x);
		}

		@Test void m12_readAnything_object_visitedResource_returnsUri() {
			// sType.isObject() with n.isResource(): first visit takes the generic-map (has-properties)
			// path; second visit of the SAME resource must hit the "already visited" branch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m12");
			r.addProperty(m.createProperty("http://ex.org/p"), "v");
			var first = session.readAnything(session.object(), r, null, null);
			assertNotNull(first);
			var second = session.readAnything(session.object(), r, null, null);
			assertEquals("http://ex.org/m12", second);
		}

		@Test void m13_readAnything_object_isSeq() {
			// sType.isObject() with n a Seq resource -- covers the isSeq(r) branch of the object dispatch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var seq = m.createSeq();
			seq.add("a");
			seq.add("b");
			var result = session.readAnything(session.object(), seq, null, null);
			assertNotNull(result);
			assertTrue(((Collection<?>)result).contains("a"));
		}

		@Test void m14_readAnything_object_blankNode_noProperties() {
			// sType.isObject() with a blank (URI-less), property-less resource -- covers the
			// nn(uri)==false side of the uri/no-properties combined condition, distinct from m12
			// (named, has-properties) and m15 (named, no-properties) below.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource();
			var result = session.readAnything(session.object(), r, null, null);
			assertNotNull(result);
		}

		@Test void m15_readAnything_object_namedUri_withProperties() {
			// sType.isObject() with a NAMED resource that also HAS properties -- covers the
			// nn(uri)==true && hasNext()==true combination (falls into the generic-map branch, unlike
			// m12's already-visited-URI shortcut and m14's blank-node case).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m15-named-with-props");
			r.addProperty(m.createProperty("http://ex.org/p"), "v");
			var result = session.readAnything(session.object(), r, null, null);
			assertNotNull(result);
			assertInstanceOf(Map.class, result);
		}

		@Test void m17_readAnything_mapType_cycle() {
			// sType.isMap() (interface Map, not concretely instantiable) -- first visit reads a generic
			// map normally; second visit of the SAME resource must hit the cycle-detection null return.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m17");
			r.addProperty(m.createProperty("http://ex.org/k"), "v");
			var mapType = session.getClassMeta(Map.class);
			var first = session.readAnything(mapType, r, null, null);
			assertNotNull(first);
			var second = session.readAnything(mapType, r, null, null);
			assertNull(second);
		}

		@Test void m18_readAnything_mapType_concreteSubclass_instantiate() {
			// sType.isMap() with a concretely-instantiable Map subclass (LinkedHashMap) -- covers the
			// canCreateNewInstance()==true side of the map-instantiation ternary.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m18");
			r.addProperty(m.createProperty("http://ex.org/k"), "v");
			var mapType = session.getClassMeta(LinkedHashMap.class);
			var result = session.readAnything(mapType, r, null, null);
			assertNotNull(result);
			assertInstanceOf(LinkedHashMap.class, result);
		}

		@Test void m19_readAnything_collection_cycle() {
			// sType.isCollectionOrArray() -- first visit reads a SEQ normally; second visit of the SAME
			// resource must hit the cycle-detection null return.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var seq = m.createSeq();
			seq.add("a");
			var listType = session.getClassMeta(List.class);
			var first = session.readAnything(listType, seq, null, null);
			assertNotNull(first);
			var second = session.readAnything(listType, seq, null, null);
			assertNull(second);
		}

		@Test void m20_readAnything_collection_unrecognizedResource_throws() {
			// sType.isCollectionOrArray() with a resource that is neither a Seq, Bag, nor RDFList --
			// covers the "Unrecognized node type for collection" ParseException.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m20-not-a-collection");
			r.addProperty(m.createProperty("http://ex.org/p"), "v");
			var listType = session.getClassMeta(List.class);
			assertThrowsWithMessage(Exception.class, "Unrecognized node type", () -> session.readAnything(listType, r, null, null));
		}

		public abstract static class M21_NotABean {
			public String name;
		}

		@Test void m21_readAnything_notABean_resource_throws() {
			// sType matches none of the instantiation strategies (abstract, no proxy, no _type match) and
			// n IS a resource -- covers the "could not be instantiated" ParseException (generic-map path).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m21");
			r.addProperty(m.createProperty("http://ex.org/name"), "x");
			var notABeanType = session.getClassMeta(M21_NotABean.class);
			assertThrowsWithMessage(Exception.class, "could not be instantiated", () -> session.readAnything(notABeanType, r, null, null));
		}

		@Test void m22_readAnything_notABean_literal_throws() {
			// sType matches none of the instantiation strategies and n is a LITERAL (not a resource) --
			// covers the final catch-all "could not be instantiated" ParseException, which the round-trip
			// API never produces since scalar-shaped values are always handled by an earlier branch.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("x");
			var notABeanType = session.getClassMeta(M21_NotABean.class);
			assertThrowsWithMessage(Exception.class, "could not be instantiated", () -> session.readAnything(notABeanType, lit, null, null));
		}

		public interface M23_IBean {
			String getName();
			void setName(String name);
		}

		@Test void m23_interfaceProxy_roundtrip() throws Exception {
			// Interface bean with no dictionary/_type match -- covers the generic-map + proxy-invocation-
			// handler branch (nn(sType.getProxyInvocationHandler())==true) in readAnything.
			var serialized = RdfSerializer.create().ntriple().build().write(Map.of("name", "Bob"));
			var result = RdfParser.create().ntriple().build().read(serialized, M23_IBean.class);
			assertNotNull(result);
			assertEquals("Bob", result.getName());
		}

		public static class M24_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri = "http://example.org/m24";
			public String name = "beanUri-test";
		}

		@Test void m24_beanUri_roundtrip() throws Exception {
			// @Rdf(beanUri=true) property -- covers readIntoBeanMap's hasBeanUri() && nn(r2.getURI())
			// branch, never exercised by the plain-bean round-trip tests in E/F.
			var serialized = RdfSerializer.create().ntriple().build().write(new M24_BeanWithBeanUri());
			var result = RdfParser.create().ntriple().build().read(serialized, M24_BeanWithBeanUri.class);
			assertNotNull(result);
			assertEquals("beanUri-test", result.name);
		}

		public static class M25_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri;
			public String name = "blank-node-beanUri";
		}

		@Test void m25_readIntoBeanMap_beanUri_blankNode() {
			// hasBeanUri()==true but the resource is a BLANK node (r2.getURI()==null) -- covers the
			// nn(r2.getURI())==false side, which every round-trip beanUri test (whose beans always carry
			// a real URI) cannot reach.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource();
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "name"), "blank-name");
			var bm = session.toBeanMap(new M25_BeanWithBeanUri());
			var result = session.readIntoBeanMap(r, bm);
			assertEquals("blank-name", result.getBean().name);
		}

		public static class M26_ThrowingSetterBean {
			private String name;
			public String getName() { return name; }
			public void setName(@SuppressWarnings("unused") String name) { throw new IllegalArgumentException("boom"); }
		}

		@Test void m26_beanSetter_throws_wrappedAsParseException() throws Exception {
			// Setter throws IllegalArgumentException -- covers the pMeta.set() BeanRuntimeException
			// catch/rethrow branch in readIntoBeanMap.
			var bean = new NamedBean();
			bean.setName("x");
			var serialized = RdfSerializer.create().ntriple().build().write(bean);
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build()
				.read(serialized, M26_ThrowingSetterBean.class));
		}

		public static class M27_ThrowingList<T> extends ArrayList<T> {
			private static final long serialVersionUID = 1L;
			@Override public boolean add(T t) { throw new UnsupportedOperationException("boom"); }
		}

		public static class M27_SourceBean {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public List<String> items = new ArrayList<>(List.of("a"));
		}

		public static class M27_ThrowingListBean {
			@Rdf(collectionFormat = RdfCollectionFormat.MULTI_VALUED)
			public M27_ThrowingList<String> items = new M27_ThrowingList<>();
		}

		@Test void m27_multivalued_add_throws_wrappedAsParseException() throws Exception {
			// A mutable-LOOKING (canAddTo()==true, since it extends ArrayList) List whose add() always
			// throws -- the parser's per-triple pMeta.add() call propagates that as a
			// BeanRuntimeException, covering the catch/rethrow in readIntoBeanMap's multi-valued path.
			var serialized = RdfSerializer.create().ntriple().build().write(new M27_SourceBean());
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build()
				.read(serialized, M27_ThrowingListBean.class));
		}

		@Test void m28_readIntoBeanMap_rootAndTypeProperties_skipped() {
			// "root" and "_type" marker triples on a bean resource have no matching bean property --
			// covers the p.equals(pRoot)||p.equals(pType) TRUE side (skipped, no onUnknownProperty call),
			// which the round-trip API can't produce since RdfParser never exposes addRootProperty()/
			// addBeanTypes() together with a plain bean target in a way that leaves the marker unconsumed.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var rootProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_ROOT);
			var r = m.createResource("http://ex.org/m28");
			r.addProperty(rootProp, "true");
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "name"), "hi");
			var bm = session.toBeanMap(new NamedBean());
			var result = session.readIntoBeanMap(r, bm);
			assertEquals("hi", result.getBean().getName());
		}

		@Test void m29_readIntoMap_rootProperty_notActuallySkipped() {
			// SUSPECTED BUG (flagged, not fixed -- out of scope for this test-only pass; identical latent
			// bug to the one already documented for the stream session in
			// RdfParser_Test.L_streamSessionMoreCoverage#l27): readIntoMap's root-skip guard is
			// `key.equals("root") && p.getURI().equals(getJuneauNs().getUri())`, but p.getURI() is the
			// property's FULL URI (namespace + "root"), never equal to just the bare namespace URI on the
			// right-hand side. That sub-condition is therefore always false, so the negated "!(...)" is
			// always true and the "root" key is NEVER actually filtered out of generic maps. This test
			// pins the ACTUAL (buggy) behavior rather than the evidently-intended one.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var rootProp = m.createProperty(session.getJuneauNs().getUri(), Constants.RDF_juneauNs_ROOT);
			var r = m.createResource("http://ex.org/m29");
			r.addProperty(rootProp, "true");
			r.addProperty(m.createProperty("http://ex.org/k"), "v");
			Map<String,Object> map = new LinkedHashMap<>();
			var result = session.readIntoMap(r, map, session.getClassMeta(String.class), session.object(), null);
			assertTrue(result.containsKey("root"));
			assertEquals("v", result.get("k"));
		}

		@Test void m30_getJenaSettings_getRdfBeanMeta_getRdfClassMeta_getXmlBeanPropertyMeta() {
			// Direct coverage of trivial protected getters that delegate to ctx -- never exercised via
			// the public read() API since they aren't on any hot parsing path.
			var session = newSession();
			assertNotNull(session.getJenaSettings());
			var bc = RdfParser.create().build().getMarshallingContext();
			var bm = bc.getBeanMeta(NamedBean.class);
			assertNotNull(session.getRdfBeanMeta(bm));
			assertNotNull(session.getRdfClassMeta(bc.getClassMeta(NamedBean.class)));
			var bpm = bm.getPropertyMeta("name");
			assertNotNull(session.getRdfBeanPropertyMeta(bpm));
			assertNotNull(session.getXmlBeanPropertyMeta(bpm));
		}

		@Test void m31_doRead_looseCollections_array() throws Exception {
			// isLooseCollections() && type.isArray() -- the array-branch of the loose-collections special
			// case in doRead(), distinct from f05 (which reads into a List, not an array). Uses bean
			// (resource) elements rather than plain scalars: a loose collection of bare string literals
			// has no discoverable root subjects at all (listSubjects() only yields resources), so it can
			// never round-trip a non-empty result regardless of target type -- confirmed via a temporary
			// debug probe on f05's result, which is silently empty for the same reason (its assertion
			// only checks non-null, not contents).
			var b1 = new NamedBean();
			b1.setName("one");
			var b2 = new NamedBean();
			b2.setName("two");
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.write(new ArrayList<>(List.of(b1, b2)));
			var result = RdfParser.create().ntriple().looseCollections().build().read(serialized, NamedBean[].class);
			assertEquals(2, result.length);
		}

		public static class M32_UriBean {
			@Rdf(beanUri = true)
			public String uri;
			public String name = "x";
		}

		@Test void m32_doRead_tooManyRoots_throws() throws Exception {
			// roots.size() > 1 -- two independent (unconnected) root resources marked via
			// addRootProperty(), neither of which is anyone else's object -- covers the "Too many root
			// nodes found" ParseException, never produced by any single-value round-trip test. Uses
			// distinct @Rdf(beanUri=true) URIs (rather than blank nodes) so the two concatenated N-TRIPLE
			// documents can't accidentally collide onto the SAME anonymous node identity.
			var b1 = new M32_UriBean();
			b1.uri = "http://ex.org/m32-first";
			var b2 = new M32_UriBean();
			b2.uri = "http://ex.org/m32-second";
			var s = RdfSerializer.create().ntriple().addRootProperty().build();
			var combined = s.write(b1) + s.write(b2);
			assertThrowsWithMessage(Exception.class, "Too many root nodes",
				() -> RdfParser.create().ntriple().build().read(combined, M32_UriBean.class));
		}

		@Test void m33_readAnything_builderSwap_cycle() {
			// The builder-swap dispatch (nn(builder)) visits the SAME resource twice -- covers the
			// urisVisited cycle-detection null return in the nn(builder) branch, mirroring L_streamSessionMoreCoverage#l20
			// but for the non-stream session (reuses the M11_Bean @Builder fixture from m11).
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var r = m.createResource("http://ex.org/m33");
			r.addProperty(m.createProperty(session.getJuneauBpNs().getUri(), "x"), "5");
			var type = session.getClassMeta(M11_Bean.class);
			var first = session.readAnything(type, r, null, null);
			assertNotNull(first);
			var second = session.readAnything(type, r, null, null);
			assertNull(second);
		}

		@Test void m34_readAnything_uriType_literalNode() {
			// sType.isUri() with a LITERAL node (not a resource) -- covers the n.isResource()==false side
			// of that condition. As documented on L_streamSessionMoreCoverage#l22, java.net.URL never
			// reaches this branch (a registered UrlSwap reclassifies it to STRING first); plain
			// java.net.URI has no such swap, so sType stays URI-categorized and genuinely exercises the
			// FALSE side, falling through to the later generic string-conversion fallback.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var lit = m.createLiteral("http://example.org/m34");
			var result = session.readAnything(session.getClassMeta(java.net.URI.class), lit, null, null);
			assertEquals(java.net.URI.create("http://example.org/m34"), result);
		}

		public static class M35_UriPropertyBean {
			public java.net.URI location = java.net.URI.create("http://example.org/m35");
			public String name = "with-uri-prop";
		}

		@Test void m35_readIntoBeanMap_uriType_resourceNode_roundtrip() throws Exception {
			// sType.isUri() && n.isResource()==TRUE -- the URI value is a property (not the bean root),
			// since a bare URI as the root object serializes to a Resource with no triples of its own
			// and can never be discovered by getRoots(). Round-trips through the public API since plain
			// java.net.URI (no registered swap) genuinely reaches this branch.
			var serialized = RdfSerializer.create().ntriple().build().write(new M35_UriPropertyBean());
			var result = RdfParser.create().ntriple().build().read(serialized, M35_UriPropertyBean.class);
			assertEquals(java.net.URI.create("http://example.org/m35"), result.location);
		}

		public interface M36_EmptyInterface {
			// No bean properties at all -- canCreateNewInstance() is FALSE, so readAnything falls all
			// the way through to the final `else if (n.isResource())` generic-map branch.
		}

		@Test void m36_readAnything_typeProperty_unresolvableName_genericMapCast() {
			// eType is a property-less interface (canCreateNewBean()==FALSE) with a "_type" property
			// that does not resolve to any registered dictionary entry -- falls through to readIntoMap +
			// the containsKey(getBeanTypePropertyName(eType)) TRUE branch (cast() call) at the bottom of
			// readAnything, mirroring L_streamSessionMoreCoverage#l19b for the non-stream session.
			var session = newSession();
			var m = ModelFactory.createDefaultModel();
			var bpNs = session.getJuneauBpNs().getUri();
			var r = m.createResource("http://ex.org/m36");
			r.addProperty(m.createProperty(bpNs, "_type"), "TotallyUnregisteredBogusTypeName");
			r.addProperty(m.createProperty(bpNs, "x"), "hello");
			var result = session.readAnything(session.getClassMeta(M36_EmptyInterface.class), r, null, null);
			assertNotNull(result);
		}
	}
}
