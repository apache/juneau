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
package org.apache.juneau.http.exception;

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Exception thrown to trigger an error HTTP status.
 *
 * <p>
 * REST methods on subclasses of <c>RestServlet</c> can throw this exception to trigger an HTTP status other than the
 * automatically-generated <c>404</c>, <c>405</c>, and <c>500</c> statuses.
 */
public class HttpException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int status;
	private AMap<String,Object> headers = AMap.of();

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param status The HTTP status code.
	 * @param msg The status message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public HttpException(Throwable cause, int status, String msg, Object...args) {
		super(message(cause, msg, args), cause);
		this.status = status;
	}

	/**
	 * Constructor.
	 *
	 * @param msg The status message.
	 */
	public HttpException(String msg) {
		super(msg, null);
	}

	private static String message(Throwable cause, String msg, Object...args) {
		if (msg == null && cause != null)
			return firstNonEmpty(cause.getLocalizedMessage(), cause.getClass().getName());
		return format(msg, args);
	}

	/**
	 * Constructor.
	 * @param cause The root exception.
	 * @param status The HTTP status code.
	 */
	public HttpException(Throwable cause, int status) {
		this(cause, status, null);
	}

	/**
	 * Constructor.
	 *
	 * @param status The HTTP status code.
	 * @param msg The status message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public HttpException(int status, String msg, Object...args) {
		this(null, status, msg, args);
	}

	/**
	 * Returns the root cause of this exception.
	 *
	 * <p>
	 * The root cause is the first exception in the init-cause parent chain that's not one of the following:
	 * <ul>
	 * 	<li>{@link HttpException}
	 * 	<li>{@link InvocationTargetException}
	 * </ul>
	 *
	 * @return The root cause of this exception, or <jk>null</jk> if no root cause was found.
	 */
	public Throwable getRootCause() {
		Throwable t = this;
		while(t != null) {
			t = t.getCause();
			if (! (t instanceof HttpException || t instanceof InvocationTargetException))
				return t;
		}
		return null;
	}

	/**
	 * Returns all error messages from all errors in this stack.
	 *
	 * <p>
	 * Typically useful if you want to render all the error messages in the stack, but don't want to render all the
	 * stack traces too.
	 *
	 * @param scrubForXssVulnerabilities
	 * 	If <jk>true</jk>, replaces <js>'&lt;'</js>, <js>'&gt;'</js>, and <js>'&amp;'</js> characters with spaces.
	 * @return All error messages from all errors in this stack.
	 */
	public String getFullStackMessage(boolean scrubForXssVulnerabilities) {
		String msg = getMessage();
		StringBuilder sb = new StringBuilder();
		if (msg != null) {
			if (scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			sb.append(msg);
		}
		Throwable e = getCause();
		while (e != null) {
			msg = e.getMessage();
			if (msg != null && scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			String cls = e.getClass().getSimpleName();
			if (msg == null)
				sb.append(format("\nCaused by ({0})", cls));
			else
				sb.append(format("\nCaused by ({0}): {1}", cls, msg));
			e = e.getCause();
		}
		return sb.toString();
	}

	@Override /* Object */
	public int hashCode() {
		int i = 0;
		Throwable t = this;
		while (t != null) {
			for (StackTraceElement e : t.getStackTrace())
			i ^= e.hashCode();
			t = t.getCause();
		}
		return i;
	}

	/**
	 * Set the status code on this exception.
	 *
	 * @param status The status code.
	 * @return This object (for method chaining).
	 */
	protected HttpException setStatus(int status) {
		this.status = status;
		return this;
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The HTTP status code.
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Add an HTTP header to this exception.
	 *
	 * @param name The header name.
	 * @param val The header value.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public HttpException header(String name, Object val) {
		headers.a(name, val);
		return this;
	}

	/**
	 * Returns the headers associated with this exception.
	 *
	 * @return The headers associated with this exception.
	 */
	@ResponseHeader("*")
	@BeanIgnore
	public Map<String,Object> getHeaders() {
		return headers;
	}

	// When serialized, just serialize the message itself.
	@Override /* Object */
	public String toString() {
		return emptyIfNull(getLocalizedMessage());
	}

	// <CONFIGURATION-PROPERTIES>

	// </CONFIGURATION-PROPERTIES>
}
