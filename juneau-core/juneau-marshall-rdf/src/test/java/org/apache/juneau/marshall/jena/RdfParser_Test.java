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

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
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
			// The 3-param swap always throws since unswapFunction is required
			assertThrows(IllegalArgumentException.class, () ->
				RdfParser.create().swap(Integer.class, String.class, String::valueOf).build()
			);
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

		@Test void e01_trimWhitespace_and_trimStrings_in_actual_parse() throws Exception {
			// Triggers RdfParserSession.decodeString() isTrimWhitespace + isTrimStrings branches
			var serialized = RdfSerializer.create().language("N-TRIPLE").build().serialize("  hello  ");
			var parser = RdfParser.create().language("N-TRIPLE").trimWhitespace().trimStrings().build();
			var result = parser.parse(serialized, String.class);
			assertNotNull(result);
		}

		@Test void e02_parse_list_with_bag_format() throws Exception {
			// Triggers RdfParserSession collectionFormat BAG path
			var list = List.of("a", "b", "c");
			var serialized = RdfSerializer.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.BAG).build().serialize(list);
			var parser = RdfParser.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.BAG).build();
			var result = parser.parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void e03_parse_list_with_seq_format() throws Exception {
			// Triggers RdfParserSession collectionFormat SEQ path
			var list = List.of("x", "y");
			var serialized = RdfSerializer.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.SEQ).build().serialize(list);
			var parser = RdfParser.create().language("N-TRIPLE").collectionFormat(RdfCollectionFormat.SEQ).build();
			var result = parser.parse(serialized, List.class);
			assertNotNull(result);
		}

		public static class BeanWithList {
			public List<String> items = new ArrayList<>();
		}

		@Test void e04_parse_multiValued_collection() throws Exception {
			// Triggers RdfParserSession isMultiValuedCollections branch via a bean property
			var bean = new BeanWithList();
			bean.items = new ArrayList<>(List.of("p", "q"));
			var serialized = RdfSerializer.create().language("TURTLE").collectionFormat(RdfCollectionFormat.MULTI_VALUED).build().serialize(bean);
			var parser = RdfParser.create().language("TURTLE").collectionFormat(RdfCollectionFormat.MULTI_VALUED).build();
			var result = parser.parse(serialized, BeanWithList.class);
			assertNotNull(result);
		}
	}

	@Nested class F_parseVariousTypes extends TestBase {

		@Test void f01_parse_boolean_ntriple() throws Exception {
			// sType.isBoolean() branch in parseAnything (line 345)
			var serialized = RdfSerializer.create().ntriple().build().serialize(true);
			var result = RdfParser.create().ntriple().build().parse(serialized, Boolean.class);
			assertNotNull(result);
		}

		@Test void f02_parse_integer() throws Exception {
			// sType.isNumber() branch in parseAnything (line 343)
			var serialized = RdfSerializer.create().ntriple().build().serialize(42);
			var result = RdfParser.create().ntriple().build().parse(serialized, Integer.class);
			assertNotNull(result);
		}

		@Test void f03_parse_map() throws Exception {
			// sType.isMap() branch in parseAnything (line 314)
			var map = new LinkedHashMap<String,String>();
			map.put("k1", "v1");
			var serialized = RdfSerializer.create().ntriple().build().serialize(map);
			var result = RdfParser.create().ntriple().build().parse(serialized, Map.class);
			assertNotNull(result);
		}

		@Test void f04_parse_string_array() throws Exception {
			// sType.isArray() branch in parseAnything (line 321) — parsed into array via temp list
			var serialized = RdfSerializer.create().ntriple().build().serialize(new ArrayList<>(List.of("a", "b")));
			var result = RdfParser.create().ntriple().build().parse(serialized, String[].class);
			assertNotNull(result);
		}

		@Test void f05_parse_looseCollections() throws Exception {
			// isLooseCollections() path in doParse (line 484)
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.serialize(new ArrayList<>(List.of("x", "y")));
			var result = RdfParser.create().ntriple().looseCollections().build().parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void f06_invalid_language_throws_on_parse() {
			// lang==null path in RdfParserSession constructor — throws on first parse call
			var p = RdfParser.create().language("NOT-A-LANGUAGE").build();
			assertThrows(Exception.class, () -> p.parse("dummy", String.class));
		}

		@Test void f07_parse_list_as_bag_roundtrip() throws Exception {
			// BAG format parse — isBag() path in parseAnything
			var list = new ArrayList<>(List.of("i", "ii", "iii"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build().serialize(list);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void f08_parse_map_as_object_type() throws Exception {
			// sType.isObject() with Resource — exercises the resource-to-JsonMap path
			var map = new LinkedHashMap<String,String>();
			map.put("name", "value");
			var serialized = RdfSerializer.create().ntriple().build().serialize(map);
			var result = RdfParser.create().ntriple().build().parse(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void f09_parse_string_ntriple_trimWhitespace_and_trimStrings() throws Exception {
			// Exercises decodeString() with both isTrimWhitespace() and isTrimStrings() true
			var serialized = RdfSerializer.create().ntriple().build().serialize("  hello  ");
			var result = RdfParser.create().ntriple().trimWhitespace().trimStrings().build()
				.parse(serialized, String.class);
			assertNotNull(result);
		}

		@Test void f10_parse_list_format_list_roundtrip() throws Exception {
			// LIST RDF list collection format — r.canAs(RDFList.class) path in parseAnything
			var list = new ArrayList<>(List.of("a", "b", "c"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build().serialize(list);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void f11_parse_char() throws Exception {
			// sType.isChar() path in parseAnything (line 341)
			var serialized = RdfSerializer.create().ntriple().build().serialize('Z');
			var result = RdfParser.create().ntriple().build().parse(serialized, Character.class);
			assertNotNull(result);
		}

		@Test void f12_parse_url_type() throws Exception {
			// sType.isUri() && n.isResource() path in parseAnything (line 347)
			// java.net.URL is a recognized URI type in Juneau
			var serialized = RdfSerializer.create().ntriple().build().serialize(new java.net.URL("http://example.org/foo"));
			var result = RdfParser.create().ntriple().build().parse(serialized, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void f13_parse_seq_list_into_typed_list() throws Exception {
			// SEQ format into a typed List<String> — uses sType.isCollectionOrArray() path (line 320)
			// with isSeq(r) true (line 328)
			var list = new ArrayList<>(List.of("p", "q", "r"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build().serialize(list);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.parse(serialized, ArrayList.class);
			assertNotNull(result);
		}

		@Test void f14_parse_uri_java_type() throws Exception {
			// sType.isUri() && n.isResource() path in parseAnything (line 347)
			// java.net.URL is a URI type in Juneau
			var url = new java.net.URL("http://example.org/foo");
			var serialized = RdfSerializer.create().ntriple().build().serialize(url);
			var result = RdfParser.create().ntriple().build().parse(serialized, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void f15_parse_multi_valued_into_bean() throws Exception {
			// MULTI_VALUED format → isMultiValuedCollections path (lines 400-405) in parseIntoBeanMap
			var bean = new RdfParser_Test.E_sessionBranchFills.BeanWithList();
			bean.items = new ArrayList<>(List.of("x", "y", "z"));
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build().serialize(bean);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build()
				.parse(serialized, RdfParser_Test.E_sessionBranchFills.BeanWithList.class);
			assertNotNull(result);
		}
	}

	@Nested class G_streamParserBranchFills extends TestBase {

		@Test void g01_stream_parse_string_thrift() throws Exception {			// RdfStreamParserSession — parse String from Thrift binary
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize("hello");
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, String.class);
			assertEquals("hello", result);
		}

		@Test void g02_stream_parse_bean_thrift() throws Exception {
			// RdfStreamParserSession — parse bean from Thrift binary
			var bean = new NamedBean();
			bean.setName("stream-bean");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(bean);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, NamedBean.class);
			assertNotNull(result);
		}

		@Test void g03_stream_parse_list_thrift() throws Exception {
			// RdfStreamParserSession — parse list from Thrift binary
			var list = new ArrayList<>(List.of("a", "b"));
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(list);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, List.class);
			assertNotNull(result);
		}

		@Test void g04_stream_parse_integer() throws Exception {
			// sType.isNumber() branch in RdfStreamParserSession
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(99);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Integer.class);
			assertNotNull(result);
		}

		@Test void g05_stream_parse_map_thrift() throws Exception {
			// sType.isMap() branch in RdfStreamParserSession
			var map = new LinkedHashMap<String,String>();
			map.put("key", "val");
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(map);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Map.class);
			assertNotNull(result);
		}

		@Test void g06_stream_parse_boolean() throws Exception {
			// sType.isBoolean() branch in RdfStreamParserSession
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(true);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, Boolean.class);
			assertNotNull(result);
		}

		@Test void g07_stream_invalid_language_throws() {
			// lang==null path in RdfStreamParserSession constructor
			var p = RdfStreamParser.create().language("INVALID").build();
			assertThrows(Exception.class, () -> p.parse(new byte[0], String.class));
		}

		@Test void g08_stream_roundtrip_array() throws Exception {
			// Array serialization and parsing via Thrift — exercises isArray paths in stream sessions
			var input = new String[]{"p", "q", "r"};
			var bytes = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build().serialize(input);
			var result = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build()
				.parse(bytes, String[].class);
			assertNotNull(result);
		}
	}

	@Nested class H_moreParserBranches extends TestBase {

		@Test void h01_parse_with_root_property() throws Exception {
			// Serialize with addRootProperty(), parse — covers getRoots() loop body (line 167) and early return (line 170)
			var serialized = RdfSerializer.create().ntriple().addRootProperty().build().serialize("hello");
			var result = RdfParser.create().ntriple().build().parse(serialized, String.class);
			assertEquals("hello", result);
		}

		@Test void h02_parse_empty_seq_collection() throws Exception {
			// Serialize empty list with SEQ format — covers parseIntoCollection(Container) empty loop (line 433)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.serialize(new ArrayList<>());
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void h03_parse_empty_list_format() throws Exception {
			// Serialize empty list with LIST format — empty RDF list is rdf:nil, parsed as null (line 442 empty loop)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.serialize(new ArrayList<>());
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.parse(serialized, List.class);
			assertNull(result);
		}

		public static class H04_SimpleBean {
			public String val = "x";
		}

		@Test void h04_parse_loose_collections_as_array() throws Exception {
			// Parse loose collection of beans as typed array — covers lines 486 (isArray), 493 (lambda body), 495-496
			var list = new ArrayList<>(List.of(new H04_SimpleBean(), new H04_SimpleBean()));
			var serialized = RdfSerializer.create().ntriple().looseCollections().build().serialize(list);
			var result = RdfParser.create().ntriple().looseCollections().build()
				.parse(serialized, H04_SimpleBean[].class);
			assertNotNull(result);
		}

		@Test void h05_parse_optional_string() throws Exception {
			// Parse as Optional — covers isOptional() branch in parseAnything (line 248)
			var serialized = RdfSerializer.create().ntriple().build().serialize("opt-value");
			var result = RdfParser.create().ntriple().build().parse(serialized, Optional.class);
			assertNotNull(result);
		}

		public interface H06Animal {}

		@Marshalled(typeName = "H06Dog")
		public static class H06_Dog implements H06Animal {
			public String name = "Buddy";
		}

		@Test void h06_parse_typed_bean_with_addBeanTypesRdf() throws Exception {
			// Manually construct N-Triple RDF with a _type triple; parse as abstract interface →
			// covers lines 253-259 (type property lookup, canCreateNewInstance=false path)
			// The parser finds the _type="H06Dog" triple and instantiates H06_Dog from the dictionary.
			var p = RdfParser.create().ntriple().beanDictionary(H06_Dog.class).build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "_:B1 <" + bpNs + "_type> \"H06Dog\" .\n"
				+ "_:B1 <" + bpNs + "name> \"Buddy\" .\n";
			var result = p.parse(rdf, H06Animal.class);
			assertNotNull(result);
		}		public static class H07_FullBean {
			public String known = "x";
			public String extra = "extra-value";
		}

		public static class H07_PartialBean {
			public String known = "y";
		}

		@Test void h07_parse_bean_unknown_properties_ignored() throws Exception {
			// Parse into bean that lacks some properties — covers line 420 (unknown property path in parseIntoBeanMap)
			var serialized = RdfSerializer.create().ntriple().build().serialize(new H07_FullBean());
			var result = RdfParser.create().ntriple().ignoreUnknownBeanProperties().build()
				.parse(serialized, H07_PartialBean.class);
			assertNotNull(result);
		}

		@Test void h08_parse_date_roundtrip() throws Exception {
			// Serialize Date and parse back — covers line 351 (isDate branch in parseAnything)
			var date = new Date(1000000000L);
			var serialized = RdfSerializer.create().ntriple().build().serialize(date);
			var result = RdfParser.create().ntriple().build().parse(serialized, Date.class);
			assertNotNull(result);
		}

		@Test void h09_parse_calendar_roundtrip() throws Exception {
			// Serialize Calendar and parse back — covers line 353 (isCalendar branch in parseAnything)
			var cal = Calendar.getInstance();
			var serialized = RdfSerializer.create().ntriple().build().serialize(cal);
			var result = RdfParser.create().ntriple().build().parse(serialized, Calendar.class);
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
			var serialized = RdfSerializer.create().ntriple().build().serialize(bean);
			var result = RdfParser.create().ntriple().build().parse(serialized, H10_BeanWithRdfFormat.class);
			assertNotNull(result);
		}

		@Test void h11_parse_empty_rdf_returns_null() throws Exception {
			// Parse empty N-Triple content — roots empty → null returned (line 500-501 null branch)
			var result = RdfParser.create().ntriple().build().parse("", Object.class);
			assertNull(result);
		}

		@Test void h12_parse_empty_rdf_as_optional() throws Exception {
			// Parse empty N-Triple content as Optional — roots empty → opte() returned (line 501 true branch)
			var result = (Optional<?>)RdfParser.create().ntriple().build().parse("", Optional.class);
			assertNotNull(result);
			assertTrue(result.isEmpty());
		}

		@Test void h13_parse_too_many_roots_throws() {
			// N-Triple with two disconnected resources — too many roots → ParseException (line 503-504)
			var rdf = """
					<http://a.example.org/1> <http://p.example.org/prop> "val1" .
					<http://b.example.org/2> <http://p.example.org/prop> "val2" .
					""";
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build().parse(rdf, String.class));
		}
	}

	@SuppressWarnings("java:S5778")
	@Nested class I_parserBranchCovers extends TestBase {

		@Test void i01_parse_self_referential() throws Exception {
			// Self-referential resource (subject == object) — line 181 FALSE branch:
			// object.isResource() is true but object.equals(subject) is true → not added to objects set.
			// The self-referential resource is still a root; recursing into it triggers cycle detection (line 274 TRUE).
			var p = RdfParser.create().ntriple().build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "<http://ex.org/a> <http://ex.org/self> <http://ex.org/a> .\n"
				+ "<http://ex.org/a> <" + bpNs + "name> \"root\" .\n";
			var result = p.parse(rdf, Map.class);
			assertNotNull(result);
		}

		public interface I02TypedInterface {}

		@SuppressWarnings("rawtypes")
		@Test void i02_parse_unknown_type_name() throws Exception {
			// _type property with unresolvable class name — line 258 FALSE: nn(tcm) is false → type unchanged.
			// Map (interface) can't be instantiated → type lookup runs; unknown type → nn(tcm)=false → sType stays Map.
			// Falls through to sType.isMap() path and returns the raw JsonMap.
			var p = RdfParser.create().ntriple().build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "_:B1 <" + bpNs + "_type> \"com.example.DoesNotExist\" .\n"
				+ "_:B1 <" + bpNs + "name> \"test\" .\n";
			Map result = p.parse(rdf, Map.class);
			assertNotNull(result);
		}

		@Test void i03_parse_bag_as_object() throws Exception {
			// BAG collection parsed as Object type — covers isBag(r) TRUE in sType.isObject() path (line 281)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.serialize(List.of("a", "b", "c"));
			var result = RdfParser.create().ntriple().build().parse(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void i04_parse_list_as_object() throws Exception {
			// LIST collection parsed as Object type — covers r.canAs(RDFList.class) TRUE in isObject path (line 284)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.serialize(List.of("x", "y"));
			var result = RdfParser.create().ntriple().build().parse(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void i05_parse_named_uri_resource_with_no_children_as_object() throws Exception {
			// Named URI resource with no child properties — covers line 291 TRUE:
			// nn(uri) && !r.listProperties().hasNext() → o = r.getURI()
			var p = RdfParser.create().ntriple().build();
			var rdf = "<http://ex.org/main> <http://ex.org/ref> <http://ex.org/orphan> .\n"
				+ "<http://ex.org/main> <" + p.getJuneauBpNs().getUri() + "name> \"main\" .\n";
			var result = p.parse(rdf, Map.class);
			assertNotNull(result);
		}

		public static class I06_CycleBean {
			public String name;
			public I06_CycleBean child1;
			public I06_CycleBean child2;
		}

		@Test void i06_parse_shared_blank_node_cycle() throws Exception {
			// Two properties pointing to same blank node — second visit triggers cycle detection (line 310 TRUE):
			// _:A → child1 → _:B; _:A → child2 → _:B; second parse of _:B returns null.
			var p = RdfParser.create().ntriple().build();
			var bpNs = p.getJuneauBpNs().getUri();
			var rdf = "_:A <" + bpNs + "name> \"root\" .\n"
				+ "_:A <" + bpNs + "child1> _:B .\n"
				+ "_:A <" + bpNs + "child2> _:B .\n"
				+ "_:B <" + bpNs + "name> \"shared\" .\n";
			var result = p.parse(rdf, I06_CycleBean.class);
			assertNotNull(result);
		}

		@Test void i07_parse_integer_literal_as_object() throws Exception {
			// Typed literal with Integer value parsed as Object — covers line 269 FALSE:
			// addLiteralTypes() emits xsd:integer; Jena's getValue() returns Integer (not String).
			var serialized = RdfSerializer.create().ntriple().addLiteralTypes().build().serialize(42);
			var result = RdfParser.create().ntriple().build().parse(serialized, Object.class);
			assertNotNull(result);
		}

		@Test void i08_parse_instant_temporal() throws Exception {
			// Serialize Instant and parse back — covers isTemporal() branch (line 355)
			var now = Instant.now();
			var serialized = RdfSerializer.create().ntriple().build().serialize(now);
			var result = RdfParser.create().ntriple().build().parse(serialized, Instant.class);
			assertNotNull(result);
		}

		@Test void i09_parse_duration() throws Exception {
			// Serialize Duration and parse back — covers isDuration() branch (line 357)
			var d = Duration.ofHours(3);
			var serialized = RdfSerializer.create().ntriple().build().serialize(d);
			var result = RdfParser.create().ntriple().build().parse(serialized, Duration.class);
			assertNotNull(result);
		}

		@Test void i10_parse_period() throws Exception {
			// Serialize Period and parse back — covers isPeriod() branch (line 359)
			var period = Period.ofDays(7);
			var serialized = RdfSerializer.create().ntriple().build().serialize(period);
			var result = RdfParser.create().ntriple().build().parse(serialized, Period.class);
			assertNotNull(result);
		}

		@Test void i11_parse_url_roundtrip() throws Exception {
			// Serialize URL and parse back — covers sType.isUri() && n.isResource() branch (line 347)
			var url = new java.net.URL("http://example.org/test-i11");
			var serialized = RdfSerializer.create().ntriple().build().serialize(url);
			var result = RdfParser.create().ntriple().build().parse(serialized, java.net.URL.class);
			assertNotNull(result);
		}

		@Test void i13_parse_nonempty_seq_as_list() throws Exception {
			// Non-empty SEQ parsed as List — exercises Container iterator loop body (line 433)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.serialize(List.of("p", "q", "r"));
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.SEQ).build()
				.parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void i14_parse_nonempty_bag_as_list() throws Exception {
			// Non-empty BAG parsed as List — covers isBag(r) TRUE in collection path (line 330) and loop body (line 433)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.serialize(List.of("x", "y", "z"));
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.BAG).build()
				.parse(serialized, List.class);
			assertNotNull(result);
		}

		@Test void i15_parse_nonempty_list_format_as_list() throws Exception {
			// Non-empty LIST format parsed as List — covers r.canAs(RDFList) TRUE in collection path (line 332)
			// and RDFList iterator loop body (line 442)
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.serialize(List.of("u", "v", "w"));
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.LIST).build()
				.parse(serialized, List.class);
			assertNotNull(result);
		}

		public static class I16_BeanWithBeanUri {
			@Rdf(beanUri = true)
			public String uri = "http://example.org/i16";
			public String name = "beanUri-test";
		}

		@Test void i16_parse_bean_with_bean_uri() throws Exception {
			// Serialize bean with @Rdf(beanUri=true) and parse back — covers parseIntoBeanMap
			// line 389 TRUE: hasBeanUri() && nn(r2.getURI()) → sets the beanUri property from the resource URI.
			var serialized = RdfSerializer.create().ntriple().build().serialize(new I16_BeanWithBeanUri());
			var result = RdfParser.create().ntriple().build().parse(serialized, I16_BeanWithBeanUri.class);
			assertNotNull(result);
		}

		@SuppressWarnings("rawtypes")
		@Test void i17_parse_map_with_root_property() throws Exception {
			// Map serialized with addRootProperty() — parseIntoMap skips the root triple (line 459 FALSE:
			// key.equals("root") && p.getURI().equals(juneauNs) → condition TRUE → skip via !(...) = FALSE)
			var serialized = RdfSerializer.create().ntriple().addRootProperty().build()
				.serialize(Map.of("name", "test"));
			Map result = RdfParser.create().ntriple().build().parse(serialized, Map.class);
			assertNotNull(result);
		}

		@Test void i18_parse_string_from_resource_without_pvalue_throws() {
			// Named resource without pValue wrapper parsed as String — triggers getValue():
			// n.isLiteral()=FALSE (line 193), n.isResource()=TRUE (line 195), nn(st)=FALSE (line 197) → ParseException
			var rdf = "<http://ex.org/a> <http://ex.org/p> \"x\" .\n";
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build().parse(rdf, String.class));
		}

		public static class I19_BeanWithListNoAnnotation {
			public List<String> tags = new ArrayList<>(List.of("a", "b"));
		}

		@Test void i19_parse_multivalue_global_format() throws Exception {
			// Collection property with no @Rdf annotation; global MULTI_VALUED format →
			// isMultiValuedCollections() FALSE branch (line 222): falls through to getCollectionFormat() check.
			var bean = new I19_BeanWithListNoAnnotation();
			var serialized = RdfSerializer.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build()
				.serialize(bean);
			var result = RdfParser.create().ntriple().collectionFormat(RdfCollectionFormat.MULTI_VALUED).build()
				.parse(serialized, I19_BeanWithListNoAnnotation.class);
			assertNotNull(result);
		}

		@Test void i20_parse_loose_collections_list() throws Exception {
			// looseCollections() → doParse line 484 TRUE: multiple root nodes added to a List
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.serialize(new ArrayList<>(List.of("x", "y", "z")));
			var result = RdfParser.create().ntriple().looseCollections().build()
				.parse(serialized, ArrayList.class);
			assertNotNull(result);
		}

		@Test void i21_parse_loose_collections_array() throws Exception {
			// looseCollections() + array type → doParse line 486-496: isArray()=TRUE → toArray()
			var serialized = RdfSerializer.create().ntriple().looseCollections().build()
				.serialize(new String[]{"a", "b"});
			var result = RdfParser.create().ntriple().looseCollections().build()
				.parse(serialized, String[].class);
			assertNotNull(result);
		}

		@Test void i22_parse_empty_model_as_optional() throws Exception {
			// Empty input → getRoots() returns empty → doParse line 501 TRUE (type.isOptional()):
			// result is Optional.empty()
			var empty = "";
			var result = (Optional<?>)RdfParser.create().ntriple().build().parse(empty, Optional.class);
			assertNotNull(result);
			assertTrue(result.isEmpty());
		}

		@Test void i23_parse_empty_model_non_optional() throws Exception {
			// Empty input → getRoots() returns empty → doParse line 501 FALSE (!type.isOptional()):
			// result is null
			var empty = "";
			var result = RdfParser.create().ntriple().build().parse(empty, String.class);
			assertNull(result);
		}

		@Test void i24_parse_multiple_roots_throws() {
			// Two unconnected resources → getRoots() returns 2 → doParse line 503-504 throws ParseException
			var rdf = """
					<http://ex.org/a> <http://ex.org/p> "x" .
					<http://ex.org/b> <http://ex.org/p> "y" .
					""";
			assertThrows(Exception.class, () -> RdfParser.create().ntriple().build().parse(rdf, String.class));
		}

		@Test void i25_parse_char_type() throws Exception {
			// sType.isChar() branch (line 342) — parse character from serialized single-char bean property
			var serialized = RdfSerializer.create().ntriple().build().serialize('A');
			var result = RdfParser.create().ntriple().build().parse(serialized, Character.class);
			assertEquals('A', result);
		}

		@Test void i26_parse_boolean_type() throws Exception {
			// sType.isBoolean() branch (line 346) — parse boolean round-trip
			var serialized = RdfSerializer.create().ntriple().build().serialize(true);
			var result = RdfParser.create().ntriple().build().parse(serialized, Boolean.class);
			assertEquals(Boolean.TRUE, result);
		}

		@Test void i27_parse_integer_type() throws Exception {
			// sType.isNumber() branch (line 344) — parse integer round-trip
			var serialized = RdfSerializer.create().ntriple().build().serialize(42);
			var result = RdfParser.create().ntriple().build().parse(serialized, Integer.class);
			assertEquals(42, result);
		}

		public static class I28_SimpleBean {
			public String name;
		}

		@Test void i28_parse_loose_collections_non_collection_type() throws Exception {
			// looseCollections()=true, type is NOT a collection → condition FALSE (branch 2):
			// isLooseCollections()=true && isCollectionOrArray()=false → falls through to single-root path
			var b = new I28_SimpleBean();
			b.name = "loose-bean";
			var serialized = RdfSerializer.create().ntriple().looseCollections().build().serialize(b);
			var result = RdfParser.create().ntriple().looseCollections().build()
				.parse(serialized, I28_SimpleBean.class);
			assertNotNull(result);
		}

		@Test void i29_parse_bean_after_add_root_property() throws Exception {
			// Bean serialized with addRootProperty() → parseIntoBeanMap encounters pRoot predicate →
			// else if NOT(p.equals(pRoot) || p.equals(pType)): condition TRUE for root → skip (p.equals(pRoot)=TRUE)
			var b = new I28_SimpleBean();
			b.name = "with-root";
			var serialized = RdfSerializer.create().ntriple().addRootProperty().build().serialize(b);
			var result = RdfParser.create().ntriple().build().parse(serialized, I28_SimpleBean.class);
			assertNotNull(result);
		}

		@Test void i30_parse_trimstrings() throws Exception {
			// trimStrings()=true → decodeString TRUE branch: s.trim() called on the decoded value
			var b = new I28_SimpleBean();
			b.name = " padded ";
			var serialized = RdfSerializer.create().ntriple().build().serialize(b);
			var result = RdfParser.create().ntriple().trimStrings().build()
				.parse(serialized, I28_SimpleBean.class);
			assertNotNull(result);
		}
	}
}
