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
package org.apache.juneau.bean.rfc7807;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Problem_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a — fromStatus factory and basic getters
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_fromStatus_setsFields() {
		var p = Problem.fromStatus(404, "Not Found", "The resource was not found.");
		assertEquals(404, p.getStatus());
		assertEquals("Not Found", p.getTitle());
		assertEquals("The resource was not found.", p.getDetail());
		assertNull(p.getType());
		assertNull(p.getInstance());
	}

	@Test void a02_fromStatus_nullTitleAndDetail() {
		var p = Problem.fromStatus(500, null, null);
		assertEquals(500, p.getStatus());
		assertNull(p.getTitle());
		assertNull(p.getDetail());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b — getTypeOrDefault: null type returns DEFAULT_TYPE; non-null returns as-is
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getTypeOrDefault_nullReturnsDefault() {
		var p = new Problem();
		assertSame(Problem.DEFAULT_TYPE, p.getTypeOrDefault());
	}

	@Test void b02_getTypeOrDefault_nonNullReturnsValue() throws Exception {
		var uri = new URI("https://example.com/probs/out-of-credit");
		var p = new Problem().setType(uri);
		assertSame(uri, p.getTypeOrDefault());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c — extension map: extraKeys(), get(), set()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_extraKeys_emptyWhenNoExtensions() {
		var p = new Problem();
		assertTrue(p.extraKeys().isEmpty(), "extraKeys() must be empty before any set() calls");
	}

	@Test void c02_get_returnsNullWhenNoExtensions() {
		var p = new Problem();
		assertNull(p.get("balance"), "get() must return null when no extensions exist");
	}

	@Test void c03_set_lazyInitializesMap() {
		var p = new Problem();
		p.set("balance", 30);
		assertEquals(30, p.get("balance"));
		assertEquals(1, p.extraKeys().size());
		assertTrue(p.extraKeys().contains("balance"));
	}

	@Test void c04_set_multipleExtensions() {
		var p = new Problem()
			.set("balance", 30)
			.set("accounts", "/account/12345");
		assertEquals(2, p.extraKeys().size());
		assertEquals(30, p.get("balance"));
		assertEquals("/account/12345", p.get("accounts"));
	}

	@Test void c05_get_unknownKeyReturnsNull() {
		var p = new Problem().set("key", "val");
		assertNull(p.get("other"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d — all setters / getters round-trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_settersRoundTrip() throws Exception {
		var type = new URI("https://example.com/problem");
		var instance = new URI("/resource/42");
		var p = new Problem()
			.setType(type)
			.setTitle("Title")
			.setStatus(422)
			.setDetail("Detail text")
			.setInstance(instance);
		assertEquals(type, p.getType());
		assertEquals("Title", p.getTitle());
		assertEquals(422, p.getStatus());
		assertEquals("Detail text", p.getDetail());
		assertEquals(instance, p.getInstance());
	}

	@Test void d02_settersAcceptNull() {
		var p = new Problem()
			.setType(null)
			.setTitle(null)
			.setStatus(null)
			.setDetail(null)
			.setInstance(null);
		assertNull(p.getType());
		assertNull(p.getTitle());
		assertNull(p.getStatus());
		assertNull(p.getDetail());
		assertNull(p.getInstance());
	}
}
