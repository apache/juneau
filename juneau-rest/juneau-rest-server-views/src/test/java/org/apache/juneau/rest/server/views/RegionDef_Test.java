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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link RegionDef#validate()} matrix: id presence, populator-name allowlisting, and blank
 * {@link RegionDef#allowedPopulators} entries.
 */
class RegionDef_Test extends TestBase {

	@Test void a01_create_andFluentChain() {
		var r = RegionDef.create("sidebar").populate("myWidget").allowPopulators("myWidget");
		assertEquals("sidebar", r.id);
		assertEquals("myWidget", r.populate);
		assertEquals(java.util.Set.of("myWidget"), r.allowedPopulators);
	}

	@Test void a02_contractVersion_isOne() {
		assertEquals("1", RegionDef.CONTRACT_VERSION);
	}

	@Test void b01_nullPopulate_accepted() {
		RegionDef.create("sidebar").validate();
	}

	@Test void b02_defaultPopulate_accepted() {
		RegionDef.create("sidebar").populate("default").validate();
	}

	@Test void b03_unallowlistedPopulate_rejected() {
		var r = RegionDef.create("sidebar").populate("myWidget");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("myWidget"), e::getMessage);
	}

	@Test void b04_optedInPopulate_accepted() {
		RegionDef.create("sidebar").populate("myWidget").allowPopulators("myWidget").validate();
	}

	@Test void c01_blankAllowedPopulatorsEntry_rejected() {
		var r = RegionDef.create("sidebar").allowPopulators("myWidget", "  ");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("allowPopulators"), e::getMessage);
	}

	@Test void d01_nullId_rejected() {
		var r = RegionDef.create(null);
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("id"), e::getMessage);
	}

	@Test void d02_blankId_rejected() {
		var r = RegionDef.create("  ");
		assertThrows(IllegalArgumentException.class, r::validate);
	}
}
