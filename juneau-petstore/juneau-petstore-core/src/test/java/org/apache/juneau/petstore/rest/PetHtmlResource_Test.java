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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class PetHtmlResource_Test extends TestBase {

	private static MockRestClient client() {
		return MockRestClient.buildJsonLax(PetHtmlResource.class);
	}

	@Test void a01_getChildDescriptions_listsCardAndTable() throws Exception {
		var content = client().get("/").run().assertStatus(200).getContent().asString();
		assertTrue(content.contains("table"));
		assertTrue(content.contains("card/1"));
	}

	@Test void a02_getPetCard_rendersSeededPet() throws Exception {
		var content = client().get("/card/1").run().assertStatus(200).getContent().asString();
		assertTrue(content.contains("Mr. Frisky"));
		assertTrue(content.contains("CAT"));
	}

	@Test void a03_getPetCard_unknownId_404() throws Exception {
		client().get("/card/99999").run().assertStatus(404);
	}

	@Test void a04_getPetTable_rendersAllSeededPets() throws Exception {
		var content = client().get("/table").run().assertStatus(200).getContent().asString();
		assertTrue(content.contains("Name"));
		assertTrue(content.contains("Mr. Frisky"));
		assertTrue(content.contains("Kibbles"));
	}
}
