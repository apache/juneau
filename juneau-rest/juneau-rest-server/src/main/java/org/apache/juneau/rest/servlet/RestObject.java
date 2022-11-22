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
package org.apache.juneau.rest.servlet;

import static org.apache.juneau.internal.ClassUtils.*;

import java.text.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

/**
 * Identical to {@link RestServlet} but doesn't extend from {@link HttpServlet}.
 *
 * <p>
 * This is particularly useful in Spring Boot environments that auto-detect servlets to deploy in servlet containers,
 * but you want this resource to be deployed as a child instead.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 */
public abstract class RestObject {

	private AtomicReference<RestContext> context = new AtomicReference<>();

	//-----------------------------------------------------------------------------------------------------------------
	// Context methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the context object for this servlet.
	 *
	 * @param context Sets the context object on this servlet.
	 * @throws ServletException If error occurred during post-initialiation.
	 */
	protected void setContext(RestContext context) throws ServletException {
		this.context.set(context);
	}

	/**
	 * Returns the read-only context object that contains all the configuration information about this resource.
	 *
	 * @return The context information on this servlet.
	 */
	protected RestContext getContext() {
		if (context.get() == null)
			throw new InternalServerError("RestContext object not set on resource.");
		return context.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience logger methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Log a message.
	 *
	 * <p>
	 * Subclasses can intercept the handling of these messages by overriding {@link #doLog(Level, Throwable, Supplier)}.
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, String msg, Object...args) {
		doLog(level, null, () -> StringUtils.format(msg, args));
	}

	/**
	 * Log a message.
	 *
	 * <p>
	 * Subclasses can intercept the handling of these messages by overriding {@link #doLog(Level, Throwable, Supplier)}.
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, Throwable cause, String msg, Object...args) {
		doLog(level, cause, () -> StringUtils.format(msg, args));
	}

	/**
	 * Main logger method.
	 *
	 * <p>
	 * The default behavior logs a message to the Java logger of the class name.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own logger handling.
	 *
	 * @param level The log level.
	 * @param cause Optional throwable.
	 * @param msg The message to log.
	 */
	protected void doLog(Level level, Throwable cause, Supplier<String> msg) {
		RestContext c = context.get();
		Logger logger = c == null ? null : c.getLogger();
		if (logger == null)
			logger = Logger.getLogger(className(this));
		logger.log(level, cause, msg);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current thread-local HTTP request.
	 *
	 * @return The current thread-local HTTP request, or <jk>null</jk> if it wasn't created.
	 */
	public synchronized RestRequest getRequest() {
		return getContext().getLocalSession().getOpSession().getRequest();
	}

	/**
	 * Returns the current thread-local HTTP response.
	 *
	 * @return The current thread-local HTTP response, or <jk>null</jk> if it wasn't created.
	 */
	public synchronized RestResponse getResponse() {
		return getContext().getLocalSession().getOpSession().getResponse();
	}
}
