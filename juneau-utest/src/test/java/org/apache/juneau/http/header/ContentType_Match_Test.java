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
package org.apache.juneau.http.header;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the Content-Type class handles matching correctly.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(NAME_ASCENDING)
public class ContentType_Match_Test {
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

			{ "XmlAndRdf-1", "text/xml+rdf", "['text/xml','text/xml+rdf']", 1 },
			{ "XmlAndRdf-2", "text/xml+rdf", "['text/xml+rdf','text/xml']", 0 },

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
		});
	}

	private String label, contentType, mediaTypes;
	private int expected;

	public ContentType_Match_Test(String label, String contentType, String mediaTypes, int expected) {
		this.label = label;
		this.contentType = contentType;
		this.mediaTypes = mediaTypes;
		this.expected = expected;
	}

	@Test
	public void test() throws Exception {
		ContentType ct = contentType(this.contentType);
		MediaType[] mt = JsonParser.DEFAULT.parse(mediaTypes, MediaType[].class);
		assertInteger(ct.match(alist(mt))).setMsg("{0} failed", label).is(expected);
	}
}
