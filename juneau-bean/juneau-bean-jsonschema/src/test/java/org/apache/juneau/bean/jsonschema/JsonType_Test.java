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
package org.apache.juneau.bean.jsonschema;

import static org.apache.juneau.bean.jsonschema.JsonType.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class JsonType_Test extends TestBase {

	@Test void a01_fromString_nullAndShort() {
		assertNull(fromString(null));
		assertNull(fromString("ab"));   // length < 4
		assertNull(fromString("arr"));  // length == 3 (< 4)
	}

	@Test void a02_fromString_arrayAndAny() {
		assertEquals(ARRAY, fromString("array"));
		// "any" has length 3 which fails the length < 4 check → returns null
		assertNull(fromString("any"));
		assertNull(fromString("aaaa")); // starts with 'a', no match → falls through to return null
	}

	@Test void a03_fromString_boolean() {
		assertEquals(BOOLEAN, fromString("boolean"));
		assertNull(fromString("bbbbb")); // starts with 'b', no match
	}

	@Test void a04_fromString_integer() {
		assertEquals(INTEGER, fromString("integer"));
		assertNull(fromString("iiiii")); // starts with 'i', no match
	}

	@Test void a05_fromString_nullType() {
		assertEquals(NULL, fromString("null"));
		// 'l' at charAt(2) but not "null" → false branch of value.equals("null") at line 84
		assertNull(fromString("nulx")); // charAt(2)='l', but != "null"
	}

	@Test void a06_fromString_number() {
		assertEquals(NUMBER, fromString("number"));
		// 'm' at charAt(2) but not "number" → false branch of value.equals("number") at line 86
		assertNull(fromString("numx")); // charAt(2)='m', but != "number"
	}

	@Test void a07_fromString_nWithNoMatch() {
		// charAt(2) is neither 'l' nor 'm' → returns null
		assertNull(fromString("nope"));
	}

	@Test void a08_fromString_object() {
		assertEquals(OBJECT, fromString("object"));
		assertNull(fromString("ooooo")); // starts with 'o', no match
	}

	@Test void a09_fromString_string() {
		assertEquals(STRING, fromString("string"));
		assertNull(fromString("sssss")); // starts with 's', no match
	}

	@Test void a10_fromString_unrecognizedFirstChar() {
		// 'z' prefix not handled by any if block → falls off to return null
		assertNull(fromString("zzzz"));
	}

	@Test void a11_toString() {
		assertEquals("array", ARRAY.toString());
		assertEquals("boolean", BOOLEAN.toString());
		assertEquals("integer", INTEGER.toString());
		assertEquals("null", NULL.toString());
		assertEquals("number", NUMBER.toString());
		assertEquals("object", OBJECT.toString());
		assertEquals("string", STRING.toString());
		assertEquals("any", ANY.toString());
	}
}
