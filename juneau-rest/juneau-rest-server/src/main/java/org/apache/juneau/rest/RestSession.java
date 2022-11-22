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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.util.*;

/**
 * Represents a single HTTP request.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class RestSession extends ContextSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request attribute name for passing path variables from parent to child.
	 */
	private static final String REST_PATHVARS_ATTR = "juneau.pathVars";

	/**
	 * Creates a builder of this object.
	 *
	 * @param ctx The context creating this builder.
	 * @return A new builder.
	 */
	public static Builder create(RestContext ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends ContextSession.Builder {

		RestContext ctx;
		Object resource;
		HttpServletRequest req;
		HttpServletResponse res;
		CallLogger logger;
		String pathInfoUndecoded;
		UrlPath urlPath;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(RestContext ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		/**
		 * Specifies the servlet implementation bean.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder resource(Object value) {
			resource = value;
			return this;
		}

		/**
		 * Specifies the HTTP servlet request object on this call.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder req(HttpServletRequest value) {
			req = value;
			return this;
		}

		/**
		 * Returns the HTTP servlet request object on this call.
		 *
		 * @return The HTTP servlet request object on this call.
		 */
		public HttpServletRequest req() {
			urlPath = null;
			pathInfoUndecoded = null;
			return req;
		}

		/**
		 * Specifies the HTTP servlet response object on this call.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder res(HttpServletResponse value) {
			res = value;
			return this;
		}

		/**
		 * Returns the HTTP servlet response object on this call.
		 *
		 * @return The HTTP servlet response object on this call.
		 */
		public HttpServletResponse res() {
			return res;
		}

		/**
		 * Specifies the logger to use for this session.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder logger(CallLogger value) {
			logger = value;
			return this;
		}

		@Override /* Session.Builder */
		public RestSession build() {
			return new RestSession(this);
		}

		/**
		 * Returns the request path info as a {@link UrlPath} bean.
		 *
		 * @return The request path info as a {@link UrlPath} bean.
		 */
		public UrlPath getUrlPath() {
			if (urlPath == null)
				urlPath = UrlPath.of(getPathInfoUndecoded());
			return urlPath;
		}

		/**
		 * Returns the request path info as a {@link UrlPath} bean.
		 *
		 * @return The request path info as a {@link UrlPath} bean.
		 */
		public String getPathInfoUndecoded() {
			if (pathInfoUndecoded == null)
				pathInfoUndecoded = RestUtils.getPathInfoUndecoded(req);
			return pathInfoUndecoded;
		}

		/**
		 * Adds resolved <c><ja>@Resource</ja>(path)</c> variable values to this call.
		 *
		 * @param value The variables to add to this call.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder pathVars(Map<String,String> value) {
			if (value != null && ! value.isEmpty()) {
				Map<String,String> m = (Map<String,String>)req.getAttribute(REST_PATHVARS_ATTR);
				if (m == null) {
					m = new TreeMap<>();
					req.setAttribute(REST_PATHVARS_ATTR, m);
				}
				m.putAll(value);
			}
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private final Object resource;
	private final RestContext context;
	private HttpServletRequest req;
	private HttpServletResponse res;

	private CallLogger logger;
	private UrlPath urlPath;
	private String pathInfoUndecoded;
	private long startTime = System.currentTimeMillis();
	private BeanStore beanStore;
	private Map<String,String[]> queryParams;
	private String method;
	private RestOpSession opSession;

	private UrlPathMatch urlPathMatch;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public RestSession(Builder builder) {
		super(builder);
		context = builder.ctx;
		resource = builder.resource;
		beanStore = BeanStore.of(context.getBeanStore(), resource).addBean(RestContext.class, context);

		req = beanStore.add(HttpServletRequest.class, builder.req);
		res = beanStore.add(HttpServletResponse.class, builder.res);
		logger = beanStore.add(CallLogger.class, builder.logger);
		urlPath = beanStore.add(UrlPath.class, builder.urlPath);
		pathInfoUndecoded = builder.pathInfoUndecoded;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the logger to use when logging this call.
	 *
	 * @param value The new value for this setting.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RestSession logger(CallLogger value) {
		logger = beanStore.add(CallLogger.class, value);
		return this;
	}

	/**
	 * Enables or disabled debug mode on this call.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 * @throws IOException Occurs if request content could not be cached into memory.
	 */
	public RestSession debug(boolean value) throws IOException {
		if (value) {
			req = CachingHttpServletRequest.wrap(req);
			res = CachingHttpServletResponse.wrap(res);
			req.setAttribute("Debug", true);
		} else {
			req.removeAttribute("Debug");
		}
		return this;
	}

	/**
	 * Sets the HTTP status on this call.
	 *
	 * @param value The status code.
	 * @return This object.
	 */
	public RestSession status(int value) {
		res.setStatus(value);
		return this;
	}

	/**
	 * Sets the HTTP status on this call.
	 *
	 * @param value The status code.
	 * @return This object.
	 */
	@SuppressWarnings("deprecation")
	public RestSession status(StatusLine value) {
		if (value != null)
			res.setStatus(value.getStatusCode(), value.getReasonPhrase());
		return this;
	}

	/**
	 * Identifies that an exception occurred during this call.
	 *
	 * @param value The thrown exception.
	 * @return This object.
	 */
	public RestSession exception(Throwable value) {
		req.setAttribute("Exception", value);
		beanStore.addBean(Throwable.class, value);
		return this;
	}

	/**
	 * Sets the URL path pattern match on this call.
	 *
	 * @param value The match pattern.
	 * @return This object.
	 */
	public RestSession urlPathMatch(UrlPathMatch value) {
		urlPathMatch = beanStore.add(UrlPathMatch.class, value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Getters
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP servlet request of this REST call.
	 *
	 * @return the HTTP servlet request of this REST call.
	 */
	public HttpServletRequest getRequest() {
		return req;
	}

	/**
	 * Returns the HTTP servlet response of this REST call.
	 *
	 * @return the HTTP servlet response of this REST call.
	 */
	public HttpServletResponse getResponse() {
		return res;
	}

	/**
	 * Returns the bean store of this call.
	 *
	 * @return The bean store of this call.
	 */
	public BeanStore getBeanStore() {
		return beanStore;
	}

	/**
	 * Returns resolved <c><ja>@Resource</ja>(path)</c> variable values on this call.
	 *
	 * @return Resolved <c><ja>@Resource</ja>(path)</c> variable values on this call.
	 */
	@SuppressWarnings("unchecked")
	public Map<String,String> getPathVars() {
		Map<String,String> m = (Map<String,String>)req.getAttribute(REST_PATHVARS_ATTR);
		return m == null ? Collections.emptyMap() : m;
	}

	/**
	 * Returns the URL path pattern match on this call.
	 *
	 * @return The URL path pattern match on this call.
	 */
	public UrlPathMatch getUrlPathMatch() {
		return urlPathMatch;
	}

	/**
	 * Returns the exception that occurred during this call.
	 *
	 * @return The exception that occurred during this call.
	 */
	public Throwable getException() {
		return (Throwable)req.getAttribute("Exception");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lifecycle methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Called at the end of a call to finish any remaining tasks such as flushing buffers and logging the response.
	 *
	 * @return This object.
	 */
	public RestSession finish() {
		try {
			req.setAttribute("ExecTime", System.currentTimeMillis() - startTime);
			if (opSession != null)
				opSession.finish();
			else {
				res.flushBuffer();
			}
		} catch (Exception e) {
			exception(e);
		}
		if (logger != null)
			logger.log(req, res);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pass-through convenience methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling <c>getRequest().getServletPath()</c>.
	 *
	 * @return The request servlet path.
	 */
	public String getServletPath() {
		return req.getServletPath();
	}

	/**
	 * Returns the request path info as a {@link UrlPath} bean.
	 *
	 * @return The request path info as a {@link UrlPath} bean.
	 */
	public UrlPath getUrlPath() {
		if (urlPath == null)
			urlPath = UrlPath.of(getPathInfoUndecoded());
		return urlPath;
	}

	/**
	 * Shortcut for calling <c>getRequest().getPathInfo()</c>.
	 *
	 * @return The request servlet path info.
	 */
	public String getPathInfo() {
		return req.getPathInfo();
	}

	/**
	 * Same as {@link #getPathInfo()} but doesn't decode encoded characters.
	 *
	 * @return The undecoded request servlet path info.
	 */
	public String getPathInfoUndecoded() {
		if (pathInfoUndecoded == null)
			pathInfoUndecoded = RestUtils.getPathInfoUndecoded(req);
		return pathInfoUndecoded;
	}

	/**
	 * Returns the query parameters on the request.
	 *
	 * <p>
	 * Unlike {@link HttpServletRequest#getParameterMap()}, this doesn't parse the content if it's a POST.
	 *
	 * @return The query parameters on the request.
	 */
	public Map<String,String[]> getQueryParams() {
		if (queryParams == null) {
			if (req.getMethod().equalsIgnoreCase("POST"))
				queryParams = RestUtils.parseQuery(req.getQueryString(), map());
			else
				queryParams = req.getParameterMap();
		}
		return queryParams;
	}

	/**
	 * Returns the HTTP method name.
	 *
	 * @return The HTTP method name, always uppercased.
	 */
	public String getMethod() {
		if (method == null) {

			Set<String> s1 = context.getAllowedMethodParams();
			Set<String> s2 = context.getAllowedMethodHeaders();

			if (! s1.isEmpty()) {
				String[] x = getQueryParams().get("method");
				if (x != null && (s1.contains("*") || s1.contains(x[0])))
					method = x[0];
			}

			if (method == null && ! s2.isEmpty()) {
				String x = req.getHeader("X-Method");
				if (x != null && (s2.contains("*") || s2.contains(x)))
					method = x;
			}

			if (method == null)
				method = req.getMethod();

			method = method.toUpperCase(Locale.ENGLISH);
		}

		return method;
	}

	/**
	 * Shortcut for calling <c>getRequest().getStatus()</c>.
	 *
	 * @return The response status code.
	 */
	public int getStatus() {
		return res.getStatus();
	}

	/**
	 * Returns the context that created this call.
	 *
	 * @return The context that created this call.
	 */
	@Override
	public RestContext getContext() {
		return context;
	}

	/**
	 * Returns the REST object.
	 *
	 * @return The rest object.
	 */
	public Object getResource() {
		return resource;
	}

	/**
	 * Returns the operation session of this REST session.
	 *
	 * <p>
	 * The operation session is created once the Java method to be invoked has been determined.
	 *
	 * @return The operation session of this REST session.
	 * @throws InternalServerError If operation session has not been created yet.
	 */
	public RestOpSession getOpSession() throws InternalServerError {
		if (opSession == null)
			throw new InternalServerError("Op Session not created.");
		return opSession;
	}

	/**
	 * Runs this session.
	 *
	 * <p>
	 * Does the following:
	 * <ol>
	 * 	<li>Finds the Java method to invoke and creates a {@link RestOpSession} for it.
	 * 	<li>Invokes {@link RestPreCall} methods by calling {@link RestContext#preCall(RestOpSession)}.
	 * 	<li>Invokes Java method by calling {@link RestOpSession#run()}.
	 * 	<li>Invokes {@link RestPostCall} methods by calling {@link RestContext#postCall(RestOpSession)}.
	 * 	<li>If the Java method produced output, finds the response processor for it and runs it by calling {@link RestContext#processResponse(RestOpSession)}.
	 * 	<li>If no Java method matched, generates a 404/405/412 by calling {@link RestContext#handleNotFound(RestSession)}.
	 * </ol>
	 *
	 * @throws Throwable Any throwable can be thrown.
	 */
	public void run() throws Throwable {
		try {
			opSession = context.getRestOperations().findOperation(this).createSession(this).build();
			context.preCall(opSession);
			opSession.run();
			context.postCall(opSession);
			if (res.getStatus() == 0)
				res.setStatus(200);
			if (opSession.getResponse().hasContent()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				context.processResponse(opSession);
			}
		} catch (NotFound e) {
			if (getStatus() == 0)
				status(404);
			exception(e);
			context.handleNotFound(this);
		}
	}
}
