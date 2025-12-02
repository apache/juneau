/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.text.*;

import org.apache.http.*;
import org.apache.juneau.http.header.*;

/**
 * Exception representing a <c>400+</c> HTTP response code against a remote resource or other exception.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClientBasics">juneau-rest-client Basics</a>
 * </ul>
 *
 * @serial exclude
 */
public class RestCallException extends HttpException {

	private static final long serialVersionUID = 1L;

	// HttpException has a bug involving ASCII control characters so just replace them with spaces.
	private static String clean(String message) {
		message = emptyIfNull(message);

		boolean needsCleaning = false;
		for (var i = 0; i < message.length() && ! needsCleaning; i++)
			if (message.charAt(i) < 32)
				needsCleaning = true;

		if (! needsCleaning)
			return message;

		var sb = new StringBuilder(message.length());
		for (var i = 0; i < message.length(); i++) {
			var c = message.charAt(i);
			sb.append(c < 32 ? ' ' : c);
		}

		return sb.toString();
	}

	private static String format(String msg, Object...args) {
		if (args.length == 0)
			return clean(msg);
		return clean(f(msg, args));
	}

	private final int statusCode;

	private final Thrown thrown;

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP response status code.  Use <c>0</c> if no connection could be made.
	 * @param thrown The value of the <js>"Thrown"</js> header on the response.  Can be <jk>null</jk>.
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestCallException(int statusCode, Thrown thrown, Throwable cause, String message, Object...args) {
		super(format(message, args), cause);
		this.statusCode = statusCode;
		this.thrown = thrown;
	}

	/**
	 * Constructor.
	 *
	 * @param response The HTTP response.  Can be <jk>null</jk>.
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestCallException(RestResponse response, Throwable cause, String message, Object...args) {
		this((response == null ? 0 : response.getStatusCode()), (response == null ? Thrown.EMPTY : response.getHeader("Thrown").as(Thrown.class).orElse(null)), cause, message, args);
	}

	/**
	 * Similar to {@link #getCause()} but searches until it finds the throwable of the specified type.
	 *
	 * @param <T> The throwable type.
	 * @param c The throwable type.
	 * @return The cause of the specified type, or <jk>null</jk> of not found.
	 */
	public <T extends Throwable> T getCause(Class<T> c) {
		return getThrowableCause(c, this);
	}

	/**
	 * Returns the HTTP response status code.
	 *
	 * @return The response status code.  If a connection could not be made at all, returns <c>0</c>.
	 */
	public int getResponseCode() { return statusCode; }

	/**
	 * Returns the value of the <js>"Thrown"</js> header on the response.
	 *
	 * @return The value of the <js>"Thrown"</js> header on the response, never <jk>null</jk>.
	 */
	public Thrown getThrown() { return thrown; }
}