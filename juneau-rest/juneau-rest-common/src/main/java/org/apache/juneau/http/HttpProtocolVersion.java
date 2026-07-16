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
package org.apache.juneau.http;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.function.*;

/**
 * The HTTP protocol version that appears in request and response start lines (e.g. {@code "HTTP/1.1"}).
 *
 * <p>
 * Mirrors the semantics of {@code org.apache.http.ProtocolVersion} without the Apache HttpCore dependency.
 * The protocol name and major/minor numbers are stored separately so callers can compare versions numerically.
 *
 * <p>
 * Create instances via {@link #of(String, int, int)} or {@link #parse(String)}:
 * <p class='bjava'>
 * 	<jc>// "HTTP/1.1"</jc>
 * 	HttpProtocolVersion <jv>v1</jv> = HttpProtocolVersion.<jsf>HTTP_1_1</jsf>;
 *
 * 	<jc>// Parse from a wire string</jc>
 * 	HttpProtocolVersion <jv>v2</jv> = HttpProtocolVersion.<jsm>parse</jsm>(<js>"HTTP/2.0"</js>);
 * </p>
 *
 * @param protocol The protocol name (e.g. {@code "HTTP"}). Must not be <jk>null</jk>.
 * @param major The major version number (e.g. {@code 1}).
 * @param minor The minor version number (e.g. {@code 1}).
 *
 * @since 10.0.0
 */
public record HttpProtocolVersion(String protocol, int major, int minor) {

	/** {@code HTTP/1.0}. */
	public static final HttpProtocolVersion HTTP_1_0 = new HttpProtocolVersion("HTTP", 1, 0);

	/** {@code HTTP/1.1}. */
	public static final HttpProtocolVersion HTTP_1_1 = new HttpProtocolVersion("HTTP", 1, 1);

	/** {@code HTTP/2.0}. */
	public static final HttpProtocolVersion HTTP_2_0 = new HttpProtocolVersion("HTTP", 2, 0);

	/**
	 * Canonical constructor.
	 *
	 * @param protocol The protocol name. Must not be <jk>null</jk>.
	 * @param major The major version (must be {@code >= 0}).
	 * @param minor The minor version (must be {@code >= 0}).
	 */
	public HttpProtocolVersion {
		assertArgNotNull("protocol", protocol);
		if (major < 0)
			throw new IllegalArgumentException("Major version must be >= 0, got " + major);
		if (minor < 0)
			throw new IllegalArgumentException("Minor version must be >= 0, got " + minor);
	}

	/**
	 * Creates a protocol version with the given protocol name and version numbers.
	 *
	 * @param protocol The protocol name (e.g. {@code "HTTP"}). Must not be <jk>null</jk>.
	 * @param major The major version number.
	 * @param minor The minor version number.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpProtocolVersion of(String protocol, int major, int minor) {
		return new HttpProtocolVersion(protocol, major, minor);
	}

	/**
	 * Parses a wire-format protocol version string.
	 *
	 * <p>
	 * Accepts the standard {@code PROTOCOL/major.minor} form (e.g. {@code "HTTP/1.1"}) and the abbreviated
	 * {@code PROTOCOL/major} form (e.g. {@code "HTTP/2"}, which is parsed as major=2, minor=0).
	 *
	 * @param s The protocol version string. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the string cannot be parsed.
	 */
	public static HttpProtocolVersion parse(String s) {
		assertArgNotNull("s", s);
		var slash = s.indexOf('/');
		if (slash < 0)
			throw iaex("Invalid protocol version: %s", s);
		var protocol = s.substring(0, slash);
		var rest = s.substring(slash + 1);
		var dot = rest.indexOf('.');
		Supplier<RuntimeException> err = () -> iaex("Invalid protocol version: %s", s);
		if (dot < 0)
			return new HttpProtocolVersion(protocol, parseInt(rest, err), 0);
		return new HttpProtocolVersion(protocol, parseInt(rest.substring(0, dot), err), parseInt(rest.substring(dot + 1), err));
	}

	/**
	 * Returns the canonical wire-format string (e.g. {@code "HTTP/1.1"}).
	 *
	 * @return The wire-format string. Never <jk>null</jk>.
	 */
	@Override /* Object */
	public String toString() {
		return protocol + "/" + major + "." + minor;
	}
}
