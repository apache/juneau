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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.server.filter.BoundaryChecks.*;
import org.apache.juneau.rest.server.filter.CsrfBoundary.*;

/**
 * Reads the forwarded origin a trusted proxy wrote, for {@link CsrfBoundary#trustedForwarding()}.
 *
 * <p>
 * One header family is read ({@code X-Forwarded-Host} plus {@code X-Forwarded-Proto}, or {@code host=} plus
 * {@code proto=} of {@code Forwarded}); the other is never looked at.  The lines of the family are flattened in wire
 * order into entries, the {@code n}th entry from the right is selected, and everything to its left is ignored.  Every
 * failure is a refusal: nothing here falls back to another value.
 */
final class ForwardedOrigin {

	/**
	 * The outcome of {@link #resolve}.
	 *
	 * @param origin The forwarded origin, or <jk>null</jk> when refused.
	 * @param leftHosts The hosts (lower-cased, no port) of the entries strictly left of the selected one that parse.
	 * @param leftUnparsed Whether an entry strictly left of the selected one carried a host that did not parse.  A container
	 * 	may normalize such an entry into a host of its own, so the cross-check treats it as a possible match.
	 * @param reason Why it was refused, or <jk>null</jk> when {@code origin} is set.
	 */
	record Resolved(Origin origin, List<String> leftHosts, boolean leftUnparsed, BoundaryReason reason) {}

	private ForwardedOrigin() {}

	/**
	 * Resolves the forwarded origin.
	 *
	 * @param source The family to read.  Must not be {@link ForwardedSource#NONE}.
	 * @param hops The number of trusted entries; the {@code hops}th from the right is selected.
	 * @param headers Every value of the named header, in wire order, or <jk>null</jk> when the container cannot say in
	 * 	what order they arrived (a handshake map that holds the name under more than one spelling).  An absent header is
	 * 	an empty list.
	 * @return The outcome.  Never <jk>null</jk>.
	 */
	static Resolved resolve(ForwardedSource source, int hops, Function<String,List<String>> headers) {
		return source == ForwardedSource.FORWARDED ? resolveForwarded(hops, headers) : resolveXForwarded(hops, headers);
	}

	/**
	 * Whether the request claims a forwarded host in the selected family, for the peer allow-list.
	 *
	 * <p>
	 * A claim is any sign that a proxy, or something posing as one, spoke: a null lookup (the container cannot say), a
	 * header with a line that does not tokenize, or a value present at all.  The other family is never looked at.
	 *
	 * @param source The family to read.  Must not be {@link ForwardedSource#NONE}.
	 * @param headers Every value of the named header, in wire order, or <jk>null</jk> when the container cannot say.
	 * @return <jk>true</jk> if the request claims a forwarded host.
	 */
	static boolean claimsHost(ForwardedSource source, Function<String,List<String>> headers) {
		if (source != ForwardedSource.FORWARDED) {
			var lines = headers.apply("X-Forwarded-Host");
			return lines == null || ! lines.isEmpty();
		}
		var lines = headers.apply("Forwarded");
		if (lines == null)
			return true;
		var elements = new ArrayList<Element>();
		for (var line : lines)
			if (! tokenize(line == null ? "" : line, elements))
				return true;
		for (var e : elements)
			if (e.params().containsKey("host") || e.repeated().contains("host"))
				return true;
		return false;
	}

