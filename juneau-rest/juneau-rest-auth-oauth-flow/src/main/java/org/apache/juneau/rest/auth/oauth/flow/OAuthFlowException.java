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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Runtime exception thrown by any OAuth flow helper when the IdP rejects the request, the HTTP round-trip
 * fails, or the response can't be parsed.
 *
 * <p>
 * Unchecked because flow acquisition is typically called from startup or service-initialization code where
 * checked exceptions would bloat call sites.
 *
 * @since 10.0.0
 */
public class OAuthFlowException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String errorCode;

	/**
	 * Constructor.
	 *
	 * @param msg The detail message.  May be {@code null}.
	 */
	public OAuthFlowException(String msg) {
		super(msg);
		errorCode = null;
	}

	/**
	 * Constructor.
	 *
	 * @param msg The detail message.  May be {@code null}.
	 * @param cause The cause.  May be {@code null}.
	 */
	public OAuthFlowException(String msg, Throwable cause) {
		super(msg, cause);
		errorCode = null;
	}

	/**
	 * Constructor carrying the OAuth error code returned by the IdP (RFC 6749 &sect;5.2).
	 *
	 * @param msg The detail message.  May be {@code null}.
	 * @param errorCode The OAuth {@code error} code (e.g. {@code "invalid_grant"}).  May be {@code null}.
	 */
	public OAuthFlowException(String msg, String errorCode) {
		super(msg);
		this.errorCode = errorCode;
	}

	/**
	 * Returns the OAuth {@code error} code the IdP returned, if this exception represents a token-endpoint error
	 * response (RFC 6749 &sect;5.2).
	 *
	 * @return The error code (e.g. {@code "invalid_grant"}), or {@link Optional#empty()} if not an IdP error response.
	 */
	public Optional<String> errorCode() {
		return o(errorCode);
	}
}
