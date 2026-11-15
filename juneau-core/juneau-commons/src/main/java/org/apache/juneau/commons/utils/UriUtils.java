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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;

/**
 * Various {@link URI}-related utility methods.
 *
 * @since 10.0.0
 */
public class UriUtils {

	/**
	 * Returns <jk>true</jk> if the specified URI either uses the <js>"https"</js> scheme or targets a loopback
	 * host.
	 *
	 * <p>
	 * A loopback host is one of <js>"localhost"</js> (case-insensitive), <js>"127.0.0.1"</js>, <js>"::1"</js>, or
	 * <js>"[::1]"</js>.  A plaintext (<js>"http"</js>) URI aimed at any other host returns <jk>false</jk>, as does
	 * a <jk>null</jk> or scheme-less (malformed) URI.  This is the check to make before transmitting anything
	 * sensitive to a caller-configured endpoint so that a plaintext transport is only tolerated for local
	 * development.
	 *
	 * @param uri The URI to check.  Can be <jk>null</jk> (returns <jk>false</jk>).
	 * @return <jk>true</jk> if the URI is <js>"https"</js> or targets a loopback host.
	 */
	public static boolean isSecureOrLoopback(URI uri) {
		if (uri == null)
			return false;
		if (eqic("https", uri.getScheme()))
			return true;
		return isLoopbackHost(uri.getHost());
	}

	/**
	 * Returns <jk>true</jk> if the specified host name is a loopback host.
	 *
	 * <p>
	 * A loopback host is one of <js>"localhost"</js> (case-insensitive), <js>"127.0.0.1"</js>, <js>"::1"</js>, or
	 * <js>"[::1]"</js>.  Other addresses in the <c>127.0.0.0/8</c> range and other spellings of the same addresses
	 * are not matched.
	 *
	 * @param host The host name.  Can be <jk>null</jk> (returns <jk>false</jk>).
	 * @return <jk>true</jk> if the host is a loopback host.
	 */
	public static boolean isLoopbackHost(String host) {
		return host != null && (eqic(host, "localhost") || eqa(host, "127.0.0.1", "[::1]", "::1"));
	}

	/**
	 * Parses a strict dotted-quad IPv4 literal.
	 *
	 * <p>
	 * The value must be exactly four decimal octets in the range 0-255 separated by dots, with no sign, no leading
	 * zeros and no surrounding whitespace.  No name lookup is done.
	 *
	 * <h4 class='section'>Example:</h4>
	 * <p class='bjava'>
	 * 	parseIpv4(<js>"10.0.0.1"</js>);  <jc>// {10, 0, 0, 1}</jc>
	 * 	parseIpv4(<js>"10.0.0.01"</js>); <jc>// null</jc>
	 * </p>
	 *
	 * @param s The string to parse.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return The four address bytes, or <jk>null</jk> if the string is not a strict dotted-quad literal.
	 */
	@SuppressWarnings({
		"java:S1168" // null signals an unparseable address; callers test for it
	})
	public static byte[] parseIpv4(String s) {
		if (s == null)
			return null;
		var parts = s.split("\\.", -1);
		if (parts.length != 4)
			return null;
		var out = new byte[4];
		for (var i = 0; i < 4; i++) {
			var v = parseOctet(parts[i]);
			if (v < 0)
				return null;
			out[i] = (byte)v;
		}
		return out;
	}

	// Returns the value of a 1-3 digit decimal octet with no leading zero, or -1 if it isn't one or exceeds 255.
	private static int parseOctet(String p) {
		var n = p.length();
		if (n == 0 || n > 3 || (n > 1 && p.charAt(0) == '0'))
			return -1;
		var v = 0;
		for (var i = 0; i < n; i++) {
			var c = p.charAt(i);
			if (c < '0' || c > '9')
				return -1;
			v = v * 10 + (c - '0');
		}
		return v > 255 ? -1 : v;
	}

	/**
	 * Throwing variant of {@link #isSecureOrLoopback(URI)}.
	 *
	 * @param uri The URI to validate.  Can be <jk>null</jk> (rejected).
	 * @return The validated URI (for fluent use).
	 * @throws IllegalArgumentException If the URI is not <js>"https"</js> and does not target a loopback host.
	 */
	public static URI assertSecureOrLoopback(URI uri) {
		if (! isSecureOrLoopback(uri))
			throw iaex("URI must use https or target a loopback host: %s", uri);
		return uri;
	}

	/**
	 * Constructor.
	 */
	protected UriUtils() {}
}
