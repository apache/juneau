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
package org.apache.juneau.http.remote;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Shared SSRF-hardening policy for the {@link Remote @Remote}/{@link RemoteOp @RemoteOp} proxy engines.
 *
 * <p>
 * Replaces the earlier scheme-only guard (<c>requireHttpScheme</c>) with a <b>deny-private</b> default policy:
 * an absolute <c>http</c>/<c>https</c> {@code @Remote} URL (an explicit {@code @Url} value, a {@code baseUrl()}
 * override, or an already-absolute default-path {@code fullPath}) is additionally checked against a deny-list of
 * loopback, RFC1918/link-local, and cloud-metadata targets &mdash; both lexically (the literal host string) and,
 * separately, against the <b>resolved</b> address actually connected to ("pin-on-connect", see
 * {@link #selectAllowedAddress(String, boolean, AddressResolver)}).
 *
 * <p>
 * Scheme-less (relative) values are unaffected &mdash; they resolve against the operator-configured client root,
 * which is not re-checked by this policy.
 *
 * <p>
 * Both engines (next-generation {@code RestClient.remote(...)} and classic {@code RestClient.getRemote(...)}) call
 * into this single shared class so the deny-list and pin-selection logic are defined exactly once.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S125", // Comments document URI-template blanking; they are not commented-out code.
	"java:S3516" // requireAllowedUrl is a validation passthrough: success returns the same URL that was supplied.
})
public final class RemoteUrlPolicy {

	private RemoteUrlPolicy() {}

	/** The maximum number of redirect hops a policy-covered {@code @Remote} call will follow before failing. */
	public static final int MAX_REDIRECT_HOPS = 5;

	/** The {@code RestClient} builder/settings key for the {@code allowPrivateUrls} opt-in (system property fallback). */
	public static final String ALLOW_PRIVATE_URLS_PROPERTY = "RestClient.allowPrivateUrls";

	/**
	 * The system property holding the narrow lab allow-list: a comma- or whitespace-separated list of CIDR ranges
	 * (e.g. {@code 198.18.0.0/15,192.0.0.8}) that re-permit addresses denied only because they fall in one of the
	 * special-purpose ranges <c>192.0.0.0/24</c>, <c>198.18.0.0/15</c>, <c>224.0.0.0/4</c>, <c>240.0.0.0/4</c> or
	 * <c>ff00::/8</c>.  Unset or blank (the default) allows nothing.
	 *
	 * <p>
	 * The allow is narrow by construction: every entry must lie entirely inside one of those five ranges (anything
	 * else, including <c>0.0.0.0/0</c> and <c>::/0</c>, is rejected with an {@link IllegalArgumentException}), and it
	 * never re-permits loopback, link-local/metadata, private, CGNAT, this-network, Teredo, local-use NAT64 or any
	 * other denied address, unlike the all-or-nothing {@link #ALLOW_PRIVATE_URLS_PROPERTY} opt-in.  An IPv4 entry
	 * matches an IPv4 address or the IPv4 address embedded in an IPv6 form; an IPv6 entry matches only a native
	 * <c>ff00::/8</c> address.
	 */
	public static final String ALLOW_ADDRESS_RANGES_PROPERTY = "RestClient.allowAddressRanges";

	// IPv4 literal: 4 dot-separated groups of 1-3 digits (over-permissive on range; InetAddress.getByName validates).
	private static final Pattern IPV4_LITERAL = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

	// Host names (case-insensitive) that are always denied by default, independent of DNS resolution.
	private static final Set<String> DENIED_HOST_NAMES = Set.of("localhost", "localhost.localdomain");

	private static final String METADATA_HOST = "metadata.google.internal";
	private static final String METADATA_HOST_SUFFIX = "." + METADATA_HOST;

	/**
	 * Resolves the {@code InetAddress} candidates for a hostname.  Injectable so pin-on-connect tests can simulate
	 * DNS rebinding / mixed A+AAAA / blocked-address resolution without depending on live (and flaky) DNS.
	 */
	@FunctionalInterface
	public interface AddressResolver {

		/**
		 * Resolves {@code host} to its candidate addresses.
		 *
		 * @param host The hostname (or IP literal) to resolve.
		 * @return The resolved candidate addresses. Never <jk>null</jk> or empty on success.
		 * @throws UnknownHostException If the host cannot be resolved.
		 */
		InetAddress[] resolve(String host) throws UnknownHostException;

