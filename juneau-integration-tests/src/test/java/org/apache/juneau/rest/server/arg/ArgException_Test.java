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
package org.apache.juneau.rest.server.arg;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class ArgException_Test extends TestBase {

	private static ParameterInfo testParameterInfo;

	@BeforeAll
	static void setup() throws Exception {
		var mi = MethodInfo.of(ArgException_Test.class.getMethod("sampleMethod", String.class));
		testParameterInfo = mi.getParameter(0);
	}

	public static void sampleMethod(String param) {
		// Sample method for creating ParameterInfo
	}

	@Test void a01_basic() {
		var x = new ArgException(testParameterInfo, "Test message");
		assertNotNull(x);
		assertTrue(x.getMessage().contains("Test message"));
		assertTrue(x.getMessage().contains("parameter 0"));
	}

	@Test void a02_withArgs() {
		var x = new ArgException(testParameterInfo, "Test %s %s", "foo", "bar");
		assertTrue(x.getMessage().contains("Test foo bar"));
		assertTrue(x.getMessage().contains("parameter 0"));
	}

	@Test void a03_fluentSetters() {
		var x = new ArgException(testParameterInfo, "Test");

		assertSame(x, x.setHeader("X-Test", "test-value"));
		assertSame(x, x.setHeader(HttpStringHeader.of("X-Foo", "foo-value")));

		assertSame(x, x.setHeaders(HttpStringHeader.of("X-Header2", "value2")));

		assertSame(x, x.setLocale(Locale.US));

		assertSame(x, x.setProtocolVersion(HttpProtocolVersion.of("HTTP", 1, 1)));

		assertSame(x, x.setReasonPhrase("Custom Reason"));

		List<HttpHeader> headerList = l(HttpStringHeader.of("X-Header3", "value3"));
		assertSame(x, x.setHeaders(headerList));

		assertSame(x, x.setContent("test content"));

		assertSame(x, x.setUnmodifiable());
	}

	@Test void a04_fluentChaining() {
		var x = new ArgException(testParameterInfo, "Initial")
			.setHeaders(l(HttpStringHeader.of("X-Chain", "chained")))
			.setContent("Chained content");

		assertEquals("chained", x.getHeaders().stream().filter(h -> "X-Chain".equalsIgnoreCase(h.getName())).findFirst().orElseThrow().getValue());
	}
}
