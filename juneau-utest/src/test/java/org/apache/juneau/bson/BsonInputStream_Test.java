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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.parser.ParserPipe;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonInputStream}.
 */
class BsonInputStream_Test extends TestBase {

	@Test
	void a01_readDocumentViaParser() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("x", 1, "y", "foo"));
		try (var pipe = new ParserPipe(bytes)) {
			try (var is = new BsonInputStream(pipe)) {
				var size = is.readDocumentSize();
				assertTrue(size > 0);
				assertTrue(size <= bytes.length);
			}
		}
	}

	@Test
	void a02_parseSimpleMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("a", 42, "b", "hello"));
		var p = BsonParser.create().build();
		var result = p.parse(bytes, JsonMap.class);
		assertNotNull(result);
		assertEquals(42, result.get("a"));
		assertEquals("hello", result.get("b"));
	}

	@Test
	void a03_parseArray() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(java.util.List.of(1, 2, 3));
		var p = BsonParser.create().build();
		var result = p.parse(bytes, java.util.List.class);
		assertNotNull(result);
		assertEquals(java.util.List.of(1, 2, 3), result);
	}
}
