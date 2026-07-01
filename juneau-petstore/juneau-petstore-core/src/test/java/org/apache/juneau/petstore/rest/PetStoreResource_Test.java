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
package org.apache.juneau.petstore.rest;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.dto.Order;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class PetStoreResource_Test extends TestBase {

	/**
	 * Builds a JSON-configured MockRestClient: requests carry {@code Content-Type: application/json}, responses
	 * carry {@code Accept: application/json}, and 4xx/5xx responses do not throw.
	 */
	private static MockRestClient client() {
		return MockRestClient.buildJsonLax(PetStoreResource.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a — pets
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_getPet_byId() throws Exception {
		var pet = client().get("/pets/1").run().assertStatus(200).getContent().as(Pet.class);
		assertBean(pet, "id,name,species,status", "1,Mr. Frisky,CAT,AVAILABLE");
	}

	@Test void a02_getPet_unknownId_404() throws Exception {
		client().get("/pets/99999").run().assertStatus(404);
	}

	@Test void a03_getPets_returnsAllSeeded() throws Exception {
		var content = client().get("/pets").run().assertStatus(200).getContent().asString();
		// Seed has 9 pets — confirm the first one's name is in the response.
		assertNotNull(content);
		assertTrue(content.contains("Mr. Frisky"), "expected seeded pet name in response: " + content);
	}

	@Test void a04_createPet_roundTrip() throws Exception {
		var c = client();
		var created = c.post("/pets", new Pet()
				.setName("Wanda")
				.setSpecies(Species.RABBIT)
				.setPrice(12.50f)
				.setStatus(PetStatus.AVAILABLE))
			.run()
			.assertStatus(200)
			.getContent().as(Pet.class);
		assertNotEquals(0L, created.getId());

		var fetched = c.get("/pets/" + created.getId()).run().assertStatus(200).getContent().as(Pet.class);
		assertBean(fetched, "name,species,status", "Wanda,RABBIT,AVAILABLE");
	}

	@Test void a05_updatePet_replaces() throws Exception {
		var c = client();
		c.put("/pets/1", new Pet().setName("Renamed").setSpecies(Species.CAT).setStatus(PetStatus.AVAILABLE))
			.run().assertStatus(200);
		var fetched = c.get("/pets/1").run().assertStatus(200).getContent().as(Pet.class);
		assertEquals("Renamed", fetched.getName());
	}

	@Test void a06_updatePet_unknownId_404() throws Exception {
		client().put("/pets/99999", new Pet().setName("Ghost").setSpecies(Species.CAT).setStatus(PetStatus.AVAILABLE))
			.run().assertStatus(404);
	}

	@Test void a07_deletePet_removes() throws Exception {
		var c = client();
		c.delete("/pets/2").run().assertStatus(200);
		c.get("/pets/2").run().assertStatus(404);
	}

	@Test void a08_deletePet_unknownId_404() throws Exception {
		client().delete("/pets/99999").run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b — orders
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getOrder_byId() throws Exception {
		var o = client().get("/orders/101").run().assertStatus(200).getContent().as(Order.class);
		assertBean(o, "id,petId,shipDate,status", "101,101,2018-01-01,PLACED");
	}

	@Test void b02_getOrder_unknownId_404() throws Exception {
		client().get("/orders/99999").run().assertStatus(404);
	}

	@Test void b03_createOrder_roundTrip() throws Exception {
		var c = client();
		var created = c.post("/orders", new Order().setPetId(1L).setStatus(OrderStatus.PLACED))
			.run().assertStatus(200).getContent().as(Order.class);
		assertNotEquals(0L, created.getId());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c — users
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getUser_byUsername() throws Exception {
		var u = client().get("/users/mwatson").run().assertStatus(200).getContent().as(User.class);
		assertBean(u, "username,firstName,lastName,userStatus", "mwatson,Marie,Watson,ACTIVE");
	}

	@Test void c02_getUser_unknown_404() throws Exception {
		client().get("/users/no-such-user").run().assertStatus(404);
	}

	@Test void c03_createUser_roundTrip() throws Exception {
		var c = client();
		c.post("/users", new User().setUsername("newuser").setFirstName("New").setUserStatus(UserStatus.ACTIVE))
			.run().assertStatus(200);
		var fetched = c.get("/users/newuser").run().assertStatus(200).getContent().as(User.class);
		assertBean(fetched, "username,firstName", "newuser,New");
	}

	@Test void c04_deleteUser_removes() throws Exception {
		var c = client();
		c.delete("/users/dvaughn").run().assertStatus(200);
		c.get("/users/dvaughn").run().assertStatus(404);
	}
}
