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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.sse.*;

/**
 * Scheduled SSE heartbeat.
 */
public class SseHeartbeat implements Runnable, AutoCloseable {

	/**
	 * Starts a heartbeat.
	 *
	 * @param scheduler The scheduler. Must not be <jk>null</jk>.
	 * @param writer The writer. Must not be <jk>null</jk>.
	 * @param interval The heartbeat interval. Must not be <jk>null</jk>.
	 * @return The heartbeat handle.
	 */
	@SuppressWarnings({
		"resource" // Returned heartbeat handle is caller-owned and must be closed by the caller.
	})
	public static SseHeartbeat start(ScheduledExecutorService scheduler, Writer writer, Duration interval) {
		var heartbeat = new SseHeartbeat(writer);
		var i = assertArgNotNull("interval", interval).toMillis();
		assertArg(i > 0, "interval must be > 0.");
		heartbeat.future = scheduler.scheduleAtFixedRate(heartbeat, i, i, TimeUnit.MILLISECONDS);
		return heartbeat;
	}

	@SuppressWarnings({
		"resource" // Writer is borrowed from response lifecycle and not owned by heartbeat.
	})
	private final Writer writer;
	private final AtomicBoolean closed;
	private ScheduledFuture<?> future;

	/**
	 * Constructor.
	 *
	 * @param writer The writer. Must not be <jk>null</jk>.
	 */
	public SseHeartbeat(Writer writer) {
		this.writer = assertArgNotNull("writer", writer);
		closed = new AtomicBoolean();
	}

	@Override /* Runnable */
	public void run() {
		if (closed.get())
			return;
		try {
			SseSerializer.writeComment(writer, "ping");
		} catch (IOException e) {
			close();
		}
	}

	@Override /* AutoCloseable */
	public void close() {
		if (closed.compareAndSet(false, true) && future != null)
			future.cancel(false);
	}
}
