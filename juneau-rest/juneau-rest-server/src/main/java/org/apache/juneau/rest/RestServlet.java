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

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Servlet implementation of a REST resource.
 *
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#TOC">REST Servlet API</a> for information about using this
 * class.
 */
public abstract class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private RestContextBuilder builder;
	private volatile RestContext context;
	private boolean isInitialized = false;
	private Exception initException;


	@Override /* Servlet */
	public final synchronized void init(ServletConfig servletConfig) throws ServletException {
		try {
			builder = new RestContextBuilder(servletConfig, this.getClass(), null);
			builder.init(this);
			super.init(servletConfig);
			if (! isInitialized) {
				builder.servletContext(this.getServletContext());
				context = new RestContext(builder);
				isInitialized = true;
			}
			context.postInit();
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
		} finally {
			isInitialized = true;
		}
	}

	/*
	 * Bypasses the init(ServletConfig) method and just calls the super.init(ServletConfig) method directly.
	 * Used when subclasses of RestServlet are attached as child resources.
	 */
	synchronized void innerInit(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
	}

	/*
	 * Sets the context object for this servlet.
	 * Used when subclasses of RestServlet are attached as child resources.
	 */
	synchronized void setContext(RestContext context) {
		this.builder = context.builder;
		this.context = context;
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
	 * <p class='bcode'>
	 * 	<jk>public void</jk> init(RestServletContext context) <jk>throws</jk> Exception;
	 * </p>
	 *
	 * @return The context information on this servlet.
	 */
	protected synchronized RestContext getContext() {
		return context;
	}

	/**
	 * Convenience method if you want to perform initialization on your resource after all configuration settings
	 * have been made.
	 *
	 * <p>
	 * This allows you to get access to the {@link RestContext} object during initialization.
	 *
	 * <p>
	 * The default implementation does nothing.
	 *
	 * @param context The servlet context containing all the set-in-stone configurations for this resource.
	 * @throws Exception Any exception can be thrown to signal an initialization failure.
	 */
	public synchronized void init(RestContext context) throws Exception {}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 */
	@Override /* Servlet */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {
		try {
			if (initException != null) {
				if (initException instanceof RestException)
					throw (RestException)initException;
				throw new RestException(SC_INTERNAL_SERVER_ERROR, initException);
			}
			if (context == null)
				throw new RestException(SC_INTERNAL_SERVER_ERROR, "Servlet {0} not initialized.  init(ServletConfig) was not called.  This can occur if you've overridden this method but didn't call super.init(RestConfig).", getClass().getName());
			if (! isInitialized)
				throw new RestException(SC_INTERNAL_SERVER_ERROR, "Servlet {0} has not been initialized", getClass().getName());

			context.getCallHandler().service(r1, r2);

		} catch (RestException e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		} catch (Throwable e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		}
	}

	/**
	 * Convenience method for calling <code>getContext().getLogger().log(level, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void log(Level level, String msg, Object...args) {
		if (context != null)
			context.getLogger().log(level, msg, args);
	}

	/**
	 * Convenience method for calling <code>getContext().getLogger().log(level, cause, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void log(Level level, Throwable cause, String msg, Object...args) {
		if (context != null)
			context.getLogger().log(level, cause, msg, args);
		else {
			// If context failed to initialize, log to the console.
			System.err.println(format(msg, args));
			if (cause != null)
				cause.printStackTrace();
		}
	}

	@Override /* GenericServlet */
	public synchronized void destroy() {
		if (context != null)
			context.destroy();
		super.destroy();
	}

	/**
	 * Convenience method for calling <code>getContext().getMessages();</code>
	 *
	 * @return The resource bundle for this resource.  Never <jk>null</jk>.
	 * @see RestContext#getProperties()
	 */
	public MessageBundle getMessages() {
		return context.getMessages();
	}

	/**
	 * Convenience method for calling <code>getContext().getProperties();</code>
	 *
	 * @return The resource properties as an {@link ObjectMap}.
	 * @see RestContext#getProperties()
	 */
	public ObjectMap getProperties() {
		return getContext().getProperties();
	}

	/**
	 * Convenience method for calling <code>getContext().getBeanContext();</code>
	 *
	 * @return The bean context used for parsing path variables and header values.
	 * @see RestContext#getBeanContext()
	 */
	public BeanContext getBeanContext() {
		return getContext().getBeanContext();
	}
}
