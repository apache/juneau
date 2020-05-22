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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.util.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Exception representing a <c>400+</c> HTTP response code against a remote resource or other exception.
 */
public final class RestCallException extends HttpException {

	private static final long serialVersionUID = 1L;

	private int responseCode;
	private String response, responseStatusMessage;
	HttpResponseException e;
	private RestResponse restResponse;

	@SuppressWarnings("unused")
	private String serverExceptionName, serverExceptionMessage, serverExceptionTrace;

	/**
	 * Converts the specified exception to a {@link RestCallException} by either casting or wrapping.
	 *
	 * @param e The exception to wrap.
	 * @return The casted or wrapped exception.
	 */
	public static RestCallException create(Exception e) {
		if (e instanceof RestCallException)
			return (RestCallException)e;
		return new RestCallException(e);
	}

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestCallException(String message, Object...args) {
		super(format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestCallException(Throwable cause, String message, Object...args) {
		this(getMessage(cause, message, null), args);
		initCause(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param e The inner cause of the exception.
	 */
	public RestCallException(Exception e) {
		super(clean(e.getLocalizedMessage()), e);
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
	 */
	public RestCallException(String msg, HttpResponse response) {
		super(clean(format("{0}\n{1}\nstatus=''{2}''\nResponse: \n{3}", msg, response.getStatusLine().getStatusCode(), readEntity(response))));
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
		super(format("HTTP method ''{0}'' call to ''{1}'' caused response code ''{2}, {3}''.\nResponse: \n{4}", method, url, responseCode, responseMsg, response));
		this.responseCode = responseCode;
		this.responseStatusMessage = responseMsg;
		this.response = response;
	}

	/**
	 * Sets the server-side exception details.
	 *
	 * @param exceptionName The <c>Exception-Name:</c> header specifying the full name of the exception.
	 * @param exceptionMessage
	 * 	The <c>Exception-Message:</c> header specifying the message returned by {@link Throwable#getMessage()}.
	 * @param exceptionTrace The stack trace of the exception returned by {@link Throwable#printStackTrace()}.
	 * @return This object (for method chaining).
	 */
	protected RestCallException setServerException(String exceptionName, String exceptionMessage, String exceptionTrace) {
		if (exceptionName != null)
			serverExceptionName = exceptionName;
		if (exceptionMessage != null)
			serverExceptionMessage = exceptionMessage;
		if (exceptionTrace != null)
			serverExceptionTrace = exceptionTrace;
		return this;
	}

	/**
	 * Tries to reconstruct and re-throw the server-side exception.
	 *
	 * <p>
	 * The exception is based on the following HTTP response headers:
	 * <ul>
	 * 	<li><c>Exception-Name:</c> - The full class name of the exception.
	 * 	<li><c>Exception-Message:</c> - The message returned by {@link Throwable#getMessage()}.
	 * 	<li><c>Exception-Trace:</c> - The stack trace of the exception returned by {@link Throwable#printStackTrace()}.
	 * </ul>
	 *
	 * <p>
	 * Does nothing if the server-side exception could not be reconstructed.
	 *
	 * <p>
	 * Currently only supports <c>Throwables</c> with either a public no-arg constructor
	 * or a public constructor that takes in a simple string message.
	 *
	 * @param cl The classloader to use to resolve the throwable class name.
	 * @param throwables The possible throwables.
	 * @throws Throwable If the throwable could be reconstructed.
	 */
	protected void throwServerException(ClassLoader cl, Class<?>...throwables) throws Throwable {
		if (serverExceptionName != null) {
			for (Class<?> t : throwables)
				if (t.getName().endsWith(serverExceptionName))
					doThrow(t, serverExceptionMessage);
			try {
				ClassInfo t = ClassInfo.of(cl.loadClass(serverExceptionName));
				if (t.isChildOf(RuntimeException.class) || t.isChildOf(Error.class))
					doThrow(t.inner(), serverExceptionMessage);
			} catch (ClassNotFoundException e2) { /* Ignore */ }
		}
	}

	private void doThrow(Class<?> t, String msg) throws Throwable {
		ConstructorInfo c = null;
		ClassInfo ci = ClassInfo.of(t);
		if (msg != null) {
			c = ci.getPublicConstructor(String.class);
			if (c != null)
				throw c.<Throwable>invoke(msg);
			c = ci.getPublicConstructor(Object.class);
			if (c != null)
				throw c.<Throwable>invoke(msg);
		}
		c = ci.getPublicConstructor();
		if (c != null)
			throw c.<Throwable>invoke();
	}

	/**
	 * Sets the HTTP response object that caused this exception.
	 *
	 * @param restResponse The REST response object.
	 * @return This object (for method chaining).
	 */
	protected RestCallException setRestResponse(RestResponse restResponse) {
		this.restResponse = restResponse;
		return this;
	}

	/**
	 * Returns the HTTP response object that caused this exception.
	 *
	 * @return
	 * 	The HTTP response object that caused this exception, or <jk>null</jk> if no response was created yet when the
	 * 	exception was thrown.
	 */
	public RestResponse getRestResponse() {
		return this.restResponse;
	}

	/**
	 * Returns the HTTP response status code.
	 *
	 * @return The response status code.  If a connection could not be made at all, returns <c>0</c>.
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
	 * Similar to {@link #getCause()} but searches until it finds the throwable of the specified type.
	 *
	 * @param <T> The throwable type.
	 * @param c The throwable type.
	 * @return The cause of the specified type, or <jk>null</jk> of not found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Throwable> T getCause(Class<T> c) {
		Throwable t = this;
		do {
			if (c.isInstance(t))
				return (T)t;
			t = t.getCause();
		} while (t != null);
		return null;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	private static String getMessage(Throwable cause, String msg, String def) {
		if (msg != null)
			return clean(msg);
		if (cause != null)
			return clean(cause.getMessage());
		return def;
	}

	private static String format(String msg, Object...args) {
		if (args.length == 0)
			return clean(msg);
		return clean(StringUtils.format(msg, args));
	}

	// HttpException has a bug involving ASCII control characters so just replace them with spaces.
	private static String clean(String message) {

		if (message == null)
			return "";

		boolean needsCleaning = false;
		for (int i = 0; i < message.length() && !needsCleaning; i++)
			if (message.charAt(i) < 32)
				needsCleaning = true;

		if (!needsCleaning)
			return message;

		StringBuilder sb = new StringBuilder(message.length());
		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			sb.append(c < 32 ? ' ' : c);
		}

		return sb.toString();
	}

	private static String readEntity(HttpResponse response) {
		try {
			return EntityUtils.toString(response.getEntity(), UTF8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
