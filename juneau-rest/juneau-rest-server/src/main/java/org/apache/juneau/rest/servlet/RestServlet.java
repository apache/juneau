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

import static java.util.logging.Level.*;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.text.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.response.*;

/**
 * Servlet implementation of a REST resource.
 *
 * <p>
 * 	The {@link RestServlet} class is the entry point for your REST resources.
 * 	It extends directly from <l>HttpServlet</l> and is deployed like any other servlet.
 * </p>
 * <p>
 * 	When the servlet <l>init()</l> method is called, it triggers the code to find and process the <l>@Rest</l>
 * 	annotations on that class and all child classes.
 * 	These get constructed into a {@link RestContext} object that holds all the configuration
 * 	information about your resource in a read-only object.
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		Users will typically extend from {@link BasicRestServlet} or {@link BasicRestServletGroup}
 * 		instead of this class directly.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 *
 * @serial exclude
 */
public abstract class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private AtomicReference<RestContext> context = new AtomicReference<>();
	private AtomicReference<Exception> initException = new AtomicReference<>();

	@Override /* Servlet */
	public synchronized void init(ServletConfig servletConfig) throws ServletException {
		try {
			if (context.get() != null)
				return;
			super.init(servletConfig);
			context.set(RestContext.create(this.getClass(), null, servletConfig).init(()->this).build());
			context.get().postInit();
			context.get().postInitChildFirst();
		} catch (ServletException e) {
			initException.set(e);
			log(SEVERE, e, "Servlet init error on class ''{0}''", className(this));
			throw e;
		} catch (BasicHttpException e) {
			initException.set(e);
			log(SEVERE, e, "Servlet init error on class ''{0}''", className(this));
		} catch (Throwable e) {
			initException.set(new InternalServerError(e));
			log(SEVERE, e, "Servlet init error on class ''{0}''", className(this));
		}
	}

	/**
	 * Sets the context object for this servlet.
	 *
	 * <p>
	 * This method is effectively a no-op if {@link #init(ServletConfig)} has already been called.
	 *
	 * @param context Sets the context object on this servlet.
	 * @throws ServletException If error occurred during initialization.
	 */
	protected void setContext(RestContext context) throws ServletException {
		if (this.context.get() == null) {
			super.init(context.getBuilder());
			this.context.set(context);
		}
	}

	/**
	 * Returns the path for this resource as defined by the @Rest(path) annotation or RestContext.Builder.path(String) method
	 * concatenated with those on all parent classes.
	 *
	 * @return The path defined on this servlet, or an empty string if not specified.
	 */
	public synchronized String getPath() {
		RestContext context = this.context.get();
		if (context != null)
			return context.getFullPath();
		ClassInfo ci = ClassInfo.of(getClass());
		Value<String> path = Value.empty();
		ci.forEachAnnotation(Rest.class, x -> isNotEmpty(x.path()), x -> path.set(trimSlashes(x.path())));
		return path.orElse("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 */
	@Override /* Servlet */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, InternalServerError, IOException {
		try {
			if (initException.get() != null)
				throw initException.get();
			if (context.get() == null)
				throw new InternalServerError("Servlet {0} not initialized.  init(ServletConfig) was not called.  This can occur if you've overridden this method but didn't call super.init(RestConfig).", className(this));
			getContext().execute(this, r1, r2);

		} catch (Throwable e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		}
	}

	@Override /* GenericServlet */
	public synchronized void destroy() {
		if (context.get() != null)
			context.get().destroy();
		super.destroy();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Context methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the read-only context object that contains all the configuration information about this resource.
	 *
	 * <p>
	 * This object is <jk>null</jk> during the call to {@link #init(ServletConfig)} but is populated by the time
	 * {@link #init()} is called.
	 *
	 * <p>
	 * Resource classes that don't extend from {@link RestServlet} can add the following method to their class to get
	 * access to this context object:
	 * <p class='bjava'>
	 * 	<jk>public void</jk> init(RestServletContext <jv>context</jv>) <jk>throws</jk> Exception;
	 * </p>
	 *
	 * @return The context information on this servlet.
	 */
	public synchronized RestContext getContext() {
		RestContext rc = context.get();
		if (rc == null)
			throw new InternalServerError("RestContext object not set on resource.");
		return rc;
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