		/** The production resolver: real DNS resolution via {@link InetAddress#getAllByName(String)}. */
		AddressResolver DEFAULT = InetAddress::getAllByName;
	}

	/**
	 * Enforces the SSRF guardrail on an effective {@code @Remote} URL: when {@code url} carries a URI scheme, it
	 * must be a canonical absolute {@code http}/{@code https} URL with a nonempty host, and &mdash; unless
	 * {@code allowPrivateUrls} is set &mdash; the literal host must not match the deny-list (see class javadoc).
	 * Scheme-less (relative) values pass through unchanged (they resolve against the client root, which is not
	 * re-checked here).
	 *
	 * <p>
	 * This is the <b>pre-check</b> stage only.  It does not resolve DNS; a hostname that is not itself denied but
	 * resolves to a denied address is instead caught by {@link #selectAllowedAddress(String, boolean, AddressResolver)}
	 * ("pin-on-connect") at connect time, including on every followed redirect hop.
	 *
	 * @param url The effective URL/path to validate.
	 * @param allowPrivateUrls When <jk>true</jk>, the deny-list check is skipped (the http/https scheme requirement
	 * 	and canonical-URI requirement still apply).
	 * @return The unchanged {@code url}.
	 * @throws IllegalArgumentException If {@code url} is absolute and violates the policy.
	 */
	public static String requireAllowedUrl(String url, boolean allowPrivateUrls) {
		return requireAllowedUrl(url, allowPrivateUrls, AllowedRanges.NONE);
	}

	/**
	 * Same as {@link #requireAllowedUrl(String, boolean)}, additionally re-permitting literal hosts that fall inside
	 * the per-client {@code allowedRanges} (unioned with the {@value #ALLOW_ADDRESS_RANGES_PROPERTY} system property).
	 *
	 * @param url The effective URL/path to validate.
	 * @param allowPrivateUrls When <jk>true</jk>, the deny-list check is skipped.
	 * @param allowedRanges The per-client narrow allow-list. Must not be <jk>null</jk>.
	 * @return The unchanged {@code url}.
	 * @throws IllegalArgumentException If {@code url} is absolute and violates the policy.
	 */
	public static String requireAllowedUrl(String url, boolean allowPrivateUrls, AllowedRanges allowedRanges) {
		var scheme = schemeOf(url);
		if (scheme == null)
			return url; // Scheme-less relative value; resolved against the client root, not re-checked here.

		if (! (eqic(scheme, "http") || eqic(scheme, "https")))
			throw iaex("Unsupported URL scheme '%s' in @Remote URL override; only http/https are allowed: %s", scheme, url);

		URI uri;
		try {
			// SVL/@Path {var} tokens (e.g. "http://host/u/{id}") are resolved later, at request-build time -- {}
			// is not valid URI syntax, so blank out any such token before parsing (the sanitized copy is used only
			// to classify scheme/host here; the original url, template tokens intact, is what is returned/used).
			uri = new URI(blankOutTemplateTokens(url));
		} catch (URISyntaxException e) {
			throw iaex(e, "Invalid @Remote URL override: %s", url);
		}
		var host = uri.getHost();
		if (ie(host))
			throw iaex("@Remote URL override has no host (non-canonical or opaque form is not allowed): %s", url);

		if (! allowPrivateUrls && isDeniedHost(host, allowedRanges))
			throw iaex("@Remote URL override targets a private/loopback/link-local/metadata/reserved host (including CGNAT, this-network, special-purpose/multicast/reserved, Teredo and local-use NAT64 ranges), which is denied by default (set allowPrivateUrls to allow local-dev targets, or list a special-purpose range such as 198.18.0.0/15 in the RestClient.allowAddressRanges system property or the client's allowAddressRanges setting): %s", url);

		return url;
	}

	// Matches {name} SVL/@Path template tokens (no nested braces) so requireAllowedUrl can parse an unsubstituted
	// @Url/@RemoteOp(path) value (tokens are filled in later, at request-build time) as a well-formed java.net.URI.
	private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{[^{}]*\\}");

