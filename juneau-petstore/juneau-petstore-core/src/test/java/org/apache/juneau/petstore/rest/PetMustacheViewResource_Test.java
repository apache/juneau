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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

class PetMustacheViewResource_Test extends TestBase {

	private static final MockRestClient CLIENT = MockRestClient.buildLax(PetMustacheViewResource.class);

	@Test void a01_viewPet_rendersTemplateWithPetName() throws Exception {
		CLIENT.get("/pets/1/view")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Mr. Frisky")
			.assertContent().asString().isContains("Rendered via Mustache.");
	}

	@Test void a02_viewPet_rendersSpeciesAndStatus() throws Exception {
		CLIENT.get("/pets/1/view")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("CAT")
			.assertContent().asString().isContains("AVAILABLE");
	}

	@Test void a03_viewPet_unknownId_404() throws Exception {
		CLIENT.get("/pets/99999/view")
			.run()
			.assertStatus(404);
	}
}
