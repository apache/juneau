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
package org.apache.juneau.rest.server.bus;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;

/**
 * Exposes the bus's session POST and SSE stream on a REST resource (§11.3).
 *
 * <ul>
 * 	<li><b>{@code POST <servlet>/juneau-bus/session}</b> &mdash; opens a session and returns its grant.  This is the
 * 		protocol's only state-changing request, so every guard the application has applies to it.
 * 	<li><b>{@code GET <servlet>/juneau-bus/stream/{sessionId}}</b> &mdash; a held-open {@code text/event-stream} of bus
 * 		frames, each as {@code event: bus}.  Gated by the capability in the path; an unknown or expired id, or another
 * 		principal, is a {@code 404}.
 * </ul>
 *
 * <p>
 * When {@link BusPolicy#boundary()} is set, both endpoints run its {@code LoopbackBoundary} check: the full write check
 * on the POST, the Host check on the GET.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <ja>@Rest</ja>(path=<js>"/ops/jobs"</js>)
 *   <jk>public class</jk> JobsRest <jk>extends</jk> BasicRestServlet <jk>implements</jk> AsyncJobsMixin, BusEventsMixin {
 *     <jk>static final</jk> ServerBus <jsf>BUS</jsf> = ServerBus.<jsm>create</jsm>(BusPolicy.<jsm>create</jsm>()
 *       .downstream(<js>"ops.jobs"</js>, <jk>true</jk>).build());
 *     <ja>@Override</ja> <jk>public</jk> ServerBus serverBus() { <jk>return</jk> <jsf>BUS</jsf>; }
 *     ...
 *   }
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// An application with its own CSRF guard puts it in front of the session POST.
 *   // Re-declaring @RestPost here is optional and registers the route once.</jc>
 *   <ja>@Override</ja> <ja>@RestPost</ja>(path=<jsf>SESSION_PATH</jsf>)
 *   <jk>public</jk> JsonMap openBusSession(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 *     requireCsrfToken(<jv>req</jv>);
 *     <jk>return</jk> BusEventsMixin.<jk>super</jk>.openBusSession(<jv>req</jv>, <jv>res</jv>);
 *   }
 * </p>
 *
 * @since 10.0.0
 */
public interface BusEventsMixin {

	/** The servlet-relative prefix of both endpoints. */
	String BUS_PREFIX = "/juneau-bus";

	/** The session POST path. */
	String SESSION_PATH = BUS_PREFIX + "/session";

	/** The SSE stream path. */
	String STREAM_PATH = BUS_PREFIX + "/stream/{sessionId}";

	/**
	 * The bus this resource serves.
	 *
	 * @return The bus.  Typically one per application, shared with the code that publishes.
	 */
	ServerBus serverBus();

	/**
	 * [POST /juneau-bus/session] &mdash; opens a bus session (spec §11.3).
	 *
	 * <p>
	 * Body (at most 64 KB; a larger body is a 400): {@code {"v":1, "bridge":"ops", "transport":"sse"|"websocket", "downstream":[...], "upstream":[...]}}.
	 * Returns the grant ({@code sessionId}, {@code heartbeatMs}, {@code graceMs}, {@code downstream} with each topic's
	 * {@code retain}, {@code upstream} and {@code paths}).  A refusal sets its status (400, 403, 429 with
	 * {@code Retry-After}, or 501) and returns {@code {"code":..., "message":..., "denied":[...]}}.
	 *
	 * @param req The REST request.
	 * @param res The REST response.
	 * @return The grant, or the refusal body.
	 */
	@SuppressWarnings({
		"resource" // False positive: serverBus() returns the shared AutoCloseable bus, which this method borrows but does not own.
	})
	@RestPost(path=SESSION_PATH, summary="Open a message-bus session", swagger=@OpSwagger(ignore=true))
	default JsonMap openBusSession(RestRequest req, RestResponse res) {
		var bus = serverBus();
		var hreq = req.getHttpServletRequest();
		var boundary = bus.policy().boundary();
		if (boundary != null) {
			var r = boundary.check(hreq);
			if (! r.isAllowed()) {
				res.setStatus(r.status());
				return JsonMap.of("code", "bus:refused", "message", r.message());
			}
		}
		try {
			var session = bus.openSession(hreq, sessionBody(req));
			return bus.grant(session, servletBase(req) + BUS_PREFIX, req.getContextPath());
		} catch (BusRefusal e) {
			res.setStatus(e.status());
			e.retryAfterSeconds().ifPresent(x -> res.setHeader("Retry-After", String.valueOf(x)));
			return e.toJson();
		}
	}

