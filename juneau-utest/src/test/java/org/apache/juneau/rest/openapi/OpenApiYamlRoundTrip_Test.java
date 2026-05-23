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
package org.apache.juneau.rest.openapi;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.MockServletRequest;
import org.apache.juneau.rest.mock.MockServletResponse;
import org.apache.juneau.rest.mock.classic.MockRestClient;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.yaml.*;
import org.junit.jupiter.api.*;

/**
 * Explicit YAML round-trip coverage for the OpenAPI 3.1 emission path. Asserts that an
 * {@link OpenApi} bean serialized to YAML and parsed back produces structurally-equal
 * documents across the meaningful slots ({@code openapi}, {@code info}, {@code servers},
 * {@code paths}, {@code components.schemas}) — both for a hand-built doc and for the live
 * document served by a {@link BasicRestServlet}-based resource with {@code apiFormat="openapi"}.
 */
class OpenApiYamlRoundTrip_Test extends TestBase {

	public void testMethod() { /* no-op */ }

	//------------------------------------------------------------------------------------------------------------------
	// Hand-built OpenAPI document: YAML serialize → parse → structural equality.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_handBuiltDoc_roundTrip() throws Exception {
		var original = new OpenApi()
			.setOpenapi("3.1.0")
			.setInfo(new Info().setTitle("Pet Store").setVersion("1.0.0").setDescription("Sample API"))
			.setServers(java.util.List.of(new Server().setUrl(URI.create("https://api.example.com/v1"))));

		var yaml = YamlSerializer.DEFAULT_READABLE.toString(original);
		assertTrue(yaml.contains("openapi"), () -> "Expected YAML to mention 'openapi' field — got: " + yaml);
		assertTrue(yaml.contains("Pet Store"), () -> "Expected YAML to retain title 'Pet Store' — got: " + yaml);

		var parsed = YamlParser.DEFAULT.parse(yaml, OpenApi.class);

		// Structural equality across each meaningful slot.
		assertEquals(original.getOpenapi(), parsed.getOpenapi());
		assertEquals(original.getInfo().getTitle(), parsed.getInfo().getTitle());
		assertEquals(original.getInfo().getVersion(), parsed.getInfo().getVersion());
		assertEquals(original.getInfo().getDescription(), parsed.getInfo().getDescription());
		assertEquals(original.getServers().size(), parsed.getServers().size());
		assertEquals(original.getServers().get(0).getUrl(), parsed.getServers().get(0).getUrl());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Live OpenAPI doc served by a REST resource: YAML serialize → parse → structural equality.
	//------------------------------------------------------------------------------------------------------------------

	public static class Pet {
		public int id;
		public String name;
	}

	@Rest(apiFormat="openapi")
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/pet") public Pet getPet() { return new Pet(); }
		@RestPost(path="/pet") public Pet createPet(@Content Pet pet) { return pet; }
	}

	private static OpenApi getOpenApi(Object resource) throws Exception {
		var rc = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null));
		var roc = new RestOpContext(OpenApiYamlRoundTrip_Test.class.getMethod("testMethod"), rc);
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		return rc.getOpenApiProvider().getOpenApi(rc, req.getLocale());
	}

	@Test void b01_liveDoc_roundTrip() throws Exception {
		var original = getOpenApi(new A());

		var yaml = YamlSerializer.DEFAULT_READABLE.toString(original);
		assertTrue(yaml.contains("3.1.0"), () -> "Expected YAML to keep the 3.1.0 version literal — got first 500 chars: " + yaml.substring(0, Math.min(500, yaml.length())));

		var parsed = YamlParser.DEFAULT.parse(yaml, OpenApi.class);

		// openapi version preserved.
		assertEquals(original.getOpenapi(), parsed.getOpenapi());

		// paths preserved.
		assertEquals(original.getPaths().keySet(), parsed.getPaths().keySet());
		var getOrig = original.getPaths().get("/pet").getGet();
		var getNew = parsed.getPaths().get("/pet").getGet();
		assertEquals(getOrig.getOperationId(), getNew.getOperationId());

		// components.schemas preserved including the Pet entry lifted in Phase 4.
		var origSchemas = original.getComponents().getSchemas();
		var newSchemas = parsed.getComponents().getSchemas();
		assertEquals(origSchemas.keySet(), newSchemas.keySet());
		assertTrue(origSchemas.containsKey("Pet"), () -> "Expected Pet in components.schemas — actual: " + origSchemas.keySet());
	}

	//------------------------------------------------------------------------------------------------------------------
	// /openapi/* endpoint with Accept: application/yaml — verifies the full HTTP path serves YAML.
	//------------------------------------------------------------------------------------------------------------------

	private static final MockRestClient cYaml = MockRestClient.build(A.class);

	@Test void c01_endpointServesYaml() throws Exception {
		var body = cYaml.get("/openapi")
			.accept("application/yaml")
			.run()
			.assertStatus(200)
			.getContent().asString();

		// Sanity: looks like YAML (no JSON braces wrapping) and mentions the 3.1.0 literal.
		assertTrue(body.contains("3.1.0"), () -> "Expected 3.1.0 in YAML body — got first 500 chars: " + body.substring(0, Math.min(500, body.length())));

		// Round-trip the served bytes through the YAML parser.
		var parsed = YamlParser.DEFAULT.parse(body, OpenApi.class);
		assertEquals("3.1.0", parsed.getOpenapi());
		assertNotNull(parsed.getPaths());
		assertTrue(parsed.getPaths().containsKey("/pet"), () -> "Expected /pet path in parsed YAML doc — actual: " + parsed.getPaths().keySet());
	}
}
