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
package org.apache.juneau.ng.http.response;

import java.util.*;

import org.apache.juneau.ng.http.*;
import org.apache.juneau.ng.http.entity.*;

/**
 * Base class for HTTP error response exceptions (4xx and 5xx) in the {@code org.apache.juneau.ng.http} stack.
 *
 * <p>
 * Extends {@link RuntimeException} while also implementing {@link HttpResponseMessage}, so that HTTP
 * error responses can be both thrown as exceptions and used as response objects.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class BasicHttpException extends RuntimeException implements HttpResponseMessage {

	private static final long serialVersionUID = 1L;

	private final HttpStatusLine statusLine;
	private final List<HttpHeader> headers;
	private final HttpBody body;

	/**
	 * Constructor with no message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase (e.g. {@code "Internal Server Error"}). May be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase) {
		this(statusCode, reasonPhrase, null, null, List.of());
	}

	/**
	 * Constructor with a plain-text message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param message The exception message and response body. May be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, String message) {
		this(statusCode, reasonPhrase, message, null, List.of());
	}

	/**
	 * Constructor with a cause and plain-text message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param message The exception message. May be <jk>null</jk>.
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, String message, Throwable cause) {
		this(statusCode, reasonPhrase, message, cause, List.of());
	}

	/**
	 * Full constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param message The exception message. May be <jk>null</jk>.
	 * @param cause The cause. May be <jk>null</jk>.
	 * @param headers Additional response headers. Must not be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, String message, Throwable cause, List<HttpHeader> headers) {
		super(message, cause);
		this.statusLine = HttpStatusLineBean.of(statusCode, reasonPhrase);
		this.headers = List.copyOf(headers);
		this.body = message != null ? StringBody.of(message) : null;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	protected BasicHttpException(BasicHttpException copyFrom) {
		this(copyFrom.statusLine.getStatusCode(), copyFrom.statusLine.getReasonPhrase(), copyFrom.getMessage(), copyFrom.getCause(), copyFrom.headers);
	}

	@Override /* HttpResponseMessage */
	public HttpStatusLine getStatusLine() {
		return statusLine;
	}

	@Override /* HttpResponseMessage */
	public List<HttpHeader> getHeaders() {
		return headers;
	}

	@Override /* HttpResponseMessage */
	public HttpBody getBody() {
		return body;
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The status code.
	 */
	public int getStatusCode() {
		return statusLine.getStatusCode();
	}

	@Override /* Object */
	public String toString() {
		return statusLine.toString();
	}
}
