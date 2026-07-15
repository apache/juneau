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
package org.apache.juneau.petstore.jetty;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.client.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.testing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * Integration test that drives the petstore's own REST API through the typed {@link PetStoreClient}
 * {@code @Remote} interface, over a real HTTP round-trip against a live Jetty-hosted server.
 *
 * <p>
 * Complements {@link PetstoreJetty_Test} (which exercises the API via a plain {@code java.net.http.HttpClient}) by
 * proving that Juneau's own next-generation {@link RestClient} can consume a Juneau REST resource end-to-end via
 * {@link RestClient#remote(Class)} — no hand-written request/response wiring, just the typed interface.
 */
@JettyMicroserviceTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@SuppressWarnings({
	"resource" // newClient() returns a Closeable owned by the caller (closed via try-with-resources in each test); Eclipse JDT @Owning warning is by design.
})
class PetStoreClient_Test {

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(App.AppConfig.class);

	/**
	 * Builds a JSON-speaking client rooted at the fixture's live server.
	 *
	 * <p>
	 * The next-gen {@link RestClient} has no implicit JSON fallback (10.0.0 removed it) — the parser set and
	 * default serializer must be configured explicitly.
	 */
	private static RestClient newClient() {
		return RestClient.builder()
			.rootUrl(fixture.getRootUrl().toString())
			.parsers(ParserSet.create().add(JsonParser.DEFAULT).build())
			.defaultSerializer(JsonSerializer.DEFAULT)
			.build();
	}

	@Test void a01_typedClientListsSeededPets() throws Exception {
		try (var client = newClient()) {
			var svc = client.remote(PetStoreClient.class);
			var pets = svc.getPets();
			assertEquals(9, pets.size());
		}
	}

	@Test void a02_typedClientGetsSeededPetById() throws Exception {
		try (var client = newClient()) {
			var svc = client.remote(PetStoreClient.class);
			var pet = svc.getPet(1L);
			assertEquals("Mr. Frisky", pet.getName());
		}
	}

	@Test void a03_typedClientAddsPet_thenVisibleInList() throws Exception {
		try (var client = newClient()) {
			var svc = client.remote(PetStoreClient.class);
			var created = svc.addPet(new Pet().setSpecies(Species.DOG).setName("Fido").setPrice(25.0f).setStatus(PetStatus.AVAILABLE));
			assertNotEquals(0L, created.getId());

			var fetched = svc.getPet(created.getId());
			assertEquals("Fido", fetched.getName());
		}
	}
}
