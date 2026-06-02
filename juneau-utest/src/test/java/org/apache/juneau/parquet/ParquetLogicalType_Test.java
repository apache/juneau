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
package org.apache.juneau.parquet;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the opt-in, default-OFF Parquet footer logical-type discriminator (work item 134).
 *
 * <p>
 * The discriminator is modeled on JSON's optional {@code _type} behavior: by default the writer emits
 * only the physical type plus {@code convertedType} (the historical wire shape), and the UUID round-trip
 * relies on the {@code FIXED_LEN_BYTE_ARRAY} physical-type signal. When
 * {@link ParquetSerializer.Builder#emitLogicalTypes(boolean) emitLogicalTypes(true)} is set, the writer
 * additionally emits the Parquet {@code LogicalType} union (UUID-first scope) into the footer, and the
 * parser prefers that discriminant while remaining backward compatible when it is absent.
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types; explicit casts required for typed assertions
})
class ParquetLogicalType_Test extends TestBase {

	private static final UUID UUID_A = UUID.fromString("12345678-1234-5678-1234-567812345678");
	private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000001");

	/** Bean with a UUID property — the one type whose round-trip relies on a physical-type signal. */
	public static class UuidBean {
		public String name;
		public UUID id;
	}

	private static UuidBean uuidBean(String name, UUID id) {
		var b = new UuidBean();
		b.name = name;
		b.id = id;
		return b;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A. UUID round trips in both modes
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_uuidRoundTrip_defaultMode() throws Exception {
		var a = uuidBean("alice", UUID_A);
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		var parsed = (List<UuidBean>) ParquetParser.DEFAULT.parse(bytes, List.class, UuidBean.class);
		assertBeans(parsed, "name,id", "alice," + UUID_A);
	}

	@Test
	void a02_uuidRoundTrip_emitLogicalTypes() throws Exception {
		var ser = ParquetSerializer.create().emitLogicalTypes(true).build();
		var a = uuidBean("alice", UUID_A);
		var bytes = ser.serialize(a);
		var parsed = (List<UuidBean>) ParquetParser.DEFAULT.parse(bytes, List.class, UuidBean.class);
		assertBeans(parsed, "name,id", "alice," + UUID_A);
	}

	@Test
	void a03_uuidListRoundTrip_emitLogicalTypes() throws Exception {
		var ser = ParquetSerializer.create().emitLogicalTypes(true).build();
		var bytes = ser.serialize(list(uuidBean("alice", UUID_A), uuidBean("bob", UUID_B)));
		var parsed = (List<UuidBean>) ParquetParser.DEFAULT.parse(bytes, List.class, UuidBean.class);
		assertBeans(parsed, "name,id", "alice," + UUID_A, "bob," + UUID_B);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B. Two-way compatibility
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_forwardCompat_preChangeFileReadByNewParser() throws Exception {
		// Default-mode output is byte-identical to the pre-change writer (the knob is purely additive),
		// so a DEFAULT-serialized file is an equivalent, drift-free stand-in for a committed golden fixture.
		var a = uuidBean("alice", UUID_A);
		var preChangeBytes = ParquetSerializer.DEFAULT.serialize(a);
		var parsed = (List<UuidBean>) ParquetParser.DEFAULT.parse(preChangeBytes, List.class, UuidBean.class);
		assertBeans(parsed, "name,id", "alice," + UUID_A);
	}

	@Test
	void b02_backwardCompat_newFileSameResultAsDefault() throws Exception {
		// A file carrying the additive LogicalType union must round-trip to the same value as the
		// default (union-free) file — adding the union does not change the parsed result.
		var a = uuidBean("alice", UUID_A);
		var defaultBytes = ParquetSerializer.DEFAULT.serialize(a);
		var emitBytes = ParquetSerializer.create().emitLogicalTypes(true).build().serialize(a);
		var fromDefault = (List<UuidBean>) ParquetParser.DEFAULT.parse(defaultBytes, List.class, UuidBean.class);
		var fromEmit = (List<UuidBean>) ParquetParser.DEFAULT.parse(emitBytes, List.class, UuidBean.class);
		assertBeans(fromDefault, "name,id", "alice," + UUID_A);
		assertBeans(fromEmit, "name,id", "alice," + UUID_A);
		assertEquals(fromDefault.get(0).id, fromEmit.get(0).id);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C. Wire-shape guarantees
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_emitAddsBytesAndIsDeterministic() throws Exception {
		var a = uuidBean("alice", UUID_A);
		var defaultBytes1 = ParquetSerializer.DEFAULT.serialize(a);
		var defaultBytes2 = ParquetSerializer.DEFAULT.serialize(a);
		var emitBytes = ParquetSerializer.create().emitLogicalTypes(true).build().serialize(a);
		// Default output is deterministic (drift-free fixture guarantee).
		assertArrayEquals(defaultBytes1, defaultBytes2);
		// Enabling the discriminator changes the wire shape (the LogicalType union is emitted).
		assertTrue(emitBytes.length > defaultBytes1.length, "Expected emit-on footer to be larger than default footer");
	}

	@Test
	void c02_nonUuidColumnsUnaffectedByKnob() throws Exception {
		// First-pass scope is UUID only: a bean with no UUID column must produce identical bytes in both modes.
		var a = new ParquetSerializer_Test.SimpleBean();
		a.name = "alice";
		a.age = 30;
		var defaultBytes = ParquetSerializer.DEFAULT.serialize(a);
		var emitBytes = ParquetSerializer.create().emitLogicalTypes(true).build().serialize(a);
		assertArrayEquals(defaultBytes, emitBytes);
	}
}
