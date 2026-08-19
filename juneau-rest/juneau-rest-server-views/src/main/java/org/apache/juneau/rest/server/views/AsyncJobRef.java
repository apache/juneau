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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.commons.bean.*;

/**
 * The "job accepted" envelope a row action's submit returns <b>instead of</b> a terminal {@link ActionResult} when
 * the write is asynchronous (design doc §6.3; {@code TODO-425}).
 *
 * <h5 class='section'>Why this is not a new {@code RowAction} wire field</h5>
 * <p>
 * Whether an action is asynchronous is a property of the <b>response</b>, not of the declared action: the same
 * {@link RowAction} POST returns either a terminal {@link ActionResult} (synchronous) or this pointer (asynchronous).
 * So the {@code RowAction} wire schema is untouched &mdash; no new field, no fail-loud contract bump &mdash; and the
 * {@code juneau-views.js} runtime routes to the streaming path purely by recognizing a {@link #streamUrl} in the 2xx
 * body.  It carries no data to render, only capability pointers, so it needs no version handshake of its own.
 *
 * <h5 class='section'>The URLs are capabilities</h5>
 * <p>
 * {@link #streamUrl} points at the SSE progress stream and {@link #cancelUrl} at the cancel endpoint; both embed the
 * job's unguessable {@value AsyncJobRegistry#CAPABILITY_BITS}-bit id (see {@link AsyncJob}), which is the whole of the
 * access control (HIGH-4).  The browser {@code EventSource} that consumes {@link #streamUrl} cannot set a CSRF header,
 * which is precisely why the id must be unguessable rather than CSRF-gated.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AsyncJobsMixin}
 * 	<li class='jc'>{@link AsyncJob}
 * 	<li class='jc'>{@link ActionResult}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="jobId,streamUrl,cancelUrl")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class AsyncJobRef {

	/** The job's unguessable capability id. */
	public String jobId;

	/** The URL of the SSE progress stream (a browser {@code EventSource} target); embeds {@link #jobId}. */
	public String streamUrl;

	/** The URL of the cancel endpoint (a non-safe POST); embeds {@link #jobId}. */
	public String cancelUrl;

	/**
	 * Builds a reference to the given job using {@code servlet:}-relative URLs against the {@link AsyncJobsMixin}
	 * mount &mdash; the same {@code servlet:} convention {@code ViewsMixin} uses for its asset URLs, resolved by
	 * Juneau's serializer against the current request.
	 *
	 * @param job The started job.  Must not be <jk>null</jk>.
	 * @return A new reference carrying the job's id and its {@code servlet:}-relative stream + cancel URLs.
	 */
	public static AsyncJobRef of(AsyncJob job) {
		assertArgNotNull("job", job);
		var r = new AsyncJobRef();
		r.jobId = job.id();
		r.streamUrl = "servlet:" + AsyncJobsMixin.streamPath(job.id());
		r.cancelUrl = "servlet:" + AsyncJobsMixin.cancelPath(job.id());
		return r;
	}

	/**
	 * Builds a reference carrying explicit stream/cancel URLs (for a consumer that resolves URLs itself, e.g. a
	 * template-rendered surface downstream of the {@code servlet:} rewrite).
	 *
	 * @param jobId The job's capability id.  Must not be <jk>null</jk> or blank.
	 * @param streamUrl The SSE stream URL.  Must not be <jk>null</jk> or blank.
	 * @param cancelUrl The cancel URL.  Must not be <jk>null</jk> or blank.
	 * @return A new reference.
	 */
	public static AsyncJobRef of(String jobId, String streamUrl, String cancelUrl) {
		var r = new AsyncJobRef();
		r.jobId = jobId;
		r.streamUrl = streamUrl;
		r.cancelUrl = cancelUrl;
		return r;
	}
}
