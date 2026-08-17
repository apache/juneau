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
package org.apache.juneau.rest.client.classic;

import java.net.*;

import org.apache.http.conn.*;
import org.apache.http.impl.conn.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.classic.remote.*;

/**
 * {@link DnsResolver} that pin-on-connects SSRF-guard-active {@code @Remote} connections: while
 * {@link RemoteUrlPolicyState#isActive()} is set on the calling thread, resolution is restricted to a single
 * address selected by {@link RemoteUrlPolicy#selectAllowedAddress} &mdash; the exact address Apache HttpClient's
 * connection operator then connects the socket to, while still using the original hostname for the {@code Host}
 * header and TLS SNI/hostname verification (Apache HttpClient's connect step keeps the {@code HttpRoute}'s target
 * host for those purposes; only the socket address comes from this resolver).  Ordinary (non-{@code @Remote})
 * connections on the same shared connection manager delegate to {@link SystemDefaultDnsResolver} unchanged.
 *
 * @since 10.0.0
 */
final class PolicyPinningDnsResolver implements DnsResolver {

	static final PolicyPinningDnsResolver INSTANCE = new PolicyPinningDnsResolver();

	private PolicyPinningDnsResolver() {}

	@Override /* DnsResolver */
	public InetAddress[] resolve(String host) throws UnknownHostException {
		if (! RemoteUrlPolicyState.isActive())
			return SystemDefaultDnsResolver.INSTANCE.resolve(host);
		var pinned = RemoteUrlPolicy.selectAllowedAddress(host, false, RemoteUrlPolicy.AddressResolver.DEFAULT);
		return new InetAddress[]{pinned};
	}
}
