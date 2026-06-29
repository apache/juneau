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

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Content-negotiation resolver contract for the next-gen {@link RestClient}.
 *
 * <p>
 * As of 10.0.0 the implicit JSON fallback and the lone-registered-entry fallback are removed: a serializer/parser is
 * resolved only by an exact media-type match or by an explicitly-configured {@code defaultSerializer(...)} /
 * {@code defaultParser(...)}; otherwise the resolver is genuinely empty (and callers throw 415 / a client-side error).
 */
class RestClient_Negotiation_Test {

	@Test
	void a01_unconfigured_resolvesEmpty() throws Exception {
		// No serializers/parsers and no explicit defaults — everything resolves empty (no implicit JSON).
		try (var c = RestClient.create()) {
			assertTrue(c.getDefaultSerializer().isEmpty());
			assertTrue(c.getMatchingParser(null).isEmpty());
			assertTrue(c.getMatchingParser("application/jsonl").isEmpty());
			assertNull(c.getDefaultAccept());
		}
	}

	@Test
	void a02_matchesByContentType() throws Exception {
		var sset = SerializerSet.create().add(JsonSerializer.DEFAULT, JsonlSerializer.DEFAULT).build();
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var c = RestClient.builder().serializers(sset).parsers(pset).build()) {
			assertInstanceOf(JsonlParser.class, c.getMatchingParser("application/jsonl").orElseThrow());
			assertInstanceOf(JsonParser.class, c.getMatchingParser("application/json").orElseThrow());
			// A registered set is NOT a default — no implicit first-entry/JSON fallback.
			assertTrue(c.getDefaultSerializer().isEmpty());
			assertTrue(c.getMatchingParser("text/unknown").isEmpty());
			assertTrue(c.getDefaultAccept().contains("application/json"));
			assertTrue(c.getDefaultAccept().contains("application/jsonl"));
		}
	}

	@Test
	void a03_explicitDefaultIsUsedForUnmatched() throws Exception {
		var sset = SerializerSet.create().add(JsonSerializer.DEFAULT, JsonlSerializer.DEFAULT).build();
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var c = RestClient.builder()
				.serializers(sset).parsers(pset)
				.defaultSerializer(JsonlSerializer.DEFAULT)
				.defaultParser(JsonlParser.DEFAULT)
				.build()) {
			assertSame(JsonlSerializer.DEFAULT, c.getDefaultSerializer().orElseThrow());
			assertSame(JsonlParser.DEFAULT, c.getMatchingParser("text/unknown").orElseThrow());
			// Exact match still wins over the configured default.
			assertInstanceOf(JsonParser.class, c.getMatchingParser("application/json").orElseThrow());
		}
	}

	@Test
	void a04_appendConvenience_isNotADefault() throws Exception {
		// Appending serializers/parsers registers them in the set but does NOT make them the default.
		try (var c = RestClient.builder()
				.serializer(JsonlSerializer.DEFAULT)
				.parser(JsonlParser.DEFAULT)
				.build()) {
			assertTrue(c.getDefaultSerializer().isEmpty());
			assertInstanceOf(JsonlParser.class, c.getMatchingParser("application/jsonl").orElseThrow());
			assertTrue(c.getMatchingParser("text/unknown").isEmpty());
		}
	}

	@Test
	void a05_explicitDefaultIsFallbackWithoutSet() throws Exception {
		try (var c = RestClient.builder()
				.defaultSerializer(JsonlSerializer.DEFAULT)
				.defaultParser(JsonlParser.DEFAULT)
				.build()) {
			assertSame(JsonlSerializer.DEFAULT, c.getDefaultSerializer().orElseThrow());
			assertSame(JsonlParser.DEFAULT, c.getMatchingParser("text/unknown").orElseThrow());
		}
	}

	@Test
	void a06_defaultAcceptExactOrder() throws Exception {
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var c = RestClient.builder().parsers(pset).build()) {
			assertEquals("application/json, text/json, application/jcs+json, application/jsonl, application/x-ndjson, text/jsonl", c.getDefaultAccept());
		}
	}

	@Test
	void a07_explicitSetWinsOverAppend() throws Exception {
		var sset = SerializerSet.create().add(JsonSerializer.DEFAULT).build();
		var pset = ParserSet.create().add(JsonParser.DEFAULT).build();
		try (var c = RestClient.builder()
				.serializers(sset).parsers(pset)
				.serializer(JsonlSerializer.DEFAULT)
				.parser(JsonlParser.DEFAULT)
				.build()) {
			// The appended jsonl entries are ignored because an explicit set was supplied.
			assertInstanceOf(JsonParser.class, c.getMatchingParser("application/json").orElseThrow());
			assertTrue(c.getMatchingParser("application/jsonl").isEmpty());
			assertTrue(c.getMatchingParser("text/unknown").isEmpty());
			assertTrue(c.getDefaultSerializer().isEmpty());
			assertFalse(c.getDefaultAccept().contains("jsonl"));
		}
	}

	@Test
	void a08_loneDefaultParserAdvertisesItsMediaTypes() throws Exception {
		try (var c = RestClient.builder().defaultParser(JsonlParser.DEFAULT).build()) {
			var accept = c.getDefaultAccept();
			assertTrue(accept.startsWith("application/jsonl"), () -> "Default Accept was: " + accept);
			assertNotEquals("application/json", accept, () -> "Default Accept was: " + accept);
		}
	}

	@Test
	void a09_exactMatchWinsOverExplicitDefault() throws Exception {
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var c = RestClient.builder().parsers(pset).defaultParser(JsonParser.DEFAULT).build()) {
			// Exact match on the registered set takes precedence over the explicit default.
			assertInstanceOf(JsonlParser.class, c.getMatchingParser("application/jsonl").orElseThrow());
			// Unmatched falls back to the explicit default.
			assertSame(JsonParser.DEFAULT, c.getMatchingParser("text/unknown").orElseThrow());
		}
	}

	@Test
	void a10_defaultSerializerOptInRestoresFallback() throws Exception {
		try (var unconfigured = RestClient.create();
			 var configured = RestClient.builder().defaultSerializer(JsonSerializer.DEFAULT).build()) {
			assertTrue(unconfigured.getDefaultSerializer().isEmpty());
			assertSame(JsonSerializer.DEFAULT, configured.getDefaultSerializer().orElseThrow());
		}
	}
}
