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
package org.apache.juneau.rest.client.okhttp;

import java.net.*;
import java.util.*;

import org.apache.juneau.http.remote.*;

import okhttp3.*;

/**
 * {@link Dns} that pin-on-connects every lookup it performs, by selecting a single address via
 * {@link RemoteUrlPolicy#selectAllowedAddress} &mdash; the exact address OkHttp's connection pool then connects the
 * socket to, while still using the original hostname (from the request URL) for TLS SNI/hostname verification.
 *
 * <p>
 * Installed only on the dedicated, policy-only {@link OkHttpClient} instance {@link OkHttpTransport} derives (via
 * {@link OkHttpClient#newBuilder()}) for SSRF-guard-active {@code @Remote} calls; that derived client shares the
 * connection pool and dispatcher of the transport's ordinary client but is never used for ordinary (non-{@code @Remote})
 * requests, so no per-call/thread-local guard is needed here.
 *
 * @since 10.0.0
 */
final class PolicyPinningDns implements Dns {

	static final PolicyPinningDns INSTANCE = new PolicyPinningDns();

	private PolicyPinningDns() {}

	@Override /* Dns */
	public List<InetAddress> lookup(String hostname) throws UnknownHostException {
		return List.of(RemoteUrlPolicy.selectAllowedAddress(hostname, false, RemoteUrlPolicy.AddressResolver.DEFAULT));
	}
}
