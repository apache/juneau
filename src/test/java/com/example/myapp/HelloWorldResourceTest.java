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
package com.example.myapp;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Serverless example test: drives {@link HelloWorldResource} via {@link MockRestClient}, and exercises the
 * {@link GreetingApi} typed {@code @Remote} client.
 */
class HelloWorldResourceTest {

	@Test
	void contentNegotiation_servesJsonXmlHtml_andReadsYamlConfig() throws Exception {
		try (var client = MockRestClient.create(HelloWorldResource.class)) {

			try (var res = client.get("/").header("Accept", "application/json").run()) {
				res.assertStatus(200);
				var json = res.body().asString();
				assertTrue(json.contains("Hello from my-app.yaml!"), json);
				assertTrue(json.trim().startsWith("{"), json);
			}

			try (var res = client.get("/").header("Accept", "text/xml").run()) {
				res.assertStatus(200);
				assertTrue(res.body().asString().contains("<"), "expected XML");
			}

			try (var res = client.get("/").header("Accept", "text/html").run()) {
				res.assertStatus(200);
				assertTrue(res.body().asString().contains("<html"), "expected an HTML document");
			}
		}
	}

	@Test
	void typedRemoteClient_bindsPathAndQuery() throws Exception {
		try (var client = MockRestClient.builder(HelloWorldResource.class).defaultParser(JsonParser.DEFAULT).build()) {
			var api = client.getClient().remote(GreetingApi.class);

			var quiet = api.greet("Juneau", false);
			assertEquals("Juneau", quiet.name);
			assertEquals("Hello Juneau!", quiet.message);

			var loud = api.greet("Juneau", true);
			assertEquals("HELLO JUNEAU!", loud.message);
		}
	}
}
