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
package org.apache.juneau.marshall.httppart;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpPart_Test extends TestBase {

	private static final HttpPartSerializerSession SESSION = SimplePartSerializer.DEFAULT.getPartSession();

	@Test void a01_getName() {
		var part = new HttpPart("Accept", HEADER, null, SESSION, "text/plain");
		assertEquals("Accept", part.getName());
	}

	@Test void a02_getValue_string() {
		var part = new HttpPart("X-Foo", HEADER, null, SESSION, "bar");
		assertEquals("bar", part.getValue());
	}

	@Test void a03_getValue_integer() {
		var part = new HttpPart("X-Count", HEADER, null, SESSION, 42);
		assertEquals("42", part.getValue());
	}

	@Test void a04_getValue_null() {
		var part = new HttpPart("X-Empty", QUERY, null, SESSION, null);
		assertNull(part.getValue());
	}
}
