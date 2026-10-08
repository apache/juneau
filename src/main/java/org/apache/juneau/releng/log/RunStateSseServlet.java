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

package org.apache.juneau.releng.log;

import static org.apache.juneau.commons.utils.Shorts.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams the run/step-status snapshots as {@code text/event-stream}: on connect the run's current snapshot, then
 * every later snapshot from that version's {@link RunStateBroadcaster}. Step console output is not streamed here; the
 * console-output region polls it from {@code ReleaseRunRest}.
 *
 * <p>Mapped at {@code /events/*}; the two trailing path segments are {@code {version}/state}.
 */
public class RunStateSseServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final long HEARTBEAT_MS = 25_000;

	/**
	 * The trailing path segment that selects the state channel.
	 */
	public static final String STATE_SEGMENT = "state";

	/**
	 * An SSE comment frame, sent when nothing was published for {@value #HEARTBEAT_MS} ms, so proxies keep the
	 * connection open and a dead client is noticed by the failed write.
	 */
	public static final String HEARTBEAT = ": heartbeat\n\n";

	private final transient Function<String, Optional<String>> initialStateJsonForVersion;
	private final transient Function<String, Optional<RunStateBroadcaster>> stateBroadcasterForVersion;

	/**
	 * Wires the two lookups the state channel needs.
	 *
	 * @param initialStateJsonForVersion {@code version} to that run's current snapshot JSON, or empty when there is
	 * 	no persisted run.
	 * @param stateBroadcasterForVersion {@code version} to that run's live broadcaster, or empty when there is no
	 * 	persisted run.
	 */
	public RunStateSseServlet(Function<String, Optional<String>> initialStateJsonForVersion,
			Function<String, Optional<RunStateBroadcaster>> stateBroadcasterForVersion) {
		this.initialStateJsonForVersion = initialStateJsonForVersion;
		this.stateBroadcasterForVersion = stateBroadcasterForVersion;
	}

	/**
	 * Frames {@code payload} as one SSE event, one {@code data:} line per payload line.
	 *
	 * @param payload The event payload.
	 * @return The frame, ending in a blank line.
	 */
	public static String sse(String payload) {
		var sb = new StringBuilder();
		for (var line : payload.split("\n", -1))
			sb.append("data: ").append(line).append('\n');
		// The split adds a trailing empty element for a payload ending in \n; normalize to one blank-line terminator.
		return sb.toString().stripTrailing() + "\n\n";
	}

	@Override
	@SuppressWarnings({
		"resource" // The servlet container owns the response writer's lifecycle; closing it here would break SSE streaming/tailing.
	})
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		var seg = trailingTwoSegments(req.getPathInfo());
		if (! STATE_SEGMENT.equals(seg[1])) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		resp.setContentType("text/event-stream");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setHeader("Connection", "keep-alive");

		try {
			streamState(seg[0], resp.getWriter());
		} catch (IOException e) {
			// SSE is one-way and best-effort: a broken pipe (client navigated away) just ends the stream.
			// There's nothing to retry, so close quietly instead of letting the exception escape doGet as a 500.
			if (! resp.isCommitted())
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	private void streamState(String version, PrintWriter out) {
		var initial = initialStateJsonForVersion.apply(version).orElse(null);
		if (initial != null) {
			out.print(sse(initial));
			out.flush();
		}
		var bc = stateBroadcasterForVersion.apply(version).orElse(null);
		if (bc == null) {
			out.print(sse("(no active run for " + version + ")"));
			out.flush();
			return;
		}
		tail(bc, out);
	}

	private void tail(Broadcaster bc, PrintWriter out) {
		var queue = new LinkedBlockingQueue<String>();
		var subscription = bc.subscribe(queue::offer);
		try {
			var running = true;
			while (running) {
				String line = null;
				try {
					line = queue.poll(HEARTBEAT_MS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					running = false;
				}
				if (running) {
					out.print(line == null ? HEARTBEAT : sse(line));
					out.flush();
					running = !out.checkError(); // client disconnected
				}
			}
		} finally {
			// AutoCloseable.close() declares `throws Exception`; this subscription's impl never actually
			// throws, so swallow defensively rather than widen the method's checked-exception signature.
			try {
				subscription.close();
			} catch (Exception ignored) {
				/* best-effort unsubscribe */ }
		}
	}

	/**
	 * Splits {@code /{version}/{channel}} into {@code [version, channel]}; either may be empty if absent.
	 */
	private static String[] trailingTwoSegments(String pathInfo) {
		if (ib(pathInfo))
			return new String[] { "", "" };
		var p = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
		var parts = p.split("/", 2);
		return parts.length == 2 ? parts : new String[] { parts[0], "" };
	}
}