	/**
	 * [GET /juneau-bus/stream/{sessionId}] &mdash; streams the session's frames as Server-Sent Events (spec §11.3).
	 *
	 * <p>
	 * Sends the resync, then live frames, until the client goes away or the bus closes the connection (bus closed,
	 * session expired, replaced by another connection, or too slow).  Each frame is one {@code event: bus} with the
	 * frame as {@code data} and its {@code seq}, when it has one, as {@code id}.
	 *
	 * <p>
	 * The stream URL contains the session capability, so an application must not log request URIs for
	 * {@code /juneau-bus/stream/*}.  Juneau's own debug pipeline logs the request path of every completed call when the
	 * resolved logger is loggable at {@code INFO} or finer, and {@code RestSession} logs it at {@code FINE} when a
	 * client disconnects; keep those loggers coarser than {@code INFO} for resources that serve the bus.
	 *
	 * @param id The session's capability id.
	 * @param req The REST request.
	 * @param res The REST response.
	 * @throws IOException If the stream could not be written.
	 */
	@SuppressWarnings({
		"resource" // False positive: serverBus() returns the shared AutoCloseable bus (not owned here) and the fluent sse.sendEvent/flush calls return the same 'sse' already managed by the try-with-resources.
	})
	@RestGet(path=STREAM_PATH, summary="Message-bus SSE stream", swagger=@OpSwagger(ignore=true))
	default void streamBus(@Path("sessionId") String id, RestRequest req, RestResponse res) throws IOException {
		var bus = serverBus();
		var hreq = req.getHttpServletRequest();
		var boundary = bus.policy().boundary();
		if (boundary != null) {
			var r = boundary.check(hreq);
			if (! r.isAllowed()) {
				res.setStatus(r.status());
				return;
			}
		}
		var session = bus.session(id, hreq.getUserPrincipal()).orElse(null);
		if (session == null) {
			res.setStatus(404);
			return;
		}
		var wait = bus.policy().heartbeat();
		var sink = new SseFrameSink(bus.subscribeSse(session), bus.policy().maxQueuedFrames());
		try (var sse = res.sse()) {
			var writer = res.getNegotiatedWriter();
			bus.connect(session, sink);
			BusStream.pump(sink, e -> sse.sendEvent(e).flush(), writer::checkError, wait);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			bus.disconnect(session, sink);
			sink.release();
		}
	}

	/** Parses the POST body; anything but a JSON object, or a body over 64 KB, is a 400. */
	private static JsonMap sessionBody(RestRequest req) throws BusRefusal {
		String text;
		try {
			text = BusStream.readCapped(req.getInputStream());
		} catch (IOException e) {
			throw BusRefusal.badRequest("the session request could not be read");
		}
		try {
			return text.isBlank() ? null : JsonMap.ofString(text, JsonParser.DEFAULT);
		} catch (RuntimeException e) {
			throw BusRefusal.badRequest("the session request must be a JSON object");
		}
	}

	/** The root-relative servlet path (context plus servlet) without a trailing slash. */
	private static String servletBase(RestRequest req) {
		return BusStream.trimBase(req.getUriContext().getRootRelativeServletPath());
	}
}
