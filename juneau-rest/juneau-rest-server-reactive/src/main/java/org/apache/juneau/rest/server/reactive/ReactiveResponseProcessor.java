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
package org.apache.juneau.rest.server.reactive;

import static jakarta.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Response processor that bridges <a class="doclink" href="https://www.reactive-streams.org/">Reactive Streams</a>
 * return values from {@code @RestOp} handlers to Juneau's response pipeline.
 *
 * <h5 class='topic'>Activation (opt-in)</h5>
 * <p>
 * This processor ships in the opt-in {@code juneau-rest-server-reactive} module and is <b>not</b> wired
 * into {@code DefaultConfig}. A bare {@code juneau-rest-server} has zero reactive behavior. When the
 * {@code juneau-rest-server-reactive} jar is on the classpath, its
 * {@code META-INF/services/org.apache.juneau.rest.server.processor.ResponseProcessor} provider file is discovered
 * by {@code RestContext} via {@link java.util.ServiceLoader} and this processor is front-loaded ahead of
 * {@code AsyncResponseProcessor} in the chain &mdash; no {@code @Rest(responseProcessors=...)} entry required.
 *
 * <h5 class='topic'>What it handles</h5>
 * <p>
 * This is the single, shared spine for all reactive return-type support. It natively understands the
 * JDK type {@link java.util.concurrent.Flow.Publisher Flow.Publisher&lt;T&gt;} (no external dependency
 * required) and, through registered {@link ReactiveStreamsAdapter} providers, any third-party reactive
 * type the opt-in {@code juneau-rest-server-reactive-reactor} module adapts to it (Project Reactor
 * {@code Mono} / {@code Flux}, RxJava 3 {@code Single} / {@code Maybe} / {@code Flowable} /
 * {@code Observable}, and the Reactive-Streams {@code org.reactivestreams.Publisher}).
 *
 * <p>
 * Single-value reactive types (e.g. {@code Mono}, {@code Single}) are adapted to a {@link CompletionStage}
 * and collapse onto the existing {@link AsyncResponseProcessor} async path, inheriting its timeout,
 * completion-executor ({@code @Rest(asyncCompletionExecutor)}), and MDC bridging behavior for free.
 *
 * <h5 class='topic'>Response shapes for multi-value streams</h5>
 * <p>
 * A streaming publisher is rendered as one of three shapes, selected by the negotiated response media
 * type (the handler's {@link RestResponse#setContentType(String) Content-Type}, then the request
 * {@code Accept} header):
 * <ul>
 * 	<li><b>SSE</b> ({@code text/event-stream}) &mdash; each element is emitted as a Server-Sent-Events
 * 		frame. {@link SseEvent} elements are written verbatim; any other element type is JSON-encoded
 * 		into the {@code data:} field.
 * 	<li><b>NDJSON</b> ({@code application/x-ndjson}, {@code application/jsonl}) &mdash; each element is
 * 		JSON-encoded on its own line.
 * 	<li><b>Buffer</b> (default, any other media type) &mdash; all elements are collected into a
 * 		{@link java.util.List List} and serialized through the normal serializer chain (e.g. a JSON
 * 		array). The collection is wrapped in a {@link CompletableFuture} and handed to the async path,
 * 		so a slow producer never blocks the request thread.
 * </ul>
 *
 * <h5 class='topic'>Backpressure</h5>
 * <p>
 * Streaming subscribers request one element at a time ({@code request(1)} on subscribe and again after
 * each frame is written and flushed). Because writing to the servlet output stream blocks until the
 * socket accepts the bytes, this gives natural backpressure &mdash; the producer is paced by the
 * client's drain rate and the server-side buffer does not grow without bound. Buffer-shape subscribers
 * request {@link Long#MAX_VALUE} since the collection is bounded by the publisher's own completion.
 *
 * <h5 class='topic'>Threading, executors, and MDC</h5>
 * <p>
 * Buffer-shape responses route through {@link AsyncResponseProcessor} and therefore honor
 * {@code @Rest(asyncCompletionExecutor)} and the SLF4J MDC bridge. Streaming-shape
 * frame writes happen on whichever thread the publisher emits on (the Reactor / RxJava scheduler); this
 * processor does not impose a {@code subscribeOn(...)} so it never fights the library's scheduler model.
 * When MDC propagation is enabled, the request-thread MDC snapshot is reinstalled around each
 * {@code onNext} / terminal callback via {@link MdcAsyncListener} so log statements emitted while writing
 * a frame see the request's diagnostic context.
 *
 * <h5 class='topic'>Synchronous fallback</h5>
 * <p>
 * In environments where {@link HttpServletRequest#startAsync()} is unsupported (notably Juneau's
 * {@code MockServletRequest}), streaming subscribes synchronously and blocks the request thread until
 * the publisher terminates (bounded by the configured async timeout), writing frames as they arrive.
 * This keeps the unit-test surface working without a real servlet container.
 *
 * @see ReactiveStreamsAdapter
 * @see AsyncResponseProcessor
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeables here are framework-managed and not owned/closed by this class; not a real leak.
})
public class ReactiveResponseProcessor implements ResponseProcessor {

