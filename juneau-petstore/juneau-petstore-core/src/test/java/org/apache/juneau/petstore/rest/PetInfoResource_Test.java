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
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class PetInfoResource_Test extends TestBase {

	private static MockRestClient client() {
		return MockRestClient.buildJsonLax(PetInfoResource.class);
	}

	@Test void a01_getChildDescriptions_listsAllThree() throws Exception {
		var content = client().get("/").run().assertStatus(200).getContent().asString();
		assertTrue(content.contains("BeanDescription"));
		assertTrue(content.contains("Hyperlink"));
		assertTrue(content.contains("SeeOtherRoot"));
	}

	@Test void a02_getPetBeanDescription_describesPet() throws Exception {
		var content = client().get("/BeanDescription").run().assertStatus(200).getContent().asString();
		assertTrue(content.contains(Pet.class.getName()));
		assertTrue(content.contains("species"));
		assertTrue(content.contains("photo"));
	}

	@Test void a03_getHyperlink_pointsAtSelf() throws Exception {
		var content = client().get("/Hyperlink").run().assertStatus(200).getContent().asString();
		assertTrue(content.contains("/petstore-info"));
	}

	@Test void a04_getSeeOtherRoot_redirectsToRoot() throws Exception {
		var noRedirect = MockRestClient.create(PetInfoResource.class).disableRedirectHandling().ignoreErrors().build();
		noRedirect.get("/SeeOtherRoot").run().assertStatus(303);
	}
}
