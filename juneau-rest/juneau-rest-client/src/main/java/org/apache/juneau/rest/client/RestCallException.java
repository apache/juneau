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

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;

import org.apache.http.*;
import org.apache.juneau.internal.*;

/**
 * Exception representing a <c>400+</c> HTTP response code against a remote resource or other exception.
 */
public final class RestCallException extends HttpException {

	private static final long serialVersionUID = 1L;

	private RestResponse response;

	/**
	 * Constructor.
	 *
	 * @param response The HTTP response.  Can be <jk>null</jk>.
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestCallException(RestResponse response, Throwable cause, String message, Object...args) {
		super(format(message,args),cause);
		this.response = response;
	}

	/**
	 * Returns the value of the <js>"Exception-Name"</js> header on the response.
	 *
	 * @return The value of the <js>"Exception-Name"</js> header on the response, or <jk>null</jk> if not found.
	 */
	public String getServerExceptionName() {
		return response == null ? null : response.getStringHeader("Exception-Name").orElse(null);
	}

	/**
	 * Returns the value of the <js>"Exception-Message"</js> header on the response.
	 *
	 * @return The value of the <js>"Exception-Message"</js> header on the response, or <jk>null</jk> if not found.
	 */
	public String getServerExceptionMessage() {
		return response == null ? null : response.getStringHeader("Exception-Message").orElse(null);
	}

	/**
	 * Returns the HTTP response object that caused this exception.
	 *
	 * @return
	 * 	The HTTP response object that caused this exception, or <jk>null</jk> if no response was created yet when the
	 * 	exception was thrown.
	 */
	public RestResponse getResponse() {
		return this.response;
	}

	/**
	 * Returns the HTTP response status code.
	 *
	 * @return The response status code.  If a connection could not be made at all, returns <c>0</c>.
	 */
	public int getResponseCode() {
		return response == null ? 0 : response.getStatusCode();
	}

	/**
	 * Similar to {@link #getCause()} but searches until it finds the throwable of the specified type.
	 *
	 * @param <T> The throwable type.
	 * @param c The throwable type.
	 * @return The cause of the specified type, or <jk>null</jk> of not found.
	 */
	public <T extends Throwable> T getCause(Class<T> c) {
		return ThrowableUtils.getCause(c, this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	private static String format(String msg, Object...args) {
		if (args.length == 0)
			return clean(msg);
		return clean(StringUtils.format(msg, args));
	}

	// HttpException has a bug involving ASCII control characters so just replace them with spaces.
	private static String clean(String message) {
		message = emptyIfNull(message);

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
}
