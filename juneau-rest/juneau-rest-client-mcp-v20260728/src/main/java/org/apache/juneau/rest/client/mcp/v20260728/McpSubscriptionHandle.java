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
package org.apache.juneau.rest.client.mcp.v20260728;

import java.io.*;

/**
 * Closeable handle for a live {@link McpClient#listen(org.apache.juneau.bean.mcp.v20260728.SubscriptionFilter, McpSubscriptionListener)}
 * subscription.
 *
 * @since 10.0.0
 */
public interface McpSubscriptionHandle extends Closeable {

	/**
	 * The client-generated JSON-RPC request id this subscription's opening
	 * {@link McpClient#listen(org.apache.juneau.bean.mcp.v20260728.SubscriptionFilter, McpSubscriptionListener)}
	 * call sent, and which the server echoes back as the {@code subscriptionId} on every frame of this stream
	 * (the acknowledged frame, each change notification, and the terminal frame). Callers can use this to
	 * correlate this handle against server-side logs/traces for the same subscription.
	 *
	 * @return The request id. Never <jk>null</jk> or empty.
	 */
	String id();

	/**
	 * Cancels the subscription: signals the background pump to stop and closes the underlying stream. Idempotent.
	 *
	 * <p>
	 * Asynchronous: this method returns as soon as the stop signal is issued and the stream close is requested,
	 * <i>without</i> waiting for the background pump thread to actually finish running. A caller that needs to
	 * know the pump has fully stopped must poll {@link #isOpen()} or otherwise synchronize with it separately —
	 * this method does not join it.
	 */
	void cancel();

	/**
	 * Alias for {@link #cancel()}, satisfying {@link Closeable} for try-with-resources use.
	 *
	 * <p>
	 * Deliberately declares no checked {@link IOException} (narrows {@link Closeable#close()}'s throws clause) —
	 * the background pump owns the stream's lifecycle and closing it is always a local, non-throwing operation.
	 *
	 * <p>
	 * Like {@link #cancel()}, this is asynchronous: it signals the pump to stop but does not wait for it to exit.
	 */
	@Override
	void close();

	/**
	 * @return <jk>true</jk> if the subscription is still open (not cancelled, errored, or gracefully completed).
	 */
	boolean isOpen();
}
