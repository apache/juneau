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
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicEntityTagsHeader}.
 */
class BasicEntityTagsHeader_Test extends TestBase {

	private static final String NAME = "If-Match";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_typedValue() {
		var h = BasicEntityTagsHeader.of(NAME, EntityTags.of("\"xyzzy\""));
		assertEquals("\"xyzzy\"", h.getValue());
	}

	@Test void a02_of_typedValue_null_returnsNull() {
		assertNull(BasicEntityTagsHeader.of(NAME, (EntityTags)null));
	}

	@Test void a03_of_wireString() {
		var h = BasicEntityTagsHeader.of(NAME, "\"xyzzy\", \"r2d2xxxx\"");
		assertEquals("\"xyzzy\", \"r2d2xxxx\"", h.getValue());
	}

	@Test void a04_of_wireString_null_returnsNull() {
		assertNull(BasicEntityTagsHeader.of(NAME, (String)null));
	}

	@Test void a05_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicEntityTagsHeader.of(NAME, () -> { calls[0]++; return EntityTags.of("\"xyzzy\""); });
		assertEquals(0, calls[0]);
		assertEquals("\"xyzzy\"", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a06_of_supplier_null_returnsNull() {
		assertNull(BasicEntityTagsHeader.of(NAME, (Supplier<EntityTags>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asEntityTags_present() {
		var h = new BasicEntityTagsHeader(NAME, "\"xyzzy\"");
		assertTrue(h.asEntityTags().isPresent());
	}

	@Test void b02_asEntityTags_absent() {
		var h = new BasicEntityTagsHeader(NAME, (EntityTags)null);
		assertTrue(h.asEntityTags().isEmpty());
	}
}
