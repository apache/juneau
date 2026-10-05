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
package org.apache.juneau.rest.server.sse;


import java.io.*;
import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.util.*;
import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * Fluent SSE response helper.
 */
@SuppressWarnings({
	"resource" // The heartbeat and negotiated writer are owned by this object/response and are closed in close().
})
public class SseResponseSupport implements AutoCloseable {

	private final RestResponse response;
	private final FinishablePrintWriter writer;
	private final ScheduledExecutorService scheduler;
	private SseHeartbeat heartbeat;

	/**
	 * Constructor.
	 *
	 * @param response The REST response. Must not be <jk>null</jk>.
	 * @throws IOException If the writer could not be created.
	 */
	public SseResponseSupport(RestResponse response) throws IOException {
		this.response = reqnn("response", response);
		this.scheduler = response.getContext().getBeanStore().getBean(ScheduledExecutorService.class).orElse(null);
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("Content-Encoding", "identity");
		writer = response.getNegotiatedWriter();
	}

	/**
	 * Starts periodic heartbeat comments.
	 *
	 * @param interval The heartbeat interval.
	 * @return This object.
	 */
	public SseResponseSupport heartbeat(Duration interval) {
		if (scheduler != null) {
			if (heartbeat != null)
				heartbeat.close();
			heartbeat = SseHeartbeat.start(scheduler, writer, interval);
		}
		return this;
	}

	/**
	 * Sends an SSE event.
	 *
	 * @param event The event.
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	public SseResponseSupport sendEvent(SseEvent event) throws IOException {
		Sse.DEFAULT.write(event, writer);
		return this;
	}

	/**
	 * Sends an SSE event from name+data values.
	 *
	 * @param name The event name. Can be <jk>null</jk> (dispatches as the default event type).
	 * @param data The event data. Can be <jk>null</jk> (no data lines emitted).
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	public SseResponseSupport sendEvent(String name, Object data) throws IOException {
		return sendEvent(new SseEvent(name, data == null ? null : data.toString()));
	}

	/**
	 * Sends a heartbeat/comment line.
	 *
	 * @param value The comment value. Can be <jk>null</jk> (treated as an empty comment).
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	public SseResponseSupport comment(String value) throws IOException {
		SseSerializer.writeComment(writer, value);
		return this;
	}

	/**
	 * Flushes pending output.
	 *
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	public SseResponseSupport flush() throws IOException {
		writer.flush();
		response.flushBuffer();
		return this;
	}

	/**
	 * Drains a subscription until disconnect or interruption.
	 *
	 * @param subscription The subscription. Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	public SseResponseSupport sendFrom(SseSubscription subscription) throws IOException {
		reqnn("subscription", subscription);
		try {
			while (! subscription.isClosed()) {
				sendEvent(subscription.take());
				flush();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			subscription.close();
		}
		return this;
	}

	@Override /* AutoCloseable */
	public void close() {
		if (heartbeat != null)
			heartbeat.close();
	}
}
