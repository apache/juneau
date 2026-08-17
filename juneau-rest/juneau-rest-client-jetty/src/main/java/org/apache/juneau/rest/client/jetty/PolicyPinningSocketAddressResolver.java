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
import org.eclipse.jetty.util.*;

/**
 * {@link SocketAddressResolver} that pin-on-connects SSRF-guard-active {@code @Remote} connections: while
 * {@link JettyHttpTransport#isPolicyActive()} is set on the calling thread, resolution is restricted to a single
 * address selected by {@link RemoteUrlPolicy#selectAllowedAddress} &mdash; the exact address Jetty's connector then
 * connects the socket to, while still using the original hostname (from the request's own origin) for TLS
 * SNI/hostname verification. Ordinary (non-{@code @Remote}) connections on the same {@code HttpClient} delegate to
 * the wrapped resolver unchanged.
 *
 * @since 10.0.0
 */
final class PolicyPinningSocketAddressResolver implements SocketAddressResolver {

	private final SocketAddressResolver delegate;

	PolicyPinningSocketAddressResolver(SocketAddressResolver delegate) {
		this.delegate = delegate;
	}

	@Override /* SocketAddressResolver */
	public void resolve(String host, int port, Map<String, Object> context, Promise<List<InetSocketAddress>> promise) {
		if (! JettyHttpTransport.isPolicyActive()) {
			delegate.resolve(host, port, context, promise);
			return;
		}
		try {
			var pinned = RemoteUrlPolicy.selectAllowedAddress(host, false, RemoteUrlPolicy.AddressResolver.DEFAULT);
			promise.succeeded(List.of(new InetSocketAddress(pinned, port)));
		} catch (UnknownHostException e) {
			promise.failed(e);
		} catch (RuntimeException e) {
			promise.failed(e);
		}
	}
}
