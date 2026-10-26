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
package org.apache.juneau.console.views;

import java.io.*;
import java.time.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin interface that exposes the async-job SSE progress stream and cancel endpoint on any Juneau REST resource.
 *
 * <p>
 * An implementing resource supplies its {@link AsyncJobRegistry} via {@link #asyncJobRegistry()} &mdash; typically a
 * single per-process instance &mdash; and gets two endpoints for free:
 * <ul class='spaced-list'>
 * 	<li><b>{@code GET /juneau-jobs/{jobId}/stream}</b> &mdash; a held-open {@code text/event-stream} of the job's
 * 		progress, terminated by one {@code result} event carrying the terminal {@link ActionResult}.  Each
 * 		{@code progress} event carries an SSE id; a fresh attach is live-only, and a {@code Last-Event-ID} header or
 * 		{@code lastEventId} query parameter resumes from a position (see {@link #streamJob(String, RestRequest, RestResponse)}).
 * 	<li><b>{@code POST /juneau-jobs/{jobId}/cancel}</b> &mdash; cancels the job and returns its terminal
 * 		{@link ActionResult} (a {@code cancelled} / {@code cancelled-after-effect} outcome).
 * </ul>
 *
 * <h5 class='section'>Authorization: the stream URL is the capability</h5>
 * <p>
 * The stream is a held-open <b>GET</b>, so {@link org.apache.juneau.rest.server.filter.LoopbackBoundary} applies only
 * its <b>Host</b> check (DNS-rebinding stays covered on every request); the Origin/CSRF/JSON checks apply to non-safe
 * methods only.  A browser {@code EventSource} cannot set an {@code X-Csrf-Token} header and cookies are unsound on a
 * loopback port, so CSRF-on-GET is not an available control.  Access is therefore gated by the <b>unguessability of
 * the {@code jobId}</b> path segment &mdash; an {@value AsyncJobRegistry#CAPABILITY_BITS}-bit
 * {@link java.security.SecureRandom} capability &mdash; and nothing else.  An unknown id answers {@code 404} (a miss
 * is indistinguishable, so the id space cannot be enumerated); a job already at its subscriber cap answers
 * {@code 429}.  A resume value grants nothing: it only picks a position inside a job the caller already holds the
 * capability for, and it is checked only after the {@code 404}, so a bad value cannot reveal whether a job exists.
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
@SuppressWarnings({
	"resource" // asyncJobRegistry() returns an AutoCloseable registry owned and closed by the implementing resource, not by these endpoints
})
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
	 * Streams progress as {@code progress} events, then sends exactly one {@code result} event carrying the terminal
	 * {@link ActionResult} as JSON and closes.  The progress event taken from the job's buffered event {@code i} is
	 * always sent with SSE id {@code i+1}; the {@code result} event has no id, so a browser's last-event-ID stays at
	 * the last progress event.
	 *
	 * <p>
	 * The start position comes from a resume value, in this order:
	 * <ol class='spaced-list'>
	 * 	<li>The {@code Last-Event-ID} header, if non-empty after trimming (a browser's own reconnect re-sends the same
	 * 		URL, query included, and adds this header).
	 * 	<li>Otherwise the {@code lastEventId} query parameter, if non-empty after trimming (an explicit opt-in to the
	 * 		buffered history; {@code lastEventId=0} replays everything).
	 * 	<li>Otherwise none: a fresh attach is <b>live-only</b> and sends only progress produced after it attached.
	 * </ol>
	 * A value {@code k} resumes with buffered event {@code k} (id {@code k+1}).  A malformed value (anything but 1 to 10
	 * ASCII digits after trimming SP/HTAB) or one past the end of the buffer is a {@code 400}.  An ended job still sends
	 * its {@code result} on every attach.
	 *
	 * <p>
	 * Access is gated solely by the unguessable {@code jobId} (see the interface javadoc).  The checks run in this
	 * order: an unknown id is a {@code 404}; a bad resume value is a {@code 400}; a job already at its subscriber cap is
	 * a {@code 429}.  The cursor is fixed before the subscriber slot is taken, so a {@code 400} never holds a slot.
	 *
	 * @param jobId The job's unguessable capability id.
	 * @param req The REST request (for the resume value).
	 * @param res The REST response.
	 * @throws IOException If the stream could not be written.
	 */
	@RestGet(path=STREAM_PATH, summary="Async job SSE progress stream", swagger=@OpSwagger(ignore=true))
	default void streamJob(@Path("jobId") String jobId, RestRequest req, RestResponse res) throws IOException {
		var job = asyncJobRegistry().get(jobId).orElse(null);
		if (job == null) {
			res.setStatus(404);
			return;
		}
		int cursor;
		try {
			cursor = job.resumeCursor(resumeValue(req));
		} catch (IllegalArgumentException ignored) {
			res.setStatus(400);
			return;
		}
		if (! job.acquireSubscriber()) {
			res.setStatus(429);
			return;
		}
		try (var sse = res.sse()) {
			while (true) {
				var update = job.awaitUpdate(cursor, STREAM_POLL_INTERVAL);
				for (var event : update.events()) {
					cursor++;   // events[i] is sent with id i+1, so after the increment the id is the new cursor.
					sse.sendEvent(new SseEvent("progress", event).setId(Integer.toString(cursor)));
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
	 * Picks the raw resume value for {@link #streamJob(String, RestRequest, RestResponse)}: a non-empty (after
	 * trimming) {@code Last-Event-ID} header, else a non-empty {@code lastEventId} query parameter, else <jk>null</jk>.
	 *
	 * @param req The REST request.
	 * @return The raw value, or <jk>null</jk> if neither source supplied one.
	 */
	private static String resumeValue(RestRequest req) {
		var header = req.getHeaderParam("Last-Event-ID").orElse(null);
		if (header != null && ! AsyncJob.trimOws(header).isEmpty())
			return header;
		var query = req.getQueryParam("lastEventId").orElse(null);
		if (query != null && ! AsyncJob.trimOws(query).isEmpty())
			return query;
		return null;
	}

	/**
	 * [POST /juneau-jobs/{jobId}/cancel] &mdash; cancel a job and return its terminal result.
	 *
	 * <p>
	 * A non-safe POST, so the boundary applies its full Origin/CSRF/JSON checks.  Cancellation is enforced
	 * server-side (a client cannot be trusted to stop the work); the returned outcome distinguishes {@code cancelled}
	 * from {@code cancelled-after-effect}.  An unknown id is a {@code 404} carrying a named refusal.
	 *
	 * @param jobId The job's unguessable capability id.
	 * @param res The REST response.
	 * @return The job's terminal {@link ActionResult}.
	 */
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
