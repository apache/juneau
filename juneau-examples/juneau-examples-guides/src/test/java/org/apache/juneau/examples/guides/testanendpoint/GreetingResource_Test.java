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
package org.apache.juneau.examples.guides.testanendpoint;

// region test
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class GreetingResource_Test {

	@Test
	void a01_getReturnsJson() throws Exception {
		try (var mock = MockRestClient.create(GreetingResource.class);
				var resp = mock.get("/Ada").header("Accept", "application/json").run()) {
			resp.assertThat().isOk().body().contains("\"message\":\"Hello, Ada!\"");
		}
	}

	@Test
	void a02_postParsesBodyIntoBean() throws Exception {
		try (var mock = MockRestClient.builder(GreetingResource.class).defaultParser(JsonParser.DEFAULT).build();
				var resp = mock.post("/").body(new Greeting("Grace")).run()) {
			resp.assertOk();
			var greeting = resp.body().as(Greeting.class);
			assertEquals("Hello, Grace!", greeting.message);
		}
	}

	@Test
	void a03_blankNameIsRejected() throws Exception {
		try (var mock = MockRestClient.builder(GreetingResource.class).noTrace().build();
				var resp = mock.post("/").bodyString("{\"name\":\"\"}").header("Content-Type", "application/json").run()) {
			resp.assertStatus(400);
		}
	}
}
// endregion test
