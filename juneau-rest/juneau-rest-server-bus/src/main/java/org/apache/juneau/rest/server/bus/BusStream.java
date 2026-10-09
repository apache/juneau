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
import java.nio.charset.*;
import java.time.*;
import java.util.function.*;

import org.apache.juneau.marshall.sse.*;

/**
 * Helpers for {@link BusEventsMixin}, kept out of the interface so they can be tested directly.
 */
final class BusStream {

	/** The most bytes a session POST body may carry. */
	static final int MAX_SESSION_BODY_BYTES = 64 * 1024;

	/** Writes one event and flushes it. */
	@FunctionalInterface
	interface EventWriter {
		void write(SseEvent event) throws IOException;
	}

	private BusStream() {}

	/**
	 * Drains the sink onto the response until the sink ends or the client goes away.
	 *
	 * @param sink The stream's sink.
	 * @param out Writes and flushes one event.
	 * @param clientGone Checked after every write; <jk>true</jk> ends the loop (a {@code PrintWriter} reports write
	 * 	failures only through {@code checkError()}).
	 * @param wait How long to wait for the next event before checking again.
	 * @throws IOException If an event could not be written.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	static void pump(SseFrameSink sink, EventWriter out, BooleanSupplier clientGone, Duration wait) throws IOException, InterruptedException {
		while (true) {
			var event = sink.poll(wait);
			if (event == null) {
				if (sink.isDone())
					return;
				continue;
			}
			out.write(event);
			if (clientGone.getAsBoolean())
				return;
		}
	}

	/**
	 * Reads a request body of at most {@link #MAX_SESSION_BODY_BYTES} bytes.
	 *
	 * @param in The body.
	 * @return The body as UTF-8 text.
	 * @throws IOException If the body could not be read.
	 * @throws BusRefusal 400 when the body is larger than the cap.
	 */
	static String readCapped(InputStream in) throws IOException, BusRefusal {
		var bytes = in.readNBytes(MAX_SESSION_BODY_BYTES + 1);
		if (bytes.length > MAX_SESSION_BODY_BYTES)
			throw BusRefusal.badRequest("the session request is larger than " + MAX_SESSION_BODY_BYTES + " bytes");
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * The root-relative servlet path without a trailing slash.
	 *
	 * @param path The root-relative servlet path.  Can be <jk>null</jk>.
	 * @return The path, or {@code ""} for a root servlet in a root context.
	 */
	static String trimBase(String path) {
		return path == null || path.equals("/") ? "" : path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}
}
