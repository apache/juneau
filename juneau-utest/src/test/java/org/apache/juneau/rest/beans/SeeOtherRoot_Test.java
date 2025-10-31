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
package org.apache.juneau.rest.beans;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class SeeOtherRoot_Test extends TestBase {

	@Test void a01_basic() {
		var x = new SeeOtherRoot();
		assertNotNull(x);
	}

	@Test void a02_withContent() {
		var x = new SeeOtherRoot("test content");
		assertNotNull(x);
	}

	@Test void a03_fluentSetters() throws Exception {
		var x = new SeeOtherRoot();

		// Test setContent(String) returns same instance
		assertSame(x, x.setContent("test content"));

		// Test setContent(HttpEntity) returns same instance
		HttpEntity entity = x.getEntity();
		assertSame(x, x.setContent(entity));

		// Test setHeader2(Header) returns same instance
		assertSame(x, x.setHeader2(BasicHeader.of("X-Test", "test-value")));

		// Test setHeader2(String, String) returns same instance
		assertSame(x, x.setHeader2("X-Test2", "test-value2"));

		// Test setHeaders(List<Header>) returns same instance
		List<Header> headerList = l(BasicHeader.of("X-Header1", "value1"));
		assertSame(x, x.setHeaders(headerList));
		assertEquals("value1", x.getFirstHeader("X-Header1").getValue());

		// Test setHeaders(HeaderList) returns same instance
		var headers = HeaderList.of(BasicHeader.of("X-Header2", "value2"));
		assertSame(x, x.setHeaders(headers));

		// Test setHeaders2(Header...) returns same instance
		assertSame(x, x.setHeaders2(BasicHeader.of("X-Header3", "value3")));

		// Test setLocale2 returns same instance
		assertSame(x, x.setLocale2(Locale.US));

		// Test setLocation(String) returns same instance
		assertSame(x, x.setLocation("servlet:/newpath"));

		// Test setLocation(URI) returns same instance
		assertSame(x, x.setLocation(new URI("http://example.com")));

		// Test setProtocolVersion returns same instance
		assertSame(x, x.setProtocolVersion(new ProtocolVersion("HTTP", 1, 1)));

		// Test setReasonPhrase2 returns same instance
		assertSame(x, x.setReasonPhrase2("Custom Reason"));

		// Test setReasonPhraseCatalog returns same instance
		assertSame(x, x.setReasonPhraseCatalog(null));

		// Test setStatusCode2 returns same instance
		assertSame(x, x.setStatusCode2(303));

		// Test setStatusLine returns same instance
		assertSame(x, x.setStatusLine(BasicStatusLine.create(303, "See Other")));

		// Test setUnmodifiable returns same instance (must be last - makes bean read-only)
		assertSame(x, x.setUnmodifiable());
	}

	@Test void a04_fluentChaining() throws Exception {
		// Test multiple fluent calls can be chained
		var x = new SeeOtherRoot()
			.setHeaders(l(BasicHeader.of("X-Chain", "chained")))
			.setContent("Redirect content");

		assertEquals("chained", x.getFirstHeader("X-Chain").getValue());
	}

	@Test void a05_reusableInstance() {
		// Test that INSTANCE is available and usable
		assertNotNull(SeeOtherRoot.INSTANCE);
	}

	@Test void a06_copy() throws Exception {
		// Test that copy() returns correct type
		var x = new SeeOtherRoot();

		SeeOtherRoot copy = x.copy();

		// Verify it's a different instance
		assertNotSame(x, copy);

		// Verify it returns the correct type (not SeeOther)
		assertInstanceOf(SeeOtherRoot.class, copy);

		// Verify location is copied (default is servlet:/)
		assertNotNull(copy.getFirstHeader("Location"));
		assertTrue(copy.getFirstHeader("Location").getValue().contains("servlet:/"));
	}
}