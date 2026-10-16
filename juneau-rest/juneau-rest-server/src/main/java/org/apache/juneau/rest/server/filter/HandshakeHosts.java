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

import java.net.*;
import java.util.*;

/**
 * The {@code Host} values of a WebSocket handshake; over RFC 8441 with no {@code Host}, one value from the request
 * URI's authority.
 *
 * <p>
 * A handshake over HTTP/2 (RFC 8441 extended CONNECT) carries its authority as the {@code :authority} pseudo-header
 * and no {@code Host} header, which a servlet container surfaces in the handshake's request URI.  A JSR 356
 * {@code HandshakeRequest} exposes no protocol, so the HTTP/2 case is recognized by what an RFC 6455 handshake must
 * carry and an RFC 8441 one does not: a {@code Sec-WebSocket-Key} or {@code Upgrade} header.  The container enforces
 * that signal on Jetty and Tomcat (a key-less HTTP/1.1 upgrade is a 400 before any configurator runs).
 *
 * <p>
 * Header names are matched ignoring case, because HTTP/2 field names arrive in lower case.
 *
 * @param values The {@code Host} values, in order.  Empty when there is no {@code Host} and no authority to stand in
 * 	for it.  Never <jk>null</jk>.
 * @param synthesised <jk>true</jk> if {@code values} is the single value built from the request URI rather than a
 * 	real {@code Host} header.
 * @since 10.0.0
 */
public record HandshakeHosts(List<String> values, boolean synthesised) {

	/**
	 * Constructor.
	 *
	 * @param values The values.  Must not be <jk>null</jk>.
	 * @param synthesised Whether the value was built from the request URI.
	 */
	public HandshakeHosts {
		reqnn("values", values);
		values = List.copyOf(values);
	}

	/**
	 * The {@code Host} values of a handshake.
	 *
	 * <ol>
	 * 	<li>Every real {@code Host} value, across all spellings of the name, is returned as is.
	 * 	<li>Otherwise, if the handshake has a {@code Sec-WebSocket-Key} or {@code Upgrade} header (any value, blank
	 * 		included), it is an RFC 6455 handshake and the result is empty.
	 * 	<li>Otherwise one value is built from the request URI: its host (IPv6 brackets kept) plus <c>":"</c> and its port
	 * 		when one is given.  No port is invented, so the caller's scheme default applies.  Userinfo is never carried.
	 * 		A <jk>null</jk> URI, or one with no host, gives the empty result.
	 * </ol>
	 *
	 * @param headers The handshake headers.  Can be <jk>null</jk>, which is empty.
	 * @param requestUri The handshake's request URI.  Can be <jk>null</jk>.
	 * @return The hosts.  Never <jk>null</jk>.
	 */
	public static HandshakeHosts of(Map<String,List<String>> headers, URI requestUri) {
		var hosts = new ArrayList<String>();
		var rfc6455 = false;
		if (headers != null) {
			for (var e : headers.entrySet()) {
				var name = e.getKey();
				if ("Host".equalsIgnoreCase(name)) {
					if (e.getValue() != null)
						for (var v : e.getValue())
							if (v != null)
								hosts.add(v);
				} else if ("Sec-WebSocket-Key".equalsIgnoreCase(name) || "Upgrade".equalsIgnoreCase(name)) {
					rfc6455 = true;
				}
			}
		}
		if (! hosts.isEmpty())
			return new HandshakeHosts(hosts, false);
		if (rfc6455 || requestUri == null)
			return new HandshakeHosts(List.of(), false);
		// getHost(), never getAuthority(): the authority would carry userinfo into the host parser.
		var host = requestUri.getHost();
		if (host == null || host.isBlank())
			return new HandshakeHosts(List.of(), false);
		var port = requestUri.getPort();
		return new HandshakeHosts(List.of(port > 0 ? host + ":" + port : host), true);
	}
}
