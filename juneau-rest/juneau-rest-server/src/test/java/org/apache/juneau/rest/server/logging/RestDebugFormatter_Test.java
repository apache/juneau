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
package org.apache.juneau.rest.server.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests that the {@link RestDebugFormatter} interface default methods are additive-safe — a bare implementation
 * supplying only {@link RestDebugFormatter#formatBasic(org.apache.juneau.rest.server.RestRequest, org.apache.juneau.rest.server.RestResponse) formatBasic}
 * inherits empty defaults for the additive tiers.
 *
 * @since 10.0.0
 */
class RestDebugFormatter_Test {

	@Test void a01_defaultHeaders_isEmpty() {
		RestDebugFormatter f = (req, res) -> "basic";
		assertEquals("", f.formatHeaders(null, null));
	}

	@Test void a02_defaultBody_isEmpty() {
		RestDebugFormatter f = (req, res) -> "basic";
		assertEquals("", f.formatBody(null, null));
	}

	@Test void a03_defaultBodyCap_is8k() {
		RestDebugFormatter f = (req, res) -> "basic";
		assertEquals(8 * 1024, f.bodyCap());
	}

	@Test void a04_basic_returnsSuppliedValue() {
		RestDebugFormatter f = (req, res) -> "basic";
		assertEquals("basic", f.formatBasic(null, null));
	}
}
