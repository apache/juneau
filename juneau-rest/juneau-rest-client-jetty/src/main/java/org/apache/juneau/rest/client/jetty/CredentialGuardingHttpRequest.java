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
package org.apache.juneau.rest.client.jetty;

import java.net.*;

import org.eclipse.jetty.client.*;
import org.eclipse.jetty.client.transport.*;

/**
 * {@link HttpRequest} that removes caller-set credential headers from the copy Jetty makes of it to follow a
 * {@code 3xx} redirect to a different origin.
 *
 * <p>
 * This is how the transport guards a client the caller supplies: that client is not a
 * {@link CredentialGuardingHttpClient} and is not changed, but {@link HttpClient}'s default {@code copyRequest} delegates
 * to {@link #copy(URI)}, so a request created as this class is guarded on any client.  The copy is again this class, so
 * every hop of a redirect chain is guarded.  A client subclass that overrides {@code copyRequest} without calling the
 * default replaces this guard with its own handling.
 *
 * <p>
 * The rule and the header set are those of {@link CredentialGuardingHttpClient}.  Only the copied headers are removed:
 * Jetty adds a {@code Cookie} from the client's cookie store and an {@code Authorization} from its authentication store
 * to the copy afterwards, as for any request.
 */
final class CredentialGuardingHttpRequest extends HttpRequest {

	/**
	 * Constructor.
	 *
	 * @param client The client that sends the request.
	 * @param uri The request URI.
	 * @throws IllegalArgumentException If Jetty refuses the URI (for example a URI with a {@code null} host).
	 */
	CredentialGuardingHttpRequest(HttpClient client, URI uri) {
		this(client, new HttpConversation(), uri);
	}

	private CredentialGuardingHttpRequest(HttpClient client, HttpConversation conversation, URI uri) {
		super(client, conversation, uri);
	}

	@Override /* HttpRequest */
	public HttpRequest copy(URI newURI) {
		var copy = super.copy(newURI);
		// A null URI copies the request for its own origin (for example to replay it with authentication).
		return newURI == null ? copy : CredentialGuardingHttpClient.stripIfCrossOrigin(this, newURI, copy);
	}

	@Override /* HttpRequest */
	protected HttpRequest copyInstance(URI newURI) {
		return new CredentialGuardingHttpRequest(getHttpClient(), getConversation(), newURI);
	}
}
