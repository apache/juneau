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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.stats.*;
import org.apache.juneau.rest.server.tracing.*;

/**
 * A specialized invoker for methods that are called during a servlet request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestContext">RestContext</a>
 * </ul>
 */
public class RestOpInvoker extends MethodInvoker {

	private final RestOpArg[] opArgs;
	private final Supplier<Object> resourceSupplier;

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 * @param opArgs The parameter resolvers.
	 * @param stats The instrumentor.
	 */
	public RestOpInvoker(java.lang.reflect.Method m, RestOpArg[] opArgs, MethodExecStats stats) {
		this(m, opArgs, stats, null);
	}

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 * @param opArgs The parameter resolvers.
	 * @param stats The instrumentor.
	 * @param resourceSupplier Optional resource supplier.  When <jk>null</jk>, falls back to {@link RestSession#getResource()}.
	 */
	public RestOpInvoker(java.lang.reflect.Method m, RestOpArg[] opArgs, MethodExecStats stats, Supplier<Object> resourceSupplier) {
		super(m, stats);
		this.opArgs = opArgs;
		this.resourceSupplier = resourceSupplier;
	}

	/**
	 * Invokes this method from the specified {@link RestSession}.
	 *
	 * @param opSession The REST call.
	 * @throws Exception If an error occurred during either parameter resolution or method invocation.
	 */
	public void invoke(RestOpSession opSession) throws Exception {
		invoke(opSession, false);
	}

	/**
	 * Invokes this method as the main {@code @RestOp} handler from the specified {@link RestOpSession},
	 * firing per-request observability hooks ({@link MetricsRecorder} and {@link TracerHook}) around the
	 * underlying invocation.
	 *
	 * <p>
	 * Called by {@link RestOpSession#run()} for the {@code @RestOp}-annotated handler method. Pre / post
	 * call methods (those invoked via {@link RestContext#preCall(RestOpSession)} /
	 * {@link RestContext#postCall(RestOpSession)}) continue to use {@link #invoke(RestOpSession)} so the
	 * observability boundary stays anchored on the user-facing handler.
	 *
	 * <h5 class='section'>Virtual-thread dispatch</h5>
	 * <p>
	 * When the operation opts into virtual threads via {@code @Rest(virtualThreads=true)} or
	 * {@code @RestOp(virtualThreads=true)} and the runtime is Java 21+, the entire body of this method
	 * (parameter resolution, observability scope opening, handler invocation, observability close) is
	 * submitted to the resource's virtual-thread executor and the request thread blocks on completion.
	 * That keeps observability scoping correct (open and close run on the same thread) while letting
	 * blocking I/O inside the handler park a virtual thread instead of the carrier.
	 *
	 * <h5 class='section'>{@link CompletableFuture} returns</h5>
	 * <p>
	 * When the handler returns a {@link CompletionStage}, the observability close is deferred to the
	 * future's {@code whenComplete} callback so metrics and traces capture the actual completion time
	 * and outcome — not the synchronous "future returned" moment.
	 *
	 * @param opSession The REST call.
	 * @throws Exception If an error occurred during either parameter resolution or method invocation.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for REST operation invocation dispatch (virtual-thread + observability paths)
		"java:S1181"  // Throwable (incl. Error) is intentionally captured to propagate it across the virtual-thread boundary; the caller rethrows Errors as-is.
	})
	public void invokeOp(RestOpSession opSession) throws Exception {
		var vtExec = opSession.getContext().isVirtualThreadsEnabled()
			? opSession.getRestContext().getVirtualThreadExecutor()
			: null;

		if (vtExec == null) {
			invoke(opSession, true);
			return;
		}

		var f = new CompletableFuture<Void>();
		vtExec.execute(() -> {
			try {
				invoke(opSession, true);
				f.complete(null);
			} catch (Throwable t) {
				f.completeExceptionally(t);
			}
		});

		try {
			f.get();
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause() == null ? ee : ee.getCause();
			if (cause instanceof Exception ex)
				throw ex;
			if (cause instanceof Error err)
				throw err;
			throw new InternalServerError(cause);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new InternalServerError(ie);
		}
	}

	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for the dispatch hot path.
		"resource"    // BeanStore is not owned here; its lifecycle is managed by RestContext.
	})
	private void invoke(RestOpSession opSession, boolean observable) throws Exception {
		var args = new Object[opArgs.length];
		for (var i = 0; i < opArgs.length; i++) {
			ParameterInfo pi = inner().getParameter(i);
			try {
				args[i] = opArgs[i].resolve(opSession);
			} catch (BasicHttpException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequest(e, "Could not resolve parameter %s of type '%s' on method '%s'.", i, pi.getParameterType(), getFullName());
			}
		}

		RestRequest req = opSession.getRequest();
		RestResponse res = opSession.getResponse();

		MetricsRecorder recorder = NoOpMetricsRecorder.INSTANCE;
		Scope tracerScope = NoOpTracerHook.NoOpScope.INSTANCE;
		long startNanos = 0L;
		Throwable observed = null;
		boolean observabilityDeferred = false;
		var opContext = opSession.getContext();
		// observable=true means this is the @RestOp handler (not a pre/post call), AND observability is not
		// explicitly disabled for this operation via @Rest(observability="false") or @RestOp(observability="false").
		boolean effectivelyObservable = observable && opContext.isObservabilityEnabled();
		if (effectivelyObservable) {
			var bs = opSession.getRestContext().getBeanStore();
			recorder = bs.getBean(MetricsRecorder.class).orElse(NoOpMetricsRecorder.INSTANCE);
			var tracer = bs.getBean(TracerHook.class).orElse(NoOpTracerHook.INSTANCE);
			tracerScope = tracer.startSpan(req);
			startNanos = System.nanoTime();
		}

		try {
			RestSession session = opSession.getRestSession();

			var target = resourceSupplier == null ? session.getResource() : resourceSupplier.get();
			Object output = super.invoke(target, args);

			// Handle manual call to req.setDebug().
			Boolean debug = req.getAttribute("Debug").as(Boolean.class).orElse(null);
			if (debug == Boolean.TRUE) {
				session.debug(true);
			} else if (debug == Boolean.FALSE) {
				session.debug(false);
			}

			if (! inner().hasReturnType(Void.TYPE) && (nn(output) || ! res.getOutputStreamCalled()))
				res.setContent(output);

			if (effectivelyObservable && output instanceof CompletionStage<?> stage) {
				observabilityDeferred = true;
				deferObservability(stage, recorder, tracerScope, startNanos, opSession, opContext.getMetricName(), opContext.getMetricTags());
			}

		} catch (IllegalAccessException | IllegalArgumentException e) {
			observed = e;
			throw new InternalServerError(e, "Error occurred invoking method '%s'.", inner().getNameFull());
		} catch (InvocationTargetException e) {
			Throwable e2 = e.getTargetException();
			observed = e2;
			res.setStatus(500);  // May be overridden later.
			res.setContent(opSession.getRestContext().convertThrowable(e2));
		} finally {
			if (effectivelyObservable && ! observabilityDeferred) {
				int status;
				if (observed != null)
					status = deriveStatus(observed);
				else {
					status = res.getStatus();
					if (status == 0)
						status = 200;
				}
				try {
					tracerScope.setStatusCode(status);
					if (nn(observed))
						tracerScope.setError(observed);
				} finally {
					try {
						tracerScope.close();
					} finally {
					var elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
					var pathTemplate = resolveUriTemplate(opSession);
					recorder.record(getFullName(), req.getMethod(), pathTemplate, status, elapsed, observed, opContext.getMetricName(), opContext.getMetricTags());
					}
				}
			}
		}
	}

	private void deferObservability(CompletionStage<?> stage, MetricsRecorder recorder, Scope tracerScope,
			long startNanos, RestOpSession opSession, String metricName, String metricTags) {
		var fullName = getFullName();
		var httpMethod = opSession.getRequest().getMethod();
		var pathTemplate = resolveUriTemplate(opSession);
		stage.whenComplete((value, error) -> {
			Throwable err = unwrapCompletionError(error);
			int status = deriveStatus(err);
			try {
				tracerScope.setStatusCode(status);
				if (err != null)
					tracerScope.setError(err);
			} finally {
				try {
					tracerScope.close();
				} finally {
				var elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
				recorder.record(fullName, httpMethod, pathTemplate, status, elapsed, err, metricName, metricTags);
				}
			}
		});
	}

	private static Throwable unwrapCompletionError(Throwable t) {
		if (t instanceof CompletionException && t.getCause() != null)
			return t.getCause();
		return t;
	}

	private static int deriveStatus(Throwable err) {
		if (err == null)
			return 200;
		if (err instanceof BasicHttpException bhe)
			return bhe.getStatusCode();
		return 500;
	}

	private static String resolveUriTemplate(RestOpSession opSession) {
		try {
			var pp = opSession.getContext().getPathPattern();
			return pp == null ? "" : pp;
		} catch (RuntimeException e) {
			// Defensive: getPathPattern() dereferences pathMatchers[0]; protect against any unusual routing setup.
			return "";
		}
	}
}
