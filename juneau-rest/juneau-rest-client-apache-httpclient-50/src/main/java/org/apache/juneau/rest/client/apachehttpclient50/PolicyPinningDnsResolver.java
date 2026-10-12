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
package org.apache.juneau.rest.client.apachehttpclient50;

import java.net.*;

import org.apache.hc.client5.http.*;
import org.apache.juneau.http.remote.*;

/**
 * {@link DnsResolver} that pin-on-connects SSRF-guard-active {@code @Remote} connections: resolution is restricted to
 * a single address selected by {@link RemoteUrlPolicy#selectAllowedAddress} &mdash; the exact address Apache
 * HttpClient's connection operator then connects the socket to, while still using the original hostname for the
 * {@code Host} header and TLS SNI/hostname verification.
 *
 * <p>
 * It is installed only on the connection manager of {@link ApacheHc5Transport}'s dedicated client for guarded
 * requests, so every connection in that pool is opened through the pin.  Ordinary (non-{@code @Remote}) requests use
 * the transport's other client, with its own pool and the system resolver, and are completely unaffected.
 *
 * @since 10.0.0
 */
final class PolicyPinningDnsResolver implements DnsResolver {

	static final PolicyPinningDnsResolver INSTANCE = new PolicyPinningDnsResolver();

	private PolicyPinningDnsResolver() {}

	@Override /* DnsResolver */
	public InetAddress[] resolve(String host) throws UnknownHostException {
		// Guarded requests are SSRF-guard-active only when the allowPrivateUrls opt-in is off
		// (TransportRequest.isSsrfGuardActive()), so the deny-private checks always apply here.
		return new InetAddress[]{RemoteUrlPolicy.selectAllowedAddress(host, false, RemoteUrlPolicy.AddressResolver.DEFAULT)};
	}

	@Override /* DnsResolver */
	public String resolveCanonicalHostname(String host) throws UnknownHostException {
		return SystemDefaultDnsResolver.INSTANCE.resolveCanonicalHostname(host);
	}
}
