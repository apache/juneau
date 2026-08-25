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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/**
 * {@link ServerValues} / {@link ServerValuesValue} bean contract: builder ordering, fail-closed
 * {@code validate()}, and the frozen {@code CONTRACT_VERSION}.
 */
class ServerValues_Test extends TestBase {

	private static final Function<VarResolverSession,?> P = s -> "x";

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", ServerValues.CONTRACT_VERSION);
	}

	@Test void a02_value_factory_defaults() {
		var v = ServerValuesValue.of("failedCount", P);
		assertEquals("failedCount", v.name);
		assertSame(P, v.provider);
		assertFalse(v.cacheable);

		var c = ServerValuesValue.of("failedCount", P, true);
		assertTrue(c.cacheable);
	}

	@Test void a03_builder_insertionOrder() {
		var sv = ServerValues.create().value("a", P).value("b", P).value("c", P);
		assertIterableEquals(List.of("a", "b", "c"), new ArrayList<>(sv.values.keySet()));
		sv.validate();
	}

	@Test void a04_value_rejectsNullName() {
		var sv = ServerValues.create();
		assertThrows(IllegalArgumentException.class, () -> sv.value(null, P));
	}

	@Test void a05_value_rejectsBlankName() {
		var sv = ServerValues.create();
		assertThrows(IllegalArgumentException.class, () -> sv.value("  ", P));
	}

	@Test void a06_value_rejectsNullProvider() {
		var sv = ServerValues.create();
		assertThrows(IllegalArgumentException.class, () -> sv.value("a", null));
	}

	@Test void a07_value_rejectsDuplicateNameAtBuilder() {
		var sv = ServerValues.create().value("a", P);
		var e = assertThrows(IllegalArgumentException.class, () -> sv.value("a", P));
		assertTrue(e.getMessage().contains("duplicate"), e::getMessage);
	}

	@Test void a08_validate_rejectsBlankNameInMap() {
		var sv = ServerValues.create();
		var m = new LinkedHashMap<String,ServerValuesValue>();
		m.put("  ", ServerValuesValue.of("  ", P));
		sv.values(m);
		assertThrows(IllegalArgumentException.class, sv::validate);
	}

	@Test void a09_validate_rejectsNullValueInMap() {
		var sv = ServerValues.create();
		var m = new LinkedHashMap<String,ServerValuesValue>();
		m.put("a", null);
		sv.values(m);
		assertThrows(IllegalArgumentException.class, sv::validate);
	}

	@Test void a10_validate_passesForWellFormed() {
		ServerValues.create().value("a", P).value("b", P).validate();
	}

	@Test void a11_values_setterReplacesMap() {
		var m = new LinkedHashMap<String,ServerValuesValue>();
		m.put("z", ServerValuesValue.of("z", P));
		var sv = ServerValues.create().values(m);
		assertSame(m, sv.values);
	}
}
