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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.client.classic.RestResponse;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Serving/security-matrix test for {@link SavedViewsMixin} &mdash; the saved-views REST endpoints (TODO-444 §3.3).
 *
 * <p>
 * Drives the fixed-mount endpoints through a {@link MockRestClient} and exercises the security matrix the plan
 * requires: unauthenticated (incl. the safe {@code GET list}) is {@code 401}; user A cannot read/write/delete user
 * B's views; a non-JSON write is {@code 415}; blank/reserved/over-length names and a missing {@code view} are
 * {@code 400}; the per-blob size cap is {@code 413} and the count quotas (per-scope and per-user aggregate) are
 * {@code 507}; {@code saveAndActivate} is atomic; set/clear active and dangling-active&rarr;Default resolve; and
 * delete works.
 *
 * <h5 class='section'>CSRF / Origin / Host are intentionally NOT tested here</h5>
 * <p>
 * Those checks are enforced by {@link org.apache.juneau.rest.server.filter.LoopbackBoundaryFilter} (a {@code /*}
 * servlet filter that runs BEFORE the mixin), which a {@code MockRestClient} does not run &mdash; and the mixin
 * deliberately does not re-implement CSRF (see the mixin javadoc).  Their coverage lives in
 * {@code LoopbackBoundary_Test} / {@code LoopbackBoundaryFilter_Test}.  The one write-time transport check the mixin
 * DOES own &mdash; {@code Content-Type: application/json} (415) &mdash; is exercised below.
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class SavedViewsMixin_Serving_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements SavedViewsMixin {
		private static final long serialVersionUID = 1L;

		/** Swappable so each test gets a clean store (and the quota tests can install a small-cap one). */
		static volatile SavedViewStore store = new InMemorySavedViewStore();

		@Override public SavedViewStore savedViewStore() { return store; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";
	private static final Principal BLANK = () -> "   ";

	private static final String BLOB = "{\"schemaVersion\":1}";
	private static final String BASE = "/juneau-saved-views";
	private static final String ITEM = BASE + "/item";
	private static final String ACTIVE = BASE + "/active";

	@BeforeEach void reset() {
		R.store = new InMemorySavedViewStore();
	}

	// Helpers -----------------------------------------------------------------------------------------------------------

	private static RestResponse getReq(Principal p, String url) throws Exception {
		var req = c.get(url);
		if (p != null)
			req.userPrincipal(p);
		req.header("Accept", "application/json");
		return req.run();
	}

	private static RestResponse putJson(Principal p, String url, String body) throws Exception {
		var req = c.put(url, body, ContentType.APPLICATION_JSON);
		if (p != null)
			req.userPrincipal(p);
		req.header("Accept", "application/json");
		return req.run();
	}

	private static RestResponse deleteJson(Principal p, String url) throws Exception {
		// The classic client refuses a content entity on DELETE, so the JSON content type travels as a bare header
		// (no body) - the mixin's delete reads 'name' from the query and never consumes a body anyway.
		var req = c.delete(url);
		if (p != null)
			req.userPrincipal(p);
		req.header("Content-Type", "application/json");
		req.header("Accept", "application/json");
		return req.run();
	}

	private static Map<?,?> asMap(RestResponse r) throws Exception {
		return Json.to(r.getContent().asString(), Map.class);
	}

	private static void assertWriteOk(RestResponse r) {
		var code = r.getStatusCode();
		assertTrue(code == 200 || code == 204, "expected a 2xx-no-error write status but got " + code);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Authentication: 401 on absent/blank principal for EVERY op, including GET list
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_listUnauthenticatedIs401() throws Exception {
		getReq(null, BASE + "?view=grid").assertStatus(401);
	}

	@Test void a02_blankPrincipalIs401() throws Exception {
		getReq(BLANK, BASE + "?view=grid").assertStatus(401);
	}

	@Test void a03_everyWriteUnauthenticatedIs401() throws Exception {
		putJson(null, ITEM + "?view=grid&name=v1", BLOB).assertStatus(401);
		putJson(null, ACTIVE + "?view=grid", "{\"name\":\"v1\"}").assertStatus(401);
		deleteJson(null, ITEM + "?view=grid&name=v1").assertStatus(401);
		getReq(null, ITEM + "?view=grid&name=v1").assertStatus(401);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Happy path: save, list, load, save+activate, set/clear active, delete
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_saveThenListThenLoad() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&page=orders&name=v1", BLOB));
		var listing = asMap(getReq(ALICE, BASE + "?view=grid&page=orders").assertStatus(200));
		assertNull(listing.get("active"));
		var views = (List<?>) listing.get("views");
		assertEquals(1, views.size());
		assertEquals("v1", ((Map<?,?>) views.get(0)).get("name"));
		var blob = asMap(getReq(ALICE, ITEM + "?view=grid&page=orders&name=v1").assertStatus(200));
		assertEquals(1, ((Number) blob.get("schemaVersion")).intValue());
	}

	@Test void b02_saveAndActivateFlipsActive() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1&activate=1", BLOB));
		var listing = asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200));
		assertEquals("v1", listing.get("active"));
	}

	@Test void b03_plainSaveDoesNotActivate() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		var listing = asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200));
		assertNull(listing.get("active"));
	}

	@Test void b04_setActiveThenClear() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		assertWriteOk(putJson(ALICE, ACTIVE + "?view=grid", "{\"name\":\"v1\"}"));
		assertEquals("v1", asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200)).get("active"));
		assertWriteOk(putJson(ALICE, ACTIVE + "?view=grid", "{}"));
		assertNull(asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200)).get("active"));
	}

	@Test void b05_danglingActiveResolvesToDefault() throws Exception {
		// Activate a name that was never saved -> list() resolves it to Default (null), never an error.
		assertWriteOk(putJson(ALICE, ACTIVE + "?view=grid", "{\"name\":\"ghost\"}"));
		assertNull(asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200)).get("active"));
	}

	@Test void b06_deleteRemovesAndClearsActive() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1&activate=1", BLOB));
		assertWriteOk(deleteJson(ALICE, ITEM + "?view=grid&name=v1"));
		var listing = asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200));
		assertTrue(((List<?>) listing.get("views")).isEmpty());
		assertNull(listing.get("active"));
		getReq(ALICE, ITEM + "?view=grid&name=v1").assertStatus(404);
	}

	@Test void b07_nameWithSpecialCharsRoundTrips() throws Exception {
		// Wire-encoded space + slash; the container decodes, the structured-tuple store accepts them verbatim.
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=My%20View%2F1", BLOB));
		var views = (List<?>) asMap(getReq(ALICE, BASE + "?view=grid").assertStatus(200)).get("views");
		assertEquals("My View/1", ((Map<?,?>) views.get(0)).get("name"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Cross-user isolation: A cannot read/write/delete B's views (and vice versa)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_userBCannotSeeUserAViews() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		var bList = asMap(getReq(BOB, BASE + "?view=grid").assertStatus(200));
		assertTrue(((List<?>) bList.get("views")).isEmpty());
		getReq(BOB, ITEM + "?view=grid&name=v1").assertStatus(404);
	}

	@Test void c02_userBWriteDoesNotDisturbUserA() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		assertWriteOk(putJson(BOB, ITEM + "?view=grid&name=v1", "{\"schemaVersion\":1,\"who\":\"bob\"}"));
		var aBlob = asMap(getReq(ALICE, ITEM + "?view=grid&name=v1").assertStatus(200));
		assertNull(aBlob.get("who"));  // A still sees A's blob, not B's
	}

	@Test void c03_userBDeleteDoesNotRemoveUserAView() throws Exception {
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		assertWriteOk(deleteJson(BOB, ITEM + "?view=grid&name=v1"));  // no-op in B's namespace
		getReq(ALICE, ITEM + "?view=grid&name=v1").assertStatus(200);  // A's view survives
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Content-Type: a non-JSON write is 415 (mixin defense-in-depth; the filter also enforces it)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_nonJsonSaveIs415() throws Exception {
		c.put(ITEM + "?view=grid&name=v1", BLOB, ContentType.TEXT_PLAIN).userPrincipal(ALICE).run().assertStatus(415);
	}

	@Test void d02_nonJsonSetActiveIs415() throws Exception {
		c.put(ACTIVE + "?view=grid", "{\"name\":\"v1\"}", ContentType.TEXT_PLAIN).userPrincipal(ALICE).run().assertStatus(415);
	}

	@Test void d03_deleteWithoutJsonContentTypeIs415() throws Exception {
		c.delete(ITEM + "?view=grid&name=v1").userPrincipal(ALICE).header("Content-Type", "text/plain").run().assertStatus(415);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Validation: 400 on missing view / blank / reserved / over-length name / non-JSON body; 404 on missing load
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_missingViewParamIs400() throws Exception {
		getReq(ALICE, BASE).assertStatus(400);
	}

	@Test void e02_blankNameIs400() throws Exception {
		putJson(ALICE, ITEM + "?view=grid&name=", BLOB).assertStatus(400);
		getReq(ALICE, ITEM + "?view=grid&name=").assertStatus(400);
		deleteJson(ALICE, ITEM + "?view=grid&name=").assertStatus(400);
	}

	@Test void e03_reservedDefaultNameIs400() throws Exception {
		putJson(ALICE, ITEM + "?view=grid&name=Default", BLOB).assertStatus(400);
		putJson(ALICE, ITEM + "?view=grid&name=default", BLOB).assertStatus(400);  // case-insensitive
		putJson(ALICE, ITEM + "?view=grid&name=%20DEFAULT%20", BLOB).assertStatus(400);  // trimmed
	}

	@Test void e04_overLengthNameIs400() throws Exception {
		var longName = "x".repeat(SavedViewsMixin.MAX_NAME_LEN + 1);
		putJson(ALICE, ITEM + "?view=grid&name=" + longName, BLOB).assertStatus(400);
	}

	@Test void e05_nonJsonBodyIs400() throws Exception {
		// Correct content type (passes 415), but the body is not a JSON object -> 400 (not a quota/415).
		putJson(ALICE, ITEM + "?view=grid&name=v1", "not json").assertStatus(400);
	}

	@Test void e06_emptyBodyIs400() throws Exception {
		putJson(ALICE, ITEM + "?view=grid&name=v1", "").assertStatus(400);
	}

	@Test void e07_loadMissingIs404() throws Exception {
		getReq(ALICE, ITEM + "?view=grid&name=nope").assertStatus(404);
	}

	@Test void e08_setActiveBlankNameIs400() throws Exception {
		// A present-but-blank name in the set-active body is a malformed name -> 400 (clearing uses {} instead).
		putJson(ALICE, ACTIVE + "?view=grid", "{\"name\":\"\"}").assertStatus(400);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Quotas: per-blob size 413; per-scope count 507; per-user aggregate 507
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_oversizeBlobIs413() throws Exception {
		var big = "{\"schemaVersion\":1,\"pad\":\"" + "x".repeat((int) SavedViewsMixin.MAX_BLOB_BYTES) + "\"}";
		putJson(ALICE, ITEM + "?view=grid&name=big", big).assertStatus(413);
	}

	@Test void f02_perScopeCountQuotaIs507() throws Exception {
		R.store = new InMemorySavedViewStore(2, 100);
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v0", BLOB));
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		putJson(ALICE, ITEM + "?view=grid&name=v2", BLOB).assertStatus(507);
	}

	@Test void f03_replaceAtScopeCapSucceeds() throws Exception {
		R.store = new InMemorySavedViewStore(2, 100);
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v0", BLOB));
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v1", BLOB));
		// Overwriting an existing name at the cap is a replace, not a create -> not a quota rejection.
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&name=v0", "{\"schemaVersion\":1,\"z\":9}"));
	}

	@Test void f04_perUserAggregateQuotaIs507() throws Exception {
		R.store = new InMemorySavedViewStore(100, 2);
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&page=p1&name=v", BLOB));
		assertWriteOk(putJson(ALICE, ITEM + "?view=grid&page=p2&name=v", BLOB));
		// Third create in yet another scope trips the aggregate ceiling, not the per-scope one.
		putJson(ALICE, ITEM + "?view=grid&page=p3&name=v", BLOB).assertStatus(507);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) The mount is the fixed prefix
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_fixedMountConstants() {
		assertEquals("/juneau-saved-views", SavedViewsMixin.SAVED_VIEWS_PREFIX);
		assertEquals("/juneau-saved-views/item", SavedViewsMixin.ITEM_PATH);
		assertEquals("/juneau-saved-views/active", SavedViewsMixin.ACTIVE_PATH);
	}
}
