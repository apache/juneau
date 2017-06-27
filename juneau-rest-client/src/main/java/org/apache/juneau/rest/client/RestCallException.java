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
package org.apache.juneau.rest.client;

import static java.lang.String.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.util.*;

/**
 * Exception representing a <code>400+</code> HTTP response code against a remote resource.
 */
public final class RestCallException extends IOException {

	private static final long serialVersionUID = 1L;

	private int responseCode;
	private String response, responseStatusMessage;
	HttpResponseException e;
	private HttpResponse httpResponse;

	@SuppressWarnings("unused")
	private String serverExceptionName, serverExceptionMessage, serverExceptionTrace;


	/**
	 * Constructor.
	 *
	 * @param msg The exception message.
	 */
	public RestCallException(String msg) {
		super(msg);
	}

	/**
	 * Constructor.
	 *
	 * @param e The inner cause of the exception.
	 */
	public RestCallException(Exception e) {
		super(e.getLocalizedMessage(), e);
		if (e instanceof FileNotFoundException) {
			responseCode = 404;
		} else if (e.getMessage() != null) {
			Pattern p = Pattern.compile("[^\\d](\\d{3})[^\\d]");
			Matcher m = p.matcher(e.getMessage());
			if (m.find())
				responseCode = Integer.parseInt(m.group(1));
		}
		setStackTrace(e.getStackTrace());
	}

	/**
	 * Create an exception with a simple message and the status code and body of the specified response.
	 *
	 * @param msg The exception message.
	 * @param response The HTTP response object.
	 * @throws ParseException
	 * @throws IOException
	 */
	public RestCallException(String msg, HttpResponse response) throws ParseException, IOException {
		super(format("%s%nstatus='%s'%nResponse: %n%s%n", msg, response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity(), UTF8)));
	}

	/**
	 * Constructor.
	 *
	 * @param responseCode The response code.
	 * @param responseMsg The response message.
	 * @param method The HTTP method (for message purposes).
	 * @param url The HTTP URL (for message purposes).
	 * @param response The response from the server.
	 */
	public RestCallException(int responseCode, String responseMsg, String method, URI url, String response) {
		super(format("HTTP method '%s' call to '%s' caused response code '%s,%s'.%nResponse: %n%s%n", method, url, responseCode, responseMsg, response));
		this.responseCode = responseCode;
		this.responseStatusMessage = responseMsg;
		this.response = response;
	}

	/**
	 * Sets the server-side exception details.
	 *
	 * @param exceptionName The <code>Exception-Name:</code> header specifying the full name of the exception.
	 * @param exceptionMessage
	 * 	The <code>Exception-Message:</code> header specifying the message returned by {@link Throwable#getMessage()}.
	 * @param exceptionTrace The stack trace of the exception returned by {@link Throwable#printStackTrace()}.
	 * @return This object (for method chaining).
	 */
	protected RestCallException setServerException(Header exceptionName, Header exceptionMessage, Header exceptionTrace) {
		if (exceptionName != null)
			serverExceptionName = exceptionName.getValue();
		if (exceptionMessage != null)
			serverExceptionMessage = exceptionMessage.getValue();
		if (exceptionTrace != null)
			serverExceptionTrace = exceptionTrace.getValue();
		return this;
	}

	/**
	 * Tries to reconstruct and re-throw the server-side exception.
	 *
	 * <p>
	 * The exception is based on the following HTTP response headers:
	 * <ul>
	 * 	<li><code>Exception-Name:</code> - The full class name of the exception.
	 * 	<li><code>Exception-Message:</code> - The message returned by {@link Throwable#getMessage()}.
	 * 	<li><code>Exception-Trace:</code> - The stack trace of the exception returned by {@link Throwable#printStackTrace()}.
	 * </ul>
	 *
	 * <p>
	 * Does nothing if the server-side exception could not be reconstructed.
	 *
	 * <p>
	 * Currently only supports <code>Throwables</code> with either a public no-arg constructor
	 * or a public constructor that takes in a simple string message.
	 *
	 * @param cl The classloader to use to resolve the throwable class name.
	 * @throws Throwable If the throwable could be reconstructed.
	 */
	protected void throwServerException(ClassLoader cl) throws Throwable {
		if (serverExceptionName != null) {
			Throwable t = null;
			try {
				Class<?> exceptionClass = cl.loadClass(serverExceptionName);
				Constructor<?> c = findPublicConstructor(exceptionClass, String.class);
				if (c != null)
					t = (Throwable)c.newInstance(serverExceptionMessage);
				if (t == null) {
					c = findPublicConstructor(exceptionClass);
					if (c != null)
						t = (Throwable)c.newInstance();
				}
			} catch (Exception e2) { /* Ignore */ }
			if (t != null)
				throw t;
		}
	}

	/**
	 * Sets the HTTP response object that caused this exception.
	 *
	 * @param httpResponse The HTTP response object.
	 * @return This object (for method chaining).
	 */
	protected RestCallException setHttpResponse(HttpResponse httpResponse) {
		this.httpResponse = httpResponse;
		return this;
	}

	/**
	 * Returns the HTTP response object that caused this exception.
	 *
	 * @return
	 * 	The HTTP response object that caused this exception, or <jk>null</jk> if no response was created yet when the
	 * 	exception was thrown.
	 */
	public HttpResponse getHttpResponse() {
		return this.httpResponse;
	}

	/**
	 * Returns the HTTP response status code.
	 *
	 * @return The response status code.  If a connection could not be made at all, returns <code>0</code>.
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * Returns the HTTP response message body text.
	 *
	 * @return The response message body text.
	 */
	public String getResponseMessage() {
		return response;
	}

	/**
	 * Returns the response status message as a plain string.
	 *
	 * @return The response status message.
	 */
	public String getResponseStatusMessage() {
		return responseStatusMessage;
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized RestCallException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
