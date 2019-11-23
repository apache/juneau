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

import java.text.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;

/**
 * Identical to {@link BasicRestServlet} but doesn't extend from {@link HttpServlet}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.Instantiation.BasicRest}
 * </ul>
 */
@Rest(
	// Allow OPTIONS requests to be simulated using ?method=OPTIONS query parameter.
	allowedMethodParams="OPTIONS"
)
@HtmlDocConfig(
	// Basic page navigation links.
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS"
	}
)
public abstract class BasicRest implements BasicRestConfig {

	private JuneauLogger logger = JuneauLogger.getLogger(getClass());
	private volatile RestContext context;

	/**
	 * Post-initialization hook to retrieve the {@link RestContext} object for this resource.
	 *
	 * @param context The context for this resource.
	 */
	@RestHook(HookEvent.POST_INIT)
	public synchronized void onPostInit(RestContext context) {
		this.context = context;
	}

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return A bean containing the contents for the OPTIONS page.
	 */
	@Override /* BasicRestConfig */
	public Swagger getOptions(RestRequest req) {
		// Localized Swagger for this resource is available through the RestRequest object.
		return req.getSwagger();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Context methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the read-only context object that contains all the configuration information about this resource.
	 *
	 * @return The context information on this servlet.
	 */
	protected synchronized RestContext getContext() {
		if (context == null)
			throw new InternalServerError("RestContext object not set on resource.");
		return context;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience logger methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Log a message.
	 *
	 * @param msg The message to log.
	 */
	public void log(String msg) {
		logger.info(msg);
	}

	/**
	 * Log a message.
	 *
	 * @param msg The message to log.
	 * @param cause The cause.
	 */
	public void log(String msg, Throwable cause) {
		logger.info(cause, msg);
	}

	/**
	 * Log a message.
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, String msg, Object...args) {
		logger.log(level, msg, args);
	}

	/**
	 * Log a message.
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void logObjects(Level level, String msg, Object...args) {
		logger.logObjects(level, msg, args);
	}

	/**
	 * Log a message.
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, Throwable cause, String msg, Object...args) {
		logger.log(level, cause, msg, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Request-time methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current HTTP request.
	 *
	 * @return The current HTTP request.
	 */
	public synchronized RestRequest getRequest() {
		return getContext().getRequest();
	}

	/**
	 * Returns the current HTTP response.
	 *
	 * @return The current HTTP response
	 */
	public synchronized RestResponse getResponse() {
		return getContext().getResponse();
	}
}
