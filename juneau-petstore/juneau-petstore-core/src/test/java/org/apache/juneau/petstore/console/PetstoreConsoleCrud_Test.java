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

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Writes made on the console are visible to the {@code /petstore} API and the other way round: one shared store.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreConsoleCrud_Test extends TestBase {

	private static MockRestClient json() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	@Test void a01_consolePetIsOnTheApi() throws Exception {
		var c = json();
		var id = new JsonMap(c.post("/console/pets", "{\"name\":\"Crudpet\",\"price\":3}").contentType("application/json").run().assertStatus(200).getContent().asString()).getMap("row").getLong("id");
		c.get("/petstore/pets/" + id).run().assertStatus(200).assertContent().isContains("Crudpet");
		c.delete("/petstore/pets/" + id).run().assertStatus(200);
		c.get("/console/pets/" + id + "/row").run().assertStatus(404);
	}

	@Test void b01_consoleApprovedOrderReadsBackApproved() throws Exception {
		var c = json();
		var id = new JsonMap(c.post("/petstore/orders", "{\"petId\":1,\"status\":\"PLACED\"}").contentType("application/json").run().assertStatus(200).getContent().asString()).getLong("id");
		c.post("/console/orders/" + id + "/approve", "").run().assertStatus(200);
		c.get("/petstore/orders/" + id).run().assertStatus(200).assertContent().isContains("APPROVED");
	}

	@Test void c01_passwordIsKeptAcrossAConsoleEdit() throws Exception {
		var c = json();
		c.post("/petstore/users", "{\"username\":\"crud1\",\"firstName\":\"C\",\"password\":\"kept\"}").contentType("application/json").run().assertStatus(200);
		c.put("/console/users/crud1", "{\"firstName\":\"Changed\"}").contentType("application/json").run().assertStatus(200);
		c.get("/petstore/users/crud1").run().assertContent().isContains("Changed");
		assertBean(store().getUser("crud1"), "password", "kept");
	}

	private static String body(MockRestClient c, String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	@Test void c02_apiNeverReturnsPasswordButAcceptsIt() throws Exception {
		var c = json();
		var created = c.post("/petstore/users", "{\"username\":\"crud2\",\"firstName\":\"D\",\"password\":\"hunter2\"}").contentType("application/json").run().assertStatus(200).getContent().asString();
		assertNotContains("hunter2", created);
		assertBean(store().getUser("crud2"), "password", "hunter2");
		c.get("/petstore/users/crud2").run().assertStatus(200).assertContent().isContains("crud2");
		assertNotContains("password", body(c, "/petstore/users/crud2"));
		assertNotContains("hunter2", body(c, "/petstore/users/crud2"));
		c.get("/petstore/users").run().assertStatus(200).assertContent().isContains("mwatson");
		for (var x : new String[]{"hunter2", "123456", "password"})
			assertNotContains(x, body(c, "/petstore/users"));
		c.put("/petstore/users/crud2", "{\"firstName\":\"E\",\"password\":\"hunter3\"}").contentType("application/json").run().assertStatus(200).assertContent().isNotContains("hunter3");
		assertBean(store().getUser("crud2"), "password", "hunter3");
	}

	@Test void c03_apiPasswordIsHiddenInEveryFormat() throws Exception {
		var c = MockRestClient.create(Host.class).noTrace().ignoreErrors().build();
		// Formats the users endpoint does not serve (e.g. text/ini answers 500, text/prototext 406) fail the same way
		// without this patch, so only 200 responses are checked; the password VALUE must never appear in any of them.
		var checked = 0;
		for (var accept : new String[]{"application/json", "text/json5", "application/jsonl", "text/html", "text/html+stripped", "text/xml", "text/xml+soap", "text/uon", "application/x-www-form-urlencoded", "text/plain", "text/yaml", "text/csv", "text/markdown", "octal/msgpack", "application/cbor", "application/bson", "text/ini", "text/prototext"}) {
			for (var path : new String[]{"/petstore/users/mwatson", "/petstore/users"}) {
				var r = c.get(path).accept(accept).run();
				if (r.getStatusCode() != 200)
					continue;
				checked++;
				var body = r.getContent().asString();
				assertContains("mwatson", body);
				assertNotContains("123456", body);
			}
		}
		assertEquals(33, checked, "of 36 format/path combinations, exactly 33 are served (200)");
	}

	@Test void c04_getThenPutKeepsThePassword() throws Exception {
		var c = json();
		c.post("/petstore/users", "{\"username\":\"crud4\",\"firstName\":\"F\",\"password\":\"keepme\"}").contentType("application/json").run().assertStatus(200);
		var got = body(c, "/petstore/users/crud4");
		c.put("/petstore/users/crud4", got).contentType("application/json").run().assertStatus(200);
		assertBean(store().getUser("crud4"), "password", "keepme");
		c.put("/petstore/users/crud4", "{\"firstName\":\"F\",\"password\":\" \"}").contentType("application/json").run().assertStatus(200);
		assertBean(store().getUser("crud4"), "password", "keepme");
		c.put("/petstore/users/crud4", "{\"firstName\":\"F\",\"password\":\"fresh\"}").contentType("application/json").run().assertStatus(200);
		assertBean(store().getUser("crud4"), "password", "fresh");
	}
}
