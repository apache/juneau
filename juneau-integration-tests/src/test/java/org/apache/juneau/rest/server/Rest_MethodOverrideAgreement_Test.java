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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Pins that dispatch ({@link RestSession#getMethod()}) and the write-protection filters ({@link MethodSafety#effectiveMethod})
 * agree: every method claim dispatch honours must make {@code effectiveMethod} unsafe when the claimed verb is unsafe.
 * On a safe request line, dispatch must also stay safe: it either refuses the claim with {@code 405} or dispatches the
 * line itself, a safe method, or an operation declared {@link RestOp#read() read=true}.
 *
 * <p>
 * The filter side is called with the {@code HttpServletRequest}'s own request-line method, raw query and headers, not with
 * the dispatched verb, which would make the check pass by construction.
 *
 * <p>
 * Limitation: {@code MockServletRequest} builds parameters from the URI query only, so these rows cannot show a container
 * surfacing a form-body {@code method=} on a {@code GET}.  Dispatch reads the claim from the raw query string only, so that
 * risk is removed structurally rather than by a row.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class Rest_MethodOverrideAgreement_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Resources
	//------------------------------------------------------------------------------------------------------------------

	/** Reports the dispatched method and the method a write-protection filter would judge the same request on. */
	public static class Base {
		@RestOp(method="*", path="/*")
		public void any(RestRequest req, RestResponse res) {
			var hsr = req.getHttpServletRequest();
			res.setHeader("X-Result", req.getMethod() + "|" + MethodSafety.effectiveMethod(hsr.getMethod(), hsr.getQueryString(), hsr::getHeader));
		}
	}

	/** Adds two read operations, which the explicit keys reach ahead of the {@code *} operation. */
	public static class ReadBase extends Base {
		@RestOp(method="VIEW", path="/*", read=true)
		public void view(RestRequest req, RestResponse res) {
			any(req, res);
		}

		@RestOp(method="PARSE", path="/*", read=true)
		public void parse(RestRequest req, RestResponse res) {
			any(req, res);
		}
	}

	/** The read verbs {@link ReadBase} declares. */
	private static final Set<String> READS = Set.of("VIEW", "PARSE");

	@Rest(allowedMethodParams="*", allowedMethodHeaders="*")
	public static class Star extends ReadBase {}

	@Rest(allowedMethodParams="PUT", allowedMethodHeaders="PUT")
	public static class OnlyPut extends ReadBase {}

	@Rest(allowedMethodParams="DELETE", allowedMethodHeaders="DELETE")
	public static class OnlyDelete extends ReadBase {}

	@Rest(allowedMethodParams="PATCH", allowedMethodHeaders="PATCH")
	public static class OnlyPatch extends ReadBase {}

	@Rest(allowedMethodParams="POST", allowedMethodHeaders="POST")
	public static class OnlyPost extends ReadBase {}

	@Rest(allowedMethodParams="FROB", allowedMethodHeaders="FROB")
	public static class OnlyFrob extends ReadBase {}

	/** Defaults: params admit HEAD and OPTIONS, headers are off. */
	@Rest()
	public static class Defaults extends ReadBase {}

	@Rest(allowedMethodHeaders="PUT")
	public static class HeadersPut extends ReadBase {}

	@Rest(allowedMethodHeaders="NONE")
	public static class HeadersNone extends ReadBase {}

	@Rest(allowedMethodParams="*")
	public static class ParamsStar extends ReadBase {}

	@Rest(allowedMethodParams="PUT")
	public static class ParamsPut extends ReadBase {}

	private static final Map<Class<?>,MockRestClient> CLIENTS = new ConcurrentHashMap<>();

	private static MockRestClient client(Class<?> c) {
		return CLIENTS.computeIfAbsent(c, k -> MockRestClient.create(k).ignoreErrors().build());
	}

	/** The {@code X-Result} header: {@code dispatched|effective}. */
	private static String[] result(RestResponse_ r) {
		return r.header.split("\\|");
	}

	private record RestResponse_(int status, String header, String allow, String content) {}

	private static RestResponse_ call(Class<?> c, String line, String uri, String... headers) throws Exception {
		var req = client(c).request(line, uri);
		for (var i = 0; i < headers.length; i += 2)
			req = req.header(headers[i], headers[i + 1]);
		var r = req.run();
		return new RestResponse_(r.getStatusCode(), r.getStringHeader("X-Result").orElse(null), r.getStringHeader("Allow").orElse(null), r.getContent().asString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Agreement
	//------------------------------------------------------------------------------------------------------------------

	static Stream<Arguments> agreementRows() {
		var configs = List.of(Star.class, OnlyPut.class, OnlyDelete.class, OnlyPatch.class, OnlyPost.class, OnlyFrob.class);
		var lines = List.of("GET", "HEAD", "OPTIONS", "TRACE");
		List<String> channels = list("q:method", "q:METHOD", "q:%6Dethod");
		MethodSafety.overrideHeaders().forEach(h -> channels.add("h:" + h));
		var values = List.of("DELETE", "PUT", "PATCH", "POST", "FROB", "delete", "VIEW", "view", "PARSE");
		var rows = new ArrayList<Arguments>();
		for (var c : configs)
			for (var l : lines)
				for (var ch : channels)
					for (var v : values)
						rows.add(Arguments.of(c, l, ch, v));
		return rows.stream();
	}

	/** How a guarded-line row ended. */
	private enum Outcome { REFUSED, READ, OTHER }

	/** Every line in the matrix is safe, so every row is checked against the rule as well as the filter. */
	private static Outcome check(Class<?> config, String line, String channel, String value) throws Exception {
		var r = channel.startsWith("q:") ? call(config, line, "/x?" + channel.substring(2) + "=" + value) : call(config, line, "/x", channel.substring(2), value);
		var row = config.getSimpleName() + " " + line + " " + channel + "=" + value;
		if (r.status == 405) {
			assertNull(r.header, row + ": refused, but an operation ran");
			assertNotNull(r.allow, row + ": 405 without Allow");
			assertFalse(r.allow.isEmpty(), row + ": 405 with an empty Allow");
			return Outcome.REFUSED;
		}
		assertNotNull(r.header, row + ": status " + r.status + " but nothing was dispatched");
		var x = result(r);
		var dispatched = x[0];
		var effective = x[1];
		var read = READS.contains(dispatched);
		assertTrue(dispatched.equals(line) || MethodSafety.isSafe(dispatched) || read, row + ": a safe line dispatched " + dispatched);
		if (! dispatched.equals(line) && ! MethodSafety.isSafe(dispatched))
			assertFalse(MethodSafety.isSafe(effective), row + ": dispatched " + dispatched + " but the filter judged " + effective);
		return read ? Outcome.READ : Outcome.OTHER;
	}

	@ParameterizedTest(name="{0} {1} {2}={3}")
	@MethodSource("agreementRows")
	void a01_dispatchedClaimIsUnsafeToTheFilter(Class<?> config, String line, String channel, String value) throws Exception {
		check(config, line, channel, value);
	}

	@Test void a02_overrideIsActuallyHonoured() throws Exception {
		// Guards a01 against being vacuous: the claims it generates really do change the dispatched method.  An unsafe
		// claim is honoured only on an unsafe line, where the line itself is what the filter judges.
		assertEquals("DELETE|POST", call(Star.class, "POST", "/x?method=DELETE").header);
		assertEquals("DELETE|POST", call(Star.class, "POST", "/x?METHOD=delete").header);
		for (var h : MethodSafety.overrideHeaders())
			assertEquals("DELETE|POST", call(Star.class, "POST", "/x", h, "DELETE").header, h);
		// On a safe line, a read is honoured.
		assertEquals("VIEW|VIEW", call(Star.class, "GET", "/x?method=VIEW").header);
	}

	@Test void a03_overrideHeadersOrderIsFrozen() {
		assertEquals(List.of("X-Method", "X-HTTP-Method-Override", "X-HTTP-Method", "X-Method-Override"), MethodSafety.overrideHeaders());
	}

	@Test void a04_firstAdmittedHeaderInOrderWins() throws Exception {
		// Headers on an enabled channel that normalise equal do not conflict; the honoured value is the first admitted one.
		assertEquals("PUT|POST", call(OnlyPut.class, "POST", "/x", "X-Method-Override", "put", "X-HTTP-Method", "PUT").header);
	}

	@Test void a05_guardedLineMatrixNotVacuous() throws Exception {
		var counts = new EnumMap<Outcome,Integer>(Outcome.class);
		for (var row : agreementRows().toList()) {
			var a = row.get();
			counts.merge(check((Class<?>)a[0], (String)a[1], (String)a[2], (String)a[3]), 1, Integer::sum);
		}
		assertTrue(counts.getOrDefault(Outcome.REFUSED, 0) > 0, counts.toString());
		assertTrue(counts.getOrDefault(Outcome.READ, 0) > 0, counts.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Disagreement
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_twoHeadersDisagree_400() throws Exception {
		var r = call(Star.class, "GET", "/x", "X-Method", "DELETE", "X-HTTP-Method", "PUT");
		assertEquals(400, r.status);
		assertNull(r.header);
	}

	@Test void b02_headerDisagreesWithParam_400() throws Exception {
		assertEquals(400, call(Star.class, "GET", "/x?method=PUT", "X-HTTP-Method-Override", "DELETE").status);
		assertEquals(400, call(Star.class, "GET", "/x?method=PUT", "X-Method-Override", "DELETE").status);
	}

	@Test void b03_headersDisabled_notAConflict() throws Exception {
		var r = call(Defaults.class, "GET", "/x?method=HEAD", "X-HTTP-Method-Override", "PUT");
		assertEquals(200, r.status);
		assertEquals("HEAD", result(r)[0]);
	}

	@Test void b04_enabledChannelCountsEvenWhenNotAdmitted() throws Exception {
		assertEquals(400, call(HeadersPut.class, "GET", "/x", "X-Method", "DELETE", "X-HTTP-Method-Override", "PUT").status);
	}

	@Test void b05_noneChannelIsDisabled() throws Exception {
		var r = call(HeadersNone.class, "GET", "/x", "X-Method", "DELETE", "X-HTTP-Method-Override", "PUT");
		assertEquals(200, r.status);
		assertEquals("GET", result(r)[0]);
	}

	@Test void b06_repeatedHeader_distinctValues_400() throws Exception {
		assertEquals(400, call(Star.class, "GET", "/x", "X-HTTP-Method-Override", "GET", "X-HTTP-Method-Override", "DELETE").status);
	}

	@Test void b07_repeatedHeader_sameValueModuloCase_ok() throws Exception {
		var r = call(Star.class, "POST", "/x", "X-HTTP-Method-Override", "PUT", "X-HTTP-Method-Override", "put");
		assertEquals(200, r.status);
		assertEquals("PUT", result(r)[0]);
	}

	@Test void b08_repeatedParam_distinctValues_400() throws Exception {
		assertEquals(400, call(ParamsStar.class, "GET", "/x?method=GET&method=DELETE").status);
	}

	@Test void b09_repeatedParam_sameValue_ok() throws Exception {
		var r = call(ParamsStar.class, "POST", "/x?method=PUT&method=put");
		assertEquals(200, r.status);
		assertEquals("PUT", result(r)[0]);
	}

	@Test void b10_paramNameIsCaseInsensitiveAndDecoded() throws Exception {
		assertEquals("PUT", result(call(ParamsPut.class, "POST", "/x?METHOD=PUT"))[0]);
		assertEquals("PUT", result(call(ParamsPut.class, "POST", "/x?%6Dethod=PUT"))[0]);
	}

	@Test void b11_formBodyMethodField_notHonoured() throws Exception {
		var r = client(ParamsStar.class).request("PUT", "/x").contentType("application/x-www-form-urlencoded").formData("method", "DELETE").run();
		assertEquals("PUT", r.getStringHeader("X-Result").orElse("").split("\\|")[0]);
	}

	@Test void b12_conflictMessageIsFixedAndDoesNotEchoClaims() throws Exception {
		var r = call(Star.class, "GET", "/x", "X-Method", "DELETE", "X-HTTP-Method", "PUTSECRET");
		assertEquals(400, r.status);
		assertTrue(r.content.contains("Conflicting HTTP method overrides."), r.content);
		assertFalse(r.content.contains("PUTSECRET"), r.content);
		assertFalse(r.content.contains("DELETE"), r.content);
	}

	@Test void b13_conflict_withCallLoggerAndDebug_stays400() throws Exception {
		var c = MockRestClient.create(Star.class).ignoreErrors().debug().build();
		var r = c.get("/x").header("X-Method", "DELETE").header("X-HTTP-Method", "PUT").run();
		assertEquals(400, r.getStatusCode());
	}

	@Test void b14_nonConflictingHeaders_stillDispatchWithTrailingSpace405() throws Exception {
		// Whitespace is trimmed for the conflict check but the honoured value is raw, so it still fails the character check.
		assertEquals(405, call(Star.class, "GET", "/x", "X-HTTP-Method-Override", "PUT ").status);
	}
}
