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
package org.apache.juneau.rest.server.health;

/**
 * Implemented by servlets that want to be told the embedded server's per-service {@link ReadinessState}, so they
 * can react when shutdown begins (see {@link ReadinessState#onOutOfService(Object, Runnable)}).
 *
 * <p>
 * The embedded-server lifecycle component (e.g. {@code JettyServerComponent}, {@code TomcatServerComponent})
 * calls {@link #acceptReadinessState(ReadinessState)} on each auto-discovered servlet that implements this
 * interface, before the server starts.  A typical use is closing long-lived streaming connections at the start
 * of a graceful shutdown so the drain does not have to wait out its stop timeout for them.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/events"</js>)
 * 	<jk>public class</jk> EventsResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> ReadinessStateAware {
 *
 * 		<ja>@Override</ja> <jc>/* ReadinessStateAware *&#47;</jc>
 * 		<jk>public void</jk> acceptReadinessState(ReadinessState <jv>state</jv>) {
 * 			<jc>// Stable key: a re-publish on server restart replaces this callback instead of adding another.</jc>
 * 			<jv>state</jv>.onOutOfService(<js>"my-streams"</js>, <jk>this</jk>::closeStreams);
 * 		}
 *
 * 		<jk>private void</jk> closeStreams() {
 * 			<jc>// End long-lived SSE/streaming responses so the graceful drain can finish.</jc>
 * 		}
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public interface ReadinessStateAware {

	/**
	 * Receives the lifecycle-owned readiness state.
	 *
	 * <p>
	 * May be called more than once (once per server start); implementations should register callbacks under a
	 * stable key so repeated calls replace rather than accumulate.
	 *
	 * @param state The per-service readiness state.  Never <jk>null</jk>.
	 */
	void acceptReadinessState(ReadinessState state);
}
