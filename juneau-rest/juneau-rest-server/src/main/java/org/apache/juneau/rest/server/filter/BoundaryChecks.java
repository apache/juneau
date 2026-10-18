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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import jakarta.servlet.http.*;

/**
 * Request predicates shared by the {@link RequestBoundary} implementations in this package.
 *
 * <p>
 * Kept in one place so two boundaries cannot come to disagree about, say, which content types are JSON.
 */
final class BoundaryChecks {

	private static final String JSON = "application/json";

	private static final String HTTP = "http";
	private static final String HTTPS = "https";

	private BoundaryChecks() {}

	/**
	 * Whether a {@code Content-Type} header value's base type is exactly {@code application/json}.
	 *
	 * <p>
	 * Parameters are ignored, so {@code application/json;charset=utf-8} passes.  A suffixed type such as
	 * {@code application/problem+json} does not: the check exists to exclude the form-encodable types, and widening it
	 * to "anything ending in json" would be a per-type judgement call at a security boundary.
	 *
	 * @param contentType The header value.  Can be <jk>null</jk>, which is not JSON.
	 * @return <jk>true</jk> if the base type is {@code application/json}, compared case-insensitively.
	 */
	static boolean isJson(String contentType) {
		return eqic(baseType(contentType), JSON);
	}

	/**
	 * Whether a {@code Content-Type} header value's base type is one a cross-origin request may send without a CORS
	 * preflight: {@code application/x-www-form-urlencoded}, {@code multipart/form-data} or {@code text/plain}.
	 *
	 * <p>
	 * These are the types a plain HTML {@code <form>} can submit, which is why they matter at a CSRF boundary.
	 *
	 * @param contentType The header value.  Can be <jk>null</jk>, which is not one of them.
	 * @return <jk>true</jk> if the base type is one of the three, compared case-insensitively and ignoring parameters.
	 */
	static boolean isSafelistedType(String contentType) {
		var base = baseType(contentType);
		return eqic(base, "application/x-www-form-urlencoded") || eqic(base, "multipart/form-data") || eqic(base, "text/plain");
	}

	/**
	 * A parsed {@code Host} value: a lower-cased host and an optional port.
	 *
	 * @param host The host, lower-cased.  A bracketed IPv6 literal keeps its brackets.
	 * @param port The explicit port, or {@code -1} when none was given.
	 */
	record Authority(String host, int port) {}

	/**
	 * A parsed origin: scheme, host and port, with the port resolved to the scheme's default when absent.
	 *
	 * <p>
	 * Two origins are the same origin exactly when their records are equal.
	 *
	 * @param scheme The scheme, lower-cased: {@code http} or {@code https}.
	 * @param host The host, lower-cased.  A bracketed IPv6 literal keeps its brackets.
	 * @param port The port, never {@code -1}.
	 */
	record Origin(String scheme, String host, int port) {}

	/**
	 * Parses a {@code Host} header value.
	 *
	 * <p>
	 * The value must be a reg-name, an IPv4 address or a bracketed IPv6 literal, optionally followed by
	 * <c>":"</c> and a decimal port from 1 to 65535 with no sign and no leading zero.  Only ASCII is accepted.
	 * Whitespace, control characters, {@code @}, {@code ,}, {@code /}, {@code \} and an IPv6 zone id ({@code %})
	 * all make the value malformed.  Nothing is re-canonicalized: {@code 127.1}, {@code [::ffff:127.0.0.1]} and a
	 * trailing dot are kept as written, so they compare unequal to other spellings of the same address.
	 *
	 * @param value The header value.  Can be <jk>null</jk>.
	 * @return The parsed value, or <jk>null</jk> if it is <jk>null</jk> or malformed.
	 */
	@SuppressWarnings({
		"java:S3776" // Strict header grammar: the bracketed and plain host branches read best as one sequence
	})
	static Authority parseHost(String value) {
		if (value == null || value.isEmpty())
			return null;
		String host;
		String rest;
		if (value.charAt(0) == '[') {
			var close = value.indexOf(']');
			if (close < 0)
				return null;
			host = value.substring(0, close + 1);
			rest = value.substring(close + 1);
			if (! isIpv6Literal(host))
				return null;
		} else {
			var colon = value.indexOf(':');
			host = colon < 0 ? value : value.substring(0, colon);
			rest = colon < 0 ? "" : value.substring(colon);
			if (host.isEmpty() || ! isRegName(host))
				return null;
		}
		var port = -1;
		if (! rest.isEmpty()) {
			if (rest.charAt(0) != ':')
				return null;
			port = parsePort(rest.substring(1));
			if (port < 0)
				return null;
		}
		return new Authority(host.toLowerCase(Locale.ROOT), port);
	}

