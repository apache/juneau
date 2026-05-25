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
package org.apache.juneau.rest.auth;

import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;

/**
 * Specialization of {@link Unauthorized} thrown by {@link BearerTokenGuard}, {@link ApiKeyGuard}, and
 * user-supplied {@link TokenValidator} / {@link ApiKeyStore} implementations to signal AuthN failure.
 *
 * <p>
 * Behaves like a plain {@code 401 Unauthorized} from the wire's perspective &mdash; same status code,
 * same reason phrase, same JSON / problem-details body shape &mdash; but adds a typed
 * {@link #wwwAuthenticate(String)} fluent setter that returns {@code AuthenticationException} (instead
 * of the generic {@link BasicHttpException} returned by the inherited {@code setHeader(...)} chain) so
 * authoring AuthN guards stays terse:
 *
 * <p class='bjava'>
 * 	<jk>throw new</jk> AuthenticationException(<js>"Bearer token missing"</js>)
 * 		.wwwAuthenticate(<js>"Bearer realm=\"api\""</js>);
 * </p>
 *
 * <p>
 * The {@code WWW-Authenticate} response header is mandated by RFC 7235 &sect;4.1 for every {@code 401}
 * response &mdash; clients use the challenge string to discover which authentication schemes the server
 * supports and to format their next request. AuthN guards in this package set it on every rejection.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Unauthorized}
 * 	<li class='jc'>{@link BearerTokenGuard}
 * 	<li class='jc'>{@link ApiKeyGuard}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7235#section-4.1">RFC 7235 &sect;4.1 &mdash; WWW-Authenticate</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class AuthenticationException extends Unauthorized {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor with no message.
	 */
	public AuthenticationException() {
		super();
	}

	/**
	 * Constructor with a {@link java.text.MessageFormat}- or {@link String#format(String, Object...) String.format}-style message.
	 *
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public AuthenticationException(String msg, Object...args) {
		super(msg, args);
	}

	/**
	 * Constructor with a cause.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public AuthenticationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor with a cause and a {@link java.text.MessageFormat}- or {@link String#format(String, Object...) String.format}-style message.
	 *
	 * @param cause The cause. May be <jk>null</jk>.
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public AuthenticationException(Throwable cause, String msg, Object...args) {
		super(cause, msg, args);
	}

	/**
	 * Sets the {@code WWW-Authenticate} response header to the supplied challenge string.
	 *
	 * <p>
	 * Per RFC 7235 &sect;4.1, the challenge identifies the authentication scheme(s) the server accepts
	 * and any scheme-specific parameters (e.g. {@code realm}). Examples:
	 *
	 * <ul>
	 * 	<li>{@code Bearer realm="api"}
	 * 	<li>{@code Bearer realm="api", error="invalid_token", error_description="The access token expired"}
	 * 	<li>{@code ApiKey realm="api"}
	 * </ul>
	 *
	 * @param value The {@code WWW-Authenticate} challenge value. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public AuthenticationException wwwAuthenticate(String value) {
		setHeader(WwwAuthenticate.of(value));
		return this;
	}
}
