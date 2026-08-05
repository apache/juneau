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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

/**
 * Bridges a Dynamic-Client-Registration result ({@link McpClientRegistration}) into F1's existing token flows &mdash;
 * the {@link McpAuthorizationCodeAcquirer} (interactive auth-code + PKCE) and {@link McpTokenProvider}
 * (client-credentials / refresh).
 *
 * <p>
 * DCR changes only how the {@code client_id}/secret were obtained; it does <b>not</b> alter the {@code resource=}
 * (RFC 8707) or {@code iss} (RFC 9207 / SEP-2468) enforcement those flows already perform &mdash; the caller still sets
 * {@code resource(...)} / {@code expectedIssuer(...)} exactly as for a statically-configured client.  These helpers only
 * transplant the DCR-issued credential material (and, for the acquirer, keep the loopback redirect <em>path and port</em>
 * consistent with what was registered, so a bind-first {@link LoopbackRedirectUris#forPort forPort} registration matches
 * at a strict exact-match authorization server).
 *
 * @since 10.0.0
 */
public final class McpClientRegistrations {

	private McpClientRegistrations() {}

	/**
	 * Applies a DCR-issued registration's {@code client_id}, optional {@code client_secret}, and registered loopback
	 * redirect path (and port, for a bind-first registration) onto an {@link McpAuthorizationCodeAcquirer.Builder}.
	 *
	 * <p>
	 * When the registration's first redirect URI carries an explicit port (a bind-first
	 * {@link LoopbackRedirectUris#forPort forPort} registration), that port is wired onto the builder via
	 * {@link McpAuthorizationCodeAcquirer.Builder#redirectPort(int) redirectPort} so the loopback receiver binds exactly
	 * the port the authorization server was told; a port-agnostic registration leaves the acquirer on its ephemeral-port
	 * default.
	 *
	 * @param builder The acquirer builder.  Must not be <jk>null</jk>.
	 * @param registration The DCR-issued registration.  Must not be <jk>null</jk>.
	 * @return The same builder, for chaining.
	 */
	public static McpAuthorizationCodeAcquirer.Builder configure(McpAuthorizationCodeAcquirer.Builder builder, McpClientRegistration registration) {
		assertArgNotNull("builder", builder);
		assertArgNotNull("registration", registration);
		builder.clientId(registration.clientId());
		registration.clientSecret().ifPresent(builder::clientSecret);
		// Keep the loopback receiver's callback path AND port consistent with the redirect URI that was actually
		// registered: a strict exact-match AS matches the redirect URI exactly, so a bind-first forPort(port,path)
		// registration only works if the receiver binds the same path and the same port.
		if (! registration.redirectUris().isEmpty()) {
			var redirect = registration.redirectUris().get(0);
			var path = redirect.getPath();
			if (path != null && path.startsWith("/"))
				builder.redirectPath(path);
			var port = redirect.getPort();
			if (port > 0)
				builder.redirectPort(port);
		}
		return builder;
	}

	/**
	 * Applies a DCR-issued registration's {@code client_id} and optional {@code client_secret} onto an
	 * {@link McpTokenProvider.Builder} (client-credentials / refresh modes).
	 *
	 * @param builder The token-provider builder.  Must not be <jk>null</jk>.
	 * @param registration The DCR-issued registration.  Must not be <jk>null</jk>.
	 * @return The same builder, for chaining.
	 */
	public static McpTokenProvider.Builder configure(McpTokenProvider.Builder builder, McpClientRegistration registration) {
		assertArgNotNull("builder", builder);
		assertArgNotNull("registration", registration);
		builder.clientId(registration.clientId());
		registration.clientSecret().ifPresent(builder::clientSecret);
		return builder;
	}
}
