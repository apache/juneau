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
package org.apache.juneau.examples.mcp.secured;

import java.net.*;

import org.apache.juneau.examples.mcp.*;
import org.apache.juneau.rest.server.auth.jwt.*;
import org.apache.juneau.rest.server.mcp.v20260728.*;

/**
 * The OAuth 2.1-secured variant of {@link ExampleMcpServer}: identical notes-service surface (tools, prompt,
 * resources, resource template, subscriptions), but every {@code POST /} JSON-RPC call now requires a valid
 * bearer token.
 *
 * <p>
 * This class changes nothing about <i>what</i> the server exposes &mdash; it inherits
 * {@link ExampleMcpServer#createMcpConfig()} unmodified, so the same {@code publishNote}/{@code deleteNote}
 * tools, {@code summarize} prompt, and {@code note:///...} resources are still there. The only override is
 * {@link #createMcpOptions()}, which layers an {@code McpResourceServerConfig} on top of the parent's
 * capabilities. That single override is the entire security story:
 *
 * <ul>
 * 	<li><b>{@code setEnabled(true)}</b> &mdash; turns on the RS (resource-server) gate at all. Off by default,
 * 		so an ordinary {@link ExampleMcpServer} is completely unaffected by this class existing.
 * 	<li><b>{@code setResource(...)}</b> &mdash; this server's own canonical URL (RFC 9728 {@code resource} /
 * 		RFC 8707 default audience). A bearer token's {@code aud} claim must include this exact URL or it is
 * 		rejected &mdash; the confused-deputy defense: a token minted for some OTHER resource cannot be replayed
 * 		here even if it is otherwise perfectly valid and signed by a trusted issuer.
 * 	<li><b>{@code setTokenValidator(...)}</b> &mdash; a {@link JwtTokenValidator} that checks the token's
 * 		signature (against {@link OfflineAuthorizationServer#jwkSource()}, offline &mdash; no JWKS HTTP fetch),
 * 		{@code iss} (must equal {@link OfflineAuthorizationServer#issuerUri()}), and {@code aud}/{@code exp}/
 * 		{@code nbf} (validated against the resource URL above and wall-clock time).
 * 	<li><b>{@code addAuthorizationServer(...)}</b> &mdash; advertised in the RFC 9728 Protected Resource
 * 		Metadata (PRM) document a client fetches from the well-known {@code .well-known/oauth-protected-resource}
 * 		path this framework serves automatically once RS auth is enabled. This is how a compliant client
 * 		discovers WHICH authorization server to go get a token from, without that URL being hardcoded into the
 * 		client at all.
 * 	<li><b>{@code addRequiredScope(...)}</b> &mdash; the coarse, endpoint-wide baseline scope ({@link #READ_SCOPE})
 * 		every request must carry (on top of a merely valid, correctly-audienced token) before any JSON-RPC
 * 		method dispatches. A token missing it gets a {@code 403} with a {@code WWW-Authenticate} challenge
 * 		naming the missing scope, not a silent failure.
 * 	<li><b>{@code addOperationScope(...)}</b> &mdash; SEP-2350 per-operation step-up (H3): {@code publishNote}
 * 		and {@code deleteNote} additionally require {@link #WRITE_SCOPE} on top of the baseline. A token
 * 		carrying only {@link #READ_SCOPE} can discover the server and read resources, but a mutating tool call
 * 		gets its own scoped {@code 403 insufficient_scope} naming {@link #WRITE_SCOPE} specifically &mdash; the
 * 		baseline scope alone is never enough to invoke either write tool.
 * </ul>
 *
 * <p>
 * The net effect on the wire: an unauthenticated (or wrongly-audienced, or expired, or insufficiently-scoped)
 * {@code POST /} now gets {@code 401}/{@code 403} plus a {@code WWW-Authenticate: Bearer ...} challenge
 * instead of ever reaching {@link ExampleMcpServer}'s tool/prompt/resource handlers; a request bearing a valid
 * bearer token dispatches exactly as it always did.
 *
 * <h5 class='section'>The one wrinkle this class works around:</h5>
 * <p>
 * {@link #getResource() The resource URL} is this server's OWN address, which is a problem when
 * {@link SecuredExampleServer} boots on an OS-assigned ephemeral port (as the end-to-end test does): naively,
 * that address is not knowable until AFTER Jetty has bound its socket. It is tempting to assume
 * {@link #createMcpOptions()} (like {@code createMcpConfig()}) is called lazily on the first incoming MCP
 * request, late enough to bind the resource URL once the port is known &mdash; but it is NOT: the REST
 * framework eagerly walks every {@code @Bean}-annotated method declared on the resource (including this
 * inherited {@code getMcpOptions()}) while building the servlet's {@code RestContext}, i.e. during Jetty's
 * OWN servlet initialization, strictly before {@code Server.start()} returns. A constructor-supplied resource
 * URL is therefore required; see {@link SecuredExampleServer#start(int)} for how it opens the Jetty connector
 * (and thus learns the real ephemeral port) before this servlet is even constructed.
 *
 * @serial exclude
 */
