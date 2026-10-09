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
package org.apache.juneau.rest.server.bus;

import java.util.*;

/**
 * One page's bridge session.  Created by the session POST; reused across reconnects within the grace period.
 *
 * <p>
 * The session's capability id is deliberately not on this interface: it never leaves the bus module except in the
 * session POST response, and {@link #toString()} redacts it.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jv>bus</jv>.publishTo(<jv>s</jv> -&gt; <js>"east"</js>.equals(<jv>s</jv>.attribute(<js>"region"</js>)), <js>"ops.alert:east"</js>, <jv>alert</jv>);
 * </p>
 *
 * @since 10.0.0
 */
public interface BusSession {

	/**
	 * The bridge id from the page contract (C1 id pattern).
	 *
	 * @return The bridge id.  Never <jk>null</jk>.
	 */
	String bridgeId();

	/**
	 * The transport requested by the session POST.
	 *
	 * @return {@code "sse"} or {@code "websocket"}.
	 */
	String transport();

	/**
	 * The name of the request principal recorded when the session was opened.
	 *
	 * @return The principal name, or <jk>null</jk> when the request was anonymous.
	 */
	String principalName();

	/**
	 * A value captured by {@link BusPolicy.Builder#sessionAttributes(java.util.function.Function)} at session open.
	 *
	 * @param name The attribute name.
	 * @return The value, or <jk>null</jk> if absent.
	 */
	Object attribute(String name);

	/**
	 * The downstream topics and patterns this session was granted, in request order.
	 *
	 * @return An unmodifiable set.
	 */
	Set<String> downstream();

	/**
	 * The upstream topics this session was granted (WebSocket sessions only), in request order.
	 *
	 * @return An unmodifiable set.
	 */
	Set<String> upstream();

	/**
	 * Whether a transport connection is currently attached.
	 *
	 * @return <jk>true</jk> while a stream or socket is connected.
	 */
	boolean isConnected();
}
