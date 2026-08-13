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

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

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
	 * @param cause The underlying cause. Can be <jk>null</jk> (no cause is chained).
	 */
	public TransportException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The underlying cause. Can be <jk>null</jk> (no cause is chained).
	 */
	public TransportException(Throwable cause) {
		super(cause);
	}

	/**
	 * Returns <jk>true</jk> if the given throwable (or any cause in its chain) is the signature of a pre-response
	 * stale pooled-connection failure — the server tore down a kept-alive connection before sending any response
	 * bytes.
	 *
	 * <p>
	 * This is recognized across transports as an {@link EOFException} or {@link ClosedChannelException}, a
	 * connection-reset {@link SocketException}, or the JDK {@link java.net.http.HttpClient}'s
	 * {@code "HTTP/1.1 header parser received no bytes"} message.  Such a failure is safe to replay exactly once on
	 * a fresh connection provided the request cannot cause a duplicate side effect (an idempotent HTTP method, or —
	 * at a higher protocol layer — a side-effect-free operation).
	 *
	 * @param t The throwable to inspect. Can be <jk>null</jk> (returns <jk>false</jk>).
	 * @return <jk>true</jk> if the throwable indicates a pre-response stale-connection failure.
	 */
	public static boolean isStaleConnectionFailure(Throwable t) {
		for (var t2 = t; t2 != null; t2 = t2.getCause()) {
			if (t2 instanceof EOFException || t2 instanceof ClosedChannelException)
				return true;
			var msg = t2.getMessage();
			if (msg != null) {
				var m = msg.toLowerCase(Locale.ROOT);
				if (m.contains("received no bytes") || (t2 instanceof SocketException && m.contains("reset")))
					return true;
			}
		}
		return false;
	}
}
