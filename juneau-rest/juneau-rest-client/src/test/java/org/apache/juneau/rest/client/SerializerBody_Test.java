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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the next-generation {@link SerializerBody} streaming request body.
 */
class SerializerBody_Test extends TestBase {

	/** A concrete {@link Serializer} whose builder never calls {@code produces(...)}, so its response content type is null. */
	private static final class BareSerializer extends Serializer {
		BareSerializer() { super(Serializer.create()); }
	}

	private static String writeToString(SerializerBody body) throws IOException {
		var baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	// ==========================================================================
	// a — of(serializer, value) (serializer's own response content type)
	// ==========================================================================

	@Test void a01_of_twoArg_usesSerializersOwnContentType() throws Exception {
		var body = SerializerBody.of(IniSerializer.DEFAULT, Map.of("k", "v"));
		assertEquals("text/ini", body.getContentType());
		assertEquals("k = v\n", writeToString(body));
	}

	@Test void a02_of_twoArg_serializerWithNullResponseContentType_yieldsNullContentType() {
		var body = SerializerBody.of(new BareSerializer(), "x");
		assertNull(body.getContentType());
	}

	@Test void a03_of_twoArg_rejectsNullSerializer() {
		assertThrows(IllegalArgumentException.class, () -> SerializerBody.of(null, "x"));
	}

	// ==========================================================================
	// b — of(serializer, value, contentType) (explicit content type override)
	// ==========================================================================

	@Test void b01_of_threeArg_explicitContentTypeOverridesSerializer() {
		var body = SerializerBody.of(IniSerializer.DEFAULT, Map.of("k", "v"), "text/x-custom-ini");
		assertEquals("text/x-custom-ini", body.getContentType());
	}

	@Test void b02_of_threeArg_nullContentTypeAllowed() {
		var body = SerializerBody.of(IniSerializer.DEFAULT, Map.of("k", "v"), null);
		assertNull(body.getContentType());
	}

	// ==========================================================================
	// c — metadata + writeTo behavior
	// ==========================================================================

	@Test void c01_metadata_notRepeatableSentinelLength() {
		var body = SerializerBody.of(IniSerializer.DEFAULT, Map.of());
		assertEquals(-1, body.getContentLength());
		assertTrue(body.isRepeatable());
	}

	@Test void c02_writeTo_reserializesOnEachCall() throws Exception {
		// Repeatable: each writeTo(...) re-runs the serializer, so the same body can be safely resent.
		var body = SerializerBody.of(IniSerializer.DEFAULT, Map.of("k", "v"));
		assertEquals(writeToString(body), writeToString(body));
	}

	@Test void c03_writeTo_serializeExceptionWrappedAsIOException() {
		// INI format requires a bean or Map<String,?> at the root; a bare String at the root fails serialization.
		var body = SerializerBody.of(IniSerializer.DEFAULT, "not a bean or map");
		var e = assertThrows(IOException.class, () -> body.writeTo(new ByteArrayOutputStream()));
		assertInstanceOf(SerializeException.class, e.getCause());
	}
}
