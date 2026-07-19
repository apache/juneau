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
package org.apache.juneau.rest.server.processor;

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Response processor that unwraps {@link CompletableFuture} / {@link CompletionStage} return values from
 * {@code @RestOp}-annotated methods and bridges them to the servlet container's
 * {@link AsyncContext asynchronous} request lifecycle.
 *
 * <p>
 * When a handler returns a future, this processor is the FIRST processor in the default chain
 * (see {@link org.apache.juneau.rest.server.config.DefaultConfig}). It detects the {@link CompletionStage}
 * content, calls {@link HttpServletRequest#startAsync()} on the underlying servlet request, and
 * registers a {@link CompletionStage#whenComplete(java.util.function.BiConsumer) whenComplete}
 * callback that:
 *
 * <ul>
 * 	<li>On success — installs the unwrapped value via {@link RestResponse#setContent(Object)}, re-runs
 * 		{@link RestContext#processResponse(RestOpSession)} (this processor sees a non-future and falls
 * 		through to {@code SerializedPojoProcessor} et al.), flushes the response, and
 * 		calls {@link AsyncContext#complete()}.
 * 	<li>On failure — runs the throwable through {@link RestContext#convertThrowable(Throwable)} so
 * 		the existing error pipeline (including {@link ThrowableProcessor} and
 * 		{@link ProblemDetailsProcessor}) handles it, then completes the {@code AsyncContext}.
 * </ul>
 *
 * <h5 class='section'>Synchronous fallback</h5>
 * <p>
 * In environments where {@link HttpServletRequest#startAsync()} is unsupported (most notably
 * Juneau's {@code MockServletRequest}, which returns {@code null}), this processor falls back to a
 * blocking {@link CompletableFuture#get(long, TimeUnit) get(timeout)} on the future and then
 * returns {@link #RESTART} so the rest of the chain runs synchronously on the unwrapped value.
 * This keeps the unit-test surface working without requiring a real servlet container while
 * preserving full {@code AsyncContext} semantics in production.
 *
 * <h5 class='section'>Timeout</h5>
 * <p>
 * The async timeout is configurable via {@code @Rest(asyncTimeoutMillis)} /
 * {@code @RestOp(asyncTimeoutMillis)} (default 30s). On timeout the processor cancels the future
 * with {@code mayInterruptIfRunning=true} and writes a {@link HttpServletResponse#SC_GATEWAY_TIMEOUT}
 * response.
 *
 * <h5 class='section'>Bare {@link Future} rejection</h5>
 * <p>
 * Bare {@link Future} return types (anything that is a {@code Future} but not a
 * {@link CompletionStage}) are rejected with an {@link InternalServerError} —
 * polling a {@code Future} would block the request thread without any of the cancellation /
 * cooperation guarantees that {@link CompletableFuture} provides. Handlers that need async dispatch
 * must return {@link CompletableFuture} or {@link CompletionStage}.
 *
 * <h5 class='section'>Completion executor</h5>
 * <p>
 * When {@code @Rest(asyncCompletionExecutor="poolName")} or the per-op equivalent is set, this processor
 * switches from {@code future.whenComplete(callback)} to {@code future.whenCompleteAsync(callback, executor)},
 * routing the response-handler work through the named {@link Executor} bean. The MDC
 * snapshot (see below) is taken on the request thread <em>before</em> the executor routing so the MDC context
 * is always available regardless of which thread the executor picks.
 *
 * <h5 class='section'>Thread-local caveats</h5>
 * <p>
 * Anything carried via {@link ThreadLocal} (SLF4J MDC, security contexts) does NOT survive the
 * async boundary — the {@code whenComplete} callback typically runs on a different thread than the
 * one that produced the future. {@code RequestAttributes}, {@link org.apache.juneau.commons.svl.VarResolverSession},
 * and {@link java.util.Locale} are request-scoped and survive correctly.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerAsync">Async Response Handling</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class AsyncResponseProcessor implements ResponseProcessor {

	private static final Logger LOG = Logger.getLogger(AsyncResponseProcessor.class.getName());

	/**
	 * Request attribute set by this processor when the {@code AsyncContext} path is taken so
	 * downstream lifecycle code (specifically {@link RestOpSession#finish()} and
	 * {@link RestSession#finish()}) can skip the synchronous {@code flushBuffer()} / {@code req.close()}
	 * — the {@code AsyncContext.complete()} call inside the {@code whenComplete} callback is
	 * responsible for committing the response.
	 *
	 * @since 10.0.0
	 */
	public static final String ATTR_ASYNC_DISPATCH_OWNED = "org.apache.juneau.rest.server.async.dispatchOwned";

	/** Default async timeout in milliseconds when no annotation override is supplied. */
	public static final long DEFAULT_ASYNC_TIMEOUT_MILLIS = 30_000L;

	@Override /* Overridden from ResponseProcessor */
	@SuppressWarnings({
		"java:S3776", // Async dispatch logic is inherently branchy — splitting further hurts readability.
		"java:S1141"  // Nested try/catch cleanly separates startAsync IllegalStateException recovery from cancellation.
	})
	public int process(RestOpSession opSession) throws IOException, BasicHttpException {
		var res = opSession.getResponse();
		var content = res.getContent().orElse(null);

		if (content == null)
			return NEXT;

		if (content instanceof CompletionStage<?> stage)
			return processAsync(opSession, stage);

		if (content instanceof Future<?>) {
			throw new InternalServerError(
				"Bare java.util.concurrent.Future is not supported as a return type from @RestOp methods. "
					+ "Return CompletableFuture or CompletionStage instead, or block on the result yourself before returning."
			);
		}

		return NEXT;
	}

	private int processAsync(RestOpSession opSession, CompletionStage<?> stage) throws BasicHttpException {
		var timeoutMs = resolveTimeoutMillis(opSession);
		var cf = stage.toCompletableFuture();
		var req = opSession.getRequest().getHttpServletRequest();

		// True async path requires both isAsyncSupported() and a non-null AsyncContext from startAsync().
		AsyncContext asyncCtx = null;
		if (req.isAsyncSupported()) {
			try {
				asyncCtx = req.startAsync();
				if (asyncCtx != null && timeoutMs > 0)
					asyncCtx.setTimeout(timeoutMs);
			} catch (IllegalStateException e) {
				asyncCtx = null;  // Already committed or async not actually supported on this request.
			}
		}

		if (asyncCtx == null)
			return processSyncFallback(opSession, cf, timeoutMs);

		dispatchAsync(opSession, cf, asyncCtx, timeoutMs);
		return FINISHED;
	}

	@SuppressWarnings({
		"java:S2142", // We re-set the interrupt flag immediately and surface as a 500.
		"java:S1166", // CancellationException is intentionally swallowed; we surface it as a 500 via convertThrowable.
		"java:S3516"  // Always returns RESTART by design: every outcome (success or error) re-runs the chain on the now-synchronous content.
	})
	private int processSyncFallback(RestOpSession opSession, CompletableFuture<?> cf, long timeoutMs) {
		var res = opSession.getResponse();
		try {
			Object value = (timeoutMs > 0) ? cf.get(timeoutMs, TimeUnit.MILLISECONDS) : cf.get();
			res.setContent(value);
			return RESTART;
		} catch (TimeoutException te) {
			cf.cancel(true);
			res.setStatus(SC_GATEWAY_TIMEOUT);
			res.setContent(opSession.getRestContext().convertThrowable(
				new GatewayTimeout("Async response timed out after " + timeoutMs + "ms.")
			));
			return RESTART;
		} catch (CancellationException ce) {
			res.setStatus(SC_INTERNAL_SERVER_ERROR);
			res.setContent(opSession.getRestContext().convertThrowable(ce));
			return RESTART;
		} catch (ExecutionException ee) {
			Throwable cause = or(ee.getCause(), ee);
			res.setStatus(SC_INTERNAL_SERVER_ERROR);
			res.setContent(opSession.getRestContext().convertThrowable(cause));
			return RESTART;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			cf.cancel(true);
			res.setStatus(SC_INTERNAL_SERVER_ERROR);
			res.setContent(opSession.getRestContext().convertThrowable(ie));
			return RESTART;
		}
	}

	private void dispatchAsync(RestOpSession opSession, CompletableFuture<?> cf, AsyncContext asyncCtx, long timeoutMs) {
		var req = opSession.getRequest().getHttpServletRequest();
		req.setAttribute(ATTR_ASYNC_DISPATCH_OWNED, Boolean.TRUE);

		var done = new AtomicBoolean(false);

		asyncCtx.addListener(new AsyncListener() {
			@Override public void onComplete(AsyncEvent ev) { /* no-op */ }
			@Override public void onStartAsync(AsyncEvent ev) { /* no-op */ }
			@Override public void onError(AsyncEvent ev) {
				finalizeAsync(opSession, cf, asyncCtx, ev.getThrowable(), done, /* timeout */ false, timeoutMs);
			}
			@Override public void onTimeout(AsyncEvent ev) {
				finalizeAsync(opSession, cf, asyncCtx, null, done, /* timeout */ true, timeoutMs);
			}
		});

		// MDC bridge: snapshot request-thread MDC at dispatch time, then restore it on the
		// completion thread so log statements inside the whenComplete callback see the same diagnostic
		// context (requestId, userId, etc.) that was set by upstream filters.
		// NOTE: the MDC wrap happens BEFORE the executor routing so the snapshot is always
		// available regardless of which thread the executor picks.
		var mdcSnapshot = opSession.getRestContext().isMdcAsyncPropagation()
			? MdcAsyncListener.snapshot()
			: null;

		var callback = MdcAsyncListener.wrap(
			(value, error) -> finalizeAsync(opSession, cf, asyncCtx, error, done, /* timeout */ false, timeoutMs, value),
			mdcSnapshot
		);

		// If a per-op or resource-level completion executor is configured, route the callback
		// through it via whenCompleteAsync; otherwise use the natural completion thread.
		var executor = opSession.getContext().getAsyncCompletionExecutor();
		if (executor != null)
			cf.whenCompleteAsync(callback, executor);
		else
			cf.whenComplete(callback);
	}

	private void finalizeAsync(RestOpSession opSession, CompletableFuture<?> cf, AsyncContext asyncCtx,
			Throwable error, AtomicBoolean done, boolean timeout, long timeoutMs) {
		finalizeAsync(opSession, cf, asyncCtx, error, done, timeout, timeoutMs, null);
	}

	@SuppressWarnings({
		"java:S3776", // Async finalization is inherently branchy.
		"java:S1141", // Nested try/catch separates response-processing from container cleanup.
		"java:S107"   // The 8 parameters are the internal async-finalization context (session, future, async ctx, error, done-latch, timeout flag, timeout ms, resolved value); a holder object would obscure this hot-path call and its sibling overload.
	})
	private void finalizeAsync(RestOpSession opSession, CompletableFuture<?> cf, AsyncContext asyncCtx,
			Throwable error, AtomicBoolean done, boolean timeout, long timeoutMs, Object value) {
		if (! done.compareAndSet(false, true))
			return;

		var res = opSession.getResponse();

		try {
			if (timeout) {
				cf.cancel(true);
				res.setStatus(SC_GATEWAY_TIMEOUT);
				res.setContent(opSession.getRestContext().convertThrowable(
					new GatewayTimeout("Async response timed out after " + timeoutMs + "ms.")
				));
			} else if (error != null) {
				Throwable cause = unwrap(error);
				res.setStatus(SC_INTERNAL_SERVER_ERROR);
				res.setContent(opSession.getRestContext().convertThrowable(cause));
			} else {
				res.setContent(value);
			}

			opSession.getRestContext().processResponse(opSession);
			res.flushBuffer();
		} catch (Exception e) {
			LOG.log(Level.WARNING, e, () -> "Async response finalization failed: " + e.getMessage());
			try {
				if (! res.getHttpServletResponse().isCommitted())
					res.getHttpServletResponse().sendError(SC_INTERNAL_SERVER_ERROR);
			} catch (Exception inner) {
				LOG.log(Level.FINEST, inner, () -> "Async response error-fallback also failed: " + inner.getMessage());
			}
		} finally {
			try {
				asyncCtx.complete();
			} catch (IllegalStateException ise) {
				LOG.log(Level.FINEST, ise, () -> "AsyncContext.complete() raced with the container: " + ise.getMessage());
			}
		}
	}

	private static Throwable unwrap(Throwable t) {
		if (t instanceof CompletionException && t.getCause() != null)
			return t.getCause();
		if (t instanceof ExecutionException && t.getCause() != null)
			return t.getCause();
		return t;
	}

	private static long resolveTimeoutMillis(RestOpSession opSession) {
		var op = opSession.getContext().getAsyncTimeoutMillis();
		if (op > 0)
			return op;
		var ctx = opSession.getRestContext().getAsyncTimeoutMillis();
		return ctx > 0 ? ctx : DEFAULT_ASYNC_TIMEOUT_MILLIS;
	}

	/**
	 * Returns whether the given session has been handed off to async dispatch. Inspected by
	 * {@link RestOpSession#finish()} / {@link RestSession#finish()} to skip the synchronous
	 * {@code flushBuffer()} call when the {@code AsyncContext.complete()} path will commit the
	 * response itself.
	 *
	 * @param opSession The session to check.
	 * @return {@code true} if {@link #ATTR_ASYNC_DISPATCH_OWNED} has been set.
	 */
	public static boolean isAsyncDispatchOwned(RestOpSession opSession) {
		if (opSession == null)
			return false;
		return isAsyncDispatchOwned(opSession.getRequest().getHttpServletRequest());
	}

	/**
	 * Variant of {@link #isAsyncDispatchOwned(RestOpSession)} that operates directly on the
	 * underlying servlet request — used by {@link RestSession#finish()} where the
	 * {@link RestOpSession} may not be available.
	 *
	 * @param req The servlet request.
	 * @return {@code true} if {@link #ATTR_ASYNC_DISPATCH_OWNED} has been set.
	 */
	public static boolean isAsyncDispatchOwned(HttpServletRequest req) {
		if (req == null)
			return false;
		return isTrue(req.getAttribute(ATTR_ASYNC_DISPATCH_OWNED));
	}
}
