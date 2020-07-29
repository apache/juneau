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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.Enablement.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.util.*;

/**
 * Default implementation of {@link RestCallHandler}.
 *
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST calls are handled.
 * <br>Subclasses MUST implement a public constructor that takes in a {@link RestContext} object.
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestContext#REST_callHandler}
 * </ul>
 */
public class BasicRestCallHandler implements RestCallHandler {

	private final RestContext context;
	private final Map<String,RestCallRouter> restCallRouters;

	/**
	 * Constructor.
	 *
	 * @param context The resource context.
	 */
	public BasicRestCallHandler(RestContext context) {
		this.context = context;
		this.restCallRouters = context.getCallRouters();
	}

	@Override /* RestCallHandler */
	public RestCall createCall(HttpServletRequest req, HttpServletResponse res) {
		return new RestCall(req, res).logger(context.getCallLogger()).loggerConfig(context.getCallLoggerConfig());
	}

	@Override /* RestCallHandler */
	public RestRequest createRequest(RestCall call) throws ServletException {
		return new RestRequest(context, call.getRequest());
	}

	@Override /* RestCallHandler */
	public RestResponse createResponse(RestCall call) throws ServletException {
		return new RestResponse(context, call.getRestRequest(), call.getResponse());
	}

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 *
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException General servlet exception.
	 * @throws IOException Thrown by underlying stream.
	 */
	@Override /* RestCallHandler */
	public void execute(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		RestCall call = createCall(r1, r2);

		try {
			context.checkForInitException();

			// If the resource path contains variables (e.g. @Rest(path="/f/{a}/{b}"), then we want to resolve
			// those variables and push the servletPath to include the resolved variables.  The new pathInfo will be
			// the remainder after the new servletPath.
			// Only do this for the top-level resource because the logic for child resources are processed next.
			if (context.pathPattern.hasVars() && context.getParentContext() == null) {
				String sp = call.getServletPath();
				String pi = call.getPathInfoUndecoded();
				UrlPathInfo upi2 = new UrlPathInfo(pi == null ? sp : sp + pi);
				UrlPathPatternMatch uppm = context.pathPattern.match(upi2);
				if (uppm != null && ! uppm.hasEmptyVars()) {
					RequestPath.addPathVars(call.getRequest(), uppm.getVars());
					call.request(
						new OverrideableHttpServletRequest(call.getRequest())
							.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
							.servletPath(uppm.getPrefix())
					);
				} else {
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
					return;
				}
			}

			// If this resource has child resources, try to recursively call them.
			String pi = call.getPathInfoUndecoded();
			if (context.hasChildResources() && pi != null && ! pi.equals("/")) {
				for (RestContext rc : context.getChildResources().values()) {
					UrlPathPattern upp = rc.pathPattern;
					UrlPathPatternMatch uppm = upp.match(call.getUrlPathInfo());
					if (uppm != null) {
						if (! uppm.hasEmptyVars()) {
							RequestPath.addPathVars(call.getRequest(), uppm.getVars());
							HttpServletRequest childRequest = new OverrideableHttpServletRequest(call.getRequest())
								.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
								.servletPath(call.getServletPath() + uppm.getPrefix());
							rc.getCallHandler().execute(childRequest, call.getResponse());
						} else {
							call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
						}
						return;
					}
				}
			}

			if (isDebug(call))
				call.debug(true);

			context.startCall(call);

			call.restRequest(createRequest(call));
			call.restResponse(createResponse(call));

			context.setRequest(call.getRestRequest());
			context.setResponse(call.getRestResponse());

			StaticFile r = null;
			if (call.getPathInfoUndecoded() != null) {
				String p = call.getPathInfoUndecoded().substring(1);
				if (context.isStaticFile(p)) {
					r = context.getStaticFile(p);
					if (! r.exists()) {
						call.output(null);
						r = null;
					}
				} else if (p.equals("favicon.ico")) {
					call.output(null);
				}
			}

			if (r != null) {
				call.status(SC_OK);
				call.output(r);
			} else {

				// If the specified method has been defined in a subclass, invoke it.
				int rc = 0;
				String m = call.getMethod();

				if (restCallRouters.containsKey(m))
					rc = restCallRouters.get(m).invoke(call);

				if ((rc == 0 || rc == 404) && restCallRouters.containsKey("*"))
					rc = restCallRouters.get("*").invoke(call);

				// Should be 405 if the URL pattern matched but HTTP method did not.
				if (rc == 0)
					for (RestCallRouter rcc : restCallRouters.values())
						if (rcc.matches(call))
							rc = SC_METHOD_NOT_ALLOWED;

				// Should be 404 if URL pattern didn't match.
				if (rc == 0)
					rc = SC_NOT_FOUND;

				// If not invoked above, see if it's an OPTIONs request
				if (rc != SC_OK)
					handleNotFound(call.status(rc));

				if (call.getStatus() == 0)
					call.status(rc);
			}

			if (call.hasOutput()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				handleResponse(call);
			}


		} catch (Throwable e) {
			handleError(call, convertThrowable(e));
		} finally {
			context.clearState();
		}

		call.finish();
		context.finishCall(call);
	}

