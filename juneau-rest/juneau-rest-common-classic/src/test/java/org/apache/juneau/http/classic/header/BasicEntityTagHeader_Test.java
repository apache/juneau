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
 * Validates {@link BasicEntityTagHeader}.
 */
class BasicEntityTagHeader_Test extends TestBase {

	private static final String NAME = "ETag";

	@Test void a01_of_typedValue() {
		var h = BasicEntityTagHeader.of(NAME, EntityTag.of("\"xyzzy\""));
		assertEquals(NAME, h.getName());
		assertEquals("\"xyzzy\"", h.getValue());
		assertNull(BasicEntityTagHeader.of(NAME, (EntityTag)null));
	}

	@Test void a02_of_wireString() {
		assertEquals("W/\"xyzzy\"", BasicEntityTagHeader.of(NAME, "W/\"xyzzy\"").getValue());
		assertNull(BasicEntityTagHeader.of(NAME, (String)null));
	}

	@Test void a03_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicEntityTagHeader.of(NAME, () -> { calls[0]++; return EntityTag.of("\"a\""); });
		assertEquals(0, calls[0]);
		assertEquals("\"a\"", h.getValue());
		assertEquals(1, calls[0]);
		assertNull(BasicEntityTagHeader.of(NAME, (Supplier<EntityTag>)null));
	}

	@Test void b01_asEntityTag_orElse_toEntityTag() {
		var tag = EntityTag.of("\"a\"");
		var h = new BasicEntityTagHeader(NAME, tag);
		assertEquals(tag, h.asEntityTag().get());
		assertEquals(tag, h.orElse((EntityTag)null));
		assertEquals(tag, h.toEntityTag());
		var none = new BasicEntityTagHeader(NAME, (Supplier<EntityTag>)() -> null);
		assertTrue(none.asEntityTag().isEmpty());
		assertEquals(tag, none.orElse(tag));
		assertNull(none.toEntityTag());
		assertNull(none.getValue());
	}
}
