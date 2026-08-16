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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams {@code text/event-stream} for two channels — per-step console log, and run/step-status
 * snapshots — registered as one servlet since both live under the same {@code /events/*} URL space, which
 * the servlet container maps exclusively.
 *
 * <p>The console channel replays that step's on-disk log on connect, then tails that step's
 * {@link LogBroadcaster} live. One stream per step, matching the UI's one-console-at-a-time rule. The
 * state channel (trailing segment {@value #STATE_SEGMENT}) sends the run's current snapshot on connect,
 * then tails that version's {@link RunStateBroadcaster} live, so every connected New-Release tab —
 * including a passive second browser — tracks rail status without polling or a page reload.
 *
 * <p>Registered via {@link org.springframework.boot.web.servlet.ServletRegistrationBean} alongside
 * {@code RootRest} — deliberately NOT a Juneau {@code @Rest} resource (no serializer in the way).
 *
 * <p>Mapped at {@code /events/*}; the two trailing path segments are {@code {version}/{stepId}}, where
 * {@code {stepId}} may instead be the literal {@value #STATE_SEGMENT} to select the state channel (no
 * registry step is ever named that).
 */
public class SseLogServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final long HEARTBEAT_MS = 25_000;

	/** Trailing-segment sentinel selecting the run-state channel instead of a per-step console. */
	public static final String STATE_SEGMENT = "state";

	/** SSE comment frame sent when no log line arrives within the heartbeat interval (keeps the connection alive). */
	public static final String HEARTBEAT = ": heartbeat\n\n";

	/** (version, stepId) -> that step's current-RC log Path (from RunStateStore's StepState.logRef). */
	private final transient BiFunction<String, String, Optional<Path>> logPathForStep;
	/** (version, stepId) -> that step's LogBroadcaster. */
	private final transient BiFunction<String, String, Optional<LogBroadcaster>> broadcasterForStep;
	/** version -> that run's current snapshot, as JSON. */
	private final transient Function<String, Optional<String>> initialStateJsonForVersion;
	/** version -> that run's RunStateBroadcaster. */
	private final transient Function<String, Optional<RunStateBroadcaster>> stateBroadcasterForVersion;

	/** Console-only constructor (no state channel); used where a caller has no run-state wiring to offer. */
	public SseLogServlet(BiFunction<String, String, Optional<Path>> logPathForStep,
			BiFunction<String, String, Optional<LogBroadcaster>> broadcasterForStep) {
		this(logPathForStep, broadcasterForStep, version -> Optional.empty(), version -> Optional.empty());
	}

	public SseLogServlet(BiFunction<String, String, Optional<Path>> logPathForStep,
			BiFunction<String, String, Optional<LogBroadcaster>> broadcasterForStep,
			Function<String, Optional<String>> initialStateJsonForVersion,
			Function<String, Optional<RunStateBroadcaster>> stateBroadcasterForVersion) {
		this.logPathForStep = logPathForStep;
		this.broadcasterForStep = broadcasterForStep;
		this.initialStateJsonForVersion = initialStateJsonForVersion;
		this.stateBroadcasterForVersion = stateBroadcasterForVersion;
	}

	/** SSE frame for a (possibly multi-line) payload: each physical line gets its own {@code data:} prefix. */
	public static String sse(String payload) {
		var sb = new StringBuilder();
		for (var line : payload.split("\n", -1))
			sb.append("data: ").append(line).append('\n');
		// The split adds a trailing empty element for a payload ending in \n; normalize to one blank-line terminator.
		return sb.toString().stripTrailing() + "\n\n";
	}

	@Override
	@SuppressWarnings({ "resource" // The servlet container owns the response writer's lifecycle; closing it here would break SSE streaming/tailing.
	})
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		var seg = trailingTwoSegments(req.getPathInfo());
		var version = seg[0];
		var stepId = seg[1];
		resp.setContentType("text/event-stream");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setHeader("Connection", "keep-alive");

		try {
			var out = resp.getWriter();

			if (STATE_SEGMENT.equals(stepId)) {
				streamState(version, out);
				return;
			}

			// 1) Replay this step's on-disk log (handles reload, step reselection, restart, and post-vote
			//    reconnect). Each step's log is dedicated to that step alone, so replay is always
			//    small/bounded, never a whole-RC file.
			var logPath = logPathForStep.apply(version, stepId).orElse(null);
			if (logPath != null && Files.isRegularFile(logPath)) {
				for (var line : Files.readAllLines(logPath))
					out.print(sse(line));
				out.flush();
			}

			// 2) Tail live via that step's broadcaster.
			var bc = broadcasterForStep.apply(version, stepId).orElse(null);
			if (bc == null) {
				out.print(sse("(no active run/step for " + version + "/" + stepId + ")"));
				out.flush();
				return;
			}
			tail(bc, out);
		} catch (IOException e) {
			// SSE is one-way and best-effort: a broken pipe (client navigated away) or an unreadable log
			// file just ends the stream. There's nothing to retry, so close quietly instead of letting the
			// exception escape doGet as a 500.
			if (!resp.isCommitted())
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	/**
	 * The state channel: send {@code version}'s current snapshot on connect (if a run is persisted for
	 * it), then tail that version's {@link RunStateBroadcaster} live — mirrors the console channel's
	 * replay-then-tail shape above, minus the on-disk replay (a snapshot has no history to replay).
	 */
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

	/** Tails {@code bc} to {@code out}, emitting a heartbeat when idle, until the client disconnects. */
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

	/** Splits {@code /{version}/{stepId}} into {@code [version, stepId]}; either may be empty if absent. */
	private static String[] trailingTwoSegments(String pathInfo) {
		if (pathInfo == null || pathInfo.isBlank())
			return new String[] { "", "" };
		var p = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
		var parts = p.split("/", 2);
		return parts.length == 2 ? parts : new String[] { parts[0], "" };
	}
}
