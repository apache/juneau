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

class RestClient_Negotiation_Test {

	@Test
	void a01_unconfigured_defaultsToJson() throws Exception {
		try (var c = RestClient.create()) {
			assertSame(JsonSerializer.DEFAULT, c.getDefaultSerializer());
			assertSame(JsonParser.DEFAULT, c.getMatchingParser(null));
			assertSame(JsonParser.DEFAULT, c.getMatchingParser("application/jsonl"));
			assertEquals("application/json", c.getDefaultAccept());
		}
	}

	@Test
	void a02_matchesByContentType() throws Exception {
		var sset = SerializerSet.create().add(JsonSerializer.DEFAULT, JsonlSerializer.DEFAULT).build();
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var c = RestClient.builder().serializers(sset).parsers(pset).build()) {
			assertInstanceOf(JsonlParser.class, c.getMatchingParser("application/jsonl"));
			assertInstanceOf(JsonParser.class, c.getMatchingParser("application/json"));
			assertSame(JsonSerializer.DEFAULT, c.getDefaultSerializer());
			assertInstanceOf(JsonParser.class, c.getMatchingParser("text/unknown"));
			assertTrue(c.getDefaultAccept().contains("application/json"));
			assertTrue(c.getDefaultAccept().contains("application/jsonl"));
		}
	}

	@Test
	void a03_explicitDefaultOverridesFirstInSet() throws Exception {
		var sset = SerializerSet.create().add(JsonSerializer.DEFAULT, JsonlSerializer.DEFAULT).build();
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var c = RestClient.builder()
				.serializers(sset).parsers(pset)
				.defaultSerializer(JsonlSerializer.DEFAULT)
				.defaultParser(JsonlParser.DEFAULT)
				.build()) {
			assertSame(JsonlSerializer.DEFAULT, c.getDefaultSerializer());
			assertSame(JsonlParser.DEFAULT, c.getMatchingParser("text/unknown"));
			assertInstanceOf(JsonParser.class, c.getMatchingParser("application/json"));
		}
	}

	@Test
	void a04_appendConvenience() throws Exception {
		try (var c = RestClient.builder()
				.serializer(JsonlSerializer.DEFAULT)
				.parser(JsonlParser.DEFAULT)
				.build()) {
			assertSame(JsonlSerializer.DEFAULT, c.getDefaultSerializer());
			assertInstanceOf(JsonlParser.class, c.getMatchingParser("application/jsonl"));
		}
	}

	@Test
	void a05_explicitDefaultIsFallbackWithoutSet() throws Exception {
		try (var c = RestClient.builder()
				.defaultSerializer(JsonlSerializer.DEFAULT)
				.defaultParser(JsonlParser.DEFAULT)
				.build()) {
			assertSame(JsonlSerializer.DEFAULT, c.getDefaultSerializer());
			assertSame(JsonlParser.DEFAULT, c.getMatchingParser("text/unknown"));
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
	void a08_loneDefaultParserAdvertisesItsMediaTypes() throws Exception {
		try (var c = RestClient.builder().defaultParser(JsonlParser.DEFAULT).build()) {
			var accept = c.getDefaultAccept();
			assertTrue(accept.startsWith("application/jsonl"), () -> "Default Accept was: " + accept);
			assertNotEquals("application/json", accept, () -> "Default Accept was: " + accept);
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
			assertSame(JsonSerializer.DEFAULT, c.getDefaultSerializer());
			assertInstanceOf(JsonParser.class, c.getMatchingParser("text/unknown"));
			assertInstanceOf(JsonParser.class, c.getMatchingParser("application/jsonl"));
			assertFalse(c.getDefaultAccept().contains("jsonl"));
		}
	}
}
