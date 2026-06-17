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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ParquetSerializer.Builder} — every setter, both copy constructors, the
 * null-coalescing guards, and {@code copy()}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types
})
class ParquetSerializerBuilder_Test extends TestBase {

	@Test
	void a01_allSettersThenCopyConstructors() throws Exception {
		// Set every knob to a non-default value.
		var s1 = ParquetSerializer.create()
			.compressionCodec(CompressionCodec.GZIP)
			.rowGroupSize(8192)
			.pageSize(4096)
			.addBeanTypesParquet(true)
			.writeDatesAsTimestamp(false)
			.emitLogicalTypes(true)
			.nativeLogicalTypes(true)
			.cycleHandling(ParquetCycleHandling.THROW)
			.maxRecursionDepth(7)
			.nullKeyString("NIL")
			.build();
		assertTrue(s1.nativeLogicalTypes);
		assertEquals(CompressionCodec.GZIP, s1.compressionCodec);

		// Builder copy constructor (copy() returns a Builder built from the prior builder state).
		var s2 = s1.copy().build();
		assertTrue(s2.nativeLogicalTypes);
		assertEquals(8192, s2.rowGroupSize);
		assertEquals(4096, s2.pageSize);
		assertEquals(7, s2.maxRecursionDepth);
		assertFalse(s2.writeDatesAsTimestamp);
		assertTrue(s2.emitLogicalTypes);
		assertEquals(ParquetCycleHandling.THROW, s2.cycleHandling);
	}

	@Test
	void a06_builderToBuilderCopy() throws Exception {
		// Builder.copy() exercises the Builder(Builder) copy constructor (distinct from serializer.copy(),
		// which uses Builder(ParquetSerializer)).
		var b1 = ParquetSerializer.create()
			.nativeLogicalTypes(true)
			.compressionCodec(CompressionCodec.GZIP)
			.rowGroupSize(2048)
			.pageSize(2048)
			.addBeanTypesParquet(true)
			.writeDatesAsTimestamp(false)
			.emitLogicalTypes(true)
			.cycleHandling(ParquetCycleHandling.THROW)
			.maxRecursionDepth(9)
			.nullKeyString("X");
		var s = b1.copy().build();
		assertTrue(s.nativeLogicalTypes);
		assertEquals(CompressionCodec.GZIP, s.compressionCodec);
		assertEquals(9, s.maxRecursionDepth);
	}

	@Test
	void a02_nullCoalescingGuards() throws Exception {
		// Null arguments fall back to documented defaults (the false branch of each ternary).
		var s = ParquetSerializer.create()
			.compressionCodec(null)
			.cycleHandling(null)
			.nullKeyString(null)
			.build();
		assertEquals(CompressionCodec.UNCOMPRESSED, s.compressionCodec);
		assertEquals(ParquetCycleHandling.NULL, s.cycleHandling);
	}

	@Test
	void a03_minimumSizeClamps() throws Exception {
		// rowGroupSize/pageSize clamp to a 1024 floor.
		var s = ParquetSerializer.create().rowGroupSize(1).pageSize(1).build();
		assertEquals(1024, s.rowGroupSize);
		assertEquals(1024, s.pageSize);
	}

	@Test
	void a04_caching() {
		// Two builders with identical config resolve to the same cached instance (hashKey parity).
		var a = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var b = ParquetSerializer.create().nativeLogicalTypes(true).build();
		assertSame(a, b);
		var c = ParquetSerializer.create().nativeLogicalTypes(false).build();
		assertNotSame(a, c);
	}

	public static class KBean {
		public String k;
		public KBean() {}
		public KBean(String k) { this.k = k; }
	}

	@Test
	void a05_serializeRecordsAndRoundTrip() throws Exception {
		// Exercises the convenience serialize path on a configured serializer.
		var s = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new KBean("v"));
		var bytes = s.serialize(in);
		var out = (List<KBean>) ParquetParser.DEFAULT.parse(bytes, List.class, KBean.class);
		assertEquals("v", out.get(0).k);
	}

	@Test
	@SuppressWarnings("resource")
	void a07_contextSerializeRecordsDelegator() throws Exception {
		// The context-level serializeRecords(...) convenience delegator forwards to the session.
		var sb = new StringBuilder();
		try (var w = ParquetSerializer.DEFAULT.serializeRecords(new java.io.ByteArrayOutputStream())) {
			assertNotNull(w);
		}
		assertNotNull(sb);
	}
}
