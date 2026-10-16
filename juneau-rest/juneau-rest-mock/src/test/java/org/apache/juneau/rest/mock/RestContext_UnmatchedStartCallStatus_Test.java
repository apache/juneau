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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * End-to-end tests for the status produced when no operation matches and a {@code @RestStartCall} hook has already
 * written a status on the response.
 *
 * <p>
 * Uses a response that defaults to the servlet-spec {@code 200}, as a real container does, rather than the mock's
 * coincidental {@code 0}.  An unmatched request must be a 404 (or 405 for a known path with the wrong method) even
 * when the start call explicitly set {@code 200}; a start call that left a 4xx status other than 404/405/412 must keep
 * that status; a 5xx left by a start call stays an HTTP 500.
 *
 * @since 10.0.0
 */
class RestContext_UnmatchedStartCallStatus_Test {

	/** Start call records which resource ran, and sets the status from the {@code X-Start-Status} request header. */
	private static void applyStartStatus(String who, HttpServletRequest req, HttpServletResponse res) {
		res.addHeader("X-Start", who);
		var s = req.getHeader("X-Start-Status");
		if (s != null)
			res.setStatus(Integer.parseInt(s));
	}

	@Rest
	public static class A_Top {
		@RestStartCall public void start(HttpServletRequest req, HttpServletResponse res) { applyStartStatus("top", req, res); }
		@RestGet(path="/foo") public String foo() { return "OK"; }
	}

	@Rest(path="/bar")
	public static class B_Child {
		@RestStartCall public void start(HttpServletRequest req, HttpServletResponse res) { applyStartStatus("child", req, res); }
		@RestGet(path="/bar") public String bar() { return "OK"; }
	}

	@Rest(children=B_Child.class)
	public static class B_Parent {
		@RestStartCall public void start(HttpServletRequest req, HttpServletResponse res) { applyStartStatus("parent", req, res); }
		@RestGet(path="/foo") public String foo() { return "OK"; }
	}

	@Rest(children=B_Child.class)
	public static class C_Group extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
		@RestStartCall public void start(HttpServletRequest req, HttpServletResponse res) { applyStartStatus("group", req, res); }
	}

	private static int dispatch(Object resource, String method, String path, Integer startStatus) throws Exception {
		return run(resource, method, path, startStatus).getStatus();
	}

	/** The start-call hooks that fired for the request (comma-separated). */
	private static String started(MockServletResponse res) {
		return String.join(",", res.getHeaders("X-Start"));
	}

	private static MockServletResponse run(Object resource, String method, String path, Integer startStatus) throws Exception {
		var ctx = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
		var req = MockServletRequest.create(method, path).header("Accept", "text/plain");
		if (startStatus != null)
			req.header("X-Start-Status", String.valueOf(startStatus));
		var res = new MockServletResponse();
		ctx.execute(resource, req, res);
		return res;
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - Start call set 200 (the real-container default) and nothing matched => 404 / 405.
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_topResource_unmatchedPath_startSets200_404() throws Exception {
		assertEquals(404, dispatch(new A_Top(), "GET", "/nope", 200));
	}

	@Test void a02_topResource_unmatchedPath_noStartStatus_404() throws Exception {
		assertEquals(404, dispatch(new A_Top(), "GET", "/nope", null));
	}

	@Test void a03_childResource_unmatchedPathInChild_startSets200_404() throws Exception {
		// Routed into the child (only the child's start call fires), where no operation matches.
		var res = run(new B_Parent(), "GET", "/bar/nope", 200);
		assertEquals(404, res.getStatus());
		assertEquals("child", started(res));
	}

	@Test void a03b_childResource_matchedOperation_200() throws Exception {
		// Control: the child is reachable, so a03 really exercises an unmatched path inside it.
		var res = run(new B_Parent(), "GET", "/bar/bar", 200);
		assertEquals(200, res.getStatus());
		assertEquals("child", started(res));
	}

	@Test void a04_childResource_noChildMatches_startSets200_404() throws Exception {
		// No child matches the path, so only the parent's start call fires.
		var res = run(new B_Parent(), "GET", "/nochild/x", 200);
		assertEquals(404, res.getStatus());
		assertEquals("parent", started(res));
	}

	@Test void a05_groupServlet_noChildMatches_startSets200_404() throws Exception {
		var res = run(new C_Group(), "GET", "/unknown", 200);
		assertEquals(404, res.getStatus());
		assertEquals("group", started(res));
	}

	@Test void a06_wrongMethodOnMatchedPath_405() throws Exception {
		assertEquals(405, dispatch(new A_Top(), "POST", "/foo", null));
	}

	@Test void a07_wrongMethodOnMatchedPath_startSets200_405() throws Exception {
		assertEquals(405, dispatch(new A_Top(), "POST", "/foo", 200));
	}

	@Test void a08_matchedPath_startSets200_200() throws Exception {
		assertEquals(200, dispatch(new A_Top(), "GET", "/foo", 200));
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - Start call left a 4xx (not 404/405/412) and nothing matched => the start call's status is kept.
	//-----------------------------------------------------------------------------------------------------------

	@Test void a09_startSets401_wrongMethod_405Wins() throws Exception {
		// A method mismatch on a matched path is reported as 405 even though the start call left a 401.
		assertEquals(405, dispatch(new A_Top(), "POST", "/foo", 401));
	}

	@Test void b01_startSets400_unmatched_keeps400() throws Exception {
		assertEquals(400, dispatch(new A_Top(), "GET", "/nope", 400));
	}

	@Test void b02_startSets401_unmatched_keeps401() throws Exception {
		assertEquals(401, dispatch(new A_Top(), "GET", "/nope", 401));
	}

	@Test void b03_startSets403_unmatched_keeps403() throws Exception {
		assertEquals(403, dispatch(new A_Top(), "GET", "/nope", 403));
	}

	@Test void b04_startSets409_unmatched_keeps409() throws Exception {
		assertEquals(409, dispatch(new A_Top(), "GET", "/nope", 409));
	}

	@Test void b05_startSets412_unmatched_keeps412() throws Exception {
		assertEquals(412, dispatch(new A_Top(), "GET", "/nope", 412));
	}

	@Test void b06_startSets418_unmatched_keeps418() throws Exception {
		assertEquals(418, dispatch(new A_Top(), "GET", "/nope", 418));
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - Start call left a 5xx and nothing matched => stays an HTTP 500 (decision pinned).
	//-----------------------------------------------------------------------------------------------------------

	@Test void c01_startSets500_unmatched_stays500() throws Exception {
		assertEquals(500, dispatch(new A_Top(), "GET", "/nope", 500));
	}

	@Test void c02_startSets503_unmatched_becomes500() throws Exception {
		assertEquals(500, dispatch(new A_Top(), "GET", "/nope", 503));
	}
}
