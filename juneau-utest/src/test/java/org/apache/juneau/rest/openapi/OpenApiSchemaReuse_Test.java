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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 coverage for {@link BasicOpenApiProviderSession}: verifies that schemas referenced from
 * two or more operations end up as a single entry in {@code components.schemas} with each
 * operation slot rewritten to a {@code $ref} pointer.
 *
 * <p>
 * Splits coverage between the bean-reference path (which goes through
 * {@code definitions} → {@code components.schemas} in the Swagger 2.0 → OpenAPI 3.1 transform) and
 * the inline-duplicate path (which is handled by the explicit dedup pass on the OpenAPI side).
 */
class OpenApiSchemaReuse_Test extends TestBase {

	public void testMethod() { /* no-op */ }

	private static OpenApi getOpenApi(Object resource) throws Exception {
		var rc = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, false));
		var roc = new RestOpContext(OpenApiSchemaReuse_Test.class.getMethod("testMethod"), rc);
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		var p = rc.getOpenApiProvider();
		return p.getOpenApi(rc, req.getLocale());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Bean class referenced from two operations → single components.schemas entry, two $refs.
	//------------------------------------------------------------------------------------------------------------------

	public static class Pet {
		public int id;
		public String name;
	}

	@Rest
	public static class A extends BasicRestResource {
		@RestGet(path="/pet") public Pet getPet() { return new Pet(); }
		@RestPost(path="/pet") public Pet createPet(@Content Pet pet) { return pet; }
	}

	@Test void a01_beanReferencedFromTwoOps_singleComponentsEntry() throws Exception {
		var doc = getOpenApi(new A());
		var schemas = doc.getComponents().getSchemas();
		assertNotNull(schemas, "components.schemas should be populated");
		assertTrue(schemas.containsKey("Pet"), () -> "components.schemas should contain Pet — actual keys: " + schemas.keySet());

		// Both operations must point at the same lifted schema via $ref.
		var paths = doc.getPaths();
		var get200 = paths.get("/pet").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
		var postReq = paths.get("/pet").getPost().getRequestBody().getContent().get("application/json").getSchema();
		assertEquals("#/components/schemas/Pet", get200.getRef());
		assertEquals("#/components/schemas/Pet", postReq.getRef());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Inline-duplicate schemas (no bean type) — explicit dedup pass lifts them with a synthesized name.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_inlineDuplicateSchemas_areHoisted() {
		// Pre-built OpenAPI document with two operations carrying identical inline response schemas.
		// Hand-built to avoid relying on a specific bean → schema emission shape.
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		paths.put("/a", makePath(makeInlineSchemaResponse()));
		paths.put("/b", makePath(makeInlineSchemaResponse()));
		doc.put("paths", paths);

		var deduped = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		var components = (Map<?,?>) deduped.get("components");
		var schemas = (Map<?,?>) components.get("schemas");
		assertEquals(1, schemas.size(), () -> "Expected one lifted schema, got: " + schemas);

		// Both operation responses now hold a $ref pointing at the lifted schema.
		var aRef = refOf(deduped, "/a");
		var bRef = refOf(deduped, "/b");
		assertEquals(aRef, bRef);
		assertTrue(aRef.startsWith("#/components/schemas/"), () -> "Unexpected ref: " + aRef);
	}

	@Test void b02_singleOccurrence_isNotHoisted() {
		// Only one occurrence → no dedup, no components.schemas entry.
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		paths.put("/a", makePath(makeInlineSchemaResponse()));
		doc.put("paths", paths);

		var deduped = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		assertFalse(deduped.containsKey("components"), () -> "No components block should be added when nothing dedups; doc = " + deduped);
	}

	@Test void b03_titleIsUsedAsHoistedName() {
		// Inline schema carries a title — that title is preferred as the lifted schema's key.
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		var op1 = makeInlineSchemaResponse();
		stampTitle(op1, "Greeting");
		var op2 = makeInlineSchemaResponse();
		stampTitle(op2, "Greeting");
		paths.put("/a", makePath(op1));
		paths.put("/b", makePath(op2));
		doc.put("paths", paths);

		var deduped = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		var schemas = (Map<?,?>) ((Map<?,?>) deduped.get("components")).get("schemas");
		assertTrue(schemas.containsKey("Greeting"), () -> "Expected 'Greeting' key — got: " + schemas.keySet());
		assertEquals("#/components/schemas/Greeting", refOf(deduped, "/a"));
	}

	private static void stampTitle(Json5Map op, String title) {
		op.getMap("responses").getMap("200").getMap("content").getMap("application/json").getMap("schema").put("title", title);
	}

	private static Json5Map makePath(Json5Map opMap) {
		var path = new Json5Map();
		path.put("get", opMap);
		return path;
	}

	private static Json5Map makeInlineSchemaResponse() {
		// Operation has one response with an inline schema (no $ref) at the application/json content slot.
		var op = new Json5Map();
		var responses = new Json5Map();
		var r200 = new Json5Map();
		r200.put("description", "OK");
		var content = new Json5Map();
		var media = new Json5Map();
		var schema = new Json5Map();
		schema.put("type", "object");
		var properties = new Json5Map();
		var nameProp = new Json5Map();
		nameProp.put("type", "string");
		properties.put("name", nameProp);
		schema.put("properties", properties);
		media.put("schema", schema);
		content.put("application/json", media);
		r200.put("content", content);
		responses.put("200", r200);
		op.put("responses", responses);
		return op;
	}

	private static String refOf(Json5Map doc, String pathKey) {
		var paths = (Map<?,?>) doc.get("paths");
		var p = (Map<?,?>) paths.get(pathKey);
		var op = (Map<?,?>) p.get("get");
		var resp = (Map<?,?>) op.get("responses");
		var r200 = (Map<?,?>) resp.get("200");
		var content = (Map<?,?>) r200.get("content");
		var media = (Map<?,?>) content.get("application/json");
		var schema = (Map<?,?>) media.get("schema");
		return String.valueOf(schema.get("$ref"));
	}
}
