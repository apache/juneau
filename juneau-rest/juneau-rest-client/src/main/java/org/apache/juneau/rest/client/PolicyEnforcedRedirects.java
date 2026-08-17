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
package org.apache.juneau.rest.client;

import java.io.*;
import java.net.*;

import org.apache.juneau.http.remote.*;

/**
 * Shared Juneau-controlled redirect loop for SSRF-guard-active ({@link TransportRequest#isSsrfGuardActive()})
 * {@code @Remote} requests.
 *
 * <p>
 * Every first-party transport that supports the SSRF guardrail's connect-time contract (see
 * {@link HttpTransport#supportsUrlPolicy()}) disables its own automatic redirect-following for policy-covered
 * requests and instead routes each hop through {@link #execute(TransportRequest, HopExecutor)}, which re-runs the
 * {@link RemoteUrlPolicy} pre-check &mdash; and, via the supplied {@link HopExecutor}, pin-on-connect &mdash; on
 * every {@code Location} hop.  A public {@code @Url} that redirects to {@code http://169.254.169.254/} or
 * {@code http://127.0.0.1/} is rejected exactly as a direct request to that target would be.
 *
 * <p>
 * Follows conventional 3xx semantics: {@code 301}/{@code 302}/{@code 303} rewrite the method to {@code GET} with
 * no body, unless the original method was already {@code GET}/{@code HEAD}; {@code 307}/{@code 308} preserve the
 * original method and body. A non-{@linkplain TransportBody#isRepeatable() repeatable} body cannot be safely
 * re-sent, so such a redirect is refused (fail closed) rather than risking a corrupt or partial replay.  The
 * chain is capped at {@link RemoteUrlPolicy#MAX_REDIRECT_HOPS} hops.
 *
 * @since 10.0.0
 */
public final class PolicyEnforcedRedirects {

	private PolicyEnforcedRedirects() {}

	/**
	 * Performs a single pinned HTTP exchange for one hop of a policy-covered redirect chain.
	 *
	 * <p>
	 * Implementations must pin-on-connect (resolve the request URI's host via
	 * {@link RemoteUrlPolicy#selectAllowedAddress}, then connect to that specific address) and must not themselves
	 * follow any redirect the target returns &mdash; {@link #execute(TransportRequest, HopExecutor)} owns the
	 * redirect loop.
	 */
	@FunctionalInterface
	public interface HopExecutor {

		/**
		 * Executes one pinned hop.
		 *
		 * @param hopRequest The request to send for this hop.
		 * @return The response received for this hop (not yet redirect-followed).
		 * @throws TransportException If a network-level error occurs, or the host cannot be pinned to an allowed address.
		 */
		TransportResponse execute(TransportRequest hopRequest) throws TransportException;
	}

	/**
	 * Runs the redirect loop for a policy-covered {@code @Remote} request.
	 *
	 * @param initial The initial request. {@link TransportRequest#isSsrfGuardActive()} is expected to be <jk>true</jk>.
	 * @param executor Performs one pinned hop; see {@link HopExecutor}.
	 * @return The final (non-redirect) response.
	 * @throws TransportException If a hop fails, the redirect chain exceeds {@link RemoteUrlPolicy#MAX_REDIRECT_HOPS},
	 * 	a hop targets a denied host, or a non-repeatable body would need to be replayed.
	 */
	@SuppressWarnings({
		"resource" // Returns the final hop's response for the caller to close; Eclipse JDT @Owning warning is by design.
	})
	public static TransportResponse execute(TransportRequest initial, HopExecutor executor) throws TransportException {
		var allowPrivateUrls = initial.isAllowPrivateUrls();
		var current = initial;
		for (var hop = 0; hop < RemoteUrlPolicy.MAX_REDIRECT_HOPS; hop++) {
			// Hop 0's URI was already validated (or is the operator-configured client root, which the calling engine
			// intentionally does not re-check -- see RemoteUrlPolicy.requireAllowedUrl) before this TransportRequest
			// was built; only a redirect Location -- a URL discovered at runtime -- needs a fresh pre-check here.
			if (hop > 0)
				requireAllowed(current.getUri(), allowPrivateUrls);
			var response = executor.execute(current);
			if (! isRedirect(response.getStatusCode()))
				return response;
			var location = requireLocation(response);
			closeQuietly(response);
			var target = RemoteUrlPolicy.resolveRedirectLocation(current.getUri(), location);
			current = forRedirect(current, target, response.getStatusCode());
		}
		throw new TransportException("Redirect limit exceeded (" + RemoteUrlPolicy.MAX_REDIRECT_HOPS
			+ " hops) while following a policy-covered @Remote redirect chain, starting at: " + initial.getUri());
	}

	private static void requireAllowed(URI uri, boolean allowPrivateUrls) throws TransportException {
		try {
			RemoteUrlPolicy.requireAllowedUrl(uri.toString(), allowPrivateUrls);
		} catch (IllegalArgumentException e) {
			throw new TransportException(e.getMessage(), e);
		}
	}

	private static boolean isRedirect(int statusCode) {
		return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
	}

	private static String requireLocation(TransportResponse response) throws TransportException {
		var h = response.getFirstHeader("Location");
		if (h == null)
			throw new TransportException("Redirect response (status " + response.getStatusCode() + ") is missing a Location header");
		return h.value();
	}

	private static void closeQuietly(TransportResponse response) {
		try {
			response.close();
		} catch (IOException e) {
			// Best-effort; the connection will be reclaimed by the pool/GC even if the close callback fails.
		}
	}

	private static TransportRequest forRedirect(TransportRequest current, URI target, int statusCode) throws TransportException {
		var preserveMethod = current.getMethod().equalsIgnoreCase("GET") || current.getMethod().equalsIgnoreCase("HEAD");
		var rewriteToGet = ! preserveMethod && (statusCode == 301 || statusCode == 302 || statusCode == 303);

		try {
			var target2 = new URI(target.getScheme(), target.getRawAuthority(), target.getRawPath(), target.getRawQuery(), target.getRawFragment());
			var builder = TransportRequest.builder()
				.method(rewriteToGet ? "GET" : current.getMethod())
				.uri(target2)
				.remoteUrlPolicy(true, false)
				.timeout(current.getTimeout());
			for (var h : current.getHeaders())
				builder.header(h.name(), h.value());
			if (! rewriteToGet) {
				var body = current.getBody();
				if (body != null) {
					if (! body.isRepeatable())
						throw new TransportException("Redirect (status " + statusCode + ") requires re-sending a "
							+ "non-repeatable request body, which cannot be safely replayed; refusing to follow "
							+ "(fail closed) for a policy-covered @Remote redirect chain: " + target);
					builder.body(body);
				}
			}
			return builder.build();
		} catch (URISyntaxException e) {
			throw new TransportException("Redirect Location did not form a valid request URI: " + target, e);
		}
	}
}
