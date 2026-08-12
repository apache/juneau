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
package org.apache.juneau.marshall.serializer;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link WriterSerializerSession}, exercised via the concrete {@link JsonSerializer}.
 */
class WriterSerializerSession_Test extends TestBase {

	//====================================================================================================
	// a. Builder.property(String,Object) dispatch
	//====================================================================================================

	@Test void a01_property_nullKeyDelegatesToBase() {
		var b = JsonSerializer.DEFAULT.createSession();
		assertThrows(IllegalArgumentException.class, () -> b.property(null, "x"));
	}

	@Test void a02_property_streamCharset() {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("streamCharset", "UTF-16")
			.build();
		assertEquals(StandardCharsets.UTF_16, s.getStreamCharset());
	}

	@Test void a03_property_streamCharset_qualified() {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("WriterSerializerSession.streamCharset", "UTF-16")
			.build();
		assertEquals(StandardCharsets.UTF_16, s.getStreamCharset());
	}

	@Test void a04_property_useWhitespace() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("useWhitespace", "true")
			.build();
		assertTrue((boolean) getProtected(s, "isUseWhitespace"));
	}

	@Test void a05_property_useWhitespace_qualified() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("WriterSerializerSession.useWhitespace", "true")
			.build();
		assertTrue((boolean) getProtected(s, "isUseWhitespace"));
	}

	@Test void a06_property_maxIndent() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("maxIndent", "5")
			.build();
		assertEquals(5, (int) getProtected(s, "getMaxIndent"));
	}

	@Test void a07_property_maxIndent_qualified() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("WriterSerializerSession.maxIndent", "5")
			.build();
		assertEquals(5, (int) getProtected(s, "getMaxIndent"));
	}

	@Test void a08_property_quoteChar() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("quoteChar", "'")
			.build();
		assertEquals('\'', (char) getProtected(s, "getQuoteChar"));
	}

	@Test void a09_property_quoteChar_qualified() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("WriterSerializerSession.quoteChar", "'")
			.build();
		assertEquals('\'', (char) getProtected(s, "getQuoteChar"));
	}

	@Test void a10_property_unknownKeyFallsThroughToBase() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("someUnknownKey", "v")
			.build();
		assertNotNull(s);
	}

	//====================================================================================================
	// b. streamCharset(Charset)/useWhitespace(Boolean) null-guard (nn() checks)
	//====================================================================================================

	@Test void b01_streamCharset_nullIgnored_defaultsToUtf8() {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.streamCharset(null)
			.build();
		assertEquals(StandardCharsets.UTF_8, s.getStreamCharset());
	}

	@Test void b02_streamCharset_nonNullApplied() {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.streamCharset(StandardCharsets.ISO_8859_1)
			.build();
		assertEquals(StandardCharsets.ISO_8859_1, s.getStreamCharset());
	}

	@Test void b03_useWhitespace_nullIgnored() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.useWhitespace(null)
			.build();
		assertFalse((boolean) getProtected(s, "isUseWhitespace"));
	}

	@Test void b04_useWhitespace_nonNullApplied() throws Exception {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.useWhitespace(true)
			.build();
		assertTrue((boolean) getProtected(s, "isUseWhitespace"));
	}

	//====================================================================================================
	// c. write(Object) convenience method and writeToString(Object) delegate
	//====================================================================================================

	@Test void c01_write_toStringConvenience() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var result = s.write("hello");
		assertEquals("\"hello\"", result);
	}

	@Test void c02_writeToString_delegatesToWrite() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		assertEquals(s.write("hello"), s.writeToString("hello"));
	}

	//====================================================================================================
	// d. properties() - reached polymorphically through ContextSession.toString()
	//====================================================================================================

	@Test void d01_properties_surfacedViaToString() {
		var s = (WriterSerializerSession) JsonSerializer.DEFAULT.createSession()
			.streamCharset(StandardCharsets.ISO_8859_1)
			.useWhitespace(true)
			.maxIndent(3)
			.quoteChar('\'')
			.build();
		var str = s.toString();
		assertTrue(str.contains("streamCharset"), () -> "Expected streamCharset in: " + str);
		assertTrue(str.contains("useWhitespace"), () -> "Expected useWhitespace in: " + str);
		assertTrue(str.contains("maxIndent"), () -> "Expected maxIndent in: " + str);
		assertTrue(str.contains("quoteChar"), () -> "Expected quoteChar in: " + str);
	}

	//====================================================================================================
	// e. isWriterSerializer()
	//====================================================================================================

	@Test void e01_isWriterSerializer_true() {
		var s = JsonSerializer.DEFAULT.createSession().build();
		assertTrue(s.isWriterSerializer());
	}

	private static Object getProtected(Object target, String method) throws Exception {
		var m = WriterSerializerSession.class.getDeclaredMethod(method);
		m.setAccessible(true);
		return m.invoke(target);
	}
}
