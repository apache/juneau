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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link ParquetColumnWriter}.
 */
class ParquetColumnWriter_Test {

	private static ParquetSchemaElement schema(Integer typeLength) {
		return new ParquetSchemaElement("col", null, typeLength, null, null, null, null, null, null, null);
	}

	private static ParquetColumnWriter writer() {
		return new ParquetColumnWriter(schema(null));
	}

	// -----------------------------------------------------------------------
	// a — writeByteArray(byte[]): null and non-null branches
	// -----------------------------------------------------------------------

	@Test void a01_writeByteArray_null_treated_as_empty() throws IOException {
		var w = writer();
		w.writeByteArray((byte[]) null);
		var bytes = w.finalizePage();
		// null → empty array → writeInt32(0), no payload bytes → 4 bytes total
		assertEquals(4, bytes.length);
		assertEquals(0, bytes[0]); // length = 0
	}

	@Test void a02_writeByteArray_empty_no_payload_written() throws IOException {
		var w = writer();
		w.writeByteArray(new byte[0]);
		var bytes = w.finalizePage();
		assertEquals(4, bytes.length);
		assertEquals(0, bytes[0]);
	}

	@Test void a03_writeByteArray_non_empty_payload_written() throws IOException {
		var w = writer();
		w.writeByteArray(new byte[]{1, 2, 3});
		var bytes = w.finalizePage();
		// 4-byte length prefix + 3 payload bytes
		assertEquals(7, bytes.length);
		assertEquals(3, bytes[0]);
		assertEquals(1, bytes[4]);
		assertEquals(2, bytes[5]);
		assertEquals(3, bytes[6]);
	}

	// -----------------------------------------------------------------------
	// b — writeByteArray(String): null branch
	// -----------------------------------------------------------------------

	@Test void b01_writeByteArray_string_null() throws IOException {
		var w = writer();
		w.writeByteArray((String) null);
		var bytes = w.finalizePage();
		assertEquals(4, bytes.length); // 4-byte length = 0
	}

	@Test void b02_writeByteArray_string_non_null() throws IOException {
		var w = writer();
		w.writeByteArray("hi");
		var bytes = w.finalizePage();
		assertEquals(6, bytes.length); // 4-byte length + 2-byte payload
		assertEquals(2, bytes[0]);
	}

	// -----------------------------------------------------------------------
	// c — writeFixedLenByteArray: null and typeLength null (early return) vs padding
	// -----------------------------------------------------------------------

	@Test void c01_writeFixedLenByteArray_null_value_returns_early() {
		var w = writer(); // schema typeLength = null
		w.writeFixedLenByteArray(null);
		var bytes = w.finalizePage();
		// early return → nothing written
		assertEquals(0, bytes.length);
		assertEquals(0, w.getValueCount());
	}

	@Test void c02_writeFixedLenByteArray_null_typeLength_returns_early() {
		var w = writer(); // schema typeLength = null → early return even with non-null value
		w.writeFixedLenByteArray(new byte[]{1, 2, 3});
		var bytes = w.finalizePage();
		assertEquals(0, bytes.length);
		assertEquals(0, w.getValueCount());
	}

	@Test void c03_writeFixedLenByteArray_value_shorter_than_typeLength_pads_zeros() {
		// typeLength=4, value=[1,2] → writes [1,2,0,0]
		var w = new ParquetColumnWriter(schema(4));
		w.writeFixedLenByteArray(new byte[]{1, 2});
		var bytes = w.finalizePage();
		assertEquals(4, bytes.length);
		assertEquals(1, bytes[0]);
		assertEquals(2, bytes[1]);
		assertEquals(0, bytes[2]); // padding
		assertEquals(0, bytes[3]); // padding
	}

	@Test void c04_writeFixedLenByteArray_value_exact_typeLength() {
		var w = new ParquetColumnWriter(schema(3));
		w.writeFixedLenByteArray(new byte[]{10, 20, 30});
		var bytes = w.finalizePage();
		assertEquals(3, bytes.length);
		assertEquals(10, bytes[0] & 0xFF);
		assertEquals(20, bytes[1] & 0xFF);
		assertEquals(30, bytes[2] & 0xFF);
	}
}
