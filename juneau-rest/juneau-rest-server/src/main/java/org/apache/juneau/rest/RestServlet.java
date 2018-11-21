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

import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.text.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;

/**
 * Servlet implementation of a REST resource.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.Instantiation.RestServlet}
 * </ul>
 */
public abstract class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private RestContextBuilder builder;
	private volatile RestContext context;
	private volatile Exception initException;
	private boolean isInitialized = false;  // Should not be volatile.
	private volatile RestResourceResolver resourceResolver;


	@Override /* Servlet */
	public final synchronized void init(ServletConfig servletConfig) throws ServletException {
		try {
			if (context != null)
				return;
			builder = RestContext.create(servletConfig, this.getClass(), null);
			if (resourceResolver != null)
				builder.resourceResolver(resourceResolver);
			builder.init(this);
			super.init(servletConfig);
			builder.servletContext(this.getServletContext());
			setContext(builder.build());
			context.postInitChildFirst();
		} catch (RestException e) {
			// Thrown RestExceptions are simply caught and re-thrown on subsequent calls to service().
			initException = e;
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
		} catch (ServletException e) {
			initException = e;
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			throw e;
		} catch (Exception e) {
			initException = e;
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			throw new ServletException(e);
		} catch (Throwable e) {
			initException = new Exception(e);
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			throw new ServletException(e);
		}
	}

	/*
	 * Bypasses the init(ServletConfig) method and just calls the super.init(ServletConfig) method directly.
	 * Used when subclasses of RestServlet are attached as child resources.
	 */
	synchronized void innerInit(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
	}

	/**
	 * Sets the context object for this servlet.
	 *
	 * @param context
	 * @throws ServletException
	 */
	public synchronized void setContext(RestContext context) throws ServletException {
		this.builder = context.builder;
		this.context = context;
		isInitialized = true;
		context.postInit();
	}

	/**
	 * Sets the resource resolver to use for this servlet and all child servlets.
	 * <p>
	 * This method can be called immediately following object construction, but must be called before {@link #init(ServletConfig)} is called.
	 * Otherwise calling this method will have no effect.
	 *
	 * @param resourceResolver The resolver instance.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public synchronized RestServlet setRestResourceResolver(RestResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
		return this;
	}

	/**
	 * Returns the path defined on this servlet if it's defined via {@link RestResource#path()}.
	 *
	 * @return The path defined on this servlet, or an empty string if not specified.
	 */
	public synchronized String getPath() {
		if (context != null)
			return context.getPath();
		for (RestResource rr : ClassUtils.getAnnotations(RestResource.class, this.getClass()))
			if (! rr.path().isEmpty())
				return trimSlashes(rr.path());
		return "";
	}

	@Override /* GenericServlet */
	public synchronized RestContextBuilder getServletConfig() {
		return builder;
	}

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
	 * <p class='bcode w800'>
	 * 	<jk>public void</jk> init(RestServletContext context) <jk>throws</jk> Exception;
	 * </p>
	 *
	 * @return The context information on this servlet.
	 */
	protected synchronized RestContext getContext() {
		return context;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
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
			// To avoid checking the volatile field context on every call, use the non-volatile isInitialized field as a first-check check.
			if (! isInitialized) {
				if (initException != null) {
					if (initException instanceof RestException)
						throw (RestException)initException;
					throw new InternalServerError(initException);
				}
				if (context == null)
					throw new InternalServerError("Servlet {0} not initialized.  init(ServletConfig) was not called.  This can occur if you've overridden this method but didn't call super.init(RestConfig).", getClass().getName());
				isInitialized = true;
			}

			context.getCallHandler().service(r1, r2);

		} catch (RestException e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		} catch (Throwable e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		}
	}

	@Override /* GenericServlet */
	public void log(String msg) {
		if (context != null)
			context.getLogger().log(INFO, msg);
	}

	@Override /* GenericServlet */
	public void log(String msg, Throwable cause) {
		if (context != null)
			context.getLogger().log(INFO, cause, msg);
	}

	/**
	 * Convenience method for calling <code>getContext().getLogger().log(level, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, String msg, Object...args) {
		if (context != null)
			context.getLogger().log(level, msg, args);
	}

	/**
	 * Convenience method for calling <code>getContext().getLogger().logObjects(level, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void logObjects(Level level, String msg, Object...args) {
		if (context != null)
			context.getLogger().logObjects(level, msg, args);
	}

	/**
	 * Convenience method for calling <code>getContext().getLogger().log(level, cause, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, Throwable cause, String msg, Object...args) {
		if (context != null)
			context.getLogger().log(level, cause, msg, args);
		else {
			// If context failed to initialize, log to the console.
			System.err.println(format(msg, args));
			if (cause != null)
				cause.printStackTrace();
		}
	}

	/**
	 * Returns the current HTTP request.
	 *
	 * @return The current HTTP request, or <jk>null</jk> if it wasn't created.
	 */
	public RestRequest getRequest() {
		if (context == null)
			return null;
		return context.getRequest();
	}

	/**
	 * Returns the current HTTP response.
	 *
	 * @return The current HTTP response, or <jk>null</jk> if it wasn't created.
	 */
	public RestResponse getResponse() {
		if (context == null)
			return null;
		return context.getResponse();
	}

	@Override /* GenericServlet */
	public synchronized void destroy() {
		if (context != null)
			context.destroy();
		super.destroy();
	}

	/**
	 * Convenience method for calling <code>getContext().getProperties();</code>
	 *
	 * @return The resource properties as an {@link RestContextProperties}.
	 * @see RestContext#getProperties()
	 */
	public RestContextProperties getProperties() {
		return getContext().getProperties();
	}
}
