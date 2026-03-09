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
import org.junit.jupiter.api.*;

/**
 * Tests for BSON media type configuration.
 */
class BsonMediaType_Test extends TestBase {

	@Test
	void a01_producesCorrectMediaType() {
		var ct = BsonSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("bson", ct.getSubType());
	}

	@Test
	void a02_acceptsMediaTypes() {
		var types = new java.util.ArrayList<String>();
		BsonSerializer.DEFAULT.forEachAcceptMediaType(mt -> types.add(mt.getType() + "/" + mt.getSubType()));
		assertTrue(types.stream().anyMatch(t -> t.contains("bson")), "Expected application/bson: " + types);
	}

	@Test
	void a03_consumesMediaTypes() {
		var types = BsonParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("bson")), "Expected bson types: " + types);
	}
}
