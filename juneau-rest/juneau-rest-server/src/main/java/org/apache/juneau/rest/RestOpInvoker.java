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
package org.apache.juneau.rest;

import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.time.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.metrics.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.rest.tracing.*;

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
	public RestOpInvoker(Method m, RestOpArg[] opArgs, MethodExecStats stats) {
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
	public RestOpInvoker(Method m, RestOpArg[] opArgs, MethodExecStats stats, Supplier<Object> resourceSupplier) {
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
	 * @param opSession The REST call.
	 * @throws Exception If an error occurred during either parameter resolution or method invocation.
	 */
	public void invokeOp(RestOpSession opSession) throws Exception {
		invoke(opSession, true);
	}

	private void invoke(RestOpSession opSession, boolean observable) throws Exception {
		var args = new Object[opArgs.length];
		for (var i = 0; i < opArgs.length; i++) {
			ParameterInfo pi = inner().getParameter(i);
			try {
				args[i] = opArgs[i].resolve(opSession);
			} catch (BasicHttpException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequest(e, "Could not resolve parameter {0} of type ''{1}'' on method ''{2}''.", i, pi.getParameterType(), getFullName());
			}
		}

		RestRequest req = opSession.getRequest();
		RestResponse res = opSession.getResponse();

		MetricsRecorder recorder = NoOpMetricsRecorder.INSTANCE;
		Scope tracerScope = NoOpTracerHook.NoOpScope.INSTANCE;
		long startNanos = 0L;
		Throwable observed = null;
		if (observable) {
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

		} catch (IllegalAccessException | IllegalArgumentException e) {
			observed = e;
			throw new InternalServerError(e, "Error occurred invoking method ''{0}''.", inner().getNameFull());
		} catch (InvocationTargetException e) {
			Throwable e2 = e.getTargetException();
			observed = e2;
			res.setStatus(500);  // May be overridden later.
			res.setContent(opSession.getRestContext().convertThrowable(e2));
		} finally {
			if (observable) {
				int status = res.getStatus();
				if (status == 0)
					status = (observed == null) ? 200 : 500;
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
						recorder.record(getFullName(), req.getMethod(), pathTemplate, status, elapsed, observed);
					}
				}
			}
		}
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