public class SecuredExampleMcpServer extends ExampleMcpServer {

	private static final long serialVersionUID = 1L;

	/**
	 * The baseline OAuth scope every request to this server must carry, on top of a valid, correctly-audienced
	 * token. Advertised in the PRM document's {@code scopes_supported} and required via
	 * {@code McpResourceServerConfig.addRequiredScope(...)}.
	 */
	public static final String READ_SCOPE = "mcp.read";

	/**
	 * The additional step-up scope {@code publishNote}/{@code deleteNote} require on top of {@link #READ_SCOPE}
	 * (H3, SEP-2350 per-operation scoping), via {@code McpResourceServerConfig.addOperationScope(...)}.
	 */
	public static final String WRITE_SCOPE = "mcp.write";

	private final transient OfflineAuthorizationServer authServer;
	private final transient URI resource;

	/**
	 * Constructor.
	 *
	 * @param authServer The offline authorization server this instance validates bearer tokens against
	 * 	(its {@link OfflineAuthorizationServer#jwkSource() public key} and
	 * 	{@link OfflineAuthorizationServer#issuerUri() issuer}). Must not be <jk>null</jk>.
	 * @param resource This server's own canonical root URL (e.g. {@code http://localhost:5001/}), known and
	 * 	fixed before construction &mdash; see the class javadoc's "one wrinkle" section for why this cannot be
	 * 	supplied later. Must not be <jk>null</jk>.
	 */
	public SecuredExampleMcpServer(OfflineAuthorizationServer authServer, URI resource) {
		if (authServer == null)
			throw new IllegalArgumentException("authServer must not be null");
		if (resource == null)
			throw new IllegalArgumentException("resource must not be null");
		this.authServer = authServer;
		this.resource = resource;
	}

	/**
	 * Returns this server's own canonical resource URL, as supplied to the constructor.
	 *
	 * @return The resource URL. Never <jk>null</jk>.
	 */
	public URI getResource() {
		return resource;
	}

	@Override
	protected McpOptions createMcpOptions() {
		var validator = JwtTokenValidator.create()
			.jwkSource(authServer.jwkSource())
			.issuer(authServer.issuerUri().toString())
			.audience(resource.toString())
			.build();
		return super.createMcpOptions().resourceServer(rs -> rs
			.setEnabled(true)
			.setResource(resource)
			.setTokenValidator(validator)
			.addAuthorizationServer(authServer.issuerUri())
			.addRequiredScope(READ_SCOPE)
			// H3: publishNote/deleteNote step up to WRITE_SCOPE on top of the READ_SCOPE baseline above.
			.addOperationScope("tools/call", "publishNote", WRITE_SCOPE)
			.addOperationScope("tools/call", "deleteNote", WRITE_SCOPE));
	}
}
