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
package org.apache.juneau.petstore.console.data;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.dto.Order;
import org.apache.juneau.petstore.service.*;
import org.junit.jupiter.api.*;

class PetstoreSeed_Test extends TestBase {

	private static PetStore seeded() {
		return PetstoreSeed.create().populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
	}

	private static String dump(PetStore s) throws Exception {
		var pets = new ArrayList<>(s.getPets());
		pets.sort(Comparator.comparingLong(Pet::getId));
		var orders = new ArrayList<>(s.getOrders());
		orders.sort(Comparator.comparingLong(Order::getId));
		var users = new ArrayList<>(s.getUsers());
		users.sort(Comparator.comparing(User::getUsername));
		return Json5.of(List.of(pets, orders, users, s.getAudit()));
	}

	@Test void a01_counts() {
		var s = seeded();
		assertSize(500, s.getPets());
		assertSize(2000, s.getOrders());
		assertSize(50, s.getUsers());
		assertSize(300, s.getAudit());
	}

	@Test void a02_classicRowsKeepTheirIds() {
		var s = seeded();
		assertBean(s.getPet(1), "id,name,species", "1,Mr. Frisky,CAT");
		assertBean(s.getOrder(101), "id,petId,shipDate,status", "101,101,2018-01-01,PLACED");
		assertBean(s.getUser("mwatson"), "firstName,lastName", "Marie,Watson");
	}

	@Test void a03_generatedIdRanges() {
		var s = seeded();
		assertNotNull(s.getPet(10));
		assertNotNull(s.getPet(500));
		assertNull(s.getPet(501));
		assertNotNull(s.getOrder(104));
		assertNotNull(s.getOrder(2100));
		assertNotNull(s.getUser("user04"));
		assertNotNull(s.getUser("user50"));
	}

	@Test void a04_countersAdvancePastSeed() {
		var s = seeded();
		assertEquals(501L, s.createPet(new Pet().setName("Next")).getId());
		assertEquals(2101L, s.createOrder(new Order().setPetId(1)).getId());
		assertEquals(301L, s.getAudit().get(300).getId());
	}

	@Test void a05_deterministic() throws Exception {
		assertEquals(dump(seeded()), dump(seeded()));
	}

	@Test void a06_differentSeedDiffers() throws Exception {
		var other = PetstoreSeed.create().randomSeed(1L).populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
		assertNotEquals(dump(seeded()), dump(other));
	}

	@Test void a07_auditBeforeClockAndOrdered() {
		var audit = seeded().getAudit();
		var now = PetstoreSeed.DEFAULT_CLOCK.instant();
		for (var i = 0; i < audit.size(); i++) {
			var e = audit.get(i);
			assertEquals(i + 1L, e.getId());
			assertTrue(e.getAt().isBefore(now), "audit " + e.getId() + " not before clock");
			if (i > 0)
				assertFalse(e.getAt().isBefore(audit.get(i - 1).getAt()), "audit not oldest-first at " + e.getId());
		}
	}

	@Test void a08_everyStatusAndSpeciesPresent() {
		var s = seeded();
		for (var st : PetStatus.values())
			assertTrue(s.getPets().stream().anyMatch(p -> p.getStatus() == st), "no pet with status " + st);
		for (var sp : Species.values())
			assertTrue(s.getPets().stream().anyMatch(p -> p.getSpecies() == sp), "no pet of species " + sp);
		for (var os : OrderStatus.values())
			assertTrue(s.getOrders().stream().anyMatch(o -> o.getStatus() == os), "no order with status " + os);
	}

	@Test void a09_populateRequiresClassicStore() {
		var s = PetstoreSeed.create().populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
		var seed = PetstoreSeed.create();
		var e = assertThrows(IllegalArgumentException.class, () -> seed.populate(s));
		assertString("PetstoreSeed.populate expects a store holding only the classic rows; found '500' pets", e.getMessage());
	}
}
