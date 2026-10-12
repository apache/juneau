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
import java.util.*;

import org.apache.juneau.http.remote.*;
import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.*;

/**
 * {@link SocketAddressResolver} that pin-on-connects SSRF-guard-active {@code @Remote} connections: when the
 * connection being opened belongs to a destination tagged with {@link #PINNED_TAG}, resolution is restricted to a
 * single address selected by {@link RemoteUrlPolicy#selectAllowedAddress} &mdash; the exact address Jetty's connector
 * then connects the socket to, while still using the original hostname (from the request's own origin) for TLS
 * SNI/hostname verification. Connections for every other destination on the same {@code HttpClient} delegate to the
 * wrapped resolver unchanged.
 *
 * <p>
 * Jetty keys its destinations (and so its connection pools) by origin <em>including</em> the request tag, so tagging
 * guarded requests gives them a connection pool of their own. Every connection in that pool is opened through this
 * pin, and a guarded request can never be handed an idle connection that an unguarded request to the same origin
 * opened without it. Keying on the destination rather than on the calling thread also keeps the pin in force when
 * Jetty opens a connection for a queued guarded request from one of its own threads.
 *
 * @since 10.0.0
 */
final class PolicyPinningSocketAddressResolver implements SocketAddressResolver {

	/**
	 * The Jetty request tag that marks an SSRF-guard-active request.
	 *
	 * <p>
	 * A plain {@link Object}, so it equals only itself: Jetty's destination lookup (which compares tags with
	 * {@code equals()}) never puts a request tagged with anything else in the pinned connection pool, and this
	 * resolver's own check compares by identity.
	 */
	static final Object PINNED_TAG = new Object();

	private final SocketAddressResolver delegate;

	PolicyPinningSocketAddressResolver(SocketAddressResolver delegate) {
		this.delegate = delegate;
	}

	@Override /* SocketAddressResolver */
	public void resolve(String host, int port, Map<String, Object> context, Promise<List<InetSocketAddress>> promise) {
		if (! isPinned(context)) {
			delegate.resolve(host, port, context, promise);
			return;
		}
		try {
			// A destination is tagged only for an SSRF-guard-active request, and the guard is active only when the
			// allowPrivateUrls opt-in is off (TransportRequest.isSsrfGuardActive()), so the deny-private checks always apply here.
			var pinned = RemoteUrlPolicy.selectAllowedAddress(host, false, RemoteUrlPolicy.AddressResolver.DEFAULT);
			promise.succeeded(List.of(new InetSocketAddress(pinned, port)));
		} catch (UnknownHostException | RuntimeException e) {
			promise.failed(e);
		}
	}

	// Jetty puts the destination the connection is being opened for into the resolve context.
	private static boolean isPinned(Map<String, Object> context) {
		return context.get(Destination.CONTEXT_KEY) instanceof Destination d && d.getOrigin().getTag() == PINNED_TAG;
	}
}
