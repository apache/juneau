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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that malformed Accept-Language ranges are ignored instead of being turned into a {@link Locale}
 * (which is later used to build file names, so it must not contain path characters).
 */
class RestRequest_Locale_Test extends TestBase {

	@Test void a01_validValues() {
		assertEquals(new Locale("en"), RestRequest.toLocale("en"));
		assertEquals(new Locale("en","US"), RestRequest.toLocale("en-US"));
		assertEquals(new Locale("fr","CA"), RestRequest.toLocale("fr-CA"));
		assertEquals(new Locale("zh"), RestRequest.toLocale("zh-Hans"));
		assertEquals(new Locale("zh","CN"), RestRequest.toLocale("zh-Hans-CN"));
		assertEquals(new Locale("es","419"), RestRequest.toLocale("es-419"));
	}

	@Test void a02_malformedValuesRejected() {
		assertNull(RestRequest.toLocale("..-.."));
		assertNull(RestRequest.toLocale(".."));
		assertNull(RestRequest.toLocale("en-.."));
		assertNull(RestRequest.toLocale("en-../.."));
		assertNull(RestRequest.toLocale("e"));
		assertNull(RestRequest.toLocale("abcdefghi"));
		assertNull(RestRequest.toLocale("en_US"));
		assertNull(RestRequest.toLocale("en-US/x"));
		assertNull(RestRequest.toLocale("%2e%2e-%2e%2e"));
		assertNull(RestRequest.toLocale(""));
		assertNull(RestRequest.toLocale("*"));
		assertNull(RestRequest.toLocale(null));
		assertNull(RestRequest.toLocale("en-USA"));
		assertNull(RestRequest.toLocale("en-x-foo"));
		assertNull(RestRequest.toLocale("en-%2e%2e"));
		assertNull(RestRequest.toLocale("en-US-POSIX"));
		assertNull(RestRequest.toLocale("de-DE-1996"));
		assertNull(RestRequest.toLocale("en-US-"));
	}
}
