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
package org.apache.juneau.rest.client.classic.remote;

/**
 * Marks the current thread as executing an SSRF-guard-active {@code @Remote} call, for the duration of one
 * blocking {@code RestRequest.run()} (including any internal retries/redirect hops it triggers on the same
 * thread).
 *
 * <p>
 * The classic engine shares a single {@code CloseableHttpClient} (and its connection manager) across every
 * request the client issues, whether {@code @Remote}-originated or an ordinary {@code client.get(url)} call. Since
 * neither Apache HttpClient's {@code DnsResolver} SPI nor its {@code RedirectStrategy} SPI receives enough
 * information to distinguish which caller triggered a given connection/redirect decision by request alone, this
 * thread-local flag is set for the duration of exactly one {@code @Remote} invocation (which runs synchronously,
 * blocking the calling thread, per Juneau's classic-engine execution model) and consulted by the shared
 * {@code DnsResolver}/{@code RedirectStrategy} installed on the client's connection manager to decide whether to
 * enforce {@code org.apache.juneau.http.remote.RemoteUrlPolicy} for that connection/redirect &mdash; leaving
 * ordinary (non-{@code @Remote}) requests on the same client completely unaffected.
 *
 * @since 10.0.0
 */
public final class RemoteUrlPolicyState {

	private RemoteUrlPolicyState() {}

	private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

	/**
	 * Marks the calling thread as executing an SSRF-guard-active {@code @Remote} call.
	 *
	 * <p>
	 * Callers must invoke {@link #deactivate()} in a {@code finally} block once the call (including any retries)
	 * has completed.
	 */
	public static void activate() {
		ACTIVE.set(Boolean.TRUE);
	}

	/** Clears the calling thread's SSRF-guard-active marker. */
	public static void deactivate() {
		ACTIVE.remove();
	}

	/**
	 * Returns {@code true} if the calling thread is currently executing an SSRF-guard-active {@code @Remote} call.
	 *
	 * @return {@code true} if the guard is active on this thread.
	 */
	public static boolean isActive() {
		return ACTIVE.get() == Boolean.TRUE;
	}
}
