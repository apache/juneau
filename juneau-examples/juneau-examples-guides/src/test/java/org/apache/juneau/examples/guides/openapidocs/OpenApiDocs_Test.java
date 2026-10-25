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
package org.apache.juneau.examples.guides.openapidocs;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.examples.guides.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

class OpenApiDocs_Test extends TestBase {

	private static final MockRestClient client = MockRestClient.buildJson(TaskResource.class);

	@Test void a01_runOutputMatchesGuide() throws Exception {
		assertEquals(GuideRegion.read("OpenApiDocs/run-output.txt", "output"), GuideRegion.normalize(TaskDocsDemo.run()));
	}

	@Test void a02_swaggerV2IsServed() throws Exception {
		var body = client.get("/api").accept("application/json").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"swagger\":\"2.0\""), body);
	}

	@Test void a03_openApiYamlIsServed() throws Exception {
		var body = client.get("/openapi.yaml").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("openapi:"), body);
	}

	@Test void a04_redocRendersHtml() throws Exception {
		client.get("/redoc").accept("text/html").run().assertStatus(200).assertContent().asString().isContains("<html");
	}

	@Test void a05_apiStillWorks() throws Exception {
		client.post("/tasks", new Task(0, "Write docs", false)).run().assertStatus(200);
		client.get("/tasks").run().assertStatus(200).assertContent().asString().isContains("Write docs");
	}

	@Test void a06_schemaDescriptionsAreDocumented() throws Exception {
		var body = client.get("/openapi.json").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("A to-do item.") && body.contains("Task identifier.") && body.contains("Returns every task, oldest first."), body);
	}
}
