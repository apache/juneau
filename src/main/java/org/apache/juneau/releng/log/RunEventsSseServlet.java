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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@code GET /events/runs}: the Release page's server-sent events from {@link EventBroadcaster}.
 */
public class RunEventsSseServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final long HEARTBEAT_MS = 25_000;

	/** An SSE comment line that keeps idle connections open. */
	public static final String HEARTBEAT = ": heartbeat\n\n";

	private final transient EventBroadcaster events;

	/**
	 * Constructor.
	 *
	 * @param events The bus to stream.
	 */
	public RunEventsSseServlet(EventBroadcaster events) {
		this.events = events;
	}

	/**
	 * Frames a payload as one SSE event, a {@code data:} line per payload line.
	 *
	 * @param payload The payload.
	 * @return The framed event.
	 */
	public static String sse(String payload) {
		var sb = new StringBuilder();
		for (var line : payload.split("\n", -1))
			sb.append("data: ").append(line).append('\n');
		return sb.toString().stripTrailing() + "\n\n";
	}

	@Override
	@SuppressWarnings({
		"resource" // The servlet container owns the response writer's lifecycle; closing it here would break SSE streaming.
	})
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		if (! "/runs".equals(req.getPathInfo())) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		resp.setContentType("text/event-stream");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setHeader("Connection", "keep-alive");
		try {
			resp.flushBuffer(); // commit the headers now so EventSource.onopen fires before the first event
			tail(resp.getWriter());
		} catch (IOException e) {
			// Best-effort and one-way: a client that went away just ends the stream.
			if (! resp.isCommitted())
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	private void tail(PrintWriter out) {
		var queue = new LinkedBlockingQueue<String>();
		var subscription = events.subscribe(queue::offer);
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
					running = ! out.checkError(); // client disconnected
				}
			}
		} finally {
			try {
				subscription.close();
			} catch (Exception ignored) {
				/* best-effort unsubscribe */ }
		}
	}
}
