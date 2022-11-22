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

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.logger.*;

/**
 * A session for a single HTTP request.
 *
 * <p>
 * This session object gets created by {@link RestSession} once the Java method to be invoked has been determined.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
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
		 * @return This object.
		 */
		public Builder logger(CallLogger value) {
			session.logger(value);
			return this;
		}

		/**
		 * Enables or disabled debug mode on this call.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 * @throws IOException Occurs if request content could not be cached into memory.
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

	@Override /* ContextSession */
	public RestOpContext getContext() {
		return ctx;
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

		ctx.getMethodInvoker().invoke(this);

		if (res.hasContent())
			for (RestConverter converter : ctx.getConverters())
				res.setContent(converter.convert(req, res.getContent().orElse(null)));
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
	 * @return This object.
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
