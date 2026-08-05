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

import java.net.*;
import java.util.*;

/**
 * Builds the pair of loopback redirect URIs (SEP-837 loopback redirect handling) an MCP native/CLI client registers
 * with an authorization server during Dynamic Client Registration.
 *
 * <p>
 * The MCP client-registration spec's CIMD example registers <b>both</b> {@code http://127.0.0.1} and
 * {@code http://localhost} forms of the callback so the authorization server accepts whichever the operating system's
 * browser resolves the loopback interface to.  Two strategies are supported (RFC 8252 &sect;7.3), both fully usable
 * end-to-end (the acquirer binds the port that was registered):
 * <ul>
 * 	<li><b>Port-agnostic</b> ({@link #portAgnostic(String)}) &mdash; the <b>easy default</b>: register the two host-only
 * 		forms and rely on RFC 8252 &sect;7.3 loopback port-agnostic matching (a spec-conformant authorization server MUST
 * 		permit any port on a loopback redirect).  The acquirer then binds an ephemeral port and no coordination is needed.
 * 	<li><b>Bind-first</b> ({@link #forPort(int, String)}) &mdash; for <b>strict exact-match</b> authorization servers
 * 		that reject port-agnostic loopback redirects: register the two port-bearing forms for a caller-chosen fixed port,
 * 		then bind that same fixed port on the acquirer (via {@link McpAuthorizationCodeAcquirer.Builder#redirectPort(int)},
 * 		wired automatically by {@link McpClientRegistrations#configure}) so the receiver listens on exactly the port the
 * 		authorization server was told.
 * </ul>
 *
 * @since 10.0.0
 */
public final class LoopbackRedirectUris {

	private LoopbackRedirectUris() {}

	/**
	 * Returns the port-agnostic loopback redirect URIs (RFC 8252 &sect;7.3): {@code http://127.0.0.1<path>} and
	 * {@code http://localhost<path>}.
	 *
	 * @param path The callback path (must start with {@code /}).  Must not be <jk>null</jk> or blank.
	 * @return An immutable, order-preserving list of the two loopback redirect URIs.  Never <jk>null</jk>.
	 */
	public static List<URI> portAgnostic(String path) {
		var p = assertArgNotNullOrBlank("path", path);
		assertArg(p.startsWith("/"), "path must start with '/' (was '%s')", p);
		return List.of(
			URI.create("http://127.0.0.1" + p),
			URI.create("http://localhost" + p));
	}

	/**
	 * Returns the port-bearing loopback redirect URIs (bind-first strategy): {@code http://127.0.0.1:<port><path>} and
	 * {@code http://localhost:<port><path>}.
	 *
	 * <p>
	 * Register these for a caller-chosen fixed port, then bind that same port on the acquirer
	 * ({@link McpAuthorizationCodeAcquirer.Builder#redirectPort(int)}) so the receiver listens on exactly the port the
	 * authorization server was told.
	 *
	 * @param port The fixed loopback port to register (and later bind on the receiver).  Must be in {@code 1..65535}.
	 * @param path The callback path (must start with {@code /}).  Must not be <jk>null</jk> or blank.
	 * @return An immutable, order-preserving list of the two loopback redirect URIs.  Never <jk>null</jk>.
	 */
	public static List<URI> forPort(int port, String path) {
		assertArg(port >= 1 && port <= 65535, "port must be in 1..65535 (was %s)", port);
		var p = assertArgNotNullOrBlank("path", path);
		assertArg(p.startsWith("/"), "path must start with '/' (was '%s')", p);
		return List.of(
			URI.create("http://127.0.0.1:" + port + p),
			URI.create("http://localhost:" + port + p));
	}
}
