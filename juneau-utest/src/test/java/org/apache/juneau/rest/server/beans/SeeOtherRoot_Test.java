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
package org.apache.juneau.rest.server.beans;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

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

	@Test void a03_fluentSetters() throws URISyntaxException {
		var x = new SeeOtherRoot();

		assertSame(x, x.setContent("test content"));

		assertSame(x, x.setHeader(HttpStringHeader.of("X-Test", "test-value")));
		assertSame(x, x.setHeader("X-Test2", "test-value2"));

		List<HttpHeader> headerList = l(HttpStringHeader.of("X-Header1", "value1"));
		assertSame(x, x.setHeaders(headerList));

		assertSame(x, x.setHeaders(HttpStringHeader.of("X-Header3", "value3")));

		assertSame(x, x.setLocale(Locale.US));

		assertSame(x, x.setLocation("servlet:/newpath"));
		assertSame(x, x.setLocation(new URI("http://example.com")));

		assertSame(x, x.setProtocolVersion(HttpProtocolVersion.of("HTTP", 1, 1)));

		assertSame(x, x.setReasonPhrase("Custom Reason"));

		assertSame(x, x.setStatusCode(303));

		assertSame(x, x.setUnmodifiable());
	}

	@Test void a04_fluentChaining() {
		var x = new SeeOtherRoot()
			.setHeaders(l(HttpStringHeader.of("X-Chain", "chained")))
			.setContent("Redirect content");

		assertEquals("chained", x.getHeaders().stream().filter(h -> "X-Chain".equalsIgnoreCase(h.getName())).findFirst().orElseThrow().getValue());
	}

	@Test void a05_reusableInstance() {
		assertNotNull(SeeOtherRoot.INSTANCE);
	}
}