	private static String blankOutTemplateTokens(String url) {
		return url.indexOf('{') == -1 ? url : TEMPLATE_TOKEN.matcher(url).replaceAll("x");
	}

	/**
	 * Returns <jk>true</jk> if the literal {@code host} string is denied by the default policy: a dotted-decimal
	 * IPv4/bracketed-IPv6 literal classified as denied by {@link #isDeniedAddress(InetAddress)} (an IPv6 zone id is
	 * ignored, and an unparseable literal containing a {@code :} is denied),
	 * or a case-insensitive match on {@code localhost}/{@code localhost.localdomain}/{@code metadata.google.internal}
	 * (and its subdomains).
	 *
	 * <p>
	 * A bare decimal/octal/hex IP literal (e.g. {@code http://2130706433}) does <b>not</b> look like a dotted IPv4
	 * literal and is intentionally left to {@link #selectAllowedAddress(String, boolean, AddressResolver)} (resolved
	 * at pin-on-connect time), not this lexical pre-check.
	 *
	 * @param host The literal host string (from {@link URI#getHost()}).
	 * @return <jk>true</jk> if the host is denied by the default policy.
	 */
	public static boolean isDeniedHost(String host) {
		return isDeniedHost(host, AllowedRanges.NONE);
	}

	/**
	 * Same as {@link #isDeniedHost(String)}, taking the per-client {@code allowedRanges} into account.
	 *
	 * @param host The literal host string (from {@link URI#getHost()}).
	 * @param allowedRanges The per-client narrow allow-list. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the host is denied.
	 */
	public static boolean isDeniedHost(String host, AllowedRanges allowedRanges) {
		var h = host.toLowerCase(Locale.ROOT);
		if (DENIED_HOST_NAMES.contains(h) || h.equals(METADATA_HOST) || h.endsWith(METADATA_HOST_SUFFIX))
			return true;
		if (looksLikeIpLiteral(host)) {
			var literal = stripZoneId(host);
			try {
				return isDeniedAddress(InetAddress.getByName(literal), allowedRanges);
			} catch (UnknownHostException e) {
				// A colon-containing literal that cannot be parsed fails closed; a dotted-decimal form that is not valid
				// (e.g. an octet > 255) is treated as an ordinary (non-denied) name and left to pin-on-connect.
				return literal.indexOf(':') != -1;
			}
		}
		return false;
	}

	// Removes an IPv6 zone/scope id (the '%...' suffix, or '%25...' as percent-encoded in a URL) from a possibly
	// bracketed literal: "[fe80::1%25en0]" -> "[fe80::1]".  The zone does not affect the address classification.
	private static String stripZoneId(String host) {
		var i = host.indexOf('%');
		if (i == -1)
			return host;
		return host.endsWith("]") ? host.substring(0, i) + "]" : host.substring(0, i);
	}

	/**
	 * Returns <jk>true</jk> if {@code host} is textually shaped like an IP literal (a dotted-decimal IPv4 form, or
	 * any form containing a {@code :} as in bracket-stripped IPv6) -- i.e. a form worth parsing locally (no DNS
	 * lookup) to classify without deferring to pin-on-connect.
	 */
	private static boolean looksLikeIpLiteral(String host) {
		return IPV4_LITERAL.matcher(host).matches() || host.indexOf(':') != -1;
	}

