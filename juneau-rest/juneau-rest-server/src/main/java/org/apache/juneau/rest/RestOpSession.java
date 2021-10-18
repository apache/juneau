// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.utils.*;

/**
 * A session for a single HTTP request.
 *
 * <p>
 * This session object gets created by {@link RestSession} once the Java method to be invoked has been determined.
 */
public class RestOpSession extends ContextSession {


	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param ctx The context object of the Java method being invoked.
	 * @param session The REST session object creating this object.
	 * @return A new builder.
	 */
	public static Builder create(RestOpContext ctx, RestSession session) {
		return new Builder(ctx, session);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends ContextSession.Builder {

		final RestOpContext ctx;
		final RestSession session;

		/**
		 * Constructor.
		 *
		 * @param ctx The context object of the Java method being invoked.
		 * @param session The REST session object creating this object.
		 */
		public Builder(RestOpContext ctx, RestSession session) {
			super(ctx);
			this.ctx = ctx;
			this.session = session;
		}

		/**
		 * Sets the logger to use when logging this call.
		 *
		 * @param value The new value for this setting.  Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		public Builder logger(RestLogger value) {
			session.logger(value);
			return this;
		}

		/**
		 * Enables or disabled debug mode on this call.
		 *
		 * @param value The new value for this setting.
		 * @return This object (for method chaining).
		 * @throws IOException Occurs if request body could not be cached into memory.
		 */
		public Builder debug(boolean value) throws IOException {
			session.debug(value);
			return this;
		}

		@Override /* Session.Builder */
		public RestOpSession build() {
			return new RestOpSession(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final RestOpContext ctx;
	private final RestSession session;
	private final RestRequest req;
	private final RestResponse res;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected RestOpSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		session = builder.session;
		try {
			req = session.getBeanStore().add(RestRequest.class, ctx.createRequest(session));
			res = session.getBeanStore().add(RestResponse.class, ctx.createResponse(session, req));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	/**
	 * Runs this session.
	 *
	 * <p>
	 * Does the following:
	 * <ol>
	 * 	<li>Runs the guards on the method.
	 * 	<li>Finds the parameter values to pass to the Java method.
	 * 	<li>Invokes the Java method.
	 * 	<li>Sets the output and status on the response.
	 * 	<li>Calls the converters on the Java method.
	 * </ol>
	 *
	 * @throws Throwable Any throwable can be thrown.
	 */
	public void run() throws Throwable {

		for (RestGuard guard : ctx.getGuards())
			if (! guard.guard(req, res))
				return;

		java.lang.reflect.Method m = ctx.getJavaMethod();
		RestOpArg[] opArgs = ctx.getOpArgs();
		MethodInvoker methodInvoker = ctx.getMethodInvoker();

		Object[] args = new Object[opArgs.length];
		for (int i = 0; i < opArgs.length; i++) {
			ParamInfo pi = methodInvoker.inner().getParam(i);
			try {
				args[i] = opArgs[i].resolve(this);
			} catch (Exception e) {
				throw toHttpException(e, BadRequest.class, "Could not convert resolve parameter {0} of type ''{1}'' on method ''{2}''.", i, pi.getParameterType(), m.getName());
			}
		}

		try {
			Object output = methodInvoker.invoke(session.getResource(), args);

			// Handle manual call to req.setDebug().
			Boolean debug = req.getAttribute("Debug").asType(Boolean.class).orElse(null);
			if (debug == Boolean.TRUE) {
				session.debug(true);
			} else if (debug == Boolean.FALSE) {
				session.debug(false);
			}

			if (res.getStatus() == 0)
				res.setStatus(200);
			if (! m.getReturnType().equals(Void.TYPE)) {
				if (output != null || ! res.getOutputStreamCalled())
					res.setOutput(output);
			}
		} catch (ExecutableException e) {
			Throwable e2 = e.unwrap();  // Get the throwable thrown from the doX() method.
			res.setStatus(500);  // May be overridden later.
			Class<?> c = e2.getClass();
			if (e2 instanceof HttpResponse || c.getAnnotation(Response.class) != null || c.getAnnotation(ResponseBody.class) != null) {
				res.setOutput(e2);
			} else {
				throw e.unwrap();
			}
		} catch (IllegalArgumentException e) {
			MethodInfo mi = MethodInfo.of(m);
			throw new BadRequest(e,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				mi.getShortName(), mi.getFullName()
			);
		}

		Optional<Optional<Object>> o = res.getOutput();
		if (o.isPresent())
			for (RestConverter converter : ctx.getConverters())
				res.setOutput(converter.convert(req, o.get().orElse(null)));
	}

	/**
	 * Returns the REST request object for this session.
	 *
	 * @return The REST request object for this session.
	 */
	public RestRequest getRequest() {
		return req;
	}

	/**
	 * Returns the REST response object for this session.
	 *
	 * @return The REST response object for this session.
	 */
	public RestResponse getResponse() {
		return res;
	}

	/**
	 * Returns the bean store for this session.
	 *
	 * @return The bean store for this session.
	 */
	public BeanStore getBeanStore() {
		return session.getBeanStore();
	}

	/**
	 * Returns the context of the parent class of this Java method.
	 *
	 * @return The context of the parent class of this Java method.
	 */
	public RestContext getRestContext() {
		return session.getContext();
	}

	/**
	 * Returns the session of the parent class of this Java method.
	 *
	 * @return The session of the parent class of this Java method.
	 */
	public RestSession getRestSession() {
		return session;
	}

	/**
	 * Sets the status of the response.
	 *
	 * @param value The new status.
	 * @return This object.
	 */
	public RestOpSession status(StatusLine value) {
		session.status(value);
		return this;
	}

	/**
	 * Called at the end of a call to finish any remaining tasks such as flushing buffers and logging the response.
	 *
	 * @return This object (for method chaining).
	 */
	public RestOpSession finish() {
		try {
			res.flushBuffer();
			req.close();
		} catch (Exception e) {
			session.exception(e);
		}
		return this;
	}
}
