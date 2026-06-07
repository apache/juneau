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
package org.apache.juneau.rest.server.openapi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted tests for {@link BasicOpenApiProviderSession}, mirroring the
 * Phase-2 work that was done for {@code BasicSwaggerProviderSession_Coverage_Test}.
 *
 * <p>
 * Splits coverage into two strategies:
 * <ul>
 *   <li>Direct unit tests against the static transform helpers
 *       ({@code transform}, {@code deduplicateInlineSchemas}, {@code rewriteRefs}) using
 *       hand-built Json5 documents — these exercise the OpenAPI 3.1 specific branches that
 *       are independent of the Swagger walker.</li>
 *   <li>End-to-end tests that drive a {@link RestContext}-backed resource through
 *       {@code RestContext.getOpenApiProvider().getOpenApi(...)} so that the live
 *       Swagger 2.0 → OpenAPI 3.1 transformation runs end-to-end on real {@code @Rest} /
 *       {@code @RestOp} annotation graphs (parameter rewrites, requestBody synthesis,
 *       responses content blocks, servers, components.schemas).</li>
 * </ul>
 */
class BasicOpenApiProviderSession_Test extends TestBase {

	public void testMethod() { /* no-op — test fixture method for RestOpContext bootstrap */ }

	private static OpenApi getOpenApi(Object resource) throws Exception {
		var rc = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, false));
		var roc = new RestOpContext(BasicOpenApiProviderSession_Test.class.getMethod("testMethod"), rc);
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		return rc.getOpenApiProvider().getOpenApi(rc, req.getLocale());
	}

	@Marshalled(typeName="Foo")
	public static class X {
		public int a;
	}

	//------------------------------------------------------------------------------------------------------------------
	// transform(Json5Map) — unit tests against the static spec-mapping entry point.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_transform_setsOpenapiVersion() {
		var out = BasicOpenApiProviderSession.transform(new Json5Map());
		assertEquals("3.1.0", out.get("openapi"));
	}

	@Test void a02_transform_copiesInfoTagsExternalDocs() {
		var swagger = new Json5Map();
		swagger.put("info", new Json5Map().append("title", "T"));
		swagger.put("tags", List.of(new Json5Map().append("name", "tagA")));
		swagger.put("externalDocs", new Json5Map().append("url", "https://ex.com"));
		var out = BasicOpenApiProviderSession.transform(swagger);
		assertNotNull(out.get("info"));
		assertNotNull(out.get("tags"));
		assertNotNull(out.get("externalDocs"));
	}

	@Test void a03_transform_buildsServersFromHostBasePathSchemes() {
		var swagger = new Json5Map();
		swagger.put("host", "api.example.com");
		swagger.put("basePath", "/v1");
		swagger.put("schemes", List.of("https", "http"));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var servers = (List<?>) out.get("servers");
		assertEquals(2, servers.size());
		assertEquals("https://api.example.com/v1", ((Map<?,?>)servers.get(0)).get("url"));
		assertEquals("http://api.example.com/v1", ((Map<?,?>)servers.get(1)).get("url"));
	}

	@Test void a04_transform_serversDefaultsToHttp_whenSchemesEmpty() {
		var swagger = new Json5Map();
		swagger.put("host", "h");
		swagger.put("basePath", "/x");
		var out = BasicOpenApiProviderSession.transform(swagger);
		var servers = (List<?>) out.get("servers");
		assertEquals(1, servers.size());
		assertEquals("http://h/x", ((Map<?,?>)servers.get(0)).get("url"));
	}

	@Test void a05_transform_noServers_whenHostBasePathSchemesAllAbsent() {
		var out = BasicOpenApiProviderSession.transform(new Json5Map());
		assertFalse(out.containsKey("servers"));
	}

	@Test void a06_transform_definitionsMovedToComponentsSchemas() {
		var swagger = new Json5Map();
		var defs = new Json5Map();
		defs.put("Foo", new Json5Map().append("type", "object"));
		swagger.put("definitions", defs);
		var out = BasicOpenApiProviderSession.transform(swagger);
		var components = (Map<?,?>) out.get("components");
		var schemas = (Map<?,?>) components.get("schemas");
		assertTrue(schemas.containsKey("Foo"));
	}

	@Test void a07_transform_securityDefinitionsMovedToComponentsSecuritySchemes() {
		var swagger = new Json5Map();
		var sec = new Json5Map();
		sec.put("apiKey", new Json5Map().append("type", "apiKey").append("in", "header").append("name", "X-API-Key"));
		swagger.put("securityDefinitions", sec);
		var out = BasicOpenApiProviderSession.transform(swagger);
		var components = (Map<?,?>) out.get("components");
		var ss = (Map<?,?>) components.get("securitySchemes");
		assertTrue(ss.containsKey("apiKey"));
	}

	@Test void a08_transform_emptyDefinitionsAndSecurityDefs_omitsComponents() {
		var swagger = new Json5Map();
		swagger.put("definitions", new Json5Map());
		swagger.put("securityDefinitions", new Json5Map());
		var out = BasicOpenApiProviderSession.transform(swagger);
		assertFalse(out.containsKey("components"));
	}

	@Test void a09_transform_pathItemNonMapEntriesSkipped() {
		var swagger = new Json5Map();
		var paths = new Json5Map();
		paths.put("/foo", "not-a-map");
		swagger.put("paths", paths);
		var out = BasicOpenApiProviderSession.transform(swagger);
		// Paths produced but the bogus entry is skipped — paths block exists, but /foo is dropped.
		var newPaths = (Map<?,?>) out.get("paths");
		assertNotNull(newPaths);
		assertFalse(newPaths.containsKey("/foo"));
	}

	@Test void a10_transform_operationNonMapEntriesSkipped() {
		var swagger = new Json5Map();
		var paths = new Json5Map();
		var pathItem = new Json5Map();
		pathItem.put("get", "not-a-map");
		paths.put("/foo", pathItem);
		swagger.put("paths", paths);
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newPaths = (Map<?,?>) out.get("paths");
		var newPath = (Map<?,?>) newPaths.get("/foo");
		assertTrue(newPath.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// transform — body parameter → requestBody (in:body branch).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_bodyParam_becomesRequestBody_withTopLevelConsumes() {
		var swagger = new Json5Map();
		swagger.put("consumes", List.of("application/xml"));
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "body").append("required", true).append("description", "the body").append("schema", new Json5Map().append("type", "object"))
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		var rb = (Map<?,?>) newOp.get("requestBody");
		assertEquals("the body", rb.get("description"));
		assertEquals(Boolean.TRUE, rb.get("required"));
		var content = (Map<?,?>) rb.get("content");
		assertTrue(content.containsKey("application/xml"));
		assertFalse(newOp.containsKey("parameters"));  // body removed.
	}

	@Test void b02_bodyParam_defaultsToJson_whenNoConsumes() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "body").append("schema", new Json5Map().append("type", "string"))
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		var rb = (Map<?,?>) newOp.get("requestBody");
		var content = (Map<?,?>) rb.get("content");
		assertTrue(content.containsKey("application/json"));
	}

	@Test void b03_bodyParam_withTypeButNoSchema_extractsInlineSchema() {
		// in:body parameter with type=string at the parameter level (no nested schema).
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "body").append("type", "string").append("format", "byte")
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		var rb = (Map<?,?>) newOp.get("requestBody");
		var media = (Map<?,?>) ((Map<?,?>)rb.get("content")).get("application/json");
		var schema = (Map<?,?>) media.get("schema");
		assertEquals("string", schema.get("type"));
		assertEquals("byte", schema.get("format"));
	}

	@Test void b04_bodyParam_opLevelConsumesOverridesTopLevel() {
		var swagger = new Json5Map();
		swagger.put("consumes", List.of("application/xml"));
		var op = new Json5Map();
		op.put("consumes", List.of("text/plain"));
		op.put("parameters", List.of(
			new Json5Map().append("in", "body").append("schema", new Json5Map().append("type", "string"))
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		var content = (Map<?,?>) ((Map<?,?>)newOp.get("requestBody")).get("content");
		assertTrue(content.containsKey("text/plain"));
		assertFalse(content.containsKey("application/xml"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// transform — formData parameters synthesize a multipart-form requestBody.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_formData_synthesizesUrlencodedRequestBody() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "formData").append("name", "f1").append("type", "string").append("required", true),
			new Json5Map().append("in", "formData").append("name", "f2").append("type", "integer")
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		var rb = (Map<?,?>) newOp.get("requestBody");
		var content = (Map<?,?>) rb.get("content");
		assertTrue(content.containsKey("application/x-www-form-urlencoded"));
		var schema = (Map<?,?>) ((Map<?,?>)content.get("application/x-www-form-urlencoded")).get("schema");
		assertEquals("object", schema.get("type"));
		var props = (Map<?,?>) schema.get("properties");
		assertTrue(props.containsKey("f1"));
		assertTrue(props.containsKey("f2"));
		// f1 is required, f2 is not.
		var required = (List<?>) schema.get("required");
		assertEquals(1, required.size());
		assertEquals("f1", required.get(0));
	}

	@Test void c02_formData_skippedWhenNameMissing() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "formData").append("type", "string")  // no name
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		// formSchema was created but no property added; requestBody still emitted.
		var rb = (Map<?,?>) newOp.get("requestBody");
		var content = (Map<?,?>) rb.get("content");
		var schema = (Map<?,?>) ((Map<?,?>)content.get("application/x-www-form-urlencoded")).get("schema");
		var props = (Map<?,?>) schema.get("properties");
		assertTrue(props.isEmpty());
	}

	@Test void c03_bodyAndFormData_bodyWins() {
		// When both in:body and in:formData params are present, the body parameter wins.
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "formData").append("name", "f1").append("type", "string"),
			new Json5Map().append("in", "body").append("schema", new Json5Map().append("type", "object"))
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("post", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("post");
		var content = (Map<?,?>) ((Map<?,?>)newOp.get("requestBody")).get("content");
		// body's content type used, not formData's urlencoded content type.
		assertTrue(content.containsKey("application/json"));
		assertFalse(content.containsKey("application/x-www-form-urlencoded"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// transform — query/header/path parameters get a schema slot extracted.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_queryParam_extractsSchemaSlot() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "query").append("name", "q").append("type", "string").append("format", "uuid").append("collectionFormat", "csv")
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var params = (List<?>) newOp.get("parameters");
		var p = (Map<?,?>) params.get(0);
		assertEquals("query", p.get("in"));
		// type/format moved into schema; collectionFormat dropped.
		assertFalse(p.containsKey("type"));
		assertFalse(p.containsKey("format"));
		assertFalse(p.containsKey("collectionFormat"));
		var schema = (Map<?,?>) p.get("schema");
		assertEquals("string", schema.get("type"));
		assertEquals("uuid", schema.get("format"));
		assertFalse(schema.containsKey("collectionFormat"));
	}

	@Test void d02_queryParam_existingSchemaMergedWith_putIfAbsent() {
		// When the parameter already has a schema map, extracted slots are merged with putIfAbsent semantics.
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map()
				.append("in", "query").append("name", "q")
				.append("type", "string")  // would be added to schema
				.append("schema", new Json5Map().append("type", "integer"))  // existing schema wins
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var params = (List<?>) newOp.get("parameters");
		var p = (Map<?,?>) params.get(0);
		var schema = (Map<?,?>) p.get("schema");
		// Existing 'integer' is preserved (putIfAbsent — old wins).
		assertEquals("integer", schema.get("type"));
	}

	@Test void d03_queryParam_emptySchemaIsNotAttached() {
		// If no PARAMETER_SCHEMA_KEYS appear, schema slot should NOT be inserted.
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("parameters", List.of(
			new Json5Map().append("in", "query").append("name", "q").append("description", "d")
		));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var params = (List<?>) newOp.get("parameters");
		var p = (Map<?,?>) params.get(0);
		assertFalse(p.containsKey("schema"));
		assertEquals("d", p.get("description"));
	}

	@Test void d04_paramListNonMapItemsSkipped() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		var params = new ArrayList<Object>();
		params.add("not-a-map");
		params.add(new Json5Map().append("in", "query").append("name", "q").append("type", "string"));
		op.put("parameters", params);
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var newParams = (List<?>) newOp.get("parameters");
		assertEquals(1, newParams.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// transform — responses produce a content block keyed by the producer media types.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_response_schemaMovedToContentBlock() {
		var swagger = new Json5Map();
		swagger.put("produces", List.of("application/json", "application/xml"));
		var op = new Json5Map();
		op.put("responses", new Json5Map().append("200",
			new Json5Map().append("description", "OK").append("schema", new Json5Map().append("type", "string"))));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var r = (Map<?,?>) ((Map<?,?>)newOp.get("responses")).get("200");
		assertEquals("OK", r.get("description"));
		assertFalse(r.containsKey("schema"));
		var content = (Map<?,?>) r.get("content");
		assertTrue(content.containsKey("application/json"));
		assertTrue(content.containsKey("application/xml"));
	}

	@Test void e02_response_examplesWithoutSchema_stillEmitted() {
		var swagger = new Json5Map();
		swagger.put("produces", List.of("application/json"));
		var op = new Json5Map();
		op.put("responses", new Json5Map().append("200",
			new Json5Map().append("description", "OK").append("examples", new Json5Map().append("application/json", "{a:1}"))));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var r = (Map<?,?>) ((Map<?,?>)newOp.get("responses")).get("200");
		assertFalse(r.containsKey("examples"));
		var content = (Map<?,?>) r.get("content");
		var media = (Map<?,?>) content.get("application/json");
		assertEquals("{a:1}", media.get("example"));
	}

	@Test void e03_response_examplesWithoutSchema_omittedWhenNoMatchingMediaKey() {
		// produces=[json] but examples key is xml — entry is empty so content is omitted.
		var swagger = new Json5Map();
		swagger.put("produces", List.of("application/json"));
		var op = new Json5Map();
		op.put("responses", new Json5Map().append("200",
			new Json5Map().append("description", "OK").append("examples", new Json5Map().append("application/xml", "<a/>"))));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var r = (Map<?,?>) ((Map<?,?>)newOp.get("responses")).get("200");
		assertFalse(r.containsKey("content"));
	}

	@Test void e04_response_nonMapEntryPreservedAsIs() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("responses", new Json5Map().append("default", "this-is-a-string"));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var responses = (Map<?,?>) newOp.get("responses");
		assertEquals("this-is-a-string", responses.get("default"));
	}

	@Test void e05_response_defaultMediaTypeWhenNoProduces() {
		var swagger = new Json5Map();
		var op = new Json5Map();
		op.put("responses", new Json5Map().append("200",
			new Json5Map().append("schema", new Json5Map().append("type", "string"))));
		swagger.put("paths", new Json5Map().append("/p", new Json5Map().append("get", op)));
		var out = BasicOpenApiProviderSession.transform(swagger);
		var newOp = (Map<?,?>) ((Map<?,?>) ((Map<?,?>)out.get("paths")).get("/p")).get("get");
		var r = (Map<?,?>) ((Map<?,?>)newOp.get("responses")).get("200");
		var content = (Map<?,?>) r.get("content");
		assertTrue(content.containsKey("application/json"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// rewriteRefs — definitions/* refs become components/schemas/* refs (recursive).
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_rewriteRefs_topLevelMapRef() {
		var in = new Json5Map().append("$ref", "#/definitions/Foo");
		var out = BasicOpenApiProviderSession.rewriteRefs(in);
		assertEquals("#/components/schemas/Foo", ((Map<?,?>)out).get("$ref"));
	}

	@Test void f02_rewriteRefs_unrelatedRefsLeftAlone() {
		var in = new Json5Map().append("$ref", "http://elsewhere/x");
		var out = BasicOpenApiProviderSession.rewriteRefs(in);
		assertEquals("http://elsewhere/x", ((Map<?,?>)out).get("$ref"));
	}

	@Test void f03_rewriteRefs_recurseIntoLists() {
		var inner = new Json5Map().append("$ref", "#/definitions/A");
		var list = List.of(inner);
		var out = BasicOpenApiProviderSession.rewriteRefs(list);
		assertEquals("#/components/schemas/A", ((Map<?,?>)((List<?>)out).get(0)).get("$ref"));
	}

	@Test void f04_rewriteRefs_nullPassthrough() {
		assertNull(BasicOpenApiProviderSession.rewriteRefs(null));
	}

	@Test void f05_rewriteRefs_scalarPassthrough() {
		assertEquals("hi", BasicOpenApiProviderSession.rewriteRefs("hi"));
		assertEquals(42, BasicOpenApiProviderSession.rewriteRefs(42));
	}

	//------------------------------------------------------------------------------------------------------------------
	// deduplicateInlineSchemas — name collision avoidance and title-vs-counter selection.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_dedup_titleAlreadyTaken_fallsBackToCounter() {
		// Two operations carrying the same titled schema, but components.schemas already has a "Greeting" entry.
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var components = new Json5Map();
		var schemas = new Json5Map();
		schemas.put("Greeting", new Json5Map().append("type", "string"));  // pre-existing
		components.put("schemas", schemas);
		doc.put("components", components);
		var paths = new Json5Map();
		paths.put("/a", makePath(makeInlineResponse("Greeting")));
		paths.put("/b", makePath(makeInlineResponse("Greeting")));
		doc.put("paths", paths);

		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		var newSchemas = (Map<?,?>) ((Map<?,?>) out.get("components")).get("schemas");
		// Existing Greeting kept untouched; new entry uses Schema1 fallback.
		assertTrue(newSchemas.containsKey("Greeting"));
		assertTrue(newSchemas.containsKey("Schema1"), () -> "Expected fallback Schema1 — got: " + newSchemas.keySet());
	}

	@Test void g02_dedup_blankTitle_usesSchemaCounter() {
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		paths.put("/a", makePath(makeInlineResponse("   ")));  // blank title
		paths.put("/b", makePath(makeInlineResponse("   ")));
		doc.put("paths", paths);
		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		var schemas = (Map<?,?>) ((Map<?,?>) out.get("components")).get("schemas");
		assertTrue(schemas.containsKey("Schema1"));
	}

	@Test void g03_dedup_skipsRefedSchemas() {
		// Schemas already carrying a $ref shouldn't be hoisted again.
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		paths.put("/a", makePath(makeRefedResponse()));
		paths.put("/b", makePath(makeRefedResponse()));
		doc.put("paths", paths);
		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		// No components block added.
		assertFalse(out.containsKey("components"));
	}

	@Test void g04_dedup_skipsEmptySchemas() {
		// Inline empty schema should not be hoisted.
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		paths.put("/a", makePath(makeEmptyResponse()));
		paths.put("/b", makePath(makeEmptyResponse()));
		doc.put("paths", paths);
		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		assertFalse(out.containsKey("components"));
	}

	@Test void g05_dedup_walksParameters_andRequestBody() {
		// Same inline schema in two different operation slots:
		//   /a: parameters[0].schema
		//   /b: requestBody.content[json].schema
		var inline = new Json5Map().append("type", "object").append("properties",
			new Json5Map().append("name", new Json5Map().append("type", "string")));

		var opA = new Json5Map();
		opA.put("parameters", List.of(new Json5Map().append("in", "query").append("name", "q").append("schema", new Json5Map(inline))));

		var opB = new Json5Map();
		var rb = new Json5Map();
		var content = new Json5Map();
		var media = new Json5Map().append("schema", new Json5Map(inline));
		content.put("application/json", media);
		rb.put("content", content);
		opB.put("requestBody", rb);

		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		doc.put("paths", new Json5Map().append("/a", new Json5Map().append("get", opA)).append("/b", new Json5Map().append("post", opB)));

		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		var schemas = (Map<?,?>) ((Map<?,?>) out.get("components")).get("schemas");
		assertEquals(1, schemas.size());
		// Parameter schema and requestBody schema both rewritten.
		var paramSchema = (Map<?,?>) ((Map<?,?>)((List<?>)((Map<?,?>)((Map<?,?>)((Map<?,?>)out.get("paths")).get("/a")).get("get")).get("parameters")).get(0)).get("schema");
		assertTrue(((String)paramSchema.get("$ref")).startsWith("#/components/schemas/"));
		var rbSchema = (Map<?,?>) ((Map<?,?>)((Map<?,?>)((Map<?,?>)((Map<?,?>)((Map<?,?>)((Map<?,?>)out.get("paths")).get("/b")).get("post")).get("requestBody")).get("content")).get("application/json")).get("schema");
		assertTrue(((String)rbSchema.get("$ref")).startsWith("#/components/schemas/"));
	}

	@Test void g06_dedup_pathsNotAMap_returnsDocUnchanged() {
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		doc.put("paths", "not-a-map");
		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		assertSame(doc, out);
	}

	@Test void g07_dedup_nonMapPathItemAndOpsSkipped() {
		var doc = new Json5Map();
		doc.put("openapi", "3.1.0");
		var paths = new Json5Map();
		paths.put("/a", "not-a-path-item");
		var pathB = new Json5Map();
		pathB.put("get", "not-an-op");
		paths.put("/b", pathB);
		doc.put("paths", paths);
		var out = BasicOpenApiProviderSession.deduplicateInlineSchemas(doc);
		assertSame(doc, out);
	}

	//------------------------------------------------------------------------------------------------------------------
	// End-to-end (RestContext) — verifies the full Swagger → OpenAPI pipeline runs against a real resource.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H1 {
		@RestGet(path="/hello")
		public X a() { return null; }
	}

	@Test void h01_endToEnd_minimalGetProducesOpenApi310() throws Exception {
		var doc = getOpenApi(new H1());
		assertEquals("3.1.0", doc.getOpenapi());
		assertNotNull(doc.getPaths());
		assertNotNull(doc.getPaths().get("/hello").getGet());
	}

	@Rest
	public static class H2 {
		@RestPost(path="/body")
		public X a(@Content X body) { return null; }
	}

	@Test void h02_endToEnd_bodyParamBecomesRequestBody() throws Exception {
		var doc = getOpenApi(new H2());
		var op = doc.getPaths().get("/body").getPost();
		assertNotNull(op.getRequestBody(), "Expected requestBody synthesized from in:body parameter");
		assertNotNull(op.getRequestBody().getContent());
	}

	@Rest
	public static class H3 {
		@RestPost(path="/form")
		public X a(@FormData("field1") String field1, @FormData("field2") @Schema(required=true) String field2) {
			return null;
		}
	}

	@Test void h03_endToEnd_formDataBecomesUrlencodedRequestBody() throws Exception {
		var doc = getOpenApi(new H3());
		var op = doc.getPaths().get("/form").getPost();
		assertNotNull(op.getRequestBody());
		var content = op.getRequestBody().getContent();
		assertTrue(content.containsKey("application/x-www-form-urlencoded"),
			() -> "Expected urlencoded media type — got: " + content.keySet());
	}

	@Rest
	public static class H4 {
		@RestGet(path="/q")
		public X a(@Query("q") @Schema(type="string",format="uuid") String q) { return null; }
	}

	@Test void h04_endToEnd_queryParamHasSchemaSlot() throws Exception {
		var doc = getOpenApi(new H4());
		var op = doc.getPaths().get("/q").getGet();
		var params = op.getParameters();
		assertNotNull(params);
		assertEquals(1, params.size());
		// Schema slot should be present (with type=string, format=uuid lifted into it).
		assertNotNull(params.get(0).getSchema());
	}

	@Rest
	public static class H5 {
		@RestGet(path="/pet")
		public X getPet() { return null; }

		@RestPost(path="/pet")
		public X createPet(@Content X pet) { return null; }
	}

	@Test void h05_endToEnd_definitionsLiftedToComponentsSchemas() throws Exception {
		var doc = getOpenApi(new H5());
		assertNotNull(doc.getComponents(), "Expected components block when definitions are present");
		var schemas = doc.getComponents().getSchemas();
		assertNotNull(schemas, "Expected components.schemas populated");
		assertFalse(schemas.isEmpty(), "Expected at least one schema in components.schemas — actual: " + schemas);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers for hand-built Json5Map docs.
	//------------------------------------------------------------------------------------------------------------------

	private static Json5Map makePath(Json5Map opMap) {
		var path = new Json5Map();
		path.put("get", opMap);
		return path;
	}

	private static Json5Map makeInlineResponse(String title) {
		var op = new Json5Map();
		var responses = new Json5Map();
		var r200 = new Json5Map();
		r200.put("description", "OK");
		var content = new Json5Map();
		var media = new Json5Map();
		var schema = new Json5Map();
		schema.put("type", "object");
		if (title != null)
			schema.put("title", title);
		media.put("schema", schema);
		content.put("application/json", media);
		r200.put("content", content);
		responses.put("200", r200);
		op.put("responses", responses);
		return op;
	}

	private static Json5Map makeRefedResponse() {
		var op = new Json5Map();
		var responses = new Json5Map();
		var r200 = new Json5Map();
		var content = new Json5Map();
		var media = new Json5Map();
		media.put("schema", new Json5Map().append("$ref", "#/components/schemas/Foo"));
		content.put("application/json", media);
		r200.put("content", content);
		responses.put("200", r200);
		op.put("responses", responses);
		return op;
	}

	private static Json5Map makeEmptyResponse() {
		var op = new Json5Map();
		var responses = new Json5Map();
		var r200 = new Json5Map();
		var content = new Json5Map();
		var media = new Json5Map();
		media.put("schema", new Json5Map());  // empty schema
		content.put("application/json", media);
		r200.put("content", content);
		responses.put("200", r200);
		op.put("responses", responses);
		return op;
	}
}
