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
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
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
	}
}

