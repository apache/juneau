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
package org.apache.juneau.http.classic.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link ClientVersion}.
 */
class ClientVersion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wireString() {
		var h = ClientVersion.of("2.0.1");
		assertEquals("Client-Version", h.getName());
		assertEquals("2.0.1", h.getValue());
		assertEquals(Version.of("2.0.1"), h.asVersion().get());
	}

	@Test void a02_of_wireString_null_returnsNull() {
		assertNull(ClientVersion.of((String)null));
	}

	@Test void a03_of_wireString_cached_returnsSameInstanceForSameValue() {
		var h1 = ClientVersion.of("3.1.4");
		var h2 = ClientVersion.of("3.1.4");
		assertSame(h1, h2);
	}

	@Test void a04_of_typedValue() {
		var h = ClientVersion.of(Version.of("2.0.1"));
		assertEquals("2.0.1", h.getValue());
	}

	@Test void a05_of_typedValue_null_returnsNull() {
		assertNull(ClientVersion.of((Version)null));
	}

	@Test void a06_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = ClientVersion.of((Supplier<Version>) () -> { calls[0]++; return Version.of("2.0.1"); });
		assertEquals(0, calls[0]);
		assertEquals("2.0.1", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a07_of_supplier_null_returnsNull() {
		assertNull(ClientVersion.of((Supplier<Version>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString_null() {
		var h = new ClientVersion((String)null);
		assertNull(h.getValue());
		assertTrue(h.asVersion().isEmpty());
	}

	@Test void b02_ctor_typedValue_null() {
		var h = new ClientVersion((Version)null);
		assertNull(h.getValue());
		assertTrue(h.asVersion().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asVersion_absent() {
		assertTrue(new ClientVersion((String)null).asVersion().isEmpty());
	}

	@Test void c02_assertVersion() {
		new ClientVersion("2.0.1").assertVersion().asMajor().is(2);
	}
}
