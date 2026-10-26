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
package org.apache.juneau.petstore.console.users;

import static org.apache.juneau.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class UsersRest_Test extends TestBase {

	private static final String QUERY = "{\"draw\":1,\"length\":500,\"columns\":[{\"data\":\"username\"},{\"data\":\"firstName\"},{\"data\":\"lastName\"}]}";

	private static MockRestClient client() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static String create(MockRestClient c, String username, String password) throws Exception {
		return c.post("/console/users", "{\"username\":\"" + username + "\",\"firstName\":\"New\",\"lastName\":\"Bie\",\"password\":\"" + password + "\"}")
			.contentType("application/json").run().assertStatus(200).getContent().asString();
	}

	@Test void a01_passwordNeverLeaves() throws Exception {
		var c = client();
		c.post("/console/users/query", QUERY).contentType("application/json").run().assertStatus(200)
			.assertContent().isContains("mwatson");
		c.post("/console/users/query", QUERY).contentType("application/json").run().assertStatus(200)
			.assertContent().isNotContains("password", "\"pw\"", "123456");
		c.get("/console/users/mwatson").run().assertStatus(200).assertContent().isNotContains("password");
	}

	@Test void b01_createWithPassword() throws Exception {
		var r = new JsonMap(create(client(), "newbie1", "s3cret"));
		assertBean(r, "outcome,message,row{username}", "success,Added 'newbie1',{newbie1}");
		assertNotContains("s3cret", r.toString());
	}

	@Test void b02_duplicate409() throws Exception {
		var c = client();
		create(c, "dupe1", "x");
		c.post("/console/users", "{\"username\":\"dupe1\",\"firstName\":\"M\",\"lastName\":\"W\",\"password\":\"x\"}").contentType("application/json").run()
			.assertStatus(409).assertContent().isContains("User 'dupe1' already exists");
	}

	@Test void b03_updateWithoutPasswordKeepsIt() throws Exception {
		var c = client();
		create(c, "keeper1", "s3cret");
		c.put("/console/users/keeper1", "{\"firstName\":\"Mary\",\"lastName\":\"Watson\"}").contentType("application/json").run().assertStatus(200);
		c.get("/console/users/keeper1").run().assertContent().isContains("Mary");
		assertBean(PetstoreConsoleFixture.store().getUser("keeper1"), "password", "s3cret");
		c.put("/console/users/keeper1", "{\"firstName\":\"Mary\",\"password\":\"n3w\"}").contentType("application/json").run().assertStatus(200);
		assertBean(PetstoreConsoleFixture.store().getUser("keeper1"), "password", "n3w");
	}

	@Test void b04_blankUsername400() throws Exception {
		client().post("/console/users", "{\"username\":\" \",\"password\":\"x\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Username must not be blank");
	}

	@Test void b05_deleteUnknown404() throws Exception {
		client().delete("/console/users/nobody").run().assertStatus(404).assertContent().isContains("Unknown user 'nobody'");
	}

	@Test void b06_dialogEnvelope() throws Exception {
		var c = client();
		c.post("/console/users", "{\"fields\":{\"username\":\"envy1\",\"firstName\":\"E\",\"password\":\"p\"}}").contentType("application/json").run().assertStatus(200);
		c.put("/console/users/envy1", "{\"fields\":{\"firstName\":\"Env\"}}").contentType("application/json").run().assertStatus(200);
		c.get("/console/users/envy1").run().assertContent().isContains("Env");
		c.delete("/console/users/envy1").run().assertStatus(200);
		c.get("/console/users/envy1").run().assertStatus(404).assertContent().isContains("Unknown user 'envy1'");
	}

	@Test void c01_formIsWriteOnly() throws Exception {
		var c = client();
		c.get("/console/users/form").run().assertStatus(200).assertContent().isContains("Password", "password");
		c.get("/console/users/form?username=mwatson").run().assertStatus(200).assertContent().isContains("Leave blank to keep the current password");
		c.get("/console/users/form?username=mwatson").run().assertStatus(200).assertContent().isNotContains("123456");
		c.get("/console/users/form?username=nobody").run().assertStatus(404);
	}

	@Test void c02_formPasswordFieldIsMaskedAndHasNoValue() throws Exception {
		var c = client();
		for (var path : new String[]{"/console/users/form", "/console/users/form?username=mwatson"}) {
			var body = c.get(path).run().assertStatus(200).getContent().asString();
			assertContains("\"name\":\"password\"", body);
			var field = body.substring(body.indexOf("\"name\":\"password\""));
			field = field.substring(0, field.indexOf('}'));
			assertContains("\"type\":\"password\"", field);
			assertNotContains("\"value\"", field);
		}
	}
}