	private static final Logger LOG = Logger.getLogger(ReactiveResponseProcessor.class.getName());

	@SuppressWarnings({
		"java:S3077" // Write-once double-checked-locking cache holding an immutable List.copyOf(...); volatile gives correct safe publication of the one-time assignment and the reference is never mutated afterward.
	})
	private static volatile List<ReactiveStreamsAdapter> adapters;

	private enum Shape { BUFFER, SSE, NDJSON }

	@FunctionalInterface
	private interface FrameEncoder {
		void write(FinishablePrintWriter w, Object element) throws IOException, SerializeException;
	}

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var res = opSession.getResponse();
		var content = res.getContent().orElse(null);

		if (content == null)
			return NEXT;

		if (content instanceof Flow.Publisher<?> pub)
			return dispatch(opSession, pub);

		var a = adapters();
		for (var adapter : a) {
			if (adapter.canAdapt(content)) {
				var adaptation = adapter.adapt(content);
				if (adaptation.isStream())
					return dispatch(opSession, adaptation.stream());
				res.setContent(adaptation.single());
				return RESTART;
			}
		}

		return NEXT;
	}

	private int dispatch(RestOpSession opSession, Flow.Publisher<?> pub) throws IOException {
		var shape = resolveShape(opSession);
		if (shape == Shape.BUFFER)
			return handleBuffer(opSession, pub);
		return handleStream(opSession, pub, shape);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Buffer shape — collect into a List, hand to the async path.
	// -----------------------------------------------------------------------------------------------------------------

	private int handleBuffer(RestOpSession opSession, Flow.Publisher<?> pub) {
		var cf = new CompletableFuture<List<Object>>();
		pub.subscribe(new CollectingSubscriber(cf));
		opSession.getResponse().setContent(cf);
		return RESTART;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Streaming shapes — SSE / NDJSON.
	// -----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S3776", // Streaming setup is branchy by nature (content-type prep + async-vs-sync fallback).
		"java:S1141"  // Nested try/catch cleanly separates startAsync recovery from the sync fallback.
	})
	private int handleStream(RestOpSession opSession, Flow.Publisher<?> pub, Shape shape) throws IOException {
		var res = opSession.getResponse();
		prepareStreamingHeaders(res, shape);

		var writer = res.getNegotiatedWriter();
		FrameEncoder encoder = shape == Shape.SSE
			? ReactiveResponseProcessor::writeSseFrame
			: ReactiveResponseProcessor::writeNdjsonFrame;

		var req = opSession.getRequest().getHttpServletRequest();
		var mdc = opSession.getRestContext().isMdcAsyncPropagation()
			? MdcAsyncListener.snapshot()
			: null;

		AsyncContext asyncCtx = null;
		if (req.isAsyncSupported()) {
			try {
				asyncCtx = req.startAsync();
			} catch (IllegalStateException e) {
				asyncCtx = null;  // Already committed, or async not actually supported.
			}
		}

		if (asyncCtx != null) {
			req.setAttribute(AsyncResponseProcessor.ATTR_ASYNC_DISPATCH_OWNED, Boolean.TRUE);
			var ac = asyncCtx;
			ac.setTimeout(0);  // No artificial timeout — streams are paced by the producer / client disconnect.
			pub.subscribe(new StreamingSubscriber(res, writer, encoder, mdc, t -> completeAsync(ac, res, t)));
			return FINISHED;
		}

		// Synchronous fallback (MockServletRequest et al.): block until the publisher terminates.
		var done = new CompletableFuture<Void>();
		pub.subscribe(new StreamingSubscriber(res, writer, encoder, mdc,
			t -> { if (t == null) done.complete(null); else done.completeExceptionally(t); }));
		awaitSync(done, syncTimeoutMillis(opSession));
		try {
			writer.flush();
			res.flushBuffer();
		} catch (IOException e) {
			LOG.log(Level.FINEST, e, () -> "Final flush of synchronous reactive stream failed: " + e.getMessage());
		}
		return FINISHED;
	}

	private static void prepareStreamingHeaders(RestResponse res, Shape shape) {
		var ct = res.getContentType();
		if (shape == Shape.SSE) {
			if (ct == null || ! ct.contains("event-stream"))
				res.setContentType(SseSerializer.MEDIA_TYPE);
			res.setHeader("X-Content-Type-Options", "nosniff");
		} else if (ct == null) {
			res.setContentType("application/x-ndjson");
		}
		res.setHeader("Cache-Control", "no-cache");
		res.setHeader("Content-Encoding", "identity");
	}

	@SuppressWarnings({
		"java:S2142" // Interrupt flag restored before returning.
	})
	private static void awaitSync(CompletableFuture<Void> done, long timeoutMs) {
		try {
			done.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (TimeoutException | ExecutionException e) {
			LOG.log(Level.FINE, e, () -> "Synchronous reactive stream did not complete cleanly: " + e.getMessage());
		}
	}

	private static void completeAsync(AsyncContext asyncCtx, RestResponse res, Throwable error) {
		try {
			if (error != null)
				LOG.log(Level.FINE, error, () -> "Reactive stream terminated with error: " + error.getMessage());
			if (! res.getHttpServletResponse().isCommitted())
				res.flushBuffer();
		} catch (IOException e) {
			LOG.log(Level.FINEST, e, () -> "Flush during async stream completion failed: " + e.getMessage());
		} finally {
			completeQuietly(asyncCtx);
		}
	}

	private static void completeQuietly(AsyncContext asyncCtx) {
		try {
			asyncCtx.complete();
		} catch (IllegalStateException e) {
			LOG.log(Level.FINEST, e, () -> "AsyncContext.complete() raced with the container: " + e.getMessage());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Frame encoders.
	// -----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S2095" // Writer is response-owned; we never close it.
	})
	private static void writeSseFrame(FinishablePrintWriter w, Object element) throws IOException, SerializeException {
		if (element == null)
			return;
		if (element instanceof SseEvent ev) {
			SseSerializer.DEFAULT.serialize(ev, w);
			return;
		}
		var data = element instanceof CharSequence c ? c.toString() : Json.of(element);
		SseSerializer.DEFAULT.serialize(new SseEvent(null, data), w);
	}

	@SuppressWarnings({
		"java:S2095" // Writer is response-owned; we never close it.
	})
	private static void writeNdjsonFrame(FinishablePrintWriter w, Object element) throws SerializeException {
		if (element == null)
			return;
		var json = element instanceof CharSequence c ? c.toString() : Json.of(element);
		w.write(json);
		w.write("\n");
		w.flush();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Shape / timeout resolution.
	// -----------------------------------------------------------------------------------------------------------------

	private static Shape resolveShape(RestOpSession opSession) {
		var res = opSession.getResponse();
		var ct = res.getContentType();
		var accept = opSession.getRequest().getHttpServletRequest().getHeader("Accept");
		var probe = ((ct == null ? "" : ct) + "," + (accept == null ? "" : accept)).toLowerCase(Locale.ROOT);
		if (probe.contains("event-stream"))
			return Shape.SSE;
		if (probe.contains("ndjson") || probe.contains("jsonl") || probe.contains("json-seq"))
			return Shape.NDJSON;
		return Shape.BUFFER;
	}

	private static long syncTimeoutMillis(RestOpSession opSession) {
		var t = opSession.getContext().getAsyncTimeoutMillis();
		if (t <= 0)
			t = opSession.getRestContext().getAsyncTimeoutMillis();
		return t > 0 ? t : AsyncResponseProcessor.DEFAULT_ASYNC_TIMEOUT_MILLIS;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Adapter discovery (ServiceLoader, cached, defensive against absent backing libraries).
	// -----------------------------------------------------------------------------------------------------------------

	private static List<ReactiveStreamsAdapter> adapters() {
		var a = adapters;
		if (a == null) {
			synchronized (ReactiveResponseProcessor.class) {
				a = adapters;
				if (a == null) {
					a = loadAdapters();
					adapters = a;
				}
			}
		}
		return a;
	}

	private static List<ReactiveStreamsAdapter> loadAdapters() {
		var out = new ArrayList<ReactiveStreamsAdapter>();
		try {
			var it = ServiceLoader.load(ReactiveStreamsAdapter.class).iterator();
			while (it.hasNext())
				addNextAdapter(it, out);
		} catch (ServiceConfigurationError | RuntimeException e) {
			LOG.log(Level.FINE, e, () -> "ServiceLoader for ReactiveStreamsAdapter failed: " + e.getMessage());
		}
		return List.copyOf(out);
	}

	private static void addNextAdapter(Iterator<ReactiveStreamsAdapter> it, List<ReactiveStreamsAdapter> out) {
		try {
			out.add(it.next());
		} catch (ServiceConfigurationError | RuntimeException e) {
			LOG.log(Level.FINE, e, () -> "Skipping reactive adapter (backing library likely absent): " + e.getMessage());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Subscribers.
	// -----------------------------------------------------------------------------------------------------------------

	/** Collects all emitted elements into a list and completes the supplied future (buffer shape). */
	private static final class CollectingSubscriber implements Flow.Subscriber<Object> {
		private final CompletableFuture<List<Object>> cf;
		private final List<Object> items = new ArrayList<>();

		CollectingSubscriber(CompletableFuture<List<Object>> cf) {
			this.cf = cf;
		}

		@Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
		@Override public void onNext(Object item) { items.add(item); }
		@Override public void onError(Throwable t) { cf.completeExceptionally(t); }
		@Override public void onComplete() { cf.complete(items); }
	}

	/** Writes each emitted element as a wire frame (SSE / NDJSON), one element at a time (bounded backpressure). */
	private static final class StreamingSubscriber implements Flow.Subscriber<Object> {
		private final RestResponse res;
		private final FinishablePrintWriter writer;
		private final FrameEncoder encoder;
		private final Map<String,String> mdc;
		private final Consumer<Throwable> onTerminate;
		private final AtomicBoolean terminated = new AtomicBoolean();
		private volatile boolean wrote;
		private Flow.Subscription subscription;

		StreamingSubscriber(RestResponse res, FinishablePrintWriter writer, FrameEncoder encoder,
				Map<String,String> mdc, Consumer<Throwable> onTerminate) {
			this.res = res;
			this.writer = writer;
			this.encoder = encoder;
			this.mdc = mdc;
			this.onTerminate = onTerminate;
		}

		@Override public void onSubscribe(Flow.Subscription s) {
			subscription = s;
			s.request(1);
		}

		@Override public void onNext(Object item) {
			try {
				withMdc(() -> writeFrame(item));
				subscription.request(1);
			} catch (RuntimeException e) {
				subscription.cancel();
				terminate(unwrap(e));
			}
		}

		@Override public void onError(Throwable t) { terminate(t); }
		@Override public void onComplete() { terminate(null); }

		private void writeFrame(Object item) {
			try {
				encoder.write(writer, item);
				writer.flush();
				res.flushBuffer();
				wrote = true;
			} catch (IOException | SerializeException e) {
				throw new IllegalStateException(e);
			}
		}

		private void withMdc(Runnable r) {
			MdcAsyncListener.wrap((BiConsumer<Object,Throwable>) (v, e) -> r.run(), mdc).accept(null, null);
		}

		private void terminate(Throwable t) {
			if (! terminated.compareAndSet(false, true))
				return;
			withMdc(() -> {
				if (t != null && ! wrote) {
					try {
						if (! res.getHttpServletResponse().isCommitted())
							res.getHttpServletResponse().sendError(SC_INTERNAL_SERVER_ERROR);
					} catch (IOException e) {
						LOG.log(Level.FINEST, e, () -> "sendError on pre-write stream failure failed: " + e.getMessage());
					}
				}
			});
			onTerminate.accept(t);
		}

		private static Throwable unwrap(Throwable t) {
			if ((t instanceof IllegalStateException || t instanceof CompletionException) && t.getCause() != null)
				return t.getCause();
			return t;
		}
	}
}