	/**
	 * Returns <jk>true</jk> if {@code addr} is denied by the default policy -- the deny-list applied both to
	 * literal-IP hosts (pre-check) and to resolved addresses (pin-on-connect).
	 *
	 * <p>
	 * Denied addresses:
	 * <ul>
	 * 	<li>Loopback, unspecified, link-local (including the <c>169.254.169.254</c> metadata address) and RFC1918
	 * 		site-local addresses.
	 * 	<li>IPv4 <c>0.0.0.0/8</c> ("this network") and <c>100.64.0.0/10</c> (carrier-grade NAT shared space).
	 * 	<li>IPv4 special-purpose ranges that are never legitimate remote targets: <c>192.0.0.0/24</c> (IETF protocol
	 * 		assignments), <c>198.18.0.0/15</c> (benchmarking; often lab or appliance networks &mdash; see
	 * 		{@link #ALLOW_ADDRESS_RANGES_PROPERTY}), <c>224.0.0.0/4</c> (multicast) and <c>240.0.0.0/4</c> (reserved,
	 * 		including the limited broadcast address <c>255.255.255.255</c>).
	 * 	<li>IPv6 unique-local <c>fc00::/7</c> and multicast <c>ff00::/8</c>.
	 * 	<li>IPv6 Teredo <c>2001::/32</c>, denied outright because the embedded client IPv4 address is obfuscated.
	 * 	<li>IPv6 local-use NAT64 <c>64:ff9b:1::/48</c> (RFC 8215), denied outright because the embedding length
	 * 		(/48, /56, /64 or /96) is operator-defined and cannot be determined from the address.
	 * </ul>
	 *
	 * <p>
	 * IPv6 forms that embed an IPv4 address are unwrapped and classified by the embedded IPv4 address:
	 * IPv4-mapped (<c>::ffff:a.b.c.d</c>), IPv4-compatible (<c>::a.b.c.d</c>), IPv4-translated
	 * (<c>::ffff:0:a.b.c.d</c>), NAT64 (<c>64:ff9b::/96</c>) and 6to4 (<c>2002::/16</c>, IPv4 in bytes 2-5).
	 * A public embedded address (e.g. <c>64:ff9b::808:808</c>) remains allowed.
	 *
	 * <p>
	 * Addresses inside a range listed in the {@value #ALLOW_ADDRESS_RANGES_PROPERTY} system property (empty by
	 * default) are re-permitted if they are denied only as a special-purpose range; see
	 * {@link #ALLOW_ADDRESS_RANGES_PROPERTY}.
	 *
	 * @param addr The address to classify. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the address is denied, taking the {@value #ALLOW_ADDRESS_RANGES_PROPERTY} allow-list
	 * 	into account.
	 * @throws IllegalArgumentException If the {@value #ALLOW_ADDRESS_RANGES_PROPERTY} property is malformed.
	 */
	public static boolean isDeniedAddress(InetAddress addr) {
		return isDeniedAddress(addr, AllowedRanges.NONE);
	}

	/**
	 * Same as {@link #isDeniedAddress(InetAddress)}, additionally re-permitting special-purpose addresses inside the
	 * per-client {@code allowedRanges}.  The effective allow-list is the <b>union</b> of those ranges and the
	 * {@value #ALLOW_ADDRESS_RANGES_PROPERTY} system property; both are bounded to the same five special-purpose
	 * ranges, so the union can never re-open any other deny.
	 *
	 * @param addr The address to classify. Must not be <jk>null</jk>.
	 * @param allowedRanges The per-client narrow allow-list. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the address is denied.
	 * @throws IllegalArgumentException If the {@value #ALLOW_ADDRESS_RANGES_PROPERTY} property is malformed.
	 */
	public static boolean isDeniedAddress(InetAddress addr, AllowedRanges allowedRanges) {
		var ranges = parseAllowedRanges(System.getProperty(ALLOW_ADDRESS_RANGES_PROPERTY));
		if (! allowedRanges.isEmpty()) {
			var union = new ArrayList<>(ranges);
			union.addAll(allowedRanges.ranges);
			ranges = union;
		}
		return isDenied(addr, ranges);
	}

	// Same as isDeniedAddress(InetAddress) but with an explicit allow-list spec instead of the system property.
	static boolean isDeniedAddress(InetAddress addr, String allowedRanges) {
		return isDenied(addr, parseAllowedRanges(allowedRanges));
	}

	private static boolean isDenied(InetAddress addr, List<Cidr> ranges) {
		if (isDeniedOther(addr))
			return true; // Never re-permitted: only special-purpose-range denials can be allowed.
		if (! isSpecialPurpose(unwrapEmbeddedIpv4(addr)))
			return false;
		if (ranges.isEmpty())
			return true;
		var raw = addr.getAddress();
		var embedded = unwrapEmbeddedIpv4(addr).getAddress();
		for (var r : ranges)
			if (r.contains(raw) || r.contains(embedded))
				return false;
		return true;
	}

	/** A parsed CIDR range. */
	private record Cidr(byte[] prefix, int bits) {
		@Override /* Object */
		public boolean equals(Object o) {
			return o instanceof Cidr other && bits == other.bits && Arrays.equals(prefix, other.prefix);
		}

