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

class BulkResult_Test extends TestBase {

	@Test void a01_contractVersion_isOwnConstant() {
		assertEquals("1", BulkResult.CONTRACT_VERSION);
	}

	/** The bulk def contract moved to "2"; the aggregate response contract did not move with it. */
	@Test void a01b_contractVersion_independentOfBulkContract() {
		assertEquals("2", BulkMutateDef.CONTRACT_VERSION);
		assertEquals("1", Json.to(Json.of(BulkResult.create().build()), Map.class).get("contractVersion"));
	}

	@Test void a02_builder_collectsSucceededNotFoundAndFailed() {
		var r = BulkResult.create()
			.succeeded("c-1", "c-2")
			.notFound("c-9")
			.failed(new BulkResult.Failure("c-3", "Change is already merged."))
			.build();
		assertEquals(List.of("c-1", "c-2"), r.succeeded());
		assertEquals(List.of("c-9"), r.notFound());
		assertEquals(List.of(new BulkResult.Failure("c-3", "Change is already merged.")), r.failed());
		assertEquals("1", r.contractVersion());
	}

	@Test void a03_builder_repeatedCallsAccumulate() {
		var r = BulkResult.create().succeeded("c-1").succeeded("c-2").build();
		assertEquals(List.of("c-1", "c-2"), r.succeeded());
	}

	@Test void a04_builder_defaultsToEmptyLists_neverNull() {
		var r = BulkResult.create().build();
		assertEquals(List.of(), r.succeeded());
		assertEquals(List.of(), r.notFound());
		assertEquals(List.of(), r.failed());
	}

	@Test void a05_serializedForm_roundTrips() {
		var r = BulkResult.create()
			.succeeded("c-1")
			.notFound("c-9")
			.failed(new BulkResult.Failure("c-3", "Change is already merged."))
			.build();
		var m = Json.to(Json.of(r), Map.class);
		assertEquals("1", m.get("contractVersion"));
		assertEquals(List.of("c-1"), m.get("succeeded"));
		assertEquals(List.of("c-9"), m.get("notFound"));
		var failed = (List<?>)m.get("failed");
		assertEquals(1, failed.size());
		var f0 = (Map<?,?>)failed.get(0);
		assertEquals("c-3", f0.get("id"));
		assertEquals("Change is already merged.", f0.get("message"));
	}
}
