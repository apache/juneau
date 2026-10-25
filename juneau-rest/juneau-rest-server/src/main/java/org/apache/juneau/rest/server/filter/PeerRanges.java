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
import java.util.regex.*;

/**
 * The peer allow-list behind {@link CsrfBoundary.Builder#trustedProxies(String...)}.
 *
 * <p>
 * Entries are IPv4 or IPv6 literals with an optional {@code /prefix}.  Nothing is ever resolved: a host name, a
 * bracketed form, a zone id, an IPv4-mapped IPv6 form and an out-of-range prefix are all refused.  Host bits are
 * masked, so {@code 10.0.0.1/8} is {@code 10.0.0.0/8}, and {@code /0} matches every address of its family.
 *
 * <p>
 * Matching is per family.  A peer written as an IPv4-mapped IPv6 address ({@code ::ffff:a.b.c.d}) is unwrapped to IPv4
 * first; other IPv4-embedded forms ({@code ::a.b.c.d}, NAT64) stay IPv6.  Anything that does not parse as a literal is
 * not in any range.
 */
final class PeerRanges {

	private static final Pattern PREFIX = Pattern.compile("0|[1-9]\\d{0,2}");
	private static final Pattern IPV6_CHARS = Pattern.compile("[0-9A-Fa-f:][0-9A-Fa-f:.]*");
	private static final Pattern ZONE = Pattern.compile("[0-9A-Za-z_.~-]+");

	@SuppressWarnings({
		"java:S6218" // Private record; never compared, hashed or printed
	})
	private record Range(byte[] network, int bits) {
		boolean contains(byte[] address) {
			if (address.length != network.length)
				return false;
			for (var i = 0; i < bits; i++) {
				var mask = 0x80 >> (i & 7);
				if ((address[i >> 3] & mask) != (network[i >> 3] & mask))
					return false;
			}
			return true;
		}

		String text() {
			try {
				return InetAddress.getByAddress(network).getHostAddress() + '/' + bits;
			} catch (UnknownHostException e) {
				throw new IllegalStateException(e);  // Unreachable: the length is 4 or 16.
			}
		}
	}

	private final List<Range> ranges;
	private final List<String> texts;

	/**
	 * Parses the entries.
	 *
	 * @param cidrs The entries.  Must not be <jk>null</jk> or empty, and must not hold a <jk>null</jk> or invalid entry.
	 * @throws IllegalArgumentException If an entry is invalid or there are none.
	 */
	PeerRanges(String...cidrs) {
		if (cidrs == null || cidrs.length == 0)
			throw iaex("trustedProxies requires at least one address range.");
		var list = new ArrayList<Range>();
		var seen = new LinkedHashSet<String>();
		for (var cidr : cidrs) {
			var r = parse(cidr);
			if (seen.add(r.text()))
				list.add(r);
		}
		ranges = List.copyOf(list);
		texts = List.copyOf(seen);
	}

	/** The normalized, masked entries in the order given, without duplicates. */
	List<String> texts() {
		return texts;
	}

	/**
	 * Whether the peer address is in a range.
	 *
	 * @param peer The address the container reported, or <jk>null</jk> when it reported none.
	 * @return <jk>true</jk> if the peer is in a range.  <jk>null</jk> and anything unparseable is not.
	 */
	boolean contains(String peer) {
		return contains(parsePeer(peer));
	}

	/**
	 * Whether the peer address is in a range.
	 *
	 * @param peer The address the container reported, or <jk>null</jk> when it reported none.
	 * @return <jk>true</jk> if the peer is in a range.  <jk>null</jk> is not.
	 */
	boolean contains(InetAddress peer) {
		return peer != null && contains(unwrap(peer.getAddress()));
	}

	private boolean contains(byte[] address) {
		if (address == null)
			return false;
		for (var r : ranges)
			if (r.contains(address))
				return true;
		return false;
	}

	// An IPv4-mapped IPv6 address is the IPv4 address it carries.  InetAddress.getByName and getByAddress(byte[]) collapse
	// it already; Inet6Address.getByAddress(host, bytes, scope) does not.
	@SuppressWarnings({
		"java:S1168" // A null address means "no peer address" and contains() tests for it; an empty array would be a different, invalid address.
	})
	private static byte[] unwrap(byte[] a) {
		if (a == null)
			return null;
		if (a.length != 16)
			return a;
		for (var i = 0; i < 10; i++)
			if (a[i] != 0)
				return a;
		if (a[10] != (byte)0xff || a[11] != (byte)0xff)
			return a;
		return Arrays.copyOfRange(a, 12, 16);
	}

	private static Range parse(String entry) {
		if (entry == null)
			throw iaex("trustedProxies entry must not be null.");
		var token = entry.trim();
		var slash = token.indexOf('/');
		var literal = slash < 0 ? token : token.substring(0, slash);
		var address = literal.indexOf(':') < 0 ? parseIpv4(literal) : parseIpv6(literal);
		if (address == null || (literal.indexOf(':') >= 0 && address.length != 16))
			throw iaex("Invalid trustedProxies entry '%s': expected an IPv4 or IPv6 literal with an optional /prefix.", entry);
		var max = address.length * 8;
		var bits = max;
		if (slash >= 0) {
			var p = token.substring(slash + 1);
			if (! PREFIX.matcher(p).matches() || (bits = Integer.parseInt(p)) > max)
				throw iaex("Invalid trustedProxies entry '%s': the prefix length must be 0-%s.", entry, max);
		}
		var network = address.clone();
		for (var i = 0; i < network.length; i++) {
			var keep = Math.max(0, Math.min(8, bits - i * 8));
			network[i] &= (byte)(0xff00 >> keep);
		}
		return new Range(network, bits);
	}

	// A strict dotted quad: four decimal octets 0-255, no signs and no leading zeros.  Null if it is not one.
	@SuppressWarnings({
		"java:S1168" // null signals an unparseable address; callers test for it
	})
	private static byte[] parseIpv4(String s) {
		var parts = s.split("\\.", -1);
		if (parts.length != 4)
			return null;
		var out = new byte[4];
		for (var i = 0; i < 4; i++) {
			var p = parts[i];
			if (! (p.equals("0") || p.matches("[1-9]\\d{0,2}")))
				return null;
			var v = Integer.parseInt(p);
			if (v > 255)
				return null;
			out[i] = (byte)v;
		}
		return out;
	}

	// Only hex digits, colons and dots reach getByName, which then parses a literal and never asks a resolver.  Returns
	// null for anything else, and for a form Java collapses to IPv4 (the address length then tells the caller).
	@SuppressWarnings({
		"java:S1168" // null signals an unparseable address; callers test for it
	})
	private static byte[] parseIpv6(String s) {
		if (! IPV6_CHARS.matcher(s).matches() || s.indexOf(':') < 0)
			return null;
		try {
			return InetAddress.getByName(s).getAddress();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	// A peer as the container reports it: optional surrounding brackets and an optional zone, which is dropped.
	@SuppressWarnings({
		"java:S1168" // null signals an unparseable address; callers test for it
	})
	private static byte[] parsePeer(String peer) {
		if (peer == null)
			return null;
		var s = peer;
		if (s.length() >= 2 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']')
			s = s.substring(1, s.length() - 1);
		var pct = s.indexOf('%');
		if (pct >= 0) {
			if (! ZONE.matcher(s.substring(pct + 1)).matches())
				return null;
			s = s.substring(0, pct);
		}
		var a = s.indexOf(':') < 0 ? parseIpv4(s) : parseIpv6(s);
		return unwrap(a);
	}
}
