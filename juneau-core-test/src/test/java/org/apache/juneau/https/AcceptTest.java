// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.https;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the Accept class handles matching correctly.
 */
@RunWith(Parameterized.class)
public class AcceptTest {
	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {

			// label, accept-header, media-types, expected-index

			// Simple matches
			{ "SimpleMatch-1", "text/json", "['text/json']", 0 },
			{ "SimpleMatch-2", "text/json", "['text/json','text/foo']", 0 },
			{ "SimpleMatch-3", "text/json", "['text/foo','text/json']", 1 },

			// Simple no-matches
			{ "SimpleNoMatch-1", "text/jsonx", "['text/json']", -1 },
			{ "SimpleNoMatch-2", "text/jso", "['text/json']", -1 },
			{ "SimpleNoMatch-3", "text/json", "['application/json']", -1 },
			{ "SimpleNoMatch-4", "text/json", "[]", -1 },

			// Meta-character matches
			{ "MetaMatch-1", "text/*", "['text/a','text/b+c','text/b+d+e']", 2 },
			{ "MetaMatch-2", "text/b+*", "['text/a','text/b+c','text/b+d+e']", 2 },
			{ "MetaMatch-3", "text/c+*", "['text/a','text/b+c','text/b+d+e']", 1 },
			{ "MetaMatch-4", "text/b+d+e", "['text/a','text/b+c','text/b+d']", 2 },
			{ "MetaMatch-5", "text/b+*", "['text/a','text/b+c','text/b+d']", 1 },
			{ "MetaMatch-6", "text/d+e+*", "['text/a','text/b+c','text/b+d+e']", 2 },

			{ "MetaMatch-7", "*/a", "['text/a','application/a']", 0 },
			{ "MetaMatch-8", "*/*", "['text/a','text/b+c']", 1 },
			{ "MetaMatch-9", "*/*", "['text/b+c','text/a']", 0 },

			// Reverse meta-character matches
			{ "RevMetaMatch-1", "text/a", "['text/*']", 0 },
			{ "RevMetaMatch-3", "text/a", "['*/a']", 0 },
			{ "RevMetaMatch-3", "text/a", "['*/*']", 0 },

			// Meta-character mixture matches
			{ "MixedMetaMatch-1", "text/*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 0 },
			{ "MixedMetaMatch-2", "*/a", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 1 },
			{ "MixedMetaMatch-3", "*/*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 0 },
			{ "MixedMetaMatch-4", "text/a+*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 2 },
			{ "MixedMetaMatch-5", "text/c+*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 3 },
			{ "MixedMetaMatch-6", "text/d+*", "['text/*','text/a','text/a+b','text/b+c','text/d+*']", 4 },

			// Fuzzy matches
			{ "Fuzzy-1", "text/1+2", "['text/1+2']", 0 },
			// Order of subtype parts shouldn't matter.
			{ "Fuzzy-2", "text/2+1", "['text/1+2']", 0 },
			// Should match if Accept has 'extra' subtypes.
			// For example, "Accept: text/json+activity" should match against the "text/json" serializer.
			{ "Fuzzy-3", "text/1+2", "['text/1']", 0 },
			// Shouldn't match because the accept media type must be at least a subset of the real media type
			// For example, "Accept: text/json" should not match against the "text/json+lax" serializer.
			{ "Fuzzy-4", "text/1", "['text/1+2']", -1 },
			{ "Fuzzy-5", "text/1+2", "['text/1','text/1+3']", 0 },
			// "text/1+2" should be a better match than just "text/1"
			{ "Fuzzy-6", "text/1+2", "['text/1','text/1+2','text/1+2+3']", 1 },
			// Same as last, but mix up the order a bit.
			{ "Fuzzy-7", "text/1+2", "['text/1+2+3','text/1','text/1+2']", 2 },
			// Same as last, but mix up the order of the subtypes as well.
			{ "Fuzzy-8", "text/1+2", "['text/3+2+1','text/1','text/2+1']", 2 },
			{ "Fuzzy-9", "text/1+2+3+4", "['text/1+2','text/1+2+3']", 1 },
			{ "Fuzzy-10", "text/1+2+3+4", "['text/1+2+3','text/1+2']", 0 },
			{ "Fuzzy-11", "text/4+2+3+1", "['text/1+2+3','text/1+2']", 0 },
			{ "Fuzzy-12", "text/4+2+3+1", "['text/1+2','text/1+2+3']", 1 },

			// Q metrics
			{ "Q-1", "text/A;q=0.9,text/B;q=0.1", "['text/A','text/B']", 0 },
			{ "Q-2", "text/A;q=0.9,text/B;q=0.1", "['text/B','text/A']", 1 },
			{ "Q-3", "text/A+1;q=0.9,text/B;q=0.1", "['text/A','text/B']", 0 },
			{ "Q-4", "text/A;q=0.9,text/B+1;q=0.1", "['text/A','text/B+1']", 0 },
			{ "Q-5", "text/A;q=0.9,text/A+1;q=0.1", "['text/A+1','text/A']", 1 },

			// Test q=0
			{ "Q0-1", "text/A;q=0,text/B;q=0.1", "['text/A','text/B']", 1 },
			{ "Q0-2", "text/A;q=0,text/B;q=0.1", "['text/A','text/A+1']", -1 },

			// Test media types with parameters
			{ "Parms-1", "text/A", "['text/A;foo=bar','text/B']", 0 },
			{ "Parms-2", "text/A;foo=bar", "['text/A','text/B']", 0 },
		});
	}

	private String label, accept, mediaTypes;
	private int expected;

	public AcceptTest(String label, String accept, String mediaTypes, int expected) {
		this.label = label;
		this.accept = accept;
		this.mediaTypes = mediaTypes;
		this.expected = expected;
	}

	@Test
	public void test() throws Exception {
		Accept accept = Accept.forString(this.accept);
		MediaType[] mt = JsonParser.DEFAULT.parse(mediaTypes, MediaType[].class);
		int r = accept.findMatch(mt);
		TestUtils.assertEquals(expected, r, "{0} failed", label);
	}
}
