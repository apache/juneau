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
package org.apache.juneau.rest.server.views;

import java.io.*;
import java.time.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin interface that exposes the async-job SSE progress stream and cancel endpoint on any Juneau REST resource
 * (design doc §6.3).
 *
 * <p>
 * An implementing resource supplies its {@link AsyncJobRegistry} via {@link #asyncJobRegistry()} &mdash; typically a
 * single per-process instance &mdash; and gets two endpoints for free:
 * <ul class='spaced-list'>
 * 	<li><b>{@code GET /juneau-jobs/{jobId}/stream}</b> &mdash; a held-open {@code text/event-stream} of the job's
 * 		progress, terminated by one {@code result} event carrying the terminal {@link ActionResult}.
 * 	<li><b>{@code POST /juneau-jobs/{jobId}/cancel}</b> &mdash; cancels the job and returns its terminal
 * 		{@link ActionResult} (a {@code cancelled} / {@code cancelled-after-effect} outcome).
 * </ul>
 *
 * <h5 class='section'>Authorization: the stream URL is the capability (HIGH-4)</h5>
 * <p>
 * The stream is a held-open <b>GET</b>, so {@link org.apache.juneau.rest.server.filter.LoopbackBoundary} applies only
 * its <b>Host</b> check (DNS-rebinding stays covered on every request); the Origin/CSRF/JSON checks apply to non-safe
 * methods only.  A browser {@code EventSource} cannot set an {@code X-Csrf-Token} header and cookies are unsound on a
 * loopback port, so CSRF-on-GET is not an available control.  Access is therefore gated by the <b>unguessability of
 * the {@code jobId}</b> path segment &mdash; an {@value AsyncJobRegistry#CAPABILITY_BITS}-bit
 * {@link java.security.SecureRandom} capability &mdash; and nothing else.  An unknown id answers {@code 404} (a miss
 * is indistinguishable, so the id space cannot be enumerated); a job already at its subscriber cap answers
 * {@code 429}.
 *
 * <h5 class='section'>Server-side hard limits</h5>
 * <p>
 * Every limit is enforced in {@link AsyncJob} / {@link AsyncJobRegistry}, not here and not on the client: the hard
 * timeout, the per-job output cap, the concurrent-job cap and the per-job subscriber cap.  The stream loop simply
 * drains the job until it is terminal, which the timeout guarantees will happen.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> AsyncJobsMixin {
 * 		<jk>private final</jk> AsyncJobRegistry <jf>jobs</jf> = <jk>new</jk> AsyncJobRegistry();
 * 		<ja>@Override</ja> <jk>public</jk> AsyncJobRegistry asyncJobRegistry() { <jk>return</jk> <jf>jobs</jf>; }
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AsyncJobRegistry}
 * 	<li class='jc'>{@link AsyncJob}
 * 	<li class='jc'>{@link AsyncJobRef}
 * </ul>
 *
 * @since 10.0.0
 */
public interface AsyncJobsMixin {

	/** The URL path prefix for the async-job endpoints (relative to the host mount). */
	String JOBS_PREFIX = "/juneau-jobs";

	/** The URL path template of the SSE progress stream. */
	String STREAM_PATH = JOBS_PREFIX + "/{jobId}/stream";

	/** The URL path template of the cancel endpoint. */
	String CANCEL_PATH = JOBS_PREFIX + "/{jobId}/cancel";

	/** How long the stream loop blocks per poll while a job is still running (a liveness tick, not a timeout). */
	Duration STREAM_POLL_INTERVAL = Duration.ofMillis(250);

	/**
	 * Returns the {@link AsyncJobRegistry} backing this resource's async jobs.
	 *
	 * @return The registry.  Never <jk>null</jk>.
	 */
	AsyncJobRegistry asyncJobRegistry();

	/**
	 * [GET /juneau-jobs/{jobId}/stream] &mdash; stream a job's progress as Server-Sent Events.
	 *
	 * <p>
	 * Replays any buffered progress (so a page reload re-attaches, per Q3), streams live progress as {@code progress}
	 * events, then sends exactly one {@code result} event carrying the terminal {@link ActionResult} as JSON and
	 * closes.  Access is gated solely by the unguessable {@code jobId} (see the interface javadoc): an unknown id is a
	 * {@code 404}; a job already at its subscriber cap is a {@code 429}.
	 *
	 * @param jobId The job's unguessable capability id.
	 * @param res The REST response.
	 * @throws IOException If the stream could not be written.
	 */
	@SuppressWarnings({
		"resource" // False positive: asyncJobRegistry() returns the shared AutoCloseable registry (not owned here) and the fluent sse.sendEvent/flush calls return the same 'sse' already managed by the try-with-resources.
	})
	@RestGet(path=STREAM_PATH, summary="Async job SSE progress stream", swagger=@OpSwagger(ignore=true))
	default void streamJob(@Path("jobId") String jobId, RestResponse res) throws IOException {
		var job = asyncJobRegistry().get(jobId).orElse(null);
		if (job == null) {
			res.setStatus(404);
			return;
		}
		if (! job.acquireSubscriber()) {
			res.setStatus(429);
			return;
		}
		try (var sse = res.sse()) {
			var cursor = 0;
			while (true) {
				var update = job.awaitUpdate(cursor, STREAM_POLL_INTERVAL);
				for (var event : update.events()) {
					sse.sendEvent("progress", event);
					cursor++;
				}
				sse.flush();
				if (update.result() != null) {
					sse.sendEvent("result", Json.of(update.result()));
					sse.flush();
					return;
				}
			}
		} finally {
			job.releaseSubscriber();
		}
	}

	/**
	 * [POST /juneau-jobs/{jobId}/cancel] &mdash; cancel a job and return its terminal result.
	 *
	 * <p>
	 * A non-safe POST, so the boundary applies its full Origin/CSRF/JSON checks.  Cancellation is enforced
	 * server-side (a client cannot be trusted to stop the work); the returned outcome distinguishes {@code cancelled}
	 * from {@code cancelled-after-effect} (Q4).  An unknown id is a {@code 404} carrying a named refusal.
	 *
	 * @param jobId The job's unguessable capability id.
	 * @param res The REST response.
	 * @return The job's terminal {@link ActionResult}.
	 */
	@SuppressWarnings({
		"resource" // False positive: asyncJobRegistry() returns the shared AutoCloseable registry, which this method borrows but does not own.
	})
	@RestPost(path=CANCEL_PATH, summary="Cancel an async job", swagger=@OpSwagger(ignore=true))
	default ActionResult cancelJob(@Path("jobId") String jobId, RestResponse res) {
		var job = asyncJobRegistry().get(jobId).orElse(null);
		if (job == null) {
			res.setStatus(404);
			return ActionResult.refusal("app:unknown-job");
		}
		job.cancel();
		return job.result();
	}

	/**
	 * Builds the servlet-relative stream path for a job id.
	 *
	 * @param jobId The job's capability id.
	 * @return The stream path, e.g. {@code /juneau-jobs/<id>/stream}.
	 */
	static String streamPath(String jobId) {
		return JOBS_PREFIX + "/" + jobId + "/stream";
	}

	/**
	 * Builds the servlet-relative cancel path for a job id.
	 *
	 * @param jobId The job's capability id.
	 * @return The cancel path, e.g. {@code /juneau-jobs/<id>/cancel}.
	 */
	static String cancelPath(String jobId) {
		return JOBS_PREFIX + "/" + jobId + "/cancel";
	}
}
