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
package org.apache.juneau.petstore.console.pages;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * The per-column pill renderers: one definition per enum column, so a value has the same colour on every page.
 */
class PillRenders_Test extends TestBase {

	private static String json(Render r) {
		return Json.of(r);
	}

	@Test void a01_petStatus() {
		assertEquals("{\"id\":\"pill\",\"meta\":{\"tones\":{\"AVAILABLE\":\"success\",\"PENDING\":\"warning\",\"SOLD\":\"neutral\"}}}", json(PillRenders.petStatus()));
	}

	@Test void a02_orderStatus() {
		assertEquals("{\"id\":\"pill\",\"meta\":{\"tones\":{\"PLACED\":\"info\",\"APPROVED\":\"warning\",\"DELIVERED\":\"success\"}}}", json(PillRenders.orderStatus()));
	}

	@Test void a03_userStatus() {
		assertEquals("{\"id\":\"pill\",\"meta\":{\"tones\":{\"ACTIVE\":\"success\",\"INACTIVE\":\"neutral\"}}}", json(PillRenders.userStatus()));
	}

	@Test void a04_changeState() {
		assertEquals("{\"id\":\"pill\",\"meta\":{\"tones\":{\"PENDING\":\"warning\",\"APPLIED\":\"success\",\"FAILED\":\"error\"}}}", json(PillRenders.changeState()));
	}

	@Test void a05_jobState() {
		assertEquals("{\"id\":\"pill\",\"meta\":{\"tones\":{\"RUNNING\":\"info\",\"DONE\":\"success\",\"EXPIRED\":\"neutral\"}}}", json(PillRenders.jobState()));
	}

	@Test void a06_auditAction() {
		assertEquals("{\"id\":\"pill\",\"meta\":{\"tones\":{\"CREATE\":\"success\",\"UPDATE\":\"info\",\"DELETE\":\"error\",\"DISCARD\":\"warning\",\"RESTOCK\":\"info\"}}}", json(PillRenders.auditAction()));
	}

	@Test void a07_species_pinsAreDistinctAndOffPaletteFree() {
		var meta = (Map<?,?>)Json.to(json(PillRenders.species()), Map.class).get("meta");
		assertEquals(true, meta.get("categorical"));
		var cats = (Map<?,?>)meta.get("categories");
		assertEquals(7, cats.size());
		assertEquals(7, new HashSet<>(cats.values()).size(), "pinned species slots must be collision-free");
		assertFalse(cats.containsValue(3) || cats.containsValue(5), "slots 3 and 5 are reserved from petstore pins");
	}

	@Test void a08_entity_pinsAreDistinct() {
		var meta = (Map<?,?>)Json.to(json(PillRenders.entity()), Map.class).get("meta");
		var cats = (Map<?,?>)meta.get("categories");
		assertEquals(5, cats.size());
		assertEquals(5, new HashSet<>(cats.values()).size());
		assertFalse(cats.containsValue(3) || cats.containsValue(5));
	}
}
