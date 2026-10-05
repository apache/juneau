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
package org.apache.juneau.rest.server.datatables;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests {@link DataTablesResults}: the {@code long} counts and the {@link DataTablesResults#error(int, String)} factory.
 */
class DataTablesResults_Test {

	@Nested class A_settersAndGetters {

		@Test void a01_drawRoundTrips() {
			assertEquals(7, DataTablesResults.<String>create().setDraw(7).getDraw());
		}

		@Test void a02_recordsTotalIsLong() {
			assertEquals(5_000_000_000L, DataTablesResults.<String>create().setRecordsTotal(5_000_000_000L).getRecordsTotal());
		}

		@Test void a03_recordsFilteredIsLong() {
			assertEquals(3_000_000_000L, DataTablesResults.<String>create().setRecordsFiltered(3_000_000_000L).getRecordsFiltered());
		}

		@Test void a04_dataRoundTrips() {
			assertList(DataTablesResults.<String>create().setData(List.of("a", "b")).getData(), "a", "b");
		}

		@Test void a05_errorRoundTrips() {
			assertEquals("boom", DataTablesResults.<String>create().setError("boom").getError());
		}

		@Test void a06_defaultsAreZeroAndNull() {
			assertBean(DataTablesResults.<String>create(), "draw,recordsTotal,recordsFiltered,data,error", "0,0,0,<null>,<null>");
		}
	}

	@Nested class B_errorFactory {

		@Test void b01_errorFactorySetsDrawAndMessage() {
			DataTablesResults<Object> r = DataTablesResults.error(9, "Unknown column 'x'.");
			assertBean(r, "draw,error", "9,Unknown column 'x'.");
		}

		@Test void b02_errorFactoryLeavesCountsAndDataAtDefaults() {
			DataTablesResults<Object> r = DataTablesResults.error(1, "boom");
			assertBean(r, "recordsTotal,recordsFiltered,data", "0,0,<null>");
		}
	}
}
