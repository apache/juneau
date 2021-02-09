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
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;

/**
 * A wrapper around a single HttpServletRequest/HttpServletResponse pair.
 */
public class RestCall {

	/**
	 * Request attribute name for passing path variables from parent to child.
	 */
	private static final String REST_PATHVARS_ATTR = "juneau.pathVars";

	private Object resource;
	private HttpServletRequest req;
	private HttpServletResponse res;
	private RestRequest rreq;
	private RestResponse rres;
	private RestContext context;
	private RestOperationContext opContext;
	private UrlPath urlPath;
	private String pathInfoUndecoded;
	private long startTime = System.currentTimeMillis();
	private RestLogger logger;
	private BeanFactory beanFactory;
	private Map<String,String[]> queryParams;
	private String method;

	private UrlPathMatch urlPathMatch;

	/**
	 * Constructor.
	 *
	 * @param resource The REST object.
	 * @param context The REST context object.
	 * @param req The incoming HTTP servlet request object.
	 * @param res The incoming HTTP servlet response object.
	 */
	public RestCall(Object resource, RestContext context, HttpServletRequest req, HttpServletResponse res) {
		this.context = context;
		this.resource = resource;
		beanFactory = BeanFactory.of(context.getRootBeanFactory(), resource);
		beanFactory.addBean(RestContext.class, context);
		request(req).response(res);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Overrides the request object on the REST call.
	 *
	 * @param value The new HTTP servlet request.
	 * @return This object (for method chaining).
	 */
	public RestCall resource(Object value) {
		resource = value;
		return this;
	}

	/**
	 * Overrides the request object on the REST call.
	 *
	 * @param value The new HTTP servlet request.
	 * @return This object (for method chaining).
	 */
	public RestCall request(HttpServletRequest value) {
		req = value;
		urlPath = null;
		pathInfoUndecoded = null;
		beanFactory.addBean(HttpServletRequest.class, value);
		return this;
	}

	/**
	 * Overrides the response object on the REST call.
	 *
	 * @param value The new HTTP servlet response.
	 * @return This object (for method chaining).
	 */
	public RestCall response(HttpServletResponse value) {
		res = value;
		beanFactory.addBean(HttpServletResponse.class, value);
		return this;
	}

	/**
	 * Sets the method context on this call.
	 *
	 * Used for logging statistics on the method.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public RestCall restOperationContext(RestOperationContext value) {
		opContext = value;
		beanFactory.addBean(RestOperationContext.class, value);
		return this;
	}

	/**
	 * Set the {@link RestRequest} object on this REST call.
	 *
	 * @param value The {@link RestRequest} object on this REST call.
	 * @return This object (for method chaining).
	 */
	public RestCall restRequest(RestRequest value) {
		rreq = value;
		beanFactory.addBean(RestRequest.class, value);
		return this;
	}

	/**
	 * Set the {@link RestResponse} object on this REST call.
	 *
	 * @param value The {@link RestResponse} object on this REST call.
	 * @return This object (for method chaining).
	 */
	public RestCall restResponse(RestResponse value) {
		rres = value;
		rreq.setResponse(value);
		beanFactory.addBean(RestResponse.class, value);
		return this;
	}

	/**
	 * Sets the logger to use when logging this call.
	 *
	 * @param value The logger to use when logging this call.
	 * @return This object (for method chaining).
	 */
	public RestCall logger(RestLogger value) {
		logger = value;
		beanFactory.addBean(RestLogger.class, value);
		return this;
	}


	/**
	 * Adds resolved <c><ja>@Resource</ja>(path)</c> variable values to this call.
	 *
	 * @param value The variables to add to this call.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestCall pathVars(Map<String,String> value) {
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

	/**
	 * Enables or disabled debug mode on this call.
	 *
	 * @param value The debug flag value.
	 * @return This object (for method chaining).
	 * @throws IOException Occurs if request body could not be cached into memory.
	 */
	public RestCall debug(boolean value) throws IOException {
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
	 * @return This object (for method chaining).
	 */
	public RestCall status(int value) {
		res.setStatus(value);
		return this;
	}

	/**
	 * Identifies that an exception occurred during this call.
	 *
	 * @param value The thrown exception.
	 * @return This object (for method chaining).
	 */
	public RestCall exception(Throwable value) {
		req.setAttribute("Exception", value);
		beanFactory.addBean(Throwable.class, value);
		return this;
	}

	/**
	 * Sets metadata about the response.
	 *
	 * @param value The metadata about the response.
	 * @return This object (for method chaining).
	 */
	public RestCall responseMeta(ResponseBeanMeta value) {
		if (rres != null)
			rres.setResponseMeta(value);
		return this;
	}

	/**
	 * Sets the output object to serialize as the response of this call.
	 *
	 * @param value The response output POJO.
	 * @return This object (for method chaining).
	 */
	public RestCall output(Object value) {
		if (rres != null)
			rres.setOutput(value);
		return this;
	}

	/**
	 * Sets the URL path pattern match on this call.
	 *
	 * @param value The match pattern.
	 * @return This object (for method chaining).
	 */
	public RestCall urlPathMatch(UrlPathMatch value) {
		urlPathMatch = value;
		beanFactory.addBean(UrlPathMatch.class, value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Getters.
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
	 * Returns the REST request of this REST call.
	 *
	 * @return the REST request of this REST call.
	 * @throws InternalServerError If the RestRequest object has not yet been created on this call.
	 */
	public RestRequest getRestRequest() {
		return Optional.ofNullable(rreq).orElseThrow(()->new InternalServerError("RestRequest object has not yet been created."));
	}


	/**
	 * Returns the REST response of this REST call.
	 *
	 * @return the REST response of this REST call.
	 */
	public RestResponse getRestResponse() {
		return Optional.ofNullable(rres).orElseThrow(()->new InternalServerError("RestResponse object has not yet been created."));
	}

	/**
	 * Returns the method context of this call.
	 *
	 * @return The method context of this call.
	 */
	public RestOperationContext getRestOperationContext() {
		return opContext;
	}

	/**
	 * Returns the bean factory of this call.
	 *
	 * @return The bean factory of this call.
	 */
	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	/**
	 * Returns the Java method of this call.
	 *
	 * @return The java method of this call, or <jk>null</jk> if it hasn't been determined yet.
	 */
	public Method getJavaMethod() {
		return opContext == null ? null : opContext.getJavaMethod();
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
	 * @return This object (for method chaining).
	 */
	public RestCall finish() {
		try {
			if (rres != null)
				rres.flushBuffer();
			else
				res.flushBuffer();
			req.setAttribute("ExecTime", System.currentTimeMillis() - startTime);
			if (rreq != null)
				rreq.close();
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
	 * Unlike {@link HttpServletRequest#getParameterMap()}, this doesn't parse the content body if it's a POST.
	 *
	 * @return The query parameters on the request.
	 */
	public Map<String,String[]> getQueryParams() {
		if (queryParams == null) {
			if (req.getMethod().equalsIgnoreCase("POST"))
				queryParams = RestUtils.parseQuery(req.getQueryString(), new LinkedHashMap<>());
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
	 * Shortcut for calling <c>getRestResponse().hasOutput()</c>.
	 *
	 * @return <jk>true</jk> if response has output.
	 */
	public boolean hasOutput() {
		if (rres != null)
			return rres.hasOutput();
		return false;
	}

	/**
	 * Shortcut for calling <c>getRestResponse().getOutput()</c>.
	 *
	 * @return The response output.
	 */
	public Object getOutput() {
		if (rres != null)
			return rres.getOutput();
		return null;
	}

	/**
	 * Shortcut for calling <c>getRestRequest().isDebug()</c>.
	 *
	 * @return <jk>true</jk> if debug is enabled for this request.
	 */
	public boolean isDebug() {
		if (rreq != null)
			return rreq.isDebug();
		return false;
	}

	/**
	 * Returns the context that created this call.
	 *
	 * @return The context that created this call.
	 */
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
}