	/**
	 * Parses an {@code Origin} header value.
	 *
	 * <p>
	 * The value must be exactly <c>scheme "://" host [":" port]</c>, where the scheme is {@code http} or
	 * {@code https} (case-insensitive) and the host and port follow {@link #parseHost(String)}.  A path, a trailing
	 * slash, userinfo, a query, a fragment or whitespace anywhere makes it unparseable, as does any other scheme
	 * ({@code file}, {@code chrome-extension}, ...).  The literal {@code null} is not an origin either; a caller that
	 * gives it its own treatment must test for it first.
	 *
	 * @param value The header value.  Can be <jk>null</jk>.
	 * @return The parsed origin with its port resolved, or <jk>null</jk> if it is <jk>null</jk> or does not parse.
	 */
	static Origin parseOrigin(String value) {
		if (value == null)
			return null;
		var sep = value.indexOf("://");
		if (sep <= 0)
			return null;
		var scheme = value.substring(0, sep).toLowerCase(Locale.ROOT);
		var port = defaultPort(scheme);
		if (port < 0)
			return null;
		var a = parseHost(value.substring(sep + 3));
		if (a == null)
			return null;
		return new Origin(scheme, a.host(), a.port() < 0 ? port : a.port());
	}

	/**
	 * The default port of a web scheme.
	 *
	 * @param scheme The scheme, lower-cased.  Can be <jk>null</jk>.
	 * @return {@code 80} for {@code http}, {@code 443} for {@code https}, else {@code -1}.
	 */
	static int defaultPort(String scheme) {
		if (HTTP.equals(scheme))
			return 80;
		if (HTTPS.equals(scheme))
			return 443;
		return -1;
	}

	/**
	 * The web scheme of a WebSocket handshake URI scheme.
	 *
	 * @param scheme The URI scheme.  Can be <jk>null</jk>.
	 * @return {@code http} for {@code ws} or {@code http}, {@code https} for {@code wss} or {@code https} (ignoring
	 * 	case), else <jk>null</jk>.
	 */
	static String handshakeScheme(String scheme) {
		if (scheme == null)
			return null;
		return switch (scheme.toLowerCase(Locale.ROOT)) {
			case "ws", "http" -> HTTP;
			case "wss", HTTPS -> HTTPS;
			default -> null;
		};
	}

	/**
	 * Every value of a request header, in order, so that a duplicate can be counted.
	 *
	 * <p>
	 * A container may return <jk>null</jk> from {@link HttpServletRequest#getHeaders(String)} when it will not
	 * enumerate headers; the single {@link HttpServletRequest#getHeader(String)} value is then all there is.
	 *
	 * @param req The request.
	 * @param name The header name.
	 * @return The values; empty when the header is absent.  Never <jk>null</jk>.
	 */
	static List<String> headerValues(HttpServletRequest req, String name) {
		var e = req.getHeaders(name);
		if (e == null) {
			var v = req.getHeader(name);
			return v == null ? List.of() : List.of(v);
		}
		var l = new ArrayList<String>();
		while (e.hasMoreElements())
			l.add(e.nextElement());
		return l;
	}

