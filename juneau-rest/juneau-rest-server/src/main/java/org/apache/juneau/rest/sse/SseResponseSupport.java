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
package org.apache.juneau.rest.sse;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.sse.*;

/**
 * Fluent SSE response helper.
 */
public class SseResponseSupport implements AutoCloseable {

	private final RestResponse response;
	@SuppressWarnings({
		"resource" // Writer is response-owned and intentionally not closed by this wrapper.
	})
	private final FinishablePrintWriter writer;
	@SuppressWarnings({
		"resource" // Scheduler is BeanStore-managed and shared; this wrapper must not close it.
	})
	private final ScheduledExecutorService scheduler;
	@SuppressWarnings({
		"resource" // Heartbeat lifecycle is controlled by this wrapper and closed in close()/heartbeat().
	})
	private SseHeartbeat heartbeat;

	/**
	 * Constructor.
	 *
	 * @param response The REST response.
	 * @throws IOException If the writer could not be created.
	 */
	@SuppressWarnings({
		"resource" // BeanStore returns container-managed scheduler reference; this wrapper borrows it.
	})
	public SseResponseSupport(RestResponse response) throws IOException {
		this.response = assertArgNotNull("response", response);
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
		SseSerializer.DEFAULT.serialize(event, writer);
		return this;
	}

	/**
	 * Sends an SSE event from name+data values.
	 *
	 * @param name The event name.
	 * @param data The event data.
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	@SuppressWarnings({
		"resource" // SSE stream resource lifecycle is managed by the servlet container.
	})
	public SseResponseSupport sendEvent(String name, Object data) throws IOException {
		return sendEvent(new SseEvent(name, data == null ? null : data.toString()));
	}

	/**
	 * Sends a heartbeat/comment line.
	 *
	 * @param value The comment value.
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
	 * @param subscription The subscription.
	 * @return This object.
	 * @throws IOException If an I/O error occurred.
	 */
	@SuppressWarnings({
		"resource" // Subscription lifecycle is handled in finally and by caller contract.
	})
	public SseResponseSupport sendFrom(SseSubscription subscription) throws IOException {
		assertArgNotNull("subscription", subscription);
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
