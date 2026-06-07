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
package org.apache.juneau.examples.rest;

import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.sse.*;

/**
 * Sample REST resource that emits a stream of Server-Sent Events.
 *
 * <p>
 * The {@link #stream()} endpoint returns a lazy {@link Iterable} of {@link SseEvent} objects whose
 * iterator sleeps {@value #SLEEP_MILLIS} milliseconds between calls to {@code next()}. Combined with the
 * per-event {@code Writer.flush()} performed by {@link SseSerializer}, this allows the per-event
 * arrival timing to be observed with a streaming HTTP client such as {@code curl -N}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	$ curl -N -H 'Accept: text/event-stream' http://localhost:10000/sseDemo/stream
 * 	event: tick-1
 * 	data: 2026-05-20T11:08:00.123Z
 *
 * 	event: tick-2
 * 	data: 2026-05-20T11:08:00.624Z
 * 	...
 * </p>
 */
@Rest(
	path = "/sseDemo",
	title = "SSE demo",
	description = "Emits a stream of Server-Sent Events with a short delay between events."
)
public class SseDemoResource extends BasicRestResource {

	private static final int EVENT_COUNT = 10;
	private static final long SLEEP_MILLIS = 500L;

	/**
	 * Streams {@value #EVENT_COUNT} SSE events with a {@value #SLEEP_MILLIS}-ms delay between
	 * successive events. Each event carries a UTC timestamp in its {@code data:} field so that
	 * per-event flush behavior is observable from the client's wall clock.
	 *
	 * @return A lazy iterable of {@link SseEvent} objects.
	 */
	@RestGet(path = "/stream", serializers = SseSerializer.class)
	public Iterable<SseEvent> stream() {
		return () -> new Iterator<>() {
			private int i = 0;

			@Override
			public boolean hasNext() {
				return i < EVENT_COUNT;
			}

			@Override
			public SseEvent next() {
				if (i >= EVENT_COUNT)
					throw new NoSuchElementException();
				if (i > 0) {
					try {
						Thread.sleep(SLEEP_MILLIS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException("Interrupted while sleeping between SSE events", e);
					}
				}
				i++;
				var ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
				return new SseEvent("tick-" + i, ts).setId(String.valueOf(i));
			}
		};
	}

	/**
	 * Demonstrates server-side broadcaster fan-out with heartbeat comments.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @param broadcaster The broadcaster bean.
	 * @throws IOException If the response write fails.
	 */
	@RestGet(path = "/broadcast", serializers = SseSerializer.class)
	@SuppressWarnings({
		"resource" // Scheduler is explicitly shutdown in finally; subscription/SSE support are closed via try-with-resources.
	})
	public void broadcast(RestRequest req, RestResponse res, SseBroadcaster broadcaster) throws IOException {
		var id = opt(req.getHttpServletRequest().getRequestId()).orElse(UUID.randomUUID().toString());
		var counter = new AtomicInteger();
		var scheduler = Executors.newSingleThreadScheduledExecutor();
		ScheduledFuture<?> task = null;
		try (var subscription = broadcaster.subscribe(id); var sse = res.sse().heartbeat(Duration.ofSeconds(15))) {
			task = scheduler.scheduleAtFixedRate(() -> {
				var i = counter.incrementAndGet();
				var ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
				broadcaster.publish(new SseEvent("tick-" + i, ts).setId(String.valueOf(i)));
				if (i >= EVENT_COUNT)
					subscription.close();
			}, 0, SLEEP_MILLIS, TimeUnit.MILLISECONDS);
			sse.sendFrom(subscription);
		} finally {
			if (task != null)
				task.cancel(true);
			scheduler.shutdownNow();
		}
	}
}
