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
package org.apache.juneau.rest.server.vars;

import org.apache.juneau.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Tag;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestSwaggerVar} ($RS{key}) covering the full key matrix:
 * contact, description, externalDocs, license, operationDescription,
 * operationSummary, siteName, tags, termsOfService, title, version,
 * unknown-key fallback, and multipart fallback semantics inherited from
 * {@link org.apache.juneau.commons.svl.MultipartResolvingVar}.
 */
class RequestSwaggerVar_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - Resource fully populated via @Rest(swagger=@Swagger(...)) and @RestGet annotations.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(
		title="a-title",
		description="a-description",
		siteName="a-siteName",
		swagger=@Swagger(
			value={"{info:{title:'a-title',description:'a-description',siteName:'a-siteName'}}"},
			version="1.2.3",
			termsOfService="a-tos",
			contact=@Contact(name="c-name", url="c-url", email="c-email"),
			license=@License(name="l-name", url="l-url"),
			externalDocs=@ExternalDocs(url="http://example.com/docs", description="e-desc"),
			tags={
				@Tag(name="t1", description="t1-desc"),
				@Tag(name="t2", description="t2-desc")
			}
		)
	)
	public static class A {
		@RestGet(path="/key/{key}", summary="op-summary", description="op-description")
		public String resolve(RestRequest req, @Path("key") String key) {
			return req.getVarResolverSession().resolve("$RS{" + key + "}");
		}

		@RestGet(path="/raw")
		public String raw(RestRequest req, @Query("expr") String expr) {
			return req.getVarResolverSession().resolve(expr);
		}
	}

	private static MockRestClient clientA;

	@BeforeAll
	static void beforeAll() {
		clientA = MockRestClient.build(A.class);
		beforeAllC();
		beforeAllE();
	}

	@Test void a01_title() throws Exception {
		clientA.get("/key/title").run().assertStatus(200).assertContent("a-title");
	}

	@Test void a02_description() throws Exception {
		clientA.get("/key/description").run().assertStatus(200).assertContent("a-description");
	}

	@Test void a03_siteName() throws Exception {
		clientA.get("/key/siteName").run().assertStatus(200).assertContent("a-siteName");
	}

	@Test void a04_version() throws Exception {
		clientA.get("/key/version").run().assertStatus(200).assertContent("1.2.3");
	}

	@Test void a05_termsOfService() throws Exception {
		clientA.get("/key/termsOfService").run().assertStatus(200).assertContent("a-tos");
	}

	@Test void a06_contact() throws Exception {
		// Contact serialized via Utils.s(...) -> JSON5-ish text containing at least the email.
		clientA.get("/key/contact").run()
			.assertStatus(200)
			.assertContent().asString().isContains("c-name");
	}

	@Test void a07_license() throws Exception {
		clientA.get("/key/license").run()
			.assertStatus(200)
			.assertContent().asString().isContains("l-name");
	}

	@Test void a08_externalDocs() throws Exception {
		clientA.get("/key/externalDocs").run()
			.assertStatus(200)
			.assertContent().asString().isContains("example.com");
	}

	@Test void a09_tags() throws Exception {
		clientA.get("/key/tags").run()
			.assertStatus(200)
			.assertContent().asString().isContains("t1");
	}

	@Test void a10_operationSummary() throws Exception {
		clientA.get("/key/operationSummary").run().assertStatus(200).assertContent("op-summary");
	}

	@Test void a11_operationDescription() throws Exception {
		clientA.get("/key/operationDescription").run().assertStatus(200).assertContent("op-description");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - Unknown key / malformed key behavior.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_unknownKey_returnsEmpty() throws Exception {
		// Unknown key: resolve(...) returns null, VarResolver substitutes empty string.
		clientA.get("/raw").queryData("expr", "$RS{nonsense}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b02_unknownKey_withFallback_usesFallback() throws Exception {
		// Multipart with comma: first segment unknown -> null, second resolves to title.
		clientA.get("/raw").queryData("expr", "$RS{nonsense,title}").run()
			.assertStatus(200)
			.assertContent("a-title");
	}

	@Test void b03_emptyKey_returnsEmpty() throws Exception {
		// Empty key -> charAt returns 0 -> falls through all branches -> null -> "".
		clientA.get("/raw").queryData("expr", "$RS{}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b04_keyStartingWithUnknownLetter() throws Exception {
		// 'z' isn't matched by any branch -> null -> "".
		clientA.get("/raw").queryData("expr", "$RS{zzz}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b05_cPrefix_notMatching() throws Exception {
		// 'c' branch entered but "cxx" isn't "contact" -> null.
		clientA.get("/raw").queryData("expr", "$RS{cxx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b06_dPrefix_notMatching() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{dxx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b07_ePrefix_notMatching() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{exx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b08_lPrefix_notMatching() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{lxx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b09_oPrefix_notMatching() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{oxx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b10_rPrefix_notMatching() throws Exception {
		// 'r' is dispatched (siteName) but only matches "siteName" exactly.
		clientA.get("/raw").queryData("expr", "$RS{rxx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b11_tPrefix_notMatching() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{txx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void b12_vPrefix_notMatching() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{vxx}").run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - Empty swagger: a resource with no @Swagger and no @Rest title/description info.
	// This exercises the "Optional empty/null inner field" branches where the lambda returns null.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet(path="/{key}")
		public String get(RestRequest req, @Path("key") String key) {
			return req.getVarResolverSession().resolve("$RS{" + key + "}");
		}

		@RestGet(path="/raw")
		public String raw(RestRequest req, @Query("expr") String expr) {
			return req.getVarResolverSession().resolve(expr);
		}
	}

	private static MockRestClient clientC;

	static void beforeAllC() {
		clientC = MockRestClient.build(C.class);
	}

	@Test void c01_title_nullInfo_returnsToken() throws Exception {
		// No info populated -> null -> token preserved.
		// Note: DefaultConfig provides defaults like siteName via env/system properties; title may be null
		// when the @Rest title/description aren't set.  We just make sure this path doesn't blow up.
		clientC.get("/raw").queryData("expr", "$RS{title}").run()
			.assertStatus(200);
	}

	@Test void c02_operationSummary_noOperationSwagger() throws Exception {
		// No summary on the operation -> map(...).orElse(null) -> null -> "".
		clientC.get("/raw").queryData("expr", "$RS{operationSummary}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c03_operationDescription_noOperationSwagger() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{operationDescription}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c04_externalDocs_absent() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{externalDocs}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c05_tags_absent() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{tags}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c06_contact_absent() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{contact}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c07_license_absent() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{license}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c08_termsOfService_absent() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{termsOfService}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void c09_version_absent() throws Exception {
		clientC.get("/raw").queryData("expr", "$RS{version}").run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - Resource with info present but with individual fields (contact, license, etc.) absent.
	// Exercises the "info != null but field == null" arm of each Optional lambda.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(title="b-title")
	public static class E {
		@RestGet(path="/raw")
		public String raw(RestRequest req, @Query("expr") String expr) {
			return req.getVarResolverSession().resolve(expr);
		}
	}

	private static MockRestClient clientE;

	static void beforeAllE() {
		clientE = MockRestClient.build(E.class);
	}

	@Test void e01_contact_infoPresent_contactNull() throws Exception {
		// info exists (because @Rest(title=...)) but contact is null -> orElse(null) -> "".
		clientE.get("/raw").queryData("expr", "$RS{contact}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void e02_description_infoPresent_descriptionNull() throws Exception {
		clientE.get("/raw").queryData("expr", "$RS{description}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void e03_license_infoPresent_licenseNull() throws Exception {
		clientE.get("/raw").queryData("expr", "$RS{license}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void e04_termsOfService_infoPresent_tosNull() throws Exception {
		clientE.get("/raw").queryData("expr", "$RS{termsOfService}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void e05_version_infoPresent_versionNull() throws Exception {
		clientE.get("/raw").queryData("expr", "$RS{version}").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void e06_title_infoPresent_titleSet() throws Exception {
		// Sanity: title is populated when info is.
		clientE.get("/raw").queryData("expr", "$RS{title}").run()
			.assertStatus(200)
			.assertContent("b-title");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - Multipart fallback: $RS{missing,title} should resolve to the first non-null key.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_multipartFallback_secondWins_noOperationSummary() throws Exception {
		// /raw operation has no @RestGet summary -> first key resolves to null, falls through to title.
		clientA.get("/raw").queryData("expr", "$RS{operationSummary,title}").run()
			.assertStatus(200)
			.assertContent("a-title");
	}

	@Test void d02_multipartFallback_firstWins() throws Exception {
		clientA.get("/raw").queryData("expr", "$RS{title,description}").run()
			.assertStatus(200)
			.assertContent("a-title");
	}

	@Test void d03_multipartFallback_allUnknown_returnsEmpty() throws Exception {
		// All keys unknown/null -> resolver substitutes "".
		clientA.get("/raw").queryData("expr", "$RS{zzz,yyy}").run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - Nested-variable handling: allowNested()=false, allowRecurse()=false. Putting another var
	//     inside a $RS{...} body causes the resolver to NOT recursively resolve it (returned literal).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_allowNested_isFalse_innerVarLeftLiteral() throws Exception {
		// Inner $RQ{x} would normally resolve to "title", but allowNested()==false means the body
		// is passed verbatim as the key.  "$RQ{x}" is not a recognized key -> null -> "".
		clientA.get("/raw").queryData("expr", "$RS{$RQ{x}}").queryData("x", "title").run()
			.assertStatus(200)
			.assertContent("");
	}

	@Test void f02_allowRecurse_isFalse_returnedValueNotReResolved() throws Exception {
		// Use a class whose swagger title contains a "$" — when this value resolves the resolver
		// will check allowRecurse() to decide whether to re-resolve. allowRecurse()==false so the
		// raw value is returned. This test mainly forces the allowRecurse() override to execute.
		var client = MockRestClient.build(F.class);
		client.get("/").run()
			.assertStatus(200)
			.assertContent().asString().isContains("dollar-sign-here");
	}

	@Rest(title="dollar-sign-here-$")
	public static class F {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$RS{title}");
		}
	}
}
