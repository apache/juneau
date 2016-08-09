/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import java.text.*;

/**
 * Exception thrown to trigger an error HTTP status.
 * <p>
 * 	REST methods on subclasses of {@link RestServlet} can throw
 * 	this exception to trigger an HTTP status other than the automatically-generated
 * 	<code>404</code>, <code>405</code>, and <code>500</code> statuses.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class RestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int status;
	private int occurrence;

	/**
	 * Constructor.
	 *
	 * @param status The HTTP status code.
	 * @param msg The status message.
	 * @param args Optional string format arguments.
	 */
	public RestException(int status, String msg, Object...args) {
		super(args.length == 0 ? msg : MessageFormat.format(msg, args));
		this.status = status;
	}

	/**
	 * Constructor.
	 *
	 * @param status The HTTP status code.
	 * @param cause The root exception.
	 */
	public RestException(int status, Throwable cause) {
		this(status, cause.getLocalizedMessage());
		initCause(cause);
	}


	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized RestException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}


	/**
	 * Returns all error messages from all errors in this stack.
	 * <p>
	 * Typically useful if you want to render all the error messages in the stack, but don't
	 * want to render all the stack traces too.
	 *
	 * @param scrubForXssVulnerabilities If <jk>true</jk>, replaces <js>'&lt;'</js>, <js>'&gt;'</js>, and <js>'&amp;'</js> characters with spaces.
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
				sb.append(MessageFormat.format("\nCaused by ({0})", cls));
			else
				sb.append(MessageFormat.format("\nCaused by ({0}): {1}", cls, msg));
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

	void setOccurrence(int occurrence) {
		this.occurrence = occurrence;
	}

	/**
	 * Returns the number of times this exception occurred on this servlet.
	 * <p>
	 * This only gets set if {@link RestServletContext#REST_useStackTraceHashes} is enabled on the servlet.
	 *
	 * @return The occurrence number if {@link RestServletContext#REST_useStackTraceHashes} is enabled, or <code>0</code> otherwise.
	 */
	public int getOccurrence() {
		return occurrence;
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The HTTP status code.
	 */
	public int getStatus() {
		return status;
	}
}
