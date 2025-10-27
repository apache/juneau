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
package org.apache.juneau.http.header;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Verifies that the Content-Type class handles matching correctly.
 */
class ContentType_Match_Test extends TestBase {

	private static final Input[] INPUT = {
		// Simple matches
		input("SimpleMatch-1", "text/json", "['text/json']", 0),
		input("SimpleMatch-2", "text/json", "['text/json','text/foo']", 0),
		input("SimpleMatch-3", "text/json", "['text/foo','text/json']", 1),

		// Simple no-matches
		input("SimpleNoMatch-1", "text/jsonx", "['text/json']", -1),
		input("SimpleNoMatch-2", "text/jso", "['text/json']", -1),
		input("SimpleNoMatch-3", "text/json", "['application/json']", -1),
		input("SimpleNoMatch-4", "text/json", "[]", -1),

		input("XmlAndRdf-1", "text/xml+rdf", "['text/xml','text/xml+rdf']", 1),
		input("XmlAndRdf-2", "text/xml+rdf", "['text/xml+rdf','text/xml']", 0),

		// Fuzzy matches
		input("Fuzzy-1", "text/1+2", "['text/1+2']", 0),
		// Order of subtype parts shouldn't matter.
		input("Fuzzy-2", "text/2+1", "['text/1+2']", 0),
		// Should match if Accept has 'extra' subtypes.
		// For example, "Accept: text/json+activity" should match against the "text/json" serializer.
		input("Fuzzy-3", "text/1+2", "['text/1']", 0),
		// Shouldn't match because the accept media type must be at least a subset of the real media type
		// For example, "Accept: text/json" should not match against the "text/json+lax" serializer.
		input("Fuzzy-4", "text/1", "['text/1+2']", -1),
		input("Fuzzy-5", "text/1+2", "['text/1','text/1+3']", 0),
		// "text/1+2" should be a better match than just "text/1"
		input("Fuzzy-6", "text/1+2", "['text/1','text/1+2','text/1+2+3']", 1),
		// Same as last, but mix up the order a bit.
		input("Fuzzy-7", "text/1+2", "['text/1+2+3','text/1','text/1+2']", 2),
		// Same as last, but mix up the order of the subtypes as well.
		input("Fuzzy-8", "text/1+2", "['text/3+2+1','text/1','text/2+1']", 2),
		input("Fuzzy-9", "text/1+2+3+4", "['text/1+2','text/1+2+3']", 1),
		input("Fuzzy-10", "text/1+2+3+4", "['text/1+2+3','text/1+2']", 0),
		input("Fuzzy-11", "text/4+2+3+1", "['text/1+2+3','text/1+2']", 0),
		input("Fuzzy-12", "text/4+2+3+1", "['text/1+2','text/1+2+3']", 1)
	};

	static Input[] input() {
		return INPUT;
	}

	protected static Input input(String label, String contentType, String mediaTypes, int expected) {
		return new Input(label, contentType, mediaTypes, expected);
	}

	private static class Input {
		String label, contentType, mediaTypes;
		int expected;

		Input(String label, String contentType, String mediaTypes, int expected) {
			this.label = label;
			this.contentType = contentType;
			this.mediaTypes = mediaTypes;
			this.expected = expected;
		}
	}

	@ParameterizedTest
	@MethodSource("input")
	void a01_basic(Input input) throws Exception {
		var ct = contentType(input.contentType);
		var mt = JsonParser.DEFAULT.parse(input.mediaTypes, MediaType[].class);
		assertEquals(input.expected, ct.match(alist(mt)), fs("{0} failed", input.label));
	}
}