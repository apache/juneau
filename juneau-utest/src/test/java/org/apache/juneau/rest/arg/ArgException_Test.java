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
package org.apache.juneau.rest.arg;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.reflect.*;
import org.junit.jupiter.api.*;

class ArgException_Test extends TestBase {

	private static ParamInfo testParamInfo;

	@BeforeAll
	public static void setup() throws Exception {
		// Create a test ParamInfo for a sample method parameter
		MethodInfo mi = MethodInfo.of(ArgException_Test.class.getMethod("sampleMethod", String.class));
		testParamInfo = mi.getParam(0);
	}

	public static void sampleMethod(String param) {
		// Sample method for creating ParamInfo
	}

	@Test void a01_basic() {
		ArgException x = new ArgException(testParamInfo, "Test message");
		assertNotNull(x);
		assertTrue(x.getMessage().contains("Test message"));
		assertTrue(x.getMessage().contains("parameter 0"));
	}

	@Test void a02_withArgs() {
		ArgException x = new ArgException(testParamInfo, "Test {0} {1}", "foo", "bar");
		assertTrue(x.getMessage().contains("Test foo bar"));
		assertTrue(x.getMessage().contains("parameter 0"));
	}

	@Test void a03_fluentSetters() {
		ArgException x = new ArgException(testParamInfo, "Test");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertTrue(x.getMessage().contains("New message"));

		// Test setHeader2 returns same instance
		assertSame(x, x.setHeader2("X-Test", "test-value"));

		// Test setHeaders(HeaderList) returns same instance
		HeaderList headers = HeaderList.of(BasicHeader.of("X-Header1", "value1"));
		assertSame(x, x.setHeaders(headers));

		// Test setHeaders2(Header...) returns same instance
		assertSame(x, x.setHeaders2(BasicHeader.of("X-Header2", "value2")));

		// Test setLocale2 returns same instance
		assertSame(x, x.setLocale2(Locale.US));

		// Test setProtocolVersion returns same instance
		assertSame(x, x.setProtocolVersion(new ProtocolVersion("HTTP", 1, 1)));

		// Test setReasonPhrase2 returns same instance
		assertSame(x, x.setReasonPhrase2("Custom Reason"));

		// Test setReasonPhraseCatalog returns same instance
		assertSame(x, x.setReasonPhraseCatalog(null));

		// Test setStatusLine returns same instance
		assertSame(x, x.setStatusLine(BasicStatusLine.create(500, "Test")));

		// Test setHeaders(List<Header>) returns same instance
		List<Header> headerList = l(BasicHeader.of("X-Header3", "value3"));
		assertSame(x, x.setHeaders(headerList));
		assertEquals("value3", x.getFirstHeader("X-Header3").getValue());

		// Test setContent(String) returns same instance
		assertSame(x, x.setContent("test content"));

		// Test setContent(HttpEntity) returns same instance
		HttpEntity entity = x.getEntity();
		assertSame(x, x.setContent(entity));

		// Test setUnmodifiable returns same instance (must be last - makes bean read-only)
		assertSame(x, x.setUnmodifiable());
	}

	@Test void a04_fluentChaining() {
		// Test multiple fluent calls can be chained
		var x = new ArgException(testParamInfo, "Initial")
			.setHeaders(l(BasicHeader.of("X-Chain", "chained")))
			.setContent("Chained content");

		assertEquals("chained", x.getFirstHeader("X-Chain").getValue());
	}

	@Test void a05_copy() {
		// Test that copy() returns correct type
		ArgException x = new ArgException(testParamInfo, "Original message");

		ArgException copy = x.copy();

		// Verify it's a different instance
		assertNotSame(x, copy);

		// Verify it returns the correct type (not InternalServerError)
		assertInstanceOf(ArgException.class, copy);

		// Verify message is copied
		assertTrue(copy.getMessage().contains("Original message"));
	}
}