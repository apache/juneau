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
 * Thrown by {@link HttpTransport#execute(TransportRequest)} when a network-level error occurs (e.g. connection
 * refused, timeout, SSL handshake failure).
 *
 * <p>
 * This exception wraps the underlying transport library's exception so that callers do not need to handle
 * Apache HttpClient, OkHttp, or JDK {@code HttpClient} exceptions directly.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
public class TransportException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 */
	public TransportException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 * @param cause The underlying cause.
	 */
	public TransportException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The underlying cause.
	 */
	public TransportException(Throwable cause) {
		super(cause);
	}
}