	private static Resolved refused(BoundaryReason reason) {
		return new Resolved(null, List.of(), false, reason);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// X-Forwarded-Host / X-Forwarded-Proto
	//-----------------------------------------------------------------------------------------------------------------

	private static Resolved resolveXForwarded(int hops, Function<String,List<String>> headers) {
		var hostLines = headers.apply("X-Forwarded-Host");
		var protoLines = headers.apply("X-Forwarded-Proto");
		if (hostLines == null)
			return refused(BoundaryReason.FORWARDED_HOST_MALFORMED);
		if (protoLines == null)
			return refused(BoundaryReason.FORWARDED_PROTO_MALFORMED);
		if (hostLines.isEmpty())
			return refused(BoundaryReason.FORWARDED_HOST_MISSING);
		if (protoLines.isEmpty())
			return refused(BoundaryReason.FORWARDED_PROTO_MISSING);

		var hostEntries = flatten(hostLines);
		var protoEntries = flatten(protoLines);
		var k = hostEntries.size();
		if (k != protoEntries.size())
			return refused(BoundaryReason.FORWARDED_HOPS_MISMATCH);
		if (k < hops)
			return refused(BoundaryReason.FORWARDED_HOPS_INSUFFICIENT);

		var selected = k - hops;
		Origin result = null;
		for (var i = selected; i < k; i++) {
			var host = BoundaryChecks.parseHost(hostEntries.get(i));
			if (host == null)
				return refused(BoundaryReason.FORWARDED_HOST_MALFORMED);
			var proto = proto(protoEntries.get(i));
			if (proto == null)
				return refused(BoundaryReason.FORWARDED_PROTO_MALFORMED);
			if (i == selected)
				result = origin(proto, host);
		}

		var left = new ArrayList<String>();
		var unparsed = false;
		for (var i = 0; i < selected; i++) {
			var host = BoundaryChecks.parseHost(hostEntries.get(i));
			if (host != null)
				left.add(host.host());
			else
				unparsed = true;
		}
		return new Resolved(result, left, unparsed, null);
	}

	/** The lines split on commas, with SP and HTAB trimmed around each comma, in wire order.  Empty entries are kept. */
	private static List<String> flatten(List<String> lines) {
		var entries = new ArrayList<String>();
		for (var line : lines) {
			var value = line == null ? "" : line;
			var start = 0;
			while (true) {
				var comma = value.indexOf(',', start);
				entries.add(trimOws(value.substring(start, comma < 0 ? value.length() : comma)));
				if (comma < 0)
					break;
				start = comma + 1;
			}
		}
		return entries;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Forwarded (RFC 7239)
	//-----------------------------------------------------------------------------------------------------------------

	/** One element of {@code Forwarded}: its parameters by lower-cased name, or a marker for a repeated name. */
	private record Element(boolean empty, Map<String,String> params, Set<String> repeated) {}

	private static Resolved resolveForwarded(int hops, Function<String,List<String>> headers) {
		var lines = headers.apply("Forwarded");
		if (lines == null)
			return refused(BoundaryReason.FORWARDED_HOST_MALFORMED);
		if (lines.isEmpty())
			return refused(BoundaryReason.FORWARDED_HOST_MISSING);

		// The whole header must tokenize, each line on its own, so a client's open quote cannot swallow a proxy's comma.
		var elements = new ArrayList<Element>();
		for (var line : lines)
			if (! tokenize(line == null ? "" : line, elements))
				return refused(BoundaryReason.FORWARDED_HOST_MALFORMED);

		var k = elements.size();
		if (k < hops)
			return refused(BoundaryReason.FORWARDED_HOPS_INSUFFICIENT);

		var selected = k - hops;
		Origin result = null;
		for (var i = selected; i < k; i++) {
			var e = elements.get(i);
			if (e.empty())
				return refused(BoundaryReason.FORWARDED_HOST_MALFORMED);
			if (e.repeated().contains("host") || e.repeated().contains("proto"))
				return refused(e.repeated().contains("host") ? BoundaryReason.FORWARDED_HOST_MALFORMED : BoundaryReason.FORWARDED_PROTO_MALFORMED);
			var hostValue = e.params().get("host");
			if (hostValue == null)
				return refused(BoundaryReason.FORWARDED_HOST_MISSING);
			var protoValue = e.params().get("proto");
			if (protoValue == null)
				return refused(BoundaryReason.FORWARDED_PROTO_MISSING);
			var host = BoundaryChecks.parseHost(hostValue);
			if (host == null)
				return refused(BoundaryReason.FORWARDED_HOST_MALFORMED);
			var proto = proto(protoValue);
			if (proto == null)
				return refused(BoundaryReason.FORWARDED_PROTO_MALFORMED);
			if (i == selected)
				result = origin(proto, host);
		}

		var left = new ArrayList<String>();
		var unparsed = false;
		for (var i = 0; i < selected; i++) {
			var value = elements.get(i).params().get("host");
			if (value == null)
				continue;
			var host = BoundaryChecks.parseHost(value);
			if (host != null)
				left.add(host.host());
			else
				unparsed = true;
		}
		return new Resolved(result, left, unparsed, null);
	}

	/**
	 * Tokenizes one line of {@code Forwarded} into elements, appending to {@code out}.
	 *
	 * <p>
	 * Elements are split at commas outside quoted-strings, parameters at semicolons.  A parameter is
	 * <c>token OWS "=" OWS (token / quoted-string)</c>.  A backslash, a control character or an unterminated quote
	 * makes the line not tokenize.
	 *
	 * @return <jk>false</jk> if the line does not tokenize.
	 */
	private static boolean tokenize(String s, List<Element> out) {
		var n = s.length();
		var i = 0;
		while (true) {
			var params = new LinkedHashMap<String,String>();
			var repeated = new HashSet<String>();
			var any = false;
			while (true) {
				i = skipOws(s, i);
				if (i < n && s.charAt(i) != ',' && s.charAt(i) != ';') {
					var nameStart = i;
					while (i < n && isTchar(s.charAt(i)))
						i++;
					if (i == nameStart)
						return false;
					var name = s.substring(nameStart, i).toLowerCase(Locale.ROOT);
					i = skipOws(s, i);
					if (i >= n || s.charAt(i) != '=')
						return false;
					i = skipOws(s, i + 1);
					String value;
					if (i < n && s.charAt(i) == '"') {
						var close = i + 1;
						while (close < n && s.charAt(close) != '"') {
							var c = s.charAt(close);
							if (c == '\\' || (c < 0x20 && c != '\t') || c == 0x7f)
								return false;
							close++;
						}
						if (close >= n)
							return false;
						value = s.substring(i + 1, close);
						i = close + 1;
					} else {
						var valueStart = i;
						while (i < n && isTchar(s.charAt(i)))
							i++;
						if (i == valueStart)
							return false;
						value = s.substring(valueStart, i);
					}
					any = true;
					if (params.putIfAbsent(name, value) != null)
						repeated.add(name);
					i = skipOws(s, i);
				}
				if (i < n && s.charAt(i) == ';') {
					i++;
					continue;
				}
				break;
			}
			if (i < n && s.charAt(i) != ',')
				return false;
			out.add(new Element(! any, params, repeated));
			if (i >= n)
				return true;
			i++;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Shared
	//-----------------------------------------------------------------------------------------------------------------

	/** {@code http} or {@code https} in any ASCII case, lower-cased; <jk>null</jk> for anything else. */
	private static String proto(String value) {
		var p = value.toLowerCase(Locale.ROOT);
		return "http".equals(p) || "https".equals(p) ? p : null;
	}

	private static Origin origin(String proto, Authority host) {
		return new Origin(proto, host.host(), host.port() >= 0 ? host.port() : BoundaryChecks.defaultPort(proto));
	}

	private static String trimOws(String s) {
		var a = 0;
		var b = s.length();
		while (a < b && (s.charAt(a) == ' ' || s.charAt(a) == '\t'))
			a++;
		while (b > a && (s.charAt(b - 1) == ' ' || s.charAt(b - 1) == '\t'))
			b--;
		return s.substring(a, b);
	}

	private static int skipOws(String s, int i) {
		while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t'))
			i++;
		return i;
	}

	/** RFC 7230 tchar. */
	private static boolean isTchar(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
	}
}
