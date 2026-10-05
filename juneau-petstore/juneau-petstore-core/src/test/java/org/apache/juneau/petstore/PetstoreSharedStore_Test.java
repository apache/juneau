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
package org.apache.juneau.petstore;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that one seeded {@link PetStore} is shared by every petstore resource mounted under a group.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A group registers the seeded store as a plain bean; children inherit it.</jc>
 * 	<ja>@Bean</ja> <jk>public</jk> PetStore petStore() {
 * 		<jk>return</jk> PetstoreSeed.<jsm>create</jsm>().populate(<jk>new</jk> PetStore());
 * 	}
 * </p>
 */
@SuppressWarnings({
	"resource" // Mock clients/responses are in-memory, so closing them is a no-op.
})
class PetstoreSharedStore_Test extends TestBase {

	private static PetStore seeded() {
		return PetstoreSeed.create().populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
	}

	@Rest(children={PetStoreResource.class, PetHtmlResource.class})
	public static class Host extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;

		@Bean public PetStore petStore() {
			return seeded();
		}
	}

	@Rest(path="/mid", children={PetStoreResource.class})
	public static class Mid extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(children={Mid.class})
	public static class DeepHost extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;

		@Bean public PetStore petStore() {
			return seeded();
		}
	}

	@Test void a01_childrenSeeTheSeededStore() throws Exception {
		var c = MockRestClient.buildJsonLax(Host.class);
		c.get("/petstore/pets/500").run().assertStatus(200);
	}

	@Test void a02_apiWriteVisibleToSiblingResource() throws Exception {
		var c = MockRestClient.buildJsonLax(Host.class);
		var created = c.post("/petstore/pets", new Pet().setName("Sharedpet").setSpecies(Species.FISH).setStatus(PetStatus.AVAILABLE))
			.run().assertStatus(200).getContent().as(Pet.class);
		var html = c.get("/petstore-html/card/" + created.getId()).accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains("Sharedpet", html);
	}

	@Test void a03_standaloneResourceFallsBackToClassicStore() throws Exception {
		MockRestClient.buildJsonLax(PetStoreResource.class).get("/pets/500").run().assertStatus(404);
	}

	// Plan Q12: does the store reach a resource two levels below the group that registered it?
	@Test void a04_twoLevelsDeepSeesTheSeededStore() throws Exception {
		var c = MockRestClient.buildJsonLax(DeepHost.class);
		c.get("/mid/petstore/pets/500").run().assertStatus(200);
	}
}
