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
package org.apache.juneau.petstore.service;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.dto.Order;
import org.junit.jupiter.api.*;

class PetStore_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a — seed loading
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_seededPetsLoaded() {
		var s = new PetStore();
		assertEquals(9, s.getPets().size());
	}

	@Test void a02_seededOrdersLoaded() {
		var s = new PetStore();
		assertEquals(3, s.getOrders().size());
	}

	@Test void a03_seededUsersLoaded() {
		var s = new PetStore();
		assertEquals(3, s.getUsers().size());
	}

	@Test void a04_seededPetByIdHasCorrectShape() {
		var s = new PetStore();
		// First seeded pet — id auto-assigned to 1.
		assertBean(s.getPet(1L), "name,species,price,status", "Mr. Frisky,CAT,39.99,AVAILABLE");
	}

	@Test void a05_seededOrderById() {
		var s = new PetStore();
		assertBean(s.getOrder(101L), "id,petId,shipDate,status", "101,101,2018-01-01,PLACED");
	}

	@Test void a06_seededUserByUsername() {
		var s = new PetStore();
		assertBean(s.getUser("mwatson"), "username,firstName,lastName,userStatus", "mwatson,Marie,Watson,ACTIVE");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b — pets CRUD
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_createPet_assignsId() {
		var s = new PetStore();
		var p = s.createPet(new Pet().setName("Newpet").setSpecies(Species.DOG).setPrice(10f).setStatus(PetStatus.AVAILABLE));
		assertNotEquals(0L, p.getId());
		assertEquals(p, s.getPet(p.getId()));
	}

	@Test void b02_updatePet_replacesContent() {
		var s = new PetStore();
		var p = s.getPet(1L).setName("Renamed");
		s.updatePet(p);
		assertEquals("Renamed", s.getPet(1L).getName());
	}

	@Test void b03_updatePet_unknownId_throws() {
		var s = new PetStore();
		var ghost = new Pet().setId(99999L).setName("Ghost").setSpecies(Species.CAT).setStatus(PetStatus.AVAILABLE);
		assertThrows(PetstoreNotFoundException.class, () -> s.updatePet(ghost));
	}

	@Test void b04_deletePet_removes() {
		var s = new PetStore();
		s.deletePet(1L);
		assertNull(s.getPet(1L));
	}

	@Test void b05_deletePet_unknownId_throws() {
		var s = new PetStore();
		assertThrows(PetstoreNotFoundException.class, () -> s.deletePet(99999L));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c — orders CRUD
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_createOrder_assignsId() {
		var s = new PetStore();
		var o = s.createOrder(new Order().setPetId(1L).setStatus(OrderStatus.PLACED));
		assertNotEquals(0L, o.getId());
		assertTrue(o.getId() > 103L); // greater than highest seeded id
	}

	@Test void c02_updateOrder_replacesContent() {
		var s = new PetStore();
		var o = s.getOrder(101L).setStatus(OrderStatus.DELIVERED);
		s.updateOrder(o);
		assertEquals(OrderStatus.DELIVERED, s.getOrder(101L).getStatus());
	}

	@Test void c03_deleteOrder_removes() {
		var s = new PetStore();
		s.deleteOrder(101L);
		assertNull(s.getOrder(101L));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d — users CRUD
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_createUser_byUsername() {
		var s = new PetStore();
		s.createUser(new User().setUsername("newuser").setFirstName("New").setUserStatus(UserStatus.ACTIVE));
		assertNotNull(s.getUser("newuser"));
	}

	@Test void d02_createUser_duplicate_throws() {
		var s = new PetStore();
		var u = new User().setUsername("mwatson").setUserStatus(UserStatus.ACTIVE);
		assertThrows(IllegalArgumentException.class, () -> s.createUser(u));
	}

	@Test void d03_updateUser_replacesContent() {
		var s = new PetStore();
		s.updateUser(s.getUser("mwatson").setFirstName("Mary"));
		assertEquals("Mary", s.getUser("mwatson").getFirstName());
	}

	@Test void d04_deleteUser_removes() {
		var s = new PetStore();
		s.deleteUser("mwatson");
		assertNull(s.getUser("mwatson"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e — audit trail
	//------------------------------------------------------------------------------------------------------------------

	private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

	private static PetStore fixedStore() {
		return new PetStore(Clock.fixed(T0, ZoneOffset.UTC));
	}

	private static Pet newPet(String name) {
		return new Pet().setName(name).setSpecies(Species.DOG).setPrice(10f).setStatus(PetStatus.AVAILABLE);
	}

	@Test void e01_classicSeedWritesNoAudit() {
		assertEmpty(fixedStore().getAudit());
	}

	@Test void e02_createPet_apiActorByDefault() {
		var s = fixedStore();
		var p = s.createPet(newPet("Newpet"));
		assertBeans(s.getAudit(), "id,at,actor,entity,entityId,action,detail",
			"1,2026-10-01T12:00:00Z,api,Pet," + p.getId() + ",CREATE,Newpet");
	}

	@Test void e03_updatePet_consoleActor() {
		var s = fixedStore();
		var p = s.getPet(1).setName("Renamed");
		s.updatePet(p, "console:alice");
		assertBeans(s.getAudit(), "actor,entity,entityId,action,detail", "console:alice,Pet,1,UPDATE,Renamed");
	}

	@Test void e04_deleteOrder_audited() {
		var s = fixedStore();
		s.deleteOrder(101, "api");
		assertBeans(s.getAudit(), "entity,entityId,action", "Order,101,DELETE");
	}

	@Test void e05_createUser_keyedByUsername() {
		var s = fixedStore();
		s.createUser(new User().setUsername("zed").setFirstName("Zed"), "console:admin");
		assertBeans(s.getAudit(), "actor,entity,entityId,action,detail", "console:admin,User,zed,CREATE,zed");
	}

	@Test void e06_failedMutationAppendsNothing() {
		var s = fixedStore();
		assertThrows(PetstoreNotFoundException.class, () -> s.deletePet(99_999));
		var existing = s.getUser("mwatson");
		assertThrows(IllegalArgumentException.class, () -> s.createUser(existing));
		assertEmpty(s.getAudit());
	}

	@Test void e07_auditIdsIncrease() {
		var s = fixedStore();
		s.createPet(newPet("A"));
		s.createPet(newPet("B"));
		assertBeans(s.getAudit(), "id,detail", "1,A", "2,B");
	}

	@Test void e08_getAuditIsImmutableSnapshot() {
		var s = fixedStore();
		s.createPet(newPet("A"));
		var snap = s.getAudit();
		s.createPet(newPet("B"));
		assertSize(1, snap);
		var entry = new AuditEntry();
		assertThrows(UnsupportedOperationException.class, () -> snap.add(entry));
	}

	@Test void e09_load_preservesIdsAndBumpsCounters() {
		var s = fixedStore();
		s.load(
			List.of(new Pet().setId(700).setName("Loaded").setSpecies(Species.CAT).setStatus(PetStatus.SOLD)),
			List.of(new Order().setId(900).setPetId(700).setStatus(OrderStatus.PLACED)),
			List.of(new User().setUsername("loaded")),
			List.of(new AuditEntry().setId(40).setAt(T0).setActor("api").setEntity("Pet").setEntityId("700").setAction("CREATE")));
		assertBean(s.getPet(700), "id,name", "700,Loaded");
		assertBean(s.getOrder(900), "id,petId", "900,700");
		assertBean(s.getUser("loaded"), "username", "loaded");
		assertEquals(701L, s.createPet(newPet("Next")).getId());
		assertEquals(901L, s.createOrder(new Order().setPetId(1).setStatus(OrderStatus.PLACED)).getId());
		assertBeans(s.getAudit(), "id", "40", "41", "42");
	}

	@Test void e10_recordAudit_public() {
		var s = fixedStore();
		var e = s.recordAudit("job:restock", "Pet", "*", "RESTOCK", "3 per species");
		assertBean(e, "id,actor,action,detail", "1,job:restock,RESTOCK,3 per species");
	}
}
