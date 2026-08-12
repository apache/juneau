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
package org.apache.juneau.rest.client.classic.assertion;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.junit.jupiter.api.*;

/**
 * Tests the transform methods of {@link FluentResponseStatusLineAssertion} that aren't already exercised
 * by end-to-end client tests (asMajor/asMinor/asProtocol/asReason), plus its overridden configuration setters
 * (setMsg/setOut/setSilent/setStdOut), which only differ from the parent implementation in their covariant
 * return type.
 */
class FluentResponseStatusLineAssertion_Test {

	private final StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");

	@Test void a01_asMajor() {
		new FluentResponseStatusLineAssertion<>(statusLine, null).asMajor().is(1);
	}

	@Test void a02_asMinor() {
		new FluentResponseStatusLineAssertion<>(statusLine, null).asMinor().is(1);
	}

	@Test void a03_asProtocol() {
		new FluentResponseStatusLineAssertion<>(statusLine, null).asProtocol().is("HTTP");
	}

	@Test void a04_asReason() {
		new FluentResponseStatusLineAssertion<>(statusLine, null).asReason().is("OK");
	}

	@Test void a05_configMethods_returnThis() {
		var a = new FluentResponseStatusLineAssertion<>(statusLine, null);
		assertSame(a, a.setMsg("msg"));
		assertSame(a, a.setOut(System.out));
		assertSame(a, a.setSilent());
		assertSame(a, a.setStdOut());
	}
}
