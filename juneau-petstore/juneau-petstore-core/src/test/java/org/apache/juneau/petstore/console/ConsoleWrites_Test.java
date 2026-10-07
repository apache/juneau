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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class ConsoleWrites_Test extends TestBase {

	@Rest(path="/w")
	public static class Probe {
		@RestGet(path="/roles")
		public String roles(RestRequest req) {
			return String.join(",", ConsoleWrites.roles(req));
		}
	}

	private static MockRestClient client() {
		return MockRestClient.create(Probe.class).noTrace().ignoreErrors().build();
	}

	@Test void a01_adminTokenIsAdminRole() throws Exception {
		assertString("admin", client().get("/roles").header("Authorization", "Bearer petstore-admin").run().assertStatus(200).getContent().asString());
	}

	@Test void a02_noTokenHasNoRoles() throws Exception {
		assertString("", client().get("/roles").run().assertStatus(200).getContent().asString());
	}

	@Test void a03_unknownTokenHasNoRoles() throws Exception {
		assertString("", client().get("/roles").header("Authorization", "Bearer nope").run().assertStatus(200).getContent().asString());
	}

	@Test void a04_bearerPrefixIsCaseInsensitive() throws Exception {
		assertString("admin", client().get("/roles").header("Authorization", "BEARER petstore-admin").run().assertStatus(200).getContent().asString());
	}
}
