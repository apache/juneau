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

import java.io.*;
import java.text.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Servlet implementation of a REST resource.
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#TOC">REST Servlet API</a> for information about using this class.
 */
@SuppressWarnings("hiding")
public abstract class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private RestConfig config;
	private RestContext context;
	private boolean isInitialized = false;
	private Exception initException;


	@Override /* Servlet */
	public final synchronized void init(ServletConfig servletConfig) throws ServletException {
		try {
			RestConfig rsc = new RestConfig(servletConfig, this.getClass(), null);
			init(rsc);
			if (! isInitialized) {
				// Subclass may not have called super.init(RestServletConfig), so initialize here.
				createContext(rsc);
				super.init(servletConfig);
			}
		} catch (RestException e) {
			// Thrown RestExceptions are simply caught and rethrown on subsequent calls to service().
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

	/**
	 * Resource initialization method.
	 * <p>
	 * Identical to {@link Servlet#init(ServletConfig)} except the config object provides
	 * access to the external config file, configuration properties, and variable resolver
	 * defined for this resource.
	 * <p>
	 * Classes can also use {@link HttpServlet#init()} and {@link RestServlet#getServletConfig()}
	 * as well to perform initialization.
	 * <p>
	 * Note that if you override this method, you must first call <code><jk>super</jk>.init(servletConfig)</code>!
	 * <p>
	 * Resource classes that don't extend from {@link RestServlet} can add this method to their class
	 * to get access to the config object.
	 *
	 * @param config The servlet configuration.
	 * @throws Exception Any exception can be thrown to signal an initialization failure.
	 */
	public synchronized void init(RestConfig config) throws Exception {
		if (isInitialized)
			return;
		createContext(config);
		super.init(config);
		init(context);
	}

	/**
	 * Convenience method if you want to perform initialization on your resource after all configuration settings
	 * have been made.
	 * <p>
	 * This allows you to get access to the {@link RestContext} object during initialization.
	 * <p>
	 * The default implementation does nothing.
	 *
	 * @param context The servlet context containing all the set-in-stone configurations for this resource.
	 * @throws Exception Any exception can be thrown to signal an initialization failure.
	 */
	public synchronized void init(RestContext context) throws Exception {}


	private synchronized void createContext(RestConfig config) throws Exception {
		if (isInitialized)
			return;
		this.config = config;
		this.context = new RestContext(this, config);
		this.isInitialized = true;
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * The main service method.
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
				throw new RestException(SC_INTERNAL_SERVER_ERROR, "Servlet not initialized.  init(RestServletConfig) was not called.");
			if (! isInitialized)
				throw new RestException(SC_INTERNAL_SERVER_ERROR, "Servlet has not been initialized");

			context.getCallHandler().service(r1, r2);

		} catch (RestException e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		} catch (Throwable e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		}
	}

	/**
	 * Returns the read-only context object that contains all the configuration information about this resource.
	 * <p>
	 * This object is <jk>null</jk> during the call to {@link #init(RestConfig)} but is populated
	 * by the time {@link #init()} is called.
	 * <p>
	 * Resource classes that don't extend from {@link RestServlet} can add the following method to their
	 * class to get access to this context object:
	 * <p class='bcode'>
	 * 	<jk>public void</jk> init(RestServletContext context) <jk>throws</jk> Exception;
	 * </p>
	 *
	 * @return The context information on this servlet.
	 */
	protected RestContext getContext() {
		return context;
	}

	/**
	 * Callback method for listening for successful completion of requests.
	 * <p>
	 * Subclasses can override this method for gathering performance statistics.
	 * <p>
	 * The default implementation does nothing.
	 * <p>
	 * Resources that don't extend from {@link RestServlet} can implement an equivalent method by
	 * 	overriding the {@link RestCallHandler#onSuccess(RestRequest, RestResponse, long)} method.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param time The time in milliseconds it took to process the request.
	 */
	protected void onSuccess(RestRequest req, RestResponse res, long time) {}

	/**
	 * Callback method that gets invoked right before the REST Java method is invoked.
	 * <p>
	 * Subclasses can override this method to override request headers or set request-duration properties
	 * 	before the Java method is invoked.
	 * <p>
	 * Resources that don't extend from {@link RestServlet} can implement an equivalent method by
	 * 	overriding the {@link RestCallHandler#onPreCall(RestRequest)} method.
	 *
	 * @param req The HTTP servlet request object.
	 * @throws RestException If any error occurs.
	 */
	protected void onPreCall(RestRequest req) throws RestException {}

	/**
	 * Callback method that gets invoked right after the REST Java method is invoked, but before
	 * 	the serializer is invoked.
	 * <p>
	 * Subclasses can override this method to override request and response headers, or
	 * 	set/override properties used by the serializer.
	 * <p>
	 * Resources that don't extend from {@link RestServlet} can implement an equivalent method by
	 * 	overriding the {@link RestCallHandler#onPostCall(RestRequest,RestResponse)} method.
	 *
	 * @param req The HTTP servlet request object.
	 * @param res The HTTP servlet response object.
	 * @throws RestException If any error occurs.
	 */
	protected void onPostCall(RestRequest req, RestResponse res) throws RestException {}

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
	}

	@Override /* GenericServlet */
	public RestConfig getServletConfig() {
		return config;
	}

	@Override /* GenericServlet */
	public void destroy() {
		if (context != null)
			context.destroy();
		super.destroy();
	}

	/**
	 * Convenience method for calling <code>getContext().getMessages();</code>
	 * @return The resource bundle for this resource.  Never <jk>null</jk>.
	 * @see RestContext#getProperties()
	 */
	public MessageBundle getMessages() {
		return context.getMessages();
	}

	/**
	 * Convenience method for calling <code>getContext().getProperties();</code>
	 * @return The resource properties as an {@link ObjectMap}.
	 * @see RestContext#getProperties()
	 */
	public ObjectMap getProperties() {
		return getContext().getProperties();
	}

	/**
	 * Convenience method for calling <code>getContext().getBeanContext();</code>
	 * @return The bean context used for parsing path variables and header values.
	 * @see RestContext#getBeanContext()
	 */
	public BeanContext getBeanContext() {
		return getContext().getBeanContext();
	}
}
