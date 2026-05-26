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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link EncodingFunctions}. */
class EncodingFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(EncodingFunctions.ALL).build();

	@Test void base64_roundtrip() {
		var encoded = vr.resolve("#{base64Encode(hello)}");
		assertEquals("aGVsbG8=", encoded);
		assertEquals("hello", vr.resolve("#{base64Decode(aGVsbG8=)}"));
	}

	@Test void urlEncode() {
		// URLEncoder uses '+' for space (application/x-www-form-urlencoded form per OQA spec).
		assertEquals("a+b%26c", vr.resolve("#{urlEncode(\"a b&c\")}"));
	}

	@Test void urlDecode() {
		assertEquals("a b&c", vr.resolve("#{urlDecode(\"a+b%26c\")}"));
	}

	@Test void htmlEscape() {
		assertEquals("&lt;b&gt;hi&amp;hello&lt;/b&gt;",
			vr.resolve("#{htmlEscape(\"<b>hi&hello</b>\")}"));
	}

	@Test void htmlUnescape() {
		assertEquals("<b>hi&hello</b>",
			vr.resolve("#{htmlUnescape(\"&lt;b&gt;hi&amp;hello&lt;/b&gt;\")}"));
	}

	@Test void htmlUnescape_numeric() {
		assertEquals("A", vr.resolve("#{htmlUnescape(\"&#65;\")}"));
	}
}
