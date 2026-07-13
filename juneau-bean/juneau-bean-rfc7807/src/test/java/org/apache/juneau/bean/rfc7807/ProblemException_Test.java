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
package org.apache.juneau.bean.rfc7807;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ProblemException} — construction, factory, getter round-trip, and throw/catch behavior.
 */
class ProblemException_Test extends TestBase {

	@Test
	void a01_constructor_storesProblem() {
		var problem = Problem.fromStatus(404, "Not Found", "Order 1 missing");
		var ex = new ProblemException(problem);
		assertSame(problem, ex.getProblem());
	}

	@Test
	void a02_constructor_nullProblem_isAllowed() {
		var ex = new ProblemException(null);
		assertNull(ex.getProblem());
		assertNull(ex.getMessage());
	}

	@Test
	void a03_message_fromDetail() {
		var problem = Problem.fromStatus(500, "Internal Server Error", "boom");
		var ex = new ProblemException(problem);
		assertEquals("boom", ex.getMessage());
	}

	@Test
	void a04_message_fallsBackToTitle_whenDetailNull() {
		var problem = new Problem().setStatus(403).setTitle("Forbidden");
		var ex = new ProblemException(problem);
		assertEquals("Forbidden", ex.getMessage());
	}

	@Test
	void a05_message_nullWhenBothDetailAndTitleNull() {
		var problem = new Problem().setStatus(418);
		var ex = new ProblemException(problem);
		assertNull(ex.getMessage());
	}

	@Test
	void a06_factory_of_returnsNewInstance() {
		var problem = Problem.fromStatus(409, "Conflict", "Version mismatch");
		var ex = ProblemException.of(problem);
		assertSame(problem, ex.getProblem());
		assertEquals("Version mismatch", ex.getMessage());
	}

	@Test
	void a07_throwCatch_roundTrip() {
		var problem = Problem.fromStatus(429, "Too Many Requests", "Slow down");
		try {
			throw new ProblemException(problem);
		} catch (ProblemException caught) {
			assertSame(problem, caught.getProblem());
			assertEquals(Integer.valueOf(429), caught.getProblem().getStatus());
			assertEquals("Too Many Requests", caught.getProblem().getTitle());
			assertEquals("Slow down", caught.getProblem().getDetail());
		}
	}

	@Test
	void a08_isRuntimeException() {
		var ex = new ProblemException(Problem.fromStatus(500, "Internal Server Error", "x"));
		assertTrue(ex instanceof RuntimeException);
	}

	@Test
	void a09_preservesExtensionFields() {
		var problem = Problem.fromStatus(403, "Insufficient credit", "Balance 30 < cost 50")
			.set("balance", 30)
			.set("accounts", "[a,b]");
		var ex = new ProblemException(problem);
		var recovered = ex.getProblem();
		assertEquals(30, recovered.get("balance"));
		assertEquals("[a,b]", recovered.get("accounts"));
	}
}
