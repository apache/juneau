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
import java.util.regex.*;

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
public final class RemoteUrlPolicy {

	private RemoteUrlPolicy() {}

	/** The maximum number of redirect hops a policy-covered {@code @Remote} call will follow before failing. */
	public static final int MAX_REDIRECT_HOPS = 5;

	/** The {@code RestClient} builder/settings key for the {@code allowPrivateUrls} opt-in (system property fallback). */
	public static final String ALLOW_PRIVATE_URLS_PROPERTY = "RestClient.allowPrivateUrls";

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
		var scheme = schemeOf(url);
		if (scheme == null)
			return url; // Scheme-less relative value; resolved against the client root, not re-checked here.

		if (! (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
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
		if (host == null || host.isEmpty())
			throw iaex("@Remote URL override has no host (non-canonical or opaque form is not allowed): %s", url);

		if (! allowPrivateUrls && isDeniedHost(host))
			throw iaex("@Remote URL override targets a private/loopback/link-local/metadata host, which is denied by default (set allowPrivateUrls to allow local-dev targets): %s", url);

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
	 * IPv4/bracketed-IPv6 literal classified as loopback/private/link-local by {@link #isDeniedAddress(InetAddress)},
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
		var h = host.toLowerCase(Locale.ROOT);
		if (DENIED_HOST_NAMES.contains(h) || h.equals(METADATA_HOST) || h.endsWith(METADATA_HOST_SUFFIX))
			return true;
		if (looksLikeIpLiteral(host)) {
			try {
				return isDeniedAddress(InetAddress.getByName(host));
			} catch (UnknownHostException e) {
				// Not a valid literal after all (e.g. an octet > 255) -- treat as an ordinary (non-denied) name.
				return false;
			}
		}
		return false;
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
	 * Returns <jk>true</jk> if {@code addr} is a loopback, unspecified, RFC1918-private, link-local, or IPv6
	 * unique-local address -- the deny-list applied both to literal-IP hosts (pre-check) and to resolved addresses
	 * (pin-on-connect).  IPv4-mapped/IPv4-compatible IPv6 addresses are unwrapped and classified by their embedded
	 * IPv4 address.
	 *
	 * @param addr The address to classify. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the address is denied by the default policy.
	 */
	public static boolean isDeniedAddress(InetAddress addr) {
		var a = unwrapIpv4Mapped(addr);
		if (a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
			return true;
		if (a instanceof Inet6Address) {
			var bytes = a.getAddress();
			// Unique-local fc00::/7 -- top 7 bits are 1111 110.
			return (bytes[0] & 0xFE) == 0xFC;
		}
		return false;
	}

	/** Unwraps an IPv4-mapped (<c>::ffff:a.b.c.d</c>) or IPv4-compatible IPv6 address to its embedded {@link Inet4Address}. */
	private static InetAddress unwrapIpv4Mapped(InetAddress addr) {
		if (! (addr instanceof Inet6Address addr2))
			return addr;
		var bytes = addr2.getAddress();
		// IPv4-mapped: first 10 bytes 0x00, next 2 bytes 0xFF; IPv4-compatible: first 12 bytes 0x00 (and not ::/::1).
		var ipv4Mapped = true;
		for (var i = 0; i < 10 && ipv4Mapped; i++)
			ipv4Mapped = bytes[i] == 0;
		if (ipv4Mapped && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF) {
			try {
				return InetAddress.getByAddress(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
			} catch (UnknownHostException e) {
				return addr; // HTT: getByAddress with a 4-byte array never throws
			}
		}
		return addr;
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
		var candidates = resolver.resolve(host);
		if (allowPrivateUrls)
			return candidates[0];
		for (var c : candidates)
			if (! isDeniedAddress(c))
				return c;
		throw iaex("No allowed address for host '%s': every resolved candidate is private/loopback/link-local (set allowPrivateUrls to allow local-dev targets)", host);
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
		if (location == null || location.isBlank())
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
