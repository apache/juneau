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

import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.rest.util.*;

/**
 * A wrapper around a single HttpServletRequest/HttpServletResponse pair.
 */
public class RestCall {

	private HttpServletRequest req;
	private HttpServletResponse res;
	private RestRequest rreq;
	private RestResponse rres;
	private RestMethodContext rmethod;
	private UrlPathInfo urlPathInfo;
	private String pathInfoUndecoded;
	private long startTime = System.currentTimeMillis();
	private RestCallLogger logger;
	private RestCallLoggerConfig loggerConfig;

	/**
	 * Constructor.
	 *
	 * @param req The incoming HTTP servlet request object.
	 * @param res The incoming HTTP servlet response object.
	 */
	public RestCall(HttpServletRequest req, HttpServletResponse res) {
		request(req).response(res);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Request/response objects.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Overrides the request object on the REST call.
	 *
	 * @param req The new HTTP servlet request.
	 * @return This object (for method chaining).
	 */
	public RestCall request(HttpServletRequest req) {
		this.req = req;
		this.urlPathInfo = null;
		this.pathInfoUndecoded = null;
		return this;
	}

	/**
	 * Overrides the response object on the REST call.
	 *
	 * @param res The new HTTP servlet response.
	 * @return This object (for method chaining).
	 */
	public RestCall response(HttpServletResponse res) {
		this.res = res;
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
	public RestCall restMethodContext(RestMethodContext value) {
		this.rmethod = value;
		return this;
	}

	/**
	 * Set the {@link RestRequest} object on this REST call.
	 *
	 * @param rreq The {@link RestRequest} object on this REST call.
	 * @return This object (for method chaining).
	 */
	public RestCall restRequest(RestRequest rreq) {
		request(rreq);
		this.rreq = rreq;
		return this;
	}

	/**
	 * Set the {@link RestResponse} object on this REST call.
	 *
	 * @param rres The {@link RestResponse} object on this REST call.
	 * @return This object (for method chaining).
	 */
	public RestCall restResponse(RestResponse rres) {
		response(rres);
		this.rres = rres;
		this.rreq.setResponse(rres);
		return this;
	}

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
	 */
	public RestRequest getRestRequest() {
		return rreq;
	}

	/**
	 * Returns the REST response of this REST call.
	 *
	 * @return the REST response of this REST call.
	 */
	public RestResponse getRestResponse() {
		return rres;
	}

	/**
	 * Returns the method context of this call.
	 *
	 * @return The method context of this call.
	 */
	public RestMethodContext getRestMethodContext() {
		return rmethod;
	}

	/**
	 * Returns the Java method of this call.
	 *
	 * @return The java method of this call, or <jk>null</jk> if it hasn't been determined yet.
	 */
	public Method getJavaMethod() {
		return rmethod == null ? null : rmethod.method;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setters.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the logger to use when logging this call.
	 *
	 * @param logger The logger to use when logging this call.
	 * @return This object (for method chaining).
	 */
	public RestCall logger(RestCallLogger logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Sets the logging configuration to use when logging this call.
	 *
	 * @param config The logging configuration to use when logging this call.
	 * @return This object (for method chaining).
	 */
	public RestCall loggerConfig(RestCallLoggerConfig config) {
		this.loggerConfig = config;
		return this;
	}

	/**
	 * Enables or disabled debug mode on this call.
	 *
	 * @param b The debug flag value.
	 * @return This object (for method chaining).
	 * @throws IOException Occurs if request body could not be cached into memory.
	 */
	public RestCall debug(boolean b) throws IOException {
		if (b) {
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
	 * @param code The status code.
	 * @return This object (for method chaining).
	 */
	public RestCall status(int code) {
		res.setStatus(code);
		return this;
	}

	/**
	 * Identifies that an exception occurred during this call.
	 *
	 * @param e The thrown exception.
	 * @return This object (for method chaining).
	 */
	public RestCall exception(Throwable e) {
		req.setAttribute("Exception", e);
		return this;
	}

	/**
	 * Sets metadata about the response.
	 *
	 * @param meta The metadata about the response.
	 * @return This object (for method chaining).
	 */
	public RestCall responseMeta(ResponseBeanMeta meta) {
		if (rres != null)
			rres.setResponseMeta(meta);
		return this;
	}

	/**
	 * Sets the output object to serialize as the response of this call.
	 *
	 * @param output The response output POJO.
	 * @return This object (for method chaining).
	 */
	public RestCall output(Object output) {
		if (rres != null)
			rres.setOutput(output);
		return this;
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
			res.flushBuffer();
			req.setAttribute("ExecTime", System.currentTimeMillis() - startTime);
			if (rreq != null)
				rreq.close();
		} catch (Exception e) {
			exception(e);
		}
		if (logger != null)
			logger.log(loggerConfig, req, res);
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
	 * Returns the request path info as a {@link UrlPathInfo} bean.
	 *
	 * @return The request path info as a {@link UrlPathInfo} bean.
	 */
	public UrlPathInfo getUrlPathInfo() {
		if (urlPathInfo == null)
			urlPathInfo = new UrlPathInfo(getPathInfoUndecoded());
		return urlPathInfo;
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
	 * Returns the HTTP method name.
	 *
	 * @return The HTTP method name, always uppercased.
	 */
	public String getMethod() {
		if (rreq != null)
			return rreq.getMethod().toUpperCase(Locale.ENGLISH);
		return req.getMethod().toUpperCase(Locale.ENGLISH);
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
}
