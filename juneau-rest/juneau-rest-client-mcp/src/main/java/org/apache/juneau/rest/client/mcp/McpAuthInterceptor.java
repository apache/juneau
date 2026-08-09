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
package org.apache.juneau.rest.client.mcp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.function.*;

import org.apache.juneau.rest.client.*;

/**
 * A builder-configurable auth-token seam implemented as a {@link RestCallInterceptor}.
 *
 * <p>
 * On {@link #onInit(RestRequest)}, sets the {@code Authorization} request header to {@code "Bearer " + token},
 * where {@code token} comes from a caller-supplied {@link Supplier}. Sub-project F's OAuth/OIDC flows plug into
 * this seam by supplying their own token-supplier implementation (e.g. one that refreshes an expired access
 * token); this class implements no OAuth/OIDC flow itself.
 *
 * <p>
 * If the supplied token is <jk>null</jk> <b>or blank</b> (empty or all-whitespace), no {@code Authorization}
 * header is set for that request (the call proceeds unauthenticated) - a blank token is never sent as a
 * credential-less {@code Authorization: Bearer } header. If the token supplier itself throws,
 * {@link RestRequest#run()}'s documented interceptor-exception handling aborts the call before it is sent.
 *
 * <p>
 * <b>Note:</b> when a token is set, {@link RestRequest#debug()} logs the full outgoing request including all
 * headers, so enabling debug logging on a client using this interceptor will log the {@code Authorization}
 * header (and therefore the bearer token) in plain text.
 *
 * @since 10.0.0
 */
public class McpAuthInterceptor implements RestCallInterceptor {

	// Argument name constants for assertArgNotNull
	private static final String ARG_TOKEN_SUPPLIER = "tokenSupplier";
	private static final String ARG_TOKEN = "token";

	private final Supplier<String> tokenSupplier;

	/**
	 * Constructor.
	 *
	 * @param tokenSupplier Supplies the bearer token for each request. Must not be <jk>null</jk>.
	 */
	public McpAuthInterceptor(Supplier<String> tokenSupplier) {
		this.tokenSupplier = assertArgNotNull(ARG_TOKEN_SUPPLIER, tokenSupplier);
	}

	/**
	 * Creates an interceptor that always sends the same static bearer token.
	 *
	 * @param token The static bearer token. Must not be <jk>null</jk>.
	 * @return A new interceptor. Never <jk>null</jk>.
	 */
	public static McpAuthInterceptor ofStaticBearer(String token) {
		assertArgNotNull(ARG_TOKEN, token);
		return new McpAuthInterceptor(() -> token);
	}

	@Override /* Overridden from RestCallInterceptor */
	public void onInit(RestRequest req) throws Exception {
		var token = tokenSupplier.get();
		if (token != null && ! token.isBlank())
			req.header("Authorization", "Bearer " + token);
	}
}
