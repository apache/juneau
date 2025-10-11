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

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpMethod_Test extends TestBase {

	@Test void a01_hasContent() {
		assertFalse(hasContent(OPTIONS));
		assertFalse(hasContent(GET));
		assertFalse(hasContent(HEAD));
		assertTrue(hasContent(POST));
		assertTrue(hasContent(PUT));
		assertFalse(hasContent(DELETE));
		assertFalse(hasContent(TRACE));
		assertFalse(hasContent(CONNECT));
		assertTrue(hasContent(PATCH));
		assertTrue(hasContent(RRPC));
		assertTrue(hasContent(OTHER));
		assertTrue(hasContent(ANY));
	}
}