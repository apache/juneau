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
package org.apache.juneau.proto;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.marshaller.Proto;
import org.junit.jupiter.api.Test;

/**
 * Tests for Proto media type configuration.
 */
class ProtoMediaType_Test {

	@Test
	void a01_producesCorrectMediaType() throws Exception {
		var ct = ProtoSerializer.DEFAULT.getResponseContentType();
		assertEquals("text", ct.getType());
		assertEquals("protobuf", ct.getSubType());
	}

	@Test
	void a02_consumesCorrectMediaType() throws Exception {
		var types = ProtoParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("protobuf")), "Expected protobuf types: " + types);
	}

	@Test
	void a03_contentNegotiation() throws Exception {
		var bean = JsonMap.of("x", 1);
		var proto = Proto.of(bean);
		assertNotNull(proto);
		var parsed = Proto.to(proto, JsonMap.class);
		assertEquals(1L, parsed.get("x"));
	}
}
