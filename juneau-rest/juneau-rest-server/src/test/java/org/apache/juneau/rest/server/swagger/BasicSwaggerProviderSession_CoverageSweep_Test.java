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
package org.apache.juneau.rest.server.swagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.ExternalDocs;
import org.apache.juneau.commons.Items;
import org.apache.juneau.commons.SubItems;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Tag;
import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link BasicSwaggerProviderSession}, exercised only through its sole production
 * entry point, {@link RestContext#getSwagger(Locale)}, against fixture {@code @Rest}-annotated resources with
 * richly-populated swagger annotations ({@code @Schema}/{@code @Items}/{@code @SubItems}/{@code @ExternalDocs}/
 * {@code @Response}/{@code @Header}/{@code @OpSwagger}) that exercise merge branches otherwise left untested.
 */
class BasicSwaggerProviderSession_CoverageSweep_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	// Injects a JsonSchemaGenerator with useBeanDefs() enabled -- BasicSwaggerProviderSession.resolveRef() only
	// evaluates its ref.startsWith("#/definitions/") branch when js.getBeanDefs() is non-null, which requires
	// useBeanDefs() (opt-in, off by default on JsonSchemaGenerator.DEFAULT).
	static RestContext.Args argsOfWithBeanDefs(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null,
			bs -> bs.addBean(org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.class, org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.create().useBeanDefs().build()),
			null, null, null, null);
	}

	//-----------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------

	// The class-level @Schema (on Fix_Ex itself) and the method-level @Schema (on getErr()) exercise the
	// "rstream(ap.find(Schema.class, eci))" (BasicSwaggerProviderSession L541, exception-class schema merge) and
	// "rstream(ap.find(Schema.class, ecmi))" (L556, exception-header schema merge) forEach-lambda bodies
	// respectively -- both previously always empty/no-op since neither carried a @Schema annotation.
	@Response
	@Schema(format = "ex-fmt")
	static class Fix_Ex extends Exception {
		private static final long serialVersionUID = 1L;
		@Header(name = "X-Err")
		@Schema(type = "string")
		public String getErr() { return "e"; }
		// @Header("*") (multi/catch-all, via the value() shorthand) exercises isMulti()'s value=="*" branch and
		// the "skip -- multi headers aren't individually documented" arm at the exception-header call site.
		@Header("*")
		public Map<String,Object> getExtra() { return Collections.emptyMap(); }
	}

	// A second, independent @StatusCode(400)-annotated exception (kept separate from Fix_Ex so as not to disturb
	// its existing default-500 assertions in a03) -- exercises getCodes()'s codes.isEmpty()==false branch.
	@Response
	@StatusCode(400)
	static class Fix_ExWithCode extends Exception {
		private static final long serialVersionUID = 1L;
	}

	@Response(headers = @Header(name = "X-Bar", schema = @Schema(type = "string")))
	public interface Fix_ResponseIfc {
		// The method-level @Schema on getBar() (distinct from the interface-level @Response(headers=@Header(
		// schema=...)) above) exercises the "rstream(ap.find(Schema.class, ecmi))" forEach-lambda body at
		// BasicSwaggerProviderSession L590 -- previously always empty/no-op since no return-type-@Response
		// header method carried a directly-annotated @Schema.
		@Header(name = "X-Bar")
		@Schema(format = "bar-fmt")
		String getBar();
		// @Header(name="*") exercises isMulti()'s name=="*" branch and the "skip" arm at the
		// return-type-@Response header call site (as opposed to Fix_Ex's value=="*" variant above).
		@Header(name = "*")
		Map<String,Object> getExtra();
	}

	// Return type for opExampleEnum() below -- Enum.valueOf() gives this a string mutater, unlike String itself
	// (ClassMeta.hasStringMutater() is unconditionally false for String -- see opExamplePlain()).
	public enum Fix_ExampleEnum { A, B }

	@Rest(
		// serializers=JsonSerializer.class is needed so addBodyExamples() has a non-empty, non-HTML serializer
		// to exercise -- getSerializers() otherwise starts from an empty set (see RestContext.serializersBuilder),
		// which would make its media-types loop a no-op and leave the "examples" map perpetually empty.
		serializers = JsonSerializer.class,
		swagger = @org.apache.juneau.rest.server.Swagger(
			value = "consumes:['text/plain'],produces:['text/plain']",
			tags = @Tag(name = "pet", description = "Pets")
		)
	)
	public static class Fix_Rich {

		/**
		 * Creates a pet.
		 *
		 * @param body The pet body.
		 * @return The pet body, echoed back.
		 * @throws Fix_Ex Never actually thrown; declared solely so {@code mi.getExceptionTypes()} in
		 * 	{@link BasicSwaggerProviderSession} picks it up to exercise its exception-response-annotation-merge branches.
		 */
		@RestPost(path = "/pets", swagger = @OpSwagger(tags = "widget", deprecated = "true"))
		public String createPet(
			@Content
			@Schema(
				type = "object",
				format = "date-time",
				readOnly = true,
				required = true,
				uniqueItems = true,
				ignore = false,
				title = "T",
				summary = "S",
				maxProperties = 5,
				minProperties = 1,
				enum_ = {"A", "B"},
				externalDocs = @ExternalDocs(description = "ed", url = "http://example.com"),
				items = @Items(
					type = "string",
					exclusiveMaximum = true,
					exclusiveMinimum = true,
					uniqueItems = true,
					maxItems = 5,
					minItems = 1,
					items = @SubItems(
						type = "integer",
						exclusiveMaximum = true,
						exclusiveMinimum = true,
						uniqueItems = true
					)
				)
			)
			String body
		) throws Fix_Ex {
			return body;
		}

		// tags="pet" duplicates the class-level "pet" tag already registered in tagMap -- exercises the
		// tagMap.containsKey(tag)==true arm (as opposed to createPet()'s "widget" tag, which backfills a new entry).
		@RestGet(path = "/response", swagger = @OpSwagger(tags = "pet"))
		public Fix_ResponseIfc opResponse() {
			return null;
		}

		/**
		 * Op with an exception-annotation-only declared exception.
		 *
		 * @return A constant string.
		 * @throws Fix_ExWithCode Never actually thrown; declared solely so {@code mi.getExceptionTypes()} in
		 * 	{@link BasicSwaggerProviderSession} picks it up to exercise the explicit-{@code @StatusCode} branch of
		 * 	{@code getCodes()}.
		 */
		@RestGet(path = "/exwithcode")
		public String opExWithCode() throws Fix_ExWithCode {
			return "x";
		}

		// @Header("*") on a Holder<String> out-param exercises the method-parameter isMulti() call site (as
		// opposed to Fix_Ex's exception-header and Fix_ResponseIfc's return-type-header call sites above).
		@RestGet(path = "/multi")
		public String opMulti(@Header("*") Holder<String> hOut) {
			hOut.set("v");
			return "ok";
		}

		// Method-level @Response (rather than @Response on the return type, as opResponse() above uses) exercises
		// the other half of the "mi.hasAnnotation(Response) || returnType.hasAnnotation(Response)" OR, and --
		// since the return type here is unannotated -- routes into the default-200/addBodyExamples() branch that
		// opResponse() (whose return type carries @Response) never reaches. The raw response "example" (set
		// directly via @OpSwagger(value), bypassing schema/type resolution) is a double-quoted JSON string
		// literal -- exercises addBodyExamples()'s isProbablyJson()==true (jp.read) branch.
		// The method-level @Schema exercises the "rstream(ap.find(Schema.class, mi))" forEach-lambda body at
		// BasicSwaggerProviderSession L574 (the method-level-@Response counterpart of a06's shorthand-@Schema
		// coverage on a @Query param) -- previously always empty/no-op since no method-level-@Response method
		// carried a directly-annotated @Schema.
		@Response
		@Schema(format = "json-fmt")
		@RestGet(path = "/example/json", swagger = @OpSwagger(value = "responses:{'200':{example:'\"hi\"'}}"))
		public String opExampleJson() {
			return "hi";
		}

		// Same shape as opExampleJson() above, but the return type (Fix_ExampleEnum) has a string mutater
		// (Enum.valueOf) and the example is plain (non-JSON) text -- exercises isProbablyJson()==false /
		// hasStringMutater()==true.
		@Response
		@RestGet(path = "/example/enum", swagger = @OpSwagger(value = "responses:{'200':{example:'A'}}"))
		public Fix_ExampleEnum opExampleEnum() {
			return Fix_ExampleEnum.A;
		}

		// Same shape again, but the return type is plain String -- ClassMeta.hasStringMutater() is unconditionally
		// false for String, so this exercises isProbablyJson()==false / hasStringMutater()==false (example
		// resolves to null, but addBodyExamples() must still complete without error).
		@Response
		@RestGet(path = "/example/plain", swagger = @OpSwagger(value = "responses:{'200':{example:'plain text'}}"))
		public String opExamplePlain() {
			return "plain text";
		}

		// @Schema(format=...) on the @Path/@Header/@FormData params (in addition to the pre-existing @Query "q")
		// exercises the "rstream(ap.find(Schema.class, mpi)).forEach(...)" merge-lambda body at each of those
		// three call sites (BasicSwaggerProviderSession L502/L510/L518) -- previously always empty/no-op here
		// since none of these params carried a @Schema annotation.
		@RestGet(path = "/examples/{id}", swagger = @OpSwagger(parameters = {
			"{in:'path', name:'id', example:'42'},",
			"{in:'header', name:'X-Foo', example:'abc', examples:{'application/json':'seed'}}"
		}))
		@SuppressWarnings({
			"unused" // xFoo/q/f are unused in the body; their @Header/@Query/@FormData/@Schema annotations are what BasicSwaggerProviderSession's parameter-merge branches exercise.
		})
		public String opExamples(
			@Path("id") @Schema(format = "path-fmt") String id,
			@Header("X-Foo") @Schema(format = "header-fmt") String xFoo,
			@Query("q") String q,
			@FormData("f") @Schema(format = "form-fmt") String f
		) {
			return id;
		}

		// Uses the shorthand @Schema/@Items/@SubItems boolean attributes (ro/r/ui/emax/emin) instead of their
		// long-form equivalents used by createPet()'s body param, to exercise the other half of each
		// long-form-OR-shorthand branch (e.g. `a.readOnly() || a.ro()`) plus the ignore=true ternary branch.
		// @Deprecated (the method-level arm, as opposed to a class-level @Deprecated) exercises the
		// "deprecated" ternary's nn(m.getAnnotation(Deprecated.class)) true branch.
		@Deprecated
		@RestGet(path = "/shorthand")
		public String opShorthand(
			@Query("q")
			@Schema(
				ro = true,
				r = true,
				ui = true,
				ignore = true,
				items = @Items(
					emax = true,
					emin = true,
					ui = true,
					items = @SubItems(emax = true, emin = true, ui = true)
				)
			)
			String q
		) {
			return q;
		}
	}

	// Messages/tags on class %s (BasicSwaggerProviderSession L345-355): a resource-bundle "tags" entry sharing a
	// name ("msgtag") with a class-level @Tag exercises the tagMap.containsKey(name)==true merge arm, while the
	// "msgtag2" entry (a name not already in tagMap) exercises the containsKey==false put arm.
	// See src/test/resources/.../Fix_MsgTags.properties.
	@Rest(
		swagger = @org.apache.juneau.rest.server.Swagger(tags = @Tag(name = "msgtag", description = "original"))
	)
	public static class Fix_MsgTags {
		@RestGet(path = "/x")
		public String x() { return "x"; }
	}

	// A resource-bundle "tags" entry with no "name" field -- exercises the (name == null) throw arm at
	// BasicSwaggerProviderSession L349-350 (the resource-bundle counterpart of the class-annotation-tags throw at
	// L339-340, which is already covered elsewhere). See src/test/resources/.../Fix_MsgTagsBadName.properties.
	@Rest
	public static class Fix_MsgTagsBadName {
		@RestGet(path = "/x")
		public String x() { return "x"; }
	}

	// A bean-typed (rather than String-typed) query param -- the JSON-schema-generator-produced base schema for
	// a bean is {type:"object", properties:{...}}; pushupSchemaFields() hoists "type" onto the parameter (making
	// param.type=="object") but has no "properties" case in its known-attribute strip list, leaving a non-empty
	// residual `schema` map -- exercises the SWAGGER_object.equals(...) && !schema.isEmpty() true arm
	// (BasicSwaggerProviderSession L1128), which pushes that leftover schema back as a nested "schema" sub-object.
	public static class Fix_QueryBean {
		public String value;
	}

	@Rest
	public static class Fix_ObjectQuery {
		@RestGet(path = "/x")
		public String x(@Query("q") Fix_QueryBean q) {
			return q.value;
		}
	}

	// A response schema whose $ref does NOT start with "#/definitions/" -- exercises resolveRef()'s
	// ref.startsWith("#/definitions/")==false arm (BasicSwaggerProviderSession L1193). Requires
	// argsOfWithBeanDefs() (see above) since resolveRef() only inspects the ref at all when useBeanDefs() is on.
	@Schema($ref = "https://example.com/schemas/Foo.json")
	public static class Fix_RefBean {}

	@Rest
	public static class Fix_Ref {
		@Response
		@RestGet(path = "/x")
		public Fix_RefBean x() { return new Fix_RefBean(); }
	}

	@Rest
	public static class Fix_BadHeader {
		@Response(headers = @Header)
		public interface BadResponse {}

		@RestGet(path = "/bad")
		public BadResponse opBad() {
			return null;
		}
	}

	//-----------------------------------------------------------------------------------------------------------
	// Tests
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_richFixture_classLevelConsumesProducesTags() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);
		assertNotNull(swagger);

		assertTrue(swagger.getConsumes().stream().anyMatch(x -> x.toString().contains("text/plain")));
		assertTrue(swagger.getProduces().stream().anyMatch(x -> x.toString().contains("text/plain")));
		assertTrue(swagger.getTags().stream().anyMatch(t -> "pet".equals(t.getName())));
		// "widget" is only referenced at the op level (@OpSwagger(tags="widget")) and isn't in the
		// class-level tag list -- this exercises the tagMap-backfill branch for op-only tags.
		assertTrue(swagger.getTags().stream().anyMatch(t -> "widget".equals(t.getName())));
	}

	@Test void a02_richFixture_bodySchemaMergesAllAnnotationAttributes() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var postOp = swagger.getPaths().values().stream()
			.map(m -> m.get("post"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(postOp);
		assertEquals(Boolean.TRUE, postOp.getDeprecated());

		var bodyParam = postOp.getParameters().stream().filter(p -> "body".equals(p.getIn())).findFirst().orElse(null);
		assertNotNull(bodyParam);
		var schema = bodyParam.getSchema();
		assertNotNull(schema);
		assertEquals(Boolean.TRUE, schema.getReadOnly());
		assertEquals(Boolean.TRUE, schema.getUniqueItems());
		assertNotNull(schema.getItems());
		assertEquals(Boolean.TRUE, schema.getItems().getExclusiveMaximum());
		assertEquals(Boolean.TRUE, schema.getItems().getExclusiveMinimum());
		assertNotNull(schema.getExternalDocs());
	}

	@Test void a03_richFixture_exceptionResponseAndHeaderAreMerged() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var postOp = swagger.getPaths().values().stream()
			.map(m -> m.get("post"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(postOp);

		var responses = postOp.getResponses();
		assertNotNull(responses);
		assertTrue(responses.containsKey("500"));
		var errResponse = responses.get("500");
		assertNotNull(errResponse.getHeaders());
		assertTrue(errResponse.getHeaders().containsKey("X-Err"));
	}

	@Test void a04_richFixture_returnTypeResponseAnnotationMergesHeader() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var getOpWithHeader = swagger.getPaths().values().stream()
			.map(m -> m.get("get"))
			.filter(Objects::nonNull)
			.filter(o -> o.getResponses() != null && o.getResponses().values().stream()
				.anyMatch(r -> r.getHeaders() != null && r.getHeaders().containsKey("X-Bar")))
			.findFirst().orElse(null);
		assertNotNull(getOpWithHeader);
	}

	@Test void a05_richFixture_pathAndHeaderParamExamplesAreResolved() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var getOpWithPathParam = swagger.getPaths().values().stream()
			.map(m -> m.get("get"))
			.filter(Objects::nonNull)
			.filter(o -> o.getParameters() != null && o.getParameters().stream().anyMatch(p -> "id".equals(p.getName())))
			.findFirst().orElse(null);
		assertNotNull(getOpWithPathParam);

		var idParam = getOpWithPathParam.getParameters().stream().filter(p -> "id".equals(p.getName())).findFirst().orElse(null);
		assertNotNull(idParam);
		assertEquals(Boolean.TRUE, idParam.getRequired());

		var fooParam = getOpWithPathParam.getParameters().stream().filter(p -> "X-Foo".equals(p.getName())).findFirst().orElse(null);
		assertNotNull(fooParam);
		assertNotNull(fooParam.getExamples());
	}

	@Test void a06_richFixture_shorthandSchemaAttributesAreMerged() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		// Filter by path (not just "has a 'q' query param") since opExamples() above also has one -- distinguishing
		// only by param name would nondeterministically match either operation's "q" parameter.
		var shorthandOp = swagger.getPaths().entrySet().stream()
			.filter(e -> e.getKey().contains("/shorthand"))
			.map(e -> e.getValue().get("get"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(shorthandOp);

		// Query (non-body) parameters carry these fields directly on the Parameter object rather than in a
		// nested "schema" sub-object (that nested form is body/response-only) -- see BasicSwaggerProviderSession
		// lines 491-497 vs. 481-489.
		var qParam = shorthandOp.getParameters().stream().filter(p -> "q".equals(p.getName())).findFirst().orElse(null);
		assertNotNull(qParam);
		assertEquals(Boolean.TRUE, qParam.getRequired());
		assertEquals(Boolean.TRUE, qParam.getUniqueItems());
		assertNotNull(qParam.getItems());
		assertEquals(Boolean.TRUE, qParam.getItems().getExclusiveMaximum());
		assertEquals(Boolean.TRUE, qParam.getItems().getExclusiveMinimum());
		assertEquals(Boolean.TRUE, qParam.getItems().getUniqueItems());
	}

	@Test void a07_headerAnnotationWithoutNameOrValue_throwsIllegalArgumentException() throws Exception {
		var ctx = new RestContext(argsOf(Fix_BadHeader.class, Fix_BadHeader::new));
		// RestContext#getSwagger() wraps any exception thrown while building the swagger doc in an
		// InternalServerError; the underlying IllegalArgumentException from BasicSwaggerProviderSession's
		// "@Header used without name or value" validation is preserved as its cause.
		var ex = assertThrows(org.apache.juneau.http.response.InternalServerError.class, () -> ctx.getSwagger(Locale.ENGLISH));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("@Header used without name or value"));
	}

	@Test void a08_richFixture_multiCatchAllHeadersAreSkippedNotDocumented() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var postOp = swagger.getPaths().values().stream()
			.map(m -> m.get("post")).filter(Objects::nonNull).findFirst().orElse(null);
		assertNotNull(postOp);
		// Fix_Ex.getExtra() is @Header("*") (multi/catch-all) -- isMulti()==true means it's skipped rather than
		// documented as a literal header named "*".
		var errHeaders = postOp.getResponses().get("500").getHeaders();
		assertTrue(errHeaders.containsKey("X-Err"));
		assertFalse(errHeaders.containsKey("*"));

		var getOpWithHeader = swagger.getPaths().values().stream()
			.map(m -> m.get("get"))
			.filter(Objects::nonNull)
			.filter(o -> o.getResponses() != null && o.getResponses().values().stream()
				.anyMatch(r -> r.getHeaders() != null && r.getHeaders().containsKey("X-Bar")))
			.findFirst().orElse(null);
		assertNotNull(getOpWithHeader);
		// Fix_ResponseIfc.getExtra() is @Header(name="*") -- same skip, via the name() (rather than value())
		// shorthand, and via the return-type-@Response header call site (rather than the exception-header one).
		var barHeaders = getOpWithHeader.getResponses().values().iterator().next().getHeaders();
		assertFalse(barHeaders.containsKey("*"));

		var multiOp = swagger.getPaths().entrySet().stream()
			.filter(e -> e.getKey().contains("/multi"))
			.map(e -> e.getValue().get("get"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(multiOp);
		// opMulti(@Header("*") Holder<String>) -- same skip again, via the method-parameter header call site.
		var multiHeaders = multiOp.getResponses().get("200").getHeaders();
		assertTrue(multiHeaders == null || ! multiHeaders.containsKey("*"));
	}

	@Test void a09_richFixture_explicitStatusCodeOverridesDefault() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var op = swagger.getPaths().entrySet().stream()
			.filter(e -> e.getKey().contains("/exwithcode"))
			.map(e -> e.getValue().get("get"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(op);
		// Fix_ExWithCode is @StatusCode(400) -- getCodes() should use the explicit code, not the default 500.
		assertTrue(op.getResponses().containsKey("400"));
		assertFalse(op.getResponses().containsKey("500"));
	}

	@Test void a10_richFixture_methodLevelResponseWithJsonExampleIsSerialized() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var op = swagger.getPaths().entrySet().stream()
			.filter(e -> e.getKey().contains("/example/json"))
			.map(e -> e.getValue().get("get"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(op);
		var resp200 = op.getResponses().get("200");
		assertNotNull(resp200);
		// The raw response example ('"hi"', a double-quoted JSON string literal) parses as JSON
		// (isProbablyJson==true) and gets serialized to at least one non-HTML media type's example string.
		assertNotNull(resp200.getExamples());
		assertFalse(resp200.getExamples().isEmpty());
	}

	@Test void a11_richFixture_methodLevelResponseWithEnumExampleIsSerialized() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var op = swagger.getPaths().entrySet().stream()
			.filter(e -> e.getKey().contains("/example/enum"))
			.map(e -> e.getValue().get("get"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(op);
		var resp200 = op.getResponses().get("200");
		assertNotNull(resp200);
		// The raw response example ("A") is plain text, not JSON, but Fix_ExampleEnum's Enum.valueOf() gives it a
		// string mutater (isProbablyJson==false, hasStringMutater()==true) -- still resolves to a real example.
		assertNotNull(resp200.getExamples());
		assertFalse(resp200.getExamples().isEmpty());
	}

	@Test void a12_richFixture_methodLevelResponseWithUnmutableExampleStillCompletes() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Rich.class, Fix_Rich::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var op = swagger.getPaths().entrySet().stream()
			.filter(e -> e.getKey().contains("/example/plain"))
			.map(e -> e.getValue().get("get"))
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
		assertNotNull(op);
		// The raw response example ("plain text") is neither JSON nor string-mutatable for a String return type
		// (ClassMeta.hasStringMutater() is unconditionally false for String) -- isProbablyJson==false,
		// hasStringMutater()==false -- addBodyExamples() must complete without error.
		assertNotNull(op.getResponses().get("200"));
	}

	@Test void a13_msgTags_resourceBundleTagsAreMergedByName() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MsgTags.class, Fix_MsgTags::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);
		assertNotNull(swagger);

		// "msgtag" exists both as a class-level @Tag and as a resource-bundle tags entry -- the bundle entry's
		// description should have been merged (putAll) into the already-registered tagMap entry.
		var msgtag = swagger.getTags().stream().filter(t -> "msgtag".equals(t.getName())).findFirst().orElse(null);
		assertNotNull(msgtag);
		assertEquals("From bundle override", msgtag.getDescription());

		// "msgtag2" only exists in the resource bundle -- a brand-new tagMap entry.
		assertTrue(swagger.getTags().stream().anyMatch(t -> "msgtag2".equals(t.getName())));
	}

	@Test void a14_msgTagsBadName_throwsSwaggerException() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MsgTagsBadName.class, Fix_MsgTagsBadName::new));
		// Same InternalServerError-wraps-the-real-cause pattern as a07, but for the resource-bundle "tags"
		// name-missing throw (BasicSwaggerProviderSession L349-350) rather than the @Header validation one.
		var ex = assertThrows(org.apache.juneau.http.response.InternalServerError.class, () -> ctx.getSwagger(Locale.ENGLISH));
		assertInstanceOf(SwaggerException.class, ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("Tag definition found without name"));
	}

	@Test void a15_objectQuery_leftoverSchemaIsPushedBackAsNestedSchema() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ObjectQuery.class, Fix_ObjectQuery::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var op = swagger.getPaths().values().stream()
			.map(m -> m.get("get")).filter(Objects::nonNull).findFirst().orElse(null);
		assertNotNull(op);
		var qParam = op.getParameters().stream().filter(p -> "q".equals(p.getName())).findFirst().orElse(null);
		assertNotNull(qParam);
		assertEquals("object", qParam.getType());
		// customX isn't a standard swagger parameter attribute pushupSchemaFields() hoists directly onto the
		// parameter, so it's left in a residual "schema" sub-object once type=="object".
		assertNotNull(qParam.getSchema());
	}

	@Test void a16_refBean_externalRefIsLeftUnresolved() throws Exception {
		var ctx = new RestContext(argsOfWithBeanDefs(Fix_Ref.class, Fix_Ref::new));
		var swagger = ctx.getSwagger(Locale.ENGLISH).orElse(null);

		var op = swagger.getPaths().values().stream()
			.map(m -> m.get("get")).filter(Objects::nonNull).findFirst().orElse(null);
		assertNotNull(op);
		var schema = op.getResponses().get("200").getSchema();
		assertNotNull(schema);
		// resolveRef() only swaps in a bean-def when the ref starts with "#/definitions/" -- an external URL ref
		// is left completely unresolved (returned as-is).
		assertEquals("https://example.com/schemas/Foo.json", schema.getRef());
	}
}
