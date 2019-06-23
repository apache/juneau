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

import static org.apache.juneau.rest.Constants.*;
import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.http.StreamResource;
import org.apache.juneau.rest.RestContext.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.util.*;

/**
 * Default implementation of {@link RestCallHandler}.
 *
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST calls are handled.
 * <br>Subclasses MUST implement a public constructor that takes in a {@link RestContext} object.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_callHandler}
 * </ul>
 */
public class BasicRestCallHandler implements RestCallHandler {

	private final RestContext context;
	private final RestLogger logger;
	private final Map<String,RestCallRouter> restCallRouters;

	/**
	 * Constructor.
	 *
	 * @param context The resource context.
	 */
	public BasicRestCallHandler(RestContext context) {
		this.context = context;
		this.logger = context.getLogger();
		this.restCallRouters = context.getCallRouters();
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Subclasses may choose to override this method to provide a specialized request object.
	 *
	 * @param req The request object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped request object.
	 * @throws ServletException If any errors occur trying to interpret the request.
	 */
	@Override /* RestCallHandler */
	public RestRequest createRequest(HttpServletRequest req) throws ServletException {
		return new RestRequest(context, req);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(HttpServletRequest)}.
	 *
	 * <p>
	 * Subclasses may choose to override this method to provide a specialized response object.
	 *
	 * @param req The request object returned by {@link #createRequest(HttpServletRequest)}.
	 * @param res The response object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped response object.
	 * @throws ServletException If any errors occur trying to interpret the request or response.
	 */
	@Override /* RestCallHandler */
	public RestResponse createResponse(RestRequest req, HttpServletResponse res) throws ServletException {
		return new RestResponse(context, req, res);
	}

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 *
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override /* RestCallHandler */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		logger.log(FINE, "HTTP: {0} {1}", r1.getMethod(), r1.getRequestURI());
		long startTime = System.currentTimeMillis();
		RestRequest req = null;

		try {
			context.checkForInitException();

			String pathInfo = RestUtils.getPathInfoUndecoded(r1);  // Can't use r1.getPathInfo() because we don't want '%2F' resolved.
			UrlPathInfo upi = new UrlPathInfo(pathInfo);

			// If this resource has child resources, try to recursively call them.
			if (pathInfo != null && context.hasChildResources() && (! pathInfo.equals("/"))) {
				for (RestContext rc : context.getChildResources().values()) {
					UrlPathPattern upp = rc.pathPattern;
					final UrlPathPatternMatch uppm = upp.match(upi);
					if (uppm != null) {
						if (uppm.hasVars()) {
							@SuppressWarnings("unchecked")
							Map<String,String> vars = (Map<String,String>)r1.getAttribute(REST_PATHVARS_ATTR);
							if (vars == null) {
								vars = new TreeMap<>();
								r1.setAttribute(REST_PATHVARS_ATTR, vars);
							}
							vars.putAll(uppm.getVars());
						}
						final String afterMatch = uppm.getSuffix();
						final String servletPath = r1.getServletPath() + uppm.getPrefix();
						final HttpServletRequest childRequest = new HttpServletRequestWrapper(r1) {
							@Override /* ServletRequest */
							public String getPathInfo() {
								return urlDecode(afterMatch);
							}
							@Override /* ServletRequest */
							public String getServletPath() {
								return servletPath;
							}
						};
						rc.getCallHandler().service(childRequest, r2);
						return;
					}
				}
			}

			context.startCall(r1, r2);

			req = createRequest(r1);
			RestResponse res = createResponse(req, r2);
			req.setResponse(res);
			context.setRequest(req);
			context.setResponse(res);
			String method = req.getMethod();
			String methodUC = method.toUpperCase(Locale.ENGLISH);

			StreamResource r = null;
			if (pathInfo != null) {
				String p = pathInfo.substring(1);
				if (context.isStaticFile(p)) {
					StaticFile sf = context.resolveStaticFile(p);
					r = sf.resource;
					res.setResponseMeta(sf.meta);
				} else if (p.equals("favicon.ico")) {
					res.setOutput(null);
				}
			}

			if (r != null) {
				res.setStatus(SC_OK);
				res.setOutput(r);
			} else {
				// If the specified method has been defined in a subclass, invoke it.
				int rc = SC_METHOD_NOT_ALLOWED;
				if (restCallRouters.containsKey(methodUC)) {
					rc = restCallRouters.get(methodUC).invoke(upi, req, res);
				} else if (restCallRouters.containsKey("*")) {
					rc = restCallRouters.get("*").invoke(upi, req, res);
				}

				// If not invoked above, see if it's an OPTIONs request
				if (rc != SC_OK)
					handleNotFound(rc, req, res);

				if (res.getStatus() == 0)
					res.setStatus(rc);
			}

			if (res.hasOutput()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				handleResponse(req, res);
			}

			// Make sure our writer in RestResponse gets written.
			res.flushBuffer();
			req.close();

			r1.setAttribute("ExecTime", System.currentTimeMillis() - startTime);

		} catch (Throwable e) {
			r1.setAttribute("Exception", e);
			r1.setAttribute("ExecTime", System.currentTimeMillis() - startTime);
			handleError(r1, r2, e);
		} finally {
			context.clearState();
		}

		context.finishCall(r1, r2);

		logger.log(FINE, "HTTP: [{0} {1}] finished in {2}ms", r1.getMethod(), r1.getRequestURI(), System.currentTimeMillis()-startTime);
	}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or
	 * returned by the Java method.
	 *
	 * <p>
	 * Subclasses may override this method if they wish to modify the way the output is rendered or support other output
	 * formats.
	 *
	 * <p>
	 * The default implementation simply iterates through the response handlers on this resource
	 * looking for the first one whose {@link ResponseHandler#handle(RestRequest, RestResponse)} method returns
	 * <jk>true</jk>.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws IOException
	 * @throws RestException
	 */
	@Override /* RestCallHandler */
	public void handleResponse(RestRequest req, RestResponse res) throws IOException, RestException, NotImplemented {
		// Loop until we find the correct handler for the POJO.
		for (ResponseHandler h : context.getResponseHandlers())
			if (h.handle(req, res))
				return;
		Object output = res.getOutput();
		throw new NotImplemented("No response handlers found to process output of type '"+(output == null ? null : output.getClass().getName())+"'");
	}

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param rc The HTTP response code.
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 */
	@Override /* RestCallHandler */
	public void handleNotFound(int rc, RestRequest req, RestResponse res) throws NotFound, PreconditionFailed, MethodNotAllowed, ServletException {
		String pathInfo = req.getPathInfo();
		String methodUC = req.getMethod();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new NotFound("Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new PreconditionFailed("Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new MethodNotAllowed("Method ''{0}'' not found on resource.", methodUC);
		else
			throw new ServletException("Invalid method response: " + rc);
	}

	/**
	 * Method for handling response errors.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	@Override /* RestCallHandler */
	public synchronized void handleError(HttpServletRequest req, HttpServletResponse res, Throwable e) throws IOException {

		int occurrence = context == null ? 0 : context.getStackTraceOccurrence(e);
		RestException e2 = (e instanceof RestException ? (RestException)e : new RestException(e, 500)).setOccurrence(occurrence);

		Throwable t = e2.getRootCause();
		if (t != null) {
			res.setHeader("Exception-Name", t.getClass().getName());
			res.setHeader("Exception-Message", t.getMessage());
		}

		try {
			res.setContentType("text/plain");
			res.setHeader("Content-Encoding", "identity");
			res.setStatus(e2.getStatus());

			PrintWriter w = null;
			try {
				w = res.getWriter();
			} catch (IllegalStateException x) {
				w = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), UTF8));
			}

			try (PrintWriter w2 = w) {
				String httpMessage = RestUtils.getHttpResponseText(e2.getStatus());
				if (httpMessage != null)
					w2.append("HTTP ").append(String.valueOf(e2.getStatus())).append(": ").append(httpMessage).append("\n\n");
				if (context != null && context.isRenderResponseStackTraces())
					e.printStackTrace(w2);
				else
					w2.append(e2.getFullStackMessage(true));
			}

		} catch (Exception e1) {
			logger.onError(req, res, new RestException(e1, 0));
		}

		if (context.isDebug()) {
			String qs = req.getQueryString();
			String msg = '[' + Integer.toHexString(e.hashCode()) + '.' + e2.getStatus() + '.' + e2.getOccurrence() + "] HTTP " + req.getMethod() + " " + e2.getStatus() + " " + req.getRequestURI() + (qs == null ? "" : "?" + qs);
			System.err.println(msg);  // NOT DEBUG
			e.printStackTrace(System.err);
			logger.log(Level.SEVERE, e, e.getLocalizedMessage());
		}

		logger.onError(req, res, e2);
	}

	/**
	 * Returns the session objects for the specified request.
	 *
	 * <p>
	 * The default implementation simply returns a single map containing <code>{'req':req}</code>.
	 *
	 * @param req The REST request.
	 * @return The session objects for that request.
	 */
	@Override /* RestCallHandler */
	public Map<String,Object> getSessionObjects(RestRequest req, RestResponse res) {
		Map<String,Object> m = new HashMap<>();
		m.put("req", req);
		m.put("res", res);
		return m;
	}
}
