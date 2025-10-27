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
package org.apache.juneau.http;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Verifies that the Accept class handles matching correctly.
 */
class MediaRanges_FindMatch_Test extends TestBase {

	private static final Input[] INPUT = {

		// label, accept-header, media-types, expected-index

		// Simple matches
		input("SimpleMatch-1", "text/json", "['text/json']", 0, 0),
		input("SimpleMatch-2", "text/json", "['text/json','text/foo']", 0, 0),
		input("SimpleMatch-3", "text/json", "['text/foo','text/json']", 1, 1),

		// Simple no-matches
		input("SimpleNoMatch-1", "text/jsonx", "['text/json']", -1, -1),
		input("SimpleNoMatch-2", "text/jso", "['text/json']", -1, -1),
		input("SimpleNoMatch-3", "text/json", "['application/json']", -1, -1),
		input("SimpleNoMatch-4", "text/json", "[]", -1, -1),

		// Meta-character matches
		input("MetaMatch-1", "text/*", "['text/a','text/b+c','text/b+d+e']", 0, 2),
		input("MetaMatch-2", "text/b+*", "['text/a','text/b+c','text/b+d+e']", 1, 2),
		input("MetaMatch-3", "text/c+*", "['text/a','text/b+c','text/b+d+e']", 1, 1),
		input("MetaMatch-4", "text/b+d+e", "['text/a','text/b+c','text/b+d']", -1, -1),
		input("MetaMatch-5", "text/b+*", "['text/a','text/b+c','text/b+d+e']", 1, 2),
		input("MetaMatch-6", "text/d+e+*", "['text/a','text/b+c','text/b+d+e']", 2, 2),

		input("MetaMatch-7", "*/a", "['text/a','application/b']", 0, 0),
		input("MetaMatch-8", "*/*", "['text/a','text/b+c']", 0, 1),
		input("MetaMatch-9", "*/*", "['text/b+c','text/a']", 0, 1),

		// Reverse meta-character matches
		input("RevMetaMatch-1", "text/a", "['text/*']", 0, 0),
		input("RevMetaMatch-2", "text/a", "['*/a']", 0, 0),
		input("RevMetaMatch-3", "text/a", "['*/*']", 0, 0),

		// Meta-character mixture matches
		input("MixedMetaMatch-1", "text/*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 0, 0),
		input("MixedMetaMatch-2", "*/a", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 1, 1),
		input("MixedMetaMatch-3", "*/*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 0, 0),
		input("MixedMetaMatch-4", "text/a+*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 1, 2),
		input("MixedMetaMatch-5", "text/c+*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 3, 3),
		input("MixedMetaMatch-6", "text/d+*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 4, 4),

		// Fuzzy matches
		input("Fuzzy-1", "text/1+2", "['text/1+2']", 0, 0),
		// Order of subtype parts shouldn't matter.
		input("Fuzzy-2", "text/2+1", "['text/1+2']", 0, 0),

		// Should match if Accept has 'extra' subtypes.
		// For example, "Accept: text/json+activity" should match against the "text/json" serializer.
		input("Fuzzy-3", "text/json+foo", "['text/json+*']", 0, 0),

		// Shouldn't match because the accept media type must be at least a subset of the real media type
		// For example, "Accept: text/json" should not match against the "text/json+lax" serializer.
		input("Fuzzy-4", "text/json", "['text/json+lax']", -1, -1),

		input("Fuzzy-5", "text/1+2", "['text/1','text/1+3']", -1, -1),

		// "text/1+2" should be a better match than just "text/1"
		input("Fuzzy-6", "text/1+2", "['text/1','text/1+2','text/1+2+3']", 1, 1),
		// Same as last, but mix up the order a bit.
		input("Fuzzy-7", "text/1+2", "['text/1+2+3','text/1','text/1+2']", 2, 2),
		// Same as last, but mix up the order of the subtypes as well.
		input("Fuzzy-8", "text/1+2", "['text/3+2+1','text/1','text/2+1']", 2, 2),
		input("Fuzzy-9", "text/1+2+3+4", "['text/1+2','text/1+2+3']", -1, -1),
		input("Fuzzy-10", "text/1+2+3+4", "['text/1+2+3','text/1+2']", -1, -1),
		input("Fuzzy-11", "text/4+2+3+1", "['text/1+2+3','text/1+2']", -1, -1),
		input("Fuzzy-12", "text/4+2+3+1", "['text/1+2','text/1+2+3']", -1, -1),

		// Q metrics
		input("Q-1", "text/A;q=0.9,text/B;q=0.1", "['text/A','text/B']", 0, 0),
		input("Q-2", "text/A;q=0.9,text/B;q=0.1", "['text/B','text/A']", 1, 1),
		input("Q-3", "text/A+1;q=0.9,text/B;q=0.1", "['text/A','text/B']", 1, 1),
		input("Q-4", "text/A;q=0.9,text/B+1;q=0.1", "['text/A','text/B+1']", 0, 0),
		input("Q-5", "text/A;q=0.9,text/A+1;q=0.1", "['text/A+1','text/A']", 1, 1),

		// Test q=0
		input("Q0-1", "text/A;q=0,text/B;q=0.1", "['text/A','text/B']", 1, 1),
		input("Q0-2", "text/A;q=0,text/B;q=0.1", "['text/A','text/A+1']", -1, -1),

		// Test media types with parameters
		input("Parms-1", "text/A", "['text/A;foo=bar','text/B']", 0, 0),
		input("Parms-2", "text/A;foo=bar", "['text/A','text/B']", 0, 0),

		// Real-world JSON
		input("Json-1a", "text/json", "['text/json','text/json+*','text/*','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-1b", "text/json", "['text/json+*','text/*','text/json+lax','text/json+lax+*','text/foo','text/json']", 5, 5),
		input("Json-1c", "text/json", "['text/json+*','text/*','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-1d", "text/json", "['text/*','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-1e", "text/json", "['text/json+lax','text/json+lax+*','text/foo']", -1, -1),

		input("Json-2a", "text/json+lax", "['text/json+lax','text/json+lax+*','text/json+*','text/lax+*','text/*','text/json','text/lax']", 0, 0),
		input("Json-2b", "text/json+lax", "['text/json+lax+*','text/json+*','text/lax+*','text/*','text/json','text/lax']", 0, 0),
		input("Json-2c", "text/json+lax", "['text/json+*','text/lax+foo+*','text/*','text/json','text/lax']", 0, 0),
		input("Json-2d", "text/json+lax", "['text/lax+*','text/*','text/json','text/lax']", 0, 0),
		input("Json-2e", "text/json+lax", "['text/*','text/json','text/lax']", 0, 0),
		input("Json-2f", "text/json+lax", "['text/json','text/lax']", -1, -1),

		input("Json-3a", "text/json+activity", "['text/json+activity','text/activity+json','text/json+activity+*','text/json+*','text/*','text/json','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-3b", "text/json+activity", "['text/activity+json','text/json+activity+*','text/json+*','text/*','text/json','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-3c", "text/json+activity", "['text/json+activity+*','text/json+*','text/*','text/json','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-3d", "text/json+activity", "['text/json+*','text/*','text/json','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-3e", "text/json+activity", "['text/*','text/json','text/json+lax','text/json+lax+*','text/foo']", 0, 0),
		input("Json-3f", "text/json+activity", "['text/json','text/json+lax','text/json+lax+*','text/foo']", -1, -1),

		// Real-world XML
		input("Xml-1a", "text/xml", "['text/xml','text/xml+*','text/xml+rdf','text/foo']", 0, 0),
		input("Xml-1b", "text/xml", "['text/xml+*','text/xml+rdf','text/foo']", 0, 0),
		input("Xml-1c", "text/xml", "['text/xml+rdf','text/foo']", -1, -1),
		input("Xml-1d", "text/xml", "['text/foo']", -1, -1),

		input("Xml-2a", "text/xml+id", "['text/xml+*','text/xml','text/xml+rdf']", 0, 0),
		input("Xml-2b", "text/xml+id", "['text/xml','text/xml+rdf']", -1, -1),
		input("Xml-2c", "text/xml+id", "['text/xml+rdf']", -1, -1),

		// Real-world RDF
		input("Rdf-1a", "text/xml+rdf", "['text/xml+rdf','text/xml+*','text/xml']", 0, 0),
		input("Rdf-1b", "text/xml+rdf", "['text/xml+*','text/xml']", 0, 0),
		input("Rdf-1c", "text/xml+rdf", "['text/xml']", -1, -1)
	};

	private static Input input(String label, String accept, String mediaTypes, int expected, int expectedReverse) {
		return new Input(label, accept, mediaTypes, expected, expectedReverse);
	}

	private static class Input {
		private String label, accept, mediaTypes;
		private int expected, expectedReverse;

		public Input(String label, String accept, String mediaTypes, int expected, int expectedReverse) {
			this.label = label;
			this.accept = accept;
			this.mediaTypes = mediaTypes;
			this.expected = expected;
			this.expectedReverse = expectedReverse;
		}
	}

	static Input[] input() {
		return INPUT;
	}

	@ParameterizedTest
	@MethodSource("input")
	void a01_basic(Input input) throws Exception {
		var a = accept(input.accept);
		var mt = JsonParser.DEFAULT.parse(input.mediaTypes, MediaType[].class);
		var r = a.match(alist(mt));
		assertEquals(input.expected, r, fs("{0} failed", input.label));
	}

	@ParameterizedTest
	@MethodSource("input")
	void a02_reversed(Input input) throws Exception {
		var a = accept(input.accept);
		var mt = JsonParser.DEFAULT.parse(input.mediaTypes, MediaType[].class);
		Collections.reverse(Arrays.asList(mt));
		var r = a.match(alist(mt));
		var expected2 = input.expectedReverse == -1 ? -1 : mt.length-input.expectedReverse-1;
		assertEquals(expected2, r);
	}
}