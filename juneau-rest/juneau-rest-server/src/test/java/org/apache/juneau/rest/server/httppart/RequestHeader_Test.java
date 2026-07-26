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
package org.apache.juneau.rest.server.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestHeader}.
 *
 * <p>
 * None of the {@code as*Header()} convenience methods or the fluent-override setters touch the {@code request}
 * field, so (mirroring {@link RequestHttpPart_Test}) this file constructs {@link RequestHeader} directly with a
 * {@code null} request -- consistent with this module's exclusion of {@code MockRestClient} from its test scope.
 */
class RequestHeader_Test {

	private static RequestHeader header(String name, String value) {
		return new RequestHeader(null, name, value);
	}

	@Test void a01_asBooleanHeader() {
		assertNotNull(header("X-Flag", "true").asBooleanHeader());
	}

	@Test void a02_asCsvHeader() {
		assertNotNull(header("X-List", "a,b").asCsvHeader());
	}

	@Test void a03_asDateHeader() {
		assertNotNull(header("X-Date", null).asDateHeader());
	}

	@Test void a04_asEntityTagHeader() {
		assertNotNull(header("ETag", "\"abc\"").asEntityTagHeader());
	}

	@Test void a05_asEntityTagsHeader() {
		assertNotNull(header("If-Match", "\"abc\"").asEntityTagsHeader());
	}

	@Test void a06_asIntegerHeader() {
		assertNotNull(header("X-Count", "5").asIntegerHeader());
	}

	@Test void a07_asLongHeader() {
		assertNotNull(header("X-Size", "5").asLongHeader());
	}

	@Test void a08_asStringHeader() {
		assertNotNull(header("X-Foo", "bar").asStringHeader());
	}

	@Test void a09_asStringRangesHeader() {
		assertNotNull(header("Accept", "text/plain").asStringRangesHeader());
	}

	@Test void a10_asUriHeader() {
		assertNotNull(header("Location", "http://example.com").asUriHeader());
	}

	@Test void b01_def() {
		assertEquals("def", header("X-Foo", null).def("def").getValue());
		assertEquals("bar", header("X-Foo", "bar").def("def").getValue());
	}

	@Test void b02_parser() {
		assertNotNull(header("X-Foo", "bar").parser(null));
	}

	@Test void b03_schema() {
		assertNotNull(header("X-Foo", "bar").schema(null));
	}

	@Test void c01_toString() {
		assertEquals("X-Foo: bar", header("X-Foo", "bar").toString());
	}
}