		@Override /* Object */
		public int hashCode() {
			return 31 * Arrays.hashCode(prefix) + bits;
		}

		@Override /* Object */
		public String toString() {
			return "Cidr[prefix=" + Arrays.toString(prefix) + ", bits=" + bits + "]";
		}

		boolean contains(byte[] b) {
			if (b.length != prefix.length)
				return false;
			for (var i = 0; i < bits; i++)
				if (((b[i >> 3] ^ prefix[i >> 3]) & (0x80 >> (i & 7))) != 0)
					return false;
			return true;
		}
	}

	/**
	 * A validated, parsed per-client allow-list of special-purpose address ranges, the per-client counterpart of the
	 * {@value RemoteUrlPolicy#ALLOW_ADDRESS_RANGES_PROPERTY} system property.
	 *
	 * <p>
	 * It has exactly the property's narrow semantics: every entry must lie entirely inside <c>192.0.0.0/24</c>,
	 * <c>198.18.0.0/15</c>, <c>224.0.0.0/4</c>, <c>240.0.0.0/4</c> or <c>ff00::/8</c> (anything else is rejected with an
	 * {@link IllegalArgumentException}), entries are parsed as literals without any DNS lookup, and the ranges never
	 * re-permit loopback, link-local/metadata, private, CGNAT or any other denied address.  Instances are immutable;
	 * two instances holding the same set of ranges are equal.
	 *
	 * @since 10.0.0
	 */
	public static final class AllowedRanges {

		/** The empty allow-list, which re-permits nothing. */
		public static final AllowedRanges NONE = new AllowedRanges(List.of());

		private final List<Cidr> ranges;
		private final Set<String> key;

		private AllowedRanges(List<Cidr> ranges) {
			this.ranges = ranges;
			this.key = ranges.stream().map(r -> Arrays.toString(r.prefix()) + "/" + r.bits()).collect(Collectors.toCollection(TreeSet::new));
		}

		/**
		 * Parses and validates allow-list entries.
		 *
		 * @param specs The entries; each element may itself hold several comma- or whitespace-separated CIDR ranges or
		 * 	single addresses.  <jk>null</jk> or empty yields {@link #NONE}.
		 * @return The parsed allow-list. Never <jk>null</jk>.
		 * @throws IllegalArgumentException If any entry is malformed or not entirely inside one of the five
		 * 	special-purpose ranges.
		 */
		public static AllowedRanges of(String...specs) {
			if (specs == null || specs.length == 0)
				return NONE;
			var list = new ArrayList<Cidr>();
			for (var spec : specs) {
				if (spec == null)
					throw iaex("Invalid allowed address range: null entry");
				list.addAll(parseAllowedRangesStrict(spec));
			}
			return list.isEmpty() ? NONE : new AllowedRanges(List.copyOf(list));
		}

		/**
		 * Returns <jk>true</jk> if this allow-list re-permits nothing.
		 *
		 * @return <jk>true</jk> if empty.
		 */
		public boolean isEmpty() {
			return ranges.isEmpty();
		}

		@Override /* Object */
		public boolean equals(Object o) {
			return o instanceof AllowedRanges other && key.equals(other.key);
		}

		@Override /* Object */
		public int hashCode() {
			return key.hashCode();
		}

		@Override /* Object */
		public String toString() {
			return "AllowedRanges" + key;
		}
	}

	// Parses a spec without touching the single-entry property cache; an invalid entry throws.
	private static List<Cidr> parseAllowedRangesStrict(String spec) {
		return Arrays.stream(spec.trim().split("[,\\s]+")).filter(token -> ! token.isEmpty()).map(RemoteUrlPolicy::parseAllowEntry).toList();
	}

