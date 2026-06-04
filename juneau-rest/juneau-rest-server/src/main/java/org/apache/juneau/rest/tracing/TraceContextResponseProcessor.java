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
package org.apache.juneau.rest.tracing;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.util.logging.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.processor.*;

/**
 * Writes the W3C Trace Context response headers ({@code traceparent} and, when present,
 * {@code tracestate}) so a client calling this server can read the resulting trace identifiers back
 * off the response.
 *
 * <h5 class='topic'>How it works</h5>
 *
 * <p>
 * A {@link TracerHook} bridge (e.g. the OpenTelemetry bridge in {@code juneau-rest-server-otel})
 * renders the W3C {@code traceparent} / {@code tracestate} values from the <i>server-started</i> span
 * context at {@link TracerHook#startSpan(RestRequest) span-start} time &mdash; the only point where
 * that context is reliably active &mdash; and stashes them as the request attributes
 * {@link #ATTR_TRACEPARENT} / {@link #ATTR_TRACESTATE}. This processor runs later, during response
 * rendering (after the span scope has already closed), reads those stashed strings, and writes them
 * as response headers.
 *
 * <p>
 * This capture-at-span-start / write-at-response-time split is deliberate: the framework opens and
 * closes the tracing scope around the {@code @RestOp} handler in
 * {@link RestOpInvoker#invokeOp(RestOpSession)}, which completes <i>before</i> response processors
 * run, so {@code Span.current()} is no longer valid here. Keeping the rendered values on the request
 * also keeps this processor free of any OpenTelemetry (or other tracer) dependency &mdash; it works
 * with any {@code TracerHook} bridge that populates the two attributes.
 *
 * <h5 class='topic'>Off-by-default behavior</h5>
 *
 * <p>
 * The first action in {@link #process(RestOpSession)} is a single request-attribute read. When no
 * non-no-op {@code TracerHook} ran for the request, no {@link #ATTR_TRACEPARENT} attribute is present
 * and the processor returns {@link #NEXT} immediately with no allocations &mdash; matching the
 * off-by-default, zero-cost contract of the observability hooks. Registration of this processor is
 * itself gated on {@code RestContext.responseTraceparent} (default {@code true}); setting it to
 * {@code false} keeps the processor out of the chain entirely.
 *
 * <h5 class='topic'>Already-committed responses</h5>
 *
 * <p>
 * If the response has already been committed (for example a streaming / SSE handler that flushed its
 * headers before returning), the headers can no longer be set. In that case the processor logs a
 * {@link Level#FINE FINE} message and skips the write &mdash; it never throws.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TracerHook}
 * 	<li class='link'><a class="doclink" href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class TraceContextResponseProcessor implements ResponseProcessor {

	/**
	 * Request-attribute key under which a {@link TracerHook} bridge stashes the rendered W3C
	 * {@code traceparent} value (e.g. {@code 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01})
	 * for this processor to emit as a response header.
	 */
	public static final String ATTR_TRACEPARENT = "juneau.traceparent";

	/**
	 * Request-attribute key under which a {@link TracerHook} bridge stashes the rendered W3C
	 * {@code tracestate} value (a comma-separated {@code key=value} list). Only set when the active
	 * trace state is non-empty.
	 */
	public static final String ATTR_TRACESTATE = "juneau.tracestate";

	private static final String HEADER_TRACEPARENT = "traceparent";
	private static final String HEADER_TRACESTATE = "tracestate";

	private static final Logger LOG = Logger.getLogger(TraceContextResponseProcessor.class.getName());

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var req = opSession.getRequest();

		// NoOp short-circuit: no active tracer stashed a trace context for this request.
		var traceparent = req.getAttribute(ATTR_TRACEPARENT).as(String.class).orElse(null);
		if (isEmpty(traceparent))
			return NEXT;

		var res = opSession.getResponse();

		// Headers already flushed (e.g. streaming / SSE response) — body was also written; chain is done.
		if (res.getHttpServletResponse().isCommitted()) {
			LOG.log(Level.FINE, () -> "Response already committed; skipping traceparent/tracestate header injection.");
			return FINISHED;
		}

		res.setHeader(HEADER_TRACEPARENT, traceparent);

		var tracestate = req.getAttribute(ATTR_TRACESTATE).as(String.class).orElse(null);
		if (tracestate != null && ! tracestate.isEmpty())
			res.setHeader(HEADER_TRACESTATE, tracestate);

		// Headers only — let the downstream processors render the body.
		return NEXT;
	}
}
