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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.classic.response.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.MockServletRequest;
import org.apache.juneau.rest.mock.MockServletResponse;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.guard.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Pins the rule for method overrides on a safe request line: a {@code GET}, {@code HEAD}, {@code OPTIONS} or
 * {@code TRACE} may be overridden only to another safe method or to an operation declared
 * {@link RestOp#read() read=true}.  Anything else is refused with {@code 405} and an {@code Allow} header, before the
 * operation, its hooks or its guards run.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class Rest_MethodOverrideGetHead_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Resources
	//------------------------------------------------------------------------------------------------------------------

	/** Counts invocations of every operation that would change state. */
	static final AtomicInteger WRITES = new AtomicInteger();

	@BeforeEach void reset() {
		WRITES.set(0);
		PRE_CALLS.set(0);
		MIXIN_START_CALLS.set(0);
	}

	/** Each operation reports its own name in {@code X-Op}, so {@code HEAD} rows can be checked without a body. */
	public static class Ops {
		@RestGet("/x")
		public String get(RestResponse res) {
			return op(res, "GET");
		}

		@RestOp(method="VIEW", path="/x", read=true)
		public String view(RestResponse res) {
			return op(res, "VIEW");
		}

		@RestOp(method="FROB", path="/x")
		public String frob(RestResponse res) {
			WRITES.incrementAndGet();
			return op(res, "FROB");
		}

		@RestDelete("/x")
		public String delete(RestResponse res) {
			WRITES.incrementAndGet();
			return op(res, "DELETE");
		}

		@RestPatch("/x")
		public String patch(RestResponse res) {
			WRITES.incrementAndGet();
			return op(res, "PATCH");
		}

		@RestPut("/x")
		public String put(RestResponse res) {
			WRITES.incrementAndGet();
			return op(res, "PUT");
		}

		@RestPost("/x")
		public String post(RestResponse res) {
			WRITES.incrementAndGet();
			return op(res, "POST");
		}

		@RestOp(method="HEAD", path="/x")
		public String head(RestResponse res) {
			return op(res, "HEAD");
		}
	}

	static String op(RestResponse res, String name) {
		res.setHeader("X-Op", name);
		return name;
	}

	@Rest(allowedMethodParams="*")
	public static class ParamsStar extends Ops {}

	@Rest(allowedMethodHeaders="*")
	public static class HeadersStar extends Ops {}

	@Rest(allowedMethodParams="*", allowedMethodHeaders="*")
	public static class BothStar extends Ops {}

	@Rest(allowedMethodParams="*", allowedMethodHeaders="*")
	public static class Wild extends Ops {
		@RestOp(method="*", path="/w")
		public String any(RestRequest req, RestResponse res) {
			WRITES.incrementAndGet();
			return op(res, "*" + req.getMethod());
		}
	}

	/** {@link Ops} has no {@code OPTIONS} operation, so the {@code ?method=OPTIONS} row uses this one. */
	@Rest(allowedMethodParams="*")
	public static class WithOptions extends Ops {
		@RestOp(method="OPTIONS", path="/x")
		public String options(RestResponse res) {
			return op(res, "OPTIONS");
		}
	}

	@Rest(allowedMethodParams="*")
	public static class Secret {
		@RestGet("/x")
		public String get() {
			return "GET";
		}

		@RestOp(method="DELETESECRET", path="/x")
		public String secret() {
			WRITES.incrementAndGet();
			return "DELETESECRET";
		}
	}

	@Rest(allowedMethodParams="*", problemDetails="true")
	public static class ProblemStar extends Ops {}

	/** Defaults: params admit {@code HEAD} and {@code OPTIONS}, headers are off. */
	@Rest()
	public static class Defaults extends Ops {}

	static final AtomicInteger PRE_CALLS = new AtomicInteger();
	static final AtomicInteger MIXIN_START_CALLS = new AtomicInteger();

	/** A guard that would refuse the request with {@code 401} if it ever ran. */
	public static class Unauthenticated extends RestGuard {
		@Override
		public boolean isRequestAllowed(RestRequest req) {
			throw new Unauthorized();
		}
	}

	@Rest
	public static class HookMixin {
		@RestStartCall
		public void start() {
			MIXIN_START_CALLS.incrementAndGet();
		}

		@RestGet("/m")
		public String get() {
			return "GET";
		}

		@RestDelete(path="/m", guards=Unauthenticated.class)
		public String delete() {
			WRITES.incrementAndGet();
			return "DELETE";
		}

		@RestOp(method="VIEW", path="/mv", read=true)
		public String view() {
			return "VIEW";
		}
	}

	@Rest(mixins=HookMixin.class, allowedMethodParams="*")
	public static class HookHost {
		@RestPreCall
		public void pre() {
			PRE_CALLS.incrementAndGet();
		}
	}

	@Remote
	public interface Calc {
		String greet(String name);
	}

	@Rest(allowedMethodParams="*", parsers={JsonParser.class}, serializers={JsonSerializer.class})
	public static class Rrpc {
		@RestOp(method="RRPC", path="/proxy/*")
		public Calc proxy() {
			return x -> "Hello, " + x;
		}
	}

	@Rest(allowedMethodParams="*")
	public static class PathVars {
		@RestGet("/v/{id}")
		public String get(@Path("id") String id) {
			return id;
		}

		@RestDelete("/v/{id}")
		public String delete(@Path("id") String id) {
			WRITES.incrementAndGet();
			return id;
		}
	}

	/** Replaces the operation table with a subclass that still delegates lookups. */
	@Rest(allowedMethodParams="*")
	public static class ReplacedOps extends Ops {
		@Bean
		public RestOperations restOperations(RestOperations.Builder b) {
			return new RestOperations(b) {
				@Override
				public RestOpContext findOperation(RestSession session) throws MethodNotAllowed, PreconditionFailed, NotFound {
					return super.findOperation(session);
				}
			};
		}
	}

	private static final Map<Class<?>,MockRestClient> CLIENTS = new ConcurrentHashMap<>();

	private static MockRestClient client(Class<?> c) {
		return CLIENTS.computeIfAbsent(c, k -> MockRestClient.create(k).ignoreErrors().build());
	}

	private record Result(int status, String op, String allow, String content) {}

	private static Result call(Class<?> c, String line, String uri, String... headers) throws Exception {
		var req = client(c).request(line, uri);
		for (var i = 0; i < headers.length; i += 2)
			req = req.header(headers[i], headers[i + 1]);
		var r = req.run();
		return new Result(r.getStatusCode(), r.getStringHeader("X-Op").orElse(null), r.getStringHeader("Allow").orElse(null), r.getContent().asString());
	}

	/** Sends a claim on one channel: {@code q} is the {@code method} parameter, anything else names a header. */
	private static Result claim(Class<?> c, String line, String channel, String value) throws Exception {
		return "q".equals(channel) ? call(c, line, "/x?method=" + value) : call(c, line, "/x", channel, value);
	}

	private static void assertRefused(Result r, String row) {
		assertEquals(405, r.status, row);
		assertNull(r.op, row + ": an operation ran");
		assertNotNull(r.allow, row + ": no Allow header");
		assertEquals(0, WRITES.get(), row + ": a write ran");
		// Every unsafe claim has an operation at /x, so this is the new refusal, not findOperation's own 405.
		if (! row.startsWith("HEAD"))
			assertTrue(r.content.contains("Method override not allowed on a safe request method."), row + ": " + r.content);
	}

	static Stream<Arguments> unsafeClaims() {
		var channels = new ArrayList<String>();
		channels.add("q");
		channels.addAll(MethodSafety.overrideHeaders());
		var values = List.of("DELETE", "delete", "Delete", "PATCH", "PUT", "POST", "FROB");
		var rows = new ArrayList<Arguments>();
		for (var ch : channels)
			for (var v : values)
				rows.add(Arguments.of(ch, v));
		return rows.stream();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Refused
	//------------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}={1}")
	@MethodSource("unsafeClaims")
	void a01_getOverrideToUnsafe_405(String channel, String value) throws Exception {
		assertRefused(claim(BothStar.class, "GET", channel, value), "GET " + channel + "=" + value);
	}

	@ParameterizedTest(name="{0}={1}")
	@MethodSource("unsafeClaims")
	void a02_headOverrideToUnsafe_405(String channel, String value) throws Exception {
		assertRefused(claim(BothStar.class, "HEAD", channel, value), "HEAD " + channel + "=" + value);
	}

	/** Records the request-line method as the servlet request carried it. */
	@Rest(allowedMethodParams="*")
	public static class RawLine extends Ops {
		static volatile String seen;

		@RestGet("/raw")
		public String raw(RestRequest req) {
			seen = req.getHttpServletRequest().getMethod();
			return "GET";
		}
	}

	@Test void a03_lowercaseRequestLine_405() throws Exception {
		// The classic mock client upper-cases the line, so these go straight to RestContext.execute.
		var ctx = new RestContext(new RestContext.Args(RawLine.class, null, null, RawLine::new, "", null, null, null, RestContext.ContextKind.ROOT)).postInit().postInitChildFirst();
		for (var line : List.of("get", "Get")) {
			RawLine.seen = null;
			var res = MockServletResponse.create();
			ctx.execute(ctx.getResource(), MockServletRequest.create(line, "/raw").servletPath("").contextPath(""), res);
			assertEquals(200, res.getStatus(), line);
			assertEquals(line, RawLine.seen, "the line did not arrive in its original case");

			res = MockServletResponse.create();
			ctx.execute(ctx.getResource(), MockServletRequest.create(line, "/x?method=DELETE").servletPath("").contextPath(""), res);
			assertEquals(405, res.getStatus(), line);
			assertEquals(0, WRITES.get(), line);
		}
	}

	@Test void a09_wildcardOpNeverRead() throws Exception {
		assertEquals(405, call(Wild.class, "GET", "/w?method=FROB").status);
		assertEquals(405, call(Wild.class, "GET", "/w?method=DELETE").status);
		assertEquals(0, WRITES.get());
		var r = call(Wild.class, "GET", "/w");
		assertEquals(200, r.status);
		assertEquals("*GET", r.op);
	}

	@Test void a10_noOpForOverride_existingStatus() throws Exception {
		assertEquals(404, call(BothStar.class, "GET", "/nope?method=DELETE").status);
		// No MOVE operation at /x, but others match the path: findOperation's own 405, which now carries Allow too.
		var r = call(BothStar.class, "GET", "/x?method=MOVE");
		assertEquals(405, r.status);
		assertEquals("DELETE, FROB, GET, HEAD, PATCH, POST, PUT, VIEW", r.allow);
	}

	@Test void a11_conflictStill400First() throws Exception {
		var r = call(BothStar.class, "GET", "/x?method=VIEW", "X-Method", "DELETE");
		assertEquals(400, r.status);
		assertTrue(r.content.contains("Conflicting HTTP method overrides."), r.content);
	}

	@Test void a12_invalidCharsStill405() throws Exception {
		var r = call(BothStar.class, "GET", "/x?method=VIEW%20");
		assertEquals(405, r.status);
		assertFalse(r.content.contains("Method override not allowed on a safe request method."), r.content);
	}

	@Test void a13_messageFixedNoEcho() throws Exception {
		var r = call(Secret.class, "GET", "/x?method=DELETESECRET");
		assertEquals(405, r.status);
		assertTrue(r.content.contains("Method override not allowed on a safe request method."), r.content);
		assertFalse(r.content.contains("DELETESECRET"), r.content);
		assertEquals(0, WRITES.get());
	}

	@Test void a14_allowHeader() throws Exception {
		assertEquals("DELETE, FROB, GET, HEAD, PATCH, POST, PUT, VIEW", call(BothStar.class, "GET", "/x?method=DELETE").allow);
		// Only the wildcard operation matches /w, and it accepts the request line.
		assertEquals("GET", call(Wild.class, "GET", "/w?method=DELETE").allow);
		assertEquals("HEAD", call(Wild.class, "HEAD", "/w?method=DELETE").allow);
	}

	@Test void a15_allowHeader_problemDetails() throws Exception {
		var r = client(ProblemStar.class).get("/x?method=DELETE").accept("application/problem+json").run();
		r.assertStatus(405);
		r.assertHeader("Content-Type").isContains("application/problem+json");
		assertEquals("DELETE, FROB, GET, HEAD, PATCH, POST, PUT, VIEW", r.getStringHeader("Allow").orElse(null));
	}

	@Test void a16_refusedBeforeAuthAndPreCall() throws Exception {
		var r = call(HookHost.class, "GET", "/m?method=DELETE");
		assertEquals(405, r.status);
		assertEquals(0, PRE_CALLS.get());
		assertEquals(0, MIXIN_START_CALLS.get());
		assertEquals(0, WRITES.get());
		// Not vacuous: the same hooks run on an accepted request, and the guard really would refuse the write.
		assertEquals(200, call(HookHost.class, "GET", "/m").status);
		assertEquals(1, PRE_CALLS.get());
		assertEquals(1, MIXIN_START_CALLS.get());
		assertEquals(401, call(HookHost.class, "DELETE", "/m").status);
	}

	@Test void a17_rrpcGetOverrideToPost_405() throws Exception {
		var r = call(Rrpc.class, "GET", "/proxy/greet?method=POST");
		assertEquals(405, r.status);
		assertEquals("GET, POST", r.allow);
		var ok = client(Rrpc.class).post("/proxy/" + urlEncode("greet/(java.lang.String)"), "[\"world\"]").contentType("application/json").run();
		ok.assertStatus(200);
		ok.assertContent("\"Hello, world\"");
	}

	@Test void a17b_allowHelperDoesNotDisturbMatch() throws Exception {
		var r = call(PathVars.class, "GET", "/v/first?method=DELETE");
		assertEquals(405, r.status);
		assertEquals("DELETE, GET", r.allow);
		assertEquals(0, WRITES.get());
		var ok = call(PathVars.class, "GET", "/v/second");
		assertEquals(200, ok.status);
		assertEquals("second", ok.content);
	}

	@Test void a19_replacedRestOperations_stillEnforced() throws Exception {
		assertRefused(call(ReplacedOps.class, "GET", "/x?method=DELETE"), "replaced RestOperations");
	}

	@ParameterizedTest(name="{0}={1}")
	@MethodSource("unsafeClaims")
	void a21_optionsOverrideToUnsafe_405(String channel, String value) throws Exception {
		assertRefused(claim(BothStar.class, "OPTIONS", channel, value), "OPTIONS " + channel + "=" + value);
	}

	@Test void a22_traceOverrideToUnsafe_405() throws Exception {
		assertRefused(call(BothStar.class, "TRACE", "/x?method=DELETE"), "TRACE ?method=DELETE");
		assertRefused(call(BothStar.class, "TRACE", "/x", "X-HTTP-Method-Override", "PATCH"), "TRACE X-HTTP-Method-Override: PATCH");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Dispatched
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_getOverrideToReadVerb_dispatches() throws Exception {
		for (var uri : List.of("/x?method=VIEW", "/x?method=view", "/x?METHOD=VIEW", "/x?%6Dethod=VIEW")) {
			var r = call(ParamsStar.class, "GET", uri);
			assertEquals(200, r.status, uri);
			assertEquals("VIEW", r.content, uri);
		}
		var r = call(HeadersStar.class, "GET", "/x", "X-HTTP-Method", "VIEW");
		assertEquals(200, r.status);
		assertEquals("VIEW", r.content);
	}

	@Test void a05_headOverrideToReadVerb_dispatches() throws Exception {
		var r = call(ParamsStar.class, "HEAD", "/x?method=VIEW");
		assertEquals(200, r.status);
		assertEquals("VIEW", r.op);
		assertEquals("", r.content);
	}

	@Test void a06_getOverrideToSafeVerb_dispatches() throws Exception {
		assertEquals("HEAD", call(ParamsStar.class, "GET", "/x?method=HEAD").op);
		assertEquals("OPTIONS", call(WithOptions.class, "GET", "/x?method=OPTIONS").op);
		var r = call(ParamsStar.class, "HEAD", "/x?method=GET");
		assertEquals(200, r.status);
		assertEquals("GET", r.op);
	}

	@Test void a07_postOverrideStillWorks() throws Exception {
		assertEquals("PATCH", call(BothStar.class, "POST", "/x?method=PATCH").op);
		assertEquals("DELETE", call(BothStar.class, "POST", "/x", "X-HTTP-Method-Override", "DELETE").op);
		assertEquals("FROB", call(BothStar.class, "POST", "/x?method=FROB").op);
		assertEquals(3, WRITES.get());
	}

	@Test void a08_putOverrideDowngradeUnchanged() throws Exception {
		assertEquals("GET", call(ParamsStar.class, "PUT", "/x?method=GET").op);
	}

	@Test void a18_mixinOpRead() throws Exception {
		var r = call(HookHost.class, "GET", "/mv?method=VIEW");
		assertEquals(200, r.status);
		assertEquals("VIEW", r.content);
	}

	@Test void a20_defaults_noBehaviourChange() throws Exception {
		var r = call(Defaults.class, "GET", "/x?method=DELETE");
		assertEquals(200, r.status);
		assertEquals("GET", r.op);
		assertEquals(0, WRITES.get());
	}

	@Test void a23_optionsAndTraceOverrideToRead_dispatch() throws Exception {
		assertEquals("VIEW", call(ParamsStar.class, "OPTIONS", "/x?method=VIEW").op);
		assertEquals("VIEW", call(ParamsStar.class, "TRACE", "/x?method=VIEW").op);
	}

	@Test void a24_optionsOverrideToSafe_dispatch() throws Exception {
		assertEquals("GET", call(ParamsStar.class, "OPTIONS", "/x?method=GET").op);
		assertEquals("HEAD", call(ParamsStar.class, "TRACE", "/x?method=HEAD").op);
	}
}