	/**
	 * The {@code Host} values of a request; on HTTP/2 with no {@code Host} header, one value synthesised from the
	 * container's {@code :authority} view.
	 *
	 * <p>
	 * A container that speaks HTTP/2 may carry the authority only as a pseudo-header, which the servlet API surfaces as
	 * {@link HttpServletRequest#getServerName()} and {@link HttpServletRequest#getServerPort()} and not as a
	 * {@code Host} header.  The rules, in order:
	 * <ol>
	 * 	<li>Any real {@code Host} header is returned unchanged, repeated values included, so they fail as they do today.
	 * 	<li>Otherwise, unless {@link HttpServletRequest#getProtocol()} is exactly {@code HTTP/2.0}, the list is empty.
	 * 		Anything else ({@code HTTP/1.x}, <jk>null</jk>, {@code HTTP/2}, a different case) stays on the HTTP/1 path.
	 * 	<li>Otherwise one value is built from the server name (a bare IPv6 address is bracketed, as Tomcat reports it)
	 * 		and, when positive, <c>":"</c> plus the server port.  A <jk>null</jk> or blank server name gives the empty list.
	 * </ol>
	 *
	 * @param req The request.
	 * @return The values; empty when there is no {@code Host} and no authority to stand in for it.  Never <jk>null</jk>.
	 */
	static List<String> hostValues(HttpServletRequest req) {
		var hosts = headerValues(req, "Host");
		if (! hosts.isEmpty())
			return hosts;
		var synthesised = synthesisedHost(req);
		return synthesised == null ? List.of() : List.of(synthesised);
	}

	/**
	 * Whether {@link #hostValues(HttpServletRequest)} would return a value synthesised from the container's
	 * {@code :authority} view rather than a real {@code Host} header.
	 *
	 * @param req The request.
	 * @return <jk>true</jk> if the request has no {@code Host} header and is HTTP/2 with a usable server name.
	 */
	static boolean isSynthesisedHost(HttpServletRequest req) {
		return headerValues(req, "Host").isEmpty() && synthesisedHost(req) != null;
	}

	/** The {@code :authority} view of an HTTP/2 request, or <jk>null</jk> when the request is not HTTP/2 or names no server. */
	private static String synthesisedHost(HttpServletRequest req) {
		if (! "HTTP/2.0".equals(req.getProtocol()))
			return null;
		var name = req.getServerName();
		if (name == null || name.isBlank())
			return null;
		// Tomcat reports an IPv6 server name without the brackets a Host header would carry.
		if (name.indexOf(':') >= 0 && ! name.startsWith("["))
			name = "[" + name + "]";
		var port = req.getServerPort();
		return port > 0 ? name + ":" + port : name;
	}

	/**
	 * Whether a {@code Sec-Fetch-Site} value is exactly {@code same-origin}.
	 *
	 * <p>
	 * Case-sensitive and untrimmed: a browser sends this exact token, and anything else did not come from one.
	 *
	 * @param value The header value.  Can be <jk>null</jk>, which is not.
	 * @return <jk>true</jk> only for {@code same-origin}.
	 */
	static boolean isSameOriginFetch(String value) {
		return "same-origin".equals(value);
	}

	/** Whether {@code s} is all reg-name characters: ASCII letters, digits, {@code - . _ ~}. */
	private static boolean isRegName(String s) {
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (! (isAsciiAlnum(c) || c == '-' || c == '.' || c == '_' || c == '~'))
				return false;
		}
		return true;
	}

	/** Whether {@code s} is a bracketed IPv6 literal: hex digits, {@code :} and {@code .} only, with a colon, no zone id. */
	private static boolean isIpv6Literal(String s) {
		var inner = s.substring(1, s.length() - 1);
		if (inner.indexOf(':') < 0)
			return false;
		for (var i = 0; i < inner.length(); i++) {
			var c = inner.charAt(i);
			if (! ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == ':' || c == '.'))
				return false;
		}
		return true;
	}

	/** A decimal port 1-65535 with no sign and no leading zero, or {@code -1}. */
	private static int parsePort(String s) {
		if (s.isEmpty() || s.length() > 5 || s.charAt(0) == '0')
			return -1;
		var p = 0;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c < '0' || c > '9')
				return -1;
			p = p * 10 + (c - '0');
		}
		return p <= 65535 ? p : -1;
	}

	private static boolean isAsciiAlnum(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
	}

	/** The media type before any parameters, trimmed, or <jk>null</jk> for a <jk>null</jk> value. */
	private static String baseType(String contentType) {
		if (contentType == null)
			return null;
		var semi = contentType.indexOf(';');
		return (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
	}
}
