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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class BulkRequest_Test extends TestBase {

	@Test void a01_fields_areSettableAndReadable() {
		var r = new BulkRequest();
		r.ids = List.of("c-1", "c-2");
		r.idempotencyKey = "a1b2c3";
		assertEquals(List.of("c-1", "c-2"), r.ids);
		assertEquals("a1b2c3", r.idempotencyKey);
	}

	@Test void a02_serializedForm_roundTrips() {
		var r = new BulkRequest();
		r.ids = List.of("c-1", "c-2");
		r.idempotencyKey = "a1b2c3";
		var m = Json.to(Json.of(r), Map.class);
		assertEquals(List.of("c-1", "c-2"), m.get("ids"));
		assertEquals("a1b2c3", m.get("idempotencyKey"));
	}

	@Test void a03_deserializedForm_populatesFields() {
		var r = Json.to("{\"ids\":[\"c-1\",\"c-2\"],\"idempotencyKey\":\"a1b2c3\"}", BulkRequest.class);
		assertEquals(List.of("c-1", "c-2"), r.ids);
		assertEquals("a1b2c3", r.idempotencyKey);
	}
}
