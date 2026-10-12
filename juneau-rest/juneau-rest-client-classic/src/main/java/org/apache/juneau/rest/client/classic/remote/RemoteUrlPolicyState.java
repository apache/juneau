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
 * The classic engine shares a single {@code CloseableHttpClient} across every request the client issues, whether
 * {@code @Remote}-originated or an ordinary {@code client.get(url)} call. Since neither Apache HttpClient's
 * {@code HttpClientConnectionManager.requestConnection} SPI nor its {@code RedirectStrategy} SPI receives enough
 * information to distinguish which caller triggered a given connection lease or redirect decision, this thread-local
 * flag is set for the duration of exactly one {@code @Remote} invocation (which runs synchronously, blocking the
 * calling thread, per Juneau's classic-engine execution model).  While it is set, the client's connection manager
 * leases connections from a dedicated pool whose connections are all opened through the
 * {@code org.apache.juneau.http.remote.RemoteUrlPolicy} pin, and its redirect strategy defers {@code 3xx} handling
 * to Juneau's revalidating redirect loop &mdash; leaving ordinary (non-{@code @Remote}) requests on the same client,
 * and their separate pool, completely unaffected.
 *
 * @since 10.0.0
 */
public final class RemoteUrlPolicyState {

	private RemoteUrlPolicyState() {}

	// Nesting depth of the guarded calls on this thread (a guarded call can make another one, for example from a
	// request interceptor); absent when none is running.
	private static final ThreadLocal<int[]> DEPTH = new ThreadLocal<>();

	/**
	 * Marks the calling thread as executing an SSRF-guard-active {@code @Remote} call.
	 *
	 * <p>
	 * Callers must invoke {@link #deactivate()} in a {@code finally} block once the call (including any retries)
	 * has completed.  Calls nest: the thread stays marked until every {@code activate()} has been matched by a
	 * {@code deactivate()}, so a guarded call made while another one is running does not end the outer call's guard.
	 */
	public static void activate() {
		var depth = DEPTH.get();
		if (depth == null)
			DEPTH.set(new int[] { 1 });
		else
			depth[0]++;
	}

	/** Ends one {@link #activate()}; the calling thread's marker is cleared when the outermost guarded call ends. */
	public static void deactivate() {
		var depth = DEPTH.get();
		if (depth != null && --depth[0] <= 0)
			DEPTH.remove();
	}

	/**
	 * Returns {@code true} if the calling thread is currently executing an SSRF-guard-active {@code @Remote} call.
	 *
	 * @return {@code true} if the guard is active on this thread.
	 */
	public static boolean isActive() {
		return DEPTH.get() != null;
	}
}
