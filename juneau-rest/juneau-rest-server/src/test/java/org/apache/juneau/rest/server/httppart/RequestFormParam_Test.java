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

import java.io.*;

import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestFormParam}.
 *
 * <p>
 * The {@code (RestRequest, String, String)} constructor sets the backing {@code jakarta.servlet.http.Part} to
 * {@code null}; every method exercised below (mirroring {@link RequestHttpPart_Test}'s null-request approach)
 * is safe under that constructor because a non-null {@code value} short-circuits before the {@code part} field
 * is ever dereferenced. The {@code part}-backed constructor and its exclusively part-driven methods
 * ({@code getContentType()}, {@code getHeader(String)}, {@code getSize()}, {@code getSubmittedFileName()}, and
 * the {@code getValue()}/{@code getStream()} lazy-read-from-part branches) require a real multipart request and
 * are left to the higher-level {@code MockRestClient} integration tests per this module's established scope.
 */
class RequestFormParam_Test {

	private static RequestFormParam formParam(String name, String value) {
		return new RequestFormParam(null, name, value);
	}

	@Test void a01_getNameAndValue() {
		var p = formParam("foo", "bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void a02_getContentType_nullWhenNoBackingPart() {
		assertNull(formParam("foo", "bar").getContentType());
	}

	@Test void a03_getStream_readsFromValueWhenPresent() throws IOException {
		var p = formParam("foo", "bar");
		assertEquals("bar", IoUtils.read(p.getStream()));
	}

	@Test void a04_def() {
		assertEquals("bar", formParam("foo", "bar").def("def").getValue());
		assertEquals("def", formParam("foo", null).def("def").getValue());
	}

	@Test void a05_parser() {
		assertNotNull(formParam("foo", "bar").parser(null));
	}

	@Test void a06_schema() {
		assertNotNull(formParam("foo", "bar").schema(null));
	}

	@Test void a07_asString() {
		assertEquals("bar", formParam("foo", "bar").asString().get());
	}

	@Test void a08_isPresent() {
		assertTrue(formParam("foo", "bar").isPresent());
		assertFalse(formParam("foo", null).isPresent());
	}
}