	// The only ranges the allow-list may re-permit (and that every allow entry must lie within).
	private static final List<Cidr> SPECIAL_PURPOSE = List.of(
		new Cidr(new byte[]{(byte)192, 0, 0, 0}, 24),
		new Cidr(new byte[]{(byte)198, 18, 0, 0}, 15),
		new Cidr(new byte[]{(byte)224, 0, 0, 0}, 4),
		new Cidr(new byte[]{(byte)240, 0, 0, 0}, 4),
		new Cidr(new byte[]{(byte)0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 8)
	);

	private static final Pattern ALLOW_ENTRY = Pattern.compile("^([\\d.]+|[\\dA-Fa-f.]*:[\\dA-Fa-f:.]*)(?:/(0|[1-9]\\d{0,2}))?$");

	// Single-entry cache (successes and failures): the spec is normally a constant system property, so parse once per
	// distinct value.  A failed parse is remembered by message so the same string throws again without re-parsing.
	private record ParsedAllow(String spec, List<Cidr> ranges, String error) {}
	private static final AtomicReference<ParsedAllow> PARSED_ALLOW = new AtomicReference<>(new ParsedAllow("", List.of(), null));

	private static List<Cidr> parseAllowedRanges(String spec) {
		if (ib(spec))
			return List.of();
		var cached = PARSED_ALLOW.get();
		if (! cached.spec().equals(spec)) {
			cached = parseAllowSpec(spec);
			PARSED_ALLOW.set(cached);
		}
		if (cached.error() != null)
			throw iaex("%s", cached.error());
		return cached.ranges();
	}

	private static ParsedAllow parseAllowSpec(String spec) {
		try {
			return new ParsedAllow(spec, List.copyOf(parseAllowedRangesStrict(spec)), null);
		} catch (IllegalArgumentException e) {
			return new ParsedAllow(spec, List.of(), e.getMessage());
		}
	}

	// Parses a dotted-quad IPv4 literal purely syntactically (octets 0-255, no leading zeros); returns null if invalid.
	@SuppressWarnings({
		"java:S1168" // null signals an invalid IPv4 literal (see comment); callers test for it
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
			out[i] = (byte) v;
		}
		return out;
	}

	private static Cidr parseAllowEntry(String token) {
		var m = ALLOW_ENTRY.matcher(token);
		if (! m.matches())
			throw iaex("Invalid allowed address range '%s': expected an IPv4 or IPv6 literal with an optional /prefix", token);
		var literal = m.group(1);
		var isIpv6 = literal.indexOf(':') != -1;
		InetAddress a;
		try {
			if (isIpv6) {
				a = InetAddress.getByName(literal); // Contains ':' and only hex/colon/dot characters, so it is parsed as a literal (never resolved via DNS).
			} else {
				var v4 = parseIpv4(literal);
				if (v4 == null)
					throw iaex("Invalid allowed address range '%s': IPv4 addresses must be dotted-quad with octets 0-255 and no leading zeros", token);
				a = InetAddress.getByAddress(v4);
			}
		} catch (UnknownHostException e) {
			throw iaex(e, "Invalid allowed address range '%s': unparseable address", token);
		}
		// An IPv6-syntax entry must be a native IPv6 address (not collapsed to IPv4, e.g. ::ffff:a.b.c.d).
		if (isIpv6 != (a instanceof Inet6Address))
			throw iaex("Invalid allowed address range '%s': IPv4-embedded IPv6 forms are not allowed here", token);
		var prefix = a.getAddress();
		var bits = prefix.length * 8;
		if (m.group(2) != null) {
			bits = Integer.parseInt(m.group(2));
			if (bits > prefix.length * 8)
				throw iaex("Invalid allowed address range '%s': prefix length must be 0-%s", token, prefix.length * 8);
		}
		for (var r : SPECIAL_PURPOSE)
			if (r.prefix().length == prefix.length && bits >= r.bits() && r.contains(prefix))
				return new Cidr(prefix, bits);
		throw iaex("Invalid allowed address range '%s': must lie entirely within 192.0.0.0/24, 198.18.0.0/15, 224.0.0.0/4, 240.0.0.0/4 or ff00::/8", token);
	}

	// True if the (already unwrapped) address is in one of the special-purpose ranges.
	private static boolean isSpecialPurpose(InetAddress a) {
		var bytes = a.getAddress();
		if (a instanceof Inet6Address)
			return (bytes[0] & 0xFF) == 0xFF;
		var b0 = bytes[0] & 0xFF;
		var b1 = bytes[1] & 0xFF;
		return (b0 == 192 && b1 == 0 && bytes[2] == 0)
			|| (b0 == 198 && (b1 & 0xFE) == 18)
			|| (b0 & 0xE0) == 0xE0; // 224.0.0.0/4 multicast + 240.0.0.0/4 reserved
	}

	// Every deny other than the special-purpose ranges (never subject to the allow-list).
	private static boolean isDeniedOther(InetAddress addr) {
		if (isTeredo(addr) || isNat64LocalUse(addr))
			return true;
		var a = unwrapEmbeddedIpv4(addr);
		if (a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
			return true;
		var bytes = a.getAddress();
		if (a instanceof Inet6Address)
			return (bytes[0] & 0xFE) == 0xFC; // Unique-local fc00::/7 (top 7 bits are 1111 110).
		// 0.0.0.0/8 ("this network") and 100.64.0.0/10 (CGNAT shared space).
		return bytes[0] == 0 || ((bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 0x40);
	}

	/** Returns <jk>true</jk> for a Teredo (<c>2001:0000::/32</c>) address. */
	private static boolean isTeredo(InetAddress addr) {
		if (! (addr instanceof Inet6Address))
			return false;
		var b = addr.getAddress();
		return b[0] == 0x20 && b[1] == 0x01 && b[2] == 0 && b[3] == 0;
	}

	/** Returns <jk>true</jk> for a local-use NAT64 (<c>64:ff9b:1::/48</c>, RFC 8215) address. */
	private static boolean isNat64LocalUse(InetAddress addr) {
		if (! (addr instanceof Inet6Address))
			return false;
		var b = addr.getAddress();
		return b[0] == 0 && b[1] == 0x64 && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B && b[4] == 0 && b[5] == 1;
	}

	/**
	 * Unwraps an IPv6 address that embeds an IPv4 address (IPv4-mapped, IPv4-compatible, IPv4-translated, NAT64
	 * or 6to4) to the embedded {@link Inet4Address}; any other address is returned unchanged.
	 */
	@SuppressWarnings({
		"java:S3776" // One flat decision table over the IPv6 embedding prefixes; splitting it would scatter the byte-layout rules.
	})
	private static InetAddress unwrapEmbeddedIpv4(InetAddress addr) {
		if (! (addr instanceof Inet6Address))
			return addr;
		var b = addr.getAddress();
		byte[] v4 = null;
		if (isZero(b, 0, 10)) {
			// IPv4-mapped ::ffff:a.b.c.d, or IPv4-compatible ::a.b.c.d (but '::' and '::1' keep their IPv6 classification).
			var mapped = (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
			var compatible = b[10] == 0 && b[11] == 0 && (b[12] != 0 || b[13] != 0 || b[14] != 0 || (b[15] & 0xFF) > 1);
			if (mapped || compatible)
				v4 = new byte[]{b[12], b[13], b[14], b[15]};
		} else if (isZero(b, 0, 8) && (b[8] & 0xFF) == 0xFF && (b[9] & 0xFF) == 0xFF && b[10] == 0 && b[11] == 0) {
			// IPv4-translated (RFC 2765) ::ffff:0:a.b.c.d
			v4 = new byte[]{b[12], b[13], b[14], b[15]};
		} else if (b[0] == 0 && b[1] == 0x64 && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B) {
			// NAT64 well-known prefix 64:ff9b::/96 (RFC 6052). The local-use 64:ff9b:1::/48 is denied before unwrapping.
			if (isZero(b, 4, 12))
				v4 = new byte[]{b[12], b[13], b[14], b[15]};
		} else if (b[0] == 0x20 && b[1] == 0x02) {
			// 6to4 2002::/16: the IPv4 address is in bytes 2-5.
			v4 = new byte[]{b[2], b[3], b[4], b[5]};
		}
		if (v4 == null)
			return addr;
		try {
			return InetAddress.getByAddress(v4);
		} catch (UnknownHostException e) {
			return addr; // HTT: getByAddress with a 4-byte array never throws
		}
	}

	// Returns true if bytes [from, to) are all zero.
	private static boolean isZero(byte[] b, int from, int to) {
		for (var i = from; i < to; i++)
			if (b[i] != 0)
				return false;
		return true;
	}

	/**
	 * Resolves {@code host} and returns the first candidate address that is allowed by the default policy
	 * ("pin-on-connect") -- the address the caller should actually connect to.  When multiple candidates are
	 * returned (e.g. mixed A/AAAA), the first allowed one wins; a candidate that is itself denied is skipped rather
	 * than rejecting the whole resolution, so a name with one public and one loopback address may still be used.
	 *
	 * @param host The hostname (or IP literal) to resolve and pin.
	 * @param allowPrivateUrls When <jk>true</jk>, the deny-list check is skipped and the first resolved candidate is
	 * 	returned unconditionally.
	 * @param resolver The address resolver to use. Pass {@link AddressResolver#DEFAULT} for real DNS, or an
	 * 	injectable stub in tests to simulate rebinding/mixed-family resolution without live DNS.
	 * @return The selected address to connect to. Never <jk>null</jk>.
	 * @throws UnknownHostException If {@code host} cannot be resolved.
	 * @throws IllegalArgumentException If every resolved candidate is denied (and {@code allowPrivateUrls} is
	 * 	<jk>false</jk>).
	 */
	public static InetAddress selectAllowedAddress(String host, boolean allowPrivateUrls, AddressResolver resolver) throws UnknownHostException {
		return selectAllowedAddress(host, allowPrivateUrls, resolver, AllowedRanges.NONE);
	}

	/**
	 * Same as {@link #selectAllowedAddress(String, boolean, AddressResolver)}, additionally re-permitting candidates
	 * inside the per-client {@code allowedRanges} (unioned with the {@value #ALLOW_ADDRESS_RANGES_PROPERTY} system property).
	 *
	 * @param host The hostname (or IP literal) to resolve and pin.
	 * @param allowPrivateUrls When <jk>true</jk>, the deny-list check is skipped.
	 * @param resolver The address resolver to use.
	 * @param allowedRanges The per-client narrow allow-list. Must not be <jk>null</jk>.
	 * @return The selected address to connect to. Never <jk>null</jk>.
	 * @throws UnknownHostException If {@code host} cannot be resolved.
	 * @throws IllegalArgumentException If every resolved candidate is denied.
	 */
	public static InetAddress selectAllowedAddress(String host, boolean allowPrivateUrls, AddressResolver resolver, AllowedRanges allowedRanges) throws UnknownHostException {
		var candidates = resolver.resolve(host);
		if (allowPrivateUrls)
			return candidates[0];
		for (var c : candidates)
			if (! isDeniedAddress(c, allowedRanges))
				return c;
		throw iaex("No allowed address for host '%s': every resolved candidate is private/loopback/link-local/reserved (set allowPrivateUrls to allow local-dev targets, or list a special-purpose range such as 198.18.0.0/15 in the RestClient.allowAddressRanges system property or the client's allowAddressRanges setting)", host);
	}

	/**
	 * Resolves a redirect {@code Location} header value against the current request URI, per RFC 7231 §7.1.2
	 * (relative locations are resolved against the request URI).
	 *
	 * @param from The current (pre-redirect) request URI. Must be absolute.
	 * @param location The {@code Location} header value. Must not be <jk>null</jk> or blank.
	 * @return The resolved absolute redirect-target URI.
	 * @throws IllegalArgumentException If {@code location} is blank or does not resolve to an absolute URI.
	 */
	public static URI resolveRedirectLocation(URI from, String location) {
		if (ib(location))
			throw iaex("Redirect response is missing a Location header");
		URI target;
		try {
			target = from.resolve(location.trim());
		} catch (IllegalArgumentException e) {
			throw iaex(e, "Redirect response has an invalid Location header: %s", location);
		}
		if (! target.isAbsolute())
			throw iaex("Redirect Location did not resolve to an absolute URI: %s", location);
		return target;
	}

	/**
	 * Returns the URI scheme of a URL (the token before the first {@code :} when it precedes any {@code /},
	 * {@code ?} or {@code #}), or <jk>null</jk> if the value has no scheme (e.g. a relative path or a value
	 * beginning with a {@code {var}} token).
	 *
	 * @param url The URL to inspect.
	 * @return The scheme, or <jk>null</jk> if none.
	 */
	public static String schemeOf(String url) {
		if (url.isEmpty() || ! Character.isLetter(url.charAt(0)))
			return null;
		for (var i = 0; i < url.length(); i++) {
			var c = url.charAt(i);
			if (c == ':')
				return url.substring(0, i);
			if (c == '/' || c == '?' || c == '#')
				return null;
			if (! (Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.'))
				return null;
		}
		return null;
	}
}
