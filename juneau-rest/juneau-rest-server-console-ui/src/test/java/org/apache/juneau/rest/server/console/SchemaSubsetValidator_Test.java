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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Exercises the {@code integer} and {@code null} branches of {@link SchemaSubsetValidator#typeMatches}
 * directly. The shipped {@code juneau-page.schema.json} only ever uses {@code object}, {@code array},
 * {@code string} and {@code boolean}, so those two branches are otherwise never reached by
 * {@link PageContractSchema_Test}.
 */
class SchemaSubsetValidator_Test extends TestBase {

	private static SchemaSubsetValidator validator(String type) {
		return new SchemaSubsetValidator(Map.of("type", type));
	}

	@Test void a01_integer_matchesIntegerAndLong() {
		assertEquals(List.of(), validator("integer").validate(1));
		assertEquals(List.of(), validator("integer").validate(1L));
	}

	@Test void a02_integer_rejectsNonIntegral() {
		assertEquals(List.of("$: expected integer"), validator("integer").validate("x"));
	}

	@Test void a03_null_matchesNull() {
		assertEquals(List.of(), validator("null").validate(null));
	}

	@Test void a04_null_rejectsNonNull() {
		assertEquals(List.of("$: expected null"), validator("null").validate("x"));
	}
}
