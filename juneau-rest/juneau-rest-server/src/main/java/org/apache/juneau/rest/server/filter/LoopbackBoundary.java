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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.rest.server.*;

import jakarta.servlet.http.*;

/**
 * The request-authenticity half of a loopback application's write protection: decides whether a request came from
 * the page this process served, and refuses it when it did not.
 *
 * <h5 class='section'>What this answers, and what it does not</h5>
 * <p>
 * An application bound to <c>127.0.0.1</c> is reachable only from this host, but "reachable only from this host"
 * is not an authorization boundary &mdash; every browser the user has open is on this host, and any page in any of
 * them can attempt a request to the port.  This class answers exactly one question:
 * <p class='bcode'>
 * 	Did this request come from the page this process served?
 * </p>
 * <p>
 * It does <b>not</b> answer <i>did the user mean it</i>.  A user who has the application's own page open, and who
 * is induced to click something on it, produces a request that passes every check here.  That is a question of
 * intent, and it needs a separate, independent mechanism &mdash; a typed confirmation, an arming gate, a
 * per-action phrase naming the specific target.  <b>The two gates are deliberately not combined, in this class or
 * in its naming.</b>  Treating an intent gate as though it authenticated the request, or this boundary as though
 * it established intent, is the specific confusion this separation exists to prevent: an application with only an
 * arming gate is forgeable by any page in the browser, and an application with only this boundary will faithfully
 * execute whatever its own page was tricked into asking for.
 *
 * <h5 class='section'>The checks</h5>
 * <table class='styled'>
 * 	<tr><th>Check</th><th>Applies to</th><th>Rule</th><th>Rejection</th></tr>
 * 	<tr>
 * 		<td>{@code Host}</td><td><b>every</b> request</td>
 * 		<td>equals the configured authority exactly</td>
 * 		<td>421 Misdirected Request</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@code Origin}</td><td>state-changing requests</td>
 * 		<td>present, and exactly <c>http://&lt;authority&gt;</c></td>
 * 		<td>403 Forbidden</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@code Sec-Fetch-Site}</td><td>state-changing requests</td>
 * 		<td>absent, or exactly <c>same-origin</c></td>
 * 		<td>403 Forbidden</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@code Content-Type}</td><td>state-changing requests</td>
 * 		<td>base type is exactly <c>application/json</c></td>
 * 		<td>415 Unsupported Media Type</td>
 * 	</tr>
 * 	<tr>
 * 		<td>CSRF token</td><td>state-changing requests</td>
 * 		<td>the configured header equals the process's {@link SynchronizerToken}</td>
 * 		<td>403 Forbidden</td>
 * 	</tr>
 * </table>
 *
 * <h5 class='section'>Why each check is present</h5>
 * <p>
 * <b>{@code Host} on every request, not only writes.</b>  This is the check that defeats DNS rebinding, and it is
 * the reason the boundary cannot be scoped to the write path.  An attacker serves a page from
 * <c>http://evil.example</c> with a very short DNS TTL, then re-resolves that name to <c>127.0.0.1</c>.  Requests
 * the page then makes to <c>http://evil.example/...</c> are <i>same-origin</i> from the browser's point of view:
 * no CORS, no preflight, {@code Origin} is the page's own origin and therefore consistent, and the response body
 * is readable.  Neither origin checking nor a CSRF token stops this &mdash; the page can simply fetch this
 * application's own HTML and scrape the token out of it.  What the attacker cannot change is the {@code Host}
 * header, which after rebinding still carries <c>evil.example</c>.
 * <p>
 * Restricting that check to writes would leave every read surface open to the one attack it exists to stop, and a
 * rebound page that can read the application's data tables is already an exfiltration problem whether or not it
 * can write.
 * <p>
 * <b>{@code Origin} on writes.</b>  Browsers set it on every cross-origin request and on same-origin POSTs, and
 * page JavaScript cannot forge it.  An absent {@code Origin} is a rejection rather than a pass, so a client that
 * simply omits the header does not thereby skip the check.
 * <p>
 * <b>A JSON-only content type on writes.</b>  The three form-encodable content types
 * (<c>application/x-www-form-urlencoded</c>, <c>multipart/form-data</c>, <c>text/plain</c>) are precisely the
 * ones a cross-origin request may use without a preflight, which is what makes a plain cross-origin
 * <c>&lt;form&gt;</c> POST possible with no JavaScript at all.  Refusing them forces any cross-origin caller into
 * a preflight, which this application never answers with CORS headers, so the real request is never sent.
 * <p>
 * <b>{@code Sec-Fetch-Site} on writes.</b>  Tolerates absence, so a non-browser client used during development is
 * not broken, and rejects any present-but-wrong value.  It adds nothing against a local process, which can set
 * the header freely; it is here because it is one line and closes a browser-side gap cheaply.
 * <p>
 * <b>A CSRF token on writes.</b>  A server-held {@link SynchronizerToken}, embedded in the served page.  See that
 * class for why a double-submit cookie is unsound on a loopback port.
 *
 * <h5 class='section'>One canonical origin</h5>
 * <p>
 * The authority is a single exact spelling.  If the application is reached at <c>127.0.0.1:8790</c> then
 * <c>localhost:8790</c> is <b>not</b> accepted, and vice versa.  Accepting both doubles the surface for no
 * benefit, and an application that links only to the spelling it accepts never notices the difference.
 *
 * <h5 class='section'>What this does not defend against</h5>
 * <p>
 * These are accepted residual risks, not oversights.  Stating them plainly matters, because a boundary described
 * as stopping more than it does invites the wrong decisions to be built on top of it.
 * <ul>
 * 	<li><b>A local process running as the same user is not defended against, and cannot be.</b>  It already holds
 * 		every credential this application holds &mdash; the same keychain entries, the same config files, the same
 * 		environment.  It does not need this application to do anything.  Nor does any of the machinery here
 * 		obstruct it: everything the browser must be able to present in order for the UI to work is equally
 * 		obtainable by a local HTTP client that fetches the same page first and reads the token out of it.  A
 * 		shared secret cannot separate "our page" from "a local program impersonating our page", because both are
 * 		given the secret by the same server.  This boundary raises the bar for pages in a browser; against a
 * 		same-user local process it is not a control at all.
 * 	<li><b>A malicious or compromised browser extension is not defended against.</b>  An extension can read the
 * 		page, read the token out of it, and issue requests as the page.  Every check here is satisfied from inside
 * 		the page.
 * 	<li><b>The human is not authenticated.</b>  There is no login.  The operating-system session is the
 * 		authentication; whoever is at the keyboard is the user.
 * 	<li><b>Intent is not established.</b>  See the first section: that is a separate gate's job.
 * </ul>
 *
 * <h5 class='section'>Non-browser callers</h5>
 * <p>
 * There is deliberately no path-exemption or trusted-caller list.  A non-browser client that must write &mdash;
 * including a process calling back into its own loopback port &mdash; presents the same {@code Host},
 * {@code Origin}, content type and token as the page does.  An exemption list is the shape of API that gets one
 * more entry added under deadline pressure until it covers the endpoint that mattered, so the boundary does not
 * offer one.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	LoopbackBoundary <jv>boundary</jv> = LoopbackBoundary.<jsm>create</jsm>()
 * 		.authority(<js>"127.0.0.1:8790"</js>)
 * 		.token(SynchronizerToken.<jsm>generate</jsm>())
 * 		.build();
 *
 * 	LoopbackBoundary.Result <jv>result</jv> = <jv>boundary</jv>.check(<jv>request</jv>);
 * 	<jk>if</jk> (! <jv>result</jv>.isAllowed())
 * 		<jv>response</jv>.sendError(<jv>result</jv>.status(), <jv>result</jv>.message());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoopbackBoundaryFilter}
 * 	<li class='jc'>{@link SynchronizerToken}
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are HTTP header names (e.g. Origin); a constant per header would obscure the wire contract.
})
public class LoopbackBoundary {

	/** Default name of the request header carrying the CSRF token. */
	public static final String DEFAULT_CSRF_HEADER = "X-Csrf-Token";

	/** The only content type accepted on a state-changing request. */
	public static final String JSON_CONTENT_TYPE = "application/json";

	private final String authority;
	private final String origin;
	private final String csrfHeader;
	private final SynchronizerToken token;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.  Must have had an authority and a token set.
	 * @throws IllegalArgumentException If the builder carries no authority or no token.
	 */
	protected LoopbackBoundary(Builder builder) {
		if (builder.authority == null)
			throw iaex("An authority is required; call Builder.authority(...).");
		if (builder.token == null)
			throw iaex("A token is required; call Builder.token(...).");
		this.authority = builder.authority;
		this.origin = "http://" + builder.authority;
		this.csrfHeader = builder.csrfHeader;
		this.token = builder.token;
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Applies the boundary to a request.
	 *
	 * @param req The request to check.  Must not be <jk>null</jk>.
	 * @return {@link Result#ALLOWED} when every applicable check passed, else the first rejection encountered.
	 */
	public Result check(HttpServletRequest req) {
		assertArgNotNull("req", req);

		// Host applies to every request; it is the DNS-rebinding check and a read is just as exfiltratable as a write.
		var host = req.getHeader("Host");
		if (host == null || ! host.equalsIgnoreCase(authority))
			return reject(Reason.HOST_MISMATCH, 421,
				"Request 'Host' does not match this server's expected authority '%s'.", authority);

		if (! isStateChanging(req.getMethod()))
			return Result.ALLOWED;

		var origin2 = req.getHeader("Origin");
		if (origin2 == null || origin2.isBlank())
			return reject(Reason.ORIGIN_MISSING, 403,
				"A state-changing request must carry an 'Origin' header of '%s'.", origin);
		if (! origin2.equals(origin))
			return reject(Reason.ORIGIN_MISMATCH, 403,
				"Request 'Origin' is not this server's origin '%s'.", origin);

		// The presented value is deliberately not echoed back: it is caller-controlled, and a rejection message is
		// rendered into a response body and a log line, neither of which should carry attacker-chosen text.
		var fetchSite = req.getHeader("Sec-Fetch-Site");
		if (fetchSite != null && ! "same-origin".equals(fetchSite))
			return reject(Reason.FETCH_SITE_NOT_SAME_ORIGIN, 403,
				"Request 'Sec-Fetch-Site' must be absent or 'same-origin' on a state-changing request.");

		if (! isJson(req.getContentType()))
			return reject(Reason.CONTENT_TYPE_NOT_JSON, 415,
				"A state-changing request must use content type '%s'.", JSON_CONTENT_TYPE);

		var presented = req.getHeader(csrfHeader);
		if (presented == null || presented.isBlank())
			return reject(Reason.CSRF_TOKEN_MISSING, 403,
				"A state-changing request must carry this server's CSRF token in the '%s' header.", csrfHeader);
		if (! token.matches(presented))
			return reject(Reason.CSRF_TOKEN_MISMATCH, 403,
				"The CSRF token in the '%s' header is not this server's token.", csrfHeader);

		return Result.ALLOWED;
	}

	/**
	 * Whether a request method is treated as state-changing, and therefore subject to the write checks.
	 *
	 * <p>
	 * {@code GET}, {@code HEAD}, {@code OPTIONS} and {@code TRACE} are read-only.  Everything else, including an
	 * unrecognized method, is state-changing.
	 *
	 * <p>
	 * Delegates to {@link MethodSafety#isSafe(String)} rather than carrying its own copy of the safe-method set.
	 * The startup check that refuses a {@link org.apache.juneau.rest.server.Mutating @Mutating} operation bound to
	 * a safe method reads the same definition, and a boundary that disagreed with that check about which methods
	 * are writes would silently defeat both.
	 *
	 * @param method The request method.  Can be <jk>null</jk>, which is treated as state-changing.
	 * @return <jk>true</jk> if the method is subject to the write checks.
	 */
	public static boolean isStateChanging(String method) {
		return ! MethodSafety.isSafe(method);
	}

	/**
	 * The expected {@code Host} value.
	 *
	 * @return The configured authority, e.g. {@code "127.0.0.1:8790"}.
	 */
	public String authority() { return authority; }

	/**
	 * The single accepted request origin, derived from the authority.
	 *
	 * @return The canonical origin, e.g. {@code "http://127.0.0.1:8790"}.
	 */
	public String origin() { return origin; }

	/**
	 * The name of the header a state-changing request must carry the CSRF token in.
	 *
	 * @return The CSRF header name.
	 */
	public String csrfHeader() { return csrfHeader; }

	/**
	 * The headers an in-process client must add when calling this application's own loopback port.
	 *
	 * <p>
	 * An application that calls back into itself over HTTP &mdash; a mock of an external service mounted on its
	 * own port, a background task that drives its own API &mdash; is a state-changing caller like any other, and
	 * the boundary offers it no exemption (see the class javadoc's non-browser-callers section).  Rather than
	 * leaving each such caller to rediscover what the boundary wants, this returns it: the accepted
	 * {@code Origin} and the CSRF token under its configured header name.
	 *
	 * <p>
	 * {@code Host} is deliberately absent.  An HTTP client derives it from the request URI, so a caller already
	 * sending to this application's authority sends the right one; and {@code Host} is a restricted header that
	 * {@link java.net.http.HttpClient} refuses to set anyway.  The caller's remaining obligation is to send
	 * {@code Content-Type: application/json} on writes, which such a client is normally doing already.
	 *
	 * <p>
	 * This is not a back door.  It hands the token only to code already running inside this process, which could
	 * equally read it off {@link #token()}; it exists so that "call your own port correctly" does not become the
	 * argument for adding a path exemption.
	 *
	 * @return An immutable map of header name to value.
	 */
	public Map<String,String> selfCallHeaders() {
		return Map.of("Origin", origin, csrfHeader, token.value());
	}

	/**
	 * The token a page must embed and a state-changing request must present.
	 *
	 * @return This boundary's token.
	 */
	public SynchronizerToken token() { return token; }

	/**
	 * Whether a {@code Content-Type} header value's base type is exactly {@code application/json}.
	 *
	 * <p>
	 * Parameters are ignored, so {@code application/json;charset=utf-8} passes.  A suffixed type such as
	 * {@code application/problem+json} does not: the check exists to exclude the form-encodable types, and
	 * widening it to "anything ending in json" would be a per-type judgement call at a security boundary.
	 */
	private static boolean isJson(String contentType) {
		if (contentType == null)
			return false;
		var semi = contentType.indexOf(';');
		var base = (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
		return base.equalsIgnoreCase(JSON_CONTENT_TYPE);
	}

	private static Result reject(Reason reason, int status, String message, Object... args) {
		return new Result(reason, status, f(message, args));
	}

	/**
	 * Why a request was rejected.
	 *
	 * <p>
	 * Enumerated rather than collapsed into a single "forbidden" so the application can render an accurate
	 * reason and log an actionable one.  Distinguishing missing from mismatched leaks nothing a cross-origin
	 * caller does not already know about its own request.
	 */
	public enum Reason {

		/** The {@code Host} header was absent or was not this server's authority.  Rejected on every request. */
		HOST_MISMATCH,

		/** A state-changing request carried no {@code Origin} header. */
		ORIGIN_MISSING,

		/** A state-changing request's {@code Origin} was not this server's origin. */
		ORIGIN_MISMATCH,

		/** A state-changing request's {@code Sec-Fetch-Site} was present and was not {@code same-origin}. */
		FETCH_SITE_NOT_SAME_ORIGIN,

		/** A state-changing request's content type was not {@code application/json}. */
		CONTENT_TYPE_NOT_JSON,

		/** A state-changing request carried no CSRF token header. */
		CSRF_TOKEN_MISSING,

		/** A state-changing request's CSRF token was not this server's token. */
		CSRF_TOKEN_MISMATCH
	}

	/**
	 * The outcome of {@link LoopbackBoundary#check(HttpServletRequest)}: either allowed, or a rejection carrying
	 * the reason, the HTTP status to answer with, and a message safe to return to the caller.
	 */
	public static final class Result {

		/** The outcome of a request that passed every applicable check. */
		public static final Result ALLOWED = new Result(null, 0, null);

		private final Reason reason;
		private final int status;
		private final String message;

		Result(Reason reason, int status, String message) {
			this.reason = reason;
			this.status = status;
			this.message = message;
		}

		/**
		 * Whether the request passed.
		 *
		 * @return <jk>true</jk> if the request may proceed.
		 */
		public boolean isAllowed() { return reason == null; }

		/**
		 * Why the request was rejected.
		 *
		 * @return The rejection reason, or <jk>null</jk> when {@link #isAllowed()} is <jk>true</jk>.
		 */
		public Reason reason() { return reason; }

		/**
		 * The HTTP status to answer a rejected request with.
		 *
		 * @return The status code, or {@code 0} when {@link #isAllowed()} is <jk>true</jk>.
		 */
		public int status() { return status; }

		/**
		 * A message describing the rejection.
		 *
		 * <p>
		 * Names the header at fault and what was expected of it, and never echoes the value the request
		 * presented for the CSRF token or reveals this server's token.
		 *
		 * @return The message, or <jk>null</jk> when {@link #isAllowed()} is <jk>true</jk>.
		 */
		public String message() { return message; }

		@Override /* Object */
		public String toString() { return isAllowed() ? "ALLOWED" : reason + "(" + status + "): " + message; }
	}

	/**
	 * Builder for {@link LoopbackBoundary}.
	 */
	public static class Builder {

		String authority;
		String csrfHeader = DEFAULT_CSRF_HEADER;
		SynchronizerToken token;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * The authority every request's {@code Host} must equal, and from which the accepted {@code Origin} is
		 * derived.
		 *
		 * <p>
		 * Required.  Must be the exact host-and-port spelling the application links to, e.g.
		 * {@code "127.0.0.1:8790"} &mdash; the boundary accepts one spelling, not a set (see the class javadoc's
		 * canonical-origin section).
		 *
		 * @param value The expected authority.  Must not be <jk>null</jk>, blank, or carry a scheme or path.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>, blank, or is not a bare
		 * 	host-and-port.
		 */
		public Builder authority(String value) {
			assertArgNotNull("value", value);
			var v = value.trim();
			if (v.isEmpty())
				throw iaex("Argument 'value' must not be blank.");
			if (v.contains("://") || v.indexOf('/') >= 0)
				throw iaex("Argument 'value' must be a bare host and port with no scheme or path: ''%s''.", value);
			authority = v;
			return this;
		}

		/**
		 * Convenience form of {@link #authority(String)} taking the host and port separately.
		 *
		 * @param host The bind host, e.g. {@code "127.0.0.1"}.  Must not be <jk>null</jk> or blank.
		 * @param port The bind port.  Must be positive.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code host} is <jk>null</jk>/blank or {@code port} is not
		 * 	positive.
		 */
		public Builder authority(String host, int port) {
			assertArgNotNull("host", host);
			if (port <= 0)
				throw iaex("Argument 'port' must be positive: %s.", port);
			return authority(host.trim() + ":" + port);
		}

		/**
		 * The header a state-changing request must carry the CSRF token in.
		 *
		 * @param value The header name.  Must not be <jk>null</jk> or blank.  Defaults to
		 * 	{@link LoopbackBoundary#DEFAULT_CSRF_HEADER}.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
		 */
		public Builder csrfHeader(String value) {
			assertArgNotNull("value", value);
			if (value.isBlank())
				throw iaex("Argument 'value' must not be blank.");
			csrfHeader = value.trim();
			return this;
		}

		/**
		 * The token a state-changing request must present.
		 *
		 * <p>
		 * Required.  Normally one {@link SynchronizerToken#generate() generated} token per process, also embedded
		 * into every page the application serves.
		 *
		 * @param value The token.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder token(SynchronizerToken value) {
			assertArgNotNull("value", value);
			token = value;
			return this;
		}

		/**
		 * Builds the boundary.
		 *
		 * @return A new {@link LoopbackBoundary}.
		 * @throws IllegalArgumentException If no authority or no token was supplied.
		 */
		public LoopbackBoundary build() {
			return new LoopbackBoundary(this);
		}
	}
}
