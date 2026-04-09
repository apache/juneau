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
package org.apache.juneau.ng.rest.client;

import java.io.*;

/**
 * Exception thrown when an HTTP call fails at the REST client level (e.g. unexpected status code, body
 * deserialization failure, or interceptor rejection).
 *
 * <p>
 * Network-level failures are reported as {@link TransportException}.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
public class NgRestCallException extends IOException {

	private static final long serialVersionUID = 1L;

	private final int statusCode;

	/**
	 * Constructor — for cases without an HTTP status code (e.g. deserialization failure).
	 *
	 * @param message The error message.
	 * @param cause The underlying cause. May be <jk>null</jk>.
	 */
	public NgRestCallException(String message, Throwable cause) {
		super(message, cause);
		this.statusCode = -1;
	}

	/**
	 * Constructor — for cases with an HTTP status code (e.g. unexpected 4xx/5xx).
	 *
	 * @param statusCode The HTTP status code.
	 * @param message The error message.
	 */
	public NgRestCallException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	/**
	 * Constructor — for cases with an HTTP status code and a cause.
	 *
	 * @param statusCode The HTTP status code.
	 * @param message The error message.
	 * @param cause The underlying cause. May be <jk>null</jk>.
	 */
	public NgRestCallException(int statusCode, String message, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
	}

	/**
	 * Returns the HTTP status code, or {@code -1} if not applicable.
	 *
	 * @return The status code.
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
