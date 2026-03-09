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
package org.apache.juneau.bson;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class BsonIntArray_Test {

	@Test
	void parseListOfIntegers() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(list(1, 2, 3));
		var result = p.parse(bytes, List.class);
		assertNotNull(result);
		assertEquals(list(1, 2, 3), result);
	}

	@Test
	void parseToObjectReturnsMapWithValue() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(ints(1, 2, 3));
		var result = p.parse(bytes, Object.class);
		assertNotNull(result);
		assertTrue(result instanceof java.util.Map);
		var m = (java.util.Map<?,?>)result;
		assertTrue(m.containsKey("value"));
		assertNotNull(m.get("value"));
	}

	@Test
	void parseHandBuiltBsonArray() throws Exception {
		// Use exact bytes from serializer dump: 26 00 00 00 04 76 61 6c 75 65 00 1a 00 00 00 ...
		var bytes = new byte[] {
			0x26, 0x00, 0x00, 0x00,  // root size 38
			0x04, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x00,  // 0x04 + "value\0"
			0x1a, 0x00, 0x00, 0x00,  // array size 26
			0x10, 0x30, 0x00, 0x01, 0x00, 0x00, 0x00,
			0x10, 0x31, 0x00, 0x02, 0x00, 0x00, 0x00,
			0x10, 0x32, 0x00, 0x03, 0x00, 0x00, 0x00,
			0x00, 0x00  // array null + root null
		};

		var p = BsonParser.create().build();
		var result = p.parse(bytes, int[].class);
		assertNotNull(result);
		assertArrayEquals(new int[]{1, 2, 3}, result);
	}

	@Test
	void parseScalarRoot() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(42);
		var result = p.parse(bytes, int.class);
		assertNotNull(result);
		assertEquals(42, result);
	}

	@Test
	void parseIntArray() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.serialize(ints(1, 2, 3));
		var result = p.parse(bytes, int[].class);
		assertNotNull(result);
		assertArrayEquals(new int[]{1, 2, 3}, result);
	}

	@Test
	void bigDecimalRoundTrip() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var value = new BigDecimal("123.45");
		var bytes = s.serialize(value);
		var result = p.parse(bytes, BigDecimal.class);
		assertNotNull(result);
		assertEquals(0, value.compareTo(result), "BigDecimal round-trip via Decimal128");
	}
}