	private boolean isDebug(RestCall call) {
		Enablement e = null;
		RestMethodContext mc = call.getRestMethodContext();
		if (mc != null)
			e = mc.getDebug();
		if (e == null)
			e = context.getDebug();
		if (e == TRUE)
			return true;
		if (e == FALSE)
			return false;
		if (e == PER_REQUEST)
			return "true".equalsIgnoreCase(call.getRequest().getHeader("X-Debug"));
		return false;
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
	 * looking for the first one whose {@link ResponseHandler#handle(RestRequest,RestResponse)} method returns
	 * <jk>true</jk>.
	 *
	 * @param call The HTTP call.
	 * @throws IOException Thrown by underlying stream.
	 * @throws HttpException Non-200 response.
	 */
	@Override /* RestCallHandler */
	public void handleResponse(RestCall call) throws IOException, HttpException, NotImplemented {

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();

		// Loop until we find the correct handler for the POJO.
		for (ResponseHandler h : context.getResponseHandlers())
			if (h.handle(req, res))
				return;

		Object output = res.getOutput();
		throw new NotImplemented("No response handlers found to process output of type '"+(output == null ? null : output.getClass().getName())+"'");
	}

	/**
	 * Method that can be subclassed to allow uncaught throwables to be treated as other types of throwables.
	 *
	 * <p>
	 * The default implementation looks at the throwable class name to determine whether it can be converted to another type:
	 *
	 * <ul>
	 * 	<li><js>"*AccessDenied*"</js> - Converted to {@link Unauthorized}.
	 * 	<li><js>"*Empty*"</js>,<js>"*NotFound*"</js> - Converted to {@link NotFound}.
	 * </ul>
	 *
	 * @param t The thrown object.
	 * @return The converted thrown object.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Throwable convertThrowable(Throwable t) {

		ClassInfo ci = ClassInfo.ofc(t);
		if (ci.is(InvocationTargetException.class)) {
			t = ((InvocationTargetException)t).getTargetException();
			ci = ClassInfo.ofc(t);
		}

		if (ci.is(HttpRuntimeException.class)) {
			t = ((HttpRuntimeException)t).getInner();
			ci = ClassInfo.ofc(t);
		}

		if (ci.isChildOf(RestException.class) || ci.hasAnnotation(Response.class))
			return t;

		if (t instanceof ParseException || t instanceof InvalidDataConversionException)
			return new BadRequest(t);

		String n = t.getClass().getName();

		if (n.contains("AccessDenied") || n.contains("Unauthorized"))
			return new Unauthorized(t);

		if (n.contains("Empty") || n.contains("NotFound"))
			return new NotFound(t);

		return t;
	}

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param call The HTTP call.
	 */
	@Override /* RestCallHandler */
	public void handleNotFound(RestCall call) throws Exception {
		String pathInfo = call.getPathInfo();
		String methodUC = call.getMethod();
		int rc = call.getStatus();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new NotFound("Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new PreconditionFailed("Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new MethodNotAllowed("Method ''{0}'' not found on resource{1}.", methodUC, onPath);
		else
			throw new ServletException("Invalid method response: " + rc);
	}

	/**
	 * Method for handling response errors.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param call The rest call.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	@Override /* RestCallHandler */
	@SuppressWarnings("deprecation")
	public synchronized void handleError(RestCall call, Throwable e) throws IOException {

		call.exception(e);

		if (call.isDebug())
			e.printStackTrace();

		int occurrence = context == null ? 0 : context.getStackTraceOccurrence(e);

		int code = 500;

		ClassInfo ci = ClassInfo.ofc(e);
		Response r = ci.getLastAnnotation(Response.class);
		if (r != null)
			if (r.code().length > 0)
				code = r.code()[0];

		RestException e2 = (e instanceof RestException ? (RestException)e : new RestException(e, code)).setOccurrence(occurrence);

		HttpServletRequest req = call.getRequest();
		HttpServletResponse res = call.getResponse();

		Throwable t = null;
		if (e instanceof HttpRuntimeException)
			t = ((HttpRuntimeException)e).getInner();
		if (t == null)
			t = e2.getRootCause();
		if (t != null) {
			res.setHeader("Exception-Name", stripInvalidHttpHeaderChars(t.getClass().getName()));
			res.setHeader("Exception-Message", stripInvalidHttpHeaderChars(t.getMessage()));
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
			req.setAttribute("Exception", e1);
		}
	}

	/**
	 * Returns the session objects for the specified request.
	 *
	 * <p>
	 * The default implementation simply returns a single map containing <c>{'req':req}</c>.
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
