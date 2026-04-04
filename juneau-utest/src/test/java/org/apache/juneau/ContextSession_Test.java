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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class ContextSession_Test extends TestBase {

	//====================================================================================================
	// ContextSession.Builder.property() - Lines 142-149
	//====================================================================================================

	@Test void a01_property_addProperty() {
		// Test line 147: adding a property with non-null value
		var session = BeanContext.DEFAULT.createSession()
			.property("key1", "value1")
			.property("key2", 123)
			.build();
		var props = session.getSessionProperties();
		assertEquals("value1", props.get("key1"));
		assertEquals(123, props.get("key2"));
	}

	@Test void a02_property_removeProperty() {
		// Test line 145: removing a property by setting value to null
		var session = BeanContext.DEFAULT.createSession()
			.property("key1", "value1")
			.property("key2", "value2")
			.property("key1", null)  // Remove key1
			.build();
		var props = session.getSessionProperties();
		assertFalse(props.containsKey("key1"));
		assertEquals("value2", props.get("key2"));
	}

	@Test void a03_property_nullKey() {
		// Test line 142: assertArgNotNull on key
		var session = BeanContext.DEFAULT.createSession();
		assertThrows(IllegalArgumentException.class, () -> {
			session.property(null, "value");
		});
	}

	//====================================================================================================
	// ContextSession constructor - Line 185
	//====================================================================================================

	@Test void b01_unmodifiableSession_emptyProperties() {
		// Test line 185: unmodifiable session with empty properties should use Collections.emptyMap()
		// Note: The actual implementation may wrap empty maps differently, so we test the behavior
		var session = BeanContext.DEFAULT.createSession()
			.unmodifiable()
			.build();
		var props = session.getSessionProperties();
		// Test line 185: if properties are empty, should use Collections.emptyMap() or equivalent
		// The key test is that it's unmodifiable and empty
		assertTrue(props.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> {
			props.put("key", "value");
		});
	}

	@Test void b02_unmodifiableSession_withProperties() {
		// Test line 185: unmodifiable session with properties should use unmodifiable map
		var session = BeanContext.DEFAULT.createSession()
			.property("key1", "value1")
			.property("key2", "value2")
			.unmodifiable()
			.build();
		var props = session.getSessionProperties();
		assertEquals("value1", props.get("key1"));
		assertEquals("value2", props.get("key2"));
		// Verify it's unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> {
			props.put("key3", "value3");
		});
	}

	//====================================================================================================
	// ContextSession.addWarning() - Line 201
	//====================================================================================================

	@Test void c01_addWarning_unmodifiableSession() {
		// Test line 201: addWarning should return early if session is unmodifiable
		var session = BeanContext.DEFAULT.createSession()
			.unmodifiable()
			.build();
		// Should not throw exception, just return early
		session.addWarning("Test warning");
		assertTrue(session.getWarnings().isEmpty());
	}

	@Test void c02_addWarning_modifiableSession() {
		// Test that addWarning works on modifiable sessions
		var session = BeanContext.DEFAULT.createSession()
			.build();
		session.addWarning("Test warning");
		var warnings = session.getWarnings();
		assertFalse(warnings.isEmpty());
		assertTrue(warnings.get(0).contains("Test warning"));
	}

	//====================================================================================================
	// ContextSession.getContext() - Line 220
	//====================================================================================================

	@Test void d01_getContext() {
		// Test line 220: getContext() returns the context that created the session
		var context = BeanContext.DEFAULT;
		var session = context.createSession().build();
		assertSame(context, session.getContext());
	}

	//====================================================================================================
	// ContextSession.Builder.property() dispatch - typed property keys
	//====================================================================================================

	@Nested class E_propertyDispatch extends TestBase {

		public static class BeanWithNullField {
			public String a = null;
			public int b = 1;
		}

		public static class BeanWithEmptyCollection {
			public String[] a = new String[0];
			public int b = 1;
		}

		public static class BeanWithEmptyMap {
			public Map<String,Object> a = new LinkedHashMap<>();
			public int b = 1;
		}

		// -- ContextSession: debug --

		@Test void e01_debug_shortForm() {
			var session = BeanContext.DEFAULT.createSession()
				.property("debug", true)
				.build();
			assertTrue(session.isDebug());
		}

		@Test void e02_debug_qualifiedForm() {
			var session = BeanContext.DEFAULT.createSession()
				.property("ContextSession.debug", "true")
				.build();
			assertTrue(session.isDebug());
		}

		@Test void e03_debug_nullResetsToDefault() {
			var session = BeanContext.DEFAULT.createSession()
				.property("debug", null)
				.build();
			assertEquals(BeanContext.DEFAULT.isDebug(), session.isDebug());
		}

		// -- BeanSession: locale --

		@Test void e04_locale_shortForm_string() {
			var session = BeanContext.DEFAULT.createSession()
				.property("locale", "fr-FR")
				.build();
			assertEquals(Locale.forLanguageTag("fr-FR"), session.getLocale());
		}

		@Test void e05_locale_qualifiedForm() {
			var session = BeanContext.DEFAULT.createSession()
				.property("BeanSession.locale", Locale.GERMAN)
				.build();
			assertEquals(Locale.GERMAN, session.getLocale());
		}

		// -- BeanSession: timeZone --

		@Test void e06_timeZone_shortForm_string() {
			var session = BeanContext.DEFAULT.createSession()
				.property("timeZone", "America/New_York")
				.build();
			assertEquals(TimeZone.getTimeZone("America/New_York"), session.getTimeZone());
		}

		@Test void e07_timeZone_qualifiedForm() {
			var tz = TimeZone.getTimeZone("UTC");
			var session = BeanContext.DEFAULT.createSession()
				.property("BeanSession.timeZone", tz)
				.build();
			assertEquals(tz, session.getTimeZone());
		}

		// -- BeanSession: mediaType --

		@Test void e08_mediaType_shortForm_string() {
			var session = BeanContext.DEFAULT.createSession()
				.property("mediaType", "application/json")
				.build();
			assertEquals("application/json", session.getMediaType().toString());
		}

		@Test void e09_mediaType_qualifiedForm() {
			var session = BeanContext.DEFAULT.createSession()
				.property("BeanSession.mediaType", "text/xml")
				.build();
			assertEquals("text/xml", session.getMediaType().toString());
		}

		// -- WriterSerializerSession: fileCharset, streamCharset, useWhitespace --

		@Test void e10_writerSerializer_fileCharset_shortForm() {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("fileCharset", "UTF-16")
				.build();
			assertEquals(Charset.forName("UTF-16"), session.getFileCharset());
		}

		@Test void e11_writerSerializer_fileCharset_qualifiedForm() {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("WriterSerializerSession.fileCharset", Charset.forName("ISO-8859-1"))
				.build();
			assertEquals(Charset.forName("ISO-8859-1"), session.getFileCharset());
		}

		@Test void e12_writerSerializer_streamCharset_shortForm() {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("streamCharset", "UTF-16")
				.build();
			assertEquals(Charset.forName("UTF-16"), session.getStreamCharset());
		}

		@Test void e13_writerSerializer_streamCharset_qualifiedForm() {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("WriterSerializerSession.streamCharset", "ISO-8859-1")
				.build();
			assertEquals(Charset.forName("ISO-8859-1"), session.getStreamCharset());
		}

		// -- ReaderParserSession: fileCharset, streamCharset --

		@Test void e14_readerParser_fileCharset_shortForm() {
			var session = (ReaderParserSession) JsonParser.DEFAULT.createSession()
				.property("fileCharset", "UTF-16")
				.build();
			assertEquals(Charset.forName("UTF-16"), session.getFileCharset());
		}

		@Test void e15_readerParser_streamCharset_qualifiedForm() {
			var session = (ReaderParserSession) JsonParser.DEFAULT.createSession()
				.property("ReaderParserSession.streamCharset", "ISO-8859-1")
				.build();
			assertEquals(Charset.forName("ISO-8859-1"), session.getStreamCharset());
		}

		// -- properties(Map) delegates to property() --

		@Test void e16_properties_map_dispatchesKnownKeys() {
			var session = BeanContext.DEFAULT.createSession()
				.properties(JsonMap.of("locale", "de-DE", "timeZone", "Europe/Berlin", "unknownKey", "someValue"))
				.build();
			assertEquals(Locale.forLanguageTag("de-DE"), session.getLocale());
			assertEquals(TimeZone.getTimeZone("Europe/Berlin"), session.getTimeZone());
			assertEquals("someValue", session.getSessionProperties().get("unknownKey"));
		}

		@Test void e17_properties_map_resetsPreviousRawProperties() {
			var session = BeanContext.DEFAULT.createSession()
				.property("rawKey1", "value1")
				.properties(JsonMap.of("rawKey2", "value2"))
				.build();
			// After properties(Map), the raw map was reset so rawKey1 should be gone
			assertFalse(session.getSessionProperties().containsKey("rawKey1"));
			assertEquals("value2", session.getSessionProperties().get("rawKey2"));
		}

		// -- Unknown keys fall through to the raw properties map --

		@Test void e18_unknownKey_goesToRawMap() {
			var session = BeanContext.DEFAULT.createSession()
				.property("myCustomKey", "myCustomValue")
				.build();
			assertEquals("myCustomValue", session.getSessionProperties().get("myCustomKey"));
		}

		// -- Locale inheritance through serializer session chain --

		@Test void e19_locale_dispatchedThroughSerializerChain() {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("locale", "ja-JP")
				.build();
			assertEquals(Locale.forLanguageTag("ja-JP"), session.getLocale());
		}

		// -- BeanTraverseSession: initialDepth --

		@Test void e20_beanTraverse_initialDepth_shortForm() {
			var session = (BeanTraverseSession) JsonSerializer.DEFAULT.createSession()
				.property("initialDepth", 3)
				.build();
			assertEquals(3, session.indent);
		}

		@Test void e21_beanTraverse_initialDepth_qualifiedForm() {
			var session = (BeanTraverseSession) JsonSerializer.DEFAULT.createSession()
				.property("BeanTraverseSession.initialDepth", "5")
				.build();
			assertEquals(5, session.indent);
		}

		// -- SerializerSession: trimStrings --

		@Test void e22_serializer_trimStrings_shortForm() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("trimStrings", true)
				.build();
			assertEquals("\"hello\"", session.serialize("  hello  "));
		}

		@Test void e23_serializer_trimStrings_qualifiedForm() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("SerializerSession.trimStrings", "true")
				.build();
			assertEquals("\"hello\"", session.serialize("  hello  "));
		}

		// -- SerializerSession: keepNullProperties --

		@Test void e24_serializer_keepNullProperties_true() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("keepNullProperties", true)
				.build();
			var result = session.serialize(new BeanWithNullField());
			assertTrue(result.contains("\"a\""));
		}

		@Test void e25_serializer_keepNullProperties_false() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("SerializerSession.keepNullProperties", "false")
				.build();
			var result = session.serialize(new BeanWithNullField());
			assertFalse(result.contains("\"a\""));
		}

		// -- SerializerSession: sortCollections --

		@Test void e26_serializer_sortCollections() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("sortCollections", true)
				.build();
			var result = session.serialize(new String[]{"c", "a", "b"});
			assertEquals("[\"a\",\"b\",\"c\"]", result);
		}

		// -- SerializerSession: sortMaps --

		@Test void e27_serializer_sortMaps() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("SerializerSession.sortMaps", "true")
				.build();
			var m = new LinkedHashMap<String,Integer>();
			m.put("c", 3);
			m.put("a", 1);
			m.put("b", 2);
			var result = session.serialize(m);
			assertEquals("{\"a\":1,\"b\":2,\"c\":3}", result);
		}

		// -- SerializerSession: trimEmptyCollections --

		@Test void e28_serializer_trimEmptyCollections() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("trimEmptyCollections", true)
				.build();
			var result = session.serialize(new BeanWithEmptyCollection());
			assertFalse(result.contains("\"a\""));
			assertTrue(result.contains("\"b\""));
		}

		// -- SerializerSession: trimEmptyMaps --

		@Test void e29_serializer_trimEmptyMaps() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("trimEmptyMaps", true)
				.build();
			var result = session.serialize(new BeanWithEmptyMap());
			assertFalse(result.contains("\"a\""));
			assertTrue(result.contains("\"b\""));
		}

		// -- WriterSerializerSession: maxIndent --

		@Test void e30_writerSerializer_maxIndent_shortForm() throws Exception {
			var ctx = JsonSerializer.create().useWhitespace().build();
			var noLimitSession = (WriterSerializerSession) ctx.createSession().build();
			var limitedSession = (WriterSerializerSession) ctx.createSession()
				.property("maxIndent", 0)
				.build();
			// With maxIndent=0, output should have no leading spaces on values
			var unlimited = noLimitSession.serialize(JsonMap.of("a", 1));
			var limited = limitedSession.serialize(JsonMap.of("a", 1));
			assertTrue(unlimited.contains("\t"));
			assertFalse(limited.contains("\t"));
		}

		@Test void e31_writerSerializer_maxIndent_qualifiedForm() throws Exception {
			var ctx = JsonSerializer.create().useWhitespace().build();
			var session = (WriterSerializerSession) ctx.createSession()
				.property("WriterSerializerSession.maxIndent", "0")
				.build();
			var result = session.serialize(JsonMap.of("a", 1));
			assertFalse(result.contains("\t"));
		}

		// -- WriterSerializerSession: quoteChar --

		@Test void e32_writerSerializer_quoteChar_shortForm() throws Exception {
			var session = (WriterSerializerSession) Json5Serializer.DEFAULT.createSession()
				.property("quoteChar", '"')
				.build();
			var result = session.serialize("hello");
			assertEquals("\"hello\"", result);
		}

		@Test void e33_writerSerializer_quoteChar_qualifiedForm() throws Exception {
			var session = (WriterSerializerSession) Json5Serializer.DEFAULT.createSession()
				.property("WriterSerializerSession.quoteChar", "\"")
				.build();
			var result = session.serialize("hello");
			assertEquals("\"hello\"", result);
		}

		// -- CsvSerializerSession: nullValue --

		@Test void e34_csv_nullValue_shortForm() throws Exception {
			var session = CsvSerializer.DEFAULT.createSession()
				.property("nullValue", "N/A")
				.build();
			var result = session.serialize(JsonMap.of("a", null));
			assertTrue(result.contains("N/A"));
		}

		@Test void e35_csv_nullValue_qualifiedForm() throws Exception {
			var session = CsvSerializer.DEFAULT.createSession()
				.property("CsvSerializerSession.nullValue", "EMPTY")
				.build();
			var result = session.serialize(JsonMap.of("a", null));
			assertTrue(result.contains("EMPTY"));
		}

		// -- CsvSerializerSession: byteArrayFormat --

		@Test void e36_csv_byteArrayFormat_shortForm() throws Exception {
			var session = CsvSerializer.DEFAULT.createSession()
				.property("byteArrayFormat", ByteArrayFormat.SEMICOLON_DELIMITED)
				.build();
			var result = session.serialize(new byte[]{1, 2, 3});
			assertTrue(result.contains("1;2;3"));
		}

		// -- ParserSession: trimStrings --

		@Test void e37_parser_trimStrings_shortForm() throws Exception {
			var session = JsonParser.DEFAULT.createSession()
				.property("trimStrings", true)
				.build();
			var result = session.parse("\" hello \"", String.class);
			assertEquals("hello", result);
		}

		@Test void e38_parser_trimStrings_qualifiedForm() throws Exception {
			var session = JsonParser.DEFAULT.createSession()
				.property("ParserSession.trimStrings", "true")
				.build();
			var result = session.parse("\" hello \"", String.class);
			assertEquals("hello", result);
		}

		// -- JsonSerializerSession: escapeSolidus --

		@Test void e39_json_escapeSolidus_shortForm() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("escapeSolidus", true)
				.build();
			var result = session.serialize("http://example.com");
			assertTrue(result.contains("\\/"));
		}

		@Test void e40_json_escapeSolidus_qualifiedForm() throws Exception {
			var session = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
				.property("JsonSerializerSession.escapeSolidus", "true")
				.build();
			var result = session.serialize("http://example.com");
			assertTrue(result.contains("\\/"));
		}

		// -- JsonParserSession: validateEnd --

		@Test void e41_json_validateEnd_shortForm() throws Exception {
			var session = JsonParser.DEFAULT.createSession()
				.property("validateEnd", true)
				.build();
			// With validateEnd=true, trailing content should throw an error
			assertThrows(Exception.class, () -> session.parse("{\"a\":1}extra", JsonMap.class));
		}

		// -- UonParserSession: validateEnd --

		@Test void e42_uon_validateEnd_shortForm() throws Exception {
			var session = UonParser.DEFAULT.createSession()
				.property("validateEnd", true)
				.build();
			assertThrows(Exception.class, () -> session.parse("(a=1)extra", JsonMap.class));
		}

		// -- UonSerializerSession: encoding --

		@Test void e43_uon_encoding_shortForm() throws Exception {
			var session = (WriterSerializerSession) UonSerializer.DEFAULT.createSession()
				.property("encoding", true)
				.build();
			var result = session.serialize("hello world");
			assertTrue(result.contains("%20") || result.contains("+"));
		}

		// -- UonSerializerSession: paramFormat --

		@Test void e44_uon_paramFormat_shortForm() throws Exception {
			var session = (WriterSerializerSession) UonSerializer.DEFAULT.createSession()
				.property("paramFormat", ParamFormat.PLAINTEXT)
				.build();
			var result = session.serialize("hello");
			assertEquals("hello", result);
		}

		// -- UrlEncodingSerializerSession: expandedParams --

		@Test void e45_urlEncoding_expandedParams_shortForm() throws Exception {
			var session = (WriterSerializerSession) UrlEncodingSerializer.DEFAULT.createSession()
				.property("expandedParams", true)
				.build();
			var result = session.serialize(JsonMap.of("a", new int[]{1, 2, 3}));
			assertTrue(result.contains("a=1") && result.contains("a=2") && result.contains("a=3"));
		}

		// -- XmlSerializerSession: enableNamespaces --

		@Test void e46_xml_enableNamespaces_shortForm() throws Exception {
			var session = (WriterSerializerSession) XmlSerializer.DEFAULT.createSession()
				.property("enableNamespaces", false)
				.build();
			var result = session.serialize(JsonMap.of("a", 1));
			assertFalse(result.contains("xmlns"));
		}

		// -- XmlParserSession: preserveRootElement --

		@Test void e47_xml_preserveRootElement_shortForm() throws Exception {
			var session = XmlParser.DEFAULT.createSession()
				.property("preserveRootElement", true)
				.build();
			var result = session.parse("<root><a>1</a></root>", JsonMap.class);
			assertTrue(result.containsKey("root"));
		}

		// -- HtmlSerializerSession: detectLinksInStrings --

		@Test void e48_html_detectLinksInStrings_shortForm() throws Exception {
			var session = (WriterSerializerSession) HtmlSerializer.DEFAULT.createSession()
				.property("detectLinksInStrings", false)
				.build();
			var result = session.serialize("http://example.com");
			assertFalse(result.contains("<a "));
		}

		// -- HtmlSerializerSession: labelParameter --

		@Test void e49_html_labelParameter_shortForm() throws Exception {
			var session = (WriterSerializerSession) HtmlSerializer.DEFAULT.createSession()
				.property("labelParameter", "customlabel")
				.build();
			var result = session.serialize("http://example.com?customlabel=MyLink");
			assertTrue(result.contains("MyLink"));
		}
	}
}

